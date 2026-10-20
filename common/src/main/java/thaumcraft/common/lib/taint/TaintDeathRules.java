package thaumcraft.common.lib.taint;

/**
 * The original TC4 taint death conversion.
 *
 * <p>Oracle: {@code thaumcraft/common/lib/events/EventHandlerEntity.java:480-507}. The original converted every
 * living entity that died while the taint poison was active into its tainted twin: a creeper became a taint
 * creeper, the five vanilla farm animals became their tainted counterparts and every other mob became a thaumic
 * slime whose size came from the max health of the dead mob.</p>
 *
 * <p>The class is pure: no world, no entity and no registry, so the conversion can be unit tested. The world
 * facing half lives in {@code thaumcraft.common.entities.monster.TaintDeathConversion}.</p>
 */
public final class TaintDeathRules {
    private TaintDeathRules() {}

    /** The tainted twin a dying mob becomes. */
    public enum TaintedTwin {
        /** The original tainted chicken. */
        CHICKEN,
        /** The original tainted cow. */
        COW,
        /** The original tainted pig. */
        PIG,
        /** The original tainted sheep. */
        SHEEP,
        /** The original tainted villager. */
        VILLAGER,
        /** The original taint creeper. */
        CREEPER,
        /** The original thaumic slime. */
        SLIME
    }

    /**
     * Original {@code EventHandlerEntity:483}: the conversion happens on the server, for a mob that carries the taint
     * poison and that is not a tainted mob already.
     */
    public static boolean converts(boolean serverSide, boolean fluxTainted, boolean alreadyTainted) {
        return serverSide && fluxTainted && !alreadyTainted;
    }

    /**
     * Original {@code EventHandlerEntity:485-502}: the conversion table of the dying mob.
     *
     * <p>The original compared the CLASS of the dead mob, so every mob that was not one of the six named ones became
     * a thaumic slime. The native port compares the registry id, because 1.21.1 has no metadata and every original
     * mob class has its own registry id.</p>
     */
    public static TaintedTwin twin(String entityId) {
        if (entityId == null) return TaintedTwin.SLIME;
        return switch (entityId) {
            case "minecraft:chicken" -> TaintedTwin.CHICKEN;
            case "minecraft:cow" -> TaintedTwin.COW;
            case "minecraft:pig" -> TaintedTwin.PIG;
            case "minecraft:sheep" -> TaintedTwin.SHEEP;
            case "minecraft:villager" -> TaintedTwin.VILLAGER;
            case "minecraft:creeper" -> TaintedTwin.CREEPER;
            default -> TaintedTwin.SLIME;
        };
    }

    /**
     * Whether the tainted animal layer owns the conversion of this mob.
     *
     * <p>The original had one handler for the whole table. The native port splits it: the tainted animal layer
     * already converts the five farm animals, so this conversion layer only owns the creeper and the slime.</p>
     */
    public static boolean animalTwin(String entityId) {
        TaintedTwin twin = twin(entityId);
        return twin == TaintedTwin.CHICKEN || twin == TaintedTwin.COW || twin == TaintedTwin.PIG
                || twin == TaintedTwin.SHEEP || twin == TaintedTwin.VILLAGER;
    }

    /**
     * Original {@code EventHandlerEntity:500}: the size of the thaumic slime a dying mob becomes.
     *
     * <p>The original wrote {@code (int)(1.0f + Math.min(maxHealth / 10.0f, 6.0f))}, so the size is one plus the
     * max health in tenths, never more than seven.</p>
     */
    public static int slimeSize(float maxHealth) {
        return (int) (1.0F + Math.min(maxHealth / 10.0F, 6.0F));
    }
}
