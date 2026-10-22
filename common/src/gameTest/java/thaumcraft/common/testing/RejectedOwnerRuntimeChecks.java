package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.lib.utils.RejectedBlockEntityData;
import thaumcraft.common.tiles.TilePedestal;

/** Real registered owner, onRemove and durable SavedData custody, not a test inventory. */
public final class RejectedOwnerRuntimeChecks {
    private RejectedOwnerRuntimeChecks() {}
    public static void infusion(GameTestHelper test) {
        var level=test.getLevel(); var registries=level.registryAccess();
        BlockPos pos=test.absolutePos(new BlockPos(1,2,1));
        level.setBlock(pos,ThaumcraftInfusion.MATRIX.get().defaultBlockState(),Block.UPDATE_ALL);
        var tile=(thaumcraft.common.tiles.TileInfusionMatrix)level.getBlockEntity(pos);
        CompoundTag bad=tile.saveWithoutMetadata(registries);
        bad.putBoolean("active",true); bad.putBoolean("crafting",true); bad.putInt("recipexp",7);
        bad.putString("recipeplayer","paid-session-owner"); bad.putUUID("RecipePlayerId",new java.util.UUID(1,2));
        var list=new net.minecraft.nbt.ListTag(); list.add(net.minecraft.nbt.StringTag.valueOf("unread paid requirement")); bad.put("recipein",list);
        tile.loadWithComponents(bad,registries);
        test.assertTrue(tile.isDataRejected(),"malformed requirement must reject the entire paid session");
        test.assertTrue(bad.equals(tile.saveWithoutMetadata(registries)),"debts and owner must not be replaced by sentinels");
        test.assertTrue(tile.getPendingEssentia(thaumcraft.api.aspects.Aspect.AIR)==0,"rejected session must not accept essentia");
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
        var archive=level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(RejectedBlockEntityData::new,RejectedBlockEntityData::load,null),RejectedBlockEntityData.ID);
        boolean found=false;
        for (Tag value:archive.save(new CompoundTag(),registries).getList("Owners",Tag.TAG_COMPOUND)) {
            CompoundTag row=(CompoundTag)value;
            if (row.getLong("Position")==pos.asLong() && row.getCompound("Payload").equals(bad)) found=true;
        }
        test.assertTrue(found,"removed rejected session must remain in the offline-repair archive");
        test.succeed();
    }
    public static void pedestal(GameTestHelper test) {
        var level=test.getLevel(); var registries=level.registryAccess();
        BlockPos pos=test.absolutePos(new BlockPos(1,2,1));
        level.setBlock(pos,ThaumcraftInfusion.PEDESTAL.get().defaultBlockState(),Block.UPDATE_ALL);
        TilePedestal tile=(TilePedestal)level.getBlockEntity(pos);
        CompoundTag bad=new CompoundTag(), stack=new CompoundTag();
        stack.putString("id","missing_addon:player_owned_relic"); stack.putInt("count",1);
        bad.put("Stack",stack); bad.putInt("Count",17);
        tile.loadWithComponents(bad,registries);
        test.assertTrue(tile.isDataRejected(),"unknown item must reject the whole pedestal");
        test.assertTrue(bad.equals(tile.saveWithoutMetadata(registries)),"rejected pedestal must save original bytes");
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
        test.assertTrue(level.getBlockEntity(pos)==null,"physical owner was removed");
        var archive=level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(RejectedBlockEntityData::new,RejectedBlockEntityData::load,null),RejectedBlockEntityData.ID);
        CompoundTag saved=archive.save(new CompoundTag(),registries);
        boolean found=false;
        for (Tag value:saved.getList("Owners",Tag.TAG_COMPOUND)) {
            CompoundTag row=(CompoundTag)value;
            if (row.getLong("Position")==pos.asLong() && row.getCompound("Payload").equals(bad)) found=true;
        }
        test.assertTrue(found,"broken rejected pedestal must survive in the offline-repair archive");
        test.assertTrue(saved.equals(RejectedBlockEntityData.load(saved,registries).save(new CompoundTag(),registries)),"archive reload is lossless");
        test.succeed();
    }
}
