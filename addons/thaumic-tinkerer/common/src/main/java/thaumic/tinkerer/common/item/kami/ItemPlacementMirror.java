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

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.item.kami.tool.ToolHandler;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Worldshaper's Looking Glass: sneak-click a block to remember it, sneak-click in the air to change the size of the
 * square (3, 5, 7, 9 or 11), click to place the square against the face you look at, taking the blocks from the
 * inventory and from Black Hole Rings. 1.7.10 stored {@code blockName}/{@code blockMeta}/{@code size} in the stack tag;
 * the block state and the size are components here.
 */
public class ItemPlacementMirror extends KamiItemBase {
    /** How far the player's look reaches for the square: 1.7.10 {@code raytraceFromEntity(..., 5)}. */
    public static final double REACH = 5;

    public ItemPlacementMirror(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    // ---- stored data ----------------------------------------------------------------------------------------

    private static void setSize(ItemStack stack, int size) {
        stack.set(KamiComponents.MIRROR_SIZE.get(), size | 1);
    }

    public static int getSize(ItemStack stack) {
        return stack.getOrDefault(KamiComponents.MIRROR_SIZE.get(), 3) | 1;
    }

    /** The state the mirror places, or null when it has none. */
    public static BlockState getBlockState(ItemStack stack) {
        return stack.get(KamiComponents.MIRROR_STATE.get());
    }

    public static Block getBlock(ItemStack stack) {
        BlockState state = getBlockState(stack);
        return state == null ? null : state.getBlock();
    }

    // ---- what is in the inventory ---------------------------------------------------------------------------

    /** 1.7.10 {@code hasBlocks}: creative players always have them; otherwise the inventory and the Black Hole Rings in it must hold {@code requiredAmount}. */
    public static boolean hasBlocks(Player player, Block requiredBlock, int requiredAmount) {
        if (player.getAbilities().instabuild) return true;
        Item requiredItem = requiredBlock.asItem();
        if (requiredItem == Items.AIR) return false;
        Item talismanItem = TTRegistry.item(LibItemNames.BLOCK_TALISMAN);

        int current = 0;
        List<ItemStack> talismansToCheck = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stackInSlot = inventory.getItem(i);
            if (stackInSlot.isEmpty()) continue;
            if (stackInSlot.is(requiredItem)) {
                current += stackInSlot.getCount();
                if (current >= requiredAmount) return true;
            } else if (stackInSlot.is(talismanItem) && ItemBlockTalisman.getBlock(stackInSlot) == requiredBlock) {
                talismansToCheck.add(stackInSlot);
            }
        }
        for (ItemStack talisman : talismansToCheck) {
            current += ItemBlockTalisman.getBlockCount(talisman);
            if (current >= requiredAmount) return true;
        }
        return false;
    }

    // ---- the square -----------------------------------------------------------------------------------------

    /**
     * The offsets of the placed square from the block that is aimed at, shifted by the aimed face (1.7.10
     * {@code getBlocksToPlace}). {@code rotation} is the player's facing as 0 to 3, {@code pitch} the look pitch in degrees.
     */
    public static List<int[]> squareOffsets(Direction side, float pitch, int rotation, int size) {
        int range = (size ^ 1) / 2;
        boolean topOrBottom = side == Direction.UP || side == Direction.DOWN;

        int xOff = !(side == Direction.WEST || side == Direction.EAST)
                ? topOrBottom ? pitch > 75 || (rotation & 1) == 0 ? range : 0 : range
                : 0;
        int yOff = topOrBottom ? pitch > 75 ? 0 : range : range;
        int zOff = !(side == Direction.SOUTH || side == Direction.NORTH)
                ? topOrBottom ? pitch > 75 || (rotation & 1) == 1 ? range : 0 : range
                : 0;

        List<int[]> offsets = new ArrayList<>();
        for (int x = -xOff; x < xOff + 1; x++) {
            for (int y = 0; y < yOff * 2 + 1; y++) {
                for (int z = -zOff; z < zOff + 1; z++) {
                    offsets.add(new int[] { x + side.getStepX(), y + side.getStepY(), z + side.getStepZ() });
                }
            }
        }
        return offsets;
    }

    /** The empty or replaceable positions of the square the player aims at; empty when the player aims at nothing. */
    public static List<BlockPos> getBlocksToPlace(ItemStack stack, Player player) {
        List<BlockPos> coords = new ArrayList<>();
        BlockHitResult hit = ToolHandler.raytrace(player, REACH);
        if (hit == null) return coords;
        Level level = player.level();
        BlockPos target = hit.getBlockPos();
        if (level.getBlockState(target).canBeReplaced()) target = target.below();

        int rotation = Mth.floor(player.getYRot() * 4F / 360F + 0.5D) & 3;
        for (int[] offset : squareOffsets(hit.getDirection(), player.getXRot(), rotation, getSize(stack))) {
            BlockPos pos = target.offset(offset[0], offset[1], offset[2]);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.canBeReplaced()) coords.add(pos);
        }
        return coords;
    }

    // ---- use ------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null) return InteractionResult.PASS;
        BlockState state = level.getBlockState(context.getClickedPos());

        if (player.isShiftKeyDown()) {
            if (state.getRenderShape() == RenderShape.MODEL && state.getBlock().asItem() != Items.AIR) {
                stack.set(KamiComponents.MIRROR_STATE.get(), state);
            }
        } else if (!level.isClientSide) {
            placeAllBlocks(stack, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            int size = getSize(stack);
            int newSize = size == 11 ? 3 : size + 2;
            setSize(stack, newSize);
            KamiHooks.showTooltip(level, Component.literal(newSize + " x " + newSize));
            KamiHooks.playToggleSound(level, player);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    public void placeAllBlocks(ItemStack stack, Player player) {
        BlockState required = getBlockState(stack);
        if (required == null) return;
        List<BlockPos> blocksToPlace = getBlocksToPlace(stack, player);
        if (!hasBlocks(player, required.getBlock(), blocksToPlace.size())) return;

        Level level = player.level();
        for (BlockPos pos : blocksToPlace) {
            // The original placed wherever the square reached; the vanilla rules of who may build where are kept.
            if (level.isOutsideBuildHeight(pos) || !level.mayInteract(player, pos) || !player.mayBuild()) continue;
            placeBlockAndConsume(player, required, pos);
        }
        KamiHooks.playThaumcraftSound(level, player, "wand", 1F, 1F);
    }

    private void placeBlockAndConsume(Player player, BlockState state, BlockPos pos) {
        player.level().setBlock(pos, state, Block.UPDATE_ALL);
        if (player.getAbilities().instabuild) return;

        Item item = state.getBlock().asItem();
        Item talismanItem = TTRegistry.item(LibItemNames.BLOCK_TALISMAN);
        List<ItemStack> talismansToCheck = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stackInSlot = inventory.getItem(i);
            if (!stackInSlot.isEmpty() && stackInSlot.is(item)) {
                stackInSlot.shrink(1);
                return;
            }
            if (!stackInSlot.isEmpty() && stackInSlot.is(talismanItem)) talismansToCheck.add(stackInSlot);
        }
        for (ItemStack talisman : talismansToCheck) {
            if (ItemBlockTalisman.getBlock(talisman) == state.getBlock()) {
                ItemBlockTalisman.remove(talisman, 1);
                return;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int size = getSize(stack);
        tooltip.add(Component.literal(size + " x " + size).withStyle(ChatFormatting.GRAY));
        Block block = getBlock(stack);
        if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
            tooltip.add(Component.translatable(block.getDescriptionId()).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_PLACEMENT_MIRROR, new ItemStack(this), 12,
                new AspectList().add(Aspect.CRAFT, 65).add(Aspect.CRYSTAL, 32).add(Aspect.MAGIC, 50)
                        .add(Aspect.MIND, 32),
                TTRegistry.stack(LibItemNames.BLOCK_TALISMAN), ItemKamiResource.stack(ItemKamiResource.ICHOR),
                new ItemStack(Items.DROPPER), new ItemStack(Items.DIAMOND), new ItemStack(Items.GLASS),
                new ItemStack(Items.BLAZE_POWDER), ItemKamiResource.stack(ItemKamiResource.ICHOR));
    }

    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_PLACEMENT_MIRROR,
                new AspectList().add(Aspect.CRAFT, 2).add(Aspect.CRYSTAL, 1).add(Aspect.ELDRITCH, 1)
                        .add(Aspect.MIND, 1),
                17, 16, TTRegistry.stack(LibItemNames.PLACEMENT_MIRROR), 0,
                KamiResearch.of(LibResearch.KEY_BLOCK_TALISMAN), KamiResearch.NONE, new ResearchPage("0"),
                ResearchHelper.infusionPage(LibResearch.KEY_PLACEMENT_MIRROR));
    }
}
