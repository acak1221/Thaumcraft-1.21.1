package thaumcraft.infusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionEnchantmentRecipe;
import thaumcraft.api.crafting.NativeRecipes;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.ProgrammaticRecipes;

/** Native item/registry and frozen-plan checks; no matrix or server admission is simulated. */
class InfusionEnchantmentXpPointsTest {
    private static HolderLookup.Provider registries;
    private static Holder<Enchantment> target;

    @BeforeAll static void bootstrapNativeItemsAndEnchantments() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = VanillaRegistries.createLookup();
        target = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
    }

    @Test void nativeAndApiViewPricesUseTheRecipesStoredMinimumCost() {
        var nativeRecipe = new thaumcraft.common.crafting.InfusionEnchantmentRecipe("", Enchantments.UNBREAKING.location(),
                List.of(Ingredient.of(Items.FLINT)), Map.of("aer", 1), 1, 60);
        ItemStack central = new ItemStack(Items.IRON_PICKAXE);
        var previousManager = NativeRecipes.manager();
        var previousRegistries = NativeRecipes.registries();
        try {
            NativeRecipes.install(previousManager, registries);
            InfusionEnchantmentRecipe view = InfusionEnchantmentRecipe.of(nativeRecipe);
            assertNotNull(view);
            assertEquals(20, nativeRecipe.xpLevels(central, registries));
            assertEquals(20, view.calcXP(central));
            assertEquals(550, nativeRecipe.xpPoints(central, registries));
            assertEquals(550, view.calcXPPoints(central));
            central.enchant(target, 1);
            assertEquals(40, view.calcXP(central));
            assertEquals(2_920, nativeRecipe.xpPoints(central, registries));
            assertEquals(2_920, view.calcXPPoints(central));
        } finally {
            NativeRecipes.install(previousManager, previousRegistries);
        }
    }

    @Test void existingVirtualNominalCostOverridesBecomePointPricesInPreparedPlans() {
        InfusionEnchantmentRecipe addon = new PlannedRecipe() {
            @Override public int calcXP(ItemStack central) { return 12; }
        };
        addon.recipeXP = 1; // The virtual answer, rather than this field or its native view, owns payment.
        InfusionRecipeInput offered = offered();
        assertEquals(12, addon.calcXP(offered.central()));
        assertEquals(216, addon.calcXPPoints(offered.central()));
        var prepared = ProgrammaticRecipes.prepareInfusion(addon, offered, null, null);
        assertNotNull(prepared);
        assertEquals(216, prepared.xp());
        assertEquals(Enchantments.UNBREAKING.location(), prepared.enchantment());
        assertEquals(1, offered.central().getCount());
        assertEquals(1, offered.components().getFirst().getCount());
    }

    @Test void explicitVirtualPointCostOverridesOwnThePreparedPrice() {
        InfusionEnchantmentRecipe addon = new PlannedRecipe() {
            @Override public int calcXP(ItemStack central) { return 12; }
            @Override public int calcXPPoints(ItemStack central) { return 19; }
        };
        var prepared = ProgrammaticRecipes.prepareInfusion(addon, offered(), null, null);
        assertNotNull(prepared);
        assertEquals(19, prepared.xp());
    }

    @Test void unrepresentableAndNegativePricesNeverProduceAnAdmissiblePlan() {
        InfusionEnchantmentRecipe addon = new PlannedRecipe();
        addon.recipeXP = 21_863;
        var maximum = ProgrammaticRecipes.prepareInfusion(addon, offered(), null, null);
        assertNotNull(maximum);
        assertEquals(2_147_407_943, maximum.xp());
        addon.recipeXP = 21_864;
        assertThrows(ArithmeticException.class, () -> addon.calcXPPoints(offered().central()));
        assertNull(ProgrammaticRecipes.prepareInfusion(addon, offered(), null, null));
        addon.recipeXP = -1;
        assertNull(ProgrammaticRecipes.prepareInfusion(addon, offered(), null, null));
        var negativePointOverride = new PlannedRecipe() {
            @Override public int calcXPPoints(ItemStack central) { return -1; }
        };
        assertNull(ProgrammaticRecipes.prepareInfusion(negativePointOverride, offered(), null, null));
    }

    @Test void malformedLegacyFieldsCannotWrapTheNominalPriceIntoAFreePlan() {
        InfusionEnchantmentRecipe addon = new PlannedRecipe();
        addon.recipeXP = 1 << 30;
        InfusionRecipeInput offered = offered();
        offered.central().enchant(target, 3);
        assertThrows(ArithmeticException.class, () -> addon.calcXP(offered.central()));
        assertNull(ProgrammaticRecipes.prepareInfusion(addon, offered, null, null));
    }

    private static InfusionRecipeInput offered() {
        return new InfusionRecipeInput(new ItemStack(Items.IRON_PICKAXE), List.of(new ItemStack(Items.FLINT)));
    }

    /** Bypasses world matching to exercise only virtual pricing and plan freezing without a fake Level. */
    private static class PlannedRecipe extends InfusionEnchantmentRecipe {
        private PlannedRecipe() {
            super("", target, 1, new AspectList().add(Aspect.AIR, 1), new ItemStack[]{new ItemStack(Items.FLINT)});
        }
        @Override public boolean matches(ArrayList<ItemStack> input, ItemStack central, Level world, Player player) {
            return true;
        }
    }
}
