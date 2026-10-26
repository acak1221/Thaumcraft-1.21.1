package thaumcraft.common.items;

import thaumcraft.api.aspects.Aspect;

/**
 * The aspects the untyped essence items roll from and cycle through.
 *
 * <p>Original ItemCrystalEssence, ItemManaBean and ItemWispEssence copy {@code Aspect.aspects} into a
 * static {@code displayAspects} array in their class initialisers. Those ran in Thaumcraft's own
 * preInit ({@code ConfigItems.init}), before the add-ons, which load after Thaumcraft, registered
 * their aspects; in practice the array therefore held exactly the 48 original aspects, so a fresh
 * untyped crystal or mana bean rolled one of them and the untyped icons cycled through them. The port
 * keeps that rule explicitly instead of copying the registry whenever the item class happens to load,
 * which on Fabric and NeoForge depends on the order of the mod initialisers.</p>
 */
final class OriginalDisplayAspects {
    /** The 48 original aspects in their registration order (original Aspect static initialiser). */
    static final Aspect[] ALL = {
            Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY,
            Aspect.VOID, Aspect.LIGHT, Aspect.WEATHER, Aspect.MOTION, Aspect.COLD, Aspect.CRYSTAL,
            Aspect.LIFE, Aspect.POISON, Aspect.ENERGY, Aspect.EXCHANGE, Aspect.METAL, Aspect.DEATH,
            Aspect.FLIGHT, Aspect.DARKNESS, Aspect.SOUL, Aspect.HEAL, Aspect.TRAVEL, Aspect.ELDRITCH,
            Aspect.MAGIC, Aspect.AURA, Aspect.TAINT, Aspect.SLIME, Aspect.PLANT, Aspect.TREE,
            Aspect.BEAST, Aspect.FLESH, Aspect.UNDEAD, Aspect.MIND, Aspect.SENSES, Aspect.MAN,
            Aspect.CROP, Aspect.MINE, Aspect.TOOL, Aspect.HARVEST, Aspect.WEAPON, Aspect.ARMOR,
            Aspect.HUNGER, Aspect.GREED, Aspect.CRAFT, Aspect.CLOTH, Aspect.MECHANISM, Aspect.TRAP};

    private OriginalDisplayAspects() {}
}
