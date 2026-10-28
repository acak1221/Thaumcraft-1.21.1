package com.example.tc4addon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import thaumcraft.api.IThaumcraftAddon;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.wands.IWandRodOnUpdate;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandHelper;
import thaumcraft.api.wands.WandRod;

/**
 * A Thaumcraft 4 add-on written only against {@code thaumcraft.api}, the way a 1.7.10 add-on was written.
 *
 * <p>Everything here is what a 1.7.10 add-on did in its {@code init}/{@code postInit}: object tags, arcane,
 * crucible and infusion recipes, a research category and research entries whose pages hold those recipe
 * objects, wand parts, a focus, a bauble, an essentia container. The differences from 1.7.10 are the 1.21.1
 * ones: items are registered in the item registry, there is no metadata, and a data-driven recipe can be shown
 * by its id. See docs/api/guide/README.md.</p>
 *
 * <p>The items are registered from the Fabric {@code main} entrypoint. The Thaumcraft registrations wait for the
 * {@code "thaumcraft"} entrypoint ({@link IThaumcraftAddon}): Fabric runs the {@code main} entrypoints of mods in no
 * particular order, so a Thaumcraft item may not exist yet when this mod's {@code main} runs. This is the 1.21.1
 * counterpart of {@code dependencies = "after:Thaumcraft"} and {@code postInit}.</p>
 */
// [doc:lifecycle]
public final class ExampleAddon implements ModInitializer, IThaumcraftAddon {
    public static final String MOD_ID = "tc4_example";
    // [/doc]
    public static final Item VIS_SHARD = new Item(new Item.Properties());
    public static final Item AMETHYST_ROD = new Item(new Item.Properties());
    public static final Item AMETHYST_CAP = new Item(new Item.Properties());
    // [doc:aspect-component-type]
    /** The data component of items that carry aspects: saved with the stack, synced to the client, equal by content. */
    public static final DataComponentType<StoredAspects> STORED_ASPECTS = DataComponentType.<StoredAspects>builder()
            .persistent(StoredAspects.CODEC).networkSynchronized(StoredAspects.STREAM_CODEC).build();
    // [/doc]
    public static final SparkFocus SPARK_FOCUS = new SparkFocus(new Item.Properties());
    public static final OrderAmulet ORDER_AMULET = new OrderAmulet(new Item.Properties());
    // A scroll teaches the aspects its component holds: two research points of each primal unless a recipe says otherwise.
    public static final InsightScroll INSIGHT_SCROLL = new InsightScroll(new Item.Properties().component(STORED_ASPECTS,
            new StoredAspects(new AspectList().add(Aspect.AIR, 2).add(Aspect.EARTH, 2).add(Aspect.FIRE, 2)
                    .add(Aspect.WATER, 2).add(Aspect.ORDER, 2).add(Aspect.ENTROPY, 2))));
    public static final ShareTome SHARE_TOME = new ShareTome(new Item.Properties());
    public static final OrderTalisman ORDER_TALISMAN = new OrderTalisman(new Item.Properties());
    public static final FocusBelt FOCUS_BELT = new FocusBelt(new Item.Properties());
    public static final EssentiaBowlBlock ESSENTIA_BOWL = new EssentiaBowlBlock(BlockBehaviour.Properties.of()
            .strength(0.5F).sound(SoundType.STONE).noOcclusion());
    public static BlockEntityType<EssentiaBowlBlockEntity> ESSENTIA_BOWL_TYPE;
    /** An aspect of the add-on; created in {@link #onThaumcraftReady()}, on both sides, before anything uses it. */
    public static Aspect TEMPUS;

    // [doc:lifecycle]
    /** Fabric "main": only the add-on's own registry objects. Thaumcraft's items may not exist yet. */
    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, id("vis_shard"), VIS_SHARD);
        // [/doc]
        Registry.register(BuiltInRegistries.ITEM, id("amethyst_rod"), AMETHYST_ROD);
        Registry.register(BuiltInRegistries.ITEM, id("amethyst_cap"), AMETHYST_CAP);
        Registry.register(BuiltInRegistries.ITEM, id("spark_focus"), SPARK_FOCUS);
        Registry.register(BuiltInRegistries.ITEM, id("order_amulet"), ORDER_AMULET);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("stored_aspects"), STORED_ASPECTS);
        Registry.register(BuiltInRegistries.ITEM, id("insight_scroll"), INSIGHT_SCROLL);
        Registry.register(BuiltInRegistries.ITEM, id("share_tome"), SHARE_TOME);
        Registry.register(BuiltInRegistries.ITEM, id("order_talisman"), ORDER_TALISMAN);
        Registry.register(BuiltInRegistries.ITEM, id("focus_belt"), FOCUS_BELT);
        // [doc:block-entity-registration]
        Registry.register(BuiltInRegistries.BLOCK, id("essentia_bowl"), ESSENTIA_BOWL);
        Registry.register(BuiltInRegistries.ITEM, id("essentia_bowl"), new BlockItem(ESSENTIA_BOWL, new Item.Properties()));
        ESSENTIA_BOWL_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("essentia_bowl"),
                BlockEntityType.Builder.of(EssentiaBowlBlockEntity::new, ESSENTIA_BOWL).build(null));
        // [/doc]
        // [doc:felling]
        // Breaking a log with the amethyst core in the hand fells the whole tree, the way the Axe of the Stream did. One call
        // of breakFurthestBlock breaks ONE block (the far end of its walk) and wears no tool; the loop, its limit and the
        // permission check are the caller's. The last call of the loop breaks the clicked log itself, so the break is cancelled.
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (!(level instanceof ServerLevel server) || player.isShiftKeyDown() || !player.getMainHandItem().is(AMETHYST_ROD)
                    || !ThaumcraftApiHelper.isWoodLog(server, pos) || !server.mayInteract(player, pos)) return true;
            for (int logs = 0; logs < 64 && ThaumcraftApiHelper.breakFurthestBlock(server, pos, state.getBlock(), player); logs++) { }
            return false;
        });
        // [/doc]
    // [doc:lifecycle]
    }

    /** The "thaumcraft" entrypoint: every mod's items exist, Thaumcraft's registries are open. Runs on both sides. */
    @Override
    public void onThaumcraftReady() {
        aspects();
        recipesAndResearch();
        wandParts();
        moreContent();
    }
    // [/doc]

    private static void aspects() {
        // [doc:aspect]
        // An aspect of its own: a compound of two registered aspects, a white glyph tinted with its colour (this example
        // borrows an original glyph), and the lang keys tc.aspect.tempus / tc.aspect.help.tempus.
        TEMPUS = new Aspect("tempus", 0xB68CFF, new Aspect[]{Aspect.VOID, Aspect.ORDER},
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/aspects/ordo.png"), 1);
        // [/doc]
        // [doc:object-tags]
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.CLOCK), new AspectList().add(TEMPUS, 4).add(Aspect.MECHANISM, 2));
        // The 1.7.10 ore dictionary call keeps working: "ingotCopper" is the c:ingots/copper tag.
        ThaumcraftApi.registerObjectTag("ingotCopper", new AspectList().add(Aspect.METAL, 3).add(Aspect.EXCHANGE, 1));
        // 1.7.10: ThaumcraftApi.registerObjectTag(new ItemStack(item, 1, meta), aspects). There is no metadata now.
        ThaumcraftApi.registerObjectTag(new ItemStack(VIS_SHARD), new AspectList().add(Aspect.MAGIC, 2).add(Aspect.CRYSTAL, 2));
        // [/doc]
        // [doc:complex-tag]
        // A complex tag adds to the aspects the item gets from its recipe, so it needs the recipes of a running server:
        // 1.7.10 add-ons called it in postInit, in 1.21.1 it goes into a server start event.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> ThaumcraftApi.registerComplexObjectTag(
                new ItemStack(ESSENTIA_BOWL), new AspectList().add(Aspect.VOID, 2), server.overworld()));
        // [/doc]
        // [doc:entity-tag]
        // An entity the original never knew, by its registry id: the thaumometer gives these aspects for scanning a frog.
        ThaumcraftApi.registerEntityTag("minecraft:frog", new AspectList().add(Aspect.WATER, 2).add(Aspect.BEAST, 2).add(TEMPUS, 1));
        // [/doc]
    }

    private static void recipesAndResearch() {
        // [doc:api-recipes]
        // The same calls and the same units as 1.7.10: arcane costs are whole vis of the six primals, crucible and
        // infusion costs are essentia. A Thaumcraft item is looked up by its 1.21.1 id.
        ShapedArcaneRecipe arcane = ThaumcraftApi.addArcaneCraftingRecipe("EXAMPLE_SHARDS", new ItemStack(VIS_SHARD, 4),
                new AspectList().add(Aspect.ORDER, 5).add(Aspect.AIR, 5),
                " A ", "ASA", " A ", 'A', new ItemStack(Items.AMETHYST_SHARD), 'S', new ItemStack(ItemsTC.require("balance_shard")));
        ShapelessArcaneRecipe shapeless = ThaumcraftApi.addShapelessArcaneCraftingRecipe("EXAMPLE_SCROLL", new ItemStack(INSIGHT_SCROLL, 2),
                new AspectList().add(Aspect.AIR, 2).add(Aspect.ORDER, 2),
                new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(VIS_SHARD), "dyeBlack");
        CrucibleRecipe crucible = ThaumcraftApi.addCrucibleRecipe("EXAMPLE_SHARDS", new ItemStack(Items.AMETHYST_SHARD),
                new ItemStack(Items.QUARTZ), new AspectList().add(Aspect.CRYSTAL, 4).add(Aspect.MAGIC, 2));
        InfusionRecipe infusion = ThaumcraftApi.addInfusionCraftingRecipe("EXAMPLE_INFUSION", new ItemStack(Items.ECHO_SHARD), 2,
                new AspectList().add(Aspect.SENSES, 8).add(Aspect.DARKNESS, 8), new ItemStack(Items.AMETHYST_SHARD),
                new ItemStack[]{new ItemStack(VIS_SHARD), new ItemStack(Items.SCULK), new ItemStack(VIS_SHARD), new ItemStack(Items.SCULK)});
        // [/doc]

        // [doc:category]
        // A research category of its own, after the six original tabs: the key, the tab icon and the background.
        ResearchCategories.registerCategory("EXAMPLE",
                ResourceLocation.withDefaultNamespace("textures/item/amethyst_shard.png"),
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchback.png"));
        // [/doc]

        // [doc:research]
        new ResearchItem("EXAMPLE_SHARDS", "EXAMPLE", new AspectList().add(Aspect.CRYSTAL, 3).add(Aspect.MAGIC, 3),
                0, 0, 1, new ItemStack(VIS_SHARD))
                .setParents("CRUCIBLE") // a parent from the original tree
                .setPages(new ResearchPage("tc4_example.research_page.EXAMPLE_SHARDS.1"),
                        new ResearchPage(arcane),
                        new ResearchPage(crucible),
                        // Not in 1.7.10: a data-driven recipe of the add-on, shown by its id.
                        ResearchPage.recipe(id("vis_shard_from_amethyst")),
                        new ResearchPage(new AspectList().add(Aspect.CRYSTAL, 3).add(Aspect.MAGIC, 3).add(TEMPUS, 2)))
                .registerResearchItem();

        new ResearchItem("EXAMPLE_INFUSION", "EXAMPLE", new AspectList().add(Aspect.SENSES, 4).add(Aspect.MAGIC, 3),
                2, 1, 2, new ItemStack(Items.ECHO_SHARD))
                .setParents("EXAMPLE_SHARDS", "INFUSION")
                .setConcealed() // shown only once its parents are known
                .setPages(new ResearchPage("tc4_example.research_page.EXAMPLE_INFUSION.1"), new ResearchPage(infusion))
                .registerResearchItem();

        new ResearchItem("EXAMPLE_SECRET", "EXAMPLE", new AspectList().add(Aspect.METAL, 2).add(Aspect.FIRE, 2),
                -2, 1, 1, ResourceLocation.withDefaultNamespace("textures/item/raw_copper.png"))
                .setParents("EXAMPLE_SHARDS")
                .setHidden() // invisible until a scan of a trigger gives its clue
                .setItemTriggers(new ItemStack(Items.ECHO_SHARD))
                .setPages(new ResearchPage("tc4_example.research_page.EXAMPLE_SECRET.1"),
                        new ResearchPage(new ItemStack(Items.RAW_COPPER)))
                .registerResearchItem();
        ThaumcraftApi.addWarpToResearch("EXAMPLE_SECRET", 1);
        // [/doc]

        new ResearchItem("EXAMPLE_SCROLL", "EXAMPLE", new AspectList().add(Aspect.MIND, 3).add(Aspect.MAGIC, 2),
                0, -2, 1, new ItemStack(INSIGHT_SCROLL))
                .setParents("EXAMPLE_SHARDS")
                .setPages(new ResearchPage("tc4_example.research_page.EXAMPLE_SCROLL.1"),
                        new ResearchPage(shapeless),
                        ResearchPage.recipe(id("insight_scroll_from_crucible")))
                .registerResearchItem();
    }

    private static void wandParts() {
        // [doc:wand-parts]
        // A wand rod and a wand cap, the 1.7.10 constructors: capacity in whole vis, the craft cost of the part, and an
        // entity-style texture with the UV layout of the original ModelWand (a 1.7.10 PNG works unchanged). The arcane
        // workbench builds wands, staves and sceptres from the part items once ROD_<tag> and CAP_<tag> are researched.
        IWandRodOnUpdate regrowth = (wand, player) -> {
            // Server, every tick, for each wand with this rod in the player's inventory: one Ordo every five seconds
            // up to a tenth of the capacity. getVis/getMaxVis are centivis, addVis takes whole vis.
            if (player.tickCount % 100 == 0 && WandHelper.getVis(wand, Aspect.ORDER) < WandHelper.getMaxVis(wand) / 10) {
                WandHelper.addVis(wand, Aspect.ORDER, 1, true);
            }
        };
        new WandRod("amethyst", 80, new ItemStack(AMETHYST_ROD), 4, regrowth,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_quartz.png"));
        WandCap cap = new WandCap("amethyst", 0.95F, java.util.List.of(Aspect.ORDER, Aspect.ENTROPY), 0.85F, new ItemStack(AMETHYST_CAP), 3);
        cap.setTexture(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_cap_thaumium.png"));
        new ResearchItem("ROD_amethyst", "EXAMPLE", new AspectList().add(Aspect.CRYSTAL, 3).add(Aspect.TOOL, 2), 0, 2, 1,
                new ItemStack(AMETHYST_ROD)).setParents("EXAMPLE_SHARDS").setSecondary()
                .setPages(new ResearchPage("tc4_example.research_page.ROD_amethyst.1")).registerResearchItem();
        new ResearchItem("CAP_amethyst", "EXAMPLE", new AspectList().add(Aspect.CRYSTAL, 2).add(Aspect.ORDER, 2), 2, 2, 1,
                new ItemStack(AMETHYST_CAP)).setParents("EXAMPLE_SHARDS").setSecondary()
                .setPages(new ResearchPage("tc4_example.research_page.CAP_amethyst.1")).registerResearchItem();
        // [/doc]
    }

    private static void moreContent() {
        // The recipes of these three are JSON files of the native recipe types (data/tc4_example/recipe), shown by id.
        new ResearchItem("EXAMPLE_FOCUS", "EXAMPLE", new AspectList().add(Aspect.FIRE, 3).add(Aspect.AIR, 2).add(Aspect.MAGIC, 2),
                -2, -1, 2, new ItemStack(SPARK_FOCUS))
                .setParents("EXAMPLE_SHARDS", "FOCUSFIRE")
                .setPages(new ResearchPage("tc4_example.research_page.EXAMPLE_FOCUS.1"), ResearchPage.recipe(id("spark_focus")))
                .registerResearchItem();
        new ResearchItem("EXAMPLE_BOWL", "EXAMPLE", new AspectList().add(Aspect.VOID, 3).add(Aspect.WATER, 2),
                2, -1, 1, new ItemStack(ESSENTIA_BOWL))
                .setParents("EXAMPLE_SHARDS", "DISTILESSENTIA")
                .setPages(new ResearchPage("tc4_example.research_page.EXAMPLE_BOWL.1"), ResearchPage.recipe(id("essentia_bowl")))
                .registerResearchItem();
        new ResearchItem("EXAMPLE_AMULET", "EXAMPLE", new AspectList().add(Aspect.ORDER, 4).add(Aspect.MAGIC, 3).add(Aspect.ARMOR, 2),
                4, 1, 2, new ItemStack(ORDER_AMULET))
                .setParents("EXAMPLE_INFUSION", "RUNICARMOR")
                .setPages(new ResearchPage("tc4_example.research_page.EXAMPLE_AMULET.1"), ResearchPage.recipe(id("order_amulet")))
                .registerResearchItem();

        // [doc:misc-registrations]
        // Crafting the amulet (infusion fires the crafted-item event) gives one sticky warp, as in 1.7.10.
        ThaumcraftApi.addWarpToItem(new ItemStack(ORDER_AMULET), 1);
        // The infernal furnace sometimes pays a bonus: 1.21.1 raw copper gets the copper nugget its ore gave in 1.7.10.
        ThaumcraftApi.addSmeltingBonus(new ItemStack(Items.RAW_COPPER), new ItemStack(ItemsTC.require("copper_nugget")));
        // Loot bags: weight 15 in the common (0) and uncommon (1) bags.
        ThaumcraftApi.addLootBagItem(new ItemStack(VIS_SHARD, 2), 15, 0, 1);
        // [/doc]
    }

    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }
}
