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
package thaumic.tinkerer.common.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumic.tinkerer.common.core.handler.ConfigHandler;

/**
 * {@code /maxresearch}: a debug command (it exists only while {@code debugCommands.enabled} is on, as
 * {@code canCommandSenderUseCommand} returned that switch in 1.7.10). It adds 99 points to every aspect of the player who
 * uses it, as {@code ResearchManager.completeAspect(player, aspect, 99)} did, which also made the aspect known to the player,
 * and answers "Added 99 research to all aspects". 1.7.10 did nothing when something else than a player used it.
 */
public final class MaxResearchCommand {
    public static final String NAME = "maxresearch";
    /** The 1.7.10 usage text. */
    public static final String USAGE = "Adds 99 to all aspects of the player using it";
    public static final int AMOUNT = 99;

    private MaxResearchCommand() {}

    /** Registers the command (once, from the mod initialisation). */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> register(dispatcher));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NAME).requires(source -> ConfigHandler.enableDebugCommands)
                .executes(context -> {
                    if (!ConfigHandler.enableDebugCommands) return 0;
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) return 0;
                    apply(player);
                    player.sendSystemMessage(Component.literal("Added 99 research to all aspects"));
                    return 1;
                }));
    }

    /** Adds {@link #AMOUNT} to the pool of every aspect and makes each of them known to the player. */
    public static void apply(ServerPlayer player) {
        for (Aspect aspect : Aspect.aspects.values()) {
            ThaumcraftApiHelper.discoverAspect(player, aspect);
            ThaumcraftApiHelper.addAspectPool(player, aspect, AMOUNT);
        }
    }
}
