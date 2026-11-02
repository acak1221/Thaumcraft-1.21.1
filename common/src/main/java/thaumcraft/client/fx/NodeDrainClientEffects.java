package thaumcraft.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.event.events.client.ClientTickEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.network.NodeDrainFxPayload;
import thaumcraft.common.tiles.TileNode;

/** Original TileNodeRenderer:200-223 wispy siphon; paid server pulses renew a short visual lease. */
public final class NodeDrainClientEffects {
    public static final int LIFETIME = 8;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/wispy.png");
    private static final Map<BlockPos, Drain> DRAINS = new LinkedHashMap<>();
    private static boolean initialized;
    private static long received, rendered;
    private static BlockPos lastNode;
    private NodeDrainClientEffects() {}

    private static final class Drain {
        final int actorId;
        final UUID actorUuid;
        int target, color = 0xffffff, ticks = LIFETIME;
        Drain(NodeDrainFxPayload payload) { actorId = payload.actorId(); actorUuid = payload.actorUuid(); target = payload.rgb(); }
    }

    public record Diagnostics(long received, long rendered, int active, BlockPos lastNode) {}
    public static Diagnostics diagnostics() { return new Diagnostics(received, rendered, DRAINS.size(), lastNode); }

    public static void init() {
        if (initialized) return;
        initialized = true;
        dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level -> clear());
        dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> clear());
        ClientTickEvent.CLIENT_POST.register(NodeDrainClientEffects::tick);
    }

    public static void accept(NodeDrainFxPayload payload) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(payload.dimension())
                || !(level.getBlockEntity(payload.node()) instanceof TileNode)) return;
        Drain previous = DRAINS.get(payload.node());
        if (!payload.active()) {
            if (previous != null && previous.actorUuid.equals(payload.actorUuid())) DRAINS.remove(payload.node());
            return;
        }
        DRAINS.entrySet().removeIf(entry -> !entry.getKey().equals(payload.node()) && entry.getValue().actorUuid.equals(payload.actorUuid()));
        if (previous == null || previous.actorId != payload.actorId() || !previous.actorUuid.equals(payload.actorUuid()))
            DRAINS.put(payload.node(), new Drain(payload));
        else { previous.target = payload.rgb(); previous.ticks = LIFETIME; }
        received++; lastNode = payload.node();
    }

    private static void tick(Minecraft client) {
        if (client.level == null) { clear(); return; }
        var iterator = DRAINS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next(); Drain drain = entry.getValue();
            if (--drain.ticks <= 0 || !client.level.hasChunkAt(entry.getKey())
                    || !(client.level.getBlockEntity(entry.getKey()) instanceof TileNode)
                    || !(client.level.getEntity(drain.actorId) instanceof Player actor)
                    || !actor.getUUID().equals(drain.actorUuid) || !actor.isAlive() || !usingWand(actor)) {
                iterator.remove(); continue;
            }
            // Original TileNode.onUsingWandTick:441-448, integer RGB easing once per use tick.
            drain.color = smoothColor(drain.color, drain.target);
        }
    }

    private static boolean usingWand(Player actor) { return actor.isUsingItem() && actor.getUseItem().getItem() instanceof ItemWandCasting; }

    public static int smoothColor(int current, int target) {
        int result = 0;
        for (int shift : new int[]{0, 8, 16})
            result |= ((((target >> shift) & 255) + 4 * ((current >> shift) & 255)) / 5) << shift;
        return result;
    }

    /** Called before the BER's revealed-node early return: a siphon never requires goggles. */
    public static void render(TileNode node, float partial, PoseStack pose, MultiBufferSource buffers) {
        var client = Minecraft.getInstance(); var level = client.level;
        Drain drain = DRAINS.get(node.getBlockPos());
        if (level == null || node.getLevel() != level || drain == null || drain.ticks <= 0
                || !(level.getEntity(drain.actorId) instanceof Player actor) || !actor.getUUID().equals(drain.actorUuid)
                || !usingWand(actor) || actor.distanceToSqr(Vec3.atCenterOf(node.getBlockPos())) > 8 * 8) return;
        int used = actor.getTicksUsingItem();
        Vec3 hand = handAnchor(actor, partial, used);
        BlockPos origin = node.getBlockPos();
        FloatyLine.draw(pose, buffers, hand.x - origin.getX(), hand.y - origin.getY(), hand.z - origin.getZ(),
                .5, .5, .5, hand.x, hand.y, hand.z, drain.color, TEXTURE, -.02F,
                Math.min(used, 10) / 10.0F, FloatyLine.DEFAULT_WIDTH, FloatyLine.Flavour.LIBRARY);
        rendered++;
    }

    /** Original hand ray and angular bob, translated to native interpolated player coordinates. */
    private static Vec3 handAnchor(Player actor, float partial, int used) {
        float wave = Mth.sin(used / 10.0F) * 10;
        Vec3 offset = new Vec3(-.1, -.1, .5)
                .xRot(-Mth.lerp(partial, actor.xRotO, actor.getXRot()) * Mth.DEG_TO_RAD)
                .yRot(-Mth.rotLerp(partial, actor.yRotO, actor.getYRot()) * Mth.DEG_TO_RAD)
                .yRot(-wave * .01F).xRot(-wave * .015F);
        // TC4 used the camera-relative local player's base; modern world coordinates need eye Y in both views.
        return actor.getPosition(partial).add(offset).add(0, actor.getEyeHeight(), 0);
    }

    public static void clear() { DRAINS.clear(); received = rendered = 0; lastNode = null; }
}
