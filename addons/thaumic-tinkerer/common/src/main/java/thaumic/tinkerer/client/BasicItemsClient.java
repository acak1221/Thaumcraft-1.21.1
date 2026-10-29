/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart: the 1.7.10 client proxy did this in
 * {@code TTClientProxy} and in each item's {@code registerIcons}/{@code getColorFromItemStack}). Distributed under the same
 * licence as the add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.minecraft.world.item.Item;
import thaumic.tinkerer.client.core.handler.ClientTickHandler;
import thaumic.tinkerer.client.core.handler.HUDHandler;
import thaumic.tinkerer.common.item.ItemCleansingTalisman;
import thaumic.tinkerer.common.item.ItemSpellCloth;
import thaumic.tinkerer.common.item.ItemXPTalisman;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Client registrations of the BasicItems group, called from {@code BasicItems.registerClient()}:
 * <ul>
 *   <li>the colour of the Spellbinding Cloth, which fades from violet to white as it wears out
 *       (1.7.10 {@code getColorFromItemStack});</li>
 *   <li>the model predicate {@code thaumictinkerer:enabled} of the two talismans: the switched-on texture of the Talisman
 *       of Remedium ({@code enabled} tag) and of the Talisman of Withhold (damage value 1), 1.7.10 {@code getIcon} and
 *       {@code getIconFromDamage};</li>
 *   <li>the overlay of the Focus of Dislocation ({@link HUDHandler}) and the tick counter ({@link ClientTickHandler}).</li>
 * </ul>
 * The Helmet of Revealing needs nothing here: its worn texture is the layer of its armour material.
 */
public final class BasicItemsClient {
    /** Item model predicate: 1 while a talisman is switched on, 0 otherwise. */
    public static final String PREDICATE_ENABLED = "enabled";

    private BasicItemsClient() {}

    public static void register() {
        Item cloth = TTRegistry.item(LibItemNames.SPELL_CLOTH);
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> tintIndex == 0 ? ItemSpellCloth.color(stack) : -1,
                cloth);

        ItemPropertiesRegistry.register(TTRegistry.item(LibItemNames.CLEANSING_TALISMAN),
                LibMisc.id(PREDICATE_ENABLED),
                (stack, level, entity, seed) -> ItemCleansingTalisman.isEnabled(stack) ? 1.0F : 0.0F);
        ItemPropertiesRegistry.register(TTRegistry.item(LibItemNames.XP_TALISMAN), LibMisc.id(PREDICATE_ENABLED),
                (stack, level, entity, seed) -> ItemXPTalisman.isAbsorbing(stack) ? 1.0F : 0.0F);

        HUDHandler.register();
        ClientTickHandler.register();
    }
}
