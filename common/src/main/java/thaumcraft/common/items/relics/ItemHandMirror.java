package thaumcraft.common.items.relics;

import dev.architectury.registry.menu.MenuRegistry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.container.ContainerHandMirror;
import thaumcraft.common.lib.crafting.DeviceContainerRules;
import thaumcraft.common.lib.crafting.DeviceContainerRules.HandMirrorTarget;
import thaumcraft.common.lib.items.EquipmentRules;
import thaumcraft.common.tiles.TileMirror;

/**
 * The Hand Mirror (original ItemHandMirror). A relic of uncommon rarity that is linked to a mirror block: right clicking a mirror stores
 * the position and the dimension of that mirror on the item, and using the linked mirror again opens the mirror's view.
 *
 * <p>The view is the hand mirror screen, the original's gui number sixteen, and the item is the bridge of that screen: the screen never
 * guesses a destination, the item knows its mirror and carries an item to it exactly as the original did (oracle ItemHandMirror:157-188).
 * The link itself is kept in a data component instead of the original's NBT tags, and a link whose mirror has gone is cleared with the
 * original's message and sound.</p>
 */
public final class ItemHandMirror extends Item implements ContainerHandMirror.Bridge {
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
        // Oracle ItemHandMirror:107-108 - only an item mirror can be linked, never an essentia mirror.
        if (!(level.getBlockEntity(pos) instanceof TileMirror)) return InteractionResult.PASS;
        String dimension = level.dimension().location().toString();
        context.getItemInHand().set(ThaumcraftLegacyItems.MIRROR_LINK.get(), MirrorLink.of(pos, dimension, level.dimension().location().getPath()));
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, JAR, SoundSource.BLOCKS, 1.0F, 2.0F);
        player.displayClientMessage(Component.translatable("tc.handmirrorlinked").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.ITALIC), false);
        player.releaseUsingItem();
        return InteractionResult.SUCCESS;
    }

    /**
     * Original ItemHandMirror.func_77659_a: a linked mirror opens the mirror's view; a link whose mirror is gone is cleared with the
     * original's sound and message. The view is the hand mirror screen, the original's gui number sixteen.
     */
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        MirrorLink link = link(stack);
        if (link == null) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        ServerLevel target = targetWorld(level, link);
        boolean mirrorFound = target != null && target.getBlockEntity(new BlockPos(link.x(), link.y(), link.z())) instanceof TileMirror;
        HandMirrorTarget outcome = DeviceContainerRules.handMirrorTarget(true, target != null, mirrorFound);
        if (DeviceContainerRules.handMirrorClearsLink(outcome)) {
            // Oracle ItemHandMirror:134-139 - the link dies with its mirror.
            stack.remove(ThaumcraftLegacyItems.MIRROR_LINK.get());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ZAP, SoundSource.PLAYERS, 1.0F, 0.8F);
            player.displayClientMessage(Component.translatable("tc.handmirrorerror").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.ITALIC), false);
            return InteractionResultHolder.fail(stack);
        }
        if (!DeviceContainerRules.handMirrorOpensView(outcome)) return InteractionResultHolder.pass(stack);
        // Oracle ItemHandMirror:140 - the mirror opens its own view.
        if (player instanceof ServerPlayer sender) openView(sender, hand);
        return InteractionResultHolder.consume(stack);
    }

    /** The registered hand mirror menu reads the hand of the viewer from the network buffer. */
    private void openView(ServerPlayer player, InteractionHand hand) {
        MenuRegistry.openExtendedMenu(player, new SimpleMenuProvider(
                (id, inventory, viewer) -> new ContainerHandMirror(id, inventory, hand),
                Component.translatable("container.handmirror")),
                buffer -> buffer.writeBoolean(hand == InteractionHand.OFF_HAND));
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
        if (link == null) return false;
        ServerLevel target = targetWorld(level, link);
        boolean mirrorFound = target != null && target.getBlockEntity(new BlockPos(link.x(), link.y(), link.z())) instanceof TileMirror;
        return DeviceContainerRules.handMirrorTarget(true, target != null, mirrorFound) == HandMirrorTarget.MIRROR;
    }

    /** The world the link points at, or null when that dimension does not exist. */
    private static ServerLevel targetWorld(Level level, MirrorLink link) {
        if (link == null || level == null || level.getServer() == null) return null;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(link.dimension()));
        if (key == null) return null;
        return level.getServer().getLevel(key);
    }

    // ------------------------------------------------------- the container bridge

    /** Oracle ContainerHandMirror:75 - a hand mirror never travels into its own slot. */
    @Override public boolean isMirror(ItemStack stack) { return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemHandMirror; }

    /** Oracle ItemHandMirror:158 - only a linked hand mirror can carry an item away. */
    @Override public boolean isLinked(ItemStack mirror) { return link(mirror) != null; }

    /**
     * Oracle ItemHandMirror:157-188 - the item leaves the hand mirror in front
     * of the mirror it is linked to, in the world of that mirror.
     */
    @Override public boolean transport(ItemStack mirror, ItemStack items, Player player, Level level) {
        MirrorLink link = link(mirror);
        if (link == null || items == null || items.isEmpty()) return false;
        ServerLevel target = targetWorld(level, link);
        boolean mirrorFound = target != null && target.getBlockEntity(new BlockPos(link.x(), link.y(), link.z())) instanceof TileMirror;
        HandMirrorTarget outcome = DeviceContainerRules.handMirrorTarget(link != null, target != null, mirrorFound);
        if (outcome != HandMirrorTarget.MIRROR) {
            if (DeviceContainerRules.handMirrorClearsLink(outcome)) {
                // Oracle ItemHandMirror:169-172 - the link dies with its mirror.
                mirror.remove(ThaumcraftLegacyItems.MIRROR_LINK.get());
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ZAP, SoundSource.PLAYERS, 1.0F, 0.8F);
                player.displayClientMessage(Component.translatable("tc.handmirrorerror").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.ITALIC), false);
            }
            return false;
        }
        BlockPos pos = new BlockPos(link.x(), link.y(), link.z());
        Direction facing = ((TileMirror) target.getBlockEntity(pos)).getFacing();
        // Oracle ItemHandMirror:175-181 - the item appears in front of the mirror.
        ItemEntity item = new ItemEntity(target, pos.getX() + 0.5D - facing.getStepX() * EquipmentRules.MIRROR_PUSH_OFFSET,
                pos.getY() + 0.5D - facing.getStepY() * EquipmentRules.MIRROR_PUSH_OFFSET,
                pos.getZ() + 0.5D - facing.getStepZ() * EquipmentRules.MIRROR_PUSH_OFFSET, items.copy());
        item.setDeltaMovement(facing.getStepX() * (float) EquipmentRules.MIRROR_ITEM_SPEED,
                facing.getStepY() * (float) EquipmentRules.MIRROR_ITEM_SPEED,
                facing.getStepZ() * (float) EquipmentRules.MIRROR_ITEM_SPEED);
        item.setPickUpDelay(EquipmentRules.MIRROR_ITEM_LIFETIME);
        target.addFreshEntity(item);
        // Oracle ItemHandMirror:183-184 - the portal sound and the sound of the mirror.
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.1F, 1.0F);
        target.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private static String blockId(Level level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
    }
}
