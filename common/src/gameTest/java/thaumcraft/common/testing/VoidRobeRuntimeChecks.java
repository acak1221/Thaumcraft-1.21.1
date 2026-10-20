package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.ArmorDyeRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.IGoggles;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.IWarpingGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.items.armor.ItemRobeArmor;
import thaumcraft.common.items.armor.ItemVoidRobeArmor;
import thaumcraft.common.items.armor.MagicWardGear;
import thaumcraft.common.items.armor.RecipesRobeArmorDyes;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.lib.MagicWard;
import thaumcraft.common.lib.WarpGear;
import thaumcraft.common.lib.potions.PotionFluxTaint;

/**
 * The Void Thaumaturge robes on real content: their numbers and repair, the three infusions (undamaged centre, goggles and robes), the
 * vis discount, warp and node sight a real connected player gets from them, the mending over real ticks (two points a second worn, one in
 * the pack, none in creative), the magic ward against real damage sources on mobs and on the player (an armor/35 share off the damage
 * at once, a point or more of wear per piece, nothing for ordinary damage, for pieces in the pack or for other armor), and the dye and
 * the wash that the two robe families share. NOT the worn model, the colors on a client or the sounds.
 */
public final class VoidRobeRuntimeChecks {
    private static final String[] IDS = {"void_robe_helmet", "void_robe_chestplate", "void_robe_leggings"};
    private static final int[] DEFENSE = {3, 7, 6}, USES = {110, 160, 150};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS};

    private VoidRobeRuntimeChecks() {}

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    private static ItemStack robe(int index) {
        return new ItemStack(switch (index) {
            case 0 -> ThaumcraftEquipment.VOID_ROBE_HELMET.get();
            case 1 -> ThaumcraftEquipment.VOID_ROBE_CHESTPLATE.get();
            default -> ThaumcraftEquipment.VOID_ROBE_LEGGINGS.get();
        });
    }

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        numbers(test);
        recipes(test, level);
        BlockPos origin = test.absolutePos(new BlockPos(3, 64, 3));
        for (int x = (origin.getX() - 18) >> 4; x <= ((origin.getX() + 18) >> 4); x++) {
            for (int z = (origin.getZ() - 18) >> 4; z <= ((origin.getZ() + 18) >> 4); z++) level.getChunk(x, z);
        }
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "robe-" + UUID.randomUUID().toString().substring(0, 6)), ClientInformation.createDefault());
        level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.setNoGravity(true);
        player.setGameMode(GameType.SURVIVAL);
        List<Mob> spawned = new ArrayList<>();
        BlockPos cauldron = origin.east(3);
        var before = level.getBlockState(cauldron);
        Runnable cleanup = () -> {
            spawned.forEach(Mob::discard);
            level.setBlock(cauldron, before, 3);
            try {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
            } finally {
                player.discard();
                channel.finishAndReleaseAll();
            }
        };
        try {
            test.assertTrue(player.level() == level && player.connection != null, "a real connected player wears the robes");
            player.setPos(Vec3.atBottomCenterOf(origin));
            gear(test, player);
            mending(test, player);
            wardMobs(test, level, origin, spawned);
            dyeAndWash(test, level, player, cauldron);
            player.getInventory().clearContent();
        } catch (RuntimeException | Error failure) {
            cleanup.run();
            throw failure;
        }
        // A real player is spared for sixty ticks after joining; only then can damage land on it.
        test.runAfterDelay(65, () -> {
            try {
                wardPlayer(test, level, player);
            } finally {
                cleanup.run();
            }
            test.succeed();
        });
    }

    // ---- numbers --------------------------------------------------------------------------------------------------------

    private static void numbers(GameTestHelper test) {
        ItemStack ingot = new ItemStack(item("thaumcraft:void_ingot"));
        var voidMaterial = ThaumcraftEquipment.VOID_ARMOR_MATERIAL.get();
        var material = ThaumcraftEquipment.VOID_ROBE_ARMOR_MATERIAL.get();
        test.assertTrue(material != voidMaterial && material != ThaumcraftWearables.ROBE_ARMOR_MATERIAL.get()
                        && material.defense().equals(voidMaterial.defense()) && material.enchantmentValue() == 10 && material.toughness() == 0 && material.knockbackResistance() == 0
                        && material.equipSound().equals(SoundEvents.ARMOR_EQUIP_IRON),
                "the original takes the void armor material (3/7/6/3, enchantability 10), as its own holder: " + material.defense());
        for (int index = 0; index < IDS.length; index++) {
            ItemStack stack = robe(index);
            ArmorItem piece = (ArmorItem) stack.getItem();
            String id = IDS[index];
            test.assertTrue(BuiltInRegistries.ITEM.getKey(piece).equals(id(id)) && piece instanceof ItemVoidRobeArmor, id + " is registered");
            test.assertTrue(piece.getDefense() == DEFENSE[index] && stack.getMaxDamage() == USES[index] && piece.getEnchantmentValue() == 10 && piece.getToughness() == 0
                            && stack.getRarity() == Rarity.EPIC && stack.getMaxStackSize() == 1 && !stack.hasFoil() && piece.getMaterial().value() == material,
                    id + ": protection " + DEFENSE[index] + ", " + USES[index] + " uses, enchantability 10, epic: " + piece.getDefense() + " " + stack.getMaxDamage() + " " + stack.getRarity());
            test.assertTrue(piece.isValidRepairItem(stack, ingot) && !piece.isValidRepairItem(stack, new ItemStack(item("thaumcraft:primal_charm")))
                            && !piece.isValidRepairItem(stack, new ItemStack(item("thaumcraft:enchanted_fabric"))) && !piece.isValidRepairItem(stack, new ItemStack(Items.GOLD_INGOT)),
                    id + " mends with the void ingot alone");
            test.assertTrue(piece instanceof IRepairable && piece instanceof IRunicArmor runic && runic.getRunicCharge(stack) == 0
                            && piece instanceof IVisDiscountGear gear && gear.getVisDiscount(stack, null, null) == 5 && gear.getVisDiscount(stack, null, Aspect.FIRE) == 5
                            && piece instanceof IWarpingGear warp && warp.getWarp(stack, null) == 2 && piece instanceof MagicWardGear ward && ward.wardArmor(stack) == DEFENSE[index],
                    id + ": repairable, runic charge 0, discount 5, warp 2, ward by its armor");
            boolean hood = index == 0;
            test.assertTrue(piece instanceof IRevealer revealer && revealer.showNodes(stack, null) == hood && piece instanceof IGoggles goggles && goggles.showIngamePopups(stack, null) == hood,
                    id + ": node sight and popups for the hood alone");
            test.assertTrue(((ItemVoidRobeArmor) piece).hasColor(stack) && ItemRobeArmor.color(stack) == 0x6A3880 && stack.get(DataComponents.DYED_COLOR).equals(new DyedItemColor(0x6A3880, false))
                            && !stack.is(ItemTags.DYEABLE), id + ": the default purple 6961280 as a prototype, no vanilla dye opt-in");
            var tooltip = new ArrayList<Component>();
            piece.appendHoverText(stack, Item.TooltipContext.of(test.getLevel()), tooltip, TooltipFlag.NORMAL);
            test.assertTrue(tooltip.equals(List.of(Component.translatable("tc.visdiscount").append(": 5%").withStyle(ChatFormatting.DARK_PURPLE))), id + ": the vis discount line");
        }
        test.assertTrue(ThaumcraftEquipment.VOID_ROBE_HELMET.get().getEquipmentSlot() == EquipmentSlot.HEAD && ThaumcraftEquipment.VOID_ROBE_CHESTPLATE.get().getEquipmentSlot() == EquipmentSlot.CHEST
                && ThaumcraftEquipment.VOID_ROBE_LEGGINGS.get().getEquipmentSlot() == EquipmentSlot.LEGS, "hood on the head, robe on the chest, leggings on the legs, and no boots");
    }

    // ---- the infusions ----------------------------------------------------------------------------------------------------

    private static void recipes(GameTestHelper test, ServerLevel level) {
        String[] recipes = {"void_robe_helm", "void_robe_chest", "void_robe_legs"};
        List<Map<String, Integer>> aspects = List.of(
                Map.of("metallum", 16, "sensus", 16, "tutamen", 16, "pannus", 16, "praecantatio", 16, "alienis", 16, "vacuos", 16),
                Map.of("metallum", 24, "tutamen", 24, "pannus", 24, "praecantatio", 16, "alienis", 16, "vacuos", 24),
                Map.of("metallum", 20, "tutamen", 20, "pannus", 20, "praecantatio", 16, "alienis", 16, "vacuos", 20));
        String[] centres = {"thaumcraft:void_helmet", "thaumcraft:void_chestplate", "thaumcraft:void_leggings"};
        String[] firsts = {"thaumcraft:goggles_revealing", "thaumcraft:robe_chestplate", "thaumcraft:robe_leggings"};
        ItemStack fabric = new ItemStack(item("thaumcraft:enchanted_fabric")), salis = new ItemStack(item("thaumcraft:salis_mundus"));
        for (int index = 0; index < recipes.length; index++) {
            var infusion = (InfusionStackRecipe) level.getRecipeManager().byKey(id("infusion/" + recipes[index])).orElseThrow(() -> new AssertionError("no recipe")).value();
            test.assertTrue(infusion.research().equals("ARMORVOIDFORTRESS") && infusion.instability() == 6 && infusion.result().is(item("thaumcraft:" + IDS[index]))
                            && infusion.result().getCount() == 1 && infusion.aspects().equals(aspects.get(index)) && infusion.components().size() == 6,
                    recipes[index] + ": ARMORVOIDFORTRESS, instability 6, the original aspects, six components: " + infusion.aspects());
            ItemStack centre = new ItemStack(item(centres[index]));
            ItemStack first = new ItemStack(item(firsts[index]));
            ItemStack last = index == 0 ? fabric : new ItemStack(Items.LEATHER);
            List<ItemStack> parts = index == 0 ? List.of(first, fabric, fabric, salis, fabric, fabric)
                    : List.of(first, new ItemStack(item("thaumcraft:void_ingot")), new ItemStack(item("thaumcraft:thaumium_ingot")), salis, fabric, last);
            test.assertTrue(infusion.matches(new InfusionRecipeInput(centre, parts), level), recipes[index] + ": the original centre and six components complete it");
            // A dyed robe is as good as a fresh one: the original compares no tags.
            ItemStack dyed = first.copy();
            if (dyed.getItem() instanceof ItemRobeArmor) ItemRobeArmor.setColor(dyed, 0xFF0000);
            List<ItemStack> withDyed = new ArrayList<>(parts);
            withDyed.set(0, dyed);
            test.assertTrue(infusion.matches(new InfusionRecipeInput(centre, withDyed), level), recipes[index] + ": a dyed robe still counts");
            ItemStack wornCentre = centre.copy();
            wornCentre.setDamageValue(1);
            test.assertTrue(!infusion.matches(new InfusionRecipeInput(wornCentre, parts), level), recipes[index] + ": a damaged centre is not the recipe's");
            ItemStack wornFirst = first.copy();
            wornFirst.setDamageValue(1);
            List<ItemStack> withWorn = new ArrayList<>(parts);
            withWorn.set(0, wornFirst);
            test.assertTrue(!infusion.matches(new InfusionRecipeInput(centre, withWorn), level), recipes[index] + ": the damaged " + firsts[index] + " is not the recipe's");
            List<ItemStack> wrongLast = new ArrayList<>(parts);
            wrongLast.set(5, new ItemStack(Items.STICK));
            test.assertTrue(!infusion.matches(new InfusionRecipeInput(centre, wrongLast), level), recipes[index] + ": a stick is no component");
            test.assertTrue(!infusion.matches(new InfusionRecipeInput(new ItemStack(Items.IRON_CHESTPLATE), parts), level), recipes[index] + ": iron armor is not the centre");
            test.assertTrue(!infusion.matches(new InfusionRecipeInput(centre, parts.subList(0, 5)), level), recipes[index] + ": five components are not enough");
        }
    }

    // ---- warp, discount and sight of a real wearer ------------------------------------------------------------------------

    private static void gear(GameTestHelper test, ServerPlayer player) {
        player.getInventory().clearContent();
        test.assertTrue(WarpGear.fromGear(player) == 0 && WandVisDiscount.percentagePoints(player, null) == 0, "a bare player has neither");
        for (int index = 0; index < IDS.length; index++) player.getInventory().setItem(20 + index, robe(index));
        test.assertTrue(WarpGear.fromGear(player) == 0 && WandVisDiscount.percentagePoints(player, null) == 0, "pieces in the pack count for nothing");
        for (int index = 0; index < IDS.length; index++) {
            player.setItemSlot(SLOTS[index], robe(index));
            test.assertTrue(WarpGear.fromGear(player) == 2 * (index + 1) && WandVisDiscount.percentagePoints(player, null) == 5 * (index + 1)
                            && WandVisDiscount.percentagePoints(player, Aspect.FIRE) == 5 * (index + 1),
                    IDS[index] + " worn: 2 warp and 5 % each: " + WarpGear.fromGear(player) + " " + WandVisDiscount.percentagePoints(player, null));
        }
        player.doTick();
        test.assertTrue(player.getArmorValue() == 16 && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0,
                "the set is 3 + 7 + 6 = 16 armor points: " + player.getArmorValue());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftEquipment.VOID_SWORD.get()));
        test.assertTrue(WarpGear.fromGear(player) == 7, "the void sword adds its own warp to the robes' six: " + WarpGear.fromGear(player));
        player.getInventory().clearContent();
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftWearables.GOGGLES.get()));
        player.setItemSlot(EquipmentSlot.CHEST, robe(1));
        test.assertTrue(WandVisDiscount.percentagePoints(player, null) == 10, "goggles 5 + a void robe 5: " + WandVisDiscount.percentagePoints(player, null));
        player.getInventory().clearContent();
    }

    // ---- mending over real ticks -------------------------------------------------------------------------------------------

    private static void mending(GameTestHelper test, ServerPlayer player) {
        player.getInventory().clearContent();
        ItemStack worn = robe(0), packed = robe(1), whole = robe(2), voidHelmet = new ItemStack(ThaumcraftEquipment.VOID_HELMET.get());
        worn.setDamageValue(50);
        packed.setDamageValue(50);
        voidHelmet.setDamageValue(50);
        player.setItemSlot(EquipmentSlot.HEAD, worn);
        player.getInventory().setItem(20, packed);
        player.getInventory().setItem(21, whole);
        player.getInventory().setItem(22, voidHelmet);
        // The level's entity loop adds the tick to tickCount and the connection calls doTick (the inventory ticks inside it); both are done by hand here.
        player.tickCount = 19;
        player.doTick();
        test.assertTrue(worn.getDamageValue() == 50 && packed.getDamageValue() == 50, "nothing mends off the twentieth tick: " + worn.getDamageValue() + " " + packed.getDamageValue());
        player.tickCount = 20;
        player.doTick();
        test.assertTrue(worn.getDamageValue() == 48, "a worn hood mended two points (inventory tick and armor tick): " + worn.getDamageValue());
        test.assertTrue(packed.getDamageValue() == 49, "a robe in the pack mended one: " + packed.getDamageValue());
        test.assertTrue(whole.getDamageValue() == 0, "a whole piece stays whole");
        test.assertTrue(voidHelmet.getDamageValue() == 49, "a plain void helmet in the pack mends one too, by the same rule: " + voidHelmet.getDamageValue());
        // A creative player's gear is not mended.
        player.setGameMode(GameType.CREATIVE);
        player.tickCount = 40;
        player.doTick();
        test.assertTrue(worn.getDamageValue() == 48 && packed.getDamageValue() == 49, "in creative nothing mends: " + worn.getDamageValue() + " " + packed.getDamageValue());
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
    }

    // ---- the ward ------------------------------------------------------------------------------------------------------------

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

    private static DamageSource taint(ServerLevel level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(PotionFluxTaint.TAINT));
    }

    private static void wardMobs(GameTestHelper test, ServerLevel level, BlockPos origin, List<Mob> spawned) {
        Vec3 spot = Vec3.atBottomCenterOf(origin).add(0, 0, 4);
        DamageSource magic = level.damageSources().magic(), indirect = level.damageSources().indirectMagic(null, null), taint = taint(level), generic = level.damageSources().generic();
        test.assertTrue(magic.is(DamageTypeTags.BYPASSES_ARMOR) && generic.is(DamageTypeTags.BYPASSES_ARMOR) && taint.is(DamageTypeTags.BYPASSES_ARMOR),
                "vanilla armor takes none of these, so the ward is the only protection");
        Mob pig = pig(level, spawned, spot);
        // The whole set: 20 magic damage becomes 20 * 19 / 35.
        for (int index = 0; index < IDS.length; index++) pig.setItemSlot(SLOTS[index], robe(index));
        for (var source : List.of(magic, indirect, taint)) {
            for (int index = 0; index < IDS.length; index++) pig.getItemBySlot(SLOTS[index]).setDamageValue(0);
            pig.setHealth(500);
            pig.invulnerableTime = 0;
            test.assertTrue(pig.hurt(source, 20F), source.type().msgId() + " lands");
            double lost = 500 - pig.getHealth();
            test.assertTrue(Math.abs(lost - 20 * 19 / 35.0) < 1e-3, source.type().msgId() + ": 20 damage becomes 20 * 19 / 35 = 10.857: " + lost);
            test.assertTrue(pig.getItemBySlot(SLOTS[0]).getDamageValue() == 1 && pig.getItemBySlot(SLOTS[1]).getDamageValue() == 4 && pig.getItemBySlot(SLOTS[2]).getDamageValue() == 3,
                    source.type().msgId() + ": hood 1, robe 4, leggings 3 points of wear: " + pig.getItemBySlot(SLOTS[0]).getDamageValue() + " " + pig.getItemBySlot(SLOTS[1]).getDamageValue() + " " + pig.getItemBySlot(SLOTS[2]).getDamageValue());
        }
        // Ordinary damage is nobody's ward.
        for (int index = 0; index < IDS.length; index++) pig.getItemBySlot(SLOTS[index]).setDamageValue(0);
        pig.setHealth(500);
        pig.invulnerableTime = 0;
        test.assertTrue(pig.hurt(generic, 20F), "generic damage lands");
        test.assertTrue(Math.abs(500 - pig.getHealth() - 20) < 1e-3, "generic damage is not warded: " + (500 - pig.getHealth()));
        test.assertTrue(pig.getItemBySlot(SLOTS[0]).getDamageValue() == 0 && pig.getItemBySlot(SLOTS[1]).getDamageValue() == 0 && pig.getItemBySlot(SLOTS[2]).getDamageValue() == 0, "nor does it wear the pieces");
        // One piece: the robe alone, a fifth.
        Mob lone = pig(level, spawned, spot.add(2, 0, 0));
        lone.setItemSlot(EquipmentSlot.CHEST, robe(1));
        lone.invulnerableTime = 0;
        test.assertTrue(lone.hurt(magic, 20F), "magic lands on the robe alone");
        test.assertTrue(Math.abs(500 - lone.getHealth() - 16) < 1e-3 && lone.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 4,
                "the robe alone takes a fifth: 20 becomes 16 and it wears 4: " + (500 - lone.getHealth()) + " " + lone.getItemBySlot(EquipmentSlot.CHEST).getDamageValue());
        // A small blow still wears a point; a ward on the head alone.
        Mob small = pig(level, spawned, spot.add(4, 0, 0));
        small.setItemSlot(EquipmentSlot.HEAD, robe(0));
        small.invulnerableTime = 0;
        test.assertTrue(small.hurt(magic, 2F), "a small magic blow lands");
        test.assertTrue(Math.abs(500 - small.getHealth() - 2 * 32 / 35.0) < 1e-3 && small.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() == 1,
                "2 becomes 2 * 32 / 35 and the hood wears one point: " + (500 - small.getHealth()));
        // Void armor and the enchanted robes are no ward.
        Mob other = pig(level, spawned, spot.add(6, 0, 0));
        other.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftEquipment.VOID_HELMET.get()));
        other.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ThaumcraftWearables.ROBE_CHESTPLATE.get()));
        other.invulnerableTime = 0;
        test.assertTrue(other.hurt(magic, 20F) && Math.abs(500 - other.getHealth() - 20) < 1e-3, "void armor and enchanted robes ward nothing: " + (500 - other.getHealth()));
        // Pieces in a mob's hand or a player's pack are not worn.
        Mob packed = pig(level, spawned, spot.add(8, 0, 0));
        packed.setItemInHand(InteractionHand.MAIN_HAND, robe(1));
        packed.invulnerableTime = 0;
        test.assertTrue(packed.hurt(magic, 20F) && Math.abs(500 - packed.getHealth() - 20) < 1e-3, "a robe in the hand wards nothing: " + (500 - packed.getHealth()));
    }

    private static void wardPlayer(GameTestHelper test, ServerLevel level, ServerPlayer player) {
        DamageSource magic = level.damageSources().magic(), generic = level.damageSources().generic();
        player.getInventory().clearContent();
        for (int index = 0; index < IDS.length; index++) player.setItemSlot(SLOTS[index], robe(index));
        player.setHealth(20F);
        player.invulnerableTime = 0;
        test.assertTrue(player.hurt(magic, 20F), "magic damage lands on a real player");
        test.assertTrue(Math.abs(20 - player.getHealth() - 20 * 19 / 35.0) < 1e-3, "the player loses 10.857 of 20: " + (20 - player.getHealth()));
        test.assertTrue(player.getItemBySlot(SLOTS[0]).getDamageValue() == 1 && player.getItemBySlot(SLOTS[1]).getDamageValue() == 4 && player.getItemBySlot(SLOTS[2]).getDamageValue() == 3,
                "and wears 1, 4 and 3 points: " + player.getItemBySlot(SLOTS[0]).getDamageValue() + " " + player.getItemBySlot(SLOTS[1]).getDamageValue() + " " + player.getItemBySlot(SLOTS[2]).getDamageValue());
        // A creative player's gear takes the share off the damage but does not wear.
        for (int index = 0; index < IDS.length; index++) player.getItemBySlot(SLOTS[index]).setDamageValue(0);
        player.setGameMode(GameType.CREATIVE);
        test.assertTrue(Math.abs(MagicWard.apply(player, magic, 20F) - 20 * 19 / 35.0) < 1e-4, "a creative wearer still gets the ward");
        test.assertTrue(player.getItemBySlot(SLOTS[0]).getDamageValue() == 0 && player.getItemBySlot(SLOTS[1]).getDamageValue() == 0 && player.getItemBySlot(SLOTS[2]).getDamageValue() == 0,
                "but nothing wears in creative");
        player.setGameMode(GameType.SURVIVAL);
        // A source that is not magic gets nothing from the same call.
        test.assertTrue(MagicWard.apply(player, generic, 20F) == 20F && MagicWard.apply(player, magic, 0F) == 0F, "the ward answers magic damage only, and damage");
        player.getInventory().clearContent();
    }

    // ---- dye and wash -------------------------------------------------------------------------------------------------------------

    private static void dyeAndWash(GameTestHelper test, ServerLevel level, ServerPlayer player, BlockPos cauldron) {
        var registries = level.registryAccess();
        var holder = level.getRecipeManager().byKey(id("robe_dye")).orElseThrow();
        test.assertTrue(holder.value() instanceof RecipesRobeArmorDyes, "the one registered dye recipe");
        var recipe = (RecipesRobeArmorDyes) holder.value();
        for (int index = 0; index < IDS.length; index++) {
            ItemStack source = robe(index);
            source.setDamageValue(5);
            source.set(DataComponents.CUSTOM_NAME, Component.literal("robe-input"));
            var red = new ItemStack(Items.RED_DYE, 4);
            var input = CraftingInput.of(2, 2, List.of(source, red, ItemStack.EMPTY, ItemStack.EMPTY));
            test.assertTrue(recipe.matches(input, level), IDS[index] + " and a dye match the robe dye recipe");
            ItemStack dyed = recipe.assemble(input, registries);
            test.assertTrue(dyed.is(source.getItem()) && dyed.getCount() == 1 && ItemRobeArmor.color(dyed) == 0x8C3960 && dyed.getDamageValue() == 5
                            && dyed.get(DataComponents.CUSTOM_NAME).equals(Component.literal("robe-input")),
                    IDS[index] + ": the default purple plus red is 8C3960, count and damage and name kept: " + Integer.toHexString(ItemRobeArmor.color(dyed)));
            test.assertTrue(!new ArmorDyeRecipe(CraftingBookCategory.EQUIPMENT).matches(input, level), "the vanilla palette cannot compete");
            ItemStack twoRobes = new ItemStack(ThaumcraftWearables.ROBE_CHESTPLATE.get());
            test.assertTrue(!recipe.matches(CraftingInput.of(2, 2, List.of(source, twoRobes, red, ItemStack.EMPTY)), level), "a void robe and an enchanted robe do not dye together");
            test.assertTrue(!recipe.matches(CraftingInput.of(2, 2, List.of(source, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)), level), "no dye, no recipe");
            test.assertTrue(!recipe.matches(CraftingInput.of(2, 2, List.of(new ItemStack(ThaumcraftEquipment.VOID_HELMET.get()), red, ItemStack.EMPTY, ItemStack.EMPTY)), level), "plain void armor is not a robe");
        }
        // The real 2 x 2 crafting grid of the player: a void hood and a dye, the result taken, one of each consumed.
        var menu = player.inventoryMenu;
        menu.getSlot(1).set(robe(0));
        menu.getSlot(2).set(new ItemStack(Items.RED_DYE, 3));
        test.assertTrue(menu.getSlot(0).getItem().is(ThaumcraftEquipment.VOID_ROBE_HELMET.get()) && ItemRobeArmor.color(menu.getSlot(0).getItem()) == 0x8C3960
                        && level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 2, List.of(robe(0), new ItemStack(Items.RED_DYE), ItemStack.EMPTY, ItemStack.EMPTY)), level)
                        .orElseThrow().id().equals(id("robe_dye")), "the player's own grid previews the dyed hood by the robe recipe");
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        ItemStack taken = menu.getCarried();
        test.assertTrue(taken.is(ThaumcraftEquipment.VOID_ROBE_HELMET.get()) && ItemRobeArmor.color(taken) == 0x8C3960 && menu.getSlot(1).getItem().isEmpty() && menu.getSlot(2).getItem().getCount() == 2,
                "taking it consumes the hood and one dye, not the stack: " + menu.getSlot(2).getItem().getCount());
        menu.setCarried(ItemStack.EMPTY);
        player.inventoryMenu.clearCraftingContent();

        // The wash in a water cauldron, through the real use path.
        ItemStack dyedRobe = robe(1);
        ItemRobeArmor.setColor(dyedRobe, 0x123456);
        player.setItemInHand(InteractionHand.MAIN_HAND, dyedRobe);
        var hit = new BlockHitResult(Vec3.atCenterOf(cauldron), Direction.UP, cauldron, false);
        for (var wrong : List.of(Blocks.CAULDRON.defaultBlockState(), Blocks.LAVA_CAULDRON.defaultBlockState(), Blocks.STONE.defaultBlockState())) {
            level.setBlock(cauldron, wrong, 3);
            var result = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            test.assertTrue(!result.consumesAction() && level.getBlockState(cauldron).equals(wrong) && ItemRobeArmor.color(player.getMainHandItem()) == 0x123456,
                    "an empty or foreign cauldron washes nothing: " + wrong);
        }
        level.setBlock(cauldron, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), 3);
        var washed = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        test.assertTrue(washed.consumesAction() && ItemRobeArmor.color(player.getMainHandItem()) == 0x6A3880 && level.getBlockState(cauldron).getValue(LayeredCauldronBlock.LEVEL) == 2,
                "a wash restores the default purple and lowers the water one level: " + Integer.toHexString(ItemRobeArmor.color(player.getMainHandItem())));
        var again = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        test.assertTrue(again.consumesAction() && level.getBlockState(cauldron).getValue(LayeredCauldronBlock.LEVEL) == 1 && ItemRobeArmor.color(player.getMainHandItem()) == 0x6A3880,
                "an undyed robe still costs a level, as in the original");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }
}
