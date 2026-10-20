package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileArcanePressurePlate: the arcane pressure plate. It is owned by
 * a player and it decides who may trigger it.
 */
public final class TileArcanePressurePlate extends TileOwned {
    /** Original TileArcanePressurePlate:16 - the sensitivity of the plate. */
    public byte setting = 0;

    public TileArcanePressurePlate(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.PRESSURE_PLATE_TILE.get(), pos, state);
    }

    public int getSetting() { return setting; }

    public void setSetting(byte setting) {
        this.setting = setting;
        setChanged();
    }

    /** The sensitivity names of the original plate. */
    public String settingName() {
        return switch (setting) {
            case EldritchBlockRules.PLATE_SETTING_NOT_OWNER -> "chat.thaumcraft.plate.not_owner";
            case EldritchBlockRules.PLATE_SETTING_OWNER_ONLY -> "chat.thaumcraft.plate.owner";
            default -> "chat.thaumcraft.plate.all";
        };
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        setting = tag.getByte("setting");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("setting", setting);
    }
}
