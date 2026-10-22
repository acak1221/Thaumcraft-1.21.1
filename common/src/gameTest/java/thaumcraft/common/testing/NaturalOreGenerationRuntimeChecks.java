package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftOres;
import thaumcraft.common.config.ThaumcraftWorldgen;

/**
 * Checks the actual biome→placed feature→fresh noise chunks chain, not placed test ore.
 * GameTestServer builds dimensions from minecraft:flat with an EMPTY LevelStem
 * registry, ignoring standalone dimension JSON. The test-only flat preset keeps
 * its vanilla three dimensions unchanged and adds this normal-noise dimension.
 */
public final class NaturalOreGenerationRuntimeChecks {
    private NaturalOreGenerationRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var key = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "natural"));
        var level = test.getLevel().getServer().getLevel(key);
        test.assertTrue(level != null, "test-only normal terrain dimension is loaded");
        level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).getHolderOrThrow(ThaumcraftWorldgen.ORES_KEY);
        Map<Block, String> infused = new LinkedHashMap<>();
        ThaumcraftOres.INFUSED.forEach((tag, block) -> infused.put(block.get(), tag));
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<ItemStack> shards = new ArrayList<>();
        var pos = new BlockPos.MutableBlockPos();
        var pickaxe = new ItemStack(Items.IRON_PICKAXE);
        // Owned test dimension only. Production generation must not force chunks;
        // here requesting new chunks is the operation being verified.
        for (int chunkX = 96; chunkX < 100; chunkX++) {
            for (int chunkZ = 96; chunkZ < 100; chunkZ++) {
                var chunk = level.getChunk(chunkX, chunkZ);
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < level.getMaxBuildHeight(); y++) {
                        pos.set(chunkX * 16 + x, y, chunkZ * 16 + z);
                        var state = chunk.getBlockState(pos);
                        String tag = infused.get(state.getBlock());
                        if (tag != null) {
                            counts.merge(tag, 1, Integer::sum);
                            if (shards.size() < 4) {
                                test.assertTrue(pickaxe.isCorrectToolForDrops(state), "normal iron pickaxe can harvest infused stone");
                                var drops = Block.getDrops(state, level, pos.immutable(), null, null, pickaxe);
                                for (var stack : drops) {
                                    test.assertTrue(stack.is(ThaumcraftContent.SHARDS.get(tag).get()), "natural infused stone drops its original primal shard");
                                    for (int count = 0; count < stack.getCount() && shards.size() < 4; count++) shards.add(stack.copyWithCount(1));
                                }
                            }
                        } else if (state.is(ThaumcraftOres.CINNABAR.get())) counts.merge("cinnabar", 1, Integer::sum);
                        else if (state.is(ThaumcraftOres.AMBER.get())) counts.merge("amber", 1, Integer::sum);
                    }
                }
            }
        }
        test.assertTrue(shards.size() == 4, "actual new terrain generates harvestable shards without fixture block insertion");
        // The original thaumometer is four shards in the four corners of the
        // three by three (oracle ConfigRecipes:458), two gold ingots and a glass.
        var grid = NonNullList.withSize(9, ItemStack.EMPTY);
        grid.set(0, shards.get(0)); grid.set(2, shards.get(1));
        grid.set(6, shards.get(2)); grid.set(8, shards.get(3));
        grid.set(3, new ItemStack(Items.GOLD_INGOT)); grid.set(5, new ItemStack(Items.GOLD_INGOT));
        grid.set(4, new ItemStack(Items.GLASS));
        var input = CraftingInput.of(3, 3, grid);
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        test.assertTrue(recipe.isPresent()
                        && recipe.get().value().assemble(input, level.registryAccess()).is(ThaumcraftContent.THAUMOMETER.get()),
                "naturally generated shard drops feed the real registered thaumometer recipe");
        Thaumcraft.LOG.info("TC4 natural noise-chunk ore generation: {}", counts);
        test.succeed();
    }
}
