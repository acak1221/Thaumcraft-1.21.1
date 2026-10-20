package thaumcraft.common.items;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** What the void metal tools and armor share (original ItemVoid*): the slow self-repair and the wither of a hit. */
public final class VoidMetal {
    /** Ticks between two repairs, counted on the carrier's age. */
    public static final int REPAIR_PERIOD = 20;
    /** Wither duration of a void sword (3 s) and of the other tools (4 s). */
    public static final int SWORD_WITHER = 60, TOOL_WITHER = 80;

    private VoidMetal() {}

    /**
     * Original onUpdate: a damaged stack carried by a living entity on a multiple of 20 ticks of its age is repaired by one point
     * ({@code damageItem(-1)}, which a creative player's stack ignores). Returns whether it repaired.
     */
    public static boolean repair(ItemStack stack, Entity carrier) {
        if (carrier.level().isClientSide || !stack.isDamaged() || !(carrier instanceof LivingEntity) || carrier.tickCount % REPAIR_PERIOD != 0) return false;
        if (carrier instanceof Player player && player.getAbilities().instabuild) return false;
        stack.setDamageValue(stack.getDamageValue() - 1);
        return true;
    }

    /**
     * What the void armor (and the void robes) do on every inventory tick: the carrier's repair once and, when it is a player wearing the
     * stack in the slot it belongs to, the original {@code onArmorTick} repair once more, so a worn piece mends two points a second.
     */
    public static void armorTick(ItemStack stack, Entity carrier, EquipmentSlot slot) {
        repair(stack, carrier);
        if (carrier instanceof Player player && player.getItemBySlot(slot) == stack) repair(stack, carrier);
    }

    /**
     * Original hit effect. The sword skips the effect when a player hits a player with player-versus-player off; the other tools skip it
     * for any player target (their original hook is the attacking player's left click). Mobs always wither.
     */
    public static void wither(LivingEntity target, LivingEntity attacker, int ticks, boolean sword) {
        if (!(target.level() instanceof ServerLevel server)) return;
        boolean pvp = server.getServer().isPvpAllowed();
        if (target instanceof Player && !pvp && (!sword || attacker instanceof Player)) return;
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, ticks));
    }
}
