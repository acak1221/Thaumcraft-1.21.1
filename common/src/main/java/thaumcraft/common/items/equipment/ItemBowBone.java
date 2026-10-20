package thaumcraft.common.items.equipment;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import thaumcraft.api.IRepairable;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The Bone Bow (original ItemBowBone). Five hundred and twelve uses, an enchantability of three, a stack of one.
 *
 * <p>The original is a full copy of the vanilla bow with three differences: it draws twice as fast (the charge time is divided by ten
 * instead of twenty, so it reaches full power in ten ticks), its arrow leaves at two and a half blocks per tick instead of three and
 * carries half a point more damage, and it lets go of the player's hand after eighteen ticks of drawing, so a held bow always fires at
 * full power. It never makes a critical arrow.
 */
public class ItemBowBone extends BowItem implements IRepairable {
    public ItemBowBone(Properties properties) {
        super(properties.stacksTo(EquipmentRules.BONE_BOW_MAX_STACK_SIZE).durability(EquipmentRules.BONE_BOW_DURABILITY));
    }

    /** Original ItemBowBone.func_77619_b: an enchantability of three. */
    @Override public int getEnchantmentValue() { return EquipmentRules.BONE_BOW_ENCHANTABILITY; }

    /** Original ItemBowBone.onUsingTick: the bow lets go of the hand after eighteen ticks of drawing. */
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof Player player)) return;
        // The original stopActiveHand released the bow, so the shot is fired: releaseUsingItem is the 1.21.1 form of it.
        if (EquipmentRules.boneBowAutoRelease(getUseDuration(stack, entity) - remaining)) player.releaseUsingItem();
    }

    /**
     * Original ItemBowBone.func_77659_a: the bow may be drawn when the player carries an arrow or when the bow carries Infinity, even in
     * creative mode.
     */
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean hasArrow = !player.getProjectile(stack).isEmpty();
        if (hasArrow || player.hasInfiniteMaterials() || enchantmentLevel(level, Enchantments.INFINITY, stack) > 0) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    /**
     * Original ItemBowBone.func_77615_a: the shot itself. The draw time is divided by ten, the arrow leaves at two and a half blocks per
     * tick and carries two and a half points of damage, the bow loses one use and the arrow is never critical.
     */
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!(entity instanceof Player player)) return;
        int charge = getUseDuration(stack, entity) - remaining;
        float power = EquipmentRules.boneBowPower(charge);
        if (!EquipmentRules.boneBowFires(charge)) return;
        ItemStack ammo = player.getProjectile(stack);
        List<ItemStack> projectiles;
        if (ammo.isEmpty()) {
            // Original ItemBowBone.func_77615_a: with Infinity the arrow is fired even when no arrow is carried.
            if (enchantmentLevel(level, Enchantments.INFINITY, stack) <= 0) return;
            projectiles = List.of(new ItemStack(Items.ARROW));
        } else {
            projectiles = draw(stack, ammo, player);
            if (projectiles.isEmpty()) return;
        }
        if (level instanceof ServerLevel server) {
            shoot(server, player, player.getUsedItemHand(), stack, projectiles,
                    EquipmentRules.boneBowArrowSpeed(power), 1.0F, false, null);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
                1.0F / (level.random.nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    /** The original arrow carries two and a half points of damage, and half a point more when the bow carries Power. */
    @Override protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean consumeAmmo) {
        Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, consumeAmmo);
        if (projectile instanceof AbstractArrow arrow) {
            arrow.setBaseDamage(EquipmentRules.boneBowArrowBaseDamage(enchantmentLevel(level, Enchantments.POWER, weapon)));
        }
        return projectile;
    }

    private static int enchantmentLevel(Level level, ResourceKey<Enchantment> key, ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), stack);
    }
}
