package thaumcraft.essentia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.essentia.BoreInventoryPlan;
import thaumcraft.common.lib.essentia.BoreInventoryPlan.Inventory;
import thaumcraft.common.lib.essentia.BoreInventoryPlan.Reason;
import thaumcraft.common.lib.essentia.BoreInventoryPlan.Refused;
import thaumcraft.common.lib.essentia.BoreInventoryPlan.Stack;
import static org.junit.jupiter.api.Assertions.*;

/** Native ItemStack/data assertions only. No native container or delivery claim. */
class BoreInventoryPlanTest {
    private static HolderLookup.Provider registries;

    @BeforeAll static void bootstrapNativeStacks() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = VanillaRegistries.createLookup();
    }

    @Test void multipleStacksWalkOneEvolvingShadowWithoutChangingBeforeOrSources() {
        ItemStack first = new ItemStack(Items.COBBLESTONE, 100), second = new ItemStack(Items.COBBLESTONE, 50);
        var sourceList = new ArrayList<>(List.of(freeze(first, 64), freeze(second, 64)));
        var slots = emptySlots(27);
        slots.set(0, cobble(60)); slots.set(2, cobble(61));
        var near = Inventory.chest(slots, allowed(2, 27));
        var plan = BoreInventoryPlan.plan(sourceList, near);
        assertCounts(plan.nearAfter(), 64, 64, 64, 64, 15);
        assertArrayEquals(new int[]{0, 1, 2, 3, 4}, plan.nearChangedSlots());
        assertEquals(150, plan.bundleItems()); assertEquals(150, plan.insertedItems());
        assertEquals(0, plan.residualItems()); assertTrue(plan.residuals().isEmpty());
        assertEquals(60, plan.nearBefore().get(0).count()); assertEquals(61, plan.nearBefore().get(2).count());
        assertTrue(slots.get(1).isEmpty()); assertEquals(60, near.slots().get(0).count());
        assertEquals(100, first.getCount()); assertEquals(50, second.getCount());
        slots.clear(); sourceList.clear();
        assertEquals(27, plan.nearBefore().size()); assertEquals(2, plan.bundle().size());
        assertConserved(plan);
    }

    @Test void nearMergesThenFarMergesTakePriorityOverNearEmptySlots() {
        var nearSlots = emptySlots(27); nearSlots.set(9, cobble(63));
        var farSlots = emptySlots(27); farSlots.set(0, cobble(62));
        var plan = BoreInventoryPlan.plan(List.of(cobble(3), cobble(4)),
                Inventory.chest(nearSlots, allowed(2, 27)), Inventory.chest(farSlots, allowed(2, 27)));
        assertEquals(64, plan.nearAfter().get(9).count()); assertEquals(64, plan.farAfter().get(0).count());
        assertEquals(4, plan.nearAfter().get(0).count(), "later member sees both earlier merge writes");
        assertEquals(-1, plan.walks().get(0).nearSlotsVisited());
        assertEquals(1, plan.walks().get(0).farMergeVisited());
        assertEquals(-1, plan.walks().get(0).farMergeAgainVisited());
        assertArrayEquals(new int[]{0, 9}, plan.nearChangedSlots());
        assertArrayEquals(new int[]{0}, plan.farChangedSlots());
        assertConserved(plan);
    }

    @Test void fullNearNeverUsesFarEmptySlotsAndActuallyReachesTheSecondFarMergePass() {
        var far = fullDirt(27); far.set(0, Stack.empty()); far.set(4, cobble(62));
        var plan = BoreInventoryPlan.plan(List.of(cobble(10)), Inventory.chest(fullDirt(27), allowed(1, 27)),
                Inventory.chest(far, allowed(1, 27)));
        assertTrue(plan.farAfter().get(0).isEmpty()); assertEquals(64, plan.farAfter().get(4).count());
        assertResidual(plan, 0, 8); assertEquals(2, plan.insertedItems());
        assertArrayEquals(new int[0], plan.nearChangedSlots()); assertArrayEquals(new int[]{4}, plan.farChangedSlots());
        var walk = plan.walks().getFirst();
        assertEquals(27, walk.nearMergeVisited()); assertEquals(27, walk.farMergeVisited());
        assertEquals(27, walk.nearSlotsVisited()); assertEquals(27, walk.farMergeAgainVisited());
        assertConserved(plan);
    }

    @Test void entirelyEmptyFarChestDoesNotTurnIntoACombined54SlotTarget() {
        var plan = BoreInventoryPlan.plan(List.of(cobble(120)), Inventory.chest(fullDirt(27), allowed(1, 27)),
                Inventory.chest(emptySlots(27), allowed(1, 27)));
        assertResidual(plan, 0, 120); assertEquals(0, plan.insertedItems());
        assertTrue(plan.farAfter().stream().allMatch(Stack::isEmpty));
        assertEquals(27, plan.walks().getFirst().farMergeAgainVisited());
        assertConserved(plan);
    }

    @Test void ordinaryCanPlaceRefusalDoesNotGetBypassedByLaterPasses() {
        var nearSlots = fullDirt(27); nearSlots.set(0, Stack.empty()); nearSlots.set(1, cobble(60));
        var farSlots = fullDirt(27); farSlots.set(2, cobble(62));
        boolean[][] nearAllowed = allowed(1, 27); nearAllowed[0][0] = false; nearAllowed[0][1] = false;
        var plan = BoreInventoryPlan.plan(List.of(cobble(8)), Inventory.chest(nearSlots, nearAllowed),
                Inventory.chest(farSlots, allowed(1, 27)));
        assertTrue(plan.nearAfter().get(0).isEmpty()); assertEquals(60, plan.nearAfter().get(1).count());
        assertEquals(64, plan.farAfter().get(2).count()); assertResidual(plan, 0, 6);
        assertArrayEquals(new int[0], plan.nearChangedSlots()); assertConserved(plan);
    }

    @Test void fixed64BoundsBothEmptySlotsAndMergesEvenWithNativeItemMaximum99() {
        ItemStack old = max99(63), incoming = max99(100);
        var slots = emptySlots(5); slots.set(0, freeze(old, 99));
        var plan = BoreInventoryPlan.plan(List.of(freeze(incoming, 99)), Inventory.hopper(slots, allowed(1, 5)));
        assertCounts(plan.nearAfter(), 64, 64, 35);
        assertEquals(64, BoreInventoryPlan.INVENTORY_LIMIT);
        assertEquals(63, old.getCount()); assertEquals(100, incoming.getCount());
        assertEquals(99, plan.nearAfter().get(1).itemMaximum());
        assertTrue(plan.residuals().isEmpty()); assertConserved(plan);
    }

    @Test void originalEmptySlotLimitIs64RatherThanItemMaximum16() {
        var slots = emptySlots(5); slots.set(0, freeze(new ItemStack(Items.ENDER_PEARL, 10), 16));
        var plan = BoreInventoryPlan.plan(List.of(freeze(new ItemStack(Items.ENDER_PEARL, 70), 16)),
                Inventory.hopper(slots, allowed(1, 5)));
        assertCounts(plan.nearAfter(), 16, 64);
        assertEquals(16, plan.nearAfter().get(1).itemMaximum());
        assertTrue(plan.residuals().isEmpty()); assertConserved(plan);
    }

    @Test void oldUnstackableOvermaximumIsPreservedAndNonpositiveRoomNeverTransfersBack() {
        ItemStack old = new ItemStack(Items.DIAMOND_PICKAXE, 10);
        old.setDamageValue(7); old.set(DataComponents.CUSTOM_NAME, Component.literal("old pick bundle"));
        var slots = emptySlots(5); slots.set(0, freeze(old, 1));
        var first = old.copyWithCount(3); var second = old.copyWithCount(2);
        var plan = BoreInventoryPlan.plan(List.of(freeze(first, 1), freeze(second, 1)),
                Inventory.hopper(slots, allowed(2, 5)));
        assertCounts(plan.nearAfter(), 10, 3, 2);
        assertTrue(ItemStack.matches(old, plan.nearAfter().get(0).copyStack(registries)));
        assertEquals(7, plan.nearAfter().get(1).copyStack(registries).getDamageValue());
        assertEquals(10, old.getCount()); assertEquals(3, first.getCount()); assertEquals(2, second.getCount());
        assertArrayEquals(new int[]{1, 2}, plan.nearChangedSlots()); assertConserved(plan);
    }

    @Test void unstackableOutputCanFillAnEmptySlotTo64ButNeverMergeIntoItsOverstack() {
        var pick = freeze(new ItemStack(Items.DIAMOND_PICKAXE, 65), 1);
        var plan = BoreInventoryPlan.plan(List.of(pick, freeze(new ItemStack(Items.DIAMOND_PICKAXE, 1), 1)),
                Inventory.hopper(emptySlots(5), allowed(2, 5)));
        assertCounts(plan.nearAfter(), 64, 1, 1); assertTrue(plan.residuals().isEmpty());
        assertEquals(65, pick.copyStack(registries).getCount()); assertConserved(plan);
    }

    @Test void sidedMergeThenOrdinaryPassUsesExposureOrderAndNeverHiddenSlots() {
        var slots = emptySlots(3); slots.set(0, cobble(63)); slots.set(2, cobble(60));
        var plan = BoreInventoryPlan.plan(List.of(cobble(9)), Inventory.furnace(slots, new int[]{2, 0}, allowed(1, 3)));
        assertCounts(plan.nearAfter(), 64, 0, 64); assertResidual(plan, 0, 4);
        assertEquals(2, plan.walks().getFirst().nearMergeVisited());
        assertEquals(2, plan.walks().getFirst().nearSlotsVisited());
        assertEquals(-1, plan.walks().getFirst().farMergeVisited()); assertConserved(plan);
    }

    @Test void sidedEmptySlotPriorityIsTheFrozenExposureOrderNotSortedPhysicalOrder() {
        var plan = BoreInventoryPlan.plan(List.of(cobble(70)),
                Inventory.furnace(emptySlots(3), new int[]{2, 0}, allowed(1, 3)));
        assertCounts(plan.nearAfter(), 6, 0, 64); assertTrue(plan.residuals().isEmpty());
        assertConserved(plan);
    }

    @Test void combinedCanPlaceAndThroughFacePermissionsAreCopiedPerBundleMember() {
        boolean[][] canPlace = {{true, true, true}, {true, true, true}};
        boolean[][] through = {{false, false, true}, {true, false, false}};
        boolean[][] combined = new boolean[2][3];
        for (int member = 0; member < 2; member++) for (int slot = 0; slot < 3; slot++) {
            combined[member][slot] = canPlace[member][slot] && through[member][slot];
        }
        int[] exposure = {2, 0};
        var snapshot = Inventory.furnace(emptySlots(3), exposure, combined);
        exposure[0] = 1; combined[0][2] = false; combined[1][0] = false;
        var returnedOrder = snapshot.slotOrder(); returnedOrder[0] = 1;
        var plan = BoreInventoryPlan.plan(List.of(cobble(10), cobble(20)), snapshot);
        assertCounts(plan.nearAfter(), 20, 0, 10); assertArrayEquals(new int[]{2, 0}, snapshot.slotOrder());
        assertConserved(plan);
    }

    @Test void falseCombinedPermissionPreventsBothMergeAndEmptyInsertion() {
        var slots = emptySlots(3); slots.set(2, cobble(60));
        var plan = BoreInventoryPlan.plan(List.of(cobble(9)),
                Inventory.furnace(slots, new int[]{2, 1, 0}, new boolean[][]{{true, false, false}}));
        assertCounts(plan.nearAfter(), 9, 0, 60);
        assertEquals(60, plan.nearBefore().get(2).count()); assertConserved(plan);
    }

    @Test void nullOrEmptySidedExposureMeansNoSlotsNotAnOrdinaryInventoryFallback() {
        for (int[] exposure : new int[][]{null, new int[0]}) {
            var plan = BoreInventoryPlan.plan(List.of(cobble(100)), Inventory.furnace(emptySlots(3), exposure, allowed(1, 3)));
            assertResidual(plan, 0, 100); assertEquals(0, plan.insertedItems());
            assertEquals(0, plan.walks().getFirst().nearMergeVisited());
            assertEquals(0, plan.walks().getFirst().nearSlotsVisited()); assertConserved(plan);
        }
    }

    @Test void fullComponentIdentityControlsMergesIncludingNameDamageCustomDataAndEnchantments() {
        ItemStack old = rich(60);
        var variants = new ArrayList<ItemStack>();
        ItemStack name = rich(10); name.set(DataComponents.CUSTOM_NAME, Component.literal("different")); variants.add(name);
        ItemStack damage = rich(10); damage.set(DataComponents.DAMAGE, 8); variants.add(damage);
        ItemStack data = rich(10); data.get(DataComponents.CUSTOM_DATA).getUnsafe().putString("marker", "different"); variants.add(data);
        ItemStack enchantment = rich(10);
        enchantment.enchant(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 2);
        variants.add(enchantment);
        for (ItemStack variant : variants) {
            var slots = emptySlots(5); slots.set(0, freeze(old, 64));
            var plan = BoreInventoryPlan.plan(List.of(freeze(variant, 64)), Inventory.hopper(slots, allowed(1, 5)));
            assertCounts(plan.nearAfter(), 60, 10);
            assertTrue(ItemStack.matches(variant, plan.nearAfter().get(1).copyStack(registries)));
            assertTrue(ItemStack.matches(old, plan.nearAfter().get(0).copyStack(registries))); assertConserved(plan);
        }
        var slots = emptySlots(5); slots.set(0, freeze(old, 64));
        var matching = BoreInventoryPlan.plan(List.of(freeze(rich(10), 64)), Inventory.hopper(slots, allowed(1, 5)));
        assertCounts(matching.nearAfter(), 64, 6); assertConserved(matching);
    }

    @Test void codecViewsUseCount1AndAllMutableNativeCopiesAreDeeplyDetached() {
        ItemStack source = rich(120);
        ItemStack expected = rich(120);
        Stack frozen = freeze(source, 64);
        source.setCount(1);
        ((MutableComponent) source.get(DataComponents.CUSTOM_NAME)).append(" mutated");
        source.get(DataComponents.CUSTOM_DATA).getUnsafe().getCompound("nested").putInt("value", 999);
        assertTrue(ItemStack.matches(expected, frozen.copyStack(registries)));
        var slots = emptySlots(5); slots.set(0, freeze(rich(60), 64));
        var plan = BoreInventoryPlan.plan(List.of(frozen), Inventory.hopper(slots, allowed(1, 5)));
        for (List<Stack> values : List.of(plan.bundle(), plan.nearBefore(), plan.nearAfter())) {
            for (Stack value : values) {
                ItemStack before = value.copyStack(registries), mutable = value.copyStack(registries);
                assertNotSame(before, mutable);
                if (!value.isEmpty()) {
                    mutable.setCount(2);
                    mutable.get(DataComponents.CUSTOM_DATA).getUnsafe().getCompound("nested").putInt("value", 12345);
                    var decodedName = (MutableComponent) mutable.get(DataComponents.CUSTOM_NAME);
                    try { decodedName.append(" external"); }
                    catch (UnsupportedOperationException immutableSiblings) {
                        // Native decoded sibling lists may be immutable. That
                        // refusal is safe, not a planner failure; still mutate
                        // the decoded component's supported style field below.
                    }
                    decodedName.setStyle(decodedName.getStyle().withBold(true));
                } else mutable.setCount(17);
                assertTrue(ItemStack.matches(before, value.copyStack(registries)));
            }
        }
        assertTrue(ItemStack.matches(expected, frozen.copyStack(registries))); assertConserved(plan);
    }

    @Test void leftoverAbove99RemainsExactlyOneResidualPerOriginalMemberWithComponents() {
        var first = freeze(rich(200), 64); var second = freeze(rich(150), 64);
        var nearSlots = fullDirt(5); nearSlots.set(0, freeze(rich(60), 64));
        var plan = BoreInventoryPlan.plan(List.of(first, second), Inventory.hopper(nearSlots, allowed(2, 5)));
        assertEquals(2, plan.residuals().size());
        assertEquals(0, plan.residuals().get(0).bundleIndex()); assertEquals(196, plan.residuals().get(0).stack().count());
        assertEquals(1, plan.residuals().get(1).bundleIndex()); assertEquals(150, plan.residuals().get(1).stack().count());
        assertTrue(ItemStack.matches(rich(196), plan.residuals().get(0).stack().copyStack(registries)));
        assertTrue(ItemStack.matches(rich(150), plan.residuals().get(1).stack().copyStack(registries)));
        var copy = plan.residuals().get(0).stack().copyStack(registries); copy.setCount(1);
        copy.get(DataComponents.CUSTOM_DATA).getUnsafe().putString("marker", "external");
        assertTrue(ItemStack.matches(rich(196), plan.residuals().get(0).stack().copyStack(registries)));
        assertEquals(350, plan.bundleItems()); assertEquals(4, plan.insertedItems()); assertEquals(346, plan.residualItems());
        assertConserved(plan);
    }

    @Test void residualIndicesDoNotRenumberAroundFullyInsertedMembers() {
        var slots = fullDirt(5); slots.set(0, cobble(63));
        var plan = BoreInventoryPlan.plan(List.of(cobble(1), cobble(130)), Inventory.hopper(slots, allowed(2, 5)));
        assertResidual(plan, 1, 130); assertEquals(1, plan.insertedItems()); assertConserved(plan);
    }

    @Test void allReturnedListsAndIndexArraysCannotModifyTheRetainedPlan() {
        var far = emptySlots(27); far.set(0, cobble(63));
        var near = Inventory.chest(emptySlots(27), allowed(1, 27));
        var plan = BoreInventoryPlan.plan(List.of(cobble(3)), near, Inventory.chest(far, allowed(1, 27)));
        for (List<Stack> values : List.of(plan.bundle(), plan.nearBefore(), plan.farBefore(), plan.nearAfter(), plan.farAfter(), near.slots())) {
            assertThrows(UnsupportedOperationException.class, () -> values.add(Stack.empty()));
        }
        assertThrows(UnsupportedOperationException.class, () -> plan.walks().clear());
        var residualPlan = BoreInventoryPlan.plan(List.of(cobble(3)), Inventory.hopper(fullDirt(5), allowed(1, 5)));
        assertThrows(UnsupportedOperationException.class, () -> residualPlan.residuals().clear());
        int[] nearChanged = plan.nearChangedSlots(), farChanged = plan.farChangedSlots();
        nearChanged[0] = 26; farChanged[0] = 26;
        assertArrayEquals(new int[]{0}, plan.nearChangedSlots()); assertArrayEquals(new int[]{0}, plan.farChangedSlots());
        var farMutable = plan.farAfter().get(0).copyStack(registries); farMutable.setCount(1);
        assertEquals(64, plan.farAfter().get(0).count()); assertEquals(63, plan.farBefore().get(0).count());
        assertConserved(plan);
    }

    @Test void maximumSourceCountWorksButAnUnrepresentableWholeBundleRefusesWithoutTruncation() {
        var maximum = freeze(new ItemStack(Items.COBBLESTONE, Integer.MAX_VALUE), 64);
        var full = Inventory.hopper(fullDirt(5), allowed(1, 5));
        var plan = BoreInventoryPlan.plan(List.of(maximum), full);
        assertResidual(plan, 0, Integer.MAX_VALUE); assertEquals(Integer.MAX_VALUE, plan.bundleItems());
        assertEquals(Integer.MAX_VALUE, maximum.copyStack(registries).getCount()); assertConserved(plan);
        var fullForTwo = Inventory.hopper(fullDirt(5), allowed(2, 5));
        var refusal = assertThrows(Refused.class, () -> BoreInventoryPlan.plan(List.of(maximum, cobble(1)), fullForTwo));
        assertEquals(Reason.BUNDLE_TOTAL_OVERFLOW, refusal.reason());
        assertEquals(Integer.MAX_VALUE, maximum.count()); assertEquals(64, fullForTwo.slots().get(0).count());
    }

    @Test void invalidInventoryCapacityExposureAndPermissionsNeverProduceAPartialPlan() {
        var oversized = emptySlots(27); oversized.set(3, cobble(65));
        assertReason(Reason.INVALID_SLOT_COUNT, () -> Inventory.chest(oversized, allowed(1, 27)));
        assertEquals(65, oversized.get(3).count());
        assertReason(Reason.INVALID_INVENTORY, () -> Inventory.chest(emptySlots(26), allowed(1, 26)));
        var nullSlot = emptySlots(5); nullSlot.set(0, null);
        assertReason(Reason.INVALID_INVENTORY, () -> Inventory.hopper(nullSlot, allowed(1, 5)));
        for (int[] exposure : new int[][]{{-1}, {3}, {1, 1}}) {
            var slots = emptySlots(3);
            assertReason(Reason.INVALID_EXPOSURE, () -> Inventory.furnace(slots, exposure, allowed(1, 3)));
            assertTrue(slots.stream().allMatch(Stack::isEmpty));
        }
        assertReason(Reason.INVALID_PERMISSIONS, () -> Inventory.hopper(emptySlots(5), null));
        assertReason(Reason.INVALID_PERMISSIONS, () -> Inventory.hopper(emptySlots(5), new boolean[][]{null}));
        assertReason(Reason.INVALID_PERMISSIONS, () -> Inventory.hopper(emptySlots(5), new boolean[1][4]));
        var wrongRows = Inventory.hopper(emptySlots(5), allowed(0, 5));
        assertReason(Reason.INVALID_PERMISSIONS, () -> BoreInventoryPlan.plan(List.of(cobble(1)), wrongRows));
        assertTrue(wrongRows.slots().stream().allMatch(Stack::isEmpty));
    }

    @Test void invalidStacksBundleAndPairingRefuseRatherThanReinterpretInputs() {
        for (int count : new int[]{0, -1, Integer.MIN_VALUE}) {
            assertReason(Reason.INVALID_STACK, () -> freeze(new ItemStack(Items.COBBLESTONE, count), 64));
        }
        assertReason(Reason.INVALID_ITEM_MAXIMUM, () -> freeze(new ItemStack(Items.COBBLESTONE), 0));
        assertReason(Reason.INVALID_ITEM_MAXIMUM, () -> freeze(new ItemStack(Items.COBBLESTONE), -1));
        assertReason(Reason.INVALID_STACK, () -> Stack.of(registries, null, 64));
        var chest = Inventory.chest(emptySlots(27), allowed(1, 27));
        var hopper = Inventory.hopper(emptySlots(5), allowed(1, 5));
        assertReason(Reason.INVALID_BUNDLE, () -> BoreInventoryPlan.plan(null, chest));
        assertReason(Reason.INVALID_BUNDLE, () -> BoreInventoryPlan.plan(List.of(Stack.empty()), chest));
        assertReason(Reason.INVALID_BUNDLE, () -> BoreInventoryPlan.plan(Arrays.asList((Stack) null), chest));
        assertReason(Reason.INVALID_INVENTORY, () -> BoreInventoryPlan.plan(List.of(cobble(1)), null));
        assertReason(Reason.INVALID_PAIR, () -> BoreInventoryPlan.plan(List.of(cobble(1)), chest, chest));
        assertReason(Reason.INVALID_PAIR, () -> BoreInventoryPlan.plan(List.of(cobble(1)), chest, hopper));
        assertReason(Reason.INVALID_PAIR, () -> BoreInventoryPlan.plan(List.of(cobble(1)), hopper, chest));
    }

    @Test void aLaterContradictoryMaximumDiscardsTheWholeShadowNotOnlyTheBadMember() {
        var slots = emptySlots(5); slots.set(0, cobble(60));
        var snapshot = Inventory.hopper(slots, allowed(2, 5));
        var first = freeze(new ItemStack(Items.DIRT), 64);
        var contradictory = freeze(new ItemStack(Items.COBBLESTONE), 32);
        assertReason(Reason.INCONSISTENT_ITEM_MAXIMUM, () -> BoreInventoryPlan.plan(List.of(first, contradictory), snapshot));
        assertEquals(60, snapshot.slots().get(0).count()); assertTrue(snapshot.slots().get(1).isEmpty());
        assertEquals(1, first.count()); assertEquals(1, contradictory.count());
    }

    @Test void emptyBundleHasAnUnchangedWholePlanRatherThanInventingAnOutput() {
        var plan = BoreInventoryPlan.plan(List.of(), Inventory.hopper(fullDirt(5), allowed(0, 5)));
        assertEquals(0, plan.bundleItems()); assertEquals(0, plan.insertedItems()); assertEquals(0, plan.residualItems());
        assertTrue(plan.residuals().isEmpty()); assertTrue(plan.walks().isEmpty());
        assertArrayEquals(new int[0], plan.nearChangedSlots()); assertConserved(plan);
    }

    private static Stack freeze(ItemStack stack, int maximum) { return Stack.of(registries, stack, maximum); }
    private static Stack cobble(int count) { return freeze(new ItemStack(Items.COBBLESTONE, count), 64); }
    private static ItemStack max99(int count) {
        var stack = new ItemStack(Items.COBBLESTONE, count); stack.set(DataComponents.MAX_STACK_SIZE, 99); return stack;
    }
    private static ItemStack rich(int count) {
        var stack = new ItemStack(Items.COBBLESTONE, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("component-rich").append(" bundle"));
        stack.set(DataComponents.DAMAGE, 7);
        var nested = new CompoundTag(); nested.putInt("value", 42);
        var data = new CompoundTag(); data.putString("marker", "original"); data.put("nested", nested);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        stack.enchant(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 1);
        return stack;
    }
    private static ArrayList<Stack> emptySlots(int size) { return new ArrayList<>(Collections.nCopies(size, Stack.empty())); }
    private static ArrayList<Stack> fullDirt(int size) {
        return new ArrayList<>(Collections.nCopies(size, freeze(new ItemStack(Items.DIRT, 64), 64)));
    }
    private static boolean[][] allowed(int members, int slots) {
        boolean[][] result = new boolean[members][slots];
        for (boolean[] row : result) Arrays.fill(row, true);
        return result;
    }
    private static void assertCounts(List<Stack> slots, int... prefix) {
        for (int slot = 0; slot < slots.size(); slot++) assertEquals(slot < prefix.length ? prefix[slot] : 0, slots.get(slot).count(), "slot " + slot);
    }
    private static void assertResidual(BoreInventoryPlan plan, int member, int count) {
        assertEquals(1, plan.residuals().size()); assertEquals(member, plan.residuals().getFirst().bundleIndex());
        assertEquals(count, plan.residuals().getFirst().stack().count());
    }
    private static void assertReason(Reason reason, org.junit.jupiter.api.function.Executable action) {
        assertEquals(reason, assertThrows(Refused.class, action).reason());
    }
    private static long total(List<Stack> slots) { return slots.stream().mapToLong(Stack::count).sum(); }
    private static void assertConserved(BoreInventoryPlan plan) {
        long before = total(plan.nearBefore()) + total(plan.farBefore());
        long after = total(plan.nearAfter()) + total(plan.farAfter());
        assertEquals(before + plan.bundleItems(), after + plan.residualItems());
        assertEquals(plan.bundleItems(), plan.insertedItems() + plan.residualItems());
    }
}
