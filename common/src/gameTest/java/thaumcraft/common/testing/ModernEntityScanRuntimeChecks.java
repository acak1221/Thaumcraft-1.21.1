package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.relics.ItemThaumometer;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.EntityAspectBindings;
import thaumcraft.common.lib.research.ModernEntityAspectBindings;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ScanManager;

/** Modern rows use the same live public registrations and physical scan/session boundary as TC4 mobs. */
public final class ModernEntityScanRuntimeChecks {
    private ModernEntityScanRuntimeChecks() {}

    public static void modernEntitiesKeepApiOverrides(GameTestHelper test) {
        var previous = ThaumcraftApi.scanEntities;
        try {
            ThaumcraftApi.scanEntities = new ArrayList<>(EntityAspectBindings.baselineTags());
            var types = Map.of(
                    "minecraft:breeze", EntityType.BREEZE, "minecraft:bogged", EntityType.BOGGED,
                    "minecraft:warden", EntityType.WARDEN, "minecraft:allay", EntityType.ALLAY,
                    "minecraft:sniffer", EntityType.SNIFFER, "minecraft:frog", EntityType.FROG,
                    "minecraft:armadillo", EntityType.ARMADILLO, "minecraft:axolotl", EntityType.AXOLOTL,
                    "minecraft:goat", EntityType.GOAT);
            for (var row : ModernEntityAspectBindings.tags()) {
                var entity = types.get(row.entityName).create(test.getLevel());
                test.assertTrue(entity != null, "registered modern mob creates: " + row.entityName);
                var described = EntityAspectBindings.describe(entity);
                test.assertTrue(described != null && described.signature().equals("@entity:" + row.entityName)
                                && described.aspects().aspects.equals(row.aspects.aspects)
                                && EntityAspectBindings.forType(entity.getType()).aspects.equals(row.aspects.aspects),
                        "real modern entity and its native type resolve their exact extension amounts: " + row.entityName);
                described.aspects().add(Aspect.AIR, 999);
                test.assertTrue(EntityAspectBindings.describe(entity).aspects().aspects.equals(row.aspects.aspects),
                        "readout does not mutate a public entity registration");
                entity.discard();
            }
            var frog = EntityType.FROG.create(test.getLevel());
            test.assertTrue(frog != null, "native frog exists for public override checks");
            frog.setNoAi(true);
            var override = new AspectList().add(Aspect.MIND, 7);
            ThaumcraftApi.registerEntityTag("minecraft:frog", override, new ThaumcraftApi.EntityTagsNBT("NoAI", (byte) 1));
            var selected = EntityAspectBindings.describe(frog);
            test.assertTrue(selected != null && selected.aspects().getAmount(Aspect.MIND) == 7
                            && selected.aspects().size() == 1 && selected.signature().contains("NoAI1"),
                    "a later matching native addon predicate replaces the bundled modern row");
            override.add(Aspect.WATER, 2);
            test.assertTrue(EntityAspectBindings.describe(frog).aspects().getAmount(Aspect.WATER) == 2,
                    "the original public registration still keeps caller-owned aspect lists live");
            ThaumcraftApi.registerEntityTag("minecraft:frog", null);
            test.assertTrue(EntityAspectBindings.describe(frog) == null, "a later explicit null row suppresses modern scanning");
            ThaumcraftApi.scanEntities.removeIf(row -> row.entityName.equals("minecraft:frog"));
            test.assertTrue(EntityAspectBindings.describe(frog) == null,
                    "removing the public modern row is respected without hidden runtime reseeding");
            frog.discard();
            test.succeed();
        } finally { ThaumcraftApi.scanEntities = previous; }
    }

    public static void modernEntityThaumometerUsesRealSession(GameTestHelper test) {
        var f = new Fixture(test);
        test.onEachTick(() -> {
            if (f.closed) return;
            try {
                if (!f.started) {
                    if (f.level.getEntity(f.frog.getId()) != f.frog) return;
                    f.begin();
                    return;
                }
                if (f.player.isUsingItem()) f.tool.getItem().onUseTick(f.level, f.player, f.tool, f.player.getUseItemRemainingTicks());
                long elapsed = f.level.getGameTime() - f.began;
                if (elapsed < ItemThaumometer.SCAN_TICKS) {
                    test.assertTrue(f.knowledge.getScannedEntities().isEmpty(), "modern scans award no knowledge before twenty real world ticks");
                    return;
                }
                if (f.tool.has(ThaumcraftContent.SCAN_SESSION.get())) return;
                if (f.phase == 0) {
                    test.assertTrue(f.knowledge.getScannedEntities().isEmpty() && f.before.equals(f.knowledge.save()),
                            "the actual held scan refuses unknown compound parents without rewarding or recording a modern mob");
                    f.player.stopUsingItem();
                    f.resetKnowledge(true);
                    f.phase = 1;
                    f.begin();
                    return;
                }
                String signature = "@entity:minecraft:frog";
                var expected = Map.of(Aspect.BEAST, 2, Aspect.WATER, 2, Aspect.SLIME, 1);
                test.assertTrue(ScanManager.hasBeenScanned(f.knowledge, signature, ScanManager.ScanKind.ENTITY),
                        "real modern entity scan records the stable native entity identity");
                var readout = ItemThaumometer.readout(f.player, f.tool);
                test.assertTrue(readout != null && readout.signature().equals(signature) && readout.aspects().aspects.equals(expected),
                        "actual thaumometer ray still describes the exact modern mob after completion");
                expected.forEach((aspect, amount) -> test.assertTrue(f.knowledge.getAspectPoolFor(aspect.getTag()) == amount,
                        "modern scan awards exactly its registered aspect amount: " + aspect.getTag()));
                test.assertTrue(f.knowledge.getTotalWarp() == 0, "scanning a normal modern mob does not invent warp");
                var earned = f.knowledge.save();
                f.player.gameMode.useItem(f.player, f.level, f.tool, InteractionHand.MAIN_HAND);
                test.assertTrue(!f.tool.has(ThaumcraftContent.SCAN_SESSION.get()) && earned.equals(f.knowledge.save()),
                        "a duplicate real held scan cannot mint repeat modern-mob rewards");
                f.close();
                test.succeed();
            } catch (RuntimeException | Error failure) { f.close(); throw failure; }
        });
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper test;
        final ServerLevel level;
        final ServerPlayer player;
        final PlayerKnowledge knowledge;
        final Frog frog;
        final ItemStack tool = new ItemStack(ThaumcraftContent.THAUMOMETER.get());
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        final EmbeddedChannel channel = new EmbeddedChannel(connection);
        final ChunkPos chunk;
        final boolean ownedForce;
        CompoundTag before;
        long began;
        int phase;
        boolean started, closed;

        Fixture(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            var target = test.absolutePos(new BlockPos(3, 12, 7)).atY(160);
            chunk = new ChunkPos(target);
            ownedForce = !level.getForcedChunks().contains(chunk.toLong());
            if (ownedForce) level.setChunkForced(chunk.x, chunk.z, true);
            level.getChunkAt(target);
            GameTestChunkAwait.entityTicking(test, target);
            player = new ServerPlayer(level.getServer(), level,
                    new GameProfile(UUID.randomUUID(), "modern-scan"), ClientInformation.createDefault());
            level.getServer().getPlayerList().placeNewPlayer(connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.setGameMode(GameType.SURVIVAL);
            player.setNoGravity(true);
            player.setPos(Vec3.atBottomCenterOf(target.north(4)));
            knowledge = KnowledgeAccess.get(player);
            resetKnowledge(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            frog = EntityType.FROG.create(level);
            test.assertTrue(frog != null, "physical modern frog exists");
            frog.setNoAi(true); frog.setNoGravity(true); frog.setSilent(true);
            frog.setPos(Vec3.atBottomCenterOf(target));
            test.assertTrue(level.addFreshEntity(frog), "physical modern frog enters the native world index");
            Vec3 delta = frog.getEyePosition().subtract(player.getEyePosition());
            player.setYRot((float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90));
            player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
        }

        void resetKnowledge(boolean all) {
            knowledge.reset(); knowledge.setInitializedAspects(true);
            for (var aspect : Aspect.aspects.values()) if (all || aspect.isPrimal()) knowledge.addDiscoveredAspect(aspect.getTag());
            before = knowledge.save();
        }

        void begin() {
            began = level.getGameTime(); started = true;
            test.assertTrue(player.gameMode.useItem(player, level, tool, InteractionHand.MAIN_HAND).consumesAction()
                            && tool.has(ThaumcraftContent.SCAN_SESSION.get()),
                    "real survival thaumometer starts a physical modern-entity scan");
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            player.stopUsingItem(); frog.discard();
            level.getServer().getPlayerList().remove(player);
            channel.finishAndReleaseAll();
            if (ownedForce) level.setChunkForced(chunk.x, chunk.z, false);
        }
    }
}
