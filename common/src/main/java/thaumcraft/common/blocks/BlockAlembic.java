package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.relics.ItemResonator;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.tiles.TileAlembic;

/** Flattened blockMetalDevice:1. The original alembic has no GUI or ticker. */
public final class BlockAlembic extends BaseEntityBlock {
    public static final MapCodec<BlockAlembic> CODEC = simpleCodec(BlockAlembic::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final SoundEvent KNOCK = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "alembicknock"));

    public BlockAlembic(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileAlembic(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        // BlockMetalDevice:722-737: player S/W/N/E -> facing N/E/S/W.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof TileAlembic tile) || tile.getAmount() == 0) return 0;
        return tile.getAmount() * 14 / tile.getMaxAmount() + 1;
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                        Player player, InteractionHand hand, BlockHitResult hit) {
        if (held.getItem() instanceof ItemResonator) {
            if (player.getItemInHand(hand) != held) return ItemInteractionResult.FAIL;
            return switch (ItemResonator.tryInspect(new UseOnContext(player, hand, hit))) {
                case SUCCESS -> ItemInteractionResult.SUCCESS;
                case CONSUME -> ItemInteractionResult.CONSUME;
                case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
                case FAIL -> ItemInteractionResult.FAIL;
                default -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            };
        }
        if (!(level.getBlockEntity(pos) instanceof TileAlembic tile)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        ItemInteractionResult phial = ItemEssence.interact(held, level, pos, player, hand, hit);
        if (phial != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return phial;
        if (ItemJar.isJar(held)) return interactJar(held, level, pos, player, hand, hit);
        ItemInteractionResult label = ItemJarLabel.interact(held, level, pos, player, hand, hit);
        if (label != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return label;
        boolean wand = held.getItem() instanceof ItemWandCasting;
        if (!wand) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!mayUse(level, pos, player, held, hit)) return ItemInteractionResult.FAIL;
        if (!level.isClientSide && hit.getDirection().getAxis().isHorizontal()) tile.setFacing(hit.getDirection());
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** ItemJar calls this before placement, including native secondary-use. */
    public static InteractionResult tryJarInteraction(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
        return interactJar(context.getItemInHand(), context.getLevel(), context.getClickedPos(), player, context.getHand(), hit).result();
    }

    private static ItemInteractionResult interactJar(ItemStack held, Level level, BlockPos pos, Player player,
                                                     InteractionHand hand, BlockHitResult hit) {
        if (!ItemJar.isJar(held) || !level.hasChunkAt(pos) || !(level.getBlockState(pos).getBlock() instanceof BlockAlembic)
                || !(level.getBlockEntity(pos) instanceof TileAlembic tile)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!mayUse(level, pos, player, held, hit) || player.getItemInHand(hand) != held || held.isEmpty()) return ItemInteractionResult.FAIL;
        // BlockMetalDevice:590-600 precedes the held-jar branch. Phials, unlike
        // held jars, are handled first by ItemEssence.onItemUseFirst above.
        ItemInteractionResult label = ItemJarLabel.interact(held, level, pos, player, hand, hit);
        if (label != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return label;
        Aspect sourceType = tile.getAspect(), storedType = ItemJar.getAspect(held), filter = ItemJar.getFilter(held);
        int source = tile.getAmount(), stored = ItemJar.getAmount(held);
        boolean emptyItem = ItemJar.isEmptyIngredient(held);
        if (sourceType == null || Aspect.getAspect(sourceType.getTag()) != sourceType || source <= 0 || stored < 0 || stored > 64
                || stored > 0 && storedType != sourceType || filter != null && filter != sourceType
                || !emptyItem && held.getCount() != 1 || stored == 0 && filter == null && !emptyItem) return ItemInteractionResult.FAIL;
        int taken = ItemJar.isVoid(held) ? source : Math.min(64 - stored, source);
        if (taken <= 0) return ItemInteractionResult.FAIL;
        ItemStack output = ItemJar.withContents(held, sourceType, Math.min(64, stored + taken));
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!EssentiaTransfers.exchangeItem(tile, player, hand, held, sourceType, source,
                sourceType, source - taken, output, !emptyItem)) return ItemInteractionResult.FAIL;
        level.playSound(null, pos, SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, .5F,
                1 + (level.random.nextFloat() - level.random.nextFloat()) * .3F);
        return ItemInteractionResult.CONSUME;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof TileAlembic tile)) return InteractionResult.PASS;
        if (!mayUse(level, pos, player, ItemStack.EMPTY, hit)) return InteractionResult.FAIL;
        ItemInteractionResult label = ItemJarLabel.interact(player.getMainHandItem(), level, pos, player, InteractionHand.MAIN_HAND, hit);
        if (label != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return label.result();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            // Source BlockMetalDevice:602-608: only clear contents after the
            // separate, higher-priority label-removal action has declined.
            tile.clearContents();
            level.playSound(null, pos, KNOCK, SoundSource.BLOCKS, .2F, 1);
            level.playSound(null, pos, SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, .5F,
                    1 + (level.random.nextFloat() - level.random.nextFloat()) * .3F);
        } else {
            int amount = tile.getAmount();
            int message = tile.getAspect() == null || amount == 0 ? 1
                    : amount < tile.getMaxAmount() * .4 ? 2
                    : amount < tile.getMaxAmount() * .8 ? 3
                    : amount < tile.getMaxAmount() ? 4 : 5;
            player.displayClientMessage(Component.translatable("tile.alembic.msg." + message).withStyle(ChatFormatting.DARK_AQUA), false);
            level.playSound(null, pos, KNOCK, SoundSource.BLOCKS, .2F, 1);
        }
        return InteractionResult.CONSUME;
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && !level.isClientSide && level.getBlockEntity(pos) instanceof TileAlembic tile && tile.getFilter() != null) {
            // Original BlockMetalDevice:534-535 drops the label, not essentia.
            DeferredItemDrops.enqueue((ServerLevel) level, pos, List.of(ItemJarLabel.blank()));
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    private static boolean mayUse(Level level, BlockPos pos, Player player, ItemStack held, BlockHitResult hit) {
        return player.level() == level && !player.isSpectator() && hit.getBlockPos().equals(pos)
                && level.hasChunkAt(pos) && level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.getBlockEntity(pos) instanceof TileAlembic tile && !EssentiaTransfers.isLocked(tile)
                && player.canInteractWithBlock(pos, 0) && level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, hit.getDirection(), held);
    }

    /**
     * Wand integration hook for native secondary-use (which skips block use).
     * Call from ItemWandCasting.useOn before its other triggers. Horizontal
     * clicks change the blocked/output-panel face for free, even while sneaking.
     */
    public static InteractionResult tryWandInteraction(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack held = context.getItemInHand();
        if (player == null || !(held.getItem() instanceof ItemWandCasting) || !level.hasChunkAt(pos)
                || !(level.getBlockEntity(pos) instanceof TileAlembic tile)) return InteractionResult.PASS;
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), pos, context.isInside());
        if (!mayUse(level, pos, player, held, hit)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (context.getClickedFace().getAxis().isHorizontal()) {
            tile.setFacing(context.getClickedFace());
            player.swing(context.getHand(), true);
        }
        return InteractionResult.CONSUME;
    }
}
