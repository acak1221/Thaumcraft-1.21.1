package thaumcraft.common.testing;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TilePedestal;

/** Finite, declared altar/essentia fixtures; genuine jar-brain recipe and matrix completion. */
public final class InfusionWarpAuditChecks {
    private InfusionWarpAuditChecks() {}

    public static void jarBrainAwardsWarpOnce(GameTestHelper test) {
        craftAwardsWarpOnce(test, "JARBRAIN", "jar_brain");
    }

    public static void compassStoneAwardsWarpOnce(GameTestHelper test) {
        craftAwardsWarpOnce(test, "SINSTONE", "compass_stone");
    }

    private static void craftAwardsWarpOnce(GameTestHelper test, String research, String outputId) {
        // Separate heights keep the two 15x15 altar scans out of neighboring tests.
        var lane = new WaveLane(test, 3, outputId.equals("jar_brain") ? 80 : 110, 3);
        TileInfusionMatrix matrix = null;
        var jars = new ArrayList<TileJar>();
        try {
            var level = lane.level;
            BlockPos centerPos = lane.origin, matrixPos = centerPos.above(2);
            var player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(matrixPos.getX() + .5, matrixPos.getY(), matrixPos.getZ() + 2);
            for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++) {
                lane.put(centerPos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                lane.put(centerPos.offset(x, 0, z), Math.abs(x) <= 1 && Math.abs(z) <= 1
                        ? Blocks.AIR.defaultBlockState() : Blocks.SKELETON_SKULL.defaultBlockState());
            }
            lane.put(centerPos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
            lane.put(matrixPos, ThaumcraftInfusion.MATRIX.get().defaultBlockState());
            for (int x : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) {
                lane.put(centerPos.offset(x, 0, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.LOWER));
                lane.put(centerPos.offset(x, 1, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.UPPER));
            }
            var recipe = level.getRecipeManager().getAllRecipesFor(ThaumcraftInfusion.INFUSION_RECIPE_TYPE.get())
                    .stream().map(h -> h.value()).filter(r -> r.research().equals(research)).findFirst().orElseThrow();
            var pedestals = new ArrayList<TilePedestal>();
            int[][] offsets = {{-3, 0}, {3, 0}, {0, -3}, {0, 3}};
            for (int i = 0; i < offsets.length; i++) {
                var pos = centerPos.offset(offsets[i][0], 0, offsets[i][1]);
                lane.put(pos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
                var pedestal = (TilePedestal) level.getBlockEntity(pos);
                pedestal.setItem(0, recipe.components().get(i).getItems()[0].copy());
                pedestals.add(pedestal);
            }
            int index = 0;
            for (var entry : recipe.aspects().entrySet()) {
                var pos = centerPos.offset(-1 + index++, 0, 5);
                lane.put(pos, ThaumcraftContent.JAR.get().defaultBlockState());
                var jar = (TileJar) level.getBlockEntity(pos);
                jar.addToContainer(Aspect.getAspect(entry.getKey()), entry.getValue());
                jars.add(jar);
            }
            var center = (TilePedestal) level.getBlockEntity(centerPos);
            center.setItem(0, research.equals("JARBRAIN") ? new ItemStack(ThaumcraftContent.JAR.get()) : new ItemStack(Items.FLINT));
            matrix = (TileInfusionMatrix) level.getBlockEntity(matrixPos);
            matrix.setActive(true);
            var knowledge = KnowledgeAccess.get(player);
            int before = knowledge.getWarpSticky();
            test.assertTrue(!matrix.startCrafting(player) && knowledge.getWarpSticky() == before,
                    "unresearched infusion refusal awards no warp");
            knowledge.addResearch(research);
            test.assertTrue(matrix.startCrafting(player) && matrix.getInstability() < 0, "real jar-brain infusion starts with symmetric stabilizers");
            for (int tick = 0; tick < 1200 && matrix.isCrafting(); tick++)
                TileInfusionMatrix.tick(level, matrixPos, matrix.getBlockState(), matrix);
            test.assertTrue(!matrix.isCrafting() && center.getItem(0).is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", outputId))),
                    "real recipe reaches physical success output: " + outputId);
            test.assertTrue(jars.stream().allMatch(j -> j.getAmount() == 0)
                    && (research.equals("JARBRAIN") ? pedestals.stream().filter(p -> !p.isEmpty()).count() == 1
                    && pedestals.stream().anyMatch(p -> p.getItem(0).is(Items.BUCKET)) : pedestals.stream().allMatch(p -> p.isEmpty())),
                    "finite essence and ingredients paid once, any bucket remainder retained");
            test.assertTrue(knowledge.getWarpSticky() == before + 1,
                    "one completed " + outputId + " infusion awards original one sticky warp; actual delta=" + (knowledge.getWarpSticky() - before));
            for (int tick = 0; tick < 40; tick++) TileInfusionMatrix.tick(level, matrixPos, matrix.getBlockState(), matrix);
            test.assertTrue(knowledge.getWarpSticky() == before + 1, "completed matrix never repeats crafting warp");
            center.setItem(0, research.equals("JARBRAIN") ? new ItemStack(ThaumcraftContent.JAR.get()) : new ItemStack(Items.FLINT));
            for (int i = 0; i < pedestals.size(); i++) pedestals.get(i).setItem(0, recipe.components().get(i).getItems()[0].copy());
            test.assertTrue(matrix.startCrafting(player), "second real ritual starts for failure-path check");
            center.setItem(0, new ItemStack(Items.DIRT));
            // Original invalid-center abort selects one hazard. Declare terrain-safe
            // explosion9 so this negative case is not itself a legitimate warp hazard.
            long seed = 0;
            while (net.minecraft.util.RandomSource.create(seed).nextInt(21) != 9) seed++;
            level.random.setSeed(seed);
            player.setPos(matrixPos.getX() + 20, matrixPos.getY(), matrixPos.getZ());
            for (int tick = 0; tick < 10 && matrix.isCrafting(); tick++) TileInfusionMatrix.tick(level, matrixPos, matrix.getBlockState(), matrix);
            test.assertTrue(!matrix.isCrafting() && center.getItem(0).is(Items.DIRT) && knowledge.getWarpSticky() == before + 1,
                    "invalid-center abort gives no output or completion warp");
        } finally {
            if (matrix != null) {
                var tag = matrix.saveWithFullMetadata(lane.level.registryAccess());
                tag.putBoolean("active", false); tag.putBoolean("crafting", false);
                matrix.loadWithComponents(tag, lane.level.registryAccess());
            }
            thaumcraft.common.lib.events.EssentiaHandler.clear(lane.level);
            lane.clean();
        }
        test.succeed();
    }
}
