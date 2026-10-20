package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.config.ThaumcraftContent;

/** Original BlockCrystal.getDrops: six shards of the crystal's own type through the loaded loot tables, Silk/Fortune ignored. */
public final class CrystalDropRuntimeChecks {
    private static final String[] TAGS = {"aer", "ignis", "aqua", "terra", "ordo", "perditio"};

    private CrystalDropRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var level = test.getLevel();
        var player = test.makeMockServerPlayerInLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3));
        var enchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
        silk.enchant(enchantments.getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        ItemStack fortune = new ItemStack(Items.IRON_PICKAXE);
        fortune.enchant(enchantments.getHolderOrThrow(Enchantments.FORTUNE), 3);
        var stateBefore = level.getBlockState(pos);
        try {
            for (String tag : TAGS) {
                Block crystal = ThaumcraftContent.CRYSTALS.get(tag).get();
                var shard = ThaumcraftContent.SHARDS.get(tag).get();
                var state = crystal.defaultBlockState();
                for (ItemStack tool : new ItemStack[] {ItemStack.EMPTY, silk, fortune}) {
                    var drops = Block.getDrops(state, level, pos, null, player, tool);
                    int total = drops.stream().filter(stack -> stack.is(shard)).mapToInt(ItemStack::getCount).sum();
                    test.assertTrue(total == 6 && drops.stream().allMatch(stack -> stack.is(shard)),
                            "crystal " + tag + " drops exactly six own shards whatever the tool, got " + drops);
                }
                level.setBlock(pos, state, Block.UPDATE_ALL);
                AABB box = new AABB(pos).inflate(2);
                level.getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
                test.assertTrue(level.destroyBlock(pos, true), "crystal " + tag + " breaks");
                int dropped = level.getEntitiesOfClass(ItemEntity.class, box).stream()
                        .filter(entity -> entity.getItem().is(shard)).mapToInt(entity -> entity.getItem().getCount()).sum();
                test.assertTrue(dropped == 6, "real breakage of crystal " + tag + " spawns six shards, got " + dropped);
                level.getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
            }
        } finally {
            level.setBlock(pos, stateBefore.isAir() ? Blocks.AIR.defaultBlockState() : stateBefore, Block.UPDATE_ALL);
        }
        test.succeed();
    }
}
