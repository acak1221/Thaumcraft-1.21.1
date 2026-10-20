package thaumcraft.common.entities.monster;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import thaumcraft.common.lib.taint.TaintCombatRules;

/** Original1.7 attack attributes on a detached copy. Never strips/replaces a live native potion modifier. */
public final class OriginalTaintCombat {
    private OriginalTaintCombat(){}
    public static float mobDamage(LivingEntity attacker){
        var live=attacker.getAttribute(Attributes.ATTACK_DAMAGE);if(live==null)return 0;
        var copy=new AttributeInstance(Attributes.ATTACK_DAMAGE,ignored->{});copy.replaceFrom(live);
        var strength=attacker.getEffect(MobEffects.DAMAGE_BOOST);
        if(strength!=null)strength.getEffect().value().createModifiers(strength.getAmplifier(),(attribute,modifier)->{
            if(attribute.equals(Attributes.ATTACK_DAMAGE)){
                copy.removeModifier(modifier.id());copy.addTransientModifier(new AttributeModifier(modifier.id(),TaintCombatRules.strengthModifier(strength.getAmplifier()),AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        });
        var weakness=attacker.getEffect(MobEffects.WEAKNESS);
        if(weakness!=null)weakness.getEffect().value().createModifiers(weakness.getAmplifier(),(attribute,modifier)->{
            if(attribute.equals(Attributes.ATTACK_DAMAGE)){
                copy.removeModifier(modifier.id());copy.addTransientModifier(new AttributeModifier(modifier.id(),TaintCombatRules.weaknessModifier(weakness.getAmplifier()),AttributeModifier.Operation.ADD_VALUE));
            }
        });
        return (float)copy.getValue();
    }
    public static float tentacleDamage(LivingEntity attacker){
        var strength=attacker.getEffect(MobEffects.DAMAGE_BOOST);var weakness=attacker.getEffect(MobEffects.WEAKNESS);
        return TaintCombatRules.tentacleExtras(mobDamage(attacker),strength==null?-1:strength.getAmplifier(),weakness==null?-1:weakness.getAmplifier());
    }
}
