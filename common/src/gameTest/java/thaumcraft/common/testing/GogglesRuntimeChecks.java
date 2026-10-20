package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.IGoggles;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.foci.ItemFocusFire;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** One bounded native craft/equip/repair case. Raw ingredients, a charged paying
 * wand, research, XP and a separately damaged anvil INPUT are declared fixtures.
 * Both crafting outputs, their payments and the anvil output must be native.
 * Root owns the loader wrapper and execution; no addon/Baubles item is invented. */
public final class GogglesRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final List<EquipmentSlot> ARMOR = List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
    private static final Map<String, Integer> GOGGLES_VIS = Map.of("aer", 5, "ignis", 5, "aqua", 5, "terra", 5, "ordo", 3, "perditio", 3);
    private GogglesRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel();
        var registries = level.registryAccess();
        BlockPos table = test.absolutePos(new BlockPos(3, 2, 3)), anvil = table.east(2);
        var tableState = level.getBlockState(table);
        var anvilState = level.getBlockState(anvil);
        var tableTag = level.getBlockEntity(table) == null ? null : level.getBlockEntity(table).saveWithFullMetadata(registries);
        var anvilTag = level.getBlockEntity(anvil) == null ? null : level.getBlockEntity(anvil).saveWithFullMetadata(registries);
        test.assertTrue(player.containerMenu == player.inventoryMenu && !player.isUsingItem(), "fixture has no open menu or held-use session to discard");
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var knowledge = KnowledgeAccess.get(player).copy();
        var effects = player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var position = player.position();
        var motion = player.getDeltaMovement();
        var mode = player.gameMode.getGameModeForPlayer();
        var food = new CompoundTag(); player.getFoodData().addAdditionalSaveData(food);
        var abilities = new CompoundTag(); player.getAbilities().addSaveData(abilities);
        int selected = player.getInventory().selected, xp = player.experienceLevel, totalXp = player.totalExperience;
        float progress = player.experienceProgress, health = player.getHealth(), absorption = player.getAbsorptionAmount();
        float yaw = player.getYRot(), pitch = player.getXRot(), fall = player.fallDistance;
        boolean sneaking = player.isShiftKeyDown(), sprinting = player.isSprinting(), grounded = player.onGround();
        var newlyLearned = new ArrayList<RecipeHolder<?>>();
        for (String name : List.of("arcane/goggles", "arcane/wand_cap_gold")) level.getRecipeManager().byKey(id(name)).ifPresent(holder -> {
            if (!player.getRecipeBook().contains(holder)) newlyLearned.add(holder);
        });
        try {
            player.getInventory().clearContent(); player.getInventory().selected = 0;
            player.removeAllEffects(); player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false); player.setSprinting(false);
            player.setPos(table.getX() + .5, table.getY() + 1, table.getZ() + .5);
            player.setDeltaMovement(Vec3.ZERO); player.setOnGround(true); player.fallDistance = 0;
            player.experienceLevel = 4; player.totalExperience = 0; player.experienceProgress = 0;
            level.setBlock(table, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState(), QUIET);
            var tile = (TileArcaneWorkbench) level.getBlockEntity(table);
            ItemStack goggles = craftGoggles(test, player, table, tile);
            properties(test, player, goggles);
            equipAndPrice(test, player, goggles);
            followup(test, player, table, tile);
            repair(test, player, anvil);
        } finally {
            player.closeContainer();
            if (level.getBlockEntity(table) instanceof TileArcaneWorkbench tile) tile.clearContent();
            player.removeAllEffects();
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot).copy());
            player.getInventory().selected = selected;
            player.doTick(); // Native equipment detection removes fixture armor modifiers.
            level.setBlock(table, tableState, QUIET); level.setBlock(anvil, anvilState, QUIET);
            if (tableTag != null && level.getBlockEntity(table) != null) level.getBlockEntity(table).loadWithComponents(tableTag, registries);
            if (anvilTag != null && level.getBlockEntity(anvil) != null) level.getBlockEntity(anvil).loadWithComponents(anvilTag, registries);
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected; player.setGameMode(mode);
            player.setShiftKeyDown(sneaking); player.setSprinting(sprinting);
            player.getAbilities().loadSaveData(abilities); player.onUpdateAbilities();
            player.setPos(position); player.setDeltaMovement(motion); player.setOnGround(grounded); player.fallDistance = fall;
            player.setYRot(yaw); player.setXRot(pitch);
            player.experienceLevel = xp; player.totalExperience = totalXp; player.experienceProgress = progress;
            player.getFoodData().readAdditionalSaveData(food);
            player.removeAllEffects(); effects.forEach(player::addEffect);
            player.setHealth(health); player.setAbsorptionAmount(absorption);
            KnowledgeAccess.get(player).copyFrom(knowledge); player.resetRecipes(newlyLearned);
        }
        test.succeed();
    }

    private static ItemStack craftGoggles(GameTestHelper test, ServerPlayer player, BlockPos pos, TileArcaneWorkbench tile) {
        var level = test.getLevel();
        var recipe = (ArcaneShapedRecipe) level.getRecipeManager().byKey(id("arcane/goggles")).orElseThrow().value();
        test.assertTrue(recipe.research().equals("GOGGLES") && recipe.pattern().equals(List.of("LGL", "L L", "TGT"))
                && recipe.vis().equals(GOGGLES_VIS), "registered original GOGGLES gate, literal shape and six base-vis costs");
        for (var ingredient : Map.of("L", Items.LEATHER, "G", Items.GOLD_INGOT, "T", ThaumcraftContent.THAUMOMETER.get()).entrySet()) {
            var matches = recipe.key().get(ingredient.getKey()).getItems();
            test.assertTrue(matches.length == 1 && matches[0].is(ingredient.getValue()), "original exact goggles ingredient, not a broad tag: " + ingredient.getKey());
        }
        for (int slot : new int[] {0, 2, 3, 5}) tile.setItem(slot, new ItemStack(Items.LEATHER));
        for (int slot : new int[] {1, 7}) tile.setItem(slot, new ItemStack(Items.GOLD_INGOT));
        for (int slot : new int[] {6, 8}) tile.setItem(slot, new ItemStack(ThaumcraftContent.THAUMOMETER.get()));
        test.assertTrue(recipe.matches(ArcaneRecipeInput.snapshot(tile), level), "four leather, two ingots and two actual thaumometers match");
        tile.setItem(4, new ItemStack(Items.LEATHER));
        test.assertTrue(!recipe.matches(ArcaneRecipeInput.snapshot(tile), level), "the empty central cell is significant");
        tile.setItem(4, ItemStack.EMPTY);
        tile.setItem(10, wand("thaumium", 2000));
        KnowledgeAccess.get(player).removeResearch("GOGGLES");
        open(player, pos);
        test.assertTrue(player.containerMenu instanceof ContainerArcaneWorkbench, "native block interaction opens the authoritative workbench");
        var menu = (ContainerArcaneWorkbench) player.containerMenu;
        var before = tile.saveWithoutMetadata(level.registryAccess());
        menu.clicked(0, 0, ClickType.PICKUP, player);
        test.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getCarried().isEmpty() && before.equals(tile.saveWithoutMetadata(level.registryAccess())),
                "absent GOGGLES research refuses without consuming any item or centivis");
        KnowledgeAccess.get(player).addResearch("GOGGLES"); menu.broadcastChanges();
        test.assertTrue(menu.craftable() && menu.getSlot(0).getItem().is(ThaumcraftWearables.GOGGLES.get()), "research unlocks the actual registered goggles result");
        for (String tag : WandData.PRIMALS) test.assertTrue(menu.requiredVis(tag) == (GOGGLES_VIS.get(tag) == 5 ? 450 : 270),
                "first craft has only the paying Thaumium cap discount, never its own unworn output's five percent: " + tag);
        WandData payer = ItemWandCasting.data(tile.getItem(10));
        ItemStack goggles = take(menu, player);
        test.assertTrue(goggles.is(ThaumcraftWearables.GOGGLES.get()) && goggles.getCount() == 1 && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),
                "native result extraction creates exactly one initially UNWORN pair");
        for (int slot = 0; slot < 9; slot++) test.assertTrue(tile.getItem(slot).isEmpty(), "every occupied leather/gold/thaumometer cell is consumed once");
        for (String tag : WandData.PRIMALS) test.assertTrue(ItemWandCasting.getVis(tile.getItem(10), tag) == payer.amount(tag) - (GOGGLES_VIS.get(tag) == 5 ? 450 : 270),
                "real first-craft debit matches native preview: " + tag);
        test.assertTrue(menu.quickMoveStack(player, 0).isEmpty(), "empty matrix cannot replay a goggles output");
        player.closeContainer();
        return goggles;
    }

    private static void properties(GameTestHelper test, ServerPlayer player, ItemStack stack) {
        ThaumcraftWearables.register(); // Repeated bootstrap is deliberately a no-op.
        var goggles = ThaumcraftWearables.GOGGLES.get();
        var material = ThaumcraftWearables.SPECIAL_ARMOR_MATERIAL.get();
        test.assertTrue(BuiltInRegistries.ITEM.getKey(goggles).equals(id("goggles_revealing")) && stack.getMaxDamage() == 350
                && stack.getMaxStackSize() == 1 && stack.getRarity() == Rarity.RARE && goggles.getEnchantmentValue() == 25
                && goggles.getEquipmentSlot() == EquipmentSlot.HEAD && goggles.getDefense() == 1 && goggles.getToughness() == 0,
                "registered native rare helmet has original350 durability, defense1 and enchantability25");
        test.assertTrue(goggles.getMaterial().value() == material && material.defense().equals(Map.of(ArmorItem.Type.HELMET, 1,
                ArmorItem.Type.CHESTPLATE, 3, ArmorItem.Type.LEGGINGS, 2, ArmorItem.Type.BOOTS, 1))
                && material.toughness() == 0 && material.knockbackResistance() == 0 && material.equipSound().equals(SoundEvents.ARMOR_EQUIP_GOLD)
                && material.layers().size() == 1 && material.layers().getFirst().texture(false).equals(id("textures/models/armor/goggles_layer_1.png")),
                "Api52 SPECIAL material and explicit native gold-sound/original-texture-path adapters");
        Item item = goggles;
        test.assertTrue(item instanceof IRepairable && item instanceof IRunicArmor runic && runic.getRunicCharge(stack) == 0
                && item instanceof IVisDiscountGear gear && gear.getVisDiscount(stack, player, null) == 5
                && item instanceof IRevealer revealer && revealer.showNodes(stack, player)
                && item instanceof IGoggles popups && popups.showIngamePopups(stack, player), "all five original API contracts, including nullable aspect and zero innate runic charge");
        test.assertTrue(material.repairIngredient().get().getItems().length == 1 && goggles.isValidRepairItem(stack, new ItemStack(Items.GOLD_INGOT))
                && !goggles.isValidRepairItem(stack, new ItemStack(Items.IRON_INGOT)) && !goggles.isValidRepairItem(stack, new ItemStack(Items.GOLD_NUGGET)),
                "native repair accepts actual gold ingots only, not arbitrary ingots or nuggets");
        var tooltip = new ArrayList<Component>();
        goggles.appendHoverText(stack, Item.TooltipContext.of(test.getLevel()), tooltip, TooltipFlag.NORMAL);
        test.assertTrue(tooltip.equals(List.of(Component.translatable("tc.visdiscount").append(": 5%").withStyle(ChatFormatting.DARK_PURPLE))),
                "original localized dark-purple five-percent tooltip");
        var registries = test.getLevel().registryAccess();
        test.assertTrue(ItemStack.matches(stack, ItemStack.parse(registries, stack.save(registries)).orElseThrow()), "earned native goggles survive the actual item NBT codec");
    }

    private static void equipAndPrice(GameTestHelper test, ServerPlayer player, ItemStack goggles) {
        ItemStack thaumium = wand("thaumium", 2000);
        player.setItemInHand(InteractionHand.MAIN_HAND, goggles);
        test.assertTrue(WandVisDiscount.percentagePoints(player, Aspect.FIRE) == 0 && ItemFocusFire.tickCost(thaumium, player) == 9, "held goggles are not worn discount gear");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); player.setItemInHand(InteractionHand.OFF_HAND, goggles);
        test.assertTrue(WandVisDiscount.percentagePoints(player, Aspect.FIRE) == 0, "offhand goggles grant no discount");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY); player.getInventory().setItem(6, goggles);
        test.assertTrue(WandVisDiscount.percentagePoints(player, Aspect.FIRE) == 0, "hotbar goggles grant no discount");
        player.setItemInHand(InteractionHand.MAIN_HAND, player.getInventory().removeItemNoUpdate(6));
        // GameTestHelper's mock overrides isCreative() to true even after
        // gameMode.setGameMode(SURVIVAL); RMB therefore CREATIVE-COPIES armor.
        // Use the real inventory route, as in the focused survival client,
        // instead of pretending that the mock's native creative clone is wear.
        player.inventoryMenu.clicked(36 + player.getInventory().selected, 0, ClickType.QUICK_MOVE, player);
        test.assertTrue(player.getMainHandItem().isEmpty() && player.getItemBySlot(EquipmentSlot.HEAD).is(ThaumcraftWearables.GOGGLES.get()),
                "native inventory QUICK_MOVE moves the earned pair onto the head without a fixture/creative armor copy");
        goggles = player.getItemBySlot(EquipmentSlot.HEAD);
        player.doTick();
        test.assertTrue(player.getArmorValue() == 1 && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0
                && player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0, "real equipped native defense1 has no invented toughness or knockback resistance");
        // iron 1.1, gold 1.0, thaumium 0.9 and void 0.8, less the goggles' five points (0.75 for void is exact in float)
        int[] one = {105, 95, 85, 75}, five = {525, 475, 424, 375}, three = {315, 285, 254, 225}, fire = {10, 9, 8, 7};
        int[] bare = {550, 500, 450, 400};
        for (int index = 0; index < WandMaterials.CAPS.size(); index++) {
            ItemStack wand = wand(WandMaterials.CAPS.get(index).tag(), 2000);
            var fivePrices = ArcaneVis.price(whole(5), wand, player);
            var onePrices = ArcaneVis.price(whole(1), wand, player);
            var threePrices = ArcaneVis.price(whole(3), wand, player);
            var nullPrices = ArcaneVis.price(whole(5), wand, null);
            for (String tag : WandData.PRIMALS) test.assertTrue(WandVisDiscount.percentagePoints(player, Aspect.getAspect(tag)) == 5
                    && onePrices.get(tag) == one[index] && fivePrices.get(tag) == five[index] && threePrices.get(tag) == three[index] && nullPrices.get(tag) == bare[index],
                    "per-aspect original float multiplication then truncation:1vis85 but3vis254/5vis424, and null-actor cap-only pricing: " + tag);
            test.assertTrue(ItemFocusFire.tickCost(wand, player) == fire[index] && ItemFocusFire.tickCost(wand, null) == 11 - index,
                    "basic Fire uses the same FIRE-specific modifier in centivis; null actor retains cap-only pricing");
        }
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        test.assertTrue(WandVisDiscount.percentagePoints(player, Aspect.FIRE) == 0, "ordinary helmet armor is not discount gear merely because it is worn");
        player.setItemSlot(EquipmentSlot.HEAD, goggles);
        // Armor-storage fixture only: copies in all four slots exercise the public
        // gear API's four-slot sum, not a claim that goggles normally fit boots.
        for (EquipmentSlot slot : ARMOR) if (slot != EquipmentSlot.HEAD) player.setItemSlot(slot, goggles.copy());
        player.setItemInHand(InteractionHand.MAIN_HAND, goggles.copy()); player.setItemInHand(InteractionHand.OFF_HAND, goggles.copy());
        player.getInventory().setItem(8, goggles.copy());
        test.assertTrue(WandVisDiscount.percentagePoints(player, Aspect.FIRE) == 20, "exactly four armor slots count; hands and ordinary inventory do not");
        for (EquipmentSlot slot : ARMOR) if (slot != EquipmentSlot.HEAD) player.setItemSlot(slot, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().setItem(8, ItemStack.EMPTY);
        for (int[] amplifiers : new int[][] {{0, 2}, {2, 0}}) {
            player.addEffect(new MobEffectInstance(ThaumcraftFlux.visExhaust(), 200, amplifiers[0]));
            player.addEffect(new MobEffectInstance(ThaumcraftFlux.infectiousVisExhaust(), 200, amplifiers[1]));
            test.assertTrue(WandVisDiscount.percentagePoints(player, Aspect.FIRE) == -25 && ArcaneVis.price(whole(5), thaumium, player).get("ignis") == 575,
                    "five gear points minus strongest exhaustion30, not two stacked penalties or separately rounded floats");
            player.removeAllEffects();
        }
        player.addEffect(new MobEffectInstance(ThaumcraftFlux.visExhaust(), 200, 1));
        for (String tag : WandData.PRIMALS) test.assertTrue(ArcaneVis.price(whole(14), thaumium, player).get(tag) == 1469,
                "net integer minus15 is divided once: native float boundary1469, not separate-float1470: " + tag);
        player.removeAllEffects();
        // Exercise the shared reducer's addon-discount boundary numerically; no
        // nonexistent high-discount wearable is registered as gameplay content.
        test.assertTrue(WandVisDiscount.modifier(.9F, 80) == .1F && WandVisDiscount.modifier(.9F, 500) == .1F
                && (int) (500 * WandVisDiscount.modifier(.9F, 500)) == 50, "FINAL float modifier floors at0.1, never free or negative vis");
    }

    private static void followup(GameTestHelper test, ServerPlayer player, BlockPos pos, TileArcaneWorkbench tile) {
        KnowledgeAccess.get(player).addResearch("CAP_gold");
        for (int slot : new int[] {0, 1, 2, 3, 5}) tile.setItem(slot, new ItemStack(Items.GOLD_NUGGET));
        open(player, pos);
        var menu = (ContainerArcaneWorkbench) player.containerMenu;
        test.assertTrue(menu.craftable() && menu.getSlot(0).getItem().is(ThaumcraftWands.GOLD_WAND_CAP.get())
                && menu.requiredVis("aer") == 254 && menu.requiredVis("ignis") == 254 && menu.requiredVis("ordo") == 254
                && menu.requiredVis("terra") == 0 && menu.requiredVis("aqua") == 0 && menu.requiredVis("perditio") == 0,
                "actual small follow-up arcane recipe previews3vis at254 with worn goggles and the same finite Thaumium wand");
        WandData payer = ItemWandCasting.data(tile.getItem(10));
        ItemStack cap = take(menu, player);
        test.assertTrue(cap.is(ThaumcraftWands.GOLD_WAND_CAP.get()) && cap.getCount() == 1, "follow-up output is natively crafted, not injected");
        for (String tag : WandData.PRIMALS) test.assertTrue(ItemWandCasting.getVis(tile.getItem(10), tag) == payer.amount(tag)
                - (List.of("aer", "ignis", "ordo").contains(tag) ? 254 : 0), "native follow-up debit exactly matches the per-aspect preview: " + tag);
        for (int slot = 0; slot < 9; slot++) test.assertTrue(tile.getItem(slot).isEmpty(), "five actual nuggets consumed once, no stale ingredients or result");
        player.closeContainer();
    }

    private static void repair(GameTestHelper test, ServerPlayer player, BlockPos pos) {
        test.getLevel().setBlock(pos, Blocks.ANVIL.defaultBlockState(), QUIET);
        open(player, pos);
        test.assertTrue(player.containerMenu instanceof AnvilMenu, "native anvil block opens its real repair menu");
        var menu = (AnvilMenu) player.containerMenu;
        ItemStack damaged = new ItemStack(ThaumcraftWearables.GOGGLES.get()); damaged.setDamageValue(175);
        menu.getSlot(0).set(damaged); menu.getSlot(1).set(new ItemStack(Items.IRON_INGOT));
        int xp = player.experienceLevel;
        menu.clicked(2, 0, ClickType.PICKUP, player);
        test.assertTrue(menu.getCarried().isEmpty() && menu.getSlot(2).getItem().isEmpty() && ItemStack.matches(menu.getSlot(0).getItem(), damaged)
                && menu.getSlot(1).getItem().is(Items.IRON_INGOT) && player.experienceLevel == xp, "native wrong-metal repair preserves both input items and XP");
        menu.getSlot(1).set(new ItemStack(Items.GOLD_INGOT));
        test.assertTrue(menu.getCost() == 1 && menu.getSlot(2).getItem().is(ThaumcraftWearables.GOGGLES.get())
                && menu.getSlot(2).getItem().getDamageValue() == 88, "one native gold ingot repairs floor350/4=87 durability for one level");
        menu.clicked(2, 0, ClickType.PICKUP, player);
        test.assertTrue(menu.getCarried().is(ThaumcraftWearables.GOGGLES.get()) && menu.getCarried().getDamageValue() == 88
                && menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty() && player.experienceLevel == xp - 1,
                "actual anvil extraction consumes one gold ingot and XP, not a supplied repaired output");
        menu.setCarried(ItemStack.EMPTY); player.closeContainer();
    }

    private static ItemStack wand(String cap, int amount) {
        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
        wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", cap, whole(amount)));
        return wand;
    }

    private static Map<String, Integer> whole(int amount) {
        var result = new LinkedHashMap<String, Integer>();
        for (String tag : WandData.PRIMALS) result.put(tag, amount);
        return result;
    }

    private static ItemStack take(ContainerArcaneWorkbench menu, ServerPlayer player) {
        menu.clicked(0, 0, ClickType.PICKUP, player);
        ItemStack result = menu.getCarried(); menu.setCarried(ItemStack.EMPTY);
        return result;
    }

    private static void open(ServerPlayer player, BlockPos pos) {
        player.closeContainer(); player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.gameMode.useItemOn(player, player.serverLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
}
