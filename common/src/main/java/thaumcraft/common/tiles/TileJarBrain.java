package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/**
 * Original TileJarBrain: the brain jar. It swallows the experience orbs that
 * come close to it and keeps them.
 */
public final class TileJarBrain extends BlockEntity {
    /** Original TileJarBrain:24 - the stored experience. */
    public int xp = 0;
    /** Original TileJarBrain:25 - the jar never keeps more than two thousand. */
    public int xpMax = 2000;
    public int eatDelay = 0;
    public float rota;
    public float rotb;
    public float headYaw;
    public float headPitch;
    public float headRoll;
    public float lookYaw;

    public TileJarBrain(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.JAR_BRAIN_TILE.get(), pos, state);
    }

    public int getXp() { return xp; }
    public int getXpMax() { return xpMax; }

    /** Original TileJarBrain:31-79 - the jar pulls the experience orbs to itself. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileJarBrain jar) {
        if (level.isClientSide) return;
        if (jar.xp > jar.xpMax) jar.xp = jar.xpMax;
        if (jar.xp >= jar.xpMax) return;
        if (jar.eatDelay > 0) {
            jar.eatDelay--;
            return;
        }
        double cx = pos.getX() + .5D;
        double cy = pos.getY() + .5D;
        double cz = pos.getZ() + .5D;
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class,
                new net.minecraft.world.phys.AABB(cx - 7.0D, cy - 7.0D, cz - 7.0D, cx + 7.0D, cy + 7.0D, cz + 7.0D))) {
            double dx = cx - orb.getX();
            double dy = cy - orb.getY();
            double dz = cz - orb.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance <= 0.0D) continue;
            double pull = 1.0D - distance / 7.0D;
            pull *= pull;
            orb.setDeltaMovement(orb.getDeltaMovement().add(dx / distance * pull * .15D,
                    dy / distance * pull * .33D, dz / distance * pull * .15D));
            if (distance < 1.0D) {
                jar.xp = Math.min(jar.xpMax, jar.xp + orb.getValue());
                orb.discard();
                jar.setChanged();
                jar.eatDelay = 5;
            }
        }
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        xp = tag.getInt("XP");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("XP", xp);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
