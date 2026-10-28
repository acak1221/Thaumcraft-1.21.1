package thaumic.tinkerer.simpleblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.common.block.BlockGas;
import thaumic.tinkerer.common.block.BlockInfusedGrain;
import thaumic.tinkerer.common.block.fire.FireRules;
import thaumic.tinkerer.common.core.helper.AspectCropLootManager;
import thaumic.tinkerer.common.core.helper.NumericAspectHelper;
import thaumic.tinkerer.common.item.InfusedSeedComponents;
import thaumic.tinkerer.common.item.ItemInfusedGrain;
import thaumic.tinkerer.common.item.ItemInfusedPotion;
import thaumic.tinkerer.common.potion.ModPotions;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The plain rules of the SimpleBlocks group that need no running game: the numbering of the soul aspects, the
 * texture groups and variants of the infused crops, the conversion of the seed components, the weighted loot of a
 * crop, and the state rules of the elemental fires. Everything that needs a world, a registry that is filled or the
 * block tags (the flammability of planks, the growth of a crop, the gases) is in the GameTest checks
 * ({@code SimpleBlocksRuntimeChecks}).</p>
 *
 * <p>Written without being able to run Gradle in the porting session: they were compiled by hand only.</p>
 */
class SimpleBlocksLogicTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // ---- NumericAspectHelper -----------------------------------------------------------------------------------

    private static final Aspect[] SOUL_ORDER = { Aspect.WATER, Aspect.MAN, Aspect.AIR, Aspect.FLIGHT, Aspect.FIRE, Aspect.MAGIC,
            Aspect.UNDEAD, Aspect.FLESH, Aspect.BEAST, Aspect.POISON, Aspect.EARTH, Aspect.ELDRITCH, Aspect.TRAVEL, Aspect.METAL,
            Aspect.SLIME, Aspect.CLOTH, Aspect.CROP };

    @Test
    void theSeventeenSoulAspectsKeepTheirOriginalNumbers() {
        assertEquals(17, NumericAspectHelper.values.size());
        for (int i = 0; i < SOUL_ORDER.length; i++) {
            assertEquals(i, NumericAspectHelper.getNumber(SOUL_ORDER[i]), SOUL_ORDER[i].getTag());
            assertSame(SOUL_ORDER[i], NumericAspectHelper.getAspect(i));
        }
    }

    @Test
    void anAspectWithoutASoulHasNoNumberAndInitTwiceDoesNotDoubleTheList() {
        assertEquals(-1, NumericAspectHelper.getNumber(Aspect.VOID));
        NumericAspectHelper.init();
        NumericAspectHelper.init();
        assertEquals(17, NumericAspectHelper.values.size());
        assertEquals(16, NumericAspectHelper.getNumber(Aspect.CROP));
    }

    // ---- the crops and their items -----------------------------------------------------------------------------

    @Test
    void thePrimalAspectsHaveTheirTextureGroupAndTheOthersTheGenericOne() {
        assertEquals(0, BlockInfusedGrain.getNumberFromAspectForTexture(Aspect.AIR));
        assertEquals(1, BlockInfusedGrain.getNumberFromAspectForTexture(Aspect.FIRE));
        assertEquals(2, BlockInfusedGrain.getNumberFromAspectForTexture(Aspect.WATER));
        assertEquals(3, BlockInfusedGrain.getNumberFromAspectForTexture(Aspect.EARTH));
        assertEquals(4, BlockInfusedGrain.getNumberFromAspectForTexture(Aspect.ORDER));
        assertEquals(5, BlockInfusedGrain.getNumberFromAspectForTexture(Aspect.ENTROPY));
        assertEquals(6, BlockInfusedGrain.getNumberFromAspectForTexture(Aspect.MAGIC));
        assertEquals(6, BlockInfusedGrain.getNumberFromAspectForTexture(null));
        assertEquals(7, BlockInfusedGrain.ASPECT_TEXTURE.getPossibleValues().size(), "seven texture groups, 0 to 6");
    }

    @Test
    void theFourGrainVariantsFollowTheMetadataOfTheOriginal() {
        assertEquals(0, ItemInfusedGrain.getMetaForAspect(Aspect.AIR));
        assertEquals(1, ItemInfusedGrain.getMetaForAspect(Aspect.FIRE));
        assertEquals(2, ItemInfusedGrain.getMetaForAspect(Aspect.EARTH));
        assertEquals(3, ItemInfusedGrain.getMetaForAspect(Aspect.WATER));
        assertEquals(0, ItemInfusedGrain.getMetaForAspect(Aspect.ORDER), "an aspect without grain is variant 0");
        assertEquals("infused_grain_aer", ItemInfusedGrain.path(0));
        assertEquals("infused_grain_ignis", ItemInfusedGrain.path(1));
        assertEquals("infused_grain_terra", ItemInfusedGrain.path(2));
        assertEquals("infused_grain_aqua", ItemInfusedGrain.path(3));
        assertEquals("infused_potion_terra", ItemInfusedPotion.path(2));
    }

    @Test
    void thePotionVariantsAndTheirEffectsAreInTheSameOrder() {
        assertEquals(List.of("potion_air", "potion_fire", "potion_earth", "potion_water"), List.of(ModPotions.PATHS));
        assertEquals(List.of("Air Imbued", "Fire Imbued", "Earth Imbued", "Water Imbued"), List.of(ModPotions.ENGLISH_NAMES));
        assertSame(Aspect.AIR, ItemInfusedPotion.ASPECTS[0]);
        assertSame(Aspect.FIRE, ItemInfusedPotion.ASPECTS[1]);
        assertSame(Aspect.EARTH, ItemInfusedPotion.ASPECTS[2]);
        assertSame(Aspect.WATER, ItemInfusedPotion.ASPECTS[3]);
    }

    // ---- the data components of the seeds ----------------------------------------------------------------------

    @Test
    void theTendenciesSurviveTheTripThroughTheComponentMap() {
        AspectList list = new AspectList().add(Aspect.AIR, 3).add(Aspect.ORDER, 5).add(Aspect.WATER, 1);
        Map<String, Integer> map = InfusedSeedComponents.toMap(list);
        assertEquals(List.of("aer", "ordo", "aqua"), List.copyOf(map.keySet()), "the order is kept");
        assertEquals(3, (int) map.get("aer"));
        AspectList back = InfusedSeedComponents.toList(map);
        assertEquals(3, back.getAmount(Aspect.AIR));
        assertEquals(5, back.getAmount(Aspect.ORDER));
        assertEquals(1, back.getAmount(Aspect.WATER));
        assertEquals(0, back.getAmount(Aspect.FIRE));
    }

    @Test
    void anEmptyListAndAMissingMapGiveEmptyResults() {
        assertTrue(InfusedSeedComponents.toMap(new AspectList()).isEmpty(), "the [null] array of an empty list is skipped");
        AspectList none = InfusedSeedComponents.toList(null);
        assertNotNull(none);
        assertEquals(0, none.size());
    }

    @Test
    void theTagOfAnUnknownAspectIsDroppedAndAZeroAmountIsKept() {
        Map<String, Integer> map = new java.util.LinkedHashMap<>();
        map.put("aer", 0);
        map.put("not_an_aspect", 4);
        AspectList list = InfusedSeedComponents.toList(map);
        assertEquals(1, list.size());
        assertTrue(list.getAspects()[0] == Aspect.AIR && list.getAmount(Aspect.AIR) == 0);
    }

    // ---- the weighted loot of a ripe crop ----------------------------------------------------------------------

    @Test
    void anAspectWithoutLootYieldsNothing() {
        assertNull(AspectCropLootManager.getLootForAspect(null));
        assertNull(AspectCropLootManager.getLootForAspect(Aspect.DARKNESS));
    }

    @Test
    void theLootIsPickedByWeightAndHandedOutAsACopy() {
        // Aspect.COLD is not used by any other check of this class; the table is static for the JVM
        AspectCropLootManager.addAspectLoot(Aspect.COLD, new ItemStack(Items.DIAMOND), 3);
        AspectCropLootManager.addAspectLoot(Aspect.COLD, new ItemStack(Items.COAL, 5), 1);
        AspectCropLootManager.addAspectLoot(Aspect.COLD, ItemStack.EMPTY, 100);

        int diamonds = 0;
        int coal = 0;
        for (int i = 0; i < 4000; i++) {
            ItemStack drawn = AspectCropLootManager.getLootForAspect(Aspect.COLD);
            assertNotNull(drawn);
            if (drawn.is(Items.DIAMOND)) diamonds++;
            else if (drawn.is(Items.COAL)) {
                coal++;
                assertEquals(5, drawn.getCount());
                drawn.setCount(1); // mutating the copy must not touch the table
            } else throw new AssertionError("unexpected " + drawn);
        }
        assertEquals(4000, diamonds + coal);
        // 3:1, a sample of 4000 has a standard deviation of about 27 around 3000
        assertTrue(Math.abs(diamonds - 3000) < 300, "diamonds " + diamonds);
        List<AspectCropLootManager.LootEntry> stored = AspectCropLootManager.getAllLoot().get(Aspect.COLD);
        assertEquals(2, stored.size(), "the empty stack was refused");
        assertEquals(5, stored.stream().filter(e -> e.stack.is(Items.COAL)).findFirst().orElseThrow().stack.getCount(),
                "the stored stack still holds five");
    }

    @Test
    void theTableOfTheRecipeViewersListsEveryAspect() {
        Map<Aspect, List<AspectCropLootManager.LootEntry>> all = AspectCropLootManager.getAllLoot();
        assertEquals(Aspect.aspects.size(), all.size());
        for (Aspect aspect : Aspect.aspects.values()) {
            assertTrue(all.containsKey(aspect), aspect.getTag());
        }
    }

    // ---- the rules of the elemental fires ----------------------------------------------------------------------

    @Test
    void onlyAWaterSourceCountsAsStillWater() {
        assertTrue(FireRules.isWaterSource(Blocks.WATER.defaultBlockState()));
        assertFalse(FireRules.isWaterSource(Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1)));
        assertFalse(FireRules.isWaterSource(Blocks.LAVA.defaultBlockState()));
        assertFalse(FireRules.isWaterSource(Blocks.AIR.defaultBlockState()));
    }

    @Test
    void aRuleMatchesItsSourceAndProducesItsTarget() {
        FireRules.Rule single = FireRules.rule(Blocks.COBBLESTONE, Blocks.STONE);
        assertTrue(single.matches(Blocks.COBBLESTONE.defaultBlockState()));
        assertFalse(single.matches(Blocks.STONE.defaultBlockState()));
        assertTrue(single.producing(Blocks.STONE.defaultBlockState()));
        assertEquals(Blocks.STONE, single.result(Blocks.COBBLESTONE.defaultBlockState()).getBlock());

        FireRules.Rule several = FireRules.rule(new net.minecraft.world.level.block.Block[] { Blocks.SAND, Blocks.GRAVEL }, Blocks.GLASS);
        assertTrue(several.matches(Blocks.SAND.defaultBlockState()));
        assertTrue(several.matches(Blocks.GRAVEL.defaultBlockState()));
        assertFalse(several.matches(Blocks.DIRT.defaultBlockState()));

        FireRules.Rule predicate = FireRules.rule(FireRules::isWaterSource, Blocks.OBSIDIAN);
        assertTrue(predicate.matches(Blocks.WATER.defaultBlockState()));
        assertFalse(predicate.matches(Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 2)));
    }

    @Test
    void theStatePropertiesTheOldAndNewBlockShareAreCarriedOver() {
        BlockState oldStairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                net.minecraft.core.Direction.WEST).setValue(BlockStateProperties.HALF, net.minecraft.world.level.block.state.properties.Half.TOP);
        BlockState carried = FireRules.carryOver(oldStairs, Blocks.COBBLESTONE_STAIRS.defaultBlockState());
        assertEquals(Blocks.COBBLESTONE_STAIRS, carried.getBlock());
        assertEquals(net.minecraft.core.Direction.WEST, carried.getValue(BlockStateProperties.HORIZONTAL_FACING));
        assertEquals(net.minecraft.world.level.block.state.properties.Half.TOP, carried.getValue(BlockStateProperties.HALF));
    }

    @Test
    void theLevelAndTheAgeOfTheOldBlockAreNeverCarriedOver() {
        BlockState oldWater = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3);
        assertEquals(0, (int) FireRules.carryOver(oldWater, Blocks.LAVA.defaultBlockState()).getValue(LiquidBlock.LEVEL));
        BlockState oldWheat = Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 6);
        assertEquals(0, (int) FireRules.carryOver(oldWheat, Blocks.CARROTS.defaultBlockState()).getValue(net.minecraft.world.level.block.CropBlock.AGE));
    }

    @Test
    void thePlainBlocksOfTheVanillaFlammabilityTable() {
        // the entries that need no block tag (the tags are bound only in a running game)
        assertEquals(100, FireRules.vanillaFlammability(Blocks.TNT.defaultBlockState()));
        assertEquals(20, FireRules.vanillaFlammability(Blocks.BOOKSHELF.defaultBlockState()));
        assertEquals(5, FireRules.vanillaFlammability(Blocks.COAL_BLOCK.defaultBlockState()));
        assertEquals(20, FireRules.vanillaFlammability(Blocks.HAY_BLOCK.defaultBlockState()));
        assertEquals(100, FireRules.vanillaFlammability(Blocks.VINE.defaultBlockState()));
        assertEquals(100, FireRules.vanillaFlammability(Blocks.SHORT_GRASS.defaultBlockState()));
        assertEquals(100, FireRules.vanillaFlammability(Blocks.DEAD_BUSH.defaultBlockState()));
        assertEquals(0, FireRules.vanillaFlammability(Blocks.STONE.defaultBlockState()));
        assertEquals(0, FireRules.vanillaFlammability(Blocks.AIR.defaultBlockState()));
    }

    // ---- the gases ---------------------------------------------------------------------------------------------

    @Test
    void aGasHasSixteenLevelsAndAirCountsAsGasCountsAsAir() {
        assertEquals(0, (int) BlockGas.LEVEL.getPossibleValues().stream().min(Integer::compare).orElseThrow());
        assertEquals(15, (int) BlockGas.LEVEL.getPossibleValues().stream().max(Integer::compare).orElseThrow());
        assertEquals(4, BlockGas.RELEASE_LEVEL);
        assertTrue(BlockGas.isAirLike(Blocks.AIR.defaultBlockState()));
        assertTrue(BlockGas.isAirLike(Blocks.CAVE_AIR.defaultBlockState()));
        assertFalse(BlockGas.isAirLike(Blocks.STONE.defaultBlockState()));
        assertFalse(BlockGas.isAirLike(Blocks.WATER.defaultBlockState()));
    }
}
