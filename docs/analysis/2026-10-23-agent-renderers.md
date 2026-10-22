# Agent `renderers` — the entity renderer coverage of the 1.21.1 port (2026-10-23)

Wave 2, crash class **"an entity type of the port that has no client renderer registered"**.
In 1.21.1 the client looks the renderer of an entity up **by entity type** and there is no
fallback, so every entity type of the port needs a registered renderer.

## 1. The defect

`EntityRenderDispatcher.getRenderer` (1.21.1, decompiled from the Loom cache jar
`minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2.jar`):

```java
public <T extends Entity> EntityRenderer<? super T> getRenderer(T entity) {
    if (entity instanceof AbstractClientPlayer) { ... }
    return this.renderers.get(entity.getType());   // line 175 -> null for an unknown type
}
```

`EntityRenderDispatcher.render` (line 206-208) calls it and then dereferences the result,
so an entity type without a registered renderer is a `NullPointerException` **the moment the
entity is drawn**.

The 1.7.10 original had no such lookup: `RenderingRegistry.registerEntityRenderingHandler`
was keyed by entity **class** and the original registered a renderer for **every** entity the
mod added.

## 2. The specification: the original registration list

Oracle `thaumcraft/client/ClientProxy.java:464-517` (`setupEntityRenderers`) registers 50
renderers for the 50 entity types of the oracle
(`thaumcraft/common/config/ConfigEntities.java:88-166`). The list, with the port's choice:

| oracle line | original renderer | original entity | port renderer |
|---|---|---|---|
| :465 | `RenderItem` | `EntityItemGrate` | vanilla `ItemEntityRenderer` |
| :466 | `RenderSpecialItem` | `EntitySpecialItem` | `RenderSpecialItem` |
| :467 | `RenderFollowingItem` | `EntityFollowingItem` | `RenderFollowingItem` |
| :468 | `RenderSpecialItem` | `EntityPermanentItem` | `RenderSpecialItem` |
| :469 | `RenderAspectOrb` | `EntityAspectOrb` | `RenderAspectOrb` |
| :470 | `RenderGolemBobber` | `EntityGolemBobber` | `RenderGolemBobber` |
| :471 | `RenderGolemBase` | `EntityGolemBase` | `RenderGolemBase` |
| :472 | `RenderWisp` | `EntityWisp` | `RenderWisp` |
| :473 | `RenderAlumentum` | `EntityAlumentum` | `RenderAlumentum` |
| :474 | `RenderPrimalOrb` | `EntityPrimalOrb` | `RenderPrimalOrb` |
| :475 | `RenderEldritchOrb` | `EntityEldritchOrb` | `RenderEldritchOrb` |
| :476 | `RenderElectricOrb` | `EntityGolemOrb` | **`RenderGolemOrb` (new)** |
| :477 | `RenderEmber` | `EntityEmber` | `RenderEmber` |
| :478 | `RenderElectricOrb` | `EntityShockOrb` | `RenderFocusOrb` (electric billboard) |
| :479 | `RenderExplosiveOrb` | `EntityExplosiveOrb` | `RenderFocusOrb` (explosive billboard) |
| :480 | `RenderPechBlast` | `EntityPechBlast` | `RenderPechBlast` |
| :481 | `RenderBrainyZombie` | `EntityBrainyZombie` | `RenderBrainyZombie` |
| :482 | `RenderInhabitedZombie` | `EntityInhabitedZombie` | `RenderInhabitedZombie` |
| :483 | `RenderBrainyZombie` | `EntityGiantBrainyZombie` | `RenderBrainyZombie` |
| :484 | `RenderPech` | `EntityPech` | `RenderPech` |
| :485 | `RenderFireBat` | `EntityFireBat` | `RenderFireBat` |
| :486 | `RenderFrostShard` | `EntityFrostShard` | `RenderFrostShard` |
| :487 | `RenderDart` | `EntityDart` | `RenderDart` |
| :488 | `RenderPrimalArrow` | `EntityPrimalArrow` | **`RenderPrimalArrowEntity` (new)** |
| :489 | `RenderFallingTaint` | `EntityFallingTaint` | `RenderFallingTaint` |
| :490 | `RenderThaumicSlime` | `EntityThaumicSlime` | `RenderThaumicSlime` |
| :491 | `RenderTaintSpider` | `EntityTaintSpider` | `RenderTaintSpider` |
| :492 | `RenderTaintacle(0.6,10)` | `EntityTaintacle` | `RenderTaintacle(.6F,10)` |
| :493 | `RenderTaintacle(0.2,6)` | `EntityTaintacleSmall` | `RenderTaintacle(.2F,6)` |
| :494 | `RenderTaintacle(1.0,14)` | `EntityTaintacleGiant` | **`RenderTaintacle(1.0F,14)` (new)** |
| :495 | `RenderTaintSpore` | `EntityTaintSpore` | `RenderTaintSpore` |
| :496 | `RenderTaintSporeSwarmer` | `EntityTaintSporeSwarmer` | `RenderTaintSporeSwarmer` |
| :497 | `RenderTaintSwarm` | `EntityTaintSwarm` | `RenderTaintSwarm` |
| :498 | `RenderTaintChicken` | `EntityTaintChicken` | `RenderTaintChicken` |
| :499 | `RenderTaintCow` | `EntityTaintCow` | `RenderTaintCow` |
| :500 | `RenderTaintCreeper` | `EntityTaintCreeper` | `RenderTaintCreeper` |
| :501 | `RenderTaintPig` | `EntityTaintPig` | `RenderTaintPig` |
| :502 | `RenderTaintSheep` | `EntityTaintSheep` | `RenderTaintSheep` |
| :503 | `RenderTaintVillager` | `EntityTaintVillager` | `RenderTaintVillager` |
| :504 | `RenderTravelingTrunk` | `EntityTravelingTrunk` | `RenderTravelingTrunk` |
| :505 | `RenderMindSpider` | `EntityMindSpider` | `RenderMindSpider` |
| :506 | `RenderEldritchGuardian` | `EntityEldritchGuardian` | `RenderEldritchGuardian` |
| :507 | `RenderEldritchGuardian` | `EntityEldritchWarden` | `RenderEldritchWarden` (port's own) |
| :508 | `RenderCultistPortal` | `EntityCultistPortal` | `RenderCultistPortal` |
| :509 | `RenderCultist` | `EntityCultistKnight` | `RenderCultist` |
| :510 | `RenderCultist` | `EntityCultistLeader` | `RenderCultist` |
| :511 | `RenderCultist` | `EntityCultistCleric` | `RenderCultist` |
| :512 | `RenderEldritchGolem` | `EntityEldritchGolem` | `RenderEldritchGolem` |
| :513 | `RenderSnowball(itemBottleTaint,0)` | `EntityBottleTaint` | **vanilla `ThrownItemRenderer` (new)** |
| :514 | `RenderEldritchCrab` | `EntityEldritchCrab` | `RenderEldritchCrab` |

The oracle has **no** tainted crawler, tainted seed or prime tainted seed: those three are
1.12.2 backport entities of the port (the oracle has no `EntityTaintCrawler`, no
`EntityTaintSeed`; `grep -rn "TaintCrawler\|TaintSeed" oracle/` returns nothing). Their
reference is the 1.12.2 donor jar in the repository root, `Thaumcraft-1.12.2-6.1.BETA26.jar`:
`thaumcraft/proxies/ProxyEntities.java:199-201` registers `RenderTaintCrawler`,
`RenderTaintSeed` and `RenderTaintSeed` with the shadow 0.6.

## 3. The complete table of the port's entity types

**56** entity types, all in `thaumcraft.common.config`:

| # | id | registration class | renderer (before) | renderer (after) |
|---|---|---|---|---|
| 1 | `special_item` | `ThaumcraftContent` | `RenderSpecialItem` | `RenderSpecialItem` |
| 2 | `inventory_item` | `ThaumcraftContent` | `ItemEntityRenderer` | `ItemEntityRenderer` |
| 3 | `bottle_taint` | `ThaumcraftProjectiles` | **none** | `ThrownItemRenderer` **(new)** |
| 4 | `golem_orb` | `ThaumcraftProjectiles` | **none** | `RenderGolemOrb` **(new)** |
| 5 | `primal_arrow` | `ThaumcraftProjectiles` | **none** | `RenderPrimalArrowEntity` **(new)** |
| 6 | `follow_item` | `ThaumcraftProjectiles` | `RenderFollowingItem` | `RenderFollowingItem` |
| 7 | `permanent_item` | `ThaumcraftProjectiles` | `RenderSpecialItem` | `RenderSpecialItem` |
| 8 | `item_grate` | `ThaumcraftProjectiles` | `ItemEntityRenderer` | `ItemEntityRenderer` |
| 9 | `falling_taint` | `ThaumcraftTaint` | `RenderFallingTaint` | `RenderFallingTaint` |
| 10 | `taint_spider` | `ThaumcraftTaint` | `RenderTaintSpider` | `RenderTaintSpider` |
| 11 | `taint_spore` | `ThaumcraftTaint` | `RenderTaintSpore` | `RenderTaintSpore` |
| 12 | `taint_swarmer` | `ThaumcraftTaint` | `RenderTaintSporeSwarmer` | `RenderTaintSporeSwarmer` |
| 13 | `taint_swarm` | `ThaumcraftTaint` | `RenderTaintSwarm` | `RenderTaintSwarm` |
| 14 | `taintacle` | `ThaumcraftTaint` | `RenderTaintacle(.6F,10)` | `RenderTaintacle(.6F,10)` |
| 15 | `taintacle_small` | `ThaumcraftTaint` | `RenderTaintacle(.2F,6)` | `RenderTaintacle(.2F,6)` |
| 16 | `tainted_chicken` | `ThaumcraftTaintAnimals` | `RenderTaintChicken` | `RenderTaintChicken` |
| 17 | `tainted_cow` | `ThaumcraftTaintAnimals` | `RenderTaintCow` | `RenderTaintCow` |
| 18 | `tainted_pig` | `ThaumcraftTaintAnimals` | `RenderTaintPig` | `RenderTaintPig` |
| 19 | `tainted_sheep` | `ThaumcraftTaintAnimals` | `RenderTaintSheep` | `RenderTaintSheep` |
| 20 | `tainted_villager` | `ThaumcraftTaintAnimals` | `RenderTaintVillager` | `RenderTaintVillager` |
| 21 | `taint_crawler` | `ThaumcraftTaintAnimals` | **none** | `RenderTaintCrawler` **(new)** |
| 22 | `taint_seed` | `ThaumcraftTaintAnimals` | **none** | `RenderTaintSeed` **(new)** |
| 23 | `taint_seed_prime` | `ThaumcraftTaintAnimals` | **none** | `RenderTaintSeed` **(new)** |
| 24 | `taint_creeper` | `ThaumcraftTaintCreeper` | `RenderTaintCreeper` | `RenderTaintCreeper` |
| 25 | `cultist_knight` | `ThaumcraftCultists` | `RenderCultist` | `RenderCultist` |
| 26 | `cultist_cleric` | `ThaumcraftCultists` | `RenderCultist` | `RenderCultist` |
| 27 | `cultist_leader` | `ThaumcraftCultists` | `RenderCultist` | `RenderCultist` |
| 28 | `cultist_portal` | `ThaumcraftCultists` | `RenderCultistPortal` | `RenderCultistPortal` |
| 29 | `cultist_portal_greater` | `ThaumcraftCultists` | `RenderCultistPortal` | `RenderCultistPortal` |
| 30 | `brainy_zombie` | `ThaumcraftMonsters` | `RenderBrainyZombie` | `RenderBrainyZombie` |
| 31 | `giant_brainy_zombie` | `ThaumcraftMonsters` | `RenderBrainyZombie` | `RenderBrainyZombie` |
| 32 | `firebat` | `ThaumcraftMonsters` | `RenderFireBat` | `RenderFireBat` |
| 33 | `wisp` | `ThaumcraftMonsters` | `RenderWisp` | `RenderWisp` |
| 34 | `mind_spider` | `ThaumcraftWarp` | `RenderMindSpider` | `RenderMindSpider` |
| 35 | `eldritch_guardian` | `ThaumcraftWarp` | `RenderEldritchGuardian` | `RenderEldritchGuardian` |
| 36 | `eldritch_orb` | `ThaumcraftWarp` | `RenderEldritchOrb` | `RenderEldritchOrb` |
| 37 | `golem` | `ThaumcraftGolems` | `RenderGolemBase` | `RenderGolemBase` |
| 38 | `traveling_trunk` | `ThaumcraftGolems` | `RenderTravelingTrunk` | `RenderTravelingTrunk` |
| 39 | `dart` | `ThaumcraftGolems` | `RenderDart` | `RenderDart` |
| 40 | `golem_bobber` | `ThaumcraftGolems` | `RenderGolemBobber` | `RenderGolemBobber` |
| 41 | `pech` | `ThaumcraftPech` | `RenderPech` | `RenderPech` |
| 42 | `pech_blast` | `ThaumcraftPech` | `RenderPechBlast` | `RenderPechBlast` |
| 43 | `primal_orb` | `ThaumcraftPrimalFoci` | `RenderPrimalOrb` | `RenderPrimalOrb` |
| 44 | `ember` | `ThaumcraftEmbers` | `RenderEmber` | `RenderEmber` |
| 45 | `alumentum` | `ThaumcraftResources` | `RenderAlumentum` | `RenderAlumentum` |
| 46 | `shock_orb` | `ThaumcraftShockOrbs` | `RenderFocusOrb` | `RenderFocusOrb` |
| 47 | `explosive_orb` | `ThaumcraftExplosiveOrbs` | `RenderFocusOrb` | `RenderFocusOrb` |
| 48 | `aspect_orb` | `ThaumcraftAspectOrbs` | `RenderAspectOrb` | `RenderAspectOrb` |
| 49 | `eldritch_warden` | `ThaumcraftEldritchMobs` | `RenderEldritchWarden` | `RenderEldritchWarden` |
| 50 | `eldritch_golem` | `ThaumcraftEldritchMobs` | `RenderEldritchGolem` | `RenderEldritchGolem` |
| 51 | `eldritch_crab` | `ThaumcraftEldritchMobs` | `RenderEldritchCrab` | `RenderEldritchCrab` |
| 52 | `inhabited_zombie` | `ThaumcraftEldritchMobs` | `RenderInhabitedZombie` | `RenderInhabitedZombie` |
| 53 | `watcher` | `ThaumcraftEldritchMobs` | `RenderWatcher` | `RenderWatcher` |
| 54 | `thaum_slime` | `ThaumcraftEldritchMobs` | `RenderThaumicSlime` | `RenderThaumicSlime` |
| 55 | `taintacle_giant` | `ThaumcraftEldritchMobs` | **none** | `RenderTaintacle(1.0F,14)` **(new)** |
| 56 | `frost_shard` | `ThaumcraftFrost` | `RenderFrostShard` | `RenderFrostShard` |

**Seven** entity types had no renderer: `bottle_taint`, `golem_orb`, `primal_arrow`,
`taint_crawler`, `taint_seed`, `taint_seed_prime`, `taintacle_giant`.

The list was verified mechanically: a script that reads every `ENTITIES.register("id", …)` of
`thaumcraft/common/config` finds exactly 56 ids, no duplicate, and the ids are exactly the
56 of the table above and of the two lists of `ThaumcraftEntityRenderers`.

## 4. What each new renderer is, and why

### `bottle_taint` — vanilla `ThrownItemRenderer`
Oracle `:513` registers `new RenderSnowball(ConfigItems.itemBottleTaint, 0)`: the 1.7.10
`RenderSnowball` draws the 3D snowball model with the **icon of the bottle of taint item**
(`RenderSnowball.getEntityTexture` answers `item.getIconFromDamage(meta)`). The 1.21.1
counterpart of that renderer is `net.minecraft.client.renderer.entity.ThrownItemRenderer`,
the renderer vanilla uses for the snowball, the egg and the ender pearl: it draws the stack
the projectile carries. `ThrownItemRenderer.render` casts the entity to
`net.minecraft.world.entity.projectile.ItemSupplier`, so `EntityBottleTaint` now implements
that interface and answers the bottle of taint stack
(`common/src/main/java/thaumcraft/common/entities/projectile/EntityBottleTaint.java:76-83`).
Deviation: the 1.7.10 bottle is a 3D snowball mesh with the bottle icon, the 1.21.1 bottle
is the item model of the bottle of taint in the ground display context — the native way to
draw a thrown item.

### `golem_orb` — `RenderGolemOrb` (the port's own `RenderElectricOrb`)
Oracle `:476` registers `new RenderElectricOrb()` for the golem orb. The port already had
`RenderElectricOrb` (a port of the oracle's `RenderElectricOrb`), but it was **never
registered** and it does not carry the red row. The oracle's renderer reads
`entity instanceof EntityGolemOrb && ((EntityGolemOrb)entity).red` for the row
(oracle `RenderElectricOrb.java:45-47`: the row is `0.875`, the red orb uses `0.75`).
`RenderElectricOrb` therefore lost its `final` and the new
`RenderGolemOrb` overrides the `isRed` hook with `EntityGolemOrb.isRed()`
(`common/src/main/java/thaumcraft/client/renderers/entity/RenderGolemOrb.java`).

### `primal_arrow` — `RenderPrimalArrowEntity` (the port's own `RenderPrimalArrow`)
Oracle `:488` registers `new RenderPrimalArrow()`. The port already had `RenderPrimalArrow`
(a port of the oracle's `RenderPrimalArrow`), but it was **never registered**. The oracle's
renderer reads two entity values: `arrow.type` for the colour of the wisp trail
(oracle `RenderPrimalArrow.java:39`) and `arrow.field_70252_j`, the number of ticks the
arrow already spent in the ground, for the fade (`:64`; the counter is raised in
`EntityPrimalArrow.onUpdate`, oracle `EntityPrimalArrow.java:163-165`, and the arrow dies at
a hundred). The port's `RenderPrimalArrow` exposes the two values as overridable hooks, and
the new `RenderPrimalArrowEntity` answers them with `EntityPrimalArrow.getArrowType()` and
the new `EntityPrimalArrow.spentLife()` (the native `inGroundTime`, which is `protected` in
1.21.1 and therefore needs the accessor).

### `taintacle_giant` — the port's own `RenderTaintacle(1.0F, 14)`
Oracle `:494` registers `new RenderTaintacle(1.0f, 14)` for the giant taintacle, the same
renderer as the taintacle (`:492`, shadow 0.6, length 10) and the small taintacle (`:493`,
shadow 0.2, length 6). The port's `RenderTaintacle(context, shadow, length)` is exactly that
renderer, and `EntityTaintacleGiant extends EntityTaintacle`, so the giant is drawn with
the shadow of one and the length of fourteen. No new class was needed.

### `taint_crawler` — `RenderTaintCrawler` (the 1.12.2 backport's own model)
The oracle has no tainted crawler; the entity comes from
`Thaumcraft-1.12.2-6.1.BETA26.jar`, whose `thaumcraft/proxies/ProxyEntities.java:199`
registers `RenderTaintCrawler`: a `RenderLiving` over the **vanilla `ModelSilverfish`** with
the shadow 0.2, the flipped turn of 180 degrees and the pre-render scale of 0.7. The native
renderer keeps the model (`net.minecraft.client.model.SilverfishModel` with
`ModelLayers.SILVERFISH`), the shadow, the flip and the scale.
**Declared deviation:** the backport drew the silverfish mesh with its own sheet
`textures/entity/crawler.png`; that sheet is not part of this port's asset tree (the native
tree carries the retained TC4 art only, see `docs/porting/resource-manifest.json`), so the
native renderer takes the vanilla silverfish skin. The 1.21.1 counterpart of the original
model is the vanilla silverfish model, which is what the brief asks for when the original
model is a vanilla one.

### `taint_seed` and `taint_seed_prime` — `RenderTaintSeed` + `ModelTaintSeed`
The oracle has no tainted seed; the entity comes from the 1.12.2 donor jar, whose
`thaumcraft/proxies/ProxyEntities.java:200-201` registers `RenderTaintSeed` (shadow 0.4)
and `RenderTaintSeed(rm, new ModelTaintSeed(), 0.6f)` (shadow 0.6) — the same model with a
bigger shadow. The backport's `ModelTaintSeed` is a 1.7.10 `ModelBase` with one eight unit
cube and seven eight or four unit segment cubes, all on the origin, twisting around the y
axis, and its `func_78088_a` lifts the chain by 1.2 plus the growth of the seed and scales
it with half the height of the entity. The native `ModelTaintSeed`
(`common/src/main/java/thaumcraft/client/renderers/models/entities/ModelTaintSeed.java`)
reproduces the chain, the lift, the scale and the original wobble
(`ModelTaintSeed.func_78087_a`: `0.1/fi * sin(partial*0.06 - k/2)/5 + ht + attackAnim` on the
x rotation and `0.1/fi * sin(partial*0.05 - k/2)/5` on the z rotation, with
`ht = maxHealth/200` and `fi = (0.1 + 0.2) * 3`).
**Declared deviations:** (a) the backport sheet `textures/entity/taintseed.png` is not part
of this port's asset tree, so the chain is drawn with the port's own taint tentacle sheet
`textures/models/taintacle.png`; (b) the per segment pulse of the backport's
`ModelRendererTaintSeed` is not reproduced — the native chain uses the port's taint tentacle
mesh and skin, so the seed reads as a small taint tentacle. Both entities are never spawned
by the port (no spawn rule, no spawn egg; `grep -rn "TAINT_SEED\|TAINT_CRAWLER"
common/src/main/java` finds only the entity classes themselves), so the deviation is only
visible to a `/summon`.

## 5. The copy-cast audit (the second entry point of the same crash class)

1.21.1 renders the item pickup animation with a **copy** of the picked entity
(`net/minecraft/client/particle/ItemPickupParticle.java`):

```java
private Entity getSafeCopy(Entity entity) {
    if (!(entity instanceof ItemEntity)) return entity;
    return ((ItemEntity)entity).copy();
}
```

and `ItemEntity.copy()` is `new ItemEntity(this)`, whose copy constructor is
`super(itemEntity.getType(), itemEntity.level())` — the copy keeps the entity **type** but
not the entity **class**. `ItemPickupParticle.render` then calls
`entityRenderDispatcher.render(this.itemEntity, …)`, so the renderer registered for that
type is asked to draw a **plain `ItemEntity`**.

The port's `ItemEntity` subclasses and their renderers:

| entity type | class | renderer | accepts a copy? |
|---|---|---|---|
| `special_item` | `EntitySpecialItem` | `RenderSpecialItem` | yes (`EntityRenderer<ItemEntity>`) |
| `permanent_item` | `EntityPermanentItem` | `RenderSpecialItem` | yes |
| `follow_item` | `EntityFollowingItem` | `RenderFollowingItem` | yes (`EntityRenderer<ItemEntity>`) |
| `inventory_item` | `EntityInventoryItem` | vanilla `ItemEntityRenderer` | yes |
| `item_grate` | `EntityItemGrate` | vanilla `ItemEntityRenderer` | yes |

The previous run had already widened `RenderSpecialItem` and `RenderFollowingItem`; the two
vanilla renderers are typed to `ItemEntity` by definition. **No remaining copy-cast crash
exists in the port.**

The audit of the other 51 renderers found no other copy path: `Entity` and `LivingEntity` in
1.21.1 have **no** `copy()` method at all (the only `copy` in `Entity` is `copyPosition`),
so the item pickup animation is the only place where a copy of an entity is drawn. The
renderers typed to a subclass are all typed to a class that the entity type of the same
name extends, so the type of the entity and the type of the renderer agree:

* `RenderTaintacle` (`EntityTaintacle`) also draws `EntityTaintacleSmall` and
  `EntityTaintacleGiant`, which extend it;
* `RenderCultist` (`EntityCultist`) also draws the cleric and the leader, which extend it;
* `RenderCultistPortal` (`EntityCultistPortal`) also draws the greater portal, which
  extends it;
* `RenderBrainyZombie` (`EntityBrainyZombie`) also draws the giant brainy zombie, which
  extends it;
* `RenderTaintSporeSwarmer` draws its own type;
* `RenderGolemBase`, `RenderPech`, `RenderTravelingTrunk`, `RenderWisp`,
  `RenderEldritchWarden`, `RenderEldritchCrab`, `RenderEldritchGolem`, `RenderWatcher`,
  `RenderThaumicSlime`, `RenderTaintVillager`, `RenderTaintSheep`, `RenderTaintPig`,
  `RenderTaintCow`, `RenderTaintChicken`, `RenderTaintCreeper`, `RenderTaintSpider`,
  `RenderMindSpider`, `RenderEldritchGuardian`, `RenderFireBat`, `RenderBrainyZombie`,
  `RenderDart`, `RenderPechBlast`, `RenderPrimalOrb`, `RenderEmber`, `RenderAlumentum`,
  `RenderAspectOrb`, `RenderFallingTaint`, `RenderFrostShard`, `RenderGolemBobber`,
  `RenderEldritchOrb` draw their own type only.

The guard test asserts the item entity rule, so a renderer typed to a subclass of
`ItemEntity` cannot come back.

## 6. The central table and the guard test

**`common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java`**
is now the single place where the entity renderers are registered. It holds

* `ENTITY_TYPES` — the 56 entity types of the port, and
* `BINDINGS` — the 56 (entity type, renderer) bindings, the native mirror of the original
  `ClientProxy.setupEntityRenderers`,

and it exposes the pure rule

```java
public static Set<String> entityTypesWithoutRenderer(Collection<String> entityTypeIds)
```

which the production `registerAll()` calls (it throws when a type has no renderer, so a
missing renderer is a readable failure at the start of the client instead of a
`NullPointerException` in the middle of a frame). The thirteen client bootstrap classes
that used to register the renderers one by one now only do their own work; the
registrations moved into the table, so the table cannot drift away from the production
registration.

**`common/src/test/java/thaumcraft/client/renderers/entity/EntityRendererCoverageTest.java`**
is the guard:

* `everyEntityTypeOfThePortHasAClientRenderer` — the entity types the port really
  registers (read from the registration classes by reflection through the existing
  `thaumcraft.assets.RegistryInventory`) minus the types with a renderer is empty;
* `noRendererIsRegisteredForAnUnknownEntityType` — the reverse direction;
* `theEntityTypeListOfTheRenderersIsTheOneOfTheRegistrationClasses` — the table lists
  exactly the registered entity types, so a new entity type that is added to a registration
  class without a renderer fails the test;
* `theSevenEntityTypesWithoutARendererHaveOneNow` — the regression of this defect;
* `theItemEntityRenderersAcceptTheCopyOfAnItemEntity` — the copy-cast rule.

The test does not construct a `Block` and does not touch the game registries: it reads the
registry ids of the `RegistrySupplier`s, which needs no registration.

**Not executed.** The tests are authored; this agent may not run Gradle, so they have not
been run. What *was* verified here, without Gradle:

* every changed and new source file of `common/src/main/java` was type-checked with a
  standalone `javac` run against the remapped 1.21.1 jar of the Loom cache
  (`minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2.jar`) plus the
  Architectury and library jars of the Gradle cache: **zero errors in the changed files**
  (the run reports 65 errors, all of them the pre-existing `BlockEntityType.Builder.of`
  access widening that the real build applies through Loom, none of them in a changed
  file);
* the 56 ids of the table, the 56 ids of the bindings and the 56 ids of the registration
  classes were compared mechanically and are equal.

## 7. The files

New:

* `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java`
* `common/src/main/java/thaumcraft/client/renderers/entity/RenderGolemOrb.java`
* `common/src/main/java/thaumcraft/client/renderers/entity/RenderPrimalArrowEntity.java`
* `common/src/main/java/thaumcraft/client/renderers/entity/RenderTaintCrawler.java`
* `common/src/main/java/thaumcraft/client/renderers/entity/RenderTaintSeed.java`
* `common/src/main/java/thaumcraft/client/renderers/models/entities/ModelTaintSeed.java`
* `common/src/test/java/thaumcraft/client/renderers/entity/EntityRendererCoverageTest.java`

Changed:

* `common/src/main/java/thaumcraft/client/ClientSetup.java` — calls the table
* `common/src/main/java/thaumcraft/client/renderers/entity/RenderElectricOrb.java` — no
  longer `final` (the golem orb renderer extends it)
* `common/src/main/java/thaumcraft/common/entities/projectile/EntityBottleTaint.java` —
  implements `ItemSupplier`
* `common/src/main/java/thaumcraft/common/entities/projectile/EntityPrimalArrow.java` —
  `spentLife()`
* `common/src/test/java/thaumcraft/assets/RegistryInventory.java` — made public
* the twelve client bootstrap classes that used to register the entity renderers

## 8. What is still open

* The **visual** result of the two 1.12.2 backport renderers (the tainted seed and the
  tainted crawler) can only be judged on a running client; the tainted seed uses the
  taint tentacle sheet because the backport sheet is not in the port's asset tree.
* The seven renderers are **not runtime verified**: the client run that found the seven
  missing types has to be repeated by the leader. The acceptance criteria are in section 4:
  a thrown bottle of taint must show the bottle item, a golem orb the electric billboard
  (red row for the red orb), a primal arrow the vanilla arrow mesh with the primal wisp
  trail, a tainted crawler a silverfish, a tainted seed a small taint tentacle and a giant
  taintacle a long taint tentacle — and, above all, the client must not throw a
  `NullPointerException` in `EntityRenderDispatcher.getRenderer` any more.
* The unit tests are authored but not executed (no Gradle in this agent).
