package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.tiles.TileNode;

/**
 * Original BlockCosmeticSolid: the sixteen decorative solid blocks of Thaumcraft.
 * <p>
 * The metadata of the original picked the block: obsidian tile, paving stone, thaumium block, tallow block,
 * arcane stone, golem stone, the infused stone totems and the taint crust. The original had one block with
 * sixteen metadata values; 1.21.1 has no metadata and no per state hardness, so the native port registers the
 * variants that are still missing as separate blocks of this class.
 * <p>
 * The metadata eight block is the base of an obsidian totem and holds a node, the metadata three block is the
 * warding stone whose tile entity belongs to the warding subsystem that the native port does not have yet.
 * <p>
 * Oracle: {@code thaumcraft/common/blocks/BlockCosmeticSolid.java}, lines 60-371.
 */
public final class BlockCosmeticSolid extends BaseEntityBlock {
    public static final MapCodec<BlockCosmeticSolid> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(), Codec.INT.fieldOf("variant").forGetter(block -> block.variant))
            .apply(instance, BlockCosmeticSolid::new));
    private final int variant;

    public BlockCosmeticSolid(Properties properties, int variant) {
        super(properties);
        this.variant = variant;
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    /** The original metadata value of this block. */
    public int variant() { return variant; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // BlockCosmeticSolid:288-296 - the obsidian totem base holds a node.
        if (variant == 8) return new TileNode(ThaumcraftLegacyTiles.COSMETIC_SOLID_NODE_BLOCK_ENTITY.get(), pos, state);
        return null;
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (variant != 8) return null;
        return createTickerHelper(type, ThaumcraftLegacyTiles.COSMETIC_SOLID_NODE_BLOCK_ENTITY.get(), TileNode::serverTick);
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    /**
     * BlockCosmeticSolid:263-276 - the paving stone of the warding street slows and weakens the living that
     * walk over it.
     */
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (variant != 2 || !(entity instanceof LivingEntity living)) return;
        living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 40, 0));
    }
}
