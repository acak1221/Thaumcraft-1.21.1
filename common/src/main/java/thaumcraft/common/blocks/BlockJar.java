package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.relics.ItemResonator;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.tiles.TileJar;

public final class BlockJar extends BaseEntityBlock {
    public static final MapCodec<BlockJar> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(), Codec.BOOL.optionalFieldOf("void", false).forGetter(BlockJar::isVoid)).apply(instance, BlockJar::new));
    /**
     * Original BlockJar:90 - the light level of the jar.
     *
     * <p>The original sets the light with the 1.7.10 setter value 0.66 and that setter stores
     * {@code (int)(15*0.66)} = 9. Every metadata value of the original jar block emits that light, the
     * brain jar and the node jar included.</p>
     */
    public static final int LIGHT = 9;
    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 12, 13);
    private static final SoundEvent JAR = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "jar"));
    private final boolean voidJar;
    public BlockJar(Properties properties) { this(properties, false); }
    public BlockJar(Properties properties, boolean voidJar) { super(properties); this.voidJar = voidJar; }
    public boolean isVoid() { return voidJar; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileJar(ThaumcraftContent.JAR_BLOCK_ENTITY.get(), pos, state);
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ThaumcraftContent.JAR_BLOCK_ENTITY.get(), TileJar::tick);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    // Original BlockJar:296-304 distinguishes compact selection from FULL
    // entity collision. Do not enlarge the visual shell or selection outline.
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileJar jar && jar.getAmount() > 0
                ? jar.getAmount() * 14 / jar.getMaxAmount() + 1 : 0;
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
        if (!(level.getBlockEntity(pos) instanceof TileJar jar)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (EssentiaTransfers.isLocked(jar)) return ItemInteractionResult.FAIL;
        // Forge ItemEssence.onItemUseFirst ran BEFORE BlockJar's label branch.
        ItemInteractionResult phial = ItemEssence.interact(held, level, pos, player, hand, hit);
        if (phial != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return phial;
        ItemInteractionResult label = ItemJarLabel.interact(held, level, pos, player, hand, hit);
        if (label != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return label;
        if (!level.getBlockState(pos).is(this) || !mayUse(level, pos, player, held, hit)
                || player.getItemInHand(hand) != held || jar.isRemoved() || level.getBlockEntity(pos) != jar) return ItemInteractionResult.FAIL;
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !player.isShiftKeyDown()
                || !level.getBlockState(pos).is(this) || !(level.getBlockEntity(pos) instanceof TileJar jar)) return InteractionResult.PASS;
        if (EssentiaTransfers.isLocked(jar)) return InteractionResult.FAIL;
        if (!mayUse(level, pos, player, player.getMainHandItem(), hit)) return InteractionResult.FAIL;
        ItemInteractionResult label = ItemJarLabel.interact(player.getMainHandItem(), level, pos, player, InteractionHand.MAIN_HAND, hit);
        if (label != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return label.result();
        if (!level.isClientSide) {
            jar.clearContents();
            level.playSound(null, pos, JAR, SoundSource.BLOCKS, .4F, 1);
            level.playSound(null, pos, SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, .5F,
                    1 + (level.random.nextFloat() - level.random.nextFloat()) * .3F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        if (level.getBlockEntity(pos) instanceof TileJar jar && entity != null) jar.setFacing(entity.getDirection().getOpposite());
    }
    private static boolean mayUse(Level level, BlockPos pos, Player player, ItemStack held, BlockHitResult hit) {
        return player.level() == level && !player.isSpectator() && hit.getBlockPos().equals(pos)
                && level.hasChunkAt(pos) && level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(pos)
                && player.canInteractWithBlock(pos, 0) && level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, hit.getDirection(), held);
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack jarItem = new ItemStack(this);
        BlockEntity tile = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        // One dynamic drop, not this PLUS the vanilla loot table. Native save
        // collects the filter component alongside the existing contents data.
        if (tile instanceof TileJar jar && (jar.getAmount() > 0 || jar.getFilter() != null)) {
            jar.saveToItem(jarItem, params.getLevel().registryAccess());
            // Original ItemJarFilled:55-56 also makes an empty labelled jar
            // non-stackable. Ordinary empty, unlabelled jars remain stackable.
            jarItem.set(DataComponents.MAX_STACK_SIZE, 1);
        }
        return List.of(jarItem);
    }
}
