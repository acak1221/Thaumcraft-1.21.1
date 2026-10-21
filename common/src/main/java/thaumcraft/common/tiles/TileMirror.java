package thaumcraft.common.tiles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileMirror: the item mirror. Two mirrors that see each other pass
 * the items that enter them back and forth.
 */
public final class TileMirror extends BlockEntity {
    public boolean linked = false;
    public int linkX;
    public int linkY;
    public int linkZ;
    public String linkDim = "";
    public int instability = 0;
    int count = 0;
    int inc = EldritchBlockRules.MIRROR_LINK_RETRY;
    private final ArrayList<ItemStack> outputStacks = new ArrayList<>();

    public TileMirror(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.MIRROR_TILE.get(), pos, state);
    }

    public boolean isLinked() { return linked; }
    public Direction getFacing() { return level == null ? Direction.NORTH : level.getBlockState(worldPosition).getValue(thaumcraft.common.blocks.BlockMirror.FACING); }
    public List<ItemStack> getOutputStacks() { return outputStacks; }

    /** The link data of the mirror, used by the dropped item. */
    public CompoundTag linkData() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("linkX", linkX);
        tag.putInt("linkY", linkY);
        tag.putInt("linkZ", linkZ);
        tag.putString("linkDim", linkDim);
        return tag;
    }

    /** Original TileMirror:71-92 - the link is restored when the other mirror is free. */
    public void restoreLink() {
        if (!isDestinationValid()) return;
        ServerLevel target = targetWorld();
        if (target == null) return;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror mirror)) return;
        mirror.linked = true;
        mirror.linkX = worldPosition.getX();
        mirror.linkY = worldPosition.getY();
        mirror.linkZ = worldPosition.getZ();
        mirror.linkDim = level.dimension().location().toString();
        mirror.setChanged();
        target.sendBlockUpdated(mirror.getBlockPos(), mirror.getBlockState(), mirror.getBlockState(), 3);
        linked = true;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Original TileMirror:94-110 - the link is dropped when the other mirror is gone. */
    public void invalidateLink() {
        ServerLevel target = targetWorld();
        if (target == null) return;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror mirror)) return;
        mirror.linked = false;
        mirror.setChanged();
        target.sendBlockUpdated(mirror.getBlockPos(), mirror.getBlockState(), mirror.getBlockState(), 3);
    }

    /** Original TileMirror:112-141 - the link is only valid when both mirrors agree. */
    public boolean isLinkValid() {
        if (!linked) return false;
        ServerLevel target = targetWorld();
        if (target == null) return false;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror mirror)) {
            linked = false;
            changed();
            return false;
        }
        if (!mirror.linked) {
            linked = false;
            changed();
            return false;
        }
        if (!EldritchBlockRules.mirrorLinksMatch(linked, mirror.linked, mirror.linkX, mirror.linkY, mirror.linkZ,
                dimensionId(mirror.linkDim), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), dimensionId(linkDim))) {
            linked = false;
            changed();
            return false;
        }
        return true;
    }

    /** Original TileMirror:143-166 - the cheap link check without the side effects. */
    public boolean isLinkValidSimple() {
        if (!linked) return false;
        ServerLevel target = targetWorld();
        if (target == null) return false;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror mirror)) return false;
        if (!mirror.linked) return false;
        return mirror.linkX == worldPosition.getX() && mirror.linkY == worldPosition.getY()
                && mirror.linkZ == worldPosition.getZ() && mirror.linkDim.equals(linkDim);
    }

    /** Original TileMirror:186-196 - the destination must not be linked itself. */
    public boolean isDestinationValid() {
        ServerLevel target = targetWorld();
        if (target == null) return false;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror mirror)) {
            linked = false;
            changed();
            return false;
        }
        return !mirror.isLinkValid();
    }

    /** Original TileMirror:198-216 - the item is moved to the linked mirror. */
    public boolean transport(ItemEntity item) {
        if (!linked || !isLinkValid()) return false;
        ServerLevel target = targetWorld();
        if (target == null) return false;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror mirror)) return false;
        mirror.addStack(item.getItem().copy());
        addInstability(null, item.getItem().getCount());
        item.discard();
        setChanged();
        mirror.setChanged();
        if (level != null) level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.BEACON_AMBIENT, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    /** Original TileMirror:218-232 - the output of the mirror is ejected. */
    public void eject() {
        if (outputStacks.isEmpty() || count <= EldritchBlockRules.MIRROR_EJECT_DELAY) return;
        int index = level.getRandom().nextInt(outputStacks.size());
        ItemStack stack = outputStacks.get(index);
        if (stack == null || stack.isEmpty()) return;
        ItemStack out = stack.copyWithCount(1);
        if (!spawnItem(out)) return;
        stack.shrink(1);
        addInstability(null, 1);
        if (stack.isEmpty()) outputStacks.remove(index);
        setChanged();
    }

    /** Original TileMirror:234-247 - the item leaves the mirror in front of it. */
    public boolean spawnItem(ItemStack stack) {
        if (level == null) return false;
        Direction face = getFacing();
        ItemEntity item = new ItemEntity(level, worldPosition.getX() + .5D - face.getStepX() * .3D,
                worldPosition.getY() + .5D - face.getStepY() * .3D, worldPosition.getZ() + .5D - face.getStepZ() * .3D, stack);
        item.setDeltaMovement(face.getStepX() * .15F, face.getStepY() * .15F, face.getStepZ() * .15F);
        item.setDefaultPickUpDelay();
        return level.addFreshEntity(item);
    }

    /** Original TileMirror:249-261 - the instability grows on both mirrors. */
    protected void addInstability(Level targetLevel, int amount) {
        instability += amount;
        if (targetLevel != null && targetLevel.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror mirror) {
            mirror.instability += amount;
            if (mirror.instability < 0) mirror.instability = 0;
            mirror.setChanged();
        }
    }

    /** Original TileMirror:287-306 - the mirror ejects, decays and repairs its link. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileMirror mirror) {
        if (level.isClientSide) return;
        // Original TileMirror:295 - the counter is READ before it is raised, so
        // the ejection, the decay and the link repair all run on the tick the
        // counter stands on. The port used the raised counter and shifted all
        // three schedules by one tick.
        if (EldritchBlockRules.mirrorShouldEject(mirror.count, mirror.instability)) mirror.eject();
        mirror.checkInstability(level);
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

    /** Original TileMirror:289-300 - the instability decays and can be paid with vis. */
    public void checkInstability(Level level) {
        if (instability > 0 && count % EldritchBlockRules.MIRROR_INSTABILITY_DECAY_PERIOD == 0) {
            instability--;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        if (instability <= 0) return;
        // Original TileMirror:314 - the instability is paid with ORDER vis of the
        // vis network the mirror stands in, at most one unit per call.
        int paid = VisNetHandler.drainVis(level, worldPosition, Aspect.ORDER,
                EldritchBlockRules.mirrorInstabilityVisPayment(instability));
        if (paid <= 0) return;
        // Original TileMirror:315-317 - both mirrors of the pair lose the paid units.
        addInstability(targetWorld(), -paid);
    }

    /** Original TileMirror:349-356 - the output stacks of the mirror. */
    public void addStack(ItemStack stack) {
        outputStacks.add(stack);
        setChanged();
    }

    /** The world of the linked mirror. */
    private ServerLevel targetWorld() {
        if (level == null || level.getServer() == null) return null;
        MinecraftServer server = level.getServer();
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.tryParse(linkDim));
        if (key == null) return null;
        ServerLevel target = server.getLevel(key);
        if (target == null) return null;
        return target;
    }

    /** The numeric dimension id of a dimension key, used by the link rules. */
    private static int dimensionId(String dimension) {
        ResourceLocation location = ResourceLocation.tryParse(dimension);
        if (location == null) return -1;
        return location.hashCode();
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
        instability = tag.getInt("instability");
        outputStacks.clear();
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = ItemStack.parse(registries, items.getCompound(i)).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) outputStacks.add(stack);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("linked", linked);
        tag.putInt("linkX", linkX);
        tag.putInt("linkY", linkY);
        tag.putInt("linkZ", linkZ);
        tag.putString("linkDim", linkDim);
        tag.putInt("instability", instability);
        ListTag items = new ListTag();
        for (int i = 0; i < outputStacks.size(); i++) {
            ItemStack stack = outputStacks.get(i);
            if (stack == null || stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) i);
            entry.put("stack", stack.save(registries));
            items.add(entry);
        }
        tag.put("Items", items);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
