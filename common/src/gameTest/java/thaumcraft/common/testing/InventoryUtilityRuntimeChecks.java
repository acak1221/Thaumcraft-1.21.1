package thaumcraft.common.testing;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import thaumcraft.common.lib.utils.InventoryUtils;

/** Native ownership boundaries for the public inventory insertion helpers. */
public final class InventoryUtilityRuntimeChecks {
    private InventoryUtilityRuntimeChecks() {}

    public static void insertionOnlyUsesTheActualDoubleChestPartner(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos origin = test.absolutePos(new BlockPos(2, 2, 2));
        Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        try {
            for (BlockPos pos : java.util.List.of(origin.west(), origin, origin.east())) {
                test.assertTrue(level.hasChunkAt(pos) && level.getBlockEntity(pos) == null,
                        "the inventory fixture replaces no foreign block entity");
                saved.put(pos, level.getBlockState(pos));
            }
            BlockState single = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
            level.setBlock(origin, single, quiet);
            // getConnectedDirection(SINGLE NORTH) points west even though no pair exists.
            BlockPos unrelatedPos = origin.relative(ChestBlock.getConnectedDirection(single));
            level.setBlock(unrelatedPos, single.setValue(ChestBlock.FACING, Direction.SOUTH), quiet);
            var source = (ChestBlockEntity) level.getBlockEntity(origin);
            var unrelated = (ChestBlockEntity) level.getBlockEntity(unrelatedPos);
            fill(source, 64); unrelated.setItem(0, new ItemStack(Items.STONE, 63));
            test.assertTrue(InventoryUtils.getDoubleChest(source) == null,
                    "two adjacent independent single chests have separate ownership");
            ItemStack left = InventoryUtils.placeItemStackIntoInventory(new ItemStack(Items.STONE), source, -1, true);
            test.assertTrue(left != null && left.is(Items.STONE) && left.getCount() == 1
                            && unrelated.getItem(0).getCount() == 63,
                    "a full single chest returns overflow without placing it in its separate neighbor");

            level.setBlock(origin, single.setValue(ChestBlock.TYPE, ChestType.LEFT), quiet);
            BlockPos partnerPos = origin.relative(ChestBlock.getConnectedDirection(level.getBlockState(origin)));
            level.setBlock(partnerPos, Blocks.TRAPPED_CHEST.defaultBlockState()
                    .setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT), quiet);
            test.assertTrue(InventoryUtils.getDoubleChest(source) == null,
                    "a trapped chest is never the other half of a normal chest");
            level.setBlock(partnerPos, single.setValue(ChestBlock.TYPE, ChestType.LEFT), quiet);
            test.assertTrue(InventoryUtils.getDoubleChest(source) == null,
                    "two same-side halves do not form a native double chest");
            level.setBlock(partnerPos, single.setValue(ChestBlock.TYPE, ChestType.RIGHT)
                    .setValue(ChestBlock.FACING, Direction.SOUTH), quiet);
            test.assertTrue(InventoryUtils.getDoubleChest(source) == null,
                    "a complementary half facing away is not a mutual native pair");

            level.setBlock(partnerPos, single.setValue(ChestBlock.TYPE, ChestType.RIGHT), quiet);
            var partner = (ChestBlockEntity) level.getBlockEntity(partnerPos);
            partner.setItem(0, new ItemStack(Items.STONE, 63));
            test.assertTrue(InventoryUtils.getDoubleChest(source) == partner && InventoryUtils.getDoubleChest(partner) == source,
                    "both real native halves resolve each other");
            ItemStack before = partner.getItem(0).copy();
            test.assertTrue(InventoryUtils.placeItemStackIntoInventory(new ItemStack(Items.STONE), source, -1, false) == null
                            && ItemStack.matches(before, partner.getItem(0)),
                    "simulation finds real partner capacity without changing either half");
            test.assertTrue(InventoryUtils.placeItemStackIntoInventory(new ItemStack(Items.STONE), source, -1, true) == null
                            && partner.getItem(0).getCount() == 64 && unrelated.getItem(0).getCount() == 63,
                    "committed overflow fills the actual partner exactly once and leaves the unrelated neighbor alone");
        } finally {
            for (BlockPos pos : saved.keySet()) {
                if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) chest.clearContent();
            }
            saved.forEach((pos, state) -> level.setBlock(pos, state, quiet));
        }
        test.succeed();
    }

    private static void fill(ChestBlockEntity chest, int count) {
        for (int slot = 0; slot < chest.getContainerSize(); slot++) chest.setItem(slot, new ItemStack(Items.STONE, count));
    }
}
