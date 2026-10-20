# Damage sources and the essentia source helper — 2026-10-21

Two defects of the port-fidelity audit of 2026-10-21 (`docs/analysis/2026-10-21-audit-a9-core.md`,
defects 5 and 6) are fixed. Oracle: TC4 4.2.3.5, the only source of truth.

## 1. The four original damage sources

The original published FOUR ready-made sources (oracle
`thaumcraft/api/damagesource/DamageSourceThaumcraft.java:19-22`):

| source | oracle | native |
|---|---|---|
| `taint` | `:19` `setDamageBypassesArmor().setMagicDamage()` | `DamageSourceThaumcraft.TAINT` |
| `tentacle` | `:20` | `DamageSourceThaumcraft.TENTACLE` |
| `swarm` | `:21` | `DamageSourceThaumcraft.SWARM` |
| `dissolve` | `:22` `setDamageBypassesArmor()` | `DamageSourceThaumcraft.DISSOLVE` |

Forced 1.21.1 change: 1.7.10 built the `DamageSource` in the constructor of the class, so the four
sources were static fields. 1.21.1 builds every source from a `Holder<DamageType>` of the datapack
registry, so the native API publishes the `ResourceKey<DamageType>` of every type plus the factory
that returns the ready source (`taint(Level)`, `dissolve(Level)`, `causeTentacleDamage(LivingEntity)`,
`causeSwarmDamage(LivingEntity)`). The type ids live in the namespace of the mod, so they never
collide with the vanilla ones.

### The datapack records

`data/thaumcraft/damage_type/dissolve.json` is new, `data/thaumcraft/damage_type/taint.json` keeps its
shape and its message id:

```json
{"message_id":"thaumcraft.dissolve","exhaustion":0.3,"scaling":"never"}
{"message_id":"thaumcraft.taint","exhaustion":0.3,"scaling":"never"}
```

* `exhaustion` 0.3 — the hunger damage of a 1.7.10 damage source is 0.3 (`DamageSource.hungerDamage`)
  and none of the four sources changes it. The native `tentacle` and `swarm` records already carried
  0.3; the `taint` record carried 0.0, which is the value of `minecraft:poison` and not an oracle
  value, so it is aligned to the oracle now.
* `scaling` `never` — the original calls `setDifficultyScaled()` on none of the four sources.
* `effects` absent — the default is `hurt`; the original sets neither fire nor projectile damage.
* `message_id` — 1.21.1 splits the identity of a type (`thaumcraft:dissolve`) from the key of its
  death message. The native port namespaces the key of the two unattributed sources
  (`thaumcraft.taint`, `thaumcraft.dissolve`), the same convention the native taint record already
  used, and the vanilla language has no `death.attack.dissolve` to collide with. Both languages carry
  the two keys of the new source.

`data/minecraft/tags/damage_type/bypasses_armor.json` now holds `thaumcraft:taint` and
`thaumcraft:dissolve`: the oracle calls `setDamageBypassesArmor()` on exactly those two (`:19`, `:22`).
The `thaumcraft:is_magic` tag keeps `thaumcraft:taint` only: `setMagicDamage()` is called on the taint
alone.

### The code that deals the damage

* **The taint poison** — `PotionFluxTaint` (oracle `thaumcraft/api/potions/PotionFluxTaint.java:56`
  and `:58`) now hurts with the published `DamageSourceThaumcraft.taint` source instead of a private
  copy of the key.
* **The death fluid** — `BlockFluidDeath.entityInside` (oracle
  `thaumcraft/common/blocks/BlockFluidDeath.java:57`) now hurts with
  `DamageSourceThaumcraft.dissolve` instead of the vanilla magic damage.
* **The taint creeper** — its explosion (oracle `EntityTaintCreeper:166`) is created with the creeper
  as the exploder, so the explosion itself keeps the vanilla attributed explosion source, exactly as
  the original. The poison that the explosion applies (oracle `:172`) is the original taint poison, and
  its damage is now the published taint source.

## 2. The essentia source helper

The original helper (oracle `thaumcraft/api/aspects/AspectSourceHelper.java:21-39`) reached
`thaumcraft.common.lib.events.EssentiaHandler` by reflection. The handler looked for ANY
`IAspectSource` around the tile and honoured both the direction and the range (oracle
`EssentiaHandler:84-134`): two axes are full (`-range..range`), the axis of the direction holds only
the cells on that side (`0..range-1`), and the unknown direction of 1.7.10 became UP with the inner
loop running from `-range` (oracle `:96-99`). The drain took one unit of the aspect out of the first
source of that order that held it (oracle `:49-56`), the search asked every source with
`doesContainerContainAmount(aspect, 1)` (oracle `:71-78`).

The native helper accepted only a `TileInfusionMatrix` and ignored the direction and the range. It
now walks the original half box in the original order and works with any `IAspectSource` of the
world. The geometry lives in the new `thaumcraft.common.lib.essentia.EssentiaSourceSearch`, which the
infusion matrix uses as well, so the matrix and the public helper walk the very same cells in the very
same order.

Forced 1.21.1 changes and remaining differences:

* `ForgeDirection` became `Direction`; the unknown direction of 1.7.10 is written as a `null`
  direction.
* The reflective lookup became a direct call, because the native handler owns the search itself.
* The original cached the sources of a tile for five seconds and punished a failed drain with the
  same delay. The native helper searches on every call, so it never returns a stale source list; the
  infusion matrix keeps its own cache and its own delay.
* The original sent its essence flow packet to the players around the tile (oracle `:54`). The native
  port has no packet for an arbitrary consumer; the matrix keeps its own effect for its own drain.
* The mutual link of two essentia mirrors (oracle `:124`) has no native counterpart: the native
  `TileMirrorEssentia` is not an `IAspectSource`, it owns its own `takeEssentia` contract.

## 3. Tests

* `thaumcraft.common.lib.essentia.EssentiaSourceSearchTest` — the pure geometry: the half box of every
  direction, the order of the walk, the skipped tile and the number of the visited cells.
* `thaumcraft.assets.DamageTypeRecordsTest` — the records of the four sources, the armour bypassing
  tag, the magic tag and the death messages of both languages.
* `thaumcraft.api.DamageSourceThaumcraftTest` — the four published keys and their namespaced ids.

## 4. Not fixed here

* The native essentia mirror is not an `IAspectSource` (see above). That is the eldritch domain.
* The taint poison checks the undead tag, while the oracle checks `isImmuneToFire()`
  (`PotionFluxTaint:55`/:57`). That is the taint domain.
* The `tentacle` and `swarm` records scale with the difficulty, while the oracle scales none of the
  four sources. That is the taint/eldritch domain; the records were not touched here.
* The original drops crystal essence when a mob is dissolved (oracle `EventHandlerEntity:468`). The
  native port has no entity aspect scan at all, so that behaviour needs the scanning subsystem first.
