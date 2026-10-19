package thaumcraft.common.testing;

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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.BlockAlembic;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileJar;

/** One native helper, integrated by the root's loader tests; no injected recipes or transport engine. */
public final class AlembicRuntimeChecks {
    private AlembicRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3)), jarPos = pos.east();
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<BlockPos, CompoundTag> tags = new LinkedHashMap<>();
        for (BlockPos saved : new BlockPos[] {pos, jarPos}) {
            states.put(saved, level.getBlockState(saved));
            if (level.getBlockEntity(saved) != null) tags.put(saved, level.getBlockEntity(saved).saveWithFullMetadata(level.registryAccess()));
        }
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); ++slot) inventory.add(player.getInventory().getItem(slot).copy());
        var knowledge = KnowledgeAccess.get(player).copy();
        var knowledgeBefore = KnowledgeAccess.get(player).save();
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
            player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
            level.setBlock(pos, ThaumcraftAlembics.ALEMBIC.get().defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(jarPos, ThaumcraftContent.JAR.get().defaultBlockState(), Block.UPDATE_ALL);
            var tile = (TileAlembic) level.getBlockEntity(pos);
            var jar = (TileJar) level.getBlockEntity(jarPos);
            IAspectContainer container = tile;
            IEssentiaTransport transport = tile;
            test.assertTrue(tile.getMaxAmount() == 32 && tile.getAmount() == 0 && container.getAspects().size() == 0,
                    "native registered alembic is an empty 32-unit TC4 container");
            // Exercise the real remainder contract used by the furnace. Its
            // actual smelt/tick/topology is checked by the furnace's own unit.
            AspectList furnaceStock = new AspectList().add(Aspect.AIR, 40);
            int accepted = 40 - container.addToContainer(Aspect.AIR, 40);
            furnaceStock.reduce(Aspect.AIR, accepted);
            test.assertTrue(accepted == 32 && furnaceStock.getAmount(Aspect.AIR) == 8 && tile.getAmount() == 32,
                    "furnace-style debit pays only the actually accepted amount, not the eight-unit remainder");
            var full = tile.saveWithoutMetadata(level.registryAccess());
            test.assertTrue(container.addToContainer(Aspect.AIR, 1) == 1 && container.addToContainer(Aspect.WATER, 8) == 8
                    && !container.takeFromContainer(Aspect.WATER, 8) && !container.takeFromContainer(Aspect.AIR, 33)
                    && !container.takeFromContainer(Aspect.AIR, -1) && container.addToContainer(null, 8) == 8
                    && full.equals(tile.saveWithoutMetadata(level.registryAccess())), "refused full/mismatched/malformed transfers preserve the entire buffer");
            container.setAspects(new AspectList().add(Aspect.WATER, 1));
            test.assertTrue(full.equals(tile.saveWithoutMetadata(level.registryAccess())), "original alembic setAspects is not a replacement/creative-fill API");

            tile.clearContents();
            tile.setFilter(Aspect.WATER);
            test.assertTrue(tile.getFilter() == Aspect.WATER && tile.getAspect() == Aspect.WATER && tile.getAmount() == 0,
                    "state-only filter hook seeds the empty alembic type without fabricating any essentia");
            test.assertTrue(container.addToContainer(Aspect.AIR, 16) == 0 && tile.getFilter() == Aspect.WATER,
                    "original filter guides furnace selection, not the unconditional empty addToContainer rule");
            ItemStack empties = new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get(), 2);
            empties.set(DataComponents.CUSTOM_NAME, Component.literal("unconsumed-phial-components"));
            player.setItemInHand(InteractionHand.MAIN_HAND, empties);
            test.assertTrue(use(player, pos, Direction.UP).consumesAction() && tile.getAmount() == 8
                    && player.getMainHandItem().getCount() == 1 && player.getMainHandItem().has(DataComponents.CUSTOM_NAME),
                    "native empty phial withdraws exactly eight and preserves the other named phial");
            int filledSlot = filledSlot(player);
            test.assertTrue(filledSlot >= 0 && ItemEssence.data(player.getInventory().getItem(filledSlot)).tag().equals(Aspect.AIR.getTag()),
                    "withdrawal creates the real registered aspect-component phial, not a fake item/tag");
            player.getInventory().setItem(8, player.getMainHandItem().copy());
            player.setItemInHand(InteractionHand.MAIN_HAND, player.getInventory().removeItem(filledSlot, 1));
            test.assertTrue(use(player, jarPos, Direction.UP).consumesAction() && jar.getAmount() == PhialData.AMOUNT
                    && jar.getAspect() == Aspect.AIR && tile.getAmount() == 8,
                    "the actual withdrawn phial inserts all eight into the existing native jar pathway");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemEssence.filled(Aspect.AIR.getTag()));
            var beforeInsert = tile.saveWithoutMetadata(level.registryAccess());
            var held = player.getMainHandItem().copy();
            use(player, pos, Direction.NORTH);
            test.assertTrue(beforeInsert.equals(tile.saveWithoutMetadata(level.registryAccess())) && ItemStack.matches(held, player.getMainHandItem()),
                    "original filled-phial insertion into an alembic is refused without consuming anything");
            jar.addToContainer(Aspect.AIR, jar.getMaxAmount() - jar.getAmount() - 4);
            use(player, jarPos, Direction.UP);
            test.assertTrue(jar.getAmount() == 60 && ItemStack.matches(held, player.getMainHandItem()),
                    "only four free jar units cannot partially drain the eight-unit phial");

            tile.clearContents();
            tile.addToContainer(Aspect.AIR, 7);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get()));
            var beforeShort = tile.saveWithoutMetadata(level.registryAccess());
            held = player.getMainHandItem().copy();
            use(player, pos, Direction.UP);
            test.assertTrue(beforeShort.equals(tile.saveWithoutMetadata(level.registryAccess())) && ItemStack.matches(held, player.getMainHandItem()),
                    "seven units cannot partly fill an empty phial");
            tile.addToContainer(Aspect.AIR, 9);
            player.setGameMode(GameType.CREATIVE);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get(), 2));
            use(player, pos, Direction.UP);
            test.assertTrue(tile.getAmount() == 8 && player.getMainHandItem().getCount() == 1,
                    "source creative extraction consumes the empty phial too");
            player.setGameMode(GameType.SURVIVAL);

            ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
            ItemWandCasting.addRealVis(wand, Aspect.AIR.getTag(), 2500, true);
            wand.set(DataComponents.CUSTOM_NAME, Component.literal("alembic-free-facing"));
            player.setItemInHand(InteractionHand.MAIN_HAND, wand.copy());
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                test.assertTrue(use(player, pos, facing).consumesAction() && tile.getFacing() == facing
                        && level.getBlockState(pos).getValue(BlockAlembic.FACING) == facing
                        && ItemStack.matches(wand, player.getMainHandItem()), "native wand sets the exact horizontal face for no vis: " + facing);
                for (Direction face : Direction.values()) test.assertTrue(transport.canOutputTo(face) == (face != Direction.DOWN && face != facing)
                        && transport.isConnectable(face) == transport.canOutputTo(face) && !transport.canInputFrom(face), "native output sidedness: " + facing + "/" + face);
            }
            Direction beforeVertical = tile.getFacing();
            use(player, pos, Direction.UP);
            use(player, pos, Direction.DOWN);
            test.assertTrue(tile.getFacing() == beforeVertical && tile.getAmount() == 8, "top/bottom wand clicks neither turn nor empty the alembic");
            player.setShiftKeyDown(true);
            use(player, pos, Direction.EAST);
            test.assertTrue(tile.getFacing() == Direction.EAST && tile.getAmount() == 8 && ItemStack.matches(wand, player.getMainHandItem()),
                    "actual secondary-use wand hook turns the alembic without inventing sneak-wand emptying");
            player.setShiftKeyDown(false);
            for (Direction face : Direction.values()) test.assertTrue(transport.addEssentia(Aspect.AIR, 8, face) == 0
                    && transport.getSuctionAmount(face) == 0 && transport.getSuctionType(face) == null,
                    "no tube input or fake suction engine: " + face);
            test.assertTrue(transport.takeEssentia(Aspect.AIR, 1, Direction.EAST) == 0
                    && transport.takeEssentia(Aspect.AIR, 1, Direction.DOWN) == 0
                    && transport.takeEssentia(Aspect.AIR, 9, Direction.UP) == 0 && tile.getAmount() == 8,
                    "blocked output and oversized take preserve the full amount");
            test.assertTrue(transport.takeEssentia(Aspect.AIR, 1, Direction.WEST) == 1 && tile.getAmount() == 7,
                    "real permitted output takes exactly the requested essentia");

            var saved = tile.saveWithFullMetadata(level.registryAccess());
            var savedState = level.getBlockState(pos);
            level.removeBlockEntity(pos);
            tile = new TileAlembic(pos, savedState);
            tile.loadWithComponents(saved, level.registryAccess());
            level.setBlockEntity(tile);
            test.assertTrue(tile.getAspect() == Aspect.AIR && tile.getAmount() == 7 && tile.getFilter() == Aspect.WATER
                    && tile.getFacing() == Direction.EAST && saved.getByte("facing") == 5 && tile.getUpdatePacket() != null,
                    "native BE round trip and update data preserve aspect/amount/filter and blockstate-native direction5");
            player.setPos(pos.getX() + 20, pos.getY() + 1, pos.getZ());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get()));
            tile.addToContainer(Aspect.AIR, 1);
            var beforeRemote = tile.saveWithoutMetadata(level.registryAccess());
            use(player, pos, Direction.UP);
            test.assertTrue(beforeRemote.equals(tile.saveWithoutMetadata(level.registryAccess())) && player.getMainHandItem().getCount() == 1,
                    "out-of-native-reach interaction cannot withdraw essentia");
            player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
            // Inventory-full paid output survives using the existing durable
            // drop path; only flush outside any setBlock/onRemove operation.
            player.getInventory().clearContent();
            for (int slot = 0; slot < 36; ++slot) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get(), 2));
            use(player, pos, Direction.UP);
            DeferredItemDrops.tick(level);
            test.assertTrue(tile.getAmount() == 0 && tile.getAspect() == null && player.getMainHandItem().getCount() == 1
                    && level.getEntitiesOfClass(ItemEntity.class, box).stream().anyMatch(entity -> !oldDrops.contains(entity.getUUID())
                    && entity.getItem().is(ThaumcraftContent.FILLED_PHIAL.get()) && ItemEssence.data(entity.getItem()).tag().equals(Aspect.AIR.getTag())),
                    "full inventory retains one real eight-unit output phial, without losing or duplicating essentia");
            tile.setFilter(null);
            tile.addToContainer(Aspect.AIR, 32);
            test.assertTrue(level.getBlockState(pos).getAnalogOutputSignal(level, pos) == 15, "original full alembic comparator value is fifteen");
            player.getInventory().clearContent();
            player.setShiftKeyDown(true);
            use(player, pos, Direction.UP);
            test.assertTrue(tile.getAmount() == 0 && tile.getAspect() == null, "native sneak-empty-hand action explicitly clears contents");
            test.assertTrue(knowledgeBefore.equals(KnowledgeAccess.get(player).save()), "transfers/refusals never award discoveries or mutate player knowledge");
            var drops = Block.getDrops(level.getBlockState(pos), level, pos, tile);
            test.assertTrue(drops.size() == 1 && drops.getFirst().is(ThaumcraftAlembics.ALEMBIC_ITEM.get())
                    && !drops.getFirst().has(DataComponents.BLOCK_ENTITY_DATA), "original alembic drops itself, not an essence-preserving jar item");
        } finally {
            if (level.getBlockEntity(pos) instanceof TileAlembic tile) { tile.setFilter(null); tile.clearContents(); }
            for (var entry : states.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            for (var entry : tags.entrySet()) if (level.getBlockEntity(entry.getKey()) != null) level.getBlockEntity(entry.getKey()).loadWithComponents(entry.getValue(), level.registryAccess());
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, box)) if (!oldDrops.contains(entity.getUUID())) entity.discard();
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); ++slot) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected;
            player.setGameMode(gameType);
            player.setShiftKeyDown(sneaking);
            player.setPos(position);
            KnowledgeAccess.get(player).copyFrom(knowledge);
        }
        test.succeed();
    }

    private static int filledSlot(ServerPlayer player) {
        for (int slot = 0; slot < 36; ++slot) if (player.getInventory().getItem(slot).is(ThaumcraftContent.FILLED_PHIAL.get())) return slot;
        return -1;
    }

    private static InteractionResult use(ServerPlayer player, BlockPos pos, Direction face) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(face, .5), face, pos, false);
        return player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }
}
