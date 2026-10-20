package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileResearchTable;

/** R10 activation prerequisite: whole 100/127/128 discoveries survive actual native owners; no copying is invoked here. */
public final class ResearchNoteOverstackRuntimeChecks {
    private ResearchNoteOverstackRuntimeChecks() {}
    public static void run(GameTestHelper test, ServerPlayer player) {
        Fixture fixture = new Fixture(test, player);
        try { fixture.prepare(); } catch (RuntimeException | Error failure) { fixture.close(); throw failure; }
        test.runAfterDelay(5, () -> {
            try {
                for (int count : List.of(100, 127, 128)) fixture.lifecycle(count);
                fixture.ordinaryItemLimits();
                test.succeed();
            } finally { fixture.close(); }
        });
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper test;
        final ServerPlayer player;
        final ServerLevel level;
        final BlockPos tablePos, chestPos;
        final Vec3 position;
        final List<ItemStack> inventory = new ArrayList<>(), ender = new ArrayList<>();
        final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final List<ItemEntity> drops = new ArrayList<>();
        TileResearchTable table;
        ChestBlockEntity chest;
        Fixture(GameTestHelper test, ServerPlayer player) {
            this.test = test; this.player = player; level = test.getLevel(); position = player.position();
            tablePos = test.absolutePos(new BlockPos(3, 3, 3)); chestPos = tablePos.east(3);
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
            for (int slot = 0; slot < player.getEnderChestInventory().getContainerSize(); slot++) ender.add(player.getEnderChestInventory().getItem(slot).copy());
        }
        void set(BlockPos pos, BlockState state) {
            blocks.putIfAbsent(pos, level.getBlockState(pos));
            level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        void prepare() {
            player.closeContainer(); player.getInventory().clearContent(); player.getEnderChestInventory().clearContent();
            player.setPos(Vec3.atCenterOf(tablePos));
            forced.addAll(GolemChunks.hold(level, List.of(tablePos, chestPos)));
            set(tablePos, ThaumcraftContent.TABLE.get().state(2)); set(tablePos.north(), ThaumcraftContent.TABLE.get().state(7));
            set(chestPos, Blocks.CHEST.defaultBlockState()); set(chestPos.above(), Blocks.AIR.defaultBlockState());
            set(tablePos.below(), Blocks.STONE.defaultBlockState());
            table = (TileResearchTable) level.getBlockEntity(tablePos); chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
            test.assertTrue(table != null && chest != null, "real research table and chest hold the lifecycle fixture");
        }
        ItemStack note(int count) {
            ItemStack stack = ItemResearchNotes.create(ResearchNoteRuntimeChecks.solvedNitor(), true);
            stack.setCount(count); stack.set(DataComponents.CUSTOM_NAME, Component.literal("copied-discovery-" + count));
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            return stack;
        }
        void equal(ItemStack actual, ItemStack expected, String message) {
            test.assertTrue(ItemStack.matches(actual, expected), message + ": " + actual + " expected " + expected);
            test.assertTrue(actual.getMaxStackSize() == 1, "copied discoveries retain their original itemmax1");
        }
        int playerSlot(ItemStack expected) {
            int found = -1;
            for (int slot = 0; slot < 36; slot++) if (ItemStack.matches(player.getInventory().getItem(slot), expected)) {
                test.assertTrue(found == -1, "the entire discovery exists in only one player slot"); found = slot;
            }
            test.assertTrue(found >= 0, "one whole discovery stack was transferred to the player");
            return found;
        }
        int tableIndex(int inventorySlot) { return inventorySlot < 9 ? 29 + inventorySlot : 2 + inventorySlot - 9; }
        int chestIndex(int inventorySlot) { return inventorySlot < 9 ? 54 + inventorySlot : 27 + inventorySlot - 9; }

        void lifecycle(int count) {
            player.getInventory().clearContent(); player.getEnderChestInventory().clearContent(); chest.clearContent();
            ItemStack expected = note(count);
            equal(ItemStack.parse(level.registryAccess(), expected.save(level.registryAccess())).orElseThrow(), expected, "native ItemStack disk round trip " + count);
            var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            try {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(wire, expected);
                equal(ItemStack.OPTIONAL_STREAM_CODEC.decode(wire), expected, "native slot stream round trip " + count);
            } finally { wire.release(); }

            table.setItem(1, expected.copy());
            var menu = new ContainerResearchTable(41, player.getInventory(), table); player.containerMenu = menu;
            equal(menu.quickMoveStack(player, 1), expected, "table quick move reports the whole transferred note");
            test.assertTrue(table.getItem(1).isEmpty(), "the table relinquishes its one whole stack");
            int slot = playerSlot(expected);

            // Both the inventory's own codec and the complete player NBT path must preserve count and every component.
            ListTag inventoryTag = player.getInventory().save(new ListTag());
            player.getInventory().clearContent(); player.getInventory().load(inventoryTag);
            equal(player.getInventory().getItem(slot), expected, "native Inventory.save/load " + count);
            CompoundTag playerTag = player.saveWithoutId(new CompoundTag());
            var restoredPlayer = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "overstack-load"), ClientInformation.createDefault());
            restoredPlayer.load(playerTag);
            equal(restoredPlayer.getInventory().getItem(slot), expected, "native Player save/load " + count);

            var chestMenu = ChestMenu.threeRows(42, player.getInventory(), chest);
            player.containerMenu = chestMenu;
            chestMenu.quickMoveStack(player, chestIndex(slot));
            equal(chest.getItem(0), expected, "vanilla chest quick move retains the whole note");
            test.assertTrue(player.getInventory().getItem(slot).isEmpty(), "chest transfer leaves no player copy");
            var loadedChest = new ChestBlockEntity(chestPos, chest.getBlockState());
            loadedChest.loadWithComponents(chest.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            equal(loadedChest.getItem(0), expected, "native chest persistence " + count);
            chestMenu.quickMoveStack(player, 0); slot = playerSlot(expected);

            var enderMenu = ChestMenu.threeRows(43, player.getInventory(), player.getEnderChestInventory());
            player.containerMenu = enderMenu;
            enderMenu.quickMoveStack(player, chestIndex(slot));
            equal(player.getEnderChestInventory().getItem(0), expected, "ender chest quick move retains the whole note");
            ListTag enderTag = player.getEnderChestInventory().createTag(level.registryAccess());
            player.getEnderChestInventory().clearContent(); player.getEnderChestInventory().fromTag(enderTag, level.registryAccess());
            equal(player.getEnderChestInventory().getItem(0), expected, "native ender chest save/load " + count);
            enderMenu.quickMoveStack(player, 0); slot = playerSlot(expected);

            player.containerMenu = menu;
            menu.quickMoveStack(player, tableIndex(slot));
            equal(table.getItem(1), expected, "player-to-table return retains the same whole stack");
            menu.clicked(1, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            equal(menu.getCarried(), expected, "normal left click picks up the entire oversized note onto the cursor");
            test.assertTrue(table.getItem(1).isEmpty(), "cursor pickup removes the table's owner");
            menu.removed(player); player.containerMenu = player.inventoryMenu;
            test.assertTrue(menu.getCarried().isEmpty(), "table menu close hands the whole cursor to durable drop custody");
            DeferredItemDrops.tick(level);
            var entity = find(expected); equal(entity.getItem(), expected, "menu-close whole discovery drop");
            entityRoundTrip(entity, expected); entity.discard();

            var thrown = player.drop(expected.copy(), false);
            test.assertTrue(thrown != null, "native player throw creates an item entity");
            drops.add(thrown); equal(thrown.getItem(), expected, "native throw retains the whole note"); entityRoundTrip(thrown, expected); thrown.discard();

            player.getInventory().setItem(0, expected.copy()); player.getInventory().dropAll();
            var deathDrop = find(expected); equal(deathDrop.getItem(), expected, "native death-inventory dropAll retains the whole note");
            entityRoundTrip(deathDrop, expected); deathDrop.discard();
            test.assertTrue(player.getInventory().isEmpty(), "death-inventory drop owns everything once");
        }
        ItemEntity find(ItemStack expected) {
            var found = level.getEntitiesOfClass(ItemEntity.class, new AABB(tablePos).inflate(8), item -> item.isAlive() && ItemStack.matches(item.getItem(), expected));
            test.assertTrue(found.size() == 1, "exactly one whole matching discovery entity exists, got " + found.size());
            drops.add(found.getFirst()); return found.getFirst();
        }
        void entityRoundTrip(ItemEntity entity, ItemStack expected) {
            var loaded = (ItemEntity) entity.getType().create(level);
            test.assertTrue(loaded != null, "the actual registered drop entity can be restored");
            loaded.load(entity.saveWithoutId(new CompoundTag()));
            equal(loaded.getItem(), expected, "native ItemEntity save/load preserves its whole physical stack");
        }
        void ordinaryItemLimits() {
            ItemStack oversized = new ItemStack(Items.STONE, 100);
            test.assertTrue(ItemStack.CODEC.encodeStart(level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), oversized).error().isPresent(),
                    "ordinary items do not acquire an enlarged native save codec");
            chest.setItem(0, new ItemStack(Items.STONE, 100));
            test.assertTrue(chest.getItem(0).getCount() == 64, "the native chest still limits ordinary items to64");
            var malformed = new CompoundTag(); malformed.putString("id", "minecraft:stone"); malformed.putInt("count", 127);
            test.assertTrue(ItemStack.CODEC.parse(level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), malformed).error().isPresent(),
                    "a forged oversized ordinary item is rejected after native id/components decoding");
        }
        @Override public void close() {
            for (var drop : drops) if (drop.isAlive()) drop.discard();
            player.containerMenu = player.inventoryMenu;
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getEnderChestInventory().clearContent();
            for (int slot = 0; slot < ender.size(); slot++) player.getEnderChestInventory().setItem(slot, ender.get(slot));
            player.setPos(position);
            if (table != null) table.clearContent(); if (chest != null) chest.clearContent();
            blocks.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
            for (var chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
        }
    }
}
