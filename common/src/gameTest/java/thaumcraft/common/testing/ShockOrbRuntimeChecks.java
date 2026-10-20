package thaumcraft.common.testing;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.common.config.ThaumcraftShockOrbs;
import thaumcraft.common.entities.projectile.EntityShockOrb;

/** Declared actors/floor/motion/RNG; actual production projectile collision, blast/fields, native contact and random tick. No fake impact callback. */
public final class ShockOrbRuntimeChecks {
    private ShockOrbRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        var f=NodeTransductionRuntimeChecks.fixture(test,false); var owned=new ArrayList<Entity>();
        try {
            f.open();
            Vec3 origin=Vec3.atCenterOf(f.base);
            var caster=cow(f,owned,origin.add(-1,0,0));
            var corner=cow(f,owned,origin.add(3.6,0,3.6));
            var hidden=cow(f,owned,origin.add(-3,0,0));
            f.put(f.base.west(2),Blocks.STONE);f.put(f.base.below(),Blocks.STONE);
            var orb=new EntityShockOrb(f.level,caster);owned.add(orb);orb.configure(4,5);orb.setPos(origin);f.level.addFreshEntity(orb);
            seed(orb,20261020L);
            corner.setYRot(90); Vec3 reflected=corner.getLookAngle().scale(.9);
            test.assertTrue(orb.hurt(f.level.damageSources().mobAttack(corner),1) && orb.getOwner()==caster && orb.getDeltaMovement().equals(reflected),
                    "deflection copies attacker look*.9 and retains actual original caster ownership");
            orb.setDeltaMovement(0,-1,0);orb.tick();
            test.assertTrue(orb.isRemoved() && caster.getHealth()==15 && corner.getHealth()==15 && hidden.getHealth()==20,
                    "real collision shocks caster and AABB corner beyond radial4, but base-to-base LOS wall protects hidden victim");
            int fields=0;BlockPos field=null;
            for(BlockPos pos:BlockPos.betweenClosed(f.base.offset(-4,-2,-4),f.base.offset(4,1,4))) if(f.level.getBlockState(pos).is(ThaumcraftShockOrbs.SHOCK_TRAP.get())) {
                fields++;field=pos.immutable();
                test.assertTrue(Math.abs(pos.getX()-f.base.getX())<4 && Math.abs(pos.getZ()-f.base.getZ())<4
                                && !f.level.getBlockState(pos.below()).isAir(),"real twenty-attempt triangular field stays above actual nonair ground");
            }
            test.assertTrue(fields>0 && fields<=20,"actual Earth Shock creates a bounded positive set of at most20 fields: "+fields);
            var state=f.level.getBlockState(field);
            test.assertTrue(!state.isAir() && state.getShape(f.level,field,CollisionContext.empty()).isEmpty()
                            && state.getCollisionShape(f.level,field,CollisionContext.empty()).isEmpty() && state.canBeReplaced()
                            && state.getLightEmission()==8 && state.getDestroySpeed(f.level,field)==100 && state.getBlock().getExplosionResistance()==50
                            && state.getBlock().asItem()==net.minecraft.world.item.Items.AIR && f.level.getBlockEntity(field)==null,
                    "native shock field has original100/50/light8/replaceable/noitem/noBE, empty shapes, and required nonAIR contact adapter");
            var contact=cow(f,owned,Vec3.atCenterOf(field));contact.setDeltaMovement(1,.25,2);
            state.entityInside(f.level,field,contact);
            test.assertTrue((contact.getHealth()==18||contact.getHealth()==19) && contact.getDeltaMovement().equals(new Vec3(.8,.25,1.6)),
                    "native field contact deals magic1..2 then damps only horizontal motion by.8");
            f.put(field,ThaumcraftShockOrbs.SHOCK_TRAP.get());f.level.getBlockState(field).randomTick(f.level,field,RandomSource.create(0));
            test.assertTrue(f.level.getBlockState(field).isAir(),"actual native random tick removes a transient shock field");
            var aging=new EntityShockOrb(ThaumcraftShockOrbs.SHOCK_ORB.get(),f.level);owned.add(aging);aging.setPos(Vec3.atCenterOf(f.base.east(9).above(2)));aging.setNoGravity(true);f.level.addFreshEntity(aging);
            for(int tick=0;tick<500;tick++)aging.tick();
            test.assertTrue(!aging.isRemoved() && aging.getAge()==500,"original orb survives exactly500 projectile updates");
            aging.tick();test.assertTrue(aging.isRemoved() && aging.getAge()==501,"original >500 boundary removes on update501");
        } finally { for(Entity entity:owned)entity.discard();f.close(); }
        test.succeed();
    }
    private static LivingEntity cow(TubeVariantsRuntimeChecks.Fixture f,ArrayList<Entity> owned,Vec3 pos) {
        var cow=EntityType.COW.create(f.level);if(cow==null)throw new IllegalStateException("Native cow factory");
        cow.setPos(pos);cow.setNoAi(true);cow.setNoGravity(true);f.level.addFreshEntity(cow);owned.add(cow);return cow;
    }
    private static void seed(EntityShockOrb orb,long seed) {
        try {
            var fields=java.util.Arrays.stream(Entity.class.getDeclaredFields()).filter(field->field.getType()==RandomSource.class).toList();
            if(fields.size()!=1)throw new IllegalStateException("Expected one native entity RNG field");
            var field=fields.getFirst();field.setAccessible(true);((RandomSource)field.get(orb)).setSeed(seed);
        } catch(ReflectiveOperationException failure) { throw new IllegalStateException("Declared native projectile RNG input",failure); }
    }
}
