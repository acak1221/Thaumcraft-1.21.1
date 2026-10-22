package thaumcraft.client.renderers.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.world.entity.item.ItemEntity;
import org.junit.jupiter.api.Test;
import thaumcraft.assets.RegistryInventory;

/**
 * The guard of the entity renderer coverage.
 *
 * <p>Minecraft 1.21.1 looks the renderer of an entity up by entity type and has no
 * fallback: {@code EntityRenderDispatcher.getRenderer} answers
 * {@code this.renderers.get(entity.getType())}, so an entity type without a registered
 * renderer crashes the client with a {@code NullPointerException} as soon as the entity is
 * drawn. The original 1.7.10 registry had no such lookup, so the original
 * {@code ClientProxy.setupEntityRenderers} registered a renderer for every entity the mod
 * added (oracle {@code thaumcraft/client/ClientProxy.java:464-517}).</p>
 *
 * <p>The tests below compare the entity types the port really registers, read from the
 * registration classes by reflection, with the renderers the port registers. They are the
 * guard that keeps this crash class out of the port.</p>
 */
class EntityRendererCoverageTest {
    /** The entity types the port registers, read from the registration classes. */
    private static Set<String> portEntityTypes() {
        return new TreeSet<>(RegistryInventory.entityIds());
    }

    @Test void theSevenEntityTypesWithoutARendererHaveOneNow() {
        // The client smoke of the port found these seven entity types of the port without a
        // registered renderer; every one of them is drawn by the client, so every one of
        // them crashed the client with a NullPointerException.
        for (String type : List.of("bottle_taint", "golem_orb", "primal_arrow", "taint_crawler",
                "taint_seed", "taint_seed_prime", "taintacle_giant")) {
            assertTrue(portEntityTypes().contains(type), "the port registers " + type);
            var binding = ThaumcraftEntityRenderers.binding(type);
            assertNotNull(binding, "the renderer of " + type);
        }
    }

    @Test void everyEntityTypeOfThePortHasAClientRenderer() {
        Set<String> missing = ThaumcraftEntityRenderers.entityTypesWithoutRenderer(portEntityTypes());
        assertTrue(missing.isEmpty(), "entity types without a client renderer: " + missing);
    }

    @Test void noRendererIsRegisteredForAnUnknownEntityType() {
        Set<String> unknown = new TreeSet<>(ThaumcraftEntityRenderers.renderedEntityTypeIds());
        unknown.removeAll(portEntityTypes());
        assertTrue(unknown.isEmpty(), "renderers for entity types the port does not register: " + unknown);
    }

    @Test void theEntityTypeListOfTheRenderersIsTheOneOfTheRegistrationClasses() {
        assertEquals(portEntityTypes(), ThaumcraftEntityRenderers.entityTypeIds(),
                "the entity type list of the renderer table must mirror the registration classes");
    }

    @Test void theItemEntityRenderersAcceptTheCopyOfAnItemEntity() {
        // 1.21.1 renders the item pickup animation with a copy of the picked entity, and
        // ItemEntity.copy() keeps the entity type but not the entity class, so the copy of
        // a special item is a plain item entity that still carries the thaumcraft type.
        // A renderer typed to the special item class would throw a ClassCastException.
        for (String type : List.of("special_item", "permanent_item", "follow_item", "inventory_item", "item_grate")) {
            var binding = ThaumcraftEntityRenderers.binding(type);
            assertNotNull(binding, "the renderer of " + type);
            assertTrue(ItemEntity.class.isAssignableFrom(binding.entityClass()),
                    "the renderer of " + type + " must accept any item entity, not " + binding.entityClass());
        }
    }
}
