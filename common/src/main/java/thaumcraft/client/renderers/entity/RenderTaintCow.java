package thaumcraft.client.renderers.entity;

import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.monster.EntityTaintCow;

/** Original TC4 RenderTaintCow: the vanilla cow mesh with the taint sheet. */
public final class RenderTaintCow extends MobRenderer<EntityTaintCow, CowModel<EntityTaintCow>> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/cow.png");

    public RenderTaintCow(EntityRendererProvider.Context context) {
        super(context, new CowModel<>(context.bakeLayer(ModelLayers.COW)), .7F);
    }

    @Override public ResourceLocation getTextureLocation(EntityTaintCow cow) { return SKIN; }
}
