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
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.common.blocks.BlockMirror;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileMirrorEssentia: the essentia mirror. It is an aspect source:
 * a single unit of an aspect is taken from the network in front of its twin
 * and handed to whoever asks the mirror for it.
 *
 * <p>The original keeps no stock of its own. The amount and the direction come
 * from the twin: the request is refused above one unit and the network that
 * pays is the one the twin looks at.</p>
 */
public final class TileMirrorEssentia extends BlockEntity implements IAspectSource {
    public boolean linked = false;
    public int linkX;
    public int linkY;
    public int linkZ;
    public String linkDim = "";
    /** The face of the twin the essentia is taken from. */
    public Direction linkedFacing = null;
    int count = 0;
    int inc = EldritchBlockRules.MIRROR_LINK_RETRY;

    public TileMirrorEssentia(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.MIRROR_ESSENTIA_TILE.get(), pos, state);
    }

    public boolean isLinked() { return linked; }
    public Direction getLinkedFacing() { return linkedFacing; }

    @Override public void setRemoved() {
        EssentiaHandler.forgetMirror(this);
        super.setRemoved();
    }

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
        mirror.linkedFacing = getFacing();
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
                || mirror.linkZ != worldPosition.getZ() || !mirror.linkDim.equals(level.dimension().location().toString())) {
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
                && mirror.linkZ == worldPosition.getZ() && mirror.linkDim.equals(level.dimension().location().toString());
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

    // ------------------------------------------------------- the aspect source

    /**
     * Original TileMirrorEssentia:186-201 - the mirror is an aspect source: a
     * single unit of the aspect is taken from the network in front of the twin,
     * in the direction the twin looks at. A larger request is refused.
     */
    @Override public boolean takeFromContainer(Aspect aspect, int amount) {
        if (!isLinkValid() || !EldritchBlockRules.mirrorEssentiaTakeAllowed(amount)) return false;
        ServerLevel target = targetWorld();
        if (target == null) return false;
        if (linkedFacing == null) linkedFacing = twinFacing(target);
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirrorEssentia twin)) return false;
        return EssentiaHandler.drainEssentiaForMirror(twin, aspect, linkedFacing, EldritchBlockRules.MIRROR_ESSENTIA_DRAIN, true);
    }

    /** Original TileMirrorEssentia:168-171 - the mirror holds no aspects of its own. */
    @Override public AspectList getAspects() { return new AspectList(); }

    /** Original TileMirrorEssentia:173-175 - the aspects of the mirror cannot be set. */
    @Override public void setAspects(AspectList aspects) {}

    /** Original TileMirrorEssentia:177-180 - the mirror never accepts an aspect. */
    @Override public boolean doesContainerAccept(Aspect aspect) { return false; }

    /** Original TileMirrorEssentia:182-185 - the mirror never stores an aspect. */
    @Override public int addToContainer(Aspect aspect, int amount) { return 0; }

    /** Original TileMirrorEssentia:203-206 - a list of aspects is never taken. */
    @Override @Deprecated public boolean takeFromContainer(AspectList aspects) { return false; }

    /** Original TileMirrorEssentia:208-211 - the mirror never holds an amount. */
    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) { return false; }

    /** Original TileMirrorEssentia:213-216 - a list of aspects is never held. */
    @Override @Deprecated public boolean doesContainerContain(AspectList aspects) { return false; }

    /** Original TileMirrorEssentia:218-221 - the mirror never holds an amount. */
    @Override public int containerContains(Aspect aspect) { return 0; }

    /** Original TileMirrorEssentia:194-196 - the direction of the twin, its metadata modulo six. */
    private Direction twinFacing(ServerLevel target) {
        BlockState state = target.getBlockState(new BlockPos(linkX, linkY, linkZ));
        return state.getBlock() instanceof BlockMirror ? state.getValue(BlockMirror.FACING) : Direction.NORTH;
    }

    /** Original TileMirrorEssentia:213-222 - the retry of a broken link. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileMirrorEssentia mirror) {
        if (level.isClientSide) return;
        // Original TileMirrorEssentia:224 - the counter is READ before it is
        // raised, so the first link check happens on the very first tick.
        if (mirror.count % mirror.inc == 0) {
            if (!mirror.isLinkValidSimple()) {
                if (mirror.inc < EldritchBlockRules.MIRROR_LINK_RETRY_MAX) {
                    mirror.inc = EldritchBlockRules.mirrorLinkRetryInterval(mirror.inc);
                }
                mirror.restoreLink();
            } else {
                mirror.inc = EldritchBlockRules.MIRROR_LINK_RETRY;
            }
        }
        mirror.count++;
    }

    /** The face the mirror is attached to. */
    public Direction getFacing() {
        if (level == null) return Direction.NORTH;
        return level.getBlockState(worldPosition).getValue(BlockMirror.FACING);
    }

    /** The world of the linked mirror. */
    private ServerLevel targetWorld() {
        if (level == null || level.getServer() == null) return null;
        MinecraftServer server = level.getServer();
        ResourceLocation location = ResourceLocation.tryParse(linkDim);
        if (location == null) return null;
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location);
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
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("linked", linked);
        tag.putInt("linkX", linkX);
        tag.putInt("linkY", linkY);
        tag.putInt("linkZ", linkZ);
        tag.putString("linkDim", linkDim);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
