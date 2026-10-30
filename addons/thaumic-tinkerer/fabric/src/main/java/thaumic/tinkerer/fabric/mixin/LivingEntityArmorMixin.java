/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import thaumic.tinkerer.common.hooks.TinkererEvents;

/**
 * Posts {@link TinkererEvents#LIVING_ARMOR_ABSORB}. {@code LivingEntity.getDamageAfterArmorAbsorb} is the armour step of
 * {@code actuallyHurt} (players reach it through {@code Player.actuallyHurt}, which calls the same method): for a source that
 * does not bypass armour it wears the pieces ({@code hurtArmor}) and returns the damage that is left after
 * {@code CombatRules.getDamageAfterAbsorb}. The method is wrapped: the original runs untouched (the wear included), then the
 * listeners see how much it took off ({@code damage - result}) and may replace that amount. The damage handed back is the
 * game's own value when nobody changed anything (no float rounding of the round trip, and no NaN for an infinite blow) and
 * {@code damage - absorbed} when a listener did: that rule is {@link TinkererEvents#damageAfterArmorAbsorb}. The tests of the
 * source (a source that bypasses armour) and of the amount live in {@link TinkererEvents#postLivingArmorAbsorb}, the one rule for
 * both loaders.
 *
 * <p><b>Priority 900, below the default, on purpose.</b> Thaumcraft wraps the same method ({@code LivingEntityMagicWardMixin}: the
 * priority-1 pass of the original's {@code ISpecialArmor}, the worn wards, comes off the damage first, then the ordinary armour sees
 * what is left). A mixin of a lower priority is applied first, so its wrapper is the inner one: this event is posted with the damage
 * that is left after the ward pass, as the priority-0 pieces of Forge's {@code ApplyArmor} saw it, and not before it (with the
 * priority raised to 1100 the wrapper becomes the outer one, the event sees the whole blow and a listener that replaces the amount
 * throws the ward's share away; the GameTest of the event checks it with a Fortress chestplate). With the default priority the order
 * is only that of the loading of the two configs.</p>
 */
@Mixin(value = LivingEntity.class, priority = 900)
public abstract class LivingEntityArmorMixin {
    @WrapMethod(method = "getDamageAfterArmorAbsorb")
    private float thaumictinkerer$armorAbsorb(DamageSource source, float damage, Operation<Float> original) {
        return TinkererEvents.damageAfterArmorAbsorb((LivingEntity) (Object) this, source, damage, original.call(source, damage));
    }
}
