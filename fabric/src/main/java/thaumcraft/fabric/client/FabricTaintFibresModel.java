package thaumcraft.fabric.client;

import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.BlockTaint;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.config.ThaumcraftTaintSettings;
import thaumcraft.common.lib.world.TaintSpreadRules;

/** Actual-neighbour fibre planes, old deterministic grass offset/crossed stalks and sparse type0 overlays. */
public final class FabricTaintFibresModel implements BakedModel,FabricBakedModel {
    private final BakedModel base;
    private final TextureAtlasSprite[] sprites,overlays;
    private final RenderMaterial tinted,glow;
    private FabricTaintFibresModel(BakedModel base,TextureAtlasSprite[] sprites,TextureAtlasSprite[] overlays){
        this.base=base;this.sprites=sprites;this.overlays=overlays;
        var renderer=java.util.Objects.requireNonNull(RendererAccess.INSTANCE.getRenderer(),"Fabric block renderer");
        tinted=renderer.materialFinder().blendMode(BlendMode.TRANSLUCENT).find();
        glow=renderer.materialFinder().blendMode(BlendMode.TRANSLUCENT).disableColorIndex(true).disableDiffuse(true).find();
    }
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    public static void init(){
        ModelLoadingPlugin.register(plugin->plugin.modifyModelAfterBake().register((model,context)->{
            if(model==null||context.topLevelId()==null||!context.topLevelId().id().equals(id("taint_fibres"))||context.topLevelId().variant().equals("inventory"))return model;
            String[] names={"taint_fibres","taintgrass1","taintgrass2","taint_spore_stalk_1","taint_spore_stalk_2"};
            var sprites=new TextureAtlasSprite[5];var overlays=new TextureAtlasSprite[4];
            for(int i=0;i<5;i++)sprites[i]=context.textureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,id("blocks/"+names[i])));
            for(int i=0;i<4;i++)overlays[i]=context.textureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,id("blocks/"+(i==0?"blank":"taint_over_"+i))));
            return new FabricTaintFibresModel(model,sprites,overlays);
        }));
    }
    @Override public boolean isVanillaAdapter(){return false;}
    @Override public void emitBlockQuads(BlockAndTintGetter world,BlockState state,BlockPos pos,Supplier<RandomSource> random,RenderContext context){
        int type=state.getValue(BlockTaintFibres.TYPE);var emitter=context.getEmitter();
        for(Direction side:Direction.values()){
            var neighbour=pos.relative(side);var support=world.getBlockState(neighbour);
            if(BlockTaint.solidFamily(support)||!support.isFaceSturdy(world,neighbour,side.getOpposite()))continue;
            plane(emitter,side.getOpposite(),.995F,sprites[0],tinted,true,false);
            if(type==0&&ThaumcraftTaintSettings.glowingTaint()){
                int overlay=TaintSpreadRules.overlay(side.get3DDataValue(),pos.getX(),pos.getY(),pos.getZ());
                if(overlay!=0)plane(emitter,side.getOpposite(),.98F,overlays[overlay],glow,false,true);
            }
        }
        if((type==1||type==2)&&BlockTaintFibres.sturdy(world,pos.below(),Direction.UP)){
            var offset=TaintSpreadRules.grassOffset(pos.getX(),pos.getY(),pos.getZ());float dx=(float)offset[0],dz=(float)offset[1];
            doubleCross(emitter,sprites[type],dx,dz);
        }else if(type==3||type==4)doubleCross(emitter,sprites[type],0,0);
    }
    private void plane(QuadEmitter out,Direction face,float depth,TextureAtlasSprite sprite,RenderMaterial material,boolean tint,boolean lit){
        out.square(face,0,0,1,1,depth).cullFace(null).material(material).colorIndex(tint?0:-1).color(-1,-1,-1,-1).uvUnitSquare().spriteBake(sprite,0);
        if(lit)out.lightmap(200,200,200,200);out.emit();
    }
    private void doubleCross(QuadEmitter out,TextureAtlasSprite sprite,float dx,float dz){
        // Old renderCrossedSquares half-width .45, four surfaces, UV normal orientation.
        float low=.05F,high=.95F;
        for(int diagonal=0;diagonal<2;diagonal++)for(int reverse=0;reverse<2;reverse++){
            float ax=low+dx,bx=high+dx,az=(diagonal==0?low:high)+dz,bz=(diagonal==0?high:low)+dz;
            if(reverse==1){float v=ax;ax=bx;bx=v;v=az;az=bz;bz=v;}
            out.pos(0,ax,1,az).pos(1,ax,0,az).pos(2,bx,0,bz).pos(3,bx,1,bz).cullFace(null).material(tinted).colorIndex(0).color(-1,-1,-1,-1).uvUnitSquare().spriteBake(sprite,0).emit();
        }
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction face,RandomSource random){return base.getQuads(state,face,random);}
    @Override public boolean useAmbientOcclusion(){return false;}
    @Override public boolean isGui3d(){return false;}
    @Override public boolean usesBlockLight(){return base.usesBlockLight();}
    @Override public boolean isCustomRenderer(){return false;}
    @Override public TextureAtlasSprite getParticleIcon(){return base.getParticleIcon();}
    @Override public ItemTransforms getTransforms(){return base.getTransforms();}
    @Override public ItemOverrides getOverrides(){return base.getOverrides();}
}
