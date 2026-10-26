package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.lib.world.BiomeColumns;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileEtherealBloom: the ethereal bloom. It turns the tainted
 * columns around it back into the overworld.
 */
public final class TileEtherealBloom extends BlockEntity {
    public int counter = 0;
    public int growthCounter = 0;

    public TileEtherealBloom(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.ETHEREAL_BLOOM_TILE.get(), pos, state);
    }

    /** The overworld biome a column generated as taint is turned into (oracle BiomeGenBase.plains). */
    public static final ResourceKey<Biome> OVERWORLD_PLAINS = ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("plains"));
    private static final SoundEvent ROOTS = SoundEvent.createVariableRangeEvent(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "roots"));

    /** Original TileEtherealBloom.updateEntity (bytecode 0-381), on both sides. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileEtherealBloom bloom) {
        if (bloom.counter == 0) bloom.counter = level.random.nextInt(EldritchBlockRules.ETHEREAL_BLOOM_START_PHASE);
        bloom.counter++;
        if (level instanceof ServerLevel server && bloom.counter % EldritchBlockRules.ETHEREAL_BLOOM_CONVERT_PERIOD == 0) {
            int x = server.random.nextInt(EldritchBlockRules.ETHEREAL_BLOOM_OFFSET) - server.random.nextInt(EldritchBlockRules.ETHEREAL_BLOOM_OFFSET);
            int z = server.random.nextInt(EldritchBlockRules.ETHEREAL_BLOOM_OFFSET) - server.random.nextInt(EldritchBlockRules.ETHEREAL_BLOOM_OFFSET);
            cleanse(server, pos, x, z);
        }
        // Bytecode 314-368: the roots sound of a bloom the client sees for the first time.
        if (level.isClientSide && bloom.growthCounter == 0)
            level.playLocalSound(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D, ROOTS, SoundSource.BLOCKS, 1.0F, .6F, false);
        bloom.growthCounter++;
    }

    /**
     * Bytecode 108-311: a taint, eerie or magical forest column within reach gets back the biome the
     * world generated there, plains for a column generated as taint. A column the taint or a pure
     * node turned into one of the three biomes is restored; a generated one is left as it is.
     */
    public static boolean cleanse(ServerLevel server, BlockPos pos, int x, int z) {
        int targetX = pos.getX() + x, targetZ = pos.getZ() + z;
        var holder = BiomeColumns.surfaceHolder(server, targetX, targetZ);
        if (holder == null) return false;
        if (!EldritchBlockRules.etherealBloomConverts(holder.is(ThaumcraftBiomes.TAINT), holder.is(ThaumcraftBiomes.EERIE),
                holder.is(ThaumcraftBiomes.MAGICAL_FOREST))) return false;
        if (!EldritchBlockRules.etherealBloomReaches(x, z)) return false;
        return BiomeColumns.restoreGenerated(server, targetX, targetZ, ThaumcraftBiomes.TAINT, OVERWORLD_PLAINS);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        counter = tag.getInt("counter");
        growthCounter = tag.getInt("growthCounter");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("counter", counter);
        tag.putInt("growthCounter", growthCounter);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
