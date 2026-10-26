package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.JarLabelData;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.WispEssenceData;
import thaumcraft.common.lib.research.EntityAspectBindings;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchAspectCombination;
import thaumcraft.common.lib.research.ResearchNotePuzzle;
import thaumcraft.common.lib.research.ScanManager;
import thaumcraft.common.tiles.TileResearchTable;

/**
 * An aspect registered by an add-on works like an original one in every pure consumer.
 *
 * <p>The registry is global: every test registers aspects under a unique random tag and removes them
 * again, so the 48-aspect assertions of the other test classes keep holding.</p>
 */
class AddonAspectTest {
    private final List<Aspect> registered = new ArrayList<>();

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach void unregister() {
        for (Aspect aspect : registered) Aspect.aspects.values().removeIf(value -> value == aspect);
        registered.clear();
        assertEquals(48, Aspect.aspects.size(), "test aspects must not leak into other tests");
    }

    private static String uniqueTag() { return "addon_test_" + UUID.randomUUID().toString().replace("-", ""); }

    private Aspect compound(Aspect first, Aspect second) {
        String tag = uniqueTag();
        Aspect aspect = new Aspect(tag, 0xB68CFF, new Aspect[]{first, second},
                ResourceLocation.fromNamespaceAndPath("myaddon", "textures/aspects/" + tag + ".png"), 1);
        registered.add(aspect);
        return aspect;
    }

    private Aspect primal() {
        Aspect aspect = new Aspect(uniqueTag(), 0x123456, "5", 771);
        registered.add(aspect);
        return aspect;
    }

    @Test void theConstructorRegistersTheAddonAfterTheOriginalAspects() {
        Aspect tempus = compound(Aspect.VOID, Aspect.ORDER);
        List<Aspect> order = new ArrayList<>(Aspect.aspects.values());
        assertEquals(49, order.size());
        assertSame(Aspect.AIR, order.get(0));
        assertSame(Aspect.TRAP, order.get(47));
        assertSame(tempus, order.get(48), "add-on aspects follow the 48 original aspects");
        assertSame(tempus, Aspect.getAspect(tempus.getTag()));
        assertFalse(tempus.isPrimal());
        assertTrue(Aspect.getCompoundAspects().contains(tempus));
        assertFalse(Aspect.getPrimalAspects().contains(tempus));
        assertEquals(ResourceLocation.fromNamespaceAndPath("myaddon", "textures/aspects/" + tempus.getTag() + ".png"), tempus.getImage());
        assertEquals(0xB68CFF, tempus.getColor());
        assertEquals(1, tempus.getBlend());
        // Oracle Aspect:74-84: a duplicate tag is refused before replacement.
        assertThrows(IllegalArgumentException.class, () -> new Aspect(tempus.getTag(), 0, new Aspect[]{Aspect.AIR, Aspect.FIRE}));
        assertSame(tempus, Aspect.getAspect(tempus.getTag()));
    }

    @Test void theShorterConstructorsDeriveTheOriginalTexturePathAndPrimalShape() {
        Aspect derived = new Aspect(uniqueTag(), 0x654321, new Aspect[]{Aspect.FIRE, Aspect.EARTH});
        registered.add(derived);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/aspects/" + derived.getTag() + ".png"), derived.getImage());
        Aspect primal = primal();
        assertTrue(primal.isPrimal());
        assertNull(primal.getComponents());
        assertEquals("5", primal.getChatcolor());
        List<Aspect> primals = Aspect.getPrimalAspects();
        assertEquals(List.of(Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY, primal), primals,
                "the six original primals keep their positions, the add-on primal follows");
    }

    @Test void theNameIsTheOriginalCapitalizeFullyOfTheTag() {
        // Original Aspect.getName: WordUtils.capitalizeFully(tag).
        assertEquals("Aer", Aspect.AIR.getName());
        assertEquals("Praecantatio", Aspect.MAGIC.getName());
        Aspect aspect = compound(Aspect.AIR, Aspect.WATER);
        String tag = aspect.getTag();
        try {
            aspect.setTag("TEMPUS");
            assertEquals("Tempus", aspect.getName());
            aspect.setTag("two words");
            assertEquals("Two Words", aspect.getName());
            aspect.setTag("");
            assertEquals("", aspect.getName());
            aspect.setTag(null);
            assertNull(aspect.getName());
        } finally {
            aspect.setTag(tag);
        }
        // Oracle Aspect:108 - the description is the translation of tc.aspect.<tag>; the unit test has
        // no language loaded, so the key itself comes back.
        assertEquals("tc.aspect." + tag, aspect.getLocalizedDescription());
    }

    @Test void aCombinationOfTheTwoParentsAnswersTheAddonUnlessAnOriginalAspectOwnsThePair() {
        Aspect tempus = compound(Aspect.VOID, Aspect.ORDER);
        assertSame(tempus, Aspect.getCombinationResult(Aspect.VOID, Aspect.ORDER));
        assertSame(tempus, Aspect.getCombinationResult(Aspect.ORDER, Aspect.VOID));
        // A pair an original aspect already uses keeps answering the original (registry order).
        compound(Aspect.AIR, Aspect.FIRE);
        assertSame(Aspect.LIGHT, Aspect.getCombinationResult(Aspect.AIR, Aspect.FIRE));
        // A compound of an add-on compound is a compound of the registry too.
        Aspect nested = compound(tempus, Aspect.MIND);
        assertSame(nested, Aspect.getCombinationResult(Aspect.MIND, tempus));
        // nested = tempus(vacuos(aer, perditio), ordo) + cognitio(ignis, spiritus(victus(aqua, terra), mortuus(victus, perditio))).
        assertEquals(Map.of(Aspect.AIR, 1, Aspect.ENTROPY, 2, Aspect.ORDER, 1, Aspect.FIRE, 1, Aspect.WATER, 2, Aspect.EARTH, 2),
                new AspectList().add(nested, 1).reduceToPrimals(false).aspects, "reduction walks add-on components down to primals");
    }

    @Test void theHelperCachesFollowTheRegistry() {
        AspectList before = ThaumcraftApiHelper.getAllAspects(733);
        AspectList compoundsBefore = ThaumcraftApiHelper.getAllCompoundAspects(733);
        assertEquals(48, before.size());
        Aspect tempus = compound(Aspect.VOID, Aspect.ORDER);
        Aspect primal = primal();
        AspectList after = ThaumcraftApiHelper.getAllAspects(733);
        assertEquals(50, after.size(), "an aspect registered after the first call is part of later answers");
        assertEquals(733, after.getAmount(tempus));
        assertEquals(733, after.getAmount(primal));
        AspectList compounds = ThaumcraftApiHelper.getAllCompoundAspects(733);
        assertEquals(compoundsBefore.size() + 1, compounds.size());
        assertEquals(733, compounds.getAmount(tempus));
        assertEquals(0, compounds.getAmount(primal));
        tempus.setComponents(null);
        try {
            assertEquals(0, ThaumcraftApiHelper.getAllCompoundAspects(733).getAmount(tempus), "retuned components refresh the compound cache");
        } finally {
            tempus.setComponents(new Aspect[]{Aspect.VOID, Aspect.ORDER});
        }
        unregister();
        assertEquals(48, ThaumcraftApiHelper.getAllAspects(733).size());
        assertEquals(compoundsBefore.size(), ThaumcraftApiHelper.getAllCompoundAspects(733).size());
    }

    @Test void theListAndTheItemComponentsPersistTheAddonByTag() {
        Aspect tempus = compound(Aspect.VOID, Aspect.ORDER);
        AspectList list = new AspectList().add(Aspect.AIR, 3).add(tempus, 7);
        CompoundTag saved = new CompoundTag();
        list.writeToNBT(saved);
        AspectList read = new AspectList();
        read.readFromNBT(saved);
        assertEquals(list.aspects, read.aspects);
        assertEquals(List.of(Aspect.AIR, tempus), List.of(read.getAspects()));

        var phial = new PhialData(tempus.getTag());
        assertEquals(phial, PhialData.CODEC.parse(NbtOps.INSTANCE, PhialData.CODEC.encodeStart(NbtOps.INSTANCE, phial).getOrThrow()).getOrThrow());
        var label = new JarLabelData(tempus.getTag());
        assertEquals(label, JarLabelData.CODEC.parse(NbtOps.INSTANCE, JarLabelData.CODEC.encodeStart(NbtOps.INSTANCE, label).getOrThrow()).getOrThrow());
        var wisp = WispEssenceData.fromAspects(new AspectList().add(tempus, 2));
        var wire = Unpooled.buffer();
        try {
            PhialData.STREAM_CODEC.encode(wire, phial);
            JarLabelData.STREAM_CODEC.encode(wire, label);
            WispEssenceData.STREAM_CODEC.encode(wire, wisp);
            assertEquals(phial, PhialData.STREAM_CODEC.decode(wire));
            assertEquals(label, JarLabelData.STREAM_CODEC.decode(wire));
            assertEquals(wisp, WispEssenceData.STREAM_CODEC.decode(wire));
            assertFalse(wire.isReadable());
        } finally {
            wire.release();
        }

        // Networking contract: a side that does not know the aspect refuses the component.
        var encoded = PhialData.CODEC.encodeStart(NbtOps.INSTANCE, phial).getOrThrow();
        unregister();
        assertTrue(PhialData.CODEC.parse(NbtOps.INSTANCE, encoded).error().isPresent());
        assertThrows(IllegalArgumentException.class, () -> new JarLabelData(phial.tag()));
        AspectList orphan = new AspectList();
        orphan.readFromNBT(saved);
        assertEquals(Map.of(Aspect.AIR, 3), orphan.aspects, "an unknown saved tag is dropped, never mapped to another aspect");
    }

    @Test void theFirstJoinGrantsEveryComponentlessAspectLikeTheOriginal() {
        // Original ResearchManager.loadPlayerData:796-802 gives aspects whose components are null.
        long seed = 20261026L;
        PlayerKnowledge plain = new PlayerKnowledge();
        plain.initializePrimalAspects(RandomSource.create(seed));
        RandomSource draws = RandomSource.create(seed);
        for (String tag : PlayerKnowledge.PRIMAL_TAGS) assertEquals(15 + draws.nextInt(5), plain.getAspectPoolFor(tag), tag);
        assertEquals(6, plain.getAspectsDiscovered().size(), "the original registry draws exactly the six primals, in order");

        Aspect addonPrimal = primal();
        Aspect threeParents = new Aspect(uniqueTag(), 0x111111, new Aspect[]{Aspect.AIR, Aspect.FIRE, Aspect.WATER});
        registered.add(threeParents);
        Aspect tempus = compound(Aspect.VOID, Aspect.ORDER);
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.initializePrimalAspects(RandomSource.create(seed));
        for (String tag : PlayerKnowledge.PRIMAL_TAGS) assertEquals(plain.getAspectPoolFor(tag), knowledge.getAspectPoolFor(tag), tag);
        assertTrue(knowledge.hasDiscoveredAspect(addonPrimal.getTag()));
        int pool = knowledge.getAspectPoolFor(addonPrimal.getTag());
        assertTrue(pool >= 15 && pool <= 19, "add-on primal pool " + pool);
        assertFalse(knowledge.hasDiscoveredAspect(threeParents.getTag()), "isPrimal is not the rule: components must be null");
        assertFalse(knowledge.hasDiscoveredAspect(tempus.getTag()));
        int[] before = {knowledge.getAspectPoolFor(addonPrimal.getTag())};
        knowledge.initializePrimalAspects(RandomSource.create(1));
        assertEquals(before[0], knowledge.getAspectPoolFor(addonPrimal.getTag()), "the grant happens once");
    }

    @Test void theScanNeedsBothParentsThenDiscoversTheAddon() {
        Aspect tempus = compound(Aspect.VOID, Aspect.ORDER);
        PlayerKnowledge knowledge = new PlayerKnowledge();
        AspectList clock = new AspectList().add(tempus, 4);
        var refused = ScanManager.awardScan(knowledge, "@minecraft:clock", clock, ScanManager.ScanKind.ITEM, ScanManager.DEFAULT_ASPECT_TOTAL_CAP);
        assertFalse(refused.success());
        assertEquals(ScanManager.Failure.MISSING_PARENT, refused.failure());
        assertSame(Aspect.VOID, refused.missingParent(), "the thaumometer names tc.aspect.help.vacuos");
        assertFalse(knowledge.hasDiscoveredAspect(tempus.getTag()));
        knowledge.addDiscoveredAspect(Aspect.VOID.getTag());
        var scanned = ScanManager.awardScan(knowledge, "@minecraft:clock", clock, ScanManager.ScanKind.ITEM, ScanManager.DEFAULT_ASPECT_TOTAL_CAP);
        assertTrue(scanned.success());
        assertTrue(knowledge.hasDiscoveredAspect(tempus.getTag()));
        assertEquals(6, knowledge.getAspectPoolFor(tempus.getTag()), "amount4 plus the first-discovery bonus2");
        assertEquals(java.util.Set.of(tempus.getTag()), scanned.discovered());
    }

    @Test void theResearchTableAndTheNotesUseTheAddonComponents() {
        Aspect tempus = compound(Aspect.VOID, Aspect.ORDER);
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.setAspectPool(Aspect.VOID.getTag(), 1);
        knowledge.setAspectPool(Aspect.ORDER.getTag(), 1);
        assertTrue(ResearchAspectCombination.canCombine(knowledge, Map.of(), "vacuos", "ordo"));
        var plan = ResearchAspectCombination.plan(knowledge, Map.of(), "vacuos", "ordo");
        assertNotNull(plan);
        assertSame(tempus, plan.output());
        assertTrue(plan.discovery());
        assertEquals(3, plan.award(), "first discovery gives 1+2 RP");
        assertEquals(3, plan.knowledge().getAspectPoolFor(tempus.getTag()));
        assertEquals(0, plan.knowledge().getAspectPoolFor("vacuos"));

        assertTrue(ResearchNotePuzzle.isDirectlyConnected(tempus.getTag(), "vacuos"));
        assertTrue(ResearchNotePuzzle.isDirectlyConnected("ordo", tempus.getTag()));
        assertFalse(ResearchNotePuzzle.isDirectlyConnected(tempus.getTag(), "aer"), "shared ancestors do not link");

        assertTrue(TileResearchTable.isCanonicalBonusTag(tempus.getTag()));
        assertEquals(Map.of(tempus.getTag(), 2), TileResearchTable.canonicalBonusPools(Map.of(tempus.getTag(), 2)));
    }

    @Test void aWispOfAnAspectRegisteredAfterTheBaselineIsScannable() {
        var previous = ThaumcraftApi.scanEntities;
        try {
            ThaumcraftApi.scanEntities = new ArrayList<>(EntityAspectBindings.originalTags());
            int baseline = ThaumcraftApi.scanEntities.size();
            assertNull(EntityAspectBindings.forOriginal("Thaumcraft.Wisp", Map.of(), "addon_test_not_registered"));
            Aspect tempus = compound(Aspect.VOID, Aspect.ORDER);
            // Original ConfigAspects.registerEntityAspects:98-99: Wisp aspects2 + magic1 + air1.
            assertEquals(Map.of(tempus, 2, Aspect.MAGIC, 1, Aspect.AIR, 1),
                    EntityAspectBindings.forOriginal("Thaumcraft.Wisp", Map.of(), tempus.getTag()).aspects);
            assertEquals(baseline + 1, ThaumcraftApi.scanEntities.size(), "one Wisp row per late aspect");
            EntityAspectBindings.registerLateWispVariants();
            assertEquals(baseline + 1, ThaumcraftApi.scanEntities.size(), "the late row is appended once");
            ThaumcraftApi.scanEntities.removeIf(row -> row.nbts.length == 1 && tempus.getTag().equals(row.nbts[0].value));
            assertNull(EntityAspectBindings.forOriginal("Thaumcraft.Wisp", Map.of(), tempus.getTag()), "a row the add-on removed is not restored");
        } finally {
            ThaumcraftApi.scanEntities = previous;
        }
    }
}
