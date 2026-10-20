package thaumcraft.common.lib.events;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchAdministration;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchRegistrationOrder;

/** Original privileged research/aspect/warp commands and the port's existing status diagnostics. */
public final class ThaumcraftCommands {
    private ThaumcraftCommands() {}

    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
            var root = dispatcher.register(root());
            dispatcher.register(Commands.literal("thaum").redirect(root));
            dispatcher.register(Commands.literal("tc").redirect(root));
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root() {
        return Commands.literal("thaumcraft")
                .then(Commands.literal("status").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("TC4 1.21.1 migration alpha: 48 aspects, "
                            + ResearchCatalog.all().size() + " research definitions. Full gameplay migration is unfinished."), false);
                    return 1;
                }))
                .then(Commands.literal("aspects").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    player.sendSystemMessage(Component.literal(KnowledgeAccess.get(player).getAspectsDiscovered().toString()));
                    return 1;
                }))
                .then(Commands.literal("help").requires(source -> source.hasPermission(2)).executes(context -> {
                    for (String text : new String[]{"You can also use /thaum or /tc instead of /thaumcraft.",
                            "/thaumcraft research <list|player> <all|reset|research>",
                            "/thaumcraft aspect <player> <aspect|all> <amount>",
                            "/thaumcraft warp <player> <add|set> <amount> [PERM|TEMP]",
                            "Not specifying PERM or TEMP changes normal warp."}) {
                        context.getSource().sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.DARK_AQUA), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("research").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("list").executes(context -> {
                            for (var definition : ResearchRegistrationOrder.enabled()) {
                                context.getSource().sendSuccess(() -> Component.literal(definition.key()).withStyle(ChatFormatting.DARK_PURPLE), false);
                            }
                            return ResearchRegistrationOrder.enabled().size();
                        }))
                        // Retain the port's existing self-grant spelling as an admin alias.
                        .then(Commands.literal("grant").then(Commands.argument("key", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (var definition : ResearchRegistrationOrder.enabled()) {
                                        if (definition.key().startsWith(builder.getRemaining())) builder.suggest(definition.key());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    var named = context.getSource().getServer().getPlayerList().getPlayerByName("grant");
                                    return research(context, named != null ? named : context.getSource().getPlayerOrException(),
                                            StringArgumentType.getString(context, "key"));
                                })))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .executes(context -> research(context, EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "key"))))))
                .then(Commands.literal("aspect").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("aspect", StringArgumentType.word())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ThaumcraftCommands::aspect)))))
                .then(Commands.literal("warp").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(warpAction("set", false, IntegerArgumentType.integer(0)))
                                .then(warpAction("add", true, IntegerArgumentType.integer(-100, 100)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> warpAction(String action, boolean add, IntegerArgumentType range) {
        return Commands.literal(action).then(Commands.argument("amount", range)
                .executes(context -> warp(context, add, ""))
                .then(Commands.argument("type", StringArgumentType.word())
                        .executes(context -> warp(context, add, StringArgumentType.getString(context, "type")))));
    }

    private static int research(CommandContext<CommandSourceStack> context, ServerPlayer player, String key) {
        var state = KnowledgeAccess.get(player);
        if (key.equalsIgnoreCase("all")) ResearchAdministration.grantAll(state);
        else if (key.equalsIgnoreCase("reset")) ResearchAdministration.resetResearch(state);
        else if (!ResearchAdministration.grant(state, key)) {
            context.getSource().sendFailure(Component.literal("Research does not exist."));
            return 0;
        }
        KnowledgeEvents.sync(player);
        player.sendSystemMessage(Component.literal(context.getSource().getTextName() + " changed your research: " + key).withStyle(ChatFormatting.DARK_PURPLE));
        success(context);
        return 1;
    }

    private static int aspect(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        String key = StringArgumentType.getString(context, "aspect");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        var state = KnowledgeAccess.get(player);
        if (key.equalsIgnoreCase("all")) {
            for (Aspect aspect : Aspect.aspects.values()) ResearchAdministration.giveAspect(state, aspect, amount);
        } else {
            Aspect chosen = Aspect.getAspect(key);
            if (chosen == null) {
                for (Aspect candidate : Aspect.aspects.values()) {
                    if (key.equalsIgnoreCase(candidate.getName())) { chosen = candidate; break; }
                }
            }
            if (chosen == null) {
                context.getSource().sendFailure(Component.literal("Aspect does not exist."));
                return 0;
            }
            ResearchAdministration.giveAspect(state, chosen, amount);
        }
        KnowledgeEvents.sync(player);
        player.sendSystemMessage(Component.literal(context.getSource().getTextName() + " gave you " + amount + " of " + key + ".").withStyle(ChatFormatting.DARK_PURPLE));
        success(context);
        return 1;
    }

    private static int warp(CommandContext<CommandSourceStack> context, boolean add, String type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        ResearchAdministration.warp(KnowledgeAccess.get(player), add, amount, type);
        KnowledgeEvents.sync(player);
        player.sendSystemMessage(Component.literal(context.getSource().getTextName() + (add ? " added " : " set your warp to ") + amount + (add ? " warp." : ".")).withStyle(ChatFormatting.DARK_PURPLE));
        success(context);
        return 1;
    }

    private static void success(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("Success!").withStyle(ChatFormatting.DARK_PURPLE), false);
    }
}
