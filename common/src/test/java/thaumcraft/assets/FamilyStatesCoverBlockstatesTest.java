package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.lib.world.FamilyVariant;

/**
 * Minecraft 1.21.1 asks for a model of EVERY state a block owns, and a state without one is logged as
 * "Exception loading blockstate definition ... missing model for variant". The state set of a family
 * block is the value set of its variant property, so the blockstate file of each group must name
 * exactly those values. This ties the three together for the families that are split into hardness
 * groups, from the same group lists the blocks are built from.
 */
class FamilyStatesCoverBlockstatesTest {
    private static Set<String> keys(String blockId) throws Exception {
        JsonObject variants = ResourceTree.object("assets/thaumcraft/blockstates/" + blockId + ".json").getAsJsonObject("variants");
        return new TreeSet<>(variants.keySet());
    }

    private static Set<String> states(List<Integer> hosted) {
        IntegerProperty property = FamilyVariant.property(hosted);
        Set<String> states = new TreeSet<>();
        for (int value : property.getPossibleValues()) states.add("variant=" + value);
        return states;
    }

    private static void check(String blockId, List<Integer> hosted) throws Exception {
        assertEquals(states(hosted), keys(blockId), blockId + ": states of the block against the entries of its blockstate file");
    }

    @Test void theCosmeticSolidGroupsNameEveryStateTheyOwn() throws Exception {
        check("cosmetic_solid", EldritchBlockRules.cosmeticGroupVariants(0));
        check("cosmetic_solid_tough", EldritchBlockRules.cosmeticGroupVariants(1));
        check("cosmetic_solid_thaumium", EldritchBlockRules.cosmeticGroupVariants(2));
    }

    @Test void theAiryGroupsNameEveryStateTheyOwn() throws Exception {
        check("airy", EldritchBlockRules.airyGroupVariants(0));
        check("airy_node", EldritchBlockRules.airyGroupVariants(1));
    }
}
