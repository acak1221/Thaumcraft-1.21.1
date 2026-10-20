package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.golems.GolemCargoRules;

/** The pure cargo rules of the golem (original EntityGolemBase.getCarrySpace). */
class GolemCargoRulesTest {

    @BeforeAll static void bootstrapNative() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void anEmptyGolemMayTakeItsWholeCarryLimit() {
        assertEquals(10, GolemCargoRules.carrySpace(10, null));
        assertEquals(10, GolemCargoRules.carrySpace(10, ItemStack.EMPTY));
        assertEquals(64, GolemCargoRules.carrySpace(64, ItemStack.EMPTY));
    }

    @Test void aLoadedGolemKeepsTheSmallerOfTheLimitAndTheStackRoom() {
        assertEquals(4, GolemCargoRules.carrySpace(10, new ItemStack(Items.IRON_INGOT, 6)));
        assertEquals(0, GolemCargoRules.carrySpace(10, new ItemStack(Items.IRON_INGOT, 10)));
        assertEquals(60, GolemCargoRules.carrySpace(64, new ItemStack(Items.DIRT, 4)));
        assertEquals(0, GolemCargoRules.carrySpace(64, new ItemStack(Items.DIRT, 64)));
    }

    @Test void theStackRoomOfTheCarriedItemIsNeverExceeded() {
        // A snowball stack holds sixteen: a golem that carries four of them may only take twelve more,
        // even though its own carry limit would still allow sixty.
        assertEquals(12, GolemCargoRules.carrySpace(64, new ItemStack(Items.SNOWBALL, 4)));
        assertEquals(0, GolemCargoRules.carrySpace(64, new ItemStack(Items.SNOWBALL, 16)));
        // The stack size is the native 1.21.1 one: the bucket stacks to sixteen there, so a golem that
        // carries one may still take fifteen more of them.
        assertEquals(15, GolemCargoRules.carrySpace(64, new ItemStack(Items.BUCKET)));
    }
}
