package thaumcraft.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumcraft.common.testing.*;

public final class ModernAdaptationsGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE, batch = "modern_entity_catalog")
    public void modernMobAspectsRespectPublicApi(GameTestHelper test) { ModernEntityScanRuntimeChecks.modernEntitiesKeepApiOverrides(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "modern_entity_scan", timeoutTicks = 400)
    public void modernMobScanUsesRealHeldSession(GameTestHelper test) { ModernEntityScanRuntimeChecks.modernEntityThaumometerUsesRealSession(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "modern_woodwork")
    public void modernWoodworkPlacementAndSignData(GameTestHelper test) { WoodworkRuntimeChecks.placementAndSignPersistence(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "modern_woodwork_recipes")
    public void modernWoodworkRecipesPayMaterials(GameTestHelper test) { WoodworkRuntimeChecks.recipesUseRealMaterials(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "modern_armor_trim")
    public void modernArmorSmithingPreservesEquipment(GameTestHelper test) { ArmorTrimRuntimeChecks.smithingKeepsEquipmentData(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "modern_loot")
    public void modernStructuresYieldNativeLoot(GameTestHelper test) { ModernExplorationLootRuntimeChecks.loadedStructureAndArchaeologyLoot(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "modern_enchantment_catalog")
    public void modernEnchantmentCompatibilityAndLimits(GameTestHelper test) { ModernEnchantmentInfusionRuntimeChecks.nativeRecipesAndCompatibility(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "modern_enchantment_payments", timeoutTicks = 2400)
    public void modernEnchantmentInfusionPaysAllInputs(GameTestHelper test) { ModernEnchantmentInfusionRuntimeChecks.actualMaceAndTreasureInfusion(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "modern_stonecutter")
    public void modernStonecutterPaysForBuildingBlocks(GameTestHelper test) { ModernUtilityRuntimeChecks.stonecutterUsesPaidNativeMenu(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "modern_research")
    public void modernResearchHasActualRecipePages(GameTestHelper test) { ModernUtilityRuntimeChecks.modernResearchKeepsOriginalCorpus(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "modern_golem_crops", timeoutTicks = 3800)
    public void modernGolemsHarvestAndReplantCrops(GameTestHelper test) { ModernGolemCropsRuntimeChecks.run(test, GolemPlacementProbe::open); }
}
