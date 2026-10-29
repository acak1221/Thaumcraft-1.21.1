/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.content;

import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.world.item.Item;
import thaumic.tinkerer.client.FociClient;
import thaumic.tinkerer.common.item.foci.FociComponents;
import thaumic.tinkerer.common.item.foci.ItemFocusDeflect;
import thaumic.tinkerer.common.item.foci.ItemFocusDislocation;
import thaumic.tinkerer.common.item.foci.ItemFocusEnderChest;
import thaumic.tinkerer.common.item.foci.ItemFocusFlight;
import thaumic.tinkerer.common.item.foci.ItemFocusHeal;
import thaumic.tinkerer.common.item.foci.ItemFocusSmelt;
import thaumic.tinkerer.common.item.foci.ItemFocusTelekinesis;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Feature group: the seven ordinary wand foci (Distortion, Dislocation, Ender Rift, Uprising, Mending, Efreet's Flame and
 * Telekinesis). The three Kami foci belong to {@link KamiGear}.
 *
 * <p>Each focus is an item of its own that mounts on a wand ({@code ItemFocusBasic}); research and recipes come from the
 * items themselves ({@code ITTinkererRegisterable}), as in 1.7.10. The registration order is the 1.7.10 one, which is the
 * order of the creative tab.</p>
 */
public final class Foci implements ContentGroup {
    public static final Foci INSTANCE = new Foci();

    private Foci() {}

    @Override
    public void register() {
        FociComponents.register();
        // 1.7.10 TTCommonProxy: ItemFocusDeflect.setupBlackList().
        ItemFocusDeflect.setupBlackList();

        TTRegistry.item(LibItemNames.FOCUS_DEFLECT, () -> new ItemFocusDeflect(new Item.Properties()));
        TTRegistry.item(LibItemNames.FOCUS_DISLOCATION, () -> new ItemFocusDislocation(new Item.Properties()));
        TTRegistry.item(LibItemNames.FOCUS_ENDER_CHEST, () -> new ItemFocusEnderChest(new Item.Properties()));
        TTRegistry.item(LibItemNames.FOCUS_FLIGHT, () -> new ItemFocusFlight(new Item.Properties()));
        TTRegistry.item(LibItemNames.FOCUS_HEAL, () -> new ItemFocusHeal(new Item.Properties()));
        TTRegistry.item(LibItemNames.FOCUS_SMELT, () -> new ItemFocusSmelt(new Item.Properties()));
        TTRegistry.item(LibItemNames.FOCUS_TELEKINESIS, () -> new ItemFocusTelekinesis(new Item.Properties()));

        // 1.7.10 PlayerTracker.onPlayerLogout: the progress of the Mending and Efreet's Flame foci is forgotten.
        PlayerEvent.PLAYER_QUIT.register(player -> {
            ItemFocusHeal.forget(player.getUUID());
            ItemFocusSmelt.forget(player.getUUID());
        });
    }

    @Override
    public void ready() {}

    @Override
    public void registerClient() {
        FociClient.register();
    }
}
