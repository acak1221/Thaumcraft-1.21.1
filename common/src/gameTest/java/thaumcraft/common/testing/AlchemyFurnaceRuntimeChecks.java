package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockAlchemyFurnace;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.container.ContainerAlchemyFurnace;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileAlchemyFurnace;

/** One native machine-flow helper for the central loader tests; no injected recipes, fuels or aspect pools. */
public final class AlchemyFurnaceRuntimeChecks {
    private AlchemyFurnaceRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3));
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<BlockPos, CompoundTag> tags = new LinkedHashMap<>();
        for (int height = 0; height <= 6; height++) {
            BlockPos saved = pos.above(height);
            states.put(saved, level.getBlockState(saved));
            if (level.getBlockEntity(saved) != null) tags.put(saved, level.getBlockEntity(saved).saveWithFullMetadata(level.registryAccess()));
        }
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var position = player.position();
        var gameType = player.gameMode.getGameModeForPlayer();
        int selected = player.getInventory().selected;
        boolean sneaking = player.isShiftKeyDown();
        AABB box = new AABB(pos).inflate(3);
        Set<UUID> oldDrops = level.getEntitiesOfClass(ItemEntity.class, box).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        try {
            player.closeContainer();
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.setPos(Vec3.atCenterOf(pos).add(0, 1, 0));
            for (int height = 1; height <= 6; height++) level.setBlock(pos.above(height), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            TileAlchemyFurnace tile = fresh(level, pos);
            TileAlembic first = alembic(level, pos.above()), second = alembic(level, pos.above(2));
            test.assertTrue(use(player, pos).consumesAction() && player.containerMenu instanceof ContainerAlchemyFurnace, "native block use opens the real furnace menu");
            var menu = (ContainerAlchemyFurnace) player.containerMenu;
            test.assertTrue(menu.slots.size() == 38 && !menu.getSlot(0).isFake() && !menu.getSlot(1).isFake()
                    && !menu.getSlot(0).mayPlace(new ItemStack(Items.BARRIER)), "only two physical furnace slots; no preview/result can be taken");
            test.assertTrue(tile.getSlotsForFace(Direction.UP).length == 0 && tile.getSlotsForFace(Direction.DOWN)[0] == 1
                    && tile.getSlotsForFace(Direction.NORTH)[0] == 0 && !tile.canPlaceItemThroughFace(0, new ItemStack(Items.COAL), Direction.UP)
                    && !tile.canTakeItemThroughFace(1, new ItemStack(Items.COAL), Direction.DOWN)
                    && tile.canTakeItemThroughFace(1, new ItemStack(Items.BUCKET), Direction.DOWN), "original top-none / bottom-fuel-bucket / side-input automation rules");
            tile.setItem(0, new ItemStack(Items.BARRIER));
            tile.setItem(1, new ItemStack(Items.COAL, 2));
            ticks(level, tile, 1);
            test.assertTrue(tile.getBurnTime() == 0 && tile.getItem(1).getCount() == 2, "unknown input cannot ignite or spend native fuel");
            tile.setItem(0, new ItemStack(Items.COAL));
            ticks(level, tile, 1);
            test.assertTrue(tile.getBurnTime() == 1600 && tile.getCookTime() == 1 && tile.getSmeltTime() == 40
                    && tile.getItem(1).getCount() == 1 && !tile.isSpeedBoost(), "one native coal fuel starts the full four-vis / forty-tick cook");
            ticks(level, tile, 39);
            test.assertTrue(tile.getItem(0).isEmpty() && tile.getVis() == 4 && tile.getAspects().getAmount(Aspect.FIRE) == 2
                    && tile.getAspects().getAmount(Aspect.ENERGY) == 2 && first.getAmount() + second.getAmount() == 0,
                    "exactly one physical coal becomes ignis2/potentia2, with no fictitious item output or premature extraction");
            test.assertTrue(level.getBlockState(pos).getValue(BlockAlchemyFurnace.LIT) && level.getBlockState(pos).getValue(BlockAlchemyFurnace.FILLED)
                    && level.getBlockState(pos).getLightEmission() == 12, "native lit/filled world state and source light12 reflect the real machine");
            tile.setItem(0, new ItemStack(Items.BARRIER));
            int burn = tile.getBurnTime();
            ticks(level, tile, 1);
            test.assertTrue(tile.getBurnTime() == burn - 1 && tile.getCookTime() == 0 && tile.getItem(0).is(Items.BARRIER), "already-lit fuel still decrements while input cannot smelt");
            tile.removeItem(0, 1);
            ticks(level, tile, 37);
            test.assertTrue(first.getAmount() + second.getAmount() == 0, "ordinary extraction waits for the real forty-tick phase");
            ticks(level, tile, 1);
            test.assertTrue(tile.getVis() == 3 && first.getAmount() == 1 && second.getAmount() == 0, "second pass initializes only the first empty alembic");
            ticks(level, tile, 40);
            test.assertTrue(tile.getVis() == 1 && first.getAmount() == 2 && second.getAmount() == 1 && first.getAspect() != second.getAspect(),
                    "typed refill excludes that tag from the second-pass random choice, preserving all four units");
            ticks(level, tile, 40);
            test.assertTrue(tile.getVis() == 0 && first.getAmount() + second.getAmount() == 4, "normal furnace transfers every distilled unit exactly once to actual alembics");

            player.getInventory().setItem(9, new ItemStack(Items.COAL, 3));
            test.assertTrue(menu.quickMoveStack(player, 2).getCount() == 3 && tile.getItem(1).getCount() == 4 && tile.getItem(0).isEmpty(), "shift-click prefers fuel before input for a tagged fuel");
            tile.setItem(1, new ItemStack(Items.COAL, 64));
            player.getInventory().setItem(9, new ItemStack(Items.COAL, 3));
            test.assertTrue(menu.quickMoveStack(player, 2).getCount() == 3 && tile.getItem(0).getCount() == 3, "a full fuel slot falls back to the actual input slot");
            test.assertTrue(menu.quickMoveStack(player, 0).getCount() == 3 && tile.getItem(0).isEmpty(), "slot0 shifts the real unprocessed input back; it is not a crafted result");
            player.closeContainer();

            // Capacity is reached by real dissolution, not by injecting an aspect catalogue.
            tile = fresh(level, pos);
            for (int height = 1; height <= 6; height++) level.setBlock(pos.above(height), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            tile.setItem(0, new ItemStack(Items.COAL, 12));
            tile.setItem(1, new ItemStack(Items.COAL, 2));
            ticks(level, tile, 480);
            tile.setItem(0, new ItemStack(Items.TORCH, 2));
            ticks(level, tile, 20);
            test.assertTrue(tile.getVis() == 50 && tile.getItem(0).isEmpty(), "twelve coal and two real torches fill exactly the canonical fifty-vis buffer");
            tile.setItem(0, new ItemStack(Items.COAL));
            burn = tile.getBurnTime();
            ticks(level, tile, 1);
            test.assertTrue(tile.getVis() == 50 && tile.getItem(0).getCount() == 1 && tile.getBurnTime() == burn - 1, "full capacity refuses consumption but not ongoing fuel burn");
            var nearlyOut = tile.saveWithoutMetadata(level.registryAccess());
            nearlyOut.putShort("BurnTime", (short) 1);
            tile.loadWithComponents(nearlyOut, level.registryAccess());
            ticks(level, tile, 1);
            test.assertTrue(tile.getBurnTime() == 0 && tile.getItem(1).getCount() == 1 && tile.getItem(0).getCount() == 1, "a full buffer cannot light the next fuel item");

            tile = fresh(level, pos);
            first = alembic(level, pos.above());
            second = alembic(level, pos.above(2));
            tile.setItem(0, new ItemStack(Items.COAL));
            tile.setItem(1, new ItemStack(ThaumcraftContent.MATERIALS.get("alumentum").get(), 2));
            ticks(level, tile, 20);
            test.assertTrue(tile.isSpeedBoost() && tile.getCurrentItemBurnTime() == 6400 && tile.getSmeltTime() == 40
                    && tile.getCookTime() == 20 && tile.getVis() == 0, "Alumentum is native fuel6400, not an invented twenty-tick coal cook");
            ticks(level, tile, 20);
            test.assertTrue(tile.getVis() == 4 && first.getAmount() == 0, "Alumentum still consumes exactly forty cook ticks");
            ticks(level, tile, 19);
            test.assertTrue(first.getAmount() == 0, "boosted extraction does not run before its twenty-tick boundary");
            ticks(level, tile, 1);
            test.assertTrue(first.getAmount() == 1 && tile.getVis() == 3, "boosted extraction runs at tick60 rather than waiting until80");

            // Native typed-empty filter alias: first-pass refill pays once, then
            // the other empty must draw the non-excluded tag (one RNG call).
            first.clearContents();
            second.clearContents();
            first.setFilter(Aspect.ENERGY);
            var beforeFilter = tile.getVis();
            ticks(level, tile, 20);
            test.assertTrue(first.getAmount() == 1 && first.getAspect() == Aspect.ENERGY && second.getAmount() == 1
                    && second.getAspect() == Aspect.FIRE && tile.getVis() == beforeFilter - 2, "empty filter alias and remainder accounting conserve exactly two real transfers");

            // Original max-five consecutive scan; no extraction through a gap.
            tile = fresh(level, pos);
            for (int height = 1; height <= 6; height++) level.setBlock(pos.above(height), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            tile.setItem(0, new ItemStack(Items.COAL, 5));
            tile.setItem(1, new ItemStack(Items.COAL));
            ticks(level, tile, 200);
            var stack = new ArrayList<TileAlembic>();
            for (int height = 1; height <= 6; height++) {
                var target = alembic(level, pos.above(height));
                target.addToContainer(Aspect.FIRE, 1);
                stack.add(target);
            }
            ticks(level, tile, 40);
            test.assertTrue(tile.getVis() == 15 && stack.subList(0, 5).stream().allMatch(target -> target.getAmount() == 2)
                    && stack.get(5).getAmount() == 1, "first pass refills all five consecutive typed alembics, never a sixth");
            level.setBlock(pos.above(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            ticks(level, tile, 40);
            test.assertTrue(tile.getVis() == 14 && stack.getFirst().getAmount() == 3 && stack.get(2).getAmount() == 2, "a non-alembic gap stops both passes rather than behaving like a global pipe");

            tile = fresh(level, pos);
            tile.setItem(0, named(new ItemStack(Items.COAL, 2), "persistent-input-components"));
            tile.setItem(1, new ItemStack(Items.LAVA_BUCKET));
            ticks(level, tile, 1);
            test.assertTrue(tile.getCurrentItemBurnTime() == 20000 && tile.getItem(1).is(Items.BUCKET) && tile.getItem(1).getCount() == 1, "native fuel remainder replaces the last lava bucket with exactly one empty bucket");
            ticks(level, tile, 46);
            tile.setCustomName(Component.literal("persistent-alchemy-furnace"));
            test.assertTrue(use(player, pos).consumesAction(), "real furnace menu reopens for persistence/authority checks");
            var stale = (ContainerAlchemyFurnace) player.containerMenu;
            var saved = tile.saveWithFullMetadata(level.registryAccess());
            test.assertTrue(saved.contains("BurnTime", Tag.TAG_SHORT) && saved.contains("CookTime", Tag.TAG_SHORT), "original burn/cook shorts are deliberately preserved in native NBT");
            // Exercise the native menu transport above signed-short range. These
            // are display durations loaded through the actual BE, not a GUI preview.
            saved.putInt("CurrentItemBurnTime", 70000);
            saved.putInt("SmeltTime", 90000);
            saved.putBoolean("speedBoost", true);
            level.removeBlockEntity(pos);
            var loaded = new TileAlchemyFurnace(pos, level.getBlockState(pos));
            loaded.loadWithComponents(saved, level.registryAccess());
            level.setBlockEntity(loaded);
            test.assertTrue(loaded.getCookTime() == 7 && loaded.getBurnTime() == 19954 && loaded.isSpeedBoost()
                    && loaded.getCurrentItemBurnTime() == 70000 && loaded.getSmeltTime() == 90000
                    && loaded.getVis() == 4 && loaded.getAspects().aspects.equals(tile.getAspects().aspects)
                    && loaded.getName().getString().equals("persistent-alchemy-furnace")
                    && ItemStack.matches(tile.getItem(0), loaded.getItem(0)), "native BE round trip retains progress, boost, custom name, components and full UI durations");
            var beforeStale = loaded.saveWithoutMetadata(level.registryAccess());
            stale.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(!stale.stillValid(player) && stale.quickMoveStack(player, 0).isEmpty()
                    && beforeStale.equals(loaded.saveWithoutMetadata(level.registryAccess())), "exact BE identity rejects stale clicks and shift transfers without touching the replacement");
            player.closeContainer();
            use(player, pos);
            menu = (ContainerAlchemyFurnace) player.containerMenu;
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeBlockPos(pos);
                var client = new ContainerAlchemyFurnace(menu.containerId, player.getInventory(), buffer);
                // A fresh native server menu gives the listener its initial
                // dirty data, without replacing the player's network synchronizer.
                var dataSource = new ContainerAlchemyFurnace(menu.containerId, player.getInventory(), loaded);
                dataSource.addSlotListener(new ContainerListener() {
                    @Override public void slotChanged(AbstractContainerMenu ignored, int slot, ItemStack item) {}
                    @Override public void dataChanged(AbstractContainerMenu ignored, int index, int value) { client.setData(index, (short) value); }
                });
                dataSource.broadcastChanges();
                test.assertTrue(client.getCurrentItemBurnTime() == 70000 && client.getSmeltTime() == 90000 && client.getCookTime() == 7
                        && client.getBurnTime() == 19954 && client.getVis() == 4 && client.isSpeedBoost(), "all live menu integers survive native listener delivery through signed-short values using low/high halves");
            } finally { buffer.release(); }
            player.setPos(Vec3.atCenterOf(pos).add(9, 0, 0));
            test.assertTrue(!menu.stillValid(player) && menu.quickMoveStack(player, 0).isEmpty(), "distance squared above64 refuses the still-identical BE menu");
            player.setPos(Vec3.atCenterOf(pos).add(0, 1, 0));
            player.closeContainer();
            loaded.setItem(0, named(new ItemStack(Items.IRON_INGOT, 7), "drop-input-components"));
            loaded.setItem(1, named(new ItemStack(Items.COAL, 11), "drop-fuel-components"));
            ItemStack input = loaded.getItem(0).copy(), fuel = loaded.getItem(1).copy();
            var neighbor = level.getBlockEntity(pos.above()).saveWithoutMetadata(level.registryAccess());
            var blockDrops = Block.getDrops(level.getBlockState(pos), level, pos, loaded);
            test.assertTrue(blockDrops.size() == 1 && blockDrops.getFirst().is(ThaumcraftDistillation.ALCHEMY_FURNACE_ITEM.get()), "normal self loot is one furnace, without stored aspects or a fabricated result");
            // Earlier in this fixture a typed filter was removed with its
            // alembic; its legitimate blank label is also delivered by this
            // shared queue. Establish the item baseline AFTER that delivery,
            // rather than misidentifying the earlier label as furnace output.
            DeferredItemDrops.tick(level);
            var existing = level.getEntitiesOfClass(ItemEntity.class, box).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            loaded.evacuate();
            DeferredItemDrops.tick(level);
            var drops = level.getEntitiesOfClass(ItemEntity.class, box).stream().filter(item -> !existing.contains(item.getUUID())).toList();
            test.assertTrue(loaded.isEmpty() && drops.size() == 2 && drops.stream().anyMatch(item -> ItemStack.matches(item.getItem(), input))
                    && drops.stream().anyMatch(item -> ItemStack.matches(item.getItem(), fuel))
                    && neighbor.equals(level.getBlockEntity(pos.above()).saveWithoutMetadata(level.registryAccess())),
                    "native onRemove defers exactly two full component-preserving stacks once and never mutates the adjacent alembic; emptied="
                            + loaded.isEmpty() + "; drops=" + drops.stream().map(item -> item.getItem().save(level.registryAccess()).toString()).toList()
                            + "; neighborBefore=" + neighbor + "; neighborAfter=" + level.getBlockEntity(pos.above()).saveWithoutMetadata(level.registryAccess()));
        } finally {
            player.closeContainer();
            if (level.getBlockEntity(pos) instanceof TileAlchemyFurnace tile) tile.clearContent();
            for (var entry : states.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            for (var entry : tags.entrySet()) if (level.getBlockEntity(entry.getKey()) != null) level.getBlockEntity(entry.getKey()).loadWithComponents(entry.getValue(), level.registryAccess());
            DeferredItemDrops.tick(level);
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, box)) if (!oldDrops.contains(entity.getUUID())) entity.discard();
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected;
            player.setGameMode(gameType);
            player.setShiftKeyDown(sneaking);
            player.setPos(position);
        }
        test.succeed();
    }

    private static TileAlchemyFurnace fresh(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TileAlchemyFurnace old) old.clearContent();
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos, ThaumcraftDistillation.ALCHEMY_FURNACE.get().defaultBlockState(), Block.UPDATE_ALL);
        return (TileAlchemyFurnace) level.getBlockEntity(pos);
    }
    private static TileAlembic alembic(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos, ThaumcraftAlembics.ALEMBIC.get().defaultBlockState(), Block.UPDATE_ALL);
        return (TileAlembic) level.getBlockEntity(pos);
    }
    private static void ticks(ServerLevel level, TileAlchemyFurnace tile, int ticks) {
        for (int tick = 0; tick < ticks; tick++) TileAlchemyFurnace.tick(level, tile.getBlockPos(), level.getBlockState(tile.getBlockPos()), tile);
    }
    private static ItemStack named(ItemStack stack, String name) { stack.set(DataComponents.CUSTOM_NAME, Component.literal(name)); return stack; }
    private static net.minecraft.world.InteractionResult use(ServerPlayer player, BlockPos pos) {
        return player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false));
    }
}
