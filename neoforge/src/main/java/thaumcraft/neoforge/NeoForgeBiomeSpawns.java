package thaumcraft.neoforge;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumcraft.common.Thaumcraft;

/**
 * NeoForge owner of the original TC4 entity spawn rules.
 *
 * <p>Fabric installs them through a {@code BiomeModifications} entry of the shared bootstrap. The
 * NeoForge equivalent is the datapack biome modifier system, so the type is registered in the
 * NeoForge biome modifier serializer registry and activated by
 * {@code data/thaumcraft/neoforge/biome_modifier/original_entity_spawns.json}.</p>
 */
final class NeoForgeBiomeSpawns {
    private NeoForgeBiomeSpawns() {}

    static void register(IEventBus bus) {
        DeferredRegister<MapCodec<? extends BiomeModifier>> types =
                DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Thaumcraft.MODID);
        types.register("original_entity_spawns", () -> OriginalEntitySpawnsBiomeModifier.CODEC);
        types.register(bus);
    }
}
