package thaumcraft.common.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Native inventory drop whose full count survives the modern stack codec's 99 limit. */
public final class EntityInventoryItem extends ItemEntity {
    private boolean saving;

    public EntityInventoryItem(EntityType<? extends ItemEntity> type, Level level) { super(type, level); }

    @Override public ItemStack getItem() {
        ItemStack stack = super.getItem();
        // ItemEntity.addAdditionalSaveData uses getItem().save. Substitute only
        // its serialization view; never mutate the tracked physical stack.
        return saving && stack.getCount() > 99 ? stack.copyWithCount(1) : stack;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        int count = super.getItem().getCount();
        saving = true;
        try {
            // Preserve native health, age, pickup delay, owner and thrower data.
            super.addAdditionalSaveData(tag);
        } finally {
            saving = false;
        }
        tag.putInt("StoredCount", count);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ItemStack stack = super.getItem();
        int count = tag.getInt("StoredCount");
        if (!stack.isEmpty() && count > 0) stack.setCount(count);
    }
}
