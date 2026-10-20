package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.items.relics.ItemHandMirror;
import thaumcraft.common.items.relics.ItemHandMirror.MirrorLink;
import thaumcraft.common.lib.crafting.DeviceContainerRules;
import thaumcraft.common.lib.crafting.DeviceContainerRules.MirrorLinkAction;
import thaumcraft.common.tiles.TileMirror;

/**
 * Original BlockMirrorItem: the item of the magic mirror.
 *
 * <p>Right clicking a mirror that is not linked yet binds the item to that
 * mirror (oracle BlockMirrorItem:86-139); placing the bound item puts the
 * second mirror of the pair into the world and links the two (oracle
 * BlockMirrorItem:141-165). The link of the original was written into the
 * NBT of the item, here it is the same data component the hand mirror uses,
 * so a mirror that is picked up remembers its partner as well.</p>
 */
public final class ItemMirrorBlock extends BlockItem {
    private static final SoundEvent JAR = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "jar"));

    public ItemMirrorBlock(Block block, Properties properties) {
        super(block, properties);
    }

    /**
     * Oracle BlockMirrorItem:86-139 - right clicking a mirror that has no
     * valid partner binds this item to it and gives the bound item back.
     */
    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!(level.getBlockState(pos).getBlock() instanceof BlockMirror)) return super.useOn(context);
        if (level.isClientSide) return InteractionResult.SUCCESS;
        TileMirror mirror = level.getBlockEntity(pos) instanceof TileMirror tile ? tile : null;
        MirrorLinkAction action = DeviceContainerRules.mirrorItemLink(true, mirror != null, mirror != null && mirror.isLinkValid());
        if (action == MirrorLinkAction.ALREADY_LINKED) {
            // Oracle BlockMirrorItem:112 - a mirror with a valid partner refuses another one.
            player.displayClientMessage(Component.translatable("tc.mirroralreadylinked")
                    .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.ITALIC), false);
            return InteractionResult.PASS;
        }
        if (action != MirrorLinkAction.BIND_ITEM) return InteractionResult.PASS;
        ItemStack bound = new ItemStack(this);
        bound.set(ThaumcraftLegacyItems.MIRROR_LINK.get(),
                MirrorLink.of(pos, level.dimension().location().toString(), level.dimension().location().getPath()));
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, JAR, SoundSource.BLOCKS, 1.0F, 2.0F);
        if (!player.getInventory().add(bound)) player.drop(bound, false);
        if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        player.releaseUsingItem();
        return InteractionResult.SUCCESS;
    }

    /**
     * Oracle BlockMirrorItem:141-165 - the placed mirror takes the partner of
     * the item and links the pair.
     */
    @Override public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (!result.consumesAction() || context.getLevel().isClientSide) return result;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        MirrorLink link = linkOf(context.getItemInHand());
        if (link == null) return result;
        if (!(level.getBlockEntity(pos) instanceof TileMirror mirror)) return result;
        mirror.linkX = link.x();
        mirror.linkY = link.y();
        mirror.linkZ = link.z();
        mirror.linkDim = link.dimension();
        mirror.restoreLink();
        return result;
    }

    /**
     * The partner of a mirror item: the bound item carries it in the mirror
     * link component, a mirror that was picked up carries it in its custom
     * data (oracle BlockMirror:128-135).
     */
    private static MirrorLink linkOf(ItemStack stack) {
        MirrorLink link = stack.get(ThaumcraftLegacyItems.MIRROR_LINK.get());
        if (link != null) return link;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        var tag = data.copyTag();
        if (!tag.contains("linkX") || !tag.contains("linkY") || !tag.contains("linkZ") || !tag.contains("linkDim")) return null;
        String dimension = tag.getString("linkDim");
        ResourceLocation location = ResourceLocation.tryParse(dimension);
        return new MirrorLink(tag.getInt("linkX"), tag.getInt("linkY"), tag.getInt("linkZ"), dimension,
                location == null ? dimension : location.getPath());
    }
}
