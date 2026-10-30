/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.core.handler.kami;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import thaumic.tinkerer.common.hooks.TinkererEvents;
import thaumic.tinkerer.common.item.kami.armor.IchorAbsorption;
import thaumic.tinkerer.common.item.kami.armor.ItemGemBoots;
import thaumic.tinkerer.common.item.kami.armor.ItemGemChest;
import thaumic.tinkerer.common.item.kami.armor.ItemGemHelm;
import thaumic.tinkerer.common.item.kami.armor.ItemGemLegs;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.network.packet.kami.PacketArmorStatus;

/**
 * The master switch of the awakened armour (1.7.10: the {@code GemArmor} flag in the player's entity data, copied to the
 * client by {@code PlayerTracker} and set from the client by the toggle key) and the per-tick dispatch of the awakened
 * pieces.
 *
 * <p>The flag is saved with the world ({@link StatusData}, a set of the players that switched it off) and survives death.
 * A client keeps its own copy, set by {@link PacketArmorStatus} on login and by its key, because the effects that run on the
 * client (the boots' speed) need it every tick.</p>
 */
public final class KamiArmorHandler {
    private static final String DATA_NAME = LibMisc.MOD_ID + "_kami_armor";
    private static boolean clientEnabled = true;

    private KamiArmorHandler() {}

    /** Whether the awakened armour of this player is on; on a client, the client's own copy. */
    public static boolean getArmorStatus(Player player) {
        if (player.level().isClientSide) return clientEnabled;
        if (player instanceof ServerPlayer server) return !data(server.server).disabled.contains(server.getUUID());
        return true;
    }

    public static void setArmorStatus(ServerPlayer player, boolean status) {
        StatusData data = data(player.server);
        boolean changed = status ? data.disabled.remove(player.getUUID()) : data.disabled.add(player.getUUID());
        if (changed) data.setDirty();
    }

    /** Client side: the flag of this client (the key, or the server's answer after login). */
    public static void setClientStatus(boolean status) {
        clientEnabled = status;
    }

    public static boolean clientStatus() {
        return clientEnabled;
    }

    /** True when the player wears at least one awakened piece. */
    public static boolean wearsAwakenedArmor(Player player) {
        return isAwakened(player.getItemBySlot(EquipmentSlot.HEAD)) || isAwakened(player.getItemBySlot(EquipmentSlot.CHEST))
                || isAwakened(player.getItemBySlot(EquipmentSlot.LEGS)) || isAwakened(player.getItemBySlot(EquipmentSlot.FEET));
    }

    private static boolean isAwakened(ItemStack stack) {
        return stack.getItem() instanceof thaumic.tinkerer.common.item.kami.armor.ItemIchorclothArmorAdv;
    }

    /**
     * Registers the events of the armour: the tick of the players, the login synchronisation of the flag, and the loader
     * events that stand in for 1.7.10's Forge events ({@code docs/HOOKS.md}): the Gem Boots' jump and fall
     * ({@code LivingJumpEvent}, {@code LivingFallEvent}) and the Ichorcloth absorption ({@code ISpecialArmor}).
     */
    public static void register() {
        dev.architectury.event.events.common.TickEvent.PLAYER_POST.register(KamiArmorHandler::onPlayerTick);
        PlayerEvent.PLAYER_JOIN.register(KamiArmorHandler::sync);
        PlayerEvent.PLAYER_QUIT.register(player -> ItemGemChest.forget(player));
        // The listeners of one event run in the order of registration. As in 1.7.10 (the boots in preInit, Ascent Boost in init) the
        // boots' jump comes first: the Enchantments group registers Ascent Boost's in its ready phase (ModEnchantments.initJump).
        TinkererEvents.LIVING_JUMP.register(ItemGemBoots::onLivingJump);
        TinkererEvents.LIVING_FALL.register(ItemGemBoots::onLivingFall);
        TinkererEvents.LIVING_ARMOR_ABSORB.register(IchorAbsorption::onLivingArmorAbsorb);
    }

    /** 1.7.10 {@code PlayerTracker}: a joining player is told whether its armour is on. */
    public static void sync(ServerPlayer player) {
        NetworkManager.sendToPlayer(player, new PacketArmorStatus(getArmorStatus(player)));
    }

    /** What 1.7.10 ran from {@code LivingUpdateEvent} in each awakened item, in the order helmet, chestplate, leggings, boots. */
    public static void onPlayerTick(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.getItem() instanceof ItemGemHelm) ItemGemHelm.tickPlayer(player, head);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof ItemGemChest) ItemGemChest.tickPlayer(player, chest);
        ItemGemChest.updatePlayerFlyStatus(player);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        if (legs.getItem() instanceof ItemGemLegs) ItemGemLegs.tickPlayer(player, legs);
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        ItemGemBoots.tick(player, feet);
    }

    private static StatusData data(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(StatusData::new, StatusData::load, null), DATA_NAME);
    }

    /** The players that switched their awakened armour off. */
    public static final class StatusData extends SavedData {
        private final Set<UUID> disabled = new HashSet<>();

        static StatusData load(CompoundTag tag, HolderLookup.Provider registries) {
            StatusData data = new StatusData();
            for (Tag entry : tag.getList("disabled", Tag.TAG_INT_ARRAY)) data.disabled.add(NbtUtils.loadUUID(entry));
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (UUID id : disabled) list.add(NbtUtils.createUUID(id));
            tag.put("disabled", list);
            return tag;
        }
    }
}
