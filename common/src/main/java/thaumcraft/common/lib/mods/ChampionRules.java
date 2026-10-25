package thaumcraft.common.lib.mods;

import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.entities.IThaumcraftBoss;
import thaumcraft.common.entities.monster.EntityCultist;
import thaumcraft.common.entities.monster.EntityEldritchCrab;
import thaumcraft.common.entities.monster.EntityInhabitedZombie;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.entities.monster.EntityTaintacle;
import thaumcraft.common.entities.monster.EntityTaintSpider;
import thaumcraft.common.entities.monster.EntityWatcher;
import thaumcraft.common.entities.monster.EntityWisp;
import thaumcraft.common.lib.WarpEvents;
import thaumcraft.common.lib.ward.WardRules;

/**
 * Champion selection and tier maths of the original 1.7 bestiary, kept pure so it can be
 * unit tested without a world. The world facing half lives in {@link Adapter}, a mob calls
 * it from its own hooks.
 */
public final class ChampionRules {
    private ChampionRules() {}

    /** Original spawn roll bound: the 1.7 original rolled nextInt(100). */
    public static final int SPAWN_ROLL_BOUND = 100;
    /** Original health gate: a mob needed at least ten base max health to become a champion. */
    public static final float MIN_CHAMPION_HEALTH = 10.0f;
    /** The modifier id of a mob that is not a champion. */
    public static final int NO_CHAMPION = -1;
    /** Original champion levels of the 1.7 whitelist. */
    public static final int TIER_NONE = -1;
    /** Original champion level of a common mob, the vanilla zombie and spider. */
    public static final int TIER_COMMON = 0;
    /** Original champion level of an uncommon mob, the witch and the wisp. */
    public static final int TIER_UNCOMMON = 1;
    /** Original champion level of a rare mob, the taintacle and the watcher. */
    public static final int TIER_RARE = 2;
    /** Original champion level of an elite mob, the inhabited zombie. */
    public static final int TIER_ELITE = 3;
    /** Original champion level of a thaumcraft boss, which always became a champion. */
    public static final int TIER_BOSS = 200;

    /**
     * Original champion whitelist. The 1.7 original matched the entity class, so a
     * subclass inherited the level of its parent; the native ids carry that inherited
     * level here and {@link #tierFor(Class)} resolves the class of a mob as well.
     */
    public static final Map<String,Integer> CHAMPION_WHITELIST = Map.ofEntries(
            Map.entry("minecraft:zombie", TIER_COMMON),
            Map.entry("minecraft:spider", TIER_COMMON),
            // Oracle ConfigEntities:196 - the original whitelisted the class of the spider,
            // so the cave spider of the original was a champion candidate of the same level.
            Map.entry("minecraft:cave_spider", TIER_COMMON),
            Map.entry("minecraft:blaze", TIER_COMMON),
            Map.entry("minecraft:enderman", TIER_COMMON),
            Map.entry("minecraft:skeleton", TIER_COMMON),
            Map.entry("minecraft:witch", TIER_UNCOMMON),
            Map.entry("thaumcraft:brainy_zombie", TIER_COMMON),
            Map.entry("thaumcraft:giant_brainy_zombie", TIER_COMMON),
            Map.entry("thaumcraft:taint_spider", TIER_COMMON),
            Map.entry("thaumcraft:eldritch_crab", TIER_COMMON),
            Map.entry("thaumcraft:taintacle", TIER_RARE),
            Map.entry("thaumcraft:taintacle_small", TIER_RARE),
            Map.entry("thaumcraft:wisp", TIER_UNCOMMON),
            Map.entry("thaumcraft:pech", TIER_UNCOMMON),
            Map.entry("thaumcraft:cultist", TIER_UNCOMMON),
            Map.entry("thaumcraft:watcher", TIER_RARE),
            Map.entry("thaumcraft:inhabited_zombie", TIER_ELITE),
            // Original ConfigEntities:208 - the boss base was whitelisted with the level 200,
            // so a boss always became a champion, even while the champion mobs were off.
            Map.entry("thaumcraft:eldritch_warden", TIER_BOSS),
            Map.entry("thaumcraft:eldritch_golem", TIER_BOSS));

    /**
     * Original champion whitelist classes of the native port. The 1.7 original matched the
     * entity class (oracle {@code EventHandlerEntity.entitySpawns:409-414}), so every subclass
     * of a whitelisted class inherited the level of its parent and the native port resolves
     * the same way.
     *
     * <p>The class of the original zombie is not part of this list: 1.21.1 merged the zombie
     * villager into the class of the zombie, and the zombie villager of the original 1.7 was a
     * class of its own that the original never whitelisted, so the class of the zombie carries
     * no class semantics here and the native ids of the zombie family keep their levels.
     */
    private static final Map<Class<?>,Integer> CHAMPION_CLASS_WHITELIST = Map.ofEntries(
            Map.entry(Spider.class, TIER_COMMON),
            Map.entry(Blaze.class, TIER_COMMON),
            Map.entry(EnderMan.class, TIER_COMMON),
            Map.entry(Skeleton.class, TIER_COMMON),
            Map.entry(Witch.class, TIER_UNCOMMON),
            Map.entry(EntityEldritchCrab.class, TIER_COMMON),
            Map.entry(EntityTaintacle.class, TIER_RARE),
            Map.entry(EntityWisp.class, TIER_UNCOMMON),
            Map.entry(EntityInhabitedZombie.class, TIER_ELITE),
            Map.entry(EntityCultist.class, TIER_UNCOMMON),
            Map.entry(EntityWatcher.class, TIER_RARE),
            Map.entry(EntityPech.class, TIER_UNCOMMON),
            Map.entry(IThaumcraftBoss.class, TIER_BOSS));

    /**
     * Original spawn roll. The 1.7 original rolled nextInt(100) and shifted the roll by
     * the difficulty, the dimension, the biome and the dangerous location.
     */
    public static int spawnRoll(Random random, boolean championsEnabled, boolean easy, boolean hard,
                                boolean outerDimension, boolean hostileBiome, boolean dangerousLocation) {
        int roll = random.nextInt(SPAWN_ROLL_BOUND);
        if (easy || !championsEnabled) roll += 2;
        if (hard) roll -= championsEnabled ? 2 : 0;
        if (outerDimension) roll -= 3;
        if (hostileBiome) roll -= championsEnabled ? 2 : 1;
        if (dangerousLocation) roll -= championsEnabled ? 10 : 3;
        return roll;
    }

    /**
     * Original whitelist shift: the 1.7 original used the largest level minus one of the
     * whitelisted classes and never went below zero. Disabled champions kept the shift
     * of zero, only a boss kept its own.
     */
    public static int tierBonus(int tier, boolean championsEnabled) {
        if (tier == TIER_NONE) return 0;
        if (!championsEnabled && tier != TIER_BOSS) return 0;
        return Math.max(0, tier - 1);
    }

    /** Original champion decision: the shifted roll had to reach zero and the mob needed its health. */
    public static boolean becomesChampion(int roll, int tier, float baseMaxHealth, boolean championsEnabled) {
        if (tier == TIER_NONE) return false;
        if (baseMaxHealth < MIN_CHAMPION_HEALTH) return false;
        return roll - tierBonus(tier, championsEnabled) <= 0;
    }

    /**
     * Original health gate. The 1.7 original had no health tiers, a mob either reached
     * the threshold of ten base max health or it did not, so an eligible mob qualifies
     * for the common tier.
     */
    public static int tierForHealth(float baseMaxHealth, float threshold) {
        return baseMaxHealth >= threshold ? TIER_COMMON : TIER_NONE;
    }

    /** Original whitelist lookup of a native entity id. */
    public static int tierFor(String entityId) { return CHAMPION_WHITELIST.getOrDefault(entityId, TIER_NONE); }

    /**
     * Original whitelist lookup of the class of a mob. The 1.7 original matched the class of
     * the entity (oracle {@code EventHandlerEntity.entitySpawns:409-414}), so a subclass of a
     * whitelisted class inherited the level of its parent and the native port resolves the
     * same way.
     */
    public static int tierFor(Class<?> entityClass) {
        int tier = TIER_NONE;
        for (Map.Entry<Class<?>,Integer> entry : CHAMPION_CLASS_WHITELIST.entrySet()) {
            if (!entry.getKey().isAssignableFrom(entityClass)) continue;
            tier = Math.max(tier, entry.getValue());
        }
        return tier;
    }

    /**
     * Original whitelist lookup of a native mob: the id of the mob and the class of the mob.
     * The original matched the class of the entity, the native ids carry the same levels.
     */
    public static int tierFor(String entityId, Class<?> entityClass) {
        return Math.max(tierFor(entityId), tierFor(entityClass));
    }

    /**
     * Original warded champion ward. Oracle {@code EntityUtils.makeChampion:271-274} read the
     * base max health (SRG func_111125_b), not the total including the champion's thirty
     * bonus health. A spider's base sixteen health therefore grants eight ward.
     */
    public static int wardedChampionWard(AttributeInstance health) {
        return ChampionWiringRules.wardedChampionWard(health.getBaseValue());
    }

    /** Original modifier roll: the 1.7 original rolled nextInt(mods.length). */
    public static int rollModifierIndex(Random random) { return random.nextInt(ChampionModifier.mods.length); }

    /** Original modifier roll. */
    public static ChampionModifier rollModifier(Random random) { return ChampionModifier.mods[rollModifierIndex(random)]; }

    /**
     * Original creeper rule: a creeper champion was always bold, because the 1.7 original
     * forced the modifier id zero for a creeper.
     */
    public static ChampionModifier rollModifier(Random random, boolean creeper) {
        return creeper ? ChampionModifier.mods[ChampionModifier.BOLD] : rollModifier(random);
    }

    /**
     * The 1.7 original gave every champion exactly one modifier at every level, the level
     * only shifted the spawn roll, so this returns a single modifier.
     */
    public static List<ChampionModifier> rollModifiers(Random random, int tier) { return List.of(rollModifier(random)); }

    /** The original modifier of an id, null when the id is not an original modifier. */
    public static ChampionModifier modifier(int id) { return ChampionModifier.byId(id); }

    /** The number of original champion modifiers. */
    public static int modifierCount() { return ChampionModifier.mods.length; }

    /** Original champion reward: the extra experience a champion mob dropped. */
    public static int championExperience(Random random) { return 5 + random.nextInt(3); }

    /** Original champion reward: the loot bag tier a champion mob dropped. */
    public static int championLootBagTier(Random random, int lootingLevel) {
        return Math.min(2, (int) Math.floor((random.nextInt(9) + lootingLevel) / 5.0f));
    }

    /** The original world random, adapted to the pure effect api. */
    static final class WorldRandom extends java.util.Random {
        private final RandomSource source;
        WorldRandom(RandomSource source) { this.source = source; }
        @Override public float nextFloat() { return source.nextFloat(); }
        @Override public int nextInt(int bound) { return source.nextInt(bound); }
        @Override public boolean nextBoolean() { return source.nextBoolean(); }
    }

    /**
     * World facing champion adapter. A mob calls these from its own hooks: the promotion
     * when it becomes a champion, the tick trigger every update, the attack trigger when
     * it hurts a living target and the damage trigger when a living attacker hurts it.
     */
    public static final class Adapter {
        /** Native identity of the original champion health bonus modifier. */
        public static final ResourceLocation CHAMPION_HEALTH_ID = ResourceLocation.fromNamespaceAndPath("thaumcraft", "champion_health");
        /** Native identity of the original champion damage bonus modifier. */
        public static final ResourceLocation CHAMPION_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath("thaumcraft", "champion_damage");
        /** Native identity of the original bold movement speed bonus modifier. */
        public static final ResourceLocation BOLD_SPEED_ID = ResourceLocation.fromNamespaceAndPath("thaumcraft", "champion_bold_speed");
        /** Native identity of the original mighty damage bonus modifier. */
        public static final ResourceLocation MIGHTY_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath("thaumcraft", "champion_mighty_damage");
        /** Native identity of the original gore sound the infested champion played. */
        public static final ResourceLocation GORE_SOUND = ResourceLocation.fromNamespaceAndPath("thaumcraft", "gore");
        private Adapter() {}

        /**
         * Original makeChampion: rolls the original modifier and promotes the mob. The
         * returned id is the modifier the mob keeps, it is not one of the original
         * attribute values because the native champion attribute no longer exists.
         */
        public static int makeChampion(LivingEntity mob, RandomSource random, boolean creeper) {
            ChampionModifier modifier = ChampionRules.rollModifier(new WorldRandom(random), creeper);
            if (mob instanceof net.minecraft.world.entity.Mob entity) ChampionHooks.setChampionModifier(entity, modifier.id);
            promote(mob, modifier.id);
            return modifier.id;
        }

        /** Original spawn roll for a native world. */
        public static int spawnRoll(RandomSource random, boolean championsEnabled, boolean easy, boolean hard,
                                    boolean outerDimension, boolean hostileBiome, boolean dangerousLocation) {
            return ChampionRules.spawnRoll(new WorldRandom(random), championsEnabled, easy, hard, outerDimension, hostileBiome, dangerousLocation);
        }

        /**
         * Original promotion: the champion health bonus, the champion damage multiplier,
         * the promotion heal, the champion name and the bonus of the rolled modifier.
         *
         * <p>Oracle {@code EntityUtils.makeChampion:243-254} skipped the first four for a
         * boss, because a boss scales with the players that hunt it, and let the boss name
         * itself after its champion modifier instead.
         *
         * <p>The original removed the modifier of the champion before it applied it again
         * (oracle {@code EntityUtils.makeChampion:241-249}), so a second promotion of the
         * same modifier replaced the bonus instead of adding it twice. The native attribute
         * refuses a modifier that is already applied, so the port removes it first as well.
         */
        public static void promote(LivingEntity mob, int modifierId) {
            ChampionModifier modifier = ChampionModifier.byId(modifierId);
            if (modifier == null) return;
            AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
            AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (ChampionWiringRules.ordinaryChampionBonuses(mob instanceof IThaumcraftBoss)) {
                if (health != null) {
                    health.removeModifier(CHAMPION_HEALTH_ID);
                    health.addPermanentModifier(new AttributeModifier(CHAMPION_HEALTH_ID, modifier.healthBonus(), AttributeModifier.Operation.ADD_VALUE));
                }
                // Both original and native operation two multiply the total by one plus the
                // modifier amount: original amount 2 means three times the attack damage.
                if (damage != null) {
                    damage.removeModifier(CHAMPION_DAMAGE_ID);
                    damage.addPermanentModifier(new AttributeModifier(CHAMPION_DAMAGE_ID, modifier.damageMultiplier() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
                mob.heal(ChampionModifier.CHAMPION_HEAL);
                mob.setCustomName(championName(mob, modifier));
            } else if (mob instanceof IThaumcraftBoss boss) {
                boss.generateName();
            }
            if (modifier.id == ChampionModifier.MIGHTY && damage != null) {
                damage.removeModifier(MIGHTY_DAMAGE_ID);
                damage.addPermanentModifier(new AttributeModifier(MIGHTY_DAMAGE_ID, modifier.extraDamageMultiplier() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            // Original operation one adds a percentage of the base, alongside other base multipliers.
            if (modifier.id == ChampionModifier.BOLD) {
                AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.removeModifier(BOLD_SPEED_ID);
                    speed.addPermanentModifier(new AttributeModifier(BOLD_SPEED_ID, modifier.speedMultiplier() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
            if (modifier.id == ChampionModifier.WARDED && health != null) {
                // Oracle EntityUtils.makeChampion:271-274 - the warded champion adds half of its
                // maximum health to its absorption, never to its health. The original read the
                // base max health attribute (func_111125_b), excluding the champion bonus:
                // a spider receives eight absorption, not twenty three.
                // 1.21.1 clamps the absorption by the max absorption attribute, so the ward
                // raises that attribute first.
                WardRules.addWard(mob, (float) wardedChampionWard(health));
            }
        }

        /** Original champion name: the modifier name in front of the entity name. */
        public static Component championName(LivingEntity mob, ChampionModifier modifier) {
            return Component.translatable(modifier.localizationKey()).append(Component.literal(" ")).append(mob.getType().getDescription());
        }

        /** Original living update trigger: the warded and the undying champion act on every tick. */
        public static void tickChampion(LivingEntity mob, int modifierId) {
            ChampionModifier modifier = ChampionModifier.byId(modifierId);
            if (modifier == null || !modifier.ticks()) return;
            IChampionModifierEffect.Context context = IChampionModifierEffect.Context.tick(mob.tickCount, mob.getHealth(), baseMaxHealth(mob), mob.getAbsorptionAmount(), mob.invulnerableTime > 0);
            apply(mob, null, modifier.effect.performEffect(context, random(mob)));
        }

        /** Original attack trigger: the champion effects that answered a hit on a living target. */
        public static void attackChampion(LivingEntity mob, int modifierId, LivingEntity target, float amount) {
            ChampionModifier modifier = ChampionModifier.byId(modifierId);
            if (modifier == null || !modifier.attacks() || amount <= 0.0f || target == null) return;
            IChampionModifierEffect.Context context = IChampionModifierEffect.Context.attack(amount, target instanceof Player,
                    mob.tickCount, mob.getHealth(), baseMaxHealth(mob), mob.getAbsorptionAmount(), mob.invulnerableTime > 0);
            apply(mob, target, modifier.effect.performEffect(context, random(mob)));
        }

        /**
         * Original damage trigger: the champion effects that answered a living attacker.
         * The 1.7 original only answered a living attacker, so other damage is untouched.
         * The returned amount is the damage the champion takes.
         */
        public static float damageChampion(LivingEntity mob, int modifierId, DamageSource source, float amount) {
            ChampionModifier modifier = ChampionModifier.byId(modifierId);
            if (modifier == null || !modifier.defends()) return amount;
            Entity attacker = source == null ? null : source.getEntity();
            if (!(attacker instanceof LivingEntity living)) return amount;
            IChampionModifierEffect.Context context = IChampionModifierEffect.Context.damaged(amount,
                    source.is(DamageTypeTags.BYPASSES_ARMOR), "thorns".equalsIgnoreCase(source.getMsgId()), true,
                    living instanceof Player, mob.tickCount, mob.getHealth(), baseMaxHealth(mob), mob.getAbsorptionAmount(), mob.invulnerableTime > 0);
            IChampionModifierEffect.Result result = modifier.effect.performEffect(context, random(mob));
            apply(mob, living, result);
            return result.amount();
        }

        /** Applies an original champion effect result to the world. */
        private static void apply(LivingEntity mob, LivingEntity target, IChampionModifierEffect.Result result) {
            if (result.heal() > 0.0f) mob.heal(result.heal());
            if (result.absorption() > 0.0f) WardRules.addWard(mob, result.absorption());
            if (result.reflected() > 0.0f && target != null) target.hurt(mob.level().damageSources().thorns(mob), result.reflected());
            if (target != null) {
                if (result.status() != IChampionModifierEffect.Status.NONE) target.addEffect(new MobEffectInstance(status(result.status()), result.statusDuration()));
                if (result.fireSeconds() > 0) target.igniteForSeconds(result.fireSeconds());
                if (result.warp() > 0 && target instanceof ServerPlayer player) WarpEvents.addTemporaryWarp(player, result.warp());
            }
            if (result.spawnSpiderling() && mob.level() instanceof ServerLevel server) {
                EntityTaintSpider spiderling = new EntityTaintSpider(server);
                spiderling.setPos(mob.getX(), mob.getY() + mob.getBbHeight() / 2.0f, mob.getZ());
                spiderling.setYRot(server.getRandom().nextFloat() * 360.0f);
                server.addFreshEntity(spiderling);
                mob.playSound(SoundEvent.createVariableRangeEvent(GORE_SOUND), 0.5f, 1.0f);
            }
        }

        /** The original world random the champion effects used. */
        private static Random random(LivingEntity mob) { return new WorldRandom(mob.level().getRandom()); }

        /**
         * Original base max health (oracle EntityUtils:272 / ChampionModWarded:26,
         * func_111125_b), excluding attribute modifiers.
         */
        private static float baseMaxHealth(LivingEntity mob) {
            AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
            return health == null ? mob.getMaxHealth() : (float) health.getBaseValue();
        }

        /** The original status effect of a champion. */
        private static Holder<MobEffect> status(IChampionModifierEffect.Status status) {
            return switch (status) {
                case WEAKNESS -> MobEffects.WEAKNESS;
                case HUNGER -> MobEffects.HUNGER;
                case POISON -> MobEffects.POISON;
                // Original ChampionModGrim:26-30 - Potion.field_82731_v is the wither.
                case WITHER -> MobEffects.WITHER;
                case NONE -> null;
            };
        }
    }

    /**
     * Dedicated server safe client bridge. The 1.7 original showed the champion
     * particles on every living update; the client owns the particles themselves.
     */
    public static final class ClientFx {
        /** The client renderer of the original champion particles. */
        public interface Renderer { void render(Entity champion, IChampionModifierEffect.Fx fx); }
        private static Renderer renderer = (champion, fx) -> {};
        private ClientFx() {}
        /** The client bootstrap sets the renderer that draws the original particles. */
        public static void setRenderer(Renderer value) { renderer = value == null ? (champion, fx) -> {} : value; }
        /** Original client trigger: the original showed the champion particles on every living update. */
        public static void showChampionFx(Entity champion, int modifierId, RandomSource random) {
            ChampionModifier modifier = ChampionModifier.byId(modifierId);
            if (modifier == null) return;
            IChampionModifierEffect.Fx fx = modifier.effect.showFx(new WorldRandom(random), champion.getBbWidth(), champion.getBbHeight());
            if (fx != null) renderer.render(champion, fx);
        }
    }
}
