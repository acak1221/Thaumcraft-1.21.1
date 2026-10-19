# Zombie brain — food and real-drop prerequisite, 2026-10-19

Authored only new `common/src/main/java/thaumcraft/common/items/ItemZombieBrain.java`, new `common/src/main/java/thaumcraft/common/lib/loot/ZombieBrainDrops.java` and this note. **Root integration / actual Fabric compilation pending.** No Java/Gradle/game/tests/Python/world/export work ran; platform API investigation read cached source/class metadata only. Existing non-WIP knowledge/infusion paths were read as agreed; golem/runtime WIP was not read and existing production/resource files were not changed.

## Food API and original behavior

- Root's existing MATERIALS `zombie_brain` branch must construct **`new ItemZombieBrain(properties)`**, retaining native ID/model/brain texture/recipe aliases. The constructor supplies native FoodProperties: **nutrition4, saturation modifier0.2** (computed saturation1.6), ordinary eating duration, **Hunger600 ticks / amplifier0 / probability0.8**. It is not always-edible or fast food; no nausea or additional potion is added.
- Authority: private oracle `../work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/items/ItemZombieBrain.java:35–37,51–58`. `Potion.field_76438_s` is **hunger**, confirmed by identifier mapping `../work/references/OpenTC4/src/main/fields.csv:1669`.
- `finishUsingItem(ItemStack,Level,LivingEntity)` charges only a same-level/server-thread **ServerPlayer** completing the actual held/use stack at native remaining-time0. It rolls **world.nextFloat()<0.1 → sticky+1**, otherwise **temporary+(1+world.nextInt(3))**, **before super**. Native super retains food payment, creative handling, saturation, stats/advancements and the Hunger roll; there is no manual shrink, item NBT or new packet.
- Charges use actual `KnowledgeAccess.get`, PlayerKnowledge sticky/temp fields and **counter=total warp**, followed by existing `KnowledgeEvents.sync`, the sticky/temp action-bar message and sticky-only whispers. These mirror `InfusionInstability.java:82–99`, `PlayerKnowledge.java:103–114,150–157`, `KnowledgeEvents.java:51–57`; original charge/message anchors are `Thaumcraft.java:302–346`, `PacketWarpMessage.java:63–76`. This persists knowledge charges, **not the unfinished Warp-effect engine**.
- Warp draw order remains before native eating. Modern food effects use the **consumer's RNG**, and modern eating sounds/notifications have native RNG draws: this is not bit-for-bit TC4 world-RNG replay. The 80% Hunger probability stays in the native food component, not a second manual roll.
- Original wolf-food=true needs root to append `thaumcraft:zombie_brain` to **`data/minecraft/tags/item/wolf_food.json`** (`replace:false`). Cached1.21.1 Wolf uses WOLF_FOOD and food nutrition; no Java builder wolf flag exists. Add to wolf_food only, not the broad meat tag. Until this root tag is added, wolf feeding is a concrete remaining gap.

## Exact ordinary drop API

```java
boolean ZombieBrainDrops.isBrainDrop(LivingEntity victim, boolean recentlyHit, int lootingLevel)
void ZombieBrainDrops.onNormalDeathLoot(LivingEntity victim, ServerLevel level,
        DamageSource source, boolean recentlyHit, ItemLike registeredBrain)
```

- Original `common/lib/events/EventHandlerEntity.java:462–463`: non-Brainy EntityZombie, **recentlyHit**, **world.random.nextInt(10)−lootingLevel<1**, then **one brain** at victim X/Z and Y+eye height. No Looting count multiplier or champion/villager-coin behavior belongs here. The predicate preserves that integer roll and consumes no RNG for ineligible calls; Looting0/1/2/3 gives10/20/30/40%, ≥9 guarantees the roll succeeds.
- Finite native types: **Zombie, Husk, Drowned, ZombieVillager, ZombifiedPiglin**; require both Zombie inheritance and one of these EntityTypes. Original PigZombie **is included**: cached1.7.10 `yh extends yq`, mapped by `vanilla-1.7.10-reference/joined.srg:1799,1808`. Cached1.21.1 headers likewise confirm all four modern subclasses extend Zombie. This does not opt every modded zombie into drops. A future Brainy Zombie's separate registered type is excluded; preserve its separate custom rule when ported (`EntityBrainyZombie.java:30–31,60–67`), not an extra ordinary roll.
- `onNormalDeathLoot` requires the same server level/thread, dead-or-dying victim, doMobLoot, eligible type and actual recent-hit boolean. It resolves **native Looting on the DamageSource's living attacker**, otherwise0, using the current registry's Enchantments.LOOTING / EnchantmentHelper.getEnchantmentLevel. A successful roll calls ordinary **`victim.spawnAtLocation(oneBrain, victim.getEyeHeight())`**; vanilla emission/admission handles the item, not a player grant, deferred queue or retry subsystem.

## Necessary root hook — narrow native death-loot point

Architectury **13.0.11 has no living-drops event** with recent-hit/Looting/drop collection: EntityEvent only offers cancellable LIVING_DEATH/HURT etc.; LootEvent modifies tables. Fabric loot-v3 MODIFY exposes registries, but native `killed_by_player` is **not exactly recentlyHit**: tamed-wolf damage can leave recent-hit time100 with a null player owner. Replacing that boolean with player-credit would silently miss legitimate original drops.

Root therefore adds one tiny common LivingEntity mixin, registered in its existing mixin configuration. Use **`@ModifyArgs`** on method `dropAllDeathLoot` at this exact invocation; pass its three arguments unmodified to `onNormalDeathLoot`, together with `(LivingEntity)(Object)this` and the existing registered zombie_brain item:

```text
Lnet/minecraft/world/entity/LivingEntity;dropCustomDeathLoot(
  Lnet/minecraft/server/level/ServerLevel;
  Lnet/minecraft/world/damagesource/DamageSource;Z)V
```

The target descriptor is contiguous in Java; arguments0/1/2 are **ServerLevel / DamageSource / native recentlyHit**. No accessor is needed. Cached mapped LivingEntity `dropAllDeathLoot` shows this invocation **inside both shouldDropLoot and doMobLoot guards**, after native loot-table generation (`.tmp/bore-native19-touch-diagnosis/net_minecraft_world_entity_LivingEntity.javap.txt:3666–3692`). Invoke **once**, not also from LIVING_DEATH, another loot pool or a tick callback. The helper is a hook implementation, not self-registered; **without this root hook real acquisition is not yet wired**.

This narrow pre-custom-loot hook changes the RNG stream position relative to TC4's later LivingDropsEvent; the world integer chance and native Looting semantics are retained. Native spawnAtLocation supplies ordinary modern drop defaults (including default pickup delay), rather than recreating TC4's raw event EntityItem. No unsafe “spawn whenever death event fires” fallback is provided.

## Remaining boundaries

- Root deltas: **MATERIALS constructor case + wolf_food tag + one normal-loot mixin hook**. No content bootstrap no-op/register stub or new loot serializer is necessary. Root compiles after integrating these sources; gameplay/visuals and full Warp effects remain unverified/deferred.
- Existing correct brain object aspects remain untouched: corpus2/cognitio4/exanimis2. In this oracle their direct source anchor is **ConfigAspects.java:401** (402 is amber); the existing `early_object_aspects.json` brain row already agrees. No scan/aspect/recipe ID replacement was made.
- Sorting still requires its separately owned infusion/worker/UI integration. This prerequisite supplies the actual brain food/drop behavior, not an all-zombies/full-Warp/Sorting acceptance claim.
