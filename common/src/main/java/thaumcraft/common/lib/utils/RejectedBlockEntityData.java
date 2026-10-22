package thaumcraft.common.lib.utils;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;

/** Offline-repair archive, never an item source. Survives block breaking/replacement. */
public final class RejectedBlockEntityData extends SavedData {
    public static final String ID="thaumcraft_rejected_block_entities";
    private static final Factory<RejectedBlockEntityData> FACTORY=new Factory<>(RejectedBlockEntityData::new,RejectedBlockEntityData::load,null);
    private CompoundTag original=new CompoundTag();

    public static void retain(ServerLevel level, BlockEntity owner, CompoundTag payload) {
        var archive=level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY,ID);
        CompoundTag record=new CompoundTag();
        record.putString("Dimension",level.dimension().location().toString());
        record.putLong("Position",owner.getBlockPos().asLong());
        record.putString("Owner",owner.getClass().getName());
        record.putString("BlockState",owner.getBlockState().toString());
        record.put("Payload",payload.copy());
        archive.retain(record);
    }
    public static RejectedBlockEntityData load(CompoundTag tag,HolderLookup.Provider registries) {
        var archive=new RejectedBlockEntityData(); archive.original=tag.copy(); return archive;
    }
    public void retain(CompoundTag record) {
        // Keep even malformed archive containers; never normalize previously retained evidence.
        if (original.contains("Owners") && (!(original.get("Owners") instanceof ListTag list)
                || (!list.isEmpty() && list.getElementType()!=Tag.TAG_COMPOUND)))
            throw new IllegalStateException("Unread "+ID+" Owners; repair archive offline before retiring another owner");
        ListTag owners=original.getList("Owners",Tag.TAG_COMPOUND);
        if (owners.contains(record)) return;
        owners.add(record.copy()); original.put("Owners",owners); setDirty();
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) { return original.copy(); }
}
