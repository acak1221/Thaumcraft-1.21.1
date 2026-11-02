package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftWearables;

/** Real vanilla smithing recipes/menu, finite payments and persistent native trim components. */
public final class ArmorTrimRuntimeChecks {
    private ArmorTrimRuntimeChecks() {}

    public static void smithingKeepsEquipmentData(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3)).atY(160);
        var old = level.getBlockState(pos);
        test.assertTrue(level.getBlockEntity(pos) == null, "the owned smithing fixture has no foreign block entity");
        var chunk = new ChunkPos(pos);
        boolean ownedForce = !level.getForcedChunks().contains(chunk.toLong());
        if (ownedForce) level.setChunkForced(chunk.x, chunk.z, true);
        level.getChunkAt(pos); GameTestChunkAwait.entityTicking(test, pos);
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        var profile = new GameProfile(UUID.randomUUID(), "trim-" + UUID.randomUUID().toString().substring(0, 8));
        var player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        boolean joined = false;
        try {
            level.setBlock(pos, Blocks.SMITHING_TABLE.defaultBlockState(), Block.UPDATE_ALL);
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false)); joined = true;
            player.setGameMode(GameType.SURVIVAL); player.setInvulnerable(true); player.setNoGravity(true);
            player.teleportTo(level, pos.getX() + .5, pos.getY() + 1, pos.getZ() + 1.5, 0, 0);
            int checked = 0;
            for (Item item : trimmableEquipment()) {
                ItemStack base = new ItemStack(item);
                test.assertTrue(base.is(ItemTags.TRIMMABLE_ARMOR), "registered armour is admitted by the actual native smithing tag: " + item);
                base.set(DataComponents.CUSTOM_NAME, Component.literal("preserved trim fixture " + checked));
                base.setDamageValue(7);
                base.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.UNBREAKING), 1);
                if (item instanceof thaumcraft.common.items.armor.DyedRobe) {
                    base.set(DataComponents.DYED_COLOR, new DyedItemColor(0x335599, true));
                }
                if (item instanceof thaumcraft.common.items.armor.ItemFortressArmor) {
                    base.set(ThaumcraftEquipment.FORTRESS_GOGGLES.get(), true);
                    base.set(ThaumcraftEquipment.FORTRESS_MASK.get(), 1);
                }
                var menu = new SmithingMenu(30 + checked, player.getInventory(), ContainerLevelAccess.create(level, pos));
                player.containerMenu = menu;
                menu.getSlot(0).set(new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, 2));
                menu.getSlot(1).set(base.copy());
                menu.getSlot(2).set(new ItemStack(Items.DIAMOND, 2));
                menu.createResult();
                ItemStack preview = menu.getSlot(3).getItem().copy();
                test.assertTrue(!preview.isEmpty() && preview.is(item), "loaded vanilla smithing recipe produces the same Thaumcraft armour item");
                var expectedPattern = level.registryAccess().registryOrThrow(Registries.TRIM_PATTERN).getHolderOrThrow(TrimPatterns.SENTRY);
                var expectedMaterial = level.registryAccess().registryOrThrow(Registries.TRIM_MATERIAL).getHolderOrThrow(TrimMaterials.DIAMOND);
                var trim = preview.get(DataComponents.TRIM);
                test.assertTrue(trim != null && trim.hasPatternAndMaterial(expectedPattern, expectedMaterial), "the result contains the selected native pattern and material");
                ItemStack otherwise = preview.copy(); otherwise.remove(DataComponents.TRIM);
                test.assertTrue(ItemStack.matches(base, otherwise), "damage, name, enchantments, robe dye and fortress mask/goggles remain exactly unchanged");
                menu.clicked(3, 0, ClickType.PICKUP, player);
                ItemStack paid = menu.getCarried();
                test.assertTrue(ItemStack.matches(preview, paid) && menu.getSlot(0).getItem().getCount() == 1
                                && menu.getSlot(1).getItem().isEmpty() && menu.getSlot(2).getItem().getCount() == 1,
                        "the actual survival result click spends one template, one armour and one material");
                ItemStack loaded = ItemStack.parse(level.registryAccess(), paid.save(level.registryAccess())).orElseThrow();
                test.assertTrue(ItemStack.matches(paid, loaded), "native save/load retains the complete trimmed item and all other components");
                menu.setCarried(ItemStack.EMPTY);
                menu.getSlot(1).set(loaded);
                menu.createResult();
                test.assertTrue(menu.getSlot(3).getItem().isEmpty(), "an identical existing native trim has no payable duplicate result");
                menu.clicked(3, 0, ClickType.PICKUP, player);
                test.assertTrue(menu.getCarried().isEmpty() && menu.getSlot(0).getItem().getCount() == 1
                                && menu.getSlot(2).getItem().getCount() == 1 && ItemStack.matches(menu.getSlot(1).getItem(), loaded),
                        "refused duplicate trim consumes no input and grants no item");
                // Close the real menu so vanilla returns the finite remaining physical inputs.
                player.closeContainer();
                player.getInventory().clearContent();
                checked++;
            }
            test.assertTrue(checked == 17, "all thaumium, void, fortress, thaumaturge robe and void robe pieces were tested");
            var refused = new SmithingMenu(60, player.getInventory(), ContainerLevelAccess.create(level, pos));
            player.containerMenu = refused;
            refused.getSlot(0).set(new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE));
            refused.getSlot(1).set(new ItemStack(Items.STICK));
            refused.getSlot(2).set(new ItemStack(Items.DIAMOND));
            refused.createResult(); refused.clicked(3, 0, ClickType.PICKUP, player);
            test.assertTrue(refused.getSlot(3).getItem().isEmpty() && refused.getCarried().isEmpty()
                            && refused.getSlot(0).getItem().getCount() == 1 && refused.getSlot(1).getItem().is(Items.STICK)
                            && refused.getSlot(2).getItem().getCount() == 1,
                    "a non-armour item never becomes a trim result or consumes smithing payment");
        } finally {
            if (joined) {
                player.closeContainer();
                level.getServer().getPlayerList().remove(player);
            }
            channel.finishAndReleaseAll();
            level.setBlock(pos, old, Block.UPDATE_ALL);
            if (ownedForce) level.setChunkForced(chunk.x, chunk.z, false);
        }
        test.succeed();
    }

    private static List<Item> trimmableEquipment() {
        return List.of(ThaumcraftEquipment.THAUMIUM_HELMET.get(), ThaumcraftEquipment.THAUMIUM_CHESTPLATE.get(),
                ThaumcraftEquipment.THAUMIUM_LEGGINGS.get(), ThaumcraftEquipment.THAUMIUM_BOOTS.get(),
                ThaumcraftEquipment.VOID_HELMET.get(), ThaumcraftEquipment.VOID_CHESTPLATE.get(),
                ThaumcraftEquipment.VOID_LEGGINGS.get(), ThaumcraftEquipment.VOID_BOOTS.get(),
                ThaumcraftEquipment.FORTRESS_HELMET.get(), ThaumcraftEquipment.FORTRESS_CHESTPLATE.get(),
                ThaumcraftEquipment.FORTRESS_LEGGINGS.get(),
                ThaumcraftWearables.ROBE_CHESTPLATE.get(), ThaumcraftWearables.ROBE_LEGGINGS.get(),
                ThaumcraftWearables.ROBE_BOOTS.get(), ThaumcraftEquipment.VOID_ROBE_HELMET.get(),
                ThaumcraftEquipment.VOID_ROBE_CHESTPLATE.get(), ThaumcraftEquipment.VOID_ROBE_LEGGINGS.get());
    }
}
