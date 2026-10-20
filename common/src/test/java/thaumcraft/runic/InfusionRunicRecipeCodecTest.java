package thaumcraft.runic;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import thaumcraft.common.crafting.InfusionRunicAugmentRecipe;
import static org.junit.jupiter.api.Assertions.*;

class InfusionRunicRecipeCodecTest {
    @Test void independentlyConstructedAndJsonDecodedStatelessRecipesEncodeOnTheNativeWire() {
        var first=new InfusionRunicAugmentRecipe();
        var json=InfusionRunicAugmentRecipe.CODEC.codec().encodeStart(JsonOps.INSTANCE,first).getOrThrow();
        assertTrue(json.isJsonObject());
        var decoded=InfusionRunicAugmentRecipe.CODEC.codec().parse(JsonOps.INSTANCE,json).getOrThrow();
        assertNotSame(first,decoded);
        var serializer=new InfusionRunicAugmentRecipe.Serializer();
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            for(var recipe:new InfusionRunicAugmentRecipe[]{first,decoded,new InfusionRunicAugmentRecipe()}) {
                serializer.streamCodec().encode(buffer,recipe);
                assertEquals(0,buffer.readableBytes());
                var received=serializer.streamCodec().decode(buffer);
                assertNotSame(recipe,received);
                assertEquals("RUNICAUGMENTATION",received.research());
            }
        } finally { buffer.release(); }
    }
}
