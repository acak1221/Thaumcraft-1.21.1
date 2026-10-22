package thaumcraft.common.items.equipment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import thaumcraft.api.IRepairable;

/**
 * Pickaxe of the Core (original ItemElementalPickaxe, Api THAUMIUM_ELEMENTAL: tier 3, 1500 uses, speed 10, damage bonus 3, enchantability
 * 18, repaired with the Thaumium ingot). What it hits burns for two seconds (a player target only with player-versus-player on), and the
 * stone it mines may give a native cluster (see {@link thaumcraft.common.lib.utils.SpecialMining}). A right click costs five uses and
 * plays the failing-wand sound; on the client it also starts the original ore scan (a see-through overlay of every ore within eight
 * blocks for five seconds, {@code thaumcraft.client.lib.ScanOverlay}) and swings the arm.
 */
public class ItemElementalPickaxe extends PickaxeItem implements IRepairable {
    /** Seconds of fire for a hit, original {@code setFire(2)}. */
    public static final int FIRE_SECONDS = 2;
    /** Uses taken by a right click, original {@code damageItem(5)}. */
    public static final int CLICK_WEAR = 5;
    private static final SoundEvent FAIL = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wandfail"));

    public ItemElementalPickaxe(Tier tier, Properties properties) {
        // Legacy pickaxe 2 + material 3 = additive 5; the attack speed is a 1.21 combat adapter, as for the Thaumium pickaxe.
        super(tier, properties.rarity(Rarity.RARE).attributes(DiggerItem.createAttributes(tier, 2, -2.8F)));
    }

    /**
     * Original {@code onLeftClickEntity}, which ran before the blow. Declared adaptation, as for the Void tools: here the fire follows a
     * landed hit, since 1.21.1 offers no hook before one.
     */
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.level() instanceof ServerLevel server && !(target instanceof Player && !server.getServer().isPvpAllowed())) {
            target.igniteForSeconds(FIRE_SECONDS);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null) context.getItemInHand().hurtAndBreak(CLICK_WEAR, player, EquipmentSlot.MAINHAND);
        if (context.getLevel() instanceof ServerLevel level) {
            var pos = context.getClickedPos();
            level.playSound(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, FAIL, SoundSource.PLAYERS, 0.2F, 0.2F + level.random.nextFloat() * 0.2F);
        }
        // Oracle ItemElementalPickaxe.java:89-92: the client half starts the scan with the
        // deadline now + 5000 and swings the arm. The client class is referenced only in this
        // client-only branch, like the animateTick effects of the blocks.
        if (player != null && context.getLevel().isClientSide) {
            thaumcraft.client.lib.ScanOverlay.start(player, context.getClickedPos(),
                    System.currentTimeMillis() + thaumcraft.client.lib.ScanOverlay.DURATION_MILLIS);
            player.swing(context.getHand());
        }
        return InteractionResult.PASS;
    }
}
