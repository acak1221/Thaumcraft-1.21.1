package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
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
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.relics.ItemResonator;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.essentia.TubeGeometry;
import thaumcraft.common.tiles.TileTube;

/** Freely floating TC4 single-unit pipes; no GUI or invented support rule. */
public final class BlockTube extends BaseEntityBlock {
    public enum Kind implements StringRepresentable {
        BASIC("basic"), VALVE("valve"), FILTER("filter"), RESTRICT("restrict"), ONEWAY("oneway");
        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;
        Kind(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public static final MapCodec<BlockTube> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(), Kind.CODEC.optionalFieldOf("kind").forGetter((BlockTube tube) -> Optional.of(tube.kind())),
            // Decode the accepted basic/valve block codec too; new encodings use kind.
            Codec.BOOL.optionalFieldOf("valve", false).forGetter((BlockTube tube) -> false)).apply(instance,
                    (properties, kind, valve) -> new BlockTube(properties, kind.orElse(valve ? Kind.VALVE : Kind.BASIC))));
    private static final SoundEvent TOOL = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "tool"));
    private static final SoundEvent PAGE = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "page"));
    private final Kind kind;

    public BlockTube(Properties properties) { this(properties, Kind.BASIC); }
    public BlockTube(Properties properties, boolean valve) { this(properties, valve ? Kind.VALVE : Kind.BASIC); }
    public BlockTube(Properties properties, Kind kind) { super(properties); this.kind = java.util.Objects.requireNonNull(kind); }
    public Kind kind() { return kind; }
    public boolean isValve() { return kind == Kind.VALVE; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return TileTube.create(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean indexed = context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof LivingEntity entity
                && (isIndexedTool(entity.getMainHandItem()) || isIndexedTool(entity.getOffhandItem()));
        return indexed ? TubeGeometry.indexedShape(level, pos) : TubeGeometry.selectionShape(level, pos);
    }

    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TubeGeometry.collisionShape(level, pos);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) { return 0; }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        try {
            if (kind == Kind.FILTER && !replacement.is(this)) {
                EssentiaTransfers.returnTubeFilterOnRemove(level, pos, state, replacement);
            }
        } finally {
            super.onRemove(state, level, pos, replacement, moving);
        }
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ThaumcraftTubes.TUBE_BLOCK_ENTITY.get(), TileTube::tick);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.level() != level || player.getItemInHand(hand) != held || !hit.getBlockPos().equals(pos)
                || !level.hasChunkAt(pos) || !level.getBlockState(pos).is(this)) return ItemInteractionResult.FAIL;
        UseOnContext context = new UseOnContext(player, hand, hit);
        // Original resonator ItemUseFirst must precede labels and valve hand use.
        InteractionResult inspect = ItemResonator.tryInspect(context);
        if (inspect != InteractionResult.PASS) return itemResult(inspect);
        if (held.getItem() instanceof ItemWandCasting) return itemResult(tryWandInteraction(context));
        InteractionResult label = tryLabelInteraction(context);
        return itemResult(label != InteractionResult.PASS ? label : tryHandInteraction(context));
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.level() != level || !hit.getBlockPos().equals(pos) || !level.hasChunkAt(pos)
                || !level.getBlockState(pos).is(this)) return InteractionResult.FAIL;
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        InteractionResult label = tryLabelInteraction(context);
        return label != InteractionResult.PASS ? label : tryHandInteraction(context);
    }

    private static boolean isIndexedTool(ItemStack held) {
        return held.getItem() instanceof ItemWandCasting || held.getItem() instanceof ItemResonator;
    }

    private static ItemInteractionResult itemResult(InteractionResult result) {
        return switch (result) {
            case SUCCESS -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> ItemInteractionResult.FAIL;
            default -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }

    /** Shared by block use and ItemJarLabel's native secondary-use hook. */
    public static InteractionResult tryLabelInteraction(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack held = context.getItemInHand();
        if (player == null || isIndexedTool(held)) return InteractionResult.PASS;
        if (!level.isClientSide && (!(level instanceof ServerLevel server) || !server.getServer().isSameThread())) return InteractionResult.FAIL;
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof TileTube tube) || !tube.isFilter()) return InteractionResult.PASS;
        Aspect before = tube.getFilter();
        boolean remove = player.isShiftKeyDown() && before != null;
        if (!remove && !(held.getItem() instanceof ItemJarLabel)) return InteractionResult.PASS;
        if (!mayUse(context, tube, held) || !TubeGeometry.hitsBlock(player, tube)
                || !mayUse(context, tube, held) || tube.getFilter() != before) return InteractionResult.FAIL;
        // Unlike a jar, a blank tube label must NOT infer from stored essentia.
        if (!remove && (before != null || held.isEmpty() || ItemJarLabel.aspect(held) == null)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean changed = remove ? EssentiaTransfers.removeTubeFilter(tube, player, context.getHand(), held, before)
                : EssentiaTransfers.attachTubeFilter(tube, player, context.getHand(), held);
        if (!changed) return InteractionResult.FAIL;
        level.playSound(null, pos, PAGE, SoundSource.BLOCKS, 1, 1);
        return InteractionResult.CONSUME;
    }

    /** Ordinary hand/item use; tube block items and inspection/configuration tools fall through. */
    public static InteractionResult tryHandInteraction(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack held = context.getItemInHand();
        // Original BlockTube:310 excludes the ONE old ItemBlock for ALL tube
        // metadata, including independent centrifuge2 and crystallizer7. Their
        // flattened native items must still reach placement, not hand-toggle.
        if (player == null || isIndexedTool(held) || held.getItem() instanceof BlockItem item
                && (item.getBlock() instanceof BlockTube || item.getBlock() instanceof BlockTubeBuffer
                    || item.getBlock() instanceof BlockCentrifuge || item.getBlock() instanceof BlockEssentiaCrystalizer)
                || !level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof TileTube tube) || !tube.isValve()) return InteractionResult.PASS;
        if (!mayUse(context, tube, held)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()
                || !TubeGeometry.hitsBlock(player, tube) || !mayUse(context, tube, held)) return InteractionResult.FAIL;
        return tube.toggleFlow() ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    /** Also called by the wand item for native secondary-use, which bypasses block use. */
    public static InteractionResult tryWandInteraction(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack held = context.getItemInHand();
        if (player == null || !(held.getItem() instanceof ItemWandCasting) || !level.hasChunkAt(pos)
                || !(level.getBlockEntity(pos) instanceof TileTube tube)) return InteractionResult.PASS;
        if (!mayUse(context, tube, held)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) return InteractionResult.FAIL;
        TubeGeometry.IndexedHit traced = TubeGeometry.retrace(player, tube);
        if (traced == null || !mayUse(context, tube, held)) return InteractionResult.FAIL;
        Direction face = traced.index() < 6 ? Direction.from3DDataValue(traced.index()) : null;
        boolean changed = face == null ? tube.cycleFacing() : tube.setSideOpen(face, !tube.isSideOpen(face));
        if (changed) {
            level.playSound(null, pos, TOOL, SoundSource.BLOCKS, .5F, .9F + level.random.nextFloat() * .2F);
            player.swing(context.getHand(), true);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean mayUse(UseOnContext context, TileTube tube, ItemStack held) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return player != null && player.level() == level && player.getItemInHand(context.getHand()) == held
                && !player.isSpectator() && !tube.isRemoved() && tube.getLevel() == level && !EssentiaTransfers.isLocked(tube)
                && level.hasChunkAt(pos) && level.getBlockEntity(pos) == tube && level.getBlockState(pos) == tube.getBlockState()
                && level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(pos)
                && player.canInteractWithBlock(pos, 0) && level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, context.getClickedFace(), held) && player.getItemInHand(context.getHand()) == held;
    }
}
