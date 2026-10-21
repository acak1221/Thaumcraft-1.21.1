package thaumcraft.neoforge;

import java.util.List;
import net.minecraft.world.level.storage.loot.LootPool;

/**
 * The pools of a loot table.
 *
 * <p>The 1.21.1 table keeps its pools in a final list and its constructor is package private, so a
 * loader cannot build a new table with a new pool. This interface hands the loader the pools of
 * the table and replaces them, which is the exact equivalent of the mutable builder of the Fabric
 * loot API: the table keeps its parameter set, its random sequence and its own functions, and
 * only the pool that carries the original rows changes.</p>
 */
public interface NeoForgeLootTableAccess {
    /** The pools of the table, in their order. */
    List<LootPool> thaumcraft$pools();

    /** Replaces the pools of the table. */
    void thaumcraft$setPools(List<LootPool> pools);
}
