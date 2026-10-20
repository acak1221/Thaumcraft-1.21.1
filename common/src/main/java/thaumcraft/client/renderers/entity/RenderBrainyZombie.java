package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.monster.EntityBrainyZombie;
import thaumcraft.common.entities.monster.EntityGiantBrainyZombie;

/** Original bzombie/bzombievil sheets, 1.7 villager head/nose UVs and anger-only giant visual scale. */
public final class RenderBrainyZombie extends HumanoidMobRenderer<EntityBrainyZombie, ZombieModel<EntityBrainyZombie>> {
    private static final ResourceLocation NORMAL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/bzombie.png");
    private static final ResourceLocation VILLAGER = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/bzombievil.png");
    private final ZombieModel<EntityBrainyZombie> normal, villager;
    public RenderBrainyZombie(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), .5F);
        normal = model; villager = new ZombieModel<>(villagerLayer().bakeRoot());
        addLayer(new HumanoidArmorLayer<>(this, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)), context.getModelManager()));
    }
    private static LayerDefinition villagerLayer() {
        var mesh = net.minecraft.client.model.HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 32).addBox(-4, -10, -4, 8, 10, 8)
                .texOffs(24, 32).addBox(-1, -3, -6, 2, 4, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
    @Override public ResourceLocation getTextureLocation(EntityBrainyZombie zombie) { return zombie.isVillagerVariant() ? VILLAGER : NORMAL; }
    @Override protected void scale(EntityBrainyZombie zombie, PoseStack pose, float partial) {
        if (zombie instanceof EntityGiantBrainyZombie giant) { float scale = giant.getAnger(); pose.scale(scale, scale, scale); }
    }
    @Override public void render(EntityBrainyZombie zombie, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        model = zombie.isVillagerVariant() ? villager : normal;
        super.render(zombie, yaw, partial, pose, buffers, light);
    }
}
