package thaumcraft.common.lib.essentia;

/** Internal guarded access, NOT an addon Container transaction promise. No live
 * backing list is exposed; the only write needs this executor's exact plan. */
public interface NativeCrystalInventoryAccess {
    CrystalInventoryTarget.Snapshot thaumcraft$crystalSnapshot();
    boolean thaumcraft$matchesCrystalSnapshot(CrystalInventoryTarget.Snapshot snapshot);
    void thaumcraft$applyCrystalInventory(CrystalInventoryTarget.Plan plan);
}
