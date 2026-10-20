package thaumcraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.client.fx.particles.RunicHitParticle;
import thaumcraft.common.lib.network.RunicChargePayload;
import thaumcraft.common.lib.network.RunicHitPayload;
import thaumcraft.common.lib.network.RunicNetworking;
import thaumcraft.common.lib.runic.RunicArmor;

/** Server packets only update this independent client HUD view, never charge gameplay state. */
public final class ThaumcraftRunicClient {
    private static final ResourceLocation PARTICLES=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png");
    private static int source=-1,charge,maximum;
    private static boolean initialized;
    private ThaumcraftRunicClient() {}
    public static void init() {
        if(initialized) return; initialized=true;
        RunicNetworking.initClient(ThaumcraftRunicClient::receive,ThaumcraftRunicClient::hit);
        ClientTooltipEvent.ITEM.register((stack,lines,context,flag)-> {
            int amount=RunicArmor.finalCharge(stack);
            if(amount>0) lines.add(Component.translatable("item.runic.charge").append(" +"+amount).withStyle(ChatFormatting.GOLD));
        });
    }
    private static void receive(RunicChargePayload packet) { source=packet.source(); charge=packet.charge(); maximum=packet.maximum(); }
    public static void reset() { source=-1; charge=maximum=0; RunicHitParticle.resetMesh(); }
    private static void hit(RunicHitPayload packet) {
        var client=Minecraft.getInstance(); var level=client.level;
        if(level==null) return;
        var victim=level.getEntity(packet.source());
        if(victim==null) return;
        if(packet.target()>=0) {
            var attacker=level.getEntity(packet.target());
            float yaw=0,pitch=90;
            if(attacker!=null) {
                double dx=victim.getX()-attacker.getX(),dy=victim.getBoundingBox().getCenter().y-attacker.getBoundingBox().getCenter().y,dz=victim.getZ()-attacker.getZ();
                double horizontal=(float)Math.sqrt(dx*dx+dz*dz);
                yaw=(float)(Math.atan2(dz,dx)*180D/Math.PI)-90F; pitch=(float)(-Math.atan2(dy,horizontal)*180D/Math.PI);
            }
            client.particleEngine.add(new RunicHitParticle(level,victim,yaw,pitch));
        } else {
            if(packet.target()==-1||packet.target()==-3) client.particleEngine.add(new RunicHitParticle(level,victim,0,90));
            if(packet.target()==-1||packet.target()==-2) client.particleEngine.add(new RunicHitParticle(level,victim,0,270));
        }
    }
    /** Original10-icon bar at armor-row coordinates, including fractional-fill ceiling. */
    public static void renderHud(GuiGraphics graphics) {
        var client=Minecraft.getInstance(); var player=client.player;
        if(player==null||source!=player.getId()||player.isCreative()||client.options.hideGui||charge<=0||maximum<=0) return;
        int left=graphics.guiWidth()/2-91,top=graphics.guiHeight()-39;
        float[] old=RenderSystem.getShaderColor().clone();
        try {
            for(int index=0;index<(float)charge/(float)maximum*10F;index++) {
                graphics.setColor(1,1,1,1);
                graphics.blit(PARTICLES,left+index*8,top,160,16,9,9);
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(left,top,0); graphics.pose().scale(.5F,.5F,1);
                    graphics.setColor(1,.75F,.24F,Mth.sin(player.tickCount/4F+index)*.4F+.6F);
                    graphics.blit(PARTICLES,index*16,0,index*16,96,16,16);
                } finally { graphics.pose().popPose(); }
            }
        } finally { graphics.setColor(old[0],old[1],old[2],old[3]); }
    }
}
