package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** One bounded native-menu/gameplay check. Raw materials, charged wand, floor,
 * no-AI cows, XP and explicitly damaged anvil INPUTS are declared fixtures.
 * Equipment crafting outputs, block drops, attack damage and anvil outputs are
 * never supplied. Root owns the loader wrapper and all execution. */
public final class ThaumiumEquipmentRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final String[] IDS = {"thaumium_sword", "thaumium_axe", "thaumium_pickaxe", "thaumium_shovel", "thaumium_hoe",
            "thaumium_helmet", "thaumium_chestplate", "thaumium_leggings", "thaumium_boots"};
    private static final String[][] PATTERNS = {{"I", "I", "S"}, {"II", "SI", "S "}, {"III", " S ", " S "},
            {"I", "S", "S"}, {"II", "S ", "S "}, {"III", "I I"}, {"I I", "III", "III"},
            {"III", "I I", "I I"}, {"I I", "I I"}};

    private ThaumiumEquipmentRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel();
        var registries = level.registryAccess();
        BlockPos origin = test.absolutePos(new BlockPos(3, 2, 3));
        BlockPos table = origin.west(2), anvil = origin.north(), mine = origin.east(2);
        var states = new LinkedHashMap<BlockPos, BlockState>();
        var tiles = new LinkedHashMap<BlockPos, CompoundTag>();
        for (BlockPos mutable : BlockPos.betweenClosed(origin.offset(-2, -1, -1), origin.offset(2, 2, 1))) {
            BlockPos pos = mutable.immutable();
            states.put(pos, level.getBlockState(pos));
            if (level.getBlockEntity(pos) != null) tiles.put(pos, level.getBlockEntity(pos).saveWithFullMetadata(registries));
        }
        test.assertTrue(player.containerMenu == player.inventoryMenu, "fixture player has no pre-existing open menu to discard");
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var knowledge = KnowledgeAccess.get(player).copy();
        var effects = player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var food = new CompoundTag();
        player.getFoodData().addAdditionalSaveData(food);
        var abilities = new CompoundTag();
        player.getAbilities().addSaveData(abilities);
        var position = player.position();
        var motion = player.getDeltaMovement();
        var mode = player.gameMode.getGameModeForPlayer();
        int selected = player.getInventory().selected, xp = player.experienceLevel, totalXp = player.totalExperience;
        float progress = player.experienceProgress, health = player.getHealth(), absorption = player.getAbsorptionAmount();
        float yaw = player.getYRot(), pitch = player.getXRot(), fall = player.fallDistance;
        boolean sneaking = player.isShiftKeyDown(), sprinting = player.isSprinting(), grounded = player.onGround();
        boolean limitedCrafting = level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING);
        var targets = new ArrayList<Cow>();
        var newlyLearned = new ArrayList<RecipeHolder<?>>();
        for (String id : IDS) level.getRecipeManager().byKey(id(id)).ifPresent(holder -> {
            if (!player.getRecipeBook().contains(holder)) newlyLearned.add(holder);
        });
        try {
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.removeAllEffects();
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false); player.setSprinting(false);
            player.experienceLevel = 40; player.totalExperience = 0; player.experienceProgress = 0;
            level.getGameRules().getRule(GameRules.RULE_LIMITED_CRAFTING).set(false, level.getServer());
            states.keySet().forEach(pos -> level.setBlock(pos, pos.getY() == origin.getY() - 1
                    ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET));
            player.setPos(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
            player.setDeltaMovement(Vec3.ZERO); player.setOnGround(true); player.fallDistance = 0;
            KnowledgeAccess.get(player).removeResearch("THAUMIUM");
            ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
            for (String primal : WandData.PRIMALS) ItemWandCasting.addRealVis(wand, primal, 777, true);
            player.getInventory().setItem(8, wand);
            ItemStack wandBefore = wand.copy();

            var crafted = craftAll(test, player, table);
            properties(test, crafted);
            mining(test, player, mine, crafted.get(2));
            combat(test, player, origin, crafted, targets);
            armor(test, player, crafted);
            repair(test, player, anvil, crafted);
            test.assertTrue(!KnowledgeAccess.get(player).isResearchComplete("THAUMIUM")
                    && ItemStack.matches(wandBefore, player.getInventory().getItem(8)),
                    "all nine ordinary crafts and native uses leave research absent and every wand centivis unchanged");
        } finally {
            player.closeContainer();
            targets.forEach(Cow::discard);
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot).copy());
            player.getInventory().selected = selected;
            // Native equipment detection removes fixture modifiers and reapplies
            // the original gear; do not leave test-applied attributes behind.
            player.doTick();
            states.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            tiles.forEach((pos, tag) -> {
                if (level.getBlockEntity(pos) != null) level.getBlockEntity(pos).loadWithComponents(tag, registries);
            });
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.setGameMode(mode); player.setShiftKeyDown(sneaking); player.setSprinting(sprinting);
            player.getAbilities().loadSaveData(abilities); player.onUpdateAbilities();
            player.setPos(position); player.setDeltaMovement(motion); player.setOnGround(grounded); player.fallDistance = fall;
            player.setYRot(yaw); player.setXRot(pitch);
            player.experienceLevel = xp; player.totalExperience = totalXp; player.experienceProgress = progress;
            player.getFoodData().readAdditionalSaveData(food);
            player.removeAllEffects(); effects.forEach(player::addEffect);
            player.setHealth(health); player.setAbsorptionAmount(absorption);
            KnowledgeAccess.get(player).copyFrom(knowledge);
            player.resetRecipes(newlyLearned);
            level.getGameRules().getRule(GameRules.RULE_LIMITED_CRAFTING).set(limitedCrafting, level.getServer());
        }
        test.succeed();
    }

    private static List<ItemStack> craftAll(GameTestHelper test, ServerPlayer player, BlockPos pos) {
        var level = test.getLevel();
        List<Item> items = List.of(ThaumcraftEquipment.THAUMIUM_SWORD.get(), ThaumcraftEquipment.THAUMIUM_AXE.get(),
                ThaumcraftEquipment.THAUMIUM_PICKAXE.get(), ThaumcraftEquipment.THAUMIUM_SHOVEL.get(), ThaumcraftEquipment.THAUMIUM_HOE.get(),
                ThaumcraftEquipment.THAUMIUM_HELMET.get(), ThaumcraftEquipment.THAUMIUM_CHESTPLATE.get(),
                ThaumcraftEquipment.THAUMIUM_LEGGINGS.get(), ThaumcraftEquipment.THAUMIUM_BOOTS.get());
        var crafted = new ArrayList<ItemStack>();
        level.setBlock(pos, Blocks.CRAFTING_TABLE.defaultBlockState(), QUIET);
        open(player, pos);
        test.assertTrue(player.containerMenu instanceof CraftingMenu, "actual vanilla table interaction opens native 3x3 crafting");
        var menu = (CraftingMenu) player.containerMenu;
        for (int index = 0; index < IDS.length; index++) {
            var input = new ArrayList<ItemStack>();
            for (int cell = 0; cell < 9; cell++) {
                int row = cell / 3, column = cell % 3;
                char symbol = row < PATTERNS[index].length && column < PATTERNS[index][row].length()
                        ? PATTERNS[index][row].charAt(column) : ' ';
                ItemStack material = symbol == 'I' ? new ItemStack(ThaumcraftContent.MATERIALS.get("thaumium_ingot").get())
                        : symbol == 'S' ? new ItemStack(Items.STICK) : ItemStack.EMPTY;
                input.add(material.copy());
                menu.getSlot(cell + 1).set(material);
            }
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, input), level).orElseThrow();
            test.assertTrue(recipe.id().equals(id(IDS[index])) && recipe.value() instanceof ShapedRecipe && menu.recipeMatches(recipe)
                    && menu.getSlot(0).getItem().is(items.get(index)) && menu.getSlot(0).getItem().getCount() == 1,
                    "registered original shaped recipe without research/vis gate: " + IDS[index]);
            menu.clicked(0, 0, ClickType.PICKUP, player);
            ItemStack result = menu.getCarried();
            menu.setCarried(ItemStack.EMPTY);
            test.assertTrue(result.is(items.get(index)) && result.getCount() == 1, "native ResultSlot extracts exactly one " + IDS[index]);
            for (int cell = 1; cell <= 9; cell++) test.assertTrue(menu.getSlot(cell).getItem().isEmpty(), "native extraction consumes every occupied input cell once");
            test.assertTrue(menu.getSlot(0).getItem().isEmpty(), "no stale equipment result can be replayed");
            crafted.add(result);
        }
        player.closeContainer();
        return crafted;
    }

    private static void properties(GameTestHelper test, List<ItemStack> crafted) {
        var tier = ThaumcraftEquipment.THAUMIUM_TIER;
        test.assertTrue(tier.getUses() == 400 && tier.getSpeed() == 7 && tier.getAttackDamageBonus() == 2
                && tier.getEnchantmentValue() == 22 && tier.getIncorrectBlocksForDrops().equals(BlockTags.INCORRECT_FOR_DIAMOND_TOOL),
                "Api48: original400/7/2/22 and harvest3 mapped to diamond incorrect-block tag");
        var ingot = new ItemStack(ThaumcraftContent.MATERIALS.get("thaumium_ingot").get());
        test.assertTrue(tier.getRepairIngredient().getItems().length == 1 && tier.getRepairIngredient().test(ingot), "repair ingredient is the actual Thaumium ingot, not recipe ore-fuzz");
        int[] durability = {400, 400, 400, 400, 400, 275, 400, 375, 325};
        for (int index = 0; index < crafted.size(); index++) {
            ItemStack stack = crafted.get(index);
            Item item = stack.getItem();
            test.assertTrue(BuiltInRegistries.ITEM.getKey(item).equals(id(IDS[index])) && stack.getMaxDamage() == durability[index]
                    && stack.getMaxStackSize() == 1 && stack.getRarity() == Rarity.UNCOMMON && item instanceof IRepairable
                    && item.getEnchantmentValue() == (index == 4 ? 5 : index < 5 ? 22 : 25),
                    "registered item, durability, uncommon rarity, Repair eligibility and original enchantability: " + IDS[index]);
            test.assertTrue(item.isValidRepairItem(stack, ingot) && !item.isValidRepairItem(stack, new ItemStack(Items.IRON_INGOT))
                    && !item.isValidRepairItem(stack, new ItemStack(Items.GOLD_INGOT)), "native repair accepts Thaumium and rejects ordinary metal ingots");
            if (index < 5) test.assertTrue(item instanceof TieredItem tool && tool.getTier() == tier, "all five native tools use the canonical tier");
            var registries = test.getLevel().registryAccess();
            test.assertTrue(ItemStack.matches(stack, ItemStack.parse(registries, stack.save(registries)).orElseThrow()), "native item NBT codec retains actual registered equipment");
        }
        test.assertTrue(crafted.get(2).getDestroySpeed(Blocks.STONE.defaultBlockState()) == 7
                && crafted.get(1).getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) == 7
                && crafted.get(3).getDestroySpeed(Blocks.DIRT.defaultBlockState()) == 7,
                "native pick/axe/shovel effective-block rules expose original efficiency7");
        test.assertTrue(crafted.get(2).isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "harvest3 genuinely permits obsidian, not merely iron-tier ores");
    }

    private static void mining(GameTestHelper test, ServerPlayer player, BlockPos pos, ItemStack pick) {
        var level = test.getLevel();
        var bounds = new AABB(pos).inflate(2);
        var previous = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().map(ItemEntity::getUUID).toList();
        try {
            level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), QUIET);
            player.setItemInHand(InteractionHand.MAIN_HAND, pick);
            test.assertTrue(player.gameMode.destroyBlock(pos) && level.isEmptyBlock(pos) && pick.getDamageValue() == 1,
                    "real survival block destruction spends one of400 durability on the earned pickaxe");
            var drops = level.getEntitiesOfClass(ItemEntity.class, bounds, drop -> !previous.contains(drop.getUUID()));
            test.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(Items.OBSIDIAN) && drops.getFirst().getItem().getCount() == 1,
                    "native correct-tier harvest creates exactly one physical obsidian, not an injected output");
        } finally {
            level.getEntitiesOfClass(ItemEntity.class, bounds, drop -> !previous.contains(drop.getUUID())).forEach(ItemEntity::discard);
        }
    }

    private static void combat(GameTestHelper test, ServerPlayer player, BlockPos origin, List<ItemStack> crafted, List<Cow> targets) {
        double[] modifiers = {6, 5, 4, 3, 0};
        double[] speeds = {-2.4F, -3.1F, -2.8F, -3F, -1F};
        for (int index = 0; index < 5; index++) {
            ItemStack weapon = crafted.get(index);
            double[] damage = attribute(weapon, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE);
            double[] speed = attribute(weapon, EquipmentSlot.MAINHAND, Attributes.ATTACK_SPEED);
            test.assertTrue(damage[0] == (index == 4 ? 0 : 1) && damage[1] == modifiers[index]
                    && speed[0] == 1 && speed[1] == speeds[index], "original additive damage, no hoe damage entry, explicit native attack-speed adapter");
            test.assertTrue(attribute(weapon, EquipmentSlot.OFFHAND, Attributes.ATTACK_DAMAGE)[0] == 0, "combat modifiers are native main-hand only");
            player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            player.resetAttackStrengthTicker();
            // These are real native player ticks, not forced cooldown fields or
            // manually installed modifiers. Axe's .9 attack speed needs>22 ticks.
            for (int tick = 0; tick < 40; tick++) player.doTick();
            player.setSprinting(false); player.setOnGround(true); player.fallDistance = 0;
            test.assertTrue(player.getAttackStrengthScale(.5F) == 1 && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 1 + modifiers[index]
                    && Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - (4 + speeds[index])) < 1e-6,
                    "native equipped attributes and full cooldown yield nominal7/6/5/4/1 without enchanted-damage fiction");
            Cow target = EntityType.COW.create(test.getLevel());
            targets.add(target);
            target.setNoAi(true); target.setPos(Vec3.atCenterOf(origin.east()).add(0, -.5, 0));
            test.assertTrue(test.getLevel().addFreshEntity(target), "real unmodified-health no-AI cow fixture spawns");
            float before = target.getHealth();
            int wear = weapon.getDamageValue();
            player.attack(target);
            test.assertTrue(target.getHealth() == before - (float)(1 + modifiers[index]) && target.getLastDamageSource() != null
                    && target.getLastDamageSource().getEntity() == player && weapon.getDamageValue() == wear + (index == 0 ? 1 : index == 4 ? 0 : 2),
                    "actual full-strength native attack applies exact base-plus-modifier damage and original sword1/digger2/hoe0 wear: " + IDS[index]);
            target.discard();
        }
    }

    private static void armor(GameTestHelper test, ServerPlayer player, List<ItemStack> crafted) {
        int[] defenses = {2, 6, 5, 2};
        int total = 0;
        for (int index = 0; index < 4; index++) {
            ItemStack stack = crafted.get(index + 5);
            var item = (ArmorItem) stack.getItem();
            test.assertTrue(item.getDefense() == defenses[index] && item.getToughness() == 0
                    && item.getMaterial().value() == ThaumcraftEquipment.THAUMIUM_ARMOR_MATERIAL.get()
                    && ((IRunicArmor) item).getRunicCharge(stack) == 0, "original armor defense/material and constant zero runic charge");
            player.setItemSlot(item.getEquipmentSlot(), stack);
            player.doTick();
            total += defenses[index];
            test.assertTrue(player.getArmorValue() == total && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0
                    && player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0, "real equipped armor attributes total2/8/13/15, with no invented toughness/knockback/shield");
        }
    }

    private static void repair(GameTestHelper test, ServerPlayer player, BlockPos pos, List<ItemStack> crafted) {
        for (ItemStack actual : crafted) {
            // Independent damaged INPUT fixture, not claimed earned wear or a
            // fabricated repaired output. Native AnvilMenu must produce that.
            ItemStack damaged = new ItemStack(actual.getItem());
            damaged.setDamageValue(damaged.getMaxDamage() / 2);
            test.getLevel().setBlock(pos, Blocks.ANVIL.defaultBlockState(), QUIET);
            open(player, pos);
            test.assertTrue(player.containerMenu instanceof AnvilMenu, "actual anvil block interaction opens native repair menu");
            var menu = (AnvilMenu) player.containerMenu;
            menu.getSlot(0).set(damaged);
            menu.getSlot(1).set(new ItemStack(Items.IRON_INGOT));
            int beforeXp = player.experienceLevel;
            menu.clicked(2, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getSlot(2).getItem().isEmpty() && menu.getCarried().isEmpty() && ItemStack.matches(menu.getSlot(0).getItem(), damaged)
                    && menu.getSlot(1).getItem().is(Items.IRON_INGOT) && menu.getSlot(1).getItem().getCount() == 1
                    && player.experienceLevel == beforeXp, "native wrong-metal repair refuses without consumption");
            menu.getSlot(1).set(new ItemStack(ThaumcraftContent.MATERIALS.get("thaumium_ingot").get()));
            int expected = damaged.getDamageValue() - damaged.getMaxDamage() / 4;
            test.assertTrue(menu.getCost() == 1 && menu.getSlot(2).getItem().getDamageValue() == expected, "native single-ingot repair restores floor(maxDurability/4), charging one level");
            menu.clicked(2, 0, ClickType.PICKUP, player);
            ItemStack repaired = menu.getCarried();
            test.assertTrue(repaired.is(actual.getItem()) && repaired.getCount() == 1 && repaired.getDamageValue() == expected
                    && menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty() && player.experienceLevel == beforeXp - 1,
                    "actual native anvil extraction consumes exactly one Thaumium ingot and pays XP for each of nine items");
            menu.setCarried(ItemStack.EMPTY);
            player.closeContainer();
        }
    }

    private static double[] attribute(ItemStack stack, EquipmentSlot slot, Holder<Attribute> attribute) {
        double[] result = {0, 0};
        stack.forEachModifier(slot, (type, modifier) -> {
            if (type.equals(attribute)) {
                result[0]++;
                result[1] = modifier.operation() == AttributeModifier.Operation.ADD_VALUE ? modifier.amount() : Double.NaN;
            }
        });
        return result;
    }

    private static void open(ServerPlayer player, BlockPos pos) {
        player.closeContainer();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.gameMode.useItemOn(player, player.serverLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
}
