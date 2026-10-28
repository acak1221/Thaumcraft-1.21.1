/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [8 Sep 2013, 22:21:13 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.common.core.helper.SimpleBlockFx;

/**
 * Gaseous Tenebrae: invisible darkness. {@code setLightOpacity(215)} is far above the 15 steps of the 1.21.1 light
 * scale, so the gas blocks all light that reaches it.
 */
public class BlockGaseousShadow extends BlockGas {

    public BlockGaseousShadow(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties properties() {
        return gasProperties();
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 15;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.0075F) {
            level.addParticle(SimpleBlockFx.SHADOW_WISP, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0, 0.02, 0.0);
        }
    }

    @Override
    public void placeParticle(Level level, BlockPos pos) {
        SimpleBlockFx.shadowWispAt(level, pos);
    }
}
