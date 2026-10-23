package thaumcraft.common.items.equipment;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IWarpingGear;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The Crimson Blade (original ItemCrimsonSword). Its material {@code CVOID} has a harvest level of four, two hundred uses, an efficiency
 * of eight, three and a half points of damage against entities and an enchantability of twenty; the sword adds its own four points, so
 * the blade strikes for seven and a half points plus the wielder's own damage.
 *
 * <p>A hit leaves the target sapped: three seconds of weakness and six seconds of hunger, unless a player hits a player while player
 * versus player is switched off. The blade adds two points of warp to its wielder and mends itself by one point every second, exactly
 * like the void metal tools do. It is repaired with the primal charm, the original quirk the void tools share.
 */
public class ItemCrimsonSword extends SwordItem implements IRepairable, IWarpingGear {
    public ItemCrimsonSword(Tier tier, Properties properties) {
        // Original ItemSword adds four points to the material damage; the helper keeps the original's seven and a half.
        super(tier, properties.rarity(Rarity.RARE).attributes(
                ToolAttributeHelper.withMainHandDamage(SwordItem.createAttributes(tier, 4, -2.4F), EquipmentRules.crimsonSwordDamage())));
    }

    /** Original ItemCrimsonSword.hitEntity: the sap of the blade. */
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean pvp = true;
        if (target.level() instanceof ServerLevel server) pvp = server.getServer().isPvpAllowed();
        if (EquipmentRules.crimsonSwordSaps(target.level().isClientSide, target instanceof Player, attacker instanceof Player, pvp)) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EquipmentRules.CRIMSON_SWORD_SAP_WEAKNESS_TICKS));
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, EquipmentRules.CRIMSON_SWORD_SAP_HUNGER_TICKS));
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /** Original ItemCrimsonSword.onUpdate: the blade mends itself by one point every twenty ticks of its carrier's age. */
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        // Original damageItem(-1) does nothing for creative players, just like void tools.
        thaumcraft.common.items.VoidMetal.repair(stack, entity);
    }

    /** Original ItemCrimsonSword.getWarp: two points of warp. */
    @Override public int getWarp(ItemStack stack, Player player) { return EquipmentRules.CRIMSON_SWORD_WARP; }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("enchantment.special.sapgreat").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
