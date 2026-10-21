package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.BlockPos;
import thaumcraft.client.fx.particles.BlockRuneParticle;

/**
 * The rune particles of the original TC4 1.7.10 client.
 *
 * <p>The original spawns them from the random update of the blocks (oracle
 * {@code thaumcraft/common/blocks/BlockCosmeticSolid.java:325-355} for the warding stone and
 * {@code thaumcraft/common/blocks/BlockEldritch.java:304} for the runed eldritch block) through the
 * client proxy helper {@code Thaumcraft.proxy.blockRunes}, which creates the particle
 * {@code FXBlockRunes} of the oracle {@code thaumcraft/client/fx/particles/FXBlockRunes.java}.</p>
 *
 * <p>The 1.7.10 random update runs three times per chunk and per tick, so a block is updated once
 * every 4096 / 3 ticks on average; the native port uses the same probability in its client tick.</p>
 */
public final class RuneEffects {
    /** The number of random updates of a chunk and of a tick of the original. */
    public static final int RANDOM_TICKS = 3;
    /** The number of blocks of a chunk of the original. */
    public static final int CHUNK_SIZE = 4096;

    private RuneEffects() {}

    /**
     * The original {@code ClientProxy.particleCount(int)} (oracle
     * {@code thaumcraft/client/ClientProxy.java:899-904}): no particle at the minimal setting, the
     * base count at the decreased one and twice the base count at the full one.
     */
    public static int particleCount(int base) {
        var status = Minecraft.getInstance().options.particles().get();
        if (status == ParticleStatus.MINIMAL) return 0;
        return status == ParticleStatus.DECREASED ? base : base * 2;
    }

    /**
     * The original random update of the warding stone (oracle
     * {@code thaumcraft/common/blocks/BlockCosmeticSolid.java:332-337}): two runes above the stone
     * with the colours {@code 0.2 + rand * 0.4}, {@code rand * 0.3} and {@code 0.8 + rand * 0.2}, the
     * duration twenty and the gravity {@code -0.02}.
     */
    public static void wardingStone(ClientLevel level, BlockPos pos) {
        int count = particleCount(2);
        for (int index = 0; index < count; index++) {
            var random = level.getRandom();
            Minecraft.getInstance().particleEngine.add(new BlockRuneParticle(level,
                    pos.getX() + .5D, pos.getY() + .7D, pos.getZ() + .5D,
                    .2F + random.nextFloat() * .4F, random.nextFloat() * .3F, .8F + random.nextFloat() * .2F,
                    20, -.02F));
        }
    }

    /**
     * The original random update of the runed eldritch block (oracle
     * {@code thaumcraft/common/blocks/BlockEldritch.java:302-304}): one rune in one of the
     * neighbouring blocks with the colours {@code 0.5 + rand * 0.5}, {@code rand * 0.3} and
     * {@code 0.9 + rand * 0.1}, the duration {@code 16 + rand(4)} and the gravity zero.
     */
    public static void eldritchRune(ClientLevel level, BlockPos pos) {
        int count = particleCount(1);
        for (int index = 0; index < count; index++) {
            var random = level.getRandom();
            int x = pos.getX() + random.nextInt(2) - random.nextInt(2);
            int y = pos.getY() + random.nextInt(2) - random.nextInt(2);
            int z = pos.getZ() + random.nextInt(2) - random.nextInt(2);
            if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) continue;
            Minecraft.getInstance().particleEngine.add(new BlockRuneParticle(level,
                    x + random.nextFloat(), y + random.nextFloat(), z + random.nextFloat(),
                    .5F + random.nextFloat() * .5F, random.nextFloat() * .3F,
                    .9F + random.nextFloat() * .1F, 16 + random.nextInt(4), 0.0F));
        }
    }

    /**
     * The original random update rate: three updates of a chunk and of a tick, so a block is updated
     * once every 4096 / 3 ticks on average.
     */
    public static boolean randomUpdate(net.minecraft.util.RandomSource random) {
        return random.nextInt(CHUNK_SIZE / RANDOM_TICKS) < RANDOM_TICKS;
    }
}
