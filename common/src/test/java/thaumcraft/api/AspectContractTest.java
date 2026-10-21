package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * The aspect contract of the public API against the 4.2.3.5 oracle.
 *
 * <p>Oracle {@code thaumcraft/api/aspects/Aspect.java}: the constructor accepts any component array
 * (:74-84), {@code isPrimal()} answers "no components or a length that is not two" (:145),
 * {@code getLocalizedDescription()} answers the translation of {@code tc.aspect.<tag>} (:108) and the
 * four runtime setters exist (:116, :124, :140, :172).</p>
 *
 * <p>Oracle {@code thaumcraft/api/aspects/AspectList.java}: the class is {@code Serializable} (:21) and
 * the {@code AspectList(ItemStack)} constructor reads the object aspects of a stack (:25-36).</p>
 */
class AspectContractTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** The object tag registry is global; the test leaves it as it found it. */
    @AfterEach void clearTestTags() {
        ThaumcraftApi.objectTags.remove(List.of(Items.STONE, 0));
    }

    @Test void aCompoundAspectWithThreeParentsIsLegalAndPrimal() {
        // Oracle Aspect:74-84 accepts any component array and oracle Aspect:145 reports a
        // compound aspect that does not have exactly two parents as primal.
        Aspect three = new Aspect("api_contract_three", 0x123456, new Aspect[]{Aspect.AIR, Aspect.FIRE, Aspect.WATER});
        try {
            assertTrue(three.isPrimal());
            assertEquals(3, three.getComponents().length);
            Aspect two = new Aspect("api_contract_two", 0x123456, new Aspect[]{Aspect.AIR, Aspect.FIRE});
            try {
                assertFalse(two.isPrimal());
            } finally {
                Aspect.aspects.remove("api_contract_two");
            }
        } finally {
            Aspect.aspects.remove("api_contract_three");
        }
    }

    @Test void theAspectCarriesItsOriginalRuntimeSetters() {
        Aspect aspect = new Aspect("api_contract_setters", 0x123456, new Aspect[]{Aspect.AIR, Aspect.FIRE});
        try {
            aspect.setTag("api_contract_setters_renamed");
            aspect.setBlend(7);
            aspect.setChatcolor("b");
            aspect.setComponents(new Aspect[]{Aspect.WATER, Aspect.EARTH});
            assertEquals("api_contract_setters_renamed", aspect.getTag());
            assertEquals(7, aspect.getBlend());
            assertEquals("b", aspect.getChatcolor());
            assertEquals(Aspect.WATER, aspect.getComponents()[0]);
            assertEquals(Aspect.EARTH, aspect.getComponents()[1]);
        } finally {
            Aspect.aspects.remove("api_contract_setters");
        }
    }

    @Test void theAspectAnswersItsLocalisedDescription() {
        // Oracle Aspect:108 - the description is the translation of tc.aspect.<tag>.
        assertNotNull(Aspect.AIR.getLocalizedDescription());
        assertTrue(Aspect.AIR.getLocalizedDescription().contains("aer"),
                "the description of an aspect names its tag: " + Aspect.AIR.getLocalizedDescription());
    }

    @Test void theAspectListIsBuiltFromAnItemStack() {
        // Oracle AspectList:25-36 - the constructor reads the object aspects of the stack.
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.STONE), new AspectList().add(Aspect.EARTH, 3));
        AspectList list = new AspectList(new ItemStack(Items.STONE));
        assertEquals(1, list.size());
        assertEquals(3, list.getAmount(Aspect.EARTH));
    }

    @Test void theAspectListIsSerializable() throws Exception {
        // Oracle AspectList:21 - public class AspectList implements Serializable.
        assertTrue(java.io.Serializable.class.isAssignableFrom(AspectList.class));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(new AspectList());
        }
        assertTrue(bytes.size() > 0);
    }
}
