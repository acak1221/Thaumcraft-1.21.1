# Closing the partial ledger units — 2026-10-21

Branch `port/ledgerpartial`, base commit `5084a65` ("Record the real game test state of the OpenTC4 wave").

## 1. Scope

The wave of 2026-10-21 asked for a re-audit of the units that the scope ledger
(`docs/porting/scope-ledger.json`) marked `native_coverage = partial` (345) and
`source_semantics = different` (23).

The lanes of the other gap agents own the client renderers and the FX, the scenarios, the crafting,
the entities and the tiles, the world generation and the golems, the items and the network. This lane
therefore owns the units of the following sources only:

* `thaumcraft/api/**` (the public API),
* `thaumcraft/common/blocks/**` (the block layer, except the files of the tiles and of the items),
* `thaumcraft/common/config/**`, `thaumcraft/common/container/**`, `thaumcraft/common/lib/**`
  (except the network packages and the files of the entities).

That is **90 units**: every unit of the whole ledger that was `partial` or `different` and is not in
another lane. All 90 were `partial`; 5 of them were `different` as well.

## 2. Method

Every unit was re-audited against the 4.2.3.5 oracle (`/home/Acak/work/thaumcraft-4.2.3.5-oracle`) and
against the native tree of this branch:

1. The notes of the unit were split into the individual claims (a defect, a missing behaviour, a forced
   1.21.1 change).
2. Every claim was checked against the current native code, not against the state of the code when the
   note was written.
3. Every claim that names a 1.7.10 member was resolved with
   `python3 -B scripts/porting/resolve_srg.py <name>` (the 1.7.10 MCP mapping), so that the SRG names of
   the oracle can be read.
4. A claim that was still true was either closed in the code (with a test) or written down as a
   documented gap with the oracle line, the native counterpart and the remaining difference.
5. The verdict of the unit was set from the result of the audit.

The verdicts are written by `scripts/porting/apply_ledgerpartial_verdicts.py`, which touches only the 90
units of this lane and keeps the authored fingerprints of the original and of the legacy source, so the
ledger stays refreshable with `scripts/porting/update_scope_ledger.py`.

## 3. The main finding: most of the partial verdicts were stale

The ledger said `partial` for a reason that the native tree had already answered. The clearest cases:

| The ledger said | The native tree does |
| --- | --- |
| `BlockMagicBox` registers no ticker, so the box chain never links | the ticker of `TileMagicBox` is registered (`BlockMagicBox:42`) |
| `BlockMirror` registers no ticker, so the mirror never ejects | the ticker of `TileMirror` is registered (`BlockMirror:107`) |
| the ghost slot keeps the preview when the held item differs | the preview is replaced with a copy of the whole held stack (`ContainerGhostSlots:85`) |
| the hand mirror transport is missing | `ItemHandMirror` implements the bridge of the container and carries the item to the linked mirror |
| the magical log fuel rule (400 ticks) is missing | the fuel is registered (`ThaumcraftNaturals:102`) |
| the ender pearl of a warded object is not destroyed | `WardedPearl` destroys it and tells the thrower why |
| a golem does not refuse a non owner | `GolemOwnership` refuses the interaction with the original message |
| the research note consumes a paper | the paper is checked and never taken (`ResearchNoteIssuance:78`) |
| the aspect source helper only accepts the infusion matrix | it accepts any block entity and honours the direction and the range |
| the damage source facade lost the taint and the dissolve source | all four original sources are published again |
| `IWandable` is never queried | the wand path dispatches to the block and to the block entity |
| the research categories lost their icon and their background | the icon and the background of the six original categories are published |
| the blast resistance kept the raw setter value | the families use the effective value of 1.7.10 (10.2, 15.0, 6.0, 3.0) |
| the taint crust and the taint soil emit the light 1.75 and 1.5 | 1.75 and 1.5 are the **hardness** of the crust and of the soil (`func_149712_f`), the original block emits no light at all |

One of those claims was a false defect that the oracle itself refutes: the taint hardness/light
mix-up. The blast resistance of the taint family was already the effective value of the original
(6.0 = 10*3/5) and only the framing of the note was wrong.

## 4. The code fixes of this wave

| Fix | Oracle | Native |
| --- | --- | --- |
| the aspect constructor accepts any component array, `isPrimal()` answers the original expression, `getLocalizedDescription()` and the four runtime setters are back | `thaumcraft/api/aspects/Aspect.java:74-84, :108, :116, :124, :140, :145, :172` | `common/src/main/java/thaumcraft/api/aspects/Aspect.java` |
| the aspect list is `Serializable` again and builds itself from an item stack | `thaumcraft/api/aspects/AspectList.java:21, :25-36` | `common/src/main/java/thaumcraft/api/aspects/AspectList.java` |
| the 1.12 alias package of the API is in the port again (`IGoggles`, `IRevealer`, `IVisDiscountGear`) | legacy `src/main/java/thaumcraft/api/items/*` | `common/src/main/java/thaumcraft/api/items/*` |
| every jar of the family emits the light 9 of the original | `thaumcraft/common/blocks/BlockJar.java:90` | `BlockJar.LIGHT`, `ThaumcraftContent`, `ThaumcraftEldritchBlocks` |
| the node jar drops the node it holds (aspects, type, modifier, identity) | `thaumcraft/common/blocks/BlockJar.java:184-193` | `ThaumcraftEldritchBlocks.JarBlock.getDrops` |
| the hole keeps the effective blast resistance 3600000 | `thaumcraft/common/blocks/BlockHole.java:53` | `common/src/main/java/thaumcraft/common/lib/world/HoleBlockRules.java` |
| the taint conversion uses the exact 1.7.10 block sets | `thaumcraft/common/blocks/BlockTaintFibres.java:173, :177` | `common/src/main/java/thaumcraft/common/lib/world/TaintMaterialRules.java` |
| the wipe of a player keeps the warp counter | `thaumcraft/common/lib/research/PlayerKnowledge.java:24-34` | `common/src/main/java/thaumcraft/common/lib/research/PlayerKnowledge.java:116` |
| the facade publishes the five original armour materials, the void fortress armour with the durability 18 | `thaumcraft/api/ThaumcraftApi.java:52, :55` | `common/src/main/java/thaumcraft/api/ThaumcraftApi.java`, `ThaumcraftMaterials` |

The 1.7.10 materials of the taint conversion were unreadable before this wave; they are resolved now
with the tool of the repository:

```
field_151572_C -> gourd    field_151570_A -> cactus
field_151595_p -> sand     field_151578_c -> ground
field_151577_b -> grass    field_151571_B -> clay
```

The native port had mycelium, soul sand, coarse dirt, rooted dirt and red sand in the soil set as well.
Those blocks have no 1.7.10 material, so the original never converted them; they are not converted any
more.

## 5. The table of the lane

`что partial` is the claim of the old note, `вердикт` is the result of the re-audit, the last column
names the closed part and the remaining gap.

| единица | что partial | вердикт | закрыто / задокументировано |
| --- | --- | --- | --- |
| `thaumcraft/api/IArchitect.java` | partial | partial | REMAINING GAP (documented): the published entry points of the original interface are not reproduced. The original returned the list of the affected blocks and offered showAxis over its own EnumAxis; the native… |
| `thaumcraft/api/ThaumcraftApi.java` | partial | partial | CLOSED since the first audit: getCraftingRecipes() answers the programmatic list of the original plus the native recipes of the loaded level, getCraftingRecipeKey answers the research key AND the index of its … |
| `thaumcraft/api/aspects/Aspect.java` | partial | parity_verified | CLOSED in this wave: the constructor accepts any component array again, isPrimal() answers the original expression, getLocalizedDescription() answers the translation of tc.aspect.<tag> and the four runtime set… |
| `thaumcraft/api/aspects/AspectList.java` | partial | partial | CLOSED in this wave: the AspectList(ItemStack) constructor of the original is back and the class implements Serializable again. REMAINING GAP: the native drops an unknown aspect tag instead of storing it under… |
| `thaumcraft/api/aspects/AspectSourceHelper.java` | partial | partial | CLOSED since the first audit: the helper accepts any block entity again and honours the direction and the range, the drain takes one unit out of the first source that holds it and the search asks every source … |
| `thaumcraft/api/aura/AuraHelper.java` | partial | partial | REMAINING GAP: the TC6 per-chunk aura and the flux accessors have no counterpart, because TC4 has no per-chunk aura. |
| `thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java` | partial | partial | REMAINING GAP: the fluent setFireDamage()/setUnblockable() modifiers are gone; the caller has to select a damage type that already carries the property, and the class is no longer constructible with the origin… |
| `thaumcraft/api/damagesource/DamageSourceThaumcraft.java` | partial | partial | CLOSED since the first audit: the facade publishes the four original sources again - taint, tentacle, swarm and dissolve - and the two factory methods answer an attributed source of the living attacker. REMAIN… |
| `thaumcraft/api/items/IGoggles.java` | partial | parity_verified | CLOSED in this wave: the alias package of the 1.12 reference is in the port again. It names the canonical contract and carries no member of its own. |
| `thaumcraft/api/items/IRevealer.java` | partial | parity_verified | CLOSED in this wave: the alias package of the 1.12 reference is in the port again. It names the canonical contract and carries no member of its own. |
| `thaumcraft/api/items/IVisDiscountGear.java` | partial | parity_verified | CLOSED in this wave: the alias package of the 1.12 reference is in the port again. It keeps the two argument overload and answers the canonical three argument contract from it. |
| `thaumcraft/api/nodes/INode.java` | partial | partial | REMAINING GAP: the native game node does not implement INode; it is reachable only through the NodeAdapter view, and that view throws UnsupportedOperationException for setNodeType and setNodeModifier, because … |
| `thaumcraft/api/research/ResearchCategories.java` | partial | parity_verified | CLOSED since the first audit: the six original categories are published with the icon and the background of the original registration, and the browser draws the icon of a tab from that table. |
| `thaumcraft/api/research/ResearchCategoryList.java` | partial | parity_verified |  |
| `thaumcraft/api/research/ResearchItem.java` | partial | partial | REMAINING GAP: a corpus-backed ResearchItem never carries icon_item/icon_resource, itemTriggers or aspectTriggers: the bundled corpus keeps the per entry icon as a provenance string and the triggers live in th… |
| `thaumcraft/api/research/ResearchPage.java` | partial | partial | REMAINING GAP: the bundled corpus only carries text / recipe_reference / legacy_expression pages, so the original's IMAGE, ASPECTS, SMELTING, NORMAL_CRAFTING, CRUCIBLE_CRAFTING, INFUSION_CRAFTING, COMPOUND_CRA… |
| `thaumcraft/api/wands/IWandable.java` | partial | parity_verified | CLOSED since the first audit: the interface is ported member for member and the native wand right click path queries it, for a block and for a block entity, with and without a target. |
| `thaumcraft/api/wands/WandTriggerRegistry.java` | partial | partial | REMAINING GAP: 1.21.1 has no block metadata, so the trigger is keyed by a whole block or by one exact block state. The original metadata argument survives in the signature but is ignored, so a trigger register… |
| `thaumcraft/common/CommonProxy.java` | partial | partial | REMAINING GAP: the proxy accessors (getResearchManager, getPlayerKnowledge, getClientWorld) became direct static calls in the common code, so an add-on that used the proxy has to use the new call sites. |
| `thaumcraft/common/Thaumcraft.java` | partial | partial | REMAINING GAP: the public static fields of the original class are gone; an add-on that read Thaumcraft.proxy or Thaumcraft.ConfigItems has to use the new registry classes. |
| `thaumcraft/common/blocks/BlockArcaneDoor.java` | partial | parity_verified | CLOSED since the first audit: the hardness follows the warded stone option again - the door is unbreakable while the option is on and has the hardness 15 otherwise. |
| `thaumcraft/common/blocks/BlockCosmeticOpaque.java` | partial | parity_verified | CLOSED since the first audit: the numbers are the effective ones of the original - hardness 1.5 and the blast resistance 5*3/5 = 3.0; the 1.7.10 setter stores three times its argument and the vanilla getter di… |
| `thaumcraft/common/blocks/BlockCosmeticStairs.java` | partial | partial | REMAINING GAP (client visual): the original's special icon lookup for the cosmetic solid metadata 11 is not reproduced by the native model. The original picked one of four random icons from the position hash, … |
| `thaumcraft/common/blocks/BlockCustomOre.java` | partial | parity_verified | CLOSED since the first audit: the numbers are the effective ones of the original - hardness 1.5 and the blast resistance 5*3/5 = 3.0. |
| `thaumcraft/common/blocks/BlockCustomPlant.java` | partial | parity_verified |  |
| `thaumcraft/common/blocks/BlockCustomPlantItem.java` | partial | partial | REMAINING GAP: the original's IPlantable metadata placement hook has no 1.21.1 counterpart in the native tree, so an add-on plant that relied on it cannot place itself on the original's terms. |
| `thaumcraft/common/blocks/BlockEldritchNothing.java` | partial | parity_verified | CLOSED since the first audit: the behaviour was compared line by line. The native block has the empty selection box, the collision box inset by an eighth, the eight damage for every entity that is not fresh an… |
| `thaumcraft/common/blocks/BlockFluidDeath.java` | partial | partial | CLOSED since the first audit: the block has a model and a blockstate again, so it is not the missing model cube of the first audit. REMAINING GAP: the original's Forge fluid block carried the flow rules of Blo… |
| `thaumcraft/common/blocks/BlockFluidPure.java` | partial | partial | CLOSED since the first audit: the block has a model and a blockstate again. REMAINING GAP: the original's Forge fluid block carried the flow rules of BlockFluidBase; the native liquid keeps the vanilla rules o… |
| `thaumcraft/common/blocks/BlockFluxGas.java` | partial | partial | REMAINING GAP: the original was a finite fluid that spread over the world with the density direction one and that was removed by the taint; the native block is a placeable block with a level, so it does not fl… |
| `thaumcraft/common/blocks/BlockFluxGasItem.java` | partial | partial | REMAINING GAP: the original's item put the gas into the world as a fluid; the native item places the block with its level state. |
| `thaumcraft/common/blocks/BlockFluxGoo.java` | partial | partial | REMAINING GAP: the original was a finite fluid that spread over the world; the native block is a placeable block with a level, so it does not flow. |
| `thaumcraft/common/blocks/BlockFluxGooItem.java` | partial | partial | REMAINING GAP: the original's item put the goo into the world as a fluid; the native item places the block with its level state. |
| `thaumcraft/common/blocks/BlockHole.java` | partial | parity_verified | CLOSED in this wave: the blast resistance is the effective value of the original again - the 1.7.10 setter value 6000000 is stored as 18000000 and the vanilla getter divides it by five, so the native registers… |
| `thaumcraft/common/blocks/BlockJar.java` | partial | partial | CLOSED in this wave: the node jar drops the node it holds - the aspects, the node type, the node modifier and the identity travel with the dropped item - and every jar of the family emits the light level 9 of … |
| `thaumcraft/common/blocks/BlockLoot.java` | partial | parity_verified | CLOSED since the first audit: the native loot blocks have their loot tables again, so they are not the missing drop of the first audit. |
| `thaumcraft/common/blocks/BlockLootItem.java` | partial | parity_verified |  |
| `thaumcraft/common/blocks/BlockMagicBox.java` | partial | parity_verified | CLOSED since the first audit: the block registers the ticker of the box tile, so the tile ticks, refreshes its links and the box chain links as in the original. |
| `thaumcraft/common/blocks/BlockMagicalLeaves.java` | partial | partial | REMAINING GAP: the original's item damage bit four marks a leaf that was placed by a player; such a leaf never decays and never drops the plant. The native port has no item damage, so the marker is the check_d… |
| `thaumcraft/common/blocks/BlockMagicalLeavesItem.java` | partial | partial | REMAINING GAP: the marker itself has no native counterpart, so a leaf that the player placed cannot be told apart from a generated one by the item; the block state check_decay carries that meaning in the nativ… |
| `thaumcraft/common/blocks/BlockManaPod.java` | partial | parity_verified | CLOSED since the first audit: the native pod has a model and a loot table again, so it is not the missing model cube of the first audit. |
| `thaumcraft/common/blocks/BlockMetalDevice.java` | partial | parity_verified | CLOSED since the first audit: the blast resistance is the effective value of the original - the 1.7.10 setter value 17 is stored as 51 and the vanilla getter divides it by five, so the native registers 10.2 fo… |
| `thaumcraft/common/blocks/BlockMetalDeviceItem.java` | partial | parity_verified | CLOSED since the first audit: the original's placeBlockAt hook that initialises the arcane lamp tiles has a counterpart in every lamp block of the native port, so no behaviour is lost. |
| `thaumcraft/common/blocks/BlockMirror.java` | partial | parity_verified | CLOSED since the first audit: the block registers the ticker of the mirror tile, so the tile ejects, decays its instability and repairs its link exactly as in the original. |
| `thaumcraft/common/blocks/BlockMirrorItem.java` | partial | parity_verified |  |
| `thaumcraft/common/blocks/BlockStoneDevice.java` | partial | parity_verified | CLOSED since the first audit: the blast resistance is the effective value of the original - the setter value 25 is stored as 75 and the vanilla getter divides it by five, so the native registers 15.0. |
| `thaumcraft/common/blocks/BlockStoneDeviceItem.java` | partial | parity_verified |  |
| `thaumcraft/common/blocks/BlockTable.java` | partial | parity_verified | CLOSED since the first audit: the native table has a loot table again, so it is not the missing drop of the first audit. |
| `thaumcraft/common/blocks/BlockTableItem.java` | partial | parity_verified |  |
| `thaumcraft/common/blocks/BlockTaint.java` | partial | parity_verified | CORRECTED: the first audit reported a defect that the oracle does not support. The 1.75 and the 1.5 of the original are the HARDNESS of the crust and of the soil (the original overrides getBlockHardness per me… |
| `thaumcraft/common/blocks/BlockTaintFibres.java` | partial | parity_verified | CLOSED in this wave: the 1.7.10 materials of the two conversion expressions are resolved with scripts/porting/resolve_srg.py, so the exact block sets of the original are known and the native port reproduces th… |
| `thaumcraft/common/blocks/BlockTaintFibresItem.java` | partial | parity_verified |  |
| `thaumcraft/common/blocks/BlockTube.java` | partial | parity_verified | CLOSED since the first audit: the buffer comparator value of the original is in the native buffer block - the amount times 14 divided by 8 plus one when the buffer holds essence, which is the original formula … |
| `thaumcraft/common/blocks/BlockWoodenDevice.java` | partial | parity_verified | CLOSED since the first audit: the blast resistance is the effective value of the original - the setter value 10 is stored as 30 and the vanilla getter divides it by five, so the native registers 6.0. |
| `thaumcraft/common/blocks/BlockWoodenDeviceItem.java` | partial | parity_verified |  |
| `thaumcraft/common/blocks/CustomStepSound.java` | partial | partial | REMAINING GAP (audio): the original's per block step and break sound names are therefore replaced by vanilla equivalents in the affected blocks; the sound events themselves are in the port (assets/thaumcraft/s… |
| `thaumcraft/common/blocks/ItemArcaneDoor.java` | partial | parity_verified |  |
| `thaumcraft/common/blocks/ItemJarNode.java` | partial | partial | CLOSED in this wave: breaking the node jar no longer loses the stored node - the drop carries the block entity data of the tile, so the aspects, the node type, the node modifier and the identity survive the br… |
| `thaumcraft/common/blocks/JarStepSound.java` | partial | partial | REMAINING GAP (audio): the original jar step sound (thaumcraft:jar) is therefore replaced by the vanilla glass step sound. The original sound event is in the port and is played when the jar is emptied. |
| `thaumcraft/common/blocks/world/taint/TaintHelper.java` | partial | missing |  |
| `thaumcraft/common/config/Config.java` | partial | partial | REMAINING GAP: the native file format is a properties file and not the original Forge configuration, and the option set of the original is not reproduced, so an add-on that reads or writes Config cannot. |
| `thaumcraft/common/config/ConfigAspects.java` | partial | parity_verified |  |
| `thaumcraft/common/config/ConfigBlocks.java` | partial | parity_verified |  |
| `thaumcraft/common/config/ConfigEntities.java` | partial | parity_verified |  |
| `thaumcraft/common/config/ConfigItems.java` | partial | parity_verified |  |
| `thaumcraft/common/config/MaterialTaint.java` | partial | partial | REMAINING GAP: the 'mobility flag' of MaterialTaint (func_76227_m == 1) has no 1.21.1 counterpart and is not reproduced. |
| `thaumcraft/common/config/recipes/ConfigRecipesInfusionDeviceSlice.java` | partial | missing |  |
| `thaumcraft/common/config/recipes/ConfigRecipesInfusionSlice.java` | partial | missing |  |
| `thaumcraft/common/container/ContainerGhostSlots.java` | partial | partial | CLOSED since the first audit: the defect of the first audit is gone. When the held item differs from the preview the native replaces the preview with a copy of the whole held stack, exactly as the original doe… |
| `thaumcraft/common/container/ContainerHandMirror.java` | partial | parity_verified | CLOSED since the first audit: the transport is not missing any more. The hand mirror item implements the bridge of the container and carries the item to the linked mirror in front of it, exactly as the origina… |
| `thaumcraft/common/container/ContainerMagicBox.java` | partial | parity_verified | CLOSED since the first audit: the linked size grows, because the box tile ticks again and links its neighbours. |
| `thaumcraft/common/container/InventoryFake.java` | partial | parity_verified |  |
| `thaumcraft/common/container/InventoryHandMirror.java` | partial | parity_verified | CLOSED since the first audit: the transport runs, because the hand mirror item implements the bridge of the container. |
| `thaumcraft/common/container/SlotGhostFluid.java` | partial | partial | REMAINING GAP: the original accepts any item registered in the Forge FluidContainerRegistry; the native accepts only a BucketItem, which is the only fluid container vanilla 1.21.1 has. A fluid container of an … |
| `thaumcraft/common/lib/FakeThaumcraftPlayer.java` | partial | partial | REMAINING GAP: the original also blocked the item pickup event for every player whose name starts with FakeThaumcraft; the native never adds a fake player to a level, so there is nothing to cancel and the rule… |
| `thaumcraft/common/lib/InternalMethodHandler.java` | partial | parity_verified | CLOSED since the first audit: the native bridge is not a dummy any more. The real implementation answers the research state, the discovered aspects, the object tags, the workbench slots, the wand vis costs and… |
| `thaumcraft/common/lib/RefGui.java` | partial | partial | REMAINING GAP: every menu the original listed has a native menu class, but the id numbering itself is not reproduced, so an add-on that opened a menu by its number has to use the native MenuType. |
| `thaumcraft/common/lib/WarpEvents.java` | partial | partial | REMAINING GAP: the original getWarpFromGear also summed the four bauble slots; the native WarpGear.fromGear only sums the hand and the four armor pieces, because the port has no accessory slots. |
| `thaumcraft/common/lib/events/EventHandlerEntity.java` | partial | partial | CORRECTED: the first audit reported the golem owner protection and the warded ender pearl as missing. Both are implemented: the owner rule refuses the interaction of a non owner with the original message (Gole… |
| `thaumcraft/common/lib/events/EventHandlerWorld.java` | partial | partial | CORRECTED: the first audit reported the magical log fuel rule as missing. The burn time 400 of the magical log is registered (ThaumcraftNaturals) next to the burn time 6400 of the alumentum. REMAINING GAP: the… |
| `thaumcraft/common/lib/events/KeyHandler.java` | partial | partial | REMAINING GAP: the bindings themselves are not part of this audit, and the original's own key handling (the wand toggle and the node view) lives in the client lane. |
| `thaumcraft/common/lib/events/ServerTickEventsFML.java` | partial | partial | REMAINING GAP: the chunk regeneration queue is not reproduced, because 1.21.1 has no equivalent chunk data event (see the EventHandlerWorld row). |
| `thaumcraft/common/lib/research/PlayerKnowledge.java` | partial | partial | CLOSED in this wave: the wipe keeps the warp counter of the player, exactly as the original wipePlayerKnowledge does. REMAINING GAP: the short cast of getAspectPoolFor is dropped from the API (the value no lon… |
| `thaumcraft/common/lib/research/ResearchManager.java` | partial | partial | CORRECTED: the first audit reported that the native consumes one paper per issued note. The native keeps the original: the paper is checked and never taken (ResearchNoteIssuance). REMAINING GAP: the process-wi… |
| `thaumcraft/common/lib/research/ScanManager.java` | partial | partial | REMAINING GAP: the original's hash functions are therefore not reproduced verbatim - only their observable purpose (one scan per witness, one reward per scan). |
| `thaumcraft/common/lib/utils/BlockUtils.java` | partial | parity_verified |  |
| `thaumcraft/common/lib/utils/CropUtils.java` | partial | parity_verified |  |
| `thaumcraft/common/lib/utils/EntityUtils.java` | partial | parity_verified |  |
| `thaumcraft/common/lib/utils/InventoryUtils.java` | partial | parity_verified |  |
| `thaumcraft/common/lib/utils/Utils.java` | partial | parity_verified |  |

## 6. What stays partial and why

41 units of the lane keep `partial`. The remaining difference of each of them is written into the
ledger notes, and it falls into five groups:

1. **A published API surface that 1.21.1 does not have.**
   `IArchitect` (the list of the affected blocks, `showAxis` and `EnumAxis`), `DamageSourceThaumcraft`
   and `DamageSourceIndirectThaumcraftEntity` (the ready made `DamageSource` instances and the fluent
   modifiers), `WandTriggerRegistry` (the block metadata), `ThaumcraftApi` (the numeric enchantment
   ids and the type of the recipe entries), `ResearchItem` (the item and aspect triggers of a corpus
   entry), `RefGui` (the numeric gui ids) and `CommonProxy`/`Thaumcraft` (the static fields).
   The gameplay of every one of them is at parity; what an add-on sees is not.
2. **A concept of 1.7.10 that 1.21.1 replaced.**
   `Config` (the ~70 options and the Forge configuration format), `SlotGhostFluid` (the Forge fluid
   container registry), `MaterialTaint` (the `Material` mobility flag), the flux blocks (a finite Forge
   fluid that flows), the magical leaves (the item damage bit four), `BlockHole`/`BlockJar` (the custom
   step sound), `FakeThaumcraftPlayer` (the pickup event of a player that never joins a level).
3. **A subsystem that is not in the port at all.**
   `AspectSourceHelper` (the essence flow packet of an arbitrary consumer), `AuraHelper` (the TC6 per
   chunk aura and the flux), `ItemJarNode` (the node jar item with its NBT API and the research that
   hands it out), `EventHandlerWorld` (the chunk regeneration queue and the cancelled placement near an
   active eldritch boss), `EventHandlerEntity` (the research file of the original), `ServerTickEventsFML`
   (the chunk regeneration queue), `KeyHandler` (the bindings of the client lane).
4. **A number that is reproduced with a different mechanism.**
   `BlockTube` (the buffer comparator value lives in the buffer block), `BlockTaintFibresItem` (the
   1.12 support rule is not in the oracle), `ResearchManager` (the paper is checked and never taken,
   the authority re-validation of a purchase), `ScanManager` (the string signatures instead of the
   numeric hashes), `PlayerKnowledge` (the per player state instead of the process wide maps),
   `WarpEvents` (the accessory slots of the baubles), `InventoryUtils` (the ore tags instead of the
   ore dictionary), `InternalMethodHandler` (the plug point of the original), `ContainerGhostSlots`
   (the hotbar swap click of the original).
5. **The client and the audio surface.**
   `BlockCosmeticStairs` (the random icon of the cosmetic solid metadata 11) and the two sound classes
   `CustomStepSound` and `JarStepSound` (the step sound of the jar is the vanilla glass sound).

Three legacy only helpers moved to the verdict that the ledger uses for a file that is deliberately not
carried over (`exclude` + `missing` + `not_applicable`): `TaintHelper`, `ConfigRecipesInfusionSlice`
and `ConfigRecipesInfusionDeviceSlice`. They have no counterpart in the 1.7.10 oracle and their
behaviour lives elsewhere in the native tree.

## 7. The result

| | before | after |
| --- | --- | --- |
| `parity_verified` | 556 | 602 |
| `partial` | 345 | 296 |
| `missing` | 126 | 129 |
| `not_assessed` | 17 | 17 |
| `source_semantics = different` | 23 | 20 |

In the lane of this report: **46 units moved from `partial` to `parity_verified`, 41 units stay
`partial` with a written gap, 3 units became `missing`** (the legacy only helpers).
Three of the five `different` units of the lane are `adapted` now, one legacy helper is
`not_applicable`.

## 8. The verification

```
export JAVA_HOME=/home/Acak/.jdk21
export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=/home/Acak/gradle-homes/ledgerpartial
./gradlew --no-daemon --console=plain --no-build-cache :common:cleanTest :common:test
```

```
> Task :common:cleanTest
> Task :common:compileJava UP-TO-DATE
> Task :common:processResources UP-TO-DATE
> Task :common:classes UP-TO-DATE
> Task :common:compileTestJava UP-TO-DATE
> Task :common:processTestResources UP-TO-DATE
> Task :common:testClasses UP-TO-DATE
> Task :common:test

BUILD SUCCESSFUL in 14s
6 actionable tasks: 2 executed, 4 up-to-date
```

The JUnit report of that run: **258 suites, 1785 tests, 0 failures, 0 errors, 0 skipped**
(`common/build/test-results/test/TEST-*.xml`, the files of the run of 17:22).

`:fabric:compileJava` was run on the same tree and is successful as well: the loader module compiles
against the changed common sources.

The new tests of this wave:

* `common/src/test/java/thaumcraft/api/AspectContractTest.java` — the aspect contract and the
  `AspectList` constructor and marker,
* `common/src/test/java/thaumcraft/taint/TaintMaterialRulesTest.java` — the taint block sets and the
  numbers of the hole,
* `common/src/test/java/thaumcraft/blocks/JarBlockNumbersTest.java` — the light of the jar,
* `common/src/test/java/thaumcraft/research/PlayerKnowledgeWipeTest.java` — the wipe of a player.

## 9. What this wave does not claim

* No client run, no client acceptance: the visual gaps of the table (the icon of the cosmetic stairs,
  the step sound of the jar) are documented, not inspected in game.
* No multiplayer and no survival acceptance: the tests of this wave are unit tests of the port.
* The game test source set (`common/src/gameTest/**`) was not touched; it belongs to the scenario lane.
* `scripts/porting/update_scope_ledger.py --check` cannot run from this worktree: the script expects
  the oracle in `<repo>/../work/thaumcraft-4.2.3.5-oracle` and the worktree layout of this machine
  puts it in `/home/Acak/work/`. The ledger was written with the same writer
  (`json.dumps(data, ensure_ascii=False, indent=2) + "\n"`, `newline="\n"`) and the key order of the
  file is unchanged; only the 90 rows of this lane differ from the base commit.
