package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftEmbers;
import thaumcraft.common.entities.projectile.EntityEmber;

/** Real registered entities/native ticks and packet codecs; root owns loader wrappers and execution. */
public final class EmberRuntimeChecks {
    private EmberRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 3, 3));
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<BlockPos, CompoundTag> tiles = new LinkedHashMap<>();
        List<EntityEmber> shots = new ArrayList<>();
        List<Mob> targets = new ArrayList<>();
        var oldPosition = player.position();
        var oldMotion = player.getDeltaMovement();
        float yaw = player.getYRot(), pitch = player.getXRot();
        boolean grief = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        for (BlockPos mutable : BlockPos.betweenClosed(pos.offset(-4, -2, -4), pos.offset(4, 3, 4))) {
            BlockPos saved = mutable.immutable();
            states.put(saved, level.getBlockState(saved));
            if (level.getBlockEntity(saved) != null) tiles.put(saved, level.getBlockEntity(saved).saveWithFullMetadata(level.registryAccess()));
        }
        try {
            states.keySet().forEach(clear -> level.setBlock(clear, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
            player.setPos(Vec3.atCenterOf(pos).add(0, 1, -2));
            player.setYRot(0); player.setXRot(0);
            player.setDeltaMovement(10, 5, 0);
            AABB box = new AABB(pos).inflate(6);
            var existing = level.getEntitiesOfClass(EntityEmber.class, box).stream().map(EntityEmber::getUUID).toList();
            test.assertTrue(ThaumcraftEmbers.emit(player, 2, 15, 20, 3), "one canonical emit reports the actual accepted native spawn");
            var emitted = level.getEntitiesOfClass(EntityEmber.class, box, shot -> !existing.contains(shot.getUUID()) && !shot.isRemoved());
            test.assertTrue(emitted.size() == 1, "emit creates exactly one real thaumcraft:ember, not a burst/stand-in");
            EntityEmber first = emitted.getFirst();
            shots.add(first);
            Vec3 muzzle = new Vec3(player.getX() - (double).16F, player.getEyeY() - (double).1F, player.getZ());
            test.assertTrue(first.getType() == ThaumcraftEmbers.EMBER.get() && first.getOwner() == player
                    && first.getDamage() == 2 && first.getScatter() == 15 && first.getDuration() == 20 && first.getFirey() == 0
                    && first.getBbWidth() == .25F && first.getBbHeight() == .25F
                    && close(first.position(), muzzle.add(first.getDeltaMovement())),
                    "native owner, .25 body, basic parameters and final-motion muzzle advance match TC4");
            first.discard();
            player.setPos(Vec3.atCenterOf(pos).add(3, 2, 3));
            player.setDeltaMovement(Vec3.ZERO);

            gaussian(test, level);
            flight(test, player, pos, shots);
            persistence(test, player, pos, shots);
            combat(test, player, pos, shots, targets);
            blocks(test, player, pos, shots);
            test.assertTrue(level.getEntitiesOfClass(EntityEmber.class, box, shot -> shots.contains(shot) && !shot.isRemoved()).isEmpty(),
                    "all emitted/impacted/expired fixture entities are terminal, with no replacement projectile");
        } finally {
            shots.forEach(EntityEmber::discard);
            targets.forEach(Mob::discard);
            states.forEach((saved, state) -> level.setBlock(saved, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
            tiles.forEach((saved, tag) -> {
                if (level.getBlockEntity(saved) != null) level.getBlockEntity(saved).loadWithComponents(tag, level.registryAccess());
            });
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(grief, level.getServer());
            player.setPos(oldPosition); player.setDeltaMovement(oldMotion);
            player.setYRot(yaw); player.setXRot(pitch);
        }
        test.succeed();
    }

    private static void gaussian(GameTestHelper test, ServerLevel level) {
        var shot = new EntityEmber(level);
        shot.getRandom().setSeed(46235L);
        var expectedRandom = RandomSource.create(46235L);
        Vec3 direction = new Vec3(-.2, .1, .3);
        Vec3 expectedFirst = heading(direction, expectedRandom, 1);
        shot.shoot(direction.x, direction.y, direction.z, 1, 1);
        test.assertTrue(close(shot.getDeltaMovement(), expectedFirst), "first heading uses legacy float normalization and X/Y/Z Gaussian error");
        Vec3 expectedSecond = heading(expectedFirst, expectedRandom, 15);
        shot.shoot(expectedFirst.x, expectedFirst.y, expectedFirst.z, 1, 15);
        test.assertTrue(close(shot.getDeltaMovement(), expectedSecond) && shot.getScatter() == 15,
                "second heading consumes the NEXT three Gaussians without triangle noise or extra random draws");
    }

    private static Vec3 heading(Vec3 input, RandomSource random, float scatter) {
        float length = (float)Math.sqrt(input.lengthSqr());
        return new Vec3(input.x / length + random.nextGaussian() * .007499999832361937 * scatter,
                input.y / length + random.nextGaussian() * .007499999832361937 * scatter,
                input.z / length + random.nextGaussian() * .007499999832361937 * scatter);
    }

    private static void flight(GameTestHelper test, ServerPlayer player, BlockPos pos, List<EntityEmber> shots) {
        Vec3 origin = Vec3.atCenterOf(pos.offset(-2, 1, -2));
        Vec3 motion = new Vec3(.2, .05, .1);
        for (int duration : new int[]{20, 30}) {
            EntityEmber shot = spawn(test, player, origin, motion, shots);
            shot.setDuration(duration);
            Vec3 step = motion.scale(duration <= 20 ? .95 : .975);
            shot.tick();
            test.assertTrue(!shot.isRemoved() && close(shot.position(), origin.add(step))
                    && close(shot.getDeltaMovement(), step.scale(.99F)) && shot.getAge() == 1,
                    "real air flight preserves duration-dependent pre-damping, inherited .99F drag and ZERO gravity");
            shot.discard();
        }
        EntityEmber grounded = spawn(test, player, origin, motion, shots);
        grounded.setOnGround(true);
        grounded.tick();
        test.assertTrue(close(grounded.position(), origin.add(motion.scale(.95).scale(.66)))
                && close(grounded.getDeltaMovement(), motion.scale(.95).scale(.66).scale(.99F)),
                "grounded ember applies .66 to ALL three motion coordinates before native flight");
        grounded.discard();
        for (BlockPos water : BlockPos.betweenClosed(pos.offset(-3, 0, -3), pos.offset(-1, 2, -1)))
            player.serverLevel().setBlock(water, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        EntityEmber wet = spawn(test, player, origin, motion, shots);
        wet.tick();
        test.assertTrue(wet.isInWater() && !wet.isRemoved() && close(wet.position(), origin.add(motion.scale(.95)))
                && close(wet.getDeltaMovement(), motion.scale(.95).scale(.8F)),
                "water does not extinguish/remove TC4 ember: native fluid state applies .8F drag, no gravity");
        wet.discard();
        EntityEmber basic = spawn(test, player, Vec3.atCenterOf(pos.offset(-2, 1, 2)), new Vec3(.05, 0, 0), shots);
        for (int tick = 0; tick < 20; ++tick) basic.tick();
        test.assertTrue(!basic.isRemoved() && basic.getAge() == 20, "basic duration20 flies through age20 with the original world pre-tick clock");
        basic.tick();
        test.assertTrue(basic.isRemoved(), "basic duration20 expires before its next movement/collision");
    }

    private static void persistence(GameTestHelper test, ServerPlayer player, BlockPos pos, List<EntityEmber> shots) {
        ServerLevel level = player.serverLevel();
        EntityEmber shot = spawn(test, player, Vec3.atCenterOf(pos.offset(-2, 1, 2)), new Vec3(.1, 0, 0), shots);
        shot.setDuration(30); shot.setDamage(2.5F); shot.setFireSeconds(5);
        shot.getRandom().setSeed(91);
        shot.shoot(1, 0, 0, .1F, 15);
        for (int tick = 0; tick < 3; ++tick) shot.tick();
        var tag = new CompoundTag();
        shot.saveWithoutId(tag);
        var loaded = new EntityEmber(level);
        loaded.load(tag);
        test.assertTrue(loaded.getOwner() == player && loaded.getDamage() == 2.5F && loaded.getFirey() == 2
                && loaded.getDuration() == 30 && loaded.getAge() == 3 && loaded.getScatter() == 15
                && close(loaded.position(), shot.position()) && loaded.getDeltaMovement().equals(shot.getDeltaMovement()),
                "real native NBT preserves UUID-resolved owner, motion, damage/firey/duration, remaining age and launch scatter");

        var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            var tracker = new ServerEntity(level, shot, 20, true, packet -> {});
            var spawn = (ClientboundAddEntityPacket)shot.getAddEntityPacket(tracker);
            ClientboundAddEntityPacket.STREAM_CODEC.encode(wire, spawn);
            var received = ClientboundAddEntityPacket.STREAM_CODEC.decode(wire);
            test.assertTrue(received.getType() == ThaumcraftEmbers.EMBER.get() && received.getData() == player.getId()
                    && Math.abs(received.getXa() - shot.getDeltaMovement().x) <= 1.25e-4,
                    "actual native spawn codec tracks the ember type, real owner ID and quantized velocity");
            var metadata = new ClientboundSetEntityDataPacket(shot.getId(), shot.getEntityData().getNonDefaultValues());
            ClientboundSetEntityDataPacket.STREAM_CODEC.encode(wire, metadata);
            var receivedData = ClientboundSetEntityDataPacket.STREAM_CODEC.decode(wire);
            var mirror = new EntityEmber(level);
            mirror.recreateFromPacket(received);
            mirror.getEntityData().assignValues(receivedData.packedItems());
            test.assertTrue(mirror.getOwner() == player && mirror.getAge() == 3 && mirror.getDuration() == 30,
                    "native entity metadata codec actually restores the renderer's duration/age, not a private test codec");
        } finally { wire.release(); }
        shot.discard();
        loaded.setUUID(UUID.randomUUID());
        shots.add(loaded);
        test.assertTrue(level.addFreshEntity(loaded), "saved projectile returns as an actual native entity fixture");
        for (int tick = 0; tick < 27; ++tick) loaded.tick();
        test.assertTrue(!loaded.isRemoved() && loaded.getAge() == 30, "strict >duration allows the original final flight age30");
        loaded.tick();
        Vec3 expiredPosition = loaded.position();
        loaded.tick(); loaded.tick();
        test.assertTrue(loaded.isRemoved() && loaded.getAge() == 31 && loaded.position().equals(expiredPosition),
                "reloaded lifetime is not reset; expiry is terminal and repeated ticks cannot move, spawn or collide");
    }

    private static void combat(GameTestHelper test, ServerPlayer player, BlockPos pos, List<EntityEmber> shots, List<Mob> targets) {
        // The owner must not stand in the flight path of the ember, or the projectile takes the
        // owner instead of the cow.
        player.setPos(Vec3.atCenterOf(pos).add(3, 2, 3));
        player.setDeltaMovement(Vec3.ZERO);
        Vec3 targetPosition = Vec3.atCenterOf(pos).add(1, -.5, 0);
        Cow cow = target(test, player, EntityType.COW, targetPosition, targets);
        EntityEmber expired = spawn(test, player, targetPosition.add(-2, .7, 0), new Vec3(3, 0, 0), shots);
        expired.setAge(20);
        expired.tick();
        test.assertTrue(expired.isRemoved() && cow.getHealth() == cow.getMaxHealth() && cow.getRemainingFireTicks() <= 0,
                "an already-expired real entity cannot deal a final post-discard collision or ignite a target");
        EntityEmber direct = strike(test, player, targetPosition, shots, 3);
        test.assertTrue(cow.getHealth() == cow.getMaxHealth() - 2 && cow.getRemainingFireTicks() == 60
                && cow.getLastDamageSource() != null && cow.getLastDamageSource().is(DamageTypes.FIREBALL)
                && cow.getLastDamageSource().is(DamageTypeTags.IS_FIRE)
                && cow.getLastDamageSource().getDirectEntity() == direct && cow.getLastDamageSource().getEntity() == player,
                "real entity collision deals exactly2 indirect fireball damage credited to the player and ignites for3 seconds");
        cow.clearFire();
        strike(test, player, targetPosition, shots, 3);
        direct.tick();
        test.assertTrue(cow.getHealth() == cow.getMaxHealth() - 2 && cow.getRemainingFireTicks() == 0,
                "native rejected hurt neither adds damage nor ignites; removed projectile cannot apply duplicate damage");
        cow.discard();

        var immune = target(test, player, EntityType.BLAZE, targetPosition, targets);
        strike(test, player, targetPosition, shots, 3);
        test.assertTrue(immune.fireImmune() && immune.getHealth() == immune.getMaxHealth() && immune.getRemainingFireTicks() <= 0,
                "TC4's explicit fire-immune exclusion prevents both direct damage and ignition");
        immune.discard();

        Cow owner = target(test, player, EntityType.COW, targetPosition, targets);
        EntityEmber returning = spawn(test, player, targetPosition.add(-1.5, .7, 0), new Vec3(3, 0, 0), shots);
        returning.setOwner(owner);
        for (int tick = 0; tick < 4; ++tick) {
            returning.setPos(targetPosition.add(-1.5, .7, 0)); returning.setDeltaMovement(3, 0, 0);
            returning.tick();
            test.assertTrue(!returning.isRemoved() && owner.getHealth() == owner.getMaxHealth(), "thrower is excluded during first four real air ticks");
        }
        returning.setPos(targetPosition.add(-1.5, .7, 0)); returning.setDeltaMovement(3, 0, 0);
        returning.tick();
        test.assertTrue(returning.isRemoved() && owner.getHealth() == owner.getMaxHealth() - 2 && owner.getRemainingFireTicks() == 60,
                "fifth-tick real collision may hit the thrower: no invented permanent owner/vehicle/team immunity");
        owner.discard();
    }

    private static void blocks(GameTestHelper test, ServerPlayer player, BlockPos pos, List<EntityEmber> shots) {
        ServerLevel level = player.serverLevel();
        BlockPos wall = pos.east(), ignition = wall.west();
        level.setBlock(wall, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(ignition.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (boolean grief : new boolean[]{false, true}) {
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(grief, level.getServer());
            level.setBlock(ignition, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            strike(test, player, Vec3.atCenterOf(wall).add(0, -.7, 0), shots, 3);
            test.assertTrue(level.getBlockState(wall).is(Blocks.STONE) && level.isEmptyBlock(ignition),
                    "basic firey0 has ZERO block ignition/explosion, regardless of mobGriefing");
            // firey40 makes the oracle's .025*firey probability1, so this is a
            // deterministic impact-branch fixture, NOT an implemented upgrade.
            strike(test, player, Vec3.atCenterOf(wall).add(0, -.7, 0), shots, 43);
            test.assertTrue(level.getBlockState(wall).is(Blocks.STONE) && level.getBlockState(ignition).is(Blocks.FIRE),
                    "upgrade-level branch ignites only the adjacent hit-face air with normal fire; no mobGriefing gate");
        }
        level.setBlock(ignition, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        strike(test, player, Vec3.atCenterOf(wall).add(0, -.7, 0), shots, 43);
        test.assertTrue(level.getBlockState(ignition).is(Blocks.WATER), "block ignition never replaces occupied fluid, even at probability1");
    }

    private static <T extends Mob> T target(GameTestHelper test, ServerPlayer player, EntityType<T> type, Vec3 position, List<Mob> targets) {
        T target = type.create(player.serverLevel());
        target.setNoAi(true); target.setPos(position);
        targets.add(target);
        test.assertTrue(player.serverLevel().addFreshEntity(target), "real living collision target spawns");
        return target;
    }

    private static EntityEmber spawn(GameTestHelper test, ServerPlayer player, Vec3 position, Vec3 motion, List<EntityEmber> shots) {
        var shot = new EntityEmber(player.serverLevel());
        shot.setOwner(player); shot.setDamage(2); shot.setPos(position); shot.setDeltaMovement(motion);
        shots.add(shot);
        test.assertTrue(player.serverLevel().addFreshEntity(shot), "registered native ember fixture spawns");
        return shot;
    }

    private static EntityEmber strike(GameTestHelper test, ServerPlayer player, Vec3 target, List<EntityEmber> shots, int seconds) {
        EntityEmber shot = spawn(test, player, target.add(-2, .7, 0), new Vec3(3, 0, 0), shots);
        shot.setFireSeconds(seconds);
        shot.tick();
        test.assertTrue(shot.isRemoved(), "real native movement/raycast collision removes the ember on first impact");
        return shot;
    }

    private static boolean close(Vec3 actual, Vec3 expected) { return actual.distanceToSqr(expected) < 1e-20; }
}
