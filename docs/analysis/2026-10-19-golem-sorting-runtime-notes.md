# Ordinary Sorting10 — runtime final, 2026-10-19

**Source final for root integration; not compiled or executed.** The preceding
Fill/Empty PASS does not validate Sorting or the newer directed throws. Root's
next actual Fabric build bundles these with its queue and final brain content.
No builds, Java/Python, game, tests, saves or agents were run by this author.

## Owned paths and source anchors

Only these production paths changed, under
`common/src/main/java/thaumcraft/common/entities/golems/`:

- `EntityGolemBase.java`: core10/baseline guards, Sorting-only destination lookup,
  actual extraction/deposit authorization and conditional home return.
- `InventoryGolemGoals.java`: actual Sorting worker phases and seeded routes.
- `GolemHomeInventory.java`: read-only same-face seed-plus-room predicate.

No new production file/public API was needed. No queue, Enum, Gather goals,
items/Data/Bell, menu/client/network, registry, recipes or resources were edited
or read as WIP. Root's link-edit/configure delegation, raw throw detach bridge,
action6/status4, seven materials/Flesh refusal, saved health/fire/names and
ordinary Fill/Empty/Gather behavior remain intact.

Authority: final `2026-10-19-golem-sorting.md`; oracle paths below are relative to
`../work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/`:

| Original | Runtime mapping |
| --- | --- |
| `entities/ai/inventory/AIHomeTakeSorting.java:38–70,94–138`; `entities/golems/GolemHelper.java:491–497,658–685` | Candidate identities come from actual attached-home face-visible slots, in native slot order, never ghosts or summed quotas. Re-read and extract from the first matching extractable slot. |
| `GolemHelper.java:545–578`; `lib/utils/InventoryUtils.java:204–265` | Positive insertion room AND real extractable matching seed on the same marked face. Native full components match, ignoring count; seed probing consumes nothing. Failure expires after10000 wall-clock milliseconds. |
| `entities/ai/inventory/AISortingGoto.java:49–113` | Nearest eligible marked container; ties retain traversal order. Golem-feet-to-container-center distance²≤16², five-tick decisions, native path200-update budget and stalled20-update detour. |
| `entities/ai/inventory/AISortingPlace.java:41–73,97–136` | Idle adjacency, center distance²<4; occupied merges before empty slots, actual remainder retained. Seed is checked again at each paid commit. |
| `entities/ai/inventory/AIHomeReplace.java:37–58,92–125`; `entities/golems/EntityGolemBase.java:335–350` | HomeReplace0 → SortingPlace1 → Goto3 → HomeTakeSorting4. HomeReplace requires timeout/no eligible route, not merely cargo near home. No Sorting world-drop branch. |

## Actual loop / stock ownership

- Source is `home.relative(homeFacing.getOpposite())`, using `homeFacing` for
  exposure/extraction. Enumerate visible candidates (including duplicates), then
  probe seeded destinations and the first matching extractable source slot.
  Actual take is min(real slot count, native item maximum, material carry space).
  Source identity/count/permissions and a current route are rechecked immediately
  before splitting the actual source stack into private cargo, ahead of native
  setters/dirty notifications. No preview/synced snapshot becomes stock.
- Markers are destinations only. The finite existing vanilla/Hungry inventory
  classes are unchanged; unknown/unloaded/non-container endpoints never qualify.
  `markedSortingInventory` is a package-private, Sorting-only lookup with the
  original **golem-relative** range≤16 and whole-home-storage exclusion.
  Fill/Empty retain their existing stricter additional home-range guard unchanged.
- Runtime phases preserve HomeReplace-before-Place-before-Goto-before-Take
  semantics; the existing native route is priority3 for Sorting. Home take,
  return and route decisions are five-tick gated, while idle adjacent placement
  has no independent decision gate. Existing five-update interaction holds and
  loaded-region navigation/return safeguards remain.
- At idle home distance²≤5, return only on the item's transient timeout or when
  **no** current seeded/roomy route remains. `placeCarriedAtHome` independently
  enforces this condition for Sorting, so valid just-taken stock does not get
  unconditionally put back before travel. Away from home, losing every route
  clears the route so the shared ReturnHome can walk the actual cargo back.
- Deposit rechecks same-face real seed plus room before **every** cargo debit,
  then rechecks live destination and actual stack identities/counts. Debit actual
  private cargo before native setter/dirty callbacks; keep all unpaid remainder.
  Missing seeds/full targets cause reroute or real home return, never world drops.
  A blocked/missing home retains cargo. Sorting cannot authorize the throw bridge.

Two agreed original correctness adaptations are intentional: logical double
chests supply candidate identities from **both** halves and the entire home is
excluded as a destination; same-face seed+room is required throughout, fixing
the original cross-face preflight and wrong-partner seed probes. No home-relative
16-block restriction was added to Sorting, and no Fill/Empty range was widened.

## Existing API / baseline data contract

- Root supplies `GolemData.SORTING = 10` and finite support for10 in the unchanged
  five-field record/CODEC/stream. Entity `hasSupportedCore()` now accepts10;
  coreless−1 remains inactive and blank100/other deferred cores remain refused.
- Sorting baseline is six EMPTY filters, six wanted0 values and false Fill mode.
  `configuredData()` and `applyConfiguration()` reject nonbaseline payloads before
  mutation/recall. On native load, invalid/nonbaseline controls freeze work and
  recall through the existing `unsupported`/`retainedConfiguration` path, retaining
  original raw configuration and separate cargo rather than flattening them.
- `setFilter`, `setWantedCount`, `setFillAnyAmount` do not edit Sorting controls.
  Live core10 installation requires a coreless golem with baseline controls and
  rechecks before payment; nonempty ghosts/counts or enabled mode leave the held
  core unconsumed. Valid install keeps existing boot/payment behavior.
- Normal recall preserves core10, markers, material placer/name and separate cargo;
  dismantling uses existing root `coreForType(10)`. Marker schema/API and native
  cargo persistence remain unchanged; other-dimension markers persist but are
  ignored for work. No `hasGhostFilters()` or other public API was added.
- Root owns Data/core/recipe/menu/GUI/network integration: hide/refuse Sort ghosts,
  quantities and Fill mode on the server too, using core10 directly. Existing
  core/marker APIs suffice. Acquisition remains the agreed brain-centered Sorting
  infusion, not runtime registration alone; this author did not edit that content.

Scope remains exact-component baseline sorting among finite supported containers.
Colors, upgrades, fuzzy matching, unknown providers and optional real chest-opening
animation remain deferred. No gameplay/visual result or full power-loss atomicity
is claimed; no previous build result is reused for these new sources.
