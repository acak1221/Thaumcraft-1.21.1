package thaumcraft.common.lib.world;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.tiles.TileNode.NodeModifier;
import thaumcraft.common.tiles.TileNode.NodeType;

/** Declared biome100/aer and unavailable environment; scripted RNG validates every original bound and draw position. */
class NodeGenerationTest {
    private record Draw(int bound,int value){}
    private static Draw i(int bound,int value){return new Draw(bound,value);}
    private static Draw b(boolean value){return new Draw(-1,value?1:0);}
    private static final class Script extends LegacyRandomSource {
        final ArrayDeque<Draw> draws;
        Script(Draw...draws){super(0);this.draws=new ArrayDeque<>(List.of(draws));}
        @Override public int nextInt(int bound){var draw=draws.removeFirst();assertEquals(draw.bound(),bound,"original RNG bound/order");assertTrue(draw.value()>=0&&draw.value()<bound);return draw.value();}
        @Override public boolean nextBoolean(){var draw=draws.removeFirst();assertEquals(-1,draw.bound(),"original boolean position");return draw.value()==1;}
        void exhausted(){assertTrue(draws.isEmpty(),"no omitted original RNG calls");}
    }
    @Test void ordinaryAndSmallQuarterAuraDifferBeforeValueDraw(){
        var ordinary=new Script(i(18,1),i(9,1),i(50,0),b(false),b(false),b(false),i(25,0));
        var full=NodeGeneration.plan(100,r->Aspect.AIR,false,BlockPos.ZERO,ordinary,false,false,false,p->null);
        assertEquals(NodeType.NORMAL,full.type());assertNull(full.modifier());assertEquals(50,full.aspects().getAmount(Aspect.AIR));ordinary.exhausted();
        var small=new Script(i(18,1),i(9,1),i(12,0),b(false),b(false),b(false),i(25,0));
        var quarter=NodeGeneration.plan(100,r->Aspect.AIR,false,BlockPos.ZERO,small,false,false,true,p->null);
        assertEquals(12,quarter.aspects().getAmount(Aspect.AIR));small.exhausted();
        var changed=quarter.aspects();changed.add(Aspect.AIR,1000);assertEquals(12,quarter.aspects().getAmount(Aspect.AIR),"plan cannot expose mutable aura aliases");
    }
    @Test void silverwoodPureSkipsSpecialAndTaintRollsAndQuartersOnlyOnce(){
        var rng=new Script(i(9,0),i(3,0),i(12,11),b(false),b(false),b(false),b(true),i(25,0),i(25,0));
        var plan=NodeGeneration.plan(100,r->Aspect.AIR,true,BlockPos.ZERO,rng,true,false,true,p->null);
        assertEquals(NodeType.PURE,plan.type());assertEquals(NodeModifier.BRIGHT,plan.modifier());
        assertEquals(11,plan.aspects().getAmount(Aspect.AIR));assertEquals(11,plan.aspects().getAmount(Aspect.LIFE));assertEquals(22,plan.aspects().visSize());rng.exhausted();
    }
    @Test void forcedDarkAddsOriginalIndependentTypeTagsBeforeSpread(){
        var rng=new Script(i(9,1),i(50,0),b(false),b(false),b(false),b(true),b(false),b(true),b(false),i(25,0),i(50,0),i(50,0));
        var plan=NodeGeneration.plan(100,r->Aspect.AIR,false,BlockPos.ZERO,rng,false,true,false,p->null);
        assertEquals(NodeType.DARK,plan.type());assertEquals(25,plan.aspects().getAmount(Aspect.AIR));assertEquals(12,plan.aspects().getAmount(Aspect.DEATH));assertEquals(12,plan.aspects().getAmount(Aspect.ENTROPY));rng.exhausted();
    }
    @Test void taintedBiomeDoubleFloatBoostPrecedesSmallDivision(){
        var rng=new Script(i(18,1),i(9,1),b(true),i(28,0),b(false),b(false),b(false),i(25,0));
        var plan=NodeGeneration.plan(100,r->Aspect.AIR,true,BlockPos.ZERO,rng,false,false,true,p->null);
        assertEquals(NodeType.TAINTED,plan.type());assertEquals(28,plan.aspects().getAmount(Aspect.AIR));rng.exhausted();
    }
    @Test void hungryTypeKeepsHungerAndConditionalGreed(){
        var rng=new Script(i(18,0),i(10,9),i(9,1),i(50,0),b(false),b(false),b(false),b(true),i(25,0),i(25,0),i(50,0));
        var plan=NodeGeneration.plan(100,r->Aspect.AIR,false,BlockPos.ZERO,rng,false,false,false,p->null);
        assertEquals(NodeType.HUNGRY,plan.type());assertEquals(20,plan.aspects().getAmount(Aspect.AIR));assertEquals(20,plan.aspects().getAmount(Aspect.HUNGER));assertEquals(10,plan.aspects().getAmount(Aspect.GREED));rng.exhausted();
    }
}
