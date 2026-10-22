package thaumcraft.common.tiles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.tiles.TileParityRules;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileMirror: the item mirror. Two mirrors that see each other pass
 * the items that enter them back and forth.
 *
 * <p>Original TileMirror implements {@code IInventory} with a single slot that always reads
 * empty; an item that a hopper pushes into the slot is handed to the linked mirror, and the
 * mirror without a twin ejects it.
 */
public final class TileMirror extends RetainedDataBlockEntity implements WorldlyContainer {
    /** The single slot of the original mirror. */
    private static final int[] SLOTS = {0};
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
    TileMirror(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type,pos,state); }

    public boolean isLinked() { return linked; }
    public Direction getFacing() { return level == null ? Direction.NORTH : level.getBlockState(worldPosition).getValue(thaumcraft.common.blocks.BlockMirror.FACING); }
    public List<ItemStack> getOutputStacks() { return isDataRejected()?List.of():outputStacks; }

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
        if (isDataRejected()) return;
        if (!isDestinationValid()) return;
        ServerLevel target = targetWorld();
        if (target == null) return;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror mirror)) return;
        if (mirror.isDataRejected()) return;
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
        if (isDataRejected()) return;
        ServerLevel target = targetWorld();
        if (target == null) return;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror mirror)) return;
        mirror.linked = false;
        mirror.setChanged();
        target.sendBlockUpdated(mirror.getBlockPos(), mirror.getBlockState(), mirror.getBlockState(), 3);
    }

    /** Original TileMirror:112-141 - the link is only valid when both mirrors agree. */
    public boolean isLinkValid() {
        if (isDataRejected()) return false;
        if (!linked) return false;
        ServerLevel target = targetWorld();
        if (target == null) return false;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror mirror)) {
            linked = false;
            changed();
            return false;
        }
        if (mirror.isDataRejected()) return false;
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
        if (isDataRejected()) return false;
        if (!linked) return false;
        ServerLevel target = targetWorld();
        if (target == null) return false;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror mirror)) return false;
        if (mirror.isDataRejected() || !mirror.linked) return false;
        return mirror.linkX == worldPosition.getX() && mirror.linkY == worldPosition.getY()
                && mirror.linkZ == worldPosition.getZ() && mirror.linkDim.equals(linkDim);
    }

    /** Original TileMirror:186-196 - the destination must not be linked itself. */
    public boolean isDestinationValid() {
        if (isDataRejected()) return false;
        ServerLevel target = targetWorld();
        if (target == null) return false;
        if (!(target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror mirror)) {
            linked = false;
            changed();
            return false;
        }
        return !mirror.isDataRejected() && !mirror.isLinkValid();
    }

    /** Original TileMirror:198-216 - the item is moved to the linked mirror. */
    public boolean transport(ItemEntity item) {
        if (isDataRejected()) return false;
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
        if (isDataRejected()) return;
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
        if (isDataRejected()) return false;
        if (level == null) return false;
        Direction face = getFacing();
        ItemEntity item = new ItemEntity(level, worldPosition.getX() + .5D - face.getStepX() * .3D,
                worldPosition.getY() + .5D - face.getStepY() * .3D, worldPosition.getZ() + .5D - face.getStepZ() * .3D, stack);
        item.setDeltaMovement(face.getStepX() * .15F, face.getStepY() * .15F, face.getStepZ() * .15F);
        item.setDefaultPickUpDelay();
        // Original TileMirror:207 - the 1.7.10 field is Entity.timeUntilPortal, so the
        // item may not use a portal for twenty ticks. It never delayed the pick up.
        item.setPortalCooldown(TileParityRules.MIRROR_PORTAL_COOLDOWN);
        return level.addFreshEntity(item);
    }

    /** Original TileMirror:249-261 - the instability grows on both mirrors. */
    protected void addInstability(Level targetLevel, int amount) {
        if (isDataRejected()) return;
        instability += amount;
        if (targetLevel != null && targetLevel.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror mirror) {
            if (mirror.isDataRejected()) return;
            mirror.instability += amount;
            if (mirror.instability < 0) mirror.instability = 0;
            mirror.setChanged();
        }
    }

    /** Original TileMirror:287-306 - the mirror ejects, decays and repairs its link. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileMirror mirror) {
        if (mirror.isDataRejected()) return;
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
        if (isDataRejected()) return;
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
        if (isDataRejected()) throw new IllegalStateException("Unread mirror at "+worldPosition+" cannot accept stock");
        outputStacks.add(stack);
        setChanged();
    }

    /** The world of the linked mirror. */
    private ServerLevel targetWorld() {        if (level == null || level.getServer() == null) return null;
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

    /**
     * Original TileMirror:330-344 - the slot byte and the stack itself share one compound,
     * so the fields of the stack sit at the top of the entry. The native reader parses the
     * entry as an item stack, therefore the stack has to be written there too: a nested
     * "stack" compound was never read back and the buffered items were lost on every save.
     */
    public static CompoundTag saveOutputStack(ItemStack stack, int slot, HolderLookup.Provider registries) {
        Tag saved = stack.save(registries);
        CompoundTag entry = saved instanceof CompoundTag compound ? compound : new CompoundTag();
        entry.putByte("Slot", (byte) slot);
        return entry;
    }

    /** The counterpart of {@link #saveOutputStack}: the entry of the original is the stack. */
    public static ItemStack loadOutputStack(CompoundTag entry, HolderLookup.Provider registries) {
        return requiredStack(registries, entry);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (retainMalformed(tag, () -> { for (Tag value:compoundList(tag,"Items")) requiredStack(registries,value); })) return;
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
            ItemStack stack = loadOutputStack(items.getCompound(i), registries);
            if (!stack.isEmpty()) outputStacks.add(stack);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (saveRejected(tag)) return;
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
            items.add(saveOutputStack(stack, i, registries));
        }
        tag.put("Items", items);
    }

    // ---------------------------------------------------------------- inventory surface
    /**
     * Original TileMirror:346-348 - the mirror has one inventory slot, and its stack limit
     * is sixty four (original TileMirror:387-389).
     */
    @Override public int getContainerSize() { return TileParityRules.MIRROR_INVENTORY_SLOTS; }

    /** The original slot never holds anything: the item is handed to the twin at once. */
    @Override public boolean isEmpty() { return true; }

    /** Original TileMirror:352 - the single slot always reads empty. */
    @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }

    /** Original TileMirror:357 - the single slot never gives anything back. */
    @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }

    /** Original TileMirror:362 - the single slot never gives anything back. */
    @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }

    /**
     * Original TileMirror:371-383 - an item that enters the slot is handed to the linked
     * mirror, which keeps it until it ejects it; a mirror without a twin ejects the item
     * in front of itself. The slot itself stays empty.
     */
    @Override public void setItem(int slot, ItemStack stack) {
        if (isDataRejected()) return;
        if (slot != 0 || stack == null || stack.isEmpty() || level == null || level.isClientSide) return;
        ServerLevel target = targetWorld();
        TileMirror twin = target == null ? null
                : target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror mirror ? mirror : null;
        if (twin!=null && twin.isDataRejected()) return;
        if (twin != null) {
            twin.addStack(stack.copy());
            addInstability(null, stack.getCount());
            setChanged();
        } else {
            spawnItem(stack.copy());
        }
    }

    /** Original TileMirror:365 - the slot holds nothing, so there is nothing to clear. */
    @Override public void clearContent() {}

    /** Original TileMirror:387-389 - a slot of the mirror holds sixty four items. */
    @Override public int getMaxStackSize() { return TileParityRules.MIRROR_STACK_LIMIT; }

    /** Original TileMirror:381 - the item is only accepted when the twin mirror exists. */
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && isItemValidForSlot(slot, stack); }

    /** Original TileMirror:406-411 - the twin mirror has to exist. */
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (isDataRejected()) return false;
        if (slot != 0 || stack == null || stack.isEmpty()) return false;
        ServerLevel target = targetWorld();
        return target != null && target.getBlockEntity(new BlockPos(linkX, linkY, linkZ)) instanceof TileMirror twin && !twin.isDataRejected();
    }

    /** The mirror takes the item from every side, as the original IInventory did. */
    @Override public int[] getSlotsForFace(Direction side) { return SLOTS; }

    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canPlaceItem(slot, stack); }

    /** Original TileMirror:378 - the mirror never gives an item back. */
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }

    /** Original TileMirror:400 - no player may open the mirror. */
    @Override public boolean stillValid(Player player) { return false; }

    /** Original TileMirror:385 - the name of the container of the mirror. */
    public Component mirrorContainerName() { return Component.translatable("container.mirror"); }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
