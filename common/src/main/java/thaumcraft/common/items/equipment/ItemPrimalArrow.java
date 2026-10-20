package thaumcraft.common.items.equipment;

import net.minecraft.world.item.Item;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The primal arrows (original ItemPrimalArrow). One item with six metadata values: the air, fire, water, earth, order and entropy
 * arrow, each of them carrying the aspect of its element and the damage the original EntityPrimalArrow gives them.
 *
 * <p>The original item itself only carries the element; the arrow's flight, damage and effects belong to the EntityPrimalArrow, which is
 * not part of this port. The numbers of that entity live in {@link EquipmentRules} so that they can be tested and reused.
 */
public final class ItemPrimalArrow extends Item {
    private final int type;

    public ItemPrimalArrow(Properties properties, int type) {
        super(properties.stacksTo(64));
        this.type = type;
    }

    /** The element of this arrow: the original metadata, zero to five. */
    public int type() { return type; }

    /** The aspect tag of the element, or null for an unknown element. */
    public String aspectTag() { return EquipmentRules.primalArrowAspect(type); }

    /** The damage the arrow of this element carries (original EntityPrimalArrow.func_70242_d). */
    public double arrowDamage() { return EquipmentRules.primalArrowDamage(type); }
}
