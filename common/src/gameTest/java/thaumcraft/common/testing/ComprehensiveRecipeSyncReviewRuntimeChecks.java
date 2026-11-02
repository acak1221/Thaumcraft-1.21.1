package thaumcraft.common.testing;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.crafting.CrucibleRecipeInput;
import thaumcraft.common.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;

/** Registry-backed JSON and real clientbound recipe packets, including tags that currently have no items. */
public final class ComprehensiveRecipeSyncReviewRuntimeChecks {
    private static final String EMPTY_TAG = "{\"tag\":\"thaumcraft_review:empty_optional_ingredient\"}";
    private static final String STICK = "{\"item\":\"minecraft:stick\"}";
    private static final String STONE = "{\"item\":\"minecraft:cobblestone\"}";
    private static final String RESULT = "\"result\":{\"id\":\"minecraft:iron_nugget\"}";

    private ComprehensiveRecipeSyncReviewRuntimeChecks() {}

    public static void arcaneAndCrucibleKeepEmptyTagsDisabledAcrossPackets(GameTestHelper test) {
        var level = test.getLevel();
        test.assertTrue(Ingredient.EMPTY.test(ItemStack.EMPTY), "native omitted-ingredient matching needs an explicit disabled-tag control");
        var rows = new ArrayList<RecipeHolder<?>>();
        for (String ingredient : List.of(EMPTY_TAG, STICK)) {
            String suffix = ingredient.equals(EMPTY_TAG) ? "disabled" : "ordinary";
            rows.add(holder("shaped_" + suffix, parse(level, ArcaneShapedRecipe.CODEC.codec(),
                    "{\"pattern\":[\"A\"],\"key\":{\"A\":" + ingredient + "}," + RESULT + ",\"vis\":{\"aer\":1}}")));
            rows.add(holder("shapeless_" + suffix, parse(level, ArcaneShapelessRecipe.CODEC.codec(),
                    "{\"ingredients\":[" + ingredient + "]," + RESULT + ",\"vis\":{\"aer\":1}}")));
            rows.add(holder("crucible_" + suffix, parse(level, CrucibleRecipe.CODEC.codec(),
                    "{\"research\":\"ALCHEMY\",\"catalyst\":" + ingredient + "," + RESULT + ",\"aspects\":{\"aer\":1},\"source_order\":0}")));
        }
        for (int pass = 0; pass < 3; pass++) {
            for (var holder : rows) {
                boolean enabled = holder.id().getPath().endsWith("ordinary");
                if (holder.value() instanceof ArcaneShapedRecipe recipe) {
                    test.assertTrue(recipe.matches(grid(new ItemStack(Items.STICK)), level) == enabled
                            && !recipe.matches(grid(ItemStack.EMPTY), level), "shaped recipe retains positive/disabled and empty-grid behavior, pass " + pass);
                } else if (holder.value() instanceof ArcaneShapelessRecipe recipe) {
                    test.assertTrue(recipe.matches(grid(new ItemStack(Items.STICK)), level) == enabled
                            && !recipe.matches(grid(ItemStack.EMPTY), level), "shapeless recipe retains positive/disabled and empty-grid behavior, pass " + pass);
                } else if (holder.value() instanceof CrucibleRecipe recipe) {
                    var cost = new AspectList().add(Aspect.AIR, 1);
                    test.assertTrue(recipe.matches(new CrucibleRecipeInput(new ItemStack(Items.STICK), cost), level) == enabled
                            && !recipe.matches(new CrucibleRecipeInput(ItemStack.EMPTY, cost), level), "crucible recipe retains positive/disabled and empty-catalyst behavior, pass " + pass);
                }
            }
            if (pass < 2) rows = roundTrip(test, level, rows);
        }
        test.succeed();
    }

    public static void stackInfusionEmptyTagsSurvivePacketsWithoutBecomingCraftable(GameTestHelper test) {
        var level = test.getLevel();
        var rows = new ArrayList<RecipeHolder<?>>();
        for (String variant : List.of("input", "component", "ordinary")) {
            String json = "{\"input\":" + (variant.equals("input") ? EMPTY_TAG : STONE)
                    + ",\"components\":[" + (variant.equals("component") ? EMPTY_TAG : STICK) + "],"
                    + RESULT + ",\"aspects\":{\"aer\":1},\"instability\":1}";
            rows.add(holder("infusion_" + variant, parse(level, InfusionStackRecipe.CODEC.codec(), json)));
        }
        var ordinary = new InfusionRecipeInput(new ItemStack(Items.COBBLESTONE), List.of(new ItemStack(Items.STICK)));
        for (int pass = 0; pass < 3; pass++) {
            for (var holder : rows) {
                var recipe = (InfusionStackRecipe) holder.value();
                boolean enabled = holder.id().getPath().endsWith("ordinary");
                test.assertTrue(recipe.matches(ordinary, level) == enabled, "central/component disabled tags cannot match ordinary altar stock, pass " + pass);
                test.assertTrue(!recipe.matches(new InfusionRecipeInput(ItemStack.EMPTY, List.of(new ItemStack(Items.STICK))), level)
                        && !recipe.matches(new InfusionRecipeInput(new ItemStack(Items.COBBLESTONE), List.of(ItemStack.EMPTY)), level)
                        && !recipe.matches(new InfusionRecipeInput(ItemStack.EMPTY, List.of(ItemStack.EMPTY)), level),
                        "resolved empty ingredients cannot match missing altar stock, pass " + pass);
                var api = InfusionRecipe.of(recipe);
                test.assertTrue(api.matches(new ArrayList<>(ordinary.components()), ordinary.central(), level, null) == enabled
                        && !api.matches(new ArrayList<>(List.of(ItemStack.EMPTY)), ItemStack.EMPTY, level, null),
                        "native public views preserve disabled predicates instead of matching their empty display samples, pass " + pass);
                test.assertTrue(recipe.result().is(Items.IRON_NUGGET) && recipe.aspects().equals(Map.of("aer", 1)), "packet preserves output and cost");
            }
            if (pass < 2) rows = roundTrip(test, level, rows);
        }
        test.assertTrue(InfusionStackRecipe.CODEC.codec().parse(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),
                JsonParser.parseString("{\"input\":[],\"components\":[" + STICK + "]," + RESULT + ",\"aspects\":{\"aer\":1},\"instability\":1}")).error().isPresent(),
                "JSON still refuses an omitted central ingredient");
        rejectsStackStream(test, level, 25, 1);
        rejectsStackStream(test, level, 1, 0);
        test.succeed();
    }

    public static void enchantmentInfusionEmptyTagSurvivesPacketsWithoutBecomingCraftable(GameTestHelper test) {
        var level = test.getLevel();
        var rows = new ArrayList<RecipeHolder<?>>();
        for (String ingredient : List.of(EMPTY_TAG, STICK)) {
            String suffix = ingredient.equals(EMPTY_TAG) ? "disabled" : "ordinary";
            rows.add(holder("enchantment_" + suffix, parse(level, InfusionEnchantmentRecipe.CODEC.codec(),
                    "{\"enchantment\":\"minecraft:unbreaking\",\"components\":[" + ingredient
                            + "],\"aspects\":{\"aer\":1},\"instability\":1,\"first_level_minimum_cost\":1}")));
        }
        for (int pass = 0; pass < 3; pass++) {
            for (var holder : rows) {
                var recipe = (InfusionEnchantmentRecipe) holder.value();
                boolean enabled = holder.id().getPath().endsWith("ordinary");
                test.assertTrue(recipe.matches(new InfusionRecipeInput(new ItemStack(Items.IRON_SWORD), List.of(new ItemStack(Items.STICK))), level) == enabled,
                        "ordinary enchantment control works while the disabled component tag never matches, pass " + pass);
                test.assertTrue(!recipe.matches(new InfusionRecipeInput(new ItemStack(Items.IRON_SWORD), List.of(ItemStack.EMPTY)), level)
                        && !recipe.matches(new InfusionRecipeInput(ItemStack.EMPTY, List.of(ItemStack.EMPTY)), level),
                        "resolved empty ingredients do not grant an enchantment from missing stock, pass " + pass);
            }
            if (pass < 2) rows = roundTrip(test, level, rows);
        }
        test.assertTrue(InfusionEnchantmentRecipe.CODEC.codec().parse(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),
                JsonParser.parseString("{\"enchantment\":\"minecraft:unbreaking\",\"components\":[[]],\"aspects\":{\"aer\":1},\"instability\":1,\"first_level_minimum_cost\":1}")).error().isPresent(),
                "JSON still refuses an omitted component ingredient");
        rejectsEnchantmentStream(test, level, 25, 1, 1);
        rejectsEnchantmentStream(test, level, 1, -1, 1);
        rejectsEnchantmentStream(test, level, 1, 1, 0);
        test.succeed();
    }

    private static <T> T parse(ServerLevel level, Codec<T> codec, String json) {
        return codec.parse(level.registryAccess().createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json)).getOrThrow();
    }

    private static <T extends Recipe<?>> RecipeHolder<T> holder(String name, T recipe) {
        return new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("thaumcraft_review", "packet/" + name), recipe);
    }

    private static ArcaneRecipeInput grid(ItemStack first) {
        var items = new ArrayList<ItemStack>();
        for (int index = 0; index < 9; index++) items.add(index == 0 ? first : ItemStack.EMPTY);
        return new ArcaneRecipeInput(items);
    }

    private static ArrayList<RecipeHolder<?>> roundTrip(GameTestHelper test, ServerLevel level, List<RecipeHolder<?>> rows) {
        var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            ClientboundUpdateRecipesPacket.STREAM_CODEC.encode(wire, new ClientboundUpdateRecipesPacket(rows));
            var decoded = ClientboundUpdateRecipesPacket.STREAM_CODEC.decode(wire);
            test.assertTrue(wire.readableBytes() == 0 && decoded.getRecipes().size() == rows.size(), "real clientbound packet preserves framing and every recipe");
            test.assertTrue(decoded.getRecipes().stream().map(RecipeHolder::id).toList().equals(rows.stream().map(RecipeHolder::id).toList()), "real packet preserves recipe identities and order");
            return new ArrayList<>(decoded.getRecipes());
        } finally { wire.release(); }
    }

    private static void rejectsStackStream(GameTestHelper test, ServerLevel level, int instability, int cost) {
        var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        boolean rejected = false;
        try {
            wire.writeUtf(""); Ingredient.CONTENTS_STREAM_CODEC.encode(wire, Ingredient.EMPTY);
            wire.writeVarInt(1); Ingredient.CONTENTS_STREAM_CODEC.encode(wire, Ingredient.of(Items.STICK));
            wire.writeVarInt(1); wire.writeUtf("aer"); wire.writeVarInt(cost); wire.writeVarInt(instability);
            ItemStack.STREAM_CODEC.encode(wire, new ItemStack(Items.IRON_NUGGET));
            wire.writeVarInt(0); wire.writeBoolean(false); wire.writeBoolean(false);
            try { InfusionStackRecipe.Serializer.STREAM_CODEC.decode(wire); } catch (RuntimeException expected) { rejected = true; }
        } finally { wire.release(); }
        test.assertTrue(rejected, "stream-only resolved-empty support must not bypass stack infusion cost/instability validation");
    }

    private static void rejectsEnchantmentStream(GameTestHelper test, ServerLevel level, int instability, int minimum, int cost) {
        var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        boolean rejected = false;
        try {
            wire.writeUtf(""); wire.writeResourceLocation(ResourceLocation.withDefaultNamespace("unbreaking"));
            wire.writeVarInt(instability); wire.writeVarInt(minimum); wire.writeVarInt(1);
            Ingredient.CONTENTS_STREAM_CODEC.encode(wire, Ingredient.EMPTY);
            wire.writeVarInt(1); wire.writeUtf("aer"); wire.writeVarInt(cost);
            try { InfusionEnchantmentRecipe.Serializer.STREAM_CODEC.decode(wire); } catch (RuntimeException expected) { rejected = true; }
        } finally { wire.release(); }
        test.assertTrue(rejected, "stream-only resolved-empty support must not bypass enchantment cost/instability validation");
    }
}
