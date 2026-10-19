package thaumcraft.common.lib.network;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class KnowledgeSyncPayloadTest {
    @Test
    public void queuedSnapshotIsIndependentOfLaterServerChanges() {
        CompoundTag source = new CompoundTag();
        source.putInt("warpPerm", 3);
        KnowledgeSyncPayload payload = new KnowledgeSyncPayload(source);
        source.putInt("warpPerm", 99);
        assertEquals(3, payload.state().getInt("warpPerm"));

        CompoundTag exposed = payload.state();
        exposed.putInt("warpPerm", 77);
        assertEquals(3, payload.state().getInt("warpPerm"));
    }
}
