package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.common.blocks.BannerData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.lib.baubles.RunicBaubleRules;
import thaumcraft.common.lib.items.EquipmentRules;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * The pure rules of the catalogue entries the port audit found missing.
 *
 * <p>Every number here comes from the decompiled TC4 4.2.3.5 oracle:
 * {@code ConfigItems}:302-334 (the wand caps), {@code ConfigItems}:46-53 and :276-293 (the nuggets and the
 * native clusters), {@code ConfigBlocks}:300-301 (the double slabs) and
 * {@code BlockCosmeticWoodSlab}:56-58 / {@code BlockCosmeticStoneSlab}:56-58 (their drops).
 */
class CatalogueGapRulesTest {
    /**
     * ConfigItems:331 - the copper cap: the base cost modifier of the iron cap, the special aspects order and
     * entropy with the special modifier one, and the craft cost two. The original registered it only when
     * another mod supplied a copper ingot; copper is vanilla in 1.21.1.
     */
    @Test void theCopperCapKeepsTheOriginalCostModifierAndCraftCost() {
        WandMaterials.Cap copper = WandMaterials.COPPER;
        assertEquals("copper", copper.tag());
        assertEquals(1.1F, copper.modifier(), 0F);
        assertEquals(2, copper.craftCost());
        assertEquals(List.of("ordo", "perditio"), copper.specialAspects());
        assertEquals(1.0F, copper.specialModifier(), 0F);
        assertEquals("CAP_copper", copper.research());
    }

    /**
     * The original ItemWandCasting:249 - the cap pays the special modifier for its special aspects and the
     * base modifier for every other one.
     */
    @Test void theCopperCapPaysItsSpecialModifierForOrderAndEntropyOnly() {
        WandMaterials.Cap copper = WandMaterials.COPPER;
        assertEquals(1.0F, copper.modifierFor("ordo"), 0F);
        assertEquals(1.0F, copper.modifierFor("perditio"), 0F);
        assertEquals(1.1F, copper.modifierFor("aer"), 0F);
        assertEquals(1.1F, copper.modifierFor("terra"), 0F);
        assertEquals(1.1F, copper.modifierFor("ignis"), 0F);
        assertEquals(1.1F, copper.modifierFor("aqua"), 0F);
    }

    /** The cap catalogue of the wand materials and the original metadata table must agree. */
    @Test void theCopperCapIsTheSameInBothOriginalTables() {
        RunicBaubleRules.Cap original = RunicBaubleRules.cap(3);
        WandMaterials.Cap port = WandMaterials.COPPER;
        assertEquals(original.tag(), port.tag());
        assertEquals(original.modifier(), port.modifier(), 0F);
        assertEquals(original.craftCost(), port.craftCost());
        assertEquals(original.specialAspects(), port.specialAspects());
        assertEquals(original.specialModifier(), port.specialModifier(), 0F);
    }

    /** The five caps of the wand materials: iron, gold, thaumium, void and copper. */
    @Test void theWandCapCatalogueHasTheFiveOriginalCaps() {
        assertEquals(List.of("iron", "gold", "thaumium", "void", "copper"),
                WandMaterials.CAPS.stream().map(WandMaterials.Cap::tag).toList());
        for (WandMaterials.Cap cap : WandMaterials.CAPS) assertEquals(cap, WandMaterials.cap(cap.tag()));
        assertNotEquals(WandMaterials.COPPER, WandMaterials.IRON);
    }

    /**
     * ConfigItems:46-53 - the nuggets and the native clusters of the original. The copper nugget and the
     * native copper cluster are carried because copper is vanilla in 1.21.1; the iron nugget is carried by the
     * vanilla iron nugget and the tin, silver and lead ones stay with their metal.
     */
    @Test void theNuggetAndClusterMetadataOfTheOriginalAreTheOnesThePortCarries() {
        assertEquals(1, EquipmentRules.NUGGET_COPPER);
        assertEquals(17, EquipmentRules.NUGGET_CLUSTER_COPPER);
        assertEquals(16, EquipmentRules.NUGGET_CLUSTER_IRON);
        assertEquals(31, EquipmentRules.NUGGET_CLUSTER_GOLD);
        assertEquals(21, EquipmentRules.NUGGET_CLUSTER_CINNABAR);
        // The original clusters of tin, silver and lead need a metal that 1.21.1 does not have.
        assertEquals(18, EquipmentRules.NUGGET_CLUSTER_TIN);
        assertEquals(19, EquipmentRules.NUGGET_CLUSTER_SILVER);
        assertEquals(20, EquipmentRules.NUGGET_CLUSTER_LEAD);
    }

    /**
     * ConfigBlocks:300-301 - the double slabs. The wooden family drops two single slabs, the stone family one
     * (BlockCosmeticWoodSlab:56-58 and BlockCosmeticStoneSlab:56-58).
     */
    @Test void theDoubleSlabsKeepTheOriginalDrops() {
        assertTrue(EldritchBlockRules.doubleSlabIsWooden(EldritchBlockRules.SLAB_DOUBLE_WOOD_GREATWOOD));
        assertTrue(EldritchBlockRules.doubleSlabIsWooden(EldritchBlockRules.SLAB_DOUBLE_WOOD_SILVERWOOD));
        assertFalse(EldritchBlockRules.doubleSlabIsWooden(EldritchBlockRules.SLAB_DOUBLE_STONE_ARCANE));
        assertFalse(EldritchBlockRules.doubleSlabIsWooden(EldritchBlockRules.SLAB_DOUBLE_STONE_ELDRITCH));
        assertEquals(2, EldritchBlockRules.doubleSlabDropCount(EldritchBlockRules.SLAB_DOUBLE_WOOD_GREATWOOD));
        assertEquals(2, EldritchBlockRules.doubleSlabDropCount(EldritchBlockRules.SLAB_DOUBLE_WOOD_SILVERWOOD));
        assertEquals(1, EldritchBlockRules.doubleSlabDropCount(EldritchBlockRules.SLAB_DOUBLE_STONE_ARCANE));
        assertEquals(1, EldritchBlockRules.doubleSlabDropCount(EldritchBlockRules.SLAB_DOUBLE_STONE_ELDRITCH));
    }

    /**
     * ConfigRecipes:184-189 - the sixteen crimson cult banners of the original. The original kept the
     * colour in the item NBT, so the native port keeps ONE banner item that carries its colour and its
     * aspect in a data component; the sixteen colours are the dye order of the original.
     */
    @Test void theBannerItemCarriesTheOriginalColourAndAspect() {
        assertEquals(EldritchBlockRules.DEVICE_WOOD_BANNER, EldritchBlockRules.variantOf("banner"));
        assertEquals(-1, EldritchBlockRules.BANNER_COLOUR_NONE);
        // The colour byte of the original runs from minus one to fifteen.
        for (int colour = 0; colour < 16; colour++) {
            BannerData data = new BannerData(colour, "");
            assertTrue(data.hasColor());
            assertEquals(colour, data.color());
        }
        assertFalse(new BannerData(EldritchBlockRules.BANNER_COLOUR_NONE, "").hasColor());
        // The aspect of the original BlockWoodenDevice:425-428 rides along in the same component.
        assertEquals("aer", new BannerData(0, "aer").aspectTag());
        assertTrue(new BannerData(0, "aer").aspect() != null);
        assertTrue(new BannerData(0, "").aspect() == null);
    }
}
