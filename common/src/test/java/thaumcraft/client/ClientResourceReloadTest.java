package thaumcraft.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.network.RunicChargePayload;

class ClientResourceReloadTest {
    @AfterEach void disconnect() { ThaumcraftRunicClient.reset(); ThaumcraftWarpClient.reset(); }

    @Test void reloadingMeshesKeepsTheLastServerShieldSnapshotUntilDisconnect() throws Exception {
        var receive = ThaumcraftRunicClient.class.getDeclaredMethod("receive", RunicChargePayload.class);
        receive.setAccessible(true);
        receive.invoke(null, new RunicChargePayload(42, (short) 7, (short) 10));
        ThaumcraftRunicClient.reloadResources();
        assertEquals(42, field(ThaumcraftRunicClient.class, "source").getInt(null));
        assertEquals(7, field(ThaumcraftRunicClient.class, "charge").getInt(null));
        assertEquals(10, field(ThaumcraftRunicClient.class, "maximum").getInt(null));
        ThaumcraftRunicClient.reset();
        assertEquals(-1, field(ThaumcraftRunicClient.class, "source").getInt(null));
        assertEquals(0, field(ThaumcraftRunicClient.class, "charge").getInt(null));
    }

    @Test void reloadingShadersDoesNotCancelTheServerFogEvent() throws Exception {
        field(ThaumcraftWarpClient.class, "fogDuration").setInt(null, 2400);
        field(ThaumcraftWarpClient.class, "fogDensity").setFloat(null, .08F);
        field(ThaumcraftWarpClient.class, "vignette").setInt(null, 100);
        ThaumcraftWarpClient.reloadResources();
        assertEquals(2400, field(ThaumcraftWarpClient.class, "fogDuration").getInt(null));
        assertEquals(.08F, field(ThaumcraftWarpClient.class, "fogDensity").getFloat(null));
        assertEquals(100, field(ThaumcraftWarpClient.class, "vignette").getInt(null));
        ThaumcraftWarpClient.reset();
        assertEquals(0, field(ThaumcraftWarpClient.class, "fogDuration").getInt(null));
        assertEquals(0F, field(ThaumcraftWarpClient.class, "fogDensity").getFloat(null));
    }

    private static Field field(Class<?> type, String name) throws Exception {
        var field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
}
