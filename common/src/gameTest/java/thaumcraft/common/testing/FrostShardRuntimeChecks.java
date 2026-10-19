package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftFrost;
import thaumcraft.common.entities.projectile.EntityFrostShard;

/** One compact native case. Unique REAL connected players, initial positions,
 * velocities, targets, armor and a filled chest are explicit SERVER INPUTS, not
 * acquisition/output grants. No RNG seed, test clock, sleeps, iframe resets or
 * manually dispatched protection events. Real ticks/raycast collisions cover
 * flight/combat/ricochets; ONLY the zero/tiny nudge branch is invoked directly.
 * Does not claim client mesh/particles/audio, remote multiplayer or full1.7 parity.
 * Root owns the one Fabric wrapper (timeout600) and centralized execution. */
public final class FrostShardRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private FrostShardRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        try (var check = new Check(test)) {
            check.start();
            check.launchAndFlight();
            check.ricochetsAndPersistence();
            check.combatAndProtections();
            check.blocksAndMotionSafety();
        }
        test.succeed();
    }

    private static final class Check implements AutoCloseable {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final BlockPos base, wall;
        private final Vec3 targetPosition;
        private final AABB bounds;
        private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        private final Map<BlockPos, CompoundTag> tiles = new LinkedHashMap<>();
        private final List<Entity> entities = new ArrayList<>();
        private final List<Connected> players = new ArrayList<>();
        private ServerPlayer owner;
        private boolean touched;

        private Check(GameTestHelper test) {
            this.test = test; level = test.getLevel(); base = test.absolutePos(new BlockPos(3, 3, 3)); wall = base.above(4);
            targetPosition = Vec3.atBottomCenterOf(wall);
            bounds = new AABB(Vec3.atLowerCornerOf(base.offset(-3, 0, -3)), Vec3.atLowerCornerOf(base.offset(4, 14, 4)));
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-3, 0, -3), base.offset(3, 13, 3))) {
                BlockPos pos = mutable.immutable(); blocks.put(pos, level.getBlockState(pos));
                if (level.getBlockEntity(pos) != null) tiles.put(pos, level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()));
            }
        }

        private void start() {
            test.assertTrue(level.getEntities((Entity)null, bounds, entity -> entity.isPickable()
                    || entity instanceof ItemEntity || entity instanceof ExperienceOrb).isEmpty(), "owned Frost lane has no pre-existing targets/items/XP to mutate or merge");
            touched = true; blocks.keySet().forEach(pos -> cell(pos, Blocks.AIR.defaultBlockState()));
            owner = player(GameType.SURVIVAL, Vec3.atCenterOf(base.above(11)).add(2, 0, 2));
            test.assertTrue(owner.getClass() == ServerPlayer.class && !owner.isCreative() && !owner.hasDisconnected()
                    && level.getServer().getPlayerList().getPlayer(owner.getUUID()) == owner, "unique connected native owner is REAL survival, not GameTestHelper's always-creative mock");
        }

        private ServerPlayer player(GameType mode, Vec3 position) {
            var connected = new Connected(level); players.add(connected);
            ServerPlayer player = connected.player;
            level.getServer().getPlayerList().placeNewPlayer(connected.connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            test.assertTrue(player.level() == level, "unique native player connects in this owned test world");
            player.setGameMode(mode); player.setNoGravity(true); player.setPos(position);
            // Expire ordinary login protection via real native ticks, never a
            // private-timer assignment or synthetic damage acceptance setter.
            for (int tick = 0; tick < 61; ++tick) player.tick();
            test.assertTrue(player.isCreative() == (mode == GameType.CREATIVE), "native GameType actually controls creative immunity");
            return player;
        }

        private void launchAndFlight() {
            Vec3 oldPosition = owner.position(), oldMotion = owner.getDeltaMovement();
            owner.setPos(Vec3.atCenterOf(base.above(8))); owner.setYRot(0); owner.setXRot(0); owner.setDeltaMovement(12, 8, 6);
            var launched = new EntityFrostShard(level, owner, 1); entities.add(launched);
            Vec3 muzzle = owner.getEyePosition().add(-(double).16F, -(double).1F, 0);
            test.assertTrue(launched.getType() == ThaumcraftFrost.FROST_SHARD.get() && launched.getOwner() == owner
                    && launched.getDamage() == 3 && launched.getBounceLimit() == 3 && launched.getImpactCount() == 0 && launched.getAge() == 0
                    && launched.getBbWidth() == .6F && launched.getBbHeight() == .6F && sameVector(launched.position(), muzzle)
                    && finite(launched.getDeltaMovement()) && EntityFrostShard.LAUNCH_SPEED == 1.5F,
                    "actual basic constructor has native owner, original muzzle, damage3/.6 body/three remaining bounces and bytecode-confirmed1.5F speed");
            // Zero scatter makes this normalization assertion deterministic while
            // leaving the real entity RNG unseeded and unmodified. Constructor's
            // six-Gaussian draw ordering is source/old-bytecode evidence, not a
            // probabilistic spread histogram masquerading as runtime proof.
            Vec3 input = new Vec3(.2, -.1, .3);
            launched.shoot(input.x, input.y, input.z, 1.5F, 0);
            float length = (float)Math.sqrt(input.lengthSqr());
            test.assertTrue(sameVector(launched.getDeltaMovement(), input.scale(1 / (double)length).scale(1.5F)), "native shoot uses FLOAT normalization and adds NO owner's movement");
            launched.discard(); owner.setPos(oldPosition); owner.setDeltaMovement(oldMotion);

            Vec3 origin = Vec3.atCenterOf(base.above(6)), motion = new Vec3(.2, .1, .15);
            EntityFrostShard air = shot(origin, motion); air.tick();
            test.assertTrue(!air.isRemoved() && air.getImpactCount() == 0 && air.getAge() == 1
                    && sameVector(air.position(), origin.add(motion)) && sameVector(air.getDeltaMovement(), dragged(motion, .99F)), "real native air flight moves THEN .99F drag/.05F gravity, without pre-damping");
            Vec3 finalMotion = dragged(motion, .99F);
            float expectedYaw = (float)(Math.atan2(finalMotion.x, finalMotion.z) * 180 / Math.PI) * .2F;
            float expectedPitch = (float)(Math.atan2(finalMotion.y, (float)Math.sqrt(finalMotion.x * finalMotion.x + finalMotion.z * finalMotion.z)) * 180 / Math.PI) * .2F;
            test.assertTrue(Math.abs(air.getYRot() - expectedYaw) < 1e-5F && Math.abs(air.getXRot() - expectedPitch) < 1e-5F,
                    "legacy .2F yaw/pitch smoothing follows final dragged/gravity-adjusted motion, not modern launch rotation");
            air.discard();
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, 5, -1), base.offset(1, 7, 1))) cell(pos, Blocks.WATER.defaultBlockState());
            var wet = shot(origin, motion); wet.tick();
            test.assertTrue(wet.isInWater() && !wet.isRemoved() && sameVector(wet.position(), origin.add(motion))
                    && sameVector(wet.getDeltaMovement(), dragged(motion, .8F)), "real still-water state uses inherited .8F drag followed by .05F gravity, not extinguishing or freezing");
            wet.discard();
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, 5, -1), base.offset(1, 7, 1))) {
                test.assertTrue(level.getBlockState(pos).is(Blocks.WATER), "BASIC frost0 does not freeze water"); cell(pos, Blocks.AIR.defaultBlockState());
            }
            var idle = shot(origin, Vec3.ZERO); idle.setNoGravity(true); // Bounded, declared no-gravity longevity INPUT.
            for (int tick = 0; tick < 1201; ++tick) idle.tick();
            test.assertTrue(!idle.isRemoved() && idle.getAge() == 1201 && idle.getImpactCount() == 0 && sameVector(idle.position(), origin), "no invented air TTL, even past1200 native projectile ticks");
            idle.discard();
        }

        private void ricochetsAndPersistence() {
            cell(wall, Blocks.STONE.defaultBlockState());
            for (Direction face : Direction.values()) {
                Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
                Vec3 incoming = new Vec3(face.getAxis() == Direction.Axis.X ? -normal.x * 1.3 : .1,
                        face.getAxis() == Direction.Axis.Y ? -normal.y * 1.3 : .2,
                        face.getAxis() == Direction.Axis.Z ? -normal.z * 1.3 : .15);
                Vec3 start = Vec3.atCenterOf(wall).add(normal.scale(1.1));
                var bounced = shot(start, incoming); bounced.tick();
                Vec3 outgoing = reflected(incoming, face.getAxis()).scale(.5);
                test.assertTrue(!bounced.isRemoved() && bounced.getImpactCount() == 1 && bounced.getBounceLimit() == 2 && bounced.hurtMarked
                        && sameVector(bounced.position(), nudged(start, outgoing).add(outgoing)) && sameVector(bounced.getDeltaMovement(), dragged(outgoing, .99F)),
                        "native " + face + " collision reflects ONLY its face axis (Y*-.9), *.5, SUBTRACT-nudges, moves with POST-bounce motion, then drags/gravitates");
                bounced.discard();
            }
            Vec3 start = Vec3.atCenterOf(wall).add(-1.1, 0, 0), incoming = new Vec3(1.3, .1, .15);
            var saved = shot(start, incoming);
            for (int impact = 1; impact <= 3; ++impact) {
                saved.setPos(start); saved.setDeltaMovement(incoming); saved.tick();
                test.assertTrue(!saved.isRemoved() && saved.getImpactCount() == impact && saved.getBounceLimit() == 3 - impact, "actual impact" + impact + " survives the original post-decrement limit");
            }
            saved.setDamage(2.5F); // Explicit nondefault metadata/NBT INPUT, not a focus upgrade.
            var tag = new CompoundTag(); saved.saveWithoutId(tag);
            var loaded = new EntityFrostShard(level); entities.add(loaded); loaded.load(tag);
            test.assertTrue(loaded.getUUID().equals(saved.getUUID()) && loaded.getOwner() == owner && loaded.getAge() == 3
                    && loaded.getImpactCount() == 3 && loaded.getBounceLimit() == 0 && loaded.getDamage() == 2.5F
                    && loaded.getBbWidth() == .15F + 2.5F * .15F && sameVector(loaded.position(), saved.position())
                    && loaded.getDeltaMovement().equals(saved.getDeltaMovement()) && !tag.getBoolean("fragile") && tag.getInt("frost") == 0,
                    "native NBT preserves projectile UUID/owner/motion, dynamic size/damage, age AND already-earned three impacts; basic flags remain false/0");
            packets(saved);
            saved.discard();
            // The same-tick test index may retain a discarded UUID; a unique
            // native identity is the respawn fixture, NOT a bounce/age reset.
            loaded.setUUID(UUID.randomUUID()); test.assertTrue(level.addFreshEntity(loaded), "saved state is returned to the ACTUAL native entity index");
            loaded.setPos(start); loaded.setDeltaMovement(incoming); loaded.tick();
            Vec3 terminal = loaded.position(); int age = loaded.getAge(); loaded.tick(); loaded.tick();
            test.assertTrue(loaded.isRemoved() && loaded.getImpactCount() == 4 && loaded.getBounceLimit() == -1
                    && loaded.getAge() == age && loaded.position().equals(terminal), "FOURTH actual collision after reload is terminal; repeat ticks cannot move, collide, damage or rejuvenate it");
            cell(wall, Blocks.AIR.defaultBlockState());
        }

        private void packets(EntityFrostShard shot) {
            var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            try {
                var tracker = new ServerEntity(level, shot, 20, true, packet -> {});
                ClientboundAddEntityPacket.STREAM_CODEC.encode(wire, (ClientboundAddEntityPacket)shot.getAddEntityPacket(tracker));
                var spawn = ClientboundAddEntityPacket.STREAM_CODEC.decode(wire);
                ClientboundSetEntityDataPacket.STREAM_CODEC.encode(wire, new ClientboundSetEntityDataPacket(shot.getId(), shot.getEntityData().getNonDefaultValues()));
                var metadata = ClientboundSetEntityDataPacket.STREAM_CODEC.decode(wire);
                var mirror = new EntityFrostShard(level); mirror.recreateFromPacket(spawn); mirror.getEntityData().assignValues(metadata.packedItems());
                test.assertTrue(spawn.getType() == ThaumcraftFrost.FROST_SHARD.get() && spawn.getData() == owner.getId()
                        && mirror.getOwner() == owner && mirror.getDamage() == 2.5F && mirror.getImpactCount() == 3 && mirror.getBounceLimit() == 0
                        && mirror.getAge() == 3 && mirror.getBbWidth() == shot.getBbWidth() && Math.abs(spawn.getXa() - shot.getDeltaMovement().x) <= 1.25e-4,
                        "ACTUAL native spawn/metadata codecs carry type/owner/motion and nondefault damage/dynamic size/counter/age");
                ClientboundSetEntityMotionPacket.STREAM_CODEC.encode(wire, new ClientboundSetEntityMotionPacket(shot));
                var motion = ClientboundSetEntityMotionPacket.STREAM_CODEC.decode(wire);
                test.assertTrue(motion.getId() == shot.getId() && Math.abs(motion.getXa() - shot.getDeltaMovement().x) <= 1.25e-4,
                        "hurtMarked bounce has a real native motion packet, not a custom projectile protocol");
            } finally { wire.release(); }
        }

        private void combatAndProtections() {
            Cow cow = target(EntityType.COW);
            var hit = strike(cow);
            test.assertTrue(cow.getHealth() == cow.getMaxHealth() - 3 && cow.getLastDamageSource() != null
                    && cow.getLastDamageSource().is(DamageTypes.THROWN) && cow.getLastDamageSource().is(DamageTypeTags.IS_PROJECTILE)
                    && !cow.getLastDamageSource().is(DamageTypeTags.BYPASSES_ARMOR) && !cow.getLastDamageSource().is(DamageTypeTags.IS_FIRE)
                    && cow.getLastDamageSource().getDirectEntity() == hit && cow.getLastDamageSource().getEntity() == owner
                    && !cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && cow.getTicksFrozen() == 0 && cow.getRemainingFireTicks() <= 0,
                    "native collision applies exactly3 ordinary owner-attributed THROWN damage, with no armor bypass/fire/slow/freeze");
            int immunity = cow.invulnerableTime; float health = cow.getHealth(); strike(cow);
            test.assertTrue(cow.getHealth() == health && cow.invulnerableTime == immunity, "second real collision respects native immunity frames without resetting them"); cow.discard();

            var armored = target(EntityType.ZOMBIE);
            armored.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET)); armored.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            armored.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS)); armored.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS)); armored.tick();
            strike(armored);
            test.assertTrue(armored.getArmorValue() >= 20 && armored.getHealth() < armored.getMaxHealth() && armored.getHealth() > armored.getMaxHealth() - 3, "actual equipped native armor/toughness reduces Frost's ordinary three-point damage"); armored.discard();

            Cow refused = target(EntityType.COW); int[] refusals = {0};
            EntityEvent.LivingHurt protection = (victim, source, amount) -> {
                if (victim != refused || source.getEntity() != owner) return EventResult.pass();
                ++refusals[0]; test.assertTrue(source.is(DamageTypes.THROWN) && source.getDirectEntity() instanceof EntityFrostShard && amount == 3, "actual native hurt protection sees original thrown damage3");
                return EventResult.interruptFalse();
            };
            EntityEvent.LIVING_HURT.register(protection);
            try {
                var rejected = strike(refused);
                test.assertTrue(refusals[0] == 1 && refused.getHealth() == refused.getMaxHealth() && rejected.getImpactCount() == 1 && rejected.getBounceLimit() == 2,
                        "REAL native damage refusal still reflects, bounces and consumes one impact");
            } finally { EntityEvent.LIVING_HURT.unregister(protection); refused.discard(); }

            Cow thrower = target(EntityType.COW); var returning = shot(targetPosition.add(-1.8, .7, 0), new Vec3(3, 0, 0)); returning.setOwner(thrower);
            for (int tick = 1; tick <= 5; ++tick) {
                returning.setPos(targetPosition.add(-1.8, .7, 0)); returning.setDeltaMovement(3, 0, 0); returning.tick();
                test.assertTrue(returning.getAge() == tick && thrower.getHealth() == thrower.getMaxHealth() - (tick == 5 ? 3 : 0)
                        && returning.getImpactCount() == (tick == 5 ? 1 : 0), "owner-only grace excludes thrower for4 actual air ticks, but native collision hits it at5");
            }
            returning.discard(); thrower.discard();

            Cow axes = target(EntityType.COW); axes.setInvulnerable(true);
            Vec3[] offsets = {new Vec3(-1.8, .7, 0), new Vec3(-1.8, 1.1, -1.1), new Vec3(.1, .7, -1.7)};
            Vec3[] motions = {new Vec3(2.2, .2, .15), new Vec3(2.2, -.1, 1.4), new Vec3(.1, .1, 2.2)};
            for (int axisCase = 0; axisCase < offsets.length; ++axisCase) {
                Vec3 start = targetPosition.add(offsets[axisCase]), incoming = motions[axisCase];
                var shot = shot(start, incoming); shot.tick();
                Vec3 outgoing = new Vec3(axisCase < 2 ? -incoming.x : incoming.x, axisCase == 1 ? incoming.y * -.9 : incoming.y,
                        axisCase > 0 ? -incoming.z : incoming.z).scale(.66).scale(.5);
                test.assertTrue(shot.getImpactCount() == 1 && sameVector(shot.position(), nudged(start, outgoing).add(outgoing))
                        && sameVector(shot.getDeltaMovement(), dragged(outgoing, .99F)), "native ENTITY impact uses floor(CURRENT position)-floor(target), not contact point/normal; per-axis reflection then ALL*.66*.5 case" + axisCase);
                shot.discard();
            }
            axes.discard();
            pvp(false, false, false); pvp(true, false, false); pvp(true, true, false); pvp(true, false, true);
        }

        private void pvp(boolean enabled, boolean friendlyRefusal, boolean creative) {
            var server = level.getServer(); boolean oldPvp = server.isPvpAllowed();
            ServerPlayer victim = player(creative ? GameType.CREATIVE : GameType.SURVIVAL, targetPosition);
            var scoreboard = server.getScoreboard();
            var team = friendlyRefusal ? scoreboard.addPlayerTeam("frost_" + UUID.randomUUID().toString().substring(0, 8)) : null;
            try {
                server.setPvpAllowed(enabled);
                if (team != null) {
                    team.setAllowFriendlyFire(false); scoreboard.addPlayerToTeam(owner.getScoreboardName(), team); scoreboard.addPlayerToTeam(victim.getScoreboardName(), team);
                }
                float health = victim.getHealth(); var shot = strike(victim);
                test.assertTrue(victim.getHealth() == health - (enabled && !friendlyRefusal && !creative ? 3 : 0)
                        && shot.getImpactCount() == 1 && shot.getBounceLimit() == 2, "REAL connected-player native PvP=" + enabled + "/friendlyRefusal=" + friendlyRefusal + "/creative=" + creative + " preserves damage protections AND bounce");
            } finally {
                server.setPvpAllowed(oldPvp); if (team != null) scoreboard.removePlayerTeam(team);
                players.stream().filter(peer -> peer.player == victim).findFirst().orElseThrow().close();
            }
        }

        private void blocksAndMotionSafety() {
            for (BlockState state : List.of(Blocks.CHEST.defaultBlockState(), Blocks.TARGET.defaultBlockState(), Blocks.ICE.defaultBlockState())) {
                cell(wall, state);
                if (level.getBlockEntity(wall) instanceof ChestBlockEntity chest) chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
                CompoundTag before = level.getBlockEntity(wall) == null ? null : level.getBlockEntity(wall).saveWithFullMetadata(level.registryAccess());
                var shot = shot(Vec3.atCenterOf(wall).add(-1.1, 0, 0), new Vec3(1.3, 0, 0)); shot.tick();
                test.assertTrue(shot.getImpactCount() == 1 && level.getBlockState(wall).equals(state)
                        && (before == null || before.equals(level.getBlockEntity(wall).saveWithFullMetadata(level.registryAccess()))), "real block impact neither mines/changes blocks/contents nor invokes originally-absent modern target activation");
                shot.discard(); cell(wall, Blocks.AIR.defaultBlockState());
            }
            Vec3 origin = Vec3.atCenterOf(base.above(6));
            for (Vec3 motion : List.of(Vec3.ZERO, new Vec3(1e-30, -1e-30, 0), new Vec3(Double.NaN, Double.POSITIVE_INFINITY, 0))) {
                var guarded = shot(origin, Vec3.ZERO); guarded.setNoGravity(true); guarded.setDeltaMovement(motion); guarded.tick();
                test.assertTrue(finite(guarded.position()) && finite(guarded.getDeltaMovement()) && guarded.getAge() == 1, "zero/tiny/invalid native flight inputs stay finite before collision queries");
                guarded.setDeltaMovement(motion);
                // Explicit safety-branch unit fixture: a zero-length native ray
                // has no contact, so this is NOT claimed as an actual collision.
                nudgeGuard(guarded);
                test.assertTrue(finite(guarded.position()) && finite(guarded.getDeltaMovement()) && guarded.getImpactCount() == 1,
                        "direct zero/tiny/invalid impact SAFETY fixture never divides by zero/underflow or produces NaN");
                guarded.discard();
            }
            test.assertTrue(level.getEntities((Entity)null, bounds, entity -> entity instanceof ItemEntity || entity instanceof ExperienceOrb).isEmpty(), "BASIC nonlethal impacts produce no block/container items or XP outputs");
        }

        private void nudgeGuard(EntityFrostShard shot) {
            try {
                var method = EntityFrostShard.class.getDeclaredMethod("onHit", HitResult.class); method.setAccessible(true);
                method.invoke(shot, new BlockHitResult(shot.position(), Direction.WEST, wall, false));
            } catch (InvocationTargetException failure) {
                if (failure.getCause() instanceof RuntimeException runtime) throw runtime;
                if (failure.getCause() instanceof Error error) throw error;
                throw new IllegalStateException("Frost nudge safety fixture", failure.getCause());
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Frost nudge safety fixture", failure); }
        }

        private <T extends Mob> T target(EntityType<T> type) {
            T target = type.create(level); entities.add(target); target.setNoAi(true); target.setNoGravity(true); target.setPos(targetPosition);
            test.assertTrue(level.addFreshEntity(target), "actual native collision target is indexed"); return target;
        }

        private EntityFrostShard shot(Vec3 position, Vec3 motion) {
            var shot = new EntityFrostShard(level); entities.add(shot); shot.setOwner(owner); shot.setDamage(3); shot.setPos(position); shot.setDeltaMovement(motion);
            test.assertTrue(level.addFreshEntity(shot), "real registered thaumcraft:frost_shard fixture spawns once"); return shot;
        }

        private EntityFrostShard strike(Entity target) {
            var shot = shot(target.position().add(-1.8, .7, 0), new Vec3(3, 0, 0)); shot.tick();
            test.assertTrue(!shot.isRemoved() && shot.getImpactCount() == 1 && shot.getBounceLimit() == 2 && shot.getDeltaMovement().x < 0, "real native flight/raycast hits and ricochets instead of a protected onHit-only test");
            return shot;
        }

        private void cell(BlockPos pos, BlockState state) {
            if (level.getBlockEntity(pos) instanceof Container container) container.clearContent();
            level.setBlock(pos, state, QUIET);
        }

        @Override public void close() {
            try {
                entities.forEach(Entity::discard);
                if (touched) {
                    blocks.forEach(this::cell);
                    tiles.forEach((pos, tag) -> { if (level.getBlockEntity(pos) != null) level.getBlockEntity(pos).loadWithComponents(tag, level.registryAccess()); });
                }
            } finally { players.forEach(Connected::close); }
        }
    }

    private static final class Connected implements AutoCloseable {
        private final ServerLevel level;
        private final ServerPlayer player;
        private final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        private final EmbeddedChannel channel = new EmbeddedChannel(connection);
        private boolean closed;

        private Connected(ServerLevel level) {
            this.level = level;
            var profile = new GameProfile(UUID.randomUUID(), "fr-" + UUID.randomUUID().toString().substring(0, 8));
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        }

        @Override public void close() {
            if (closed) return; closed = true;
            try {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
            } finally {
                try { player.discard(); }
                finally { channel.finishAndReleaseAll(); }
            }
        }
    }

    private static Vec3 reflected(Vec3 motion, Direction.Axis axis) {
        return new Vec3(axis == Direction.Axis.X ? -motion.x : motion.x, axis == Direction.Axis.Y ? motion.y * -.9 : motion.y, axis == Direction.Axis.Z ? -motion.z : motion.z);
    }
    private static Vec3 nudged(Vec3 position, Vec3 outgoing) { return position.subtract(outgoing.scale((double).05F / (float)Math.sqrt(outgoing.lengthSqr()))); }
    private static Vec3 dragged(Vec3 motion, float drag) { return motion.scale(drag).add(0, -(double).05F, 0); }
    private static boolean sameVector(Vec3 actual, Vec3 expected) { return actual.distanceToSqr(expected) < 1e-18; }
    private static boolean finite(Vec3 value) { return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z); }
}
