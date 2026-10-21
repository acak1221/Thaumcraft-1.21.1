package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.BlockBellows;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftBellows;
import thaumcraft.common.config.ThaumcraftResources;
import thaumcraft.common.mixin.VanillaFurnaceBellowsAccess;
import thaumcraft.common.tiles.TileBellows;
import thaumcraft.common.tiles.TileCrucible;

/** Native Bellows flow for the central loader tests; original TileBellows:29-92. */
public final class BellowsRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private BellowsRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3));
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<BlockPos, BlockEntity> blockEntities = new LinkedHashMap<>();
        // Include the immediate placement-update halo, not an unbounded world scan.
        for (BlockPos mutable : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            BlockPos saved = mutable.immutable();
            states.put(saved, level.getBlockState(saved));
            if (level.getBlockEntity(saved) != null) blockEntities.put(saved, level.getBlockEntity(saved));
        }
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var position = player.position();
        var motion = player.getDeltaMovement();
        var gameType = player.gameMode.getGameModeForPlayer();
        var abilities = new CompoundTag();
        player.getAbilities().addSaveData(abilities);
        int selected = player.getInventory().selected;
        boolean sneaking = player.isShiftKeyDown();
        var used = Stats.ITEM_USED.get(ThaumcraftBellows.BELLOWS_ITEM.get());
        int usedBefore = player.getStats().getValue(used);
        try {
            // Detach originals before replacing blocks: their inventories and
            // transient BE state must neither spill nor become test resources.
            for (BlockPos saved : blockEntities.keySet()) level.removeBlockEntity(saved);
            for (BlockPos saved : states.keySet()) set(level, saved, Blocks.AIR.defaultBlockState());
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(true); // Native sneak placement, not opening the furnace menu.
            player.setPos(Vec3.atCenterOf(pos).add(2, 0, 2));
            set(level, pos, Blocks.FURNACE.defaultBlockState());
            var furnace = (FurnaceBlockEntity) level.getBlockEntity(pos);
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, 3));
            furnace.setItem(1, new ItemStack(Items.COAL, 2));
            ItemStack input = furnace.getItem(0).copy(), fuel = furnace.getItem(1).copy();
            cooking(level, furnace, 40, 200);
            var data = (VanillaFurnaceBellowsAccess) furnace;
            TileBellows bellows = null;
            // BlockWoodenDeviceItem:56-70: every clicked face points inward.
            for (Direction face : Direction.values()) {
                bellows = place(test, player, pos, face);
                var saved = bellows.saveWithFullMetadata(level.registryAccess());
                test.assertTrue(bellows.getFacing() == face.getOpposite() && saved.getBoolean("onVanillaFurnace")
                        && saved.contains("orientation", Tag.TAG_BYTE) && saved.getByte("orientation") == face.getOpposite().get3DDataValue()
                        && TileBellows.getBellows(level, pos, List.of(face)) == 1, "actual six-way inward placement and vanilla-furnace attachment: " + face);
                var bounds = level.getBlockState(bellows.getBlockPos()).getCollisionShape(level, bellows.getBlockPos()).bounds();
                test.assertTrue(Math.abs(bounds.minX - .1) < .00001 && Math.abs(bounds.minZ - .1) < .00001
                        && Math.abs(bounds.maxX - .9) < .00001 && Math.abs(bounds.maxZ - .9) < .00001
                        && bounds.minY == 0 && bounds.maxY == 1, "original static collision does not rotate with the nozzle: " + face);
                data.thaumcraft$setCookingProgress(40);
                ticks(level, bellows, 1);
                test.assertTrue(data.thaumcraft$getCookingProgress() == 40, "one bellows tick cannot boost cooking: " + face);
                ticks(level, bellows, 1);
                test.assertTrue(data.thaumcraft$getCookingProgress() == (face.getAxis().isHorizontal() ? 41 : 40),
                        "two ticks add exactly one, preserving the original ignored-Y vertical quirk: " + face);
                if (face != Direction.EAST) set(level, bellows.getBlockPos(), Blocks.AIR.defaultBlockState());
            }
            test.assertTrue(!level.getBlockState(pos).getValue(FurnaceBlock.LIT) && furnace.saveWithoutMetadata(level.registryAccess()).getShort("BurnTime") == 0
                    && ItemStack.matches(input, furnace.getItem(0)) && ItemStack.matches(fuel, furnace.getItem(1)) && furnace.getItem(2).isEmpty(),
                    "source boost works even unlit, without spending input/fuel or manufacturing output");

            BlockPos bellowsPos = pos.east(), power = bellowsPos.above();
            data.thaumcraft$setCookingProgress(40);
            ticks(level, bellows, 1);
            set(level, power, Blocks.REDSTONE_BLOCK.defaultBlockState());
            test.assertTrue(bellows.isPowered() && TileBellows.getBellows(level, pos, Direction.Plane.HORIZONTAL) == 0, "real redstone excludes an otherwise inward-facing bellows");
            ticks(level, bellows, 6);
            test.assertTrue(data.thaumcraft$getCookingProgress() == 40, "power pauses cooking assistance");
            set(level, power, Blocks.AIR.defaultBlockState());
            ticks(level, bellows, 1);
            test.assertTrue(data.thaumcraft$getCookingProgress() == 41, "unpowering resumes the saved half-cycle rather than resetting or advancing it");
            for (int progress : new int[] {0, 198, 199, 200}) {
                data.thaumcraft$setCookingProgress(progress);
                ticks(level, bellows, 2);
                test.assertTrue(data.thaumcraft$getCookingProgress() == (progress == 198 ? 199 : progress), "original strict 0<progress<199 boost guard: " + progress);
            }
            cooking(level, furnace, 8, 10);
            ticks(level, bellows, 2);
            test.assertTrue(data.thaumcraft$getCookingProgress() == 9, "short native recipe retains its last completion tick");
            ticks(level, bellows, 2);
            test.assertTrue(data.thaumcraft$getCookingProgress() == 9, "datapack-duration safety cannot skip native equality completion");
            cooking(level, furnace, 40, 200);

            var saved = bellows.saveWithFullMetadata(level.registryAccess());
            level.removeBlockEntity(bellowsPos);
            bellows = new TileBellows(bellowsPos, level.getBlockState(bellowsPos));
            bellows.loadWithComponents(saved, level.registryAccess());
            level.setBlockEntity(bellows);
            ticks(level, bellows, 2);
            test.assertTrue(data.thaumcraft$getCookingProgress() == 41 && bellows.getFacing() == Direction.WEST
                    && bellows.getUpdateTag(level.registryAccess()).getBoolean("onVanillaFurnace") && bellows.getUpdatePacket() != null,
                    "native BE replacement retains the furnace-attachment flag, blockstate direction and update data");

            set(level, bellowsPos, Blocks.AIR.defaultBlockState());
            furnace.clearContent();
            set(level, pos, Blocks.STONE.defaultBlockState());
            bellows = place(test, player, pos, Direction.EAST);
            test.assertTrue(!bellows.saveWithoutMetadata(level.registryAccess()).getBoolean("onVanillaFurnace"), "non-furnace placement does not opt into vanilla assistance");
            set(level, pos, Blocks.FURNACE.defaultBlockState());
            furnace = (FurnaceBlockEntity) level.getBlockEntity(pos);
            cooking(level, furnace, 40, 200);
            ticks(level, bellows, 2);
            test.assertTrue(((VanillaFurnaceBellowsAccess) furnace).thaumcraft$getCookingProgress() == 40, "a later furnace does not retroactively change the persistent placement flag");

            set(level, bellowsPos, Blocks.AIR.defaultBlockState());
            set(level, pos, ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState());
            set(level, pos.below(), ThaumcraftResources.NITOR.get().defaultBlockState());
            // TileCrucible:360-369 counts horizontal IDs, NOT TileBellows.getBellows.
            set(level, pos.above(), ThaumcraftBellows.BELLOWS.get().defaultBlockState().setValue(BlockBellows.FACING, Direction.DOWN));
            var crucible = (TileCrucible) level.getBlockEntity(pos);
            test.assertTrue(crucible.fillWater(1000, true) == 1000, "registered crucible accepts real water above native Nitor");
            TileCrucible.tick(level, pos, level.getBlockState(pos), crucible);
            test.assertTrue(crucible.getHeat() == 1, "a vertical bellows is not a crucible heat neighbor");
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos adjacent = pos.relative(side);
                set(level, adjacent, ThaumcraftBellows.BELLOWS.get().defaultBlockState().setValue(BlockBellows.FACING, side));
                set(level, adjacent.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                test.assertTrue(((TileBellows) level.getBlockEntity(adjacent)).isPowered(), "crucible fixture bellows is actually powered: " + side);
            }
            test.assertTrue(TileBellows.getBellows(level, pos, Direction.Plane.HORIZONTAL) == 0, "powered, outward nozzles supply no directional machine boost");
            // Original TileCrucible:69 and :98-100 - the bellows are counted ONCE, on the first
            // tick, and the count is never invalidated, so a bellows that is placed later is not
            // seen by the crucible any more.
            TileCrucible.tick(level, pos, level.getBlockState(pos), crucible);
            test.assertTrue(crucible.getHeat() == 2 && crucible.getWaterAmount() == 1000 && crucible.tagAmount() == 0,
                    "the cached bellows count of the original does not see a bellows placed after the first tick");
            // A fresh crucible tile counts the four bellows again and heats by one plus two each.
            // The block has to be replaced, because the same state keeps the same tile entity and
            // with it the cached bellows count.
            set(level, pos, Blocks.AIR.defaultBlockState());
            set(level, pos, ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState());
            var counted = (TileCrucible) level.getBlockEntity(pos);
            counted.fillWater(1000, true);
            TileCrucible.tick(level, pos, level.getBlockState(pos), counted);
            test.assertTrue(counted.getHeat() == 9 && counted.getWaterAmount() == 1000 && counted.tagAmount() == 0,
                    "all four actual adjacent bellows add two crucible heat each, regardless of power or direction");
        } finally {
            try {
                for (BlockPos saved : states.keySet()) level.removeBlockEntity(saved);
                for (var entry : states.entrySet()) set(level, entry.getKey(), entry.getValue());
                for (var entry : blockEntities.entrySet()) level.setBlockEntity(entry.getValue());
            } finally {
                player.getInventory().clearContent();
                for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
                player.getInventory().selected = selected;
                player.setGameMode(gameType);
                player.getAbilities().loadSaveData(abilities);
                player.onUpdateAbilities();
                player.setShiftKeyDown(sneaking);
                player.setPos(position);
                player.setDeltaMovement(motion);
                player.getStats().setValue(player, used, usedBefore);
            }
        }
        test.succeed();
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) { level.setBlock(pos, state, QUIET); }
    private static TileBellows place(GameTestHelper test, ServerPlayer player, BlockPos support, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftBellows.BELLOWS_ITEM.get(), 2));
        var hit = new BlockHitResult(Vec3.atCenterOf(support).relative(face, .5), face, support, false);
        test.assertTrue(player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit).consumesAction()
                && player.serverLevel().getBlockEntity(support.relative(face)) instanceof TileBellows && player.getMainHandItem().getCount() == 1,
                "native item placement creates one registered bellows and consumes exactly one item: " + face);
        return (TileBellows) player.serverLevel().getBlockEntity(support.relative(face));
    }
    private static void ticks(ServerLevel level, TileBellows tile, int ticks) {
        for (int tick = 0; tick < ticks; tick++) TileBellows.tick(level, tile.getBlockPos(), level.getBlockState(tile.getBlockPos()), tile);
    }
    private static void cooking(ServerLevel level, FurnaceBlockEntity furnace, int progress, int total) {
        // Actual 1.21.1 AbstractFurnaceBlockEntity load/save keys, not a mock timer.
        CompoundTag tag = furnace.saveWithoutMetadata(level.registryAccess());
        tag.putShort("BurnTime", (short) 0);
        tag.putShort("CookTime", (short) progress);
        tag.putShort("CookTimeTotal", (short) total);
        furnace.loadWithComponents(tag, level.registryAccess());
    }
}
