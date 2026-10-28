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
package thaumic.tinkerer.common.item.kami.tool;

import dev.architectury.event.EventResult;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.item.kami.ItemKamiResource;
import thaumic.tinkerer.common.item.kami.KamiHooks;
import thaumic.tinkerer.common.item.kami.KamiResearch;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Awakened Ichorium Pickaxe: block mode, square mode (5 x 5 around the block, offset upward) and line mode (ten
 * blocks into the wall). It also breaks bedrock: sneaking on bedrock near the bottom of the Overworld or the top of the
 * Bedrock dimension makes a portal, inside the Bedrock dimension it removes the bedrock.
 */
public class ItemIchorPickAdv extends ItemIchorPick implements IAdvancedTool {

    public ItemIchorPickAdv(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            ToolHandler.changeMode(stack);
            KamiHooks.showTooltip(level, ToolHandler.getToolModeStr(this, stack));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    /** 1.7.10 {@code func_150893_a}/{@code getDigSpeed}: bedrock is mined at once. */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return state.is(Blocks.BEDROCK) ? Float.MAX_VALUE : super.getDestroySpeed(stack, state);
    }

    /** 1.7.10 {@code func_150897_b}: bedrock can be harvested. */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return state.is(Blocks.BEDROCK) || super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public void onBlockStartBreak(ItemStack stack, ServerLevel level, BlockPos pos, BlockState state,
            ServerPlayer player) {
        if (!ToolHandler.MATERIALS_PICK.test(state)) return;
        BlockHitResult hit = ToolHandler.raytrace(player);
        if (hit == null) return;
        Direction direction = hit.getDirection();

        switch (ToolHandler.getMode(stack)) {
            case 1 -> {
                boolean doX = direction.getStepX() == 0;
                boolean doY = direction.getStepY() == 0;
                boolean doZ = direction.getStepZ() == 0;
                if (!player.isShiftKeyDown()) {
                    ToolHandler.removeBlocksInIteration(player, level, pos, doX ? -2 : 0, doY ? -1 : 0, doZ ? -2 : 0,
                            doX ? 3 : 1, doY ? 4 : 1, doZ ? 3 : 1, null, ToolHandler.MATERIALS_PICK);
                }
            }
            case 2 -> {
                int xo = -direction.getStepX();
                int yo = -direction.getStepY();
                int zo = -direction.getStepZ();
                if (!player.isShiftKeyDown()) {
                    ToolHandler.removeBlocksInIteration(player, level, pos, xo >= 0 ? 0 : -10, yo >= 0 ? 0 : -10,
                            zo >= 0 ? 0 : -10, xo > 0 ? 10 : 1, yo > 0 ? 10 : 1, zo > 0 ? 10 : 1, null,
                            ToolHandler.MATERIALS_PICK);
                }
            }
            default -> {}
        }
    }

    /**
     * Bedrock to portal or air (the sneaking branch of 1.7.10's {@code onBlockStartBreak}). Bedrock cannot be mined in
     * survival (hardness -1), so the conversion runs when the block is clicked and the click is then cancelled; the
     * original ran it in the break and let the break go on, which in survival never started.
     *
     * @return true when the bedrock was converted
     */
    public static boolean convertBedrock(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!player.isShiftKeyDown() || !KamiHooks.bedrockDimensionAvailable()) return false;
        if (!level.getBlockState(pos).is(Blocks.BEDROCK)) return false;
        boolean bedrockDimension = level.dimension() == KamiHooks.BEDROCK_DIMENSION;
        boolean top = pos.getY() > level.getMaxBuildHeight() - 3;
        boolean overworldFloor = level.dimension() == Level.OVERWORLD && pos.getY() < level.getMinBuildHeight() + 5;
        Block portal = KamiHooks.optionalBlock(LibBlockNames.PORTAL);
        if (portal != null && (overworldFloor || (bedrockDimension && top))) {
            level.setBlock(pos, portal.defaultBlockState(), Block.UPDATE_ALL);
            return true;
        }
        if (bedrockDimension && !top) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    /** Architectury {@code InteractionEvent.LEFT_CLICK_BLOCK}: sneak-clicking bedrock with this pickaxe. */
    public static EventResult onLeftClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer server)) return EventResult.pass();
        if (!(server.getMainHandItem().getItem() instanceof ItemIchorPickAdv)) return EventResult.pass();
        return convertBedrock(server, server.serverLevel(), pos) ? EventResult.interruptFalse() : EventResult.pass();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(ToolHandler.getToolModeStr(this, stack).copy().withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public String getType() {
        return "pick";
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_ICHOR_PICK_GEM, new ItemStack(this), 15,
                new AspectList().add(Aspect.FIRE, 50).add(Aspect.MINE, 64).add(Aspect.METAL, 32).add(Aspect.EARTH, 32)
                        .add(Aspect.HARVEST, 32).add(Aspect.GREED, 16).add(Aspect.SENSES, 16),
                TTRegistry.stack(LibItemNames.ICHOR_PICK), ItemKamiResource.stack(ItemKamiResource.ICHORIUM),
                ItemKamiResource.stack(ItemKamiResource.ICHOR), KamiResearch.tc("itemPickElemental", 0),
                KamiResearch.tc("itemFocusExcavation", 0), new ItemStack(Items.TNT), KamiResearch.tc("itemNugget", 21),
                KamiResearch.tc("itemNugget", 16), KamiResearch.tc("itemNugget", 31), new ItemStack(Items.DIAMOND),
                KamiResearch.tc("itemFocusExcavation", 0), KamiResearch.tc("itemPickElemental", 0),
                ItemKamiResource.stack(ItemKamiResource.ICHOR_CLOTH));
    }

    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_ICHOR_PICK_GEM,
                new AspectList().add(Aspect.FIRE, 2).add(Aspect.TOOL, 1).add(Aspect.MINE, 1).add(Aspect.EARTH, 1), 13,
                15, TTRegistry.stack(LibItemNames.ICHOR_PICK_GEM), 0, KamiResearch.of(LibResearch.KEY_ICHOR_TOOLS),
                KamiResearch.NONE, new ResearchPage("0"), ResearchHelper.infusionPage(LibResearch.KEY_ICHOR_PICK_GEM),
                new ResearchPage("1"));
    }
}
