package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileWardingStone;

/**
 * Original BlockCosmeticSolid: the sixteen decorative stones of the Outer Lands.
 *
 * <p>The original is ONE block with sixteen metadata values; the metadata became the
 * {@link #VARIANT} block state. Minecraft 1.21.1 binds the hardness and the blast resistance to
 * the block and not to the block state, so the metadata that differ in hardness (the oracle
 * BlockCosmeticSolid:213-225 and :241-253) became separate blocks of this class; every one of them
 * hosts the metadata of its own hardness group. The two metadata values that carry a block entity,
 * the warding stone and the node, keep it.
 */
public final class BlockCosmeticSolid extends BaseEntityBlock implements StateDependentBlockEntity, PerStateBlockEntity, OriginalPistonPushable {
    public static final MapCodec<BlockCosmeticSolid> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 2).fieldOf("group").forGetter(BlockCosmeticSolid::group),
            propertiesCodec()).apply(instance, BlockCosmeticSolid::new));
    /** Original BlockCosmeticSolid metadata 0..15. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 15);

    /**
     * The variant property of every group of the original metadata.
     *
     * <p>Minecraft 1.21.1 binds the state space of a block to its property, so every group of the
     * original metadata became a block of its own and carries only the states of that group. The
     * property of a group therefore holds exactly the metadata of that group (they are not always consecutive), and it is chosen before the block
     * is built, because Minecraft builds the state definition inside the block constructor.
     */
    private static final java.util.Map<Integer, Property<Integer>> GROUP_PROPERTIES = new java.util.concurrent.ConcurrentHashMap<>();

    private static Property<Integer> groupProperty(int group) {
        return GROUP_PROPERTIES.computeIfAbsent(group, key -> {
            java.util.List<Integer> variants = EldritchBlockRules.cosmeticGroupVariants(key);
            // A group with a single metadata value has exactly one state, so it needs no property.
            if (variants.size() < 2) return null;
            return thaumcraft.common.lib.world.FamilyVariant.property(variants);
        });
    }

    /** The block of one group of the original metadata. */
    public static BlockCosmeticSolid group(int group, Properties properties) {
        return new BlockCosmeticSolid(group, pendingProperty(group), properties);
    }

    private final List<Integer> variants;
    private final Property<Integer> variantProperty;
    private final int group;

    /** The hardness and the blast resistance belong to the block in 1.21.1. */
    public BlockCosmeticSolid(int group, Properties properties) {
        this(group, pendingProperty(group), properties);
    }

    /**
     * The property of the group, handed over before the block is built: Minecraft builds the state
     * definition of a block inside the block constructor, before the fields of the block exist.
     */
    private static Property<Integer> pendingProperty(int group) {
        Property<Integer> property = groupProperty(group);
        if (property == null) thaumcraft.common.lib.world.FamilyVariant.pending().remove();
        else thaumcraft.common.lib.world.FamilyVariant.pending().set(property);
        return property;
    }

    private BlockCosmeticSolid(int group, Property<Integer> property, Properties properties) {
        super(properties);
        thaumcraft.common.lib.world.FamilyVariant.pending().remove();
        this.group = group;
        this.variants = EldritchBlockRules.cosmeticGroupVariants(group);
        this.variantProperty = property;
        if (this.variants.isEmpty()) throw new IllegalArgumentException("Unknown original cosmetic hardness group " + group);
        registerDefaultState(property == null ? stateDefinition.any()
                : stateDefinition.any().setValue(property, this.variants.get(0)));
    }

    /** The hardness group of this block. */
    public int group() { return group; }

    /** The original metadata values that this block hosts. */
    public List<Integer> variants() { return variants; }

    /** True when the block hosts the original metadata. */
    public boolean allows(int variant) { return variants.contains(variant); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        Property<?> pending = thaumcraft.common.lib.world.FamilyVariant.pending().get();
        if (pending != null) builder.add(pending);
    }

    /**
     * The original BlockCosmeticSolid is an ordinary block: the 1.7.10 class extends
     * {@code net.minecraft.block.Block} (OpenTC4 BlockCosmeticSolid:52) and it does not
     * override {@code getRenderType()}, so the vanilla render type zero draws its model.
     * The 1.21.1 port carries a block entity for the warding stone and for the node, and
     * {@code BaseEntityBlock} answers {@code RenderShape.INVISIBLE} for every block entity
     * block (Minecraft 1.21.1 BaseEntityBlock:22-25), which made the whole family invisible.
     * The stone is drawn as the model of the variant.
     */
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    /** The original metadata value of a state. */
    public static int variant(BlockState state) {
        Property<?> property = thaumcraft.common.lib.world.FamilyVariant.of(state);
        if (property != null) return (Integer) state.getValue(property);
        // A group with a single metadata value carries no property: the value is the one of the group.
        if (state.getBlock() instanceof BlockCosmeticSolid block) {
            java.util.List<Integer> variants = block.variants();
            if (variants != null && !variants.isEmpty()) return variants.getFirst();
        }
        return 0;
    }

    /**
     * The original metadata value of the state of a group, read without the block itself: the
     * light of the block is asked while the block is still built.
     */
    public static int variantOf(BlockState state, int group) {
        Property<?> property = thaumcraft.common.lib.world.FamilyVariant.of(state);
        if (property != null) return (Integer) state.getValue(property);
        java.util.List<Integer> variants = EldritchBlockRules.cosmeticGroupVariants(group);
        return variants.isEmpty() ? 0 : variants.getFirst();
    }

    /** The variant property of the block, or null when the group has a single metadata value. */
    public Property<Integer> variantProperty() { return variantProperty; }

    /** BlockCosmeticSolid:227-239 - the per metadata light level. */
    public static int lightOf(BlockState state) { return EldritchBlockRules.cosmeticLight(variant(state)); }

    /** BlockCosmeticSolid:278-296 - the warding stone and the node tile carry a block entity. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        int variant = variant(state);
        if (variant == EldritchBlockRules.COSMETIC_WARDING_STONE) {
            return new TileWardingStone(pos, state);
        }
        if (variant == EldritchBlockRules.COSMETIC_OBSIDIAN_TILE_SHADED) {
            return new TileNode(ThaumcraftEldritchBlocks.COSMETIC_NODE_TILE.get(), pos, state);
        }
        return null;
    }

    /**
     * BlockCosmeticSolid:259-265 - the original created the tile for the metadata three (the
     * warding stone) and for the metadata eight (the node) only, and answered
     * {@code hasTileEntity(int)} with exactly that. The port answers the question per state, so
     * the fourteen other stones of the family are moved by a piston like in the original.
     */
    @Override public boolean hasBlockEntityForState(BlockState state) {
        return EldritchBlockRules.cosmeticHasTile(variant(state));
    }

    /**
     * Oracle BlockCosmeticSolid:60-61 - the 1.7.10 class extended {@code
     * net.minecraft.block.Block} and did not implement {@code ITileEntityProvider}, so the piston
     * of the original moved the family for every metadata, the warding stone (the metadata three)
     * and the node (the metadata eight) included, and the move destroyed the tile of the two.
     *
     * <p>Minecraft 1.21.1 refuses every block entity block ({@code PistonBaseBlock#isPushable}
     * ends with {@code return !blockState.hasBlockEntity();}), so the port would refuse the whole
     * family; the mixin {@code thaumcraft.common.mixin.PistonPushabilityMixin} asks this answer
     * instead.
     */
    @Override public boolean isPushableInOriginal(BlockState state) {
        return OriginalPistonRule.Family.COSMETIC_SOLID.isPushableInOriginal(variant(state));
    }

    /**
     * Original TileWardingStone:31-56 - the warding stone pushes the entities out of itself and
     * seals the two blocks above it. The original ticked the tile on both sides, so the ticker
     * runs on the client as well.
     */
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (variant(state) == EldritchBlockRules.COSMETIC_OBSIDIAN_TILE_SHADED) {
            // Oracle BlockCosmeticSolid:288-296 - the metadata eight hosts the node of the
            // totem of the original, and the node of the original ticks there.
            return createTickerHelper(type, ThaumcraftEldritchBlocks.COSMETIC_NODE_TILE.get(),
                    TileNode::serverTick);
        }
        if (variant(state) != EldritchBlockRules.COSMETIC_WARDING_STONE) return null;
        return createTickerHelper(type, ThaumcraftEldritchBlocks.WARDING_STONE_TILE.get(),
                (lvl, pos, st, tile) -> { if (tile instanceof TileWardingStone stone) TileWardingStone.tick(lvl, pos, st, stone); });
    }

    /** BlockCosmeticSolid:360-370 - the golem stone toggles with the redstone power. */
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighbourPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighbourPos, movedByPiston);
        int variant = variant(state);
        if (variant == EldritchBlockRules.COSMETIC_GOLEM_STONE && level.hasNeighborSignal(pos)) {
            level.setBlock(pos, thaumcraft.common.lib.world.FamilyVariant.with(state, EldritchBlockRules.COSMETIC_GOLEM_STONE_ACTIVE), Block.UPDATE_ALL);
        } else if (variant == EldritchBlockRules.COSMETIC_GOLEM_STONE_ACTIVE && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, thaumcraft.common.lib.world.FamilyVariant.with(state, EldritchBlockRules.COSMETIC_GOLEM_STONE), Block.UPDATE_ALL);
        }
    }

    /**
     * BlockCosmeticSolid:263-276 - a living entity that walks over the paving stone of travel
     * gets the speed effect for forty ticks and the jump effect for forty ticks.
     */
    @Override public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (variant(state) != EldritchBlockRules.COSMETIC_TRAVEL_STONE) return;
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) return;
        living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
        living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.JUMP, 40, 0, false, false));
    }

    /** BlockCosmeticSolid:182-188 - the decorative stone is a full block. */
    @Override protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return true;
    }
}
