/**
 * Created by pixlepix on 4/14/14.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.common.block.tile.TileInfusedFarmland;
import thaumic.tinkerer.common.block.tile.TileInfusedGrain;
import thaumic.tinkerer.common.content.SimpleBlocks;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.core.helper.AspectCropLootManager;
import thaumic.tinkerer.common.item.ItemInfusedSeeds;

/**
 * The infused crop. It grows from the tendencies in its block entity, not from random ticks (1.7.10: "Growth takes
 * place in the tile entity"), and the harvest is code, not a loot table: the seeds of the crop's aspect (or of an
 * aspect bred with the soil's residue), the bounty of the aspect when it is ripe, and a deposit of residue in the soil.
 *
 * <p>The 1.7.10 icon was chosen by the aspect in the tile entity and the age in the metadata. Here the age is
 * {@link CropBlock#AGE} and the aspect's texture group is the {@link #ASPECT_TEXTURE} property (0 aer, 1 ignis, 2 aqua,
 * 3 terra, 4 ordo, 5 perditio, 6 generic), set when the crop is planted; the blockstate file maps the pairs to models
 * and the colour handler tints group 6 with the colour of the aspect.</p>
 */
public class BlockInfusedGrain extends CropBlock implements EntityBlock {

    // Code based off vanilla potato code

    public static final int BREEDING_CHANCE = 10;

    /** 0..5 for the primal aspects ({@link #getNumberFromAspectForTexture}), 6 for the others. */
    public static final IntegerProperty ASPECT_TEXTURE = IntegerProperty.create("texture", 0, 6);

    public BlockInfusedGrain(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ASPECT_TEXTURE, 0));
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak()
                .sound(SoundType.CROP).pushReaction(PushReaction.DESTROY).noLootTable();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ASPECT_TEXTURE);
    }

    // Returns 0-5 for primal aspects, or 6 if compound aspect
    public static int getNumberFromAspectForTexture(Aspect aspect) {
        if (aspect == Aspect.AIR) {
            return 0;
        }
        if (aspect == Aspect.FIRE) {
            return 1;
        }
        if (aspect == Aspect.WATER) {
            return 2;
        }
        if (aspect == Aspect.EARTH) {
            return 3;
        }
        if (aspect == Aspect.ORDER) {
            return 4;
        }
        if (aspect == Aspect.ENTROPY) {
            return 5;
        }
        return 6;
    }

    @Nullable
    public static Aspect getAspect(BlockGetter world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof TileInfusedGrain tile ? tile.aspect : null;
    }

    public static void setAspect(BlockGetter world, BlockPos pos, Aspect aspect) {
        if (world.getBlockEntity(pos) instanceof TileInfusedGrain tile) {
            tile.aspect = aspect;
        }
    }

    /** The crop state of a seed of this aspect: the texture group goes into the state, the aspect into the tile. */
    public BlockState stateFor(Aspect aspect) {
        return defaultBlockState().setValue(AGE, 0).setValue(ASPECT_TEXTURE, getNumberFromAspectForTexture(aspect));
    }

    /** 1.7.10 {@code canPlaceBlockOn}: any farmland, the infused one included (it refuses every other plant itself). */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof FarmBlock;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileInfusedGrain(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != SimpleBlocks.TILE_INFUSED_GRAIN.get()) return null;
        return (lvl, pos, st, be) -> TileInfusedGrain.serverTick(lvl, pos, st, (TileInfusedGrain) be);
    }

    /** 1.7.10 {@code getDrops}, with the state's age as the metadata and the tile of the crop from the loot parameters. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> ret = new ArrayList<>();
        ServerLevel world = params.getLevel();
        Vec3 origin = params.getOptionalParameter(LootContextParams.ORIGIN);
        if (world == null || origin == null) return ret;
        BlockPos pos = BlockPos.containing(origin);
        BlockEntity found = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        TileInfusedGrain tileGrain = found instanceof TileInfusedGrain grain ? grain
                : world.getBlockEntity(pos) instanceof TileInfusedGrain other ? other : null;
        return harvest(world, pos, state, tileGrain, world.random);
    }

    /** The drops of the crop at {@code pos}: what 1.7.10 {@code getDrops} computed, including the soil deposit. */
    public List<ItemStack> harvest(ServerLevel world, BlockPos pos, BlockState state, @Nullable TileInfusedGrain tileGrain,
            RandomSource rand) {
        List<ItemStack> ret = new ArrayList<>();
        int metadata = state.getValue(AGE);
        int count = 1;
        for (int i = 0; i < count; i++) {
            ItemStack seedStack = new ItemStack(ItemInfusedSeeds.item());
            ItemInfusedSeeds.setAspect(seedStack, getAspectDropped(world, pos, metadata, tileGrain, rand));
            if (tileGrain != null) {
                ItemInfusedSeeds.setAspectTendencies(seedStack, tileGrain.primalTendencies);
            }
            while (rand.nextInt(10000) < Math.pow(getPrimalTendencyCount(tileGrain, Aspect.ENTROPY), 2)
                    && seedStack.getCount() < seedStack.getMaxStackSize()) {
                seedStack.grow(1);
            }
            ret.add(seedStack);
            fertilizeSoil(world, pos, metadata, tileGrain, rand);
        }
        if (metadata >= 7) {
            do {
                Aspect aspect = tileGrain == null ? null : tileGrain.aspect;
                ItemStack retItem = AspectCropLootManager.getLootForAspect(aspect);
                if (retItem != null) ret.add(retItem);

            } while (rand.nextInt(75) < getPrimalTendencyCount(tileGrain, Aspect.ORDER));
        }

        return ret;
    }

    public static int getPrimalTendencyCount(@Nullable TileInfusedGrain tileGrain, Aspect aspect) {
        return tileGrain != null ? tileGrain.primalTendencies.getAmount(aspect) : 0;
    }

    private void fertilizeSoil(Level world, BlockPos pos, int metadata, @Nullable TileInfusedGrain tileGrain, RandomSource rand) {
        if (metadata >= 7) {
            do {
                if (world.getBlockEntity(pos.below()) instanceof TileInfusedFarmland farmland) {
                    Aspect currentAspect = tileGrain == null ? null : tileGrain.aspect;
                    farmland.aspectList.add(currentAspect, 1);
                    farmland.reduceSaturatedAspects();
                    farmland.setChanged();
                    BlockState soil = world.getBlockState(pos.below());
                    world.sendBlockUpdated(pos.below(), soil, soil, 3);
                }
            } while (rand.nextInt(55) < getPrimalTendencyCount(tileGrain, Aspect.EARTH));
        }
    }

    /** 1.7.10 {@code updateTick}: only the check that the crop may stay; the growth is in the tile entity. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            dropResources(state, level, pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    public Aspect getAspectDropped(Level world, BlockPos pos, int metadata, @Nullable TileInfusedGrain tileGrain, RandomSource rand) {
        Aspect currentAspect = tileGrain == null ? null : tileGrain.aspect;
        if (metadata >= 7 && currentAspect != null) {
            if (world.getBlockEntity(pos.below()) instanceof TileInfusedFarmland farmland) {
                AspectList farmlandAspectList = farmland.aspectList;
                for (Aspect aspect : farmlandAspectList.getAspects()) {
                    if (rand.nextInt(BREEDING_CHANCE) < (getPrimalTendencyCount(tileGrain, Aspect.FIRE) + 1)
                            * farmlandAspectList.getAmount(aspect)
                            * farmlandAspectList.getAmount(aspect)) {
                        if (Aspect.getCombinationResult(aspect, currentAspect) != null) {
                            return Aspect.getCombinationResult(aspect, currentAspect);
                        }
                    }
                }
            }
        }
        return currentAspect;
    }

    // ---- bone meal: the bone meal switch of the configuration (the Hoe of Growth has its own path) -------------

    /** The 1.7.10 {@code BonemealEventHandler} cancelled bone meal on these crops unless {@code cropsAllowBonemeal}. */
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return ConfigHandler.cropsAllowBonemeal && !isMaxAge(state);
    }

    /** Grows by the usual two to five stages, keeping the texture group (vanilla would reset the state). */
    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        int age = Math.min(getAge(state) + getBonemealAgeIncrease(level), getMaxAge());
        level.setBlock(pos, state.setValue(AGE, age), Block.UPDATE_CLIENTS);
    }

    /** The seeds of the crop's aspect (the pick-block of the crop). */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(ItemInfusedSeeds.item());
        Aspect aspect = level.getBlockEntity(pos) instanceof TileInfusedGrain tile ? tile.aspect : null;
        if (aspect != null) ItemInfusedSeeds.setAspect(stack, aspect);
        return stack;
    }
}
