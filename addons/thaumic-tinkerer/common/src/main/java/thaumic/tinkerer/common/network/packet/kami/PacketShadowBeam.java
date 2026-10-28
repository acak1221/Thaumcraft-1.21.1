/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.network.packet.kami;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * The path of a Shadowbeam (server to client): the first visible point, the points where the beam bounced off a wall and
 * the end. 1.7.10 spawned a dark sparkle at every step of the beam on the side that ran the beam; the client draws the
 * path from these points.
 */
public record PacketShadowBeam(List<Vec3> points) implements CustomPacketPayload {
    public static final int MAX_POINTS = 96;
    public static final Type<PacketShadowBeam> TYPE = new Type<>(LibMisc.id("shadow_beam"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketShadowBeam> CODEC = StreamCodec.of(
            (buf, packet) -> {
                int size = Math.min(packet.points.size(), MAX_POINTS);
                buf.writeVarInt(size);
                for (int i = 0; i < size; i++) {
                    Vec3 point = packet.points.get(i);
                    buf.writeDouble(point.x);
                    buf.writeDouble(point.y);
                    buf.writeDouble(point.z);
                }
            }, buf -> {
                int size = Math.min(buf.readVarInt(), MAX_POINTS);
                List<Vec3> points = new ArrayList<>(size);
                for (int i = 0; i < size; i++) points.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
                return new PacketShadowBeam(points);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
