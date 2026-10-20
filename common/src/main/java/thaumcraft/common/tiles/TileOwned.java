package thaumcraft.common.tiles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.tiles.LegacyTileRules;

/**
 * Original TileOwned: the owner of a block and the list of names that may use it.
 * <p>
 * The owner is the player who placed the block; the access list holds the extra names that were granted
 * access. The original keeps both in the tile entity and never ticks it.
 * <p>
 * One tile serves the arcane door, the arcane pressure plate and the warded glass. The pressure plate
 * needs its own registered block entity type, so the subclass names the type it belongs to and the
 * shared constructor resolves the owned type of the other two blocks.
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

    /**
     * The constructor of the original tile: the owned block entity of the arcane
     * door, the arcane pressure plate and the warded glass.
     */
    public TileOwned(BlockPos pos, BlockState state) {
        this(ThaumcraftEldritchBlocks.OWNED_TILE.get(), pos, state);
    }

    public TileOwned(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    /** The owner name, as the original door and device blocks set it. */
    public void setOwner(String owner) {
        this.owner = owner == null ? "" : owner;
        setChanged();
    }

    /** TileOwned:30 - the owner of the block. */
    public String owner() { return owner; }

    /** TileOwned:30 - the owner of the block. */
    public String getOwner() { return owner; }

    /** TileOwned:32-37 - the access list of the block. */
    public List<String> accessList() { return List.copyOf(accessList); }

    /** TileOwned:32-37 - the access list of the block. */
    public List<String> getAccessList() { return accessList; }

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
        // TileOwned:37-48 - the access list is stored as a list of named compounds.
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
        // TileOwned:26-35 - the owner and the access list survive a reload.
        owner = tag.contains("owner") ? tag.getString("owner") : "";
        accessList.clear();
        ListTag access = tag.getList("access", Tag.TAG_COMPOUND);
        for (int index = 0; index < access.size(); index++) accessList.add(access.getCompound(index).getString("name"));
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
