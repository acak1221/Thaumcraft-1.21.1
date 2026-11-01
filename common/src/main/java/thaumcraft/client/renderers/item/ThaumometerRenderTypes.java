package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.fx.particles.MonsterSpriteParticle;
import thaumcraft.client.renderers.models.ModelThaumometer;

/** The scanner's own sampler and lit additive glyph state, without changing other item effects. */
final class ThaumometerRenderTypes {
    private static final int MAX_ICON_TYPES = 256;
    private static final Map<ResourceLocation, RenderType> ICONS = new LinkedHashMap<>(16, .75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<ResourceLocation, RenderType> entry) {
            return size() > MAX_ICON_TYPES;
        }
    };
    private static final RenderType SCREEN = new SurfaceType(ModelThaumometer.SCREEN, true);
    private static final Map<RenderType, RenderType> TEXT = new LinkedHashMap<>(16, .75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<RenderType, RenderType> entry) {
            return size() > MAX_ICON_TYPES;
        }
    };

    private ThaumometerRenderTypes() {}

    static synchronized RenderType icon(ResourceLocation texture) {
        return ICONS.computeIfAbsent(texture, key -> new SurfaceType(key, false));
    }

    static RenderType screen() { return SCREEN; }

    /** Keep the native font texture/shader/light while applying the scanner's additive blend. */
    static MultiBufferSource textBuffers(MultiBufferSource buffers) {
        return original -> buffers.getBuffer(text(original));
    }

    private static synchronized RenderType text(RenderType original) {
        return TEXT.computeIfAbsent(original, TextType::new);
    }

    private static final class TextType extends RenderType {
        private TextType(RenderType original) {
            super("thaumcraft_scanner_text", original.format(), original.mode(), original.bufferSize(), false, true,
                    () -> { original.setupRenderState(); LIGHTNING_TRANSPARENCY.setupRenderState(); },
                    () -> { LIGHTNING_TRANSPARENCY.clearRenderState(); original.clearRenderState(); });
        }
    }

    private static final class SurfaceType extends RenderType {
        // Resolve the shader at draw time: both loaders replace it on every resource reload.
        private static final ShaderStateShard ICON_SHADER = new ShaderStateShard(MonsterSpriteParticle::litShader);

        private SurfaceType(ResourceLocation texture, boolean screen) {
            this(new TextureStateShard(texture, screen, false), screen);
        }

        private SurfaceType(TextureStateShard texture, boolean screen) {
            super(screen ? "thaumcraft_scanner_screen" : "thaumcraft_scanner_aspect",
                    screen ? DefaultVertexFormat.NEW_ENTITY : DefaultVertexFormat.PARTICLE,
                    VertexFormat.Mode.QUADS, 1536, false, true,
                    () -> begin(texture, screen), () -> end(texture, screen));
        }

        private static void begin(TextureStateShard texture, boolean screen) {
            shader(screen).setupRenderState();
            texture.setupRenderState();
            // LIGHTNING_TRANSPARENCY is SRC_ALPHA, ONE; ordinary ADDITIVE is ONE, ONE.
            transparency(screen).setupRenderState();
            NO_CULL.setupRenderState();
            LIGHTMAP.setupRenderState();
            if (screen) OVERLAY.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            COLOR_DEPTH_WRITE.setupRenderState();
        }

        private static void end(TextureStateShard texture, boolean screen) {
            COLOR_DEPTH_WRITE.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            if (screen) OVERLAY.clearRenderState();
            LIGHTMAP.clearRenderState();
            NO_CULL.clearRenderState();
            transparency(screen).clearRenderState();
            texture.clearRenderState();
            shader(screen).clearRenderState();
        }

        private static ShaderStateShard shader(boolean screen) {
            // The existing particle shader multiplies UV2 light without normal shading and
            // retains the original drawTag alpha threshold of 1/255, including its soft edges.
            return screen ? RENDERTYPE_ENTITY_TRANSLUCENT_SHADER : ICON_SHADER;
        }

        private static TransparencyStateShard transparency(boolean screen) {
            return screen ? TRANSLUCENT_TRANSPARENCY : LIGHTNING_TRANSPARENCY;
        }
    }
}
