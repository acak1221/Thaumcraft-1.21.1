# Merged-code quality audit — a2quality

Input: `c6feeda7`, branch `agent/a2quality`. No Gradle invocation. This is a
source-quality audit, **not** client acceptance or a claim that the port is finished.

Paths below use **N/** = `common/src/main/java/thaumcraft/` and **O/** =
`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/thaumcraft/`.
Comment-sample coordinates refer to the input commit; other coordinates refer to
the audited source unless explicitly labelled before-fix. Native-only architecture
findings have no oracle counterpart; that is stated rather than inventing one.

Full absence-of-reference, static-state, catch-body and composed-path inventories
are in [`CODE_INVENTORY.json`](CODE_INVENTORY.json). It deliberately includes false
positive candidates and compatibility surfaces rather than silently hiding them.

## Summary

| Item | Actually inspected | Findings |
|---|---|---|
| 3.1 | 6,994 public-static declarations, 94 rule/pose-class candidates | 428 non-API test-only symbols/classes: 423 retained, four wired, one deleted class |
| 3.2 | 12 concrete duplicate pairs across 20 source owners | 12 pairs; three consolidated |
| 3.3 | 314 literal candidate lines screened; 30 source sites checked against oracle | 30 should-fix sites below |
| 3.4 | 6,950 matching comment blocks screened; 107 blocks checked as detailed below | 32 defective blocks, including citation errors; 31 corrected, one equivalence claim left open |
| 3.5 | All 1,442 input Java files; seven diagnostic matches examined | Three diagnostic sites; all corrected |
| 3.6 | 438 non-final static fields (401 declaration-screen entries); 240 collection declarations, including 90 explicitly mutable initializers | Six concrete ownership/lifecycle findings; full field lists retained |
| 3.7 | 164 broad catch clauses; 267 IllegalStateException candidate lines; 34 resource/registry fail-fast sites in seven common loaders | Five should-fix error-handling findings; no demonstrated save corruption |
| 3.8 | 37 composed-path candidates, of which 34 actually load resources; verifier implementation and real verify run | 34 uncovered resource call sites; verifier coverage gap is should-fix |

The counts are inspections, not a claim of full semantic reachability analysis.
In particular, a same-named occurrence is not proof that Java resolves it to the
declaration under review. An absent identifier is stronger evidence.

## 3.1 Dead code

**Inspection:** Java 21 compiler-tree parsing enumerated 3,455 public static
methods and 3,539 public static fields, including comma-declared fields. A
comment/string-stripped identifier screen searched common, Fabric and NeoForge
production code, including same-class uses and method references. It found **741
declarations with no non-declaration name reference** before wiring four pouch
helpers. The final count is **737**, of which **423** are outside the public API
package and have test references: **215 methods and 208 fields**. All 737, with declaration and first test locations, are listed in the
JSON; 314 remaining no-reference candidates require API/entry-point disposition.
Do not describe the positive side of this lexical screen as a resolved call graph.

**Should-fix — disconnected proof surface:** the 423 retained test-only symbols
are findings, not gameplay coverage. The largest clusters include:

* `N/common/lib/tiles/TileParityRules.java:32-287`: crucible heat/decay,
  infusion, mirror, jar and stabilizer rules tested separately from live tiles.
* `N/common/lib/tiles/LegacyTileRules.java:159-386`: nozzle, tube, valve,
  workbench and cosmetic-block predicates.
* `N/common/lib/crafting/DeviceContainerRules.java:101-311`: focus-pouch,
  harness, research-table and quick-move helpers with no production use.
* `N/common/lib/world/EldritchBlockRules.java:133-1195`: hardness, jars,
  doors, airy blocks and brain-box predicates with test-only callers.
* `N/common/lib/items/EquipmentRules.java:69-604`: arrow/stabilizer helpers
  and historical metadata constants retained only in tests.
* `N/common/lib/world/AuraGenRules.java:67,69,71`: scattered spacing,
  separation and salt constants. The live path reads modern structure starts;
  those tests do not prove it uses the original placement algorithm.
* `N/client/renderers/block/OreOverlayRules.java:51,53`: `insetMin` and
  `insetMax`; live code reads `INSET` instead.
* Before-fix `N/client/renderers/block/GasSurfaceRules.java:139`: `topLight`.
* `N/common/lib/golems/GolemLadderRules.java:105`: `ladderOf`, despite live
  use of other members of this class.

**Fixed — should-fix (four symbols):** `focusPouchSlotX:143`, `focusPouchSlotY:144`,
`focusPouchPlayerY:145` and `focusPouchHotbarY:146` in DeviceContainerRules now
own the identical live slot-position expressions in
`N/common/container/ContainerFocusPouch.java:38,41,44`. Original expressions are
`O/common/container/ContainerFocusPouch.java:47,63-69`. No transfer or inventory
authority rule changed.

The JSON is the complete per-symbol list, not just these examples. A test-only
accessor may be intentional, but it still needs that explicit disposition; a
tested duplicate rule must not be offered as evidence for the live implementation.
Bulk deletion or rewiring would be a large semantic/test refactor, so it is left
to the leader. This finding does **not** assert that every corresponding gameplay
behavior is absent: many are implemented independently, which is also §3.2's problem.

**Fixed — should-fix:** removed `N/common/lib/utils/ChestLootPoolRules.java:17-33`
and its seven disconnected tests in
`common/src/test/java/thaumcraft/crash/ChestLootPoolRulesTest.java:27-68`.
Neither loader calls the JSON helper. Its comment claiming loader sharing was
false. No live loot path was removed. This is the additional, 428th finding.

**Class-level cross-check:** of 94 initial rule/pose candidates, 90 have another
common production reference; two are valid loader-only entry points:
`BossPlacementRules` (`fabric/src/main/java/thaumcraft/fabric/FabricBossPlacement.java:20`,
`neoforge/src/main/java/thaumcraft/neoforge/NeoForgeBossPlacement.java:39`) and
`OriginalSpawnRules` (`fabric/src/main/java/thaumcraft/fabric/world/FabricBiomeSpawns.java:31`,
`neoforge/src/main/java/thaumcraft/neoforge/OriginalEntitySpawnsBiomeModifier.java:58`).
`BoneBowHeldPoseMixin` is configuration-selected, not a dead class. The remaining
candidate was the deleted chest helper. Literal compliance with “a common caller”
must not cause deletion of actual loader integration.

Requested-name production references:

| Class | Non-test reference under N/ |
|---|---|
| CandleDripRules | `client/renderers/block/PositionBlockModels.java:55` |
| GasSurfaceRules | `client/renderers/block/GasWorldQuads.java:33` |
| OreOverlayRules | `client/renderers/block/OreRenderRules.java:7` |
| SparkleCallRules | `client/ThaumcraftHolesClient.java:28` |
| AuraGenRules | `common/lib/world/ThaumcraftTotemFeature.java:85` |
| GolemFluidRules | `common/lib/fluid/GolemFluidAccess.java:108` |
| WandContextPose | `client/ThaumcraftGearModelsClient.java:160` |
| BoneBowPose | `common/mixin/client/BoneBowHeldPoseMixin.java:27` |
| ScanOverlay | `client/ClientStateReset.java:33` |
| HoveredSlotAspectRow | `common/mixin/client/ContainerScreenAspectRowMixin.java:38` |
| ThaumometerReadout | `client/renderers/item/ItemThaumometerRenderer.java:184` |
| GolemLadderRules | `common/entities/golems/InventoryGolemGoals.java:261` |
| FamilyVariant | `common/tiles/TileWardingStone.java:80` |

## 3.2 Duplication

**Inspected: 12 duplicate pairs**, including shared constants, repeated RNG,
fluid averaging, readout light and biome tables. Distinct fluid-registration and
world-source predicates were also checked: `GolemFluidRules.registrable:28-31`,
`GolemFluidBlocks.isSource:39-40`, and `LiquidGolemGoals.sourceFluid:98-99` are
different layers, not three interchangeable predicates. Likewise, no second
copy of `ThaumometerReadout.lightmap` was found; pure-fluid max-light sampling
is a different operation.

| Pair, both under N/ | Severity / decision | Oracle owner |
|---|---|---|
| Before-fix `client/renderers/block/GasSurfaceRules.java:75-86` / `common/lib/fluid/PureFlowRules.java:55-58` | should-fix; **fixed**: gas delegates to the common average; threshold now named once | `O/client/renderers/block/BlockGasRenderer.java:38-53` |
| `client/renderers/item/ItemThaumometerRenderer.java:211` / before-fix `client/renderers/item/ThaumometerReadout.java:209` | should-fix; **fixed**: screen calls `brightness` | `O/client/renderers/item/ItemThaumometerRenderer.java:154,157,229` |
| Before-fix `client/renderers/models/ModelTunnelField.java:51,59,86-88` / `client/renderers/tile/LinkedMirrorField.java:45-50` | should-fix; same seed/colour recipe, already drifted: tunnel RNG persists, linked mirror resets per call | `O/client/renderers/tile/TileHoleRenderer.java:43,96-98`; `TileMirrorRenderer.java:57,110-112` |
| `client/renderers/entity/RenderSpecialItem.java:60-74` / `client/renderers/tile/TileInfusionMatrixRenderer.java:83-96` | nit; shared six RNG rotations/seed 245, but different accumulation and geometry must remain distinct | `O/client/renderers/entity/RenderSpecialItem.java:62-89`; `O/client/renderers/tile/TileRunicMatrixRenderer.java:55-90` |
| `common/lib/world/NormalNodeBiomeAdapter.java:21-37` / `common/lib/world/MagicalTreeBiomeAdapter.java:18-33` | should-fix; same aura/aspect dictionary table, separately maintained and using different native tag namespaces | `O/common/config/Config.java:782-812`; `O/common/lib/world/biomes/BiomeHandler.java:26-64` |
| `client/renderers/tile/TileVisRelayRenderer.java:49` / `client/renderers/tile/TileMagicWorkbenchChargerRenderer.java:88-90` | should-fix; same `/200` colour and `50 + 150 * (sin(t/2)*.05+.95)` light recipe | `O/client/renderers/tile/TileVisRelayRenderer.java:64-67`; `TileMagicWorkbenchChargerRenderer.java:68-71` |
| `common/lib/crystals/CrystalClusterGeometry.java:10` / `common/tiles/TileVisRelay.java:36` | should-fix; same six-colour shard palette | `O/common/blocks/BlockCustomOreItem.java:17`; `O/common/items/ItemShard.java:54`, not `Aspect` colours |
| `common/lib/crystals/CrystalClusterGeometry.java:10` / `client/ClientSetup.java:232` | should-fix; same palette | same |
| `common/lib/crystals/CrystalClusterGeometry.java:10` / `client/renderers/entity/RenderPrimalArrow.java:25` | should-fix; same palette | same |
| `common/lib/crystals/CrystalClusterGeometry.java:10` / `client/renderers/entity/RenderPrimalOrb.java:22` | should-fix; same palette | same |
| `common/lib/crafting/DeviceContainerRules.java:143-146` / before-fix `common/container/ContainerFocusPouch.java:37,40,43` | should-fix; **fixed**: live pouch now calls the four position helpers | `O/common/container/ContainerFocusPouch.java:47,63-69` |
| `client/gui/HoveredSlotAspectRow.java:192-220` / `client/renderers/item/ThaumometerReadout.java:176-185` | nit; repeated amount/half-scale glyph rules, with different blend/coordinate adapters; do not blindly share the entire renderer | `O/client/lib/UtilsFX.java:583-599` |

The remaining nine pairs need an owner decision. In particular, changing the tunnel
RNG or moving its projection is a visible behavior change and was **not** smuggled
into this cleanup. The consolidated functions were compile-checked; the corner
average additionally passed 100,000 bit-identical comparison cases, including
NaN/infinities/empty arrays, plus two fixed oracle override examples.

## 3.3 Magic numbers

**314 candidate lines screened; these 30 sites checked directly. All should-fix.**
These are oracle-specific tuning/pose values, not generic white colours, bit masks,
triangle counts or data-serialization widths. No broad constant-extraction refactor
was attempted. Repeated uses on a source line count as one site.

| N/ file:line | Inline oracle values | O/ file:line |
|---|---|---|
| `client/renderers/item/BoneBowPose.java:16` | -.25, -.1875, .1875 | `client/renderers/item/ItemBowBoneRenderer.java:58` |
| same:17 | .125, .3125 | same:60 |
| same:18 | -20 degrees | same:61 |
| same:20 | -100 degrees | same:63 |
| same:21 | 45 degrees | same:64 |
| same:22 | -.3 | same:82-84 |
| same:24 | 50 degrees | same:87 |
| same:25 | 335 degrees | same:88 |
| same:26 | -.9375, -.0625 | same:89 |
| `client/renderers/item/BoneBowGlintRules.java:8` | 3000, 4873 ms | `client/renderers/item/ItemBowBoneRenderer.java:103,110` |
| same:9 | scroll multiplier 8 | same:103,110 |
| same:10 | -50, 10 degrees | same:105,112 |
| same:11 | texture scale .125 | same:101-102 |
| `client/gui/FocusRadialAnimation.java:13` | .05 step | `client/lib/REHWandHandler.java:167-175` |
| same:14 | 5 ms gate | same:178 |
| same:20 | 1.3 hover limit, .025 step | same:159-160 |
| same:21 | .025 return step | same:163-164 |
| same:24 | 16 + count * 2.5 | same:212 |
| same:25 | -90 initial angle, 360/count | same:250-251 |
| `client/renderers/item/ThaumometerReadout.java:120` | node-text y=-40 | `client/renderers/item/ItemThaumometerRenderer.java:201` |
| same:182 | amount scale .5 | `client/lib/UtilsFX.java:585` |
| same:210 | 190, 10, 10 pulse | `client/renderers/item/ItemThaumometerRenderer.java:157` |
| `client/renderers/tile/TileVisRelayRenderer.java:48` | .75 scale, -.16 shift | `client/renderers/tile/TileVisRelayRenderer.java:55-56` |
| same:49 | 200 divisor, 50/150 pulse, .05/.95 | same:64-67 |
| `client/renderers/tile/TileMagicWorkbenchChargerRenderer.java:88` | 200 colour divisor | `client/renderers/tile/TileMagicWorkbenchChargerRenderer.java:68` |
| same:90 | 50/150 pulse, .05/.95 | same:70-71 |
| `client/renderers/models/ModelTunnelField.java:86` | .1 outer alpha | `client/renderers/tile/TileHoleRenderer.java:53` |
| same:87 | .0625 inner texture scale | same:49 |
| same:89 | .5/.1 colour range | same:96 |
| same:101 | 4321, 9, 2 rotation | same:90 |

The last four tunnel coordinates include the three-line comment expansion in this
branch; in input `c6feeda7` they were 83,84,86,98 respectively. They are listed
because the limitation comment was repaired, not because those values were named.

## 3.4 Comments that lie

**6,950 matching blocks screened; 107 checked**, not “all comments verified”.
The table accounts for 100 separately located input comment blocks. Each oracle
range was opened. Small constant comments are separate blocks, not 100 lines of
one paragraph. The seven additional focused blocks are listed afterwards.

| N/ source | Input comment starts (count) | Checked against O/ |
|---|---|---|
| `client/renderers/item/WandContextPose.java` | 34,36,38,40,42,44,46,48,50,52 (10) | `client/renderers/item/ItemWandRenderer.java:61,66,67,69,72,74,79,81,82,84` |
| `client/renderers/item/WandUsePose.java` | 23,25,27,29,31,33,35,37,42,52,74,79,84 (13) | `client/renderers/item/ItemWandRenderer.java:89-112` |
| `client/renderers/item/ThaumometerReadout.java` | 38,40,42,44,46,48,50,52,54,62,73,82,108,114,127,144 (16) | `client/renderers/item/ItemThaumometerRenderer.java:164,191-203,221-258` |
| `client/gui/HoveredSlotAspectRow.java` | 41,43,45,47,49,51,60,62,87,92,96,115,120,145,150,170,192 (17) | `client/lib/ClientTickEventsFML.java:301-309,678-719`; `client/lib/UtilsFX.java:540-546,583-599` |
| `client/renderers/block/GasSurfaceRules.java` | 23,25,27,29,31,37,39,44,65,92 (10) | `client/renderers/block/BlockGasRenderer.java:32-65,104-117` |
| `client/renderers/block/CandleDripRules.java` | 7,34,36,38,51 (5) | `client/renderers/block/BlockCandleRenderer.java:43-66` |
| `common/lib/fx/SparkleCallRules.java` | 5,41,43,46,48,50,52,55,57,59,61,63,68,83,103,116 (16) | `client/ClientProxy.java:648-655,899-903`; `common/blocks/BlockMagicalLeaves.java:214-215`; `common/blocks/BlockLifter.java:80-81`; `client/fx/particles/FXSparkle.java:47` |
| `client/renderers/block/OreOverlayRules.java` | 3,21,23,25,30,40 (6) | `client/renderers/block/BlockCustomOreRenderer.java:47,54-70` |
| `common/lib/fluid/GolemFluidRules.java` | 8,34,44 (3) | `common/entities/golems/GolemHelper.java:331-369` |
| `common/lib/world/AuraGenRules.java` | 118,134 (2) | `common/lib/world/ThaumcraftWorldGenerator.java:190,419,430-451` |
| `client/renderers/item/BoneBowPose.java` | 6 (1) | `client/renderers/item/ItemBowBoneRenderer.java:48-64,82-89` |
| `client/renderers/item/BoneBowGlintRules.java` | 3 (1) | same:97-112 |

Additional focused blocks: `N/common/lib/world/biomes/BiomeGenEldritch.java:11`,
`N/common/items/equipment/ItemElementalAxe.java:20`,
`N/common/lib/world/AuraGenRules.java:16,80,108`,
`N/client/renderers/item/ThaumometerReadout.java:14`, and
`N/client/renderers/models/ModelTunnelField.java:12`.

**32 defective blocks** (31 repaired, one retained):

* **Nit, fixed (10):** all ten WandContextPose constant citations were shifted.
  Several pointed at a different transform: e.g. dropped-staff scale cited
  original `:69` (wand translation), inventory scale cited `:82` (translation),
  and inventory roll cited `:84` (staff translation). Actual lines are in the
  sample table. Values were already correct; only citations changed.
* **Nit, fixed (4):** hovered-row constants `:60,62` cited original
  `UtilsFX:544-546`, the **three-argument silhouette overload**, while claiming
  the six-argument colour overload. Correct citation is `:540-542`. Its draw
  comments `:115,120` also pointed to the background branch rather than colour
  (`ClientTickEventsFML:710`) and unknown-icon (`:712-719`) calls.
* **Nit, fixed (3):** candle header `:7` omitted the wick draw from its range;
  body `:34` cited the boolean declaration `O/BlockCandleRenderer:44`, not body
  bounds `:45`; wick `:36` cited reset-to-full-cube lines `:68-69`, not `:63-66`.
* **Should-fix, fixed (2):** `SparkleCallRules:41` called the proxy roll “one in
  six”. Original `ClientProxy:649,899-903` accepts 4/6, 2/6 or 0/6 outcomes by
  particle setting. `SparkleCallRules:83` said leaves sparkles stay inside the
  block; original `BlockMagicalLeaves:215` ranges approximately -.5 to 1.5 on
  each axis. Neither correction changes the actual rule.
* **Nit, fixed (2):** sparkle multiplier `:43` cited the no-clip assignment
  instead of constructor `O/ClientProxy:650` / lifetime `O/FXSparkle:47`;
  lifter-position prose `:116` now unambiguously says 20–80% horizontally.
* **Should-fix, fixed (3):** thaumometer header `:14`, icon prose `:144`, and
  initial-row constant comment `:38`: the original is **5,4,3,… icons per row**,
  spaced **16**, not eight, pixels vertically (`O/ItemThaumometerRenderer:233-237`).
* **Nit, fixed (2):** `AuraGenRules:16,134` cited closing braces and ring
  configuration as successful-build gates. Correct gates are original
  `ThaumcraftWorldGenerator:430-431,444-445,450-451`.
* **Should-fix, fixed (1):** `AuraGenRules:108` invented original tolerance of
  removed structure-registry entries. Original `ThaumcraftWorldGenerator:413`
  instantiates MapGenScatteredFeature directly. The nullable-registry fallback
  is now described as a native datapack adaptation, not original behavior.
* **Nit, fixed (1):** `GolemFluidRules:34` cited a tile lookup (`O/GolemHelper:348`)
  for the carried-fluid filter, which actually occurs at `:352`.
* **Should-fix, fixed (1):** Eldritch biome header `:11` said the zombie type was
  not implemented and JSON could only encode the guardian. Merged
  `common/src/main/resources/data/thaumcraft/worldgen/biome/eldritch_lands.json:40-49`
  encodes both; original `O/common/lib/world/biomes/BiomeGenEldritch.java:31-32`
  has both. The comment was stale, not a remaining entity omission.
* **Should-fix, fixed (1):** `ItemElementalAxe:20` said Repair was not implemented.
  Current `N/common/lib/enchantment/EquipmentEnchantments.java:32` calls
  `EnchantmentRepair.tick`; `EnchantmentRepair.java:30` admits IRepairable,
  which the axe implements. Original `O/common/items/equipment/ItemElementalAxe.java:64`
  implements that same contract. The comment now names the shared owner.
* **Should-fix, fixed (1):** `ModelTunnelField:12` asserted projection could not
  be reproduced without fixed-function GL and claimed original seeded colours.
  `LinkedMirrorField:32-58` already reconstructs projection in a shader; original
  `O/TileHoleRenderer:43,141` resets RNG, unlike retained field `N/ModelTunnelField:59`.
  Comment now admits the implementation gaps. **The visual gaps remain open.**
* **Should-fix, left open (1):** `AuraGenRules:80` calls modern start-bounding-box
  centre and origin-chunk selection equivalent to the original finder. Original
  `O/ThaumcraftWorldGenerator:413-417` calls a different finder; the comment's
  first-component assertion cites no available vanilla source line. One-node
  accounting alone does not establish equal positions or selection. Needs the
  official vanilla/mapping audit, not another unsupported parity sentence.

The pre-existing beam-origin/AuraHandler/wand-tip cases were not used to pad the
sample. No “chunk replay is impossible” claim was found in the current common
comments; `ChunkRegeneration:166-175` now describes the live replay.

## 3.5 Leftover diagnostics

**All 1,442 input Java files screened, seven matches read. Three findings, fixed:**

| Site under N/ | Severity | Fix |
|---|---|---|
| `client/lib/UtilsFX.java:82` | should-fix | replace `printStackTrace` with contextual mod logger warning |
| same:104 | should-fix | same; include divisor |
| `common/lib/events/ChunkRegeneration.java:141` | should-fix | per-tick progress log demoted from INFO to DEBUG |

The other four INFO sites are initialization/catalog/dimension validation, not
per-frame/per-tick emissions: `Thaumcraft:252`, `SmeltingBonusCatalog:90`,
`OuterLandsDimension:64`, `ObjectAspectCatalog:206`. No `System.out`,
`TODO`, `FIXME`, `XXX`, or `DIAG-` match remained in production common code.
These are native logging hygiene decisions, not oracle gameplay deviations.

## 3.6 Static mutable state

**438 non-final static fields enumerated by the Java compiler tree, plus 401
declaration-screen entries and 240 collection declarations listed in full in the
JSON**, including 90 unmistakably mutable initializers. Static array fields are
also listed separately, including final arrays whose contents remain mutable.
Comma-declared fields share a declaration entry; immutable `Map.of`/`List.of`
rows are intentionally included in the collection superset, not mislabelled as
mutable. Factory-returned collections remain visible in that superset.

Ownership classification: registration/init flags and content/API registries are
startup state; `ThreadLocal` guards are thread-local; `thaumcraft.client` caches
are client-owned, not evidence of a dedicated-server import. Callback sinks in
common classes are startup-installed client/server bridges, not shared stock.
The 46 `net.minecraft.client` imports under the common Java package all occur
under `common/mixin/client`, not ordinary server gameplay classes.

**Six should-fix findings**, no blind synchronization/refactor attempted:

1. `N/api/ThaumcraftApiHelper.java:33-34,200-216`: static mutable aspect-result
   caches exposed through side-neutral public helpers; no thread owner. No native
   production caller currently exercises those methods, so this is an API hazard,
   **not a demonstrated integrated-server race**. Registry-like aspect data does
   not make these lazily written result maps a registry.
2. `N/common/lib/utils/BlockUtils.java:34-38,196-235`: static recursive walk
   scratch coordinates/distance. No thread/local-operation owner or guard. The
   helper accepts `Level`, and same-thread reentrancy can also clobber scratch.
   Native caller absence must not be mistaken for thread-safety. Original static
   scratch is not a justification for sharing it in a concurrent native API.
3. `N/common/lib/utils/Utils.java:54,364-379`: global vis-effect expiry map.
   Server-only method parameter prevents a client write, but keys contain dimension
   and position, **not server identity**; no server-stop cleanup. Separate worlds
   can inherit suppression and expired entries are removed only when revisited.
4. `N/common/items/ItemCompassStone.java:26,33,45-60`: client cache has no
   level/server identity or disconnect clear. Its actual writer is client-gated
   `N/common/tiles/TileNode.java:495-499`, so **not** a proven client/server race.
   Reconnect to another world in the same dimension within ten seconds can reuse
   coordinates. `N/client/ClientStateReset.java:32-40` does not clear it.
5. `N/common/entities/golems/EntityTravelingTrunk.java:72,392-408`: mutable
   trunk-link map is global by player UUID, not server instance; no shutdown clear
   or documented thread ownership. Native writer is `customServerAiStep:284`,
   not the client. Weak trunk references reduce retention, but do not isolate
   overlapping/still-referenced server worlds.
6. `N/common/lib/world/StructurePlacementRules.java:103-106`: public static
   lists contain mutable `int[]` sample rows. Unmodifiable outer lists do not
   freeze the coordinates. Unlike registries, these are supposed to be immutable
   placement facts; callers/addons can silently alter later generation.

No shared client/server tessellation buffer was demonstrated. `VisNetRender.MESH`
is client-only; `OreRenderRules.anisotropicFiltering` is volatile and explicitly
documents render-thread write / terrain-worker read. Many other client render
caches still rely on implicit thread convention; the inventory records them,
and a wholesale claim that all 438 fields are documented would be false.
These architecture findings have no direct oracle specification for Java 21
thread ownership or multi-server JVM lifecycle.

## 3.7 Error handling

**164 broad catch clauses screened.** The 159 direct `Exception`/`RuntimeException`
clauses have bodies in the inventory: 84 log, 31 rethrow/aggregate, 19 reject or
quarantine payloads, 25 require closer fallback assessment. Five additional union
catches occur at `ItemApi:84`, `ThaumcraftOuterLands:338`, `ModelVisRelay:33`,
`WarpSonicParticle:60`, `RunicHitParticle:53`: optional reflection fallback or
explicit rethrow, not silent inventory/save loss.

All 25 fallback candidates, rather than just `ignored` variable names:

| N/ site | Judgment |
|---|---|
| `common/tiles/TileArcaneFurnaceNozzle.java:60` | **should-fix F1:** surrounds raw neighbor `takeEssentia:75`; if an addon debits then throws, the grant is lost and the catch hides it. Not an injected-failure reproduction. |
| same:90 | empty catch around local lookup; no stock mutation, but hides programming errors; include in F1 cleanup |
| `common/tiles/TileEldritchPortal.java:166` | missing-dimension fallback; no paid inventory mutation in catch |
| `common/tiles/TileArcaneBore.java:854` | clears malformed **client view**, not authoritative saved stock; no demonstrated disk loss |
| `common/tiles/TileTube.java:198,214` | neighbor suction/vent queries; no transfer rollback; silent addon faults obscure diagnosis, include in F2 |
| same:234 | **should-fix F2:** catches transfer/post-commit callback failures without logging; paid units are not rolled back, but the failure is invisible |
| `common/crafting/InfusionEnchantmentRecipe.java:127,153` | unknown enchantment/target fail closed; no paid commit in these predicates |
| `common/lib/utils/DeferredItemDrops.java:367` | validation rejects bad entity marker; not dropping saved custody in catch |
| `common/lib/utils/UUIDHelper.java:43,67` | **should-fix F3:** external HTTP/JSON failure indistinguishable from “account not found”; no chunk/inventory loss, but network diagnostics lost |
| same:83 | malformed UUID parsing returns null, acceptable validation |
| `common/lib/utils/Utils.java:227` | optional-class detection returns false, acceptable |
| `common/lib/world/AuraGenRules.java:99` | **should-fix F4:** any runtime chunk failure becomes “no temple”; can silently omit a generation pass, not merely handle an unloaded chunk |
| `common/lib/world/EldritchRingFeature.java:120` | **should-fix F5:** unreadable maze becomes failed preflight with no diagnostic; comment says another pass reports it, but catch itself preserves no error |
| `common/lib/essentia/EssentiaTransfers.java:632,786,869` | returns failure inside `Attempt`, not actually swallowed; actual stock count retained |
| `common/entities/golems/FishingGolemGoals.java:390` | retains `unreadPending` raw copy; failed decode does not flatten paid stock |
| `common/entities/golems/GolemHarvestPlatform.java:97` | denies unknown/refused actor, fail closed |
| `common/items/wands/ItemFocusPouch.java:58` | rejects malformed pouch without replacing payload |
| `api/internal/ThaumcraftInternalMethods.java:153` | missing mixin returns null for knowledge; no chunk save mutation here |
| `api/aspects/AspectList.java:32` | swallowed addon query failure, original API fallback; no item debit/save occurs here |
| `client/lib/ScanOverlay.java:102` | client ore-hint reflection failure returns zero; no authoritative state mutation |

The two UtilsFX broad catches now log (§3.5). `ChunkRegeneration:219-225` also
logs failures, so it is not a silent catch. It can still mark a partly replayed
chunk dirty; this audit does not claim regeneration is transactional.

**Fail-fast resource loading:** 267 `throw new IllegalStateException` candidate
lines were screened. These **34** sites in seven server-side loaders were checked:

* `N/common/lib/crafting/SmeltingBonusCatalog.java:60,65,73,86` (4).
* `N/common/lib/research/ObjectAspectCatalog.java:60,63,76,142,204` (5;
  `:204` is registry validation rather than byte loading).
* `N/common/lib/research/EarlyObjectAspectBindings.java:46,50,60,65,73,78,100` (7).
* `N/common/lib/research/ResearchRegistrationOrder.java:37,46,49` (3).
* `N/common/lib/research/ResearchCatalog.java:30,33,40,58,61,67,72` (7).
* `N/common/lib/research/EntityAspectBindings.java:155,158,163,171,173` (5).
* `N/common/lib/research/ResearchRecipeCoverage.java:130,642,648` (3).

These can crash a dedicated server on missing/corrupt packaged corpora or an
inconsistent registry installation; **that is justified fail-fast**, not something
to convert into empty research/aspect data. Recipe discovery `nativeResearchGate`
returns null on missing bytes at `:125`, rather than throwing. Client OBJ/mesh
fail-fast paths (`ModelThaumometer:40,64`, `ModelAdvancedAlchemyFurnace:21,43`,
`ModelVisRelay:33`, sonic/runic meshes) are not dedicated-server loaders.
No blocker-level data-loss reproduction was established. F1–F5 need targeted
failure-injection/ownership decisions; changing swallowing semantics was not safe
behavior-neutral work.

## 3.8 Resource loading

**37 concatenation candidates inspected; 34 actual resource-loading sites.**
The other three construct discovered recipe IDs or a translation key, not resource
paths (`ResearchRecipeCoverage:659,672`, `ItemWandCasting:376`).

**Should-fix:** the following 34 sites are invisible to the asset verifier's
Java-reference coverage. This is not a claim that all 34 resolve to missing files:

| N/ file:line | Composed family |
|---|---|
| `client/ThaumcraftArchitectClient.java:134` | warded_glass texture suffix |
| `client/ThaumcraftWarpClient.java:150` | post-process shader name |
| `client/ThaumcraftShockClient.java:216` | p_small / p_large texture |
| `client/ThaumcraftGearModelsClient.java:349` | armor layer sheet |
| `client/BoreClientEffects.java:51` | generic textures/path.png |
| `client/ThaumcraftExcavationClient.java:297` | beam texture |
| `common/lib/research/ResearchRecipeCoverage.java:124` | classpath recipe JSON |
| `api/wands/FocusUpgradeType.java:48` | foci key texture |
| `api/wands/StaffRod.java:17,26` | rod tag texture (two sites) |
| `api/wands/WandRod.java:51,56,66` | rod tag texture (three sites) |
| `api/wands/WandCap.java:32,44,57` | cap tag texture (three sites) |
| `api/aspects/Aspect.java:88` | aspect tag texture |
| `client/renderers/VisNetRender.java:32,127` | generic texture / model sheet (two sites) |
| `client/gui/GuiResearchTable.java:63` | GUI texture helper |
| `client/gui/HoveredSlotAspectRow.java:71` | aspect backing / unknown helper |
| `client/gui/GuiFocalManipulator.java:60` | GUI texture helper |
| `client/renderers/models/ModelEssentiaCrystalizer.java:30` | name.obj |
| `client/renderers/tile/TileWardedRenderer.java:27` | warded_glass index texture |
| `client/renderers/tile/TileNodeStabilizerRenderer.java:73` | model name texture |
| `client/renderers/tile/TileAlchemyFurnaceAdvancedRenderer.java:20` | model path texture |
| `client/renderers/tile/TileResearchTableRenderer.java:24` | texture helper |
| `client/renderers/entity/RenderPech.java:43` | model texture name |
| `client/renderers/entity/RenderTravelingTrunk.java:75` | upgrade key texture |
| `client/renderers/models/gear/ModelWand.java:47,52` | rod/cap tags (two sites) |
| `client/fx/particles/WarpSonicParticle.java:80` | ripple frame |
| `client/fx/particles/RunicHitParticle.java:71` | hemis frame |
| `client/fx/particles/WardHitParticle.java:21` | hemis frame |

`scripts/porting/convert_assets.py:644-736` checks atlas entries, model JSON
references, blockstates and sounds. It does not inspect Java, shader post/program
dependencies, classpath research/recipe JSON or OBJ names. Static renderer texture
literals can therefore also escape it. Registry IDs and network channel IDs were
not counted as asset paths.

Actual command run: `python3 scripts/porting/convert_assets.py --verify` succeeded:
3,943 model texture refs, 3,018 local model refs, 518 atlas sprites, 111 sound files,
66 sound events, **missing_references: 0**. That zero does **not** close this item.
Leader should add an explicit composed-family manifest with bounded enumeration,
including addon/open-ended defaults, and verify shader/OBJ/corpus dependencies.
Replacing every ResourceLocation helper with literals is not the appropriate fix.

## Verification and scope of fixes

* Java 21 `javac --release 21 -proc:none -sourcepath ''` compiled **all 1,441
  remaining common Java files**, not stubs; no source errors, only existing
  deprecation/unchecked notes. Final rerun recorded in the lane analysis.
* Classpath: original remapped `minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2.jar`,
  this worktree's `common/build/classes/java/main`, binary jars under
  `~/.gradle-leader/caches/modules-2`, with the named Architectury NeoForge API
  jar before intermediary API jars. This is a compile API choice, **not a NeoForge
  build/runtime acceptance run**.
* Fresh worktree had no common classes. Plain javac initially failed at access
  wideners, not source syntax. A temporary five-class bytecode **access-only
  overlay** reproduces the three declared common access wideners plus the native
  `BlockEntityType.BlockEntitySupplier` visibility required by the normal loader
  compile environment. Original cached jar untouched. No gameplay method bodies
  or signatures were replaced; this is not a stubbed compile or a clean Gradle gate.
* Gas-average consolidation: 100,000 old/new float-bit comparisons and two pinned
  oracle examples passed. The pouch layout also passed all 18 slot locations,
  three inventory row locations and the hotbar location (22 cases).
  These comparisons are behavior-neutral refactor evidence,
  not independent gameplay parity evidence.
* No Gradle, game client, dedicated server, or full JUnit suite was run by this lane.
* The retained tunnel rendering/RNG gap, disconnected rule surface, lifecycle
  ownership and verifier blind spots require leader decisions. This audit is not
  green code-quality acceptance.
