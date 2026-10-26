# Thaumcraft content, entities and the world

## Reaching Thaumcraft's items and blocks

Every item and block has a 1.21.1 id in the `thaumcraft` namespace.

| Need | Call |
|---|---|
| an item by id | `ItemsTC.require("thaumium_ingot")` (throws when unknown), `ItemsTC.get(...)` (null when unknown), `ItemsTC.stack("salis_mundus", 4)` |
| a block by id | `BlocksTC.get("arcane_stone")`, `BlocksTC.require(...)` |
| a 1.7.10 `ConfigItems`/`ConfigBlocks` field with metadata | `ItemApi.getItem("itemResource", 14)` (salis mundus), `ItemApi.getBlock(name, meta)`: the original names resolve to the flattened 1.21.1 item, block or state |

The whole table of original names is [LEGACY_IDS.md](../LEGACY_IDS.md). Call these from `onThaumcraftReady` or later:
Thaumcraft's items may not exist when your own `onInitialize` runs.

## Infusion stabilisers

A block that implements `IInfusionStabiliser` (`canStabaliseInfusion(level, pos)`) steadies an infusion when it stands
symmetrically around the altar, like candles and skulls do.

## Entities

| Interface | Effect |
|---|---|
| `IEldritchMob` | an eldritch mob: Thaumcraft's eldritch mobs treat it as an ally and do not attack it |
| `ITaintedMob` | a tainted creature: flux gas, taint bottles and taint conversion pass it by, and taintacles and taint creepers do not target it |
| `IThaumcraftBoss` | a boss is never promoted to a champion; `generateName()` names it after its champion modifier |

Entity tags (the aspects of an entity) are on the [aspects](aspects.md#entity-tags) page.

## Damage and effects

Damage types are data in 1.21.1, so the damage sources need a level or an entity:

| 1.7.10 | 1.21.1 |
|---|---|
| `DamageSourceThaumcraft.taint` | `DamageSourceThaumcraft.taint(level)` |
| `DamageSourceThaumcraft.dissolve` | `DamageSourceThaumcraft.dissolve(level)` |
| `causeTentacleDamage(entity)`, `causeSwarmDamage(entity)` | the same names |
| `DamageSourceIndirectThaumcraftEntity` | `DamageSourceIndirectThaumcraftEntity.create(direct, cause, type)` |

The original potions are registered mob effects: `PotionFluxTaint.instance()` / `holder()` / `KEY`, and the same for
`PotionVisExhaust`. Apply one with `new MobEffectInstance(PotionFluxTaint.holder(), duration, amplifier)`.

## World generation

| Call | Effect |
|---|---|
| `ThaumcraftApi.addDimBlacklist(ResourceKey<Level>, level)` | the original numeric dimension blacklist; 0 or 2 suppress Thaumcraft's ores and aura, every level suppresses its vegetation and structures |
| `ThaumcraftApi.addBiomeBlacklist(ResourceKey<Biome>, level)` | keeps Thaumcraft's vegetation and ores out of a biome (aura nodes still form) |
| the block tag `thaumcraft:portable_hole_blacklist` | blocks the portable hole focus will not open (the original `portableHoleBlackList`, now data) |

## Loot bags

`ThaumcraftApi.addLootBagItem(stack, weight, bagTypes...)` adds an entry to Thaumcraft's loot bags; bag types are 0
common, 1 uncommon, 2 rare (the example adds its shard to the first two; see [recipes](recipes.md#crafting-extras)).

## Not public API

- Golems had no API in 1.7.10 and have none now.
- The Thaumcraft 6 shaped classes of `thaumcraft.api` (`theorycraft`, `ResearchEntry`/`ResearchStage`,
  `ScanningManager`, `IWandFocus`) are compatibility shells for add-ons compiled against 1.12; they are not wired into
  Thaumcraft 4 gameplay. Use the TC4 classes in these guides.
- Everything outside `thaumcraft.api` (`thaumcraft.common`, `thaumcraft.client`) may change without notice.
