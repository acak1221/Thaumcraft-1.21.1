package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.wands.IFocusPouch;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.FocusSelectionSources;
import thaumcraft.common.lib.baubles.PlayerAccessories;

/** Original worn-pouch/main-inventory lookup and pouch-first focus return, with
 * a callback-free material plan and complete physical source validation.
 * A pouch is any {@link IFocusPouch}; its cells are read and written only through that interface.
 *
 * <p>An add-on's pouch may misbehave, and the selection is all or nothing whatever it does: a pouch whose
 * {@code getInventory} throws is inactive ({@link FocusSelectionSources#contents}); one whose {@code setInventory} throws
 * while the selection is planned (on scratch copies) gives no focus and takes none, and one that throws while the plan is
 * committed to the live stacks refuses the selection after every stack already written has been put back as it was.</p> */
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
        AccessoryNetworking.syncChanged(player);
        if (changed) player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                CAMERA_TICKS, SoundSource.PLAYERS, .3F, payload.inventorySlot() == -1 ? .9F : 1F);
        return changed;
    }

    private static boolean select(ServerPlayer player, FocusSelectionPayload payload) {
        if (payload == null || !player.isAlive() || player.isSpectator() || player.isRemoved() || player.hasDisconnected()
                ||player.server.getPlayerList().getPlayer(player.getUUID())!=player
                ||player.containerMenu!=player.inventoryMenu||!player.inventoryMenu.getCarried().isEmpty()) return false;
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
        var worn=PlayerAccessories.get(player);
        if(worn.isRejected())return false;
        ItemStack[] wornIdentities=new ItemStack[4],wornBefore=new ItemStack[4],wornAfter=new ItemStack[4];
        for(int slot=0;slot<4;slot++) {
            wornIdentities[slot]=worn.getItem(slot);wornBefore[slot]=wornIdentities[slot].copy();wornAfter[slot]=wornBefore[slot].copy();
        }

        ItemStack next = ItemStack.EMPTY;
        int source = payload.inventorySlot();
        if(FocusSelectionSources.isInventoryPouch(source)||FocusSelectionSources.isWornPouch(source)) {
            int outer=FocusSelectionSources.outerSlot(source),cell=FocusSelectionSources.pouchSlot(source);
            ItemStack container=FocusSelectionSources.isWornPouch(source)?wornAfter[outer]:after[outer];
            ItemStack[] contents=FocusSelectionSources.contents(container);
            // The address names a cell; a pouch smaller than the address (a crafted intent, or a pouch that changed) has none.
            if(contents==null||cell>=FocusSelectionSources.addressable(contents)||!FocusData.isSupportedFocus(contents[cell]))return false;
            next=contents[cell].copy();contents[cell]=ItemStack.EMPTY;
            // A pouch that cannot write the cell back cannot give the focus up either.
            if(!store(container,contents))return false;
        } else if (source >= 0) {
            if (!FocusData.isSupportedFocus(before[source])) return false;
            next = before[source].copy();
            // Original :495-508 extracts the ENTIRE single focus even in
            // creative, BEFORE returning the old one. This frees swap capacity.
            after[source] = ItemStack.EMPTY;
        } else if (previous.isEmpty()) {
            return false;
        }
        if (!previous.isEmpty()) {
            // Original addFocusToPouch checks all worn pouches before the36
            // main slots, and scans each pouch from cell0. Extraction above
            // frees a real source cell even when all other storage is full.
            boolean returned=returnToPouch(wornAfter,4,previous)||returnToPouch(after,36,previous);
            int destination = -1;
            // An unstackable focus uses the original first-empty 0..35 order;
            // on a full-inventory swap that is the just-freed source slot.
            if(!returned)for (int slot = 0; slot < 36; slot++) if (after[slot].isEmpty()) { destination = slot; break; }
            // Original :479-484 does not drop the focus on failed removal.
            // No deferred item-drop path, overflow deletion or creative copy.
            if (!returned&&destination == -1) return false;
            if(!returned)after[destination] = previous.copy();
        }
        FocusData replacement = next.isEmpty() ? null : new FocusData(next);
        // The final cells of every changed pouch, read back from the planned copy through the pouch's own interface: a pouch
        // that keeps its cells in any component is written the same way. A plan that cannot be read back refuses the selection.
        ItemStack[][] cells=new ItemStack[36][],wornCells=new ItemStack[4][];
        for(int slot=0;slot<36;slot++)
            if(!ItemStack.matches(before[slot],after[slot])&&before[slot].getItem() instanceof IFocusPouch&&(cells[slot]=FocusSelectionSources.contents(after[slot]))==null)return false;
        for(int slot=0;slot<4;slot++)
            if(!ItemStack.matches(wornBefore[slot],wornAfter[slot])&&(wornCells[slot]=FocusSelectionSources.contents(wornAfter[slot]))==null)return false;

        // Re-resolve actor, real hand, stack identity, all native components and
        // the COMPLETE inventory before the callback-free material commit.
        if (player.getItemInHand(payload.hand()) != wand || !ItemStack.matches(wand, wandBefore)) return false;
        for (int slot = 0; slot < identities.length; slot++) {
            if (inventory.getItem(slot) != identities[slot] || !ItemStack.matches(inventory.getItem(slot), before[slot])) return false;
        }
        if(player.containerMenu!=player.inventoryMenu||!player.inventoryMenu.getCarried().isEmpty()||worn.isRejected())return false;
        for(int slot=0;slot<4;slot++)if(worn.getItem(slot)!=wornIdentities[slot]||!ItemStack.matches(worn.getItem(slot),wornBefore[slot]))return false;
        // The only add-on code that runs on the live stacks. A pouch that fails here has possibly written part of its payload,
        // so the stacks written so far are put back and the wand keeps its focus: the extracted focus cannot be lost.
        boolean written = true;
        for (int slot = 0; slot < 36 && written; slot++) {
            if (!ItemStack.matches(before[slot], after[slot])) {
                if(cells[slot]!=null)written=store(identities[slot],cells[slot]);
                else inventory.setItem(slot, after[slot]);
            }
        }
        // Updating the existing worn physical pouch's contents in place avoids false
        // unequip/equip callbacks in the middle of a material transaction.
        for(int slot=0;slot<4&&written;slot++)if(wornCells[slot]!=null)written=store(wornIdentities[slot],wornCells[slot]);
        if(!written) {
            rollback(inventory,identities,before,wornIdentities,wornBefore);
            return false;
        }
        if (replacement == null) wand.remove(ThaumcraftFoci.WAND_FOCUS.get());
        else wand.set(ThaumcraftFoci.WAND_FOCUS.get(), replacement);
        inventory.setChanged();
        worn.setChanged();
        return true;
    }
    private static boolean returnToPouch(ItemStack[] outer,int size,ItemStack focus) {
        for(int slot=0;slot<size;slot++) {
            ItemStack[] contents=FocusSelectionSources.contents(outer[slot]);if(contents==null)continue;
            for(int cell=0,cells=FocusSelectionSources.addressable(contents);cell<cells;cell++)if(contents[cell].isEmpty()) {
                ItemStack unwritten=outer[slot].copy();
                contents[cell]=focus.copy();
                if(store(outer[slot],contents))return true;
                // The pouch would not take the focus (and may have changed its scratch copy while refusing): it is skipped like
                // an unreadable one, and the plan keeps the copy as it was.
                outer[slot]=unwritten;
                break;
            }
        }
        return false;
    }
    /** Writes the cells through the pouch's own method; false (and one log line per pouch class) when the add-on throws. */
    private static boolean store(ItemStack stack,ItemStack[] cells) {
        IFocusPouch pouch=(IFocusPouch)stack.getItem();
        try { pouch.setInventory(stack,cells);return true; }
        catch(RuntimeException refused){FocusSelectionSources.reportBrokenPouch(pouch,"setInventory",refused);return false;}
    }
    /**
     * Puts back what a failed commit may have touched: a slot whose stack was replaced gets its original object again, and a
     * pouch that was rewritten in place gets its original components again (no add-on code runs).
     */
    private static void rollback(Inventory inventory,ItemStack[] identities,ItemStack[] before,ItemStack[] wornIdentities,ItemStack[] wornBefore) {
        for(int slot=0;slot<identities.length;slot++) {
            if(inventory.getItem(slot)!=identities[slot])inventory.setItem(slot,identities[slot]);
            restore(identities[slot],before[slot]);
        }
        for(int slot=0;slot<wornIdentities.length;slot++)restore(wornIdentities[slot],wornBefore[slot]);
    }
    private static void restore(ItemStack live,ItemStack saved) {
        if(ItemStack.matches(live,saved))return;
        for(var type:List.copyOf(live.getComponents().keySet()))if(!saved.has(type))live.remove(type);
        live.applyComponents(saved.getComponents());
    }
}
