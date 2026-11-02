package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftCultists;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.entities.monster.EntityCultist;
import thaumcraft.common.entities.monster.EntityCultistCleric;
import thaumcraft.common.entities.monster.EntityTaintacleSmall;
import thaumcraft.common.lib.world.BiomeColumns;

/** Native Mob/GoalSelector clocks and the actual distant-attack tentacle path. */
public final class TailMobCadenceReviewRuntimeChecks {
    private TailMobCadenceReviewRuntimeChecks() {}

    /** A sustained production melee goal counts its ten-tick cooldown in real server ticks. */
    public static void cultistMeleeCooldownUsesServerTicks(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            List<ChunkPos> held = GolemChunks.hold(f.level, List.of(f.base));
            var attacker = new MeleeWitness(f.level);
            var victim = EntityType.PIG.create(f.level);
            try {
                GameTestChunkAwait.entityTicking(test, f.base);
                test.assertTrue(victim != null, "real melee victim is available");
                victim.setNoAi(true);
                victim.setNoGravity(true);
                victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                victim.setHealth(100);
                attacker.setNoGravity(true);
                attacker.setPersistenceRequired();
                BlockPos feet = f.base.below();
                attacker.setPos(Vec3.atBottomCenterOf(feet));
                victim.setPos(Vec3.atBottomCenterOf(feet.east()));
                test.assertTrue(f.level.addFreshEntity(victim) && f.level.addFreshEntity(attacker),
                        "native cultist and physical melee victim are admitted");
                attacker.setTarget(victim);
                for (int tick = 0; tick < 100; tick++) {
                    attacker.setPos(Vec3.atBottomCenterOf(feet));
                    attacker.setDeltaMovement(Vec3.ZERO);
                    attacker.setOnGround(true);
                    victim.setPos(Vec3.atBottomCenterOf(feet.east()));
                    victim.setDeltaMovement(Vec3.ZERO);
                    victim.invulnerableTime = 0;
                    f.level.tickNonPassenger(attacker);
                }
                test.assertTrue(victim.getHealth() < 100 && attacker.hits.size() >= 3,
                        "installed real melee goal delivers repeated native damage: " + attacker.hits);
                test.assertTrue(intervalsEqual(attacker.hits, 10),
                        "original ten-tick melee cooldown uses native server ticks: " + attacker.hits);
            } finally {
                attacker.discard();
                if (victim != null) victim.discard();
                for (ChunkPos chunk : held) f.level.setChunkForced(chunk.x, chunk.z, false);
            }
        }
        test.succeed();
    }

    /** The actual Cleric's untouched installed ranged goal fires every thirty ticks at distance twelve. */
    public static void clericRangedCooldownUsesServerTicks(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            List<ChunkPos> held = GolemChunks.hold(f.level, List.of(f.base));
            var cleric = new RangedWitness(f.level);
            AABB area = new AABB(f.base).inflate(4);
            Set<UUID> originalProjectiles = new HashSet<>();
            f.level.getEntitiesOfClass(Projectile.class, area).forEach(entity -> originalProjectiles.add(entity.getUUID()));
            try {
                GameTestChunkAwait.entityTicking(test, f.base);
                BlockPos feet = f.base.below();
                clearShaft(f, feet, 15);
                cleric.setNoGravity(true);
                cleric.setPersistenceRequired();
                cleric.setPos(Vec3.atBottomCenterOf(feet));
                // A vertical lane keeps the twelve-block target inside this fixture's horizontal footprint.
                f.player.setPos(cleric.getX(), cleric.getY() + 12, cleric.getZ());
                test.assertTrue(f.level.addFreshEntity(cleric), "registered Cleric is admitted");
                test.assertTrue(cleric.hasLineOfSight(f.player),
                        "registered Cleric sees its actual survival target through the owned clear shaft: cleric="
                                + cleric.position() + ", target=" + f.player.position());
                cleric.setTarget(f.player);
                for (int tick = 0; tick < 200; tick++) {
                    cleric.setPos(Vec3.atBottomCenterOf(feet));
                    cleric.setDeltaMovement(Vec3.ZERO);
                    cleric.setOnGround(true);
                    f.player.setPos(cleric.getX(), cleric.getY() + 12, cleric.getZ());
                    f.level.tickNonPassenger(cleric);
                }
                long projectiles = f.level.getEntitiesOfClass(Projectile.class, area).stream()
                        .filter(entity -> !originalProjectiles.contains(entity.getUUID())).count();
                test.assertTrue(cleric.shots.size() >= 2 && projectiles >= cleric.shots.size(),
                        "actual installed ranged goal publishes native attack projectiles: ticks="
                                + cleric.shots + ", projectiles=" + projectiles);
                test.assertTrue(intervalsEqual(cleric.shots, 30),
                        "distance twelve keeps the original thirty-server-tick ranged cooldown: " + cleric.shots);
            } finally {
                cleric.discard();
                f.level.getEntitiesOfClass(Projectile.class, area).stream()
                        .filter(entity -> !originalProjectiles.contains(entity.getUUID())).forEach(Entity::discard);
                for (ChunkPos chunk : held) f.level.setChunkForced(chunk.x, chunk.z, false);
            }
        }
        test.succeed();
    }

    /** The original remote-hit path accepts the actual registered Outer Lands biome on ordinary stone. */
    public static void giantRemoteHitSummonsInRegisteredEldritchBiome(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            BlockPos victim = f.base.below(), parent = victim.east(20);
            List<ChunkPos> held = GolemChunks.hold(f.level, List.of(victim, parent));
            var giant = ThaumcraftEldritchMobs.TAINTACLE_GIANT.get().create(f.level);
            AABB area = new AABB(victim).inflate(3);
            Set<UUID> originalSmall = new HashSet<>();
            f.level.getEntitiesOfClass(EntityTaintacleSmall.class, area)
                    .forEach(entity -> originalSmall.add(entity.getUUID()));
            try (var column = new Column(f.level, victim)) {
                GameTestChunkAwait.entityTicking(test, victim);
                GameTestChunkAwait.entityTicking(test, parent);
                test.assertTrue(giant != null, "registered Giant Taintacle is available");
                test.assertTrue(BiomeColumns.set(f.level, victim.getX(), victim.getZ(), ThaumcraftBiomes.ELDRITCH_LANDS)
                                || BiomeColumns.surfaceHolder(f.level, victim.getX(), victim.getZ()).is(ThaumcraftBiomes.ELDRITCH_LANDS),
                        "actual stored surface column has registered eldritch_lands biome");
                f.player.setPos(Vec3.atBottomCenterOf(victim));
                giant.setPos(Vec3.atBottomCenterOf(parent));
                giant.setPersistenceRequired();
                giant.setNoGravity(true);
                test.assertTrue(f.level.addFreshEntity(giant) && f.level.getEntity(giant.getUUID()) == giant,
                        "actual remote parent is admitted and visible in the native level");
                test.assertTrue(giant.distanceTo(f.player) > 16 && f.level.getBlockState(victim).isAir()
                                && f.level.getBlockState(victim.below()).isCollisionShapeFullBlock(f.level, victim.below()),
                        "real remote attacker stands in air over ordinary solid support");
                test.assertTrue(giant.hurt(f.level.damageSources().playerAttack(f.player), 1),
                        "actual remote incoming hit is admitted");
                long added = f.level.getEntitiesOfClass(EntityTaintacleSmall.class, area).stream()
                        .filter(entity -> !originalSmall.contains(entity.getUUID())).count();
                var state = f.level.getBlockState(victim);
                boolean taint = BiomeColumns.surfaceHolder(f.level, victim.getX(), victim.getZ()).is(ThaumcraftBiomes.TAINT);
                test.assertTrue(added == 1 && taint && state.getBlock() instanceof BlockTaintFibres,
                        "remote hit summons one real Small Taintacle and converts Outer Lands support column/fibres: "
                                + "added=" + added + ", taint=" + taint + ", block=" + state);
            } finally {
                if (giant != null) giant.discard();
                f.level.getEntitiesOfClass(EntityTaintacleSmall.class, area).stream()
                        .filter(entity -> !originalSmall.contains(entity.getUUID())).forEach(Entity::discard);
                for (ChunkPos chunk : held) f.level.setChunkForced(chunk.x, chunk.z, false);
            }
        }
        test.succeed();
    }

    /** The registered ordinary Watcher's installed gaze must damage after its ten preparation plus eighty charge ticks. */
    public static void watcherGazeUsesServerTicks(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            List<ChunkPos> held = GolemChunks.hold(f.level, List.of(f.base));
            var watcher = ThaumcraftEldritchMobs.WATCHER.get().create(f.level);
            var victim = EntityType.PIG.create(f.level);
            try {
                GameTestChunkAwait.entityTicking(test, f.base);
                BlockPos feet = f.base.below();
                clearShaft(f, feet, 15);
                test.assertTrue(watcher != null && victim != null, "registered Watcher and native gaze victim exist");
                f.player.setGameMode(GameType.SPECTATOR); // The declared pig is the only attackable living target.
                watcher.setNoGravity(true);
                watcher.setPersistenceRequired();
                watcher.setPos(Vec3.atBottomCenterOf(feet));
                victim.setNoAi(true);
                victim.setNoGravity(true);
                victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                victim.setHealth(100);
                victim.setPos(Vec3.atBottomCenterOf(feet.above(12)));
                test.assertTrue(f.level.addFreshEntity(victim) && f.level.addFreshEntity(watcher)
                                && !watcher.isElder() && watcher.hasLineOfSight(victim),
                        "ordinary native Watcher sees the real victim through the owned clear shaft");
                watcher.setTarget(victim);
                boolean beamStarted = false;
                for (int tick = 0; tick < 110; tick++) {
                    watcher.setPos(Vec3.atBottomCenterOf(feet));
                    watcher.setDeltaMovement(Vec3.ZERO);
                    victim.setPos(Vec3.atBottomCenterOf(feet.above(12)));
                    victim.setDeltaMovement(Vec3.ZERO);
                    victim.invulnerableTime = 0;
                    f.level.tickNonPassenger(watcher);
                    if (watcher.hasTargetedEntity() && watcher.getTargetedEntity() == victim) beamStarted = true;
                }
                test.assertTrue(beamStarted, "actual installed GuardianAttackGoal publishes the native beam target");
                test.assertTrue(victim.getHealth() < 100,
                        "ordinary Watcher completes original preparation/charge and damages within110 native server ticks; health="
                                + victim.getHealth() + ", targetIdActive=" + watcher.hasTargetedEntity());
            } finally {
                if (watcher != null) watcher.discard();
                if (victim != null) victim.discard();
                for (ChunkPos chunk : held) f.level.setChunkForced(chunk.x, chunk.z, false);
            }
        }
        test.succeed();
    }

    private static boolean intervalsEqual(List<Integer> ticks, int expected) {
        for (int index = 1; index < ticks.size(); index++) {
            if (ticks.get(index) - ticks.get(index - 1) != expected) return false;
        }
        return ticks.size() >= 2;
    }

    private static void clearShaft(TubeVariantsRuntimeChecks.Fixture f, BlockPos feet, int height) {
        for (int dy = 0; dy <= height; dy++) {
            BlockPos pos = feet.above(dy);
            f.states.putIfAbsent(pos, f.level.getBlockState(pos));
            BlockEntity tile = f.level.getBlockEntity(pos);
            if (tile != null) f.originals.putIfAbsent(pos, tile);
            f.put(pos, net.minecraft.world.level.block.Blocks.AIR);
        }
    }

    /** Long memory is a declared input to keep the production goal running after path arrival;
     * native Mob ticks still own GoalSelector admission, every-tick policy and the attack counter. */
    private static final class MeleeWitness extends EntityCultist {
        final List<Integer> hits = new ArrayList<>();
        MeleeWitness(Level level) { super(ThaumcraftCultists.CULTIST_KNIGHT.get(), level); }
        @Override protected void registerGoals() { goalSelector.addGoal(0, new CultistMeleeGoal(this, 1, true)); }
        @Override public boolean doHurtTarget(Entity target) {
            hits.add(tickCount);
            return super.doHurtTarget(target);
        }
    }

    private static final class RangedWitness extends EntityCultistCleric {
        final List<Integer> shots = new ArrayList<>();
        RangedWitness(Level level) { super(ThaumcraftCultists.CULTIST_CLERIC.get(), level); }
        @Override public void performRangedAttack(LivingEntity target, float distance) {
            shots.add(tickCount);
            super.performRangedAttack(target, distance);
        }
    }

    /** Preserve every quart Y of just this one native column, including preexisting cave-biome holders. */
    private static final class Column implements AutoCloseable {
        final ServerLevel level;
        final LevelChunk chunk;
        final int x, z;
        final Map<Integer, Holder<Biome>> original = new HashMap<>();
        Column(ServerLevel level, BlockPos position) {
            this.level = level;
            chunk = level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4);
            if (chunk == null) throw new IllegalStateException("owned actor chunk must already be loaded");
            x = QuartPos.fromBlock(position.getX());
            z = QuartPos.fromBlock(position.getZ());
            for (int y = QuartPos.fromBlock(chunk.getMinBuildHeight()); y <= QuartPos.fromBlock(chunk.getMaxBuildHeight() - 1); y++) {
                original.put(y, chunk.getNoiseBiome(x, y, z));
            }
        }
        @Override public void close() {
            chunk.fillBiomesFromNoise((qx, y, qz, sampler) -> qx == x && qz == z
                    ? original.get(y) : chunk.getNoiseBiome(qx, y, qz), level.getChunkSource().randomState().sampler());
            chunk.setUnsaved(true);
            level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
        }
    }
}
