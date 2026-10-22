package thaumcraft.common.items.equipment;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.IRepairable;
import thaumcraft.common.config.ThaumcraftEquipment;

/**
 * Shovel of the Earthmover (original ItemElementalShovel, Api THAUMIUM_ELEMENTAL: tier 3, 1500 uses, speed 10, damage 1 + 3, enchantability
 * 18, repaired with the thaumium ingot). Mining takes the 3 x 3 square across the face (see {@link ElementalShovelHarvest}). A right click on
 * a block without a block entity copies it into a 3 x 3 square beside the clicked face, a use and one block from the inventory each (a
 * grass block with no grass in the inventory is laid as dirt; creative players pay nothing), over air and anything replaceable. The
 * orientation (key, 0..2) turns the square. The original also showed the squares as a ghost preview and sparkled: not ported.
 */
public class ItemElementalShovel extends ShovelItem implements IRepairable {
    public ItemElementalShovel(Tier tier, Properties properties) {
        // Legacy spade 1 + material 3 = additive 4; the attack speed is a 1.21 combat adapter, as for the Thaumium shovel.
        super(tier, properties.rarity(Rarity.RARE).attributes(DiggerItem.createAttributes(tier, 1, -3F)));
    }

    /** The orientation stored on the stack, 0 when never set. */
    public static int orientation(ItemStack stack) {
        return Math.floorMod(stack.getOrDefault(ThaumcraftEquipment.SHOVEL_ORIENTATION.get(), 0), ElementalShovelGeometry.ORIENTATIONS);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tc.port.shovel.orientation." + orientation(stack)));
    }

    /** The original has no path-flattening branch: a click only lays the square. */
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !(context.getLevel() instanceof ServerLevel level)) return InteractionResult.PASS;
        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);
        if (level.getBlockEntity(clicked) != null) return InteractionResult.PASS;
        ItemStack shovel = context.getItemInHand();
        Direction face = context.getClickedFace();
        boolean acted = false;
        for (BlockPos cell : ElementalShovelGeometry.layout(clicked, face, orientation(shovel), player.getYRot())) {
            if (shovel.isEmpty()) break;
            BlockState there = level.getBlockState(cell);
            if (!there.isAir() && !there.canBeReplaced()) continue;
            if (!level.isInWorldBounds(cell) || !level.mayInteract(player, cell) || !player.mayUseItemAt(cell, face, shovel)) continue;
            BlockState laid = null;
            if (state.getBlock().asItem() != Items.AIR && (player.getAbilities().instabuild || consume(player, state.getBlock().asItem()))) laid = state;
            else if (state.is(Blocks.GRASS_BLOCK) && (player.getAbilities().instabuild || consume(player, Items.DIRT))) laid = Blocks.DIRT.defaultBlockState();
            if (laid == null) continue;
            // Stock has already been paid (or creative placement accepted).
            // Even a rejected world write must not fall through to another use.
            acted = true;
            level.setBlock(cell, laid, 3);
            level.playSound(null, cell, laid.getSoundType().getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 0.9F + level.random.nextFloat() * 0.2F);
            shovel.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            player.swing(context.getHand(), true);
        }
        return acted ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    /** Original {@code InventoryUtils.consumeInventoryItem}: one item from the first matching stack of the main inventory. */
    private static boolean consume(Player player, Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
