package thaumcraft.client.lib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/** Pickaxe scan: oracle RenderEventHandler:366-501, ItemElementalPickaxe:83-95.
 * The odd negative first-250ms fade and negative attenuation outside radius eight are deliberate.
 * The original clamps alpha only when submitting colour, not before multiplying the two factors. */
public final class ScanOverlay {
    public static final int RANGE = 8, SIZE = 17;
    public static final long DURATION_MILLIS = 5000L;
    public static final int WATER = -5, LAVA = -10, NO_VALUE = -1;
    public static final int WATER_COLOUR = 3986684, LAVA_COLOUR = 16734721;
    public static final int INNER_COLOUR = 0xAAAA11, OUTER_COLOUR = 0xAA1122;
    public static final int FRAMES = 32;
    // UtilsFX:209 and RenderEventHandler:498. These ARE representable native lightmap coordinates.
    public static final int ORE_LIGHT = 220, FLUID_LIGHT = 200;
    private static final TagKey<Item> ORES = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "ores"));
    private static final ResourceLocation GLASS = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/wardedglass.png");
    private static final int[] VALUES = new int[SIZE * SIZE * SIZE];
    private static ClientLevel scanLevel;
    private static long deadline;
    private static BlockPos origin = BlockPos.ZERO;

    private ScanOverlay() {}

    /** RenderEventHandler:427-435. No early clamp: negative * negative can be positive. */
    public static float alpha(long remaining) {
        float alpha = 1;
        if (remaining > 4750L) alpha = 1 - (remaining - 4750L) / 5F;
        if (remaining < 1500L) alpha = remaining / 1500F;
        return alpha;
    }

    public static float distanceAlpha(int x, int y, int z) {
        return 1 - (x * x + y * y + z * z) / 64F;
    }

    public static float cellAlpha(long remaining, int x, int y, int z) {
        return alpha(remaining) * distanceAlpha(x, y, z);
    }

    public static int specialColour(int value) {
        return value == WATER ? WATER_COLOUR : value == LAVA ? LAVA_COLOUR : -1;
    }

    public static float innerScale(int value) { return .2F * (value / 7F); }
    public static float outerScale(int value) { return .5F * (value / 7F); }
    public static int frame(long now) { return (int) (now / 50L % 32L); }
    public static boolean isDrawn(int value) { return value == WATER || value == LAVA || value >= 0; }

    public static void start(Player player, BlockPos pos, long end) {
        if (!(player.level() instanceof ClientLevel level)) return;
        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = -RANGE; y <= RANGE; y++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    BlockPos sample = pos.offset(x, y, z);
                    VALUES[index(x, y, z)] = level.hasChunkAt(sample) && !level.isOutsideBuildHeight(sample)
                            ? classify(level.getBlockState(sample), level) : NO_VALUE;
                }
            }
        }
        origin = pos.immutable();
        deadline = end;
        scanLevel = level;
    }

    public static void clear() { scanLevel = null; deadline = 0; }

    private static int classify(BlockState state, ClientLevel level) {
        if (state.isAir() || state.is(Blocks.BEDROCK)) return NO_VALUE;
        // Original material tests, not waterlogged solids. Tags include flowing fluid states.
        if (state.getBlock() instanceof LiquidBlock) {
            if (state.getFluidState().is(FluidTags.LAVA)) return LAVA;
            if (state.getFluidState().is(FluidTags.WATER)) return WATER;
            return NO_VALUE;
        }
        ItemStack stack = new ItemStack(state.getBlock());
        // OreDictionary names containing ORE migrate to the conventional ore tag.
        if (stack.isEmpty() || !stack.is(ORES)) return NO_VALUE;
        // Original sets zero before attempting either aspect query (RenderEventHandler:390-406).
        try { return ObjectAspectResolver.scanTags(stack, level).visSize(); }
        catch (Exception ignored) { return 0; }
    }

    private static int index(int x, int y, int z) {
        return ((x + RANGE) * SIZE + y + RANGE) * SIZE + z + RANGE;
    }

    /** Called after world markers with the un-translated world-view pose.
     * Use the actual camera (including third person), not player feet, in the native world pass. */
    public static void render(PoseStack pose, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        if (mc.level != scanLevel || now >= deadline) { clear(); return; }
        if (scanLevel == null || mc.player == null || pose == null) return;
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        int frame = frame(now);
        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = -RANGE; y <= RANGE; y++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    int value = VALUES[index(x, y, z)];
                    if (!isDrawn(value)) continue;
                    float alpha = cellAlpha(deadline - now, x, y, z);
                    pose.pushPose();
                    try {
                        pose.translate(origin.getX() + x + .5 - camera.x,
                                origin.getY() + y + .5 - camera.y, origin.getZ() + z + .5 - camera.z);
                        int colour = specialColour(value);
                        if (colour >= 0) drawFaces(pose, colour, alpha);
                        else {
                            pose.mulPose(mc.gameRenderer.getMainCamera().rotation());
                            strip(pose, innerScale(value), alpha, frame, INNER_COLOUR);
                            strip(pose, outerScale(value), alpha, frame, OUTER_COLOUR);
                        }
                    } finally { pose.popPose(); }
                }
            }
        }
    }

    private static void strip(PoseStack pose, float size, float alpha, int frame, int colour) {
        VisNetRender.drawScan(VisNetRender.NODES, false, out -> {
            float u0 = frame / 32F, u1 = (frame + 1) / 32F;
            vertex(out, pose, -size, -size, u1, 1 / 32F, colour, alpha, ORE_LIGHT);
            vertex(out, pose, -size, size, u1, 0, colour, alpha, ORE_LIGHT);
            vertex(out, pose, size, size, u0, 0, colour, alpha, ORE_LIGHT);
            vertex(out, pose, size, -size, u0, 1 / 32F, colour, alpha, ORE_LIGHT);
        });
    }

    private static void drawFaces(PoseStack pose, int colour, float alpha) {
        for (Direction direction : Direction.values()) {
            pose.pushPose();
            try {
                pose.mulPose(Axis.of(new org.joml.Vector3f(-direction.getStepY(),
                        direction.getStepX(), -direction.getStepZ())).rotationDegrees(90));
                pose.translate(0, 0, direction.getStepZ() < 0 ? .5 : -.5);
                VisNetRender.drawScan(GLASS, true, out -> {
                    vertex(out, pose, -.5F, .5F, 0, 1, colour, alpha, FLUID_LIGHT);
                    vertex(out, pose, .5F, .5F, 1, 1, colour, alpha, FLUID_LIGHT);
                    vertex(out, pose, .5F, -.5F, 1, 0, colour, alpha, FLUID_LIGHT);
                    vertex(out, pose, -.5F, -.5F, 0, 0, colour, alpha, FLUID_LIGHT);
                });
            } finally { pose.popPose(); }
        }
    }

    private static void vertex(VertexConsumer out, PoseStack pose, float x, float y, float u, float v,
                               int colour, float alpha, int light) {
        VisNetRender.vertex(out, pose, new Vec3(x, y, 0), u, v, colour, alpha);
        out.setLight(light);
    }
}
