package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.entities.golems.GolemHelper;
import thaumcraft.common.lib.fluid.GolemFluidAccess;
import thaumcraft.common.lib.fluid.GolemFluidRules;

/**
 * The fluid half of the original golem helper.
 *
 * <p>Oracle {@code thaumcraft/common/entities/golems/GolemHelper.java:331-369}: the registered
 * liquids are the <b>still</b> fluids of 1.7.10, the fluid of the cargo narrows the set and the
 * fluid of a filled container in the filter is the only fluid that a filtered golem asks for.
 * Oracle {@code FluidContainerRegistry.getFluidForFilledItem} answers with water for a water
 * bucket, never with the flowing variant. No block is built here.</p>
 */
class GolemFluidRulesTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void theRegistrableLiquidsAreTheStillOnes() {
        // GolemHelper.getReggedLiquids:331-339 iterates FluidRegistry.getRegisteredFluidIDs(),
        // which holds water and lava, not the flowing render fluids and not the empty fluid.
        assertTrue(GolemFluidRules.registrable(Fluids.WATER));
        assertTrue(GolemFluidRules.registrable(Fluids.LAVA));
        assertFalse(GolemFluidRules.registrable(Fluids.FLOWING_WATER));
        assertFalse(GolemFluidRules.registrable(Fluids.FLOWING_LAVA));
        assertFalse(GolemFluidRules.registrable(Fluids.EMPTY));
        assertFalse(GolemFluidRules.registrable(null));
    }

    @Test void theRegisteredSetHoldsWaterAndLavaOnly() {
        List<ResourceLocation> liquids = GolemHelper.getReggedLiquids();
        assertTrue(liquids.contains(BuiltInRegistries.FLUID.getKey(Fluids.WATER)));
        assertTrue(liquids.contains(BuiltInRegistries.FLUID.getKey(Fluids.LAVA)));
        assertFalse(liquids.contains(BuiltInRegistries.FLUID.getKey(Fluids.EMPTY)));
        assertFalse(liquids.contains(BuiltInRegistries.FLUID.getKey(Fluids.FLOWING_WATER)));
        assertFalse(liquids.contains(BuiltInRegistries.FLUID.getKey(Fluids.FLOWING_LAVA)));
    }

    @Test void aLoadedCargoAsksForItsOwnFluidOnly() {
        // GolemHelper.getMissingLiquids:348 - fluidCarried != null && amount > 0 && fluidID != id.
        ResourceLocation water = BuiltInRegistries.FLUID.getKey(Fluids.WATER);
        ResourceLocation lava = BuiltInRegistries.FLUID.getKey(Fluids.LAVA);
        assertTrue(GolemFluidRules.carriedAccepts(water, lava, 0));
        assertTrue(GolemFluidRules.carriedAccepts(water, null, 0));
        assertTrue(GolemFluidRules.carriedAccepts(water, water, 1000));
        assertFalse(GolemFluidRules.carriedAccepts(lava, water, 1000));
    }

    @Test void aFilterAdmitsTheFluidOfItsOwnContainerOnly() {
        // GolemHelper.getMissingLiquids:354-363 - fis.isFluidEqual(fs) is the filter test.
        ResourceLocation water = BuiltInRegistries.FLUID.getKey(Fluids.WATER);
        ResourceLocation lava = BuiltInRegistries.FLUID.getKey(Fluids.LAVA);
        assertTrue(GolemFluidRules.filterAccepts(lava, false, null));
        assertTrue(GolemFluidRules.filterAccepts(water, true, water));
        assertFalse(GolemFluidRules.filterAccepts(lava, true, water));
        // A non-empty filter that is no filled container leaves no missing liquid.
        assertFalse(GolemFluidRules.filterAccepts(lava, true, null));
        assertFalse(GolemFluidRules.filterAccepts(water, true, null));
    }

    @Test void aBucketNamesTheStillFluidAndNeverTheFlowingVariant() {
        // FluidContainerRegistry.getFluidForFilledItem(water bucket) is the water of the original;
        // the 1.21.1 registry list is empty, flowing_water, water, flowing_lava, lava, and both
        // water entries share the same bucket item.
        assertEquals(BuiltInRegistries.FLUID.getKey(Fluids.WATER),
                GolemFluidAccess.bucketFluid(new ItemStack(Items.WATER_BUCKET)));
        assertEquals(BuiltInRegistries.FLUID.getKey(Fluids.LAVA),
                GolemFluidAccess.bucketFluid(new ItemStack(Items.LAVA_BUCKET)));
        assertEquals(BuiltInRegistries.FLUID.getKey(Fluids.WATER),
                GolemFluidAccess.fluidOfFilledContainer(new ItemStack(Items.WATER_BUCKET)));
        assertNull(GolemFluidAccess.bucketFluid(new ItemStack(Items.BUCKET)));
        assertNull(GolemFluidAccess.bucketFluid(new ItemStack(Items.STONE)));
        assertNull(GolemFluidAccess.bucketFluid(ItemStack.EMPTY));
    }
}
