package thaumcraft.common.tiles;

import dev.architectury.hooks.item.ItemStackHooks;
import dev.architectury.registry.fuel.FuelRegistry;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.BlockAlchemyFurnace;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.container.ContainerAlchemyFurnace;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/**
 * TC4 TileAlchemyFurnace: two physical slots and a 50-vis mixed aspect buffer.
 * Original anchors: tick211-289, capacity/time291-304, smelt311-323,
 * sided inventory340-359, uniform (not weighted) extraction362-386.
 */
public final class TileAlchemyFurnace extends BlockEntity implements WorldlyContainer, MenuProvider, Nameable {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int MAX_VIS = 50;
    private static final ResourceLocation BELLOWS = ResourceLocation.fromNamespaceAndPath("thaumcraft", "bellows");
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final AspectList aspects = new AspectList();
    private int furnaceBurnTime;
    private int currentItemBurnTime;
    private int furnaceCookTime;
    private int smeltTime = 100;
    private int bellows = -1;
    private boolean speedBoost;
    private Component customName;
    // Original extraction phase is transient and restarts on loading the BE.
    private int count;
    private boolean processing;
    private boolean transferring;
    private boolean evacuating;

    public TileAlchemyFurnace(BlockPos pos, BlockState state) {
        super(ThaumcraftDistillation.ALCHEMY_FURNACE_BLOCK_ENTITY.get(), pos, state);
    }

    public AspectList getAspects() { return aspects.copy(); }
    public int getVis() { return aspects.visSize(); }
    public int getMaxVis() { return MAX_VIS; }
    public int getBurnTime() { return furnaceBurnTime; }
    public int getCurrentItemBurnTime() { return currentItemBurnTime; }
    public int getCookTime() { return furnaceCookTime; }
    public int getSmeltTime() { return smeltTime; }
    public int getBellowsCount() { return Math.max(0, bellows); }
    public boolean isSpeedBoost() { return speedBoost; }
    public boolean isBurning() { return furnaceBurnTime > 0; }

    private boolean isCurrent() {
        return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this
                && level.getBlockState(worldPosition).is(ThaumcraftDistillation.ALCHEMY_FURNACE.get());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileAlchemyFurnace tile) {
        if (!(level instanceof ServerLevel server) || !tile.isCurrent() || tile.processing) return;
        if (!server.getServer().isSameThread()) throw new IllegalStateException("Alchemy furnace ticking requires the server thread");
        tile.processing = true;
        try { tile.serverTick(server); }
        finally { tile.processing = false; }
    }

    private void serverTick(ServerLevel server) {
        ++count;
        int oldBurn = furnaceBurnTime, oldCook = furnaceCookTime, oldDuration = currentItemBurnTime, oldSmelt = smeltTime;
        // Fuel already burning is spent even when input is missing/unknown/full.
        if (furnaceBurnTime > 0) --furnaceBurnTime;
        if (bellows < 0) getBellows();
        if (count % (speedBoost ? 20 : 40) == 0 && aspects.size() > 0) transferToAlembics(server);
        if (!isCurrent()) return;
        if (furnaceBurnTime == 0 && canSmelt()) ignite();
        if (!isCurrent()) return;
        if (isBurning() && canSmelt()) {
            if (++furnaceCookTime >= smeltTime) {
                furnaceCookTime = 0;
                distillOne();
            }
        } else furnaceCookTime = 0;
        // Persist every live progress transition, without firing comparator
        // callbacks once per tick. Slots/transactions use normal setChanged().
        if (oldBurn != furnaceBurnTime || oldCook != furnaceCookTime || oldDuration != currentItemBurnTime || oldSmelt != smeltTime) server.blockEntityChanged(worldPosition);
        syncDisplay();
    }

    private void ignite() {
        ItemStack before = items.get(FUEL_SLOT).copy();
        int burn = getFuelTime(before);
        // Native remainder hooks receive a copy, never the physical fuel stack.
        ItemStack remainder = burn > 0 && before.getCount() == 1 ? ItemStackHooks.getCraftingRemainingItem(before.copy()).copy() : ItemStack.EMPTY;
        // A loader fuel/remainder callback must not spend a replaced stack/BE or
        // ignite against an input/capacity decision made before that callback.
        if (!isCurrent() || !ItemStack.matches(before, items.get(FUEL_SLOT)) || !canSmelt()) return;
        currentItemBurnTime = burn;
        if (burn <= 0) return;
        ItemStack after = before.copy();
        after.shrink(1);
        items.set(FUEL_SLOT, after.isEmpty() ? remainder : after);
        furnaceBurnTime = currentItemBurnTime = burn;
        speedBoost = before.is(ThaumcraftContent.MATERIALS.get("alumentum").get());
        setChanged();
    }

    public static int getFuelTime(ItemStack stack) { return stack.isEmpty() ? 0 : Math.max(0, FuelRegistry.get(stack.copy())); }
    public static boolean isItemFuel(ItemStack stack) { return getFuelTime(stack) > 0; }

    public static boolean hasAspects(ItemStack stack, Level level) { return ObjectAspectResolver.get(stack, level).size() > 0; }

    private record Distillation(ItemStack input, AspectList tags) {}

    private Distillation resolveInput() {
        ItemStack input = items.get(INPUT_SLOT).copy();
        if (input.isEmpty() || level == null) return null;
        AspectList tags = ObjectAspectResolver.get(input, level);
        if (tags.size() == 0) return null;
        long size = 0;
        for (Aspect aspect : tags.getAspects()) {
            int amount = tags.getAmount(aspect);
            if (amount < 0) return null;
            size += amount;
        }
        if (size > MAX_VIS - (long) getVis()) return null;
        // Source float multiplication and int truncation, NOT Alumentum /2.
        smeltTime = (int) ((float) (size * 10) * (1.0F - .125F * getBellowsCount()));
        return new Distillation(input, tags);
    }

    public boolean canSmelt() { return resolveInput() != null; }

    private boolean distillOne() {
        // Re-resolve at consumption, rather than trusting a menu or the earlier
        // per-tick capacity test. Commit one real input and its tags together.
        Distillation craft = resolveInput();
        if (craft == null || !isCurrent() || !ItemStack.matches(craft.input(), items.get(INPUT_SLOT))
                || craft.tags().visSize() > MAX_VIS - getVis()) return false;
        ItemStack next = craft.input().copy();
        next.shrink(1);
        items.set(INPUT_SLOT, next.isEmpty() ? ItemStack.EMPTY : next);
        aspects.add(craft.tags());
        // Original smeltItem does NOT return an input container or invent a
        // result slot. Only the fuel's final item has a native remainder.
        setChanged();
        return true;
    }

    private void transferToAlembics(ServerLevel server) {
        if (transferring) return;
        transferring = true;
        try {
            AspectList excluded = new AspectList();
            // First pass may refill EACH already-typed buffer, including a
            // typed zero-amount filter alias. Filter is irrelevant in this pass.
            for (int depth = 1; depth <= 5 && isCurrent(); depth++) {
                TileAlembic target = alembicAt(depth);
                if (target == null) break;
                Aspect type = target.getAspect();
                if (type != null && target.getAmount() < target.getMaxAmount() && aspects.getAmount(type) > 0
                        && pushOne(target, type)) excluded.merge(type, 1);
            }
            // Second pass initializes at most ONE empty alembic. A filter may
            // request a first-pass-excluded tag; only random selection excludes.
            for (int depth = 1; depth <= 5 && isCurrent(); depth++) {
                TileAlembic target = alembicAt(depth);
                if (target == null) break;
                if (target.getAspect() != null && target.getAmount() != 0) continue;
                Aspect type = target.getFilter();
                if (type == null) {
                    Aspect[] choices = Arrays.stream(aspects.getAspects()).filter(aspect -> excluded.getAmount(aspect) == 0).toArray(Aspect[]::new);
                    // Exactly one world RNG draw per eligible unfiltered empty,
                    // none for a filter or an empty candidate set.
                    if (choices.length > 0) type = choices[server.random.nextInt(choices.length)];
                }
                if (type != null && pushOne(target, type)) break;
            }
        } finally { transferring = false; }
    }

    private TileAlembic alembicAt(int depth) {
        BlockPos pos = worldPosition.above(depth);
        return level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof TileAlembic target && !target.isRemoved() ? target : null;
    }

    private boolean pushOne(TileAlembic target, Aspect type) {
        int before = target.getAmount();
        if (!isCurrent() || type == null || aspects.getAmount(type) <= 0 || target.isRemoved()
                || level.getBlockEntity(target.getBlockPos()) != target || target.getLevel() != level
                || before < 0 || before >= target.getMaxAmount() || before > 0 && target.getAspect() != type) return false;
        int remainder = target.addToContainer(type, 1);
        if (remainder != 0) return false;
        // Only an actually accepted unit is paid. No output interface/pipe
        // simulation, and zero-valued source entries cannot mint essentia.
        if (target.getAspect() != type || target.getAmount() != before + 1) return false;
        if (target.isRemoved() || level.getBlockEntity(target.getBlockPos()) != target) {
            // A synchronous native update replaced the destination. Its buffer
            // is no longer real storage, so retain the source unit. Never mutate
            // the replacement BE or debit for a detached destination.
            return false;
        }
        aspects.remove(type, 1);
        if (isCurrent()) setChanged();
        return true;
    }

    /** Original checks all six adjacent directions, exact orientation, and no redstone power. */
    public void getBellows() {
        bellows = 0;
        if (level == null) return;
        Block bellowsBlock = BuiltInRegistries.BLOCK.getOptional(BELLOWS).orElse(null);
        // Only a genuine registered bellows counts, never substitute fans.
        if (bellowsBlock == null) return;
        for (Direction direction : Direction.values()) {
            BlockPos pos = worldPosition.relative(direction);
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!state.is(bellowsBlock) || level.hasNeighborSignal(pos)) continue;
            Direction facing = null;
            for (var property : state.getProperties()) {
                if (property instanceof DirectionProperty facingProperty
                        && (property.getName().equals("facing") || property.getName().equals("orientation"))) {
                    facing = state.getValue(facingProperty);
                    break;
                }
            }
            // Future native bellows may retain TC4's BE orientation byte instead
            // of exposing a blockstate direction. Never guess an absent value.
            BlockEntity tile = level.getBlockEntity(pos);
            if (facing == null && tile != null) {
                CompoundTag tag = tile.saveWithoutMetadata(level.registryAccess());
                if (tag.contains("orientation", Tag.TAG_ANY_NUMERIC)) {
                    int orientation = tag.getByte("orientation");
                    if (orientation >= 0 && orientation < 6) facing = Direction.from3DDataValue(orientation);
                }
            }
            if (facing == direction.getOpposite()) ++bellows;
        }
    }

    private void syncDisplay() {
        if (!isCurrent()) return;
        BlockState before = level.getBlockState(worldPosition);
        BlockState after = before.setValue(BlockAlchemyFurnace.LIT, isBurning()).setValue(BlockAlchemyFurnace.FILLED, getVis() > 0);
        if (before != after) level.setBlock(worldPosition, after, Block.UPDATE_CLIENTS);
    }

    @Override public int getContainerSize() { return 2; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == INPUT_SLOT ? hasAspects(stack, level) : slot == FUEL_SLOT && isItemFuel(stack); }
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[] {FUEL_SLOT} : side == Direction.UP ? new int[0] : new int[] {INPUT_SLOT}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return side != Direction.UP && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return side != Direction.DOWN || slot != FUEL_SLOT || stack.is(Items.BUCKET); }

    @Override public boolean stillValid(Player player) {
        return isCurrent() && player.level() == level
                && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64;
    }

    /** Drops exactly the two stored stacks once, never an aspect/result preview. */
    public void evacuate() {
        if (!(level instanceof ServerLevel server) || evacuating || isEmpty()) return;
        evacuating = true;
        try {
            DeferredItemDrops.enqueue(server, worldPosition, items.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList());
            items.clear();
            // Deliberately NOT setChanged(): it invokes neighboring comparators
            // while this BE is still inside LevelChunk's removal operation.
        } finally { evacuating = false; }
    }

    @Override public Component getName() { return customName == null ? Component.translatable("container.alchemyfurnace") : customName; }
    @Override public Component getDisplayName() { return getName(); }
    @Override public Component getCustomName() { return customName; }
    public void setCustomName(Component name) { customName = name; setChanged(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ContainerAlchemyFurnace(id, inventory, this); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        // Original TileAlchemyFurnace:125-126,154 really writes these as shorts.
        tag.putShort("BurnTime", (short) furnaceBurnTime);
        tag.putShort("CookTime", (short) furnaceCookTime);
        tag.putShort("Vis", (short) getVis());
        tag.putBoolean("speedBoost", speedBoost);
        aspects.writeToNBT(tag);
        if (customName != null) tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
        // Additional native UI state avoids guessing the duration from the NEXT
        // fuel stack after loading; it does not change burn/cook mechanics.
        tag.putInt("CurrentItemBurnTime", currentItemBurnTime);
        tag.putInt("SmeltTime", smeltTime);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        furnaceBurnTime = tag.getShort("BurnTime");
        furnaceCookTime = tag.getShort("CookTime");
        speedBoost = tag.getBoolean("speedBoost");
        aspects.readFromNBT(tag);
        // Vis is a cache in TC4; the persisted aspect list is authoritative.
        customName = tag.contains("CustomName", Tag.TAG_STRING) ? parseCustomNameSafe(tag.getString("CustomName"), registries) : null;
        currentItemBurnTime = tag.contains("CurrentItemBurnTime", Tag.TAG_ANY_NUMERIC) ? tag.getInt("CurrentItemBurnTime") : getFuelTime(items.get(FUEL_SLOT));
        smeltTime = tag.contains("SmeltTime", Tag.TAG_ANY_NUMERIC) ? tag.getInt("SmeltTime") : 100;
        bellows = -1;
        count = 0;
    }

    @Override protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        customName = input.get(DataComponents.CUSTOM_NAME);
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (customName != null) builder.set(DataComponents.CUSTOM_NAME, customName);
    }
    @Override public void removeComponentsFromTag(CompoundTag tag) { super.removeComponentsFromTag(tag); tag.remove("CustomName"); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
