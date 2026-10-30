package thaumic.tinkerer.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.EventResult;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumic.tinkerer.common.core.fakeplayer.TinkererFakePlayer;
import thaumic.tinkerer.common.hooks.FloatingTicks;
import thaumic.tinkerer.common.hooks.TinkererEvents;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>Runtime checks of the loader hooks ({@code docs/HOOKS.md}): each event is registered inside the check on a real server,
 * the vanilla code path that should post it is run (a zombie's blow, an arrow, the fake player's attack, a real dig start,
 * a jump, a 10-block fall, a drawn bow, a death), and what the listener saw and what vanilla did afterwards are asserted.
 * Checks of one batch run side by side in the same ticks, so every listener answers only for the entities of its own check
 * and is taken away again in a {@code finally}.</p>
 *
 * <p>Not covered, because a server cannot run it: that the same events are posted on the client (the dig prediction, the
 * jump and the bow of the local player). The mixins are the vanilla classes' own code on both sides.</p>
 */
public final class HooksRuntimeChecks {
    private HooksRuntimeChecks() {}

    private static final float EPSILON = 0.001F;

    // ---- helpers ---------------------------------------------------------------------------------------------------

    /** A floor of stone at relative y = 0 (its top is at y = 1), so that mobs and the mock players stand. */
    private static void floor(GameTestHelper test) {
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) test.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    /** A survival mock player on the floor with an empty inventory (the helper's mock is flagged creative). */
    @SuppressWarnings("removal") // the only mock that is a ServerPlayer (game mode, attribute and network paths need one)
    private static ServerPlayer player(GameTestHelper test, double x, double y, double z) {
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = test.absoluteVec(new Vec3(x, y, z));
        player.moveTo(at.x, at.y, at.z, 0F, 0F);
        player.getInventory().clearContent();
        return player;
    }

    private static void finish(GameTestHelper test, ServerPlayer... players) {
        for (ServerPlayer player : players) player.discard();
        test.succeed();
    }

    /** A mob that does not wander and cannot die of a few blows. */
    private static <T extends Mob> T tough(GameTestHelper test, EntityType<T> type, double x, double y, double z) {
        T mob = test.spawn(type, new Vec3(x, y, z));
        mob.setNoAi(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
        mob.setHealth(1000F);
        return mob;
    }

    /** A zombie that can attack (it has a helmet, so the sun does not burn it while a check waits). */
    private static Zombie zombie(GameTestHelper test, double x, double y, double z) {
        Zombie zombie = test.spawn(EntityType.ZOMBIE, new Vec3(x, y, z));
        zombie.setNoAi(true);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        return zombie;
    }

    private static TinkererFakePlayer fakePlayer(GameTestHelper test, ItemStack held) {
        TinkererFakePlayer fake = new TinkererFakePlayer(test.getLevel(), new GameProfile(UUID.randomUUID(), "[TTHooksTest]"));
        Vec3 at = test.absoluteVec(new Vec3(1.5, 1.0, 5.5));
        fake.placeAt(at.x, at.y, at.z, -90F, 0F);
        fake.getInventory().setItem(0, held);
        fake.getInventory().selected = 0;
        return fake;
    }

    /** What vanilla's armour formula leaves of a hit on an entity, with no enchantments or effects in play. */
    private static float afterArmour(LivingEntity victim, DamageSource source, float amount) {
        return CombatRules.getDamageAfterAbsorb(victim, amount, source, (float) victim.getArmorValue(),
                (float) victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }

    private static void near(GameTestHelper test, float actual, float expected, String what) {
        test.assertTrue(Math.abs(actual - expected) < EPSILON, what + ": " + actual + " (expected " + expected + ")");
    }

    private static Holder<Enchantment> enchantment(GameTestHelper test, net.minecraft.resources.ResourceKey<Enchantment> key) {
        return test.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
    }

    private static List<ItemEntity> itemsAround(GameTestHelper test, Vec3 relative, double radius) {
        Vec3 at = test.absoluteVec(relative);
        return test.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(radius));
    }

    private static boolean has(List<ItemEntity> items, net.minecraft.world.item.Item item) {
        return items.stream().anyMatch(entity -> entity.getItem().is(item));
    }

    // ---- LIVING_DAMAGE ---------------------------------------------------------------------------------------------

    /**
     * A zombie's blow on an armoured pig: the listener sees the whole amount, before the armour; a listener that halves it
     * leaves the armour formula to work on the half; two listeners chain in the order they were registered.
     */
    public static void livingDamageIsPostedBeforeArmourAndChainsInOrder(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        Zombie zombie = zombie(test, 1.5, 1, 3.5);
        Pig pig = tough(test, EntityType.PIG, 3.5, 1, 3.5);
        pig.getAttribute(Attributes.ARMOR).setBaseValue(20D);
        DamageSource blow = level.damageSources().mobAttack(zombie);

        // control: no listener, vanilla only
        float before = pig.getHealth();
        test.assertTrue(pig.hurt(blow, 10F), "the hit lands");
        near(test, before - pig.getHealth(), afterArmour(pig, blow, 10F), "without a listener the armour works on the whole hit");
        test.assertTrue(before - pig.getHealth() < 6F, "the armour did reduce the hit (control of the formula)");

        List<Float> seen = new ArrayList<>();
        List<Boolean> rightSource = new ArrayList<>();
        TinkererEvents.LivingDamage halve = (victim, source, amount) -> {
            if (victim != pig) return amount;
            seen.add(amount);
            rightSource.add(source.getEntity() == zombie);
            return amount * 0.5F;
        };
        TinkererEvents.LIVING_DAMAGE.register(halve);
        try {
            pig.invulnerableTime = 0;
            before = pig.getHealth();
            test.assertTrue(pig.hurt(blow, 10F), "the halved hit lands");
            test.assertTrue(seen.equals(List.of(10F)), "the listener saw the whole hit once, before the armour: " + seen);
            test.assertTrue(rightSource.equals(List.of(true)), "and the damage source with the zombie in it");
            near(test, before - pig.getHealth(), afterArmour(pig, blow, 5F), "the armour then worked on the halved hit");
        } finally {
            TinkererEvents.LIVING_DAMAGE.unregister(halve);
        }

        // two listeners: (10 * 2) - 3 = 17 in this order, (10 - 3) * 2 = 14 in the other
        List<Float> secondSaw = new ArrayList<>();
        TinkererEvents.LivingDamage doubled = (victim, source, amount) -> victim == pig ? amount * 2F : amount;
        TinkererEvents.LivingDamage minusThree = (victim, source, amount) -> {
            if (victim != pig) return amount;
            secondSaw.add(amount);
            return amount - 3F;
        };
        TinkererEvents.LIVING_DAMAGE.register(doubled);
        TinkererEvents.LIVING_DAMAGE.register(minusThree);
        try {
            pig.invulnerableTime = 0;
            before = pig.getHealth();
            pig.hurt(blow, 10F);
            test.assertTrue(secondSaw.equals(List.of(20F)), "the second listener saw what the first returned: " + secondSaw);
            near(test, before - pig.getHealth(), afterArmour(pig, blow, 17F), "(10 * 2) - 3 went on to the armour");
        } finally {
            TinkererEvents.LIVING_DAMAGE.unregister(doubled);
            TinkererEvents.LIVING_DAMAGE.unregister(minusThree);
        }
        TinkererEvents.LIVING_DAMAGE.register(minusThree);
        TinkererEvents.LIVING_DAMAGE.register(doubled);
        try {
            pig.invulnerableTime = 0;
            before = pig.getHealth();
            pig.hurt(blow, 10F);
            near(test, before - pig.getHealth(), afterArmour(pig, blow, 14F), "(10 - 3) * 2 in the other order");
        } finally {
            TinkererEvents.LIVING_DAMAGE.unregister(doubled);
            TinkererEvents.LIVING_DAMAGE.unregister(minusThree);
        }

        // unregistered: the plain game again
        pig.invulnerableTime = 0;
        before = pig.getHealth();
        pig.hurt(blow, 10F);
        near(test, before - pig.getHealth(), afterArmour(pig, blow, 10F), "listeners taken away, vanilla again");
        zombie.discard();
        pig.discard();
        test.succeed();
    }

    /**
     * A listener that cancels leaves the entity as an invulnerable one is left: no damage, no hurt animation, no knockback, no
     * invulnerability frames. A listener that halves moves the frame comparison (the later, weaker hit is dropped).
     */
    public static void livingDamageCancelIsLikeInvulnerableAndFramesFollowTheListener(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        Zombie zombie = zombie(test, 1.5, 1, 3.5);
        Pig hit = tough(test, EntityType.PIG, 3.5, 1, 3.5);
        Pig cancelled = tough(test, EntityType.PIG, 3.5, 1, 5.5);
        Pig invulnerable = tough(test, EntityType.PIG, 3.5, 1, 1.5);
        invulnerable.setInvulnerable(true);
        DamageSource blow = level.damageSources().mobAttack(zombie);

        // the control shows what a hit does to the state that the cancel must leave alone
        TinkererEvents.LivingDamage cancelOne = (victim, source, amount) -> victim == cancelled ? 0F : amount;
        TinkererEvents.LIVING_DAMAGE.register(cancelOne);
        try {
            test.assertTrue(hit.hurt(blow, 10F), "an ordinary hit lands");
            test.assertTrue(hit.getHealth() == 990F && hit.invulnerableTime > 0 && hit.hurtTime > 0, "and costs health, starts the frames and the animation");
            test.assertTrue(hit.getDeltaMovement().x > 0.05D, "and knocks the pig away from the zombie: " + hit.getDeltaMovement());

            test.assertTrue(!cancelled.hurt(blow, 10F), "the cancelled hit is refused, as for an invulnerable entity");
            test.assertTrue(!invulnerable.hurt(blow, 10F), "(the invulnerable pig refuses it too)");
            for (Pig pig : List.of(cancelled, invulnerable)) {
                test.assertTrue(pig.getHealth() == 1000F, "no damage");
                test.assertTrue(pig.invulnerableTime == 0 && pig.hurtTime == 0, "no frames and no hurt animation: " + pig.invulnerableTime + "/" + pig.hurtTime);
                test.assertTrue(pig.getDeltaMovement().equals(Vec3.ZERO), "no knockback: " + pig.getDeltaMovement());
            }
            test.assertTrue(cancelled.getLastDamageSource() == null, "and no last damage source");
        } finally {
            TinkererEvents.LIVING_DAMAGE.unregister(cancelOne);
        }
        // the entity that was cancelled can be hurt right away once nobody cancels
        test.assertTrue(cancelled.hurt(blow, 10F) && cancelled.getHealth() == 990F, "without the listener the same pig is hurt");

        // frames: the listener halves, so a second hit is compared after halving. 10 -> 5 sets the last hurt to 5; 8 -> 4 is not more.
        Pig framed = tough(test, EntityType.PIG, 5.5, 1, 3.5);
        TinkererEvents.LivingDamage halve = (victim, source, amount) -> victim == framed ? amount * 0.5F : amount;
        TinkererEvents.LIVING_DAMAGE.register(halve);
        try {
            test.assertTrue(framed.hurt(blow, 10F) && framed.getHealth() == 995F, "5 of the 10 came through: " + framed.getHealth());
            test.assertTrue(!framed.hurt(blow, 8F) && framed.getHealth() == 995F, "8 halved is 4, not more than the last 5: refused");
            test.assertTrue(framed.hurt(blow, 14F) && framed.getHealth() == 993F, "14 halved is 7, 2 more than the last 5: " + framed.getHealth());
        } finally {
            TinkererEvents.LIVING_DAMAGE.unregister(halve);
        }
        for (Mob mob : List.of(zombie, hit, cancelled, invulnerable, framed)) mob.discard();
        test.succeed();
    }

    /** The fake player (the Animation Tablet's) hits a mob; a zombie hits a player; both go through the event. */
    public static void livingDamageSeesAFakePlayersBlowAndAZombiesBlowOnAPlayer(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        // the fake player, never ticked, with a diamond sword
        TinkererFakePlayer fake = fakePlayer(test, new ItemStack(Items.DIAMOND_SWORD));
        Pig pig = tough(test, EntityType.PIG, 3.5, 1, 5.5);
        List<Float> seen = new ArrayList<>();
        TinkererEvents.LivingDamage doubled = (victim, source, amount) -> {
            if (victim != pig) return amount;
            seen.add(amount);
            return amount * 2F;
        };
        TinkererEvents.LIVING_DAMAGE.register(doubled);
        try {
            float before = pig.getHealth();
            fake.attackEntity(pig);
            test.assertTrue(seen.size() == 1, "the blow of the fake player posted the event once: " + seen);
            float base = (float) fake.getAttributeValue(Attributes.ATTACK_DAMAGE);
            test.assertTrue(base > 1F, "the sword is in the attack damage of the fake player: " + base);
            near(test, seen.get(0), base, "the listener saw the full blow");
            near(test, before - pig.getHealth(), base * 2F, "and the doubled blow is what the pig lost");
        } finally {
            TinkererEvents.LIVING_DAMAGE.unregister(doubled);
        }

        fake.discard();
        pig.discard();

        // a zombie against a player: the amount is the difficulty-scaled one (Player.hurt scales before the living entity's hurt).
        // A new ServerPlayer refuses every hit for 60 ticks (spawnInvulnerableTime, counted down by ServerPlayer.tick, which the level
        // calls for a mock player as for any), so the blow comes after that.
        ServerPlayer victim = player(test, 5.5, 1, 1.5);
        Zombie zombie = zombie(test, 4.5, 1, 1.5);
        Difficulty difficulty = level.getDifficulty();
        test.assertTrue(difficulty != Difficulty.PEACEFUL, "the test world is not peaceful (a zombie would do no damage)");
        float raw = (float) zombie.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float scaled = difficulty == Difficulty.EASY ? Math.min(raw / 2F + 1F, raw) : difficulty == Difficulty.HARD ? raw * 1.5F : raw;
        List<Float> playerSaw = new ArrayList<>();
        TinkererEvents.LivingDamage halveForPlayer = (who, source, amount) -> {
            if (who != victim) return amount;
            playerSaw.add(amount);
            return amount * 0.5F;
        };
        TinkererEvents.LIVING_DAMAGE.register(halveForPlayer);
        test.runAfterDelay(65, () -> {
            try {
                victim.setHealth(20F);
                test.assertTrue(zombie.doHurtTarget(victim), "the zombie's blow lands on the player (not refused before the event)");
                test.assertTrue(playerSaw.size() == 1, "the player's hurt posted the event once: " + playerSaw);
                near(test, playerSaw.get(0), scaled, "the listener saw the blow after the difficulty scaling (" + difficulty + ")");
                near(test, 20F - victim.getHealth(), scaled * 0.5F, "the player lost the halved amount (no armour)");
            } finally {
                TinkererEvents.LIVING_DAMAGE.unregister(halveForPlayer);
                zombie.discard();
            }
            finish(test, victim);
        });
    }

    /** An arrow (indirect damage with the shooter as attacker) goes through the event and the changed amount is dealt. */
    public static void livingDamageSeesAnArrow(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        Zombie shooter = zombie(test, 0.5, 1, 3.5);
        Pig pig = tough(test, EntityType.PIG, 4.5, 1, 3.5);
        List<Float> seen = new ArrayList<>();
        List<Boolean> shotBy = new ArrayList<>();
        TinkererEvents.LivingDamage cut = (victim, source, amount) -> {
            if (victim != pig) return amount;
            seen.add(amount);
            shotBy.add(source.getEntity() == shooter);
            return amount - 1F;
        };
        TinkererEvents.LIVING_DAMAGE.register(cut);
        Arrow arrow = new Arrow(level, shooter, new ItemStack(Items.ARROW), null);
        Vec3 from = test.absoluteVec(new Vec3(1.5, 1.4, 3.5));
        arrow.setPos(from.x, from.y, from.z);
        arrow.shoot(1, 0, 0, 1.5F, 0F);
        level.addFreshEntity(arrow);
        test.runAfterDelay(6, () -> {
            try {
                test.assertTrue(seen.size() == 1, "the arrow's hit posted the event once: " + seen);
                test.assertTrue(shotBy.equals(List.of(true)), "with the shooter as the attacker of the source");
                test.assertTrue(seen.get(0) >= 2F && seen.get(0) <= 3F, "an arrow of speed 1.5 and base damage 2 hits for about 3: " + seen);
                near(test, 1000F - pig.getHealth(), seen.get(0) - 1F, "the pig lost what the listener left");
            } finally {
                TinkererEvents.LIVING_DAMAGE.unregister(cut);
                arrow.discard();
                shooter.discard();
                pig.discard();
            }
            test.succeed();
        });
    }

    // ---- BREAK_SPEED -----------------------------------------------------------------------------------------------

    /**
     * The dig speed of a server player and of a fake player that is never ticked: {@code BlockState.getDestroyProgress} posts the
     * event with the block's position; chains in order; a speed of zero is "cannot dig"; and a dig that starts through the real
     * {@code ServerPlayerGameMode} breaks the block at once when the listener makes it fast enough.
     */
    public static void breakSpeedIsPostedForAServerPlayerAndAFakePlayer(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        BlockPos dirtRel = new BlockPos(3, 1, 2);
        BlockPos dirt = test.absolutePos(dirtRel);
        test.setBlock(dirtRel, Blocks.DIRT);
        BlockState state = level.getBlockState(dirt);

        ServerPlayer player = player(test, 6.5, 1, 6.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
        TinkererFakePlayer fake = fakePlayer(test, new ItemStack(Items.IRON_SHOVEL));

        float plainPlayer = state.getDestroyProgress(player, level, dirt);
        float plainFake = state.getDestroyProgress(fake, level, dirt);
        test.assertTrue(plainPlayer > 0F && plainPlayer < 1F, "an iron shovel does not break dirt at once: " + plainPlayer);
        test.assertTrue(plainFake > 0F && plainFake < 1F, "nor does the fake player's: " + plainFake);

        List<Object[]> calls = new ArrayList<>();
        TinkererEvents.BreakSpeed times5 = (who, blockState, pos, speed) -> {
            if (who != player && who != fake) return speed;
            calls.add(new Object[] { who, blockState, pos, speed });
            return speed * 5F;
        };
        TinkererEvents.BREAK_SPEED.register(times5);
        try {
            calls.clear();
            near(test, state.getDestroyProgress(player, level, dirt), plainPlayer * 5F, "the server player digs five times as fast");
            test.assertTrue(calls.size() == 1, "one post for one progress: " + calls.size());
            Object[] call = calls.get(0);
            test.assertTrue(call[1] == state && dirt.equals(call[2]), "the listener got the block state and its position: " + call[2]);
            near(test, (Float) call[3], (float) player.getDestroySpeed(state), "and the speed that Player.getDestroySpeed worked out");

            calls.clear();
            near(test, state.getDestroyProgress(fake, level, dirt), plainFake * 5F, "the fake player that is never ticked digs five times as fast");
            test.assertTrue(calls.size() == 1 && calls.get(0)[0] == fake && dirt.equals(calls.get(0)[2]), "the event was posted for the fake player with the position");
        } finally {
            TinkererEvents.BREAK_SPEED.unregister(times5);
        }

        // order of two listeners: (s * 3) - 1 against (s - 1) * 3 differ, with s the plain speed
        float plainSpeed = (float) player.getDestroySpeed(state);
        TinkererEvents.BreakSpeed times3 = (who, blockState, pos, speed) -> who == player ? speed * 3F : speed;
        TinkererEvents.BreakSpeed minus1 = (who, blockState, pos, speed) -> who == player ? speed - 1F : speed;
        float perSpeed = plainPlayer / plainSpeed;
        TinkererEvents.BREAK_SPEED.register(times3);
        TinkererEvents.BREAK_SPEED.register(minus1);
        try {
            near(test, state.getDestroyProgress(player, level, dirt), (plainSpeed * 3F - 1F) * perSpeed, "(s * 3) - 1");
        } finally {
            TinkererEvents.BREAK_SPEED.unregister(times3);
            TinkererEvents.BREAK_SPEED.unregister(minus1);
        }
        TinkererEvents.BREAK_SPEED.register(minus1);
        TinkererEvents.BREAK_SPEED.register(times3);
        try {
            near(test, state.getDestroyProgress(player, level, dirt), (plainSpeed - 1F) * 3F * perSpeed, "(s - 1) * 3");
        } finally {
            TinkererEvents.BREAK_SPEED.unregister(minus1);
            TinkererEvents.BREAK_SPEED.unregister(times3);
        }

        // cannot dig
        TinkererEvents.BreakSpeed never = (who, blockState, pos, speed) -> who == player ? -1F : speed;
        TinkererEvents.BREAK_SPEED.register(never);
        try {
            test.assertTrue(state.getDestroyProgress(player, level, dirt) == 0F, "a listener that says cannot dig gives progress zero");
            test.assertTrue(state.getDestroyProgress(fake, level, dirt) == plainFake, "and another player is not affected");
        } finally {
            TinkererEvents.BREAK_SPEED.unregister(never);
        }

        // the real dig of a server player: START_DESTROY_BLOCK breaks a block whose progress is 1 or more at once
        TinkererEvents.BreakSpeed instant = (who, blockState, pos, speed) -> who == player && dirt.equals(pos) ? speed * 1000F : speed;
        player.gameMode.handleBlockBreakAction(dirt, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, Direction.UP,
                level.getMaxBuildHeight(), 1);
        test.assertTrue(level.getBlockState(dirt).is(Blocks.DIRT), "without a listener the dig has only begun and the dirt stays");
        player.gameMode.handleBlockBreakAction(dirt, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, Direction.UP,
                level.getMaxBuildHeight(), 2);
        TinkererEvents.BREAK_SPEED.register(instant);
        try {
            player.gameMode.handleBlockBreakAction(dirt, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, Direction.UP,
                    level.getMaxBuildHeight(), 3);
            test.assertTrue(level.getBlockState(dirt).isAir(), "with a listener that makes the speed a thousandfold the first click breaks the dirt");
        } finally {
            TinkererEvents.BREAK_SPEED.unregister(instant);
        }
        fake.discard();
        finish(test, player);
    }

    // ---- LIVING_JUMP -----------------------------------------------------------------------------------------------

    /** The jump of a mob and of a player pressing the key: the listener sees the motion of the jump and changes it. */
    public static void livingJumpIsPostedAfterTheJumpMotionIsApplied(GameTestHelper test) {
        floor(test);
        Pig pig = tough(test, EntityType.PIG, 3.5, 1, 3.5);
        ServerPlayer player = player(test, 6.5, 1, 6.5);
        List<Double> seenY = new ArrayList<>();
        List<Object> who = new ArrayList<>();
        TinkererEvents.LivingJump boost = entity -> {
            if (entity != pig && entity != player) return;
            who.add(entity);
            Vec3 motion = entity.getDeltaMovement();
            seenY.add(motion.y);
            entity.setDeltaMovement(motion.x, motion.y * 2D, motion.z);
        };
        TinkererEvents.LIVING_JUMP.register(boost);
        try {
            pig.jumpFromGround();
            test.assertTrue(who.equals(List.of(pig)), "the jump of the pig posted the event once");
            test.assertTrue(Math.abs(seenY.get(0) - 0.42D) < 1.0E-4D, "the listener saw the motion of the jump already applied: " + seenY.get(0));
            test.assertTrue(Math.abs(pig.getDeltaMovement().y - 0.84D) < 1.0E-4D, "and doubled it: " + pig.getDeltaMovement().y);

            // the player's own path: the jump key (setJumping) on the ground, one tick of the player
            who.clear();
            seenY.clear();
            player.setOnGround(true);
            player.setJumping(true);
            player.doTick();
            test.assertTrue(who.equals(List.of(player)), "the jump key on the ground made the server player jump once: " + who.size());
            test.assertTrue(player.getDeltaMovement().y > seenY.get(0) * 1.5D || player.getY() > test.absoluteVec(new Vec3(0, 1, 0)).y + 0.1D,
                    "the doubled jump is the player's motion: " + player.getDeltaMovement().y + " after " + seenY.get(0));
        } finally {
            TinkererEvents.LIVING_JUMP.unregister(boost);
        }
        // no listener: a plain jump
        pig.setDeltaMovement(Vec3.ZERO);
        pig.jumpFromGround();
        test.assertTrue(Math.abs(pig.getDeltaMovement().y - 0.42D) < 1.0E-4D, "unregistered: the plain jump");
        pig.discard();
        finish(test, player);
    }

    // ---- LIVING_FALL -----------------------------------------------------------------------------------------------

    /**
     * Three pigs fall from 10.5 blocks above the floor onto stone: one is left alone, one has its fall cancelled, one has the
     * distance halved. The listener sees the distance vanilla counted (the fall height less the movement of the landing tick,
     * about 9.7 here), and what each pig lost is worked out from that distance with the damage formula. A player's fall is
     * cancelled through {@code Player.causeFallDamage}.
     */
    public static void livingFallIsPostedOnLandingAndCanHalveOrCancel(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        // The harness closes the 8-high test area with a ceiling of barrier blocks at relative y = 9: open three columns of it, so
        // the pigs can fall from above it (11.5) to the floor (top at 1.0), 10.5 blocks.
        List<BlockPos> ceiling = new ArrayList<>();
        for (int x = 0; x <= 6; x++) for (int z = 2; z <= 4; z++) ceiling.add(new BlockPos(x, 9, z));
        ceiling.forEach(pos -> test.setBlock(pos, Blocks.AIR));
        Pig plain = tough(test, EntityType.PIG, 1.5, 11.5, 3.5);
        Pig cancelled = tough(test, EntityType.PIG, 3.5, 11.5, 3.5);
        Pig halved = tough(test, EntityType.PIG, 5.5, 11.5, 3.5);
        for (Pig pig : List.of(plain, cancelled, halved)) {
            pig.setNoAi(false);
            pig.removeFreeWill(); // gravity yes, wandering no
        }
        List<Float> plainSaw = new ArrayList<>();
        List<Float> cancelledSaw = new ArrayList<>();
        List<Float> halvedSaw = new ArrayList<>();
        List<Boolean> clientSide = new ArrayList<>();
        TinkererEvents.LivingFall listener = (entity, fall) -> {
            if (entity == cancelled) {
                cancelledSaw.add(fall.getDistance());
                return EventResult.interruptFalse();
            }
            if (entity == halved) {
                halvedSaw.add(fall.getDistance());
                fall.setDistance(fall.getDistance() / 2F);
            }
            if (entity == plain) {
                plainSaw.add(fall.getDistance());
                clientSide.add(entity.level().isClientSide);
            }
            return EventResult.pass();
        };
        TinkererEvents.LIVING_FALL.register(listener);
        ServerPlayer player = player(test, 6.5, 1, 1.5);
        List<Float> playerSaw = new ArrayList<>();
        TinkererEvents.LivingFall cancelPlayer = (entity, fall) -> {
            if (entity != player) return EventResult.pass();
            playerSaw.add(fall.getDistance());
            return EventResult.interruptFalse();
        };
        TinkererEvents.LIVING_FALL.register(cancelPlayer);
        try {
            player.setHealth(20F);
            test.assertTrue(!player.causeFallDamage(12F, 1F, level.damageSources().fall()), "a cancelled fall returns false");
            test.assertTrue(playerSaw.equals(List.of(12F)) && player.getHealth() == 20F, "and the player takes nothing: " + player.getHealth());
        } finally {
            TinkererEvents.LIVING_FALL.unregister(cancelPlayer);
        }
        test.runAfterDelay(45, () -> {
            try {
                float safe = (float) plain.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
                test.assertTrue(plain.onGround() && cancelled.onGround() && halved.onGround(), "all three pigs have landed");
                test.assertTrue(plainSaw.size() == 1 && cancelledSaw.size() == 1 && halvedSaw.size() == 1,
                        "each pig's landing posted the event once: " + plainSaw + cancelledSaw + halvedSaw);
                test.assertTrue(clientSide.equals(List.of(false)), "and the pig that was left alone was seen on the server");
                for (float distance : List.of(plainSaw.get(0), cancelledSaw.get(0), halvedSaw.get(0))) {
                    test.assertTrue(distance > 9F && distance < 10.5F, "the listener saw the distance of a 10.5 block fall as vanilla counts it: " + distance);
                }
                float seen = plainSaw.get(0);
                near(test, 1000F - plain.getHealth(), Mth.ceil(seen - safe), "an untouched fall costs the vanilla damage of the distance seen");
                test.assertTrue(plain.getHealth() < 1000F, "and it is some damage");
                test.assertTrue(cancelled.getHealth() == 1000F && cancelled.invulnerableTime == 0, "the cancelled fall does nothing: " + cancelled.getHealth());
                float halfSeen = halvedSaw.get(0) / 2F;
                near(test, 1000F - halved.getHealth(), Mth.ceil(halfSeen - safe), "the halved distance costs the damage of half the distance seen");
                test.assertTrue(halved.getHealth() > plain.getHealth(), "and that is less than the untouched fall");
            } finally {
                TinkererEvents.LIVING_FALL.unregister(listener);
                for (Pig pig : List.of(plain, cancelled, halved)) pig.discard();
                ceiling.forEach(pos -> test.setBlock(pos, Blocks.BARRIER));
            }
            finish(test, player);
        });
    }

    // ---- USE_ITEM_TICK ---------------------------------------------------------------------------------------------

    /**
     * A bow is drawn for twelve ticks by two players; the listener takes two extra ticks off the count of one of them each
     * tick. Its draw clock reads three times as long, and the arrow it releases flies at full power, the other's at the power
     * of twelve ticks.
     */
    public static void useItemTickExtendsABowDraw(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        ServerPlayer plain = player(test, 2.5, 1, 2.5);
        ServerPlayer quick = player(test, 5.5, 1, 5.5);
        for (ServerPlayer player : List.of(plain, quick)) {
            player.getInventory().setItem(0, new ItemStack(Items.BOW));
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 8));
            player.getInventory().selected = 0;
        }
        List<Integer> remainings = new ArrayList<>();
        TinkererEvents.UseItemTick takeTwo = (entity, stack, remaining) -> {
            if (entity != quick) return remaining;
            remainings.add(remaining);
            return remaining - 2;
        };
        TinkererEvents.USE_ITEM_TICK.register(takeTwo);
        test.runAfterDelay(2, () -> {
            try {
                plain.startUsingItem(InteractionHand.MAIN_HAND);
                quick.startUsingItem(InteractionHand.MAIN_HAND);
                int full = plain.getUseItemRemainingTicks();
                for (int i = 0; i < 12; i++) {
                    plain.doTick();
                    quick.doTick();
                }
                int plainTicks = plain.getTicksUsingItem();
                int quickTicks = quick.getTicksUsingItem();
                test.assertTrue(plain.isUsingItem() && quick.isUsingItem(), "both bows are still drawn");
                test.assertTrue(plainTicks >= 10, "the plain draw clock ran: " + plainTicks);
                test.assertTrue(quickTicks == plainTicks * 3, "the listener's two extra ticks per tick make the clock read three times: "
                        + quickTicks + " against " + plainTicks);
                test.assertTrue(remainings.size() == plainTicks, "the listener was called once per tick, with the count so far: " + remainings.size());
                test.assertTrue(remainings.get(0) == full, "first with the full count " + full + ": " + remainings.get(0));
                test.assertTrue(quick.getUseItemRemainingTicks() == full - quickTicks, "the count the entity holds is the listener's");

                plain.releaseUsingItem();
                quick.releaseUsingItem();
                double plainSpeed = fastestArrowNear(level, plain);
                double quickSpeed = fastestArrowNear(level, quick);
                test.assertTrue(plainSpeed > 0.5D && plainSpeed < 2.0D, "twelve ticks of draw is a weak arrow: " + plainSpeed);
                test.assertTrue(quickSpeed > 2.5D, "the extended draw is a full-power arrow (speed 3): " + quickSpeed);
            } finally {
                TinkererEvents.USE_ITEM_TICK.unregister(takeTwo);
            }
            finish(test, plain, quick);
        });
    }

    private static double fastestArrowNear(ServerLevel level, ServerPlayer player) {
        double fastest = -1D;
        for (Arrow arrow : level.getEntitiesOfClass(Arrow.class, player.getBoundingBox().inflate(3D))) {
            if (arrow.getOwner() == player) fastest = Math.max(fastest, arrow.getDeltaMovement().length());
        }
        return fastest;
    }

    // ---- LIVING_DROPS ----------------------------------------------------------------------------------------------

    /**
     * A pig is killed by the fake player with a Looting III sword: the listener sees the drops before they are in the world,
     * the looting level and the recent-hit flag; it adds a diamond, removes the pork, clears the list or cancels. Experience
     * orbs are not touched.
     */
    public static void livingDropsAreEditedBeforeTheyEnterTheWorld(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        test.assertTrue(!TinkererEvents.hasLivingDropsListeners(), "nobody listens when the check starts");
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(test, Enchantments.LOOTING), 3);
        TinkererFakePlayer fake = fakePlayer(test, sword);
        Vec3 spot = new Vec3(3.5, 1, 3.5);

        // control: the plain death of a pig drops pork and no diamond
        Pig control = tough(test, EntityType.PIG, spot.x, spot.y, spot.z);
        control.setHealth(1F);
        fake.attackEntity(control);
        test.assertTrue(control.isDeadOrDying(), "the blow killed the pig");
        List<ItemEntity> plain = itemsAround(test, spot, 2D);
        test.assertTrue(has(plain, Items.PORKCHOP) && !has(plain, Items.DIAMOND), "control: pork, no diamond: " + plain);
        test.assertTrue(!level.getEntitiesOfClass(ExperienceOrb.class, new AABB(test.absoluteVec(spot), test.absoluteVec(spot)).inflate(3D)).isEmpty(),
                "control: experience orbs (the pig was killed by a player)");
        plain.forEach(ItemEntity::discard);
        control.discard();

        List<Object[]> seen = new ArrayList<>();
        int[] mode = { 0 }; // 0 add a diamond, 1 remove the pork, 2 clear, 3 cancel
        TinkererEvents.LivingDrops listener = (victim, source, drops, looting, recentlyHit) -> {
            if (!(victim instanceof Pig) || source.getEntity() != fake) return EventResult.pass();
            seen.add(new Object[] { new ArrayList<>(drops), looting, recentlyHit, drops.stream().anyMatch(e -> e.isAlive()), drops.stream().allMatch(e -> e.level().getEntity(e.getId()) == null) });
            switch (mode[0]) {
                case 0 -> drops.add(new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), new ItemStack(Items.DIAMOND)));
                case 1 -> drops.removeIf(e -> e.getItem().is(Items.PORKCHOP));
                case 2 -> drops.clear();
                case 3 -> {
                    return EventResult.interruptFalse();
                }
                default -> { }
            }
            return EventResult.pass();
        };
        TinkererEvents.LIVING_DROPS.register(listener);
        try {
            test.assertTrue(TinkererEvents.hasLivingDropsListeners(), "the listener is registered");
            for (int scenario = 0; scenario < 4; scenario++) {
                mode[0] = scenario;
                seen.clear();
                Pig pig = tough(test, EntityType.PIG, spot.x, spot.y, spot.z);
                pig.setHealth(1F);
                fake.attackEntity(pig);
                String what = "scenario " + scenario + ": ";
                test.assertTrue(pig.isDeadOrDying() && seen.size() == 1, what + "the death posted the event once: " + seen.size());
                Object[] call = seen.get(0);
                @SuppressWarnings("unchecked")
                List<ItemEntity> offered = (List<ItemEntity>) call[0];
                test.assertTrue(has(offered, Items.PORKCHOP), what + "the loot table's pork was in the list the listener got");
                test.assertTrue((Integer) call[1] == 3, what + "the looting level of the sword: " + call[1]);
                test.assertTrue((Boolean) call[2], what + "recently hit by a player");
                test.assertTrue((Boolean) call[4], what + "and none of the offered entities was in the world yet");
                List<ItemEntity> world = itemsAround(test, spot, 2D);
                switch (scenario) {
                    case 0 -> test.assertTrue(has(world, Items.PORKCHOP) && has(world, Items.DIAMOND), what + "pork and the added diamond: " + world);
                    case 1 -> test.assertTrue(!has(world, Items.PORKCHOP) && world.isEmpty(), what + "the removed pork never appeared: " + world);
                    case 2 -> test.assertTrue(world.isEmpty(), what + "a cleared list drops nothing: " + world);
                    default -> test.assertTrue(world.isEmpty(), what + "a cancel drops nothing: " + world);
                }
                test.assertTrue(!level.getEntitiesOfClass(ExperienceOrb.class, new AABB(test.absoluteVec(spot), test.absoluteVec(spot)).inflate(3D)).isEmpty(),
                        what + "experience is not an item drop and still comes");
                world.forEach(ItemEntity::discard);
                level.getEntitiesOfClass(ExperienceOrb.class, new AABB(test.absoluteVec(spot), test.absoluteVec(spot)).inflate(3D)).forEach(ExperienceOrb::discard);
                pig.discard();
            }
        } finally {
            TinkererEvents.LIVING_DROPS.unregister(listener);
        }
        test.assertTrue(!TinkererEvents.hasLivingDropsListeners(), "the listener is gone again");

        // a death after that is the plain one again
        Pig after = tough(test, EntityType.PIG, spot.x, spot.y, spot.z);
        after.setHealth(1F);
        fake.attackEntity(after);
        List<ItemEntity> world = itemsAround(test, spot, 2D);
        test.assertTrue(has(world, Items.PORKCHOP) && !has(world, Items.DIAMOND), "unregistered: the plain drops again");
        world.forEach(ItemEntity::discard);
        after.discard();
        fake.discard();
        test.succeed();
    }

    /** The inventory of a dying player is part of the drops (via {@code ServerPlayer.drop}); a drop by hand is not. */
    public static void livingDropsCoverAPlayersInventoryButNotADropByHand(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        Vec3 spotA = new Vec3(2.5, 1, 2.5);
        Vec3 spotB = new Vec3(5.5, 1, 5.5);
        ServerPlayer observed = player(test, spotA.x, spotA.y, spotA.z);
        ServerPlayer cleared = player(test, spotB.x, spotB.y, spotB.z);
        observed.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 5));
        cleared.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 5));
        List<ItemStack> offered = new ArrayList<>();
        TinkererEvents.LivingDrops listener = (victim, source, drops, looting, recentlyHit) -> {
            if (victim == observed) drops.forEach(e -> offered.add(e.getItem().copy()));
            if (victim == cleared) {
                drops.forEach(e -> offered.add(e.getItem().copy()));
                drops.clear();
            }
            return EventResult.pass();
        };
        TinkererEvents.LIVING_DROPS.register(listener);
        try {
            // a drop by the player's own hand is not a death drop: the item appears at once
            ItemEntity dropped = observed.drop(new ItemStack(Items.STICK), false, false);
            test.assertTrue(dropped != null && dropped.isAlive() && has(itemsAround(test, spotA, 1.5D), Items.STICK), "a drop by hand is not captured");
            itemsAround(test, spotA, 1.5D).forEach(ItemEntity::discard);

            DamageSource generic = level.damageSources().generic();
            observed.die(generic);
            cleared.die(generic);
            test.assertTrue(offered.stream().filter(s -> s.is(Items.COBBLESTONE)).count() == 2,
                    "both deaths offered their cobblestone to the listener: " + offered);
            test.assertTrue(has(itemsAround(test, spotA, 1.5D), Items.COBBLESTONE), "the player the listener left alone dropped his inventory");
            test.assertTrue(!has(itemsAround(test, spotB, 1.5D), Items.COBBLESTONE), "the player whose list was cleared dropped nothing");
        } finally {
            TinkererEvents.LIVING_DROPS.unregister(listener);
            itemsAround(test, spotA, 1.5D).forEach(ItemEntity::discard);
            itemsAround(test, spotB, 1.5D).forEach(ItemEntity::discard);
        }
        finish(test, observed, cleared);
    }

    // ---- FloatingTicks ---------------------------------------------------------------------------------------------

    /** The accessor on a real connection against an independent read of the private field; a player without a connection is a no-op. */
    @SuppressWarnings("removal")
    public static void floatingTicksReadAndResetTheRealCounter(GameTestHelper test) {
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        test.assertTrue(player.connection != null, "the mock player has a connection");
        test.assertTrue(FloatingTicks.isAvailable(player), "the connection implements the accessor (the mixin was applied)");
        test.assertTrue(player.connection instanceof FloatingTicks.Access, "ServerGamePacketListenerImpl carries the interface");

        Field field = counterField();
        test.assertTrue(field != null, "the private counter field is found for the independent read");
        try {
            test.assertTrue(FloatingTicks.set(player, 79), "the counter can be written");
            test.assertTrue(FloatingTicks.get(player) == 79, "and read back: " + FloatingTicks.get(player));
            test.assertTrue(field.getInt(player.connection) == 79, "the field itself holds it: " + field.getInt(player.connection));

            field.setInt(player.connection, 33);
            test.assertTrue(FloatingTicks.get(player) == 33, "a write to the field shows in the accessor");

            FloatingTicks.RESET.accept(player);
            test.assertTrue(FloatingTicks.get(player) == 0 && field.getInt(player.connection) == 0, "RESET sets it to zero");
        } catch (IllegalAccessException e) {
            test.fail("the counter field cannot be read: " + e);
        }

        // vanilla's own tick puts a counter of a connection that is not floating back to zero: the field is the one it uses
        FloatingTicks.set(player, 50);
        player.connection.tick();
        test.assertTrue(FloatingTicks.get(player) == 0, "the connection's tick, for a player who is not floating, resets the same counter: " + FloatingTicks.get(player));

        // a player that has no connection (just created, or a fake player without one): nothing to read, nothing to write
        ServerPlayer bare = new ServerPlayer(test.getLevel().getServer(), test.getLevel(), new GameProfile(UUID.randomUUID(), "[TTBare]"),
                ClientInformation.createDefault());
        test.assertTrue(bare.connection == null, "the new player has no connection");
        test.assertTrue(!FloatingTicks.isAvailable(bare) && FloatingTicks.get(bare) == -1 && !FloatingTicks.set(bare, 5), "no connection: -1 and false");
        FloatingTicks.reset(bare); // must not throw
        bare.discard();

        // the fake player of the Animation Tablet has a silent connection of the same class
        TinkererFakePlayer fake = fakePlayer(test, ItemStack.EMPTY);
        test.assertTrue(FloatingTicks.set(fake, 7) && FloatingTicks.get(fake) == 7, "the fake player's silent connection works as well");
        fake.discard();
        finish(test, player);
    }

    private static Field counterField() {
        for (String name : new String[] { "aboveGroundTickCount", "field_14138" }) {
            try {
                Field field = ServerGamePacketListenerImpl.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException | RuntimeException notThisName) {
                // the other mapping
            }
        }
        return null;
    }
}
