/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.registry;

import com.google.common.base.Suppliers;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * The registries of the add-on and the single place that knows its registered objects.
 *
 * <p>1.7.10 kept every block and item in a class-keyed map ({@code getFirstItemFromClass}) and registered the lot in
 * {@code preInit}. Here every feature group of {@code thaumic.tinkerer.common.content} adds its objects to the
 * {@link DeferredRegister}s below from its {@code register()} method, and objects of another group are reached by
 * their registry path ({@link #item(String)}, {@link #block(String)}, {@link #stack(String)}), never through the
 * class of the other group, so the groups do not depend on one another at compile time.</p>
 */
public final class TTRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(LibMisc.MOD_ID, Registries.ITEM);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(LibMisc.MOD_ID, Registries.BLOCK);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(LibMisc.MOD_ID,
            Registries.BLOCK_ENTITY_TYPE);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(LibMisc.MOD_ID, Registries.MENU);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(LibMisc.MOD_ID,
            Registries.ENTITY_TYPE);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(LibMisc.MOD_ID,
            Registries.MOB_EFFECT);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(LibMisc.MOD_ID,
            Registries.SOUND_EVENT);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister
            .create(LibMisc.MOD_ID, Registries.DATA_COMPONENT_TYPE);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(LibMisc.MOD_ID,
            Registries.ARMOR_MATERIAL);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(LibMisc.MOD_ID,
            Registries.CREATIVE_MODE_TAB);

    private static final List<ITTinkererRegisterable> REGISTERABLES = new ArrayList<>();
    private static final List<TabEntry> TAB_ENTRIES = new ArrayList<>();
    private static final List<Supplier<ItemStack>> TAB_STACKS = new ArrayList<>();

    /** The creative tab of the add-on: every item and block registered through the helpers, then extra stacks. */
    public static final RegistrySupplier<CreativeModeTab> CREATIVE_TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable(LibMisc.CREATIVE_TAB_KEY))
                    .icon(TTRegistry::tabIcon)
                    .displayItems((parameters, output) -> {
                        for (TabEntry entry : TAB_ENTRIES) {
                            if (entry.shown()) output.accept(entry.item().asItem());
                        }
                        for (Supplier<ItemStack> stack : TAB_STACKS) {
                            ItemStack value = stack.get();
                            if (value != null && !value.isEmpty()) output.accept(value);
                        }
                    }).build());

    private record TabEntry(Supplier<? extends ItemLike> itemSupplier, Supplier<Object> owner) {
        ItemLike item() {
            return itemSupplier.get();
        }

        boolean shown() {
            return !(owner.get() instanceof ITTinkererRegisterable r) || r.shouldDisplayInTab();
        }
    }

    private TTRegistry() {}

    // ---- registration helpers ------------------------------------------------------------------------------

    /** Registers an item and puts it in the creative tab. */
    public static <T extends Item> RegistrySupplier<T> item(String path, Supplier<T> factory) {
        RegistrySupplier<T> supplier = ITEMS.register(path, () -> track(factory.get()));
        TAB_ENTRIES.add(new TabEntry(supplier, supplier::get));
        return supplier;
    }

    /** Registers an item that the creative tab does not list. */
    public static <T extends Item> RegistrySupplier<T> hiddenItem(String path, Supplier<T> factory) {
        return ITEMS.register(path, () -> track(factory.get()));
    }

    /** Registers a block with a plain {@link BlockItem} of the same path, listed in the creative tab. */
    public static <T extends Block> RegistrySupplier<T> block(String path, Supplier<T> factory) {
        return block(path, factory, block -> new BlockItem(block, new Item.Properties()));
    }

    /** Registers a block with the given item (null for none), listed in the creative tab when it has one. */
    public static <T extends Block> RegistrySupplier<T> block(String path, Supplier<T> factory,
            Function<? super T, ? extends Item> itemFactory) {
        RegistrySupplier<T> supplier = BLOCKS.register(path, () -> track(factory.get()));
        if (itemFactory != null) {
            RegistrySupplier<Item> item = ITEMS.register(path, () -> itemFactory.apply(supplier.get()));
            TAB_ENTRIES.add(new TabEntry(item, supplier::get));
        }
        return supplier;
    }

    /** Registers a block that has no item (fire, portals, gases, crops...). */
    public static <T extends Block> RegistrySupplier<T> blockWithoutItem(String path, Supplier<T> factory) {
        return block(path, factory, null);
    }

    /** Adds a stack the creative tab lists after the items (a wand with vis, a full-charge variant...). */
    public static void addTabStack(Supplier<ItemStack> stack) {
        TAB_STACKS.add(stack);
    }

    private static <T> T track(T object) {
        if (object instanceof ITTinkererRegisterable registerable) REGISTERABLES.add(registerable);
        return object;
    }

    /** The objects that bring recipe and research, in registration order. */
    public static List<ITTinkererRegisterable> registerables() {
        return Collections.unmodifiableList(REGISTERABLES);
    }

    /** Hands every deferred registry to the loader. Call once, after every group added its objects. */
    public static void registerAll() {
        SOUND_EVENTS.register();
        DATA_COMPONENT_TYPES.register();
        MOB_EFFECTS.register();
        ARMOR_MATERIALS.register();
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        MENUS.register();
        ENTITY_TYPES.register();
        CREATIVE_TABS.register();
    }

    // ---- lookup by registry path ---------------------------------------------------------------------------

    /** The item with this path in the add-on's namespace; fails if it does not exist. */
    public static Item item(String path) {
        return BuiltInRegistries.ITEM.getOptional(LibMisc.id(path))
                .orElseThrow(() -> new IllegalStateException("No Thaumic Tinkerer item " + path));
    }

    /** The block with this path in the add-on's namespace; fails if it does not exist. */
    public static Block block(String path) {
        return BuiltInRegistries.BLOCK.getOptional(LibMisc.id(path))
                .orElseThrow(() -> new IllegalStateException("No Thaumic Tinkerer block " + path));
    }

    public static boolean hasItem(String path) {
        return BuiltInRegistries.ITEM.containsKey(LibMisc.id(path));
    }

    public static ItemStack stack(String path) {
        return new ItemStack(item(path));
    }

    public static ItemStack stack(String path, int count) {
        return new ItemStack(item(path), count);
    }

    private static ItemStack tabIcon() {
        ResourceLocation ichor = LibMisc.id(LibItemNames.KAMI_RESOURCE_NAMES[0]);
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(ichor).orElse(Items.ENDER_EYE));
    }

    /** Memoised lookup of a registry-path item for use in static initialisers of recipes. */
    public static Supplier<Item> lazyItem(String path) {
        return Suppliers.memoize(() -> item(path));
    }
}
