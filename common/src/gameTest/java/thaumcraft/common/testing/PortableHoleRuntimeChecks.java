package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftHoles;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;
import thaumcraft.common.items.wands.foci.ItemFocusPortableHole;
import thaumcraft.common.items.wands.foci.PortableHoleRules;
import thaumcraft.common.items.wands.foci.WandFocusActions;
import thaumcraft.common.tiles.TileHole;

/**
 * The Portable Hole on real content and a real connected player: the focus's numbers and upgrades, the infusion, then a dozen walls shot
 * with a real wand: a tunnel of 108 holes (12 blocks, each 3 x 3 across) made one block a tick, paid exactly (10 entropy and 10 air per
 * block of depth), the Enlarge reach (33 and 41 blocks), the stops (bedrock, a chest, water, a piston, no vis, no target) and the return
 * of every block in its exact state after 120 ticks, 180 with Extend. NOT the opening's drawing, the particles or the sounds.
 */
public final class PortableHoleRuntimeChecks {
    private static final int HEIGHT_BELOW = 2, HEIGHT_ABOVE = 4, HALF_WIDTH = 2;

    private PortableHoleRuntimeChecks() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    /** One wall: stone from x0 east for {@code length} blocks, 7 high and 5 wide round the line the player aims along. */
    private record Region(BlockPos start, int length, Map<BlockPos, BlockState> before) {
        BlockPos cell(int along, int dy, int dz) { return start.offset(along, dy, dz); }
        List<BlockPos> positions() {
            List<BlockPos> all = new ArrayList<>();
            for (int x = 0; x < length; x++) for (int y = -HEIGHT_BELOW - 1; y <= HEIGHT_ABOVE - 1; y++) for (int z = -HALF_WIDTH; z <= HALF_WIDTH; z++) all.add(start.offset(x, y, z));
            return all;
        }
    }

    private static ItemStack wand(String cap, int each, ItemStack focus) {
        Map<String, Integer> amounts = new LinkedHashMap<>();
        for (String tag : WandData.PRIMALS) amounts.put(tag, each);
        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
        wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", cap, amounts));
        wand.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(focus));
        return wand;
    }

    private static ItemStack focus(FocusUpgradeType... upgrades) {
        ItemStack focus = new ItemStack(ThaumcraftFoci.PORTABLE_HOLE_FOCUS.get());
        for (int rank = 0; rank < upgrades.length; rank++) {
            focus.set(ThaumcraftFoci.FOCUS_UPGRADES.get(), FocusUpgradeData.read(focus).withUpgrade(upgrades[rank], rank + 1));
        }
        return focus;
    }

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        recipe(test, level);
        // Far above the other tests' rooms: their checks look for stray entities near them.
        BlockPos origin = test.absolutePos(new BlockPos(3, 100, 3));
        // The tunnels grow only in chunks that tick, and the tickets of the neighbouring tests are not a thing to lean on (they end when
        // those tests do): the columns are forced for the length of this test, as the golem tests do (and only the ones nobody forced).
        Set<ChunkPos> forced = new LinkedHashSet<>();
        for (int x = (origin.getX() - 16) >> 4; x <= ((origin.getX() + 60) >> 4); x++) {
            for (int z = (origin.getZ() - 16) >> 4; z <= ((origin.getZ() + 16) >> 4); z++) {
                level.getChunk(x, z);
                ChunkPos chunk = new ChunkPos(x, z);
                if (!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(x, z, true); forced.add(chunk); }
            }
        }
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "hole-" + UUID.randomUUID().toString().substring(0, 6)), ClientInformation.createDefault());
        level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.setNoGravity(true);
        player.setGameMode(GameType.SURVIVAL);
        List<Region> regions = new ArrayList<>();
        Runnable cleanup = () -> {
            for (Region region : regions) for (BlockPos pos : region.positions()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            forced.clear();
            try {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
            } finally {
                player.discard();
                channel.finishAndReleaseAll();
            }
        };
        try {
            test.assertTrue(player.level() == level && player.connection != null, "a real connected player holds the wand");
            numbers(test, player);
            shoot(test, level, player, origin, regions, cleanup);
        } catch (RuntimeException | Error failure) {
            cleanup.run();
            throw failure;
        }
    }

    // ---- numbers and the infusion -------------------------------------------------------------------------------------------

    private static void numbers(GameTestHelper test, ServerPlayer player) {
        ItemStack focus = focus();
        var item = (ItemFocusPortableHole) focus.getItem();
        test.assertTrue(item.getVisCost(focus).getAmount(thaumcraft.api.aspects.Aspect.ENTROPY) == 10 && item.getVisCost(focus).getAmount(thaumcraft.api.aspects.Aspect.AIR) == 10
                && item.getVisCost(focus).size() == 2 && !item.isVisCostPerTick(focus), "10 entropy and 10 air per block, once: " + item.getVisCost(focus).size());
        test.assertTrue(item.getFocusColor(focus) == 594985 && item.getSortingHelper(focus).equals("BPH-1-1-1-1-1"), "color 594985, sorting BPH and the five empty ranks: " + item.getSortingHelper(focus));
        test.assertTrue(item.getActivationCooldown(focus) == 0, "no cooldown");
        test.assertTrue(FocusData.isSupportedFocus(focus), "a supported focus");
        for (int rank = 1; rank <= 5; rank++) {
            test.assertTrue(java.util.Arrays.equals(item.getPossibleUpgradesByRank(focus, rank), new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.enlarge, FocusUpgradeType.extend}),
                    "rank " + rank + " offers Frugal, Enlarge and Extend");
        }
        test.assertTrue(item.getPossibleUpgradesByRank(focus, 0).length == 0 && item.getPossibleUpgradesByRank(focus, 6).length == 0, "no rank 0 or 6");
        test.assertTrue(FocusUpgradeType.extend.id == 7 && FocusUpgradeType.get((short) 7) == FocusUpgradeType.extend, "Extend is the original upgrade 7");
        ItemStack extended = focus(FocusUpgradeType.extend, FocusUpgradeType.extend);
        test.assertTrue(FocusUpgradeData.level(extended, FocusUpgradeType.extend) == 2 && FocusData.isSupportedFocus(extended), "Extend may be taken twice");
        ItemStack gold = wand("gold", 1000, focus());
        test.assertTrue(ItemFocusPortableHole.activationCost(gold, player, 0).equals(Map.of("perditio", 10, "aer", 10))
                && ItemFocusPortableHole.activationCost(gold, player, 1).equals(Map.of("perditio", 10, "aer", 10))
                && ItemFocusPortableHole.activationCost(gold, player, 5).equals(Map.of("perditio", 50, "aer", 50))
                && ItemFocusPortableHole.activationCost(gold, player, 12).equals(Map.of("perditio", 120, "aer", 120)),
                "a depth of 0 or 1 costs 10 each, deeper ones 10 a block (the original keeps the larger): " + ItemFocusPortableHole.activationCost(gold, player, 0));
        ItemStack thrifty = wand("gold", 1000, focus(FocusUpgradeType.frugal));
        test.assertTrue(ItemFocusPortableHole.activationCost(thrifty, player, 10).equals(Map.of("perditio", 90, "aer", 90)), "Frugal I takes a tenth off");
        test.assertTrue(PortableHoleRules.maxDepth(0) == 33 && PortableHoleRules.maxDepth(2) == 49 && PortableHoleRules.duration(0) == 120 && PortableHoleRules.duration(3) == 300, "reach and duration numbers");
        var block = ThaumcraftHoles.HOLE_BLOCK.get();
        var state = block.defaultBlockState();
        // Oracle BlockHole:53 - the original sets the blast resistance to 6000000, and 1.7.10 stores
        // that three times and divides it by five again, so the effective resistance is 3600000.
        test.assertTrue(state.getLightEmission() == 10 && state.getDestroySpeed(test.getLevel(), BlockPos.ZERO) == -1.0F && block.getExplosionResistance() == 3600000.0F
                && state.getCollisionShape(test.getLevel(), BlockPos.ZERO).isEmpty() && state.getShape(test.getLevel(), BlockPos.ZERO).isEmpty()
                && !state.canBeReplaced() && state.isSolid() && BuiltInRegistries.ITEM.getOptional(id("hole")).isEmpty(),
                "the hole: light 10, unbreakable, no collision or outline, not replaceable, solid for fluids, and no item");
    }

    private static void recipe(GameTestHelper test, ServerLevel level) {
        var infusion = (InfusionStackRecipe) level.getRecipeManager().byKey(id("infusion/focus_portable_hole")).orElseThrow(() -> new AssertionError("no recipe")).value();
        test.assertTrue(infusion.research().equals("FOCUSPORTABLEHOLE") && infusion.instability() == 3 && infusion.result().is(ThaumcraftFoci.PORTABLE_HOLE_FOCUS.get())
                        && infusion.aspects().equals(Map.of("iter", 25, "alienis", 10, "permutatio", 10, "perditio", 25)) && infusion.components().size() == 6,
                "the infusion: FOCUSPORTABLEHOLE, instability 3, 25 travel + 10 eldritch + 10 exchange + 25 entropy: " + infusion.aspects());
        ItemStack quartz = new ItemStack(Items.QUARTZ);
        var parts = List.of(quartz, new ItemStack(BuiltInRegistries.ITEM.get(id("earth_shard"))), quartz, new ItemStack(BuiltInRegistries.ITEM.get(id("air_shard"))), quartz,
                new ItemStack(BuiltInRegistries.ITEM.get(id("entropy_shard"))));
        test.assertTrue(infusion.matches(new InfusionRecipeInput(new ItemStack(Items.ENDER_PEARL), parts), level), "an ender pearl, three quartz and earth, air and entropy shards complete it");
        test.assertTrue(!infusion.matches(new InfusionRecipeInput(new ItemStack(Items.SNOWBALL), parts), level), "a snowball is not the centre");
        var wrong = new ArrayList<>(parts);
        wrong.set(5, new ItemStack(BuiltInRegistries.ITEM.get(id("fire_shard"))));
        test.assertTrue(!infusion.matches(new InfusionRecipeInput(new ItemStack(Items.ENDER_PEARL), wrong), level), "a fire shard is not the entropy shard");
    }

    // ---- the walls ------------------------------------------------------------------------------------------------------------

    private static Region build(ServerLevel level, BlockPos origin, int index, int length) {
        BlockPos start = origin.offset(2, 12 * index, 0);
        Region region = new Region(start, length, new LinkedHashMap<>());
        for (BlockPos pos : region.positions()) level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        return region;
    }

    private static void record(ServerLevel level, Region region) {
        for (BlockPos pos : region.positions()) region.before().put(pos, level.getBlockState(pos));
    }

    private static void aim(ServerPlayer player, Region region, ItemStack wand) {
        // The eye (1.62 up) is level with the middle of the first block of the wall.
        player.setPos(region.start().getX() - 1.5, region.start().getY() - 1.12, region.start().getZ() + .5);
        player.setYRot(-90F);
        player.setXRot(0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
    }

    private static int holes(ServerLevel level, Region region) {
        int total = 0;
        for (BlockPos pos : region.positions()) if (level.getBlockState(pos).is(ThaumcraftHoles.HOLE_BLOCK.get())) total++;
        return total;
    }

    private static boolean hole(ServerLevel level, Region region, int along, int dy, int dz) {
        return level.getBlockState(region.cell(along, dy, dz)).is(ThaumcraftHoles.HOLE_BLOCK.get());
    }

    private static void shoot(GameTestHelper test, ServerLevel level, ServerPlayer player, BlockPos origin, List<Region> regions, Runnable cleanup) {
        // Region 0: the plain focus. Region 1: Extend. 2 and 3: the reach. 4: bedrock. 5: a chest. 6: water. 7: a piston. 8: no vis. 9: nothing to hit.
        int[] lengths = {12, 12, 50, 50, 12, 12, 12, 12, 12, 12};
        for (int index = 0; index < lengths.length; index++) regions.add(build(level, origin, index, lengths[index]));
        Region plain = regions.get(0), extended = regions.get(1), reach = regions.get(2), enlarged = regions.get(3), bedrock = regions.get(4),
                chest = regions.get(5), water = regions.get(6), piston = regions.get(7), dry = regions.get(8), sky = regions.get(9);
        // Blocks with state, in the ring of the plain wall, to see them come back exactly.
        BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP).setValue(StairBlock.SHAPE, StairsShape.INNER_LEFT);
        BlockState slab = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP).setValue(SlabBlock.WATERLOGGED, true);
        BlockState snowy = Blocks.GRASS_BLOCK.defaultBlockState().setValue(BlockStateProperties.SNOWY, true);
        level.setBlock(plain.cell(5, 1, 1), stairs, Block.UPDATE_CLIENTS);
        level.setBlock(plain.cell(6, -1, -1), slab, Block.UPDATE_CLIENTS);
        level.setBlock(plain.cell(7, 0, 1), snowy, Block.UPDATE_CLIENTS);
        level.setBlock(extended.cell(5, 1, 1), stairs, Block.UPDATE_CLIENTS);
        level.setBlock(bedrock.cell(5, 0, 0), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(chest.cell(3, 0, 0), Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(water.cell(2, 0, 0), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(piston.cell(0, 0, 0), Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP), Block.UPDATE_CLIENTS);
        for (Region region : regions) record(level, region);

        // ---- every shot, in order, and the payments -------------------------------------------------------------------------------
        record Shot(Region region, ItemStack wand, int eachCost, boolean paid) {}
        List<Shot> shots = List.of(
                new Shot(plain, wand("gold", 2000, focus()), 120, true),
                new Shot(extended, wand("gold", 2000, focus(FocusUpgradeType.extend)), 120, true),
                new Shot(reach, wand("gold", 2000, focus()), 330, true),
                new Shot(enlarged, wand("gold", 2000, focus(FocusUpgradeType.enlarge)), 410, true),
                new Shot(bedrock, wand("gold", 2000, focus()), 50, true),
                new Shot(chest, wand("gold", 2000, focus()), 120, true),
                new Shot(water, wand("gold", 2000, focus()), 120, true),
                new Shot(piston, wand("gold", 2000, focus()), 10, true),
                new Shot(dry, wand("gold", 5, focus()), 0, false));
        for (Shot shot : shots) {
            aim(player, shot.region(), shot.wand());
            var result = WandFocusActions.use(player, InteractionHand.MAIN_HAND);
            test.assertTrue(result.getResult().consumesAction(), "the focus answers a right click");
            WandData data = ItemWandCasting.data(shot.wand());
            boolean fine = shot.paid() ? data.amount("aer") == 2000 - shot.eachCost() && data.amount("perditio") == 2000 - shot.eachCost() && data.amount("terra") == 2000 && data.amount("ignis") == 2000
                    : data.amount("aer") == 5 && data.amount("perditio") == 5;
            test.assertTrue(fine, "wall at " + shot.region().start() + " paid " + shot.eachCost() + " entropy and air: aer " + data.amount("aer") + ", perditio " + data.amount("perditio"));
        }
        // Nothing to hit: looking at the sky costs nothing and changes nothing.
        ItemStack skyWand = wand("gold", 2000, focus());
        aim(player, sky, skyWand);
        player.setXRot(-90F);
        WandFocusActions.use(player, InteractionHand.MAIN_HAND);
        test.assertTrue(ItemWandCasting.data(skyWand).amount("aer") == 2000 && holes(level, sky) == 0, "a click on nothing is free");
        // The first block of each shot is a hole at once, with the chain written on it.
        TileHole first = (TileHole) level.getBlockEntity(plain.cell(0, 0, 0));
        test.assertTrue(first != null && level.getBlockState(plain.cell(0, 0, 0)).is(ThaumcraftHoles.HOLE_BLOCK.get()) && first.oldState().is(Blocks.STONE)
                        && first.count() == 13 && first.direction() == 4 && first.countdownMax() == 120 && first.countdown() == 0,
                "the clicked block is a hole that remembers stone, 13 blocks to go, east-facing wall (side 4), 120 ticks");
        TileHole longest = (TileHole) level.getBlockEntity(enlarged.cell(0, 0, 0));
        test.assertTrue(((TileHole) level.getBlockEntity(reach.cell(0, 0, 0))).count() == 34 && longest.count() == 42, "reach 33 and with Enlarge 41 blocks: " + ((TileHole) level.getBlockEntity(reach.cell(0, 0, 0))).count() + " " + longest.count());
        test.assertTrue(((TileHole) level.getBlockEntity(extended.cell(0, 0, 0))).countdownMax() == 180, "Extend I lasts 180 ticks");
        test.assertTrue(level.getBlockState(piston.cell(0, 0, 0)).is(Blocks.PISTON) && holes(level, piston) == 0, "a piston is never a hole, but the vis was paid");
        test.assertTrue(holes(level, dry) == 0 && level.getBlockState(dry.cell(0, 0, 0)).is(Blocks.STONE), "without the vis nothing happens");

        // ---- thirty ticks on: the tunnels are cut ------------------------------------------------------------------------------------
        test.runAfterDelay(60, () -> guard(cleanup, () -> {
            test.assertTrue(holes(level, plain) == 108 && holes(level, extended) == 108, "12 blocks and 8 round each: 108 holes: " + holes(level, plain) + " " + holes(level, extended));
            test.assertTrue(hole(level, plain, 11, 0, 0) && !hole(level, plain, 12, 0, 0) && hole(level, plain, 5, 1, 1) && hole(level, plain, 5, -1, -1) && !hole(level, plain, 5, 2, 2)
                            && !hole(level, plain, 5, 0, 2) && !hole(level, plain, 5, 3, 0), "the tunnel is 3 x 3 across and 12 long");
            // The chain is the measured depth plus the clicked block: 34 centres, and a ring round every one but the last.
            test.assertTrue(holes(level, reach) == 34 + 33 * 8 && hole(level, reach, 33, 0, 0) && !hole(level, reach, 34, 0, 0) && !hole(level, reach, 33, 1, 0), "33 blocks of depth: " + holes(level, reach));
            test.assertTrue(holes(level, enlarged) == 42 + 41 * 8 && hole(level, enlarged, 41, 0, 0) && !hole(level, enlarged, 42, 0, 0), "41 blocks with Enlarge: " + holes(level, enlarged));
            test.assertTrue(holes(level, bedrock) == 5 * 9 && hole(level, bedrock, 4, 0, 0) && level.getBlockState(bedrock.cell(5, 0, 0)).is(Blocks.BEDROCK) && !hole(level, bedrock, 6, 0, 0),
                    "the tunnel stops at bedrock: " + holes(level, bedrock));
            test.assertTrue(holes(level, chest) == 3 * 9 && level.getBlockState(chest.cell(3, 0, 0)).is(Blocks.CHEST) && !hole(level, chest, 4, 0, 0), "and at a block with a tile: " + holes(level, chest));
            test.assertTrue(holes(level, water) == 2 * 9 && level.getBlockState(water.cell(2, 0, 0)).is(Blocks.WATER) && !hole(level, water, 3, 0, 0)
                    && !level.getBlockState(water.cell(1, 0, 0)).is(Blocks.WATER), "and at water, which does not flow into a hole: " + holes(level, water));
            var tile = (TileHole) level.getBlockEntity(plain.cell(3, 0, 0));
            test.assertTrue(tile.countdown() >= 55 && tile.countdown() <= 60 && tile.count() == 10, "a hole has aged and counts down the chain: " + tile.countdown() + " " + tile.count());
            // A tile survives saving and loading.
            CompoundTag saved = tile.saveWithFullMetadata(level.registryAccess());
            var copy = new TileHole(tile.getBlockPos(), tile.getBlockState());
            copy.loadWithComponents(saved, level.registryAccess());
            test.assertTrue(copy.countdown() == tile.countdown() && copy.countdownMax() == 120 && copy.count() == 10 && copy.direction() == 4 && copy.oldState().is(Blocks.STONE), "the tile saves and loads");
            var stairsTile = (TileHole) level.getBlockEntity(plain.cell(5, 1, 1));
            var stairsCopy = new TileHole(stairsTile.getBlockPos(), stairsTile.getBlockState());
            stairsCopy.loadWithComponents(stairsTile.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            test.assertTrue(stairsCopy.oldState().equals(stairs), "with the exact state of what it replaced: " + stairsCopy.oldState());
        }));
        // ---- after 120 ticks the plain wall is back; Extend's is still open ----------------------------------------------------------------
        test.runAfterDelay(150, () -> guard(cleanup, () -> {
            test.assertTrue(holes(level, plain) == 0, "after 120 ticks the plain wall is whole again: " + holes(level, plain) + " holes");
            for (var entry : plain.before().entrySet()) test.assertTrue(level.getBlockState(entry.getKey()).equals(entry.getValue()), "back as it was: " + entry.getKey() + " " + level.getBlockState(entry.getKey()));
            test.assertTrue(level.getBlockState(plain.cell(5, 1, 1)).equals(stairs) && level.getBlockState(plain.cell(6, -1, -1)).equals(slab) && level.getBlockState(plain.cell(7, 0, 1)).equals(snowy), "stairs, waterlogged slab and snowy grass in their exact states");
            test.assertTrue(level.getBlockEntity(plain.cell(2, 0, 0)) == null, "no tile is left");
            test.assertTrue(holes(level, extended) == 108, "Extend's wall is still open at 150 ticks: " + holes(level, extended));
        }));
        test.runAfterDelay(215, () -> {
            try {
                test.assertTrue(holes(level, extended) == 0, "and closed by 215: " + holes(level, extended));
                for (var entry : extended.before().entrySet()) test.assertTrue(level.getBlockState(entry.getKey()).equals(entry.getValue()), "Extend's wall back as it was: " + entry.getKey());
                for (Region region : List.of(reach, enlarged, bedrock, chest, water, piston, dry, sky)) {
                    test.assertTrue(holes(level, region) == 0, "every other wall is whole again: " + region.start());
                    for (var entry : region.before().entrySet()) test.assertTrue(level.getBlockState(entry.getKey()).equals(entry.getValue()), "back as it was: " + entry.getKey());
                }
            } finally {
                cleanup.run();
            }
            test.succeed();
        });
    }

    private static void guard(Runnable cleanup, Runnable body) {
        try {
            body.run();
        } catch (RuntimeException | Error failure) {
            cleanup.run();
            throw failure;
        }
    }
}
