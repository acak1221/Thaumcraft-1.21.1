package thaumcraft.crafting;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import thaumcraft.common.crafting.ArcaneVis;

class ComprehensiveArcaneVisReviewTest {
    @Test void unrepresentableProgrammaticCostCannotWrapIntoFourCentivis() {
        // Public programmatic recipes are not constrained by the JSON recipe codec.
        // 42,949,673 * 100 == 2^32 + 4 in integer arithmetic.
        assertThrows(IllegalArgumentException.class,
                () -> ArcaneVis.price(Map.of("aer", 42_949_673), ItemStack.EMPTY));
    }

    @Test void unrepresentableMaximumCostIsRefusedRatherThanReturningANegativePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> ArcaneVis.price(Map.of("aer", Integer.MAX_VALUE), ItemStack.EMPTY));
    }

    @Test void ordinaryWholeVisCostsKeepTheirExactCentivisSemantics() {
        assertEquals(Map.of("aer", 100, "aqua", 2500),
                ArcaneVis.price(Map.of("aer", 1, "aqua", 25), ItemStack.EMPTY));
    }
}
