package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;

/** TEST ONLY. Reproduces LevelChunk's normal-null-after-onRemove path through a
 * real held Excavation channel, not a forged world/delegate/result or manual loot.
 * No runtime Block construction, RNG control or production instrumentation. */
public final class ExcavationRemovalFailureRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static RemovalProbe probe;
    private static Context active;

    private ExcavationRemovalFailureRuntimeChecks() {}

    /** Isolated gameTest main bootstrap ONLY, before the native registry freezes. */
    public static synchronized void registerEarlyProbe() {
        if (probe == null) probe = Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "excavation_removal_failure_probe"), new RemovalProbe());
    }

    /** Synchronous owned fixture; root's wrapper timeout240 ticks is sufficient. */
    public static void run(GameTestHelper test) {
        if (probe == null) throw new IllegalStateException("Register Excavation removal probe BEFORE block registry freeze");
        Context previous = active;
        if (previous != null) throw new IllegalStateException("Another removal probe context is already active");
        try (var actor = new FocusUpgradeRuntimeChecks.Actor(test)) {
            try {
                exercise(actor, Mode.REPLACE);
                exercise(actor, Mode.REPLACE_AND_SWAP);
                exercise(actor, Mode.REPLACE_AND_THROW);
                exercise(actor, Mode.PASS); // Actual recovery after exceptional removal.
            } finally {
                active = previous; // EXACT previous identity, before owned-world cleanup.
            }
        }
        test.assertTrue(active == previous, "test-only global probe context restored exactly");
        test.succeed();
    }

    private static ItemStack focus(FocusUpgradeRuntimeChecks.Actor actor) {
        return actor.focus(ThaumcraftFoci.EXCAVATION_FOCUS.get(), FocusUpgradeType.potency, 1,
                FocusUpgradeType.enlarge, 2, FocusUpgradeType.potency, 3,
                FocusUpgradeType.enlarge, 4, FocusUpgradeType.potency, 5);
    }

    private static void exercise(FocusUpgradeRuntimeChecks.Actor actor, Mode mode) {
        active = null; // Fixture removal must not invoke the adversarial callback.
        actor.clearLane(); actor.equip(focus(actor), "gold", 100); actor.aim();
        BlockPos fault = actor.root.north(), followup = actor.root.south();
        actor.put(actor.root, probe.defaultBlockState());
        actor.put(fault, probe.defaultBlockState());
        // Not initially eligible: only the one fault cell can be selected first.
        // Its onRemove exposes a fresh matching neighbour to prove permit revocation
        // without choosing/controlling the original six-direction world shuffle.
        actor.put(followup, Blocks.TNT.defaultBlockState());
        Context context = new Context(actor, mode, fault, followup);
        active = context;
        BlockEvent.Break listener = (world, pos, state, player, xp) -> {
            if (world == actor.level && player == actor.player) {
                if (pos.equals(actor.root)) context.primaryBreak++;
                else if (pos.equals(fault)) context.extraBreak++;
                else context.otherBreak++;
            }
            return EventResult.pass();
        };
        boolean propagated = false;
        BlockEvent.BREAK.register(listener);
        try (var after = new AfterProbe(context)) {
            try { actor.begin(); actor.tick(2); }
            catch (ReportedException reported) {
                // Native block/world layers may themselves report before doTick's
                // Ticking-player wrapper. Both report and cause paths must retain
                // the exact fixture Throwable, not merely a matching message/type.
                propagated = mode == Mode.REPLACE_AND_THROW
                        && containsExact(reported.getCause(), context.failure)
                        && containsExact(reported.getReport().getException(), context.failure);
                if (!propagated) throw reported;
            }
        } finally {
            BlockEvent.BREAK.unregister(listener);
        }

        actor.require(propagated == (mode == Mode.REPLACE_AND_THROW), "only the deliberate native onRemove throw propagates through its exact wrapper chain");
        actor.require(context.primaryBreak == 1 && context.extraBreak == 1 && context.otherBreak == 0
                && context.primaryOnRemove == 1 && context.extraOnRemove == 1,
                "real protected native channel enters root and exactly one extra delegate; no reused permit/further neighbour attempts");
        actor.require(context.sawRequestedAir && context.visAtExtraOnRemove == 70,
                "extra native onRemove runs AFTER requested AIR and bound15-centivis reservation, in addition to paid primary15");
        actor.require(actor.level.isEmptyBlock(actor.root) && context.primaryAfter == 1,
                "same registered probe at root has callback disabled and a genuine successful-removal witness");
        actor.require(ItemWandCasting.getVis(context.original, "terra") == 70,
                "normal FALSE and exceptional post-mutation removal retain exact original-wand primary+extra cost; never refund/reset");
        ItemStack expected = context.originalBefore.copy();
        expected.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(expected).withAmount("terra", 70));
        actor.require(ItemStack.matches(context.original, expected),
                "only the original operation-owned30terra changes; all other vis, materials, focus and item components remain exact");
        actor.require(ItemFocusExcavation.progress(actor.player) != null && ItemFocusExcavation.progress(actor.player).amount() == 0,
                "completed root resets channel progress even when its extra is indeterminate or throws");

        if (mode == Mode.PASS) {
            actor.require(actor.level.isEmptyBlock(fault) && actor.level.getBlockState(followup).is(Blocks.TNT)
                    && context.extraAfter == 1 && cobbleItems(actor) == 2,
                    "fresh native attempt after throw has clean scope and two genuine paid drops, with no stale probe/permit/frame");
        } else {
            actor.require(context.dirtWritten && actor.level.getBlockState(fault) == Blocks.DIRT.defaultBlockState()
                    && actor.level.getBlockState(followup) == probe.defaultBlockState(),
                    "partial onRemove mutation's exact DIRT survives and newly exposed matching neighbour is never reused");
            actor.require(context.extraAfter == 0,
                    "normal native false/throw has no accepted extra AFTER witness; actual callbacks=" + context.extraAfter);
            actor.require(cobbleItems(actor) == 1 && actor.items().size() == 1,
                    "only genuine primary loot survives indeterminate extra; actual cobble=" + cobbleItems(actor) + ", item entities=" + actor.items().size());
        }
        if (mode == Mode.REPLACE_AND_SWAP) {
            actor.require(actor.player.getMainHandItem() == context.replacement
                    && ItemWandCasting.getVis(context.replacement, "terra") == 777
                    && ItemStack.matches(context.replacement, context.replacementBefore),
                    "onRemove hand replacement is preserved exactly and never charged/refunded on behalf of the original reservation");
        } else {
            actor.require(actor.player.getMainHandItem() == context.original,
                    "unchanged actual hand cannot continue an indeterminate expansion even with remaining funds");
        }
    }

    private static int cobbleItems(FocusUpgradeRuntimeChecks.Actor actor) {
        return actor.items().stream().mapToInt(entity -> entity.getItem().is(Items.COBBLESTONE) ? entity.getItem().getCount() : 0).sum();
    }

    private static boolean containsExact(Throwable cause, Throwable expected) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int depth = 0; cause != null && depth < 16 && visited.add(cause); depth++) {
            if (cause == expected) return true;
            cause = cause instanceof ReportedException reported ? reported.getReport().getException() : cause.getCause();
        }
        return false;
    }

    private enum Mode { REPLACE, REPLACE_AND_SWAP, REPLACE_AND_THROW, PASS }

    private static final class Context {
        private final FocusUpgradeRuntimeChecks.Actor actor;
        private final ServerLevel level;
        private final Mode mode;
        private final BlockPos fault, followup;
        private final ItemStack original, originalBefore, replacement, replacementBefore;
        private final IllegalStateException failure = new IllegalStateException("owned extra onRemove fixture exception");
        private int primaryBreak, extraBreak, otherBreak, primaryOnRemove, extraOnRemove, primaryAfter, extraAfter, visAtExtraOnRemove;
        private boolean sawRequestedAir, dirtWritten;

        private Context(FocusUpgradeRuntimeChecks.Actor actor, Mode mode, BlockPos fault, BlockPos followup) {
            this.actor = actor; level = actor.level; this.mode = mode; this.fault = fault; this.followup = followup;
            original = actor.wand; originalBefore = original.copy();
            replacement = FocusUpgradeRuntimeChecks.Actor.charged(focus(actor), "gold", 777);
            replacementBefore = replacement.copy();
        }
    }

    private static final class RemovalProbe extends Block {
        private RemovalProbe() {
            // Real vanilla unconditional item loot. TNT's table requires the
            // actual minecraft:tnt block and unstable=false, so a custom probe
            // cannot use it as a positive drop control. Cobble has no such gate.
            // A fabricated extra success would still visibly duplicate the output.
            super(BlockBehaviour.Properties.of().strength(0F).dropsLike(Blocks.COBBLESTONE));
            getStateDefinition().getPossibleStates().forEach(BlockState::initCache);
        }
        @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState requested, boolean moving) {
            super.onRemove(state, level, pos, requested, moving);
            Context context = active;
            if (context == null || level != context.level || !requested.isAir()) return;
            if (pos.equals(context.actor.root)) { context.primaryOnRemove++; return; }
            if (!pos.equals(context.fault) || context.extraOnRemove != 0) return;
            context.extraOnRemove++;
            context.sawRequestedAir = level.getBlockState(pos).isAir();
            context.visAtExtraOnRemove = ItemWandCasting.getVis(context.original, "terra");
            if (context.mode == Mode.PASS) return;
            context.dirtWritten = level.setBlock(pos, Blocks.DIRT.defaultBlockState(), QUIET)
                    && level.getBlockState(pos) == Blocks.DIRT.defaultBlockState();
            level.setBlock(context.followup, defaultBlockState(), QUIET);
            if (context.mode == Mode.REPLACE_AND_SWAP)
                context.actor.player.setItemInHand(InteractionHand.MAIN_HAND, context.replacement);
            if (context.mode == Mode.REPLACE_AND_THROW) throw context.failure;
            // Return normally with DIRT instead of requested AIR. LevelChunk's
            // post-onRemove requested-block check returns null -> removeBlock false.
        }
    }

    /** Counts actual loader AFTER callbacks; never invokes harvesting/events by hand.
     * Restore EXACT old invoker, including when native onRemove throws. */
    private static final class AfterProbe implements AutoCloseable {
        private final Object event, previous;
        private final Field invoker;
        private AfterProbe(Context context) {
            try {
                Class<?> owner = Class.forName("net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents");
                event = owner.getField("AFTER").get(null);
                invoker = event.getClass().getSuperclass().getDeclaredField("invoker"); invoker.setAccessible(true);
                previous = invoker.get(event);
                Class<?> callback = Class.forName(owner.getName() + "$After");
                invoker.set(event, Proxy.newProxyInstance(callback.getClassLoader(), new Class<?>[]{callback}, (proxy, method, args) -> {
                    Object result;
                    try { result = method.invoke(previous, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                    if (args != null && args.length == 5 && args[0] == context.level && args[1] == context.actor.player) {
                        if (context.actor.root.equals(args[2])) context.primaryAfter++;
                        if (context.fault.equals(args[2])) context.extraAfter++;
                    }
                    return result;
                }));
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Native removal AFTER observer unavailable", failure); }
        }
        @Override public void close() {
            try { invoker.set(event, previous); }
            catch (IllegalAccessException failure) { throw new IllegalStateException("Cannot restore native removal AFTER observer", failure); }
        }
    }
}
