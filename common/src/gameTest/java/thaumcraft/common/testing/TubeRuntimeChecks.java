package thaumcraft.common.testing;

import java.util.ArrayList;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.essentia.TubeGeometry;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileTube;

/** Bounded native content check; central loader wrappers own execution. No fake transports/recipes. */
public final class TubeRuntimeChecks {
    private TubeRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos base = test.absolutePos(new BlockPos(3, 2, 3));
        BlockPos firstPos = base.above(), secondPos = base.above(2), tubePos = firstPos.east(), upperPos = secondPos.east(), jarPos = base.east();
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<BlockPos, CompoundTag> tags = new LinkedHashMap<>();
        for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(3, 4, 3))) {
            BlockPos pos = mutable.immutable();
            states.put(pos, level.getBlockState(pos));
            if (level.getBlockEntity(pos) != null) tags.put(pos, level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()));
        }
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var crafting = player.inventoryMenu.getCraftSlots();
        var oldCrafting = new ArrayList<ItemStack>();
        for (int slot = 0; slot < crafting.getContainerSize(); slot++) oldCrafting.add(crafting.getItem(slot).copy());
        ItemStack cursor = player.inventoryMenu.getCarried().copy();
        Vec3 position = player.position();
        float yaw = player.getYRot(), pitch = player.getXRot();
        float headYaw = player.yHeadRot, previousHeadYaw = player.yHeadRotO;
        GameType gameType = player.gameMode.getGameModeForPlayer();
        int selected = player.getInventory().selected;
        boolean sneaking = player.isShiftKeyDown();
        AABB box = new AABB(base).inflate(6);
        Set<UUID> oldDrops = level.getEntitiesOfClass(ItemEntity.class, box).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        try {
            player.closeContainer();
            crafting.clearContent();
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.setPos(Vec3.atCenterOf(base).add(1, 1, 2));
            for (BlockPos pos : states.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            recipes(test, level);

            level.setBlock(base, ThaumcraftDistillation.ALCHEMY_FURNACE.get().defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(firstPos, ThaumcraftAlembics.ALEMBIC.get().defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(secondPos, ThaumcraftAlembics.ALEMBIC.get().defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(tubePos, ThaumcraftTubes.ESSENTIA_TUBE.get().defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(upperPos, ThaumcraftTubes.ESSENTIA_TUBE.get().defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(jarPos, ThaumcraftContent.JAR.get().defaultBlockState(), Block.UPDATE_ALL);
            var furnace = (TileAlchemyFurnace) level.getBlockEntity(base);
            var first = (TileAlembic) level.getBlockEntity(firstPos);
            var second = (TileAlembic) level.getBlockEntity(secondPos);
            var tube = (TileTube) level.getBlockEntity(tubePos);
            var upper = (TileTube) level.getBlockEntity(upperPos);
            var jar = (TileJar) level.getBlockEntity(jarPos);
            test.assertTrue(tube != null && upper != null && tube.getFacing() == Direction.NORTH && tube.getEssentiaCapacity() == 1,
                    "real registered basic tubes have the canonical default facing and single-unit buffer");
            for (Direction face : Direction.values()) test.assertTrue(tube.isConnectable(face) && tube.canInputFrom(face) && tube.canOutputTo(face), "basic tube opens both directions: " + face);
            test.assertTrue(!tube.isConnectable(null) && !tube.canInputFrom(null) && !tube.canOutputTo(null)
                    && tube.getEssentiaType(null) == null && tube.getEssentiaAmount(null) == 0 && !tube.renderExtendedTube(), "UNKNOWN is native null; unsided getters still work");
            boolean rawRefused = false;
            try { tube.setEssentiaStorageRaw(Aspect.FIRE, 1); }
            catch (IllegalStateException expected) { rawRefused = true; }
            test.assertTrue(rawRefused && tube.getEssentiaAmount(null) == 0, "raw storage hook is not a public fill/duplication path");

            // The only supplied stock is physical coal/fuel. Labels choose types
            // but create no essentia. This is not a survival/worldgen verdict.
            first.setFilter(Aspect.FIRE);
            second.setFilter(Aspect.ENERGY);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound(Aspect.FIRE.getTag()));
            test.assertTrue(use(player, jarPos, Direction.UP).consumesAction() && jar.getFilter() == Aspect.FIRE && jar.getAmount() == 0, "native paid label configures the actual destination jar");
            furnace.setItem(0, new ItemStack(Items.COAL, 2));
            furnace.setItem(1, new ItemStack(Items.COAL));
            for (int tick = 1; tick <= 300; tick++) {
                TileAlchemyFurnace.tick(level, base, furnace.getBlockState(), furnace);
                int beforeSources = first.getAmount() + second.getAmount();
                TileTube.tick(level, upperPos, upper.getBlockState(), upper);
                TileTube.tick(level, tubePos, tube.getBlockState(), tube);
                int taken = beforeSources - first.getAmount() - second.getAmount();
                test.assertTrue(taken >= 0 && taken <= 1, "one matching source unit at most per tube pull phase");
                if (taken > 0) test.assertTrue(tube.getTickCount() % 5 == 0, "source debit occurs on the tube's local phase, not global game time");
                int beforeJar = jar.getAmount();
                TileJar.tick(level, jarPos, jar.getBlockState(), jar);
                test.assertTrue(jar.getAmount() - beforeJar >= 0 && jar.getAmount() - beforeJar <= 1
                        && (jar.getAmount() == beforeJar || tick % 5 == 0), "jar independently pulls one unit every five ticks");
                test.assertTrue((tube.getEssentiaAmount(null) == 0 || tube.getEssentiaType(null) == Aspect.FIRE)
                        && (upper.getEssentiaAmount(null) == 0 || upper.getEssentiaType(null) == Aspect.FIRE), "label suction never drains the other typed alembic");
                int total = furnace.getVis() + first.getAmount() + second.getAmount() + tube.getEssentiaAmount(null) + upper.getEssentiaAmount(null) + jar.getAmount();
                test.assertTrue(total == (2 - furnace.getItem(0).getCount()) * 4, "every real distilled unit remains in furnace/alembics/tubes/jar");
            }
            test.assertTrue(furnace.getVis() == 0 && first.getAmount() == 0 && second.getAmount() == 4 && second.getAspect() == Aspect.ENERGY
                    && jar.getAmount() == 4 && jar.getAspect() == Aspect.FIRE, "actual coal distillation delivers all four ignis and leaves all four potentia");

            ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, wand.copy());
            aim(player, Vec3.atLowerCornerOf(tubePos).add(.5, .85, .5));
            var armTrace = TubeGeometry.retrace(player, tube);
            var worldTrace = player.pick(player.blockInteractionRange(), 1, false);
            test.assertTrue(armTrace != null && armTrace.index() == 1, "server ray resolves the indexed upper arm, not the supplied hit face; eye="
                    + player.getEyePosition(1) + "; view=" + player.getViewVector(1) + "; target=" + tubePos
                    + "; worldHit=" + worldTrace.getType() + "/" + (worldTrace instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : "none")
                    + "; localHit=" + TubeGeometry.trace(level, tubePos, player.getEyePosition(1), player.getEyePosition(1).add(player.getViewVector(1).scale(player.blockInteractionRange()))));
            test.assertTrue(use(player, tubePos, Direction.SOUTH).consumesAction() && !tube.isConnectable(Direction.UP) && !upper.isConnectable(Direction.DOWN)
                    && ItemStack.matches(wand, player.getMainHandItem()), "native wand closes matching opposing sides without vis/item cost");
            test.assertTrue(upper.addEssentia(Aspect.FIRE, 50, Direction.UP) == 1 && EssentiaTransfers.pullOne(tube, Direction.UP, Aspect.FIRE) == 0,
                    "fixture stock is bounded to one; a closed connection refuses an otherwise available unit");
            test.assertTrue(TubeGeometry.hasTransportNeighbor(level, tubePos, Direction.UP) && TubeGeometry.retrace(player, tube).index() == 1,
                    "closed opposite sides remain indexed so the wand can reopen them");
            player.setShiftKeyDown(true);
            test.assertTrue(use(player, tubePos, Direction.DOWN).consumesAction() && tube.isConnectable(Direction.UP) && upper.isConnectable(Direction.DOWN),
                    "actual sneaking wand item hook reopens the same indexed arm; no client subHit is trusted");
            player.setShiftKeyDown(false);
            test.assertTrue(EssentiaTransfers.pullOne(tube, Direction.UP, Aspect.FIRE) == 1 && upper.getEssentiaAmount(null) == 0 && tube.getEssentiaAmount(null) == 1
                    && !EssentiaTransfers.isLocked(tube) && !EssentiaTransfers.isLocked(upper), "native pair transfer conserves the unit and releases both locks");
            test.assertTrue(jar.addToContainer(Aspect.FIRE, 60) == 0 && jar.getAmount() == 64 && jar.getSuctionAmount(null) == 0
                    && jar.getMinimumSuction() == 64 && EssentiaTransfers.pullOne(jar, Direction.UP, Aspect.FIRE) == 0 && tube.getEssentiaAmount(null) == 1,
                    "full64 retains its labelled minimum but cannot consume the tube buffer");
            jar.takeFromContainer(Aspect.FIRE, 1);
            for (int tick = 0; tick < 5; tick++) TileJar.tick(level, jarPos, jar.getBlockState(), jar);
            test.assertTrue(jar.getAmount() == 64 && tube.getEssentiaAmount(null) == 0, "one newly free jar cell accepts exactly the available native unit");

            tube.setFacing(Direction.NORTH);
            aim(player, Vec3.atCenterOf(tubePos));
            test.assertTrue(TubeGeometry.retrace(player, tube).index() == 6 && use(player, tubePos, Direction.SOUTH).consumesAction() && tube.getFacing() == Direction.EAST,
                    "core click cycles facing toward the next open opposite transport, not one-way flow");
            player.setPos(player.position().add(0, 0, 20));
            Direction facing = tube.getFacing();
            use(player, tubePos, Direction.SOUTH);
            test.assertTrue(tube.getFacing() == facing && ItemStack.matches(wand, player.getMainHandItem()), "out-of-reach native wand packet cannot alter the tube");

            // A real adjacent tube supplies competing suction, not a fake addon.
            jar.takeFromContainer(Aspect.FIRE, 1);
            upper.setSuction(null, 0);
            // Advance only the real local counter, with no source stock, until
            // the next tick is BOTH a suction and pull phase. Supply one explicit
            // fixture unit through the real alembic API, not a raw storage hook.
            for (int tick = 0; tick < 10 && (tube.getTickCount() + 1) % 10 != 0; tick++) TileTube.tick(level, tubePos, tube.getBlockState(), tube);
            test.assertTrue((tube.getTickCount() + 1) % 10 == 0, "bounded fixture reaches the next local suction/pull phase");
            first.addToContainer(Aspect.FIRE, 1);
            upper.setSuction(Aspect.ENERGY, 63);
            TileTube.tick(level, tubePos, tube.getBlockState(), tube);
            test.assertTrue(tube.getVenting() == 40 && tube.getSuctionAmount(null) == 63 && tube.getSuctionType(null) == Aspect.FIRE
                    && first.getAmount() == 0 && tube.getEssentiaAmount(null) == 1 && tube.getEssentiaType(null) == Aspect.FIRE,
                    "typed conflict starts40, and its current tick can still pull one without deleting stock/suction or making flux");
            int phase = tube.getTickCount();
            TileTube.tick(level, tubePos, tube.getBlockState(), tube);
            test.assertTrue(tube.getVenting() == 39 && tube.getTickCount() == phase && tube.takeEssentia(Aspect.FIRE, 100, Direction.NORTH) == 1
                    && tube.addEssentia(Aspect.FIRE, 100, Direction.SOUTH) == 1, "vent pauses the local phase but ordinary external add/take still each move one");
            tube.setSideOpen(Direction.UP, false);
            tube.setSuction(Aspect.FIRE, 17);
            var saved = tube.saveWithFullMetadata(level.registryAccess());
            var update = tube.getUpdateTag(level.registryAccess());
            test.assertTrue(saved.getInt("amount") == 1 && saved.getInt("side") == Direction.EAST.get3DDataValue() && saved.getByteArray("open").length == 6
                    && saved.getInt("samount") == 17 && saved.getString("stype").equals(Aspect.FIRE.getTag())
                    && !update.contains("samount") && !update.contains("stype") && !saved.contains("count") && !saved.contains("venting"), "disk has original fields; update omits suction; phase/vent remain transient");
            var stale = tube;
            level.removeBlockEntity(tubePos);
            tube = new TileTube(tubePos, level.getBlockState(tubePos));
            tube.loadWithComponents(saved, level.registryAccess());
            level.setBlockEntity(tube);
            test.assertTrue(tube.getVenting() == 0 && tube.getTickCount() == 0 && tube.getFacing() == Direction.EAST && !tube.isConnectable(Direction.UP)
                    && tube.getEssentiaType(null) == Aspect.FIRE && tube.getEssentiaAmount(null) == 1 && tube.getSuctionAmount(null) == 17 && tube.getUpdatePacket() != null
                    && stale.takeEssentia(Aspect.FIRE, 1, Direction.NORTH) == 0 && EssentiaTransfers.pullOne(stale, Direction.UP, Aspect.FIRE) == 0,
                    "native replacement round-trip keeps storage/open/facing/suction but not phase/vent; stale BE cannot transfer");
            test.assertTrue(TubeGeometry.collisionShape(level, tubePos).toAabbs().size() == 1
                    && tube.getBlockState().getShape(level, tubePos, CollisionContext.empty()).bounds().minZ == 5.0 / 16
                    && TubeGeometry.collisionShape(level, tubePos).bounds().minZ == 6.0 / 16, "ordinary selection and single-AABB collision retain different original cross sections");
            test.assertTrue(level.destroyBlock(tubePos, true), "real native break removes a filled pipe");
            var drops = level.getEntitiesOfClass(ItemEntity.class, box).stream().filter(entity -> !oldDrops.contains(entity.getUUID()) && entity.getItem().is(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get())).toList();
            test.assertTrue(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1 && !drops.getFirst().getItem().has(DataComponents.BLOCK_ENTITY_DATA), "broken basic pipe drops only itself, not preserved contents");
            aim(player, Vec3.atCenterOf(firstPos));
            player.setItemInHand(InteractionHand.MAIN_HAND, drops.getFirst().getItem().copy());
            drops.getFirst().discard();
            test.assertTrue(use(player, firstPos, Direction.EAST).consumesAction() && level.getBlockEntity(tubePos) instanceof TileTube placed
                    && placed.getFacing() == Direction.EAST && placed.getEssentiaAmount(null) == 0, "native tube item places with clicked FACE, not opposite or old buffered contents");
        } finally {
            if (level.getBlockEntity(base) instanceof TileAlchemyFurnace furnace) furnace.clearContent();
            for (BlockPos pos : new BlockPos[] {firstPos, secondPos}) if (level.getBlockEntity(pos) instanceof TileAlembic alembic) alembic.setFilter(null);
            crafting.clearContent();
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            for (var entry : states.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            for (var entry : tags.entrySet()) if (level.getBlockEntity(entry.getKey()) != null) level.getBlockEntity(entry.getKey()).loadWithComponents(entry.getValue(), level.registryAccess());
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, box)) if (!oldDrops.contains(entity.getUUID())) entity.discard();
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            for (int slot = 0; slot < oldCrafting.size(); slot++) crafting.setItem(slot, oldCrafting.get(slot));
            player.inventoryMenu.setCarried(cursor);
            player.getInventory().selected = selected;
            player.setGameMode(gameType);
            player.setShiftKeyDown(sneaking);
            player.setPos(position);
            player.setYRot(yaw);
            player.setXRot(pitch);
            player.yHeadRot = headYaw;
            player.yHeadRotO = previousHeadYaw;
        }
        test.succeed();
    }

    private static void recipes(GameTestHelper test, ServerLevel level) {
        var dropId = ResourceLocation.fromNamespaceAndPath("thaumcraft", "quicksilver_drop");
        test.assertTrue(BuiltInRegistries.ITEM.containsKey(dropId), "genuine ItemNugget5 prerequisite is registered");
        ItemStack drop = new ItemStack(BuiltInRegistries.ITEM.get(dropId));
        test.assertTrue(ObjectAspectResolver.get(drop, level).getAmount(Aspect.METAL) == 1, "quicksilver drop carries original metallum1");
        ItemStack quicksilver = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "quick_silver")));
        var split = CraftingInput.of(1, 1, List.of(quicksilver));
        var splitRecipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, split, level).orElseThrow();
        var nine = splitRecipe.value().assemble(split, level.registryAccess());
        test.assertTrue(nine.is(drop.getItem()) && nine.getCount() == 9, "one real quicksilver makes exactly nine drops");
        var combine = CraftingInput.of(3, 3, java.util.Collections.nCopies(9, drop));
        var whole = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, combine, level).orElseThrow().value().assemble(combine, level.registryAccess());
        test.assertTrue(whole.is(quicksilver.getItem()) && whole.getCount() == 1, "nine drops return one whole quicksilver");
        var matrix = new ArcaneRecipeInput(List.of(ItemStack.EMPTY, drop, ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GLASS),
                new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY, new ItemStack(Items.GOLD_NUGGET), ItemStack.EMPTY));
        var recipe = level.getRecipeManager().getRecipeFor(ThaumcraftArcane.ARCANE_RECIPE_TYPE.get(), matrix, level).orElseThrow().value();
        ItemStack output = recipe.assemble(matrix, level.registryAccess());
        test.assertTrue(recipe.research().equals("TUBES") && recipe.vis().equals(Map.of("aqua", 5, "ordo", 5))
                && output.is(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get()) && output.getCount() == 8, "actual original TUBES recipe uses drop/iron/glassBLOCK/goldnugget and aqua5/ordo5 for eight pipes");
        var wrong = new ArrayList<>(matrix.items());
        wrong.set(1, quicksilver);
        test.assertTrue(!recipe.matches(new ArcaneRecipeInput(wrong), level), "whole quicksilver is not the recipe's drop prerequisite");
    }

    private static void aim(ServerPlayer player, Vec3 target) {
        player.setPos(target.x, target.y - player.getEyeHeight(), target.z + 2);
        player.setYRot(180);
        // LivingEntity.getViewYRot reads the HEAD, not setYRot's body yaw.
        // This synchronous fixture has no intervening native entity tick.
        player.setYHeadRot(180);
        player.yHeadRotO = 180;
        player.setXRot(0);
    }

    private static InteractionResult use(ServerPlayer player, BlockPos pos, Direction face) {
        return player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).relative(face, .5), face, pos, false));
    }
}
