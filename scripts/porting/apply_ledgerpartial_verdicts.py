#!/usr/bin/env python3
"""Apply the ledgerpartial review verdicts to docs/porting/scope-ledger.json.

Only the units of the ledgerpartial lane are touched: the 345 units that were marked
``native_coverage = partial`` and the 23 units that were marked
``source_semantics = different`` outside the lanes of the other gap agents (the client
renderers and the FX, the scenarios, the crafting, the entities and the tiles, the world
generation and the golems, the items and the network).

Every entry was re-audited against the 4.2.3.5 oracle and against the native tree of this
branch. A unit that the re-audit found to be covered keeps its authored fingerprints, so the
ledger stays refreshable with scripts/porting/update_scope_ledger.py.

Usage:
    python3 -B scripts/porting/apply_ledgerpartial_verdicts.py [--check]
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LEDGER = ROOT / "docs/porting/scope-ledger.json"
OWNER = "port-ledgerpartial"

# path -> (native_coverage, source_semantics, disposition, evidence, notes)
VERDICTS: dict[str, tuple[str, str | None, str | None, list[str], list[str]]] = {
    # ------------------------------------------------------------------ public api
    "thaumcraft/api/IArchitect.java": (
        "partial", "different", None,
        ["oracle thaumcraft/api/IArchitect.java:18 ArrayList<BlockCoordinates> getArchitectBlocks(ItemStack, World, int, int, int, int, EntityPlayer)",
         "oracle thaumcraft/api/IArchitect.java:20 boolean showAxis(ItemStack, World, EntityPlayer, int, EnumAxis)",
         "oracle thaumcraft/api/IArchitect.java:22 enum EnumAxis {X, Y, Z}",
         "native common/src/main/java/thaumcraft/api/IArchitect.java:7 int getMaxAreaSize(ItemStack focus)",
         "native common/src/main/java/thaumcraft/api/IArchitect.java:9 default int getAxisModes()",
         "native common/src/main/java/thaumcraft/common/items/wands/WandArchitect.java:64 public static Area area(ItemStack wand, ItemStack focus)",
         "native common/src/main/java/thaumcraft/common/items/wands/WandArchitect.java:82 public static List<BlockPos> planarBlocks(...)"],
        ["The gameplay of the Architect focus is at parity: the bounded area, the number of the selectable axes and the connected traversal live in WandArchitect and are driven by the native block states.",
         "REMAINING GAP (documented): the published entry points of the original interface are not reproduced. The original returned the list of the affected blocks and offered showAxis over its own EnumAxis; the native interface answers the area size and the number of the axes.",
         "An add-on that implements the original contract therefore still cannot compile against the native interface, and the native has no BlockCoordinates type (that unit is excluded for the same reason)."]),
    "thaumcraft/api/ThaumcraftApi.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/api/ThaumcraftApi.java:48-60 the tool, armour and enchantment constants",
         "oracle thaumcraft/api/ThaumcraftApi.java:88-99 getSmeltingBonus answers the exact entry, the wildcard entry and the first ore name",
         "oracle thaumcraft/api/ThaumcraftApi.java:100 List getCraftingRecipes()",
         "oracle thaumcraft/api/ThaumcraftApi.java:161 Object[] getCraftingRecipeKey(EntityPlayer, ItemStack)",
         "native common/src/main/java/thaumcraft/api/ThaumcraftApi.java:101 getCraftingRecipes() answers the programmatic list and the native recipes",
         "native common/src/main/java/thaumcraft/api/ThaumcraftApi.java:121 getSmeltingBonus falls back to the first original ore name",
         "native common/src/main/java/thaumcraft/api/ThaumcraftApi.java:280 getCraftingRecipeKey answers the research key and the index of its page",
         "native common/src/main/java/thaumcraft/api/ThaumcraftApi.java:60-72 the five original armour materials"],
        ["The registry, the object-tag maps, the warp map, the smelting-bonus map, the scan-event list and the entity-tag list are ported and are public and mutable as in the original.",
         "CLOSED since the first audit: getCraftingRecipes() answers the programmatic list of the original plus the native recipes of the loaded level, getCraftingRecipeKey answers the research key AND the index of its page, getSmeltingBonus falls back to the first original ore name of the input and the facade publishes the five original armour materials.",
         "REMAINING GAP: the five enchantment ids stay -1, because a 1.21.1 enchantment is a datapack record without a numeric id.",
         "REMAINING GAP: the native recipe list holds the native recipe views and not the original IArcaneRecipe objects, so an add-on that casts an entry to the original type still fails."]),
    "thaumcraft/api/aspects/Aspect.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/api/aspects/Aspect.java:74-84 the constructor accepts any component array",
         "oracle thaumcraft/api/aspects/Aspect.java:108 getLocalizedDescription()",
         "oracle thaumcraft/api/aspects/Aspect.java:116 setTag(String)",
         "oracle thaumcraft/api/aspects/Aspect.java:124 setComponents(Aspect[])",
         "oracle thaumcraft/api/aspects/Aspect.java:140 setBlend(int)",
         "oracle thaumcraft/api/aspects/Aspect.java:145 isPrimal() answers null or a length that is not two",
         "oracle thaumcraft/api/aspects/Aspect.java:172 setChatcolor(String)",
         "native common/src/main/java/thaumcraft/api/aspects/Aspect.java the 48 constants, the constructor, isPrimal and the four setters"],
        ["All 48 aspects exist with the same tags, the same colours, the same blend values, the same chat colours and the same component pairs; the machine-checkable corpus in data/thaumcraft/tc4/aspects.json matches the oracle line for line.",
         "CLOSED in this wave: the constructor accepts any component array again, isPrimal() answers the original expression, getLocalizedDescription() answers the translation of tc.aspect.<tag> and the four runtime setters setTag, setComponents, setBlend and setChatcolor are back.",
         "The native still hands out a copy of the component array instead of the array itself; an add-on that writes into the returned array does not change the aspect, which the original allowed."]),
    "thaumcraft/api/aspects/AspectList.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/api/aspects/AspectList.java:21 public class AspectList implements Serializable",
         "oracle thaumcraft/api/aspects/AspectList.java:25 public AspectList(ItemStack stack)",
         "oracle thaumcraft/api/aspects/AspectList.java:129 public boolean reduce(Aspect, int)",
         "oracle thaumcraft/api/aspects/AspectList.java:191 this.add(Aspect.getAspect(...), ...) accepts a null aspect",
         "native common/src/main/java/thaumcraft/api/aspects/AspectList.java the ItemStack constructor and the Serializable marker"],
        ["add, merge, remove, reduce, copy, size, visSize, getAmount, getAspects, getPrimalAspects, getAspectsSorted, getAspectsSortedAmount and both NBT forms are ported with the same amounts.",
         "CLOSED in this wave: the AspectList(ItemStack) constructor of the original is back and the class implements Serializable again.",
         "REMAINING GAP: the native drops an unknown aspect tag instead of storing it under a null key, so an entry with an unknown tag is not counted by size() and is not answered by getAmount.",
         "The native also adds reduceToPrimals(boolean) and getAspectsSortedByAmount(), which have no original counterpart."]),
    "thaumcraft/api/aspects/AspectSourceHelper.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/api/aspects/AspectSourceHelper.java:21 drainEssentia(TileEntity, Aspect, ForgeDirection, int)",
         "oracle thaumcraft/api/aspects/AspectSourceHelper.java:35 findEssentia(TileEntity, Aspect, ForgeDirection, int)",
         "oracle thaumcraft/common/lib/events/EssentiaHandler.java:39 the handler searches any IAspectSource in the range",
         "oracle thaumcraft/common/lib/events/EssentiaHandler.java:54 the original sends the essence flow packet to the players around the tile",
         "native common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java:19 drainEssentia(BlockEntity, Aspect, Direction, int)",
         "native common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java:38 findEssentia(BlockEntity, Aspect, Direction, int)",
         "native common/src/main/java/thaumcraft/common/lib/essentia/EssentiaSourceSearch.java the original half box search"],
        ["CLOSED since the first audit: the helper accepts any block entity again and honours the direction and the range, the drain takes one unit out of the first source that holds it and the search asks every source whether it holds the aspect.",
         "REMAINING GAP: the original sent its essence flow packet to the players around the tile; the native has no flow effect for an arbitrary consumer, only the infusion matrix keeps its own effect."]),
    "thaumcraft/api/aura/AuraHelper.java": (
        "partial", "adapted", None,
        ["legacy src/main/java/thaumcraft/api/aura/AuraHelper.java the TC6 aura helper with the per chunk aura and the flux accessors",
         "native common/src/main/java/thaumcraft/api/aura/AuraHelper.java:16 public final class AuraHelper",
         "native common/src/main/java/thaumcraft/api/aura/AuraHelper.java:29 public static TileNode node(Level level, BlockPos pos)"],
        ["Not an original TC4 class: a TC6 helper. The native projects it onto the aura node: the vis accessors read and write the node at the position.",
         "REMAINING GAP: the TC6 per-chunk aura and the flux accessors have no counterpart, because TC4 has no per-chunk aura."]),
    "thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java:21 public DamageSourceIndirectThaumcraftEntity(String, Entity, Entity)",
         "oracle thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java:25 the fire damage modifier",
         "oracle thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java:30 the unblockable modifier",
         "native common/src/main/java/thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java:21 public static DamageSource create(Entity direct, Entity cause, Holder<DamageType> type)"],
        ["Forced 1.21.1 change: 1.21.1 has no EntityDamageSourceIndirect, so the class became a factory over the registered damage type; the source names the direct attacker and the real cause exactly as the original did.",
         "REMAINING GAP: the fluent setFireDamage()/setUnblockable() modifiers are gone; the caller has to select a damage type that already carries the property, and the class is no longer constructible with the original signature."]),
    "thaumcraft/api/damagesource/DamageSourceThaumcraft.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/api/damagesource/DamageSourceThaumcraft.java:19 the taint source, bypasses armour and magic damage",
         "oracle thaumcraft/api/damagesource/DamageSourceThaumcraft.java:20 tentacle, :21 swarm, :22 dissolve",
         "oracle thaumcraft/api/damagesource/DamageSourceThaumcraft.java:36 causeSwarmDamage, :40 causeTentacleDamage",
         "native common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:22-25 the four damage type keys",
         "native common/src/main/resources/data/thaumcraft/damage_type/taint.json the taint damage type"],
        ["CLOSED since the first audit: the facade publishes the four original sources again - taint, tentacle, swarm and dissolve - and the two factory methods answer an attributed source of the living attacker.",
         "REMAINING GAP: 1.21.1 has no DamageSource subclass, so the original's ready made source instances became the keys of the registered damage types plus the factories that build them.",
         "REMAINING GAP: the 'bypasses armour' and 'magic' flags of the original taint source live on the entity type tags in 1.21.1 and not on the damage type, so they are not carried by the source."]),
    "thaumcraft/api/items/IGoggles.java": (
        "parity_verified", "equivalent", None,
        ["legacy src/main/java/thaumcraft/api/items/IGoggles.java:3 public interface IGoggles extends thaumcraft.api.IGoggles",
         "native common/src/main/java/thaumcraft/api/items/IGoggles.java the alias of the canonical contract",
         "native common/src/main/java/thaumcraft/api/IGoggles.java the canonical contract"],
        ["CLOSED in this wave: the alias package of the 1.12 reference is in the port again. It names the canonical contract and carries no member of its own.",
         "The canonical contract is the one the native items implement: ItemGoggles, ItemVoidRobeArmor and ItemFortressArmor."]),
    "thaumcraft/api/items/IRevealer.java": (
        "parity_verified", "equivalent", None,
        ["legacy src/main/java/thaumcraft/api/items/IRevealer.java:3 public interface IRevealer extends thaumcraft.api.nodes.IRevealer",
         "native common/src/main/java/thaumcraft/api/items/IRevealer.java the alias of the canonical contract",
         "native common/src/main/java/thaumcraft/api/nodes/IRevealer.java the canonical contract"],
        ["CLOSED in this wave: the alias package of the 1.12 reference is in the port again. It names the canonical contract and carries no member of its own.",
         "The canonical contract is the one the native items implement: ItemGoggles, ItemVoidRobeArmor and ItemFortressArmor."]),
    "thaumcraft/api/items/IVisDiscountGear.java": (
        "parity_verified", "equivalent", None,
        ["legacy src/main/java/thaumcraft/api/items/IVisDiscountGear.java the two argument overload and the default three argument answer",
         "native common/src/main/java/thaumcraft/api/items/IVisDiscountGear.java the alias with the two argument overload",
         "native common/src/main/java/thaumcraft/api/IVisDiscountGear.java the canonical three argument contract"],
        ["CLOSED in this wave: the alias package of the 1.12 reference is in the port again. It keeps the two argument overload and answers the canonical three argument contract from it.",
         "The canonical contract is the one the native items implement: ItemHoverHarness, ItemGoggles, the cultist and the void robes and the cultist boots."]),
    "thaumcraft/api/nodes/INode.java": (
        "partial", "equivalent", None,
        ["oracle thaumcraft/api/nodes/INode.java:14-28 the nine members of the contract",
         "native common/src/main/java/thaumcraft/api/nodes/INode.java the nine members of the contract",
         "native common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java setNodeType and setNodeModifier throw UnsupportedOperationException"],
        ["The interface itself is ported member for member.",
         "REMAINING GAP: the native game node does not implement INode; it is reachable only through the NodeAdapter view, and that view throws UnsupportedOperationException for setNodeType and setNodeModifier, because the native node fixes them when it is created.",
         "The native also adds NodeBase, a self-contained INode implementation an add-on can subclass."]),
    "thaumcraft/api/research/ResearchCategories.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/config/ConfigResearch.java:67-73 the six categories with the icon and the background of their tab",
         "native common/src/main/java/thaumcraft/api/research/ResearchCategories.java:20 the public map",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftResearchCategories.java:24 the icon and the background of every original category"],
        ["CLOSED since the first audit: the six original categories are published with the icon and the background of the original registration, and the browser draws the icon of a tab from that table.",
         "The public map stays mutable and the original registration methods (registerCategory, addResearch) still work, so an add-on may add its own category and research.",
         "The original's linear getResearch scan was replaced by a map lookup; the observable answer is identical."]),
    "thaumcraft/api/research/ResearchCategoryList.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/api/research/ResearchCategoryList.java:14-21 the class with the icon, the background and the research map",
         "native common/src/main/java/thaumcraft/api/research/ResearchCategoryList.java the LinkedHashMap of the research",
         "native common/src/main/java/thaumcraft/common/lib/research/ResearchRegistrationOrder.java the original per category HashMap order"],
        ["Forced change: HashMap -> LinkedHashMap so the browser draws the cells in a stable order; this is documented in the class comment.",
         "The original's HashMap iteration order is reproduced where it is observable (the RNG selection) by ResearchRegistrationOrder, which rebuilds the original per-category HashMap from the corpus provenance.",
         "The icon and the background of the six original categories are published; see the ResearchCategories row."]),
    "thaumcraft/api/research/ResearchItem.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/api/research/ResearchItem.java:19-29 the fields of the entry",
         "oracle thaumcraft/api/research/ResearchItem.java:90-135 the flag setters",
         "native common/src/main/java/thaumcraft/api/research/ResearchItem.java:81 of(ResearchDefinition) reads every original flag",
         "native common/src/main/java/thaumcraft/common/lib/research/ResearchTriggerBindings.java the item triggers of the built in entries"],
        ["The builder API is preserved and every original flag (special, stub, lost, concealed, hidden, virtual, round, auto unlock, secondary) is read from the corpus.",
         "REMAINING GAP: a corpus-backed ResearchItem never carries icon_item/icon_resource, itemTriggers or aspectTriggers: the bundled corpus keeps the per entry icon as a provenance string and the triggers live in the corpus and in ResearchTriggerBindings.",
         "An add-on that reads getItemTriggers()/getAspectTriggers() from a corpus entry sees null/empty instead of the original's executable triggers."]),
    "thaumcraft/api/research/ResearchPage.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/api/research/ResearchPage.java:25-32 the fields of a page",
         "oracle thaumcraft/api/research/ResearchPage.java:34-112 every page constructor of the original",
         "native common/src/main/java/thaumcraft/api/research/ResearchPage.java:128 of(ResearchDefinition.Page)"],
        ["Forced 1.21.1 changes: IRecipe became Recipe, the furnace lookup needs a Level, StatCollector became Component.",
         "Every page kind and every constructor of the original is present in the native class.",
         "REMAINING GAP: the bundled corpus only carries text / recipe_reference / legacy_expression pages, so the original's IMAGE, ASPECTS, SMELTING, NORMAL_CRAFTING, CRUCIBLE_CRAFTING, INFUSION_CRAFTING, COMPOUND_CRAFTING and INFUSION_ENCHANTMENT kinds are never produced from the corpus; of() maps legacy_expression to IMAGE, which is a lossy but presentation-only mapping."]),
    "thaumcraft/api/wands/IWandable.java": (
        "parity_verified", "equivalent", None,
        ["oracle thaumcraft/api/wands/IWandable.java:16-22 the four members of the contract",
         "native common/src/main/java/thaumcraft/api/wands/IWandable.java the four members of the contract",
         "native common/src/main/java/thaumcraft/common/items/wands/ItemWandCasting.java:157 and :161 the wand dispatches to the block and to the block entity",
         "native common/src/main/java/thaumcraft/common/items/wands/ItemWandCasting.java:213 and :217 the contract without a target",
         "native common/src/main/java/thaumcraft/common/tiles/TileJarNode.java:27 the node jar implements the contract"],
        ["CLOSED since the first audit: the interface is ported member for member and the native wand right click path queries it, for a block and for a block entity, with and without a target.",
         "The node jar of the native port implements the contract, so the original's own use of it is covered."]),
    "thaumcraft/api/wands/WandTriggerRegistry.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/api/wands/WandTriggerRegistry.java:25 registerWandBlockTrigger(IWandTriggerManager, int, Block, int, String)",
         "oracle thaumcraft/api/wands/WandTriggerRegistry.java:38 hasTrigger(Block, int)",
         "oracle thaumcraft/api/wands/WandTriggerRegistry.java:55 performTrigger(..., Block, int)",
         "native common/src/main/java/thaumcraft/api/wands/WandTriggerRegistry.java the block and the block state keys"],
        ["The registry, the modid separation, the -1 wildcard meaning 'any metadata' and the first successful trigger wins are all ported.",
         "REMAINING GAP: 1.21.1 has no block metadata, so the trigger is keyed by a whole block or by one exact block state. The original metadata argument survives in the signature but is ignored, so a trigger registered for one metadata now fires for all states of that block."]),

    # ------------------------------------------------------------------ common bootstrap
    "thaumcraft/common/CommonProxy.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/CommonProxy.java the proxy with the research, the knowledge and the client world accessors",
         "native common/src/main/java/thaumcraft/common/Thaumcraft.java the common bootstrap",
         "fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabric.java the platform bootstrap"],
        ["The 1.7 CommonProxy is replaced by the Architectury layout: a common bootstrap plus one platform bootstrap per loader.",
         "REMAINING GAP: the proxy accessors (getResearchManager, getPlayerKnowledge, getClientWorld) became direct static calls in the common code, so an add-on that used the proxy has to use the new call sites."]),
    "thaumcraft/common/Thaumcraft.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/Thaumcraft.java the @Mod singleton with the static registry fields",
         "native common/src/main/java/thaumcraft/common/Thaumcraft.java the bootstrap with the init sequence",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftBlocks.java and the other per-domain registries"],
        ["The 1.7 @Mod singleton became a plain bootstrap class; the original static registry fields (ConfigItems, ConfigBlocks, ConfigEntities, Thaumcraft.proxy) were split into the per-domain Thaumcraft* config classes.",
         "The original init order (aspects, blocks, items, research, entities) is kept as an explicit init sequence.",
         "REMAINING GAP: the public static fields of the original class are gone; an add-on that read Thaumcraft.proxy or Thaumcraft.ConfigItems has to use the new registry classes."]),

    # ------------------------------------------------------------------ blocks
    "thaumcraft/common/blocks/BlockArcaneDoor.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockArcaneDoor.java:67 setResistance(999.0f)",
         "oracle thaumcraft/common/blocks/BlockArcaneDoor.java:68 setHardness(Config.wardedStone ? -1.0f : 15.0f)",
         "native common/src/main/java/thaumcraft/common/blocks/BlockArcaneDoor.java:73 getDestroyProgress returns zero while the warded stone option is on",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java the door with the effective blast resistance 599.4"],
        ["The four metadata values of the original became the native block states of one door block (facing, open, half, flip).",
         "CLOSED since the first audit: the hardness follows the warded stone option again - the door is unbreakable while the option is on and has the hardness 15 otherwise.",
         "The blast resistance keeps the effective value of the original: the 1.7.10 setter value 999 is stored as 2997 and the vanilla getter divides it by five, so the native registers 599.4."]),
    "thaumcraft/common/blocks/BlockCosmeticOpaque.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockCosmeticOpaque.java:66 setResistance(5.0f)",
         "oracle thaumcraft/common/blocks/BlockCosmeticOpaque.java:67 setHardness(1.5f)",
         "native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:343 the hardness 1.5",
         "native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:344 the effective blast resistance 3.0"],
        ["The metadata values became separate native blocks (ThaumcraftEldritchBlocks).",
         "CLOSED since the first audit: the numbers are the effective ones of the original - hardness 1.5 and the blast resistance 5*3/5 = 3.0; the 1.7.10 setter stores three times its argument and the vanilla getter divides the stored value by five."]),
    "thaumcraft/common/blocks/BlockCosmeticStairs.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockCosmeticStairs.java:37-40 the icon of the stairs delegates to the cosmetic solid of the metadata 11",
         "oracle thaumcraft/common/blocks/BlockCosmeticSolid.java:157-162 the random icon of the metadata 11 and 13",
         "native common/src/main/java/thaumcraft/common/blocks/BlockCosmeticStairs.java the native stairs"],
        ["The native stairs blocks are registered per material.",
         "REMAINING GAP (client visual): the original's special icon lookup for the cosmetic solid metadata 11 is not reproduced by the native model. The original picked one of four random icons from the position hash, so the stairs of that metadata show a fixed texture in the native port."]),
    "thaumcraft/common/blocks/BlockCustomOre.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockCustomOre.java:59 setResistance(5.0f)",
         "oracle thaumcraft/common/blocks/BlockCustomOre.java:60 setHardness(1.5f)",
         "native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java the effective numbers of the ore family"],
        ["The infused stone variants became separate blocks with their own ids instead of one metadata block.",
         "CLOSED since the first audit: the numbers are the effective ones of the original - hardness 1.5 and the blast resistance 5*3/5 = 3.0."]),
    "thaumcraft/common/blocks/BlockCustomPlant.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockCustomPlant.java the plant family",
         "native common/src/main/java/thaumcraft/common/blocks/BlockCustomPlant.java the native plants",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java the ethereal bloom as its own block"],
        ["The metadata values became separate native blocks; the ethereal bloom is now its own block.",
         "The growth, the drops and the tint of every plant of the original are in the native plant blocks and in their loot tables."]),
    "thaumcraft/common/blocks/BlockCustomPlantItem.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockCustomPlantItem.java the item of the plant family",
         "native common/src/main/java/thaumcraft/common/blocks/BlockCustomPlantItem.java the native item"],
        ["1.21.1 has no item metadata; the six plant variants became separate block ids.",
         "REMAINING GAP: the original's IPlantable metadata placement hook has no 1.21.1 counterpart in the native tree, so an add-on plant that relied on it cannot place itself on the original's terms."]),
    "thaumcraft/common/blocks/BlockEldritchNothing.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockEldritchNothing.java:49 setResistance(6000000.0f)",
         "oracle thaumcraft/common/blocks/BlockEldritchNothing.java:51 setLightLevel(0.2f)",
         "oracle thaumcraft/common/blocks/BlockEldritchNothing.java:93-100 the block entity only exists while the block is exposed",
         "oracle thaumcraft/common/blocks/BlockEldritchNothing.java:102-106 eight damage for everything that is not a fresh entity",
         "oracle thaumcraft/common/blocks/BlockEldritchNothing.java:108-111 the collision box inset by an eighth",
         "native common/src/main/java/thaumcraft/common/blocks/BlockEldritchNothing.java the exposed state, the empty selection box and the eight damage"],
        ["CLOSED since the first audit: the behaviour was compared line by line. The native block has the empty selection box, the collision box inset by an eighth, the eight damage for every entity that is not fresh and not invulnerable, the exposed state that carries the block entity and the light level 3.",
         "Forced 1.21.1 change: 1.21.1 has no per block renderer, so the original client renderer of the invisible wall was replaced by a block state with an exposed flag.",
         "The native has no item for the block, so the pick block answer of the original (null) is the 1.21.1 default."]),
    "thaumcraft/common/blocks/BlockFluidDeath.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockFluidDeath.java the Forge fluid block",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftLegacyTiles.java:46 the native death fluid",
         "native common/src/main/resources/assets/thaumcraft/blockstates/death_fluid.json the model of the fluid"],
        ["The original's Forge fluid block became a native liquid block with a fluid type.",
         "CLOSED since the first audit: the block has a model and a blockstate again, so it is not the missing model cube of the first audit.",
         "REMAINING GAP: the original's Forge fluid block carried the flow rules of BlockFluidBase (the flow distance, the waterlogging and the block damage of the fluid); the native liquid keeps the vanilla rules of 1.21.1."]),
    "thaumcraft/common/blocks/BlockFluidPure.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockFluidPure.java the Forge fluid block",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftSpa.java:37 the native purifying fluid",
         "native common/src/main/resources/assets/thaumcraft/blockstates/purifying_fluid.json the model of the fluid"],
        ["The original's Forge fluid block became a native liquid block with a fluid type.",
         "CLOSED since the first audit: the block has a model and a blockstate again.",
         "REMAINING GAP: the original's Forge fluid block carried the flow rules of BlockFluidBase; the native liquid keeps the vanilla rules of 1.21.1."]),
    "thaumcraft/common/blocks/BlockFluxGas.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockFluxGas.java:44-49 the finite Forge fluid with the density direction one",
         "native common/src/main/java/thaumcraft/common/blocks/BlockFluxGas.java the native gas block with the level property",
         "native common/src/main/resources/assets/thaumcraft/models/block/flux_gas_0.json the model of the gas"],
        ["The original's Forge fluid block became a native block with a level property; the native block has a model again.",
         "REMAINING GAP: the original was a finite fluid that spread over the world with the density direction one and that was removed by the taint; the native block is a placeable block with a level, so it does not flow."]),
    "thaumcraft/common/blocks/BlockFluxGasItem.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockFluxGasItem.java the item of the gas fluid",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftFlux.java:30 the native gas block"],
        ["1.21.1 has no item metadata and no per-metadata stack limit; the gas level is a block state.",
         "REMAINING GAP: the original's item put the gas into the world as a fluid; the native item places the block with its level state."]),
    "thaumcraft/common/blocks/BlockFluxGoo.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockFluxGoo.java the finite Forge fluid",
         "native common/src/main/java/thaumcraft/common/blocks/BlockFluxGoo.java the native goo block with the level property",
         "native common/src/main/resources/assets/thaumcraft/models/block/flux_goo_0.json the model of the goo"],
        ["The original's Forge fluid block became a native block with a level property; the native block has a model again.",
         "REMAINING GAP: the original was a finite fluid that spread over the world; the native block is a placeable block with a level, so it does not flow."]),
    "thaumcraft/common/blocks/BlockFluxGooItem.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockFluxGooItem.java the item of the goo fluid",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftFlux.java:31 the native goo block"],
        ["1.21.1 has no item metadata and no per-metadata stack limit.",
         "REMAINING GAP: the original's item put the goo into the world as a fluid; the native item places the block with its level state."]),
    "thaumcraft/common/blocks/BlockHole.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockHole.java:53 setResistance(6000000.0f)",
         "oracle thaumcraft/common/blocks/BlockHole.java:55 setLightLevel(0.7f)",
         "native common/src/main/java/thaumcraft/common/blocks/BlockHole.java:27 strength(-1.0F, 3600000.0F)"],
        ["CLOSED in this wave: the blast resistance is the effective value of the original again - the 1.7.10 setter value 6000000 is stored as 18000000 and the vanilla getter divides it by five, so the native registers 3600000 and not 6000000.",
         "The light level is 10, the original setter value 0.7 stores (int)(15*0.7) = 10, the block keeps no collision, no outline and no drops and its tile gives the removed block back."]),
    "thaumcraft/common/blocks/BlockJar.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockJar.java:87-90 the hardness 0.3 and the light level 0.66",
         "oracle thaumcraft/common/blocks/BlockJar.java:159-181 the drops of the fillable jar",
         "oracle thaumcraft/common/blocks/BlockJar.java:184-193 the drop of the node jar carries the aspects and the node attributes",
         "oracle thaumcraft/common/blocks/BlockJar.java:196-199 the brain jar hands a random part of the stored experience back",
         "native common/src/main/java/thaumcraft/common/blocks/BlockJar.java:115 the drop of the fillable jar",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java the brain jar and the node jar"],
        ["The four metadata values became two blocks (jar, jar_void) plus jar_brain and jar_node.",
         "CLOSED in this wave: the node jar drops the node it holds - the aspects, the node type, the node modifier and the identity travel with the dropped item - and every jar of the family emits the light level 9 of the original (the 1.7.10 setter value 0.66 stores (int)(15*0.66) = 9).",
         "CLOSED since the first audit: the brain jar hands a random share of at most sixty four experience back on a right click and drops the stored experience when it is broken.",
         "REMAINING GAP (audio): the original's jar step sound is the custom sound thaumcraft:jar; the native plays the vanilla glass step sound. The original sound itself is in the port and is used for the emptying of the jar."]),
    "thaumcraft/common/blocks/BlockLoot.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockLoot.java:45 setHardness(0.15f)",
         "oracle thaumcraft/common/blocks/BlockLoot.java:46 setResistance(0.0f)",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java:245 the loot urn and the loot crate"],
        ["The two render types became two native blocks.",
         "CLOSED since the first audit: the native loot blocks have their loot tables again, so they are not the missing drop of the first audit.",
         "The hardness 0.15 and the blast resistance 0 are the original numbers."]),
    "thaumcraft/common/blocks/BlockLootItem.java": (
        "parity_verified", "equivalent", None,
        ["oracle thaumcraft/common/blocks/BlockLootItem.java the item of the loot family",
         "native common/src/main/java/thaumcraft/common/blocks/BlockLootItem.java the native item"],
        ["The original's per metadata item became one item per block.",
         "The native item places the block and carries the original's stack limit of one."]),
    "thaumcraft/common/blocks/BlockMagicBox.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockMagicBox.java:64-70 the box drops its whole content",
         "oracle thaumcraft/common/blocks/BlockMagicBox.java:71-85 the right click opens the box interface",
         "oracle thaumcraft/common/blocks/BlockMagicBox.java:87-91 every magic box carries the inventory block entity",
         "native common/src/main/java/thaumcraft/common/blocks/BlockMagicBox.java:41 the ticker of the box",
         "native common/src/main/java/thaumcraft/common/tiles/TileMagicBox.java the linking of the boxes"],
        ["CLOSED since the first audit: the block registers the ticker of the box tile, so the tile ticks, refreshes its links and the box chain links as in the original.",
         "The right click opens the menu with the size of the linked chain and the broken box drops its whole content."]),
    "thaumcraft/common/blocks/BlockMagicalLeaves.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockMagicalLeaves.java:144-200 the random update of the leaves",
         "oracle thaumcraft/common/blocks/BlockMagicalLeaves.java:225-230 the plant that drops when the leaves decay",
         "oracle thaumcraft/common/blocks/BlockMagicalLeavesItem.java the item damage bit four",
         "native common/src/main/java/thaumcraft/common/blocks/BlockMagicalLeaves.java the check_decay state",
         "native common/src/main/resources/data/thaumcraft/loot_table/blocks/greatwood_leaves.json the sapling chance condition"],
        ["The metadata values became separate native blocks; the native port uses the check_decay state instead of the original's metadata bits.",
         "REMAINING GAP: the original's item damage bit four marks a leaf that was placed by a player; such a leaf never decays and never drops the plant. The native port has no item damage, so the marker is the check_decay state of the block and the loot table condition check_decay=true and persistent=false.",
         "The native leaves of the world are generated with check_decay=false and only begin to decay when a neighbour is removed, while the original checked the support in the random tick of every leaf."]),
    "thaumcraft/common/blocks/BlockMagicalLeavesItem.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockMagicalLeavesItem.java:22 public int getItemDamage(int) returns the metadata or four",
         "native common/src/main/java/thaumcraft/common/blocks/BlockMagicalLeaves.java the check_decay state"],
        ["1.21.1 has no item damage value, so the original's bit 4 leaf marker was dropped.",
         "REMAINING GAP: the marker itself has no native counterpart, so a leaf that the player placed cannot be told apart from a generated one by the item; the block state check_decay carries that meaning in the native port."]),
    "thaumcraft/common/blocks/BlockManaPod.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockManaPod.java the pod of the mana beans",
         "native common/src/main/java/thaumcraft/common/blocks/BlockManaPod.java the age state",
         "native common/src/main/resources/assets/thaumcraft/blockstates/mana_pod.json the model of the pod"],
        ["The growth stage became a block state property.",
         "CLOSED since the first audit: the native pod has a model and a loot table again, so it is not the missing model cube of the first audit.",
         "The light level of the original is the age of the pod and the native keeps it as the light level of the state."]),
    "thaumcraft/common/blocks/BlockMetalDevice.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockMetalDevice.java:101 setHardness(3.0f)",
         "oracle thaumcraft/common/blocks/BlockMetalDevice.java:102 setResistance(17.0f)",
         "native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:572 the hardness 3.0",
         "native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:573 the effective blast resistance 10.2"],
        ["The 15 metadata values were split into separate native blocks (crucible, alembic, alchemy furnace, advanced furnace, thaumatorium, nitor, grate, brainbox).",
         "CLOSED since the first audit: the blast resistance is the effective value of the original - the 1.7.10 setter value 17 is stored as 51 and the vanilla getter divides it by five, so the native registers 10.2 for every block of the family."]),
    "thaumcraft/common/blocks/BlockMetalDeviceItem.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockMetalDeviceItem.java the item of the metal device family",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftEquipment.java the items of the metal devices"],
        ["1.21.1 has no item metadata; the fifteen metal device metadata values became separate native blocks and items.",
         "CLOSED since the first audit: the original's placeBlockAt hook that initialises the arcane lamp tiles has a counterpart in every lamp block of the native port, so no behaviour is lost."]),
    "thaumcraft/common/blocks/BlockMirror.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockMirror.java the mirror of the original",
         "native common/src/main/java/thaumcraft/common/blocks/BlockMirror.java:107 the ticker of the mirror",
         "native common/src/main/java/thaumcraft/common/tiles/TileMirror.java the eject, the instability and the repair of the link"],
        ["The metadata facing became a block state property, as documented in the class comment.",
         "CLOSED since the first audit: the block registers the ticker of the mirror tile, so the tile ejects, decays its instability and repairs its link exactly as in the original."]),
    "thaumcraft/common/blocks/BlockMirrorItem.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockMirrorItem.java the item of the mirror",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftLegacyItems.java the mirror link component"],
        ["The 1.7 item class became the item plus a data component; the original NBT tags linkX/linkY/linkZ/linkDim became the MirrorLink component.",
         "The component keeps the position and the dimension of the linked mirror, so the item answers the original's queries."]),
    "thaumcraft/common/blocks/BlockStoneDevice.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockStoneDevice.java:93 setHardness(3.0f)",
         "oracle thaumcraft/common/blocks/BlockStoneDevice.java:94 setResistance(25.0f)",
         "native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:592 the hardness 3.0",
         "native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:593 the effective blast resistance 15.0"],
        ["The metadata values were split into separate native blocks.",
         "CLOSED since the first audit: the blast resistance is the effective value of the original - the setter value 25 is stored as 75 and the vanilla getter divides it by five, so the native registers 15.0."]),
    "thaumcraft/common/blocks/BlockStoneDeviceItem.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockStoneDeviceItem.java the item of the stone device family",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftEquipment.java the items of the stone devices"],
        ["1.21.1 has no item metadata; each stone device metadata value became its own block and item id.",
         "Every metadata value of the original has its own native item, so no behaviour is lost."]),
    "thaumcraft/common/blocks/BlockTable.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockTable.java:63 setHardness(2.5f)",
         "native common/src/main/java/thaumcraft/common/blocks/BlockTable.java the table block",
         "native common/src/main/java/thaumcraft/common/lib/research/TableTopology.java the topology of the table"],
        ["The table and the research table became two native blocks that share one block entity type.",
         "CLOSED since the first audit: the native table has a loot table again, so it is not the missing drop of the first audit.",
         "The hardness 2.5 is the original number."]),
    "thaumcraft/common/blocks/BlockTableItem.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockTableItem.java the item of the table family",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftContent.java:103 the research table item"],
        ["1.21.1 has no item metadata; the eight research table metadata values became one research_table item.",
         "The native item places the research table block and keeps the name of the original."]),
    "thaumcraft/common/blocks/BlockTaint.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockTaint.java:87 setHardness(2.0f)",
         "oracle thaumcraft/common/blocks/BlockTaint.java:88 setResistance(10.0f)",
         "oracle thaumcraft/common/blocks/BlockTaint.java:248-259 getBlockHardness returns 1.75, 1.5 and 0.2 per metadata",
         "native common/src/main/java/thaumcraft/common/lib/world/TaintBlockRules.java:30 taintHardness(int)",
         "native common/src/main/java/thaumcraft/common/lib/world/TaintBlockRules.java:27 the effective blast resistance 6.0"],
        ["CORRECTED: the first audit reported a defect that the oracle does not support. The 1.75 and the 1.5 of the original are the HARDNESS of the crust and of the soil (the original overrides getBlockHardness per metadata), not a light level; the original taint block emits no light at all.",
         "CLOSED since the first audit: the native numbers are the original ones - the family hardness 2.0, the hardness 1.75 of the crust, 1.5 of the soil and 0.2 of the flesh and the effective blast resistance 10*3/5 = 6.0.",
         "Verified equal: the 1/200 taint spore swarmer spawn, the flux goo replacement, the soil to dirt change, canFallBelow, the falling taint start and the step-on poison.",
         "The original only ran its update in the random tick; the native also hooks Block.tick, which no taint block ever schedules, so the rate is unchanged."]),
    "thaumcraft/common/blocks/BlockTaintFibres.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockTaintFibres.java:173 the crust of a wood log, of Material.field_151572_C and of Material.field_151570_A",
         "oracle thaumcraft/common/blocks/BlockTaintFibres.java:177 the soil of Material.field_151595_p, field_151578_c, field_151577_b and field_151571_B",
         "resolved field_151572_C = gourd, field_151570_A = cactus, field_151595_p = sand, field_151578_c = ground, field_151577_b = grass, field_151571_B = clay",
         "native common/src/main/java/thaumcraft/common/lib/world/TaintMaterialRules.java:16 crust and :19 soil"],
        ["CLOSED in this wave: the 1.7.10 materials of the two conversion expressions are resolved with scripts/porting/resolve_srg.py, so the exact block sets of the original are known and the native port reproduces them.",
         "The crust set is a wood log, a gourd (pumpkin, carved pumpkin, jack o'lantern, melon) and a cactus; the soil set is sand, gravel, dirt, farmland, the grass block and clay.",
         "The native port had mycelium, soul sand, coarse dirt, rooted dirt and red sand in the soil set as well; those blocks have no 1.7.10 counterpart and the original never converted them, so they are not converted any more.",
         "Verified equal: the fibre lottery, the biome spread chance, getAdjacentTaint, the 1/40 poison bound and the 80/160 tick poison, the stalk and spore lifecycle and the block shape."]),
    "thaumcraft/common/blocks/BlockTaintFibresItem.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockTaintFibresItem.java the item of the fibres",
         "native common/src/main/java/thaumcraft/common/blocks/BlockTaintFibresItem.java the native item"],
        ["Forced 1.21.1 change: the item became a BlockItem with an explicit type property; the biome gate of the original is preserved.",
         "The 1.12 reference additionally required solid support for the non-zero fibre types; that rule is not in the oracle and is not in the port, so the port follows the oracle."]),
    "thaumcraft/common/blocks/BlockTube.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockTube.java:283-295 the comparator value of the buffer",
         "native common/src/main/java/thaumcraft/common/blocks/BlockTubeBuffer.java:54 the comparator value of the buffer",
         "native common/src/main/java/thaumcraft/common/blocks/BlockTube.java:84 the tube block answers zero"],
        ["The metadata dispatch is replaced by a Kind enum and the centrifuge and the crystallizer became separate native blocks, so the tube block only owns the five tube kinds.",
         "CLOSED since the first audit: the buffer comparator value of the original is in the native buffer block - the amount times 14 divided by 8 plus one when the buffer holds essence, which is the original formula floor(visSize/8*14) plus one.",
         "The valve hand toggle, the filter label attach and detach and the filter label drop on removal are ported and keep the original guards."]),
    "thaumcraft/common/blocks/BlockWoodenDevice.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockWoodenDevice.java:82 setHardness(2.5f)",
         "oracle thaumcraft/common/blocks/BlockWoodenDevice.java:83 setResistance(10.0f)",
         "native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:575 the hardness 2.5",
         "native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:577 the effective blast resistance 6.0"],
        ["The metadata values were split into separate native blocks (arcane door, magic box, bellows, banner, arcane bore).",
         "CLOSED since the first audit: the blast resistance is the effective value of the original - the setter value 10 is stored as 30 and the vanilla getter divides it by five, so the native registers 6.0."]),
    "thaumcraft/common/blocks/BlockWoodenDeviceItem.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/BlockWoodenDeviceItem.java the item of the wooden device family",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftEquipment.java the items of the wooden devices"],
        ["1.21.1 has no item metadata; the wooden device metadata values became separate native blocks and items.",
         "Every metadata value of the original has its own native item, so no behaviour is lost."]),
    "thaumcraft/common/blocks/CustomStepSound.java": (
        "partial", "not_applicable", None,
        ["oracle thaumcraft/common/blocks/CustomStepSound.java the sound type that names thaumcraft:<sound>",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftResources.java:38 a custom SoundType of the native port"],
        ["The per block custom sound class was dropped in favour of the vanilla SoundType enum.",
         "REMAINING GAP (audio): the original's per block step and break sound names are therefore replaced by vanilla equivalents in the affected blocks; the sound events themselves are in the port (assets/thaumcraft/sounds.json)."]),
    "thaumcraft/common/blocks/ItemArcaneDoor.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/blocks/ItemArcaneDoor.java the item of the arcane door",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java the arcane door items"],
        ["1.21.1 has no item damage value; the original's metadata naming became one item per warded door block.",
         "Every metadata value of the original has its own native item, so the names and the stack limits of the original are preserved."]),
    "thaumcraft/common/blocks/ItemJarNode.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/blocks/ItemJarNode.java the item that carries the aspects, the node type, the node modifier and the identity",
         "oracle thaumcraft/common/blocks/BlockJar.java:184-193 the drop of the node jar carries them",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java the drop of the node jar",
         "native common/src/main/java/thaumcraft/common/tiles/TileJarNode.java the stored aspects, the type, the modifier and the identity"],
        ["The itemJarNode item was replaced by a placeable jar_node block.",
         "CLOSED in this wave: breaking the node jar no longer loses the stored node - the drop carries the block entity data of the tile, so the aspects, the node type, the node modifier and the identity survive the break and the placement.",
         "REMAINING GAP: the original published an item with the NBT API of the node (setAspects, setNodeAttributes, getNodeType, getNodeModifier, getNodeId) and the original research NODEJAR handed that item to the player; the native jar_node is a block and the node jar is not created by a research yet."]),
    "thaumcraft/common/blocks/JarStepSound.java": (
        "partial", "not_applicable", None,
        ["oracle thaumcraft/common/blocks/JarStepSound.java the sound type of the jar",
         "oracle thaumcraft/common/blocks/BlockJar.java:88 the jar uses that sound type",
         "native common/src/main/resources/assets/thaumcraft/sounds.json the jar sound event",
         "native common/src/main/java/thaumcraft/common/blocks/BlockJar.java the jar uses the vanilla glass step sound"],
        ["1.21.1 block step sounds come from the block SoundType, so the per-block sound class was dropped.",
         "REMAINING GAP (audio): the original jar step sound (thaumcraft:jar) is therefore replaced by the vanilla glass step sound. The original sound event is in the port and is played when the jar is emptied."]),
    "thaumcraft/common/blocks/world/taint/TaintHelper.java": (
        "missing", "not_applicable", "exclude",
        ["legacy src/main/java/thaumcraft/common/blocks/world/taint/TaintHelper.java the TC6 facade",
         "native common/src/main/java/thaumcraft/common/blocks/BlockTaintFibres.java spreadFibres"],
        ["Legacy-only TC6 facade: the native port has no TaintHelper class; its two functions are the native BlockTaintFibres.spreadFibres.",
         "The legacy facade was a thin wrapper over the same 1.12 taint fibre logic, so no behaviour was lost and the file itself is deliberately not carried over."]),

    # ------------------------------------------------------------------ configuration
    "thaumcraft/common/config/Config.java": (
        "partial", "different", None,
        ["oracle thaumcraft/common/config/Config.java the ~70 configuration options of the original",
         "native common/src/main/java/thaumcraft/common/config/ResearchSettings.java the native settings of the research",
         "native common/src/main/java/thaumcraft/common/config/BiomeSettings.java the native biome settings",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftTaintSettings.java the native taint settings"],
        ["The native configuration keeps the research, the taint and the biome settings; the other ~70 original options (the golem delay and link quality, the champion mobs, the runic shield values, the node rarity, the notification limits, the potion ids, the dimension id, the world regeneration switches) have no native counterpart and are hard-coded or absent.",
         "The original golem_delay (5) and golem_ignore_delay (10000) values ARE reproduced as constants in the native golem code, so the default behaviour matches even though the option itself is gone.",
         "REMAINING GAP: the native file format is a properties file and not the original Forge configuration, and the option set of the original is not reproduced, so an add-on that reads or writes Config cannot."]),
    "thaumcraft/common/config/ConfigAspects.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/config/ConfigAspects.java the registration of the aspects",
         "native common/src/main/java/thaumcraft/api/aspects/Aspect.java the 48 aspects",
         "native common/src/main/resources/data/thaumcraft/tc4/aspects.json the machine checkable corpus"],
        ["The original registration class became three native classes; the aspect values are reproduced there.",
         "Forced 1.21.1 change: the original had no equivalent of the native data components, so the item comparison had to be rebuilt (see GolemItemMatch)."]),
    "thaumcraft/common/config/ConfigBlocks.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/config/ConfigBlocks.java the static block fields",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftBlocks.java and the other per-domain registries"],
        ["The single ConfigBlocks holder with the static block fields became one registry class per content area (ThaumcraftBlocks, ThaumcraftEldritchBlocks, ThaumcraftMasonry, ...), because 1.21.1 has no static field registry.",
         "The block set itself is not part of this domain; every block of the original has its own native registration."]),
    "thaumcraft/common/config/ConfigEntities.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/config/ConfigEntities.java the static entity fields",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftMonsters.java and the other per-domain registries",
         "native common/src/main/java/thaumcraft/common/lib/world/EldritchMobRules.java the champion whitelist"],
        ["The static entity fields became per-domain registries; the champion whitelist moved into ChampionRules.",
         "Every entity of the original has its own native registration."]),
    "thaumcraft/common/config/ConfigItems.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/config/ConfigItems.java the static item fields",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftLegacyItems.java and the other per-domain registries"],
        ["The static item fields became per-domain registries (1.21.1 DeferredRegister).",
         "Every item of the original has its own native registration."]),
    "thaumcraft/common/config/MaterialTaint.java": (
        "partial", "not_applicable", None,
        ["oracle thaumcraft/common/config/MaterialTaint.java the taint material",
         "native common/src/main/java/thaumcraft/common/blocks/BlockTaint.java the taint block family"],
        ["1.21.1 has no net.minecraft.block.material.Material; the observable traits of MaterialTaint (solid, not replaceable, not liquid, grass map colour) are expressed with BlockBehaviour.Properties and the taint block family.",
         "REMAINING GAP: the 'mobility flag' of MaterialTaint (func_76227_m == 1) has no 1.21.1 counterpart and is not reproduced."]),
    "thaumcraft/common/config/recipes/ConfigRecipesInfusionDeviceSlice.java": (
        "missing", "not_applicable", "exclude",
        ["legacy src/main/java/thaumcraft/common/config/recipes/ConfigRecipesInfusionDeviceSlice.java the legacy helper",
         "native common/src/main/resources/data/thaumcraft/recipe the native recipe tree"],
        ["Legacy only helper (no 1.7 oracle counterpart): the native replaced the code registered recipes with data files.",
         "The original NODESTABILIZERADV recipe has no native infusion recipe file; the advanced node stabilizer belongs to the advanced device family (M09).",
         "The original WANDPEDFOC (the wand pedestal with the focus block) has no separate native recipe file; only the plain wand pedestal exists."]),
    "thaumcraft/common/config/recipes/ConfigRecipesInfusionSlice.java": (
        "missing", "not_applicable", "exclude",
        ["legacy src/main/java/thaumcraft/common/config/recipes/ConfigRecipesInfusionSlice.java the legacy helper",
         "native common/src/main/resources/data/thaumcraft/recipe the native recipe tree"],
        ["Legacy only helper (no 1.7 oracle counterpart): the native replaced the code registered recipes with data files.",
         "The original CAP_silver recipe only exists when another mod supplies a silver ingot; the native has no copper or silver cap recipe, so the add-on caps are not ported."]),

    # ------------------------------------------------------------------ containers
    "thaumcraft/common/container/ContainerGhostSlots.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/container/ContainerGhostSlots.java:47-56 the quick move of a ghost slot",
         "oracle thaumcraft/common/container/ContainerGhostSlots.java:93-95 a different item replaces the preview with a copy of the whole held stack",
         "oracle thaumcraft/common/container/ContainerGhostSlots.java:110-117 the creative clone",
         "oracle thaumcraft/common/container/ContainerGhostSlots.java:122-130 the throw",
         "oracle thaumcraft/common/container/ContainerGhostSlots.java:149-160 the hotbar swap clears the ghost slot",
         "native common/src/main/java/thaumcraft/common/container/ContainerGhostSlots.java:56 the ghost click handling"],
        ["CLOSED since the first audit: the defect of the first audit is gone. When the held item differs from the preview the native replaces the preview with a copy of the whole held stack, exactly as the original does.",
         "The quick move, the creative clone and the throw of the original are implemented as well.",
         "REMAINING GAP: the hotbar swap click (mod 2) of the original clears the ghost slot without taking the item; the native refuses that click and keeps the preview."]),
    "thaumcraft/common/container/ContainerHandMirror.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/container/ContainerHandMirror.java:43-52 the player inventory of the mirror",
         "oracle thaumcraft/common/container/ContainerHandMirror.java:55-66 the transport of the item into the linked mirror",
         "oracle thaumcraft/common/container/ContainerHandMirror.java:98-105 the slot content returns to the player",
         "native common/src/main/java/thaumcraft/common/container/ContainerHandMirror.java:88 onInputChanged",
         "native common/src/main/java/thaumcraft/common/items/relics/ItemHandMirror.java the bridge of the transport"],
        ["CLOSED since the first audit: the transport is not missing any more. The hand mirror item implements the bridge of the container and carries the item to the linked mirror in front of it, exactly as the original did.",
         "The slot content returns to the player when the menu closes and a mirror never travels into its own slot."]),
    "thaumcraft/common/container/ContainerMagicBox.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/container/ContainerMagicBox.java the menu of the box",
         "native common/src/main/java/thaumcraft/common/container/ContainerMagicBox.java the menu of the box",
         "native common/src/main/java/thaumcraft/common/blocks/BlockMagicBox.java:41 the ticker of the box tile"],
        ["The 1.7 Container became a 1.21.1 menu; the linked size and the quick move boundary are unchanged.",
         "CLOSED since the first audit: the linked size grows, because the box tile ticks again and links its neighbours."]),
    "thaumcraft/common/container/InventoryFake.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/container/InventoryFake.java the single slot inventory of the crafting",
         "native common/src/main/java/thaumcraft/common/container/InventoryFake.java the single slot inventory"],
        ["The native class exists with the same single slot contract.",
         "Forced 1.21.1 change: the original fired the Forge player crafting event with the fake inventory; 1.21.1 has no such event, so the native calls the mod event hook instead."]),
    "thaumcraft/common/container/InventoryHandMirror.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/container/InventoryHandMirror.java the one slot mirror of the hand mirror",
         "native common/src/main/java/thaumcraft/common/container/ContainerHandMirror.java the MirrorInventory"],
        ["The separate inventory class became the private MirrorInventory inside the native container.",
         "CLOSED since the first audit: the transport runs, because the hand mirror item implements the bridge of the container."]),
    "thaumcraft/common/container/SlotGhostFluid.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/container/SlotGhostFluid.java the slot of the fluid container",
         "native common/src/main/java/thaumcraft/common/container/SlotGhostFluid.java the slot of the fluid container"],
        ["The 1.7 slot became a 1.21.1 slot with the same limit of one.",
         "REMAINING GAP: the original accepts any item registered in the Forge FluidContainerRegistry; the native accepts only a BucketItem, which is the only fluid container vanilla 1.21.1 has. A fluid container of an add-on is therefore refused."]),

    # ------------------------------------------------------------------ library
    "thaumcraft/common/lib/FakeThaumcraftPlayer.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/lib/FakeThaumcraftPlayer.java the fake player",
         "oracle thaumcraft/common/lib/events/EventHandlerEntity.java the pickup event of the fake player",
         "native common/src/main/java/thaumcraft/common/lib/FakeThaumcraftPlayer.java the native fake player"],
        ["The 1.7 EntityPlayer subclass became a native fake player; the silence, the command refusal and the fixed origin are kept.",
         "REMAINING GAP: the original also blocked the item pickup event for every player whose name starts with FakeThaumcraft; the native never adds a fake player to a level, so there is nothing to cancel and the rule is not reproduced."]),
    "thaumcraft/common/lib/InternalMethodHandler.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/lib/InternalMethodHandler.java the real implementation of the plug point",
         "oracle thaumcraft/api/internal/DummyInternalMethodHandler.java the dummy implementation",
         "native common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java the real native implementation",
         "native common/src/main/java/thaumcraft/api/internal/IInternalMethodHandler.java the interface of the plug point"],
        ["CLOSED since the first audit: the native bridge is not a dummy any more. The real implementation answers the research state, the discovered aspects, the object tags, the workbench slots, the wand vis costs and the warp of the original.",
         "The dummy of the original is ported as well, so an add-on may still replace the plug point."]),
    "thaumcraft/common/lib/RefGui.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/lib/RefGui.java the numeric gui ids of the original",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftDeviceMenus.java the native menu types"],
        ["The 1.7 GUI id constants are gone: 1.21.1 menus are identified by their own MenuType, so the numeric ids have no counterpart.",
         "REMAINING GAP: every menu the original listed has a native menu class, but the id numbering itself is not reproduced, so an add-on that opened a menu by its number has to use the native MenuType."]),
    "thaumcraft/common/lib/WarpEvents.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/lib/WarpEvents.java:72-80 the warp roll and the devil mask",
         "oracle thaumcraft/common/lib/WarpEvents.java:327-336 getWarpFromGear sums the gear, the four armor pieces and the four bauble slots",
         "native common/src/main/java/thaumcraft/common/lib/warp/WarpRules.java the arithmetic of the warp",
         "native common/src/main/java/thaumcraft/common/lib/warp/WarpGear.java fromGear sums the hand and the four armor pieces"],
        ["Forced 1.21.1 change: the 1.7 static helper class became a dispatcher plus a pure WarpRules class that owns the arithmetic (the decision, the event ladder, the research unlocks and the death gaze cone).",
         "Verified equal: the roll order, the effective warp formula, the counter decay, the severity roll, the devil mask, every effect duration and amplifier, the empty branches, the CLARITY branch and the research unlocks.",
         "REMAINING GAP: the original getWarpFromGear also summed the four bauble slots; the native WarpGear.fromGear only sums the hand and the four armor pieces, because the port has no accessory slots.",
         "REMAINING GAP: the research unlock path adds the research warp (perm and sticky split) in the native, which the research layer does in the original; the exact split was not compared with ResearchManager.completeResearch."]),
    "thaumcraft/common/lib/events/EventHandlerEntity.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/lib/events/EventHandlerEntity.java:348-356 the owner protection of a golem",
         "oracle thaumcraft/common/lib/events/EventHandlerEntity.java:358-379 the ender pearl that a warded object destroys",
         "native common/src/main/java/thaumcraft/common/items/golems/GolemOwnership.java the owner rule",
         "native common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java:2297 the refused interaction of a golem",
         "native common/src/main/java/thaumcraft/common/lib/ward/WardedPearl.java the warded pearl rule"],
        ["The 1.7 Forge event handler became a mixin and platform-event architecture; most of its behaviours are present (the research save and load, the repair enchantment, the warp and death gaze checks, the champion spawn roll and drops, the zombie brain, the villager resource, the crystal essence on dissolve, the taint conversion on death, the aspect orbs, the primal arrow, the unnatural hunger, the bath salts, the special mining drops, the pure bucket, the note block and the travel gear).",
         "CORRECTED: the first audit reported the golem owner protection and the warded ender pearl as missing. Both are implemented: the owner rule refuses the interaction of a non owner with the original message (GolemOwnership) and the warded pearl dies next to an owned block with the original message (WardedPearl).",
         "ADAPTED: the original wrote the thrower name into the dropped item NBT (ItemTossEvent) and TileCrucible.attemptSmelt read it for the crucible owner rule; the native resolves the thrower from the vanilla ItemEntity owner, so the rule survives with a different mechanism.",
         "NOT NEEDED: the original cancelled the item pickup of every fake player whose name starts with FakeThaumcraft; the native never adds a fake player to a level, so there is nothing to cancel.",
         "REMAINING GAP: the original world load and save of the research file has no counterpart, because a fresh 1.21 world is the only supported input."]),
    "thaumcraft/common/lib/events/EventHandlerWorld.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/lib/events/EventHandlerWorld.java:139-146 getBurnTime of the alumentum and the magical log",
         "oracle thaumcraft/common/lib/events/EventHandlerWorld.java:227-244 the block placement near an active eldritch boss",
         "oracle thaumcraft/common/lib/events/EventHandlerWorld.java the chunk regeneration queue",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftResources.java:31 the burn time 400 of the magical log",
         "native common/src/main/java/thaumcraft/common/config/ThaumcraftNaturals.java:102 the fuel registration of the magical log"],
        ["The world handler was split into mixins and platform events; the special mining drops, the note block events, the pure and death bucket fills, the vis effect and the sticky warp on crafting are all present.",
         "CORRECTED: the first audit reported the magical log fuel rule as missing. The burn time 400 of the magical log is registered (ThaumcraftNaturals) next to the burn time 6400 of the alumentum.",
         "REMAINING GAP: the chunk regeneration queue (the original re-generated the chunks whose Thaumcraft chunk tag was absent) has no counterpart, because 1.21.1 has no equivalent chunk data event.",
         "REMAINING GAP: the block placement is still not cancelled near an active eldritch boss in the Outer Lands.",
         "REMAINING GAP: the original maze load and save on the world load and save events has no native counterpart (the Outer Lands maze is a different subsystem)."]),
    "thaumcraft/common/lib/events/KeyHandler.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/lib/events/KeyHandler.java the key bindings of the original",
         "native fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java the key binding registration"],
        ["The 1.7 key input handler became the Architectury key binding registration.",
         "REMAINING GAP: the bindings themselves are not part of this audit, and the original's own key handling (the wand toggle and the node view) lives in the client lane."]),
    "thaumcraft/common/lib/events/ServerTickEventsFML.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/lib/events/ServerTickEventsFML.java the server tick handler and the chunk regeneration queue",
         "native common/src/main/java/thaumcraft/common/lib/events/ the native tick events"],
        ["The 1.7 tick handler became the Architectury tick events plus the native excavation code.",
         "The excavation block swap and the note block clearing are implemented in the native excavation and arcane ear code.",
         "REMAINING GAP: the chunk regeneration queue is not reproduced, because 1.21.1 has no equivalent chunk data event (see the EventHandlerWorld row)."]),
    "thaumcraft/common/lib/research/PlayerKnowledge.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/lib/research/PlayerKnowledge.java:24-34 wipePlayerKnowledge keeps the warp counter",
         "oracle thaumcraft/common/lib/research/PlayerKnowledge.java the process wide player maps",
         "native common/src/main/java/thaumcraft/common/lib/research/PlayerKnowledge.java:116 reset()",
         "native common/src/main/java/thaumcraft/common/mixin/PlayerKnowledgeMixin.java the per player state of 1.21.1"],
        ["Forced change: the process-wide player-name maps became a per-player object attached by a mixin; the values, the aspect-pool arithmetic and the overdraw quirk are preserved.",
         "CLOSED in this wave: the wipe keeps the warp counter of the player, exactly as the original wipePlayerKnowledge does.",
         "REMAINING GAP: the short cast of getAspectPoolFor is dropped from the API (the value no longer overflows) and is only applied where the original's signed-short query is observable (the research table).",
         "REMAINING GAP: the native persists the AutoUnlock research entries, the original skipped them in saveResearchNBT (oracle ResearchManager:793); the native re-grants them on join anyway.",
         "REMAINING GAP: the .thaum file of the original is replaced by the per-player state of 1.21.1, because a fresh world is the only supported input."]),
    "thaumcraft/common/lib/research/ResearchManager.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/lib/research/ResearchManager.java the manager of the original",
         "oracle thaumcraft/common/lib/research/ResearchManager.java:126-127 the paper is checked and never taken",
         "oracle thaumcraft/common/lib/research/ResearchManager.java:793 the auto unlock research is not saved",
         "native common/src/main/java/thaumcraft/common/lib/research/ResearchNoteIssuance.java:80 the note issuance",
         "native common/src/main/java/thaumcraft/common/lib/research/ResearchPurchase.java the admission rules of a purchase"],
        ["The 863-line manager was split into focused classes; every live original method has a counterpart (createClue -> ResearchClues, findHiddenResearch -> ResearchHiddenSelection, createNote -> ResearchNoteGenerator, checkResearchCompletion -> ResearchNotePuzzle, completeResearch -> ResearchCompletion, the command behaviour -> ResearchAdministration).",
         "CORRECTED: the first audit reported that the native consumes one paper per issued note. The native keeps the original: the paper is checked and never taken (ResearchNoteIssuance).",
         "REMAINING GAP: the process-wide player maps and the .thaum file format of the original were replaced by the per-player capability state.",
         "REMAINING GAP: findMatchingResearch is not ported; it has no caller in the original, so it was dropped with the dead code.",
         "DOCUMENTED DEVIATION: the original's PacketPlayerCompleteToServer did not re-check the aspect cost; the native re-validates every admission rule (an authority fix, documented in ResearchPurchase)."]),
    "thaumcraft/common/lib/research/ScanManager.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/common/lib/research/ScanManager.java the scan manager of the original",
         "native common/src/main/java/thaumcraft/common/lib/research/ScanManager.java the native scan manager",
         "native common/src/main/java/thaumcraft/common/lib/research/ObjectAspectCatalog.java the object aspect bindings"],
        ["The scan reward arithmetic, the aspect discovery rule, the parent-aspect admission and the node aspect scaling are the original numbers.",
         "Forced change: the numeric item and entity hashes of the original cannot exist in a fresh 1.21.1 world; the native uses readable string signatures and a separate object and entity aspect binding catalogue.",
         "REMAINING GAP: the original's hash functions are therefore not reproduced verbatim - only their observable purpose (one scan per witness, one reward per scan)."]),
    "thaumcraft/common/lib/utils/BlockUtils.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/lib/utils/BlockUtils.java the helpers of the original",
         "native common/src/main/java/thaumcraft/common/lib/utils/BlockUtils.java the native helpers"],
        ["The helper set is reproduced (every original method name is still present) and the new helpers (canHarvest, drops, markForRenderUpdate, matches) are native additions.",
         "Forced 1.21.1 change: the 1.7 IBlockAccess and World calls became the 1.21.1 Level and BlockState calls."]),
    "thaumcraft/common/lib/utils/CropUtils.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/lib/utils/CropUtils.java the crop helpers of the original",
         "native common/src/main/java/thaumcraft/common/lib/utils/CropUtils.java the native helpers"],
        ["Every original method name is present in the native class; the 1.7 Block crop check became the 1.21.1 BlockState check."]),
    "thaumcraft/common/lib/utils/EntityUtils.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/lib/utils/EntityUtils.java the entity helpers of the original",
         "native common/src/main/java/thaumcraft/common/lib/utils/EntityUtils.java the native helpers"],
        ["Every original method name is present; the champion attribute of the original became the native champion data (championId, modifier, modifierAttribute, modifierUuid)."]),
    "thaumcraft/common/lib/utils/InventoryUtils.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/lib/utils/InventoryUtils.java the inventory helpers of the original",
         "native common/src/main/java/thaumcraft/common/lib/utils/InventoryUtils.java the native helpers"],
        ["Every original method name is present in the native class and the native adds the ore tag helpers (matchesOre, oresOf, registerOreTag) that replace the 1.7 ore dictionary.",
         "Forced 1.21.1 change: the 1.7 ore dictionary had no 1.21.1 equivalent, so the comparison had to be rebuilt (see GolemItemMatch)."]),
    "thaumcraft/common/lib/utils/Utils.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/common/lib/utils/Utils.java the helpers of the original",
         "native common/src/main/java/thaumcraft/common/lib/utils/Utils.java the native helpers"],
        ["The native class keeps every original helper except the three that were moved into their own classes, and it says so in its own header (SpecialMining, LootTables, BiomeColumns).",
         "The world-gen helpers of the original (genGear, getGearItemForSlot, generateLoot) live in the native LootTables and travel gear code."]),
}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="report the difference instead of writing it")
    args = parser.parse_args()

    data = json.loads(LEDGER.read_text(encoding="utf-8"))
    index = {unit["canonical_path"]: unit for unit in data["units"]}
    changed = 0
    unknown = []
    for path, (coverage, semantics, disposition, evidence, notes) in VERDICTS.items():
        unit = index.get(path)
        if unit is None:
            unknown.append(path)
            continue
        review = unit["review"]
        review["native_coverage"] = coverage
        if semantics is not None:
            review["source_semantics"] = semantics
        if disposition is not None:
            review["disposition"] = disposition
        review["evidence"] = list(evidence)
        review["notes"] = list(notes)
        review["owner"] = OWNER
        changed += 1
    if unknown:
        print(json.dumps({"unknown": unknown}, ensure_ascii=False))
        return 2

    # The disposition counter of the header is derived from the rows, exactly like
    # scripts/porting/update_scope_ledger.py computes it.
    dispositions = Counter(unit["review"]["disposition"] for unit in data["units"])
    data["counts"]["dispositions"] = dict(sorted(dispositions.items()))

    text = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    if args.check:
        current = LEDGER.read_text(encoding="utf-8")
        print(json.dumps({"changed": changed, "same": current == text}, ensure_ascii=False))
        return 0
    LEDGER.write_text(text, encoding="utf-8", newline="\n")
    print(json.dumps({"applied": changed}, ensure_ascii=False))
    return 0

if __name__ == "__main__":
    sys.exit(main())
