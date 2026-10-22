package thaumcraft.client.renderers.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.BlockCustomOre;
import thaumcraft.common.blocks.BlockTaint;
import thaumcraft.common.lib.world.TaintSpreadRules;

/** Shared ISBRH passes. Light is explicit: vanilla ignores baked vertex lightmaps. */
public final class LegacyBlockQuads {
    private LegacyBlockQuads() {}
    public static boolean handles(BlockState state) {
        return state.getBlock() instanceof BlockCustomOre ore && ore.originalVariant()>0 && ore.originalVariant()<7
                || state.getBlock() instanceof BlockTaint taint && TaintOverlayRules.applies(taint.metadata())
                || state.getBlock() instanceof thaumcraft.common.blocks.FluxFiniteBlock
                || state.getBlock() instanceof thaumcraft.common.blocks.BlockTaintFibres;
    }
    public static TextureAtlasSprite sprite(String name) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(
                ResourceLocation.fromNamespaceAndPath("thaumcraft","blocks/"+name));
    }
    public static List<BakedQuad> quads(BlockAndTintGetter world, BlockPos pos, BlockState state, BakedModel base, boolean layered) {
        if(state.getBlock() instanceof thaumcraft.common.blocks.FluxFiniteBlock)return GasWorldQuads.quads(world,pos,state,base);
        if(state.getBlock() instanceof thaumcraft.common.blocks.BlockTaintFibres)return TaintFibreWorldQuads.quads(world,pos,state);
        List<BakedQuad> out=new ArrayList<>();
        List<BakedQuad> emissive=new ArrayList<>();
        RandomSource random=RandomSource.create(state.getSeed(pos));
        int light=LevelRenderer.getLightColor(world,state,pos);
        for(Direction face:Direction.values()) {
            if(!Block.shouldRenderFace(state,world,pos,face,pos.relative(face)))continue;
            for(BakedQuad quad:base.getQuads(state,face,random)) {
                if(state.getBlock() instanceof BlockCustomOre && quad.isTinted())
                    emissive.add(new LitQuad(quad,OreRenderRules.overlayLight(light)));
                else out.add(quad);
            }
        }
        out.addAll(base.getQuads(state,null,random));
        out.addAll(emissive);
        if(state.getBlock() instanceof BlockCustomOre && OreRenderRules.stonePass()) {
            List<BakedQuad> stone=new ArrayList<>();
            var sprite=sprite("legacy_stone");
            PositionBlockModels.box(stone,new float[]{OreRenderRules.STONE_MIN*16,OreRenderRules.STONE_MIN*16,OreRenderRules.STONE_MIN*16,
                    OreRenderRules.STONE_MAX*16,OreRenderRules.STONE_MAX*16,OreRenderRules.STONE_MAX*16},sprite,0,false);
            for(BakedQuad quad:stone)if(Block.shouldRenderFace(state,world,pos,quad.getDirection(),pos.relative(quad.getDirection())))
                out.add(new LitQuad(quad,light));
        }
        if(layered && state.getBlock() instanceof BlockTaint)out.addAll(taintOverlays(world,pos,state));
        return out;
    }
    public static List<BakedQuad> taintOverlays(BlockAndTintGetter world, BlockPos pos, BlockState state) {
        List<BakedQuad> out=new ArrayList<>();
        if(state.getBlock() instanceof BlockTaint taint && TaintOverlayRules.applies(taint.metadata())) {
            // BlockTaintRenderer:41-65: white, brightness 200, no diffuse lighting,
            // opposite-side texture index (not the adjacent-support fibre plane).
            for(Direction face:Direction.values()) {
                if(!Block.shouldRenderFace(state,world,pos,face,pos.relative(face)))continue;
                int index=TaintSpreadRules.overlay(TaintOverlayRules.textureSide(face.get3DDataValue()),pos.getX(),pos.getY(),pos.getZ());
                if(index==0)continue;
                List<BakedQuad> cube=new ArrayList<>();
                PositionBlockModels.box(cube,new float[]{0,0,0,16,16,16},sprite("taint_over_"+index),-1,false);
                for(BakedQuad q:cube)if(q.getDirection()==face)out.add(new TransparentQuad(q,TaintOverlayRules.BRIGHTNESS));
            }
        }
        return out;
    }
    public static class LitQuad extends BakedQuad {
        private final int light;
        public LitQuad(BakedQuad quad,int light) {
            super(quad.getVertices(),quad.getTintIndex(),quad.getDirection(),quad.getSprite(),false);
            this.light=light;
        }
        public int light() {return light;}
    }
    public static final class TransparentQuad extends LitQuad {
        public TransparentQuad(BakedQuad quad,int light) {super(quad,light);}
    }

    public static void render(BlockAndTintGetter world,BlockPos pos,BlockState state,
            com.mojang.blaze3d.vertex.PoseStack poses,com.mojang.blaze3d.vertex.VertexConsumer vertices,
            List<BakedQuad> quads,int overlay) {
        poses.pushPose();
        var offset=state.getOffset(world,pos);poses.translate(offset.x,offset.y,offset.z);
        var minecraft=Minecraft.getInstance();
        for(var quad:quads) {
            int color=quad.isTinted()?minecraft.getBlockColors().getColor(state,world,pos,quad.getTintIndex()):0xffffff;
            float shade=world.getShade(quad.getDirection(),quad.isShade());
            int light=quad instanceof LitQuad lit?lit.light():LevelRenderer.getLightColor(world,state,pos.relative(quad.getDirection()));
            vertices.putBulkData(poses.last(),quad,new float[]{shade,shade,shade,shade},(color>>16&255)/255F,
                    (color>>8&255)/255F,(color&255)/255F,1F,new int[]{light,light,light,light},overlay,true);
        }
        poses.popPose();
    }
}
