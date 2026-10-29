package thaumcraft.common.testing;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.ThaumcraftSoundIds;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.api.entities.IThaumcraftBoss;
import thaumcraft.api.entities.ThaumcraftEntities;
import thaumcraft.api.potions.PotionFluxTaint;
import thaumcraft.api.potions.PotionVisExhaust;
import thaumcraft.api.potions.WarpEffects;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EntityTravelingTrunk;
import thaumcraft.common.entities.monster.EntityBrainyZombie;
import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.entities.monster.EntityWisp;
import thaumcraft.common.entities.monster.*;
import thaumcraft.common.entities.projectile.EntityFrostShard;

/**
 * The stable keys of the add-on API against the live registries of a running world.
 *
 * <p>The JUnit test {@code StableKeysApiTest} checks that every key is well formed and names an id the mod's sources
 * register; only a running game has the registries filled, so this check proves each key resolves to the registered object
 * of the right kind: the effect classes and their categories, the entity classes (and the API families they belong to), the
 * armour materials with the numbers of the original, and the sound events.</p>
 */
public final class StableKeysRuntimeChecks {
    private StableKeysRuntimeChecks() {}

    public static void stableKeysResolveToTheRegisteredObjects(GameTestHelper test) {
        warpEffects(test);
        entities(test);
        armourMaterials(test);
        sounds(test);
        test.succeed();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<ResourceKey<T>> keysOf(Class<?> holder) {
        List<ResourceKey<T>> keys = new ArrayList<>();
        try {
            for (Field field : holder.getDeclaredFields()) {
                if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && field.getType() == ResourceKey.class)
                    keys.add((ResourceKey<T>) field.get(null));
            }
        } catch (IllegalAccessException impossible) {
            throw new AssertionError(impossible);
        }
        return keys;
    }

    private static void warpEffects(GameTestHelper test) {
        record Expected(ResourceKey<MobEffect> key, Class<? extends MobEffect> type, MobEffectCategory category, MobEffect internal) {}
        List<Expected> expected = List.of(
                new Expected(WarpEffects.WARP_WARD, thaumcraft.common.lib.potions.PotionWarpWard.class, MobEffectCategory.BENEFICIAL, ThaumcraftWarp.WARP_WARD.get()),
                new Expected(WarpEffects.SUN_SCORNED, thaumcraft.common.lib.potions.PotionSunScorned.class, MobEffectCategory.HARMFUL, ThaumcraftWarp.SUN_SCORNED.get()),
                new Expected(WarpEffects.DEATH_GAZE, thaumcraft.common.lib.potions.PotionDeathGaze.class, MobEffectCategory.HARMFUL, ThaumcraftWarp.DEATH_GAZE.get()),
                new Expected(WarpEffects.BLURRED_VISION, thaumcraft.common.lib.potions.PotionBlurredVision.class, MobEffectCategory.HARMFUL, ThaumcraftWarp.BLURRED_VISION.get()),
                new Expected(WarpEffects.UNNATURAL_HUNGER, thaumcraft.common.lib.potions.PotionUnnaturalHunger.class, MobEffectCategory.HARMFUL, ThaumcraftWarp.UNNATURAL_HUNGER.get()),
                new Expected(WarpEffects.THAUMARHIA, thaumcraft.common.lib.potions.PotionThaumarhia.class, MobEffectCategory.HARMFUL, ThaumcraftWarp.THAUMARHIA.get()));
        Set<ResourceKey<MobEffect>> covered = new HashSet<>();
        LivingEntity pig = (LivingEntity) test.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1);
        for (Expected e : expected) {
            covered.add(e.key());
            Holder<MobEffect> holder = WarpEffects.holder(e.key());
            test.assertTrue(holder.is(e.key()), e.key() + ": the holder carries its own key");
            test.assertTrue(BuiltInRegistries.MOB_EFFECT.get(e.key()) == holder.value(), e.key() + ": the registry answers the same object");
            test.assertTrue(WarpEffects.instance(e.key()) == holder.value(), e.key() + ": instance() is the holder's value");
            test.assertTrue(holder.value() == e.internal(), e.key() + ": the key is the effect the mod's own code applies");
            test.assertTrue(e.type().isInstance(holder.value()), e.key() + ": the registered class is " + e.type().getSimpleName());
            test.assertTrue(holder.value().getCategory() == e.category(), e.key() + ": the category of the original is " + e.category());
            // The handle works in the effect calls an add-on makes.
            pig.addEffect(new MobEffectInstance(holder, 200, 0));
            test.assertTrue(pig.hasEffect(holder), e.key() + ": the effect applies through the holder");
            pig.removeEffect(holder);
            test.assertTrue(!pig.hasEffect(holder), e.key() + ": the effect is removed through the holder");
        }
        // The seventh effect, the infectious vis exhaust, is registered next to the flux effects.
        Holder<MobEffect> infectious = WarpEffects.holder(WarpEffects.INFECTIOUS_VIS_EXHAUST);
        covered.add(WarpEffects.INFECTIOUS_VIS_EXHAUST);
        test.assertTrue(infectious.value() instanceof thaumcraft.common.lib.potions.PotionInfectiousVisExhaust, "infectious vis exhaust: its class");
        test.assertTrue(infectious.value().getCategory() == MobEffectCategory.HARMFUL, "infectious vis exhaust: harmful");
        pig.addEffect(new MobEffectInstance(infectious, 200, 0));
        test.assertTrue(pig.hasEffect(infectious), "infectious vis exhaust applies through the holder");
        pig.removeEffect(infectious);
        test.assertTrue(covered.equals(new HashSet<>(WarpEffects.all())) && covered.equals(new HashSet<>(StableKeysRuntimeChecks.<MobEffect>keysOf(WarpEffects.class))),
                "every key of WarpEffects was checked, and all() lists exactly the constants");
        // The two flux effects keep the handles they always had.
        test.assertTrue(PotionFluxTaint.holder().value() instanceof thaumcraft.common.lib.potions.PotionFluxTaint, "flux taint: its class");
        test.assertTrue(PotionVisExhaust.holder().value() instanceof thaumcraft.common.lib.potions.PotionVisExhaust, "vis exhaust: its class");
    }

    private static void entities(GameTestHelper test) {
        var level = test.getLevel();
        List<ResourceKey<EntityType<?>>> keys = keysOf(ThaumcraftEntities.class);
        test.assertTrue(keys.size() >= 30, "the entity keys are all there: " + keys.size());
        Set<ResourceKey<EntityType<?>>> distinct = new HashSet<>(keys);
        test.assertTrue(distinct.size() == keys.size(), "no entity key repeats");
        List<Entity> made = new ArrayList<>();
        for (ResourceKey<EntityType<?>> key : keys) {
            test.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(key), key + ": registered");
            Holder<EntityType<?>> holder = ThaumcraftEntities.holder(key);
            test.assertTrue(holder.is(key), key + ": the holder carries its own key");
            test.assertTrue(ThaumcraftEntities.type(key) == holder.value(), key + ": type() is the holder's value");
            test.assertTrue(BuiltInRegistries.ENTITY_TYPE.getKey(holder.value()).equals(key.location()), key + ": the type knows its id");
            Entity entity = holder.value().create(level);
            test.assertTrue(entity != null, key + ": the type creates an entity");
            test.assertTrue(entity.getType() == holder.value(), key + ": the entity is of that type");
            test.assertTrue(ThaumcraftEntities.is(entity, key), key + ": is() recognises its own entity");
            made.add(entity);
        }
        // is() answers for exactly one key per entity.
        for (int i = 0; i < keys.size(); i++) {
            for (int j = 0; j < keys.size(); j++) {
                test.assertTrue(ThaumcraftEntities.is(made.get(i), keys.get(j)) == (i == j),
                        "is(" + keys.get(i).location() + ", " + keys.get(j).location() + ") is true only for its own key");
            }
        }
        test.assertTrue(!ThaumcraftEntities.is(null, ThaumcraftEntities.WISP) && !ThaumcraftEntities.is(made.get(0), null), "null on either side is no match");
        test.assertTrue(!ThaumcraftEntities.is(test.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1), ThaumcraftEntities.WISP), "a vanilla pig is no wisp");
        // Every key creates exactly the class its Javadoc names: a key whose path points at another existing entity (two paths
        // swapped) passes everything above, since each entity is still recognised by exactly one key.
        Map<ResourceKey<EntityType<?>>, Class<? extends Entity>> classes = Map.ofEntries(
                Map.entry(ThaumcraftEntities.WISP, EntityWisp.class), Map.entry(ThaumcraftEntities.FIREBAT, EntityFireBat.class),
                Map.entry(ThaumcraftEntities.BRAINY_ZOMBIE, EntityBrainyZombie.class), Map.entry(ThaumcraftEntities.GIANT_BRAINY_ZOMBIE, EntityGiantBrainyZombie.class),
                Map.entry(ThaumcraftEntities.PECH, EntityPech.class), Map.entry(ThaumcraftEntities.MIND_SPIDER, EntityMindSpider.class),
                Map.entry(ThaumcraftEntities.THAUMIC_SLIME, EntityThaumicSlime.class),
                Map.entry(ThaumcraftEntities.CULTIST_CLERIC, EntityCultistCleric.class), Map.entry(ThaumcraftEntities.CULTIST_KNIGHT, EntityCultistKnight.class),
                Map.entry(ThaumcraftEntities.CULTIST_LEADER, EntityCultistLeader.class), Map.entry(ThaumcraftEntities.CULTIST_PORTAL, EntityCultistPortal.class),
                Map.entry(ThaumcraftEntities.CULTIST_PORTAL_GREATER, EntityCultistPortalGreater.class),
                Map.entry(ThaumcraftEntities.ELDRITCH_GUARDIAN, EntityEldritchGuardian.class), Map.entry(ThaumcraftEntities.ELDRITCH_WARDEN, EntityEldritchWarden.class),
                Map.entry(ThaumcraftEntities.ELDRITCH_GOLEM, EntityEldritchGolem.class), Map.entry(ThaumcraftEntities.ELDRITCH_CRAB, EntityEldritchCrab.class),
                Map.entry(ThaumcraftEntities.WATCHER, EntityWatcher.class), Map.entry(ThaumcraftEntities.INHABITED_ZOMBIE, EntityInhabitedZombie.class),
                Map.entry(ThaumcraftEntities.TAINT_CREEPER, EntityTaintCreeper.class), Map.entry(ThaumcraftEntities.TAINT_SPIDER, EntityTaintSpider.class),
                Map.entry(ThaumcraftEntities.TAINT_SWARM, EntityTaintSwarm.class), Map.entry(ThaumcraftEntities.TAINT_SWARMER, EntityTaintSporeSwarmer.class),
                Map.entry(ThaumcraftEntities.TAINT_SPORE, EntityTaintSpore.class), Map.entry(ThaumcraftEntities.TAINT_CRAWLER, EntityTaintCrawler.class),
                Map.entry(ThaumcraftEntities.TAINT_SEED, EntityTaintSeed.class), Map.entry(ThaumcraftEntities.TAINT_SEED_PRIME, EntityTaintSeedPrime.class),
                Map.entry(ThaumcraftEntities.TAINTACLE, EntityTaintacle.class), Map.entry(ThaumcraftEntities.TAINTACLE_SMALL, EntityTaintacleSmall.class),
                Map.entry(ThaumcraftEntities.TAINTACLE_GIANT, EntityTaintacleGiant.class),
                Map.entry(ThaumcraftEntities.TAINTED_CHICKEN, EntityTaintChicken.class), Map.entry(ThaumcraftEntities.TAINTED_COW, EntityTaintCow.class),
                Map.entry(ThaumcraftEntities.TAINTED_PIG, EntityTaintPig.class), Map.entry(ThaumcraftEntities.TAINTED_SHEEP, EntityTaintSheep.class),
                Map.entry(ThaumcraftEntities.TAINTED_VILLAGER, EntityTaintVillager.class),
                Map.entry(ThaumcraftEntities.GOLEM, EntityGolemBase.class), Map.entry(ThaumcraftEntities.TRAVELING_TRUNK, EntityTravelingTrunk.class),
                Map.entry(ThaumcraftEntities.FROST_SHARD, EntityFrostShard.class));
        test.assertTrue(classes.keySet().equals(distinct), "the class table covers exactly the keys of ThaumcraftEntities: " + classes.size() + " of " + keys.size());
        for (int i = 0; i < keys.size(); i++)
            test.assertTrue(made.get(i).getClass() == classes.get(keys.get(i)),
                    keys.get(i).location() + " creates " + made.get(i).getClass().getSimpleName() + ", expected " + classes.get(keys.get(i)).getSimpleName());
        // The right class behind the names an add-on is most likely to ask for.
        test.assertTrue(ThaumcraftEntities.type(ThaumcraftEntities.WISP).create(level) instanceof EntityWisp, "wisp: EntityWisp");
        test.assertTrue(ThaumcraftEntities.type(ThaumcraftEntities.FIREBAT).create(level) instanceof EntityFireBat, "firebat: EntityFireBat");
        test.assertTrue(ThaumcraftEntities.type(ThaumcraftEntities.BRAINY_ZOMBIE).create(level) instanceof EntityBrainyZombie, "brainy zombie: EntityBrainyZombie");
        test.assertTrue(ThaumcraftEntities.type(ThaumcraftEntities.GOLEM).create(level) instanceof EntityGolemBase, "golem: EntityGolemBase");
        test.assertTrue(ThaumcraftEntities.type(ThaumcraftEntities.FROST_SHARD).create(level) instanceof EntityFrostShard, "frost shard: EntityFrostShard");
        test.assertTrue(ThaumcraftEntities.WISP.location().equals(ThaumcraftMonsters.WISP.getId()), "wisp: the id the mod registers");
        test.assertTrue(ThaumcraftEntities.GOLEM.location().equals(ThaumcraftGolems.GOLEM.getId()), "golem: the id the mod registers");
        // The families of the API agree with the keys.
        test.assertTrue(ThaumcraftEntities.type(ThaumcraftEntities.ELDRITCH_GUARDIAN).create(level) instanceof IEldritchMob, "eldritch guardian is an IEldritchMob");
        test.assertTrue(ThaumcraftEntities.type(ThaumcraftEntities.TAINT_CREEPER).create(level) instanceof ITaintedMob, "taint creeper is an ITaintedMob");
        test.assertTrue(ThaumcraftEntities.type(ThaumcraftEntities.ELDRITCH_WARDEN).create(level) instanceof IThaumcraftBoss, "eldritch warden is an IThaumcraftBoss");
        test.assertTrue(!(ThaumcraftEntities.type(ThaumcraftEntities.WISP).create(level) instanceof IEldritchMob), "a wisp is no eldritch mob");
    }

    private static void armourMaterials(GameTestHelper test) {
        record Expected(ResourceKey<ArmorMaterial> key, Holder<ArmorMaterial> holder, ArmorMaterial object, ArmorItem own,
                        int helmet, int chest, int legs, int boots, int enchantability) {}
        List<Expected> expected = List.of(
                new Expected(ThaumcraftMaterials.ARMORMAT_THAUMIUM_KEY, ThaumcraftMaterials.armorMatThaumiumHolder(), ThaumcraftApi.armorMatThaumium(),
                        ThaumcraftEquipment.THAUMIUM_HELMET.get(), 2, 6, 5, 2, 25),
                new Expected(ThaumcraftMaterials.ARMORMAT_THAUMIUM_FORTRESS_KEY, ThaumcraftMaterials.armorMatThaumiumFortressHolder(), ThaumcraftApi.armorMatThaumiumFortress(),
                        ThaumcraftEquipment.FORTRESS_HELMET.get(), 3, 7, 6, 3, 25),
                new Expected(ThaumcraftMaterials.ARMORMAT_VOID_KEY, ThaumcraftMaterials.armorMatVoidHolder(), ThaumcraftApi.armorMatVoid(),
                        ThaumcraftEquipment.VOID_HELMET.get(), 3, 7, 6, 3, 10),
                new Expected(ThaumcraftMaterials.ARMORMAT_VOID_ROBE_KEY, ThaumcraftMaterials.armorMatVoidRobeHolder(), ThaumcraftMaterials.armorMatVoidRobe(),
                        ThaumcraftEquipment.VOID_ROBE_HELMET.get(), 3, 7, 6, 3, 10));
        for (Expected e : expected) {
            test.assertTrue(BuiltInRegistries.ARMOR_MATERIAL.containsKey(e.key()), e.key() + ": registered");
            test.assertTrue(e.holder().is(e.key()), e.key() + ": the holder is the registry reference of its key");
            test.assertTrue(e.holder().value() == e.object(), e.key() + ": the holder's value is the object the 1.7.10 accessor answers");
            test.assertTrue(BuiltInRegistries.ARMOR_MATERIAL.get(e.key()) == e.object(), e.key() + ": the registry answers the same object");
            test.assertTrue(e.object().enchantmentValue() == e.enchantability(), e.key() + ": enchantability " + e.enchantability());
            // The numbers of the original, slot by slot.
            var defence = e.object().defense();
            test.assertTrue(defence.get(ArmorItem.Type.HELMET) == e.helmet() && defence.get(ArmorItem.Type.CHESTPLATE) == e.chest()
                    && defence.get(ArmorItem.Type.LEGGINGS) == e.legs() && defence.get(ArmorItem.Type.BOOTS) == e.boots(),
                    e.key() + ": defence of the four slots is " + e.helmet() + "/" + e.chest() + "/" + e.legs() + "/" + e.boots() + " but " + defence);
            // The holder is the one the mod's own armour of that family holds, so an add-on that passes it to an ArmorItem
            // gets the same material (an Item can only be constructed while the registry is open, so this reads the registered one).
            test.assertTrue(e.own().getMaterial().is(e.key()) && e.own().getMaterial().value() == e.holder().value(),
                    e.key() + ": the armour Thaumcraft registers holds the same material");
        }
        // The same holders through ThaumcraftApi, the class the 1.7.10 field lived in.
        test.assertTrue(ThaumcraftApi.armorMatThaumiumHolder().value() == ThaumcraftEquipment.THAUMIUM_ARMOR_MATERIAL.get(), "ThaumcraftApi.armorMatThaumiumHolder");
        test.assertTrue(ThaumcraftApi.armorMatThaumiumFortressHolder().value() == ThaumcraftEquipment.FORTRESS_ARMOR_MATERIAL.get(), "ThaumcraftApi.armorMatThaumiumFortressHolder");
        test.assertTrue(ThaumcraftApi.armorMatVoidHolder().value() == ThaumcraftEquipment.VOID_ARMOR_MATERIAL.get(), "ThaumcraftApi.armorMatVoidHolder");
        // The two originals that were never registered under their own names stay plain objects with the original numbers.
        test.assertTrue(BuiltInRegistries.ARMOR_MATERIAL.getKey(ThaumcraftMaterials.ARMORMAT_SPECIAL) == null, "armorMatSpecial is a plain object");
        test.assertTrue(BuiltInRegistries.ARMOR_MATERIAL.getKey(ThaumcraftMaterials.ARMORMAT_VOID_FORTRESS) == null, "armorMatVoidFortress is a plain object");
        test.assertTrue(ThaumcraftApi.armorMatSpecial() == ThaumcraftMaterials.ARMORMAT_SPECIAL && ThaumcraftApi.armorMatVoidFortress() == ThaumcraftMaterials.ARMORMAT_VOID_FORTRESS,
                "the 1.7.10 accessors answer the plain objects");
        // Their numbers: the defences and the enchantability of the originals (the durability factor is on the item, not here).
        test.assertTrue(ThaumcraftMaterials.ARMORMAT_SPECIAL.enchantmentValue() == 25, "armorMatSpecial: enchantability 25");
        test.assertTrue(ThaumcraftMaterials.ARMORMAT_VOID_FORTRESS.enchantmentValue() == 10, "armorMatVoidFortress: enchantability 10");
        var special = ThaumcraftMaterials.ARMORMAT_SPECIAL.defense();
        test.assertTrue(special.get(ArmorItem.Type.HELMET) == 1 && special.get(ArmorItem.Type.CHESTPLATE) == 3
                && special.get(ArmorItem.Type.LEGGINGS) == 2 && special.get(ArmorItem.Type.BOOTS) == 1, "armorMatSpecial: defence 1/3/2/1 but " + special);
        var fortress = ThaumcraftMaterials.ARMORMAT_VOID_FORTRESS.defense();
        test.assertTrue(fortress.get(ArmorItem.Type.HELMET) == 4 && fortress.get(ArmorItem.Type.CHESTPLATE) == 8
                && fortress.get(ArmorItem.Type.LEGGINGS) == 7 && fortress.get(ArmorItem.Type.BOOTS) == 4, "armorMatVoidFortress: defence 4/8/7/4 but " + fortress);
    }

    private static void sounds(GameTestHelper test) {
        List<ResourceLocation> constants = new ArrayList<>();
        try {
            for (Field field : ThaumcraftSoundIds.class.getDeclaredFields()) {
                if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && field.getType() == ResourceLocation.class)
                    constants.add((ResourceLocation) field.get(null));
            }
        } catch (IllegalAccessException impossible) {
            throw new AssertionError(impossible);
        }
        test.assertTrue(constants.size() >= 60, "the sound constants are all there: " + constants.size());
        Set<ResourceLocation> registered = new HashSet<>();
        for (ResourceLocation id : BuiltInRegistries.SOUND_EVENT.keySet()) if (id.getNamespace().equals("thaumcraft")) registered.add(id);
        test.assertTrue(registered.equals(new HashSet<>(constants)),
                "one constant for every sound event Thaumcraft registers, and none for another: missing " + diff(registered, constants) + ", extra " + diff(new HashSet<>(constants), registered));
        var pos = test.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        for (ResourceLocation id : constants) {
            SoundEvent event = ThaumcraftSoundIds.event(id);
            test.assertTrue(event.getLocation().equals(id), id + ": the event carries its id");
            test.assertTrue(BuiltInRegistries.SOUND_EVENT.get(id) == event, id + ": event() answers the registered object");
            // Plays on the server without error; no player is in range.
            test.getLevel().playSound(null, pos, event, SoundSource.PLAYERS, 0.0F, 1.0F);
        }
        for (String name : List.of("wand", "tool", "zap", "wind", "brain", "egidle", "key")) {
            test.assertTrue(constants.contains(ResourceLocation.fromNamespaceAndPath("thaumcraft", name)), "the sound the originals used: " + name);
        }
        boolean refused = false;
        try {
            ThaumcraftSoundIds.event(ResourceLocation.fromNamespaceAndPath("thaumcraft", "no_such_sound"));
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        test.assertTrue(refused, "an id that is no sound event is refused");
    }

    private static <T> Set<T> diff(Set<T> a, java.util.Collection<T> b) {
        Set<T> rest = new HashSet<>(a);
        rest.removeAll(b);
        return rest;
    }
}
