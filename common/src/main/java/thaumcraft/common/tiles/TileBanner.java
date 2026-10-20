package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileBanner: the arcane banner. It keeps its facing, its colour and
 * the aspect that was applied to it.
 */
public final class TileBanner extends BlockEntity {
    private Direction facing = Direction.NORTH;
    private byte color = (byte) EldritchBlockRules.BANNER_COLOUR_NONE;
    private Aspect aspect = null;
    private boolean onWall = false;

    public TileBanner(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.BANNER_TILE.get(), pos, state);
    }

    public Direction getFacing() { return facing; }

    public void setFacing(Direction face) {
        this.facing = face == null ? Direction.NORTH : face;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public byte getColor() { return color; }

    public void setColor(byte color) {
        this.color = color;
        setChanged();
    }

    public Aspect getAspect() { return aspect; }

    public void setAspect(Aspect aspect) {
        this.aspect = aspect;
        setChanged();
    }

    public boolean isOnWall() { return onWall; }

    public void setWall(boolean onWall) {
        this.onWall = onWall;
        setChanged();
    }

    /** Original TileBanner:100 - a banner is drawn only with a colour or an aspect. */
    public boolean hasContent() {
        return EldritchBlockRules.bannerHasContent(color, aspect != null);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        facing = Direction.from3DDataValue(tag.getByte("facing"));
        color = tag.getByte("color");
        String aspectTag = tag.getString("aspect");
        aspect = aspectTag == null || aspectTag.isEmpty() ? null : Aspect.getAspect(aspectTag);
        onWall = tag.getBoolean("wall");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("facing", (byte) facing.get3DDataValue());
        tag.putByte("color", color);
        tag.putString("aspect", aspect == null ? EldritchBlockRules.bannerAspectTag(null) : aspect.getTag());
        tag.putBoolean("wall", onWall);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
