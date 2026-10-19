package thaumcraft.common.testing;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
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
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
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
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.JarLabelData;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileJar;

/** Native recipes, hand interactions, drops and persistence; no injected content. */
public final class JarLabelRuntimeChecks {
    private JarLabelRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel();
        BlockPos jarPos = test.absolutePos(new BlockPos(3, 2, 3)), alembicPos = jarPos.east();
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<BlockPos, CompoundTag> tags = new LinkedHashMap<>();
        for (BlockPos pos : new BlockPos[] {jarPos, jarPos.below(), alembicPos}) {
            states.put(pos, level.getBlockState(pos));
            if (level.getBlockEntity(pos) != null) tags.put(pos, level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()));
        }
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); ++slot) inventory.add(player.getInventory().getItem(slot).copy());
        var crafting = player.inventoryMenu.getCraftSlots();
        var oldCrafting = new ArrayList<ItemStack>();
        for (int slot = 0; slot < crafting.getContainerSize(); ++slot) oldCrafting.add(crafting.getItem(slot).copy());
        ItemStack oldCursor = player.inventoryMenu.getCarried().copy();
        var position = player.position();
        var gameType = player.gameMode.getGameModeForPlayer();
        int selected = player.getInventory().selected;
        boolean sneaking = player.isShiftKeyDown();
        AABB box = new AABB(jarPos).inflate(3);
        Set<UUID> oldDrops = level.getEntitiesOfClass(ItemEntity.class, box).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        try {
            player.closeContainer();
            crafting.clearContent();
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.setPos(jarPos.getX() + .5, jarPos.getY() + 1, jarPos.getZ() + .5);
            level.setBlock(jarPos, ThaumcraftContent.JAR.get().defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(alembicPos, ThaumcraftAlembics.ALEMBIC.get().defaultBlockState(), Block.UPDATE_ALL);
            var jar = (TileJar) level.getBlockEntity(jarPos);
            var alembic = (TileAlembic) level.getBlockEntity(alembicPos);
            test.assertTrue(ItemJarLabel.blank().getItem() instanceof ItemJarLabel && jar.getMaxAmount() == 64 && alembic.getMaxAmount() == 32,
                    "real label item factory and original jar/alembic capacities are registered");

            var paper = new ItemStack(Items.PAPER);
            // Original dyeBlack included the old dye:0 ink sac. Modern
            // convention dyes/black alone does not guarantee that ingredient.
            var blankInput = CraftingInput.of(3, 2, List.of(new ItemStack(Items.INK_SAC), new ItemStack(Items.SLIME_BALL), paper, paper.copy(), paper.copy(), paper.copy()));
            var blankRecipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, blankInput, level).orElseThrow();
            ItemStack blanks = blankRecipe.value().assemble(blankInput, level.registryAccess());
            test.assertTrue(blanks.is(ItemJarLabel.blank().getItem()) && blanks.getCount() == 4 && ItemJarLabel.data(blanks) == null,
                     "original ordinary six-ingredient shapeless recipe makes four genuinely blank labels");
            var modernInput = CraftingInput.of(3, 2, List.of(new ItemStack(Items.BLACK_DYE), new ItemStack(Items.SLIME_BALL), paper, paper.copy(), paper.copy(), paper.copy()));
            var modernRecipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, modernInput, level).orElseThrow();
            test.assertTrue(modernRecipe.id().equals(blankRecipe.id())
                    && ItemStack.matches(blanks, modernRecipe.value().assemble(modernInput, level.registryAccess())),
                    "original ink sac AND modern black dye resolve the SAME real four-blank-label recipe; native convention-tag coverage cannot omit the original input");
            ItemStack phials = ItemEssence.filled(Aspect.WATER.getTag());
            phials.setCount(2);
            phials.set(DataComponents.CUSTOM_NAME, Component.literal("label-catalyst-components"));
            crafting.setItem(0, blanks.copyWithCount(3));
            crafting.setItem(1, phials.copy());
            player.inventoryMenu.slotsChanged(crafting);
            player.inventoryMenu.clicked(0, 0, ClickType.QUICK_MOVE, player);
            test.assertTrue(crafting.getItem(0).isEmpty() && ItemStack.matches(phials, crafting.getItem(1))
                    && countLabels(player, Aspect.WATER) == 3, "native shift-crafting pays each label exactly once and retains precisely the two component-bearing eight-unit phials");
            player.inventoryMenu.clicked(0, 0, ClickType.QUICK_MOVE, player);
            test.assertTrue(countLabels(player, Aspect.WATER) == 3 && ItemStack.matches(phials, crafting.getItem(1)),
                    "a repeated empty result click cannot duplicate labels or catalyst phials");
            ItemStack bound = findLabel(player, Aspect.WATER).copyWithCount(1);
            test.assertTrue(ItemStack.matches(bound, ItemStack.parse(level.registryAccess(), bound.save(level.registryAccess())).orElseThrow())
                    && JarLabelData.CODEC.parse(JsonOps.INSTANCE, new com.google.gson.JsonPrimitive("not_a_tc4_aspect")).error().isPresent(),
                    "native label component round-trips and rejects unknown saved aspect ids");
            var buffer = Unpooled.buffer();
            try {
                JarLabelData.STREAM_CODEC.encode(buffer, ItemJarLabel.data(bound));
                test.assertTrue(JarLabelData.STREAM_CODEC.decode(buffer).equals(ItemJarLabel.data(bound)), "label wire codec preserves the aspect identity without an amount");
                buffer.clear();
                ByteBufCodecs.STRING_UTF8.encode(buffer, "not_a_tc4_aspect");
                boolean invalidRejected = false;
                try { JarLabelData.STREAM_CODEC.decode(buffer); }
                catch (IllegalArgumentException expected) { invalidRejected = true; }
                test.assertTrue(invalidRejected, "label wire codec rejects unknown aspect ids too");
            } finally { buffer.release(); }
            crafting.clearContent();
            crafting.setItem(0, bound.copyWithCount(2));
            player.inventoryMenu.slotsChanged(crafting);
            int beforeBlank = countLabels(player, null);
            player.inventoryMenu.clicked(0, 0, ClickType.QUICK_MOVE, player);
            test.assertTrue(crafting.getItem(0).isEmpty() && countLabels(player, null) == beforeBlank + 2,
                    "native clear-label recipe returns one blank per paid bound label, with no retained binding");
            player.getInventory().clearContent();

            player.setItemInHand(InteractionHand.MAIN_HAND, blanks.copyWithCount(2));
            var before = jar.saveWithoutMetadata(level.registryAccess());
            use(player, jarPos, Direction.UP);
            test.assertTrue(before.equals(jar.saveWithoutMetadata(level.registryAccess())) && player.getMainHandItem().getCount() == 2,
                    "blank label on empty jar refuses without loss or a guessed primal aspect");
            player.setItemInHand(InteractionHand.MAIN_HAND, bound.copy());
            test.assertTrue(use(player, jarPos, Direction.UP).consumesAction() && jar.getFilter() == Aspect.WATER && jar.getAmount() == 0,
                    "a real craft-bound label configures an empty jar without creating essentia");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemEssence.filled(Aspect.AIR.getTag()));
            ItemStack held = player.getMainHandItem().copy();
            use(player, jarPos, Direction.UP);
            test.assertTrue(jar.getAmount() == 0 && ItemStack.matches(held, player.getMainHandItem()), "wrong-filter phial is refused intact");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemEssence.filled(Aspect.WATER.getTag()));
            test.assertTrue(use(player, jarPos, Direction.UP).consumesAction() && jar.getAmount() == 8 && jar.getAspect() == Aspect.WATER,
                    "the matching native phial inserts all eight units through the label filter");
            jar.addToContainer(Aspect.WATER, 56);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemEssence.filled(Aspect.WATER.getTag()));
            held = player.getMainHandItem().copy();
            use(player, jarPos, Direction.UP);
            test.assertTrue(jar.getAmount() == 64 && ItemStack.matches(held, player.getMainHandItem()), "full labelled jar refuses an entire phial without loss");
            player.setItemInHand(InteractionHand.MAIN_HAND, blanks.copyWithCount(2));
            use(player, jarPos, Direction.UP);
            test.assertTrue(player.getMainHandItem().getCount() == 2 && jar.getAmount() == 64, "an existing label cannot be overwritten or charged again");

            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            use(player, jarPos, jar.getFacing().getOpposite());
            test.assertTrue(jar.getAmount() == 0 && jar.getFilter() == Aspect.WATER, "original sneak-empty on another face empties contents, not the jar label");
            int dropped = blankDrops(level.getEntitiesOfClass(ItemEntity.class, box), oldDrops);
            use(player, jarPos, jar.getFacing());
            DeferredItemDrops.tick(level);
            test.assertTrue(jar.getFilter() == null && jar.getAmount() == 0 && blankDrops(level.getEntitiesOfClass(ItemEntity.class, box), oldDrops) == dropped + 1,
                    "sneaking on the label face clears the BE filter and drops one truly blank label");
            player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, blanks.copyWithCount(2));
            use(player, jarPos, Direction.UP);
            test.assertTrue(jar.getFilter() == null && player.getMainHandItem().getCount() == 2, "an empty jar's retained old type cannot bind a blank label");
            jar.addToContainer(Aspect.AIR, 8);
            player.setItemInHand(InteractionHand.MAIN_HAND, bound.copyWithCount(2));
            player.setGameMode(GameType.CREATIVE);
            use(player, jarPos, Direction.UP);
            test.assertTrue(jar.getFilter() == Aspect.AIR && jar.getAmount() == 8 && player.getMainHandItem().getCount() == 1,
                    "original filled-jar type overrides the bound label, consuming one label even in creative");
            player.setGameMode(GameType.SURVIVAL);
            int beforeBreak = blankDrops(level.getEntitiesOfClass(ItemEntity.class, box), oldDrops);
            level.destroyBlock(jarPos, true);
            var jarDrops = level.getEntitiesOfClass(ItemEntity.class, box).stream().filter(entity -> !oldDrops.contains(entity.getUUID())
                    && entity.getItem().is(ThaumcraftContent.JAR.get().asItem())).toList();
            test.assertTrue(jarDrops.size() == 1 && jarDrops.getFirst().getItem().getCount() == 1
                    && jarDrops.getFirst().getItem().getMaxStackSize() == 1
                    && ItemJarLabel.aspect(jarDrops.getFirst().getItem()) == Aspect.AIR
                    && blankDrops(level.getEntitiesOfClass(ItemEntity.class, box), oldDrops) == beforeBreak,
                    "native jar break emits exactly one contents/filter-bearing jar, not a duplicate loose label or loot-table jar");
            ItemStack jarItem = ItemStack.parse(level.registryAccess(), jarDrops.getFirst().getItem().save(level.registryAccess())).orElseThrow();
            jarDrops.getFirst().discard();
            level.setBlock(jarPos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            player.setItemInHand(InteractionHand.MAIN_HAND, jarItem);
            test.assertTrue(use(player, jarPos.below(), Direction.UP).consumesAction(), "native component-bearing jar item is placeable");
            jar = (TileJar) level.getBlockEntity(jarPos);
            test.assertTrue(jar != null && jar.getAmount() == 8 && jar.getAspect() == Aspect.AIR && jar.getFilter() == Aspect.AIR
                    && jar.getUpdateTag(level.registryAccess()).getString("AspectFilter").equals(Aspect.AIR.getTag()),
                    "real native break/save/place and update data retain both contents and filter");
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            use(player, jarPos, jar.getFacing());
            use(player, jarPos, Direction.UP);
            DeferredItemDrops.tick(level);
            var ordinaryJar = Block.getDrops(level.getBlockState(jarPos), level, jarPos, jar).getFirst();
            test.assertTrue(ordinaryJar.getMaxStackSize() == 64 && !ordinaryJar.has(DataComponents.BLOCK_ENTITY_DATA) && ItemJarLabel.data(ordinaryJar) == null,
                    "the re-placed jar returns to ordinary stackability after its label and contents are removed");
            player.setShiftKeyDown(false);

            player.setItemInHand(InteractionHand.MAIN_HAND, blanks.copyWithCount(2));
            use(player, alembicPos, Direction.UP);
            test.assertTrue(alembic.getFilter() == null && player.getMainHandItem().getCount() == 2, "blank label also refuses an empty alembic");
            alembic.addToContainer(Aspect.AIR, 16);
            test.assertTrue(use(player, alembicPos, Direction.UP).consumesAction() && alembic.getFilter() == Aspect.AIR
                    && alembic.getAmount() == 16 && player.getMainHandItem().getCount() == 1, "blank label binds to known alembic contents without changing any units");
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            dropped = blankDrops(level.getEntitiesOfClass(ItemEntity.class, box), oldDrops);
            use(player, alembicPos, Direction.DOWN);
            DeferredItemDrops.tick(level);
            test.assertTrue(alembic.getFilter() == null && alembic.getAmount() == 16
                    && blankDrops(level.getEntitiesOfClass(ItemEntity.class, box), oldDrops) == dropped + 1,
                    "any-face alembic removal returns a blank label and does not erase contents");
            use(player, alembicPos, Direction.UP);
            player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, bound.copyWithCount(2));
            ItemStack realHand = player.getMainHandItem();
            before = alembic.saveWithoutMetadata(level.registryAccess());
            ItemJarLabel.interact(realHand.copy(), level, alembicPos, player, InteractionHand.MAIN_HAND, hit(alembicPos, Direction.UP));
            test.assertTrue(before.equals(alembic.saveWithoutMetadata(level.registryAccess())) && realHand.getCount() == 2,
                    "a stale/copied handed-off stack cannot authorize a second label transaction");
            player.setPos(jarPos.getX() + 20, jarPos.getY() + 1, jarPos.getZ());
            use(player, alembicPos, Direction.UP);
            test.assertTrue(alembic.getFilter() == null && realHand.getCount() == 2, "out-of-native-reach label interaction has no effect");
            player.setPos(jarPos.getX() + .5, jarPos.getY() + 1, jarPos.getZ() + .5);
            player.setGameMode(GameType.CREATIVE);
            player.setShiftKeyDown(true);
            test.assertTrue(use(player, alembicPos, Direction.UP).consumesAction() && alembic.getFilter() == Aspect.WATER
                    && alembic.getAspect() == Aspect.WATER && alembic.getAmount() == 0 && player.getMainHandItem().getCount() == 1,
                    "native secondary-use in creative still pays a bound label and seeds only an empty alembic's type, never its amount");
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            dropped = blankDrops(level.getEntitiesOfClass(ItemEntity.class, box), oldDrops);
            level.destroyBlock(alembicPos, true);
            DeferredItemDrops.tick(level);
            test.assertTrue(blankDrops(level.getEntitiesOfClass(ItemEntity.class, box), oldDrops) == dropped + 1,
                    "original alembic destruction drops exactly one blank label, not a retained bound aspect");
        } finally {
            if (level.getBlockEntity(alembicPos) instanceof TileAlembic tile) tile.setFilter(null);
            crafting.clearContent();
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            for (var entry : states.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            for (var entry : tags.entrySet()) if (level.getBlockEntity(entry.getKey()) != null) level.getBlockEntity(entry.getKey()).loadWithComponents(entry.getValue(), level.registryAccess());
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, box)) if (!oldDrops.contains(entity.getUUID())) entity.discard();
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); ++slot) player.getInventory().setItem(slot, inventory.get(slot));
            for (int slot = 0; slot < oldCrafting.size(); ++slot) crafting.setItem(slot, oldCrafting.get(slot));
            player.inventoryMenu.setCarried(oldCursor);
            player.getInventory().selected = selected;
            player.setGameMode(gameType);
            player.setShiftKeyDown(sneaking);
            player.setPos(position);
        }
        test.succeed();
    }

    private static int countLabels(ServerPlayer player, Aspect aspect) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); ++slot) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof ItemJarLabel && ItemJarLabel.aspect(stack) == aspect) count += stack.getCount();
        }
        return count;
    }
    private static ItemStack findLabel(ServerPlayer player, Aspect aspect) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); ++slot) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof ItemJarLabel && ItemJarLabel.aspect(stack) == aspect) return stack;
        }
        throw new IllegalStateException("No native crafted label for " + aspect.getTag());
    }
    private static int blankDrops(List<ItemEntity> drops, Set<UUID> oldDrops) {
        return drops.stream().filter(entity -> !oldDrops.contains(entity.getUUID()) && entity.getItem().getItem() instanceof ItemJarLabel
                && ItemJarLabel.data(entity.getItem()) == null).mapToInt(entity -> entity.getItem().getCount()).sum();
    }
    private static BlockHitResult hit(BlockPos pos, Direction face) { return new BlockHitResult(Vec3.atCenterOf(pos).relative(face, .5), face, pos, false); }
    private static InteractionResult use(ServerPlayer player, BlockPos pos, Direction face) {
        return player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(pos, face));
    }
}
