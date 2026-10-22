package thaumcraft.fabric;

import dev.architectury.event.EventResult;
import thaumcraft.common.lib.events.BossPlacementRules;

/**
 * The Fabric half of the original placement cancellation next to an active eldritch boss.
 *
 * <p>Oracle {@code thaumcraft/common/lib/events/EventHandlerWorld.java:225-245} cancelled the
 * placement of a block that stood next to an active boss of the Outer Lands. The cancellable
 * placement event of the common artifact of Architectury is the counterpart of the original
 * {@code BlockEvent.PlaceEvent} on Fabric; the NeoForge half uses the native NeoForge event, so
 * neither loader depends on the signature of the other one.</p>
 */
final class FabricBossPlacement {
    private FabricBossPlacement() {}

    static void register() {
        dev.architectury.event.events.common.BlockEvent.PLACE.register((level, pos, state, entity) -> {
            if (!BossPlacementRules.nearActiveBoss(level, entity, pos)) return EventResult.pass();
            return EventResult.interruptFalse();
        });
    }
}
