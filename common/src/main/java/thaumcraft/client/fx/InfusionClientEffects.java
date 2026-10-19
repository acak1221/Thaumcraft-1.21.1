package thaumcraft.client.fx;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.client.fx.particles.InfusionArcParticle;
import thaumcraft.client.fx.particles.InfusionParticle;
import thaumcraft.common.lib.network.InfusionFxPayload;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TilePedestal;

/**
 * S2C visual adapter for committed infusion actions. Only local animation/FX
 * counters live here: never change a tile, inventory, recipe, demand or entity.
 * TC4 M703-780 / PacketFXInfusionSource:69-90 / ClientTickEventsFML:249-269.
 */
public final class InfusionClientEffects {
    private static final int MAX_MATRICES = 64, MAX_SOURCES = 128, MAX_SOURCES_PER_MATRIX = 24;
    private static final int MAX_PARTICLES = 1024, MAX_EMISSIONS_PER_TICK = 128, WATCH_TTL = 200;
    private static final SoundEvent CRAFT_START = sound("craftstart"), CRAFT_FAIL = sound("craftfail"),
            INFUSER_START = sound("infuserstart"), INFUSER = sound("infuser");
    private static final Map<TileInfusionMatrix, VisualState> MATRICES = new WeakHashMap<>();
    private static final List<TrackedParticle> PARTICLES = new ArrayList<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private static WeakReference<LocalPlayer> lastPlayer = new WeakReference<>(null);
    private static int lastPlayerTick = Integer.MIN_VALUE, emissions;
    private static long clock;

    private InfusionClientEffects() {}

    public record Animation(float startup, float craftCount) {}
    private record SourceKey(BlockPos position, int event, int color) {}
    private record TrackedParticle(WeakReference<Particle> reference, InfusionParticle.Scope scope) {}

    private static final class Source {
        private int ticks;
        private Source(int ticks) { this.ticks = ticks; }
    }

    private static final class VisualState {
        private final Map<SourceKey, Source> sources = new LinkedHashMap<>();
        private InfusionParticle.Scope scope;
        private long touched;
        private float startup, previousStartup;
        private int craftCount, previousCraftCount;
        private boolean wasCrafting, terminal;

        private VisualState(TileInfusionMatrix matrix) { scope = new InfusionParticle.Scope(matrix); }

        private void stopStreams(TileInfusionMatrix matrix) {
            cancel(scope);
            scope = new InfusionParticle.Scope(matrix);
            sources.clear();
        }

        private void close() { cancel(scope); sources.clear(); }
    }

    private static SoundEvent sound(String name) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", name));
    }

    /** Read-only render snapshot. Timers advance in CLIENT_POST, never per frame. */
    public static Animation animation(TileInfusionMatrix matrix, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        useWorld(client.level);
        if (client.level == null || matrix.getLevel() != client.level || !current(client.level, matrix)) return new Animation(0, 0);
        VisualState state = watch(matrix);
        float partial = Mth.clamp(partialTick, 0, 1);
        return new Animation(Mth.lerp(partial, state.previousStartup, state.startup),
                Mth.lerp(partial, (float)state.previousCraftCount, (float)state.craftCount));
    }

    /** Called on the native client thread by InfusionFxNetworking.initClient. */
    public static void accept(InfusionFxPayload payload) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        useWorld(level);
        if (level == null || !level.dimension().location().equals(payload.dimension())
                || !InfusionParticle.loaded(level, payload.matrix()) || !InfusionParticle.loaded(level, payload.target())
                || payload.matrix().distSqr(payload.target()) > 32 * 32
                || !(level.getBlockEntity(payload.matrix()) instanceof TileInfusionMatrix matrix) || !current(level, matrix)) return;
        pruneParticles();
        VisualState state = watch(matrix);
        switch (payload.event()) {
            case 0 -> {
                state.stopStreams(matrix);
                state.terminal = false;
                state.previousCraftCount = state.craftCount = 0;
                // Original M305/328 server craftstart is relocated to this
                // committed S2C cue. Do NOT also broadcast it on the server.
                play(level, matrix.getBlockPos(), CRAFT_START);
                // infuserstart belongs to the actual crafting transition below,
                // including clients that join an already-running saved session.
            }
            case 1, 2 -> {
                var target = level.getBlockEntity(payload.target());
                if (payload.event() == 1 && !(target instanceof IAspectSource)
                        || payload.event() == 2 && !(target instanceof TilePedestal)) return;
                int ttl = target instanceof TilePedestal ? 60 : 15;
                int color = payload.event() == 1 ? payload.data() & 0xffffff : 0;
                SourceKey key = new SourceKey(payload.target().immutable(), payload.event(), color);
                Source existing = state.sources.get(key);
                if (existing != null) existing.ticks = ttl;
                else if (state.sources.size() < MAX_SOURCES_PER_MATRIX && sourceCount() < MAX_SOURCES) state.sources.put(key, new Source(ttl));
            }
            case 3 -> {
                var entity = level.getEntity(payload.data());
                if (entity == null || entity.isRemoved() || entity.level() != level || !InfusionParticle.loaded(level, entity.blockPosition())) return;
                Vec3 destination = entity.position().add(0, entity.getBbHeight() / 2, 0);
                if (destination.distanceToSqr(Vec3.atCenterOf(matrix.getBlockPos())) <= 32 * 32) {
                    add(state.scope, () -> new InfusionArcParticle(level, state.scope, Vec3.atCenterOf(matrix.getBlockPos()), destination));
                }
            }
            case 4 -> {
                // The server has ALREADY dropped/destroyed the real item and
                // applied goo/gas/blast. Only the original zap and purple burst
                // are ours. In particular, do not spawn a second ItemEntity.
                state.sources.entrySet().removeIf(entry -> entry.getKey().position.equals(payload.target()));
                Vec3 destination = Vec3.atCenterOf(payload.target()).add(0, 1, 0);
                add(state.scope, () -> new InfusionArcParticle(level, state.scope, Vec3.atCenterOf(matrix.getBlockPos()), destination));
                burst(level, state, payload.target().above(), 0xc000c0, count(5) * count(2));
            }
            case 5, 6 -> {
                state.stopStreams(matrix);
                state.terminal = true;
                state.wasCrafting = false;
                if (payload.event() == 5 && InfusionParticle.loaded(level, matrix.getCenterPos().above())) {
                    burst(level, state, matrix.getCenterPos().above(), -9999, count(10) * count(2));
                }
                if (payload.event() == 6) {
                    BlockPos pos = matrix.getBlockPos();
                    // Original M417: a genuine invalid-center abort, not an
                    // invented completion sound or a second gameplay failure.
                    level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), CRAFT_FAIL, SoundSource.BLOCKS, 1, .6F, false);
                }
                // Original TilePedestal:203-209 is a multicolour burst, NOT a
                // made-up craftdone sound (no such retained TC4 sound event).
            }
            case 7 -> {
                // Watching the actual active BE starts its native-tick startup
                // ramp, even before its first rendered frame. Formation already
                // broadcasts the original wand sound: never double it here.
                state.previousStartup = state.startup;
            }
            default -> { /* The bounded payload codec rejects other events. */ }
        }
    }

    public static void tick(Minecraft client) {
        useWorld(client.level);
        pruneParticles();
        if (client.level == null || client.player == null || client.isPaused()) return;
        if (lastPlayer.get() == client.player && lastPlayerTick == client.player.tickCount) return;
        lastPlayer = new WeakReference<>(client.player);
        lastPlayerTick = client.player.tickCount;
        emissions = 0;
        ++clock;
        ClientLevel level = client.level;
        var iterator = MATRICES.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            TileInfusionMatrix matrix = entry.getKey();
            VisualState state = entry.getValue();
            if (!current(level, matrix) || clock - state.touched > WATCH_TTL) {
                state.close();
                iterator.remove();
                continue;
            }
            state.previousStartup = state.startup;
            state.previousCraftCount = state.craftCount;
            // Original M721-735. These are transient CLIENT values only; never
            // derive paid progression by draining/synthesising an aspect demand.
            if (matrix.isActive() && state.startup != 1) {
                state.startup += Math.max(state.startup / 10, .001F);
                if (state.startup > .999F) state.startup = 1;
            } else if (!matrix.isActive() && state.startup > 0) {
                state.startup -= state.startup / 10;
                if (state.startup < .001F) state.startup = 0;
            }
            boolean crafting = matrix.isCrafting() && !state.terminal;
            if (crafting) {
                if (!state.wasCrafting) play(level, matrix.getBlockPos(), INFUSER_START);
                else if (state.craftCount > 0 && state.craftCount % 65 == 0) play(level, matrix.getBlockPos(), INFUSER);
                if (state.craftCount < Integer.MAX_VALUE) ++state.craftCount;
                if (InfusionParticle.loaded(level, matrix.getCenterPos()) && visible(client, matrix.getCenterPos())) {
                    add(state.scope, () -> InfusionParticle.rune(level, state.scope, matrix.getCenterPos()));
                }
                if (matrix.getInstability() > 0 && level.random.nextInt(200) <= matrix.getInstability()) {
                    Vec3 from = Vec3.atCenterOf(matrix.getBlockPos());
                    Vec3 to = from.add(spread(level, 2), spread(level, 2), spread(level, 2));
                    if (InfusionParticle.loaded(level, BlockPos.containing(to))) {
                        add(state.scope, () -> new InfusionArcParticle(level, state.scope, from, to));
                    }
                }
            } else {
                state.craftCount = Math.max(0, Math.min(50, state.craftCount - 2));
                if (state.wasCrafting && !state.terminal) state.stopStreams(matrix);
            }
            if (!matrix.isCrafting()) state.terminal = false;
            state.wasCrafting = crafting;
            tickSources(client, level, matrix, state);
        }
    }

    private static void tickSources(Minecraft client, ClientLevel level, TileInfusionMatrix matrix, VisualState state) {
        var iterator = state.sources.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            SourceKey key = entry.getKey();
            Source source = entry.getValue();
            if (source.ticks <= 0 || state.terminal || !InfusionParticle.loaded(level, key.position)) {
                iterator.remove();
                continue;
            }
            if (visible(client, key.position)) {
                if (key.event == 1) {
                    float scale = source.ticks > 5 ? 1 : source.ticks * source.ticks / 25F;
                    int phase = (int)(clock & 0x7fffffff) - Math.max(0, 5 - source.ticks);
                    add(state.scope, () -> InfusionParticle.essentia(level, state.scope, key.position, matrix.getBlockPos(), key.color, phase, scale));
                } else if (level.getBlockEntity(key.position) instanceof TilePedestal pedestal && !pedestal.getItem(0).isEmpty()) {
                    var actual = pedestal.getItem(0);
                    if (level.random.nextInt(3) == 0) {
                        Vec3 from = new Vec3(key.position.getX() + level.random.nextFloat(), key.position.getY() + 1 + level.random.nextFloat(),
                                key.position.getZ() + level.random.nextFloat());
                        add(state.scope, () -> InfusionParticle.sparkle(level, state.scope, key.position, from, matrix.getBlockPos()));
                    } else {
                        boolean block = actual.getItem() instanceof BlockItem;
                        for (int i = 0; i < count(2); i++) {
                            Vec3 from = new Vec3(key.position.getX() + (block ? level.random.nextFloat() : .4 + level.random.nextFloat() * .2),
                                    key.position.getY() + (block ? 1 + level.random.nextFloat() : 1.23 + level.random.nextFloat() * .2),
                                    key.position.getZ() + (block ? level.random.nextFloat() : .4 + level.random.nextFloat() * .2));
                            add(state.scope, () -> InfusionParticle.fragment(level, state.scope, key.position, from, matrix.getBlockPos(), actual));
                        }
                    }
                } else {
                    iterator.remove();
                    continue;
                }
            }
            --source.ticks;
        }
    }

    private static VisualState watch(TileInfusionMatrix matrix) {
        VisualState state = MATRICES.get(matrix);
        if (state == null) {
            if (MATRICES.size() >= MAX_MATRICES) {
                TileInfusionMatrix oldest = null;
                long touched = Long.MAX_VALUE;
                for (var entry : MATRICES.entrySet()) {
                    if (entry.getValue().touched < touched) { oldest = entry.getKey(); touched = entry.getValue().touched; }
                }
                if (oldest != null) MATRICES.remove(oldest).close();
            }
            state = new VisualState(matrix);
            MATRICES.put(matrix, state);
        }
        state.touched = clock;
        return state;
    }

    private static boolean current(ClientLevel level, TileInfusionMatrix matrix) {
        return matrix != null && !matrix.isRemoved() && matrix.getLevel() == level
                && InfusionParticle.loaded(level, matrix.getBlockPos()) && level.getBlockEntity(matrix.getBlockPos()) == matrix;
    }

    private static void useWorld(ClientLevel level) {
        if (world.get() != level) { clear(); world = new WeakReference<>(level); }
    }

    /** Disconnect and level-identity changes kill even already-emitted native particles. */
    public static void clear() {
        MATRICES.values().forEach(VisualState::close);
        MATRICES.clear();
        for (var tracked : PARTICLES) { var particle = tracked.reference.get(); if (particle != null) particle.remove(); }
        PARTICLES.clear();
        world = new WeakReference<>(null);
        lastPlayer = new WeakReference<>(null);
        lastPlayerTick = Integer.MIN_VALUE;
        clock = 0;
        emissions = 0;
    }

    private static void pruneParticles() {
        PARTICLES.removeIf(tracked -> {
            Particle particle = tracked.reference.get();
            return particle == null || !particle.isAlive();
        });
    }

    private static void cancel(InfusionParticle.Scope scope) {
        scope.cancel();
        // Reclaim slots immediately, so a long source stream cannot suppress
        // its own completion burst until the next native particle tick.
        PARTICLES.removeIf(tracked -> {
            if (tracked.scope != scope) return false;
            Particle particle = tracked.reference.get();
            if (particle != null) particle.remove();
            return true;
        });
    }

    private static void add(InfusionParticle.Scope scope, Supplier<? extends Particle> factory) {
        if (emissions >= MAX_EMISSIONS_PER_TICK || PARTICLES.size() >= MAX_PARTICLES) return;
        Particle particle = factory.get();
        PARTICLES.add(new TrackedParticle(new WeakReference<>(particle), scope));
        ++emissions;
        Minecraft.getInstance().particleEngine.add(particle);
    }

    private static int sourceCount() {
        int count = 0;
        for (VisualState state : MATRICES.values()) count += state.sources.size();
        return count;
    }

    private static int count(int base) {
        ParticleStatus setting = Minecraft.getInstance().options.particles().get();
        return setting == ParticleStatus.MINIMAL ? 0 : setting == ParticleStatus.DECREASED ? base : base * 2;
    }

    private static boolean visible(Minecraft client, BlockPos pos) {
        var camera = client.getCameraEntity();
        int range = Minecraft.useFancyGraphics() ? 64 : 32;
        return camera != null && camera.distanceToSqr(Vec3.atCenterOf(pos)) <= range * range;
    }

    private static double spread(ClientLevel level, double scale) { return (level.random.nextFloat() - level.random.nextFloat()) * scale; }

    private static void play(ClientLevel level, BlockPos pos, SoundEvent sound) {
        level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), sound, SoundSource.BLOCKS, .5F, 1, false);
    }

    private static void burst(ClientLevel level, VisualState state, BlockPos pos, int color, int amount) {
        if (!InfusionParticle.loaded(level, pos) || !visible(Minecraft.getInstance(), pos)) return;
        for (int i = 0; i < amount; i++) {
            int rgb = color;
            if (rgb == -9999) rgb = (int)((.33F + level.random.nextFloat() * .67F) * 255) << 16
                    | (int)((.33F + level.random.nextFloat() * .67F) * 255) << 8 | (int)((.33F + level.random.nextFloat() * .67F) * 255);
            int tint = rgb;
            add(state.scope, () -> InfusionParticle.burst(level, state.scope, pos, tint));
        }
    }
}
