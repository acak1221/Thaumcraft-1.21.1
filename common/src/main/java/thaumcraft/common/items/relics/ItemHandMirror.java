package thaumcraft.common.items.relics;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The Hand Mirror (original ItemHandMirror). A relic of uncommon rarity that is linked to a mirror block: right clicking a mirror stores
 * the position and the dimension of that mirror on the item, and using the linked mirror again opens the mirror's view.
 *
 * <p>The original's view (the gui number sixteen) and its transport function belong to the mirror screen, which is not part of this port.
 * The link itself is kept in a data component instead of the original's NBT tags, and a link whose mirror has gone is cleared with the
 * original's message and sound.
 */
public final class ItemHandMirror extends Item {
    /** The link of a hand mirror: the position and the dimension of the mirror it was linked to. */
    public record MirrorLink(int x, int y, int z, String dimension, String dimensionName) {
        public static MirrorLink of(BlockPos pos, String dimension, String dimensionName) {
            return new MirrorLink(pos.getX(), pos.getY(), pos.getZ(), dimension, dimensionName);
        }
    }

    private static final SoundEvent JAR = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "jar"));
    private static final SoundEvent ZAP = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "zap"));

    public ItemHandMirror(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    /** The link of the stack, or null when the mirror was never linked. */
    public static MirrorLink link(ItemStack stack) { return stack.get(ThaumcraftLegacyItems.MIRROR_LINK.get()); }

    /**
     * Original ItemHandMirror.onItemUseFirst: right clicking a mirror links the hand mirror to it, with the jar sound and the original's
     * message. Right clicking anything else does nothing.
     */
    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || level.isClientSide) return InteractionResult.PASS;
        if (!EquipmentRules.isMirrorBlock(blockId(level, pos))) return InteractionResult.PASS;
        String dimension = level.dimension().location().toString();
        context.getItemInHand().set(ThaumcraftLegacyItems.MIRROR_LINK.get(), MirrorLink.of(pos, dimension, level.dimension().location().getPath()));
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, JAR, SoundSource.BLOCKS, 1.0F, 2.0F);
        player.displayClientMessage(Component.translatable("tc.handmirrorlinked").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.ITALIC), false);
        player.releaseUsingItem();
        return InteractionResult.SUCCESS;
    }

    /**
     * Original ItemHandMirror.func_77659_a: a linked mirror opens the mirror's view; a link whose mirror is gone is cleared with the
     * original's sound and message. The view itself is the mirror screen, which is not part of this port.
     */
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        MirrorLink link = link(stack);
        if (link == null) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide && !mirrorStillThere(level, link)) {
            stack.remove(ThaumcraftLegacyItems.MIRROR_LINK.get());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ZAP, SoundSource.PLAYERS, 1.0F, 0.8F);
            player.displayClientMessage(Component.translatable("tc.handmirrorerror").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.ITALIC), false);
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MirrorLink link = link(stack);
        if (link == null) return;
        tooltip.add(Component.translatable("tc.handmirrorlinkedto").append(" " + link.x() + "," + link.y() + "," + link.z()
                + " in " + link.dimensionName()));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** Whether the mirror the hand mirror is linked to is still a mirror. */
    public static boolean mirrorStillThere(Level level, MirrorLink link) {
        if (!level.dimension().location().toString().equals(link.dimension())) return true;
        BlockPos pos = new BlockPos(link.x(), link.y(), link.z());
        return level.isLoaded(pos) && EquipmentRules.isMirrorBlock(blockId(level, pos));
    }

    private static String blockId(Level level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
    }
}
