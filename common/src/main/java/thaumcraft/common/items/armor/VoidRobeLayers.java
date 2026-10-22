package thaumcraft.common.items.armor;

import java.util.List;

/**
 * The two worn layers of the void thaumaturge robe of the original.
 *
 * <p>Oracle {@code thaumcraft/common/items/armor/ItemVoidRobeArmor.java:98-100} — the worn look of
 * the void thaumaturge is two sheets: the layer that carries the dye is the {@code _overlay} sheet
 * and the layer that carries the metal trim is the plain sheet. The official Mojang 1.7.10
 * {@code client.jar} settles the order: {@code net.minecraft.client.renderer.entity.RenderPlayer}
 * draws the first layer with {@code ItemArmor.getArmorTexture(stack, entity, slot, null)} — for
 * the void robe the {@code _overlay} sheet — under the dye colour of the stack, and then, because
 * the piece carries a colour, draws the second layer with
 * {@code getArmorTexture(stack, entity, slot, "overlay")} — for the void robe the plain sheet —
 * untinted on top of it.</p>
 *
 * <p>The two sheets of the original are disjoint: of the 32768 pixels of the 256x128 sheet the
 * plain one owns 6224 and the overlay one 5652, and only 21 pixels belong to both. The trim and
 * the cloth therefore never fight for the same pixel and the trim stays untinted.</p>
 *
 * <p>Forced 1.21.1 change: none. The rule is a pure list so that it can be asserted without a
 * world, a player or a registry.</p>
 */
public final class VoidRobeLayers {
    private VoidRobeLayers() {}

    /** The two layers of the worn void thaumaturge, in the order the original draws them. */
    public enum Layer {
        /** The dyed cloth: the original's {@code void_robe_armor_overlay.png}, drawn first. */
        DYED_CLOTH("void_robe_armor_overlay.png", true),
        /** The untinted metal trim: the original's {@code void_robe_armor.png}, drawn after it. */
        UNTINTED_TRIM("void_robe_armor.png", false);

        private final String sheet;
        private final boolean dyed;

        Layer(String sheet, boolean dyed) { this.sheet = sheet; this.dyed = dyed; }

        /** The file name of the original sheet of this layer. */
        public String sheet() { return sheet; }

        /** Whether the original tints this layer with the dye colour of the stack. */
        public boolean dyed() { return dyed; }
    }

    /** The worn layers of the void thaumaturge, the dyed cloth first and the trim after it. */
    public static final List<Layer> WORN_LAYERS = List.of(Layer.DYED_CLOTH, Layer.UNTINTED_TRIM);

    /** The default dye colour of the void thaumaturge (the original {@code 6961280}). */
    public static final int DEFAULT_DYE = 0x6A3880;

    /** The layer at the given index, or null when the index is not one of the two layers. */
    public static Layer layer(int index) {
        return index >= 0 && index < WORN_LAYERS.size() ? WORN_LAYERS.get(index) : null;
    }

    /** Whether the layer at the given index carries the dye colour of the stack. */
    public static boolean layerIsDyed(int index) {
        Layer layer = layer(index);
        return layer != null && layer.dyed();
    }
}
