/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.fabric.hooks;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Collects the item entities that one entity spawns while its death loot is worked out, instead of letting them into the
 * level, so {@code TinkererEvents.LIVING_DROPS} can change them first (NeoForge does the same with {@code Entity.captureDrops}).
 *
 * <p>Plain helper of the mixins {@code LivingEntityDropsMixin}, {@code EntityDropCaptureMixin} and
 * {@code ServerPlayerDropCaptureMixin}; it lives outside the mixin package because mixin classes cannot be referenced
 * directly. The capture is per thread and per entity: only what {@link #owner()} itself spawns is taken, so an unrelated spawn
 * during the death (another thread's, or another entity's) is untouched. Captures nest: a death inside a death restores
 * the outer capture when it ends.</p>
 */
public final class DropCapture {
    private static final ThreadLocal<DropCapture> CURRENT = new ThreadLocal<>();

    private final Entity owner;
    private final List<ItemEntity> drops = new ArrayList<>();
    @Nullable
    private final DropCapture outer;
    private boolean ended;

    private DropCapture(Entity owner, @Nullable DropCapture outer) {
        this.owner = owner;
        this.outer = outer;
    }

    /** Starts collecting what {@code owner} spawns; end it with {@link #end()} in a {@code finally}. */
    public static DropCapture begin(Entity owner) {
        DropCapture capture = new DropCapture(owner, CURRENT.get());
        CURRENT.set(capture);
        return capture;
    }

    /** The innermost capture of this thread, or null. */
    @Nullable
    public static DropCapture current() {
        return CURRENT.get();
    }

    /** If {@code spawner} is the entity being captured, takes the drop and returns true; otherwise false and nothing is taken. */
    public static boolean offer(Entity spawner, ItemEntity drop) {
        DropCapture capture = CURRENT.get();
        if (capture == null || capture.owner != spawner) return false;
        capture.drops.add(drop);
        return true;
    }

    public Entity owner() {
        return owner;
    }

    /** What was collected so far, in the order it was spawned (live list: the event's listeners edit it). */
    public List<ItemEntity> drops() {
        return drops;
    }

    /** Stops collecting and gives the outer capture (if any) back to the thread. */
    public void end() {
        if (ended) return;
        ended = true;
        if (outer == null) CURRENT.remove();
        else CURRENT.set(outer);
    }
}
