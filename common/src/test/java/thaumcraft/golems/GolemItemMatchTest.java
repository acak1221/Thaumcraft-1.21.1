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
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemGuardTargets;
import thaumcraft.common.items.golems.GolemItemMatch;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;

/** Entropy's three item-comparison switches (original InventoryUtils.areItemStacksEqual, GuiGolem:214-262): rules and placer data. */
class GolemItemMatchTest {
    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final int ENTROPY = GolemUpgrade.ENTROPY.id;
    private static final List<ItemStack> NO_FILTERS = Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY);
    private static final List<Integer> NO_COUNTS = Collections.nCopies(GolemData.FILTER_SLOTS, 0);

    @BeforeAll static void bootstrapNativeStacks() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static GolemData data(int core, List<Integer> upgrades, int flags) {
        return new GolemData(core, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT, upgrades, List.of(),
                GolemGuardTargets.DEFAULT, false, "", flags);
    }

    @Test void buttonsKeepTheOriginalIdsAndMapToTheThreeSwitches() {
        assertEquals(55, GolemItemMatch.FIRST_BUTTON);
        assertEquals(GolemItemMatch.TAGS, GolemItemMatch.flagOfButton(55));
        assertEquals(GolemItemMatch.IGNORE_DAMAGE, GolemItemMatch.flagOfButton(56));
        assertEquals(GolemItemMatch.IGNORE_COMPONENTS, GolemItemMatch.flagOfButton(57));
        for (int other : new int[] {50, 51, 54, 58, 60, 100, -1}) assertEquals(0, GolemItemMatch.flagOfButton(other), "button " + other);
        for (int flag : new int[] {1, 2, 4}) assertEquals(flag, GolemItemMatch.flagOfButton(GolemItemMatch.buttonOf(flag)));
        assertEquals(-1, GolemItemMatch.buttonOf(3));
        assertEquals(-1, GolemItemMatch.buttonOf(8));
        assertEquals(3, GolemItemMatch.toggle(1, 2));
        assertEquals(1, GolemItemMatch.toggle(5, 4));
        assertEquals(5, GolemItemMatch.toggle(1, 4));
        assertEquals(1, GolemItemMatch.toggle(1, 8), "a flag that is not a switch changes nothing");
        assertEquals(1, GolemItemMatch.toggle(1, 3), "two bits are not one switch");
    }

    @Test void theSwitchesBelongToFiveCoresAndNeedEntropy() {
        for (int core : new int[] {GolemData.FILL, GolemData.EMPTY_CORE, GolemData.GATHER, GolemData.USE, GolemData.SORTING}) {
            assertTrue(GolemItemMatch.appliesTo(core), "core " + core);
            assertTrue(GolemItemMatch.configurable(core, 1) && GolemItemMatch.configurable(core, 2));
            assertFalse(GolemItemMatch.configurable(core, 0), "no Entropy, no switches");
        }
        for (int core : new int[] {GolemData.NO_CORE, GolemData.HARVEST, GolemData.GUARD, GolemData.LIQUID, GolemData.ALCHEMY, GolemData.LUMBER,
                GolemData.BUTCHER, GolemData.FISHING}) {
            assertFalse(GolemItemMatch.appliesTo(core), "core " + core);
            assertFalse(GolemItemMatch.configurable(core, 2));
        }
        assertTrue(GolemItemMatch.acceptable(0, GolemData.GUARD, 0), "zero is always acceptable");
        assertTrue(GolemItemMatch.acceptable(7, GolemData.SORTING, 1));
        assertFalse(GolemItemMatch.acceptable(1, GolemData.SORTING, 0));
        assertFalse(GolemItemMatch.acceptable(1, GolemData.GUARD, 2));
        assertFalse(GolemItemMatch.acceptable(8, GolemData.FILL, 2), "only three bits exist");
        assertFalse(GolemItemMatch.valid(-1));
    }

    @Test void entropyWidensEmptyAndUseToTagEquivalentsWhateverTheSwitchesSay() {
        assertEquals(GolemItemMatch.TAGS, GolemItemMatch.effective(0, GolemData.EMPTY_CORE, 1));
        assertEquals(GolemItemMatch.TAGS, GolemItemMatch.effective(0, GolemData.USE, 2));
        assertEquals(GolemItemMatch.TAGS | GolemItemMatch.IGNORE_DAMAGE, GolemItemMatch.effective(GolemItemMatch.IGNORE_DAMAGE, GolemData.EMPTY_CORE, 1));
        assertEquals(0, GolemItemMatch.effective(0, GolemData.EMPTY_CORE, 0), "without Entropy nothing is widened");
        assertEquals(0, GolemItemMatch.effective(0, GolemData.FILL, 2), "Fill follows only the switches");
        assertEquals(0, GolemItemMatch.effective(0, GolemData.SORTING, 2));
        assertEquals(0, GolemItemMatch.effective(0, GolemData.GATHER, 2));
        assertEquals(GolemItemMatch.IGNORE_COMPONENTS, GolemItemMatch.effective(GolemItemMatch.IGNORE_COMPONENTS, GolemData.FILL, 1));
    }

    @Test void onlyMaterialSpecificConventionTagsAndWoodFamiliesAreGroups() {
        for (String id : List.of("c:ingots/iron", "c:ores/diamond", "c:dusts/redstone", "c:gems/emerald", "c:nuggets/gold", "c:storage_blocks/iron",
                "c:raw_materials/copper", "c:dyes/red", "c:rods/wooden", "c:crops/wheat", "minecraft:logs", "minecraft:planks",
                "minecraft:saplings", "minecraft:leaves", "minecraft:wooden_slabs", "minecraft:wooden_stairs")) {
            assertTrue(GolemItemMatch.isGroupTag(ResourceLocation.parse(id)), id);
        }
        for (String id : List.of("c:ingots", "c:ores", "c:foods", "c:/x", "c:ingots/", "c:tools/pickaxe", "c:tools/sword", "c:armors/helmet", "c:foods/fruit",
                "c:buckets/water", "minecraft:stone_bricks", "minecraft:wool", "minecraft:logs_that_burn",
                "thaumcraft:iron_ingots", "thaumcraft:shards", "other:ingots/iron")) {
            assertFalse(GolemItemMatch.isGroupTag(ResourceLocation.parse(id)), id);
        }
        assertFalse(GolemItemMatch.isGroupTag(null));
    }

    @Test void withoutSwitchesTheComparisonIsExactAndEmptiesMeetOnlyEmpties() {
        var pick = new ItemStack(Items.DIAMOND_PICKAXE);
        assertTrue(GolemItemMatch.matches(pick, new ItemStack(Items.DIAMOND_PICKAXE), 0));
        var damaged = new ItemStack(Items.DIAMOND_PICKAXE);
        damaged.setDamageValue(100);
        var named = new ItemStack(Items.DIAMOND_PICKAXE);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Rusty"));
        assertFalse(GolemItemMatch.matches(damaged, pick, 0), "damage differs");
        assertFalse(GolemItemMatch.matches(named, pick, 0), "a name differs");
        assertFalse(GolemItemMatch.matches(new ItemStack(Items.IRON_PICKAXE), pick, 0));
        assertTrue(GolemItemMatch.matches(new ItemStack(Items.DIAMOND_PICKAXE, 1), new ItemStack(Items.DIAMOND_PICKAXE), 0), "the count is never compared");
        assertTrue(GolemItemMatch.matches(new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 1), 0));
        assertTrue(GolemItemMatch.matches(ItemStack.EMPTY, ItemStack.EMPTY, 0));
        assertFalse(GolemItemMatch.matches(ItemStack.EMPTY, pick, 0));
        assertFalse(GolemItemMatch.matches(pick, ItemStack.EMPTY, GolemItemMatch.ALL));
        assertTrue(GolemItemMatch.matches(ItemStack.EMPTY, ItemStack.EMPTY, GolemItemMatch.ALL));
    }

    @Test void ignoringDamageIgnoresOnlyTheDamageComponent() {
        var pick = new ItemStack(Items.DIAMOND_PICKAXE);
        var damaged = new ItemStack(Items.DIAMOND_PICKAXE);
        damaged.setDamageValue(100);
        var damagedAndNamed = damaged.copy();
        damagedAndNamed.set(DataComponents.CUSTOM_NAME, Component.literal("Rusty"));
        int damage = GolemItemMatch.IGNORE_DAMAGE;
        assertTrue(GolemItemMatch.matches(damaged, pick, damage));
        assertTrue(GolemItemMatch.matches(pick, damaged, damage));
        assertFalse(GolemItemMatch.matches(damagedAndNamed, pick, damage), "a name still differs");
        assertFalse(GolemItemMatch.matches(new ItemStack(Items.IRON_PICKAXE), pick, damage), "the item must be the same");
        assertTrue(GolemItemMatch.matches(new ItemStack(Items.STICK), new ItemStack(Items.STICK), damage), "an undamageable item is simply equal");
        assertTrue(GolemItemMatch.matches(damagedAndNamed, damagedAndNamed.copy(), damage));
    }

    @Test void ignoringComponentsOnlyNeedsTheSameItem() {
        var pick = new ItemStack(Items.DIAMOND_PICKAXE);
        var worn = new ItemStack(Items.DIAMOND_PICKAXE);
        worn.setDamageValue(500);
        worn.set(DataComponents.CUSTOM_NAME, Component.literal("Rusty"));
        int data = GolemItemMatch.IGNORE_COMPONENTS;
        assertTrue(GolemItemMatch.matches(worn, pick, data));
        assertTrue(GolemItemMatch.matches(pick, worn, data));
        assertFalse(GolemItemMatch.matches(new ItemStack(Items.IRON_PICKAXE), pick, data));
        assertTrue(GolemItemMatch.matches(worn, pick, data | GolemItemMatch.IGNORE_DAMAGE));
    }

    @Test void theItemMatchRoundTripsThroughJsonNbtAndTheNetworkAndIsOmittedAtZero() {
        var plain = data(GolemData.FILL, List.of(ENTROPY), 0);
        JsonObject plainJson = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, plain).getOrThrow().getAsJsonObject();
        assertFalse(plainJson.has("item_match"), "earlier stacks keep their exact serialised form");
        for (int flags = 1; flags <= 7; flags++) {
            var data = data(GolemData.SORTING, List.of(ENTROPY, ENTROPY), flags);
            JsonObject json = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow().getAsJsonObject();
            assertEquals(flags, json.get("item_match").getAsInt());
            assertEquals(data, GolemData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), "json " + flags);
            assertEquals(data, GolemData.CODEC.parse(NbtOps.INSTANCE, GolemData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getOrThrow(), "nbt " + flags);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            GolemData.STREAM_CODEC.encode(buffer, data);
            assertEquals(data, GolemData.STREAM_CODEC.decode(buffer), "network " + flags);
            assertEquals(0, buffer.readableBytes());
        }
        var everything = data(GolemData.USE, List.of(ENTROPY), 5).withDecoration("HGM").withAdvanced(true);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        GolemData.STREAM_CODEC.encode(buffer, everything);
        assertEquals(everything, GolemData.STREAM_CODEC.decode(buffer), "flags, decoration and advanced together");
    }

    @Test void theNetworkAppendsTheSwitchesAfterTheDecorationAndKeepsPlainGolemsUnchanged() {
        var plainBuffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        GolemData.STREAM_CODEC.encode(plainBuffer, data(GolemData.FILL, List.of(ENTROPY), 0));
        byte[] plain = new byte[plainBuffer.readableBytes()];
        plainBuffer.getBytes(0, plain);
        var matched = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        GolemData.STREAM_CODEC.encode(matched, data(GolemData.FILL, List.of(ENTROPY), 6).withDecoration("H"));
        byte[] bytes = new byte[matched.readableBytes()];
        matched.getBytes(0, bytes);
        int tail = plain.length - 1;
        assertEquals(plain.length + 3, bytes.length, "a decoration string (length + letter) and one switch byte follow the trailing byte");
        assertEquals(0x20 | 0x40 | GolemGuardTargets.DEFAULT, bytes[tail] & 0xFF);
        assertEquals(1, bytes[tail + 1]);
        assertEquals('H', bytes[tail + 2]);
        assertEquals(6, bytes[tail + 3]);
        for (int bad : new int[] {0, 8, 200}) {
            byte[] corrupt = bytes.clone();
            corrupt[tail + 3] = (byte) bad;
            assertThrows(DecoderException.class, () -> GolemData.STREAM_CODEC.decode(
                    new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(corrupt), RegistryAccess.EMPTY)), "switch byte " + bad);
        }
        for (int bad : new int[] {0x80, 0xC0, 0xFF}) {
            byte[] corrupt = bytes.clone();
            corrupt[tail] = (byte) bad;
            assertThrows(DecoderException.class, () -> GolemData.STREAM_CODEC.decode(
                    new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(corrupt), RegistryAccess.EMPTY)), "tail " + bad);
        }
    }

    @Test void switchesWithoutEntropyOrOnAnotherCoreAreRefusedEverywhere() {
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.FILL, List.of(), 1), "no Entropy");
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.GUARD, List.of(ENTROPY), 1), "a Guard has no such switches");
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.FILL, List.of(ENTROPY), 8));
        assertThrows(IllegalArgumentException.class, () -> data(GolemData.FILL, List.of(GolemUpgrade.ORDER.id), 1), "Order is not Entropy");
        var good = data(GolemData.FILL, List.of(ENTROPY), 3);
        JsonObject json = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, good).getOrThrow().getAsJsonObject();
        var withoutEntropy = json.deepCopy();
        withoutEntropy.remove("upgrades");
        assertTrue(GolemData.CODEC.parse(JsonOps.INSTANCE, withoutEntropy).error().isPresent(), "stripping the Entropy upgrade orphans the switches");
        for (String bad : List.of("8", "-1", "\"1\"", "1.5", "true", "[]", "null")) {
            var forged = json.deepCopy();
            forged.add("item_match", JsonParser.parseString(bad));
            var decoded = GolemData.CODEC.parse(JsonOps.INSTANCE, forged);
            assertTrue(decoded.error().isPresent() && decoded.result().isEmpty(), bad);
        }
    }

    @Test void equalityAndHashingCoverTheSwitchesAndWithItemMatchCarriesEverythingOver() {
        var base = data(GolemData.SORTING, List.of(ENTROPY), 3);
        assertEquals(base, data(GolemData.SORTING, List.of(ENTROPY), 3));
        assertEquals(base.hashCode(), data(GolemData.SORTING, List.of(ENTROPY), 3).hashCode());
        assertNotEquals(base, data(GolemData.SORTING, List.of(ENTROPY), 1));
        assertNotEquals(base.hashCode(), data(GolemData.SORTING, List.of(ENTROPY), 5).hashCode());
        assertSame(base, base.withItemMatch(3));
        var decorated = base.withDecoration("BM").withAdvanced(true);
        var changed = decorated.withItemMatch(4);
        assertTrue(changed.itemMatch() == 4 && changed.decoration().equals("BM") && changed.advanced() && changed.core() == GolemData.SORTING
                && changed.upgrades().equals(List.of(ENTROPY)), "changing the switches carries every other field over");
        assertEquals(0, changed.withItemMatch(0).itemMatch());
        assertEquals(1, GolemData.entropyCount(List.of(ENTROPY, GolemUpgrade.FIRE.id)));
        assertEquals(2, GolemData.entropyCount(List.of(ENTROPY, ENTROPY)));
        assertEquals(0, GolemData.entropyCount(null));
    }
}
