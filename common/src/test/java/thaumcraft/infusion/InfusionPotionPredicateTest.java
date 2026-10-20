package thaumcraft.infusion;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import thaumcraft.common.crafting.InfusionComponentPredicate.Declaration;
import static org.junit.jupiter.api.Assertions.*;

class InfusionPotionPredicateTest {
    @Test void originalFourPotionRequirementsRoundTripWithNoFakeWearField() {
        for(String potion:new String[]{"strong_strength","strong_swiftness","long_regeneration","strong_harming"}) {
            var json=JsonParser.parseString("{\"component\":3,\"potion\":\"minecraft:"+potion+"\"}");
            var declaration=Declaration.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
            assertEquals(java.util.Optional.of("minecraft:"+potion),declaration.potion());assertTrue(declaration.optionalDamage().isEmpty());
            assertEquals(declaration,Declaration.CODEC.parse(JsonOps.INSTANCE,Declaration.CODEC.encodeStart(JsonOps.INSTANCE,declaration).getOrThrow()).getOrThrow());
        }
        for(String json:new String[]{"{\"component\":3,\"potion\":\"minecraft:water\"}","{\"component\":3,\"potion\":\"minecraft:strong_strength\",\"empty_jar\":true}"})
            assertTrue(Declaration.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(json)).error().isPresent());
    }
}
