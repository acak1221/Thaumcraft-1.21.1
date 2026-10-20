package thaumcraft.common.entities.monster;

import java.util.Objects;
import net.minecraft.world.entity.Entity;

/** Dedicated-server-safe bridge; the real client owns its original orbiting
 * particles, their lifetime and status6 reset. No gameplay entity is replaced. */
public final class TaintSwarmVisuals {
    public interface Effects {
        void tick(Entity target,int desired,float speed,float turnSpeed,float gravity);
        void tickScaled(Entity target,int base,int minimum,float speed,float turnSpeed,float gravity);
        void sploosh(Entity target,int count);
        void clear(Entity target);
    }
    private static Effects effects=new Effects(){public void tick(Entity e,int n,float s,float t,float g){}public void tickScaled(Entity e,int b,int m,float s,float t,float g){}public void sploosh(Entity e,int n){}public void clear(Entity e){}};
    private TaintSwarmVisuals() {}
    public static void setEffects(Effects value){effects=Objects.requireNonNull(value);}
    public static void tick(Entity target,int desired,float speed,float turnSpeed,float gravity){if(target.level().isClientSide)effects.tick(target,desired,speed,turnSpeed,gravity);}
    public static void tick(Entity target,int base,int minimum,float speed,float turnSpeed,float gravity){if(target.level().isClientSide)effects.tickScaled(target,base,minimum,speed,turnSpeed,gravity);}
    public static void sploosh(Entity target,int count){if(target.level().isClientSide)effects.sploosh(target,count);}
    public static void clear(Entity target){if(target.level().isClientSide)effects.clear(target);}
}
