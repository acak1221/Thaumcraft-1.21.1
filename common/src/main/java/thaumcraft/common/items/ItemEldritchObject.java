package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.common.blocks.BlockEldritch;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.items.EquipmentRules;

import java.util.List;

/**
 * The eldritch objects (original ItemEldritchObject) that are not ported elsewhere: the eldritch eye, the runed tablet and the eldritch
 * obelisk placer. The crimson rites and the primordial pearl of the original are two separate items in this port.
 *
 * <p>The eye watches, the tablet is part of something larger and the placer puts up the obelisk of the eldritch, which is a creative tool
 * of the original. The obelisk is the native eldritch block of this port, so the placer puts up the altar, the obelisk and the middle of
 * the obelisk of the original.
 */
public final class ItemEldritchObject extends Item {
    private final int type;

    public ItemEldritchObject(Properties properties, int type) {
        // Oracle ItemEldritchObject:56 sets the stack size of ONE for every metadata of the
        // original item, so the runed tablet does not stack either.
        super(properties.stacksTo(1).rarity(rarityOf(type)));
        this.type = type;
    }

    /** Original ItemEldritchObject.func_77613_e: the tablet is rare, the primordial pearl is epic, the rest is uncommon. */
    private static Rarity rarityOf(int type) {
        return switch (type) {
            case EquipmentRules.ELDRITCH_TABLET -> Rarity.RARE;
            case EquipmentRules.ELDRITCH_NODE_STABILIZER -> Rarity.EPIC;
            default -> Rarity.UNCOMMON;
        };
    }

    /** The original metadata of this object. */
    public int type() { return type; }

    /** Original ItemEldritchObject.func_77624_a: what the object says about itself. */
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        switch (type) {
            case EquipmentRules.ELDRITCH_EYE -> tooltip.add(Component.translatable("item.ItemEldritchObject.text.1").withStyle(ChatFormatting.DARK_PURPLE));
            case EquipmentRules.ELDRITCH_CRIMSON_RITES -> {
                tooltip.add(Component.translatable("item.ItemEldritchObject.text.2").withStyle(ChatFormatting.DARK_PURPLE));
                tooltip.add(Component.translatable("item.ItemEldritchObject.text.3").withStyle(ChatFormatting.DARK_BLUE));
            }
            case EquipmentRules.ELDRITCH_TABLET -> tooltip.add(Component.translatable("item.ItemEldritchObject.text.4").withStyle(ChatFormatting.DARK_PURPLE));
            case EquipmentRules.ELDRITCH_NODE_STABILIZER -> {
                tooltip.add(Component.translatable("item.ItemEldritchObject.text.5").withStyle(ChatFormatting.DARK_PURPLE));
                tooltip.add(Component.translatable("item.ItemEldritchObject.text.6").withStyle(ChatFormatting.DARK_PURPLE));
            }
            case EquipmentRules.ELDRITCH_OBELISK_PLACER -> tooltip.add(Component.literal("Creative Mode Only").withStyle(ChatFormatting.ITALIC));
            default -> { }
        }
    }

    /** Original ItemEldritchObject.onItemUseFirst: the obelisk placer puts up the obelisk of the eldritch. */
    @Override public InteractionResult useOn(UseOnContext context) {
        if (type != EquipmentRules.ELDRITCH_OBELISK_PLACER) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        // Original ItemEldritchObject.onItemUseFirst: the obelisk is only put up on the top face of a block.
        if (context.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        // Original ItemEldritchObject.onItemUseFirst: the six blocks above the ground have to be free.
        for (int height = 1; height <= EquipmentRules.ELDRITCH_PLACER_FREE_HEIGHT; height++) {
            if (!level.getBlockState(pos.offset(0, height, 0)).isAir()) return InteractionResult.PASS;
        }
        // Original ItemEldritchObject.onItemUseFirst: the placer lets go of the hand of the player.
        player.getCooldowns().removeCooldown(this);
        if (level.isClientSide) return InteractionResult.SUCCESS;
        // Original ItemEldritchObject.onItemUseFirst: the altar, the obelisk and the middle of the obelisk.
        Block eldritch = ThaumcraftEldritchBlocks.ELDRITCH.get();
        for (int height = 1; height <= 7; height++) {
            if (height == 2) continue;
            level.setBlock(pos.offset(0, height, 0), eldritch.defaultBlockState()
                    .setValue(BlockEldritch.VARIANT, EquipmentRules.eldritchPlacerVariant(height)), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }
}
