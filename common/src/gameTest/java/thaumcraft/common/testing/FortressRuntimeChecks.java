package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.IGoggles;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionResultPatches;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.items.armor.FortressRules;
import thaumcraft.common.items.armor.ItemFortressArmor;
import thaumcraft.common.items.armor.MagicWardGear;
import thaumcraft.common.mixin.LivingEntityAttackStrengthAccess;

/**
 * The Thaumium Fortress armor on real content: its numbers, the goggles and masks it can carry, the seven infusions (the three pieces
 * and the four upgrades, which patch the helm itself), and the ward against real damage: magic at armor/35 and fire and explosions
 * at armor/20 off the damage first, the set multiplier on a real connected player, the armor value vanilla's formula is given, and
 * the two masks' blows (a real attack and real hits). NOT the worn model, the mask drawing or the Grinning Devil's warp effect.
 */
public final class FortressRuntimeChecks {
    private static final String[] IDS = {"fortress_helmet", "fortress_chestplate", "fortress_leggings"};
    private static final int[] DEFENSE = {3, 7, 6}, USES = {440, 640, 600};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS};

    private FortressRuntimeChecks() {}

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    private static ItemStack piece(int index) {
        return new ItemStack(switch (index) {
            case 0 -> ThaumcraftEquipment.FORTRESS_HELMET.get();
            case 1 -> ThaumcraftEquipment.FORTRESS_CHESTPLATE.get();
            default -> ThaumcraftEquipment.FORTRESS_LEGGINGS.get();
        });
    }

    private static ItemStack masked(int mask) {
        ItemStack helm = piece(0);
        helm.set(ThaumcraftEquipment.FORTRESS_MASK.get(), mask);
        return helm;
    }

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        numbers(test);
        recipes(test, level);
        patches(test);
        // Far above the other tests' rooms, as the Portable Hole test does.
        BlockPos origin = test.absolutePos(new BlockPos(3, 100, 3));
        for (int x = (origin.getX() - 16) >> 4; x <= ((origin.getX() + 16) >> 4); x++) {
            for (int z = (origin.getZ() - 16) >> 4; z <= ((origin.getZ() + 16) >> 4); z++) level.getChunk(x, z);
        }
        List<Mob> spawned = new ArrayList<>();
        try {
            mobs(test, level, origin, spawned);
        } catch (RuntimeException | Error failure) {
            spawned.forEach(Mob::discard);
            throw failure;
        }
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "fort-" + UUID.randomUUID().toString().substring(0, 6)), ClientInformation.createDefault());
        level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.setNoGravity(true);
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atBottomCenterOf(origin));
        // A real player is spared for sixty ticks after joining; only then can damage land on it.
        test.runAfterDelay(65, () -> {
            try {
                wearer(test, level, player, origin, spawned);
            } finally {
                spawned.forEach(Mob::discard);
                try {
                    if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                } finally {
                    player.discard();
                    channel.finishAndReleaseAll();
                }
            }
            test.succeed();
        });
    }

    // ---- numbers --------------------------------------------------------------------------------------------------------

    private static void numbers(GameTestHelper test) {
        var material = ThaumcraftEquipment.FORTRESS_ARMOR_MATERIAL.get();
        test.assertTrue(material.defense().equals(java.util.Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 7, ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3))
                        && material.enchantmentValue() == 25 && material.toughness() == 0 && material.knockbackResistance() == 0
                        && material.repairIngredient().get().test(new ItemStack(item("thaumcraft:thaumium_ingot"))),
                "the original armorMatThaumiumFortress: 3/7/6/3, enchantability 25, mended with thaumium");
        ItemStack thaumium = new ItemStack(item("thaumcraft:thaumium_ingot"));
        for (int index = 0; index < IDS.length; index++) {
            ItemStack stack = piece(index);
            ArmorItem armor = (ArmorItem) stack.getItem();
            test.assertTrue(BuiltInRegistries.ITEM.getKey(armor).equals(id(IDS[index])) && armor instanceof ItemFortressArmor, IDS[index] + " is registered");
            test.assertTrue(armor.getDefense() == DEFENSE[index] && stack.getMaxDamage() == USES[index] && armor.getEnchantmentValue() == 25 && armor.getToughness() == 0
                            && stack.getRarity() == Rarity.RARE && stack.getMaxStackSize() == 1 && !stack.hasFoil(),
                    IDS[index] + ": protection " + DEFENSE[index] + ", " + USES[index] + " uses, enchantability 25, rare: " + armor.getDefense() + " " + stack.getMaxDamage());
            test.assertTrue(armor.isValidRepairItem(stack, thaumium) && !armor.isValidRepairItem(stack, new ItemStack(item("thaumcraft:void_ingot")))
                            && !armor.isValidRepairItem(stack, new ItemStack(Items.GOLD_INGOT)) && !armor.isValidRepairItem(stack, new ItemStack(Items.LEATHER)), IDS[index] + " mends with the thaumium ingot alone");
            test.assertTrue(armor instanceof IRepairable && armor instanceof IRunicArmor runic && runic.getRunicCharge(stack) == 0
                            && armor instanceof MagicWardGear ward && ward.wardArmor(stack) == DEFENSE[index] && ward.wardsFire(),
                    IDS[index] + ": repairable, runic charge 0, ward by its armor, fire too");
            test.assertTrue(armor instanceof IRevealer revealer && !revealer.showNodes(stack, null) && armor instanceof IGoggles goggles && !goggles.showIngamePopups(stack, null),
                    IDS[index] + ": no sight without the goggles upgrade");
            var tooltip = new ArrayList<Component>();
            armor.appendHoverText(stack, Item.TooltipContext.of(test.getLevel()), tooltip, TooltipFlag.NORMAL);
            test.assertTrue(tooltip.isEmpty(), IDS[index] + ": nothing to say of a plain piece: " + tooltip);
        }
        ItemStack helm = piece(0);
        helm.set(ThaumcraftEquipment.FORTRESS_GOGGLES.get(), true);
        helm.set(ThaumcraftEquipment.FORTRESS_MASK.get(), 1);
        var tooltip = new ArrayList<Component>();
        helm.getItem().appendHoverText(helm, Item.TooltipContext.of(test.getLevel()), tooltip, TooltipFlag.NORMAL);
        test.assertTrue(tooltip.equals(List.of(Component.translatable("item.ItemGoggles.name").withStyle(ChatFormatting.DARK_PURPLE),
                        Component.translatable("item.HelmetFortress.mask.1").withStyle(ChatFormatting.GOLD))), "goggles in purple, the mask in gold: " + tooltip);
        test.assertTrue(((IRevealer) helm.getItem()).showNodes(helm, null) && ((IGoggles) helm.getItem()).showIngamePopups(helm, null), "the goggles upgrade gives node sight and popups");
        test.assertTrue(ItemFortressArmor.mask(helm) == 1 && ItemFortressArmor.mask(piece(0)) == -1 && ItemFortressArmor.mask(new ItemStack(Items.IRON_HELMET)) == -1 && ItemFortressArmor.mask(ItemStack.EMPTY) == -1,
                "the mask on the stack, -1 for none or for anything else");
        // Both upgrades survive saving, as the original's NBT tags did.
        var registries = test.getLevel().registryAccess();
        ItemStack copy = ItemStack.parse(registries, helm.save(registries)).orElseThrow();
        test.assertTrue(ItemFortressArmor.mask(copy) == 1 && copy.has(ThaumcraftEquipment.FORTRESS_GOGGLES.get()), "the goggles and the mask are saved with the stack");
    }

    // ---- the infusions ----------------------------------------------------------------------------------------------------

    private static InfusionStackRecipe recipe(GameTestHelper test, ServerLevel level, String path) {
        return (InfusionStackRecipe) level.getRecipeManager().byKey(id("infusion/" + path)).orElseThrow(() -> new AssertionError("no recipe " + path)).value();
    }

    private static boolean matches(InfusionStackRecipe recipe, ServerLevel level, ItemStack centre, List<ItemStack> parts) {
        return recipe.matches(new InfusionRecipeInput(centre, parts), level);
    }

    private static void recipes(GameTestHelper test, ServerLevel level) {
        ItemStack ti = new ItemStack(item("thaumcraft:thaumium_ingot")), gold = new ItemStack(Items.GOLD_INGOT), leather = new ItemStack(Items.LEATHER);
        record Armor(String path, String centre, List<ItemStack> parts, int instability, int metal, int armor) {}
        List<Armor> armors = List.of(
                new Armor("fortress_helm", "thaumcraft:thaumium_helmet", List.of(ti, ti, gold, gold, new ItemStack(Items.EMERALD)), 3, 24, 16),
                new Armor("fortress_chest", "thaumcraft:thaumium_chestplate", List.of(ti, ti, ti, ti, gold, leather), 3, 24, 24),
                new Armor("fortress_legs", "thaumcraft:thaumium_leggings", List.of(ti, ti, ti, gold, leather), 3, 24, 20));
        for (int index = 0; index < armors.size(); index++) {
            Armor spec = armors.get(index);
            var recipe = recipe(test, level, spec.path());
            test.assertTrue(recipe.research().equals("ARMORFORTRESS") && recipe.instability() == spec.instability() && recipe.result().is(item("thaumcraft:" + IDS[index]))
                            && recipe.aspects().equals(java.util.Map.of("metallum", spec.metal(), "tutamen", spec.armor(), "praecantatio", 16)) && recipe.resultPatch().isEmpty(),
                    spec.path() + ": ARMORFORTRESS, instability 3, the original aspects: " + recipe.aspects());
            ItemStack centre = new ItemStack(item(spec.centre()));
            test.assertTrue(matches(recipe, level, centre, spec.parts()), spec.path() + ": the thaumium piece and its parts complete it");
            ItemStack worn = centre.copy();
            worn.setDamageValue(1);
            test.assertTrue(!matches(recipe, level, worn, spec.parts()), spec.path() + ": a damaged centre is not the recipe's");
            var wrong = new ArrayList<>(spec.parts());
            wrong.set(0, new ItemStack(Items.IRON_INGOT));
            test.assertTrue(!matches(recipe, level, centre, wrong), spec.path() + ": iron is not thaumium");
            test.assertTrue(!matches(recipe, level, new ItemStack(Items.DIAMOND_CHESTPLATE), spec.parts()), spec.path() + ": diamond armor is not the centre");
        }
        ItemStack fe = new ItemStack(Items.IRON_INGOT);
        record Upgrade(String path, String research, List<ItemStack> parts, int instability, String patch, java.util.Map<String, Integer> aspects) {}
        List<Upgrade> upgrades = List.of(
                new Upgrade("fortress_goggles", "HELMGOGGLES", List.of(new ItemStack(Items.SLIME_BALL), new ItemStack(item("thaumcraft:goggles_revealing"))), 5, "fortress_goggles",
                        java.util.Map.of("sensus", 32, "auram", 16, "tutamen", 16)),
                new Upgrade("fortress_mask_devil", "MASKGRINNINGDEVIL", List.of(new ItemStack(Items.INK_SAC), fe, leather, new ItemStack(item("thaumcraft:shimmerleaf")), new ItemStack(item("thaumcraft:zombie_brain")), fe), 8,
                        "fortress_mask_0", java.util.Map.of("cognitio", 64, "sano", 64, "tutamen", 16)),
                new Upgrade("fortress_mask_ghost", "MASKANGRYGHOST", List.of(new ItemStack(Items.BONE_MEAL), fe, leather, new ItemStack(Items.POISONOUS_POTATO), new ItemStack(Items.WITHER_SKELETON_SKULL), fe), 8,
                        "fortress_mask_1", java.util.Map.of("perditio", 64, "mortuus", 64, "tutamen", 16)),
                new Upgrade("fortress_mask_fiend", "MASKSIPPINGFIEND", List.of(new ItemStack(Items.RED_DYE), fe, leather, new ItemStack(Items.GHAST_TEAR), new ItemStack(Items.MILK_BUCKET), fe), 8,
                        "fortress_mask_2", java.util.Map.of("exanimis", 64, "victus", 64, "tutamen", 16)));
        for (Upgrade spec : upgrades) {
            var recipe = recipe(test, level, spec.path());
            test.assertTrue(recipe.research().equals(spec.research()) && recipe.instability() == spec.instability() && recipe.aspects().equals(spec.aspects())
                            && recipe.resultPatch().orElse("").equals(spec.patch()) && recipe.result().is(ThaumcraftEquipment.FORTRESS_HELMET.get()), spec.path() + ": the original research, aspects and patch: " + recipe.aspects());
            ItemStack worn = piece(0);
            worn.setDamageValue(300);
            test.assertTrue(matches(recipe, level, worn, spec.parts()), spec.path() + ": a worn Fortress helm is a centre (the original's wildcard)");
            test.assertTrue(matches(recipe, level, piece(0), spec.parts()), spec.path() + ": so is a new one");
            test.assertTrue(!matches(recipe, level, new ItemStack(item("thaumcraft:thaumium_helmet")), spec.parts()), spec.path() + ": a Thaumium helm is not");
            test.assertTrue(!matches(recipe, level, piece(1), spec.parts()), spec.path() + ": nor is a Fortress cuirass");
            var short1 = new ArrayList<>(spec.parts());
            short1.remove(short1.size() - 1);
            test.assertTrue(!matches(recipe, level, piece(0), short1), spec.path() + ": a missing part is refused");
        }
    }

    private static void patches(GameTestHelper test) {
        var registries = test.getLevel().registryAccess();
        ItemStack helm = piece(0);
        helm.setDamageValue(123);
        helm.set(DataComponents.CUSTOM_NAME, Component.literal("old helm"));
        helm.enchant(registries.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.PROTECTION), 2);
        ItemStack goggled = InfusionResultPatches.apply(InfusionResultPatches.FORTRESS_GOGGLES, helm);
        test.assertTrue(goggled.is(helm.getItem()) && goggled.getCount() == 1 && goggled.getDamageValue() == 123 && goggled.has(ThaumcraftEquipment.FORTRESS_GOGGLES.get())
                        && goggled.get(DataComponents.CUSTOM_NAME).equals(Component.literal("old helm")) && goggled.isEnchanted() && !helm.has(ThaumcraftEquipment.FORTRESS_GOGGLES.get()),
                "the goggles patch gives the sight and keeps wear, name and enchantment; the old stack is not touched");
        ItemStack ghost = InfusionResultPatches.apply(InfusionResultPatches.FORTRESS_MASK_GHOST, goggled);
        test.assertTrue(ItemFortressArmor.mask(ghost) == 1 && ghost.has(ThaumcraftEquipment.FORTRESS_GOGGLES.get()) && ghost.getDamageValue() == 123, "a mask keeps the goggles");
        ItemStack fiend = InfusionResultPatches.apply(InfusionResultPatches.FORTRESS_MASK_FIEND, ghost);
        test.assertTrue(ItemFortressArmor.mask(fiend) == 2 && ItemFortressArmor.mask(InfusionResultPatches.apply(InfusionResultPatches.FORTRESS_MASK_DEVIL, fiend)) == 0, "a later mask replaces the earlier one");
        for (String patch : List.of(InfusionResultPatches.FORTRESS_GOGGLES, InfusionResultPatches.FORTRESS_MASK_DEVIL)) {
            test.assertTrue(InfusionResultPatches.known(patch) && !InfusionResultPatches.canApply(patch, piece(1)) && !InfusionResultPatches.canApply(patch, piece(2))
                            && !InfusionResultPatches.canApply(patch, new ItemStack(item("thaumcraft:thaumium_helmet"))) && InfusionResultPatches.apply(patch, piece(1)).isEmpty(),
                    patch + " only fits the Fortress helm");
        }
    }

    // ---- the ward on mobs ---------------------------------------------------------------------------------------------------

    private static Mob pig(ServerLevel level, List<Mob> spawned, Vec3 at) {
        Mob pig = EntityType.PIG.create(level);
        pig.moveTo(at.x, at.y, at.z, 0, 0);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        pig.setPersistenceRequired();
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);
        pig.setHealth(500);
        if (!level.addFreshEntity(pig)) throw new AssertionError("the pig was not admitted");
        spawned.add(pig);
        return pig;
    }

    private static double lost(Mob pig, DamageSource source, float damage) {
        pig.setHealth(500);
        pig.invulnerableTime = 0;
        if (!pig.hurt(source, damage)) throw new AssertionError(source.type().msgId() + " did not land");
        return 500 - pig.getHealth();
    }

    private static void dress(Mob pig, int... indices) {
        for (int index : indices) pig.setItemSlot(SLOTS[index], piece(index));
        pig.tick(); // the armor attributes follow the equipment on the entity's own tick
    }

    private static void mobs(GameTestHelper test, ServerLevel level, BlockPos origin, List<Mob> spawned) {
        Vec3 spot = Vec3.atBottomCenterOf(origin);
        var damage = level.damageSources();
        DamageSource magic = damage.magic(), fire = damage.inFire(), burning = damage.onFire(), blast = damage.explosion(null, null), cactus = damage.cactus();
        Mob full = pig(level, spawned, spot);
        dress(full, 0, 1, 2);
        test.assertTrue(full.getArmorValue() == 16, "the pig's armor attribute is 3 + 7 + 6: " + full.getArmorValue());
        // Magic: 16/35 off the damage at once, and wear 1, 4, 3.
        test.assertTrue(Math.abs(lost(full, magic, 20F) - 20 * 19 / 35.0) < 1e-3, "magic: 20 becomes 20 * 19 / 35: " + lost(full, magic, 20F));
        for (int index = 0; index < 3; index++) full.getItemBySlot(SLOTS[index]).setDamageValue(0);
        lost(full, magic, 20F);
        test.assertTrue(full.getItemBySlot(SLOTS[0]).getDamageValue() == 1 && full.getItemBySlot(SLOTS[1]).getDamageValue() == 4 && full.getItemBySlot(SLOTS[2]).getDamageValue() == 3,
                "magic wears the pieces 1, 4 and 3: " + full.getItemBySlot(SLOTS[0]).getDamageValue() + " " + full.getItemBySlot(SLOTS[1]).getDamageValue() + " " + full.getItemBySlot(SLOTS[2]).getDamageValue());
        // Fire, an explosion and burning: 16/20 off, wear 3, 7, 6, and the vanilla formula sees no armor from them (fire and blasts do not bypass it).
        for (var source : List.of(fire, blast, burning)) {
            for (int index = 0; index < 3; index++) full.getItemBySlot(SLOTS[index]).setDamageValue(0);
            double loss = lost(full, source, 20F);
            test.assertTrue(Math.abs(loss - 4.0) < 1e-3, source.type().msgId() + ": 20 becomes 20 * 4 / 20 = 4, and the pieces are not counted twice: " + loss);
            test.assertTrue(full.getItemBySlot(SLOTS[0]).getDamageValue() == 3 && full.getItemBySlot(SLOTS[1]).getDamageValue() == 7 && full.getItemBySlot(SLOTS[2]).getDamageValue() == 6,
                    source.type().msgId() + " wears them 3, 7 and 6");
        }
        // Ordinary damage is vanilla's, with the armor the pig has (a mob gets no set bonus).
        for (int index = 0; index < 3; index++) full.getItemBySlot(SLOTS[index]).setDamageValue(0);
        double ordinary = lost(full, cactus, 20F);
        test.assertTrue(Math.abs(ordinary - CombatRules.getDamageAfterAbsorb(full, 20F, cactus, 16F, 0F)) < 1e-3 && ordinary < 19.9, "ordinary damage meets the plain armor value 16: " + ordinary);
        // The helm alone: 3/35 and 3/20, the set factor of one piece being exactly 1.
        Mob helm = pig(level, spawned, spot.add(2, 0, 0));
        dress(helm, 0);
        test.assertTrue(Math.abs(lost(helm, magic, 20F) - 20 * 32 / 35.0) < 1e-3 && Math.abs(lost(helm, fire, 20F) - 17.0) < 1e-3, "the helm alone: magic 20 * 32 / 35, fire 17: " + lost(helm, magic, 20F) + " " + lost(helm, fire, 20F));
        // Nothing worn: nothing warded.
        Mob bare = pig(level, spawned, spot.add(4, 0, 0));
        test.assertTrue(lost(bare, magic, 20F) == 20 && lost(bare, fire, 20F) == 20, "a bare pig takes it all");
        // The void robe's chest (magic only) beside a Fortress helm: magic 3/35 + 7/35, but fire only 3/20; the robe's armor stays in vanilla's pass.
        Mob mixed = pig(level, spawned, spot.add(6, 0, 0));
        mixed.setItemSlot(EquipmentSlot.HEAD, piece(0));
        mixed.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ThaumcraftEquipment.VOID_ROBE_CHESTPLATE.get()));
        mixed.tick();
        test.assertTrue(Math.abs(lost(mixed, magic, 20F) - 20 * 25 / 35.0) < 1e-3, "a Fortress helm and a void robe ward magic together: " + lost(mixed, magic, 20F));
        double afterFire = 20F * (1 - 3 / 20F);
        double expected = CombatRules.getDamageAfterAbsorb(mixed, (float) afterFire, fire, 7F, 0F);
        test.assertTrue(Math.abs(lost(mixed, fire, 20F) - expected) < 1e-3, "fire: the helm's 3/20, then vanilla with the robe's 7 armor alone: " + lost(mixed, fire, 20F) + " vs " + expected);
        // The masks do nothing to a pig with no player or attacker in it.
        Mob ghosted = pig(level, spawned, spot.add(8, 0, 0));
        ghosted.setItemSlot(EquipmentSlot.HEAD, masked(FortressRules.MASK_GHOST));
        Mob zombie = EntityType.ZOMBIE.create(level);
        zombie.moveTo(spot.x + 10, spot.y, spot.z, 0, 0);
        zombie.setNoAi(true);
        zombie.setNoGravity(true);
        zombie.setPersistenceRequired();
        ChampionJoinRoll.join(level, zombie);
        spawned.add(zombie);
        lost(ghosted, damage.mobAttack(zombie), 10F);
        test.assertTrue(!zombie.hasEffect(MobEffects.WITHER), "the Angry Ghost's mask works for a player wearer only");
    }

    // ---- a real wearer ---------------------------------------------------------------------------------------------------------

    private static void dress(ServerPlayer player, ItemStack... pieces) {
        player.getInventory().clearContent();
        for (ItemStack piece : pieces) player.setItemSlot(((ArmorItem) piece.getItem()).getEquipmentSlot(), piece);
        player.doTick(); // the attributes follow the equipment
    }

    private static double lost(ServerPlayer player, DamageSource source, float damage) {
        player.setHealth(20F);
        player.invulnerableTime = 0;
        if (!player.hurt(source, damage)) throw new AssertionError(source.type().msgId() + " did not land on the player");
        return 20 - player.getHealth();
    }

    private static void wearer(GameTestHelper test, ServerLevel level, ServerPlayer player, BlockPos origin, List<Mob> spawned) {
        var damage = level.damageSources();
        DamageSource magic = damage.magic(), fire = damage.inFire(), cactus = damage.cactus();
        dress(player, piece(0), piece(1), piece(2));
        test.assertTrue(player.getArmorValue() == 16, "the player's armor is 3 + 7 + 6: " + player.getArmorValue());
        test.assertTrue(Math.abs(ItemFortressArmor.set(player) - 1.25) < 1e-12, "the whole set is 1.25");
        // The set bonus: magic 16/35 * 1.25, fire 16/20 * 1.25 = everything, ordinary armor 16 * 1.25 = 20 for vanilla's formula.
        test.assertTrue(Math.abs(lost(player, magic, 20F) - 20 * (1 - 16 / 35.0 * 1.25)) < 1e-3, "magic with the set bonus: " + lost(player, magic, 20F));
        test.assertTrue(lost(player, fire, 20F) < 1e-3, "fire is stopped altogether by the whole set: " + lost(player, fire, 20F));
        double plain = CombatRules.getDamageAfterAbsorb(player, 10F, cactus, 16F, 0F), bonus = CombatRules.getDamageAfterAbsorb(player, 10F, cactus, 20F, 0F);
        test.assertTrue(bonus < plain - 0.2, "the sample has room to tell armor 20 from 16: " + bonus + " " + plain);
        test.assertTrue(Math.abs(lost(player, cactus, 10F) - bonus) < 1e-3, "ordinary damage meets 16 * 1.25 = 20 armor: " + lost(player, cactus, 10F) + " vs " + bonus);
        // The mask lifts it to 1.30.
        dress(player, masked(FortressRules.MASK_DEVIL), piece(1), piece(2));
        test.assertTrue(Math.abs(ItemFortressArmor.set(player) - 1.3) < 1e-12, "a masked helm: 1.30");
        test.assertTrue(Math.abs(lost(player, magic, 20F) - 20 * (1 - 16 / 35.0 * 1.3)) < 1e-3, "magic with the masked set: " + lost(player, magic, 20F));
        test.assertTrue(Math.abs(lost(player, cactus, 10F) - CombatRules.getDamageAfterAbsorb(player, 10F, cactus, 20.8F, 0F)) < 1e-3, "ordinary armor 16 * 1.3 = 20.8");
        // One piece: factor exactly 1; two: 1.125.
        dress(player, piece(1));
        test.assertTrue(Math.abs(ItemFortressArmor.set(player) - 1.0) < 1e-12 && Math.abs(lost(player, cactus, 10F) - CombatRules.getDamageAfterAbsorb(player, 10F, cactus, 7F, 0F)) < 1e-3, "one piece is plain armor");
        dress(player, piece(1), piece(2));
        test.assertTrue(Math.abs(ItemFortressArmor.set(player) - 1.125) < 1e-12 && Math.abs(lost(player, magic, 20F) - 20 * (1 - 13 / 35.0 * 1.125)) < 1e-3, "two pieces: 1.125: " + lost(player, magic, 20F));
        // Boots do not count towards the set.
        dress(player, piece(1), new ItemStack(Items.DIAMOND_BOOTS));
        test.assertTrue(Math.abs(ItemFortressArmor.set(player) - 1.0) < 1e-12, "boots are not part of the set");

        // ---- the masks ------------------------------------------------------------------------------------------------------
        Mob zombie = EntityType.ZOMBIE.create(level);
        zombie.moveTo(Vec3.atBottomCenterOf(origin).add(3, 0, 0));
        zombie.setNoAi(true);
        zombie.setNoGravity(true);
        zombie.setPersistenceRequired();
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);
        ChampionJoinRoll.join(level, zombie);
        spawned.add(zombie);
        dress(player, masked(FortressRules.MASK_GHOST));
        lost(player, damage.mobAttack(zombie), 10F);
        var wither = zombie.getEffect(MobEffects.WITHER);
        test.assertTrue(wither != null && wither.getDuration() == 80 && wither.getAmplifier() == 0, "Angry Ghost: a blow of 10 always withers the dealer for 80 ticks: " + wither);
        zombie.removeAllEffects();
        var arrow = EntityType.ARROW.create(level);
        arrow.setOwner(zombie);
        lost(player, damage.arrow(arrow, zombie), 10F);
        test.assertTrue(!zombie.hasEffect(MobEffects.WITHER), "an arrow is not a melee blow: no wither");
        dress(player, masked(FortressRules.MASK_DEVIL));
        lost(player, damage.mobAttack(zombie), 10F);
        test.assertTrue(!zombie.hasEffect(MobEffects.WITHER), "another mask does not wither");
        dress(player, piece(0));
        lost(player, damage.mobAttack(zombie), 10F);
        test.assertTrue(!zombie.hasEffect(MobEffects.WITHER), "a plain helm does not");
        zombie.discard();

        Mob pig = pig(level, spawned, Vec3.atBottomCenterOf(origin).add(-3, 0, 0));
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(20);
        player.getInventory().clearContent();
        dress(player, masked(FortressRules.MASK_FIEND));
        player.setHealth(10F);
        ((LivingEntityAttackStrengthAccess) player).thaumcraft$setAttackStrengthTicker(1000);
        player.attack(pig);
        test.assertTrue(pig.getHealth() < 500 && Math.abs(player.getHealth() - 11F) < 1e-3, "Sipping Fiend: a blow of 20 heals the player one point: " + player.getHealth() + " " + pig.getHealth());
        dress(player, masked(FortressRules.MASK_GHOST));
        player.setHealth(10F);
        pig.invulnerableTime = 0;
        ((LivingEntityAttackStrengthAccess) player).thaumcraft$setAttackStrengthTicker(1000);
        player.attack(pig);
        test.assertTrue(Math.abs(player.getHealth() - 10F) < 1e-3, "another mask heals nothing: " + player.getHealth());
        dress(player, piece(0));
        player.setHealth(10F);
        pig.invulnerableTime = 0;
        ((LivingEntityAttackStrengthAccess) player).thaumcraft$setAttackStrengthTicker(1000);
        player.attack(pig);
        test.assertTrue(Math.abs(player.getHealth() - 10F) < 1e-3, "nor does a plain helm: " + player.getHealth());
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1);
        player.getInventory().clearContent();
    }
}
