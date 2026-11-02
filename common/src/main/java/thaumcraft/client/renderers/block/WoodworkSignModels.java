package thaumcraft.client.renderers.block;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Set;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.WoodType;

/** Original native sign geometry with block-sprite UVs, and separate native alpha chain art. */
public final class WoodworkSignModels {
    private static SignRenderer.SignModel standing;
    private static net.minecraft.client.model.geom.ModelPart hanging;
    private WoodworkSignModels() {}

    public static SignRenderer.SignModel standing() {
        if (standing == null) standing = new SignRenderer.SignModel(standingLayer().bakeRoot());
        return standing;
    }

    public static LayerDefinition standingLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("sign", box(-12, -14, -1, 24, 12, 2), PartPose.ZERO);
        root.addOrReplaceChild("stick", box(-1, -2, -1, 2, 14, 2), PartPose.ZERO);
        // Vanilla scales standing signs by 2/3. Twenty-four model pixels are
        // one block, so a 16px plank covers the board at its natural world scale.
        return LayerDefinition.create(mesh, 24, 24);
    }

    public static LayerDefinition hangingLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("board", box(-7, 0, -1, 14, 10, 2), PartPose.ZERO);
        root.addOrReplaceChild("plank", box(-8, -6, -2, 16, 2, 4), PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
    }

    /** Each face samples its own bounded patch; a 64x32 entity unwrap cannot sample a block sprite. */
    private static CubeListBuilder box(float x, float y, float z, int width, int height, int depth) {
        var cubes = CubeListBuilder.create();
        face(cubes, Direction.NORTH, -depth, -depth, x, y, z, width, height, depth);
        face(cubes, Direction.SOUTH, -2 * depth - width, -depth, x, y, z, width, height, depth);
        face(cubes, Direction.WEST, 0, -depth, x, y, z, width, height, depth);
        face(cubes, Direction.EAST, -depth - width, -depth, x, y, z, width, height, depth);
        face(cubes, Direction.UP, -depth - width, 0, x, y, z, width, height, depth);
        face(cubes, Direction.DOWN, -depth, 0, x, y, z, width, height, depth);
        return cubes;
    }

    private static void face(CubeListBuilder cubes, Direction face, int u, int v,
                             float x, float y, float z, int width, int height, int depth) {
        cubes.texOffs(u, v).addBox(x, y, z, width, height, depth, Set.of(face));
    }

    public static void renderStanding(PoseStack pose, MultiBufferSource buffers, Material wood,
                                      int light, int overlay, boolean showStick) {
        var model = standing();
        model.stick.visible = showStick;
        model.root.render(pose, wood.buffer(buffers, RenderType::entityCutoutNoCull), light, overlay);
    }

    public static void renderHanging(PoseStack pose, MultiBufferSource buffers, Material wood,
                                     int light, int overlay, HangingSignRenderer.HangingSignModel nativeModel) {
        if (hanging == null) hanging = hangingLayer().bakeRoot();
        hanging.getChild("plank").visible = nativeModel.plank.visible;
        hanging.render(pose, wood.buffer(buffers, RenderType::entityCutoutNoCull), light, overlay);
        // The zero-depth native chain planes require their 64x32 alpha sheet.
        // Only those chain parts use vanilla's material; the wood always uses
        // the actual magical plank sprite. Native attachment visibility stays intact.
        var chains = Sheets.getHangingSignMaterial(WoodType.OAK).buffer(buffers, RenderType::entityCutoutNoCull);
        nativeModel.normalChains.render(pose, chains, light, overlay);
        nativeModel.vChains.render(pose, chains, light, overlay);
    }
}
