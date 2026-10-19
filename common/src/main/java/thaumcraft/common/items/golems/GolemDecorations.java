package thaumcraft.common.items.golems;

/**
 * Pure rules of the golem accessories (original ItemGolemDecoration, EntityGolemBase:206-216, 329-331, 388, 419-423, 868-889,
 * 1047-1052, 1068-1071). A golem's decoration is the string of its accessory letters, in installation order, exactly as the
 * original keeps it in the entity's {@code Decoration} tag; every effect is a lookup of one letter in it.
 *
 * <p>Only letters whose effect exists in this port can be installed or stored, so no accessory ever lacks its effect. The dart
 * launcher ({@link #DART}) acts only on a Guard or Butcher golem (the ranged attack goal of {@code GuardGolemGoals}); on any other core
 * it is installed and stored, as the original allowed, and does nothing.
 */
public final class GolemDecorations {
    /** Top hat: +5 maximum health. */
    public static final char HAT = 'H';
    /** Spectacles: the golem's range grows by 10% (at least 1). */
    public static final char GLASSES = 'G';
    /** Bowtie: 10% faster. */
    public static final char BOWTIE = 'B';
    /** Fez: repairs itself at two thirds of the delay. */
    public static final char FEZ = 'F';
    /** Dart launcher: a ranged attack for combat golems (Guard and Butcher). */
    public static final char DART = 'R';
    /** Visor: +1 armour and the victims drop experience as if a player had struck them. */
    public static final char VISOR = 'V';
    /** Iron plating: +4 armour, 12% slower. */
    public static final char ARMOR = 'P';
    /** Mace arm: +2 melee damage. */
    public static final char MACE = 'M';

    /** The original item damage order 0..7 ({@code ItemGolemDecoration.getDecoChar}). */
    public static final String ALL = "HGBFRVPM";
    /** The letters this port honours, in item damage order. */
    public static final String SUPPORTED = "HGBFRVPM";
    public static final int MAX_ARMOR = 20;

    private GolemDecorations() {}

    public static boolean isSupported(char letter) { return SUPPORTED.indexOf(letter) >= 0; }

    public static boolean has(String decoration, char letter) { return decoration != null && decoration.indexOf(letter) >= 0; }

    /** The letter the original item damage {@code damage} stands for, or {@code 0} for none. */
    public static char letterOfDamage(int damage) { return damage >= 0 && damage < ALL.length() ? ALL.charAt(damage) : 0; }

    /** Pairs that exclude each other (original E:872-881): head wear, eye wear and the speed accessories. */
    private static boolean conflicts(String decoration, char letter) {
        return switch (letter) {
            case FEZ -> has(decoration, HAT);
            case HAT -> has(decoration, FEZ);
            case GLASSES -> has(decoration, VISOR);
            case VISOR -> has(decoration, GLASSES);
            case BOWTIE -> has(decoration, ARMOR);
            case ARMOR -> has(decoration, BOWTIE);
            default -> false;
        };
    }

    /** Original {@code addDecoration}: not a repeat and not the partner of an installed letter. */
    public static boolean canAdd(String decoration, char letter) {
        return isSupported(letter) && !has(decoration, letter) && !conflicts(decoration, letter);
    }

    /** The decoration after installing {@code letter}, or null when it cannot be installed. */
    public static String add(String decoration, char letter) { return canAdd(decoration, letter) ? decoration + letter : null; }

    /** A stored decoration must be a sequence that the installation rules could have produced. */
    public static boolean valid(String decoration) {
        if (decoration == null) return false;
        String built = "";
        for (int index = 0; index < decoration.length(); index++) {
            built = add(built, decoration.charAt(index));
            if (built == null) return false;
        }
        return true;
    }

    /** Original E:206: the top hat adds five maximum health. */
    public static int maxHealthBonus(String decoration) { return has(decoration, HAT) ? 5 : 0; }

    /** Original E:214: the mace adds two melee damage. */
    public static int meleeDamageBonus(String decoration) { return has(decoration, MACE) ? 2 : 0; }

    /** Original E:329-331: a float factor, the bowtie's 1.1 first and then the plating's 0.88. */
    public static float speedFactor(String decoration) {
        float factor = has(decoration, BOWTIE) ? 1.1f : 1.0f;
        if (has(decoration, ARMOR)) factor *= 0.88f;
        return factor;
    }

    /** Original E:388: {@code (int)((float) delay * 0.66f)} with the fez. */
    public static int regenDelay(int materialDelay, String decoration) {
        return has(decoration, FEZ) ? (int) ((float) materialDelay * 0.66f) : materialDelay;
    }

    /** Original E:419-421: spectacles add {@code max(range * 0.1f, 1.0f)}. */
    public static float rangeBonus(float range, String decoration) {
        return has(decoration, GLASSES) ? Math.max(range * 0.1f, 1.0f) : 0f;
    }

    /** Original E:1047-1052: visor +1 and plating +4 on the material armour, never above 20. */
    public static int armor(int materialArmor, String decoration) {
        int armor = materialArmor;
        if (has(decoration, VISOR)) armor++;
        if (has(decoration, ARMOR)) armor += 4;
        return Math.min(armor, MAX_ARMOR);
    }

    /** Original E:1068-1071: a hit by a visor wearer marks the victim as recently struck for this many ticks. */
    public static final int RECENT_HIT_TICKS = 100;

    public static boolean marksRecentHit(String decoration) { return has(decoration, VISOR); }
}
