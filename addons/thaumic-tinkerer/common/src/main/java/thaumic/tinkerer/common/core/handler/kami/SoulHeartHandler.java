/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Dec 29, 2013, 9:33:23 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.core.handler.kami;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.network.packet.kami.PacketSoulHearts;

/**
 * The Soul Hearts: an extra health pool of up to 20 points (ten hearts) per player, filled one point at a time by the Soul
 * mode of the Awakened Ichorium Sword and spent before health when the player is hurt.
 *
 * <p>1.7.10 kept the number in the player's entity data, which a respawn does not copy (so the hearts are lost on death),
 * and spent it in {@code LivingHurtEvent}, before armour: the damage {@code (int) amount} is taken off the pool and the rest
 * (an int) is the damage that goes on. Here the number is saved with the world per player ({@link SoulHeartData}, cleared
 * when a player is re-created by a respawn), and sent to the player's client ({@link PacketSoulHearts}) on every change and on
 * login.</p>
 *
 * <p><b>The damage hook.</b> Architectury's {@code LIVING_HURT} can cancel a hit but not change its amount. A hit is
 * therefore swallowed (the damage animation and sound still play) when the pool covers it, and when the pool runs out
 * the hit is cancelled and the rest is dealt as a new hit of the same source (see {@link #onLivingHurt}). Exact 1.7.10
 * behaviour wants a hook that sets the amount; this is the closest the shared events allow, and it only acts for a player
 * that has soul hearts (docs/deviations/KamiWorld.md, docs/api-requests/KamiWorld.md).</p>
 */
public final class SoulHeartHandler {
    private static final String DATA_NAME = LibMisc.MOD_ID + "_soul_hearts";
    /** 1.7.10 {@code MAX_HP}: twenty points, ten hearts. */
    public static final int MAX_HP = 20;

    /** True while this handler deals the rest of a hit itself, so that hit is not taken apart again. Server thread only. */
    private static boolean reissuing;

    /**
     * Set by a loader hook that calls {@link #modifyIncomingDamage} from an event that can change the amount of a hit
     * (NeoForge {@code LivingIncomingDamageEvent}, a mixin on Fabric): the Architectury fallback of {@link #onLivingHurt}
     * then stands aside. False until the lead wires such a hook (docs/api-requests/KamiWorld.md).
     */
    public static volatile boolean exactDamageHook;

    private SoulHeartHandler() {}

    // ---- the pool ------------------------------------------------------------------------------------------------

    /** 1.7.10 {@code addHearts}: one point (half a heart), and the player's client is told. */
    public static void addHearts(Player player) {
        addHP(player, 1);
        updateClient(player);
    }

    /** Adds points up to {@link #MAX_HP}; false when the pool was already full. */
    public static boolean addHP(Player player, int hp) {
        int current = getHP(player);
        if (current >= MAX_HP) return false;

        setHP(player, afterAdd(current, hp));
        return true;
    }

    /** Takes points off the pool and returns the overflow: the part of {@code hp} the pool could not cover. */
    public static int removeHP(Player player, int hp) {
        int current = getHP(player);
        setHP(player, afterRemove(current, hp));
        return overflow(current, hp);
    }

    public static void setHP(Player player, int hp) {
        if (player instanceof ServerPlayer server) data(server.server).set(server.getUUID(), hp);
    }

    /** The soul hearts of a player, in points; 0 for a player that is not on a server. */
    public static int getHP(Player player) {
        return player instanceof ServerPlayer server ? data(server.server).get(server.getUUID()) : 0;
    }

    /** Sends the player's pool to the player's client (1.7.10 {@code updateClient}). */
    public static void updateClient(Player player) {
        if (player instanceof ServerPlayer server && server.connection != null) {
            NetworkManager.sendToPlayer(server, new PacketSoulHearts(getHP(player)));
        }
    }

    // ---- the pure rules ------------------------------------------------------------------------------------------

    /** The pool after adding: {@code min(MAX_HP, current + hp)}; a pool that is full does not change. */
    public static int afterAdd(int current, int hp) {
        return current >= MAX_HP ? current : Math.min(MAX_HP, current + hp);
    }

    /** The pool after taking {@code hp} off: never below 0. */
    public static int afterRemove(int current, int hp) {
        return Math.max(0, current - hp);
    }

    /** 1.7.10 {@code removeHP}'s return: the damage the pool could not cover, {@code max(0, hp - current)}. */
    public static int overflow(int current, int hp) {
        return Math.max(0, -(current - hp));
    }

    /**
     * What the pool does with a hit of {@code amount} (1.7.10 {@code onPlayerDamage}): the whole points of the hit are taken
     * off the pool and what is left is the damage that goes on, always a whole number (so a hit below one point is
     * swallowed and the fraction of every other hit is lost).
     */
    public static int damageAfterPool(int current, float amount) {
        return overflow(current, (int) amount);
    }

    /**
     * The amount to pass to {@code Player.hurt} so that, after the player's difficulty scaling, the damage is
     * {@code scaled}: the inverse of Easy ({@code min(x / 2 + 1, x)}) and Hard ({@code x * 3 / 2}); Normal does not scale.
     */
    public static float beforeDifficultyScaling(float scaled, boolean scalesWithDifficulty, Difficulty difficulty) {
        if (!scalesWithDifficulty) return scaled;
        return switch (difficulty) {
            case EASY -> scaled >= 2F ? 2F * (scaled - 1F) : scaled;
            case HARD -> scaled * 2F / 3F;
            default -> scaled;
        };
    }

    /**
     * 1.7.10 {@code onPlayerDamage} exactly, for a hook that can set the amount of the hit: the whole points of {@code amount}
     * are taken off the pool (always, whether the player has hearts or not) and the new amount is what is left, an int.
     * The pool is saved and the player's client told. Call it once per hit, before armour.
     */
    public static float modifyIncomingDamage(ServerPlayer player, float amount) {
        if (amount <= 0F) return amount;
        int rest = removeHP(player, (int) amount);
        updateClient(player);
        return rest;
    }

    // ---- the events ----------------------------------------------------------------------------------------------

    /** Registers the events (once, from the registrar). */
    public static void register() {
        EntityEvent.LIVING_HURT.register(SoulHeartHandler::onLivingHurt);
        PlayerEvent.PLAYER_JOIN.register(SoulHeartHandler::updateClient);
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wonGame) -> setHP(newPlayer, 0));
        PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd, reason) -> updateClient(newPlayer));
    }

    /**
     * 1.7.10 {@code onPlayerDamage} ({@code LivingHurtEvent}, before armour): a player that has soul hearts spends them
     * on the hit. A hit the pool covers is swallowed; one it does not cover is cancelled and dealt again with what is left.
     * Hits during the invulnerability frames after a hit are left to the game (it would ignore most of them), and so are
     * players without soul hearts.
     */
    public static EventResult onLivingHurt(LivingEntity entity, DamageSource source, float amount) {
        if (exactDamageHook || reissuing || !(entity instanceof ServerPlayer player) || amount <= 0F) return EventResult.pass();
        if (player.invulnerableTime > 10 && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) return EventResult.pass();
        if (getHP(player) <= 0) return EventResult.pass();

        int rest = removeHP(player, (int) amount);
        updateClient(player);
        if (rest <= 0) {
            // The hit is absorbed: no damage, but the player is seen and heard to be hit.
            if (player.level() instanceof ServerLevel level) level.broadcastDamageEvent(player, source);
            return EventResult.interruptFalse();
        }
        reissuing = true;
        try {
            player.hurt(source, beforeDifficultyScaling(rest, source.scalesWithDifficulty(), player.level().getDifficulty()));
        } finally {
            reissuing = false;
        }
        return EventResult.interruptFalse();
    }

    // ---- the saved numbers ---------------------------------------------------------------------------------------

    private static SoulHeartData data(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(SoulHeartData::new, SoulHeartData::load, null), DATA_NAME);
    }

    /** The soul hearts of every player, by UUID, saved with the world. */
    public static final class SoulHeartData extends SavedData {
        private final Map<UUID, Integer> hearts = new HashMap<>();

        public static SoulHeartData load(CompoundTag tag, HolderLookup.Provider registries) {
            SoulHeartData data = new SoulHeartData();
            for (Tag entry : tag.getList("hearts", Tag.TAG_COMPOUND)) {
                CompoundTag compound = (CompoundTag) entry;
                int value = compound.getInt("hp");
                if (value > 0) data.hearts.put(NbtUtils.loadUUID(compound.get("player")), value);
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (Map.Entry<UUID, Integer> entry : hearts.entrySet()) {
                CompoundTag compound = new CompoundTag();
                compound.put("player", NbtUtils.createUUID(entry.getKey()));
                compound.putInt("hp", entry.getValue());
                list.add(compound);
            }
            tag.put("hearts", list);
            return tag;
        }

        public int get(UUID player) {
            return hearts.getOrDefault(player, 0);
        }

        public void set(UUID player, int hp) {
            int old = get(player);
            if (hp > 0) hearts.put(player, hp);
            else hearts.remove(player);
            if (old != Math.max(0, hp)) setDirty();
        }
    }
}
