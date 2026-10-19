# Fill/Empty implementation API — 19.10.2026

Source authority: [original contracts](2026-10-19-golem-fill-empty.md).
This is a content implementation agreement, not a test or acceptance receipt.

## Bounded working scope

Implement Fill0 / Empty1 for the existing finite vanilla/Hungry containers,
alongside Gather2 and coreless−1. Seven materials stay unchanged. No new provider
framework, journals, forced loading or world fixtures. Unknown/unloaded endpoints
never authorize taking, depositing, or dropping items. **Intentional world-throw
branches remain deferred in this first container-routing slice** and must be
disclosed, not replaced by dumping cargo. Full/missing targets retain cargo.

Fill sums duplicate matching filter rows' wanted quantities, then clamps deficit
at zero. This explicitly fixes the original summed-target/individual-row negative
deficit bug. Six logical quantity controls replace the overlapping twelve Fill
ghost menu entries. Neither change manufactures real stacks or doubles storage.

## Shared data API

`common.items.golems.GolemData` becomes:

```java
record GolemData(int core, List<ItemStack> filters, List<Integer> wantedCounts,
                 boolean fillAnyAmount, List<GolemMarker> markers)
```

- Keep `GolemData(int core, List<ItemStack> filters)` for old callers: default
  wanted0 for EMPTY /1 otherwise, false mode, empty markers.
- Constants NO_CORE−1, FILL0, EMPTY_CORE1, GATHER2; `isSupportedCore(int)`.
- Six native full-component **one-count** exemplars remain separate from six
  integers: EMPTY→0, populated→1…256. Counts are logical wanted stock, not cargo.
- `CODEC` accepts the old two-field representation with defaults. New fields
  are bounded and validated; unknown/future fields still fail. Native stream
  transports all five fields. Defensive copies and semantic stack equality/hash.
- `GolemMarker(ResourceLocation dimension, BlockPos pos, Direction face)` in the
  same items.golems package, immutable position, native CODEC, maximum64 markers.
  No color/fuzzy/upgrade data; default original color−1 is implicit.

## Entity/runtime API

Keep existing `configuredData()/applyConfiguration`, material/home/cargo/filter
APIs; add `getWantedCount(int)`, `setWantedCount(int,int)`, `isFillAnyAmount()`,
`setFillAnyAmount(boolean)`, `getMarkers()`, `setMarkers(List<GolemMarker>)`.
These are server-owned configuration, persisted and included in normal recall;
real cargo remains separate. Old Gather saves/placers default correctly. Keeping
mode through recall is a disclosed native improvement (old recall omitted toggles).

Fill requires a populated filter and fetches marked supply to attached home,
honoring precise/any mode. Empty preflights marked destination room before home
extraction; keeps/returns leftover cargo to home if no route works. Use original
5-tick decision cadence, distances, native navigation and transient10s item-failure
timeouts; no permanent blacklist. World-throw markers are skipped before withdrawal.
Native source/destination identity, sided permissions, merge-before-empty and
existing local busy/cargo guards apply. No synthetic stock or copied cargo.

## Item/bell API

Core items allow100/0/1/2; only0/1/2 fit a coreless golem. Registry adds CORE_FILL,
CORE_EMPTY and `coreForType(int)` returning the exact item or null. Dismantling
recall returns the fitted core. Normal recall round-trips full configuration.

Bell selection snapshots actual entity markers. Marker toggle changes the actual
selected loaded UUID/dimension entity and bell cache without rehoming. Preserve
ordinary player/world/hand checks, no owner-only lock. Add authoritative
`clearMarkers(ItemStack, Player)` for root's actual item-key packet and client-only
read accessor `selectedMarkers(ItemStack)` for outlines. Retain invalid data,
never silently reset it or force chunks loaded. Existing playerless clear helper
must not pretend to clear a worker while editing only a stale cache.

## Menu/client API (root owns menu/network/bootstrap)

ContainerGolem retains6 ghosts +36 real inventory slots; adds8 data fields:
wanted0…5, any-mode6, core7. Public `getWantedCount(int)`, `isFillAnyAmount()`,
`core()`. Server click button **50** toggles precise/any only for Fill. Quantity
ghost clicks adjust logical counts without modifying cursor/player stock.
Gather/Empty remain one-count whitelist controls. Shift/pick-all/drag cannot turn
ghosts into real items.

Client draws quantities, correct translated core caption/badge, Fill toggle and
selected-bell marker outlines. Root extends the existing shared F item-action
input (no duplicate key binding) and registers a hand-only bell-reset C2S packet.
Native packet/menu validation remains server-owned. Gameplay, visuals and marker
input remain unverified until the user's deferred final pass.
