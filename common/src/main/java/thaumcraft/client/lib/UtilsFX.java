package thaumcraft.client.lib;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The client helper library of the original {@code thaumcraft/client/lib/UtilsFX.java}.
 *
 * <p>The original class was a bag of static drawing helpers and of reflection helpers that
 * read the private fields of the 1.7.10 vanilla client. The native port owns the drawing
 * helpers per renderer ({@code VisNetRender}, {@code FloatyLine}, the aspect icons of the
 * screens), so this class owns the helpers that read the state of the 1.21.1 client:</p>
 *
 * <ul>
 *   <li>{@link #getTextureAnimationSize(String)} and {@link #getTextureSize(String, int)} are
 *       the original animated texture helpers (oracle {@code UtilsFX.java:469-507}): they read
 *       the size of the PNG of the mod and divide it.</li>
 *   <li>{@link #getEquippedProgress(float)} is the equip
 *       progress of the held item. The original read the private fields
 *       {@code equippedProgress} and {@code prevEquippedProgress} of the 1.7.10
 *       {@code ItemRenderer} by reflection (oracle {@code UtilsFX.java:861-877}); the 1.21.1
 *       {@code ItemInHandRenderer} has mainHandHeight/oMainHandHeight, but its target now
 *       cubes attack strength (vanilla ItemInHandRenderer:549-574). A tick-driven tracker
 *       preserves the 1.7.10 curve without that combat-cooldown addition.</li>
 *   <li>{@link #getGuiZLevel()} is the original GUI z level (oracle
 *       {@code UtilsFX.java:906-913}); the 1.21.1 GUI has no z level, the depth of the
 *       screens is owned by the render state, so the native value is zero.</li>
 *   <li>{@link #getParticleTexture()} is the original particle atlas (oracle
 *       {@code UtilsFX.java:915-922}); the 1.21.1 particle atlas is
 *       {@code minecraft:particles}.</li>
 * </ul>
 */
public final class UtilsFX {
    /**
     * The sixteen colours of the original, in the vanilla dye order White .. Black (oracle UtilsFX
     * {@code <clinit>} 228-338, {@code UtilsFX.colors}): the golem marker colours of the Order upgrade.
     */
    private static final int[] COLORS = {0xF0F0F0, 0xEB8844, 0xC354CD, 0x6689D3, 0xDECF2A, 0x41CD34, 0xD88198, 0x434343,
            0xA0A0A0, 0x287697, 0x7B2FBE, 0x253192, 0x51301A, 0x3B511A, 0xB3312C, 0x1E1B1B};

    /** One of the sixteen original colours. */
    public static int color(int index) { return COLORS[index]; }

    /** The original fallback of the texture helpers (oracle UtilsFX.java:485,505). */
    public static final int TEXTURE_SIZE_FALLBACK = 16;
    /**
     * The original step of the equip progress (the 1.7.10 vanilla
     * {@code ItemRenderer.updateEquippedItem}, oracle {@code UtilsFX.java:861-877} and the
     * decompiled 1.7.10 {@code ItemRenderer} method {@code updateEquippedItem}).
     */
    public static final float EQUIP_STEP = 0.4F;
    /**
     * The original threshold below which the tracked item is replaced (the 1.7.10 vanilla
     * {@code ItemRenderer.updateEquippedItem}).
     */
    public static final float EQUIP_RESET = 0.1F;

    private static final Map<Object, Integer> TEXTURE_SIZE_CACHE = new HashMap<>();

    private static ItemStack equippedStack;
    private static int equippedSlot = -1;
    private static float equippedProgress;
    private static float prevEquippedProgress;
    private static Object equippedLevel;

    private UtilsFX() {}

    /**
     * The original {@code UtilsFX.getTextureAnimationSize}: the number of frames of an
     * animated texture, the width of the PNG divided by its height.
     *
     * @param path the path of the texture inside the mod, e.g. {@code textures/misc/particles.png}
     */
    public static int getTextureAnimationSize(String path) {
        Integer cached = TEXTURE_SIZE_CACHE.get(path);
        if (cached != null) return cached;
        try {
            int[] size = readPngSize(path);
            int frames = size[0] / size[1];
            TEXTURE_SIZE_CACHE.put(path, frames);
            return frames;
        } catch (Exception e) {
            thaumcraft.common.Thaumcraft.LOG.warn("Cannot read texture animation size for {}", path, e);
            return TEXTURE_SIZE_FALLBACK;
        }
    }

    /**
     * The original {@code UtilsFX.getTextureSize}: the width of the PNG divided by the
     * number of the tiles of a row.
     *
     * @param path    the path of the texture inside the mod
     * @param divisor the number of the tiles of a row
     */
    public static int getTextureSize(String path, int divisor) {
        List<Object> key = List.of(path, divisor);
        Integer cached = TEXTURE_SIZE_CACHE.get(key);
        if (cached != null) return cached;
        try {
            int[] size = readPngSize(path);
            int tiles = size[0] / divisor;
            TEXTURE_SIZE_CACHE.put(key, tiles);
            return tiles;
        } catch (Exception e) {
            thaumcraft.common.Thaumcraft.LOG.warn("Cannot read texture size for {} (divisor {})", path, divisor, e);
            return TEXTURE_SIZE_FALLBACK;
        }
    }

    /**
     * The size of the PNG of the mod, read from its header.
     *
     * <p>The original read the image with {@code ImageIO} (oracle
     * {@code UtilsFX.java:474-478}); the native port reads the IHDR chunk of the PNG,
     * which holds the same two numbers.</p>
     */
    private static int[] readPngSize(String path) throws IOException {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
        try (InputStream stream = Minecraft.getInstance().getResourceManager().open(location)) {
            byte[] header = stream.readNBytes(24);
            if (header.length < 24) throw new IOException("Image not found: " + path);
            return pngSize(header);
        }
    }

    /** PNG signature (8), IHDR length (4), IHDR type (4), then width/height.
     * Pure header parsing keeps ImageIO's dimensions without depending on a client in tests. */
    public static int[] pngSize(byte[] header) throws IOException {
        if (header.length < 24) throw new IOException("Truncated PNG header");
        DataInputStream data = new DataInputStream(new java.io.ByteArrayInputStream(header));
        if (data.readLong() != 0x89504e470d0a1a0aL || data.readInt() != 13 || data.readInt() != 0x49484452)
            throw new IOException("Not a PNG IHDR");
        int width = data.readInt(), height = data.readInt();
        if (width <= 0 || height <= 0) throw new IOException("Invalid PNG dimensions");
        return new int[]{width, height};
    }

    /**
     * The equip progress of the item in the hand.
     *
     * <p>The original read the two private fields of the 1.7.10 {@code ItemRenderer} by
     * reflection (oracle {@code UtilsFX.java:861-877}) and interpolated them with the
     * partial tick (oracle {@code ItemThaumometerRenderer.java:106}). The 1.21.1
     * {@code net.minecraft.client.renderer.ItemInHandRenderer} adds attack-strength easing,
     * so the native port separately reproduces the curve of the 1.7.10
     * vanilla {@code ItemRenderer.updateEquippedItem}:</p>
     *
     * <ul>
     *   <li>the previous value takes the current value, once per tick;</li>
     *   <li>the target is one while the same item stays in the same hand, otherwise zero;
     *       an item of the same type and the same damage counts as the same item;</li>
     *   <li>the step is at most {@code 0.4} in both directions;</li>
     *   <li>below {@code 0.1} the tracked item is replaced by the current one.</li>
     * </ul>
     *
     * @param partialTick the partial tick of the frame
     * @return the interpolated progress, between zero and one
     */
    public static float getEquippedProgress(float partialTick) {
        return prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTick;
    }

    /** Vanilla 1.7.10 bly.a:518-544 (MCP ItemRenderer.updateEquippedItem).
     * Run even when the thaumometer is NOT rendered, including third-person and open menus. */
    public static void tick(Minecraft client) {
        if (client.player == null || equippedLevel != client.level) {
            equippedStack = null;
            equippedSlot = -1;
            prevEquippedProgress = equippedProgress = 0;
            equippedLevel = client.level;
        }
        if (client.player == null) return;
        if (client.isPaused()) return;
        ItemStack current = client.player.getMainHandItem();
        ItemStack held = current.isEmpty() ? null : current;
        int slot = client.player.getInventory().selected;
        prevEquippedProgress = equippedProgress;
        boolean same = equippedSlot == slot && held == equippedStack;
        if (equippedStack == null && held == null) same = true;
        if (held != null && equippedStack != null && held != equippedStack
                && held.getItem() == equippedStack.getItem()
                && held.getDamageValue() == equippedStack.getDamageValue()) {
            equippedStack = held;
            same = true;
        }
        equippedProgress = equipStep(equippedProgress, same);
        if (equippedProgress < EQUIP_RESET) { equippedStack = held; equippedSlot = slot; }
    }

    public static float equipStep(float progress, boolean same) {
        return progress + Math.max(-EQUIP_STEP, Math.min(EQUIP_STEP, (same ? 1 : 0) - progress));
    }

    /** RenderWisp:65-72 / RenderPrimalArrow:112-119: keep the unusual width/64 nudge. */
    public static float wispUvSpan(int textureSize) {
        return (textureSize - .01F) / (textureSize * 4F);
    }

    /**
     * The original {@code UtilsFX.getGuiZLevel}: the z level of the GUI. The 1.21.1 GUI has
     * no z level - the depth of the screens is owned by the render state - so the native
     * value is zero.
     */
    public static float getGuiZLevel() {
        return 0.0F;
    }

    /**
     * The original {@code UtilsFX.getTimer}: the timer of the client. The 1.21.1 client owns
     * a {@code DeltaTracker} instead of the 1.7.10 {@code Timer}.
     */
    public static DeltaTracker getTimer() {
        return Minecraft.getInstance().getTimer();
    }

    /**
     * The original {@code UtilsFX.getParticleTexture}: the atlas of the particles. The
     * 1.21.1 particle atlas is {@code minecraft:particles}.
     */
    public static ResourceLocation getParticleTexture() {
        return TextureAtlas.LOCATION_PARTICLES;
    }
}
