package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ItemApi;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ArcaneRecipes;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

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

    /**
     * {@code ItemApi.getBlock(field, meta)} gives the stack a player holds. The block's own item carries the state of
     * the metadata as {@code minecraft:block_state}; a variant item of its own (the obsidian tile) carries nothing, even
     * though the loaders map the block to that variant item, the last block item registered for it. Thaumic Tinkerer's
     * Osmotic Enchanter asks for five {@code blockCosmeticSolid:1}: with the state on the recipe's tile no crafted tile
     * matched, and the matrix never started.
     */
    public static void legacyBlockStacksMatchTheStacksPlayersHold(GameTestHelper test) {
        ItemStack tile = ItemApi.getBlock("blockCosmeticSolid", 1);
        ItemStack crafted = new ItemStack(ThaumcraftEldritchBlocks.OBSIDIAN_TILE_ITEM.get());
        test.assertTrue(tile != null && tile.is(crafted.getItem()), "blockCosmeticSolid:1 is the obsidian tile: " + tile);
        test.assertTrue(tile.get(DataComponents.BLOCK_STATE) == null,
                "the obsidian tile places its variant itself and carries no state: " + tile.getComponentsPatch());
        test.assertTrue(InfusionRecipe.areItemStacksEqual(crafted, tile, true), "a crafted obsidian tile is the one a recipe asks for");
        InfusionRecipe enchanter = new InfusionRecipe("", new ItemStack(Items.DIAMOND), 0, new AspectList(),
                new ItemStack(Items.ENCHANTING_TABLE), new ItemStack[] { tile.copy(), tile.copy() });
        test.assertTrue(enchanter.matches(new ArrayList<>(List.of(crafted.copy(), crafted.copy())), new ItemStack(Items.ENCHANTING_TABLE),
                test.getLevel(), null), "an add-on infusion of legacy obsidian tiles takes crafted tiles");

        ItemStack charged = ItemApi.getBlock("blockCosmeticSolid", 8);
        BlockState chargedState = ItemApi.legacyBlockState("blockCosmeticSolid", 8);
        test.assertTrue(charged != null && charged.is(ThaumcraftEldritchBlocks.COSMETIC_SOLID_TOUGH_ITEM.get()),
                "blockCosmeticSolid:8 is the totem block's own item: " + charged);
        BlockItemStateProperties carried = charged.get(DataComponents.BLOCK_STATE);
        test.assertTrue(carried != null && carried.apply(ThaumcraftEldritchBlocks.COSMETIC_SOLID_TOUGH.get().defaultBlockState()) == chargedState,
                "the charged totem keeps its variant although the obsidian tile is registered after the totem's own item: " + carried);

        List<String> wrong = new ArrayList<>();
        for (String field : ItemApi.legacyNames()) {
            for (int meta : ItemApi.legacyMetadata(field)) {
                ResourceLocation blockId = ItemApi.legacyBlockId(field, meta);
                ResourceLocation itemId = ItemApi.legacyItemId(field, meta);
                if (meta == ItemApi.WILDCARD || blockId == null || itemId == null) continue;
                ItemStack stack = ItemApi.getBlock(field, meta);
                if (stack == null) {
                    wrong.add(field + ":" + meta + " gives no stack");
                    continue;
                }
                BlockItemStateProperties state = stack.get(DataComponents.BLOCK_STATE);
                if (!itemId.equals(blockId)) {
                    if (state != null) wrong.add(field + ":" + meta + " variant item " + itemId + " carries " + state);
                    continue;
                }
                BlockState expected = ItemApi.legacyBlockState(field, meta);
                if (expected == null || !(stack.getItem() instanceof BlockItem own) || expected.getBlock() != own.getBlock()) continue;
                BlockState placed = state == null ? own.getBlock().defaultBlockState() : state.apply(own.getBlock().defaultBlockState());
                if (placed != expected) wrong.add(field + ":" + meta + " places " + placed + ", not " + expected);
            }
        }
        test.assertTrue(wrong.isEmpty(), "legacy block stacks that differ from what their metadata names: " + wrong);
        test.succeed();
    }
}
