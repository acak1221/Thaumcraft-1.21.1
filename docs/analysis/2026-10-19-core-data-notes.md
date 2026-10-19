# Fill/Empty item data and Bell handoff — 2026-10-19

Frozen for root integration; **actual Fabric compilation pending**. Changed only `GolemData.java`, `ItemGolemCore.java`, `ItemGolemBell.java`; added `GolemMarker.java` in `common/src/main/java/thaumcraft/common/items/golems/` and this note. No Placer/material, entity/AI, helper, menu/client/network, registry/resource or other existing-document edits. Other owners' WIP was not read; no Java/Gradle/game/tests/Python/smoke/world work ran. The earlier seven-material build is not evidence for these inputs.

Authority: [final inventory API](2026-10-19-golem-inventory-api.md), superseding proposed signatures in [original Fill/Empty contract](2026-10-19-golem-fill-empty.md).

## Public data API / storage

```java
GolemData(int core, List<ItemStack> filters, List<Integer> wantedCounts,
          boolean fillAnyAmount, List<GolemMarker> markers)
GolemData(int core, List<ItemStack> filters) // retained: counts0/1, false, no markers
GolemMarker(ResourceLocation dimension, BlockPos pos, Direction face)
```

- `NO_CORE=-1`, `FILL=0`, `EMPTY_CORE=1`, `GATHER=2`; `isSupportedCore(int)` accepts exactly those four, not blank100. Bounds: `FILTER_SLOTS=6`, `MAX_WANTED=256`, `MAX_MARKERS=64` (modern bound, not an original limit).
- Six full-component exemplars must be EMPTY or count1. Each separate wanted integer is0 for EMPTY, otherwise1…256. All constructor inputs are copied; mutable stacks are also copied on `filters()`. Counts/markers are immutable lists, marker positions immutable. Equality/hash include semantic native stacks, quantities, mode and ordered markers. Duplicate **filter rows remain intact**; duplicate markers are rejected, not silently removed.
- Persistent `CODEC`: required `core` and `filters`; optional `wanted_counts` (absent→0/1 per filter), `fill_any_amount` (false), `markers` (empty). Only these five keys are accepted. Supplied malformed fields, unknown keys, unsupported cores, fractional/overflowing core/count numbers, invalid row/count relationships and partial codec results fail; no malformed-to-EMPTY fallback.
- `GolemMarker.CODEC` uses native ResourceLocation / BlockPos / Direction codecs with exactly `dimension`, `pos`, `face`; coordinate integers are checked before native decoding can truncate them. No color/upgrade fields. Other-dimension marker rows are preserved, not pruned by data or Bell; runtime must skip them for work.
- `STREAM_CODEC` always carries all five fields: core VarInt; six native optional stacks; six count VarInts; boolean mode; bounded marker-count VarInt; each marker's native ResourceLocation, three coordinate VarInts, face byte0…5. Three integers avoid packed-position truncation. The stream requires the matching new build; only the **persistent old two-field data** gets defaults.
- Material remains native placer item identity, name remains CUSTOM_NAME, and **real cargo never enters GolemData**. No legacy world/NBT migration is promised.

## Core / Bell API

- `ItemGolemCore(Properties,int)` now accepts100/0/1/2; unchanged `getCore()`. Tooltip keys: `tooltip.thaumcraft.golem.core_blank`, `core_fill`, `core_empty`, `core_gather`. No attach/use/payment/shrink callback was added; the actual entity owns those actions.
- Existing `ItemGolemBell.select(stack,player,golem)` snapshots **actual entity markers**, never reuses the old Bell cache as the worker's state. `useOn` re-reads actual markers before toggling; it mutates that entity and the Bell cache **before feedback**, without changing home, filters, quantities, mode or cargo.
- New **`public static List<GolemMarker> selectedMarkers(ItemStack)`** returns a read-only cached snapshot for client outlines. Invalid data returns an empty view **without rewriting anything**; no entity lookup/loading occurs.
- New **`public static boolean clearMarkers(ItemStack, Player)`** is root's server item-key entry point. It requires a real held Bell, server thread, selected loaded UUID/current dimension and the existing world/entity/`canConfigure` checks. It clears the actual entity and synchronizes the cache, including when the cache was stale. Missing/unloaded/invalid subjects are refused, never loaded to satisfy reset.
- Old **`clearMarkers(ItemStack)` is deprecated and always returns false**, without a cache-only edit. Root must use the player-context overload; no packet, key binding or sneak-air input was added here.
- Bell CUSTOM_DATA remains `thaumcraft:golem_bell`, canonical `Golem/Dimension/Home/Face/Markers`; marker conversion helpers use shared GolemMarker while retaining `Pos/Dimension/Side/Color` and accepting only color−1. Unknown/future/colored/malformed selections are retained and refused. Other CUSTOM_DATA keys/components survive edits. Packed legacy Bell positions must round-trip exactly or selection/edit is refused rather than truncated.

## Owner integration / explicit remaining scope

- Bell directly depends on agreed entity `getMarkers()` / `setMarkers(List<GolemMarker>)` plus existing home/face/`canConfigure` methods. Runtime owns the complete `configuredData()` / `applyConfiguration`, wanted-count and mode accessors, entity persistence/normal recall and actual worker loops. Use the **five-field** configuration when editing/recalling existing golems; the two-argument constructor deliberately supplies defaults, not a partial-update operation.
- Root owns registration/recipes/language, six-ghost quantity menu fields and hand-only F-reset C2S dispatch to `clearMarkers(held,player)`. Client owner uses shared record accessors for outlines. Keep tooltip key `tooltip.thaumcraft.golem.bell_markers_local`, but replace its old local-only wording with actual container-marker routing and the world-throw limitation.
- This slice does not implement workers, duplicate-row deficit aggregation, networking or GUI. The agreed runtime fix sums duplicate wanted rows/clamps deficit; data does not merge or discard those rows. Unknown/unloaded endpoints never authorize stock movement. **Intentional world throws remain deferred** for first vanilla/Hungry container routing; no fallback dump was added. Upgrades/order colors/fuzzy/advanced cores remain unsupported.
- Mode round-trip through normal recall is the agreed explicit improvement over original recall, which omitted toggles; the data field exists here, while runtime owns using it. No codec round-trip, gameplay, visual or full-core acceptance was run/claimed.

Original private-oracle anchors (`../work/thaumcraft-4.2.3.5-oracle/thaumcraft/`): `common/entities/InventoryMob.java:236–276` quantities; `common/entities/golems/ContainerGolem.java:46–58,114–130` double-ghost/unsafe quick-move; `Marker.java:14–35` identity; `ItemGolemBell.java:103–128,131–189,209–241,313–334` marker select/edit/reset/recall; `EntityGolemBase.java:581–623,626–703` persistence; `common/config/ConfigRecipes.java:173–174` Fill/Empty costs and `ConfigResearch.java:330–331` research. Full behavior/deferred branches are in the source contract, not invented in these item classes.

## Current source SHA-256

| File | SHA-256 |
| --- | --- |
| `GolemData.java` | `4ba3daef0e6a13d677b8703ed40d9ccd74c963f1355d0973922fb4d2f65a225a` |
| `ItemGolemCore.java` | `9036c240cd7a3bda996d02616dcb3aaed39f957d820f6dd90303ea6e1e2f7877` |
| `ItemGolemBell.java` | `b450ead04dfcda1db68369950eb4be5aa0f3112cb9a3e927af3dfeb466ecacc8` |
| `GolemMarker.java` | `b8ce5a51aa38867f696f2e79e85253f965b7cb1f34d29fa062ccc70d59dcb743` |
