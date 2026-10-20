package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockMagicWorkbenchCharger;
import thaumcraft.common.tiles.TileMagicWorkbenchCharger;

public final class ThaumcraftVisCharging {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    public static final RegistrySupplier<BlockMagicWorkbenchCharger> VIS_CHARGE_RELAY = BLOCKS.register("vis_charge_relay", () -> new BlockMagicWorkbenchCharger(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3, 10.2F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<Item> VIS_CHARGE_RELAY_ITEM = ITEMS.register("vis_charge_relay", () -> new BlockItem(VIS_CHARGE_RELAY.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileMagicWorkbenchCharger>> VIS_CHARGE_RELAY_TILE = TILES.register("vis_charge_relay", () ->
            BlockEntityType.Builder.of(TileMagicWorkbenchCharger::new, VIS_CHARGE_RELAY.get()).build(null));
    private static boolean registered;
    private ThaumcraftVisCharging() {}
    public static void register() {
        if (registered) return;
        registered = true;
        BLOCKS.register(); ITEMS.register(); TILES.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, VIS_CHARGE_RELAY_ITEM);
    }
}
