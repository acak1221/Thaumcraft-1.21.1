package thaumcraft.common.lib.essentia;

import java.util.Objects;
import net.minecraft.core.Direction;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;

/** Internal native stock, not an atomicity promise for arbitrary transport addons.
 * Snapshot/prepare/match/apply are callback-free and NEVER query a random public
 * transport type. Separate input/output stores can supply their own full State. */
public interface EssentiaAtomicEndpoint extends IEssentiaTransport {
    interface State {}

    /** A prepared, owner-bound field replacement. Only the executor can authorize apply. */
    record Change(EssentiaAtomicEndpoint owner, State before, State after, int units, boolean changed) {
        public Change {
            Objects.requireNonNull(owner); Objects.requireNonNull(before); Objects.requireNonNull(after);
            if (units <= 0) throw new IllegalArgumentException("An atomic change must account for positive units");
        }
    }

    State snapshotAtomicState();
    Change prepareExtract(State before, Aspect aspect, int units);
    Change prepareInsert(State before, Aspect aspect, int units);
    default boolean matchesAtomicState(State before) { return before.equals(snapshotAtomicState()); }
    void applyAtomicRaw(Change change);

    /** Callback-bearing source arbitration, BEFORE preparation/field commits. */
    default boolean preflightTake(Aspect aspect, Direction outputFace) { return true; }
    void notifyEssentiaStorageChanged();

    static boolean validAspect(Aspect aspect) { return aspect != null && Aspect.getAspect(aspect.getTag()) == aspect; }
}
