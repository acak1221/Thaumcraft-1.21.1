/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [29 Sep 2013, 11:06:22 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.lib;

/**
 * The names of the fourteen enchantments. 1.7.10 gave an enchantment a name that was a language key
 * ({@code ttinkerer:ascentBoost}); 1.21.1 enchantments are datapack entries, so the name is the registry path of
 * {@code data/thaumictinkerer/enchantment/<path>.json} and the language key is
 * {@code enchantment.thaumictinkerer.<path>} (the converted language files already carry the 1.7.10 texts under these keys).
 */
public final class LibEnchantNames {

    public static final String ASCENT_BOOST = "ascent_boost";
    public static final String SLOW_FALL = "slow_fall";
    public static final String AUTO_SMELT = "auto_smelt";
    public static final String DESINTEGRATE = "desintegrate";
    public static final String QUICK_DRAW = "quick_draw";
    public static final String VAMPIRISM = "vampirism";

    public static final String dispersedStrikes = "dispersed_strike";
    public static final String finalStrike = "final_strike";
    public static final String focusedStrike = "focused_strike";
    public static final String pounce = "pounce";
    public static final String shatter = "shatter";
    public static final String shockwave = "shockwave";
    public static final String tunnel = "tunnel";
    public static final String valiance = "valiance";

    /** Every name, in the order of {@code ModEnchantments.initEnchantments}. */
    public static final String[] ALL = { ASCENT_BOOST, SLOW_FALL, AUTO_SMELT, DESINTEGRATE, QUICK_DRAW, VAMPIRISM,
            dispersedStrikes, finalStrike, focusedStrike, pounce, shatter, shockwave, tunnel, valiance };

    private LibEnchantNames() {}
}
