/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.enchantment;

import java.util.UUID;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import thaumic.tinkerer.common.core.helper.ItemNBTHelper;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * Pounce, Final Strike, Valiance, Focused Strikes and Dispersed Strikes: the multiplier of the damage of a melee attack.
 *
 * <p>1.7.10 changed {@code event.ammount} in {@code LivingHurtEvent}. Architectury's {@code EntityEvent.LIVING_HURT} can
 * only veto a hit, and a veto makes {@code Player.attack} treat the blow as a miss (no knockback, no fire aspect, no wear
 * of the weapon), so the damage cannot be changed there. The multiplier is applied one step earlier instead: just before
 * the player's attack runs ({@code PlayerEvent.ATTACK_ENTITY}, or {@link #beforeAttack} for an attack that does not come from
 * the network, such as the Animation Tablet's), a transient modifier on the player's {@code ATTACK_DAMAGE} attribute makes
 * the damage that {@code Player.attack} computes {@code factor} times what it would have been. The attack formula of
 * 1.21.1 (the cooldown scale, the critical hit, the bonus of the weapon's enchantments that the cooldown scales on its
 * own) is evaluated here to find the modifier, so that the damage that reaches the victim is {@code factor} times the
 * unmodified damage (see {@link #solveBase}). The modifier is removed when the blow reaches the victim
 * ({@link #afterAttack}), and at the latest in the next tick of the player.</p>
 */
public final class AttackBoost {
    /** Id of the transient attribute modifier on {@code ATTACK_DAMAGE}. */
    public static final ResourceLocation MODIFIER = LibMisc.id("enchant_attack_boost");

    /** The keys of the strike counter on the weapon, as in 1.7.10. */
    public static final String NBT_LAST_TARGET = "TTEnchantLastTarget";
    public static final String NBT_LAST_TARGET_2 = "TTEnchantLastTarget2";
    public static final String NBT_SUCCESSIVE_STRIKE = "TTEnchantSuccessiveStrike";

    private AttackBoost() {}

    /**
     * Call this before {@code player.attack(target)} runs. Works out the factor of the enchantments of the held item and of
     * the leggings and arms the attack with it; {@link #afterAttack} (or the next tick of the player) disarms it. Server
     * side only; a client, a non-living target or an attack without enchantments is left alone.
     */
    public static void beforeAttack(Player player, Entity target) {
        afterAttack(player);
        if (!(player.level() instanceof ServerLevel level) || !(target instanceof LivingEntity victim)) return;

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return;

        int pounce = ModEnchantments.level(player.getItemBySlot(EquipmentSlot.LEGS), ModEnchantments.pounce);
        int finalStrike = ModEnchantments.level(held, ModEnchantments.finalStrike);
        int valiance = ModEnchantments.level(held, ModEnchantments.valiance);
        int focused = ModEnchantments.level(held, ModEnchantments.focusedStrike);
        int dispersed = ModEnchantments.level(held, ModEnchantments.dispersedStrikes);
        if (pounce <= 0 && finalStrike <= 0 && valiance <= 0 && focused <= 0 && dispersed <= 0) return;

        boolean pounceInAir = pounce > 0 && level.getBlockState(
                BlockPos.containing(player.getX(), Math.floor(player.getY()) - 1, player.getZ())).isAir();
        int roll = finalStrike > 0 ? player.getRandom().nextInt(19) : 0;

        int successive = 0;
        if (focused > 0 || dispersed > 0) {
            UUID lastTarget = new UUID(ItemNBTHelper.getLong(held, NBT_LAST_TARGET, 0L),
                    ItemNBTHelper.getLong(held, NBT_LAST_TARGET_2, 0L));
            int stored = ItemNBTHelper.getInt(held, NBT_SUCCESSIVE_STRIKE, 0);
            UUID targetId = victim.getUUID();
            successive = EnchantmentFormulas.nextSuccessiveStrikes(lastTarget, stored, targetId);
            if (!lastTarget.equals(targetId)) {
                ItemNBTHelper.setLong(held, NBT_LAST_TARGET, targetId.getMostSignificantBits());
                ItemNBTHelper.setLong(held, NBT_LAST_TARGET_2, targetId.getLeastSignificantBits());
            }
            ItemNBTHelper.setInt(held, NBT_SUCCESSIVE_STRIKE, successive);
        }

        float factor = EnchantmentFormulas.damageFactor(new EnchantmentFormulas.Attack(pounce, pounceInAir, finalStrike, roll,
                valiance, player.getHealth(), player.getMaxHealth(), focused, dispersed, successive));
        if (factor == 1F) return;

        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (base <= 0) return;

        double solved = solveBase(base, factor, attackDamageOf(player, level, held, victim));
        double amount = solved / base - 1D;
        if (Math.abs(amount) < 1.0E-6) return;

        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attribute != null) {
            attribute.addOrUpdateTransientModifier(
                    new AttributeModifier(MODIFIER, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** Disarms the attack of the player (the modifier of {@link #beforeAttack}). Safe to call at any time. */
    public static void afterAttack(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attribute != null && attribute.hasModifier(MODIFIER)) attribute.removeModifier(MODIFIER);
    }

    /**
     * The damage {@code Player.attack} of 1.21.1 sends to the victim for a base attack damage {@code base}, written out for
     * the current state of the player: the enchantment bonus of the weapon is worked out for the base and scales with the
     * cooldown; the base scales with {@code 0.2 + 0.8 * cooldown^2}, the bonus of the item is added, and a critical hit
     * multiplies that by 1.5.
     */
    static DoubleUnaryOperator attackDamageOf(Player player, ServerLevel level, ItemStack weapon, LivingEntity victim) {
        DamageSource source = player.damageSources().playerAttack(player);
        float strength = player.getAttackStrengthScale(0.5F);
        float scale = 0.2F + strength * strength * 0.8F;
        boolean charged = strength > 0.9F;
        boolean critical = charged && player.fallDistance > 0.0F && !player.onGround() && !player.onClimbable()
                && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger()
                && !player.isSprinting();
        return base -> {
            float enchanted = EnchantmentHelper.modifyDamage(level, weapon, victim, source, (float) base) - (float) base;
            float damage = (float) base * scale;
            damage += weapon.getItem().getAttackDamageBonus(victim, damage, source);
            if (critical) damage *= 1.5F;
            return damage + enchanted * strength;
        };
    }

    /**
     * The base attack damage for which the damage of the attack is {@code factor} times what it is for {@code base}. The
     * attack is linear in its base for every enchantment of the game, so one step of the secant is exact; a flat or falling
     * line (a data pack that does something odd with the damage) falls back to scaling the base itself. The result is not
     * below zero.
     */
    public static double solveBase(double base, double factor, DoubleUnaryOperator damageOfBase) {
        double current = damageOfBase.applyAsDouble(base);
        double wanted = current * factor;
        double slope = damageOfBase.applyAsDouble(base + 1D) - current;
        double solved = slope > 1.0E-6 ? base + (wanted - current) / slope : base * factor;
        return Math.max(0D, solved);
    }
}
