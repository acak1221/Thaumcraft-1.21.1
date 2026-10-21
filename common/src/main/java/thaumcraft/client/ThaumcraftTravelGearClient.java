package thaumcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import thaumcraft.client.fx.particles.HarnessBoltParticle;
import thaumcraft.client.gui.GuiHoverHarness;
import thaumcraft.client.renderers.models.gear.HarnessArmorRenderer;
import thaumcraft.common.config.ThaumcraftTravelGear;
import thaumcraft.common.items.armor.Hover;
import thaumcraft.common.items.armor.ItemHoverHarness;
import thaumcraft.common.items.armor.TravelGearRules;
import thaumcraft.common.lib.enchantment.EquipmentEnchantments;
import thaumcraft.common.lib.network.HoverNetworking;

/** Native H input sends intent only. Audio, client damping, animated rings and
 * grounded visual sparks derive from the actual worn server-synced harness. */
public final class ThaumcraftTravelGearClient {
    // Original KeyHandler.keyH is LWJGL2 key 35, and the LWJGL2 key table makes 35 the H key.
    public static final KeyMapping HOVER=new KeyMapping("key.thaumcraft.hover",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_H,KeyMapping.CATEGORY_MISC);
    private static final ResourceLocation RING=ResourceLocation.fromNamespaceAndPath("thaumcraft","items/lightningring");
    private static final RenderType RING_TYPE=new RingRenderType();
    private static final Map<Player,Boolean> LAST=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Player,Long> SOUND=Collections.synchronizedMap(new WeakHashMap<>()),SPARK=Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean initialized,pressed;
    private ThaumcraftTravelGearClient() {}
    public static void init() {
        if(initialized)return;initialized=true;KeyMappingRegistry.register(HOVER);MenuRegistry.registerScreenFactory(ThaumcraftTravelGear.HARNESS_MENU.get(),GuiHoverHarness::new);
        ItemHoverHarness.setTooltipViewer(()->Minecraft.getInstance().player);
        Hover.setClientEffects(new Hover.ClientEffects(){public void tick(Player player,ItemStack armor,boolean active){wornTick(player,active);}public void forget(Player player){LAST.remove(player);SOUND.remove(player);SPARK.remove(player);}});
        ClientTickEvent.CLIENT_POST.register(ThaumcraftTravelGearClient::keys);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player->{Hover.forget(player);reset();});ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level->reset());
    }
    public static void reset() {LAST.clear();SOUND.clear();SPARK.clear();pressed=false;HarnessArmorRenderer.reset();}
    private static void keys(Minecraft client) {
        while(HOVER.consumeClick()) { }
        boolean down=HOVER.isDown();
        if(client.player==null||client.level==null||!client.isWindowActive()||client.screen!=null||client.getOverlay()!=null) {pressed=down;return;}
        if(down&&!pressed&&Hover.hasHarness(client.player))HoverNetworking.requestToggle();pressed=down;
    }
    private static SoundEvent sound(String path) {return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft",path));}
    private static void wornTick(Player player,boolean active) {
        var client=Minecraft.getInstance();if(player!=client.player)return;
        Boolean previous=LAST.put(player,active);if(previous!=null&&previous!=active)player.playSound(sound(active?"hhon":"hhoff"),.33F,1F);
        if(!active||!player.getAbilities().flying||player.getAbilities().instabuild)return;
        long now=System.currentTimeMillis();if(SOUND.getOrDefault(player,0L)<now) {SOUND.put(player,now+1200L);player.playSound(sound("jacobs"),.05F,1F+player.level().random.nextFloat()*.05F);}
        int haste=EquipmentEnchantments.level(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST),player.level(),EquipmentEnchantments.HASTE);
        float damping=TravelGearRules.damping(haste,Hover.girdle(player));Vec3 motion=player.getDeltaMovement();player.setDeltaMovement(motion.x*damping,motion.y,motion.z*damping);
    }
    public static void renderHarnessEffects(PoseStack pose,MultiBufferSource buffers,ItemStack armor,LivingEntity wearer) {
        if(!(wearer instanceof Player player)||!Hover.getHover(player)||!(player.level() instanceof ClientLevel level))return;
        float sneak=player.isShiftKeyDown()?.075F:0;
        pose.pushPose();try {
            if(player.isShiftKeyDown()) {pose.mulPose(Axis.XP.rotationDegrees(28.64789F));pose.translate(0,.075,-.05);}
            pose.translate(0,.2,.55);var sprite=Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(RING);var out=buffers.getBuffer(RING_TYPE);
            ring(pose,out,sprite,2.5F,0xffffffff);pose.mulPose(Axis.YP.rotationDegrees(180));pose.translate(0,0,.03);ring(pose,out,sprite,1.5F,0xffff7fff);
        }finally {pose.popPose();}
        long now=System.currentTimeMillis();if(SPARK.getOrDefault(player,0L)>=now)return;
        SPARK.put(player,now+50+level.random.nextInt(50));float yaw=player.yBodyRot-90-level.random.nextInt(180),pitch=-80+level.random.nextInt(160);
        double y=player.getEyeY()-.45F-sneak;Vec3 from=new Vec3(player.getX(),y,player.getZ());
        float cy=Mth.cos(-yaw*((float)Math.PI/180F)-(float)Math.PI),sy=Mth.sin(-yaw*((float)Math.PI/180F)-(float)Math.PI),cp=-Mth.cos(-pitch*((float)Math.PI/180F)),sp=Mth.sin(-pitch*((float)Math.PI/180F));
        Vec3 to=from.add((double)(sy*cp)*6,(double)sp*6,(double)(cy*cp)*6);var hit=level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if(hit.getType()==HitResult.Type.BLOCK) {float angle=(player.yBodyRot+90)/180F*3.141593F;
            Vec3 source=new Vec3(player.getX()-(double)(Mth.cos(angle)*.5F),y,player.getZ()-(double)(Mth.sin(angle)*.5F));HarnessBoltParticle.emit(level,source,hit.getLocation(),level.random.nextLong());}
    }
    private static void ring(PoseStack pose,VertexConsumer out,TextureAtlasSprite sprite,float size,int color) {
        float h=size*.5F;vertex(pose,out,-h,h,sprite.getU1(),sprite.getV1(),color);vertex(pose,out,h,h,sprite.getU0(),sprite.getV1(),color);
        vertex(pose,out,h,-h,sprite.getU0(),sprite.getV0(),color);vertex(pose,out,-h,-h,sprite.getU1(),sprite.getV0(),color);
    }
    private static void vertex(PoseStack pose,VertexConsumer out,float x,float y,float u,float v,int color) {out.addVertex(pose.last(),x,y,0).setColor(color).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(230).setNormal(pose.last(),0,0,1);}
    private static final class RingRenderType extends RenderType {
        private static final TextureStateShard TEXTURE=new TextureStateShard(InventoryMenu.BLOCK_ATLAS,false,false);
        RingRenderType() {super("thaumcraft_harness_ring",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,4096,false,true,RingRenderType::begin,RingRenderType::end);}
        private static void begin() {RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();TEXTURE.setupRenderState();ADDITIVE_TRANSPARENCY.setupRenderState();CULL.setupRenderState();LIGHTMAP.setupRenderState();OVERLAY.setupRenderState();}
        private static void end() {OVERLAY.clearRenderState();LIGHTMAP.clearRenderState();CULL.clearRenderState();ADDITIVE_TRANSPARENCY.clearRenderState();TEXTURE.clearRenderState();RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();}
    }
}
