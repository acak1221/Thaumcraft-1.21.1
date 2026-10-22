package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.ReportedException;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;
import thaumcraft.common.lib.world.ExcavationHarvest;

/** Actual held-channel Fortune/Enlarge/protection fixtures. Synchronous, bounded,
 * own connected player; no seed, fake clock, manual drops or new registered blocks.
 * Root registers this GameTestHelper-only entry with timeout<=240 ticks. */
public final class ExcavationUpgradeRuntimeChecks {
    private ExcavationUpgradeRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        try (var actor = new FocusUpgradeRuntimeChecks.Actor(test)) {
            treasure(actor); neighbours(actor); cancellations(actor); mutationFences(actor); afterReservation(actor);
        }
        test.succeed();
    }

    private static ItemStack enlarged(FocusUpgradeRuntimeChecks.Actor a) {
        return a.focus(ThaumcraftFoci.EXCAVATION_FOCUS.get(), FocusUpgradeType.enlarge, 2, FocusUpgradeType.enlarge, 4);
    }

    private static void prepare(FocusUpgradeRuntimeChecks.Actor a, int vis) {
        a.clearLane(); a.equip(enlarged(a), "gold", vis); a.aim();
        a.put(a.root, Blocks.TNT.defaultBlockState());
        // EAST would occlude the root ray from this real caster; never fixture it.
        a.put(a.root.north(), Blocks.TNT.defaultBlockState());
        a.put(a.root.south(), Blocks.TNT.defaultBlockState());
        a.put(a.root.west(), Blocks.TNT.defaultBlockState());
    }

    private static int removed(FocusUpgradeRuntimeChecks.Actor a) {
        int count = a.level.isEmptyBlock(a.root) ? 1 : 0;
        for (Direction dir : List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST)) if (a.level.isEmptyBlock(a.root.relative(dir))) count++;
        return count;
    }

    private static void treasure(FocusUpgradeRuntimeChecks.Actor a) {
        a.clearLane();
        ItemStack focus = a.focus(ThaumcraftFoci.EXCAVATION_FOCUS.get(), FocusUpgradeType.treasure, 1,
                FocusUpgradeType.treasure, 3, FocusUpgradeType.treasure, 5);
        a.equip(focus, "gold", 100); a.aim();
        a.wand.enchant(a.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        var actualEnchantments = a.wand.get(DataComponents.ENCHANTMENTS);
        a.put(a.root, Blocks.GRAVEL.defaultBlockState()); a.begin(); a.tick(5);
        a.require(a.level.isEmptyBlock(a.root) && a.vis("terra") == 85, "three Treasure ranks retain exact completed-attempt price/timing");
        // Vanilla gravel Fortune3 guarantees flint: deterministic evidence of the
        // real loot tool, not a probabilistic 'some ores should give extra' test.
        a.require(a.items().stream().mapToInt(entity -> entity.getItem().is(Items.FLINT) ? entity.getItem().getCount() : 0).sum() == 1
                && a.items().stream().noneMatch(entity -> entity.getItem().is(Items.GRAVEL)), "actual native Fortune3 produces guaranteed flint despite Silk on the real wand");
        a.require(actualEnchantments.equals(a.wand.get(DataComponents.ENCHANTMENTS)), "private Fortune policy never strips/enchants the actual held wand");
    }

    private static void neighbours(FocusUpgradeRuntimeChecks.Actor a) {
        prepare(a, 100);
        a.put(a.root.west(), Blocks.TNT.defaultBlockState().setValue(TntBlock.UNSTABLE, true));
        a.put(a.root.north(2), Blocks.TNT.defaultBlockState());
        a.put(a.root.north().above(), Blocks.TNT.defaultBlockState());
        a.begin(); a.tick(2);
        a.require(removed(a) == 3 && a.vis("terra") == 55, "Enlarge2 removes root plus two immediate matches and pays exactly three attempts");
        a.require(a.level.getBlockState(a.root.west()).getValue(TntBlock.UNSTABLE)
                && a.level.getBlockState(a.root.north(2)).is(Blocks.TNT) && a.level.getBlockState(a.root.north().above()).is(Blocks.TNT),
                "exact native-state match, no recursive/diagonal/radius mining");
        a.require(a.items().stream().mapToInt(entity -> entity.getItem().is(Items.TNT) ? entity.getItem().getCount() : 0).sum() == 3, "each witnessed native removal drops once");

        prepare(a, 15); a.begin(); a.tick(2);
        a.require(removed(a) == 1 && a.vis("terra") == 0, "last funded primary does not authorize unfunded extra removal");

        a.clearLane(); a.equip(enlarged(a), "gold", 100); a.aim(); a.put(a.root, Blocks.TNT.defaultBlockState());
        a.begin(); a.tick(2);
        a.require(a.level.isEmptyBlock(a.root) && a.vis("terra") == 85, "funded no-match iterations consume no extra vis");
    }

    private static void cancellations(FocusUpgradeRuntimeChecks.Actor a) {
        prepare(a, 100); int[] calls = new int[2];
        BlockEvent.Break listener = (world, pos, state, player, xp) -> {
            if (world != a.level || player != a.player) return EventResult.pass();
            calls[pos.equals(a.root) ? 0 : 1]++;
            if (!pos.equals(a.root)) {
                a.require(a.vis("terra") == 85, "every rejected extra BREAK sees unreserved funds; cancellation is free");
                return EventResult.interruptFalse();
            }
            return EventResult.pass();
        };
        BlockEvent.BREAK.register(listener);
        try { a.begin(); a.tick(2); } finally { BlockEvent.BREAK.unregister(listener); }
        a.require(calls[0] == 1 && calls[1] == 6 && removed(a) == 1 && a.vis("terra") == 85,
                "two fresh funded iterations each try all three cancelled matching neighbours, once per real event");

        prepare(a, 100); calls[0] = calls[1] = 0;
        BlockEvent.Break denyRoot = (world, pos, state, player, xp) -> {
            if (world != a.level || player != a.player) return EventResult.pass();
            calls[pos.equals(a.root) ? 0 : 1]++; return EventResult.interruptFalse();
        };
        BlockEvent.BREAK.register(denyRoot);
        try { a.begin(); a.tick(2); } finally { BlockEvent.BREAK.unregister(denyRoot); }
        a.require(calls[0] == 1 && calls[1] == 0 && removed(a) == 0 && a.vis("terra") == 85 && a.items().isEmpty(),
                "cancelled paid primary never mints an expansion permit or drops");
    }

    private static void mutationFences(FocusUpgradeRuntimeChecks.Actor a) {
        prepare(a, 100);
        ItemStack original = a.wand, replacement = FocusUpgradeRuntimeChecks.Actor.charged(enlarged(a), "gold", 777);
        int[] calls = {0};
        BlockEvent.Break swap = (world, pos, state, player, xp) -> {
            if (world == a.level && player == a.player && !pos.equals(a.root)) {
                calls[0]++; a.player.setItemInHand(InteractionHand.MAIN_HAND, replacement);
            }
            return EventResult.pass();
        };
        BlockEvent.BREAK.register(swap);
        try { a.begin(); a.tick(2); } finally { BlockEvent.BREAK.unregister(swap); }
        a.require(calls[0] == 1 && removed(a) == 1 && ItemWandCasting.getVis(original, "terra") == 85
                && ItemWandCasting.getVis(replacement, "terra") == 777, "pre-removal hand replacement refuses extra without charging either the old reservation or unrelated new hand");

        prepare(a, 100);
        BlockEvent.Break drain = (world, pos, state, player, xp) -> {
            if (world == a.level && player == a.player && !pos.equals(a.root))
                a.wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(a.wand).withAmount("terra", 0));
            return EventResult.pass();
        };
        BlockEvent.BREAK.register(drain);
        try { a.begin(); a.tick(2); } finally { BlockEvent.BREAK.unregister(drain); }
        a.require(removed(a) == 1 && a.vis("terra") == 0, "callback-owned funding change survives; no unpaid extra and no snapshot reset/refund");

        prepare(a, 100); var failure = new IllegalStateException("owned extra BREAK fixture exception");
        BlockEvent.Break throwing = (world, pos, state, player, xp) -> {
            if (world == a.level && player == a.player && !pos.equals(a.root)) throw failure;
            return EventResult.pass();
        };
        boolean propagated = false;
        BlockEvent.BREAK.register(throwing);
        try { a.begin(); a.tick(2); }
        catch (ReportedException reported) {
            propagated = reported.getCause() == failure && reported.getReport().getException() == failure;
        }
        finally { BlockEvent.BREAK.unregister(throwing); }
        a.require(propagated && removed(a) == 1 && a.vis("terra") == 85 && ItemFocusExcavation.progress(a.player).amount() == 0,
                "native Ticking player wrapper retains the EXACT extra BREAK failure in cause/report; paid primary/reset retained with no leaked free removal");
        prepare(a, 100); a.begin(); a.tick(2);
        a.require(removed(a) == 3 && a.vis("terra") == 55, "new native attempt after exception has no stale permit/frame/reservation");
    }

    private static void afterReservation(FocusUpgradeRuntimeChecks.Actor a) {
        prepare(a, 100);
        ItemStack original = a.wand, replacement = FocusUpgradeRuntimeChecks.Actor.charged(enlarged(a), "gold", 777);
        int[] observed = {0};
        try (var probe = new AfterProbe(a, pos -> {
            if (pos.equals(a.root)) { a.require(a.vis("terra") == 85, "primary AFTER observes its prior payment"); return; }
            observed[0]++;
            a.require(a.vis("terra") == 70, "extra AFTER observes documented final-removal reservation, not a free successful removal");
            a.require(!ExcavationHarvest.attempt(a.player, pos, a.wand), "callback cannot recursively borrow this native permit");
            a.player.setItemInHand(InteractionHand.MAIN_HAND, replacement);
        })) {
            a.begin(); a.tick(2);
        }
        a.require(observed[0] == 1 && removed(a) == 2 && ItemWandCasting.getVis(original, "terra") == 70
                && ItemWandCasting.getVis(replacement, "terra") == 777, "successful extra settles original reservation exactly once; replacement is never debited and next extra stops");

        prepare(a, 100); observed[0] = 0;
        try (var probe = new AfterProbe(a, pos -> {
            if (pos.equals(a.root)) return;
            observed[0]++; a.require(a.vis("terra") == 70, "reservation exists before external post-success drain");
            a.wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(a.wand).withAmount("terra", 0));
        })) { a.begin(); a.tick(2); }
        a.require(observed[0] == 1 && removed(a) == 2 && a.vis("terra") == 0, "post-success drain cannot erase billing, cause second debit, mint refund or authorize another extra");
    }

    /** The loader's own AFTER phase. The loader owns the mechanism: Fabric decorates its own
     * AFTER event, NeoForge observes its real native removal. No loader class is named here. */
    private static final class AfterProbe implements AutoCloseable {
        private final NativeBreakPhases probe;
        AfterProbe(FocusUpgradeRuntimeChecks.Actor actor, Consumer<BlockPos> action) {
            probe = NativeBreakPhases.Holder.factory().install(actor.level, actor.player,
                    NativeBreakPhases.Phase.AFTER, new NativeBreakPhases.Listener() {
                        @Override public void after(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
                            if (level != actor.level || player != actor.player) return;
                            action.accept(pos);
                        }
                    });
        }
        @Override public void close() { probe.close(); }
    }
}
