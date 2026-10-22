package thaumcraft.common.lib.essentia;

import net.minecraft.core.Direction;
import thaumcraft.api.aspects.Aspect;

/**
 * The pure rules of the source set of the Alchemy golem.
 *
 * <p>Oracle {@code thaumcraft/common/entities/ai/fluid/AIEssentiaGather.java:56-124}: the home cell
 * of the golem may be <b>any</b> essentia transport. The jar and the reservoir are accepted without
 * a side test, every other transport must be able to output to the home facing. The side of the
 * payment is the original's: {@code UP} for the jar, the reservoir's own facing for the reservoir
 * and the home facing for every other transport ({@code AIEssentiaGather:94-99}).</p>
 *
 * <p>These are the numbers of the original and nothing else; the world, the tiles and the payment
 * live in the goals and in {@link EssentiaTransfers}.</p>
 */
public final class EssentiaSourceRules {
    /** The kind of the transport that sits at the home of the golem. */
    public enum Kind { JAR, RESERVOIR, OTHER }

    private EssentiaSourceRules() {}

    /**
     * The side the original's {@code takeEssentia} call uses.
     *
     * <p>Oracle {@code AIEssentiaGather:94-99}: the alembic and the jar pay upwards, the reservoir
     * pays on its own facing and every other transport pays on the home facing of the golem.</p>
     */
    public static Direction paymentFace(Kind kind, Direction homeFacing, Direction reservoirFacing) {
        return switch (kind) {
            case JAR -> Direction.UP;
            case RESERVOIR -> reservoirFacing;
            case OTHER -> homeFacing;
        };
    }

    /** The kind of a transport, as the original distinguishes it. */
    public static Kind kindOf(boolean jar, boolean reservoir) {
        if (jar) return Kind.JAR;
        if (reservoir) return Kind.RESERVOIR;
        return Kind.OTHER;
    }

    /**
     * Whether the transport may serve as the source at all.
     *
     * <p>Oracle {@code AIEssentiaGather:58-64}: a jar or a reservoir is accepted without a side
     * test, every other transport must be able to output to the home facing. The transport must
     * hold essentia on that side, the aspect must fit the cargo of the golem and the cargo must
     * still have room.</p>
     */
    public static boolean eligible(Kind kind, boolean canOutputToFace, Aspect carried, Aspect offered, Aspect unknown,
                                   int available, int carriedAmount, int carryLimit) {
        if (kind != Kind.JAR && kind != Kind.RESERVOIR && !canOutputToFace) return false;
        if (available <= 0) return false;
        if (!aspectMatches(carried, offered, unknown)) return false;
        return carriedAmount < carryLimit;
    }

    /**
     * Whether the aspect of the cargo fits the aspect of the transport.
     *
     * <p>Oracle {@code AIEssentiaGather:60}: an empty cargo takes anything, a loaded cargo takes
     * only the aspect of the transport on the side or the aspect of its unsided query.</p>
     */
    public static boolean aspectMatches(Aspect carried, Aspect offered, Aspect unknown) {
        return carried == null || carried.equals(offered) || carried.equals(unknown);
    }

    /**
     * The aspect the original takes.
     *
     * <p>Oracle {@code AIEssentiaGather:104-107}: the type of the side, and the unsided type when
     * the side names none.</p>
     */
    public static Aspect takenAspect(Aspect offered, Aspect unknown) {
        return offered != null ? offered : unknown;
    }

    /**
     * The amount the original takes.
     *
     * <p>Oracle {@code AIEssentiaGather:108-112}: the stock of the transport, and never more than
     * the room of the cargo of the golem.</p>
     */
    public static int takenAmount(int available, int carriedAmount, int carryLimit) {
        return Math.min(available, carryLimit - carriedAmount);
    }
}
