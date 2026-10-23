package thaumcraft.neoforge.mixin;

import java.util.List;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import thaumcraft.neoforge.NeoForgeLootTableAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Read-only access to the pools of a loot table for the row-appending adapter.
 *
 * <p>The pools of a table are a final list, and the constructor of the table is package private,
 * so a loader cannot build a new table with a new pool. This mixin exposes the existing pools
 * so the pool adapter can append rows without replacing the table's final pool list.</p>
 */
@Mixin(LootTable.class)
public abstract class NeoForgeLootTableMixin implements NeoForgeLootTableAccess {
    /** The pools of the table, left final just as in the target. */
    @Shadow @Final private List<LootPool> pools;

    @Override
    @Unique
    public List<LootPool> thaumcraft$pools() {
        return this.pools;
    }
}
