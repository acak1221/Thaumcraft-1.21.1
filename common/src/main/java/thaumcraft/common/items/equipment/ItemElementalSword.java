package thaumcraft.common.items.equipment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.IRepairable;
import thaumcraft.common.mixin.ServerGamePacketListenerFloatAccess;

/**
 * Sword of the Zephyr (original ItemElementalSword, Api THAUMIUM_ELEMENTAL: tier 3, 1500 uses, speed 10, damage 4 + 3, enchantability 18,
 * repaired with the thaumium ingot). Held on the use button it lifts its user, slows a fall, keeps the server from calling the hover
 * flying, blows everything within 2.5 blocks away, plays the wind and wears one use every twentieth tick, the first included (see
 * {@link ElementalSwordRules}). A blow also strikes the mobs round its target ({@link thaumcraft.common.lib.world.ElementalSwordSweep}).
 * The original's client side smoke spirals of the held sword are ported ({@link ClientEffects}); the original's use animation was the sword
 * block: in 1.21.1 that would be a shield turning every frontal blow away, so the bow pose stands in.
 */
public class ItemElementalSword extends SwordItem implements IRepairable {
    private static final SoundEvent WIND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wind"));
    private static ClientEffects clientEffects;

    /**
     * Original {@code ItemElementalSword.onUsingTick} (oracle
     * {@code thaumcraft/common/items/equipment/ItemElementalSword.java:128-141}): the client side
     * smoke of the held sword, five spirals per tick and, while the user sneaks, one vanilla
     * smoke particle that drifts away from the user.
     */
    public interface ClientEffects {
        /** The five smoke spirals of one tick of the hold. */
        void smokeSpiral(Player player, int floor, int count);
    }

    /** The client bootstrap owns the client effects; the server never sees them. */
    public static void setClientEffects(ClientEffects value) { clientEffects = value; }

    public ItemElementalSword(Tier tier, Properties properties) {
        // Legacy sword 4 + material 3 = additive 7; the attack speed is a 1.21 combat adapter, as for the Thaumium sword.
        super(tier, properties.rarity(Rarity.RARE).attributes(SwordItem.createAttributes(tier, 4, -2.4F)));
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return ElementalSwordRules.USE_DURATION; }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** Runs on the client and on the server, as the original did: the lift is the user's own movement, the rest the server's. */
    @Override public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(user instanceof Player player)) return;
        int ticks = getUseDuration(stack, user) - remaining;
        Vec3 motion = player.getDeltaMovement();
        player.fallDistance = ElementalSwordRules.fall(player.fallDistance, motion.y);
        player.setDeltaMovement(motion.x, ElementalSwordRules.rise(motion.y), motion.z);
        // Original ItemElementalSword.onUsingTick:128-141, the client side smoke of the hold.
        if (level.isClientSide && clientEffects != null) {
            int floor = (int) (player.getY() - 2.0);
            boolean sneaking = player.isShiftKeyDown();
            if (sneaking) floor = (int) Math.floor(player.getY());
            clientEffects.smokeSpiral(player, floor, 5);
            if (sneaking) {
                float angle = level.random.nextFloat() * 360F;
                double mx = -Math.sin(angle / 180F * Math.PI) / 5F;
                double mz = Math.cos(angle / 180F * Math.PI) / 5F;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        player.getX(), player.getY() + .1, player.getZ(), mx, 0.0, mz);
            }
        }
        if (!(level instanceof ServerLevel server)) return;
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            ((ServerGamePacketListenerFloatAccess) serverPlayer.connection).thaumcraft$setAboveGroundTickCount(0);
        }
        for (Entity other : server.getEntities(player, player.getBoundingBox().inflate(ElementalSwordRules.REACH))) {
            if (other instanceof Player || !other.isAlive() || player.getVehicle() == other) continue;
            other.setDeltaMovement(other.getDeltaMovement().add(ElementalSwordRules.push(player.position(), other.position())));
        }
        if (ticks % ElementalSwordRules.PERIOD == 0) {
            server.playSound(null, player.getX(), player.getY(), player.getZ(), WIND, SoundSource.PLAYERS, 0.5F, 0.9F + server.random.nextFloat() * 0.2F);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        }
    }
}
