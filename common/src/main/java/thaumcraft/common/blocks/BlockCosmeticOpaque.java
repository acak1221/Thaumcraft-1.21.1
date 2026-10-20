package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.tiles.TileOwned;

/**
 * Original BlockCosmeticOpaque: the amber block, the amber bricks and the warded glass.
 * <p>
 * The amber block and the amber bricks are solid and dim the light a little, the warded glass is a see through
 * block that cannot be broken and that remembers who placed it. The native port already has the amber block
 * and the amber bricks, so this class serves the warded glass.
 * <p>
 * Oracle: {@code thaumcraft/common/blocks/BlockCosmeticOpaque.java}, lines 58-268.
 */
public final class BlockCosmeticOpaque extends BaseEntityBlock {
    public static final MapCodec<BlockCosmeticOpaque> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(), Codec.INT.fieldOf("variant").forGetter(block -> block.variant))
            .apply(instance, BlockCosmeticOpaque::new));
    private final int variant;

    public BlockCosmeticOpaque(Properties properties, int variant) {
        super(properties);
        this.variant = variant;
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    /** The original metadata value of this block. */
    public int variant() { return variant; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // BlockCosmeticOpaque:214-219 - the warded glass remembers its owner.
        if (variant == 2) return new TileOwned(ThaumcraftLegacyTiles.COSMETIC_OPAQUE_BLOCK_ENTITY.get(), pos, state);
        return null;
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    /**
     * BlockCosmeticOpaque:240-247 - the warded glass belongs to the player who placed it.
     */
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (variant != 2 || !(placer instanceof Player player)) return;
        if (level.getBlockEntity(pos) instanceof TileOwned owned) {
            owned.owner = player.getGameProfile().getName();
            owned.setChanged();
        }
    }
}
