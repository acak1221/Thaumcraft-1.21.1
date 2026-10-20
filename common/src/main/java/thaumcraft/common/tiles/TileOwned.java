package thaumcraft.common.tiles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.lib.tiles.LegacyTileRules;

/**
 * Original TileOwned: the owner of a block and the list of names that may use it.
 * <p>
 * The owner is the player who placed the block; the access list holds the extra names that were granted
 * access. The original keeps both in the tile entity and never ticks it.
 * <p>
 * Oracle: {@code thaumcraft/common/tiles/TileOwned.java}, lines 17-60.
 */
public class TileOwned extends BlockEntity {
    /** TileOwned:19 - the owner name, empty while the block has no owner. */
    public String owner = "";
    /** TileOwned:20 - the names that may use the block besides its owner. */
    public final List<String> accessList = new ArrayList<>();
    /** TileOwned:21 - set once a wand has claimed the block; the original never saves it. */
    public boolean safeToRemove = false;

    public TileOwned(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    /** TileOwned:30 - the owner of the block. */
    public String owner() { return owner; }

    /** TileOwned:32-37 - the access list of the block. */
    public List<String> accessList() { return List.copyOf(accessList); }

    /** The owner name, under the name the original door and device blocks use. */
    public String getOwner() { return owner; }

    /** The access list, under the name the original door and device blocks use. */
    public List<String> getAccessList() { return List.copyOf(accessList); }

    /** Whether the given name may use the block. */
    public boolean hasAccess(String name) { return LegacyTileRules.ownedAccess(owner, accessList, name); }

    /** Whether the block may be removed safely. */
    public boolean isSafeToRemove() { return safeToRemove; }

    /** The name of the owner or of an allowed user, as the original door collects it. */
    public void addAccess(String name) {
        if (name == null || name.isEmpty() || accessList.contains(name)) return;
        accessList.add(name);
        setChanged();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("owner", owner);
        ListTag access = new ListTag();
        for (String name : accessList) {
            CompoundTag entry = new CompoundTag();
            entry.putString("name", name);
            access.add(entry);
        }
        tag.put("access", access);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.contains("owner") ? tag.getString("owner") : "";
        accessList.clear();
        ListTag access = tag.getList("access", Tag.TAG_COMPOUND);
        for (int index = 0; index < access.size(); index++) accessList.add(access.getCompound(index).getString("name"));
    }
}
