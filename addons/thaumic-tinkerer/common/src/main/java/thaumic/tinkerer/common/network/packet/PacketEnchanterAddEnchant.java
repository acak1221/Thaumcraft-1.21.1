/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [16 Sep 2013, 15:45:54 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.network.packet;

import dev.architectury.networking.NetworkManager;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import thaumic.tinkerer.common.block.tile.TileEnchanter;
import thaumic.tinkerer.common.block.tile.container.ContainerEnchanter;
import thaumic.tinkerer.common.enchantment.ModEnchantments;
import thaumic.tinkerer.common.enchantment.core.EnchantmentManager;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * Client to server: the player chose an enchantment of the Osmotic Enchanter, or changed its level. The level is 0 for
 * "add", -1 for "remove" and the new level otherwise (1.7.10 {@code PacketEnchanterAddEnchant}, which sent the numeric id of
 * the enchantment; the port sends its registry id).
 */
public record PacketEnchanterAddEnchant(BlockPos pos, ResourceLocation enchant, int level) implements CustomPacketPayload {
    public static final Type<PacketEnchanterAddEnchant> TYPE = new Type<>(LibMisc.id("enchanter_add_enchant"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketEnchanterAddEnchant> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketEnchanterAddEnchant::pos,
            ResourceLocation.STREAM_CODEC, PacketEnchanterAddEnchant::enchant,
            ByteBufCodecs.VAR_INT, PacketEnchanterAddEnchant::level,
            PacketEnchanterAddEnchant::new);

    public PacketEnchanterAddEnchant(TileEnchanter tile, ResourceKey<Enchantment> enchant, int level) {
        this(tile.getBlockPos(), enchant.location(), level);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Registers the receiver (both sides, from the mod initialisation). */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TYPE, STREAM_CODEC, (payload, context) -> context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer sender) payload.apply(sender);
        }));
    }

    /**
     * {@code onMessage} on the server. Returns whether the enchanter changed. The enchantment has to be one the
     * Enchanter knows (the original would fail on any other), and a tool that is enchanted already takes none.
     */
    public boolean apply(ServerPlayer sender) {
        TileEnchanter tile = PacketTile.resolve(sender, pos, TileEnchanter.class, ContainerEnchanter.class, menu -> menu.enchanter);
        if (tile == null || tile.working) return false;

        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, enchant);
        if (!EnchantmentManager.isKnown(key)) return false;

        if (level == -1) {
            int index = tile.enchantments.indexOf(key);
            if (index != -1) {
                tile.removeLevel(index);
                tile.removeEnchant(index);
            }
        } else {
            if (!tile.enchantments.contains(key)) {
                ItemStack tool = tile.getItem(TileEnchanter.SLOT_TOOL);
                if (!tool.isEnchanted()
                        && EnchantmentManager.canApply(sender.level().registryAccess(), tool, key, tile.enchantments)
                        && EnchantmentManager.canEnchantmentBeUsed(sender, key)) {
                    tile.appendEnchant(key);
                    tile.appendLevel(1);
                }
            } else {
                Optional<Holder.Reference<Enchantment>> holder = ModEnchantments.holder(sender.level().registryAccess(), key);
                int maxLevel = holder.map(reference -> reference.value().getMaxLevel()).orElse(1);
                tile.setLevel(tile.enchantments.indexOf(key), Math.max(1, Math.min(maxLevel, level)));
            }
        }
        tile.updateAspectList();
        tile.sync();
        return true;
    }
}
