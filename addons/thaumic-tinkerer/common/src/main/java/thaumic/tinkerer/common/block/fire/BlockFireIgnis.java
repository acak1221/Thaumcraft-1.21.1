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

/** Ignis Imbued Fire: turns the surface of the world into the matching blocks of the Nether. */
public class BlockFireIgnis extends BlockFireBase {

    private List<FireRules.Rule> blockTransformation = null;

    public BlockFireIgnis(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        if (!ConfigHandler.enableFire) return null;
        return (TTResearchItem) new TTResearchItem(
                LibResearch.KEY_FIRE_IGNIS,
                new AspectList().add(Aspect.FIRE, 10),
                4,
                -4,
                2,
                new ItemStack(this)).setParents(LibResearch.KEY_BRIGHT_NITOR).setConcealed()
                        .setPages(new ResearchPage("0"), ResearchHelper.crucibleRecipePage(LibResearch.KEY_FIRE_IGNIS))
                        .setSecondary();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        if (!ConfigHandler.enableFire) return null;
        return new ThaumicTinkererCrucibleRecipe(
                LibResearch.KEY_FIRE_IGNIS,
                new ItemStack(this),
                ItemApi.getItem("itemShard", 1),
                new AspectList().add(Aspect.FIRE, 10).add(Aspect.AIR, 5));
    }

    private static List<FireRules.Rule> initBlockTransformation() {
        List<FireRules.Rule> rules = new ArrayList<>();
        rules.add(FireRules.rule(Blocks.GRASS_BLOCK, Blocks.NETHERRACK));
        rules.add(FireRules.rule(new net.minecraft.world.level.block.Block[] { Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL },
                Blocks.NETHERRACK));
        rules.add(FireRules.rule(new net.minecraft.world.level.block.Block[] { Blocks.SAND, Blocks.RED_SAND }, Blocks.SOUL_SAND));
        rules.add(FireRules.rule(Blocks.GRAVEL, Blocks.SOUL_SAND));
        rules.add(FireRules.rule(Blocks.CLAY, Blocks.GLOWSTONE));
        rules.add(FireRules.rule(BlockTags.COAL_ORES, Blocks.NETHER_QUARTZ_ORE));
        rules.add(FireRules.rule(BlockTags.IRON_ORES, Blocks.NETHER_QUARTZ_ORE));
        rules.add(FireRules.rule(BlockTags.DIAMOND_ORES, Blocks.NETHER_QUARTZ_ORE));
        rules.add(FireRules.rule(BlockTags.EMERALD_ORES, Blocks.NETHER_QUARTZ_ORE));
        rules.add(FireRules.rule(Blocks.GOLD_BLOCK, Blocks.NETHER_QUARTZ_ORE)); // (sic, as in 1.7.10: gold block, not gold ore)
        rules.add(FireRules.rule(BlockTags.LAPIS_ORES, Blocks.NETHER_QUARTZ_ORE));
        rules.add(FireRules.rule(BlockTags.REDSTONE_ORES, Blocks.NETHER_QUARTZ_ORE)); // redstone_ore and lit_redstone_ore
        rules.add(FireRules.rule(FireRules::isWaterSource, Blocks.LAVA));
        rules.add(FireRules.rule(new net.minecraft.world.level.block.Block[] { Blocks.WHEAT, Blocks.POTATOES, Blocks.CARROTS },
                Blocks.NETHER_WART));
        rules.add(FireRules.rule(new net.minecraft.world.level.block.Block[] { Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM,
                Blocks.AZURE_BLUET, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP,
                Blocks.OXEYE_DAISY }, Blocks.BROWN_MUSHROOM)); // red_flower
        rules.add(FireRules.rule(Blocks.DANDELION, Blocks.DANDELION)); // yellow_flower -> yellow_flower (sic)
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
