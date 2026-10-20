package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.common.lib.items.EquipmentRules;

import java.util.List;

/**
 * The eldritch objects (original ItemEldritchObject) that are not ported elsewhere: the eldritch eye, the runed tablet and the eldritch
 * obelisk placer. The crimson rites and the primordial pearl of the original are two separate items in this port.
 *
 * <p>The eye watches, the tablet is part of something larger and the placer puts up the obelisk of the eldritch, which is a creative tool
 * of the original.
 */
public final class ItemEldritchObject extends Item {
    /** The registry names the eldritch block may carry. */
    private static final String[] ELDRITCH_BLOCK_IDS = {"thaumcraft:eldritch", "thaumcraft:block_eldritch"};

    private final int type;

    public ItemEldritchObject(Properties properties, int type) {
        super(properties.stacksTo(type == EquipmentRules.ELDRITCH_OBELISK_PLACER ? 1 : 64).rarity(rarityOf(type)));
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
        if (context.getClickedFace() != net.minecraft.core.Direction.UP) return InteractionResult.PASS;
        Block block = eldritchBlock();
        if (block == null) return InteractionResult.PASS;
        for (int height = 1; height <= 6; height++) {
            if (!level.getBlockState(pos.offset(0, height, 0)).canBeReplaced()) return InteractionResult.PASS;
        }
        player.releaseUsingItem();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        level.setBlock(pos.offset(0, 1, 0), block.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.offset(0, 3, 0), block.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.offset(0, 4, 0), block.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.offset(0, 5, 0), block.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.offset(0, 6, 0), block.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.offset(0, 7, 0), block.defaultBlockState(), Block.UPDATE_ALL);
        return InteractionResult.SUCCESS;
    }

    private static Block eldritchBlock() {
        for (String id : ELDRITCH_BLOCK_IDS) {
            var block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id));
            if (block.isPresent()) return block.get();
        }
        return null;
    }
}
