package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileOwned;

/**
 * Original BlockCosmeticOpaque: the amber block, the amber brick and the
 * warded glass. The warded glass variant carries the owned block entity.
 *
 * <p>Minecraft 1.21.1 binds the hardness and the blast resistance to the block and
 * not to the block state, so the warded glass (the original metadata two, which is
 * unbreakable and resists every explosion with 999) became a block of its own; the
 * amber block and the amber brick stay the metadata zero and one of this block.</p>
 */
public final class BlockCosmeticOpaque extends BaseEntityBlock implements StateDependentBlockEntity, PerStateBlockEntity, OriginalPistonPushable {
    public static final MapCodec<BlockCosmeticOpaque> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 1).fieldOf("group").forGetter(BlockCosmeticOpaque::group),
            propertiesCodec()).apply(instance, BlockCosmeticOpaque::new));
    /** Original BlockCosmeticOpaque metadata 0..2. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);

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
            java.util.List<Integer> variants = EldritchBlockRules.cosmeticOpaqueGroupVariants(key);
            // A group with a single metadata value has exactly one state, so it needs no property.
            if (variants.size() < 2) return null;
            return thaumcraft.common.lib.world.FamilyVariant.property(variants);
        });
    }

    /** The block of one group of the original metadata. */
    public static BlockCosmeticOpaque group(int group, Properties properties) {
        return new BlockCosmeticOpaque(group, pendingProperty(group), properties);
    }
    /** The amber block and the amber brick. */
    public static final int GROUP_AMBER = 0;
    /** The warded glass, the original metadata two. */
    public static final int GROUP_WARDED = 1;

    private final Property<Integer> variantProperty;
    private final int group;

    public BlockCosmeticOpaque(int group, Properties properties) {
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

    private BlockCosmeticOpaque(int group, Property<Integer> property, Properties properties) {
        super(properties);
        thaumcraft.common.lib.world.FamilyVariant.pending().remove();
        this.variantProperty = property;
        this.group = group;
        registerDefaultState(property == null ? stateDefinition.any()
                : stateDefinition.any().setValue(property,
                group == GROUP_WARDED ? EldritchBlockRules.COSMETIC_OPAQUE_OWNED : 0));
    }

    /** The hardness group of this block. */
    public int group() { return group; }

    /** True when this block is the warded glass. */
    public boolean wardedGlass() { return group == GROUP_WARDED; }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        Property<?> pending = thaumcraft.common.lib.world.FamilyVariant.pending().get();
        if (pending != null) builder.add(pending);
    }

    /**
     * The original BlockCosmeticOpaque is drawn by the renderer of the family: the 1.7.10
     * block answers the render id of the family (OpenTC4 BlockCosmeticOpaque:68-69) and the
     * client registers {@code BlockCosmeticOpaqueRenderer} for it (OpenTC4
     * {@code client/renderers/block/BlockCosmeticOpaqueRenderer.java:77}), so the amber block,
     * the amber brick and the warded glass are all visible.
     * {@code BaseEntityBlock} answers {@code RenderShape.INVISIBLE} for every block entity
     * block (Minecraft 1.21.1 BaseEntityBlock:22-25), so the port has to say it again.
     */
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    public static int variant(BlockState state) {
        Property<?> property = thaumcraft.common.lib.world.FamilyVariant.of(state);
        if (property != null) return (Integer) state.getValue(property);
        // A group with a single metadata value carries no property: the value is the one of the group.
        if (state.getBlock() instanceof BlockCosmeticOpaque block) {
            java.util.List<Integer> variants = block.variants();
            if (variants != null && !variants.isEmpty()) return variants.getFirst();
        }
        return 0;
    }

    /**
     * The original metadata value of the state of a group, read without the block itself: the
     * light of the block is asked while the block is still built.
     */
    public java.util.List<Integer> variants() { return EldritchBlockRules.cosmeticOpaqueGroupVariants(group()); }

    public static int variantOf(BlockState state, int group) {
        Property<?> property = thaumcraft.common.lib.world.FamilyVariant.of(state);
        if (property != null) return (Integer) state.getValue(property);
        java.util.List<Integer> variants = EldritchBlockRules.cosmeticOpaqueGroupVariants(group);
        return variants.isEmpty() ? 0 : variants.getFirst();
    }

    /** The variant property of the block, or null when the group has a single metadata value. */
    public Property<Integer> variantProperty() { return variantProperty; }

    /** BlockCosmeticOpaque - the amber block and brick let some light through. */
    @Override protected int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return variant(state) <= 1 ? 3 : 0;
    }

    /**
     * Original BlockCosmeticOpaque:253-259 - the warded glass is unbreakable while
     * the warded stone option is on and has a hardness of five otherwise; the amber
     * block and the amber brick keep the hardness of one and a half.
     *
     * <p>Minecraft 1.21.1 binds the destroy speed to the block state, so the
     * unbreakable state of the original is expressed as a mining progress of zero.</p>
     */
    @Override protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (wardedGlass() && ThaumcraftSettings.current().wardedStone()) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

    /** BlockCosmeticOpaque:216 - only the warded glass carries the owned block entity. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (!EldritchBlockRules.cosmeticOpaqueHasTile(variant(state))) return null;
        // The warded glass is the metadata two of this block, so the block owns the owned tile.
        return new TileOwned(ThaumcraftEldritchBlocks.OWNED_TILE.get(), pos, state);
    }

    /**
     * BlockCosmeticOpaque:190-192 and :221-223 - the original created the owned tile for the
     * metadata two only; the amber block and the amber brick got no tile at all, although the
     * 1.7.10 class is a {@code BlockContainer} and claims a tile for every metadata.
     * The port answers the question per state, so the amber block and the amber brick carry no
     * block entity.
     */
    @Override public boolean hasBlockEntityForState(BlockState state) {
        return EldritchBlockRules.cosmeticOpaqueHasTile(variant(state));
    }

    /**
     * Oracle BlockCosmeticOpaque:58-59 - the 1.7.10 class extended {@code
     * net.minecraft.block.BlockContainer}, which implements {@code ITileEntityProvider}, so the
     * piston of the original never moved the family: the amber block and the amber brick were
     * refused as well, although they carry no tile.
     *
     * <p>The question of the piston is a question of its own in 1.21.1 ({@code
     * PistonBaseBlock#isPushable} asks {@code !blockState.hasBlockEntity()}, and the answer of the
     * block entity contract is the answer for the creation of the block entity), so the answer of
     * the original is written down here and the mixin {@code
     * thaumcraft.common.mixin.PistonPushabilityMixin} asks it.
     */
    @Override public boolean isPushableInOriginal(BlockState state) {
        return OriginalPistonRule.Family.COSMETIC_OPAQUE.isPushableInOriginal(variant(state));
    }
}
