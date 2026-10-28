/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [8 Sep 2013, 15:59:00 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.quartz;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumic.tinkerer.common.item.quartz.ItemDarkQuartz;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererCraftingBenchRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipeMulti;

/**
 * The block of Smokey Quartz. The three 1.7.10 metadata variants are three blocks (this one, the chiseled block and
 * the pillar, which is a {@code RotatedPillarBlock}); this block registers the recipes of all three, as the one
 * 1.7.10 block did (the data recipes 1, 3 and 4 of {@link ItemDarkQuartz#RECIPE_NAMES}).
 */
public class BlockDarkQuartz extends Block implements ITTinkererRegisterable {

    public BlockDarkQuartz(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Hardness 0.8, resistance 10 in the 1.7.10 setter (6 in the units of today), stone, needs a pickaxe. */
    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.8F, 6.0F).sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererRecipeMulti(
                new ThaumicTinkererCraftingBenchRecipe(ItemDarkQuartz.RECIPE_NAMES[1]),
                new ThaumicTinkererCraftingBenchRecipe(ItemDarkQuartz.RECIPE_NAMES[3]),
                new ThaumicTinkererCraftingBenchRecipe(ItemDarkQuartz.RECIPE_NAMES[4]));
    }
}
