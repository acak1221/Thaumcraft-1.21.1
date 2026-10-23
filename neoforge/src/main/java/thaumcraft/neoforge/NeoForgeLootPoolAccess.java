package thaumcraft.neoforge;

import java.util.List;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

/**
 * The rows of a weighted loot pool.
 *
 * <p>The 1.21.1 pool holds its rows in a final list and the constructor of the pool is package
 * private, so a loader that wants to add a row has to encode the whole table and decode it again.
 * That round trip cannot work for the vanilla chest tables: the
 * {@code minecraft:enchant_randomly} function of the dungeon tables writes the list of its
 * enchantments, and the enchantment registry is a dynamic registry that the loot table loader
 * does not have, so the encoding of the table fails.</p>
 *
 * <p>This interface therefore hands the loader the exact rows of the pool: the original rows keep
 * their identity, their order and their weight, and the appended rows are the very objects the
 * Fabric mutable builder produces. The weighted list of the pool is therefore the same list on
 * both loaders, and the draw order of the chest is the same as well.</p>
 */
public interface NeoForgeLootPoolAccess {
    /** Appends the given rows to the pool, keeping the rows that are already there. */
    void thaumcraft$appendEntries(List<LootPoolEntryContainer> extra);
}
