/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [13 Sep 2013, 15:00:40 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.core.handler;

import dev.architectury.event.events.client.ClientTickEvent;

/**
 * The client tick counter of the add-on.
 *
 * <p>1.7.10 did three things at the end of every client tick: it swapped the Thaumonomicon page of the ComputerCraft
 * peripherals research for {@code GuiResearchPeripheral} (a page that opened a web address on Enter), it ticked the tool
 * mode message of the Kami gear ({@code ToolModeHUDHandler.clientTick}) and it counted ticks. The first belongs to the
 * peripherals research, which the port drops with ComputerCraft (see {@code docs/deviations/BasicItems.md}); the
 * second is registered by the Kami group itself ({@code ToolModeHUDHandler.register}) and must not run twice; so what is
 * left is the counter.</p>
 *
 * <p>With the Enter shortcut gone, the text of the research page {@code PERIPHERALS} must not promise it: the group's language
 * fragments ({@code lang-extra/basic_items.*.json}) replace {@code ttresearch.page.PERIPHERALS.0} in every language with a
 * text that says the computer link is not part of this version. The counter is read by nobody in the add-on today (the Kami
 * group's {@code ToolModeHUDHandler} keeps one of its own); it stays because it is public in 1.7.10.</p>
 */
public final class ClientTickHandler {
    /** Client ticks since the game started (1.7.10 {@code elapsedTicks}). */
    public static int elapsedTicks;

    private static boolean registered;

    private ClientTickHandler() {}

    /** Hooks the counter into the end of the client tick; safe to call twice. */
    public static void register() {
        if (registered) return;
        registered = true;
        ClientTickEvent.CLIENT_POST.register(client -> tickEnd());
    }

    /** One tick has passed (1.7.10: {@code tickEnd} for the END phase). */
    public static void tickEnd() {
        ++elapsedTicks;
    }
}
