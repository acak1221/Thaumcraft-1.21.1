package thaumcraft.common.tiles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Original TileOwned: the owner and the access list of the arcane door, the
 * arcane pressure plate and the warded glass.
 */
public class TileOwned extends BlockEntity {
    public String owner = "";
    public ArrayList<String> accessList = new ArrayList<>();
    public boolean safeToRemove = false;

    public TileOwned(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.OWNED_TILE.get(), pos, state);
    }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner == null ? "" : owner; }
    public List<String> getAccessList() { return accessList; }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // TileOwned:26-35 - the owner and the access list survive a reload.
        owner = tag.contains("owner") ? tag.getString("owner") : "";
        accessList = new ArrayList<>();
        if (tag.contains("access")) {
            for (int i = 0; i < tag.getList("access", 10).size(); i++) {
                accessList.add(tag.getList("access", 10).getCompound(i).getString("name"));
            }
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // TileOwned:37-48 - the access list is stored as a list of named compounds.
        tag.putString("owner", owner);
        tag.put("access", listTag());
    }

    private net.minecraft.nbt.ListTag listTag() {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (String name : accessList) {
            CompoundTag entry = new CompoundTag();
            entry.putString("name", name);
            list.add(entry);
        }
        return list;
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
