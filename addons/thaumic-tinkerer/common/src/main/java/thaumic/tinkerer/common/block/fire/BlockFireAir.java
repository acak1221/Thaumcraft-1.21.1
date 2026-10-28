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

/** Aer Imbued Fire: turns logs, leaves, ice, dirt and grass into sand-like blocks, and (optionally) water into cake. */
public class BlockFireAir extends BlockFireBase {

    private List<FireRules.Rule> blockTransformation = null;

    public BlockFireAir(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        if (!ConfigHandler.enableFire) return null;
        return (TTResearchItem) new TTResearchItem(
                LibResearch.KEY_FIRE_AER,
                new AspectList().add(Aspect.FIRE, 5).add(Aspect.AIR, 5),
                3,
                -7,
                2,
                new ItemStack(this)).setParents(LibResearch.KEY_BRIGHT_NITOR).setConcealed()
                        .setPages(new ResearchPage("0"), ResearchHelper.crucibleRecipePage(LibResearch.KEY_FIRE_AER))
                        .setSecondary();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        if (!ConfigHandler.enableFire) return null;
        return new ThaumicTinkererCrucibleRecipe(
                LibResearch.KEY_FIRE_AER,
                new ItemStack(this),
                ItemApi.getItem("itemShard", 0),
                new AspectList().add(Aspect.FIRE, 5).add(Aspect.MAGIC, 5).add(Aspect.AIR, 5));
    }

    private static List<FireRules.Rule> initBlockTransformation() {
        List<FireRules.Rule> rules = new ArrayList<>();
        rules.add(FireRules.rule(BlockTags.OVERWORLD_NATURAL_LOGS, Blocks.SAND)); // log, log2
        rules.add(FireRules.rule(BlockTags.LEAVES, Blocks.SANDSTONE)); // leaves, leaves2
        rules.add(FireRules.rule(Blocks.ICE, Blocks.GLASS));
        if (ConfigHandler.enableCake) {
            rules.add(FireRules.rule(FireRules::isWaterSource, Blocks.CAKE));
        }
        rules.add(FireRules.rule(new net.minecraft.world.level.block.Block[] { Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL },
                Blocks.SAND));
        rules.add(FireRules.rule(Blocks.GRASS_BLOCK, Blocks.SAND));
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
