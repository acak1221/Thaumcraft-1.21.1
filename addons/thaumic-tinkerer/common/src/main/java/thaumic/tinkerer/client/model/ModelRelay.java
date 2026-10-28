/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [12 Sep 2013, 17:56:17 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The model of the Levitational Locomotive Relay (a base plate, a magnet and four panels), as the 1.7.10
 * {@code ModelRelay} defined it. The 1.7.10 port did not use it (see {@link thaumic.tinkerer.client.render.tile.RenderTileRelay}).
 */
public class ModelRelay {

    private final ModelPart root;

    public ModelRelay(ModelPart root) {
        this.root = root;
    }

    /** The layer of the original: a 64 by 64 texture and seven boxes. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("panel", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 14, 2, 14),
                PartPose.offsetAndRotation(-7F, 22F, -7F, 0F, 0F, 0F));
        root.addOrReplaceChild("magnet", CubeListBuilder.create().texOffs(0, 16).addBox(0F, 0F, 0F, 4, 13, 4),
                PartPose.offsetAndRotation(-2F, 9F, -2F, 0F, 0F, 0F));
        root.addOrReplaceChild("box1", CubeListBuilder.create().texOffs(28, 19).addBox(0F, -2F, 0F, 6, 14, 0),
                PartPose.offsetAndRotation(3F, 10F, -3F, 0F, -1.570796F, 0F));
        root.addOrReplaceChild("box2", CubeListBuilder.create().texOffs(28, 33).addBox(0F, 0F, 0F, 6, 14, 0),
                PartPose.offsetAndRotation(-3F, 8F, 3F, 0F, 1.570796F, 0F));
        root.addOrReplaceChild("box3", CubeListBuilder.create().texOffs(40, 19).addBox(0F, 0F, 0F, 6, 14, 0),
                PartPose.offsetAndRotation(3F, 8F, 3F, 0F, 3.141593F, 0F));
        root.addOrReplaceChild("box4", CubeListBuilder.create().texOffs(40, 33).addBox(0F, 0F, 0F, 6, 14, 0),
                PartPose.offsetAndRotation(-3F, 8F, -3F, 0F, 0F, 0F));
        root.addOrReplaceChild("box5", CubeListBuilder.create().texOffs(28, 49).addBox(0F, 0F, 0F, 6, 0, 6),
                PartPose.offsetAndRotation(-3F, 8F, -3F, 0F, 0F, 0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        root.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
