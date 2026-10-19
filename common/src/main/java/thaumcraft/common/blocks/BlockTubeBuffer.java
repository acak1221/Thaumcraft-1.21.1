package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.items.relics.ItemResonator;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.essentia.TubeGeometry;
import thaumcraft.common.tiles.TileTubeBuffer;

/** Independent mixed buffer, free-floating like the original tube block. */
public final class BlockTubeBuffer extends BaseEntityBlock {
    public static final MapCodec<BlockTubeBuffer> CODEC = simpleCodec(BlockTubeBuffer::new);
    private static final SoundEvent TOOL = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "tool"));
    private static final SoundEvent SQUEEK = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "squeek"));
    public BlockTubeBuffer(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileTubeBuffer(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean indexed = context instanceof EntityCollisionContext collision && collision.getEntity() instanceof LivingEntity entity
                && (isTool(entity.getMainHandItem()) || isTool(entity.getOffhandItem()));
        return indexed ? TubeGeometry.indexedShape(level, pos) : TubeGeometry.bufferSelectionShape();
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return TubeGeometry.collisionShape(level, pos); }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        int amount = level.getBlockEntity(pos) instanceof TileTubeBuffer buffer ? buffer.getAmount() : 0;
        return amount * 14 / 8 + (amount > 0 ? 1 : 0);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return createTickerHelper(type, ThaumcraftTubes.BUFFER_BLOCK_ENTITY.get(), TileTubeBuffer::tick); }
    private static boolean isTool(ItemStack held) { return held.getItem() instanceof ItemWandCasting || held.getItem() instanceof ItemResonator; }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.level() != level || player.getItemInHand(hand) != held || !pos.equals(hit.getBlockPos()) || !level.hasChunkAt(pos) || !level.getBlockState(pos).is(this)) return ItemInteractionResult.FAIL;
        var context = new UseOnContext(player, hand, hit);
        InteractionResult inspect = ItemResonator.tryInspect(context);
        InteractionResult result = inspect != InteractionResult.PASS ? inspect : tryWandInteraction(context);
        return switch (result) {
            case SUCCESS -> ItemInteractionResult.SUCCESS; case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL; case FAIL -> ItemInteractionResult.FAIL;
            default -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }
    /** Also used by the wand item when native secondary-use skips block use. */
    public static InteractionResult tryWandInteraction(UseOnContext context) {
        Player player = context.getPlayer(); Level level = context.getLevel(); BlockPos pos = context.getClickedPos(); ItemStack held = context.getItemInHand();
        if (player == null || !(held.getItem() instanceof ItemWandCasting)) return InteractionResult.PASS;
        if (!level.isClientSide && (!(level instanceof ServerLevel server) || !server.getServer().isSameThread())) return InteractionResult.FAIL;
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof TileTubeBuffer buffer)) return InteractionResult.PASS;
        if (!mayUse(context, held, buffer)) return InteractionResult.FAIL;
        var hit = TubeGeometry.retrace(player, buffer);
        if (hit == null || !mayUse(context, held, buffer)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (hit.index() >= 6) return InteractionResult.CONSUME; // Core has no fake orientation operation.
        Direction face = Direction.from3DDataValue(hit.index()); boolean sneak = player.isShiftKeyDown();
        boolean changed = sneak ? buffer.cycleChoke(face) : buffer.setSideOpen(face, !buffer.isSideOpen(face));
        if (changed) {
            level.playSound(null, pos, sneak ? SQUEEK : TOOL, SoundSource.BLOCKS, sneak ? .6F : .5F, (sneak ? 1.1F : .9F) + level.random.nextFloat() * .2F);
            player.swing(context.getHand(), true);
        }
        return InteractionResult.CONSUME;
    }
    private static boolean mayUse(UseOnContext context, ItemStack held, TileTubeBuffer buffer) {
        Player player = context.getPlayer(); Level level = context.getLevel(); BlockPos pos = context.getClickedPos();
        return player != null && player.level() == level && player.getItemInHand(context.getHand()) == held && !player.isSpectator()
                && !buffer.isRemoved() && !buffer.isQuarantined() && buffer.getLevel() == level && !EssentiaTransfers.isLocked(buffer)
                && level.hasChunkAt(pos) && level.getBlockEntity(pos) == buffer && level.getBlockState(pos) == buffer.getBlockState()
                && level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(pos) && player.canInteractWithBlock(pos, 0)
                && level.mayInteract(player, pos) && player.mayUseItemAt(pos, context.getClickedFace(), held) && player.getItemInHand(context.getHand()) == held;
    }
}
