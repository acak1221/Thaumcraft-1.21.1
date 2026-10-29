/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [9 Sep 2013, 01:20:22 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import java.awt.Color;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererCrucibleRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Spellbinding Cloth: put it into the crafting grid with one enchanted item and the item comes out without its
 * enchantments (see {@link SpellClothRecipe}); the cloth stays in the grid, worn by one use.
 *
 * <p>The cloth lasts {@link #USES} + 1 cleanings, as in 1.7.10, where the crafting slot destroyed a container item only
 * when its damage went <em>above</em> its maximum (see {@link SpellClothCraftingHandler}). Its colour fades from violet to
 * white as it wears out.</p>
 */
public class ItemSpellCloth extends Item implements ITTinkererRegisterable {
    /** {@code LibFeatures.SPELL_CLOTH_USES}: the maximum damage of the cloth. */
    public static final int USES = 35;

    public ItemSpellCloth(Item.Properties properties) {
        super(properties.durability(USES).stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return (TTResearchItem) new TTResearchItem(
                LibResearch.KEY_SPELL_CLOTH,
                new AspectList().add(Aspect.MAGIC, 2).add(Aspect.CLOTH, 1),
                3,
                2,
                2,
                new ItemStack(this)).setParentsHidden("ENCHFABRIC").setPages(
                        new ResearchPage("0"),
                        ResearchHelper.crucibleRecipePage(LibResearch.KEY_SPELL_CLOTH));
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        // itemResource:7, the Enchanted Fabric
        return new ThaumicTinkererCrucibleRecipe(
                LibResearch.KEY_SPELL_CLOTH,
                new ItemStack(this),
                ItemApi.getItem("itemResource", 7),
                new AspectList().add(Aspect.MAGIC, 10).add(Aspect.ENTROPY, 6).add(Aspect.EXCHANGE, 4));
    }

    /**
     * 1.7.10 {@code getColorFromItemStack}: hue 0.75 (violet), saturation from 0.5 for a new cloth down to 0 for a worn-out
     * one, full brightness ({@code Color.HSBtoRGB}, as the original). Opaque ARGB, as the item colour handler expects.
     */
    public static int color(ItemStack stack) {
        float max = stack.getMaxDamage();
        float saturation = (max - (float) stack.getDamageValue()) / max * 0.5F;
        return Color.HSBtoRGB(0.75F, saturation, 1F);
    }
}
