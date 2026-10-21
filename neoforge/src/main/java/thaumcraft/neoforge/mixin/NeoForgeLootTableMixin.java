package thaumcraft.neoforge.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import thaumcraft.neoforge.NeoForgeLootTableAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * The NeoForge way to replace the pools of a loot table.
 *
 * <p>The pools of a table are a final list, and the constructor of the table is package private,
 * so a loader cannot build a new table with a new pool. This mixin hands the loader the pools of
 * the table and replaces them, which is the exact equivalent of the mutable builder of the Fabric
 * loot API: the table keeps its parameter set, its random sequence and its own functions, and
 * only the pool that carries the original rows changes.</p>
 */
@Mixin(LootTable.class)
public abstract class NeoForgeLootTableMixin implements NeoForgeLootTableAccess {
    /** The pools of the table. The field is final in the target and mutable here. */
    @Mutable @Shadow @Final private List<LootPool> pools;

    @Override
    @Unique
    public List<LootPool> thaumcraft$pools() {
        return this.pools;
    }

    @Override
    @Unique
    public void thaumcraft$setPools(List<LootPool> pools) {
        this.pools = new ArrayList<>(pools);
    }
}
