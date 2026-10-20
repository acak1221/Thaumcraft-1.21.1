package thaumcraft.client;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.world.entity.Entity;
import thaumcraft.client.fx.particles.TaintSplooshParticle;
import thaumcraft.client.fx.particles.TaintSwarmParticle;
import thaumcraft.client.renderers.entity.RenderTaintSpider;
import thaumcraft.client.renderers.entity.RenderTaintSpore;
import thaumcraft.client.renderers.entity.RenderTaintSporeSwarmer;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.entities.monster.TaintSwarmVisuals;

/** Actual client particle provider shared by Spore, Swarmer and living Swarm.
 * One dead entry removed and one replacement added per original entity tick. */
public final class ThaumcraftTaintSporesClient {
    private static final Map<Entity,ArrayList<TaintSwarmParticle>> SWARMS=new WeakHashMap<>();
    private static boolean initialized;
    private ThaumcraftTaintSporesClient(){}
    public static void init(){
        if(initialized)return;initialized=true;
        EntityRendererRegistry.register(ThaumcraftTaint.TAINT_SPIDER,RenderTaintSpider::new);
        EntityRendererRegistry.register(ThaumcraftTaint.TAINT_SPORE,RenderTaintSpore::new);
        EntityRendererRegistry.register(ThaumcraftTaint.TAINT_SWARMER,RenderTaintSporeSwarmer::new);
        TaintSwarmVisuals.setEffects(new TaintSwarmVisuals.Effects(){
            public void tick(Entity target,int desired,float speed,float turn,float gravity){visualTick(target,desired,speed,turn,gravity);}
            public void tickScaled(Entity target,int base,int minimum,float speed,float turn,float gravity){var mode=Minecraft.getInstance().options.particles().get();int count=mode==ParticleStatus.MINIMAL?0:mode==ParticleStatus.DECREASED?base:base*2;visualTick(target,Math.max(count,minimum),speed,turn,gravity);}
            public void sploosh(Entity target,int count){TaintSplooshParticle.emit(target,count);}
            public void clear(Entity target){var particles=SWARMS.remove(target);if(particles!=null)particles.forEach(TaintSwarmParticle::remove);}
        });
        ClientTickEvent.CLIENT_POST.register(client->SWARMS.keySet().removeIf(entity->entity.isRemoved()||entity.level()!=client.level));
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level->reset());ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player->reset());
    }
    private static void visualTick(Entity target,int desired,float speed,float turn,float gravity){
        var client=Minecraft.getInstance();if(client.level!=target.level())return;
        var particles=SWARMS.computeIfAbsent(target,ignored->new ArrayList<>());
        for(int i=0;i<particles.size();i++)if(particles.get(i)==null||!particles.get(i).isAlive()){particles.remove(i);break;}
        if(particles.size()<desired)particles.add(TaintSwarmParticle.spawn(target,speed,turn,gravity));
    }
    public static void reset(){SWARMS.values().forEach(list->list.forEach(particle->{if(particle!=null)particle.remove();}));SWARMS.clear();TaintSwarmParticle.resetBuzz();}
}
