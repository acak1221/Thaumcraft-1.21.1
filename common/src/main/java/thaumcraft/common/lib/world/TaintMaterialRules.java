package thaumcraft.common.lib.world;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Native replacements for removed 1.7 wood-log, gourd/cactus and soil/sand material predicates. */
public final class TaintMaterialRules {
    private TaintMaterialRules() {}
    public static boolean log(BlockState state){return state.is(BlockTags.LOGS);}
    public static boolean crust(BlockState state) {
        return log(state)||state.is(Blocks.CACTUS)||state.is(Blocks.PUMPKIN)||state.is(Blocks.CARVED_PUMPKIN)||state.is(Blocks.JACK_O_LANTERN)||state.is(Blocks.MELON);
    }
    public static boolean soil(BlockState state) {
        return state.is(BlockTags.DIRT)||state.is(BlockTags.SAND)||state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.MYCELIUM)
                ||state.is(Blocks.FARMLAND)||state.is(Blocks.CLAY)||state.is(Blocks.GRAVEL)||state.is(Blocks.SOUL_SAND);
    }
}
