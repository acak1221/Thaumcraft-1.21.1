package thaumcraft.common.testing;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftPlants;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.world.WorldGenCustomFlowers;

/** Two genuine plants and their real recipes, not a natural-distribution verdict. */
public final class PlantRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private PlantRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
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
                    level.setBlock(origin, Blocks.AIR.defaultBlockState(), QUIET);
                    // Oracle BlockCustomPlantItem:74 - the ITEM asks the soil for its own plant type
                    // through the Forge 1.7.10 table of Block.canSustainPlant: the inherited
                    // BlockBush soil (the grass, the dirt, the farmland) for every plant type and the
                    // sand for the Desert type (the cinderpearl, the metadata three). The red sand
                    // is not in the table of the original: the 1.7.10 Desert soil is Blocks.sand.
                    boolean planted = plantWithItem(player, level, origin.below(), new ItemStack(flower.asItem()));
                    boolean itemSoil = soil == Blocks.DIRT || soil == Blocks.GRASS_BLOCK || soil == Blocks.FARMLAND
                            || (cinderpearl && soil == Blocks.SAND);
                    test.assertTrue(planted == itemSoil,
                            "original Forge BlockBush/Plains/Desert soil rule of the ITEM: cinderpearl=" + cinderpearl + "; soil=" + soil);
                    // Oracle BlockCustomPlant:130-132 answers canPlaceBlockAt with a plain true, so the
                    // plant is never refused a position. The rule that removes it again is the
                    // inherited 1.7.10 BlockBush.func_149718_j (canBlockStay, the official client jar,
                    // the class ajr): the grass, the dirt or the farmland below. The cinderpearl
                    // stands on the sand of the original item but it does not stay there.
                    level.setBlock(origin, flower.defaultBlockState(), QUIET);
                    boolean staySoil = soil == Blocks.DIRT || soil == Blocks.GRASS_BLOCK || soil == Blocks.FARMLAND;
                    test.assertTrue(flower.defaultBlockState().canSurvive(level, origin) == staySoil,
                            "original BlockBush.canBlockStay rule of the BLOCK: cinderpearl=" + cinderpearl + "; soil=" + soil);
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
            // Oracle BlockCustomPlantItem:85-101 - the item of the manashroom (the metadata five) asks
            // the soil for a Cave plant, and the Cave case of the Forge 1.7.10 table of
            // Block.canSustainPlant is isSideSolid(world, x, y, z, UP): the soil must present a solid
            // face on its top side. The glass of 1.21.1 carries noOcclusion, so it is not solid on its
            // top side, exactly as the translucent glass of the 1.7.10 was not.
            for (Block soil : List.of(Blocks.STONE, Blocks.SAND, Blocks.GLASS)) {
                level.setBlock(origin.below(), soil.defaultBlockState(), QUIET);
                level.setBlock(origin, Blocks.AIR.defaultBlockState(), QUIET);
                boolean planted = plantWithItem(player, level, origin.below(),
                        new ItemStack(ThaumcraftBiomes.VISHROOM_ITEM.get()));
                test.assertTrue(planted == (soil != Blocks.GLASS),
                        "original Forge Cave soil rule of the ITEM (isSideSolid UP): soil=" + soil);
            }
        } finally {
            for (var entry : states.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), QUIET);
            for (var entry : entities.entrySet()) { entry.getValue().clearRemoved(); level.setBlockEntity(entry.getValue()); }
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        test.succeed();
    }

    /**
     * The original placement of the item of the plant.
     *
     * <p>Oracle {@code thaumcraft/common/blocks/BlockCustomPlantItem.java:69-83}: the item places
     * the plant only on the top face of a block whose soil supports its plant type
     * ({@code BlockCustomPlantItem:74} asks the soil through the Forge table of
     * {@code Block.canSustainPlant}) and it consumes one item.</p>
     */
    private static boolean plantWithItem(ServerPlayer player, ServerLevel level, BlockPos soil, ItemStack item) {
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        var hit = new BlockHitResult(Vec3.atCenterOf(soil).add(0.0, 0.5, 0.0), Direction.UP, soil, false);
        var used = player.gameMode.useItemOn(player, level, player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND, hit);
        boolean placed = used.consumesAction() && level.getBlockState(soil.above()).is(item.getItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return placed;
    }
}
