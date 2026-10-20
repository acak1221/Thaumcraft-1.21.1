package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileMirrorEssentia: the essentia mirror. It pulls essentia out of
 * the tube network of its twin and hands it to the tube network it faces.
 */
public final class TileMirrorEssentia extends BlockEntity {
    public boolean linked = false;
    public int linkX;
    public int linkY;
    public int linkZ;
    public String linkDim = "";
    /** The face of the twin the essentia is taken from. */
    public Direction linkedFacing = null;
    int count = 0;
    int inc = EldritchBlockRules.MIRROR_LINK_RETRY;
    private Aspect aspect;
    private int amount;

    public TileMirrorEssentia(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.MIRROR_ESSENTIA_TILE.get(), pos, state);
    }

    public boolean isLinked() { return linked; }
    public Aspect getAspect() { return aspect; }
    public int getAmount() { return amount; }
    public Direction getLinkedFacing() { return linkedFacing; }

    /** The link data of the mirror, used by the dropped item. */
    public CompoundTag linkData() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("linkX", linkX);
        tag.putInt("linkY", linkY);
        tag.putInt("linkZ", linkZ);
        tag.putString("linkDim", linkDim);
        return tag;
    }

    /** Original TileMirrorEssentia:38-60 - the link is restored when the twin is free. */
    public void restoreLink() {
        if (!isDestinationValid()) return;
        ServerLevel target = targetWorld();
        if (target == null) return;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirrorEssentia mirror)) return;
        mirror.linked = true;
        mirror.linkX = worldPosition.getX();
        mirror.linkY = worldPosition.getY();
        mirror.linkZ = worldPosition.getZ();
        mirror.linkDim = level.dimension().location().toString();
        mirror.setChanged();
        target.sendBlockUpdated(mirror.getBlockPos(), mirror.getBlockState(), mirror.getBlockState(), 3);
        linkedFacing = mirror.getFacing();
        linked = true;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Original TileMirrorEssentia:62-78 - the link is dropped when the twin is gone. */
    public void invalidateLink() {
        ServerLevel target = targetWorld();
        if (target == null) return;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirrorEssentia mirror)) return;
        mirror.linked = false;
        mirror.linkedFacing = null;
        mirror.setChanged();
        target.sendBlockUpdated(mirror.getBlockPos(), mirror.getBlockState(), mirror.getBlockState(), 3);
    }

    /** Original TileMirrorEssentia:80-108 - the link is only valid when both mirrors agree. */
    public boolean isLinkValid() {
        if (!linked) return false;
        ServerLevel target = targetWorld();
        if (target == null) return false;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirrorEssentia mirror)) {
            linked = false;
            changed();
            return false;
        }
        if (!mirror.linked) {
            linked = false;
            changed();
            return false;
        }
        if (mirror.linkX != worldPosition.getX() || mirror.linkY != worldPosition.getY()
                || mirror.linkZ != worldPosition.getZ() || !mirror.linkDim.equals(linkDim)) {
            linked = false;
            changed();
            return false;
        }
        return true;
    }

    /** Original TileMirrorEssentia:110-124 - the cheap link check. */
    public boolean isLinkValidSimple() {
        if (!linked) return false;
        ServerLevel target = targetWorld();
        if (target == null) return false;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirrorEssentia mirror)) return false;
        if (!mirror.linked) return false;
        return mirror.linkX == worldPosition.getX() && mirror.linkY == worldPosition.getY()
                && mirror.linkZ == worldPosition.getZ() && mirror.linkDim.equals(linkDim);
    }

    /** Original TileMirrorEssentia:126-145 - the destination must not be linked itself. */
    public boolean isDestinationValid() {
        ServerLevel target = targetWorld();
        if (target == null) return false;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirrorEssentia mirror)) {
            linked = false;
            changed();
            return false;
        }
        return !mirror.isLinkValid();
    }

    /**
     * Original TileMirrorEssentia:178-197 - a single unit may be taken and the
     * twin provides at most eight units at a time.
     */
    public int takeEssentia(Aspect requested, int requestedAmount) {
        if (!isLinkValid() || !EldritchBlockRules.mirrorEssentiaTakeAllowed(requestedAmount)) return 0;
        ServerLevel target = targetWorld();
        if (target == null) return 0;
        if (linkedFacing == null) {
            linkedFacing = getFacing();
        }
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirrorEssentia mirror)) return 0;
        if (mirror.aspect != requested || mirror.amount <= 0) return 0;
        int drained = Math.min(mirror.amount, EldritchBlockRules.mirrorEssentiaDrainAmount(mirror.amount));
        mirror.amount -= drained;
        if (mirror.amount <= 0) {
            mirror.amount = 0;
            mirror.aspect = null;
        }
        mirror.setChanged();
        target.sendBlockUpdated(mirror.getBlockPos(), mirror.getBlockState(), mirror.getBlockState(), 3);
        amount += drained;
        if (amount > 64) amount = 64;
        if (aspect == null) aspect = requested;
        setChanged();
        return drained;
    }

    /** Original TileMirrorEssentia:213-222 - the retry of a broken link. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileMirrorEssentia mirror) {
        if (level.isClientSide) return;
        mirror.count++;
        if (mirror.count % mirror.inc != 0) return;
        if (!mirror.isLinkValidSimple()) {
            if (mirror.inc < EldritchBlockRules.MIRROR_LINK_RETRY_MAX) {
                mirror.inc = EldritchBlockRules.mirrorLinkRetryInterval(mirror.inc);
            }
            mirror.restoreLink();
        } else {
            mirror.inc = EldritchBlockRules.MIRROR_LINK_RETRY;
        }
    }

    /** The face the mirror is attached to. */
    public Direction getFacing() {
        if (level == null) return Direction.NORTH;
        return level.getBlockState(worldPosition).getValue(thaumcraft.common.blocks.BlockMirror.FACING);
    }

    /** The world of the linked mirror. */
    private ServerLevel targetWorld() {
        if (level == null || level.getServer() == null) return null;
        MinecraftServer server = level.getServer();
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.tryParse(linkDim));
        if (key == null) return null;
        return server.getLevel(key);
    }

    /** The mirror reports its change to the client. */
    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        linked = tag.getBoolean("linked");
        linkX = tag.getInt("linkX");
        linkY = tag.getInt("linkY");
        linkZ = tag.getInt("linkZ");
        linkDim = tag.getString("linkDim");
        aspect = Aspect.getAspect(tag.getString("Aspect"));
        amount = tag.getInt("Amount");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("linked", linked);
        tag.putInt("linkX", linkX);
        tag.putInt("linkY", linkY);
        tag.putInt("linkZ", linkZ);
        tag.putString("linkDim", linkDim);
        if (aspect != null) tag.putString("Aspect", aspect.getTag());
        tag.putInt("Amount", amount);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
