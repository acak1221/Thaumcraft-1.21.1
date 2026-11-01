package thaumcraft.common.testing;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.config.ThaumcraftOuterLands;
import thaumcraft.common.lib.mods.ChampionHooks;
import thaumcraft.common.lib.world.DungeonRooms;

/** Real registered entities exercise room RNG custody and persisted boss scaling (R02/R17). */
public final class ReviewWorldBossRuntimeChecks {
    private ReviewWorldBossRuntimeChecks() {}

    public static void loadedBossKeepsHealthRatio(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            lane.put(lane.origin, Blocks.AIR.defaultBlockState()); // Own the Warden's field footprint too.
            for (var type : List.<EntityType<? extends Mob>>of(
                    ThaumcraftEldritchMobs.ELDRITCH_WARDEN.get(), ThaumcraftEldritchMobs.ELDRITCH_GOLEM.get())) {
                Mob boss = lane.add(type.create(lane.level));
                boss.setNoAi(true);
                boss.moveTo(lane.origin.getX() + .5, lane.origin.getY(), lane.origin.getZ() + .5, 0, 0);
                float base = boss.getMaxHealth();
                boss.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath("thaumcraft", "health_buff_1"), 50,
                        AttributeModifier.Operation.ADD_VALUE));
                boss.setHealth((base + 50) * .4F);
                CompoundTag saved = boss.saveWithoutId(new CompoundTag());
                Mob restored = lane.add(type.create(lane.level));
                restored.load(saved);
                restored.setNoAi(true);
                restored.setNoGravity(true);
                test.assertTrue(Math.abs(restored.getMaxHealth() - base - 50) < .001F,
                        "native entity load restores the persistent multiplayer health modifier");
                restored.setTarget(test.makeMockServerPlayerInLevel());
                // ServerLevel owns the tickCount increment; calling Entity.tick directly leaves it at19.
                restored.tickCount = 19; // Shared boss scaling runs at tick20; no passive heal at tick30.
                lane.level.tickNonPassenger(restored);
                test.assertTrue(Math.abs(restored.getMaxHealth() - base) < .001F
                                && Math.abs(restored.getHealth() - base * .4F) < .001F,
                        "first scaling after native NBT load preserves 40 percent health for " + type
                                + "; base=" + base + ", actualMax=" + restored.getMaxHealth()
                                + ", actualHealth=" + restored.getHealth() + ", tick=" + restored.tickCount
                                + ", noAi=" + restored.isNoAi() + ", target=" + restored.getTarget());
                restored.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath("thaumcraft", "health_buff_2"), 50,
                        AttributeModifier.Operation.ADD_VALUE));
                restored.setHealth((base + 50) * .2F);
                restored.tickCount = 39;
                lane.level.tickNonPassenger(restored);
                test.assertTrue(Math.abs(restored.getHealth() - base * .2F) < .001F,
                        "a subsequent external maximum change also preserves its actual health ratio"
                                + "; base=" + base + ", actualMax=" + restored.getMaxHealth()
                                + ", actualHealth=" + restored.getHealth() + ", tick=" + restored.tickCount);
            }
        } finally { lane.clean(); }
        test.succeed();
    }

    public static void roomSpawnsUseOnlyTheirDeterministicStream(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            BlockPos at = lane.origin;
            lane.put(at, Blocks.SPAWNER.defaultBlockState());
            SpawnerBlockEntity spawner = (SpawnerBlockEntity) lane.level.getBlockEntity(at);
            var builder = new DungeonRooms.Builder();
            builder.spawns.add(new DungeonRooms.Spawn(at.getX(), at.getY(), at.getZ(),
                    DungeonRooms.SpawnKind.ELDRITCH_GUARDIAN, 0, 1, true,
                    new DungeonRooms.Home(at.getX(), at.getY(), at.getZ(), 16)));
            builder.spawns.add(new DungeonRooms.Spawn(at.getX(), at.getY(), at.getZ(),
                    DungeonRooms.SpawnKind.MIND_SPIDER, 0, 1, false, null));
            List<Entity> first = new ArrayList<>(), replay = new ArrayList<>();
            RandomSource input = RandomSource.create(20261101), repeated = RandomSource.create(20261101);
            // The region captures publication like a ProtoChunk. Entity construction/configuration is native;
            // promotion into the live server is a later phase and may legitimately use its world RNG.
            try (var ignored = new ServerRandomGuard(lane.level)) {
                ThaumcraftOuterLands.spawnRoomEntities(region(lane.level, spawner, first), builder, input);
                ThaumcraftOuterLands.spawnRoomEntities(region(lane.level, spawner, replay), builder, repeated);
            }
            test.assertTrue(first.size() == 1 && replay.size() == 1, "both room passes capture one real guardian");
            Mob guardian = (Mob) first.getFirst(), again = (Mob) replay.getFirst();
            test.assertTrue(ChampionHooks.championId(guardian) >= 0
                            && ChampionHooks.championId(guardian) == ChampionHooks.championId(again)
                            && guardian.getYRot() == again.getYRot() && input.nextLong() == repeated.nextLong(),
                    "guardian yaw, champion identity and remaining RNG replay from the same room seed");
            test.assertTrue(guardian.getRestrictCenter().equals(at) && guardian.getRestrictRadius() == 16,
                    "RNG custody preserves native key-room guardian home configuration");
            first.forEach(Entity::discard);
            replay.forEach(Entity::discard);
        } finally { lane.clean(); }
        test.succeed();
    }

    private static WorldGenLevel region(ServerLevel level, SpawnerBlockEntity spawner, List<Entity> captured) {
        return (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getLevel" -> level;
                    case "getBlockEntity" -> spawner;
                    case "addFreshEntity" -> captured.add((Entity) args[0]);
                    default -> throw new AssertionError("Unexpected generation access: " + method.getName());
                });
    }

    /** Reject the original defect deterministically instead of hoping two threads race in a test. */
    private static final class ServerRandomGuard implements AutoCloseable {
        private final Level level;
        private final Field field;
        private final Object previous;
        ServerRandomGuard(Level level) {
            this.level = level;
            try {
                field = Level.class.getDeclaredField("random");
                field.setAccessible(true);
                previous = field.get(level);
                field.set(level, Proxy.newProxyInstance(RandomSource.class.getClassLoader(), new Class<?>[]{RandomSource.class},
                        (proxy, method, args) -> { throw new AssertionError("Room generation touched the live server RNG: " + method.getName()); }));
            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        }
        @Override public void close() {
            try { field.set(level, previous); }
            catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        }
    }
}
