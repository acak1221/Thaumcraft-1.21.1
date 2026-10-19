package thaumcraft.common.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockAlembic;
import thaumcraft.common.blocks.BlockJar;
import thaumcraft.common.blocks.BlockTube;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftLabels;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.items.relics.ItemResonator;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileJar;

/** Flattened ItemResource:13; binding is an aspect identity, not stored essentia. */
public final class ItemJarLabel extends Item {
    private static final SoundEvent PAGE = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "page"));
    private static final ItemInteractionResult PASS = ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

    public ItemJarLabel(Properties properties) { super(properties); }
    public static ItemStack blank() { return new ItemStack(ThaumcraftContent.MATERIALS.get("jar_label").get()); }
    public static ItemStack bound(String tag) {
        ItemStack stack = blank();
        stack.set(ThaumcraftLabels.JAR_LABEL_DATA.get(), new JarLabelData(tag));
        return stack;
    }
    public static JarLabelData data(ItemStack stack) { return stack.get(ThaumcraftLabels.JAR_LABEL_DATA.get()); }
    public static Aspect aspect(ItemStack stack) {
        JarLabelData data = data(stack);
        return data == null ? null : Aspect.getAspect(data.tag());
    }
    public static Component aspectName(Aspect aspect) { return Component.translatable("tc.aspect." + aspect.getTag()); }

    @Override public Component getName(ItemStack stack) {
        Aspect aspect = aspect(stack);
        return aspect == null ? super.getName(stack) : Component.translatable("item.thaumcraft.jar_label.bound", aspectName(aspect));
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        Aspect aspect = aspect(stack);
        if (aspect != null) tooltip.add(aspectName(aspect).copy().withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        InteractionResult tube = BlockTube.tryLabelInteraction(context);
        if (tube != InteractionResult.PASS) return tube;
        // Native secondary-use skips block interaction. Use the SAME validated
        // transaction here rather than an unguarded second attachment path.
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
        return interact(context.getItemInHand(), context.getLevel(), context.getClickedPos(), player, context.getHand(), hit).result();
    }

    public static ItemInteractionResult interact(ItemStack held, Level level, BlockPos pos, Player player,
                                                 InteractionHand hand, BlockHitResult hit) {
        // Item-first inspection must never fall through to sneak label removal.
        // The block/item's shared inspection dispatcher owns its one message.
        if (held.getItem() instanceof ItemResonator) return PASS;
        if (!level.hasChunkAt(pos)) return PASS;
        BlockEntity tile = level.getBlockEntity(pos);
        TileJar jar = tile instanceof TileJar value && level.getBlockState(pos).getBlock() instanceof BlockJar ? value : null;
        TileAlembic alembic = tile instanceof TileAlembic value && level.getBlockState(pos).getBlock() instanceof BlockAlembic ? value : null;
        if (jar == null && alembic == null) return PASS;
        Aspect filter = jar != null ? jar.getFilter() : alembic.getFilter();
        // BlockJar:258 versus BlockMetalDevice:590: jars require the label face;
        // alembics allow removal from any face. Both return a BLANK label.
        boolean remove = player.isShiftKeyDown() && filter != null && (jar == null || hit.getDirection() == jar.getFacing());
        if (!(held.getItem() instanceof ItemJarLabel) && !remove) return PASS;
        if (!mayUse(level, pos, player, held, hand, hit, tile)) return ItemInteractionResult.FAIL;
        if (remove) {
            if (!level.isClientSide) {
                if (jar != null) jar.setFilter(null);
                else alembic.setFilter(null);
                DeferredItemDrops.enqueue((ServerLevel) level, pos, List.of(blank()));
                level.playSound(null, pos, PAGE, SoundSource.BLOCKS, 1, jar != null ? 1 : 1.1F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (filter != null || held.isEmpty()) return ItemInteractionResult.FAIL;
        int amount = jar != null ? jar.getAmount() : alembic.getAmount();
        // Original filled containers override a pre-bound label's type. A blank
        // must never infer a type from an empty container's stale aspect field.
        Aspect chosen = amount > 0 ? (jar != null ? jar.getAspect() : alembic.getAspect()) : aspect(held);
        if (chosen == null || Aspect.getAspect(chosen.getTag()) != chosen) return ItemInteractionResult.FAIL;
        if (!level.isClientSide) {
            if (jar != null) {
                if (!jar.setFilter(chosen)) return ItemInteractionResult.FAIL;
                jar.setFacing(player.getDirection().getOpposite());
            } else alembic.setFilter(chosen);
            // BlockJar:282 / BlockMetalDevice:617 consume even in creative.
            held.shrink(1);
            // Native creative Item.useOn restores the passed stack's count.
            // Detach its paid remainder so secondary-use cannot undo the debit.
            player.setItemInHand(hand, held.isEmpty() ? ItemStack.EMPTY : held.copy());
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            level.playSound(null, pos, PAGE, SoundSource.BLOCKS, 1, .9F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean mayUse(Level level, BlockPos pos, Player player, ItemStack held, InteractionHand hand,
                                  BlockHitResult hit, BlockEntity tile) {
        return player.level() == level && !player.isSpectator() && hit.getBlockPos().equals(pos)
                && level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(pos)
                && player.canInteractWithBlock(pos, 0) && level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, hit.getDirection(), held)
                && player.getItemInHand(hand) == held && !tile.isRemoved() && !EssentiaTransfers.isLocked(tile)
                && level.getBlockEntity(pos) == tile
                && level.getBlockState(pos).is(tile.getBlockState().getBlock());
    }
}
