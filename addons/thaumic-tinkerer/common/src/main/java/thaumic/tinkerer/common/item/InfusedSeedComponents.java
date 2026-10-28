/**
 * This class is new in the port of Thaumic Tinkerer to Minecraft 1.21.1 for Thaumcraft 4 Reload (no 1.7.10
 * counterpart); it is distributed under the same licence as the add-on (Creative Commons
 * Attribution-NonCommercial-ShareAlike 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * The data components of the infused seeds. 1.7.10 kept the aspect of the seeds and its tendencies in the NBT of the
 * stack ({@code mainAspect} and {@code aspectTendencies}, both saved as an aspect list); here the aspect is the tag of
 * the aspect and the tendencies are a map from tag to amount. The Thaumcraft API has no codec for aspects yet
 * (docs/api-requests/SimpleBlocks.md).
 */
public final class InfusedSeedComponents {
    public static final String ASPECT_PATH = "infused_aspect";
    public static final String TENDENCIES_PATH = "infused_tendencies";

    private static RegistrySupplier<DataComponentType<?>> aspect;
    private static RegistrySupplier<DataComponentType<?>> tendencies;

    private InfusedSeedComponents() {}

    /** Registers the two types with the add-on's deferred register. Call once, from the {@code register()} phase. */
    public static void register() {
        if (aspect != null) return;
        aspect = TTRegistry.DATA_COMPONENT_TYPES.register(ASPECT_PATH, () -> DataComponentType.<String>builder()
                .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).cacheEncoding().build());
        StreamCodec<RegistryFriendlyByteBuf, Map<String, Integer>> network = ByteBufCodecs.map(LinkedHashMap::new,
                ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT);
        tendencies = TTRegistry.DATA_COMPONENT_TYPES.register(TENDENCIES_PATH, () -> DataComponentType.<Map<String, Integer>>builder()
                .persistent(Codec.unboundedMap(Codec.STRING, Codec.INT)).networkSynchronized(network).cacheEncoding().build());
    }

    @SuppressWarnings("unchecked")
    public static DataComponentType<String> aspectType() {
        return (DataComponentType<String>) aspect.get();
    }

    @SuppressWarnings("unchecked")
    public static DataComponentType<Map<String, Integer>> tendenciesType() {
        return (DataComponentType<Map<String, Integer>>) tendencies.get();
    }

    /** An aspect list as the map of the component (the order is kept; amounts of zero are kept, as in 1.7.10). */
    public static Map<String, Integer> toMap(AspectList list) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Aspect aspect : list.getAspects()) {
            if (aspect != null) map.put(aspect.getTag(), list.getAmount(aspect));
        }
        return map;
    }

    /** The aspect list of a component map; tags of unknown aspects are dropped. */
    public static AspectList toList(Map<String, Integer> map) {
        AspectList list = new AspectList();
        if (map == null) return list;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            Aspect aspect = Aspect.getAspect(entry.getKey());
            if (aspect != null) list.add(aspect, entry.getValue());
        }
        return list;
    }
}
