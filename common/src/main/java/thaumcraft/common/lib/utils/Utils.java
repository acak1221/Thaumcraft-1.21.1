package thaumcraft.common.lib.utils;

import dev.architectury.networking.NetworkManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.WorldCoordinates;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.network.VisNetFxPayload;

/**
 * Original TC4 {@code thaumcraft.common.lib.utils.Utils}: the general helper layer.
 *
 * <p>Everything that needs no world is a pure static function and is covered by {@code thaumcraft.utils.LegacyUtilsTest}:
 * the bit helpers, the boolean packing, the NBT reader, the cone test and the vector maths. The world-aware helpers
 * are thin wrappers around those.
 *
 * <p>Two original entries are NOT repeated here because the native tree already owns their replacement:
 * {@code addSpecialMiningResult}/{@code findSpecialMiningResult} live in {@link SpecialMining}, and
 * {@code generateLoot} lives in {@code thaumcraft.common.lib.loot.LootTables}. {@code setBiomeAt} is replaced by
 * {@code thaumcraft.common.lib.world.BiomeColumns}. The original special mining tables are therefore not duplicated.
 */
public final class Utils {
    /** Original {@code colorNames}: the sixteen dye colours of 1.7.10. */
    public static final String[] colorNames = new String[]{"White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime", "Pink",
            "Gray", "Light Gray", "Cyan", "Purple", "Blue", "Brown", "Green", "Red", "Black"};
    /** Original {@code colors}: the matching dye colour values. */
    public static final int[] colors = new int[]{0xF0F0F0, 15435844, 12801229, 6719955, 14602026, 4312372, 14188952, 0x434343,
            0xA0A0A0, 2651799, 8073150, 2437522, 5320730, 3887386, 11743532, 0x1E1B1B};

    /** Original {@code effectBuffer}: the per-position throttle of the vis drain effect. */
    public static final Map<WorldCoordinates, Long> effectBuffer = new HashMap<>();

    private Utils() {}

    // ------------------------------------------------------------------ pure helpers

    /** Original {@code hasColor}: true when any byte of the colour buffer is a real colour. */
    public static boolean hasColor(byte[] colors) {
        if (colors == null) return false;
        for (byte color : colors) {
            if (color < 0) continue;
            return true;
        }
        return false;
    }

    /** Original {@code clamp_float}. */
    public static float clamp_float(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    /** Original {@code getBit}. */
    public static boolean getBit(int value, int bit) {
        return (value & 1 << bit) != 0;
    }

    /** Original {@code setBit}. */
    public static int setBit(int value, int bit) {
        return value | 1 << bit;
    }

    /** Original {@code clearBit}. */
    public static int clearBit(int value, int bit) {
        return value & ~(1 << bit);
    }

    /** Original {@code toggleBit}. */
    public static int toggleBit(int value, int bit) {
        return value ^ 1 << bit;
    }

    /** Original {@code pack}: the bits of the array, first bit in the highest position. */
    public static byte pack(boolean[] values) {
        byte result = 0;
        for (boolean bit : values) {
            result = (byte) (result << 1 | (bit ? 1 : 0) & 1);
        }
        return result;
    }

    /** Original {@code unpack}: the inverse of {@link #pack(boolean[])}. */
    public static boolean[] unpack(byte value) {
        boolean[] result = new boolean[8];
        for (int i = 0; i < 8; ++i) {
            result[i] = (byte) (value >> 7 - i & 1) == 1;
        }
        return result;
    }

    /**
     * Original {@code getNBTDataFromId}: the NBT value of the given type id.
     *
     * <p>1 byte, 2 short, 3 int, 4 long, 5 float, 6 double, 7 byte array, 8 string, 9 tag list, 10 compound,
     * 11 int array. The original returned null for every other id.
     */
    public static Object getNBTDataFromId(CompoundTag tag, byte id, String key) {
        if (tag == null) return null;
        switch (id) {
            case 1: return tag.getByte(key);
            case 2: return tag.getShort(key);
            case 3: return tag.getInt(key);
            case 4: return tag.getLong(key);
            case 5: return tag.getFloat(key);
            case 6: return tag.getDouble(key);
            case 7: return tag.getByteArray(key);
            case 8: return tag.getString(key);
            case 9: return tag.getList(key, Tag.TAG_COMPOUND);
            case 10: return tag.getCompound(key);
            case 11: return tag.getIntArray(key);
        }
        return null;
    }

    /**
     * Original {@code isLyingInCone}: is the point {@code x} inside the cone with the apex {@code t}, the axis
     * {@code t -> b} and the given aperture?
     */
    public static boolean isLyingInCone(double[] x, double[] t, double[] b, float aperture) {
        double halfAperture = aperture / 2.0f;
        double[] apexToXVect = dif(t, x);
        double[] axisVect = dif(t, b);
        boolean isInInfiniteCone = dotProd(apexToXVect, axisVect) / magn(apexToXVect) / magn(axisVect) > Math.cos(halfAperture);
        if (!isInInfiniteCone) return false;
        // The round cap test of the original: the projection must not reach past the axis length.
        return dotProd(apexToXVect, axisVect) / magn(axisVect) < magn(axisVect);
    }

    /** Original {@code dotProd}. */
    public static double dotProd(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /** Original {@code dif}: the component-wise difference {@code a - b}. */
    public static double[] dif(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    /** Original {@code magn}: the length of a three component vector. */
    public static double magn(double[] a) {
        return Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
    }

    /** Original {@code distanceSquared2d}. */
    public static double distanceSquared2d(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return dx * dx + dz * dz;
    }

    /** Original {@code distanceSquared3d}. */
    public static double distanceSquared3d(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Original {@code calculateVelocity}: the launch velocity that carries a projectile from {@code from} to
     * {@code to} with the given height gain and gravity.
     */
    public static Vec3 calculateVelocity(Vec3 from, Vec3 to, double heightGain, double gravity) {
        double endGain = to.y - from.y;
        double horizDist = Math.sqrt(distanceSquared2d(from, to));
        double gain = heightGain;
        double maxGain = gain > endGain + gain ? gain : endGain + gain;
        double a = -horizDist * horizDist / (4.0 * maxGain);
        double b = horizDist;
        double c = -endGain;
        double slope = -b / (2.0 * a) - Math.sqrt(b * b - 4.0 * a * c) / (2.0 * a);
        double vy = Math.sqrt(maxGain * gravity);
        double vh = vy / slope;
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double mag = Math.sqrt(dx * dx + dz * dz);
        double dirx = dx / mag;
        double dirz = dz / mag;
        double vx = vh * dirx;
        double vz = vh * dirz;
        return new Vec3(vx, vy, vz);
    }

    /** Original {@code copyFile}. */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) destFile.createNewFile();
        FileChannel source = null;
        AbstractInterruptibleChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            ((FileChannel) destination).transferFrom(source, 0L, source.size());
        } finally {
            if (source != null) source.close();
            if (destination != null) destination.close();
        }
    }

    /** Original {@code isEETransmutionItem}: the Equivalent Exchange III transmutation stone check. */
    public static boolean isEETransmutionItem(Item item) {
        if (item == null) return false;
        try {
            Class<?> ee = Class.forName("com.pahimar.ee3.item.ITransmutationStone");
            return ee.isAssignableFrom(item.getClass());
        } catch (Exception ignored) {
            // The original returned false whenever the class was absent.
        }
        return false;
    }

    /**
     * Original {@code setPrivateFinalValue}: write a private final field, without the Forge name remapping.
     *
     * <p>The original stripped the {@code final} flag through the removed {@code Field.modifiers} hack. The native
     * equivalent writes the field through a private {@link VarHandle}, which is the supported way to write a final
     * instance field.
     */
    public static <E> void setPrivateFinalValue(Class<?> classToAccess, Object instance, E value, String... fieldNames) {
        for (String fieldName : fieldNames) {
            Field field = findField(classToAccess, fieldName);
            if (field == null) continue;
            try {
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup());
                VarHandle handle = lookup.unreflectVarHandle(field);
                handle.set(instance, value);
                return;
            } catch (Throwable failure) {
                try {
                    field.setAccessible(true);
                    field.set(instance, value);
                    return;
                } catch (Throwable ignored) {
                    Thaumcraft.LOG.warn("Could not write private final field {} of {}", fieldName, classToAccess.getName(), failure);
                }
            }
        }
        Thaumcraft.LOG.warn("Could not find private final field {} of {}", String.join("/", fieldNames), classToAccess.getName());
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // Try the superclass.
            }
        }
        return null;
    }

    // -------------------------------------------------------------- world aware helpers

    /** Original {@code isChunkLoaded}: is the chunk of the given block coordinates loaded? */
    public static boolean isChunkLoaded(Level level, int x, int z) {
        return level != null && level.hasChunkAt(x >> 4, z >> 4);
    }

    /**
     * Original {@code useBonemealAtLoc}: apply bonemeal to the crop at the given position.
     *
     * <p>The original fired the Forge {@code BonemealEvent} first; 1.21.1 has no such event, so the crop's own
     * {@code performBonemeal} is the growth. The original also refused a crop that could not grow any further;
     * that check is kept by only accepting the crop blocks of the original {@code IGrowable} family.
     */
    public static boolean useBonemealAtLoc(Level level, Player player, int x, int y, int z) {
        if (level == null) return false;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (!CropUtils.isGrowable(state)) return false;
        if (!level.isClientSide && level instanceof ServerLevel server
                && state.getBlock() instanceof BonemealableBlock bonemealable) {
            bonemealable.performBonemeal(server, server.random, pos, state);
        }
        return true;
    }

    /** Original {@code getFirstUncoveredY}: the first y at or above 5 whose next cell is air. */
    public static int getFirstUncoveredY(Level level, int x, int z) {
        int y = 5;
        while (!isAir(level, x, y + 1, z) && y < level.getMaxBuildHeight() - 2) ++y;
        return y;
    }

    /**
     * Original {@code getFirstUncoveredBlockHeight}: the first y at or above 10 whose next cell is air, never above 250.
     *
     * <p>The original loop condition was {@code !isAir || y > 250}, so it only stopped below 251. The native loop
     * keeps that bound and adds the world height, which the original reached through its empty sky.
     */
    public static int getFirstUncoveredBlockHeight(Level level, int x, int z) {
        int y = 10;
        while ((!isAir(level, x, y + 1, z) || y > 250) && y < level.getMaxBuildHeight() - 2) ++y;
        return y;
    }

    private static boolean isAir(Level level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.isOutsideBuildHeight(pos)) return true;
        return level.getBlockState(pos).isAir();
    }

    /**
     * Original {@code isWoodLog}: is the block a log the elemental axe may harvest?
     *
     * <p>The original excluded the plain oak log, accepted every leaf block and then asked the {@code "woodLog"}
     * ore dictionary, which the configuration filled with every log item. In 1.21.1 that dictionary is the
     * {@code #minecraft:logs} block tag.
     */
    public static boolean isWoodLog(Level level, int x, int y, int z) {
        if (level == null) return false;
        return isWoodLogState(level.getBlockState(new BlockPos(x, y, z)));
    }

    /** Pure form of {@link #isWoodLog(Level, int, int, int)}. */
    public static boolean isWoodLogState(BlockState state) {
        if (state == null) return false;
        if (state.is(net.minecraft.world.level.block.Blocks.OAK_LOG)) return false;
        if (state.getBlock() instanceof LeavesBlock) return true;
        return state.is(BlockTags.LOGS);
    }

    /**
     * Original {@code resetFloatCounter}: clear the server's anti-float timer for the player.
     *
     * <p>The original wrote the private {@code floatingTickCount} of the play server handler. The native field is
     * the private {@code tickCount} of the same handler.
     */
    public static void resetFloatCounter(ServerPlayer player) {
        if (player == null || player.connection == null) return;
        setPrivateFinalValue(player.connection.getClass(), player.connection, 0, "tickCount", "field_147365_f");
    }

    /**
     * Original {@code generateVisEffect}: the visual drain between two points.
     *
     * <p>The original throttled the effect per world position for 500 ms plus a random 100 ms and then sent the
     * drain packet to every player around the source. The native payload carries the same two points and the same
     * colour. The original's numeric dimension id has no native equivalent, so the world itself carries it.
     */
    public static void generateVisEffect(ServerLevel level, int x, int y, int z, int x2, int y2, int z2, int color) {
        if (level == null) return;
        BlockPos origin = new BlockPos(x, y, z);
        BlockPos target = new BlockPos(x2, y2, z2);
        if (origin.equals(target) || origin.distSqr(target) > 64 * 64) return;
        if (!Level.isInSpawnableBounds(origin) || !Level.isInSpawnableBounds(target)) return;
        long now = System.currentTimeMillis();
        WorldCoordinates key = new WorldCoordinates(level.dimension(), origin);
        Long expires = effectBuffer.get(key);
        if (expires != null) {
            // Original quirk: an expired entry is removed by THIS call, so the next effect is emitted.
            if (expires < now) effectBuffer.remove(key);
            return;
        }
        effectBuffer.put(key, now + 500L + new Random(now).nextInt(100));
        VisNetFxPayload payload = new VisNetFxPayload(level.dimension().location(), origin, target, color & 0xffffff);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(Vec3.atCenterOf(origin)) <= 64 * 64) NetworkManager.sendToPlayer(player, payload);
        }
    }

}
