package thaumic.tinkerer.common.block.mobilizer;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.common.block.tile.TileEntityRelay;
import thaumic.tinkerer.common.content.TileBlocks;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;

/**
 * The Levitational Locomotive Relay: an iron cube that finds a partner relay in a straight line and projects a beam to
 * it. It has no research of its own (its recipe is part of the Levitational Locomotive's research).
 */
public class BlockMobilizerRelay extends BaseEntityBlock implements ITTinkererRegisterable {
    public static final MapCodec<BlockMobilizerRelay> CODEC = simpleCodec(BlockMobilizerRelay::new);

    public BlockMobilizerRelay() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).instabreak());
    }

    protected BlockMobilizerRelay(Properties properties) {
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
        return new TileEntityRelay(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TileBlocks.relayType(), TileEntityRelay::tick);
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererArcaneRecipe(
                LibResearch.KEY_RELAY,
                LibResearch.KEY_MOBILIZER,
                new ItemStack(this),
                new AspectList().add(Aspect.AIR, 20).add(Aspect.ORDER, 5).add(Aspect.EARTH, 15),
                "WFW",
                "SIs",
                "WFW",
                'I',
                new ItemStack(Items.IRON_INGOT),
                's',
                TileBlocks.legacyItem("itemShard", 3),
                'S',
                TileBlocks.legacyItem("itemShard", 0),
                'W',
                TileBlocks.legacyBlock("blockMagicalLog", 0),
                'F',
                new ItemStack(Blocks.GLASS));
    }
}
