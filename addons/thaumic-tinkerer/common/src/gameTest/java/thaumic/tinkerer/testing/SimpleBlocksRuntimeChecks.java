package thaumic.tinkerer.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.research.ResearchCategories;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.block.BlockCamo;
import thaumic.tinkerer.common.block.BlockGas;
import thaumic.tinkerer.common.block.BlockInfusedGrain;
import thaumic.tinkerer.common.block.BlockNitorGas;
import thaumic.tinkerer.common.block.BlockPlatform;
import thaumic.tinkerer.common.block.BlockWardSlab;
import thaumic.tinkerer.common.block.fire.BlockFireBase;
import thaumic.tinkerer.common.block.tile.TileCamo;
import thaumic.tinkerer.common.block.tile.TileForcefield;
import thaumic.tinkerer.common.block.tile.TileInfusedFarmland;
import thaumic.tinkerer.common.block.tile.TileInfusedGrain;
import thaumic.tinkerer.common.block.tile.TileWardedSlab;
import thaumic.tinkerer.common.content.SimpleBlocks;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.core.helper.AspectCropLootManager;
import thaumic.tinkerer.common.item.InfusedSeedComponents;
import thaumic.tinkerer.common.item.ItemBrightNitor;
import thaumic.tinkerer.common.item.ItemGas;
import thaumic.tinkerer.common.item.ItemGasRemover;
import thaumic.tinkerer.common.item.ItemInfusedGrain;
import thaumic.tinkerer.common.item.ItemInfusedPotion;
import thaumic.tinkerer.common.item.ItemInfusedSeeds;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.potion.ModPotions;
import thaumic.tinkerer.common.potion.PotionEffectHandler;
import thaumic.tinkerer.common.registry.TTRecipes;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>Runtime checks of the SimpleBlocks group in a running server: registration, the gases, the platform, the warded
 * and travel slabs, the forcefield, the infused soil and crops, the potions and the elemental fires, the data recipes and
 * the persistence of the block entities. They were written without being able to run them (no Gradle in the porting
 * session): the first run may show a wrong assumption of a check as well as a defect.</p>
 *
 * <p>Every check builds its own scene on a stone floor at relative y = 0 (the empty structure is 8 by 8 cells wide).
 * Where a check needs the passing of time it uses {@code runAfterDelay}; everything that can be driven by calling the
 * tick method directly is, so that a check does not depend on chance.</p>
 */
public final class SimpleBlocksRuntimeChecks {
    private SimpleBlocksRuntimeChecks() {}

    // ---- helpers ---------------------------------------------------------------------------------------------------

    private static void floor(GameTestHelper test) {
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) test.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    /** A server player who is not in creative mode (the gas and the seeds are used up), standing on the floor. */
    private static ServerPlayer mockPlayer(GameTestHelper test, double x, double z) {
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.setPos(test.absoluteVec(new Vec3(x, 1.0, z)));
        return player;
    }

    /** Removes a mock player (it stays in the world otherwise and keeps the nitor gas alive) and ends the check. */
    private static void finish(GameTestHelper test, ServerPlayer player) {
        player.discard();
        test.succeed();
    }

    private static <T extends BlockEntity> T tile(GameTestHelper test, BlockPos relative, Class<T> type) {
        BlockEntity entity = test.getLevel().getBlockEntity(test.absolutePos(relative));
        test.assertTrue(type.isInstance(entity), "a " + type.getSimpleName() + " at " + relative + ", found " + entity);
        return type.cast(entity);
    }

    private static BlockState stateAt(GameTestHelper test, BlockPos relative) {
        return test.getLevel().getBlockState(test.absolutePos(relative));
    }

    private static boolean isGas(BlockState state, Block gas, int level) {
        return state.is(gas) && state.getValue(BlockGas.LEVEL) == level;
    }

    private static boolean has(String path) {
        return BuiltInRegistries.BLOCK.containsKey(LibMisc.id(path));
    }

    /** The server tick of 1.7.10 {@code updateTick}, through the public path of the block state. */
    private static void tickBlock(ServerLevel level, BlockPos absolute) {
        level.getBlockState(absolute).tick(level, absolute, level.random);
    }

    // ---- registration and data -------------------------------------------------------------------------------------

    private static final List<String> BLOCKS_WITH_ITEM = List.of("dark_quartz", "dark_quartz_chiseled", "dark_quartz_pillar",
            "dark_quartz_slab", "dark_quartz_stairs", "travel_slab", "travel_stairs", "ward_slab", "platform", "fire_air",
            "fire_water", "fire_earth", "fire_fire", "fire_order", "fire_chaos");
    private static final List<String> BLOCKS_WITHOUT_ITEM = List.of("gaseous_light", "gaseous_shadow", "nitor_gas", "forcefield",
            "infused_farmland", "infused_grain_block");
    private static final List<String> ITEMS = List.of("dark_quartz_item", "gaseous_light_item", "gaseous_shadow_item", "gas_remover",
            "bright_nitor", "infused_grain_aer", "infused_grain_ignis", "infused_grain_terra", "infused_grain_aqua",
            "infused_potion_aer", "infused_potion_ignis", "infused_potion_terra", "infused_potion_aqua", "infused_seeds");

    public static void registrationIsComplete(GameTestHelper test) {
        test.assertTrue(ThaumicTinkerer.failures().isEmpty(), "registration failures: " + ThaumicTinkerer.failures());
        for (String block : BLOCKS_WITH_ITEM) {
            test.assertTrue(has(block), "block " + block);
            test.assertTrue(BuiltInRegistries.ITEM.containsKey(LibMisc.id(block)), "block item " + block);
        }
        for (String block : BLOCKS_WITHOUT_ITEM) {
            test.assertTrue(has(block), "block " + block);
            test.assertTrue(!BuiltInRegistries.ITEM.containsKey(LibMisc.id(block)), "no item for " + block);
        }
        for (String item : ITEMS) test.assertTrue(BuiltInRegistries.ITEM.containsKey(LibMisc.id(item)), "item " + item);
        for (String effect : ModPotions.PATHS) {
            test.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(LibMisc.id(effect)), "effect " + effect);
        }
        test.assertTrue(SimpleBlocks.TILE_CAMO.isPresent() && SimpleBlocks.TILE_WARDED_SLAB.isPresent()
                && SimpleBlocks.TILE_FORCEFIELD.isPresent() && SimpleBlocks.TILE_INFUSED_FARMLAND.isPresent()
                && SimpleBlocks.TILE_INFUSED_GRAIN.isPresent(), "block entities");
        test.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(LibMisc.id(InfusedSeedComponents.ASPECT_PATH))
                && BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(LibMisc.id(InfusedSeedComponents.TENDENCIES_PATH)), "components");

        // research and recipes of the group
        for (String key : List.of(LibResearch.KEY_PLATFORM, LibResearch.KEY_DARK_QUARTZ, LibResearch.KEY_GASEOUS_LIGHT,
                LibResearch.KEY_GASEOUS_SHADOW, LibResearch.KEY_GAS_REMOVER, LibResearch.KEY_BRIGHT_NITOR, LibResearch.KEY_POTIONS)) {
            test.assertTrue(ResearchCategories.getResearch(key) != null, "research " + key);
        }
        if (ConfigHandler.enableFire) {
            for (String key : List.of(LibResearch.KEY_FIRE_AER, LibResearch.KEY_FIRE_AQUA, LibResearch.KEY_FIRE_IGNIS,
                    LibResearch.KEY_FIRE_TERRA, LibResearch.KEY_FIRE_ORDO, LibResearch.KEY_FIRE_PERDITIO)) {
                test.assertTrue(ResearchCategories.getResearch(key) != null, "research " + key);
                test.assertTrue(TTRecipes.has(key), "recipe " + key);
            }
        }
        for (String recipe : List.of(LibResearch.KEY_PLATFORM, LibResearch.KEY_GASEOUS_LIGHT, LibResearch.KEY_GASEOUS_SHADOW,
                LibResearch.KEY_GAS_REMOVER, LibResearch.KEY_BRIGHT_NITOR, LibResearch.KEY_POTIONS + 0, LibResearch.KEY_POTIONS + 5,
                LibResearch.KEY_POTIONS + "POT0", LibResearch.KEY_POTIONS + "POT3")) {
            test.assertTrue(TTRecipes.has(recipe), "recipe " + recipe);
        }

        // the loot of the crops was filled when Thaumcraft was ready
        test.assertTrue(!AspectCropLootManager.getAllLoot().get(Aspect.AIR).isEmpty(), "loot of the aer crop");
        test.assertTrue(!AspectCropLootManager.getAllLoot().get(Aspect.AURA).isEmpty(), "loot of the aura crop");
        test.succeed();
    }

    public static void tagsOfTheGroupAreBound(GameTestHelper test) {
        for (String fire : List.of("fire_air", "fire_water", "fire_earth", "fire_fire", "fire_order", "fire_chaos")) {
            test.assertTrue(BuiltInRegistries.BLOCK.get(LibMisc.id(fire)).defaultBlockState().is(BlockTags.FIRE), fire + " is fire");
        }
        for (String slab : List.of("dark_quartz_slab", "travel_slab", "ward_slab")) {
            test.assertTrue(BuiltInRegistries.BLOCK.get(LibMisc.id(slab)).defaultBlockState().is(BlockTags.SLABS), slab + " is a slab");
        }
        for (String stairs : List.of("dark_quartz_stairs", "travel_stairs")) {
            test.assertTrue(BuiltInRegistries.BLOCK.get(LibMisc.id(stairs)).defaultBlockState().is(BlockTags.STAIRS), stairs + " are stairs");
        }
        for (String block : List.of("dark_quartz", "dark_quartz_chiseled", "dark_quartz_pillar", "dark_quartz_slab", "dark_quartz_stairs",
                "travel_slab", "travel_stairs", "ward_slab")) {
            test.assertTrue(BuiltInRegistries.BLOCK.get(LibMisc.id(block)).defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                    block + " is mined with a pickaxe");
        }
        test.assertTrue(SimpleBlocks.PLATFORM.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE), "platform: axe");
        test.assertTrue(SimpleBlocks.INFUSED_FARMLAND.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_SHOVEL), "farmland: shovel");
        test.succeed();
    }

    // ---- recipes ---------------------------------------------------------------------------------------------------

    private static ItemStack craft(GameTestHelper test, int width, int height, ItemStack... grid) {
        CraftingInput input = CraftingInput.of(width, height, List.of(grid));
        ServerLevel level = test.getLevel();
        Optional<RecipeHolder<CraftingRecipe>> found = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (found.isEmpty()) return ItemStack.EMPTY;
        return found.get().value().assemble(input, level.registryAccess());
    }

    public static void quartzRecipesCraftTheirResults(GameTestHelper test) {
        ItemStack quartz = new ItemStack(Items.QUARTZ);
        ItemStack coal = new ItemStack(Items.COAL);
        ItemStack charcoal = new ItemStack(Items.CHARCOAL);
        ItemStack e = ItemStack.EMPTY;
        ItemStack item = new ItemStack(SimpleBlocks.DARK_QUARTZ_ITEM.get());
        ItemStack block = new ItemStack(SimpleBlocks.DARK_QUARTZ.get());
        ItemStack chiseled = new ItemStack(SimpleBlocks.DARK_QUARTZ_CHISELED.get());
        ItemStack pillar = new ItemStack(SimpleBlocks.DARK_QUARTZ_PILLAR.get());
        ItemStack slab = new ItemStack(SimpleBlocks.DARK_QUARTZ_SLAB.get());

        ItemStack made = craft(test, 3, 3, quartz, e, quartz, e, coal, e, quartz, e, quartz);
        test.assertTrue(made.is(SimpleBlocks.DARK_QUARTZ_ITEM.get()) && made.getCount() == 4, "four smokey quartz from coal: " + made);
        made = craft(test, 3, 3, quartz, e, quartz, e, charcoal, e, quartz, e, quartz);
        test.assertTrue(made.is(SimpleBlocks.DARK_QUARTZ_ITEM.get()) && made.getCount() == 4, "and from charcoal: " + made);

        made = craft(test, 2, 2, item, item, item, item);
        test.assertTrue(made.is(SimpleBlocks.DARK_QUARTZ.get().asItem()) && made.getCount() == 1, "a block from four: " + made);

        made = craft(test, 3, 1, block, chiseled, pillar);
        test.assertTrue(made.is(SimpleBlocks.DARK_QUARTZ_SLAB.get().asItem()) && made.getCount() == 6, "six slabs from any three blocks: " + made);
        made = craft(test, 1, 2, chiseled, block);
        test.assertTrue(made.is(SimpleBlocks.DARK_QUARTZ_PILLAR.get().asItem()) && made.getCount() == 2, "two pillars from any two blocks: " + made);
        made = craft(test, 1, 2, slab, slab);
        test.assertTrue(made.is(SimpleBlocks.DARK_QUARTZ_CHISELED.get().asItem()) && made.getCount() == 1, "a chiselled block from two slabs: " + made);
        made = craft(test, 3, 3, e, e, block, e, block, block, block, block, block);
        test.assertTrue(made.is(SimpleBlocks.DARK_QUARTZ_STAIRS.get().asItem()) && made.getCount() == 4, "four stairs: " + made);
        made = craft(test, 3, 3, block, e, e, block, block, e, block, block, block);
        test.assertTrue(made.is(SimpleBlocks.DARK_QUARTZ_STAIRS.get().asItem()) && made.getCount() == 4, "four stairs, mirrored: " + made);
        test.succeed();
    }

    public static void pavingRecipesCraftTheirResults(GameTestHelper test) {
        Item travel = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "paving_stone_travel"));
        Item ward = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "paving_stone_warding"));
        test.assertTrue(travel != Items.AIR && ward != Items.AIR, "Thaumcraft's paving stones exist");
        ItemStack t = new ItemStack(travel);
        ItemStack w = new ItemStack(ward);
        ItemStack e = ItemStack.EMPTY;
        ItemStack made = craft(test, 3, 1, t, t, t);
        test.assertTrue(made.is(SimpleBlocks.TRAVEL_SLAB.get().asItem()) && made.getCount() == 6, "six travel slabs: " + made);
        made = craft(test, 3, 3, e, e, t, e, t, t, t, t, t);
        test.assertTrue(made.is(SimpleBlocks.TRAVEL_STAIRS.get().asItem()) && made.getCount() == 4, "four travel stairs: " + made);
        made = craft(test, 3, 1, w, w, w);
        test.assertTrue(made.is(SimpleBlocks.WARD_SLAB.get().asItem()) && made.getCount() == 6, "six ward slabs: " + made);
        test.succeed();
    }

    // ---- drops -----------------------------------------------------------------------------------------------------

    public static void dropsAreTheOnesOfTheOriginal(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        BlockPos rel = new BlockPos(3, 1, 3);
        BlockPos abs = test.absolutePos(rel);
        for (Block block : List.of(SimpleBlocks.DARK_QUARTZ.get(), SimpleBlocks.DARK_QUARTZ_CHISELED.get(), SimpleBlocks.DARK_QUARTZ_PILLAR.get(),
                SimpleBlocks.DARK_QUARTZ_STAIRS.get(), SimpleBlocks.TRAVEL_STAIRS.get(), SimpleBlocks.PLATFORM.get())) {
            test.setBlock(rel, block);
            List<ItemStack> drops = Block.getDrops(level.getBlockState(abs), level, abs, level.getBlockEntity(abs));
            test.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()) && drops.get(0).getCount() == 1, block + " drops itself: " + drops);
        }
        for (Block block : List.of(SimpleBlocks.DARK_QUARTZ_SLAB.get(), SimpleBlocks.TRAVEL_SLAB.get(), SimpleBlocks.WARD_SLAB.get())) {
            test.setBlock(rel, block.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            List<ItemStack> drops = Block.getDrops(level.getBlockState(abs), level, abs, level.getBlockEntity(abs));
            test.assertTrue(drops.size() == 1 && drops.get(0).getCount() == 1, block + " half slab: " + drops);
            test.setBlock(rel, block.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE));
            drops = Block.getDrops(level.getBlockState(abs), level, abs, level.getBlockEntity(abs));
            int total = 0;
            for (ItemStack drop : drops) total += drop.is(block.asItem()) ? drop.getCount() : 1000;
            test.assertTrue(total == 2, block + " double slab drops two slabs: " + drops);
        }
        test.setBlock(rel, SimpleBlocks.INFUSED_FARMLAND.get());
        List<ItemStack> soil = Block.getDrops(level.getBlockState(abs), level, abs, level.getBlockEntity(abs));
        test.assertTrue(soil.size() == 1 && soil.get(0).is(Items.DIRT), "infused farmland drops dirt: " + soil);
        for (Block gas : List.of(SimpleBlocks.GASEOUS_LIGHT.get(), SimpleBlocks.GASEOUS_SHADOW.get(), SimpleBlocks.NITOR_GAS.get(),
                SimpleBlocks.FORCEFIELD.get(), SimpleBlocks.FIRE_AIR.get())) {
            test.setBlock(rel, gas);
            test.assertTrue(Block.getDrops(level.getBlockState(abs), level, abs, level.getBlockEntity(abs)).isEmpty(), gas + " drops nothing");
        }
        test.succeed();
    }

    // ---- gases -----------------------------------------------------------------------------------------------------

    public static void aGasCloudSpreadsInDiamondsAndSettles(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        Block gas = SimpleBlocks.GASEOUS_LIGHT.get();
        BlockPos rel = new BlockPos(3, 5, 3);
        BlockPos origin = test.absolutePos(rel);
        test.setBlock(rel, gas.defaultBlockState().setValue(BlockGas.LEVEL, BlockGas.RELEASE_LEVEL));

        tickBlock(level, origin);
        test.assertTrue(isGas(level.getBlockState(origin), gas, 0), "the source settles at level 0");
        for (Direction direction : Direction.values()) {
            test.assertTrue(isGas(level.getBlockState(origin.relative(direction)), gas, BlockGas.RELEASE_LEVEL - 1), "level 3 to the " + direction);
        }

        // keep ticking whatever still has a level: a level n cell makes level n-1 cells, level 1 makes the last, level 0, ones
        for (int round = 0; round < 6; round++) {
            for (BlockPos cell : BlockPos.betweenClosed(origin.offset(-5, -5, -5), origin.offset(5, 5, 5))) {
                BlockState state = level.getBlockState(cell);
                if (state.is(gas) && state.getValue(BlockGas.LEVEL) > 0) tickBlock(level, cell.immutable());
            }
        }
        int cells = 0;
        for (BlockPos cell : BlockPos.betweenClosed(origin.offset(-6, -6, -6), origin.offset(6, 6, 6))) {
            BlockState state = level.getBlockState(cell);
            int distance = Math.abs(cell.getX() - origin.getX()) + Math.abs(cell.getY() - origin.getY()) + Math.abs(cell.getZ() - origin.getZ());
            if (distance <= RELEASE_RANGE) {
                test.assertTrue(isGas(state, gas, 0), "gas at distance " + distance + " from the source, found " + state);
                cells++;
            } else {
                test.assertTrue(!state.is(gas), "no gas at distance " + distance + ", found " + state);
            }
        }
        test.assertTrue(cells == 129, "129 cells within four steps, found " + cells);
        test.succeed();
    }

    private static final int RELEASE_RANGE = BlockGas.RELEASE_LEVEL;

    public static void gasSpreadsIntoAirAndOtherGasButNotIntoSolidBlocksOrItself(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        Block light = SimpleBlocks.GASEOUS_LIGHT.get();
        Block shadow = SimpleBlocks.GASEOUS_SHADOW.get();
        BlockPos rel = new BlockPos(3, 2, 3);
        BlockPos origin = test.absolutePos(rel);
        test.setBlock(rel.east(), Blocks.STONE);
        // 1.7.10 counted every gas as air (isAirBlock), so a gas spreads over a gas of another kind, but not over its own kind
        test.setBlock(rel.west(), shadow.defaultBlockState().setValue(BlockGas.LEVEL, 2));
        test.setBlock(rel.north(), light.defaultBlockState().setValue(BlockGas.LEVEL, 1));
        test.setBlock(rel, light.defaultBlockState().setValue(BlockGas.LEVEL, 4));
        tickBlock(level, origin);
        test.assertTrue(level.getBlockState(origin.east()).is(Blocks.STONE), "stone stays");
        test.assertTrue(isGas(level.getBlockState(origin.west()), light, 3), "the other gas is overwritten");
        test.assertTrue(isGas(level.getBlockState(origin.north()), light, 1), "gas of its own kind is left as it was");
        test.assertTrue(isGas(level.getBlockState(origin.above()), light, 3), "air is filled");
        test.assertTrue(isGas(level.getBlockState(origin), light, 0), "and the source settles");
        test.succeed();
    }

    public static void aGasStateIsTheLightAndTheShadowOfTheOriginal(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos any = test.absolutePos(new BlockPos(1, 3, 1));
        BlockState light = SimpleBlocks.GASEOUS_LIGHT.get().defaultBlockState();
        BlockState shadow = SimpleBlocks.GASEOUS_SHADOW.get().defaultBlockState();
        BlockState nitor = SimpleBlocks.NITOR_GAS.get().defaultBlockState();
        test.assertTrue(light.getLightEmission() == 12, "Gaseous Illuminae shines with 12");
        test.assertTrue(shadow.getLightEmission() == 0 && shadow.getLightBlock(level, any) == 15, "Gaseous Tenebrae stops all light");
        test.assertTrue(light.getLightBlock(level, any) == 0 && nitor.getLightBlock(level, any) == 0, "the bright gases let light through");
        test.assertTrue(nitor.setValue(BlockGas.LEVEL, 0).getLightEmission() == 12 && nitor.setValue(BlockGas.LEVEL, 1).getLightEmission() == 15,
                "nitor gas: 12, and 15 for the strong variant");
        for (BlockState gas : List.of(light, shadow, nitor)) {
            test.assertTrue(gas.getCollisionShape(level, any).isEmpty(), "no collision");
            test.assertTrue(gas.getShape(level, any).isEmpty(), "no outline");
            test.assertTrue(gas.canBeReplaced(), "replaceable");
            test.assertTrue(BlockGas.isAirLike(gas) && !gas.isAir(), "counts as air for the add-on, is no air block for the light engine");
        }
        test.succeed();
    }

    public static void aPhialReleasesGasAtTheHeadAndTheCloudSpreadsByItself(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test, 3.5, 3.5);
        ServerLevel level = test.getLevel();
        ItemStack phial = new ItemStack(SimpleBlocks.GASEOUS_SHADOW_ITEM.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, phial);
        BlockPos gasRel = new BlockPos(3, 2, 3);

        InteractionResultHolder<ItemStack> result = phial.getItem().use(level, player, InteractionHand.MAIN_HAND);
        test.assertTrue(result.getResult().consumesAction(), "the use is consumed");
        test.assertTrue(phial.getCount() == 1, "one phial used up");
        test.assertTrue(isGas(stateAt(test, gasRel), SimpleBlocks.GASEOUS_SHADOW.get(), BlockGas.RELEASE_LEVEL), "gas of level 4 above the feet");

        // the scheduled tick (ten ticks) spreads it
        test.runAfterDelay(14, () -> {
            test.assertTrue(isGas(stateAt(test, gasRel), SimpleBlocks.GASEOUS_SHADOW.get(), 0), "the source settled by its scheduled tick");
            test.assertTrue(isGas(stateAt(test, gasRel.above()), SimpleBlocks.GASEOUS_SHADOW.get(), 3), "and spread above");
            finish(test, player);
        });
    }

    public static void aPhialIsUsedUpEvenWithoutRoomButNotInCreativeMode(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test, 3.5, 3.5);
        ServerLevel level = test.getLevel();
        BlockPos headRel = new BlockPos(3, 2, 3);
        test.setBlock(headRel, Blocks.STONE);
        ItemStack phial = new ItemStack(SimpleBlocks.GASEOUS_LIGHT_ITEM.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, phial);
        phial.getItem().use(level, player, InteractionHand.MAIN_HAND);
        test.assertTrue(phial.getCount() == 1 && stateAt(test, headRel).is(Blocks.STONE), "used up all the same, the stone stays");

        player.getAbilities().instabuild = true;
        phial.getItem().use(level, player, InteractionHand.MAIN_HAND);
        test.assertTrue(phial.getCount() == 1, "a creative player keeps the phial");
        finish(test, player);
    }

    public static void theGasRemoverClearsACubeOnlyWhileSneaking(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test, 3.5, 3.5);
        ServerLevel level = test.getLevel();
        ItemStack remover = new ItemStack(SimpleBlocks.GAS_REMOVER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, remover);
        Block light = SimpleBlocks.GASEOUS_LIGHT.get();
        Block nitor = SimpleBlocks.NITOR_GAS.get();
        // the player stands in the cell (3,1,3); the cube reaches 3 cells down and to the west and 2 up and to the east
        BlockPos inside = new BlockPos(0, 2, 5);
        BlockPos insideFar = new BlockPos(5, 3, 0);
        BlockPos outsideEast = new BlockPos(6, 2, 3);
        BlockPos outsideUp = new BlockPos(3, 4, 3);
        test.setBlock(inside, light);
        test.setBlock(insideFar, nitor);
        test.setBlock(outsideEast, light);
        test.setBlock(outsideUp, light);

        player.setShiftKeyDown(false);
        InteractionResultHolder<ItemStack> plain = remover.getItem().use(level, player, InteractionHand.MAIN_HAND);
        test.assertTrue(!plain.getResult().consumesAction() && stateAt(test, inside).is(light), "not sneaking: nothing happens");

        player.setShiftKeyDown(true);
        remover.getItem().use(level, player, InteractionHand.MAIN_HAND);
        test.assertTrue(stateAt(test, inside).isAir() && stateAt(test, insideFar).isAir(), "sneaking: the gas of the cube is gone, nitor gas too");
        test.assertTrue(stateAt(test, outsideEast).is(light) && stateAt(test, outsideUp).is(light), "gas outside the cube stays");
        finish(test, player);
    }

    // ---- nitor ------------------------------------------------------------------------------------------------------

    public static void nitorGasStaysOnlyWhileACarrierIsNear(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test, 3.5, 3.5);
        ServerLevel level = test.getLevel();
        Block nitor = SimpleBlocks.NITOR_GAS.get();
        BlockPos rel = new BlockPos(3, 2, 3);
        BlockPos abs = test.absolutePos(rel);

        test.setBlock(rel, nitor.defaultBlockState().setValue(BlockGas.LEVEL, 0));
        tickBlock(level, abs);
        test.assertTrue(stateAt(test, rel).isAir(), "a player without the nitor does not keep the gas");

        player.getInventory().add(new ItemStack(SimpleBlocks.BRIGHT_NITOR.get()));
        test.setBlock(rel, nitor.defaultBlockState().setValue(BlockGas.LEVEL, 0));
        tickBlock(level, abs);
        test.assertTrue(stateAt(test, rel).is(nitor), "a carrier keeps it");
        test.assertTrue(level.getBlockTicks().hasScheduledTick(abs, nitor), "and the next check is scheduled");

        // the weak gas looks one cell around, the strong one six
        BlockPos farRel = new BlockPos(3, 5, 3);
        BlockPos far = test.absolutePos(farRel);
        test.setBlock(farRel, nitor.defaultBlockState().setValue(BlockGas.LEVEL, 0));
        tickBlock(level, far);
        test.assertTrue(stateAt(test, farRel).isAir(), "weak gas, carrier four cells away: gone");
        test.setBlock(farRel, nitor.defaultBlockState().setValue(BlockGas.LEVEL, 1));
        tickBlock(level, far);
        test.assertTrue(stateAt(test, farRel).is(nitor), "strong gas, carrier four cells away: stays");
        finish(test, player);
    }

    public static void theBrightNitorMakesGasAtTheHoldersHead(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test, 3.5, 3.5);
        ServerLevel level = test.getLevel();
        Block nitor = SimpleBlocks.NITOR_GAS.get();
        BlockPos headRel = new BlockPos(3, 2, 3);

        ItemBrightNitor.placeNear(level, player, 0);
        test.assertTrue(isGas(stateAt(test, headRel), nitor, 0), "weak nitor gas above the feet");
        ItemBrightNitor.placeNear(level, player, 1);
        test.assertTrue(isGas(stateAt(test, headRel), nitor, 1), "the Kami leggings turn it strong");
        test.setBlock(headRel, Blocks.STONE);
        ItemBrightNitor.placeNear(level, player, 0);
        test.assertTrue(stateAt(test, headRel).is(Blocks.STONE), "it does not replace a block");
        test.setBlock(headRel, SimpleBlocks.GASEOUS_LIGHT.get());
        ItemBrightNitor.placeNear(level, player, 0);
        test.assertTrue(stateAt(test, headRel).is(SimpleBlocks.GASEOUS_LIGHT.get()), "nor another gas");
        test.assertTrue(((BlockNitorGas) nitor).tickRate(level) == 20, "tick rate 20 in the Overworld");
        ServerLevel nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        if (nether != null) test.assertTrue(((BlockNitorGas) nitor).tickRate(nether) == 60, "tick rate 60 in the Nether");
        finish(test, player);
    }

    // ---- the platform ----------------------------------------------------------------------------------------------

    public static void thePlatformIsSolidOnlyForWhoStandsAbove(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        BlockPos rel = new BlockPos(3, 3, 3);
        BlockPos abs = test.absolutePos(rel);
        test.setBlock(rel, SimpleBlocks.PLATFORM.get());
        BlockState platform = level.getBlockState(abs);
        ServerPlayer player = mockPlayer(test, 3.5, 6.5);
        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 1, 6));

        // the players: solid when the eyes (feet + 1.62) are above two blocks over the platform's bottom
        player.setPos(abs.getX() + 0.5, abs.getY() + 1.0, abs.getZ() + 0.5);
        test.assertTrue(isFull(platform.getCollisionShape(level, abs, CollisionContext.of(player))), "a player standing on top is held");
        player.setPos(abs.getX() + 0.5, abs.getY() + 0.3, abs.getZ() + 0.5);
        test.assertTrue(platform.getCollisionShape(level, abs, CollisionContext.of(player)).isEmpty(), "a player inside the block falls through");
        player.setPos(abs.getX() + 0.5, abs.getY() - 1.0, abs.getZ() + 0.5);
        test.assertTrue(platform.getCollisionShape(level, abs, CollisionContext.of(player)).isEmpty(), "a player below walks through");
        player.setPos(abs.getX() + 0.5, abs.getY() + 1.0, abs.getZ() + 0.5);
        player.setShiftKeyDown(true);
        test.assertTrue(platform.getCollisionShape(level, abs, CollisionContext.of(player)).isEmpty(), "a sneaking player sinks through");
        player.setShiftKeyDown(false);

        // everybody else: solid when their feet are above the bottom of the block
        pig.setPos(abs.getX() + 0.5, abs.getY() + 1.0, abs.getZ() + 0.5);
        test.assertTrue(isFull(platform.getCollisionShape(level, abs, CollisionContext.of(pig))), "a pig on top is held");
        pig.setPos(abs.getX() + 0.5, abs.getY() - 0.5, abs.getZ() + 0.5);
        test.assertTrue(platform.getCollisionShape(level, abs, CollisionContext.of(pig)).isEmpty(), "a pig below walks through");
        test.assertTrue(platform.getCollisionShape(level, abs, CollisionContext.empty()).isEmpty(), "nothing without an entity");
        test.assertTrue(!platform.isPathfindable(PathComputationType.LAND), "the path finder keeps out");
        finish(test, player);
    }

    private static boolean isFull(VoxelShape shape) {
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) == 1.0 && shape.min(Direction.Axis.Y) == 0.0;
    }

    public static void aCamouflageIsSetAndClearedByTheHand(GameTestHelper test) {
        floor(test);
        BlockPos rel = new BlockPos(3, 1, 3);
        test.setBlock(rel, SimpleBlocks.PLATFORM.get());
        ServerPlayer player = mockPlayer(test, 6.5, 6.5);
        TileCamo tile = tile(test, rel, TileCamo.class);
        test.assertTrue(tile.getCamo() == null && !stateAt(test, rel).getValue(BlockCamo.CAMOUFLAGED), "plain at first");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        test.useBlock(rel, player);
        test.assertTrue(tile.getCamo() != null && tile.getCamo().is(Blocks.STONE), "a stone block is imitated");
        test.assertTrue(stateAt(test, rel).getValue(BlockCamo.CAMOUFLAGED), "and the property says so");
        test.assertTrue(stateAt(test, rel).is(SimpleBlocks.PLATFORM.get()) && tile(test, rel, TileCamo.class) == tile, "the block and its tile stay");

        // saved and loaded
        CompoundTag saved = tile.saveWithFullMetadata(test.getLevel().registryAccess());
        TileCamo loaded = new TileCamo(test.absolutePos(rel), stateAt(test, rel));
        loaded.loadWithComponents(saved, test.getLevel().registryAccess());
        test.assertTrue(loaded.getCamo() != null && loaded.getCamo().is(Blocks.STONE), "the camouflage survives a save");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        test.useBlock(rel, player);
        test.assertTrue(tile.getCamo() != null, "an item that is no block changes nothing");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_SLAB));
        test.useBlock(rel, player);
        test.assertTrue(tile.getCamo().is(Blocks.STONE), "a block that is no full cube is refused");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SimpleBlocks.PLATFORM.get()));
        test.useBlock(rel, player);
        test.assertTrue(tile.getCamo().is(Blocks.STONE), "another camouflage block is refused");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        test.useBlock(rel, player);
        test.assertTrue(tile.getCamo() == null && !stateAt(test, rel).getValue(BlockCamo.CAMOUFLAGED), "the empty hand takes the cover off");
        finish(test, player);
    }

    // ---- slabs -----------------------------------------------------------------------------------------------------

    public static void theTravelSlabAndStairsQuickenWhoWalkOnThem(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 1, 6));
        for (Block block : List.of(SimpleBlocks.TRAVEL_SLAB.get(), SimpleBlocks.TRAVEL_STAIRS.get(), SimpleBlocks.WARD_SLAB.get())) {
            pig.removeAllEffects();
            BlockPos rel = new BlockPos(3, 1, 3);
            test.setBlock(rel, block);
            BlockPos abs = test.absolutePos(rel);
            block.stepOn(level, abs, level.getBlockState(abs), pig);
            MobEffectInstance speed = pig.getEffect(MobEffects.MOVEMENT_SPEED);
            MobEffectInstance jump = pig.getEffect(MobEffects.JUMP);
            test.assertTrue(speed != null && speed.getAmplifier() == 1 && speed.getDuration() <= 40 && speed.getDuration() > 30,
                    block + ": speed II for forty ticks, found " + speed);
            test.assertTrue(jump != null && jump.getAmplifier() == 0 && jump.getDuration() <= 40 && jump.getDuration() > 30,
                    block + ": jump boost I for forty ticks, found " + jump);
        }
        // the effects are given only for the block that was stepped on (a state of another block does nothing)
        pig.removeAllEffects();
        Block travel = SimpleBlocks.TRAVEL_SLAB.get();
        travel.stepOn(level, test.absolutePos(new BlockPos(3, 1, 3)), Blocks.STONE.defaultBlockState(), pig);
        test.assertTrue(pig.getEffect(MobEffects.MOVEMENT_SPEED) == null, "stone gives no speed");
        test.succeed();
    }

    public static void aWardSlabSealsTheTwoCellsAbove(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        BlockState aura = BlocksTC.state("blockAiry", 4);
        test.assertTrue(aura != null, "Thaumcraft provides the warding aura");
        BlockPos rel = new BlockPos(3, 1, 3);
        test.setBlock(rel, SimpleBlocks.WARD_SLAB.get());
        BlockPos abs = test.absolutePos(rel);
        TileWardedSlab tile = tile(test, rel, TileWardedSlab.class);
        test.assertTrue(!stateAt(test, rel).getValue(BlockWardSlab.POWERED), "unpowered");

        for (int i = 0; i < 100; i++) TileWardedSlab.serverTick(level, abs, level.getBlockState(abs), tile);
        test.assertTrue(stateAt(test, rel.above()) == aura, "the aura in the first cell above: " + stateAt(test, rel.above()));
        test.assertTrue(stateAt(test, rel.above(2)) == aura, "and in the second: " + stateAt(test, rel.above(2)));

        // a solid block is not replaced
        test.setBlock(rel.above(), Blocks.AIR);
        test.setBlock(rel.above(2), Blocks.STONE);
        for (int i = 0; i < 100; i++) TileWardedSlab.serverTick(level, abs, level.getBlockState(abs), tile);
        test.assertTrue(stateAt(test, rel.above()) == aura && stateAt(test, rel.above(2)).is(Blocks.STONE), "stone stays, the free cell is sealed again");
        test.succeed();
    }

    public static void aWardSlabTurnsWithTheRedstoneSignal(GameTestHelper test) {
        floor(test);
        BlockPos rel = new BlockPos(3, 1, 3);
        test.setBlock(rel, SimpleBlocks.WARD_SLAB.get());
        BlockState quiet = stateAt(test, rel);
        test.assertTrue(!quiet.getValue(BlockWardSlab.POWERED), "unpowered at first");
        test.assertTrue(quiet.isPathfindable(PathComputationType.LAND), "unpowered: the path finder walks over it, as getBlocksMovement said");

        test.setBlock(rel.east(), Blocks.REDSTONE_BLOCK);
        BlockState powered = stateAt(test, rel);
        test.assertTrue(powered.getValue(BlockWardSlab.POWERED), "a redstone block next to it powers it");
        test.assertTrue(!powered.isPathfindable(PathComputationType.LAND), "powered: the path finder keeps out");
        test.assertTrue(tile(test, rel, TileWardedSlab.class).gettingPower(), "and the tile knows");

        test.setBlock(rel.east(), Blocks.AIR);
        test.assertTrue(!stateAt(test, rel).getValue(BlockWardSlab.POWERED), "the signal is gone");
        test.succeed();
    }

    public static void aWardSlabPushesFloatingMobsAwayButNotPlayersOrPoweredOnes(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        BlockPos rel = new BlockPos(3, 1, 3);
        test.setBlock(rel, SimpleBlocks.WARD_SLAB.get());
        BlockPos abs = test.absolutePos(rel);
        TileWardedSlab tile = tile(test, rel, TileWardedSlab.class);

        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 3, 3));
        pig.setNoGravity(true);
        pig.setPos(abs.getX() + 0.5, abs.getY() + 1.6, abs.getZ() + 0.5);
        pig.setDeltaMovement(Vec3.ZERO);
        ServerPlayer player = mockPlayer(test, 3.5, 3.5);
        player.setPos(abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5);
        player.setDeltaMovement(Vec3.ZERO);
        Vec3 playerBefore = player.getDeltaMovement();

        // within a hundred ticks the count passes a multiple of five
        for (int i = 0; i < 100; i++) TileWardedSlab.serverTick(level, abs, level.getBlockState(abs), tile);
        Vec3 pushed = pig.getDeltaMovement();
        test.assertTrue(pushed.y < 0 && (Math.abs(pushed.x) > 1.0E-6 || Math.abs(pushed.z) > 1.0E-6), "the pig was pushed: " + pushed);
        test.assertTrue(player.getDeltaMovement().equals(playerBefore), "the player was not");

        // with a signal nothing is pushed
        test.setBlock(rel.east(), Blocks.REDSTONE_BLOCK);
        pig.setDeltaMovement(Vec3.ZERO);
        for (int i = 0; i < 100; i++) TileWardedSlab.serverTick(level, abs, level.getBlockState(abs), tile);
        test.assertTrue(pig.getDeltaMovement().equals(Vec3.ZERO), "a powered slab pushes nobody: " + pig.getDeltaMovement());
        finish(test, player);
    }

    // ---- the forcefield --------------------------------------------------------------------------------------------

    public static void aForcefieldIsSolidForThreeSecondsAndThenGone(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        BlockPos rel = new BlockPos(3, 1, 3);
        test.setBlock(rel, SimpleBlocks.FORCEFIELD.get());
        BlockPos abs = test.absolutePos(rel);
        BlockState field = stateAt(test, rel);
        TileForcefield tile = tile(test, rel, TileForcefield.class);

        test.assertTrue(isFull(field.getCollisionShape(level, abs, CollisionContext.empty())), "solid");
        test.assertTrue(field.canBeReplaced(), "replaceable");
        test.assertTrue(field.getRenderShape() == net.minecraft.world.level.block.RenderShape.INVISIBLE, "invisible");

        for (int i = 0; i < 61; i++) TileForcefield.tick(level, abs, level.getBlockState(abs), tile);
        test.assertTrue(stateAt(test, rel).is(SimpleBlocks.FORCEFIELD.get()), "still there after 61 ticks");
        TileForcefield.tick(level, abs, level.getBlockState(abs), tile);
        test.assertTrue(stateAt(test, rel).isAir(), "gone on the 62nd");
        test.succeed();
    }

    // ---- infused soil and crops ------------------------------------------------------------------------------------

    private static UseOnContext onTop(GameTestHelper test, ServerPlayer player, ItemStack stack, BlockPos soilRel, Direction face) {
        BlockPos soil = test.absolutePos(soilRel);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(soil).add(0, 0.5, 0), face, soil, false);
        return new UseOnContext(test.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
    }

    public static void seedsPlantOnFarmlandAndInfuseTheSoil(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test, 6.5, 6.5);
        BlockPos soil = new BlockPos(3, 1, 3);
        test.setBlock(soil, Blocks.FARMLAND);
        ItemStack seeds = ItemInfusedSeeds.getStackFromAspect(Aspect.FIRE);
        ItemInfusedSeeds.setAspectTendencies(seeds, new AspectList().add(Aspect.WATER, 3).add(Aspect.ORDER, 2));
        seeds.setCount(3);
        player.setItemInHand(InteractionHand.MAIN_HAND, seeds);

        InteractionResult result = seeds.getItem().useOn(onTop(test, player, seeds, soil, Direction.UP));
        test.assertTrue(result.consumesAction(), "planted");
        test.assertTrue(stateAt(test, soil).is(SimpleBlocks.INFUSED_FARMLAND.get()), "the soil is infused now");
        BlockState crop = stateAt(test, soil.above());
        test.assertTrue(crop.is(SimpleBlocks.INFUSED_GRAIN.get()) && crop.getValue(CropBlock.AGE) == 0, "a young crop above: " + crop);
        test.assertTrue(crop.getValue(BlockInfusedGrain.ASPECT_TEXTURE) == 1, "with the picture of ignis");
        TileInfusedGrain tile = tile(test, soil.above(), TileInfusedGrain.class);
        test.assertTrue(tile.aspect == Aspect.FIRE, "the aspect of the seeds");
        test.assertTrue(tile.primalTendencies.getAmount(Aspect.WATER) == 3 && tile.primalTendencies.getAmount(Aspect.ORDER) == 2,
                "and their tendencies");
        test.assertTrue(seeds.getCount() == 2, "one seed used up");
        tile(test, soil, TileInfusedFarmland.class);

        // the infused soil takes the next seeds as well, and keeps its residue
        TileInfusedFarmland farmland = tile(test, soil, TileInfusedFarmland.class);
        farmland.aspectList.add(Aspect.EARTH, 4);
        test.setBlock(soil.above(), Blocks.AIR);
        seeds.getItem().useOn(onTop(test, player, seeds, soil, Direction.UP));
        test.assertTrue(stateAt(test, soil.above()).is(SimpleBlocks.INFUSED_GRAIN.get()) && seeds.getCount() == 1, "planted again");
        test.assertTrue(tile(test, soil, TileInfusedFarmland.class) == farmland && farmland.aspectList.getAmount(Aspect.EARTH) == 4,
                "the residue of the soil is untouched");
        finish(test, player);
    }

    public static void seedsRefuseWhatIsNotFarmlandOrFreeSpace(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test, 6.5, 6.5);
        ItemStack seeds = ItemInfusedSeeds.getStackFromAspect(Aspect.AIR);
        seeds.setCount(4);
        player.setItemInHand(InteractionHand.MAIN_HAND, seeds);

        BlockPos dirt = new BlockPos(1, 1, 1);
        test.setBlock(dirt, Blocks.DIRT);
        test.assertTrue(!seeds.getItem().useOn(onTop(test, player, seeds, dirt, Direction.UP)).consumesAction(), "not on dirt");

        BlockPos blocked = new BlockPos(3, 1, 3);
        test.setBlock(blocked, Blocks.FARMLAND);
        test.setBlock(blocked.above(), Blocks.STONE);
        test.assertTrue(!seeds.getItem().useOn(onTop(test, player, seeds, blocked, Direction.UP)).consumesAction(), "not into a stone block");
        test.assertTrue(stateAt(test, blocked).is(Blocks.FARMLAND), "and the soil is not changed then");

        BlockPos side = new BlockPos(5, 1, 5);
        test.setBlock(side, Blocks.FARMLAND);
        test.assertTrue(!seeds.getItem().useOn(onTop(test, player, seeds, side, Direction.NORTH)).consumesAction(), "not from the side");
        test.assertTrue(seeds.getCount() == 4, "no seed used up");
        finish(test, player);
    }

    private static BlockPos plantRipeCrop(GameTestHelper test, Aspect aspect, int age) {
        BlockPos soil = new BlockPos(3, 1, 3);
        test.setBlock(soil, SimpleBlocks.INFUSED_FARMLAND.get());
        BlockInfusedGrain grain = SimpleBlocks.INFUSED_GRAIN.get();
        test.setBlock(soil.above(), grain.stateFor(aspect).setValue(CropBlock.AGE, age));
        tile(test, soil.above(), TileInfusedGrain.class).aspect = aspect;
        return soil.above();
    }

    public static void aRipeCropYieldsSeedsLootAndLeavesResidue(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        BlockPos cropRel = plantRipeCrop(test, Aspect.AIR, 7);
        BlockPos crop = test.absolutePos(cropRel);
        BlockInfusedGrain grain = SimpleBlocks.INFUSED_GRAIN.get();
        TileInfusedGrain tile = tile(test, cropRel, TileInfusedGrain.class);
        tile.primalTendencies.add(Aspect.WATER, 4);
        TileInfusedFarmland farmland = tile(test, cropRel.below(), TileInfusedFarmland.class);

        List<ItemStack> drops = Block.getDrops(level.getBlockState(crop), level, crop, tile);
        test.assertTrue(drops.size() >= 2, "seeds and at least one loot stack: " + drops);
        ItemStack seeds = drops.get(0);
        test.assertTrue(seeds.is(ItemInfusedSeeds.item()) && ItemInfusedSeeds.getAspect(seeds) == Aspect.AIR, "seeds of the crop's aspect: " + seeds);
        test.assertTrue(ItemInfusedSeeds.getAspectTendencies(seeds).getAmount(Aspect.WATER) == 4, "they carry the tendencies");
        test.assertTrue(!drops.get(1).isEmpty(), "loot: " + drops.get(1));
        test.assertTrue(farmland.aspectList.getAmount(Aspect.AIR) >= 1, "the ripe crop left a point of its aspect in the soil");
        test.assertTrue(farmland.aspectList.getAmount(Aspect.AIR) <= TileInfusedFarmland.MAX_ASPECTS, "within the cap");

        // an unripe crop gives only seeds and leaves nothing
        farmland.aspectList = new AspectList();
        test.setBlock(cropRel, grain.stateFor(Aspect.AIR).setValue(CropBlock.AGE, 4));
        tile(test, cropRel, TileInfusedGrain.class).aspect = Aspect.AIR;
        drops = Block.getDrops(level.getBlockState(crop), level, crop, level.getBlockEntity(crop));
        test.assertTrue(drops.size() == 1 && drops.get(0).is(ItemInfusedSeeds.item()), "an unripe crop: seeds only: " + drops);
        test.assertTrue(farmland.aspectList.getAmount(Aspect.AIR) == 0, "and no residue");
        test.succeed();
    }

    public static void residueInTheSoilBreedsANewAspect(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        BlockPos cropRel = plantRipeCrop(test, Aspect.AIR, 7);
        BlockPos crop = test.absolutePos(cropRel);
        BlockInfusedGrain grain = SimpleBlocks.INFUSED_GRAIN.get();
        TileInfusedFarmland farmland = tile(test, cropRel.below(), TileInfusedFarmland.class);
        TileInfusedGrain tile = tile(test, cropRel, TileInfusedGrain.class);

        // five points of perditio in the soil: the chance is 5 * 5 = 25 against a roll of 0 to 9, so it always happens
        farmland.aspectList.add(Aspect.ENTROPY, 5);
        Aspect bred = grain.getAspectDropped(level, crop, 7, tile, level.random);
        test.assertTrue(bred == Aspect.VOID, "aer and perditio make vacuos: " + (bred == null ? null : bred.getTag()));
        // an unripe crop never breeds
        test.assertTrue(grain.getAspectDropped(level, crop, 6, tile, level.random) == Aspect.AIR, "unripe: the crop's own aspect");
        // a soil with other residue and no combination keeps the aspect
        farmland.aspectList = new AspectList().add(Aspect.AIR, 5);
        test.assertTrue(grain.getAspectDropped(level, crop, 7, tile, level.random) == Aspect.AIR, "no combination of aer with itself");
        test.succeed();
    }

    public static void aCropGrowsInItsTileAndNotByRandomTicks(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        BlockPos cropRel = plantRipeCrop(test, Aspect.WATER, 0);
        BlockPos crop = test.absolutePos(cropRel);
        TileInfusedGrain tile = tile(test, cropRel, TileInfusedGrain.class);
        // fifty points of aqua make the chance of a step 1 in 60 per tick instead of 1 in 15060
        tile.primalTendencies.add(Aspect.WATER, 50);
        // light: a glowstone two cells away, and the light engine needs a few ticks
        test.setBlock(new BlockPos(5, 1, 3), Blocks.GLOWSTONE);

        test.runAfterDelay(10, () -> {
            for (int i = 0; i < 2000; i++) {
                BlockState state = level.getBlockState(crop);
                test.assertTrue(state.is(SimpleBlocks.INFUSED_GRAIN.get()), "the crop is still there");
                state.randomTick(level, crop, level.random);
            }
            test.assertTrue(level.getBlockState(crop).getValue(CropBlock.AGE) == 0, "random ticks do not grow the crop");

            int ticks = 0;
            while (ticks < 40000 && level.getBlockState(crop).getValue(CropBlock.AGE) < 7) {
                TileInfusedGrain.serverTick(level, crop, level.getBlockState(crop), tile);
                ticks++;
            }
            BlockState grown = level.getBlockState(crop);
            test.assertTrue(grown.getValue(CropBlock.AGE) == 7, "ripe after " + ticks + " ticks of the tile");
            test.assertTrue(grown.getValue(BlockInfusedGrain.ASPECT_TEXTURE) == 2, "the picture of aqua is kept");
            test.assertTrue(level.getBlockEntity(crop) == tile, "and the tile is the same");
            test.succeed();
        });
    }

    public static void bonemealGrowsTheCropOnlyWhenTheConfigurationAllowsAndKeepsItsPicture(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        BlockPos cropRel = plantRipeCrop(test, Aspect.EARTH, 0);
        BlockPos crop = test.absolutePos(cropRel);
        BlockInfusedGrain grain = SimpleBlocks.INFUSED_GRAIN.get();
        BlockState state = level.getBlockState(crop);
        test.assertTrue(grain.isValidBonemealTarget(level, crop, state) == ConfigHandler.cropsAllowBonemeal, "bone meal follows the switch");
        grain.growCrops(level, crop, state);
        BlockState grown = level.getBlockState(crop);
        test.assertTrue(grown.getValue(CropBlock.AGE) >= 2 && grown.getValue(CropBlock.AGE) <= 5, "two to five stages: " + grown);
        test.assertTrue(grown.getValue(BlockInfusedGrain.ASPECT_TEXTURE) == 3, "the picture of terra is kept");
        test.setBlock(cropRel, state.setValue(CropBlock.AGE, 7));
        test.assertTrue(!grain.isValidBonemealTarget(level, crop, level.getBlockState(crop)), "a ripe crop takes no bone meal");
        test.succeed();
    }

    public static void aCropNeedsFarmland(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        BlockInfusedGrain grain = SimpleBlocks.INFUSED_GRAIN.get();
        BlockPos rel = new BlockPos(3, 1, 3);
        BlockPos above = test.absolutePos(rel.above());
        BlockState crop = grain.stateFor(Aspect.AIR);
        test.setBlock(rel, Blocks.DIRT);
        test.assertTrue(!crop.canSurvive(level, above), "not on dirt");
        test.setBlock(rel, Blocks.FARMLAND);
        test.assertTrue(crop.canSurvive(level, above), "on farmland");
        test.setBlock(rel, SimpleBlocks.INFUSED_FARMLAND.get());
        test.assertTrue(crop.canSurvive(level, above), "on infused farmland");
        // and nothing else grows on the infused soil
        test.assertTrue(!Blocks.WHEAT.defaultBlockState().canSurvive(level, above), "wheat does not");
        test.assertTrue(!Blocks.OAK_SAPLING.defaultBlockState().canSurvive(level, above), "a sapling does not");
        test.succeed();
    }

    public static void infusedSoilIsNeverTrampledOrDriedAndTurnsToDirtUnderStone(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        BlockPos rel = new BlockPos(3, 1, 3);
        BlockPos abs = test.absolutePos(rel);
        test.setBlock(rel, SimpleBlocks.INFUSED_FARMLAND.get());
        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 1, 6));
        for (int i = 0; i < 40; i++) SimpleBlocks.INFUSED_FARMLAND.get().fallOn(level, level.getBlockState(abs), abs, pig, 10.0F);
        test.assertTrue(stateAt(test, rel).is(SimpleBlocks.INFUSED_FARMLAND.get()), "not trampled");
        for (int i = 0; i < 2000; i++) level.getBlockState(abs).randomTick(level, abs, level.random);
        test.assertTrue(stateAt(test, rel).is(SimpleBlocks.INFUSED_FARMLAND.get()), "not dried");
        test.assertTrue(isFull(level.getBlockState(abs).getCollisionShape(level, abs)), "a full block");

        test.setBlock(rel.above(), Blocks.STONE);
        test.runAfterDelay(5, () -> {
            test.assertTrue(stateAt(test, rel).is(Blocks.DIRT), "a solid block on top turns it into dirt, as farmland did: " + stateAt(test, rel));
            test.succeed();
        });
    }

    public static void theSoilAndTheCropRememberTheirAspects(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        BlockPos cropRel = plantRipeCrop(test, Aspect.MAGIC, 3);
        TileInfusedGrain tile = tile(test, cropRel, TileInfusedGrain.class);
        tile.primalTendencies.add(Aspect.AIR, 3).add(Aspect.ORDER, 7);
        TileInfusedFarmland farmland = tile(test, cropRel.below(), TileInfusedFarmland.class);
        farmland.aspectList.add(Aspect.FIRE, 4).add(Aspect.WATER, 2);

        CompoundTag savedCrop = tile.saveWithFullMetadata(level.registryAccess());
        TileInfusedGrain loadedCrop = new TileInfusedGrain(test.absolutePos(cropRel), stateAt(test, cropRel));
        loadedCrop.loadWithComponents(savedCrop, level.registryAccess());
        test.assertTrue(loadedCrop.aspect == Aspect.MAGIC, "the crop's aspect");
        test.assertTrue(loadedCrop.primalTendencies.getAmount(Aspect.AIR) == 3 && loadedCrop.primalTendencies.getAmount(Aspect.ORDER) == 7,
                "and its tendencies");

        CompoundTag savedSoil = farmland.saveWithFullMetadata(level.registryAccess());
        TileInfusedFarmland loadedSoil = new TileInfusedFarmland(test.absolutePos(cropRel.below()), stateAt(test, cropRel.below()));
        loadedSoil.loadWithComponents(savedSoil, level.registryAccess());
        test.assertTrue(loadedSoil.aspectList.getAmount(Aspect.FIRE) == 4 && loadedSoil.aspectList.getAmount(Aspect.WATER) == 2, "the residue");

        // a crop that never got an aspect saves and loads without one
        TileInfusedGrain bare = new TileInfusedGrain(test.absolutePos(cropRel), stateAt(test, cropRel));
        TileInfusedGrain loadedBare = new TileInfusedGrain(test.absolutePos(cropRel), stateAt(test, cropRel));
        loadedBare.aspect = Aspect.FIRE;
        loadedBare.loadWithComponents(bare.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
        test.assertTrue(loadedBare.aspect == null, "an empty aspect stays empty");
        test.succeed();
    }

    public static void theSoilAndTheCropHoldAtMostTheirCaps(GameTestHelper test) {
        floor(test);
        BlockPos cropRel = plantRipeCrop(test, Aspect.AIR, 0);
        TileInfusedGrain tile = tile(test, cropRel, TileInfusedGrain.class);
        tile.primalTendencies.add(Aspect.AIR, 30).add(Aspect.WATER, 30).add(Aspect.FIRE, 20);
        tile.reduceSaturatedAspects();
        test.assertTrue(sum(tile.primalTendencies) == 50, "tendencies: fifty at most, found " + sum(tile.primalTendencies));

        TileInfusedFarmland farmland = tile(test, cropRel.below(), TileInfusedFarmland.class);
        farmland.aspectList.add(Aspect.EARTH, 14).add(Aspect.ORDER, 9).add(Aspect.ENTROPY, 7);
        farmland.reduceSaturatedAspects();
        test.assertTrue(sum(farmland.aspectList) == TileInfusedFarmland.MAX_ASPECTS, "residue: twenty at most, found " + sum(farmland.aspectList));
        farmland.aspectList.add(Aspect.EARTH, 1);
        farmland.reduceSaturatedAspects();
        test.assertTrue(sum(farmland.aspectList) == TileInfusedFarmland.MAX_ASPECTS, "still twenty");
        test.succeed();
    }

    private static int sum(AspectList list) {
        int total = 0;
        for (Aspect aspect : list.getAspects()) if (aspect != null) total += list.getAmount(aspect);
        return total;
    }

    public static void theGrainAndPotionItemsCarryTheirAspects(GameTestHelper test) {
        Aspect[] aspects = { Aspect.AIR, Aspect.FIRE, Aspect.EARTH, Aspect.WATER };
        for (int i = 0; i < 4; i++) {
            ItemStack grain = new ItemStack(BuiltInRegistries.ITEM.get(LibMisc.id(ItemInfusedGrain.path(i))));
            ItemStack potion = new ItemStack(BuiltInRegistries.ITEM.get(LibMisc.id(ItemInfusedPotion.path(i))));
            test.assertTrue(((ItemInfusedGrain) grain.getItem()).getAspect(grain) == aspects[i], "grain " + i);
            test.assertTrue(((ItemInfusedPotion) potion.getItem()).getAspect(potion) == aspects[i], "potion " + i);
            test.assertTrue(ItemInfusedGrain.stackFor(aspects[i]).is(grain.getItem()), "stackFor " + i);
            test.assertTrue(potion.getMaxStackSize() == 1, "a potion does not stack");
        }
        test.assertTrue(ItemInfusedSeeds.getStackFromAspect(Aspect.ENTROPY).getItem() == ItemInfusedSeeds.item(), "one item for all seeds");
        ItemStack bare = new ItemStack(ItemInfusedSeeds.item());
        test.assertTrue(ItemInfusedSeeds.getAspect(bare) == null, "bare seeds have no aspect");
        ItemInfusedSeeds.setAspect(bare, Aspect.ORDER);
        test.assertTrue(ItemInfusedSeeds.getAspect(bare) == Aspect.ORDER, "and take one");
        test.succeed();
    }

    // ---- the potions -----------------------------------------------------------------------------------------------

    public static void theFourEffectsAreBadEffectsWithoutAnyPowerOfTheirOwn(GameTestHelper test) {
        for (int i = 0; i < 4; i++) {
            net.minecraft.world.effect.MobEffect effect = ModPotions.holder(i).value();
            test.assertTrue(effect.getCategory() == MobEffectCategory.HARMFUL, ModPotions.PATHS[i] + " is harmful");
            test.assertTrue(effect.getColor() == 0, ModPotions.PATHS[i] + " has colour zero");
            test.assertTrue(effect.getDescriptionId().equals("effect." + LibMisc.MOD_ID + "." + ModPotions.PATHS[i]), "name key " + effect.getDescriptionId());
        }
        test.succeed();
    }

    public static void drinkingAPotionGivesThreeMinutesOfItsEffect(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test, 3.5, 3.5);
        ServerLevel level = test.getLevel();
        for (int i = 0; i < 4; i++) {
            ItemStack potion = new ItemStack(BuiltInRegistries.ITEM.get(LibMisc.id(ItemInfusedPotion.path(i))));
            player.setItemInHand(InteractionHand.MAIN_HAND, potion);
            ItemStack returned = potion.getItem().finishUsingItem(potion, level, player);
            MobEffectInstance effect = player.getEffect(ModPotions.holder(i));
            test.assertTrue(effect != null && effect.getDuration() == 3600, "effect " + i + " for 3600 ticks: " + effect);
            test.assertTrue(returned.is(Items.GLASS_BOTTLE), "an empty bottle is left: " + returned);
        }
        finish(test, player);
    }

    public static void anAirHitFlingsTheVictimForAWhile(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test, 2.5, 3.5);
        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 3));
        PotionEffectHandler.clear();
        PotionEffectHandler.onPlayerHit(player, pig);
        test.assertTrue(PotionEffectHandler.airHits() == 0 && PotionEffectHandler.fireHits() == 0, "no effect, no victim");
        player.addEffect(new MobEffectInstance(ModPotions.air(), 400));
        PotionEffectHandler.onPlayerHit(player, pig);
        test.assertTrue(PotionEffectHandler.airHits() == 1 && PotionEffectHandler.fireHits() == 0, "the victim is remembered");
        test.runAfterDelay(30, () -> {
            test.assertTrue(PotionEffectHandler.airHits() == 0, "and forgotten after a second");
            PotionEffectHandler.clear();
            finish(test, player);
        });
    }

    public static void aFireHitSetsTheVictimAlight(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test, 2.5, 3.5);
        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 3));
        PotionEffectHandler.clear();
        player.addEffect(new MobEffectInstance(ModPotions.fire(), 400));
        PotionEffectHandler.onPlayerHit(player, pig);
        test.assertTrue(PotionEffectHandler.fireHits() == 1, "the victim is remembered");
        test.runAfterDelay(12, () -> {
            test.assertTrue(pig.isOnFire(), "set on fire within a few ticks");
            test.assertTrue(PotionEffectHandler.fireHits() == 1, "and still remembered");
            PotionEffectHandler.clear();
            pig.clearFire();
            finish(test, player);
        });
    }

    public static void anEarthHitRaisesAWallHalfWayBetween(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        ServerPlayer player = mockPlayer(test, 2.5, 3.5);
        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 3));
        pig.setPos(test.absoluteVec(new Vec3(4.5, 1.0, 3.5)));
        player.addEffect(new MobEffectInstance(ModPotions.earth(), 400));
        PotionEffectHandler.onPlayerHit(player, pig);

        // along the x axis: the plane x = 3, from two cells under to two over the player's head (feet + 2)
        int walls = 0;
        for (int y = 1; y <= 5; y++) {
            for (int z = 1; z <= 5; z++) {
                if (stateAt(test, new BlockPos(3, y, z)).is(SimpleBlocks.FORCEFIELD.get())) walls++;
            }
        }
        test.assertTrue(walls == 25, "a wall of 5 by 5 forcefield cells, found " + walls);
        test.assertTrue(stateAt(test, new BlockPos(2, 2, 3)).isAir() && stateAt(test, new BlockPos(4, 2, 3)).isAir(), "only the one plane");
        // an occupied cell is not touched
        test.setBlock(new BlockPos(3, 3, 3), Blocks.AIR);
        test.setBlock(new BlockPos(3, 2, 2), Blocks.STONE);
        PotionEffectHandler.onPlayerHit(player, pig);
        test.assertTrue(stateAt(test, new BlockPos(3, 2, 2)).is(Blocks.STONE), "stone stays");
        test.assertTrue(stateAt(test, new BlockPos(3, 3, 3)).is(SimpleBlocks.FORCEFIELD.get()), "a free cell is filled again");
        finish(test, player);
    }

    public static void aWaterImbuedPlayerTurnsNearbyLavaIntoObsidian(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test, 3.5, 3.5);
        player.addEffect(new MobEffectInstance(ModPotions.water(), 400));
        BlockPos near = new BlockPos(2, 1, 2);
        BlockPos far = new BlockPos(7, 1, 7);
        test.setBlock(near, Blocks.LAVA);
        test.setBlock(far, Blocks.LAVA);
        test.runAfterDelay(6, () -> {
            test.assertTrue(stateAt(test, near).is(Blocks.OBSIDIAN), "lava two cells away is obsidian: " + stateAt(test, near));
            test.assertTrue(stateAt(test, far).is(Blocks.LAVA), "lava far away is lava");
            finish(test, player);
        });
    }

    // ---- the elemental fires ---------------------------------------------------------------------------------------

    private static boolean fireMechanicsOn() {
        return ConfigHandler.enableFire && ConfigHandler.enableFireMechanics;
    }

    public static void theFiresTransmuteTheBlocksOfTheirTables(GameTestHelper test) {
        BlockFireBase air = SimpleBlocks.FIRE_AIR.get();
        BlockFireBase water = SimpleBlocks.FIRE_WATER.get();
        BlockFireBase earth = SimpleBlocks.FIRE_EARTH.get();
        BlockFireBase ignis = SimpleBlocks.FIRE_FIRE.get();
        BlockFireBase order = SimpleBlocks.FIRE_ORDER.get();
        BlockFireBase chaos = SimpleBlocks.FIRE_CHAOS.get();

        test.assertTrue(result(air, Blocks.OAK_LOG) == Blocks.SAND && result(air, Blocks.BIRCH_LOG) == Blocks.SAND, "aer: logs to sand");
        test.assertTrue(result(air, Blocks.OAK_LEAVES) == Blocks.SANDSTONE, "aer: leaves to sandstone");
        test.assertTrue(result(air, Blocks.ICE) == Blocks.GLASS, "aer: ice to glass");
        test.assertTrue(result(air, Blocks.DIRT) == Blocks.SAND && result(air, Blocks.GRASS_BLOCK) == Blocks.SAND, "aer: dirt and grass to sand");
        test.assertTrue(result(air, Blocks.WATER) == (ConfigHandler.enableCake ? Blocks.CAKE : null), "aer: water to cake, by the switch");
        test.assertTrue(result(air, Blocks.STONE) == null, "aer: stone is none of its business");

        test.assertTrue(result(water, Blocks.SAND) == Blocks.ICE && result(water, Blocks.RED_SAND) == Blocks.ICE, "aqua: sand to ice");
        test.assertTrue(result(water, Blocks.NETHERRACK) == Blocks.SNOW_BLOCK, "aqua: netherrack to snow");
        test.assertTrue(result(water, Blocks.LAVA) == Blocks.OBSIDIAN, "aqua: lava to obsidian");

        test.assertTrue(result(earth, Blocks.SAND) == Blocks.DIRT && result(earth, Blocks.GRAVEL) == Blocks.CLAY, "terra: sand to dirt, gravel to clay");
        test.assertTrue(result(earth, Blocks.NETHER_BRICKS) == Blocks.OAK_PLANKS && result(earth, Blocks.CACTUS) == Blocks.OAK_LOG, "terra: nether bricks, cactus");
        test.assertTrue(result(earth, Blocks.SNOW) == Blocks.DEAD_BUSH, "terra: a snow layer to the shrub");
        test.assertTrue(result(earth, Blocks.SPAWNER) == Blocks.IRON_BLOCK, "terra: a spawner to iron");
        test.assertTrue(result(earth, Blocks.STONE) == Blocks.DIRT && result(earth, Blocks.COBBLESTONE) == Blocks.DIRT && result(earth, Blocks.GLASS) == Blocks.DIRT,
                "terra: stone, cobblestone and glass to dirt");
        test.assertTrue(result(earth, Blocks.SPRUCE_PLANKS) == Blocks.DIRT && result(earth, Blocks.ACACIA_LEAVES) == Blocks.DIRT, "terra: every plank and leaf");

        test.assertTrue(result(ignis, Blocks.GRASS_BLOCK) == Blocks.NETHERRACK && result(ignis, Blocks.DIRT) == Blocks.NETHERRACK, "ignis: grass and dirt to netherrack");
        test.assertTrue(result(ignis, Blocks.SAND) == Blocks.SOUL_SAND && result(ignis, Blocks.GRAVEL) == Blocks.SOUL_SAND, "ignis: sand and gravel to soul sand");
        test.assertTrue(result(ignis, Blocks.CLAY) == Blocks.GLOWSTONE, "ignis: clay to glowstone");
        test.assertTrue(result(ignis, Blocks.COAL_ORE) == Blocks.NETHER_QUARTZ_ORE && result(ignis, Blocks.DEEPSLATE_DIAMOND_ORE) == Blocks.NETHER_QUARTZ_ORE
                && result(ignis, Blocks.GOLD_BLOCK) == Blocks.NETHER_QUARTZ_ORE, "ignis: ores (and the gold block, as in 1.7.10) to quartz ore");
        test.assertTrue(result(ignis, Blocks.WHEAT) == Blocks.NETHER_WART && result(ignis, Blocks.POPPY) == Blocks.BROWN_MUSHROOM, "ignis: wheat to wart, flowers to mushrooms");
        test.assertTrue(result(ignis, Blocks.DANDELION) == Blocks.DANDELION, "ignis: the dandelion to itself (sic)");
        test.assertTrue(result(ignis, Blocks.WATER) == Blocks.LAVA, "ignis: water to lava");

        test.assertTrue(order.getBlockTransformation().isEmpty(), "ordo: nothing, as in 1.7.10");
        test.assertTrue(result(chaos, air.defaultBlockState()) == Blocks.FIRE && result(chaos, earth.defaultBlockState()) == Blocks.FIRE
                && result(chaos, order.defaultBlockState()) == Blocks.FIRE, "perditio: the imbued fires to plain fire");
        test.assertTrue(result(chaos, chaos.defaultBlockState()) == null, "perditio: not itself");
        test.assertTrue(chaos.tickRate(test.getLevel()) == 1 && air.tickRate(test.getLevel()) == (ConfigHandler.enableFireMechanics ? 200 : 0x7ffffff0),
                "tick rates");
        test.succeed();
    }

    private static Block result(BlockFireBase fire, Block from) {
        return result(fire, from.defaultBlockState());
    }

    private static Block result(BlockFireBase fire, BlockState from) {
        for (var rule : fire.getBlockTransformation()) {
            if (rule.matches(from)) return rule.result(from).getBlock();
        }
        return null;
    }

    public static void theFiresKnowTheFlammabilityOfTheOriginal(GameTestHelper test) {
        BlockFireBase air = SimpleBlocks.FIRE_AIR.get();
        ServerLevel level = test.getLevel();
        BlockPos rel = new BlockPos(3, 1, 3);
        BlockPos abs = test.absolutePos(rel);
        floor(test);
        test.setBlock(rel, Blocks.DIRT);
        test.assertTrue(air.getBlockFlammability(level, abs) == 100, "a block of the table burns surely");
        test.setBlock(rel, Blocks.SAND);
        test.assertTrue(air.getBlockFlammability(level, abs) == 0, "a result of the table never burns");
        test.setBlock(rel, Blocks.OAK_PLANKS);
        test.assertTrue(air.getBlockFlammability(level, abs) == 20, "planks: 20 as in vanilla 1.7.10");
        test.setBlock(rel, Blocks.SPRUCE_LOG);
        test.assertTrue(air.getBlockFlammability(level, abs) == 100, "a log is in the table of aer");
        test.setBlock(rel, Blocks.TNT);
        test.assertTrue(air.getBlockFlammability(level, abs) == 100, "tnt: 100");
        test.setBlock(rel, Blocks.WHITE_WOOL);
        test.assertTrue(air.getBlockFlammability(level, abs) == 60, "wool: 60");
        test.setBlock(rel, Blocks.STONE);
        test.assertTrue(air.getBlockFlammability(level, abs) == 0, "stone: 0");
        test.succeed();
    }

    public static void anAerFireTurnsTheDirtNextToItIntoSand(GameTestHelper test) {
        floor(test);
        if (!fireMechanicsOn()) {
            test.succeed();
            return;
        }
        ServerLevel level = test.getLevel();
        level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(true, level.getServer());
        level.setWeatherParameters(6000, 0, false, false);
        BlockPos rel = new BlockPos(3, 1, 3);
        BlockPos abs = test.absolutePos(rel);
        test.setBlock(rel.east(), Blocks.DIRT);
        test.setBlock(rel, SimpleBlocks.FIRE_AIR.get().defaultBlockState().setValue(FireBlock.AGE, 0));
        test.assertTrue(stateAt(test, rel).is(SimpleBlocks.FIRE_AIR.get()), "the fire burns on stone");

        int ticks = 0;
        while (ticks < 3000 && !stateAt(test, rel.east()).is(Blocks.SAND)) {
            if (!level.getBlockState(abs).is(SimpleBlocks.FIRE_AIR.get())) break;
            tickBlock(level, abs);
            ticks++;
        }
        test.assertTrue(stateAt(test, rel.east()).is(Blocks.SAND), "the dirt turned to sand after " + ticks + " ticks");
        test.assertTrue(stateAt(test, rel).is(SimpleBlocks.FIRE_AIR.get()), "a source never burns out");
        test.succeed();
    }

    public static void anImbuedFireBurnsOutWithoutFuelUnlessItIsASource(GameTestHelper test) {
        floor(test);
        if (!fireMechanicsOn()) {
            test.succeed();
            return;
        }
        ServerLevel level = test.getLevel();
        level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(true, level.getServer());
        level.setWeatherParameters(6000, 0, false, false);
        BlockPos source = new BlockPos(1, 1, 1);
        BlockPos young = new BlockPos(5, 1, 5);
        // (the Aer fire: the stone floor is no fuel for it, as it would be for the Terra fire)
        test.setBlock(source, SimpleBlocks.FIRE_AIR.get().defaultBlockState().setValue(FireBlock.AGE, 0));
        test.setBlock(young, SimpleBlocks.FIRE_AIR.get().defaultBlockState().setValue(FireBlock.AGE, 3));
        tickBlock(level, test.absolutePos(source));
        tickBlock(level, test.absolutePos(young));
        test.assertTrue(stateAt(test, source).is(SimpleBlocks.FIRE_AIR.get()), "the source stays");
        test.assertTrue(stateAt(test, young).isAir(), "a fire of age 3 with nothing to transmute next to it goes out");
        test.succeed();
    }

    public static void aChaosFireTurnsImbuedFireIntoPlainFire(GameTestHelper test) {
        floor(test);
        if (!fireMechanicsOn()) {
            test.succeed();
            return;
        }
        ServerLevel level = test.getLevel();
        level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(true, level.getServer());
        level.setWeatherParameters(6000, 0, false, false);
        BlockPos rel = new BlockPos(3, 1, 3);
        BlockPos abs = test.absolutePos(rel);
        test.setBlock(rel.west(), SimpleBlocks.FIRE_WATER.get().defaultBlockState().setValue(FireBlock.AGE, 0));
        test.setBlock(rel, SimpleBlocks.FIRE_CHAOS.get().defaultBlockState().setValue(FireBlock.AGE, 0));
        int ticks = 0;
        while (ticks < 3000 && !stateAt(test, rel.west()).is(Blocks.FIRE)) {
            if (!level.getBlockState(abs).is(SimpleBlocks.FIRE_CHAOS.get())) break;
            tickBlock(level, abs);
            ticks++;
        }
        test.assertTrue(stateAt(test, rel.west()).is(Blocks.FIRE), "the aqua fire became plain fire after " + ticks + " ticks: " + stateAt(test, rel.west()));
        test.succeed();
    }

    public static void anImbuedFireIsAFireForTheGame(GameTestHelper test) {
        floor(test);
        BlockPos rel = new BlockPos(3, 1, 3);
        test.setBlock(rel, SimpleBlocks.FIRE_FIRE.get().defaultBlockState().setValue(FireBlock.AGE, 0));
        BlockState fire = stateAt(test, rel);
        test.assertTrue(fire.getLightEmission() == 15, "it shines");
        test.assertTrue(fire.is(BlockTags.FIRE), "it is fire");
        test.assertTrue(fire.canBeReplaced(), "it can be replaced");
        test.assertTrue(fire.getCollisionShape(test.getLevel(), test.absolutePos(rel)).isEmpty(), "one walks through it");
        Entity pig = test.spawnWithNoFreeWill(EntityType.PIG, rel);
        test.runAfterDelay(8, () -> {
            test.assertTrue(pig.isOnFire(), "and it sets what stands in it on fire");
            pig.clearFire();
            test.succeed();
        });
    }

    // ---- a final look at the failures ------------------------------------------------------------------------------

    public static void nothingFailedWhileTheGroupWasRegistered(GameTestHelper test) {
        List<String> failures = new ArrayList<>(ThaumicTinkerer.failures());
        test.assertTrue(failures.isEmpty(), "failures: " + failures);
        test.succeed();
    }
}
