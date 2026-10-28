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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.registry.ThaumicTinkererCrucibleRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/** Perditio Imbued Fire: the antidote, which turns the other imbued fires back into plain fire. Ticks every tick. */
public class BlockFireChaos extends BlockFireBase {

    private List<FireRules.Rule> blockTransformation = null;

    public BlockFireChaos(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        if (!ConfigHandler.enableFire) return null;
        return (TTResearchItem) new TTResearchItem(
                LibResearch.KEY_FIRE_PERDITIO,
                new AspectList().add(Aspect.FIRE, 5).add(Aspect.ENTROPY, 5),
                2,
                -8,
                3,
                new ItemStack(this))
                        .setParents(LibResearch.KEY_BRIGHT_NITOR).setConcealed()
                        .setPages(
                                new ResearchPage("0"),
                                ResearchHelper.crucibleRecipePage(LibResearch.KEY_FIRE_PERDITIO))
                        .setSecondary();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        if (!ConfigHandler.enableFire) return null;
        return new ThaumicTinkererCrucibleRecipe(
                LibResearch.KEY_FIRE_PERDITIO,
                new ItemStack(this),
                ItemApi.getItem("itemShard", 5),
                new AspectList().add(Aspect.FIRE, 5).add(Aspect.MAGIC, 5).add(Aspect.ENTROPY, 5));
    }

    @Override
    public int tickRate(Level level) {
        return 1;
    }

    private static List<FireRules.Rule> initBlockTransformation() {
        List<FireRules.Rule> rules = new ArrayList<>();
        for (String path : new String[] { LibBlockNames.BLOCK_FIRE_AIR, LibBlockNames.BLOCK_FIRE_WATER, LibBlockNames.BLOCK_FIRE_EARTH,
                LibBlockNames.BLOCK_FIRE_FIRE, LibBlockNames.BLOCK_FIRE_ORDER }) {
            Block fire = TTRegistry.block(path);
            rules.add(FireRules.rule(fire, Blocks.FIRE));
        }
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
