package thaumcraft.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumcraft.common.testing.InventoryScanRuntimeChecks;

/**
 * The built-in inventory scanning (setting {@code inventory_scanning}) through a real Fabric
 * connection. One batch per case: every case owns its connected actor and the server-wide session map.
 */
public final class InventoryScanGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "inventory_scan_00", timeoutTicks = 200)
    public void twentyFiveNativeTicks(GameTestHelper test) { InventoryScanRuntimeChecks.twentyFiveNativeTicks(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "inventory_scan_01", timeoutTicks = 200)
    public void serverCursorRequired(GameTestHelper test) { InventoryScanRuntimeChecks.serverCursorRequired(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "inventory_scan_02", timeoutTicks = 200)
    public void invalidAddressesAndResultSlots(GameTestHelper test) { InventoryScanRuntimeChecks.invalidAddressesAndResultSlots(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "inventory_scan_03", timeoutTicks = 200)
    public void permissionAndSlotIdentity(GameTestHelper test) { InventoryScanRuntimeChecks.permissionAndSlotIdentity(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "inventory_scan_04", timeoutTicks = 200)
    public void menuIdentityAba(GameTestHelper test) { InventoryScanRuntimeChecks.menuIdentityAba(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "inventory_scan_05", timeoutTicks = 200)
    public void itemIdentityComponentsAndCount(GameTestHelper test) { InventoryScanRuntimeChecks.itemIdentityComponentsAndCount(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "inventory_scan_06", timeoutTicks = 200)
    public void cursorReplacementCancels(GameTestHelper test) { InventoryScanRuntimeChecks.cursorReplacementCancels(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "inventory_scan_07", timeoutTicks = 200)
    public void missingParentIsAtomic(GameTestHelper test) { InventoryScanRuntimeChecks.missingParentIsAtomic(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "inventory_scan_08", timeoutTicks = 200)
    public void repeatAndOldRequestDoNotAward(GameTestHelper test) { InventoryScanRuntimeChecks.repeatAndOldRequestDoNotAward(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "inventory_scan_09", timeoutTicks = 200)
    public void selfScanAndNoContainerSpoof(GameTestHelper test) { InventoryScanRuntimeChecks.selfScanAndNoContainerSpoof(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "inventory_scan_10", timeoutTicks = 200)
    public void cancelAndHeartbeatTimeout(GameTestHelper test) { InventoryScanRuntimeChecks.cancelAndHeartbeatTimeout(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "inventory_scan_11", timeoutTicks = 200)
    public void retiringPermissionCallback(GameTestHelper test) { InventoryScanRuntimeChecks.retiringPermissionCallback(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "inventory_scan_12", timeoutTicks = 200)
    public void disconnectAndReconnect(GameTestHelper test) { InventoryScanRuntimeChecks.disconnectAndReconnect(test); }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "inventory_scan_13", timeoutTicks = 200)
    public void settingOffAdmitsNothing(GameTestHelper test) { InventoryScanRuntimeChecks.settingOffAdmitsNothing(test); }
}
