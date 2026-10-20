package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileWardingStoneFence;

/**
 * Original BlockAiry: the airy family. This port owns the warding stone
 * fence; the node, the nitor and the energised node of the original family
 * already exist as their own native blocks.
 */
public final class BlockAiry extends BaseEntityBlock {
    public static final MapCodec<BlockAiry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 3).fieldOf("group").forGetter(BlockAiry::group),
            propertiesCodec()).apply(instance, BlockAiry::new));
    /** Original BlockAiry metadata 0..12. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 15);

    private final java.util.List<Integer> variants;
    private final int group;

    /** The hardness and the blast resistance belong to the block in 1.21.1. */
    public BlockAiry(int group, Properties properties) {
        super(properties);
        this.group = group;
        this.variants = EldritchBlockRules.airyGroupVariants(group);
        if (this.variants.isEmpty()) throw new IllegalArgumentException("Unknown original airy hardness group " + group);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, this.variants.get(0)));
    }

    /** The hardness group of this block. */
    public int group() { return group; }

    /** The original metadata values that this block hosts. */
    public java.util.List<Integer> variants() { return variants; }

    /** True when the block hosts the original metadata. */
    public boolean allows(int variant) { return variants.contains(variant); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }

    public static int variant(BlockState state) { return state.getValue(VARIANT); }

    /** BlockAiry:216-230 - the per metadata light level. */
    public static int lightOf(BlockState state) { return EldritchBlockRules.airyLight(state.getValue(VARIANT)); }

    /** BlockAiry:206-213 - the replaceable metadata values. */
    @Override protected boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        return EldritchBlockRules.airyReplaceable(state.getValue(VARIANT));
    }

    /** BlockAiry:263-265 - the leaves metadata values are treated as leaves. */
    @Override protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return !EldritchBlockRules.airyIsLeaves(state.getValue(VARIANT));
    }

    /** BlockAiry:272-280 - most airy variants have no selection box at all. */
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        switch (state.getValue(VARIANT)) {
            case EldritchBlockRules.AIRY_NODE:
            case EldritchBlockRules.AIRY_LEAVES:
            case EldritchBlockRules.AIRY_LEAVES_ALT:
            case EldritchBlockRules.AIRY_WARDING_FENCE:
            case EldritchBlockRules.AIRY_ENERGIZED_NODE:
            case EldritchBlockRules.AIRY_MAGIC_HOLE:
            case EldritchBlockRules.AIRY_VOID_HOLE:
            case EldritchBlockRules.AIRY_UNBREAKABLE:
                return Shapes.empty();
            default:
                return Shapes.block();
        }
    }

    /** BlockAiry:96-100 - the warding fence carries the warding stone block entity. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(VARIANT) != EldritchBlockRules.AIRY_WARDING_FENCE) return null;
        return new TileWardingStoneFence(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, tile) -> {
                    if (tile instanceof TileWardingStoneFence fence) TileWardingStoneFence.tick(lvl, pos, st, fence);
                };
    }
}
