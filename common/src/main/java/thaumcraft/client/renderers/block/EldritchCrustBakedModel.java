package thaumcraft.client.renderers.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import thaumcraft.common.blocks.BlockEldritch;

/**
 * The baked geometry of the eldritch crust, the glyphed stone and the decorated stone.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/block/BlockEldritchRenderer.java:35-63}.
 * The original draws the metadata four, five and six with a box that is inset by two
 * sixteenths on every side whose neighbour is not a solid material
 * ({@code Block.isBlockSolid}, material solidity) and by zero on the sides that touch
 * one, so a field of crust joins without a groove while an exposed face keeps the
 * original inset. The six flags are carried by the
 * {@link BlockEldritch#CRUST_NEIGHBOR_MASK} block state property, which the block
 * recomputes on placement and on every neighbour change, because a Minecraft 1.21.1
 * {@code BakedModel#getQuads} has neither the position nor the level.
 *
 * <p>The inventory is the isolated {@code 2..14} cube of
 * {@code BlockEldritchRenderer.java:22-33}: the item asks with a null state and the
 * quad of the wrapper delegates to the fixed cube of the block model.
 *
 * <p>The class is the model and the pure rule at once: {@link #minX(int)} and its five
 * siblings are static, so the bounds of every one of the sixty four masks can be
 * checked in a unit test without a world.
 */
public final class EldritchCrustBakedModel implements BakedModel {
    /** The original inset {@code W2}, in sixteenths of a block. */
    public static final float INSET = 2.0F;
    /** The original full size of a block, in sixteenths of a block. */
    public static final float FULL = 16.0F;
    /** The number of neighbour masks, {@code 1 << 6}. */
    public static final int MASKS = 64;
    /** The six faces in the order of {@link Direction#get3DDataValue()}. */
    private static final Direction[] FACES = Direction.values();
    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    /** The unrotated model state of the original block model. */
    private static final ModelState IDENTITY = new ModelState() {};
    private static final BlockFaceUV FULL_UV = new BlockFaceUV(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0);

    private final BakedModel delegate;
    /** One quad per mask and face, indexed {@code mask * 6 + face.get3DDataValue()}. */
    private final List<BakedQuad> quads;
    /** The unculled list of every mask: the inset faces of that mask. */
    private final List<List<BakedQuad>> unculled;

    /**
     * Bakes the sixty four shapes from the sprite of the delegated model.
     *
     * @param delegate the fixed {@code 2..14} cube of the block model, whose particle
     *                 icon is the crust texture of that variant
     */
    public EldritchCrustBakedModel(BakedModel delegate) {
        this.delegate = delegate;
        TextureAtlasSprite sprite = delegate.getParticleIcon();
        List<BakedQuad> baked = new ArrayList<>(MASKS * FACES.length);
        List<List<BakedQuad>> exposed = new ArrayList<>(MASKS);
        for (int mask = 0; mask < MASKS; ++mask) {
            List<BakedQuad> exposedFaces = new ArrayList<>(FACES.length);
            for (Direction face : FACES) {
                BakedQuad quad = bake(mask, face, sprite);
                baked.add(quad);
                if (!joined(mask, face)) exposedFaces.add(quad);
            }
            exposed.add(List.copyOf(exposedFaces));
        }
        this.quads = List.copyOf(baked);
        this.unculled = List.copyOf(exposed);
    }

    private static BakedQuad bake(int mask, Direction face, TextureAtlasSprite sprite) {
        Vector3f from = new Vector3f(minX(mask), minY(mask), minZ(mask));
        Vector3f to = new Vector3f(maxX(mask), maxY(mask), maxZ(mask));
        // A joined face is coplanar with the neighbour and is culled by the requested
        // direction; an exposed face must never be culled. The block model renderer asks
        // for the joined quads by direction and for the exposed ones with a null direction.
        Direction cull = joined(mask, face) ? face : null;
        BlockElementFace element = new BlockElementFace(cull, -1, "", FULL_UV);
        return FACE_BAKERY.bakeQuad(from, to, element, sprite, face, IDENTITY, null, true);
    }

    /** True when the original does not inset the given side, that is when its neighbour is solid. */
    public static boolean joined(int mask, Direction face) {
        return (mask & (1 << face.get3DDataValue())) != 0;
    }

    /** The original minimum x of the box, in sixteenths. */
    public static float minX(int mask) { return joined(mask, Direction.WEST) ? 0.0F : INSET; }
    /** The original minimum y of the box, in sixteenths. */
    public static float minY(int mask) { return joined(mask, Direction.DOWN) ? 0.0F : INSET; }
    /** The original minimum z of the box, in sixteenths. */
    public static float minZ(int mask) { return joined(mask, Direction.NORTH) ? 0.0F : INSET; }
    /** The original maximum x of the box, in sixteenths. */
    public static float maxX(int mask) { return joined(mask, Direction.EAST) ? FULL : FULL - INSET; }
    /** The original maximum y of the box, in sixteenths. */
    public static float maxY(int mask) { return joined(mask, Direction.UP) ? FULL : FULL - INSET; }
    /** The original maximum z of the box, in sixteenths. */
    public static float maxZ(int mask) { return joined(mask, Direction.SOUTH) ? FULL : FULL - INSET; }

    /** The six original bounds in the order west, down, north, east, up, south, in sixteenths. */
    public static float[] bounds(int mask) {
        return new float[]{minX(mask), minY(mask), minZ(mask), maxX(mask), maxY(mask), maxZ(mask)};
    }

    /** World adapter entry: consume the maintained state and preserve world-aligned texture UVs. */
    public static List<BakedQuad> worldQuads(BlockState state, TextureAtlasSprite sprite) {
        List<BakedQuad> result = new ArrayList<>(6);
        PositionBlockModels.box(result,bounds(state.getValue(BlockEldritch.CRUST_NEIGHBOR_MASK)),sprite,-1,false);
        return result;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
        if (state == null || !state.hasProperty(BlockEldritch.CRUST_NEIGHBOR_MASK)) {
            return delegate.getQuads(state, face, random);
        }
        int mask = state.getValue(BlockEldritch.CRUST_NEIGHBOR_MASK);
        if (face == null) return unculled.get(mask);
        return joined(mask, face) ? List.of(quads.get(mask * FACES.length + face.get3DDataValue())) : List.of();
    }

    @Override public boolean useAmbientOcclusion() { return delegate.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return delegate.isGui3d(); }
    @Override public boolean usesBlockLight() { return delegate.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return delegate.getParticleIcon(); }
    @Override public ItemTransforms getTransforms() { return delegate.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return delegate.getOverrides(); }
}
