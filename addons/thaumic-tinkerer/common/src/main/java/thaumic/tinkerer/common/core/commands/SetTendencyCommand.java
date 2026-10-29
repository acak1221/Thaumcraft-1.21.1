/**
 * Created by pixlepix on 8/16/14.
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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumic.tinkerer.common.block.tile.TileInfusedGrain;
import thaumic.tinkerer.common.core.handler.ConfigHandler;

/**
 * {@code /setCropTendency <Aspect> <Count>}: a debug command (it exists only while {@code debugCommands.enabled} is on, as
 * {@code canCommandSenderUseCommand} returned that switch in 1.7.10, for everybody). It merges {@code Count} points of the
 * primal aspect into the tendencies of the infused crop the player looks at (the larger of the old and the new amount, as
 * {@code AspectList.merge} does) and then trims the tendencies to fifty points.
 *
 * <p>Both arguments are words, like the 1.7.10 string array: an aspect that is not primal does nothing, a count that is no
 * number answers "Invalid number". 1.7.10 found the crop with {@code BlockUtils.getTargetBlock}; this is
 * {@code player.pick} with the same reach of ten blocks.</p>
 */
public final class SetTendencyCommand {
    public static final String NAME = "setCropTendency";
    public static final String USAGE = "/setCropTendency <Aspect> <Count>";
    /** The reach of the look ray, in blocks (the 1.7.10 helper traced ten). */
    public static final double REACH = 10.0D;
    /**
     * The largest count that is merged. {@code reduceSaturatedAspects} removes one point at a time, so 1.7.10 spent two
     * billion iterations on the server thread for a count of 2147483647; 10000 trims to the same fifty points.
     */
    public static final int MAX_COUNT = 10000;

    /** What {@link #apply} did. */
    public enum Result {
        /** The tendency was merged. */
        DONE,
        /** The name is no primal aspect: nothing happened. */
        NO_SUCH_ASPECT,
        /** The count is no number ("Invalid number"). */
        INVALID_NUMBER
    }

    private SetTendencyCommand() {}

    /** Registers the command (once, from the registrar). */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> register(dispatcher));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NAME).requires(source -> ConfigHandler.enableDebugCommands)
                .then(Commands.argument("aspect", StringArgumentType.word())
                        .then(Commands.argument("count", StringArgumentType.word())
                                .executes(context -> run(context.getSource(), StringArgumentType.getString(context, "aspect"),
                                        StringArgumentType.getString(context, "count"))))));
    }

    private static int run(CommandSourceStack source, String aspect, String count) throws CommandSyntaxException {
        if (!ConfigHandler.enableDebugCommands) return 0;
        ServerPlayer player = source.getPlayerOrException();
        HitResult hit = player.pick(REACH, 1.0F, true);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return 0;
        ServerLevel level = player.serverLevel();
        BlockPos pos = blockHit.getBlockPos();
        if (!(level.getBlockEntity(pos) instanceof TileInfusedGrain tile)) return 0;

        Result result = apply(tile, aspect, count);
        if (result == Result.INVALID_NUMBER) {
            source.sendSystemMessage(Component.literal("Invalid number"));
        } else if (result == Result.DONE) {
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
            return 1;
        }
        return 0;
    }

    /**
     * 1.7.10 {@code processCommand} once the crop is found: for each primal aspect whose name equals the argument (case
     * does not matter), merge the number into the tendencies and trim them.
     */
    public static Result apply(TileInfusedGrain tile, String aspectName, String count) {
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            if (aspectName.equalsIgnoreCase(aspect.getName())) {
                try {
                    tile.primalTendencies.merge(aspect, Math.min(Integer.parseInt(count), MAX_COUNT));
                    tile.reduceSaturatedAspects();
                    tile.setChanged();
                    return Result.DONE;
                } catch (NumberFormatException e) {
                    return Result.INVALID_NUMBER;
                }
            }
        }
        return Result.NO_SUCH_ASPECT;
    }
}
