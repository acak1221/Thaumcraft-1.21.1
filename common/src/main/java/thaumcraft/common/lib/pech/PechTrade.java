package thaumcraft.common.lib.pech;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftLoot;
import thaumcraft.common.items.ItemManaBean;

/** Finite staged original ContainerPech reward algorithm. No live inventory,
 * tame state or player callback is changed while planning. Original uniform
 * dungeon ENTRY selection, rejection draw order, 50th-entry acceptance and
 * duplicate direct-table entries are retained. Original unbounded retries and
 * silently discarded merge remainders are fixed by refusing the unpaid trade. */
public final class PechTrade {
    public static final int MAX_ROUNDS=128,MAX_TIER_REJECTIONS=512;
    private PechTrade(){}
    public record Plan(List<ItemStack> pack,List<ItemStack> outputs,List<ItemStack> craftedRewards,boolean untame){
        public Plan{pack=copy(pack);outputs=copy(outputs);craftedRewards=copy(craftedRewards);}
        @Override public List<ItemStack> pack(){return copy(pack);}
        @Override public List<ItemStack> outputs(){return copy(outputs);}
        @Override public List<ItemStack> craftedRewards(){return copy(craftedRewards);}
    }
    public static Plan prepare(int value,List<ItemStack> livePack,List<PechTradeTables.Entry> table,Supplier<List<ThaumcraftLoot.DungeonEntry>> dungeon,RandomSource random){
        if(value<1||value>32||livePack.size()!=9||table.isEmpty())return null;
        var pack=copy(livePack);var outputs=new ArrayList<ItemStack>();for(int i=0;i<4;i++)outputs.add(ItemStack.EMPTY);
        var crafted=new ArrayList<ItemStack>();boolean untame=random.nextInt(100)<=value/2;
        if(random.nextInt(5)==0)value+=random.nextInt(3);else if(random.nextBoolean())value-=random.nextInt(3);
        int rounds=0;
        while(value>0){
            if(++rounds>MAX_ROUNDS)return null;
            int amount=Math.min(5,Math.max((value+1)/2,random.nextInt(value)+1));value-=amount;
            if(amount==1&&random.nextBoolean()){
                var filled=new ArrayList<Integer>();for(int i=0;i<pack.size();i++)if(!pack.get(i).isEmpty())filled.add(i);
                if(!filled.isEmpty()){
                    int index=filled.get(random.nextInt(filled.size()));ItemStack reward=pack.get(index).copyWithCount(1);
                    if(!merge(outputs,reward))return null;pack.get(index).shrink(1);if(pack.get(index).isEmpty())pack.set(index,ItemStack.EMPTY);continue;
                }
            }
            ItemStack reward;
            if(amount>=4&&random.nextBoolean()){
                List<ThaumcraftLoot.DungeonEntry> contents=dungeon.get();if(contents==null||contents.isEmpty())return null;
                ThaumcraftLoot.DungeonEntry entry=null;
                for(int attempts=0;attempts<50;attempts++){
                    entry=contents.get(random.nextInt(contents.size()));
                    if(entry!=null&&!entry.baseStack().isEmpty()&&entry.weight()<=5&&entry.maxCount()<=1)break;
                }
                if(entry==null||entry.baseStack().isEmpty()){value+=amount;continue;}
                reward=entry.baseStack().copy();
            }else{
                PechTradeTables.Entry chosen=null;
                for(int attempts=0;attempts<MAX_TIER_REJECTIONS;attempts++){
                    var entry=table.get(random.nextInt(table.size()));if(entry.tier()==amount){chosen=entry;break;}
                }
                if(chosen==null)return null;reward=chosen.baseStack();
            }
            // The ONLY original onCreated callback that changes reward identity.
            // Perform item-local randomization on this private copy before merge;
            // actual player callbacks run once only after the paid commit.
            if(reward.getItem() instanceof ItemManaBean bean)bean.initializeFresh(reward);
            crafted.add(reward.copy());if(!merge(outputs,reward))return null;
        }
        return new Plan(pack,outputs,crafted,untame);
    }
    /** All-or-nothing merge. The input argument is never changed. */
    public static boolean merge(List<ItemStack> outputs,ItemStack incoming){
        if(incoming.isEmpty())return true;
        int left=incoming.getCount();
        for(ItemStack stored:outputs)if(!stored.isEmpty()&&ItemStack.isSameItemSameComponents(stored,incoming))left-=Math.min(left,Math.max(0,stored.getMaxStackSize()-stored.getCount()));
        for(ItemStack stored:outputs)if(stored.isEmpty())left-=Math.min(left,incoming.getMaxStackSize());
        if(left>0)return false;
        left=incoming.getCount();
        for(ItemStack stored:outputs)if(left>0&&!stored.isEmpty()&&ItemStack.isSameItemSameComponents(stored,incoming)){int amount=Math.min(left,Math.max(0,stored.getMaxStackSize()-stored.getCount()));stored.grow(amount);left-=amount;}
        for(int i=0;i<outputs.size()&&left>0;i++)if(outputs.get(i).isEmpty()){int amount=Math.min(left,incoming.getMaxStackSize());outputs.set(i,incoming.copyWithCount(amount));left-=amount;}
        return left==0;
    }
    private static ArrayList<ItemStack> copy(List<ItemStack> stacks){var copy=new ArrayList<ItemStack>(stacks.size());for(ItemStack stack:stacks)copy.add(stack.copy());return copy;}
}
