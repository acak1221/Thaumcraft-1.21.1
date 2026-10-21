package thaumcraft.common.entities.golems;

/** Original TC4 ordinal/property table; enum presence is NOT deployment support. */
public enum EnumGolemType {
    STRAW(10, 0, 0.38F, false, 1, 1, 75, 0),
    WOOD(20, 6, 0.35F, false, 1, 4, 75, 1),
    TALLOW(20, 9, 0.33F, false, 2, 8, 75, 2),
    CLAY(25, 9, 0.33F, true, 1, 8, 100, 2),
    FLESH(15, 6, 0.35F, false, 2, 4, 40, 1),
    STONE(30, 12, 0.32F, true, 1, 16, 100, 3),
    IRON(35, 15, 0.31F, true, 1, 32, 125, 4),
    THAUMIUM(40, 15, 0.32F, true, 2, 32, 100, 4);

    public final int health;
    public final int armor;
    public final float speed;
    public final boolean fireResist;
    public final int upgrades;
    public final int carry;
    public final int regenDelay;
    public final int strength;
    private static final EnumGolemType[] TYPES = values();

    public static EnumGolemType getType(int material) {
        return material >= 0 && material < TYPES.length ? TYPES[material] : null;
    }

    public static boolean isSupported(int material) {
        return switch (material) {
            case 0, 1, 2, 3, 4, 5, 6, 7 -> true;
            default -> false;
        };
    }

    /**
     * Original EntityGolemBase:380-382 - the stone, the iron and the thaumium golem move twice as fast in water.
     * The 1.12 legacy line of this mod calls the same three materials its "heavy water golem".
     */
    public boolean heavyWater() { return this == STONE || this == IRON || this == THAUMIUM; }

    private EnumGolemType(int health, int armor, float speed, boolean fireResist,
                          int upgrades, int carry, int regenDelay, int strength) {
        this.health = health;
        this.armor = armor;
        this.speed = speed;
        this.fireResist = fireResist;
        this.upgrades = upgrades;
        this.carry = carry;
        this.regenDelay = regenDelay;
        this.strength = strength;
    }
}
