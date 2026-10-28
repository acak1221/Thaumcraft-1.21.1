/**
 * Created by Katrina on 16/04/2015.
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

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererCraftingBenchRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipeMulti;

/**
 * Paving Stair of Travel. 1.7.10 built the stairs over the Paving Stone of Travel of Thaumcraft and copied its
 * hardness (2) and blast resistance (6 in the units of today); the base state here is stone, whose blast resistance is
 * also 6, because the Thaumcraft block may not exist yet when this block is registered.
 */
public class BlockTravelStairs extends StairBlock implements ITTinkererRegisterable {
    /** The data recipe of the stairs (1.7.10 name {@code PAVETRAVEL}). */
    public static final String RECIPE = "pave_travel_stairs";

    public BlockTravelStairs(BlockState base, BlockBehaviour.Properties properties) {
        super(base, properties);
    }

    public static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F, 6.0F).sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererRecipeMulti(new ThaumicTinkererCraftingBenchRecipe(RECIPE));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        BlockTravelSlab.walkEffects(level, pos, state, this, entity);
        super.stepOn(level, pos, state, entity);
    }
}
