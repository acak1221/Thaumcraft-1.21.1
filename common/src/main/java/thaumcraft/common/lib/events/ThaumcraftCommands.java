package thaumcraft.common.lib.events;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchCatalog;

/** Diagnostic/admin commands; not a replacement for the pending Thaumonomicon. */
public final class ThaumcraftCommands {
    private ThaumcraftCommands() {}
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> dispatcher.register(
                Commands.literal("thaumcraft")
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
                        .then(Commands.literal("research").requires(source -> source.hasPermission(2))
                                .then(Commands.literal("grant").then(Commands.argument("key", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (var definition : ResearchCatalog.all()) {
                                                if (definition.key().startsWith(builder.getRemaining())) builder.suggest(definition.key());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            String key = StringArgumentType.getString(context, "key");
                                            if (ResearchCatalog.get(key) == null) {
                                                context.getSource().sendFailure(Component.literal("Unknown TC4 research key: " + key));
                                                return 0;
                                            }
                                            var player = context.getSource().getPlayerOrException();
                                            boolean added = KnowledgeAccess.get(player).addResearch(key);
                                            KnowledgeEvents.sync(player);
                                            context.getSource().sendSuccess(() -> Component.literal(key + (added ? " granted" : " already known")), false);
                                            return added ? 1 : 0;
                                        }))))));
    }
}
