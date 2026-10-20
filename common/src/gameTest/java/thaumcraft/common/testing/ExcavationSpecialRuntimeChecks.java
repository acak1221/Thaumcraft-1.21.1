package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoublePredicate;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;
import thaumcraft.common.lib.utils.SpecialMining;

/** Actual connected native channels/loot/XP/protection for Silk8 and Dowsing20; seed sampling is declared only for their probability cases. */
public final class ExcavationSpecialRuntimeChecks {
    private ExcavationSpecialRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        try (var actor = new FocusUpgradeRuntimeChecks.Actor(test)) {
            upgrades(actor); silk(actor); dowsing(actor, false); dowsing(actor, true); focusFortune(actor); enlarged(actor); refused(actor);
        }
        test.succeed();
    }
    private static ItemStack silkFocus(FocusUpgradeRuntimeChecks.Actor a) {
        return a.focus(ThaumcraftFoci.EXCAVATION_FOCUS.get(), FocusUpgradeType.potency,1, FocusUpgradeType.silktouch,5);
    }
    private static ItemStack dowsingFocus(FocusUpgradeRuntimeChecks.Actor a, boolean silk) {
        return silk ? a.focus(ThaumcraftFoci.EXCAVATION_FOCUS.get(), FocusUpgradeType.treasure,1, FocusUpgradeType.dowsing,3, FocusUpgradeType.silktouch,5)
                : a.focus(ThaumcraftFoci.EXCAVATION_FOCUS.get(), FocusUpgradeType.treasure,1, FocusUpgradeType.dowsing,3);
    }
    private static void upgrades(FocusUpgradeRuntimeChecks.Actor a) {
        var item = ThaumcraftFoci.EXCAVATION_FOCUS.get();
        a.require(FocusUpgradeType.get(20) == FocusUpgradeType.dowsing && FocusUpgradeType.dowsing.getAspects().getAmount(thaumcraft.api.aspects.Aspect.MINE) == 1,
                "original Dowsing20 costs one Mining aspect in the closed upgrade catalogue");
        ItemStack focus = new ItemStack(item);
        ItemStack malformed = new ItemStack(item);
        malformed.set(ThaumcraftFoci.FOCUS_UPGRADES.get(), new FocusUpgradeData(List.of((short)-1,(short)20,(short)-1,(short)-1,(short)-1)));
        var invalidTooltip = new ArrayList<Component>();
        item.appendHoverText(malformed, Item.TooltipContext.of(a.level), invalidTooltip, TooltipFlag.NORMAL);
        a.require(!FocusData.isSupportedFocus(malformed) && invalidTooltip.stream().anyMatch(line -> line.getContents()
                        instanceof net.minecraft.network.chat.contents.TranslatableContents translation
                        && translation.getKey().equals("tc.port.focus.upgrades.invalid")),
                "raw Dowsing in illegal rank2 gets the actual invalid tooltip without evaluating a crashing specialization price");
        a.require(!item.applyUpgrade(focus, FocusUpgradeType.dowsing,2) && !item.applyUpgrade(focus, FocusUpgradeType.silktouch,4), "special upgrades cannot occupy another rank");
        a.require(item.applyUpgrade(focus, FocusUpgradeType.dowsing,3) && item.applyUpgrade(focus, FocusUpgradeType.silktouch,5)
                        && FocusData.isSupportedFocus(focus), "one real focus carries rank3 Dowsing and rank5 Silk together");
        var shown = item.getVisCost(focus);
        a.require(shown.size()==6 && shown.getAmount(thaumcraft.api.aspects.Aspect.EARTH)==16
                        || shown.size()==3 && shown.getAmount(thaumcraft.api.aspects.Aspect.EARTH)==15
                        && shown.getAmount(thaumcraft.api.aspects.Aspect.FIRE)==2 && shown.getAmount(thaumcraft.api.aspects.Aspect.ORDER)==2,
                "native focus tooltip uses one of the original first-initialized shared-cost2 branches");
        a.require(item.getVisCost(dowsingFocus(a,false)).aspects.equals(shown.aspects),
                "native Dowsing and Silk tooltips share the same already initialized cost2");
        shown.aspects.clear();
        a.require(item.getVisCost(focus).size()>0, "a returned mutable AspectList cannot corrupt the shared tooltip cache");
        a.equip(focus, "gold",100);
        a.require(item.getVisCost(a.wand).getAmount(thaumcraft.api.aspects.Aspect.EARTH) == 15 && item.getVisCost(a.wand).size() == 1
                        && ItemFocusExcavation.attemptCost(a.wand,a.player) == 15,
                "the actual outer-wand getVisCost quirk remains base15terra, independent of the nested tooltip surcharge");
        var encoded = a.wand.save(a.level.registryAccess());
        a.require(ItemStack.matches(a.wand, ItemStack.parse(a.level.registryAccess(),encoded).orElseThrow()), "native nested focus codec retains actual Silk8/Dowsing20 slots");
    }
    private static void prime(FocusUpgradeRuntimeChecks.Actor a, ItemStack focus, BlockState state) {
        a.clearLane(); a.equip(focus,"gold",100); a.aim(); a.put(a.root,state); a.begin();
    }
    private static void complete(FocusUpgradeRuntimeChecks.Actor a) {
        for (int tick = 0; tick < 1024 && !a.level.isEmptyBlock(a.root); tick++) a.tick(1);
        a.require(a.level.isEmptyBlock(a.root), "bounded actual held channel removes its looked-at source");
        a.player.stopUsingItem();
    }
    private static int count(FocusUpgradeRuntimeChecks.Actor a, Item item) {
        return a.items().stream().filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
    }
    private static int xp(FocusUpgradeRuntimeChecks.Actor a) {
        return a.level.getEntitiesOfClass(ExperienceOrb.class,a.bounds).stream().mapToInt(ExperienceOrb::getValue).sum();
    }
    private static void exactPrice(FocusUpgradeRuntimeChecks.Actor a,int removals) {
        for (String primal : WandData.PRIMALS) a.require(a.vis(primal) == (primal.equals("terra") ? 100-15*removals : 100),
                "actual special casting pays only original15terra per attempt, with no tooltip surcharge: "+primal);
    }
    private static void silk(FocusUpgradeRuntimeChecks.Actor a) {
        for (Block block : List.of(Blocks.STONE,Blocks.GLASS,Blocks.ICE,Blocks.COAL_ORE)) {
            prime(a,silkFocus(a),block.defaultBlockState());
            // Neither the original object's enchantments nor its focus are rewritten when the private loot tool receives Silk.
            a.wand.enchant(a.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),3);
            var before = a.wand.get(DataComponents.ENCHANTMENTS);
            complete(a); exactPrice(a,1);
            a.require(count(a,block.asItem()) == 1 && a.items().stream().mapToInt(entity -> entity.getItem().getCount()).sum() == 1 && xp(a) == 0,
                    "native Silk returns exactly the real "+block+" once and no XP; Ice remains air instead of running playerDestroy water");
            a.require(before.equals(a.wand.get(DataComponents.ENCHANTMENTS)), "actual enchanted wand is not the mutable private loot tool");
        }
        prime(a,silkFocus(a),Blocks.SPAWNER.defaultBlockState());
        complete(a); exactPrice(a,1);
        a.require(a.items().isEmpty() && xp(a) >= 15 && xp(a) <= 43,
                "original non-Silk-harvestable spawner keeps ordinary XP even when the focus has Silk Touch");
    }
    private static ItemStack lootTool(FocusUpgradeRuntimeChecks.Actor a, boolean silk) {
        ItemStack tool = a.wand.copyWithCount(1);
        tool.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY); tool.remove(DataComponents.STORED_ENCHANTMENTS);
        var registry = a.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        tool.enchant(registry.getOrThrow(Enchantments.FORTUNE),1);
        if (silk) tool.enchant(registry.getOrThrow(Enchantments.SILK_TOUCH),1);
        return tool;
    }
    private static void dowsing(FocusUpgradeRuntimeChecks.Actor a,boolean silk) {
        BlockState ore = silk ? Blocks.DEEPSLATE_IRON_ORE.defaultBlockState() : Blocks.IRON_ORE.defaultBlockState();
        prime(a,dowsingFocus(a,silk),ore);
        a.tick(1); // Prime the coordinate and its one initial rumble before the declared world-RNG seed sample.
        Item cluster = ThaumcraftContent.MATERIALS.get("cluster_iron").get();
        ResourceLocation lootSequence = ResourceLocation.withDefaultNamespace(silk ? "blocks/deepslate_iron_ore" : "blocks/iron_ore");
        long lootSeed = 0x544334D0L;
        List<ItemStack> expected = null; long chosen = -1;
        // LegacyRandomSource's first floats for seeds0..4095 are all .58..95,
        // above Treasure1's .275. Select a declared low-roll seed from a wider
        // bounded domain, then still witness the ACTUAL native loot pipeline.
        for (long seed = 0; seed < 100000; seed++) {
            if (RandomSource.create(seed).nextFloat() >= .05F) continue;
            a.level.random.setSeed(seed);
            // Modern Fortune uses a separate named loot RNG. World seeding
            // alone repeats Dowsing's chance but not the native raw-ore count.
            a.level.getRandomSequence(lootSequence).setSeed(lootSeed);
            var drops = Block.getDrops(ore,a.level,a.root,null,a.player,lootTool(a,silk));
            if (drops.size() == 1 && drops.getFirst().is(cluster)) { chosen=seed;expected=drops;break; }
        }
        a.require(chosen >= 0 && expected != null, "declared actual native loot seed reaches the original Dowsing probability branch");
        int amount = expected.getFirst().getCount();
        float hardness = ore.getDestroySpeed(a.level,a.root);
        for (int tick = 0; tick < 1024; tick++) {
            var progress = ItemFocusExcavation.progress(a.player);
            if (progress != null && progress.amount() >= hardness) break;
            a.tick(1);
        }
        a.require(!a.level.isEmptyBlock(a.root) && a.vis("terra") == 100, "no payment or output before the completed original progress boundary");
        a.level.random.setSeed(chosen);
        a.level.getRandomSequence(lootSequence).setSeed(lootSeed);
        a.tick(1); a.player.stopUsingItem();
        a.require(a.level.isEmptyBlock(a.root) && count(a,cluster) == amount
                        && a.items().stream().mapToInt(entity -> entity.getItem().getCount()).sum() == amount && xp(a) == 0,
                "actual Dowsing "+(silk?"Silk/deep ore":"raw ore/Fortune1")+" gives precisely its witnessed cluster count, once; worldSeed="+chosen
                        +"; lootSequence="+lootSequence+"; lootSeed="+lootSeed+"; expected="+expected+"; actual="+a.items().stream().map(entity->entity.getItem().toString()).toList()
                        +"; source="+a.level.getBlockState(a.root)+"; XP="+xp(a)+"; terra="+a.vis("terra"));
        exactPrice(a,1);
        a.tick(2);
        a.require(count(a,cluster) == amount, "completed special cast does not duplicate its cluster output on later player ticks");
    }
    private static long seed(DoublePredicate accepts) {
        for (long seed=0;seed<100000;seed++) if (accepts.test(RandomSource.create(seed).nextFloat())) return seed;
        throw new AssertionError("No declared probability fixture seed");
    }
    private static void focusFortune(FocusUpgradeRuntimeChecks.Actor a) {
        a.clearLane(); a.equip(dowsingFocus(a,false),"gold",100);
        a.wand.enchant(a.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),10);
        Item cluster=ThaumcraftContent.MATERIALS.get("cluster_iron").get();
        a.level.random.setSeed(seed(roll -> roll>.3 && roll<.4));
        var miss=SpecialMining.apply(List.of(new ItemStack(Items.RAW_IRON,2)),a.level,a.root,a.player,a.wand);
        a.require(miss.size()==1 && miss.getFirst().is(Items.RAW_IRON) && miss.getFirst().getCount()==2,
                "Dowsing uses actual focus Treasure1 chance0.275; a forged native Fortune10 enchant cannot raise it");
        a.level.random.setSeed(seed(roll -> roll<.05));
        var hit=SpecialMining.apply(List.of(new ItemStack(Items.RAW_IRON,2)),a.level,a.root,a.player,a.wand);
        a.require(hit.size()==1 && hit.getFirst().is(cluster) && hit.getFirst().getCount()==2 && a.items().isEmpty(),
                "special-mining list transformation preserves input amount and does not physically publish a helper output");
    }
    private static void enlarged(FocusUpgradeRuntimeChecks.Actor a) {
        ItemStack focus=a.focus(ThaumcraftFoci.EXCAVATION_FOCUS.get(),FocusUpgradeType.treasure,1,FocusUpgradeType.enlarge,2,
                FocusUpgradeType.dowsing,3,FocusUpgradeType.enlarge,4,FocusUpgradeType.silktouch,5);
        prime(a,focus,Blocks.STONE.defaultBlockState());
        for (Direction face:List.of(Direction.NORTH,Direction.SOUTH,Direction.WEST)) a.put(a.root.relative(face),Blocks.STONE.defaultBlockState());
        complete(a); exactPrice(a,3);
        a.require(count(a,Items.STONE)==3 && count(a,Items.COBBLESTONE)==0 && xp(a)==0,
                "Enlarge2 carries the actual Silk/Dowsing focus through two separately paid native extras and exact block drops");
    }
    private static void refused(FocusUpgradeRuntimeChecks.Actor a) {
        prime(a,dowsingFocus(a,true),Blocks.DIAMOND_ORE.defaultBlockState());
        BlockEvent.Break listener=(world,pos,state,actor,experience)->world==a.level && pos.equals(a.root) && actor==a.player
                ? EventResult.interruptFalse() : EventResult.pass();
        BlockEvent.BREAK.register(listener);
        try {
            for (int tick=0;tick<1024 && a.vis("terra")==100;tick++) a.tick(1);
            a.player.stopUsingItem();
        } finally { BlockEvent.BREAK.unregister(listener); }
        a.require(a.level.getBlockState(a.root).is(Blocks.DIAMOND_ORE) && a.items().isEmpty() && xp(a)==0,
                "real native protection refusal leaves the source and creates no Silk ore, Dowsing output or XP");
        exactPrice(a,1); // Original pays a completed attempt before break permission is returned.
    }
}
