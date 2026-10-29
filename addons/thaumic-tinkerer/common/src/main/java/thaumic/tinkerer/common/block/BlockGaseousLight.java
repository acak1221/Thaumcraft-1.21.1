/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [8 Sep 2013, 22:20:30 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.common.core.helper.SimpleBlockFx;

/** Gaseous Illuminae: invisible light of level 12 ({@code setLightLevel(0.85F)} is {@code (int) (15 * 0.85F)}). */
public class BlockGaseousLight extends BlockGas {
    /** {@code (int) (15.0F * 0.85F)}. */
    public static final int LIGHT = 12;

    public BlockGaseousLight(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return gasProperties().lightLevel(state -> LIGHT);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.0075F) {
            SimpleBlockFx.sparkle(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.0F, SimpleBlockFx.SPARKLE_YELLOW,
                    random.nextFloat() / 2);
        }
    }

    @Override
    public void placeParticle(Level level, BlockPos pos) {
        SimpleBlockFx.sparkleAt(level, pos, SimpleBlockFx.SPARKLE_YELLOW);
    }
}
