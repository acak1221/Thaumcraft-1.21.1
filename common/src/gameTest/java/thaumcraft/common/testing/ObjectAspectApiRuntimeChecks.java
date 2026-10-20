package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.config.ThaumcraftArcaneEar;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ObjectAspectCatalog;
import thaumcraft.common.lib.research.ObjectAspectRegistry;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.ScanCallbacks;
import thaumcraft.common.tiles.TileAlchemyFurnace;

/** Actual public registrations consumed by native resolution, scanning and the production distillation ticker. */
public final class ObjectAspectApiRuntimeChecks {
    private ObjectAspectApiRuntimeChecks() {}

    public static void nativeDirectAndGroupedTagsReachProductionConsumers(GameTestHelper test) {
        var level = test.getLevel(); var position = test.absolutePos(new BlockPos(3, 3, 3));
        var oldBlock = level.getBlockState(position); var oldTile = level.getBlockEntity(position);
        var oldNbt = oldTile == null ? null : oldTile.saveWithFullMetadata(level.registryAccess());
        try (var maps = new Maps()) {
            var declared = new ItemStack(Items.STONE, 32); declared.set(DataComponents.CUSTOM_NAME, Component.literal("NBT does not narrow object tags"));
            var live = new AspectList().add(Aspect.AIR, 7).add(Aspect.EARTH, 2);
            ThaumcraftApi.registerObjectTag(declared, live);
            test.assertTrue(ThaumcraftApi.objectTags.get(List.of(Items.STONE, 0)) == live
                            && ObjectAspectResolver.get(new ItemStack(Items.STONE, 64), level).aspects.equals(live.aspects),
                    "original object registration ignores count and custom NBT, retains caller reference and replaces the baseline for real stacks");
            var returned = ObjectAspectResolver.get(new ItemStack(Items.STONE), level); returned.add(Aspect.FIRE, 90);
            test.assertTrue(live.getAmount(Aspect.FIRE) == 0, "a resolved stack does not mutate the public registration");
            test.assertTrue(ScanCallbacks.resolve(level, ScanResult.item(new ItemStack(Items.STONE))).aspects().aspects.equals(live.aspects),
                    "the actual native scan callback resolver consumes public object-tag overrides");

            level.setBlock(position, ThaumcraftDistillation.ALCHEMY_FURNACE.get().defaultBlockState(), 3);
            var furnace = (TileAlchemyFurnace) level.getBlockEntity(position); test.assertTrue(furnace != null, "real native alchemy furnace exists");
            furnace.setItem(TileAlchemyFurnace.INPUT_SLOT, new ItemStack(Items.STONE, 2));
            furnace.setItem(TileAlchemyFurnace.FUEL_SLOT, new ItemStack(Items.COAL));
            for (int tick = 0; tick < 89; tick++) TileAlchemyFurnace.tick(level, position, level.getBlockState(position), furnace);
            test.assertTrue(furnace.getItem(0).getCount() == 2 && furnace.getVis() == 0 && furnace.getCookTime() == 89,
                    "nine registered vis require the actual90-call cook; public tags do not inject finished essentia");
            TileAlchemyFurnace.tick(level, position, level.getBlockState(position), furnace);
            test.assertTrue(furnace.getItem(0).getCount() == 1 && furnace.getItem(1).isEmpty() && furnace.getAspects().aspects.equals(live.aspects),
                    "one genuine furnace cook consumes one stone and one coal and produces exactly registered aer7/terra2");
            ThaumcraftApi.registerObjectTag(new ItemStack(Items.STONE), null);
            test.assertTrue(ObjectAspectResolver.baseTags(new ItemStack(Items.STONE), level).size() == 0 && !furnace.canPlaceItem(0, new ItemStack(Items.STONE)),
                    "explicit EMPTY registration blocks both recipe fallback and the actual furnace input policy");
            ThaumcraftApi.objectTags.remove(List.of(Items.STONE, 0));
            test.assertTrue(ObjectAspectResolver.baseTags(new ItemStack(Items.STONE), level).size() == 0,
                    "removing a public direct tag cannot silently resurrect its immutable catalogue baseline");

            ThaumcraftApi.registerObjectTag(ResourceLocation.parse("minecraft:stone"), 0, new AspectList().add(Aspect.FIRE, 3));
            test.assertTrue(ObjectAspectResolver.get(new ItemStack(Items.STONE), level).getAmount(Aspect.FIRE) == 3, "native registry-ID registration reaches actual stack resolution");
            var shared = new AspectList().add(Aspect.WATER, 3); ThaumcraftApi.registerObjectTag(ItemTags.COALS, shared); shared.add(Aspect.MAGIC, 1);
            test.assertTrue(ObjectAspectResolver.baseTags(new ItemStack(Items.COAL), level).aspects.equals(shared.aspects)
                            && ObjectAspectResolver.baseTags(new ItemStack(Items.CHARCOAL), level).aspects.equals(shared.aspects),
                    "native TagKey registration snapshots real current membership and shares its caller list across members");
            ThaumcraftApi.registerObjectTag("minecraft:coals", new AspectList().add(Aspect.ORDER, 2));
            test.assertTrue(ObjectAspectResolver.baseTags(new ItemStack(Items.CHARCOAL), level).getAmount(Aspect.ORDER) == 2,
                    "native namespaced string item-tag overload uses the actual tag rather than inventing an OreDictionary registry");

            ThaumcraftApi.objectTags.keySet().removeIf(key -> key.getFirst() == Items.BOW);
            ThaumcraftApi.registerObjectTag(Items.BOW, 32767, new AspectList().add(Aspect.WATER, 3));
            ThaumcraftApi.registerObjectTag(Items.BOW, 2, new AspectList().add(Aspect.FIRE, 1));
            var bow = new ItemStack(Items.BOW); bow.setDamageValue(2);
            test.assertTrue(ObjectAspectResolver.baseTags(bow, level).getAmount(Aspect.FIRE) == 1, "exact native damage metadata precedes a wildcard registration");
            bow.setDamageValue(40);
            test.assertTrue(ObjectAspectResolver.baseTags(bow, level).getAmount(Aspect.WATER) == 3, "other native damage values resolve the explicit32767 wildcard without fabricated stack damage");
            bow.setDamageValue(2); int[] group = {7, 2, 5};
            ThaumcraftApi.registerObjectTag(bow, group, new AspectList().add(Aspect.AIR, 6));
            var scan = ScanCallbacks.resolve(level, ScanResult.item(bow));
            test.assertTrue(Arrays.equals(group, new int[]{7, 2, 5}) && scan.aspects().getAmount(Aspect.AIR) == 6 && scan.aspects().getAmount(Aspect.FIRE) == 0
                            && ObjectAspectResolver.baseTags(bow, level).getAmount(Aspect.FIRE) == 1 && bow.getDamageValue() == 2,
                    "scan-only canonical metadata uses caller first7 while ordinary dissolution keeps exact2 and physical damage remains unchanged");
            // Native ordinary items clamp damage to0, so their grouped lookup metadata must remain a separate typed witness.
            ThaumcraftApi.registerObjectTag(new ItemStack(Items.STONE), new int[]{7, 0}, new AspectList().add(Aspect.ORDER, 6));
            test.assertTrue(ObjectAspectResolver.groupedScanWitness(new ItemStack(Items.STONE)).metadata() == 7
                            && ObjectAspectResolver.scanTags(new ItemStack(Items.STONE), level).getAmount(Aspect.ORDER) == 6
                            && ObjectAspectResolver.get(new ItemStack(Items.STONE), level).getAmount(Aspect.FIRE) == 3,
                    "plain native item group7/0 preserves original metadata semantics without silently clamping the canonical lookup to0");

            ThaumcraftApi.objectTags.keySet().removeIf(key -> key.getFirst() == Items.BOW);
            int[] range = {9, 2, 5}; var raw = new AspectList().add(Aspect.AIR, 70);
            ThaumcraftApi.objectTags.put(List.of(Items.BOW, range), raw); bow.setDamageValue(5);
            test.assertTrue(!ThaumcraftApi.exists(Items.BOW, 5) && ObjectAspectResolver.baseTags(bow, level).getAmount(Aspect.AIR) == 70
                            && Arrays.equals(range, new int[]{2, 5, 9}),
                    "manually inserted range keys sort their live array and return before cap64 while exists ignores ranges");
            test.succeed();
        } finally {
            // Test-owned temporary stock is not a paid player transaction; remove the BE before restoring its prior fixture.
            level.removeBlockEntity(position); level.setBlock(position, Blocks.AIR.defaultBlockState(), 3); level.setBlock(position, oldBlock, 3);
            if (oldNbt != null && level.getBlockEntity(position) != null) level.getBlockEntity(position).loadWithComponents(oldNbt, level.registryAccess());
        }
    }

    public static void complexRegistrationUsesOriginalBranchesAndServerPhase(GameTestHelper test) {
        var level = test.getLevel(); var server = level.getServer();
        try (var maps = new Maps()) {
            test.assertTrue(ObjectAspectRegistry.requireContext() == server.overworld(), "actual server lifecycle has published its valid recipe context");
            var old = new AspectList().add(Aspect.AIR, 5); ThaumcraftApi.registerObjectTag(new ItemStack(Items.STONE), old);
            var supplied = new AspectList().add(Aspect.AIR, 90).add(Aspect.WATER, 8);
            ThaumcraftApi.registerComplexObjectTag(new ItemStack(Items.STONE), supplied, level);
            var actual = ThaumcraftApi.objectTags.get(List.of(Items.STONE, 0));
            test.assertTrue(actual.getAmount(Aspect.AIR) == 5 && actual.aspects.containsKey(Aspect.WATER) && actual.getAmount(Aspect.WATER) == 0
                            && supplied.getAmount(Aspect.AIR) == 90 && old.size() == 1,
                    "existing complex branch ignores supplied values, retains old amounts, inserts absent zero-valued tags and leaves caller additions intact");

            ThaumcraftApi.objectTags.keySet().removeIf(key -> key.getFirst() == Items.OAK_DOOR);
            var expected = ObjectAspectResolver.generatedTags(Items.OAK_DOOR, level); test.assertTrue(expected.visSize() > 0, "actual vanilla door recipe has a real generated ingredient base");
            var additions = new AspectList().add(Aspect.FIRE, 4);
            ThaumcraftApi.registerComplexObjectTag(new ItemStack(Items.OAK_DOOR), additions, level);
            var combined = expected.copy().add(Aspect.FIRE, 4);
            test.assertTrue(ThaumcraftApi.objectTags.get(List.of(Items.OAK_DOOR, 0)) == additions && additions.aspects.equals(combined.aspects),
                    "absent complex branch mutates and retains supplied AspectList by ADDING the real generated recipe base");

            var goggles = new ItemStack(ThaumcraftWearables.GOGGLES.get()); goggles.setDamageValue(40);
            var beforeGoggles = ObjectAspectResolver.baseTags(goggles, level);
            test.assertTrue(ThaumcraftApi.exists(goggles.getItem(), 40), "original433 durable goggles wildcard remains an existing native on-demand complex declaration");
            ThaumcraftApi.registerComplexObjectTag(goggles, new AspectList().add(Aspect.ELDRITCH, 90), level);
            test.assertTrue(ObjectAspectResolver.baseTags(goggles, level).getAmount(Aspect.SENSES) == beforeGoggles.getAmount(Aspect.SENSES)
                            && ObjectAspectResolver.baseTags(goggles, level).getAmount(Aspect.ELDRITCH) == 0,
                    "API complex registration over a native existing complex item does not double its base or accept supplied90eldritch");
            var ear = ObjectAspectResolver.baseTags(new ItemStack(ThaumcraftArcaneEar.ARCANE_EAR_ITEM.get()), level);
            test.assertTrue(ear.getAmount(Aspect.SENSES) == 4, "actually available Arcane Ear gets its original434 raw complex senses4 on top of the real recipe");

            ObjectAspectRegistry.serverStopped(server); boolean rejected = false;
            try { ThaumcraftApi.registerComplexObjectTag(new ItemStack(Items.STONE), new AspectList().add(Aspect.FIRE, 1)); }
            catch (IllegalStateException expectedFailure) { rejected = expectedFailure.getMessage().contains("server recipe"); }
            finally { ObjectAspectRegistry.serverStarted(server); }
            test.assertTrue(rejected && ThaumcraftApi.objectTags.get(List.of(Items.STONE, 0)) == actual,
                    "cleared recipe context rejects original-shaped calls with an explicit migration message and no public-map mutation");
            Throwable[] offThread = {null};
            Thread worker = new Thread(() -> { try { ObjectAspectRegistry.requireContext(level); } catch (Throwable failure) { offThread[0] = failure; } }, "r16-context-check");
            worker.start(); try { worker.join(1000); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
            test.assertTrue(!worker.isAlive() && offThread[0] instanceof IllegalStateException, "explicit Level overload cannot access server recipes from another thread");
            test.succeed();
        }
    }

    public static void callbackCanRegisterComplexTagsAndEarnOnlyOriginalExistingAmounts(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel(); var maps = new Maps(); var oldHandlers = ThaumcraftApi.scanEventhandlers;
        var knowledge = KnowledgeAccess.get(player); var oldKnowledge = knowledge.copy(); var position = player.position();
        float yaw = player.getYRot(), pitch = player.getXRot(); var gameType = player.gameMode.getGameModeForPlayer();
        var inventory = new ArrayList<ItemStack>(); for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var origin = test.absolutePos(new BlockPos(3, 12, 3)); var chunk = new ChunkPos(origin); boolean forced = !level.getForcedChunks().contains(chunk.toLong());
        boolean[] closed = {false}; int[] calls = {0};
        Runnable cleanup = () -> {
            if (closed[0]) return; closed[0] = true; player.stopUsingItem(); maps.close(); ThaumcraftApi.scanEventhandlers = oldHandlers;
            player.getInventory().clearContent(); for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.setPos(position); player.setYRot(yaw); player.setXRot(pitch); player.setGameMode(gameType); knowledge.copyFrom(oldKnowledge);
            if (forced) level.setChunkForced(chunk.x, chunk.z, false);
        };
        try {
            if (forced) level.setChunkForced(chunk.x, chunk.z, true);
            player.stopUsingItem(); player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent(); player.setPos(Vec3.atBottomCenterOf(origin)); player.setXRot(-90); player.setYRot(0);
            knowledge.reset(); for (var aspect : Aspect.aspects.values()) knowledge.addDiscoveredAspect(aspect.getTag());
            ThaumcraftApi.registerObjectTag(new ItemStack(Items.STONE), new AspectList().add(Aspect.AIR, 3));
            var tool = new ItemStack(ThaumcraftContent.THAUMOMETER.get()); player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            ThaumcraftApi.scanEventhandlers = new ArrayList<>();
            ThaumcraftApi.registerScanEventhandler((instrument, world, actor) -> {
                test.assertTrue(instrument == tool && world == level && actor == player, "native callback receives the actual server use context");
                calls[0]++; ThaumcraftApi.registerComplexObjectTag(new ItemStack(Items.STONE), new AspectList().add(Aspect.EARTH, 99));
                return ScanResult.item(new ItemStack(Items.STONE));
            });
            test.assertTrue(player.gameMode.useItem(player, level, tool, InteractionHand.MAIN_HAND).consumesAction(), "real RMB resolves a callback that uses the original-shaped complex API in its valid native phase");
            long started = level.getGameTime();
            test.onEachTick(() -> {
                if (closed[0]) return;
                try {
                    if (player.isUsingItem()) tool.getItem().onUseTick(level, player, tool, player.getUseItemRemainingTicks());
                    if (level.getGameTime() - started < 20) { test.assertTrue(!knowledge.hasScannedItem("@minecraft:stone"), "no API callback RP before twenty real world ticks"); return; }
                    if (!knowledge.hasScannedItem("@minecraft:stone")) return;
                    test.assertTrue(calls[0] >= 2 && knowledge.getAspectPoolFor("aer") == 3 && knowledge.getAspectPoolFor("terra") == 0
                                    && !tool.has(ThaumcraftContent.SCAN_SESSION.get()) && !player.isUsingItem() && tool.getCount() == 1,
                            "real callback/complex API/thaumometer chain pays only original existing aer3, never supplied99earth, and completes once");
                    cleanup.run(); test.succeed();
                } catch (RuntimeException | Error error) { cleanup.run(); throw error; }
            });
            test.runAfterDelay(80, () -> { if (!closed[0]) { cleanup.run(); test.fail("object-aspect callback did not complete within80 real ticks"); } });
        } catch (RuntimeException | Error error) { cleanup.run(); throw error; }
    }

    private static final class Maps implements AutoCloseable {
        final ConcurrentHashMap<List<?>, AspectList> old;
        final ConcurrentHashMap<List<?>, int[]> groups;
        Maps() {
            ObjectAspectCatalog.initializePublicTags(); old = ThaumcraftApi.objectTags; groups = ThaumcraftApi.groupedObjectTags;
            ThaumcraftApi.objectTags = new ConcurrentHashMap<>(old); ThaumcraftApi.groupedObjectTags = new ConcurrentHashMap<>(groups);
        }
        @Override public void close() { ThaumcraftApi.objectTags = old; ThaumcraftApi.groupedObjectTags = groups; }
    }
}
