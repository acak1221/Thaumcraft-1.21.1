package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileWardingStone;

/**
 * Original BlockCosmeticSolid: the sixteen decorative stone variants of the
 * Outer Lands. The original metadata became the {@link #VARIANT} block state.
 */
public final class BlockCosmeticSolid extends BaseEntityBlock {
    public static final MapCodec<BlockCosmeticSolid> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 2).fieldOf("group").forGetter(BlockCosmeticSolid::group),
            propertiesCodec()).apply(instance, BlockCosmeticSolid::new));
    /** Original BlockCosmeticSolid metadata 0..15. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 15);

    private final java.util.List<Integer> variants;
    private final int group;

    /** The hardness and the blast resistance belong to the block in 1.21.1. */
    public BlockCosmeticSolid(int group, Properties properties) {
        super(properties);
        this.group = group;
        this.variants = EldritchBlockRules.cosmeticGroupVariants(group);
        if (this.variants.isEmpty()) throw new IllegalArgumentException("Unknown original cosmetic hardness group " + group);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, this.variants.get(0)));
    }

    /**
     * A single original metadata value of the cosmetic solid family. The legacy
     * registrations name one block per stone (the obsidian tile, the paving
     * stones, the eldritch stone), so this constructor keeps that shape next to
     * the hardness group constructor.
     */
    public BlockCosmeticSolid(Properties properties, int variant) {
        super(properties);
        this.group = EldritchBlockRules.cosmeticGroup(variant);
        this.variants = java.util.List.of(variant);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, variant));
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

    /** BlockCosmeticSolid:227-239 - the per metadata light level. */
    public static int lightOf(BlockState state) { return EldritchBlockRules.cosmeticLight(state.getValue(VARIANT)); }

    /** BlockCosmeticSolid:259-261 - the original damage value of the dropped item. */
    public int damageDropped(BlockState state) {
        int variant = state.getValue(VARIANT);
        if (variant == EldritchBlockRules.COSMETIC_OBSIDIAN_TILE_SHADED) return 1;
        if (variant == EldritchBlockRules.COSMETIC_GOLEM_STONE_ACTIVE) return 9;
        return variant;
    }

    /** BlockCosmeticSolid:288-296 - the warding stone and the node tile carry a block entity. */
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

    /** BlockCosmeticSolid:182-188 - the decorative stone is a full block. */
    @Override protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return true;
    }
}
