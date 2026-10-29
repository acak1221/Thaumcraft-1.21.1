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
package thaumic.tinkerer.common.content;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import thaumic.tinkerer.client.BasicItemsClient;
import thaumic.tinkerer.common.item.BasicItemComponents;
import thaumic.tinkerer.common.item.ItemCleansingTalisman;
import thaumic.tinkerer.common.item.ItemInfusedInkwell;
import thaumic.tinkerer.common.item.ItemRevealingHelm;
import thaumic.tinkerer.common.item.ItemShareBook;
import thaumic.tinkerer.common.item.ItemSpellCloth;
import thaumic.tinkerer.common.item.ItemXPTalisman;
import thaumic.tinkerer.common.item.ShareTomeRecipe;
import thaumic.tinkerer.common.item.SpellClothRecipe;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Feature group: items that are neither foci nor Kami: the Spellbinding Cloth, the Talisman of Remedium, the Talisman of
 * Withhold, the Tome of Knowledge Sharing, the Infused Scribing Tools and the Helmet of Revealing, with the two crafting
 * recipe types they need (the cloth's wipe recipe and the tome's recipe that follows the config switch) and the
 * client-side overlays of the group.
 *
 * <p>Registry paths are the 1.7.10 names ({@link LibItemNames}); the 1.7.10 damage values that switched the Talisman of
 * Withhold on and off and the tag that switched the Talisman of Remedium are data components
 * ({@link BasicItemComponents}).</p>
 */
public final class BasicItems implements ContentGroup {
    public static final BasicItems INSTANCE = new BasicItems();

    /**
     * The recipe serializers of the group. They are registered from this group's own register (the shared registries of
     * {@code TTRegistry} do not carry recipe serializers).
     */
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister
            .create(LibMisc.MOD_ID, Registries.RECIPE_SERIALIZER);

    /** {@code thaumictinkerer:spell_cloth}: one cloth and one enchanted item in the grid give the item without its enchantments. */
    public static final RegistrySupplier<RecipeSerializer<SpellClothRecipe>> SPELL_CLOTH_SERIALIZER = RECIPE_SERIALIZERS
            .register(LibItemNames.SPELL_CLOTH, () -> new SimpleCraftingRecipeSerializer<>(SpellClothRecipe::new));

    /** {@code thaumictinkerer:share_tome}: the shaped recipe of the Tome of Knowledge Sharing, gated by the config option. */
    public static final RegistrySupplier<RecipeSerializer<ShareTomeRecipe>> SHARE_TOME_SERIALIZER = RECIPE_SERIALIZERS
            .register("share_tome", ShareTomeRecipe.Serializer::new);

    private BasicItems() {}

    @Override
    public void register() {
        BasicItemComponents.register();
        ItemRevealingHelm.registerMaterial();

        TTRegistry.item(LibItemNames.SPELL_CLOTH, () -> new ItemSpellCloth(new Item.Properties()));
        TTRegistry.item(LibItemNames.CLEANSING_TALISMAN, () -> new ItemCleansingTalisman(new Item.Properties()));
        TTRegistry.item(LibItemNames.XP_TALISMAN, () -> new ItemXPTalisman(new Item.Properties()));
        TTRegistry.item(LibItemNames.SHARE_BOOK, () -> new ItemShareBook(new Item.Properties()));
        TTRegistry.item(LibItemNames.REVEALING_HELM,
                () -> new ItemRevealingHelm(ItemRevealingHelm.MATERIAL, new Item.Properties()));
        // Not in the creative tab (ItemInfusedInkwell.shouldDisplayInTab is false): the item is unfinished in 1.7.10 as well.
        TTRegistry.item(LibItemNames.INFUSED_INKWELL, () -> new ItemInfusedInkwell(new Item.Properties()));

        RECIPE_SERIALIZERS.register();

        // The Talisman of Remedium remembers what it removed last for each wearer; forget a player that leaves.
        PlayerEvent.PLAYER_QUIT.register(player -> ItemCleansingTalisman.forget(player.getUUID()));
        LifecycleEvent.SERVER_STOPPED.register(server -> ItemCleansingTalisman.forgetAll());
    }

    @Override
    public void ready() {}

    @Override
    public void registerClient() {
        BasicItemsClient.register();
    }
}
