package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Original BlockCosmeticStairs: the stairs that take the texture of another cosmetic block.
 * <p>
 * The original built the eldritch stairs from the eldritch stone of the cosmetic solid, the greatwood and the
 * silverwood stairs from the planks of the wooden device. The native port already has the greatwood and the
 * silverwood stairs, so this block serves the eldritch stairs.
 * <p>
 * Oracle: {@code thaumcraft/common/blocks/BlockCosmeticStairs.java}, lines 23-43.
 */
public final class BlockCosmeticStairs extends StairBlock {
    public static final MapCodec<BlockCosmeticStairs> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockState.CODEC.fieldOf("base_state").forGetter(stairs -> stairs.parent),
            propertiesCodec()).apply(instance, BlockCosmeticStairs::new));

    /**
     * The index of the ancient stone texture the stair shows.
     *
     * <p>The original of 1.7.10 had no block state property: the icon of a stair was asked per
     * rendered face and the icon of the eldritch stone was the one the hash of the position
     * chose. Minecraft 1.21.1 bakes one model per block state, so the port carries the index
     * of the texture in the state and the blockstate document maps every index to its own
     * model.</p>
     */
    public static final IntegerProperty TEXTURE = IntegerProperty.create("texture", 0, 3);

    /** The four ancient stone textures of the original (the icons 17 to 20 of the cosmetic solid). */
    public static final int TEXTURE_COUNT = 4;

    /** The 1.7.10 side index of the top face. */
    public static final int SIDE_UP = 1;

    /** The 1.7.10 side index of the bottom face. */
    public static final int SIDE_DOWN = 0;

    /** The base of the bound of the draw of the original (oracle BlockCosmeticSolid:160). */
    static final int DRAW_BASE = 12345;

    /**
     * The offset the cosmetic stairs add to the side index before they ask the cosmetic solid
     * for the icon (oracle BlockCosmeticStairs:39, {@code side + 100}).
     */
    static final int STAIR_SIDE_OFFSET = 100;

    private final BlockState parent;

    public BlockCosmeticStairs(BlockState parent, Properties properties) {
        super(parent, properties);
        this.parent = parent;
    }

    @Override public MapCodec<? extends StairBlock> codec() { return CODEC; }

    /** The block the stairs take their texture from. */
    public BlockState parentState() { return parent; }

    // BlockCosmeticStairs:32 sets the light opacity of the original to zero.
    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return lightOpacity(state.getValue(WATERLOGGED), super.getLightBlock(state, level, pos));
    }

    /**
     * BlockCosmeticStairs:32 - the original sets the light opacity of the stairs to zero, so a
     * dry stair lets the light through; a waterlogged stair keeps the vanilla behaviour.
     *
     * <p>The rule is a pure function so that it can be asserted without constructing a block: a
     * constructed block would leave an unregistered intrusive holder behind and break the
     * registry freeze of the whole test suite.</p>
     *
     * @param waterlogged    the stair carries water
     * @param vanillaOpacity the opacity the vanilla stair would have
     */
    public static int lightOpacity(boolean waterlogged, int vanillaOpacity) {
        return waterlogged ? vanillaOpacity : 0;
    }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return state.getValue(WATERLOGGED); }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return !state.getValue(WATERLOGGED); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TEXTURE);
    }

    /**
     * Oracle BlockCosmeticSolid:157-162 - the icon of the ancient stone is the one the hash of
     * the position chooses.
     *
     * <p>The original draws the icon of the face that is rendered: the seed is the hash of the
     * concatenated coordinates multiplied by a hundred plus one, and the draw is
     * {@code nextInt(12345 + side) % 4}. The cosmetic stairs ask the cosmetic solid with the
     * side index plus a hundred (oracle BlockCosmeticStairs:39), so the bound of the stairs is
     * {@code 12445 + side}.</p>
     *
     * <p>The rule is a pure function so that it can be asserted without constructing a block: a
     * constructed block would leave an unregistered intrusive holder behind and break the
     * registry freeze of the whole test suite.</p>
     *
     * @param x    the block coordinate of the stair
     * @param y    the block coordinate of the stair
     * @param z    the block coordinate of the stair
     * @param side the 1.7.10 side index of the face: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east
     * @return the index of the ancient stone texture, 0 to 3
     */
    public static int textureIndex(int x, int y, int z, int side) {
        String key = x + "" + y + "" + z;
        Random random = new Random(Math.abs(key.hashCode() * 100) + 1);
        return random.nextInt(DRAW_BASE + STAIR_SIDE_OFFSET + side) % TEXTURE_COUNT;
    }

    /**
     * The texture index of the stair at the position.
     *
     * <p>The 1.7.10 renderer asked the icon of every face separately, so the original stair
     * could show up to four different ancient stone textures at once. This state value is the
     * top-face fallback for inventory/data models; PositionBlockModels remaps each world quad
     * independently using its final world-facing direction and the actual position.</p>
     */
    public static int textureIndex(int x, int y, int z) {
        return textureIndex(x, y, z, SIDE_UP);
    }

    /**
     * The state of the stair that is placed at the position: the texture index of the original
     * hash of the position.
     */
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        // StairBlock#getStateForPlacement returns null when the context refuses the placement;
        // dereferencing it would turn a refused placement into a crash.
        if (state == null) return null;
        BlockPos pos = context.getClickedPos();
        return state.setValue(TEXTURE, textureIndex(pos.getX(), pos.getY(), pos.getZ()));
    }

    /**
     * The texture of the stair follows the position, so a stair that a piston moved takes the
     * texture of its new place, exactly like the original, where the icon was a function of the
     * position and not of the block.
     */
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        int index = textureIndex(pos.getX(), pos.getY(), pos.getZ());
        if (state.getValue(TEXTURE) == index) return;
        level.setBlock(pos, state.setValue(TEXTURE, index), Block.UPDATE_CLIENTS);
    }
}
