package thaumic.tinkerer.tileblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.common.item.ItemConnector;
import thaumic.tinkerer.common.network.packet.PacketPlacerButton;
import thaumic.tinkerer.common.network.packet.PacketTabletButton;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The Transvector Binder's memory of a device (in the stack's custom data, where 1.7.10 used {@code posy == -1} for
 * "nothing remembered", a real height now) and the wire format of the two menu packets.</p>
 */
class ConnectorAndPacketTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void aFreshBinderRemembersNothing() {
        ItemStack binder = new ItemStack(Items.STICK);
        assertFalse(ItemConnector.hasTarget(binder));
        assertFalse(ItemConnector.getConnectingGolem(binder));
    }

    @Test
    void theBinderRemembersAnyHeightIncludingMinusOne() {
        for (BlockPos pos : new BlockPos[] { new BlockPos(3, 70, -8), new BlockPos(0, -1, 0), new BlockPos(-30000000, -64, 29999999) }) {
            ItemStack binder = new ItemStack(Items.STICK);
            ItemConnector.setTarget(binder, pos);
            assertTrue(ItemConnector.hasTarget(binder), pos.toString());
            assertEquals(pos, ItemConnector.getTarget(binder));
        }
    }

    @Test
    void forgettingTheDeviceLeavesTheOtherDataAlone() {
        ItemStack binder = new ItemStack(Items.STICK);
        CompoundTag other = new CompoundTag();
        other.putString("mine", "kept");
        binder.set(DataComponents.CUSTOM_DATA, CustomData.of(other));

        ItemConnector.setTarget(binder, new BlockPos(1, 2, 3));
        ItemConnector.clearTarget(binder);

        assertFalse(ItemConnector.hasTarget(binder));
        assertEquals("kept", binder.get(DataComponents.CUSTOM_DATA).copyTag().getString("mine"));
    }

    @Test
    void forgettingTheOnlyDataRemovesTheComponent() {
        ItemStack binder = new ItemStack(Items.STICK);
        ItemConnector.setTarget(binder, new BlockPos(1, 2, 3));
        ItemConnector.clearTarget(binder);
        assertNull(binder.get(DataComponents.CUSTOM_DATA));
    }

    @Test
    void theGolemFlagIsKeptSeparately() {
        ItemStack binder = new ItemStack(Items.STICK);
        ItemConnector.setConnectingGolem(binder, true);
        assertTrue(ItemConnector.getConnectingGolem(binder));
        assertFalse(ItemConnector.hasTarget(binder));
        ItemConnector.setConnectingGolem(binder, false);
        assertFalse(ItemConnector.getConnectingGolem(binder));
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    @Test
    void theTabletPacketSurvivesTheWire() {
        for (boolean left : new boolean[] { true, false }) {
            for (boolean redstone : new boolean[] { true, false }) {
                PacketTabletButton sent = new PacketTabletButton(new BlockPos(-5, 12, 900), left, redstone);
                RegistryFriendlyByteBuf buf = buffer();
                PacketTabletButton.STREAM_CODEC.encode(buf, sent);
                assertEquals(sent, PacketTabletButton.STREAM_CODEC.decode(buf));
                assertEquals(0, buf.readableBytes(), "the packet is read completely");
            }
        }
    }

    @Test
    void thePlacerPacketSurvivesTheWire() {
        PacketPlacerButton sent = new PacketPlacerButton(new BlockPos(7, -3, 11), 4);
        RegistryFriendlyByteBuf buf = buffer();
        PacketPlacerButton.STREAM_CODEC.encode(buf, sent);
        assertEquals(sent, PacketPlacerButton.STREAM_CODEC.decode(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void theTwoPacketsHaveDistinctTypes() {
        assertEquals("thaumictinkerer:tablet_button", PacketTabletButton.TYPE.id().toString());
        assertEquals("thaumictinkerer:placer_button", PacketPlacerButton.TYPE.id().toString());
    }
}
