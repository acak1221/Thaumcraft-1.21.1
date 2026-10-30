#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Assembles the standalone repository of Thaumic Tinkerer (Fabric, Minecraft 1.21.1) from the Thaumcraft 4 Reload monorepo.
#
#   addons/thaumic-tinkerer/tools/export-standalone.sh [-n] [-f] [-d] TARGET_DIR
#
# TARGET_DIR gets, in this order of precedence (a later source replaces a file of the same path of an earlier one):
#   1. the add-on tree, addons/thaumic-tinkerer (without standalone/, tools/export-standalone.sh and local state, see below)
#   2. the template, addons/thaumic-tinkerer/standalone/ (settings.gradle, build.gradle, gradle.properties, .gitignore, the CI workflow, README.md,
#      README.ru.md, NOTICE.md, LICENSE-MIT, tools/publish-thaumcraft.sh, tools/porting.sh, scripts/porting/README.md)
#   3. the Gradle wrapper of the monorepo (gradlew, gradlew.bat, gradle/wrapper/) and its .gitattributes (line endings)
#   4. the Python tools that the add-on's documents name, from scripts/porting/ of the monorepo (MIT), into scripts/porting/
# and the marker EXPORTED-FROM-MONOREPO.txt (monorepo commit, whether its worktree was modified, a digest of the exported tree).
#
# The script is idempotent: it compares the new tree with TARGET_DIR file by file, writes only what differs, removes the files that no longer
# exist in the monorepo, and reports "no changes" when nothing differs. It never touches TARGET_DIR/.git. Local state is left alone, in the target
# and in the monorepo alike: .git/ .gradle/ .tmp/ build/ out/ run/ run-*/ .idea/ .vscode/ in the root, logs/ crash-reports/ .mixin.out/ __pycache__/ at
# any depth, and the files *.log *.pyc *.hprof *~ *.swp .DS_Store.
#
# It only writes into a directory that it made, and checks that before it changes anything (exit 1, nothing written):
#   * TARGET_DIR must be empty (a directory with only .git counts as empty) or hold the marker file, and the marker must be a real one: its first
#     line is the one this script writes and the tree_sha256 that it records is the digest of the files that are there now. A directory whose
#     files differ from the last export (edited by hand, files added by hand, a marker copied from elsewhere) is refused; -d overwrites and
#     removes them. The other directories of a machine cannot be made targets by naming the marker file.
#   * TARGET_DIR must not be inside the monorepo or contain it, must not be / and must not contain a '..' after a part that does not exist yet.
#   * a marked TARGET_DIR must not hold a symbolic link to a directory (or to nothing): the export does not write through links.
# The marker is written last, so an export that was cut short leaves a directory that is refused, not one that looks complete.
# Needs bash 4.4+, git, find, cmp, cp, sha256sum (or shasum).
#
#   -n   dry run: say what would change, write nothing (a target that differs from its marker is reported, not refused)
#   -f   export although the status table of a standalone README differs from the one of the add-on README (otherwise exit 3)
#   -d   export although the files of TARGET_DIR differ from the last export: they are overwritten or removed
#
# Exit codes: 0 done (or nothing to do), 1 refused, 2 usage or environment error, 3 README drift.
set -euo pipefail
if [ "${BASH_VERSINFO[0]}" -lt 4 ] || { [ "${BASH_VERSINFO[0]}" -eq 4 ] && [ "${BASH_VERSINFO[1]}" -lt 4 ]; }; then
  echo "export-standalone: bash 4.4 or newer is needed (this is bash $BASH_VERSION; on macOS install a newer one, e.g. with Homebrew)" >&2; exit 2
fi

ADDON="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
MONO="$(cd "$ADDON/../.." && pwd -P)"
MARKER="EXPORTED-FROM-MONOREPO.txt"
MARKER_FIRST_LINE="This directory was written by addons/thaumic-tinkerer/tools/export-standalone.sh of the Thaumcraft 4 Reload monorepo."
# The Python tools the add-on's documents name (NOTICE.md, docs/PARITY_REPORT.md, docs/DEVIATIONS.md), each with its unit tests.
PORTING_FILES=(tt_assets.py tt_assets_map.json tt_oracle.py test_tt_assets.py test_tt_oracle.py)
DEFAULT_MONOREPO_URL="https://github.com/acak1221/Thaumcraft-1.21.1"

DRY=0; FORCE=0; DISCARD=0
usage() { sed -n '3,$ { /^#/!q; s/^# \{0,1\}//; p }' "${BASH_SOURCE[0]}" >&2; exit 2; }
while getopts "nfd" opt; do
  case "$opt" in n) DRY=1;; f) FORCE=1;; d) DISCARD=1;; *) usage;; esac
done
shift $((OPTIND-1))
[ "$#" -eq 1 ] || usage
say() { echo "export-standalone: $*"; }
die() { echo "export-standalone: $1" >&2; exit "${2:-1}"; }

[ -d "$ADDON/standalone" ] && [ -f "$MONO/gradlew" ] && [ -f "$MONO/.gitattributes" ] && [ -f "$MONO/gradle/wrapper/gradle-wrapper.jar" ] && [ -d "$MONO/scripts/porting" ] \
  || die "this script must stay in addons/thaumic-tinkerer/tools of the monorepo (looked at $MONO)" 2
if command -v sha256sum >/dev/null 2>&1; then SHA=(sha256sum)
elif command -v shasum >/dev/null 2>&1; then SHA=(shasum -a 256)
else die "neither sha256sum nor shasum found" 2; fi

# --- the files of a tree: no local state --------------------------------------------------------------------------------
list_files() { # DIR [-l] [EXTRA_ROOT_DIR_TO_SKIP...]: relative paths of the files (-l: and of the symbolic links), NUL separated, sorted
  local dir="$1" kinds=(-type f) extra=() p; shift
  if [ "${1:-}" = "-l" ]; then kinds=(\( -type f -o -type l \)); shift; fi
  for p in "$@"; do extra+=(-o -path "$p"); done
  (cd "$dir" && find . \( -type d \( -path ./.git -o -path ./.gradle -o -path ./.tmp -o -path ./build -o -path ./out -o -path ./run -o -path './run-*' \
        -o -path ./.idea -o -path ./.vscode -o -name logs -o -name crash-reports -o -name .mixin.out -o -name __pycache__ "${extra[@]}" \) -prune \) -o \
      "${kinds[@]}" ! -name '*.log' ! -name '*.pyc' ! -name '*.hprof' ! -name '.DS_Store' ! -name '*~' ! -name '*.swp' -print0 | LC_ALL=C sort -z)
}
tree_digest() { # DIR: sha256 of the sha256 list of its files (the marker not counted); what the marker records as tree_sha256
  (cd "$1" && list_files . | xargs -0 "${SHA[@]}" | { grep -v -F "  ./$MARKER" || true; } | "${SHA[@]}" | cut -d' ' -f1)
}

# --- the target ----------------------------------------------------------------------------------------------------
# An absolute path without symlinks, also for a directory that does not exist yet: the part that exists is resolved by the shell, the rest
# is taken as it is and must not contain '..' (it would lead somewhere that the checks below did not look at).
[ -n "$1" ] || die "the target directory is empty" 2
T="$1"; REST=""
while [ ! -d "$T" ]; do
  [ ! -L "$T" ] || die "$1: $T is a symbolic link that points to nothing" 1
  [ ! -e "$T" ] || die "$1: $T exists and is not a directory" 1
  part="$(basename -- "$T")"
  case "$part" in
    ..) die "$1: '..' after a part that does not exist; give the path without it (the checks could not tell where it leads)" 1;;
    .)  ;;
    *)  REST="/$part$REST";;
  esac
  T="$(dirname -- "$T")"
done
TARGET="$(cd -P -- "$T" && pwd -P)$REST"
TARGET="${TARGET%/}"
[ -n "$TARGET" ] || die "/ cannot be the target" 1
case "$TARGET/" in "$MONO"/*) die "$TARGET is inside the monorepo $MONO" 1;; esac
case "$MONO/" in "$TARGET"/*) die "$TARGET contains the monorepo $MONO" 1;; esac

HAS_MARKER=0
if [ -d "$TARGET" ]; then
  if [ -e "$TARGET/$MARKER" ] || [ -L "$TARGET/$MARKER" ]; then
    { [ -f "$TARGET/$MARKER" ] && [ ! -L "$TARGET/$MARKER" ] && [ "$(head -n 1 "$TARGET/$MARKER" | tr -d '\r')" = "$MARKER_FIRST_LINE" ]; } \
      || die "$TARGET/$MARKER is not a marker written by this script (a regular file whose first line is \"$MARKER_FIRST_LINE\"); nothing was changed" 1
    HAS_MARKER=1
  elif [ -n "$(find "$TARGET" -mindepth 1 -maxdepth 1 ! -name .git -print -quit)" ]; then
    die "$TARGET is not empty and has no $MARKER: it was not made by this script; nothing was changed" 1
  fi
fi
if [ "$HAS_MARKER" = 1 ]; then
  BAD_LINKS=()
  while IFS= read -r -d '' rel; do
    rel="${rel#./}"
    if [ -d "$TARGET/$rel" ] || [ ! -e "$TARGET/$rel" ]; then BAD_LINKS+=("$rel"); fi
  done < <(list_files "$TARGET" -l)
  [ "${#BAD_LINKS[@]}" -eq 0 ] \
    || die "$TARGET holds symbolic links to directories or to nothing (${BAD_LINKS[*]:0:3}): the export does not write through links; remove them. Nothing was changed" 1
  RECORDED="$(sed -n 's/^tree_sha256:[[:space:]]*\([0-9a-f]\{64\}\).*/\1/p' "$TARGET/$MARKER" | head -n 1)"
  if [ "$RECORDED" != "$(tree_digest "$TARGET")" ]; then
    if [ "$DISCARD" = 1 ]; then say "WARNING: the files of $TARGET differ from the last export; -d: they are overwritten or removed"
    elif [ "$DRY" = 1 ]; then say "WARNING: the files of $TARGET differ from the last export (edited or added by hand?); a real run is refused without -d"
    else die "the files of $TARGET differ from the state that its $MARKER records (edited or added by hand, or a marker that is not from this directory): they would be overwritten or removed; nothing was changed. See what would change with -n; -d goes on" 1; fi
  fi
fi

# --- the work directory; the status tables of the standalone READMEs must be the ones of the add-on README -----------------
WORK="$(mktemp -d -t tt-export.XXXXXX)"
STAGE="$WORK/stage"; mkdir "$STAGE"
trap 'rm -rf "$WORK"' EXIT
status_section() { awk -v h="$2" '$0 == "## " h { f = 1; next } /^## / { f = 0 } f' "$1" | sed 's#fabric/build/#build/#g' | grep -v '^[[:space:]]*$' || true; }
first_difference() { # A_FILE B_FILE: the first differing line of two files, around the first differing character
  diff "$1" "$2" | awk '
    /^< / && a == "" { a = substr($0, 3); next }
    /^> / && b == "" { b = substr($0, 3) }
    END { n = length(a) < length(b) ? length(a) : length(b)
          for (i = 1; i <= n && substr(a, i, 1) == substr(b, i, 1); i++) ;
          s = i > 40 ? i - 40 : 1
          printf "  add-on README: ...%s\n  standalone   : ...%s\n", substr(a, s, 100), substr(b, s, 100) }' >&2 || true
}
DRIFT=0
for pair in "README.md|Status" "README.ru.md|Состояние"; do
  f="${pair%%|*}"; h="${pair#*|}"
  [ -f "$ADDON/$f" ] && [ -f "$ADDON/standalone/$f" ] || die "missing $f in the add-on or in standalone/" 2
  status_section "$ADDON/$f" "$h" > "$WORK/addon.$f"
  status_section "$ADDON/standalone/$f" "$h" > "$WORK/standalone.$f"
  if ! cmp -s "$WORK/addon.$f" "$WORK/standalone.$f"; then
    DRIFT=1
    echo "export-standalone: the section \"$h\" of standalone/$f differs from the one of $f (apart from fabric/build/ -> build/); first difference:" >&2
    first_difference "$WORK/addon.$f" "$WORK/standalone.$f"
  fi
done
if [ "$DRIFT" = 1 ]; then
  [ "$FORCE" = 1 ] && say "WARNING: exporting with a stale status table (-f)" \
    || die "bring standalone/README.md and standalone/README.ru.md up to date (or use -f); nothing was changed" 3
fi

# --- the new tree, in the work directory ---------------------------------------------------------------------------------
copy_tree() { # SRC_DIR [EXCLUDED_RELATIVE_PATH...]
  local src="$1" rel skip x; shift
  while IFS= read -r -d '' rel; do
    rel="${rel#./}"; skip=0
    for x in "$@"; do [ "$rel" = "$x" ] && skip=1; done
    [ "$skip" = 1 ] && continue
    mkdir -p "$STAGE/$(dirname "$rel")"
    cp -p "$src/$rel" "$STAGE/$rel"
  done < <(list_files "$src" ./standalone)
}

copy_tree "$ADDON" tools/export-standalone.sh
REPLACED=()
while IFS= read -r -d '' rel; do
  rel="${rel#./}"
  [ -e "$STAGE/$rel" ] && REPLACED+=("$rel")
  mkdir -p "$STAGE/$(dirname "$rel")"
  cp -p "$ADDON/standalone/$rel" "$STAGE/$rel"
done < <(list_files "$ADDON/standalone")
TEMPLATE_COUNT=$(list_files "$ADDON/standalone" | tr -cd '\0' | wc -c)

mkdir -p "$STAGE/gradle/wrapper"
cp -p "$MONO/gradlew" "$MONO/gradlew.bat" "$MONO/.gitattributes" "$STAGE/"
cp -p "$MONO/gradle/wrapper/gradle-wrapper.jar" "$MONO/gradle/wrapper/gradle-wrapper.properties" "$STAGE/gradle/wrapper/"
mkdir -p "$STAGE/scripts/porting"
for f in "${PORTING_FILES[@]}"; do
  [ -f "$MONO/scripts/porting/$f" ] || die "the Python tool scripts/porting/$f is missing in the monorepo" 2
  cp -p "$MONO/scripts/porting/$f" "$STAGE/scripts/porting/$f"
done

# Nothing of Thaumcraft 4 (a jar, the decompiled source) and no jar but the Gradle wrapper may travel.
BAD="$(cd "$STAGE" && find . \( -name '*.jar' ! -path ./gradle/wrapper/gradle-wrapper.jar -o -path '*/thaumcraft_src*' -o -name '*.class' \) -print | head -n 5)"
[ -z "$BAD" ] || die "the export would contain files that must not travel: $BAD" 2

# --- the marker -------------------------------------------------------------------------------------------------------
COMMIT="unknown (the monorepo is not a git checkout)"; BRANCH="unknown"; URL=""; WORKTREE="unknown"; DIRTY=1
if git -C "$MONO" rev-parse --git-dir >/dev/null 2>&1; then
  COMMIT="$(git -C "$MONO" rev-parse HEAD 2>/dev/null || echo "unknown (no commit yet)")"
  BRANCH="$(git -C "$MONO" rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)"
  URL="$(git -C "$MONO" remote get-url origin 2>/dev/null | sed -E 's#//[^/@]*@#//#' || true)"
  if ! STATUS="$(git -C "$MONO" status --porcelain -- addons/thaumic-tinkerer scripts/porting gradle gradlew gradlew.bat .gitattributes 2>/dev/null)"; then
    WORKTREE="unknown (git status failed)"
  elif [ -n "$STATUS" ]; then
    WORKTREE="modified (uncommitted changes in the exported paths)"
  else WORKTREE="clean"; DIRTY=0; fi
fi
TREE_SHA="$(tree_digest "$STAGE")"
cat > "$STAGE/$MARKER" <<EOF
$MARKER_FIRST_LINE
The monorepo is the place of development for now: do not edit files here by hand, the next export refuses a tree that differs from this one (and overwrites the edits only with -d).
When this repository takes over, delete this file (docs/EXPORT.md, "Making the standalone repository the primary one").

monorepo:          ${URL:-$DEFAULT_MONOREPO_URL}
monorepo_branch:   $BRANCH
monorepo_commit:   $COMMIT
monorepo_worktree: $WORKTREE
tree_sha256:       $TREE_SHA   (sha256 of the sha256 list of every other file of this tree)
EOF

# --- compare with the target and write the differences ---------------------------------------------------------------------
differs() { # STAGED_FILE TARGET_FILE: other content or other executable bit
  ! cmp -s "$1" "$2" && return 0
  { [ -x "$1" ] && [ ! -x "$2" ]; } && return 0
  { [ ! -x "$1" ] && [ -x "$2" ]; } && return 0
  return 1
}
mapfile -d '' -t STAGED < <(list_files "$STAGE")
ADDED=(); UPDATED=(); REMOVED=(); UNCHANGED=0; WRITE_MARKER=0
if [ "$DRY" = 0 ]; then
  mkdir -p "$TARGET"
  [ "$(cd -P -- "$TARGET" && pwd -P)" = "$TARGET" ] || die "$TARGET does not resolve to itself after it was made; nothing was written" 1
fi
for rel in "${STAGED[@]}"; do
  rel="${rel#./}"
  if [ ! -f "$TARGET/$rel" ]; then ADDED+=("$rel")
  elif differs "$STAGE/$rel" "$TARGET/$rel"; then UPDATED+=("$rel")
  else UNCHANGED=$((UNCHANGED+1)); continue; fi
  if [ "$rel" = "$MARKER" ]; then WRITE_MARKER=1; continue; fi      # the marker is written last
  if [ "$DRY" = 0 ]; then
    mkdir -p "$TARGET/$(dirname "$rel")"
    rm -f "$TARGET/$rel"
    cp -p "$STAGE/$rel" "$TARGET/$rel"
  fi
done
# Files of the target that the new tree no longer has (local state excepted).
if [ -d "$TARGET" ]; then
  mapfile -d '' -t PRESENT < <(list_files "$TARGET" -l)
  for rel in "${PRESENT[@]}"; do
    rel="${rel#./}"
    if [ -f "$STAGE/$rel" ] && [ ! -L "$TARGET/$rel" ]; then continue; fi
    REMOVED+=("$rel")
    [ "$DRY" = 1 ] || rm -f "$TARGET/$rel"
  done
  # Directories that the removals left empty (never .git or the local-state directories).
  if [ "$DRY" = 0 ] && [ "${#REMOVED[@]}" -gt 0 ]; then
    KEEP=()
    for d in ./.git ./.gradle ./.tmp ./build ./out ./run './run-*' ./.idea ./.vscode; do KEEP+=(! -path "$d" ! -path "$d/*"); done
    for n in logs crash-reports .mixin.out __pycache__; do KEEP+=(! -name "$n" ! -path "*/$n/*"); done
    (cd "$TARGET" && find . -depth -type d -empty ! -path . "${KEEP[@]}" -delete 2>/dev/null || true)
  fi
fi
if [ "$WRITE_MARKER" = 1 ] && [ "$DRY" = 0 ]; then
  rm -f "$TARGET/$MARKER"
  cp -p "$STAGE/$MARKER" "$TARGET/$MARKER"
fi

# --- the summary --------------------------------------------------------------------------------------------------------
FILES="${#STAGED[@]}"
say "monorepo ${COMMIT:0:12} on $BRANCH, worktree $WORKTREE"
REPLACED_LIST=""; [ "${#REPLACED[@]}" -eq 0 ] || REPLACED_LIST=" (${REPLACED[*]})"
say "template: $TEMPLATE_COUNT files, ${#REPLACED[@]} of them replace add-on files$REPLACED_LIST"
list_some() { # LABEL ARRAY...
  local label="$1" n=0; shift
  for x in "$@"; do n=$((n+1)); [ "$n" -le 10 ] && echo "export-standalone:   $label $x"; done
  [ "$n" -le 10 ] || echo "export-standalone:   ... and $((n-10)) more $label"
}
[ "${#ADDED[@]}" -eq 0 ] || [ "$HAS_MARKER" = 0 ] || list_some added "${ADDED[@]}"
[ "${#UPDATED[@]}" -eq 0 ] || list_some updated "${UPDATED[@]}"
[ "${#REMOVED[@]}" -eq 0 ] || list_some removed "${REMOVED[@]}"
CHANGES=$(( ${#ADDED[@]} + ${#UPDATED[@]} + ${#REMOVED[@]} ))
VERB="written to"; [ "$DRY" = 1 ] && VERB="would be written to"; [ "$CHANGES" -ne 0 ] || VERB="compared with"
say "$FILES files $VERB $TARGET: ${#ADDED[@]} added, ${#UPDATED[@]} updated, ${#REMOVED[@]} removed, $UNCHANGED unchanged"
if [ "$CHANGES" -eq 0 ]; then say "no changes"
elif [ "$DRY" = 1 ]; then say "dry run: nothing was written"
elif [ "$HAS_MARKER" = 0 ]; then say "new export; the .git directory of the target was not touched; nothing was committed"; fi
[ "$DIRTY" = 0 ] || say "NOTE: the monorepo worktree has changes that are not committed (or its state is unknown); the marker says so"
exit 0
