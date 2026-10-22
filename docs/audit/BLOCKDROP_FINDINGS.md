# Block-drop audit — g2blocks, 2026-10-23

Hypothesis: removal loses or duplicates contents **relative to TC4 4.2.3.5**, not relative to a blanket rule that every machine must retain every resource.

Scope: **40 block entries inspected** (registered variants counted separately where listed below), plus the Traveling Trunk entity. Source review is not runtime acceptance. Only cases explicitly recorded as run below have new execution evidence. No NeoForge/client acceptance is claimed.

Result: **1 confirmed content-loss block fixed, 0 confirmed content duplications**. Fourteen focused Fabric GameTests cover ten block entries: three Magic Box regressions fail before the fix, and all fourteen pass afterward. The remaining thirty entries are source-reviewed only, with limitations/candidates explicitly retained below.

## Oracle rules that matter

* Oracle `BlockMagicBox:72-75`, `BlockTable:127-129`, `BlockStoneDevice:312-318`, `BlockMetalDevice:529-539`, `BlockChestHungry:106-133`: inventory evacuation is a removal callback, not just a player harvest callback.
* Oracle `InventoryUtils:354-378` copies count, damage and NBT, and clears physical slots. There is **no creative-player exclusion** in this removal helper. Workbench result slot 9 is deliberately excluded.
* Oracle `BlockJar:158-194` stores aspect amount/filter and node attributes in the dropped item. Brain XP is released by `breakBlock` (`197-207`). Jar harvesting additionally has a forced-drop hook (`153-155`); creative parity of that separate hook remains unverified.
* Crucible water is deliberately discarded; remaining aspects spill (`TileCrucible:217-227`). Alembics drop a blank label, not stored essentia (`BlockMetalDevice:534-535`). Tubes drop only a filter label (`BlockTube:298-303`); buffers and centrifuges do not retain their transient essentia. Reservoir contents become a burst/flux (`BlockEssentiaReservoir:104-125`). These are not port losses.
* Vanilla 1.7.10 pistons reject `ITileEntityProvider` blocks. Modern native piston rejection of BE-bearing blocks can preserve that rule even when `PushReaction` itself is `NORMAL`; checking only that enum would produce false positives.

The 36 corresponding static loot tables under `common/src/main/resources/data/thaumcraft/loot_table/blocks/` were also parsed. They supply shells/structural blocks, not inventory-copy functions; most gate shell drops with `survives_explosion`. Arcane-furnace and advanced-furnace tables select structural items by block state. Jar/node dynamic overrides and workbench/table overrides are separate code paths, not additive second inventory loot tables. Thus shell explosion decay must not be mistaken for permission to discard a container's loose stock.

## Inventory/removal matrix

`R` means source-reviewed only; `T` names a new GameTest (execution status below). Existing tests mentioned here were inspected, not rerun unless explicitly recorded.

| # | Block entry | Port content path | Content disposition / oracle comparison | Test / fix |
|---|---|---|---|---|
| 1 | Magic Box | Initially only `playerWillDestroy`; fixed to `onRemove` → `TileMagicBox.dropContents` → durable deferred drops, clear | **Confirmed loss, fixed** on explosion, non-player destruction and replacement; native creative/Silk breaks already preserved stock | T: `blockDropMagicBoxDestroy`, `SetBlock`, `Explosion`, `Creative`, `Silk` (suffixes use the same `blockDropMagicBox` prefix); all five pass after fix |
| 2 | Hungry Chest | `onRemove` → `evacuate` → deferred drops, clear | Whole component stacks evacuated | T: `blockDropHungryChest` |
| 3 | Arcane pedestal | `onRemove` → `evacuate`; obstruction also evacuates | Item transferred and cleared, no double drop from later empty removal | T: `blockDropPedestal` |
| 4 | Deconstruction table | `onRemove` → `evacuate` | Physical input evacuated; research reward is not an item | T: `blockDropDeconstruction` |
| 5 | Arcane workbench | `onRemove` → `evacuate` | Physical stacks evacuated; result slot is virtual/excluded | T: `blockDropWorkbench` |
| 6 | Alchemy furnace | `onRemove` → `evacuate` | Input/fuel copied, cleared and deferred | T: `blockDropAlchemyFurnace` |
| 7 | Focal manipulator | `onRemove` → `evacuate` | Focus copied and cleared; job canceled | T: `blockDropFocalManipulator` |
| 8 | Thaumatorium lower | `onRemove` → `evacuate` | Input and pending physical output evacuated; recipe selection/essentia are machine state, not inventory drops | T: `blockDropThaumatorium`; recipe/essentia parity R |
| 9 | Thaumatorium upper | Lower owns inventory; upper has none | No independent stock to duplicate | R |
| 10 | Research table | `BlockTable.onRemove` → `TileResearchTable.evacuate` | Main inventory copied and cleared; topology handles pair | R; existing `TableDropCancellationRuntimeChecks` |
| 11 | Wand pedestal | `onRemove` → `removeItemNoUpdate(0)` → deferred drop | Single physical wand transferred out | R |
| 12 | Arcane furnace | `playerWillDestroy` and `onRemove` → guarded collapse → `dropContents` | Drop helper empties all slots; two entrypoints require collapse guard, not two inventories | R; multiblock/explosion behavior not newly run |
| 13 | Advanced alchemy furnace | `onRemove` → `AdvancedFurnaceFormation.broken` | Structural teardown entry exists; full resource disposition not verified | R, incomplete deep audit |
| 14 | Arcane Bore | `onRemove` → `BoreWork.evacuate` | Native retirement/ownership path, not standalone drop | R; existing Bore checks, not rerun |
| 15 | Bore base | No content drop override | Base is not the tool inventory owner | R |
| 16 | Aspect jar | Dynamic `getDrops` → `saveToItem` | Exactly one jar carries amount and filter; no extra static loot path | T: `blockDropAspectJar`; existing `JarLabelRuntimeChecks` break/save/place assertions |
| 17 | Void jar | Same dynamic path | Same item contents/filter preservation | T: `blockDropVoidJar` |
| 18 | Brain jar | `JarBlock.onRemove` → XP orbs | XP released rather than saved in jar item, like oracle | R; callback reentrancy not exercised |
| 19 | Node jar | Dynamic `JarBlock.getDrops` → item API | Aspects/type/modifier/id copied when `drop` is true; false branch returns empty jar unlike oracle's empty list — follow-up candidate, not runtime-confirmed | R; no fix claimed |
| 20 | Crucible | `onRemove` → `onReplaced` | Water loss/spill is intentional oracle behavior, not a filled-crucible item | R |
| 21 | Infusion matrix | `onRemove` → active-crafting blast after BE removal | Matrix is not pedestal inventory owner; active destruction blast is intentional | R |
| 22 | Alembic | `onRemove` → deferred blank label | Filter returns as blank label, essentia deliberately not retained | R; existing `JarLabelRuntimeChecks` |
| 23 | Essentia tube | Ordinary block loot; no essence evacuation | Transient essentia intentionally lost, like oracle | R |
| 24 | Essentia valve | Same | Same | R |
| 25 | Essentia filter | `onRemove` → `returnTubeFilterOnRemove` | Label-specific removal path exists | R; existing `TubeFilterLabelRuntimeChecks` |
| 26 | Restriction tube | Ordinary block loot | No retained essentia in oracle | R |
| 27 | One-way tube | Ordinary block loot | No retained essentia in oracle | R |
| 28 | Tube buffer | Ordinary block loot | Aspect buffer not included in oracle drops | R |
| 29 | Centrifuge | Ordinary block loot | Internal essentia not included in oracle drops | R |
| 30 | Essentia crystallizer | Ordinary block loot | In-flight essence/progress not an inventory drop in oracle | R |
| 31 | Essentia reservoir | `onRemove` → burst/flux | Destructive conversion, not filled item, like oracle | R; burst reentrancy not exercised |
| 32 | Arcane lamp | `onRemove` → `removeLights` | Generated light cleanup, not item inventory | R |
| 33 | Growth lamp | Ordinary loot, no essentia retention | Oracle metal-device removal does not serialize lamp fuel | R |
| 34 | Fertility lamp | Same | Same | R |
| 35 | Arcane Spa | `onRemove` → `evacuate` | Salt evacuated; tank deliberately discarded | R |
| 36 | Levitator | `onRemove` refreshes stack | No physical inventory evacuation needed | R |
| 37 | Node stabilizer | Ordinary loot | Stockless device | R |
| 38 | Node converter | `onRemove` → Vis node failure handling | Specialized network retirement, not a container | R; deep audit deferred |
| 39 | Banner | Dynamic `getDrops` copies `BannerData` | Colour/aspect component retained | R |
| 40 | Metal-device brainbox/grate family | Ordinary loot; no inventory evacuation override | No physical container identified in inspected family; tile identity/config not proved retained | R; incomplete deep audit |

The Traveling Trunk is an **entity**, not a piston-movable block: `die` calls `inventory.dropAll` before `super.die`. This was source-inspected only and is excluded from the 40-block count. A TC4 Golem Press block was not located; no invented TC5 block is included in the count.

## Execution evidence

**Baseline runtime: 14 tests, 3 failures, 11 passes.** The only failures are `blockDropMagicBoxExplosion`, `blockDropMagicBoxDestroy`, and `blockDropMagicBoxSetBlock`, each reporting missing exact 17 component-bearing diamonds. Both jar controls, all seven container controls, and the Magic Box's native creative/Silk breaks pass against unchanged production code. Red XML was preserved as `.tmp/g2-red-results.xml`; runtime log `.tmp/g2-red-runtime.log`.

**Fixed runtime: 14 tests, 0 failures; Gradle `BUILD SUCCESSFUL`.** Green XML is preserved as `.tmp/g2-green-results.xml`, log `.tmp/g2-green-runtime.log`; the normal focused report is `fabric/build/gametest-focused-results.xml`. All `T` rows above passed in both runs except the three explicitly named baseline failures. Actual final common/Fabric production sources and GameTests compiled; test/production source-root isolation passed. Bootstrap workarounds and exact execution setup are recorded in `docs/analysis/2026-10-23-agent-g2blocks.md`.

The new tests use a named component-bearing diamond stack, native destruction/replacement, ground-item enumeration, old-BE emptiness/removal assertions, and the native piston admission predicate. Creative and Silk Touch use the native server player's `gameMode.destroyBlock`; explosion uses a real small block-damaging explosion. Non-player `destroyBlock` is not mislabeled as survival mining, and `setBlockAndUpdate` exercises the replacement hook rather than the command parser.

## Limits

This is a bounded removal-path audit, not acceptance of all ~50 proposed blocks in every mode. Silk Touch is tested only for the Magic Box. Physical piston actuation, every block's explosions/creative behavior, canceled entity-spawn callbacks, linked-box topology, and same-block state transitions in multiblock furnaces remain unverified unless separately named above. Reasoned-only candidates are not counted as proven losses or duplicates.
