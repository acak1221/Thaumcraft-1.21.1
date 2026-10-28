/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [8 Sep 2013, 19:36:25 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.core.helper;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Small ad-hoc values on an item stack, the 1.21.1 form of the stack tag.
 *
 * <p>1.7.10 gave every stack a mutable {@code NBTTagCompound}; 1.21.1 has data components, and the free-form one is
 * {@link DataComponents#CUSTOM_DATA}. This class keeps the 1.7.10 function names and stores under that component, so the
 * values of items that have no component of their own (and the tag a stack of another mod carries) read and write as before.
 * The items of this add-on that are read often use a component of their own instead (see {@code BasicItemComponents}).
 * One thing cannot stay: {@code getNBT} handed out the live tag, and {@link CustomData} is immutable, so {@link #getNBT}
 * is a <em>copy</em> and every change goes through a setter, {@link #injectNBT} or {@link #update}.</p>
 */
public final class ItemNBTHelper {
    private ItemNBTHelper() {}

    /** Whether the stack carries custom data at all (1.7.10 {@code hasTagCompound}). */
    public static boolean detectNBT(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    /** Gives the stack an empty tag unless it has one. */
    public static void initNBT(ItemStack stack) {
        if (!detectNBT(stack)) injectNBT(stack, new CompoundTag());
    }

    /** Replaces the custom data of the stack with a copy of the tag, no checks made. */
    public static void injectNBT(ItemStack stack, CompoundTag nbt) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt.copy()));
    }

    /** A copy of the custom data of the stack, empty when it has none. Changing the copy changes nothing. */
    public static CompoundTag getNBT(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    /** Changes the custom data of the stack in place: the consumer gets a tag whose changes are kept. */
    public static void update(ItemStack stack, Consumer<CompoundTag> change) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, change);
    }

    // SETTERS ///////////////////////////////////////////////////////////////////

    public static void setBoolean(ItemStack stack, String tag, boolean b) {
        update(stack, cmp -> cmp.putBoolean(tag, b));
    }

    public static void setByte(ItemStack stack, String tag, byte b) {
        update(stack, cmp -> cmp.putByte(tag, b));
    }

    public static void setShort(ItemStack stack, String tag, short s) {
        update(stack, cmp -> cmp.putShort(tag, s));
    }

    public static void setInt(ItemStack stack, String tag, int i) {
        update(stack, cmp -> cmp.putInt(tag, i));
    }

    public static void setLong(ItemStack stack, String tag, long l) {
        update(stack, cmp -> cmp.putLong(tag, l));
    }

    public static void setFloat(ItemStack stack, String tag, float f) {
        update(stack, cmp -> cmp.putFloat(tag, f));
    }

    public static void setDouble(ItemStack stack, String tag, double d) {
        update(stack, cmp -> cmp.putDouble(tag, d));
    }

    /** Stores a copy of the compound under the tag. (1.7.10 refused the key {@code ench}; enchantments are a component now.) */
    public static void setCompound(ItemStack stack, String tag, CompoundTag cmp) {
        CompoundTag copy = cmp.copy();
        update(stack, root -> root.put(tag, copy));
    }

    public static void setString(ItemStack stack, String tag, String s) {
        update(stack, cmp -> cmp.putString(tag, s));
    }

    /** Removes the key; the custom data stays on the stack (empty when nothing else is in it). */
    public static void removeTag(ItemStack stack, String tag) {
        if (verifyExistance(stack, tag)) update(stack, cmp -> cmp.remove(tag));
    }

    // GETTERS ///////////////////////////////////////////////////////////////////

    public static boolean verifyExistance(ItemStack stack, String tag) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(tag);
    }

    public static boolean getBoolean(ItemStack stack, String tag, boolean defaultExpected) {
        return verifyExistance(stack, tag) ? getNBT(stack).getBoolean(tag) : defaultExpected;
    }

    public static byte getByte(ItemStack stack, String tag, byte defaultExpected) {
        return verifyExistance(stack, tag) ? getNBT(stack).getByte(tag) : defaultExpected;
    }

    public static short getShort(ItemStack stack, String tag, short defaultExpected) {
        return verifyExistance(stack, tag) ? getNBT(stack).getShort(tag) : defaultExpected;
    }

    public static int getInt(ItemStack stack, String tag, int defaultExpected) {
        return verifyExistance(stack, tag) ? getNBT(stack).getInt(tag) : defaultExpected;
    }

    public static long getLong(ItemStack stack, String tag, long defaultExpected) {
        return verifyExistance(stack, tag) ? getNBT(stack).getLong(tag) : defaultExpected;
    }

    public static float getFloat(ItemStack stack, String tag, float defaultExpected) {
        return verifyExistance(stack, tag) ? getNBT(stack).getFloat(tag) : defaultExpected;
    }

    public static double getDouble(ItemStack stack, String tag, double defaultExpected) {
        return verifyExistance(stack, tag) ? getNBT(stack).getDouble(tag) : defaultExpected;
    }

    /** A copy of the compound under the tag; when there is none, null if {@code nullifyOnFail} and a new tag otherwise. */
    public static CompoundTag getCompound(ItemStack stack, String tag, boolean nullifyOnFail) {
        return verifyExistance(stack, tag) ? getNBT(stack).getCompound(tag)
                : nullifyOnFail ? null : new CompoundTag();
    }

    public static String getString(ItemStack stack, String tag, String defaultExpected) {
        return verifyExistance(stack, tag) ? getNBT(stack).getString(tag) : defaultExpected;
    }
}
