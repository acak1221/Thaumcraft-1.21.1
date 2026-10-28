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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.block.tile.TileSummon;
import thaumic.tinkerer.common.content.Machines;
import thaumic.tinkerer.common.item.ItemMobAspect;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/** The Tablet of Necromancy (registry path {@code spawner}, 1.7.10 name {@code spawner}). */
public class BlockSummon extends BaseEntityBlock implements ITTinkererRegisterable {

    public static final MapCodec<BlockSummon> CODEC = simpleCodec(BlockSummon::new);

    /** {@code setBlockBounds(0F, 0F, 0F, 1F, 1F / 16F * 2F, 1F)}. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public BlockSummon(Properties properties) {
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileSummon(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Machines.TILE_SUMMON.get(), TileSummon::serverTick);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return (IRegisterableResearch) new TTResearchItem(LibResearch.KEY_SUMMON,
                new AspectList().add(Aspect.WEAPON, 1).add(Aspect.BEAST, 3).add(Aspect.MAGIC, 3), -5, 8, 3,
                new ItemStack(this)).setWarp(3).setParents(LibResearch.KEY_BLOOD_SWORD).setPages(
                        new ResearchPage("0"), ResearchHelper.arcaneRecipePage(LibResearch.KEY_SUMMON + "0"),
                        ResearchHelper.recipePage(ItemMobAspect.CONDENSE_RECIPE_NAME),
                        ResearchHelper.infusionPage(LibResearch.KEY_SUMMON), new ResearchPage("1"));
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererArcaneRecipe(LibResearch.KEY_SUMMON + "0", LibResearch.KEY_SUMMON,
                new ItemStack(this), new AspectList().add(Aspect.ORDER, 50).add(Aspect.ENTROPY, 50), "WWW", "SSS", 'S',
                new ItemStack(Items.STONE), 'W', ItemApi.getBlock("blockCosmeticSolid", 1));
    }
}
