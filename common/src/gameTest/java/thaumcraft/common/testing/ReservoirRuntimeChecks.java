package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.common.blocks.BlockEssentiaReservoir;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.essentia.ReservoirRules;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.tiles.TileEssentiaReservoir;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileTubeBuffer;

/**
 * The Essentia Reservoir on real content: its block and recipe, the container and transport contract of a mixed 256-unit store with
 * one port (capacity over all aspects, partial insertion, per-aspect removal, the sided and unsided type queries), persistence
 * (including a total above the capacity and malformed data), real pulls from a real buffer and through a real tube (and the cases
 * where nothing may move), the infusion's wireless drain, placement and the wand's turning by a real connected player, and the burst
 * when a stocked one is broken. NOT the tile renderer, the creak and colour on a client, or the golems' use of reservoirs.
 */
public final class ReservoirRuntimeChecks {
    private static final Aspect AQUA = Aspect.getAspect("aqua"), IGNIS = Aspect.getAspect("ignis"), TERRA = Aspect.getAspect("terra");

    private ReservoirRuntimeChecks() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static TileEssentiaReservoir place(ServerLevel level, BlockPos pos, Direction facing) {
        level.setBlock(pos, ThaumcraftTubes.ESSENTIA_RESERVOIR.get().defaultBlockState(), Block.UPDATE_ALL);
        var tile = (TileEssentiaReservoir) level.getBlockEntity(pos);
        if (!tile.setFacing(facing)) throw new AssertionError("a placed reservoir turns");
        return tile;
    }

    private static Map<Aspect, Integer> stock(TileEssentiaReservoir tile) {
        Map<Aspect, Integer> map = new LinkedHashMap<>();
        AspectList list = tile.getAspects();
        for (Aspect aspect : list.getAspects()) map.put(aspect, list.getAmount(aspect));
        return map;
    }

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        numbers(test, level);
        recipe(test, level);
        // Far above the other tests' rooms, as the Portable Hole test does; fixtures are spread so no blast reaches another.
        // The band is its own: the other high scenarios (Portable Hole, Deconstructor, Thaumatorium) use the
        // hundred band, and the vanilla GameTest grid puts the scenarios about twenty five blocks apart, so a
        // fixture of this scenario would land on the room of a neighbour there.
        BlockPos origin = test.absolutePos(new BlockPos(3, 140, 3));
        for (int x = (origin.getX() - 16) >> 4; x <= ((origin.getX() + 120) >> 4); x++) {
            for (int z = (origin.getZ() - 16) >> 4; z <= ((origin.getZ() + 100) >> 4); z++) level.getChunk(x, z);
        }
        List<Mob> spawned = new ArrayList<>();
        // Entities (pigs, drops, the player) only exist where the chunk is entity-ticking; force the columns the fixtures use (and only
        // those nobody else forced, so that the unforcing below never takes another test's).
        Set<ChunkPos> forced = new LinkedHashSet<>();
        for (BlockPos spot : List.of(origin.offset(30, 0, 0), origin.offset(44, 0, 0), origin.offset(58, 0, 0), origin.offset(72, 0, 0),
                origin.offset(0, 0, 48), origin.offset(0, 0, 56), origin.offset(0, 0, 64), origin.offset(0, 0, 72), origin.offset(0, 0, 80),
                origin.offset(0, 0, 90), origin.offset(10, 0, 90))) {
            for (int x = (spot.getX() - 3) >> 4; x <= ((spot.getX() + 3) >> 4); x++) for (int z = (spot.getZ() - 3) >> 4; z <= ((spot.getZ() + 3) >> 4); z++) {
                ChunkPos chunk = new ChunkPos(x, z);
                if (!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(x, z, true); forced.add(chunk); }
            }
        }
        try {
            container(test, level, origin);
            persistence(test, level, origin.offset(0, 0, 12));
            infusion(test, level, origin.offset(0, 0, 24));
        } catch (RuntimeException | Error failure) {
            unforce(level, forced);
            throw failure;
        }
        // A forced chunk becomes entity-ticking a tick later: the pigs and the player come after that.
        test.runAfterDelay(5, () -> {
            try {
                burst(test, level, origin.offset(30, 0, 0), spawned);
                pulls(test, level, origin, spawned, forced);
            } catch (RuntimeException | Error failure) {
                spawned.forEach(Mob::discard);
                unforce(level, forced);
                throw failure;
            }
        });
    }

    private static void unforce(ServerLevel level, Set<ChunkPos> forced) {
        for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
        forced.clear();
    }

    // ---- numbers ---------------------------------------------------------------------------------------------------------

    private static void numbers(GameTestHelper test, ServerLevel level) {
        var block = ThaumcraftTubes.ESSENTIA_RESERVOIR.get();
        BlockState state = block.defaultBlockState();
        BlockPos pos = test.absolutePos(new BlockPos(1, 1, 1));
        test.assertTrue(BuiltInRegistries.BLOCK.getKey(block).equals(id("essentia_reservoir")) && block instanceof BlockEssentiaReservoir, "the block is registered");
        test.assertTrue(state.getDestroySpeed(level, pos) == 2.0F && Math.abs(block.getExplosionResistance() - 10.2F) < 1e-4F && state.getSoundType() == SoundType.METAL
                        && state.requiresCorrectToolForDrops() && state.is(BlockTags.MINEABLE_WITH_PICKAXE),
                "hardness 2, resistance 17 (3 x 17 / 5 = 10.2), metal, a pickaxe's: " + state.getDestroySpeed(level, pos) + " " + block.getExplosionResistance());
        test.assertTrue(!state.canOcclude() && !state.isRedstoneConductor(level, pos) && !state.isSuffocating(level, pos) && state.getRenderShape() == RenderShape.MODEL
                        && state.hasAnalogOutputSignal(), "not an opaque or normal cube, drawn as a model for now, and a comparator reads it");
        test.assertTrue(BuiltInRegistries.ITEM.get(id("essentia_reservoir")) instanceof BlockItem blockItem && blockItem.getBlock() == block, "the item places the block");
        test.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id("essentia_reservoir")) == ThaumcraftTubes.RESERVOIR_BLOCK_ENTITY.get()
                && ThaumcraftTubes.RESERVOIR_BLOCK_ENTITY.get().isValid(state), "the tile type is registered for the block");
        test.assertTrue(TileEssentiaReservoir.MAX_AMOUNT == 256 && ReservoirRules.SUCTION == 24 && ReservoirRules.PULL_INTERVAL == 5, "256 units, suction 24, a pull every fifth tick");
    }

    private static void recipe(GameTestHelper test, ServerLevel level) {
        var recipe = (InfusionStackRecipe) level.getRecipeManager().byKey(id("infusion/essentia_reservoir")).orElseThrow(() -> new AssertionError("no infusion")).value();
        test.assertTrue(recipe.research().equals("ESSENTIARESERVOIR") && recipe.instability() == 6 && recipe.result().is(item("thaumcraft:essentia_reservoir"))
                        && recipe.aspects().equals(Map.of("aqua", 8, "vacuos", 8, "praecantatio", 8, "permutatio", 8)), "ESSENTIARESERVOIR, instability 6: " + recipe.aspects());
        ItemStack ingot = new ItemStack(item("thaumcraft:void_ingot")), jar = new ItemStack(item("thaumcraft:jar")), buffer = new ItemStack(item("thaumcraft:essentia_buffer"));
        List<ItemStack> parts = List.of(ingot, jar, jar, ingot, jar, jar);
        test.assertTrue(recipe.matches(new InfusionRecipeInput(buffer, parts), level), "an essentia buffer, two void ingots and four jars complete it");
        test.assertTrue(!recipe.matches(new InfusionRecipeInput(new ItemStack(item("thaumcraft:essentia_tube")), parts), level), "a plain tube is not the centre");
        List<ItemStack> thaumium = new ArrayList<>(parts);
        thaumium.set(0, new ItemStack(item("thaumcraft:thaumium_ingot")));
        test.assertTrue(!recipe.matches(new InfusionRecipeInput(buffer, thaumium), level), "thaumium is not void metal");
        List<ItemStack> five = new ArrayList<>(parts);
        five.remove(5);
        test.assertTrue(!recipe.matches(new InfusionRecipeInput(buffer, five), level), "a missing jar is refused");
    }

    // ---- the container and the transport contract --------------------------------------------------------------------

    private static void container(GameTestHelper test, ServerLevel level, BlockPos pos) {
        TileEssentiaReservoir tile = place(level, pos, Direction.DOWN);
        IAspectSource source = tile;
        test.assertTrue(tile.getFacing() == Direction.DOWN && tile.getAmount() == 0 && tile.comparatorSignal() == 0, "a new reservoir opens downward and is empty");
        for (Direction face : Direction.values()) {
            boolean open = face == Direction.DOWN;
            test.assertTrue(tile.isConnectable(face) == open && tile.canInputFrom(face) == open && tile.canOutputTo(face) == open, "only the facing side is a port: " + face);
            test.assertTrue(tile.getSuctionAmount(face) == 24 && tile.getSuctionType(face) == null && tile.getMinimumSuction() == 24 && tile.getEssentiaAmount(face) == 0, "suction 24 from any query: " + face);
        }
        test.assertTrue(tile.getSuctionAmount(null) == 24 && tile.getEssentiaType(null) == null && !tile.renderExtendedTube(), "the unsided query is empty too");
        test.assertTrue(source.addToContainer(AQUA, 10) == 0 && source.addToContainer(IGNIS, 5) == 0, "both aspects fit");
        test.assertTrue(stock(tile).equals(Map.of(AQUA, 10, IGNIS, 5)) && tile.getAmount() == 15 && source.containerContains(AQUA) == 10 && source.containerContains(IGNIS) == 5 && source.containerContains(TERRA) == 0,
                "one mixed store, kept per aspect: " + stock(tile));
        test.assertTrue(tile.getEssentiaType(null) == AQUA && tile.getEssentiaType(Direction.DOWN) == null && tile.getEssentiaType(Direction.UP) == null && tile.getEssentiaAmount(Direction.DOWN) == 15,
                "only the unsided query names a type (the first stored), a sided one is answered with nothing; the amount is the total");
        test.assertTrue(tile.comparatorSignal() == ReservoirRules.comparator(15) && tile.comparatorSignal() == 1 && level.getBlockState(pos).getAnalogOutputSignal(level, pos) == 1, "the comparator reads 1");
        AspectList copy = source.getAspects();
        copy.add(TERRA, 99);
        source.setAspects(new AspectList().add(TERRA, 1));
        test.assertTrue(stock(tile).equals(Map.of(AQUA, 10, IGNIS, 5)), "neither the list read nor a list given back writes into the store");
        test.assertTrue(source.addToContainer(AQUA, 0) == 0 && source.addToContainer(AQUA, -3) == -3 && source.addToContainer(null, 4) == 4 && tile.getAmount() == 15, "nothing, a negative or an unknown aspect adds nothing");
        // Capacity over all aspects: 15 + 241 = 256, the rest of 300 is returned.
        test.assertTrue(source.addToContainer(TERRA, 300) == 59 && tile.getAmount() == 256 && source.containerContains(TERRA) == 241, "an insert takes what fits and returns the rest: " + stock(tile));
        test.assertTrue(tile.getSuctionAmount(Direction.DOWN) == 0 && tile.getSuctionAmount(null) == 0 && tile.comparatorSignal() == 15 && source.addToContainer(AQUA, 1) == 1 && tile.getAmount() == 256,
                "full: no suction, comparator 15, nothing more is taken");
        test.assertTrue(!source.takeFromContainer(TERRA, 242) && tile.getAmount() == 256 && !source.takeFromContainer(TERRA, -1) && tile.getAmount() == 256, "a take is all or nothing and never negative");
        test.assertTrue(source.takeFromContainer(TERRA, 41) && tile.getAmount() == 215 && tile.getSuctionAmount(Direction.DOWN) == 24, "taking makes room: suction is back");
        test.assertTrue(source.takeFromContainer(AQUA, 0) && tile.getAmount() == 215, "a zero take succeeds without moving anything");
        test.assertTrue(source.takeFromContainer(AQUA, 10) && source.containerContains(AQUA) == 0 && !stock(tile).containsKey(AQUA) && tile.getEssentiaType(null) == IGNIS,
                "an aspect emptied is gone from the list: the first stored is now ignis: " + stock(tile));
        test.assertTrue(!source.takeFromContainer(new AspectList().add(IGNIS, 1)) && tile.getAmount() == 205, "the list take is not supported");
        test.assertTrue(source.doesContainerContainAmount(IGNIS, 5) && !source.doesContainerContainAmount(IGNIS, 6) && source.doesContainerContainAmount(TERRA, 200) && !source.doesContainerContainAmount(AQUA, 1),
                "amount queries");
        test.assertTrue(source.doesContainerContain(new AspectList().add(IGNIS, 5).add(TERRA, 200)) && !source.doesContainerContain(new AspectList().add(IGNIS, 5).add(TERRA, 201)) && !source.doesContainerContain(new AspectList().add(AQUA, 1)),
                "a list is contained only when every amount is");
        test.assertTrue(source.doesContainerAccept(AQUA) && source.doesContainerAccept(Aspect.getAspect("alienis")) && !source.doesContainerAccept(null), "it accepts every real aspect");
        // The port's own transport calls.
        test.assertTrue(tile.takeEssentia(IGNIS, 3, Direction.DOWN) == 3 && source.containerContains(IGNIS) == 2 && tile.takeEssentia(IGNIS, 1, Direction.UP) == 0 && source.containerContains(IGNIS) == 2
                        && tile.takeEssentia(IGNIS, 9, Direction.DOWN) == 0 && source.containerContains(IGNIS) == 2, "takeEssentia works only through the port and only for what is there");
        test.assertTrue(tile.addEssentia(AQUA, 4, Direction.DOWN) == 4 && tile.addEssentia(AQUA, 4, Direction.NORTH) == 0 && source.containerContains(AQUA) == 4, "addEssentia works only through the port");
        int space = tile.getSpace();
        test.assertTrue(tile.addEssentia(AQUA, space + 20, Direction.DOWN) == space && tile.getAmount() == 256, "addEssentia reports what was taken: " + space);
        source.takeFromContainer(AQUA, source.containerContains(AQUA));
        source.takeFromContainer(IGNIS, source.containerContains(IGNIS));
        source.takeFromContainer(TERRA, source.containerContains(TERRA));
        test.assertTrue(tile.getAmount() == 0 && tile.getAspects().size() == 0, "emptied again");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    // ---- persistence -------------------------------------------------------------------------------------------------------

    private static void persistence(GameTestHelper test, ServerLevel level, BlockPos pos) {
        var registries = level.registryAccess();
        TileEssentiaReservoir tile = place(level, pos, Direction.EAST);
        tile.addToContainer(AQUA, 7);
        tile.addToContainer(IGNIS, 3);
        tile.addToContainer(TERRA, 120);
        CompoundTag saved = tile.saveWithoutMetadata(registries);
        test.assertTrue(saved.getByte("face") == Direction.EAST.get3DDataValue() && saved.getList("Aspects", Tag.TAG_COMPOUND).size() == 3, "saved as a list of aspects and the face: " + saved);
        TileEssentiaReservoir twin = new TileEssentiaReservoir(pos.above(), tile.getBlockState());
        twin.loadWithComponents(saved, registries);
        test.assertTrue(stock(twin).equals(stock(tile)) && new ArrayList<>(stock(twin).keySet()).equals(List.of(AQUA, IGNIS, TERRA)) && twin.getFacing() == Direction.EAST,
                "the stock, its order and the facing come back: " + stock(twin));
        test.assertTrue(tile.getUpdateTag(registries).getList("Aspects", Tag.TAG_COMPOUND).size() == 3 && tile.getUpdateTag(registries).getByte("face") == Direction.EAST.get3DDataValue(), "the client is sent the same stock");
        // More than the capacity is an empty list (the original's readCustomNBT), the face is kept.
        CompoundTag over = saved.copy();
        ListTag rows = new ListTag();
        for (Aspect aspect : List.of(AQUA, IGNIS)) { var row = new CompoundTag(); row.putString("key", aspect.getTag()); row.putInt("amount", 150); rows.add(row); }
        over.put("Aspects", rows);
        TileEssentiaReservoir overflowing = new TileEssentiaReservoir(pos.above(2), tile.getBlockState());
        overflowing.loadWithComponents(over, registries);
        test.assertTrue(overflowing.getAmount() == 0 && !overflowing.isQuarantined() && overflowing.getFacing() == Direction.EAST, "300 units do not fit 256: the list is dropped, the reservoir stays usable");
        CompoundTag exact = saved.copy();
        ListTag full = new ListTag();
        var fullRow = new CompoundTag();
        fullRow.putString("key", IGNIS.getTag());
        fullRow.putInt("amount", 256);
        full.add(fullRow);
        exact.put("Aspects", full);
        TileEssentiaReservoir exactly = new TileEssentiaReservoir(pos.above(3), tile.getBlockState());
        exactly.loadWithComponents(exact, registries);
        test.assertTrue(exactly.getAmount() == 256, "exactly 256 is kept");
        CompoundTag nofacing = saved.copy();
        nofacing.remove("face");
        TileEssentiaReservoir downward = new TileEssentiaReservoir(pos.above(4), tile.getBlockState());
        downward.loadWithComponents(nofacing, registries);
        test.assertTrue(downward.getFacing() == Direction.DOWN && downward.getAmount() == 10 + 120, "a missing face is down, as the original's missing byte was");
        // Malformed data closes the reservoir and is kept verbatim.
        record Bad(String name, java.util.function.Consumer<CompoundTag> damage) {}
        for (Bad bad : List.of(
                new Bad("an unknown aspect", t -> { var list = new ListTag(); var row = new CompoundTag(); row.putString("key", "nothing"); row.putInt("amount", 3); list.add(row); t.put("Aspects", list); }),
                new Bad("a zero amount", t -> { var list = new ListTag(); var row = new CompoundTag(); row.putString("key", AQUA.getTag()); row.putInt("amount", 0); list.add(row); t.put("Aspects", list); }),
                new Bad("a face of 9", t -> t.putByte("face", (byte) 9)),
                new Bad("a list of strings", t -> { var list = new ListTag(); list.add(net.minecraft.nbt.StringTag.valueOf("x")); t.put("Aspects", list); }))) {
            CompoundTag damaged = saved.copy();
            bad.damage().accept(damaged);
            TileEssentiaReservoir closed = new TileEssentiaReservoir(pos.above(5), tile.getBlockState());
            closed.loadWithComponents(damaged, registries);
            test.assertTrue(closed.isQuarantined() && closed.getAmount() == 0 && !closed.isConnectable(Direction.DOWN) && !closed.canOutputTo(Direction.DOWN) && closed.getSuctionAmount(Direction.DOWN) == 0,
                    bad.name() + ": the reservoir is closed");
            CompoundTag again = closed.saveWithoutMetadata(registries);
            test.assertTrue(again.contains("RejectedReservoir") && !again.contains("Aspects"), bad.name() + ": the payload is kept: " + again);
            TileEssentiaReservoir restored = new TileEssentiaReservoir(pos.above(6), tile.getBlockState());
            restored.loadWithComponents(again, registries);
            test.assertTrue(restored.isQuarantined() && restored.saveWithoutMetadata(registries).equals(again), bad.name() + ": and survives another save");
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    // ---- the infusion's wireless drain -------------------------------------------------------------------------------------

    private static void infusion(GameTestHelper test, ServerLevel level, BlockPos pos) {
        var registries = level.registryAccess();
        BlockPos matrixPos = pos, reservoirPos = pos.offset(5, 3, -4);
        level.setBlock(matrixPos, thaumcraft.common.config.ThaumcraftInfusion.MATRIX.get().defaultBlockState(), Block.UPDATE_ALL);
        TileInfusionMatrix matrix = (TileInfusionMatrix) level.getBlockEntity(matrixPos);
        TileEssentiaReservoir tile = place(level, reservoirPos, Direction.UP);
        tile.addToContainer(AQUA, 2);
        tile.addToContainer(IGNIS, 5);
        EssentiaHandler.clear(level);
        CompoundTag demand = matrix.saveWithFullMetadata(registries);
        demand.putBoolean("active", true);
        demand.putBoolean("crafting", true);
        demand.putInt("recipetype", 0);
        // This endpoint fixture represents a supported saved stack ritual, not an empty-output foreign session.
        var recipe = (InfusionStackRecipe) level.getRecipeManager().byKey(id("infusion/essentia_reservoir")).orElseThrow().value();
        ItemStack result = recipe.getResultItem(registries);
        CompoundTag output = new CompoundTag();
        output.put("Stack", result.copyWithCount(1).save(registries));
        output.putInt("StoredCount", result.getCount());
        demand.putString("rotype", "@");
        demand.put("recipeout", output);
        ListTag rows = new ListTag();
        var row = new CompoundTag();
        row.putString("key", AQUA.getTag());
        row.putInt("amount", 3);
        rows.add(row);
        demand.put("Aspects", rows);
        matrix.loadWithComponents(demand, registries);
        test.assertTrue(matrix.getPendingEssentia(AQUA) == 3, "the matrix wants 3 aqua: " + matrix.getPendingEssentia(AQUA));
        test.assertTrue(EssentiaHandler.drainEssentia(matrix, AQUA) && tile.containerContains(AQUA) == 1 && tile.containerContains(IGNIS) == 5 && tile.getAmount() == 6 && matrix.getPendingEssentia(AQUA) == 2,
                "one unit of the wanted aspect leaves the reservoir, the others stay: " + stock(tile));
        test.assertTrue(EssentiaHandler.drainEssentia(matrix, AQUA) && tile.containerContains(AQUA) == 0 && !stock(tile).containsKey(AQUA) && tile.getAmount() == 5 && matrix.getPendingEssentia(AQUA) == 1,
                "the second one empties the aspect out of the list: " + stock(tile));
        test.assertTrue(!EssentiaHandler.drainEssentia(matrix, AQUA) && tile.getAmount() == 5 && matrix.getPendingEssentia(AQUA) == 1, "nothing is taken of an aspect the reservoir does not hold");
        EssentiaHandler.clear(level);
        level.setBlock(reservoirPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        CompoundTag stop = matrix.saveWithFullMetadata(registries);
        stop.putBoolean("crafting", false);
        stop.putBoolean("active", false);
        matrix.loadWithComponents(stop, registries);
        level.setBlock(matrixPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    // ---- the burst ---------------------------------------------------------------------------------------------------------

    private static int fluxBlocks(ServerLevel level, BlockPos pos, List<BlockPos> goo, List<BlockPos> gas) {
        int found = 0;
        for (int x = -5; x <= 5; x++) for (int y = -5; y <= 5; y++) for (int z = -5; z <= 5; z++) {
            BlockPos at = pos.offset(x, y, z);
            BlockState state = level.getBlockState(at);
            if (!(state.getBlock() instanceof FluxFiniteBlock)) continue;
            found++;
            test(state.getValue(FluxFiniteBlock.LEVEL) == ReservoirRules.FLUX_LEVEL, "flux level 8 at " + at);
            if (state.is(thaumcraft.common.config.ThaumcraftFlux.FLUX_GOO.get())) goo.add(at); else gas.add(at);
        }
        return found;
    }

    private static void test(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static void burst(GameTestHelper test, ServerLevel level, BlockPos start, List<Mob> spawned) {
        record Case(int units, int sprays) {}
        int index = 0;
        for (Case spec : List.of(new Case(15, 0), new Case(16, 1), new Case(48, 3), new Case(256, 16))) {
            BlockPos pos = start.offset(index++ * 14, 0, 0);
            TileEssentiaReservoir tile = place(level, pos, Direction.DOWN);
            tile.addToContainer(AQUA, spec.units() / 2);
            tile.addToContainer(TERRA, spec.units() - spec.units() / 2);
            var pig = EntityType.PIG.create(level);
            pig.setNoAi(true);
            pig.setNoGravity(true);
            pig.setPersistenceRequired();
            pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);
            pig.setHealth(500F);
            pig.setPos(Vec3.atCenterOf(pos.east()));
            level.addFreshEntity(pig);
            spawned.add(pig);
            boolean broken = level.destroyBlock(pos, false);
            test.assertTrue(broken && level.getBlockState(pos).isAir(), "the reservoir of " + spec.units() + " units is broken: " + broken + " " + level.getBlockState(pos) + " " + level.hasChunkAt(pos) + " " + level.isPositionEntityTicking(pos));
            List<BlockPos> goo = new ArrayList<>(), gas = new ArrayList<>();
            int flux = fluxBlocks(level, pos, goo, gas);
            if (spec.sprays() == 0) {
                test.assertTrue(flux == 0 && pig.getHealth() == 500F, spec.units() + " units: under 16 nothing bursts: " + flux + " flux, health " + pig.getHealth());
            } else {
                test.assertTrue(flux == spec.sprays() + 1, spec.units() + " units: one more flux block than sixteenths of units: " + flux + " vs " + (spec.sprays() + 1));
                test.assertTrue(goo.stream().allMatch(at -> at.getY() < pos.getY()) && gas.stream().allMatch(at -> at.getY() >= pos.getY()), "goo below the reservoir's level, gas at or above it");
                test.assertTrue(pig.getHealth() < 500F - 1F, spec.units() + " units: the explosion hurts what stands next to it: " + pig.getHealth());
            }
            for (BlockPos at : goo) level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            for (BlockPos at : gas) level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            pig.discard();
        }
    }

    // ---- pulls, placement and the wand -----------------------------------------------------------------------------------

    private static Map<Aspect, Integer> bufferStock(TileTubeBuffer buffer) {
        Map<Aspect, Integer> map = new LinkedHashMap<>();
        AspectList list = buffer.getAspects();
        for (Aspect aspect : list.getAspects()) map.put(aspect, list.getAmount(aspect));
        return map;
    }

    private static Map<Aspect, Integer> sum(Map<Aspect, Integer> first, Map<Aspect, Integer> second) {
        Map<Aspect, Integer> total = new LinkedHashMap<>(first);
        second.forEach((aspect, amount) -> total.merge(aspect, amount, Integer::sum));
        total.values().removeIf(amount -> amount == 0);
        return total;
    }

    private static TileTubeBuffer buffer(ServerLevel level, BlockPos pos, Aspect... units) {
        level.setBlock(pos, ThaumcraftTubes.ESSENTIA_BUFFER.get().defaultBlockState(), Block.UPDATE_ALL);
        var buffer = (TileTubeBuffer) level.getBlockEntity(pos);
        for (Aspect aspect : units) if (buffer.addToContainer(aspect, 1) != 0) throw new AssertionError("a buffer takes eight units");
        return buffer;
    }

    private static void pulls(GameTestHelper test, ServerLevel level, BlockPos origin, List<Mob> spawned, Set<ChunkPos> forced) {
        Aspect[] units = {AQUA, AQUA, AQUA, IGNIS, IGNIS, TERRA, TERRA};
        Map<Aspect, Integer> loaded = new LinkedHashMap<>();
        for (Aspect aspect : units) loaded.merge(aspect, 1, Integer::sum);
        // Row 1: a reservoir opening east draws from the buffer next to it, a unit every fifth tick.
        BlockPos direct = origin.offset(0, 0, 48);
        TileEssentiaReservoir directReservoir = place(level, direct, Direction.EAST);
        TileTubeBuffer directBuffer = buffer(level, direct.east(), units);
        // Row 2: nothing may move: the opening faces away, the buffer's side is closed, the reservoir is full.
        BlockPos away = origin.offset(0, 0, 56), closedSide = origin.offset(0, 0, 64), fullPos = origin.offset(0, 0, 72);
        TileEssentiaReservoir awayReservoir = place(level, away, Direction.UP);
        TileTubeBuffer awayBuffer = buffer(level, away.east(), units);
        TileEssentiaReservoir closedReservoir = place(level, closedSide, Direction.EAST);
        TileTubeBuffer closedBuffer = buffer(level, closedSide.east(), units);
        closedBuffer.setSideOpen(Direction.WEST, false);
        TileEssentiaReservoir fullReservoir = place(level, fullPos, Direction.EAST);
        fullReservoir.addToContainer(AQUA, 256);
        TileTubeBuffer fullBuffer = buffer(level, fullPos.east(), units);
        // Row 3: through a real tube: buffer, tube, reservoir.
        BlockPos chain = origin.offset(0, 0, 80);
        TileEssentiaReservoir chainReservoir = place(level, chain, Direction.EAST);
        level.setBlock(chain.east(), ThaumcraftTubes.ESSENTIA_TUBE.get().defaultBlockState(), Block.UPDATE_ALL);
        TileTubeBuffer chainBuffer = buffer(level, chain.east(2), AQUA, AQUA, IGNIS, TERRA);
        Map<Aspect, Integer> chainLoaded = Map.of(AQUA, 2, IGNIS, 1, TERRA, 1);
        test.assertTrue(directReservoir.getAmount() == 0 && directBuffer.getAmount() == 7 && awayBuffer.getAmount() == 7 && closedBuffer.getAmount() == 7 && fullBuffer.getAmount() == 7 && chainBuffer.getAmount() == 4,
                "the fixtures are stocked");
        test.runAfterDelay(12, () -> {
            test.assertTrue(directReservoir.getAmount() >= 1 && directReservoir.getAmount() <= 3
                            && sum(stock(directReservoir), bufferStock(directBuffer)).equals(loaded), "after twelve ticks one to three units came over and none was lost: " + stock(directReservoir) + " " + bufferStock(directBuffer));
            test.runAfterDelay(60, () -> {
                test.assertTrue(directReservoir.getAmount() == 7 && directBuffer.getAmount() == 0 && stock(directReservoir).equals(loaded),
                        "a unit every fifth tick moves the whole buffer, aspect by aspect: " + stock(directReservoir) + " " + bufferStock(directBuffer));
                test.assertTrue(awayReservoir.getAmount() == 0 && awayBuffer.getAmount() == 7, "a reservoir opening away from the buffer draws nothing");
                test.assertTrue(closedReservoir.getAmount() == 0 && closedBuffer.getAmount() == 7, "a buffer whose side is closed gives nothing");
                test.assertTrue(fullReservoir.getAmount() == 256 && fullReservoir.getSuctionAmount(Direction.EAST) == 0 && fullBuffer.getAmount() == 7, "a full reservoir draws nothing");
                // The pulls go on as long as something is there: open the side of the closed buffer, turn the away one around.
                closedBuffer.setSideOpen(Direction.WEST, true);
                test.assertTrue(awayReservoir.setFacing(Direction.EAST), "the wand turns it");
                test.runAfterDelay(60, () -> {
                    test.assertTrue(closedReservoir.getAmount() == 7 && closedBuffer.getAmount() == 0 && stock(closedReservoir).equals(loaded), "the side opened: everything came over: " + stock(closedReservoir));
                    test.assertTrue(awayReservoir.getAmount() == 7 && awayBuffer.getAmount() == 0 && stock(awayReservoir).equals(loaded), "turned toward it: everything came over: " + stock(awayReservoir));
                    test.runAfterDelay(120, () -> {
                        test.assertTrue(chainReservoir.getAmount() == 4 && chainBuffer.getAmount() == 0 && stock(chainReservoir).equals(chainLoaded),
                                "through a real tube the whole buffer reached the reservoir: " + stock(chainReservoir) + " " + bufferStock(chainBuffer));
                        player(test, level, origin.offset(0, 0, 90), spawned, forced);
                    });
                });
            });
        });
    }

    private static InteractionResult use(ServerPlayer player, BlockPos pos, Direction face) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(face, .5), face, pos, false);
        return player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    private static void player(GameTestHelper test, ServerLevel level, BlockPos pos, List<Mob> spawned, Set<ChunkPos> forced) {
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "res-" + UUID.randomUUID().toString().substring(0, 6)), ClientInformation.createDefault());
        level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        try {
            player.setNoGravity(true);
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(Vec3.atBottomCenterOf(pos.offset(0, 0, 3)));
            test.assertTrue(player.level() == level && player.connection != null, "a real connected player");
            // Placement: the opening faces what was clicked, whatever the side.
            BlockPos base = pos.offset(0, 0, 0);
            level.setBlock(base, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftTubes.ESSENTIA_RESERVOIR_ITEM.get(), 8));
            for (Direction face : List.of(Direction.UP, Direction.NORTH, Direction.WEST, Direction.EAST, Direction.SOUTH)) {
                BlockPos at = base.relative(face);
                level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                test.assertTrue(use(player, base, face).consumesAction() && level.getBlockEntity(at) instanceof TileEssentiaReservoir placed && placed.getFacing() == face.getOpposite() && placed.getAmount() == 0,
                        "placed on the " + face + " side of a block it opens toward it (" + face.getOpposite() + ")");
                level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            // The wand turns it: away from the clicked side, or toward it while sneaking.
            Map<String, Integer> vis = new LinkedHashMap<>();
            ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
            wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", "iron", vis));
            BlockPos target = pos.offset(0, 0, 1);
            TileEssentiaReservoir tile = place(level, target, Direction.DOWN);
            tile.addToContainer(AQUA, 9);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            player.setShiftKeyDown(false);
            for (Direction side : Direction.values()) {
                test.assertTrue(use(player, target, side).consumesAction() && tile.getFacing() == side.getOpposite() && tile.getAmount() == 9,
                        "a wand click on the " + side + " side opens it toward " + side.getOpposite() + ": " + tile.getFacing());
            }
            player.setShiftKeyDown(true);
            for (Direction side : Direction.values()) {
                test.assertTrue(use(player, target, side).consumesAction() && tile.getFacing() == side && tile.getAmount() == 9,
                        "sneaking, a click on the " + side + " side opens it toward " + side + ": " + tile.getFacing());
            }
            player.setShiftKeyDown(false);
            Direction before = tile.getFacing();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            use(player, target, Direction.NORTH);
            test.assertTrue(tile.getFacing() == before, "only a wand turns it");
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            // A closed (malformed) reservoir is not turned; a wand on other blocks does not touch it.
            CompoundTag broken = tile.saveWithoutMetadata(level.registryAccess());
            broken.putByte("face", (byte) 9);
            tile.loadWithComponents(broken, level.registryAccess());
            test.assertTrue(tile.isQuarantined(), "the fixture is closed");
            use(player, target, Direction.EAST);
            test.assertTrue(tile.getFacing() == Direction.DOWN, "a closed reservoir is not turned");
            level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(base, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            // Mined for real: the burst comes first and the block is dropped after it, so the item survives its own blast and holds no stock.
            BlockPos mined = pos.offset(10, 0, 0);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
            place(level, mined, Direction.DOWN).addToContainer(AQUA, 48);
            test.assertTrue(player.gameMode.destroyBlock(mined) && level.getBlockState(mined).isAir(), "a pickaxe mines the stocked reservoir");
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(mined).inflate(6)).stream().filter(entity -> entity.getItem().is(item("thaumcraft:essentia_reservoir"))).toList();
            List<BlockPos> goo = new ArrayList<>(), gas = new ArrayList<>();
            test.assertTrue(drops.size() == 1 && drops.get(0).getItem().getCount() == 1 && drops.get(0).getItem().getComponentsPatch().isEmpty(), "the empty block comes back, the stock does not: " + drops.size());
            test.assertTrue(fluxBlocks(level, mined, goo, gas) == 4, "mining bursts it too: one more flux block than sixteenths of units: " + (goo.size() + gas.size()));
            drops.forEach(ItemEntity::discard);
            goo.forEach(at -> level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
            gas.forEach(at -> level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
            // By hand nothing is dropped (a metal block needs a tool), but the burst is the same.
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            place(level, mined, Direction.DOWN).addToContainer(TERRA, 20);
            test.assertTrue(player.gameMode.destroyBlock(mined), "broken by hand");
            goo.clear();
            gas.clear();
            test.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(mined).inflate(6)).stream().noneMatch(entity -> entity.getItem().is(item("thaumcraft:essentia_reservoir"))) && fluxBlocks(level, mined, goo, gas) == 2,
                    "no item by hand, still two flux blocks for twenty units: " + (goo.size() + gas.size()));
            goo.forEach(at -> level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
            gas.forEach(at -> level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
        } finally {
            spawned.forEach(Mob::discard);
            unforce(level, forced);
            try {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
            } finally {
                player.discard();
                channel.finishAndReleaseAll();
            }
        }
        test.succeed();
    }
}
