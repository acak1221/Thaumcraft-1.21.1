package thaumcraft.common.lib;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;

/**
 * Original TC4 {@code thaumcraft.common.lib.CustomSoundType}: a block sound whose sounds live in the
 * {@code thaumcraft} namespace.
 *
 * <p>The original prefixed the sound name with {@code thaumcraft:} and let the sound engine resolve
 * the five sounds of the type. The two classes of the original, {@code thaumcraft.common.lib
 * .CustomSoundType} and {@code thaumcraft.common.blocks.CustomStepSound}, answer the same names for
 * the step sound and for the break sound, and the base sound type gives the same name to the
 * place, the fall and the hit sound as well. One sound name therefore fills all five sounds of the
 * type here too.</p>
 *
 * <p>1.21.1 hands the sound events to the sound type directly, so the name is resolved from the
 * registry and, when the event is not registered, built directly from the identifier. A missing
 * sound falls back to the vanilla block sound of the same name and finally to the stone sounds, so a
 * sound type never carries a null event.</p>
 */
public class CustomSoundType extends SoundType {
    /** The sound name without the namespace. */
    private final String name;

    public CustomSoundType(String name, float volume, float pitch) {
        super(volume, pitch, sound(name), sound(name), sound(name), sound(name), sound(name));
        this.name = name;
    }

    /** The original {@code getSoundType}: the namespaced name of the sound of this type. */
    public String getName() {
        return "thaumcraft:" + this.name;
    }

    /** The sound name without the namespace. */
    public String getSoundName() {
        return this.name;
    }

    private static SoundEvent sound(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("thaumcraft", name);
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(id);
        if (event != null) return event;
        event = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("minecraft", "block." + name));
        if (event != null) return event;
        return SoundEvent.createVariableRangeEvent(id);
    }
}
