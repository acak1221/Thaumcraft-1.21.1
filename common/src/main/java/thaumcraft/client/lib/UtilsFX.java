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
import net.minecraft.world.InteractionHand;
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
 *   <li>{@link #getEquippedProgress(ItemStack, InteractionHand, int, float)} is the equip
 *       progress of the held item. The original read the private fields
 *       {@code equippedProgress} and {@code prevEquippedProgress} of the 1.7.10
 *       {@code ItemRenderer} by reflection (oracle {@code UtilsFX.java:861-877}); the 1.21.1
 *       {@code ItemInHandRenderer} has no such field - the equip animation of the item was
 *       removed from the vanilla client - so the native port reproduces the original curve
 *       of the 1.7.10 {@code ItemRenderer.updateEquippedItem} (see the method).</li>
 *   <li>{@link #getGuiZLevel()} is the original GUI z level (oracle
 *       {@code UtilsFX.java:906-913}); the 1.21.1 GUI has no z level, the depth of the
 *       screens is owned by the render state, so the native value is zero.</li>
 *   <li>{@link #getParticleTexture()} is the original particle atlas (oracle
 *       {@code UtilsFX.java:915-922}); the 1.21.1 particle atlas is
 *       {@code minecraft:particles}.</li>
 * </ul>
 */
public final class UtilsFX {
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
    private static InteractionHand equippedHand;
    private static float equippedProgress;
    private static float prevEquippedProgress;
    private static int equippedTick = -1;

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
            e.printStackTrace();
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
            e.printStackTrace();
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
            DataInputStream data = new DataInputStream(new java.io.ByteArrayInputStream(header));
            data.readInt();// the length of the IHDR chunk
            data.readInt();// "IHDR"
            int width = data.readInt();
            int height = data.readInt();
            return new int[]{width, height};
        }
    }

    /**
     * The equip progress of the item in the hand.
     *
     * <p>The original read the two private fields of the 1.7.10 {@code ItemRenderer} by
     * reflection (oracle {@code UtilsFX.java:861-877}) and interpolated them with the
     * partial tick (oracle {@code ItemThaumometerRenderer.java:106}). The 1.21.1
     * {@code net.minecraft.client.renderer.ItemInHandRenderer} has no
     * {@code equippedProgress} field, so the native port reproduces the curve of the 1.7.10
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
     * @param held        the stack in the hand
     * @param hand        the hand that holds the stack
     * @param tick        the tick counter of the player
     * @param partialTick the partial tick of the frame
     * @return the interpolated progress, between zero and one
     */
    public static float getEquippedProgress(ItemStack held, InteractionHand hand, int tick, float partialTick) {
        if (tick != equippedTick) {
            equippedTick = tick;
            prevEquippedProgress = equippedProgress;
            boolean same = equippedHand == hand && sameStack(equippedStack, held);
            if (equippedStack == null && held == null) same = true;
            if (held != null && equippedStack != null && !sameStack(equippedStack, held)
                    && held.getItem() == equippedStack.getItem()
                    && held.getDamageValue() == equippedStack.getDamageValue()) {
                equippedStack = held;
                same = true;
            }
            float delta = (same ? 1.0F : 0.0F) - equippedProgress;
            if (delta < -EQUIP_STEP) delta = -EQUIP_STEP;
            if (delta > EQUIP_STEP) delta = EQUIP_STEP;
            equippedProgress += delta;
            if (equippedProgress < EQUIP_RESET) {
                equippedStack = held;
                equippedHand = hand;
            }
        }
        return prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTick;
    }

    /** The identity of the two stacks, the 1.7.10 test of the same item instance. */
    private static boolean sameStack(ItemStack first, ItemStack second) {
        if (first == null || second == null) return first == second;
        return first == second;
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
