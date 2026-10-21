package thaumcraft.api.wands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The original TC4 wand trigger registry.
 *
 * <p>Forced 1.21.1 change: the original keyed a trigger by block and metadata; 1.21.1 has no block metadata,
 * so a trigger is keyed either by a whole block or by one exact block state. The original metadata argument
 * still exists and is ignored: a block that was registered for one metadata is now registered for all of its
 * states, because the flattened native blocks have no variants left.</p>
 */
public class WandTriggerRegistry {
    private record Trigger(IWandTriggerManager manager, int event) {}

    private static final Map<String, Map<Object, Trigger>> triggers = new HashMap<>();
    private static final String DEFAULT = "default";

    private WandTriggerRegistry() {}

    /** Registers a trigger for every state of a block. */
    public static void registerWandBlockTrigger(IWandTriggerManager manager, int event, Block block, String modid) {
        if (manager == null || block == null) return;
        triggers.computeIfAbsent(modid == null ? DEFAULT : modid, ignored -> new HashMap<>())
                .put(block, new Trigger(manager, event));
    }

    /** The original metadata form; the metadata no longer exists and is ignored. */
    public static void registerWandBlockTrigger(IWandTriggerManager manager, int event, Block block, int meta, String modid) {
        registerWandBlockTrigger(manager, event, block, modid);
    }

    /** The original default-modid form. */
    public static void registerWandBlockTrigger(IWandTriggerManager manager, int event, Block block) {
        registerWandBlockTrigger(manager, event, block, DEFAULT);
    }

    /** The original metadata form with the default mod id; the metadata is ignored. */
    public static void registerWandBlockTrigger(IWandTriggerManager manager, int event, Block block, int meta) {
        registerWandBlockTrigger(manager, event, block, DEFAULT);
    }

    /** Registers a trigger for one exact block state. */
    public static void registerWandBlockTrigger(IWandTriggerManager manager, int event, BlockState state, String modid) {
        if (manager == null || state == null) return;
        triggers.computeIfAbsent(modid == null ? DEFAULT : modid, ignored -> new HashMap<>())
                .put(state, new Trigger(manager, event));
    }

    public static boolean hasTrigger(Block block) {
        return hasTrigger(block, (String) null);
    }

    /**
     * The original metadata form.
     *
     * <p>Forced 1.21.1 change: 1.21.1 has no block metadata, so the metadata of the original is ignored
     * and a trigger registered for a block answers for every state of that block.</p>
     */
    public static boolean hasTrigger(Block block, int meta) {
        return hasTrigger(block, (String) null);
    }

    public static boolean hasTrigger(Block block, String modid) {
        if (block == null) return false;
        for (var entry : triggers.entrySet()) {
            if (modid != null && !entry.getKey().equals(modid)) continue;
            if (entry.getValue().containsKey(block)) return true;
        }
        return false;
    }

    public static boolean hasTrigger(BlockState state) {
        return hasTrigger(state, null);
    }

    public static boolean hasTrigger(BlockState state, String modid) {
        if (state == null) return false;
        for (var entry : triggers.entrySet()) {
            if (modid != null && !entry.getKey().equals(modid)) continue;
            if (entry.getValue().containsKey(state)) return true;
        }
        return false;
    }

    /** Dispatches the trigger of a block state; the first successful trigger wins. */
    public static boolean performTrigger(Level level, ItemStack wand, Player player, int x, int y, int z, int side, BlockState state) {
        return performTrigger(level, wand, player, x, y, z, side, state, null);
    }

    public static boolean performTrigger(Level level, ItemStack wand, Player player, int x, int y, int z, int side, BlockState state, String modid) {
        if (state == null) return false;
        for (var entry : new ArrayList<>(triggers.entrySet())) {
            if (modid != null && !entry.getKey().equals(modid)) continue;
            Trigger trigger = entry.getValue().get(state);
            if (trigger == null) trigger = entry.getValue().get(state.getBlock());
            if (trigger == null) continue;
            if (trigger.manager().performTrigger(level, wand, player, x, y, z, side, trigger.event())) return true;
        }
        return false;
    }

    /** The registered mod ids; the original kept them apart per mod. */
    public static List<String> modIds() {
        return List.copyOf(triggers.keySet());
    }
}
