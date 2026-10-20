# Purifying fluid — original Classic flow and native1.21 adapter

Specification: TC4 4.2.3.5 BlockFluidPure; Forge1.7.10.1614 BlockFluidClassic,
BlockFluidBase and RenderBlockFluid. The private oracle is outside the repository
under `/workspace/work/spa-forge-oracle`. Forge Entity.java.patch is pinned to
MinecraftForge/MinecraftForge commit9274e4fe435cb415099a8216c1b42235f185443e.
No original source/JAR is copied into this repository.

## Implemented rules

The real LiquidBlock has source metadata0 and flow metadata1..7, backed by two
registered native FlowingFluid identities. Classic has8quanta, density1000,
tick5, no infinite sources, downward metadata1, four horizontal directions in
W/E/N/S order and the original recursive cost limit4. That recursion can see a
hole5horizontal cells away; distance6 is outside its search. All1000 ties remain
optimal, even blocked directions, before flowIntoBlock's separate displacement
check. A starved flowing cell removes itself and still makes the original
subsequent downward metadata1 attempt. Upper self/W/E/N/S feeders give7quanta.

The native consumer uses BLOCK scheduled ticks. Original Classic had one block
identity across metadata changes; scheduling the two native fluid identities
would silently skip an old flowing tick after Spa replaced that cell with a
source. Native Fluid.tick delegates the same Classic consumer, without running
modern infinite-water propagation. The block retains original random ticks.

Displacement and lateral path search intentionally differ. Vanilla water and
lava were not Forge BlockFluidBase and therefore have unknown MAX density:
Classic can displace them downwards, while its lateral path search rejects their
water/lava materials. MaterialTaint is non-solid but blocks movement, so finite
flux/taint is not displaced just because its native collision is empty. Thin
pressure plates, eaten cake and open gates retain their old solid materials;
ladders and layered snow retain their old non-solid materials. Native inventory
stock and foreign BE replacements are preserved. A bounded seven-cell synchronous
frame prevents two reentrant Classic sources publishing the same plant drop.

Native vanilla water uses narrowly scoped asymmetric water-consumer adapters:
vanilla water recognizes both Pure identities because old vanilla compared
Material.WATER; Pure still recognizes only Pure because Classic compared its
exact block. Lava/other fluids are unchanged. This also lets an actual vanilla
flowing-water cell be fed by a Pure source and fill visually beneath Pure.
Native FlowingFluid asks both consumer.isSame(neighbor) and
neighbor.isSame(consumer). WaterFluid.isSame alone covers only the first. The
second form is adapted only inside native WaterFluid consumers: getNewLiquid
(horizontal and above), affectsFlow, isSolidFace, isWaterHole and source checks.
Pure.isSame remains unchanged. Native runtime vectors cover source, flowing3,
above and the negative reverse Classic consumer. This corrects the first actual
native run's reproducible water7 becoming AIR beside a preserved Pure source.

The original bucket is real: ThaumcraftSpa.BUCKET_PURE / ItemBucketPure, ID
thaumcraft:bucket_pure, native Fluid.getBucket returns it. Original ConfigItems
487–490 registers the item and1000mB FluidContainerRegistry pairing; original
FillBucketEvent208–225 consumes only metadata0. Native pickupBlock explicitly
sets SOURCE to AIR with UPDATE_ALL and returns the full bucket. Flowing1..7
returns EMPTY and is preserved. Original full-bucket placement always offsets
to the hit face, checks the target material/edit permission, preserves the full
bucket in creative and returns plain BUCKET in survival, without Nether
vaporization. No dispenser or new recipe is invented. This corrects the initial
unit contract's missed ConfigItems/EventHandlerWorld bucket registration.

## Three distinct height contracts

- Classic renderer: source.875, flowing(q/8)*.875, full1 beneath any liquid or
  old IFluidBlock. Its four corners use the exact original9-cell sampling and
  full-source pinning, rather than modern weighted-water averages.
- Forge physical immersion: Entity.isInsideOfMaterial called
  IFluidBlock.getFilledPercentage: min(1,(q+1)/8). Native FluidState uses source1,
  flow1=1, flow2=.875 through flow7=.25 for real water-tag eye/volume physics.
- Original source ray bounds are a full cube. Native SOURCE_ONLY ClipContext
  keeps that full source selection above its lower visual surface; flowing
  cells do not match the source-only predicate. Physical block collision is empty.

The native engine owns swimming, drowning and fluid pushing. Classic's normalized
flow vector is used directly; its old32x vector was normalized again by1.7 World
before applying acceleration. Native movement/animation remains the1.21 engine,
not a wholesale transplant of the old entity movement loop.

## Ward, salts and appearance

Only a source colliding with an actual server player without Ward gives ambient
Warp Ward then consumes that source with an explicit AIR/UPDATE_ALL write.
Native Level.removeBlock cannot be used: its getFluidState.createLegacyBlock
recreates the same SOURCE (proven by the first exact Ward native diagnostics).
Existing Ward preserves source/effect;
flowing collision grants nothing. There is no creative or wuss-mode exception.
Duration is min(32000,200000/max(1,floor(sqrt(permanentWarp)))). A bounded source
claim frame prevents effect-callback reentry and preserves foreign replacements.
Soap independently recognizes ANY actual Pure block; its flowing bonus case is
covered by the Eldritch item unit.

BathSaltTicks and its lifespan mixin belong to the shared Spa unit. The native
fixture observes real admitted ItemEntity ticks199/200: an entire named64 stack
expires on200 and converts exactly one stationary vanilla-water source. A real
flowing-water stack also expires but creates no Pure fluid. Item ages are never
assigned or manually advanced in this lifespan case.

Configured luminosity10 becomes source light8 because Classic subtracts one
extra quantum; metadata0..7 emits8,7,6,5,3,2,1,0. The world renderer uses original
fluidpure still/flow sprite and white tint, fixed1/.5/.8/.6 face shading,
0.001F offsets, rotating top UV, half-size side UV and double-sided top/sides.
Client animateTick invokes the shared real bubbleAlpha helper at age0/alpha.25/white,
then original1/25 lava-pop branch at blockY+maxY(1), with exact RNG order/volume/
pitch. GuiSpa separately uses the actual fluid still sprite drawn WHITE.

## Validation state

Five independent pure-rule JUnit methods were compiled and invoked with Java21
and the actual Jupiter assertions: PASS. They cover finite feeding, MAX-density
versus liquid-material gates, recursive fifth/sixth-cell boundary, source/reverse
routing and original visual/physical height/corner vectors.

PureFluidRuntimeChecks.run(GameTestHelper), timeout280, separate batch
`m14_spa_fluid`, includes actual native states, water-tag physics/eye boundaries,
SOURCE_ONLY ray, source bucket/flowing preservation, downward and finite flow, real vanilla
water fed by Pure, material exceptions, reentrant physical plant drop, source Ward
including creative/large-warp/existing-effect cases, scheduled5tick metadata
replacement, and whole-stack200tick source/flowing salt expiry. It temporarily
sets randomTickSpeed0 and restores it, with owned blocks/entities/chunks restored
on completion/failure. Its stated timing evidence is ordinary native world and
entity ticking, without direct lifetime callbacks or synthetic age assignment.

The unit is integrated; the first native run exposed the reverse native-water
comparison above. The corrected native fluid checks PASS in full replay4 and focused Soap/Pure2, including actual Ward source payment and ordinary200tick salts.
No client visual PASS or multiplayer acceptance is claimed by direct pure JUnit.

Native Fabric Transfer deliberately normalizes FluidVariant.of(flowing) to the
registered still/source identity before bucket lookup. The bucket transaction
fixture asserts that documented native identity and exact full-container
commit/abort conservation. This new automation API behavior does not relax
original world pickup: actual flowing metadata1..7 still refuses and survives.
