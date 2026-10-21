package thaumcraft.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ArcaneRecipes;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.InfusionEnchantmentRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.NativeRecipes;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.api.internal.IInternalMethodHandler;
import thaumcraft.api.internal.ThaumcraftInternalMethods;
import thaumcraft.api.research.IScanEventHandler;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.lib.research.EntityAspectBindings;
import thaumcraft.common.lib.research.ObjectAspectCatalog;
import thaumcraft.common.lib.research.ObjectAspectRegistry;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchDefinition;
import thaumcraft.common.lib.research.ResearchRecipeCoverage;

/**
 * The public registration facade of the native port.
 *
 * <p>It publishes the original scan/object-tag registries, the original materials, the original recipe,
 * smelting-bonus and warp registries and the original plug point between the API and the gameplay
 * internals. Every answer is read from the native implementation; nothing is copied.</p>
 */
public final class ThaumcraftApi {
    /**
     * Original TC4 tool materials.
     *
     * <p>Forced 1.21.1 change: {@code Item.ToolMaterial} no longer exists, so the three original materials are
     * the native {@link Tier} values that the registered tools already use. The original numbers are unchanged:
     * thaumium 3/400/7.0/2.0/22, void 4/150/8.0/3.0/10, elemental 3/1500/10.0/3.0/18.</p>
     */
    public static Tier toolMatThaumium = ThaumcraftEquipment.THAUMIUM_TIER;
    public static Tier toolMatVoid = ThaumcraftEquipment.VOID_TIER;
    public static Tier toolMatElemental = ThaumcraftEquipment.ELEMENTAL_TIER;
    /** Original armor materials; 1.21.1 registers them, so they are resolved through accessors. */
    public static ArmorMaterial armorMatThaumium() { return ThaumcraftEquipment.THAUMIUM_ARMOR_MATERIAL.get(); }
    public static ArmorMaterial armorMatThaumiumFortress() { return ThaumcraftEquipment.FORTRESS_ARMOR_MATERIAL.get(); }
    public static ArmorMaterial armorMatVoid() { return ThaumcraftEquipment.VOID_ARMOR_MATERIAL.get(); }
    /** Original enchantment ids. 1.21.1 has no numeric enchantment ids; -1 means "not present in this runtime". */
    public static int enchantFrugal = -1;
    public static int enchantPotency = -1;
    public static int enchantWandFortune = -1;
    public static int enchantHaste = -1;
    public static int enchantRepair = -1;
    /** Original portable hole blacklist; the authoritative native list is the block tag of the same name. */
    public static final ArrayList<Block> portableHoleBlackList = new ArrayList<>();
    /** The original plug point between the public API and the gameplay internals. */
    public static IInternalMethodHandler internalMethods = new ThaumcraftInternalMethods();
    public static ArrayList<IScanEventHandler> scanEventhandlers = new ArrayList<>();
    public static ArrayList<EntityTags> scanEntities = new ArrayList<>(EntityAspectBindings.originalTags());
    public static ConcurrentHashMap<List<?>, AspectList> objectTags = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<List<?>, int[]> groupedObjectTags = new ConcurrentHashMap<>();
    /** Original programmatic recipe registry; the native datapack recipes live in the recipe manager. */
    private static final ArrayList<Object> craftingRecipes = new ArrayList<>();
    private static final Map<Object, ItemStack> smeltingBonus = new HashMap<>();
    private static final Map<Object, Integer> warpMap = new HashMap<>();
    private static final Map<List<?>, Object[]> keyCache = new HashMap<>();
    private ThaumcraftApi() {}

    // ------------------------------------------------------------------ smelting bonus

    /** Original bonus registry: the exact input identity pays the extra output. */
    public static void addSmeltingBonus(ItemStack in, ItemStack out) {
        if (in == null || in.isEmpty() || out == null || out.isEmpty()) return;
        smeltingBonus.put(List.of(in.getItem(), in.getDamageValue()), out.copy());
    }

    /** Original ore-name shortcut. 1.21.1 has no ore dictionary, so the name is a native item tag path. */
    public static void addSmeltingBonus(String in, ItemStack out) {
        if (in == null || out == null || out.isEmpty()) return;
        smeltingBonus.put(in, out.copy());
    }

    public static ItemStack getSmeltingBonus(ItemStack in) {
        if (in == null || in.isEmpty()) return null;
        ItemStack out = smeltingBonus.get(List.of(in.getItem(), in.getDamageValue()));
        if (out == null) out = smeltingBonus.get(List.of(in.getItem(), Short.MAX_VALUE));
        return out == null ? null : out.copy();
    }

    // ------------------------------------------------------------------ crafting recipes

    /**
     * The original programmatic recipe list and the native recipes of the loaded level.
     *
     * <p>Forced 1.21.1 change: the original kept every recipe of the game in one static list; 1.21.1
     * keeps the recipes of a world in the recipe manager of the loaded level. The runtime installs that
     * manager through {@link #installRecipeManager(RecipeManager, net.minecraft.core.HolderLookup.Provider)}
     * and this list answers from both.</p>
     */
    public static List<Object> getCraftingRecipes() {
        List<Object> result = new ArrayList<>(craftingRecipes);
        result.addAll(NativeRecipes.views());
        return result;
    }

    /** The runtime installs the recipe manager of the level it loaded; the cached answers belong to it. */
    public static void installRecipeManager(RecipeManager manager, net.minecraft.core.HolderLookup.Provider registries) {
        NativeRecipes.install(manager, registries);
        keyCache.clear();
    }

    public static ShapedArcaneRecipe addArcaneCraftingRecipe(String research, ItemStack result, AspectList aspects, Object... recipe) {
        ShapedArcaneRecipe r = new ShapedArcaneRecipe(research, result, aspects, recipe);
        craftingRecipes.add(r);
        return r;
    }

    public static ShapelessArcaneRecipe addShapelessArcaneCraftingRecipe(String research, ItemStack result, AspectList aspects, Object... recipe) {
        ShapelessArcaneRecipe r = new ShapelessArcaneRecipe(research, result, aspects, recipe);
        craftingRecipes.add(r);
        return r;
    }

    /** Original guard: only an item stack or an object array may be an infusion output. */
    public static InfusionRecipe addInfusionCraftingRecipe(String research, Object result, int instability, AspectList aspects, ItemStack input, ItemStack[] recipe) {
        if (!(result instanceof ItemStack) && !(result instanceof Object[])) return null;
        InfusionRecipe r = new InfusionRecipe(research, result, instability, aspects, input, recipe);
        craftingRecipes.add(r);
        return r;
    }

    public static InfusionEnchantmentRecipe addInfusionEnchantmentRecipe(String research, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment, int instability, AspectList aspects, ItemStack[] recipe) {
        if (enchantment == null) return null;
        InfusionEnchantmentRecipe r = new InfusionEnchantmentRecipe(research, enchantment, instability, aspects, recipe);
        craftingRecipes.add(r);
        return r;
    }

    public static InfusionRecipe getInfusionRecipe(ItemStack res) {
        for (Object r : getCraftingRecipes()) {
            if (!(r instanceof InfusionRecipe recipe)) continue;
            Object output = recipe.getRecipeOutput();
            if (!(output instanceof ItemStack stack) || !sameOutput(stack, res)) continue;
            return recipe;
        }
        return null;
    }

    /** The original stack identity: the same item and the same damage. */
    private static boolean sameOutput(ItemStack produced, ItemStack wanted) {
        if (produced == null || wanted == null) return false;
        return produced.getItem() == wanted.getItem() && produced.getDamageValue() == wanted.getDamageValue();
    }

    public static CrucibleRecipe addCrucibleRecipe(String key, ItemStack result, Object catalyst, AspectList tags) {
        CrucibleRecipe rc = new CrucibleRecipe(key, result, catalyst, tags);
        craftingRecipes.add(rc);
        return rc;
    }

    public static CrucibleRecipe getCrucibleRecipe(ItemStack stack) {
        for (Object r : getCraftingRecipes()) {
            if (!(r instanceof CrucibleRecipe recipe) || !sameOutput(recipe.getRecipeOutput(), stack)) continue;
            return recipe;
        }
        return null;
    }

    public static CrucibleRecipe getCrucibleRecipeFromHash(int hash) {
        for (Object r : getCraftingRecipes()) {
            if (!(r instanceof CrucibleRecipe recipe) || recipe.hash != hash) continue;
            return recipe;
        }
        return null;
    }

    /**
     * Original research lookup of the recipe that produces a stack.
     *
     * <p>Forced 1.21.1 change: the original scanned the research pages for a recipe object; the native
     * corpus keeps the recipe references as keys, and the page is resolved through the native recipe of
     * the recipe manager. The answer is the research key and the index of its page, exactly as the
     * original returned them.</p>
     */
    public static Object[] getCraftingRecipeKey(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return null;
        ResearchCatalog.loadBundled();
        List<?> key = List.of(stack.getItem(), stack.getDamageValue());
        if (keyCache.containsKey(key)) {
            Object[] cached = keyCache.get(key);
            return cached == null ? null : researchVisible(player, cached);
        }
        for (ResearchDefinition definition : ResearchCatalog.all()) {
            List<ResearchDefinition.Page> pages = definition.pages();
            for (int index = 0; index < pages.size(); index++) {
                for (String recipeKey : pageRecipeKeys(definition, pages.get(index))) {
                    ItemStack output = nativeResult(recipeKey);
                    if (output == null || !sameOutput(output, stack)) continue;
                    Object[] found = new Object[]{definition.key(), index};
                    keyCache.put(key, found);
                    return researchVisible(player, found);
                }
            }
        }
        keyCache.put(key, null);
        return null;
    }

    /** The original recipe keys a research page shows: its own recipe, or the array of the original page. */
    private static List<String> pageRecipeKeys(ResearchDefinition definition, ResearchDefinition.Page page) {
        if (page.recipeKey() != null) return List.of(page.recipeKey());
        List<String> array = ResearchRecipeCoverage.pageRecipeArrays().get(definition.key());
        if (array == null || array.isEmpty()) return List.of();
        // The original page that shows an ARRAY of recipes (ConfigResearch:194-199).
        String expression = page.javaExpression();
        if (expression == null || !expression.endsWith(".toArray(new IArcaneRecipe[0])")) return List.of();
        return array;
    }

    /** The result of the native recipe of an original recipe key. */
    private static ItemStack nativeResult(String recipeKey) {
        String id = ResearchRecipeCoverage.nativeRecipeId(recipeKey);
        if (id == null) return null;
        Recipe<?> recipe = NativeRecipes.byId(ResourceLocation.parse(id));
        if (recipe == null) return null;
        ItemStack result = recipe.getResultItem(ArcaneRecipes.registries());
        return result.isEmpty() ? null : result;
    }

    private static Object[] researchVisible(net.minecraft.world.entity.player.Player player, Object[] found) {
        String research = (String) found[0];
        return ThaumcraftApiHelper.isResearchComplete(player, research) ? found : null;
    }

    // ------------------------------------------------------------------ warp

    public static void addWarpToItem(ItemStack craftresult, int amount) {
        if (craftresult == null || craftresult.isEmpty()) return;
        warpMap.put(List.of(craftresult.getItem(), craftresult.getDamageValue()), amount);
    }

    public static void addWarpToResearch(String research, int amount) {
        if (research == null) return;
        warpMap.put(research, amount);
    }

    public static int getWarp(Object in) {
        if (in == null) return 0;
        if (in instanceof ItemStack stack) {
            Integer value = warpMap.get(List.of(stack.getItem(), stack.getDamageValue()));
            return value == null ? 0 : value;
        }
        if (in instanceof String research) {
            Integer value = warpMap.get(research);
            return value == null ? 0 : value;
        }
        return 0;
    }

    /** The native block tag is authoritative; this mirrors it for the original public list. */
    public static void refreshPortableHoleBlackList() {
        portableHoleBlackList.clear();
        var tag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("thaumcraft", "portable_hole_blacklist"));
        BuiltInRegistries.BLOCK.getTag(tag).ifPresent(holders -> holders.forEach(holder -> portableHoleBlackList.add(holder.value())));
    }

    public static void registerScanEventhandler(IScanEventHandler handler) { scanEventhandlers.add(Objects.requireNonNull(handler, "handler")); }
    public static void registerEntityTag(String entityName, AspectList aspects, EntityTagsNBT... nbt) {
        if (entityName == null || entityName.isEmpty()) throw new IllegalArgumentException("An entity tag requires its original or native registry name");
        scanEntities.add(new EntityTags(entityName, aspects, nbt));
    }

    /** Item identity and native damage metadata only: count, names and other NBT/components never narrow this registration. */
    public static void registerObjectTag(ItemStack item, AspectList aspects) {
        if (item == null || item.isEmpty()) return;
        registerObjectTag(item.getItem(), item.getDamageValue(), aspects);
    }
    public static void registerObjectTag(Item item, int metadata, AspectList aspects) {
        ObjectAspectRegistry.register(objectTags, item, metadata, aspects);
    }
    public static void registerObjectTag(ResourceLocation item, int metadata, AspectList aspects) {
        if (!BuiltInRegistries.ITEM.containsKey(item)) throw new IllegalArgumentException("Unknown native object-tag item: " + item);
        registerObjectTag(BuiltInRegistries.ITEM.get(item), metadata, aspects);
    }
    public static void registerObjectTag(ItemStack item, int[] metadata, AspectList aspects) {
        if (item == null || item.isEmpty()) return;
        ObjectAspectRegistry.register(objectTags, groupedObjectTags, item.getItem(), metadata, aspects);
    }
    /** Current native tag membership is snapshotted immediately, as original OreDictionary membership was. */
    public static void registerObjectTag(TagKey<Item> tag, AspectList aspects) {
        AspectList shared = aspects == null ? new AspectList() : aspects;
        BuiltInRegistries.ITEM.getTag(tag).ifPresent(items -> items.forEach(item -> registerObjectTag(item.value(), 0, shared)));
    }
    /** Native namespaced item tags replace the unavailable Forge OreDictionary; old ore names require an explicit caller mapping. */
    public static void registerObjectTag(String nativeItemTag, AspectList aspects) {
        ResourceLocation id = ResourceLocation.tryParse(nativeItemTag);
        if (id == null || !nativeItemTag.contains(":"))
            throw new IllegalArgumentException("Use a native namespaced item tag or TagKey<Item>, not a legacy OreDictionary name: " + nativeItemTag);
        registerObjectTag(TagKey.create(Registries.ITEM, id), aspects);
    }
    public static boolean exists(Item item, int metadata) {
        return ObjectAspectRegistry.exists(objectTags, item, metadata)
                || ObjectAspectCatalog.hasDerivedRegistration(BuiltInRegistries.ITEM.getKey(item), metadata);
    }
    public static void registerComplexObjectTag(ItemStack item, AspectList aspects) {
        registerComplexObjectTag(item, aspects, ObjectAspectRegistry.requireContext());
    }
    public static void registerComplexObjectTag(ItemStack item, AspectList aspects, Level world) {
        if (item == null || item.isEmpty()) throw new IllegalArgumentException("Complex object tags require an item");
        registerComplexObjectTag(item, item.getDamageValue(), aspects, world);
    }
    public static void registerComplexObjectTag(Item item, int metadata, AspectList aspects, Level world) {
        if (item == null) throw new IllegalArgumentException("Complex object tags require an item");
        registerComplexObjectTag(new ItemStack(item), metadata, aspects, world);
    }
    private static void registerComplexObjectTag(ItemStack item, int metadata, AspectList aspects, Level world) {
        var level = ObjectAspectRegistry.requireContext(world);
        Objects.requireNonNull(aspects, "Complex object-tag additions");
        ObjectAspectCatalog.initializePublicTags();
        AspectList result = ObjectAspectRegistry.complex(exists(item.getItem(), metadata), aspects,
                () -> ObjectAspectResolver.baseTags(item, level, metadata), () -> ObjectAspectResolver.generatedTags(item.getItem(), level));
        registerObjectTag(item.getItem(), metadata, result);
    }

    /** Original public registration keeps the supplied stack reference and silently ignores unknown rarities. */
    public static void addLootBagItem(ItemStack item, int weight, int... bagTypes) {
        if (bagTypes == null || bagTypes.length == 0) {
            thaumcraft.api.internal.WeightedRandomLoot.lootBagCommon.add(new thaumcraft.api.internal.WeightedRandomLoot(item, weight));
            return;
        }
        for (int rarity : bagTypes) {
            var entry = new thaumcraft.api.internal.WeightedRandomLoot(item, weight);
            switch (rarity) {
                case 0 -> thaumcraft.api.internal.WeightedRandomLoot.lootBagCommon.add(entry);
                case 1 -> thaumcraft.api.internal.WeightedRandomLoot.lootBagUncommon.add(entry);
                case 2 -> thaumcraft.api.internal.WeightedRandomLoot.lootBagRare.add(entry);
                default -> { }
            }
        }
    }

    /** Fields/references remain mutable as in the original API; a resolved scan copies the selected aspect list. */
    public static class EntityTags {
        public String entityName;
        public EntityTagsNBT[] nbts;
        public AspectList aspects;
        public EntityTags(String entityName, AspectList aspects, EntityTagsNBT... nbts) {
            this.entityName = entityName; this.aspects = aspects; this.nbts = nbts;
        }
    }
    public static class EntityTagsNBT {
        public String name;
        public Object value;
        public EntityTagsNBT(String name, Object value) { this.name = name; this.value = value; }
    }
}
