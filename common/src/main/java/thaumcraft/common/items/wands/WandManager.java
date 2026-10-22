package thaumcraft.common.items.wands;

import java.util.LinkedHashMap;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.lib.enchantment.EquipmentEnchantmentRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.entities.EntitySpecialItem;

/** Original bookshelf trigger and inventory raw-vis payment; other wand structures have their own native paths. */
public final class WandManager {
    private WandManager() {}

    /**
     * Original {@code WandManager:459-478} - the Thaumonomicon of the original is made from a
     * bookshelf by a wand WITHOUT an installed focus: {@code if (wand.getFocus(itemstack) != null)
     * return false;}. A wand with a focus leaves the bookshelf untouched.
     */
    public static boolean mayCreateThaumonomicon(boolean hasFocus) { return !hasFocus; }

    public static boolean canCreateThaumonomicon(UseOnContext context) {
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        return player != null && !player.isSpectator()
                && context.getItemInHand().is(ThaumcraftContent.WAND.get())
                && mayCreateThaumonomicon(!thaumcraft.common.items.wands.foci.FocusData.get(context.getItemInHand()).isEmpty())
                && context.getLevel().getBlockState(pos).is(Blocks.BOOKSHELF)
                && context.getLevel().mayInteract(player, pos)
                && player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand());
    }

    public static boolean createThaumonomicon(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || !canCreateThaumonomicon(context)) return false;
        BlockPos pos = context.getClickedPos();
        EntitySpecialItem result = ThaumcraftContent.SPECIAL_ITEM.get().create(level);
        if (result == null) return false;
        result.setPos(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5);
        result.setItem(new ItemStack(ThaumcraftContent.THAUMONOMICON.get()));
        result.setDeltaMovement(Vec3.ZERO);
        // Original trigger consumes one bookshelf, with no vis or wand cost.
        if (!level.removeBlock(pos, false)) return false;
        if (!level.addFreshEntity(result)) {
            level.setBlockAndUpdate(pos, Blocks.BOOKSHELF.defaultBlockState());
            return false;
        }
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 24, 0.3, 0.3, 0.3, 0.05);
        // Original WandManager:420 - the bookshelf flashes with the original random
        // grey of the PacketFXBlockSparkle.
        thaumcraft.common.lib.network.BlockSparkleFxNetworking.sendBlockSparkle(level, pos, -9999);
        level.playSound(null, pos, SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand")), SoundSource.PLAYERS, 1, 1);
        return true;
    }
    /** Original WandManager104–120: raw centivis, one full payer, main slots35→0. */
    public static boolean consumeVisFromInventory(Player player, AspectList rawCost) {
        if (player == null || player.level().isClientSide || rawCost == null || rawCost.size() == 0) return false;
        // Original amulet slots0..3 are independent full RAW payers, with no wand cap/gear discount.
        var accessories = thaumcraft.common.lib.baubles.PlayerAccessories.get(player);
        for (int slot=0;slot<4;slot++) {
            ItemStack stack=accessories.getItem(slot);
            if (stack.getItem() instanceof thaumcraft.common.items.baubles.ItemAmuletVis amulet && amulet.consumeAllVis(stack,rawCost,true)) {
                accessories.setChanged(); return true;
            }
        }
        // Each wand is tested independently, last main-inventory slot first. Costs never split across wands.
        for (int slot = player.getInventory().items.size() - 1; slot >= 0; slot--) {
            ItemStack wand = player.getInventory().items.get(slot);
            if (!ArcaneVis.isCraftingWand(wand)) continue;
            var current = ItemWandCasting.data(wand);
            var prices = new LinkedHashMap<String, Integer>();
            boolean canPay = true;
            for (Aspect aspect : rawCost.getAspects()) {
                int amount = EquipmentEnchantmentRules.repairPrice(rawCost.getAmount(aspect), WandVisDiscount.modifier(wand, player, aspect));
                if (current.amount(aspect.getTag()) < amount) { canPay = false; break; }
                prices.put(aspect.getTag(), amount);
            }
            if (!canPay) continue;
            for (var entry : prices.entrySet()) current = current.withAmount(entry.getKey(), current.amount(entry.getKey()) - entry.getValue());
            wand.set(ThaumcraftContent.WAND_DATA.get(), current);
            player.getInventory().setChanged();
            return true;
        }
        return false;
    }
}
