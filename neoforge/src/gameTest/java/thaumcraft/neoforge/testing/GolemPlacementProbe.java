package thaumcraft.neoforge.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import thaumcraft.common.entities.golems.GolemHarvestPlatform;
import thaumcraft.common.testing.GolemFireOrderRuntimeChecks;

/** TEST MODULE ONLY. A real separate NeoForge RightClickBlock listener, never a replacement of the golem platform.
 * The NeoForge counterpart of the Fabric probe: NeoForgeGolemHarvestPlatform posts the native cancellable event
 * for a replanting placement. The permanent listener is inert (and retains nothing) while no probe is open; only
 * the native golem actor is observed. */
public final class GolemPlacementProbe implements GolemFireOrderRuntimeChecks.PlacementProbe {
    private static GolemPlacementProbe active;
    private static boolean installed;
    private final List<BlockPos> consulted = new ArrayList<>();
    private Predicate<BlockPos> denied = pos -> false;

    public static GolemPlacementProbe open() {
        if (active != null) throw new IllegalStateException("Overlapping placement probes");
        if (!installed) {
            NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
                GolemPlacementProbe current = active;
                if (current == null || !GolemHarvestPlatform.MACHINE_NAME.equals(event.getEntity().getGameProfile().getName())) return;
                BlockPos clicked = event.getHitVec().getBlockPos().immutable();
                current.consulted.add(clicked);
                if (current.denied.test(clicked)) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.FAIL);
                }
            });
            installed = true;
        }
        GolemPlacementProbe probe = new GolemPlacementProbe();
        active = probe;
        return probe;
    }

    @Override public void deny(Predicate<BlockPos> region) { denied = region; }
    @Override public List<BlockPos> consulted() { return List.copyOf(consulted); }
    @Override public void close() { if (active == this) active = null; }
}
