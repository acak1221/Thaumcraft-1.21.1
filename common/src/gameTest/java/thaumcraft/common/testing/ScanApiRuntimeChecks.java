package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.IScanEventHandler;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.EntityAspectBindings;
import thaumcraft.common.lib.research.ResearchClues;
import thaumcraft.common.lib.research.ResearchTriggerBindings;
import thaumcraft.common.lib.research.ScanCallbacks;
import thaumcraft.common.lib.research.ScanManager;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.api.nodes.NodeType;

/** Actual native entity NBT, node locators and held-tool callback/session behavior. No client rendering claim. */
public final class ScanApiRuntimeChecks {
    private ScanApiRuntimeChecks() {}

    public static void nativeApiUsesWorldNbtAndNodeLocators(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel(); var rows = ThaumcraftApi.scanEntities;
        var position = test.absolutePos(new BlockPos(4, 3, 4)); var oldBlock = level.getBlockState(position);
        var oldTile = level.getBlockEntity(position); var oldNbt = oldTile == null ? null : oldTile.saveWithFullMetadata(level.registryAccess());
        Entity indexed = null;
        try {
            ThaumcraftApi.scanEntities = new ArrayList<>(EntityAspectBindings.originalTags());
            var source = new AspectList().add(Aspect.AIR, 7);
            ThaumcraftApi.registerEntityTag("minecraft:cow", source, new ThaumcraftApi.EntityTagsNBT("NoAI", (byte) 1));
            var cow = EntityType.COW.create(level); test.assertTrue(cow != null, "native Cow fixture exists"); cow.setNoAi(true);
            var cowTarget = EntityAspectBindings.describe(cow);
            test.assertTrue(cowTarget.aspects().getAmount(Aspect.AIR) == 7 && cowTarget.signature().contains("NoAI1"),
                    "public native-ID predicate reads actual byte-valued entity NBT and changes its original variant identity");
            cowTarget.aspects().add(Aspect.AIR, 10);
            test.assertTrue(source.getAmount(Aspect.AIR) == 7, "resolved scan aspects cannot mutate the live addon registration");
            test.assertTrue(ScanCallbacks.resolve(level, ScanResult.entity(cow)) == null, "unindexed entity callback cannot invent a world target");
            cow.setPos(Vec3.atCenterOf(position.above(3))); test.assertTrue(level.addFreshEntity(cow), "callback entity enters actual native index"); indexed = cow;
            var resolved = ScanCallbacks.resolve(level, ScanResult.entity(cow));
            test.assertTrue(resolved != null && resolved.sessionKey().equals("entity:" + cow.getId()) && resolved.aspects().getAmount(Aspect.AIR) == 7,
                    "indexed native callback entity uses its instance ID for the use session");
            cow.discard(); indexed = null;
            test.assertTrue(ScanCallbacks.resolve(level, ScanResult.entity(cow)) == null, "removed entities cannot be rewarded through stale DTOs");

            var bow = new ItemStack(Items.BOW); bow.setDamageValue(40); bow.set(DataComponents.CUSTOM_NAME, Component.literal("native witness"));
            var itemResult = ScanResult.item(bow); bow.setDamageValue(41);
            var item = ScanCallbacks.resolve(level, itemResult);
            test.assertTrue(item != null && item.sessionKey().equals("item:minecraft:bow:40") && item.clue().item().getHoverName().getString().equals("native witness"),
                    "native DTO snapshots damage and components independently of its caller's stack");
            var numeric = ScanCallbacks.resolve(level, new ScanResult((byte) 1, BuiltInRegistries.ITEM.getId(Items.BOW), 40, null, ""));
            test.assertTrue(numeric != null && numeric.sessionKey().equals(item.sessionKey()), "original-shaped numeric callback IDs mean current native item ID and durable damage");
            test.assertTrue(ScanCallbacks.resolve(level, new ScanResult((byte) 1, BuiltInRegistries.ITEM.getId(Items.RED_WOOL), 1, null, "")) == null,
                    "flattened native wool requires its registry identity rather than reinterpreting old subtype metadata");
            test.assertTrue(ScanCallbacks.resolve(level, new ScanResult((byte) 3, 0, 0, null, "FREE_RESEARCH")) == null,
                    "unimplemented phenomena never become arbitrary research rewards");

            level.setBlock(position, ThaumcraftContent.AURA_NODE.get().defaultBlockState(), 3);
            var node = (TileNode) level.getBlockEntity(position); test.assertTrue(node != null, "real aura-node block entity exists");
            node.setAspects(new AspectList().add(Aspect.ORDER, 50));
            var nodeNbt = node.saveWithoutMetadata(level.registryAccess()); nodeNbt.putByte("type", (byte) NodeType.PURE.ordinal());
            node.loadWithComponents(nodeNbt, level.registryAccess()); // Explicit saved-node fixture; no natural PURE ecology claim.
            var locator = ScanResult.node(node); var nodeResult = ScanCallbacks.resolve(level, locator);
            test.assertTrue(nodeResult != null && nodeResult.aspects().getAmount(Aspect.ORDER) == 7 && nodeResult.aspects().getAmount(Aspect.HEAL) == 2
                            && node.getAspects().getAmount(Aspect.ORDER) == 50 && node.getAspects().getAmount(Aspect.HEAL) == 0,
                    "native node locator verifies the actual UUID and applies original PURE MAX-heal/ADD-order scan bonuses without draining vis");
            locator.phenomena = "NODEstale";
            test.assertTrue(ScanCallbacks.resolve(level, locator) == null, "a mismatched node UUID is rejected at its current native location");
            locator = ScanResult.node(node); locator.nodePosition = position.offset(1_000_000, 0, 1_000_000);
            test.assertTrue(!level.hasChunkAt(locator.nodePosition) && ScanCallbacks.resolve(level, locator) == null && !level.hasChunkAt(locator.nodePosition),
                    "node callbacks reject unloaded locations without force-loading their chunks");
            var pearl = new ItemStack(ThaumcraftContent.PRIMORDIAL_PEARL.get());
            test.assertTrue(ResearchTriggerBindings.matches("PRIMPEARL", ResearchClues.Context.item(pearl)), "new real primordial pearl is original eldritchObject3 trigger");
            pearl.set(DataComponents.CUSTOM_NAME, Component.literal("not the original blank NBT"));
            test.assertTrue(!ResearchTriggerBindings.matches("PRIMPEARL", ResearchClues.Context.item(pearl)), "late item trigger preserves original NBT equality");
            test.succeed();
        } finally {
            if (indexed != null) indexed.discard(); ThaumcraftApi.scanEntities = rows;
            level.setBlock(position, oldBlock, 3);
            if (oldNbt != null && level.getBlockEntity(position) != null) level.getBlockEntity(position).loadWithComponents(oldNbt, level.registryAccess());
        }
    }

    public static void identicalPhysicalTargetsCancelOldSession(GameTestHelper test, ServerPlayer player) {
        var fixture = new Fixture(test, player);
        fixture.whenEntitiesVisible(() -> identicalPhysicalTargets(test, player, fixture));
    }

    private static void identicalPhysicalTargets(GameTestHelper test, ServerPlayer player, Fixture fixture) {
        try (var f = fixture) {
            var first = EntityType.COW.create(f.level); var second = EntityType.COW.create(f.level);
            test.assertTrue(first != null && second != null, "two identical native mobs exist");
            first.setPos(f.front); second.setPos(f.side);
            for (var cow : List.of(first, second)) { cow.setNoAi(true); cow.setNoGravity(true); cow.setSilent(true); f.add(cow); }
            test.assertTrue(EntityAspectBindings.describe(first).signature().equals(EntityAspectBindings.describe(second).signature()), "fixture mobs share the same persistent knowledge key");
            aim(player, first.getEyePosition()); f.start(); String original = f.tool.get(ThaumcraftContent.SCAN_SESSION.get()).signature();
            first.setPos(f.side); second.setPos(f.front); aim(player, second.getEyePosition()); f.tick();
            // Oracle ScanResult.equals:26-43 compares the entity ID (func_145782_y) of a type two
            // scan, so another mob of the same type is a DIFFERENT target: the original
            // ItemThaumometer.onUsingTick:153-167 finds the new scan unequal to its start scan and
            // only clears startScan - the instrument stays raised until its twenty five ticks are
            // over (the original stops the use only in the completion branch, :157).
            test.assertTrue(!f.tool.has(ThaumcraftContent.SCAN_SESSION.get()) && player.isUsingItem() && original.equals("entity:" + first.getId())
                            && f.knowledge.getScannedEntities().isEmpty(), "retargeting another identical mob cancels before any reward, using the original entity-ID equality rule");
            first.discard(); second.discard();
            var drop1 = new ItemEntity(f.level, f.front.x, f.front.y + 1, f.front.z, new ItemStack(Items.STONE), 0, 0, 0);
            var drop2 = new ItemEntity(f.level, f.side.x, f.side.y + 1, f.side.z, new ItemStack(Items.STONE), 0, 0, 0);
            for (var drop : List.of(drop1, drop2)) { drop.setNoGravity(true); drop.setNeverPickUp(); f.add(drop); }
            aim(player, drop1.getEyePosition()); f.start();
            var api1 = ScanCallbacks.resolve(f.level, ScanResult.entity(drop1)); var api2 = ScanCallbacks.resolve(f.level, ScanResult.entity(drop2));
            test.assertTrue(api1 != null && api2 != null, "both indexed physical drops resolve through the real native callback API");
            test.assertTrue(api1.signature().equals(api2.signature()) && !api1.sessionKey().equals(api2.sessionKey()) && api1.kind() == ScanManager.ScanKind.ITEM,
                    "two original type2 item drops share item knowledge but retain separate entity session identities");
            drop1.setPos(f.side.add(0, 1, 0)); drop2.setPos(f.front.add(0, 1, 0)); aim(player, drop2.getEyePosition()); f.tick();
            test.assertTrue(!f.tool.has(ThaumcraftContent.SCAN_SESSION.get()) && f.knowledge.getScannedItems().isEmpty(), "retargeting an identical physical drop also cancels the held scan");
        }
        test.succeed();
    }

    public static void callbacksRespectOcclusionAndTwentyTickSession(GameTestHelper test, ServerPlayer player) {
        var fixture = new Fixture(test, player);
        fixture.whenEntitiesVisible(() -> callbacks(test, player, fixture));
    }

    private static void callbacks(GameTestHelper test, ServerPlayer player, Fixture f) {
        int[] calls = {0}; boolean[] closed = {false};
        Runnable cleanup = () -> { if (!closed[0]) { closed[0] = true; f.close(); } };
        try {
            ThaumcraftApi.scanEventhandlers = new ArrayList<>();
            ThaumcraftApi.registerScanEventhandler((stack, world, actor) -> { calls[0]++; return ScanResult.item(new ItemStack(Items.STONE)); });
            var unsupported = EntityType.ARMOR_STAND.create(f.level); test.assertTrue(unsupported != null, "unsupported native physical entity fixture exists");
            unsupported.setNoGravity(true); unsupported.setPos(f.front); f.add(unsupported); aim(player, unsupported.getEyePosition());
            // Oracle ItemThaumometer:88-96 - the pointed entity IS the scan target and the method
            // returns before the addon callbacks of :134-138, so a physical entity that carries no
            // scan aspects occludes the callbacks instead of falling through to a different reward.
            // Oracle ItemThaumometer:142-149 - the original always raises the instrument
            // (p.setItemInUse), so the use consumes the action even when no scan ever starts.
            test.assertTrue(player.gameMode.useItem(player, f.level, f.tool, InteractionHand.MAIN_HAND).consumesAction() && calls[0] == 0
                            && !f.knowledge.hasScannedItem("@minecraft:stone"),
                    "an unsupported physical entity occludes addon callbacks instead of falling through to a different reward"); unsupported.discard();
            var block = BlockPos.containing(f.front).above(); var old = f.level.getBlockState(block); f.level.setBlock(block, Blocks.STONE.defaultBlockState(), 3);
            try { aim(player, Vec3.atCenterOf(block)); f.start(); test.assertTrue(calls[0] == 0, "a real physical block precedes all scan callbacks"); player.stopUsingItem(); f.tool.remove(ThaumcraftContent.SCAN_SESSION.get()); }
            finally { f.level.setBlock(block, old, 3); }
            // Aim into the open sky: the ray hits nothing, so the addon callbacks are really asked.
            aim(player, player.getEyePosition().add(0.0, 6.0, 0.0)); calls[0] = 0;
            ThaumcraftApi.scanEventhandlers.clear();
            ThaumcraftApi.registerScanEventhandler((stack, world, actor) -> new ScanResult((byte) 99, 0, 0, null, "invalid first"));
            ThaumcraftApi.registerScanEventhandler((stack, world, actor) -> { calls[0]++; return ScanResult.item(new ItemStack(Items.STONE)); });
            // Oracle ItemThaumometer:134-138 - the first nonnull callback result ends the dispatch,
            // and the instrument is raised whatever that result is (oracle ItemThaumometer:147).
            test.assertTrue(player.gameMode.useItem(player, f.level, f.tool, InteractionHand.MAIN_HAND).consumesAction() && calls[0] == 0,
                    "the first nonnull callback, including an invalid one, prevents all later callbacks");
            ThaumcraftApi.scanEventhandlers.clear();
            ThaumcraftApi.registerScanEventhandler((stack, world, actor) -> null);
            ThaumcraftApi.registerScanEventhandler((stack, world, actor) -> {
                test.assertTrue(stack == f.tool && world == f.level && actor == player, "every dispatch receives the actual held stack, server world and actor");
                calls[0]++; return ScanResult.item(new ItemStack(Items.STONE));
            });
            ThaumcraftApi.registerScanEventhandler((stack, world, actor) -> { throw new AssertionError("first nonnull must stop callback dispatch"); });
            f.start(); long began = f.level.getGameTime();
            test.onEachTick(() -> {
                if (closed[0]) return;
                try {
                    if (player.isUsingItem()) f.tick();
                    if (f.level.getGameTime() - began < 20) {
                        test.assertTrue(!f.knowledge.hasScannedItem("@minecraft:stone"), "callback scan awards nothing before twenty real world ticks"); return;
                    }
                    if (!f.knowledge.hasScannedItem("@minecraft:stone")) return;
                    test.assertTrue(calls[0] >= 2 && f.knowledge.getAspectPoolFor("terra") > 0 && f.tool.getCount() == 1
                                    && !f.tool.has(ThaumcraftContent.SCAN_SESSION.get()) && !player.isUsingItem(),
                            "real held-tool callback resolves repeatedly, earns native item aspects once at twenty ticks and ends its session");
                    cleanup.run(); test.succeed();
                } catch (RuntimeException | Error error) { cleanup.run(); throw error; }
            });
            test.runAfterDelay(80, () -> { if (!closed[0]) { cleanup.run(); test.fail("native callback scan did not finish within80 world ticks"); } });
        } catch (RuntimeException | Error error) { cleanup.run(); throw error; }
    }

    private static void aim(ServerPlayer player, Vec3 target) {
        var delta = target.subtract(player.getEyePosition());
        player.setYRot((float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90));
        player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper test; final ServerPlayer player; final net.minecraft.server.level.ServerLevel level;
        final thaumcraft.common.lib.research.PlayerKnowledge knowledge, oldKnowledge;
        final ArrayList<IScanEventHandler> oldHandlers; final Vec3 oldPosition, position, front, side; final float oldYaw, oldPitch;
        final GameType oldGameType; final List<ItemStack> inventory = new ArrayList<>(); final List<Entity> entities = new ArrayList<>();
        final ItemStack tool; final List<ChunkPos> ownedForcedChunks = new ArrayList<>();
        Fixture(GameTestHelper test, ServerPlayer player) {
            this.test = test; this.player = player; level = test.getLevel(); knowledge = KnowledgeAccess.get(player); oldKnowledge = knowledge.copy();
            oldHandlers = ThaumcraftApi.scanEventhandlers; oldPosition = player.position(); oldYaw = player.getYRot(); oldPitch = player.getXRot(); oldGameType = player.gameMode.getGameModeForPlayer();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
            var origin = test.absolutePos(new BlockPos(3, 12, 3));
            position = Vec3.atBottomCenterOf(origin);
            front = position.add(0, 0, 4); side = position.add(12, 0, 0);
            // The parking target can be in a different chunk. addFreshEntity returning true does
            // not make an entity in an inaccessible section visible through ServerLevel.getEntity.
            // Own all fixture chunks before adding either cows or drops; do not depend on a prior test's tickets.
            for (var target : List.of(position, front, side)) {
                var chunk = new ChunkPos(BlockPos.containing(target));
                if (!level.getForcedChunks().contains(chunk.toLong())) {
                    ownedForcedChunks.add(chunk);
                    level.setChunkForced(chunk.x, chunk.z, true);
                }
                level.getChunk(chunk.x, chunk.z);
            }
            player.stopUsingItem(); player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent(); player.setPos(Vec3.atBottomCenterOf(origin));
            knowledge.reset(); for (var aspect : Aspect.aspects.values()) knowledge.addDiscoveredAspect(aspect.getTag());
            tool = new ItemStack(ThaumcraftContent.THAUMOMETER.get()); player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        }
        /**
         * Runs the scenario once every fixture position is entity ticking. A chunk this fixture forces
         * is loaded at once, but the entity section of a fresh chunk becomes visible a tick or more
         * later: under load an entity added before that was missing from ServerLevel.getEntity. The
         * wait is a GameTest sequence, so the scenario may still schedule its own delayed checks.
         */
        void whenEntitiesVisible(Runnable scenario) {
            test.startSequence().thenWaitUntil(() -> {
                for (var target : List.of(position, front, side))
                    if (!level.isPositionEntityTicking(BlockPos.containing(target)))
                        throw new net.minecraft.gametest.framework.GameTestAssertException("fixture chunk is not entity ticking yet: " + target);
            }).thenExecute(scenario);
        }

        void add(Entity entity) {
            entities.add(entity);
            test.assertTrue(level.addFreshEntity(entity), "fixture entity is accepted by the native world");
            test.assertTrue(level.getEntity(entity.getId()) == entity, "fixture target is accessible in the actual native entity index");
        }
        void start() { test.assertTrue(player.gameMode.useItem(player, level, tool, InteractionHand.MAIN_HAND).consumesAction() && tool.has(ThaumcraftContent.SCAN_SESSION.get()), "real native RMB starts a valid held-tool scan"); }
        void tick() { tool.getItem().onUseTick(level, player, tool, player.getUseItemRemainingTicks()); }
        @Override public void close() {
            player.stopUsingItem(); for (var entity : entities) entity.discard(); ThaumcraftApi.scanEventhandlers = oldHandlers;
            player.getInventory().clearContent(); for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.setGameMode(oldGameType); player.setPos(oldPosition); player.setYRot(oldYaw); player.setXRot(oldPitch); knowledge.copyFrom(oldKnowledge);
            for (var chunk : ownedForcedChunks) level.setChunkForced(chunk.x, chunk.z, false);
        }
    }
}
