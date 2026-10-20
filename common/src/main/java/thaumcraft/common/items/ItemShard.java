package thaumcraft.common.items;

import net.minecraft.world.item.Item;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The vis shards (original ItemShard). One item with seven metadata values: the air, fire, water, earth, order and entropy shard and the
 * balanced shard, which carries no element of its own.
 *
 * <p>The metadata is carried by the item itself in 1.21.1, so every shard of the original is a separate item here. The aspect of a shard
 * lives in {@link EquipmentRules}; the original's colour of the shard is a client side tint.
 */
public final class ItemShard extends Item {
    private final int type;

    public ItemShard(Properties properties, int type) {
        super(properties.stacksTo(64));
        this.type = type;
    }

    /** The original metadata of this shard. */
    public int type() { return type; }

    /** The aspect tag of this shard, or null for the balanced shard. */
    public String aspectTag() { return EquipmentRules.shardAspect(type); }
}
