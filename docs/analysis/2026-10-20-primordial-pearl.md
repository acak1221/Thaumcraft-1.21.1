# Primordial Pearl — original node interaction

Source: private TC4 4.2.3.5 oracle `ItemEldritchObject:142–196`,
`TileNode:201–210,265–271`, original `AspectList.add/merge/reduce`.
Metadata3 becomes the native item `thaumcraft:primordial_pearl`, EPIC, stack1,
retaining `eldritch_object_3.png` and the two dark-purple original tooltip lines.
This is a real consuming node interaction, with no research gate: `PRIMNODE`
improves the random bounds only. Energized nodes are outside this item branch.

The first pass iterates CURRENT keys, including retained zero keys, and changes
BASE only. A compound's true coin requests BASE−1; a primal requests
`(short)(BASE−2+nextInt(researched?9:6))`. Original `setNodeVisBase` refuses a
negative target because `AspectList.reduce` refuses an overdraw: it does NOT
clamp to zero. A zero target retains the BASE key. The second pass visits all
six primals, draws `nextInt(researched?4:3)`, and if positive and above BASE sets
that BASE then calls original `addToContainer(aspect,1)`.

That refill has a subtle original effect: `left=max(0,1+CURRENT−BASE)` followed
by `CURRENT+=1−left`. When CURRENT is already over the new BASE, it REDUCES
CURRENT to BASE. For example, current3/base3 first becomes current3/base1, then
fallbackr2 produces current2/base2. Replacing it with a nonnegative room-clamped
increment would change gameplay.

Modifier upgrades form one else-if chain: FADING true coin→PALE, PALE true
coin→null, null nextInt5==0→BRIGHT. One use cannot perform multiple upgrades.
Node type, scan identity, natural status, tick/lock state and regeneration cache
are retained. The raw native mutation is owned by a private opaque TileNode
change, not a general public BASE setter.

Server item admission checks the real connected actor, exact held stack/reference,
count/components, current node, native interaction/build permissions, border and
loaded effect area. A private single-use native receipt consumes exactly one
physical pearl before assigning the preallocated current/BASE/modifier change.
No world query, callback, notification or allocation lies between that debit and
the raw assignment. The node rejects an unpaid or stale prepared token. Effects
follow committed payment; an exception/removal never refunds or retries it.
The original also consumes pearls in creative. Native game-mode creative count
restoration is handled by detaching the consumed argument's hand reference only
after raw node assignment.

Original `World.createExplosion(entity,x,y,z,strength,smoking=true)` means a
NON-FIRE, block-damaging explosion, centered `(x+.5,y+1.5,z+.5)`, strength
`3+nextFloat*(researched?3:5)`. It precedes all33 flux trials. Each trial consumes
six bound6 draws in x,x,y,y,z,z order. Air below node Y receives goo, other air
receives gas; metadata8 is NINE units in the existing finite-flux implementation.
The tail still runs if the explosion destroyed the source node; no source block
is recreated. Candidate writes revalidate exact air ownership around geometry
queries.

Forced authority adaptations: reject unavailable chunks before payment instead
of loading them for a blast (radius18 covers maximum unresisted ray reach<14
plus native neighbor reads), skip out-of-world/border targets, honor native
interaction permissions, and close exact debit/mutation ownership against
synchronous reentry. Modifier and original negative/zero/current-over-BASE quirks
are intentionally retained.

`PrimordialPearlRuntimeChecks.run` uses a real connected survival/creative actor,
registered item, native game-mode interaction and real native explosion inside
an isolated bedrock chamber. Explicit server INPUT fixtures provide nodes and
physical pearls. It reuses the existing scoped branch RNG adapter (restoring the
exact original instance), checks unresearched/researched draw prefixes, all198
flux offset draws, negative rejection, zero keys, fallback CURRENT reduction,
paid/unpaid/stale authority, creative consumption, actual NBT replacement,
actual blast packet/glass destruction and goo/gas metadata8. Implemented checks
are pending compilation/runtime until the root's next target run; no client
inspection is claimed.

Still separate and visible: original boss and rare-loot-bag survival acquisition,
advanced construct's recipe-specific retained pearl and full Advanced Alchemical
Furnace. The item is not a generic crafting-remainder catalyst; other original
uses such as Primal Crusher infusion consume it. Fixtures do not establish
survival acquisition or a finished full-port acceptance.
