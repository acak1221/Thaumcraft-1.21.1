package thaumcraft.client.renderers.tile;

import net.minecraft.core.Direction;

/** Oracle TileThaumatoriumRenderer:53-73,81-99; no block or world construction. */
public final class ThaumatoriumPose {
    public static final float MODEL_PITCH = -90, PREVIEW_Y = 1.325F, PREVIEW_SCALE = .75F;
    private ThaumatoriumPose() {}
    public static float modelRoll(Direction facing) {
        return switch (facing) { case NORTH -> 270; case SOUTH -> 90; case EAST -> 180; default -> 0; };
    }
    public static float previewYaw(Direction facing) {
        return switch (facing) { case EAST -> 90; case WEST -> 270; case NORTH -> 180; default -> 0; };
    }
    public static int previewIndex(int ticks, int count) { return count == 0 ? -1 : ticks / 40 % count; }
}
