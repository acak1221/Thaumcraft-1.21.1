package thaumcraft.common.lib.world;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Native replacements for the removed 1.7.10 wood-log and material predicates.
 *
 * <p>The original converts a block into taint crust or taint soil by the material of the 1.7.10
 * world (oracle {@code thaumcraft/common/blocks/BlockTaintFibres.java}:173 and :177). The 1.7.10
 * materials behind those two expressions are, with the 1.7.10 MCP names resolved by
 * {@code scripts/porting/resolve_srg.py}:</p>
 * <ul>
 *   <li>crust: {@code Material.field_151572_C} = gourd (pumpkin and melon), {@code Material.field_151570_A}
 *       = cactus, plus {@code Utils.isWoodLog}, which is every wood log.</li>
 *   <li>soil: {@code Material.field_151595_p} = sand (sand and gravel in 1.7.10),
 *       {@code Material.field_151578_c} = ground (dirt and farmland), {@code Material.field_151577_b} = grass
 *       (the grass block) and {@code Material.field_151571_B} = clay (the clay block).</li>
 * </ul>
 * <p>The native sets are exactly these blocks. The 1.21.1 blocks that have no 1.7.10 counterpart
 * (mycelium, soul sand, coarse dirt, rooted dirt and red sand) are not converted, because the
 * original never converted them either.</p>
 */
public final class TaintMaterialRules {
    private TaintMaterialRules() {}
    /** Original Utils.isWoodLog - every wood log of the world. */
    public static boolean log(BlockState state){return state.is(BlockTags.LOGS);}
    /** Original BlockTaintFibres:173 - a wood log, a gourd or a cactus becomes taint crust. */
    public static boolean crust(BlockState state) {
        return log(state)||state.is(Blocks.CACTUS)||state.is(Blocks.PUMPKIN)||state.is(Blocks.CARVED_PUMPKIN)||state.is(Blocks.JACK_O_LANTERN)||state.is(Blocks.MELON);
    }
    /** Original BlockTaintFibres:177 - sand, gravel, dirt, farmland, grass or clay becomes taint soil. */
    public static boolean soil(BlockState state) {
        return state.is(Blocks.SAND)||state.is(Blocks.GRAVEL)||state.is(Blocks.DIRT)||state.is(Blocks.FARMLAND)
                ||state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.CLAY);
    }
}
