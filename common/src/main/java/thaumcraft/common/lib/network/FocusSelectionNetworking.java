package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.foci.FocusData;

/** Original WandManager:444-523's loose-inventory branch, without unported focus pouches. */
public final class FocusSelectionNetworking {
    private static final SoundEvent CAMERA_TICKS = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "cameraticks"));
    private static boolean initialized;
    private FocusSelectionNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, FocusSelectionPayload.TYPE, FocusSelectionPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) handle(player, payload);
                }));
    }

    /** Client UI sends no item, components, price, vis, damage, player or world address. */
    public static void request(InteractionHand hand, int inventorySlot) {
        NetworkManager.sendToServer(new FocusSelectionPayload(hand, inventorySlot));
    }

    /** Actual production receiver; a direct runtime call does not prove wire dispatch. */
    public static boolean handle(ServerPlayer player, FocusSelectionPayload payload) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Focus selection requires the server thread");
        boolean changed = select(player, payload);
        // Correct refusals and late/replayed selections too. All slot/component
        // writes finish before observers, networking or sound can call back.
        player.inventoryMenu.sendAllDataToRemote();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        if (changed) player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                CAMERA_TICKS, SoundSource.PLAYERS, .3F, payload.inventorySlot() == -1 ? .9F : 1F);
        return changed;
    }

    private static boolean select(ServerPlayer player, FocusSelectionPayload payload) {
        if (payload == null || !player.isAlive() || player.isSpectator()) return false;
        ItemStack wand = player.getItemInHand(payload.hand());
        if (wand.getCount() != 1 || !ArcaneVis.isCraftingWand(wand)) return false;
        ItemStack wandBefore = wand.copy();
        ItemStack previous = FocusData.get(wand);
        var inventory = player.getInventory();
        ItemStack[] identities = new ItemStack[inventory.getContainerSize()];
        ItemStack[] before = new ItemStack[identities.length];
        ItemStack[] after = new ItemStack[identities.length];
        for (int slot = 0; slot < identities.length; slot++) {
            identities[slot] = inventory.getItem(slot);
            before[slot] = identities[slot].copy();
            after[slot] = before[slot].copy();
        }

        ItemStack next = ItemStack.EMPTY;
        int source = payload.inventorySlot();
        if (source >= 0) {
            if (!FocusData.isSupportedFocus(before[source])) return false;
            next = before[source].copy();
            // Original :495-508 extracts the ENTIRE single focus even in
            // creative, BEFORE returning the old one. This frees swap capacity.
            after[source] = ItemStack.EMPTY;
        } else if (previous.isEmpty()) {
            return false;
        }
        if (!previous.isEmpty()) {
            int destination = -1;
            // An unstackable focus uses the original first-empty 0..35 order;
            // on a full-inventory swap that is the just-freed source slot.
            for (int slot = 0; slot < 36; slot++) if (after[slot].isEmpty()) { destination = slot; break; }
            // Original :479-484 does not drop the focus on failed removal.
            // No deferred item-drop path, overflow deletion or creative copy.
            if (destination == -1) return false;
            after[destination] = previous.copy();
        }
        FocusData replacement = next.isEmpty() ? null : new FocusData(next);

        // Re-resolve actor, real hand, stack identity, all native components and
        // the COMPLETE inventory before the callback-free material commit.
        if (player.getItemInHand(payload.hand()) != wand || !ItemStack.matches(wand, wandBefore)) return false;
        for (int slot = 0; slot < identities.length; slot++) {
            if (inventory.getItem(slot) != identities[slot] || !ItemStack.matches(inventory.getItem(slot), before[slot])) return false;
        }
        for (int slot = 0; slot < 36; slot++) {
            if (!ItemStack.matches(before[slot], after[slot])) inventory.setItem(slot, after[slot]);
        }
        if (replacement == null) wand.remove(ThaumcraftFoci.WAND_FOCUS.get());
        else wand.set(ThaumcraftFoci.WAND_FOCUS.get(), replacement);
        inventory.setChanged();
        return true;
    }
}
