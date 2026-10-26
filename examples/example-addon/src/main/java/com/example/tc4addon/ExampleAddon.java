package com.example.tc4addon;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.IThaumcraftAddon;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;

/**
 * A Thaumcraft 4 add-on written only against {@code thaumcraft.api}, the way a 1.7.10 add-on was written.
 *
 * <p>Everything here is what a 1.7.10 add-on did in its {@code init}/{@code postInit}: object tags, arcane,
 * crucible and infusion recipes, a research category and research entries whose pages hold those recipe
 * objects. The differences from 1.7.10 are the 1.21.1 ones: items are registered in the item registry, there
 * is no metadata, and a data-driven recipe can be shown by its id. See docs/api/PORTING.md.</p>
 *
 * <p>The item is registered from the Fabric {@code main} entrypoint. The Thaumcraft registrations wait for the
 * {@code "thaumcraft"} entrypoint ({@link IThaumcraftAddon}): Fabric runs the {@code main} entrypoints of mods in no
 * particular order, so a Thaumcraft item may not exist yet when this mod's {@code main} runs. This is the 1.21.1
 * counterpart of {@code dependencies = "after:Thaumcraft"} and {@code postInit}.</p>
 */
public final class ExampleAddon implements ModInitializer, IThaumcraftAddon {
    public static final String MOD_ID = "tc4_example";
    public static final Item VIS_SHARD = new Item(new Item.Properties());
    public static final Item AMETHYST_ROD = new Item(new Item.Properties());
    public static final Item AMETHYST_CAP = new Item(new Item.Properties());

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, id("vis_shard"), VIS_SHARD);
        Registry.register(BuiltInRegistries.ITEM, id("amethyst_rod"), AMETHYST_ROD);
        Registry.register(BuiltInRegistries.ITEM, id("amethyst_cap"), AMETHYST_CAP);
    }

    @Override
    public void onThaumcraftReady() {
        // 1.7.10: ThaumcraftApi.registerObjectTag(new ItemStack(item, 1, meta), aspects). There is no metadata now.
        ThaumcraftApi.registerObjectTag(new ItemStack(VIS_SHARD), new AspectList().add(Aspect.MAGIC, 2).add(Aspect.CRYSTAL, 2));

        // Recipes: the same calls and the same units as 1.7.10 (arcane costs are whole vis, crucible and infusion
        // costs are essentia). A Thaumcraft item is looked up by its 1.21.1 id.
        ShapedArcaneRecipe arcane = ThaumcraftApi.addArcaneCraftingRecipe("EXAMPLE_SHARDS", new ItemStack(VIS_SHARD, 4),
                new AspectList().add(Aspect.ORDER, 5).add(Aspect.AIR, 5),
                " A ", "ASA", " A ", 'A', new ItemStack(Items.AMETHYST_SHARD), 'S', new ItemStack(ItemsTC.require("balance_shard")));
        CrucibleRecipe crucible = ThaumcraftApi.addCrucibleRecipe("EXAMPLE_SHARDS", new ItemStack(Items.AMETHYST_SHARD),
                new ItemStack(Items.QUARTZ), new AspectList().add(Aspect.CRYSTAL, 4).add(Aspect.MAGIC, 2));
        InfusionRecipe infusion = ThaumcraftApi.addInfusionCraftingRecipe("EXAMPLE_INFUSION", new ItemStack(Items.ECHO_SHARD), 2,
                new AspectList().add(Aspect.SENSES, 8).add(Aspect.DARKNESS, 8), new ItemStack(Items.AMETHYST_SHARD),
                new ItemStack[]{new ItemStack(VIS_SHARD), new ItemStack(Items.SCULK), new ItemStack(VIS_SHARD), new ItemStack(Items.SCULK)});

        // A research category of its own, after the six original tabs.
        ResearchCategories.registerCategory("EXAMPLE",
                ResourceLocation.withDefaultNamespace("textures/item/amethyst_shard.png"),
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchback.png"));

        new ResearchItem("EXAMPLE_SHARDS", "EXAMPLE", new AspectList().add(Aspect.CRYSTAL, 3).add(Aspect.MAGIC, 3),
                0, 0, 1, new ItemStack(VIS_SHARD))
                .setParents("CRUCIBLE") // a parent from the original tree
                .setPages(new ResearchPage("tc4_example.research_page.EXAMPLE_SHARDS.1"),
                        new ResearchPage(arcane),
                        new ResearchPage(crucible),
                        // Not in 1.7.10: a data-driven recipe of the add-on, shown by its id.
                        ResearchPage.recipe(id("vis_shard_from_amethyst")),
                        new ResearchPage(new AspectList().add(Aspect.CRYSTAL, 3).add(Aspect.MAGIC, 3)))
                .registerResearchItem();

        new ResearchItem("EXAMPLE_INFUSION", "EXAMPLE", new AspectList().add(Aspect.SENSES, 4).add(Aspect.MAGIC, 3),
                2, 1, 2, new ItemStack(Items.ECHO_SHARD))
                .setParents("EXAMPLE_SHARDS", "INFUSION")
                .setConcealed()
                .setPages(new ResearchPage("tc4_example.research_page.EXAMPLE_INFUSION.1"), new ResearchPage(infusion))
                .registerResearchItem();

        new ResearchItem("EXAMPLE_SECRET", "EXAMPLE", new AspectList().add(Aspect.METAL, 2).add(Aspect.FIRE, 2),
                -2, 1, 1, ResourceLocation.withDefaultNamespace("textures/item/raw_copper.png"))
                .setParents("EXAMPLE_SHARDS")
                .setHidden()
                .setItemTriggers(new ItemStack(Items.ECHO_SHARD))
                .setPages(new ResearchPage("tc4_example.research_page.EXAMPLE_SECRET.1"),
                        new ResearchPage(new ItemStack(Items.RAW_COPPER)))
                .registerResearchItem();
        ThaumcraftApi.addWarpToResearch("EXAMPLE_SECRET", 1);

        // A wand rod and a wand cap, the 1.7.10 constructors: capacity in whole vis, the craft cost of the part, and an
        // entity-style texture with the UV layout of the original ModelWand (a 1.7.10 PNG works unchanged). The arcane
        // workbench builds wands, staves and sceptres from the part items once ROD_<tag> and CAP_<tag> are researched.
        new WandRod("amethyst", 80, new ItemStack(AMETHYST_ROD), 4,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_quartz.png"));
        WandCap cap = new WandCap("amethyst", 0.95F, java.util.List.of(Aspect.ORDER, Aspect.ENTROPY), 0.85F, new ItemStack(AMETHYST_CAP), 3);
        cap.setTexture(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_cap_thaumium.png"));
        new ResearchItem("ROD_amethyst", "EXAMPLE", new AspectList().add(Aspect.CRYSTAL, 3).add(Aspect.TOOL, 2), 0, 2, 1,
                new ItemStack(AMETHYST_ROD)).setParents("EXAMPLE_SHARDS").setSecondary()
                .setPages(new ResearchPage("tc4_example.research_page.ROD_amethyst.1")).registerResearchItem();
        new ResearchItem("CAP_amethyst", "EXAMPLE", new AspectList().add(Aspect.CRYSTAL, 2).add(Aspect.ORDER, 2), 2, 2, 1,
                new ItemStack(AMETHYST_CAP)).setParents("EXAMPLE_SHARDS").setSecondary()
                .setPages(new ResearchPage("tc4_example.research_page.CAP_amethyst.1")).registerResearchItem();
    }

    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }
}
