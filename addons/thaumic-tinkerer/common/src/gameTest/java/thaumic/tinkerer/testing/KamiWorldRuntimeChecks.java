/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): runtime checks of the KamiWorld group, run by the Fabric GameTests. Distributed under the same licence as the add-on
 * (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.testing;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.block.kami.BlockBedrockPortal;
import thaumic.tinkerer.common.block.tile.container.kami.ContainerWarpGate;
import thaumic.tinkerer.common.block.tile.kami.TileWarpGate;
import thaumic.tinkerer.common.content.KamiWorld;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.core.handler.kami.DimensionalShardDropHandler;
import thaumic.tinkerer.common.core.handler.kami.SoulHeartHandler;
import thaumic.tinkerer.common.dim.ChunkGeneratorBedrock;
import thaumic.tinkerer.common.dim.EnumOreFrequency;
import thaumic.tinkerer.common.dim.TeleporterBedrock;
import thaumic.tinkerer.common.dim.WorldProviderBedrock;
import thaumic.tinkerer.common.item.kami.ItemSkyPearl;
import thaumic.tinkerer.common.item.kami.KamiHooks;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.TTRecipes;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>Runtime checks of the KamiWorld group in a running server: registration (blocks, block entity, menu, generator type,
 * the dimension itself), the terrain and ore veins of the Bedrock dimension with the config options, where the portal leads
 * (and an item carried through it), the Celestial Gateway (saving, teleporting, refusing, the menu), the Soul Hearts, the
 * shard drops and {@code /iskamiunlocked}. They were written without being able to run them (no Gradle and no game in the
 * porting session): the first run may show a wrong assumption of a check as well as a defect.</p>
 */
public final class KamiWorldRuntimeChecks {
    private KamiWorldRuntimeChecks() {}

    // ---- helpers ---------------------------------------------------------------------------------------------------

    private static ServerLevel bedrock(GameTestHelper test) {
        ServerLevel level = test.getLevel().getServer().getLevel(WorldProviderBedrock.KEY);
        test.assertTrue(level != null, "the Bedrock dimension exists");
        return level;
    }

    /** A floor of stone at relative y = 0, so that items and the mock player stand. */
    private static void floor(GameTestHelper test) {
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) test.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    private static ServerPlayer player(GameTestHelper test, double x, double y, double z) {
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        Vec3 at = test.absoluteVec(new Vec3(x, y, z));
        player.moveTo(at.x, at.y, at.z, 0F, 0F);
        player.getInventory().clearContent();
        return player;
    }

    /** Removes the mock players (they would stay in the world and pick up what later checks drop) and ends the check. */
    private static void finish(GameTestHelper test, ServerPlayer... players) {
        for (ServerPlayer player : players) player.discard();
        test.succeed();
    }

    private static TileWarpGate gate(GameTestHelper test, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        test.setBlock(pos, KamiWorld.warpGateBlock());
        BlockEntity entity = test.getBlockEntity(pos);
        test.assertTrue(entity instanceof TileWarpGate, "the gateway has its block entity");
        return (TileWarpGate) entity;
    }

    private static ItemStack pearl(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos target) {
        ItemStack stack = TTRegistry.stack(LibItemNames.SKY_PEARL);
        ItemSkyPearl.setValues(stack, dimension, target);
        return stack;
    }

    /** The blocks the valid ores of the dimension are made of, with bedrock. */
    private static Set<Block> allowedBlocks() {
        Set<Block> allowed = new HashSet<>();
        allowed.add(Blocks.BEDROCK);
        for (EnumOreFrequency ore : EnumOreFrequency.getValidOres(new String[0])) allowed.add(ore.firstBlock());
        return allowed;
    }

    /** Generates the chunks cx..cx+size-1 by cz..cz+size-1 of the Bedrock dimension and counts what is in them. */
    private static int[] scanChunks(GameTestHelper test, ServerLevel level, int cx, int cz, int size, Set<Block> allowed,
            Set<Block> found) {
        int ore = 0;
        int solid = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int chunkX = cx; chunkX < cx + size; chunkX++) {
            for (int chunkZ = cz; chunkZ < cz + size; chunkZ++) {
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 0; y < 256; y++) {
                            BlockState state = chunk.getBlockState(pos.set(chunkX * 16 + x, y, chunkZ * 16 + z));
                            test.assertTrue(allowed.contains(state.getBlock()),
                                    "only bedrock and ore at " + pos + ": " + state);
                            solid++;
                            if (!state.is(Blocks.BEDROCK)) {
                                ore++;
                                found.add(state.getBlock());
                                // 1.7.10: veins start between 6 and 250 and reach a few blocks around, never the outer layers.
                                test.assertTrue(y >= 2 && y <= 251, "ore at the edge of the world: " + pos);
                            }
                        }
                    }
                }
            }
        }
        return new int[] { ore, solid };
    }

    // ---- registration ----------------------------------------------------------------------------------------------

    public static void registrationIsComplete(GameTestHelper test) {
        ResourceLocation gate = LibMisc.id(LibBlockNames.WARP_GATE);
        ResourceLocation portal = LibMisc.id(LibBlockNames.PORTAL);
        test.assertTrue(ConfigHandler.enableKami, "the Kami content is enabled in the test run");
        test.assertTrue(BuiltInRegistries.BLOCK.containsKey(gate), "the gateway block is registered");
        test.assertTrue(BuiltInRegistries.BLOCK.containsKey(portal), "the portal block is registered");
        test.assertTrue(BuiltInRegistries.ITEM.containsKey(gate), "the gateway has an item");
        test.assertTrue(BuiltInRegistries.ITEM.containsKey(portal),
                "the portal has the default item block (1.7.10 getItemBlock() null registered one) and is in the creative tab");
        test.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(gate), "the gateway's block entity type is registered");
        test.assertTrue(BuiltInRegistries.MENU.containsKey(gate), "the gateway's menu type is registered");
        test.assertTrue(BuiltInRegistries.CHUNK_GENERATOR.containsKey(LibMisc.id("bedrock")), "the generator type is registered");
        test.assertTrue(KamiWorld.warpGateBlock().defaultBlockState().getDestroySpeed(test.getLevel(), BlockPos.ZERO) == 5.0F,
                "1.7.10 hardness 5");
        test.assertTrue(KamiWorld.portalBlock().defaultBlockState().getDestroySpeed(test.getLevel(), BlockPos.ZERO) < 0,
                "the portal cannot be broken");
        test.assertTrue(KamiWorld.portalBlock().defaultBlockState().getCollisionShape(test.getLevel(), BlockPos.ZERO).isEmpty(),
                "the portal has no collision box");

        ServerLevel bedrock = bedrock(test);
        test.assertTrue(bedrock.getChunkSource().getGenerator() instanceof ChunkGeneratorBedrock, "the dimension uses the generator");
        test.assertTrue(bedrock.dimensionType().minY() == 0 && bedrock.dimensionType().height() == 256, "256 blocks from y = 0");
        test.assertTrue(bedrock.dimensionType().hasSkyLight() && !bedrock.dimensionType().hasCeiling(), "a sky");
        test.assertTrue(!bedrock.dimensionType().bedWorks(), "beds do not work");

        // The research of the gateway exists exactly when Thaumcraft allows mirrors, with its recipe.
        ResearchItem research = ResearchCategories.getResearch(LibResearch.KEY_WARP_GATE);
        test.assertTrue((research != null) == ThaumcraftApi.isMirrorsAllowed(), "WARP_GATE is registered while mirrors are allowed");
        test.assertTrue(TTRecipes.has(LibResearch.KEY_WARP_GATE) == ThaumcraftApi.isMirrorsAllowed(), "and so is its recipe");
        if (research != null) {
            test.assertTrue(research.category.equals(LibResearch.CATEGORY_THAUMICTINKERER), "in the add-on's tab");
            test.assertTrue(research.key.equals(LibResearch.KEY_WARP_GATE), "the key");
            test.assertTrue(List.of(research.parents).contains(LibResearch.KEY_ICHORCLOTH_CHEST_GEM), "parent: the awakened chestplate");
            test.assertTrue(List.of(research.parentsHidden).contains(LibResearch.KEY_ICHORCLOTH_BOOTS_GEM), "hidden parent: the awakened boots");
            InfusionRecipe recipe = TTRecipes.get(LibResearch.KEY_WARP_GATE, InfusionRecipe.class);
            test.assertTrue(recipe.getInstability() == 8, "instability 8");
            test.assertTrue(recipe.getAspects().size() == 3, "travel, eldritch and flight");
            test.assertTrue(recipe.getComponents().length == 6, "six components: ichor, ender shard, dislocator, nether shard, diamond, feather");
            test.assertTrue(recipe.getRecipeOutput() instanceof ItemStack stack && stack.is(KamiWorld.warpGateBlock().asItem()),
                    "the recipe makes the gateway");
        }
        test.assertTrue(ThaumicTinkerer.failures().isEmpty(), "registration failures: " + ThaumicTinkerer.failures());
        test.succeed();
    }

    // ---- the Bedrock dimension -------------------------------------------------------------------------------------

    /** Every block of the new chunks is bedrock or one of the valid ores, and there is ore. */
    public static void bedrockDimensionIsSolidBedrockWithOreVeins(GameTestHelper test) {
        ServerLevel level = bedrock(test);
        Set<Block> allowed = allowedBlocks();
        test.assertTrue(allowed.size() > 1, "the tags of the data packs give at least one ore: " + allowed);
        Set<Block> found = new HashSet<>();
        int[] counts = scanChunks(test, level, 5000, 5000, 3, allowed, found);
        test.assertTrue(counts[1] == 9 * 16 * 16 * 256, "nine full chunks");
        test.assertTrue(counts[0] > 0, "ore veins were generated");
        test.assertTrue(counts[0] < counts[1] / 2, "bedrock is the bulk: " + counts[0] + " ore blocks of " + counts[1]);
        // The outer layers are never ore.
        for (int y : new int[] { 0, 1, 252, 253, 254, 255 }) {
            for (int x = 0; x < 16; x++) {
                test.assertTrue(level.getBlockState(new BlockPos(5000 * 16 + 20 + x, y, 5000 * 16 + 20)).is(Blocks.BEDROCK),
                        "bedrock at layer " + y);
            }
        }
        // The terrain answers like solid ground to the world: the first free height is the top.
        test.assertTrue(level.getChunkSource().getGenerator().getBaseHeight(5000 * 16, 5000 * 16,
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState()) == 256,
                "solid to the top");
        test.succeed();
    }

    /** {@code bedrockOreDensity} and {@code bedrockOreBlacklist} decide what is generated in chunks that are made afterwards. */
    public static void bedrockOreConfigDecidesDensityAndBlacklist(GameTestHelper test) {
        ServerLevel level = bedrock(test);
        int density = ConfigHandler.bedrockOreDensity;
        String[] blacklist = ConfigHandler.bedrockOreBlacklist;
        try {
            Set<Block> allowed = allowedBlocks();
            List<EnumOreFrequency> valid = EnumOreFrequency.getValidOres(new String[0]);
            test.assertTrue(!valid.isEmpty(), "there are valid ores");

            ConfigHandler.bedrockOreDensity = 0;
            Set<Block> found = new HashSet<>();
            int[] none = scanChunks(test, level, 6000, 6000, 2, allowed, found);
            test.assertTrue(none[0] == 0, "a density of 0 makes no ore: " + none[0]);

            ConfigHandler.bedrockOreDensity = 1;
            List<String> allNames = new ArrayList<>();
            for (EnumOreFrequency e : EnumOreFrequency.values()) allNames.add(e.name);
            ConfigHandler.bedrockOreBlacklist = allNames.toArray(new String[0]);
            int[] blacklisted = scanChunks(test, level, 6100, 6100, 2, allowed, found);
            test.assertTrue(blacklisted[0] == 0, "with every ore on the blacklist there is none: " + blacklisted[0]);

            // Only one ore left: everything generated is that ore.
            EnumOreFrequency keep = valid.get(0);
            List<String> others = new ArrayList<>();
            for (EnumOreFrequency e : EnumOreFrequency.values()) if (e != keep) others.add(e.name);
            ConfigHandler.bedrockOreBlacklist = others.toArray(new String[0]);
            Set<Block> onlyKept = new HashSet<>(Set.of(Blocks.BEDROCK, keep.firstBlock()));
            Set<Block> foundKept = new HashSet<>();
            int[] kept = scanChunks(test, level, 6200, 6200, 3, onlyKept, foundKept);
            test.assertTrue(kept[0] > 0, "the remaining ore is generated");
            test.assertTrue(foundKept.equals(Set.of(keep.firstBlock())), "only " + keep + ": " + foundKept);

            // A block id on the blacklist works as well as the 1.7.10 name.
            ConfigHandler.bedrockOreBlacklist = new String[] { BuiltInRegistries.BLOCK.getKey(keep.firstBlock()).toString() };
            test.assertTrue(!EnumOreFrequency.getValidOres(ConfigHandler.bedrockOreBlacklist).contains(keep), "blacklisted by block id");
            test.assertTrue(keep.isBlacklisted(new String[] { "#" + keep.tag().location() }, null), "blacklisted by tag id");
        } finally {
            ConfigHandler.bedrockOreDensity = density;
            ConfigHandler.bedrockOreBlacklist = blacklist;
        }
        test.succeed();
    }

    // ---- the portal ------------------------------------------------------------------------------------------------

    /** Overworld to Bedrock: the column is prepared, the destination is the middle of the block at 251; back is random. */
    public static void portalDestinationsFollowTheRules(GameTestHelper test) {
        floor(test);
        ServerLevel overworld = test.getLevel();
        ServerLevel bedrock = bedrock(test);
        ServerPlayer player = player(test, 3.5, 1, 3.5);
        BlockPos portalPos = test.absolutePos(new BlockPos(3, 2, 3));
        BlockBedrockPortal portal = KamiWorld.portalBlock();

        // In: from a surface world.
        DimensionTransition in = portal.getPortalDestination(overworld, player, portalPos);
        test.assertTrue(in != null, "the Overworld portal has a destination");
        test.assertTrue(in.newLevel() == bedrock, "the Bedrock dimension");
        test.assertTrue(in.pos().equals(new Vec3(portalPos.getX() + 0.5, 251, portalPos.getZ() + 0.5)), "the middle of the block at 251: " + in.pos());
        test.assertTrue(in.yRot() == 0F && in.xRot() == 0F, "yaw 0, pitch 0");
        test.assertTrue(in.speed().equals(Vec3.ZERO), "no momentum");
        int x = portalPos.getX();
        int z = portalPos.getZ();
        // The ore veins of the dimension start as high as layer 250 and reach layer 251 at most (1.7.10 and here alike), and the
        // portal only clears stone and bedrock: whether a vein of this world's seed crosses the column is chance, so the
        // layers 249 to 251 may hold an ore block instead of bedrock or air. Layers 252 and above never do.
        test.assertTrue(!bedrock.getBlockState(new BlockPos(x, TeleporterBedrock.FLOOR_Y, z)).isAir(), "something solid below the cavity");
        for (int y = TeleporterBedrock.CAVITY_MIN_Y; y <= TeleporterBedrock.CAVITY_MAX_Y; y++) {
            BlockState cell = bedrock.getBlockState(new BlockPos(x, y, z));
            test.assertTrue(cell.isAir() || (y <= 251 && !cell.is(Blocks.STONE) && !cell.is(Blocks.BEDROCK)),
                    "air at " + y + " (an ore of a vein is left alone at 250 and 251): " + cell);
        }
        test.assertTrue(bedrock.getBlockState(new BlockPos(x, TeleporterBedrock.EXIT_Y, z)).is(portal), "the exit portal at 254");
        test.assertTrue(bedrock.getBlockState(new BlockPos(x, 255, z)).is(Blocks.BEDROCK), "bedrock above it");
        test.assertTrue(bedrock.getBlockState(new BlockPos(x + 1, 252, z)).is(Blocks.BEDROCK), "the cavity is one block wide");

        // A second time: what the player built stays, stone and bedrock are cleared again.
        bedrock.setBlock(new BlockPos(x, 252, z), Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
        bedrock.setBlock(new BlockPos(x, 251, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        portal.getPortalDestination(overworld, player, portalPos);
        test.assertTrue(bedrock.getBlockState(new BlockPos(x, 252, z)).is(Blocks.GLASS), "other blocks are not cleared");
        test.assertTrue(bedrock.getBlockState(new BlockPos(x, 251, z)).isAir(), "stone is");

        // Out: from the Bedrock dimension, players only, to the Overworld.
        DimensionTransition out = portal.getPortalDestination(bedrock, player, new BlockPos(x, 254, z));
        test.assertTrue(out != null && out.newLevel() == overworld.getServer().getLevel(Level.OVERWORLD), "players leave to the Overworld");
        double px = player.getX();
        double pz = player.getZ();
        test.assertTrue(out.pos().x >= Math.floor(px) - 50 && out.pos().x < Math.floor(px) + 50 + 1, "within 50 blocks in x: " + out.pos());
        test.assertTrue(out.pos().z >= Math.floor(pz) - 50 && out.pos().z < Math.floor(pz) + 50 + 1, "within 50 blocks in z: " + out.pos());
        BlockPos landing = BlockPos.containing(out.pos());
        ServerLevel target = out.newLevel();
        test.assertTrue(target.getBlockState(landing).isAir() && target.getBlockState(landing.below()).isAir()
                && target.getBlockState(landing.below(2)).isAir(), "three free blocks above the ground: the player falls two");
        test.assertTrue(!target.getBlockState(landing.below(3)).isAir(), "the first block that is not air is three below the feet");

        Zombie zombie = EntityType.ZOMBIE.create(bedrock);
        test.assertTrue(portal.getPortalDestination(bedrock, zombie, new BlockPos(x, 254, z)) == null,
                "only players leave the Bedrock dimension");
        test.assertTrue(portal.getPortalDestination(overworld, zombie, portalPos) == null, "only players enter the Bedrock dimension");
        ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
        if (nether != null) {
            test.assertTrue(portal.getPortalDestination(nether, player, new BlockPos(0, 64, 0)) == null, "the Nether is no surface world");
        }
        finish(test, player);
    }

    /**
     * 1.7.10 sent players only (the tile entity that would have sent everything never existed in the world): an item lying in
     * an Overworld portal stays in the Overworld, and the portal has no destination for it.
     */
    public static void portalLeavesNonPlayersAlone(GameTestHelper test) {
        floor(test);
        test.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        test.setBlock(new BlockPos(3, 2, 3), KamiWorld.portalBlock());
        BlockPos portalPos = test.absolutePos(new BlockPos(3, 2, 3));
        ItemEntity item = test.spawnItem(Items.COBBLESTONE, 3.5F, 2.2F, 3.5F);
        Zombie zombie = EntityType.ZOMBIE.create(test.getLevel());
        test.assertTrue(zombie != null, "a zombie can be made");
        test.assertTrue(KamiWorld.portalBlock().getPortalDestination(test.getLevel(), item, portalPos) == null, "no destination for an item");
        test.assertTrue(KamiWorld.portalBlock().getPortalDestination(test.getLevel(), zombie, portalPos) == null, "no destination for a mob");
        test.runAfterDelay(40, () -> {
            test.assertTrue(!item.isRemoved() && item.level() == test.getLevel(), "the item is still in the Overworld after two seconds");
            test.assertTrue(Math.abs(item.getX() - (portalPos.getX() + 0.5)) < 1.0 && Math.abs(item.getZ() - (portalPos.getZ() + 0.5)) < 1.0,
                    "and still at the portal: " + item.position());
            item.discard();
            test.succeed();
        });
    }

    // ---- the Celestial Gateway -------------------------------------------------------------------------------------

    public static void warpGateKeepsItsFlagAndPearlsThroughSaving(GameTestHelper test) {
        TileWarpGate gate = gate(test, 2, 1, 2);
        test.assertTrue(!gate.locked, "a new gateway accepts teleports");
        test.assertTrue(gate.getContainerSize() == 10, "ten slots");
        ItemStack first = pearl(Level.OVERWORLD, new BlockPos(5, 64, -3));
        ItemStack second = pearl(Level.NETHER, new BlockPos(-100, 70, 40));
        gate.setItem(3, first);
        gate.setItem(9, second);
        gate.setLocked(true);
        gate.setItem(4, new ItemStack(TTRegistry.item(LibItemNames.SKY_PEARL), 5));
        test.assertTrue(gate.getItem(4).getCount() == 1, "a slot holds one pearl");
        test.assertTrue(gate.canPlaceItem(0, TTRegistry.stack(LibItemNames.SKY_PEARL)), "pearls fit");
        test.assertTrue(!gate.canPlaceItem(0, new ItemStack(Items.STONE)), "nothing else does");
        gate.removeItem(4, 1);

        var registries = test.getLevel().registryAccess();
        CompoundTag saved = gate.saveWithFullMetadata(registries);
        BlockEntity loaded = BlockEntity.loadStatic(gate.getBlockPos(), gate.getBlockState(), saved, registries);
        test.assertTrue(loaded instanceof TileWarpGate, "the saved data makes a gateway again");
        TileWarpGate copy = (TileWarpGate) loaded;
        test.assertTrue(copy.locked, "the locked flag survives");
        test.assertTrue(saved.getBoolean("locked"), "under the key the Recall focus' default reads: locked");
        test.assertTrue(ItemSkyPearl.isAttuned(copy.getItem(3)) && ItemSkyPearl.getPos(copy.getItem(3)).equals(new BlockPos(5, 64, -3))
                && Level.OVERWORLD.equals(ItemSkyPearl.getDim(copy.getItem(3))), "the first pearl survives with its place");
        test.assertTrue(ItemSkyPearl.isAttuned(copy.getItem(9)) && Level.NETHER.equals(ItemSkyPearl.getDim(copy.getItem(9)))
                && ItemSkyPearl.getPos(copy.getItem(9)).equals(new BlockPos(-100, 70, 40)), "the second pearl survives with its dimension");
        for (int i : new int[] { 0, 1, 2, 4, 5, 6, 7, 8 }) test.assertTrue(copy.getItem(i).isEmpty(), "slot " + i + " is empty");

        // What a client gets is the same.
        CompoundTag update = gate.getUpdateTag(registries);
        test.assertTrue(update.getBoolean("locked") && update.contains("Items"), "the update tag carries the flag and the pearls");
        test.assertTrue(gate.getUpdatePacket() != null, "an update packet exists");
        test.succeed();
    }

    public static void warpGateTeleportsToAnotherGateAndRefusesWhatItShould(GameTestHelper test) {
        floor(test);
        TileWarpGate start = gate(test, 1, 1, 1);
        TileWarpGate target = gate(test, 6, 1, 6);
        ServerPlayer player = player(test, 1.5, 2, 1.5);
        test.assertTrue(player.connection != null, "the mock player has a connection");
        BlockPos targetPos = target.getBlockPos();
        Level level = test.getLevel();

        start.setItem(3, pearl(level.dimension(), targetPos));
        start.setItem(5, pearl(level.dimension(), test.absolutePos(new BlockPos(4, 1, 0)))); // no gateway there
        start.setItem(6, TTRegistry.stack(LibItemNames.SKY_PEARL)); // not attuned
        test.assertTrue(!start.teleportPlayer(player, 5), "no gateway at the pearl's place: refused");
        test.assertTrue(!start.teleportPlayer(player, 6), "an unattuned pearl goes nowhere");
        test.assertTrue(!start.teleportPlayer(player, 0), "an empty slot goes nowhere");
        test.assertTrue(!start.teleportPlayer(player, -1) && !start.teleportPlayer(player, 10), "no such slot");

        target.setLocked(true);
        Vec3 before = player.position();
        test.assertTrue(!start.teleportPlayer(player, 3), "a locked gateway refuses incoming teleports");
        test.assertTrue(player.position().equals(before), "the player did not move");

        target.setLocked(false);
        test.assertTrue(start.teleportPlayer(player, 3), "an unlocked gateway accepts");
        test.assertTrue(Math.abs(player.getX() - (targetPos.getX() + 0.5)) < 1e-6 && Math.abs(player.getZ() - (targetPos.getZ() + 0.5)) < 1e-6
                && Math.abs(player.getY() - (targetPos.getY() + 1.6)) < 1e-6, "1.6 blocks above the destination: " + player.position());
        test.assertTrue(!start.teleportPlayer(player, 3), "at most one teleport per tick");
        TileWarpGate.tick(level, start.getBlockPos(), start.getBlockState(), start);
        test.assertTrue(start.teleportPlayer(player, 3), "the next tick it works again");
        // The Focus of Celestial Recall's hook is this method.
        player.moveTo(test.absoluteVec(new Vec3(1.5, 2, 1.5)));
        test.assertTrue(KamiHooks.warpGateTeleporter.teleport(player, targetPos), "the hook of the Recall focus teleports to the gateway");
        test.assertTrue(Math.abs(player.getX() - (targetPos.getX() + 0.5)) < 1e-6, "and puts the player there");
        target.setLocked(true);
        test.assertTrue(!KamiHooks.warpGateTeleporter.teleport(player, targetPos), "and refuses a locked one");
        test.assertTrue(!KamiHooks.warpGateTeleporter.teleport(player, test.absolutePos(new BlockPos(4, 1, 0))), "and a place without a gateway");
        finish(test, player);
    }

    /** A pearl of another dimension takes the player to that dimension's gateway (the gateway is placed in the Bedrock dimension). */
    public static void warpGateTakesAPearlOfAnotherDimension(GameTestHelper test) {
        floor(test);
        TileWarpGate start = gate(test, 1, 1, 1);
        ServerLevel bedrock = bedrock(test);
        BlockPos far = new BlockPos(90000, 100, 90000);
        bedrock.getChunk(far.getX() >> 4, far.getZ() >> 4);
        bedrock.setBlock(far, KamiWorld.warpGateBlock().defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity farEntity = bedrock.getBlockEntity(far);
        test.assertTrue(farEntity instanceof TileWarpGate, "a gateway in the Bedrock dimension");
        ServerPlayer player = player(test, 1.5, 2, 1.5);
        start.setItem(0, pearl(WorldProviderBedrock.KEY, far));
        boolean teleported = start.teleportPlayer(player, 0);
        test.assertTrue(teleported, "the pearl leads to the Bedrock dimension's gateway");
        test.assertTrue(player.level() == bedrock, "the player is in the Bedrock dimension: " + player.level().dimension().location());
        test.assertTrue(Math.abs(player.getY() - (far.getY() + 1.6)) < 1e-6, "1.6 above the gateway");
        // And the pearl of a dimension nobody has gets "no destination".
        ServerPlayer other = player(test, 2.5, 2, 2.5);
        start.setItem(1, pearl(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                LibMisc.id("no_such_dimension")), BlockPos.ZERO));
        TileWarpGate.tick(test.getLevel(), start.getBlockPos(), start.getBlockState(), start);
        test.assertTrue(!start.teleportPlayer(other, 1), "a dimension that does not exist has no destination");
        bedrock.removeBlock(far, false);
        // The first player was moved out of the test's dimension; take it off the server properly.
        test.getLevel().getServer().getPlayerList().remove(player);
        finish(test, other);
    }

    public static void warpGateMenuMovesOnlyPearls(GameTestHelper test) {
        TileWarpGate gate = gate(test, 2, 1, 2);
        ServerPlayer player = player(test, 2.5, 2, 2.5);
        ContainerWarpGate menu = new ContainerWarpGate(7, player.getInventory(), gate);
        test.assertTrue(menu.slots.size() == 10 + 36, "ten pearl slots and the player's inventory");
        ItemStack pearl = pearl(Level.OVERWORLD, new BlockPos(1, 2, 3));
        test.assertTrue(menu.getSlot(0).mayPlace(pearl) && !menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)), "only pearls fit");
        test.assertTrue(menu.getSlot(0).getMaxStackSize() == 1, "one per slot");
        test.assertTrue(menu.stillValid(player), "the player stands next to the gateway");

        // Shift-click a pearl in: into the first free pearl slot.
        player.getInventory().setItem(9, pearl.copy());
        ItemStack moved = menu.quickMoveStack(player, 10);
        test.assertTrue(ItemStack.isSameItemSameComponents(moved, pearl), "what moved is returned");
        test.assertTrue(ItemSkyPearl.isAttuned(gate.getItem(0)), "the pearl is in slot 0");
        test.assertTrue(player.getInventory().getItem(9).isEmpty(), "and left the inventory");
        // A stone does not move at all.
        player.getInventory().setItem(10, new ItemStack(Items.STONE, 3));
        test.assertTrue(menu.quickMoveStack(player, 11).isEmpty(), "nothing else moves into the gateway");
        test.assertTrue(player.getInventory().getItem(10).getCount() == 3, "the stone stays");
        // Shift-click a pearl out: into the main inventory (menu slots 10 to 35), the first free one.
        player.getInventory().setItem(9, ItemStack.EMPTY);
        ItemStack out = menu.quickMoveStack(player, 0);
        test.assertTrue(!out.isEmpty() && ItemSkyPearl.isAttuned(player.getInventory().getItem(9)), "the pearl goes to the inventory");
        test.assertTrue(gate.getItem(0).isEmpty(), "and leaves the gateway");

        // The flag is a data slot of the menu.
        gate.locked = true;
        test.assertTrue(menu.slots.size() == 46, "no slot was added for the flag");
        player.moveTo(test.absoluteVec(new Vec3(40, 2, 40)));
        test.assertTrue(!menu.stillValid(player), "far from the gateway the menu closes");
        finish(test, player);
    }

    // ---- the Soul Hearts -------------------------------------------------------------------------------------------

    public static void soulHeartsAbsorbDamageBeforeHealth(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 2.5, 1, 2.5);
        // The mock player is flagged creative; the damage rules are the survival ones.
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        SoulHeartHandler.setHP(player, 0);
        player.setHealth(20F);
        test.assertTrue(SoulHeartHandler.getHP(player) == 0, "no hearts at first");

        // The hook of the Awakened Sword's Soul Mode: one point per call, twenty at most.
        KamiHooks.soulHeartSink.accept(player);
        test.assertTrue(SoulHeartHandler.getHP(player) == 1, "one point");
        for (int i = 0; i < 30; i++) KamiHooks.soulHeartSink.accept(player);
        test.assertTrue(SoulHeartHandler.getHP(player) == SoulHeartHandler.MAX_HP, "twenty points at most");
        test.assertTrue(!SoulHeartHandler.addHP(player, 1), "a full pool takes no more");

        // A hit the pool covers costs no health.
        SoulHeartHandler.setHP(player, 5);
        DamageSource generic = test.getLevel().damageSources().generic();
        player.hurt(generic, 3F);
        test.assertTrue(player.getHealth() == 20F, "a hit of 3 against 5 soul hearts costs no health: " + player.getHealth());
        test.assertTrue(SoulHeartHandler.getHP(player) == 2, "it costs 3 soul hearts: " + SoulHeartHandler.getHP(player));

        // A hit it does not cover costs the rest.
        player.invulnerableTime = 0;
        player.hurt(generic, 4F);
        test.assertTrue(SoulHeartHandler.getHP(player) == 0, "the pool is empty: " + SoulHeartHandler.getHP(player));
        test.assertTrue(player.getHealth() == 18F, "2 points of the hit of 4 went on: " + player.getHealth());

        // Without soul hearts the game is not changed.
        player.invulnerableTime = 0;
        player.hurt(generic, 1.5F);
        test.assertTrue(player.getHealth() == 16.5F, "no hearts, no change: " + player.getHealth());
        SoulHeartHandler.setHP(player, 0);
        finish(test, player);
    }

    public static void soulHeartsAreSavedPerPlayerAndNotShared(GameTestHelper test) {
        ServerPlayer a = player(test, 1.5, 1, 1.5);
        ServerPlayer b = player(test, 2.5, 1, 2.5);
        SoulHeartHandler.setHP(a, 7);
        SoulHeartHandler.setHP(b, 0);
        test.assertTrue(SoulHeartHandler.getHP(a) == 7 && SoulHeartHandler.getHP(b) == 0, "one pool per player");
        MinecraftServer server = test.getLevel().getServer();
        var data = server.overworld().getDataStorage().computeIfAbsent(
                new net.minecraft.world.level.saveddata.SavedData.Factory<>(SoulHeartHandler.SoulHeartData::new,
                        SoulHeartHandler.SoulHeartData::load, null), LibMisc.MOD_ID + "_soul_hearts");
        CompoundTag saved = data.save(new CompoundTag(), test.getLevel().registryAccess());
        SoulHeartHandler.SoulHeartData loaded = SoulHeartHandler.SoulHeartData.load(saved, test.getLevel().registryAccess());
        test.assertTrue(loaded.get(a.getUUID()) == 7, "the pool of a player survives saving");
        test.assertTrue(loaded.get(b.getUUID()) == 0, "an empty pool is not saved");
        test.assertTrue(SoulHeartHandler.removeHP(a, 10) == 3, "ten damage against seven hearts leaves three");
        test.assertTrue(SoulHeartHandler.getHP(a) == 0, "and takes the pool");
        SoulHeartHandler.setHP(a, 0);
        finish(test, a, b);
    }

    // ---- the shard drops -------------------------------------------------------------------------------------------

    private static LootParams killedBy(ServerLevel level, net.minecraft.world.entity.LivingEntity victim, DamageSource source) {
        return new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, victim)
                .withParameter(LootContextParams.ORIGIN, victim.position()).withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .create(LootContextParamSets.ENTITY);
    }

    private static int shards(LootTable table, LootParams params, Item shard, int rolls) {
        int count = 0;
        for (int i = 0; i < rolls; i++) {
            for (ItemStack stack : table.getRandomItems(params)) if (stack.is(shard)) count += stack.getCount();
        }
        return count;
    }

    /** The pool of the shard drop: a player's blow, the right dimension, the chance. */
    public static void shardDropPoolHasItsThreeConditions(GameTestHelper test) {
        MinecraftServer server = test.getLevel().getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        ServerLevel end = server.getLevel(Level.END);
        test.assertTrue(nether != null && end != null, "the Nether and the End exist");
        ServerPlayer player = player(test, 1.5, 1, 1.5);
        Item netherShard = TTRegistry.item(LibItemNames.KAMI_RESOURCE_NAMES[6]);
        LootTable always = LootTable.lootTable().withPool(DimensionalShardDropHandler.pool(netherShard, Level.NETHER, 1.0F)).build();
        LootTable never = LootTable.lootTable().withPool(DimensionalShardDropHandler.pool(netherShard, Level.NETHER, 0.0F)).build();

        var piglin = EntityType.ZOMBIFIED_PIGLIN.create(nether);
        piglin.moveTo(0.5, 70, 0.5);
        DamageSource byPlayer = nether.damageSources().playerAttack(player);
        test.assertTrue(shards(always, killedBy(nether, piglin, byPlayer), netherShard, 5) == 5, "a player's blow in the Nether drops the shard");
        test.assertTrue(shards(never, killedBy(nether, piglin, byPlayer), netherShard, 50) == 0, "with chance 0 it never does");
        test.assertTrue(shards(always, killedBy(nether, piglin, nether.damageSources().generic()), netherShard, 5) == 0,
                "damage that is not a player's gives nothing");
        Zombie zombie = EntityType.ZOMBIE.create(nether);
        test.assertTrue(shards(always, killedBy(nether, piglin, nether.damageSources().mobAttack(zombie)), netherShard, 5) == 0,
                "nor does a mob's");
        var overworldPiglin = EntityType.ZOMBIFIED_PIGLIN.create(test.getLevel());
        overworldPiglin.moveTo(0.5, 70, 0.5);
        test.assertTrue(shards(always, killedBy(test.getLevel(), overworldPiglin, test.getLevel().damageSources().playerAttack(player)),
                netherShard, 5) == 0, "in the Overworld there is no Nether shard");
        finish(test, player);
    }

    /** The loot tables of the game got the pools: an Enderman in the End drops the Ender Shard about once in 32 kills. */
    public static void shardDropsAreAddedToTheEntityLootTables(GameTestHelper test) {
        MinecraftServer server = test.getLevel().getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        ServerLevel end = server.getLevel(Level.END);
        test.assertTrue(nether != null && end != null, "the Nether and the End exist");
        ServerPlayer player = player(test, 1.5, 1, 1.5);
        Item enderShard = TTRegistry.item(LibItemNames.KAMI_RESOURCE_NAMES[7]);
        Item netherShard = TTRegistry.item(LibItemNames.KAMI_RESOURCE_NAMES[6]);
        LootTable endermanTable = server.reloadableRegistries().getLootTable(DimensionalShardDropHandler.ENDERMAN);
        LootTable piglinTable = server.reloadableRegistries().getLootTable(DimensionalShardDropHandler.ZOMBIFIED_PIGLIN);

        var enderman = EntityType.ENDERMAN.create(end);
        enderman.moveTo(0.5, 70, 0.5);
        int rolls = 20000;
        int inTheEnd = shards(endermanTable, killedBy(end, enderman, end.damageSources().playerAttack(player)), enderShard, rolls);
        // One in 32: 625 expected, the standard deviation is 25.
        test.assertTrue(inTheEnd > 450 && inTheEnd < 800, "about 1/32 of " + rolls + " kills drop the Ender Shard: " + inTheEnd);
        var overworldEnderman = EntityType.ENDERMAN.create(test.getLevel());
        overworldEnderman.moveTo(0.5, 70, 0.5);
        test.assertTrue(shards(endermanTable, killedBy(test.getLevel(), overworldEnderman,
                test.getLevel().damageSources().playerAttack(player)), enderShard, 2000) == 0, "outside the End: none");

        var piglin = EntityType.ZOMBIFIED_PIGLIN.create(nether);
        piglin.moveTo(0.5, 70, 0.5);
        int inTheNether = shards(piglinTable, killedBy(nether, piglin, nether.damageSources().playerAttack(player)), netherShard, rolls);
        // One in 16: 1250 expected, the standard deviation is 34.
        test.assertTrue(inTheNether > 1000 && inTheNether < 1500, "about 1/16 of " + rolls + " kills drop the Nether Shard: " + inTheNether);
        test.assertTrue(shards(piglinTable, killedBy(nether, piglin, nether.damageSources().generic()), netherShard, 2000) == 0,
                "a kill that is not a player's: none");
        // The vanilla drops are still there.
        int flesh = 0;
        for (int i = 0; i < 200; i++) {
            for (ItemStack stack : piglinTable.getRandomItems(killedBy(nether, piglin, nether.damageSources().playerAttack(player)))) {
                if (stack.is(Items.ROTTEN_FLESH)) flesh += stack.getCount();
            }
        }
        test.assertTrue(flesh > 0, "the Piglin still drops rotten flesh");
        finish(test, player);
    }

    // ---- the command -----------------------------------------------------------------------------------------------

    private static int run(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher, String command,
            net.minecraft.commands.CommandSourceStack source) {
        try {
            return dispatcher.execute(command, source);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("/" + command + " failed: " + e.getMessage(), e);
        }
    }

    public static void kamiUnlockedCommandAnswersNoUntilEverythingIsResearched(GameTestHelper test) {
        MinecraftServer server = test.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        var node = dispatcher.getRoot().getChild("iskamiunlocked");
        test.assertTrue(node != null, "the command is registered");
        ServerPlayer player = player(test, 1.5, 1, 1.5);
        var source = player.createCommandSourceStack();
        test.assertTrue(node.canUse(source), "everyone may use it (permission level 0)");
        ResearchItem ichor = ResearchCategories.getResearch(LibResearch.KEY_ICHOR);
        test.assertTrue(ichor != null && ichor.parentsHidden != null && ichor.parentsHidden.length > 0, "the Kami entry requires research");
        test.assertTrue(run(dispatcher, "iskamiunlocked", source) == 0, "a new player has not unlocked it");
        for (String key : ichor.parentsHidden) ThaumcraftApiHelper.completeResearch(player, key);
        boolean all = ThaumcraftApiHelper.getCompletedResearch(player).containsAll(List.of(ichor.parentsHidden));
        test.assertTrue(run(dispatcher, "iskamiunlocked", source) == (all ? 1 : 0),
                "the command answers what the player's research says (all completed: " + all + ")");
        finish(test, player);
    }

    // ---- the whole group -------------------------------------------------------------------------------------------

    public static void groupHadNoRegistrationFailures(GameTestHelper test) {
        test.assertTrue(ThaumicTinkerer.failures().isEmpty(), "no registration failures: " + ThaumicTinkerer.failures());
        test.succeed();
    }
}
