package thaumcraft.common.items.golems;

import java.util.Objects;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.common.entities.golems.EnumGolemType;

/**
 * Owned native fluid stock, separate from golem configuration and ghost exemplars.
 *
 * <p>Oracle {@code thaumcraft/common/entities/golems/EntityGolemBase.java:144,594-595,877-883}: the
 * cargo of the Decanting golem is a {@code FluidStack}, that is a fluid <b>and</b> an amount, and the
 * original stores it in the NBT of the golem and shows the bucket of the fluid with the amount as
 * its metadata. The unit is the <b>millibucket</b> of the original.</p>
 *
 * <p>Callers must retain the original payload when strict parsing or validation fails.</p>
 */
public record GolemFluidCargo(ResourceLocation fluid, int amountMb) {
    public static final int BUCKET = 1000;
    public static final GolemFluidCargo EMPTY = new GolemFluidCargo(null, 0);
    public static final String PLACER_KEY = "FluidCargo";

    /**
     * Absolute ceiling of the payload: the largest capacity any ordinary golem can have,
     * Thaumium (carry 32) with two Earth upgrades = 64 -> floor(sqrt(64)) * 1000. The actual limit of
     * one golem is {@link #capacity}; a test pins this constant to that maximum.
     */
    public static final int MAX_AMOUNT_MB = 8000;
    private static final int VERSION = 1;
    private static final Set<String> PAYLOAD_KEYS = Set.of("Version", "Fluid", "AmountMb");

    public GolemFluidCargo {
        if (amountMb < 0 || amountMb > MAX_AMOUNT_MB) {
            throw new IllegalArgumentException("Fluid cargo must be between 0 and " + MAX_AMOUNT_MB + "mB: " + amountMb);
        }
        if (amountMb == 0 ? fluid != null : fluid == null) {
            throw new IllegalArgumentException("Fluid cargo needs a fluid with an amount and none without");
        }
    }

    /**
     * Fluid a golem of this material with this many Earth upgrades holds (original getFluidCarryLimit,
     * derived from getCarryLimit). Ordinary supported materials only; enum presence alone is not deployment support.
     */
    public static int capacity(int material, int earth) {
        if (!EnumGolemType.isSupported(material)) {
            throw new IllegalArgumentException("Unsupported golem fluid cargo material: " + material);
        }
        return GolemUpgrades.fluidLimitMb(GolemUpgrades.carry(EnumGolemType.getType(material).carry, earth));
    }

    public void validate(int material, int earth) {
        int capacity = capacity(material, earth);
        if (amountMb > capacity) {
            throw new IllegalArgumentException("Fluid cargo " + amountMb + "mB exceeds material " + material
                + " with " + earth + " Earth upgrades capacity " + capacity + "mB");
        }
    }

    /** Returns a fresh exact payload, including explicit zero stock. */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", VERSION);
        tag.putString("Fluid", fluid == null ? "" : fluid.toString());
        tag.putInt("AmountMb", amountMb);
        return tag;
    }

    /** Strict read only: invalid, future or extra data never becomes empty stock. */
    public static GolemFluidCargo parse(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag");
        if (!tag.getAllKeys().equals(PAYLOAD_KEYS)
            || !tag.contains("Version", Tag.TAG_INT)
            || !tag.contains("Fluid", Tag.TAG_STRING)
            || !tag.contains("AmountMb", Tag.TAG_INT)) {
            throw new IllegalArgumentException("Malformed golem fluid cargo payload");
        }
        if (tag.getInt("Version") != VERSION) {
            throw new IllegalArgumentException("Unsupported golem fluid cargo version");
        }
        String name = tag.getString("Fluid");
        int amount = tag.getInt("AmountMb");
        if (name.isEmpty()) {
            if (amount != 0) throw new IllegalArgumentException("A golem fluid cargo without a fluid holds nothing");
            return EMPTY;
        }
        ResourceLocation fluid = ResourceLocation.tryParse(name);
        if (fluid == null) throw new IllegalArgumentException("Unknown golem cargo fluid: " + name);
        return new GolemFluidCargo(fluid, amount);
    }

    /**
     * Reads only a sole FluidCargo custom-data entry. Absent and empty CustomData
     * are equivalent; other keys are refused, not discarded or treated as absence.
     * Item/material/core association is a separate placement responsibility.
     */
    public static GolemFluidCargo readPlacer(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return EMPTY;
        }
        CompoundTag tag = custom.copyTag();
        if (tag.isEmpty()) {
            return EMPTY;
        }
        if (!tag.getAllKeys().equals(Set.of(PLACER_KEY))) {
            throw new IllegalArgumentException("Unsupported golem placer custom-data keys");
        }
        return parseReserved(tag);
    }

    /**
     * Updates only the reserved compound on a real stack, preserving unrelated data.
     * An existing invalid reserved payload is refused before any stack mutation.
     * This serialization helper neither transfers ownership nor authorizes placement.
     */
    public void writeToPlacer(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Cannot write fluid cargo to an empty placer stack");
        }
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = custom == null ? new CompoundTag() : custom.copyTag();
        if (tag.contains(PLACER_KEY)) {
            parseReserved(tag);
        }
        if (amountMb == 0) {
            tag.remove(PLACER_KEY);
        } else {
            tag.put(PLACER_KEY, save());
        }
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    private static GolemFluidCargo parseReserved(CompoundTag tag) {
        if (!tag.contains(PLACER_KEY, Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Golem FluidCargo must be a compound");
        }
        return parse(tag.getCompound(PLACER_KEY));
    }
}
