package thaumcraft.client.lib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Locale;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/**
 * The scanned-block overlay of the Pickaxe of the Core.
 *
 * <p>Original {@code thaumcraft/common/items/equipment/ItemElementalPickaxe.java:83-95}
 * ({@code onItemUse}): a right click on the client calls
 * {@code renderEventHandler.startScan(player, x, y, z, now + 5000, 8)} and swings the arm.
 * {@code RenderEventHandler.java:366-415} ({@code startScan}) samples the {@code 17x17x17} cube
 * around the clicked block and stores one integer per cell: {@code -1} for air, bedrock and every
 * ordinary block, {@code -10} for lava, {@code -5} for water and the vis size of the scan aspects
 * for an ore ({@code -1} when the ore has no aspects).</p>
 *
 * <p>{@code RenderEventHandler.java:417-462} ({@code showScannedBlocks}) draws the cube while
 * {@code scanCount > now}: the alpha fades in over the first 250 ms of the deadline
 * ({@code dif > 4750 -> 1 - (dif - 4750) / 5}), fades out over the last 1500 ms
 * ({@code dif < 1500 -> dif / 1500}) and is multiplied by {@code 1 - (dx^2 + dy^2 + dz^2) / 64};
 * water and lava are a quad of {@code textures/blocks/wardedglass.png} on each of the six faces
 * with the fixed colours {@code 3986684} and {@code 16734721}; an ore is two camera-facing strips
 * of {@code textures/misc/nodes.png} with the scales {@code 0.2 * (value / 7)} and
 * {@code 0.5 * (value / 7)}, the colours {@code 0xAAAA11} and {@code 0xAA1122}, and the frame
 * {@code (time / 50) % 32}.</p>
 *
 * <p>The numbers of the original are pure static functions here ({@link #alpha},
 * {@link #distanceAlpha}, {@link #specialColour}, {@link #innerScale}, {@link #outerScale},
 * {@link #frame}) so a test can assert them without a level or a client. The render itself uses
 * the native, unlit additive strip of {@link VisNetRender}, the same owner the port already uses
 * for the node strips; the 1.7.10 fixed-function brightness ({@code 220} of
 * {@code UtilsFX.renderFacingStrip:198}) has no counterpart in the 1.21.1
 * {@code POSITION_TEX_COLOR} effect shader and becomes full brightness, documented in the unit
 * evidence.</p>
 */
public final class ScanOverlay {
    /** Oracle {@code ItemElementalPickaxe.java:92} - the range of the scan. */
    public static final int RANGE = 8;
    /** The side of the cube: {@code 2 * RANGE + 1}, oracle {@code RenderEventHandler.java:367}. */
    public static final int SIZE = 2 * RANGE + 1;
    /** Oracle {@code ItemElementalPickaxe.java:92} - the duration of the scan in milliseconds. */
    public static final long DURATION_MILLIS = 5000L;

    /** Oracle {@code RenderEventHandler.java:423-425} - the end of the fade in, in ms left. */
    public static final long FADE_IN_END = 4750L;
    /** Oracle {@code RenderEventHandler.java:424} - the fade-in divisor. */
    public static final float FADE_IN_DIVISOR = 5.0F;
    /** Oracle {@code RenderEventHandler.java:426-428} - the start of the fade out. */
    public static final long FADE_OUT_START = 1500L;
    /** Oracle {@code RenderEventHandler.java:427} - the fade-out divisor. */
    public static final float FADE_OUT_DIVISOR = 1500.0F;
    /** Oracle {@code RenderEventHandler.java:430} - the radius squared of the attenuation. */
    public static final float DISTANCE_RADIUS_SQUARED = 64.0F;

    /** Oracle {@code RenderEventHandler.java:381} - the value of a water cell. */
    public static final int WATER = -5;
    /** Oracle {@code RenderEventHandler.java:379} - the value of a lava cell. */
    public static final int LAVA = -10;

    /** Oracle {@code RenderEventHandler.java:433} - the colour of the water faces. */
    public static final int WATER_COLOUR = 3986684;
    /** Oracle {@code RenderEventHandler.java:437} - the colour of the lava faces. */
    public static final int LAVA_COLOUR = 16734721;
    /** Oracle {@code RenderEventHandler.java:450} - the inner strip colour {@code 0xAAAA11}. */
    public static final int INNER_COLOUR = 0xAAAA11;
    /** Oracle {@code RenderEventHandler.java:453} - the outer strip colour {@code 0xAA1122}. */
    public static final int OUTER_COLOUR = 0xAA1122;
    /** Oracle {@code RenderEventHandler.java:451,454} - {@code frame = (time / 50) % 32}. */
    public static final int FRAMES = 32;
    public static final long FRAME_MILLIS = 50L;
    /** Oracle {@code RenderEventHandler.java:450-454} - the two strip scales per vis size. */
    public static final float INNER_SCALE = 0.2F;
    public static final float OUTER_SCALE = 0.5F;
    /** Oracle {@code RenderEventHandler.java:450,454} - the vis divisor of the strip scale. */
    public static final float VIS_DIVISOR = 7.0F;

    /** Oracle {@code RenderEventHandler.java:404-406} - the cell of a water/lava cell is skipped. */
    public static final int NO_VALUE = -1;

    /** The {@code #c:ores} item tag: the 1.21.1 equivalent of the OreDictionary "ORE" names. */
    public static final TagKey<Item> ORES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores"));
    private static final ResourceLocation WARDED_GLASS =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/wardedglass.png");

    private static final int[] VALUES = new int[SIZE * SIZE * SIZE];

    private static boolean active;
    private static long deadline;
    private static BlockPos origin = BlockPos.ZERO;

    private ScanOverlay() {}

    // ---- pure rules of the original ---------------------------------------------------------

    /**
     * Oracle {@code RenderEventHandler.java:423-429}: the fade of the scan. {@code dif} is
     * {@code scanCount - now}, i.e. the milliseconds left. The original applies the fade-in test
     * first and the fade-out test second, and does not clamp the result.
     */
    public static float alpha(long dif) {
        float alpha = 1.0F;
        if (dif > FADE_IN_END) alpha = 1.0F - (dif - FADE_IN_END) / FADE_IN_DIVISOR;
        if (dif < FADE_OUT_START) alpha = dif / FADE_OUT_DIVISOR;
        return alpha;
    }

    /** Oracle {@code RenderEventHandler.java:430}: {@code 1 - (dx^2 + dy^2 + dz^2) / 64}. */
    public static float distanceAlpha(int dx, int dy, int dz) {
        return 1.0F - (dx * dx + dy * dy + dz * dz) / DISTANCE_RADIUS_SQUARED;
    }

    /** Oracle {@code RenderEventHandler.java:423-430}: the alpha of one cell of the cube. */
    public static float cellAlpha(long dif, int dx, int dy, int dz) {
        return alpha(dif) * distanceAlpha(dx, dy, dz);
    }

    /** Oracle {@code RenderEventHandler.java:431-441}: the fixed colour of a special cell, or -1. */
    public static int specialColour(int value) {
        if (value == WATER) return WATER_COLOUR;
        if (value == LAVA) return LAVA_COLOUR;
        return -1;
    }

    /** Oracle {@code RenderEventHandler.java:450}: the inner strip is {@code 0.2 * (value / 7)}. */
    public static float innerScale(int visSize) {
        return INNER_SCALE * visSize / VIS_DIVISOR;
    }

    /** Oracle {@code RenderEventHandler.java:454}: the outer strip is {@code 0.5 * (value / 7)}. */
    public static float outerScale(int visSize) {
        return OUTER_SCALE * visSize / VIS_DIVISOR;
    }

    /** Oracle {@code RenderEventHandler.java:451,454}: the animation cell {@code (time / 50) % 32}. */
    public static int frame(long timeMillis) {
        return (int) (timeMillis / FRAME_MILLIS % FRAMES);
    }

    /**
     * Oracle {@code RenderEventHandler.java:431-443}: the cells that are drawn. Water and lava
     * have their own pass; every other cell is drawn only when {@code value >= 0} (an ore).
     */
    public static boolean isDrawn(int value) {
        return value == WATER || value == LAVA || value >= 0;
    }

    /** Whether the cube is drawn at {@code nowMillis}: oracle {@code renderLast:267}. */
    public static boolean isActive(long nowMillis) {
        return active && deadline > nowMillis;
    }

    // ---- the scan ----------------------------------------------------------------------------

    /**
     * Oracle {@code RenderEventHandler.java:366-415}: sample the cube around {@code pos}. The
     * caller passes {@code now + 5000} like {@code ItemElementalPickaxe.onItemUse:92}.
     */
    public static void start(Player player, BlockPos pos, long deadlineMillis) {
        if (!(player.level() instanceof ClientLevel level)) return;
        for (int xx = -RANGE; xx <= RANGE; xx++) {
            for (int yy = -RANGE; yy <= RANGE; yy++) {
                for (int zz = -RANGE; zz <= RANGE; zz++) {
                    BlockPos sample = pos.offset(xx, yy, zz);
                    int value = NO_VALUE;
                    if (!level.isOutsideBuildHeight(sample) && level.hasChunkAt(sample)) {
                        BlockState state = level.getBlockState(sample);
                        // Oracle: air and bedrock are -1.
                        if (!state.isAir() && !state.is(Blocks.BEDROCK)) {
                            value = classify(state, level);
                        }
                    }
                    VALUES[index(xx, yy, zz)] = value;
                }
            }
        }
        origin = pos.immutable();
        deadline = deadlineMillis;
        active = true;
    }

    /** Stop the overlay; a new world or a new level must not keep an old cube. */
    public static void clear() {
        active = false;
        deadline = 0L;
    }

    /**
     * The value of one cell: lava, water, the vis size of an ore, or {@code -1}.
     *
     * <p>The original tests {@code Material.lava}, then {@code Material.water}, then the
     * OreDictionary name for the substring {@code "ORE"}. The native test is the
     * {@link LiquidBlock} with the vanilla fluid — a waterlogged solid block (which 1.7.10 did
     * not have) is not water, matching the original's {@code Material} test — and the
     * {@code #c:ores} item tag for the ore dictionary name.</p>
     */
    private static int classify(BlockState state, ClientLevel level) {
        if (state.getBlock() instanceof LiquidBlock) {
            if (state.getFluidState().is(Fluids.LAVA)) return LAVA;
            if (state.getFluidState().is(Fluids.WATER)) return WATER;
            return NO_VALUE;
        }
        ItemStack stack = new ItemStack(state.getBlock());
        if (stack.isEmpty() || !stack.is(ORES)) return NO_VALUE;
        // Oracle: value = ScanManager.getScanAspects(...).visSize(), and -1 on an exception.
        return ObjectAspectResolver.scanTags(stack, level).visSize();
    }

    private static int index(int xx, int yy, int zz) {
        return ((xx + RANGE) * SIZE + (yy + RANGE)) * SIZE + (zz + RANGE);
    }

    // ---- the render --------------------------------------------------------------------------

    /**
     * Oracle {@code RenderEventHandler.java:417-462}: draw the cube. Call from the late world pass
     * ({@code renderLast:252-268}), after the golem markers, with a pose that is already the
     * camera-relative world pose of that pass.
     */
    public static void render(PoseStack pose, float partialTick) {
        long now = System.currentTimeMillis();
        if (!isActive(now)) {
            if (active) clear();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) return;
        long dif = deadline - now;
        Vec3 feet = player.getPosition(partialTick);
        int animationFrame = frame(now);
        // Original showScannedBlocks:418-420: depth writes off, depth test off for the cube.
        for (int xx = -RANGE; xx <= RANGE; xx++) {
            for (int yy = -RANGE; yy <= RANGE; yy++) {
                for (int zz = -RANGE; zz <= RANGE; zz++) {
                    int value = VALUES[index(xx, yy, zz)];
                    if (!isDrawn(value)) continue;
                    float alpha = cellAlpha(dif, xx, yy, zz);
                    int x = origin.getX() + xx;
                    int y = origin.getY() + yy;
                    int z = origin.getZ() + zz;
                    int colour = specialColour(value);
                    if (colour >= 0) {
                        drawSpecialFaces(pose, feet, x, y, z, colour, alpha);
                        continue;
                    }
                    // Original showScannedBlocks:444-456: additive, no cull, 1/255 alpha test.
                    pose.pushPose();
                    try {
                        pose.translate(x + .5 - feet.x, y + .5 - feet.y, z + .5 - feet.z);
                        // Original drawPickScannedObject:465-472, two camera-facing strips.
                        VisNetRender.billboard(pose, VisNetRender.NODES, 0.0F, innerScale(value),
                                alpha, FRAMES, 0, animationFrame, INNER_COLOUR, GL11.GL_ONE);
                        VisNetRender.billboard(pose, VisNetRender.NODES, 0.0F, outerScale(value),
                                alpha, FRAMES, 0, animationFrame, OUTER_COLOUR, GL11.GL_ONE);
                    } finally {
                        pose.popPose();
                    }
                }
            }
        }
    }

    /**
     * Oracle {@code RenderEventHandler.java:465-501} ({@code drawSpecialBlockoverlay}): the six
     * faces of a water or lava cell. The original translates to the cell centre relative to the
     * camera, rotates by {@code glRotatef(90, -dir.offsetY, dir.offsetX, -dir.offsetZ)}, moves
     * half a block along Z ({@code +0.5} when the face is on the negative Z side, {@code -0.5}
     * otherwise) and draws one {@code wardedglass.png} quad with the fixed colour.
     */
    private static void drawSpecialFaces(PoseStack pose, Vec3 feet, int x, int y, int z,
                                         int colour, float alpha) {
        int red = colour >> 16 & 255;
        int green = colour >> 8 & 255;
        int blue = colour & 255;
        for (Direction direction : Direction.values()) {
            pose.pushPose();
            try {
                pose.translate(-feet.x + x + .5, -feet.y + y + .5, -feet.z + z + .5);
                float axisX = -direction.getStepY();
                float axisY = direction.getStepX();
                float axisZ = -direction.getStepZ();
                if (axisX != 0 || axisY != 0 || axisZ != 0) {
                    pose.mulPose(Axis.of(new org.joml.Vector3f(axisX, axisY, axisZ))
                            .rotationDegrees(90.0F));
                }
                pose.translate(0.0, 0.0, direction.getStepZ() < 0 ? .5 : -.5);
                // Original renderQuadCenteredFromTexture:359-376: no cull change, additive
                // destination ONE, and the source alpha of the caller.
                VisNetRender.drawCulled(WARDED_GLASS, GL11.GL_ONE, false, out -> {
                    VisNetRender.vertex(out, pose, new Vec3(-.5, .5, 0), 0, 1, colour, alpha);
                    VisNetRender.vertex(out, pose, new Vec3(.5, .5, 0), 1, 1, colour, alpha);
                    VisNetRender.vertex(out, pose, new Vec3(.5, -.5, 0), 1, 0, colour, alpha);
                    VisNetRender.vertex(out, pose, new Vec3(-.5, -.5, 0), 0, 0, colour, alpha);
                });
            } finally {
                pose.popPose();
            }
        }
    }

    /** A lower-case name for the diagnostics of the client report. */
    public static String describe() {
        return String.format(Locale.ROOT, "scan(active=%s, origin=%s, deadline=%d)", active, origin, deadline);
    }
}
