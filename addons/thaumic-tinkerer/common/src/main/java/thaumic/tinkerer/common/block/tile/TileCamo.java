/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Dec 27, 2013, 6:26:18 PM (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.TileThaumcraft;
import thaumic.tinkerer.common.block.BlockCamo;
import thaumic.tinkerer.common.content.SimpleBlocks;

/**
 * The block entity of a camouflageable block: the block state it imitates.
 *
 * <p>1.7.10 saved the block's registry name and its metadata ({@code camo}, {@code camoMeta}); the state of 1.21.1
 * carries both and is saved as a block-state tag under {@code camo}. The tile is synchronised with the clients through
 * {@code TileThaumcraft} (custom data in the update tag and packet). A subclass passes its own block entity type and
 * keeps calling {@code super} from {@link #readCustomNBT} and {@link #writeCustomNBT}.</p>
 */
public class TileCamo extends TileThaumcraft {

    private static final String TAG_CAMO = "camo";

    @Nullable
    private BlockState camo;

    /** The tile of the Ethereal Platform. */
    public TileCamo(BlockPos pos, BlockState state) {
        this(SimpleBlocks.TILE_CAMO.get(), pos, state);
    }

    public TileCamo(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The state the block pretends to be, or null for its own look. */
    @Nullable
    public BlockState getCamo() {
        return camo;
    }

    /**
     * Sets or clears the camouflage on the server: updates the block's {@link BlockCamo#CAMOUFLAGED} property, saves the
     * chunk and resends the tile to the clients (1.7.10 {@code markBlockForUpdate}).
     */
    public void setCamo(@Nullable BlockState newCamo) {
        if (newCamo != null && newCamo.isAir()) newCamo = null;
        this.camo = newCamo;
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.hasProperty(BlockCamo.CAMOUFLAGED) && state.getValue(BlockCamo.CAMOUFLAGED) != (camo != null)) {
            state = state.setValue(BlockCamo.CAMOUFLAGED, camo != null);
            level.setBlock(worldPosition, state, Block.UPDATE_ALL);
        }
        setChanged();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
    }

    @Override
    public void writeCustomNBT(CompoundTag cmp) {
        super.writeCustomNBT(cmp);
        if (camo != null) cmp.put(TAG_CAMO, NbtUtils.writeBlockState(camo));
    }

    @Override
    public void readCustomNBT(CompoundTag cmp) {
        super.readCustomNBT(cmp);
        camo = null;
        if (cmp.contains(TAG_CAMO, 10)) {
            BlockState read = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), cmp.getCompound(TAG_CAMO));
            camo = read.isAir() ? null : read;
        }
    }
}
