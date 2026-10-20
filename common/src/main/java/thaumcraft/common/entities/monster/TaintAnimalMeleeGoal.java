package thaumcraft.common.entities.monster;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.pathfinder.Path;

/** Original TC4 AIAttackOnCollide, used by every tainted animal of this unit.
 *
 * <p>The current navigation supplies the path, but the original ten-tick attack
 * gate, the original squared-distance reach, the original path retry penalties
 * and the original flat attribute damage are kept: the native
 * MeleeAttackGoal uses a twenty-tick gate, a native reach test and the native
 * attack-strength scaling, none of which existed in TC4.
 *
 * <p>The original EntityAISwimming has no1.21 goal class, so the nested Swim goal
 * keeps its behaviour: the mob rises while its head is submerged. */
public class TaintAnimalMeleeGoal extends Goal {
    private final PathfinderMob attacker;
    private final Class<? extends LivingEntity> targetClass;
    private final double speedTowardsTarget;
    private final boolean longMemory;
    private Path path;
    private int pathDelay,attackTick,failedPathFindingPenalty;
    private double oldX,oldY,oldZ;

    public TaintAnimalMeleeGoal(PathfinderMob attacker,Class<? extends LivingEntity> targetClass,double speed,boolean longMemory) {
        this.attacker=attacker;this.targetClass=targetClass;this.speedTowardsTarget=speed;this.longMemory=longMemory;
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean canUse() {
        LivingEntity target=attacker.getTarget();
        if(target==null||!target.isAlive())return false;
        if(targetClass!=null&&!targetClass.isInstance(target))return false;
        if(--pathDelay<=0) {
            path=attacker.getNavigation().createPath(target,0);
            pathDelay=4+attacker.getRandom().nextInt(7);
            return path!=null;
        }
        return true;
    }
    /** Original: without long memory the goal only continues while the navigator still has a path. */
    @Override public boolean canContinueToUse() {
        LivingEntity target=attacker.getTarget();
        if(target==null||!target.isAlive())return false;
        return longMemory || !attacker.getNavigation().isDone();
    }
    @Override public void start() {
        attacker.getNavigation().moveTo(path,speedTowardsTarget);
        pathDelay=0;
    }
    @Override public void stop() { attacker.getNavigation().stop(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void tick() {
        LivingEntity target=attacker.getTarget();
        if(target==null)return;
        attacker.getLookControl().setLookAt(target,30.0F,30.0F);
        double distance=attacker.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ());
        // Original reach: the attacker's doubled width squared plus the target width.
        double reach=attacker.getBbWidth()*2.0F*attacker.getBbWidth()*2.0F+target.getBbWidth();
        --pathDelay;
        if(attackTick>0)--attackTick;
        if((longMemory||attacker.getSensing().hasLineOfSight(target))&&pathDelay<=0
                &&(oldX==0.0D&&oldY==0.0D&&oldZ==0.0D||target.distanceToSqr(oldX,oldY,oldZ)>=1.0D||attacker.getRandom().nextFloat()<0.05F)) {
            oldX=target.getX();oldY=target.getBoundingBox().minY;oldZ=target.getZ();
            pathDelay=failedPathFindingPenalty+4+attacker.getRandom().nextInt(7);
            Path current=attacker.getNavigation().getPath();
            var end=current==null?null:current.getEndNode();
            failedPathFindingPenalty=end!=null&&target.distanceToSqr(end.x,end.y,end.z)<1.0D ? 0 : failedPathFindingPenalty+10;
            if(distance>1024.0D)pathDelay+=10;
            else if(distance>256.0D)pathDelay+=5;
            if(!attacker.getNavigation().moveTo(target,speedTowardsTarget))pathDelay+=15;
        }
        if(distance<=reach&&attackTick<=0) {
            attackTick=10;
            if(!attacker.getMainHandItem().isEmpty())attacker.swing(InteractionHand.MAIN_HAND);
            beforeAttack(target);
            originalAttack(target);
        }
    }
    /** Original EntityMob.attackEntityAsMob hook for the mobs that announce their attack. */
    protected void beforeAttack(LivingEntity target) {}
    /** Original EntityMob.attackEntityAsMob: the flat attack damage attribute with the original potion math. */
    private void originalAttack(LivingEntity target) {
        var server=attacker.level() instanceof ServerLevel level?level:null;
        float damage=OriginalTaintCombat.mobDamage(attacker);
        DamageSource source=attacker.damageSources().mobAttack(attacker);
        if(server!=null)damage=EnchantmentHelper.modifyDamage(server,attacker.getMainHandItem(),target,source,damage);
        boolean hit=target.hurt(source,damage);
        if(hit) {
            // The native1.21 melee pipeline owns the knockback; the original1.7 EntityMob had none.
            float knockback=server==null ? 0.0F : EnchantmentHelper.modifyKnockback(server,attacker.getMainHandItem(),target,source,0.0F);
            if(knockback>0.0F) {
                target.push(-(float)(Math.sin(attacker.getYRot()*(float)Math.PI/180.0F)*knockback*0.5D),
                        0.1D,(float)(Math.cos(attacker.getYRot()*(float)Math.PI/180.0F)*knockback*0.5D));
                attacker.setDeltaMovement(attacker.getDeltaMovement().multiply(0.6D,1.0D,0.6D));
            }
            if(server!=null)EnchantmentHelper.doPostAttackEffects(server,target,source);
            attacker.setLastHurtMob(target);
            afterAttack(target);
        }
    }
    /** Original EntityMob.attackEntityAsMob hook for the mobs that poison on a successful hit. */
    protected void afterAttack(LivingEntity target) {}

    /** Original1.7 EntityAISwimming: no1.21 goal class exists, so the mob keeps swimming up by itself. */
    public static final class Swim extends Goal {
        private final PathfinderMob mob;
        public Swim(PathfinderMob mob) { this.mob=mob;setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if(mob.isPassenger()||!mob.isInWater())return false;
            BlockPos eye=BlockPos.containing(mob.getX(),mob.getEyeY(),mob.getZ());
            return mob.level().getFluidState(eye).is(FluidTags.WATER);
        }
        @Override public void tick() {
            // Original moveRelative(0,1,0,1) adds exactly one to the vertical motion.
            mob.setDeltaMovement(mob.getDeltaMovement().add(0.0D,1.0D,0.0D));
        }
    }
}
