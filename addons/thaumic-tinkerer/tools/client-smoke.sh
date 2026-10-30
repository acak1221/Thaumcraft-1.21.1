#!/usr/bin/env bash
# Runs the Thaumic Tinkerer client smoke: a real Minecraft 1.21.1 client with Thaumcraft and the add-on, under Xvfb with Mesa
# software rendering, driven by the harness in addons/thaumic-tinkerer/fabric/src/clientSmoke (docs/CLIENT_VERIFICATION.md).
#
#   addons/thaumic-tinkerer/tools/client-smoke.sh [-l GRADLE_LOG] [-t TIMEOUT_SECONDS] [-- EXTRA_GRADLE_ARGS...]
#
# Results go to fabric/build/tinkerer-client-smoke/: report.json, screenshots/*.png, client-log-excerpt.txt, plus gradle.log and
# client-latest.log (copies). The exit code is 0 only when the harness itself completed; what the checks found is in report.json.
# Gradle runs through the shared wrapper /root/bin/gradle-mirror-loop.sh, which takes the shared lock itself: do not wrap this
# script in flock. Never run it next to another Gradle build you care about: it holds the lock for the whole run (some minutes).
set -uo pipefail
ADDON="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT="$(cd "$ADDON/../.." && pwd)"
OUT="$ROOT/fabric/build/tinkerer-client-smoke"
RUN="$ROOT/fabric/run-tinkerer-clientsmoke"
LOG="${TMPDIR:-/tmp}/tt-client-smoke-gradle.log"
TIMEOUT=1500
while getopts "l:t:" opt; do
  case "$opt" in l) LOG="$OPTARG";; t) TIMEOUT="$OPTARG";; *) exit 2;; esac
done
shift $((OPTIND-1))
[ "${1:-}" = "--" ] && shift

for tool in xvfb-run /root/bin/gradle-mirror-loop.sh; do
  command -v "$tool" >/dev/null 2>&1 || [ -x "$tool" ] || { echo "client-smoke: $tool not found" >&2; exit 2; }
done

# Mesa software rendering: no GPU in this machine. llvmpipe offers OpenGL 4.5, Minecraft 1.21.1 needs 3.2.
export LIBGL_ALWAYS_SOFTWARE=1
export GALLIUM_DRIVER=llvmpipe
export LP_NUM_THREADS="${LP_NUM_THREADS:-$(nproc)}"

echo "client-smoke: gradle log $LOG, results $OUT" >&2
cd "$ROOT" || exit 2
xvfb-run -a -s "-screen 0 1280x720x24 -nolisten tcp" \
  /root/bin/gradle-mirror-loop.sh "$LOG" :fabric:runTinkererClientSmoke -PtinkererClientSmokeTimeout="$TIMEOUT" "$@"
status=$?

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
