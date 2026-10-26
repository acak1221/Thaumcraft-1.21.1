package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockCosmeticSolid;
import thaumcraft.common.blocks.BlockEldritch;
import thaumcraft.common.blocks.BlockStoneDevice;
import thaumcraft.common.blocks.BlockWoodenDevice;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.lib.world.BiomeColumns;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileBanner;
import thaumcraft.common.tiles.TileEldritchAltar;
import thaumcraft.common.tiles.TileEldritchObelisk;
import thaumcraft.common.tiles.TileEtherealBloom;
import thaumcraft.common.tiles.TileFluxScrubber;
import thaumcraft.common.tiles.TileMagicBox;
import thaumcraft.common.tiles.TileWardingStone;

/**
 * The original TC4 eldritch blocks and their block entities inside a real dedicated server: the
 * eldritch altar that counts the eldritch eyes and switches its spawn type, the eldritch obelisk
 * that only slows the eldritch mobs, the chain of magic boxes with its original twenty seven
 * slots per box, the warding stone that pushes the entities away and the arcane banner that keeps
 * its colour and its aspect, and the flux scrubber that spends its charges.
 */
public final class EldritchBlockRuntimeChecks {
    private EldritchBlockRuntimeChecks() {}

    /** The original eldritch altar takes the eldritch eyes and switches its spawn type. */
    public static void altarTakesEyesAndSwitchesItsSpawnType(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            lane.floor(3);
            BlockPos altarPos = lane.origin;
            lane.put(altarPos, thaumcraft.common.lib.world.FamilyVariant.with(ThaumcraftEldritchBlocks.ELDRITCH.get().defaultBlockState(), EldritchBlockRules.ELDRITCH_ALTAR));
            test.assertTrue(level.getBlockEntity(altarPos) instanceof TileEldritchAltar,
                    "the eldritch altar carries the original altar block entity");
            var altar = (TileEldritchAltar) level.getBlockEntity(altarPos);
            test.assertTrue(altar.getEyes() == 0 && !altar.isSpawner()
                            && altar.getSpawnType() == EldritchBlockRules.ALTAR_SPAWN_TYPE_CULTIST,
                    "a fresh altar has no eye, is no spawner and spawns cultists: " + altar.getEyes());

            var eye = new ItemStack(ThaumcraftWarp.ELDRITCH_EYE.get(), 4);
            use(player, eye, altarPos, Direction.UP);
            test.assertTrue(altar.getEyes() == 1 && eye.getCount() == 3,
                    "the first eye is counted and consumed: eyes=" + altar.getEyes() + " count=" + eye.getCount());
            test.assertTrue(!altar.isSpawner() && altar.getSpawnType() == EldritchBlockRules.ALTAR_SPAWN_TYPE_CULTIST,
                    "one eye still spawns cultists: " + altar.getSpawnType());
            use(player, eye, altarPos, Direction.UP);
            test.assertTrue(altar.getEyes() == 2 && !altar.isSpawner()
                            && altar.getSpawnType() == EldritchBlockRules.ALTAR_SPAWN_TYPE_CULTIST,
                    "two eyes still spawn cultists: eyes=" + altar.getEyes() + " spawner=" + altar.isSpawner()
                            + " type=" + altar.getSpawnType());
            use(player, eye, altarPos, Direction.UP);
            test.assertTrue(altar.getEyes() == 3 && altar.isSpawner()
                            && altar.getSpawnType() == EldritchBlockRules.ALTAR_SPAWN_TYPE_GUARDIAN,
                    "from the third eye on the altar becomes the guardian spawner: eyes=" + altar.getEyes()
                            + " spawner=" + altar.isSpawner() + " type=" + altar.getSpawnType());
            use(player, eye, altarPos, Direction.UP);
            test.assertTrue(altar.getEyes() == 4 && eye.getCount() == 0,
                    "the altar takes at most four eyes: eyes=" + altar.getEyes() + " count=" + eye.getCount());
            var extra = new ItemStack(ThaumcraftWarp.ELDRITCH_EYE.get(), 1);
            use(player, extra, altarPos, Direction.UP);
            test.assertTrue(altar.getEyes() == 4 && extra.getCount() == 1,
                    "a full altar refuses the fifth eye and keeps it: eyes=" + altar.getEyes() + " count=" + extra.getCount());
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /**
     * BlockEldritch:197-205 - breaking glyphed eldritch stone gives one to four experience. NeoForge
     * awards block experience only through its getExpDrop hook, never through spawnAfterBreak.
     */
    public static void glyphedStoneGivesItsExperience(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            lane.floor(3);
            BlockPos pos = lane.origin;
            var bounds = new net.minecraft.world.phys.AABB(pos).inflate(3);
            for (int round = 0; round < 4; round++) {
                lane.put(pos, thaumcraft.common.lib.world.FamilyVariant.with(ThaumcraftEldritchBlocks.ELDRITCH_CRUST.get().defaultBlockState(),
                        EldritchBlockRules.ELDRITCH_GLYPHED));
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
                test.assertTrue(player.gameMode.destroyBlock(pos) && level.getBlockState(pos).isAir(),
                        "survival mining removes the glyphed eldritch stone");
                int experience = 0;
                for (var orb : level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, bounds)) {
                    CompoundTag tag = new CompoundTag();
                    orb.addAdditionalSaveData(tag);
                    experience += orb.getValue() * Math.max(1, tag.getInt("Count"));
                    orb.discard();
                }
                test.assertTrue(experience >= 1 && experience <= 4,
                        "glyphed eldritch stone gives the original one to four experience: " + experience);
            }
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /**
     * TileEtherealBloom.updateEntity: the bloom ticks on both sides, and on the server turns a taint,
     * eerie or magical forest column within reach back into the biome the world generated there,
     * plains where that was taint itself. Until 2026-10-26 the bloom had no ticker in the port, so it
     * neither grew nor cleansed anything.
     */
    public static void etherealBloomCleansesBackToTheGeneratedBiome(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        ServerLevel level = lane.level;
        BlockPos pos = lane.origin;
        BlockPos tainted = pos.offset(3, 0, 2), forest = pos.offset(-2, 0, -1);
        var saved = new java.util.LinkedHashMap<BlockPos, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>>();
        try {
            for (BlockPos column : java.util.List.of(tainted, forest)) {
                var holder = BiomeColumns.surfaceHolder(level, column.getX(), column.getZ());
                test.assertTrue(holder != null && holder.unwrapKey().isPresent(), "the lane holds the chunk of " + column);
                saved.put(column, holder.unwrapKey().get());
            }
            lane.floor(1);
            lane.put(pos, ThaumcraftEldritchBlocks.ETHEREAL_BLOOM.get().defaultBlockState());
            test.assertTrue(level.getBlockEntity(pos) instanceof TileEtherealBloom, "the bloom is a real block entity");
            var bloom = (TileEtherealBloom) level.getBlockEntity(pos);
            BiomeColumns.set(level, tainted.getX(), tainted.getZ(), ThaumcraftBiomes.TAINT);
            test.assertTrue(TileEtherealBloom.cleanse(level, pos, 3, 2), "the bloom cleanses a tainted column three and two blocks away");
            var expected = generated(level, tainted);
            test.assertTrue(BiomeColumns.surfaceHolder(level, tainted.getX(), tainted.getZ()) == expected && !expected.is(ThaumcraftBiomes.TAINT),
                    "the column gets back the biome generated there, plains for generated taint: " + expected.unwrapKey().orElseThrow());
            test.assertTrue(!TileEtherealBloom.cleanse(level, pos, 3, 2), "a column that has its generated biome stays as it is");
            BiomeColumns.set(level, forest.getX(), forest.getZ(), ThaumcraftBiomes.MAGICAL_FOREST);
            var forestGenerated = generated(level, forest);
            test.assertTrue(TileEtherealBloom.cleanse(level, pos, -2, -1) == !forestGenerated.is(ThaumcraftBiomes.MAGICAL_FOREST)
                            && BiomeColumns.surfaceHolder(level, forest.getX(), forest.getZ()) == forestGenerated,
                    "a column a pure node turned into magical forest goes back as well: " + forestGenerated.unwrapKey().orElseThrow());
            int counter = bloom.counter, growth = bloom.growthCounter;
            test.runAfterDelay(2, () -> {
                try {
                    test.assertTrue(bloom.growthCounter == growth + 2 && bloom.counter >= counter + 2 && bloom.counter <= counter + 2 + EldritchBlockRules.ETHEREAL_BLOOM_START_PHASE,
                            "the bloom ticks on the server: growth " + growth + " -> " + bloom.growthCounter + ", counter " + counter + " -> " + bloom.counter);
                } finally {
                    restore(level, saved);
                    lane.clean();
                }
                test.succeed();
            });
        } catch (RuntimeException | Error failure) {
            restore(level, saved);
            lane.clean();
            throw failure;
        }
    }

    /** The generated biome of a column at its surface, plains for generated taint, as the bloom puts it back. */
    private static net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> generated(ServerLevel level, BlockPos column) {
        var chunk = level.getChunk(column);
        int surface = net.minecraft.util.Mth.clamp(chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, column.getX() & 15, column.getZ() & 15),
                level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        var biome = level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(column.getX()),
                net.minecraft.core.QuartPos.fromBlock(surface), net.minecraft.core.QuartPos.fromBlock(column.getZ()), level.getChunkSource().randomState().sampler());
        return biome.is(ThaumcraftBiomes.TAINT)
                ? level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(TileEtherealBloom.OVERWORLD_PLAINS)
                : biome;
    }

    private static void restore(ServerLevel level, java.util.Map<BlockPos, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>> saved) {
        saved.forEach((column, key) -> BiomeColumns.set(level, column.getX(), column.getZ(), key));
    }

    /** The original eldritch obelisk only buffs the eldritch mobs around it. */
    public static void obeliskSlowsOnlyTheEldritchMobs(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            player.setPos(lane.origin.getX() + 40D, lane.origin.getY(), lane.origin.getZ());
            lane.floor(4);
            BlockPos obeliskPos = lane.origin;
            lane.put(obeliskPos, thaumcraft.common.lib.world.FamilyVariant.with(ThaumcraftEldritchBlocks.ELDRITCH.get().defaultBlockState(), EldritchBlockRules.ELDRITCH_OBELISK));
            test.assertTrue(level.getBlockEntity(obeliskPos) instanceof TileEldritchObelisk,
                    "the eldritch obelisk carries the original obelisk block entity");

            var crab = lane.add(ThaumcraftEldritchMobs.ELDRITCH_CRAB.get().create(level));
            crab.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 2.5D, 0F, 0F);
            crab.setNoAi(true);
            level.addFreshEntity(crab);
            var pig = lane.add(EntityType.PIG.create(level));
            pig.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 3.5D, 0F, 0F);
            pig.setNoAi(true);
            level.addFreshEntity(pig);
            var obelisk = (TileEldritchObelisk) level.getBlockEntity(obeliskPos);
            for (int tick = 0; tick < 21; tick++) {
                TileEldritchObelisk.tick(level, obeliskPos, level.getBlockState(obeliskPos), obelisk);
                level.tickNonPassenger(crab);
                level.tickNonPassenger(pig);
            }
            // Oracle TileEldritchObelisk:51-52 - damage boost and regeneration,
            // never a slowdown: the obelisk empowers the eldritch mobs.
            test.assertTrue(crab.hasEffect(MobEffects.DAMAGE_BOOST) && crab.hasEffect(MobEffects.REGENERATION),
                    "the obelisk empowers the eldritch crab: " + crab.getEffect(MobEffects.DAMAGE_BOOST));
            test.assertTrue(crab.getEffect(MobEffects.DAMAGE_BOOST).getDuration() <= EldritchBlockRules.OBELISK_EFFECT_DURATION
                            && crab.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() == EldritchBlockRules.OBELISK_EFFECT_AMPLIFIER,
                    "the obelisk effect is the original forty ticks of amplifier zero: "
                            + crab.getEffect(MobEffects.DAMAGE_BOOST).getDuration());
            test.assertTrue(!pig.hasEffect(MobEffects.DAMAGE_BOOST) && !pig.hasEffect(MobEffects.REGENERATION),
                    "the obelisk never touches a mob that is not an eldritch mob: " + pig.getEffect(MobEffects.DAMAGE_BOOST));
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original chain of magic boxes owns twenty seven slots per box. */
    public static void magicBoxChainOwnsTwentySevenSlotsPerBox(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            lane.floor(3);
            BlockPos first = lane.origin;
            BlockPos second = lane.origin.east();
            lane.put(first, ThaumcraftEldritchBlocks.MAGIC_BOX.get().defaultBlockState());
            lane.put(second, ThaumcraftEldritchBlocks.MAGIC_BOX.get().defaultBlockState());
            test.assertTrue(level.getBlockEntity(first) instanceof TileMagicBox && level.getBlockEntity(second) instanceof TileMagicBox,
                    "the magic box carries the original box block entity");
            var box = (TileMagicBox) level.getBlockEntity(first);
            test.assertTrue(box.getContainerSize() == EldritchBlockRules.MAGIC_BOX_SLOTS_PER_BOX,
                    "a lonely magic box owns the original twenty seven slots: " + box.getContainerSize());
            TileMagicBox.tick(level, first, level.getBlockState(first), box);
            test.assertTrue(box.getLinkedBoxes() == 2,
                    "the magic box finds the original neighbour box: " + box.getLinkedBoxes());
            test.assertTrue(box.getContainerSize() == EldritchBlockRules.magicBoxSize(2),
                    "the chain of two boxes owns the original eighty one slots: " + box.getContainerSize());
            test.assertTrue(box.getMaxStackSize() == EldritchBlockRules.MAGIC_BOX_MAX_STACK,
                    "a magic box slot holds the original sixty four items: " + box.getMaxStackSize());
            box.setSorting((byte) EldritchBlockRules.MAGIC_BOX_SORT_NAME_ASCENDING);
            box.setItem(0, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 1));
            box.setItem(1, new ItemStack(net.minecraft.world.item.Items.APPLE, 1));
            box.sort();
            test.assertTrue(box.getItem(0).is(net.minecraft.world.item.Items.APPLE)
                            && box.getItem(1).is(net.minecraft.world.item.Items.DIAMOND),
                    "the magic box sorts its content by the original name order: " + box.getItem(0) + " " + box.getItem(1));
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original warding stone pushes the entities away and the banner keeps its aspect. */
    public static void wardingStonePushesAndBannerKeepsItsAspect(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            lane.floor(3);
            BlockPos stonePos = lane.origin;
            lane.put(stonePos, thaumcraft.common.lib.world.FamilyVariant.with(ThaumcraftEldritchBlocks.COSMETIC_SOLID.get().defaultBlockState(), EldritchBlockRules.COSMETIC_WARDING_STONE));
            test.assertTrue(level.getBlockEntity(stonePos) instanceof TileWardingStone,
                    "the warding stone carries the original warding stone block entity");
            var stone = (TileWardingStone) level.getBlockEntity(stonePos);
            test.assertTrue(!stone.gettingPower(level),
                    "the ward of the warding stone only works with a redstone signal");

            var pig = lane.add(EntityType.PIG.create(level));
            pig.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            pig.setNoAi(true);
            pig.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(pig);
            player.setPos(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D);
            player.setNoGravity(true);
            player.setDeltaMovement(Vec3.ZERO);
            double startDistance = pig.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(stonePos));
            float pushed = 0F;
            double playerStart = player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(stonePos));
            for (int tick = 0; tick < 101; tick++) {
                TileWardingStone.tick(level, stonePos, level.getBlockState(stonePos), stone);
                if (tick == 4) pushed = (float) pig.getDeltaMovement().length();
                level.tickNonPassenger(pig);
            }
            test.assertTrue(pushed > 0F,
                    "the warding stone pushes the entities inside it away: " + pushed + " " + pig.position());
            test.assertTrue(pig.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(stonePos)) >= startDistance - 1e-6D,
                    "the warding stone never pulls a mob into its ward: " + pig.position());
            test.assertTrue(Math.abs(player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(stonePos)) - playerStart) < 1e-6D,
                    "the warding stone never pushes a player: " + player.position());
            // The original seal of the warding stone closes the two blocks above it.
            test.assertTrue(level.getBlockState(stonePos.above(2)).is(ThaumcraftEldritchBlocks.AIRY.get()),
                    "the warding stone seals the block above it: " + level.getBlockState(stonePos.above(2)));
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original arcane banner keeps its colour and its aspect. */
    public static void bannerKeepsItsColourAndAspect(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            lane.floor(3);
            BlockPos bannerPos = lane.origin;
            lane.put(bannerPos, ThaumcraftEldritchBlocks.WOODEN_DEVICE.get().defaultBlockState()
                    .setValue(BlockWoodenDevice.VARIANT, EldritchBlockRules.DEVICE_WOOD_BANNER));
            test.assertTrue(level.getBlockEntity(bannerPos) instanceof TileBanner,
                    "the arcane banner carries the original banner block entity");
            var banner = (TileBanner) level.getBlockEntity(bannerPos);
            test.assertTrue(banner.getColor() == EldritchBlockRules.BANNER_COLOUR_NONE && banner.getAspect() == null
                            && !banner.hasContent(),
                    "a fresh banner has no colour and no aspect: " + banner.getColor());
            banner.setFacing(Direction.EAST);
            banner.setColor((byte) 14);
            banner.setAspect(Aspect.getAspect("aer"));
            banner.setWall(true);
            CompoundTag saved = banner.saveWithFullMetadata(level.registryAccess());
            var restored = new TileBanner(bannerPos, level.getBlockState(bannerPos));
            restored.loadWithComponents(saved, level.registryAccess());
            test.assertTrue(restored.getFacing() == Direction.EAST && restored.getColor() == 14
                            && restored.getAspect() != null && "aer".equals(restored.getAspect().getTag()) && restored.isOnWall(),
                    "the banner keeps its original facing, colour and aspect: " + restored.getFacing() + " " + restored.getColor() + " "
                            + restored.getAspect());
            test.assertTrue(restored.hasContent(), "a banner with a colour has content");
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original flux scrubber spends its charges and stores its essentia. */
    public static void fluxScrubberSpendsItsCharges(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            lane.floor(3);
            BlockPos scrubberPos = lane.origin;
            lane.put(scrubberPos, ThaumcraftEldritchBlocks.STONE_DEVICE.get().defaultBlockState()
                    .setValue(BlockStoneDevice.VARIANT, EldritchBlockRules.DEVICE_STONE_FLUX_SCRUBBER));
            test.assertTrue(level.getBlockEntity(scrubberPos) instanceof TileFluxScrubber,
                    "the flux scrubber carries the original scrubber block entity");
            var scrubber = (TileFluxScrubber) level.getBlockEntity(scrubberPos);
            scrubber.charges = EldritchBlockRules.FLUX_CHARGE_COST;
            TileFluxScrubber.tick(level, scrubberPos, level.getBlockState(scrubberPos), scrubber);
            test.assertTrue(scrubber.getCharges() == 0,
                    "the scrubber spends the original four charges at once: " + scrubber.getCharges());
            test.assertTrue(scrubber.getEssentia() == 0 || scrubber.getEssentia() == 1,
                    "the scrubber stores at most one essentia per spent charge: " + scrubber.getEssentia());
            scrubber.essentia = 2;
            test.assertTrue(scrubber.takeEssentia(1) == 1 && scrubber.getEssentia() == 1,
                    "the scrubber hands its essentia out one unit at a time: " + scrubber.getEssentia());
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** A real player interaction with a real block, exactly as the original altar expects it. */
    private static void use(ServerPlayer player, ItemStack hand, BlockPos pos, Direction face) {
        player.setPos(pos.getX() + .5D, pos.getY(), pos.getZ() + 2D);
        player.setItemInHand(InteractionHand.MAIN_HAND, hand);
        player.gameMode.useItemOn(player, player.serverLevel(), hand, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).relative(face, .5D), face, pos, false));
    }
}
