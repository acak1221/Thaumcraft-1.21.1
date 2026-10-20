package thaumcraft.common.lib.mods;

import java.util.UUID;

/**
 * Original 1.7 champion modifier: plain data for the thirteen champion modifiers, the
 * kind of trigger the original gave them, the original attribute modifier identity and
 * the original effect. A modifier never touches an entity, {@code ChampionRules.Adapter}
 * applies it.
 */
public class ChampionModifier {
    /** Original champion health bonus modifier uuid. */
    public static final UUID CHAMPION_HEALTH_UUID = UUID.fromString("a62bef38-48cc-42a6-ac5e-ef913841c4fd");
    /** Original champion damage bonus modifier uuid. */
    public static final UUID CHAMPION_DAMAGE_UUID = UUID.fromString("a340d2db-d881-4c25-ac62-f0ad14cd63b0");
    /** Original bold movement speed bonus modifier uuid. */
    public static final UUID BOLD_SPEED_UUID = UUID.fromString("4b1edd33-caa9-47ae-a702-d86c05701037");
    /** Original mighty damage bonus modifier uuid. */
    public static final UUID MIGHTY_DAMAGE_UUID = UUID.fromString("7163897f-07f5-49b3-9ce4-b74beb83d2d3");
    /** Original reset modifier uuid the 1.7 original used for a mob that was not a champion. */
    public static final UUID ATTRIBUTE_MOD_NONE_UUID = UUID.fromString("1e645a3d-9115-4807-a61c-705172839f87");

    /** Original champion health bonus: the champion modifier added thirty max health. */
    public static final double CHAMPION_HEALTH_BONUS = 30.0;
    /** Original champion damage multiplier: the champion modifier multiplied attack damage by two. */
    public static final double CHAMPION_DAMAGE_MULTIPLIER = 2.0;
    /** Original bold movement speed multiplier: thirty percent over the base speed. */
    public static final double BOLD_SPEED_MULTIPLIER = 1.3;
    /** Original mighty damage multiplier: attack damage multiplied by three. */
    public static final double MIGHTY_DAMAGE_MULTIPLIER = 3.0;
    /** Original promotion heal: a new champion was healed for twenty five health. */
    public static final float CHAMPION_HEAL = 25.0f;

    /** Original modifier ids. */
    public static final int BOLD = 0, SPINE = 1, ARMOR = 2, MIGHTY = 3, GRIM = 4, WARDED = 5, WARP = 6,
            UNDYING = 7, FIERY = 8, SICKLY = 9, VENOMOUS = 10, VAMPIRIC = 11, INFESTED = 12;

    /** Original modifier id. */
    public final int id;
    /** Original modifier name, the suffix of the original localisation key. */
    public final String name;
    /** Original modifier display name, used when the localisation is missing. */
    public final String displayName;
    /** Original trigger kind of the modifier. */
    public final int type;
    /** Original effect of the modifier. */
    public final IChampionModifierEffect effect;
    /** Original attribute modifier uuid the 1.7 original gave the modifier. */
    public final UUID attributeModifierId;
    /** Original attribute amount: the 1.7 champion attribute held the modifier id plus two. */
    public final double attributeAmount;

    public ChampionModifier(int id, String name, String displayName, int type, IChampionModifierEffect effect, UUID attributeModifierId) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.type = type;
        this.effect = effect;
        this.attributeModifierId = attributeModifierId;
        this.attributeAmount = id + 2;
    }

    /** The thirteen original champion modifiers, in their original order. */
    public static final ChampionModifier[] mods = new ChampionModifier[]{
            new ChampionModifier(BOLD, "bold", "Bold", IChampionModifierEffect.TRIGGER_ATTRIBUTE, new ChampionModBold(), UUID.fromString("40289aa1-907f-4ac6-ad79-e6681efe2cbc")),
            new ChampionModifier(SPINE, "spine", "Spined", IChampionModifierEffect.TRIGGER_DAMAGED, new ChampionModSpined(), UUID.fromString("365eead5-3f15-42a8-9e68-36100faef945")),
            new ChampionModifier(ARMOR, "armor", "Armored", IChampionModifierEffect.TRIGGER_DAMAGED, new ChampionModArmored(), UUID.fromString("4e23758d-348e-42a8-8de6-08ae0a59033c")),
            new ChampionModifier(MIGHTY, "mighty", "Mighty", IChampionModifierEffect.TRIGGER_ATTRIBUTE, new ChampionModMighty(), UUID.fromString("6d2ffe79-f034-4a06-b288-e1916c21e385")),
            new ChampionModifier(GRIM, "grim", "Grim", IChampionModifierEffect.TRIGGER_ATTACK, new ChampionModGrim(), UUID.fromString("0f23321e-f921-4246-90b8-21ef202de224")),
            new ChampionModifier(WARDED, "warded", "Warded", IChampionModifierEffect.TRIGGER_TICK, new ChampionModWarded(), UUID.fromString("b622c4d8-abc6-4db7-b3ee-5cf71b8e5286")),
            new ChampionModifier(WARP, "warp", "Warped", IChampionModifierEffect.TRIGGER_ATTACK, new ChampionModWarp(), UUID.fromString("107da049-af7a-4409-989a-6de23c8fe036")),
            new ChampionModifier(UNDYING, "undying", "Undying", IChampionModifierEffect.TRIGGER_TICK, new ChampionModUndying(), UUID.fromString("cb9484d3-6255-4893-a4f2-3ecc375692ee")),
            new ChampionModifier(FIERY, "fiery", "Fiery", IChampionModifierEffect.TRIGGER_ATTACK, new ChampionModFire(), UUID.fromString("6b567fdf-9245-48f5-8314-f93fe5db1427")),
            new ChampionModifier(SICKLY, "sickly", "Sickly", IChampionModifierEffect.TRIGGER_ATTACK, new ChampionModSickly(), UUID.fromString("b5718868-9ab0-424c-af1f-8b35e836b46e")),
            new ChampionModifier(VENOMOUS, "venomous", "Venomous", IChampionModifierEffect.TRIGGER_ATTACK, new ChampionModPoison(), UUID.fromString("ab9a132e-c619-4c0a-a103-10cbbcfba1a2")),
            new ChampionModifier(VAMPIRIC, "vampiric", "Vampiric", IChampionModifierEffect.TRIGGER_ATTACK, new ChampionModVampire(), UUID.fromString("3412251e-af81-4c3c-93ba-2e1c33b049ea")),
            new ChampionModifier(INFESTED, "infested", "Infested", IChampionModifierEffect.TRIGGER_DAMAGED, new ChampionModInfested(), UUID.fromString("9c577fbe-ddbc-4ea2-a661-770ea775f43b"))};

    /** The original modifier of an id, null when the id is not an original modifier. */
    public static ChampionModifier byId(int id) { return id >= 0 && id < mods.length ? mods[id] : null; }
    /** The original localisation key of the modifier. */
    public String localizationKey() { return "champion.mod." + name; }
    /** The original trigger kind of the modifier. */
    public int trigger() { return type; }
    /** The max health the original champion bonus added to every champion. */
    public double healthBonus() { return CHAMPION_HEALTH_BONUS; }
    /** The attack damage multiplier the original champion bonus gave every champion. */
    public double damageMultiplier() { return CHAMPION_DAMAGE_MULTIPLIER; }
    /** The movement speed multiplier of the modifier, one for every modifier but bold. */
    public double speedMultiplier() { return id == BOLD ? BOLD_SPEED_MULTIPLIER : 1.0; }
    /** The extra attack damage multiplier of the modifier, three for mighty and one otherwise. */
    public double extraDamageMultiplier() { return id == MIGHTY ? MIGHTY_DAMAGE_MULTIPLIER : 1.0; }
    /** The original modifier acts on every living update. */
    public boolean ticks() { return type == IChampionModifierEffect.TRIGGER_TICK; }
    /** The original modifier acts when the champion hits a living target. */
    public boolean attacks() { return type == IChampionModifierEffect.TRIGGER_ATTACK; }
    /** The original modifier acts when a living attacker hurts the champion. */
    public boolean defends() { return type == IChampionModifierEffect.TRIGGER_DAMAGED; }
}
