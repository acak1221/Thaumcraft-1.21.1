package thaumcraft.common.lib.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/**
 * Original TC4 {@code thaumcraft.common.lib.utils.InventoryUtils}: the inventory helpers of the arcane workbench,
 * the infusion matrix, the hungry chest and the pedestals.
 *
 * <p>1.7.10 knew the Forge {@code IInventory} and its sided variant; 1.21.1 knows {@link Container} and
 * {@link WorldlyContainer}. The side numbers are the {@code ForgeDirection} ordinals, which are the same as the
 * native {@link Direction} ordinals.
 *
 * <p>The item metadata of 1.7.10 is the damage value of 1.21.1. The wildcard metadata of the original
 * ({@code Short.MAX_VALUE}) has no native form, so the comparisons that used it are documented where they occur.
 */
public final class InventoryUtils {
    /** The native replacement of the Forge ore dictionary: the original ore names mapped to native item tags. */
    private static final Map<String, TagKey<Item>> oreTags = new HashMap<>();

    private InventoryUtils() {}

    /** Register the native item tag of an original ore dictionary name. */
    public static void registerOreTag(String oreName, TagKey<Item> tag) {
        if (oreName == null || oreName.isEmpty() || tag == null) return;
        oreTags.put(oreName, tag);
    }

    /** The items of an original ore dictionary name. */
    public static List<ItemStack> oresOf(String oreName) {
        TagKey<Item> tag = oreTags.get(oreName);
        if (tag == null) return List.of();
        var registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        var holder = registry.getTag(tag).orElse(null);
        if (holder == null) return List.of();
        List<ItemStack> ores = new ArrayList<>();
        holder.forEach(item -> ores.add(new ItemStack(item.value())));
        return ores;
    }

    /**
     * The original {@code OreDictionary} shortcut: are the two items members of the same ore dictionary name?
     *
     * <p>Forced 1.21.1 change: the original asked the Forge ore dictionary for the FIRST ore id of the first stack
     * and compared the second stack against the ores of that one id. The native port asks every registered original
     * ore name, so the answer is the same for every material the original recipes use.</p>
     */
    public static boolean matchesOre(ItemStack stack0, ItemStack stack1) {
        if (stack0 == null || stack1 == null || stack0.isEmpty() || stack1.isEmpty()) return false;
        for (TagKey<Item> tag : oreTags.values()) {
            if (!stack0.is(tag)) continue;
            if (stack1.is(tag)) return true;
        }
        return false;
    }

    /** The original ore names this port knows, with their native item tags. */
    public static Map<String, TagKey<Item>> registeredOreTags() {
        return java.util.Collections.unmodifiableMap(oreTags);
    }

    // -------------------------------------------------------------------- insertion

    /** Original {@code placeItemStackIntoInventory(ItemStack, IInventory, int, boolean)}. */
    public static ItemStack placeItemStackIntoInventory(ItemStack stack, Container inventory, int side, boolean doit) {
        if (stack == null || inventory == null) return null;
        ItemStack itemstack = stack.copy();
        ItemStack itemstack1 = insertStack(inventory, itemstack, side, doit);
        if (itemstack1 == null || itemstack1.isEmpty()) {
            if (doit) inventory.setChanged();
            return null;
        }
        return itemstack1.copy();
    }

    /** Original {@code insertStack(IInventory, ItemStack, int, boolean)}. */
    public static ItemStack insertStack(Container inventory, ItemStack stack1, int side, boolean doit) {
        if (inventory == null || stack1 == null) return null;
        if (inventory instanceof WorldlyContainer sided && side > -1) {
            Direction direction = directionOf(side);
            int[] slots = direction == null ? null : sided.getSlotsForFace(direction);
            if (slots != null) {
                for (int slot : slots) {
                    if (stack1 == null || stack1.isEmpty()) break;
                    ItemStack present = inventory.getItem(slot);
                    if (present != null && !present.isEmpty() && ItemStack.isSameItem(present, stack1)) {
                        stack1 = attemptInsertion(inventory, stack1, slot, side, doit);
                    }
                    if (stack1 == null || stack1.isEmpty()) break;
                }
                if (slots != null && stack1 != null && !stack1.isEmpty()) {
                    for (int slot : slots) {
                        if (stack1 == null || stack1.isEmpty()) break;
                        stack1 = attemptInsertion(inventory, stack1, slot, side, doit);
                    }
                }
            }
        } else {
            int size = inventory.getContainerSize();
            for (int slot = 0; slot < size; ++slot) {
                if (stack1 == null || stack1.isEmpty()) break;
                ItemStack present = inventory.getItem(slot);
                if (present != null && !present.isEmpty() && ItemStack.isSameItem(present, stack1)) {
                    stack1 = attemptInsertion(inventory, stack1, slot, side, doit);
                }
                if (stack1 == null || stack1.isEmpty()) break;
            }
            if (stack1 != null && !stack1.isEmpty()) {
                BlockEntity tile = inventory instanceof BlockEntity entity ? entity : null;
                ChestBlockEntity doubleChest = getDoubleChest(tile);
                if (doubleChest != null) {
                    int chestSize = doubleChest.getContainerSize();
                    for (int slot = 0; slot < chestSize; ++slot) {
                        if (stack1 == null || stack1.isEmpty()) break;
                        ItemStack present = doubleChest.getItem(slot);
                        if (present != null && !present.isEmpty() && ItemStack.isSameItem(present, stack1)) {
                            stack1 = attemptInsertion(doubleChest, stack1, slot, side, doit);
                        }
                        if (stack1 == null || stack1.isEmpty()) break;
                    }
                }
                if (stack1 != null && !stack1.isEmpty()) {
                    for (int slot = 0; slot < size; ++slot) {
                        if (stack1 == null || stack1.isEmpty()) break;
                        stack1 = attemptInsertion(inventory, stack1, slot, side, doit);
                    }
                    if (stack1 != null && !stack1.isEmpty() && doubleChest != null) {
                        int chestSize = doubleChest.getContainerSize();
                        for (int slot = 0; slot < chestSize; ++slot) {
                            if (stack1 == null || stack1.isEmpty()) break;
                            ItemStack present = doubleChest.getItem(slot);
                            if (present != null && !present.isEmpty() && ItemStack.isSameItem(present, stack1)) {
                                stack1 = attemptInsertion(doubleChest, stack1, slot, side, doit);
                            }
                            if (stack1 == null || stack1.isEmpty()) break;
                        }
                    }
                }
            }
        }
        if (stack1 != null && stack1.isEmpty()) stack1 = null;
        return stack1;
    }

    /** Original {@code attemptInsertion(IInventory, ItemStack, int, int, boolean)}. */
    public static ItemStack attemptInsertion(Container inventory, ItemStack stack, int slot, int side, boolean doit) {
        if (inventory == null || stack == null) return stack;
        ItemStack slotStack = inventory.getItem(slot);
        if (!canInsertItemToInventory(inventory, stack, slot, side)) return stack;
        boolean flag = false;
        if (slotStack == null || slotStack.isEmpty()) {
            if (inventory.getMaxStackSize() < stack.getCount()) {
                ItemStack in = stack.split(inventory.getMaxStackSize());
                if (doit) inventory.setItem(slot, in);
            } else {
                if (doit) inventory.setItem(slot, stack);
                stack = null;
            }
            flag = true;
        } else if (areItemStacksEqualStrict(slotStack, stack)) {
            int k = Math.min(inventory.getMaxStackSize() - slotStack.getCount(), stack.getMaxStackSize() - slotStack.getCount());
            int l = Math.min(stack.getCount(), k);
            stack.setCount(stack.getCount() - l);
            if (doit) slotStack.setCount(slotStack.getCount() + l);
            flag = l > 0;
        }
        if (flag && doit) inventory.setChanged();
        return stack;
    }

    /** Original {@code getFirstItemInInventory(IInventory, int, int, boolean)}. */
    public static ItemStack getFirstItemInInventory(Container inventory, int size, int side, boolean doit) {
        if (inventory == null) return null;
        ItemStack stack1 = null;
        if (inventory instanceof WorldlyContainer sided && side > -1) {
            Direction direction = directionOf(side);
            int[] slots = direction == null ? null : sided.getSlotsForFace(direction);
            if (slots != null) {
                for (int slot : slots) {
                    if (stack1 == null && inventory.getItem(slot) != null && !inventory.getItem(slot).isEmpty()) {
                        stack1 = inventory.getItem(slot).copy();
                        stack1.setCount(size);
                    }
                    if (stack1 != null) stack1 = attemptExtraction(inventory, stack1, slot, side, false, false, false, doit);
                    if (stack1 == null) continue;
                    break;
                }
            }
        } else {
            int size2 = inventory.getContainerSize();
            for (int slot = 0; slot < size2; ++slot) {
                if (stack1 == null && inventory.getItem(slot) != null && !inventory.getItem(slot).isEmpty()) {
                    stack1 = inventory.getItem(slot).copy();
                    stack1.setCount(size);
                }
                if (stack1 != null) stack1 = attemptExtraction(inventory, stack1, slot, side, false, false, false, doit);
                if (stack1 == null) continue;
                break;
            }
        }
        if (stack1 == null || stack1.isEmpty()) {
            if (doit) inventory.setChanged();
            return null;
        }
        return stack1.copy();
    }

    // ------------------------------------------------------------------- extraction

    /** Original {@code inventoryContains(IInventory, ItemStack, int, boolean, boolean, boolean)}. */
    public static boolean inventoryContains(Container inventory, ItemStack stack, int side, boolean useOre, boolean ignoreDamage, boolean ignoreNBT) {
        return extractStack(inventory, stack, side, useOre, ignoreDamage, ignoreNBT, false) != null;
    }

    /** Original {@code extractStack(IInventory, ItemStack, int, boolean, boolean, boolean, boolean)}. */
    public static ItemStack extractStack(Container inventory, ItemStack stack1, int side, boolean useOre, boolean ignoreDamage, boolean ignoreNBT, boolean doit) {
        if (inventory == null || stack1 == null) return null;
        ItemStack outStack = null;
        if (inventory instanceof WorldlyContainer sided && side > -1) {
            Direction direction = directionOf(side);
            int[] slots = direction == null ? null : sided.getSlotsForFace(direction);
            if (slots != null) {
                for (int slot : slots) {
                    if (stack1 == null || stack1.isEmpty() || outStack != null) break;
                    outStack = attemptExtraction(inventory, stack1, slot, side, useOre, ignoreDamage, ignoreNBT, doit);
                }
            }
        } else {
            int size = inventory.getContainerSize();
            for (int slot = 0; slot < size; ++slot) {
                if (stack1 == null || stack1.isEmpty() || outStack != null) break;
                outStack = attemptExtraction(inventory, stack1, slot, side, useOre, ignoreDamage, ignoreNBT, doit);
            }
        }
        if (outStack == null || outStack.isEmpty()) return null;
        return outStack.copy();
    }

    /** Original {@code attemptExtraction(IInventory, ItemStack, int, int, boolean, boolean, boolean, boolean)}. */
    public static ItemStack attemptExtraction(Container inventory, ItemStack stack, int slot, int side, boolean useOre,
                                              boolean ignoreDamage, boolean ignoreNBT, boolean doit) {
        if (inventory == null || stack == null) return null;
        ItemStack slotStack = inventory.getItem(slot);
        ItemStack outStack = stack.copy();
        if (!canExtractItemFromInventory(inventory, slotStack, slot, side)) return null;
        if (!areItemStacksEqual(slotStack, stack, useOre, ignoreDamage, ignoreNBT)) return null;
        outStack = slotStack.copy();
        outStack.setCount(stack.getCount());
        int k = stack.getCount() - slotStack.getCount();
        if (k >= 0) {
            outStack.setCount(outStack.getCount() - k);
            if (doit) inventory.setItem(slot, ItemStack.EMPTY);
        } else if (doit) {
            slotStack.setCount(slotStack.getCount() - outStack.getCount());
            inventory.setItem(slot, slotStack);
        }
        if (doit) inventory.setChanged();
        return outStack;
    }

    /** Original {@code canInsertItemToInventory(IInventory, ItemStack, int, int)}. */
    public static boolean canInsertItemToInventory(Container inventory, ItemStack stack1, int slot, int side) {
        if (inventory == null || stack1 == null) return false;
        if (!inventory.canPlaceItem(slot, stack1)) return false;
        if (inventory instanceof WorldlyContainer sided) {
            Direction direction = directionOf(side);
            if (direction != null && !sided.canPlaceItemThroughFace(slot, stack1, direction)) return false;
        }
        return true;
    }

    /** Original {@code canExtractItemFromInventory(IInventory, ItemStack, int, int)}. */
    public static boolean canExtractItemFromInventory(Container inventory, ItemStack stack1, int slot, int side) {
        if (inventory == null || stack1 == null) return false;
        if (inventory instanceof WorldlyContainer sided) {
            Direction direction = directionOf(side);
            if (direction != null && !sided.canTakeItemThroughFace(slot, stack1, direction)) return false;
        }
        return true;
    }

    // -------------------------------------------------------------------- equality

    /** Original {@code compareMultipleItems(ItemStack, ItemStack[])}. */
    public static boolean compareMultipleItems(ItemStack c1, ItemStack[] c2) {
        if (c1 == null || c1.getCount() <= 0 || c2 == null) return false;
        for (ItemStack is : c2) {
            if (is == null || !ItemStack.isSameItem(c1, is) || !ItemStack.isSameItemSameComponents(c1, is)) continue;
            return true;
        }
        return false;
    }

    /** Original {@code areItemStacksEqualStrict(ItemStack, ItemStack)}. */
    public static boolean areItemStacksEqualStrict(ItemStack stack0, ItemStack stack1) {
        return areItemStacksEqual(stack0, stack1, false, false, false);
    }

    /**
     * Original {@code areItemStacksEqualForCrafting(ItemStack, ItemStack, boolean, boolean, boolean)}.
     *
     * <p>The original compared the tags with the crafting rule: a recipe tag that the slot does not carry is a
     * mismatch, but a slot with more tags than the recipe still matches.
     */
    public static boolean areItemStacksEqualForCrafting(ItemStack stack0, ItemStack stack1, boolean useOre, boolean ignoreDamage, boolean ignoreNBT) {
        if (stack0 == null || stack1 == null) return false;
        if (stack0.isEmpty() && stack1.isEmpty()) return true;
        if (useOre && matchesOre(stack0, stack1)) return true;
        boolean t1 = true;
        if (!ignoreNBT) t1 = areItemStackTagsEqualForCrafting(stack0, stack1);
        boolean t2 = stack0.getDamageValue() != stack1.getDamageValue();
        if (ignoreDamage && stack0.isDamageableItem() && stack1.isDamageableItem()) t2 = false;
        if (t2) return false;
        return stack0.getItem() == stack1.getItem() && t1;
    }

    /** The original {@code ThaumcraftApiHelper.areItemStackTagsEqualForCrafting}. */
    public static boolean areItemStackTagsEqualForCrafting(ItemStack slotItem, ItemStack recipeItem) {
        if (slotItem == null || recipeItem == null) return false;
        CompoundTag recipeTag = tagOf(recipeItem);
        CompoundTag slotTag = tagOf(slotItem);
        if (recipeTag != null && slotTag == null) return false;
        if (recipeTag == null) return true;
        for (String key : recipeTag.getAllKeys()) {
            if (slotTag.contains(key)) {
                if (slotTag.get(key).toString().equals(recipeTag.get(key).toString())) continue;
                return false;
            }
            return false;
        }
        return true;
    }

    /** The custom data of a stack, or null when it carries none. */
    private static CompoundTag tagOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    /**
     * Original {@code areItemStacksEqual(ItemStack, ItemStack, boolean, boolean, boolean)}.
     *
     * <p>The original's metadata comparison became the damage value comparison. The wildcard metadata of the
     * original ({@code Short.MAX_VALUE}) has no native form and is therefore gone.
     */
    public static boolean areItemStacksEqual(ItemStack stack0, ItemStack stack1, boolean useOre, boolean ignoreDamage, boolean ignoreNBT) {
        if (stack0 == null || stack1 == null) return false;
        if (stack0.isEmpty() && stack1.isEmpty()) return true;
        if (useOre && matchesOre(stack0, stack1)) return true;
        boolean t1 = true;
        if (!ignoreNBT) t1 = ItemStack.isSameItemSameComponents(stack0, stack1);
        boolean t2 = stack0.getDamageValue() != stack1.getDamageValue();
        if (ignoreDamage && stack0.isDamageableItem() && stack1.isDamageableItem()) t2 = false;
        if (t2) return false;
        return stack0.getItem() == stack1.getItem() && t1;
    }

    // ------------------------------------------------------------------- the player

    /** Original {@code consumeInventoryItem(EntityPlayer, Item, int)}. */
    public static boolean consumeInventoryItem(Player player, Item item, int md) {
        if (player == null || item == null) return false;
        for (int slot = 0; slot < player.getInventory().items.size(); ++slot) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (stack == null || stack.isEmpty() || stack.getItem() != item || stack.getDamageValue() != md) continue;
            stack.setCount(stack.getCount() - 1);
            if (stack.getCount() <= 0) player.getInventory().setItem(slot, ItemStack.EMPTY);
            return true;
        }
        return false;
    }

    /** Original {@code isPlayerCarrying(EntityPlayer, ItemStack)}. */
    public static int isPlayerCarrying(Player player, ItemStack stack) {
        if (player == null || stack == null) return -1;
        for (int slot = 0; slot < player.getInventory().items.size(); ++slot) {
            ItemStack carried = player.getInventory().items.get(slot);
            if (carried == null || carried.isEmpty() || !ItemStack.isSameItem(carried, stack)) continue;
            return slot;
        }
        return -1;
    }

    /**
     * Original {@code isWandInHotbarWithRoom(Aspect, int, EntityPlayer)}.
     *
     * <p>The original returned the hotbar slot of the first wand that could not hold the whole amount, or -1.
     */
    public static int isWandInHotbarWithRoom(Aspect aspect, int amount, Player player) {
        if (player == null || aspect == null) return -1;
        for (int slot = 0; slot < player.getInventory().items.size(); ++slot) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemWandCasting)) continue;
            if (ItemWandCasting.addRealVis(stack, aspect.getTag(), amount, false) > 0) return slot;
        }
        return -1;
    }

    // ----------------------------------------------------------------------- drops

    /**
     * Original {@code dropItems(World, int, int, int)}.
     *
     * <p>The original scattered the contents of the block's inventory and emptied it. The native delivery of the
     * scattered items is {@link DeferredItemDrops}, which keeps the original's own random offsets.
     */
    public static void dropItems(Level level, int x, int y, int z) {
        if (level == null) return;
        BlockPos pos = new BlockPos(x, y, z);
        BlockEntity tile = level.getBlockEntity(pos);
        if (!(tile instanceof Container container)) return;
        if (!(level instanceof ServerLevel server)) return;
        List<ItemStack> drops = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); ++slot) {
            ItemStack item = container.getItem(slot);
            if (item == null || item.isEmpty() || item.getCount() <= 0) continue;
            drops.add(item.copy());
            container.setItem(slot, ItemStack.EMPTY);
        }
        if (drops.isEmpty()) return;
        DeferredItemDrops.enqueue(server, pos, drops);
        container.setChanged();
    }

    /** Original {@code dropItemsAtEntity(World, int, int, int, Entity)}. */
    public static void dropItemsAtEntity(Level level, int x, int y, int z, Entity entity) {
        if (level == null || entity == null) return;
        BlockPos pos = new BlockPos(x, y, z);
        BlockEntity tile = level.getBlockEntity(pos);
        if (!(tile instanceof Container container)) return;
        for (int slot = 0; slot < container.getContainerSize(); ++slot) {
            ItemStack item = container.getItem(slot);
            if (item == null || item.isEmpty() || item.getCount() <= 0) continue;
            if (level instanceof ServerLevel server) {
                ItemEntity dropped = new ItemEntity(server, entity.getX(), entity.getY() + entity.getEyeHeight() / 2.0f, entity.getZ(), item.copy());
                server.addFreshEntity(dropped);
            }
            container.setItem(slot, ItemStack.EMPTY);
        }
        container.setChanged();
    }

    /** Original {@code dropItemsWithChance(World, int, int, int, float, int, ArrayList<ItemStack>)}. */
    public static void dropItemsWithChance(Level level, int x, int y, int z, float chance, int fortune, List<ItemStack> items) {
        if (level == null || items == null || items.isEmpty()) return;
        if (level.isClientSide) return;
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)) return;
        BlockPos pos = new BlockPos(x, y, z);
        for (ItemStack item : items) {
            if (item == null || item.isEmpty() || item.getCount() <= 0) continue;
            if (!(level.getRandom().nextFloat() <= chance)) continue;
            if (level instanceof ServerLevel server) DeferredItemDrops.enqueueBlockHarvest(server, pos, List.of(item.copy()));
        }
    }

    // -------------------------------------------------------------------- durability

    /**
     * Original {@code damageItem(int, ItemStack, World)}.
     *
     * <p>The original applied the unbreaking reduction itself and then broke the item when the damage passed the
     * maximum, lowering the stack size by one. The native enchantment data of the stack is read directly, so the
     * reduction does not need a world.
     */
    public static ItemStack damageItem(int amount, ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) return stack;
        if (amount > 0) {
            int unbreaking = unbreakingLevel(level, stack);
            int avoided = 0;
            for (int index = 0; unbreaking > 0 && index < amount; ++index) {
                if (!breakItem(stack, unbreaking, level == null ? RandomSource.create() : level.getRandom())) continue;
                ++avoided;
            }
            if ((amount -= avoided) <= 0) return stack;
        }
        stack.setDamageValue(stack.getDamageValue() + amount);
        if (stack.getDamageValue() > stack.getMaxDamage()) {
            stack.setCount(stack.getCount() - 1);
            if (stack.getCount() < 0) stack.setCount(0);
            stack.setDamageValue(0);
        }
        return stack;
    }

    /** The original {@code EnchantmentDurability.breakItem}: the unbreaking reduction of one damage point. */
    private static boolean breakItem(ItemStack stack, int unbreaking, RandomSource random) {
        if (unbreaking <= 0) return false;
        if (random.nextInt(unbreaking + 1) > 0) return false;
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        int step = 1 + random.nextInt(Math.max(0, remaining) + 1);
        stack.setDamageValue(stack.getDamageValue() + step);
        return true;
    }

    /** The unbreaking level of a stack, read from its native enchantment data. */
    public static int unbreakingLevel(Level level, ItemStack stack) {
        if (stack == null || stack.isEmpty() || level == null) return 0;
        var enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) return 0;
        // A registry or an entry that the runtime does not know means "no
        // unbreaking", never a crash in the durability path.
        return level.registryAccess().lookup(Registries.ENCHANTMENT)
                .flatMap(lookup -> lookup.get(Enchantments.UNBREAKING))
                .map(unbreaking -> enchantments.getLevel(unbreaking))
                .orElse(0);
    }

    // --------------------------------------------------------------------- chests

    /** Original {@code getDoubleChest(TileEntity)}. */
    public static ChestBlockEntity getDoubleChest(BlockEntity tile) {
        if (tile == null || !(tile instanceof ChestBlockEntity chest)) return null;
        Level level = tile.getLevel();
        if (level == null) return null;
        BlockPos pos = tile.getBlockPos();
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) return null;
        Direction connected = ChestBlock.getConnectedDirection(state);
        if (connected == null) return null;
        if (level.getBlockEntity(pos.relative(connected)) instanceof ChestBlockEntity other) return other;
        return null;
    }

    // ------------------------------------------------------------------ cycling

    /**
     * Original {@code cycleItemStack(Object)}.
     *
     * <p>The original cycled the metadata of an item with sub types or a damage value, or walked the list of an
     * ore dictionary name. 1.21.1 has no metadata, so only the damage value and the list walking survive.
     */
    public static ItemStack cycleItemStack(Object input) {
        if (input instanceof ItemStack stack) {
            if (stack.isEmpty()) return null;
            if (stack.isDamageableItem()) {
                int damage = (int) (System.currentTimeMillis() / 10L % Math.max(1, stack.getMaxDamage()));
                ItemStack cycled = new ItemStack(stack.getItem(), 1);
                cycled.setDamageValue(damage);
                CompoundTag tag = tagOf(stack);
                if (tag != null) cycled.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                return cycled;
            }
            return stack;
        }
        if (input instanceof List<?> list) {
            if (list.isEmpty()) return null;
            int index = (int) (System.currentTimeMillis() / 1000L % list.size());
            return cycleItemStack(list.get(index));
        }
        if (input instanceof String oreName) {
            List<ItemStack> ores = oresOf(oreName);
            if (ores.isEmpty()) return null;
            int index = (int) (System.currentTimeMillis() / 1000L % ores.size());
            return cycleItemStack(ores.get(index));
        }
        return null;
    }

    /** The native side number of an original {@code ForgeDirection} ordinal. */
    private static Direction directionOf(int side) {
        if (side < 0 || side >= Direction.values().length) return null;
        return Direction.values()[side];
    }
}
