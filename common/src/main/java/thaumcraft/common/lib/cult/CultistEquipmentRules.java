package thaumcraft.common.lib.cult;

/**
 * The original TC4 cultist equipment table, transcribed from the 4.2.3.5 oracle.
 *
 * <p>Sources: {@code thaumcraft/common/entities/monster/EntityCultistKnight.java:75-96},
 * {@code thaumcraft/common/entities/monster/EntityCultistCleric.java:92-102} and
 * {@code thaumcraft/common/entities/monster/boss/EntityCultistLeader.java:132-143}.
 * The table is pure so it can be asserted without a world, a registry or a running game;
 * the entity resolves the pieces it names through the registered items.
 */
public final class CultistEquipmentRules {
    private CultistEquipmentRules() {}

    /** The ten pieces of the original crimson cult armour. */
    public enum Piece {
        ROBE_HOOD(CultistRules.ITEM_ROBE_HOOD),
        ROBE_CHEST(CultistRules.ITEM_ROBE_CHESTPLATE),
        ROBE_LEGS(CultistRules.ITEM_ROBE_LEGGINGS),
        PLATE_HELM(CultistRules.ITEM_PLATE_HELM),
        PLATE_CHEST(CultistRules.ITEM_PLATE_CHESTPLATE),
        PLATE_LEGS(CultistRules.ITEM_PLATE_LEGGINGS),
        LEADER_HELM(CultistRules.ITEM_LEADER_HELM),
        LEADER_CHEST(CultistRules.ITEM_LEADER_CHESTPLATE),
        LEADER_LEGS(CultistRules.ITEM_LEADER_LEGGINGS),
        BOOTS(CultistRules.ITEM_BOOTS);

        private final String id;

        Piece(String id) { this.id = id; }

        /** The native registry id of the piece, the key of the runic bauble piece table. */
        public String id() { return id; }
    }

    /** The four armour slots of a cultist. */
    public enum Slot { HELM, CHEST, LEGS, FEET }

    /** The original blades a cultist carries. */
    public enum Weapon { NONE, IRON_SWORD, VOID_SWORD, THAUMIUM_SWORD, CRIMSON_SWORD }

    /**
     * The equipment of one cultist. The helm of the knight and the boots of the
     * cleric are empty when the original left the slot bare.
     */
    public record Loadout(Piece helm, Piece chest, Piece legs, Piece feet, Weapon weapon) {
        public Loadout {
            if (chest == null || legs == null || weapon == null)
                throw new IllegalArgumentException("a cultist loadout needs its chest, its legs and its blade");
        }

        /** The piece of one armour slot, empty when the original left the slot bare. */
        public Piece piece(Slot slot) {
            return switch (slot) {
                case HELM -> helm;
                case CHEST -> chest;
                case LEGS -> legs;
                case FEET -> feet;
            };
        }
    }

    /**
     * The original random source of the equipment rolls. The cultists draw their
     * rolls in the original order, so the conditional draws live in the table.
     */
    public interface Rolls {
        float nextFloat();
        int nextInt(int bound);
        boolean nextBoolean();
    }

    /**
     * Original EntityCultistKnight.addRandomArmor: the crimson plate, the cultist boots
     * and the iron blade. The rare branch draws the void blade with the robe hood or the
     * thaumium blade, and the thaumium branch drops the helm on a coin flip.
     *
     * @param hard  the original hard difficulty flag of the world
     * @param rolls the original random source of the knight
     */
    public static Loadout knight(boolean hard, Rolls rolls) {
        if (rolls.nextFloat() < CultistRules.knightRareChance(hard)) {
            if (CultistRules.knightRareWeapon(rolls.nextInt(5)) == CultistRules.KNIGHT_WEAPON_VOID) {
                return new Loadout(Piece.ROBE_HOOD, Piece.PLATE_CHEST, Piece.PLATE_LEGS, Piece.BOOTS, Weapon.VOID_SWORD);
            }
            Piece helm = CultistRules.knightKeepsHelm(rolls.nextBoolean()) ? Piece.PLATE_HELM : null;
            return new Loadout(helm, Piece.PLATE_CHEST, Piece.PLATE_LEGS, Piece.BOOTS, Weapon.THAUMIUM_SWORD);
        }
        return new Loadout(Piece.PLATE_HELM, Piece.PLATE_CHEST, Piece.PLATE_LEGS, Piece.BOOTS, Weapon.IRON_SWORD);
    }

    /**
     * Original EntityCultistCleric.addRandomArmor: the crimson robe and, on the original
     * chance, the cultist boots. The cleric carries no blade.
     *
     * @param hard  the original hard difficulty flag of the world
     * @param rolls the original random source of the cleric
     */
    public static Loadout cleric(boolean hard, Rolls rolls) {
        Piece feet = rolls.nextFloat() < CultistRules.clericBootsChance(hard) ? Piece.BOOTS : null;
        return new Loadout(Piece.ROBE_HOOD, Piece.ROBE_CHEST, Piece.ROBE_LEGS, feet, Weapon.NONE);
    }

    /**
     * Original EntityCultistLeader.addRandomArmor: the crimson praetor armour, the cultist
     * boots and the blade of the difficulty. The praetor draws no roll.
     *
     * @param easy the original easy difficulty flag of the world
     */
    public static Loadout praetor(boolean easy) {
        return new Loadout(Piece.LEADER_HELM, Piece.LEADER_CHEST, Piece.LEADER_LEGS, Piece.BOOTS,
                easy ? Weapon.VOID_SWORD : Weapon.CRIMSON_SWORD);
    }
}
