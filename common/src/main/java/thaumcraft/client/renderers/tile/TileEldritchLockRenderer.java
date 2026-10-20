package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.renderers.models.ModelCube;
import thaumcraft.client.renderers.models.ModelTunnelField;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.tiles.TileEldritchLock;

/**
 * Original TileEldritchLockRenderer: the eldritch lock.
 *
 * <p>The original stacks little cubes out of the {@code eldritch_cube.png}
 * texture around the lock, shows the runed tablet in front of an unlocked lock
 * and draws the fixed function tunnel on the face the lock points at.
 */
public final class TileEldritchLockRenderer implements BlockEntityRenderer<TileEldritchLock> {
    /** The original texture of the cubes of the lock. */
    public static final ResourceLocation CUBE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/eldritch_cube.png");
    /** The original range of the detailed effect, in blocks. */
    private static final double IN_RANGE = 512.0D;

    private final ModelCube cube = new ModelCube(0);
    private final ModelTunnelField tunnel = new ModelTunnelField();

    public TileEldritchLockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileEldritchLock tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (tile.getLevel() == null) return;
        Direction facing = Direction.from3DDataValue(tile.getFacing());
        var player = Minecraft.getInstance().player;
        float count = (player == null ? 0.0F : player.tickCount + partialTick);
        var out = buffers.getBuffer(RenderType.entityCutoutNoCull(CUBE));
        pose.pushPose();
        try {
            pose.translate(.5F, .5F, .5F);
            for (int side = 0; side < 4; side++) {
                pose.pushPose();
                try {
                    around(pose, facing, side * 90.0F);
                    // The original draws one cube less for every five ticks of the countdown.
                    int limit = 5 - (tile.count + side * 5) / 20;
                    for (int step = 1; step < limit; step++) {
                        pose.pushPose();
                        try {
                            pose.translate(0.0F, .25F + .5F * step, 0.0F);
                            float wobble = Mth.sin((count + step * 10.0F + side * 20.0F) / 20.0F) * .1F;
                            if (step == 1 || step == 4) wobble = wobble / 2.0F + .2F;
                            pose.scale(.5F + wobble, .5F, .5F + wobble);
                            cube.render(pose, out, light, overlay, 0xffffffff);
                        } finally {
                            pose.popPose();
                        }
                    }
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            pose.popPose();
        }
        // The original shows the runed tablet in front of a lock that is not counting down.
        if (tile.count >= 0) {
            ItemStack tablet = new ItemStack(ThaumcraftLegacyItems.ELDRITCH_TABLET.get());
            pose.pushPose();
            try {
                pose.translate(.5F + facing.getStepX() * .525F, .285F, .5F + facing.getStepZ() * .525F);
                if (facing == Direction.EAST) pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
                else if (facing == Direction.WEST) pose.mulPose(Axis.ZP.rotationDegrees(270.0F));
                else if (facing == Direction.NORTH) pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                Minecraft.getInstance().getItemRenderer()
                        .renderStatic(tablet, ItemDisplayContext.FIXED, light, overlay, pose, buffers,
                                tile.getLevel(), 0);
            } finally {
                pose.popPose();
            }
        }
        // The tunnel of the face the lock points at.
        pose.pushPose();
        try {
            pose.translate(0.0F, 0.0F, 0.0F);
            pose.scale(1.0F, 3.0F, 1.0F);
            boolean inRange = player == null || player.distanceToSqr(Vec3.atCenterOf(tile.getBlockPos())) < IN_RANGE * IN_RANGE;
            Direction plane = switch (facing) {
                case NORTH -> Direction.NORTH;
                case SOUTH -> Direction.SOUTH;
                case WEST -> Direction.WEST;
                case EAST -> Direction.EAST;
                default -> Direction.NORTH;
            };
            if (inRange) tunnel.renderFace(pose, buffers, plane, 1.0F, light, overlay);
            else tunnel.renderFarFace(pose, buffers, plane, light, overlay);
        } finally {
            pose.popPose();
        }
    }

    /** The original rotation of the cube ring around the axis of the facing. */
    private static void around(PoseStack pose, Direction facing, float degrees) {
        // The original rotates around the offset vector of the facing.
        if (facing.getAxis() == Direction.Axis.Y) pose.mulPose(Axis.YP.rotationDegrees(degrees));
        else if (facing.getAxis() == Direction.Axis.Z) pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        else pose.mulPose(Axis.XP.rotationDegrees(degrees));
    }
}
