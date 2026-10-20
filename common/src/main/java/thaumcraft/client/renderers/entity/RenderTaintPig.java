package thaumcraft.client.renderers.entity;

import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.monster.EntityTaintPig;

/** Original TC4 RenderTaintPig: the vanilla pig mesh with the taint sheet. */
public final class RenderTaintPig extends MobRenderer<EntityTaintPig, PigModel<EntityTaintPig>> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/pig.png");

    public RenderTaintPig(EntityRendererProvider.Context context) {
        super(context, new PigModel<>(context.bakeLayer(ModelLayers.PIG)), .7F);
    }

    @Override public ResourceLocation getTextureLocation(EntityTaintPig pig) { return SKIN; }
}
