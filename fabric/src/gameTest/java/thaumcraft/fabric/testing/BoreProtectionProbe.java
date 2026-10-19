package thaumcraft.fabric.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import thaumcraft.common.testing.BoreProtectionRuntimeChecks.Attempt;
import thaumcraft.common.testing.BoreProtectionRuntimeChecks.Fault;
import thaumcraft.common.testing.BoreProtectionRuntimeChecks.Probe;

/** TEST MODULE ONLY. Real separate loader listeners, never a replacement
 * BorePlatform. Permanent Fabric callbacks retain NO world/player/fixture when
 * ACTIVE is null. Root runs one isolated declared scenario at a time. */
public final class BoreProtectionProbe implements Probe {
    private static Attempt active;
    private static boolean fabricInstalled;
    private Attempt owner;
    private boolean architecturyInstalled;
    private final BlockEvent.Break architectury = (world, pos, old, player, xp) -> {
        Attempt current = active;
        if (current == null || !current.applies(world, pos)) return EventResult.pass();
        return current.architectury(player, old) ? EventResult.pass() : EventResult.interruptFalse();
    };

    @Override public void bind(Attempt attempt) {
        if (attempt == null || active != null || owner != null) throw new IllegalStateException("Overlapping isolated Bore protection fixtures");
        installFabricOnce(); owner = attempt; active = attempt;
        architecturyInstalled = true; BlockEvent.BREAK.register(architectury);
    }
    private static void installFabricOnce() {
        if (fabricInstalled) return;
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, old, be) -> {
            Attempt current = active;
            return current == null || !current.applies(world, pos) || current.before(player, old, be);
        });
        PlayerBlockBreakEvents.CANCELED.register((world, player, pos, old, be) -> {
            Attempt current = active;
            if (current != null && current.applies(world, pos)) current.canceled(player, old, be);
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, old, be) -> {
            Attempt current = active;
            if (current != null && current.applies(world, pos)) current.after(player, old, be);
        });
        fabricInstalled = true;
    }
    @Override public void close() {
        // Clear strong references FIRST, even if event removal itself throws.
        if (active == owner) active = null;
        owner = null;
        if (architecturyInstalled) { BlockEvent.BREAK.unregister(architectury); architecturyInstalled = false; }
    }
    public static Fault nativeEntry(Level world, BlockPos pos, boolean moving) {
        Attempt current = active;
        return current == null ? Fault.NONE : current.nativeEntry(world, pos, moving);
    }
    public static void nativeReturn(Level world, BlockPos pos, boolean removed) {
        Attempt current = active;
        if (current != null) current.nativeReturn(world, pos, removed);
    }
}
