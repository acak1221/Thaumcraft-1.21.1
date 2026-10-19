package thaumcraft.common.items.wands.foci;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.crafting.ArcaneVis;

/** Dispatch AFTER active IWandable/node use, matching original ItemWandCasting:492-572. */
public final class WandFocusActions {
    // Original WandManager:679-704 uses wall time, shared by a player's foci,
    // not server ticks or a persistent item cooldown. Values retain no player,
    // world, server or stack. Entity.equals compares numeric IDs, so integrated
    // client/server players MUST use separate maps despite distinct instances.
    private static final Map<Player, Long> SERVER_COOLDOWNS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Player, Long> CLIENT_COOLDOWNS = Collections.synchronizedMap(new WeakHashMap<>());
    // Synchronous native callbacks cannot recursively start/spend another channel
    // for this same actor. Identity, not integrated-side Entity.equals; no retained
    // players after finally, and no persistent cooldown/progress substitute.
    private static final ThreadLocal<Set<Player>> ACTIVE = new ThreadLocal<>();
    private WandFocusActions() {}

    private static Map<Player, Long> cooldowns(Player player) {
        return player.level().isClientSide ? CLIENT_COOLDOWNS : SERVER_COOLDOWNS;
    }

    /** Read-only real-clock remainder, also useful to the native cooldown UI. */
    public static long cooldownMillis(Player player) {
        return Math.max(0L, cooldowns(player).getOrDefault(player, 0L) - System.currentTimeMillis());
    }

    /** Shock's original right-click cancels the initial dispatcher cooldown. */
    public static void clearCooldown(Player player) { cooldowns(player).remove(player); }
    public static void clearCooldowns() { SERVER_COOLDOWNS.clear(); CLIENT_COOLDOWNS.clear(); }

    private static boolean activate(Player player, ItemStack focus) {
        if (!FocusData.isSupportedFocus(focus)) return false;
        long now = System.currentTimeMillis();
        Map<Player, Long> cooldowns = cooldowns(player);
        if (cooldowns.getOrDefault(player, 0L) > now) return false;
        int cooldown = ((ItemFocusBasic) focus.getItem()).getActivationCooldown(focus);
        if (cooldown == 0) cooldowns.remove(player); // Fire remains an every-native-tick channel.
        else cooldowns.put(player, now + cooldown);
        return true;
    }

    private static boolean enter(Player player) {
        Set<Player> active = ACTIVE.get();
        if (active == null) { active = Collections.newSetFromMap(new IdentityHashMap<>()); ACTIVE.set(active); }
        return active.add(player);
    }
    private static boolean active(Player player) { Set<Player> active = ACTIVE.get(); return active != null && active.contains(player); }
    private static void leave(Player player) { Set<Player> active = ACTIVE.get(); if (active != null) { active.remove(player); if (active.isEmpty()) ACTIVE.remove(); } }

    public static InteractionResultHolder<ItemStack> use(Player player, InteractionHand hand) {
        ItemStack wand = player.getItemInHand(hand);
        ItemStack focus = FocusData.get(wand);
        if (!ArcaneVis.isCraftingWand(wand) || focus.isEmpty() || active(player) || !activate(player, focus) || !enter(player)) return InteractionResultHolder.pass(wand);
        try {
            ItemStack result = ((ItemFocusBasic) focus.getItem()).onFocusRightClick(wand, player.level(), player, hand);
            return result == null ? InteractionResultHolder.pass(wand) : InteractionResultHolder.consume(result);
        } finally { leave(player); }
    }

    /** True means a focus owns this tick, including an unfunded/client-only tick. */
    public static boolean onUseTick(Level level, LivingEntity entity, ItemStack wand, int remaining) {
        if (!(entity instanceof Player player) || !ArcaneVis.isCraftingWand(wand)) return false;
        ItemStack focus = FocusData.get(wand);
        if (focus.isEmpty()) return false;
        if (!player.isUsingItem() || player.getUseItem() != wand || player.getItemInHand(player.getUsedItemHand()) != wand) return true;
        // ItemWandCasting:547-557 sets this BEFORE invoking the focus, even if
        // the subsequent affordability check fails. Never reset on release.
        if (active(player) || !activate(player, focus) || !enter(player)) return true;
        try { ((ItemFocusBasic) focus.getItem()).onUsingFocusTick(wand, player, remaining); }
        finally { leave(player); }
        return true;
    }

    public static void onRelease(ItemStack wand, Level level, LivingEntity entity, int remaining) {
        if (!(entity instanceof Player player)) return;
        ItemStack focus = FocusData.get(wand);
        if (!focus.isEmpty()) ((ItemFocusBasic) focus.getItem()).onPlayerStoppedUsingFocus(wand, level, player, remaining);
    }
}
