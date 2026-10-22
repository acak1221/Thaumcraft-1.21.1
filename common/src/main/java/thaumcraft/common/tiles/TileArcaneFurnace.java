package thaumcraft.common.tiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.blocks.BlockArcaneFurnace;
import thaumcraft.common.blocks.BlockArcaneFurnaceNozzle;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.lib.tiles.LegacyTileRules;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/**
 * Original TileArcaneFurnace: the furnace core of the three by three by three arcane furnace.
 * <p>
 * The core owns thirty two slots, cooks one item at a time and ejects the result towards its output nozzle.
 * A bellows two blocks away takes twenty ticks off the cook time, and five fire vis a tick from the vis
 * network make the furnace cook in eighty ticks instead of one hundred and forty.
 * <p>
 * Oracle: {@code thaumcraft/common/tiles/TileArcaneFurnace.java}, lines 38-305.
 */
public class TileArcaneFurnace extends RetainedDataBlockEntity {
    /** The addon hook of the original {@code ThaumcraftApi.getSmeltingBonus}. */
    public interface SmeltingBonus { ItemStack bonusFor(ItemStack input); }
    private static SmeltingBonus bonusProvider = input -> null;

    /** Installs the addon smelting bonus provider; the original asks the api for it on every smelt. */
    public static void setSmeltingBonus(SmeltingBonus provider) { bonusProvider = provider == null ? input -> null : provider; }

    private final ItemStack[] furnaceItemStacks = new ItemStack[LegacyTileRules.FURNACE_SLOTS];
    /** TileArcaneFurnace:42 - the ticks the current item still needs. */
    public int furnaceCookTime;
    /** TileArcaneFurnace:43 - the ticks the current item needs in total. */
    public int furnaceMaxCookTime;
    /** TileArcaneFurnace:44 - the ticks of speedy cooking the furnace still has. */
    public int speedyTime;
    /** TileArcaneFurnace:45-46 - the direction the results leave in; minus five means "not worked out yet". */
    public int facingX = -5;
    public int facingZ = -5;
    /** The blaze of the collapsed furnace is released exactly once. */
    private boolean blazeReleased;

    public TileArcaneFurnace(BlockPos pos, BlockState state) {
        super(ThaumcraftLegacyTiles.ARCANE_FURNACE_BLOCK_ENTITY.get(), pos, state);
        for (int slot = 0; slot < furnaceItemStacks.length; slot++) furnaceItemStacks[slot] = ItemStack.EMPTY;
    }
    TileArcaneFurnace(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type,pos,state); java.util.Arrays.fill(furnaceItemStacks,ItemStack.EMPTY);
    }

    /** TileArcaneFurnace:47-49 - the furnace has thirty two slots. */
    public int getSizeInventory() { return furnaceItemStacks.length; }
    /** TileArcaneFurnace:51-53 - the stack in one slot. */
    public ItemStack getStackInSlot(int slot) { return !isDataRejected() && slot >= 0 && slot < furnaceItemStacks.length ? furnaceItemStacks[slot] : ItemStack.EMPTY; }

    /** TileArcaneFurnace:55-71 - take some items out of a slot. */
    public ItemStack decrStackSize(int slot, int amount) {
        if (isDataRejected()) return ItemStack.EMPTY;
        if (slot < 0 || slot >= furnaceItemStacks.length || furnaceItemStacks[slot].isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack stack = furnaceItemStacks[slot];
        if (stack.getCount() <= amount) {
            furnaceItemStacks[slot] = ItemStack.EMPTY;
            setChanged();
            return stack;
        }
        ItemStack split = stack.split(amount);
        if (stack.isEmpty()) furnaceItemStacks[slot] = ItemStack.EMPTY;
        setChanged();
        return split;
    }

    /** TileArcaneFurnace:73-79 - put a stack into a slot; a slot holds sixty four items. */
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (isDataRejected()) return;
        if (slot < 0 || slot >= furnaceItemStacks.length) return;
        if (!stack.isEmpty() && stack.getCount() > LegacyTileRules.FURNACE_STACK_LIMIT) stack.setCount(LegacyTileRules.FURNACE_STACK_LIMIT);
        furnaceItemStacks[slot] = stack;
        setChanged();
    }

    /** The whole inventory, for the menus and for the drops. */
    public ItemStack[] contents() { return isDataRejected()?new ItemStack[0]:furnaceItemStacks; }

    /** Whether the blaze of the collapsed furnace was already released. */
    public boolean blazeReleased() { return blazeReleased; }

    /** The furnace releases exactly one blaze when it collapses. */
    public void markBlazeReleased() { blazeReleased = true; }

    /** BlockContainer:breakBlock - the contents of the furnace are dropped when it breaks. */
    public void dropContents(ServerLevel level) {
        if (isDataRejected()) return;
        List<ItemStack> drops = new ArrayList<>();
        for (int slot = 0; slot < furnaceItemStacks.length; slot++) {
            if (furnaceItemStacks[slot].isEmpty()) continue;
            drops.add(furnaceItemStacks[slot].copy());
            furnaceItemStacks[slot] = ItemStack.EMPTY;
        }
        if (!drops.isEmpty()) DeferredItemDrops.enqueue(level, worldPosition, drops);
        setChanged();
    }

    /** TileArcaneFurnace:131-133 - the furnace drains five fire vis a tick from the vis network. */
    public int drainVis(Level level) {
        if (isDataRejected()) return 0;
        return VisNetHandler.drainVis(level, worldPosition, Aspect.FIRE, LegacyTileRules.FURNACE_VIS_DRAIN);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileArcaneFurnace tile) {
        if (tile.isDataRejected()) return;
        if (tile.level != level || !pos.equals(tile.worldPosition) || tile.isRemoved() || tile.getBlockState() != state) return;
        if (tile.facingX == -5) tile.getFacing(level);
        if (level.isClientSide || !(level instanceof ServerLevel server)) return;
        if (!server.getServer().isSameThread()) throw new IllegalStateException("Arcane furnace ticks require the server thread");
        boolean cookedFlag = false;
        if (tile.furnaceCookTime > 0) {
            --tile.furnaceCookTime;
            cookedFlag = true;
        }
        if (cookedFlag && tile.speedyTime > 0) --tile.speedyTime;
        if (tile.speedyTime <= 0) tile.speedyTime = tile.drainVis(level);
        if (tile.furnaceMaxCookTime == 0) tile.furnaceMaxCookTime = tile.calcCookTime(level);
        if (tile.furnaceCookTime > tile.furnaceMaxCookTime) tile.furnaceCookTime = tile.furnaceMaxCookTime;
        if (tile.furnaceCookTime == 0 && cookedFlag) {
            for (int slot = 0; slot < tile.getSizeInventory(); slot++) {
                ItemStack source = tile.furnaceItemStacks[slot];
                if (source.isEmpty()) continue;
                ItemStack result = tile.smeltResult(level, source);
                if (result.isEmpty()) continue;
                tile.ejectItem(level, result, source);
                level.blockEvent(pos, state.getBlock(), 3, 0);
                source.shrink(1);
                if (source.isEmpty()) tile.furnaceItemStacks[slot] = ItemStack.EMPTY;
                tile.setChanged();
                break;
            }
        }
        if (tile.furnaceCookTime == 0 && !cookedFlag) {
            for (int slot = 0; slot < tile.getSizeInventory(); slot++) {
                if (!tile.canSmelt(level, slot)) continue;
                tile.furnaceCookTime = tile.furnaceMaxCookTime = tile.calcCookTime(level);
                tile.setChanged();
                break;
            }
        }
    }

    /**
     * TileArcaneFurnace:162-173 - a bellows two blocks away, facing the furnace and unpowered, makes the
     * furnace cook faster. At most three bellows count.
     */
    public int getBellows(Level level) {
        int bellows = 0;
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue;
            BlockPos bellowsPos = worldPosition.offset(direction.getStepX() * 2, direction.getStepY() * 2, direction.getStepZ() * 2);
            if (!level.hasChunkAt(bellowsPos)) continue;
            if (!(level.getBlockEntity(bellowsPos) instanceof TileBellows tile)) continue;
            if (tile.getFacing() != direction.getOpposite() || tile.isPowered()) continue;
            ++bellows;
        }
        return LegacyTileRules.clampBellows(bellows);
    }

    /** TileArcaneFurnace:175-177 - the cook time of one item. */
    public int calcCookTime(Level level) {
        return LegacyTileRules.cookTime(speedyTime > 0, getBellows(level));
    }

    /**
     * TileArcaneFurnace:179-198 - the mouth of the furnace swallows a dropped stack. A stack that cannot be
     * smelted is burnt away with a fizz.
     */
    public boolean addItemsToInventory(ItemStack items) {
        if (isDataRejected()) return false;
        if (items == null || items.isEmpty()) return false;
        for (int slot = 0; slot < getSizeInventory(); slot++) {
            ItemStack stack = furnaceItemStacks[slot];
            if (!stack.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(stack, items)) continue;
                if (stack.getCount() + items.getCount() > items.getMaxStackSize()) continue;
                stack.grow(items.getCount());
                if (!canSmelt(level, slot)) destroyItem(level, slot);
                setChanged();
                return true;
            }
            setInventorySlotContents(slot, items.copy());
            if (!canSmelt(level, slot)) destroyItem(level, slot);
            setChanged();
            return true;
        }
        return false;
    }

    /** TileArcaneFurnace:200-207 - a stack that cannot be smelted is lost with a fizz and a lava particle. */
    private void destroyItem(Level level, int slot) {
        furnaceItemStacks[slot] = ItemStack.EMPTY;
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, .3F,
                2.6F + (level.random.nextFloat() - level.random.nextFloat()) * .8F);
        level.addParticle(net.minecraft.core.particles.ParticleTypes.LAVA,
                worldPosition.getX() + level.random.nextFloat(), worldPosition.getY() + 1D, worldPosition.getZ() + level.random.nextFloat(),
                0D, 0D, 0D);
    }

    /** TileArcaneFurnace:209-219 - the core ejects towards the output nozzle next to it. */
    public void getFacing(Level level) {
        facingX = 0;
        facingZ = 0;
        boolean west = isOutputNozzle(level, worldPosition.west());
        boolean east = isOutputNozzle(level, worldPosition.east());
        boolean north = isOutputNozzle(level, worldPosition.north());
        LegacyTileRules.Facing facing = LegacyTileRules.furnaceFacing(west, east, north);
        facingX = facing.x();
        facingZ = facing.z();
    }

    /** Whether the given cell holds the output nozzle of the furnace. */
    public static boolean isOutputNozzle(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BlockArcaneFurnaceNozzle;
    }

    /**
     * TileArcaneFurnace:221-283 - the result leaves the furnace towards its nozzle, with the smelting bonus
     * and the experience of the recipe.
     */
    public void ejectItem(Level level, ItemStack items, ItemStack furnaceItemStack) {
        if (isDataRejected()) return;
        if (items == null || items.isEmpty()) return;
        ItemStack bit = items.copy();
        int bellows = getBellows(level);
        float lx = .5F;
        float lz = .5F;
        float mx = LegacyTileRules.ejectMomentum(facingX);
        float mz = LegacyTileRules.ejectMomentum(facingZ);
        lx += facingX * LegacyTileRules.EJECT_OFFSET;
        lz += facingZ * LegacyTileRules.EJECT_OFFSET;
        ItemEntity entity = new ItemEntity(level, worldPosition.getX() + lx, worldPosition.getY() + .4F, worldPosition.getZ() + lz, items);
        entity.setDeltaMovement(mx, 0D, mz);
        level.addFreshEntity(entity);
        ItemStack bonus = bonusProvider.bonusFor(furnaceItemStack);
        // The registry entry only carries the identity of the bonus; the amount
        // is decided by the bellows rolls below (oracle TileArcaneFurnace.java:237-259).
        if (bonus != null) {
            int count = 0;
            int rolls = LegacyTileRules.bonusRolls(bellows);
            float chance = LegacyTileRules.bonusChance(bellows);
            RandomSource random = level.random;
            for (int roll = 0; roll < rolls; roll++) if (random.nextFloat() < chance) ++count;
            if (count > 0) {
                // Build the paid stack from the item, so a registry entry without
                // an amount can never turn into the shared empty stack.
                ItemStack bonusStack = new ItemStack(bonus.getItem(), count);
                mx = LegacyTileRules.ejectMomentum(facingX);
                mz = LegacyTileRules.ejectMomentum(facingZ);
                ItemEntity bonusEntity = new ItemEntity(level, worldPosition.getX() + lx, worldPosition.getY() + .4F, worldPosition.getZ() + lz, bonusStack);
                bonusEntity.setDeltaMovement(mx, 0D, mz);
                level.addFreshEntity(bonusEntity);
            }
        }
        int amount = items.getCount();
        float experience = smeltExperience(level, bit);
        if (experience == 0F) {
            amount = 0;
        } else if (experience < 1F) {
            int floor = (int) (amount * experience);
            if (floor < Math.ceil(amount * experience) && level.random.nextFloat() < amount * experience - floor) ++floor;
            amount = floor;
        }
        while (amount > 0) {
            int value = ExperienceOrb.getExperienceValue(amount);
            amount -= value;
            ExperienceOrb orb = new ExperienceOrb(level, worldPosition.getX() + lx, worldPosition.getY() + .4F, worldPosition.getZ() + lz, value);
            orb.setDeltaMovement(LegacyTileRules.ejectMomentum(facingX), 0D, LegacyTileRules.ejectMomentum(facingZ));
            level.addFreshEntity(orb);
        }
    }

    /** The smelting result of a stack, or an empty stack when it cannot be smelted. */
    public ItemStack smeltResult(Level level, ItemStack stack) {
        Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
        return recipe.map(holder -> holder.value().assemble(new SingleRecipeInput(stack), level.registryAccess()).copy())
                .orElse(ItemStack.EMPTY);
    }

    /** The experience the recipe pays, or zero when there is none. */
    public float smeltExperience(Level level, ItemStack stack) {
        Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
        return recipe.map(holder -> holder.value().getExperience()).orElse(0F);
    }

    /** TileArcaneFurnace:285-291 - a stack can be smelted when the recipe book knows a recipe for it. */
    public boolean canSmelt(Level level, int slot) {
        if (isDataRejected()) return false;
        ItemStack stack = furnaceItemStacks[slot];
        if (stack == null || stack.isEmpty()) return false;
        return !smeltResult(level, stack).isEmpty();
    }

    /** The client side lava effect of the finished smelt. */
    public void clientSmeltEffect(Level level) {
        for (int round = 0; round < 5; round++) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.LAVA,
                    worldPosition.getX() + .5D + (level.random.nextFloat() - .5F) * .4D,
                    worldPosition.getY() + .5D + level.random.nextFloat() * .2D,
                    worldPosition.getZ() + .5D + (level.random.nextFloat() - .5F) * .4D,
                    facingX * .02D, .02D, facingZ * .02D);
            level.playLocalSound(worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D,
                    net.minecraft.sounds.SoundEvents.LAVA_POP, net.minecraft.sounds.SoundSource.BLOCKS,
                    .1F + level.random.nextFloat() * .1F, .9F + level.random.nextFloat() * .15F, false);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (saveRejected(tag)) return;
        super.saveAdditional(tag, registries);
        tag.putShort("CookTime", (short) furnaceCookTime);
        tag.putShort("SpeedyTime", (short) speedyTime);
        tag.put("Items", writeItems(registries));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (retainMalformed(tag, () -> validateSlots(tag,"Items",furnaceItemStacks.length,registries))) return;
        super.loadAdditional(tag, registries);
        furnaceCookTime = tag.getShort("CookTime");
        speedyTime = tag.getShort("SpeedyTime");
        for (int slot = 0; slot < furnaceItemStacks.length; slot++) furnaceItemStacks[slot] = ItemStack.EMPTY;
        var list = tag.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            int slot = entry.getByte("Slot") & 0xFF;
            if (slot < 0 || slot >= furnaceItemStacks.length) continue;
            furnaceItemStacks[slot] = requiredStack(registries, entry.get("Stack"));
        }
    }

    private net.minecraft.nbt.ListTag writeItems(HolderLookup.Provider registries) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (int slot = 0; slot < furnaceItemStacks.length; slot++) {
            if (furnaceItemStacks[slot].isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) slot);
            entry.put("Stack", furnaceItemStacks[slot].save(registries));
            list.add(entry);
        }
        return list;
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide && !isRemoved() && level.hasChunkAt(worldPosition)) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
