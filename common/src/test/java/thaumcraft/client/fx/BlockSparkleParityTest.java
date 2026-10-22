package thaumcraft.client.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import thaumcraft.client.fx.particles.BlockSparkleParticle;
import thaumcraft.common.lib.network.BlockSparkleFxNetworking;
import thaumcraft.common.lib.network.BlockSparkleFxPayload;

/**
 * The original block sparkle packet and its particle.
 *
 * <p>Original {@code thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java:36-52}: the packet
 * carries the position and the raw colour, and the client half calls
 * {@code blockSparkle(world, x, y, z, color, 7)} with seven particles.</p>
 *
 * <p>Original {@code thaumcraft/client/ClientProxy.java:632-644} ({@code blockSparkle}): the three
 * colour channels are the bytes of the colour divided by 255, every channel is jittered by
 * {@code -0.2 + random * 0.4}, the special colour {@code -9999} is replaced by a random grey
 * {@code 0.33 + random * 0.67} per channel and per particle, the alpha is {@code 0.9f} and the
 * particle is drawn at {@code x - 0.1 + random * 1.2}.</p>
 *
 * <p>Original {@code thaumcraft/client/fx/particles/FXGeneric.java:39-52}: the atlas frames run from
 * 112 over nine frames, the life is {@code 5 + random(8)} plus the delay {@code random(10)} and the
 * scale is {@code 0.7 + random * 0.4} of the tenth of the particle.</p>
 */
class BlockSparkleParityTest {
    @Test void thePacketCarriesTheOriginalRandomGreyMarker() {
        assertEquals(-9999, BlockSparkleFxPayload.RANDOM_GREY,
                "the original uses -9999 as the random grey marker of the block sparkle");
        assertEquals(32.0D, BlockSparkleFxNetworking.RANGE,
                "the original TargetPoint radius of the block sparkle is thirty two blocks");
    }

    @Test void theOrdinaryColourIsTheThreeBytesOfThePacket() {
        float[] colour = BlockSparkleParticle.baseColour(0xDD00FF, RandomSource.create(1L));
        assertEquals(0xDD / 255F, colour[0], 1.0E-6F);
        assertEquals(0x00 / 255F, colour[1], 1.0E-6F);
        assertEquals(0xFF / 255F, colour[2], 1.0E-6F);
    }

    @Test void theRandomGreyIsTheOriginalRange() {
        var random = RandomSource.create(1234L);
        for (int i = 0; i < 64; i++) {
            float[] colour = BlockSparkleParticle.baseColour(BlockSparkleFxPayload.RANDOM_GREY, random);
            for (float channel : colour) {
                assertTrue(channel >= .33F && channel <= 1.0F,
                        "the original random grey is 0.33 + random * 0.67, not " + channel);
            }
        }
    }

    @Test void theJitterIsTheOriginalMinusTwoTenthsPlusFourTenths() {
        var random = RandomSource.create(99L);
        float[] base = {.25F, 1.0F, .25F};
        for (int i = 0; i < 64; i++) {
            float[] colour = BlockSparkleParticle.jitter(base, random);
            for (int channel = 0; channel < 3; channel++) {
                assertTrue(colour[channel] >= base[channel] - .2F && colour[channel] <= base[channel] + .2F,
                        "the original jitter is -0.2 + random * 0.4, not " + colour[channel]);
            }
        }
    }

    @Test void theParticleKeepsTheOriginalFramesAlphaAndScale() {
        assertEquals(112, BlockSparkleParticle.FIRST_FRAME, "the original FXGeneric starts at the frame 112");
        assertEquals(9, BlockSparkleParticle.FRAME_COUNT, "the original FXGeneric draws nine frames");
        assertEquals(.9F, BlockSparkleParticle.ALPHA, "the original block sparkle alpha is 0.9f");
        assertEquals(.1F, BlockSparkleParticle.QUAD, "the original FXGeneric quarter is a tenth");
    }
}
