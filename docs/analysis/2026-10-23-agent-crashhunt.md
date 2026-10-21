# Cross-cutting crash hunt — Thaumcraft 4.2.3.5 → Minecraft 1.21.1

Agent: `crashhunt` (parallel port wave, 16 agents)
Worktree: `/home/cyberfox-kali/wt/crashhunt`, branch `agent/crashhunt`
Date: 2026-10-23

## 1. Scope and method

This agent owns no gameplay family. It hunted **crashes and game-breaking defects across the whole
port**, fixed the ones that live in files it owns, and routed the rest.

Owned and therefore edited:

| Path | Note |
|---|---|
| `common/src/main/java/thaumcraft/common/lib/utils/**` | shared helper layer |
| `common/src/main/java/thaumcraft/common/lib/network/**` | Architectury payload layer |
| `common/src/main/java/thaumcraft/common/lib/events/**` | event helpers |
| `common/src/main/java/thaumcraft/common/lib/enchantment/**` | enchantment identities |
| `common/src/main/java/thaumcraft/common/lib/potions/**` | mob effects |
| `fabric/src/main/java/**`, `neoforge/src/main/java/**` | loader bootstrap and mixins (except the two excavation mixins, which the leader owns) |

Method:

1. `grep`/`rg` over `common/src/main/java`, `fabric/src/main/java`, `neoforge/src/main/java`
   for every crash idiom of the port checklist (383 `.getValue(` sites, 391 `getBlockEntity(`
   sites, every `Registry`/`BuiltInRegistries` lookup, every `Optional.get()`, every
   `lookupOrThrow`/`getOrThrow`, every `ItemStack` component read, every C2S `StreamCodec`).
2. Every candidate was checked against the **real 1.21.1 API**, not from memory: the decompiled
   Minecraft 1.21.1 jar from the Gradle Loom cache was decompiled with CFR into
   `/tmp/opencode/mcsrc` and read (`StateHolder.getValue`, `StateHolder.setValue`,
   `CompoundTag.getList`, `NbtUtils.loadUUID`, `Registry.getHolder`, `HolderLookup.lookup`,
   `BlockEntity.getBlockState`, `BlockableEventLoop.doRunTask`, `ChunkSource.getChunkNow`,
   `ListTag.add`, `Entity.setRemoved`).
3. Every candidate was checked against the **1.7.10 original**: the pinned reference
   `/home/cyberfox-kali/OpenTC4` and the decompiled original JAR
   (`/tmp/opencode/tc4src`, CFR of `Thaumcraft-1.7.10-4.2.3.5.jar`). A crash the original does
   not have is a port defect; a crash the original has is a documented original quirk.
4. Ownership was respected: **nothing outside the owned paths was edited.**

## 2. Defect table

Legend — *Port-only*: the 1.7.10 original cannot hit it. *Original too*: the original has the same
defect (documented, not "fixed" unless the port can prove it). *Fixed*: changed in this branch.
*Routed*: the file belongs to another agent, the exact fix is in §4.

| # | file:line | trigger | crash type | port-only? | fixed? |
|---|---|---|---|---|---|
| D1 | `neoforge/src/main/java/thaumcraft/neoforge/NeoForgeLootTables.java:66` | any load of one of the ten mapped vanilla chest loot tables (`minecraft:chests/dungeon_chest`, `desert_pyramid`, `jungle_temple`, `abandoned_mineshaft`, three `stronghold_*`, three `village_*_smith`/`_armorer`) on NeoForge | inverted guard: `!root.getAsJsonArray("pools").isEmpty()` returns early for **every** real chest table, so the TC4 chest rows are **never** appended (parity loss vs Fabric), and the code below is reachable only for an **empty** `pools` array, where `pools.get(0)` throws `IndexOutOfBoundsException` | port-only | **yes** |
| D2 | `common/src/main/java/thaumcraft/common/lib/network/AccessoryNetworking.java:27` | the `thaumcraft:accessories_contents` S2C payload arrives while the local player is being torn down (disconnect race) → `context.getPlayer()` is null | `NullPointerException` in `PlayerAccessories.get(null)` → `((AccessoryHolder)null).thaumcraft$accessories()`. **The only S2C receiver in the whole port that dereferences `context.getPlayer()` without a null check** (all 12 siblings check it) | port-only (1.7.10 had no such packet) | **yes** |
| D3 | `common/src/main/java/thaumcraft/common/lib/utils/SpecialMining.java:68` | a dropped item whose registry name is unknown | `RESULTS` is an immutable `Map.of`; `Map.get(null)` throws `NullPointerException` (`probe(pk)` calls `pk.hashCode()`) | port-only (1.7.10 ore dict) | **yes** |
| D4 | `common/src/main/java/thaumcraft/common/lib/utils/SpecialMining.java:91` | the cluster replacement item is not in the registry | `BuiltInRegistries.ITEM.get(...)` → null → `new ItemStack(null, count)` → NPE inside `ItemStack`'s `item.asItem()`. Hits the **block break path** of every Core pickaxe / Dowsing mining | port-only | **yes** |
| D5 | `common/src/main/java/thaumcraft/common/lib/utils/InventoryUtils.java:526` | the enchantment registry or the `minecraft:unbreaking` entry is absent | `RegistryAccess.lookupOrThrow` → `IllegalStateException("Registry ... not found")`; `Registry.getOrThrow` → `IllegalStateException("Missing key in ...")` — inside `damageItem`, i.e. the durability path of every item | port-only | **yes** |
| D6 | `common/src/main/java/thaumcraft/common/lib/enchantment/EquipmentEnchantments.java:18` | the enchantment level is read with a null stack or a detached world | `stack.isEmpty()` → NPE; `world.registryAccess()` → NPE; `registryOrThrow` → `IllegalStateException`. Reached from the **living tick** of every player (`PlayerEnchantmentTickMixin` → `EquipmentEnchantments.playerLivingTick`) | port-only (1.7.10 enchantment ids were ints) | **yes** |
| D7 | `common/src/main/java/thaumcraft/common/lib/utils/CropUtils.java:44` | `key(Block, stage)` for a block the registry does not know | `BuiltInRegistries.BLOCK.getKey(block)` → null → `.toString()` → NPE. `key` is called by **every** crop predicate (growth lamp, harvesting focus, golem replanter) | port-only | **yes** |
| D8 | `common/src/main/java/thaumcraft/common/lib/utils/BlockUtils.java:128` | the following-drop spawner runs on a level that is not a `ServerLevel` | unchecked `(ServerLevel) level` cast → `ClassCastException` | port-only (1.7.10 had one world type) | **yes** |
| D9 | `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:56` | the mirror block at `worldPosition` is no longer a `BlockMirror` | `level.getBlockState(worldPosition).getValue(BlockMirror.FACING)` → `IllegalArgumentException("Cannot get property facing as it does not exist in ...")` (`StateHolder.getValue` throws for a missing property) | port-only | routed §4.1 |
| D10 | `common/src/main/java/thaumcraft/common/tiles/TileMirrorEssentia.java:204` | same as D9 | same `IllegalArgumentException` | port-only | routed §4.1 |
| D11 | `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:143` | `knowledge(String username)` is called before any `knowledge(Player)` has stored the server reference | `SERVER.get()` is null → `.get()` → NPE. `SERVER.set(...)` only happens inside `knowledge(Player)` | port-only | routed §4.2 |
| D12 | `common/src/main/java/thaumcraft/client/gui/GuiResearchTable.java:109,111`; `client/gui/ThaumcraftRecipeDisplay.java:592`; `client/gui/GuiResearchBrowser.java:225`; `client/ThaumcraftBaublesClient.java:27`; `api/research/ResearchItem.java:84`; `api/internal/ThaumcraftInternalMethods.java:54`; `common/crafting/CrucibleRecipe.java:51`; `common/lib/research/ObjectAspectResolver.java:299,305` | a research definition or a recipe whose aspect tag is not registered | `Aspect.getAspect(tag)` returns null → `.getColor()` / `.getName()` / `getAmount(null)` → NPE. **The port itself proves the tag can be unknown**: `ResearchPurchase.validate` rejects with `UNAVAILABLE` when `Aspect.getAspect(cost.getKey()) == null` | port-only | routed §4.3 |
| D13 | `neoforge/src/main/java/thaumcraft/neoforge/ThaumcraftNeoForge.java:41-56` | every player login on NeoForge | not a crash: a leftover `DIAG` `PlayerLoggedInEvent` listener logs five values at INFO per login | — | hygiene, see §5 |
| D14 | `common/src/main/java/thaumcraft/common/lib/network/ResearchTableEditNetworking.java:100-104` | a `RuntimeException` inside the research-table edit | **not** a server crash: the handler runs in `BlockableEventLoop.doRunTask`, which catches `Exception` and logs it with the FATAL marker; the server survives. Reported so it is not mistaken for a crash | — | no change |

### 2.1 Checked and found clean (negative results, so they are not repeated)

These were hunted and are **safe**; recording them avoids a second sweep.

* **Every `.getValue(` site (383).** The property owner of every `Block*.PROPERTY` was extracted
  mechanically and each dynamic call site was checked for an `is(...)`, `hasProperty(...)` or
  `instanceof` guard. Every unguarded one is on the block entity's **own** cached state.
  `BlockEntity.getBlockState()` returns the **cached** `blockState` in 1.21.1
  (`mcsrc/net/minecraft/world/level/block/entity/BlockEntity.java:199`), so
  `getBlockState().getValue(...)` on a block entity is always safe — only the *live world* read
  (`level.getBlockState(pos).getValue(...)`) is dangerous, and that is exactly D9/D10.
* **Vanilla vs. TC4 property instances.** `BlockStateProperties.FACING/LIT/AGE_7/...` are shared
  instances (`BlockMnemonicMatrix.FACING == BlockStateProperties.FACING`,
  `CropBlock.AGE == BlockStateProperties.AGE_7`, `BlockAlchemyFurnace.LIT == BlockStateProperties.LIT`),
  so `getValue(BlockStateProperties.X)` on a TC4 block is valid. The TC4 blocks that declare their
  **own** `facing`/`open`/`half`/`age` instance (`BlockArcaneDoor`, `BlockMirror`,
  `BlockArcaneLamp`, `BlockEssentiaLamp`, `BlockManaPod`) are only read through their own
  constant, except `ThaumcraftOuterLands.java:292-294`, which is guarded by `hasProperty(...)`.
* **Entity attributes (both loaders).** All 58 registered entity ids (57 entity classes) were
  listed and matched against the attribute catalogue. The fifteen direct catalogue entries of the
  Fabric bootstrap plus the four shared catalogues (`ThaumcraftEldritchMobs`,
  `ThaumcraftCultists`, `ThaumcraftTaintCreeper`, `ThaumcraftTaintAnimals`) cover every
  `LivingEntity`; the remaining types are `Entity`, `ItemEntity` or `Projectile` and need no
  attributes. `NeoForgeEntityAttributes` mirrors the Fabric list exactly.
* **`BlockEntityType.Builder.of(...)`.** Every valid-block list matches the block the tile is
  attached to (`MIRROR`/`MIRROR_ESSENTIA_TILE` → `MIRROR`, `GRATE_BLOCK_ENTITY` → `ITEM_GRATE` +
  `METAL_DEVICE`, `TILE`/`NOZZLE`/`PART_TILE` → the advanced furnace, …).
* **NBT.** `Tag.TAG_*` still exist in 1.21.1 (and gained `TAG_LONG_ARRAY`, which the 1.7.10
  original never had, so `Utils.getNBTDataFromId` stays faithful). `CompoundTag.getList(key, type)`
  answers an **empty** list — not an exception — on an element-type mismatch, and `ListTag.add`
  refuses a mismatching type, so the `(CompoundTag) value` casts in `AspectOrbQueue.load` and
  `DeferredItemDrops.load` are safe. `CompoundTag.hasUUID` validates the `IntArrayTag` of length 4
  before `getUUID` reads it (`NbtUtils.loadUUID` throws for a wrong type/length).
* **Aspect registry.** All 47 aspect tags used by `data/thaumcraft/tc4/research.json` are among
  the 48 registered aspects (only `tempestas` is unused by the research data).
* **C2S payload decoders.** Every `StreamCodec` in `lib/network` bounds its lengths and validates
  its indices before use; `FocusSelectionPayload` rejects an out-of-range `inventorySlot` through
  `FocusSelectionSources.valid`, `ResearchTableEditPayload` through its record constructor.
* **Threading.** Every network receiver defers with `context.queue(...)`; every server-thread
  sensitive entry point (`TileXxx` ticks, `VisNetHandler`, `EssentiaHandler`, the drop queues)
  asserts `isSameThread()`. No `Level` access was found on the netty thread.
* **Item stack mutation.** The port copies stacks before mutating them; `ItemStack.set`/`setCount`
  are only used on owned copies (`FocusSelectionNetworking` re-validates the identity of every
  slot it touches).

## 3. The fixes (all in owned files)

| file | change |
|---|---|
| `neoforge/.../NeoForgeLootTables.java` | the pool guard is corrected (`pools` **empty** → nothing to extend) and the selection is delegated to the new shared helper `common/.../lib/utils/ChestLootPoolRules.java`, which returns `null` for a table without pools. The TC4 chest rows now reach the first pool of the native table on NeoForge, exactly like the Fabric `modifyPools` adapter, and the empty-pool case can no longer index an empty array. |
| `common/.../lib/network/AccessoryNetworking.java` | the S2C receiver drops the snapshot when the context carries no player, like every other S2C receiver in the port. |
| `common/.../lib/utils/SpecialMining.java` | a missing stack / unknown registry name is "no listed result"; an unknown cluster item keeps the original drop. |
| `common/.../lib/utils/InventoryUtils.java` | the unbreaking level uses `lookup(...)` + `Optional` instead of `lookupOrThrow`/`getOrThrow`. |
| `common/.../lib/enchantment/EquipmentEnchantments.java` | the enchantment level answers `0` for a missing stack, a detached world or an unknown registry/entry. |
| `common/.../lib/utils/CropUtils.java` | the crop list key survives a missing registry name. |
| `common/.../lib/utils/BlockUtils.java` | the following-drop spawner receives the server level through an `instanceof` check. |
| `common/.../lib/utils/ChestLootPoolRules.java` (new) | the pure "first weighted pool of an encoded loot table" rule shared by the loader adapters. |

Every change is behaviour preserving for the states that already worked; only the crashing or the
dead branch changes.

## 4. Routed findings — precise fixes for the owning agents

### 4.1 `TileMirror` / `TileMirrorEssentia` read the live world state (devices agent)

`common/src/main/java/thaumcraft/common/tiles/TileMirror.java:56`

```java
public Direction getFacing() { return level == null ? Direction.NORTH : level.getBlockState(worldPosition).getValue(thaumcraft.common.blocks.BlockMirror.FACING); }
```

`common/src/main/java/thaumcraft/common/tiles/TileMirrorEssentia.java:204`

```java
public Direction getFacing() {
    if (level == null) return Direction.NORTH;
    return level.getBlockState(worldPosition).getValue(BlockMirror.FACING);
}
```

`StateHolder.getValue` throws
`IllegalArgumentException("Cannot get property facing as it does not exist in ...")` when the
state has no such property. The **live** world state at `worldPosition` is not guaranteed to be a
`BlockMirror`: any block replacement that leaves the tile in place (the port's
`StateDependentBlockEntity` family swaps blocks under one tile type) makes this a hard crash in
the mirror tick, in the renderer and in `ItemHandMirror.transport`
(`common/src/main/java/thaumcraft/common/items/relics/ItemHandMirror.java:175`).

Proposed fix (the block entity's own cached state always belongs to the mirror):

```java
// TileMirror.java:56 and TileMirrorEssentia.java:204
public Direction getFacing() { return getBlockState().getValue(BlockMirror.FACING); }
```

`BlockEntity.getBlockState()` returns the cached `blockState`, which for these tiles is always a
`BlockMirror` state and therefore always has `FACING`. `TileMirrorEssentia.java:180` already uses
the defensive form `state.getBlock() instanceof BlockMirror ? state.getValue(BlockMirror.FACING)
: Direction.NORTH`; that form is equally acceptable.

### 4.2 `knowledge(String)` dereferences a possibly absent server reference (coremisc agent)

`common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:143`

```java
MinecraftServer server = SERVER.get().get();
```

`SERVER` is an `AtomicReference<WeakReference<MinecraftServer>>` that is only written inside
`knowledge(Player)`; `knowledge(String)` can therefore run first and dereference a null
`AtomicReference` → NPE.

Proposed fix:

```java
var reference = SERVER.get();
if (reference == null) return null;
MinecraftServer server = reference.get();
if (server == null) return null;
```

### 4.3 A null aspect from `Aspect.getAspect(tag)` (research / clientrender / alchemy agents)

`Aspect.getAspect(String)` returns `null` for an unknown tag (`common/src/main/java/thaumcraft/api/aspects/Aspect.java:132`).
The port proves that unknown tags are possible: `ResearchPurchase.validate`
(`common/src/main/java/thaumcraft/common/lib/research/ResearchPurchase.java:84`) explicitly
rejects a cost whose aspect is unknown with `Failure.UNAVAILABLE`.

Proposed fix for each site — resolve the aspect once and skip/guard a null:

* `common/src/main/java/thaumcraft/client/gui/GuiResearchTable.java:109,111` (clientrender):
  `entry -> Aspect.getAspect(entry.getKey()).getName()` → sort and render with
  `Aspect.getAspect(entry.getKey())`, skipping a null aspect (or rendering the raw tag).
* `common/src/main/java/thaumcraft/client/gui/ThaumcraftRecipeDisplay.java:592` (clientrender):
  same — `Aspect aspect = Aspect.getAspect(cost.getKey()); if (aspect == null) continue;`.
* `common/src/main/java/thaumcraft/client/gui/GuiResearchBrowser.java:225` (clientrender): the
  aspect is already resolved into a local; add the null branch.
* `common/src/main/java/thaumcraft/client/ThaumcraftBaublesClient.java:27` (clientrender):
  `Aspect.getAspect(entry.getKey()).getColor()` → resolve with a null check.
* `common/src/main/java/thaumcraft/api/research/ResearchItem.java:84` (coremisc/research):
  same guard.
* `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:54` (coremisc):
  same guard.
* `common/src/main/java/thaumcraft/common/crafting/CrucibleRecipe.java:51` (alchemy):
  `input.aspects().getAmount(Aspect.getAspect(entry.getKey()))` → resolve the aspect first and
  treat a null aspect as "no amount".
* `common/src/main/java/thaumcraft/common/lib/research/ObjectAspectResolver.java:299,305`
  (research): `target.add(Aspect.getAspect(entry.getKey()), ...)` → resolve first and skip a null
  aspect instead of adding it.

### 4.4 Hygiene: the NeoForge login diagnostic (leader / NeoForge owner)

`neoforge/src/main/java/thaumcraft/neoforge/ThaumcraftNeoForge.java:41-56` registers a
`PlayerLoggedInEvent` listener that logs five DIAG values at INFO for **every** player login
(the connection identity, the channel id, whether the payload setup is present). It is not a
crash, but it is debug output in the production bootstrap. It was introduced together with the
NeoForge parity work (`f39307a7`) and is not referenced by any test or report. Suggested removal
of that listener block (the file is owned by this agent, so it is left in place here rather than
removed silently in a crash-fix commit).

## 5. Tests (authored, **NOT executed**)

`common/src/test/java/thaumcraft/crash/` (new package):

| file | what it pins |
|---|---|
| `ChestLootPoolRulesTest.java` | the first-pool selection of D1: a table with pools has a first pool, a table without pools has none, **an empty `pools` array has none and does not throw** (the exact input that used to reach `pools.get(0)`), a missing table has none, a non-object pool is ignored, and the returned pool keeps its own `entries` array. |
| `SpecialMiningSafetyTest.java` | D3/D4: a missing or empty stack is no listed result, a registered drop is a listed result, an unlisted drop is no result, an unknown replacement keeps the original drop and never carries a missing item, and the original roll boundary (`roll <= chance * factor`) and chance progression are unchanged. |
| `NullSafeRegistryLookupTest.java` | D5/D6/D7: the crop key of a missing block does not throw and the registered form is `minecraft:wheat#3`, a missing stack / detached world / unknown registry answers `0` for the enchantment and the unbreaking level, and the enchantment identities stay stable. |
| `AccessoryContentsPayloadTest.java` | D2's payload contract: exactly four slots, and the snapshot copies its stacks instead of aliasing the caller's stack. |

**These tests are authored but not executed.** This agent may not run Gradle (BRIEF rule 5) and the
sandbox has no `javac`/`javap` (JRE-only JDKs), so the tests were never compiled or run. The
leader must run `:common:test`.

## 6. Ledger

`docs/porting/scope-ledger.json` — evidence added for the five units whose file this agent fixed;
the JSON shape, the row order and `counts` are untouched and the file still round-trips byte for
byte through `json.dumps(..., ensure_ascii=False, indent=2) + "\n"`:

| unit | coverage | change |
|---|---|---|
| `thaumcraft/common/lib/utils/Utils.java` | `parity_verified` | evidence for the `SpecialMining` null-safe table and replacement lookup |
| `thaumcraft/common/lib/utils/InventoryUtils.java` | `parity_verified` | evidence for the null-safe unbreaking lookup |
| `thaumcraft/common/lib/utils/CropUtils.java` | `parity_verified` | evidence for the null-safe crop key |
| `thaumcraft/common/lib/utils/BlockUtils.java` | `parity_verified` | evidence for the checked server-level cast |
| `thaumcraft/common/lib/events/EventHandlerEntity.java` | `partial` (unchanged) | evidence for the null-safe enchantment level; the unit's documented gap (the original research file load/save) is structural and stays open |

`docs/porting/class-inventory.json` was **not** regenerated: it is already stale on the base commit
(34 recorded fingerprints and 6 native files out of date, all from other agents' merged work), and
regenerating it here would collide with the other 11 branches of this wave. The leader's
`update_migration_inventory.py --write` will pick up the new
`common/src/main/java/thaumcraft/common/lib/utils/ChestLootPoolRules.java` together with the other
pending files; `update_scope_ledger.py --write` must follow it, because the ledger stores the
canonical fingerprint of the inventory.

**`update_scope_ledger.py --check` could not be run in this sandbox**: the private TC4 oracle root
`/home/cyberfox-kali/wt/work/thaumcraft-4.2.3.5-oracle` does not exist here, and the script exits
with `Missing source root`. The ledger was therefore updated by hand in exactly the shape the
script writes, and the two invariants the check enforces were verified by hand instead:
the file round-trips byte for byte through the script's own serialisation, and
`inventory_provenance.canonical_json_sha256` still matches the (untouched) inventory. The ledger's
`counts.stale_review_fingerprints` remains `0` (no oracle or legacy source changed).

## 7. What could not be done here

* **No build, no test run.** Rule 5 forbids Gradle in a parallel wave, and the sandbox has no
  `javac`. The fixes are therefore *compile-unverified*; the API they use was read from the
  decompiled 1.21.1 sources (`RegistryAccess.lookup`, `HolderGetter.get`,
  `ItemEnchantments.getLevel(Holder<Enchantment>)`, `JsonArray.isEmpty/get`).
* **No oracle, therefore no `--check`.** See §6.
* **No client run.** The sandbox cannot create a GL context, so D2 (the client NPE) could not be
  reproduced visually; it is proved by the code shape (the only S2C receiver without the null
  check that all its siblings have).
* **The 40-file `class-inventory` refresh** is left to the leader (see §6).
