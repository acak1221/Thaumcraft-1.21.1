package thaumcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import thaumcraft.client.fx.particles.SanityBubbleParticle;
import thaumcraft.common.items.ItemSanitySoap;

/** Original local soap sounds, particle-setting counts and exact float-coordinate lather samples. */
public final class ThaumcraftEldritchItemsClient {
    private static boolean initialized;
    private ThaumcraftEldritchItemsClient(){}
    public static void init(){if(initialized)return;initialized=true;ItemSanitySoap.setClientEffects(ThaumcraftEldritchItemsClient::lather);}
    private static void lather(Player player,boolean finished){
        if(!(player.level() instanceof ClientLevel level)||Minecraft.getInstance().level!=level)return;
        if(finished)level.playLocalSound(player.getX(),player.getY(),player.getZ(),sound("craftstart"),SoundSource.PLAYERS,.25F,1F,false);
        else if(level.random.nextFloat()<.2F)level.playLocalSound(player.getX(),player.getY(),player.getZ(),sound("roots"),SoundSource.PLAYERS,.1F,1.5F+level.random.nextFloat()*.2F,false);
        var setting=Minecraft.getInstance().options.particles().get();int base=finished?20:5;
        int count=setting==ParticleStatus.MINIMAL?0:setting==ParticleStatus.DECREASED?base:base*2;
        for(int i=0;i<count;i++)bubble(level,(float)player.getX()-.5F+level.random.nextFloat()*(finished?1.5F:1F),
                (float)player.getBoundingBox().minY+level.random.nextFloat()*player.getBbHeight(),
                (float)player.getZ()-.5F+level.random.nextFloat()*(finished?1.5F:1F),1,finished?.7F:.8F,.9F);
    }
    /** The same original crucibleBubble(age1) API is available to actual spa/purifying-fluid effects. */
    public static void bubble(ClientLevel level,float x,float y,float z,float red,float green,float blue){
        bubble(level,x,y,z,red,green,blue,1);
    }
    public static void bubble(ClientLevel level,float x,float y,float z,float red,float green,float blue,int age){
        bubbleAlpha(level,x,y,z,red,green,blue,age,1F);
    }
    public static void bubbleAlpha(ClientLevel level,float x,float y,float z,float red,float green,float blue,int age,float alpha){
        if(Minecraft.getInstance().level==level){var particle=new SanityBubbleParticle(level,x,y,z,red,green,blue,age);particle.setAlpha(alpha);Minecraft.getInstance().particleEngine.add(particle);}
    }
    private static SoundEvent sound(String id){return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));}
}
