package thaumcraft.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.utils.CropUtils;
import thaumcraft.common.lib.utils.InventoryUtils;

/** Actual native states/components at the public legacy helper boundary. */
class TailUtilityContractsReviewTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void cropGrowthUsesEachNativeCropsAgeProperty() {
        CropBlock wheat = (CropBlock) Blocks.WHEAT;
        assertTrue(CropUtils.canGrow(wheat.getStateForAge(0)));
        assertFalse(CropUtils.canGrow(wheat.getStateForAge(wheat.getMaxAge())));
        assertTrue(CropUtils.canGrow(Blocks.PUMPKIN_STEM.defaultBlockState()));
        assertFalse(CropUtils.canGrow(Blocks.PUMPKIN_STEM.defaultBlockState().setValue(StemBlock.AGE, StemBlock.MAX_AGE)));
        assertTrue(CropUtils.canGrow(Blocks.NETHER_WART.defaultBlockState()));
        assertFalse(CropUtils.canGrow(Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, NetherWartBlock.MAX_AGE)));
        assertFalse(CropUtils.canGrow(Blocks.STONE.defaultBlockState()));
        assertFalse(CropUtils.canGrow(null));

        CropBlock beetroot = (CropBlock) Blocks.BEETROOTS;
        assertTrue(CropUtils.canGrow(beetroot.getStateForAge(0)), "native beetroot grows with its own age0..3 property");
        assertFalse(CropUtils.canGrow(beetroot.getStateForAge(beetroot.getMaxAge())), "native beetroot stops at age3");
        CropBlock torchflower = (CropBlock) Blocks.TORCHFLOWER_CROP;
        assertTrue(CropUtils.canGrow(torchflower.getStateForAge(0)), "native torchflower also has its own age property");
        assertFalse(CropUtils.canGrow(torchflower.getStateForAge(torchflower.getMaxAge())), "the mature native torchflower is no longer a growing crop");
    }

    @Test void ignoringWearRetainsStrictNamesAndOtherComponents() {
        ItemStack pristine = new ItemStack(Items.IRON_SWORD);
        ItemStack worn = pristine.copy();
        worn.setDamageValue(17);
        assertFalse(InventoryUtils.areItemStacksEqual(pristine, worn, false, false, false), "ordinary comparison retains wear");
        assertTrue(InventoryUtils.areItemStacksEqual(pristine, pristine.copyWithCount(2), false, false, false), "this helper compares kind/data, not quantity");
        assertFalse(InventoryUtils.areItemStacksEqual(pristine, new ItemStack(Items.DIAMOND_SWORD), false, true, true));
        assertTrue(InventoryUtils.areItemStacksEqual(pristine, worn, false, true, false), "ignoreDamage ignores only the native DAMAGE component");

        ItemStack named = worn.copy();
        named.set(DataComponents.CUSTOM_NAME, Component.literal("kept name"));
        assertFalse(InventoryUtils.areItemStacksEqual(pristine, named, false, true, false), "ignoring wear must retain a custom name");
        ItemStack withData = worn.copy();
        CompoundTag data = new CompoundTag();
        data.putInt("review", 1);
        withData.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        assertFalse(InventoryUtils.areItemStacksEqual(pristine, withData, false, true, false), "ignoring wear must retain custom data");
        ItemStack withRepairCost = worn.copy();
        withRepairCost.set(DataComponents.REPAIR_COST, 3);
        assertFalse(InventoryUtils.areItemStacksEqual(pristine, withRepairCost, false, true, false), "other native components remain strict");
        assertTrue(InventoryUtils.areItemStacksEqual(pristine, named, false, true, true), "the independent ignoreNBT switch still permits other component differences");
        assertFalse(InventoryUtils.areItemStacksEqual(pristine, named, false, false, true), "ignoreNBT alone does not also ignore wear");
    }

    @Test void ignoredDamageRetainsTheOriginalWildcardAndNondurableRules() {
        // Native getDamageValue clamps to MAX_DAMAGE; ordinary stone cannot represent metadata.
        // A valid unbreakable tool retains nonzero damage while isDamageableItem is false.
        ItemStack plain = new ItemStack(Items.IRON_SWORD);
        plain.set(DataComponents.MAX_DAMAGE, 40000);
        plain.set(DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(false));
        ItemStack different = plain.copy();
        different.set(DataComponents.DAMAGE, 5);
        assertFalse(plain.isDamageableItem());
        assertTrue(different.getDamageValue() == 5);
        assertFalse(InventoryUtils.areItemStacksEqual(plain, different, false, true, false), "ordinary nondurable metadata differences remain strict");
        ItemStack wildcard = plain.copy();
        wildcard.set(DataComponents.DAMAGE, (int) Short.MAX_VALUE);
        assertTrue(wildcard.getDamageValue() == Short.MAX_VALUE);
        assertTrue(InventoryUtils.areItemStacksEqual(plain, wildcard, false, true, false), "the original wildcard still matches when damage is ignored");
        assertTrue(InventoryUtils.areItemStacksEqual(wildcard, plain, false, true, false), "the wildcard is symmetric");
        assertFalse(InventoryUtils.areItemStacksEqual(plain, wildcard, false, false, false), "the wildcard needs the ignoreDamage switch");
        wildcard.set(DataComponents.CUSTOM_NAME, Component.literal("still strict"));
        assertFalse(InventoryUtils.areItemStacksEqual(plain, wildcard, false, true, false), "a wildcard does not waive other components");
        ItemStack stone = new ItemStack(Items.STONE), unusualStone = stone.copy();
        unusualStone.set(DataComponents.DAMAGE, 5);
        assertFalse(InventoryUtils.areItemStacksEqual(stone, unusualStone, false, true, false),
                "ignoreDamage does not discard unrelated raw components on a nondurable native item");
    }
}
