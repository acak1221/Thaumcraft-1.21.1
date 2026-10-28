/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Nov 24, 2013, 10:08:39 PM (GMT)]
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
import thaumic.tinkerer.common.block.camo.PulseDeviceBlock;
import thaumic.tinkerer.common.block.tile.transvector.TileTransvectorDislocator;
import thaumic.tinkerer.common.content.TileBlocks;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/** The Transvector Dislocator: swaps the block in front of it with the block it is bound to when it gets a redstone pulse. */
public class BlockTransvectorDislocator extends PulseDeviceBlock implements ITTinkererRegisterable {
    public static final MapCodec<BlockTransvectorDislocator> CODEC = simpleCodec(BlockTransvectorDislocator::new);

    public BlockTransvectorDislocator() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3F, 10F).noOcclusion());
    }

    protected BlockTransvectorDislocator(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileTransvectorDislocator(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, TileBlocks.dislocatorType(), TileTransvectorDislocator::serverTick);
    }

    /** Thaumcraft's {@code Config.allowMirrors}: without mirrors the dislocator has neither research nor recipe. */
    @Override
    public IRegisterableResearch getResearchItem() {
        if (!TileBlocks.mirrorsAllowed()) {
            return null;
        }

        return (IRegisterableResearch) new TTResearchItem(
                LibResearch.KEY_DISLOCATOR,
                new AspectList().add(Aspect.TRAVEL, 2).add(Aspect.MECHANISM, 1).add(Aspect.ELDRITCH, 1),
                -6,
                1,
                3,
                new ItemStack(this)).setConcealed().setParents(LibResearch.KEY_INTERFACE).setParentsHidden("MIRROR")
                        .setPages(new ResearchPage("0"), ResearchHelper.arcaneRecipePage(LibResearch.KEY_DISLOCATOR))
                        .setSecondary();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        if (!TileBlocks.mirrorsAllowed()) {
            return null;
        }
        return new ThaumicTinkererArcaneRecipe(
                LibResearch.KEY_DISLOCATOR,
                LibResearch.KEY_DISLOCATOR,
                new ItemStack(this),
                new AspectList().add(Aspect.EARTH, 5).add(Aspect.ENTROPY, 5),
                " M ",
                " I ",
                " C ",
                'M',
                TileBlocks.legacyItem("itemResource", 10),
                'I',
                new ItemStack(TileBlocks.interfaceBlock()),
                'C',
                new ItemStack(Items.COMPARATOR));
    }
}
