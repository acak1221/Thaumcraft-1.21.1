package thaumcraft.fabric.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import thaumcraft.common.entities.golems.GolemHarvestPlatform;
import thaumcraft.common.testing.GolemFireOrderRuntimeChecks;

/** TEST MODULE ONLY. A real separate Fabric UseBlockCallback listener, never a replacement of the golem platform.
 * The permanent callback is inert (and retains nothing) while no probe is open; only the native golem actor is observed. */
public final class GolemPlacementProbe implements GolemFireOrderRuntimeChecks.PlacementProbe {
    private static GolemPlacementProbe active;
    private static boolean installed;
    private final List<BlockPos> consulted = new ArrayList<>();
    private Predicate<BlockPos> denied = pos -> false;

    public static GolemPlacementProbe open() {
        if (active != null) throw new IllegalStateException("Overlapping placement probes");
        if (!installed) {
            UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
                GolemPlacementProbe current = active;
                if (current == null || !GolemHarvestPlatform.MACHINE_NAME.equals(player.getGameProfile().getName())) return InteractionResult.PASS;
                BlockPos clicked = hit.getBlockPos().immutable();
                current.consulted.add(clicked);
                return current.denied.test(clicked) ? InteractionResult.FAIL : InteractionResult.PASS;
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
