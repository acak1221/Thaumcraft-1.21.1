package thaumcraft.common.testing;

import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ArcaneRecipes;

/** The add-on lifecycle and the 1.7.10 ore dictionary names of the public API, in a running world. */
public final class AddonApiRuntimeChecks {
    private AddonApiRuntimeChecks() {}

    public static void addonHookRanBeforeTheWorld(GameTestHelper test) {
        test.assertTrue(ThaumcraftApi.isReady(), "the loader fired the IThaumcraftAddon hooks before the world started");
        boolean[] ran = {false};
        ThaumcraftApi.whenReady(() -> ran[0] = true);
        test.assertTrue(ran[0], "a callback registered after that moment runs at once");
        test.succeed();
    }

    /** Every effect of the facade is sent on the server thread without a player in range; no crash, nothing else. */
    public static void effectsOfTheOriginalPacketsAreSent(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        var center = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        thaumcraft.api.ThaumcraftFX.blockSparkle(level, pos, thaumcraft.api.ThaumcraftFX.RANDOM_GREY);
        thaumcraft.api.ThaumcraftFX.zap(level, center, center.add(2, 1, 0));
        thaumcraft.api.ThaumcraftFX.bubbles(level, pos, 0x3cd4fc);
        thaumcraft.api.ThaumcraftFX.essentiaTrail(level, pos, pos.above(2), Aspect.WATER);
        thaumcraft.api.ThaumcraftFX.visDrain(level, pos, pos.above(3), Aspect.AIR);
        boolean refused = false;
        try {
            java.util.concurrent.CompletableFuture.runAsync(() -> thaumcraft.api.ThaumcraftFX.blockSparkle(level, pos, 0xFFFFFF)).join();
        } catch (java.util.concurrent.CompletionException expected) {
            refused = expected.getCause() instanceof IllegalStateException;
        }
        test.assertTrue(refused, "another thread is refused, as the facade documents");
        test.succeed();
    }

    public static void oreDictionaryNamesReachTheTagsOfTheWorld(GameTestHelper test) {
        List<Object> key = List.of(Items.SLIME_BALL, 0);
        AspectList before = ThaumcraftApi.objectTags.get(key);
        try {
            ThaumcraftApi.objectTags.remove(key);
            AspectList slime = new AspectList().add(Aspect.SLIME, 7);
            ThaumcraftApi.registerObjectTag("slimeball", slime);
            test.assertTrue(ThaumcraftApi.objectTags.get(key) == slime, "a 1.7.10 ore name registers the bound members at once");
            // As if the add-on had registered at mod initialization, when no tag was bound yet.
            ThaumcraftApi.objectTags.remove(key);
            ThaumcraftApi.applyTagObjectTags();
            test.assertTrue(ThaumcraftApi.objectTags.get(key) == slime, "binding the tags applies the kept registration");
            AspectList own = new AspectList().add(Aspect.WATER, 1);
            ThaumcraftApi.registerObjectTag(new ItemStack(Items.SLIME_BALL), own);
            ThaumcraftApi.applyTagObjectTags();
            test.assertTrue(ThaumcraftApi.objectTags.get(key) == own, "an item's own registration wins over a tag");

            test.assertTrue(ArcaneRecipes.ingredient("ingotIron").test(new ItemStack(Items.IRON_INGOT)),
                    "the ore name Thaumcraft 4 used is its original tag");
            test.assertTrue(ArcaneRecipes.ingredient("ingotCopper").test(new ItemStack(Items.COPPER_INGOT)),
                    "a conventional ore name is the c: tag");
            test.assertTrue(!ArcaneRecipes.ingredient("ingotCopper").test(new ItemStack(Items.IRON_INGOT)), "and only that material");
        } finally {
            if (before == null) ThaumcraftApi.objectTags.remove(key);
            else ThaumcraftApi.objectTags.put(key, before);
        }
        test.succeed();
    }
}
