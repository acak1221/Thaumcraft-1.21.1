package thaumcraft.common.lib.bore;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/** Loader event forwarding only, NOT a completion/loot/payment permit.
 * BoreWork binds/revalidates its exact native attempt around every callback.
 * Architectury BREAK is independent and dispatched by that closed scope. */
public final class BorePlatform {
    public static final UUID MACHINE_UUID = UUID.fromString("41c82c87-7afb-4024-ba57-13d2c99cae77");
    public static final String MACHINE_NAME = "FakeThaumcraftBore";
    private static Bridge bridge;

    private BorePlatform() {}
    public interface Bridge {
        ServerPlayer actor(ServerLevel level);
        /**
         * The shared break protection notification of the machine actor.
         *
         * <p>The shared {@code BlockEvent.BREAK} of Architectury is the notification every loader
         * owes to the other mods. It must be delivered EXACTLY once for one break: the Fabric
         * loader has no native counterpart of it, so the loader delivers it here; the NeoForge
         * loader's native cancellable break event already delivers it, so its implementation
         * answers without a second notification. Delivering it twice would let a protection mod
         * of the shared event veto a break it already saw, and it would let a listener that
         * counts the notifications observe two breaks for one removal.</p>
         *
         * @return whether the break may continue.
         */
        boolean sharedBreak(ServerLevel level, ServerPlayer actor, BlockPos target, BlockState capturedOldState);
        boolean before(ServerLevel level, ServerPlayer actor, BlockPos target, BlockState capturedOldState);
        void canceled(ServerLevel level, ServerPlayer actor, BlockPos target, BlockState capturedOldState);
        void after(ServerLevel level, ServerPlayer actor, BlockPos target, BlockState capturedOldState);
        boolean isNativeMachineActor(ServerPlayer actor);
    }

    /** Startup-only immutable loader binding. Do not retain native worlds/actors. */
    public static synchronized void install(Bridge nativeBridge) {
        Objects.requireNonNull(nativeBridge, "nativeBridge");
        if (bridge != null && bridge != nativeBridge) throw new IllegalStateException("Bore platform is already installed");
        bridge = nativeBridge;
    }
    public static synchronized Bridge required() {
        if (bridge == null) throw new IllegalStateException("No native Bore platform bridge is installed");
        return bridge;
    }
}
