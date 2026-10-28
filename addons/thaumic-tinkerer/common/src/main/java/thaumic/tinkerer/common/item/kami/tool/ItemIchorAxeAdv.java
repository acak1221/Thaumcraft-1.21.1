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

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.item.kami.ItemKamiResource;
import thaumic.tinkerer.common.item.kami.KamiHooks;
import thaumic.tinkerer.common.item.kami.KamiResearch;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/** The Awakened Ichorium Axe: block mode, square mode (5 x 5) and tree mode; it also cuts plants and wool as fast as shears. */
public class ItemIchorAxeAdv extends ItemIchorAxe implements IAdvancedTool {

    public ItemIchorAxeAdv(Item.Properties properties) {
        super(properties);
    }

    /** 1.7.10 {@code func_150893_a}: the larger of the axe's and the shears' speed. */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return Math.max(super.getDestroySpeed(stack, state),
                Items.SHEARS.getDefaultInstance().getDestroySpeed(state));
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

    /** Sneaking is the mode switch; the stripping of logs of 1.21.1 (not in 1.7.10) must not take the click first. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) return InteractionResult.PASS;
        return super.useOn(context);
    }

    @Override
    public void onBlockStartBreak(ItemStack stack, ServerLevel level, BlockPos pos, BlockState state,
            ServerPlayer player) {
        if (!ToolHandler.MATERIALS_AXE.test(state)) return;
        BlockHitResult hit = ToolHandler.raytrace(player);
        if (hit == null) return;
        Direction direction = hit.getDirection();

        switch (ToolHandler.getMode(stack)) {
            case 1 -> {
                boolean doX = direction.getStepX() == 0;
                boolean doY = direction.getStepY() == 0;
                boolean doZ = direction.getStepZ() == 0;
                ToolHandler.removeBlocksInIteration(player, level, pos, doX ? -2 : 0, doY ? -1 : 0, doZ ? -2 : 0,
                        doX ? 3 : 1, doY ? 4 : 1, doZ ? 3 : 1, null, ToolHandler.MATERIALS_AXE);
            }
            case 2 -> ToolHandler.fellTree(level, pos, player);
            default -> {}
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(ToolHandler.getToolModeStr(this, stack).copy().withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public String getType() {
        return "axe";
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_ICHOR_AXE_GEM, new ItemStack(this), 15,
                new AspectList().add(Aspect.WATER, 50).add(Aspect.MINE, 64).add(Aspect.TOOL, 32).add(Aspect.TREE, 32)
                        .add(Aspect.HARVEST, 32).add(Aspect.CROP, 16).add(Aspect.SENSES, 16),
                TTRegistry.stack(LibItemNames.ICHOR_AXE), ItemKamiResource.stack(ItemKamiResource.ICHORIUM),
                ItemKamiResource.stack(ItemKamiResource.ICHOR), KamiResearch.tc("itemAxeElemental", 0),
                KamiResearch.tc("itemFocusExcavation", 0), new ItemStack(Items.TNT), KamiResearch.tc("itemNugget", 21),
                KamiResearch.tc("itemNugget", 16), KamiResearch.tc("itemNugget", 31), new ItemStack(Items.DIAMOND),
                KamiResearch.tc("itemFocusExcavation", 0), KamiResearch.tc("itemAxeElemental", 0),
                ItemKamiResource.stack(ItemKamiResource.ICHOR_CLOTH));
    }

    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_ICHOR_AXE_GEM,
                new AspectList().add(Aspect.WATER, 2).add(Aspect.TOOL, 1).add(Aspect.TREE, 1).add(Aspect.CROP, 1), 16,
                14, TTRegistry.stack(LibItemNames.ICHOR_AXE_GEM), 0, KamiResearch.of(LibResearch.KEY_ICHOR_TOOLS),
                KamiResearch.NONE, new ResearchPage("0"), ResearchHelper.infusionPage(LibResearch.KEY_ICHOR_AXE_GEM));
    }
}
