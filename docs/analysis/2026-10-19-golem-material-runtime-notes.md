# Bounded golem materials — common runtime source freeze, 2026-10-19

**Implemented, not compiled or tested by this author.** Root's preceding
Straw/Gather main build PASS is historical evidence for its preceding inputs,
not validation of these material changes. Endgame gameplay checks remain
deferred; no game, survival, visual, whole-golem or NeoForge parity is claimed.

## Owned changes and API

Only these production files were changed/added:

- `common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java`
- `common/src/main/java/thaumcraft/common/entities/golems/EnumGolemType.java` (new)

Root's **`thaumcraft.common.container.ContainerGolem` import is preserved**.
The prior Straw author's note naming the original menu package is superseded by
that integrated import. `getBootupTimer()` remains `float`; no model/client file
was touched. Goals, home-inventory helper, items/data, registry, recipes, GUI,
loot tables, access widener, shared Bore/drop code and worlds were not edited.
No other author's WIP was read and no agents were spawned.

New public API:

```java
// EnumGolemType
public static EnumGolemType getType(int material); // nullable, never substitutes another ordinal
public static boolean isSupported(int material);  // only 0, 1, 2, 3, 5, 6, 7

// EntityGolemBase
public int getGolemMaterial();                     // server identity / native client synced identity
public void setGolemMaterial(int material);        // native fresh server candidate only
```

The item author must call `setGolemMaterial(itemMaterial)` **before**
`applyConfiguration(GolemData)`, on a newly created, unadmitted `ServerLevel`
candidate. Unsupported IDs (including known-but-deferred Flesh 4) throw
`IllegalArgumentException`. Non-native/client levels, ticked/admitted/removed
entities, existing cargo, unsupported retained state or an active transaction
are refused with `IllegalStateException`. Both actual entity ID and UUID
admission are checked, even if the current chunk is unavailable. This setter
is not a live transmutation/healing API.

The new root-owned integration dependency is:

```java
ThaumcraftGolems.placerForMaterial(int material) // Item; null for unsupported/unavailable
```

Both recall modes use that factory, not `GOLEM_STRAW` unconditionally. A null
factory result refuses recall before payment, cargo clearing or discard.
Existing core, filter, home, owner, menu and recall APIs otherwise remain
unchanged. `GolemData` still contains only core plus six ghost exemplars:
material identity is the actual placer item ID, not an extra component field.

Native display names now override only protected `getTypeName()`, returning
`placerForMaterial(getGolemMaterial()).getDescription()` when available and the
generic native superclass name otherwise. Native custom-name precedence remains
intact; no `getName()` override or material-specific entity type is introduced.

## Exact original material table

Read-only gameplay authority:
`../work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/entities/golems/EnumGolemType.java:10-26,43-51`.
All eight constants retain original order, properties and public field names.
Presence in this table does **not** register or authorize a future material.
Per the content-scope update, root reports the Tallow/Thaumium prerequisite blocks
final and integrated (`docs/analysis/2026-10-19-golem-material-blocks.md`). Their
materials 2 and 7 therefore join this same batch. Only Flesh 4 remains blocked
on unported Taint block 2; no resource/registry WIP was read or changed here.

| Ordinal | Type | Health | Armor | Speed | Fire resist | Upgrade capacity | Carry | Regen counter | Strength | Fresh support |
| --- | --- | ---: | ---: | ---: | --- | ---: | ---: | ---: | ---: | --- |
| 0 | STRAW | 10 | 0 | .38 | false | 1 | 1 | 75 | 0 | yes |
| 1 | WOOD | 20 | 6 | .35 | false | 1 | 4 | 75 | 1 | yes |
| 2 | TALLOW | 20 | 9 | .33 | false | 2 | 8 | 75 | 2 | yes |
| 3 | CLAY | 25 | 9 | .33 | true | 1 | 8 | 100 | 2 | yes |
| 4 | FLESH | 15 | 6 | .35 | false | 2 | 4 | 40 | 1 | **deferred** |
| 5 | STONE | 30 | 12 | .32 | true | 1 | 16 | 100 | 3 | yes |
| 6 | IRON | 35 | 15 | .31 | true | 1 | 32 | 125 | 4 | yes |
| 7 | THAUMIUM | 40 | 15 | .32 | true | 2 | 32 | 100 | 4 | yes |

Additional original anchors in `EntityGolemBase.java`:

- `235-264,1088-1099`: native material health/armor and attack base
  `2 + material strength` (no decorations/upgrades in this slice).
- `355-383,793-798`: material carry/remaining space and material movement speed.
- `427-437`: decrement a positive counter, otherwise reset to material delay
  and heal 1. Supported steady recurrences in ordinal order are
  **76/76/76/101/101/126/101 ticks**.
- `493-497,736-743`: per-material fire resistance, not global entity-type immunity.

Native metadata was consulted only to confirm public overridable
`Entity.fireImmune()`, protected `Entity.getTypeName()` and public
`Item.getDescription()` in 1.21.1, not as a donor gameplay specification.

## Runtime parameterization and stock preservation

- Native base attributes are selected from the validated table: maximum health,
  armor, movement speed and attack damage only. Navigation's existing modifier
  remains **1**, avoiding an accidental second material-speed multiplication.
  Native modifiers and unrelated saved attributes are not cleared/replaced.
- A legitimate fresh material selection fills health to its new native maximum.
  **Load does not refill.** After native super loading, the original saved
  `Health` value (or current native health if absent) is retained while only the
  four canonical material bases are applied, then restored through native
  `setHealth`. This also recovers legal supported-material saved health if
  absent attribute NBT caused super loading to temporarily use Straw's maximum.
  Unsupported retained configurations do not receive those material-base writes.
- `fireImmune()` reads the live material table, including on the client through
  native synced material identity. Straw/Wood/Tallow burn normally;
  Clay/Stone/Iron/Thaumium are immune. No entity-type-wide fire-immune flag is
  introduced.
- `getGolemStrength()` / `getCarryLimit()` now select actual table properties;
  invalid ordinals do not silently become Straw or Wood. Existing carried stock
  is never truncated to a new limit on load.
- Empty carry capacity is the material limit. Occupied capacity is
  `max(0, min(materialCarry, itemMaximum) - actualCount)`. Pickup additionally
  caps the transfer by source count and the source's native item maximum. Thus
  a low-max anomalous overstack cannot become an illegal held stack; its excess
  stays in the same physical source entity, with native components/identity,
  UUID, age, motion and ownership retained.
- The already-carrying candidate rule remains strict native component equality
  plus **the candidate's whole count fitting remaining space**. The existing
  admission fence, live debit-before-feedback ownership writes, local busy guard,
  six one-count ghost filters, home merge-before-empty insertion, sided slot
  permissions and retained unsatisfied remainder are unchanged.
- `GolemType` remains an entity NBT integer. New `MATERIAL` native synced data
  supplies the raw identity to client renderer/GUI before rendering from that
  tracked state. The single living entity type and .4 × .95 dimensions remain
  root-owned and unchanged.
- Work/configured deployment/configured snapshots/recall all reject unsupported
  ordinals independently of enum-table presence. Unknown/future or deferred raw
  material/configuration/cargo still uses the existing frozen-retention branch,
  not a lossy conversion into a supported placer. Client visual fallback for an
  unknown material is not deployment/work authorization.
- Normal and dismantling recall emit the correct material's placer item; normal
  recall retains core/filter data and native custom name, dismantling separates
  the fitted Gather core. Real cargo remains separately queue-owned, never a
  component inventory. Death still queues only real cargo.

No goals/pathfinding/home-inventory behavior was refactored. Repair effect,
doors, owner attribution/no owner-only lock, menu pausing, cookie behavior,
bell attack classification, durable drop path and unrelated native mobs/cows
remain at the integrated Straw/Gather behavior. There are no new upgrades,
advanced behavior, other cores or registered unsupported variants here.

## Remaining verification / source hashes

No Gradle, Java, Python, tests, game, world or smoke invocation occurred. Root
still owns the actual final integrated Fabric compile. Dynamic attributes,
damaged save/reload, material fire behavior, varied carrying, low-max anomalous
source remainder, material-specific recall and visuals are **not runtime-tested**
by this author. Native movement composition is not an exact old/new tick proof.
Only Flesh remains prerequisite-blocked; broader compatibility
and the prior optional chest/fence-gate/client-boot-sound gaps remain deferred.

Frozen production SHA-256:

```text
df9ce6ba7bbcc80570fc570806867c9aba0c16cbc88d0e2d0fbf8b6bee12241e  common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java
be75c178cc7ce4af44acdc01fb5ee890a2a1b218c7ff38e743972ee846e91317  common/src/main/java/thaumcraft/common/entities/golems/EnumGolemType.java
```

Unchanged helper SHA-256 (identical to their prior final sources):

```text
3c17ec124677ae5fa63a3f871480227462d2082d1b12a3f432de786ad162c165  common/src/main/java/thaumcraft/common/entities/golems/GatherGolemGoals.java
c73c998ed1a8d753d6c802a7a4fd7845c3982f5a8bdd81fd3448c031e0c46404  common/src/main/java/thaumcraft/common/entities/golems/GolemHomeInventory.java
```
