package thaumcraft.common.lib.fluid;

/** Finite original Forge1.7 BlockFluidClassic rules, scoped to the purifying fluid. */
public final class PureFlowRules {
    public static final int QUANTA=8,TICK_DELAY=5,DENSITY=1000,NO_ROUTE=1000;
    private static final int[] DX={-1,1,0,0},DZ={0,0,-1,1};
    private PureFlowRules(){}
    public static int quanta(int metadata){return QUANTA-metadata;}
    public static int expectedQuanta(boolean fedAbove,int west,int east,int north,int south){
        if(fedAbove)return 7;
        int largest=-100;for(int value:new int[]{west,east,north,south})if(value>0&&value>=largest)largest=value;
        return largest-1;
    }
    /** Classic downward displacement permits vanilla water/lava's unknown MAX density. */
    public static boolean canDisplace(boolean air,boolean same,boolean veto,boolean blocksMovement,boolean portal,int density){
        if(air)return true;if(same||veto||blocksMovement||portal)return false;
        return density==Integer.MAX_VALUE||DENSITY>density;
    }
    /** Its lateral search separately rejects the two old vanilla liquid materials. */
    public static boolean canFlowInto(boolean air,boolean same,boolean veto,boolean blocksMovement,boolean portal,boolean waterOrLava,int density){
        if(air||same)return true;if(veto||blocksMovement||portal||waterOrLava)return false;
        return density==Integer.MAX_VALUE||DENSITY>density;
    }
    public interface Grid {
        boolean canFlowInto(int x,int z);
        boolean source(int x,int z);
        boolean canFlowDown(int x,int z);
    }
    public static int[] costs(Grid grid,int x,int z){
        int[] costs=new int[4];java.util.Arrays.fill(costs,NO_ROUTE);
        for(int side=0;side<4;side++){
            int xx=x+DX[side],zz=z+DZ[side];
            if(!grid.canFlowInto(xx,zz)||grid.source(xx,zz))continue;
            costs[side]=grid.canFlowDown(xx,zz)?0:cost(grid,xx,zz,1,side);
        }
        return costs;
    }
    private static int cost(Grid grid,int x,int z,int depth,int previous){
        int best=NO_ROUTE;
        for(int side=0;side<4;side++){
            if((side^1)==previous)continue;
            int xx=x+DX[side],zz=z+DZ[side];
            if(!grid.canFlowInto(xx,zz)||grid.source(xx,zz))continue;
            if(grid.canFlowDown(xx,zz))return depth;
            if(depth<4)best=Math.min(best,cost(grid,xx,zz,depth+1,side));
        }
        return best;
    }
    public static boolean[] optimal(int[] costs){
        if(costs.length!=4)throw new IllegalArgumentException("Four original horizontal directions required");
        int minimum=Math.min(Math.min(costs[0],costs[1]),Math.min(costs[2],costs[3]));
        return new boolean[]{costs[0]==minimum,costs[1]==minimum,costs[2]==minimum,costs[3]==minimum};
    }
    /** Forge's renderer selects an actual full/source corner instead of vanilla's weighted average. */
    public static float renderAverage(float... heights){
        float sum=0,full=0;int count=0;
        for(float height:heights){if(height>=.875F&&full!=1F)full=height;if(height>=0){sum+=height;count++;}}
        return full==0?sum/count:full;
    }
    /** Forge Entity.isInsideOfMaterial uses IFluidBlock.getFilledPercentage. */
    public static float filledHeight(int metadata){return Math.min(1F,(quanta(metadata)+1)/8F);}
    public static float renderHeight(int metadata){return metadata==0?.875F:quanta(metadata)/8F*.875F;}
}
