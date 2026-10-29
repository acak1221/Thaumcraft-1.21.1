/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.content;

import com.mojang.serialization.MapCodec;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import thaumcraft.api.ThaumcraftApi;
import thaumic.tinkerer.client.KamiWorldClient;
import thaumic.tinkerer.common.block.kami.BlockBedrockPortal;
import thaumic.tinkerer.common.block.kami.BlockWarpGate;
import thaumic.tinkerer.common.block.tile.container.kami.ContainerWarpGate;
import thaumic.tinkerer.common.block.tile.kami.TileWarpGate;
import thaumic.tinkerer.common.core.commands.KamiUnlockedCommand;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.core.handler.kami.DimensionalShardDropHandler;
import thaumic.tinkerer.common.core.handler.kami.SoulHeartHandler;
import thaumic.tinkerer.common.dim.ChunkGeneratorBedrock;
import thaumic.tinkerer.common.dim.WorldProviderBedrock;
import thaumic.tinkerer.common.item.kami.ItemBlockWarpGate;
import thaumic.tinkerer.common.item.kami.KamiHooks;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.network.packet.kami.PacketSoulHearts;
import thaumic.tinkerer.common.network.packet.kami.PacketWarpGateButton;
import thaumic.tinkerer.common.network.packet.kami.PacketWarpGateTeleport;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Feature group: the Kami world: the Bedrock dimension and its portal, the Celestial Gateway, the Soul Hearts, the
 * dimensional shard drops and {@code /iskamiunlocked}.
 *
 * <p>Gated by {@code ConfigHandler.enableKami}: with the option off nothing of the group is registered, with one exception,
 * the chunk generator type {@code thaumictinkerer:bedrock}. The Bedrock dimension is a file of the data pack
 * ({@code data/thaumictinkerer/dimension/bedrock.json}) that names that type; a data pack that cannot decode its
 * dimensions stops the world from loading, so the type must exist even when no portal leads there.</p>
 *
 * <p>The Kami gear group reaches this one through registry paths ({@code thaumictinkerer:bedrock}, the block
 * {@code bedrock_portal}, the block {@code warp_gate}) and the two hooks of {@link KamiHooks} that this registrar
 * connects: the Soul Hearts of the Awakened Sword and the teleport of the Focus of Celestial Recall.</p>
 */
public final class KamiWorld implements ContentGroup {
    public static final KamiWorld INSTANCE = new KamiWorld();

    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister
            .create(LibMisc.MOD_ID, Registries.CHUNK_GENERATOR);

    private static RegistrySupplier<BlockWarpGate> warpGate;
    private static RegistrySupplier<BlockEntityType<TileWarpGate>> warpGateType;
    private static RegistrySupplier<MenuType<ContainerWarpGate>> warpGateMenu;
    private static RegistrySupplier<BlockBedrockPortal> portal;

    private KamiWorld() {}

    @Override
    public void register() {
        // The generator type of the dimension file: always (see the class comment).
        CHUNK_GENERATORS.register("bedrock", () -> ChunkGeneratorBedrock.CODEC);
        CHUNK_GENERATORS.register();

        if (!ConfigHandler.enableKami) return;

        warpGate = TTRegistry.block(LibBlockNames.WARP_GATE, BlockWarpGate::new,
                block -> new ItemBlockWarpGate(block, new Item.Properties()));
        warpGateType = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.WARP_GATE,
                () -> BlockEntityType.Builder.of(TileWarpGate::new, warpGate.get()).build(null));
        warpGateMenu = TTRegistry.MENUS.register(LibBlockNames.WARP_GATE,
                () -> MenuRegistry.ofExtended(ContainerWarpGate::fromNetwork));
        // The portal has no item: the pickaxe makes it from bedrock.
        portal = TTRegistry.blockWithoutItem(LibBlockNames.PORTAL, BlockBedrockPortal::new);

        PacketWarpGateButton.register();
        PacketWarpGateTeleport.register();
        // The payload the server sends is known on both sides; the client's receiver is registered by registerClient().
        NetworkManager.registerS2CPayloadType(PacketSoulHearts.TYPE, PacketSoulHearts.CODEC);

        SoulHeartHandler.register();
        DimensionalShardDropHandler.register();
        KamiUnlockedCommand.register();

        // The contracts of the Kami gear group (KamiHooks): Soul Mode of the sword, Focus of Celestial Recall.
        KamiHooks.soulHeartSink = SoulHeartHandler::addHearts;
        KamiHooks.warpGateTeleporter = TileWarpGate::teleportPlayer;
    }

    @Override
    public void ready() {
        if (!ConfigHandler.enableKami) return;
        // Thaumcraft's ores, aura nodes and vegetation stay out of the dimension (they never reached 1.7.10's either).
        ThaumcraftApi.addDimBlacklist(WorldProviderBedrock.KEY, 2);
    }

    @Override
    public void registerClient() {
        if (!ConfigHandler.enableKami) return;
        KamiWorldClient.register();
    }

    // ---- registered objects --------------------------------------------------------------------------------------

    public static BlockWarpGate warpGateBlock() {
        return warpGate.get();
    }

    public static BlockEntityType<TileWarpGate> warpGateType() {
        return warpGateType.get();
    }

    public static MenuType<ContainerWarpGate> warpGateMenu() {
        return warpGateMenu.get();
    }

    public static BlockBedrockPortal portalBlock() {
        return portal.get();
    }

    /** The blocks of the group, for the tests and the resource checks. */
    public static List<Block> blocks() {
        return List.of(warpGateBlock(), portalBlock());
    }
}
