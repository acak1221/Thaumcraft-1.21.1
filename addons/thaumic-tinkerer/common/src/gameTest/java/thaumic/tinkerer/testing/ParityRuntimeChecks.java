package thaumic.tinkerer.testing;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.InfusionEnchantmentRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.TTRecipes;
import thaumic.tinkerer.testing.parity.KnownDifference;
import thaumic.tinkerer.testing.parity.OracleData;
import thaumic.tinkerer.testing.parity.OracleData.Condition;
import thaumic.tinkerer.testing.parity.ParityComparator;
import thaumic.tinkerer.testing.parity.ParityComparator.Difference;
import thaumic.tinkerer.testing.parity.ParityComparator.Result;
import thaumic.tinkerer.testing.parity.PortSnapshot;
import thaumic.tinkerer.testing.parity.PortSnapshot.PoolEntry;
import thaumic.tinkerer.testing.parity.PortSnapshot.PortPage;
import thaumic.tinkerer.testing.parity.PortSnapshot.PortRecipe;
import thaumic.tinkerer.testing.parity.PortSnapshot.PortResearch;

/**
 * Parity of the content the add-on registers with Thaumcraft against Thaumic Tinkerer 1.7.10: every research entry
 * and every recipe the original registered (extracted by {@code scripts/porting/tt_oracle.py} into
 * {@code tt_oracle/research.json} and {@code recipes.json}) must exist at run time with the same category, aspects,
 * position, complexity, parents, flags, warp, pages, output, count, instability and ingredients, and the add-on must
 * register nothing the original did not. The differences the deviation texts document are the allow-list
 * {@code tt_oracle/known_differences.json}; everything else fails with the subject, the field and both values. The
 * comparison itself is plain Java in {@link ParityComparator} (unit-tested with mutated oracles); this class reads the
 * running registries into a {@link PortSnapshot}. Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0
 * like the add-on.
 */
public final class ParityRuntimeChecks {
    private static final String REPORT = "tinkerer-parity-report.txt";
    private static final int SHOWN = 60;
    private static Result cached;

    private ParityRuntimeChecks() {}

    // ------------------------------------------------------------------------------------------ the running port

    /** The truth of a condition of the original in the running port; none of the 1.7.10 mods exists on 1.21.1. */
    public static boolean condition(Condition c) {
        switch (c.kind()) {
            case "mod":
                return false;
            case "config":
                if (c.expr().equals("Config.allowMirrors")) return ThaumcraftApi.isMirrorsAllowed();
                if (c.expr().startsWith("ConfigHandler.")) {
                    try {
                        Field f = ConfigHandler.class.getField(c.expr().substring("ConfigHandler.".length()));
                        return f.getBoolean(null);
                    } catch (ReflectiveOperationException error) {
                        throw new IllegalStateException("the port has no configuration flag " + c.expr(), error);
                    }
                }
                break;
            default:
        }
        throw new IllegalStateException("unknown condition " + c);
    }

    private static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static String shown(ItemStack stack) {
        return stack == null ? "null" : id(stack) + " x" + stack.getCount();
    }

    private static Map<String, Integer> aspects(AspectList list) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (list != null) for (Aspect a : list.getAspects()) out.put(a.getTag(), list.getAmount(a));
        return out;
    }

    private static List<String> strings(String[] values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(List.of(values));
    }

    /** The ids that can fill an ingredient slot of a recipe. */
    private static List<String> candidates(Object ingredient) {
        List<String> ids = new ArrayList<>();
        if (ingredient instanceof Ingredient i) {
            for (ItemStack s : i.getItems()) ids.add(id(s));
        } else if (ingredient instanceof ItemStack s) {
            ids.add(id(s));
        } else if (ingredient instanceof net.minecraft.world.item.Item item) {
            ids.add(BuiltInRegistries.ITEM.getKey(item).toString());
        } else if (ingredient instanceof net.minecraft.world.level.block.Block block) {
            ids.add(BuiltInRegistries.BLOCK.getKey(block).toString());
        } else if (ingredient != null) {
            ids.add("?" + ingredient);
        }
        if (ids.isEmpty()) ids.add("none");
        return ids;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> ttRecipes() {
        try {
            Field f = TTRecipes.class.getDeclaredField("RECIPES");
            f.setAccessible(true);
            return (Map<String, Object>) f.get(null);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("TTRecipes no longer keeps its recipes in RECIPES", error);
        }
    }

    private static final String[] FLAGS = { "secondary", "concealed", "hidden", "special", "stub", "round", "autoUnlock",
            "virtual", "lost" };

    private static boolean flag(ResearchItem r, String name) {
        return switch (name) {
            case "secondary" -> r.isSecondary();
            case "concealed" -> r.isConcealed();
            case "hidden" -> r.isHidden();
            case "special" -> r.isSpecial();
            case "stub" -> r.isStub();
            case "round" -> r.isRound();
            case "autoUnlock" -> r.isAutoUnlock();
            case "virtual" -> r.isVirtual();
            case "lost" -> r.isLost();
            default -> throw new IllegalArgumentException(name);
        };
    }

    private static List<String> recipeOutputs(Object recipe, MinecraftServer server) {
        List<String> out = new ArrayList<>();
        if (recipe instanceof Object[] array) {
            for (Object o : array) out.addAll(recipeOutputs(o, server));
        } else if (recipe instanceof IArcaneRecipe r) {
            out.add(shown(r.getRecipeOutput()));
        } else if (recipe instanceof CrucibleRecipe r) {
            out.add(shown(r.getRecipeOutput()));
        } else if (recipe instanceof InfusionRecipe r) {
            out.add(r.getRecipeOutput() instanceof ItemStack s ? shown(s) : "patch of " + shown(r.getRecipeInput()));
        } else if (recipe instanceof net.minecraft.world.item.crafting.Recipe<?> r) {
            out.add(shown(r.getResultItem(server.registryAccess())));
        } else if (recipe != null) {
            out.add("?" + recipe.getClass().getSimpleName());
        }
        return out;
    }

    private static PortPage page(String key, ResearchPage p, MinecraftServer server) {
        String kind;
        String text = null;
        List<String> recipes = new ArrayList<>();
        switch (p.type) {
            case TEXT -> {
                kind = "text";
                String prefix = "ttresearch.page." + key + ".";
                text = p.text != null && p.text.startsWith(prefix) ? p.text.substring(prefix.length()) : p.text;
            }
            case TEXT_CONCEALED -> {
                kind = "text_concealed";
                text = p.text;
            }
            case IMAGE -> kind = "image";
            case ASPECTS -> kind = "aspects";
            case SMELTING -> kind = "smelting";
            case COMPOUND_CRAFTING -> kind = "compound";
            case ARCANE_CRAFTING -> {
                kind = "arcane";
                recipes.addAll(recipeOutputs(p.recipe, server));
            }
            case CRUCIBLE_CRAFTING -> {
                kind = "crucible";
                recipes.addAll(recipeOutputs(p.recipe, server));
            }
            case INFUSION_CRAFTING -> {
                kind = "infusion";
                recipes.addAll(recipeOutputs(p.recipe, server));
            }
            case INFUSION_ENCHANTMENT -> kind = "enchant";
            case NORMAL_CRAFTING -> {
                kind = "normal";
                if (p.recipeIds != null) {
                    for (ResourceLocation rl : p.recipeIds) {
                        var holder = server.getRecipeManager().byKey(rl);
                        recipes.add(holder.isPresent() ? shown(holder.get().value().getResultItem(server.registryAccess())) : "unresolved:" + rl);
                    }
                } else {
                    recipes.addAll(recipeOutputs(p.recipe, server));
                }
            }
            default -> throw new IllegalStateException("page type " + p.type);
        }
        return new PortPage(kind, text, recipes);
    }

    /** What the add-on registered, read from the running registries of Thaumcraft and the recipe manager. */
    public static PortSnapshot snapshot(MinecraftServer server) {
        Map<String, PortResearch> research = new LinkedHashMap<>();
        List<PoolEntry> pool = new ArrayList<>();
        for (Map.Entry<String, ResearchCategoryList> category : ResearchCategories.researchCategories.entrySet()) {
            for (ResearchItem r : category.getValue().research.values()) {
                pool.add(new PoolEntry(r.key, category.getKey(), r.isLost(), r.isVirtual()));
            }
        }
        ResearchCategoryList tab = ResearchCategories.getResearchList(LibResearch.CATEGORY_THAUMICTINKERER);
        if (tab != null) {
            for (ResearchItem r : tab.research.values()) {
                Map<String, Boolean> flags = new LinkedHashMap<>();
                for (String f : FLAGS) flags.put(f, flag(r, f));
                List<PortPage> pages = new ArrayList<>();
                if (r.getPages() != null) for (ResearchPage p : r.getPages()) pages.add(page(r.key, p, server));
                research.put(r.key, new PortResearch(r.key, r.category, aspects(r.tags), r.displayColumn, r.displayRow,
                        r.getComplexity(), strings(r.parents), strings(r.parentsHidden), strings(r.siblings), flags,
                        ThaumcraftApi.getWarp(r.key), pages, r.icon_item == null ? null : id(r.icon_item)));
            }
        }
        return new PortSnapshot(research, pool, recipes(server));
    }

    private static List<PortRecipe> recipes(MinecraftServer server) {
        IdentityHashMap<Object, String> names = new IdentityHashMap<>();
        for (Map.Entry<String, Object> e : ttRecipes().entrySet()) names.put(e.getValue(), e.getKey());
        List<PortRecipe> out = new ArrayList<>();
        int n = 0;
        for (Object o : ThaumcraftApi.getProgrammaticCraftingRecipes()) {
            String name = names.get(o);
            String at = "programmatic#" + (n++);
            if (o instanceof ShapedArcaneRecipe r) {
                List<List<String>> cells = new ArrayList<>();
                for (Object cell : r.getInput()) if (cell != null) cells.add(candidates(cell));
                out.add(new PortRecipe(at, name, "arcane_shaped", r.getResearch(), id(r.getRecipeOutput()), r.getRecipeOutput().getCount(),
                        aspects(r.getAspects()), null, r.width, r.height, cells, List.of()));
            } else if (o instanceof ShapelessArcaneRecipe r) {
                List<List<String>> components = new ArrayList<>();
                for (Object cell : r.getInput()) components.add(candidates(cell));
                out.add(new PortRecipe(at, name, "arcane_shapeless", r.getResearch(), id(r.getRecipeOutput()), r.getRecipeOutput().getCount(),
                        aspects(r.getAspects()), null, 0, 0, List.of(), components));
            } else if (o instanceof InfusionRecipe r) {
                ItemStack result = r.getRecipeOutput() instanceof ItemStack s ? s : r.getRecipeInput();
                List<List<String>> components = new ArrayList<>();
                for (ItemStack c : r.getComponents()) components.add(candidates(c));
                out.add(new PortRecipe(at, name, "infusion", r.getResearch(), id(result), result.getCount(), aspects(r.getAspects()),
                        r.getInstability(), 0, 0, List.of(candidates(r.getRecipeInput())), components));
            } else if (o instanceof CrucibleRecipe r) {
                out.add(new PortRecipe(at, name, "crucible", r.key, id(r.getRecipeOutput()), r.getRecipeOutput().getCount(),
                        aspects(r.aspects), null, 0, 0, List.of(candidates(r.catalyst)), List.of()));
            } else if (o instanceof InfusionEnchantmentRecipe) {
                out.add(new PortRecipe(at, name, "infusion_enchantment", null, "?", 0, Map.of(), null, 0, 0, List.of(), List.of()));
            } else {
                out.add(new PortRecipe(at, name, "?" + o.getClass().getSimpleName(), null, "?", 0, Map.of(), null, 0, 0, List.of(), List.of()));
            }
        }
        for (RecipeHolder<CraftingRecipe> holder : server.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!holder.id().getNamespace().equals(LibMisc.MOD_ID)) continue;
            CraftingRecipe r = holder.value();
            ItemStack result = r.getResultItem(server.registryAccess());
            List<List<String>> slots = new ArrayList<>();
            for (Ingredient i : r.getIngredients()) if (!i.isEmpty()) slots.add(candidates(i));
            if (r instanceof ShapedRecipe s) {
                out.add(new PortRecipe(holder.id().toString(), holder.id().getPath(), "crafting_shaped", null, id(result), result.getCount(),
                        null, null, s.getWidth(), s.getHeight(), slots, List.of()));
            } else if (r instanceof ShapelessRecipe) {
                out.add(new PortRecipe(holder.id().toString(), holder.id().getPath(), "crafting_shapeless", null, id(result), result.getCount(),
                        null, null, 0, 0, List.of(), slots));
            } else {
                out.add(new PortRecipe(holder.id().toString(), holder.id().getPath(), "?" + r.getClass().getSimpleName(), null, id(result),
                        result.getCount(), null, null, 0, 0, List.of(), slots));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------ the comparison

    /** Compared once per server run; the individual checks assert on parts of this result. */
    public static synchronized Result result(GameTestHelper test) {
        if (cached == null) {
            test.assertTrue(ThaumicTinkerer.isReady(), "the Thaumcraft ready hook of the add-on ran");
            MinecraftServer server = test.getLevel().getServer();
            cached = ParityComparator.compare(OracleData.fromClasspath(), snapshot(server), ParityRuntimeChecks::condition,
                    KnownDifference.fromClasspath());
            writeReport(cached);
        }
        return cached;
    }

    private static void writeReport(Result result) {
        StringBuilder sb = new StringBuilder("Parity of the add-on with Thaumic Tinkerer 1.7.10 (tt_oracle)\n");
        sb.append(result.counts).append('\n').append(result.describe());
        sb.append("documented differences (known_differences.json):\n");
        for (Difference d : result.allowed) sb.append("  ALLOWED ").append(d).append('\n');
        try {
            Files.writeString(Path.of(REPORT), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            // the failure messages of the checks carry the same information
        }
    }

    private static boolean isExtra(Difference d) {
        return d.field().equals("presence") && !d.port().equals("missing");
    }

    private static void assertNone(GameTestHelper test, List<Difference> failures, String what) {
        if (failures.isEmpty()) return;
        StringBuilder sb = new StringBuilder(failures.size() + " " + what + " (full list: " + REPORT + " in the run directory)\n");
        for (int i = 0; i < Math.min(SHOWN, failures.size()); i++) sb.append("  ").append(failures.get(i)).append('\n');
        if (failures.size() > SHOWN) sb.append("  ... and ").append(failures.size() - SHOWN).append(" more\n");
        test.fail(sb.toString());
    }

    /** The oracle belongs to the pinned commit and is not empty, so a broken extraction cannot make everything pass. */
    public static void oracleIsPinnedAndComplete(GameTestHelper test) {
        OracleData oracle = OracleData.fromClasspath();
        test.assertTrue("28f4def938e80d7a0b6f433c2b84300dfb7c35ce".equals(oracle.commit()), "the oracle is of commit " + oracle.commit());
        long keys = oracle.research().stream().map(OracleData.Research::key).distinct().count();
        test.assertTrue(keys == 78, "the original registers 78 research keys, the oracle has " + keys);
        test.assertTrue(oracle.recipes().size() == 129, "the original registers 129 recipes, the oracle has " + oracle.recipes().size());
        test.succeed();
    }

    public static void researchMatchesTheOriginal(GameTestHelper test) {
        Result result = result(test);
        assertNone(test, result.failures.stream().filter(d -> d.kind().equals("research") && !isExtra(d)).toList(),
                "research entries differ from the original");
        test.assertTrue(result.counts.get("research compared") >= 70, "research compared: " + result.counts);
        test.succeed();
    }

    public static void recipesMatchTheOriginal(GameTestHelper test) {
        Result result = result(test);
        assertNone(test, result.failures.stream().filter(d -> d.kind().equals("recipe") && !isExtra(d)).toList(),
                "recipes differ from the original");
        test.assertTrue(result.counts.get("recipes compared") >= 120, "recipes compared: " + result.counts);
        test.succeed();
    }

    /** The reverse check: research and recipes the add-on registers that the original did not. */
    public static void theAddOnRegistersNothingTheOriginalDidNot(GameTestHelper test) {
        Result result = result(test);
        assertNone(test, result.failures.stream().filter(ParityRuntimeChecks::isExtra).toList(),
                "registrations that are new in the port (list them in known_differences.json with the deviation that explains them)");
        test.succeed();
    }

    /** An allow-list entry that no longer matches a difference hides nothing and must go. */
    public static void everyKnownDifferenceStillMatches(GameTestHelper test) {
        Result result = result(test);
        if (!result.unusedKnown.isEmpty()) {
            StringBuilder sb = new StringBuilder(result.unusedKnown.size() + " known_differences.json entries match no difference:\n");
            for (KnownDifference k : result.unusedKnown) sb.append("  ").append(k).append('\n');
            test.fail(sb.toString());
        }
        test.succeed();
    }

    /**
     * The comparison is not weak: with a position, an aspect amount or a parent of the oracle changed, the live
     * registries no longer match. (The unit test ParityComparatorTest does the same for every entry.)
     */
    public static void aChangedOracleValueFailsAgainstTheLiveRegistries(GameTestHelper test) {
        MinecraftServer server = test.getLevel().getServer();
        PortSnapshot port = snapshot(server);
        List<KnownDifference> none = List.of();
        for (String mutation : List.of("column", "aspects", "parents")) {
            JsonObject research = OracleData.readResource(OracleData.RESEARCH_RESOURCE).deepCopy();
            JsonObject entry = null;
            for (var e : research.getAsJsonArray("entries")) {
                if (e.getAsJsonObject().get("key").getAsString().equals("ENCHANTER")) entry = e.getAsJsonObject();
            }
            test.assertTrue(entry != null, "ENCHANTER is in the oracle");
            switch (mutation) {
                case "column" -> entry.addProperty("column", entry.get("column").getAsInt() + 1);
                case "aspects" -> {
                    JsonObject a = entry.getAsJsonObject("aspects");
                    String first = a.keySet().iterator().next();
                    a.addProperty(first, a.get(first).getAsInt() + 1);
                }
                default -> {
                    JsonArray parents = new JsonArray();
                    parents.add("NO_SUCH_PARENT");
                    entry.add("parents", parents);
                }
            }
            OracleData changed = OracleData.parse(research, OracleData.readResource(OracleData.RECIPES_RESOURCE));
            Result result = ParityComparator.compare(changed, port, ParityRuntimeChecks::condition, none);
            String field = mutation.equals("column") ? "column" : mutation;
            test.assertTrue(result.failures.stream().anyMatch(d -> d.subject().equals("ENCHANTER") && d.field().equals(field)),
                    "changing " + mutation + " of ENCHANTER in the oracle must fail the comparison, got " + result.failures.size() + " differences");
        }
        test.succeed();
    }
}
