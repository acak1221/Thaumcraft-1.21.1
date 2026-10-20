package thaumcraft.common.lib.taint;

/** Official1.7 rv.g/t→rs override values and the separate TC4 tentacle additions. */
public final class TaintCombatRules {
    private TaintCombatRules(){}
    public static double strengthModifier(int amplifier){return 1.3D*(double)(amplifier+1);}
    public static double weaknessModifier(int amplifier){return (double)(-.5F*(float)(amplifier+1));}
    public static float tentacleExtras(float attribute,int strengthAmplifier,int weaknessAmplifier){
        float damage=attribute;
        if(strengthAmplifier>=0)damage+=(float)(3<<strengthAmplifier);
        if(weaknessAmplifier>=0)damage-=(float)(2<<weaknessAmplifier);
        return damage;
    }
}
