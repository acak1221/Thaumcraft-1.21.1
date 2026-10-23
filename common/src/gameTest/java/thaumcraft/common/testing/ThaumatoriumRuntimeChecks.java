package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockThaumatorium;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftThaumatorium;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.container.ContainerThaumatorium;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.ThaumatoriumRules;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.essentia.ThaumatoriumOutput;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileCrucible;
import thaumcraft.common.tiles.TileEssentiaReservoir;
import thaumcraft.common.tiles.TileThaumatorium;
import thaumcraft.common.tiles.TileThaumatoriumTop;

/** Real paid wand formation, a real menu selection, native source debits and closed vanilla inventory output. */
public final class ThaumatoriumRuntimeChecks {
    private ThaumatoriumRuntimeChecks() {}
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    public static void worldScheduled(GameTestHelper test) {
        Check check = new Check(test);
        DeviceWorldRuntimeChecks.cleanup(test, check::finish);
        check.prepare();
        test.runAfterDelay(5, () -> {
            check.form();
            check.tile.setItem(0, new ItemStack(Items.IRON_NUGGET));
            check.select();
        });
        test.runAfterDelay(70, () -> {
            test.assertTrue(check.tile.isHeated() && check.tile.getItem(0).isEmpty()
                            && check.source.getAmount() == 0 && check.tile.getAspects().visSize() == 0
                            && check.outputCount() == 3,
                    "world formed thaumatorium pays nugget1+metallum2 for iron nuggets3");
        });
        test.runAfterDelay(100, () -> {
            test.assertTrue(check.outputCount() == 3, "world empty thaumatorium cannot replay paid recipe");
            check.finish(); test.succeed();
        });
    }
    public static void run(GameTestHelper test) {
        Check check = new Check(test);
        try { check.prepare(); } catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        test.runAfterDelay(5, () -> {
            try {
                check.form(); check.recipesAndPayment(); check.persistenceAndTop(); check.brainboxes(); check.physicalOutput();
                check.live();
            } catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        });
    }
    private static final class Check {
        final GameTestHelper test;
        final ServerLevel level;
        final ServerPlayer player;
        final BlockPos base;
        final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final java.util.Set<UUID> existing = new java.util.HashSet<>();
        TileThaumatorium tile;
        TileThaumatoriumTop top;
        TileEssentiaReservoir source;
        ChestBlockEntity chest;
        boolean finished;
        Check(GameTestHelper test) { this.test = test; level = test.getLevel(); player = test.makeMockServerPlayerInLevel(); base = test.absolutePos(new BlockPos(3, 100, 3)); }
        ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
        void set(BlockPos pos, BlockState state) { original.putIfAbsent(pos, level.getBlockState(pos)); level.setBlock(pos, state, QUIET); }
        void prepare() {
            forced.addAll(GolemChunks.hold(level, List.of(base.offset(-3, -3, -3), base.offset(3, 3, 3))));
            level.getAllEntities().forEach(entity -> existing.add(entity.getUUID()));
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-3, -3, -3), base.offset(3, 3, 3))) set(pos.immutable(), Blocks.AIR.defaultBlockState());
            set(base.below(2), BuiltInRegistries.BLOCK.get(id("nitor")).defaultBlockState());
            set(base.below(), ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState());
            set(base, ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT.get().defaultBlockState());
            set(base.above(), ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT.get().defaultBlockState());
            player.setPos(Vec3.atCenterOf(base).add(2, 0, 0));
        }
        ItemStack wand(boolean charged) {
            var wand = new ItemStack(ThaumcraftContent.WAND.get());
            var vis = new LinkedHashMap<String, Integer>(); for (String primal : WandData.PRIMALS) vis.put(primal, charged ? 10000 : 0);
            wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", "iron", vis)); return wand;
        }
        InteractionResult click(BlockPos pos) {
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
            return player.getMainHandItem().getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        }
        void form() {
            var crucible = level.getBlockEntity(base.below());
            player.setItemInHand(InteractionHand.MAIN_HAND, wand(true));
            ItemStack before = player.getMainHandItem().copy();
            test.assertTrue(click(base) == InteractionResult.FAIL && level.getBlockState(base).is(ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT.get())
                            && ItemStack.matches(before, player.getMainHandItem()), "unknown THAUMATORIUM spends no vis and converts no blocks");
            KnowledgeAccess.get(player).addResearch("THAUMATORIUM");
            player.setItemInHand(InteractionHand.MAIN_HAND, wand(false));
            test.assertTrue(click(base) == InteractionResult.FAIL, "unfunded formation is refused");
            player.setItemInHand(InteractionHand.MAIN_HAND, wand(true));
            Map<String, Integer> price = ArcaneVis.price(ThaumatoriumRules.FORM_VIS, player.getMainHandItem(), player);
            test.assertTrue(click(base.above()).consumesAction(), "an actual wand click on the upper construct forms the original three-block stack");
            tile = (TileThaumatorium) level.getBlockEntity(base); top = (TileThaumatoriumTop) level.getBlockEntity(base.above());
            test.assertTrue(tile != null && top != null && tile.structureValid() && tile.facing() == Direction.NORTH
                            && level.getBlockEntity(base.below()) == crucible, "both constructs are replaced; the actual crucible stays unchanged");
            for (String primal : WandData.PRIMALS) test.assertTrue(ItemWandCasting.data(player.getMainHandItem()).amount(primal) == 10000 - price.getOrDefault(primal, 0), "exact formation payment for " + primal);
            test.assertTrue(click(base) != InteractionResult.CONSUME, "the formed stack cannot be charged for formation a second time");
            set(base.north(), Blocks.CHEST.defaultBlockState()); chest = (ChestBlockEntity) level.getBlockEntity(base.north());
            set(base.west(), ThaumcraftTubes.ESSENTIA_RESERVOIR.get().defaultBlockState()); source = (TileEssentiaReservoir) level.getBlockEntity(base.west());
            test.assertTrue(source.setFacing(Direction.EAST) && source.addToContainer(Aspect.METAL, 2) == 0, "a real source holds exactly two metal units");
        }
        void ticks(int count) { for (int tick = 0; tick < count; tick++) TileThaumatorium.tick(level, base, level.getBlockState(base), tile); }
        int outputCount() { int count = 0; for (int slot = 0; slot < chest.getContainerSize(); slot++) if (chest.getItem(slot).is(Items.IRON_NUGGET)) count += chest.getItem(slot).getCount(); return count; }
        void select() {
            var menu = new ContainerThaumatorium(22, player.getInventory(), tile); player.containerMenu = menu;
            int index = -1; for (int i = 0; i < menu.recipes().size(); i++) if (menu.recipes().get(i).id().equals(id("crucible/trans_iron"))) index = i;
            test.assertTrue(index >= 0 && !menu.clickMenuButton(player, index), "an unknown recipe cannot be selected using a menu packet");
            KnowledgeAccess.get(player).addResearch("TRANSIRON");
            test.assertTrue(menu.clickMenuButton(player, index) && tile.selectedRecipe().equals(id("crucible/trans_iron")), "a known matching recipe is selected by the real menu");
            test.assertTrue(!menu.clickMenuButton(player, -1) && !menu.clickMenuButton(player, menu.recipes().size()), "invalid buttons grant no recipes");
            player.containerMenu = player.inventoryMenu;
            test.assertTrue(!menu.clickMenuButton(player, index), "a stale menu cannot clear or select a formula");
        }
        void recipesAndPayment() {
            tile.setItem(0, new ItemStack(Items.IRON_NUGGET, 2)); select();
            for (int slot = 0; slot < chest.getContainerSize(); slot++) chest.setItem(slot, new ItemStack(Items.STONE, 64));
            ticks(40);
            test.assertTrue(tile.isHeated() && source.containerContains(Aspect.METAL) == 2 && tile.containerContains(Aspect.METAL) == 0 && tile.getItem(0).getCount() == 2,
                    "a full output inventory pauses BEFORE essentia or catalyst is taken");
            chest.setItem(0, new ItemStack(Items.IRON_NUGGET, 63)); ticks(10);
            test.assertTrue(source.containerContains(Aspect.METAL) == 2 && tile.getItem(0).getCount() == 2, "one free place does not fit the three-nugget output");
            chest.setItem(1, ItemStack.EMPTY); ticks(15);
            test.assertTrue(source.containerContains(Aspect.METAL) == 0 && tile.getAspects().visSize() == 0 && tile.getItem(0).getCount() == 1
                            && chest.getItem(0).getCount() == 64 && chest.getItem(1).is(Items.IRON_NUGGET) && chest.getItem(1).getCount() == 2,
                    "two real source units and exactly one catalyst pay the full output, split between merge and empty slots");
            int produced = outputCount(); ticks(20);
            test.assertTrue(outputCount() == produced && tile.getItem(0).getCount() == 1, "no second output is created without another two paid essentia");
            test.assertTrue(source.addToContainer(Aspect.METAL, 2) == 0, "second declared source input");
            set(base.below().east(), Blocks.REDSTONE_BLOCK.defaultBlockState()); ticks(40);
            test.assertTrue(tile.getItem(0).getCount() == 1 && source.containerContains(Aspect.METAL) == 2, "power at the crucible level pauses the whole structure");
            set(base.below().east(), Blocks.AIR.defaultBlockState()); ticks(15);
            test.assertTrue(tile.getItem(0).isEmpty() && source.containerContains(Aspect.METAL) == 0 && outputCount() == produced + 3, "unpowering resumes one exact paid craft");
        }
        void persistenceAndTop() {
            top.setItem(0, new ItemStack(Items.IRON_NUGGET, 5));
            test.assertTrue(tile.getItem(0).getCount() == 5 && top.removeItem(0, 2).getCount() == 2 && tile.getItem(0).getCount() == 3, "upper automation accesses the SAME actual catalyst inventory");
            test.assertTrue(tile.addToContainer(Aspect.METAL, 1) == 0, "declared partial-progress input");
            var saved = tile.saveWithoutMetadata(level.registryAccess());
            var restored = new TileThaumatorium(base, level.getBlockState(base)); restored.loadWithComponents(saved, level.registryAccess());
            test.assertTrue(restored.getItem(0).getCount() == 3 && restored.containerContains(Aspect.METAL) == 1 && restored.selectedRecipe().equals(tile.selectedRecipe()), "input, partial payment and selected recipe survive real NBT");
            test.assertTrue(tile.takeFromContainer(Aspect.METAL, 1), "remove the declared partial payment for the physical-output test");
            var plan = ThaumatoriumOutput.prepare(level, base.north(), Direction.SOUTH, new ItemStack(Items.IRON_NUGGET, 3));
            test.assertTrue(plan != null && !plan.commit(tile), "an external native-output plan has no unpaid craft authority");
            tile.setItem(0, ItemStack.EMPTY);
        }
        void physicalOutput() {
            chest.clearContent(); // Retire the explicit chest fixture, not its already asserted output back into the live test world.
            set(base.north(), Blocks.AIR.defaultBlockState());
            tile.setItem(0, new ItemStack(Items.IRON_NUGGET)); source.addToContainer(Aspect.METAL, 2); ticks(15);
            test.assertTrue(tile.getItem(0).isEmpty() && source.getAmount() == 0 && tile.getAspects().visSize() == 0, "physical output is paid exactly once");
            DeferredItemDrops.tick(level);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(3), item -> !existing.contains(item.getUUID()) && item.getItem().is(Items.IRON_NUGGET));
            test.assertTrue(drops.stream().mapToInt(item -> item.getItem().getCount()).sum() == 3, "the durable output queue publishes the three paid nuggets");
            ItemEntity actual = drops.getFirst();
            test.assertTrue(Math.abs(actual.getX() - (base.getX() + .5)) < 1e-6 && Math.abs(actual.getZ() - (base.getZ() + .5 - .66)) < 1e-6
                            && Math.abs(actual.getDeltaMovement().z - (double) (-.075F)) < 1e-6 && Math.abs(actual.getDeltaMovement().y - (double) .025F) < 1e-6,
                    "the physical output keeps the original front position and directed velocity");
            drops.forEach(ItemEntity::discard);
        }
        void brainboxes() {
            var recipe = (thaumcraft.common.crafting.ArcaneShapedRecipe) level.getRecipeManager().byKey(id("arcane/mnemonic_matrix")).orElseThrow().value();
            var amber = new ItemStack(ThaumcraftContent.MATERIALS.get("amber_gem").get()); var brain = new ItemStack(ThaumcraftContent.MATERIALS.get("zombie_brain").get()); var iron = new ItemStack(Items.IRON_INGOT);
            var input = new thaumcraft.common.crafting.ArcaneRecipeInput(List.of(iron, amber, iron, amber, brain, amber, iron, amber, iron));
            test.assertTrue(recipe.matches(input, level) && recipe.research().equals("THAUMATORIUM") && recipe.vis().equals(Map.of("ignis", 5, "aqua", 5, "ordo", 5))
                            && recipe.getResultItem(level.registryAccess()).is(ThaumcraftThaumatorium.MNEMONIC_MATRIX_ITEM.get()), "the real Mnemonic Matrix recipe has four amber, four iron and a brain, with exact costs");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftThaumatorium.MNEMONIC_MATRIX_ITEM.get()));
            var hit = new BlockHitResult(Vec3.atCenterOf(base).add(.5, 0, 0), Direction.EAST, base, false);
            test.assertTrue(player.getMainHandItem().getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction()
                            && tile.recipeCapacity() == 3 && level.getBlockState(base.east()).getValue(thaumcraft.common.blocks.BlockMnemonicMatrix.FACING) == Direction.WEST,
                    "real block-item placement attaches a matrix toward the machine and immediately adds two formula slots");
            set(base.above().east(), ThaumcraftThaumatorium.MNEMONIC_MATRIX.get().defaultBlockState().setValue(thaumcraft.common.blocks.BlockMnemonicMatrix.FACING, Direction.WEST));
            test.assertTrue(tile.recipeCapacity() == 5, "a matrix on the upper half adds another two slots through its real placement hook");
            set(base.above().west(), ThaumcraftThaumatorium.MNEMONIC_MATRIX.get().defaultBlockState().setValue(thaumcraft.common.blocks.BlockMnemonicMatrix.FACING, Direction.WEST));
            test.assertTrue(tile.recipeCapacity() == 5, "a matrix facing away contributes no recipe memory");
            KnowledgeAccess.get(player).addResearch("TRANSGOLD"); KnowledgeAccess.get(player).addResearch("NITOR");
            tile.setItem(0, new ItemStack(Items.GOLD_NUGGET));
            test.assertTrue(tile.toggleRecipe(player, id("crucible/trans_gold")), "the first additional formula fits the matrix capacity");
            tile.setItem(0, new ItemStack(Items.GLOWSTONE_DUST));
            test.assertTrue(tile.toggleRecipe(player, id("crucible/nitor")) && tile.recipeCount() == 3, "a third formula is remembered alongside the first two");
            ticks(5); test.assertTrue(tile.addToContainer(Aspect.FIRE, 1) == 0, "one actual paid unit is retained while formulas are reconfigured");
            var loaded = new TileThaumatorium(base, level.getBlockState(base)); loaded.loadWithComponents(tile.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
            test.assertTrue(loaded.recipeCount() == 3 && loaded.recipeCapacity() == 5 && loaded.isRecipeSelected(id("crucible/nitor")) && loaded.containerContains(Aspect.FIRE) == 1, "multiple formulas, capacity and partial essence survive NBT");
            set(base.above().east(), Blocks.AIR.defaultBlockState()); set(base.east(), Blocks.AIR.defaultBlockState()); set(base.above().west(), Blocks.AIR.defaultBlockState());
            test.assertTrue(tile.recipeCapacity() == 1 && tile.recipeCount() == 1 && tile.isRecipeSelected(id("crucible/trans_iron"))
                            && !tile.isRecipeSelected(id("crucible/trans_gold")) && !tile.isRecipeSelected(id("crucible/nitor")) && tile.containerContains(Aspect.FIRE) == 1,
                    "removing the matrices truncates formulas from the end, while preserving already paid essence");
        }
        void live() {
            // Leave a final craft to ordinary server ticks: this proves the registered block ticker, not only direct method calls.
            set(base.north(), Blocks.CHEST.defaultBlockState()); chest = (ChestBlockEntity) level.getBlockEntity(base.north());
            tile.setItem(0, new ItemStack(Items.IRON_NUGGET)); source.addToContainer(Aspect.METAL, 2);
            test.runAfterDelay(25, () -> {
                try {
                    test.assertTrue(tile.getItem(0).isEmpty() && source.getAmount() == 0 && outputCount() == 3, "ordinary server ticks run the paid automatic recipe through the registered real block");
                    test.succeed();
                } finally { finish(); }
            });
        }
        void finish() {
            if (finished) return; finished = true;
            if (chest != null) chest.clearContent();
            if (tile != null && tile.current()) { tile.clearContent(); if (source != null) for (Aspect aspect : source.getAspects().getAspects()) source.takeFromContainer(aspect, source.containerContains(aspect)); }
            player.containerMenu = player.inventoryMenu;
            original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
        }
    }
}
