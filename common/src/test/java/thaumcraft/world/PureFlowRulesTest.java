package thaumcraft.world;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.fluid.PureFlowRules;
import static org.junit.jupiter.api.Assertions.*;

class PureFlowRulesTest {
    @Test void finiteQuantaNeverMakeAnInfiniteSourceAndTheFiveUpperFeedersGiveSeven() {
        assertEquals(8,PureFlowRules.quanta(0));assertEquals(1,PureFlowRules.quanta(7));
        assertEquals(7,PureFlowRules.expectedQuanta(false,8,8,0,0));
        assertEquals(6,PureFlowRules.expectedQuanta(false,7,0,-1,-1));
        assertEquals(-101,PureFlowRules.expectedQuanta(false,0,-1,0,-1));
        assertEquals(7,PureFlowRules.expectedQuanta(true,1,-1,-1,-1));
        assertEquals(5,PureFlowRules.TICK_DELAY);
    }
    @Test void classicDensityAndLateralMaterialRulesAreIntentionallyDifferent() {
        // Vanilla water/lava are not old Forge IFluidBlock: their density is MAX.
        assertTrue(PureFlowRules.canDisplace(false,false,false,false,false,Integer.MAX_VALUE));
        assertFalse(PureFlowRules.canFlowInto(false,false,false,false,false,true,Integer.MAX_VALUE));
        assertFalse(PureFlowRules.canDisplace(false,true,false,false,false,1000));
        assertTrue(PureFlowRules.canFlowInto(false,true,false,false,false,true,1000));
        assertTrue(PureFlowRules.canDisplace(false,false,false,false,false,999));
        assertFalse(PureFlowRules.canDisplace(false,false,false,false,false,1000));
        assertFalse(PureFlowRules.canDisplace(false,false,false,true,false,-4));
        assertFalse(PureFlowRules.canDisplace(false,false,true,false,false,Integer.MAX_VALUE));
        assertFalse(PureFlowRules.canDisplace(false,false,false,false,true,Integer.MAX_VALUE));
    }
    @Test void recursiveCostSeesTheFifthCellButDoesNotSeeTheSixth() {
        var grid=new Corridor();for(int x=1;x<=6;x++)grid.open.add(x+",0");
        grid.holes.add("5,0");assertArrayEquals(new int[]{1000,4,1000,1000},PureFlowRules.costs(grid,0,0));
        grid.holes.clear();grid.holes.add("6,0");
        assertArrayEquals(new int[]{1000,1000,1000,1000},PureFlowRules.costs(grid,0,0));
        assertArrayEquals(new boolean[]{true,true,true,true},PureFlowRules.optimal(PureFlowRules.costs(grid,0,0)));
        grid.holes.add("1,0");assertArrayEquals(new boolean[]{false,true,false,false},PureFlowRules.optimal(PureFlowRules.costs(grid,0,0)));
    }
    @Test void sourceNeighboursAreNeverRoutesAndTheRecursiveSearchCannotImmediatelyReverse() {
        var grid=new Corridor();grid.open.addAll(Set.of("-1,0","0,0","1,0","2,0"));grid.holes.add("0,0");
        // Reversing immediately into the origin hole would incorrectly give cost1.
        assertEquals(1000,PureFlowRules.costs(grid,0,0)[1]);
        grid.holes.add("2,0");assertEquals(1,PureFlowRules.costs(grid,0,0)[1]);
        grid.sources.add("1,0");assertEquals(1000,PureFlowRules.costs(grid,0,0)[1]);
        assertArrayEquals(new boolean[]{true,false,true,false},PureFlowRules.optimal(new int[]{2,4,2,1000}));
    }
    @Test void classicCornerAveragePinsActualSourcesWithoutModernWeighting() {
        assertEquals(1F,PureFlowRules.filledHeight(0));assertEquals(1F,PureFlowRules.filledHeight(1));assertEquals(.875F,PureFlowRules.filledHeight(2));assertEquals(.25F,PureFlowRules.filledHeight(7));
        assertEquals(.875F,PureFlowRules.renderHeight(0));assertEquals(.765625F,PureFlowRules.renderHeight(1));assertEquals(.109375F,PureFlowRules.renderHeight(7));
        assertEquals(.875F,PureFlowRules.renderAverage(.875F,0,0,0));
        assertEquals(1F,PureFlowRules.renderAverage(1,.875F,0,0));
        assertEquals(1F,PureFlowRules.renderAverage(.875F,1,.875F,0));
        assertEquals(.2734375F,PureFlowRules.renderAverage(.765625F,.109375F,.21875F,0));
        assertEquals(.4375F,PureFlowRules.renderAverage(-.109375F,.875F/2,-.109375F,.875F/2));
    }
    private static final class Corridor implements PureFlowRules.Grid {
        final Set<String> open=new HashSet<>(),holes=new HashSet<>(),sources=new HashSet<>();
        public boolean canFlowInto(int x,int z){return open.contains(x+","+z);}
        public boolean canFlowDown(int x,int z){return holes.contains(x+","+z);}
        public boolean source(int x,int z){return sources.contains(x+","+z);}
    }
}
