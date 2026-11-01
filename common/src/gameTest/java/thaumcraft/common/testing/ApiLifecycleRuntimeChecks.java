package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.GameType;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.NativeRecipes;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ObjectAspectCatalog;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/** Public API behavior against registered items, the native recipe manager and real item-tag binding. */
public final class ApiLifecycleRuntimeChecks {
    private ApiLifecycleRuntimeChecks() {}

    public static void craftingVisPaysWholeUnitsAndCapPrice(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        AspectList whole = new AspectList().add(Aspect.AIR, 10);
        ItemStack gold = wand("gold", Map.of("aer", 5000));
        test.assertTrue(ThaumcraftApiHelper.consumeVisFromWandCrafting(gold, player, whole, false), "the quote can be paid");
        test.assertValueEqual(ItemWandCasting.getVis(gold, "aer"), 5000, "quoting never spends vis");
        test.assertTrue(ThaumcraftApiHelper.consumeVisFromWandCrafting(gold, player, whole, true), "the gold-capped wand pays");
        test.assertValueEqual(ItemWandCasting.getVis(gold, "aer"), 4000, "ten whole vis costs 1000 centivis");

        ItemStack iron = wand("iron", Map.of("aer", 5000));
        test.assertTrue(ThaumcraftApiHelper.consumeVisFromWandCrafting(iron, player, whole, true), "the iron-capped wand pays");
        test.assertValueEqual(ItemWandCasting.getVis(iron, "aer"), 3900, "the original iron cap increases crafting cost by ten percent");
        test.assertTrue(ThaumcraftApiHelper.consumeVisFromWand(iron, player, new AspectList().add(Aspect.AIR, 100), true, false),
                "the other public helper accepts centivis");
        test.assertValueEqual(ItemWandCasting.getVis(iron, "aer"), 3790, "the centivis helper also applies the cap price");

        ItemStack insufficient = wand("gold", Map.of("aer", 5000, "ignis", 999));
        test.assertTrue(!ThaumcraftApiHelper.consumeVisFromWandCrafting(insufficient, player,
                new AspectList().add(Aspect.AIR, 10).add(Aspect.FIRE, 10), true), "all aspects must be affordable");
        test.assertValueEqual(ItemWandCasting.getVis(insufficient, "aer"), 5000, "failure preserves the affordable aspect too");
        test.assertValueEqual(ItemWandCasting.getVis(insufficient, "ignis"), 999, "failure preserves the insufficient aspect");
        test.succeed();
    }

    private static ItemStack wand(String cap, Map<String, Integer> vis) {
        ItemStack stack = new ItemStack(ThaumcraftContent.WAND.get());
        ItemWandCasting.withData(stack, new WandData("greatwood", cap, vis));
        return stack;
    }

    public static void registeredEquipmentKeepsOriginalAspectBonuses(GameTestHelper test) {
        bonus(test, ThaumcraftEquipment.THAUMIUM_SWORD.get(), Aspect.WEAPON, 3);
        bonus(test, ThaumcraftEquipment.ELEMENTAL_SWORD.get(), Aspect.WEAPON, 4);
        bonus(test, ThaumcraftEquipment.VOID_SWORD.get(), Aspect.WEAPON, 4);
        bonus(test, ThaumcraftLegacyItems.CRIMSON_SWORD.get(), Aspect.WEAPON, 4);
        bonus(test, ThaumcraftEquipment.VOID_PICKAXE.get(), Aspect.MINE, 5);
        bonus(test, ThaumcraftEquipment.VOID_AXE.get(), Aspect.TOOL, 5);
        bonus(test, ThaumcraftEquipment.VOID_SHOVEL.get(), Aspect.TOOL, 5);
        bonus(test, ThaumcraftLegacyItems.PRIMAL_CRUSHER.get(), Aspect.TOOL, 6);
        bonus(test, ThaumcraftEquipment.ELEMENTAL_HOE.get(), Aspect.HARVEST, 4);
        bonus(test, ThaumcraftEquipment.VOID_HOE.get(), Aspect.HARVEST, 3);
        for (Item item : List.of(ThaumcraftEquipment.ELEMENTAL_HOE.get(), ThaumcraftEquipment.VOID_HOE.get())) {
            test.assertValueEqual(ObjectAspectResolver.bonusTags(new ItemStack(item)).getAmount(Aspect.TOOL), 0,
                    "hoes use their original ItemHoe branch, even though the modern class extends DiggerItem");
        }
        test.succeed();
    }

    private static void bonus(GameTestHelper test, Item item, Aspect aspect, int amount) {
        test.assertValueEqual(ObjectAspectResolver.bonusTags(new ItemStack(item)).getAmount(aspect), amount,
                BuiltInRegistries.ITEM.getKey(item) + " original bonus " + aspect.getTag());
    }

    public static void recipeLookupRefreshesWhenTheSameManagerReloads(GameTestHelper test) {
        var manager = test.getLevel().getRecipeManager();
        var recipes = new ArrayList<>(manager.getRecipes());
        var previousManager = NativeRecipes.manager();
        var previousRegistries = NativeRecipes.registries();
        var player = test.makeMockServerPlayerInLevel();
        KnowledgeAccess.get(player).addResearch("NITOR");
        try {
            manager.replaceRecipes(List.of());
            ThaumcraftApi.installRecipeManager(manager, test.getLevel().registryAccess());
            test.assertTrue(ThaumcraftApi.getCraftingRecipeKey(player, new ItemStack(Items.DIAMOND)) == null,
                    "cache a miss before the recipe exists");
            manager.replaceRecipes(List.of(nitor(Items.DIAMOND)));
            Object[] found = ThaumcraftApi.getCraftingRecipeKey(player, new ItemStack(Items.DIAMOND));
            test.assertTrue(found != null && found[0].equals("NITOR") && found[1].equals(1),
                    "a same-manager reload invalidates a cached miss");
            found[0] = "MUTATED";
            test.assertValueEqual(ThaumcraftApi.getCraftingRecipeKey(player, new ItemStack(Items.DIAMOND))[0], "NITOR",
                    "cached answers are not mutable through the returned array");
            manager.replaceRecipes(List.of(nitor(Items.EMERALD)));
            test.assertTrue(ThaumcraftApi.getCraftingRecipeKey(player, new ItemStack(Items.DIAMOND)) == null,
                    "a same-manager reload also retires cached successful results");
            Object[] replacement = ThaumcraftApi.getCraftingRecipeKey(player, new ItemStack(Items.EMERALD));
            test.assertTrue(replacement != null && replacement[0].equals("NITOR"), "the replacement output is visible without a manual cache clear");
        } finally {
            manager.replaceRecipes(recipes);
            ThaumcraftApi.installRecipeManager(previousManager, previousRegistries);
        }
        test.succeed();
    }

    private static RecipeHolder<ShapelessRecipe> nitor(Item output) {
        return new RecipeHolder<>(ResourceLocation.parse("thaumcraft:crucible/nitor"),
                new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(output),
                        NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.STONE))));
    }

    public static void tagAspectsFollowBindingAndPreserveExplicitOverrides(GameTestHelper test) {
        Map<TagKey<Item>, List<Holder<Item>>> originalTags = new HashMap<>();
        BuiltInRegistries.ITEM.getTags().forEach(pair -> originalTags.put(pair.getFirst(), pair.getSecond().stream().toList()));
        ObjectAspectCatalog.initializePublicTags();
        ThaumcraftApi.clearTagObjectTags();
        var originalObjects = ThaumcraftApi.objectTags;
        TagKey<Item> first = TagKey.create(Registries.ITEM, ResourceLocation.parse("thaumcraft:runtime_test/aspect_first"));
        TagKey<Item> later = TagKey.create(Registries.ITEM, ResourceLocation.parse("thaumcraft:runtime_test/aspect_later"));
        AspectList air = new AspectList().add(Aspect.AIR, 7);
        AspectList fire = new AspectList().add(Aspect.FIRE, 9);
        var stick = List.of(Items.STICK, 0);
        var diamond = List.of(Items.DIAMOND, 0);
        try {
            // Isolate the public extension map while exercising the actual native item registry and binder mixin.
            ThaumcraftApi.objectTags = new ConcurrentHashMap<>();
            Map<TagKey<Item>, List<Holder<Item>>> tags = new HashMap<>(originalTags);
            tags.put(first, List.of(Items.STICK.builtInRegistryHolder()));
            BuiltInRegistries.ITEM.bindTags(tags);
            ThaumcraftApi.registerObjectTag(first, air);
            test.assertTrue(ThaumcraftApi.objectTags.get(stick) == air, "a late tag registration applies current members");

            tags.put(first, List.of(Items.DIAMOND.builtInRegistryHolder()));
            BuiltInRegistries.ITEM.bindTags(tags);
            test.assertTrue(!ThaumcraftApi.objectTags.containsKey(stick) && ThaumcraftApi.objectTags.get(diamond) == air,
                    "native tag rebind removes old members and adds new members without a manual refresh");
            ThaumcraftApi.registerObjectTag(later, fire);
            tags.put(later, List.of(Items.DIAMOND.builtInRegistryHolder()));
            BuiltInRegistries.ITEM.bindTags(tags);
            test.assertTrue(ThaumcraftApi.objectTags.get(diamond) == fire, "later tag registrations win after rebind");
            tags.put(later, List.of());
            BuiltInRegistries.ITEM.bindTags(tags);
            test.assertTrue(ThaumcraftApi.objectTags.get(diamond) == air, "removing the later membership restores the earlier tag");

            // Reuse the exact expanded list: an explicit API call must transfer ownership even in this case.
            ThaumcraftApi.registerObjectTag(Items.DIAMOND, 0, air);
            ThaumcraftApi.clearTagObjectTags();
            test.assertTrue(ThaumcraftApi.objectTags.get(diamond) == air, "world cleanup retains explicit add-on registrations");
            tags.put(first, List.of(Items.STICK.builtInRegistryHolder()));
            BuiltInRegistries.ITEM.bindTags(tags);
            test.assertTrue(!ThaumcraftApi.objectTags.containsKey(stick), "a detached world's tag rebind does not repopulate derived state");
            ThaumcraftApi.applyTagObjectTags();
            test.assertTrue(ThaumcraftApi.objectTags.get(stick) == air && ThaumcraftApi.objectTags.get(diamond) == air,
                    "the next world expands current members while retaining the explicit item override");
            AspectList rawOverride = new AspectList().add(Aspect.EARTH, 12);
            ThaumcraftApi.objectTags.put(stick, rawOverride);
            ThaumcraftApi.clearTagObjectTags();
            test.assertTrue(ThaumcraftApi.objectTags.get(stick) == rawOverride, "a raw-map override also survives cleanup");
        } finally {
            ThaumcraftApi.clearTagObjectTags();
            ThaumcraftApi.objectTags = originalObjects;
            BuiltInRegistries.ITEM.bindTags(originalTags);
            ThaumcraftApi.applyTagObjectTags();
        }
        test.succeed();
    }
}
