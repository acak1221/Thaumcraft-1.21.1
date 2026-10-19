package thaumcraft.common.lib.essentia;

import thaumcraft.api.aspects.Aspect;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Internal native storage contract, not an atomicity promise for arbitrary
 * IEssentiaTransport addons. Getters and the raw setter must not call out to
 * other code. The setter changes fields ONLY and requires the transfer's raw
 * mutation scope; notifications happen after BOTH endpoints have been written.
 */
public interface EssentiaStorageEndpoint extends EssentiaAtomicEndpoint {
    Aspect getStoredEssentiaType();
    int getStoredEssentiaAmount();
    int getEssentiaCapacity();
    /** Callback-free native sink permission; capacity remains the real stored bound. */
    default boolean allowsEssentiaOverflow() { return false; }
    void setEssentiaStorageRaw(Aspect aspect, int amount);
    void notifyEssentiaStorageChanged();

    record SingletonState(Aspect type, int amount, int capacity, boolean overflow) implements State {
        boolean valid() { return capacity > 0 && amount >= 0 && amount <= capacity && (amount == 0 || EssentiaAtomicEndpoint.validAspect(type)); }
    }

    @Override default State snapshotAtomicState() {
        return new SingletonState(getStoredEssentiaType(), getStoredEssentiaAmount(), getEssentiaCapacity(), allowsEssentiaOverflow());
    }

    @Override default Change prepareExtract(State before, Aspect aspect, int units) {
        if (!(before instanceof SingletonState stock) || !stock.valid() || units <= 0 || !EssentiaAtomicEndpoint.validAspect(aspect)
                || stock.type() != aspect || stock.amount() < units) return null;
        int left = stock.amount() - units;
        return new Change(this, before, new SingletonState(left == 0 ? null : aspect, left, stock.capacity(), stock.overflow()), units, true);
    }

    @Override default Change prepareInsert(State before, Aspect aspect, int units) {
        if (!(before instanceof SingletonState stock) || !stock.valid() || units <= 0 || !EssentiaAtomicEndpoint.validAspect(aspect)
                || stock.amount() > 0 && stock.type() != aspect || !stock.overflow() && units > stock.capacity() - stock.amount()) return null;
        int after = (int) Math.min(stock.capacity(), (long) stock.amount() + units);
        boolean changed = after != stock.amount();
        return new Change(this, before, changed ? new SingletonState(aspect, after, stock.capacity(), stock.overflow()) : before, units, changed);
    }

    @Override default void applyAtomicRaw(Change change) {
        EssentiaTransfers.requireAtomicChange((BlockEntity) this, change);
        SingletonState after = (SingletonState) change.after();
        setEssentiaStorageRaw(after.type(), after.amount());
    }
}
