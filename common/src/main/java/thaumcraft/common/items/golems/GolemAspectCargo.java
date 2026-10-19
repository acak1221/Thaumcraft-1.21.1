package thaumcraft.common.items.golems;

import java.util.Objects;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.entities.golems.EnumGolemType;

/** Actual single-aspect stock, never golem configuration, a filter or a display jar. */
public record GolemAspectCargo(Aspect aspect, int amount) {
    public static final GolemAspectCargo EMPTY = new GolemAspectCargo(null, 0);
    public static final String PLACER_KEY = "AspectCargo";

    /**
     * Absolute ceiling of the payload: Thaumium (carry 32) with two Earth upgrades = 32 + 16 * 2 = 64.
     * The actual limit of one golem is {@link #capacity}; a test pins this constant to that maximum.
     */
    public static final int MAX_AMOUNT = 64;
    private static final int VERSION = 1;
    private static final Set<String> PAYLOAD_KEYS = Set.of("Version", "Aspect", "Amount");

    public GolemAspectCargo {
        if (amount < 0 || amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("Aspect cargo must be between 0 and " + MAX_AMOUNT + " units: " + amount);
        }
        if (amount == 0 ? aspect != null : aspect == null || aspect.getTag() == null
                || aspect.getTag().isEmpty() || Aspect.getAspect(aspect.getTag()) != aspect) {
            throw new IllegalArgumentException("Aspect cargo requires null for empty or a canonical positive aspect");
        }
    }

    /** Essentia a golem of this material with this many Earth upgrades holds (original getCarryLimit). */
    public static int capacity(int material, int earth) {
        if (!EnumGolemType.isSupported(material)) {
            throw new IllegalArgumentException("Unsupported golem aspect cargo material: " + material);
        }
        return GolemUpgrades.carry(EnumGolemType.getType(material).carry, earth);
    }

    public void validate(int material, int earth) {
        int capacity = capacity(material, earth);
        if (amount > capacity) {
            throw new IllegalArgumentException("Aspect cargo " + amount + " exceeds material " + material
                + " with " + earth + " Earth upgrades capacity " + capacity);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", VERSION);
        tag.putString("Aspect", aspect == null ? "" : aspect.getTag());
        tag.putInt("Amount", amount);
        return tag;
    }

    /** Strict read only; the caller must retain raw data on every rejected payload. */
    public static GolemAspectCargo parse(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag");
        if (!tag.getAllKeys().equals(PAYLOAD_KEYS)
            || !tag.contains("Version", Tag.TAG_INT)
            || !tag.contains("Aspect", Tag.TAG_STRING)
            || !tag.contains("Amount", Tag.TAG_INT)) {
            throw new IllegalArgumentException("Malformed golem aspect cargo payload");
        }
        if (tag.getInt("Version") != VERSION) {
            throw new IllegalArgumentException("Unsupported golem aspect cargo version");
        }
        String name = tag.getString("Aspect");
        Aspect aspect = name.isEmpty() ? null : Aspect.getAspect(name);
        if (!name.isEmpty() && (aspect == null || !name.equals(aspect.getTag()))) {
            throw new IllegalArgumentException("Unknown or noncanonical golem cargo aspect: " + name);
        }
        return new GolemAspectCargo(aspect, tag.getInt("Amount"));
    }

    /** Absence/empty CustomData is empty; unknown top-level or nested data is not. */
    public static GolemAspectCargo readPlacer(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return EMPTY;
        CompoundTag tag = custom.copyTag();
        if (tag.isEmpty()) return EMPTY;
        if (!tag.getAllKeys().equals(Set.of(PLACER_KEY))) {
            throw new IllegalArgumentException("Unsupported golem placer custom-data keys");
        }
        return parseReserved(tag);
    }

    /** Changes only AspectCargo; malformed existing stock is refused before mutation. */
    public void writeToPlacer(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Cannot write aspect cargo to an empty placer stack");
        }
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = custom == null ? new CompoundTag() : custom.copyTag();
        if (tag.contains(PLACER_KEY)) parseReserved(tag);
        if (amount == 0) tag.remove(PLACER_KEY);
        else tag.put(PLACER_KEY, save());
        if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static GolemAspectCargo parseReserved(CompoundTag tag) {
        if (!tag.contains(PLACER_KEY, Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Golem AspectCargo must be a compound");
        }
        return parse(tag.getCompound(PLACER_KEY));
    }
}
