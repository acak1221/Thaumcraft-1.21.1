package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/**
 * Original TileJarBrain: the brain jar. It swallows the experience orbs that
 * come close to it and keeps them.
 *
 * <p>Oracle TileJarBrain.java:53-113 - the jar pulls the CLOSEST orb of the
 * six blocks around it towards its middle with the strength
 * {@code (1 - distance/7)^2}, and it swallows every orb that lies inside its
 * own block. The stored experience is capped at two thousand at the start of
 * the next tick; the block that empties the jar sets the eat delay to forty.</p>
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
    /** Original TileJarBrain:26 - the next sigh of the brain jar. */
    public long lastSigh = System.currentTimeMillis() + 1500L;
    /** The original brain sigh. */
    private static final net.minecraft.sounds.SoundEvent BRAIN = net.minecraft.sounds.SoundEvent
            .createVariableRangeEvent(net.minecraft.resources.ResourceLocation
                    .fromNamespaceAndPath("thaumcraft", "brain"));

    public TileJarBrain(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.JAR_BRAIN_TILE.get(), pos, state);
    }

    public int getXp() { return xp; }
    public int getXpMax() { return xpMax; }

    /** Original TileJarBrain:31-79 - the jar pulls the closest orb to itself. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileJarBrain jar) {
        if (level.isClientSide) {
            clientTick(level, pos, jar);
            return;
        }
        if (jar.xp > jar.xpMax) jar.xp = jar.xpMax;
        ExperienceOrb closest = closestOrb(level, pos);
        if (jar.xp < jar.xpMax && closest != null && jar.eatDelay == 0) {
            double dx = (pos.getX() + .5D - closest.getX()) / 7.0D;
            double dy = (pos.getY() + .5D - closest.getY()) / 7.0D;
            double dz = (pos.getZ() + .5D - closest.getZ()) / 7.0D;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double pull = 1.0D - length;
            if (pull > 0.0D) {
                pull *= pull;
                closest.setDeltaMovement(closest.getDeltaMovement().add(
                        dx / length * pull * .15D, dy / length * pull * .33D, dz / length * pull * .15D));
            }
        }
        if (jar.eatDelay > 0) {
            jar.eatDelay--;
            return;
        }
        if (jar.xp >= jar.xpMax) return;
        // Oracle TileJarBrain:107-113 - every orb inside the block is swallowed.
        boolean eaten = false;
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class,
                new AABB(pos.getX() - .1D, pos.getY() - .1D, pos.getZ() - .1D,
                        pos.getX() + 1.1D, pos.getY() + 1.1D, pos.getZ() + 1.1D))) {
            jar.xp += orb.getValue();
            level.playSound(null, orb.getX(), orb.getY(), orb.getZ(), SoundEvents.GENERIC_EAT,
                    SoundSource.BLOCKS, .1F, (level.random.nextFloat() - level.random.nextFloat()) * .2F + 1.0F);
            orb.discard();
            eaten = true;
        }
        if (eaten) {
            jar.setChanged();
            level.sendBlockUpdated(pos, jar.getBlockState(), jar.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Oracle TileJarBrain:114-130 - the closest orb of the six blocks around the jar. */
    private static ExperienceOrb closestOrb(Level level, BlockPos pos) {        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).inflate(6, 6, 6);
        ExperienceOrb closest = null;
        double best = Double.MAX_VALUE;
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box)) {
            double dx = pos.getX() + .5D - orb.getX();
            double dy = pos.getY() + .5D - orb.getY();
            double dz = pos.getZ() + .5D - orb.getZ();
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance >= best) continue;
            closest = orb;
            best = distance;
        }
        return closest;
    }

    /** Oracle TileJarBrain:64-97 - the client side head turn and the periodic sigh of the brain jar. */
    private static void clientTick(Level level, BlockPos pos, TileJarBrain jar) {
        jar.rotb = jar.rota;
        // The original looks at the closest experience orb first and only at a player when there is none.
        ExperienceOrb orb = jar.xp < jar.xpMax ? closestOrb(level, pos) : null;
        net.minecraft.world.entity.Entity look = orb;
        if (look == null) {
            Player player = level.getNearestPlayer(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D, 6.0D, false);
            if (player != null && jar.lastSigh < System.currentTimeMillis()) {
                level.playSound(null, pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D, BRAIN,
                        SoundSource.BLOCKS, .15F, .8F + level.random.nextFloat() * .4F);
                jar.lastSigh = System.currentTimeMillis() + 5000L + (long) level.random.nextInt(25000);
            }
            look = player;
        }
        if (look != null) {
            double dx = look.getX() - (pos.getX() + .5D);
            double dz = look.getZ() - (pos.getZ() + .5D);
            jar.lookYaw = (float) Math.atan2(dz, dx);
            jar.headPitch += .1F;
            if (jar.headPitch < .5F || level.random.nextInt(40) == 0) {
                float previous = jar.headRoll;
                do {
                    jar.headRoll += (level.random.nextInt(4) - level.random.nextInt(4));
                } while (previous == jar.headRoll);
            }
        } else {
            jar.lookYaw += .01F;
        }
        while (jar.rota >= 3.141593F) jar.rota -= 6.283185F;
        while (jar.rota < -3.141593F) jar.rota += 6.283185F;
        while (jar.lookYaw >= 3.141593F) jar.lookYaw -= 6.283185F;
        while (jar.lookYaw < -3.141593F) jar.lookYaw += 6.283185F;
        float delta = jar.lookYaw - jar.rota;
        while (delta >= 3.141593F) delta -= 6.283185F;
        while (delta < -3.141593F) delta += 6.283185F;
        jar.rota += delta * .04F;
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
