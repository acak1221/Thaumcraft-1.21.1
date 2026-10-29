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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApi;
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
import thaumcraft.common.lib.utils.BlockUtils;

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
            // An aspect that is not registered under its own tag (an add-on retagged it) cannot be written to the knowledge.
            String trapTag = Aspect.TRAP.getTag();
            Aspect.TRAP.setTag("kn_retagged_trap");
            try {
                test.assertTrue(!ThaumcraftApiHelper.discoverAspect(b.player, Aspect.TRAP), "a retagged aspect is not discovered");
                test.assertTrue(b.capture().isEmpty() && !kb.hasDiscoveredAspect("kn_retagged_trap") && !kb.hasDiscoveredAspect(trapTag),
                        "and nothing was written to the knowledge or sent");
            } finally {
                Aspect.TRAP.setTag(trapTag);
            }

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

        // A group of damage values registered together resolves as a scan resolves it: the facade and the readout agree.
        Item pole = Items.WARPED_FUNGUS_ON_A_STICK;
        ThaumcraftApi.registerObjectTag(new ItemStack(pole), new int[] {0, 1, 2}, new AspectList().add(Aspect.MOTION, 3).add(Aspect.PLANT, 2));
        try {
            ItemStack damaged = new ItemStack(pole);
            damaged.setDamageValue(2);
            AspectList scan = ThaumcraftApiHelper.getObjectAspects(damaged, level);
            test.assertTrue(scan.getAmount(Aspect.MOTION) >= 3 && scan.getAmount(Aspect.PLANT) >= 2,
                    "fixture: the scan resolves damage 2 through the group registered at damage 0: " + scan.aspects);
            test.assertTrue(sameAmounts(scan, ThaumcraftApiHelper.getBonusObjectTags(damaged, null, level)),
                    "the bonus facade resolves the group the way the scan does");
        } finally {
            ThaumcraftApi.objectTags.remove(List.of(pole, 0));
            for (int damage = 0; damage <= 2; damage++) ThaumcraftApi.groupedObjectTags.remove(List.of(pole, damage));
        }

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
            // The rule is membership of #minecraft:logs, which the plain oak log is part of (original Utils.isWoodLog: every block that
            // can sustain leaves, and a log does).
            for (Block block : new Block[] {Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.SPRUCE_LOG, Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG,
                    Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG, Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_BIRCH_LOG, Blocks.OAK_WOOD, Blocks.BIRCH_WOOD,
                    Blocks.CRIMSON_STEM, ThaumcraftNaturals.GREATWOOD_LOG.get(), ThaumcraftNaturals.SILVERWOOD_LOG.get()}) {
                level.setBlock(at, block.defaultBlockState(), 3);
                test.assertTrue(ThaumcraftApiHelper.isWoodLog(level, at), block + " is in #minecraft:logs");
            }
            // A leaf block sustains no leaves and is no log, so a click on a hedge is no tree.
            for (Block block : new Block[] {Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.AZALEA_LEAVES}) {
                level.setBlock(at, block.defaultBlockState(), 3);
                test.assertTrue(!ThaumcraftApiHelper.isWoodLog(level, at), block + " is a leaf block, which is no log");
            }
            for (Block block : new Block[] {Blocks.AIR, Blocks.STONE, Blocks.DIRT, Blocks.OAK_PLANKS, Blocks.BIRCH_PLANKS, Blocks.BEDROCK, Blocks.CRAFTING_TABLE,
                    Blocks.GOLD_BLOCK, Blocks.CHEST}) {
                level.setBlock(at, block.defaultBlockState(), 3);
                test.assertTrue(!ThaumcraftApiHelper.isWoodLog(level, at), block + " is no log");
            }
            BlockPos far = at.offset(1_000_000, 0, 1_000_000);
            test.assertTrue(!level.hasChunkAt(far), "fixture: the far position is in no loaded chunk");
            test.assertTrue(!ThaumcraftApiHelper.isWoodLog(level, far), "an unloaded position is no log, and the call loads nothing");
            test.assertTrue(!level.hasChunkAt(far), "still not loaded");
            test.assertTrue(!ThaumcraftApiHelper.isWoodLog(level, new BlockPos(at.getX(), level.getMaxBuildHeight() + 3, at.getZ())), "above the build height is no log");
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
        BlockPos leaves = test.absolutePos(new BlockPos(1, 2, 3));
        BlockPos oak = test.absolutePos(new BlockPos(3, 2, 3));
        BlockPos gold = test.absolutePos(new BlockPos(5, 2, 3));
        BlockPos chest = test.absolutePos(new BlockPos(7, 2, 3));
        AABB area = new AABB(test.absolutePos(new BlockPos(0, 0, 0))).inflate(0, 0, 0).expandTowards(8, 8, 8);
        try (Connected survivor = new Connected(test, GameType.SURVIVAL); Connected creative = new Connected(test, GameType.CREATIVE)) {
            // The start must itself be a log of the block: a position forwarded by a client cannot mine anything else.
            level.setBlock(dirt, Blocks.DIRT.defaultBlockState(), 3);
            test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, dirt, Blocks.DIRT, survivor.player), "a dirt start is refused");
            test.assertTrue(level.getBlockState(dirt).is(Blocks.DIRT), "the dirt stays");
            test.assertTrue(level.getEntitiesOfClass(ItemEntity.class, area).isEmpty(), "and nothing dropped");
            level.setBlock(leaves, Blocks.OAK_LEAVES.defaultBlockState(), 3);
            test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, leaves, Blocks.OAK_LEAVES, survivor.player), "a leaf start is refused: leaves are no logs");
            test.assertTrue(level.getBlockState(leaves).is(Blocks.OAK_LEAVES), "the leaves stay");
            level.setBlock(gold, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
            for (Player who : new Player[] {survivor.player, creative.player, null}) {
                test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, gold, Blocks.GOLD_BLOCK, who), "a gold block start is refused for " + who);
                test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, chest, Blocks.CHEST, who), "a chest start is refused for " + who);
            }
            test.assertTrue(level.getBlockState(gold).is(Blocks.GOLD_BLOCK) && level.getBlockState(chest).is(Blocks.CHEST), "both stay");
            level.setBlock(bedrock, Blocks.BEDROCK.defaultBlockState(), 3);
            test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, bedrock, Blocks.BEDROCK, survivor.player), "bedrock is no log either");
            test.assertTrue(level.getBlockState(bedrock).is(Blocks.BEDROCK), "and stays");

            // A start that holds another block than the one asked for is no start.
            level.setBlock(oak, Blocks.OAK_LOG.defaultBlockState(), 3);
            test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, oak, Blocks.BIRCH_LOG, survivor.player), "the block asked for must be the block at the start");
            test.assertTrue(level.getBlockState(oak).is(Blocks.OAK_LOG), "so the oak log stays");
            // The plain oak log is a log: a lone one is its own end and is broken, with its loot.
            test.assertTrue(ThaumcraftApiHelper.breakFurthestBlock(level, oak, Blocks.OAK_LOG, survivor.player), "a lone oak log is felled");
            test.assertTrue(level.getBlockState(oak).isAir(), "it is gone");
            test.assertTrue(level.getEntitiesOfClass(ItemEntity.class, area).stream().anyMatch(item -> item.getItem().is(Items.OAK_LOG)), "and dropped its loot to a survival player");
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) item.discard();

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

            // The block helper behind it takes a null player for a block the game announces to the player (a block that
            // piglins guard dereferenced the player) and breaks the start, whatever it is: it is the original form.
            test.assertTrue(BlockUtils.breakFurthestBlock(level, gold.getX(), gold.getY(), gold.getZ(), Blocks.GOLD_BLOCK, null), "the block helper breaks a guarded block for no player");
            test.assertTrue(level.getBlockState(gold).isAir(), "the gold block is gone");
            test.assertTrue(BlockUtils.breakFurthestBlock(level, chest.getX(), chest.getY(), chest.getZ(), Blocks.CHEST, null), "and a chest");
            test.assertTrue(level.getBlockState(chest).isAir(), "the chest is gone");

            // Server thread only: it breaks blocks and spawns items.
            level.setBlock(offThreadLog, Blocks.MANGROVE_LOG.defaultBlockState(), 3);
            expectRefused(() -> ThaumcraftApiHelper.breakFurthestBlock(level, offThreadLog, Blocks.MANGROVE_LOG, null), test, "breakFurthestBlock");
            test.assertTrue(level.getBlockState(offThreadLog).is(Blocks.MANGROVE_LOG), "the refused call broke nothing");
        } finally {
            for (BlockPos pos : List.of(dirt, bedrock, air, creativeLog, nobodyLog, offThreadLog, leaves, oak, gold, chest)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) item.discard();
        }
        test.succeed();
    }

    /** findFurthestBlock answers the cell the next break takes, reads only, and says null where nothing would be broken. */
    public static void furthestBlockFindAnswersTheCellThatBreaks(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos foot = test.absolutePos(new BlockPos(2, 1, 2));
        BlockPos top = foot.above(3);
        BlockPos dirt = test.absolutePos(new BlockPos(5, 2, 5));
        BlockPos leaves = test.absolutePos(new BlockPos(6, 2, 5));
        BlockPos chest = test.absolutePos(new BlockPos(7, 2, 5));
        BlockPos bedrock = test.absolutePos(new BlockPos(5, 2, 7));
        AABB area = new AABB(foot).inflate(8.0, 8.0, 8.0);
        try (Connected survivor = new Connected(test, GameType.SURVIVAL)) {
            // A plain OAK trunk: the plain oak log is a log, and the walk follows it to the top.
            for (int y = 0; y < 4; y++) level.setBlock(foot.above(y), Blocks.OAK_LOG.defaultBlockState(), 3);
            BlockPos found = ThaumcraftApiHelper.findFurthestBlock(level, foot, Blocks.OAK_LOG);
            test.assertTrue(top.equals(found), "the end of the oak trunk: " + found);
            test.assertTrue(top.equals(ThaumcraftApiHelper.findFurthestBlock(level, foot, Blocks.OAK_LOG)), "asking twice gives the same cell");
            for (int y = 0; y < 4; y++) test.assertTrue(level.getBlockState(foot.above(y)).is(Blocks.OAK_LOG), "nothing was broken by asking, log " + y);
            test.assertTrue(level.getEntitiesOfClass(ItemEntity.class, area).isEmpty() && !level.getBlockTicks().hasScheduledTick(top, Blocks.OAK_LOG),
                    "no loot, no scheduled tick");
            test.assertTrue(top.equals(ThaumcraftApiHelper.findFurthestBlock(level, top.below(), Blocks.OAK_LOG)), "from the log below the top the end is the top as well");
            // "Furthest" is measured from the clicked log: the same trunk clicked at its top ends at the foot, and a log with no
            // other log within reach is its own end.
            test.assertTrue(foot.equals(ThaumcraftApiHelper.findFurthestBlock(level, top, Blocks.OAK_LOG)), "clicked at the top, the walk runs down to the foot");
            BlockPos lone = test.absolutePos(new BlockPos(1, 2, 7));
            level.setBlock(lone, Blocks.OAK_LOG.defaultBlockState(), 3);
            try {
                test.assertTrue(lone.equals(ThaumcraftApiHelper.findFurthestBlock(level, lone, Blocks.OAK_LOG)), "a log with no other log within reach is its own end");
            } finally {
                level.setBlock(lone, Blocks.AIR.defaultBlockState(), 3);
            }

            // It is the cell that the break takes.
            test.assertTrue(ThaumcraftApiHelper.breakFurthestBlock(level, foot, Blocks.OAK_LOG, survivor.player), "the break takes the oak trunk's end");
            test.assertTrue(level.getBlockState(found).isAir(), "exactly the cell find answered");
            test.assertTrue(level.getBlockState(found.below()).is(Blocks.OAK_LOG), "and not the log below");
            BlockPos next = ThaumcraftApiHelper.findFurthestBlock(level, foot, Blocks.OAK_LOG);
            test.assertTrue(foot.above(2).equals(next), "the next end is one log lower: " + next);

            // Null where nothing would be broken.
            level.setBlock(dirt, Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(leaves, Blocks.OAK_LEAVES.defaultBlockState(), 3);
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
            level.setBlock(bedrock, Blocks.BEDROCK.defaultBlockState(), 3);
            test.assertTrue(ThaumcraftApiHelper.findFurthestBlock(level, foot, Blocks.BIRCH_LOG) == null, "the block asked for is not the block at the start");
            test.assertTrue(ThaumcraftApiHelper.findFurthestBlock(level, dirt, Blocks.DIRT) == null, "a dirt start");
            test.assertTrue(ThaumcraftApiHelper.findFurthestBlock(level, leaves, Blocks.OAK_LEAVES) == null, "a leaf start: leaves are no logs");
            test.assertTrue(ThaumcraftApiHelper.findFurthestBlock(level, chest, Blocks.CHEST) == null, "a chest start");
            test.assertTrue(ThaumcraftApiHelper.findFurthestBlock(level, bedrock, Blocks.BEDROCK) == null, "a bedrock start");
            test.assertTrue(ThaumcraftApiHelper.findFurthestBlock(level, top.above(10), Blocks.OAK_LOG) == null, "an air start");
            test.assertTrue(ThaumcraftApiHelper.findFurthestBlock(level, new BlockPos(foot.getX(), level.getMaxBuildHeight() + 3, foot.getZ()), Blocks.OAK_LOG) == null,
                    "a start above the build height");
            BlockPos far = foot.offset(1_000_000, 0, 1_000_000);
            test.assertTrue(!level.hasChunkAt(far), "fixture: the far position is in no loaded chunk");
            test.assertTrue(ThaumcraftApiHelper.findFurthestBlock(level, far, Blocks.OAK_LOG) == null && !level.hasChunkAt(far), "an unloaded start, and nothing was loaded");
            test.assertTrue(ThaumcraftApiHelper.findFurthestBlock(null, foot, Blocks.OAK_LOG) == null
                    && ThaumcraftApiHelper.findFurthestBlock(level, null, Blocks.OAK_LOG) == null
                    && ThaumcraftApiHelper.findFurthestBlock(level, foot, null) == null, "missing arguments");
        } finally {
            for (BlockPos pos : List.of(dirt, leaves, chest, bedrock)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            for (int y = 0; y < 4; y++) level.setBlock(foot.above(y), Blocks.AIR.defaultBlockState(), 3);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) item.discard();
        }
        test.succeed();
    }

    /** The game's own rules are asked for the far cell that is broken, not for the clicked one. */
    public static void furthestBlockAsksTheRulesForTheBrokenCell(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos start = test.absolutePos(new BlockPos(1, 2, 1));
        BlockPos end = start.east(3);
        AABB area = new AABB(start).inflate(8.0, 8.0, 8.0);
        WorldBorder border = level.getWorldBorder();
        double centerX = border.getCenterX(), centerZ = border.getCenterZ(), size = border.getSize();
        try (Connected survivor = new Connected(test, GameType.SURVIVAL); Connected adventurer = new Connected(test, GameType.ADVENTURE);
             Connected spectator = new Connected(test, GameType.SPECTATOR)) {
            for (int dx = 0; dx <= 3; dx++) level.setBlock(start.east(dx), Blocks.BIRCH_LOG.defaultBlockState(), 3);
            test.assertTrue(end.equals(ThaumcraftApiHelper.findFurthestBlock(level, start, Blocks.BIRCH_LOG)), "fixture: the walk ends three blocks east");

            // Game mode: an adventure player and a spectator break nothing; the clicked cell being allowed does not matter.
            test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, start, Blocks.BIRCH_LOG, adventurer.player), "an adventure player breaks nothing");
            test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, start, Blocks.BIRCH_LOG, spectator.player), "a spectator breaks nothing");
            for (int dx = 0; dx <= 3; dx++) test.assertTrue(level.getBlockState(start.east(dx)).is(Blocks.BIRCH_LOG), "the log " + dx + " still stands");
            test.assertTrue(level.getEntitiesOfClass(ItemEntity.class, area).isEmpty(), "and nothing dropped");

            // World border: the clicked cell and its neighbour are inside, the broken cell is not. The border is put back afterwards.
            border.setCenter(start.getX() + 0.5, start.getZ() + 0.5);
            border.setSize(3.0);
            test.assertTrue(level.getWorldBorder().isWithinBounds(start) && !level.getWorldBorder().isWithinBounds(end), "fixture: the clicked cell is inside the border, the end is not");
            test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, start, Blocks.BIRCH_LOG, survivor.player), "a survival player cannot break a cell outside the border");
            test.assertTrue(!ThaumcraftApiHelper.breakFurthestBlock(level, start, Blocks.BIRCH_LOG, null), "nor can nobody: the border holds for every caller");
            test.assertTrue(level.getBlockState(end).is(Blocks.BIRCH_LOG) && level.getBlockState(start).is(Blocks.BIRCH_LOG), "nothing changed");
            test.assertTrue(end.equals(ThaumcraftApiHelper.findFurthestBlock(level, start, Blocks.BIRCH_LOG)),
                    "findFurthestBlock reports the cell all the same: asking the protection is the caller's step");
            border.setSize(9.0);
            test.assertTrue(level.getWorldBorder().isWithinBounds(end), "fixture: a wider border contains the end");
            test.assertTrue(ThaumcraftApiHelper.breakFurthestBlock(level, start, Blocks.BIRCH_LOG, survivor.player), "inside the border the survival player breaks the end");
            test.assertTrue(level.getBlockState(end).isAir(), "the end is gone");
        } finally {
            border.setCenter(centerX, centerZ);
            border.setSize(size);
            for (int dx = 0; dx <= 3; dx++) level.setBlock(start.east(dx), Blocks.AIR.defaultBlockState(), 3);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) item.discard();
        }
        test.succeed();
    }

    /**
     * The walk and the block ticks around the broken cell read no chunk that is not loaded, and load none. The logs stand two
     * and three blocks inside the last loaded chunk: the walk from the end scans two cells beyond the edge and the ticks reach
     * three, while the ordinary block updates of the removal (direct neighbours only) stay inside the loaded chunk.
     */
    public static void furthestBlockLoadsNoChunk(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos origin = test.absolutePos(new BlockPos(0, 1, 0));
        // The last loaded chunk east of the test, and the cell at its east edge: the next cell is in a chunk that is not loaded.
        int chunkX = origin.getX() >> 4;
        for (int step = 0; step < 64 && level.hasChunkAt(new BlockPos(((chunkX + 1) << 4), origin.getY(), origin.getZ())); step++) chunkX++;
        BlockPos edge = new BlockPos((chunkX << 4) + 15, origin.getY(), origin.getZ());
        BlockPos beyond = edge.east();
        test.assertTrue(level.hasChunkAt(edge) && !level.hasChunkAt(beyond) && !level.hasChunkAt(beyond.east(2)),
                "fixture: the cells east of " + edge + " are in no loaded chunk");
        BlockPos start = edge.west(3);
        BlockPos end = edge.west();
        try {
            for (int dx = 3; dx >= 1; dx--) level.setBlock(edge.west(dx), Blocks.SPRUCE_LOG.defaultBlockState(), 3);
            test.assertTrue(!level.hasChunkAt(beyond), "fixture: placing the logs loaded nothing");
            test.assertTrue(end.equals(ThaumcraftApiHelper.findFurthestBlock(level, start, Blocks.SPRUCE_LOG)), "the walk ends at the log next to the edge");
            test.assertTrue(!level.hasChunkAt(beyond), "finding the end loaded no chunk");
            test.assertTrue(ThaumcraftApiHelper.breakFurthestBlock(level, start, Blocks.SPRUCE_LOG, null), "that log is broken");
            test.assertTrue(level.getBlockState(end).isAir() && level.getBlockState(start).is(Blocks.SPRUCE_LOG), "exactly that log");
            test.assertTrue(!level.hasChunkAt(beyond), "breaking it, and the block ticks around it, loaded no chunk either");
        } finally {
            for (int dx = 3; dx >= 1; dx--) level.setBlock(edge.west(dx), Blocks.AIR.defaultBlockState(), 3);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(edge).inflate(8.0))) item.discard();
        }
        test.succeed();
    }
}
