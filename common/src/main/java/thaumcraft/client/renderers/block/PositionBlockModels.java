package thaumcraft.client.renderers.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import thaumcraft.common.blocks.BlockColoredTallowCandle;
import thaumcraft.common.blocks.BlockEldritch;
import thaumcraft.common.blocks.BlockTallowCandle;

/** Position-aware world geometry, shared by the vanilla and Fabric terrain adapters. */
public final class PositionBlockModels {
    private static final FaceBakery BAKERY = new FaceBakery();
    private static final ModelState IDENTITY = new ModelState() {};
    private PositionBlockModels() {}

    public static boolean handles(BlockState state) {
        return state.getBlock() instanceof BlockTallowCandle
                || state.getBlock() instanceof BlockColoredTallowCandle
                || state.hasProperty(BlockEldritch.CRUST_NEIGHBOR_MASK);
    }

    public static BakedModel worldModel(BlockAndTintGetter world, BlockPos pos, BlockState state, BakedModel base) {
        List<BakedQuad> quads = new ArrayList<>();
        if (state.hasProperty(BlockEldritch.CRUST_NEIGHBOR_MASK)) {
            // Also sample here: worldgen/setBlock placement does not call getStateForPlacement.
            int mask = BlockEldritch.crustMask(world, pos);
            quads.addAll(EldritchCrustBakedModel.worldQuads(
                    state.setValue(BlockEldritch.CRUST_NEIGHBOR_MASK,mask),base.getParticleIcon()));
        } else {
            var candle = CandleDripRules.candle(pos.getX(), pos.getY(), pos.getZ());
            int tint = 0;
            var sprite = base.getParticleIcon();
            box(quads, candle.body(), sprite, tint);
            for (var drip : candle.drips()) box(quads, drip, sprite, tint);
            // Keep the original uncoloured stub sprite from the native model.
            RandomSource random = RandomSource.create(0);
            for (BakedQuad quad : base.getQuads(state, null, random)) {
                if (!quad.getSprite().equals(sprite)) quads.add(quad);
            }
        }
        return new Geometry(base, List.copyOf(quads));
    }

    private static void box(List<BakedQuad> out, CandleDripRules.Box b, TextureAtlasSprite sprite, int tint) {
        box(out, new float[]{b.minX()*16, b.minY()*16, b.minZ()*16,
                b.maxX()*16, b.maxY()*16, b.maxZ()*16}, sprite, tint, true);
    }

    static void box(List<BakedQuad> out, float[] b, TextureAtlasSprite sprite, int tint, boolean shade) {
        Vector3f from = new Vector3f(b[0], b[1], b[2]), to = new Vector3f(b[3], b[4], b[5]);
        for (Direction face : Direction.values()) {
            // RenderBlocks' world-aligned UVs, not a full texture stretched over every drip.
            float[] uv = switch (face) {
                case DOWN -> new float[]{b[0],16-b[5],b[3],16-b[2]};
                case UP -> new float[]{b[0],b[2],b[3],b[5]};
                case NORTH -> new float[]{16-b[3],16-b[4],16-b[0],16-b[1]};
                case SOUTH -> new float[]{b[0],16-b[4],b[3],16-b[1]};
                case WEST -> new float[]{b[2],16-b[4],b[5],16-b[1]};
                case EAST -> new float[]{16-b[5],16-b[4],16-b[2],16-b[1]};
            };
            out.add(BAKERY.bakeQuad(from,to,new BlockElementFace(null,tint,"",new BlockFaceUV(uv,0)),
                    sprite,face,IDENTITY,null,shade));
        }
    }

    private record Geometry(BakedModel base, List<BakedQuad> quads) implements BakedModel {
        @Override public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
            return face == null ? quads : List.of();
        }
        @Override public boolean useAmbientOcclusion() { return false; }
        @Override public boolean isGui3d() { return base.isGui3d(); }
        @Override public boolean usesBlockLight() { return base.usesBlockLight(); }
        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return base.getParticleIcon(); }
        @Override public ItemTransforms getTransforms() { return base.getTransforms(); }
        @Override public ItemOverrides getOverrides() { return base.getOverrides(); }
    }
}
