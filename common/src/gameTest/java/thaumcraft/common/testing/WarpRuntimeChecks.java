package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.events.common.PlayerEvent;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.common.entities.monster.EntityMindSpider;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;
import thaumcraft.common.lib.WarpEvents;
import thaumcraft.common.lib.WarpRules;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.potions.FluxEffects;
import thaumcraft.common.lib.research.PlayerKnowledge;

/** Native server dispatcher, original research thresholds, food mixin, cone targeting and real event mobs.
 * Random draws, a connected player and an empty supported floor are declared inputs; effects, rewards and entities are outputs.
 * Client rendering, acquisition of the ward and the later obelisk/Outer Lands population are separate acceptance work. */
public final class WarpRuntimeChecks {
    private WarpRuntimeChecks() {}

    private static final class Draws implements RandomSource {
        private final RandomSource base = RandomSource.create(20261020L);
        private final ArrayDeque<Integer> values = new ArrayDeque<>();
        Draws(int... input) { for (int value : input) values.add(value); }
        @Override public RandomSource fork() { return base.fork(); }
        @Override public PositionalRandomFactory forkPositional() { return base.forkPositional(); }
        @Override public void setSeed(long seed) { base.setSeed(seed); }
        @Override public int nextInt() { return values.isEmpty() ? 0 : values.remove(); }
        @Override public int nextInt(int bound) {
            int value = nextInt();
            if (value < 0 || value >= bound) throw new IllegalStateException("Invalid declared warp draw " + value + " for " + bound);
            return value;
        }
        @Override public long nextLong() { return base.nextLong(); }
        @Override public boolean nextBoolean() { return base.nextBoolean(); }
        @Override public float nextFloat() { return base.nextFloat(); }
        @Override public double nextDouble() { return base.nextDouble(); }
        @Override public double nextGaussian() { return base.nextGaussian(); }
    }

    public static void run(GameTestHelper test) {
        var fixture = new Fixture(test);
        try {
            fixture.start();
            fixture.dispatch();
            fixture.research();
            fixture.foodAndWard();
            fixture.effectTicks();
            fixture.gaze();
            fixture.mobs();
            fixture.ordinaryScheduledTick();
        } catch (Throwable failure) { fixture.close(); throw failure; }
    }

    private static final class Fixture {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final BlockPos origin;
        private final ServerPlayer player;
        private final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        private final EmbeddedChannel channel = new EmbeddedChannel(connection);
        private final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        private final List<ChunkPos> forced = new ArrayList<>();
        private final List<Entity> owned = new ArrayList<>();
        private final boolean oldWuss = WarpEvents.isWussMode();
        private final AABB bounds;
        private boolean closed;

        Fixture(GameTestHelper test) {
            this.test = test; level = test.getLevel(); origin = test.absolutePos(new BlockPos(3, 96, 3));
            bounds = new AABB(origin).inflate(28, 4, 28);
            player = new ServerPlayer(level.getServer(), level,
                    new GameProfile(UUID.randomUUID(), "warp-" + UUID.randomUUID().toString().substring(0, 6)), ClientInformation.createDefault());
        }
        void start() {
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            WarpEvents.setWussMode(false);
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true);
            player.teleportTo(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-26, -1, -26), origin.offset(26, 2, 26))) saved.put(pos.immutable(), level.getBlockState(pos));
            forced.addAll(GolemChunks.hold(level, saved.keySet()));
            saved.forEach((pos, state) -> level.setBlock(pos, pos.getY() == origin.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16));
            test.assertTrue(player.level() == level && player.connection != null, "native connected player and loaded supported event floor");
        }
        PlayerKnowledge knowledge() { return KnowledgeAccess.get(player); }
        void clean() {
            player.removeAllEffects(); player.getInventory().clearContent(); knowledge().reset();
            for (Entity entity : level.getEntities((Entity) null, bounds, e -> e instanceof EntityMindSpider || e instanceof EntityEldritchGuardian || e instanceof EntityEldritchOrb)) entity.discard();
        }
        WarpRules.Decision event(int severity) {
            clean(); knowledge().setWarpTemp(150); knowledge().setWarpCounter(150);
            // The original rolled the champion join roll of every mob that joined the world, and
            // the mind spider of the original spider class is a champion candidate, so the mobs
            // of the event keep their own numbers only while the declared roll keeps them
            // ordinary.
            var decision = ChampionJoinRoll.declared(level, ChampionJoinRoll.DECLARED,
                    () -> WarpEvents.checkWarpEvent(player, new Draws(0, severity)));
            test.assertTrue(decision.triggered() && decision.effectiveWarp() == 100 && decision.severity() == severity,
                    "accepted native dispatch of severity " + severity + ": " + decision);
            test.assertTrue(knowledge().getWarpTemp() == 149 && knowledge().getWarpCounter() == 125,
                    "accepted dispatch decays temporary warp once and original counter by 2*sqrt(counter)");
            return decision;
        }
        void effect(Holder<MobEffect> effect, int duration, int amplifier, boolean uncurable) {
            var active = player.getEffect(effect);
            test.assertTrue(active != null && active.getDuration() == duration && active.getAmplifier() == amplifier
                    && FluxEffects.isUncurable(active) == uncurable, "native effect duration/amplifier/curatives " + effect + ": " + active);
        }
        void dispatch() {
            // Every original branch executes against the native world. The gaps consume warp but add nothing.
            for (int severity : new int[]{0, 5, 85}) {
                event(severity); test.assertTrue(player.getActiveEffects().isEmpty() && pool() == 0, "original empty severity " + severity);
            }
            event(1); test.assertTrue(pool() == 1, "one primal reward from the declared low research draw");
            event(9); test.assertTrue(player.getActiveEffects().isEmpty(), "noise branch has no server status");
            event(13); effect(ThaumcraftFlux.visExhaust(), 5000, 3, true);
            event(17); effect(ThaumcraftWarp.effect(ThaumcraftWarp.THAUMARHIA), 1000, 0, true);
            event(21); effect(ThaumcraftWarp.effect(ThaumcraftWarp.UNNATURAL_HUNGER), 5000, 3, true);
            event(25); test.assertTrue(player.getActiveEffects().isEmpty(), "following branch has no server status");
            event(29); guardians(1);
            event(33); effect(ThaumcraftWarp.effect(ThaumcraftWarp.BLURRED_VISION), 1000, 0, false);
            event(37); effect(ThaumcraftWarp.effect(ThaumcraftWarp.SUN_SCORNED), 5000, 3, true);
            event(41); effect(MobEffects.DIG_SLOWDOWN, 1200, 3, false);
            event(45); effect(ThaumcraftFlux.infectiousVisExhaust(), 6000, 3, true);
            event(49); effect(MobEffects.NIGHT_VISION, 4000, 0, false);
            event(53); effect(ThaumcraftWarp.effect(ThaumcraftWarp.DEATH_GAZE), 6000, 3, true);
            event(57); spiders(true);
            event(61); test.assertTrue(player.getActiveEffects().isEmpty(), "watching branch has no server status");
            event(65); guardians(3);
            event(69); effect(MobEffects.BLINDNESS, 500, 0, false);
            for (int severity : new int[]{73, 75, 77, 80}) { event(severity); effect(ThaumcraftWarp.effect(ThaumcraftWarp.UNNATURAL_HUNGER), 6000, 3, true); }
            clean(); knowledge().setWarpSticky(2); knowledge().setWarpTemp(148); knowledge().setWarpCounter(150);
            WarpEvents.checkWarpEvent(player, new Draws(0, 76));
            test.assertTrue(knowledge().getWarpSticky() == 1 && knowledge().getWarpTemp() == 147, "clarity removes one sticky warp before temporary decay");
            event(81); test.assertTrue(pool() == 1, "high research branch native reward generation");
            event(89); spiders(false);
            event(93); guardians(6);
            clean(); knowledge().setWarpTemp(3); knowledge().setWarpCounter(3);
            var declined = WarpEvents.checkWarpEvent(player, new Draws(99));
            test.assertTrue(!declined.triggered() && knowledge().getWarpTemp() == 2 && knowledge().getWarpCounter() == 3,
                    "failed event roll still consumes one temporary warp without changing the counter");
        }
        int pool() { return knowledge().getAspectsDiscovered().values().stream().mapToInt(Integer::intValue).sum(); }
        void guardians(int amount) {
            var guardians = level.getEntitiesOfClass(EntityEldritchGuardian.class, bounds);
            test.assertTrue(guardians.size() == amount && guardians.stream().allMatch(e -> e.getTarget() == player && e.getMaxHealth() == 50),
                    "mist creates " + amount + " actual guardians targeting its player, got " + guardians.size());
        }
        void spiders(boolean harmless) {
            var spiders = level.getEntitiesOfClass(EntityMindSpider.class, bounds);
            test.assertTrue(spiders.size() == 50 && spiders.stream().allMatch(e -> e.isHarmless() == harmless && e.getTarget() == player
                            && e.getMaxHealth() == 1 && (!harmless || e.getViewer().equals(player.getGameProfile().getName()))),
                    "spider event creates fifty actual " + (harmless ? "viewer-scoped visions" : "hostile spiders") + ", got " + spiders.size());
        }
        void research() {
            for (int actual : new int[]{10, 11, 25, 26, 50, 51}) {
                clean(); knowledge().setWarpPerm(actual); knowledge().setWarpTemp(150); knowledge().setWarpCounter(150);
                WarpEvents.checkWarpEvent(player, new Draws(0, 0));
                test.assertTrue(knowledge().isResearchComplete("@BATHSALTS") == (actual > 10)
                                && knowledge().isResearchComplete("ELDRITCHMINOR") == (actual > 25)
                                && knowledge().isResearchComplete("ELDRITCHMAJOR") == (actual > 50), "strict actual-warp research thresholds at " + actual);
                int reward = pool();
                WarpEvents.checkWarpEvent(player, new Draws(0, 0));
                test.assertTrue(pool() == reward, "completed automatic researches do not grant their primal rewards again");
            }
            clean(); knowledge().setWarpTemp(100); knowledge().setWarpCounter(100);
            WarpEvents.checkWarpEvent(player, new Draws(0, 0));
            test.assertTrue(knowledge().getResearchComplete().isEmpty(), "temporary warp cannot unlock actual-warp researches");
            clean();
            test.assertTrue(WarpEvents.onCrafted(player, new ItemStack(ThaumcraftGolems.GOLEM_FLESH.get(), 3)) == 1
                            && knowledge().getWarpSticky() == 1 && knowledge().getWarpCounter() == 1,
                    "flesh golem craft adds sticky warp once, independent of produced count");
            test.assertTrue(WarpEvents.onCrafted(player, new ItemStack(Items.STONE)) == 0 && knowledge().getWarpSticky() == 1,
                    "ordinary output adds no crafting warp");
            clean(); var paidInputs = new SimpleContainer(0);
            PlayerEvent.CRAFT_ITEM.invoker().craft(player, new ItemStack(ThaumcraftGolems.GOLEM_FLESH.get(), 3), paidInputs);
            PlayerEvent.CRAFT_ITEM.invoker().craft(player, new ItemStack(thaumcraft.common.config.ThaumcraftWands.PRIMAL_STAFF_ROD.get()), paidInputs);
            test.assertTrue(knowledge().getWarpSticky() == 2 && knowledge().getWarpCounter() == 2,
                    "actual registered CRAFT_ITEM listener grants exactly once for each of two legitimate same-tick warped outputs");
            WarpEvents.setWussMode(true);
            PlayerEvent.CRAFT_ITEM.invoker().craft(player, new ItemStack(ThaumcraftGolems.GOLEM_FLESH.get()), paidInputs);
            test.assertTrue(knowledge().getWarpSticky() == 2, "actual CRAFT_ITEM listener respects wuss mode"); WarpEvents.setWussMode(false);
            clean(); knowledge().setWarpTemp(150); knowledge().setWarpCounter(150);
            var helmet = new ItemStack(ThaumcraftEquipment.FORTRESS_HELMET.get()); helmet.set(ThaumcraftEquipment.FORTRESS_MASK.get(), 0);
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
            var masked = WarpEvents.checkWarpEvent(player, new Draws(0, 32, 3));
            test.assertTrue(masked.severity() == 27 && masked.event() == WarpRules.Event.FOLLOWING
                            && level.getEntitiesOfClass(EntityEldritchGuardian.class, bounds).isEmpty(), "actual Grinning Devil mask0 subtracts 2+draw(4) before dispatch");
            clean(); knowledge().setWarpCounter(100); player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ThaumcraftEquipment.VOID_ROBE_CHESTPLATE.get()));
            var gear = WarpEvents.checkWarpEvent(player, new Draws(0, 0));
            test.assertTrue(gear.triggered() && !knowledge().isResearchComplete("@BATHSALTS"), "worn warping gear opens the event gate without becoming actual research warp");
        }
        void foodAndWard() {
            clean(); var hunger = ThaumcraftWarp.effect(ThaumcraftWarp.UNNATURAL_HUNGER);
            player.addEffect(FluxEffects.uncurable(new MobEffectInstance(hunger, 1000, 1, true, true)));
            player.eat(level, new ItemStack(Items.ROTTEN_FLESH));
            effect(hunger, 400, 0, true);
            player.eat(level, new ItemStack(Items.ROTTEN_FLESH));
            test.assertTrue(!player.hasEffect(hunger), "last actual rotten flesh uses the native food mixin and removes exhausted hunger");
            player.addEffect(FluxEffects.uncurable(new MobEffectInstance(hunger, 1000, 0, true, true)));
            player.eat(level, new ItemStack(Items.APPLE)); effect(hunger, 1000, 0, true);
            clean(); knowledge().setWarpTemp(100); knowledge().setWarpCounter(100); player.tickCount = 2000;
            player.addEffect(new MobEffectInstance(ThaumcraftWarp.effect(ThaumcraftWarp.WARP_WARD), 1000)); WarpEvents.playerTick(player);
            test.assertTrue(knowledge().getWarpTemp() == 100 && knowledge().getWarpCounter() == 100, "ward prevents the whole scheduled check and its decay");
            player.removeAllEffects(); WarpEvents.setWussMode(true); WarpEvents.playerTick(player);
            test.assertTrue(knowledge().getWarpTemp() == 100 && WarpEvents.onCrafted(player, new ItemStack(ThaumcraftGolems.GOLEM_FLESH.get())) == 0,
                    "wuss mode prevents scheduled and crafting warp");
            WarpEvents.setWussMode(false);
        }
        void effectTicks() {
            clean(); player.getFoodData().setExhaustion(0);
            ThaumcraftWarp.UNNATURAL_HUNGER.get().applyEffectTick(player, 3);
            test.assertTrue(Math.abs(player.getFoodData().getExhaustionLevel() - .1F) < .00001F, "native unnatural hunger tick adds .025*(amplifier+1) exhaustion");
            long seed = 0; while (RandomSource.create(seed).nextInt(15) != 0) seed++;
            level.random.setSeed(seed); ThaumcraftWarp.THAUMARHIA.get().applyEffectTick(player, 0);
            test.assertTrue(level.getBlockState(player.blockPosition()).is(ThaumcraftFlux.FLUX_GOO.get()), "native Thaumarhia tick rolls one in15 and places actual flux goo at the feet");
            level.setBlock(player.blockPosition(), Blocks.STONE.defaultBlockState(), 2 | 16);
            level.random.setSeed(seed); ThaumcraftWarp.THAUMARHIA.get().applyEffectTick(player, 0);
            test.assertTrue(level.getBlockState(player.blockPosition()).is(Blocks.STONE), "Thaumarhia never replaces an occupied feet block");
            level.setBlock(player.blockPosition(), Blocks.AIR.defaultBlockState(), 2 | 16);
            long oldTime = level.getDayTime();
            try {
                level.setDayTime(6000); level.updateSkyBrightness(); player.clearFire();
                float brightness = player.getLightLevelDependentMagicValue();
                test.assertTrue(brightness > .5F && level.canSeeSky(player.blockPosition()), "sun-scorned native fixture is genuinely in bright open sky");
                seed = 0; while (RandomSource.create(seed).nextFloat() * 30 >= (brightness - .4F) * 2) seed++;
                level.random.setSeed(seed); ThaumcraftWarp.SUN_SCORNED.get().applyEffectTick(player, 0);
                test.assertTrue(player.isOnFire() && player.getRemainingFireTicks() == 80, "native Sun Scorned bright-light roll ignites four seconds");
                level.setDayTime(18000); level.updateSkyBrightness(); player.clearFire(); player.setHealth(10);
                brightness = player.getLightLevelDependentMagicValue(); test.assertTrue(brightness < .25F, "sun-scorned native fixture is genuinely dark at midnight");
                seed = 0; while (RandomSource.create(seed).nextFloat() <= brightness * 2) seed++;
                level.random.setSeed(seed); ThaumcraftWarp.SUN_SCORNED.get().applyEffectTick(player, 0);
                test.assertTrue(player.getHealth() == 11 && !player.isOnFire(), "native Sun Scorned darkness roll heals one point");
            } finally { level.setDayTime(oldTime); level.updateSkyBrightness(); player.clearFire(); player.setHealth(player.getMaxHealth()); }
        }
        <T extends LivingEntity> T spawn(EntityType<T> type, double dx, double dy, double dz) {
            T mob = type.create(level); test.assertTrue(mob != null, "registered native type " + type);
            mob.setPos(player.getX() + dx, player.getY() + dy, player.getZ() + dz); mob.setNoGravity(true);
            ChampionJoinRoll.join(level, mob); owned.add(mob); return mob;
        }
        void gaze() {
            clean(); player.setYRot(0); player.setXRot(0);
            var front = spawn(EntityType.PIG, 0, player.getEyeHeight() - .45, 6);
            var behind = spawn(EntityType.PIG, 0, player.getEyeHeight() - .45, -6);
            var side = spawn(EntityType.PIG, 6, player.getEyeHeight() - .45, 0);
            player.addEffect(new MobEffectInstance(ThaumcraftWarp.effect(ThaumcraftWarp.DEATH_GAZE), 6000, 0)); WarpEvents.checkDeathGaze(player);
            test.assertTrue(front.hasEffect(MobEffects.WITHER) && front.getEffect(MobEffects.WITHER).getDuration() == 80
                            && front.getTarget() == player && front.getLastHurtByMob() == player && !behind.hasEffect(MobEffects.WITHER) && !side.hasEffect(MobEffects.WITHER),
                    "death gaze applies wither and retaliation only in the visible forward cone");
            front.discard(); behind.discard(); side.discard();
        }
        void mobs() {
            clean(); var pig = spawn(EntityType.PIG, 2, 0, 0);
            var vision = spawn(ThaumcraftWarp.MIND_SPIDER.get(), 0, 0, 2); vision.setHarmless(true); vision.setViewer("observer");
            float hp = pig.getHealth(); test.assertTrue(!vision.doHurtTarget(pig) && pig.getHealth() == hp && !vision.isPushable(), "harmless spider neither damages nor pushes");
            CompoundTag data = new CompoundTag(); vision.addAdditionalSaveData(data);
            var loaded = spawn(ThaumcraftWarp.MIND_SPIDER.get(), 0, 0, 3); loaded.readAdditionalSaveData(data); loaded.tickCount = 1201; loaded.tick();
            test.assertTrue(loaded.isHarmless() && loaded.getViewer().equals("observer") && !loaded.isRemoved(), "original harmless reload watcher/lifespan quirk survives native save/load");
            // ServerLevel increments tickCount before calling tick; direct native ticks require the declared age input.
            vision.tickCount = 1200; vision.tick(); test.assertTrue(!vision.isRemoved(), "harmless vision survives the exact 1200-tick boundary");
            vision.tickCount = 1201; vision.tick(); test.assertTrue(vision.isRemoved(), "unsaved harmless vision expires after 1200 ticks");
            var real = spawn(ThaumcraftWarp.MIND_SPIDER.get(), 0, 0, 4);
            test.assertTrue(real.doHurtTarget(pig) && pig.getHealth() < hp && real.getAttributeValue(Attributes.ATTACK_DAMAGE) == 1, "real one-health spider damages its target");
            real.setTarget(null); real.tickCount = 2 + real.getId() % 2; real.tick();
            test.assertTrue(real.getTarget() == player, "mind spider reacquires a nearby player with no vanilla spider light gate");
            var guardian = spawn(ThaumcraftWarp.ELDRITCH_GUARDIAN.get(), 0, 0, -8);
            test.assertTrue(guardian.getMaxHealth() == 50 && guardian.getAttributeValue(Attributes.ATTACK_DAMAGE) == 7 && guardian.isInvertedHealAndHarm(), "guardian native combat attributes and undead healing");
            test.assertTrue(guardian.getType().is(EntityTypeTags.UNDEAD) && guardian.getType().is(EntityTypeTags.INVERTED_HEALING_AND_HARM)
                            && guardian.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE), "loaded native tags preserve the original undead classification for taint, instant potions and Smite");
            thaumcraft.common.config.ThaumcraftInfusion.FLUX_TAINT.get().applyEffectTick(guardian, 0);
            test.assertTrue(guardian.getHealth() == 50, "actual Flux Taint tick leaves the undead guardian unharmed");
            guardian.hurt(guardian.damageSources().magic(), 8);
            test.assertTrue(Math.abs(guardian.getHealth() - 46) < .001, "guardian halves magic damage before native damage processing");
            guardian.restrictTo(origin, 16); CompoundTag home = new CompoundTag(); guardian.addAdditionalSaveData(home);
            var other = spawn(ThaumcraftWarp.ELDRITCH_GUARDIAN.get(), 8, 0, -8); other.readAdditionalSaveData(home);
            test.assertTrue(other.hasRestriction() && other.getRestrictCenter().equals(origin) && other.getRestrictRadius() == 16, "guardian home survives native entity save/load");
            test.assertTrue(guardian.isAlliedTo(other) && !guardian.canAttack(other), "original IEldritchMob alliance also reaches native ally targeting");
            // A fixed seed chooses the original ordinary ranged branch.
            guardian.getRandom().setSeed(5); guardian.performRangedAttack(player, 1);
            var orbs = level.getEntitiesOfClass(EntityEldritchOrb.class, bounds);
            test.assertTrue(orbs.size() == 1 && orbs.get(0).getOwner() == guardian, "guardian ranged branch creates its owned native orb");
            for (EntityEldritchOrb orb : orbs) {
                orb.tickCount = 100; orb.tick(); test.assertTrue(!orb.isRemoved(), "orb survives the exact age100 boundary");
                orb.tickCount = 101; orb.tick(); test.assertTrue(orb.isRemoved(), "orb expires after 100 native ticks");
            }
            pig.invulnerableTime = 0; float beforeOrb = pig.getHealth();
            var undead = spawn(EntityType.ZOMBIE, 2, 0, 1); undead.setNoAi(true);
            var impact = new EntityEldritchOrb(level, guardian); impact.setPos(pig.getX() - 1.5, pig.getY() + .35, pig.getZ());
            impact.setDeltaMovement(1, 0, 0); level.addFreshEntity(impact); owned.add(impact);
            for (int i = 0; i < 4 && !pig.hasEffect(MobEffects.WEAKNESS); i++) impact.tick();
            test.assertTrue(pig.hasEffect(MobEffects.WEAKNESS) && pig.getEffect(MobEffects.WEAKNESS).getDuration() == 160
                            && Math.abs(beforeOrb - pig.getHealth() - 7 * .666F) < .001F,
                    "native orb collision deals .666*owner attack to living targets in radius2 and applies weakness160");
            test.assertTrue(undead.getHealth() == undead.getMaxHealth() && !undead.hasEffect(MobEffects.WEAKNESS), "original undead exclusion protects the orb's undead faction");
            impact.discard();
            // Find an input seed for the original 10% branch; its output is still produced by the actual combat method.
            long sonicSeed = 0; while (RandomSource.create(sonicSeed).nextFloat() > .1F) sonicSeed++;
            guardian.getRandom().setSeed(sonicSeed); knowledge().reset(); player.removeAllEffects(); guardian.performRangedAttack(player, 1);
            test.assertTrue(player.hasEffect(MobEffects.WITHER) && player.getEffect(MobEffects.WITHER).getDuration() == 400
                            && knowledge().getWarpTemp() >= 1 && knowledge().getWarpTemp() <= 3 && knowledge().getWarpCounter() == knowledge().getTotalWarp(),
                    "guardian sonic branch withers400 and adds/synchronizes one to three temporary warp");
        }
        void ordinaryScheduledTick() {
            clean(); for (Entity entity : owned) entity.discard();
            // Embedded transport must join the native connection loop: Player.tick is reached through packetListener.tick -> doTick.
            test.assertTrue(level.getServer().getConnection().getConnections().add(connection), "fixture transport joins the native server connection loop");
            knowledge().setWarpTemp(150);
            // Every possible nextInt(100) passes sqrt(10000). Unrelated native-world random draws cannot make this test flaky.
            knowledge().setWarpCounter(10000); level.random.setSeed(20261020L);
            player.tickCount = 1999;
            test.assertTrue(knowledge().getWarpTemp() == 150 && knowledge().getWarpCounter() == 10000,
                    "ordinary scheduler starts with declared player age1999/warp150/counter10000 and seeded world RNG");
            // No direct WarpEvents call, event invoker or entity.tick: the connected player remains in PlayerList and the world.
            // Three ticks accommodate GameTest/world/connection phase order and still contain exactly one multiple of2000.
            test.runAfterDelay(3, () -> {
                try {
                    test.assertTrue(player.tickCount >= 2000 && player.tickCount < 2006,
                            "the connected player advanced across age2000 through ordinary server ticks, age=" + player.tickCount);
                    test.assertTrue(knowledge().getWarpTemp() == 149 && knowledge().getWarpCounter() == 9800,
                            "registered PLAYER_POST ran the accepted event and its temporary decay exactly once, temp="
                                    + knowledge().getWarpTemp() + ", counter=" + knowledge().getWarpCounter());
                    close(); test.succeed();
                } catch (Throwable failure) { close(); throw failure; }
            });
        }
        void close() {
            if (closed) return; closed = true;
            level.getServer().getConnection().getConnections().remove(connection);
            WarpEvents.setWussMode(oldWuss);
            for (Entity entity : owned) entity.discard();
            for (Entity entity : level.getEntities((Entity) null, bounds, e -> e instanceof EntityMindSpider || e instanceof EntityEldritchGuardian || e instanceof EntityEldritchOrb)) entity.discard();
            saved.forEach((pos, state) -> level.setBlock(pos, state, 2 | 16));
            for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            try { if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player); }
            finally { player.discard(); channel.finishAndReleaseAll(); }
        }
    }
}
