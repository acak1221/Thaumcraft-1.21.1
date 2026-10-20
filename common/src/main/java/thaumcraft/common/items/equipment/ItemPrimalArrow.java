package thaumcraft.common.items.equipment;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.entities.projectile.EntityPrimalArrow;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The primal arrows (original ItemPrimalArrow). One item with six metadata values: the air, fire, water, earth, order and entropy
 * arrow, each of them carrying the aspect of its element and the damage the original EntityPrimalArrow gives them.
 *
 * <p>The original item is the arrow the bow fires; the original bow event replaces the arrow of the shot with the EntityPrimalArrow of
 * the element of the item (original EventHandlerEntity.bowShot). In 1.21.1 the arrow item is the native hook for that: the item
 * creates the primal arrow itself, so every bow that accepts the item fires the primal arrow of its element, and the element travels
 * with the arrow instead of a metadata value.
 */
public final class ItemPrimalArrow extends ArrowItem {
    private final int type;

    public ItemPrimalArrow(Properties properties, int type) {
        super(properties);
        this.type = type;
    }

    /** The element of this arrow: the original metadata, zero to five. */
    public int type() { return type; }

    /** The aspect tag of the element, or null for an unknown element. */
    public String aspectTag() { return EquipmentRules.primalArrowAspect(type); }

    /** The damage the arrow of this element carries (original EntityPrimalArrow.func_70242_d). */
    public double arrowDamage() { return EquipmentRules.primalArrowDamage(type); }

    /** Original EventHandlerEntity.bowShot: the arrow of the shot is the primal arrow of the element of the item. */
    @Override public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter, ItemStack weapon) {
        return new EntityPrimalArrow(level, shooter, type);
    }

    /** Original EventHandlerEntity.bowShot: the dispenser fires the primal arrow of the element of the item as well. */
    @Override public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        EntityPrimalArrow arrow = new EntityPrimalArrow(level);
        arrow.setPos(pos.x(), pos.y(), pos.z());
        arrow.setArrowType(type);
        return arrow;
    }
}
