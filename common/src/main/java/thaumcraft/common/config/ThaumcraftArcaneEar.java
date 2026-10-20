package thaumcraft.common.config;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
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
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockArcaneEar;
import thaumcraft.common.lib.ArcaneEarNotes;
import thaumcraft.common.tiles.TileSensor;

/** Original blockWoodenDevice:1, flattened to its own id. */
public final class ThaumcraftArcaneEar {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    public static final RegistrySupplier<BlockArcaneEar> ARCANE_EAR = BLOCKS.register("arcane_ear", () ->
            new BlockArcaneEar(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS)
                    .strength(2.5F, 6.0F).sound(SoundType.WOOD)));
    public static final RegistrySupplier<Item> ARCANE_EAR_ITEM = ITEMS.register("arcane_ear", () -> new BlockItem(ARCANE_EAR.get(), new Item.Properties()));
    public static final RegistrySupplier<BlockEntityType<TileSensor>> ARCANE_EAR_TILE = TILES.register("arcane_ear", () ->
            BlockEntityType.Builder.of(TileSensor::new, ARCANE_EAR.get()).build(null));
    private static boolean registered;
    private ThaumcraftArcaneEar() {}
    public static void register() {
        if (registered) return;
        registered = true;
        BLOCKS.register(); ITEMS.register(); TILES.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, ARCANE_EAR_ITEM);
        TickEvent.SERVER_LEVEL_POST.register(ArcaneEarNotes::clear);
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(ArcaneEarNotes::clear);
    }
}
