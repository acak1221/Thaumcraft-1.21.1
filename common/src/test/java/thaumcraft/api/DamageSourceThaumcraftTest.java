package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.Bootstrap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.damagesource.DamageSourceThaumcraft;

/**
 * The four damage sources of the original TC4 API.
 *
 * <p>Oracle {@code thaumcraft/api/damagesource/DamageSourceThaumcraft.java:19-22} published
 * {@code taint}, {@code tentacle}, {@code swarm} and {@code dissolve}.</p>
 */
class DamageSourceThaumcraftTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void theFourOriginalSourcesArePublished() {
        List<ResourceKey<DamageType>> types = List.of(
                DamageSourceThaumcraft.TAINT, DamageSourceThaumcraft.TENTACLE,
                DamageSourceThaumcraft.SWARM, DamageSourceThaumcraft.DISSOLVE);
        assertEquals(List.of("thaumcraft:taint", "thaumcraft:tentacle", "thaumcraft:swarm", "thaumcraft:dissolve"),
                types.stream().map(type -> type.location().toString()).toList());
        for (ResourceKey<DamageType> type : types) {
            assertEquals(Registries.DAMAGE_TYPE, type.registryKey());
            assertTrue(type.location().getNamespace().equals("thaumcraft"), type.location() + " must stay namespaced");
        }
    }

    @Test void theSourceIdsDoNotCollideWithEachOther() {
        assertNotEquals(DamageSourceThaumcraft.TAINT, DamageSourceThaumcraft.DISSOLVE);
        assertNotEquals(DamageSourceThaumcraft.TENTACLE, DamageSourceThaumcraft.SWARM);
    }
}
