# Actual TC4 biomes — compiled implementation, native world replay pending

Canonical public keys `thaumcraft.common.config.ThaumcraftBiomes`:
`MAGICAL_FOREST` (`thaumcraft:magical_forest`), `EERIE` (`thaumcraft:eerie`),
`TAINT` (`thaumcraft:taint`). ResourceKey<Biome>, actual native biome JSONs;
no legacy numeric registry identifiers.

Original-only behavior source: immutable TC4 4.2.3.5 BiomeGenMagicalForest,
BiomeGenEerie, BiomeGenTaint, Config.registerBiomes, ConfigEntities.initEntitySpawns,
ThaumcraftWorldGenerator.initialize, WorldGenBigMagicTree, WorldGenManaPods,
BlockCustomPlant. Dependency audits outside repo:
- Vanilla1.7.10 `axn/axf/ayc/ahu/aia` (GenLayer/Biome/Zoom/Base/Decorator).
- Official Forge1.7.10-10.13.4.1614 universal JAR, BiomeManager + EntityRegistry,
  SHA25600d1ca02192c7efb87da95552ebf247021f1e1d642f86d0a03076314def11529.
  GenLayerBiome Forge patch from official MinecraftForge1.7.10 branch.

## Registration / tags / spawn

MAGICAL_FOREST: MAGICAL+FOREST; EERIE: MAGICAL+SPOOKY; TAINT:
MAGICAL+WASTELAND. DO NOT add DEAD or substitute any vanilla biome.
Existing native ore_biome_types and node_biome_types tags carry the exact
Config.registerBiomes memberships. Existing magical_biomes, magical_tree_biomes,
normal_aura_node_biomes include actual TC4 IDs; silverwood exclusion retains
MagicalForest/Taint (own decorator supplies MagicalForest Silverwood).
BiomeTags.IS_OVERWORLD for real fresh Overworld biomes, but don't add vanilla
is_forest to Eerie/Taint or make vanilla forest magical.

Base1.7 MONSTER rows: spider100/4, zombie100/4, skeleton100/4, creeper100/4,
slime100/4, enderman10/1..4, witch5/1. CREATURE sheep12/4, pig10/4,
chicken10/4,cow8/4; WATER_CREATURE squid10/4; AMBIENT bat10/8.
MagicalForest retains base and adds wolf2/1..3,horse2/1..3,witch3/1,enderman3/1,
Pech10/1..2,Wisp10/1..2. Actual later Forge addSpawn(Pech10/1..1)
UPDATES first Pech row and ALWAYS APPENDS another row: final TWO separate
Pech10/group1 rows (effective weight20), not a deduplicated single10 row.
Eerie clears CREATURE then adds BAT3/1 in original CREATURE list; retains base
MONSTER and appends witch8/1,enderman4/1,Brainy32/1,Giant8/1,Wisp3/1,
Guardian1/1; later exactly one Pech10/1. BAT native category mismatch needs
explicit adapter preserving actual original table, not moving silently.
Taint clears CREATURE,MONSTER,WATER_CREATURE, retains AMBIENT bat10/8;
adds actual Taintacle1/group1 (root mob chain), later Pech10/1 (its entity spawn
predicate rejects Taint). No empty placeholder Taintacle row.

## Climate / native forced adapter proposal

Original climate insertion: MagicalForest5 + Taint2 in WARM and COOL.
Forge base WARM six10 rows (Forest,RoofedForest,ExtremeHills,Plains,
BirchForest,Swamp) =60; COOL four10 rows(Forest,ExtremeHills,Taiga,Plains)=40.
Modded list chooses nextInt(totalWeight), so totals67/47, not /10 multiples.
Eerie NEVER climate-inserted: actual DARK-node/totem ecology produces it.

Fabric1.21 has no public OverworldBiomes insertion API. Proposed narrow native
MultiNoiseBiomeSource adapter ONLY for actual OVERWORLD parameter-list preset:
- Preset holder constructor captures real biome holders from HolderGetter.
- collectPossibleBiomes includes these real holders (feature sorter/spawns).
- int getNoiseBiome RETURN overlays the custom winner on native eligible inland
  surface results, preserving ocean/river/beach/cave/mushroom terrain biomes.
- Climate.Sampler bridge records native worldseed at RandomState construction.
  No global mutable seeded random and no dependency on Fabric internal hooks.
- Use native OverworldBiomeBuilder five temperature bands, mapped to original
  ICY/COOL/WARM/WARM/DESERT, preserving actual1.21 climate surfaces. This mapping
  is a forced platform adapter, not identical1.7 temperature generation.
- At coarse256-block cells select original distinct vanilla weighted palette
  tokens plus5/2 TC4 tickets with original GenLayerBiome salt200 and LCG.
  Preserve distinct non-TC4 tokens through zoom! Collapsing them to one generic
  vanilla token before mode-voting would unfairly suppress custom islands.
- SIX exact old zooms:1000,1001 beforeHills;1000,1001,1002,1003 afterRareBiome.
  Native quart cells already represent4blocks, so256-block coarse scale, not64.
  Original selectRandom/selectModeOrRandom and all three cell draw ordering
  apply. Native BiomeManager supplies final native spatial jitter.
- Return unchanged delegate for noncustom winning tokens. Preserve1.21 density
  router/terrain/caves, not the full removed1.7 GenLayerHills/Rivers world.
  No old seed identity or old height(.2,.2) reproduction claim. Need root review
  of climate mapping and preserved special-biome gates before activation.
- Thread-local bounded primitive caches by sampler+seed, six zoom levels,
  ensure native worker concurrency and no recursive exponential sampling.

## Decoration / dependencies

MagicalForest exact counters: trees2,flowers10,grass12,waterlily6,mushrooms6.
Eerie trees2,flowers1,grass2; Taint trees2,flowers-999,grass2,reeds-999.
Original custom order MagicalForest: nextInt3 mossy cobble blobs;4x4 offset
bigmushroom grid (1/40); superDecorator;10 ManaPod generators fromY64;8
Vishrooms adjacent to actual wood logs after descending toward grass toY50.
Taint superDecorator, nextInt3 actual taint block blobs;10 surface fibres;
8 first-uncovered fibre attempts with actual biome column expansion.

BigMagicTree is separate OAK tree algorithm (.6618 height attenuation,.381
branch slope,11+privateNextInt12 height, three leaf layers). Decorator calls
setScale(1,1,1), so leafDensity=.8. Preserve original shared mutable chosen /
obstruction-shortened height across chunks: world-scoped weak-key HeightState
(int only), serialized temporary per-call generator. Original reload resets
height; no SavedData. Native parallel visitation order differs from1.7, but
height must not reset per chunk. Reuse MagicalTreePlacement after generic
LeavesBlock distance/leaves-last adaptation (CHECK_DECAY remains TC4-only).
Greatwood/Silverwood existing real generators reused, no duplicate core trees.
Greatwood draft now completes real CaveSpider spawner,50web attempts,actual
weighted38-entry descriptor chest and original eight overwrite-slot writes.
It uses the region/immutable-provider loot overload and PRIVATE tree RNG,
with worker-safe actual BEs; no live ServerLevel queries or shared WORLD RNG.
New source and runtime fixture compile; native runtime execution remains pending.
Silverwood PURE embedded node lifecycle next_content-owned, replaces old
entire-tree skip. Need both before full decoration acceptance.

Vishroom absent from actual plants (onlymetadata2/3 currently): new actual
block/item idvishroom, old .1..9 x/z bounds,height.8, no collision, light8,
plains-soil/light stay checks, exactcontact CONFUSION200/amp0 for living mobs,
ordinary self drops/fire spread60/flammability100. Client original FXWisp
purple .1/shrink/gravity.015 at1/3 animate gate; actual manashroom texture.

ManaPod generator: whileY<min(128,columnHeight), air andairbelow,
DOWNsupport+actualMAGICAL biome; AGE2+nextInt5 then actualgrowth to3..7 with
existing ordered adjacent crossbreed. Otherwise x/z eachinitial+int4-int4,
thenY++. Native WorldGenRegion typed BE generation requires region-safe
initialization; no main-thread ServerLevel.forceGet in worker generation.
OriginalTile checkGrowth consumes WORLD RNG; native parallel feature contexts
need an explicitly separated deterministic worldgen stream adapter while runtime
randomTick continues exact WORLD stream. Need preserve source distinction.

Native biome JSON supplies real colors/temp/rain, base carvers/ore/spring
features using native height/stone palette, with custom ordered decorator for
TC4 counters/trees/pods/Vishrooms/Taint fibres. Native1.21 ore heights and
vanilla geometry are declared terrain adapters, not1.7 world seed replay.
Full unit acceptance requires natural source lookup overrealfreshseed,
registered possiblebiomes+featureSorter, actual generatedtrees/typedpods,
Pech natural population+spawn gates, column packets/persistence, and inspected
nativeclientcolors/foliage/pods/Vishroom. Compile is not these results.

## Exact config consumers / immutable settings draft

BiomeSettings + ThaumcraftBiomeSettings (same original `thaumcraft.properties`):
- magical_forest_biome_weight=5, taint_biome_weight=2 are REAL startup consumers
  in original ThaumcraftWorldGenerator.initialize:93..96. Each policy captures
  the immutable settings value; cache matching includes it. Zero tickets
  exclude that biome; weights are never silently fixed to defaults.
- generate_trees=true is REAL at generateSurface:406; only the GLOBAL tree
  pass is gated. The biome decorators' own trees are NOT gated by this value.
  Global pass also requires dimension-blacklist==-1 and non-FlatLevelSource,
  replacing the old generator-name.startsWith("flat") check.
- generate_taint=true is read/stored but has NO consumer anywhere in original
  TC4 4.2.3.5. Preserve its ineffectiveness and document it, rather than
  inventing a disabling gate or using it instead of the real taint weight.
- blue_magical_forest=false is REAL local color getter behavior. Client-only
  BiomeColors grass/foliage adapters use canonical registry identity, exact
  green/blue constants and native getBlockTint blending; no research sync
  fields and no server/remote config overwrite.
- Invalid negative/overflow weights would produce an invalid old weighted
  pool. Native settings reject with an explicit load error/default snapshot;
  this is a documented configuration crash guard, never a silent clamp.

Root hooks needed on coherent activation:
1. Thaumcraft.init immediately after existing ThaumcraftSettings.load calls
   ThaumcraftBiomeSettings.load(the same config path), BEFORE world registries.
   Dedicated and local client both read their own startup file. Add discoverable
   defaults/comments to the existing shared config template; no second file.
2. Original biome keys/feature/plant registration ThaumcraftBiomes.register().
3. Common mixin JSON add Tc4BiomeParameterListMixin, Tc4ClimateSamplerMixin,
   Tc4RandomStateMixin, Tc4MultiNoiseBiomeSourceMixin; client entry
   client.Tc4BiomeColorsMixin. The source guard requires canonical keyed
   minecraft:overworld parameter-list holder, not merely a compatible preset.
4. ClientSetup calls ThaumcraftBiomeClient.init() after ResourceParticle setup.
5. Exact tag data joins for actual3 biomes; no vanilla magic tagging.

Separate existing-unit Config consumers for root status (not hidden in the
new record): generate_aura_nodes at original generator190/412;
generate_structures149/421; generate_cinnibar_ore495;
generate_amber_ore505; generate_infused_stone513. Existing current native
ore/node/structure units must wire these via their owners/shared restart
settings. Fresh-world-only scope excludes world regeneration/regen-key work.
Original spawn flags are also REAL constructor/global spawn consumers;
constructor-only native JSON rows plus isolated FabricBiomeSpawns implement
these switches and original global row order. New policy/adapter are drafts:
 spawn_angry_zombies,spawn_fire_bats,spawn_wisps,
spawn_taintacles,spawn_taint_spores,spawn_pechs,spawn_eldercreatures.
Root TaintSettings already owns spawn_taint_spores; do not duplicate it.


## Exact startup spawn adapter (original recheck, no residual-row assumption)

Original constructor guards are explicit in oracle: MagicalForest66/69
Pech/Wisp; Eerie35/39/42 Brainy+Giant/Wisp/Guardian; Taint52 Taintacle.
All are guarded; Guardian is controlled by spawn_eldercreatures.

Biome JSON contains ORIGINAL constructor rows only: forest Pech10/1..2 and
Wisp10/1..2; Eerie Brainy32/Giant8/Wisp3/Guardian1, Taint Taintacle1.
FabricBiomeSpawns runs one REPLACEMENTS transformation, copying the complete
actual MONSTER list, applying exact original constructor guards first, then
ConfigEntities.initEntitySpawns order Angry -> Pech -> Firebat -> Wisp:
- Angry10/1 only allowedBiomes with nonempty current MONSTER list.
- Pech10/1 only actual MAGICAL with nonempty current MONSTER list.
- Firebat10/1..2 in NETHER; Halloween adds5/1..2 in allowedBiomes.
- Wisp5/1 in NETHER.
Official Forge addSpawn updates FIRST matching row in place, then ALWAYS
appends another. List order and all other rows remain, no weighted deduplication.
False switches remove only the original first exact constructor row, preserving
independently declared native datapack/addon rows. Native datapack overrides
are an explicit modern extension, not old Forge biome-constructor replay.

Important dependent gate: disable Taintacles -> original Taint MONSTER list
empty -> global Pech skips it even if spawn_pechs=true. A genuine addon monster
restores that nonempty condition. No blanket entity-type removal/disable.

Forge WorldChunkManager.allowedBiomes defaults are seven originals: forest,
plains,taiga,taigaHills,forestHills,jungle,jungleHills. Existing native
brainy_zombie_spawns maps them to four current IDs after removed hill IDs
collapse; no TC4 biome enters this tag. Official patch cached outside repo
with the rest of the dependency audit. Seasonal Calendar is captured once at
startup, matching original initEntitySpawns. Spore setting remains owned by
root TaintSettings.

ThaumcraftBiomeSettings now atomically publishes one immutable startup
snapshot of BiomeSettings, OriginalSpawnSettings and OriginalWorldgenSettings.
Root existing Properties loader calls configure(Properties); no new file or
research packet. worldgen() provides the real node/structure/ore flags above.
Only the global natural normal-node feature consumes auraNodes: explicit
Silverwood/Primal/PURE node creators keep their original caller semantics.

## Concrete activation / validation boundary

Exact file map and root hooks are in activation-manifest.json. Stale
MagicalTreePlacement draft is EXCLUDED; next_content owns the single merged
knot-aware + generic LeavesBlock helper. ResourceParticle is an additive
vishroom method only, never replacement of the stale whole-file draft.

Three wrappers are proposed: source240ticks, trees/dungeon400ticks, fresh
forest natural1600ticks, separate batches. Natural test reuses the EXISTING
normal-noise test dimension, finds actual source/base-height forest without
loading chunks, requests25 genuinely fresh chunks, inspects real oak/magical
trees/Vishroom/typed pods, harvests a natural pod via connected survival
block-break, transfers its real delayed ItemEntities through native pickup,
and replants the actually owned reward with native RMB/exact one-bean cost.
It then invokes unmodified native weighted NaturalSpawner callback on actual
forest ground over512 bounded declared WORLD seeds: no forced Pech type,
construction/finalizeSpawn bypass, fake biome tags or test plant insertion.
This is native weighted callback coverage, not an ordinary multi-hour natural
population replay. All new code and fixtures compile; native runtime execution remains UNRUN.


Builtin-registry bootstrap guard (pre-activation correction):
MultiNoiseBiomeSourceParameterLists.bootstrap runs inside an explicit thread
local scope. ParameterList constructor captures no custom holders in that
scope, preventing RegistrySetBuilder.UniversalLookup from creating unknown
TC4 forward references before datapack biomes exist. Actual registry JSON
decoding outside builtin bootstrap captures real custom holders. Sources
with no captured custom holders remain untouched, even if canonical-keyed.
Native source regression explicitly rebuilds VanillaRegistries.createLookup,
checks successful vanilla-only result and closed guard, then independently
checks real runtime Overworld holders/source. Extra common mixin required:
Tc4BuiltinBiomeParameterListsMixin. No builtin asset/preset table mutation.


Activation checkpoint: own manifest activated after d17b966; shared root hooks
are integrated. Parent reports918 JUnit passing and261 native wrappers compiled
after this group. These are compile/unit results only: server runtime and visual
acceptance remain pending until the actual immutable CI/client runs.


Narrow existing ore consumer integration:
ThaumcraftOreFeature gates all three exact original passes independently:
18 cinnabar,20 amber,8 infused attempts. Disabled pass consumes no RNG draws.
Original dimension/biome blacklist levels0/2 reject before pass RNG; existing
native resource tag blacklist remains an additive registry adaptation.
Default flags leave attempt/draw order unchanged.

No actual native mound/hilltop-stones/totem/Eldritch-ring/maze structure
generator exists in current common/Fabric sources (hidden/no-ignore search
confirmed). generate_structures is parsed but has no native consumer yet;
this remains explicitly MISSING rather than adding a fictitious guard or
claiming original structures are implemented. Original surface structures
require dimensionBlacklist==-1, genStructure, dimension0, nonflat, freshworld;
Nether totem and Outer branches have distinct original guards, so the future
unit must inspect each caller rather than applying one universal switch.


Current validation evidence: parent completed common/Fabric production and
all native source-set compilation,918 JUnit PASS,261 GameTest wrappers
compiled. Biome policy/settings/ordered-spawn unit tests are part of that
run. Actual new biome server GameTests are pending immutable GitHub replay;
all biome client visuals remain uninspected. An extra differently keyed
compatible-preset negative fixture is queued only in .tmp for a later
CI-fix window and is NOT part of this compiled snapshot.
