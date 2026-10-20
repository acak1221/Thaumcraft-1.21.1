package thaumcraft.common.lib.research;

import java.util.Random;
import java.util.function.IntUnaryOperator;
import thaumcraft.api.aspects.Aspect;

/** Original ItemResource metadata15: world draws are independent of tooltip's local RNG. */
public final class PrimalCharmRules {
    private static final Aspect[] PRIMALS={Aspect.AIR,Aspect.EARTH,Aspect.FIRE,Aspect.WATER,Aspect.ORDER,Aspect.ENTROPY};
    private PrimalCharmRules() {}
    public static int worldRoll(IntUnaryOperator random) { return random.applyAsInt(20000); }
    public static boolean emits(int roll) { return roll>=0&&roll<20; }
    public static boolean clues(int roll,boolean focusComplete,boolean clueComplete) { return roll==42&&!focusComplete&&!clueComplete; }
    public static Aspect aspect(IntUnaryOperator random) { return PRIMALS[random.applyAsInt(6)]; }
    /** int addition/overflow precedes widening, exactly as stack.hashCode()+player.ticksExisted/120. */
    public static int tooltip(int stackHash,int playerTicks) {
        Random random=new Random(stackHash+playerTicks/120);
        return random.nextInt(200)<25?random.nextInt(5):-1;
    }
}
