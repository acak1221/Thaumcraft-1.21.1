package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftFX;
import thaumcraft.api.client.ThaumcraftClientFX;
import thaumcraft.common.lib.network.ClientFxPayload;

/**
 * The server twins of the client effect facade ({@code ThaumcraftFX.sparkle}, {@code burst}, {@code blockRunes},
 * {@code smokeSpiral}) and the facade itself on a dedicated server, in a running world.
 *
 * <p>What a client draws when the payload arrives needs a client and is not checked here (see the notes in
 * {@code docs/api/guide/world-and-content.md}); this proves the server half: the arguments are checked, the sender refuses another
 * thread, a player far away is not sent anything, and the client facade is a harmless no-op when called with a server level.</p>
 */
public final class ClientFxApiRuntimeChecks {
    private ClientFxApiRuntimeChecks() {}

    public static void serverTwinsCheckAndSendOnTheServerThread(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(1, 2, 1));
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;

        // Two players in the level, one inside the 32 blocks of the effect and one far outside it. Their connections are
        // replaced by recorders, so what the sender really put on the wire is read back.
        ServerPlayer near = null, far = null;
        try {
            near = test.makeMockServerPlayerInLevel();
            near.setPos(x + 2, y, z);
            far = test.makeMockServerPlayerInLevel();
            far.setPos(x + 200, y, z);
            test.assertTrue(level.players().contains(near) && level.players().contains(far), "both mock players are visited by the sender");
            List<ClientFxPayload> heardNear = record(test, near), heardFar = record(test, far);

            ThaumcraftFX.sparkle(level, x, y, z, 1.5F, 4, -0.02F);
            ThaumcraftFX.burst(level, x, y + 1, z, 0.5F);
            ThaumcraftFX.blockRunes(level, x, y + 2, z, 0.2F, 0.0F, 0.8F, 20, -0.02F);
            ThaumcraftFX.smokeSpiral(level, x, y + 3, z, 2.0F, 90, pos.getY(), 0xFF09E2FF);

            test.assertTrue(heardFar.isEmpty(), "a player 200 blocks away is sent nothing: " + heardFar);
            test.assertTrue(heardNear.size() == 4, "the player in range is sent the four effects: " + heardNear);
            var dimension = level.dimension().location();
            ClientFxPayload sparkle = heardNear.get(0), burst = heardNear.get(1), runes = heardNear.get(2), spiral = heardNear.get(3);
            test.assertTrue(sparkle.kind() == ClientFxPayload.SPARKLE && sparkle.dimension().equals(dimension) && sparkle.x() == x && sparkle.y() == y && sparkle.z() == z
                    && sparkle.a() == 1.5F && sparkle.i() == 4 && sparkle.b() == -0.02F, "the sparkle carries what was asked: " + sparkle);
            test.assertTrue(burst.kind() == ClientFxPayload.BURST && burst.y() == y + 1 && burst.a() == 0.5F, "the burst carries what was asked: " + burst);
            test.assertTrue(runes.kind() == ClientFxPayload.RUNES && runes.y() == y + 2 && runes.a() == 0.2F && runes.b() == 0.0F && runes.c() == 0.8F
                    && runes.i() == 20 && runes.d() == -0.02F, "the runes carry what was asked: " + runes);
            test.assertTrue(spiral.kind() == ClientFxPayload.SMOKE_SPIRAL && spiral.a() == 2.0F && spiral.i() == 90 && spiral.j() == pos.getY()
                    && spiral.k() == 0x09E2FF, "the spiral carries what was asked, with the colour cut to 24 bits: " + spiral);

            // A player who walks into range hears the next effect; one who leaves it no longer does.
            far.setPos(x - 31, y, z);
            ThaumcraftFX.burst(level, x, y, z, 1.0F);
            test.assertTrue(heardFar.size() == 1, "31 blocks away is in range: " + heardFar);
            far.setPos(x - 33, y, z);
            ThaumcraftFX.burst(level, x, y, z, 1.0F);
            test.assertTrue(heardFar.size() == 1, "33 blocks away is out of range: " + heardFar);
            heardNear.clear();

            expectRefused(test, () -> ThaumcraftFX.sparkle(level, Double.NaN, y, z, 1.0F, 2, 0.0F), IllegalArgumentException.class, "a NaN position");
            expectRefused(test, () -> ThaumcraftFX.sparkle(level, x, y, z, 1.0F, 8, 0.0F), IllegalArgumentException.class, "sparkle type 8");
            expectRefused(test, () -> ThaumcraftFX.sparkle(level, x, y, z, 0.0F, 2, 0.0F), IllegalArgumentException.class, "a size of zero");
            expectRefused(test, () -> ThaumcraftFX.burst(level, x, y, z, -1.0F), IllegalArgumentException.class, "a negative burst");
            expectRefused(test, () -> ThaumcraftFX.blockRunes(level, x, y, z, 1.0F, 1.0F, 1.0F, 0, 0.0F), IllegalArgumentException.class, "a rune of no duration");
            expectRefused(test, () -> ThaumcraftFX.smokeSpiral(level, x, y, z, 100.0F, 0, 0, 0), IllegalArgumentException.class, "a spiral wider than 64");

            for (Runnable other : new Runnable[]{
                    () -> ThaumcraftFX.sparkle(level, x, y, z, 1.0F, 2, 0.0F),
                    () -> ThaumcraftFX.burst(level, x, y, z, 1.0F),
                    () -> ThaumcraftFX.blockRunes(level, x, y, z, 0.2F, 0.0F, 0.8F, 20, -0.02F),
                    () -> ThaumcraftFX.smokeSpiral(level, x, y, z, 2.0F, 90, 0, 0x9E2FF)}) {
                boolean refused = false;
                try {
                    CompletableFuture.runAsync(other).join();
                } catch (CompletionException expected) {
                    refused = expected.getCause() instanceof IllegalStateException;
                }
                test.assertTrue(refused, "another thread is refused, as the facade documents");
            }

            // The client facade on a dedicated server: a server level draws nothing and fails nothing, on any thread, and the
            // arguments are still checked.
            ThaumcraftClientFX.sparkle(level, x, y, z, 1.0F, 2, 0.0F);
            ThaumcraftClientFX.sparkle(level, x, y, z, 3);
            ThaumcraftClientFX.burst(level, x, y, z, 1.0F);
            ThaumcraftClientFX.wisp(level, x, y, z, 0.1F, 0.5F, 0.5F, 0.5F);
            ThaumcraftClientFX.wisp(level, x, y, z, 0.1F, 4, true, true, -0.02F);
            ThaumcraftClientFX.wisp(level, x, y, z, x, y + 3, z, 0.1F, 4, true, 0.0F);
            ThaumcraftClientFX.blockRunes(level, x, y, z, 0.5F, 0.0F, 0.5F, 20, -0.02F);
            ThaumcraftClientFX.smokeSpiral(level, x, y, z, 2.0F, 90, pos.getY(), 0x9E2FF);
            ThaumcraftClientFX.essentiaTrail(level, pos, pos.above(3), 7, 0xFFAA00, 0.7F);
            ThaumcraftClientFX.beam(level, near, Vec3.atCenterOf(pos), Vec3.atCenterOf(pos.above(4)), 0xFF0000, 2, true, 0.0F, 1);
            ThaumcraftClientFX.beam(level, near, Vec3.atCenterOf(pos.above(4)), 0xFF0000, 2, true, 0.0F, 1);
            // The ranges of both beam forms, with a real owner: they are refused on a server too, and the limits themselves pass.
            final ServerPlayer owner = near;
            final Vec3 start = Vec3.atCenterOf(pos), end = Vec3.atCenterOf(pos.above(4));
            final Vec3 nanEnd = new Vec3(end.x, end.y, Double.NaN), nanStart = new Vec3(Double.NaN, start.y, start.z);
            expectRefused(test, () -> ThaumcraftClientFX.beam(level, owner, start, end, 0xFF0000, 4, true, 0.0F, 1), IllegalArgumentException.class, "beam texture 4");
            expectRefused(test, () -> ThaumcraftClientFX.beam(level, owner, start, end, 0xFF0000, -1, true, 0.0F, 1), IllegalArgumentException.class, "beam texture -1");
            expectRefused(test, () -> ThaumcraftClientFX.beam(level, owner, start, end, 0xFF0000, 2, true, 8.5F, 1), IllegalArgumentException.class, "beam end scale 8.5");
            expectRefused(test, () -> ThaumcraftClientFX.beam(level, owner, start, end, 0xFF0000, 2, true, Float.NaN, 1), IllegalArgumentException.class, "beam end scale NaN");
            expectRefused(test, () -> ThaumcraftClientFX.beam(level, owner, start, end, 0xFF0000, 2, true, 1.0F, 6), IllegalArgumentException.class, "beam impact 6");
            expectRefused(test, () -> ThaumcraftClientFX.beam(level, owner, start, nanEnd, 0xFF0000, 2, true, 1.0F, 1), IllegalArgumentException.class, "beam end with a NaN");
            expectRefused(test, () -> ThaumcraftClientFX.beam(level, owner, nanStart, end, 0xFF0000, 2, true, 1.0F, 1), IllegalArgumentException.class, "beam start with a NaN");
            expectRefused(test, () -> ThaumcraftClientFX.beam(level, owner, end, 0xFF0000, 4, true, 0.0F, 1), IllegalArgumentException.class, "wand beam texture 4");
            expectRefused(test, () -> ThaumcraftClientFX.beam(level, owner, end, 0xFF0000, 2, true, 8.5F, 1), IllegalArgumentException.class, "wand beam end scale 8.5");
            expectRefused(test, () -> ThaumcraftClientFX.beam(level, owner, end, 0xFF0000, 2, true, 1.0F, 6), IllegalArgumentException.class, "wand beam impact 6");
            expectRefused(test, () -> ThaumcraftClientFX.beam(level, owner, nanEnd, 0xFF0000, 2, true, 1.0F, 1), IllegalArgumentException.class, "wand beam end with a NaN");
            ThaumcraftClientFX.beam(level, owner, start, end, 0xFF0000, 3, true, 8.0F, 5);
            ThaumcraftClientFX.beam(level, owner, start, end, 0xFF0000, 0, false, 0.0F, 0);
            ThaumcraftClientFX.beam(level, owner, end, 0xFF0000, 3, true, 8.0F, 5);
            test.assertTrue(ThaumcraftClientFX.wandTip(near).distanceTo(near.getEyePosition()) < 0.4, "the wand tip is within a third of a block of the eye");
            wandTipFollowsTheYawAndTheLook(test, near);
            test.assertTrue(ThaumcraftClientFX.particleCount(level, 5) == 0, "a server level has no particles");
            CompletableFuture.runAsync(() -> ThaumcraftClientFX.sparkle(level, x, y, z, 1.0F, 2, 0.0F)).join();
            expectRefused(test, () -> ThaumcraftClientFX.sparkle(level, x, y, z, 1.0F, 9, 0.0F), IllegalArgumentException.class, "the client facade checks its arguments on a server too");
            test.succeed();
        } finally {
            // The mock players joined the player list; leave it as the test found it.
            for (ServerPlayer player : new ServerPlayer[]{near, far})
                if (player != null && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
        }
    }

    /**
     * The tip of the wand, {@code wandTip(Player)}, against the documented formula with a yaw and a pitch chosen so that a wrong
     * mapping shows: the eye is moved {@code 0.066} against the cosine and {@code 0.04} against the sine of the yaw (the yaw of the
     * player, in radians), {@code 0.06} down and {@code 0.3} along the look vector. The expected point is built from the numbers
     * below, not from the code under test; a yaw of 90 and a pitch of 30 make the pitch (30) read as a yaw, the look and the
     * offset swapped, or a missing term all land more than a hundredth of a block away.
     */
    private static void wandTipFollowsTheYawAndTheLook(GameTestHelper test, ServerPlayer player) {
        float yaw = 90.0F, pitch = 30.0F;
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(pitch);
        // Standing: the look vector of the game for these angles, from first principles.
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        Vec3 look = new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
        test.assertTrue(player.getLookAngle().distanceTo(look) < 1.0E-4, "the mock player looks where the angles say: " + player.getLookAngle() + " against " + look);
        Vec3 eye = player.getEyePosition();
        Vec3 expected = eye.add(-Math.cos(y) * 0.066, -0.06, -Math.sin(y) * 0.04).add(look.scale(0.3));
        Vec3 tip = ThaumcraftClientFX.wandTip(player);
        test.assertTrue(tip.distanceTo(expected) < 1.0E-3, "the wand tip is " + tip + " but the formula gives " + expected);
        // The literal offset for this yaw and pitch, (-0.2598, -0.21, -0.04) from the eye, so the numbers are visible in a failure.
        test.assertTrue(Math.abs((tip.x - eye.x) - (-0.3 * Math.cos(p))) < 1.0E-3 && Math.abs((tip.y - eye.y) - (-0.06 - 0.3 * Math.sin(p))) < 1.0E-3
                && Math.abs((tip.z - eye.z) - (-0.04)) < 1.0E-3, "the wand tip is offset " + tip.subtract(eye) + " from the eye");
        // Another yaw moves the sideways offset: the yaw of the player is read, not the pitch.
        player.setYRot(0.0F);
        player.setYHeadRot(0.0F);
        player.setXRot(0.0F);
        Vec3 south = ThaumcraftClientFX.wandTip(player).subtract(player.getEyePosition());
        test.assertTrue(Math.abs(south.x - (-0.066)) < 1.0E-3 && Math.abs(south.y - (-0.06)) < 1.0E-3 && Math.abs(south.z - 0.3) < 1.0E-3,
                "looking south the tip is (-0.066, -0.06, 0.3) from the eye but " + south);
    }

    /** Replaces the connection listener of a mock player by one that keeps the client effect payloads it is asked to send. */
    private static List<ClientFxPayload> record(GameTestHelper test, ServerPlayer player) {
        List<ClientFxPayload> heard = new ArrayList<>();
        try {
            Field field = ServerCommonPacketListenerImpl.class.getDeclaredField("connection");
            field.setAccessible(true);
            Connection connection = (Connection) field.get(player.connection);
            player.connection = new ServerGamePacketListenerImpl(test.getLevel().getServer(), connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) { note(packet); }
                @Override public void send(Packet<?> packet, PacketSendListener listener) { note(packet); }
                private void note(Packet<?> packet) {
                    if (!(packet instanceof ClientboundCustomPayloadPacket custom) || !custom.payload().type().id().equals(ClientFxPayload.TYPE.id())) return;
                    try {
                        if (custom.payload() instanceof ClientFxPayload fx) { heard.add(fx); return; }
                        // Architectury carries a registered payload as the bytes its codec wrote; read them back the way a client does.
                        byte[] bytes = (byte[]) custom.payload().getClass().getMethod("payload").invoke(custom.payload());
                        var wire = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), test.getLevel().registryAccess());
                        heard.add(ClientFxPayload.STREAM_CODEC.decode(wire));
                        if (wire.readableBytes() != 0) throw new AssertionError("the payload was not read to its end");
                    } catch (ReflectiveOperationException e) {
                        throw new AssertionError("cannot read the payload Architectury sent", e);
                    }
                }
            };
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot reach the connection of the mock player", e);
        }
        return heard;
    }

    private static void expectRefused(GameTestHelper test, Runnable call, Class<? extends RuntimeException> kind, String what) {
        boolean refused = false;
        try {
            call.run();
        } catch (RuntimeException e) {
            refused = kind.isInstance(e);
        }
        test.assertTrue(refused, what + " is refused with " + kind.getSimpleName());
    }
}
