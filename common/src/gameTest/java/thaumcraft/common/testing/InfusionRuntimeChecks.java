package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.lib.infusion.InfusionStructure;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TilePedestal;

/** One bounded real-content check. Root alone supplies the loader wrapper/execution. */
public final class InfusionRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private InfusionRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos centerPos = test.absolutePos(new BlockPos(8, 3, 8)), matrixPos = centerPos.above(2);
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<BlockPos, CompoundTag> tags = new LinkedHashMap<>();
        List<ItemStack> inventory = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var knowledge = KnowledgeAccess.get(player).copy();
        Vec3 oldPosition = player.position();
        float yaw = player.getYRot(), pitch = player.getXRot(), head = player.yHeadRot, oldHead = player.yHeadRotO;
        GameType mode = player.gameMode.getGameModeForPlayer();
        int selected = player.getInventory().selected;
        boolean sneaking = player.isShiftKeyDown();
        AABB box = new AABB(matrixPos).inflate(24);
        Set<UUID> oldDrops = level.getEntitiesOfClass(ItemEntity.class, box).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        Cow victim = null;
        try {
            player.closeContainer();
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            KnowledgeAccess.get(player).addResearch("INFUSION");
            KnowledgeAccess.get(player).addResearch("ROD_silverwood");
            InfusionStackRecipe recipe = level.getRecipeManager().getAllRecipesFor(ThaumcraftInfusion.INFUSION_RECIPE_TYPE.get()).stream()
                    .map(holder -> holder.value()).filter(value -> value.research().equals("ROD_silverwood")).findFirst().orElseThrow();
            test.assertTrue(recipe.instability() == 5 && recipe.components().size() == 7 && recipe.aspects().size() == 7
                    && recipe.aspects().values().stream().allMatch(amount -> amount == 9) && recipe.aspectCost().visSize() == 63,
                    "registered original Silverwood recipe: seven components, seven costs of nine, instability five");

            List<BlockPos> peripheral = new ArrayList<>();
            for (int dx : new int[] {-3, 0, 3}) for (int dz : new int[] {-3, 0, 3}) {
                if (dx != 0 || dz != 0) peripheral.add(centerPos.offset(dx, 0, dz));
            }
            // Genuine original symmetric skull stabilisers, NOT a forced I=0
            // field or a bypass of the hazard branch. All stock/vis/research/raw
            // blocks here are fixtures; this is not a full survival verdict.
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
                BlockPos pos = centerPos.offset(dx, 0, dz);
                put(level, states, tags, pos.below(), Blocks.STONE.defaultBlockState());
                put(level, states, tags, pos.above(), Blocks.AIR.defaultBlockState());
                put(level, states, tags, pos, Math.abs(dx) <= 1 && Math.abs(dz) <= 1 || peripheral.contains(pos)
                        ? Blocks.AIR.defaultBlockState() : Blocks.SKELETON_SKULL.defaultBlockState());
            }
            for (BlockPos pos : BlockPos.betweenClosed(matrixPos.offset(-1, -2, -1), matrixPos.offset(1, 0, 1))) {
                put(level, states, tags, pos, Blocks.AIR.defaultBlockState());
            }
            put(level, states, tags, centerPos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
            put(level, states, tags, matrixPos, ThaumcraftInfusion.MATRIX.get().defaultBlockState());
            for (int dx : new int[] {-1, 1}) for (int dz : new int[] {-1, 1}) {
                put(level, states, tags, centerPos.offset(dx, 0, dz), ThaumcraftMasonry.ARCANE_STONE_BRICKS.get().defaultBlockState());
                put(level, states, tags, centerPos.offset(dx, 1, dz), ThaumcraftMasonry.ARCANE_STONE.get().defaultBlockState());
            }
            for (BlockPos pos : peripheral) put(level, states, tags, pos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());

            Map<Aspect, TileJar> sources = new LinkedHashMap<>();
            List<Aspect> types = new ArrayList<>(recipe.aspects().keySet().stream().map(Aspect::getAspect).toList());
            types.add(Aspect.CRYSTAL);
            for (int index = 0; index < types.size(); index++) {
                BlockPos pos = centerPos.offset(-8 + index, 0, 9);
                put(level, states, tags, pos.below(), Blocks.STONE.defaultBlockState());
                put(level, states, tags, pos, ThaumcraftContent.JAR.get().defaultBlockState());
                sources.put(types.get(index), (TileJar) level.getBlockEntity(pos));
            }
            // No finished essentia injection: 48 real primal shards are distilled
            // through the registered furnace/alembics, then transferred using
            // native phial actions. Vitreus48 is a real retained waste stream.
            BlockPos furnacePos = centerPos.offset(9, 0, -9);
            put(level, states, tags, furnacePos, ThaumcraftDistillation.ALCHEMY_FURNACE.get().defaultBlockState());
            for (int depth = 1; depth <= 3; depth++) put(level, states, tags, furnacePos.above(depth), ThaumcraftAlembics.ALEMBIC.get().defaultBlockState());
            distill(test, player, furnacePos, sources);
            test.assertTrue(stock(sources) == 192 && sources.get(Aspect.MAGIC).getAmount() == 48
                    && sources.get(Aspect.CRYSTAL).getAmount() == 48, "all 192 actual distilled units remain physically stored before infusion");

            ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
            Map<String, Integer> vis = new LinkedHashMap<>(), formation = new LinkedHashMap<>();
            for (String primal : WandData.PRIMALS) { vis.put(primal, 5000); formation.put(primal, 25); }
            wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", "gold", vis));
            player.getInventory().setItem(3, player.getMainHandItem().copy()); // Retain the reused physical empty phial.
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            var price = ArcaneVis.price(formation, wand, player);
            test.assertTrue(use(player, matrixPos, Direction.UP).consumesAction() && InfusionStructure.isValid(level, matrixPos), "native wand forms the registered blueprint");
            TileInfusionMatrix matrix = (TileInfusionMatrix) level.getBlockEntity(matrixPos);
            test.assertTrue(matrix.isActive() && !matrix.isCrafting(), "formation activates without starting or supplying a reward");
            for (String primal : WandData.PRIMALS) test.assertTrue(ItemWandCasting.data(player.getMainHandItem()).amount(primal) == 5000 - price.get(primal), "one canonical formation debit: " + primal);
            ItemStack paidWand = player.getMainHandItem().copy();

            TilePedestal center = (TilePedestal) level.getBlockEntity(centerPos);
            ItemStack input = new ItemStack(ThaumcraftNaturals.SILVERWOOD_LOG.get());
            CompoundTag extra = new CompoundTag();
            extra.putString("extra", "plain original input permits extra data");
            input.set(DataComponents.CUSTOM_DATA, CustomData.of(extra));
            center.setItem(0, input.copy());
            supplyComponents(level, recipe, peripheral);
            KnowledgeAccess.get(player).removeResearch("ROD_silverwood");
            var refusalInputs = peripheral.stream().map(pos -> ((TilePedestal) level.getBlockEntity(pos)).getItem(0).copy()).toList();
            test.assertTrue(!matrix.startCrafting(player) && !matrix.isCrafting() && stock(sources) == 192
                    && ItemStack.matches(input, center.getItem(0)) && ItemStack.matches(paidWand, player.getMainHandItem()),
                    "missing infusion research refuses before central/component/essentia/vis debit");
            for (int i = 0; i < peripheral.size(); i++) test.assertTrue(ItemStack.matches(refusalInputs.get(i),
                    ((TilePedestal) level.getBlockEntity(peripheral.get(i))).getItem(0)), "research refusal preserves pedestal " + i);
            KnowledgeAccess.get(player).addResearch("ROD_silverwood");
            var extraPedestal = (TilePedestal) level.getBlockEntity(peripheral.getLast());
            test.assertTrue(extraPedestal.isEmpty(), "eighth pedestal is outside the seven required components");
            extraPedestal.setItem(0, new ItemStack(Items.DIRT));
            test.assertTrue(!matrix.startCrafting(player) && !matrix.isCrafting() && stock(sources) == 192
                    && ItemStack.matches(input, center.getItem(0)) && extraPedestal.getItem(0).is(Items.DIRT),
                    "extra occupied pedestal refuses recipe admission with no input/essentia debit");
            for (int i = 0; i < peripheral.size() - 1; i++) test.assertTrue(ItemStack.matches(refusalInputs.get(i),
                    ((TilePedestal) level.getBlockEntity(peripheral.get(i))).getItem(0)), "extra-input refusal preserves pedestal " + i);
            extraPedestal.setItem(0, ItemStack.EMPTY);
            test.assertTrue(recipe.matches(new InfusionRecipeInput(input, recipe.components().stream().map(ingredient -> ingredient.getItems()[0]).toList()), level), "untagged original input accepts additional components/NBT");
            test.assertTrue(use(player, matrixPos, Direction.UP).consumesAction() && matrix.isCrafting()
                    && matrix.getDemand().visSize() == 63 && matrix.getInstability() == matrix.getSymmetry() + 5
                    && matrix.getInstability() < -7 && ItemStack.matches(paidWand, player.getMainHandItem()), "researched start snapshots the real cost, with unclamped original negative stability and no wand debit");
            for (int tick = 0; tick < 130; tick++) tick(matrix);
            test.assertTrue(matrix.getDemand().visSize() == 50 && matrix.getDemand().getAmount(Aspect.AIR) == 0
                    && matrix.getDemand().size() == 7 && stock(sources) == 179, "exactly thirteen units; insertion order and zero demand keys survive");
            var demandCopy = matrix.getDemand();
            demandCopy.add(Aspect.AIR, 100);
            test.assertTrue(matrix.getDemand().getAmount(Aspect.AIR) == 0, "demand getter is defensive");
            CompoundTag saved = matrix.saveWithFullMetadata(level.registryAccess());
            test.assertTrue(!saved.contains("count") && !saved.contains("IngredientCountdown") && saved.getInt("recipeinst") == 5
                    && saved.getCompound("recipeout").getInt("StoredCount") == 1 && saved.hasUUID("RecipePlayerId"), "persistent session excludes transient cycle/beam state");
            TileInfusionMatrix stale = matrix;
            level.removeBlockEntity(matrixPos);
            matrix = new TileInfusionMatrix(matrixPos, level.getBlockState(matrixPos));
            matrix.loadWithComponents(saved, level.registryAccess());
            level.setBlockEntity(matrix);
            test.assertTrue(matrix.isCrafting() && matrix.isActive() && matrix.getTickCount() == 0
                    && matrix.getDemand().visSize() == 50 && matrix.getRemainingIngredientCount() == 7
                    && !matrix.startCrafting(player) && !stale.startCrafting(player), "native reload resumes a frozen output/cost, not matching/debiting a new recipe");

            // Replace a source BE at its coordinates. Wireless must resolve the
            // current tile rather than retaining the original Java reference.
            TileJar oldJar = sources.get(Aspect.FIRE);
            CompoundTag jarTag = oldJar.saveWithFullMetadata(level.registryAccess());
            level.removeBlockEntity(oldJar.getBlockPos());
            TileJar newJar = new TileJar(ThaumcraftContent.JAR_BLOCK_ENTITY.get(), oldJar.getBlockPos(), oldJar.getBlockState());
            newJar.loadWithComponents(jarTag, level.registryAccess());
            level.setBlockEntity(newJar);
            sources.put(Aspect.FIRE, newJar);
            BlockPos corner = centerPos.offset(-1, 0, -1);
            BlockState lower = level.getBlockState(corner);
            put(level, states, tags, corner, Blocks.AIR.defaultBlockState());
            for (int tick = 0; tick < 20; tick++) tick(matrix);
            test.assertTrue(!matrix.isActive() && matrix.isCrafting() && matrix.getDemand().visSize() == 49, "tick10 can drain; tick20 broken structure pauses without clearing the session");
            for (int tick = 0; tick < 40; tick++) tick(matrix);
            test.assertTrue(matrix.getDemand().visSize() == 49 && stock(sources) == 178, "inactive session consumes nothing");
            put(level, states, tags, corner, lower);
            test.assertTrue(use(player, matrixPos, Direction.UP).consumesAction() && matrix.isActive() && matrix.isCrafting()
                    && matrix.getDemand().visSize() == 49 && ItemStack.matches(paidWand, player.getMainHandItem()), "repair plus free wand activation resumes, without a restart");
            for (int tick = 0; tick < 600 && matrix.getDemand().visSize() > 0; tick++) {
                tick(matrix);
                test.assertTrue(stock(sources) - matrix.getDemand().visSize() == 129, "each wireless commit debits source and demand together");
            }
            test.assertTrue(matrix.getDemand().visSize() == 0 && stock(sources) == 129 && matrix.getRemainingIngredientCount() == 7,
                    "all63 essentia are spent before the first physical ingredient");
            for (int tick = 0; tick < 10; tick++) tick(matrix);
            test.assertTrue(matrix.getIngredientCountdown() == 5 && matrix.getIngredientTarget() != null
                    && matrix.getRemainingIngredientCount() == 7, "first ingredient cycle emits its beam but consumes nothing");
            CompoundTag beamSave = matrix.saveWithFullMetadata(level.registryAccess());
            level.removeBlockEntity(matrixPos);
            matrix = new TileInfusionMatrix(matrixPos, level.getBlockState(matrixPos));
            matrix.loadWithComponents(beamSave, level.registryAccess());
            level.setBlockEntity(matrix);
            test.assertTrue(matrix.isCrafting() && matrix.getIngredientCountdown() == 0 && matrix.getIngredientTarget() == null
                    && matrix.getDemand().size() == 7 && matrix.getDemand().visSize() == 0 && matrix.getRemainingIngredientCount() == 7,
                    "mid-beam reload preserves all zero keys and remaining ingredients, but restarts only the transient countdown");
            int ingredientCycles = 0, consumed = 0;
            for (int tick = 0; tick < 500 && matrix.isCrafting(); tick++) {
                boolean cycle = (matrix.getTickCount() + 1) % 10 == 0;
                int before = matrix.getRemainingIngredientCount();
                tick(matrix);
                if (cycle && before > 0) {
                    ++ingredientCycles;
                    if (matrix.getRemainingIngredientCount() != before) {
                        ++consumed;
                        test.assertTrue(ingredientCycles == consumed * 6 && matrix.getRemainingIngredientCount() == before - 1,
                                "each component requires exactly six successful ten-tick cycles");
                    }
                }
                test.assertTrue(stock(sources) == 129, "ingredient/finish cycles do not debit essentia again");
            }
            ResourceLocation reward = ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand_rod_silverwood");
            test.assertTrue(!matrix.isCrafting() && center.getItem(0).is(BuiltInRegistries.ITEM.get(reward)) && center.getItem(0).getCount() == 1
                    && stock(sources) == 129 && sources.get(Aspect.MAGIC).getAmount() == 39 && sources.get(Aspect.CRYSTAL).getAmount() == 48
                    && ingredientCycles == 42 && consumed == 7
                    && peripheral.stream().allMatch(pos -> ((TilePedestal) level.getBlockEntity(pos)).isEmpty()), "one real Silverwood core costs exactly63 essentia and seven physical components, with all waste/surplus retained");
            for (String primal : WandData.PRIMALS) test.assertTrue(sources.get(Aspect.getAspect(primal)).getAmount() == 7, "seven surplus primal units: " + primal);

            // Preserve the earned core, then start a SECOND raw-input fixture to
            // exercise the original first-missing-cache-entry break/backoff.
            player.getInventory().setItem(2, center.removeItemNoUpdate(0));
            center.setItem(0, input.copy());
            supplyComponents(level, recipe, peripheral);
            TileJar first = sources.get(Aspect.AIR);
            test.assertTrue(level.destroyBlock(first.getBlockPos(), true), "native source-jar break");
            test.assertTrue(use(player, matrixPos, Direction.UP).consumesAction() && matrix.isCrafting(), "second start uses the registered recipe, not an injected session");
            do { tick(matrix); } while (matrix.getTickCount() % 10 != 0);
            test.assertTrue(matrix.getDemand().visSize() == 63 && matrix.getRemainingIngredientCount() == 7
                    && sources.entrySet().stream().filter(entry -> entry.getKey() != Aspect.AIR).mapToInt(entry -> entry.getValue().getAmount()).sum() == 122,
                    "a stale first source stops every drain even when later cached sources contain matching essentia");
            ItemEntity droppedJar = level.getEntitiesOfClass(ItemEntity.class, new AABB(first.getBlockPos()).inflate(2)).stream()
                    .filter(entity -> !oldDrops.contains(entity.getUUID()) && entity.getItem().is(ThaumcraftContent.JAR.get().asItem())).findFirst().orElseThrow();
            player.getInventory().setItem(1, player.getMainHandItem());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setPos(droppedJar.position());
            droppedJar.setNoPickUpDelay();
            droppedJar.playerTouch(player);
            test.assertTrue(droppedJar.isRemoved() && use(player, first.getBlockPos().below(), Direction.UP).consumesAction(), "actual physical jar pickup/placement, not source-content injection");
            TileJar restored = (TileJar) level.getBlockEntity(first.getBlockPos());
            EssentiaHandler.refreshSources(matrix);
            test.assertTrue(restored.getAmount() == 7 && !EssentiaHandler.drainEssentia(matrix, Aspect.AIR)
                    && restored.getAmount() == 7 && matrix.getDemand().visSize() == 63, "refresh clears coordinates but not the original five-second wall-clock shortage delay");

            // Invalid center MUST select a real hazard before aborting. Seed the
            // fixture's first nextInt(21) to9 (one native terrain-safe explosion);
            // production never rerolls, clamps I to0, or skips any outcome.
            center.setItem(0, new ItemStack(Items.DIRT));
            put(level, states, tags, matrixPos.east(), Blocks.GLASS.defaultBlockState());
            victim = EntityType.COW.create(level);
            test.assertTrue(victim != null, "native hazard victim exists");
            victim.setNoAi(true);
            victim.setPos(Vec3.atCenterOf(matrixPos));
            test.assertTrue(level.addFreshEntity(victim), "native hazard victim added");
            float health = victim.getHealth();
            player.setPos(Vec3.atCenterOf(matrixPos).add(20, 0, 20));
            long seed = 0;
            while (RandomSource.create(seed).nextInt(21) != 9) ++seed;
            level.random.setSeed(seed);
            for (int tick = 0; tick < 10 && matrix.isCrafting(); tick++) tick(matrix);
            test.assertTrue(!matrix.isCrafting() && matrix.getDemand().visSize() == 0 && matrix.getInstability() == 0
                    && matrix.getRemainingIngredientCount() == 7 && (victim.isRemoved() || victim.getHealth() < health)
                    && level.getBlockState(matrixPos.east()).is(Blocks.GLASS) && center.getItem(0).is(Items.DIRT),
                    "invalid center executes the selected real damaging, terrain-safe hazard then aborts without consuming ingredients or producing a reward");
        } finally {
            if (victim != null) victim.discard();
            // Cleanup is not recipe progress. Prevent the original power2 removal
            // blast from a failing assertion destroying neighboring fixture cells.
            if (level.getBlockEntity(matrixPos) instanceof TileInfusionMatrix matrix) {
                CompoundTag stop = matrix.saveWithFullMetadata(level.registryAccess());
                stop.putBoolean("crafting", false);
                stop.putBoolean("active", false);
                matrix.loadWithComponents(stop, level.registryAccess());
            }
            for (BlockPos pos : states.keySet()) {
                if (level.getBlockEntity(pos) instanceof TilePedestal pedestal) pedestal.clearContent();
                if (level.getBlockEntity(pos) instanceof TileAlchemyFurnace furnace) furnace.clearContent();
            }
            List<BlockPos> restore = new ArrayList<>(states.keySet());
            Collections.reverse(restore);
            for (BlockPos pos : restore) level.setBlock(pos, states.get(pos), QUIET);
            tags.forEach((pos, tag) -> { if (level.getBlockEntity(pos) != null) level.getBlockEntity(pos).loadWithComponents(tag, level.registryAccess()); });
            for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, box)) if (!oldDrops.contains(drop.getUUID())) drop.discard();
            EssentiaHandler.clear(level);
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            KnowledgeAccess.get(player).copyFrom(knowledge);
            player.getInventory().selected = selected;
            player.setGameMode(mode);
            player.setShiftKeyDown(sneaking);
            player.setPos(oldPosition);
            player.setYRot(yaw);
            player.setXRot(pitch);
            player.yHeadRot = head;
            player.yHeadRotO = oldHead;
        }
        test.succeed();
    }

    private static void distill(GameTestHelper test, ServerPlayer player, BlockPos pos, Map<Aspect, TileJar> sources) {
        ServerLevel level = player.serverLevel();
        TileAlchemyFurnace furnace = (TileAlchemyFurnace) level.getBlockEntity(pos);
        TileAlembic primal = (TileAlembic) level.getBlockEntity(pos.above());
        TileAlembic magic = (TileAlembic) level.getBlockEntity(pos.above(2));
        TileAlembic crystal = (TileAlembic) level.getBlockEntity(pos.above(3));
        furnace.setItem(1, new ItemStack(Items.COAL, 6));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get()));
        magic.setFilter(Aspect.MAGIC);
        crystal.setFilter(Aspect.CRYSTAL);
        for (String tag : WandData.PRIMALS) {
            Aspect aspect = Aspect.getAspect(tag);
            primal.setFilter(aspect);
            furnace.setItem(0, new ItemStack(ThaumcraftContent.SHARDS.get(tag).get(), 8));
            for (int tick = 0; tick < 2400 && (!furnace.getItem(0).isEmpty() || furnace.getVis() > 0); tick++) TileAlchemyFurnace.tick(level, pos, furnace.getBlockState(), furnace);
            test.assertTrue(furnace.getItem(0).isEmpty() && furnace.getVis() == 0 && primal.getAmount() == 16
                    && magic.getAmount() == 8 && crystal.getAmount() == 8, "eight real " + tag + " shards distill to16primal+8magic+8vitreus");
            for (TileAlembic alembic : List.of(primal, magic, crystal)) {
                Aspect type = alembic.getAspect();
                while (alembic.getAmount() >= 8) {
                    test.assertTrue(player.getMainHandItem().is(ThaumcraftContent.EMPTY_PHIAL.get())
                            && use(player, alembic.getBlockPos(), Direction.UP).consumesAction()
                            && player.getMainHandItem().is(ThaumcraftContent.FILLED_PHIAL.get())
                            && use(player, sources.get(type).getBlockPos(), Direction.UP).consumesAction(), "native phial extraction and insertion preserve each real batch of eight");
                }
            }
        }
    }

    private static void supplyComponents(ServerLevel level, InfusionStackRecipe recipe, List<BlockPos> positions) {
        for (int index = 0; index < recipe.components().size(); index++) ((TilePedestal) level.getBlockEntity(positions.get(index)))
                .setItem(0, recipe.components().get(index).getItems()[0].copyWithCount(1));
    }

    private static int stock(Map<Aspect, TileJar> sources) { return sources.values().stream().mapToInt(TileJar::getAmount).sum(); }
    private static void tick(TileInfusionMatrix matrix) { TileInfusionMatrix.tick(matrix.getLevel(), matrix.getBlockPos(), matrix.getBlockState(), matrix); }

    private static void put(ServerLevel level, Map<BlockPos, BlockState> states, Map<BlockPos, CompoundTag> tags, BlockPos mutable, BlockState state) {
        BlockPos pos = mutable.immutable();
        level.getChunkAt(pos); // Fixture only; gameplay scans/transfers never load chunks.
        if (!states.containsKey(pos)) {
            states.put(pos, level.getBlockState(pos));
            if (level.getBlockEntity(pos) != null) tags.put(pos, level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()));
        }
        level.setBlock(pos, state, QUIET);
    }

    private static InteractionResult use(ServerPlayer player, BlockPos pos, Direction face) {
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + 2);
        player.setYRot(180);
        player.setYHeadRot(180);
        player.yHeadRotO = 180;
        player.setXRot(20);
        return player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).relative(face, .5), face, pos, false));
    }
}
