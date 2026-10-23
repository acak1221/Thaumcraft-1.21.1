package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.relics.ItemThaumometer;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ScanManager;
import thaumcraft.common.tiles.TileNode;

/** Real connected survival actor, held tool, physical targets and twenty world ticks.
 * Fixtures declare knowledge prerequisites, not survival acquisition. Packet counts are server
 * emission evidence, not inspected client toast/rune rendering or a socket relogin. */
public final class ScanKnowledgeRuntimeChecks {
    private ScanKnowledgeRuntimeChecks() {}

    public static void containerBonusIsNotCappedAfterBase(GameTestHelper test) {
        var f = new Fixture(test);
        // Controlled public-addon base registration: original getObjectTags caps the BASE,
        // getBonusTags adds actual contained essentia, then cullTags selects six without a cap.
        var item = ThaumcraftContent.JAR.get().asItem();
        thaumcraft.common.lib.research.ObjectAspectCatalog.initializePublicTags();
        var oldTags = new java.util.HashMap<>(ThaumcraftApi.objectTags);
        try {
            ThaumcraftApi.registerObjectTag(new ItemStack(item), new AspectList().add(Aspect.AIR, 4));
            var jar = ItemJar.withContents(new ItemStack(item), Aspect.AIR, 32);
            f.drop(jar); f.begin();
            test.runAfterDelay(10, () -> { if (!f.closed) f.drop.setItem(ItemJar.withContents(jar, Aspect.AIR, 64)); });
            f.afterScan(() -> {
                f.check(f.knowledge.hasScannedItem("@thaumcraft:jar"), "real full-jar scan records the actor's item identity");
                f.exactPools(Map.of("aer", 68));
                f.check(ItemJar.getAmount(f.drop.getItem()) == 64, "scanning never consumes contained essentia");
                f.exactReadout(Map.of("aer", 68));
            }, () -> { ThaumcraftApi.objectTags.clear(); ThaumcraftApi.objectTags.putAll(oldTags); });
        } catch (RuntimeException | Error error) {
            ThaumcraftApi.objectTags.clear(); ThaumcraftApi.objectTags.putAll(oldTags); f.close(); throw error;
        }
    }

    public static void missingParentsBlockThenDiscoverOnce(GameTestHelper test) {
        var f = new Fixture(test);
        try {
            f.knowledge.reset(); f.knowledge.setInitializedAspects(true);
            var before = f.knowledge.save(); f.drop(new ItemStack(Items.DIAMOND)); f.begin();
            f.afterScan(() -> {
                f.check(f.knowledge.save().equals(before), "missing greed parents reject the entire real diamond scan without partial crystal discovery or RP");
                f.check(f.receipts.stream().noneMatch(id -> id.getPath().equals("aspect_discovery") || id.getPath().equals("aspect_pool")), "rejected scan emits neither discovery nor pool rewards");
                f.knowledge.addDiscoveredAspect(Aspect.MAN.getTag()); f.knowledge.addDiscoveredAspect(Aspect.HUNGER.getTag());
                f.receipts.clear(); f.capture(); f.begin();
                f.afterScan(() -> {
                    f.exactPools(Map.of("vitreus", 6, "lucrum", 6));
                    f.check(f.knowledge.hasScannedItem("@minecraft:diamond") && f.knowledge.hasDiscoveredAspect("vitreus") && f.knowledge.hasDiscoveredAspect("lucrum"), "retry discovers both aspects with exact original plus-two bonus");
                    f.check(f.receipts.stream().filter(id -> id.getPath().equals("aspect_discovery")).count() == 2 && f.receipts.stream().filter(id -> id.getPath().equals("aspect_pool")).count() == 2, "one discovery packet and one RP packet per newly discovered aspect");
                    f.duplicate(); f.close(); test.succeed();
                }, null);
            }, null);
        } catch (RuntimeException | Error error) { f.close(); throw error; }
    }

    public static void advancedMarkersSuppressThaumometer(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            // Saved advanced-scan knowledge is an explicit input, not a claim that this
            // thaumometer can itself produce # scans. Original @ admission checks # first.
            f.drop(new ItemStack(Items.DIAMOND)); f.knowledge.scanItem("#minecraft:diamond"); f.duplicate();
            f.clearTarget(); f.resetKnowledge();
            f.level.setBlock(f.target, Blocks.STONE.defaultBlockState(), 3); f.aim(Vec3.atCenterOf(f.target));
            f.knowledge.scanItem("#minecraft:stone"); f.duplicate();
            f.clearTarget(); f.resetKnowledge();
            var mob = EntityType.ENDERMAN.create(f.level); f.check(mob != null, "real advanced-marker entity target exists");
            mob.setNoAi(true); mob.setNoGravity(true); mob.setPos(Vec3.atBottomCenterOf(f.target)); f.entity = mob;
            f.check(f.level.addFreshEntity(mob), "real advanced-marker entity enters world"); f.aim(mob.getEyePosition());
            f.knowledge.scanEntity("#entity:Enderman"); f.duplicate();
            f.clearTarget(); f.resetKnowledge();
            f.level.setBlock(f.target, ThaumcraftContent.AURA_NODE.get().defaultBlockState(), 3);
            var node = (TileNode) f.level.getBlockEntity(f.target); f.aim(Vec3.atCenterOf(f.target));
            f.knowledge.scanPhenomena("#NODE" + node.getScanId()); f.duplicate();
        }
        test.succeed();
    }

    public static void physicalKindsRecordExactKnowledge(GameTestHelper test) {
        var f = new Fixture(test);
        try {
            // ConfigAspects original vanilla entries: diamond vitreus4/lucrum4; stone terra2.
            f.drop(new ItemStack(Items.DIAMOND)); f.begin();
            f.afterScan(() -> {
                f.exactReadout(Map.of("vitreus", 4, "lucrum", 4));
                f.exactPools(Map.of("vitreus", 4, "lucrum", 4));
                f.check(f.knowledge.getScannedItems().equals(java.util.Set.of("@minecraft:diamond")), "diamond scan has the exact item knowledge entry");
                f.duplicate(); f.persistAndIsolate();
                f.clearTarget(); f.resetKnowledge();
                f.level.setBlock(f.target, Blocks.STONE.defaultBlockState(), 3); f.aim(Vec3.atCenterOf(f.target)); f.begin();
                f.afterScan(() -> {
                    f.exactReadout(Map.of("terra", 2)); f.exactPools(Map.of("terra", 2));
                    f.check(f.knowledge.getScannedItems().equals(java.util.Set.of("@minecraft:stone")), "block scan records picked ITEM, not a separate block bucket");
                    f.duplicate(); f.tableConsumesStoneReward(); f.clearTarget(); f.resetKnowledge();
                    var mob = EntityType.ENDERMAN.create(f.level); f.check(mob != null, "real Enderman exists");
                    mob.setNoAi(true); mob.setNoGravity(true); mob.setSilent(true); mob.setPos(Vec3.atBottomCenterOf(f.target));
                    f.entity = mob; f.check(f.level.addFreshEntity(mob), "Enderman enters the actual world index"); f.aim(mob.getEyePosition()); f.begin();
                    f.afterScan(() -> {
                        f.exactReadout(Map.of("alienis", 4, "iter", 2, "aer", 2)); f.exactPools(Map.of("alienis", 4, "iter", 2, "aer", 2));
                        f.check(f.knowledge.getScannedEntities().equals(java.util.Set.of("@entity:Enderman")) && f.knowledge.isResearchComplete("@MIRROR"), "entity scan records exact variant and original MIRROR clue");
                        f.check(f.receipts.stream().filter(id -> id.getPath().equals("research_notice")).count() == 1, "one real research-popup packet for the direct Enderman clue");
                        f.duplicate(); f.clearTarget(); f.resetKnowledge();
                        scanNode(f, 0);
                    }, null);
                }, null);
            }, null);
        } catch (RuntimeException | Error error) { f.close(); throw error; }
    }

    private static void scanNode(Fixture f, int index) {
        NodeType[] types = NodeType.values();
        if (index == types.length) {
            f.clearTarget(); f.resetKnowledge();
            var handlers = ThaumcraftApi.scanEventhandlers;
            ThaumcraftApi.scanEventhandlers = new ArrayList<>();
            try {
                ThaumcraftApi.registerScanEventhandler((tool, world, player) -> new ScanResult((byte) 3, 0, 0, null, "NOT_A_NODE"));
                f.aim(f.player.getEyePosition().add(0, 6, 0)); var before = f.knowledge.save();
                f.player.gameMode.useItem(f.player, f.level, f.tool, InteractionHand.MAIN_HAND);
                f.check(!f.tool.has(ThaumcraftContent.SCAN_SESSION.get()) && before.equals(f.knowledge.save()) && f.capture().isEmpty(), "arbitrary phenomenon callback gives no knowledge, reward or rune (original only implements NODE)");
            } finally { ThaumcraftApi.scanEventhandlers = handlers; }
            f.close(); f.test.succeed(); return;
        }
        // Explicit original server option isolates knowledge from hungry-node motion/destruction.
        // This is not an ecology test; no session time or reward amount is injected.
        var settings = f.settings;
        thaumcraft.common.config.ThaumcraftSettings.configureServer(new thaumcraft.common.config.ResearchSettings(settings.cheatSheet(), settings.wardedStone(), settings.mirrors(), false, settings.wuss(), settings.difficulty(), settings.aspectCap()));
        f.resetKnowledge(); f.level.setBlock(f.target, ThaumcraftContent.AURA_NODE.get().defaultBlockState(), 3);
        var node = (TileNode) f.level.getBlockEntity(f.target); node.setAspects(new AspectList().add(Aspect.ORDER, 50));
        node.setNodeType(types[index]); node.setNodeModifier(NodeModifier.BRIGHT);
        String signature = "@NODE" + node.getScanId();
        Map<String, Integer> expected = switch (types[index]) {
            case NORMAL -> Map.of("ordo", 5);
            case UNSTABLE -> Map.of("ordo", 5, "perditio", 4);
            case HUNGRY -> Map.of("ordo", 5, "fames", 4);
            case TAINTED -> Map.of("ordo", 5, "vitium", 4);
            case PURE -> Map.of("ordo", 7, "sano", 2);
            case DARK -> Map.of("ordo", 5, "mortuus", 2, "tenebrae", 2);
        };
        f.aim(Vec3.atCenterOf(f.target)); f.begin();
        f.afterScan(() -> {
            f.exactReadout(expected); f.exactPools(expected);
            f.check(f.knowledge.getScannedPhenomena().equals(java.util.Set.of(signature)), "node type " + types[index] + " records actual UUID phenomenon");
            f.check(node.getAspects().getAmount(Aspect.ORDER) == 50 && node.getAspects().size() == 1, "node scan cannot drain/add live vis");
            f.check(f.knowledge.getResearchComplete().isEmpty(), "node has no clue witness or research popup"); f.duplicate();
            // Brightness is ignored by the original generateNodeAspects. Check all saved modifiers on the real readout.
            node.setNodeModifier(null); f.exactReadout(expected);
            for (var modifier : NodeModifier.values()) {
                node.setNodeModifier(modifier); f.exactReadout(expected);
            }
            f.clearTarget(); scanNode(f, index + 1);
        }, null);
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper test; final net.minecraft.server.level.ServerLevel level; final ServerPlayer player;
        final Connection connection = new Connection(PacketFlow.SERVERBOUND); final EmbeddedChannel channel = new EmbeddedChannel(connection);
        final PlayerKnowledge knowledge; final ItemStack tool = new ItemStack(ThaumcraftContent.THAUMOMETER.get());
        final thaumcraft.common.config.ResearchSettings settings = thaumcraft.common.config.ThaumcraftSettings.server();
        final BlockPos target; final net.minecraft.world.level.block.state.BlockState oldState; final CompoundTag oldNbt;
        final List<ResourceLocation> receipts = new ArrayList<>(); Entity entity; ItemEntity drop; boolean closed; Runnable waiting; long began;
        Fixture(GameTestHelper test) {
            this.test = test; level = test.getLevel(); target = test.absolutePos(new BlockPos(3, 12, 7));
            oldState = level.getBlockState(target); oldNbt = level.getBlockEntity(target) == null ? null : level.getBlockEntity(target).saveWithFullMetadata(level.registryAccess());
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "scan-" + UUID.randomUUID().toString().substring(0, 8)), ClientInformation.createDefault());
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setPos(Vec3.atBottomCenterOf(target.north(4)));
            knowledge = KnowledgeAccess.get(player); resetKnowledge(); player.setItemInHand(InteractionHand.MAIN_HAND, tool); capture();
            test.onEachTick(() -> {
                if (closed || waiting == null) return;
                try {
                    if (player.isUsingItem()) tool.getItem().onUseTick(level, player, tool, player.getUseItemRemainingTicks());
                    if (level.getGameTime() - began < 20) { check(knowledge.getScannedItems().isEmpty() && knowledge.getScannedEntities().isEmpty() && knowledge.getScannedPhenomena().isEmpty(), "no early knowledge before twenty real world ticks"); return; }
                    if (tool.has(ThaumcraftContent.SCAN_SESSION.get())) return;
                    var next = waiting; waiting = null; receipts.addAll(capture()); next.run();
                } catch (RuntimeException | Error error) { close(); throw error; }
            });
        }
        void resetKnowledge() { knowledge.reset(); knowledge.setInitializedAspects(true); for (var aspect : Aspect.aspects.values()) knowledge.addDiscoveredAspect(aspect.getTag()); receipts.clear(); capture(); }
        void drop(ItemStack stack) { drop = new ItemEntity(level, target.getX() + .5, target.getY() + .5, target.getZ() + .5, stack, 0, 0, 0); drop.setNoGravity(true); drop.setNeverPickUp(); entity = drop; check(level.addFreshEntity(drop), "actual item drop enters world"); aim(drop.getEyePosition()); }
        void aim(Vec3 at) { var delta = at.subtract(player.getEyePosition()); player.setYRot((float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90)); player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)))); }
        void begin() { began = level.getGameTime(); check(player.gameMode.useItem(player, level, tool, InteractionHand.MAIN_HAND).consumesAction() && tool.has(ThaumcraftContent.SCAN_SESSION.get()), "real survival held thaumometer starts observed-target scan"); }
        void afterScan(Runnable assertions, Runnable cleanup) {
            waiting = () -> { try { assertions.run(); if (cleanup != null) { close(); test.succeed(); } } finally { if (cleanup != null) cleanup.run(); } };
        }
        void exactReadout(Map<String, Integer> expected) { var readout = ItemThaumometer.readout(player, tool); var actual = new java.util.TreeMap<String, Integer>(); check(readout != null, "real ray still sees target"); for (var aspect : readout.aspects().getAspects()) actual.put(aspect.getTag(), readout.aspects().getAmount(aspect)); check(actual.equals(expected), "exact washed scan aspects expected " + expected + " actual " + actual); }
        void exactPools(Map<String, Integer> expected) { var actual = new java.util.TreeMap<String, Integer>(); knowledge.getAspectsDiscovered().forEach((tag, amount) -> { if (amount != 0) actual.put(tag, amount); }); check(actual.equals(expected), "exact scan RP expected " + expected + " actual " + actual); check(knowledge.getTotalWarp() == 0, "ordinary scan awards no warp, as original completeScan"); }
        void duplicate() {
            var before = knowledge.save(); capture(); player.gameMode.useItem(player, level, tool, InteractionHand.MAIN_HAND);
            check(!tool.has(ThaumcraftContent.SCAN_SESSION.get()) && capture().isEmpty() && before.equals(knowledge.save()), "duplicate real RMB emits no rune/reward/discovery/research packet and changes no knowledge");
            player.stopUsingItem(); var target = ItemThaumometer.readout(player, tool); check(target != null, "duplicate target remains physically observable");
            check(ScanManager.hasBeenScanned(knowledge, target.signature(), target.kind()), "shared readout predicate recognizes either original knowledge prefix");
            check(ScanManager.awardScan(knowledge, target.signature(), target.aspects(), target.kind(), settings.aspectCap()).failure() == ScanManager.Failure.DUPLICATE
                    && before.equals(knowledge.save()), "authoritative reward boundary also rejects duplicate without mutation even if admission is bypassed");
        }
        void persistAndIsolate() { var saved = new CompoundTag(); player.addAdditionalSaveData(saved); var other = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "scan-peer"), ClientInformation.createDefault()); check(!KnowledgeAccess.get(other).hasScannedItem("@minecraft:diamond"), "separate real ServerPlayer holder does not inherit scan"); other.readAdditionalSaveData(saved); check(KnowledgeAccess.get(other).save().equals(knowledge.save()), "actual ServerPlayer save/read restores exact scanned entry and aspect pools (not socket relogin)"); }
        void tableConsumesStoneReward() {
            var side = target.north(); var state = level.getBlockState(side); var tile = level.getBlockEntity(side);
            var saved = tile == null ? null : tile.saveWithFullMetadata(level.registryAccess());
            try {
                knowledge.setAspectPool("aqua", 1); // Declared second input; terra comes ONLY from the real stone scan.
                level.setBlock(target, ThaumcraftContent.TABLE.get().state(2), 2);
                level.setBlock(side, ThaumcraftContent.TABLE.get().state(7), 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(target), net.minecraft.core.Direction.SOUTH, target, false);
                check(player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit).consumesAction()
                        && player.containerMenu instanceof thaumcraft.common.container.ContainerResearchTable, "actual two-piece research table opens for scanning actor");
                var menu = (thaumcraft.common.container.ContainerResearchTable) player.containerMenu;
                var payload = new thaumcraft.common.lib.network.ResearchTableCombinePayload(menu.containerId, menu.sessionToken(), menu.tableRevision(), "terra", "aqua");
                check(thaumcraft.common.lib.network.ResearchTableCombineNetworking.handle(player, payload)
                        && knowledge.getAspectPoolFor("terra") == 1 && knowledge.getAspectPoolFor("aqua") == 0 && knowledge.getAspectPoolFor("victus") == 1,
                        "real research-table receiver reads and spends scan-earned terra from the same player holder");
            } finally {
                player.closeContainer(); player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                level.removeBlockEntity(side); level.setBlock(side, state, 2);
                if (saved != null && level.getBlockEntity(side) != null) level.getBlockEntity(side).loadWithComponents(saved, level.registryAccess());
            }
        }
        List<ResourceLocation> capture() { connection.flushChannel(); channel.runPendingTasks(); var ids = new ArrayList<ResourceLocation>(); Object packet; while ((packet = channel.readOutbound()) != null) { try { if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload().type().id().getNamespace().equals("thaumcraft")) ids.add(custom.payload().type().id()); } finally { ReferenceCountUtil.release(packet); } } return ids; }
        void check(boolean value, String why) { test.assertTrue(value, why); }
        void clearTarget() { player.stopUsingItem(); tool.remove(ThaumcraftContent.SCAN_SESSION.get()); if (entity != null) { entity.discard(); entity = null; } level.removeBlockEntity(target); level.setBlock(target, Blocks.AIR.defaultBlockState(), 3); }
        @Override public void close() { if (closed) return; closed = true; clearTarget(); level.setBlock(target, oldState, 3); if (oldNbt != null && level.getBlockEntity(target) != null) level.getBlockEntity(target).loadWithComponents(oldNbt, level.registryAccess()); thaumcraft.common.config.ThaumcraftSettings.configureServer(settings); level.getServer().getPlayerList().remove(player); channel.finishAndReleaseAll(); }
    }
}
