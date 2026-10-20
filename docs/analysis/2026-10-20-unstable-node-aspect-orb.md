# UNSTABLE ordinary nodes and Aspect Orbs

Gameplay authority is the private original Thaumcraft 4.2.3.5 oracle outside this
repository: `TileNode` stability branch (726–740), `EntityAspectOrb` (40–244),
`InventoryUtils` hotbar selection (398–409), `RenderAspectOrb`, and original
`ThaumcraftWorldGenerator.createRandomNodeAt` (218–399). Legacy repository
`src/` is not the authority.

Implemented on the Fabric-first shared target:

- Original wild-node roll now actually admits NORMAL and UNSTABLE. DARK, PURE
  and HUNGRY rolls still skip without replacement; no frequency/reroll change.
  Native initialization records the rolled type and replaces creative current
  and BASE stock with the actual generated spread. Non-Overworld generation,
  silverwood knots and the remaining special-node ecology are still pending.
- Existing ordinary discharge/recharge/pruning also ticks UNSTABLE. Every100
  ticks its own first Boolean gates the branch. An unlocked node draws among
  CURRENT primal keys including retained zero keys; positive draws debit ONE
  current unit only, leaving BASE and compounds unchanged. Empty primal lists
  deliberately no-op instead of original `nextInt(0)` crashing.
- A real basic stabilizer lock suppresses emission and applies the original
  `nextInt(10000/lock)==42` conversion to NORMAL. Separate FADING repair still
  runs afterwards. Neither type conversion nor pearl modifier changes reset
  the existing regeneration cache. Advanced stabilizer gameplay remains pending.
- Actual native MISC Aspect Orb entity, size.125, native tracking8chunks (original
 120blocks rounded to chunks), update20. Original Math.random initial scatter,
  gravity.03F, lava bounce, squared8-block player-eye attraction, hotbar-first
  wand choice, friction/bounce, health5/false damage return and TTL150 are kept.
  Whole value1 becomes100centivis, never research points or XP. One centivis of
  capacity consumes the whole orb and wastes the other99 as original rounding.
  Inventory object/components survive; credited orbs close before notifications.
- The exact original16-frame/25ms particle strip, aspect color/blend, alpha128,
  asymmetric quad and lifetime shrinking have a native renderer. Shader/state
  integration uses existing VisNetRender. No inspected-client visual evidence
  exists for this new renderer; exact old offset/brightness appearance is not
  claimed. Modern entity feet/bounding-box positioning replaces old yOffset.
- Scanning delegates to the separately audited `NodeScanAspects` helper; original
  UNSTABLE Entropy MAX4 augments scan results without changing aura stock.

## Closed paid delivery

`AspectOrbQueue` is a dimension SavedData journal for this one-unit source only,
not a generic reward/spawn framework. It prepares the actual UUID, original
position/scatter/yaw and storage BEFORE debit. TileNode opens a private exact
completion scope; a public prepared token cannot spend arbitrary node aura.
The journal entry is installed before the callback-free source decrease.
Native ADD cancellation retains the paid record with retry20; chunk loading is
never forced by delivery. Original source removal does not cancel paid output.

The entity's native `setLevelCallback` hook records UUID/section admission before
onCreated callbacks. A spawn accepted and then removed/throwing cannot reissue
its paid UUID. Pending/settled SavedData and same-lifetime receipts cover normal
reload/reentry; another UUID owner and invalid or ambiguous rows are quarantined
with raw payloads, never silently converted. This does not claim atomic recovery
across arbitrary process termination between separate vanilla chunk/data writes.

## Primordial Pearl coordination

TileNode owns opaque `PrimalPearlChange` with private constructor. Preparation
stages independent current/BASE maps, all original current-key and six-primal
rolls, short narrowing, refused negative BASE targets and one modifier upgrade.
The original fallback `addToContainer` can reduce current above lowered BASE;
AIR current3 / BASE3→1 / fallback2 ends BASE2,current2. `matches` ends with local
state checks after native world checks. The paid ItemPrimordialPearl receipt is
required for raw assignment; no allocation/world callback/notification occurs
between physical payment and assignments. Item/effects/tests belong to the
coordinated Pearl unit and are described separately.

## Verification state

Five new JUnit rule methods and `UnstableNodeRuntimeChecks.run` are implemented.
The native scenario uses one fixed actual natural roll and a real timed scanner,
source zero/positive draws, unpaid-token refusal, NBT, canceled recursive ADD,
SavedData replacement, exact saved trajectory, source removal, one actual hotbar
credit/rounding/replay, malformed duplicate/nonfinite payload preservation,
actual basic stabilizer winning draw/cache retention, entity force at4/outside8,
physical entity NBT/quarantine, gravity, health and149/150tick expiry. Ordinary
registered ticking must also advance the natural node and deliver the paid orb.

These new tests have NOT yet run as of this source-ready note. Parent coordinator
owns the single Gradle build/runtime run and will record its exact result. The
previous620-JUnit/78-Python checkpoint predates this unit. Client inspection,
remote multiplayer and NeoForge parity remain unverified; this unit does not
close the complete M02 node family.
