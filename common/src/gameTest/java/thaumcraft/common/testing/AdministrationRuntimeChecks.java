package thaumcraft.common.testing;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Executes the registered native dispatcher, including selector resolution and permission refusals. */
public final class AdministrationRuntimeChecks {
    private AdministrationRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        var knowledge = KnowledgeAccess.get(player);
        var saved = knowledge.copy();
        var dispatcher = test.getLevel().getServer().getCommands().getDispatcher();
        var source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        try {
            knowledge.reset();
            var empty = knowledge.save();
            test.assertTrue(dispatcher.execute("tc HeLp ignored unused \"", source) == 1
                            && dispatcher.execute("thaumcraft help ignored unused", source) == 1
                            && knowledge.save().equals(empty),
                    "original mixed-case HELP and native help ignore arbitrary unused tail without any knowledge mutation");
            int listed = thaumcraft.common.lib.research.ResearchRegistrationOrder.enabled().size();
            test.assertTrue(dispatcher.execute("thaumcraft research LIST unused tokens", source) == listed
                            && dispatcher.execute("tc ReSeArCh LiSt unused tokens", source) == listed
                            && dispatcher.execute("thaum research list unused tokens", source) == listed
                            && knowledge.save().equals(empty),
                    "both literal research node and mixed root retain original case-insensitive LIST and ignored extras");
            test.assertTrue(dispatcher.execute("tc ReSeArCh @s RESEARCHER1", source) == 1
                            && knowledge.isResearchComplete("RESEARCHER1"),
                    "mixed-case original research action still resolves the native target and grants the exact key");
            test.assertTrue(dispatcher.execute("tc research grant FLUXSCRUB", source) == 1
                            && knowledge.isResearchComplete("INFUSION") && knowledge.isResearchComplete("JARLABEL"),
                    "registered tc alias recursively grants the hidden parent and distillation sibling");
            int counter = knowledge.getWarpCounter();
            test.assertTrue(dispatcher.execute("thaumcraft warp @s ADD 3 PeRm", source) == 1
                            && knowledge.getWarpPerm() >= 3 && knowledge.getWarpCounter() == counter,
                    "lowercase native warp node accepts mixed-case ADD and exactly one PERM type without touching event counter");
            int permanent = knowledge.getWarpPerm(), temporary = knowledge.getWarpTemp(), sticky = knowledge.getWarpSticky();
            test.assertTrue(dispatcher.execute("thaumcraft warp @s add 7 TEMP ignored", source) == 1
                            && knowledge.getWarpSticky() == sticky + 7 && knowledge.getWarpTemp() == temporary,
                    "fully lowercase warp with more than five original arguments ignores TEMP and all extras, selecting sticky");
            test.assertTrue(dispatcher.execute("tc WaRp @s SeT 9 PERM ignored \"", source) == 1
                            && knowledge.getWarpSticky() == 9 && knowledge.getWarpPerm() == permanent,
                    "mixed warp SET with unused trailing tokens preserves the original sticky fallback and ignores malformed unused tail");
            String quotedSelf = "@s[name=\"" + player.getGameProfile().getName() + "\"]";
            test.assertTrue(dispatcher.execute("thaumcraft warp " + quotedSelf + " SeT 13 tEmP", source) == 1
                            && knowledge.getWarpTemp() == 13,
                    "normalization preserves raw native quoted selector syntax and same source identity");
            test.assertTrue(dispatcher.execute("thaum warp @s set 17 TEMP", source) == 1
                            && knowledge.getWarpTemp() == 17 && knowledge.getWarpCounter() == counter,
                    "native self-selector, thaum alias and TEMP setter preserve the old event-counter quirk");
            test.assertTrue(dispatcher.execute("thaumcraft warp @s add -100", source) == 1 && knowledge.getWarpSticky() == 0,
                    "default warp command changes normal sticky warp and clamps at zero");
            knowledge.setAspectPool("aer", 33000);
            test.assertTrue(dispatcher.execute("thaumcraft AsPeCt @s Aer 32768", source) == 1 && knowledge.getAspectPoolFor("aer") == 232,
                    "native aspect parser retains case-insensitive name fallback and signed-short amount narrowing");
            var before = knowledge.save();
            try {
                dispatcher.execute("thaumcraft warp @s add 101", source);
                test.fail("out-of-range warp command accepted");
            } catch (CommandSyntaxException expected) {
                test.assertTrue(knowledge.save().equals(before), "invalid parsed amount cannot mutate target state");
            }
            try {
                dispatcher.execute("tc research grant RESEARCHER2", source.withPermission(0));
                test.fail("unprivileged admin alias accepted");
            } catch (CommandSyntaxException expected) {
                test.assertTrue(knowledge.save().equals(before), "alias permissions are checked by the actual command tree");
            }
            for (String denied : new String[]{"tc HeLp ignored", "thaumcraft research LIST ignored", "tc WaRp @s ADD 1 TEMP ignored", "tc AsPeCt @s aer 5"}) {
                try { dispatcher.execute(denied, source.withPermission(0)); test.fail("unprivileged original spelling accepted: " + denied); }
                catch (CommandSyntaxException expected) { test.assertTrue(knowledge.save().equals(before), "canonical normalization retains native permission refusal without free mutation"); }
            }
            for (String invalid : new String[]{"thaumcraft warp @s ADD 101 TEMP ignored", "tc WaRp @s SeT -1", "tc AsPeCt @s aer 0", "tc ReSeArCh @s RESEARCHER2 unused", "tc unknown @s add 1",
                    "tc help;unused", "tc research LIST:unused", "tc warp @s ADD 1junk ignored", "tc WaRp @s SeT 12.5 PERM ignored"}) {
                try { dispatcher.execute(invalid, source); test.fail("invalid original action accepted: " + invalid); }
                catch (CommandSyntaxException expected) { test.assertTrue(knowledge.save().equals(before), "known-action-only normalization preserves native ranges and exact research/aspect arity"); }
            }
            test.assertTrue(dispatcher.execute("thaumcraft research @s reset", source) == 1
                            && !knowledge.isResearchComplete("FLUXSCRUB") && knowledge.isResearchComplete("RESEARCH")
                            && knowledge.getAspectPoolFor("aer") == 232 && knowledge.getWarpTemp() == 17,
                    "native reset restores AutoUnlock only, retaining pools and warp");
        } catch (CommandSyntaxException invalid) {
            throw new IllegalStateException("Native admin command failed", invalid);
        } finally { knowledge.copyFrom(saved); }
        test.succeed();
    }
}
