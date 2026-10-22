package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The loader's own block-break phases, as the shared harness observes them.
 *
 * <p>The three excavation scenarios of the shared harness need the three phases of a block
 * break: BEFORE (which may cancel the break), CANCELED (the break was denied and no block was
 * removed) and AFTER (the block was really removed). The two loaders own different, native
 * graphs for them:</p>
 *
 * <ul>
 *   <li><b>Fabric</b> owns the three events of
 *       {@code net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents}. Its BEFORE runs
 *       directly before the {@code Block.playerWillDestroy} call of
 *       {@code ServerPlayerGameMode.destroyBlock} and its AFTER directly before the
 *       {@code Block.destroy} call of the same method, so AFTER only runs after a real removal.</li>
 *   <li><b>NeoForge</b> owns the cancellable
 *       {@code net.neoforged.neoforge.event.level.BlockEvent.BreakEvent}, which
 *       {@code CommonHooks.fireBlockBreak} posts at the head of
 *       {@code ServerPlayerGameMode.destroyBlock}. The event is already cancelled when the
 *       adventure restriction, the game master block or the tool tier forbids the break, which
 *       is exactly the set of conditions that keeps the Fabric BEFORE from running.</li>
 * </ul>
 *
 * <p>The shared harness therefore never reflects into a loader class. Every loader installs its
 * own {@link Factory} during its game-test bootstrap and the shared scenarios ask that factory
 * for the observer of one phase. The shared side stays loader neutral and the Fabric behaviour
 * is unchanged.</p>
 */
public interface NativeBreakPhases extends AutoCloseable {
    /** The three phases of a block break. */
    enum Phase { BEFORE, CANCELED, AFTER }

    /**
     * The phase callbacks of the shared harness.
     *
     * <p>Every callback receives the level, the player, the position and the state of the
     * broken block, so the shared scenarios can filter the break they observe themselves.</p>
     */
    interface Listener {
        /**
         * BEFORE: the break is about to happen.
         *
         * @return false to cancel the break, exactly like the Fabric BEFORE event
         */
        default boolean before(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
            return true;
        }

        /** CANCELED: the break was denied, so no block was removed. */
        default void canceled(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {}

        /** AFTER: the block was really removed. */
        default void after(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {}
    }

    /** Installs the observer of one phase of the breaks of one player. */
    interface Factory {
        /**
         * Installs the observer of one phase.
         *
         * @param level the level of the observed break
         * @param player the player that breaks the block
         * @param phase the observed phase
         * @param listener the phase callbacks of the shared harness
         * @return the installed observer, which removes itself again on {@link #close()}
         */
        NativeBreakPhases install(ServerLevel level, ServerPlayer player, Phase phase, Listener listener);
    }

    /** Removes the observer again and restores the exact previous state of the loader. */
    @Override void close();

    /** The holder the loader bootstrap fills. */
    final class Holder {
        private static volatile Factory factory;

        private Holder() {}

        /** Installs the factory of this loader. */
        public static void install(Factory installed) {
            factory = installed;
        }

        /** The factory of this loader. */
        public static Factory factory() {
            var installed = factory;
            if (installed == null) {
                throw new IllegalStateException("This loader did not install its native block break observer");
            }
            return installed;
        }
    }
}
