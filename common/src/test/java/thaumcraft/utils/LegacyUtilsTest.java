package thaumcraft.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.utils.CropUtils;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.lib.utils.TCVec3;
import thaumcraft.common.lib.utils.TCVec3Pool;
import thaumcraft.common.lib.utils.UUIDHelper;
import thaumcraft.common.lib.utils.Utils;

/**
 * The pure part of the original utility layer (TC4 4.2.3.5 {@code thaumcraft.common.lib.utils}) without a world.
 *
 * <p>Every expectation below is the behaviour of the original oracle file, including the places where the original
 * is inconsistent with itself.
 */
class LegacyUtilsTest {
    /** The native registries must exist before an item or a block can be read. */
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // ------------------------------------------------------------------- bit helpers

    @Test void theBitHelpersAreTheOriginalOnes() {
        assertTrue(Utils.getBit(0b0100, 2));
        assertFalse(Utils.getBit(0b0100, 1));
        assertEquals(0b0101, Utils.setBit(0b0100, 0));
        assertEquals(0b0100, Utils.clearBit(0b0101, 0));
        assertEquals(0b0101, Utils.toggleBit(0b0100, 0));
        assertEquals(0b1100, Utils.toggleBit(0b0100, 3));
    }

    /**
     * The original {@code pack} shifts the accumulated byte left and ORs the next bit in at the low end, so the
     * FIRST element of the array becomes the HIGHEST bit, and {@code unpack} reads the array from the high bit
     * down. The two are inverses of each other.
     */
    @Test void theBooleanPackingKeepsTheOriginalBitOrder() {
        assertEquals(-128, Utils.pack(new boolean[]{true, false, false, false, false, false, false, false}));
        assertEquals(64, Utils.pack(new boolean[]{false, true, false, false, false, false, false, false}));
        assertEquals(-1, Utils.pack(new boolean[]{true, true, true, true, true, true, true, true}));
        assertEquals(0, Utils.pack(new boolean[]{false, false, false, false, false, false, false, false}));
        assertArrayEquals(new boolean[]{true, false, false, false, false, false, false, false}, Utils.unpack((byte) -128));
        assertArrayEquals(new boolean[]{false, false, false, false, false, false, false, true}, Utils.unpack((byte) 1));
        assertArrayEquals(new boolean[]{true, true, true, true, true, true, true, true}, Utils.unpack((byte) -1));
    }

    @Test void theFloatClampAndTheColourTestAreTheOriginalOnes() {
        assertEquals(0.0F, Utils.clamp_float(-1.0F, 0.0F, 10.0F));
        assertEquals(5.0F, Utils.clamp_float(5.0F, 0.0F, 10.0F));
        assertEquals(10.0F, Utils.clamp_float(11.0F, 0.0F, 10.0F));
        assertTrue(Utils.hasColor(new byte[]{0}));
        assertFalse(Utils.hasColor(new byte[]{-1}));
        assertFalse(Utils.hasColor(new byte[]{-1, -1}));
    }

    // ------------------------------------------------------------------ the NBT reader

    @Test void everyNbtTypeIdOfTheOriginalIsRead() {
        CompoundTag tag = new CompoundTag();
        tag.putByte("byte", (byte) 7);
        tag.putShort("short", (short) 300);
        tag.putInt("int", 70000);
        tag.putLong("long", 5000000000L);
        tag.putFloat("float", 1.5F);
        tag.putDouble("double", 2.5D);
        tag.putByteArray("bytes", new byte[]{1, 2, 3});
        tag.putString("string", "text");
        CompoundTag child = new CompoundTag();
        child.putInt("value", 1);
        ListTag list = new ListTag();
        list.add(child);
        tag.put("list", list);
        tag.put("compound", child);
        tag.putIntArray("ints", new int[]{4, 5, 6});

        assertEquals((byte) 7, Utils.getNBTDataFromId(tag, (byte) 1, "byte"));
        assertEquals((short) 300, Utils.getNBTDataFromId(tag, (byte) 2, "short"));
        assertEquals(70000, Utils.getNBTDataFromId(tag, (byte) 3, "int"));
        assertEquals(5000000000L, Utils.getNBTDataFromId(tag, (byte) 4, "long"));
        assertEquals(1.5F, Utils.getNBTDataFromId(tag, (byte) 5, "float"));
        assertEquals(2.5D, Utils.getNBTDataFromId(tag, (byte) 6, "double"));
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) Utils.getNBTDataFromId(tag, (byte) 7, "bytes"));
        assertEquals("text", Utils.getNBTDataFromId(tag, (byte) 8, "string"));
        assertEquals(list, Utils.getNBTDataFromId(tag, (byte) 9, "list"));
        assertEquals(child, Utils.getNBTDataFromId(tag, (byte) 10, "compound"));
        assertArrayEquals(new int[]{4, 5, 6}, (int[]) Utils.getNBTDataFromId(tag, (byte) 11, "ints"));
        assertNull(Utils.getNBTDataFromId(tag, (byte) 12, "int"), "the original returned null for an unknown id");
    }

    // --------------------------------------------------------------------- the cone

    @Test void theConeTestIsTheOriginalOne() {
        double[] apex = {0, 0, 0};
        double[] axis = {0, 0, 10};
        assertTrue(Utils.isLyingInCone(new double[]{0, 0, 5}, apex, axis, 60.0F), "a point on the axis is inside");
        assertFalse(Utils.isLyingInCone(new double[]{100, 0, 5}, apex, axis, 60.0F), "a point far off the axis is outside");
        assertFalse(Utils.isLyingInCone(new double[]{0, 0, -5}, apex, axis, 60.0F), "a point behind the apex is outside");
        assertFalse(Utils.isLyingInCone(new double[]{0, 0, 20}, apex, axis, 60.0F), "a point beyond the round cap is outside");
    }

    @Test void theVectorHelpersOfTheOriginalAreTheSame() {
        assertEquals(32.0, Utils.dotProd(new double[]{1, 2, 3}, new double[]{4, 5, 6}));
        assertArrayEquals(new double[]{-3, -3, -3}, Utils.dif(new double[]{1, 2, 3}, new double[]{4, 5, 6}));
        assertEquals(Math.sqrt(14), Utils.magn(new double[]{1, 2, 3}));
        assertEquals(100.0, Utils.distanceSquared2d(new Vec3(0, 5, 0), new Vec3(6, 9, 8)));
        assertEquals(116.0, Utils.distanceSquared3d(new Vec3(0, 5, 0), new Vec3(6, 9, 8)));
    }

    @Test void theLaunchVelocityIsTheOriginalOne() {
        Vec3 velocity = Utils.calculateVelocity(new Vec3(0, 0, 0), new Vec3(10, 5, 0), 1.0D, 1.0D);
        assertEquals(1.4495D, velocity.x, 1e-3D);
        assertEquals(2.4495D, velocity.y, 1e-3D);
        assertEquals(0.0D, velocity.z, 1e-9D);
    }

    // ------------------------------------------------------------------ the UUID helper

    @Test void theLocalUuidConversionsAreTheOriginalOnes() {
        assertEquals("abc123", UUIDHelper.cleanUUID("a-b_c 123"));
        assertEquals("01234567-89ab-cdef-0123-456789abcdef",
                UUIDHelper.fullUUID("0123456789abcdef0123456789abcdef"));
        assertEquals("01234567-89ab-cdef-0123-456789abcdef",
                UUIDHelper.fullUUID("01234567-89ab-cdef-0123-456789abcdef"));
        assertNull(UUIDHelper.stringToUUID("not a uuid"));
        assertNotNull(UUIDHelper.stringToUUID("01234567-89ab-cdef-0123-456789abcdef"));
        UUID offline = UUIDHelper.getOfflineUUID("Notch");
        assertEquals(UUID.nameUUIDFromBytes("OfflinePlayer:Notch".getBytes(StandardCharsets.UTF_8)), offline);
        assertFalse(offline.equals(UUIDHelper.getOfflineUUID("Herobrine")));
    }

    // -------------------------------------------------------------------- the vectors

    @Test void theVectorFoldsTheNegativeZeroAndKeepsItsArithmetic() {
        TCVec3 vector = TCVec3.createVectorHelper(-0.0D, 2.0D, 3.0D);
        assertEquals(0L, Double.doubleToRawLongBits(vector.xCoord), "the original folded the negative zero");
        assertEquals("(1.0, 2.0, 3.0)", TCVec3.createVectorHelper(1.0D, 2.0D, 3.0D).toString());
        assertEquals(0.6D, TCVec3.createVectorHelper(3.0D, 0.0D, 4.0D).normalize().xCoord, 1e-9D);
        assertEquals(0.8D, TCVec3.createVectorHelper(3.0D, 0.0D, 4.0D).normalize().zCoord, 1e-9D);
        assertEquals(5.0D, TCVec3.createVectorHelper(0.0D, 0.0D, 0.0D).distanceTo(TCVec3.createVectorHelper(3.0D, 4.0D, 0.0D)), 1e-9D);
        assertEquals(25.0D, TCVec3.createVectorHelper(0.0D, 0.0D, 0.0D).squareDistanceTo(3.0D, 4.0D, 0.0D), 1e-9D);
        assertEquals(14.0D, TCVec3.createVectorHelper(1.0D, 2.0D, 3.0D).dotProduct(TCVec3.createVectorHelper(1.0D, 2.0D, 3.0D)), 1e-9D);
        assertEquals(3.0D, TCVec3.createVectorHelper(1.0D, 2.0D, 3.0D).addVector(2.0D, 3.0D, 4.0D).xCoord, 1e-9D);
        TCVec3 cross = TCVec3.createVectorHelper(1.0D, 0.0D, 0.0D).crossProduct(TCVec3.createVectorHelper(0.0D, 1.0D, 0.0D));
        assertEquals(0.0D, cross.xCoord, 1e-9D);
        assertEquals(0.0D, cross.yCoord, 1e-9D);
        assertEquals(1.0D, cross.zCoord, 1e-9D);
        assertEquals(0.0D, TCVec3.createVectorHelper(0.0D, 0.0D, 0.0D).normalize().lengthVector(), 1e-9D);
    }

    @Test void theIntermediatePointOfTheOriginalIsKept() {
        TCVec3 from = TCVec3.createVectorHelper(0.0D, 0.0D, 0.0D);
        TCVec3 to = TCVec3.createVectorHelper(10.0D, 0.0D, 0.0D);
        TCVec3 middle = from.getIntermediateWithXValue(to, 5.0D);
        assertNotNull(middle);
        assertEquals(5.0D, middle.xCoord, 1e-9D);
        assertNull(from.getIntermediateWithXValue(to, 20.0D), "outside the segment there is no point");
        assertNull(from.getIntermediateWithXValue(to, -1.0D));
        assertNull(from.getIntermediateWithYValue(to, 1.0D), "a segment without a y part has no y point");
    }

    @Test void theRotationsOfTheOriginalAreKept() {
        TCVec3 aroundY = TCVec3.createVectorHelper(1.0D, 0.0D, 0.0D);
        aroundY.rotateAroundY((float) (Math.PI / 2));
        assertEquals(0.0D, aroundY.xCoord, 1e-6D);
        assertEquals(-1.0D, aroundY.zCoord, 1e-6D);
        TCVec3 aroundZ = TCVec3.createVectorHelper(1.0D, 0.0D, 0.0D);
        aroundZ.rotateAroundZ((float) (Math.PI / 2));
        assertEquals(0.0D, aroundZ.xCoord, 1e-6D);
        assertEquals(-1.0D, aroundZ.yCoord, 1e-6D);
    }

    @Test void thePoolOfTheOriginalIsDisabledByItsOwnNumbers() {
        TCVec3Pool shared = new TCVec3Pool(-1, -1);
        TCVec3 first = shared.getVecFromPool(1.0D, 2.0D, 3.0D);
        TCVec3 second = shared.getVecFromPool(1.0D, 2.0D, 3.0D);
        assertNotSame(first, second, "a negative threshold disables the pool, so every vector is its own");
        assertEquals(0, shared.getPoolSize());
        assertEquals(0, shared.getNextFreeSpace());
    }

    @Test void anEnabledPoolReusesItsVectors() {
        TCVec3Pool pool = new TCVec3Pool(2, 1);
        TCVec3 first = pool.getVecFromPool(1.0D, 2.0D, 3.0D);
        assertEquals(1, pool.getPoolSize());
        pool.clear();
        TCVec3 second = pool.getVecFromPool(4.0D, 5.0D, 6.0D);
        assertSame(first, second);
        assertEquals(4.0D, second.xCoord, 1e-9D);
        assertEquals(1, pool.getNextFreeSpace());
    }

    // --------------------------------------------------------------------- the crops

    @Test void theCropStagesAreTheOriginalMetadata() {
        assertEquals(0, CropUtils.age(Blocks.WHEAT.defaultBlockState()));
        assertEquals(0, CropUtils.age(Blocks.NETHER_WART.defaultBlockState()));
        assertEquals(0, CropUtils.age(Blocks.STONE.defaultBlockState()));
        assertEquals(7, CropUtils.age(Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7)));
        assertEquals(3, CropUtils.age(Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, 3)));
        assertEquals(5, CropUtils.age(Blocks.PUMPKIN_STEM.defaultBlockState().setValue(StemBlock.AGE, 5)));
        assertTrue(CropUtils.isGrowable(Blocks.WHEAT.defaultBlockState()));
        assertTrue(CropUtils.isGrowable(Blocks.PUMPKIN_STEM.defaultBlockState()));
        assertTrue(CropUtils.isGrowable(Blocks.NETHER_WART.defaultBlockState()));
        assertFalse(CropUtils.isGrowable(Blocks.STONE.defaultBlockState()));
        assertTrue(CropUtils.canGrow(Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3)));
        assertFalse(CropUtils.canGrow(Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7)));
        assertFalse(CropUtils.canGrow(Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, 3)));
        assertTrue(CropUtils.canGrow(Blocks.PUMPKIN_STEM.defaultBlockState().setValue(StemBlock.AGE, 0)));
    }

    // ---------------------------------------------------------------- the inventories

    @Test void theItemComparisonOfTheOriginalIsKept() {
        ItemStack stone = new ItemStack(Items.STONE);
        ItemStack otherStone = new ItemStack(Items.STONE);
        ItemStack dirt = new ItemStack(Items.DIRT);
        assertTrue(InventoryUtils.areItemStacksEqual(stone, otherStone, false, false, false));
        assertFalse(InventoryUtils.areItemStacksEqual(stone, dirt, false, false, false));
        assertTrue(InventoryUtils.areItemStacksEqualStrict(stone, otherStone));
        assertFalse(InventoryUtils.areItemStacksEqualStrict(stone, dirt));
        assertTrue(InventoryUtils.compareMultipleItems(stone, new ItemStack[]{dirt, otherStone}));
        assertFalse(InventoryUtils.compareMultipleItems(stone, new ItemStack[]{dirt}));
        assertFalse(InventoryUtils.compareMultipleItems(ItemStack.EMPTY, new ItemStack[]{otherStone}));
    }
}
