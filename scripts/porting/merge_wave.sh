#!/usr/bin/env bash
# Root-only integration helper for the 2026-10-20 parallel content wave.
#
# Usage:
#   ./scripts/porting/merge_wave.sh --rebase <branch> [<branch> ...]
#       Rebase each port/<branch> onto origin/work, then merge it into work.
#       Conflicts are LEFT UNRESOLVED on purpose and reported, so the root
#       can resolve them deliberately (never auto-resolved).
#   ./scripts/porting/merge_wave.sh --list
#       Show the current state of every port branch.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

MODE="${1:-}"
shift || true

git fetch origin work || exit 1
# The agent branches are cut from the LOCAL work head, so they must be rebased onto
# the LOCAL work head as well: rebasing onto origin/work would replay every already
# merged wave again.
BASE="$(git rev-parse work)"
echo "work = $BASE ($(git log -1 --format=%s work))"
echo "origin/work = $(git rev-parse origin/work) ($(git log -1 --format=%s origin/work))"

if [ "$MODE" = "--list" ]; then
  for b in $(git for-each-ref --format='%(refname:short)' refs/heads/port/); do
    n="${b#port/}"
    behind=$(git rev-list --count "$BASE..$b" 2>/dev/null || echo 0)
    ahead=$(git rev-list --count "$b..$BASE" 2>/dev/null || echo 0)
    files=$(git diff --name-only "$BASE...$b" 2>/dev/null | wc -l)
    printf "%-16s ahead=%-4s behind=%-4s files=%-4s %s\n" "$n" "$behind" "$ahead" "$files" \
      "$(git log -1 --format=%s "$b" 2>/dev/null)"
  done
  exit 0
fi

[ "$MODE" = "--rebase" ] || { echo "usage: $0 --rebase <branch>..."; exit 2; }

for n in "$@"; do
  b="port/$n"
  git show-ref --verify --quiet "refs/heads/$b" || { echo "!! missing branch $b"; continue; }
  # A branch that is checked out in a worktree must be rebased inside that worktree.
  WT="$(git worktree list --porcelain | awk -v br="refs/heads/$b" '
        $1=="worktree"{w=$2} $1=="branch" && $2==br {print w}')"
  echo "=== rebasing $b onto work (worktree: ${WT:-none}) ==="
  if [ -n "$WT" ]; then
    ( cd "$WT" && git rebase "$BASE" ) || {
      echo "!!! CONFLICT rebasing $b -- resolve inside $WT, then: git rebase --continue / --abort"; exit 1; }
  else
    git checkout -q "$b"
    git rebase "$BASE" || { echo "!!! CONFLICT rebasing $b"; exit 1; }
    git checkout -q work
  fi
  echo "=== merging $b into work ==="
  git checkout -q work
  if git merge --no-ff --no-edit "$b"; then
    echo "--- merged $b"
  else
    echo "!!! CONFLICT merging $b"
    git status --short | sed -n '1,60p'
    exit 1
  fi
done
echo "=== work is now ==="
git log --oneline -1
