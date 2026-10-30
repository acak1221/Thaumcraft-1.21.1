#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Runs the Thaumic Tinkerer client smoke: a real Minecraft 1.21.1 client with Thaumcraft and the add-on, under Xvfb with Mesa
# software rendering, driven by the harness in fabric/src/clientSmoke of the add-on (docs/CLIENT_VERIFICATION.md).
#
#   tools/client-smoke.sh [-l GRADLE_LOG] [-t TIMEOUT_SECONDS] [-- EXTRA_GRADLE_ARGS...]
#
# Works in the standalone repository (the add-on is the Gradle root project, task runTinkererClientSmoke) and in the Thaumcraft
# monorepo (addons/thaumic-tinkerer, task :fabric:runTinkererClientSmoke); it finds out which by the settings.gradle next to tools/.
# Results go to build/tinkerer-client-smoke/ (monorepo: fabric/build/tinkerer-client-smoke/): report.json, screenshots/*.png,
# client-log-excerpt.txt, plus gradle.log and client-latest.log (copies). The exit code is 0 only when the harness itself completed;
# what the checks found is in report.json.
#
# Gradle is run as `./gradlew` in the project root. GRADLE_CMD replaces it: a command line (split at blanks) that is given the Gradle
# arguments last and whose output is tee'd to the log. If it contains the word {LOG}, that is replaced by the log path and nothing is
# tee'd (for a wrapper that writes the log itself: one that serializes Gradle builds, retries downloads, and so on), e.g.
#   GRADLE_CMD='my-gradle-wrapper.sh /path/to/project {LOG}' tools/client-smoke.sh
# Do not wrap the script in flock when the wrapper takes a lock itself; it holds it for the whole run (some minutes).
set -uo pipefail
ADDON="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [ -f "$ADDON/settings.gradle" ]; then      # standalone repository
  ROOT="$ADDON"; PROJECT=""; BUILD="$ROOT/build"
else                                          # monorepo: the add-on is wired into the :fabric project
  ROOT="$(cd "$ADDON/../.." && pwd)"; PROJECT=":fabric:"; BUILD="$ROOT/fabric/build"
fi
OUT="$BUILD/tinkerer-client-smoke"
RUN="$(dirname "$BUILD")/run-tinkerer-clientsmoke"
LOG="${TMPDIR:-/tmp}/tt-client-smoke-gradle.log"
TIMEOUT=1500
while getopts "l:t:" opt; do
  case "$opt" in l) LOG="$OPTARG";; t) TIMEOUT="$OPTARG";; *) exit 2;; esac
done
shift $((OPTIND-1))
[ "${1:-}" = "--" ] && shift

GRADLE_CMD="${GRADLE_CMD:-./gradlew}"
TEE=1
case "$GRADLE_CMD" in *'{LOG}'*) GRADLE_CMD="${GRADLE_CMD//\{LOG\}/$LOG}"; TEE=0;; esac
read -r -a GRADLE <<< "$GRADLE_CMD"
[ "$TEE" = 1 ] && GRADLE+=(--console=plain)
for tool in xvfb-run "${GRADLE[0]}"; do
  case "$tool" in ./*) [ -x "$ROOT/$tool" ] && continue;; esac
  command -v "$tool" >/dev/null 2>&1 || [ -x "$tool" ] || { echo "client-smoke: $tool not found" >&2; exit 2; }
done

# Mesa software rendering: no GPU in this machine. llvmpipe offers OpenGL 4.5, Minecraft 1.21.1 needs 3.2.
export LIBGL_ALWAYS_SOFTWARE=1
export GALLIUM_DRIVER=llvmpipe
export LP_NUM_THREADS="${LP_NUM_THREADS:-$(nproc)}"

echo "client-smoke: gradle log $LOG, results $OUT" >&2
cd "$ROOT" || exit 2
if [ "$TEE" = 1 ]; then
  xvfb-run -a -s "-screen 0 1280x720x24 -nolisten tcp" \
    "${GRADLE[@]}" "${PROJECT}runTinkererClientSmoke" -PtinkererClientSmokeTimeout="$TIMEOUT" "$@" 2>&1 | tee "$LOG"
  status=${PIPESTATUS[0]}
else
  xvfb-run -a -s "-screen 0 1280x720x24 -nolisten tcp" \
    "${GRADLE[@]}" "${PROJECT}runTinkererClientSmoke" -PtinkererClientSmokeTimeout="$TIMEOUT" "$@"
  status=$?
fi

# The Gradle task deletes $OUT first; keep the evidence next to the results.
mkdir -p "$OUT/screenshots"
cp -f "$LOG" "$OUT/gradle.log" 2>/dev/null
cp -f "$RUN/logs/latest.log" "$OUT/client-latest.log" 2>/dev/null
ls -t "$RUN"/crash-reports/*.txt >/dev/null 2>&1 && { mkdir -p "$OUT/crash-reports"; cp -f "$RUN"/crash-reports/*.txt "$OUT/crash-reports/" 2>/dev/null; }

# Without a report (the harness never started) there is still an excerpt of the log.
if [ ! -f "$OUT/client-log-excerpt.txt" ] && [ -f "$RUN/logs/latest.log" ]; then
  grep -E '/(WARN|ERROR|FATAL)\]' "$RUN/logs/latest.log" | grep -Ei 'thaumictinkerer|thaumic\.tinkerer|missing|unable|exception|failed' > "$OUT/client-log-excerpt.txt" || true
fi

if [ -f "$OUT/report.json" ]; then
  python3 -I - "$OUT/report.json" <<'PY'
import json, sys
report = json.load(open(sys.argv[1]))
summary = report.get("summary", {})
print("client-smoke: harness %s; checks %s" % (report["harness"].get("status"), {k: v for k, v in summary.items() if k != "failures_by_group"}))
for group, kinds in summary.get("failures_by_group", {}).items():
    print("client-smoke:   %-14s %s" % (group, dict(kinds)))
print("client-smoke: %d screenshots" % len(report.get("screenshots", [])))
PY
else
  echo "client-smoke: no report.json was written; see $OUT/gradle.log and $OUT/client-latest.log" >&2
fi
exit $status
