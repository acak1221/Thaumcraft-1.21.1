package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
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
public final class BlockEldritch extends BaseEntityBlock {
    public static final MapCodec<BlockEldritch> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 5).fieldOf("group").forGetter(BlockEldritch::group),
            propertiesCodec()).apply(instance, BlockEldritch::new));
    /** Original BlockEldritch metadata 0..10. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 15);

    private final java.util.List<Integer> variants;
    private final int group;

    /**
     * The hardness and the blast resistance belong to the block in 1.21.1, so
     * the original metadata families that differ become separate blocks.
     */
    public BlockEldritch(int group, Properties properties) {
        super(properties);
        this.group = group;
        this.variants = EldritchBlockRules.eldritchGroupVariants(group);
        if (this.variants.isEmpty()) throw new IllegalArgumentException("Unknown original eldritch hardness group " + group);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, this.variants.get(0)));
    }

    /** The hardness group of this block. */
    public int group() { return group; }

    /** The original metadata values that this block hosts. */
    public java.util.List<Integer> variants() { return variants; }

    /** True when the block hosts the original metadata. */
    public boolean allows(int variant) { return variants.contains(variant); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }

    public static int variant(BlockState state) { return state.getValue(VARIANT); }

    /** BlockEldritch:120-135 - the per metadata light level. */
    public static int lightOf(BlockState state) { return EldritchBlockRules.eldritchLight(state.getValue(VARIANT)); }

    /** BlockEldritch:189-214 - only the glowing crust and the glyphed variant drop anything. */
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int variant = state.getValue(VARIANT);
        if (EldritchBlockRules.eldritchDropsResource(variant)) {
            // BlockEldritch:209-211 - one knowledge fragment, never the block itself.
            return List.of(new ItemStack(ThaumcraftContent.MATERIALS.get("knowledge_fragment").get()));
        }
        if (EldritchBlockRules.eldritchDropsSelf(variant)) {
            return List.of(new ItemStack(this));
        }
        // BlockEldritch:190 - every other variant drops nothing.
        return List.of();
    }

    /** BlockEldritch:197-205 - the original experience amounts. */
    @Override protected void spawnAfterBreak(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (!dropExperience) return;
        int experience = EldritchBlockRules.eldritchExperience(state.getValue(VARIANT), new java.util.Random());
        if (experience > 0) popExperience(level, pos, experience);
    }

    /** BlockEldritch:155-175 - the six original block entities. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return createTile(pos, state); }

    /** The dispatching factory of the shared eldritch block entity type. */
    public static BlockEntity createTile(BlockPos pos, BlockState state) {
        int variant = state.getValue(VARIANT);
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
        int variant = state.getValue(VARIANT);
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
        if (!level.isClientSide && EldritchBlockRules.eldritchBreaksStructure(state.getValue(VARIANT))) {
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
                                && EldritchBlockRules.eldritchStructureMember(targetState.getValue(VARIANT))) {
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
