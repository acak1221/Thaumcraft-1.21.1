package thaumcraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import thaumcraft.common.tiles.TileArcaneBore;

/** Bore-only drawing. Geometry reads a current CLIENT tile and loaded collision
 * cells; no particle/target request, crack stage, harvest witness or server write.
 * Exact original effect pulse/lifetime/debris/impact/audio parity is NOT claimed. */
public final class BoreClientEffects {
    public static final RenderType SHELL = new BoreType("shell", texture("models/bore"), Material.SHELL);
    public static final RenderType GLASS = new BoreType("glass", texture("models/jar"), Material.GLASS);
    private static final RenderType VORTEX = new BoreType("vortex", texture("misc/vortex"), Material.VORTEX);
    private static final RenderType GREEN = new BoreType("beam1", texture("misc/beam1"), Material.BEAM);
    private static final RenderType ORANGE = new BoreType("beam2", texture("misc/beam2"), Material.BEAM);
    private static final Vec3 CENTER = new Vec3(.5, .5, .5);
    private BoreClientEffects() {}

    private static ResourceLocation texture(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + path + ".png"); }

    public static void finish(MultiBufferSource buffers, RenderType type) {
        if (buffers instanceof MultiBufferSource.BufferSource nativeBuffers) nativeBuffers.endBatch(type);
    }

    /** Three literal UtilsFX349–375 quads under the caller's original yaw/tilt
     * pose. Widths are .4/.3/.2, not twice those values; UV order is preserved. */
    public static void vortices(PoseStack pose, MultiBufferSource buffers, int cameraTicks, float partialTick) {
        float spin = (cameraTicks % 45 + partialTick) * 8;
        vortex(pose, buffers, -.17F, -spin, 10, .4F, 1);
        vortex(pose, buffers, -.21F, spin, 10, .3F, .8F);
        vortex(pose, buffers, -.25F, -spin, -10, .2F, .8F);
        finish(buffers, VORTEX);
    }
    private static void vortex(PoseStack pose, MultiBufferSource buffers, float y, float spin, float lean, float width, float alpha) {
        pose.pushPose();
        try {
            pose.translate(0, y, 0); pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.mulPose(Axis.ZP.rotationDegrees(spin)); pose.mulPose(Axis.YP.rotationDegrees(lean)); pose.scale(width, width, width);
            var out = buffers.getBuffer(VORTEX);
            vertex(out, pose, new Vec3(-.5, .5, 0), 0, 1, 0xFFFFFF, alpha);
            vertex(out, pose, new Vec3(.5, .5, 0), 1, 1, 0xFFFFFF, alpha);
            vertex(out, pose, new Vec3(.5, -.5, 0), 1, 0, 0xFFFFFF, alpha);
            vertex(out, pose, new Vec3(-.5, -.5, 0), 0, 0, 0xFFFFFF, alpha);
        } finally { pose.popPose(); }
    }

    /** Minimal actual aiming beam: original two colours/textures/reverse flags,
     * three rotating60-degree planes per layer and actual client collision end.
     * A cosmetic collision is NEVER promoted to a protected harvest success. */
    public static void beam(TileArcaneBore tile, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        var minecraft = Minecraft.getInstance();
        if (!(tile.getLevel() instanceof ClientLevel level) || minecraft.level != level || !current(tile, level)
                || tile.clientTarget() == null || tile.getBeamLength() < 1 || tile.getBeamLength() > 64
                || !level.hasChunkAt(tile.clientTarget()) || !tile.rotation().aligned() || !tile.isPowered()
                || !tile.hasFocus() || !tile.hasPickaxe() || tile.isQuarantined()) return;
        ItemStack pick = tile.getItem(1); // COPY only; no raw inventory reference.
        if (!pick.isDamageableItem() || (long) pick.getDamageValue() + 1 >= pick.getMaxDamage()) return;
        var camera = minecraft.getCameraEntity();
        double visible = Minecraft.useFancyGraphics() ? 50 : 25;
        Vec3 origin = Vec3.atCenterOf(tile.getBlockPos());
        if (camera == null || camera.position().distanceToSqr(origin) > visible * visible
                || Vec3.atCenterOf(tile.clientTarget()).distanceToSqr(origin) > 70 * 70
                || !Float.isFinite(tile.getAimX()) || !Float.isFinite(tile.getAimZ())) return;
        float yaw = (tile.rotation().x() + 90 - tile.getAimX()) * Mth.DEG_TO_RAD;
        float tilt = (tile.rotation().z() + 90 - tile.getAimZ()) * Mth.DEG_TO_RAD;
        Vec3 direction = new Vec3(Mth.sin(yaw) * Mth.cos(tilt), Mth.sin(tilt), Mth.cos(yaw) * Mth.cos(tilt));
        Vec3 maximum = origin.add(direction.scale(tile.getBeamLength()));
        var view = new LoadedClientView(level);
        var hit = view.clip(new ClipContext(origin.add(direction), maximum, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (!view.available || !current(tile, level)) return; // No guessed ray through unavailable cells.
        boolean collided = hit.getType() == HitResult.Type.BLOCK;
        Vec3 end = (collided ? hit.getLocation() : maximum).subtract(Vec3.atLowerCornerOf(tile.getBlockPos()));
        Vec3 delta = end.subtract(CENTER);
        double length = delta.length();
        if (!Double.isFinite(length) || length < .001 || length > 65) return;
        Vec3 axis = delta.scale(1 / length);
        Vec3 side = axis.cross(Math.abs(axis.y) > .99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 other = axis.cross(side).normalize();
        float spin = (level.getDayTime() % 72) * 5 + partialTick * 5;
        float age = (minecraft.player == null ? camera.tickCount : minecraft.player.tickCount) + partialTick;
        layer(pose, buffers.getBuffer(GREEN), end, side, other, length, spin, -age, 0x00FF66, collided ? 2 : 0);
        finish(buffers, GREEN);
        layer(pose, buffers.getBuffer(ORANGE), end, side, other, length, spin, age, 0xFF8855, collided ? 2 : 0);
        finish(buffers, ORANGE);
    }

    private static void layer(PoseStack pose, VertexConsumer out, Vec3 end, Vec3 side, Vec3 other, double length,
                              float spin, float age, int color, int endModifier) {
        float slide = -age * .2F - Mth.floor(-age * .1F);
        for (int plane = 0; plane < 3; plane++) {
            float angle = (spin + 60 * (plane + 1)) * Mth.DEG_TO_RAD;
            Vec3 width = side.scale(.15 * Mth.cos(angle)).add(other.scale(.15 * Mth.sin(angle)));
            Vec3 tip = width.scale(endModifier);
            float v = -1 + slide + plane / 3F;
            vertex(out, pose, end.subtract(tip), 1, v + (float) length, color, .4F);
            vertex(out, pose, CENTER.subtract(width), 1, v, color, .4F);
            vertex(out, pose, CENTER.add(width), 0, v, color, .4F);
            vertex(out, pose, end.add(tip), 0, v + (float) length, color, .4F);
        }
    }
    private static void vertex(VertexConsumer out, PoseStack pose, Vec3 point, float u, float v, int colour, float alpha) {
        out.addVertex(pose.last(), (float) point.x, (float) point.y, (float) point.z)
                .setColor(colour >> 16 & 255, colour >> 8 & 255, colour & 255, Mth.clamp((int) (alpha * 255), 0, 255))
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(200).setNormal(pose.last(), 0, 1, 0);
    }
    private static boolean current(TileArcaneBore tile, ClientLevel level) {
        return tile.getLevel() == level && !tile.isRemoved() && level.hasChunkAt(tile.getBlockPos())
                && level.getBlockEntity(tile.getBlockPos()) == tile && level.getBlockState(tile.getBlockPos()) == tile.getBlockState();
    }

    /** A closed CLIENT collision view: no chunk fetch/generation, mutable target
     * list, block setter or untrusted server state is exposed to drawing. */
    private static final class LoadedClientView implements BlockGetter {
        private final ClientLevel level;
        private boolean available = true;
        private LoadedClientView(ClientLevel level) { this.level = level; }
        private boolean loaded(BlockPos pos) {
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) { available = false; return false; }
            return true;
        }
        @Override public BlockState getBlockState(BlockPos pos) { return loaded(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState(); }
        @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        @Override public BlockEntity getBlockEntity(BlockPos pos) { return loaded(pos) ? level.getBlockEntity(pos) : null; }
        @Override public int getHeight() { return level.getHeight(); }
        @Override public int getMinBuildHeight() { return level.getMinBuildHeight(); }
    }

    private enum Material { SHELL, GLASS, VORTEX, BEAM }

    /** Native QUADS for ModelParts and effects. State is captured at actual DRAW
     * time, not while vertices are queued, and restored even if drawing throws. */
    private static final class BoreType extends RenderType {
        private final TextureStateShard texture;
        private final Material material;
        private NativeState previous;
        private BoreType(String name, ResourceLocation texture, Material material) {
            super("thaumcraft_bore_" + name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 8192, false, false, () -> {}, () -> {});
            this.texture = new TextureStateShard(texture, false, false); this.material = material;
        }
        @Override public void draw(MeshData mesh) {
            RenderSystem.assertOnRenderThread(); setupRenderState();
            try { BufferUploader.drawWithShader(mesh); }
            finally { clearRenderState(); }
        }
        @Override public void setupRenderState() {
            if (previous != null) throw new IllegalStateException("Nested bore material draw");
            previous = new NativeState();
            try {
                if (material == Material.SHELL) RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER.setupRenderState();
                else if (material == Material.BEAM) RENDERTYPE_EYES_SHADER.setupRenderState();
                else RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();
                texture.setupRenderState(); LIGHTMAP.setupRenderState(); OVERLAY.setupRenderState();
                RenderSystem.setShaderColor(1, 1, 1, 1); RenderSystem.enableDepthTest(); RenderSystem.depthFunc(GL11.GL_LEQUAL); RenderSystem.disableCull();
                RenderSystem.depthMask(material == Material.SHELL || material == Material.VORTEX);
                if (material == Material.SHELL) RenderSystem.disableBlend();
                else { RenderSystem.enableBlend(); RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, material == Material.BEAM ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA); }
            } catch (RuntimeException | Error failure) { NativeState saved = previous; previous = null; saved.close(); throw failure; }
        }
        @Override public void clearRenderState() {
            try {
                OVERLAY.clearRenderState(); LIGHTMAP.clearRenderState(); texture.clearRenderState();
                if (material == Material.SHELL) RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER.clearRenderState();
                else if (material == Material.BEAM) RENDERTYPE_EYES_SHADER.clearRenderState();
                else RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();
            } finally { NativeState saved = previous; previous = null; if (saved != null) saved.close(); }
        }
    }
    private static final class NativeState implements AutoCloseable {
        private final ShaderInstance shader = RenderSystem.getShader();
        private final int[] textures = {RenderSystem.getShaderTexture(0), RenderSystem.getShaderTexture(1), RenderSystem.getShaderTexture(2)};
        private final float[] colour = RenderSystem.getShaderColor().clone();
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC), src = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                dst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB), srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        @Override public void close() {
            RenderSystem.setShader(() -> shader);
            for (int index = 0; index < textures.length; index++) RenderSystem.setShaderTexture(index, textures[index]);
            RenderSystem.setShaderColor(colour[0], colour[1], colour[2], colour[3]);
            RenderSystem.blendFuncSeparate(src, dst, srcAlpha, dstAlpha);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            RenderSystem.depthMask(write); RenderSystem.depthFunc(depthFunction);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
        }
    }
}
