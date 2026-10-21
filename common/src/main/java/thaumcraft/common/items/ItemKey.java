package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import thaumcraft.common.blocks.BlockArcaneDoor;
import thaumcraft.common.blocks.BlockWoodenDevice;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.lib.items.EquipmentRules;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileOwned;

import java.util.List;

/**
 * The keys (original ItemKey). An iron key and a gold key link themselves to an arcane door or to an arcane pressure plate; a linked key
 * grants access to that door again, and the gold key may also hand the access on to others.
 *
 * <p>The ownership of a door lives in the TileOwned of the door: the first use of a key links the key to the door of its owner, every
 * further use writes the name of the player into the access list of the door.
 */
public final class ItemKey extends Item {
    /** The link of a key: the position of the door it was linked to and the kind of door. */
    public record KeyLink(String location, int type) {}

    /** The kind of the block a key works on: the arcane door. */
    private static final int KIND_DOOR = 0;
    /** The kind of the block a key works on: the arcane pressure plate. */
    private static final int KIND_PLATE = 1;

    private static final SoundEvent KEY = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "key"));

    private final int keyType;

    public ItemKey(Properties properties, int keyType) {
        super(properties.stacksTo(64).rarity(Rarity.UNCOMMON));
        this.keyType = keyType;
    }

    /** The kind of key: zero the iron key, one the gold key. */
    public int keyType() { return keyType; }

    /**
     * Oracle ItemKey:90-92 glints a key that carries its link data. The original used the
     * presence of the NBT compound; the native equivalent is the presence of the link component.
     */
    @Override public boolean isFoil(ItemStack stack) { return stack.has(ThaumcraftLegacyItems.KEY_LINK.get()); }

    /** The link of the stack, or null when the key was never linked. */
    public static KeyLink link(ItemStack stack) { return stack.get(ThaumcraftLegacyItems.KEY_LINK.get()); }

    /** Original ItemKey.onItemUseFirst: the key links itself to a door, or grants access to the door it is linked to. */
    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Target target = target(level, pos);
        if (target == null) return InteractionResult.PASS;
        TileOwned owned = owned(level, target.owner());
        if (owned == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        KeyLink link = link(stack);
        String name = player.getGameProfile().getName();
        String location = EquipmentRules.keyLocation(target.owner().getX(), target.owner().getY(), target.owner().getZ());
        if (link == null) {
            // Original ItemKey.onItemUseFirst: the key claims the door of its owner, or a door the player may enter with the gold key.
            if (!EquipmentRules.keyMayLink(name.equals(owned.owner()),
                    EquipmentRules.keyHasGrantAccess(owned.accessList(), name), keyType)) return InteractionResult.PASS;
            if (!level.isClientSide) {
                ItemStack linked = new ItemStack(stack.getItem(), 1);
                linked.set(ThaumcraftLegacyItems.KEY_LINK.get(), new KeyLink(location, target.kind()));
                if (!player.getInventory().add(linked)) player.drop(linked, false);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                player.displayClientMessage(Component.translatable(target.kind() == KIND_DOOR ? "tc.key1" : "tc.key2")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
                level.playSound(null, target.owner().getX() + 0.5, target.owner().getY() + 0.5, target.owner().getZ() + 0.5,
                        KEY, SoundSource.BLOCKS, 1.0F, 0.9F);
            }
            return InteractionResult.SUCCESS;
        }
        boolean owner = name.equals(owned.owner());
        boolean hasEntry = EquipmentRules.keyHasAccess(owned.accessList(), keyType, name);
        boolean hasGrant = EquipmentRules.keyHasGrantAccess(owned.accessList(), name);
        if (!owner && !hasEntry && !hasGrant && location.equals(link.location())) {
            // Original ItemKey.onItemUseFirst: the key writes its access entry into the access list of the door.
            if (!level.isClientSide) {
                String entry = EquipmentRules.keyAccessEntry(keyType, name);
                owned.addAccess(entry);
                if (target.kind() == KIND_DOOR) {
                    // Original ItemKey.onItemUseFirst: the upper half of the door carries the same access list.
                    TileOwned other = owned(level, target.other());
                    if (other != null) other.addAccess(entry);
                }
                player.displayClientMessage(Component.translatable(target.kind() == KIND_DOOR ? "tc.key3" : "tc.key5")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)
                        .append(keyType == EquipmentRules.KEY_GOLD
                                ? Component.translatable(target.kind() == KIND_DOOR ? "tc.key4" : "tc.key6")
                                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)
                                : Component.empty()), false);
                level.playSound(null, target.owner().getX() + 0.5, target.owner().getY() + 0.5, target.owner().getZ() + 0.5,
                        KEY, SoundSource.BLOCKS, 1.0F, 1.1F);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide) {
            // Original ItemKey.onItemUseFirst: the key belongs to another door, or the player may not enter this one.
            player.displayClientMessage(Component.translatable(location.equals(link.location()) ? "tc.key8" : "tc.key7")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
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
        tooltip.add(Component.translatable(link.type() == KIND_DOOR ? "tc.key10" : "tc.key11").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        String[] parts = link.location().split(",");
        String where = parts.length == 3 ? "x " + parts[0] + ", z " + parts[2] + ", y " + parts[1] : link.location();
        tooltip.add(Component.literal(where).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** The block a key works on: the arcane door or the arcane pressure plate of the wooden device. */
    private static Target target(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BlockArcaneDoor) {
            // Original ItemKey.onItemUseFirst: the owner of a door is its lower half, the upper half carries the same access list.
            boolean upper = state.getValue(BlockArcaneDoor.HALF) == Half.TOP;
            return new Target(KIND_DOOR, upper ? pos.below() : pos, upper ? pos : pos.above());
        }
        if (state.getBlock() instanceof BlockWoodenDevice) {
            int variant = BlockWoodenDevice.variant(state);
            // Original ItemKey.onItemUseFirst: the wooden device with the metadata two or three, the arcane pressure plate.
            if (variant == EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE
                    || variant == EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE_PRESSED) {
                return new Target(KIND_PLATE, pos, pos);
            }
        }
        return null;
    }

    /** The owned block entity of a position, or null when the position carries none. */
    private static TileOwned owned(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileOwned owned ? owned : null;
    }

    /** The block a key works on: its kind and the positions of the halves that carry the ownership. */
    private record Target(int kind, BlockPos owner, BlockPos other) {}
}
