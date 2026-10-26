package thaumcraft.client.renderers.models.gear;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Original ModelWand46-81 (oracle
 * {@code thaumcraft/client/renderers/models/gear/ModelWand.java}): the rod, the cap,
 * the cap's lower half and the focus cube of a wand.
 *
 * <p>The original model texture is 32x32 and every part calls
 * {@code setTextureSize(64,32)} AFTER its {@code addBox}. 1.7.10's ModelBox
 * reads the texture size while the box is built, so every cube keeps the
 * 32x32 normalization: native layers use 32x32, exactly like the retained
 * {@code PrimalWandFocusRenderer} focus shell.
 *
 * <p>The original also assigns {@code mirror=true} after each {@code addBox},
 * so the built cubes keep their plain UVs and native {@code .mirror()} is not used.
 */
public final class ModelWand {
    /** The focus uses the shared wand sheet; the rod and cap use their own material sheets. */
    public static final ResourceLocation FOCUS_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand.png");
    private final ModelPart rod, cap, capBottom, focus;

    public ModelWand() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -1, -1, 2, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("cap_bottom", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -1, -1, 2, 2, 2), PartPose.offset(0, 20, 0));
        root.addOrReplaceChild("rod", CubeListBuilder.create().texOffs(0, 8).addBox(-1, -1, -1, 2, 18, 2), PartPose.offset(0, 2, 0));
        root.addOrReplaceChild("focus", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -6, -3, 6, 6, 6), PartPose.ZERO);
        ModelPart parts = LayerDefinition.create(mesh, 32, 32).bakeRoot();
        cap = parts.getChild("cap");
        capBottom = parts.getChild("cap_bottom");
        rod = parts.getChild("rod");
        focus = parts.getChild("focus");
    }

    /** The retained rod sheet of a wand material, e.g. {@code wand_rod_wood.png}. */
    public static ResourceLocation rodTexture(String baseTag) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + baseTag + ".png");
    }

    /** The retained cap sheet of a wand material, e.g. {@code wand_cap_iron.png}. */
    public static ResourceLocation capTexture(String tag) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_cap_" + tag + ".png");
    }

    /** Original render110: the rod, in the rod material's own sheet. */
    public void renderRod(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        rod.render(pose, vertices, light, overlay, -1);
    }

    /** Original render136: the cap's upper half. */
    public void renderCap(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        cap.render(pose, vertices, light, overlay, -1);
    }

    /** Original render146: the cap's lower half. */
    public void renderCapBottom(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        capBottom.render(pose, vertices, light, overlay, -1);
    }

    /** Original render206: the focus cube, drawn with the focus colour. */
    public void renderFocus(PoseStack pose, VertexConsumer vertices, int light, int overlay, int argb) {
        focus.render(pose, vertices, light, overlay, argb);
    }

    /**
     * The original's wand transforms, isolated from GL state so they can be
     * checked without a client: the staff stretch of the rod, the cap's own
     * scale and the focus' scale.
     */
    public static final class Geometry {
        /** Original render107-108: a staff's rod is stretched and shifted. */
        public static final float STAFF_ROD_XZ = 1.2F, STAFF_ROD_Y = 2.0F, STAFF_ROD_SHIFT = -.1F;
        /** Original render120-123: the cap's scale, staff and wand. */
        public static final float STAFF_CAP_XZ = 1.3F, STAFF_CAP_Y = 1.1F, WAND_CAP_XZ = 1.2F, WAND_CAP_Y = 1.0F;
        /** Original render126-134: the sceptre draws the cap twice, the second one higher and flat. */
        public static final float SCEPTRE_CAP_SCALE = 1.3F, SCEPTRE_CAP_SHIFT_Y = .3F, SCEPTRE_CAP_SCALE_Y = .66F;
        /** Original render139-144: the staff's extra cap ring. */
        public static final float STAFF_RING_Y = .66F, STAFF_RING_SHIFT = .225F, STAFF_BOTTOM_SHIFT = .65F;
        /** Original render194-199: the focus scale, staff and wand. */
        public static final float STAFF_FOCUS_XZ = .525F, STAFF_FOCUS_Y = .5525F, STAFF_FOCUS_SHIFT = -.0475F, WAND_FOCUS = .5F;
        /** Original render175-181: the focus depth layer scale. */
        public static final float STAFF_DEPTH_XZ = .165F, STAFF_DEPTH_Y = .1765F, STAFF_DEPTH_SHIFT = -.15F;
        public static final float WAND_DEPTH = .16F, WAND_DEPTH_SHIFT = -.09F;
        /** Original render180-199: the ornament of a focus is drawn twice, the second turned 90 degrees, both shifted and halved. */
        public static final float ORNAMENT_SHIFT_X = -.25F, ORNAMENT_SHIFT_Y = -.1F, ORNAMENT_SHIFT_Z = .0275F, ORNAMENT_SCALE = .5F,
                ORNAMENT_THICKNESS = .1F;
        private Geometry() {}

        /**
         * Original render87-101: a glowing rod ({@code WandRod.isGlowing}) is drawn with the raw lightmap value
         * {@code (int) (200 + sin(ticks) * 5 + 5)} of the client player's ticks, block light only; 1.7.10's raw value is the
         * modern packed light (block light in the low sixteen bits).
         */
        public static int glowLight(int ticks) { return (int) (200.0F + net.minecraft.util.Mth.sin(ticks) * 5.0F + 5.0F); }
    }
}
