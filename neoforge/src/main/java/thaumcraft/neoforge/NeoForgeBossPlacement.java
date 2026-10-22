package thaumcraft.neoforge;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import thaumcraft.common.lib.events.BossPlacementRules;

/**
 * The NeoForge half of the original placement cancellation next to an active eldritch boss.
 *
 * <p>Oracle {@code thaumcraft/common/lib/events/EventHandlerWorld.java:225-245} cancelled the
 * {@code BlockEvent.PlaceEvent} and the {@code BlockEvent.MultiPlaceEvent} of a placement next to
 * an active boss of the Outer Lands. NeoForge owns the cancellable
 * {@link BlockEvent.EntityPlaceEvent} and its {@link BlockEvent.EntityMultiPlaceEvent} subclass,
 * so the NeoForge half listens to them directly and never depends on the signature of the
 * common artifact of Architectury.</p>
 */
final class NeoForgeBossPlacement {
    private NeoForgeBossPlacement() {}

    static void register(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener(NeoForgeBossPlacement::onPlace);
        NeoForge.EVENT_BUS.addListener(NeoForgeBossPlacement::onMultiPlace);
    }

    /** The NeoForge counterpart of the original {@code BlockEvent.PlaceEvent}. */
    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        cancel(event);
    }

    /** The NeoForge counterpart of the original {@code BlockEvent.MultiPlaceEvent}. */
    private static void onMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        cancel(event);
    }

    private static void cancel(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level)) return;
        if (!BossPlacementRules.nearActiveBoss(level, event.getEntity(), event.getPos())) return;
        event.setCanceled(true);
    }
}
