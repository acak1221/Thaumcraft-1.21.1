package thaumcraft.common.items.golems;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityTravelingTrunk;

/**
 * Original ItemTrunkSpawner: sets the trunk down next to the clicked face with a random facing, the user as owner, the anvil name, and
 * the {@code upgrade} and contents the item carries (flattened from NBT to data components). Not consumed in creative.
 */
public final class ItemTrunkSpawner extends Item {
    public ItemTrunkSpawner(Properties properties) { super(properties); }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        super.appendHoverText(stack, context, tooltip, flags);
        Integer upgrade = stack.get(ThaumcraftGolems.TRUNK_UPGRADE.get());
        if (upgrade != null && upgrade > TrunkRules.NO_UPGRADE && upgrade <= TrunkRules.ENTROPY) {
            for (GolemUpgrade kind : GolemUpgrade.values()) if (kind.id == upgrade) {
                tooltip.add(Component.translatable(ThaumcraftGolems.upgradeItem(kind).getDescriptionId()).withStyle(ChatFormatting.BLUE));
            }
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null && contents.nonEmptyItems().iterator().hasNext()) tooltip.add(Component.translatable("item.TrunkSpawner.text.1"));
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos at = clicked.relative(face);
        // Original: a fence post top lifts the trunk half a block (render type 11).
        double lift = face == Direction.UP && level.getBlockState(clicked).getBlock() instanceof FenceBlock ? 0.5 : 0.0;
        EntityTravelingTrunk trunk = ThaumcraftGolems.TRUNK.get().create(level);
        if (trunk == null) return InteractionResult.FAIL;
        float yaw = Math.floorMod((int) (level.random.nextFloat() * 360F), 360) - 180F;
        trunk.moveTo(at.getX() + 0.5, at.getY() + lift, at.getZ() + 0.5, yaw, 0.0F);
        trunk.setYHeadRot(yaw);
        trunk.setYBodyRot(yaw);
        trunk.setOwner(player);
        if (stack.has(DataComponents.CUSTOM_NAME)) trunk.setCustomName(stack.getHoverName());
        Integer upgrade = stack.get(ThaumcraftGolems.TRUNK_UPGRADE.get());
        if (upgrade != null) trunk.setUpgrade(upgrade);
        trunk.setInvSize();
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            int slot = 0;
            for (ItemStack kept : contents.nonEmptyItems()) {
                if (slot >= TrunkRules.MAX_SLOTS) break;
                trunk.inventory.setItem(slot++, kept.copy());
            }
        }
        if (!level.addFreshEntity(trunk)) return InteractionResult.FAIL;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }
}
