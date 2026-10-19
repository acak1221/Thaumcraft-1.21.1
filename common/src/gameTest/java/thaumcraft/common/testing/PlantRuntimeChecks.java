package thaumcraft.common.testing;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftPlants;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.world.WorldGenCustomFlowers;

/** Two genuine plants and their real recipes, not a natural-distribution verdict. */
public final class PlantRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private PlantRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var level = test.getLevel();
        var origin = test.absolutePos(new BlockPos(3, 2, 3));
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<BlockPos, BlockEntity> entities = new LinkedHashMap<>();
        for (BlockPos mutable : BlockPos.betweenClosed(origin.offset(-7, -4, -7), origin.offset(7, 4, 7))) {
            var pos = mutable.immutable();
            states.put(pos, level.getBlockState(pos));
            if (level.getBlockEntity(pos) != null) entities.put(pos, level.getBlockEntity(pos));
        }
        try {
            for (boolean cinderpearl : new boolean[] {false, true}) {
                var flower = cinderpearl ? ThaumcraftPlants.CINDERPEARL.get() : ThaumcraftPlants.SHIMMERLEAF.get();
                for (var pos : states.keySet()) level.setBlock(pos, pos.getY() == origin.getY() - 1
                        ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
                level.setBlock(origin, flower.defaultBlockState(), QUIET);
                var state = level.getBlockState(origin);
                test.assertTrue(state.getLightEmission() == 8 && state.getCollisionShape(level, origin).isEmpty()
                        && state.getShape(level, origin).bounds().maxY == .8,
                        "original decorative plants emit8, have no collision and keep the .8-height selection bounds");
                for (Block soil : List.of(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.FARMLAND, Blocks.SAND, Blocks.RED_SAND, Blocks.STONE)) {
                    level.setBlock(origin.below(), soil.defaultBlockState(), QUIET);
                    boolean expected = soil != Blocks.STONE && (cinderpearl || soil != Blocks.SAND && soil != Blocks.RED_SAND);
                    test.assertTrue(state.canSurvive(level, origin) == expected,
                            "original Forge Bush/Plains/Desert soil rule: cinderpearl=" + cinderpearl + "; soil=" + soil);
                }
                var drops = Block.getDrops(state, level, origin, null);
                test.assertTrue(drops.size() == 1 && drops.getFirst().is(flower.asItem()) && drops.getFirst().getCount() == 1,
                        "harvest returns the actual plant, not direct powder/quicksilver");
                var input = CraftingInput.of(1, 1, List.of(new ItemStack(flower)));
                var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow().value();
                var result = recipe.assemble(input, level.registryAccess());
                test.assertTrue(result.getCount() == 1 && result.is(cinderpearl ? Items.BLAZE_POWDER : ThaumcraftContent.MATERIALS.get("quick_silver").get()),
                        "ConfigRecipes414-415: exactly one resource from one genuine harvested flower");
                var tags = ObjectAspectResolver.get(new ItemStack(flower), level);
                test.assertTrue(tags.getAmount(Aspect.PLANT) == 2 && tags.getAmount(Aspect.MAGIC) == 2
                        && tags.getAmount(cinderpearl ? Aspect.FIRE : Aspect.EXCHANGE) == 2,
                        "ConfigAspects395-396 original direct plant tags, shared by scans and alchemy");

                level.setBlock(origin, Blocks.AIR.defaultBlockState(), QUIET);
                level.setBlock(origin.below(), Blocks.GRASS_BLOCK.defaultBlockState(), QUIET);
                var source = RandomSource.create(10);
                Set<BlockPos> expected = new HashSet<>();
                for (int attempt = 0; attempt < 18; attempt++) {
                    int x = source.nextInt(8) - source.nextInt(8), y = source.nextInt(4) - source.nextInt(4), z = source.nextInt(8) - source.nextInt(8);
                    if (y == 0) expected.add(origin.offset(x, y, z));
                }
                var actualRandom = RandomSource.create(10);
                int planted = WorldGenCustomFlowers.place(level, actualRandom, origin, flower);
                test.assertTrue(planted == expected.size() && actualRandom.nextInt() == source.nextInt()
                        && expected.stream().allMatch(pos -> level.getBlockState(pos).is(flower)),
                        "original18-attempt flower pass preserves its108 integer draws and grass-supported actual placements");
            }
        } finally {
            for (var entry : states.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), QUIET);
            for (var entry : entities.entrySet()) { entry.getValue().clearRemoved(); level.setBlockEntity(entry.getValue()); }
        }
        test.succeed();
    }
}
