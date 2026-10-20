package thaumcraft.common.lib.crafting;

import java.util.List;

/**
 * Original TC4 machine/device container decisions, extracted from the oracle
 * containers into pure functions so the 1.21.1 slot and container layer and its
 * unit tests share one authority.
 *
 * <p>Oracle: {@code thaumcraft/common/container/}. Every number below is the
 * original value; nothing here invents a slot, a limit or a routing.</p>
 */
public final class DeviceContainerRules {
    private DeviceContainerRules() {}

    /** An original {@code mergeItemStack} range, half open like the original. */
    public record Range(int start, int end) {
        public boolean contains(int index) { return index >= start && index < end; }
    }

    // ------------------------------------------------------------------ magic box
    // Oracle ContainerMagicBox:16-77. A linked box owns more slots than the
    // three rows the container binds, and the original quick-move boundary
    // uses the WHOLE linked size, not the twenty-seven bound slots.
    public static final int MAGIC_BOX_ROWS = 3;
    public static final int MAGIC_BOX_COLUMNS = 9;
    public static final int MAGIC_BOX_SLOTS = MAGIC_BOX_ROWS * MAGIC_BOX_COLUMNS;
    public static final int MAGIC_BOX_PLAYER_Y = 103;
    public static final int MAGIC_BOX_HOTBAR_Y = 161;

    /** Oracle ContainerMagicBox:20 - the linked size divided by nine. */
    public static int magicBoxRows(int inventorySize) { return inventorySize / 9; }

    /** Oracle ContainerMagicBox:44 - the quick-move boundary of a linked box. */
    public static int magicBoxQuickMoveBoundary(int inventorySize) { return (inventorySize / 9) * 9; }

    public static int magicBoxSlotX(int column) { return 8 + column * 18; }
    public static int magicBoxSlotY(int row) { return 18 + row * 18; }
    public static int magicBoxPlayerX(int column) { return 8 + column * 18; }
    public static int magicBoxPlayerY(int row) { return MAGIC_BOX_PLAYER_Y + row * 18; }
    public static int magicBoxHotbarY() { return MAGIC_BOX_HOTBAR_Y; }

    // --------------------------------------------------------------- hand mirror
    // Oracle ContainerHandMirror, InventoryHandMirror.
    public static final int HAND_MIRROR_INPUT_SLOTS = 1;
    public static final int HAND_MIRROR_STACK_LIMIT = 64;
    public static final int HAND_MIRROR_INPUT_X = 80;
    public static final int HAND_MIRROR_INPUT_Y = 24;
    public static final int HAND_MIRROR_PLAYER_Y = 84;
    public static final int HAND_MIRROR_HOTBAR_Y = 142;

    /** The original input slot restricts nothing; only the quick move does. */
    public static boolean handMirrorSlotAcceptsAnyItem() { return true; }

    /** Oracle ContainerHandMirror:73 - a hand mirror never travels into the slot. */
    public static boolean handMirrorQuickMoveAllowed(boolean sourceIsMirror) { return !sourceIsMirror; }

    /** Oracle ContainerHandMirror:68 - the transport needs a linked mirror. */
    public static boolean handMirrorCanTransport(boolean mirrorLinked, boolean serverSide) { return serverSide && mirrorLinked; }

    /** Oracle ContainerHandMirror:65 - an equal stack opens the player inventory. */
    public static boolean handMirrorShowsLinkedInventory(boolean slotEmpty, boolean slotEqualsMirror) { return !slotEmpty && slotEqualsMirror; }

    // --------------------------------------------------------------- focus pouch
    // Oracle ContainerFocusPouch, InventoryFocusPouch.
    public static final int FOCUS_POUCH_SLOTS = 18;
    public static final int FOCUS_POUCH_COLUMNS = 6;

    public static int focusPouchSlotX(int slot) { return 37 + slot % FOCUS_POUCH_COLUMNS * 18; }
    public static int focusPouchSlotY(int slot) { return 51 + slot / FOCUS_POUCH_COLUMNS * 18; }
    public static int focusPouchPlayerY(int row) { return 151 + row * 18; }
    public static int focusPouchHotbarY() { return 209; }

    /** Oracle InventoryFocusPouch:63 - only a basic focus fits a focus pouch. */
    public static boolean focusPouchAccepts(boolean isBasicFocus) { return isBasicFocus; }

    // ------------------------------------------------------------- hover harness
    // Oracle ContainerHoverHarness, InventoryHoverHarness.
    public static final int HOVER_HARNESS_SLOTS = 1;

    public static int hoverHarnessSlotX() { return 80; }
    public static int hoverHarnessSlotY() { return 32; }
    public static int hoverHarnessPlayerY(int row) { return 84 + row * 18; }
    public static int hoverHarnessHotbarY() { return 142; }

    /** Oracle InventoryHoverHarness:70 - a filled jar that still carries energy. */
    public static boolean hoverHarnessAccepts(boolean filledJar, boolean carriesEnergy) { return filledJar && carriesEnergy; }

    // --------------------------------------------------------------- ghost slots
    // Oracle SlotGhost, SlotGhostFluid, ContainerGhostSlots.
    public static final int GHOST_SLOT_LIMIT = 256;
    public static final int GHOST_FLUID_SLOT_LIMIT = 1;
    public static final int GHOST_QUICK_MOVE_STEP = 16;

    public static int ghostSlotLimit(boolean fluid) { return fluid ? GHOST_FLUID_SLOT_LIMIT : GHOST_SLOT_LIMIT; }

    /** Oracle SlotGhostFluid:30 - a fluid container, and only a fluid container. */
    public static boolean ghostSlotAcceptsFluidContainer(boolean isFluidContainer) { return isFluidContainer; }

    /** Oracle ContainerGhostSlots:64 - a held stack fills an empty ghost slot. */
    public static int ghostFillCount(int limit, int heldCount, boolean rightClick) { return Math.min(limit, rightClick ? 1 : heldCount); }

    /** Oracle ContainerGhostSlots:73 - an empty cursor adds or removes one. */
    public static int ghostAdjustCount(int count, int limit, boolean rightClick) { return rightClick ? Math.min(limit, count + 1) : count - 1; }

    /**
     * Oracle ContainerGhostSlots:84-92 - the ghost grows by the held amount,
     * bounded by the slot limit and by the held stack maximum. The original
     * mixes the held maximum with the slot count; that quirk is preserved.
     */
    public static int ghostMergeCount(int count, int heldCount, int heldMax, int limit, boolean rightClick) {
        int added = rightClick ? 1 : heldCount;
        if (added > limit - count) added = limit - count;
        if (added > heldMax - count) added = heldMax - count;
        // The original mixes the held maximum with the slot count, so the
        // preview can also shrink. The result never goes below zero.
        return Math.max(0, count + added);
    }

    /** Oracle ContainerGhostSlots:47-56 - a quick move clears or adds sixteen. */
    public static int ghostQuickMoveCount(int count, int limit, boolean rightClick) { return rightClick ? Math.min(limit, count + GHOST_QUICK_MOVE_STEP) : 0; }

    /** Oracle ContainerGhostSlots:110 - a throw removes one or the whole ghost. */
    public static int ghostThrowCount(int count, boolean wholeStack) { return wholeStack ? 0 : Math.max(0, count - 1); }

    /**
     * The complete original ghost click, reduced to the resulting ghost count.
     * Zero means the ghost slot is empty again.
     */
    public static int ghostClickCount(int slotCount, int heldCount, int heldMax, int limit, boolean sameItem, boolean rightClick) {
        if (slotCount <= 0) return heldCount <= 0 ? 0 : ghostFillCount(limit, heldCount, rightClick);
        if (heldCount <= 0) return Math.max(0, ghostAdjustCount(slotCount, limit, rightClick));
        if (sameItem) return ghostMergeCount(slotCount, heldCount, heldMax, limit, rightClick);
        // Oracle ContainerGhostSlots:93-95 - a different item replaces the ghost
        // with the whole held stack, unclamped by the slot limit.
        return heldCount;
    }

    // -------------------------------------------------------- limited-by rules
    /** Oracle SlotLimitedByClass:29 - an assignable item class. */
    public static boolean limitedByClass(Class<?> accepted, Class<?> actual) { return accepted != null && actual != null && accepted.isAssignableFrom(actual); }

    /** Oracle SlotLimitedByWand:29 - a casting wand, never a staff. */
    public static boolean limitedByWand(boolean isWand, boolean isStaff) { return isWand && !isStaff; }

    /** Oracle SlotLimitedHasAspects:22 - an item that carries aspects. */
    public static boolean limitedHasAspects(int aspectCount) { return aspectCount > 0; }

    /** Oracle SlotLimitedByItemstack:22 - an item stack match. */
    public static boolean limitedByItemStack(boolean matches) { return matches; }

    // ------------------------------------------------------------ research table
    // Oracle ContainerResearchTable.
    public static final int RESEARCH_TABLE_SLOTS = 2;

    public static int researchTableScribeX() { return 14; }
    public static int researchTableScribeY() { return 10; }
    public static int researchTableNotesX() { return 70; }
    public static int researchTableNotesY() { return 10; }
    public static int researchTablePlayerY(int row) { return 175 + row * 18; }
    public static int researchTableHotbarY() { return 233; }

    public static boolean researchTableScribeSlot(boolean isScribeTool) { return isScribeTool; }
    public static boolean researchTableNotesSlot(boolean isResearchNotes) { return isResearchNotes; }

    // -------------------------------------------------------- quick move routing
    // Oracle ContainerAlchemyFurnace:87-104.
    public static List<Range> alchemyFurnaceQuickMove(int slot, boolean fuel, boolean hasAspects) {
        if (slot == 0 || slot == 1) return List.of(new Range(2, 38));
        if (fuel) return List.of(new Range(1, 2), new Range(0, 1));
        if (hasAspects) return List.of(new Range(0, 1));
        if (slot >= 2 && slot < 29) return List.of(new Range(29, 38));
        return List.of(new Range(2, 29));
    }

    // Oracle ContainerDeconstructionTable:82-96.
    public static List<Range> deconstructionQuickMove(int slot, boolean hasAspects) {
        if (slot == 0) return List.of(new Range(1, 37));
        if (hasAspects) return List.of(new Range(0, 1));
        if (slot >= 1 && slot < 28) return List.of(new Range(28, 37));
        return List.of(new Range(1, 28));
    }

    // Oracle ContainerFocalManipulator:60-70.
    public static List<Range> focalManipulatorQuickMove(int slot, boolean isFocus) {
        if (slot == 0) return List.of(new Range(1, 37));
        if (isFocus) return List.of(new Range(0, 1));
        if (slot >= 1 && slot < 28) return List.of(new Range(28, 37));
        return List.of(new Range(1, 28));
    }

    // Oracle ContainerResearchTable:72-75.
    public static List<Range> researchTableQuickMove(int slot) { return slot < RESEARCH_TABLE_SLOTS ? List.of(new Range(2, 38)) : List.of(new Range(0, 2)); }

    // Oracle ContainerSpa:44-47.
    public static List<Range> spaQuickMove(int slot) { return slot == 0 ? List.of(new Range(1, 37)) : List.of(new Range(0, 1)); }

    // Oracle ContainerThaumatorium:96-112.
    public static List<Range> thaumatoriumQuickMove(int slot) { return slot == 0 ? List.of(new Range(1, 37)) : List.of(new Range(0, 1)); }

    // Oracle ContainerArcaneBore:44-51.
    public static List<Range> arcaneBoreQuickMove(int slot, boolean isExcavationFocus, boolean isPickaxe) {
        if (slot <= 1) return List.of(new Range(2, 38));
        if (isExcavationFocus) return List.of(new Range(0, 1));
        if (isPickaxe) return List.of(new Range(1, 2));
        return List.of();
    }

    // Oracle ContainerArcaneWorkbench:96-118.
    public static List<Range> arcaneWorkbenchQuickMove(int slot, boolean isWand) {
        if (slot == 0) return List.of(new Range(11, 47));
        if (slot >= 11 && slot < 38) return isWand ? List.of(new Range(1, 2)) : List.of(new Range(38, 47));
        if (slot >= 38 && slot < 47) return isWand ? List.of(new Range(1, 2)) : List.of(new Range(11, 38));
        return List.of(new Range(11, 47));
    }

    // Oracle ContainerFocusPouch:64-70.
    public static List<Range> focusPouchQuickMove(int slot) { return slot < FOCUS_POUCH_SLOTS ? List.of(new Range(18, 54)) : List.of(new Range(0, 18)); }

    // Oracle ContainerHoverHarness:60-66.
    public static List<Range> hoverHarnessQuickMove(int slot) { return slot == 0 ? List.of(new Range(1, 37)) : List.of(new Range(0, 1)); }

    // Oracle ContainerHandMirror:73-77.
    public static List<Range> handMirrorQuickMove(int slot) { return slot == 0 ? List.of(new Range(1, 37)) : List.of(new Range(0, 1)); }

    // Oracle ContainerMagicBox:44-52.
    public static List<Range> magicBoxQuickMove(int slot, int inventorySize) {
        int boundary = magicBoxQuickMoveBoundary(inventorySize);
        return slot < boundary ? List.of(new Range(boundary, MAGIC_BOX_SLOTS + 36)) : List.of(new Range(0, boundary));
    }
}
