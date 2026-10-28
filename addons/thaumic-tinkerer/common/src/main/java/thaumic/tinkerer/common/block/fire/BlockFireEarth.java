/**
 * Created for the Thaumic Tinkerer imbued fires (the original header of this file names no author).
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.fire;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererCrucibleRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/** Terra Imbued Fire: turns a wide variety of blocks into dirt. */
public class BlockFireEarth extends BlockFireBase {

    private List<FireRules.Rule> blockTransformation = null;

    public BlockFireEarth(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        if (!ConfigHandler.enableFire) return null;
        return (TTResearchItem) new TTResearchItem(
                LibResearch.KEY_FIRE_TERRA,
                new AspectList().add(Aspect.FIRE, 5).add(Aspect.EARTH, 5),
                4,
                -6,
                2,
                new ItemStack(this)).setParents(LibResearch.KEY_BRIGHT_NITOR).setConcealed()
                        .setPages(new ResearchPage("0"), ResearchHelper.crucibleRecipePage(LibResearch.KEY_FIRE_TERRA))
                        .setSecondary();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        if (!ConfigHandler.enableFire) return null;
        return new ThaumicTinkererCrucibleRecipe(
                LibResearch.KEY_FIRE_TERRA,
                new ItemStack(this),
                ItemApi.getItem("itemShard", 3),
                new AspectList().add(Aspect.FIRE, 5).add(Aspect.MAGIC, 5).add(Aspect.EARTH, 5));
    }

    private static List<FireRules.Rule> initBlockTransformation() {
        List<FireRules.Rule> rules = new ArrayList<>();
        rules.add(FireRules.rule(new net.minecraft.world.level.block.Block[] { Blocks.SAND, Blocks.RED_SAND }, Blocks.DIRT));
        rules.add(FireRules.rule(Blocks.GRAVEL, Blocks.CLAY));
        rules.add(FireRules.rule(Blocks.NETHER_BRICKS, Blocks.OAK_PLANKS));
        rules.add(FireRules.rule(Blocks.NETHER_BRICK_FENCE, Blocks.OAK_FENCE));
        rules.add(FireRules.rule(Blocks.NETHER_BRICK_STAIRS, Blocks.OAK_STAIRS));
        rules.add(FireRules.rule(Blocks.CACTUS, Blocks.OAK_LOG));
        rules.add(FireRules.rule(Blocks.SNOW, Blocks.DEAD_BUSH)); // snow_layer -> tallgrass of metadata 0, the withered shrub
        rules.add(FireRules.rule(Blocks.STONE, Blocks.DIRT));
        rules.add(FireRules.rule(Blocks.SPAWNER, Blocks.IRON_BLOCK));
        rules.add(FireRules.rule(BlockTags.OVERWORLD_NATURAL_LOGS, Blocks.DIRT)); // log, log2
        rules.add(FireRules.rule(BlockTags.LEAVES, Blocks.DIRT)); // leaves, leaves2
        rules.add(FireRules.rule(Blocks.COBBLESTONE, Blocks.DIRT));
        rules.add(FireRules.rule(BlockTags.PLANKS, Blocks.DIRT));
        rules.add(FireRules.rule(Blocks.GLASS, Blocks.DIRT));
        return List.copyOf(rules);
    }

    @Override
    public List<FireRules.Rule> getBlockTransformation() {
        if (blockTransformation == null) {
            blockTransformation = initBlockTransformation();
        }

        return blockTransformation;
    }
}
