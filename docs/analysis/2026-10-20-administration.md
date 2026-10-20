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
native command error rather than the old hand-written usage message. Original delta-specific
PacketWarpMessage popup/whispers now use the real shared client notification queue; source and
consumer contracts are recorded in `2026-10-20-warp-notifications.md`, with target/runtime/client
verification tracked separately. An online player named grant takes precedence over the existing self-grant spelling, including console use.
Original case-insensitive help/research/aspect/warp and research LIST/warp ADD/SET spellings
now enter a narrowly scoped standard native Brigadier greedy-argument fallback. Only these recognized TC4
normalizations can execute the canonical tree with the same source and a private recursion guard;
unchanged invalid syntax never enters replay. No custom argument serializer is needed in the native
client command tree. Scoped fallbacks at research/warp handle the native
dispatcher's exact-literal preference, while help/list have their original ignored-tail children.
Selectors, including quoted selector values, use the native EntityArgument reader without rewriting
their contents. Help and research list ignore extras. Warp honors a type only with exactly five
original arguments; additional tokens select sticky warp and are ignored. Research/aspect retain
their exact arity, native target lookup, ranges and permission checks. Native runtime cases cover
both lowercase parent literals with mixed child actions and mixed whole actions, ignored malformed
unused tails, quoted selectors, unchanged state on permission/range/arity refusal and unknown actions.
The old self-grant alias now uses the real
recursive original research operation. Public API breadth and arbitrary addon research registration
remain separate pending work.

Four pure scenarios cover recursive/known-key behavior, warp idempotence/reset preservation, signed
aspect amounts and direct warp counter/clamping. The native GameTest executes the actual registered
command tree, aliases, @s resolution, native knowledge mutation, range rejection and permission
refusal. Validation of this new source window is pending the next integrated compile/CI; prior green
commits do not cover it.
