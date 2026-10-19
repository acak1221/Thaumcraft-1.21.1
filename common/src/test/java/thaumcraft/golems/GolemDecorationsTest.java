package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.Collections;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemDecorations;
import thaumcraft.common.items.golems.GolemGuardTargets;
import thaumcraft.common.items.golems.GolemUpgrades;
import thaumcraft.common.items.golems.GolemUseOptions;

/** Golem accessories (original ItemGolemDecoration, E:206-216, 329-331, 388, 419-423, 868-889, 1047-1052): rules and placer data. */
class GolemDecorationsTest {
    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final List<ItemStack> NO_FILTERS = Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY);
    private static final List<Integer> NO_COUNTS = Collections.nCopies(GolemData.FILTER_SLOTS, 0);

    @BeforeAll static void bootstrapNativeStacks() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static GolemData data(String decoration) {
        return GolemData.EMPTY.withDecoration(decoration);
    }

    @Test void itemDamageOrderMapsToTheOriginalLetters() {
        assertEquals('H', GolemDecorations.letterOfDamage(0));
        assertEquals('G', GolemDecorations.letterOfDamage(1));
        assertEquals('B', GolemDecorations.letterOfDamage(2));
        assertEquals('F', GolemDecorations.letterOfDamage(3));
        assertEquals('R', GolemDecorations.letterOfDamage(4));
        assertEquals('V', GolemDecorations.letterOfDamage(5));
        assertEquals('P', GolemDecorations.letterOfDamage(6));
        assertEquals('M', GolemDecorations.letterOfDamage(7));
        assertEquals(0, GolemDecorations.letterOfDamage(8));
        assertEquals(0, GolemDecorations.letterOfDamage(-1));
        assertTrue(GolemDecorations.ALL.indexOf('R') == 4 && GolemDecorations.isSupported('R'), "the dart launcher has its attack goal");
        assertEquals(GolemDecorations.ALL, GolemDecorations.SUPPORTED, "every original letter is honoured now");
        for (char letter : GolemDecorations.SUPPORTED.toCharArray()) assertTrue(GolemDecorations.isSupported(letter), "" + letter);
        assertFalse(GolemDecorations.isSupported('X'));
    }

    @Test void aLetterIsInstalledOnceAndPartnersExcludeEachOther() {
        assertEquals("H", GolemDecorations.add("", 'H'));
        assertNull(GolemDecorations.add("H", 'H'), "no repeat");
        for (String pair : List.of("HF", "FH", "GV", "VG", "BP", "PB")) {
            assertNull(GolemDecorations.add(pair.substring(0, 1), pair.charAt(1)), pair + " are partners");
            assertFalse(GolemDecorations.canAdd(pair.substring(0, 1), pair.charAt(1)));
        }
        // Unrelated letters combine in any order, kept in installation order.
        assertEquals("HGBM", GolemDecorations.add(GolemDecorations.add(GolemDecorations.add(GolemDecorations.add("", 'H'), 'G'), 'B'), 'M'));
        assertEquals("MBGH", GolemDecorations.add(GolemDecorations.add(GolemDecorations.add(GolemDecorations.add("", 'M'), 'B'), 'G'), 'H'));
        assertEquals("FVP", GolemDecorations.add(GolemDecorations.add(GolemDecorations.add("", 'F'), 'V'), 'P'));
        assertEquals("R", GolemDecorations.add("", 'R'), "the dart launcher installs like any other letter");
        assertNull(GolemDecorations.add("R", 'R'), "once");
        assertEquals("HRM", GolemDecorations.add(GolemDecorations.add(GolemDecorations.add("", 'H'), 'R'), 'M'), "and has no partner");
        assertNull(GolemDecorations.add("", 'X'));
    }

    @Test void onlySequencesTheInstallationRulesCouldProduceAreValid() {
        for (String good : List.of("", "H", "HGBM", "FVP", "MPV", "BVFM", "R", "HRM")) assertTrue(GolemDecorations.valid(good), good);
        for (String bad : List.of("HH", "HF", "FH", "GV", "BP", "RR", "HRR", "X", "h", "HGBFVPMM", " ")) assertFalse(GolemDecorations.valid(bad), bad);
        assertFalse(GolemDecorations.valid(null));
        // The longest honest decoration uses one of each pair plus the dart launcher and the mace: at most five letters.
        String longest = "";
        for (char letter : "HGBFRVPM".toCharArray()) { String next = GolemDecorations.add(longest, letter); if (next != null) longest = next; }
        assertEquals("HGBRM", longest);
    }

    @Test void effectsAreSingleLetterLookups() {
        assertEquals(5, GolemDecorations.maxHealthBonus("H"));
        assertEquals(5, GolemDecorations.maxHealthBonus("MH"));
        assertEquals(0, GolemDecorations.maxHealthBonus("F"));
        assertEquals(2, GolemDecorations.meleeDamageBonus("M"));
        assertEquals(0, GolemDecorations.meleeDamageBonus("HG"));
        assertEquals(1.0f, GolemDecorations.speedFactor(""), 0f);
        assertEquals(1.1f, GolemDecorations.speedFactor("B"), 0f);
        assertEquals(0.88f, GolemDecorations.speedFactor("P"), 0f);
        assertEquals(1.0f, GolemDecorations.speedFactor("HGFVM"), 0f, "unrelated letters do not touch the speed");
        assertEquals(100, GolemDecorations.regenDelay(100, "VM"));
        assertEquals((int) (100f * 0.66f), GolemDecorations.regenDelay(100, "F"));
        assertEquals((int) (75f * 0.66f), GolemDecorations.regenDelay(75, "F"), "the product is a float truncated to int");
        assertEquals(0f, GolemDecorations.rangeBonus(16f, "BM"), 0f);
        assertEquals(16f * 0.1f, GolemDecorations.rangeBonus(16f, "G"), 0f);
        assertEquals(1.0f, GolemDecorations.rangeBonus(4f, "G"), 0f, "never less than one");
        assertTrue(GolemDecorations.marksRecentHit("V") && !GolemDecorations.marksRecentHit("HGBM"));
        assertEquals(100, GolemDecorations.RECENT_HIT_TICKS);
    }

    @Test void armourAddsOneAndFourAndStopsAtTwenty() {
        assertEquals(9, GolemDecorations.armor(9, ""));
        assertEquals(10, GolemDecorations.armor(9, "V"));
        assertEquals(13, GolemDecorations.armor(9, "P"));
        assertEquals(14, GolemDecorations.armor(9, "VP"));
        assertEquals(20, GolemDecorations.armor(15, "VP"), "15 + 1 + 4 = 20 is allowed");
        assertEquals(20, GolemDecorations.armor(18, "P"), "capped at 20");
        assertEquals(20, GolemDecorations.armor(30, ""), "the cap applies to the whole value");
    }

    @Test void upgradeArithmeticFoldsTheAccessoriesInTheOriginalOrder() {
        // Undecorated results are bit-for-bit the earlier ones.
        assertEquals(GolemUpgrades.speed(0.33F, 1, true), GolemUpgrades.speed(0.33F, 1, true, ""), 0f);
        assertEquals(GolemUpgrades.range(2, true), GolemUpgrades.range(2, true, ""), 0f);
        assertEquals(GolemUpgrades.meleeDamage(4, 1), GolemUpgrades.meleeDamage(4, 1, ""), 0f);
        // Speed: material x bowtie/plating, then Air, then advanced.
        float expected = 0.33F * 1.1F;
        expected *= 1.0F + 2 * 0.15F;
        expected *= 1.1F;
        assertEquals(expected, GolemUpgrades.speed(0.33F, 2, true, "B"), 0f);
        float plated = 0.33F * 0.88F;
        plated *= 1.0F + 1 * 0.15F;
        assertEquals(plated, GolemUpgrades.speed(0.33F, 1, false, "P"), 0f);
        // Range: spectacles first, then the advanced golem's share of the enlarged value.
        float range = 16F;
        range += Math.max(range * 0.1F, 1.0F);
        assertEquals(range, GolemUpgrades.range(0, false, "G"), 0f);
        range += Math.max(range * 0.2F, 2.0F);
        assertEquals(range, GolemUpgrades.range(0, true, "G"), 0f);
        float wide = 24F;
        wide += Math.max(wide * 0.1F, 1.0F);
        assertEquals(wide, GolemUpgrades.range(2, false, "G"), 0f);
        assertEquals(GolemUpgrades.range(1), GolemUpgrades.range(1, false, "HBM"), 0f, "other letters leave the range alone");
        // Melee: Earth is counted twice (original quirk), the mace adds two more.
        assertEquals(2 + 4 + 1 + 1 + 2, GolemUpgrades.meleeDamage(4, 1, "M"), 0f);
    }

    @Test void theDecorationRoundTripsThroughJsonNbtAndTheNetworkAndIsOmittedWhileEmpty() {
        var plain = GolemData.EMPTY;
        JsonObject plainJson = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, plain).getOrThrow().getAsJsonObject();
        assertFalse(plainJson.has("decoration"), "earlier stacks keep their exact serialised form");
        var decorated = data("HGBM").withAdvanced(true);
        JsonObject json = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, decorated).getOrThrow().getAsJsonObject();
        assertEquals("HGBM", json.get("decoration").getAsString());
        assertEquals(decorated, GolemData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertEquals(decorated, GolemData.CODEC.parse(NbtOps.INSTANCE, GolemData.CODEC.encodeStart(NbtOps.INSTANCE, decorated).getOrThrow()).getOrThrow());
        for (var sample : List.of(data("H"), data("FVP"), data("MBGH").withAdvanced(true))) {
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            GolemData.STREAM_CODEC.encode(buffer, sample);
            assertEquals(sample, GolemData.STREAM_CODEC.decode(buffer), sample.decoration());
            assertEquals(0, buffer.readableBytes());
        }
    }

    @Test void aPlainGolemKeepsItsExactNetworkEndingAndADecoratedOneAddsAMarkedString() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        GolemData.STREAM_CODEC.encode(buffer, GolemData.EMPTY);
        byte[] plain = new byte[buffer.readableBytes()];
        buffer.getBytes(0, plain);
        assertEquals(GolemGuardTargets.DEFAULT, plain[plain.length - 1], "the trailing byte is the default Guard mask, every flag clear");
        var decorated = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        GolemData.STREAM_CODEC.encode(decorated, data("HM"));
        byte[] bytes = new byte[decorated.readableBytes()];
        decorated.getBytes(0, bytes);
        assertEquals(plain.length + 3, bytes.length, "a length byte and two letters follow the trailing byte");
        assertEquals(0x20 | GolemGuardTargets.DEFAULT, bytes[plain.length - 1] & 0xFF, "bit 5 announces the string");
        assertEquals(2, bytes[plain.length]);
        assertEquals('H', bytes[plain.length + 1]);
        assertEquals('M', bytes[plain.length + 2]);
        for (int bad : new int[] {0x80, 0xA0, 0xFF}) {
            byte[] corrupt = bytes.clone();
            corrupt[plain.length - 1] = (byte) bad;
            assertThrows(DecoderException.class, () -> GolemData.STREAM_CODEC.decode(
                    new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(corrupt), RegistryAccess.EMPTY)), "tail " + bad);
        }
        // A marked but empty string, or a string that breaks the installation rules, is corrupt.
        byte[] empty = java.util.Arrays.copyOf(bytes, plain.length + 1);
        empty[plain.length] = 0;
        assertThrows(DecoderException.class, () -> GolemData.STREAM_CODEC.decode(
                new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(empty), RegistryAccess.EMPTY)));
        byte[] partners = bytes.clone();
        partners[plain.length + 1] = 'H';
        partners[plain.length + 2] = 'F';
        assertThrows(DecoderException.class, () -> GolemData.STREAM_CODEC.decode(
                new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(partners), RegistryAccess.EMPTY)));
    }

    @Test void impossibleDecorationsAreRefusedEverywhereNotDefaulted() {
        for (String bad : List.of("HF", "RR", "HH", "GV", "x")) {
            assertThrows(IllegalArgumentException.class, () -> GolemData.EMPTY.withDecoration(bad), bad);
            JsonObject json = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, GolemData.EMPTY).getOrThrow().getAsJsonObject();
            json.addProperty("decoration", bad);
            var decoded = GolemData.CODEC.parse(JsonOps.INSTANCE, json);
            assertTrue(decoded.error().isPresent() && decoded.result().isEmpty(), bad);
        }
        for (String malformed : List.of("1", "true", "[]", "{}")) {
            JsonObject json = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, GolemData.EMPTY).getOrThrow().getAsJsonObject();
            json.add("decoration", JsonParser.parseString(malformed));
            assertTrue(GolemData.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent(), malformed);
        }
    }

    @Test void equalityAndHashingCoverTheDecoration() {
        var base = data("HG");
        assertEquals(base, data("HG"));
        assertEquals(base.hashCode(), data("HG").hashCode());
        assertNotEquals(base, data("GH"), "installation order is part of the value");
        assertNotEquals(base, data("H"));
        assertNotEquals(base, GolemData.EMPTY);
        assertNotEquals(base.hashCode(), GolemData.EMPTY.hashCode());
        assertSame(base, base.withDecoration("HG"));
        assertEquals(GolemData.EMPTY, base.withDecoration(""));
        var withOthers = new GolemData(GolemData.GUARD, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT, List.of(4),
                List.of(), GolemGuardTargets.ANIMALS, true, "BM");
        assertTrue(withOthers.advanced() && withOthers.guardTargets() == GolemGuardTargets.ANIMALS && withOthers.upgrades().equals(List.of(4)));
        var again = withOthers.withDecoration("");
        assertTrue(again.advanced() && again.guardTargets() == GolemGuardTargets.ANIMALS && again.upgrades().equals(List.of(4)) && again.decoration().isEmpty(),
                "changing the decoration carries every other field over");
    }
}
