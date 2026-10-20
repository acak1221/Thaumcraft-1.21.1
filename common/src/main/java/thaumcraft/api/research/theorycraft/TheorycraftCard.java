package thaumcraft.api.research.theorycraft;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * One theorycraft card of the research table.
 *
 * <p>Forced 1.21.1 changes: {@code EntityPlayer} became {@link Player} and the item NBT became the native
 * {@link CompoundTag}.</p>
 */
public abstract class TheorycraftCard {
    private long seed;

    public long getSeed() { return seed; }

    /** Called once when the card is drawn; false refuses the draw. */
    public boolean initialize(Player player, ResearchTableData data) { return true; }

    /** An aid-only card never appears in the draw pile. */
    public boolean isAidOnly() { return false; }

    public abstract int getInspirationCost();

    /** The research category this card belongs to, or null. */
    public String getResearchCategory() { return null; }

    public abstract String getLocalizedName();

    public abstract String getLocalizedText();

    /** The items this card needs, or null when it needs none. */
    public ItemStack[] getRequiredItems() { return null; }

    /** Which of the required items are consumed, or null when it does not apply. */
    public boolean[] getRequiredItemsConsumed() { return null; }

    /** Activates the card; false refuses the activation. */
    public abstract boolean activate(Player player, ResearchTableData data);

    public void setSeed(long seed) { this.seed = seed; }

    /** The saved state of the card. */
    public CompoundTag serialize() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("seed", seed);
        return nbt;
    }

    public void deserialize(CompoundTag nbt) {
        seed = nbt.getLong("seed");
    }

    /** The translated name of the card. */
    public String name() { return Component.translatable(getLocalizedName()).getString(); }

    /** The translated text of the card. */
    public String text() { return Component.translatable(getLocalizedText()).getString(); }
}
