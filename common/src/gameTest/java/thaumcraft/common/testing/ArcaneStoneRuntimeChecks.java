package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** One loader-neutral check using loaded production recipes, native menus, placement and survival harvest. */
public final class ArcaneStoneRuntimeChecks {
    private ArcaneStoneRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3));
        List<BlockPos> positions = List.of(pos, pos.east(), pos.below());
        var states = positions.stream().map(level::getBlockState).toList();
        var entities = new ArrayList<CompoundTag>();
        for (var at : positions) entities.add(level.getBlockEntity(at) == null ? null
                : level.getBlockEntity(at).saveWithFullMetadata(level.registryAccess()));
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var knowledge = KnowledgeAccess.get(player).copy();
        var oldPosition = player.position();
        var oldMode = player.gameMode.getGameModeForPlayer();
        int oldSelected = player.getInventory().selected;
        float oldYaw = player.getYRot(), oldExhaustion = player.getFoodData().getExhaustionLevel();
        boolean oldSneaking = player.isShiftKeyDown();
        AABB bounds = new AABB(pos).inflate(4);
        Set<UUID> oldDrops = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        try {
            player.closeContainer();
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.setPos(pos.getX() - 2, pos.getY(), pos.getZ() + .5);
            level.setBlock(pos, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState(), Block.UPDATE_ALL);
            test.assertTrue(use(player, hit(pos, Direction.UP, 1)).consumesAction()
                    && player.containerMenu instanceof ContainerArcaneWorkbench, "real workbench opens for masonry recipes");
            var menu = (ContainerArcaneWorkbench) player.containerMenu;
            var tile = (TileArcaneWorkbench) level.getBlockEntity(pos);
            var recipe = level.getRecipeManager().getAllRecipesFor(ThaumcraftArcane.ARCANE_RECIPE_TYPE.get()).stream()
                    .filter(holder -> holder.id().equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/arcane_stone")))
                    .findFirst().orElseThrow().value();
            test.assertTrue(recipe.research().equals("ARCANESTONE") && recipe.vis().equals(Map.of("terra", 1, "ignis", 1))
                    && ItemStack.matches(recipe.getResultItem(level.registryAccess()), new ItemStack(ThaumcraftMasonry.ARCANE_STONE.get(), 9)),
                    "ConfigRecipes205: research ARCANESTONE, terra1/ignis1, nine metadata6 stones");

            Item shard = ThaumcraftContent.SHARDS.values().iterator().next().get();
            ring(tile, Items.STONE, shard);
            ItemStack charged = wand(110);
            tile.setItem(10, charged.copy());
            KnowledgeAccess.get(player).removeResearch("ARCANESTONE");
            menu.broadcastChanges();
            var beforeRefusal = tile.saveWithoutMetadata(level.registryAccess());
            menu.clicked(0, 1, ClickType.PICKUP, player);
            test.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getCarried().isEmpty()
                    && beforeRefusal.equals(tile.saveWithoutMetadata(level.registryAccess())), "missing research cannot consume stone/shard/vis or grant output");
            KnowledgeAccess.get(player).addResearch("ARCANESTONE");
            ItemStack shortWand = charged.copy();
            shortWand.set(ThaumcraftContent.WAND_DATA.get(), shortWand.get(ThaumcraftContent.WAND_DATA.get()).withAmount("ignis", 109));
            tile.setItem(10, shortWand);
            menu.broadcastChanges();
            beforeRefusal = tile.saveWithoutMetadata(level.registryAccess());
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(!menu.craftable() && menu.getCarried().isEmpty()
                    && beforeRefusal.equals(tile.saveWithoutMetadata(level.registryAccess())), "insufficient centivis refuses the entire nine-stone craft");
            for (var invalid : List.of(Items.COBBLESTONE, Items.DEEPSLATE, Items.ANDESITE)) {
                ring(tile, invalid, shard);
                test.assertTrue(!recipe.matches(ArcaneRecipeInput.snapshot(tile), level), "original oreDict stone excludes " + invalid);
            }
            test.assertTrue(ThaumcraftContent.SHARDS.size() == 7, "all six original shard colors plus the balanced shard are represented");
            for (var originalShard : ThaumcraftContent.SHARDS.values()) {
                ring(tile, Items.STONE, originalShard.get());
                var inputs = ArcaneRecipeInput.snapshot(tile).items();
                tile.setItem(10, charged.copy());
                menu.broadcastChanges();
                test.assertTrue(recipe.matches(ArcaneRecipeInput.snapshot(tile), level) && menu.craftable()
                        && menu.requiredVis("terra") == 110 && menu.requiredVis("ignis") == 110 && menu.requiredVis("aer") == 0,
                        "every original shard matches actual JSON, with only iron-cap-priced earth/fire vis");
                menu.clicked(0, 1, ClickType.PICKUP, player);
                test.assertTrue(ItemStack.matches(menu.getCarried(), new ItemStack(ThaumcraftMasonry.ARCANE_STONE.get(), 9)),
                        "native right-click takes nine ordinary component-clean arcane stones, never half the output");
                for (int slot = 0; slot < 9; slot++) test.assertTrue(ItemStack.matches(tile.getItem(slot), inputs.get(slot).copyWithCount(1)),
                        "one ingredient consumed and remaining named components preserved in slot " + slot);
                ItemStack paid = charged.copy();
                paid.set(ThaumcraftContent.WAND_DATA.get(), charged.get(ThaumcraftContent.WAND_DATA.get()).withAmount("terra", 0).withAmount("ignis", 0));
                test.assertTrue(ItemStack.matches(tile.getItem(10), paid), "exact two-primal debit preserves wand name and all unrelated primal balances");
                menu.setCarried(ItemStack.EMPTY);
            }
            KnowledgeAccess.get(player).removeResearch("ARCANESTONE");
            tile.clearContent();
            plain(test, player, menu, tile, new String[] {"SS", "SS"}, ThaumcraftMasonry.ARCANE_STONE.get(), ThaumcraftMasonry.ARCANE_STONE_BRICKS.get(), 4);
            plain(test, player, menu, tile, new String[] {"K  ", "KK ", "KKK"}, ThaumcraftMasonry.ARCANE_STONE_BRICKS.get(), ThaumcraftMasonry.STAIRS.get(), 4);
            plain(test, player, menu, tile, new String[] {"KKK"}, ThaumcraftMasonry.ARCANE_STONE_BRICKS.get(), ThaumcraftMasonry.SLAB.get(), 6);
            tile.clearContent();
            player.closeContainer();

            var enchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            ItemStack silk = new ItemStack(Items.WOODEN_PICKAXE);
            silk.enchant(enchantments.getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
            ItemStack fortune = new ItemStack(Items.WOODEN_PICKAXE);
            fortune.enchant(enchantments.getHolderOrThrow(Enchantments.FORTUNE), 3);
            for (Block block : List.of(ThaumcraftMasonry.ARCANE_STONE.get(), ThaumcraftMasonry.ARCANE_STONE_BRICKS.get(),
                    ThaumcraftMasonry.STAIRS.get(), ThaumcraftMasonry.SLAB.get())) {
                var state = block.defaultBlockState();
                boolean full = !(block instanceof StairBlock) && !(block instanceof SlabBlock);
                test.assertTrue(state.getDestroySpeed(level, pos) == (full ? 4.0F : 2.0F)
                        && block.getExplosionResistance() == (full ? 20.0F : 6.0F), "original full/stair/slab hardness and effective explosion resistance (legacy setter10*3/5=6)");
                test.assertTrue(state.isRandomlyTicking() == full && (full || state.getLightBlock(level, pos) == 0
                        && !state.useShapeForLightOcclusion() && state.propagatesSkylightDown(level, pos)), "original cosmetic-solid tick flag and dry stair/slab opacity0");
                test.assertTrue(state.requiresCorrectToolForDrops() && new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(state)
                        && !new ItemStack(Items.WOODEN_AXE).isCorrectToolForDrops(state), "original rock harvest needs a pickaxe but no higher tier");
                test.assertTrue(!state.ignitedByLava() && state.getLightEmission() == 0
                        && state.is(BlockTags.BEACON_BASE_BLOCKS) == full, "nonflammable, unlit masonry; only full cosmetic-solid blocks are beacon bases");
                for (ItemStack tool : List.of(new ItemStack(Items.WOODEN_PICKAXE), silk, fortune)) {
                    var drops = Block.getDrops(state, level, pos, null, player, tool);
                    test.assertTrue(drops.size() == 1 && ItemStack.matches(drops.getFirst(), new ItemStack(block)), "ordinary/silk/fortune all drop exactly one clean self item");
                }
                harvest(test, player, pos, state, ItemStack.EMPTY, 0);
                harvest(test, player, pos, state, new ItemStack(Items.WOODEN_PICKAXE), 1);
            }
            var slab = ThaumcraftMasonry.SLAB.get();
            for (SlabType type : SlabType.values()) {
                var state = slab.defaultBlockState().setValue(SlabBlock.TYPE, type);
                int count = type == SlabType.DOUBLE ? 2 : 1;
                test.assertTrue(state.getLightBlock(level, pos) == 0 && state.propagatesSkylightDown(level, pos), "original slab opacity0 also applies to the dry double");
                for (ItemStack tool : List.of(new ItemStack(Items.WOODEN_PICKAXE), silk, fortune)) {
                    var drops = Block.getDrops(state, level, pos, null, player, tool);
                    test.assertTrue(drops.size() == 1 && ItemStack.matches(drops.getFirst(), new ItemStack(slab, count)), "native bottom/top/double slab loot counts are 1/1/2, including silk and fortune");
                }
                harvest(test, player, pos, state, silk, count);
            }

            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos.east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            player.setYRot(-90);
            place(test, player, new ItemStack(ThaumcraftMasonry.STAIRS.get(), 2), hit(pos.below(), Direction.UP, 1));
            test.assertTrue(level.getBlockState(pos).getValue(StairBlock.FACING) == Direction.EAST
                    && level.getBlockState(pos).getValue(StairBlock.HALF) == Half.BOTTOM, "real stair BlockItem uses native player-facing and bottom placement");
            level.setBlock(pos.east(), ThaumcraftMasonry.STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
            test.assertTrue(level.getBlockState(pos).getValue(StairBlock.SHAPE) == StairsShape.OUTER_LEFT, "native neighbor update forms an outer stair corner");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            player.setYRot(90);
            place(test, player, new ItemStack(ThaumcraftMasonry.STAIRS.get(), 2), hit(pos.below(), Direction.UP, 1));
            test.assertTrue(level.getBlockState(pos).getValue(StairBlock.SHAPE) == StairsShape.INNER_RIGHT, "native placement forms an inner stair corner against the same neighbor");
            level.setBlock(pos.east(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            place(test, player, new ItemStack(ThaumcraftMasonry.STAIRS.get(), 2), hit(pos.east(), Direction.WEST, .75));
            test.assertTrue(level.getBlockState(pos).getValue(StairBlock.HALF) == Half.TOP, "native side-hit high placement creates upside-down stairs");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            place(test, player, new ItemStack(slab, 3), hit(pos.below(), Direction.UP, 1));
            test.assertTrue(level.getBlockState(pos).getValue(SlabBlock.TYPE) == SlabType.BOTTOM, "native first slab places a bottom half");
            place(test, player, player.getMainHandItem().copy(), hit(pos, Direction.UP, .5));
            test.assertTrue(level.getBlockState(pos).getValue(SlabBlock.TYPE) == SlabType.DOUBLE
                    && !level.getBlockState(pos).getValue(SlabBlock.WATERLOGGED) && player.getMainHandItem().getCount() == 1,
                    "native merge consumes exactly the second named slab and forms one double state");
            test.assertTrue(!level.getBlockState(pos).canBeReplaced(new BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                    player.getMainHandItem(), hit(pos, Direction.UP, 1))), "double slab cannot merge a third slab or duplicate it");
            harvest(test, player, pos, level.getBlockState(pos), silk, 2);
            place(test, player, new ItemStack(slab, 2), hit(pos.east(), Direction.WEST, .75));
            test.assertTrue(level.getBlockState(pos).getValue(SlabBlock.TYPE) == SlabType.TOP, "native high side-hit places the top slab half");
            harvest(test, player, pos, level.getBlockState(pos), new ItemStack(Items.WOODEN_PICKAXE), 1);
        } finally {
            if (level.getBlockEntity(pos) instanceof TileArcaneWorkbench tile) tile.clearContent();
            player.containerMenu.setCarried(ItemStack.EMPTY);
            player.closeContainer();
            for (int i = 0; i < positions.size(); i++) {
                level.setBlock(positions.get(i), states.get(i), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                var entity = level.getBlockEntity(positions.get(i));
                if (entity != null && entities.get(i) != null) entity.loadWithComponents(entities.get(i), level.registryAccess());
            }
            for (var drop : level.getEntitiesOfClass(ItemEntity.class, bounds)) if (!oldDrops.contains(drop.getUUID())) drop.discard();
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = oldSelected;
            player.setGameMode(oldMode);
            player.setShiftKeyDown(oldSneaking);
            player.setPos(oldPosition);
            player.setYRot(oldYaw);
            player.getFoodData().setExhaustion(oldExhaustion);
            KnowledgeAccess.get(player).copyFrom(knowledge);
        }
        test.succeed();
    }

    private static ItemStack wand(int amount) {
        ItemStack stack = new ItemStack(ThaumcraftContent.WAND.get());
        WandData data = WandData.EMPTY;
        for (String primal : WandData.PRIMALS) data = data.withAmount(primal, amount);
        stack.set(ThaumcraftContent.WAND_DATA.get(), data);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("arcane-stone-wand-components"));
        return stack;
    }

    private static void ring(TileArcaneWorkbench tile, Item stone, Item shard) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = new ItemStack(slot == 4 ? shard : stone, 2);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("arcane-stone-input-" + slot));
            tile.setItem(slot, stack);
        }
    }

    private static void plain(GameTestHelper test, ServerPlayer player, ContainerArcaneWorkbench menu, TileArcaneWorkbench tile,
                              String[] pattern, Block input, Block output, int count) {
        tile.clearContent();
        for (int y = 0; y < pattern.length; y++) for (int x = 0; x < pattern[y].length(); x++) {
            if (pattern[y].charAt(x) == ' ') continue;
            ItemStack stack = new ItemStack(input, 2);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("ordinary-masonry-components"));
            tile.setItem(x + y * 3, stack);
        }
        var before = ArcaneRecipeInput.snapshot(tile).items();
        menu.broadcastChanges();
        test.assertTrue(menu.craftable() && WandData.PRIMALS.stream().allMatch(tag -> menu.requiredVis(tag) == 0), "bricks/stairs/slabs use real ordinary recipes without research or a wand");
        menu.clicked(0, 1, ClickType.PICKUP, player);
        test.assertTrue(ItemStack.matches(menu.getCarried(), new ItemStack(output, count)), "ConfigRecipes206-208 native output count/components: " + output);
        for (int slot = 0; slot < 9; slot++) test.assertTrue(ItemStack.matches(tile.getItem(slot), before.get(slot).isEmpty() ? ItemStack.EMPTY : before.get(slot).copyWithCount(1)), "ordinary craft consumes one per occupied slot, preserving remaining components");
        menu.setCarried(ItemStack.EMPTY);
    }

    private static void harvest(GameTestHelper test, ServerPlayer player, BlockPos pos, BlockState state, ItemStack tool, int count) {
        var level = test.getLevel();
        AABB bounds = new AABB(pos).inflate(3);
        Set<UUID> old = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool.copy());
        test.assertTrue(player.gameMode.destroyBlock(pos) && level.getBlockState(pos).isAir(), "real survival harvest removes masonry");
        var drops = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().filter(drop -> !old.contains(drop.getUUID())).toList();
        try {
            test.assertTrue(drops.stream().allMatch(drop -> ItemStack.isSameItemSameComponents(drop.getItem(), new ItemStack(state.getBlock())))
                    && drops.stream().mapToInt(drop -> drop.getItem().getCount()).sum() == count, "survival tool gate and exact self/slab counts, with no extra components or duplicate drops");
        } finally {
            drops.forEach(ItemEntity::discard);
        }
    }

    private static BlockHitResult hit(BlockPos pos, Direction face, double height) {
        return new BlockHitResult(new Vec3(pos.getX() + (face == Direction.WEST ? 0 : .5), pos.getY() + height, pos.getZ() + .5), face, pos, false);
    }

    private static net.minecraft.world.InteractionResult use(ServerPlayer player, BlockHitResult hit) {
        return player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    private static void place(GameTestHelper test, ServerPlayer player, ItemStack stack, BlockHitResult hit) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("placed-masonry-components"));
        ItemStack expected = stack.copyWithCount(stack.getCount() - 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        test.assertTrue(use(player, hit).consumesAction() && ItemStack.matches(player.getMainHandItem(), expected), "native BlockItem placement consumes exactly one and preserves remaining stack components");
    }
}
