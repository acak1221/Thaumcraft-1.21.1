package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.common.blocks.BlockItemGrate;
import thaumcraft.common.config.ThaumcraftCosmeticWood;
import thaumcraft.common.config.ThaumcraftGrate;
import thaumcraft.common.tiles.TileGrate;

/**
 * The Item Grate (original BlockMetalDevice metadata 5/6 + TileGrate) and the Greatwood / Silverwood stairs and slabs on the real
 * server: the registered numbers and tags, the real crafting recipes, a bare-hand toggle through the real player interaction, redstone
 * closing and opening, the collision the engine really asks for with a mob and with a dropped item, a hopper and an explicit insertion
 * feeding the grate from above and from the side, real items falling through an open grate and standing on a closed one, and the stair
 * and slab placement, drops and breaking. NOT the models or the sounds (client, unseen).
 */
public final class GrateRuntimeChecks {
    private GrateRuntimeChecks() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    private static BlockItemGrate grate() { return ThaumcraftGrate.ITEM_GRATE.get(); }

    private static InteractionResult use(ServerPlayer player, ItemStack hand, BlockPos pos, Direction face) {
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, hand);
        return player.gameMode.useItemOn(player, player.serverLevel(), hand, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).relative(face, .5), face, pos, false));
    }

    private static int dropped(ServerLevel level, AABB area, Item item) {
        int total = 0;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area, found -> found.getItem().is(item))) total += entity.getItem().getCount();
        return total;
    }

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos origin = test.absolutePos(new BlockPos(3, 100, 3));
        Set<ChunkPos> forced = new LinkedHashSet<>();
        List<Entity> spawned = new ArrayList<>();
        try {
            for (int x = (origin.getX() - 3) >> 4; x <= ((origin.getX() + 40) >> 4); x++) for (int z = (origin.getZ() - 8) >> 4; z <= ((origin.getZ() + 8) >> 4); z++) {
                level.getChunk(x, z);
                ChunkPos chunk = new ChunkPos(x, z);
                if (!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(x, z, true); forced.add(chunk); }
            }
            definitions(test, level, origin);
            recipes(test, level);
            interaction(test, level, player, origin, spawned);
            stairsAndSlabs(test, level, player, origin);
            falling(test, level, origin, forced, spawned);
        } catch (RuntimeException | Error failure) {
            cleanup(level, forced, spawned);
            throw failure;
        }
    }

    private static void cleanup(ServerLevel level, Set<ChunkPos> forced, List<Entity> spawned) {
        spawned.forEach(Entity::discard);
        for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
        forced.clear();
    }

    // ---- registered numbers and tags -------------------------------------------------------------------------------------

    private static void definitions(GameTestHelper test, ServerLevel level, BlockPos origin) {
        BlockItemGrate block = grate();
        BlockState open = block.defaultBlockState();
        test.assertTrue(BuiltInRegistries.BLOCK.getKey(block).equals(id("item_grate")) && !open.getValue(BlockItemGrate.CLOSED), "the grate is registered and starts open");
        test.assertTrue(open.getDestroySpeed(level, origin) == 3.0F && Math.abs(block.getExplosionResistance() - 10.2F) < 1e-4F && open.getSoundType() == SoundType.METAL
                        && open.requiresCorrectToolForDrops() && open.is(BlockTags.MINEABLE_WITH_PICKAXE) && !open.canOcclude(),
                "hardness 3, resistance 17 stored (3 x 17 / 5 = 10.2), metal, a pickaxe breaks it, not a full cube: " + open.getDestroySpeed(level, origin) + " " + block.getExplosionResistance());
        test.assertTrue(BuiltInRegistries.ITEM.get(id("item_grate")) instanceof BlockItem item && item.getBlock() == block, "the item places the block");
        test.assertTrue(ThaumcraftGrate.GRATE_BLOCK_ENTITY.get().isValid(open) && ThaumcraftGrate.GRATE_BLOCK_ENTITY.get().isValid(open.setValue(BlockItemGrate.CLOSED, true))
                && BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id("item_grate")) == ThaumcraftGrate.GRATE_BLOCK_ENTITY.get(), "both states carry the tile, which is registered");
        for (var wood : List.of("greatwood", "silverwood")) {
            Block stairs = BuiltInRegistries.BLOCK.get(id(wood + "_stairs")), slab = BuiltInRegistries.BLOCK.get(id(wood + "_slab")), planks = BuiltInRegistries.BLOCK.get(id(wood + "_planks"));
            test.assertTrue(stairs instanceof StairBlock && slab instanceof SlabBlock, wood + " stairs and slab are native stair and slab blocks");
            BlockState stairState = stairs.defaultBlockState(), slabState = slab.defaultBlockState();
            test.assertTrue(stairState.getDestroySpeed(level, origin) == 2.5F && Math.abs(stairs.getExplosionResistance() - 6.0F) < 1e-4F && stairState.getSoundType() == SoundType.WOOD
                            && stairState.is(BlockTags.MINEABLE_WITH_AXE) && stairState.is(BlockTags.STAIRS) && stairState.is(BlockTags.WOODEN_STAIRS) && !stairState.requiresCorrectToolForDrops(),
                    wood + " stairs take the planks' strength 2.5 / 6, wood, axe, in the stair tags");
            test.assertTrue(slabState.getDestroySpeed(level, origin) == 2.0F && Math.abs(slab.getExplosionResistance() - 3.0F) < 1e-4F && slabState.getSoundType() == SoundType.WOOD
                            && slabState.is(BlockTags.MINEABLE_WITH_AXE) && slabState.is(BlockTags.SLABS) && slabState.is(BlockTags.WOODEN_SLABS) && !slabState.requiresCorrectToolForDrops(),
                    wood + " slab: hardness 2, resistance 5 stored (3.0), wood, axe, in the slab tags");
            test.assertTrue(planks != Blocks.AIR && BuiltInRegistries.ITEM.get(id(wood + "_stairs")) instanceof BlockItem && BuiltInRegistries.ITEM.get(id(wood + "_slab")) instanceof BlockItem, wood + " items exist");
            test.assertTrue(stairState.getLightBlock(level, origin) == 0 && !stairState.setValue(StairBlock.WATERLOGGED, true).propagatesSkylightDown(level, origin),
                    wood + " stairs keep the original opacity-0 quirk when dry, and block skylight when waterlogged");
        }
    }

    // ---- recipes -------------------------------------------------------------------------------------------------------------

    private static ItemStack craft(ServerLevel level, int width, int height, ItemStack... items) {
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(width, height, List.of(items)), level)
                .map(holder -> holder.value().assemble(CraftingInput.of(width, height, List.of(items)), level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private static void recipes(GameTestHelper test, ServerLevel level) {
        ItemStack bars = new ItemStack(Items.IRON_BARS), oak = new ItemStack(Items.OAK_TRAPDOOR), spruce = new ItemStack(Items.SPRUCE_TRAPDOOR);
        ItemStack made = craft(level, 1, 2, bars, oak);
        test.assertTrue(made.is(grate().asItem()) && made.getCount() == 1, "iron bars over an oak trapdoor make one grate: " + made);
        test.assertTrue(craft(level, 1, 2, oak, bars).isEmpty(), "the other way round does not");
        test.assertTrue(craft(level, 1, 2, bars, spruce).isEmpty(), "the original only knew the oak trapdoor");
        test.assertTrue(craft(level, 2, 1, bars, oak).isEmpty(), "side by side does not");
        for (var wood : List.of("greatwood", "silverwood")) {
            ItemStack plank = new ItemStack(BuiltInRegistries.ITEM.get(id(wood + "_planks"))), other = new ItemStack(BuiltInRegistries.ITEM.get(id(wood.equals("greatwood") ? "silverwood_planks" : "greatwood_planks")));
            ItemStack e = ItemStack.EMPTY;
            ItemStack stairs = craft(level, 3, 3, plank, e, e, plank.copy(), plank.copy(), e, plank.copy(), plank.copy(), plank.copy());
            test.assertTrue(stairs.is(BuiltInRegistries.ITEM.get(id(wood + "_stairs"))) && stairs.getCount() == 4, wood + " planks in the staircase pattern make four stairs: " + stairs);
            ItemStack mirrored = craft(level, 3, 3, e, e, plank, e, plank.copy(), plank.copy(), plank.copy(), plank.copy(), plank.copy());
            test.assertTrue(mirrored.getCount() == 4, "and mirrored too, as in 1.7: " + mirrored);
            ItemStack slabs = craft(level, 3, 1, plank, plank.copy(), plank.copy());
            test.assertTrue(slabs.is(BuiltInRegistries.ITEM.get(id(wood + "_slab"))) && slabs.getCount() == 6, wood + " planks in a row make six slabs: " + slabs);
            test.assertTrue(craft(level, 3, 1, plank, other, plank.copy()).isEmpty(), "mixed planks make nothing");
            test.assertTrue(craft(level, 3, 1, new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS)).is(Items.OAK_SLAB), "the vanilla oak slab recipe is untouched");
        }
    }

    // ---- hand, redstone, collision, hoppers ------------------------------------------------------------------------------------

    private static void interaction(GameTestHelper test, ServerLevel level, ServerPlayer player, BlockPos origin, List<Entity> spawned) {
        BlockPos pos = origin.offset(0, 0, 0);
        level.setBlock(pos, grate().defaultBlockState(), Block.UPDATE_ALL);
        TileGrate tile = (TileGrate) level.getBlockEntity(pos);
        test.assertTrue(tile != null && tile.getContainerSize() == 1 && tile.isEmpty() && tile.getItem(0).isEmpty(), "a placed grate has its one-slot tile and it is always empty");

        // A bare hand toggles it both ways and keeps the same tile.
        test.assertTrue(use(player, ItemStack.EMPTY, pos, Direction.UP).consumesAction() && level.getBlockState(pos).getValue(BlockItemGrate.CLOSED), "a bare right click closes an open grate");
        test.assertTrue(level.getBlockEntity(pos) == tile, "the tile survives the toggle");
        test.assertTrue(use(player, ItemStack.EMPTY, pos, Direction.UP).consumesAction() && !level.getBlockState(pos).getValue(BlockItemGrate.CLOSED), "and opens a closed one");

        // Redstone: a powered neighbour closes it, losing power opens it, a stone placed beside a hand-closed one changes nothing.
        BlockPos side = pos.east();
        level.setBlock(side, Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(level.getBlockState(pos).getValue(BlockItemGrate.CLOSED), "a redstone block beside the open grate closes it");
        level.setBlock(side, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(!level.getBlockState(pos).getValue(BlockItemGrate.CLOSED), "taking the redstone block away opens it again");
        use(player, ItemStack.EMPTY, pos, Direction.UP);
        level.setBlock(side, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(level.getBlockState(pos).getValue(BlockItemGrate.CLOSED), "a stone changing beside a hand-closed grate does not reopen it (BlockMetalDevice:490)");
        level.setBlock(side, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(side, Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(side, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(!level.getBlockState(pos).getValue(BlockItemGrate.CLOSED), "but a lost power source does reopen it");
        test.assertTrue(level.getBlockState(pos).getBlock().asItem() == grate().asItem() && grate().asItem() == BuiltInRegistries.ITEM.get(id("item_grate")), "open and closed are one item");

        // Collision as the engine asks for it: a mob and a dropped item over the plate, open and closed.
        Pig pig = EntityType.PIG.create(level);
        pig.setNoAi(true);
        pig.setPos(Vec3.atBottomCenterOf(pos));
        ItemEntity item = new ItemEntity(level, pos.getX() + .5, pos.getY() + 13 / 16.0D + 0.01D, pos.getZ() + .5, new ItemStack(Items.STICK));
        BlockState openState = grate().defaultBlockState(), closedState = openState.setValue(BlockItemGrate.CLOSED, true);
        level.setBlock(pos, openState, Block.UPDATE_ALL);
        test.assertTrue(!openState.getCollisionShape(level, pos, CollisionContext.of(pig)).isEmpty(), "an open grate stops a pig");
        test.assertTrue(openState.getCollisionShape(level, pos, CollisionContext.of(item)).isEmpty(), "an open grate lets a dropped item through");
        test.assertTrue(openState.getCollisionShape(level, pos, CollisionContext.empty()).isEmpty(), "asked without an entity an open grate has no box (kept quirk)");
        test.assertTrue(!closedState.getCollisionShape(level, pos, CollisionContext.of(item)).isEmpty() && !closedState.getCollisionShape(level, pos, CollisionContext.empty()).isEmpty(), "a closed grate stops items and answers without an entity");
        test.assertTrue(openState.getCollisionShape(level, pos, CollisionContext.of(pig)).bounds().minY == 13 / 16.0D, "the plate begins 13/16 up");
        test.assertTrue(!level.noCollision(pig, new AABB(pos.getX() + .3, pos.getY() + 14 / 16.0D, pos.getZ() + .3, pos.getX() + .7, pos.getY() + 15 / 16.0D, pos.getZ() + .7)), "the engine's own collision query stops the pig in the plate");
        test.assertTrue(level.noCollision(item, new AABB(pos.getX() + .3, pos.getY() + 14 / 16.0D, pos.getZ() + .3, pos.getX() + .7, pos.getY() + 15 / 16.0D, pos.getZ() + .7)), "and lets the dropped item through it");
        level.setBlock(pos, closedState, Block.UPDATE_ALL);
        test.assertTrue(!level.noCollision(item, new AABB(pos.getX() + .3, pos.getY() + 14 / 16.0D, pos.getZ() + .3, pos.getX() + .7, pos.getY() + 15 / 16.0D, pos.getZ() + .7)), "a closed grate stops the same item");
        level.setBlock(pos, openState, Block.UPDATE_ALL);
        tile = (TileGrate) level.getBlockEntity(pos);

        // Feeding it: only an open grate, only from above, and the stack reappears below.
        test.assertTrue(java.util.Arrays.equals(tile.getSlotsForFace(Direction.UP), new int[] {0}), "an open grate offers its slot from above");
        for (Direction face : Direction.values()) if (face != Direction.UP) test.assertTrue(tile.getSlotsForFace(face).length == 0, "and nothing from " + face);
        test.assertTrue(HopperBlockEntity.addItem(null, tile, new ItemStack(Items.STICK, 2), Direction.NORTH).getCount() == 2, "a stack pushed from the side is refused untouched");
        test.assertTrue(dropped(level, new AABB(pos).inflate(2), Items.STICK) == 0, "and nothing appeared");
        ItemStack rest = HopperBlockEntity.addItem(null, tile, new ItemStack(Items.STICK, 2), Direction.UP);
        test.assertTrue(rest.isEmpty(), "a stack pushed from above is accepted whole");
        List<ItemEntity> made = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1), found -> found.getItem().is(Items.STICK));
        spawned.addAll(made);
        test.assertTrue(made.size() == 1 && made.get(0).getItem().getCount() == 2, "it came out as one dropped item of the same stack");
        ItemEntity out = made.get(0);
        test.assertTrue(Math.abs(out.getX() - (pos.getX() + .5)) < 1e-9 && Math.abs(out.getY() - (pos.getY() + .6)) < 1e-9 && Math.abs(out.getZ() - (pos.getZ() + .5)) < 1e-9,
                "centred, 0.6 above the floor of the block: " + out.position());
        test.assertTrue(out.getDeltaMovement().x == 0.0D && out.getDeltaMovement().z == 0.0D && out.getDeltaMovement().y == -0.1D, "moving straight down at 0.1: " + out.getDeltaMovement());
        test.assertTrue(tile.isEmpty() && tile.getItem(0).isEmpty(), "the tile still holds nothing");
        out.discard();
        level.setBlock(pos, closedState, Block.UPDATE_ALL);
        tile = (TileGrate) level.getBlockEntity(pos);
        test.assertTrue(tile.getSlotsForFace(Direction.UP).length == 0 && HopperBlockEntity.addItem(null, tile, new ItemStack(Items.STICK), Direction.UP).getCount() == 1, "a closed grate takes nothing");
        test.assertTrue(dropped(level, new AABB(pos).inflate(2), Items.STICK) == 0, "and drops nothing");
        test.assertTrue(!tile.canTakeItemThroughFace(0, new ItemStack(Items.STICK), Direction.DOWN), "and nothing can be pulled out of either grate");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        pig.discard();
        item.discard();
    }

    // ---- stairs and slabs in the world ---------------------------------------------------------------------------------------------

    private static void stairsAndSlabs(GameTestHelper test, ServerLevel level, ServerPlayer player, BlockPos origin) {
        BlockPos floor = origin.offset(0, 0, 4);
        level.setBlock(floor, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        for (var wood : List.of("greatwood", "silverwood")) {
            Item slabItem = BuiltInRegistries.ITEM.get(id(wood + "_slab")), stairsItem = BuiltInRegistries.ITEM.get(id(wood + "_stairs"));
            Block slabBlock = BuiltInRegistries.BLOCK.get(id(wood + "_slab"));
            BlockPos above = floor.above();
            // Placing a slab on the floor gives the bottom half; a second one on its top face makes the double block.
            test.assertTrue(use(player, new ItemStack(slabItem, 2), floor, Direction.UP).consumesAction() && level.getBlockState(above).is(slabBlock)
                    && level.getBlockState(above).getValue(SlabBlock.TYPE) == SlabType.BOTTOM, wood + " slab placed on stone is a bottom half: " + level.getBlockState(above));
            test.assertTrue(use(player, new ItemStack(slabItem, 1), above, Direction.UP).consumesAction() && level.getBlockState(above).getValue(SlabBlock.TYPE) == SlabType.DOUBLE,
                    "a second one on top joins into a double slab: " + level.getBlockState(above));
            List<ItemStack> drops = Block.getDrops(level.getBlockState(above), level, above, null);
            test.assertTrue(drops.size() == 1 && drops.get(0).is(slabItem) && drops.get(0).getCount() == 2, "a double slab drops two slabs: " + drops);
            level.setBlock(above, level.getBlockState(above).setValue(SlabBlock.TYPE, SlabType.BOTTOM), Block.UPDATE_ALL);
            drops = Block.getDrops(level.getBlockState(above), level, above, null);
            test.assertTrue(drops.size() == 1 && drops.get(0).is(slabItem) && drops.get(0).getCount() == 1, "a half drops one: " + drops);
            level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

            // Stairs: placed by a player, native facing and half; their drop is themselves; they connect into corners like any stairs.
            test.assertTrue(use(player, new ItemStack(stairsItem), floor, Direction.UP).consumesAction() && level.getBlockState(above).getBlock() instanceof StairBlock
                    && level.getBlockState(above).getValue(StairBlock.HALF) == net.minecraft.world.level.block.state.properties.Half.BOTTOM, wood + " stairs are placed by the player: " + level.getBlockState(above));
            drops = Block.getDrops(level.getBlockState(above), level, above, null);
            test.assertTrue(drops.size() == 1 && drops.get(0).is(stairsItem) && drops.get(0).getCount() == 1, "stairs drop themselves: " + drops);
            level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(floor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        // The grate drops itself closed or open; with a pickaxe only.
        BlockPos pos = origin.offset(0, 0, 8);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        for (boolean closed : new boolean[] {false, true}) {
            BlockState state = grate().defaultBlockState().setValue(BlockItemGrate.CLOSED, closed);
            level.setBlock(pos, state, Block.UPDATE_ALL);
            List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), null, new ItemStack(Items.IRON_PICKAXE));
            test.assertTrue(drops.size() == 1 && drops.get(0).is(grate().asItem()) && drops.get(0).getCount() == 1, (closed ? "closed" : "open") + " grate dropped with a pickaxe is the one item: " + drops);
            test.assertTrue(Block.getDrops(state, level, pos, null, null, ItemStack.EMPTY).size() == 1, "the loot table itself has no tool condition; the engine's drop rule does");
            test.assertTrue(!state.requiresCorrectToolForDrops() || !player.hasCorrectToolForDrops(state), "a bare hand does not harvest it");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    // ---- real items, real ticks ---------------------------------------------------------------------------------------------------------

    private static void falling(GameTestHelper test, ServerLevel level, BlockPos origin, Set<ChunkPos> forced, List<Entity> spawned) {
        BlockPos open = origin.offset(10, 0, 0), closed = origin.offset(14, 0, 0), fed = origin.offset(18, 0, 0), blocked = origin.offset(22, 0, 0);
        for (BlockPos spot : List.of(open, closed, fed, blocked)) {
            level.setBlock(spot, grate().defaultBlockState().setValue(BlockItemGrate.CLOSED, spot.equals(closed) || spot.equals(blocked)), Block.UPDATE_ALL);
            level.setBlock(spot.below(3), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(fed.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
        ((HopperBlockEntity) level.getBlockEntity(fed.above())).setItem(0, new ItemStack(Items.STICK, 3));
        level.setBlock(blocked.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
        ((HopperBlockEntity) level.getBlockEntity(blocked.above())).setItem(0, new ItemStack(Items.STICK, 3));
        ItemEntity[] items = new ItemEntity[2];
        BlockPos[] drop = {open, closed};
        // The chunks were forced a moment ago: their entity sections start ticking a tick or more later,
        // and later still under the load of the whole suite. Start the sixty ticks only once the apples
        // and the hoppers tick (the same race as the scan fixture, docs/analysis/2026-10-26-unwired-tickers-golem-marks-ru.md).
        test.startSequence().thenWaitUntil(() -> {
            for (BlockPos spot : List.of(open, closed, fed, blocked)) {
                if (!level.isPositionEntityTicking(spot)) throw new net.minecraft.gametest.framework.GameTestAssertException("waiting for the grate chunks to tick: " + spot);
            }
        }).thenExecute(() -> {
            for (int index = 0; index < 2; index++) {
                items[index] = new ItemEntity(level, drop[index].getX() + .5, drop[index].getY() + 1.6, drop[index].getZ() + .5, new ItemStack(Items.APPLE));
                items[index].setDeltaMovement(Vec3.ZERO);
                items[index].setNoPickUpDelay();
                items[index].setUnlimitedLifetime();
                level.addFreshEntity(items[index]);
                spawned.add(items[index]);
            }
        }).thenIdle(60).thenExecute(() -> {
            try {
                test.assertTrue(items[0].getY() < open.getY() - 1.5D && items[0].getY() > open.getY() - 2.2D, "an apple dropped on an open grate falls through to the floor three blocks down: " + (items[0].getY() - open.getY()));
                test.assertTrue(Math.abs(items[1].getY() - (closed.getY() + 1.0D)) < 0.05D, "an apple dropped on a closed grate rests on its plate: " + (items[1].getY() - closed.getY()));
                AABB below = new AABB(fed).inflate(1.5D, 0.0D, 1.5D).setMinY(fed.getY() - 3.5D).setMaxY(fed.getY() + 0.9D);
                test.assertTrue(dropped(level, below, Items.STICK) == 3, "a hopper over an open grate empties itself into items falling below: " + dropped(level, below, Items.STICK));
                test.assertTrue(((HopperBlockEntity) level.getBlockEntity(fed.above())).isEmpty(), "the hopper over the open grate is empty");
                level.getEntitiesOfClass(ItemEntity.class, below).forEach(spawned::add);
                AABB under = new AABB(blocked).inflate(1.5D, 0.0D, 1.5D).setMinY(blocked.getY() - 3.5D).setMaxY(blocked.getY() + 0.9D);
                test.assertTrue(dropped(level, under, Items.STICK) == 0 && ((HopperBlockEntity) level.getBlockEntity(blocked.above())).getItem(0).getCount() == 3,
                        "a hopper over a closed grate keeps everything: " + dropped(level, under, Items.STICK));
            } finally {
                cleanup(level, forced, spawned);
            }
        }).thenSucceed();
    }
}
