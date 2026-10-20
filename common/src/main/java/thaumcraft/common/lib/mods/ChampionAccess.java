package thaumcraft.common.lib.mods;

/**
 * The champion modifier state of a mob. The 1.7 original kept the modifier id in the
 * custom tc.mobmod attribute of every EntityMob, which 1.21.1 cannot register; the
 * native port keeps the id in one int field of the mob and mirrors it into the
 * entity's synced data, so the client can reproduce the original champion particles.
 * The synced mirror of that id lives in {@link ChampionData}.
 */
public interface ChampionAccess {
    /** The persistent champion modifier id of the mob, {@code NOT_ROLLED} while it was never rolled. */
    int thaumcraft$getChampionModifier();
    /** Writes the persistent champion modifier id of the mob. */
    void thaumcraft$setChampionModifier(int modifier);
}
