package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemKnowledgeFragment;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.AspectPoolPayload;
import thaumcraft.common.testing.NodeTransductionRuntimeChecks.InputRandom;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;

/** Explicit physical fragments/seeded RNG inputs; native Item/game-mode use, no survival acquisition claim. */
public final class KnowledgeFragmentRuntimeChecks {
    private static final List<String> PRIMALS=List.of("aer","terra","ignis","aqua","ordo","perditio");
    private KnowledgeFragmentRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        try(var f=new Fixture(test)) {
            f.open();var knowledge=KnowledgeAccess.get(f.player);knowledge.reset();knowledge.setInitializedAspects(true);
            knowledge.setWarpPerm(2);knowledge.setWarpSticky(3);knowledge.setWarpTemp(4);knowledge.setWarpCounter(-1);knowledge.scanItem("#fragment-unrelated");
            var item=ThaumcraftContent.MATERIALS.get("knowledge_fragment").get();
            f.check(item instanceof ItemKnowledgeFragment && item.getDefaultInstance().getMaxStackSize()==64,"native original metadata9 has real consumable class and ordinary64 stack, not an inert resource template");
            var stack=new ItemStack(item,3);stack.set(DataComponents.CUSTOM_NAME,Component.literal("physical-fragment-components"));f.player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var before=stack.copy();var expected=new ArrayList<CustomPacketPayload>();long seed=1706;var draws=RandomSource.create(seed);
            for(String tag:PRIMALS) { int amount=draws.nextInt(2)+1;expected.add(new AspectPoolPayload(tag,(short)amount,(short)amount)); }
            KnowledgeNotificationRuntimeChecks.feedback(f);f.level.random.setSeed(seed);
            f.check(f.player.gameMode.useItem(f.player,f.level,stack,InteractionHand.MAIN_HAND).consumesAction(),"native survival RMB consumes one real fragment");
            f.check(f.player.getMainHandItem().getCount()==2&&ItemStack.isSameItemSameComponents(f.player.getMainHandItem(),before)
                            &&KnowledgeNotificationRuntimeChecks.feedback(f).equals(expected)&&f.level.random.nextLong()==draws.nextLong(),
                    "one native physical unit pays six original ordered independent bound2 draws, preserves components and emits exact real per-primal RP packets");
            for(var packet:expected) { var point=(AspectPoolPayload)packet;f.check(knowledge.getAspectPoolFor(point.key())==point.amount(),"actual independent1..2 RP for original primal "+point.key()); }
            f.check(knowledge.getResearchComplete().isEmpty()&&knowledge.getScannedItems().contains("#fragment-unrelated")&&knowledge.getWarpPerm()==2&&knowledge.getWarpSticky()==3
                            &&knowledge.getWarpTemp()==4&&knowledge.getWarpCounter()==-1,"fragment use changes only primal pools, never unrelated research/scans/warp/counter");

            f.mode(GameType.CREATIVE);var held=f.player.getMainHandItem();var pools=knowledge.getAspectsDiscovered();
            f.level.random.setSeed(1707);f.player.gameMode.useItem(f.player,f.level,held,InteractionHand.MAIN_HAND);
            var creative=KnowledgeNotificationRuntimeChecks.feedback(f);
            f.check(held.getCount()==2&&creative.size()==6&&creative.stream().allMatch(value->value instanceof AspectPoolPayload point&&point.amount()>=1&&point.amount()<=2),
                    "original creative use is free but still awards six actual primal point draws once, not a new permission/research-gated refusal");
            for(String tag:PRIMALS)f.check(knowledge.getAspectPoolFor(tag)>pools.get(tag),"creative actual grant includes every original primal "+tag);

            f.mode(GameType.SURVIVAL);var offhand=new ItemStack(item);f.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.player.setItemInHand(InteractionHand.OFF_HAND,offhand);
            f.player.gameMode.useItem(f.player,f.level,offhand,InteractionHand.OFF_HAND);
            f.check(f.player.getOffhandItem().isEmpty()&&KnowledgeNotificationRuntimeChecks.feedback(f).size()==6,"native modern offhand pays its exact one physical fragment and gets the same six original grants");

            var guarded=new ItemStack(item,2);f.player.setItemInHand(InteractionHand.MAIN_HAND,guarded);var unchanged=knowledge.save();var replacement=new ItemStack(item,2);int[] callbacks={0};
            try(var rng=new InputRandom(f,bound->{
                f.check(bound==2,"fragment prepayment draws only original bound2");
                if(callbacks[0]++==0) {
                    f.check(!item.use(f.level,f.player,InteractionHand.MAIN_HAND).getResult().consumesAction(),"native world-RNG reentry cannot enter a second fragment frame");
                    f.player.setItemInHand(InteractionHand.MAIN_HAND,replacement);
                }
                return 0;
            },true)) {
                f.check(!f.player.gameMode.useItem(f.player,f.level,guarded,InteractionHand.MAIN_HAND).consumesAction(),"actual physical hand replacement during native RNG planning invalidates the whole unpaid frame");
                f.check(rng.bounds.equals(List.of(2,2,2,2,2,2)),"source fence follows exactly six original draws, never an invented retry or partial grant");
            }
            f.check(guarded.getCount()==2&&replacement.getCount()==2&&knowledge.save().equals(unchanged)&&KnowledgeNotificationRuntimeChecks.feedback(f).isEmpty(),
                    "reentrant source replacement cannot debit detached/other stack, grant free RP or publish paid feedback");
            f.mode(GameType.SPECTATOR);f.player.setItemInHand(InteractionHand.MAIN_HAND,replacement);
            f.check(!f.player.gameMode.useItem(f.player,f.level,replacement,InteractionHand.MAIN_HAND).consumesAction()&&knowledge.save().equals(unchanged)
                            &&replacement.getCount()==2&&KnowledgeNotificationRuntimeChecks.feedback(f).isEmpty(),"native spectator refusal has no free fragment mutation or notification");
        }
        test.succeed();
    }
}
