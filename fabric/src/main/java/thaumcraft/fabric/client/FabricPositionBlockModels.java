package thaumcraft.fabric.client;

import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.client.renderers.block.PositionBlockModels;

/** Indigo bypasses ModelBlockRenderer, so emit the same shared world quads here. */
public record FabricPositionBlockModels(BakedModel base) implements BakedModel, FabricBakedModel {
    public static void init() {
        ModelLoadingPlugin.register(plugin -> plugin.modifyModelAfterBake().register((model, context) -> {
            if (model == null || context.topLevelId() == null
                    || !context.topLevelId().id().getNamespace().equals("thaumcraft")
                    || context.topLevelId().variant().equals("inventory")) return model;
            String path = context.topLevelId().id().getPath();
            if (path.equals("tallow_candle") || path.endsWith("_tallow_candle")
                    || path.equals("eldritch_crust") || path.equals("eldritch_deco") || path.equals("eldritch_stairs")
                    || path.equals("taint_crust") || path.equals("taint_soil") || path.equals("taint_fibres")
                    || path.equals("flux_gas") || path.equals("flux_goo") || path.endsWith("_infused_stone")) {
                return new FabricPositionBlockModels(model);
            }
            return model;
        }));
    }
    @Override public boolean isVanillaAdapter() { return false; }
    @Override public void emitBlockQuads(BlockAndTintGetter world, BlockState state, BlockPos pos,
            Supplier<RandomSource> random, RenderContext context) {
        var model = PositionBlockModels.worldModel(world,pos,state,base);
        var emitter = context.getEmitter();
        var renderer = java.util.Objects.requireNonNull(RendererAccess.INSTANCE.getRenderer());
        var shaded = renderer.materialFinder().ambientOcclusion(net.fabricmc.fabric.api.util.TriState.FALSE).find();
        var flat = renderer.materialFinder().ambientOcclusion(net.fabricmc.fabric.api.util.TriState.FALSE).disableDiffuse(true).find();
        for (BakedQuad quad : model.getQuads(state,null,random.get())) {
            emitter.fromVanilla(quad,quad.isShade() ? shaded : flat,null);
            if (quad instanceof thaumcraft.client.renderers.block.LegacyBlockQuads.LitQuad lit)
                emitter.lightmap(lit.light(),lit.light(),lit.light(),lit.light());
            emitter.emit();
        }
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction face,RandomSource random) { return base.getQuads(state,face,random); }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return base.isGui3d(); }
    @Override public boolean usesBlockLight() { return base.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return base.getParticleIcon(); }
    @Override public ItemTransforms getTransforms() { return base.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return base.getOverrides(); }
}
