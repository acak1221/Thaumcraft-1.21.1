package thaumcraft.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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
    /** Resolves the registered fortress armor material; call after material registration, with no mutation. */
    public static ArmorMaterial armorMatThaumiumFortress() { return ThaumcraftEquipment.FORTRESS_ARMOR_MATERIAL.get(); }
    /** Resolves the registered void armor material; call after material registration, with no mutation. */
    public static ArmorMaterial armorMatVoid() { return ThaumcraftEquipment.VOID_ARMOR_MATERIAL.get(); }
    /**
     * Original {@code armorMatSpecial} (oracle {@code thaumcraft/api/ThaumcraftApi.java:52}):
     * durability 25, the defenses 1/3/2/1 and the enchantability 25. The original published it as a
     * field; 1.21.1 registers the material, so the native answers through an accessor.
     */
    public static ArmorMaterial armorMatSpecial() { return ThaumcraftMaterials.ARMORMAT_SPECIAL; }
    /**
     * Original {@code armorMatVoidFortress} (oracle {@code thaumcraft/api/ThaumcraftApi.java:55}):
     * durability 18, the defenses 4/8/7/4 and the enchantability 10. The original published it as a
     * field; 1.21.1 registers the material, so the native answers through an accessor.
     */
    public static ArmorMaterial armorMatVoidFortress() { return ThaumcraftMaterials.ARMORMAT_VOID_FORTRESS; }
    /**
     * Original enchantment ids. 1.21.1 has no numeric enchantment ids; -1 means "not present in this
     * runtime".
     *
     * <p>Forced 1.21.1 change: an enchantment of 1.21.1 is a data pack record whose identity is its
     * registry key, so the original ids have no counterpart. The identity of every original enchantment is
     * published as a {@link ResourceKey} next to these fields: {@link #ENCHANTMENT_FRUGAL},
     * {@link #ENCHANTMENT_POTENCY}, {@link #ENCHANTMENT_WAND_FORTUNE}, {@link #ENCHANTMENT_HASTE} and
     * {@link #ENCHANTMENT_REPAIR}.</p>
     */
    /** Mutable legacy 1.7.10 numeric enchantment sentinel, initially -1; no native identity is assigned.
     * @deprecated Resolve {@link #ENCHANTMENT_FRUGAL} through the level's enchantment registry in 1.21.1. */
    @Deprecated
    public static int enchantFrugal = -1;
    /** Mutable legacy 1.7.10 numeric enchantment sentinel, initially -1; no native identity is assigned.
     * @deprecated Resolve {@link #ENCHANTMENT_POTENCY} through the level's enchantment registry in 1.21.1. */
    @Deprecated
    public static int enchantPotency = -1;
    /** Mutable legacy 1.7.10 numeric enchantment sentinel, initially -1; no native identity is assigned.
     * @deprecated Resolve {@link #ENCHANTMENT_WAND_FORTUNE} through the level's enchantment registry in 1.21.1. */
    @Deprecated
    public static int enchantWandFortune = -1;
    /** Mutable legacy 1.7.10 numeric enchantment sentinel, initially -1; no native identity is assigned.
     * @deprecated Resolve {@link #ENCHANTMENT_HASTE} through the level's enchantment registry in 1.21.1. */
    @Deprecated
    public static int enchantHaste = -1;
    /** Mutable legacy 1.7.10 numeric enchantment sentinel, initially -1; no native identity is assigned.
     * @deprecated Resolve {@link #ENCHANTMENT_REPAIR} through the level's enchantment registry in 1.21.1. */
    @Deprecated
    public static int enchantRepair = -1;
    /** The identity of the original frugal enchantment (oracle {@code Config.enchFrugal}). */
    public static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_FRUGAL = enchantmentKey("frugal");
    /** The identity of the original potency enchantment (oracle {@code Config.enchPotency}). */
    public static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_POTENCY = enchantmentKey("potency");
    /** The identity of the original wand fortune enchantment (oracle {@code Config.enchWandFortune}). */
    public static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_WAND_FORTUNE = enchantmentKey("wandfortune");
    /** The identity of the original haste enchantment (oracle {@code Config.enchHaste}). */
    public static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_HASTE = enchantmentKey("haste");
    /** The identity of the original repair enchantment (oracle {@code Config.enchRepair}). */
    public static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENT_REPAIR = enchantmentKey("repair");
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
    private static boolean originalSmeltingBonusesLoaded;
    private ThaumcraftApi() {}

    /** The registry key of one original enchantment of this mod. */
    private static ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantmentKey(String id) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("thaumcraft", id));
    }

    // ------------------------------------------------------------------ smelting bonus

    /**
     * Original bonus registry: the exact input identity pays the extra output.
     *
     * <p>Forced 1.21.1 change: the original stores {@code new ItemStack(out.getItem(), 0, out.getItemDamage())}
     * (oracle {@code thaumcraft/api/ThaumcraftApi.java:80-82}), so the registry amount is always zero: the
     * arcane furnace decides how many units it pays with its own bellows rolls (oracle
     * {@code thaumcraft/common/tiles/TileArcaneFurnace.java:237-259}). 1.21.1 cannot hold an amount-zero
     * stack as a real stack, because {@code ItemStack.copy()} of an amount-zero stack returns the shared
     * {@code ItemStack.EMPTY} and the entry would lose its item. The native registry therefore keeps the
     * amount the addon registered; the furnace still ignores it and pays its own amount.</p>
     */
    public static void addSmeltingBonus(ItemStack in, ItemStack out) {
        if (in == null || in.isEmpty() || out == null || out.isEmpty()) return;
        smeltingBonus.put(List.of(in.getItem(), in.getDamageValue()), out.copy());
    }

    /**
     * Original ore-name shortcut. 1.21.1 has no ore dictionary, so the name is an original ore name that
     * {@link thaumcraft.common.lib.utils.OriginalOreTags#firstOreName(ItemStack)} resolves through the
     * native item tags of the original ore names.
     */
    public static void addSmeltingBonus(String in, ItemStack out) {
        if (in == null || out == null || out.isEmpty()) return;
        smeltingBonus.put(in, out.copy());
    }

    /**
     * The original lookup: the exact (item, damage) entry, then the wildcard (item, 32767) entry, then the
     * entry of the FIRST original ore name the input belongs to (oracle
     * {@code thaumcraft/api/ThaumcraftApi.java:88-99}).
     *
     * <p>The original registrations of the catalogue {@code ConfigRecipes:493-510} live in the same
     * registry, so they are folded into it before the first answer.</p>
     */
    public static ItemStack getSmeltingBonus(ItemStack in) {
        if (in == null || in.isEmpty()) return null;
        loadOriginalSmeltingBonuses();
        ItemStack out = smeltingBonus.get(List.of(in.getItem(), in.getDamageValue()));
        if (out == null) out = smeltingBonus.get(List.of(in.getItem(), Short.MAX_VALUE));
        if (out == null) {
            String ore = thaumcraft.common.lib.utils.OriginalOreTags.firstOreName(in);
            if (ore != null) out = smeltingBonus.get(ore);
        }
        return out == null ? null : out.copy();
    }

    /**
     * Folds the original catalogue of {@code ConfigRecipes:493-510} into the registry.
     *
     * <p>Forced 1.21.1 change: the original registered its entries during the mod construction, when the
     * item registry already held the items. The native port parses the bundled corpus during the
     * bootstrap and resolves the items on the first lookup, so the registry stays one map and keeps the
     * original precedence: an exact entry wins over an ore-name entry. An addon entry that is already
     * registered is never overwritten.</p>
     */
    private static void loadOriginalSmeltingBonuses() {
        if (originalSmeltingBonusesLoaded) return;
        synchronized (ThaumcraftApi.class) {
            if (originalSmeltingBonusesLoaded) return;
            originalSmeltingBonusesLoaded = true;
            foldOriginalSmeltingBonuses();
        }
    }

    /**
     * Resolves the original catalogue again.
     *
     * <p>The tag based entries need the data pack tags of the level, which are bound after the data packs
     * are loaded. The server start hook calls this so the raw fish entries exist even when something asked
     * for a bonus before the tags were bound.</p>
     */
    public static void reloadOriginalSmeltingBonuses() {
        synchronized (ThaumcraftApi.class) {
            originalSmeltingBonusesLoaded = false;
            foldOriginalSmeltingBonuses();
            originalSmeltingBonusesLoaded = true;
        }
    }

    private static void foldOriginalSmeltingBonuses() {
        for (thaumcraft.common.lib.crafting.SmeltingBonusCatalog.Entry entry
                : thaumcraft.common.lib.crafting.SmeltingBonusCatalog.entries()) {
            Item item = thaumcraft.common.lib.crafting.SmeltingBonusCatalog.resolve(entry.output());
            if (item == null) continue;
            ItemStack out = new ItemStack(item);
            if (entry.isOre()) {
                if (!smeltingBonus.containsKey(entry.ore())) smeltingBonus.put(entry.ore(), out.copy());
                continue;
            }
            for (ItemStack input : thaumcraft.common.lib.crafting.SmeltingBonusCatalog.inputs(entry)) {
                Object key = List.of(input.getItem(), input.getDamageValue());
                if (!smeltingBonus.containsKey(key)) smeltingBonus.put(key, out.copy());
            }
        }
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

    /** Constructs and appends a live shaped recipe to the global programmatic list, returning it.
     * Costs are whole primal vis, not centivis; nullable research/aspects are retained, result is copied.
     * Null result/recipe throws NPE; malformed patterns/ingredients may throw before registration.
     * Register during coordinated initialization, not concurrently with recipe iteration. */
    public static ShapedArcaneRecipe addArcaneCraftingRecipe(String research, ItemStack result, AspectList aspects, Object... recipe) {
        ShapedArcaneRecipe r = new ShapedArcaneRecipe(research, result, aspects, recipe);
        craftingRecipes.add(r);
        return r;
    }

    /** Constructs and appends a live shapeless recipe to the global programmatic list, returning it.
     * Costs are whole primal vis, not centivis; nullable research/aspects are retained, result is copied.
     * Null result/recipe throws NPE and invalid/absent ingredients throw IAE before registration.
     * Register during coordinated initialization, not concurrently with recipe iteration. */
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

    /** Appends and returns a live enchantment recipe with essentia-unit costs and dimensionless instability.
     * Null enchantment returns null without registration; remaining copying/validation follows the recipe constructor.
     * Mutates global registration state; use coordinated initialization, not concurrent iteration. */
    public static InfusionEnchantmentRecipe addInfusionEnchantmentRecipe(String research, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment, int instability, AspectList aspects, ItemStack[] recipe) {
        if (enchantment == null) return null;
        InfusionEnchantmentRecipe r = new InfusionEnchantmentRecipe(research, enchantment, instability, aspects, recipe);
        craftingRecipes.add(r);
        return r;
    }

    /** Returns the first live infusion registration producing the same item/damage (not components/count), or null for null/no match; may initialize native views. */
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

    /** Constructs and globally registers a live crucible recipe with essentia-unit costs, returning it.
     * Retains result/tags references; null result/tags throws NPE, null/invalid catalyst throws IAE before registration.
     * Use coordinated initialization, not concurrent recipe iteration. */
    public static CrucibleRecipe addCrucibleRecipe(String key, ItemStack result, Object catalyst, AspectList tags) {
        CrucibleRecipe rc = new CrucibleRecipe(key, result, catalyst, tags);
        craftingRecipes.add(rc);
        return rc;
    }

    /** Returns the first live crucible registration producing the same item/damage (not components/count), or null for null/no match; may initialize native views. */
    public static CrucibleRecipe getCrucibleRecipe(ItemStack stack) {
        for (Object r : getCraftingRecipes()) {
            if (!(r instanceof CrucibleRecipe recipe) || !sameOutput(recipe.getRecipeOutput(), stack)) continue;
            return recipe;
        }
        return null;
    }

    /** Returns the first live crucible recipe with this dimensionless hash, or null; hashes are not guaranteed unique or persistent identities. */
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
     *
     * @return a caller-owned two-element array containing the research key ({@link String}) and
     *         zero-based page index ({@link Integer}), or null if no visible recipe matches;
     *         mutating the array never changes cached answers
     */
    public static Object[] getCraftingRecipeKey(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return null;
        ResearchCatalog.loadBundled();
        List<?> key = List.of(stack.getItem(), stack.getDamageValue());
        if (keyCache.containsKey(key)) {
            Object[] cached = keyCache.get(key);
            // The cached answer belongs to the registry: a caller may not edit it.
            return cached == null ? null : researchVisible(player, cached.clone());
        }
        for (ResearchDefinition definition : ResearchCatalog.all()) {
            List<ResearchDefinition.Page> pages = definition.pages();
            for (int index = 0; index < pages.size(); index++) {
                for (String recipeKey : pageRecipeKeys(definition, pages.get(index))) {
                    ItemStack output = nativeResult(recipeKey);
                    if (output == null || !sameOutput(output, stack)) continue;
                    Object[] found = new Object[]{definition.key(), index};
                    keyCache.put(key, found);
                    return researchVisible(player, found.clone());
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

    /** Replaces the item/damage registration with signed warp points; null/empty stacks are ignored and no stack reference is retained. */
    public static void addWarpToItem(ItemStack craftresult, int amount) {
        if (craftresult == null || craftresult.isEmpty()) return;
        warpMap.put(List.of(craftresult.getItem(), craftresult.getDamageValue()), amount);
    }

    /** Replaces the research registration with signed warp points; null keys are ignored, with no player mutation or packet. */
    public static void addWarpToResearch(String research, int amount) {
        if (research == null) return;
        warpMap.put(research, amount);
    }

    /** Returns registered/catalogue warp points for an item/damage or research key; null, unsupported values and missing entries return zero. */
    public static int getWarp(Object in) {
        if (in == null) return 0;
        if (in instanceof ItemStack stack) {
            Integer value = warpMap.get(List.of(stack.getItem(), stack.getDamageValue()));
            if (value != null) return value;
            // ConfigResearch's eight metadata outputs were flattened into distinct native item ids.
            // Explicit registrations (including zero) override this original catalogue.
            if (stack.isEmpty() || stack.getDamageValue() != 0) return 0;
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!id.getNamespace().equals("thaumcraft")) return 0;
            return switch (id.getPath()) {
                case "focus_hellbat", "compass_stone", "jar_brain", "bucket_death", "bottle_taint", "golem_flesh", "focus_primal", "staff_rod_primal" -> 1;
                default -> 0;
            };
        }
        if (in instanceof String research) {
            Integer value = warpMap.get(research);
            if (value != null) return value;
            var definition = ResearchCatalog.get(research);
            return definition == null ? 0 : definition.currentWarp();
        }
        return 0;
    }

    /** Native catalogue bridge: retain the extracted default unless the public API overrides it. */
    public static int getWarpForResearch(ResearchDefinition research) {
        return warpMap.getOrDefault(research.key(), research.warp());
    }

    /** The native block tag is authoritative; this mirrors it for the original public list. */
    public static void refreshPortableHoleBlackList() {
        portableHoleBlackList.clear();
        var tag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("thaumcraft", "portable_hole_blacklist"));
        BuiltInRegistries.BLOCK.getTag(tag).ifPresent(holders -> holders.forEach(holder -> portableHoleBlackList.add(holder.value())));
    }

    /** Appends a live phenomena callback to the global list; null handler throws NPE, without invoking it at registration. */
    public static void registerScanEventhandler(IScanEventHandler handler) { scanEventhandlers.add(Objects.requireNonNull(handler, "handler")); }
    /** Appends a live entity-tag registration retaining nullable aspects/filter arrays; null/empty entity name throws IAE, and registration does not scan entities. */
    public static void registerEntityTag(String entityName, AspectList aspects, EntityTagsNBT... nbt) {
        if (entityName == null || entityName.isEmpty()) throw new IllegalArgumentException("An entity tag requires its original or native registry name");
        scanEntities.add(new EntityTags(entityName, aspects, nbt));
    }

    /** Item identity and native damage metadata only: count, names and other NBT/components never narrow this registration. */
    public static void registerObjectTag(ItemStack item, AspectList aspects) {
        if (item == null || item.isEmpty()) return;
        registerObjectTag(item.getItem(), item.getDamageValue(), aspects);
    }
    /** Registers item aspect amounts using native damage points (32767 is wildcard), not 1.7.10 subtype metadata.
     * Use distinct native item IDs or {@link #registerObjectTag(TagKey, AspectList)} for flattened variants;
     * this is not a component predicate. Null item is ignored, null aspects stores a new empty list;
     * otherwise the live aspects reference replaces the global registration. */
    public static void registerObjectTag(Item item, int metadata, AspectList aspects) {
        ObjectAspectRegistry.register(objectTags, item, metadata, aspects);
    }
    /** Resolves a native item ID then registers damage-point metadata, not old subtype IDs; unknown IDs throw IAE.
     * In 1.21.1 use distinct item IDs or TagKey registration rather than 1.7.10 metadata variants. */
    public static void registerObjectTag(ResourceLocation item, int metadata, AspectList aspects) {
        if (!BuiltInRegistries.ITEM.containsKey(item)) throw new IllegalArgumentException("Unknown native object-tag item: " + item);
        registerObjectTag(BuiltInRegistries.ITEM.get(item), metadata, aspects);
    }
    /** Registers a group of native damage-point keys; null/empty stack or null/empty metadata is ignored.
     * Unlike 1.7.10 subtype metadata, these cannot select flattened variants: use native item IDs or TagKey registration. */
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
    /** Tests ordinary/wildcard and bundled derived registrations using native damage points (32767 wildcard), not subtype metadata; null item throws NPE, with no registration mutation. */
    public static boolean exists(Item item, int metadata) {
        return ObjectAspectRegistry.exists(objectTags, item, metadata)
                || ObjectAspectCatalog.hasDerivedRegistration(BuiltInRegistries.ITEM.getKey(item), metadata);
    }
    /** Registers recipe-derived aspect-unit additions using the loaded server context; no vis is spent.
     * Requires the server thread and loaded overworld, otherwise throws IllegalStateException before validating arguments;
     * null/empty item then throws IAE and null aspects throws NPE. May mutate supplied aspects when no registration exists,
     * initializes public tags, and stores the resulting live list. Prefer the Level overload for explicit lifecycle scope. */
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
        /** Stores all nullable arguments by reference without validation or registration; subsequent edits remain visible. */
        public EntityTags(String entityName, AspectList aspects, EntityTagsNBT... nbts) {
            this.entityName = entityName; this.aspects = aspects; this.nbts = nbts;
        }
    }
    /** Mutable entity-tag filter datum; this is entity NBT compatibility data, not an item component predicate. */
    public static class EntityTagsNBT {
        public String name;
        public Object value;
        /** Stores nullable name/value references without validation or registration. */
        public EntityTagsNBT(String name, Object value) { this.name = name; this.value = value; }
    }
}
