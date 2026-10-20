package thaumcraft.runic;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.network.RunicChargePayload;
import thaumcraft.common.lib.network.RunicHitPayload;
import static org.junit.jupiter.api.Assertions.*;

class RunicPayloadTest {
    @Test void chargeAndHitKeepTheOriginalEightByteSignedWireFormats() {
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            var charge=new RunicChargePayload(123,(short)-128,Short.MAX_VALUE);
            RunicChargePayload.STREAM_CODEC.encode(buffer,charge);
            assertEquals(8,buffer.readableBytes()); assertEquals(charge,RunicChargePayload.STREAM_CODEC.decode(buffer));
            assertFalse(buffer.isReadable());
            for(int target:new int[]{-3,-2,-1,0,Integer.MAX_VALUE}) {
                buffer.clear(); var hit=new RunicHitPayload(45,target); RunicHitPayload.STREAM_CODEC.encode(buffer,hit);
                assertEquals(8,buffer.readableBytes()); assertEquals(hit,RunicHitPayload.STREAM_CODEC.decode(buffer)); assertFalse(buffer.isReadable());
            }
        } finally { buffer.release(); }
    }
    @Test void aTruncatedChargeCannotBeDecodedIntoPartialClientState() {
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            buffer.writeInt(1); buffer.writeShort(2);
            assertThrows(IndexOutOfBoundsException.class,()->RunicChargePayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}
