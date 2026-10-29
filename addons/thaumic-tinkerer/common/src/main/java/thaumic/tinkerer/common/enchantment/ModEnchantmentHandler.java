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
package thaumic.tinkerer.common.enchantment;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.api.ThaumcraftSoundIds;

/**
 * The event handler of the fourteen enchantments ({@code ModEnchantmentHandler} of 1.7.10).
 *
 * <p>Where Forge had an event, Architectury has one in most cases; where it has none the effect is made from what the game
 * offers instead. Every hook of 1.7.10 and what it became:</p>
 *
 * <ul>
 *   <li>{@code LivingHurtEvent}, changing the damage (Pounce, Final Strike, Valiance, Focused and Dispersed Strikes): the
 *       damage of the attack is prepared before the attack runs, see {@link AttackBoost}
 *       ({@code PlayerEvent.ATTACK_ENTITY}). Vampirism needs no change of damage and stays on
 *       {@code EntityEvent.LIVING_HURT}.</li>
 *   <li>{@code LivingUpdateEvent} (Slow Fall): {@code TickEvent.PLAYER_PRE}, the start of the player's tick, as before.</li>
 *       The bow ({@code itemInUseCount} of Quick Draw) has no counterpart that can be written to, so the arrow gets the
 *       speed of the longer draw when it leaves the bow ({@link #onEntityAdd}).
 *   <li>{@code LivingJumpEvent} (Ascent Boost): there is no jump event; the client detects the jump
 *       ({@code thaumic.tinkerer.client.core.handler.EnchantmentClientHandler}).</li>
 *   <li>{@code LivingFallEvent} (Shockwave): there is no fall event; the landing of a player is detected from the fall
 *       distance of the last tick in the air ({@link #onPlayerPost}).</li>
 *   <li>{@code BlockEvent.BreakEvent} (Tunnel): {@code BlockEvent.BREAK}.</li>
 *   <li>{@code PlayerEvent.BreakSpeed} (Shatter, Tunnel, Disintegrate, Flaming Touch): there is no such event; the dig
 *       speed attribute of the player follows the block under the crosshair, see {@link BreakSpeedControl}.</li>
 * </ul>
 */
public final class ModEnchantmentHandler {
    private static final Logger LOG = LoggerFactory.getLogger("ThaumicTinkerer");

    /** The fall distance of every player in the air with a fall distance, as seen at the end of the last tick (server only). */
    private static final Map<UUID, Float> AIRBORNE = new HashMap<>();

    private ModEnchantmentHandler() {}

    /** Registers the Architectury listeners. Called once by {@link ModEnchantments#initEnchantments()}. */
    static void register() {
        EntityEvent.LIVING_HURT.register(ModEnchantmentHandler::onEntityDamaged);
        PlayerEvent.ATTACK_ENTITY.register(ModEnchantmentHandler::onPlayerAttack);
        TickEvent.PLAYER_PRE.register(ModEnchantmentHandler::onEntityUpdate);
        TickEvent.PLAYER_POST.register(ModEnchantmentHandler::onPlayerPost);
        BlockEvent.BREAK.register(BreakSpeedControl::onBlockBreak);
        EntityEvent.ADD.register(ModEnchantmentHandler::onEntityAdd);
        PlayerEvent.PLAYER_QUIT.register(ModEnchantmentHandler::onPlayerQuit);
    }

    // ---- attacks ------------------------------------------------------------------------------------------------

    /** The attack of a player is about to run: arm it with the factor of the enchantments (see {@link AttackBoost}). */
    static EventResult onPlayerAttack(Player player, Level level, Entity target, InteractionHand hand,
            @Nullable EntityHitResult hit) {
        if (!level.isClientSide) AttackBoost.beforeAttack(player, target);
        return EventResult.pass();
    }

    /**
     * {@code onEntityDamaged}, the part that does not change the damage. The blow has reached its victim, so the
     * modifier of {@link AttackBoost} has done its work and is removed. Vampirism: the attacker heals by the level of the
     * enchantment on the held item and a zap is heard at the victim.
     */
    static EventResult onEntityDamaged(LivingEntity victim, DamageSource source, float amount) {
        if (victim.level().isClientSide) return EventResult.pass();

        Entity attacker = source.getEntity();
        if (attacker instanceof Player player) AttackBoost.afterAttack(player);

        if (attacker instanceof LivingEntity living) {
            ItemStack heldItem = living.getMainHandItem();
            int vampirism = ModEnchantments.level(heldItem, ModEnchantments.vampirism);
            // The original ran for every damage that was actually dealt; a hit inside the invulnerability frames of the
            // victim is not one (it deals nothing unless it is stronger than the last, and the strength is not known here).
            if (vampirism > 0 && amount > 0 && victim.invulnerableTime <= 10 && !victim.isDeadOrDying()
                    && !victim.isInvulnerableTo(source)) {
                living.heal(EnchantmentFormulas.vampirismHeal(vampirism));
                playZap(victim);
            }
        }
        return EventResult.pass();
    }

    private static void playZap(LivingEntity victim) {
        try {
            SoundEvent zap = ThaumcraftSoundIds.event(ThaumcraftSoundIds.ZAP);
            victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), zap, SoundSource.PLAYERS, 0.6F, 1F);
        } catch (IllegalArgumentException e) {
            LOG.debug("The zap sound of Thaumcraft is not registered, Vampirism is silent", e);
        }
    }

    // ---- the tick of a player -----------------------------------------------------------------------------------

    /**
     * {@code onEntityUpdate} ({@code LivingUpdateEvent}, highest priority): the start of the tick of a player. Slow Fall;
     * for a player of the server also the dig speed of the tool in the hand and the clean-up of an attack that never
     * reached its victim.
     */
    static void onEntityUpdate(Player player) {
        slowFall(player);

        if (player instanceof ServerPlayer serverPlayer) {
            AttackBoost.afterAttack(serverPlayer);
            BreakSpeedControl.update(serverPlayer);
        }
    }

    /**
     * Slow Fall, with the highest level on the armor. A client (which simulates the fall of its own player) divides the
     * vertical motion, and both sides reduce the fall distance. A server does not simulate the fall of a player, it only
     * counts the fall distance from the reports of the client, so it has no motion to look at and acts on the fall
     * distance alone.
     */
    private static void slowFall(Player player) {
        int slowFall = ModEnchantments.maxArmorLevel(player, ModEnchantments.slowFall);
        if (slowFall <= 0) return;

        if (player.level().isClientSide) {
            if (!player.isLocalPlayer()) return;
            Vec3 motion = player.getDeltaMovement();
            if (EnchantmentFormulas.slowFallActs(slowFall, player.isShiftKeyDown(), motion.y, player.fallDistance)) {
                player.setDeltaMovement(motion.x, EnchantmentFormulas.slowFallMotion(motion.y, slowFall), motion.z);
                player.fallDistance = EnchantmentFormulas.slowFallDistance(player.fallDistance, slowFall);

                // 1.7.10 spawned the cloud at posY - 1, and the posY of a player was the height of its eyes.
                Vec3 now = player.getDeltaMovement();
                player.level().addParticle(ParticleTypes.CLOUD, player.getX() + 0.25, player.getY() + 0.62,
                        player.getZ() + 0.25, -now.x, now.y, -now.z);
            }
        } else if (EnchantmentFormulas.slowFallActsOnServer(slowFall, player.isShiftKeyDown(), player.onGround(),
                player.fallDistance)) {
            player.fallDistance = EnchantmentFormulas.slowFallDistance(player.fallDistance, slowFall);
        }
    }

    /**
     * {@code onFall} ({@code LivingFallEvent}, low priority): Shockwave. The end of the tick of a player of the server.
     * The fall distance of the last tick in the air is the distance of the fall (the game resets the fall distance when the
     * player touches the ground, before the tick ends), and the first tick on the ground is the landing.
     */
    static void onPlayerPost(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        UUID id = serverPlayer.getUUID();
        if (serverPlayer.onGround()) {
            Float distance = AIRBORNE.remove(id);
            if (distance != null && !serverPlayer.getAbilities().mayfly) {
                int shockwave = ModEnchantments.level(serverPlayer.getItemBySlot(EquipmentSlot.FEET), ModEnchantments.shockwave);
                if (EnchantmentFormulas.shockwaveActs(shockwave, distance)) shockwave(serverPlayer, shockwave, distance);
            }
        } else if (serverPlayer.fallDistance > 0F) {
            AIRBORNE.put(id, serverPlayer.fallDistance);
        } else {
            AIRBORNE.remove(id);
        }
    }

    /** Every creature within ten blocks takes {@code .1 * shockwave * distance} of fall damage; the faller does not. */
    static void shockwave(ServerPlayer faller, int level, float distance) {
        ServerLevel world = faller.serverLevel();
        AABB area = new AABB(faller.getX() - EnchantmentFormulas.SHOCKWAVE_RADIUS, faller.getY() - EnchantmentFormulas.SHOCKWAVE_RADIUS,
                faller.getZ() - EnchantmentFormulas.SHOCKWAVE_RADIUS, faller.getX() + EnchantmentFormulas.SHOCKWAVE_RADIUS,
                faller.getY() + EnchantmentFormulas.SHOCKWAVE_RADIUS, faller.getZ() + EnchantmentFormulas.SHOCKWAVE_RADIUS);
        float damage = EnchantmentFormulas.shockwaveDamage(level, distance);
        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target != faller) target.hurt(world.damageSources().fall(), damage);
        }
    }

    private static void onPlayerQuit(ServerPlayer player) {
        AIRBORNE.remove(player.getUUID());
        AttackBoost.afterAttack(player);
    }

    // ---- Quick Draw -----------------------------------------------------------------------------------------------

    /**
     * Quick Draw. 1.7.10 took extra ticks off {@code itemInUseCount} while a bow was drawn. The draw clock of 1.21.1 cannot
     * be written to, so the arrow that leaves the bow is given the speed of the draw that the original would have made
     * of the same time: the bonus ticks of {@link EnchantmentFormulas#quickDrawBonusTicks} added to the ticks of the draw, a
     * full draw also making the arrow a critical one. (A draw shorter than the game's minimum power of 0.1 releases
     * no arrow, as it did not in 1.7.10 with the real draw time.)
     */
    static EventResult onEntityAdd(Entity entity, Level level) {
        if (level.isClientSide || !(entity instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof ServerPlayer player)
                || !player.isUsingItem()) {
            return EventResult.pass();
        }

        ItemStack bow = player.getUseItem();
        if (!(bow.getItem() instanceof BowItem)) return EventResult.pass();
        int quickDraw = ModEnchantments.level(bow, ModEnchantments.quickDraw);
        if (quickDraw <= 0) return EventResult.pass();

        int ticks = player.getTicksUsingItem();
        int bonus = EnchantmentFormulas.quickDrawBonusTicks(quickDraw, ticks);
        float power = BowItem.getPowerForTime(ticks);
        float boosted = BowItem.getPowerForTime(ticks + bonus);
        if (bonus > 0 && power > 0F && boosted > power) {
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(boosted / power));
            if (boosted >= 1.0F) arrow.setCritArrow(true);
        }
        return EventResult.pass();
    }
}
