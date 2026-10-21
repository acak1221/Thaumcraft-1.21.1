package thaumcraft.common.lib.ward;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import thaumcraft.common.tiles.TileOwned;

/**
 * The original's warded-object protection of the ender pearl.
 *
 * <p>The original watched every entity that joined the world and destroyed an ender pearl that appeared within reach of an
 * owned block: it scanned the cube from -5 to 5 on every axis around the pearl for a {@code TileOwned} block entity, told the
 * thrower why the pearl died and killed the pearl (oracle
 * {@code thaumcraft/common/lib/events/EventHandlerEntity.java}:358-379, the {@code entitySpawns} handler).
 *
 * <p>The 1.21.1 equivalent of the original's {@code EntityJoinWorldEvent} is the entity admission of the level's entity
 * manager, which Architectury publishes as {@link EntityEvent#ADD}. The rule is applied on the server only, exactly like the
 * original, and the pearl dies the way the original killed it: it is marked dead and leaves the world on the next tick.
 *
 * <p>Oracle: {@code thaumcraft/common/lib/events/EventHandlerEntity.java}, lines 358-379.
 */
public final class WardedPearl {
    /** The original's scan around the pearl: -5..5 on every axis (oracle EventHandlerEntity.entitySpawns:366-378). */
    public static final int RADIUS = 5;
    /** The original's message, kept in the language file as {@code tc.warded.enderpearl}. */
    public static final String MESSAGE_KEY = "tc.warded.enderpearl";

    private static boolean eventsEnabled;

    private WardedPearl() {}

    /** Registers the admission rule. Call once from the common bootstrap. */
    public static void enableEvents() {
        if (eventsEnabled) return;
        eventsEnabled = true;
        EntityEvent.ADD.register((entity, reason) -> {
            if (entity instanceof ThrownEnderpearl pearl && pearl.level() instanceof ServerLevel server) destroy(server, pearl);
            return EventResult.pass();
        });
    }

    /**
     * Whether an owned block stands within the original's cube around the position. Chunks that are not loaded are skipped:
     * the original only ever read the blocks of the loaded world.
     */
    public static boolean nearOwnedBlock(Level level, BlockPos pos) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    BlockPos candidate = pos.offset(x, y, z);
                    if (!level.hasChunkAt(candidate)) continue;
                    if (level.getBlockEntity(candidate) instanceof TileOwned) return true;
                }
            }
        }
        return false;
    }

    /** The original's rule: a pearl next to a warded object dies, and its thrower is told why. */
    public static void destroy(ServerLevel level, ThrownEnderpearl pearl) {
        if (!nearOwnedBlock(level, pearl.blockPosition())) return;
        Entity thrower = pearl.getOwner();
        if (thrower instanceof Player player) {
            player.displayClientMessage(Component.translatable(MESSAGE_KEY).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
        }
        pearl.discard(); // The original's setDead(): the pearl leaves the world on the next tick.
    }
}
