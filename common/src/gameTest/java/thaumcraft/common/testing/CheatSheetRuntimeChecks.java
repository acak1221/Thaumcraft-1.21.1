package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.items.relics.ItemThaumonomicon;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchRegistrationOrder;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;

/** Real connected actor and registered item/tab. Explicit server-settings fixtures are restored after the case. */
public final class CheatSheetRuntimeChecks {
    private CheatSheetRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        ResearchSettings original = ThaumcraftSettings.server();
        try (var f = new Fixture(test)) {
            f.open();
            var knowledge = KnowledgeAccess.get(f.player);
            var cheat = new ItemStack(ThaumcraftContent.THAUMONOMICON_CHEAT.get());
            var normal = new ItemStack(ThaumcraftContent.THAUMONOMICON.get());
            f.check(cheat.getItem() instanceof ItemThaumonomicon book && book.isCheatSheet()
                            && cheat.getRarity() == Rarity.EPIC && cheat.getMaxStackSize() == 1
                            && normal.getRarity() == Rarity.UNCOMMON && normal.getMaxStackSize() == 1,
                    "native metadata42 flattening has EPIC/stacks1 identity, independent from normal uncommon book");
            var expectedBookAspects = thaumcraft.common.lib.research.ObjectAspectResolver.baseTags(new ItemStack(net.minecraft.world.item.Items.BOOKSHELF), f.level).add(Aspect.MAGIC,2).merge(Aspect.MIND,2);
            f.check(thaumcraft.common.lib.research.ObjectAspectResolver.baseTags(normal,f.level).aspects.equals(expectedBookAspects.aspects)
                            && thaumcraft.common.lib.research.ObjectAspectResolver.baseTags(cheat,f.level).aspects.equals(expectedBookAspects.aspects)
                            && thaumcraft.common.lib.research.ObjectAspectResolver.get(normal,f.level).aspects.equals(thaumcraft.common.lib.research.ObjectAspectResolver.get(cheat,f.level).aspects),
                    "actual object-aspect resolver applies original wildcard metadata book binding to BOTH identities, derived from native bookshelf plus magic2/merge mind2");
            var tooltip = new ArrayList<net.minecraft.network.chat.Component>();
            cheat.getItem().appendHoverText(cheat, net.minecraft.world.item.Item.TooltipContext.of(f.level), tooltip, TooltipFlag.NORMAL);
            f.check(tooltip.stream().anyMatch(line -> line.getString().equals("Cheat Sheet")), "exact original metadata42 tooltip");

            // Disabled book follows the NORMAL one-pass sibling path, even though item identity/rarity remains special.
            configure(false, false, true, true);
            knowledge.reset(); knowledge.setInitializedAspects(true); knowledge.addResearch("DISTILESSENTIA");
            knowledge.setAspectPool("aer", 7); knowledge.setAspectPool("lux", -3);
            var pools = knowledge.getAspectsDiscovered();
            use(f, cheat);
            f.check(knowledge.isResearchComplete("JARLABEL") && !knowledge.isResearchComplete("PRIMPEARL")
                            && knowledge.getAspectsDiscovered().equals(pools),
                    "server-disabled cheat item grants only normal book's sibling; no extra RP or all-research permission");
            rebuild(f);
            f.check(!hasCheat(), "false current server settings excludes cheat item from actual rebuilt native creative tab");

            configure(true, false, false, false);
            rebuild(f);
            f.check(hasCheat(), "same creative tab callback reads NEW true settings at rebuild, not captured startup defaults");
            knowledge.reset(); knowledge.setInitializedAspects(true); knowledge.addResearch("DISTILESSENTIA");
            knowledge.setAspectPool("aer", 7); knowledge.setAspectPool("lux", -3);
            knowledge.setWarpPerm(5); knowledge.setWarpSticky(7); knowledge.setWarpTemp(3); knowledge.setWarpCounter(-1);
            var beforeResearch = knowledge.getResearchComplete();
            var enabled = ResearchRegistrationOrder.enabled(); var expected = new LinkedHashSet<>(beforeResearch);
            int permanent = 5, sticky = 7;
            for (var research : enabled) {
                expected.add(research.key());
                if (!beforeResearch.contains(research.key())) { permanent += research.warp() - research.warp()/2; sticky += research.warp()/2; }
            }
            use(f, cheat);
            f.check(knowledge.getResearchComplete().equals(expected) && knowledge.isResearchComplete("PRIMPEARL")
                            && !knowledge.isResearchComplete("WARDEDARCANA") && !knowledge.isResearchComplete("MIRROR"),
                    "ALL and ONLY dynamically enabled registered research is granted directly without prerequisites, including lost PRIMPEARL");
            for (Aspect aspect : Aspect.aspects.values()) {
                int amount = aspect.getTag().equals("aer") ? 7 : aspect.getTag().equals("lux") ? -3
                        : thaumcraft.common.lib.research.PlayerKnowledge.PRIMAL_TAGS.contains(aspect.getTag()) ? 0 : 50;
                f.check(knowledge.hasDiscoveredAspect(aspect.getTag()) && knowledge.getAspectPoolFor(aspect.getTag()) == amount,
                        "exact50 only on first discovery; preexisting positive/zero/negative pools retained: " + aspect.getTag());
            }
            f.check(knowledge.getWarpPerm() == permanent && knowledge.getWarpSticky() == sticky && knowledge.getWarpTemp() == 3
                            && knowledge.getWarpCounter() == permanent + sticky + 3,
                    "every newly completed research awards original ceil/floor warp once and sets counter to total");
            var completed = knowledge.save(); use(f, cheat);
            f.check(knowledge.save().equals(completed), "repeated cheat use does not refill RP or award research warp twice");
            f.check(f.player.getMainHandItem() == cheat && cheat.getCount() == 1, "cheat sheet itself is never consumed or replaced");

            configure(true, true, true, true);
            knowledge.reset(); knowledge.setInitializedAspects(true); knowledge.setWarpPerm(5); knowledge.setWarpSticky(7); knowledge.setWarpTemp(3); knowledge.setWarpCounter(42);
            use(f, cheat);
            f.check(knowledge.isResearchComplete("PRIMPEARL") && knowledge.isResearchComplete("WARDEDARCANA")
                            && knowledge.getWarpPerm() == 5 && knowledge.getWarpSticky() == 7 && knowledge.getWarpTemp() == 3 && knowledge.getWarpCounter() == 42,
                    "server Wuss bypasses only newly awarded warp; it still grants enabled research and never resets prior warp/counter");
            configure(false, true, true, true); rebuild(f);
            f.check(!hasCheat(), "dynamic current setting hides same registered cheat identity again");
        } finally {
            ThaumcraftSettings.configureServer(original);
            ThaumcraftContent.TAB.get().buildContents(new CreativeModeTab.ItemDisplayParameters(test.getLevel().enabledFeatures(), false, test.getLevel().registryAccess()));
        }
        test.succeed();
    }
    private static void configure(boolean allow, boolean wuss, boolean wards, boolean mirrors) {
        ThaumcraftSettings.configureServer(new ResearchSettings(allow, wards, mirrors, true, wuss, 0, 100));
    }
    private static void use(Fixture f, ItemStack stack) {
        f.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        f.check(stack.getItem().use(f.level, f.player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "real registered book native use succeeds");
    }
    private static void rebuild(Fixture f) {
        ThaumcraftContent.TAB.get().buildContents(new CreativeModeTab.ItemDisplayParameters(f.level.enabledFeatures(), false, f.level.registryAccess()));
    }
    private static boolean hasCheat() {
        return ThaumcraftContent.TAB.get().getDisplayItems().stream().anyMatch(stack -> stack.is(ThaumcraftContent.THAUMONOMICON_CHEAT.get()));
    }
}
