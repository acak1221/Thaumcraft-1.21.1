package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.tiles.TileCrucible;

/** Original fluid formula and FXBubble atlas frames, rendered through native buffers. */
public final class TileCrucibleRenderer implements BlockEntityRenderer<TileCrucible> {
    private static final ResourceLocation WATER = ResourceLocation.withDefaultNamespace("block/water_still");
    // Original 1.7.10 water_still was pre-coloured blue; native 1.21.1's
    // sprite is grayscale. Supply the native ordinary-water tint, then TC4's
    // pollution multiplier. Keep the original biome-independent crucible colour.
    private static final int WATER_COLOR = 0x3F76E4;
    private static final ResourceLocation PARTICLES = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final SoundEvent SPILL = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "spill"));
    private static final RenderType BUBBLES = new BubbleRenderType();
    private static final Map<TileCrucible, List<Bubble>> EFFECTS = new WeakHashMap<>();

    public TileCrucibleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileCrucible tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (tile.getWaterAmount() > 0) {
            var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WATER);
            float tint = tile.tagAmount() / 100F;
            if (tint > 0) tint = .5F + tint / 2;
            int color = rgb((WATER_COLOR >> 16 & 255) / 255F * (1 - tint / 3),
                    (WATER_COLOR >> 8 & 255) / 255F * (1 - tint), (WATER_COLOR & 255) / 255F * (1 - tint / 2));
            float height = tile.getFluidHeight();
            var water = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
            vertex(pose, water, 0, height, 0, sprite.getU1(), sprite.getV1(), color, light, 0, 1, 0);
            vertex(pose, water, 0, height, 1, sprite.getU1(), sprite.getV0(), color, light, 0, 1, 0);
            vertex(pose, water, 1, height, 1, sprite.getU0(), sprite.getV0(), color, light, 0, 1, 0);
            vertex(pose, water, 1, height, 0, sprite.getU0(), sprite.getV1(), color, light, 0, 1, 0);
        }
        List<Bubble> bubbles = EFFECTS.get(tile);
        if (bubbles == null || bubbles.isEmpty()) return;
        var buffer = buffers.getBuffer(BUBBLES);
        for (Bubble bubble : bubbles) {
            pose.pushPose();
            try {
                pose.translate(Mth.lerp(partialTick, bubble.ox, bubble.x), Mth.lerp(partialTick, bubble.oy, bubble.y), Mth.lerp(partialTick, bubble.oz, bubble.z));
                pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
                float u = bubble.frame % 16 / 16F, v = bubble.frame / 16 / 16F;
                float endU = u + .0624375F, endV = v + .0624375F, size = bubble.size;
                vertex(pose, buffer, -size, -size, 0, endU, endV, bubble.color, LightTexture.FULL_BRIGHT, 0, 0, 1);
                vertex(pose, buffer, size, -size, 0, u, endV, bubble.color, LightTexture.FULL_BRIGHT, 0, 0, 1);
                vertex(pose, buffer, size, size, 0, u, v, bubble.color, LightTexture.FULL_BRIGHT, 0, 0, 1);
                vertex(pose, buffer, -size, size, 0, endU, v, bubble.color, LightTexture.FULL_BRIGHT, 0, 0, 1);
            } finally {
                pose.popPose();
            }
        }
    }

    private static int rgb(float r, float g, float b) {
        return Mth.clamp((int) (r * 255), 0, 255) << 16 | Mth.clamp((int) (g * 255), 0, 255) << 8 | Mth.clamp((int) (b * 255), 0, 255);
    }
    private static void vertex(PoseStack pose, VertexConsumer buffer, float x, float y, float z, float u, float v,
                               int color, int light, float nx, float ny, float nz) {
        buffer.addVertex(pose.last(), x, y, z).setColor(color >> 16 & 255, color >> 8 & 255, color & 255, 255)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), nx, ny, nz);
    }

    /** TC4 TileCrucible.drawEffects and ClientProxy:685-724; never consumes server RNG. */
    public static final class Effects implements TileCrucible.ClientEffects {
        @Override public void tick(TileCrucible tile) {
            List<Bubble> bubbles = EFFECTS.get(tile);
            if (bubbles != null) {
                bubbles.removeIf(bubble -> !bubble.tick(tile.getLevel().random));
                if (bubbles.isEmpty()) EFFECTS.remove(tile);
            }
            if (tile.getWaterAmount() <= 0 || !visible(tile)) return;
            var random = tile.getLevel().random;
            float height = tile.getFluidHeight();
            if (tile.getHeat() > 150) {
                add(tile, .2F + random.nextFloat() * .6F, height, .2F + random.nextFloat() * .6F, 0x7f7fb2, -4, 1, -.001);
                if (tile.tagAmount() > 100) {
                    for (int i = 0; i < 2; ++i) {
                        add(tile, 0, 1, random.nextFloat(), 0x7f7fb2, -4, 2, -.005);
                        add(tile, 1, 1, random.nextFloat(), 0x7f7fb2, -4, 2, -.005);
                        add(tile, random.nextFloat(), 1, 0, 0x7f7fb2, -4, 2, -.005);
                        add(tile, random.nextFloat(), 1, 1, 0x7f7fb2, -4, 2, -.005);
                    }
                }
            }
            AspectList aspects = tile.getAspects();
            if (random.nextInt(6) == 0 && aspects.size() > 0) {
                int color = aspects.getAspects()[random.nextInt(aspects.size())].getColor();
                float x = (5 + random.nextInt(22)) / 32F + .015625F;
                float z = (5 + random.nextInt(22)) / 32F + .015625F;
                random.nextInt(10); // Original unused drawEffects delay.
                add(tile, x, .05F + height, z, color, 1, 0, .002);
            }
        }

        @Override public void event(TileCrucible tile, int id, int data) {
            if (tile.getLevel() == null) return;
            var level = tile.getLevel();
            var pos = tile.getBlockPos();
            if (id == 1) {
                for (int i = 0; i < 5; ++i) level.addParticle(ParticleTypes.ENCHANT,
                        pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
                        level.random.nextFloat() - .5, level.random.nextFloat(), level.random.nextFloat() - .5);
                return;
            }
            level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, SPILL, SoundSource.BLOCKS, .2F, 1, false);
            if (!visible(tile)) return;
            AspectList aspects = tile.getAspects();
            for (int i = 0; i < 10; ++i) {
                level.random.nextInt(22); level.random.nextInt(22); // Original unused x/y.
                int color = aspects.size() == 0 ? 0xffffff : aspects.getAspects()[level.random.nextInt(aspects.size())].getColor();
                add(tile, .2F + level.random.nextFloat() * .6F, .1F + tile.getFluidHeight(),
                        .2F + level.random.nextFloat() * .6F, color, 3, 0, .003 * data);
            }
        }
    }

    private static boolean visible(TileCrucible tile) {
        var camera = Minecraft.getInstance().getCameraEntity();
        return camera != null && camera.distanceToSqr(tile.getBlockPos().getX() + .5, tile.getBlockPos().getY() + .5, tile.getBlockPos().getZ() + .5) <= 2500;
    }
    private static void add(TileCrucible tile, float x, float y, float z, int color, int age, int froth, double speed) {
        EFFECTS.computeIfAbsent(tile, ignored -> new ArrayList<>()).add(new Bubble(tile.getLevel().random, x, y, z, color, age, froth, speed));
    }

    /** FXBubble:26-88's size, three pop frames, buoyancy and damping. */
    private static final class Bubble {
        private double x, y, z, ox, oy, oz, dx, dy, dz;
        private final double speed;
        private final float size;
        private final int color;
        private int life, frame = 16;

        private Bubble(RandomSource random, float x, float y, float z, int color, int age, int froth, double speed) {
            this.x = ox = x; this.y = oy = y; this.z = oz = z;
            this.color = color;
            this.speed = speed;
            float scale = (random.nextFloat() * .5F + .5F) * 2 * (random.nextFloat() * .3F + .2F);
            size = .1F * scale * (froth == 0 ? 1 : .75F);
            dx = (random.nextDouble() * 2 - 1) * .02;
            dy = random.nextDouble() * .02;
            dz = (random.nextDouble() * 2 - 1) * .02;
            life = (int) (age + 2 + 8 / (random.nextDouble() * .8 + .2));
            if (froth != 0) {
                life = froth == 1 ? 4 + random.nextInt(3) : 12 + random.nextInt(12);
                dx /= 5; dy /= 10; dz /= 5;
            }
        }
        private boolean tick(RandomSource random) {
            ox = x; oy = y; oz = z;
            dy += speed;
            if (speed > 0) {
                dx += (random.nextFloat() - random.nextFloat()) * .01F;
                dz += (random.nextFloat() - random.nextFloat()) * .01F;
            }
            x += dx; y += dy; z += dz;
            dx *= .85F; dy *= .85F; dz *= .85F;
            if (life-- <= 0) return false;
            if (life <= 2) ++frame;
            return true;
        }
    }

    private static final class BubbleRenderType extends RenderType {
        private static final TextureStateShard TEXTURE = new TextureStateShard(PARTICLES, false, false);
        private BubbleRenderType() {
            super("thaumcraft_crucible_bubbles", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                    1536, false, true, BubbleRenderType::begin, BubbleRenderType::end);
        }
        private static void begin() {
            RENDERTYPE_EYES_SHADER.setupRenderState();
            TEXTURE.setupRenderState();
            LIGHTNING_TRANSPARENCY.setupRenderState(); // TC4 particle layer0: SRC_ALPHA, ONE.
            NO_CULL.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            COLOR_WRITE.setupRenderState();
        }
        private static void end() {
            COLOR_WRITE.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            NO_CULL.clearRenderState();
            LIGHTNING_TRANSPARENCY.clearRenderState();
            TEXTURE.clearRenderState();
            RENDERTYPE_EYES_SHADER.clearRenderState();
        }
    }
}
