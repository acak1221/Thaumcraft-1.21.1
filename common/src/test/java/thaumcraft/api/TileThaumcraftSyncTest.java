package thaumcraft.api;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** An add-on block entity on {@link TileThaumcraft}: its custom data reaches a client with the chunk and with each update. */
class TileThaumcraftSyncTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static class Counter extends TileThaumcraft {
        int amount;
        Counter() { super(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState()); }
        @Override public void readCustomNBT(CompoundTag tag) { amount = tag.getInt("Amount"); }
        @Override public void writeCustomNBT(CompoundTag tag) { tag.putInt("Amount", amount); }
    }

    @Test void theChunkTagCarriesTheCustomData() {
        Counter server = new Counter();
        server.amount = 7;
        CompoundTag chunk = server.getUpdateTag(null);
        assertEquals(7, chunk.getInt("Amount"), "the chunk tag is the custom data (1.7.10 sent the description packet with the chunk)");
        Counter client = new Counter();
        client.readCustomNBT(chunk);
        assertEquals(7, client.amount);
    }

    @Test void anEmptiedContainerStillReachesTheClient() {
        Counter server = new Counter() {
            @Override public void writeCustomNBT(CompoundTag tag) { if (amount > 0) tag.putInt("Amount", amount); }
        };
        CompoundTag emptied = server.getUpdateTag(null);
        // The 1.21.1 client drops an update whose tag is empty; the old contents would stay on screen.
        assertFalse(emptied.isEmpty(), "the update of empty custom data is not empty");
        assertTrue(emptied.getBoolean(TileThaumcraft.UPDATE_KEY));
        Counter client = new Counter();
        client.amount = 3;
        client.readCustomNBT(emptied);
        assertEquals(0, client.amount, "the client reads the emptied contents");
    }
}
