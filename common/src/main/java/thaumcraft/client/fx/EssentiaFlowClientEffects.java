package thaumcraft.client.fx;

import dev.architectury.event.events.client.ClientTickEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.fx.particles.EssentiaTrailParticle;
import thaumcraft.common.lib.network.EssentiaFlowFxPayload;
import thaumcraft.common.tiles.TileInfusionMatrix;

/**
 * The client half of the original essence flow packet.
 *
 * <p>Oracle {@code thaumcraft/common/lib/network/fx/PacketFXEssentiaSource.java:64-78}: the packet puts the
 * flow into a map with the key
 * {@code x + ":" + y + ":" + z + ":" + tx + ":" + ty + ":" + tz + ":" + color} and a lifetime of fifteen
 * ticks. A packet that arrives for a flow that is already running resets that flow to fifteen ticks.</p>
 *
 * <p>Oracle {@code thaumcraft/client/lib/ClientTickEventsFML.java:249-269}: every client tick draws the
 * trail of every running flow. The trail starts at the SOURCE and flies to the CONSUMER. The destination of
 * an infusion matrix is one block lower, the trail shrinks over the last five ticks with the scale
 * {@code ticks * ticks / 25} and the phase of the mote counts back from the client tick.</p>
 */
public final class EssentiaFlowClientEffects {
    /** The lifetime of a flow of the original, in client ticks. */
    public static final int LIFETIME = 15;
    /** The number of ticks the flow keeps its full size. */
    public static final int FULL_SIZE_TICKS = 5;
    private static final Map<String, Flow> FLOWS = new LinkedHashMap<>();
    private static int tickCount;
    private static boolean initialized;
    private EssentiaFlowClientEffects() {}

    /** One running flow: the consumer, the source, the colour and the remaining ticks. */
    private static final class Flow {
        private final BlockPos consumer;
        private final BlockPos source;
        private final int rgb;
        private int ticks;
        private Flow(BlockPos consumer, BlockPos source, int rgb, int ticks) {
            this.consumer = consumer; this.source = source; this.rgb = rgb; this.ticks = ticks;
        }
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level -> clear());
        dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> clear());
        ClientTickEvent.CLIENT_POST.register(EssentiaFlowClientEffects::tick);
    }

    /**
     * The key of the original flow map.
     *
     * <p>Oracle {@code PacketFXEssentiaSource.java:68}:
     * {@code message.x + ":" + message.y + ":" + message.z + ":" + tx + ":" + ty + ":" + tz + ":" + message.color}.</p>
     */
    public static String key(int x, int y, int z, int sourceX, int sourceY, int sourceZ, int rgb) {
        return x + ":" + y + ":" + z + ":" + sourceX + ":" + sourceY + ":" + sourceZ + ":" + rgb;
    }

    /**
     * The size of the trail of a flow that has {@code ticks} ticks left.
     *
     * <p>Oracle {@code ClientTickEventsFML.java:265-266}: the trail keeps its full size while more than five
     * ticks are left and shrinks with {@code (ticks * ticks) / 25} over the last five ticks.</p>
     */
    public static float scale(int ticks) {
        return ticks > FULL_SIZE_TICKS ? 1.0F : (float) (ticks * ticks) / 25.0F;
    }

    /**
     * The phase of the mote of a flow that has {@code ticks} ticks left.
     *
     * <p>Oracle {@code ClientTickEventsFML.java:263-266}: the phase is the client tick while the trail is
     * full and counts back by the missing ticks of the shrinking tail.</p>
     */
    public static int phase(int tick, int ticks) {
        return ticks > FULL_SIZE_TICKS ? tick : tick - (FULL_SIZE_TICKS - ticks);
    }

    /**
     * The vertical offset of the destination of a flow.
     *
     * <p>Oracle {@code ClientTickEventsFML.java:258-261}: the consumer is one block lower when it is an
     * infusion matrix.</p>
     */
    public static int destinationOffset(boolean infusionMatrix) {
        return infusionMatrix ? -1 : 0;
    }

    /** The original answer of the packet: the flow starts or restarts its fifteen ticks. */
    public static void accept(EssentiaFlowFxPayload payload) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || !level.dimension().location().equals(payload.dimension())) return;
        BlockPos consumer = payload.consumer();
        BlockPos source = payload.source();
        if (!level.hasChunkAt(consumer) || !level.hasChunkAt(source)) return;
        if (consumer.distSqr(source) > 64 * 64) return;
        String key = key(consumer.getX(), consumer.getY(), consumer.getZ(),
                source.getX(), source.getY(), source.getZ(), payload.rgb());
        Flow flow = FLOWS.get(key);
        // Oracle PacketFXEssentiaSource:69-76 - a running flow is reset to its
        // fifteen ticks, a new flow starts with fifteen ticks.
        if (flow != null) flow.ticks = LIFETIME;
        else FLOWS.put(key, new Flow(consumer, source, payload.rgb(), LIFETIME));
    }

    /** The original client tick of the flow map. */
    public static void tick(Minecraft client) {
        ++tickCount;
        if (client.level == null) {
            FLOWS.clear();
            return;
        }
        ClientLevel level = client.level;
        var iterator = FLOWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Flow> entry = iterator.next();
            Flow flow = entry.getValue();
            if (flow.ticks <= 0) {
                iterator.remove();
                continue;
            }
            if (!level.hasChunkAt(flow.consumer) || !level.hasChunkAt(flow.source)) continue;
            int offset = destinationOffset(level.getBlockEntity(flow.consumer) instanceof TileInfusionMatrix);
            Vec3 from = Vec3.atCenterOf(flow.source);
            Vec3 to = new Vec3(flow.consumer.getX() + .5D, flow.consumer.getY() + .5D + offset,
                    flow.consumer.getZ() + .5D);
            int count = phase(tickCount, flow.ticks);
            float size = scale(flow.ticks);
            client.particleEngine.add(new EssentiaTrailParticle(level, flow.source, from, to, flow.rgb, count, size));
            --flow.ticks;
        }
    }

    /** The number of running flows, for the diagnostics of the client. */
    public static int runningFlows() { return FLOWS.size(); }

    /** The level change and the disconnect drop every running flow. */
    public static void clear() {
        FLOWS.clear();
        tickCount = 0;
    }
}
