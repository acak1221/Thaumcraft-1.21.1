package thaumcraft.client.renderers;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The camera basis of the 1.7.10 original, {@code ActiveRenderInfo}.
 *
 * <p>The original builds the camera facing quads of the cultist portal and of the
 * eldritch gate from two unit vectors that are derived from the look angles of the
 * player and not from the camera:
 *
 * <ul>
 *   <li>the left vector is {@code (cos(yaw), 0, sin(yaw))};</li>
 *   <li>the up vector is {@code (-sin(yaw) sin(pitch), cos(pitch), cos(yaw) sin(pitch))}.</li>
 * </ul>
 *
 * <p>Both vectors are negated in their horizontal part in the third person view
 * ({@code ActiveRenderInfo.updateRenderInfo} multiplies them with {@code 1 - 2i}
 * where {@code i} is one in the third person), so the quad of the original is
 * mirrored in that view and it always faces the way the player looks.
 */
public final class CameraBasis {
    private CameraBasis() {}

    /** The left vector of the basis of the original. */
    public static Vector3f left() {
        var player = Minecraft.getInstance().player;
        float yaw = player == null ? 0.0F : player.getYRot();
        return leftFor(yaw, thirdPerson());
    }

    /** The up vector of the basis of the original. */
    public static Vector3f up() {
        var player = Minecraft.getInstance().player;
        float yaw = player == null ? 0.0F : player.getYRot();
        float pitch = player == null ? 0.0F : player.getXRot();
        return upFor(yaw, pitch, thirdPerson());
    }

    /**
     * The left vector of the basis of the original for the given look angles.
     *
     * <p>Oracle {@code ActiveRenderInfo.updateRenderInfo}: the vector is
     * {@code (cos(yaw), 0, sin(yaw))} and it is negated in the third person view.
     */
    public static Vector3f leftFor(float yaw, boolean thirdPerson) {
        float side = thirdPerson ? -1.0F : 1.0F;
        float cos = Mth.cos(yaw * ((float) Math.PI / 180.0F));
        float sin = Mth.sin(yaw * ((float) Math.PI / 180.0F));
        return new Vector3f(cos * side, 0.0F, sin * side);
    }

    /**
     * The up vector of the basis of the original for the given look angles.
     *
     * <p>Oracle {@code ActiveRenderInfo.updateRenderInfo}: the vector is
     * {@code (-sin(yaw) sin(pitch), cos(pitch), cos(yaw) sin(pitch))} and its
     * horizontal part is negated in the third person view.
     */
    public static Vector3f upFor(float yaw, float pitch, boolean thirdPerson) {
        float side = thirdPerson ? -1.0F : 1.0F;
        float cosYaw = Mth.cos(yaw * ((float) Math.PI / 180.0F));
        float sinYaw = Mth.sin(yaw * ((float) Math.PI / 180.0F));
        float sinPitch = Mth.sin(pitch * ((float) Math.PI / 180.0F));
        float cosPitch = Mth.cos(pitch * ((float) Math.PI / 180.0F));
        return new Vector3f(-sinYaw * sinPitch * side, cosPitch, cosYaw * sinPitch * side);
    }

    /** The original mirror of the basis in the third person view. */
    public static boolean thirdPerson() {
        var minecraft = Minecraft.getInstance();
        return minecraft.options != null && !minecraft.options.getCameraType().isFirstPerson();
    }
}
