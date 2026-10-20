package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.monster.EntityTaintVillager;

/**
 * Original TC4 RenderTaintVillager: the vanilla villager mesh with the taint sheet at
 * 0.9375 scale. The original villager hat, profession and unhappy face were suppressed
 * by the original renderer; the native villager model keeps its own hat layer.
 */
public final class RenderTaintVillager extends MobRenderer<EntityTaintVillager, VillagerModel<EntityTaintVillager>> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/villager.png");

    public RenderTaintVillager(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), .5F);
    }

    @Override public ResourceLocation getTextureLocation(EntityTaintVillager villager) { return SKIN; }

    /** Original func_40292_b:74-77. */
    @Override protected void scale(EntityTaintVillager villager, PoseStack pose, float partial) {
        pose.scale(.9375F, .9375F, .9375F);
    }
}
