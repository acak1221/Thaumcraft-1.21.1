package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.EarlyObjectAspectBindings;
import thaumcraft.common.lib.research.ObjectAspectCatalog;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileJar;

/** Test-only mechanical source prerequisite, not Bore registration/acquisition.
 * SERVER INPUTS: four bare pistons, ONE coal, one EMPTY reusable phial, empty
 * furnace/five filtered alembics/six filtered jars. No supplied essentia, filled
 * phials, research, XP or vis. Native ticker calls are NOT elapsed-client time. */
public final class BorePistonAspectRuntimeChecks {
    private static final ResourceLocation PISTON = ResourceLocation.fromNamespaceAndPath("minecraft", "piston");
    private static final int PISTONS = 4;
    // The furnace only visits five alembics: Metallum is intentionally retained
    // in its mixed buffer until the emptied Motus alembic is reconfigured.
    private static final List<Aspect> TYPES = List.of(Aspect.MOTION, Aspect.MECHANISM, Aspect.TREE, Aspect.EARTH, Aspect.ENTROPY, Aspect.METAL);
    private BorePistonAspectRuntimeChecks() {}

    /** Root-owned suggested wrapper ID: bore_piston_aspects_distillation. */
    public static void pistonRecipeTagsAndFiniteDistillation(GameTestHelper test) {
        AspectList perItem = registeredRecipeTagsAndCache(test);
        requireOwnedFootprint(test);
        try (var f = new Fixture(test)) {
            // Extend ONLY this test fixture's restoration set for the two upper
            // alembics outside its ordinary six-cell-high shadow. Preflight above
            // already refused all foreign BEs/stock and unloaded cells.
            for (int depth = 4; depth <= 5; depth++) {
                var pos = f.base.above(depth);
                f.states.put(pos, f.level.getBlockState(pos));
            }
            f.open();
            var knowledge = KnowledgeAccess.get(f.player).save();
            int xp = f.player.totalExperience;
            f.put(f.base.below(), Blocks.STONE);
            f.put(f.base, ThaumcraftDistillation.ALCHEMY_FURNACE.get());
            var furnace = (TileAlchemyFurnace) f.level.getBlockEntity(f.base);
            var alembics = new ArrayList<TileAlembic>();
            var jars = new ArrayList<TileJar>();
            for (int index = 0; index < 5; index++) {
                var pos = f.base.above(index + 1);
                f.put(pos, ThaumcraftAlembics.ALEMBIC.get());
                var alembic = (TileAlembic) f.level.getBlockEntity(pos);
                alembics.add(alembic);
                f.check(alembic.getAmount() == 0 && alembic.getFilter() == null, "fresh owned alembic has no supplied stock/filter");
                alembic.setFilter(TYPES.get(index));
                f.check(alembic.getAmount() == 0 && alembic.getAspect() == TYPES.get(index), "fixture filter seeds ONLY a zero-amount native type");
            }
            for (int index = 0; index < TYPES.size(); index++) {
                var pos = f.base.offset(2, 0, index - 2);
                f.put(pos.below(), Blocks.STONE); f.put(pos, ThaumcraftContent.JAR.get());
                var jar = (TileJar) f.level.getBlockEntity(pos); jars.add(jar);
                f.check(jar.getAmount() == 0 && jar.getFilter() == null && !jar.isVoid(), "fresh ordinary owned jar has zero stock");
                f.check(jar.setFilter(TYPES.get(index)) && jar.getAmount() == 0, "typed jar fixture configuration supplies NO essence");
            }
            f.check(furnace.getVis() == 0 && furnace.isEmpty() && furnace.getBurnTime() == 0 && furnace.getCookTime() == 0,
                    "new native furnace has no hidden stock, fuel or progress");
            furnace.setItem(TileAlchemyFurnace.INPUT_SLOT, new ItemStack(Items.PISTON, PISTONS));
            furnace.setItem(TileAlchemyFurnace.FUEL_SLOT, new ItemStack(Items.COAL));
            f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get()));
            f.check(TileAlchemyFurnace.getFuelTime(furnace.getItem(TileAlchemyFurnace.FUEL_SLOT)) == 1600,
                    "actual registered coal fuel is1600 ticks, not replaced or rewritten for this fixture");
            var run = new Distillation(f, furnace, alembics, jars, perItem);
            run.step();
            f.check(furnace.getBurnTime() == 1600 && furnace.getCookTime() == 1 && furnace.getSmeltTime() == 170
                            && furnace.getItem(TileAlchemyFurnace.INPUT_SLOT).getCount() == PISTONS,
                    "one actual coal lights a17-vis/170-tick bare piston cook; no early output");
            for (int tick = 0; tick < 2400 && !firstFiveComplete(run); tick++) run.step();
            f.check(firstFiveComplete(run) && run.ticks == 840,
                    "four native170-tick cooks and genuine40-tick extraction passes reach the exact first-five result at840 calls");
            f.check(furnace.getAspects().aspects.equals(Map.of(Aspect.METAL, 12)) && furnace.getVis() == 12,
                    "the SIXTH collateral is retained as actual Metallum12 in the furnace, not culled/discarded to fit five alembics");

            int batches = 0;
            for (int index = 0; index < 5; index++) {
                batches += transferWholePhials(run, alembics.get(index), jars.get(index), TYPES.get(index), index == 0);
            }
            f.check(batches == 6 && jars.get(0).getAmount() == 16 && jars.get(1).getAmount() == 8
                            && jars.get(2).getAmount() == 8 && alembics.get(3).getAmount() == 4 && alembics.get(4).getAmount() == 4,
                    "actual first six phial exchanges retain all Motus16/Machina8/Arbor8 and the two four-unit collateral remainders");
            var reused = alembics.getFirst();
            f.check(reused.getAmount() == 0 && reused.getAspect() == null, "only the genuinely emptied Motus buffer can be reused");
            reused.setFilter(Aspect.METAL);
            f.check(reused.getAmount() == 0 && reused.getAspect() == Aspect.METAL, "empty filter change is NOT a stock grant");
            for (int tick = 0; tick < 800 && furnace.getVis() > 0; tick++) run.step();
            f.check(furnace.getVis() == 0 && reused.getAmount() == 12 && reused.getAspect() == Aspect.METAL && run.ticks == 1320,
                    "the same real furnace transfers the last12 metal in twelve40-tick passes, no reset or additional piston/fuel");
            batches += transferWholePhials(run, reused, jars.get(5), Aspect.METAL, false);
            run.conserved();
            f.check(batches == 7 && jars.stream().mapToInt(TileJar::getAmount).sum() == 56
                            && reused.getAmount() == 4 && alembics.get(3).getAmount() == 4 && alembics.get(4).getAmount() == 4
                            && alembics.stream().mapToInt(TileAlembic::getAmount).sum() == 12,
                    "all68 source units remain: jar56 plus alembic12, never a nominal Motus-only assertion");
            for (int index = 0; index < TYPES.size(); index++) {
                int expected = perItem.getAmount(TYPES.get(index)) * PISTONS / PhialData.AMOUNT * PhialData.AMOUNT;
                f.check(jars.get(index).getAspect() == TYPES.get(index) && jars.get(index).getAmount() == expected,
                        "complete exact typed jar collateral: " + TYPES.get(index).getTag());
            }
            f.check(furnace.getItem(TileAlchemyFurnace.INPUT_SLOT).isEmpty() && furnace.getItem(TileAlchemyFurnace.FUEL_SLOT).isEmpty()
                            && furnace.getCurrentItemBurnTime() == 1600 && furnace.getBurnTime() == 281 && furnace.getCookTime() == 0
                            && !furnace.isSpeedBoost() && furnace.getBellowsCount() == 0,
                    "exact finite input/fuel ledger: four pistons and one coal spent;281 native burn ticks retained, no counter rewrite");
            f.check(f.player.getInventory().countItem(ThaumcraftContent.EMPTY_PHIAL.get()) == 1
                            && f.player.getInventory().countItem(ThaumcraftContent.FILLED_PHIAL.get()) == 0
                            && f.player.totalExperience == xp && KnowledgeAccess.get(f.player).save().equals(knowledge),
                    "the SAME one empty phial is returned through all exchanges; no filled-item, XP or research fixture reward");
            f.check(f.drops().isEmpty() && f.pendingRows().isEmpty(), "no loose or pending fixture essence/labels are silently lost");
        }
        test.succeed();
    }

    private static AspectList registeredRecipeTagsAndCache(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        var manager = level.getRecipeManager();
        var holder = manager.byKey(PISTON).orElseThrow();
        test.assertTrue(holder.value() instanceof ShapedRecipe && holder.value().getType() == RecipeType.CRAFTING,
                "the actual registered vanilla piston is a native shaped recipe, not a synthetic aspect fixture");
        var recipe = (ShapedRecipe) holder.value();
        test.assertTrue(recipe.getWidth() == 3 && recipe.getHeight() == 3 && recipe.getIngredients().size() == 9
                        && ItemStack.matches(recipe.getResultItem(level.registryAccess()), new ItemStack(Items.PISTON)),
                "actual piston shape3x3/output1 is inspected, not inferred from an ID string");
        Item[] cells = {Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS,
                Items.COBBLESTONE, Items.IRON_INGOT, Items.COBBLESTONE, Items.COBBLESTONE, Items.REDSTONE, Items.COBBLESTONE};
        var grid = NonNullList.withSize(9, ItemStack.EMPTY);
        AspectList sum = new AspectList();
        for (int index = 0; index < cells.length; index++) {
            grid.set(index, new ItemStack(cells[index]));
            test.assertTrue(recipe.getIngredients().get(index).test(grid.get(index)), "actual native piston ingredient cell:" + index);
            // The selected ingredients are plain direct bases (no equipment or
            // final-six cull), so their full resolver values are the recipe bases.
            sum.add(ObjectAspectResolver.get(grid.get(index), level));
        }
        var offered = CraftingInput.of(3, 3, grid);
        test.assertTrue(recipe.matches(offered, level) && ItemStack.matches(recipe.assemble(offered, level.registryAccess()), new ItemStack(Items.PISTON))
                        && manager.getRecipeFor(RecipeType.CRAFTING, offered, level).orElseThrow().id().equals(PISTON),
                "native manager really matches/assembles the original-shaped vanilla recipe; preview is NOT granted to the machine");
        test.assertTrue(sum.aspects.equals(Map.of(Aspect.TREE, 3, Aspect.EARTH, 4, Aspect.ENTROPY, 4,
                        Aspect.METAL, 4, Aspect.ENERGY, 2, Aspect.MECHANISM, 1)),
                "complete actual ingredient sum before per-aspect0.75 truncation");
        AspectList scaled = new AspectList();
        for (Aspect aspect : sum.getAspects()) {
            int amount = (int) (sum.getAmount(aspect) * .75F / recipe.getResultItem(level.registryAccess()).getCount());
            if (amount > 0) scaled.add(aspect, amount);
        }
        test.assertTrue(scaled.aspects.equals(Map.of(Aspect.TREE, 2, Aspect.EARTH, 3, Aspect.ENTROPY, 3, Aspect.METAL, 3, Aspect.ENERGY, 1)),
                "ingredient Machina1 becomes ZERO and is removed BEFORE the raw complex addition");
        var raw = EarlyObjectAspectBindings.complex().get(PISTON);
        test.assertTrue(raw != null && raw.aspects.equals(Map.of(Aspect.MECHANISM, 2, Aspect.MOTION, 4))
                        && !EarlyObjectAspectBindings.direct().containsKey(PISTON)
                        && !ObjectAspectCatalog.registeredIds().contains(PISTON) && ObjectAspectCatalog.get(PISTON).size() == 0,
                "original row332 is only a raw complex addition; legacy partial/direct fallback cannot bypass recipe derivation");
        var beforeCull = scaled.copy().add(raw);
        test.assertTrue(beforeCull.size() == 7 && beforeCull.visSize() == 18 && beforeCull.getAmount(Aspect.ENERGY) == 1,
                "raw Machina2/Motus4 is added at the unscaled stage; bare piston has no equipment/container/enchantment bonus or cap64 change");
        var actual = ObjectAspectResolver.get(new ItemStack(Items.PISTON), level);
        test.assertTrue(actual.aspects.equals(finalPiston().aspects) && actual.visSize() == 17 && actual.getAmount(Aspect.ENERGY) == 0,
                "actual full final tags remove ONLY uniquely weakest Potentia1 in the original weighted six-tag cull");
        var mutated = ObjectAspectResolver.get(new ItemStack(Items.PISTON, 64), level);
        test.assertTrue(mutated.aspects.equals(actual.aspects), "cached recipe index still resolves ONE piston regardless of stack count");
        mutated.add(Aspect.MOTION, 1000).remove(Aspect.METAL);
        test.assertTrue(ObjectAspectResolver.get(new ItemStack(Items.PISTON), level).aspects.equals(actual.aspects),
                "returned complete aspect lists cannot mutate later recipe/cache lookups");

        var originals = new ArrayList<>(manager.getRecipes());
        long pistonRecipes = originals.stream().filter(entry -> entry.value().getResultItem(level.registryAccess()).is(Items.PISTON)).count();
        test.assertTrue(pistonRecipes == 1, "isolated bundled manager has ONE actual piston recipe; no foreign recipe may change source value");
        try {
            // SERVER TEST ONLY: remove the actual recipe, never inject a fake.
            // Warm->changed->restored manager identity must invalidate its index.
            manager.replaceRecipes(originals.stream().filter(entry -> !entry.id().equals(PISTON)).toList());
            test.assertTrue(ObjectAspectResolver.get(new ItemStack(Items.PISTON), level).aspects.equals(raw.aspects),
                    "native recipe-index cache rebuilds when the real recipe disappears; no stale17/direct fallback survives");
        } finally { manager.replaceRecipes(originals); }
        test.assertTrue(ObjectAspectResolver.get(new ItemStack(Items.PISTON), level).aspects.equals(actual.aspects),
                "restoring EXACT real recipe holders restores full17 tags on the same RecipeManager, before any distillation");
        return actual;
    }

    private static AspectList finalPiston() {
        return new AspectList().add(Aspect.TREE, 2).add(Aspect.EARTH, 3).add(Aspect.ENTROPY, 3)
                .add(Aspect.METAL, 3).add(Aspect.MECHANISM, 2).add(Aspect.MOTION, 4);
    }

    private static boolean firstFiveComplete(Distillation run) {
        if (!run.furnace.getItem(TileAlchemyFurnace.INPUT_SLOT).isEmpty()) return false;
        for (int index = 0; index < 5; index++) {
            var source = run.alembics.get(index);
            if (source.getAspect() != TYPES.get(index) || source.getAmount() != run.perItem.getAmount(TYPES.get(index)) * PISTONS) return false;
        }
        return run.furnace.getAspects().aspects.equals(Map.of(Aspect.METAL, 12));
    }

    private static int transferWholePhials(Distillation run, TileAlembic source, TileJar target, Aspect type, boolean mismatch) {
        Fixture f = run.f;
        int batches = 0;
        while (source.getAmount() >= PhialData.AMOUNT) {
            int before = source.getAmount(), stored = target.getAmount();
            f.check(f.player.getMainHandItem().is(ThaumcraftContent.EMPTY_PHIAL.get()) && f.player.getMainHandItem().getCount() == 1,
                    "reuse only the actual returned empty phial, never supply a filled replacement");
            f.aim(Vec3.atCenterOf(source.getBlockPos()));
            f.check(f.use(source.getBlockPos()).consumesAction() && source.getAmount() == before - PhialData.AMOUNT,
                    "actual native alembic interaction withdraws exactly8:" + type.getTag());
            var held = f.player.getMainHandItem();
            var data = ItemEssence.data(held);
            f.check(held.is(ThaumcraftContent.FILLED_PHIAL.get()) && held.getCount() == 1 && data != null && data.tag().equals(type.getTag())
                            && ObjectAspectResolver.get(held, f.level).aspects.equals(Map.of(type, PhialData.AMOUNT)),
                    "the genuinely withdrawn typed phial has an empty direct base plus exactly its actual contained8");
            run.conserved();
            if (mismatch && batches == 0) {
                var wrong = run.jars.get(1);
                var wrongBefore = wrong.saveWithoutMetadata(f.level.registryAccess());
                var paidHand = held.copy(); f.aim(Vec3.atCenterOf(wrong.getBlockPos()));
                f.use(wrong.getBlockPos());
                f.check(wrongBefore.equals(wrong.saveWithoutMetadata(f.level.registryAccess())) && ItemStack.matches(paidHand, f.player.getMainHandItem()),
                        "Motus phial cannot enter the owned Machina-filter jar, with no partial debit/refund");
                run.conserved();
            }
            f.aim(Vec3.atCenterOf(target.getBlockPos()));
            f.check(f.use(target.getBlockPos()).consumesAction() && target.getAspect() == type && target.getAmount() == stored + PhialData.AMOUNT
                            && f.player.getMainHandItem().is(ThaumcraftContent.EMPTY_PHIAL.get()) && f.player.getMainHandItem().getCount() == 1,
                    "same actual phial inserts all8 through existing native jar exchange and returns ONE empty");
            ++batches; run.conserved();
        }
        if (source.getAmount() > 0) {
            var before = source.saveWithoutMetadata(f.level.registryAccess()); var held = f.player.getMainHandItem().copy();
            f.aim(Vec3.atCenterOf(source.getBlockPos())); f.use(source.getBlockPos());
            f.check(source.getAmount() == 4 && before.equals(source.saveWithoutMetadata(f.level.registryAccess()))
                            && ItemStack.matches(held, f.player.getMainHandItem()),
                    "real four-unit collateral cannot fill an8-unit phial and remains physically retained:" + type.getTag());
            run.conserved();
        }
        return batches;
    }

    private static final class Distillation {
        final Fixture f;
        final TileAlchemyFurnace furnace;
        final List<TileAlembic> alembics;
        final List<TileJar> jars;
        final AspectList perItem;
        int ticks;
        Distillation(Fixture f, TileAlchemyFurnace furnace, List<TileAlembic> alembics, List<TileJar> jars, AspectList perItem) {
            this.f = f; this.furnace = furnace; this.alembics = alembics; this.jars = jars; this.perItem = perItem.copy();
        }
        void step() {
            f.check(f.level.getBlockEntity(furnace.getBlockPos()) == furnace, "only the original installed furnace owns this finite input");
            var ticker = furnace.getBlockState().getTicker(f.level, ThaumcraftDistillation.ALCHEMY_FURNACE_BLOCK_ENTITY.get());
            if (ticker == null) throw new IllegalStateException("Missing actual native alchemy furnace ticker");
            ticker.tick(f.level, furnace.getBlockPos(), furnace.getBlockState(), furnace); ++ticks;
            f.check(furnace.getItem(TileAlchemyFurnace.FUEL_SLOT).isEmpty() && furnace.getCurrentItemBurnTime() == 1600
                            && furnace.getBurnTime() == Math.max(0, 1601 - ticks) && !furnace.isSpeedBoost(),
                    "native tick/fuel accounting cannot refill, accelerate or refund the one actual coal");
            conserved();
        }
        void conserved() {
            int left = furnace.getItem(TileAlchemyFurnace.INPUT_SLOT).getCount();
            f.check(left >= 0 && left <= PISTONS && (left == 0 || furnace.getItem(TileAlchemyFurnace.INPUT_SLOT).is(Items.PISTON)),
                    "remaining physical source is only the declared four bare pistons");
            AspectList expected = new AspectList(), stored = new AspectList();
            for (Aspect aspect : perItem.getAspects()) if (PISTONS > left) expected.add(aspect, perItem.getAmount(aspect) * (PISTONS - left));
            addPositive(stored, furnace.getAspects());
            for (TileAlembic source : alembics) {
                f.check(f.level.getBlockEntity(source.getBlockPos()) == source && !source.isRemoved(), "owned alembic identity remains installed");
                addPositive(stored, source.getAspects());
            }
            for (TileJar jar : jars) {
                f.check(f.level.getBlockEntity(jar.getBlockPos()) == jar && !jar.isRemoved(), "owned jar identity remains installed");
                addPositive(stored, jar.getAspects());
            }
            for (int slot = 0; slot < f.player.getInventory().getContainerSize(); slot++) {
                var stack = f.player.getInventory().getItem(slot);
                if (stack.is(ThaumcraftContent.FILLED_PHIAL.get())) {
                    var data = ItemEssence.data(stack);
                    f.check(data != null && Aspect.getAspect(data.tag()) != null, "every carried filled phial has genuine typed contents");
                    stored.add(Aspect.getAspect(data.tag()), PhialData.AMOUNT * stack.getCount());
                }
            }
            f.check(expected.aspects.equals(stored.aspects), "complete per-aspect conservation after native tick/exchange: expected=" + expected.aspects + " stored=" + stored.aspects);
        }
    }

    private static void addPositive(AspectList target, AspectList source) {
        for (Aspect aspect : source.getAspects()) if (source.getAmount(aspect) > 0) target.add(aspect, source.getAmount(aspect));
    }

    private static void requireOwnedFootprint(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        var base = test.absolutePos(new BlockPos(3, 3, 3)); var level = test.getLevel();
        var min = base.offset(-3, -2, -3); var max = base.offset(4, 6, 4);
        test.assertTrue(!level.isOutsideBuildHeight(min) && !level.isOutsideBuildHeight(max), "entire furnace/five-alembic/neighbor footprint lies inside build height");
        for (var pos : BlockPos.betweenClosed(min, max)) {
            test.assertTrue(level.hasChunkAt(pos), "machine/neighbor footprint must ALREADY be loaded; never force chunks for this source fixture");
            test.assertTrue(level.getBlockEntity(pos) == null, "refuse any foreign furnace/source/transport/BE before the shared fixture can overwrite it");
        }
        test.assertTrue(level.getEntitiesOfClass(ItemEntity.class,
                        new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max.offset(1, 1, 1)))).isEmpty(),
                "isolated source fixture contains no foreign loose items to merge with or silently clean up");
    }
}
