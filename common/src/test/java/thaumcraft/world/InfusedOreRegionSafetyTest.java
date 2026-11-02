package thaumcraft.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.ThaumcraftOreFeature;

/** A refused vein must not force/read terrain or consume random draws in the native ore generator. */
class InfusedOreRegionSafetyTest {
    private static final BlockPos ORIGIN = new BlockPos(15, -32, 15);

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void outOfHeightVeinsDoNotInspectOrGenerateNeighbourChunks() throws Exception {
        List<String> calls = new ArrayList<>();
        assertRefused(region(true, false, false, calls));
        assertEquals(List.of("height"), calls);
    }

    @Test void missingNeighbourChunksAreNotRequestedByEitherOrePass() throws Exception {
        for (boolean deep : new boolean[]{false, true}) {
            List<String> calls = new ArrayList<>();
            assertRefused(region(false, false, false, calls), deep);
            assertEquals(List.of("height", "loaded:12,-32,12:18,-32,18"), calls);
        }
    }

    @Test void loadedButUnwritableNeighbourhoodDoesNotReadOrPlaceAnOre() throws Exception {
        for (boolean deep : new boolean[]{false, true}) {
            List<String> calls = new ArrayList<>();
            assertRefused(region(false, true, false, calls), deep);
            assertEquals(List.of("height", "loaded:12,-32,12:18,-32,18", "write:12,-32,12"), calls);
        }
    }

    private static void assertRefused(WorldGenLevel region) throws Exception { assertRefused(region, true); }

    private static void assertRefused(WorldGenLevel region, boolean deep) throws Exception {
        Method place = ThaumcraftOreFeature.class.getDeclaredMethod("infusedVein", WorldGenLevel.class,
                RandomSource.class, BlockPos.class, int.class, boolean.class);
        place.setAccessible(true);
        RandomSource random = RandomSource.create(9102026L);
        assertFalse((boolean) place.invoke(null, region, random, ORIGIN, 1, deep));
        assertEquals(RandomSource.create(9102026L).nextLong(), random.nextLong(), "a refused native vein consumes no RNG");
    }

    private static WorldGenLevel region(boolean outside, boolean loaded, boolean writable, List<String> calls) {
        return (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isOutsideBuildHeight" -> { calls.add("height"); yield outside; }
                    case "hasChunksAt" -> {
                        calls.add("loaded:" + compact((BlockPos) args[0]) + ":" + compact((BlockPos) args[1])); yield loaded;
                    }
                    case "ensureCanWrite" -> { calls.add("write:" + compact((BlockPos) args[0])); yield writable; }
                    default -> throw new AssertionError("Refused ore touched terrain: " + method.getName());
                });
    }

    private static String compact(BlockPos pos) { return pos.getX() + "," + pos.getY() + "," + pos.getZ(); }
}
