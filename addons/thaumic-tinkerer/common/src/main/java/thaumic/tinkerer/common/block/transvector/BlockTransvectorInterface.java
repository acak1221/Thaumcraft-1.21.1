/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [8 Sep 2013, 18:57:25 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.transvector;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.block.camo.CamoDeviceBlock;
import thaumic.tinkerer.common.block.tile.transvector.TileTransvectorInterface;
import thaumic.tinkerer.common.content.TileBlocks;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/** The Transvector Interface: an iron cube (hardness 3, resistance 10) that stands in for the block it is bound to. */
public class BlockTransvectorInterface extends CamoDeviceBlock implements ITTinkererRegisterable {
    public static final MapCodec<BlockTransvectorInterface> CODEC = simpleCodec(BlockTransvectorInterface::new);

    /** {@code setHardness(3F); setResistance(10F)}: a 1.7.10 resistance counts 0.6 of its argument, so 6 here. */
    public BlockTransvectorInterface() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3F, 6F).requiresCorrectToolForDrops().noOcclusion());
    }

    protected BlockTransvectorInterface(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileTransvectorInterface(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, TileBlocks.interfaceType(), TileTransvectorInterface::serverTick);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return (IRegisterableResearch) new TTResearchItem(
                LibResearch.KEY_INTERFACE,
                new AspectList().add(Aspect.ENTROPY, 4).add(Aspect.ORDER, 4),
                -4,
                2,
                1,
                new ItemStack(this))
                        .setParents(LibResearch.KEY_DARK_QUARTZ).setPages(
                                new ResearchPage("0"),
                                ResearchHelper.arcaneRecipePage(LibResearch.KEY_INTERFACE),
                                new ResearchPage("1"),
                                ResearchHelper.arcaneRecipePage(LibResearch.KEY_INTERFACE + "1"),
                                new ResearchPage("2"));
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererArcaneRecipe(
                LibResearch.KEY_INTERFACE,
                LibResearch.KEY_INTERFACE,
                new ItemStack(this),
                new AspectList().add(Aspect.ORDER, 12).add(Aspect.ENTROPY, 16),
                "BRB",
                "LEL",
                "BRB",
                'B',
                TileBlocks.legacyBlock("blockCosmeticSolid", 6),
                'E',
                new ItemStack(Items.ENDER_PEARL),
                'L',
                new ItemStack(Items.LAPIS_LAZULI),
                'R',
                new ItemStack(Items.REDSTONE));
    }
}
