package thaumcraft.client.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.PushReaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.client.fx.particles.MonsterSpriteParticle;
import thaumcraft.client.fx.particles.PrimalWispParticle;
import thaumcraft.client.fx.particles.WarpGuardianParticle;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.tiles.TileCrucible;

/**
 * The numbers of the native client particles and materials against the TC4 4.2.3.5 oracle.
 *
 * <p>Original wisp mote (oracle {@code thaumcraft/client/fx/particles/FXWisp.java:44} and :180): the
 * particle scale is the size the caller gives and the quad is drawn with the half size
 * {@code 0.5f * scale * ageScale}. The original never randomises that size.</p>
 *
 * <p>Original burst (oracle {@code thaumcraft/client/fx/particles/FXBurst.java:32} and :50): the particle
 * scale is the size the caller gives and the quad is drawn with that half size.</p>
 *
 * <p>Original guardian haze (oracle {@code thaumcraft/client/fx/particles/FXWispEG.java:25} and :53): the
 * particle scale is the fixed {@code 0.4f} and the half size is {@code 0.5f * 0.4f}.</p>
 *
 * <p>Original crucible (oracle {@code thaumcraft/common/tiles/TileCrucible.java:99-101}): the heat sources
 * are the blocks with the material fire, the blocks with the material lava and the nitor. The 1.7.10 fire
 * MATERIAL is the fire block alone.</p>
 *
 * <p>Original taint material (oracle {@code thaumcraft/common/config/MaterialTaint.java:24-38} and
 * {@code thaumcraft/common/config/Config.java:917-918}): the mobility flag one keeps a piston from pushing
 * the taint, the flux gas and the flux goo.</p>
 */
class ParticleScaleParityTest {
    /** The 1.21.1 registries need the bootstrap before a block state can be read. */
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void theWispMoteKeepsTheOriginalHalfSize() {
        // The original wisp entity trail uses the size 0.1f, the primal orb trail the size 0.4f.
        assertEquals(0.05F, MonsterSpriteParticle.moteHalfSize(.1F), 1.0E-6);
        assertEquals(0.2F, MonsterSpriteParticle.moteHalfSize(.4F), 1.0E-6);
        // The 1.21.1 particle engine halves the quad size, so the quad size is the particle scale itself.
        assertEquals(.4F, PrimalWispParticle.moteQuadSize(.4F), 1.0E-6);
    }

    @Test void theBurstKeepsTheOriginalHalfSize() {
        assertEquals(1.0F, MonsterSpriteParticle.burstHalfSize(1.0F), 1.0E-6);
        assertEquals(0.25F, MonsterSpriteParticle.burstHalfSize(.25F), 1.0E-6);
    }

    @Test void theGuardianHazeKeepsTheOriginalConstantSize() {
        assertEquals(.4F, WarpGuardianParticle.QUAD_SIZE, 1.0E-6);
    }

    @Test void theCrucibleIsHeatedByTheFireBlockAlone() {
        assertTrue(TileCrucible.isHeatSource(Blocks.FIRE.defaultBlockState()), "the fire block heats the crucible");
        assertFalse(TileCrucible.isHeatSource(Blocks.CAMPFIRE.defaultBlockState()),
                "the 1.7.10 fire MATERIAL is the fire block alone, a campfire did not heat the crucible");
        assertFalse(TileCrucible.isHeatSource(Blocks.SOUL_CAMPFIRE.defaultBlockState()),
                "the 1.7.10 fire MATERIAL is the fire block alone, a soul campfire did not heat the crucible");
        assertFalse(TileCrucible.isHeatSource(Blocks.STONE.defaultBlockState()), "stone does not heat the crucible");
    }

    @Test void theTaintAndTheFluxAreNotPushable() {
        assertEquals(PushReaction.BLOCK, ThaumcraftTaint.PUSH_REACTION,
                "the mobility flag one of the original taint material keeps a piston from pushing the taint");
        assertEquals(PushReaction.BLOCK, ThaumcraftFlux.PUSH_REACTION,
                "the flux gas and the flux goo use the same taint material");
    }
}
