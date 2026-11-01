package thaumcraft.api.internal;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.HoveredSlotAspectRow;
import thaumcraft.common.lib.research.ItemScanIdentity;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ScanManager;

class PotionScanIdentityTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void waterHealingAndStrongerHealingCanEachBeScannedOnceAndTheApiAgrees() {
        var actor = new PlayerKnowledge();
        // Declared prerequisites: this test exercises subtype identity, not the aspect-discovery gate.
        for (var aspect : Aspect.aspects.values()) actor.addDiscoveredAspect(aspect.getTag());
        var water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        var healing = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
        var stronger = PotionContents.createItemStack(Items.POTION, Potions.STRONG_HEALING);
        for (var stack : new ItemStack[]{water, healing, stronger}) {
            assertFalse(ThaumcraftInternalMethods.scannedItem(actor, stack));
            var result = ScanManager.awardScan(actor, ItemScanIdentity.signature(stack),
                    ObjectAspectResolver.potionTags(stack), ScanManager.ScanKind.ITEM, 100);
            assertTrue(result.success(), ItemScanIdentity.signature(stack));
            assertTrue(ThaumcraftInternalMethods.scannedItem(actor, stack));
            assertEquals(ScanManager.Failure.DUPLICATE, ScanManager.awardScan(actor, ItemScanIdentity.signature(stack),
                    ObjectAspectResolver.potionTags(stack), ScanManager.ScanKind.ITEM, 100).failure());
        }
        assertEquals(9, actor.getAspectPoolFor("sano"), "healing3 and strong healing6 are both earned");
        ItemStack named = healing.copyWithCount(5);
        named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("another label"));
        assertEquals(ItemScanIdentity.signature(healing), ItemScanIdentity.signature(named));
    }

    @Test void existingOrdinaryAndWaterKeysSurviveWithoutMaskingOtherPotionTypes() {
        var actor = new PlayerKnowledge();
        actor.scanItem("#minecraft:potion");
        actor.scanItem("@minecraft:diamond_sword");
        var water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        var healing = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
        assertEquals("@minecraft:potion", ItemScanIdentity.signature(water));
        assertEquals("@minecraft:potion", ItemScanIdentity.signature(new ItemStack(Items.POTION)));
        assertTrue(ThaumcraftInternalMethods.scannedItem(actor, water));
        assertFalse(ThaumcraftInternalMethods.scannedItem(actor, healing));
        ItemStack damaged = new ItemStack(Items.DIAMOND_SWORD);
        damaged.setDamageValue(37);
        assertEquals("@minecraft:diamond_sword", ItemScanIdentity.signature(damaged));
        assertTrue(ThaumcraftInternalMethods.scannedItem(actor, damaged));
        var splash = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING);
        assertNotEquals(ItemScanIdentity.signature(healing), ItemScanIdentity.signature(splash));
        actor.scanItem("#" + ItemScanIdentity.signature(healing).substring(1));
        var saved = actor.save();
        var reloaded = new PlayerKnowledge(); reloaded.load(saved);
        assertTrue(ThaumcraftInternalMethods.scannedItem(reloaded, healing));
        assertFalse(ThaumcraftInternalMethods.scannedItem(reloaded, splash));
        assertTrue(reloaded.getScannedItems().contains("#minecraft:potion"), "old marker is retained unchanged");
    }

    @Test void hoveredAspectRowRecognizesOnlyTheScannedPotionSubtypeForBothPrefixes() {
        var water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        var healing = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
        var strong = PotionContents.createItemStack(Items.POTION, Potions.STRONG_HEALING);
        for (String prefix : new String[]{"@", "#"}) {
            var waterScans = java.util.Set.of(prefix + ItemScanIdentity.signature(water).substring(1));
            assertTrue(HoveredSlotAspectRow.isScanned(waterScans, water));
            assertFalse(HoveredSlotAspectRow.isScanned(waterScans, healing));
            var healingScans = java.util.Set.of(prefix + ItemScanIdentity.signature(healing).substring(1));
            assertTrue(HoveredSlotAspectRow.isScanned(healingScans, healing));
            assertFalse(HoveredSlotAspectRow.isScanned(healingScans, water));
            assertFalse(HoveredSlotAspectRow.isScanned(healingScans, strong));
        }
    }

    @Test void subtypeSessionsPreserveOrdinaryShapesAndCancelChangedPotionContents() {
        var redstone = new ItemStack(Items.REDSTONE);
        var bow = new ItemStack(Items.BOW); bow.setDamageValue(40);
        var water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        var healing = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
        var strong = PotionContents.createItemStack(Items.POTION, Potions.STRONG_HEALING);
        assertEquals("item:minecraft:redstone:0", ItemScanIdentity.itemSessionKey(redstone, 0));
        assertEquals("item:minecraft:bow:40", ItemScanIdentity.itemSessionKey(bow, 40));
        assertEquals("entity:37", ItemScanIdentity.entitySessionKey(37, redstone));
        assertEquals("entity:37", ItemScanIdentity.entitySessionKey(37, water));
        assertNotEquals(ItemScanIdentity.itemSessionKey(water, 0), ItemScanIdentity.itemSessionKey(healing, 0));
        assertNotEquals(ItemScanIdentity.entitySessionKey(37, water), ItemScanIdentity.entitySessionKey(37, healing));
        assertNotEquals(ItemScanIdentity.entitySessionKey(37, healing), ItemScanIdentity.entitySessionKey(37, strong));
        assertNotEquals(ItemScanIdentity.entitySessionKey(37, healing), ItemScanIdentity.entitySessionKey(38, healing));
    }

    @Test void sharingAndMergingSubtypeMarkersPreservesEachVariantsAdvancedMarker() {
        var water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        var healing = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
        var strong = PotionContents.createItemStack(Items.POTION, Potions.STRONG_HEALING);
        String waterKey = ItemScanIdentity.signature(water), healingKey = ItemScanIdentity.signature(healing), strongKey = ItemScanIdentity.signature(strong);
        var recipient = new PlayerKnowledge();
        recipient.scanItem("#" + waterKey.substring(1)); recipient.scanItem("#" + healingKey.substring(1));
        var incoming = new thaumcraft.api.ThaumcraftApiHelper.Scans(java.util.Set.of(waterKey, healingKey, strongKey), java.util.Set.of(), java.util.Set.of());
        assertEquals(1, ThaumcraftInternalMethods.mergeScans(recipient, incoming), "only the unseen strong subtype is new");
        assertTrue(ThaumcraftInternalMethods.scannedItem(recipient, water));
        assertTrue(ThaumcraftInternalMethods.scannedItem(recipient, healing));
        assertTrue(ThaumcraftInternalMethods.scannedItem(recipient, strong));
        assertEquals(java.util.Set.of("#" + waterKey.substring(1), "#" + healingKey.substring(1), strongKey), recipient.getScannedItems());
        assertEquals(0, ThaumcraftInternalMethods.mergeScans(recipient, incoming), "repeated import is idempotent");
    }
}
