#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Builds Thaumcraft 4 Reload at the commit this add-on needs and installs it into the local Maven repository (~/.m2), where
# build.gradle looks for thaumcraft:Thaumcraft-fabric:<thaumcraft_version>.
#
#   tools/publish-thaumcraft.sh [-b REF] [-d DIR] [-u URL] [-- EXTRA_GRADLE_ARGS...]
#
#   -b REF     branch, tag or commit of the Thaumcraft repository   (default: $THAUMCRAFT_REF or thaumcraft_ref of gradle.properties, a commit)
#   -d DIR     where the checkout is (made if absent)               (default: $THAUMCRAFT_DIR or ../Thaumcraft-1.21.1, next to this repository)
#   -u URL     the repository to clone                              (default: $THAUMCRAFT_URL or https://github.com/acak1221/Thaumcraft-1.21.1)
#
# The add-on needs extensions of thaumcraft.api that exist only on the branch claude/hopeful-hamilton-v8m5pi; a Thaumcraft from another
# branch does not compile it. The default ref is a commit of that branch, so the result does not change when the branch moves; to follow the
# branch, pass -b claude/hopeful-hamilton-v8m5pi. An existing checkout is fetched and moved to the ref; it is refused when it has local changes.
# Gradle is ./gradlew of the checkout unless GRADLE_CMD says otherwise (a command, e.g. GRADLE_CMD=./gradlew). The build of Thaumcraft takes a
# few minutes the first time.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PINNED="$(sed -n 's/^thaumcraft_ref=//p' "$HERE/gradle.properties" 2>/dev/null | head -n1 | tr -d '\r')"
BRANCH="${THAUMCRAFT_REF:-${THAUMCRAFT_BRANCH:-$PINNED}}"
[ -n "$BRANCH" ] || { echo "publish-thaumcraft: no ref: thaumcraft_ref is missing in gradle.properties; pass -b REF" >&2; exit 2; }
DIR="${THAUMCRAFT_DIR:-$HERE/../Thaumcraft-1.21.1}"
URL="${THAUMCRAFT_URL:-https://github.com/acak1221/Thaumcraft-1.21.1}"
while getopts "b:d:u:" opt; do
  case "$opt" in b) BRANCH="$OPTARG";; d) DIR="$OPTARG";; u) URL="$OPTARG";; *) exit 2;; esac
done
shift $((OPTIND-1))
[ "${1:-}" = "--" ] && shift
command -v git >/dev/null 2>&1 || { echo "publish-thaumcraft: git not found" >&2; exit 2; }

if [ -d "$DIR/.git" ]; then
  if [ -n "$(git -C "$DIR" status --porcelain)" ]; then
    echo "publish-thaumcraft: $DIR has local changes; commit or stash them, or use another -d" >&2; exit 1
  fi
  echo "publish-thaumcraft: updating $DIR" >&2
  git -C "$DIR" fetch --tags --prune origin
elif [ -e "$DIR" ] && [ -n "$(ls -A "$DIR" 2>/dev/null)" ]; then
  echo "publish-thaumcraft: $DIR exists and is not a git checkout" >&2; exit 1
else
  echo "publish-thaumcraft: cloning $URL into $DIR" >&2
  git clone "$URL" "$DIR"
fi

if git -C "$DIR" show-ref --verify --quiet "refs/remotes/origin/$BRANCH"; then
  git -C "$DIR" checkout "$BRANCH"
  git -C "$DIR" merge --ff-only "origin/$BRANCH"
else
  git -C "$DIR" rev-parse --verify --quiet "$BRANCH^{commit}" >/dev/null \
    || { echo "publish-thaumcraft: $BRANCH is no branch, tag or commit of $DIR" >&2; exit 1; }
  git -C "$DIR" checkout --detach "$BRANCH"   # a tag or a commit
fi
echo "publish-thaumcraft: Thaumcraft at $(git -C "$DIR" rev-parse --short HEAD) ($BRANCH)" >&2

GRADLE_CMD="${GRADLE_CMD:-./gradlew}"
cd "$DIR"
# shellcheck disable=SC2086  # GRADLE_CMD is a command line on purpose
$GRADLE_CMD :fabric:publishToMavenLocal --console=plain "$@"

VERSION="$(sed -n 's/^mod_version=//p' "$DIR/gradle.properties" | head -n1 | tr -d '\r')"
WANT="$(sed -n 's/^thaumcraft_version=//p' "$HERE/gradle.properties" | head -n1 | tr -d '\r')"
JAR="${HOME}/.m2/repository/thaumcraft/Thaumcraft-fabric/$VERSION/Thaumcraft-fabric-$VERSION.jar"
[ -f "$JAR" ] || { echo "publish-thaumcraft: $JAR was not written; look for the error above (a custom maven.repo.local would put it elsewhere)" >&2; exit 1; }
echo "publish-thaumcraft: installed thaumcraft:Thaumcraft-fabric:$VERSION ($JAR)" >&2
if [ -n "$WANT" ] && [ "$WANT" != "$VERSION" ]; then
  echo "publish-thaumcraft: WARNING: gradle.properties of this repository asks for thaumcraft_version=$WANT, the checkout built $VERSION; set thaumcraft_version=$VERSION" >&2
  exit 1
fi
