#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Runs the Python tools of scripts/porting (tt_assets.py, tt_oracle.py and their tests) against this repository.
#
#   tools/porting.sh test                      the unit tests of both tools
#   tools/porting.sh tt_oracle.py [ARGS...]    e.g. --check --source PATH_TO_THE_1.7.10_CHECKOUT
#   tools/porting.sh tt_assets.py [ARGS...]    e.g. --check --source PATH_TO_THE_1.7.10_CHECKOUT
#
# The tools come from the Thaumcraft 4 Reload monorepo, where the add-on is addons/thaumic-tinkerer and the Thaumcraft sources are in the
# same tree: they find the add-on at <root>/addons/thaumic-tinkerer and (tt_oracle.py) Thaumcraft's thaumcraft.api sources at <root>/common.
# This script builds that layout in a temporary directory (copies of the tools, a link to this repository as addons/thaumic-tinkerer, a link to
# the common/ module of a Thaumcraft checkout if there is one) so that the tools run unchanged and write their output into this repository.
#   THAUMCRAFT_DIR   the Thaumcraft checkout (default ../Thaumcraft-1.21.1, the one tools/publish-thaumcraft.sh makes); only tt_oracle.py needs it
#   TT_1710_SOURCE   the pinned checkout of the original 1.7.10 mod (see NOTICE.md; default: none)
# The tools hard-code a path of the machine they were written on (DEFAULT_SOURCE in tt_oracle.py and tt_assets.py). With TT_1710_SOURCE set, this script
# writes that checkout there in its temporary copies of the tools, so `test` replays the extraction against it and --source may be left out. Without
# it, the tests against the real checkout run only if the hard-coded path happens to exist, otherwise they are skipped; --source still works. The tests
# against the real checkout also need THAUMCRAFT_DIR.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
THAUMCRAFT_DIR="${THAUMCRAFT_DIR:-$ROOT/../Thaumcraft-1.21.1}"
[ "$#" -ge 1 ] || { sed -n '3,$ { /^#/!q; s/^# \{0,1\}//; p }' "${BASH_SOURCE[0]}" >&2; exit 2; }
command -v python3 >/dev/null 2>&1 || { echo "porting: python3 not found" >&2; exit 2; }
[ -d "$ROOT/scripts/porting" ] || { echo "porting: $ROOT/scripts/porting not found" >&2; exit 2; }

TMP="$(mktemp -d -t tt-porting.XXXXXX)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/scripts/porting" "$TMP/addons"
cp "$ROOT"/scripts/porting/*.py "$TMP/scripts/porting/"
# The provenance map is written by tt_assets.py: a link, so that the real file is the one updated.
ln -s "$ROOT/scripts/porting/tt_assets_map.json" "$TMP/scripts/porting/tt_assets_map.json"
if [ -n "${TT_1710_SOURCE:-}" ]; then
  [ -d "$TT_1710_SOURCE" ] || { echo "porting: TT_1710_SOURCE=$TT_1710_SOURCE is not a directory" >&2; exit 2; }
  python3 -I - "$TMP/scripts/porting" "$(cd "$TT_1710_SOURCE" && pwd)" <<'PY'
import pathlib, re, sys
folder, source = pathlib.Path(sys.argv[1]), sys.argv[2]
for name in ("tt_assets.py", "tt_oracle.py"):
    path = folder / name
    text, count = re.subn(r"^DEFAULT_SOURCE = Path\(.*\)$", lambda m: "DEFAULT_SOURCE = Path(%r)" % source, path.read_text(encoding="utf-8"), flags=re.M)
    if count != 1:
        sys.exit("porting: DEFAULT_SOURCE not found in %s (the tools changed?)" % name)
    path.write_text(text, encoding="utf-8")
PY
fi
ln -s "$ROOT" "$TMP/addons/thaumic-tinkerer"
[ -d "$THAUMCRAFT_DIR/common/src/main/java/thaumcraft/api" ] && ln -s "$(cd "$THAUMCRAFT_DIR" && pwd)/common" "$TMP/common"

cd "$TMP"
case "$1" in
  test) shift; python3 -I -m unittest discover -s scripts/porting -p "test_tt_*.py" "$@" ;;
  tt_assets.py|tt_oracle.py) tool="$1"; shift; python3 -I "scripts/porting/$tool" "$@" ;;
  *) echo "porting: unknown command $1 (test, tt_assets.py or tt_oracle.py)" >&2; exit 2 ;;
esac
