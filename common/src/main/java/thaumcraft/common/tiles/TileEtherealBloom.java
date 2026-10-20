package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
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

    /** The overworld biome the tainted column is turned into. */
    public static final ResourceKey<Biome> OVERWORLD_PLAINS = ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("plains"));

    /** Original TileEtherealBloom:22-49 - the bloom cleans the tainted columns. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileEtherealBloom bloom) {
        bloom.counter++;
        bloom.growthCounter++;
        if (level.isClientSide || !(level instanceof ServerLevel server)) return;
        if (bloom.counter % EldritchBlockRules.ETHEREAL_BLOOM_CONVERT_PERIOD != 0) return;
        int x = server.getRandom().nextInt(EldritchBlockRules.ETHEREAL_BLOOM_OFFSET * 2 + 1)
                - EldritchBlockRules.ETHEREAL_BLOOM_OFFSET;
        int z = server.getRandom().nextInt(EldritchBlockRules.ETHEREAL_BLOOM_OFFSET * 2 + 1)
                - EldritchBlockRules.ETHEREAL_BLOOM_OFFSET;
        int targetX = pos.getX() + x;
        int targetZ = pos.getZ() + z;
        var holder = BiomeColumns.surfaceHolder(server, targetX, targetZ);
        if (holder == null) return;
        ResourceKey<Biome> key = holder.unwrapKey().orElse(null);
        if (key == null) return;
        if (!EldritchBlockRules.etherealBloomConverts(
                key.equals(ThaumcraftBiomes.TAINT), key.equals(ThaumcraftBiomes.EERIE), key.equals(ThaumcraftBiomes.MAGICAL_FOREST))) {
            return;
        }
        double distance = Math.sqrt(Math.pow(targetX + .5D - pos.getX(), 2.0D)
                + Math.pow(targetZ + .5D - pos.getZ(), 2.0D));
        if (distance * distance > EldritchBlockRules.ETHEREAL_BLOOM_RADIUS_SQUARED) return;
        if (!key.equals(ThaumcraftBiomes.TAINT)) return;
        BiomeColumns.set(server, targetX, targetZ, OVERWORLD_PLAINS);
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
