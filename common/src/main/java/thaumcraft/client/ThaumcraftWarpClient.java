package thaumcraft.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.lib.network.WarpEventNetworking;

/** Original heartbeat/vignette100, mist2400/200, and the four effect post chains; no server-only class reaches client rendering.
 *
 * <p>Original shader driver: the 1.7 TC4 built its four full screen effects with vanilla's
 * {@code ShaderGroup} (oracle {@code thaumcraft/client/lib/ClientTickEventsFML.java:118,168-231},
 * rendered by {@code thaumcraft/client/lib/RenderEventHandler.java:161-197}). The groups read
 * {@code assets/minecraft/shaders/post/{desaturatetc,blurtc,hunger,sunscorned}.json} and their
 * programs from {@code assets/minecraft/shaders/program/}. The native successor to that
 * driver is vanilla {@code PostChain}, and the shader inventory of
 * this port is:
 * <ul>
 * <li>{@code assets/thaumcraft/shaders/core/} - native 1.21.1 core shaders, registered by the
 * loader client bootstrap ({@code monster_lit_particle}, {@code node_stabilizer_bubble}).</li>
 * <li>{@code assets/thaumcraft/shaders/post/} - the four vanilla post chains of this class, the
 * native equivalent of the original's four vanilla groups.</li>
 * <li>{@code assets/minecraft/shaders/program/} - the three original TC4 post programs
 * ({@code thaumcraft_color_convolve2}, {@code thaumcraft_bloom_color}, {@code thaumcraft_bloom2}
 * and their shared vertex program {@code thaumcraft_bloom_vertex_base}), ported to the GLSL 150
 * dialect of 1.21.1.
 *
 * <p>The programs live in the DEFAULT namespace because that is the only place 1.21.1 looks for
 * them: {@code net.minecraft.client.renderer.EffectInstance} resolves the program description with
 * {@code ResourceLocation.withDefaultNamespace("shaders/program/" + name + ".json")} and the
 * vertex/fragment sources with the same default namespace and the {@code .vsh}/{@code .fsh}
 * extension. A pass named {@code minecraft:box_blur} or {@code thaumcraft:bloom2} would be looked
 * up as {@code minecraft:shaders/program/minecraft:box_blur.json} and could never be found; the
 * chains therefore name the vanilla programs ({@code color_convolve}, {@code blit},
 * {@code thaumcraft_blur}) and the three ported TC4 programs by their plain names. The two chains that
 * need no TC4 program are the original {@code desaturatetc} and {@code blurtc} ones: the original
 * {@code hunger} chain used TC4's own {@code color_convolve2} and the original
 * {@code sunscorned} chain used TC4's own {@code bloomColor} and {@code bloom2}.
 *
 * <p>The original {@code blurtc} chain uses Mojang's 1.7.10 {@code blur} program. Its
 * asymmetric squared weights are NOT the modern {@code box_blur}; {@code thaumcraft_blur}
 * retains the original program with GLSL150 syntax, Radius10 and horizontal/vertical pass order.
 *
 * <p>The original switch {@code Config.shaders} (oracle
 * {@code thaumcraft/common/config/Config.java:410-412}) gates the whole chain and is read from
 * the local configuration file by {@link thaumcraft.common.config.ThaumcraftSettings#shaders()}. */
public final class ThaumcraftWarpClient {
    private static final ResourceLocation VIGNETTE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/vignette.png");
    private static final Map<String, PostChain> CHAINS = new LinkedHashMap<>();
    private static final Map<String, Integer> FAILED = new LinkedHashMap<>();
    private static int vignette, fogDuration, width, height;
    private static float fogDensity, vignetteBrightness;
    private static Object lastLevel;
    private static boolean initialized;
    private ThaumcraftWarpClient() {}
    public static void init() {
        if (initialized) return; initialized = true;
        WarpEventNetworking.initClient(ThaumcraftWarpClient::receive, source -> {
            var level = Minecraft.getInstance().level;
            var entity = level == null ? null : level.getEntity(source);
            if (entity != null) Minecraft.getInstance().particleEngine.add(new thaumcraft.client.fx.particles.WarpSonicParticle(level, entity));
        });
        thaumcraft.common.entities.projectile.EntityEldritchOrb.setClientImpactEffect(thaumcraft.client.fx.particles.WarpOrbParticle::explode);
        thaumcraft.common.entities.monster.EntityEldritchGuardian.setClientAmbient(thaumcraft.client.fx.particles.WarpGuardianParticle::ambient);
        ClientTickEvent.CLIENT_POST.register(client -> tick());
    }
    public static void receive(int event) {
        var client = Minecraft.getInstance();
        if (client.player == null) return;
        if (event == 0) {
            vignette = 100;
            client.player.playNotifySound(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "heartbeat")), SoundSource.PLAYERS, 1, 1);
        } else if (event == 1) fogDuration = 2400;
        else if (event == 2) fogDuration = Math.max(200, fogDuration);
    }
    public static void tick() {
        var client = Minecraft.getInstance();
        if (client.level != lastLevel) { reset(); lastLevel = client.level; }
        if (client.player == null || client.isPaused()) return;
        if (client.player.hasEffect(ThaumcraftWarp.effect(ThaumcraftWarp.DEATH_GAZE))) vignette = 10;
        if (vignette > 0) vignette--;
        if (fogDuration > 0) {
            if (fogDuration < 100) fogDensity = .1F * fogDuration / 100;
            else if (fogDensity < .1F) fogDensity += .001F;
            fogDuration--;
        } else fogDensity = 0;
    }
    public static void reset() {
        vignette = fogDuration = 0; fogDensity = vignetteBrightness = 0;
        for (PostChain chain : CHAINS.values()) chain.close();
        CHAINS.clear(); FAILED.clear(); width = height = 0;
        thaumcraft.client.fx.particles.WarpSonicParticle.resetMesh();
    }
    /** Root's native HUD render callback invokes this after the ordinary HUD has established GUI coordinates. */
    public static void renderHud(GuiGraphics graphics) {
        var client = Minecraft.getInstance();
        if (client.player == null) return;
        vignetteBrightness += ((vignette > 0 ? 1 : 0) - vignetteBrightness) * .01F;
        if (vignetteBrightness <= .001F) return;
        float brightness = vignetteBrightness * (1 + (float)Math.sin(client.player.tickCount / 2F) * .1F);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); graphics.setColor(brightness, brightness, brightness, 1);
        graphics.blit(VIGNETTE, 0, 0, 0, 0, graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(), graphics.guiHeight());
        graphics.flush(); graphics.setColor(1, 1, 1, 1); RenderSystem.depthMask(true); RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc(); RenderSystem.disableBlend();
    }
    /** Native 1.21 fog uses a linear range; the old exponential density is mapped to the same reciprocal visibility length. */
    public static void applyFog() {
        if (fogDensity <= 0) return;
        RenderSystem.setShaderFogStart(0); RenderSystem.setShaderFogEnd(1 / fogDensity); RenderSystem.setShaderFogShape(FogShape.SPHERE);
    }
    /** Called after world rendering, before GUI; each chain owns/reuses its swap targets and closes on level/resource lifecycle. */
    public static void renderPostEffects(float partialTick) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        // Original Config.shaders: the whole chain is off when the local setting is off.
        if (!thaumcraft.common.config.ThaumcraftSettings.shaders()) {
            for (String old : new ArrayList<>(CHAINS.keySet())) CHAINS.remove(old).close();
            return;
        }
        boolean resized = width != client.getWindow().getWidth() || height != client.getWindow().getHeight();
        width = client.getWindow().getWidth(); height = client.getWindow().getHeight();
        var active = new java.util.LinkedHashSet<String>();
        // The original group order is the index order of its shader group map: 0 desaturatetc,
        // 1 blurtc, 2 hunger, 3 sunscorned (oracle ClientTickEventsFML.java:114-118).
        if (client.player.hasEffect(ThaumcraftWarp.effect(ThaumcraftWarp.DEATH_GAZE))) active.add("warp_death_gaze");
        if (client.player.hasEffect(ThaumcraftWarp.effect(ThaumcraftWarp.BLURRED_VISION))) active.add("warp_blurred");
        if (client.player.hasEffect(ThaumcraftWarp.effect(ThaumcraftWarp.UNNATURAL_HUNGER))) active.add("warp_hunger");
        if (client.player.hasEffect(ThaumcraftWarp.effect(ThaumcraftWarp.SUN_SCORNED))) active.add("warp_sun_scorned");
        for (String old : new ArrayList<>(CHAINS.keySet())) if (!active.contains(old)) CHAINS.remove(old).close();
        for (String name : active) {
            if (java.util.Objects.equals(FAILED.get(name), client.player.tickCount)) continue;
            PostChain chain = CHAINS.get(name);
            try {
                if (chain == null) {
                    chain = new PostChain(client.getTextureManager(), client.getResourceManager(), client.getMainRenderTarget(),
                            ResourceLocation.fromNamespaceAndPath("thaumcraft", "shaders/post/" + name + ".json"));
                    chain.resize(width, height); CHAINS.put(name, chain);
                } else if (resized) chain.resize(width, height);
                chain.process(partialTick);
                FAILED.remove(name);
            } catch (Exception failure) {
                // Original checkShaders:168-209 retries missing groups on the next player tick.
                // Do not turn a transient/resource-pack failure into a permanent session latch.
                if (chain != null) chain.close(); CHAINS.remove(name); FAILED.put(name, client.player.tickCount);
                Thaumcraft.LOG.error("Unable to render warp post chain {}", name, failure);
            }
        }
        client.getMainRenderTarget().bindWrite(false);
    }
}
