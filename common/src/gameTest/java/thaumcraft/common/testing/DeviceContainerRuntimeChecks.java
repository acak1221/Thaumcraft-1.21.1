package thaumcraft.common.testing;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftDeviceMenus;
import thaumcraft.common.container.ContainerGhostSlots;
import thaumcraft.common.container.ContainerHandMirror;
import thaumcraft.common.container.ContainerMagicBox;
import thaumcraft.common.lib.crafting.DeviceContainerRules;

/**
 * The original TC4 device containers inside a real dedicated server: the magic box that binds
 * three rows whatever the size of the chain is, the hand mirror with its single input slot and
 * the ghost slot preview with its original limits and its original quick move step.
 */
public final class DeviceContainerRuntimeChecks {
    private DeviceContainerRuntimeChecks() {}

    /** The original magic box and hand mirror containers bind their original slots. */
    public static void magicBoxAndHandMirrorBindTheOriginalSlots(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            lane.floor(3);

            var box = new SimpleContainer(DeviceContainerRules.MAGIC_BOX_SLOTS);
            var magic = new ContainerMagicBox(1, player.getInventory(), box, lane.origin, DeviceContainerRules.MAGIC_BOX_SLOTS);
            test.assertTrue(magic.slots.size() == DeviceContainerRules.MAGIC_BOX_SLOTS + 36,
                    "the magic box binds the original three rows and the player inventory: " + magic.slots.size());
            test.assertTrue(magic.numRows() == DeviceContainerRules.MAGIC_BOX_ROWS,
                    "the magic box always binds the original three rows: " + magic.numRows());
            test.assertTrue(DeviceContainerRules.magicBoxQuickMoveBoundary(DeviceContainerRules.MAGIC_BOX_SLOTS)
                            == DeviceContainerRules.MAGIC_BOX_SLOTS,
                    "the quick move boundary of a lonely box is its own twenty seven slots");
            test.assertTrue(DeviceContainerRules.magicBoxQuickMoveBoundary(thaumcraft.common.lib.world.EldritchBlockRules.magicBoxSize(1)) == 54,
                    "the quick move boundary of a linked box uses the whole original chain size: "
                            + DeviceContainerRules.magicBoxQuickMoveBoundary(thaumcraft.common.lib.world.EldritchBlockRules.magicBoxSize(1)));
            test.assertTrue(magic.boxPos().equals(lane.origin) && magic.boxContents() == box,
                    "the magic box keeps its own position and its own content");
            magic.removed(player);

            var mirror = new ContainerHandMirror(2, player.getInventory(), net.minecraft.world.InteractionHand.MAIN_HAND);
            test.assertTrue(mirror.slots.size() == DeviceContainerRules.HAND_MIRROR_INPUT_SLOTS + 36,
                    "the hand mirror binds its original single input slot and the player inventory: " + mirror.slots.size());
            test.assertTrue(mirror.slots.get(0).mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT)),
                    "the input slot of the hand mirror accepts every item");
            test.assertTrue(DeviceContainerRules.handMirrorQuickMoveAllowed(false) && !DeviceContainerRules.handMirrorQuickMoveAllowed(true),
                    "the hand mirror never travels into its own input slot");
            test.assertTrue(DeviceContainerRules.handMirrorCanTransport(true, true) && !DeviceContainerRules.handMirrorCanTransport(false, true),
                    "the hand mirror only transports with a linked mirror block");
            mirror.removed(player);
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original ghost slot preview keeps its limits and its quick move step. */
    public static void ghostSlotsKeepTheOriginalLimits(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            lane.floor(3);

            var ghosts = new SimpleContainer(DeviceContainerRules.GHOST_SLOT_LIMIT);
            var preview = new ContainerGhostSlots(ThaumcraftDeviceMenus.GHOST_SLOTS.get(), 3, player.getInventory(),
                    DeviceContainerRules.GHOST_SLOT_LIMIT, ghosts);
            test.assertTrue(preview.slots.size() == DeviceContainerRules.GHOST_SLOT_LIMIT + 36,
                    "the ghost preview binds its original 256 ghost slots: " + preview.slots.size());
            var fluidGhosts = new SimpleContainer(DeviceContainerRules.GHOST_FLUID_SLOT_LIMIT);
            var fluidPreview = new ContainerGhostSlots(ThaumcraftDeviceMenus.GHOST_SLOTS.get(), 4, player.getInventory(),
                    DeviceContainerRules.GHOST_FLUID_SLOT_LIMIT, fluidGhosts, true);
            test.assertTrue(fluidPreview.slots.size() == DeviceContainerRules.GHOST_FLUID_SLOT_LIMIT + 36,
                    "the fluid preview binds its original single ghost slot: " + fluidPreview.slots.size());
            test.assertTrue(DeviceContainerRules.ghostSlotLimit(false) == 256 && DeviceContainerRules.ghostSlotLimit(true) == 1,
                    "the ghost slots keep the original limits of 256 and one");
            test.assertTrue(DeviceContainerRules.ghostQuickMoveCount(0, 256, false) == 0
                            && DeviceContainerRules.ghostQuickMoveCount(0, 256, true) == 16,
                    "the quick move of a ghost slot uses the original step of sixteen");
            test.assertTrue(DeviceContainerRules.ghostClickCount(0, 5, 64, 256, false, false) == 5
                            && DeviceContainerRules.ghostClickCount(0, 5, 64, 256, false, true) == 1,
                    "a held stack fills an empty ghost slot whole, a right click fills it with one");
            test.assertTrue(DeviceContainerRules.ghostClickCount(3, 0, 64, 256, false, true) == 4
                            && DeviceContainerRules.ghostClickCount(3, 0, 64, 256, false, false) == 2,
                    "an empty cursor adds or removes one from the ghost slot");
            test.assertTrue(DeviceContainerRules.ghostThrowCount(3, false) == 2 && DeviceContainerRules.ghostThrowCount(3, true) == 0,
                    "a throw removes one or the whole ghost slot");
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }
}
