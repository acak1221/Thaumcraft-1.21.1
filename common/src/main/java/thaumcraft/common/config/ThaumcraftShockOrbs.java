package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockShockTrap;
import thaumcraft.common.entities.projectile.EntityShockOrb;

public final class ThaumcraftShockOrbs {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Thaumcraft.MODID,Registries.ENTITY_TYPE);
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Thaumcraft.MODID,Registries.BLOCK);
    public static final RegistrySupplier<EntityType<EntityShockOrb>> SHOCK_ORB=ENTITIES.register("shock_orb",()->EntityType.Builder.<EntityShockOrb>of(EntityShockOrb::new,MobCategory.MISC)
            .sized(.25F,.25F).clientTrackingRange(4).updateInterval(20).build("thaumcraft:shock_orb"));
    public static final RegistrySupplier<BlockShockTrap> SHOCK_TRAP=BLOCKS.register("shock_trap",()->new BlockShockTrap(BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
            .strength(100,50).lightLevel(state->8).replaceable().noCollission().noOcclusion().randomTicks().noLootTable().pushReaction(PushReaction.DESTROY)));
    private static boolean initialized;
    private ThaumcraftShockOrbs() {}
    public static void register() { if(initialized)return;initialized=true;BLOCKS.register();ENTITIES.register(); }
}
