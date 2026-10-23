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
     * states. It cannot express selection among 1.21.1 BlockState properties; metadata dispatch
     * substitutes defaultBlockState. Use BlockState registration/query/dispatch for exact states,
     * or explicit whole-Block forms when every state is intended.</p>
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

    /**
     * Retained metadata form; {@code meta} is ignored and every state of the block is registered.
     *
     * @deprecated Use {@link #registerWandBlockTrigger(IWandTriggerManager, int, Block, String)}
     * for all states, or {@link #registerWandBlockTrigger(IWandTriggerManager, int, BlockState, String)}
     * for one exact state. Metadata cannot select a state in 1.21.1.
     */
    @Deprecated
    public static void registerWandBlockTrigger(IWandTriggerManager manager, int event, Block block, int meta, String modid) {
        registerWandBlockTrigger(manager, event, block, modid);
    }

    /** The original default-modid form. */
    public static void registerWandBlockTrigger(IWandTriggerManager manager, int event, Block block) {
        registerWandBlockTrigger(manager, event, block, DEFAULT);
    }

    /**
     * Retained metadata form using the default mod id; {@code meta} is ignored.
     *
     * @deprecated Use {@link #registerWandBlockTrigger(IWandTriggerManager, int, Block)}
     * for all states, or the {@code BlockState} overload with an explicit mod id for one state.
     */
    @Deprecated
    public static void registerWandBlockTrigger(IWandTriggerManager manager, int event, Block block, int meta) {
        registerWandBlockTrigger(manager, event, block, DEFAULT);
    }

    /** Registers a trigger for one exact block state. */
    public static void registerWandBlockTrigger(IWandTriggerManager manager, int event, BlockState state, String modid) {
        if (manager == null || state == null) return;
        triggers.computeIfAbsent(modid == null ? DEFAULT : modid, ignored -> new HashMap<>())
                .put(state, new Trigger(manager, event));
    }

    /** Returns whether any mod registered this whole block; null returns false. Exact-state entries are not queried. */
    public static boolean hasTrigger(Block block) {
        return hasTrigger(block, (String) null);
    }

    /**
     * The original metadata form.
     *
     * <p>Forced 1.21.1 change: 1.21.1 has no block metadata, so the metadata of the original is ignored
     * and a trigger registered for a block answers for every state of that block.</p>
     *
     * @deprecated Use {@link #hasTrigger(Block)} for whole-block registrations or
     * {@link #hasTrigger(BlockState)} to include exact-state registrations. Metadata is ignored.
     */
    @Deprecated
    public static boolean hasTrigger(Block block, int meta) {
        return hasTrigger(block, (String) null);
    }

    /**
     * The original per-mod form.
     *
     * <p>Original {@code WandTriggerRegistry:47-53}: the answer is the exact registration of the block
     * or the wildcard registration of the whole block. Forced 1.21.1 change: the metadata no longer
     * exists, so the wildcard registration is the one of the whole block.</p>
     *
     * @deprecated Use {@link #hasTrigger(Block, String)} for whole-block registrations or
     * {@link #hasTrigger(BlockState, String)} to include exact-state registrations. Metadata is ignored.
     */
    @Deprecated
    public static boolean hasTrigger(Block block, int meta, String modid) {
        return hasTrigger(block, modid);
    }

    /**
     * Queries whole-block registrations without dispatching a callback.
     * A null block returns false; a null mod id searches all mods. Exact-state entries are not queried.
     */
    public static boolean hasTrigger(Block block, String modid) {
        if (block == null) return false;
        for (var entry : triggers.entrySet()) {
            if (modid != null && !entry.getKey().equals(modid)) continue;
            if (entry.getValue().containsKey(block)) return true;
        }
        return false;
    }

    /** Returns whether any mod registered this state or its whole block; null returns false. */
    public static boolean hasTrigger(BlockState state) {
        return hasTrigger(state, null);
    }

    /**
     * The original answer for a clicked block.
     *
     * <p>Original {@code WandTriggerRegistry:38-45}: the exact registration of the block and the
     * metadata answered for every registration of the whole block. The native registry keys a trigger
     * either by one exact block state or by the whole block, so the state answer has to consult both
     * forms, exactly like {@link #performTrigger} does. Before this fix the public answer and the
     * public dispatch disagreed for a trigger that was registered for a whole block.</p>
     */
    public static boolean hasTrigger(BlockState state, String modid) {
        if (state == null) return false;
        for (var entry : triggers.entrySet()) {
            if (modid != null && !entry.getKey().equals(modid)) continue;
            Map<Object, Trigger> registered = entry.getValue();
            if (registered.containsKey(state)) return true;
            if (registered.containsKey(state.getBlock())) return true;
        }
        return false;
    }

    /** Dispatches the trigger of a block state; the first successful trigger wins. */
    public static boolean performTrigger(Level level, ItemStack wand, Player player, int x, int y, int z, int side, BlockState state) {
        return performTrigger(level, wand, player, x, y, z, side, state, null);
    }

    /**
     * The original block and metadata form of the dispatch.
     *
     * <p>Forced 1.21.1 change: 1.21.1 has no block metadata, so the metadata argument is ignored and
     * the exact state of the block is its default state. The wildcard registration of the whole block
     * is still the fallback, exactly as in the original.</p>
     *
     * @deprecated Use {@link #performTrigger(Level, ItemStack, Player, int, int, int, int, BlockState)}
     * with the actual clicked state. This overload ignores metadata and uses the block's default state.
     */
    @Deprecated
    public static boolean performTrigger(Level level, ItemStack wand, Player player, int x, int y, int z, int side, Block block, int meta) {
        return performTrigger(level, wand, player, x, y, z, side, block, meta, null);
    }

    /**
     * Retained per-mod dispatch; metadata is ignored and a null block returns false.
     *
     * @deprecated Use {@link #performTrigger(Level, ItemStack, Player, int, int, int, int, BlockState, String)}
     * with the actual clicked state, not a legacy metadata value.
     */
    @Deprecated
    public static boolean performTrigger(Level level, ItemStack wand, Player player, int x, int y, int z, int side, Block block, int meta, String modid) {
        if (block == null) return false;
        return performTrigger(level, wand, player, x, y, z, side, block.defaultBlockState(), modid);
    }

    /**
     * Dispatches matching callbacks until one returns true; returns false for a null state or no success.
     * Exact-state registrations take precedence over whole-block registrations within each mod.
     * A null mod id searches all mods, with unspecified order between mods. Coordinates are block
     * coordinates and {@code side} is passed through unchanged. Level, wand and player, including null,
     * are passed to the callback; its side effects and exceptions are not suppressed.
     */
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
