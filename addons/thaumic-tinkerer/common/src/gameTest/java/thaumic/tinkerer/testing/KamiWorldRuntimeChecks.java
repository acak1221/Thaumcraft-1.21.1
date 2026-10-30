/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): runtime checks of the KamiWorld group, run by the Fabric GameTests. Distributed under the same licence as the add-on
 * (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.testing;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.EventResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
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
import thaumic.tinkerer.common.core.fakeplayer.TinkererFakePlayer;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.core.handler.kami.DimensionalShardDropHandler;
import thaumic.tinkerer.common.core.handler.kami.SoulHeartHandler;
import thaumic.tinkerer.common.dim.ChunkGeneratorBedrock;
import thaumic.tinkerer.common.dim.EnumOreFrequency;
import thaumic.tinkerer.common.dim.TeleporterBedrock;
import thaumic.tinkerer.common.dim.WorldProviderBedrock;
import thaumic.tinkerer.common.hooks.TinkererEvents;
import thaumic.tinkerer.common.item.kami.ItemSkyPearl;
import thaumic.tinkerer.common.item.kami.KamiHooks;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.network.packet.kami.PacketSoulHearts;
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

    /**
     * The dimension of a normal world and the level of the test world are the same dimension.
     *
     * <p>A normal (dedicated or integrated) server gets the level stems of the data packs through {@code WorldDimensions.bake}, so
     * {@code data/thaumictinkerer/dimension/bedrock.json} is all a world needs. The vanilla {@code GameTestServer} does not: it
     * bakes the dimensions of the flat world preset against an <b>empty</b> level stem registry, so a data-pack dimension has no
     * level in the test world unless the preset names it. That is why the test resources override
     * {@code data/minecraft/worldgen/world_preset/flat.json} with the three vanilla dimensions plus a copy of the Bedrock
     * dimension (the mod's own GameTests do the same for the Outer Lands). This check decodes the real file with the server's
     * registries (the codec path of the data pack loader), compares it to the copy in the preset, and to the running level.</p>
     */
    public static void bedrockDimensionFileDecodesAndIsTheLevelOfTheTestWorld(GameTestHelper test) {
        MinecraftServer server = test.getLevel().getServer();
        com.google.gson.JsonElement file = json(test, server, LibMisc.id("dimension/bedrock.json"));
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, server.registryAccess());
        net.minecraft.world.level.dimension.LevelStem stem = net.minecraft.world.level.dimension.LevelStem.CODEC.parse(ops, file)
                .getOrThrow(message -> new AssertionError("the dimension file does not decode: " + message));
        test.assertTrue(stem.type().is(LibMisc.id("bedrock")), "the dimension type of the file: " + stem.type());
        test.assertTrue(stem.generator() instanceof ChunkGeneratorBedrock, "the generator of the file: " + stem.generator());
        var biomes = stem.generator().getBiomeSource().possibleBiomes();
        test.assertTrue(biomes.size() == 1 && biomes.iterator().next().is(LibMisc.id("bedrock")), "one biome, the Bedrock biome: " + biomes);

        // The copy of the test world's preset is the same JSON (the preset is read from the active data packs, the test pack on top).
        com.google.gson.JsonElement preset = json(test, server, ResourceLocation.withDefaultNamespace("worldgen/world_preset/flat.json"));
        com.google.gson.JsonElement copy = preset.getAsJsonObject().getAsJsonObject("dimensions").get(WorldProviderBedrock.KEY.location().toString());
        test.assertTrue(file.equals(copy), "the preset of the test resources holds the dimension file as it is: " + copy);

        // And the running level is that dimension.
        ServerLevel level = bedrock(test);
        test.assertTrue(level.dimension() == WorldProviderBedrock.KEY, "the level key");
        test.assertTrue(level.dimensionTypeRegistration().is(LibMisc.id("bedrock")), "the level's dimension type");
        test.assertTrue(level.getChunkSource().getGenerator() instanceof ChunkGeneratorBedrock, "the level's generator");
        test.assertTrue(level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().stream().allMatch(b -> b.is(LibMisc.id("bedrock"))),
                "the level's biome");
        test.succeed();
    }

    private static com.google.gson.JsonElement json(GameTestHelper test, MinecraftServer server, ResourceLocation id) {
        var resource = server.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath()));
        test.assertTrue(resource.isPresent(), "the data pack holds " + id);
        try (java.io.Reader reader = resource.get().openAsReader()) {
            return com.google.gson.JsonParser.parseReader(reader);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

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

    /**
     * Runs {@code body}; when it throws (a failed assertion), runs {@code cleanup} first and rethrows. A failed GameTest does not
     * run the callbacks it still has waiting, so what a check must take away (a mock player, a pool, a listener) is taken away here.
     */
    private static void guarded(Runnable cleanup, Runnable body) {
        try {
            body.run();
        } catch (RuntimeException | Error failure) {
            cleanup.run();
            throw failure;
        }
    }

    /** The packets that the mock player's connection has been sent so far (the mock's channel is an {@code EmbeddedChannel}, which keeps them). */
    private static List<Object> sentPackets(ServerPlayer player) {
        try {
            java.lang.reflect.Field connection = net.minecraft.server.network.ServerCommonPacketListenerImpl.class.getDeclaredField("connection");
            connection.setAccessible(true);
            java.lang.reflect.Field channel = net.minecraft.network.Connection.class.getDeclaredField("channel");
            channel.setAccessible(true);
            io.netty.channel.embedded.EmbeddedChannel embedded = (io.netty.channel.embedded.EmbeddedChannel) channel.get(connection.get(player.connection));
            embedded.flush(); // the server thread writes without flushing (the real connections are flushed at the end of the tick)
            return new ArrayList<>(embedded.outboundMessages());
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException("cannot read the packets of the mock player's connection", e);
        }
    }

    /** How many damage events (the hurt animation and sound) the clients of {@code player} have been told about it. */
    private static long damageEventsOf(ServerPlayer player) {
        return sentPackets(player).stream().filter(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundDamageEventPacket event
                && event.entityId() == player.getId()).count();
    }

    /**
     * The pools that the client of {@code player} has been told so far ({@link PacketSoulHearts}, S2C), in the order they were sent.
     * Architectury sends the payload as its own wrapper of the encoded bytes ({@code NetworkAggregator.BufCustomPacketPayload}), which
     * the client decodes with the codec of the packet; so does this.
     */
    private static List<Integer> soulHeartPacketsOf(ServerPlayer player) {
        List<Integer> told = new ArrayList<>();
        for (Object packet : sentPackets(player)) {
            if (!(packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom)) continue;
            net.minecraft.network.protocol.common.custom.CustomPacketPayload payload = custom.payload();
            if (!payload.type().id().equals(PacketSoulHearts.TYPE.id())) continue;
            if (payload instanceof PacketSoulHearts hearts) {
                told.add(hearts.hearts());
            } else if (payload instanceof dev.architectury.impl.NetworkAggregator.BufCustomPacketPayload wrapped) {
                net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(
                        io.netty.buffer.Unpooled.wrappedBuffer(wrapped.payload()), player.registryAccess());
                told.add(PacketSoulHearts.CODEC.decode(buf).hearts());
            } else {
                throw new IllegalStateException("the soul hearts packet is sent as " + payload.getClass().getName());
            }
        }
        return told;
    }

    /**
     * The pool is filled by the sword's hook and spent before health when the player is hurt - by the real damage event
     * ({@code TinkererEvents.LIVING_DAMAGE}, posted by {@code LivingEntity.hurt}), with the exact rule of the original.
     *
     * <p>Set-up. A new {@code ServerPlayer} refuses every hit for 60 ticks ({@code spawnInvulnerableTime}, counted down by
     * {@code ServerPlayer.tick}, which the level calls for a mock player as for any), long before the damage event is posted:
     * a hit in the tick of the set-up changes neither health nor the pool. The hits therefore come after that time, and ten
     * ticks apart or more, because the pool keeps the invulnerability frames of the original
     * ({@link SoulHeartHandler#FRAME_TICKS}; {@link #soulHeartsKeepTheFramesOfTheOriginal} is about those).</p>
     *
     * <p>What it asserts: a hit the pool covers costs the pool and no health and the game refuses it (a cancel is a refusal of the
     * whole hit, HOOKS.md section 3: no frames), a hit it does not cover costs the pool and then the rest of the whole points,
     * and a player with no hearts still has the fraction of every hit cut (1.7.10 passed {@code (int) amount} for everybody).</p>
     */
    public static void soulHeartsAbsorbDamageBeforeHealth(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 2.5, 1, 2.5);
        // The mock player is flagged creative; the damage rules are the survival ones.
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        SoulHeartHandler.setHP(player, 0);
        player.setHealth(20F);
        test.assertTrue(SoulHeartHandler.getHP(player) == 0, "no hearts at first");
        Runnable cleanup = () -> {
            SoulHeartHandler.setHP(player, 0);
            player.discard();
        };

        // The hook of the Awakened Sword's Soul Mode: one point per call, twenty at most.
        KamiHooks.soulHeartSink.accept(player);
        test.assertTrue(SoulHeartHandler.getHP(player) == 1, "one point");
        for (int i = 0; i < 30; i++) KamiHooks.soulHeartSink.accept(player);
        test.assertTrue(SoulHeartHandler.getHP(player) == SoulHeartHandler.MAX_HP, "twenty points at most");
        test.assertTrue(!SoulHeartHandler.addHP(player, 1), "a full pool takes no more");

        // Spawn protection first (see above): 60 ticks, the other checks of the add-on wait 65.
        test.runAfterDelay(65, () -> guarded(cleanup, () -> {
            player.setHealth(20F);
            // A hit the pool covers costs no health.
            SoulHeartHandler.setHP(player, 5);
            DamageSource generic = test.getLevel().damageSources().generic();
            test.assertTrue(!player.hurt(generic, 3F), "the covered hit is cancelled by the event: the game refuses it");
            test.assertTrue(player.getHealth() == 20F, "a hit of 3 against 5 soul hearts costs no health: " + player.getHealth());
            test.assertTrue(SoulHeartHandler.getHP(player) == 2, "it costs 3 soul hearts: " + SoulHeartHandler.getHP(player));
            test.assertTrue(player.invulnerableTime == 0, "and starts none of the game's invulnerability frames (HOOKS.md section 3): " + player.invulnerableTime);

            test.runAfterDelay(SoulHeartHandler.FRAME_TICKS + 1, () -> guarded(cleanup, () -> {
                // A hit it does not cover costs the rest.
                test.assertTrue(player.hurt(generic, 4F), "a hit with something left is dealt");
                test.assertTrue(SoulHeartHandler.getHP(player) == 0, "the pool is empty: " + SoulHeartHandler.getHP(player));
                test.assertTrue(player.getHealth() == 18F, "2 points of the hit of 4 went on: " + player.getHealth());

                test.runAfterDelay(SoulHeartHandler.FRAME_TICKS + 1, () -> guarded(cleanup, () -> {
                    // No hearts: the exact rule of the original cuts the fraction of the hit all the same (1.7.10: (int) 1.5 = 1).
                    test.assertTrue(player.hurt(generic, 1.5F), "a hit of 1.5 with no hearts is dealt");
                    test.assertTrue(player.getHealth() == 17F, "the fraction of the hit is cut: " + player.getHealth());
                    cleanup.run();
                    test.succeed();
                }));
            }));
        }));
    }

    /**
     * The invulnerability frames of the original through the real event, hit by hit (the tick of every hit is relative to the first,
     * {@code b}; the player has soul hearts or not as each step says). 1.7.10 set {@code hurtResistantTime} before the event, so
     * a hit that the pool swallowed still began the frames: a weaker or equal hit inside them was refused before the event
     * (no event, no pool), a stronger one reached the event as {@code amount - lastDamage}, and the hit ten ticks after the first
     * was a new one. Here a cancel starts no frames of the game's, so {@link SoulHeartHandler} keeps them (HOOKS.md section 3).
     *
     * <p>Also: the clients are told of a covered hit that began the frames (the hurt animation and sound), and of no other
     * covered hit; the pool of another player and the hurt of a pig are not touched by the frames of the first player.</p>
     */
    public static void soulHeartsKeepTheFramesOfTheOriginal(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 2.5, 1, 2.5);
        ServerPlayer other = player(test, 5.5, 1, 5.5);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        other.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.animal.Pig pig = test.spawn(EntityType.PIG, new Vec3(5.5, 1, 1.5));
        pig.setNoAi(true);
        float pigHealth = pig.getHealth();
        Runnable cleanup = () -> {
            SoulHeartHandler.setHP(player, 0);
            SoulHeartHandler.setHP(other, 0);
            player.discard();
            other.discard();
            pig.discard();
        };
        SoulHeartHandler.setHP(player, 20);
        SoulHeartHandler.setHP(other, 7);
        player.setHealth(20F);
        other.setHealth(20F);
        DamageSource generic = test.getLevel().damageSources().generic();

        test.runAfterDelay(65, () -> guarded(cleanup, () -> {
            long b = test.getTick();
            java.util.function.BiConsumer<Long, Runnable> at = (offset, body) -> test.runAtTickTime(b + offset, () -> guarded(cleanup, body));
            long events0 = damageEventsOf(player);
            // the pools the clients are told (PacketSoulHearts), from now on: one after every hit that the pool charged, none for a refused hit
            int told0 = soulHeartPacketsOf(player).size();
            int otherTold0 = soulHeartPacketsOf(other).size();
            java.util.function.Supplier<List<Integer>> told = () -> {
                List<Integer> all = soulHeartPacketsOf(player);
                return all.subList(told0, all.size());
            };

            // b: 4 against a full pool: covered, the first hit of the frames
            test.assertTrue(!player.hurt(generic, 4F) && SoulHeartHandler.getHP(player) == 16 && player.getHealth() == 20F, "b: a covered hit of 4: pool 16, health 20");
            test.assertTrue(damageEventsOf(player) == events0 + 1, "b: and the player is told of it (the hurt animation and sound): " + (damageEventsOf(player) - events0));
            test.assertTrue(told.get().equals(List.of(16)), "b: and of the pool (1.7.10 updateClient after the event): " + told.get());

            // another player and a pig are not in these frames
            at.accept(1L, () -> {
                test.assertTrue(!player.hurt(generic, 4F) && SoulHeartHandler.getHP(player) == 16, "b+1: the repeat of the same hit is refused as the original refused it: no pool");
                test.assertTrue(!other.hurt(generic, 4F) && SoulHeartHandler.getHP(other) == 3 && other.getHealth() == 20F,
                        "b+1: another player's pool is charged on its own: " + SoulHeartHandler.getHP(other));
                test.assertTrue(pig.hurt(generic, 3.5F) && pig.getHealth() == pigHealth - 3.5F,
                        "b+1: a pig is not the pool's: it takes the whole 3.5: " + pig.getHealth());
                test.assertTrue(damageEventsOf(player) == events0 + 1, "b+1: a refused repeat is not announced again");
                test.assertTrue(told.get().equals(List.of(16)), "b+1: nor is the pool sent again for it: " + told.get());
                List<Integer> otherAll = soulHeartPacketsOf(other);
                test.assertTrue(otherAll.subList(otherTold0, otherAll.size()).equals(List.of(3)),
                        "b+1: another player is told her own pool: " + otherAll.subList(otherTold0, otherAll.size()));
            });
            at.accept(3L, () -> {
                test.assertTrue(!player.hurt(generic, 3F) && SoulHeartHandler.getHP(player) == 16, "b+3: a weaker hit is refused: no pool");
                test.assertTrue(told.get().equals(List.of(16)), "b+3: and nothing is sent: " + told.get());
            });
            at.accept(5L, () -> {
                // 6 against the last 4: the event gets the difference, 2
                test.assertTrue(!player.hurt(generic, 6F) && SoulHeartHandler.getHP(player) == 14 && player.getHealth() == 20F,
                        "b+5: a stronger hit costs the difference (2): " + SoulHeartHandler.getHP(player));
                test.assertTrue(damageEventsOf(player) == events0 + 1, "b+5: a stronger hit inside the frames is silent (1.7.10: no hurt animation there)");
                test.assertTrue(told.get().equals(List.of(16, 14)), "b+5: but the new pool is sent: " + told.get());
            });
            at.accept(6L, () -> {
                test.assertTrue(!player.hurt(generic, 6F) && SoulHeartHandler.getHP(player) == 14, "b+6: and the same 6 again is refused");
                test.assertTrue(told.get().equals(List.of(16, 14)), "b+6: nothing is sent: " + told.get());
            });
            at.accept(9L, () -> {
                test.assertTrue(!player.hurt(generic, 5F) && SoulHeartHandler.getHP(player) == 14, "b+9: the last tick of the frames: still refused");
                test.assertTrue(told.get().equals(List.of(16, 14)), "b+9: nothing is sent: " + told.get());
            });
            at.accept(10L, () -> {
                // ten ticks after the first hit: a new hit, charged whole
                test.assertTrue(!player.hurt(generic, 4F) && SoulHeartHandler.getHP(player) == 10, "b+10: a new hit, charged whole: " + SoulHeartHandler.getHP(player));
                test.assertTrue(damageEventsOf(player) == events0 + 2, "b+10: it began new frames and is announced");
                test.assertTrue(told.get().equals(List.of(16, 14, 10)), "b+10: and the pool is sent: " + told.get());
            });
            at.accept(11L, () -> {
                test.assertTrue(!player.hurt(generic, 4F) && SoulHeartHandler.getHP(player) == 10, "b+11: refused again");
                test.assertTrue(told.get().equals(List.of(16, 14, 10)), "b+11: nothing is sent: " + told.get());
            });

            // a pool that does not cover the hit: 4 against 2 leaves 2, which the game deals and begins its own frames with
            at.accept(20L, () -> {
                SoulHeartHandler.setHP(player, 2);
                test.assertTrue(player.hurt(generic, 4F), "b+20: the hit with something left is dealt");
                test.assertTrue(SoulHeartHandler.getHP(player) == 0 && player.getHealth() == 18F, "b+20: the pool is gone and 2 went on: " + player.getHealth());
                test.assertTrue(player.invulnerableTime == 20, "b+20: the game's frames began: " + player.invulnerableTime);
                test.assertTrue(told.get().equals(List.of(16, 14, 10, 0)), "b+20: the pool that is left is sent: " + told.get());
            });
            at.accept(21L, () -> {
                test.assertTrue(!player.hurt(generic, 4F) && player.getHealth() == 18F && SoulHeartHandler.getHP(player) == 0,
                        "b+21: the repeat is refused (the game would let it by: 4 is more than the 2 it dealt)");
                test.assertTrue(told.get().equals(List.of(16, 14, 10, 0)), "b+21: nothing is sent: " + told.get());
            });
            at.accept(22L, () -> {
                // 6 against the last 4: the event gets 2, the pool is empty, 2 go on; the game has dealt 2 already, so it is handed 4
                test.assertTrue(player.hurt(generic, 6F), "b+22: a stronger hit is dealt");
                test.assertTrue(player.getHealth() == 16F, "b+22: the difference of 2 went on (the game was handed 2 already dealt + rest 2): " + player.getHealth());
                test.assertTrue(told.get().equals(List.of(16, 14, 10, 0, 0)), "b+22: an empty pool is sent too, as the original did after every event: " + told.get());
            });
            at.accept(23L, () -> {
                test.assertTrue(!player.hurt(generic, 6F) && player.getHealth() == 16F, "b+23: the same 6 again is refused");
                test.assertTrue(told.get().equals(List.of(16, 14, 10, 0, 0)), "b+23: nothing is sent: " + told.get());
            });
            at.accept(30L, () -> {
                // a new hit with no hearts: the fraction is cut like 1.7.10's (int) 5.9
                test.assertTrue(player.hurt(generic, 5.9F) && player.getHealth() == 11F, "b+30: 5.9 deals 5: " + player.getHealth());
                test.assertTrue(told.get().equals(List.of(16, 14, 10, 0, 0, 0)), "b+30: the pool is sent: " + told.get());
            });
            at.accept(40L, () -> {
                // a hit below one point is gone altogether, even with no hearts
                test.assertTrue(!player.hurt(generic, 0.5F) && player.getHealth() == 11F, "b+40: 0.5 is swallowed: " + player.getHealth());
                test.assertTrue(told.get().equals(List.of(16, 14, 10, 0, 0, 0, 0)), "b+40: and the pool is sent for it (0 whole points taken): " + told.get());
                cleanup.run();
                test.succeed();
            });
        }));
    }

    /**
     * What the game is handed when its own invulnerability frames and the pool's are not the same: the game starts its frames when it
     * deals a hit and the pool starts its own at every hit it charges whole, so a hit that the pool covers begins the pool's frames and
     * not the game's, and the two end at different times. The game turns the amount it is handed into damage by its own rule (inside its
     * frames {@code handed - lastHurt}, outside them {@code handed}), so the pool must ask the game's frames
     * ({@code invulnerableTime}), not its own, whether the damage already dealt is to be added. The sequence is that of the
     * original, hit by hit (pool 5, generic damage, ticks relative to the first hit {@code b}); in 1.7.10 the frames began at {@code b}
     * and ended at {@code b + 10}, and the hit at {@code b + 10} was a new one whose frames last to {@code b + 20}:
     * <ul>
     *   <li>{@code b}: 3 is covered (pool 2), the pool's frames begin, the game's do not;</li>
     *   <li>{@code b + 3}: 6 is stronger than 3, the event gets 3, the pool has 2 left: 1 goes on (health 19) and the game's frames
     *       begin here, three ticks late;</li>
     *   <li>{@code b + 10}: 3 is a new hit for the original, the pool is empty: 3 goes on (health 16). The pool's frames are over
     *       and the game's still run (they end at {@code b + 13}), with {@code lastHurt} 1: it has to be handed 1 + 3, or it would deal 2;</li>
     *   <li>{@code b + 14}: 5 is stronger than the 3 of the frames of {@code b + 10}: the event gets 2, 2 goes on (health 14). The pool's
     *       frames still run and the game's are over: it has to be handed 2, not 3 + 2, or it would deal all 5 (health 12).</li>
     * </ul>
     */
    public static void soulHeartsAgreeWithTheOriginalWhenTheGamesFramesBeginLater(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 2.5, 1, 2.5);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        Runnable cleanup = () -> {
            SoulHeartHandler.setHP(player, 0);
            player.discard();
        };
        SoulHeartHandler.setHP(player, 5);
        player.setHealth(20F);
        DamageSource generic = test.getLevel().damageSources().generic();

        test.runAfterDelay(65, () -> guarded(cleanup, () -> {
            long b = test.getTick();
            java.util.function.BiConsumer<Long, Runnable> at = (offset, body) -> test.runAtTickTime(b + offset, () -> guarded(cleanup, body));

            test.assertTrue(!player.hurt(generic, 3F), "b: 3 against 5 is covered: the hit is refused");
            test.assertTrue(SoulHeartHandler.getHP(player) == 2 && player.getHealth() == 20F && player.invulnerableTime == 0,
                    "b: pool 2, health 20, no frames of the game's: " + SoulHeartHandler.getHP(player) + " " + player.getHealth() + " " + player.invulnerableTime);

            at.accept(3L, () -> {
                test.assertTrue(player.hurt(generic, 6F), "b+3: 6 is stronger than 3 and has something left: it is dealt");
                test.assertTrue(SoulHeartHandler.getHP(player) == 0 && player.getHealth() == 19F,
                        "b+3: the event got 3, the pool had 2: 1 went on: pool " + SoulHeartHandler.getHP(player) + ", health " + player.getHealth());
                test.assertTrue(player.invulnerableTime == 20, "b+3: the game's frames begin here, three ticks after the pool's: " + player.invulnerableTime);
            });
            at.accept(10L, () -> {
                test.assertTrue(player.invulnerableTime > 10, "b+10: the game's frames are still running (the case this check is about): " + player.invulnerableTime);
                test.assertTrue(player.hurt(generic, 3F), "b+10: a new hit for the original (the pool's frames are over): dealt");
                test.assertTrue(player.getHealth() == 16F, "b+10: all of its 3 went on, the game had dealt 1 in its frames: " + player.getHealth());
            });
            at.accept(14L, () -> {
                test.assertTrue(player.invulnerableTime <= 10, "b+14: the game's frames are over, the pool's (from b+10) still run (the case this check is about): "
                        + player.invulnerableTime);
                test.assertTrue(player.hurt(generic, 5F), "b+14: 5 is stronger than the 3 of the frames of b+10: dealt");
                test.assertTrue(player.getHealth() == 14F, "b+14: the difference of 2 went on, not all 5: " + player.getHealth());
            });
            at.accept(15L, () -> test.assertTrue(!player.hurt(generic, 5F) && player.getHealth() == 14F, "b+15: the same 5 again is refused by the frames"));
            at.accept(24L, () -> {
                // all the frames are over now: a new hit of 4 is dealt whole
                test.assertTrue(player.hurt(generic, 4F) && player.getHealth() == 10F, "b+24: a new hit, dealt whole: " + player.getHealth());
                cleanup.run();
                test.succeed();
            });
        }));
    }

    /**
     * A source that hits every tick costs the pool once per ten ticks, not once per tick (the original set {@code hurtResistantTime}
     * before the event). Two mock players stand in a pit of lava, each in a pit of their own, and are ticked every tick
     * ({@code doTick}, so {@code Entity.baseTick} burns them with the lava's 4 points every tick): the one with a full pool pays 4
     * points every ten ticks and loses no health; the one whose pool (3) does not cover the first hit loses what is left of it (1), then
     * 4 every ten ticks. Without the frames the pool of the first would be gone in five ticks.
     */
    public static void soulHeartsAreChargedOncePerTenTicksByLava(GameTestHelper test) {
        floor(test);
        ServerPlayer covered = lavaPlayer(test, 2);
        ServerPlayer partly = lavaPlayer(test, 5);
        covered.getFoodData().setFoodLevel(10); // no natural regeneration, no starvation: the health changes are the lava's
        partly.getFoodData().setFoodLevel(10);
        SoulHeartHandler.setHP(covered, 20);
        SoulHeartHandler.setHP(partly, 3);
        covered.setHealth(20F);
        partly.setHealth(20F);
        Runnable cleanup = () -> {
            SoulHeartHandler.setHP(covered, 0);
            SoulHeartHandler.setHP(partly, 0);
            covered.discard();
            partly.discard();
        };
        List<Long> coveredSpent = new ArrayList<>();
        List<Long> partlyHurt = new ArrayList<>();
        int[] coveredPool = { 20 };
        float[] partlyHealth = { 20F };
        List<Float> partlyLoss = new ArrayList<>();
        // from tick 0: the first hit that is not refused as spawn protection comes at about tick 61
        test.onEachTick(() -> guarded(cleanup, () -> {
            covered.doTick();
            partly.doTick();
            long tick = test.getTick();
            int pool = SoulHeartHandler.getHP(covered);
            if (pool != coveredPool[0]) {
                test.assertTrue(coveredPool[0] - pool == 4, "tick " + tick + ": the lava takes 4 points a hit: " + coveredPool[0] + " -> " + pool);
                coveredSpent.add(tick);
                coveredPool[0] = pool;
            }
            float health = partly.getHealth();
            if (health != partlyHealth[0]) {
                partlyHurt.add(tick);
                partlyLoss.add(partlyHealth[0] - health);
                partlyHealth[0] = health;
            }
        }));
        test.runAtTickTime(98, () -> guarded(cleanup, () -> {
            test.assertTrue(coveredSpent.size() == 4, "four hits in the 37 ticks after the spawn protection, not one per tick: " + coveredSpent);
            for (int i = 1; i < coveredSpent.size(); i++) {
                test.assertTrue(coveredSpent.get(i) - coveredSpent.get(i - 1) == SoulHeartHandler.FRAME_TICKS,
                        "the pool is charged every " + SoulHeartHandler.FRAME_TICKS + " ticks: " + coveredSpent);
            }
            test.assertTrue(SoulHeartHandler.getHP(covered) == 4, "20 - 4 x 4 points are left: " + SoulHeartHandler.getHP(covered));
            test.assertTrue(covered.getHealth() == 20F, "and all of it was covered: " + covered.getHealth());

            test.assertTrue(partlyHurt.size() == 4, "the player whose pool ran out is hurt four times: " + partlyHurt);
            for (int i = 1; i < partlyHurt.size(); i++) {
                test.assertTrue(partlyHurt.get(i) - partlyHurt.get(i - 1) == SoulHeartHandler.FRAME_TICKS,
                        "and every " + SoulHeartHandler.FRAME_TICKS + " ticks: " + partlyHurt);
            }
            test.assertTrue(partlyLoss.equals(List.of(1F, 4F, 4F, 4F)),
                    "the first hit loses what the pool could not cover (4 - 3 = 1), the others 4: " + partlyLoss);
            test.assertTrue(SoulHeartHandler.getHP(partly) == 0, "the pool of 3 is gone");
            cleanup.run();
            test.succeed();
        }));
    }

    /** A survival mock player standing in a pit of one lava source (stone round it, so the lava neither flows nor lets the player wander). */
    private static ServerPlayer lavaPlayer(GameTestHelper test, int x) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                for (int y = 1; y <= 2; y++) test.setBlock(new BlockPos(x + dx, y, 3 + dz), Blocks.STONE);
            }
        }
        test.setBlock(new BlockPos(x, 1, 3), Blocks.LAVA);
        ServerPlayer player = player(test, x + 0.5, 1, 3.5);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
    }

    /**
     * The rule of the original on its seam ({@link SoulHeartHandler#modifyIncomingDamage}, what the loader hook calls for every
     * hit of a server player): the whole points of the hit come off the pool, the rest is a whole number, a hit below one point
     * is swallowed even with no hearts, and the pool is saved. A hit that is not positive is not touched.
     */
    public static void soulHeartsExactRuleCutsTheFractionOfEveryHit(GameTestHelper test) {
        ServerPlayer player = player(test, 2.5, 1, 2.5);
        try {
            SoulHeartHandler.setHP(player, 5);
            test.assertTrue(SoulHeartHandler.modifyIncomingDamage(player, 3.9F) == 0F, "3.9 against 5 hearts: nothing goes on");
            test.assertTrue(SoulHeartHandler.getHP(player) == 2, "it takes the 3 whole points: " + SoulHeartHandler.getHP(player));
            test.assertTrue(SoulHeartHandler.modifyIncomingDamage(player, 4.9F) == 2F, "4.9 against 2 hearts: 4 points, 2 go on, a whole number");
            test.assertTrue(SoulHeartHandler.getHP(player) == 0, "the pool is empty");
            test.assertTrue(SoulHeartHandler.modifyIncomingDamage(player, 1.5F) == 1F, "no hearts: the fraction of the hit is cut (1.7.10 int amount)");
            test.assertTrue(SoulHeartHandler.modifyIncomingDamage(player, 0.5F) == 0F, "and a hit below one point is gone");
            test.assertTrue(SoulHeartHandler.getHP(player) == 0, "the pool does not go below 0");
            test.assertTrue(SoulHeartHandler.modifyIncomingDamage(player, 0F) == 0F && SoulHeartHandler.modifyIncomingDamage(player, -2F) == -2F,
                    "a hit that is not positive is returned as it is");
            SoulHeartHandler.setHP(player, 20);
            test.assertTrue(SoulHeartHandler.modifyIncomingDamage(player, 100F) == 80F, "a big hit: 20 points absorbed, 80 go on");
            test.assertTrue(SoulHeartHandler.getHP(player) == 0, "the whole pool is gone");
        } finally {
            SoulHeartHandler.setHP(player, 0);
        }
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

    /**
     * Kills {@code victim} for real ({@code LivingEntity.die}: the death loot, the event, the drops) by {@code source} and returns the
     * item entities that the listeners left in the list of drops, as a listener registered after the group's own sees them. With
     * {@code keep} false that listener cancels the drops, so nothing of the death enters the world.
     */
    private static List<ItemEntity> die(LivingEntity victim, DamageSource source, boolean keep) {
        List<ItemEntity> seen = new ArrayList<>();
        TinkererEvents.LivingDrops probe = (who, by, drops, looting, recentlyHit) -> {
            if (who != victim) return EventResult.pass();
            seen.addAll(drops);
            return keep ? EventResult.pass() : EventResult.interruptFalse();
        };
        TinkererEvents.LIVING_DROPS.register(probe);
        try {
            victim.die(source);
        } finally {
            TinkererEvents.LIVING_DROPS.unregister(probe);
        }
        return seen;
    }

    /**
     * Whether the level has accepted this entity ({@code PersistentEntitySectionManager.isLoaded(uuid)}: its UUID is known). The usual
     * lookup ({@code ServerLevel.getEntity}) only finds entities of chunks that are visible, which the Nether and the End of the
     * test world are not.
     */
    private static boolean isInTheLevel(ServerLevel level, ItemEntity item) {
        try {
            java.lang.reflect.Field field = ServerLevel.class.getDeclaredField("entityManager");
            field.setAccessible(true);
            return ((net.minecraft.world.level.entity.PersistentEntitySectionManager<?>) field.get(level)).isLoaded(item.getUUID()) && !item.isRemoved();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot ask the level for its entities", e);
        }
    }

    private static int count(List<ItemEntity> drops, Item item) {
        int count = 0;
        for (ItemEntity drop : drops) if (drop.getItem().is(item)) count += drop.getItem().getCount();
        return count;
    }

    private static <T extends LivingEntity> T mobAt(EntityType<T> type, ServerLevel level) {
        T mob = type.create(level);
        mob.moveTo(0.5, 70, 0.5, 0F, 0F);
        return mob;
    }

    /**
     * The conditions of the original handler through the real event: the mob dies, {@code LivingEntity.die} posts
     * {@code LIVING_DROPS}, and the shard is in the list of drops exactly when a player
     * dealt the blow to an Enderman in the End or a Zombified Piglin in the Nether and the dice allow it (the dice are
     * made certain here; {@link #shardDropsComeFromRealDeathsWithTheChanceOfTheOriginal} uses the real ones), and the level takes the
     * shard when the drops are kept and never sees it when they are cancelled. Everything else is
     * left alone: the other mob, the other dimension, a blow that was not a player's.
     */
    public static void shardDropsFollowTheConditionsOfTheOriginal(GameTestHelper test) {
        MinecraftServer server = test.getLevel().getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        ServerLevel end = server.getLevel(Level.END);
        test.assertTrue(nether != null && end != null, "the Nether and the End exist");
        test.assertTrue(TinkererEvents.hasLivingDropsListeners(), "the group listens to the drops of the dying (LIVING_DROPS)");
        ServerPlayer player = player(test, 1.5, 1, 1.5);
        TinkererFakePlayer fake = new TinkererFakePlayer(end, new GameProfile(UUID.randomUUID(), "[TTKamiTest]"));
        Item enderShard = TTRegistry.item(LibItemNames.KAMI_RESOURCE_NAMES[7]);
        Item netherShard = TTRegistry.item(LibItemNames.KAMI_RESOURCE_NAMES[6]);
        java.util.function.DoubleSupplier realDice = DimensionalShardDropHandler.dice;
        List<ItemEntity> everything = new ArrayList<>();
        // The same on the way out and on a failure: the mock player would stay in the world (the harness does not remove players with
        // the structure) and pick up what later checks drop.
        Runnable cleanup = () -> {
            DimensionalShardDropHandler.dice = realDice;
            everything.forEach(ItemEntity::discard);
            fake.discard();
            player.discard();
        };
        guarded(cleanup, () -> {
            DamageSource blow = end.damageSources().playerAttack(player);
            DimensionalShardDropHandler.dice = () -> 0D; // every roll succeeds

            // an Enderman in the End killed by a player: one Ender Shard, at its position
            EnderMan enderman = mobAt(EntityType.ENDERMAN, end);
            List<ItemEntity> drops = die(enderman, blow, true);
            everything.addAll(drops);
            test.assertTrue(count(drops, enderShard) == 1 && count(drops, netherShard) == 0, "an Enderman in the End: one Ender Shard: " + drops);
            ItemEntity shard = drops.stream().filter(drop -> drop.getItem().is(enderShard)).findFirst().orElseThrow();
            test.assertTrue(shard.getX() == 0.5 && shard.getY() == 70 && shard.getZ() == 0.5 && shard.getItem().getCount() == 1,
                    "a single shard at the mob's position: " + shard);

            // the same with the roll at the edge of the chance: 1.7.10 Math.random() <= 1D / 32D
            DimensionalShardDropHandler.dice = () -> 1D / 32D;
            List<ItemEntity> edge = die(mobAt(EntityType.ENDERMAN, end), blow, true);
            everything.addAll(edge);
            test.assertTrue(count(edge, enderShard) == 1, "a roll of exactly 1/32 drops (<=)");
            DimensionalShardDropHandler.dice = () -> Math.nextUp(1D / 32D);
            List<ItemEntity> above = die(mobAt(EntityType.ENDERMAN, end), blow, true);
            everything.addAll(above);
            test.assertTrue(count(above, enderShard) == 0, "a roll above 1/32 does not");
            DimensionalShardDropHandler.dice = () -> 1D / 16D;
            List<ItemEntity> pigmanEdge = die(mobAt(EntityType.ZOMBIFIED_PIGLIN, nether), nether.damageSources().playerAttack(player), true);
            everything.addAll(pigmanEdge);
            test.assertTrue(count(pigmanEdge, netherShard) == 1, "a roll of exactly 1/16 drops the Nether Shard");
            DimensionalShardDropHandler.dice = () -> 0D;

            // a Zombified Piglin in the Nether: the Nether Shard, and not the other one
            List<ItemEntity> piglin = die(mobAt(EntityType.ZOMBIFIED_PIGLIN, nether), nether.damageSources().playerAttack(player), true);
            everything.addAll(piglin);
            test.assertTrue(count(piglin, netherShard) == 1 && count(piglin, enderShard) == 0, "a Zombified Piglin in the Nether: one Nether Shard: " + piglin);

            // the shard really reaches the level when the drops are kept (the poster adds what the listeners left), and not when
            // a later listener cancels them (a listener of the check registered after the group's, as another group's could be)
            List<ItemEntity> kept = die(mobAt(EntityType.ZOMBIFIED_PIGLIN, nether), nether.damageSources().playerAttack(player), true);
            everything.addAll(kept);
            ItemEntity keptShard = kept.stream().filter(drop -> drop.getItem().is(netherShard)).findFirst().orElseThrow();
            test.assertTrue(isInTheLevel(nether, keptShard), "the Nether Shard that the listener added is in the level: " + kept);
            List<ItemEntity> refused = die(mobAt(EntityType.ZOMBIFIED_PIGLIN, nether), nether.damageSources().playerAttack(player), false);
            everything.addAll(refused);
            ItemEntity refusedShard = refused.stream().filter(drop -> drop.getItem().is(netherShard)).findFirst().orElseThrow();
            test.assertTrue(!isInTheLevel(nether, refusedShard), "but the one of a cancelled death never is: " + refused);

            // what is not the original's: the wrong dimension, the wrong mob, a blow that is not a player's
            List<ItemEntity> wrong = new ArrayList<>();
            wrong.addAll(die(mobAt(EntityType.ENDERMAN, test.getLevel()), test.getLevel().damageSources().playerAttack(player), true));
            wrong.addAll(die(mobAt(EntityType.ENDERMAN, nether), nether.damageSources().playerAttack(player), true));
            wrong.addAll(die(mobAt(EntityType.ZOMBIFIED_PIGLIN, test.getLevel()), test.getLevel().damageSources().playerAttack(player), true));
            wrong.addAll(die(mobAt(EntityType.ZOMBIFIED_PIGLIN, end), end.damageSources().playerAttack(player), true));
            wrong.addAll(die(mobAt(EntityType.ZOMBIE, nether), nether.damageSources().playerAttack(player), true));
            wrong.addAll(die(mobAt(EntityType.COW, end), end.damageSources().playerAttack(player), true));
            everything.addAll(wrong);
            test.assertTrue(count(wrong, enderShard) == 0 && count(wrong, netherShard) == 0,
                    "another dimension or another mob drops no shard, whatever the dice: " + wrong);
            Zombie zombie = mobAt(EntityType.ZOMBIE, nether);
            List<ItemEntity> notPlayers = new ArrayList<>();
            notPlayers.addAll(die(mobAt(EntityType.ENDERMAN, end), end.damageSources().generic(), true));
            notPlayers.addAll(die(mobAt(EntityType.ENDERMAN, end), end.damageSources().mobAttack(zombie), true));
            notPlayers.addAll(die(mobAt(EntityType.ZOMBIFIED_PIGLIN, nether), nether.damageSources().generic(), true));
            notPlayers.addAll(die(mobAt(EntityType.ZOMBIFIED_PIGLIN, nether), nether.damageSources().mobAttack(zombie), true));
            everything.addAll(notPlayers);
            test.assertTrue(count(notPlayers, enderShard) == 0 && count(notPlayers, netherShard) == 0,
                    "damage that is not a player's gives nothing: " + notPlayers);

            // the attacker of the blow, not the direct cause: a player behind an arrow or a potion counts (indirect), as getEntity() did
            List<ItemEntity> indirect = die(mobAt(EntityType.ENDERMAN, end), end.damageSources().indirectMagic(zombie, player), true);
            everything.addAll(indirect);
            test.assertTrue(count(indirect, enderShard) == 1, "a player behind the blow counts: " + indirect);
            // the Animation Tablet's fake player is a player too (1.7.10: a FakePlayer is an EntityPlayer)
            List<ItemEntity> byFake = die(mobAt(EntityType.ENDERMAN, end), end.damageSources().playerAttack(fake), true);
            everything.addAll(byFake);
            test.assertTrue(count(byFake, enderShard) == 1, "the fake player counts: " + byFake);
            // instanceof: a subclass of the Enderman is an Enderman
            EnderMan subclass = new EnderMan(EntityType.ENDERMAN, end) { };
            subclass.moveTo(0.5, 70, 0.5, 0F, 0F);
            List<ItemEntity> bySubclass = die(subclass, blow, true);
            everything.addAll(bySubclass);
            test.assertTrue(count(bySubclass, enderShard) == 1, "a subclass of the Enderman drops it too (instanceof): " + bySubclass);
        });

        cleanup.run();
        test.assertTrue(player.isRemoved() && fake.isRemoved(), "the mock player of the check does not stay in the world");
        test.succeed();
    }

    /**
     * Real deaths with the real dice: 6400 Endermen killed by a player in the End drop the Ender Shard about once in 32 (200
     * expected, a standard deviation of 14), 3200 Zombified Piglins in the Nether the Nether Shard about once in 16 (200, 14), the
     * same Piglins killed by plain damage none, and the Piglin's own drops are still in the list. The drops are cancelled by a listener
     * of the check after the group's (the bounds are five standard deviations wide), so nothing enters the world.
     */
    public static void shardDropsComeFromRealDeathsWithTheChanceOfTheOriginal(GameTestHelper test) {
        MinecraftServer server = test.getLevel().getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        ServerLevel end = server.getLevel(Level.END);
        test.assertTrue(nether != null && end != null, "the Nether and the End exist");
        ServerPlayer player = player(test, 1.5, 1, 1.5);
        Item enderShard = TTRegistry.item(LibItemNames.KAMI_RESOURCE_NAMES[7]);
        Item netherShard = TTRegistry.item(LibItemNames.KAMI_RESOURCE_NAMES[6]);
        try {
            int ender = 0;
            for (int i = 0; i < 6400; i++) ender += count(die(mobAt(EntityType.ENDERMAN, end), end.damageSources().playerAttack(player), false), enderShard);
            test.assertTrue(ender >= 130 && ender <= 270, "about 1/32 of 6400 Endermen drop the Ender Shard (200 expected): " + ender);

            int shards = 0;
            int flesh = 0;
            int wrongShard = 0;
            for (int i = 0; i < 3200; i++) {
                List<ItemEntity> drops = die(mobAt(EntityType.ZOMBIFIED_PIGLIN, nether), nether.damageSources().playerAttack(player), false);
                shards += count(drops, netherShard);
                wrongShard += count(drops, enderShard);
                flesh += count(drops, Items.ROTTEN_FLESH);
            }
            test.assertTrue(shards >= 130 && shards <= 270, "about 1/16 of 3200 Piglins drop the Nether Shard (200 expected): " + shards);
            test.assertTrue(wrongShard == 0, "and never the Ender Shard");
            test.assertTrue(flesh > 0, "the Piglins' own drops are still in the list: rotten flesh " + flesh);

            int byPlainDamage = 0;
            for (int i = 0; i < 3200; i++) byPlainDamage += count(die(mobAt(EntityType.ZOMBIFIED_PIGLIN, nether), nether.damageSources().generic(), false), netherShard);
            test.assertTrue(byPlainDamage == 0, "a death that is not a player's gives no shard: " + byPlainDamage);
        } finally {
            player.discard();
        }
        test.succeed();
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
