package thaumcraft.neoforge;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;

/**
 * The NeoForge delivery of the shared Architectury break event.
 *
 * <p>The shared {@code dev.architectury.event.events.common.BlockEvent.BREAK} is the
 * notification every native protection of the port uses: the excavation boundary, the bore and
 * the golem harvest all listen to it. On Fabric it is delivered for every break attempt. On
 * NeoForge it is delivered from a listener of the native
 * {@link net.neoforged.neoforge.event.level.BlockEvent.BreakEvent}, and the NeoForge event bus
 * does not call a listener with the default {@code receiveCanceled = false} for an event that
 * is already cancelled.</p>
 *
 * <p>NeoForge posts the {@code BreakEvent} <em>already cancelled</em> in exactly three cases -
 * the NeoForge javadoc of the event names them: {@code Player#blockActionRestricted} (the
 * adventure restriction), a {@code GameMasterBlock} without the permission and an
 * {@code Item#canAttackBlock} that forbids the block. The shared break event was therefore never
 * delivered for those three refusals, while the Fabric loader delivers it. This listener
 * restores the parity: it delivers the shared break event for the pre-cancelled break events
 * as well, exactly once.</p>
 *
 * <p>The delivery is guarded by a counter of the shared deliveries of the current break, so the
 * shared event is never delivered twice: the Architectury listener of the loader delivers it at
 * {@code EventPriority.HIGH} whenever the native event is not cancelled there, and this listener
 * only delivers it when the counter of the current break is still zero.</p>
 */
public final class NeoForgeSharedBreakEvent {
    /** The number of shared break deliveries of the break that is running on this thread. */
    private static final ThreadLocal<int[]> DELIVERIES = ThreadLocal.withInitial(() -> new int[1]);
    private static boolean installed;

    private NeoForgeSharedBreakEvent() {}

    /** Installs the delivery. It never delivers the shared event a second time. */
    public static void init() {
        if (installed) return;
        installed = true;
        // The observer counts every delivery of the shared break event, whoever made it.
        BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            DELIVERIES.get()[0]++;
            return EventResult.pass();
        });
        NeoForge.EVENT_BUS.register(NeoForgeSharedBreakEvent.class);
    }

    /**
     * The pre-cancelled break: the shared listener of the loader is skipped for it, so the
     * shared break event is delivered here.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onBreakHighest(BreakEvent event) {
        DELIVERIES.get()[0] = 0;
        if (!event.isCanceled()) return;
        deliver(event);
    }

    /**
     * A break that was cancelled between the highest and the lowest priority: the shared
     * listener of the loader was skipped for it as well.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onBreakLowest(BreakEvent event) {
        if (DELIVERIES.get()[0] > 0) return;
        if (!event.isCanceled()) return;
        deliver(event);
    }

    /** The delivery of the shared break event, exactly like the loader does it. */
    private static void deliver(BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof Level level)) return;
        var result = BlockEvent.BREAK.invoker().breakBlock(level, event.getPos(), event.getState(), player, null);
        if (result.isFalse()) event.setCanceled(true);
    }
}
