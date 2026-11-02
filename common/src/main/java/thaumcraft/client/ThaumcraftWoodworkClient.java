package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.WoodType;
import thaumcraft.common.config.ThaumcraftWoodwork;
import thaumcraft.client.renderers.block.WoodworkSignModels;

/** Native sign models/text/editing, with the real magical planks instead of unrelated vanilla wood art. */
public final class ThaumcraftWoodworkClient {
    private ThaumcraftWoodworkClient() {}
    public static void init() {
        BlockEntityRendererRegistry.register(ThaumcraftWoodwork.SIGN_TYPE.get(), context -> {
            var renderer = new Sign(context);
            return (tile, partial, pose, buffers, light, overlay) -> renderer.render(tile, partial, pose, buffers, light, overlay);
        });
        BlockEntityRendererRegistry.register(ThaumcraftWoodwork.HANGING_SIGN_TYPE.get(), context -> {
            var renderer = new Hanging(context);
            return (tile, partial, pose, buffers, light, overlay) -> renderer.render(tile, partial, pose, buffers, light, overlay);
        });
        for (var family : ThaumcraftWoodwork.FAMILIES.values()) {
            // Keep material lookup consistent for clients of the native sign API.
            net.minecraft.client.renderer.Sheets.SIGN_MATERIALS.put(family.woodType(), material(family.woodType()));
            net.minecraft.client.renderer.Sheets.HANGING_SIGN_MATERIALS.put(family.woodType(), material(family.woodType()));
            RenderTypeRegistry.register(RenderType.cutout(), family.door().get(), family.trapdoor().get());
        }
    }
    public static Material material(WoodType type) {
        var family = ThaumcraftWoodwork.family(type);
        if (family == null) throw new IllegalArgumentException("Not a registered magical wood: " + type);
        return new Material(TextureAtlas.LOCATION_BLOCKS,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/planks_" + family.name()));
    }
    private static final class Sign extends SignRenderer {
        private Sign(BlockEntityRendererProvider.Context context) { super(context); }
        @Override public Material getSignMaterial(WoodType type) { return material(type); }
        @Override public void renderSign(PoseStack pose, MultiBufferSource buffers, int light, int overlay, WoodType type, Model nativeModel) {
            pose.pushPose();
            float scale = getSignModelRenderScale();
            pose.scale(scale, -scale, -scale);
            WoodworkSignModels.renderStanding(pose, buffers, material(type), light, overlay, ((SignRenderer.SignModel) nativeModel).stick.visible);
            pose.popPose();
        }
    }
    private static final class Hanging extends HangingSignRenderer {
        private Hanging(BlockEntityRendererProvider.Context context) { super(context); }
        @Override public Material getSignMaterial(WoodType type) { return material(type); }
        @Override public void renderSign(PoseStack pose, MultiBufferSource buffers, int light, int overlay, WoodType type, Model nativeModel) {
            pose.pushPose();
            float scale = getSignModelRenderScale();
            pose.scale(scale, -scale, -scale);
            WoodworkSignModels.renderHanging(pose, buffers, material(type), light, overlay, (HangingSignRenderer.HangingSignModel) nativeModel);
            pose.popPose();
        }
    }
}
