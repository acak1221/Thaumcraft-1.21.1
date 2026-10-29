package thaumic.tinkerer.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.IGoggles;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IScribeTools;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.ItemApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.api.potions.WarpEffects;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.item.BasicItemComponents;
import thaumic.tinkerer.common.item.BasicItemComponents.ShareData;
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
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.TTRecipes;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>Runtime checks of the BasicItems group in a running server: registration, the data pack (recipes and tags), the
 * Spellbinding Cloth in a real crafting menu (the wipe, the wear, what it refuses), the Talisman of Remedium's switch and its
 * one-effect-per-second rule with the durability it costs, the Talisman of Withhold's switch, bottling and absorbing, the Tome
 * of Knowledge Sharing between players (bound, live, offline snapshot, saved), its recipe and the config gate, the refill
 * recipe of the Infused Scribing Tools and the Helmet of Revealing's interfaces, discount and armour. They were written
 * without being able to run them as GameTests (no Gradle in the porting session): the first run may show a wrong assumption
 * of a check as well as a defect.</p>
 */
public final class BasicItemsRuntimeChecks {
    private BasicItemsRuntimeChecks() {}

    private static final List<String> ITEMS = List.of(LibItemNames.SPELL_CLOTH, LibItemNames.CLEANSING_TALISMAN,
            LibItemNames.XP_TALISMAN, LibItemNames.SHARE_BOOK, LibItemNames.REVEALING_HELM,
            LibItemNames.INFUSED_INKWELL);

    // ---- helpers ---------------------------------------------------------------------------------------------------

    /** A floor of stone at relative y = 0, so that items, orbs and the mock player stand. */
    private static void floor(GameTestHelper test) {
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) test.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    /** A mock player (it joins the player list and ticks with the world, with a fresh id) at the relative position. */
    private static ServerPlayer player(GameTestHelper test, double x, double y, double z) {
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        Vec3 at = test.absoluteVec(new Vec3(x, y, z));
        player.moveTo(at.x, at.y, at.z, 0, 0);
        player.getInventory().clearContent();
        return player;
    }

    private static ItemStack roundTrip(GameTestHelper test, ItemStack stack) {
        Tag saved = stack.save(test.getLevel().registryAccess());
        return ItemStack.parse(test.getLevel().registryAccess(), saved).orElseThrow();
    }

    private static List<Component> tooltip(GameTestHelper test, ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(test.getLevel()), lines, TooltipFlag.NORMAL);
        return lines;
    }

    /** The translation key of a line, or its plain text when it is not a translation. */
    private static String key(Component line) {
        return line.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : line.getString();
    }

    private static int count(ServerPlayer player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static ItemStack enchantedSword(ServerLevel level, String name) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        Holder<Enchantment> sharpness = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS);
        sword.enchant(sharpness, 3);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return sword;
    }

    private static CraftingMenu craftingMenu(GameTestHelper test, ServerPlayer player) {
        return new CraftingMenu(1, player.getInventory(), ContainerLevelAccess.create(test.getLevel(), test.absolutePos(BlockPos.ZERO)));
    }

    /** Slot 0 of a crafting menu is the result, 1 to 9 are the grid. */
    private static void clearGrid(CraftingMenu menu) {
        for (int slot = 1; slot <= 9; slot++) menu.getSlot(slot).set(ItemStack.EMPTY);
    }

    private static Optional<RecipeHolder<CraftingRecipe>> recipeFor(GameTestHelper test, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return test.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, test.getLevel());
    }

    private static List<ItemStack> grid(ItemStack... cells) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(i < cells.length && cells[i] != null ? cells[i] : ItemStack.EMPTY);
        return grid;
    }

    private static ItemStack tc(String name) {
        return new ItemStack(ItemsTC.require(name));
    }

    // ---- registration ----------------------------------------------------------------------------------------------

    public static void registrationIsComplete(GameTestHelper test) {
        test.assertTrue(ThaumicTinkerer.failures().isEmpty(), "registration failures: " + ThaumicTinkerer.failures());
        for (String path : ITEMS) test.assertTrue(BuiltInRegistries.ITEM.containsKey(LibMisc.id(path)), "item " + path);

        test.assertTrue(BuiltInRegistries.RECIPE_SERIALIZER.containsKey(LibMisc.id("spell_cloth")), "the cloth serializer");
        test.assertTrue(BuiltInRegistries.RECIPE_SERIALIZER.containsKey(LibMisc.id("share_tome")), "the tome serializer");
        test.assertTrue(BasicItemComponents.XP != null && BasicItemComponents.XP.isPresent()
                && BasicItemComponents.ABSORBING.isPresent() && BasicItemComponents.CLEANSING_ENABLED.isPresent()
                && BasicItemComponents.SHARE_TOME.isPresent(), "the four data components");

        // Research: one entry for each item but the unfinished inkwell.
        for (String key : List.of(LibResearch.KEY_SPELL_CLOTH, LibResearch.KEY_CLEANSING_TALISMAN,
                LibResearch.KEY_XP_TALISMAN, LibResearch.KEY_SHARE_TOME, LibResearch.KEY_REVEALING_HELM)) {
            test.assertTrue(ResearchCategories.getResearch(key) != null, "research " + key);
        }
        test.assertTrue(ResearchCategories.getResearch(LibResearch.KEY_INFUSED_INKWELL) == null,
                "the inkwell has no research entry, as in 1.7.10");
        test.assertTrue(((ITTinkererRegisterable) TTRegistry.item(LibItemNames.INFUSED_INKWELL)).getResearchItem() == null,
                "and answers null");
        test.assertTrue(!((ITTinkererRegisterable) TTRegistry.item(LibItemNames.INFUSED_INKWELL)).shouldDisplayInTab(),
                "the inkwell is not in the creative tab");

        // Recipes: crucible, infusion and arcane objects under the research keys, the bench recipes by file name.
        for (String recipe : List.of(LibResearch.KEY_SPELL_CLOTH, LibResearch.KEY_CLEANSING_TALISMAN,
                LibResearch.KEY_XP_TALISMAN, LibResearch.KEY_REVEALING_HELM, LibResearch.KEY_INFUSED_INKWELL,
                ItemInfusedInkwell.REFILL_RECIPE)) {
            test.assertTrue(TTRecipes.has(recipe), "recipe " + recipe);
        }
        test.assertTrue(TTRecipes.has(ItemShareBook.RECIPE_NAME) == ConfigHandler.enableSurvivalShareTome,
                "the tome recipe is registered exactly when the survival option is on");
        test.succeed();
    }

    public static void itemsCarryTheNumbersOfTheOriginal(GameTestHelper test) {
        ItemStack cloth = TTRegistry.stack(LibItemNames.SPELL_CLOTH);
        test.assertTrue(cloth.getMaxDamage() == 35 && cloth.getMaxStackSize() == 1, "cloth: 35 uses, no stacking");
        test.assertTrue(cloth.getRarity() == Rarity.UNCOMMON, "the cloth is uncommon");
        test.assertTrue(!cloth.hasFoil(), "the cloth does not shine");

        ItemStack remedium = TTRegistry.stack(LibItemNames.CLEANSING_TALISMAN);
        test.assertTrue(remedium.getMaxDamage() == 100 && remedium.getMaxStackSize() == 1, "remedium: 100, no stacking");
        test.assertTrue(remedium.getRarity() == Rarity.UNCOMMON, "remedium is uncommon");
        test.assertTrue(remedium.getItem().getEnchantmentValue() == 10, "enchantability 10, equivalent to iron");
        test.assertTrue(remedium.getItem().isValidRepairItem(remedium, TTRegistry.stack(LibItemNames.DARK_QUARTZ)),
                "smokey quartz mends it");
        test.assertTrue(!remedium.getItem().isValidRepairItem(remedium, new ItemStack(Items.QUARTZ)),
                "plain quartz does not");

        ItemStack withhold = TTRegistry.stack(LibItemNames.XP_TALISMAN);
        test.assertTrue(withhold.getMaxStackSize() == 1 && !withhold.isDamageableItem(), "withhold: no stacking, no wear");
        test.assertTrue(withhold.getRarity() == Rarity.UNCOMMON, "withhold is uncommon");

        ItemStack tome = TTRegistry.stack(LibItemNames.SHARE_BOOK);
        test.assertTrue(tome.getMaxStackSize() == 1 && tome.getRarity() == Rarity.EPIC, "the tome is epic and single");

        ItemStack inkwell = TTRegistry.stack(LibItemNames.INFUSED_INKWELL);
        test.assertTrue(inkwell.getMaxDamage() == 800 && inkwell.getMaxStackSize() == 1, "inkwell: 800 ink");
        test.assertTrue(inkwell.getItem() instanceof IScribeTools, "the research table takes it as scribing tools");
        test.assertTrue(inkwell.hasFoil(), "the inkwell shines like an enchanted item");

        ItemStack helm = TTRegistry.stack(LibItemNames.REVEALING_HELM);
        test.assertTrue(helm.getMaxDamage() == 500, "helm: 500 durability");
        test.assertTrue(helm.getRarity() == Rarity.UNCOMMON, "the helm is uncommon");

        // Bauble slots: both talismans are amulets.
        for (ItemStack talisman : List.of(remedium, withhold)) {
            test.assertTrue(talisman.getItem() instanceof IBauble bauble && bauble.getBaubleType(talisman) == BaubleType.AMULET,
                    talisman + " is an amulet");
        }
        test.succeed();
    }

    // ---- data pack -------------------------------------------------------------------------------------------------

    public static void dataPackCarriesTheRecipesAndTheTags(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        for (String recipe : List.of("spell_cloth", "share_tome", ItemInfusedInkwell.REFILL_RECIPE)) {
            test.assertTrue(level.getRecipeManager().byKey(LibMisc.id(recipe)).isPresent(), "recipe file " + recipe);
        }
        test.assertTrue(level.getRecipeManager().byKey(LibMisc.id("spell_cloth")).get().value() instanceof SpellClothRecipe,
                "the cloth recipe is a SpellClothRecipe");
        test.assertTrue(level.getRecipeManager().byKey(LibMisc.id("share_tome")).get().value() instanceof ShareTomeRecipe,
                "the tome recipe is a ShareTomeRecipe");

        ItemStack helm = TTRegistry.stack(LibItemNames.REVEALING_HELM);
        TagKey<Item> repair = TagKey.create(Registries.ITEM, ResourceLocation.parse("thaumcraft:enchantable/repair"));
        test.assertTrue(helm.is(repair), "the helm may take the Repair enchantment");
        test.assertTrue(helm.is(ItemTags.HEAD_ARMOR_ENCHANTABLE), "the helm takes helmet enchantments");
        test.assertTrue(helm.is(ItemTags.ARMOR_ENCHANTABLE), "and armour enchantments");
        test.assertTrue(helm.is(ItemTags.DURABILITY_ENCHANTABLE), "and Unbreaking");
        test.assertTrue(helm.isEnchantable(), "the helm can be enchanted");

        ItemStack remedium = TTRegistry.stack(LibItemNames.CLEANSING_TALISMAN);
        test.assertTrue(remedium.is(ItemTags.DURABILITY_ENCHANTABLE), "Unbreaking fits the talisman (enchantability 10)");
        test.assertTrue(remedium.isEnchantable(), "the talisman can be enchanted");
        test.assertTrue(!TTRegistry.stack(LibItemNames.SPELL_CLOTH).isEnchantable()
                || TTRegistry.stack(LibItemNames.SPELL_CLOTH).getItem().getEnchantmentValue() == 0,
                "the cloth has no enchantability");
        test.succeed();
    }

    // ---- the Spellbinding Cloth -------------------------------------------------------------------------------------

    public static void spellClothWipesTheEnchantmentsInTheCraftingMenu(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 4.5, 1, 4.5);
        CraftingMenu menu = craftingMenu(test, player);
        ServerLevel level = test.getLevel();

        menu.getSlot(2).set(TTRegistry.stack(LibItemNames.SPELL_CLOTH));
        test.assertTrue(menu.getSlot(0).getItem().isEmpty(), "a cloth alone makes nothing");
        ItemStack sword = enchantedSword(level, "Blade");
        test.assertTrue(sword.isEnchanted(), "the sword is enchanted");
        menu.getSlot(5).set(sword);

        ItemStack shown = menu.getSlot(0).getItem();
        test.assertTrue(shown.is(Items.DIAMOND_SWORD) && shown.getCount() == 1, "the result is one diamond sword: " + shown);
        test.assertTrue(!shown.isEnchanted(), "without its enchantments");
        test.assertTrue(shown.has(DataComponents.CUSTOM_NAME) && "Blade".equals(shown.getHoverName().getString()),
                "but with its name, as 1.7.10 left everything but 'ench'");
        test.assertTrue(sword.isEnchanted(), "assembling the result does not touch the original stack");

        menu.clicked(0, 0, ClickType.PICKUP, player);
        ItemStack taken = menu.getCarried();
        test.assertTrue(taken.is(Items.DIAMOND_SWORD) && !taken.isEnchanted(), "the player takes the clean sword: " + taken);
        test.assertTrue(menu.getSlot(5).getItem().isEmpty(), "the enchanted sword is used up");
        ItemStack stayed = menu.getSlot(2).getItem();
        test.assertTrue(stayed.getItem() instanceof ItemSpellCloth && stayed.getDamageValue() == 1,
                "the cloth stays in its slot, worn by one use: " + stayed);
        test.assertTrue(menu.getSlot(0).getItem().isEmpty(), "and the result slot is empty again");
        test.succeed();
    }

    public static void spellClothLastsThirtySixCleaningsAndTheLastOneDestroysIt(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 4.5, 1, 4.5);
        CraftingMenu menu = craftingMenu(test, player);
        ServerLevel level = test.getLevel();

        menu.getSlot(2).set(TTRegistry.stack(LibItemNames.SPELL_CLOTH));
        for (int cleaning = 1; cleaning <= 36; cleaning++) {
            menu.getSlot(5).set(enchantedSword(level, "Blade " + cleaning));
            ItemStack shown = menu.getSlot(0).getItem();
            test.assertTrue(shown.is(Items.DIAMOND_SWORD) && !shown.isEnchanted(), "cleaning " + cleaning + " has a result");
            menu.clicked(0, 0, ClickType.PICKUP, player);
            ItemStack taken = menu.getCarried();
            test.assertTrue(taken.is(Items.DIAMOND_SWORD) && !taken.isEnchanted(), "cleaning " + cleaning + " gives a clean sword");
            menu.setCarried(ItemStack.EMPTY);

            ItemStack cloth = menu.getSlot(2).getItem();
            if (cleaning <= 35) {
                test.assertTrue(cloth.getItem() instanceof ItemSpellCloth && cloth.getDamageValue() == cleaning,
                        "after cleaning " + cleaning + " the cloth has damage " + cleaning + ": " + cloth);
            } else {
                test.assertTrue(cloth.isEmpty(), "the 36th cleaning destroys the cloth (damage above the maximum): " + cloth);
            }
        }
        test.succeed();
    }

    public static void spellClothRefusesWhatIsNotACleaning(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 4.5, 1, 4.5);
        CraftingMenu menu = craftingMenu(test, player);
        ServerLevel level = test.getLevel();
        ItemStack cloth = TTRegistry.stack(LibItemNames.SPELL_CLOTH);

        test.assertTrue(recipeFor(test, grid(cloth, new ItemStack(Items.DIAMOND_SWORD))).isEmpty(),
                "an unenchanted item is not wiped");
        test.assertTrue(recipeFor(test, grid(enchantedSword(level, "a"))).isEmpty(), "an enchanted item alone is not wiped");
        test.assertTrue(recipeFor(test, grid(cloth, cloth.copy(), enchantedSword(level, "a"))).isEmpty(),
                "two cloths are an invalid grid (the original allowed one)");
        test.assertTrue(recipeFor(test, grid(cloth, enchantedSword(level, "a"), enchantedSword(level, "b"))).isEmpty(),
                "two enchanted items are an invalid grid");
        test.assertTrue(recipeFor(test, grid(cloth, enchantedSword(level, "a"), new ItemStack(Items.STICK))).isEmpty(),
                "any extra item breaks the recipe");

        // An enchanted book keeps its enchantments in another component, as the 1.7.10 book did in another tag.
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.upgrade(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 2);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        test.assertTrue(recipeFor(test, grid(cloth, book)).isEmpty(), "a book's stored enchantments are not wiped");

        // The shape does not matter, only the contents: the cloth and the sword in the corners of the grid.
        test.assertTrue(recipeFor(test, grid(cloth, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, enchantedSword(level, "c"))).isPresent(),
                "the places in the grid do not matter");

        // A 2x2 table (the inventory grid) takes it as well.
        CraftingInput small = CraftingInput.of(2, 2, List.of(cloth, enchantedSword(level, "d"), ItemStack.EMPTY, ItemStack.EMPTY));
        test.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, small, level).isPresent(),
                "the 2 x 2 grid works too");
        clearGrid(menu);
        test.succeed();
    }

    // ---- the Talisman of Remedium ------------------------------------------------------------------------------------

    public static void cleansingTalismanSneakClickTogglesAndKeepsTheSwitch(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 4.5, 1, 4.5);
        ItemStack talisman = TTRegistry.stack(LibItemNames.CLEANSING_TALISMAN);
        player.setItemInHand(InteractionHand.MAIN_HAND, talisman);
        IBauble bauble = (IBauble) talisman.getItem();

        test.assertTrue(!ItemCleansingTalisman.isEnabled(talisman), "a new talisman is off");
        test.assertTrue(key(tooltip(test, talisman).get(0)).equals("ttmisc.inactive"), "the tooltip says inactive");

        player.setShiftKeyDown(false);
        talisman.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(!ItemCleansingTalisman.isEnabled(talisman), "a plain click does not switch it");
        test.assertTrue(bauble.equipsOnUse(talisman, player), "a plain click puts it on (the port's hand equip)");

        player.setShiftKeyDown(true);
        test.assertTrue(!bauble.equipsOnUse(talisman, player), "a sneak click is the talisman's own");
        talisman.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(ItemCleansingTalisman.isEnabled(talisman), "a sneak click switches it on");
        test.assertTrue(key(tooltip(test, talisman).get(0)).equals("ttmisc.active"), "the tooltip says active");

        ItemStack loaded = roundTrip(test, talisman);
        test.assertTrue(ItemCleansingTalisman.isEnabled(loaded), "the switch survives saving");
        test.assertTrue(ItemStack.isSameItemSameComponents(loaded, talisman), "the same stack after saving");

        talisman.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(!ItemCleansingTalisman.isEnabled(talisman), "and off again");
        test.assertTrue(ItemStack.isSameItemSameComponents(talisman, TTRegistry.stack(LibItemNames.CLEANSING_TALISMAN)),
                "a switched-off talisman is the same stack as a new one");
        test.succeed();
    }

    public static void cleansingTalismanRemovesOneEffectPerSecondAndPaysDurability(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 4.5, 1, 4.5);
        ItemStack talisman = TTRegistry.stack(LibItemNames.CLEANSING_TALISMAN);
        IBauble bauble = (IBauble) talisman.getItem();
        ItemCleansingTalisman.forget(player.getUUID());
        try {
            // Switched off: nothing happens.
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 600));
            player.tickCount = 20;
            bauble.onWornTick(talisman, player);
            test.assertTrue(player.hasEffect(MobEffects.POISON) && talisman.getDamageValue() == 0, "off: nothing is removed");

            ItemCleansingTalisman.setEnabled(talisman, true);

            // Not at the second: nothing.
            player.tickCount = 21;
            bauble.onWornTick(talisman, player);
            test.assertTrue(player.hasEffect(MobEffects.POISON), "only every 20th tick works");

            // One harmful effect per pass; the others wait, the harmless ones stay.
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600));
            player.tickCount = 20;
            bauble.onWornTick(talisman, player);
            test.assertTrue(!player.hasEffect(MobEffects.POISON), "the first harmful effect is removed");
            test.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the second waits for the next second");
            test.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED), "a beneficial effect is never touched");
            test.assertTrue(talisman.getDamageValue() == 1, "one point of durability: " + talisman.getDamageValue());
            player.tickCount = 40;
            bauble.onWornTick(talisman, player);
            test.assertTrue(!player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the second effect goes on the next pass");
            test.assertTrue(talisman.getDamageValue() == 2, "two points now");

            // An effect that is applied again right after its removal is permanent: removed for nothing.
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
            player.tickCount = 60;
            bauble.onWornTick(talisman, player);
            test.assertTrue(!player.hasEffect(MobEffects.WEAKNESS) && talisman.getDamageValue() == 3, "weakness costs 1");
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
            player.tickCount = 80;
            bauble.onWornTick(talisman, player);
            test.assertTrue(!player.hasEffect(MobEffects.WEAKNESS), "weakness is removed again");
            test.assertTrue(talisman.getDamageValue() == 3, "but for nothing: it is permanent now");
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
            player.tickCount = 100;
            bauble.onWornTick(talisman, player);
            test.assertTrue(!player.hasEffect(MobEffects.WEAKNESS) && talisman.getDamageValue() == 3, "still for nothing");

            // When it stops coming it is forgotten; the next time it costs again.
            player.tickCount = 120;
            bauble.onWornTick(talisman, player);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
            player.tickCount = 140;
            bauble.onWornTick(talisman, player);
            test.assertTrue(!player.hasEffect(MobEffects.WEAKNESS) && talisman.getDamageValue() == 4,
                    "forgotten, so it costs 1 again: " + talisman.getDamageValue());

            // Effects of less than a second are not touched.
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 10));
            player.tickCount = 160;
            bauble.onWornTick(talisman, player);
            test.assertTrue(player.hasEffect(MobEffects.HUNGER) && talisman.getDamageValue() == 4, "a short effect is skipped");
            player.removeEffect(MobEffects.HUNGER);

            // The effects of warp wear it ten times as much.
            Holder<net.minecraft.world.effect.MobEffect> blurred = WarpEffects.holder(WarpEffects.BLURRED_VISION);
            player.addEffect(new MobEffectInstance(blurred, 600));
            player.tickCount = 180;
            bauble.onWornTick(talisman, player);
            test.assertTrue(!player.hasEffect(blurred), "blurred vision is removed");
            test.assertTrue(talisman.getDamageValue() == 14, "for ten points: " + talisman.getDamageValue());

            // Fire is an effect too.
            player.setRemainingFireTicks(200);
            player.tickCount = 200;
            bauble.onWornTick(talisman, player);
            test.assertTrue(!player.isOnFire(), "the fire is put out");
            test.assertTrue(talisman.getDamageValue() == 15, "for one point: " + talisman.getDamageValue());

            // The last point breaks it.
            talisman.setDamageValue(99);
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 600));
            player.tickCount = 220;
            bauble.onWornTick(talisman, player);
            test.assertTrue(!player.hasEffect(MobEffects.WITHER), "the wither is removed");
            test.assertTrue(talisman.isEmpty(), "and the talisman is spent: " + talisman);
        } finally {
            ItemCleansingTalisman.forget(player.getUUID());
        }
        test.succeed();
    }

    // ---- the Talisman of Withhold --------------------------------------------------------------------------------------

    public static void xpTalismanSwitchesOnSneakClickAndKeepsItsPoints(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 4.5, 1, 4.5);
        ItemStack talisman = TTRegistry.stack(LibItemNames.XP_TALISMAN);
        player.setItemInHand(InteractionHand.MAIN_HAND, talisman);
        IBauble bauble = (IBauble) talisman.getItem();

        test.assertTrue(!ItemXPTalisman.isAbsorbing(talisman) && ItemXPTalisman.getXP(talisman) == 0, "new: off, empty");
        List<Component> lines = tooltip(test, talisman);
        test.assertTrue(key(lines.get(0)).equals("ttmisc.xpTalisman.xp") && lines.get(0).getString().contains("0"),
                "XP line: " + lines.get(0).getString());
        test.assertTrue(key(lines.get(1)).equals("ttmisc.notAbsorbing"), "not absorbing");
        test.assertTrue(!bauble.equipsOnUse(talisman, player), "a click never puts it on: it bottles and switches");

        player.setShiftKeyDown(true);
        talisman.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(ItemXPTalisman.isAbsorbing(talisman), "sneak use switches it on");
        test.assertTrue(key(tooltip(test, talisman).get(1)).equals("ttmisc.absorbing"), "absorbing");

        ItemXPTalisman.setXP(talisman, 123);
        ItemStack loaded = roundTrip(test, talisman);
        test.assertTrue(ItemXPTalisman.getXP(loaded) == 123 && ItemXPTalisman.isAbsorbing(loaded),
                "points and switch survive saving");
        test.assertTrue(ItemXPTalisman.hasCmp(loaded), "it carries values of its own");
        test.assertTrue(lineKeys(tooltip(test, loaded)).get(0).equals("ttmisc.xpTalisman.xp"), "the XP line");

        talisman.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(!ItemXPTalisman.isAbsorbing(talisman), "and off again");

        // Full: the switch cannot be turned on.
        ItemXPTalisman.setXP(talisman, ItemXPTalisman.MAX_XP);
        talisman.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(!ItemXPTalisman.isAbsorbing(talisman), "a full talisman does not switch on");
        test.assertTrue(key(tooltip(test, talisman).get(1)).equals("ttmisc.full"), "Full");

        ItemXPTalisman.setXP(talisman, 0);
        ItemXPTalisman.setAbsorbing(talisman, false);
        test.assertTrue(ItemStack.isSameItemSameComponents(talisman, TTRegistry.stack(LibItemNames.XP_TALISMAN)),
                "an empty, switched-off talisman is the same stack as a new one");
        test.succeed();
    }

    private static List<String> lineKeys(List<Component> lines) {
        return lines.stream().map(BasicItemsRuntimeChecks::key).toList();
    }

    public static void xpTalismanTurnsTenPointsAndABottleIntoABottleOEnchanting(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 4.5, 1, 4.5);
        ItemStack talisman = TTRegistry.stack(LibItemNames.XP_TALISMAN);
        player.setItemInHand(InteractionHand.MAIN_HAND, talisman);
        player.setShiftKeyDown(false);
        ServerLevel level = test.getLevel();

        ItemXPTalisman.setXP(talisman, 25);
        talisman.getItem().use(level, player, InteractionHand.MAIN_HAND);
        test.assertTrue(ItemXPTalisman.getXP(talisman) == 25 && count(player, Items.EXPERIENCE_BOTTLE) == 0,
                "without a bottle nothing happens");

        player.getInventory().setItem(20, new ItemStack(Items.GLASS_BOTTLE, 2));
        talisman.getItem().use(level, player, InteractionHand.MAIN_HAND);
        test.assertTrue(ItemXPTalisman.getXP(talisman) == 15, "10 points are spent: " + ItemXPTalisman.getXP(talisman));
        test.assertTrue(count(player, Items.GLASS_BOTTLE) == 1 && count(player, Items.EXPERIENCE_BOTTLE) == 1,
                "a bottle becomes a Bottle o' Enchanting");
        talisman.getItem().use(level, player, InteractionHand.MAIN_HAND);
        test.assertTrue(ItemXPTalisman.getXP(talisman) == 5 && count(player, Items.GLASS_BOTTLE) == 0
                && count(player, Items.EXPERIENCE_BOTTLE) == 2, "a second bottle: 5 points left");

        player.getInventory().setItem(20, new ItemStack(Items.GLASS_BOTTLE, 2));
        talisman.getItem().use(level, player, InteractionHand.MAIN_HAND);
        test.assertTrue(ItemXPTalisman.getXP(talisman) == 5 && count(player, Items.GLASS_BOTTLE) == 2,
                "under 10 points nothing is bottled and the bottles stay");

        // A full inventory: the bottle is dropped at the player.
        ItemXPTalisman.setXP(talisman, 10);
        player.getInventory().clearContent();
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            player.getInventory().items.set(slot, new ItemStack(Items.DIRT, 64));
        }
        player.getInventory().items.set(30, new ItemStack(Items.GLASS_BOTTLE, 2));
        talisman.getItem().use(level, player, InteractionHand.MAIN_HAND);
        test.assertTrue(ItemXPTalisman.getXP(talisman) == 0, "the points are spent");
        AABB around = player.getBoundingBox().inflate(3);
        boolean dropped = !level.getEntitiesOfClass(ItemEntity.class, around,
                item -> item.getItem().is(Items.EXPERIENCE_BOTTLE)).isEmpty();
        test.assertTrue(dropped, "the Bottle o' Enchanting is dropped when the inventory is full");
        test.succeed();
    }

    public static void xpTalismanAbsorbsOrbsInRangeUpToItsLimit(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 4.5, 1, 4.5);
        ServerLevel level = test.getLevel();
        ItemStack talisman = TTRegistry.stack(LibItemNames.XP_TALISMAN);
        IBauble bauble = (IBauble) talisman.getItem();
        Vec3 at = player.position();

        List<ExperienceOrb> orbs = new ArrayList<>();
        for (int value : new int[] { 3, 5, 7 }) {
            ExperienceOrb orb = new ExperienceOrb(level, at.x + 1, at.y + 0.5, at.z, value);
            level.addFreshEntity(orb);
            orbs.add(orb);
        }
        ExperienceOrb far = new ExperienceOrb(level, at.x + 4, at.y, at.z, 9);
        level.addFreshEntity(far);

        // Switched off: the orbs stay.
        bauble.onWornTick(talisman, player);
        test.assertTrue(orbs.stream().noneMatch(ExperienceOrb::isRemoved) && ItemXPTalisman.getXP(talisman) == 0,
                "off: nothing is absorbed");

        ItemXPTalisman.setAbsorbing(talisman, true);
        bauble.onWornTick(talisman, player);
        test.assertTrue(orbs.stream().allMatch(ExperienceOrb::isRemoved), "the three orbs in range are gone");
        test.assertTrue(!far.isRemoved(), "the orb 4 blocks away is out of the 3 block range");
        test.assertTrue(ItemXPTalisman.getXP(talisman) == 15, "their 15 points are inside: " + ItemXPTalisman.getXP(talisman));
        test.assertTrue(ItemXPTalisman.isAbsorbing(talisman), "it keeps absorbing while there is room");
        far.discard();

        // Near the limit: an orb that does not fit stays (and 1.7.10's arithmetic stops the loop after the first).
        ItemXPTalisman.setXP(talisman, 1495);
        ExperienceOrb small = new ExperienceOrb(level, at.x, at.y + 0.5, at.z + 1, 3);
        ExperienceOrb big = new ExperienceOrb(level, at.x, at.y + 0.5, at.z - 1, 10);
        level.addFreshEntity(small);
        level.addFreshEntity(big);
        bauble.onWornTick(talisman, player);
        int xp = ItemXPTalisman.getXP(talisman);
        test.assertTrue(xp <= ItemXPTalisman.MAX_XP, "never above the limit: " + xp);
        test.assertTrue(!big.isRemoved(), "the orb of 10 does not fit into the 5 points that are left");
        test.assertTrue(xp == 1498, "the orb of 3 was taken, whatever the order of the two: " + xp);
        big.discard();
        small.discard();

        // Full: the switch goes off by itself and nothing is taken.
        ItemXPTalisman.setXP(talisman, ItemXPTalisman.MAX_XP);
        ItemXPTalisman.setAbsorbing(talisman, true);
        ExperienceOrb extra = new ExperienceOrb(level, at.x, at.y + 0.5, at.z + 1, 3);
        level.addFreshEntity(extra);
        bauble.onWornTick(talisman, player);
        test.assertTrue(!ItemXPTalisman.isAbsorbing(talisman), "a full talisman switches itself off");
        test.assertTrue(ItemXPTalisman.getXP(talisman) == ItemXPTalisman.MAX_XP && !extra.isRemoved(),
                "and leaves the orb alone");
        extra.discard();
        test.succeed();
    }

    // ---- the Tome of Knowledge Sharing -------------------------------------------------------------------------------------

    public static void shareTomeBindsToItsPlayerAndHandsOverResearchAndAspects(GameTestHelper test) {
        floor(test);
        ServerPlayer owner = player(test, 2.5, 1, 4.5);
        ServerPlayer reader = player(test, 5.5, 1, 4.5);
        ServerLevel level = test.getLevel();

        test.assertTrue(ThaumcraftApiHelper.completeResearch(owner, "NITOR"), "the owner completes NITOR");
        ThaumcraftApiHelper.discoverAspect(owner, Aspect.METAL);
        test.assertTrue(ThaumcraftApiHelper.hasDiscoveredAspect(owner, Aspect.METAL), "the owner knows metallum");
        test.assertTrue(!ThaumcraftApiHelper.isResearchComplete(reader, "NITOR"), "the reader has not done NITOR");
        test.assertTrue(!ThaumcraftApiHelper.hasDiscoveredAspect(reader, Aspect.METAL), "nor knows metallum");

        ItemStack tome = TTRegistry.stack(LibItemNames.SHARE_BOOK);
        test.assertTrue(ItemShareBook.getData(tome) == null && ItemShareBook.getPlayerName(tome) == null, "unassigned");
        test.assertTrue(key(tooltip(test, tome).get(0)).equals("ttmisc.shareTome.noAssign"), "the tooltip: not assigned");

        // The first click binds the tome to the owner and writes the research into it.
        owner.setItemInHand(InteractionHand.MAIN_HAND, tome);
        tome.getItem().use(level, owner, InteractionHand.MAIN_HAND);
        ShareData data = ItemShareBook.getData(tome);
        test.assertTrue(data != null, "the first click binds the tome");
        test.assertTrue(data.owner().equals(owner.getUUID()) && data.player().equals(owner.getGameProfile().getName()),
                "to the owner: " + data);
        test.assertTrue(data.research().contains("NITOR"), "NITOR is written into it: " + data.research());
        test.assertTrue(data.aspects().contains(Aspect.METAL.getTag()), "so is metallum: " + data.aspects());
        List<Component> lines = tooltip(test, tome);
        test.assertTrue(key(lines.get(0)).equals("ttmisc.shareTome.playerName")
                && lines.get(0).getString().contains(owner.getGameProfile().getName()), "the tooltip names the player");

        // Another player clicks: gets everything.
        reader.setItemInHand(InteractionHand.MAIN_HAND, tome);
        tome.getItem().use(level, reader, InteractionHand.MAIN_HAND);
        test.assertTrue(ThaumcraftApiHelper.isResearchComplete(reader, "NITOR"), "the reader has NITOR now");
        test.assertTrue(ThaumcraftApiHelper.hasDiscoveredAspect(reader, Aspect.METAL), "and knows metallum");
        test.assertTrue(ItemShareBook.getData(tome).equals(data), "the tome is unchanged by being read");

        // The owner is online: research done after the binding comes along (the latest, not the snapshot).
        test.assertTrue(ThaumcraftApiHelper.completeResearch(owner, "ENCHFABRIC"), "the owner does more research");
        test.assertTrue(!data.research().contains("ENCHFABRIC"), "the snapshot of the tome lacks it");
        ServerPlayer second = player(test, 5.5, 1, 2.5);
        second.setItemInHand(InteractionHand.MAIN_HAND, tome);
        tome.getItem().use(level, second, InteractionHand.MAIN_HAND);
        test.assertTrue(ThaumcraftApiHelper.isResearchComplete(second, "ENCHFABRIC"),
                "an online owner's latest research is given");
        test.assertTrue(ItemShareBook.getData(tome).equals(data), "the tome itself keeps the first snapshot");

        // Saving keeps the binding and the snapshot.
        ItemStack loaded = roundTrip(test, tome);
        test.assertTrue(data.equals(ItemShareBook.getData(loaded)), "the tome survives saving");
        test.assertTrue(ItemStack.isSameItemSameComponents(loaded, tome), "as the same stack");
        test.succeed();
    }

    public static void shareTomeFallsBackToItsSnapshotWhenItsPlayerIsNotOnline(GameTestHelper test) {
        floor(test);
        ServerPlayer reader = player(test, 5.5, 1, 4.5);
        ServerLevel level = test.getLevel();

        ItemStack tome = TTRegistry.stack(LibItemNames.SHARE_BOOK);
        tome.set(BasicItemComponents.SHARE_TOME.get(), new ShareData("Ghost", UUID.randomUUID(),
                List.of("NITOR"), List.of(Aspect.METAL.getTag(), "not_an_aspect")));
        test.assertTrue(!ThaumcraftApiHelper.isResearchComplete(reader, "NITOR"), "nothing yet");
        reader.setItemInHand(InteractionHand.MAIN_HAND, tome);
        tome.getItem().use(level, reader, InteractionHand.MAIN_HAND);
        test.assertTrue(ThaumcraftApiHelper.isResearchComplete(reader, "NITOR"),
                "the snapshot of an offline player is given");
        test.assertTrue(ThaumcraftApiHelper.hasDiscoveredAspect(reader, Aspect.METAL), "with its aspects");
        test.assertTrue(ItemShareBook.getPlayerName(tome).equals("Ghost"), "the tome stays bound to the offline player");

        // A tome that names a research or an aspect that does not exist does no harm.
        ItemStack odd = TTRegistry.stack(LibItemNames.SHARE_BOOK);
        odd.set(BasicItemComponents.SHARE_TOME.get(), new ShareData("Odd", UUID.randomUUID(),
                List.of("THIS_RESEARCH_DOES_NOT_EXIST"), List.of("neither_does_this_aspect")));
        reader.setItemInHand(InteractionHand.MAIN_HAND, odd);
        odd.getItem().use(level, reader, InteractionHand.MAIN_HAND);
        test.assertTrue(!ThaumcraftApiHelper.isResearchComplete(reader, "THIS_RESEARCH_DOES_NOT_EXIST"),
                "an unknown research is not completed");
        test.succeed();
    }

    public static void shareTomeRecipeFollowsTheSurvivalOption(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        boolean before = ConfigHandler.enableSurvivalShareTome;
        try {
            ItemStack paper = new ItemStack(Items.PAPER);
            ItemStack thaumonomicon = tc("thaumonomicon");
            ItemStack tools = tc("inkwell");
            List<ItemStack> grid = grid(ItemStack.EMPTY, tools, ItemStack.EMPTY, paper, thaumonomicon, paper.copy(),
                    ItemStack.EMPTY, paper.copy(), ItemStack.EMPTY);

            ConfigHandler.enableSurvivalShareTome = true;
            Optional<RecipeHolder<CraftingRecipe>> recipe = recipeFor(test, grid);
            test.assertTrue(recipe.isPresent() && recipe.get().value() instanceof ShareTomeRecipe,
                    "scribing tools over a Thaumonomicon between paper make the tome");
            ItemStack result = recipe.get().value().assemble(CraftingInput.of(3, 3, grid), level.registryAccess());
            test.assertTrue(result.is(TTRegistry.item(LibItemNames.SHARE_BOOK)) && result.getCount() == 1,
                    "one tome: " + result);

            // The 1.7.10 pattern wanted the tools with damage 0: a used set does not make the tome.
            ItemStack worn = tc("inkwell");
            worn.setDamageValue(50);
            test.assertTrue(recipeFor(test, grid(ItemStack.EMPTY, worn, ItemStack.EMPTY, paper, thaumonomicon,
                    paper.copy(), ItemStack.EMPTY, paper.copy(), ItemStack.EMPTY)).isEmpty(),
                    "used scribing tools do not make the tome (the 1.7.10 pattern wanted damage 0)");
            test.assertTrue(recipeFor(test, grid(ItemStack.EMPTY, tools, ItemStack.EMPTY, paper, new ItemStack(Items.BOOK),
                    paper.copy(), ItemStack.EMPTY, paper.copy(), ItemStack.EMPTY)).isEmpty(), "a plain book does not");

            ConfigHandler.enableSurvivalShareTome = false;
            test.assertTrue(recipeFor(test, grid).isEmpty(), "with the option off the recipe does nothing");
        } finally {
            ConfigHandler.enableSurvivalShareTome = before;
        }
        test.succeed();
    }

    // ---- the Infused Scribing Tools ----------------------------------------------------------------------------------------

    public static void infusedInkwellIsRefilledByEightInkSacs(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        ItemStack inkwell = TTRegistry.stack(LibItemNames.INFUSED_INKWELL);
        inkwell.setDamageValue(700);
        ItemStack sac = new ItemStack(Items.INK_SAC);
        List<ItemStack> grid = grid(sac, sac.copy(), sac.copy(), sac.copy(), inkwell, sac.copy(), sac.copy(), sac.copy(),
                sac.copy());
        Optional<RecipeHolder<CraftingRecipe>> recipe = recipeFor(test, grid);
        test.assertTrue(recipe.isPresent(), "eight ink sacs around the tools are a recipe");
        ItemStack result = recipe.get().value().assemble(CraftingInput.of(3, 3, grid), level.registryAccess());
        test.assertTrue(result.is(TTRegistry.item(LibItemNames.INFUSED_INKWELL)) && result.getDamageValue() == 0
                && result.getCount() == 1, "a full set comes out: " + result);

        grid.set(0, ItemStack.EMPTY);
        Optional<RecipeHolder<CraftingRecipe>> seven = recipeFor(test, grid);
        test.assertTrue(seven.isEmpty() || !seven.get().value().assemble(CraftingInput.of(3, 3, grid),
                level.registryAccess()).is(TTRegistry.item(LibItemNames.INFUSED_INKWELL)), "seven sacs are not enough");
        test.succeed();
    }

    // ---- the Helmet of Revealing ------------------------------------------------------------------------------------------

    public static void helmOfRevealingIsGogglesAndAThaumiumHelmetWithADiscount(GameTestHelper test) {
        floor(test);
        ServerPlayer player = player(test, 4.5, 1, 4.5);
        ItemStack helm = TTRegistry.stack(LibItemNames.REVEALING_HELM);
        Item item = helm.getItem();

        test.assertTrue(item instanceof IGoggles && item instanceof IRevealer && item instanceof IVisDiscountGear
                && item instanceof IRunicArmor && item instanceof IRepairable, "all five Thaumcraft interfaces");
        test.assertTrue(((IGoggles) item).showIngamePopups(helm, player), "it shows the popups");
        test.assertTrue(((IRevealer) item).showNodes(helm, player), "and the nodes");
        test.assertTrue(((IVisDiscountGear) item).getVisDiscount(helm, player, null) == 5
                && ((IVisDiscountGear) item).getVisDiscount(helm, player, Aspect.FIRE) == 5, "5% for every aspect");
        test.assertTrue(((IRunicArmor) item).getRunicCharge(helm) == 0, "no runic shield");

        ArmorItem armor = (ArmorItem) item;
        test.assertTrue(armor.getType() == ArmorItem.Type.HELMET, "a helmet");
        test.assertTrue(armor.getDefense() == 2, "2 armour points like the thaumium helmet: " + armor.getDefense());
        test.assertTrue(armor.getMaterial().value().enchantmentValue() == 25, "enchantability 25");
        test.assertTrue(item.isValidRepairItem(helm, tc("thaumium_ingot")), "thaumium ingots mend it");
        test.assertTrue(!item.isValidRepairItem(helm, new ItemStack(Items.IRON_INGOT)), "iron does not");
        test.assertTrue(armor.getMaterial().value().layers().get(0).texture(false)
                .equals(LibMisc.id("textures/models/armor/revealing_layer_1.png")),
                "the worn texture is revealing_layer_1.png: " + armor.getMaterial().value().layers().get(0).texture(false));

        List<Component> lines = tooltip(test, helm);
        test.assertTrue(lines.size() == 1 && key(lines.get(0)).equals("tc.visdiscount")
                && lines.get(0).getString().endsWith(": 5%"), "the tooltip line: " + lines);

        // Worn, it lowers the cost of a wand by 5 points (the default wooden rod with an iron cap, modifier 1.1 without).
        ItemStack wand = ItemApi.getItem("itemWandCasting", 0);
        test.assertTrue(WandHelper.isWand(wand), "a wand");
        float bare = WandHelper.getConsumptionModifier(wand, player, Aspect.FIRE, true);
        player.setItemSlot(EquipmentSlot.HEAD, helm);
        float worn = WandHelper.getConsumptionModifier(wand, player, Aspect.FIRE, true);
        test.assertTrue(Math.abs((bare - worn) - 0.05F) < 1e-4F, "the helmet takes 5% off: " + bare + " -> " + worn);
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

        // A click with it in the hand puts it on (it is armour).
        player.setItemInHand(InteractionHand.MAIN_HAND, helm);
        item.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(item), "using it equips it");

        // The debugging leftover of 1.7.10: a click on a block tells the chat what block entity is there.
        test.setBlock(new BlockPos(1, 1, 1), Blocks.CHEST);
        BlockPos chest = test.absolutePos(new BlockPos(1, 1, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, TTRegistry.stack(LibItemNames.REVEALING_HELM));
        InteractionResult result = player.getMainHandItem().getItem().useOn(new UseOnContext(player,
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(chest), Direction.UP, chest, false)));
        test.assertTrue(result == InteractionResult.PASS, "using it on a block passes on to the equip: " + result);
        test.succeed();
    }

    public static void everyGroupItemKeepsItsDataThroughSaving(GameTestHelper test) {
        for (String path : ITEMS) {
            ItemStack stack = TTRegistry.stack(path);
            ItemStack loaded = roundTrip(test, stack);
            test.assertTrue(ItemStack.isSameItemSameComponents(stack, loaded) && loaded.is(stack.getItem()),
                    path + " survives saving");
        }
        ItemStack worn = TTRegistry.stack(LibItemNames.SPELL_CLOTH);
        worn.setDamageValue(17);
        test.assertTrue(roundTrip(test, worn).getDamageValue() == 17, "the wear of the cloth survives saving");
        test.assertTrue(ItemSpellCloth.color(TTRegistry.stack(LibItemNames.SPELL_CLOTH)) == java.awt.Color.HSBtoRGB(0.75F, 0.5F, 1F),
                "a new cloth is violet");
        worn.setDamageValue(35);
        worn.setDamageValue(34);
        test.assertTrue(ItemSpellCloth.color(worn) == java.awt.Color.HSBtoRGB(0.75F, (35F - 34F) / 35F * 0.5F, 1F),
                "a nearly worn cloth is nearly white");
        worn.setDamageValue(35);
        test.assertTrue(ItemSpellCloth.color(worn) == 0xFFFFFFFF, "a worn-out cloth is white");
        test.assertTrue(ThaumicTinkerer.failures().isEmpty(), "no registration failures: " + ThaumicTinkerer.failures());
        test.succeed();
    }
}
