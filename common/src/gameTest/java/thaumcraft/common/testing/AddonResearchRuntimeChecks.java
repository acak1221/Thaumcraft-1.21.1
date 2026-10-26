package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.AddonResearch;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchBrowserData;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchClues;
import thaumcraft.common.lib.research.ResearchNoteIssuance;
import thaumcraft.common.lib.research.ResearchPurchase;
import thaumcraft.common.lib.research.ResearchRegistrationOrder;

/**
 * Research an add-on registers through the public API is research of the game, not only of the map.
 *
 * <p>The scenario registers what a 1.7.10 add-on registered in its init: a category, an entry with a parent of the
 * original tree, pages holding the add-on's arcane and crucible recipe objects and an aspect page, a hidden entry
 * revealed by scanning an item, a secondary entry bought with research points, a warp value, and a change to an
 * original entry made through its public view. It then walks the native systems over them and removes everything it
 * registered, so no other scenario sees it.</p>
 */
public final class AddonResearchRuntimeChecks {
    private static final String CATEGORY = "TCAPI_TEST";
    private static final String CORE = "TCAPI_CORE", SECRET = "TCAPI_SECRET", BOUGHT = "TCAPI_BOUGHT", SIBLING = "TCAPI_SIBLING";

    private AddonResearchRuntimeChecks() {}

    public static void addonResearchJoinsGameplay(GameTestHelper test, ServerPlayer player) {
        PlayerKnowledge knowledge = KnowledgeAccess.get(player);
        PlayerKnowledge baseline = knowledge.copy();
        List<ItemStack> inventory = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        ResearchItem nitor = ResearchCategories.getResearch("NITOR");
        String[] nitorSiblings = nitor.siblings;
        List<Object> recipes = new ArrayList<>();
        try {
            ShapedArcaneRecipe arcane = ThaumcraftApi.addArcaneCraftingRecipe(CORE, new ItemStack(Items.AMETHYST_CLUSTER),
                    new AspectList().add(Aspect.AIR, 5), "AA", "AA", 'A', new ItemStack(Items.AMETHYST_SHARD));
            CrucibleRecipe crucible = ThaumcraftApi.addCrucibleRecipe(CORE, new ItemStack(Items.ECHO_SHARD),
                    new ItemStack(Items.AMETHYST_SHARD), new AspectList().add(Aspect.MAGIC, 2));
            recipes.add(arcane);
            recipes.add(crucible);
            ResearchCategories.registerCategory(CATEGORY,
                    ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/items/thaumonomiconcheat.png"),
                    ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchback.png"));
            new ResearchItem(CORE, CATEGORY, new AspectList().add(Aspect.MAGIC, 3).add(Aspect.AIR, 2), 0, 0, 2,
                    new ItemStack(Items.AMETHYST_SHARD)).setParents("CRUCIBLE")
                    .setPages(new ResearchPage("tc.research_page." + CORE + ".1"), new ResearchPage(arcane),
                            new ResearchPage(crucible), new ResearchPage(new AspectList().add(Aspect.MAGIC, 4)))
                    .registerResearchItem();
            ThaumcraftApi.addWarpToResearch(CORE, 2);
            new ResearchItem(SECRET, CATEGORY, new AspectList().add(Aspect.DARKNESS, 2), 2, 0, 1, new ItemStack(Items.SNIFFER_EGG))
                    .setParents(CORE).setHidden().setItemTriggers(new ItemStack(Items.SNIFFER_EGG))
                    .setPages(new ResearchPage("tc.research_page." + SECRET + ".1")).registerResearchItem();
            new ResearchItem(BOUGHT, CATEGORY, new AspectList().add(Aspect.MAGIC, 1), 0, 2, 1, new ItemStack(Items.ECHO_SHARD))
                    .setSecondary().setParents(CORE).registerResearchItem();
            new ResearchItem(SIBLING, CATEGORY).registerResearchItem();
            // A 1.7.10 add-on could change an original entry through its public view.
            nitor.setSiblings(SIBLING);
            AddonResearch.refresh();

            var core = ResearchCatalog.get(CORE);
            test.assertTrue(core != null && core.category().equals(CATEGORY), "the add-on entry is in the live catalogue");
            test.assertValueEqual(core.aspectCost(), Map.of("praecantatio", 3, "aer", 2), "the cost is the entry's aspect list");
            test.assertValueEqual(core.parents(), List.of("CRUCIBLE"), "a parent of the original tree");
            test.assertTrue(!ResearchCatalog.isBundled(CORE) && ResearchCatalog.original(CORE) == null, "an add-on key is not an original key");
            test.assertValueEqual(ResearchCatalog.get("NITOR").siblings(), List.of(SIBLING), "the changed original entry is live");
            test.assertValueEqual(ResearchCatalog.original("NITOR").siblings(), List.of(), "the original corpus is unchanged");

            knowledge.reset();
            knowledge.setInitializedAspects(true);
            knowledge.addResearch("CRUCIBLE");
            var completed = knowledge.getResearchComplete();
            List<String> tabs = ResearchBrowserData.categories(completed);
            test.assertTrue(tabs.indexOf(CATEGORY) > tabs.indexOf("GOLEMANCY"), "the add-on tab follows the original ones: " + tabs);
            test.assertTrue(ResearchBrowserData.entries(CATEGORY, completed).stream().anyMatch(entry -> entry.key().equals(CORE)),
                    "the entry is drawn once its parent is complete");
            test.assertTrue(ResearchBrowserData.entries(CATEGORY, completed).stream().noneMatch(entry -> entry.key().equals(SECRET)),
                    "a hidden entry stays hidden");

            // The research table writes a note of the add-on entry.
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
            player.getInventory().setItem(0, new ItemStack(Items.PAPER, 4));
            player.getInventory().setItem(1, new ItemStack(ThaumcraftContent.INKWELL.get()));
            var issued = ResearchNoteIssuance.issue(player, CORE, new Random(42));
            test.assertTrue(issued.success(), "a research note of the add-on entry is issued: " + issued);
            boolean noted = false;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                var data = stack.is(ThaumcraftContent.RESEARCH_NOTE.get()) ? ItemResearchNotes.data(stack) : null;
                if (data != null && data.key().equals(CORE) && !data.cells().isEmpty()) noted = true;
            }
            test.assertTrue(noted, "the note carries the add-on key and a hex grid");

            // Completion through the public API pays the registered warp, like the original.
            test.assertTrue(ThaumcraftApiHelper.completeResearch(player, CORE), "the API completes the add-on entry");
            test.assertTrue(ThaumcraftApiHelper.isResearchComplete(player, CORE), "completion is visible through the API");
            test.assertTrue(ThaumcraftApiHelper.getCompletedResearch(player).contains(CORE), "and in the completed set");
            if (!thaumcraft.common.lib.WarpEvents.isWussMode()) {
                test.assertValueEqual(ThaumcraftApiHelper.getWarpPermanent(player) + ThaumcraftApiHelper.getWarpSticky(player), 2,
                        "the registered warp of the add-on entry is paid");
            }
            test.assertValueEqual(java.util.Arrays.asList(ThaumcraftApi.getCraftingRecipeKey(player, new ItemStack(Items.AMETHYST_CLUSTER))),
                    List.<Object>of(CORE, 1), "the arcane recipe object of page 1 is the click-through target");
            test.assertValueEqual(java.util.Arrays.asList(ThaumcraftApi.getCraftingRecipeKey(player, new ItemStack(Items.ECHO_SHARD))),
                    List.<Object>of(CORE, 2), "the crucible recipe object of page 2 is the click-through target");

            // Scanning the trigger item reveals the clue of the hidden entry.
            test.assertTrue(ResearchRegistrationOrder.enabled().stream().anyMatch(entry -> entry.key().equals(SECRET)),
                    "the hidden entry joins the clue draw order");
            String clue = ResearchClues.reveal(knowledge, ResearchClues.Context.item(new ItemStack(Items.SNIFFER_EGG)), Map.of(), bound -> 0);
            test.assertValueEqual(clue, SECRET, "the item trigger of the add-on entry reveals it");
            test.assertTrue(knowledge.isResearchComplete("@" + SECRET), "as a clue, not as completed research");
            test.assertTrue(ResearchClues.reveal(knowledge, ResearchClues.Context.item(new ItemStack(Items.EGG)), Map.of(), bound -> 0) == null,
                    "another item is no trigger");

            // A secondary entry is bought with research points.
            knowledge.setAspectPool("praecantatio", 1);
            var bought = ResearchPurchase.purchase(knowledge, BOUGHT);
            test.assertTrue(bought.success(), "the secondary add-on entry is bought: " + bought);
            test.assertValueEqual(knowledge.getAspectPoolFor("praecantatio"), 0, "its price is spent");

            // Completing the changed original entry completes its new sibling.
            test.assertTrue(ThaumcraftApiHelper.completeResearch(player, "NITOR"), "the original entry completes");
            test.assertTrue(knowledge.isResearchComplete(SIBLING), "with the sibling an add-on gave it");

            // The knowledge of a player keeps add-on keys through a save.
            PlayerKnowledge reloaded = new PlayerKnowledge();
            reloaded.load(knowledge.save());
            test.assertTrue(reloaded.isResearchComplete(CORE) && reloaded.isResearchComplete(BOUGHT), "add-on keys survive a save");
        } finally {
            nitor.siblings = nitorSiblings;
            var list = ResearchCategories.researchCategories.remove(CATEGORY);
            if (list != null) list.research.clear();
            for (Object recipe : recipes) ThaumcraftApi.unregisterCraftingRecipe(recipe);
            ThaumcraftApi.addWarpToResearch(CORE, 0);
            AddonResearch.refresh();
            knowledge.copyFrom(baseline);
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
        }
        test.assertTrue(ResearchCatalog.get(CORE) == null, "removing the registration removes the entry");
        test.assertTrue(ResearchCatalog.get("NITOR") == ResearchCatalog.original("NITOR"), "restoring the view restores the original record");
        test.succeed();
    }
}
