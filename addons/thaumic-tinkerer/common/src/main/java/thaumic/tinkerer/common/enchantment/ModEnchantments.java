/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.enchantment;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;
import thaumic.tinkerer.common.lib.LibEnchantIDs;

/**
 * The fourteen enchantments of Thaumic Tinkerer.
 *
 * <p>1.7.10 created fourteen {@code Enchantment} objects here and kept them in static fields. In 1.21.1 an enchantment
 * is a record of a data pack ({@code data/thaumictinkerer/enchantment/<path>.json}, with the item tags of
 * {@code data/thaumictinkerer/tags/item/enchantable}); it has no Java class. What is left of this class is the identity
 * of every enchantment (the {@link ResourceKey}s, which replace the numeric ids of {@code LibEnchantIDs}), the way to read
 * a level off a stack or off the armor of an entity, and {@link #initEnchantments()}, which registers the event handler as
 * the original did.</p>
 */
public final class ModEnchantments {

    public static final ResourceKey<Enchantment> ascentBoost = LibEnchantIDs.idAscentBoost;
    public static final ResourceKey<Enchantment> slowFall = LibEnchantIDs.idSlowFall;
    public static final ResourceKey<Enchantment> autoSmelt = LibEnchantIDs.idAutoSmelt;
    public static final ResourceKey<Enchantment> desintegrate = LibEnchantIDs.idDesintegrate;
    public static final ResourceKey<Enchantment> quickDraw = LibEnchantIDs.idQuickDraw;
    public static final ResourceKey<Enchantment> vampirism = LibEnchantIDs.idVampirism;
    public static final ResourceKey<Enchantment> dispersedStrikes = LibEnchantIDs.dispersedStrikes;
    public static final ResourceKey<Enchantment> finalStrike = LibEnchantIDs.finalStrike;
    public static final ResourceKey<Enchantment> focusedStrike = LibEnchantIDs.focusedStrike;
    public static final ResourceKey<Enchantment> pounce = LibEnchantIDs.pounce;
    public static final ResourceKey<Enchantment> shatter = LibEnchantIDs.shatter;
    public static final ResourceKey<Enchantment> shockwave = LibEnchantIDs.shockwave;
    public static final ResourceKey<Enchantment> tunnel = LibEnchantIDs.tunnel;
    public static final ResourceKey<Enchantment> valiance = LibEnchantIDs.valiance;

    /** The four slots of {@code player.inventory.armorInventory}. */
    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST,
            EquipmentSlot.HEAD };

    private static boolean handlerRegistered;

    private ModEnchantments() {}

    /** Registers the event handler of the enchantments (once). Called from the registration of the group. */
    public static synchronized void initEnchantments() {
        if (handlerRegistered) return;
        handlerRegistered = true;
        ModEnchantmentHandler.register();
    }

    /**
     * {@code EnchantmentHelper.getEnchantmentLevel(id, stack)}: the level of the enchantment on the stack, 0 when it has none.
     * Reads the stack's own component, so it needs no registry (it works on a client and in a unit test).
     */
    public static int level(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if (stack.isEmpty()) return 0;
        ItemEnchantments enchantments = stack.getEnchantments();
        if (enchantments.isEmpty()) return 0;
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (holder.is(enchantment)) return enchantments.getLevel(holder);
        }
        return 0;
    }

    /**
     * {@code EnchantmentHelper.getMaxEnchantmentLevel(id, player.inventory.armorInventory)}: the highest level of the
     * enchantment on any of the four armor pieces (whatever the piece, as in 1.7.10).
     */
    public static int maxArmorLevel(LivingEntity entity, ResourceKey<Enchantment> enchantment) {
        int max = 0;
        for (EquipmentSlot slot : ARMOR) {
            max = Math.max(max, level(entity.getItemBySlot(slot), enchantment));
        }
        return max;
    }

    /** The holder of an enchantment in the registry of the level, if the data pack defines it. */
    public static Optional<Holder.Reference<Enchantment>> holder(@Nullable RegistryAccess access,
            ResourceKey<Enchantment> enchantment) {
        if (access == null) return Optional.empty();
        Optional<HolderLookup.RegistryLookup<Enchantment>> lookup = access.lookup(Registries.ENCHANTMENT);
        return lookup.flatMap(registry -> registry.get(enchantment));
    }
}
