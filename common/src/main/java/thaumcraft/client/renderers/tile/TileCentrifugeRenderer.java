package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.tiles.TileCentrifuge;

/** Original ModelCentrifuge:23-72 and TileCentrifugeRenderer:26-34, checked
 * against pinned OpenTC4. The six measured boxes use the retained WORLD sheet;
 * centrifuge_inventory.png is not referenced by either original render path. */
public final class TileCentrifugeRenderer implements BlockEntityRenderer<TileCentrifuge> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/centrifuge.png");
    private record Parts(ModelPart fixed, ModelPart rotor) {}
    private static final class Model { private static final Parts PARTS = bake(); }
    public TileCentrifugeRenderer(BlockEntityRendererProvider.Context context) {}

    private static Parts bake() {
        var mesh = new MeshDefinition(); var root = mesh.getRoot();
        root.addOrReplaceChild("fixed", CubeListBuilder.create()
                .texOffs(20, 16).addBox(-4, -8, -4, 8, 4, 8)
                .texOffs(20, 16).addBox(-4, 4, -4, 8, 4, 8), PartPose.ZERO);
        root.addOrReplaceChild("rotor", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-4, -1, -1, 8, 2, 2)
                .texOffs(0, 16).addBox(4, -3, -2, 4, 6, 4)
                .texOffs(0, 16).addBox(-8, -3, -2, 4, 6, 4)
                .texOffs(0, 0).addBox(-1.5F, -4, -1.5F, 3, 8, 3), PartPose.ZERO);
        // Legacy mirror=true followed addBox; those existing cubes were NOT
        // mirrored. Native addBox vertices already use model units /16.
        ModelPart baked = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        return new Parts(baked.getChild("fixed"), baked.getChild("rotor"));
    }

    @Override public void render(TileCentrifuge centrifuge, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderModel(centrifuge.getRotation(), pose, buffers, light, overlay);
    }

    /** Six-argument native builtin hook. SAME six boxes/sheet at original fresh
     * rotation0, without allocating a fake BE/world or reading item-facing data. */
    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (stack.is(ThaumcraftTubes.CENTRIFUGE_ITEM.get())) renderModel(0, pose, buffers, light, overlay);
    }

    private static void renderModel(float rotation, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose(); try { pose.translate(.5, .5, .5);
            var vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            Model.PARTS.fixed().render(pose, vertices, light, overlay);
            // Original transient rotation only: no facing transform, interpolation,
            // animation reset, fabricated motion or render-triggered pump sound.
            pose.mulPose(Axis.YP.rotationDegrees(rotation));
            Model.PARTS.rotor().render(pose, vertices, light, overlay);
        } finally {
            pose.popPose();
        }
    }
}
