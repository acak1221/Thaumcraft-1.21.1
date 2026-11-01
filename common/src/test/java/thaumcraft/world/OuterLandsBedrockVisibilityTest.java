package thaumcraft.world;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.OuterLandsProvider;

import static org.junit.jupiter.api.Assertions.*;

/** Oracle GenCommon:319-325: a nonopaque opening must actually expose the opposite bedrock. */
class OuterLandsBedrockVisibilityTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void ordinaryTwoSidedCornersRemainDecorationCandidates() throws Exception {
        assertFalse(showing(Map.of(BlockPos.ZERO.below(), Blocks.AIR.defaultBlockState(),
                BlockPos.ZERO.north(), Blocks.AIR.defaultBlockState())));
    }

    @Test void anOpeningFacingTheBedrockLiningIsRejected() throws Exception {
        assertTrue(showing(Map.of(BlockPos.ZERO.north(), Blocks.AIR.defaultBlockState(),
                BlockPos.ZERO.south(), Blocks.BEDROCK.defaultBlockState())));
    }

    @Test void opaqueNeighboursHideBedrockButGlassDoesNot() throws Exception {
        assertFalse(showing(Map.of(BlockPos.ZERO.south(), Blocks.BEDROCK.defaultBlockState())));
        assertTrue(showing(Map.of(BlockPos.ZERO.north(), Blocks.GLASS.defaultBlockState(),
                BlockPos.ZERO.south(), Blocks.BEDROCK.defaultBlockState())));
    }

    private static boolean showing(Map<BlockPos, BlockState> neighbours) throws Exception {
        WorldGenLevel region = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getBlockState"))
                        return neighbours.getOrDefault((BlockPos) args[0], Blocks.STONE.defaultBlockState());
                    throw new AssertionError("Unexpected world access: " + method.getName());
                });
        Method method = OuterLandsProvider.OuterLandsMazeFeature.class.getDeclaredMethod(
                "bedrockShowing", WorldGenLevel.class, BlockPos.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, region, BlockPos.ZERO);
    }
}
