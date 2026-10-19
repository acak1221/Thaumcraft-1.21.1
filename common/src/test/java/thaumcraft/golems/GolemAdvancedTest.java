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
import thaumcraft.common.items.golems.GolemGuardTargets;
import thaumcraft.common.items.golems.GolemUpgrades;
import thaumcraft.common.items.golems.GolemUseOptions;

/** Advanced golem (ADVANCEDGOLEM, original E:149, 376-383, 460-469, 1148): arithmetic and placer data. */
class GolemAdvancedTest {
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

    private static GolemData data(boolean advanced, List<Integer> upgrades) {
        return new GolemData(GolemData.HARVEST, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT, upgrades,
                List.of(), GolemGuardTargets.DEFAULT, advanced);
    }

    @Test void anAdvancedGolemHasOneMoreUpgradeSlot() {
        for (int material : new int[] {1, 2}) {
            assertEquals(material, GolemUpgrades.slotCount(material, false));
            assertEquals(material + 1, GolemUpgrades.slotCount(material, true));
        }
        assertEquals(GolemUpgrades.MAX_SLOTS, GolemUpgrades.slotCount(2, true), "Thaumium + advanced fills the three-slot maximum");
        assertNotNull(GolemUpgrades.fit(List.of(0, 1, 5), GolemUpgrades.slotCount(2, true)));
        assertNull(GolemUpgrades.fit(List.of(0, 1, 5), GolemUpgrades.slotCount(2, false)), "never silently truncated");
        assertNull(GolemUpgrades.fit(List.of(0, 1), GolemUpgrades.slotCount(1, false)));
        assertNotNull(GolemUpgrades.fit(List.of(0, 1), GolemUpgrades.slotCount(1, true)));
    }

    @Test void speedIsAirThenTimesOnePointOne() {
        assertEquals(0.38F, GolemUpgrades.speed(0.38F, 0, false), 0F);
        assertEquals(0.38F * 1.1F, GolemUpgrades.speed(0.38F, 0, true), 0F);
        float expected = 0.38F * (1.0F + 2 * 0.15F);
        expected *= 1.1F;
        assertEquals(expected, GolemUpgrades.speed(0.38F, 2, true), 0F, "float products in the original order");
        assertEquals(GolemUpgrades.speed(0.33F, 1), GolemUpgrades.speed(0.33F, 1, false), 0F, "the two-argument form stays ordinary");
    }

    @Test void rangeAddsTwentyPercentOrTwoWhicheverIsLarger() {
        assertEquals(16F, GolemUpgrades.range(0, false));
        assertEquals(16F + 3.2F, GolemUpgrades.range(0, true), 1e-6F);
        assertEquals(20F + 4F, GolemUpgrades.range(1, true), 1e-6F);
        assertEquals(24F + 24F * 0.2F, GolemUpgrades.range(2, true), 1e-6F);
        for (int water = 0; water <= 2; water++) {
            assertEquals(GolemUpgrades.range(water), GolemUpgrades.range(water, false), 0F);
            assertTrue(GolemUpgrades.range(water, true) >= GolemUpgrades.range(water) + 2F);
        }
    }

    @Test void attackDelayIsEighteenTicksWhenAdvanced() {
        assertEquals(20, GolemUpgrades.attackSpeed(false));
        assertEquals(18, GolemUpgrades.attackSpeed(true));
    }

    @Test void theFlagRoundTripsThroughJsonNbtAndTheNetworkAndIsOmittedWhileFalse() {
        var advanced = data(true, List.of(0, 1, 5));
        var plain = data(false, List.of(0, 1));
        JsonObject plainJson = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, plain).getOrThrow().getAsJsonObject();
        assertFalse(plainJson.has("advanced"), "earlier stacks keep their exact serialised form");
        JsonObject advancedJson = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, advanced).getOrThrow().getAsJsonObject();
        assertTrue(advancedJson.get("advanced").getAsBoolean());
        assertEquals(advanced, GolemData.CODEC.parse(JsonOps.INSTANCE, advancedJson).getOrThrow());
        assertEquals(plain, GolemData.CODEC.parse(JsonOps.INSTANCE, plainJson).getOrThrow());
        assertEquals(advanced, GolemData.CODEC.parse(NbtOps.INSTANCE, GolemData.CODEC.encodeStart(NbtOps.INSTANCE, advanced).getOrThrow()).getOrThrow());

        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        GolemData.STREAM_CODEC.encode(buffer, advanced);
        assertEquals(advanced, GolemData.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
        buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        GolemData.STREAM_CODEC.encode(buffer, plain);
        assertFalse(GolemData.STREAM_CODEC.decode(buffer).advanced());
    }

    @Test void theFlagSharesTheTrailingNetworkByteWithTheGuardMask() {
        var guard = new GolemData(GolemData.GUARD, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT,
                List.of(4), List.of(), GolemGuardTargets.ANIMALS | GolemGuardTargets.PLAYERS, true);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        GolemData.STREAM_CODEC.encode(buffer, guard);
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.getBytes(0, bytes);
        assertEquals(0x10 | GolemGuardTargets.ANIMALS | GolemGuardTargets.PLAYERS, bytes[bytes.length - 1] & 0xFF);
        var decoded = GolemData.STREAM_CODEC.decode(buffer);
        assertEquals(guard, decoded);
        assertEquals(GolemGuardTargets.ANIMALS | GolemGuardTargets.PLAYERS, decoded.guardTargets());
        assertTrue(decoded.advanced());
        // Any other bit of that byte is corrupt, never a silently ignored flag (bits 5 and 6 announce a decoration and an item-match byte).
        bytes[bytes.length - 1] |= 0x80;
        assertThrows(DecoderException.class, () -> GolemData.STREAM_CODEC.decode(
                new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), RegistryAccess.EMPTY)));
    }

    @Test void aMalformedFlagIsRefusedNotDefaulted() {
        JsonObject base = GolemData.CODEC.encodeStart(JsonOps.INSTANCE, GolemData.EMPTY).getOrThrow().getAsJsonObject();
        for (String bad : List.of("\"true\"", "1.5", "[]", "{}", "null")) {
            JsonObject json = base.deepCopy();
            json.add("advanced", JsonParser.parseString(bad));
            var decoded = GolemData.CODEC.parse(JsonOps.INSTANCE, json);
            assertTrue(decoded.error().isPresent() && decoded.result().isEmpty(), bad);
        }
    }

    @Test void withAdvancedKeepsEverythingElseAndEqualityCoversTheFlag() {
        var plain = data(false, List.of(1, 3));
        var advanced = plain.withAdvanced(true);
        assertNotEquals(plain, advanced);
        assertNotEquals(plain.hashCode(), advanced.hashCode());
        assertTrue(advanced.advanced());
        assertSame(plain, plain.withAdvanced(false));
        assertEquals(plain, advanced.withAdvanced(false));
        assertEquals(plain.upgrades(), advanced.upgrades());
        assertEquals(plain.core(), advanced.core());
        assertFalse(GolemData.EMPTY.advanced());
        assertTrue(GolemData.EMPTY.withAdvanced(true).advanced());
    }
}
