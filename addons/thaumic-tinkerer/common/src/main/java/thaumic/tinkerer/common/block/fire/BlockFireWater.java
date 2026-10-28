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

/** Aqua Imbued Fire: freezes sand, soul sand and glowstone, snows netherrack and cools lava to obsidian. */
public class BlockFireWater extends BlockFireBase {

    private List<FireRules.Rule> blockTransformation = null;

    public BlockFireWater(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        if (!ConfigHandler.enableFire) return null;
        return (TTResearchItem) new TTResearchItem(
                LibResearch.KEY_FIRE_AQUA,
                new AspectList().add(Aspect.FIRE, 5).add(Aspect.WATER, 5),
                2,
                -2,
                2,
                new ItemStack(this)).setParents(LibResearch.KEY_BRIGHT_NITOR).setConcealed()
                        .setPages(new ResearchPage("0"), ResearchHelper.crucibleRecipePage(LibResearch.KEY_FIRE_AQUA))
                        .setSecondary();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        if (!ConfigHandler.enableFire) return null;
        return new ThaumicTinkererCrucibleRecipe(
                LibResearch.KEY_FIRE_AQUA,
                new ItemStack(this),
                ItemApi.getItem("itemShard", 2),
                new AspectList().add(Aspect.FIRE, 5).add(Aspect.MAGIC, 5).add(Aspect.WATER, 5));
    }

    private static List<FireRules.Rule> initBlockTransformation() {
        List<FireRules.Rule> rules = new ArrayList<>();
        rules.add(FireRules.rule(new net.minecraft.world.level.block.Block[] { Blocks.SAND, Blocks.RED_SAND }, Blocks.ICE));
        rules.add(FireRules.rule(Blocks.NETHERRACK, Blocks.SNOW_BLOCK));
        rules.add(FireRules.rule(Blocks.SOUL_SAND, Blocks.ICE));
        rules.add(FireRules.rule(Blocks.GLOWSTONE, Blocks.ICE));
        rules.add(FireRules.rule(Blocks.LAVA, Blocks.OBSIDIAN)); // lava and flowing_lava
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
