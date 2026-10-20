# Seven original Crystal Clusters — Fabric 1.21.1

The six existing `crystal_air`, `crystal_fire`, `crystal_water`, `crystal_earth`, `crystal_order` and `crystal_entropy` IDs now use real inert crystal tiles and the original attachment behavior. Metadata6 becomes `thaumcraft:crystal_mixed`, exposed by `ThaumcraftCrystalClusters.MIXED` and `MIXED_ITEM`. The six-entry `ThaumcraftContent.CRYSTALS` map remains primal-only: the original research-table bonus branches distinguish metadata0..5 and do not give mixed clusters a seventh bonus.

This unit covers original `blockCrystal` metadata0..6. Metadata7's Eldritch Crystal and `TileEldritchCrystal` are a separate contract.

## Original authority

All original reads use the immutable, external TC4 4.2.3.5 oracle at `/workspace/work/thaumcraft-4.2.3.5-oracle`; its files are neither copied into the repository nor modified.

| Oracle source | Contract |
| --- | --- |
| `common/blocks/BlockCrystal.java:57` | Hardness0.7, resistance setter1, light0.5 and custom crystal sound. |
| `common/blocks/BlockCrystal.java:83` | Spark probability1/17, original primal colors, mixed random primal and alpha0.8. |
| `common/blocks/BlockCrystal.java:105` | Non-opaque/non-normal render block; no custom collision or selection bounds are installed. |
| `common/blocks/BlockCrystal.java:135` | Six separate one-count shard stacks; metadata6 returns one of each primal. Fortune is ignored. |
| `common/blocks/BlockCrystal.java:156` | Any solid attachment is required, then loss of the selected orientation removes the cluster even if another attachment remains. |
| `common/blocks/BlockCrystal.java:207` | Placement checks the clicked side, not any other supporting neighbor. |
| `common/blocks/BlockCrystal.java:245` | All seven variants unconditionally implement infusion stabilization. |
| `common/blocks/BlockCrystalItem.java:36` | Successful item placement writes the clicked side to the actual tile after superclass placement. |
| `common/tiles/TileCrystal.java:14` | Constructor orientation1; NBT key `orientation` is a signed SHORT; missing saved key reads0; no growth or tick method. |
| `common/config/ConfigBlocks.java:277` | Original block and custom block-item registration. |
| `common/config/ConfigRecipes.java:443` | Six equal shards make each primal cluster; one of each primal makes mixed, all ordinary shapeless recipes. |
| `common/config/ConfigResearch.java:370` | `Clusters0` through `Clusters6` appear on the automatically unlocked ORE stub. |
| `common/tiles/TileResearchTable.java:214` | Only the six primal cluster metadata values participate in the original nearby research bonuses. |
| `client/renderers/tile/TileCrystalRenderer.java:33` | Six attachment transforms, six pieces, positional Java RNG, mixed color sequence and viewer-tick light pulse. |
| `client/renderers/models/ModelCrystal.java:17` | Original 64x32 texture, cube dimensions, pivot and rotations. |
| `client/renderers/block/BlockCrystalRenderer.java:30` | Original inventory display invokes the tile renderer at zero coordinates, UP orientation, with a90-degree Y rotation. |
| `client/fx/particles/FXSpark.java:22` | Stationary spark, lifetime5..9, three atlas rows, mirrored UV choice and age-based eight-frame animation. |

The original Minecraft1.7.10 `Block` bytecode was also inspected outside the repository: `setResistance(1)` stores3 and `getExplosionResistance` divides that field by5. The actual native resistance is therefore **0.6**, not the setter's argument1. Modern `strength(.7F, .6F)` preserves the effective explosion value. Original light0.5 becomes integer light7.

## Gameplay and persistence

`BlockCrystal` now extends `BaseEntityBlock`; `TileCrystal` is registered for all seven block identities. The original selection and collision boxes remain full cubes even though the rendered crystals occupy a smaller volume. Its support shape is empty, preserving the original non-normal block's inability to provide another crystal's solid attachment.

Orientation values0..5 retain DOWN, UP, NORTH, SOUTH, WEST and EAST. A failed clicked-side check consumes nothing. Losing an unrelated neighbor preserves the cluster. Losing its selected support drops it once, including when another adjacent solid block remains. A loaded signed SHORT outside0..5 remains unchanged: after the any-support check, none of the original six orientation comparisons matches it. Missing saved orientation still reads0 rather than the constructor's1.

Seven `clusters/*` recipes use actual shard identities and ordinary native crafting result slots. Six pools in each loot table preserve six physical one-count entries. Mixed drops air, fire, water, earth, order and entropy once each; balanced shards never substitute. There is no Silk Touch/Fortune branch, copied tile NBT, recipe research charge, wand or vis cost. Explosion survival is applied independently to the six entries through native loot conditions.

The native item adapter writes orientation after `BlockItem.place` finishes its block-entity component merge. Otherwise a supplied `BLOCK_ENTITY_DATA.orientation` could overwrite the clicked side after an earlier `placeBlock` override. The runtime fixture supplies a valid `thaumcraft:crystal_cluster` block-entity ID with conflicting orientation5 and places on DOWN0, so this ordering is exercised against the real item path.

Two explicit engine adaptations are bounded here. Original Forge `isSideSolid` maps to the native `BlockState.isFaceSturdy` face contract and respects loaded chunk boundaries. Missing or wrong block entities cause safe support removal instead of reproducing the original unchecked cast/null crash. Neither adaptation adds growth or changes the saved orientation domain.

## Presentation and resource provenance

`CrystalClusterGeometry` reproduces the original int-expression seed `metadata + x + y*z`, including int overflow before widening to Java `Random`. Each piece consumes the same angle, pulse and scale draws. The mixed center is ORDER; its five outer pieces are AIR, FIRE, WATER, EARTH and ENTROPY. Viewer ticks alter the pulse but not the shape. All six attachment transforms and the unrecognized-orientation identity transform are preserved.

`TileCrystalRenderer` uses the original model cube, UV dimensions, pivot, rotations, original `textures/models/crystal.png`, alpha rendering and original light values. The old `mirror=true` assignment occurred after cube creation and did not retroactively mirror baked UVs. Block items use the same native renderer with zero positional seed and the original UP/90-degree display, within native item-display transforms. Model JSONs deliberately have no crossed planes; actual geometry belongs to the tile/builtin renderer.

`CrystalSparkParticle` uses the retained original `textures/misc/particles2.png`, original lifetime, animation, additive blend, positions, primal colors and alpha. The original `thaumcraft:crystal` sound and `textures/blocks/crystal.png` particle sprite are retained. These existing original assets retain their recorded provenance and attribution; this unit adds no substitute textures.

Modern `ModelPart`, `PoseStack`, render buffers and the native particle shader replace immediate-mode OpenGL and the old custom particle engine. Shader alpha handling and item-display transforms require inspected client evidence; compile success does not establish visual parity. The block/tile and all seven Fabric builtin item renderer registrations are wired, but client visual acceptance remains pending.

The converter has a bounded seven-crystal regeneration function that writes only the seven blockstates, block models and builtin item models. Its fixture regenerates into a temporary directory and compares against the checked-in files. No full asset generation was run for this change. The resource manifest updates only the seven crystal mappings and related crystal descriptions: six existing identities retain their metadata/tints, mixed adds the actual45th foundation identity without a fake fixed tint, and all seven mappings declare the implemented native renderer with client verification pending. During this manifest update the helper's21 resource writes were explicitly suppressed. Other identity mappings, original copied asset hashes and provenance remain unchanged.

## Meaningful verification

Four `CrystalClusterGeometryTest` checks passed in the local JUnit XML timestamped `2026-10-20T10:53:02`: original Java-RNG golden values, mixed color order, seed collisions/viewer-only pulse changes, and all attachment transforms including invalid signed orientation. These pure checks cover deterministic rendering inputs, not screenshots.

`CrystalLootTableTest` now verifies all seven actual JSON tables, six separate unconditional one-count entries, independent explosion conditions and mixed one-of-each output. It replaces the earlier one-pool/count6 expectation, which erased the original six-stack drop contract.

Two native GameTests are supplied:

- `sevenRecipesPlaceOnSixFacesAndRecoverTheirSixShards` starts with declared shard inventory. It uses actual crafting-table menus and result-slot packets, refuses an extra balanced-shard input without consuming it, crafts every placed cluster, tests all42 variant/face placements, rejects an unsupported clicked fence face despite another valid support, checks conflicting item NBT, tile serialization and physical properties, tests EMPTY/Silk/Fortune loot, removes selected and unrelated supports, picks up real dropped entities and conserves every input shard. It also checks that ordinary crafting leaves research knowledge unchanged.
- `mixedOrientationActuallyUnloadsAndReloadsFromChunkNbt` uses a private distant chunk and its own removable tickets. It requires actual chunk absence and old tile removal before reloading a new chunk/tile from storage, checks mixed/EAST5 SHORT persistence and absence of a growth ticker, then removes support and checks six actual distinct one-count item entities. The fixture restores previous block/NBT state and removes its own tickets.

The coordinator subsequently reported production compilation, the four geometry checks and the corrected loot assertions passing, plus all84 Python checks/resource verification on the intermediate snapshot. The final crystal-only manifest/assertion update requires its subsequent Python rerun. The two native GameTests still require the coordinator's compile/runtime result. No full server runtime or inspected client pass is claimed by this document.

An adjacent converter defect was corrected while checking these resources: native blockstate `multipart[].apply` may be an object or a weighted list, just as `variants` values may. Validation now resolves every model reference in both forms and reports malformed/missing references clearly. Temporary-directory tests retain ordinary variants validation and detect a missing model in a later multipart alternative. This changes resource validation, not furnace or crystal gameplay.
