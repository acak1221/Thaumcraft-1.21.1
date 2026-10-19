package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.ArmorDyeRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.armor.ItemRobeArmor;
import thaumcraft.common.items.armor.RecipesRobeArmorDyes;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.foci.ItemFocusFire;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** One bounded native craft/dye/wash/equip/anvil case, source-only until root runs
 * it. INPUTS: actual enchanted fabric and vanilla dyes, finite charged Thaumium
 * payer, research, existing registered goggles (not acquired here), XP, cauldron
 * water/states, and explicitly damaged/named/enchanted repair/dye inputs.
 * Every robe craft, dye result, wash transition, vis payment and repaired output
 * must come through the real menus/useOn flow. No output/world-color injection. */
public final class RobeRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final String[] IDS = {"robe_chestplate", "robe_leggings", "robe_boots"};
    private static final String[][] PATTERNS = {{"I I", "III", "III"}, {"III", "I I", "I I"}, {"I I", "I I"}};
    private static final String[] ASPECTS = {"aer", "aqua", "terra"};
    private static final int[] BASE_VIS = {5, 5, 3}, DURABILITY = {400, 375, 325}, DEFENSE = {3, 2, 1}, DISCOUNT = {2, 2, 1};
    private RobeRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel();
        var registries = level.registryAccess();
        BlockPos table = test.absolutePos(new BlockPos(3, 2, 3)), craft = table.west(), wash = table.east(), anvil = table.south();
        var states = new LinkedHashMap<BlockPos, BlockState>();
        var tiles = new LinkedHashMap<BlockPos, CompoundTag>();
        for (BlockPos pos : List.of(table, craft, wash, anvil)) {
            states.put(pos, level.getBlockState(pos));
            if (level.getBlockEntity(pos) != null) tiles.put(pos, level.getBlockEntity(pos).saveWithFullMetadata(registries));
        }
        test.assertTrue(player.containerMenu == player.inventoryMenu && !player.isUsingItem()
                && player.inventoryMenu.getCraftSlots().isEmpty() && player.inventoryMenu.getCarried().isEmpty(), "fixture has no existing menu, crafting input or held-use session to discard");
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var knowledge = KnowledgeAccess.get(player).copy();
        var effects = player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var position = player.position(); var motion = player.getDeltaMovement(); var mode = player.gameMode.getGameModeForPlayer();
        var food = new CompoundTag(); player.getFoodData().addAdditionalSaveData(food);
        var abilities = new CompoundTag(); player.getAbilities().addSaveData(abilities);
        int selected = player.getInventory().selected, xp = player.experienceLevel, totalXp = player.totalExperience;
        float progress = player.experienceProgress, health = player.getHealth(), absorption = player.getAbsorptionAmount();
        float yaw = player.getYRot(), pitch = player.getXRot(), fall = player.fallDistance;
        boolean sneaking = player.isShiftKeyDown(), sprinting = player.isSprinting(), grounded = player.onGround();
        var newlyLearned = new ArrayList<RecipeHolder<?>>();
        for (String path : List.of("arcane/robe_chestplate", "arcane/robe_leggings", "arcane/robe_boots", "robe_dye")) level.getRecipeManager().byKey(id(path)).ifPresent(holder -> {
            if (!player.getRecipeBook().contains(holder)) newlyLearned.add(holder);
        });
        try {
            player.getInventory().clearContent(); player.getInventory().selected = 0;
            player.removeAllEffects(); player.setGameMode(GameType.SURVIVAL); player.setShiftKeyDown(false); player.setSprinting(false);
            player.setPos(table.getX() + .5, table.getY() + 1, table.getZ() + .5);
            player.setDeltaMovement(Vec3.ZERO); player.setOnGround(true); player.fallDistance = 0;
            player.experienceLevel = 8; player.totalExperience = 0; player.experienceProgress = 0;
            level.setBlock(table, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState(), QUIET);
            var tile = (TileArcaneWorkbench) level.getBlockEntity(table);
            var robes = craftRobes(test, player, table, tile);
            properties(test, player, robes);
            robes.set(0, dye(test, player, craft, robes.getFirst()));
            robes.set(0, wash(test, player, wash, robes.getFirst()));
            equipAndPay(test, player, table, tile, robes);
            repair(test, player, anvil, robes);
        } finally {
            player.closeContainer(); player.inventoryMenu.clearCraftingContent();
            if (level.getBlockEntity(table) instanceof TileArcaneWorkbench tile) tile.clearContent();
            player.removeAllEffects(); player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot).copy());
            player.getInventory().selected = selected; player.doTick();
            states.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            tiles.forEach((pos, tag) -> { if (level.getBlockEntity(pos) != null) level.getBlockEntity(pos).loadWithComponents(tag, registries); });
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected; player.setGameMode(mode);
            player.getAbilities().loadSaveData(abilities); player.onUpdateAbilities();
            player.setShiftKeyDown(sneaking); player.setSprinting(sprinting);
            player.setPos(position); player.setDeltaMovement(motion); player.setOnGround(grounded); player.fallDistance = fall;
            player.setYRot(yaw); player.setXRot(pitch);
            player.experienceLevel = xp; player.totalExperience = totalXp; player.experienceProgress = progress;
            player.getFoodData().readAdditionalSaveData(food); player.removeAllEffects(); effects.forEach(player::addEffect);
            player.setHealth(health); player.setAbsorptionAmount(absorption);
            KnowledgeAccess.get(player).copyFrom(knowledge); player.resetRecipes(newlyLearned);
        }
        test.succeed();
    }

    private static ArrayList<ItemStack> craftRobes(GameTestHelper test, ServerPlayer player, BlockPos pos, TileArcaneWorkbench tile) {
        var level = test.getLevel();
        var amounts = new LinkedHashMap<String, Integer>();
        for (String tag : WandData.PRIMALS) amounts.put(tag, 2000);
        ItemStack payer = new ItemStack(ThaumcraftContent.WAND.get());
        payer.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", "thaumium", amounts));
        tile.setItem(10, payer);
        var outputs = new ArrayList<ItemStack>();
        for (int index = 0; index < IDS.length; index++) {
            var recipe = (ArcaneShapedRecipe) level.getRecipeManager().byKey(id("arcane/" + IDS[index])).orElseThrow().value();
            test.assertTrue(recipe.research().equals("ENCHFABRIC") && recipe.pattern().equals(List.of(PATTERNS[index]))
                    && recipe.vis().equals(Map.of(ASPECTS[index], BASE_VIS[index])) && recipe.key().size() == 1
                    && recipe.key().get("I").getItems().length == 1 && recipe.key().get("I").getItems()[0].is(fabric()), "original ENCHFABRIC gate, exact fabric and literal shape/vis: " + IDS[index]);
            fill(tile, index);
            test.assertTrue(recipe.matches(ArcaneRecipeInput.snapshot(tile), level), "actual fabric input matches " + IDS[index]);
            KnowledgeAccess.get(player).removeResearch("ENCHFABRIC");
            open(player, pos);
            test.assertTrue(player.containerMenu instanceof ContainerArcaneWorkbench, "native arcane workbench opens");
            var menu = (ContainerArcaneWorkbench) player.containerMenu;
            var before = tile.saveWithoutMetadata(level.registryAccess());
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().isEmpty() && menu.getSlot(0).getItem().isEmpty() && before.equals(tile.saveWithoutMetadata(level.registryAccess())), "missing ENCHFABRIC refuses without fabric or finite-vis loss");
            KnowledgeAccess.get(player).addResearch("ENCHFABRIC"); menu.broadcastChanges();
            int price = index == 2 ? 270 : 450;
            test.assertTrue(menu.craftable() && menu.getSlot(0).getItem().is(robeItems().get(index))
                    && WandVisDiscount.percentagePoints(player, null) == 0, "unworn robe outputs do not discount their own native craft");
            WandData old = ItemWandCasting.data(tile.getItem(10));
            for (String tag : WandData.PRIMALS) test.assertTrue(menu.requiredVis(tag) == (tag.equals(ASPECTS[index]) ? price : 0), "exact cap-only450/450/270 preview: " + tag);
            ItemStack result = take(menu, player);
            test.assertTrue(result.is(robeItems().get(index)) && result.getCount() == 1, "native extraction produces exactly one " + IDS[index]);
            for (int slot = 0; slot < 9; slot++) test.assertTrue(tile.getItem(slot).isEmpty(), "native craft consumes every fabric cell once");
            for (String tag : WandData.PRIMALS) test.assertTrue(ItemWandCasting.getVis(tile.getItem(10), tag) == old.amount(tag) - (tag.equals(ASPECTS[index]) ? price : 0), "native finite payer debit matches preview: " + tag);
            test.assertTrue(menu.quickMoveStack(player, 0).isEmpty(), "empty matrix cannot replay the robe output");
            outputs.add(result); player.closeContainer();
        }
        return outputs;
    }

    private static void properties(GameTestHelper test, ServerPlayer player, List<ItemStack> robes) {
        ThaumcraftWearables.register();
        var material = ThaumcraftWearables.ROBE_ARMOR_MATERIAL.get();
        test.assertTrue(material != ThaumcraftWearables.SPECIAL_ARMOR_MATERIAL.get() && material.defense().equals(Map.of(
                ArmorItem.Type.HELMET, 1, ArmorItem.Type.CHESTPLATE, 3, ArmorItem.Type.LEGGINGS, 2, ArmorItem.Type.BOOTS, 1))
                && material.enchantmentValue() == 25 && material.toughness() == 0 && material.knockbackResistance() == 0
                && material.equipSound().equals(SoundEvents.ARMOR_EQUIP_LEATHER) && material.repairIngredient().get().getItems().length == 1
                && material.repairIngredient().get().test(new ItemStack(fabric())), "separate robe holder retains original SPECIAL stats, real fabric repair and explicit native cloth-sound adapter");
        test.assertTrue(material.layers().size() == 2 && material.layers().get(0).dyeable() && !material.layers().get(1).dyeable(), "dyed base and untinted overlay are native material layers");
        for (boolean inner : new boolean[] {false, true}) {
            int layer = inner ? 2 : 1;
            test.assertTrue(material.layers().get(0).texture(inner).equals(id("textures/models/armor/robes_layer_" + layer + ".png"))
                    && material.layers().get(1).texture(inner).equals(id("textures/models/armor/robes_layer_" + layer + "_overlay.png")), "native armor layer paths append the explicit _overlay suffix literally");
        }
        for (int index = 0; index < robes.size(); index++) {
            ItemStack stack = robes.get(index); ItemRobeArmor item = (ItemRobeArmor) stack.getItem(); Item api = item;
            test.assertTrue(BuiltInRegistries.ITEM.getKey(item).equals(id(IDS[index])) && stack.getMaxDamage() == DURABILITY[index]
                    && stack.getMaxStackSize() == 1 && stack.getRarity() == Rarity.UNCOMMON && item.getEnchantmentValue() == 25
                    && item.getDefense() == DEFENSE[index] && item.getToughness() == 0 && item.getMaterial().value() == material
                    && !stack.hasFoil() && !stack.is(ItemTags.DYEABLE), "original native armor stats, no invented glint, and no vanilla modern-dye opt-in: " + IDS[index]);
            test.assertTrue(api instanceof IRepairable && api instanceof IRunicArmor runic && runic.getRunicCharge(stack) == 0
                    && api instanceof IVisDiscountGear gear && gear.getVisDiscount(stack, player, null) == DISCOUNT[index]
                    && gear.getVisDiscount(stack, player, Aspect.FIRE) == DISCOUNT[index], "Repair/runic0/nullable-aspect constant2/2/1 contracts");
            test.assertTrue(item.hasColor(stack) && ItemRobeArmor.color(stack) == 0x6A3880
                    && stack.get(DataComponents.DYED_COLOR).equals(new DyedItemColor(0x6A3880, false)), "default purple is a native prototype color, not absent leather brown");
            test.assertTrue(item.isValidRepairItem(stack, new ItemStack(fabric())) && !item.isValidRepairItem(stack, new ItemStack(Items.GOLD_INGOT))
                    && !item.isValidRepairItem(stack, new ItemStack(Items.LEATHER)) && !item.isValidRepairItem(stack, new ItemStack(Items.PURPLE_WOOL)), "fabric-only per-item repair explicitly rejects gold/leather/wool");
            var tooltip = new ArrayList<Component>(); item.appendHoverText(stack, Item.TooltipContext.of(test.getLevel()), tooltip, TooltipFlag.NORMAL);
            test.assertTrue(tooltip.equals(List.of(Component.translatable("tc.visdiscount").append(": " + DISCOUNT[index] + "%").withStyle(ChatFormatting.DARK_PURPLE))), "original localized purple vis-discount tooltip");
        }
    }

    private static ItemStack dye(GameTestHelper test, ServerPlayer player, BlockPos pos, ItemStack source) {
        var level = test.getLevel(); var registries = level.registryAccess();
        var holder = level.getRecipeManager().byKey(id("robe_dye")).orElseThrow();
        test.assertTrue(holder.value() instanceof RecipesRobeArmorDyes && holder.value().getSerializer() == ThaumcraftWearables.ROBE_DYE_RECIPE_SERIALIZER.get(), "registered native special recipe uses the robe serializer");
        var recipe = (RecipesRobeArmorDyes) holder.value();
        // Explicit INPUT metadata/wear fixture on the earned chest, never a dye result.
        source.setDamageValue(67); source.set(DataComponents.CUSTOM_NAME, Component.literal("robe-dye-input"));
        var extra = new CompoundTag(); extra.putString("preserve", "native-components"); source.set(DataComponents.CUSTOM_DATA, CustomData.of(extra));
        source.enchant(registries.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.UNBREAKING), 1);
        ItemStack before = source.copy();
        var red = new ItemStack(Items.RED_DYE, 32);
        var input = CraftingInput.of(2, 2, List.of(source, red, ItemStack.EMPTY, ItemStack.EMPTY));
        var oneRed = CraftingInput.of(2, 2, List.of(source, new ItemStack(Items.RED_DYE), ItemStack.EMPTY, ItemStack.EMPTY));
        var twoRed = CraftingInput.of(2, 2, List.of(source, new ItemStack(Items.RED_DYE), new ItemStack(Items.RED_DYE), ItemStack.EMPTY));
        test.assertTrue(recipe.matches(input, level) && ItemRobeArmor.color(recipe.assemble(input, registries)) == 0x8C3960
                && ItemRobeArmor.color(recipe.assemble(oneRed, registries)) == 0x8C3960
                && ItemRobeArmor.color(recipe.assemble(twoRed, registries)) != 0x8C3960, "legacy sheep RED plus DEFAULT purple is8C3960; each occupied slot, not stack count, contributes");
        test.assertTrue(!new ArmorDyeRecipe(CraftingBookCategory.EQUIPMENT).matches(input, level), "vanilla modern palette cannot compete for robe dyeing");
        for (var invalid : List.of(
                CraftingInput.of(2, 2, List.of(source, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)),
                CraftingInput.of(2, 2, List.of(source, new ItemStack(robeItems().get(1)), red, ItemStack.EMPTY)),
                CraftingInput.of(2, 2, List.of(new ItemStack(Items.LEATHER_CHESTPLATE), red, ItemStack.EMPTY, ItemStack.EMPTY)),
                CraftingInput.of(2, 2, List.of(new ItemStack(ThaumcraftWearables.GOGGLES.get()), red, ItemStack.EMPTY, ItemStack.EMPTY)),
                CraftingInput.of(2, 2, List.of(source, new ItemStack(Items.RED_WOOL), ItemStack.EMPTY, ItemStack.EMPTY)),
                CraftingInput.of(2, 2, List.of(source, new ItemStack(Items.INK_SAC), ItemStack.EMPTY, ItemStack.EMPTY)),
                CraftingInput.of(2, 2, List.of(red, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)))) {
            test.assertTrue(!recipe.matches(invalid, level) && recipe.assemble(invalid, registries).isEmpty(), "no dye/no robe/two robes/other armor/non-dye refuses even direct assembly");
        }
        ItemStack stacked = source.copyWithCount(4);
        test.assertTrue(recipe.assemble(CraftingInput.of(2, 2, List.of(stacked, red, ItemStack.EMPTY, ItemStack.EMPTY)), registries).getCount() == 1
                && ItemStack.matches(before, source), "even a separately stacked INPUT yields count1 without mutating source components");
        var inventory = player.inventoryMenu;
        inventory.getSlot(1).set(source); inventory.getSlot(2).set(red);
        test.assertTrue(inventory.getSlot(0).getItem().getCount() == 1 && ItemRobeArmor.color(inventory.getSlot(0).getItem()) == 0x8C3960
                && level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow().id().equals(id("robe_dye")), "actual native2x2 result uses the registered robe recipe");
        ItemStack first = take(inventory, player); unchangedExceptColor(test, before, first);
        test.assertTrue(inventory.getSlot(1).getItem().isEmpty() && inventory.getSlot(2).getItem().getCount() == 31
                && inventory.getSlot(0).getItem().isEmpty(), "native2x2 extraction consumes one robe and one dye, not the whole32-stack");
        player.getInventory().setItem(9, inventory.getSlot(2).remove(31));
        test.assertTrue(player.getInventory().getItem(9).getCount() == 31, "31 unused actual red dyes retained");
        level.setBlock(pos, Blocks.CRAFTING_TABLE.defaultBlockState(), QUIET); open(player, pos);
        test.assertTrue(player.containerMenu instanceof CraftingMenu, "native table interaction opens3x3 dye crafting");
        var menu = (CraftingMenu) player.containerMenu;
        ItemStack firstBefore = first.copy();
        menu.getSlot(1).set(first); menu.getSlot(5).set(new ItemStack(Items.BLUE_DYE, 5)); menu.getSlot(9).set(new ItemStack(Items.WHITE_DYE, 2));
        test.assertTrue(menu.getSlot(0).getItem().getCount() == 1 && menu.getSlot(0).getItem().is(firstBefore.getItem()), "native3x3 supports multiple occupied dye slots");
        ItemStack result = take(menu, player); unchangedExceptColor(test, firstBefore, result);
        // Blue+white remain real inputs and legitimately preview vanilla's TWO
        // light-blue dyes after the robe is consumed. An empty result would be
        // the wrong expectation; never disable that unrelated vanilla recipe.
        test.assertTrue(ItemRobeArmor.color(result) == 0xA08BBF && menu.getSlot(1).getItem().isEmpty()
                && menu.getSlot(5).getItem().getCount() == 4 && menu.getSlot(9).getItem().getCount() == 1
                && menu.getSlot(0).getItem().is(Items.LIGHT_BLUE_DYE) && menu.getSlot(0).getItem().getCount() == 2,
                "native3x3 original8C3960+BLUE+WHITE=A08BBF; each dye consumed once, remaining dyes offer only their ordinary vanilla preview");
        player.getInventory().setItem(10, menu.getSlot(5).remove(4)); player.getInventory().setItem(11, menu.getSlot(9).remove(1));
        test.assertTrue(player.getInventory().getItem(10).getCount() == 4 && player.getInventory().getItem(11).getCount() == 1, "unused actual blue/white dyes retained");
        test.assertTrue(menu.getSlot(0).getItem().isEmpty(), "retrieving the two leftover dye inputs clears the uncrafted vanilla preview");
        test.assertTrue(ItemStack.matches(result, ItemStack.parse(registries, result.save(registries)).orElseThrow()), "native item codec retains dye, damage, name, enchantments and unrelated components");
        player.closeContainer(); return result;
    }

    private static ItemStack wash(GameTestHelper test, ServerPlayer player, BlockPos pos, ItemStack robe) {
        var level = test.getLevel(); player.setItemInHand(InteractionHand.MAIN_HAND, robe);
        for (BlockState wrong : List.of(Blocks.CAULDRON.defaultBlockState(), Blocks.LAVA_CAULDRON.defaultBlockState(),
                Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2), Blocks.STONE.defaultBlockState())) {
            level.setBlock(pos, wrong, QUIET); ItemStack before = player.getMainHandItem().copy();
            var result = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(pos));
            test.assertTrue(!result.consumesAction() && level.getBlockState(pos).equals(wrong) && ItemStack.matches(before, player.getMainHandItem()), "actual native use refuses empty/wrong cauldrons without color/water/item changes");
        }
        level.setBlock(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), QUIET);
        ItemStack before = player.getMainHandItem().copy(); int xp = player.experienceLevel;
        for (int index = 0; index < 3; index++) {
            player.setGameMode(index == 2 ? GameType.CREATIVE : GameType.SURVIVAL); player.setShiftKeyDown(index == 1);
            var result = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(pos));
            var state = level.getBlockState(pos);
            test.assertTrue(result.consumesAction() && (index == 2 ? state.is(Blocks.CAULDRON)
                    : state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 2 - index)
                    && player.getMainHandItem().get(DataComponents.DYED_COLOR).equals(new DyedItemColor(ItemRobeArmor.DEFAULT_COLOR, false))
                    && player.experienceLevel == xp, "actual useOn washes custom then DEFAULT/sneaking and creative DEFAULT, spending exactly one water level each and no XP");
            unchangedExceptColor(test, before, player.getMainHandItem());
        }
        player.setShiftKeyDown(false); player.setGameMode(GameType.SURVIVAL);
        return player.getInventory().removeItemNoUpdate(player.getInventory().selected);
    }

    private static void equipAndPay(GameTestHelper test, ServerPlayer player, BlockPos pos, TileArcaneWorkbench tile, List<ItemStack> robes) {
        var gear = new ArrayList<>(robes);
        gear.add(new ItemStack(ThaumcraftWearables.GOGGLES.get())); // Explicit existing-item INPUT, not crafted in this case.
        for (int index = 0; index < gear.size(); index++) player.getInventory().setItem(index + 1, gear.get(index));
        test.assertTrue(WandVisDiscount.percentagePoints(player, null) == 0, "all three real robes and INPUT goggles in inventory grant no discount");
        int defense = 0, discount = 0;
        for (int index = 0; index < gear.size(); index++) {
            ItemStack stack = gear.get(index), record = stack.copy(); var item = (ArmorItem) stack.getItem();
            // GameTestHelper$2.isCreative is ALWAYS true: avoid RMB's native
            // creative-copy and genuinely MOVE each earned item into its slot.
            player.inventoryMenu.clicked(37 + index, 0, ClickType.QUICK_MOVE, player); player.doTick();
            defense += item.getDefense(); discount += index == 3 ? 5 : DISCOUNT[index];
            test.assertTrue(player.getInventory().getItem(index + 1).isEmpty() && ItemStack.matches(record, player.getItemBySlot(item.getEquipmentSlot()))
                    && player.getArmorValue() == defense && WandVisDiscount.percentagePoints(player, null) == discount
                    && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0 && player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0, "native QUICK_MOVE gear record: " + BuiltInRegistries.ITEM.getKey(item) + ", armor=" + defense + ", points=" + discount);
            if (index < 3) robes.set(index, player.getItemBySlot(item.getEquipmentSlot()));
        }
        ItemStack payer = tile.getItem(10);
        for (String tag : WandData.PRIMALS) test.assertTrue(WandVisDiscount.percentagePoints(player, Aspect.getAspect(tag)) == 10, "real robe set5 + goggles5 totals10 for every primal");
        test.assertTrue(ItemFocusFire.tickCost(payer, player) == 7 && (int) (25 * WandVisDiscount.modifier(payer, player, Aspect.AIR)) == 19
                && ArcaneVis.price(Map.of("terra", 3), payer, player).get("terra") == 239, "original FLOAT .9F-.10F truncates to Fire7,25-unit19 and3vis239, never rounded8/20/240");
        fill(tile, 2); open(player, pos);
        var menu = (ContainerArcaneWorkbench) player.containerMenu; WandData old = ItemWandCasting.data(tile.getItem(10));
        test.assertTrue(menu.craftable() && menu.requiredVis("terra") == 239, "actual native follow-up previews the equipped10-point discount");
        ItemStack extraBoots = take(menu, player);
        test.assertTrue(extraBoots.is(ThaumcraftWearables.ROBE_BOOTS.get()) && extraBoots.getCount() == 1, "native discounted follow-up creates another real boots output");
        for (String tag : WandData.PRIMALS) test.assertTrue(ItemWandCasting.getVis(tile.getItem(10), tag) == old.amount(tag) - (tag.equals("terra") ? 239 : 0), "actual equipped follow-up pays239terra and preserves all other primals");
        player.getInventory().setItem(12, extraBoots);
        test.assertTrue(player.getInventory().getItem(12).is(ThaumcraftWearables.ROBE_BOOTS.get()), "real follow-up boots retained"); player.closeContainer();
    }

    private static void repair(GameTestHelper test, ServerPlayer player, BlockPos pos, List<ItemStack> robes) {
        for (int index = 0; index < robes.size(); index++) {
            ItemStack actual = robes.get(index);
            // Separate damaged INPUT copy, not claimed acquired wear or a
            // supplied repaired result. Native AnvilMenu must produce the latter.
            ItemStack damaged = actual.copy(); damaged.setDamageValue(actual.getMaxDamage() / 2);
            test.getLevel().setBlock(pos, Blocks.ANVIL.defaultBlockState(), QUIET); open(player, pos);
            test.assertTrue(player.containerMenu instanceof AnvilMenu, "native anvil block interaction opens fabric repair");
            var menu = (AnvilMenu) player.containerMenu; int xp = player.experienceLevel;
            menu.getSlot(0).set(damaged);
            // The real client sends the existing name on input change. Without
            // this native menu call, a server-only fixture requests name removal.
            if (damaged.has(DataComponents.CUSTOM_NAME)) menu.setItemName(damaged.getHoverName().getString());
            menu.getSlot(1).set(new ItemStack(Items.GOLD_INGOT));
            menu.clicked(2, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().isEmpty() && menu.getSlot(2).getItem().isEmpty() && ItemStack.matches(menu.getSlot(0).getItem(), damaged)
                    && menu.getSlot(1).getItem().is(Items.GOLD_INGOT) && player.experienceLevel == xp, "native anvil explicitly refuses goggles' gold without input/XP loss");
            menu.getSlot(1).set(new ItemStack(fabric())); int expected = damaged.getDamageValue() - damaged.getMaxDamage() / 4;
            test.assertTrue(menu.getCost() == 1 && menu.getSlot(2).getItem().is(actual.getItem()) && menu.getSlot(2).getItem().getDamageValue() == expected, "one enchanted fabric repairs floor(maxDamage/4) for one native level");
            ItemStack result = takeAt(menu, player, 2);
            test.assertTrue(result.is(actual.getItem()) && result.getCount() == 1 && result.getDamageValue() == expected
                    && menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty() && player.experienceLevel == xp - 1
                    && Objects.equals(result.get(DataComponents.DYED_COLOR), damaged.get(DataComponents.DYED_COLOR))
                    && Objects.equals(result.get(DataComponents.CUSTOM_DATA), damaged.get(DataComponents.CUSTOM_DATA))
                    && Objects.equals(result.get(DataComponents.CUSTOM_NAME), damaged.get(DataComponents.CUSTOM_NAME))
                    && result.getEnchantments().equals(damaged.getEnchantments()), "actual anvil output preserves color/name/data/enchantments and pays exactly one fabric/XP per robe");
            player.getInventory().setItem(13 + index, result);
            test.assertTrue(player.getInventory().getItem(13 + index).is(actual.getItem()), "actual repaired output retained"); player.closeContainer();
        }
    }

    private static void unchangedExceptColor(GameTestHelper test, ItemStack before, ItemStack after) {
        test.assertTrue(after.is(before.getItem()) && after.getCount() == 1 && before.getComponents().keySet().equals(after.getComponents().keySet()), "native dye/wash retains item, count1 and component keys");
        for (var component : before.getComponents()) if (component.type() != DataComponents.DYED_COLOR)
            test.assertTrue(Objects.equals(component.value(), after.get(component.type())), "native dye/wash retains unrelated component: " + component.type());
    }

    private static void fill(TileArcaneWorkbench tile, int index) {
        for (int slot = 0; slot < 9; slot++) tile.setItem(slot, slot / 3 < PATTERNS[index].length && PATTERNS[index][slot / 3].charAt(slot % 3) == 'I'
                ? new ItemStack(fabric()) : ItemStack.EMPTY);
    }
    private static Item fabric() { return ThaumcraftContent.MATERIALS.get("enchanted_fabric").get(); }
    private static List<ItemRobeArmor> robeItems() { return List.of(ThaumcraftWearables.ROBE_CHESTPLATE.get(), ThaumcraftWearables.ROBE_LEGGINGS.get(), ThaumcraftWearables.ROBE_BOOTS.get()); }
    private static ItemStack take(AbstractContainerMenu menu, ServerPlayer player) { return takeAt(menu, player, 0); }
    private static ItemStack takeAt(AbstractContainerMenu menu, ServerPlayer player, int slot) {
        menu.clicked(slot, 0, ClickType.PICKUP, player); ItemStack result = menu.getCarried(); menu.setCarried(ItemStack.EMPTY); return result;
    }
    private static void open(ServerPlayer player, BlockPos pos) {
        player.closeContainer();
        player.gameMode.useItemOn(player, player.serverLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit(pos));
    }
    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
}
