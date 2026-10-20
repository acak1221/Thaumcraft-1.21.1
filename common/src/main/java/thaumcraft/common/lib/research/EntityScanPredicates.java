package thaumcraft.common.lib.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import thaumcraft.api.ThaumcraftApi.EntityTagsNBT;

/** Original Utils.getNBTDataFromId types and Java equals semantics, rather than numeric coercion or array deep equality. */
public final class EntityScanPredicates {
    private EntityScanPredicates() {}
    public static boolean matches(EntityTagsNBT[] predicates, CompoundTag data) {
        if (predicates == null) return true;
        for (var predicate : predicates) {
            if (predicate == null || predicate.name == null || !data.contains(predicate.name)) return false;
            Object actual = value(data, predicate.name);
            if (actual == null || !actual.equals(predicate.value)) return false;
        }
        return true;
    }
    public static Object value(CompoundTag data, String name) {
        Tag tag = data.get(name);
        if (tag == null) return null;
        return switch (tag.getId()) {
            case Tag.TAG_BYTE -> data.getByte(name);
            case Tag.TAG_SHORT -> data.getShort(name);
            case Tag.TAG_INT -> data.getInt(name);
            case Tag.TAG_LONG -> data.getLong(name);
            case Tag.TAG_FLOAT -> data.getFloat(name);
            case Tag.TAG_DOUBLE -> data.getDouble(name);
            case Tag.TAG_BYTE_ARRAY -> data.getByteArray(name);
            case Tag.TAG_STRING -> data.getString(name);
            case Tag.TAG_LIST -> data.getList(name, Tag.TAG_COMPOUND);
            case Tag.TAG_COMPOUND -> data.getCompound(name);
            case Tag.TAG_INT_ARRAY -> data.getIntArray(name);
            default -> null;
        };
    }
}
