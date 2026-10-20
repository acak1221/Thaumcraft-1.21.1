package thaumcraft.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.lib.network.WarpEventNetworking;
import thaumcraft.client.renderers.entity.RenderEldritchGuardian;
import thaumcraft.client.renderers.entity.RenderEldritchOrb;
import thaumcraft.client.renderers.entity.RenderMindSpider;

/** Original heartbeat/vignette100, mist2400/200, and the four effect post chains; no server-only class reaches client rendering. */
public final class ThaumcraftWarpClient {
    private static final ResourceLocation VIGNETTE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/vignette.png");
    private static final Map<String, PostChain> CHAINS = new LinkedHashMap<>();
    private static final java.util.Set<String> FAILED = new java.util.HashSet<>();
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
        EntityRendererRegistry.register(ThaumcraftWarp.MIND_SPIDER, RenderMindSpider::new);
        EntityRendererRegistry.register(ThaumcraftWarp.ELDRITCH_GUARDIAN, RenderEldritchGuardian::new);
        EntityRendererRegistry.register(ThaumcraftWarp.ELDRITCH_ORB, RenderEldritchOrb::new);
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
        boolean resized = width != client.getWindow().getWidth() || height != client.getWindow().getHeight();
        width = client.getWindow().getWidth(); height = client.getWindow().getHeight();
        var active = new java.util.LinkedHashSet<String>();
        if (client.player.hasEffect(ThaumcraftWarp.effect(ThaumcraftWarp.DEATH_GAZE))) active.add("warp_death_gaze");
        if (client.player.hasEffect(ThaumcraftWarp.effect(ThaumcraftWarp.BLURRED_VISION))) active.add("warp_blurred");
        if (client.player.hasEffect(ThaumcraftWarp.effect(ThaumcraftWarp.UNNATURAL_HUNGER))) active.add("warp_hunger");
        if (client.player.hasEffect(ThaumcraftWarp.effect(ThaumcraftWarp.SUN_SCORNED))) active.add("warp_sun_scorned");
        for (String old : new ArrayList<>(CHAINS.keySet())) if (!active.contains(old)) CHAINS.remove(old).close();
        for (String name : active) {
            if (FAILED.contains(name)) continue;
            PostChain chain = CHAINS.get(name);
            try {
                if (chain == null) {
                    chain = new PostChain(client.getTextureManager(), client.getResourceManager(), client.getMainRenderTarget(),
                            ResourceLocation.fromNamespaceAndPath("thaumcraft", "shaders/post/" + name + ".json"));
                    chain.resize(width, height); CHAINS.put(name, chain);
                } else if (resized) chain.resize(width, height);
                chain.process(partialTick);
            } catch (Exception failure) {
                if (chain != null) chain.close(); CHAINS.remove(name); FAILED.add(name);
                Thaumcraft.LOG.error("Unable to render warp post chain {}", name, failure);
            }
        }
        client.getMainRenderTarget().bindWrite(false);
    }
}
