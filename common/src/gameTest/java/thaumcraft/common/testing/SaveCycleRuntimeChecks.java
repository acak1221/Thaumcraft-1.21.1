package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.saveddata.SavedData;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.world.OuterLandsProvider;
import thaumcraft.common.tiles.TileArcaneFurnace;
import thaumcraft.common.tiles.TileCrucible;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileResearchTable;

/** Disk lifecycle checks. Fixture stock/session tags are inputs, not survival acquisition. */
public final class SaveCycleRuntimeChecks {
    private static final TicketType<BlockPos> TICKET = TicketType.create("thaumcraft_save_cycle", Comparator.comparingLong(BlockPos::asLong));
    private SaveCycleRuntimeChecks() {}

    public static void chunks(GameTestHelper test) { new ChunkCheck(test).prepare(); }

    private static final class ChunkCheck {
        final GameTestHelper test;
        final net.minecraft.server.level.ServerLevel level;
        final BlockPos base;
        final ChunkPos chunk;
        final Map<BlockPos, BlockEntity> old = new LinkedHashMap<>();
        final Map<BlockPos, CompoundTag> expected = new LinkedHashMap<>();
        int polls;
        ChunkCheck(GameTestHelper test) {
            this.test = test; level = test.getLevel();
            var origin = test.absolutePos(BlockPos.ZERO);
            base = new BlockPos(((origin.getX() + 1_200_000) >> 4) * 16 + 2, origin.getY() + 70,
                    ((origin.getZ() + 1_200_000) >> 4) * 16 + 2);
            chunk = new ChunkPos(base);
        }
        void prepare() {
            level.setChunkForced(chunk.x, chunk.z, true);
            try {
                level.getChunk(chunk.x, chunk.z);
                put(base, ThaumcraftContent.JAR.get().defaultBlockState());
                var jar = (TileJar)level.getBlockEntity(base);
                jar.addToContainer(Aspect.WATER, 37); jar.setFilter(Aspect.WATER); jar.setFacing(Direction.WEST);
                put(base.offset(2, 0, 0), ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState());
                ((TileCrucible)level.getBlockEntity(base.offset(2, 0, 0))).fillWater(725, true);
                put(base.offset(4, 0, 0), ThaumcraftLegacyTiles.ARCANE_FURNACE.get().defaultBlockState());
                var furnace = (TileArcaneFurnace)level.getBlockEntity(base.offset(4, 0, 0));
                ItemStack stack = new ItemStack(Items.COBBLESTONE, 23);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("disk-cycle-inventory"));
                furnace.setInventorySlotContents(7, stack);
                // Valid paired research table, so topology repair cannot legitimately evacuate the note.
                put(base.offset(6, 0, 0), ThaumcraftContent.TABLE.get().state(2));
                put(base.offset(6, 0, -1), ThaumcraftContent.TABLE.get().state(7));
                var table = (TileResearchTable)level.getBlockEntity(base.offset(6, 0, 0));
                table.setItem(1, ItemResearchNotes.create(new ResearchNoteData("INFUSION", 0x123456, false, 2,
                        List.of(new ResearchNoteData.Cell(0, 0, 1, "aer"))), false));
                table.setBonusPools(Map.of("aer", 7, "aqua", 3));
                put(base.offset(8, 0, 0), ThaumcraftInfusion.MATRIX.get().defaultBlockState());
                var matrix = (TileInfusionMatrix)level.getBlockEntity(base.offset(8, 0, 0));
                // Explicit selected-session fixture; no claim to have started a real recipe here.
                var session = matrix.saveWithoutMetadata(level.registryAccess());
                session.putBoolean("active", false); session.putBoolean("crafting", true);
                session.putInt("recipeinst", 5); session.putInt("instability", -8);
                session.putString("recipeplayer", "save-cycle"); session.putUUID("RecipePlayerId", UUID.randomUUID());
                new AspectList().add(Aspect.AIR, 0).add(Aspect.WATER, 11).writeToNBT(session);
                new AspectList().add(Aspect.AIR, 9).add(Aspect.WATER, 11).writeToNBT(session, "RecipeCost");
                var selected = level.getRecipeManager().getAllRecipesFor(ThaumcraftInfusion.INFUSION_RECIPE_TYPE.get()).stream()
                        .map(holder -> holder.value()).filter(recipe -> recipe.research().equals("ROD_silverwood")).findFirst().orElseThrow();
                var requirements = new net.minecraft.nbt.ListTag();
                var ops = level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                for (var predicate : selected.selectedComponents()) requirements.add(thaumcraft.common.crafting.InfusionComponentPredicate.CODEC.encodeStart(ops, predicate).getOrThrow());
                session.put("recipein", requirements);
                var input = new CompoundTag(); input.putInt("StoredCount", 1); input.put("Stack", selected.input().getItems()[0].save(level.registryAccess()));
                session.put("recipeinput", input);
                var output = new CompoundTag(); output.putInt("StoredCount", selected.result().getCount()); output.put("Stack", selected.result().copyWithCount(1).save(level.registryAccess()));
                session.put("recipeout", output);
                matrix.loadWithComponents(session, level.registryAccess()); matrix.setChanged();
                put(base.offset(10, 0, 0), ThaumcraftLegacyTiles.ARCANE_FURNACE.get().defaultBlockState());
                var unread = (TileArcaneFurnace)level.getBlockEntity(base.offset(10, 0, 0));
                var raw = unread.saveWithoutMetadata(level.registryAccess());
                var missingStack = new CompoundTag(); missingStack.putString("id", "thaumcraft:g7_absent_item"); missingStack.putInt("count", 3);
                var row = new CompoundTag(); row.putByte("Slot", (byte)2); row.put("Stack", missingStack);
                var items = new net.minecraft.nbt.ListTag(); items.add(row); raw.put("Items", items);
                unread.loadWithComponents(raw, level.registryAccess()); unread.setChanged();
                test.assertTrue(unread.isDataRejected(), "absent item registry name is quarantined, not decoded as empty stock");
                for (var entry : old.entrySet()) expected.put(entry.getKey(), entry.getValue().saveWithoutMetadata(level.registryAccess()));
                level.getChunkSource().save(true);
                level.setChunkForced(chunk.x, chunk.z, false);
                unload();
            } catch (RuntimeException | Error failure) { release(); throw failure; }
        }
        void put(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            test.assertTrue(level.getBlockState(pos).isAir(), "remote owned fixture starts in air");
            level.setBlock(pos, state, TubeVariantsRuntimeChecks.QUIET);
            old.put(pos, level.getBlockEntity(pos));
        }
        void unload() {
            test.runAfterDelay(5, () -> {
                try {
                    if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null || old.values().stream().anyMatch(be -> !be.isRemoved())) {
                        test.assertTrue(++polls < 160, "native chunk must unload and retire EVERY old BE: " + level.getChunkSource().getChunkDebugData(chunk));
                        unload(); return;
                    }
                    // Snapshot after retirement: ticking while tickets expire is gameplay, not serialization loss.
                    for (var entry : old.entrySet()) expected.put(entry.getKey(), entry.getValue().saveWithoutMetadata(level.registryAccess()));
                    // Flush unload writes too: a reload must not merely consume IOWorker's pending NBT cache.
                    level.getChunkSource().save(true);
                    level.getChunkSource().addRegionTicket(TICKET, chunk, 0, base); polls = 0; reload();
                } catch (RuntimeException | Error failure) { release(); throw failure; }
            });
        }
        void reload() {
            test.runAfterDelay(1, () -> {
                try {
                    var loaded = level.getChunkSource().getChunkNow(chunk.x, chunk.z);
                    if (loaded == null) { test.assertTrue(++polls < 200, "ticket loads saved chunk"); reload(); return; }
                    for (var entry : old.entrySet()) {
                        var next = loaded.getBlockEntity(entry.getKey());
                        test.assertTrue(next != null && next != entry.getValue(), "disk reload creates fresh BE at " + entry.getKey());
                        var actual = next.saveWithoutMetadata(level.registryAccess());
                        // Heat/cook timers can tick again between ticket arrival and this observation.
                        if (next instanceof TileJar jar) {
                            test.assertTrue(jar.getAmount() == 37 && jar.getAspect() == Aspect.WATER && jar.getFilter() == Aspect.WATER
                                    && jar.getFacing() == Direction.WEST && expected.get(entry.getKey()).equals(actual), "exact jar contents/filter/facing survive disk");
                        } else if (next instanceof TileResearchTable table) {
                            var note = ItemResearchNotes.data(table.getItem(1));
                            test.assertTrue(note != null && note.key().equals("INFUSION") && note.copies() == 2 && note.color() == 0x123456
                                    && table.bonusFor("aer") == 7 && table.bonusFor("aqua") == 3 && expected.get(entry.getKey()).equals(actual),
                                    "research note component and exact table bonus pools survive disk");
                        } else if (next instanceof TileCrucible crucible) test.assertTrue(crucible.getWaterAmount() == 725, "crucible fluid survives disk");
                        else if (next instanceof TileArcaneFurnace furnace) {
                            if (((TileArcaneFurnace)entry.getValue()).isDataRejected()) {
                                // Quarantine retains the full native load tag, including chunk-added envelope metadata.
                                var payload = actual.copy();
                                for (String key : List.of("id", "x", "y", "z", "keepPacked")) payload.remove(key);
                                test.assertTrue(furnace.isDataRejected() && expected.get(entry.getKey()).equals(payload), "unknown registry item retains exact raw inventory through disk reload; gameplay disabled; expected=" + expected.get(entry.getKey()) + ", actual=" + actual);
                            } else test.assertTrue(furnace.getStackInSlot(7).getCount() == 23 && furnace.getStackInSlot(7).is(Items.COBBLESTONE)
                                    && furnace.getStackInSlot(7).getHoverName().getString().equals("disk-cycle-inventory")
                                    && ItemStack.matches(furnace.getStackInSlot(7), ((TileArcaneFurnace)entry.getValue()).getStackInSlot(7)), "full component-bearing furnace inventory survives disk");
                        } else if (next instanceof TileInfusionMatrix matrix) {
                            // Explicit paused-session fixture avoids progress while tickets expire.
                            test.assertTrue(matrix.isCrafting() && matrix.getDemand().getAmount(Aspect.WATER) == 11
                                    && matrix.getRecipeCost().getAmount(Aspect.AIR) == 9, "matrix selected debt/cost survives disk");
                            var selectedBefore = expected.get(entry.getKey()).copy(); selectedBefore.remove("active");
                            var selectedAfter = actual.copy(); selectedAfter.remove("active");
                            test.assertTrue(selectedBefore.equals(selectedAfter), "all selected matrix session fields except structure-dependent activation survive disk");
                        } else test.assertTrue(expected.get(entry.getKey()).equals(actual), "exact persistent fields survive for " + next.getClass().getSimpleName() + ": expected=" + expected.get(entry.getKey()) + ", actual=" + actual);
                    }
                    release(); test.succeed();
                } catch (RuntimeException | Error failure) { release(); throw failure; }
            });
        }
        void release() {
            level.setChunkForced(chunk.x, chunk.z, false);
            level.getChunkSource().removeRegionTicket(TICKET, chunk, 0, base);
        }
    }

    public static void player(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var knowledge = KnowledgeAccess.get(f.player);
            knowledge.setAspectPool("aer", 73); knowledge.addResearch("INFUSION"); knowledge.scanItem("minecraft:stone");
            knowledge.setWarpPerm(9); knowledge.setWarpSticky(8); knowledge.setWarpTemp(7); knowledge.setWarpCounter(123);
            // Pick a real registered bauble of each slot's type; no invented item registry entry.
            for (int slot = 0; slot < 4; slot++) {
                final int index = slot;
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().filter(value ->
                        thaumcraft.common.lib.baubles.PlayerAccessoryInventory.matchesType(index, new ItemStack(value))).findFirst().orElseThrow();
                var accessory = new ItemStack(item); accessory.set(DataComponents.CUSTOM_NAME, Component.literal("disk-slot-" + slot));
                PlayerAccessories.get(f.player).setItem(slot, accessory);
            }
            var before = knowledge.saveAttachment().copy();
            var accessories = PlayerAccessories.get(f.player).saveAttachment(f.level.registryAccess()).copy();
            GameProfile profile = f.player.getGameProfile();
            // Real PlayerList.remove writes playerdata/<uuid>.dat, then placeNewPlayer loads it.
            f.level.getServer().getPlayerList().remove(f.player);
            var next = new ServerPlayer(f.level.getServer(), f.level, profile, ClientInformation.createDefault());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            var channel = new EmbeddedChannel(connection);
            try {
                try {
                    f.level.getServer().getPlayerList().placeNewPlayer(connection, next, CommonListenerCookie.createInitial(profile, false));
                    test.assertTrue(next != f.player && KnowledgeAccess.get(next) != knowledge, "relogin owns fresh player and attachment");
                    test.assertTrue(before.equals(KnowledgeAccess.get(next).saveAttachment()), "all knowledge/research/scans/warp survives real playerdata reload");
                    test.assertTrue(accessories.equals(PlayerAccessories.get(next).saveAttachment(next.registryAccess())), "four accessory slots and item components survive real relogin");
                } finally { f.level.getServer().getPlayerList().remove(next); next.discard(); }
            } finally { channel.finishAndReleaseAll(); }
        }
        test.succeed();
    }

    public static void savedData(GameTestHelper test) {
        var level = test.getLevel();
        var maze = OuterLandsProvider.maze(level);
        var generator = new thaumcraft.common.lib.world.MazeGenerator(5, 5, 173L);
        generator.grid[2][2] = 512; // Explicit nonempty marker input, not a labyrinth generation acceptance.
        maze.storeLabyrinth(generator, 123000, -123000);
        var before = maze.save(new CompoundTag(), level.registryAccess());
        level.getDataStorage().save();
        // A separate EMPTY cache opens the actual world's data directory, not a copy of the live map.
        var directory = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data").toFile();
        var fresh = new DimensionDataStorage(directory, level.getServer().getFixerUpper(), level.registryAccess());
        var factory = new SavedData.Factory<>(OuterLandsProvider.MazeSavedData::new, OuterLandsProvider.MazeSavedData::load, net.minecraft.util.datafix.DataFixTypes.LEVEL);
        var loaded = fresh.get(factory, OuterLandsProvider.MazeSavedData.ID);
        test.assertTrue(loaded != null && loaded != maze, "fresh storage cache reads thaumcraft_maze.dat from disk");
        test.assertTrue(loaded.cell(thaumcraft.common.lib.world.StructurePlacementRules.mazeOriginChunk(123000, 5) + 2,
                thaumcraft.common.lib.world.StructurePlacementRules.mazeOriginChunk(-123000, 5) + 2) == 512, "explicit nonempty labyrinth marker survives disk");
        test.assertTrue(before.equals(loaded.save(new CompoundTag(), level.registryAccess())), "whole labyrinth map survives native SavedData disk save/load");
        test.succeed();
    }
}
