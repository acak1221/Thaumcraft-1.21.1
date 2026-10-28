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
package thaumic.tinkerer.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.block.tile.TileAspectAnalyzer;
import thaumic.tinkerer.common.block.tile.container.ContainerAspectAnalyzer;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

public class BlockAspectAnalyzer extends BaseEntityBlock implements ITTinkererRegisterable {

    public static final MapCodec<BlockAspectAnalyzer> CODEC = simpleCodec(BlockAspectAnalyzer::new);

    public BlockAspectAnalyzer(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof TileAspectAnalyzer analyzer) {
                player.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable(getDescriptionId());
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                        return new ContainerAspectAnalyzer(id, inventory, analyzer);
                    }
                });
            }
        }
        // 1.7.10 returned true on both sides, also for a player that holds an item.
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = useWithoutItem(state, level, pos, player, hit);
        return result.consumesAction() ? net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide)
                : net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof TileAspectAnalyzer analyzer) {
                MachineDrops.scatter(level, pos, analyzer);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileAspectAnalyzer(pos, state);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        TTResearchItem entry = (TTResearchItem) new TTResearchItem(LibResearch.KEY_ASPECT_ANALYZER,
                new AspectList().add(Aspect.MECHANISM, 2).add(Aspect.SENSES, 1).add(Aspect.MIND, 1), 0, 1, 2,
                new ItemStack(this)).setParents(LibResearch.KEY_PERIPHERALS).setParentsHidden("GOGGLES", "THAUMIUM")
                        .setConcealed().setRound().setPages(new ResearchPage("0"),
                                ResearchHelper.arcaneRecipePage(LibResearch.KEY_ASPECT_ANALYZER));
        return () -> {
            ensurePeripheralsEntry();
            entry.registerResearch();
        };
    }

    /**
     * The parent of the Aspectalyzer in the book. 1.7.10 registered this documentation entry in {@code ResearchHelper}
     * only when ComputerCraft was loaded; the Aspectalyzer is registered here without a computer mod, so the entry has
     * to exist for its child. A second registration of the key (by the lead's {@code ResearchHelper}) is ignored by
     * Thaumcraft.
     */
    static void ensurePeripheralsEntry() {
        if (ResearchCategories.getResearch(LibResearch.KEY_PERIPHERALS) != null) return;
        new TTResearchItem(LibResearch.KEY_PERIPHERALS, new AspectList(), -1, 0, 0, new ItemStack(Items.REDSTONE))
                .setAutoUnlock().setRound().setPages(new ResearchPage("0")).registerResearchItem();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererArcaneRecipe(LibResearch.KEY_ASPECT_ANALYZER, LibResearch.KEY_ASPECT_ANALYZER,
                new ItemStack(this), new AspectList().add(Aspect.ORDER, 1).add(Aspect.ENTROPY, 1), "TWT", "WMW", "TWT",
                'W', ItemApi.getBlock("blockWoodenDevice", 6), 'M', ItemApi.getItem("itemThaumometer", 0), 'T',
                ItemApi.getItem("itemResource", 2));
    }
}
