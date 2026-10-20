package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The shape of the published API surface.
 *
 * <p>The original TC4 jar published these classes; every one of them must exist here with the original
 * name, so an add-on that was compiled against TC4 still links.</p>
 */
class ApiSurfaceTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final List<String> ORIGINAL_API_CLASSES = List.of(
            "thaumcraft.api.ThaumcraftApi",
            "thaumcraft.api.ThaumcraftApiHelper",
            "thaumcraft.api.ItemApi",
            "thaumcraft.api.ItemRunic",
            "thaumcraft.api.TileThaumcraft",
            "thaumcraft.api.ThaumcraftMaterials",
            "thaumcraft.api.aspects.Aspect",
            "thaumcraft.api.aspects.AspectList",
            "thaumcraft.api.aspects.AspectHelper",
            "thaumcraft.api.aspects.AspectEventProxy",
            "thaumcraft.api.aspects.AspectRegistryEvent",
            "thaumcraft.api.aspects.AspectSourceHelper",
            "thaumcraft.api.aspects.IAspectContainer",
            "thaumcraft.api.aspects.IAspectSource",
            "thaumcraft.api.aspects.IEssentiaContainerItem",
            "thaumcraft.api.aspects.IEssentiaTransport",
            "thaumcraft.api.aura.AuraHelper",
            "thaumcraft.api.baubles.IBauble",
            "thaumcraft.api.baubles.BaubleType",
            "thaumcraft.api.blocks.BlocksTC",
            "thaumcraft.api.capabilities.IPlayerKnowledge",
            "thaumcraft.api.crafting.CrucibleRecipe",
            "thaumcraft.api.crafting.IArcaneRecipe",
            "thaumcraft.api.crafting.IInfusionStabiliser",
            "thaumcraft.api.crafting.InfusionEnchantmentRecipe",
            "thaumcraft.api.crafting.InfusionRecipe",
            "thaumcraft.api.crafting.ShapedArcaneRecipe",
            "thaumcraft.api.crafting.ShapelessArcaneRecipe",
            "thaumcraft.api.damagesource.DamageSourceThaumcraft",
            "thaumcraft.api.damagesource.DamageSourceIndirectThaumcraftEntity",
            "thaumcraft.api.entities.IEldritchMob",
            "thaumcraft.api.entities.ITaintedMob",
            "thaumcraft.api.IArchitect",
            "thaumcraft.api.IGoggles",
            "thaumcraft.api.internal.DummyInternalMethodHandler",
            "thaumcraft.api.internal.IInternalMethodHandler",
            "thaumcraft.api.internal.WeightedRandomLoot",
            "thaumcraft.api.IRepairable",
            "thaumcraft.api.IRepairableExtended",
            "thaumcraft.api.IRunicArmor",
            "thaumcraft.api.IScribeTools",
            "thaumcraft.api.items.ItemsTC",
            "thaumcraft.api.IVisDiscountGear",
            "thaumcraft.api.IWarpingGear",
            "thaumcraft.api.nodes.INode",
            "thaumcraft.api.nodes.IRevealer",
            "thaumcraft.api.nodes.NodeModifier",
            "thaumcraft.api.nodes.NodeType",
            "thaumcraft.api.potions.PotionFluxTaint",
            "thaumcraft.api.potions.PotionVisExhaust",
            "thaumcraft.api.research.IScanEventHandler",
            "thaumcraft.api.research.IScanThing",
            "thaumcraft.api.research.ResearchAddendum",
            "thaumcraft.api.research.ResearchCategories",
            "thaumcraft.api.research.ResearchCategoriesCompat",
            "thaumcraft.api.research.ResearchCategory",
            "thaumcraft.api.research.ResearchCategoryList",
            "thaumcraft.api.research.ResearchEntry",
            "thaumcraft.api.research.ResearchItem",
            "thaumcraft.api.research.ResearchPage",
            "thaumcraft.api.research.ResearchStage",
            "thaumcraft.api.research.ScanBlock",
            "thaumcraft.api.research.ScanningManager",
            "thaumcraft.api.research.ScanResult",
            "thaumcraft.api.research.theorycraft.ITheorycraftAid",
            "thaumcraft.api.research.theorycraft.ResearchTableData",
            "thaumcraft.api.research.theorycraft.TheorycraftCard",
            "thaumcraft.api.research.theorycraft.TheorycraftManager",
            "thaumcraft.api.visnet.TileVisNode",
            "thaumcraft.api.visnet.VisNetHandler",
            "thaumcraft.api.wands.FocusUpgradeType",
            "thaumcraft.api.wands.ItemFocusBasic",
            "thaumcraft.api.wands.IWandable",
            "thaumcraft.api.wands.IWandFocus",
            "thaumcraft.api.wands.IWandRodOnUpdate",
            "thaumcraft.api.wands.IWandTriggerManager",
            "thaumcraft.api.wands.StaffRod",
            "thaumcraft.api.wands.WandCap",
            "thaumcraft.api.wands.WandRod",
            "thaumcraft.api.wands.WandTriggerRegistry");

    @Test void everyOriginalApiClassExists() throws ClassNotFoundException {
        for (String name : ORIGINAL_API_CLASSES) {
            Class<?> type = Class.forName(name);
            assertTrue(Modifier.isPublic(type.getModifiers()), name + " must stay public");
        }
    }

    @Test void thePublicApiUsesTheNativeTypes() throws Exception {
        assertEquals(thaumcraft.api.aspects.AspectList.class,
                Class.forName("thaumcraft.api.ThaumcraftApiHelper").getMethod("getObjectAspects", net.minecraft.world.item.ItemStack.class)
                        .getReturnType());
        assertEquals(net.minecraft.world.level.block.entity.BlockEntity.class,
                Class.forName("thaumcraft.api.ThaumcraftApiHelper").getMethod("getConnectableTile", net.minecraft.world.level.Level.class,
                        int.class, int.class, int.class, net.minecraft.core.Direction.class).getReturnType());
        assertNotNull(Class.forName("thaumcraft.api.crafting.IArcaneRecipe").getMethod("matches",
                thaumcraft.common.crafting.ArcaneRecipeInput.class, net.minecraft.world.level.Level.class,
                net.minecraft.world.entity.player.Player.class));
    }

    @Test void theInternalHandlerIsBoundToTheNativeImplementation() {
        assertTrue(thaumcraft.api.internal.IInternalMethodHandler.class.isInstance(ThaumcraftApi.internalMethods));
        assertNotNull(ThaumcraftApi.internalMethods);
    }
}
