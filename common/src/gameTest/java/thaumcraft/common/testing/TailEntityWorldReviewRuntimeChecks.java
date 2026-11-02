package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.entities.monster.EntityThaumicSlime;
import thaumcraft.common.entities.monster.EntityWatcher;
import thaumcraft.common.lib.world.StreamAxeItemMotion;

/** Native entity AI, real death loot and a survival-chopped item's disk boundary. */
public final class TailEntityWorldReviewRuntimeChecks {
    private TailEntityWorldReviewRuntimeChecks() {}

    public static void watcherNativeControlsProcessQueuedCommands(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            try (var chunk = new TickingChunk(f)) {
                // Remove unrelated random/attack goals, retaining the actual Watcher constructor,
                // controls, navigation, native Mob.serverAiStep and entity tick.
                var watcher = new CommandWatcher(f.level);
                var target = EntityType.PIG.create(f.level);
                try {
                    test.assertTrue(target != null, "native control target exists");
                    target.setNoAi(true); target.setNoGravity(true);
                    watcher.setNoGravity(true); watcher.setOnGround(true);
                    watcher.setPos(Vec3.atBottomCenterOf(f.base));
                    target.setPos(Vec3.atBottomCenterOf(f.base.east(3)));
                    watcher.setYRot(0); watcher.setYHeadRot(0);
                    test.assertTrue(f.level.addFreshEntity(target) && f.level.addFreshEntity(watcher),
                            "actual native Watcher and target are admitted in an entity-ticking chunk");
                    watcher.setTarget(target);
                    var route = new Path(List.of(new Node(f.base.getX(), f.base.getY(), f.base.getZ()),
                            new Node(f.base.getX() + 3, f.base.getY(), f.base.getZ())), f.base.east(3), true);
                    test.assertTrue(watcher.getNavigation().moveTo(route, 1D) && !watcher.getNavigation().isDone(),
                            "a real pending native navigation path is supplied");
                    watcher.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1D);
                    watcher.getLookControl().setLookAt(target, 90F, 90F);
                    f.level.tickNonPassenger(watcher);
                    test.assertTrue(watcher.isGazing() && watcher.getSpeed() > 0 && watcher.getYHeadRot() < -5F,
                            "native Mob tick processes the Watcher controls returned to navigation/goals; gazing="
                                    + watcher.isGazing() + ", speed=" + watcher.getSpeed() + ", head=" + watcher.getYHeadRot());
                } finally { watcher.discard(); if (target != null) target.discard(); }
            }
        }
        test.succeed();
    }

    public static void watcherRetainsOriginalFireImmunity(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var watcher = ThaumcraftEldritchMobs.WATCHER.get().create(f.level);
            var control = EntityType.PIG.create(f.level);
            try {
                test.assertTrue(watcher != null && control != null, "registered Watcher and vanilla fire control exist");
                watcher.setPos(Vec3.atBottomCenterOf(f.base));
                control.setPos(Vec3.atBottomCenterOf(f.base.east()));
                test.assertTrue(f.level.addFreshEntity(watcher) && f.level.addFreshEntity(control), "native fire targets are admitted");
                test.assertTrue(control.hurt(f.level.damageSources().onFire(), 2F), "ordinary native fire damage is admitted");
                float before = watcher.getHealth();
                boolean damaged = watcher.hurt(f.level.damageSources().onFire(), 2F);
                test.assertTrue(watcher.fireImmune() && !damaged && watcher.getHealth() == before,
                        "the original registered Watcher is fire immune; native flag=" + watcher.fireImmune()
                                + ", admitted=" + damaged + ", health=" + watcher.getHealth());
            } finally { if (watcher != null) watcher.discard(); if (control != null) control.discard(); }
        }
        test.succeed();
    }

    public static void slimeSpitsAtPlayerEightBlocksAway(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            try (var chunk = new TickingChunk(f)) {
                var slime = ThaumcraftEldritchMobs.THAUMIC_SLIME.get().create(f.level);
                var owned = new ArrayList<Entity>();
                try {
                    test.assertTrue(slime != null && f.level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL,
                            "a registered native slime can run its ordinary server AI");
                    slime.setSlimeSize(4); slime.setNoGravity(true); slime.setOnGround(false);
                    slime.setPos(Vec3.atBottomCenterOf(f.base));
                    f.player.setPos(slime.position().add(8, 0, 0));
                    test.assertTrue(f.level.addFreshEntity(slime), "native size-four slime is admitted");
                    owned.add(slime);
                    test.assertTrue(slime.distanceTo(f.player) == 8F, "actual connected survival actor is eight blocks away");
                    var before = f.level.getEntitiesOfClass(EntityThaumicSlime.class, slime.getBoundingBox().inflate(4))
                            .stream().map(Entity::getUUID).toList();
                    for (int tick = 0; tick < 110; tick++) f.level.tickNonPassenger(slime);
                    var children = f.level.getEntitiesOfClass(EntityThaumicSlime.class, slime.getBoundingBox().inflate(4))
                            .stream().filter(other -> !before.contains(other.getUUID())).toList();
                    owned.addAll(children);
                    test.assertTrue(slime.getSlimeSize() == 3 && children.size() == 1 && children.getFirst().getSlimeSize() == 1,
                            "native AI reaches the original hundred-tick spit at a player inside sixteen blocks; parent="
                                    + slime.getSlimeSize() + ", new slimes=" + children.size());
                } finally { owned.forEach(Entity::discard); if (slime != null) slime.discard(); }
            }
        }
        test.succeed();
    }

    public static void smallSlimeDeathDropsTaintedGoo(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var slimes = new ArrayList<EntityThaumicSlime>();
            try {
                test.assertTrue(f.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT), "native mob loot is enabled");
                // Sixty-four explicit entity RNG seeds exercise the genuine death path. No loot
                // callback or drop method is replaced, and no item is supplied as output.
                for (int seed = 0; seed < 64; seed++) {
                    var slime = ThaumcraftEldritchMobs.THAUMIC_SLIME.get().create(f.level);
                    test.assertTrue(slime != null, "registered small slime exists");
                    slimes.add(slime); slime.setSlimeSize(1); slime.setNoAi(true);
                    slime.setPos(Vec3.atBottomCenterOf(f.base));
                    test.assertTrue(f.level.addFreshEntity(slime), "native small slime death input is admitted");
                    slime.getRandom().setSeed(seed);
                    test.assertTrue(slime.hurt(f.level.damageSources().genericKill(), 1000F) && slime.getHealth() <= 0,
                            "actual native lethal damage reaches death loot");
                }
                var goo = ThaumcraftContent.MATERIALS.get("tainted_goo").get();
                int count = f.drops().stream().filter(drop -> drop.getItem().is(goo)).mapToInt(drop -> drop.getItem().getCount()).sum();
                test.assertTrue(count > 0 && count < 64,
                        "the original one-in-three small-slime reward resolves to native tainted goo; actual drops=" + count);
            } finally { slimes.forEach(Entity::discard); f.drops(); }
        }
        test.succeed();
    }

    public static void axeChoppedDropReloadsWithOrdinaryGravity(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            try (var chunk = new TickingChunk(f)) {
                ItemEntity restored = null;
                try {
                    f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftEquipment.ELEMENTAL_AXE.get()));
                    for (int height = 0; height < 3; height++) f.put(f.base.above(height), Blocks.OAK_LOG);
                    test.assertTrue(f.level.mayInteract(f.player, f.base), "survival chop is outside unrelated spawn protection");
                    boolean ordinary = f.player.gameMode.destroyBlock(f.base);
                    var logs = f.drops().stream().filter(drop -> drop.getItem().is(Items.OAK_LOG)).toList();
                    test.assertTrue(!ordinary && f.level.getBlockState(f.base).is(Blocks.OAK_LOG)
                                    && f.level.getBlockState(f.base.above(2)).isAir() && logs.size() == 1,
                            "actual survival stream-axe chop produces one genuine endpoint log");
                    var log = logs.getFirst();
                    test.assertTrue(log instanceof StreamAxeItemMotion.Following following && following.thaumcraft$streamFollowing(),
                            "the genuine newly chopped log starts its transient follow");
                    f.player.setPos(Vec3.atBottomCenterOf(f.base).add(7, 0, 0));
                    f.level.tickNonPassenger(log);
                    test.assertTrue(log.isNoGravity(), "native following tick enters actual airborne approach");
                    var saved = log.saveWithoutId(new CompoundTag());
                    restored = EntityType.ITEM.create(f.level);
                    test.assertTrue(restored != null, "ordinary native item reload target exists");
                    restored.load(saved); restored.setUUID(UUID.randomUUID()); log.discard();
                    test.assertTrue(ItemStack.matches(restored.getItem(), new ItemStack(Items.OAK_LOG))
                                    && restored instanceof StreamAxeItemMotion.Following following && !following.thaumcraft$streamFollowing(),
                            "native disk reload preserves the real log and leaves no saved transient follow target");
                    restored.setPos(Vec3.atCenterOf(f.base.above(3))); restored.setDeltaMovement(Vec3.ZERO);
                    test.assertTrue(f.level.addFreshEntity(restored), "reloaded native log is admitted with its own UUID");
                    f.level.tickNonPassenger(restored);
                    test.assertTrue(!restored.isNoGravity() && restored.getDeltaMovement().y < 0,
                            "a reloaded former following drop resumes ordinary native falling; NoGravity="
                                    + restored.isNoGravity() + ", velocity=" + restored.getDeltaMovement());
                    // An intentionally gravity-free ordinary item retains vanilla persistence.
                    var intentional = new ItemEntity(f.level, 0, 0, 0, new ItemStack(Items.DIAMOND), 0, 0, 0);
                    intentional.setNoGravity(true);
                    var copy = EntityType.ITEM.create(f.level);
                    test.assertTrue(copy != null, "ordinary persistence control exists");
                    try {
                        copy.load(intentional.saveWithoutId(new CompoundTag()));
                        test.assertTrue(copy.isNoGravity(), "ordinary explicit NoGravity remains a vanilla saved property");
                        BlockPos endpoint = f.base.above(3);
                        Set<Integer> before = StreamAxeItemMotion.idsAround(f.level, endpoint);
                        intentional.setPos(Vec3.atCenterOf(endpoint));
                        test.assertTrue(f.level.addFreshEntity(intentional)
                                        && StreamAxeItemMotion.followNewDrops(f.player, endpoint, before) == 1,
                                "an intentionally gravity-free native item also enters the real new-drop follow path");
                        f.level.tickNonPassenger(intentional);
                        copy.load(intentional.saveWithoutId(new CompoundTag()));
                        test.assertTrue(copy.isNoGravity(), "following preserves intentional NoGravity in native disk data");
                        f.player.setPos(intentional.getX(), intentional.getY() - f.player.getBbHeight() / 2D, intentional.getZ());
                        f.level.tickNonPassenger(intentional);
                        test.assertTrue(intentional instanceof StreamAxeItemMotion.Following following
                                        && !following.thaumcraft$streamFollowing() && intentional.isNoGravity(),
                                "arrival restores the item's original intentional NoGravity flag");
                    } finally { intentional.discard(); copy.discard(); }
                } finally { if (restored != null) restored.discard(); f.drops(); }
            }
        }
        test.succeed();
    }

    private static final class CommandWatcher extends EntityWatcher {
        CommandWatcher(net.minecraft.world.level.Level level) { super(ThaumcraftEldritchMobs.WATCHER.get(), level); }
        @Override protected void registerGoals() {}
    }

    private static final class TickingChunk implements AutoCloseable {
        private final TubeVariantsRuntimeChecks.Fixture fixture;
        private final ChunkPos pos;
        private final boolean owned;
        TickingChunk(TubeVariantsRuntimeChecks.Fixture fixture) {
            this.fixture = fixture; pos = new ChunkPos(fixture.base);
            owned = !fixture.level.getForcedChunks().contains(pos.toLong());
            if (owned) fixture.level.setChunkForced(pos.x, pos.z, true);
            try { GameTestChunkAwait.entityTicking(fixture.test, fixture.base); }
            catch (RuntimeException | Error failure) { close(); throw failure; }
        }
        @Override public void close() { if (owned) fixture.level.setChunkForced(pos.x, pos.z, false); }
    }
}
