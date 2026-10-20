package thaumcraft.common.lib.world;

import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.LongSupplier;

/** Finite original BlockTaintFibres lottery and render facts; world mutation remains in the actual blocks. */
public final class TaintSpreadRules {
    private TaintSpreadRules() {}
    public static int fibreType(IntUnaryOperator random,BooleanSupplier airAbove,BooleanSupplier solidBelow) {
        if(random.applyAsInt(10)==0&&airAbove.getAsBoolean()&&solidBelow.getAsBoolean()) {
            if(random.applyAsInt(10)<9)return 1;
            return random.applyAsInt(12)<10?2:3;
        }
        return 0;
    }
    public static int poisonBound(boolean fibres,boolean player) { return fibres?(player?1000:500):(player?100:20); }
    public static int poisonDuration(boolean player) { return player?80:160; }
    /** Original nextInt(rate*5) on every representable bound; larger positive rates avoid its int-overflow crash. */
    public static boolean biomeChance(int rate,IntUnaryOperator nextInt,LongSupplier nextLong) {
        if(rate<=0)return false;
        long bound=(long)rate*5L;if(bound<=Integer.MAX_VALUE)return nextInt.applyAsInt((int)bound)==0;
        // RandomSource has no nextLong(bound). Use the standard unbiased positive-long rejection formula.
        long bits,value;do{bits=nextLong.getAsLong()>>>1;value=bits%bound;}while(bits-value+(bound-1L)<0L);
        return value==0L;
    }
    public static int overlay(int side,int x,int y,int z) {
        Random random=new Random(side+y+x*z);
        return random.nextInt(100)<95?0:1+random.nextInt(3);
    }
    public static double[] grassOffset(int x,int y,int z) {
        long value=(long)(x*3129871)^(long)z*116129781L^(long)y;
        value=value*value*42317861L+value*11L;
        return new double[]{((double)((float)(value>>16&15L)/15F)-.5D)*.5D,((double)((float)(value>>24&15L)/15F)-.5D)*.5D};
    }
}
