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

import dev.architectury.event.events.common.LifecycleEvent;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import thaumic.tinkerer.common.hooks.TinkererEvents;
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
 * <p><b>The damage hook.</b> {@link TinkererEvents#LIVING_DAMAGE} is the {@code LivingHurtEvent} of the original: it is
 * posted before armour and may set the amount, so {@link #onLivingDamage} applies the exact 1.7.10 rule ({@link
 * #modifyIncomingDamage}) to every hit of every server player. What the event does not give is the original's
 * invulnerability frames for a hit it cancels: in 1.7.10 {@code hurtResistantTime} was set <i>before</i> the event, so a hit
 * the pool swallowed still started the frames and a source that hits every tick (lava, fire, cactus) cost the pool once
 * per ten ticks; here a cancelled hit starts none (docs/HOOKS.md section 3), the same hit would be posted again on the next
 * tick and the pool would be gone ten times faster. The handler therefore keeps the frames of the original itself, per
 * player ({@link Frames}), for every hit it handles (docs/deviations/KamiWorld.md section 5). The NeoForge poster of the
 * event does not exist yet: until it does the Soul Hearts do not act there.</p>
 */
public final class SoulHeartHandler {
    private static final String DATA_NAME = LibMisc.MOD_ID + "_soul_hearts";
    /** 1.7.10 {@code MAX_HP}: twenty points, ten hearts. */
    public static final int MAX_HP = 20;

    /**
     * How long the invulnerability frames of a hit last, in game ticks: 1.7.10 refused a weaker hit while
     * {@code hurtResistantTime > maxHurtResistantTime / 2} (20 / 2 = 10), counting down from 20 at the hit, so the hit that
     * comes ten ticks after the first is a new one.
     */
    public static final int FRAME_TICKS = 10;

    /** The game's own frames run while {@code invulnerableTime} is above this ({@code LivingEntity.hurt}: {@code invulnerableTime > 10.0F}). */
    private static final int GAME_FRAMES_ABOVE = 10;

    /** The frames of the last hit that the pool saw, by player. Server thread only (the event and the player events are). */
    private static final Map<UUID, Frames> FRAMES = new HashMap<>();

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
        TinkererEvents.LIVING_DAMAGE.register(SoulHeartHandler::onLivingDamage);
        PlayerEvent.PLAYER_JOIN.register(SoulHeartHandler::updateClient);
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wonGame) -> {
            setHP(newPlayer, 0);
            FRAMES.remove(newPlayer.getUUID());
        });
        PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd, reason) -> updateClient(newPlayer));
        PlayerEvent.PLAYER_QUIT.register(player -> FRAMES.remove(player.getUUID()));
        LifecycleEvent.SERVER_STOPPED.register(server -> FRAMES.clear());
    }

    /**
     * The invulnerability frames of the original, as far as the pool needs them. {@code hurtResistantTime} was set to 20
     * <i>before</i> {@code LivingHurtEvent}; while it was above 10 a hit that was not stronger than {@code lastDamage} was
     * refused before the event (no event, no pool), and a stronger one reached the event as {@code amount - lastDamage}.
     * The game's own frames (which {@link TinkererEvents#LIVING_DAMAGE} is posted inside of) do not cover a hit that
     * is cancelled, and compare the next hit with what the listener returned instead of with the raw amount, so this
     * keeps the original's numbers: when the hit began them and the strongest raw amount so far, and, apart from the frames, what
     * the game's own {@code lastHurt} holds because of the amounts handed back to it ({@link #handed}). One record per player,
     * kept for as long as the player is on the server: {@link #handed} outlives the pool's frames, which can end before the
     * game's own.
     */
    private static final class Frames {
        /** The game tick of the hit that began the pool's current frames. Meaningful only after {@link #begin}. */
        long start;
        /** 1.7.10 {@code lastDamage}: the raw amount of the strongest hit of the pool's current frames. */
        float raw;
        /**
         * What the game's {@code lastHurt} holds as far as the pool knows: the last amount it handed back (the game sets {@code lastHurt} to
         * the amount it is handed, in its frames and out of them). The game's frames and the pool's begin at different times (a covered
         * hit begins the pool's and not the game's), so the two can be running or over independently; the game is asked
         * ({@code invulnerableTime}) when this value matters.
         */
        float handed;
        private boolean begun;

        void begin(long now, float amount) {
            this.start = now;
            this.raw = amount;
            this.begun = true;
        }

        boolean running(long now) {
            return begun && now >= start && now - start < FRAME_TICKS;
        }
    }

    /**
     * 1.7.10 {@code onPlayerDamage} ({@code LivingHurtEvent}, before armour) as a listener of {@link TinkererEvents#LIVING_DAMAGE}:
     * the pool takes the whole points of the hit and the rest is the damage that goes on, for every server player (the
     * original did not ask whether the player had hearts). Answers only for server players; anything else gets its amount back.
     *
     * <p>The frames of the original are kept here ({@link Frames}). Inside them a hit that is not stronger than the strongest
     * so far is the one the original refused before the event: it is cancelled and the pool is not charged. A stronger one is
     * charged for the difference only ({@code amount - lastDamage}). Outside the frames the hit is charged whole and begins
     * new frames. A hit the pool covers entirely is cancelled: the game starts no frames of its own and the hit is silent (no
     * knockback, no hurt sound or animation, {@code hurt} returns {@code false}: HOOKS.md section 3). Only a covered hit that
     * <i>began</i> the frames is announced to the clients with the damage event, so the player sees and hears the hit as in
     * 1.7.10, where the covered hit still showed. A hit with something left is dealt, and the game's frames start with the
     * returned amount as {@code lastHurt}.</p>
     *
     * <p><b>What the game is handed.</b> The game turns the returned amount into damage by its own rule: while its frames run
     * ({@code invulnerableTime > 10}, the source not in {@code bypasses_cooldown}) it deals {@code returned - lastHurt} and sets
     * {@code lastHurt = returned}; otherwise it deals {@code returned} and starts its frames with {@code lastHurt = returned}. The
     * pool wants the game to deal exactly {@code rest}, so it hands back {@code handed + rest} while the game's frames run (the game
     * subtracts what it has {@code handed} already) and {@code rest} alone when they do not. Which of the two it is depends on the
     * game's frames, not on the pool's: they start at different times (a hit the pool covers starts the pool's and not the game's)
     * and so also end at different times, in either order. Handing {@code handed + rest} when the game's frames are over would
     * deal {@code handed} too much, and handing {@code rest} when they run would deal too little or nothing.</p>
     */
    public static float onLivingDamage(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof ServerPlayer player)) return amount;
        long now = player.level().getGameTime();
        boolean bypassesCooldown = source.is(DamageTypeTags.BYPASSES_COOLDOWN);
        Frames frames = FRAMES.get(player.getUUID());
        boolean inFrames = frames != null && frames.running(now) && !bypassesCooldown;
        float event;
        if (inFrames) {
            if (amount <= frames.raw) return 0F; // the original refused it before the event
            event = amount - frames.raw;
            frames.raw = amount;
        } else {
            event = amount;
            if (frames == null) {
                frames = new Frames();
                FRAMES.put(player.getUUID(), frames);
            }
            frames.begin(now, amount);
        }
        float rest = modifyIncomingDamage(player, event);
        if (!(rest > 0F)) {
            if (!inFrames && player.level() instanceof ServerLevel level) level.broadcastDamageEvent(player, source);
            return 0F;
        }
        boolean gameInFrames = player.invulnerableTime > GAME_FRAMES_ABOVE && !bypassesCooldown;
        frames.handed = gameInFrames ? frames.handed + rest : rest;
        return frames.handed;
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
