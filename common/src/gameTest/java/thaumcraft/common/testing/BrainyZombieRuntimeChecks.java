package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.entities.monster.EntityBrainyZombie;
import thaumcraft.common.entities.monster.EntityGiantBrainyZombie;

/** Native registry/spawn/attributes, original rage/size/save quirks, paid villager cure and real gated death drops.
 * Bare mobs, a supported restored floor, seeds and high Looting are explicit fixture inputs, not survival acquisition. */
public final class BrainyZombieRuntimeChecks {
    private BrainyZombieRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        var fixture = new Fixture(test);
        try { fixture.start(); fixture.attributesAndRage(); fixture.curing(); fixture.deathLoot(); fixture.population(); }
        finally { fixture.close(); }
        test.succeed();
    }
    private static final class Fixture {
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos origin;
        final AABB bounds;
        final List<Entity> entities = new ArrayList<>();
        final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        final EmbeddedChannel channel = new EmbeddedChannel(connection);
        final ServerPlayer player;
        final boolean oldLoot;
        Fixture(GameTestHelper test) {
            this.test = test; level = test.getLevel(); origin = test.absolutePos(new BlockPos(3, 80, 3)); bounds = new AABB(origin).inflate(12);
            oldLoot = level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "brainy-" + UUID.randomUUID().toString().substring(0, 6)), ClientInformation.createDefault());
        }
        void start() {
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -1, -8), origin.offset(8, 6, 8))) saved.put(pos.immutable(), level.getBlockState(pos));
            forced.addAll(GolemChunks.hold(level, saved.keySet()));
            saved.forEach((pos, state) -> level.setBlock(pos, pos.getY() == origin.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16));
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.teleportTo(origin.getX() + .5, origin.getY(), origin.getZ() - 5);
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(true, level.getServer());
        }
        <T extends LivingEntity> T spawn(EntityType<T> type, int dx) {
            T mob = type.create(level); test.assertTrue(mob != null, "registered native entity " + type);
            mob.setPos(origin.getX() + dx + .5, origin.getY(), origin.getZ() + .5);
            mob.setNoGravity(true); if (mob instanceof net.minecraft.world.entity.Mob ai) ai.setNoAi(true);
            // The 1.7 original rolled nextInt100 of the world random for every EntityMob that
            // joined the world (oracle EventHandlerEntity.entitySpawns:391). The fixture declares
            // that draw, so the exact original attributes of the Brainy stay deterministic.
            test.assertTrue(ChampionJoinRoll.join(level, mob), "actual registered hostile enters the native world index");
            entities.add(mob); return mob;
        }
        void attributesAndRage() {
            var brainy = spawn(ThaumcraftMonsters.BRAINY_ZOMBIE.get(), 0);
            test.assertTrue(brainy.getMaxHealth() == 25 && brainy.getAttributeValue(Attributes.ATTACK_DAMAGE) == 5
                            && brainy.getAttributeValue(Attributes.FOLLOW_RANGE) == 40 && brainy.getAttributeValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE) == 0
                            && brainy.getArmorValue() == 5, "Brainy has original health/damage/follow/reinforcement/armor values");
            brainy.getAttribute(Attributes.ARMOR).setBaseValue(30); test.assertTrue(brainy.getArmorValue() == 20, "Brainy total armor is capped at20"); brainy.getAttribute(Attributes.ARMOR).setBaseValue(2);
            for (var type : List.of(ThaumcraftMonsters.BRAINY_ZOMBIE.get(), ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE.get()))
                test.assertTrue(type.is(EntityTypeTags.UNDEAD) && type.is(EntityTypeTags.SENSITIVE_TO_SMITE) && type.is(EntityTypeTags.INVERTED_HEALING_AND_HARM), "loaded original undead traits " + type);
            var giant = spawn(ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE.get(), 3);
            test.assertTrue(giant.getMaxHealth() == 60 && giant.getAttributeValue(Attributes.ATTACK_DAMAGE) == 7 && giant.getAnger() == 1, "Giant starts with60/7/anger1");
            size(giant, .6F, 1.8F, "original final-Zombie.setSize constructor quirk leaves the actual adult box unchanged");
            giant.setInvulnerable(true);
            test.assertTrue(!giant.hurt(level.damageSources().mobAttack(brainy), 1) && Math.abs(giant.getAnger() - 1.1F) < .00001,
                    "refused damage still increases original Giant anger");
            giant.aiStep();
            test.assertTrue(Math.abs(giant.getAnger() - 1.098F) < .00001 && Math.abs(giant.getAttributeValue(Attributes.ATTACK_DAMAGE) - 7.49) < .0001,
                    "Giant living update decays anger .002 and derives damage7+(anger-1)*5");
            size(giant, .6F, 1.8F, "anger changes only stored dimensions, as executable1.7 oracle");
            giant.setBaby(false); size(giant, 1.3788F, 4.1364F, "explicit adult child-size setter applies the stored rage dimensions");
            giant.setBaby(true); size(giant, .6894F, 2.0682F, "baby setter applies one half of the same stored dimensions"); giant.setBaby(false);
            for (int i = 0; i < 20; i++) giant.hurt(level.damageSources().mobAttack(brainy), 1);
            test.assertTrue(giant.getAnger() == 2, "refused hits cap anger at2");
            giant.setAnger(1.0001F); giant.aiStep();
            test.assertTrue(giant.getAnger() < 1 && giant.getAttributeValue(Attributes.ATTACK_DAMAGE) < 7, "original .002 decay may undershoot anger1 without clamping");
            CompoundTag tag = new CompoundTag(); giant.addAdditionalSaveData(tag);
            test.assertTrue(!tag.contains("Anger") && !tag.contains("anger"), "Giant anger is intentionally not persisted");
            var loaded = spawn(ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE.get(), -3); loaded.readAdditionalSaveData(tag);
            test.assertTrue(loaded.getAnger() == 1, "native save/load resets the unpersisted original anger");
            size(loaded, .6F, 1.8F, "fresh adult reload retains original unapplied size quirk");
            var pig = spawn(EntityType.PIG, -5); float health = pig.getHealth(); brainy.doHurtTarget(pig);
            test.assertTrue(pig.getHealth() == health - 5, "native Brainy melee uses its actual five attack damage");
            for (Entity entity : entities) entity.discard(); entities.clear();
        }
        void size(Entity entity, float width, float height, String why) {
            test.assertTrue(Math.abs(entity.getBbWidth() - width) < .0001 && Math.abs(entity.getBbHeight() - height) < .0001,
                    why + ": got " + entity.getBbWidth() + "x" + entity.getBbHeight());
        }
        void curing() {
            var zombie = spawn(ThaumcraftMonsters.BRAINY_ZOMBIE.get(), 0);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE, 2));
            zombie.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1000));
            test.assertTrue(!zombie.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction() && player.getMainHandItem().getCount() == 2 && !zombie.isConverting(),
                    "normal Brainy refuses curing and does not consume an apple");
            zombie.setVillagerVariant(true); CompoundTag variant = new CompoundTag(); zombie.addAdditionalSaveData(variant);
            var restored = spawn(ThaumcraftMonsters.BRAINY_ZOMBIE.get(), 3); restored.readAdditionalSaveData(variant);
            test.assertTrue(restored.isVillagerVariant(), "old IsVillager flag survives native NBT"); restored.discard();
            test.assertTrue(zombie.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction() && player.getMainHandItem().getCount() == 1
                            && zombie.isConverting() && !zombie.hasEffect(MobEffects.WEAKNESS), "actual curable variant pays one apple and starts conversion");
            CompoundTag conversion = new CompoundTag(); zombie.addAdditionalSaveData(conversion);
            test.assertTrue(conversion.getInt("ConversionTime") >= 3600 && conversion.getInt("ConversionTime") <= 6000, "original3600..6000 native conversion timer");
            conversion.putInt("ConversionTime", 1); zombie.readAdditionalSaveData(conversion); zombie.tick();
            var villagers = level.getEntitiesOfClass(Villager.class, bounds);
            test.assertTrue(zombie.isRemoved() && villagers.size() == 1 && !villagers.get(0).isBaby(), "a real final conversion tick produces one native adult villager");
            villagers.forEach(Entity::discard); player.getInventory().clearContent();
        }
        static long allFleshSeed(int attempts) {
            for (long seed = 0; seed < 1000000; seed++) {
                RandomSource random = RandomSource.create(seed); boolean all = true;
                for (int i = 0; i < attempts; i++) if (!random.nextBoolean()) { all = false; break; }
                if (all && random.nextInt(10) <= 4) return seed;
            }
            throw new AssertionError("No declared all-flesh world RNG seed found");
        }
        /**
         * The reduced primals of the original giant brainy zombie, in the original order.
         *
         * <p>Oracle {@code thaumcraft/common/config/ConfigAspects.java:89} registers UNDEAD 4,
         * MAN 2, MIND 1 and EARTH 2; the reduction to the primals of
         * {@code ResearchManager.reduceToPrimals} (oracle
         * {@code thaumcraft/common/lib/research/ResearchManager.java:466-495}) leaves air 6,
         * order 6, water 12, earth 14, entropy 7 and fire 3.</p>
         */
        private static final int[] GIANT_ORB_AMOUNTS = {6, 6, 12, 14, 7, 3};
        /**
         * The all-flesh seed of a death that drops the original aspect orbs.
         *
         * <p>Oracle {@code thaumcraft/common/lib/events/EventHandlerEntity.java:508-516}: the death
         * of a mob that was hit by a player draws one world boolean per primal aspect of the mob
         * and, for every true roll, the value of that aspect, BEFORE the loot of the same death.
         * The seed of the fixture therefore has to start behind those draws.</p>
         */
        static long allFleshSeedAfterOrbs(int attempts, int[] orbAmounts) {
            for (long seed = 0; seed < 1000000; seed++) {
                RandomSource random = RandomSource.create(seed);
                for (int amount : orbAmounts) if (random.nextBoolean()) random.nextInt(amount);
                boolean all = true;
                for (int i = 0; i < attempts; i++) if (!random.nextBoolean()) { all = false; break; }
                if (all && random.nextInt(10) <= 4) return seed;
            }
            throw new AssertionError("No declared all-flesh world RNG seed found");
        }
        void clearDrops() { level.getEntitiesOfClass(ItemEntity.class, bounds).forEach(Entity::discard); }
        int amount(net.minecraft.world.item.Item item) { return level.getEntitiesOfClass(ItemEntity.class, bounds).stream().map(ItemEntity::getItem).filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum(); }
        void deathLoot() {
            var brain = ThaumcraftContent.MATERIALS.get("zombie_brain").get();
            var emission = spawn(ThaumcraftMonsters.BRAINY_ZOMBIE.get(), 0);
            level.random.setSeed(98765); emission.getRandom().setSeed(54321);
            var drop = emission.spawnAtLocation(new ItemStack(Items.ROTTEN_FLESH), 1.5F);
            test.assertTrue(drop != null && drop.getDeltaMovement().x >= -.1F && drop.getDeltaMovement().x <= .1F
                            && drop.getDeltaMovement().y == (double) .2F && drop.getDeltaMovement().z >= -.1F && drop.getDeltaMovement().z <= .1F,
                    "actual native loot admission retains the original independent float item motion");
            test.assertTrue(level.random.nextLong() == RandomSource.create(98765).nextLong()
                            && emission.getRandom().nextLong() == RandomSource.create(54321).nextLong(),
                    "drop motion consumes neither world nor mob gameplay RNG before the next flesh/brain/rare roll");
            emission.discard(); clearDrops();
            clearDrops(); var ordinary = spawn(ThaumcraftMonsters.BRAINY_ZOMBIE.get(), 0); level.random.setSeed(allFleshSeed(3));
            ordinary.hurt(level.damageSources().generic(), 1000);
            test.assertTrue(ordinary.isDeadOrDying() && amount(Items.ROTTEN_FLESH) == 3 && amount(brain) == 1,
                    "actual environmental Brainy death emits three independent flesh successes and its own brain, without ordinary Zombie duplicate or player gating");
            var brainDrop = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().filter(item -> item.getItem().is(brain)).findFirst().orElseThrow();
            test.assertTrue(Math.abs(brainDrop.getY() - (origin.getY() + 1.5)) < .0001, "Brainy brain emits at original1.5 drop height");
            clearDrops(); var giant = spawn(ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE.get(), 0); level.random.setSeed(allFleshSeed(12)); giant.hurt(level.damageSources().generic(), 1000);
            test.assertTrue(amount(Items.ROTTEN_FLESH) == 24 && amount(brain) == 1, "actual Giant environmental death retains both six-boolean loops, maximum24 flesh plus own brain");
            brainDrop = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().filter(item -> item.getItem().is(brain)).findFirst().orElseThrow();
            test.assertTrue(Math.abs(brainDrop.getY() - (origin.getY() + 2)) < .0001, "Giant brain emits at original2 drop height");
            clearDrops(); var sword = new ItemStack(Items.DIAMOND_SWORD);
            // Explicit addon-level Looting input makes every original rare gate pass, irrespective of sound/equipment entity-RNG draws.
            sword.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), 200); player.setItemInHand(InteractionHand.MAIN_HAND, sword);
            var rare = spawn(ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE.get(), 0); level.random.setSeed(allFleshSeedAfterOrbs(12, GIANT_ORB_AMOUNTS)); rare.hurt(level.damageSources().playerAttack(player), 1000);
            int extras = amount(Items.CARROT) + amount(Items.POTATO) + amount(ThaumcraftContent.MATERIALS.get("thaumium_ingot").get()) + amount(ThaumcraftContent.MATERIALS.get("amber_gem").get());
            test.assertTrue(amount(Items.ROTTEN_FLESH) == 24 && amount(brain) == 1 && extras == 1,
                    "native credited Giant death emits exactly one original rare-pool item; Looting never multiplies custom flesh");
            clearDrops(); level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false, level.getServer());
            var blocked = spawn(ThaumcraftMonsters.BRAINY_ZOMBIE.get(), 0); blocked.hurt(level.damageSources().playerAttack(player), 1000);
            test.assertTrue(level.getEntitiesOfClass(ItemEntity.class, bounds).isEmpty(), "native doMobLoot gate suppresses all custom zombie loot");
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(true, level.getServer()); player.getInventory().clearContent();
        }
        void population() {
            var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
            for (var key : List.of(Biomes.PLAINS, Biomes.FOREST, Biomes.TAIGA, Biomes.JUNGLE)) {
                var entries = biomes.get(key).getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap();
                test.assertTrue(entries.stream().anyMatch(entry -> entry.type == ThaumcraftMonsters.BRAINY_ZOMBIE.get() && entry.minCount == 1
                                && entry.maxCount == 1 && entry.getWeight().asInt() == 10), "native biome population uses original weight10/group1 in " + key);
            }
            var desert = biomes.get(Biomes.DESERT).getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap();
            test.assertTrue(desert.stream().noneMatch(entry -> entry.type == ThaumcraftMonsters.BRAINY_ZOMBIE.get()), "no invented all-Overworld Brainy expansion outside the original allowed list");
            long oldTime = level.getDayTime();
            try {
                level.setDayTime(6000); level.updateSkyBrightness();
                // Original EntityMob: saved sky against32, then combined light against8.
                // getBlockLightValue includes darkened skylight, not just the BLOCK channel.
                boolean consistent = true;
                for (long seed = 0; seed < 32; ++seed) {
                    var expected = RandomSource.create(seed);
                    int skyRoll = expected.nextInt(32);
                    int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, origin);
                    boolean expectedResult = skyLight > skyRoll ? false
                            : level.getMaxLocalRawBrightness(origin, level.isThundering() ? 10 : level.getSkyDarken()) <= expected.nextInt(8);
                    boolean actual = SpawnPlacements.checkSpawnRules(ThaumcraftMonsters.BRAINY_ZOMBIE.get(), level, MobSpawnType.NATURAL, origin, RandomSource.create(seed));
                    if (actual != expectedResult) { consistent = false;
                        System.out.println("LIGHTDIAG seed=" + seed + " sky=" + skyLight + " skyRoll=" + skyRoll + " block=" + level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, origin) + " expected=" + expectedResult + " actual=" + actual); }
                }
                test.assertTrue(consistent, "placement follows original saved-sky32 and combined-light8 rolls");
                level.setDayTime(18000); level.updateSkyBrightness(); boolean allowed = false;
                for (long seed = 0; seed < 500 && !allowed; seed++) allowed = SpawnPlacements.checkSpawnRules(ThaumcraftMonsters.BRAINY_ZOMBIE.get(), level, MobSpawnType.NATURAL, origin, RandomSource.create(seed));
                test.assertTrue(allowed, "registered native placement admits a supported dark night cell through the original32/8 light rolls");
            } finally { level.setDayTime(oldTime); level.updateSkyBrightness(); }
        }
        void close() {
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(oldLoot, level.getServer());
            for (Entity entity : entities) entity.discard(); clearDrops();
            level.getEntitiesOfClass(Villager.class, bounds).forEach(Entity::discard);
            saved.forEach((pos, state) -> level.setBlock(pos, state, 2 | 16)); for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            try { if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player); }
            finally { player.discard(); channel.finishAndReleaseAll(); }
        }
    }
}
