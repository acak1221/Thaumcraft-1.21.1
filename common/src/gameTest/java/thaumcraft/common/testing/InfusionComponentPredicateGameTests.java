package thaumcraft.common.testing;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.crafting.InfusionComponentPredicate.Declaration;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.crafting.InfusionMatching;
import thaumcraft.common.lib.infusion.InfusionStructure;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TilePedestal;

/** SERVER-ONLY prerequisite fixtures, NOT a registered bore recipe or acquisition
 * claim. Root owns loader wrappers, required IDs and execution. No clock/RNG or
 * progress edits: native start, ticker, paired source payments and consumption. */
public final class InfusionComponentPredicateGameTests {
    private static final ResourceLocation FIXTURE_ID = ResourceLocation.fromNamespaceAndPath("thaumcraft", "test/infusion_component_predicate");
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private InfusionComponentPredicateGameTests() {}

    /** Suggested wrapper ID: infusion_component_predicate_codec. */
    public static void recipeCodecAndDeclaredTools(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        var ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var original = fixtureRecipe(true);
        var json = InfusionStackRecipe.CODEC.codec().encodeStart(ops, original).getOrThrow().getAsJsonObject();
        var fromJson = InfusionStackRecipe.CODEC.codec().parse(ops, json).getOrThrow();
        var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        InfusionStackRecipe fromWire;
        try {
            InfusionStackRecipe.Serializer.STREAM_CODEC.encode(wire, fromJson);
            fromWire = InfusionStackRecipe.Serializer.STREAM_CODEC.decode(wire);
            test.assertTrue(!wire.isReadable(), "bounded recipe stream consumes exactly its own payload");
        } finally { wire.release(); }
        for (var recipe : List.of(original, fromJson, fromWire)) {
            test.assertTrue(recipe.componentPredicates().equals(List.of(new Declaration(2, 0), new Declaration(3, 0)))
                            && new ArrayList<>(recipe.aspects().keySet()).equals(List.of("aer", "ordo"))
                            && recipe.aspectCost().visSize() == 3,
                    "JSON and wire retain exact indices/damage and ORIGINAL aspect/payment insertion order");
            matchingCases(test, recipe);
        }
        wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            ClientboundUpdateRecipesPacket.STREAM_CODEC.encode(wire,
                    new ClientboundUpdateRecipesPacket(List.of(new RecipeHolder<>(FIXTURE_ID, fromJson))));
            var packet = ClientboundUpdateRecipesPacket.STREAM_CODEC.decode(wire);
            var received = (InfusionStackRecipe) packet.getRecipes().getFirst().value();
            test.assertTrue(received.componentPredicates().equals(original.componentPredicates()) && !wire.isReadable(),
                    "actual native recipe packet dispatches the registered serializer with its declared predicate");
            matchingCases(test, received);
        } finally { wire.release(); }

        var plainJson = json.deepCopy(); plainJson.remove("component_predicates");
        var old = InfusionStackRecipe.CODEC.codec().parse(ops, plainJson).getOrThrow();
        test.assertTrue(old.componentPredicates().isEmpty()
                        && old.matches(offered(new ItemStack(Items.DIAMOND_PICKAXE), worn(Items.DIAMOND_SHOVEL)), level)
                        && fixtureRecipe(false).matches(offered(worn(Items.DIAMOND_PICKAXE), worn(Items.DIAMOND_SHOVEL)), level),
                "absent JSON field AND old Java constructor retain native wear-ignoring ingredients");
        wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            InfusionStackRecipe.Serializer.STREAM_CODEC.encode(wire, old);
            var legacy = InfusionStackRecipe.Serializer.STREAM_CODEC.decode(wire);
            test.assertTrue(legacy.componentPredicates().isEmpty() && !wire.isReadable()
                            && legacy.matches(offered(worn(Items.DIAMOND_PICKAXE), worn(Items.DIAMOND_SHOVEL)), level),
                    "empty predicate list retains old semantics through the new recipe network format");
        } finally { wire.release(); }
        var exactJson = json.deepCopy();
        exactJson.add("component_predicates", JsonParser.parseString("[{\"component\":2,\"damage\":7},{\"component\":3,\"damage\":0}]"));
        var exact = InfusionStackRecipe.CODEC.codec().parse(ops, exactJson).getOrThrow();
        var seven = decorated(level, Items.DIAMOND_PICKAXE); seven.setDamageValue(7);
        test.assertTrue(exact.componentDamage(2).orElseThrow() == 7
                        && exact.matches(offered(seven, new ItemStack(Items.DIAMOND_SHOVEL)), level)
                        && !exact.matches(offered(new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_SHOVEL)), level),
                "declared damage is exact, not a global pristine flag or maximum-damage range; copied book examples use this same value");
        for (String path : List.of("infusion/wand_rod_silverwood", "infusion/wand_cap_thaumium")) {
            var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", path)).orElseThrow();
            var recipe = (InfusionStackRecipe) holder.value();
            test.assertTrue(recipe.componentPredicates().isEmpty(), "existing Silverwood/cap recipes have NO implicit pristine policy");
            var stacks = recipe.components().stream().map(ingredient -> ingredient.getItems()[0].copyWithCount(1)).toList();
            test.assertTrue(recipe.matches(new InfusionRecipeInput(recipe.input().getItems()[0].copy(), stacks), level),
                    "existing registered infusion still matches its ordinary ingredients: " + holder.id());
        }
        var greedy = new InfusionStackRecipe("", Ingredient.of(Items.PISTON),
                List.of(Ingredient.of(Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL), Ingredient.of(Items.DIAMOND_PICKAXE)),
                Map.of("aer", 1), 0, new ItemStack(Items.DIAMOND), List.of(new Declaration(0, 0), new Declaration(1, 0)));
        test.assertTrue(!greedy.matches(new InfusionRecipeInput(new ItemStack(Items.PISTON),
                        List.of(new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_SHOVEL))), level)
                        && greedy.matches(new InfusionRecipeInput(new ItemStack(Items.PISTON),
                        List.of(new ItemStack(Items.DIAMOND_SHOVEL), new ItemStack(Items.DIAMOND_PICKAXE))), level),
                "ordered FIRST match is retained, not an optimal/sorted bipartite reassignment");
        test.assertTrue(InfusionMatching.centralStillMatches(worn(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_PICKAXE)),
                "central retention still ignores wear; component rule is not global");

        for (String predicates : List.of("[{\"component\":-1,\"damage\":0}]", "[{\"component\":4,\"damage\":0}]",
                "[{\"component\":2,\"damage\":-1}]", "[{\"component\":2,\"damage\":0},{\"component\":2,\"damage\":1}]",
                "[{\"component\":0,\"damage\":0}]", "[{\"component\":2,\"damage\":1561}]",
                "[{\"component\":2}]", "[{\"component\":2,\"damage\":\"pristine\"}]", "{}")) {
            var bad = json.deepCopy(); bad.add("component_predicates", JsonParser.parseString(predicates));
            test.assertTrue(InfusionStackRecipe.CODEC.codec().parse(ops, bad).error().isPresent(),
                    "malformed/out-of-range/duplicate/non-damageable declaration is a decode error: " + predicates);
        }
        for (var invalid : List.of(List.of(new Declaration(-1, 0)), List.of(new Declaration(4, 0)),
                List.of(new Declaration(2, -1)), List.of(new Declaration(0, 0)),
                List.of(new Declaration(2, 1561)), List.of(new Declaration(2, 0), new Declaration(2, 0)))) {
            wire = rawRecipeWire(level, original, invalid, invalid.size());
            boolean rejected = false;
            try { InfusionStackRecipe.Serializer.STREAM_CODEC.decode(wire); }
            catch (IllegalArgumentException | IllegalStateException expected) { rejected = true; }
            finally { wire.release(); }
            test.assertTrue(rejected, "native wire rejects invalid indexed damage, never weakens it");
            var invalidRecipe = new InfusionStackRecipe(original.research(), original.input(), original.components(),
                    original.aspects(), original.instability(), original.result(), invalid);
            test.assertTrue(!invalidRecipe.matches(offered(new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_SHOVEL)), level),
                    "direct malformed declaration cannot bypass actual recipe matching");
            test.assertTrue(InfusionStackRecipe.CODEC.codec().encodeStart(ops, invalidRecipe).error().isPresent(),
                    "direct Java construction cannot bypass JSON encoder validation");
            wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess()); rejected = false;
            try { InfusionStackRecipe.Serializer.STREAM_CODEC.encode(wire, invalidRecipe); }
            catch (IllegalArgumentException | IllegalStateException expected) { rejected = true; }
            finally { wire.release(); }
            test.assertTrue(rejected, "direct Java construction cannot bypass native wire encoder validation");
        }
        for (int count : new int[] {-1, 5, Integer.MAX_VALUE}) {
            wire = rawRecipeWire(level, original, List.of(), count);
            boolean rejected = false;
            try { InfusionStackRecipe.Serializer.STREAM_CODEC.decode(wire); }
            catch (IllegalArgumentException expected) { rejected = true; }
            finally { wire.release(); }
            test.assertTrue(rejected, "unchecked predicate counts are rejected BEFORE allocation/read: " + count);
        }
        test.succeed();
    }

    private static void matchingCases(GameTestHelper test, InfusionStackRecipe recipe) {
        var level = test.getLevel();
        var pick = decorated(level, Items.DIAMOND_PICKAXE);
        var shovel = decorated(level, Items.DIAMOND_SHOVEL);
        pick.setDamageValue(9); pick.setDamageValue(0); // Repaired, still named/enchanted/custom-data bearing.
        test.assertTrue(recipe.matches(offered(pick, shovel), level), "repaired pristine tools accept ALL extra name/enchantment/custom data");
        test.assertTrue(!recipe.matches(offered(worn(Items.DIAMOND_PICKAXE), shovel), level)
                        && !recipe.matches(offered(pick, worn(Items.DIAMOND_SHOVEL)), level),
                "damage1 of EITHER exact diamond tool rejects independently");
        test.assertTrue(!recipe.matches(offered(new ItemStack(Items.IRON_PICKAXE), shovel), level)
                        && !recipe.matches(offered(pick, new ItemStack(Items.IRON_SHOVEL)), level)
                        && !recipe.matches(offered(new ItemStack(Items.DIAMOND_SHOVEL), shovel), level),
                "wrong material/tool remains invalid despite pristine damage");
        var swapped = new ArrayList<>(offered(pick, shovel).components()); Collections.reverse(swapped);
        test.assertTrue(recipe.matches(new InfusionRecipeInput(new ItemStack(Items.PISTON), swapped), level),
                "indices refer to declarations, NOT offered pedestal order");
        var extra = new ArrayList<>(swapped); extra.add(new ItemStack(Items.GOLD_INGOT));
        test.assertTrue(!recipe.matches(new InfusionRecipeInput(new ItemStack(Items.PISTON), extra), level)
                        && !recipe.matches(new InfusionRecipeInput(new ItemStack(Items.PISTON),
                        List.of(new ItemStack(Items.GOLD_INGOT, 2), pick, shovel)), level),
                "extra occupied pedestal rejects, and one stack of two cannot replace two occupied pedestals");
    }

    /** Suggested wrapper ID: infusion_component_predicate_session. Synchronous
     * native ticker fixture; not elapsed-world/client-time or chunk-unload proof. */
    public static void selectedSessionReloadAndReplacement(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        var manager = test.getLevel().getRecipeManager();
        var originalRecipes = new ArrayList<>(manager.getRecipes());
        try (var f = new Fixture(test)) {
            f.open();
            try (var scene = new Scene(test)) {
                KnowledgeAccess.get(f.player).addResearch("INFUSION");
                f.player.setPos(Vec3.atCenterOf(scene.matrixPos).add(0, 0, 2));
                scene.supply(true);
                var recipe = fixtureRecipe(true);
                manager.replaceRecipes(List.of(new RecipeHolder<>(FIXTURE_ID, recipe)));
                var matrix = scene.matrix(); matrix.setActive(true);
                scene.component(2).setItem(0, worn(Items.DIAMOND_PICKAXE));
                var before = matrix.saveWithoutMetadata(f.level.registryAccess());
                test.assertTrue(!matrix.startCrafting(f.player) && before.equals(matrix.saveWithoutMetadata(f.level.registryAccess()))
                                && scene.stock() == 64, "worn initial tool refuses actual start without payment/snapshot/output");
                scene.component(2).setItem(0, decorated(f.level, Items.DIAMOND_PICKAXE));
                test.assertTrue(matrix.startCrafting(f.player) && matrix.getDemand().visSize() == 3 && scene.stock() == 64,
                        "actual manager start selects declaration with no initial debit");
                // Remove the test recipe immediately: the selected session must
                // never rematch or reacquire predicates/cost/output from a recipe ID.
                manager.replaceRecipes(originalRecipes);
                ticks(matrix, 30);
                test.assertTrue(scene.stock() == 61 && matrix.getDemand().visSize() == 0 && matrix.getDemand().size() == 2,
                        "three genuine atomic source debits precede component consumption, retaining both zero cost keys");
                for (int tick = 0; tick < 120 && matrix.getRemainingIngredientCount() > 2; tick++) tick(matrix);
                test.assertTrue(matrix.getRemainingIngredientCount() == 2 && scene.component(0).isEmpty() && scene.component(1).isEmpty()
                                && scene.stock() == 61, "two EARLIER declarations really consume before saving the tool requirements");
                ticks(matrix, 10);
                test.assertTrue(matrix.getIngredientCountdown() == 5, "actual first tool beam has begun without consuming it");
                var wornPick = worn(Items.DIAMOND_PICKAXE);
                var wornShovel = worn(Items.DIAMOND_SHOVEL);
                scene.component(2).setItem(0, wornPick.copy()); scene.component(3).setItem(0, wornShovel.copy());
                ticks(matrix, 10);
                test.assertTrue(matrix.getRemainingIngredientCount() == 2 && matrix.getDemand().visSize() > 0
                                && ItemStack.matches(scene.component(2).getItem(0), wornPick)
                                && ItemStack.matches(scene.component(3).getItem(0), wornShovel),
                        "post-start worn replacements stall ACTUAL native consumption, including the in-flight beam");
                CompoundTag saved = matrix.saveWithFullMetadata(f.level.registryAccess());
                var remaining = saved.getList("recipein", Tag.TAG_COMPOUND);
                test.assertTrue(remaining.size() == 2 && remaining.getCompound(0).contains("damage", Tag.TAG_INT)
                                && remaining.getCompound(0).getInt("damage") == 0 && remaining.getCompound(1).getInt("damage") == 0
                                && !saved.contains("count") && !saved.contains("IngredientCountdown"),
                        "shifted remaining entries retain paired damage0; transient countdown is NOT newly persisted");
                int demand = matrix.getDemand().visSize();
                matrix = scene.reload(saved);
                test.assertTrue(matrix.getRemainingIngredientCount() == 2 && matrix.getDemand().visSize() == demand
                                && matrix.getRecipeCost().visSize() == 3 && matrix.getIngredientCountdown() == 0 && matrix.getTickCount() == 0,
                        "registered native NBT load retains exact paid progress/cost/requirements without rematch or countdown persistence");
                ticks(matrix, 120);
                int afterStall = scene.stock();
                test.assertTrue(matrix.isCrafting() && matrix.isActive() && matrix.getRemainingIngredientCount() == 2
                                && scene.center().getItem(0).is(Items.PISTON) && afterStall <= 61
                                && ItemStack.matches(scene.component(2).getItem(0), wornPick)
                                && ItemStack.matches(scene.component(3).getItem(0), wornShovel),
                        "reloaded selected predicate still refuses worn tools; already-paid essentia is NEVER refunded during original shortages");
                demand = matrix.getDemand().visSize();
                matrix = scene.reload(matrix.saveWithFullMetadata(f.level.registryAccess()));
                test.assertTrue(scene.stock() == afterStall && matrix.getDemand().visSize() == demand && matrix.getRecipeCost().visSize() == 3,
                        "saving a stalled paid session cannot replenish sources or reset its outstanding original surcharge demand");
                scene.component(2).setItem(0, decorated(f.level, Items.DIAMOND_PICKAXE));
                scene.component(3).setItem(0, decorated(f.level, Items.DIAMOND_SHOVEL));
                for (int tick = 0; tick < 1000 && matrix.isCrafting(); tick++) tick(matrix);
                test.assertTrue(!matrix.isCrafting() && scene.center().getItem(0).is(Items.DIAMOND)
                                && scene.center().getItem(0).getCount() == 1 && matrix.getRemainingIngredientCount() == 0
                                && scene.component(2).isEmpty() && scene.component(3).isEmpty() && scene.stock() <= afterStall,
                        "restoring genuine named/enchanted pristine tools allows real consumption and ONE completion, no paid refund");

                // Independent legacy-field-absent selected session, not a rewind
                // of the completed paid craft. Its worn tools must still consume.
                scene.supply(false);
                manager.replaceRecipes(List.of(new RecipeHolder<>(FIXTURE_ID, fixtureRecipe(false))));
                test.assertTrue(matrix.startCrafting(f.player), "legacy constructor starts with worn tools");
                saved = matrix.saveWithFullMetadata(f.level.registryAccess());
                test.assertTrue(saved.getList("recipein", Tag.TAG_COMPOUND).stream()
                                .noneMatch(entry -> ((CompoundTag) entry).contains("damage")), "old selected entries omit the optional field entirely");
                manager.replaceRecipes(originalRecipes);
                matrix = scene.reload(saved);
                for (int tick = 0; tick < 1000 && matrix.isCrafting(); tick++) tick(matrix);
                test.assertTrue(!matrix.isCrafting() && scene.center().getItem(0).is(Items.DIAMOND)
                                && matrix.getRemainingIngredientCount() == 0, "old native session reload still consumes worn unqualified ingredients");

                // New independent malformed-save fixture: explicit wrong-typed
                // damage cannot decode as 'absent', consume, or spend another unit.
                scene.supply(true);
                manager.replaceRecipes(List.of(new RecipeHolder<>(FIXTURE_ID, recipe)));
                test.assertTrue(matrix.startCrafting(f.player), "new valid session supplies malformed-load test input");
                saved = matrix.saveWithFullMetadata(f.level.registryAccess());
                saved.getList("recipein", Tag.TAG_COMPOUND).getCompound(2).putString("damage", "not-an-integer");
                int stock = scene.stock();
                matrix = scene.reload(saved); ticks(matrix, 10);
                test.assertTrue(!matrix.isActive() && matrix.isCrafting() && matrix.getRemainingIngredientCount() == 4
                                && scene.stock() == stock && scene.center().getItem(0).is(Items.PISTON)
                                && scene.component(2).getItem(0).is(Items.DIAMOND_PICKAXE),
                        "malformed explicit selected requirement fails CLOSED before payment/consume/output, never downgraded to plain ingredient");
            }
        } finally { manager.replaceRecipes(originalRecipes); }
        test.succeed();
    }

    private static InfusionStackRecipe fixtureRecipe(boolean qualified) {
        var costs = new LinkedHashMap<String, Integer>(); costs.put("aer", 2); costs.put("ordo", 1);
        var ingredients = List.of(Ingredient.of(Items.GOLD_INGOT), Ingredient.of(Items.GOLD_INGOT),
                Ingredient.of(Items.DIAMOND_PICKAXE), Ingredient.of(Items.DIAMOND_SHOVEL));
        return qualified ? new InfusionStackRecipe("INFUSION", Ingredient.of(Items.PISTON), ingredients, costs, 0,
                new ItemStack(Items.DIAMOND), List.of(new Declaration(2, 0), new Declaration(3, 0)))
                : new InfusionStackRecipe("INFUSION", Ingredient.of(Items.PISTON), ingredients, costs, 0, new ItemStack(Items.DIAMOND));
    }

    private static InfusionRecipeInput offered(ItemStack pick, ItemStack shovel) {
        return new InfusionRecipeInput(new ItemStack(Items.PISTON),
                List.of(new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT), pick, shovel));
    }
    private static ItemStack worn(Item item) { var stack = new ItemStack(item); stack.setDamageValue(1); return stack; }
    private static ItemStack decorated(ServerLevel level, Item item) {
        var stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("test-only-pristine-extra-components"));
        var data = new CompoundTag(); data.putString("Unrelated", "allowed, not an equality requirement");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 1);
        return stack;
    }

    /** Deliberately BYPASSES encoder validation to test actual native decoder errors. */
    private static RegistryFriendlyByteBuf rawRecipeWire(ServerLevel level, InfusionStackRecipe recipe, List<Declaration> declarations, int count) {
        var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        wire.writeUtf(recipe.research()); Ingredient.CONTENTS_STREAM_CODEC.encode(wire, recipe.input());
        wire.writeVarInt(recipe.components().size());
        for (var ingredient : recipe.components()) Ingredient.CONTENTS_STREAM_CODEC.encode(wire, ingredient);
        wire.writeVarInt(recipe.aspects().size());
        recipe.aspects().forEach((aspect, amount) -> { wire.writeUtf(aspect); wire.writeVarInt(amount); });
        wire.writeVarInt(recipe.instability()); ItemStack.STREAM_CODEC.encode(wire, recipe.result());
        wire.writeVarInt(count);
        // Current native layout: four fields per predicate (water-potion and empty-jar flags
        // were appended by the Alchemy core), then the optional central input_damage marker
        // and the optional result-patch marker (ADVANCEDGOLEM).
        for (var declaration : declarations) {
            wire.writeVarInt(declaration.component()); wire.writeVarInt(declaration.damage());
            wire.writeBoolean(declaration.waterPotion()); wire.writeBoolean(declaration.emptyJar());
        }
        wire.writeBoolean(false);
        wire.writeBoolean(false);
        return wire;
    }

    private static void tick(TileInfusionMatrix matrix) {
        var ticker = matrix.getBlockState().getTicker(matrix.getLevel(), ThaumcraftInfusion.MATRIX_BLOCK_ENTITY.get());
        if (ticker == null) throw new IllegalStateException("Missing real native matrix ticker");
        ticker.tick(matrix.getLevel(), matrix.getBlockPos(), matrix.getBlockState(), matrix);
    }
    private static void ticks(TileInfusionMatrix matrix, int count) { for (int index = 0; index < count; index++) tick(matrix); }

    /** Bounded isolated server scene, snapshot/restored before the actor fixture
     * closes. Only explicit source stock (32air+32order), raw tools/materials and
     * formed altar/stabilisers are test inputs. Never a user/saved-world fixture. */
    private static final class Scene implements AutoCloseable {
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos centerPos, matrixPos;
        final List<BlockPos> components;
        final Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        final List<TileJar> sources = new ArrayList<>();
        Scene(GameTestHelper test) {
            this.test = test; level = test.getLevel();
            centerPos = test.absolutePos(new BlockPos(8, 3, 8)); matrixPos = centerPos.above(2);
            components = List.of(centerPos.offset(-3, 0, 0), centerPos.offset(0, 0, -3), centerPos.offset(0, 0, 3), centerPos.offset(3, 0, 0));
            for (int dx = -12; dx <= 12; dx++) for (int dz = -12; dz <= 12; dz++) {
                test.assertTrue(level.hasChunkAt(matrixPos.offset(dx, 0, dz)), "native scan/source footprint is already loaded; no chunk-forcing fixture");
            }
            for (var pos : BlockPos.betweenClosed(matrixPos.offset(-12, -12, -12), matrixPos.offset(12, 11, 12))) {
                test.assertTrue(!(level.getBlockEntity(pos) instanceof IAspectSource),
                        "isolated native source-search footprint contains no foreign stock that the matrix could debit");
            }
            try {
                for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
                    var pos = centerPos.offset(dx, 0, dz);
                    put(pos.below(), Blocks.STONE.defaultBlockState());
                    put(pos.above(), Blocks.AIR.defaultBlockState());
                    put(pos, Math.abs(dx) <= 1 && Math.abs(dz) <= 1 || components.contains(pos)
                            ? Blocks.AIR.defaultBlockState() : Blocks.SKELETON_SKULL.defaultBlockState());
                }
                put(centerPos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
                put(matrixPos, ThaumcraftInfusion.MATRIX.get().defaultBlockState());
                for (int dx : new int[] {-1, 1}) for (int dz : new int[] {-1, 1}) {
                    put(centerPos.offset(dx, 0, dz), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.LOWER));
                    put(centerPos.offset(dx, 1, dz), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.UPPER));
                }
                for (var pos : components) put(pos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
                for (int index = 0; index < 2; index++) {
                    var pos = centerPos.offset(-1 + index * 2, 0, 6);
                    put(pos, ThaumcraftContent.JAR.get().defaultBlockState());
                    var source = (TileJar) level.getBlockEntity(pos); sources.add(source);
                    test.assertTrue(source.addToContainer(index == 0 ? Aspect.AIR : Aspect.ORDER, 32) == 0, "declared finite native source fixture stock");
                }
                var scan = InfusionStructure.scan(level, matrixPos);
                test.assertTrue(scan.pedestals().size() == 4 && scan.pedestals().containsAll(components)
                                && scan.symmetry() < -12 && InfusionStructure.isValid(level, matrixPos),
                        "only four owned peripheral pedestals; genuine symmetric skulls suppress hazards without editing instability/RNG");
            } catch (RuntimeException | Error failure) { close(); throw failure; }
        }
        TileInfusionMatrix matrix() { return (TileInfusionMatrix) level.getBlockEntity(matrixPos); }
        TilePedestal center() { return (TilePedestal) level.getBlockEntity(centerPos); }
        TilePedestal component(int index) { return (TilePedestal) level.getBlockEntity(components.get(index)); }
        int stock() { return sources.stream().mapToInt(TileJar::getAmount).sum(); }
        void supply(boolean pristine) {
            center().setItem(0, new ItemStack(Items.PISTON));
            component(0).setItem(0, new ItemStack(Items.GOLD_INGOT)); component(1).setItem(0, new ItemStack(Items.GOLD_INGOT));
            component(2).setItem(0, pristine ? decorated(level, Items.DIAMOND_PICKAXE) : worn(Items.DIAMOND_PICKAXE));
            component(3).setItem(0, pristine ? decorated(level, Items.DIAMOND_SHOVEL) : worn(Items.DIAMOND_SHOVEL));
        }
        TileInfusionMatrix reload(CompoundTag saved) {
            var previous = matrix(); level.removeBlockEntity(matrixPos);
            var loaded = BlockEntity.loadStatic(matrixPos, level.getBlockState(matrixPos), saved, level.registryAccess());
            test.assertTrue(loaded instanceof TileInfusionMatrix, "registered native matrix factory load, not a fake recipe/session model");
            level.setBlockEntity(loaded);
            test.assertTrue(loaded != previous && previous.isRemoved(), "old matrix identity is retired before selected session resumes");
            return (TileInfusionMatrix) loaded;
        }
        void put(BlockPos mutable, BlockState state) {
            var pos = mutable.immutable();
            if (!states.containsKey(pos)) {
                test.assertTrue(level.getBlockEntity(pos) == null, "new scene refuses to overwrite any pre-existing block entity");
                states.put(pos, level.getBlockState(pos));
            }
            level.setBlock(pos, state, QUIET);
        }
        @Override public void close() {
            if (states.containsKey(matrixPos) && level.getBlockEntity(matrixPos) instanceof TileInfusionMatrix matrix) {
                // Failure cleanup only: suppress original removal explosion, not
                // progress/payment. No altered session is used by an assertion.
                var stopped = matrix.saveWithFullMetadata(level.registryAccess());
                stopped.putBoolean("crafting", false); stopped.putBoolean("active", false);
                matrix.loadWithComponents(stopped, level.registryAccess());
            }
            for (var pos : states.keySet()) if (level.getBlockEntity(pos) instanceof TilePedestal pedestal) pedestal.clearContent();
            var restore = new ArrayList<>(states.keySet()); Collections.reverse(restore);
            for (var pos : restore) level.setBlock(pos, states.get(pos), QUIET);
        }
    }
}
