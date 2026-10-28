/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the client registrations of the KamiGear group.
 * Distributed under the same licence as the add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client;

import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import thaumic.tinkerer.client.core.handler.GemArmorKeyHandler;
import thaumic.tinkerer.client.core.handler.kami.KamiArmorClientHandler;
import thaumic.tinkerer.client.core.handler.kami.ShadowBeamClientHandler;
import thaumic.tinkerer.client.core.handler.kami.ToolModeHUDHandler;
import thaumic.tinkerer.client.gui.kami.GuiIchorPouch;
import thaumic.tinkerer.client.model.kami.ModelWings;
import thaumic.tinkerer.common.item.kami.ItemBlockTalisman;
import thaumic.tinkerer.common.item.kami.ItemIchorPouch;
import thaumic.tinkerer.common.item.kami.KamiHooks;
import thaumic.tinkerer.common.item.kami.tool.ToolHandler;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.network.packet.kami.PacketArmorStatus;
import thaumic.tinkerer.common.network.packet.kami.PacketShadowBeam;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Client registrations of the Kami gear, called from {@code KamiGear.registerClient()}: the pouch screen, the HUD message,
 * the toggle key, the item model predicates (the mode of an awakened tool, the state of the Black Hole Ring), the model
 * layer of the wings and the receivers of the server's packets.
 *
 * <p>Two things need a hook of the loader that Architectury does not have: adding the wings layer to the player
 * renderers and drawing the Placement Mirror's phantom blocks after the world. The loader glue of the add-on calls
 * {@code WingsLayer} and {@code PlacementMirrorPredictionRenderer.render} (Fabric: {@code KamiGearFabricClient}).</p>
 */
public final class KamiGearClient {
    /** Item model predicate: the mode of an awakened tool as 0, 0.5 or 1 (predicates are clamped to 0..1). */
    public static final String PREDICATE_MODE = "kami_mode";
    /** Item model predicate: 1 while a Black Hole Ring is active. */
    public static final String PREDICATE_ACTIVE = "kami_active";

    private KamiGearClient() {}

    public static void register() {
        KamiHooks.setLocalPlayerSupplier(() -> Minecraft.getInstance().player);
        ToolModeHUDHandler.register();
        GemArmorKeyHandler.register();

        MenuRegistry.registerScreenFactory(ItemIchorPouch.MENU.get(), GuiIchorPouch::new);
        EntityModelLayerRegistry.register(ModelWings.LAYER, ModelWings::createLayer);

        registerItemPredicates();

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PacketArmorStatus.TYPE, PacketArmorStatus.CODEC,
                (packet, context) -> context.queue(() -> KamiArmorClientHandler.onStatus(packet)));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PacketShadowBeam.TYPE, PacketShadowBeam.CODEC,
                (packet, context) -> context.queue(() -> ShadowBeamClientHandler.show(packet)));
    }

    private static void registerItemPredicates() {
        for (String path : new String[] { LibItemNames.ICHOR_PICK_GEM, LibItemNames.ICHOR_SHOVEL_GEM,
                LibItemNames.ICHOR_AXE_GEM, LibItemNames.ICHOR_SWORD_GEM }) {
            Item item = TTRegistry.item(path);
            ItemPropertiesRegistry.register(item, LibMisc.id(PREDICATE_MODE),
                    (stack, level, entity, seed) -> ToolHandler.getMode(stack) / 2.0F);
        }
        ItemPropertiesRegistry.register(TTRegistry.item(LibItemNames.BLOCK_TALISMAN), LibMisc.id(PREDICATE_ACTIVE),
                (stack, level, entity, seed) -> ItemBlockTalisman.isActive(stack) ? 1.0F : 0.0F);
    }
}
