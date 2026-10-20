# Arcane Spa, purifying fluids, Bath Salts, Soap and Crimson Rites

Active Fabric implementation; focused native Soap/Pure replay passed; full checkpoint pending. No inspected-client,
full survival/multiplayer or NeoForge acceptance is claimed.

The specification is the immutable TC4 4.2.3.5 TileSpa, ContainerSpa, GuiSpa,
ItemBathSalts, BlockFluidPure, ItemSanitySoap, ItemEldritchObject1,
EventHandlerEntity.itemExpire, BlockStoneDevice12, ConfigRecipes179–180/ArcaneSpa,
ConfigResearch and ConfigAspects449. Actual Forge1.7.10.1614 FluidTank,
FluidStack, FluidContainerRegistry and BlockFluidClassic are audited alongside
vanilla1.7 caller bytecode. Original and donor JARs were not modified or committed.

## Functional unit

- Actual registered source/flowing Pure fluids, LiquidBlock, block-scheduled
  Classic five-tick flow and Fabric renderer; no decorative-water substitute.
  The separate purifying-fluid audit records physical, eye and visual heights,
  native water-material compatibility and original density/displacement rules.
- Dropped whole Bath Salts stacks expire at200 updates: only an actual stationary
  vanilla water cell becomes one Pure source. Pure SOURCE grants absent Warp
  Ward and is removed; flowing fluid does not. Duration is min(32000,
  200000/max(1,floor(sqrt(permanentWarp)))). No bucket identity is invented here;
  original Pure bucket is now implemented in the same group; Death bucket remains pending.
- Arcane Spa has one salt slot and5000mB. Initial work is immediate, then every
 40 updates while unpowered. It pays1000mB+one salt for mixing; nonmix places an
  actual placeable tank fluid without salt. Center-first, then original X/Z5x5
  source-adjacency order, original reciprocal-quanta normalization, original
  rejected-placement payment and ultrawarm water exception are preserved.
- Original partial filled-container acceptance consumes the WHOLE container,
  including creative, and spills overflow.4500+1000 becomes5000mB. The actual
  Forge registered WATER POTION amount is1000mB, not the Crucible's separate250mB
  adapter. Empty buckets open the real GUI; direct draining is not invented.
- Native Fabric fluid automation uses81000droplets per bucket and full fluid
  component patches; common uses Architectury native units. UP advertises no
  capability. Nested savepoints and outer abort/commit use real transactions.
- Original menu has one salt plus36 inventory slots, reverse quick-move,
  button1 toggles then returns false. GUI preserves original white fluid icon,
  mirrored U coordinates,48px fill/55px gauge/8px toggle. Pure bubbles use age0
  and ALPHA.25, not quarter scale (actual func_82338_g writes particleAlpha).
- Soap uses the actual held-item release path after strict elapsed>195,
  spends one even in creative, and takes one WORLD float even at zero warp.
  Chance .33F plus.25F each for Ward and any actual Pure fluid level; sticky
  decreases at most1, temporary clears, permanent stays. Original float rounding,
  counter updates, ordered notifications and local lather are retained.
- Reusable Crimson Rites learn only CRIMSON once, no prerequisite/item payment;
  original warp3 splits permanent2/sticky1 unless Wuss. Actual original recipe
  exists only for Soap; Rites acquisition awaits actual Cultist rare loot.
  Original row449 tags are cognitio5/praecantatio3/alienis3/spiritus3.

## Authority and native adaptation

Spa work, inventory/menu and fluid mutations require the current native owner.
Fluid leases block ordinary work, item mutation and menu bypass while native
snapshots own the tentative tank. Saves encode committed initial tank state.
A real same-owner load while an open transfer has extracted fluid could restore
that committed snapshot beside an already funded destination. It is explicitly
refused BEFORE super.loadAdditional: native FINAL loadWithComponents would still
assign components after a plain return. The exact IllegalStateException is
"Cannot reload a live spa during a fluid transaction". Existing lease remains
valid for commit/abort; detached cold loads and ordinary chunk reload remain.
The runtime vector uses TWO real Spa tanks and actual transfer participants,
not a fluid-policy imitation. No general deferred reload infrastructure exists.

Paid world-placement intent is bounded to the Spa footprint and exact real
source state; ingredient debit precedes callbacks, intent retires before world
release, malformed receipts are retained inert. Block removal destroys remaining
tank fluid by original design and retires salt items through existing durable
DeferredItemDrops. This is not atomic cross-chunk filesystem crash recovery.

## Verification

47 owned files plus shared bootstraps, real recipe/book/icon/language bindings,
actual Fabric automation and8 new wrappers are active. Required Fabric total270.
All main/Fabric/gameTest/clientTest classes compiled;932JUnit PASS,84Python PASS,
resources0missing (2745texture refs/1142local model refs). The historical ALL90
aspect digest remains unchanged; new Rites row is checked independently.
Logs: .tmp/spa-soap-integration-compile-3.log, .tmp/spa-soap-python-1.log,
.tmp/spa-soap-assets-1.log. Current fresh-world native run is
.tmp/spa-soap-native-1.log; results are not accepted before completion.

Core parent827a0b3 GitHub run39432156806 had261/262PASS; all six previous Taint,
Hungry and biome failures passed. Remaining lamp fixture purged actual ordinary
lights immediately before observing them, racing asynchronous light decreases;
the redundant purge is removed without changing production/RNG/ticker assertions.
Its next replay is included in the270 current cases. Last fully accepted server
baseline remains b1e49f2 until a complete source-bound build is accepted.

## First native replay and dependency correction

Fresh270-case replay completed266PASS/4FAIL. Lamp and all previous262 cases
passed; four failures were in new fluid patches, water-material feeding and
Soap feedback/RNG diagnostics. No acceptance is promoted.

Architectury13.0.11 Fabric shipped FluidStackImpl.Pair.getPatch has its empty
predicate inverted: NONEMPTY fluid returns DataComponentPatch.EMPTY, while
empty fluid returns the stale map. This loses identity in equality, copy,
copyWithAmount, codec saves and native transfer, not only Spa mixing. A narrow
Fabric-only mixin corrects this single proven predicate with the actual native
component map. Binary Pair.class SHA256:
1473e0668681b72a466f224d3807ed8c394b6d1f7a0681d01cdc6bec9c405ac1.
New native checks retain full patches across copy, depletion, real NBT codec,
registry-aware native network codec and actual source/destination transfers.
No vendor fork or weaker equality rule is used.

Native FlowingFluid also asks neighbor.isSame(consumer), opposite the existing
WaterFluid.isSame(Pure) adapter direction. A scoped consumer-local mixin handles
only native WaterFluid consuming a Pure neighbor. Classic Pure identity stays
exact-family. Real source/flowing/above-feed and reverse-refusal checks cover
the asymmetry; every unrelated comparison delegates unchanged.

Soap's first native cleansing/payment/counter assertion passed; the following
combined feedback/RNG assertion is split for exact native diagnostics. Its
production behavior is unchanged pending evidence from the next replay.

## Final native diagnosis and actual Pure bucket integration

Focused native2 completed2/2PASS after removing all temporary RNG tracing.
The proven full-player-frame order is one Soap nextFloat, two native
Player.playShoulderEntityAmbientSound nextInt200 calls for actually EMPTY tags,
then the fixture nextFloat. The exact seeded sequence detects extra Soap calls;
physical payment, original thresholds, all warp channels, packet ordering,
creative and zero-warp assertions are retained. Production Soap was unchanged.
Full replay4 Pure assertions already passed including explicit Ward SOURCE debit
and actual whole-stack200tick salt expiry; focused2 repeats those successfully.
Logs: .tmp/spa-soap-focused-{1,2}.log (first1/2FAIL; second2/2PASS).

PureBucket adds actual ItemBucketPure plus the narrowly scoped removed original
FillBucketEvent for empty buckets. ConfigItems487–490 and EventHandlerWorld208–225
prove original bucket registration/source-only capture. Source debit is explicit
AIR; flowing1..7 stays intact. Creative empty-bucket capture removes the source
but keeps its EMPTY container, with no invented filled inventory reward.
The filled bucket always offsets to the hit face, preserves the full creative
container, returns plain survival BUCKET and has no Nether evaporation.
Paid full-inventory return reuses durable DeferredItemDrops with original player
throw4 world floats/pickupDelay40, without PechDrop. No recipe/dispenser/death
bucket is invented. Native Fabric bucket/tank transactions preserve components
by the new transfer API convention; old world/Spa actions retain plain templates.
Required wrapper total272. Bucket actual-target compilation/native runs pending.

The test-only -PgameTestFilter selects exact registered methods and writes a
separate focused-results XML. Default full CI retains all272 required wrappers;
focused diagnostics cannot satisfy full-manifest/freshness acceptance. This
reduces local failure diagnosis time without expanding Bore smoke infrastructure.

Crystal replay4 failed its existing real unload540tick assertion; only diagnostics
were added before cleanup (phase, holder/BE identity, entity readiness, ticket
status, elapsed wall time). Assertions, time limit, tickets and actual disk reload
are unchanged. Earlier local1–3 and GitHub827a0b3 passed that case. Full next CI
must prove the checkpoint on its exact inputs.

## Bucket actual-target verification before CI

All Fabric source sets compile,932JUnit and84Python pass, asset scan missing0
(2746texture/1142model refs). Focused12 native replay:11PASS/1FAIL. All eight
Spa/Pure/Soap/Rites cases plus actual bucket world/Nether/Spa custody, Lamp and
actual Crystal unload pass. Only the new transfer fixture's claim that
FluidVariant.of(flowing) remains a distinct flowing identity failed.

Shipped Fabric Transfer5.4.4 public FluidVariant Javadoc explicitly normalizes
flowing/still variants; FluidVariantImpl.of calls FlowingFluid.getSource before
cached lookup. The test now proves that canonical identity, refuses partial
physical-container insertion, then moves the real tank unit through this
normalized variant with unchanged abort/commit conservation assertions.
No production workaround was added. Strict actual WORLD flowing1..7 pickup
refusal remains independently asserted and passed. Final classes compile and
full272 default GitHub native replay are required for this correction.
Logs: .tmp/spa-pure-bucket-{compile-1,focused-1,final-compile}.log.
