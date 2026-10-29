package thaumic.tinkerer.enchantments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.client.gui.EnchantIcons;
import thaumic.tinkerer.common.enchantment.ModEnchantments;
import thaumic.tinkerer.common.lib.LibEnchantIDs;
import thaumic.tinkerer.common.network.packet.PacketEnchanterAddEnchant;
import thaumic.tinkerer.common.network.packet.PacketEnchanterStartWorking;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The wire format of the two packets of the Enchanter's screen, the reading of a level off a stack, and the size of the
 * icon textures (the screen draws a 32 pixel and a 512 pixel texture onto the same 16 by 16 button).</p>
 */
class EnchanterPacketsAndIconsTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    @Test
    void theAddEnchantPacketCarriesThePositionTheEnchantmentAndTheLevel() {
        for (int level : new int[] { -1, 0, 1, 5 }) {
            PacketEnchanterAddEnchant sent = new PacketEnchanterAddEnchant(new BlockPos(-12, 63, 40000),
                    ResourceLocation.parse("thaumictinkerer:final_strike"), level);
            RegistryFriendlyByteBuf buf = buffer();
            PacketEnchanterAddEnchant.STREAM_CODEC.encode(buf, sent);
            PacketEnchanterAddEnchant received = PacketEnchanterAddEnchant.STREAM_CODEC.decode(buf);
            assertEquals(sent, received, "level " + level);
            assertEquals(0, buf.readableBytes());
        }
    }

    @Test
    void theStartPacketCarriesThePosition() {
        PacketEnchanterStartWorking sent = new PacketEnchanterStartWorking(new BlockPos(1, -60, -7));
        RegistryFriendlyByteBuf buf = buffer();
        PacketEnchanterStartWorking.STREAM_CODEC.encode(buf, sent);
        assertEquals(sent, PacketEnchanterStartWorking.STREAM_CODEC.decode(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void thePacketsHaveDistinctIdsInTheNamespaceOfTheAddOn() {
        assertEquals("thaumictinkerer:enchanter_add_enchant", PacketEnchanterAddEnchant.TYPE.id().toString());
        assertEquals("thaumictinkerer:enchanter_start_working", PacketEnchanterStartWorking.TYPE.id().toString());
    }

    @Test
    void aLevelIsReadOffTheEnchantmentsComponentOfAStack() {
        // standalone holders are enough: the level is found by the registry key of the holder
        HolderOwner<Enchantment> owner = new HolderOwner<>() {};
        Holder<Enchantment> shatter = Holder.Reference.createStandAlone(owner, LibEnchantIDs.shatter);
        Holder<Enchantment> sharpness = Holder.Reference.createStandAlone(owner, Enchantments.SHARPNESS);

        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        assertEquals(0, ModEnchantments.level(pickaxe, ModEnchantments.shatter));

        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(shatter, 3);
        enchantments.set(sharpness, 2);
        pickaxe.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());

        assertEquals(3, ModEnchantments.level(pickaxe, ModEnchantments.shatter));
        assertEquals(2, ModEnchantments.level(pickaxe, Enchantments.SHARPNESS));
        assertEquals(0, ModEnchantments.level(pickaxe, ModEnchantments.tunnel));
        assertEquals(0, ModEnchantments.level(ItemStack.EMPTY, ModEnchantments.shatter));
    }

    private static byte[] header(String resource) throws IOException {
        try (InputStream in = EnchanterPacketsAndIconsTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource);
            return in.readNBytes(24);
        }
    }

    @Test
    void theSizeOfAnIconIsReadFromItsPng() throws IOException {
        // the eight textures of the add-on's own enchantments are 512 pixels, the others 32
        for (String big : new String[] { "pounce", "tunnel", "shatter", "shockwave", "valiance", "final_strike", "focused_strikes",
                "dispersed_strikes" }) {
            assertEquals(512, EnchantIcons.pngWidth(header("/assets/thaumictinkerer/textures/enchant/" + big + ".png")), big);
        }
        for (String small : new String[] { "protection", "sharpness", "quick_draw", "vamprisim", "ascent_boost", "repair" }) {
            assertEquals(32, EnchantIcons.pngWidth(header("/assets/thaumictinkerer/textures/enchant/" + small + ".png")), small);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void clearForgetsTheSizesSoAResourcePackWithOtherIconsIsReadAgain() throws ReflectiveOperationException {
        java.lang.reflect.Field field = EnchantIcons.class.getDeclaredField("SIZES");
        field.setAccessible(true);
        java.util.Map<ResourceLocation, Integer> sizes = (java.util.Map<ResourceLocation, Integer>) field.get(null);
        sizes.put(ResourceLocation.parse("thaumictinkerer:textures/enchant/pounce.png"), 32);
        sizes.put(ResourceLocation.parse("thaumictinkerer:textures/enchant/missing.png"), 32);
        EnchantIcons.clear();
        assertTrue(sizes.isEmpty(), "the sizes and the fallbacks are gone, the next draw reads the PNG again");
    }

    @Test
    void anythingThatIsNoPngHeaderGivesTheDefaultSize() {
        assertEquals(32, EnchantIcons.pngWidth(new byte[0]));
        assertEquals(32, EnchantIcons.pngWidth(new byte[24]));
        assertFalse(EnchantIcons.pngWidth(new byte[24]) == 0);
        assertTrue(EnchantIcons.SIZE == 16);
    }
}
