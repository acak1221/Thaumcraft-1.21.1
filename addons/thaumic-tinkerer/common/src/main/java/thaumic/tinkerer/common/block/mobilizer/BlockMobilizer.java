package thaumic.tinkerer.common.block.mobilizer;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.block.tile.TileEntityMobilizer;
import thaumic.tinkerer.common.content.TileBlocks;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Levitational Locomotive: an iron cube with a block entity that slides along the beam of two relays. Like every
 * block of the original that never set a hardness it breaks at once.
 */
public class BlockMobilizer extends BaseEntityBlock implements ITTinkererRegisterable {
    public static final MapCodec<BlockMobilizer> CODEC = simpleCodec(BlockMobilizer::new);

    public BlockMobilizer() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).instabreak());
    }

    protected BlockMobilizer(Properties properties) {
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMobilizer(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TileBlocks.mobilizerType(), TileEntityMobilizer::serverTick);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return (IRegisterableResearch) new TTResearchItem(
                LibResearch.KEY_MOBILIZER,
                new AspectList().add(Aspect.MOTION, 2).add(Aspect.ORDER, 2),
                -7,
                5,
                3,
                new ItemStack(this))
                        .setParents(LibResearch.KEY_MAGNETS)
                        .setPages(
                                new ResearchPage("0"),
                                ResearchHelper.infusionPage(LibResearch.KEY_MOBILIZER),
                                ResearchHelper.arcaneRecipePage(LibResearch.KEY_RELAY))
                        .setSecondary();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(
                LibResearch.KEY_MOBILIZER,
                new ItemStack(this),
                4,
                new AspectList().add(Aspect.MOTION, 15).add(Aspect.ORDER, 20).add(Aspect.MAGIC, 15),
                TileBlocks.legacyBlock("blockLifter", 0),
                new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.FEATHER),
                new ItemStack(Items.IRON_INGOT),
                TileBlocks.legacyBlock("blockCosmeticSolid", 1));
    }
}
