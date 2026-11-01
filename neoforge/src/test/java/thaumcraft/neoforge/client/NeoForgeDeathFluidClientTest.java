package thaumcraft.neoforge.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.client.extensions.common.ClientExtensionsManager;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.fluid.DeathFluidAppearance;

/** Real NeoForge registration/lookup without a GL context; fixture types never replace vanilla extensions. */
class NeoForgeDeathFluidClientTest {
    private final List<FluidType> fixtures = new ArrayList<>();

    @AfterEach void removeOnlyFixtureRegistrations() throws ReflectiveOperationException {
        var field = ClientExtensionsManager.class.getDeclaredField("FLUID_TYPE_EXTENSIONS");
        field.setAccessible(true);
        var registrations = (Map<?, ?>) field.get(null);
        fixtures.forEach(registrations::remove);
    }

    @Test void sharedSourceAndFlowingTypeRegistersOnceWithOriginalSpritesAndTint() throws ReflectiveOperationException {
        var type = type();
        var event = event();
        assertFalse(event.isFluidTypeRegistered(type));
        assertDoesNotThrow(() -> NeoForgeDeathFluidClient.register(event, type, type),
                "the common death-fluid base shares one type, which NeoForge must not register twice");
        assertTrue(event.isFluidTypeRegistered(type));
        var extension = IClientFluidTypeExtensions.of(type);
        assertNotSame(IClientFluidTypeExtensions.DEFAULT, extension);
        assertEquals(DeathFluidAppearance.TEXTURE, extension.getStillTexture());
        assertEquals(DeathFluidAppearance.TEXTURE, extension.getFlowingTexture());
        assertEquals(DeathFluidAppearance.TEXTURE, extension.getStillTexture(null, null, null));
        assertEquals(DeathFluidAppearance.TEXTURE, extension.getFlowingTexture(null, null, null));
        assertEquals(0xFFFFFFFF, extension.getTintColor());
        assertEquals(8, type.getLightLevel());
    }

    @Test void distinctSourceAndFlowingTypesBothGetTheSameNativeExtension() throws ReflectiveOperationException {
        var source = type();
        var flowing = type();
        var event = event();
        NeoForgeDeathFluidClient.register(event, source, flowing);
        assertTrue(event.isFluidTypeRegistered(source));
        assertTrue(event.isFluidTypeRegistered(flowing));
        assertSame(IClientFluidTypeExtensions.of(source), IClientFluidTypeExtensions.of(flowing));
        assertEquals(DeathFluidAppearance.TEXTURE, IClientFluidTypeExtensions.of(flowing).getFlowingTexture());
    }

    private FluidType type() {
        var type = new FluidType(FluidType.Properties.create().descriptionId("review.death_fluid_fixture").lightLevel(8));
        fixtures.add(type);
        return type;
    }

    private static RegisterClientExtensionsEvent event() throws ReflectiveOperationException {
        var constructor = RegisterClientExtensionsEvent.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
