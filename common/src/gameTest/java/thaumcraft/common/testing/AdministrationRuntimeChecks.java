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
            test.assertTrue(dispatcher.execute("tc research grant FLUXSCRUB", source) == 1
                            && knowledge.isResearchComplete("INFUSION") && knowledge.isResearchComplete("JARLABEL"),
                    "registered tc alias recursively grants the hidden parent and distillation sibling");
            int counter = knowledge.getWarpCounter();
            test.assertTrue(dispatcher.execute("thaum warp @s set 17 TEMP", source) == 1
                            && knowledge.getWarpTemp() == 17 && knowledge.getWarpCounter() == counter,
                    "native self-selector, thaum alias and TEMP setter preserve the old event-counter quirk");
            test.assertTrue(dispatcher.execute("thaumcraft warp @s add -100", source) == 1 && knowledge.getWarpSticky() == 0,
                    "default warp command changes normal sticky warp and clamps at zero");
            knowledge.setAspectPool("aer", 33000);
            test.assertTrue(dispatcher.execute("thaumcraft aspect @s Aer 32768", source) == 1 && knowledge.getAspectPoolFor("aer") == 232,
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
