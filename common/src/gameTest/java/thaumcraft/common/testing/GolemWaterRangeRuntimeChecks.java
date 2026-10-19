package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUpgrade;

/**
 * Water range on two real workers. An Iron Lumber golem has a trunk 7 blocks from its home (its radius is ceil(16 / 3) = 6) and an
 * Iron Harvest golem a ripe melon 5 blocks from its home (ceil(16 / 4) = 4); with the golems' own scan geometry neither can be
 * reached. After a window longer than a full scan cycle of the wider radius (so the absence is real, not a missed roll) a real
 * connected player installs a real Water upgrade through the native interaction, and the SAME already-running goals must then
 * (radius 7 and 5) fell the far end of the trunk and break the melon. The golems, home, trunk, melon, upgrade item and player are
 * explicit INPUTS; scanning, walking, the radius decision, the guarded removal and the drops are never supplied. NOT a Fishing
 * worker (its pond radius is only pinned by unit tests), a protection-mod, multiplayer, save/reload, network or visual check.
 */
public final class GolemWaterRangeRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int RADIUS = 9, TRUNK = 5;
    /** A full shuffled Harvest cycle at radius 5 is 121 columns * 5 ticks = 605 ticks. */
    public static final int SCAN_TICKS = 640, DEADLINE = 1950;

    private GolemWaterRangeRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var level = test.getLevel();
        var base = test.absolutePos(new BlockPos(3, 3, 3));
        BlockPos lumberHome = base, harvestHome = base.offset(0, 0, -24);
        BlockPos trunk = lumberHome.offset(7, 0, 0), melon = harvestHome.offset(5, 0, 0);

        Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        for (BlockPos home : List.of(lumberHome, harvestHome)) {
            for (BlockPos pos : BlockPos.betweenClosed(home.offset(-RADIUS, -1, -RADIUS), home.offset(RADIUS, 9, RADIUS))) {
                original.put(pos.immutable(), level.getBlockState(pos));
            }
        }
        Set<Integer> existingEntities = new HashSet<>();
        level.getAllEntities().forEach(entity -> existingEntities.add(entity.getId()));
        // The golems' pathfinder refuses to plan across unloaded chunks; release only the chunks this test forced.
        List<ChunkPos> forced = new ArrayList<>();
        var center = new ChunkPos(base);
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            var chunk = new ChunkPos(center.x + dx, center.z + dz);
            if (!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(chunk.x, chunk.z, true); forced.add(chunk); }
        }
        for (BlockPos pos : original.keySet()) {
            level.setBlock(pos, pos.getY() == base.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
        }
        for (int index = 0; index < TRUNK; index++) level.setBlock(trunk.above(index), Blocks.OAK_LOG.defaultBlockState(), QUIET);
        level.setBlock(melon, Blocks.MELON.defaultBlockState(), QUIET);

        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "wat-" + UUID.randomUUID().toString().substring(0, 8)),
                ClientInformation.createDefault());
        EntityGolemBase lumber = worker(level, GolemData.LUMBER, lumberHome), harvest = worker(level, GolemData.HARVEST, harvestHome);

        Runnable cleanup = () -> {
            try {
                lumber.discard();
                harvest.discard();
                level.getEntitiesOfClass(ItemEntity.class, new AABB(Vec3.atLowerCornerOf(base.offset(-RADIUS - 2, -2, -RADIUS - 26)),
                        Vec3.atLowerCornerOf(base.offset(RADIUS + 3, 12, RADIUS + 3))), item -> !existingEntities.contains(item.getId())).forEach(Entity::discard);
                original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
                for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            } finally {
                try {
                    if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                } finally { player.discard(); channel.finishAndReleaseAll(); }
            }
        };
        boolean[] done = {false}, installed = {false}, felled = {false}, broken = {false};
        try {
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.setNoGravity(true);
            test.assertTrue(level.mayInteract(player, base) && level.mayInteract(player, harvestHome), "fixture is not under unrelated spawn protection");
            test.assertTrue(level.addFreshEntity(lumber) && lumber.hasSupportedCore() && level.addFreshEntity(harvest) && harvest.hasSupportedCore(),
                    "a real Lumber worker and a real Harvest worker are admitted");
            test.assertTrue(lumber.getRange() == 16F && harvest.getRange() == 16F, "both start with the ordinary range 16");
        } catch (RuntimeException failure) {
            done[0] = true;
            cleanup.run();
            throw failure;
        }

        test.runAtTickTime(SCAN_TICKS, () -> {
            if (done[0]) return;
            try {
                test.assertTrue(logs(level, trunk) == TRUNK && level.getBlockState(melon).is(Blocks.MELON)
                                && drops(level, existingEntities, trunk, Items.OAK_LOG) == 0 && drops(level, existingEntities, melon, Items.MELON_SLICE) == 0,
                        "before any Water, " + SCAN_TICKS + " ticks (a full scan cycle) did not reach the trunk at 7 or the melon at 5: "
                                + logs(level, trunk) + " logs, melon " + level.getBlockState(melon));
                install(test, player, lumber);
                install(test, player, harvest);
                player.setPos(base.getX() + .5, base.getY() + 40, base.getZ() + .5);
                test.assertTrue(lumber.getRange() == 20F && harvest.getRange() == 20F && lumber.getUpgradeAmount(GolemUpgrade.WATER) == 1,
                        "both golems report range 20 after the real installation");
                installed[0] = true;
            } catch (RuntimeException failure) {
                done[0] = true;
                cleanup.run();
                throw failure;
            }
        });
        test.onEachTick(() -> {
            if (done[0] || !installed[0]) return;
            try {
                int remaining = logs(level, trunk);
                if (!felled[0] && remaining < TRUNK) {
                    test.assertTrue(remaining == TRUNK - 1 && !level.getBlockState(trunk.above(TRUNK - 1)).is(Blocks.OAK_LOG) && level.getBlockState(trunk).is(Blocks.OAK_LOG),
                            "the Water golem removed exactly ONE log, the far top end, of the trunk 7 blocks away: " + remaining + " left");
                    test.assertTrue(drops(level, existingEntities, trunk, Items.OAK_LOG) >= 1, "and its log dropped in the world");
                    felled[0] = true;
                }
                if (!broken[0] && !level.getBlockState(melon).is(Blocks.MELON)) {
                    test.assertTrue(level.getBlockState(melon).isAir() && drops(level, existingEntities, melon, Items.MELON_SLICE) >= 1,
                            "the Water golem broke the melon 5 blocks away and it dropped slices: " + level.getBlockState(melon));
                    broken[0] = true;
                }
                if (felled[0] && broken[0]) {
                    done[0] = true;
                    cleanup.run();
                    test.succeed();
                }
            } catch (RuntimeException failure) {
                done[0] = true;
                cleanup.run();
                throw failure;
            }
        });
        test.runAtTickTime(DEADLINE, () -> {
            if (done[0]) return;
            done[0] = true;
            String state = "installed=" + installed[0] + ", trunk logs left=" + logs(level, trunk) + ", melon=" + level.getBlockState(melon)
                    + ", ranges " + lumber.getRange() + "/" + harvest.getRange();
            cleanup.run();
            test.fail("the Water golems did not reach their far targets within " + DEADLINE + " ticks: " + state);
        });
    }

    private static EntityGolemBase worker(ServerLevel level, int core, BlockPos home) {
        var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
        golem.setGolemMaterial(6);
        golem.applyConfiguration(new GolemData(core, List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)));
        golem.setPos(Vec3.atBottomCenterOf(home));
        golem.setHome(home, Direction.UP);
        golem.setPersistenceRequired();
        return golem;
    }

    private static void install(GameTestHelper test, ServerPlayer player, EntityGolemBase golem) {
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftGolems.UPGRADE_WATER.get()));
        player.setPos(golem.position().add(1.2, 0, 0));
        test.assertTrue(golem.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME && golem.getUpgradeAmount(GolemUpgrade.WATER) == 1
                        && player.getMainHandItem().isEmpty(),
                "a real player installs one Water upgrade on the running " + golem.getGolemCore() + " worker through the native interaction");
    }

    private static int logs(ServerLevel level, BlockPos trunk) {
        int count = 0;
        for (int index = 0; index < TRUNK; index++) if (level.getBlockState(trunk.above(index)).is(Blocks.OAK_LOG)) count++;
        return count;
    }

    private static long drops(ServerLevel level, Set<Integer> existing, BlockPos at, net.minecraft.world.item.Item item) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(Vec3.atLowerCornerOf(at.offset(-4, -3, -4)), Vec3.atLowerCornerOf(at.offset(5, 9, 5))),
                entity -> !existing.contains(entity.getId()) && entity.getItem().is(item)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
    }
}
