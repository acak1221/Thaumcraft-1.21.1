package thaumcraft.common.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The nuggets and native clusters (original ItemNugget). One item with thirty two metadata values: the nuggets of iron, copper, tin,
 * silver, lead, quicksilver, thaumium and void metal, and the native clusters of iron, copper, tin, silver, lead, cinnabar and gold.
 *
 * <p>The metadata is carried by the item itself in 1.21.1, so every nugget of the original is a separate item here. The identity of a
 * nugget - whether it is a native cluster - lives in {@link EquipmentRules}.
 */
public final class ItemNugget extends Item {
    private final int type;

    public ItemNugget(Properties properties, int type) {
        super(properties.stacksTo(64));
        this.type = type;
    }

    /** The original metadata of this nugget. */
    public int type() { return type; }

    /** Whether this nugget is one of the native clusters (original ItemNugget metadata sixteen to twenty one and thirty one). */
    public boolean isCluster() { return EquipmentRules.nuggetIsCluster(type); }

    @Override public String getDescriptionId(ItemStack stack) { return super.getDescriptionId(stack); }
}
