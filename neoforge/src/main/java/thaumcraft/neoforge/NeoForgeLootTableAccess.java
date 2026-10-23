package thaumcraft.neoforge;

import java.util.List;
import net.minecraft.world.level.storage.loot.LootPool;

/**
 * Read access to the pools of a loot table.
 *
 * <p>The 1.21.1 table keeps its pools in a final list and its constructor is package private, so a
 * loader cannot build a new table with a new pool. This interface hands the loader the pools of
 * the table so it can append rows to the first pool through {@link NeoForgeLootPoolAccess}:
 * the table keeps its parameter set, its random sequence and its own functions.</p>
 */
public interface NeoForgeLootTableAccess {
    /** The pools of the table, in their order. */
    List<LootPool> thaumcraft$pools();
}
