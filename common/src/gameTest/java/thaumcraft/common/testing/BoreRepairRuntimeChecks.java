package thaumcraft.common.testing;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.lib.bore.BoreWork;
import thaumcraft.common.lib.enchantment.EquipmentEnchantments;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNodeEnergized;

/** Declared raw node16x6, damaged/enchanted pick and installed Bore are inputs.
 * Existing converter helper EARNS the source with1001 production updates; after
 * setup all source and Bore updates are ordinary world ticks. No output, phase,
 * repair reserve, funding result or acceleration credit is injected. */
public final class BoreRepairRuntimeChecks {
    private BoreRepairRuntimeChecks() {}

    /** Suggested isolated wrapper timeout200; tests repair with no focus/power. */
    public static void run(GameTestHelper test) {
        Check check = new Check(test);
        try { check.prepare(); check.observe(); }
        catch (RuntimeException | Error failure) { check.close(); throw failure; }
    }

    private static final class Check implements AutoCloseable {
        private static final Map<String, Integer> COST = Map.of("aer", 6, "terra", 12, "ignis", 4, "aqua", 2, "ordo", 10, "perditio", 4);
        final GameTestHelper test;
        final TubeVariantsRuntimeChecks.Fixture network;
        BoreWorkRuntimeChecks.Fixture bore;
        TileNodeEnergized source;
        boolean closed, partial;
        Check(GameTestHelper test) {
            this.test = test; network = NodeTransductionRuntimeChecks.fixture(test, false);
            var info = BoreWorkRuntimeChecks.testInfo(test);
            info.addListener(new GameTestListener() {
                @Override public void testStructureLoaded(GameTestInfo actual) {}
                @Override public void testPassed(GameTestInfo actual, GameTestRunner runner) { close(); }
                @Override public void testFailed(GameTestInfo actual, GameTestRunner runner) { close(); }
                @Override public void testAddedForRerun(GameTestInfo previous, GameTestInfo next, GameTestRunner runner) { close(); }
            });
        }
        void prepare() {
            network.open();
            source = NodeTransductionRuntimeChecks.energize(network, network.base.east(6),
                    NodeTransductionRuntimeChecks.primals(16), null, TileNode.NodeType.NORMAL);
            bore = new BoreWorkRuntimeChecks.Fixture(test);
            ItemStack pick = new ItemStack(ThaumcraftEquipment.THAUMIUM_PICKAXE.get());
            pick.enchant(network.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EquipmentEnchantments.REPAIR), 2);
            pick.setDamageValue(10);
            var base = ObjectAspectResolver.baseTags(pick, network.level);
            test.assertTrue(base.size() == 3 && base.getAmount(Aspect.METAL) == 9 && base.getAmount(Aspect.MAGIC) == 4
                            && base.getAmount(Aspect.TREE) == 1,
                    "original recipe-derived pick base is metallum9/praecantatio4/arbor1; equipment enchantment bonuses are excluded");
            bore.top.setItem(1, pick);
            test.assertTrue(!bore.top.hasFocus() && !bore.top.isPowered() && bore.top.getAccelerationCredit() == 0,
                    "actual unpowered/no-focus Bore begins with zero acceleration and zero repair reserve");
            long revision = bore.top.revision();
            BoreWork.repair(bore.top);
            test.assertTrue(bore.top.revision() == revision && bore.top.repairState().counter() == 0,
                    "external repair request cannot borrow a native ticker or grant raw vis");
        }
        void observe() {
            // Register observers before the GameTest scheduled-task iterator exists.
            test.onEachTick(() -> {
                if (closed) return;
                try {
                    bore.requireOpen();
                    var state = bore.top.repairState();
                    test.assertTrue(!bore.top.hasFocus() && !bore.top.isPowered(), "repair stays independent of mining gates");
                    if (!partial && state.counter() >= 5) {
                        test.assertTrue(state.counter() < 10 && state.cost().equals(COST) && state.available("terra") == 4
                                        && !state.canPay() && bore.top.getItem(1).getDamageValue() == 10,
                                "first five-tick raw grant is only4 of terra12; incomplete reserve never reduces wear: " + state);
                        test.assertTrue(source.getVisBase().aspects.values().stream().allMatch(amount -> amount == 4)
                                        && source.getRefillEpoch() > 0,
                                "actual converted node supplies bounded4-unit renewable pulses, not the full repair bill");
                        partial = true;
                    }
                    if (bore.top.getItem(1).getDamageValue() != 8) return;
                    test.assertTrue(partial && state.counter() % 40 == 1 && state.cost().equals(COST)
                                    && state.available("aer") == 2 && state.available("terra") == 0 && state.available("ignis") == 0
                                    && state.available("aqua") == 0 && state.available("ordo") == 2 && state.available("perditio") == 0,
                            "native RepairII spends its complete stored primal bill once; full-price fills retain original aer/ordo overfill: " + state);
                    // Original FLOAT additions:25*.8F is still below20;
                    // the26th real4-unit pulse reaches20.799997 (raw104 total).
                    test.assertTrue(Float.floatToIntBits(bore.top.getAccelerationCredit()) == 0x41a66665 && bore.top.workReference() == null
                                    && network.level.getBlockState(bore.target).isAir(),
                            "normal separate prefill consumed104 raw perditio for original float credit20.799997; no mining/output occurred: credit=" + bore.top.getAccelerationCredit());
                    var saved = bore.top.saveWithFullMetadata(network.level.registryAccess());
                    // Explicit native NBT decode on the same idle installed fixture,
                    // as existing exhausted-revision checks do; not a world restart.
                    bore.top.loadWithComponents(saved, network.level.registryAccess());
                    test.assertTrue(bore.top.getItem(1).getDamageValue() == 8 && bore.top.getAccelerationCredit() == 20
                                    && bore.top.repairState().counter() == 0 && bore.top.repairState().cost().isEmpty()
                                    && bore.top.repairState().vis().isEmpty(),
                            "native BE save/decode retains physical wear/credit while original counter, MAX cost and repair reserve are transient");
                    close(); test.succeed();
                } catch (RuntimeException | Error failure) { close(); throw failure; }
            });
            test.runAfterDelay(160, () -> {
                if (closed) return;
                String state = bore.top.repairState().toString();
                close(); test.fail("native Bore repair did not complete from renewable4-unit source pulses: " + state);
            });
        }
        @Override public void close() {
            if (closed) return; closed = true;
            try { if (bore != null) bore.close(); }
            finally { network.close(); }
        }
    }
}
