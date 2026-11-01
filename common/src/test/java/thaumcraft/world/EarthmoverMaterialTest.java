package thaumcraft.world;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.EarthmoverMaterial;

import static org.junit.jupiter.api.Assertions.*;

class EarthmoverMaterialTest {
    @BeforeAll static void bootstrap(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    @Test void multiplePlacementItemsCannotBecomeOneUnitOfStock(){
        assertEquals(2,EarthmoverMaterial.units(Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE)));
        assertEquals(8,EarthmoverMaterial.units(Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS,8)));
        assertEquals(4,EarthmoverMaterial.units(Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES,4)));
        assertEquals(4,EarthmoverMaterial.units(Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.PICKLES,4)));
        assertEquals(4,EarthmoverMaterial.units(Blocks.TURTLE_EGG.defaultBlockState().setValue(BlockStateProperties.EGGS,4)));
        assertEquals(4,EarthmoverMaterial.units(Blocks.PINK_PETALS.defaultBlockState().setValue(BlockStateProperties.FLOWER_AMOUNT,4)));
    }
    @Test void ordinaryOrientationDoesNotChangeMaterialIdentity(){
        for(var face:List.of(net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.SOUTH))
            assertTrue(EarthmoverMaterial.matches(new ItemStack(Items.STONE_STAIRS),Blocks.STONE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,face)));
        assertFalse(EarthmoverMaterial.matches(new ItemStack(Items.COBBLESTONE),Blocks.STONE.defaultBlockState()));
        assertEquals(1,EarthmoverMaterial.units(Blocks.STONE.defaultBlockState()));
    }
    @Test void copiedMetadataDoesNotIncludeAnUnpaidNativeWaterSource(){
        var wet=Blocks.STONE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING,net.minecraft.core.Direction.SOUTH)
                .setValue(BlockStateProperties.HALF,net.minecraft.world.level.block.state.properties.Half.TOP)
                .setValue(BlockStateProperties.WATERLOGGED,true);
        var dry=EarthmoverMaterial.placementState(wet);
        assertEquals(wet.setValue(BlockStateProperties.WATERLOGGED,false),dry);
        assertFalse(dry.getValue(BlockStateProperties.WATERLOGGED));
        assertSame(Blocks.STONE.defaultBlockState(),EarthmoverMaterial.placementState(Blocks.STONE.defaultBlockState()));
    }
}
