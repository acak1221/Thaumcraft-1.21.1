/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.kami;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipeMulti;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.KamiResearchItem;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The eight metadata values of the 1.7.10 {@code kamiResource} item are eight items now, in the original order
 * ({@link LibItemNames#KAMI_RESOURCE_NAMES}): 0 {@code ichor}, 1 {@code ichor_cloth}, 2 {@code ichorium}, 3
 * {@code ichor_nugget}, 4 {@code ichor_cap}, 5 {@code ichorcloth_rod}, 6 {@code nether_shard}, 7 {@code ender_shard}.
 * The two shards are common, everything else has the Kami rarity.
 *
 * <p>The recipes and the research of the whole family belong to the Ichor item (index 0), as the research of the
 * metadata item was one {@code TTResearchItemMulti} in 1.7.10.</p>
 */
public class ItemKamiResource extends KamiItemBase {
    public static final int ICHOR = 0;
    public static final int ICHOR_CLOTH = 1;
    public static final int ICHORIUM = 2;
    public static final int ICHOR_NUGGET = 3;
    public static final int ICHOR_CAP = 4;
    public static final int ICHORCLOTH_ROD = 5;
    public static final int NETHER_SHARD = 6;
    public static final int ENDER_SHARD = 7;

    private final int index;

    public ItemKamiResource(int index, Item.Properties properties) {
        super(properties, index == NETHER_SHARD || index == ENDER_SHARD ? Rarity.COMMON : KAMI_RARITY);
        this.index = index;
    }

    public int index() {
        return index;
    }

    /** A stack of the resource with that 1.7.10 metadata value; only after registration. */
    public static ItemStack stack(int meta) {
        return TTRegistry.stack(LibItemNames.KAMI_RESOURCE_NAMES[meta]);
    }

    public static ItemStack stack(int meta, int count) {
        return TTRegistry.stack(LibItemNames.KAMI_RESOURCE_NAMES[meta], count);
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        if (index != ICHOR) return null;
        // 1.7.10 also registered GameRegistry.addShapelessRecipe(9 x nugget <- ichorium): data/thaumictinkerer/recipe/ichor_nuggets.json
        return new ThaumicTinkererRecipeMulti(
                new ThaumicTinkererArcaneRecipe(LibResearch.KEY_ICHOR_CLOTH, LibResearch.KEY_ICHOR_CLOTH,
                        stack(ICHOR_CLOTH, 3),
                        new AspectList().add(Aspect.FIRE, 125).add(Aspect.EARTH, 125).add(Aspect.WATER, 125)
                                .add(Aspect.AIR, 125).add(Aspect.ORDER, 125).add(Aspect.ENTROPY, 125),
                        "CCC", "III", "DDD", 'C', KamiResearch.tc("itemResource", 7), 'I', stack(ICHOR), 'D',
                        new ItemStack(Items.DIAMOND)),
                new ThaumicTinkererArcaneRecipe(LibResearch.KEY_ICHORIUM, LibResearch.KEY_ICHORIUM, stack(ICHORIUM),
                        new AspectList().add(Aspect.FIRE, 100).add(Aspect.EARTH, 100).add(Aspect.WATER, 100)
                                .add(Aspect.AIR, 100).add(Aspect.ORDER, 100).add(Aspect.ENTROPY, 100),
                        " T ", "IDI", " I ", 'T', KamiResearch.tc("itemResource", 2), 'I', stack(ICHOR), 'D',
                        new ItemStack(Items.DIAMOND)),
                new ThaumicTinkererArcaneRecipe(LibResearch.KEY_ICHOR_CAP, LibResearch.KEY_ICHOR_CAP, stack(ICHOR_CAP, 2),
                        new AspectList().add(Aspect.FIRE, 100).add(Aspect.EARTH, 100).add(Aspect.WATER, 100)
                                .add(Aspect.AIR, 100).add(Aspect.ORDER, 100).add(Aspect.ENTROPY, 100),
                        "ICI", " M ", "ICI", 'M', stack(ICHORIUM), 'I', stack(ICHOR), 'C',
                        KamiResearch.tc("itemWandCap", 2)),
                new ThaumicTinkererInfusionRecipe(LibResearch.KEY_ICHOR, stack(ICHOR, 8), 7,
                        new AspectList().add(Aspect.MAN, 32).add(Aspect.LIGHT, 32).add(Aspect.SOUL, 64),
                        new ItemStack(Items.NETHER_STAR), new ItemStack(Items.DIAMOND), stack(ENDER_SHARD),
                        new ItemStack(Items.ENDER_EYE), stack(NETHER_SHARD)),
                new ThaumicTinkererInfusionRecipe(LibResearch.KEY_ICHORCLOTH_ROD, stack(ICHORCLOTH_ROD), 9,
                        new AspectList().add(Aspect.MAGIC, 100).add(Aspect.LIGHT, 32).add(Aspect.TOOL, 32),
                        KamiResearch.tc("itemWandRod", 2), stack(ICHOR), stack(ICHOR_CLOTH),
                        KamiResearch.tc("itemResource", 14), new ItemStack(Items.GHAST_TEAR),
                        KamiResearch.tc("itemResource", 14), stack(ICHOR_CLOTH)));
    }

    /** The six research entries of the family (1.7.10: the {@code TTResearchItemMulti} of {@code ItemKamiResource}). */
    public static List<TTResearchItem> research() {
        TTResearchItem shards = new KamiResearchItem(LibResearch.KEY_DIMENSION_SHARDS, new AspectList(), 7, 8, 0,
                stack(ENDER_SHARD));
        shards.setStub();
        shards.setAutoUnlock();
        shards.setRound();
        shards.setPages(new ResearchPage("0"));

        TTResearchItem ichor = KamiResearch.entry(LibResearch.KEY_ICHOR,
                new AspectList().add(Aspect.MAN, 1).add(Aspect.LIGHT, 2).add(Aspect.SOUL, 1).add(Aspect.TAINT, 1), 9, 8,
                stack(ICHOR), 5, KamiResearch.NONE, KamiResearch.NONE, new ResearchPage("0"),
                ResearchHelper.infusionPage(LibResearch.KEY_ICHOR));

        TTResearchItem cloth = KamiResearch.entry(LibResearch.KEY_ICHOR_CLOTH,
                new AspectList().add(Aspect.CLOTH, 2).add(Aspect.LIGHT, 1).add(Aspect.CRAFT, 1).add(Aspect.SENSES, 1),
                11, 7, stack(ICHOR_CLOTH), 0, KamiResearch.of(LibResearch.KEY_ICHOR), KamiResearch.NONE,
                new ResearchPage("0"), ResearchHelper.arcaneRecipePage(LibResearch.KEY_ICHOR_CLOTH));

        TTResearchItem ichorium = KamiResearch.entry(LibResearch.KEY_ICHORIUM,
                new AspectList().add(Aspect.METAL, 2).add(Aspect.LIGHT, 1).add(Aspect.CRAFT, 1).add(Aspect.TOOL, 1), 11,
                9, stack(ICHORIUM), 0, KamiResearch.of(LibResearch.KEY_ICHOR),
                KamiResearch.of(LibResearch.KEY_ICHOR_CLOTH), new ResearchPage("0"),
                ResearchHelper.arcaneRecipePage(LibResearch.KEY_ICHORIUM));

        TTResearchItem cap = KamiResearch.entry(LibResearch.KEY_ICHOR_CAP,
                new AspectList().add(Aspect.TOOL, 2).add(Aspect.METAL, 1).add(Aspect.LIGHT, 1).add(Aspect.MAGIC, 1), 11,
                11, stack(ICHOR_CAP), 2, KamiResearch.of(LibResearch.KEY_ICHORIUM), KamiResearch.NONE,
                new ResearchPage("0"), ResearchHelper.arcaneRecipePage(LibResearch.KEY_ICHOR_CAP));

        TTResearchItem rod = KamiResearch.entry(LibResearch.KEY_ICHORCLOTH_ROD,
                new AspectList().add(Aspect.TOOL, 2).add(Aspect.CLOTH, 1).add(Aspect.LIGHT, 1).add(Aspect.MAGIC, 1), 14,
                2, stack(ICHORCLOTH_ROD), 1, KamiResearch.of(LibResearch.KEY_ICHOR_CLOTH),
                KamiResearch.of(LibResearch.KEY_ICHOR_CAP), new ResearchPage("0"),
                ResearchHelper.infusionPage(LibResearch.KEY_ICHORCLOTH_ROD));

        return List.of(shards, ichor, cloth, ichorium, cap, rod);
    }
}
