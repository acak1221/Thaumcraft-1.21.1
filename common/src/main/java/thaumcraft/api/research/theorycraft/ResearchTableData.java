package thaumcraft.api.research.theorycraft;

import java.util.TreeMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The state of one research table session.
 *
 * <p>Forced 1.21.1 changes: {@code EntityPlayer} became {@link Player} and {@code TileEntity} became
 * {@link BlockEntity}.</p>
 */
public class ResearchTableData {
    public BlockEntity table;
    public String player;
    public int inspiration;
    public int inspirationStart;
    public int bonusDraws;
    public final TreeMap<String, Integer> categoryTotals = new TreeMap<>();

    public ResearchTableData(BlockEntity table) {
        this.table = table;
    }

    public ResearchTableData(Player player, BlockEntity table) {
        this(table);
        if (player != null) this.player = player.getGameProfile().getName();
    }

    public boolean hasTotal(String category) {
        return categoryTotals.containsKey(category);
    }

    public int getTotal(String category) {
        Integer total = categoryTotals.get(category);
        return total == null ? 0 : total;
    }

    public void addTotal(String category, int amount) {
        categoryTotals.put(category, getTotal(category) + amount);
    }

    public void addInspiration(int amount) {
        inspiration += amount;
    }
}
