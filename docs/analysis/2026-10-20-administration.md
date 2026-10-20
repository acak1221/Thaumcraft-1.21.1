# Original administrator commands — 2026-10-20

Specification: original TC4 CommandThaumcraft (private oracle), ResearchManager.completeResearch,
PlayerKnowledge.addAspectPool and direct warp setters. These are privileged server actions,
registered through the native Brigadier dispatcher. `/thaumcraft`, `/thaum` and `/tc` share the
same permission checks; existing status/aspects diagnostics and self-grant spelling remain available.

Research supports original list / player all / player reset / player key. A single-key grant marks
that key first, then recursively visits ordered visible parents, hidden parents and siblings. An already
known key stops recursion, including the original case where its missing parents stay missing.
All grants use the enabled original category/HashMap registration order and add each research's
Wuss-gated warp only once. Reset clears completed research/clues, restores enabled AutoUnlock,
and retains aspect pools, scans and existing warp. No research point, paper or ink charge applies
to an administrator grant.

Aspect commands parse an int >=1 and then retain the original signed-short narrowing. In particular
32768 is -32768, 65535 is -1 and 65536 is zero; overdraw preserves the original positive pool when
AspectList.reduce would refuse it. Case-insensitive aspect-name fallback is retained. Warp add
accepts -100..100, set accepts nonnegative int, PERM/TEMP select their channels and all other/missing
type strings select sticky warp. Direct command mutations do not reset warpCounter. Native knowledge
snapshots synchronize the target after each accepted action.

Native adaptation: selectors and player lookup use Brigadier's current rules; malformed syntax is a
native command error rather than the old hand-written usage message. The original delta-specific
PacketWarpMessage popup/whispers and the shared client notification queue remain a separate pending
client unit; command state synchronization is implemented. An online player named grant takes precedence over the existing self-grant spelling, including console use.
Native literals use current case-sensitive Brigadier parsing; mixed-case action spellings and ignored trailing
warp tokens from the original hand parser remain explicit command-parser parity gaps.
The old self-grant alias now uses the real
recursive original research operation. Public API breadth and arbitrary addon research registration
remain separate pending work.

Four pure scenarios cover recursive/known-key behavior, warp idempotence/reset preservation, signed
aspect amounts and direct warp counter/clamping. The native GameTest executes the actual registered
command tree, aliases, @s resolution, native knowledge mutation, range rejection and permission
refusal. Validation of this new source window is pending the next integrated compile/CI; prior green
commits do not cover it.
