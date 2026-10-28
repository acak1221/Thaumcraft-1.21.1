package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.ThaumcraftApiHelper.Scans;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeSyncPayload;
import thaumcraft.common.lib.research.PlayerKnowledge;

/**
 * The knowledge, bonus-tag and tree-felling helpers of the add-on API ({@code ThaumcraftApiHelper}) in a running world:
 * real connected players whose outbound packets are read, and real blocks. Fixtures declare knowledge prerequisites
 * directly (a scan is written with the knowledge methods the thaumometer itself ends in); they are not a claim that a
 * thaumometer produced them.
 */
public final class KnowledgeApiRuntimeChecks {
    private KnowledgeApiRuntimeChecks() {}

    /** A real player placed in the world with a recording connection, so the packets of the server can be read. */
    private static final class Connected implements AutoCloseable {
        final ServerPlayer player;
        private final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        private final EmbeddedChannel channel = new EmbeddedChannel(connection);
        private final ServerLevel level;

        Connected(GameTestHelper test, GameType mode) {
            level = test.getLevel();
            player = new ServerPlayer(level.getServer(), level,
                    new GameProfile(UUID.randomUUID(), "kn-" + UUID.randomUUID().toString().substring(0, 8)), ClientInformation.createDefault());
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.setGameMode(mode);
            player.setNoGravity(true);
            capture();
        }

        PlayerKnowledge knowledge() { return KnowledgeAccess.get(player); }

        /** The Thaumcraft payloads sent since the last call, in order. */
        List<CustomPacketPayload> capture() {
            connection.flushChannel();
            channel.runPendingTasks();
            List<CustomPacketPayload> payloads = new ArrayList<>();
            Object packet;
            while ((packet = channel.readOutbound()) != null) {
                try {
                    if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload().type().id().getNamespace().equals("thaumcraft")) {
                        payloads.add(custom.payload());
                    }
                } finally {
                    ReferenceCountUtil.release(packet);
                }
            }
            return payloads;
        }

        /**
         * The knowledge snapshots of the sync packets sent since the last call; any other Thaumcraft packet fails the
         * test. An element is null when the payload of the loader cannot be read back (see {@link #decode}).
         */
        List<PlayerKnowledge> syncs() {
            List<PlayerKnowledge> states = new ArrayList<>();
            for (CustomPacketPayload payload : capture()) {
                if (!payload.type().id().equals(KnowledgeSyncPayload.TYPE.id())) {
                    throw new AssertionError("an unexpected Thaumcraft packet: " + payload.type().id());
                }
                states.add(decode(payload));
            }
            return states;
        }

        /**
         * The snapshot a client would load from a sync packet. Architectury sends its payloads as a byte array inside a
         * wrapper record, so the bytes are read back with the payload's own stream codec; a payload of another shape
         * gives null and the caller skips the content check (the packet is still counted).
         */
        PlayerKnowledge decode(CustomPacketPayload payload) {
            CompoundTag state;
            if (payload instanceof KnowledgeSyncPayload sync) {
                state = sync.state();
            } else {
                try {
                    byte[] bytes = (byte[]) payload.getClass().getMethod("payload").invoke(payload);
                    state = KnowledgeSyncPayload.STREAM_CODEC.decode(new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), level.registryAccess())).state();
                } catch (ReflectiveOperationException | ClassCastException | IllegalArgumentException unreadable) {
                    thaumcraft.common.Thaumcraft.LOG.info("KnowledgeApiRuntimeChecks: sync payload {} is not readable back, content check skipped", payload.getClass().getName());
                    return null;
                }
            }
            PlayerKnowledge client = new PlayerKnowledge();
            client.load(state);
            thaumcraft.common.Thaumcraft.LOG.info("KnowledgeApiRuntimeChecks: sync payload {} decoded", payload.getClass().getName());
            return client;
        }

        @Override public void close() {
            level.getServer().getPlayerList().remove(player);
            channel.finishAndReleaseAll();
        }
    }

    private static Set<String> set(String... keys) { return new LinkedHashSet<>(List.of(keys)); }

    // =============================================================================== A3: scans and discovery

    public static void scansAndDiscovery(GameTestHelper test) {
        try (Connected a = new Connected(test, GameType.SURVIVAL); Connected b = new Connected(test, GameType.SURVIVAL)) {
            PlayerKnowledge ka = a.knowledge();
            PlayerKnowledge kb = b.knowledge();
            ka.reset();
            kb.reset();
            ka.scanItem("@minecraft:diamond");
            ka.scanItem("#minecraft:stone");
            ka.scanEntity("@entity:Pig");
            ka.scanPhenomena("@NODEknowledge-test");

            ItemStack diamond = new ItemStack(Items.DIAMOND, 5);
            test.assertTrue(ThaumcraftApiHelper.hasScannedItem(a.player, diamond), "the thaumometer marker of the item counts, whatever the count");
            test.assertTrue(ThaumcraftApiHelper.hasScannedItem(a.player, new ItemStack(Items.STONE)), "the advanced marker counts as scanned");
            test.assertTrue(!ThaumcraftApiHelper.hasScannedItem(a.player, new ItemStack(Items.DIRT)), "an item nobody scanned");
            test.assertTrue(!ThaumcraftApiHelper.hasScannedItem(a.player, ItemStack.EMPTY), "no stack, nothing scanned");
            test.assertTrue(!ThaumcraftApiHelper.hasScannedItem(null, diamond), "no player, nothing scanned");
            test.assertTrue(!ThaumcraftApiHelper.hasScannedItem(b.player, diamond), "the knowledge of another player is separate");

            Scans scans = ThaumcraftApiHelper.getScans(a.player);
            test.assertTrue(List.copyOf(scans.items()).equals(List.of("@minecraft:diamond", "#minecraft:stone")), "items in scan order: " + scans.items());
            test.assertTrue(scans.entities().equals(Set.of("@entity:Pig")) && scans.phenomena().equals(Set.of("@NODEknowledge-test")), "entities and phenomena");
            test.assertTrue(ThaumcraftApiHelper.getScans(null).isEmpty(), "no player, no scans");

            // Copy the scans of A to B, as a Tome of Sharing does.
            b.capture();
            int added = ThaumcraftApiHelper.addScans(b.player, scans);
            test.assertTrue(added == 4, "four new markers, got " + added);
            List<PlayerKnowledge> syncs = b.syncs();
            test.assertTrue(syncs.size() == 1, "exactly one knowledge sync packet, got " + syncs.size());
            PlayerKnowledge seenByTheClient = syncs.get(0);
            if (seenByTheClient != null) {
                test.assertTrue(seenByTheClient.getScannedItems().equals(scans.items()) && seenByTheClient.getScannedEntities().equals(scans.entities())
                        && seenByTheClient.getScannedPhenomena().equals(scans.phenomena()), "the sync packet carries the new scans");
            }
            test.assertTrue(ThaumcraftApiHelper.getScans(b.player).equals(scans), "B now holds exactly the scans of A");
            test.assertTrue(ThaumcraftApiHelper.hasScannedItem(b.player, diamond), "and the item readout agrees");
            test.assertTrue(kb.getAspectsDiscovered().values().stream().allMatch(pool -> pool == 0) && kb.getResearchComplete().isEmpty(),
                    "scans give no research points, no clue and no research");

            // Idempotent: nothing new, nothing sent.
            test.assertTrue(ThaumcraftApiHelper.addScans(b.player, scans) == 0, "the same scans again are not new");
            test.assertTrue(b.capture().isEmpty(), "and nothing is sent when nothing changed");
            test.assertTrue(ThaumcraftApiHelper.addScans(b.player, Scans.EMPTY) == 0 && ThaumcraftApiHelper.addScans(b.player, null) == 0, "empty and null add nothing");

            // An advanced marker replaces a basic one; a basic one never replaces an advanced one.
            kb.reset();
            kb.scanItem("#minecraft:gold_ingot");
            kb.scanItem("@minecraft:dirt");
            b.capture();
            int upgrades = ThaumcraftApiHelper.addScans(b.player,
                    new Scans(set("@minecraft:gold_ingot", "#minecraft:dirt"), Set.of(), Set.of()));
            test.assertTrue(upgrades == 1, "only the advanced marker of the dirt is new, got " + upgrades);
            test.assertTrue(kb.getScannedItems().equals(Set.of("#minecraft:gold_ingot", "#minecraft:dirt")), "markers: " + kb.getScannedItems());
            test.assertTrue(b.syncs().size() == 1, "the upgrade was synced");

            // Quiet discovery of an aspect.
            test.assertTrue(!ThaumcraftApiHelper.hasDiscoveredAspect(b.player, Aspect.VOID), "a compound aspect is not known after a reset");
            b.capture();
            test.assertTrue(ThaumcraftApiHelper.discoverAspect(b.player, Aspect.VOID), "newly discovered");
            List<PlayerKnowledge> sent = b.syncs();
            test.assertTrue(sent.size() == 1, "one sync packet and no discovery notice, got " + sent.size());
            if (sent.get(0) != null) test.assertTrue(sent.get(0).hasDiscoveredAspect(Aspect.VOID.getTag()), "the sync packet carries the discovery");
            test.assertTrue(ThaumcraftApiHelper.hasDiscoveredAspect(b.player, Aspect.VOID), "discovered on the server");
            test.assertTrue(ThaumcraftApiHelper.getAspectPool(b.player, Aspect.VOID) == 0, "with an empty research pool");
            test.assertTrue(!ThaumcraftApiHelper.discoverAspect(b.player, Aspect.VOID), "a second discovery is no news");
            test.assertTrue(b.capture().isEmpty(), "and sends nothing");
            test.assertTrue(!ThaumcraftApiHelper.discoverAspect(b.player, null), "no aspect, nothing discovered");
            test.assertTrue(!ThaumcraftApiHelper.discoverAspect(null, Aspect.TRAP), "no player, nothing discovered");

            // Server thread only: the knowledge is not thread safe.
            expectRefused(() -> ThaumcraftApiHelper.discoverAspect(b.player, Aspect.TRAP), test, "discoverAspect");
            expectRefused(() -> ThaumcraftApiHelper.addScans(b.player, new Scans(set("@minecraft:emerald"), Set.of(), Set.of())), test, "addScans");
            test.assertTrue(!kb.hasDiscoveredAspect(Aspect.TRAP.getTag()) && !kb.hasScannedItem("@minecraft:emerald"), "the refused calls changed nothing");
        }
        test.succeed();
    }

    private static void expectRefused(Runnable call, GameTestHelper test, String what) {
        boolean refused = false;
        try {
            CompletableFuture.runAsync(call).join();
        } catch (CompletionException expected) {
            refused = expected.getCause() instanceof IllegalStateException;
        }
        test.assertTrue(refused, what + " on another thread is refused with an IllegalStateException");
    }

    // ========================================================================================= A13: bonus tags

    public static void bonusObjectTags(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        ItemStack diamond = new ItemStack(Items.DIAMOND, 3);
        AspectList base = ThaumcraftApiHelper.getObjectAspects(diamond, level);
        test.assertTrue(base.size() > 0, "the diamond has aspects");

        AspectList alone = ThaumcraftApiHelper.getBonusObjectTags(diamond, null, level);
        test.assertTrue(sameAmounts(base, alone), "no bonus: exactly the aspects of the item, per single item (not times 3): " + alone.aspects);

        AspectList bonus = new AspectList().add(Aspect.FIRE, 3).add(base.getAspects()[0], 1);
        AspectList bonusBefore = bonus.copy();
        AspectList sum = ThaumcraftApiHelper.getBonusObjectTags(diamond, bonus, level);
        AspectList everything = base.copy().add(bonus);
        for (Aspect aspect : everything.getAspects()) {
            test.assertTrue(sum.getAmount(aspect) == base.getAmount(aspect) + bonus.getAmount(aspect),
                    "the bonus is summed onto the aspects of the item: " + aspect.getTag());
        }
        test.assertTrue(sum.size() == everything.size(), "and adds no other aspect");
        test.assertTrue(sameAmounts(bonusBefore, bonus), "the bonus list is never modified");
        test.assertTrue(sum != bonus && sum != alone, "a new list");

        // The 1.7.10 habit of passing the base list doubles every amount: the base is already part of the answer.
        AspectList doubled = ThaumcraftApiHelper.getBonusObjectTags(diamond, base, level);
        for (Aspect aspect : base.getAspects()) {
            test.assertTrue(doubled.getAmount(aspect) == 2 * base.getAmount(aspect), "passing the base as the bonus counts " + aspect.getTag() + " twice");
        }

        // The contents of an essentia container are part of what a scan sees.
        var jarItem = ThaumcraftContent.JAR.get().asItem();
        ItemStack jar = ItemJar.withContents(new ItemStack(jarItem), Aspect.AIR, 32);
        AspectList withContents = ThaumcraftApiHelper.getBonusObjectTags(jar, null, level);
        test.assertTrue(withContents.getAmount(Aspect.AIR) >= 32, "a filled jar shows its essentia: " + withContents.aspects);
        test.assertTrue(sameAmounts(ThaumcraftApiHelper.getObjectAspects(jar, level), withContents), "and the scan readout of the stack agrees");

        // Without a stack or a world only the bonus is left.
        AspectList onlyBonus = ThaumcraftApiHelper.getBonusObjectTags(ItemStack.EMPTY, bonus, level);
        test.assertTrue(sameAmounts(bonus, onlyBonus) && onlyBonus != bonus, "an empty stack: a copy of the bonus");
        test.assertTrue(sameAmounts(bonus, ThaumcraftApiHelper.getBonusObjectTags(diamond, bonus, null)), "no level: a copy of the bonus");
        test.succeed();
    }

    private static boolean sameAmounts(AspectList first, AspectList second) {
        if (first.size() != second.size()) return false;
        for (Aspect aspect : first.getAspects()) if (first.getAmount(aspect) != second.getAmount(aspect)) return false;
        return true;
    }

    // ================================================================================== A6: logs and the walk

    public static void woodLogRule(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos at = test.absolutePos(new BlockPos(1, 2, 1));
        try {
            for (Block block : new Block[] {Blocks.BIRCH_LOG, Blocks.SPRUCE_LOG, Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG,
                    Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG, Blocks.STRIPPED_BIRCH_LOG, Blocks.BIRCH_WOOD, Blocks.CRIMSON_STEM,
                    ThaumcraftNaturals.GREATWOOD_LOG.get(), ThaumcraftNaturals.SILVERWOOD_LOG.get()}) {
                level.setBlock(at, block.defaultBlockState(), 3);
                test.assertTrue(ThaumcraftApiHelper.isWoodLog(level, at), block + " is in #minecraft:logs");
            }
            for (Block block : new Block[] {Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES}) {
                level.setBlock(at, block.defaultBlockState(), 3);
                test.assertTrue(ThaumcraftApiHelper.isWoodLog(level, at), block + " is a leaf block, which the port rule counts");
            }
            // PINS THE CURRENT PORT RULE, not a verified original (see KNOWN_DEVIATIONS section 9): the plain oak log is no log.
            level.setBlock(at, Blocks.OAK_LOG.defaultBlockState(), 3);
            test.assertTrue(!ThaumcraftApiHelper.isWoodLog(level, at), "the plain oak log is not recognised by the port rule");
            for (Block block : new Block[] {Blocks.AIR, Blocks.STONE, Blocks.DIRT, Blocks.OAK_PLANKS, Blocks.BIRCH_PLANKS, Blocks.BEDROCK, Blocks.CRAFTING_TABLE}) {
                level.setBlock(at, block.defaultBlockState(), 3);
                test.assertTrue(!ThaumcraftApiHelper.isWoodLog(level, at), block + " is no log");
            }
            BlockPos far = at.offset(1_000_000, 0, 1_000_000);
            test.assertTrue(!level.hasChunkAt(far), "fixture: the far position is in no loaded chunk");
            test.assertTrue(!ThaumcraftApiHelper.isWoodLog(level, far), "an unloaded position is no log, and the call loads nothing");
            test.assertTrue(!level.hasChunkAt(far), "still not loaded");
            test.assertTrue(!ThaumcraftApiHelper.isWoodLog(null, at) && !ThaumcraftApiHelper.isWoodLog(level, null), "missing arguments");
        } finally {
            level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        }
        test.succeed();
    }

    /** Fells a birch trunk from its foot with a survival player: one block per call, the top first, loot, no wear, ticks. */
    public static void furthestBlockFellsATrunkOneLogPerCall(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos foot = test.absolutePos(new BlockPos(3, 1, 3));
        BlockPos top = foot.above(4);
        BlockPos marker = top.offset(3, -3, 3);
        BlockPos outside = top.offset(4, 0, 0);
        try (Connected survivor = new Connected(test, GameType.SURVIVAL)) {
            for (int y = 0; y < 5; y++) level.setBlock(foot.above(y), Blocks.BIRCH_LOG.defaultBlockState(), 3);
            level.setBlock(marker, Blocks.OBSIDIAN.defaultBlockState(), 3);
            level.setBlock(outside, Blocks.OBSIDIAN.defaultBlockState(), 3);
            ItemStack axe = new ItemStack(Items.IRON_AXE);
            survivor.player.setItemInHand(InteractionHand.MAIN_HAND, axe);
            float exhaustion = survivor.player.getFoodData().getExhaustionLevel();
            test.assertTrue(!level.getBlockTicks().hasScheduledTick(marker, Blocks.OBSIDIAN), "fixture: no tick scheduled yet");

            boolean first = ThaumcraftApiHelper.breakFurthestBlock(level, foot, Blocks.BIRCH_LOG, survivor.player);
            test.assertTrue(first, "the walk ended on a breakable log");
            test.assertTrue(level.getBlockState(top).isAir(), "the end of the trunk was broken");
            for (int y = 0; y < 4; y++) test.assertTrue(level.getBlockState(foot.above(y)).is(Blocks.BIRCH_LOG), "the log " + y + " on the way stays");
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(top).inflate(2.0));
            test.assertTrue(items.size() == 1 && items.get(0).getItem().is(Items.BIRCH_LOG) && items.get(0).getItem().getCount() == 1,
                    "one birch log dropped at the broken cell: " + items);
            test.assertTrue(items.get(0).blockPosition().distManhattan(top) <= 2, "where it was broken, not at the player");
            test.assertTrue(axe.getDamageValue() == 0 && survivor.player.getMainHandItem() == axe, "the tool takes no damage");
            test.assertTrue(survivor.player.getFoodData().getExhaustionLevel() == exhaustion, "and the player no exhaustion");
            test.assertTrue(level.getBlockTicks().hasScheduledTick(marker, Blocks.OBSIDIAN),
                    "a cell three blocks from the broken one got a scheduled tick");
            test.assertTrue(!level.getBlockTicks().hasScheduledTick(outside, Blocks.OBSIDIAN), "a cell four blocks away did not");

            // Felling the whole trunk is a loop of the caller; it ends by itself because the start cell becomes air.
            int calls = 1;
            while (calls < 20 && ThaumcraftApiHelper.breakFurthestBlock(level, foot, Blocks.BIRCH_LOG, survivor.player)) calls++;
            test.assertTrue(calls == 5, "five calls fell five logs (the last one is the start cell itself), got " + calls);
            for (int y = 0; y < 5; y++) test.assertTrue(level.getBlockState(foot.above(y)).isAir(), "the trunk is gone at " + y);
            test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, foot, Blocks.BIRCH_LOG, survivor.player), "an air start answers false");
            int logs = 0;
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(foot).inflate(6.0, 8.0, 6.0))) {
                if (item.getItem().is(Items.BIRCH_LOG)) logs += item.getItem().getCount();
            }
            test.assertTrue(logs >= 5, "all five logs dropped as items, got " + logs);
        } finally {
            level.setBlock(marker, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(outside, Blocks.AIR.defaultBlockState(), 3);
            for (int y = 0; y < 5; y++) level.setBlock(foot.above(y), Blocks.AIR.defaultBlockState(), 3);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(foot).inflate(8.0, 10.0, 8.0))) item.discard();
        }
        test.succeed();
    }

    /** "Furthest" is the first local maximum of a greedy walk in scan order, not the farthest log of the structure. */
    public static void furthestBlockIsTheFirstLocalMaximum(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos start = test.absolutePos(new BlockPos(3, 2, 3));
        try (Connected player = new Connected(test, GameType.SURVIVAL)) {
            // A fallen trunk along x with the start in the middle; a spruce log beyond its west end.
            for (int dx = -1; dx <= 3; dx++) level.setBlock(start.offset(dx, 0, 0), Blocks.JUNGLE_LOG.defaultBlockState(), 3);
            level.setBlock(start.offset(-2, 0, 0), Blocks.ACACIA_LOG.defaultBlockState(), 3);

            test.assertTrue(ThaumcraftApiHelper.breakFurthestBlock(level, start, Blocks.JUNGLE_LOG, player.player), "a log was broken");
            test.assertTrue(level.getBlockState(start.offset(-1, 0, 0)).isAir(),
                    "the walk stepped to the first qualifying cell in scan order (x = -1) and found nothing further from there");
            test.assertTrue(level.getBlockState(start.offset(3, 0, 0)).is(Blocks.JUNGLE_LOG), "the farthest log of the trunk (3 blocks east) is untouched");
            test.assertTrue(level.getBlockState(start).is(Blocks.JUNGLE_LOG) && level.getBlockState(start.offset(1, 0, 0)).is(Blocks.JUNGLE_LOG), "so are the others");
            test.assertTrue(level.getBlockState(start.offset(-2, 0, 0)).is(Blocks.ACACIA_LOG), "another wood is not followed: the block must be the very same");
        } finally {
            for (int dx = -2; dx <= 3; dx++) level.setBlock(start.offset(dx, 0, 0), Blocks.AIR.defaultBlockState(), 3);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(start).inflate(8.0))) item.discard();
        }
        test.succeed();
    }

    /** Gaps of one block and diagonals are crossed: the box around the current cell is 5 by 5 by 5. */
    public static void furthestBlockCrossesGapsAndDiagonals(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos start = test.absolutePos(new BlockPos(1, 1, 1));
        BlockPos middle = start.offset(2, 2, 2);
        BlockPos end = start.offset(4, 4, 4);
        try (Connected player = new Connected(test, GameType.SURVIVAL)) {
            for (BlockPos pos : List.of(start, middle, end)) level.setBlock(pos, Blocks.SPRUCE_LOG.defaultBlockState(), 3);
            test.assertTrue(ThaumcraftApiHelper.breakFurthestBlock(level, start, Blocks.SPRUCE_LOG, player.player), "a log was broken");
            test.assertTrue(level.getBlockState(end).isAir(), "two diagonal jumps over a gap reached the last log");
            test.assertTrue(level.getBlockState(start).is(Blocks.SPRUCE_LOG) && level.getBlockState(middle).is(Blocks.SPRUCE_LOG), "the ones on the way stay");
        } finally {
            for (BlockPos pos : List.of(start, middle, end)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(start).inflate(8.0))) item.discard();
        }
        test.succeed();
    }

    /** The walk never leaves 48 blocks above or below the start; the log just beyond is left standing. */
    public static void furthestBlockStaysInsideTheOriginalBox(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos start = test.absolutePos(new BlockPos(3, 1, 3));
        int height = 60;
        try (Connected player = new Connected(test, GameType.SURVIVAL)) {
            for (int dy = 0; dy < height; dy++) level.setBlock(start.above(dy), Blocks.DARK_OAK_LOG.defaultBlockState(), 3);
            test.assertTrue(ThaumcraftApiHelper.breakFurthestBlock(level, start, Blocks.DARK_OAK_LOG, player.player), "a log was broken");
            test.assertTrue(level.getBlockState(start.above(48)).isAir(), "the log 48 blocks above the start was the end of the walk");
            test.assertTrue(level.getBlockState(start.above(47)).is(Blocks.DARK_OAK_LOG) && level.getBlockState(start.above(49)).is(Blocks.DARK_OAK_LOG),
                    "its neighbours, and everything beyond the box, stay");
            test.assertTrue(level.getBlockState(start).is(Blocks.DARK_OAK_LOG), "the start stays");
        } finally {
            for (int dy = 0; dy < height; dy++) level.setBlock(start.above(dy), Blocks.AIR.defaultBlockState(), 3);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(start).inflate(8.0, 70.0, 8.0))) item.discard();
        }
        test.succeed();
    }

    /** What the helper does when the start is no log, and who gets drops. */
    public static void furthestBlockStartAndPlayerRules(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos dirt = test.absolutePos(new BlockPos(1, 2, 1));
        BlockPos bedrock = test.absolutePos(new BlockPos(3, 2, 1));
        BlockPos air = test.absolutePos(new BlockPos(5, 2, 1));
        BlockPos creativeLog = test.absolutePos(new BlockPos(1, 2, 5));
        BlockPos nobodyLog = test.absolutePos(new BlockPos(3, 2, 5));
        BlockPos offThreadLog = test.absolutePos(new BlockPos(5, 2, 5));
        AABB area = new AABB(test.absolutePos(new BlockPos(0, 0, 0))).inflate(0, 0, 0).expandTowards(8, 8, 8);
        try (Connected survivor = new Connected(test, GameType.SURVIVAL); Connected creative = new Connected(test, GameType.CREATIVE)) {
            // The start is never checked: with no further log the helper breaks the start, whatever it is.
            level.setBlock(dirt, Blocks.DIRT.defaultBlockState(), 3);
            test.assertTrue(ThaumcraftApiHelper.breakFurthestBlock(level, dirt, Blocks.DIRT, survivor.player), "a dirt start is broken");
            test.assertTrue(level.getBlockState(dirt).isAir(), "the dirt is gone: call isWoodLog first, as the original axe did");
            test.assertTrue(level.getEntitiesOfClass(ItemEntity.class, area).stream().anyMatch(item -> item.getItem().is(Items.DIRT)), "it dropped its loot to a survival player");
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) item.discard();

            level.setBlock(bedrock, Blocks.BEDROCK.defaultBlockState(), 3);
            test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, bedrock, Blocks.BEDROCK, survivor.player), "an unbreakable start answers false");
            test.assertTrue(level.getBlockState(bedrock).is(Blocks.BEDROCK), "and stays");

            test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, air, Blocks.MANGROVE_LOG, survivor.player), "an air start answers false");
            test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, dirt, null, survivor.player), "no block, nothing to follow");
            test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, null, Blocks.MANGROVE_LOG, survivor.player), "no position");

            // A creative player and no player get no drops; the log is broken all the same.
            level.setBlock(creativeLog, Blocks.MANGROVE_LOG.defaultBlockState(), 3);
            test.assertTrue(ThaumcraftApiHelper.breakFurthestBlock(level, creativeLog, Blocks.MANGROVE_LOG, creative.player), "the creative player's log is broken");
            level.setBlock(nobodyLog, Blocks.MANGROVE_LOG.defaultBlockState(), 3);
            test.assertTrue(ThaumcraftApiHelper.breakFurthestBlock(level, nobodyLog, Blocks.MANGROVE_LOG, null), "so is the log of no player");
            test.assertTrue(level.getBlockState(creativeLog).isAir() && level.getBlockState(nobodyLog).isAir(), "both are gone");
            test.assertTrue(level.getEntitiesOfClass(ItemEntity.class, area).isEmpty(), "and neither dropped anything");

            // Server thread only: the walk keeps static state.
            level.setBlock(offThreadLog, Blocks.MANGROVE_LOG.defaultBlockState(), 3);
            expectRefused(() -> ThaumcraftApiHelper.breakFurthestBlock(level, offThreadLog, Blocks.MANGROVE_LOG, null), test, "breakFurthestBlock");
            test.assertTrue(level.getBlockState(offThreadLog).is(Blocks.MANGROVE_LOG), "the refused call broke nothing");
        } finally {
            for (BlockPos pos : List.of(dirt, bedrock, air, creativeLog, nobodyLog, offThreadLog)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) item.discard();
        }
        test.succeed();
    }
}
