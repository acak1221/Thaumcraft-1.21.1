package thaumcraft.fabric.clienttest;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.FamilyVariant;
import thaumcraft.common.lib.world.OuterLandsDimension;
import thaumcraft.common.tiles.TileEldritchObelisk;

/** Deliberately supplied visual fixture, never evidence of survival acquisition. */
final class EldritchVisualClientSequence {
    private int step, ticks;
    private CompletableFuture<Void> action;
    private static final BlockPos OBELISK = new BlockPos(8, 161, 8);

    boolean tick(Minecraft client, Consumer<String> capture) {
        if (++ticks > 1200) throw new IllegalStateException("Obelisk visual timeout at " + step);
        switch (step) {
            case 0 -> {
                client.setScreen(null);
                client.options.setCameraType(CameraType.FIRST_PERSON);
                action = onServer(client, player -> {
                    var outer = OuterLandsDimension.level(player.server);
                    if (outer == null) throw new IllegalStateException("Outer Lands absent");
                    player.setGameMode(GameType.CREATIVE);
                    player.setInvulnerable(true);
                    for (BlockPos pos : BlockPos.betweenClosed(0, 160, 0, 20, 167, 20))
                        outer.setBlockAndUpdate(pos, pos.getY() == 160 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                    outer.setBlockAndUpdate(OBELISK, FamilyVariant.with(ThaumcraftEldritchBlocks.ELDRITCH.get().defaultBlockState(), 1));
                    // Same five-block tower as the native Outer Lands arena fixture.
                    for (int y = 1; y <= 4; y++)
                        outer.setBlockAndUpdate(OBELISK.above(y), FamilyVariant.with(ThaumcraftEldritchBlocks.ELDRITCH.get().defaultBlockState(), 2));
                    if (!(outer.getBlockEntity(OBELISK) instanceof TileEldritchObelisk))
                        throw new IllegalStateException("Fixture did not create the real obelisk tile");
                    player.teleportTo(outer, 14.5, 161, 16.5, 0, 0);
                });
                next();
            }
            case 1 -> {
                if (!action.isDone()) return false;
                action.join();
                if (!client.level.dimension().location().getPath().equals("outer_lands")
                        || !(client.level.getBlockEntity(OBELISK) instanceof TileEldritchObelisk) || ticks < 100) return false;
                aim(client, new Vec3(8.5, 163, 8.5));
                next();
            }
            case 2 -> {
                if (ticks < 40) return false;
                next();
                capture.accept("201-outer-lands-obelisk-tunnel-wide-en.png");
            }
            case 3 -> {
                action = onServer(client, player -> player.teleportTo(player.serverLevel(), 11.5, 161, 12.5, 0, 0));
                next();
            }
            case 4 -> {
                if (!action.isDone()) return false;
                action.join();
                if (ticks < 60) return false;
                aim(client, new Vec3(8.5, 163, 8.5));
                next();
            }
            case 5 -> {
                if (ticks < 40) return false;
                next();
                capture.accept("202-outer-lands-obelisk-tunnel-close-en.png");
            }
            default -> { return true; }
        }
        return false;
    }

    private void next() { step++; ticks = 0; }

    static CompletableFuture<Void> onServer(Minecraft client, Consumer<ServerPlayer> operation) {
        var server = client.getSingleplayerServer();
        var uuid = client.player.getUUID();
        return CompletableFuture.runAsync(() -> {
            var player = server.getPlayerList().getPlayer(uuid);
            if (player == null) throw new IllegalStateException("Missing visual fixture player");
            operation.accept(player);
            player.inventoryMenu.broadcastChanges();
        }, server);
    }

    static void aim(Minecraft client, Vec3 target) {
        Vec3 delta = target.subtract(client.player.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        client.player.setYRot(yaw);
        client.player.setXRot(pitch);
        client.player.yRotO = yaw;
        client.player.xRotO = pitch;
        client.player.yHeadRot = yaw;
        client.player.yBodyRot = yaw;
    }
}
