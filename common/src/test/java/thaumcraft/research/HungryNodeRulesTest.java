package thaumcraft.research;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.research.HungryNodeRules;

class HungryNodeRulesTest {
    @Test void fifteenBlockSphereUsesSquaredFalloffAndStrongerVerticalForce() {
        assertEquals(.0375D,HungryNodeRules.pull(new Vec3(7.5,0,0)).x,1e-12);
        assertEquals(.0625D,HungryNodeRules.pull(new Vec3(0,7.5,0)).y,1e-12);
        assertEquals(Vec3.ZERO,HungryNodeRules.pull(new Vec3(15,0,0)));
        assertEquals(Vec3.ZERO,HungryNodeRules.pull(new Vec3(15.001,0,0)));
        assertEquals(Vec3.ZERO,HungryNodeRules.pull(Vec3.ZERO));
        assertEquals(Vec3.ZERO,HungryNodeRules.pull(new Vec3(Double.NaN,0,0)));
    }
    @Test void depletedCurrentRefillsOneWithNoCapacityChanceAndNoMutationOfInputs() {
        var current=new AspectList().add(Aspect.EARTH,2); var base=new AspectList().add(Aspect.EARTH,5);
        var bounds=new ArrayList<Integer>();
        var meal=HungryNodeRules.meal(new AspectList().add(Aspect.EARTH,2),current,base,bound->{bounds.add(bound);return 0;});
        assertTrue(meal.refill());assertFalse(meal.grow());assertEquals(Aspect.EARTH,meal.aspect());
        assertEquals(List.of(1),bounds);assertEquals(2,current.getAmount(Aspect.EARTH));assertEquals(5,base.getAmount(Aspect.EARTH));
    }
    @Test void distinctPrimalSelectionIsUnweightedAndGrowthUsesTwiceBasePlusOne() {
        var tags=new AspectList().add(Aspect.EARTH,100).add(Aspect.FIRE,2);
        var current=new AspectList().add(Aspect.FIRE,3);var base=current.copy();var bounds=new ArrayList<Integer>();
        var miss=HungryNodeRules.meal(tags,current,base,bound->{bounds.add(bound);return bound==2?1:2;});
        assertEquals(Aspect.FIRE,miss.aspect());assertFalse(miss.refill());assertFalse(miss.grow());assertEquals(List.of(2,7),bounds);
        var hit=HungryNodeRules.meal(tags,current,base,bound->1);
        assertTrue(hit.grow());assertFalse(hit.refill());assertEquals(3,current.getAmount(Aspect.FIRE));assertEquals(3,base.getAmount(Aspect.FIRE));
    }
    @Test void aPreviouslyAbsentPrimalGrowsOnlyItsBaseAndEmptyMealsDoNotDraw() {
        var meal=HungryNodeRules.meal(new AspectList().add(Aspect.ORDER,2),new AspectList(),new AspectList(),bound->{assertEquals(1,bound);return 0;});
        assertTrue(meal.grow());assertFalse(meal.refill());assertEquals(Aspect.ORDER,meal.aspect());
        assertNull(HungryNodeRules.meal(new AspectList(),new AspectList(),new AspectList(),bound->{fail("No empty meal draw");return 0;}));
    }
    @Test void blockHardnessBoundsIncludeZeroAndExcludeExactlyFiveAndUnbreakable() {
        assertTrue(HungryNodeRules.breakable(0));assertTrue(HungryNodeRules.breakable(4.999F));
        assertFalse(HungryNodeRules.breakable(5));assertFalse(HungryNodeRules.breakable(-1));assertFalse(HungryNodeRules.breakable(Float.NaN));
    }
}
