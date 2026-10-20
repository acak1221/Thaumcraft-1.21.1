package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The resources (original ItemResource). One item with nineteen metadata values: the alumentum, the nitor, the thaumium ingot, the
 * quicksilver, the magic tallow, the zombie brain, the amber, the enchanted fabric, the vis filter, the knowledge fragment, the mirrored
 * glass, the tainted goo, the taint tendril, the jar label, the salis mundus, the primal charm, the void metal ingot, the void seed and
 * the gold coin.
 *
 * <p>The metadata is carried by the item itself in 1.21.1, so every resource of the original is a separate item here. The tainted goo and
 * the taint tendril infect their carrier: once in four thousand three hundred and twenty one ticks the carrier is poisoned with taint for
 * two seconds and, when the carrier is a player, the tainted resource is used up.
 */
public final class ItemResource extends Item {
    private final int type;

    public ItemResource(Properties properties, int type) {
        super(properties.stacksTo(64));
        this.type = type;
    }

    /** The original metadata of this resource. */
    public int type() { return type; }

    /** Whether this resource is one of the two tainted ones that poison their carrier. */
    public boolean isTainted() { return EquipmentRules.resourceIsTainted(type); }

    /** Original ItemResource.onUpdate: the taint of a tainted resource that is carried around. */
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !isTainted() || entity == null || !(entity instanceof LivingEntity living)) return;
        if (!living.isAlive() || living.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get()))) return;
        if (!EquipmentRules.taintItemInfects(level.random.nextInt(EquipmentRules.TAINT_ITEM_CHANCE), stack.getMaxDamage())) return;
        if (level instanceof ServerLevel server) {
            living.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get()), EquipmentRules.TAINT_ITEM_POISON_TICKS, 0, false, true));
        }
        if (entity instanceof Player player) {
            String name = BuiltInRegistries.ITEM.getKey(this).getPath();
            player.displayClientMessage(Component.translatable("tc.taint_item_poison", Component.translatable("item.thaumcraft." + name)
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC)), false);
            consumeFromInventory(player);
        }
    }

    /** Original InventoryUtils.consumeInventoryItem: one tainted resource is used up. */
    private void consumeFromInventory(Player player) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack held = player.getInventory().items.get(slot);
            if (held.getItem() != this) continue;
            held.shrink(1);
            return;
        }
    }
}
