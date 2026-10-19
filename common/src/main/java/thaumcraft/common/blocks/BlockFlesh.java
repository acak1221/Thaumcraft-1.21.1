package thaumcraft.common.blocks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Original BlockTaint metadata2, without the other metadata's taint behavior. */
public class BlockFlesh extends Block {
    private static final SoundEvent GORE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "gore"));
    private static final SoundType FLESH_SOUND = new SoundType(0.5F, 0.8F, GORE, GORE, GORE, GORE, GORE);

    /** Pass fresh Properties.of(), not copied rock/tool/no-occlusion properties. */
    public BlockFlesh(BlockBehaviour.Properties properties) {
        // BlockTaint:69-75,248-259: hardness .2; setResistance(10) becomes 30/5.
        // MaterialTaint.isSolid=false does not make Block.isOpaqueCube false.
        // Preserve movement blocking/full-cube occlusion, not a forced non-solid
        // native state: the old material's separate isSolid flag has no direct map.
        super(properties.mapColor(MapColor.GRASS)
                .strength(0.2F, 6.0F)
                .sound(FLESH_SOUND)
                .pushReaction(PushReaction.DESTROY)
                .isRedstoneConductor((state, level, pos) -> true)
                .isSuffocating((state, level, pos) -> true)
                .isViewBlocking((state, level, pos) -> true)
                .randomTicks());
        // No required tool, fire entry, BE or callbacks. Inherited randomTick is
        // a no-op, matching metadata2's immediate return in BlockTaint:152-157.
    }
}
