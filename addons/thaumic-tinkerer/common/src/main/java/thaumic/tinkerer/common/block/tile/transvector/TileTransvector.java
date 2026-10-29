/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Nov 24, 2013, 6:40:49 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile.transvector;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.common.block.camo.CamoDeviceBlockEntity;

/**
 * A block entity that is bound to another block by the Transvector Binder (the Transvector Interface and the Dislocator).
 *
 * <p>1.7.10 marked "not bound" with {@code y == -1}, which is a real height in 1.21.1 (the world reaches down to -64), so
 * the binding is an optional position and a flag in the saved data. The binding is dropped when the target is further
 * away than {@link #getMaxDistance()} (unless the "cheaty" flag of the saved data is set), or when the target block
 * entity is gone and the device needs one.</p>
 */
public abstract class TileTransvector extends CamoDeviceBlockEntity {

    private static final String TAG_X_TARGET = "xt";
    private static final String TAG_Y_TARGET = "yt";
    private static final String TAG_Z_TARGET = "zt";
    private static final String TAG_LINKED = "linked";
    private static final String TAG_CHEATY_MODE = "cheatyMode";

    @Nullable
    private BlockPos target;
    private boolean cheaty;

    protected TileTransvector(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The bound position, or null when the device is not bound. */
    @Nullable
    public final BlockPos getTarget() {
        return target;
    }

    public final boolean isBound() {
        return target != null;
    }

    /** Binds the device to a position (the Transvector Binder checks the distance before it calls this). */
    public final void setTarget(@Nullable BlockPos pos) {
        this.target = pos == null ? null : pos.immutable();
        setChanged();
    }

    @Override
    protected void writeCustomData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeCustomData(tag, registries);

        tag.putBoolean(TAG_LINKED, target != null);
        if (target != null) {
            tag.putInt(TAG_X_TARGET, target.getX());
            tag.putInt(TAG_Y_TARGET, target.getY());
            tag.putInt(TAG_Z_TARGET, target.getZ());
        }
        tag.putBoolean(TAG_CHEATY_MODE, cheaty);
    }

    @Override
    protected void readCustomData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readCustomData(tag, registries);

        target = tag.getBoolean(TAG_LINKED)
                ? new BlockPos(tag.getInt(TAG_X_TARGET), tag.getInt(TAG_Y_TARGET), tag.getInt(TAG_Z_TARGET))
                : null;
        cheaty = tag.getBoolean(TAG_CHEATY_MODE);
    }

    /**
     * The block entity the device is bound to, or null. A target outside the loaded chunks answers null and keeps the
     * binding; a target that is too far, or missing when it is required, ends the binding (on the server; a client
     * that has not received the target yet keeps it).
     */
    @Nullable
    public final BlockEntity getTile() {
        if (target == null || level == null) return null;
        if (!level.isLoaded(target)) return null;

        BlockEntity tile = level.getBlockEntity(target);

        boolean tooFar = Math.abs(target.getX() - worldPosition.getX()) > getMaxDistance()
                || Math.abs(target.getY() - worldPosition.getY()) > getMaxDistance()
                || Math.abs(target.getZ() - worldPosition.getZ()) > getMaxDistance();
        if (!level.isClientSide && (tile == null && tileRequiredAtLink() || tooFar && !cheaty)) {
            target = null;
            setChanged();
            return null;
        }

        return tile;
    }

    public abstract int getMaxDistance();

    boolean tileRequiredAtLink() {
        return !cheaty;
    }
}
