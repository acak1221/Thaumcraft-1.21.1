package forbiddenmagic.common.items;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import forbiddenmagic.common.ForbiddenMagic;

/**
 * Original ForbiddenItems.addItems() (fox.spiteful.forbidden.items.ForbiddenItems), checkpoint-1
 * slice only: the "NetherShard" family, the standalone GluttonyShard and the two base-game
 * FMResource subtypes. See docs/addons/forbidden-magic/STATUS.md for the rest of the original's
 * ~40 items, which are not ported yet.
 *
 * <h2>Metadata flattening (documented deviation)</h2>
 * The original's single {@code ItemDeadlyShard} (one Item, 7 damage values: wrath/envy/taint/
 * pride/lust-or-despair/sloth/greed) and 5-value {@code ItemResource} are split into one item
 * per original damage value, per modern Minecraft's item registry (no metadata subtypes). The
 * original's slot 4 is ONE item whose name/aspects flip with {@code Config.noLust}; here both
 * {@code lust_shard} and {@code despair_shard} are always registered as distinct items (the
 * sibling forbidden-magic-reforged port resolves the same modern-flattening problem the same
 * way: "registers an alternative aspect/item view for the corresponding shard"). Which one is
 * canonical is {@link forbiddenmagic.common.config.ForbiddenMagicConfig#noLust}; recipes/research
 * gating on it are not ported yet, so right now both items simply exist and both carry their
 * original aspect tags unconditionally (see {@link ForbiddenMagicAspectTags}).
 *
 * <p>{@code FMResource} subtypes 2-4 (Manasteel/Tainted Blood/Elementium nuggets) are Botania/
 * Blood Magic compat items; skipped, matching the sibling port's own "external mods" scope cut.</p>
 */
public final class ForbiddenMagicItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForbiddenMagic.MODID, Registries.ITEM);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(ForbiddenMagic.MODID, Registries.CREATIVE_MODE_TAB);

    // "NetherShard" (original fox.spiteful.forbidden.items.ItemDeadlyShard), one item per original damage value.
    public static final RegistrySupplier<Item> WRATH_SHARD = ITEMS.register("wrath_shard", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistrySupplier<Item> ENVY_SHARD = ITEMS.register("envy_shard", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistrySupplier<Item> TAINT_SHARD = ITEMS.register("taint_shard", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistrySupplier<Item> PRIDE_SHARD = ITEMS.register("pride_shard", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistrySupplier<Item> LUST_SHARD = ITEMS.register("lust_shard", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistrySupplier<Item> DESPAIR_SHARD = ITEMS.register("despair_shard", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistrySupplier<Item> SLOTH_SHARD = ITEMS.register("sloth_shard", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistrySupplier<Item> GREED_SHARD = ITEMS.register("greed_shard", () -> new Item(new Item.Properties().stacksTo(64)));

    // Original fox.spiteful.forbidden.items.ItemGluttonyShard: ItemFood(2, 0.1F, false).
    private static final FoodProperties GLUTTONY_FOOD = new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build();
    public static final RegistrySupplier<Item> GLUTTONY_SHARD = ITEMS.register("gluttony_shard",
            () -> new Item(new Item.Properties().food(GLUTTONY_FOOD)));

    // Original fox.spiteful.forbidden.items.ItemResource subtypes 0 and 1 only (see class doc).
    public static final RegistrySupplier<Item> EMERALD_FRAGMENT = ITEMS.register("emerald_fragment",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistrySupplier<Item> SHADOW_INK = ITEMS.register("shadow_ink",
            () -> new ItemShadowInk(new Item.Properties().stacksTo(64)));

    // Original Forbidden.tab's icon is ForbiddenItems.fork (Diabolist Fork), not ported yet;
    // using the Wrath Shard as a placeholder icon until the fork lands.
    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("forbiddenmagic",
            () -> CreativeTabRegistry.create(Component.translatable("itemGroup.forbidden"), () -> new ItemStack(WRATH_SHARD.get())));

    private static boolean registered;

    private ForbiddenMagicItems() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ITEMS.register();
        TABS.register();
        CreativeTabRegistry.append(TAB, WRATH_SHARD, ENVY_SHARD, TAINT_SHARD, PRIDE_SHARD, LUST_SHARD, DESPAIR_SHARD,
                SLOTH_SHARD, GREED_SHARD, GLUTTONY_SHARD, EMERALD_FRAGMENT, SHADOW_INK);
    }
}
