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
import thaumcraft.common.blocks.BlockCosmeticStairs;

/** Position-aware world geometry, shared by the vanilla and Fabric terrain adapters. */
public final class PositionBlockModels {
    private static final FaceBakery BAKERY = new FaceBakery();
    private static final ModelState IDENTITY = new ModelState() {};
    private PositionBlockModels() {}

    public static boolean handles(BlockState state) {
        return state.getBlock() instanceof BlockTallowCandle
                || state.getBlock() instanceof BlockColoredTallowCandle
                || state.hasProperty(BlockEldritch.CRUST_NEIGHBOR_MASK)
                || state.getBlock() instanceof BlockCosmeticStairs || LegacyBlockQuads.handles(state);
    }

    public static BakedModel worldModel(BlockAndTintGetter world, BlockPos pos, BlockState state, BakedModel base) {
        return worldModel(world,pos,state,base,false);
    }

    /** Platform adapters able to route individual quad materials request both passes. */
    public static BakedModel worldModel(BlockAndTintGetter world, BlockPos pos, BlockState state, BakedModel base, boolean layered) {
        List<BakedQuad> quads = new ArrayList<>();
        if (LegacyBlockQuads.handles(state)) {
            quads.addAll(LegacyBlockQuads.quads(world,pos,state,base,layered));
        } else if (state.getBlock() instanceof BlockCosmeticStairs) {
            RandomSource random = RandomSource.create(state.getSeed(pos));
            for (Direction face : Direction.values()) {
                if (net.minecraft.world.level.block.Block.shouldRenderFace(state,world,pos,face,pos.relative(face)))
                    for (BakedQuad quad : base.getQuads(state,face,random)) quads.add(stairQuad(quad,pos));
            }
            for (BakedQuad quad : base.getQuads(state,null,random)) quads.add(stairQuad(quad,pos));
        } else if (state.hasProperty(BlockEldritch.CRUST_NEIGHBOR_MASK)) {
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

    /** Marks an adapter which already routes both cube and overlay to their chunk layers. */
    public interface LayeredModel {}

    public static BakedModel geometry(BakedModel base,List<BakedQuad> quads) {return new Geometry(base,quads);}

    /** One shared light/pass implementation for vanilla and NeoForge's ModelData overload. */
    public static boolean tesselate(net.minecraft.client.renderer.block.ModelBlockRenderer renderer,
            BlockAndTintGetter world,BakedModel base,BlockState state,BlockPos pos,
            com.mojang.blaze3d.vertex.PoseStack poses,com.mojang.blaze3d.vertex.VertexConsumer vertices,
            boolean checkSides,RandomSource random,long seed,int overlay) {
        if(!handles(state)||state.getBlock() instanceof thaumcraft.common.blocks.BlockTaint)return false;
        var model=worldModel(world,pos,state,base);
        boolean ao=net.minecraft.client.Minecraft.useAmbientOcclusion()&&state.getLightEmission()==0&&base.useAmbientOcclusion();
        if(LegacyBlockQuads.handles(state)) {
            var quads=model.getQuads(state,null,random);
            if(state.getBlock() instanceof thaumcraft.common.blocks.BlockCustomOre) {
                var solid=geometry(base,quads.stream().filter(q->!(q instanceof LegacyBlockQuads.LitQuad)).toList());
                if(ao)renderer.tesselateWithAO(world,solid,state,pos,poses,vertices,checkSides,random,seed,overlay);
                else renderer.tesselateWithoutAO(world,solid,state,pos,poses,vertices,checkSides,random,seed,overlay);
                quads=quads.stream().filter(q->q instanceof LegacyBlockQuads.LitQuad).toList();
            }
            LegacyBlockQuads.render(world,pos,state,poses,vertices,quads,overlay);
        } else if(state.getBlock() instanceof BlockCosmeticStairs && ao) {
            renderer.tesselateWithAO(world,model,state,pos,poses,vertices,checkSides,random,seed,overlay);
        } else renderer.tesselateWithoutAO(world,model,state,pos,poses,vertices,checkSides,random,seed,overlay);
        return true;
    }

    private static BakedQuad stairQuad(BakedQuad quad, BlockPos pos) {
        int index = BlockCosmeticStairs.textureIndex(pos.getX(),pos.getY(),pos.getZ(),quad.getDirection().get3DDataValue());
        var sprite = net.minecraft.client.Minecraft.getInstance().getTextureAtlas(
                net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft","blocks/es_"+(index+1)));
        int[] data = quad.getVertices().clone();
        int stride = data.length / 4;
        var old = quad.getSprite();
        for (int v=0;v<4;v++) {
            float u = Float.intBitsToFloat(data[v*stride+4]);
            float w = Float.intBitsToFloat(data[v*stride+5]);
            data[v*stride+4] = Float.floatToRawIntBits(sprite.getU((u-old.getU0())/(old.getU1()-old.getU0())));
            data[v*stride+5] = Float.floatToRawIntBits(sprite.getV((w-old.getV0())/(old.getV1()-old.getV0())));
        }
        return new BakedQuad(data,quad.getTintIndex(),quad.getDirection(),sprite,quad.isShade());
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
