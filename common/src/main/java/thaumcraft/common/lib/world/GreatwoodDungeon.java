package thaumcraft.common.lib.world;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftLoot;
import thaumcraft.common.config.ThaumcraftNaturals;

/** Actual original cave-spider/50web/chest branch. Vanilla descriptors plus
 * actual TC4 weighted additions use PRIVATE tree RNG, not a modern loot table.
 * Original eight slot writes may overwrite one another; they are not merged. */
final class GreatwoodDungeon {
    private GreatwoodDungeon() {}
    static void place(WorldGenLevel level,RandomSource caller,RandomSource treeRandom,BlockPos origin) {
        BlockPos spawnerPos=origin.below(),chestPos=origin.below(2);
        if(!available(level,spawnerPos)||!available(level,chestPos))return;
        if(!level.setBlock(spawnerPos,Blocks.SPAWNER.defaultBlockState(),3)||!(level.getBlockEntity(spawnerPos) instanceof SpawnerBlockEntity spawner))return;
        // Fresh native spawnPotentials is empty and consumes NO random draw.
        // Null-level suppresses live ServerLevel updates from a worker region;
        // the actual saved spawner BE and later full chunk packet carry the ID.
        spawner.getSpawner().setEntityId(EntityType.CAVE_SPIDER,null,treeRandom,spawnerPos);
        for(int i=0;i<50;i++){
            int x=origin.getX()-7+caller.nextInt(14),y=origin.getY()+caller.nextInt(10),z=origin.getZ()-7+caller.nextInt(14);var pos=new BlockPos(x,y,z);
            if(!available(level,pos)||!level.isEmptyBlock(pos))continue;
            boolean touching=false;for(Direction direction:Direction.values()){
                var neighbor=pos.relative(direction);if(!available(level,neighbor))continue;BlockState state=level.getBlockState(neighbor);
                if(state.is(ThaumcraftNaturals.GREATWOOD_LEAVES.get())||state.is(ThaumcraftNaturals.SILVERWOOD_LEAVES.get())
                    ||state.is(ThaumcraftNaturals.GREATWOOD_LOG.get())||state.is(ThaumcraftNaturals.SILVERWOOD_LOG.get())){touching=true;break;}
            }
            if(touching)level.setBlock(pos,Blocks.COBWEB.defaultBlockState(),3);
        }
        if(!level.setBlock(chestPos,Blocks.CHEST.defaultBlockState(),3)||!(level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest))return;
        List<ThaumcraftLoot.DungeonEntry> entries=ThaumcraftLoot.dungeonEntries(level,treeRandom);
        var items=NonNullList.withSize(27,ItemStack.EMPTY);
        int total=0;for(var entry:entries)total=Math.addExact(total,entry.weight());
        // Forge dungeonChest min=max=8; getCount consumes NO random draw.
        for(int roll=0;roll<8;roll++){
            int ticket=treeRandom.nextInt(total);ThaumcraftLoot.DungeonEntry chosen=null;
            for(var entry:entries){ticket-=entry.weight();if(ticket<0){chosen=entry;break;}}
            if(chosen==null)throw new IllegalStateException("Original dungeon descriptor weights are inconsistent");
            int count=chosen.minCount()+treeRandom.nextInt(chosen.maxCount()-chosen.minCount()+1);
            ItemStack stack=chosen.baseStack();
            if(stack.getMaxStackSize()>=count){stack.setCount(count);items.set(treeRandom.nextInt(items.size()),stack);}
            else for(int i=0;i<count;i++){var single=stack.copy();single.setCount(1);items.set(treeRandom.nextInt(items.size()),single);}
        }
        CompoundTag data=chest.saveWithoutMetadata(level.registryAccess());ContainerHelper.saveAllItems(data,items,level.registryAccess());
        chest.loadWithComponents(data,level.registryAccess());
        if(level instanceof ServerLevel server){chest.setChanged();spawner.setChanged();server.sendBlockUpdated(chestPos,level.getBlockState(chestPos),level.getBlockState(chestPos),Block.UPDATE_CLIENTS);}
    }
    private static boolean available(WorldGenLevel level,BlockPos pos) {return !level.isOutsideBuildHeight(pos)&&level.hasChunkAt(pos)&&level.ensureCanWrite(pos);}
}
