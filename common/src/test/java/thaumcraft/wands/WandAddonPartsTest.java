package thaumcraft.wands;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.IWandRodOnUpdate;
import thaumcraft.api.wands.StaffRod;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandHelper;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.WandModels;
import thaumcraft.common.items.wands.WandRodPrimalOnUpdate;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.lib.enchantment.WandEnchantmentRules;

/**
 * The bridge between the public part registries ({@code WandRod.rods}, {@code WandCap.caps}) and the native catalogue: the
 * native parts as API views with the original values, add-on parts resolved lazily and live, and the native values unchanged.
 */
class WandAddonPartsTest {
    private LinkedHashMap<String, WandRod> rods;
    private LinkedHashMap<String, WandCap> caps;

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach void snapshot() { rods = new LinkedHashMap<>(WandRod.rods); caps = new LinkedHashMap<>(WandCap.caps); }
    @AfterEach void restore() {
        WandRod.rods.clear(); WandRod.rods.putAll(rods);
        WandCap.caps.clear(); WandCap.caps.putAll(caps);
    }

    @Test void theNativePartsAreRegisteredAsViewsWithTheOriginalValues() {
        for (var rod : WandMaterials.RODS) {
            WandRod view = WandRod.rods.get(rod.tag());
            assertNotNull(view, rod.tag());
            assertSame(rod, view.nativeRod(), rod.tag());
            assertEquals(rod.capacity() / 100, view.getCapacity(), rod.tag() + ": the original constructors take whole vis");
            assertEquals(rod.craftCost(), view.getCraftCost(), rod.tag());
            assertEquals(rod.staff(), view instanceof StaffRod, rod.tag() + ": original isStaff is instanceof StaffRod");
            if (view instanceof StaffRod staff) assertEquals(rod.hasRunes(), staff.hasRunes(), rod.tag());
            assertEquals("ROD_" + rod.tag(), view.getResearch());
            assertEquals(rod.tag().equals("blaze"), view.isGlowing(), "original ConfigItems:305 lights the blaze rod only");
            if (rod.passive() == null) assertNull(view.getOnUpdate(), rod.tag());
            else {
                var hook = assertInstanceOf(WandRodPrimalOnUpdate.class, view.getOnUpdate(), rod.tag());
                if (WandMaterials.ANY_PRIMAL.equals(rod.passive())) assertTrue(hook.anyPrimal());
                else assertEquals(rod.passive(), hook.aspect());
            }
            assertNotNull(view.getItem(), "a native view answers a stack, empty until the items are registered");
        }
        // Original capacities, in the original unit (ConfigItems:292-313).
        assertEquals(25, WandRod.rods.get("wood").getCapacity());
        assertEquals(100, WandRod.rods.get("silverwood").getCapacity());
        assertEquals(125, WandRod.rods.get("greatwood_staff").getCapacity());
        assertEquals(250, WandRod.rods.get("primal_staff").getCapacity());
        for (var cap : WandMaterials.CAPS) {
            WandCap view = WandCap.caps.get(cap.tag());
            assertSame(cap, view.nativeCap(), cap.tag());
            assertEquals(cap.modifier(), view.getBaseCostModifier(), cap.tag());
            assertEquals(cap.craftCost(), view.getCraftCost(), cap.tag());
            assertEquals("CAP_" + cap.tag(), view.getResearch());
        }
        assertEquals(List.of(Aspect.ORDER, Aspect.ENTROPY), WandCap.caps.get("copper").getSpecialCostModifierAspects());
        assertEquals(1.0F, WandCap.caps.get("copper").getSpecialCostModifier());
        assertNull(WandCap.caps.get("iron").getSpecialCostModifierAspects());
    }

    @Test void nativeLookupsStillAnswerTheNativeConstants() {
        for (var rod : WandMaterials.RODS) {
            assertSame(rod, WandMaterials.rod(rod.tag()));
            assertFalse(rod.isAddon());
            assertNull(rod.onUpdate());
            assertNull(rod.texture());
        }
        for (var cap : WandMaterials.CAPS) assertSame(cap, WandMaterials.cap(cap.tag()));
        assertEquals(288, WandMaterials.assemblyCost("primal_staff", "void"));
        assertEquals(37500, WandMaterials.capacity("primal_staff", true));
        assertEquals(WandMaterials.RODS, WandMaterials.allRods().subList(0, WandMaterials.RODS.size()), "native rods first, in their order");
    }

    @Test void anAddonRodRegisteredLaterIsResolvedLiveFromTheRegistry() {
        assertFalse(WandMaterials.supportsRod("junit_ember"));
        IWandRodOnUpdate hook = (stack, player) -> {};
        var texture = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("myaddon", "textures/models/wand_rod_ember.png");
        WandRod rod = new WandRod("junit_ember", 150, new ItemStack(Items.BLAZE_ROD), 12, hook, texture);
        assertTrue(WandMaterials.supportsRod("junit_ember"));
        var resolved = WandMaterials.rod("junit_ember");
        assertTrue(resolved.isAddon());
        assertSame(rod, resolved.addon());
        assertEquals(15000, resolved.capacity(), "150 whole vis are 15000 centivis");
        assertEquals(15000, WandMaterials.capacity("junit_ember", false));
        assertEquals(22500, WandMaterials.capacity("junit_ember", true), "a sceptre holds one and a half times");
        assertEquals(12, resolved.craftCost());
        assertSame(hook, resolved.onUpdate());
        assertEquals(texture, resolved.texture());
        assertEquals("ROD_junit_ember", resolved.research());
        assertFalse(resolved.staff());
        assertNull(resolved.passive(), "an add-on rod never runs the native passive");
        assertTrue(WandMaterials.allRods().contains(resolved));
        // Setter calls after the registration are seen.
        rod.setCapacity(200);
        rod.setCraftCost(5);
        assertEquals(20000, WandMaterials.capacity("junit_ember", false));
        assertEquals(5, WandMaterials.rodCraftCost("junit_ember"));
        // An overridden research key is honoured.
        new WandRod("junit_secret", 10, ItemStack.EMPTY, 1) {
            @Override public String getResearch() { return "MYADDON_SECRET_ROD"; }
        };
        assertEquals("MYADDON_SECRET_ROD", WandMaterials.rod("junit_secret").research());
        // A removed registration is no longer a part.
        WandRod.rods.remove("junit_ember");
        assertFalse(WandMaterials.supportsRod("junit_ember"));
        assertThrows(IllegalArgumentException.class, () -> WandMaterials.rod("junit_ember"));
    }

    /**
     * A tag is also the suffix of a research key (ROD_ICHORCLOTH, CAP_ICHOR), so an add-on may tag a part in upper case; the
     * texture path derived from it must still be a legal lower-case resource path (found by the Thaumic Tinkerer add-on, whose
     * Ichor parts are tagged like the research keys of 1.7.10).
     */
    @Test void theDerivedTextureOfAnUpperCaseTagIsALegalResourcePath() {
        WandRod rod = new WandRod("JUNIT_ICHORCLOTH", 100, new ItemStack(Items.BLAZE_ROD), 10);
        WandCap cap = new WandCap("JUNIT_ICHOR", 0.7F, new ItemStack(Items.GOLD_INGOT), 10);
        StaffRod staff = new StaffRod("JUNIT_STAFF", 100, new ItemStack(Items.STICK), 10);
        assertEquals("thaumcraft:textures/models/wand_rod_junit_ichorcloth.png", rod.getTexture().toString());
        assertEquals("thaumcraft:textures/models/wand_cap_junit_ichor.png", cap.getTexture().toString());
        assertEquals("thaumcraft:textures/models/wand_rod_junit_staff.png", staff.getTexture().toString());
        assertEquals("ROD_JUNIT_ICHORCLOTH", rod.getResearch(), "the research key keeps the tag as it was written");
        assertEquals("CAP_JUNIT_ICHOR", cap.getResearch());
        assertSame(rod, WandRod.rods.get("JUNIT_ICHORCLOTH"));
    }

    @Test void anAddonStaffRodIsAStaffNamedAfterItsPlainRod() {
        StaffRod staff = new StaffRod("junit_ember", 200, ItemStack.EMPTY, 15);
        var resolved = WandMaterials.rod("junit_ember_staff");
        assertTrue(resolved.staff());
        assertEquals("junit_ember", resolved.baseTag());
        assertEquals("thaumcraft:textures/models/wand_rod_junit_ember.png", staff.getTexture().toString());
        assertFalse(resolved.hasRunes());
        staff.setRunes(true);
        assertTrue(WandMaterials.rod("junit_ember_staff").hasRunes());
        assertEquals(1, WandMaterials.focusPotency(0, WandMaterials.rod("junit_ember_staff")));
        // Original isStaff is instanceof StaffRod; the name rule strips "_staff" from any rod tag.
        new WandRod("junit_odd_staff_x", 10, ItemStack.EMPTY, 1);
        assertFalse(WandMaterials.rod("junit_odd_staff_x").staff());
        assertEquals("junit_odd", WandMaterials.rod("junit_odd_staff_x").baseTag());
    }

    @Test void anAddonCapDiscountsItsSpecialAspectsWithTheirOwnModifier() {
        WandCap cap = new WandCap("junit_tidal", 0.9F, List.of(Aspect.WATER), 0.8F, ItemStack.EMPTY, 7);
        var resolved = WandMaterials.cap("junit_tidal");
        assertTrue(resolved.isAddon());
        assertEquals(0.8F, resolved.modifierFor("aqua"));
        assertEquals(0.9F, resolved.modifierFor("ignis"));
        assertEquals(0.9F, resolved.modifierFor(null));
        assertEquals("CAP_junit_tidal", resolved.research());
        // Original ItemWandCasting.getConsumptionModifier: cap, minus gear, frugal and the sceptre, never below a tenth.
        assertEquals(Math.max(0.8F - 10 / 100F - 2 / 10F - .1F, .1F), WandVisDiscount.focusModifier(resolved.modifierFor("aqua"), 10, 2, true));
        assertEquals(Math.max(0.9F - .1F, .1F), WandVisDiscount.modifier(resolved.modifierFor("ignis"), 0, true));
        new WandRod("junit_ember", 150, ItemStack.EMPTY, 12);
        assertEquals(84, WandMaterials.assemblyCost("junit_ember", "junit_tidal"));
        assertEquals(126, WandEnchantmentRules.sceptreCost(7, 12));
        // A null special list behaves like the plain constructor.
        new WandCap("junit_plain", 1.2F, null, 0F, ItemStack.EMPTY, 2);
        assertEquals(1.2F, WandMaterials.cap("junit_plain").modifierFor("aqua"));
        cap.setCraftCost(9);
        assertEquals(108, WandMaterials.assemblyCost("junit_ember", "junit_tidal"), "the cap is read live");
    }

    @Test void aRegistrationUnderANativeTagNeverReplacesTheNativePart() {
        new WandRod("greatwood", 999, new ItemStack(Items.STICK), 99);
        new WandCap("gold", 0.1F, new ItemStack(Items.GOLD_INGOT), 99);
        assertSame(WandMaterials.GREATWOOD, WandMaterials.rod("greatwood"));
        assertSame(WandMaterials.GOLD, WandMaterials.cap("gold"));
        assertNull(WandMaterials.addonRod("greatwood"));
        assertEquals(WandMaterials.RODS.size(), WandMaterials.allRods().stream().filter(rod -> !rod.isAddon()).count());
        assertTrue(WandMaterials.allRods().stream().noneMatch(rod -> rod.isAddon() && rod.tag().equals("greatwood")),
                "the replacement is not listed as an add-on part either");
        assertTrue(WandMaterials.allCaps().stream().noneMatch(cap -> cap.isAddon() && cap.tag().equals("gold")));
    }

    @Test void addonValuesAreClampedToWhatAWandCanStore() {
        new WandRod("junit_negative", -5, ItemStack.EMPTY, -3);
        assertEquals(0, WandMaterials.rod("junit_negative").capacity());
        assertEquals(0, WandMaterials.rod("junit_negative").craftCost());
        new WandRod("junit_huge", Integer.MAX_VALUE, ItemStack.EMPTY, 1000);
        assertEquals(WandMaterials.MAX_ADDON_CAPACITY * 100, WandMaterials.capacity("junit_huge", false));
        assertTrue(WandMaterials.capacity("junit_huge", true) > 0, "the sceptre capacity does not overflow");
        new WandCap("junit_dear", 1F, ItemStack.EMPTY, 1000);
        assertEquals((int) Short.MAX_VALUE, WandMaterials.assemblyCost("junit_huge", "junit_dear"), "the original short metadata");
    }

    @Test void theWandComponentAcceptsAddonPartsAndTheirOwnCapacity() {
        new WandRod("junit_big", 400, ItemStack.EMPTY, 20);
        new WandCap("junit_tidal", 0.9F, ItemStack.EMPTY, 7);
        var data = new WandData("junit_big", "junit_tidal", Map.of("aer", 50000));
        assertEquals(60000, data.maxCapacity(), "400 whole vis as a sceptre");
        var json = WandData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        assertEquals(data, WandData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), "add-on parts round-trip, above the native 37500");
        assertThrows(IllegalArgumentException.class, () -> new WandData("junit_big", "junit_tidal", Map.of("aer", 60001)));
        assertTrue(WandData.CODEC.parse(JsonOps.INSTANCE, WandData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow()
                .getAsJsonObject().deepCopy()).result().isPresent());
        var tooMuch = json.getAsJsonObject().deepCopy();
        tooMuch.getAsJsonObject("vis").addProperty("aer", 60001);
        assertTrue(WandData.CODEC.parse(JsonOps.INSTANCE, tooMuch).error().isPresent(), "the rod's own sceptre capacity bounds the decoder");
        var nativeTooMuch = WandData.CODEC.encodeStart(JsonOps.INSTANCE, new WandData("primal_staff", "iron", Map.of())).getOrThrow().getAsJsonObject();
        var vis = new com.google.gson.JsonObject();
        vis.addProperty("aer", 37501);
        nativeTooMuch.add("vis", vis);
        assertTrue(WandData.CODEC.parse(JsonOps.INSTANCE, nativeTooMuch).error().isPresent(), "a native wand above 375 vis is still refused");
        var unknown = json.getAsJsonObject().deepCopy();
        unknown.addProperty("rod", "junit_never_registered");
        assertTrue(WandData.CODEC.parse(JsonOps.INSTANCE, unknown).error().isPresent(), "an unregistered rod is refused");
        WandRod.rods.remove("junit_big");
        assertTrue(WandData.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent(), "a rod that is gone can no longer be decoded");
    }

    @Test void theModelSelectorFlagsEveryWandWithAnAddonPart() {
        for (var rod : WandMaterials.RODS) for (var cap : WandMaterials.CAPS) assertEquals(0F, WandModels.addonValue(rod.tag(), cap.tag()));
        new WandRod("junit_ember", 150, ItemStack.EMPTY, 12);
        new WandCap("junit_tidal", 0.9F, ItemStack.EMPTY, 7);
        assertEquals(1F, WandModels.addonValue("junit_ember", "iron"));
        assertEquals(1F, WandModels.addonValue("greatwood", "junit_tidal"));
        assertEquals(1F, WandModels.addonValue("junit_ember", "junit_tidal"));
    }

    @Test void theHelperAnswersNothingForAStackThatIsNotAWand() {
        ItemStack stick = new ItemStack(Items.STICK);
        assertFalse(WandHelper.isWand(stick));
        assertFalse(WandHelper.isWand(ItemStack.EMPTY));
        assertNull(WandHelper.getRod(stick));
        assertNull(WandHelper.getCap(stick));
        assertNull(WandHelper.getFocus(stick));
        assertTrue(WandHelper.getFocusItem(stick).isEmpty());
        assertEquals(0, WandHelper.getVis(stick, Aspect.FIRE));
        assertEquals(0, WandHelper.getMaxVis(stick));
        assertEquals(0, WandHelper.getAllVis(stick).size());
        assertEquals(0, WandHelper.addVis(stick, Aspect.FIRE, 5, true));
        assertEquals(0, WandHelper.addRealVis(stick, Aspect.FIRE, 5, true));
        assertFalse(WandHelper.consumeVis(stick, null, Aspect.FIRE, 5, false));
        assertFalse(WandHelper.consumeAllVis(stick, null, new AspectList().add(Aspect.FIRE, 5), true, false));
        assertFalse(WandHelper.consumeAllVisCrafting(stick, null, new AspectList().add(Aspect.FIRE, 5), true));
        assertEquals(1F, WandHelper.getConsumptionModifier(stick, null, Aspect.FIRE, false));
        assertFalse(WandHelper.isStaff(stick) || WandHelper.isSceptre(stick) || WandHelper.hasRunes(stick));
        assertEquals(0, WandHelper.getFocusPotency(stick) + WandHelper.getFocusFrugal(stick) + WandHelper.getFocusTreasure(stick)
                + WandHelper.getFocusEnlarge(stick) + WandHelper.getFocusExtend(stick));
        assertFalse(WandHelper.setFocus(stick, ItemStack.EMPTY));
        assertTrue(stick.getComponentsPatch().isEmpty(), "the stack is never changed");
    }
}
