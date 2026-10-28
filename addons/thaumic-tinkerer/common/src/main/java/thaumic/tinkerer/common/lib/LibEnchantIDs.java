/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [29 Sep 2013, 11:09:54 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.lib;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The identities of the fourteen enchantments. 1.7.10 configured numeric ids (220 to 225 and 166 to 173); 1.21.1 has
 * none, an enchantment is identified by its registry key and its level is read from the item's enchantments component
 * through that key. The field names are the 1.7.10 ones.
 */
public final class LibEnchantIDs {

    public static final ResourceKey<Enchantment> idAscentBoost = key(LibEnchantNames.ASCENT_BOOST);
    public static final ResourceKey<Enchantment> idSlowFall = key(LibEnchantNames.SLOW_FALL);
    public static final ResourceKey<Enchantment> idAutoSmelt = key(LibEnchantNames.AUTO_SMELT);
    public static final ResourceKey<Enchantment> idDesintegrate = key(LibEnchantNames.DESINTEGRATE);
    public static final ResourceKey<Enchantment> idQuickDraw = key(LibEnchantNames.QUICK_DRAW);
    public static final ResourceKey<Enchantment> idVampirism = key(LibEnchantNames.VAMPIRISM);

    public static final ResourceKey<Enchantment> dispersedStrikes = key(LibEnchantNames.dispersedStrikes);
    public static final ResourceKey<Enchantment> shatter = key(LibEnchantNames.shatter);
    public static final ResourceKey<Enchantment> finalStrike = key(LibEnchantNames.finalStrike);
    public static final ResourceKey<Enchantment> focusedStrike = key(LibEnchantNames.focusedStrike);
    public static final ResourceKey<Enchantment> pounce = key(LibEnchantNames.pounce);
    public static final ResourceKey<Enchantment> shockwave = key(LibEnchantNames.shockwave);
    public static final ResourceKey<Enchantment> tunnel = key(LibEnchantNames.tunnel);
    public static final ResourceKey<Enchantment> valiance = key(LibEnchantNames.valiance);

    private LibEnchantIDs() {}

    /** The registry key of an enchantment of the add-on, from its registry path. */
    public static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, LibMisc.id(path));
    }
}
