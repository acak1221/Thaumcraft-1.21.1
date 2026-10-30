/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.hooks;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The events that Architectury has no equivalent for, which the 1.7.10 mod used through Forge events
 * ({@code LivingHurtEvent}, {@code PlayerEvent.BreakSpeed}, {@code LivingJumpEvent}, {@code LivingFallEvent},
 * {@code LivingDropsEvent} and the {@code itemInUseCount} of the bow). They are plain Architectury {@link Event}s: a
 * listener is registered with {@code TinkererEvents.X.register(listener)} and removed with {@code unregister(listener)}.
 * Loader modules post them: on Fabric the mixins in {@code thaumic.tinkerer.fabric.mixin}, on NeoForge (not written yet) the
 * handlers of the matching NeoForge events (see {@code docs/HOOKS.md}). Common code never posts them itself.
 *
 * <h2>Rules that hold for every event</h2>
 * <ul>
 *   <li>Listeners run in the order they were registered. Events that change a value pass the value from one listener to the
 *       next, so the order matters (a multiplication before an addition is not an addition before a multiplication).</li>
 *   <li>A listener runs on the thread of the side that posted the event (the server thread, or the client thread for the
 *       events that are posted on the client too) and must be fast: some events are posted every tick.</li>
 *   <li>A listener that throws brings the exception into the game code that posted the event, as a listener of a Forge event
 *       would. Do not throw.</li>
 *   <li>Registration is not thread safe (Architectury's {@code Event}): register during mod initialisation, or on the server
 *       thread while nothing is posting (the GameTests do).</li>
 *   <li>The {@code post...} methods hold the rules of each event (what a returned value means). The loader modules call them,
 *       so both loaders behave alike; a listener never needs them.</li>
 * </ul>
 */
public final class TinkererEvents {
    private TinkererEvents() {}

    // ---- LIVING_DAMAGE ---------------------------------------------------------------------------------------------

    /** A listener of {@link #LIVING_DAMAGE}. */
    @FunctionalInterface
    public interface LivingDamage {
        /**
         * @param victim the entity that is about to be hurt
         * @param source the damage source ({@link DamageSource#getEntity()} is the attacker, if any)
         * @param amount the amount so far: the one the entity was hurt with, or what the listeners before this one returned;
         *               always positive
         * @return the amount to go on with. Return {@code amount} to leave it alone; a result of zero or less (or NaN)
         *         cancels the damage altogether.
         */
        float onLivingDamage(LivingEntity victim, DamageSource source, float amount);
    }

    private static final LivingDamage NO_DAMAGE_LISTENERS = (victim, source, amount) -> amount;

    /**
     * Posted on the <b>server</b> every time a living entity is about to take damage: from {@code LivingEntity.hurt}, after
     * the checks that make an entity ignore a hit (the entity is invulnerable to the source, it is dead, it is on the client,
     * it has Fire Resistance against fire) and <b>before</b> anything reduces or absorbs the amount: before the blocking of a
     * shield, the extra damage of freezing and the helmet's share, before the invulnerability frames compare it with the last
     * hit, before the armour, the Protection enchantments, Resistance and the Absorption hearts. It is the position of the
     * {@code LivingHurtEvent} of 1.7.10 whose {@code ammount} the enchantments (Pounce, Final Strike, Valiance, Focused and
     * Dispersed Strikes) and the Soul Hearts changed, and of NeoForge's {@code LivingIncomingDamageEvent}.
     * Posted for every living entity, player or not, whatever dealt the damage (a melee blow, an arrow, thorns, a fall, fire);
     * a hit of zero damage (a snowball) is not posted. For a player the amount is already scaled by the difficulty
     * ({@code Player.hurt} does that before it calls the living entity's).
     *
     * <p>A listener may change the amount by returning a different one (the next listener sees it), and cancels the damage
     * by returning zero: {@code LivingEntity.hurt} then returns {@code false} at once, as for an invulnerable entity - no
     * damage, no hurt animation or sound, no knockback, no invulnerability frames, nothing is recorded as the last hit. Once
     * a listener cancels, the ones after it are not called.</p>
     *
     * <p>Not posted when a mod sets the health directly instead of hurting the entity.</p>
     */
    public static final Event<LivingDamage> LIVING_DAMAGE = EventFactory.of(listeners -> {
        if (listeners.isEmpty()) return NO_DAMAGE_LISTENERS;
        LivingDamage[] chain = listeners.toArray(new LivingDamage[0]);
        return (victim, source, amount) -> {
            float current = amount;
            for (LivingDamage listener : chain) {
                current = listener.onLivingDamage(victim, source, current);
                if (!(current > 0.0F)) return 0.0F;
            }
            return current;
        };
    });

    /**
     * Runs the listeners of {@link #LIVING_DAMAGE} (the loader modules call this).
     *
     * @return the amount to hurt with; zero when a listener cancelled the damage. An {@code amount} that is not positive is
     *         returned as it is, without posting.
     */
    public static float postLivingDamage(LivingEntity victim, DamageSource source, float amount) {
        if (!(amount > 0.0F)) return amount;
        float result = LIVING_DAMAGE.invoker().onLivingDamage(victim, source, amount);
        return result > 0.0F ? result : 0.0F;
    }

    // ---- BREAK_SPEED -----------------------------------------------------------------------------------------------

    /** A listener of {@link #BREAK_SPEED}. */
    @FunctionalInterface
    public interface BreakSpeed {
        /**
         * @param player the player (a real one, or the fake player of the Animation Tablet) who digs
         * @param state  the block state that is dug
         * @param pos    the position of the block; {@code null} if a poster does not know it (the Fabric and NeoForge posters
         *               always do)
         * @param speed  the speed so far: what {@code Player.getDestroySpeed} worked out (the tool, Efficiency, Haste, Mining
         *               Fatigue, water and being in the air included), or what the listeners before this one returned;
         *               always positive
         * @return the speed to go on with. Return {@code speed} to leave it alone; a result of zero or less (or NaN) means that
         *         the player cannot dig the block.
         */
        float onBreakSpeed(Player player, BlockState state, @Nullable BlockPos pos, float speed);
    }

    private static final BreakSpeed NO_SPEED_LISTENERS = (player, state, pos, speed) -> speed;

    /**
     * Posted on <b>both sides</b> wherever the destroy progress of a block is worked out for a player, which is
     * {@code BlockBehaviour.getDestroyProgress}, that is {@code BlockState.getDestroyProgress(player, level, pos)}: every
     * tick of the digging of the server player (and when the dig starts), the client's prediction of the same player (the
     * block cracks and breaks on the client on its own count, so a server-only event would show a wrong crack and send the
     * break too early or too late; this is the 1.7.10 {@code PlayerEvent.BreakSpeed}), and any mod that asks a player's
     * progress itself. That includes a {@code ServerPlayer} that is never ticked, such as the fake player of the Animation
     * Tablet when it calls {@code state.getDestroyProgress(fakePlayer, level, pos)}. Not posted for a block that cannot be
     * dug at all (hardness -1), for the instant breaking of creative mode, or for a block whose class overrides
     * {@code getDestroyProgress} without calling {@code super}.
     *
     * <p>The speed is the dig speed that goes into {@code speed / hardness / 30} (or {@code / 100} when the tool does not
     * harvest the block). A listener changes it by returning another value; a result of zero or less means that the player
     * cannot dig the block (the progress is zero, so the block never breaks and the crack does not grow; the same
     * result on both sides keeps the prediction right). Once a listener says that, the ones after it are not called.
     * A listener that depends on state only one side has must make sure the other side agrees, or the prediction of the
     * client drifts from the server.</p>
     */
    public static final Event<BreakSpeed> BREAK_SPEED = EventFactory.of(listeners -> {
        if (listeners.isEmpty()) return NO_SPEED_LISTENERS;
        BreakSpeed[] chain = listeners.toArray(new BreakSpeed[0]);
        return (player, state, pos, speed) -> {
            float current = speed;
            for (BreakSpeed listener : chain) {
                current = listener.onBreakSpeed(player, state, pos, current);
                if (!(current > 0.0F)) return 0.0F;
            }
            return current;
        };
    });

    /**
     * Runs the listeners of {@link #BREAK_SPEED} (the loader modules call this).
     *
     * @return the speed to dig with; zero when the player cannot dig the block. A {@code speed} that is not positive is
     *         returned as it is, without posting.
     */
    public static float postBreakSpeed(Player player, BlockState state, @Nullable BlockPos pos, float speed) {
        if (!(speed > 0.0F)) return speed;
        float result = BREAK_SPEED.invoker().onBreakSpeed(player, state, pos, speed);
        return result > 0.0F ? result : 0.0F;
    }

    // ---- LIVING_JUMP -----------------------------------------------------------------------------------------------

    /** A listener of {@link #LIVING_JUMP}. */
    @FunctionalInterface
    public interface LivingJump {
        void onLivingJump(LivingEntity entity);
    }

    private static final LivingJump NO_JUMP_LISTENERS = entity -> { };

    /**
     * Posted on <b>both sides</b> at the end of {@code LivingEntity.jumpFromGround}, after the vertical motion of the jump
     * (and the push of a sprinting jump) is applied: the entity's delta movement already holds the jump, and a listener may
     * change it ({@code entity.setDeltaMovement}, as Ascent Boost does). The 1.7.10 {@code LivingJumpEvent}. Posted for every
     * living entity that jumps by that method (players by the jump key, mobs by their jump control); not posted when the
     * jump power is zero, and not for a mounted horse's jump, a swimming bob or a slime's own hop that do not use the method.
     * The server's player and the client's copy of the same player each run it, so a listener has to change the motion by
     * the same rule on both sides. It cannot be cancelled.
     */
    public static final Event<LivingJump> LIVING_JUMP = EventFactory.of(listeners -> {
        if (listeners.isEmpty()) return NO_JUMP_LISTENERS;
        LivingJump[] chain = listeners.toArray(new LivingJump[0]);
        return entity -> {
            for (LivingJump listener : chain) listener.onLivingJump(entity);
        };
    });

    /** Runs the listeners of {@link #LIVING_JUMP} (the loader modules call this). */
    public static void postLivingJump(LivingEntity entity) {
        LIVING_JUMP.invoker().onLivingJump(entity);
    }

    // ---- LIVING_FALL -----------------------------------------------------------------------------------------------

    /** What a fall is worked out from; the listeners of {@link #LIVING_FALL} change it. */
    public static final class FallData {
        private float distance;
        private float damageMultiplier;

        public FallData(float distance, float damageMultiplier) {
            this.distance = distance;
            this.damageMultiplier = damageMultiplier;
        }

        /** The distance fallen, in blocks. The damage is {@code ceil((distance - safe fall distance) * multiplier * fall damage attribute)}. */
        public float getDistance() {
            return distance;
        }

        public void setDistance(float distance) {
            this.distance = distance;
        }

        /** The damage multiplier of the block landed on (1, and lower on hay bales, honey and the like). */
        public float getDamageMultiplier() {
            return damageMultiplier;
        }

        public void setDamageMultiplier(float damageMultiplier) {
            this.damageMultiplier = damageMultiplier;
        }
    }

    /** A listener of {@link #LIVING_FALL}. */
    @FunctionalInterface
    public interface LivingFall {
        /**
         * @return {@link EventResult#pass()} to go on with the next listener and the fall; any interrupting result
         *         ({@link EventResult#interruptFalse()} by convention) cancels the fall, and the listeners after it are not
         *         called.
         */
        EventResult onLivingFall(LivingEntity entity, FallData fall);
    }

    private static final LivingFall NO_FALL_LISTENERS = (entity, fall) -> EventResult.pass();

    /**
     * Posted on <b>both sides</b> at the start of {@code LivingEntity.causeFallDamage}, where the fall damage of a landing is
     * worked out (the 1.7.10 {@code LivingFallEvent}, which Forge posted from {@code EntityLivingBase.fall} on both sides, and
     * NeoForge's, which has no side check either). It is posted on the client too because vanilla works out the landing there
     * as well and the local player plays the sound of a hard fall by that count: a listener that only runs on the server
     * (Slow Fall taking the damage away) would leave the client with the thud of a fall that did nothing. <b>A listener with an
     * effect on the world (Shockwave's blast) must test {@code entity.level().isClientSide} and act on the server only.</b>
     * Posted for every living entity that lands with a fall distance (including one that has no damage to take: the
     * distance is the listener's to look at), and for the passengers of a vehicle that lands (each with the distance the
     * vehicle's event left). A listener may change the distance and the damage multiplier in {@code fall} (Slow Fall,
     * Shockwave's threshold), and may cancel the fall: {@code causeFallDamage} then returns {@code false} at once, without
     * damage, sound or the fall of the passengers.
     */
    public static final Event<LivingFall> LIVING_FALL = EventFactory.of(listeners -> {
        if (listeners.isEmpty()) return NO_FALL_LISTENERS;
        LivingFall[] chain = listeners.toArray(new LivingFall[0]);
        return (entity, fall) -> {
            for (LivingFall listener : chain) {
                EventResult result = listener.onLivingFall(entity, fall);
                if (result != null && result.interruptsFurtherEvaluation()) return result;
            }
            return EventResult.pass();
        };
    });

    /**
     * Runs the listeners of {@link #LIVING_FALL} (the loader modules call this).
     *
     * @return true when a listener cancelled the fall
     */
    public static boolean postLivingFall(LivingEntity entity, FallData fall) {
        EventResult result = LIVING_FALL.invoker().onLivingFall(entity, fall);
        return result != null && result.interruptsFurtherEvaluation();
    }

    // ---- USE_ITEM_TICK ---------------------------------------------------------------------------------------------

    /** A listener of {@link #USE_ITEM_TICK}. */
    @FunctionalInterface
    public interface UseItemTick {
        /**
         * @param entity    the entity that holds the use of an item (a bow being drawn, food being eaten)
         * @param stack     the stack in use
         * @param remaining the ticks of use that remain, as the entity counts them: {@code getUseItemRemainingTicks()}
         * @return the remaining ticks to go on with; return {@code remaining} to leave it alone. A result below 1 is raised
         *         to 1 by the poster, so the use ends on this tick as it does in vanilla when the count runs down to zero.
         */
        int onUseItemTick(LivingEntity entity, ItemStack stack, int remaining);
    }

    private static final UseItemTick NO_USE_LISTENERS = (entity, stack, remaining) -> remaining;

    /**
     * Posted on <b>both sides</b> at the start of every tick of {@code LivingEntity.updateUsingItem}, while an entity holds the
     * use of an item (before the item's {@code onUseTick} and before the count goes down by one). The 1.7.10
     * {@code itemInUseCount} of the bow that Quick Draw took extra ticks off. The listener may change the remaining ticks
     * (a draw that is shorter by the ticks taken off); the count is the entity's own {@code useItemRemaining}, so what reads
     * it back sees the change: {@code getTicksUsingItem}, which the bow's pose and the arrow's power follow, and
     * {@code releaseUsing} with the ticks left. The client runs it for its own copy of the player, so a listener must change
     * the count by the same rule on both sides. It cannot be cancelled.
     */
    public static final Event<UseItemTick> USE_ITEM_TICK = EventFactory.of(listeners -> {
        if (listeners.isEmpty()) return NO_USE_LISTENERS;
        UseItemTick[] chain = listeners.toArray(new UseItemTick[0]);
        return (entity, stack, remaining) -> {
            int current = remaining;
            for (UseItemTick listener : chain) current = listener.onUseItemTick(entity, stack, current);
            return current;
        };
    });

    /**
     * Runs the listeners of {@link #USE_ITEM_TICK} (the loader modules call this).
     *
     * @return the remaining ticks to set, at least 1
     */
    public static int postUseItemTick(LivingEntity entity, ItemStack stack, int remaining) {
        int result = USE_ITEM_TICK.invoker().onUseItemTick(entity, stack, remaining);
        return result < 1 ? 1 : result;
    }

    // ---- LIVING_DROPS ----------------------------------------------------------------------------------------------

    /** A listener of {@link #LIVING_DROPS}. */
    @FunctionalInterface
    public interface LivingDrops {
        /**
         * @param victim       the entity that died
         * @param source       what killed it
         * @param drops        the item entities of the death, not yet in the world: the loot table, the equipment and, for a
         *                     player, the inventory. The list can be changed freely: remove entries, add entities
         *                     ({@code new ItemEntity(level, x, y, z, stack)}; give them a pickup delay and motion as you
         *                     want them), clear it.
         * @param lootingLevel the level of Looting on the attacker's weapon ({@code source.getEntity()}), 0 if there is none
         * @param recentlyHit  true if a player hit the victim lately (the condition of the loot table's player drops)
         * @return {@link EventResult#pass()} to go on; an interrupting result ({@link EventResult#interruptFalse()} by
         *         convention) cancels the drops: none of the entities in the list is spawned, and the listeners after it are
         *         not called
         */
        EventResult onLivingDrops(LivingEntity victim, DamageSource source, List<ItemEntity> drops, int lootingLevel,
                boolean recentlyHit);
    }

    private static final LivingDrops NO_DROPS_LISTENERS = (victim, source, drops, looting, recentlyHit) -> EventResult.pass();

    /**
     * Posted on the <b>server</b> when a living entity has died and its death loot is worked out
     * ({@code LivingEntity.dropAllDeathLoot}), after the loot table, the custom loot and the equipment of the victim
     * have produced their item entities and before any of them is in the world (the 1.7.10 {@code LivingDropsEvent}, and
     * NeoForge's). The listener sees the drops as a mutable list and may remove, replace or add to them, or cancel them all.
     * Only the item entities of the victim are in the list (what it spawns at its own position through
     * {@code spawnAtLocation}, and a player's inventory); experience orbs are not items and are not in it, as in 1.7.10, and
     * an item that another mod spawns somewhere else with {@code addFreshEntity} is not captured. Whatever stays in the list
     * is added to the level after the listeners, in order.
     *
     * <p>When nobody listens the loot is spawned as vanilla does (see {@link #hasLivingDropsListeners()}).</p>
     */
    public static final Event<LivingDrops> LIVING_DROPS = EventFactory.of(listeners -> {
        if (listeners.isEmpty()) return NO_DROPS_LISTENERS;
        LivingDrops[] chain = listeners.toArray(new LivingDrops[0]);
        return (victim, source, drops, looting, recentlyHit) -> {
            for (LivingDrops listener : chain) {
                EventResult result = listener.onLivingDrops(victim, source, drops, looting, recentlyHit);
                if (result != null && result.interruptsFurtherEvaluation()) return result;
            }
            return EventResult.pass();
        };
    });

    /** True when something listens to {@link #LIVING_DROPS}; a poster that has nobody to tell lets the drops spawn as vanilla. */
    public static boolean hasLivingDropsListeners() {
        return LIVING_DROPS.invoker() != NO_DROPS_LISTENERS;
    }

    /**
     * Runs the listeners of {@link #LIVING_DROPS} (the loader modules call this).
     *
     * @return true when a listener cancelled the drops (the poster then spawns nothing of {@code drops})
     */
    public static boolean postLivingDrops(LivingEntity victim, DamageSource source, List<ItemEntity> drops, int lootingLevel,
            boolean recentlyHit) {
        EventResult result = LIVING_DROPS.invoker().onLivingDrops(victim, source, drops, lootingLevel, recentlyHit);
        return result != null && result.interruptsFurtherEvaluation();
    }

    /** The level of Looting on the weapon of the attacker of {@code source}, 0 when it has no living attacker or no Looting. */
    public static int lootingLevel(ServerLevel level, DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity attacker)) return 0;
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.LOOTING)
                .map(looting -> EnchantmentHelper.getEnchantmentLevel(looting, attacker)).orElse(0);
    }
}
