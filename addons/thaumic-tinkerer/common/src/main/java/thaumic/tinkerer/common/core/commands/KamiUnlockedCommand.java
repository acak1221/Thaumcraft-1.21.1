/**
 * Created by Katrina on 27/02/14.
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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumic.tinkerer.common.lib.LibResearch;

/**
 * {@code /iskamiunlocked}: tells the player whether the research that the Kami entries need has been completed
 * ("Yes"), or "No" and the keys still missing. Everyone may use it (1.7.10 permission level 0).
 *
 * <p>The requirement is the hidden parents of the {@code ICHOR} entry, which the Kami research collects from every other
 * entry (1.7.10: {@code ResearchHelper.kamiResearch.parentsHidden}).</p>
 */
public final class KamiUnlockedCommand {
    public static final String NAME = "iskamiunlocked";

    private KamiUnlockedCommand() {}

    /** Registers the command (once, from the registrar). */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> register(dispatcher));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NAME).executes(context -> run(context.getSource())));
    }

    private static int run(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ResearchItem ichor = ResearchCategories.getResearch(LibResearch.KEY_ICHOR);
        if (ichor == null || ichor.parentsHidden == null) {
            player.sendSystemMessage(Component.literal("No"));
            return 0;
        }
        List<String> parents = Arrays.asList(ichor.parentsHidden);
        Set<String> unlocked = ThaumcraftApiHelper.getCompletedResearch(player);
        if (unlocked.containsAll(parents)) {
            player.sendSystemMessage(Component.literal("Yes"));
            return 1;
        }
        player.sendSystemMessage(Component.literal("No"));
        List<String> list = new ArrayList<>(parents);
        list.removeAll(unlocked);
        player.sendSystemMessage(Component.literal("Remaining: " + String.join(",", list)));
        return 0;
    }
}
