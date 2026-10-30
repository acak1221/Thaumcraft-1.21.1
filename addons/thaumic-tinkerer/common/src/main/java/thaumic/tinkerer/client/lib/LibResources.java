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
package thaumic.tinkerer.client.lib;

/**
 * Resource paths of the add-on as {@code namespace:path} strings ({@link net.minecraft.resources.ResourceLocation#parse}).
 * The file names are the 1.7.10 ones in snake_case (the rule of scripts/porting/tt_assets.py), the
 * {@code model} directory is {@code models} and {@code enchants} is {@code enchant}.
 */
public final class LibResources {

    public static final String PREFIX_MOD = "thaumictinkerer:";

    public static final String PREFIX_LANG = "/assets/thaumictinkerer/lang/";

    public static final String PREFIX_GUI = PREFIX_MOD + "textures/gui/";
    public static final String PREFIX_MISC = PREFIX_MOD + "textures/misc/";
    public static final String PREFIX_MODEL = PREFIX_MOD + "textures/models/";
    public static final String PREFIX_ENCHANTS = PREFIX_MOD + "textures/enchant/";

    public static final String EMTPY_TEXTURE = "empty_texture";

    public static final String GUI_ANIMATION_TABLET = PREFIX_GUI + "animation_tablet.png";
    public static final String GUI_MOB_MAGNET = PREFIX_GUI + "mob_magnet.png";
    public static final String GUI_ENCHANTER = PREFIX_GUI + "enchanter.png";
    public static final String GUI_ASPECT_ANALYZER = PREFIX_GUI + "aspect_analyzer.png";
    public static final String GUI_SOUL_HEARTS = PREFIX_GUI + "soul_hearts.png";
    public static final String GUI_ICHOR_POUCH = PREFIX_GUI + "ichor_pouch.png";
    public static final String GUI_WARP_GATE = PREFIX_GUI + "warp_gate.png";
    public static final String GUI_REMOTE_PLACER = PREFIX_GUI + "remote_placer.png";
    public static final String NEI_RECIPE_ARROW = PREFIX_GUI + "nei_recipe_arrow.png";

    public static final String MISC_R_ENCHANTING = PREFIX_MISC + "r_enchanting.png";
    public static final String MISC_AT_CENTER = PREFIX_MISC + "at_center.png";
    public static final String MISC_AT_LEFT = PREFIX_MISC + "at_left.png";
    public static final String MISC_AT_RIGHT = PREFIX_MISC + "at_right.png";
    public static final String MISC_AT_INDENT = PREFIX_MISC + "at_facing_indent.png";
    public static final String MISC_REPAIR = PREFIX_MISC + "repair.png";
    public static final String MISC_REPAIR_OFF = PREFIX_MISC + "repair_off.png";

    public static final String MODEL_RELAY_TEXTURE = PREFIX_MODEL + "relay.png";
    public static final String MODEL_MAGNET_N = PREFIX_MODEL + "magnet_n.png";
    public static final String MODEL_MAGNET_S = PREFIX_MODEL + "magnet_s.png";
    public static final String MODEL_MOB_MAGNET_N = PREFIX_MODEL + "mob_magnet_n.png";
    public static final String MODEL_MOB_MAGNET_S = PREFIX_MODEL + "mob_magnet_s.png";
    public static final String MODEL_REVEALING_HELM = PREFIX_MODEL + "armor/revealing_layer_1.png";
    public static final String MODEL_REPAIRER = PREFIX_MODEL + "repairer.png";
    public static final String MODEL_CAP_ICHOR = PREFIX_MODEL + "cap_ichor.png";
    public static final String MODEL_ROD_ICHORCLOTH = PREFIX_MODEL + "rod_ichorcloth.png";
    public static final String MODEL_ARMOR_ICHOR_1 = PREFIX_MODEL + "ichor1.png";
    public static final String MODEL_ARMOR_ICHOR_2 = PREFIX_MODEL + "ichor2.png";
    public static final String MODEL_ARMOR_ICHOR_GEM_1 = PREFIX_MODEL + "ichor_gem1.png";
    public static final String MODEL_ARMOR_ICHOR_GEM_2 = PREFIX_MODEL + "ichor_gem2.png";

    public static final String ENCHANT_PROTECTION = PREFIX_ENCHANTS + "protection.png";
    public static final String ENCHANT_FIRE_PROTECTION = PREFIX_ENCHANTS + "fire_protection.png";
    public static final String ENCHANT_FEATHER_FALLING = PREFIX_ENCHANTS + "feather_falling.png";
    public static final String ENCHANT_BLAST_PROTECTION = PREFIX_ENCHANTS + "blast_protection.png";
    public static final String ENCHANT_PROJECTILE_PROTECTION = PREFIX_ENCHANTS + "projectile_protection.png";
    public static final String ENCHANT_RESPIRATION = PREFIX_ENCHANTS + "respiration.png";
    public static final String ENCHANT_AQUA_AFFINITY = PREFIX_ENCHANTS + "aqua_affinity.png";
    public static final String ENCHANT_THORNS = PREFIX_ENCHANTS + "thorns.png";
    public static final String ENCHANT_SHARPNESS = PREFIX_ENCHANTS + "sharpness.png";
    public static final String ENCHANT_SMITE = PREFIX_ENCHANTS + "smite.png";
    public static final String ENCHANT_BANE_OF_ARTHROPODS = PREFIX_ENCHANTS + "bane_of_awful_turds.png";
    public static final String ENCHANT_KNOCKBACK = PREFIX_ENCHANTS + "knockback.png";
    public static final String ENCHANT_FIRE_ASPECT = PREFIX_ENCHANTS + "fire_aspect.png";
    public static final String ENCHANT_LOOTING = PREFIX_ENCHANTS + "looting.png";
    public static final String ENCHANT_EFFICIENCY = PREFIX_ENCHANTS + "efficiency.png";
    public static final String ENCHANT_SILK_TOUCH = PREFIX_ENCHANTS + "silk_touch.png";
    public static final String ENCHANT_UNBREAKING = PREFIX_ENCHANTS + "unbreaking.png";
    public static final String ENCHANT_FORTUNE = PREFIX_ENCHANTS + "fortune.png";
    public static final String ENCHANT_POWER = PREFIX_ENCHANTS + "power.png";
    public static final String ENCHANT_PUNCH = PREFIX_ENCHANTS + "punch.png";
    public static final String ENCHANT_FLAME = PREFIX_ENCHANTS + "flame.png";
    public static final String ENCHANT_INFINITY = PREFIX_ENCHANTS + "infinity.png";
    public static final String ENCHANT_LUCK_OF_THE_SEA = PREFIX_ENCHANTS + "luck_of_the_sea.png";
    public static final String ENCHANT_LURE = PREFIX_ENCHANTS + "lure.png";
    public static final String ENCHANT_POTENCY = PREFIX_ENCHANTS + "potency.png";
    public static final String ENCHANT_FRUGAL = PREFIX_ENCHANTS + "frugal.png";
    public static final String ENCHANT_TREASURE = PREFIX_ENCHANTS + "treasure.png";
    public static final String ENCHANT_HASTE = PREFIX_ENCHANTS + "haste.png";
    public static final String ENCHANT_REPAIR = PREFIX_ENCHANTS + "repair.png";

    public static final String ENCHANT_SLOW_FALL = PREFIX_ENCHANTS + "slow_fall.png";
    public static final String ENCHANT_ASCENT_BOOST = PREFIX_ENCHANTS + "ascent_boost.png";
    public static final String ENCHANT_AUTO_SMELT = PREFIX_ENCHANTS + "auto_smelt.png";
    public static final String ENCHANT_DESINTEGRATE = PREFIX_ENCHANTS + "desintegrate.png";
    public static final String ENCHANT_VAMPIRISM = PREFIX_ENCHANTS + "vamprisim.png";
    public static final String ENCHANT_QUICK_DRAW = PREFIX_ENCHANTS + "quick_draw.png";

    public static final String ENCHANT_FOCUSED_STRIKE = PREFIX_ENCHANTS + "focused_strikes.png";
    public static final String ENCHANT_DISPERSED_STRIKE = PREFIX_ENCHANTS + "dispersed_strikes.png";
    public static final String ENCHANT_VALIANCE = PREFIX_ENCHANTS + "valiance.png";
    public static final String ENCHANT_FINAL_STRIKE = PREFIX_ENCHANTS + "final_strike.png";
    public static final String ENCHANT_TUNNEL = PREFIX_ENCHANTS + "tunnel.png";

    public static final String ENCHANT_SHATTER = PREFIX_ENCHANTS + "shatter.png";
    public static final String ENCHANT_SHOCKWAVE = PREFIX_ENCHANTS + "shockwave.png";
    public static final String ENCHANT_POUNCE = PREFIX_ENCHANTS + "pounce.png";
}
