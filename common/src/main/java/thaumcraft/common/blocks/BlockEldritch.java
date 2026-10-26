package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.client.fx.RuneEffects;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.lib.world.OuterLandsWiringRules;
import thaumcraft.common.tiles.TileEldritchAltar;
import thaumcraft.common.tiles.TileEldritchCap;
import thaumcraft.common.tiles.TileEldritchCrabSpawner;
import thaumcraft.common.tiles.TileEldritchCrystal;
import thaumcraft.common.tiles.TileEldritchLock;
import thaumcraft.common.tiles.TileEldritchObelisk;
import thaumcraft.common.tiles.TileEldritchTrap;

/**
 * Original BlockEldritch: the eleven metadata variants of the eldritch stone
 * family. The original metadata became the {@link #VARIANT} block state
 * property; every number comes from {@link EldritchBlockRules}.
 */
public final class BlockEldritch extends BaseEntityBlock implements StateDependentBlockEntity, PerStateBlockEntity, OriginalPistonPushable {
    public static final MapCodec<BlockEldritch> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 5).fieldOf("group").forGetter(BlockEldritch::group),
            propertiesCodec()).apply(instance, BlockEldritch::new));
    /** Original BlockEldritch metadata 0..10. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 15);

    /**
     * The neighbour mask of the eldritch crust, {@code 1 << 6} values.
     *
     * <p>Oracle {@code BlockEldritchRenderer:35-63} insets a box side by two sixteenths unless the
     * neighbour on that side is a solid material, so a field of crust joins seamlessly while an
     * exposed face keeps the groove. The original asked the world during the draw; a 1.21.1
     * {@code BakedModel#getQuads} has neither the position nor the level, so the six flags have to
     * ride on the state. Bit {@code n} is set when the face {@code Direction.get3DDataValue() == n}
     * has a solid neighbour, which is exactly the condition under which the original draws no inset
     * on that side.</p>
     */
    public static final IntegerProperty CRUST_NEIGHBOR_MASK = IntegerProperty.create("crust_mask", 0, 63);

    /**
     * The variant property of every group of the original metadata.
     *
     * <p>Minecraft 1.21.1 binds the state space of a block to its property, so every group of the
     * original metadata became a block of its own and carries only the states of that group. The
     * property of a group therefore holds exactly the metadata of that group (they are not always consecutive), and it is chosen before the block
     * is built, because Minecraft builds the state definition inside the block constructor.
     */
    private static final java.util.Map<Integer, Property<Integer>> GROUP_PROPERTIES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final ThreadLocal<Integer> PENDING_GROUP = new ThreadLocal<>();

    private static Property<Integer> groupProperty(int group) {
        return GROUP_PROPERTIES.computeIfAbsent(group, key -> {
            java.util.List<Integer> variants = EldritchBlockRules.eldritchGroupVariants(key);
            // A group with a single metadata value has exactly one state, so it needs no property.
            if (variants.size() < 2) return null;
            return thaumcraft.common.lib.world.FamilyVariant.property(variants);
        });
    }
    /** The block of one group of the original metadata. */
    public static BlockEldritch group(int group, Properties properties) {
        return new BlockEldritch(group, pendingProperty(group), properties);
    }

    private final java.util.List<Integer> variants;
    private final Property<Integer> variantProperty;
    private final int group;

    /**
     * The hardness and the blast resistance belong to the block in 1.21.1, so
     * the original metadata families that differ become separate blocks.
     */
    public BlockEldritch(int group, Properties properties) {
        this(group, pendingProperty(group), properties);
    }

    /**
     * The property of the group, handed over before the block is built: Minecraft builds the state
     * definition of a block inside the block constructor, before the fields of the block exist.
     */
    private static Property<Integer> pendingProperty(int group) {
        PENDING_GROUP.set(group);
        Property<Integer> property = groupProperty(group);
        if (property == null) thaumcraft.common.lib.world.FamilyVariant.pending().remove();
        else thaumcraft.common.lib.world.FamilyVariant.pending().set(property);
        return property;
    }

    private BlockEldritch(int group, Property<Integer> property, Properties properties) {
        super(properties);
        PENDING_GROUP.remove();
        thaumcraft.common.lib.world.FamilyVariant.pending().remove();
        this.group = group;
        this.variants = EldritchBlockRules.eldritchGroupVariants(group);
        this.variantProperty = property;
        if (this.variants.isEmpty()) throw new IllegalArgumentException("Unknown original eldritch hardness group " + group);
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
        // State definitions run during super(), before this.group has been assigned.
        Integer pendingGroup = PENDING_GROUP.get();
        if (pendingGroup != null && hostsCrustMask(pendingGroup)) builder.add(CRUST_NEIGHBOR_MASK);
    }

    /** Original metadata 4, 5 AND 6 all share the neighbour-dependent inset. */
    private static boolean hostsCrustMask(int group) {
        return EldritchBlockRules.eldritchGroupVariants(group).stream().anyMatch(v -> v == 4 || v == 5 || v == 6);
    }

    /**
     * The six neighbour flags of the crust.
     *
     * <p>Oracle {@code BlockEldritchRenderer:35-63}: a side draws no inset when the block on that
     * side is a solid material. Bit {@code n} is set for the face with
     * {@code Direction.get3DDataValue() == n}.</p>
     */
    public static int crustMask(BlockGetter level, BlockPos pos) {
        int mask = 0;
        for (Direction face : Direction.values()) {
            BlockPos neighbour = pos.relative(face);
            // Block.isBlockSolid: the neighbour counts when its material is solid, which is what
            // the original passed to the renderer.
            if (BlockFluidPure.legacySolid(level.getBlockState(neighbour))) {
                mask |= 1 << face.get3DDataValue();
            }
        }
        return mask;
    }

    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
                                               net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.hasProperty(CRUST_NEIGHBOR_MASK)) {
            // The mask of the crust depends on one neighbour per face, so a change on any side
            // recomputes it. The original read the world at draw time and needed no update.
            int mask = crustMask(level, pos);
            if (state.getValue(CRUST_NEIGHBOR_MASK) != mask) state = state.setValue(CRUST_NEIGHBOR_MASK, mask);
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null && state.hasProperty(CRUST_NEIGHBOR_MASK)) {
            state = state.setValue(CRUST_NEIGHBOR_MASK, crustMask(context.getLevel(), context.getClickedPos()));
        }
        return state;
    }

    /**
     * The original BlockEldritch is drawn by the renderer of the family: the 1.7.10 block
     * answers the render id of the family (OpenTC4 BlockEldritch:140-141) and the client
     * registers {@code BlockEldritchRenderer} for it (OpenTC4
     * {@code client/renderers/block/BlockEldritchRenderer.java:86}), so the eldritch stone
     * itself is visible; only the parts that the tile renderer draws (the cap of the altar,
     * the lock, the crab spawner) are added by the block entity renderer of the port.
     * {@code BaseEntityBlock} answers {@code RenderShape.INVISIBLE} for every block entity
     * block (Minecraft 1.21.1 BaseEntityBlock:22-25), so the port has to say it again.
     */
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    /** The original metadata value of the state, read through the property of that state. */
    public static int variant(BlockState state) {
        Property<?> property = thaumcraft.common.lib.world.FamilyVariant.of(state);
        if (property != null) return (Integer) state.getValue(property);
        // A group with a single metadata value carries no property: the value is the one of the group.
        if (state.getBlock() instanceof BlockEldritch block) {
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
        java.util.List<Integer> variants = EldritchBlockRules.eldritchGroupVariants(group);
        return variants.isEmpty() ? 0 : variants.getFirst();
    }

    /** The variant property of the block, or null when the group has a single metadata value. */
    public Property<Integer> variantProperty() { return variantProperty; }

    /** BlockEldritch:120-135 - the per metadata light level. */
    public static int lightOf(BlockState state) { return EldritchBlockRules.eldritchLight(variant(state)); }

    /** BlockEldritch:189-214 - only the glowing crust, the glyphed variant and the crystal drop anything. */
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int variant = variant(state);
        if (EldritchBlockRules.eldritchDropsResource(variant)) {
            // BlockEldritch:209-211 - one knowledge fragment, never the block itself.
            return List.of(new ItemStack(ThaumcraftContent.MATERIALS.get("knowledge_fragment").get()));
        }
        if (EldritchBlockRules.eldritchDropsSelf(variant)) {
            return List.of(new ItemStack(this));
        }
        if (EldritchBlockRules.eldritchDropsBalancedShard(variant)) {
            // BlockCrystal:149-151 - the eldritch crystal drops one balanced shard.
            return List.of(new ItemStack(ThaumcraftContent.SHARDS.get("balanced").get()));
        }
        // BlockEldritch:190 - every other variant drops nothing.
        return List.of();
    }

    /** BlockEldritch:197-205 - the original experience amounts. */
    @Override protected void spawnAfterBreak(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (!dropExperience) return;
        int experience = getExpDrop(state, level, pos, null, null, tool);
        if (experience > 0) popExperience(level, pos, experience);
    }

    /**
     * Also the native NeoForge IBlockExtension hook, intentionally without @Override so common
     * compiles against vanilla (as {@link BlockCustomOre}). NeoForge awards block experience only
     * through this hook and calls spawnAfterBreak(..., false); Fabric calls spawnAfterBreak(..., true).
     */
    public int getExpDrop(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos,
                          BlockEntity blockEntity, net.minecraft.world.entity.Entity breaker, ItemStack tool) {
        return EldritchBlockRules.eldritchExperience(variant(state), new java.util.Random());
    }

    /** BlockEldritch:155-175 - the six original block entities. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return createTile(pos, state); }

    /**
     * BlockEldritch:120-122 - the original created the tile for the metadata zero (the altar),
     * one (the obelisk), three (the cap), eight (the lock), nine (the crab spawner) and ten
     * (the runed trap) only, and answered {@code hasTileEntity(int)} with exactly that. The
     * port answers the question per state, so the stones of the family that never had a tile
     * carry no block entity.
     */
    @Override public boolean hasBlockEntityForState(BlockState state) {
        return EldritchBlockRules.eldritchHasTile(variant(state));
    }

    /**
     * Oracle BlockEldritch:60-61 - the 1.7.10 class extended {@code
     * net.minecraft.block.BlockContainer}, which implements {@code ITileEntityProvider}, so the
     * piston of the original never moved the family: the stones that never had a tile were
     * refused as well.
     *
     * <p>The question of the piston is a question of its own in 1.21.1 ({@code
     * PistonBaseBlock#isPushable} asks {@code !blockState.hasBlockEntity()}, and the answer of the
     * block entity contract is the answer for the creation of the block entity), so the answer of
     * the original is written down here and the mixin {@code
     * thaumcraft.common.mixin.PistonPushabilityMixin} asks it.
     */
    @Override public boolean isPushableInOriginal(BlockState state) {
        return OriginalPistonRule.Family.ELDRITCH.isPushableInOriginal(variant(state));
    }

    /** The dispatching factory of the shared eldritch block entity type. */
    public static BlockEntity createTile(BlockPos pos, BlockState state) {
        int variant = variant(state);
        if (!EldritchBlockRules.eldritchHasTile(variant)) return null;
        return switch (variant) {
            case EldritchBlockRules.ELDRITCH_ALTAR -> new TileEldritchAltar(pos, state);
            case EldritchBlockRules.ELDRITCH_OBELISK -> new TileEldritchObelisk(pos, state);
            case EldritchBlockRules.ELDRITCH_CAPSTONE -> new TileEldritchCap(pos, state);
            case EldritchBlockRules.ELDRITCH_LOCK -> new TileEldritchLock(pos, state);
            case EldritchBlockRules.ELDRITCH_CRAB_SPAWNER -> new TileEldritchCrabSpawner(pos, state);
            case EldritchBlockRules.ELDRITCH_RUNED -> new TileEldritchTrap(pos, state);
            case EldritchBlockRules.ELDRITCH_CRYSTAL -> new TileEldritchCrystal(pos, state);
            default -> null;
        };
    }

    /**
     * The original random update of the runed eldritch block (oracle
     * {@code thaumcraft/common/blocks/BlockEldritch.java:302-304}): the rune floats in one of the
     * neighbouring air blocks. The 1.21.1 client equivalent of the random update is the client tick
     * of the block, with the same average rate of the original.
     */
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (variant(state) == EldritchBlockRules.ELDRITCH_LOCK) {
            // Original BlockEldritch:289-302 - the counting eldritch lock throws a spark of the
            // size 0.5 with the colour 0.65+random*0.1/1/1 and the alpha 0.8.
            if (!(level instanceof net.minecraft.client.multiplayer.ClientLevel client)) return;
            if (!(level.getBlockEntity(pos) instanceof thaumcraft.common.tiles.TileEldritchLock lock) || lock.count < 0) return;
            thaumcraft.client.fx.particles.ShockSparkParticle.spark(client,
                    pos.getX() + client.random.nextFloat(), pos.getY() + client.random.nextFloat(),
                    pos.getZ() + client.random.nextFloat(), .5F, .65F + client.random.nextFloat() * .1F, 1F, 1F, .8F);
            return;
        }
        if (variant(state) != EldritchBlockRules.ELDRITCH_RUNED) return;
        if (!(level instanceof net.minecraft.client.multiplayer.ClientLevel client)) return;
        if (!RuneEffects.randomUpdate(random)) return;
        RuneEffects.eldritchRune(client, pos);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ThaumcraftEldritchBlocks.ELDRITCH_TILE.get(),
                (lvl, pos, st, tile) -> {
                    if (tile instanceof TileEldritchAltar altar) TileEldritchAltar.tick(lvl, pos, st, altar);
                    else if (tile instanceof TileEldritchObelisk obelisk) TileEldritchObelisk.tick(lvl, pos, st, obelisk);
                    else if (tile instanceof TileEldritchCrabSpawner spawner) TileEldritchCrabSpawner.tick(lvl, pos, st, spawner);
                    else if (tile instanceof TileEldritchLock lock) TileEldritchLock.tick(lvl, pos, st, lock);
                    else if (tile instanceof TileEldritchTrap trap) TileEldritchTrap.tick(lvl, pos, st, trap);
                });
    }

    /** BlockEldritch:262-286 - the altar eye and the lock key interactions. */
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int variant = variant(state);
        if (variant == EldritchBlockRules.ELDRITCH_ALTAR) {
            ItemStack held = player.getItemInHand(player.getUsedItemHand());
            if (level.isClientSide || player.isShiftKeyDown() || held.isEmpty()) return InteractionResult.PASS;
            if (!isEldritchEye(held)) return InteractionResult.PASS;
            if (!(level.getBlockEntity(pos) instanceof TileEldritchAltar altar)) return InteractionResult.PASS;
            if (!EldritchBlockRules.altarAcceptsEye(altar.getEyes())) return InteractionResult.PASS;
            altar.setSpawner(EldritchBlockRules.altarSpawnTypeForEyes(altar.getEyes()) == EldritchBlockRules.ALTAR_SPAWN_TYPE_GUARDIAN);
            altar.setSpawnType(EldritchBlockRules.altarSpawnTypeForEyes(altar.getEyes()));
            altar.setEyes((byte) EldritchBlockRules.altarEyesAfterActivation(altar.getEyes()));
            altar.checkForMaze();
            if (!player.getAbilities().instabuild) held.shrink(1);
            altar.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, .2F, 1.0F);
            return InteractionResult.CONSUME;
        }
        if (variant == EldritchBlockRules.ELDRITCH_LOCK) {
            ItemStack held = player.getItemInHand(player.getUsedItemHand());
            if (level.isClientSide || held.isEmpty()) return InteractionResult.PASS;
            if (!isEldritchLockKey(held)) return InteractionResult.PASS;
            if (!(level.getBlockEntity(pos) instanceof TileEldritchLock lock)) return InteractionResult.PASS;
            if (lock.count >= 0) return InteractionResult.PASS;
            lock.count = 0;
            lock.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            if (!player.getAbilities().instabuild) held.shrink(1);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** Original ItemEldritchObject metadata zero, flattened to the eldritch eye. */
    public static boolean isEldritchEye(ItemStack stack) {
        return stack.is(ThaumcraftWarp.ELDRITCH_EYE.get());
    }

    /** Original ItemEldritchObject metadata two, the eldritch lock key. */
    public static boolean isEldritchLockKey(ItemStack stack) {
        // Oracle BlockEldritch:278 - the lock accepts the eldritch object with the metadata two.
        // The original carried the five metadata values on one item; the 1.21.1 port flattens
        // them into one item each, so the key is the runed tablet.
        ResourceLocation key = ResourceLocation.parse(OuterLandsWiringRules.eldritchObjectItem(
                EldritchBlockRules.ELDRITCH_OBJECT_TABLET));
        ResourceLocation held = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.equals(held);
    }

    /** BlockEldritch:216-229 - breaking a structural piece removes the whole altar. */
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && EldritchBlockRules.eldritchBreaksStructure(variant(state))) {
            int rx = EldritchBlockRules.eldritchStructureRadiusX();
            int ry = EldritchBlockRules.eldritchStructureRadiusY();
            List<BlockPos> doomed = new ArrayList<>();
            for (int x = pos.getX() - rx; x <= pos.getX() + rx; x++) {
                for (int y = pos.getY() - ry; y <= pos.getY() + ry; y++) {
                    for (int z = pos.getZ() - rx; z <= pos.getZ() + rx; z++) {
                        BlockPos target = new BlockPos(x, y, z);
                        if (target.equals(pos)) continue;
                        BlockState targetState = level.getBlockState(target);
                        if (targetState.getBlock() == this
                                && EldritchBlockRules.eldritchStructureMember(variant(targetState))) {
                            doomed.add(target);
                        }
                    }
                }
            }
            for (BlockPos target : doomed) level.destroyBlock(target, false);
            level.explode(null, pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D, 1.0F, Level.ExplosionInteraction.NONE);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
