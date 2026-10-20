package thaumcraft.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.entity.Entity;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.particles.MonsterSpriteParticle;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.lib.mods.ChampionRules;
import thaumcraft.common.lib.mods.IChampionModifierEffect;

/**
 * The original champion particles of the 1.7 bestiary. The 1.7 original drew them on
 * every living update of a champion mob, from the retained particle sheet of the
 * original mod: the generic champion particle walks its frames of the particle sheet,
 * the bold champion uses the spark sheet, and the infested champion uses the original
 * slime jump. The native port draws the same sheets with the native particle engine.
 */
public final class ThaumcraftChampionClient {
    private static boolean initialized;
    private ThaumcraftChampionClient() {}

    /** The client bootstrap sets the renderer that draws the original champion particles. */
    public static void init() {
        if (initialized) return;
        initialized = true;
        ChampionRules.ClientFx.setRenderer(ThaumcraftChampionClient::render);
    }

    /** The original client trigger of the champion particles. */
    private static void render(Entity champion, IChampionModifierEffect.Fx fx) {
        if (!(champion.level() instanceof ClientLevel level)) return;
        switch (fx.kind()) {
            case GENERIC -> add(new ChampionParticle(level, champion, fx, false));
            case SPARK -> add(new ChampionParticle(level, champion, fx, true));
            case SLIME_JUMP -> add(new ChampionSlimeJumpParticle(level, champion));
            default -> {
            }
        }
    }

    private static void add(SingleQuadParticle particle) {
        Minecraft.getInstance().particleEngine.add(particle);
    }

    /**
     * The original generic champion particle and the original spark. The generic
     * particle walks the frames of its cell range of the particle sheet, the spark
     * walks the eight frames of its own strip of the second sheet.
     */
    private static final class ChampionParticle extends SingleQuadParticle {
        private final IChampionModifierEffect.Fx fx;
        private final boolean spark;
        private final int frameBase;
        private final int frameCount;
        private final float size;
        private final boolean flip;

        private ChampionParticle(ClientLevel level, Entity champion, IChampionModifierEffect.Fx fx, boolean spark) {
            super(level, champion.getBoundingBox().minX + fx.x(), champion.getBoundingBox().minY + fx.y(),
                    champion.getBoundingBox().minZ + fx.z());
            this.fx = fx;
            this.spark = spark;
            hasPhysics = false;
            xo = x;
            yo = y;
            zo = z;
            if (spark) {
                // The original FXSpark rolled its own strip and its own lifetime.
                frameBase = level.random.nextInt(3) * 8;
                frameCount = 8;
                lifetime = 5 + level.random.nextInt(5);
                size = fx.scale();
                setColor(1.0F, 1.0F, 1.0F);
                alpha = fx.alpha();
                flip = level.random.nextBoolean();
                xd = 0.0D;
                yd = 0.0D;
                zd = 0.0D;
            } else {
                frameBase = fx.start();
                frameCount = Math.max(1, fx.num());
                lifetime = Math.max(1, fx.age() + fx.delay());
                size = fx.scale();
                setColor(fx.red(), fx.green(), fx.blue());
                alpha = fx.alpha();
                flip = false;
                xd = fx.vx();
                yd = fx.vy();
                zd = fx.vz();
            }
            quadSize = size;
            setSize(0.01F, 0.01F);
        }

        private static double[] position(Entity champion, IChampionModifierEffect.Fx fx) {
            AABB box = champion.getBoundingBox();
            return new double[]{box.minX + fx.x(), box.minY + fx.y(), box.minZ + fx.z()};
        }

        /** The original frame of the particle. */
        private int frame() {
            if (spark) return frameBase + (int) ((float) age / (float) lifetime * 7.0F);
            if (fx.loop()) return frameBase + age / Math.max(1, fx.inc()) % frameCount;
            float fs = (float) age / (float) lifetime;
            return frameBase + (int) Math.min((float) frameCount * fs, (float) (frameCount - 1));
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            if (age++ >= lifetime) {
                remove();
                return;
            }
            if (!spark) {
                move(xd, yd, zd);
                xd *= 0.91D;
                yd *= 0.98D;
                zd *= 0.91D;
            }
        }

        @Override
        public float getQuadSize(float partial) { return size; }

        @Override
        protected float getU0() {
            int frame = frame();
            if (spark) return flip ? (frame % 8) / 8.0F + 0.125F : (frame % 8) / 8.0F;
            return (frame % 16) / 16.0F;
        }

        @Override
        protected float getU1() {
            int frame = frame();
            if (spark) return flip ? (frame % 8) / 8.0F : (frame % 8) / 8.0F + 0.125F;
            return (frame % 16) / 16.0F + 0.0624375F;
        }

        @Override
        protected float getV0() {
            int frame = frame();
            if (spark) return (frame / 8) / 8.0F;
            return (frame / 16) / 16.0F;
        }

        @Override
        protected float getV1() {
            int frame = frame();
            if (spark) return (frame / 8) / 8.0F + 0.125F;
            return (frame / 16) / 16.0F + 0.0624375F;
        }

        @Override
        public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

        @Override
        public void render(VertexConsumer unused, Camera camera, float partial) {
            float original = alpha;
            if (!spark && (age <= 1 || age >= lifetime - 1)) alpha = original * 0.5F;
            try {
                VisNetRender.drawParticle(spark ? VisNetRender.texture("misc/particles2") : VisNetRender.PARTICLES,
                        GL11.GL_ONE, MonsterSpriteParticle.litShader(), out -> super.render(out, camera, partial));
            } finally {
                alpha = original;
            }
        }
    }

    /**
     * The original slime jump of the infested champion. The 1.7 original broke the
     * slime ball item in the middle of the champion, the native port draws the same
     * purple breaking glow of the original colour and lifetime.
     */
    private static final class ChampionSlimeJumpParticle extends SingleQuadParticle {
        private ChampionSlimeJumpParticle(ClientLevel level, Entity champion) {
            super(level, champion.getX(), (champion.getBoundingBox().minY + champion.getBoundingBox().maxY) / 2.0, champion.getZ());
            hasPhysics = false;
            xo = x;
            yo = y;
            zo = z;
            xd = 0.0D;
            yd = 0.0D;
            zd = 0.0D;
            setColor(0.7F, 0.0F, 1.0F);
            alpha = 0.4F;
            lifetime = (int) (66.0F / (level.random.nextFloat() * 0.9F + 0.1F));
            quadSize = 0.3F;
            setSize(0.1F, 0.1F);
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            if (age++ >= lifetime) {
                remove();
                return;
            }
        }

        @Override
        public float getQuadSize(float partial) { return 0.3F; }

        @Override
        protected float getU0() { return 0.0F; }

        @Override
        protected float getU1() { return 0.0624375F; }

        @Override
        protected float getV0() { return 0.0F; }

        @Override
        protected float getV1() { return 0.0624375F; }

        @Override
        public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

        @Override
        public void render(VertexConsumer unused, Camera camera, float partial) {
            VisNetRender.drawParticle(VisNetRender.PARTICLES, GL11.GL_ONE, MonsterSpriteParticle.litShader(),
                    out -> super.render(out, camera, partial));
        }
    }
}
