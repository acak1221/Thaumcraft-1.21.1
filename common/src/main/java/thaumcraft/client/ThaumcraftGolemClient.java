package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import thaumcraft.client.gui.GuiGolem;
import thaumcraft.client.gui.GuiTravelingTrunk;
import thaumcraft.client.renderers.entity.RenderGolemBase;
import thaumcraft.client.renderers.entity.RenderGolemBobber;
import thaumcraft.client.renderers.entity.RenderTravelingTrunk;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.ItemGolemBell;

/** Client-only bindings; root owns common registration and the bootstrap call. */
public final class ThaumcraftGolemClient {
    public static final ResourceLocation HAS_CORE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "has_core");
    private static boolean initialized;
    private ThaumcraftGolemClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        EntityRendererRegistry.register(ThaumcraftGolems.GOLEM, RenderGolemBase::new);
        EntityRendererRegistry.register(ThaumcraftGolems.GOLEM_BOBBER, RenderGolemBobber::new);
        EntityRendererRegistry.register(ThaumcraftGolems.TRUNK, RenderTravelingTrunk::new);
        MenuRegistry.registerScreenFactory(ThaumcraftGolems.GOLEM_MENU.get(), GuiGolem::new);
        MenuRegistry.registerScreenFactory(ThaumcraftGolems.TRUNK_MENU.get(), GuiTravelingTrunk::new);
        for (var placer : ThaumcraftGolems.placerItems()) ItemPropertiesRegistry.register(placer, HAS_CORE,
                (stack, level, entity, seed) -> stack.getOrDefault(ThaumcraftGolems.GOLEM_DATA.get(), GolemData.EMPTY).core() >= 0 ? 1F : 0F);
    }

    /** Root calls this at Fabric LAST with the un-translated world-view pose.
     * Camera subtraction happens here; buffer flushing belongs to root.
     * Static marker positions need no partial-tick interpolation or world scan. */
    public static void renderMarkers(PoseStack pose, MultiBufferSource buffers, float partialTick, Camera camera) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null || !player.isAlive() || pose == null || buffers == null || camera == null) return;
        var bell = player.getMainHandItem();
        if (!(bell.getItem() instanceof ItemGolemBell)) {
            bell = player.getOffhandItem();
            if (!(bell.getItem() instanceof ItemGolemBell)) return;
        }
        // Main-hand bell wins even if its selected marker snapshot is empty.
        var markers = ItemGolemBell.selectedMarkers(bell);
        if (markers.isEmpty()) return;
        var dimension = level.dimension().location();
        var eye = camera.getPosition();
        var lines = buffers.getBuffer(RenderType.lines());
        for (GolemMarker marker : markers) {
            // hasChunkAt is only a loaded-status check; never fetch blocks,
            // inventories or chunks merely to decorate a saved marker.
            if (!dimension.equals(marker.dimension()) || !level.hasChunkAt(marker.pos())) continue;
            var pos = marker.pos();
            pose.pushPose();
            try {
                // Subtract in double precision before native float vertices.
                pose.translate(pos.getX() - eye.x, pos.getY() - eye.y, pos.getZ() - eye.z);
                LevelRenderer.renderLineBox(pose, lines, new AABB(0, 0, 0, 1, 1, 1).inflate(.002), .3F, .8F, 1F, .6F);
                LevelRenderer.renderLineBox(pose, lines, markerFace(marker.face()), 1F, .85F, .25F, 1F);
            } finally { pose.popPose(); }
        }
    }

    /** Thin outline outside the clicked unit-block face; all six directions. */
    private static AABB markerFace(Direction face) {
        double offset = .008, halfThickness = .001;
        return switch (face) {
            case DOWN -> new AABB(0, -offset - halfThickness, 0, 1, -offset + halfThickness, 1);
            case UP -> new AABB(0, 1 + offset - halfThickness, 0, 1, 1 + offset + halfThickness, 1);
            case NORTH -> new AABB(0, 0, -offset - halfThickness, 1, 1, -offset + halfThickness);
            case SOUTH -> new AABB(0, 0, 1 + offset - halfThickness, 1, 1, 1 + offset + halfThickness);
            case WEST -> new AABB(-offset - halfThickness, 0, 0, -offset + halfThickness, 1, 1);
            case EAST -> new AABB(1 + offset - halfThickness, 0, 0, 1 + offset + halfThickness, 1, 1);
        };
    }
}
