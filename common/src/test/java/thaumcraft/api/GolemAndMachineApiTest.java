package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.golems.GolemApi;
import thaumcraft.api.golems.GolemMaterial;
import thaumcraft.api.golems.GolemView;
import thaumcraft.api.machines.IArcaneBore;
import thaumcraft.api.machines.IArcaneEar;
import thaumcraft.api.machines.IBrainJar;
import thaumcraft.api.machines.IDeconstructor;
import thaumcraft.common.entities.golems.EnumGolemType;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.tiles.TileArcaneBore;
import thaumcraft.common.tiles.TileDeconstructionTable;
import thaumcraft.common.tiles.TileJarBrain;
import thaumcraft.common.tiles.TileSensor;

/**
 * The plain-data half of {@code thaumcraft.api.golems} and {@code thaumcraft.api.machines}: the records and the enum,
 * the answers for null and the binding of the interfaces to the block entities. What a real golem and real block
 * entities answer, and that the writes change, sync and save them, is checked in a running world (GameTests of
 * {@code GolemMachineApiRuntimeChecks}).
 */
class GolemAndMachineApiTest {
    private static final ResourceKey<Level> NOWHERE = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "api_test_elsewhere"));

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** Original {@code EnumGolemType} order; the 1.7.10 Golem Connector's type names. */
    @Test void materialIdsAreTheOrdinalsOfTheOriginalGolemTypes() {
        assertEquals(EnumGolemType.values().length, GolemMaterial.values().length);
        for (EnumGolemType type : EnumGolemType.values()) {
            GolemMaterial material = GolemMaterial.byId(type.ordinal());
            assertEquals(type.name(), material.name(), "material " + type.ordinal());
            assertEquals(type.ordinal(), material.id());
        }
        assertEquals(List.of("Straw", "Wood", "Tallow", "Clay", "Flesh", "Stone", "Iron", "Thaumium"),
                Arrays.stream(GolemMaterial.values()).map(GolemMaterial::displayName).toList());
        assertNull(GolemMaterial.byId(-1));
        assertNull(GolemMaterial.byId(8));
        assertNull(GolemMaterial.byId(Integer.MAX_VALUE));
    }

    /** The core ids the view documents are the golem's own (original {@code ItemGolemCore} damage values). */
    @Test void coreAndLimitConstantsAreTheGolemsOwn() {
        assertEquals(GolemData.NO_CORE, GolemView.NO_CORE);
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), List.of(GolemData.FILL, GolemData.EMPTY_CORE, GolemData.GATHER,
                GolemData.HARVEST, GolemData.GUARD, GolemData.LIQUID, GolemData.ALCHEMY, GolemData.LUMBER, GolemData.USE,
                GolemData.BUTCHER, GolemData.SORTING, GolemData.FISHING));
        assertEquals(GolemData.MAX_MARKERS, GolemApi.MAX_MARKERS);
        assertEquals(GolemMarker.ANY, GolemView.Marker.ANY_COLOR);
        assertEquals(GolemMarker.LAST_COLOR, GolemView.Marker.LAST_COLOR);
    }

    @Test void aMarkerChecksItsComponents() {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(1, 2, 3);
        var marker = new GolemView.Marker(mutable, Level.NETHER, Direction.EAST, 15);
        mutable.set(9, 9, 9);
        assertEquals(new BlockPos(1, 2, 3), marker.pos(), "the position is an immutable copy");
        assertFalse(marker.pos() instanceof BlockPos.MutableBlockPos);
        assertTrue(marker.hasColor());
        var plain = new GolemView.Marker(BlockPos.ZERO, Level.OVERWORLD, Direction.UP);
        assertEquals(GolemView.Marker.ANY_COLOR, plain.color());
        assertFalse(plain.hasColor());
        assertEquals(plain, new GolemView.Marker(BlockPos.ZERO, Level.OVERWORLD, Direction.UP, -1), "a value record");
        assertEquals(NOWHERE, new GolemView.Marker(BlockPos.ZERO, NOWHERE, Direction.UP).dimension(),
                "a dimension that is not loaded is still a dimension key");

        assertThrows(IllegalArgumentException.class, () -> new GolemView.Marker(BlockPos.ZERO, Level.OVERWORLD, Direction.UP, 16));
        assertThrows(IllegalArgumentException.class, () -> new GolemView.Marker(BlockPos.ZERO, Level.OVERWORLD, Direction.UP, -2));
        assertThrows(NullPointerException.class, () -> new GolemView.Marker(null, Level.OVERWORLD, Direction.UP));
        assertThrows(NullPointerException.class, () -> new GolemView.Marker(BlockPos.ZERO, null, Direction.UP));
        assertThrows(NullPointerException.class, () -> new GolemView.Marker(BlockPos.ZERO, Level.OVERWORLD, null));
        @SuppressWarnings({"unchecked", "rawtypes"})
        ResourceKey<Level> biome = (ResourceKey) ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("plains"));
        assertThrows(IllegalArgumentException.class, () -> new GolemView.Marker(BlockPos.ZERO, biome, Direction.UP),
                "a key of another registry is no dimension");
    }

    @Test void aViewCopiesItsMarkersAndNamesItsMaterial() {
        List<GolemView.Marker> markers = new ArrayList<>(List.of(new GolemView.Marker(new BlockPos(4, 5, 6), Level.END, Direction.DOWN, 3)));
        var view = view(7, 0, markers);
        markers.clear();
        assertEquals(1, view.markers().size(), "the view keeps its own copy");
        assertThrows(UnsupportedOperationException.class, () -> view.markers().add(view.markers().getFirst()));
        assertSame(GolemMaterial.THAUMIUM, view.material());
        assertTrue(view.hasCore());
        assertNull(view(42, GolemView.NO_CORE, List.of()).material(), "an unknown saved material has no name");
        assertFalse(view(0, GolemView.NO_CORE, List.of()).hasCore());

        assertThrows(NullPointerException.class, () -> view(0, 0, Arrays.asList((GolemView.Marker) null)));
        assertThrows(NullPointerException.class, () -> new GolemView(UUID.randomUUID(), Level.OVERWORLD, 0, 0, null, 1, Vec3.ZERO,
                BlockPos.ZERO, Direction.UP, List.of()));
        BlockPos.MutableBlockPos home = new BlockPos.MutableBlockPos(1, 1, 1);
        var withMutableHome = new GolemView(UUID.randomUUID(), Level.OVERWORLD, 0, 0, "", 1, Vec3.ZERO, home, Direction.UP, List.of());
        home.set(2, 2, 2);
        assertEquals(new BlockPos(1, 1, 1), withMutableHome.home(), "the home is an immutable copy");
    }

    /** Null answers "no golem"; nothing throws for it. A real non-golem entity is checked in the GameTest. */
    @Test void nullIsNoGolem() {
        assertFalse(GolemApi.isGolem(null));
        assertTrue(GolemApi.view(null).isEmpty());
        assertFalse(GolemApi.setHome(null, BlockPos.ZERO, Direction.UP));
        assertFalse(GolemApi.setMarkers(null, List.of()));
    }

    /** The original block entities of the four 1.7.10 peripherals implement the interfaces; nothing else of the mod is asked. */
    @Test void theMachineInterfacesAreThoseOfTheOriginalPeripheralBlockEntities() {
        assertTrue(IArcaneEar.class.isAssignableFrom(TileSensor.class));
        assertTrue(IBrainJar.class.isAssignableFrom(TileJarBrain.class));
        assertTrue(IDeconstructor.class.isAssignableFrom(TileDeconstructionTable.class));
        assertTrue(IArcaneBore.class.isAssignableFrom(TileArcaneBore.class));
        assertEquals(0, IArcaneEar.MIN_NOTE);
        assertEquals(24, IArcaneEar.MAX_NOTE);
        // Exactly the questions of the original peripherals (tt_arcaneear, tt_braininajar, tt_deconstructor, tt_arcanebore).
        assertEquals(List.of("getNote", "setNote"), methods(IArcaneEar.class));
        assertEquals(List.of("getXp", "getXpMax"), methods(IBrainJar.class));
        assertEquals(List.of("getReward", "hasItem"), methods(IDeconstructor.class));
        assertEquals(List.of("getPickaxe", "getPickaxeFortune", "getRadius", "getSpeed", "hasFocus", "hasNativeClusters",
                "hasPickaxe", "hasSilkTouch", "isPickaxeBroken", "isPowered", "isWorking"), methods(IArcaneBore.class));
    }

    @Test void theGolemApiIsAStaticFacade() throws NoSuchMethodException {
        assertTrue(Modifier.isFinal(GolemApi.class.getModifiers()));
        for (Method method : GolemApi.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) assertTrue(Modifier.isStatic(method.getModifiers()), method.getName());
        }
        assertEquals(boolean.class, GolemApi.class.getMethod("setHome", net.minecraft.world.entity.Entity.class, BlockPos.class,
                Direction.class).getReturnType());
        assertNotSame(GolemView.Marker.class, GolemMarker.class);
    }

    private static List<String> methods(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods()).filter(method -> !method.isSynthetic()).map(Method::getName).sorted().toList();
    }

    private static GolemView view(int material, int core, List<GolemView.Marker> markers) {
        return new GolemView(UUID.randomUUID(), Level.OVERWORLD, material, core, "HG", 20F, new Vec3(1.5, 2, 3.5),
                new BlockPos(1, 2, 3), Direction.NORTH, markers);
    }
}
