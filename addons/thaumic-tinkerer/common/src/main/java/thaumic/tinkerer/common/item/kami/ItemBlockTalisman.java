/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.kami;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.item.kami.KamiComponents.TalismanStore;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Black Hole Ring: condenses one block type. 1.7.10 kept {@code blockName}/{@code blockMeta}/{@code blockCount} in the
 * stack tag and the active flag in the damage value; here they are the {@link KamiComponents#TALISMAN_STORE} and
 * {@link KamiComponents#TALISMAN_ACTIVE} components, and the block state replaces name plus metadata.
 */
public class ItemBlockTalisman extends KamiItemBase implements IBauble {

    public ItemBlockTalisman(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    /** A use context for a stack that is not the one in the player's hand ({@code UseOnContext}'s constructor is protected). */
    static final class HeldStackContext extends UseOnContext {
        HeldStackContext(Level level, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
            super(level, player, hand, stack, hit);
        }
    }

    // ---- stored data ----------------------------------------------------------------------------------------

    /** The block the ring is bound to, {@link Blocks#AIR} when it is bound to none. */
    public static Block getBlock(ItemStack stack) {
        TalismanStore store = stack.get(KamiComponents.TALISMAN_STORE.get());
        if (store == null) return Blocks.AIR;
        return BuiltInRegistries.BLOCK.getOptional(store.block()).orElse(Blocks.AIR);
    }

    public static int getBlockCount(ItemStack stack) {
        TalismanStore store = stack.get(KamiComponents.TALISMAN_STORE.get());
        return store == null ? 0 : store.count();
    }

    public static boolean isActive(ItemStack stack) {
        return stack.getOrDefault(KamiComponents.TALISMAN_ACTIVE.get(), false);
    }

    private static void setStore(ItemStack stack, Block block, int count) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        stack.set(KamiComponents.TALISMAN_STORE.get(), new TalismanStore(id, Math.max(0, count)));
    }

    private static void setCount(ItemStack stack, int count) {
        TalismanStore store = stack.get(KamiComponents.TALISMAN_STORE.get());
        if (store != null) stack.set(KamiComponents.TALISMAN_STORE.get(), store.withCount(Math.max(0, count)));
    }

    /** Takes up to {@code count} blocks out of the ring and answers how many it took (1.7.10 {@code remove}). */
    public static int remove(ItemStack stack, int count) {
        int current = getBlockCount(stack);
        setCount(stack, Math.max(current - count, 0));
        return Math.min(current, count);
    }

    private static void add(ItemStack stack, int count) {
        int current = getBlockCount(stack);
        // 1.7.10 let the int overflow; a ring holds "over 2 billion" blocks, then it is full.
        setCount(stack, (int) Math.min((long) current + count, Integer.MAX_VALUE));
    }

    // ---- use ------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        BlockState clicked = level.getBlockState(pos);

        // setBlock: an empty ring takes the type of the block it is clicked on.
        if (getBlock(stack) == Blocks.AIR || getBlockCount(stack) == 0) {
            if (clicked.getBlock().asItem() == Items.AIR) return InteractionResult.PASS;
            setStore(stack, clicked.getBlock(), 0);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockEntity tile = level.getBlockEntity(pos);
        if (level.isClientSide) {
            // The server decides; a ring with blocks always acts on a block that takes them (a container) or places one.
            return InteractionResult.sidedSuccess(true);
        }

        Block block = getBlock(stack);
        Item blockItem = block.asItem();
        if (tile instanceof Container container) {
            boolean set = insertInto(stack, container, blockItem, context.getClickedFace());
            return set ? InteractionResult.sidedSuccess(false) : InteractionResult.PASS;
        }

        // Place one block as if the player held it; a failed placement keeps the block in the ring.
        ItemStack single = new ItemStack(blockItem);
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), pos,
                context.isInside());
        InteractionResult placed = blockItem.useOn(new HeldStackContext(level, player, context.getHand(), single, hit));
        if (placed.consumesAction()) {
            remove(stack, 1);
            return InteractionResult.sidedSuccess(false);
        }
        return InteractionResult.FAIL;
    }

    private static boolean insertInto(ItemStack ring, Container container, Item blockItem, Direction face) {
        boolean sided = container instanceof WorldlyContainer;
        int[] slots = sided ? ((WorldlyContainer) container).getSlotsForFace(face) : linearSlots(container);
        boolean set = false;
        for (int slot : slots) {
            ItemStack inSlot = container.getItem(slot);
            if (inSlot.isEmpty()) {
                int amount = Math.min(blockItem.getDefaultMaxStackSize(), getBlockCount(ring));
                if (amount <= 0) continue;
                ItemStack candidate = new ItemStack(blockItem, amount);
                // 1.7.10 took the blocks out of the ring before asking whether the slot takes them and lost them
                // when it did not; here nothing leaves the ring unless the slot accepts the stack.
                if (container.canPlaceItem(slot, candidate)
                        && (!sided || ((WorldlyContainer) container).canPlaceItemThroughFace(slot, candidate, face))) {
                    remove(ring, amount);
                    container.setItem(slot, candidate);
                    container.setChanged();
                    set = true;
                }
            } else if (ItemStack.isSameItemSameComponents(inSlot, new ItemStack(blockItem))) {
                int missing = inSlot.getMaxStackSize() - inSlot.getCount();
                if (container.canPlaceItem(slot, inSlot)
                        && (!sided || ((WorldlyContainer) container).canPlaceItemThroughFace(slot, inSlot, face))) {
                    inSlot.grow(remove(ring, Math.max(missing, 0)));
                    container.setChanged();
                    set = true;
                }
            }
        }
        return set;
    }

    private static int[] linearSlots(Container container) {
        int[] slots = new int[container.getContainerSize()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getBlock(stack) != Blocks.AIR && player.isShiftKeyDown()) {
            stack.set(KamiComponents.TALISMAN_ACTIVE.get(), !isActive(stack));
            KamiHooks.playToggleSound(level, player);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Block block = getBlock(stack);
        if (block != Blocks.AIR) {
            tooltip.add(Component.translatable(block.getDescriptionId())
                    .append(" (x" + getBlockCount(stack) + ")").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(isActive(stack) ? "ttmisc.active" : "ttmisc.inactive")
                .withStyle(ChatFormatting.GRAY));
    }

    // ---- bauble ---------------------------------------------------------------------------------------------

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.RING;
    }

    /** A ring that is sneak-clicked to toggle is not put on by the click ({@code IBauble.equipsOnUse}, API request A1). */
    @Override
    public boolean equipsOnUse(ItemStack stack, Player player) {
        return false;
    }

    @Override
    public void onWornTick(ItemStack stack, LivingEntity wearer) {
        Block block = getBlock(stack);
        if (wearer.level().isClientSide || !isActive(stack) || block == Blocks.AIR || !(wearer instanceof Player player)) {
            return;
        }
        Item item = block.asItem();
        if (item == Items.AIR) return;
        ItemStack plain = new ItemStack(item);
        Inventory inventory = player.getInventory();
        // 1.7.10: the slots of the inventory without the armour slots, which are the 36 main slots.
        int size = Inventory.INVENTORY_SIZE;

        int highest = -1;
        boolean hasFreeSlot = false;
        int[] counts = new int[size];
        for (int i = 0; i < size; i++) {
            ItemStack inSlot = inventory.getItem(i);
            if (inSlot.isEmpty()) {
                hasFreeSlot = true;
                continue;
            }
            if (ItemStack.isSameItemSameComponents(inSlot, plain)) {
                counts[i] = inSlot.getCount();
                if (highest == -1) highest = i;
                else highest = counts[i] > counts[highest] && highest > 8 ? i : highest;
            }
        }

        if (highest == -1) {
            ItemStack held = player.containerMenu.getCarried();
            if (hasFreeSlot && (held.isEmpty() || held.is(item) || held.getDamageValue() != 0)) {
                int amount = remove(stack, Math.min(64, item.getDefaultMaxStackSize()));
                if (amount != 0) inventory.add(new ItemStack(item, amount));
            }
        } else {
            for (int i = 0; i < size; i++) {
                int count = counts[i];
                if (i == highest || count == 0) continue;
                add(stack, count);
                inventory.setItem(i, ItemStack.EMPTY);
            }
            int countInHighest = counts[highest];
            int maxSize = item.getDefaultMaxStackSize();
            if (countInHighest < maxSize) {
                inventory.getItem(highest).grow(remove(stack, maxSize - countInHighest));
            }
        }
    }

    // ---- research and recipe --------------------------------------------------------------------------------

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_BLOCK_TALISMAN, new ItemStack(this), 9,
                new AspectList().add(Aspect.VOID, 65).add(Aspect.DARKNESS, 32).add(Aspect.MAGIC, 50)
                        .add(Aspect.ELDRITCH, 32),
                KamiResearch.tc("itemFocusPortableHole", 0), ItemKamiResource.stack(ItemKamiResource.ICHOR),
                new ItemStack(Items.ENDER_CHEST), new ItemStack(Items.DIAMOND),
                ItemKamiResource.stack(ItemKamiResource.ICHOR), KamiResearch.tc("itemResource", 11),
                KamiResearch.tcBlock("blockJar", 3));
    }

    /** The research entry; the registrar queues it for the last registration pass. */
    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_BLOCK_TALISMAN,
                new AspectList().add(Aspect.VOID, 2).add(Aspect.DARKNESS, 1).add(Aspect.ELDRITCH, 1)
                        .add(Aspect.MAGIC, 1),
                14, 17, TTRegistry.stack(LibItemNames.BLOCK_TALISMAN), 0,
                KamiResearch.of(LibResearch.KEY_ICHOR_PICK_GEM, LibResearch.KEY_ICHOR_SHOVEL_GEM), KamiResearch.NONE,
                new ResearchPage("0"), ResearchHelper.infusionPage(LibResearch.KEY_BLOCK_TALISMAN));
    }
}
