/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.model.kami;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * The wings of the Robes of the Stratosphere. 1.7.10 made them two flat boxes (0 x 7 x 12) on the body of a biped model
 * that hid everything else; the same boxes, with the same texture offsets and rest poses, are the model layer here, drawn
 * over the body by {@code WingsLayer} while the robes are worn.
 */
public final class ModelWings {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LibMisc.id("kami_wings"), "main");
    public static final String WING_1 = "wing1";
    public static final String WING_2 = "wing2";
    /** Rest angles of the wings around the vertical axis (1.7.10 {@code setRotation}). */
    public static final float WING_1_REST = -0.6108652F;
    public static final float WING_2_REST = 0.4468043F;

    private ModelWings() {}

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(WING_1, CubeListBuilder.create().texOffs(16, -12).addBox(0F, 0F, 0F, 0F, 7F, 12F),
                PartPose.offsetAndRotation(-2F, 1F, 2F, 0F, WING_1_REST, 0F));
        root.addOrReplaceChild(WING_2, CubeListBuilder.create().texOffs(16, -12).addBox(0.1F, 0F, 0F, 0F, 7F, 12F),
                PartPose.offsetAndRotation(2F, 1F, 2F, 0F, WING_2_REST, 0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
