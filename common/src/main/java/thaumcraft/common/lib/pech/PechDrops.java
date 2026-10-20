package thaumcraft.common.lib.pech;

import net.minecraft.world.entity.item.ItemEntity;

/** Original EntityItem Thrower="PechDrop". Native throwers are UUIDs; this
 * persisted ENTITY marker retains the string predicate without modifying the
 * reward's components, stacking identity, pickup permission or owner UUID. */
public final class PechDrops {
    private PechDrops(){}
    public interface Marker {boolean thaumcraft$isPechDrop();void thaumcraft$setPechDrop(boolean value);}
    public static boolean isMarked(ItemEntity item){return item instanceof Marker marker&&marker.thaumcraft$isPechDrop();}
    public static void mark(ItemEntity item){if(!(item instanceof Marker marker))throw new IllegalStateException("Pech drop marker mixin is not installed");marker.thaumcraft$setPechDrop(true);}
}
