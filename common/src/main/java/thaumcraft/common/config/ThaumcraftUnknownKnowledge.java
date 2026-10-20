package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.ItemUnknownResearchNotes;

/** Flattened original notes metadata42 (the nine-fragment recipe) and24 (supported variant; no invented natural source). */
public final class ThaumcraftUnknownKnowledge {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    public static final RegistrySupplier<Item> UNKNOWN_NOTE = ITEMS.register("unknown_research_note", () -> new ItemUnknownResearchNotes(new Item.Properties().rarity(Rarity.RARE), 42));
    public static final RegistrySupplier<Item> UNKNOWN_NOTE_24 = ITEMS.register("unknown_research_note_24", () -> new ItemUnknownResearchNotes(new Item.Properties().rarity(Rarity.RARE), 24));
    private static boolean registered;
    private ThaumcraftUnknownKnowledge() {}
    public static void register() { if (!registered) { registered = true; ITEMS.register(); } }
}
