package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftWards;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.WandFocusActions;
import thaumcraft.common.lib.world.WardedBehaviour;
import thaumcraft.common.lib.world.WardingCasts;
import thaumcraft.common.lib.world.WardingOverlay;
import thaumcraft.common.tiles.TileWarded;

/** Actual owned wards through native enchanting, beacon, soil, leaf-distance and source-ore callbacks. */
public final class WardingBehaviourRuntimeChecks {
    private WardingBehaviourRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        var check = new Check(test);
        try { check.prepare(); } catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        test.runAfterDelay(5, () -> check.stage(check::start));
        test.runAfterDelay(20, () -> check.stage(() -> { check.leaves(); check.restoreOre(); }));
        test.runAfterDelay(95, () -> check.stage(() -> { check.beacon(); check.finish(); test.succeed(); }));
    }
    private static final class Check {
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos base;
        final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        final EmbeddedChannel channel = new EmbeddedChannel(connection);
        ServerPlayer player;
        ItemStack wand;
        BlockPos leafPos, beaconPos, orePos;
        boolean finished;
        Check(GameTestHelper test) { this.test = test; level = test.getLevel(); base = test.absolutePos(new BlockPos(3, 125, 3)); }
        void stage(Runnable action) { if (finished) return; try { action.run(); } catch (RuntimeException | Error failure) { finish(); throw failure; } }
        void prepare() {
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-4, -2, -4), base.offset(32, 5, 24))) {
                test.assertTrue(level.getBlockEntity(pos) == null, "forwarding fixture does not overwrite a real inventory");
                saved.put(pos.immutable(), level.getBlockState(pos));
            }
            forced.addAll(GolemChunks.hold(level, saved.keySet()));
            saved.forEach((pos, state) -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS));
        }
        TileWarded ward(BlockPos pos, BlockState source) {
            level.setBlock(pos, source, Block.UPDATE_CLIENTS);
            // A top-down actual ray reaches every cell of the solid beacon platform without hitting its west neighbour.
            player.setPos(pos.getX() + .5, pos.getY() + .88, pos.getZ() + .5);
            player.setYRot(0); player.setYHeadRot(0); player.setXRot(90);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            WandFocusActions.use(player, InteractionHand.MAIN_HAND);
            test.assertTrue(level.getBlockState(pos).is(ThaumcraftWards.WARDED.get()) && level.getBlockEntity(pos) instanceof TileWarded,
                    "a real held-focus ray wards the behaviour source " + source);
            var tile = (TileWarded) level.getBlockEntity(pos);
            test.assertTrue(tile.originalState() == source && tile.ownedBy(player.getUUID()), "actual remembered state and owner");
            return tile;
        }
        void start() {
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "ward-behaviour"), ClientInformation.createDefault());
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true);
            var amounts = new LinkedHashMap<String, Integer>();
            for (String primal : WandData.PRIMALS) amounts.put(primal, 1000);
            wand = new ItemStack(ThaumcraftContent.WAND.get());
            wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", "gold", amounts));
            wand.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(new ItemStack(ThaumcraftFoci.WARDING_FOCUS.get())));
            bookshelf(); soil(); ore(); logs(); platform();
            player.setPos(base.getX() + 30, base.getY() + 4, base.getZ() + 20);
        }
        void bookshelf() {
            BlockPos table = base, shelf = table.east(2);
            level.setBlock(table, Blocks.ENCHANTING_TABLE.defaultBlockState(), Block.UPDATE_CLIENTS);
            ward(shelf, Blocks.BOOKSHELF.defaultBlockState());
            test.assertTrue(EnchantingTableBlock.isValidBookShelf(level, table, new BlockPos(2, 0, 0)),
                    "the actual vanilla enchanting-table lookup still counts the warded bookshelf through its air gap");
            test.assertTrue(level.getBlockState(shelf).getBlock().getCloneItemStack(level, shelf, level.getBlockState(shelf)).is(Items.BOOKSHELF),
                    "native pick-block remembers the actual original item, not an obtainable wrapper");
            level.setBlock(table.east(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            test.assertTrue(!EnchantingTableBlock.isValidBookShelf(level, table, new BlockPos(2, 0, 0)), "an obstructed native enchanting gap still refuses");
        }
        void soil() {
            plant(base.offset(6, 0, 0), Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DANDELION.defaultBlockState());
            plant(base.offset(6, 0, 5), Blocks.SAND.defaultBlockState(), Blocks.CACTUS.defaultBlockState());
            BlockPos cane = base.offset(6, 0, 10);
            BlockPos water = cane.east();
            for (Direction face : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST})
                level.setBlock(water.relative(face), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(water, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            plant(cane, Blocks.SAND.defaultBlockState(), Blocks.SUGAR_CANE.defaultBlockState());
            plant(base.offset(6, 0, 18), Blocks.MYCELIUM.defaultBlockState(), Blocks.RED_MUSHROOM.defaultBlockState());
        }
        void plant(BlockPos soil, BlockState source, BlockState plant) {
            ward(soil, source);
            BlockPos pos = soil.above();
            test.assertTrue(plant.canSurvive(level, pos), "actual native plant soil check accepts remembered " + source);
            level.setBlock(pos, plant, Block.UPDATE_ALL);
            test.assertTrue(level.getBlockState(pos).is(plant.getBlock()), "native neighbour updates retain the supported plant");
        }
        void ore() {
            BlockPos pos = orePos = base.offset(12, 0, 0);
            BlockState original = Blocks.REDSTONE_ORE.defaultBlockState().setValue(RedStoneOreBlock.LIT, false);
            TileWarded tile = ward(pos, original);
            BlockState wrapper = level.getBlockState(pos);
            wrapper.attack(level, pos, player);
            wrapper.getBlock().stepOn(level, pos, wrapper, player);
            test.assertTrue(level.getBlockState(pos) == wrapper && level.getBlockEntity(pos) == tile && tile.originalState() == original,
                    "real redstone-ore attack/step effects cannot replace the protected block with an ordinary lit ore");
            BlockPos adjacent = pos.east();
            boolean caught = false;
            try { WardedBehaviour.invoke(level, pos, original, () -> {
                level.setBlock(adjacent, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                throw new IllegalStateException("ward-source-callback-fixture");
            }); }
            catch (IllegalStateException expected) { caught = true; }
            test.assertTrue(caught && level.getBlockState(adjacent).is(Blocks.DIRT),
                    "delegated source callbacks may still perform ordinary native writes at another coordinate");
            BlockPos redstone = base.offset(12, 0, 5);
            level.setBlock(redstone, Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            test.assertTrue(level.getBlockState(redstone).getSignal(level, redstone, Direction.UP) == 15, "ordinary redstone block supplies power");
            ward(redstone, Blocks.REDSTONE_BLOCK.defaultBlockState());
            test.assertTrue(level.getBlockState(redstone).getSignal(level, redstone, Direction.UP) == 0,
                    "original BlockWarded does not forward redstone power; no accidental new power feature");
        }
        void restoreOre() {
            player.setPos(orePos.getX() + .5, orePos.getY() + .88, orePos.getZ() + .5);
            player.setYRot(0); player.setYHeadRot(0); player.setXRot(90);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            WandFocusActions.use(player, InteractionHand.MAIN_HAND);
            test.assertTrue(level.getBlockState(orePos) == Blocks.REDSTONE_ORE.defaultBlockState() && level.getBlockEntity(orePos) == null,
                    "an exceptional source callback leaks no guard: the real owner can subsequently unward the same coordinate");
        }
        void logs() {
            BlockPos log = base.offset(12, 0, 12);
            ward(log, Blocks.OAK_LOG.defaultBlockState());
            leafPos = log.east();
            level.setBlock(leafPos, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 7).setValue(LeavesBlock.PERSISTENT, false), Block.UPDATE_ALL);
            level.scheduleTick(leafPos, Blocks.OAK_LEAVES, 1);
        }
        void leaves() {
            test.assertTrue(level.getBlockState(leafPos).is(Blocks.OAK_LEAVES) && level.getBlockState(leafPos).getValue(LeavesBlock.DISTANCE) == 1,
                    "the actual scheduled native leaf-distance tick recognizes its warded original log and does not decay it");
        }
        void platform() {
            BlockPos centre = base.offset(24, 0, 15);
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) ward(centre.offset(x, 0, z), Blocks.IRON_BLOCK.defaultBlockState());
            test.assertTrue(WardingOverlay.mask(level, centre, Direction.UP, player.getUUID()) == 255
                            && WardingOverlay.mask(level, centre, Direction.UP, UUID.randomUUID()) == 0,
                    "all eight real same-owner wards connect, foreign UUIDs never share their runes");
            test.assertTrue(!WardingOverlay.visible(level, centre, Direction.WEST) && WardingOverlay.visible(level, centre, Direction.UP),
                    "internal wrapper faces disappear while the outer face remains visible");
            beaconPos = centre.above();
            level.setBlock(beaconPos, Blocks.BEACON.defaultBlockState(), Block.UPDATE_ALL);
            test.assertTrue(level.getBlockEntity(beaconPos) instanceof BeaconBlockEntity, "a real native beacon is the live consumer");
        }
        void beacon() {
            var beacon = (BeaconBlockEntity) level.getBlockEntity(beaconPos);
            test.assertTrue(beacon != null && beacon.saveWithoutMetadata(level.registryAccess()).getInt("Levels") == 1
                            && !beacon.getBeamSections().isEmpty(),
                    "actual world beacon ticks accept the full warded iron platform and retain its beam");
        }
        void finish() {
            if (finished) return; finished = true;
            WardingCasts.clear(level);
            saved.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS));
            if (player != null) {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                player.discard();
            }
            for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            channel.finishAndReleaseAll();
        }
    }
}
