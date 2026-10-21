package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftOuterLands;
import thaumcraft.common.config.ThaumcraftTaint;

/**
 * The boss rooms of the Outer Lands that the eldritch lock opens.
 *
 * <p>Oracle {@code TileEldritchLock:62-354} - the lock of a boss room counts to a hundred
 * and one, then it tears the room down, builds the arena of one of the four bosses and
 * spawns that boss. The arena of the boss is built in the middle of the four chunks of the
 * boss room at the height fifty, and the boss is chosen by the counter of the boss map:
 * the counter is raised by one for every lock and by a second one in a quarter of the
 * cases, and the boss is the counter modulo four:
 *
 * <ul>
 *   <li>zero - the eldritch golem,</li>
 *   <li>one - the eldritch warden,</li>
 *   <li>two - the portal of the crimson cult,</li>
 *   <li>three - the hall of the taint.</li>
 * </ul>
 *
 * <p>The 1.21.1 port has no room generator that runs at the moment the player opens the
 * lock, so the four arenas are built here, at the moment the lock opens.
 */
public final class OuterLandsBossRooms {
    private OuterLandsBossRooms() {}

    /** Oracle TileEldritchLock:63 - the sound of the opening lock. */
    public static final SoundEvent ICE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "ice"));
    /** The height of the arenas, oracle TileEldritchLock:132. */
    public static final int ARENA_Y = 50;
    /** Oracle TileEldritchLock:129 - a player hears the arrival of the boss within this square distance. */
    public static final double ANNOUNCE_DISTANCE_SQ = 300.0D;
    /** Oracle TileEldritchLock:88-90 - a quarter of the locks count twice. */
    public static final float DOUBLE_BOSS_CHANCE = 0.25F;
    /** Oracle TileEldritchLock:165 - the urns of the warden appear in nine of ten cells. */
    public static final float WARDEN_URN_CHANCE = 0.9F;
    /** Oracle TileEldritchLock:167 - the rare urn of the warden. */
    public static final float WARDEN_URN_RARE = 0.1F;
    /** Oracle TileEldritchLock:167 - the uncommon urn of the warden. */
    public static final float WARDEN_URN_UNCOMMON = 0.3F;
    /** Oracle TileEldritchLock:259 - the rare urn of the golem. */
    public static final float GOLEM_URN_RARE = 0.05F;
    /** Oracle TileEldritchLock:259 - the uncommon urn of the golem. */
    public static final float GOLEM_URN_UNCOMMON = 0.2F;
    /** Oracle TileEldritchLock:257 - the loot of the golem appears in one cell of fifteen. */
    public static final float GOLEM_LOOT_CHANCE = 0.15F;
    /** Oracle TileEldritchLock:260 - three of ten crates of the golem are crates. */
    public static final float GOLEM_CRATE_CHANCE = 0.3F;
    /** Oracle TileEldritchLock:288 - the second ring of the floor of the cult is halved. */
    public static final float CULTIST_RING_TWO_CHANCE = 0.5F;
    /** Oracle TileEldritchLock:288 - the third ring of the floor of the cult keeps two thirds. */
    public static final float CULTIST_RING_THREE_KEEP = 0.33F;
    /** Oracle TileEldritchLock:288 - the fourth ring of the floor of the cult keeps three quarters. */
    public static final float CULTIST_RING_FOUR_KEEP = 0.25F;
    /** Oracle TileEldritchLock:330 - the taint floor of the fourth ring is halved. */
    public static final float TAINT_RING_FOUR_CHANCE = 0.5F;
    /** Oracle TileEldritchLock:330 - the taint floor of the fifth ring keeps two thirds. */
    public static final float TAINT_RING_FIVE_KEEP = 0.33F;
    /** Oracle TileEldritchLock:330 - the taint floor of the seventh ring keeps three quarters. */
    public static final float TAINT_RING_SEVEN_KEEP = 0.25F;
    /** Oracle TileEldritchLock:42 - the pattern of the pedestal of the boss. */
    public static final int[][] PEDESTAL = {{2, 2, 2}, {0, -1, 1}, {3, 3, 3}};

    /**
     * The offset of the arena on the X axis for the exit of the boss room.
     *
     * <p>Oracle {@code TileEldritchLock:136-152,219-239} - the exit two is the upper right
     * quarter, the exit three the upper left, the exit four the lower right and the exit five
     * the lower left. An exit without a direction keeps the middle of the arena.
     */
    public static int exitOffsetX(int exit) {
        return switch (exit) {
            case 2, 4 -> 8;
            case 3, 5 -> -8;
            default -> 0;
        };
    }

    /**
     * The offset of the arena on the Z axis for the exit of the boss room.
     *
     * <p>Oracle {@code TileEldritchLock:136-152,219-239} - see {@link #exitOffsetX(int)}.
     */
    public static int exitOffsetZ(int exit) {
        return switch (exit) {
            case 2, 3 -> 8;
            case 4, 5 -> -8;
            default -> 0;
        };
    }

    /** Oracle {@code TileEldritchLock:167} - the rarity of a loot urn of the warden. */
    public static int wardenUrnMeta(float roll) {
        return urnMeta(roll, WARDEN_URN_RARE, WARDEN_URN_UNCOMMON);
    }

    /** Oracle {@code TileEldritchLock:259} - the rarity of a loot urn of the golem. */
    public static int golemUrnMeta(float roll) {
        return urnMeta(roll, GOLEM_URN_RARE, GOLEM_URN_UNCOMMON);
    }

    /**
     * Oracle {@code TileEldritchLock:257} - the four corners of the floor of the golem carry
     * the loot of the arena.
     */
    public static boolean golemLootCorner(int a, int b) {
        return (a < -2 && b < -2) || (a > 2 && b > 2) || (a < -2 && b > 2) || (a > 2 && b < -2);
    }

    /**
     * The second ring of the floor of the cult of a cell of the arena.
     *
     * <p>Oracle {@code TileEldritchLock:288} - the ring two is halved.
     */
    public static boolean cultistRingTwo(int a, int b) {
        return Math.abs(a) == 2 || Math.abs(b) == 2;
    }

    /**
     * The third ring of the floor of the cult of a cell of the arena.
     *
     * <p>Oracle {@code TileEldritchLock:288} - the ring three keeps two thirds of its cells.
     */
    public static boolean cultistRingThree(int a, int b) {
        return Math.abs(a) == 3 || Math.abs(b) == 3;
    }

    /**
     * The fourth ring of the floor of the cult of a cell of the arena.
     *
     * <p>Oracle {@code TileEldritchLock:288} - the ring four keeps three quarters of its cells.
     */
    public static boolean cultistRingFour(int a, int b) {
        return Math.abs(a) == 4 || Math.abs(b) == 4;
    }

    /**
     * The fourth ring of the floor of the taint of a cell of the arena.
     *
     * <p>Oracle {@code TileEldritchLock:330} - the ring four is halved.
     */
    public static boolean taintRingFour(int a, int b) {
        return Math.abs(a) == 4 || Math.abs(b) == 4;
    }

    /**
     * The fifth ring of the floor of the taint of a cell of the arena.
     *
     * <p>Oracle {@code TileEldritchLock:330} - the ring five keeps two thirds of its cells.
     */
    public static boolean taintRingFive(int a, int b) {
        return Math.abs(a) >= 5 || Math.abs(b) >= 5;
    }

    /**
     * The seventh ring of the floor of the taint of a cell of the arena.
     *
     * <p>Oracle {@code TileEldritchLock:330} - the ring seven keeps three quarters of its cells.
     */
    public static boolean taintRingSeven(int a, int b) {
        return Math.abs(a) >= 7 || Math.abs(b) >= 7;
    }

    /**
     * The counter of the boss rooms.
     *
     * <p>Oracle {@code MapBossData} - the original keeps the counter in the saved data of the
     * world, so every world counts its own boss rooms. The native port keeps the counter in
     * the saved data of the dimension of the lock.
     */
    public static final class BossSavedData extends SavedData {
        /** The name of the saved data of the original ("BossMapData"). */
        public static final String ID = "thaumcraft_boss_map";
        private static final SavedData.Factory<BossSavedData> FACTORY = new SavedData.Factory<>(
                BossSavedData::new, BossSavedData::load, DataFixTypes.LEVEL);
        /** The number of the boss rooms that were opened. */
        public int bossCount;

        public BossSavedData() {
        }

        public static BossSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
            BossSavedData data = new BossSavedData();
            data.bossCount = tag.getInt("bossCount");
            return data;
        }

        @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putInt("bossCount", bossCount);
            return tag;
        }
    }

    /**
     * Oracle {@code TileEldritchLock:49-121} - the lock opens.
     *
     * @param level   the level of the lock
     * @param lockPos the position of the lock
     */
    public static void openLock(ServerLevel level, BlockPos lockPos) {
        // Oracle TileEldritchLock:63 - the ice of the opening lock.
        level.playSound(null, lockPos, ICE, SoundSource.BLOCKS, 1.0F, 1.0F);
        int cx = lockPos.getX() >> 4;
        int cz = lockPos.getZ() >> 4;
        int centreX = cx;
        int centreZ = cz;
        int exit = 0;
        // Oracle TileEldritchLock:70-80 - the centre of the boss room is the chunk of the
        // cell with the feature two, the exit is the feature of the last boss cell that has
        // an exit at all.
        OuterLandsProvider.MazeSavedData maze = OuterLandsProvider.maze(level);
        for (int a = -2; a <= 2; ++a) {
            for (int b = -2; b <= 2; ++b) {
                short packed = maze.cell(cx + a, cz + b);
                if (packed == 0) continue;
                MazeGenerator.Cell cell = new MazeGenerator.Cell(packed);
                if (cell.feature == OuterLandsRules.FEATURE_BOSS_UPPER_LEFT) {
                    centreX = cx + a;
                    centreZ = cz + b;
                }
                if (cell.feature < OuterLandsRules.FEATURE_BOSS_UPPER_LEFT
                        || cell.feature > OuterLandsRules.FEATURE_BOSS_LOWER_RIGHT) continue;
                if (!cell.north && !cell.south && !cell.east && !cell.west) continue;
                exit = cell.feature;
            }
        }
        BossSavedData bosses = level.getDataStorage().computeIfAbsent(BossSavedData.FACTORY, BossSavedData.ID);
        bosses.bossCount++;
        if (level.getRandom().nextFloat() < DOUBLE_BOSS_CHANCE) bosses.bossCount++;
        bosses.setDirty();
        switch (bosses.bossCount % 4) {
            case 0 -> spawnGolemRoom(level, lockPos, centreX, centreZ, exit);
            case 1 -> spawnWardenRoom(level, lockPos, centreX, centreZ, exit);
            case 2 -> spawnCultistRoom(level, lockPos, centreX, centreZ, exit);
            default -> spawnTaintRoom(level, lockPos, centreX, centreZ, exit);
        }
        // Oracle TileEldritchLock:110-119 - the invisible barrier of the room disappears and
        // the lock itself with it.
        Block airy = ThaumcraftOuterLands.block("airy");
        if (airy != null) {
            for (int a = -2; a <= 2; ++a) {
                for (int b = -2; b <= 2; ++b) {
                    for (int c = -2; c <= 2; ++c) {
                        BlockPos target = lockPos.offset(a, b, c);
                        if (!level.getBlockState(target).is(airy)) continue;
                        level.removeBlock(target, false);
                    }
                }
            }
        }
        level.removeBlock(lockPos, false);
    }

    /* ------------------------------------------------------------------ */
    /* The four arenas                                                     */
    /* ------------------------------------------------------------------ */

    /** Oracle {@code TileEldritchLock:123-204} - the arena of the eldritch warden. */
    private static void spawnWardenRoom(ServerLevel level, BlockPos lockPos, int cx, int cz, int exit) {
        announce(level, lockPos, "tc.boss.warden");
        int x = cx * 16 + 16;
        int y = ARENA_Y;
        int z = cz * 16 + 16;
        int x2 = x + exitOffsetX(exit);
        int z2 = z + exitOffsetZ(exit);
        obelisk(level, x2, y + 4, z);
        obelisk(level, x, y + 4, z2);
        set(level, x2, y + 2, z, DungeonRooms.Kind.ELDRITCH, 3);
        set(level, x, y + 2, z2, DungeonRooms.Kind.ELDRITCH, 3);
        RandomSource random = level.getRandom();
        for (int a = -1; a <= 1; ++a) {
            for (int b = -1; b <= 1; ++b) {
                if (a != 0 && b != 0 && random.nextFloat() < WARDEN_URN_CHANCE) {
                    set(level, x2 + a, y + 2, z + b, DungeonRooms.Kind.LOOT_URN, wardenUrnMeta(random.nextFloat()));
                }
                if (a == 0 || b == 0 || !(random.nextFloat() < WARDEN_URN_CHANCE)) continue;
                set(level, x + a, y + 2, z2 + b, DungeonRooms.Kind.LOOT_URN, wardenUrnMeta(random.nextFloat()));
            }
        }
        set(level, x - 2, y + 3, z - 2, DungeonRooms.Kind.ELDRITCH, 10);
        set(level, x - 2, y + 3, z + 2, DungeonRooms.Kind.ELDRITCH, 10);
        set(level, x + 2, y + 3, z + 2, DungeonRooms.Kind.ELDRITCH, 10);
        set(level, x + 2, y + 3, z - 2, DungeonRooms.Kind.ELDRITCH, 10);
        set(level, x - 2, y + 2, z - 2, DungeonRooms.Kind.COSMETIC_SOLID, 15);
        set(level, x - 2, y + 2, z + 2, DungeonRooms.Kind.COSMETIC_SOLID, 15);
        set(level, x + 2, y + 2, z + 2, DungeonRooms.Kind.COSMETIC_SOLID, 15);
        set(level, x + 2, y + 2, z - 2, DungeonRooms.Kind.COSMETIC_SOLID, 15);
        pedestal(level, x2, y + 2, z2);
        Entity boss = create(level, "eldritch_warden");
        if (boss == null) return;
        faceLock(boss, lockPos, x2, y + 3, z2);
        // Oracle TileEldritchLock:201 - the boss of the arena is spawned with its equipment.
        if (boss instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(lockPos), MobSpawnType.SPAWN_EGG, null);
            // Oracle TileEldritchLock:202 - the warden anchors itself at the middle of the room.
            mob.restrictTo(new BlockPos(x, y + 2, z), 32);
        }
        level.addFreshEntity(boss);
    }

    /** Oracle {@code TileEldritchLock:206-273} - the arena of the eldritch golem. */
    private static void spawnGolemRoom(ServerLevel level, BlockPos lockPos, int cx, int cz, int exit) {
        announce(level, lockPos, "tc.boss.golem");
        int x = cx * 16 + 16;
        int y = ARENA_Y;
        int z = cz * 16 + 16;
        int x2 = exitOffsetX(exit);
        int z2 = exitOffsetZ(exit);
        obelisk(level, x + x2, y + 4, z + z2);
        obelisk(level, x - x2, y + 4, z + z2);
        obelisk(level, x + x2, y + 4, z - z2);
        set(level, x + x2, y + 2, z + z2, DungeonRooms.Kind.ELDRITCH, 3);
        set(level, x - x2, y + 2, z + z2, DungeonRooms.Kind.ELDRITCH, 3);
        set(level, x + x2, y + 2, z - z2, DungeonRooms.Kind.ELDRITCH, 3);
        pedestal(level, x, y + 2, z);
        RandomSource random = level.getRandom();
        for (int a = -10; a <= 10; ++a) {
            for (int b = -10; b <= 10; ++b) {
                if (!golemLootCorner(a, b)) continue;
                if (!(random.nextFloat() < GOLEM_LOOT_CHANCE)) continue;
                BlockPos target = new BlockPos(x + a, y + 2, z + b);
                if (!level.getBlockState(target).isAir()) continue;
                int meta = golemUrnMeta(random.nextFloat());
                set(level, x + a, y + 2, z + b,
                        random.nextFloat() < GOLEM_CRATE_CHANCE ? DungeonRooms.Kind.LOOT_CRATE : DungeonRooms.Kind.LOOT_URN, meta);
            }
        }
        Entity boss = create(level, "eldritch_golem");
        if (boss == null) return;
        faceLock(boss, lockPos, x, y + 3, z);
        // Oracle TileEldritchLock:271 - the boss of the arena is spawned with its equipment.
        if (boss instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(lockPos), MobSpawnType.SPAWN_EGG, null);
        }
        level.addFreshEntity(boss);
    }

    /** Oracle {@code TileEldritchLock:275-306} - the arena of the portal of the crimson cult. */
    private static void spawnCultistRoom(ServerLevel level, BlockPos lockPos, int cx, int cz, int exit) {
        announce(level, lockPos, "tc.boss.crimson");
        int x = cx * 16 + 16;
        int y = ARENA_Y;
        int z = cz * 16 + 16;
        RandomSource random = level.getRandom();
        for (int a = -4; a <= 4; ++a) {
            for (int b = -4; b <= 4; ++b) {
                if (cultistRingTwo(a, b) && random.nextBoolean()) continue;
                if (cultistRingThree(a, b) && random.nextFloat() > CULTIST_RING_THREE_KEEP) continue;
                if (cultistRingFour(a, b) && random.nextFloat() > CULTIST_RING_FOUR_KEEP) continue;
                set(level, x + b, y + 1, z + a, DungeonRooms.Kind.ELDRITCH, 7);
            }
        }
        for (int a = 0; a < 5; ++a) {
            for (int b = 0; b < 5; ++b) {
                if (a != 0 && a != 4 && b != 0 && b != 4) continue;
                set(level, x - 8 + b * 4, y + 2, z - 8 + a * 4, DungeonRooms.Kind.COSMETIC_SOLID, 11);
                set(level, x - 8 + b * 4, y + 3, z - 8 + a * 4, DungeonRooms.Kind.ELDRITCH, 5);
                set(level, x - 8 + b * 4, y + 4, z - 8 + a * 4, DungeonRooms.Kind.COSMETIC_SLAB_STONE, 1);
                set(level, x - 8 + b * 4, y + 10, z - 8 + a * 4, DungeonRooms.Kind.COSMETIC_SOLID, 11);
                set(level, x - 8 + b * 4, y + 9, z - 8 + a * 4, DungeonRooms.Kind.ELDRITCH, 5);
                set(level, x - 8 + b * 4, y + 8, z - 8 + a * 4, DungeonRooms.Kind.COSMETIC_SLAB_STONE, 9);
            }
        }
        Entity boss = create(level, "cultist_portal");
        if (boss == null) return;
        boss.moveTo(x + 0.5D, y + 2, z + 0.5D, 0.0F, 0.0F);
        // Oracle TileEldritchLock:304 - the portal of the cult is spawned into the room.
        level.addFreshEntity(boss);
    }

    /** Oracle {@code TileEldritchLock:308-354} - the arena of the taint. */
    private static void spawnTaintRoom(ServerLevel level, BlockPos lockPos, int cx, int cz, int exit) {
        announce(level, lockPos, "tc.boss.taint");
        int x = cx * 16 + 16;
        int y = ARENA_Y;
        int z = cz * 16 + 16;
        RandomSource random = level.getRandom();
        for (int a = -12; a <= 12; ++a) {
            for (int b = -12; b <= 12; ++b) {
                // Oracle TileEldritchLock:319 - the arena is the biome of the taint.
                level.getChunk((x + b) >> 4, (z + a) >> 4);
                BiomeColumns.set(level, x + b, z + a, ThaumcraftBiomes.TAINT);
                for (int c = 0; c < 9; ++c) {
                    BlockPos target = new BlockPos(x + b, y + 2 + c, z + a);
                    if (!level.getBlockState(target).isAir()) continue;
                    if (!adjacentToSolid(level, target)) continue;
                    if (random.nextInt(3) == 0) continue;
                    setFibres(level, target, random.nextInt(4) == 0 ? 1 : 0);
                }
                if (random.nextFloat() < 0.15F) {
                    setTaint(level, x + b, y + 2, z + a, 0);
                    if (random.nextFloat() < 0.2F) setTaint(level, x + b, y + 3, z + a, 0);
                }
                if (taintRingFour(a, b) && random.nextBoolean()) continue;
                if (taintRingFive(a, b) && random.nextFloat() > TAINT_RING_FIVE_KEEP) continue;
                if (taintRingSeven(a, b) && random.nextFloat() > TAINT_RING_SEVEN_KEEP) continue;
                setTaint(level, x + b, y + 1, z + a, 1);
            }
        }
        // Oracle TileEldritchLock:334-353 - five champions of the taint.
        boolean hard = level.getDifficulty() == net.minecraft.world.Difficulty.HARD;
        Entity boss1 = create(level, hard ? "taintacle_giant" : "taintacle");
        if (boss1 != null) {
            boss1.moveTo(x + 0.5D, y + 3, z + 0.5D, 0.0F, 0.0F);
            champion(level, boss1);
            level.addFreshEntity(boss1);
        }
        Entity boss2 = create(level, random.nextBoolean() ? "taintacle_giant" : "taintacle");
        if (boss2 != null) {
            boss2.moveTo(x + 3.5D, y + 3, z + 3.5D, 0.0F, 0.0F);
            champion(level, boss2);
            level.addFreshEntity(boss2);
        }
        Entity boss3 = create(level, isGiant(boss2) ? "taintacle" : "taintacle_giant");
        if (boss3 != null) {
            boss3.moveTo(x - 2.5D, y + 3, z + 3.5D, 0.0F, 0.0F);
            champion(level, boss3);
            level.addFreshEntity(boss3);
        }
        Entity boss4 = create(level, random.nextBoolean() ? "taintacle_giant" : "taintacle");
        if (boss4 != null) {
            boss4.moveTo(x + 3.5D, y + 3, z - 2.5D, 0.0F, 0.0F);
            champion(level, boss4);
            level.addFreshEntity(boss4);
        }
        Entity boss5 = create(level, isGiant(boss4) ? "taintacle" : "taintacle_giant");
        if (boss5 != null) {
            boss5.moveTo(x - 2.5D, y + 3, z - 2.5D, 0.0F, 0.0F);
            champion(level, boss5);
            level.addFreshEntity(boss5);
        }
    }

    /* ------------------------------------------------------------------ */
    /* The helpers of the arenas                                           */
    /* ------------------------------------------------------------------ */

    /** Oracle {@code GenCommon.genObelisk:256-262} - the obelisk of the arena. */
    private static void obelisk(ServerLevel level, int x, int y, int z) {
        set(level, x, y, z, DungeonRooms.Kind.ELDRITCH, 1);
        for (int i = 1; i <= 4; ++i) set(level, x, y + i, z, DungeonRooms.Kind.ELDRITCH, 2);
    }

    /** Oracle {@code TileEldritchLock:184-192} - the pedestal of the boss. */
    private static void pedestal(ServerLevel level, int x, int y, int z) {
        for (int a = 0; a < 3; ++a) {
            for (int b = 0; b < 3; ++b) {
                int meta = PEDESTAL[a][b];
                if (meta < 0) {
                    set(level, x - 1 + b, y, z - 1 + a, DungeonRooms.Kind.ELDRITCH, 4);
                    continue;
                }
                set(level, x - 1 + b, y, z - 1 + a, DungeonRooms.Kind.STAIRS, meta);
            }
        }
    }

    /**
     * The rarity of a loot urn.
     *
     * @param roll      the roll of the original
     * @param rare      the bound of the rare urn
     * @param uncommon  the bound of the uncommon urn
     */
    public static int urnMeta(float roll, float rare, float uncommon) {
        if (roll < rare) return 2;
        if (roll < uncommon) return 1;
        return 0;
    }

    /** One block of an arena. */
    private static void set(ServerLevel level, int x, int y, int z, DungeonRooms.Kind kind, int variant) {
        BlockState state = ThaumcraftOuterLands.state(kind, variant, -1);
        if (state == null) return;
        level.setBlock(new BlockPos(x, y, z), state, net.minecraft.world.level.block.Block.UPDATE_NEIGHBORS
                | net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    /** The taint of the arena: the original block carries the metadata zero and one. */
    private static void setTaint(ServerLevel level, int x, int y, int z, int metadata) {
        BlockState state = (metadata == 0 ? ThaumcraftTaint.CRUST : ThaumcraftTaint.SOIL).get().defaultBlockState();
        level.setBlock(new BlockPos(x, y, z), state, net.minecraft.world.level.block.Block.UPDATE_NEIGHBORS
                | net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    /** The fibres of the arena: the original block carries the metadata zero and one. */
    private static void setFibres(ServerLevel level, BlockPos pos, int metadata) {
        BlockState state = ThaumcraftTaint.FIBRES.get().defaultBlockState()
                .setValue(BlockTaintFibres.TYPE, metadata);
        level.setBlock(pos, state, net.minecraft.world.level.block.Block.UPDATE_NEIGHBORS
                | net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    /** Oracle {@code BlockUtils.isAdjacentToSolidBlock:274-281} - a solid block touches the cell. */
    public static boolean adjacentToSolid(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            if (!level.getBlockState(neighbour).isFaceSturdy(level, neighbour, direction.getOpposite())) continue;
            return true;
        }
        return false;
    }

    /** Oracle {@code TileEldritchLock:126-130} - the players near the lock are told what comes. */
    private static void announce(ServerLevel level, BlockPos lockPos, String key) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(lockPos.getX(), lockPos.getY(), lockPos.getZ()) >= ANNOUNCE_DISTANCE_SQ) continue;
            player.sendSystemMessage(Component.translatable(key));
        }
    }

    /** The boss of the arena. */
    private static Entity create(ServerLevel level, String id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, id));
        if (type == null) return null;
        return type.create(level);
    }

    /**
     * Oracle {@code TileEldritchLock:193-203} - the boss looks at the lock that opened the room.
     *
     * @param boss   the boss
     * @param lockPos the position of the lock
     * @param x      the block coordinate of the boss
     * @param y      the block coordinate of the boss
     * @param z      the block coordinate of the boss
     */
    public static void faceLock(Entity boss, BlockPos lockPos, double x, double y, double z) {
        double d0 = lockPos.getX() - (x + 0.5D);
        double d1 = lockPos.getY() - (y + boss.getEyeHeight());
        double d2 = lockPos.getZ() - (z + 0.5D);
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        float yaw = (float) (Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(d1, d3) * 180.0D / Math.PI);
        boss.moveTo(x + 0.5D, y, z + 0.5D, yaw, pitch);
    }

    /** Oracle {@code EntityUtils.makeChampion:212-252} - the champion of the taint arena. */
    private static void champion(ServerLevel level, Entity boss) {
        if (boss instanceof net.minecraft.world.entity.LivingEntity living) {
            thaumcraft.common.lib.utils.EntityUtils.makeChampion(living, true);
        }
    }

    /** True when the boss is the giant taintacle. */
    private static boolean isGiant(Entity boss) {
        if (boss == null) return false;
        return "taintacle_giant".equals(BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).getPath());
    }
}
