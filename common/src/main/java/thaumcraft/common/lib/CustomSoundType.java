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
 * <p>The original prefixed the sound name with {@code thaumcraft:} and let the sound engine resolve the five
 * sounds of the type. 1.21.1 hands the sound events to the sound type directly, so the same names are resolved
 * from the registry here. A missing sound falls back to the vanilla block sound of the same name and finally to
 * the stone sounds, so a sound type never carries a null event.
 */
public class CustomSoundType extends SoundType {
    /** The sound name without the namespace. */
    private final String name;

    public CustomSoundType(String name, float volume, float pitch) {
        super(volume, pitch, sound(name, "break"), sound(name, "step"), sound(name, "place"), sound(name, "hit"), sound(name, "fall"));
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

    private static SoundEvent sound(String name, String kind) {
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", name + "." + kind));
        if (event != null) return event;
        event = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("minecraft", "block." + name + "." + kind));
        if (event != null) return event;
        return switch (kind) {
            case "break" -> SoundEvents.STONE_BREAK;
            case "step" -> SoundEvents.STONE_STEP;
            case "place" -> SoundEvents.STONE_PLACE;
            case "hit" -> SoundEvents.STONE_HIT;
            default -> SoundEvents.STONE_FALL;
        };
    }
}
