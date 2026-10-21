package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileWardingStone;

/**
 * Original BlockCosmeticSolid: the sixteen decorative stones of the Outer Lands.
 *
 * <p>The original is ONE block with sixteen metadata values; the metadata became the
 * {@link #VARIANT} block state. Minecraft 1.21.1 binds the hardness and the blast resistance to
 * the block and not to the block state, so the metadata that differ in hardness (the oracle
 * BlockCosmeticSolid:213-225 and :241-253) became separate blocks of this class; every one of them
 * hosts the metadata of its own hardness group. The two metadata values that carry a block entity,
 * the warding stone and the node, keep it.
 */
public final class BlockCosmeticSolid extends BaseEntityBlock {
    public static final MapCodec<BlockCosmeticSolid> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 2).fieldOf("group").forGetter(BlockCosmeticSolid::group),
            propertiesCodec()).apply(instance, BlockCosmeticSolid::new));
    /** Original BlockCosmeticSolid metadata 0..15. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 15);

    private final List<Integer> variants;
    private final int group;

    /** The hardness and the blast resistance belong to the block in 1.21.1. */
    public BlockCosmeticSolid(int group, Properties properties) {
        super(properties);
        this.group = group;
        this.variants = EldritchBlockRules.cosmeticGroupVariants(group);
        if (this.variants.isEmpty()) throw new IllegalArgumentException("Unknown original cosmetic hardness group " + group);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, this.variants.get(0)));
    }

    /** The hardness group of this block. */
    public int group() { return group; }

    /** The original metadata values that this block hosts. */
    public List<Integer> variants() { return variants; }

    /** True when the block hosts the original metadata. */
    public boolean allows(int variant) { return variants.contains(variant); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }

    /** The original metadata value of a state. */
    public static int variant(BlockState state) { return state.getValue(VARIANT); }

    /** BlockCosmeticSolid:227-239 - the per metadata light level. */
    public static int lightOf(BlockState state) { return EldritchBlockRules.cosmeticLight(state.getValue(VARIANT)); }

    /** BlockCosmeticSolid:278-296 - the warding stone and the node tile carry a block entity. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        int variant = state.getValue(VARIANT);
        if (variant == EldritchBlockRules.COSMETIC_WARDING_STONE) {
            return new TileWardingStone(pos, state);
        }
        if (variant == EldritchBlockRules.COSMETIC_OBSIDIAN_TILE_SHADED) {
            return new TileNode(ThaumcraftEldritchBlocks.COSMETIC_NODE_TILE.get(), pos, state);
        }
        return null;
    }

    /**
     * Original TileWardingStone:31-56 - the warding stone pushes the entities out of itself and
     * seals the two blocks above it. The original ticked the tile on both sides, so the ticker
     * runs on the client as well.
     */
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(VARIANT) == EldritchBlockRules.COSMETIC_OBSIDIAN_TILE_SHADED) {
            // Oracle BlockCosmeticSolid:288-296 - the metadata eight hosts the node of the
            // totem of the original, and the node of the original ticks there.
            return createTickerHelper(type, ThaumcraftEldritchBlocks.COSMETIC_NODE_TILE.get(),
                    TileNode::serverTick);
        }
        if (state.getValue(VARIANT) != EldritchBlockRules.COSMETIC_WARDING_STONE) return null;
        return createTickerHelper(type, ThaumcraftEldritchBlocks.WARDING_STONE_TILE.get(),
                (lvl, pos, st, tile) -> { if (tile instanceof TileWardingStone stone) TileWardingStone.tick(lvl, pos, st, stone); });
    }

    /** BlockCosmeticSolid:360-370 - the golem stone toggles with the redstone power. */
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighbourPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighbourPos, movedByPiston);
        int variant = state.getValue(VARIANT);
        if (variant == EldritchBlockRules.COSMETIC_GOLEM_STONE && level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.setValue(VARIANT, EldritchBlockRules.COSMETIC_GOLEM_STONE_ACTIVE), Block.UPDATE_ALL);
        } else if (variant == EldritchBlockRules.COSMETIC_GOLEM_STONE_ACTIVE && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.setValue(VARIANT, EldritchBlockRules.COSMETIC_GOLEM_STONE), Block.UPDATE_ALL);
        }
    }

    /**
     * BlockCosmeticSolid:263-276 - a living entity that walks over the paving stone of travel
     * gets the speed effect for forty ticks and the jump effect for forty ticks.
     */
    @Override public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (state.getValue(VARIANT) != EldritchBlockRules.COSMETIC_TRAVEL_STONE) return;
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) return;
        living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
        living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.JUMP, 40, 0, false, false));
    }

    /** BlockCosmeticSolid:182-188 - the decorative stone is a full block. */
    @Override protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return true;
    }
}
