package thaumcraft.common.config;

import dev.architectury.registry.fuel.FuelRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockNitor;
import thaumcraft.common.entities.projectile.EntityAlumentum;
import thaumcraft.common.tiles.TileNitor;

/** Functional resource outputs; the two ITEM ids remain owned by MATERIALS. */
public final class ThaumcraftResources {
    // TC4 EventHandlerWorld.getBurnTime:139-146, not the later-version value.
    public static final int ALUMENTUM_BURN_TIME = 6400;
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<BlockNitor> NITOR = BLOCKS.register("nitor", () -> new BlockNitor(
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0, 0)
                    .sound(new SoundType(0, 1, SoundEvents.WOOL_BREAK, SoundEvents.WOOL_STEP,
                            SoundEvents.WOOL_PLACE, SoundEvents.WOOL_HIT, SoundEvents.WOOL_FALL))
                    .noCollission().noOcclusion().forceSolidOff().noTerrainParticles()
                    .lightLevel(state -> 15).pushReaction(PushReaction.DESTROY)));
    public static final RegistrySupplier<BlockEntityType<TileNitor>> NITOR_BLOCK_ENTITY = BLOCK_ENTITIES.register("nitor",
            () -> BlockEntityType.Builder.of(TileNitor::new, NITOR.get()).build(null));
    public static final RegistrySupplier<EntityType<EntityAlumentum>> ALUMENTUM = ENTITIES.register("alumentum",
            () -> EntityType.Builder.<EntityAlumentum>of(EntityAlumentum::new, MobCategory.MISC)
                    .sized(.25F, .25F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:alumentum"));

    private static boolean registered;
    private ThaumcraftResources() {}

    public static void register() {
        if (registered) return;
        registered = true;
        BLOCKS.register();
        BLOCK_ENTITIES.register();
        ENTITIES.register();
        // listen is safe both before and after the loader's item registry event.
        // Neither resource item constructor resolves a deferred block/entity.
        ThaumcraftContent.MATERIALS.get("alumentum").listen(item -> {
            FuelRegistry.register(ALUMENTUM_BURN_TIME, item);
            DispenserBlock.registerProjectileBehavior(item);
        });
    }
}
