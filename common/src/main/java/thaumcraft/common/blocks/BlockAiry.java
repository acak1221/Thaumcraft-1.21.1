package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileWardingStoneFence;

/**
 * Original BlockAiry: the airy family. This port owns the warding stone
 * fence; the node, the nitor and the energised node of the original family
 * already exist as their own native blocks.
 */
public final class BlockAiry extends BaseEntityBlock implements StateDependentBlockEntity, PerStateBlockEntity, OriginalPistonPushable {
    public static final MapCodec<BlockAiry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 3).fieldOf("group").forGetter(BlockAiry::group),
            propertiesCodec()).apply(instance, BlockAiry::new));
    /** Original BlockAiry metadata 0..12. */
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
            java.util.List<Integer> variants = EldritchBlockRules.airyGroupVariants(key);
            // A group with a single metadata value has exactly one state, so it needs no property.
            if (variants.size() < 2) return null;
            return thaumcraft.common.lib.world.FamilyVariant.property(variants);
        });
    }
    /** The block of one group of the original metadata. */
    public static BlockAiry group(int group, Properties properties) {
        return new BlockAiry(group, pendingProperty(group), properties);
    }

    private final java.util.List<Integer> variants;
    private final Property<Integer> variantProperty;
    private final int group;

    /** The hardness and the blast resistance belong to the block in 1.21.1. */
    public BlockAiry(int group, Properties properties) {
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

    private BlockAiry(int group, Property<Integer> property, Properties properties) {
        super(properties);
        thaumcraft.common.lib.world.FamilyVariant.pending().remove();
        this.group = group;
        this.variants = EldritchBlockRules.airyGroupVariants(group);
        this.variantProperty = property;
        if (this.variants.isEmpty()) throw new IllegalArgumentException("Unknown original airy hardness group " + group);
        registerDefaultState(property == null ? stateDefinition.any()
                : stateDefinition.any().setValue(property, this.variants.get(0)));
    }

    /** The hardness group of this block. */
    public int group() { return group; }

    /** The original metadata values that this block hosts. */
    public java.util.List<Integer> variants() { return variants; }

    /** True when the block hosts the original metadata. */
    public boolean allows(int variant) { return variants.contains(variant); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        Property<?> pending = thaumcraft.common.lib.world.FamilyVariant.pending().get();
        if (pending != null) builder.add(pending);
    }

    /**
     * The original BlockAiry is not drawn as a block: the 1.7.10 block answers the render type
     * minus one (OpenTC4 BlockAiry:196-197) and the family is the invisible filler of the aura
     * nodes. The port keeps the invisible answer on purpose, and the state that carries the
     * warding fence is drawn by its own tile.
     */
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    public static int variant(BlockState state) {
        Property<?> property = thaumcraft.common.lib.world.FamilyVariant.of(state);
        if (property != null) return (Integer) state.getValue(property);
        // A group with a single metadata value carries no property: the value is the one of the group.
        if (state.getBlock() instanceof BlockAiry block) {
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
        java.util.List<Integer> variants = EldritchBlockRules.airyGroupVariants(group);
        return variants.isEmpty() ? 0 : variants.getFirst();
    }

    /** The variant property of the block, or null when the group has a single metadata value. */
    public Property<Integer> variantProperty() { return variantProperty; }

    /** BlockAiry:216-230 - the per metadata light level. */
    public static int lightOf(BlockState state) { return EldritchBlockRules.airyLight(variant(state)); }

    /** BlockAiry:206-213 - the replaceable metadata values. */
    @Override protected boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        return EldritchBlockRules.airyReplaceable(variant(state));
    }

    /** BlockAiry:263-265 - the leaves metadata values are treated as leaves. */
    @Override protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return !EldritchBlockRules.airyIsLeaves(variant(state));
    }

    /** BlockAiry:272-280 - most airy variants have no selection box at all. */
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        switch (variant(state)) {
            case EldritchBlockRules.AIRY_NODE:
            case EldritchBlockRules.AIRY_LEAVES:
            case EldritchBlockRules.AIRY_LEAVES_ALT:
            case EldritchBlockRules.AIRY_WARDING_FENCE:
            case EldritchBlockRules.AIRY_ENERGIZED_NODE:
            case EldritchBlockRules.AIRY_MAGIC_HOLE:
            case EldritchBlockRules.AIRY_VOID_HOLE:
            case EldritchBlockRules.AIRY_UNBREAKABLE:
                return Shapes.empty();
            default:
                return Shapes.block();
        }
    }

    /** Original BlockAiry:393-405 - the warden's field, not an inert visual effect. */
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (variant(state) != 11 || entity instanceof thaumcraft.api.entities.IEldritchMob) return;
        if (!level.isClientSide && level.random.nextInt(100) == 0)
            entity.hurt(level.damageSources().wither(), 1F);
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(.66D, 1D, .66D));
        if (!level.isClientSide) {
            if (entity instanceof net.minecraft.world.entity.player.Player player) player.causeFoodExhaustion(.05F);
            if (entity instanceof net.minecraft.world.entity.LivingEntity living)
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 1, true, true));
        }
    }

    /** Original BlockAiry:409-414 - both scheduled and random ticks retire the field. */
    @Override protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos,
                                  net.minecraft.util.RandomSource random) {
        if (variant(state) == 11) level.removeBlock(pos, false);
    }

    @Override protected void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos,
                                        net.minecraft.util.RandomSource random) {
        tick(state, level, pos, random);
    }

    /** BlockAiry:96-100 - the warding fence carries the warding stone block entity. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (variant(state) != EldritchBlockRules.AIRY_WARDING_FENCE) return null;
        return new TileWardingStoneFence(pos, state);
    }

    /**
     * BlockAiry:275-289 - the original created a tile for the metadata zero (the node), one
     * (the nitor) and five (the energised node) and no tile for the other metadata, although
     * the 1.7.10 class is a {@code BlockContainer} and claims a tile for every metadata. The
     * port owns the warding fence only, the node, the nitor and the energised node are native
     * blocks of their own, so the port answers the question per state: only the fence has a
     * tile.
     */
    @Override public boolean hasBlockEntityForState(BlockState state) {        return variant(state) == EldritchBlockRules.AIRY_WARDING_FENCE;
    }

    /**
     * Oracle BlockAiry:299-311 - the client tick of the airy family.
     *
     * <p>The metadata ten and the metadata eleven throw a spark of the size {@code 0.33 + h} with
     * {@code h = random * 0.33} at the height {@code 0.1515 + h / 2} over the block: the metadata
     * ten is the white spark of the original with the colour {@code 0.65 + random * 0.1 / 1 / 1} and
     * the alpha {@code 0.8}, the metadata eleven is the blue spark with the colour
     * {@code 0.3 - random * 0.1 / 0 / 0.5 + random * 0.2}. One tick in fifty plays the original
     * "jacobs" sound.</p>
     */
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        super.animateTick(state, level, pos, random);
        int metadata = variant(state);
        if (metadata != 10 && metadata != 11) return;
        if (!(level instanceof net.minecraft.client.multiplayer.ClientLevel client)) return;
        float h = random.nextFloat() * .33F;
        double x = pos.getX() + client.random.nextFloat();
        double y = pos.getY() + .1515F + h / 2F;
        double z = pos.getZ() + client.random.nextFloat();
        float size = .33F + h;
        if (metadata == 10) {
            thaumcraft.client.fx.particles.ShockSparkParticle.spark(client, x, y, z, size,
                    .65F + client.random.nextFloat() * .1F, 1F, 1F, .8F);
        } else {
            thaumcraft.client.fx.particles.ShockSparkParticle.spark(client, x, y, z, size,
                    .3F - client.random.nextFloat() * .1F, 0F, .5F + client.random.nextFloat() * .2F, 1F);
        }
        if (random.nextInt(50) == 0) {
            client.playLocalSound(pos.getX(), pos.getY(), pos.getZ(),
                    net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "jacobs")),
                    net.minecraft.sounds.SoundSource.BLOCKS, .5F,
                    1F + (client.random.nextFloat() - client.random.nextFloat()) * .2F, false);
        }
    }

    /**
     * Oracle BlockAiry:77-78 - the 1.7.10 class extended {@code
     * net.minecraft.block.BlockContainer}, which implements {@code ITileEntityProvider}, so the
     * piston of the original never moved the family: the variants that never had a tile were
     * refused as well.
     *
     * <p>The question of the piston is a question of its own in 1.21.1 ({@code
     * PistonBaseBlock#isPushable} asks {@code !blockState.hasBlockEntity()}, and the answer of the
     * block entity contract is the answer for the creation of the block entity), so the answer of
     * the original is written down here and the mixin {@code
     * thaumcraft.common.mixin.PistonPushabilityMixin} asks it.
     */
    @Override public boolean isPushableInOriginal(BlockState state) {
        return OriginalPistonRule.Family.AIRY.isPushableInOriginal(variant(state));
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, tile) -> {
                    if (tile instanceof TileWardingStoneFence fence) TileWardingStoneFence.tick(lvl, pos, st, fence);
                };
    }
}
