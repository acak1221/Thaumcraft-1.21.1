package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.equipment.ElementalSwordRules;
import thaumcraft.common.mixin.LivingEntityAttackStrengthAccess;
import thaumcraft.common.mixin.ServerGamePacketListenerFloatAccess;

/**
 * The Sword of the Zephyr on real content: its numbers and repair, the infusion, the hold (a real connected player's lift and slowed fall,
 * the entities within 2.5 blocks blown away and the ones beyond left, a use worn on the first tick and every twentieth, the server's hover
 * count reset) and the sweep of a real player's blow (the mobs round the target struck for the weapon's full damage and a use each, the
 * target struck as hard as ever, the player's own golem and tame wolf, a mob out of reach, an armor stand and a thaumium sword's blow
 * leaving the rest alone, and the sweep ending when the sword breaks). NOT the client side of the lift, the smoke or the sounds.
 */
public final class ElementalSwordRuntimeChecks {
    private ElementalSwordRuntimeChecks() {}

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        numbers(test);
        recipe(test, level);
        // High above the small GameTest structure, as the other world-shaping checks do.
        BlockPos origin = test.absolutePos(new BlockPos(3, 64, 3));
        // The sweep happens thirty blocks further along z, so the chunks of the sweep room belong to
        // the fixture as well; a room whose chunk is not loaded does not hold its actors.
        for (int x = (origin.getX() - 18) >> 4; x <= ((origin.getX() + 18) >> 4); x++) {
            for (int z = (origin.getZ() - 18) >> 4; z <= ((origin.getZ() + 40) >> 4); z++) level.getChunk(x, z);
        }
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "zephyr-" + UUID.randomUUID().toString().substring(0, 6)), ClientInformation.createDefault());
        level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.setNoGravity(true);
        player.setGameMode(GameType.SURVIVAL);
        List<Entity> spawned = new java.util.ArrayList<>();
        try {
            test.assertTrue(player.level() == level && player.connection != null, "a real connected player holds the sword");
            hold(test, level, player, origin, spawned);
            sweep(test, level, player, origin, spawned);
        } finally {
            spawned.forEach(Entity::discard);
            try {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
            } finally {
                player.discard();
                channel.finishAndReleaseAll();
            }
        }
        test.succeed();
    }

    private static ItemStack sword() { return new ItemStack(ThaumcraftEquipment.ELEMENTAL_SWORD.get()); }

    private static void numbers(GameTestHelper test) {
        ItemStack sword = sword();
        test.assertTrue(sword.getMaxDamage() == 1500 && sword.getItem().getEnchantmentValue() == 18 && sword.getRarity() == Rarity.RARE,
                "1500 uses, enchantability 18, rare: " + sword.getMaxDamage() + " " + sword.getItem().getEnchantmentValue() + " " + sword.getRarity());
        double[] stats = {0, 0};
        sword.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE)) stats[0] += modifier.amount();
            if (attribute.is(Attributes.ATTACK_SPEED)) stats[1] += modifier.amount();
        });
        test.assertTrue(stats[0] == 7.0 && Math.abs(stats[1] + 2.4) < 1e-6, "legacy sword 4 + material 3 = 7 damage, -2.4 speed: " + stats[0] + " " + stats[1]);
        test.assertTrue(sword.getItem().isValidRepairItem(sword, new ItemStack(item("thaumcraft:thaumium_ingot"))) && !sword.getItem().isValidRepairItem(sword, new ItemStack(Items.IRON_INGOT)),
                "repaired with the thaumium ingot alone");
        test.assertTrue(sword.is(ItemTags.SWORDS) && sword.getUseAnimation() == UseAnim.BOW && sword.getUseDuration(null) == 72000,
                "a sword by tag held like a bow for the vanilla duration, not as a shield: " + sword.getUseAnimation());
    }

    private static void recipe(GameTestHelper test, ServerLevel level) {
        var infusion = (InfusionStackRecipe) level.getRecipeManager().byKey(id("infusion/elemental_sword")).orElseThrow(() -> new AssertionError("no recipe")).value();
        test.assertTrue(infusion.research().equals("ELEMENTALSWORD") && infusion.instability() == 1 && infusion.result().is(item("thaumcraft:elemental_sword"))
                        && infusion.result().getCount() == 1 && infusion.aspects().equals(Map.of("aer", 8, "motus", 8, "potentia", 8)) && infusion.components().size() == 4,
                "the infusion: ELEMENTALSWORD, instability 1, 8 air + 8 motion + 8 energy, four components: " + infusion.aspects());
        ItemStack centre = new ItemStack(item("thaumcraft:thaumium_sword"));
        List<ItemStack> parts = List.of(new ItemStack(item("thaumcraft:air_shard")), new ItemStack(item("thaumcraft:air_shard")),
                new ItemStack(Items.DIAMOND), new ItemStack(item("thaumcraft:greatwood_log")));
        test.assertTrue(infusion.matches(new InfusionRecipeInput(centre, parts), level), "a thaumium sword, two air shards, a diamond and a greatwood log complete it");
        test.assertTrue(!infusion.matches(new InfusionRecipeInput(new ItemStack(Items.IRON_SWORD), parts), level), "an iron sword is not the centre");
        test.assertTrue(!infusion.matches(new InfusionRecipeInput(centre, List.of(parts.get(0), new ItemStack(item("thaumcraft:fire_shard")), parts.get(2), parts.get(3))), level), "a fire shard is not the second air shard");
    }

    private static <T extends Entity> T put(ServerLevel level, List<Entity> spawned, T entity, Vec3 at) {
        entity.moveTo(at.x, at.y, at.z, 0, 0);
        entity.setNoGravity(true);
        if (entity instanceof Mob mob) mob.setNoAi(true);
        if (!level.addFreshEntity(entity)) throw new AssertionError("the fixture entity was not admitted: " + entity);
        spawned.add(entity);
        return entity;
    }

    /** A second real connected player, so that the hold can be seen to leave players alone. */
    private record Visitor(ServerPlayer player, EmbeddedChannel channel) {
        static Visitor join(ServerLevel level, Vec3 at) {
            var connection = new Connection(PacketFlow.SERVERBOUND);
            var channel = new EmbeddedChannel(connection);
            var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "visitor-" + UUID.randomUUID().toString().substring(0, 6)), ClientInformation.createDefault());
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.setNoGravity(true);
            player.moveTo(at.x, at.y, at.z, 0, 0);
            player.setDeltaMovement(Vec3.ZERO);
            return new Visitor(player, channel);
        }

        void leave(ServerLevel level) {
            try {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
            } finally {
                player.discard();
                channel.finishAndReleaseAll();
            }
        }
    }

    // ---- the hold -------------------------------------------------------------------------------------------------------

    private static void hold(GameTestHelper test, ServerLevel level, ServerPlayer player, BlockPos origin, List<Entity> spawned) {
        Vec3 spot = Vec3.atBottomCenterOf(origin);
        player.setPos(spot);
        ItemStack sword = sword();
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        Cow near = put(level, spawned, EntityType.COW.create(level), spot.add(1.5, 0, 0));
        Cow far = put(level, spawned, EntityType.COW.create(level), spot.add(0, 0, 4.0));
        ItemEntity loose = put(level, spawned, new ItemEntity(level, spot.x - 1.2, spot.y, spot.z, new ItemStack(Items.STICK)), spot.add(-1.2, 0, 0));
        ArmorStand stand = put(level, spawned, EntityType.ARMOR_STAND.create(level), spot.add(0, 0, -1.4));
        Vec3 nearBefore = near.getDeltaMovement(), farBefore = far.getDeltaMovement();
        // A falling user is slowed and lifted, and the hover count is wiped.
        player.setDeltaMovement(0, -1.0, 0);
        player.fallDistance = 6.0F;
        var listener = (ServerGamePacketListenerFloatAccess) player.connection;
        listener.thaumcraft$setAboveGroundTickCount(50); // a user who has been hovering for 50 ticks
        sword.getItem().onUseTick(level, player, sword, ElementalSwordRules.USE_DURATION);
        double expected = -1.0 / ElementalSwordRules.DAMPING + ElementalSwordRules.LIFT;
        test.assertTrue(Math.abs(player.getDeltaMovement().y - expected) < 1e-6 && Math.abs(player.fallDistance - 6.0F / ElementalSwordRules.DAMPING) < 1e-5,
                "a fall of -1.0 becomes " + expected + " and the fall distance a 1.2th: " + player.getDeltaMovement().y + ", " + player.fallDistance);
        test.assertTrue(listener.thaumcraft$getAboveGroundTickCount() == 0, "the server's hover count is wiped");
        Vec3 pushed = near.getDeltaMovement().subtract(nearBefore);
        test.assertTrue(Math.abs(pushed.x - 1.5 / 2.5 / 1.6) < 1e-6 && Math.abs(pushed.y) < 1e-6 && Math.abs(pushed.z) < 1e-6,
                "a cow 1.5 blocks away is blown that way by 0.375: " + pushed);
        test.assertTrue(far.getDeltaMovement().equals(farBefore), "a cow 4 blocks away is left alone");
        test.assertTrue(loose.getDeltaMovement().x < 0 && stand.getDeltaMovement().z < 0, "loose items and armor stands are blown away too: " + loose.getDeltaMovement() + " " + stand.getDeltaMovement());
        test.assertTrue(sword.getDamageValue() == 1, "the first tick of the hold costs a use: " + sword.getDamageValue());
        // The ceiling.
        player.setDeltaMovement(0, 0.45, 0);
        sword.getItem().onUseTick(level, player, sword, ElementalSwordRules.USE_DURATION - 1);
        test.assertTrue(Math.abs(player.getDeltaMovement().y - ElementalSwordRules.LIFT_RESET) < 1e-6, "above 0.5 the speed is reset to 0.2: " + player.getDeltaMovement().y);
        // A use every twentieth tick, the first counted.
        for (int ticks = 2; ticks <= 40; ++ticks) sword.getItem().onUseTick(level, player, sword, ElementalSwordRules.USE_DURATION - ticks);
        test.assertTrue(sword.getDamageValue() == 3, "ticks 0, 20 and 40 cost a use each: " + sword.getDamageValue());
        // Players are not blown away.
        player.setDeltaMovement(Vec3.ZERO);
        var visitor = Visitor.join(level, spot.add(1.0, 0, 0));
        try {
            sword.getItem().onUseTick(level, player, sword, ElementalSwordRules.USE_DURATION - 3);
            test.assertTrue(visitor.player().getDeltaMovement().equals(Vec3.ZERO), "a bystanding player is not blown away: " + visitor.player().getDeltaMovement());
        } finally {
            visitor.leave(level);
        }
        // Anything else in the hand does nothing.
        ItemStack plain = new ItemStack(item("thaumcraft:thaumium_sword"));
        player.setDeltaMovement(0, -1.0, 0);
        plain.getItem().onUseTick(level, player, plain, 72000);
        test.assertTrue(player.getDeltaMovement().y == -1.0, "the thaumium sword lifts nothing");
    }

    // ---- the sweep ------------------------------------------------------------------------------------------------------

    private static float health(LivingEntity entity) { return entity.getHealth(); }

    /** The weapon's attribute modifiers join the player's attributes on the player's next tick, as they do for any player. */
    private static void equip(ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.doTick();
    }

    private static void sweep(GameTestHelper test, ServerLevel level, ServerPlayer player, BlockPos origin, List<Entity> spawned) {
        Vec3 spot = Vec3.atBottomCenterOf(origin).add(0, 0, 30);
        player.setPos(spot.add(-3, 0, 0));
        player.fallDistance = 0;
        player.setSprinting(false);
        var cooldown = (LivingEntityAttackStrengthAccess) player;
        ItemStack sword = sword();
        equip(player, sword);
        cooldown.thaumcraft$setAttackStrengthTicker(1000);
        Cow target = put(level, spawned, EntityType.COW.create(level), spot);
        Cow left = put(level, spawned, EntityType.COW.create(level), spot.add(0, 0, -0.9));
        Cow right = put(level, spawned, EntityType.COW.create(level), spot.add(0, 0, 0.9));
        Wolf strangers = put(level, spawned, EntityType.WOLF.create(level), spot.add(0.9, 0, 0.0));
        strangers.setTame(true, false);
        strangers.setOwnerUUID(UUID.randomUUID());
        Wolf pet = put(level, spawned, EntityType.WOLF.create(level), spot.add(-0.9, 0, 0.0));
        pet.setTame(true, false);
        pet.setOwnerUUID(player.getUUID());
        var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
        golem.setGolemMaterial(7);
        golem.setOwnerName(player.getGameProfile().getName());
        Entity ownGolem = put(level, spawned, golem, spot.add(0.0, 0, 0.5));
        Cow distant = put(level, spawned, EntityType.COW.create(level), spot.add(0, 0, 3.5));
        ArmorStand stand = put(level, spawned, EntityType.ARMOR_STAND.create(level), spot.add(0.5, 0, -0.5));
        float full = 8F; // the player's 1 and the sword's 7, at full strength, standing still
        player.attack(target);
        test.assertTrue(Math.abs(health(target) - (10F - full)) < 1e-3, "the target is struck as hard as ever: " + health(target));
        test.assertTrue(Math.abs(health(left) - (10F - full)) < 1e-3 && Math.abs(health(right) - (10F - full)) < 1e-3 && Math.abs(health(strangers) - (strangers.getMaxHealth() - full)) < 1e-3,
                "the mobs round it take the full blow: " + health(left) + " " + health(right) + " " + health(strangers));
        test.assertTrue(health(pet) == pet.getMaxHealth() && health(distant) == 10F && health(stand) == stand.getMaxHealth() && ((LivingEntity) ownGolem).getHealth() == ((LivingEntity) ownGolem).getMaxHealth(),
                "the player's wolf and golem, a cow out of reach and an armor stand are spared");
        test.assertTrue(sword.getDamageValue() == 4, "four blows, a use each, no nested sweeps: " + sword.getDamageValue());
        test.assertTrue(cooldown.thaumcraft$getAttackStrengthTicker() == 0, "the cooldown of the blow itself starts over as usual: " + cooldown.thaumcraft$getAttackStrengthTicker());
        // The thaumium sword strikes the target alone.
        spawned.forEach(entity -> { if (entity instanceof LivingEntity living && !(entity instanceof ArmorStand) && !(entity instanceof EntityGolemBase)) living.setHealth(living.getMaxHealth()); });
        for (Entity entity : spawned) entity.invulnerableTime = 0;
        ItemStack plain = new ItemStack(item("thaumcraft:thaumium_sword"));
        equip(player, plain);
        cooldown.thaumcraft$setAttackStrengthTicker(1000);
        player.attack(target);
        test.assertTrue(health(target) < 10F && health(left) == 10F && health(right) == 10F, "a thaumium sword's blow does not sweep: " + health(left));
        // A sword that gives out in the sweep ends it.
        for (Entity entity : spawned) { if (entity instanceof LivingEntity living && !(entity instanceof ArmorStand) && !(entity instanceof EntityGolemBase)) living.setHealth(living.getMaxHealth()); entity.invulnerableTime = 0; }
        ItemStack worn = sword();
        worn.setDamageValue(worn.getMaxDamage() - 2);
        equip(player, worn);
        cooldown.thaumcraft$setAttackStrengthTicker(1000);
        player.attack(target);
        int untouched = 0;
        for (LivingEntity candidate : List.of(left, right, strangers)) if (health(candidate) == candidate.getMaxHealth()) ++untouched;
        test.assertTrue(untouched == 1 && player.getMainHandItem().isEmpty(), "the sword breaks on the second blow of the sweep and the third mob is spared: " + untouched);
    }
}
