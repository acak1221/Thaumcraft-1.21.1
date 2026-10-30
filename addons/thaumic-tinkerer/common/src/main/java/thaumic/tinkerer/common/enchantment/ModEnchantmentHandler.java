/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [29 Sep 2013, 11:52:00 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.enchantment;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.utils.value.IntValue;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.api.ThaumcraftSoundIds;
import thaumic.tinkerer.common.core.helper.ItemNBTHelper;
import thaumic.tinkerer.common.hooks.TinkererEvents;

/**
 * The event handler of the fourteen enchantments ({@code ModEnchantmentHandler} of 1.7.10), on the loader events of
 * {@link TinkererEvents} (see {@code docs/HOOKS.md}). Every handler of the original and what it became:
 *
 * <ul>
 *   <li>{@code LivingHurtEvent} ({@link #onEntityDamaged}): {@code TinkererEvents.LIVING_DAMAGE}. Pounce, Final Strike,
 *       Valiance, Focused and Dispersed Strikes change the amount, then Vampirism heals; for any living attacker, whatever
 *       the weapon of the blow (melee, the fake player of the Animation Tablet, arrows, thorns).</li>
 *   <li>{@code PlayerEvent.BreakSpeed} ({@link #onGetHarvestSpeed}): {@code TinkererEvents.BREAK_SPEED}. Shatter, Tunnel,
 *       Disintegrate and Flaming Touch, on both sides; a fake player that is never ticked gets them too.</li>
 *   <li>{@code BlockEvent.BreakEvent} ({@link #onBreakBlock}): Architectury's {@code BlockEvent.BREAK}. Tunnel remembers the
 *       way the player looked; the wear of Disintegrate and Flaming Touch.</li>
 *   <li>{@code LivingJumpEvent} ({@link #onPlayerJump}): {@code TinkererEvents.LIVING_JUMP}. Ascent Boost, on both sides.</li>
 *   <li>{@code LivingFallEvent}: {@code TinkererEvents.LIVING_FALL}. Slow Fall ({@link #onSlowFallLanding}, both sides) and
 *       Shockwave ({@link #onFall}, the server only), in that order, so that Shockwave sees the distance that is left.</li>
 *   <li>{@code LivingUpdateEvent} (Slow Fall's motion and cloud, {@link #onEntityUpdate}): Architectury's
 *       {@code TickEvent.PLAYER_PRE}, on the client of the player who falls. The Quick Draw part of the same 1.7.10 handler is
 *       {@code TinkererEvents.USE_ITEM_TICK} ({@link #onUseItemTick}), on both sides.</li>
 * </ul>
 */
public final class ModEnchantmentHandler {
    private static final Logger LOG = LoggerFactory.getLogger("ThaumicTinkerer");

    /** The keys of the strike counter on the weapon, as in 1.7.10. */
    public static final String NBT_LAST_TARGET = "TTEnchantLastTarget";
    public static final String NBT_LAST_TARGET_2 = "TTEnchantLastTarget2";
    public static final String NBT_SUCCESSIVE_STRIKE = "TTEnchantSuccessiveStrike";

    /** The key of the direction that Tunnel remembers on the tool, as in 1.7.10 ({@code NBTTunnelDirection}). */
    public static final String NBT_TUNNEL_DIRECTION = "TTEnchantTunnelDir";

    private ModEnchantmentHandler() {}

    /** Registers the listeners, once. Called by {@link ModEnchantments#initEnchantments()}. */
    static void register() {
        TinkererEvents.LIVING_DAMAGE.register(ModEnchantmentHandler::onEntityDamaged);
        TinkererEvents.BREAK_SPEED.register(ModEnchantmentHandler::onGetHarvestSpeed);
        TinkererEvents.USE_ITEM_TICK.register(ModEnchantmentHandler::onUseItemTick);
        // The chain runs in the order of registration: Slow Fall shortens the distance first (in 1.7.10 its effect had been
        // done tick by tick before the landing event), then Shockwave works with what is left. Shockwave's LOW priority of
        // 1.7.10 (after every other handler of the event) is NOT reproduced against the listeners of other groups: the
        // order of ContentGroups.ALL has KamiGear register the Gem Boots' listener after both (docs/deviations/KamiGear.md).
        TinkererEvents.LIVING_FALL.register(ModEnchantmentHandler::onSlowFallLanding);
        TinkererEvents.LIVING_FALL.register(ModEnchantmentHandler::onFall);
        BlockEvent.BREAK.register(ModEnchantmentHandler::onBreakBlock);
        TickEvent.PLAYER_PRE.register(ModEnchantmentHandler::onEntityUpdate);
    }

    /**
     * Registers Ascent Boost's jump listener, once, after every group has registered its own. The Gem Boots' jump listener
     * is registered in {@code TTRegistry.preInit} of 1.7.10 and this one in {@code init}, so the boots come first in the
     * chain of {@code TinkererEvents.LIVING_JUMP}; the registration phase of this group runs before KamiGear's, hence the
     * ready phase (docs/deviations/KamiGear.md section 3, "Order against Ascent Boost"). Called by
     * {@link ModEnchantments#initJump()}.
     */
    static void registerJump() {
        TinkererEvents.LIVING_JUMP.register(ModEnchantmentHandler::onPlayerJump);
    }

    // ---- damage: Pounce, Final Strike, Valiance, Focused and Dispersed Strikes, Vampirism ---------------------------

    /**
     * {@code onEntityDamaged} ({@code LivingHurtEvent}). The attacker is {@code source.getEntity()}, the entity that caused
     * the damage (the shooter of an arrow, the wearer of thorns), whatever it is, with the enchantments of its main hand (and
     * the leggings of a player, for Pounce). The factors multiply the amount in the order of the original (Pounce, Final
     * Strike, Valiance, Focused, Dispersed, {@link EnchantmentFormulas#modifyDamage}); Vampirism comes last and heals the
     * attacker and plays the zap at the victim.
     *
     * <p>Where the event stands (docs/HOOKS.md section 3): before armor, shield and the invulnerability frames, where 1.7.10's came
     * after the frames had taken the last hit off (so the factors multiply the whole amount of a hit that goes on, and
     * vanilla then takes the last hit off the result). A hit that the frames refuse by its raw amount (not above the last hit
     * as vanilla stored it, which is the listener's result) is not posted, so the counter of Focused and Dispersed Strikes and
     * Vampirism are not charged for it; that is not the 1.7.10 test, which compared the raw amounts: for a second blow inside
     * the frames the two differ in a band (a factor above one: a blow above the first raw blow but not above its boosted amount
     * is refused here and dealt there; a factor below one: a blow not above the first raw blow is posted here and refused
     * there, so the counter moves and Vampirism heals for a blow that deals nothing; docs/deviations/Enchantments.md section 2).</p>
     *
     * <p>Every entity that a hit reaches is a hit of its own: the sweep of a sword ({@code Player.attack} hurts each swept
     * entity with the same source, which 1.7.10 did not have) answers once for each of them, with a heal of Vampirism, a roll
     * of Final Strike and a step of the strike counter for each.</p>
     */
    static float onEntityDamaged(LivingEntity victim, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof LivingEntity attacker)) return amount;
        ItemStack heldItem = attacker.getMainHandItem();
        if (heldItem.isEmpty()) return amount;

        int pounce = 0;
        boolean pounceInAir = false;
        if (attacker instanceof Player player) {
            pounce = ModEnchantments.level(player.getItemBySlot(EquipmentSlot.LEGS), ModEnchantments.pounce);
            // The block below the feet; the server's posY of 1.7.10 was the height of the feet.
            pounceInAir = pounce > 0 && player.level().getBlockState(
                    BlockPos.containing(player.getX(), Math.floor(player.getY()) - 1, player.getZ())).isAir();
        }

        int finalStrike = ModEnchantments.level(heldItem, ModEnchantments.finalStrike);
        int roll = finalStrike > 0 ? attacker.getRandom().nextInt(19) : 0;
        int valiance = ModEnchantments.level(heldItem, ModEnchantments.valiance);
        int focusedStrikes = ModEnchantments.level(heldItem, ModEnchantments.focusedStrike);
        int dispersedStrikes = ModEnchantments.level(heldItem, ModEnchantments.dispersedStrikes);

        int successiveStrikes = 0;
        if (focusedStrikes > 0 || dispersedStrikes > 0) {
            UUID lastTarget = new UUID(ItemNBTHelper.getLong(heldItem, NBT_LAST_TARGET, 0L),
                    ItemNBTHelper.getLong(heldItem, NBT_LAST_TARGET_2, 0L));
            UUID entityId = victim.getUUID();
            successiveStrikes = EnchantmentFormulas.nextSuccessiveStrikes(lastTarget, ItemNBTHelper.getInt(heldItem,
                    NBT_SUCCESSIVE_STRIKE, 0), entityId);
            if (!lastTarget.equals(entityId)) {
                ItemNBTHelper.setLong(heldItem, NBT_LAST_TARGET, entityId.getMostSignificantBits());
                ItemNBTHelper.setLong(heldItem, NBT_LAST_TARGET_2, entityId.getLeastSignificantBits());
            }
            ItemNBTHelper.setInt(heldItem, NBT_SUCCESSIVE_STRIKE, successiveStrikes);
        }

        float result = EnchantmentFormulas.modifyDamage(amount, new EnchantmentFormulas.Attack(pounce, pounceInAir, finalStrike,
                roll, valiance, attacker.getHealth(), attacker.getMaxHealth(), focusedStrikes, dispersedStrikes,
                successiveStrikes));

        int vampirism = ModEnchantments.level(heldItem, ModEnchantments.vampirism);
        if (vampirism > 0) {
            attacker.heal(EnchantmentFormulas.vampirismHeal(vampirism));
            playZap(victim);
        }
        return result;
    }

    private static void playZap(LivingEntity victim) {
        try {
            SoundEvent zap = ThaumcraftSoundIds.event(ThaumcraftSoundIds.ZAP);
            victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), zap, SoundSource.PLAYERS, 0.6F, 1F);
        } catch (IllegalArgumentException e) {
            LOG.debug("The zap sound of Thaumcraft is not registered, Vampirism is silent", e);
        }
    }

    // ---- digging: Shatter, Tunnel, Disintegrate, Flaming Touch ---------------------------------------------------

    /**
     * {@code onGetHarvestSpeed} ({@code PlayerEvent.BreakSpeed}, highest priority). Posted on both sides by
     * {@code BlockState.getDestroyProgress}, for every player that asks it, a never ticked fake player included; the tool is
     * the main hand's, the hardness that of the block, the Tunnel direction the one on the tool and the player's yaw, so both
     * sides come to the same speed. A speed of zero is the cancelled event of the original: the player cannot dig the block.
     */
    static float onGetHarvestSpeed(Player player, BlockState state, @Nullable BlockPos pos, float speed) {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) return speed;

        int shatter = ModEnchantments.level(heldItem, ModEnchantments.shatter);
        int tunnel = ModEnchantments.level(heldItem, ModEnchantments.tunnel);
        int desintegrate = ModEnchantments.level(heldItem, ModEnchantments.desintegrate);
        int autoSmelt = ModEnchantments.level(heldItem, ModEnchantments.autoSmelt);
        if (shatter <= 0 && tunnel <= 0 && desintegrate <= 0 && autoSmelt <= 0) return speed;

        // The original asked the hardness at (0, 0, 0) for Shatter and read the field for the others: both are the hardness
        // of the block, which the poster gives with the position (BlockPos.ZERO if a poster does not know it).
        float hardness = state.getDestroySpeed(player.level(), pos != null ? pos : BlockPos.ZERO);
        boolean hasDirection = ItemNBTHelper.verifyExistance(heldItem, NBT_TUNNEL_DIRECTION);
        float angleDifference = 0F;
        if (hasDirection) {
            float oldDirection = ItemNBTHelper.getFloat(heldItem, NBT_TUNNEL_DIRECTION, 0F);
            angleDifference = Math.abs(Mth.wrapDegrees(oldDirection - player.getYRot()));
        }
        return EnchantmentFormulas.breakSpeed(speed, shatter, tunnel, desintegrate, autoSmelt, hardness, isWooden(state),
                hasDirection, angleDifference);
    }

    /**
     * {@code onBreakBlock} ({@code BlockEvent.BreakEvent}): Tunnel remembers the way the player looked at the block. And the
     * wear of Disintegrate and Flaming Touch: 1.7.10 called {@code damageItem(1)} every time the speed event fired for a block
     * that was theirs (every tick of the dig, the speed being {@code Float.MAX_VALUE}, so once or twice per block); the speed
     * event here is also asked by the client and by everything that wants a dig time, so the wear is taken once, when the
     * block is broken. A block that is not theirs cannot be dug at all (the speed is zero), so it does not get here.
     */
    static EventResult onBreakBlock(Level level, BlockPos pos, BlockState state, ServerPlayer player, IntValue xp) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return EventResult.pass();

        if (ModEnchantments.level(held, ModEnchantments.tunnel) > 0) {
            ItemNBTHelper.setFloat(held, NBT_TUNNEL_DIRECTION, player.getYRot());
        }

        int desintegrate = ModEnchantments.level(held, ModEnchantments.desintegrate);
        int autoSmelt = ModEnchantments.level(held, ModEnchantments.autoSmelt);
        if (EnchantmentFormulas.desintegrateApplies(desintegrate, state.getDestroySpeed(level, pos))
                || EnchantmentFormulas.autoSmeltApplies(autoSmelt, isWooden(state))) {
            held.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        return EventResult.pass();
    }

    /**
     * {@code block.getMaterial() == Material.wood}. 1.21.1 has no materials; the wooden blocks of 1.7.10 (logs, planks,
     * fences, doors, chests, the crafting table, the bookshelf, the jukebox, signs...) are the blocks with a wooden step sound
     * and the blocks of the vanilla wood tags.
     */
    public static boolean isWooden(BlockState state) {
        SoundType sound = state.getSoundType();
        return sound == SoundType.WOOD || sound == SoundType.NETHER_WOOD || sound == SoundType.CHERRY_WOOD
                || sound == SoundType.BAMBOO_WOOD || sound == SoundType.STEM || state.is(BlockTags.LOGS)
                || state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_TRAPDOORS) || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.ALL_SIGNS);
    }

    // ---- movement: Ascent Boost, Slow Fall, Shockwave ---------------------------------------------------------------

    /**
     * {@code onPlayerJump} ({@code LivingJumpEvent}): Ascent Boost multiplies the vertical motion of the jump by
     * {@code (boost + 2) / 2}, with the highest level on the four armor slots, unless the player sneaks. Posted on both
     * sides, after the motion of the jump is set, and the same rule on both.
     */
    static void onPlayerJump(LivingEntity entity) {
        if (!(entity instanceof Player player)) return;
        int boost = ModEnchantments.maxArmorLevel(player, ModEnchantments.ascentBoost);
        if (boost >= 1 && !player.isShiftKeyDown()) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, motion.y * EnchantmentFormulas.ascentBoostFactor(boost), motion.z);
        }
    }

    /**
     * Slow Fall, the fall distance of the landing (see {@link EnchantmentFormulas#slowFallLanding}): the highest level on the
     * four armor slots, not while sneaking. {@code LIVING_FALL} is posted on both sides, and the rule is the same on both: the
     * client plays the thud of the fall from its own count, the server decides the damage.
     */
    static EventResult onSlowFallLanding(LivingEntity entity, TinkererEvents.FallData fall) {
        if (!(entity instanceof Player player)) return EventResult.pass();
        int slowFall = ModEnchantments.maxArmorLevel(player, ModEnchantments.slowFall);
        if (slowFall > 0) {
            fall.setDistance(EnchantmentFormulas.slowFallLanding(slowFall, player.isShiftKeyDown(), fall.getDistance()));
        }
        return EventResult.pass();
    }

    /**
     * {@code onFall} ({@code LivingFallEvent}, low priority in 1.7.10; here registered after Slow Fall's listener and before
     * the listeners that other groups register later, see {@link #register()}): Shockwave, with the boots of the faller. Every other living
     * entity within ten blocks takes {@code .1 * shockwave * distance} of fall damage when the fall is more than three blocks. The
     * event is posted on both sides; the blast is an effect on the world and happens on the server only. (A player that can
     * fly does not get here: {@code Player.causeFallDamage} refuses it before the event, as {@code EntityPlayer.fall} did.)
     */
    static EventResult onFall(LivingEntity entity, TinkererEvents.FallData fall) {
        if (!(entity instanceof Player player) || player.level().isClientSide) return EventResult.pass();
        int shockwave = ModEnchantments.level(player.getItemBySlot(EquipmentSlot.FEET), ModEnchantments.shockwave);
        if (EnchantmentFormulas.shockwaveActs(shockwave, fall.getDistance())) shockwave(player, shockwave, fall.getDistance());
        return EventResult.pass();
    }

    /** Every creature within ten blocks takes {@code .1 * shockwave * distance} of fall damage; the faller does not. */
    static void shockwave(Player faller, int level, float distance) {
        Level world = faller.level();
        AABB area = new AABB(faller.getX() - EnchantmentFormulas.SHOCKWAVE_RADIUS, faller.getY() - EnchantmentFormulas.SHOCKWAVE_RADIUS,
                faller.getZ() - EnchantmentFormulas.SHOCKWAVE_RADIUS, faller.getX() + EnchantmentFormulas.SHOCKWAVE_RADIUS,
                faller.getY() + EnchantmentFormulas.SHOCKWAVE_RADIUS, faller.getZ() + EnchantmentFormulas.SHOCKWAVE_RADIUS);
        float damage = EnchantmentFormulas.shockwaveDamage(level, distance);
        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target != faller) target.hurt(world.damageSources().fall(), damage);
        }
    }

    /**
     * The part of {@code onEntityUpdate} ({@code LivingUpdateEvent}) of Slow Fall that is a motion: on the client that simulates
     * the fall of its own player, the vertical motion is divided by {@code 1 + .33 * level} and the cloud is spawned, while the
     * player falls fast enough, for 2.9 blocks or more and does not sneak. (The server has no motion to look at, it counts the
     * fall from the reports of the client; the fall distance is the landing's, {@link #onSlowFallLanding}.)
     */
    static void onEntityUpdate(Player player) {
        if (!player.level().isClientSide || !player.isLocalPlayer()) return;
        int slowFall = ModEnchantments.maxArmorLevel(player, ModEnchantments.slowFall);
        if (slowFall <= 0) return;

        Vec3 motion = player.getDeltaMovement();
        if (EnchantmentFormulas.slowFallActs(slowFall, player.isShiftKeyDown(), motion.y, player.fallDistance)) {
            player.setDeltaMovement(motion.x, EnchantmentFormulas.slowFallMotion(motion.y, slowFall), motion.z);

            // 1.7.10 spawned the cloud at posY - 1, and the posY of a player was the height of its eyes.
            Vec3 now = player.getDeltaMovement();
            player.level().addParticle(ParticleTypes.CLOUD, player.getX() + 0.25, player.getY() + 0.62, player.getZ() + 0.25,
                    -now.x, now.y, -now.z);
        }
    }

    // ---- Quick Draw -------------------------------------------------------------------------------------------------

    /**
     * Quick Draw, the part of {@code onEntityUpdate} that wrote {@code itemInUseCount}: while a player draws a bow with the
     * enchantment, ticks are taken off the count (see {@link EnchantmentFormulas#quickDrawRemaining}, which also says where in
     * the tick the original did it). The count is the entity's own, so the draw clock the bow pose, the HUD and the power of
     * the arrow read ({@code getTicksUsingItem}, {@code releaseUsing}) follows. Posted on both sides; it only reads the enchantments
     * of the stack in use, which both sides have. The level is that of the bow in use (in 1.7.10 the item in the hand, which was
     * the item in use).
     */
    static int onUseItemTick(LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof Player) || !(stack.getItem() instanceof BowItem)) return remaining;
        int quickDraw = ModEnchantments.level(stack, ModEnchantments.quickDraw);
        if (quickDraw <= 0) return remaining;
        return EnchantmentFormulas.quickDrawRemaining(quickDraw, remaining);
    }
}
