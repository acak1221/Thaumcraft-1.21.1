package thaumcraft.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import thaumcraft.client.fx.particles.MonsterSpriteParticle;
import thaumcraft.client.fx.particles.ShockSparkParticle;
import thaumcraft.client.renderers.entity.RenderFocusOrb;
import thaumcraft.common.blocks.BlockShockTrap;
import thaumcraft.common.config.ThaumcraftShockOrbs;
import thaumcraft.common.entities.projectile.EntityShockOrb;

public final class ThaumcraftShockOrbsClient {
    private static boolean initialized;
    private ThaumcraftShockOrbsClient() {}
    public static void init() {
        if(initialized)return;initialized=true;
        EntityRendererRegistry.register(ThaumcraftShockOrbs.SHOCK_ORB,ctx->new RenderFocusOrb<EntityShockOrb>(ctx,false));
        EntityShockOrb.setClientImpact(orb->{if(orb.level() instanceof ClientLevel level)MonsterSpriteParticle.burst(level,orb.getX(),orb.getY(),orb.getZ(),3);});
        BlockShockTrap.setClientEffect((world,pos)-> {
            if(!(world instanceof ClientLevel level))return;
            float h=level.random.nextFloat()*.33F;
            double x=pos.getX()+level.random.nextFloat(),y=pos.getY()+.1515F+h/2F,z=pos.getZ()+level.random.nextFloat();
            var spark=new ShockSparkParticle(level,x,y,z,.33F+h);spark.setColor(.65F+level.random.nextFloat()*.1F,1,1);
            Minecraft.getInstance().particleEngine.add(spark);
            if(level.random.nextInt(50)==0)level.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","jacobs")),SoundSource.BLOCKS,.5F,1+(level.random.nextFloat()-level.random.nextFloat())*.2F,false);
        });
    }
}
