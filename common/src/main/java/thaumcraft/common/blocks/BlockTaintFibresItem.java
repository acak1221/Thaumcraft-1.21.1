package thaumcraft.common.blocks;

import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Explicit subtype placement; only fibre0 owns native Block.asItem's canonical mapping. */
public final class BlockTaintFibresItem extends BlockItem {
    private final int type;
    public BlockTaintFibresItem(Block block,Properties properties,int type) {
        super(block,properties);if(type<0||type>4)throw new IllegalArgumentException("Invalid original taint fibre type");this.type=type;
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        if(!BlockTaintFibres.isTaintBiome(context.getLevel(),context.getClickedPos()))return null;
        var state=super.getPlacementState(context);return state==null?null:state.setValue(BlockTaintFibres.TYPE,type);
    }
    @Override public void registerBlocks(Map<Block,Item> map,Item item) { if(type==0)super.registerBlocks(map,item); }
    @Override public String getDescriptionId() {
        return switch(type) {
            case 1 -> "item.thaumcraft.taint_grass";
            case 2 -> "item.thaumcraft.taint_plant";
            case 3 -> "item.thaumcraft.taint_spore_stalk";
            case 4 -> "item.thaumcraft.taint_spore_stalk_mature";
            default -> super.getDescriptionId();
        };
    }
}
