package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.IWarpingGear;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.lib.WarpGear;

/**
 * The nine Void metal tools and armor on real content: their numbers (uses, speed, damage, protection, enchantability, rarity, the
 * repair quirk of the original that names the Primal Charm for tools and the void ingot for armor), the nine recipes, and a REAL
 * connected player: a damaged piece mends one point on every twentieth tick of the player (a worn piece two, a creative player's none,
 * an ordinary Thaumium tool none), a hit withers a real mob for three seconds with a sword and four with the other tools (never a player
 * while player-versus-player is off), and the warp of the gear in the hand and on the body is summed. Players, mobs and tools are explicit
 * inputs; the numbers, the mending, the wither and the warp are never supplied. NOT a client check (the icons and the armor layers are
 * unseen).
 */
public final class VoidGearRuntimeChecks {
    private record Tool(String id, Item item, float damage, int uses, int enchant, int wither) {}

    private VoidGearRuntimeChecks() {}

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static List<Tool> tools() {
        // Legacy damage: sword 4, pickaxe 2, axe 3, shovel 1, no damage for the hoe, plus the material's 3 (the player's own 1 is extra).
        return List.of(new Tool("void_sword", ThaumcraftEquipment.VOID_SWORD.get(), 7, 150, 10, 60),
                new Tool("void_pickaxe", ThaumcraftEquipment.VOID_PICKAXE.get(), 5, 150, 10, 80),
                new Tool("void_axe", ThaumcraftEquipment.VOID_AXE.get(), 6, 150, 10, 80),
                new Tool("void_shovel", ThaumcraftEquipment.VOID_SHOVEL.get(), 4, 150, 10, 80),
                new Tool("void_hoe", ThaumcraftEquipment.VOID_HOE.get(), 0, 150, 5, 80));
    }

    private static List<ArmorItem> armor() {
        return List.of(ThaumcraftEquipment.VOID_HELMET.get(), ThaumcraftEquipment.VOID_CHESTPLATE.get(), ThaumcraftEquipment.VOID_LEGGINGS.get(), ThaumcraftEquipment.VOID_BOOTS.get());
    }

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        numbers(test);
        recipes(test, level);
        var attacker = new Actor(level, "void-a-");
        var victim = new Actor(level, "void-v-");
        boolean pvp = level.getServer().isPvpAllowed();
        try {
            test.assertTrue(attacker.player.level() == level && level.getServer().getPlayerList().getPlayer(attacker.player.getUUID()) == attacker.player, "a real connected player carries the gear");
            warp(test, attacker.player);
            mendingAndWither(test, level, attacker, victim, pvp);
        } catch (RuntimeException failure) {
            level.getServer().setPvpAllowed(pvp);
            attacker.close(level);
            victim.close(level);
            throw failure;
        }
    }

    private static final class Actor {
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        final EmbeddedChannel channel = new EmbeddedChannel(connection);
        final ServerPlayer player;

        Actor(ServerLevel level, String prefix) {
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), prefix + UUID.randomUUID().toString().substring(0, 6)), ClientInformation.createDefault());
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.setGameMode(GameType.SURVIVAL);
            player.setNoGravity(true);
        }

        void close(ServerLevel level) {
            try {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
            } finally { player.discard(); channel.finishAndReleaseAll(); }
        }
    }

    // ---- numbers --------------------------------------------------------------------------------------------------------

    private static double attackDamage(ItemStack stack) {
        double[] total = {0};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> { if (attribute.is(Attributes.ATTACK_DAMAGE)) total[0] += modifier.amount(); });
        return total[0];
    }

    private static void numbers(GameTestHelper test) {
        ItemStack charm = new ItemStack(item("thaumcraft:primal_charm"));
        ItemStack ingot = new ItemStack(item("thaumcraft:void_ingot"));
        for (Tool tool : tools()) {
            ItemStack stack = new ItemStack(tool.item());
            test.assertTrue(stack.getMaxDamage() == tool.uses() && stack.getRarity() == Rarity.UNCOMMON && tool.item().getEnchantmentValue() == tool.enchant(),
                    tool.id() + ": 150 uses, uncommon, enchantability " + tool.enchant() + ": " + stack.getMaxDamage() + " " + tool.item().getEnchantmentValue());
            test.assertTrue(attackDamage(stack) == tool.damage(), tool.id() + ": attack damage " + tool.damage() + ", saw " + attackDamage(stack));
            test.assertTrue(tool.item().isValidRepairItem(stack, charm) && !tool.item().isValidRepairItem(stack, ingot),
                    tool.id() + ": the original's repair check names the Primal Charm, not the void ingot");
            test.assertTrue(tool.item() instanceof IWarpingGear, tool.id() + " is warping gear");
        }
        ItemStack pickaxe = new ItemStack(ThaumcraftEquipment.VOID_PICKAXE.get());
        test.assertTrue(pickaxe.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 8.0F && pickaxe.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),
                "the void pickaxe digs at speed 8 and takes obsidian");
        int[] defense = {3, 7, 6, 3}, uses = {110, 160, 150, 130};
        List<ArmorItem> armor = armor();
        for (int index = 0; index < armor.size(); index++) {
            ArmorItem piece = armor.get(index);
            ItemStack stack = new ItemStack(piece);
            test.assertTrue(piece.getDefense() == defense[index] && stack.getMaxDamage() == uses[index] && piece.getEnchantmentValue() == 10 && stack.getRarity() == Rarity.UNCOMMON
                            && piece.getToughness() == 0,
                    "void armor piece " + index + ": protection " + defense[index] + ", " + uses[index] + " uses, enchantability 10");
            test.assertTrue(piece.isValidRepairItem(stack, ingot) && !piece.isValidRepairItem(stack, charm), "armor mends with the void ingot");
            test.assertTrue(piece instanceof IWarpingGear, "armor is warping gear");
        }
    }

    // ---- recipes --------------------------------------------------------------------------------------------------------

    private static void recipes(GameTestHelper test, ServerLevel level) {
        ItemStack ingot = new ItemStack(item("thaumcraft:void_ingot")), stick = new ItemStack(net.minecraft.world.item.Items.STICK);
        record Shape(String id, int width, int height, List<ItemStack> grid) {}
        ItemStack e = ItemStack.EMPTY;
        List<Shape> shapes = List.of(
                new Shape("void_helmet", 3, 2, List.of(ingot, ingot, ingot, ingot, e, ingot)),
                new Shape("void_chestplate", 3, 3, List.of(ingot, e, ingot, ingot, ingot, ingot, ingot, ingot, ingot)),
                new Shape("void_leggings", 3, 3, List.of(ingot, ingot, ingot, ingot, e, ingot, ingot, e, ingot)),
                new Shape("void_boots", 3, 2, List.of(ingot, e, ingot, ingot, e, ingot)),
                new Shape("void_shovel", 1, 3, List.of(ingot, stick, stick)),
                new Shape("void_pickaxe", 3, 3, List.of(ingot, ingot, ingot, e, stick, e, e, stick, e)),
                new Shape("void_axe", 2, 3, List.of(ingot, ingot, stick, ingot, stick, e)),
                new Shape("void_hoe", 2, 3, List.of(ingot, ingot, stick, e, stick, e)),
                new Shape("void_sword", 1, 3, List.of(ingot, ingot, stick)));
        for (Shape shape : shapes) {
            var recipe = (CraftingRecipe) level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", shape.id())).orElseThrow(
                    () -> new AssertionError("no recipe for " + shape.id())).value();
            var input = CraftingInput.of(shape.width(), shape.height(), shape.grid());
            test.assertTrue(recipe.matches(input, level) && recipe.assemble(input, level.registryAccess()).is(item("thaumcraft:" + shape.id())), shape.id() + ": void ingots (and sticks) make it");
            List<ItemStack> wrong = new ArrayList<>();
            for (ItemStack stack : shape.grid()) wrong.add(stack == ingot ? new ItemStack(item("thaumcraft:thaumium_ingot")) : stack);
            test.assertTrue(!recipe.matches(CraftingInput.of(shape.width(), shape.height(), wrong), level), shape.id() + ": thaumium ingots do not");
        }
    }

    // ---- warp ---------------------------------------------------------------------------------------------------------------

    private static void warp(GameTestHelper test, ServerPlayer player) {
        player.getInventory().clearContent();
        test.assertTrue(WarpGear.fromGear(player) == 0, "an empty player carries no warp");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftEquipment.VOID_SWORD.get()));
        test.assertTrue(WarpGear.fromGear(player) == 1, "a void sword in the hand is one warp");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ThaumcraftEquipment.VOID_PICKAXE.get()));
        player.getInventory().setItem(10, new ItemStack(ThaumcraftEquipment.VOID_AXE.get()));
        test.assertTrue(WarpGear.fromGear(player) == 1, "neither the off hand nor the pack counts");
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        List<ArmorItem> armor = armor();
        for (int index = 0; index < 4; index++) player.setItemSlot(slots[index], new ItemStack(armor.get(index)));
        test.assertTrue(WarpGear.fromGear(player) == 5, "the sword and four armor pieces make five: " + WarpGear.fromGear(player));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftEquipment.THAUMIUM_HELMET.get()));
        test.assertTrue(WarpGear.fromGear(player) == 4, "a Thaumium helmet adds nothing");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftEquipment.THAUMIUM_SWORD.get()));
        test.assertTrue(WarpGear.fromGear(player) == 3, "nor does a Thaumium sword");
        player.getInventory().clearContent();
    }

    // ---- mending and the wither, over real ticks ----------------------------------------------------------------------------

    private static Mob pig(ServerLevel level, GameTestHelper test, int dx) {
        Mob pig = EntityType.PIG.create(level);
        pig.moveTo(Vec3.atBottomCenterOf(test.absolutePos(new net.minecraft.core.BlockPos(2 + dx, 2, 5))));
        pig.setNoAi(true);
        pig.setNoGravity(true);
        pig.setPersistenceRequired();
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);
        pig.setHealth(500);
        test.assertTrue(level.addFreshEntity(pig), "a real pig is admitted");
        return pig;
    }

    private static ItemStack damaged(Item item, int damage) {
        ItemStack stack = new ItemStack(item);
        stack.setDamageValue(damage);
        return stack;
    }

    private static void mendingAndWither(GameTestHelper test, ServerLevel level, Actor attacker, Actor victim, boolean pvp) {
        ServerPlayer player = attacker.player;
        player.getInventory().clearContent();
        ItemStack mendedTool = damaged(ThaumcraftEquipment.VOID_PICKAXE.get(), 50);
        ItemStack worn = damaged(ThaumcraftEquipment.VOID_HELMET.get(), 50);
        ItemStack packed = damaged(ThaumcraftEquipment.VOID_BOOTS.get(), 50);
        ItemStack plain = damaged(ThaumcraftEquipment.THAUMIUM_PICKAXE.get(), 50);
        ItemStack whole = new ItemStack(ThaumcraftEquipment.VOID_SWORD.get());
        player.getInventory().setItem(20, mendedTool);
        player.getInventory().setItem(21, packed);
        player.getInventory().setItem(22, plain);
        player.getInventory().setItem(23, whole);
        player.setItemSlot(EquipmentSlot.HEAD, worn);
        player.tickCount = 15;
        long start = test.getTick();
        // A player that no network handler ticks does not tick its inventory either; doTick is what the connection calls.
        test.onEachTick(player::doTick);
        List<Mob> pigs = new ArrayList<>();
        for (int index = 0; index < 5; index++) pigs.add(pig(level, test, index));
        test.runAtTickTime(start + 14, () -> guarded(level, attacker, victim, pvp, pigs, () -> {
            test.assertTrue(player.tickCount >= 20 && player.tickCount < 40, "the player passed tick 20 once: " + player.tickCount);
            test.assertTrue(mendedTool.getDamageValue() == 49, "a void pickaxe in the pack mended one point: " + mendedTool.getDamageValue());
            test.assertTrue(packed.getDamageValue() == 49, "so did void boots in the pack: " + packed.getDamageValue());
            test.assertTrue(worn.getDamageValue() == 48, "a worn void helmet mended two (inventory tick and armor tick): " + worn.getDamageValue());
            test.assertTrue(plain.getDamageValue() == 50, "a Thaumium pickaxe mends nothing: " + plain.getDamageValue());
            test.assertTrue(whole.getDamageValue() == 0, "an undamaged piece stays whole");
            // A creative player's pieces are not mended (damageItem does nothing for creative players).
            player.setGameMode(GameType.CREATIVE);
            player.tickCount = 35;
            test.runAtTickTime(start + 28, () -> guarded(level, attacker, victim, pvp, pigs, () -> {
                test.assertTrue(mendedTool.getDamageValue() == 49 && worn.getDamageValue() == 48, "in creative nothing mends: " + mendedTool.getDamageValue() + " " + worn.getDamageValue());
                player.setGameMode(GameType.SURVIVAL);
                wither(test, level, attacker, victim, pvp, pigs, start + 28);
            }));
        }));
    }

    private static void guarded(ServerLevel level, Actor attacker, Actor victim, boolean pvp, List<Mob> pigs, Runnable body) {
        try {
            body.run();
        } catch (RuntimeException failure) {
            level.getServer().setPvpAllowed(pvp);
            pigs.forEach(Mob::discard);
            attacker.close(level);
            victim.close(level);
            throw failure;
        }
    }

    private static void wither(GameTestHelper test, ServerLevel level, Actor attacker, Actor victim, boolean pvp, List<Mob> pigs, long after) {
        ServerPlayer player = attacker.player;
        List<Tool> tools = tools();
        // One tool per few ticks: swap, wait for the attack cooldown to refill, hit a fresh pig.
        for (int index = 0; index < tools.size(); index++) {
            Tool tool = tools.get(index);
            Mob pig = pigs.get(index);
            test.runAtTickTime(after + 4 + 12L * index, () -> guarded(level, attacker, victim, pvp, pigs, () -> player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool.item()))));
            test.runAtTickTime(after + 12 + 12L * index, () -> guarded(level, attacker, victim, pvp, pigs, () -> {
                test.assertTrue(pig.getEffect(MobEffects.WITHER) == null, tool.id() + ": the mob is clean before the hit");
                player.attack(pig);
                var effect = pig.getEffect(MobEffects.WITHER);
                test.assertTrue(effect != null && effect.getDuration() == tool.wither() && effect.getAmplifier() == 0,
                        tool.id() + " withers the mob for " + tool.wither() + " ticks: " + effect);
            }));
        }
        long players = after + 12 + 12L * tools.size() + 4;
        // Another player: with player-versus-player on a hit withers, with it off it does not (the sword and a tool alike).
        test.runAtTickTime(players, () -> guarded(level, attacker, victim, pvp, pigs, () -> {
            ServerPlayer target = victim.player;
            target.setPos(Vec3.atBottomCenterOf(test.absolutePos(new net.minecraft.core.BlockPos(2, 2, 8))));
            level.getServer().setPvpAllowed(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftEquipment.VOID_SWORD.get()));
            target.setGameMode(GameType.SURVIVAL);
            player.attack(target);
            test.assertTrue(target.hasEffect(MobEffects.WITHER), "with player-versus-player on a void sword withers a player");
            target.removeEffect(MobEffects.WITHER);
            level.getServer().setPvpAllowed(false);
            // A hit between players does not even land with player-versus-player off, so the item's own hook is asked directly.
            new ItemStack(ThaumcraftEquipment.VOID_SWORD.get()).hurtEnemy(target, player);
            test.assertTrue(!target.hasEffect(MobEffects.WITHER), "with it off a void sword does not");
            test.runAtTickTime(players + 12, () -> guarded(level, attacker, victim, pvp, pigs, () -> {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftEquipment.VOID_AXE.get()));
                new ItemStack(ThaumcraftEquipment.VOID_AXE.get()).hurtEnemy(target, player);
                test.assertTrue(!target.hasEffect(MobEffects.WITHER), "nor does a void axe");
                new ItemStack(ThaumcraftEquipment.VOID_HOE.get()).hurtEnemy(target, player);
                test.assertTrue(!target.hasEffect(MobEffects.WITHER), "nor a void hoe");
                level.getServer().setPvpAllowed(true);
                test.runAtTickTime(players + 24, () -> guarded(level, attacker, victim, pvp, pigs, () -> {
                    player.attack(target);
                    test.assertTrue(target.hasEffect(MobEffects.WITHER) && target.getEffect(MobEffects.WITHER).getDuration() == 80, "and with it on again the axe withers a player for four seconds too");
                    // A mob hitting a player with a void sword: the sword's own rule only spares player-on-player hits.
                    level.getServer().setPvpAllowed(false);
                    target.removeEffect(MobEffects.WITHER);
                    thaumcraft.common.items.VoidMetal.wither(target, pigs.get(0), thaumcraft.common.items.VoidMetal.SWORD_WITHER, true);
                    test.assertTrue(target.hasEffect(MobEffects.WITHER), "a sword in a mob's hand still withers a player with player-versus-player off");
                    level.getServer().setPvpAllowed(pvp);
                    pigs.forEach(Mob::discard);
                    attacker.close(level);
                    victim.close(level);
                    test.succeed();
                }));
            }));
        }));
    }
}
