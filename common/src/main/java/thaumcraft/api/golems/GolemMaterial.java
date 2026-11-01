package thaumcraft.api.golems;

/**
 * The eight golem materials of TC4, in the order of the original {@code EnumGolemType}: the {@link #id()} of a material is
 * its ordinal there, the value a golem saves as {@code GolemType} and the 1.7.10 damage value of its golem placer.
 *
 * <p>Plain data: no registry or world is read, so it is safe on any side and any thread.</p>
 */
public enum GolemMaterial {
    /** Straw golem, id 0. */
    STRAW("Straw"),
    /** Wood golem, id 1. */
    WOOD("Wood"),
    /** Tallow golem, id 2. */
    TALLOW("Tallow"),
    /** Clay golem, id 3. */
    CLAY("Clay"),
    /** Flesh golem, id 4. */
    FLESH("Flesh"),
    /** Stone golem, id 5. */
    STONE("Stone"),
    /** Iron golem, id 6. */
    IRON("Iron"),
    /** Thaumium golem, id 7. */
    THAUMIUM("Thaumium");

    private static final GolemMaterial[] BY_ID = values();
    private final String displayName;

    GolemMaterial(String displayName) {
        this.displayName = displayName;
    }

    /** The original material id: the ordinal of {@code EnumGolemType}, 0 (straw) to 7 (thaumium). */
    public int id() {
        return ordinal();
    }

    /**
     * The fixed English name of the material, {@code "Straw"} to {@code "Thaumium"}: the strings the 1.7.10 Golem
     * Connector of Thaumic Tinkerer answered for {@code getType}. Not translated; use the golem's own name for a player.
     */
    public String displayName() {
        return displayName;
    }

    /**
     * The material of an original id.
     *
     * @param id a material id, as {@link GolemView#materialId()} holds it
     * @return the material, or {@code null} for an id that is no TC4 material (a golem loaded from data this version
     *         does not know)
     */
    public static GolemMaterial byId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
    }
}
