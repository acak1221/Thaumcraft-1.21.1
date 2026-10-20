package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.lib.items.EquipmentRules;

import java.util.List;

/**
 * The keys (original ItemKey). An iron key and a gold key link themselves to an arcane door or to an arcane pressure plate; a linked key
 * grants access to that door again, and the gold key may also hand the access on to others.
 *
 * <p>The ownership of a door lives in the TileOwned of the door, which is not part of this port, so the key keeps its link, its messages
 * and its own consumption but cannot yet write into the access list of a door.
 */
public final class ItemKey extends Item {
    /** The link of a key: the position of the door it was linked to and the kind of door. */
    public record KeyLink(String location, int type) {}

    /** The registry names of the arcane door. */
    private static final String[] ARCANE_DOOR_IDS = {"thaumcraft:arcane_door", "thaumcraft:block_arcane_door"};
    /** The registry names of the wooden device that carries the arcane pressure plate. */
    private static final String[] ARCANE_PLATE_IDS = {"thaumcraft:wooden_device", "thaumcraft:block_wooden_device"};
    /** The registry names of the arcane pressure plate as a block of its own. */
    private static final String[] ARCANE_PRESSURE_PLATE_IDS = {"thaumcraft:arcane_pressure_plate", "thaumcraft:block_arcane_pressure_plate"};

    private static final SoundEvent KEY = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "key"));

    private final int keyType;

    public ItemKey(Properties properties, int keyType) {
        super(properties.stacksTo(64).rarity(Rarity.UNCOMMON));
        this.keyType = keyType;
    }

    /** The kind of key: zero the iron key, one the gold key. */
    public int keyType() { return keyType; }

    /** The link of the stack, or null when the key was never linked. */
    public static KeyLink link(ItemStack stack) { return stack.get(ThaumcraftLegacyItems.KEY_LINK.get()); }

    /** Original ItemKey.onItemUseFirst: the key links itself to a door, or grants access to the door it is linked to. */
    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        int target = targetType(level, pos);
        if (target < 0) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        KeyLink link = link(stack);
        if (link == null) {
            if (!level.isClientSide) {
                ItemStack linked = new ItemStack(stack.getItem(), 1);
                linked.set(ThaumcraftLegacyItems.KEY_LINK.get(), new KeyLink(EquipmentRules.keyLocation(pos.getX(), pos.getY(), pos.getZ()), target));
                if (!player.getInventory().add(linked)) player.drop(linked, false);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                player.displayClientMessage(Component.translatable(target == 0 ? "tc.key1" : "tc.key2")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, KEY, SoundSource.BLOCKS, 1.0F, 0.9F);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide) {
            if (!link.location().equals(EquipmentRules.keyLocation(pos.getX(), pos.getY(), pos.getZ()))) {
                player.displayClientMessage(Component.translatable("tc.key7").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
                return InteractionResult.SUCCESS;
            }
            player.displayClientMessage(Component.translatable(target == 0 ? "tc.key3" : "tc.key5")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
            if (keyType == EquipmentRules.KEY_GOLD) {
                player.displayClientMessage(Component.translatable(target == 0 ? "tc.key4" : "tc.key6")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
            }
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, KEY, SoundSource.BLOCKS, 1.0F, 1.1F);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        KeyLink link = link(stack);
        if (link == null) {
            super.appendHoverText(stack, context, tooltip, flag);
            return;
        }
        tooltip.add(Component.translatable("tc.key9").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(link.type() == 0 ? "tc.key10" : "tc.key11").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        String[] parts = link.location().split(",");
        String where = parts.length == 3 ? "x " + parts[0] + ", z " + parts[2] + ", y " + parts[1] : link.location();
        tooltip.add(Component.literal(where).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** The kind of door at the position: zero the arcane door, one the arcane pressure plate, minus one for anything else. */
    private static int targetType(Level level, BlockPos pos) {
        String id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
        for (String door : ARCANE_DOOR_IDS) if (door.equals(id)) return 0;
        for (String plate : ARCANE_PRESSURE_PLATE_IDS) if (plate.equals(id)) return 1;
        for (String device : ARCANE_PLATE_IDS) if (device.equals(id)) return 1;
        return -1;
    }
}
