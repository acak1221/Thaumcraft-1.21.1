package thaumcraft.api.research;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.tiles.TileNode;

/** Original callback DTO; numeric item IDs are current native registry IDs, never old-world import IDs. */
public class ScanResult {
    public byte type;
    public int id;
    public int meta;
    public Entity entity;
    public String phenomena;
    /** Native flattened item witness, used in preference to the legacy-shaped id/meta fields. */
    public ItemStack stack;
    /** A native node locator replaces the old process-global dimension/integer location list. */
    public BlockPos nodePosition;
    public ResourceKey<Level> nodeDimension;

    public ScanResult(byte type, int itemId, int itemMeta, Entity entity, String phenomena) {
        this.type = type; this.id = itemId; this.meta = itemMeta; this.entity = entity; this.phenomena = phenomena;
    }

    public static ScanResult item(ItemStack item) {
        if (item == null || item.isEmpty()) throw new IllegalArgumentException("A scan requires an item");
        var result = new ScanResult((byte) 1, BuiltInRegistries.ITEM.getId(item.getItem()), item.getDamageValue(), null, "");
        result.stack = item.copyWithCount(1); return result;
    }
    public static ScanResult entity(Entity entity) {
        if (entity == null) throw new NullPointerException("entity");
        return new ScanResult((byte) 2, 0, 0, entity, "");
    }
    public static ScanResult node(TileNode node) {
        if (node == null) throw new NullPointerException("node");
        var result = new ScanResult((byte) 3, 0, 0, null, "NODE" + node.getScanId());
        result.nodePosition = node.getBlockPos().immutable();
        result.nodeDimension = node.getLevel() == null ? null : node.getLevel().dimension();
        return result;
    }

    /** Retains the original public DTO's permissive non-ScanResult quirk; server authority never uses this equals method. */
    @Override public boolean equals(Object other) {
        if (other instanceof ScanResult result) {
            if (type != result.type) return false;
            if (type == 1 && (id != result.id || meta != result.meta)) return false;
            if (type == 2 && entity.getId() != result.entity.getId()) return false;
            if (type == 3 && !phenomena.equals(result.phenomena)) return false;
        }
        return true;
    }
}
