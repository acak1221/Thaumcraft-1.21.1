package thaumcraft.baubles;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.Map;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.baubles.VisAmuletData;
import static org.junit.jupiter.api.Assertions.*;

class VisAmuletDataTest {
    @Test void eachNativeCapacityHasItsOwnBoundedPersistentAndWireCodec() {
        for(int capacity:new int[]{2500,25000}) {
            var data=new VisAmuletData(Map.of("aer",capacity,"terra",0));var codec=VisAmuletData.codec(capacity);
            assertEquals(data,codec.parse(JsonOps.INSTANCE,codec.encodeStart(JsonOps.INSTANCE,data).getOrThrow()).getOrThrow());
            var buffer=Unpooled.buffer();try {VisAmuletData.streamCodec(capacity).encode(buffer,data);assertEquals(data,VisAmuletData.streamCodec(capacity).decode(buffer));assertFalse(buffer.isReadable());}finally{buffer.release();}
            for(String json:new String[]{"{\"aer\":-1}","{\"aer\":"+(capacity+1)+"}","{\"praecantatio\":1}"})assertTrue(codec.parse(JsonOps.INSTANCE,JsonParser.parseString(json)).error().isPresent());
        }
        assertTrue(VisAmuletData.codec(2500).encodeStart(JsonOps.INSTANCE,new VisAmuletData(Map.of("aer",2501))).error().isPresent());
    }
    @Test void componentSnapshotsCannotBeMutatedAndTransfersRemainRawCentivis() {
        var data=VisAmuletData.EMPTY.withAmount("aer",5,2500);
        assertEquals(5,data.amount("aer"));assertEquals(0,VisAmuletData.EMPTY.amount("aer"));
        assertThrows(UnsupportedOperationException.class,()->data.vis().put("aer",100));
        assertThrows(IllegalArgumentException.class,()->data.withAmount("aer",2501,2500));
        assertThrows(IllegalArgumentException.class,()->data.withAmount("praecantatio",1,2500));
    }
}
