#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Fast compile of the Thaumic Tinkerer sources with javac, outside Gradle.
#
#   tools/compile.sh [-o OUTDIR] [-g] [FILE_OR_DIR ...]     (monorepo: addons/thaumic-tinkerer/tools/compile.sh)
#
# With no FILE_OR_DIR the whole add-on (common + fabric main sources) is compiled. Given files or directories, only
# those are compiled and javac finds the rest of the add-on through the source path, so a group can check its own
# files while another group is half way through its edits.  -g adds the GameTest sources and classpath.
# The classpath comes from `./gradlew exportTinkererClasspath` (monorepo: `./gradlew :fabric:exportTinkererClasspath`); run it once per checkout.
# Output goes to OUTDIR (default: a fresh directory under the system temp dir) and is not used by Gradle.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT=""; GAMETEST=0
while getopts "o:g" opt; do
  case "$opt" in o) OUT="$OPTARG";; g) GAMETEST=1;; *) exit 2;; esac
done
shift $((OPTIND-1))
CPFILE="$ROOT/.tmp/classpath.txt"; [ "$GAMETEST" = 1 ] && CPFILE="$ROOT/.tmp/classpath-gametest.txt"
if [ ! -f "$CPFILE" ]; then
  TASK="exportTinkererClasspath"; [ -f "$ROOT/settings.gradle" ] || TASK=":fabric:exportTinkererClasspath"   # no settings.gradle next to tools/: the monorepo
  echo "missing $CPFILE: run ./gradlew $TASK" >&2; exit 2
fi
[ -n "$OUT" ] || OUT="$(mktemp -d -t tt-javac.XXXXXX)"
mkdir -p "$OUT"
SRCPATH="$ROOT/common/src/main/java:$ROOT/fabric/src/main/java"
[ "$GAMETEST" = 1 ] && SRCPATH="$SRCPATH:$ROOT/common/src/gameTest/java:$ROOT/fabric/src/gameTest/java"
if [ "$#" -eq 0 ]; then
  mapfile -t FILES < <(find "$ROOT/common/src/main/java" "$ROOT/fabric/src/main/java" -name '*.java')
  [ "$GAMETEST" = 1 ] && mapfile -t -O "${#FILES[@]}" FILES < <(find "$ROOT/common/src/gameTest/java" "$ROOT/fabric/src/gameTest/java" -name '*.java' 2>/dev/null)
else
  FILES=()
  for arg in "$@"; do
    if [ -d "$arg" ]; then mapfile -t -O "${#FILES[@]}" FILES < <(find "$arg" -name '*.java'); else FILES+=("$arg"); fi
  done
fi
echo "javac: ${#FILES[@]} files -> $OUT" >&2
exec javac -proc:none --release 21 -encoding UTF-8 -Xlint:-options -implicit:class -d "$OUT" \
  -cp "$(cat "$CPFILE")" -sourcepath "$SRCPATH" "${FILES[@]}"
