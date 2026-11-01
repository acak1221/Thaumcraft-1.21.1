package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.items.relics.ItemThaumometer;
import thaumcraft.common.lib.WarpEvents;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeSyncPayload;
import thaumcraft.common.lib.network.RunicChargePayload;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.research.ScanManager;
import thaumcraft.common.lib.research.ScanCallbacks;
import thaumcraft.common.lib.runic.RunicArmor;
import thaumcraft.common.lib.runic.RunicShield;

/** Review regressions over real owner attachments and captured outbound packets; inputs are declared fixtures. */
public final class ReviewKnowledgeRuntimeChecks {
    private ReviewKnowledgeRuntimeChecks() {}

    public static void absoluteAspectAndExactResearch(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var knowledge = KnowledgeAccess.get(f.player); knowledge.reset();
            knowledge.setAspectPool("aer", 10);
            packets(f, KnowledgeSyncPayload.TYPE, KnowledgeSyncPayload.STREAM_CODEC);
            ResearchManager.INSTANCE.completeAspect(f.player, Aspect.AIR, (short)15);
            test.assertTrue(knowledge.getAspectPoolFor("aer") == 15, "original completeAspect replaces the absolute total, not adds it");
            var sent = packets(f, KnowledgeSyncPayload.TYPE, KnowledgeSyncPayload.STREAM_CODEC);
            test.assertTrue(sent.size() == 1, "absolute update sends one snapshot");
            var client = new PlayerKnowledge(); client.load(sent.getFirst().state());
            test.assertTrue(client.getAspectPoolFor("aer") == 15, "snapshot contains the corrected absolute total");
            ResearchManager.INSTANCE.completeAspect(f.player, Aspect.AIR, (short)0);
            ResearchManager.INSTANCE.completeAspect(f.player, Aspect.LIGHT, (short)0);
            test.assertTrue(knowledge.getAspectPoolFor("aer") == 0 && knowledge.hasDiscoveredAspect("lux")
                    && knowledge.getAspectPoolFor("lux") == 0, "zero sets the pool and discovers an unknown aspect");
            ResearchManager.INSTANCE.completeAspect(f.player, Aspect.AIR, (short)-7);
            test.assertTrue(knowledge.getAspectPoolFor("aer") == -7, "original signed-short total is retained");
            ResearchManager.INSTANCE.completeAspect(f.player, Aspect.AIR, (short)10);
            ThaumcraftApiHelper.addAspectPool(f.player, Aspect.AIR, 5);
            test.assertTrue(knowledge.getAspectPoolFor("aer") == 15, "the separate native API still adds a delta");

            knowledge.reset();
            String key = "ADVANCEDGOLEM";
            int warp = WarpEvents.isWussMode() ? 0 : ResearchCatalog.get(key).currentWarp();
            test.assertTrue(ThaumcraftApiHelper.completeResearch(f.player, key), "the API can grant the requested research without prerequisites");
            test.assertTrue(knowledge.getResearchComplete().equals(Set.of(key)), "an exact grant does not grant parents, hidden parents or siblings");
            test.assertTrue(knowledge.getWarpPerm() == warp - warp / 2 && knowledge.getWarpSticky() == warp / 2
                    && knowledge.getWarpCounter() == warp, "only the granted research contributes warp");
            var before = knowledge.save();
            test.assertTrue(!ThaumcraftApiHelper.completeResearch(f.player, key) && knowledge.save().equals(before),
                    "repeated completion reports no grant and adds no warp");
        }
        test.succeed();
    }

    public static void potionSubtypesUseDistinctWorldTargets(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var knowledge = KnowledgeAccess.get(f.player); knowledge.reset();
            for (var aspect : Aspect.aspects.values()) knowledge.addDiscoveredAspect(aspect.getTag());
            var point = Vec3.atCenterOf(f.base); f.aim(point);
            var water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
            var healing = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
            var instrument = new ItemStack(ThaumcraftContent.THAUMOMETER.get());
            var bottle = new ItemEntity(f.level, point.x, point.y, point.z, water); bottle.setNoGravity(true);
            test.assertTrue(f.level.addFreshEntity(bottle), "fixture bottle enters the native world");
            try {
                var first = ItemThaumometer.readout(f.player, instrument);
                test.assertTrue(first != null && first.signature().equals("@minecraft:potion"), "ray cast reads the compatible water key");
                var waterItemCallback = ScanCallbacks.resolve(f.level, ScanResult.item(water));
                var waterDropCallback = ScanCallbacks.resolve(f.level, ScanResult.entity(bottle));
                test.assertTrue(waterItemCallback != null && waterDropCallback != null
                        && waterItemCallback.signature().equals(first.signature()) && waterDropCallback.signature().equals(first.signature()),
                        "stack and indexed entity callbacks resolve the same water knowledge key as the physical target");
                test.assertTrue(ScanManager.completeScan(f.player, first.signature(), first.aspects(), first.kind()), "the first world target awards its aspects");
                bottle.setItem(healing);
                var second = ItemThaumometer.readout(f.player, instrument);
                test.assertTrue(second != null && !second.signature().equals(first.signature()), "same item entity now containing healing has another subtype key");
                var healingItemCallback = ScanCallbacks.resolve(f.level, ScanResult.item(healing));
                var healingDropCallback = ScanCallbacks.resolve(f.level, ScanResult.entity(bottle));
                test.assertTrue(healingItemCallback != null && healingDropCallback != null
                        && healingItemCallback.signature().equals(second.signature()) && healingDropCallback.signature().equals(second.signature())
                        && !healingItemCallback.sessionKey().equals(waterItemCallback.sessionKey())
                        && !healingDropCallback.sessionKey().equals(waterDropCallback.sessionKey()),
                        "both callback paths preserve subtype knowledge and cancel a session whose potion changes");
                test.assertTrue(!ThaumcraftApiHelper.hasScannedItem(f.player, healing)
                        && ScanManager.completeScan(f.player, second.signature(), second.aspects(), second.kind()), "water does not suppress the healing scan");
                test.assertTrue(knowledge.getAspectPoolFor("sano") == 3 && ThaumcraftApiHelper.hasScannedItem(f.player, healing), "healing awards its own aspects and the API reads its subtype marker");
                test.assertTrue(!ScanManager.completeScan(f.player, healingItemCallback.signature(), healingItemCallback.aspects(), healingItemCallback.kind())
                        && !ScanManager.completeScan(f.player, healingDropCallback.signature(), healingDropCallback.aspects(), healingDropCallback.kind())
                        && knowledge.getAspectPoolFor("sano") == 3, "callback aliases cannot pay the physical healing scan a second time");
            } finally { bottle.discard(); }
        }
        test.succeed();
    }

    public static void runicMaximumSyncsWithoutRecharge(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            ItemStack boots = RunicArmor.augment(new ItemStack(ThaumcraftEquipment.THAUMIUM_BOOTS.get()));
            f.player.setItemSlot(EquipmentSlot.FEET, boots);
            RunicShield.markDirty(f.player); RunicShield.tick(f.player);
            packets(f, RunicChargePayload.TYPE, RunicChargePayload.STREAM_CODEC);
            test.assertTrue(RunicShield.view(f.player).charge() == 0 && RunicShield.view(f.player).maximum() == 1,
                    "empty inventory cannot pay a charge; one hardening point is equipped");

            f.player.setItemSlot(EquipmentSlot.FEET, RunicArmor.augment(boots));
            RunicShield.markDirty(f.player); RunicShield.tick(f.player);
            var increased = packets(f, RunicChargePayload.TYPE, RunicChargePayload.STREAM_CODEC);
            test.assertTrue(increased.size() == 1 && increased.getFirst().charge() == 0 && increased.getFirst().maximum() == 2,
                    "maximum increase reaches the client while charge stays zero and no vis exists");
            f.player.setItemSlot(EquipmentSlot.FEET, boots);
            RunicShield.markDirty(f.player); RunicShield.tick(f.player);
            var decreased = packets(f, RunicChargePayload.TYPE, RunicChargePayload.STREAM_CODEC);
            test.assertTrue(decreased.size() == 1 && decreased.getFirst().charge() == 0 && decreased.getFirst().maximum() == 1,
                    "maximum decrease above unchanged charge reaches the client too");
            RunicShield.tick(f.player);
            test.assertTrue(packets(f, RunicChargePayload.TYPE, RunicChargePayload.STREAM_CODEC).isEmpty(), "unchanged charge and maximum produce no redundant snapshot");
        }
        test.succeed();
    }

    private static <T extends CustomPacketPayload> List<T> packets(TubeVariantsRuntimeChecks.Fixture f,
            CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        f.connection.flushChannel(); f.channel.runPendingTasks();
        var result = new ArrayList<T>(); Object packet;
        while ((packet = f.channel.readOutbound()) != null) {
            try {
                if (!(packet instanceof ClientboundCustomPayloadPacket custom) || !custom.payload().type().id().equals(type.id())) continue;
                byte[] bytes;
                if (custom.payload() instanceof KnowledgeSyncPayload sync) {
                    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), f.level.registryAccess());
                    try { KnowledgeSyncPayload.STREAM_CODEC.encode(buffer, sync); bytes = new byte[buffer.readableBytes()]; buffer.readBytes(bytes); }
                    finally { buffer.release(); }
                } else if (custom.payload() instanceof RunicChargePayload charge) {
                    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), f.level.registryAccess());
                    try { RunicChargePayload.STREAM_CODEC.encode(buffer, charge); bytes = new byte[buffer.readableBytes()]; buffer.readBytes(bytes); }
                    finally { buffer.release(); }
                } else {
                    try { bytes = (byte[])custom.payload().getClass().getMethod("payload").invoke(custom.payload()); }
                    catch (ReflectiveOperationException failure) { throw new AssertionError("Unreadable native payload wrapper", failure); }
                }
                var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), f.level.registryAccess());
                try { result.add(codec.decode(buffer)); }
                finally { buffer.release(); }
            } finally { ReferenceCountUtil.release(packet); }
        }
        return result;
    }
}
