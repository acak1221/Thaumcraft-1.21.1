package thaumcraft.pech;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ThaumcraftLoot;
import thaumcraft.common.lib.pech.PechTrade;
import thaumcraft.common.lib.pech.PechTradeTables;
import static org.junit.jupiter.api.Assertions.*;

/** Real native stack/component transactions; scripted RNG verifies ORIGINAL
 * short-circuit/rejection order rather than comparing the port with itself. */
class PechTradeTest {
    @BeforeAll static void bootstrap(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    private static ArrayList<ItemStack> empty(int count){var result=new ArrayList<ItemStack>();for(int i=0;i<count;i++)result.add(ItemStack.EMPTY);return result;}
    private static final class Draws extends LegacyRandomSource {
        final ArrayDeque<Integer> integers;final ArrayDeque<Boolean> booleans;final List<Integer> bounds=new ArrayList<>();
        Draws(List<Integer> integers,Boolean...booleans){super(1);this.integers=new ArrayDeque<>(integers);this.booleans=new ArrayDeque<>(Arrays.asList(booleans));}
        @Override public int nextInt(int bound){bounds.add(bound);assertFalse(integers.isEmpty(),"Unexpected integer draw "+bound);int value=integers.removeFirst();assertTrue(value>=0&&value<bound,"Fixture integer outside original bound");return value;}
        @Override public boolean nextBoolean(){assertFalse(booleans.isEmpty(),"Unexpected boolean draw");return booleans.removeFirst();}
        void exhausted(){assertTrue(integers.isEmpty());assertTrue(booleans.isEmpty());}
    }
    @Test void directTierSamplingRejectsWholeTableEntriesInOrderAndReturnsIndependentCopies(){
        var pack=empty(9);pack.set(2,new ItemStack(Items.DIAMOND,3));
        var rows=List.of(new PechTradeTables.Entry(5,new ItemStack(Items.DIRT)),new PechTradeTables.Entry(1,new ItemStack(Items.GOLD_INGOT)),new PechTradeTables.Entry(1,new ItemStack(Items.EMERALD)));
        var draws=new Draws(List.of(99,1,0,0,1),false,false);
        var plan=PechTrade.prepare(1,pack,rows,()->{fail("Tier1 cannot request dungeon descriptors");return List.of();},draws);
        assertNotNull(plan);draws.exhausted();assertEquals(List.of(100,5,1,3,3),draws.bounds);
        assertTrue(plan.outputs().getFirst().is(Items.GOLD_INGOT));assertEquals(1,plan.outputs().getFirst().getCount());assertEquals(3,pack.get(2).getCount());assertFalse(plan.untame());
        plan.outputs().getFirst().setCount(50);plan.pack().get(2).setCount(1);assertEquals(1,plan.outputs().getFirst().getCount());assertEquals(3,plan.pack().get(2).getCount());assertEquals(3,pack.get(2).getCount());
    }
    @Test void packRewardCopiesExactlyOneWithoutOnCreatedAndNeverConsumesLivePack(){
        var pack=empty(9);pack.set(1,new ItemStack(Items.IRON_INGOT,4));pack.set(8,new ItemStack(Items.GOLD_INGOT,6));
        var draws=new Draws(List.of(99,1,0,1),false,true);
        var plan=PechTrade.prepare(1,pack,List.of(new PechTradeTables.Entry(1,new ItemStack(Items.DIRT))),List::of,draws);
        assertNotNull(plan);draws.exhausted();assertTrue(plan.outputs().getFirst().is(Items.GOLD_INGOT));assertEquals(5,plan.pack().get(8).getCount());assertEquals(6,pack.get(8).getCount());assertTrue(plan.craftedRewards().isEmpty());
    }
    @Test void dungeonUsesUniformEntryRejectionAndAcceptsTheInvalidFiftiethBaseStack(){
        var integers=new ArrayList<Integer>(List.of(99,1,4));for(int i=0;i<50;i++)integers.add(0);
        var draws=new Draws(integers,false,true);var calls=new AtomicInteger();
        var plan=PechTrade.prepare(5,empty(9),List.of(new PechTradeTables.Entry(5,new ItemStack(Items.DIRT))),()->{calls.incrementAndGet();return List.of(new ThaumcraftLoot.DungeonEntry(new ItemStack(Items.SADDLE),6,20,10));},draws);
        assertNotNull(plan);draws.exhausted();assertEquals(1,calls.get());assertEquals(53,draws.bounds.size());assertTrue(plan.outputs().getFirst().is(Items.SADDLE));assertEquals(1,plan.outputs().getFirst().getCount(),"Copy descriptor base: no min/max count roll");
    }
    @Test void dungeonGateUsesInheritedWeightAndMaximumCountNotMinimumCount(){
        var draws=new Draws(List.of(99,1,4,0),false,true);
        var plan=PechTrade.prepare(5,empty(9),List.of(new PechTradeTables.Entry(5,new ItemStack(Items.DIRT))),
                ()->List.of(new ThaumcraftLoot.DungeonEntry(new ItemStack(Items.GOLDEN_HORSE_ARMOR),1,1,2)),draws);
        assertNotNull(plan);draws.exhausted();assertEquals(List.of(100,5,5,1),draws.bounds);
        assertTrue(plan.outputs().getFirst().is(Items.GOLDEN_HORSE_ARMOR),"weight2/max1 passes on FIRST entry, despite weight>1");
        var integers=new ArrayList<Integer>(List.of(99,1,4));for(int i=0;i<50;i++)integers.add(0);
        draws=new Draws(integers,false,true);
        plan=PechTrade.prepare(5,empty(9),List.of(new PechTradeTables.Entry(5,new ItemStack(Items.DIRT))),
                ()->List.of(new ThaumcraftLoot.DungeonEntry(new ItemStack(Items.GOLD_INGOT),1,3,5)),draws);
        assertNotNull(plan);draws.exhausted();assertEquals(53,draws.bounds.size(),"min1/max3/weight5 rejects49 times, then unconditional50th");
    }
    @Test void missingTierHasFiniteOriginalRejectionBudgetAndDoesNotChangePack(){
        var integers=new ArrayList<Integer>(List.of(99,1,0));for(int i=0;i<PechTrade.MAX_TIER_REJECTIONS;i++)integers.add(0);
        var draws=new Draws(integers,false,false);var pack=empty(9);pack.set(0,new ItemStack(Items.DIAMOND,4));
        assertNull(PechTrade.prepare(1,pack,List.of(new PechTradeTables.Entry(5,new ItemStack(Items.DIRT))),List::of,draws));draws.exhausted();assertEquals(4,pack.get(0).getCount());
    }
    @Test void nullDungeonBaseCannotRefundForeverOrSpendLivePack(){
        var draws=new LegacyRandomSource(1){int rounds;@Override public int nextInt(int bound){if(bound==100)return 99;if(bound==5)return 4;if(bound==3)return 0;return 0;}@Override public boolean nextBoolean(){return true;}};
        var pack=empty(9);pack.set(0,new ItemStack(Items.DIAMOND,4));var calls=new AtomicInteger();
        assertNull(PechTrade.prepare(5,pack,List.of(new PechTradeTables.Entry(5,new ItemStack(Items.DIRT))),()->{calls.incrementAndGet();return List.of(new ThaumcraftLoot.DungeonEntry(ItemStack.EMPTY,1,1,1));},draws));
        assertTrue(calls.get()<=PechTrade.MAX_ROUNDS);assertEquals(4,pack.get(0).getCount());
    }
    @Test void outputOverflowRefusesAtomicallyAndComponentIdentityControlsMerging(){
        var output=empty(4);for(int i=0;i<3;i++)output.set(i,new ItemStack(Items.SADDLE));output.set(3,new ItemStack(Items.GOLD_INGOT,63));
        ItemStack input=new ItemStack(Items.GOLD_INGOT,2);assertFalse(PechTrade.merge(output,input));assertEquals(63,output.get(3).getCount());assertEquals(2,input.getCount());
        ItemStack named=new ItemStack(Items.GOLD_INGOT);named.set(DataComponents.CUSTOM_NAME,Component.literal("different"));assertFalse(PechTrade.merge(output,named));assertEquals(63,output.get(3).getCount());
        assertTrue(PechTrade.merge(output,new ItemStack(Items.GOLD_INGOT)));assertEquals(64,output.get(3).getCount());
    }
}
