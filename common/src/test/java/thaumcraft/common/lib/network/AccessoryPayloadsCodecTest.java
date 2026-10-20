package thaumcraft.common.lib.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The three payloads of the accessory screen and the architect key: two carry nothing, one carries the four worn items. */
class AccessoryPayloadsCodecTest {
    private static RegistryAccess access;

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test void theOpenRequestAndTheArchitectKeyAreEmptyPackets() {
        assertEquals(0, PayloadWire.bytes(AccessoryOpenPayload.STREAM_CODEC, new AccessoryOpenPayload()).length);
        assertEquals(new AccessoryOpenPayload(), PayloadWire.decode(AccessoryOpenPayload.STREAM_CODEC));
        assertEquals(0, PayloadWire.bytes(WandArchitectPayload.STREAM_CODEC, new WandArchitectPayload()).length);
        assertEquals(new WandArchitectPayload(), PayloadWire.decode(WandArchitectPayload.STREAM_CODEC));
    }

    @Test void anEmptyPacketRefusesAnyTrailingByteSoNothingHidesInIt() {
        assertThrows(DecoderException.class, () -> PayloadWire.decode(AccessoryOpenPayload.STREAM_CODEC, 0));
        assertThrows(DecoderException.class, () -> PayloadWire.decode(WandArchitectPayload.STREAM_CODEC, 1, 2, 3));
    }

    @Test void fourEmptySlotsAreFourBytes() {
        AccessoryContentsPayload empty = new AccessoryContentsPayload(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        byte[] wire = PayloadWire.bytes(AccessoryContentsPayload.STREAM_CODEC, empty, access);
        assertArrayEquals(new int[] {0, 0, 0, 0}, PayloadWire.unsigned(wire));
        AccessoryContentsPayload decoded = PayloadWire.decode(AccessoryContentsPayload.STREAM_CODEC, access, PayloadWire.unsigned(wire));
        assertEquals(4, decoded.stacks().size());
        assertTrue(decoded.stacks().stream().allMatch(ItemStack::isEmpty));
    }

    @Test void wornItemsKeepTheirSlotOrderAndCountsThroughTheWire() {
        List<ItemStack> worn = List.of(new ItemStack(Items.DIAMOND, 3), ItemStack.EMPTY, new ItemStack(Items.GOLD_INGOT, 1), new ItemStack(Items.STICK, 64));
        AccessoryContentsPayload payload = new AccessoryContentsPayload(worn);
        AccessoryContentsPayload decoded = PayloadWire.decode(AccessoryContentsPayload.STREAM_CODEC, access,
                PayloadWire.unsigned(PayloadWire.bytes(AccessoryContentsPayload.STREAM_CODEC, payload, access)));
        for (int slot = 0; slot < 4; slot++) assertTrue(ItemStack.matches(worn.get(slot), decoded.stacks().get(slot)), "slot " + slot);
    }

    @Test void theContentsAreAlwaysExactlyFourAndAreCopiedOnConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new AccessoryContentsPayload(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new AccessoryContentsPayload(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)));
        assertThrows(IllegalArgumentException.class, () -> new AccessoryContentsPayload(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)));
        ItemStack live = new ItemStack(Items.DIAMOND, 2);
        AccessoryContentsPayload payload = new AccessoryContentsPayload(List.of(live, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        live.setCount(40);
        assertEquals(2, payload.stacks().get(0).getCount(), "a later change to the player's stack does not alter the queued packet");
        assertNotSame(live, payload.stacks().get(0));
    }

    @Test void extraBytesAfterTheFourSlotsAreRefused() {
        assertThrows(DecoderException.class, () -> PayloadWire.decode(AccessoryContentsPayload.STREAM_CODEC, access, 0, 0, 0, 0, 0));
    }
}
