package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.AspectRegistryEvent;

import java.util.List;

/** The aspect registration surface of the public API. */
class AspectApiTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** The object tag registry is global; the test leaves it as it found it. */
    @AfterEach void clearTestTags() {
        ThaumcraftApi.objectTags.remove(List.of(Items.STONE, 0));
        ThaumcraftApi.objectTags.remove(List.of(Items.DIRT, 0));
    }

    @Test void aRegisteredObjectTagIsReadableThroughTheHelper() {
        AspectList aspects = new AspectList().add(Aspect.EARTH, 3);
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.STONE), aspects);
        AspectList read = AspectHelper.getObjectAspects(new ItemStack(Items.STONE));
        assertEquals(3, read.getAmount(Aspect.EARTH));
        assertTrue(ThaumcraftApi.exists(Items.STONE, 0));
    }

    @Test void theRegistryEventRaisesItsProxyToEveryListener() {
        int[] calls = {0};
        AspectList registered = new AspectList();
        java.util.function.Consumer<AspectRegistryEvent> listener = event -> {
            calls[0]++;
            event.register.registerObjectTag(new ItemStack(Items.DIRT), registered.add(Aspect.FIRE, 2));
        };
        AspectRegistryEvent.addListener(listener);
        try {
            assertEquals(1, AspectRegistryEvent.listenerCount());
            AspectRegistryEvent.post();
            assertEquals(1, calls[0]);
            assertEquals(2, AspectHelper.getObjectAspects(new ItemStack(Items.DIRT)).getAmount(Aspect.FIRE));
        } finally {
            AspectRegistryEvent.removeListener(listener);
        }
        assertEquals(0, AspectRegistryEvent.listenerCount());
    }

    @Test void theAspectSourceHelperRefusesATileWithoutAWorld() {
        // The original helper searched any IAspectSource around the tile (oracle AspectSourceHelper:21-39);
        // a tile that is not in a world has no source at all.
        assertFalse(thaumcraft.api.aspects.AspectSourceHelper.drainEssentia(null, Aspect.AIR, net.minecraft.core.Direction.UP, 12));
        assertFalse(thaumcraft.api.aspects.AspectSourceHelper.findEssentia(null, Aspect.AIR, net.minecraft.core.Direction.UP, 12));
    }

    @Test void thePortKnowsTheOriginalFortyEightAspects() {
        assertEquals(48, Aspect.aspects.size());
        assertNotNull(Aspect.getAspect("aer"));
        assertNull(Aspect.getAspect("not_an_aspect"));
        assertTrue(Aspect.AIR.isPrimal());
        assertFalse(Aspect.VOID.isPrimal());
        assertEquals(Aspect.VOID, Aspect.getCombinationResult(Aspect.VOID.getComponents()[0], Aspect.VOID.getComponents()[1]));
    }
}
