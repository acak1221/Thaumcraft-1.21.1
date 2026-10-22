package thaumcraft.common.tiles;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.utils.RejectedBlockEntityData;
import static org.junit.jupiter.api.Assertions.*;

class StackOwnerSaveIntegrityTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    private final HolderLookup.Provider registries=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private final BlockEntityType<?> type=BlockEntityType.CHEST;
    private final net.minecraft.world.level.block.state.BlockState state=Blocks.CHEST.defaultBlockState();
    private CompoundTag stack() { CompoundTag tag=new CompoundTag(); tag.putString("id","missing_addon:owned_item"); tag.putInt("count",1); return tag; }
    private CompoundTag owner(String key, boolean nested) {
        CompoundTag tag=new CompoundTag(), row=nested?new CompoundTag():stack();
        if(nested) row.put("Stack",stack()); row.putByte("Slot",(byte)0); row.putInt("StoredCount",12);
        ListTag rows=new ListTag(); rows.add(row); tag.put(key,rows); return tag;
    }
    private void check(RetainedDataBlockEntity tile,CompoundTag input) {
        tile.loadAdditional(input,registries);
        CompoundTag saved=new CompoundTag(); tile.saveAdditional(saved,registries);
        assertEquals(input,saved,tile.getClass().getSimpleName());
        assertTrue(tile.isDataRejected());
        tile.setRemoved();
        CompoundTag again=new CompoundTag(); tile.saveAdditional(again,registries); assertEquals(input,again);
    }
    @Test void pedestalRetainsUnreadStack() {
        CompoundTag tag=new CompoundTag(); tag.put("Stack",stack()); tag.putInt("Count",12);
        var tile=new TilePedestal(type,BlockPos.ZERO,state); check(tile,tag);
        tile.setItem(0,new ItemStack(Items.DIRT)); assertTrue(tile.getItem(0).isEmpty());
    }
    @Test void researchTableRetainsUnreadStack() { check(new TileResearchTable(type,BlockPos.ZERO,state),owner("Inventory",true)); }
    @Test void mirrorRetainsUnreadStack() { check(new TileMirror(type,BlockPos.ZERO,state),owner("Items",false)); }
    @Test void magicBoxRetainsUnreadStack() { check(new TileMagicBox(type,BlockPos.ZERO,state),owner("Items",false)); }
    @Test void arcaneFurnaceRetainsUnreadStack() { check(new TileArcaneFurnace(type,BlockPos.ZERO,state),owner("Items",true)); }
    @Test void sharedInventoryRetainsUnreadStack() { check(new TileThaumcraftInventory(type,BlockPos.ZERO,state),owner("Items",true)); }
    @Test void wrongCollectionElementsAndInvalidBonusAreRetained() {
        CompoundTag bad=new CompoundTag(); ListTag list=new ListTag(); list.add(StringTag.valueOf("unread row")); bad.put("Items",list);
        check(new TileMagicBox(type,BlockPos.ZERO,state),bad);
        check(new TileMirror(type,BlockPos.ZERO,state),bad);
        check(new TileArcaneFurnace(type,BlockPos.ZERO,state),bad);
        check(new TileThaumcraftInventory(type,BlockPos.ZERO,state),bad);
        bad=new CompoundTag(); bad.put("Inventory",list.copy()); check(new TileResearchTable(type,BlockPos.ZERO,state),bad);
        bad=new CompoundTag(); CompoundTag bonus=new CompoundTag(); bonus.putString("tag","INVALID"); bonus.putInt("amount",12);
        ListTag bonuses=new ListTag(); bonuses.add(bonus); bad.put("BonusAspects",bonuses);
        check(new TileResearchTable(type,BlockPos.ZERO,state),bad);
    }
    @Test void retirementArchiveIsLosslessAndDeduplicatesOnlyIdenticalRecords() {
        var archive=new RejectedBlockEntityData(); CompoundTag record=owner("Inventory",true);
        archive.retain(record); archive.retain(record.copy());
        CompoundTag saved=archive.save(new CompoundTag(),null);
        assertEquals(1,saved.getList("Owners",Tag.TAG_COMPOUND).size());
        assertEquals(record,saved.getList("Owners",Tag.TAG_COMPOUND).getCompound(0));
        assertEquals(saved,RejectedBlockEntityData.load(saved,null).save(new CompoundTag(),null));
    }
}
