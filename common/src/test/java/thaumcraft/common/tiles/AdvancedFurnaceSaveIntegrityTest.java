package thaumcraft.common.tiles;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdvancedFurnaceSaveIntegrityTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    private TileAlchemyFurnaceAdvanced tile() {
        return new TileAlchemyFurnaceAdvanced(BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
    }
    @Test void wrongElementStockIsQuarantinedLosslessly() {
        CompoundTag input = new CompoundTag();
        ListTag list = new ListTag(); list.add(StringTag.valueOf("unread stock")); input.put("Aspects", list);
        var tile = tile(); tile.loadAdditional(input, null);
        assertTrue(tile.isQuarantined());
        CompoundTag saved = new CompoundTag(); tile.saveAdditional(saved, null);
        assertEquals(input, saved.getCompound("RejectedAdvancedFurnace"));
    }
    @Test void malformedRejectedEnvelopeSurvivesRepeatedReload() {
        CompoundTag input = new CompoundTag(); input.putString("RejectedAdvancedFurnace", "unread envelope");
        var tile = tile(); tile.loadAdditional(input, null);
        CompoundTag saved = new CompoundTag(); tile.saveAdditional(saved, null);
        assertEquals(input, saved.getCompound("RejectedAdvancedFurnace"));
        var next = tile(); next.loadAdditional(saved, null);
        CompoundTag again = new CompoundTag(); next.saveAdditional(again, null);
        assertEquals(saved, again);
    }
}
