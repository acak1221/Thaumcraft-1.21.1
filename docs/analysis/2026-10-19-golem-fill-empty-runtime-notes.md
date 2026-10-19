# Ordinary Fill / Empty — common runtime notes, 2026-10-19

**Implemented, not compiled or executed by this author.** Root's preceding
seven-material Fabric build result applies to its preceding inputs, not these
new sources. No game, test, visual, survival, all-core or NeoForge acceptance is
claimed. Root owns the actual integrated compile after all authors are final.

## Owned paths and integration API

Only these production paths were changed/added:

- `common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java`
- `common/src/main/java/thaumcraft/common/entities/golems/GatherGolemGoals.java`
- `common/src/main/java/thaumcraft/common/entities/golems/GolemHomeInventory.java`
- `common/src/main/java/thaumcraft/common/entities/golems/InventoryGolemGoals.java` (new)

No item/data/marker/menu/client/registry/network/resource, shared Bore/drop,
world, test, Python or smoke files were edited. Other authors' WIP was not read;
no agents were spawned and no commands/builds/tests were executed. Root's
`thaumcraft.common.container.ContainerGolem` import is retained. The material
table/guard, native attributes/fire/name/health-load handling, physical Gather
pickup, player permission checks, death and durable recall ownership remain.

Existing public entity APIs are preserved. Additions:

```java
public boolean hasSupportedCore(); // ACTIVE Fill0 / Empty1 / Gather2, not coreless
public int getWantedCount(int slot);
public void setWantedCount(int slot, int wanted);
public boolean isFillAnyAmount();
public void setFillAnyAmount(boolean any);
public List<GolemMarker> getMarkers();
public void setMarkers(List<GolemMarker> markers);
```

- Root's menu should combine `hasSupportedCore()` with existing `canConfigure`
  rather than accept only Gather. Menu start/stop pausing applies to all three
  active cores and remains local to this golem's actual opener set.
- Wanted controls are six logical integers, not extra container slots. Clearing
  a row sets wanted0; a populated exemplar defaults to1, with maximum256. Setting
  wanted<=0 clears that ghost; positive wanted cannot invent an empty exemplar.
  `getFilter` remains a one-count native component-preserving snapshot.
- Marker setters/getters defensively copy an immutable maximum64 record list.
  Other dimensions are retained, ignored for work, and never loaded. Bell code
  remains responsible for authoritative player/selected-entity validation.
- `configuredData()` / `applyConfiguration(GolemData)` use the agreed five-field
  record `(core, filters, wantedCounts, fillAnyAmount, markers)`. Material remains
  placer item identity; actual cargo/home/owner/health do not enter configuration.
- Core attachment obtains the ID from existing **instance**
  `ItemGolemCore.getCore()`, accepts only0/1/2 on a coreless entity, and preserves
  the existing payment/creative/boot33/event7/upgrade-sound behavior. Blank100
  and other cores do not install. Dismantle uses root's
  `ThaumcraftGolems.coreForType(int)` exact Item/null lookup, retaining the entity
  and cargo if no exact supported core item is available.
- Normal recall round-trips all five settings and the native custom name;
  mode preservation is the agreed improvement over original recall omitting
  toggles. Cargo is still separately enqueued before clearing/discard.

## Gameplay authority / concrete implementation

Read final agreements:

- `docs/analysis/2026-10-19-golem-fill-empty.md`
- `docs/analysis/2026-10-19-golem-inventory-api.md`

Read-only oracle paths below are under
`../work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/`:

| Original anchor | Native behavior |
| --- | --- |
| `entities/ai/inventory/AIFillGoto.java:52-115` | Empty cargo, populated filter required; first missing exemplar with supply, then nearest eligible marked source. |
| `entities/golems/GolemHelper.java:166-223` | Precise Fill counts matching home-face-visible stock, regardless of extraction permission. Any mode bypasses counting but still requires an exemplar. |
| `entities/ai/inventory/AIFillTake.java:38-74` | After navigation ends, re-read adjacent marked sources/faces, not an exclusive reservation; extract min(deficit/carry space), or carry space in Any mode. |
| `entities/ai/inventory/AIHomeTake.java:37-105` and `GolemHelper.java:448-479,581-655` | Empty checks destination room before home withdrawal. Populated filters are whitelist identities, not retained home stock targets; empty filters accept any extractable item. |
| `entities/ai/inventory/AIEmptyGoto.java:49-80` and `AIEmptyPlace.java:41-76,100-139` | Walk to nearest eligible marked destination; on idle adjacency insert through marked faces, retaining residual cargo. |
| `entities/ai/inventory/AIHomeReplace.java:37-69,92-125` | Empty returns cargo to home when it has no eligible destination or its item is timed out. A blocked home retains actual cargo. |
| `entities/golems/GolemHelper.java:515-578` | Failed item identity/component timeout is transient **10000 wall-clock milliseconds**, using `System.currentTimeMillis()`, not permanent or game-tick-based. |
| `lib/utils/InventoryUtils.java:208-266,313-340` | Extract only from the first matching exposed/extractable slot; strict item/components ignoring count. Actual transfer is not assembled across slots. |
| `entities/ai/inventory/AIFillGoto.java:120-144`, `AIEmptyGoto.java:95-119` | Native walking with200-update pursuit budget and20-update stalled local detour; no teleport to targets. |

`InventoryGolemGoals` holds only transient watched ghost identity, marker route,
path budget, five-update interaction hold and expiring failed-item identities.
There is no new stock owner, recovery queue, provider registry or global tracker.
Shared creeper avoidance, swim/doors, return-home and look/idle goals remain;
the physical ItemEntity pickup goal is installed **only** for Gather.

Decision cadence is five server ticks for Goto selection, home take/place and
home replacement. Marker take/place can act after navigation ends without an
extra five-tick gate. Actual progress starts a five-update hold, without fake
player/container viewers. Pending route selection is distinguished from an
already-started native path, so idle navigation before goal start does not cancel
the selection. Return-home waits while an inventory route/interaction hold exists.
An idle-arrival handoff also lets adjacent take/place act before the lower-priority
return goal starts a new path; it is transient scheduling state, not a reservation.

Coordinates and thresholds remain explicit:

- Attached inventory `B = home.relative(homeFacing.getOpposite())`; insertion and
  extraction use `homeFacing`, not its opposite or a guessed default side.
- Markers identify actual block/dimension/face. No markers means no remote
  targets, not a neighbor scan or owner-follow behavior. Bell does not rehome.
- Golem-to-marker center distance²<=256, home-to-marker block distance²<256;
  marker interactions require idle navigation and distance²<4.
- Home interactions require idle navigation and home-center distance²<=5;
  original shared return starts at distance²>=3. Existing loaded-region path
  guards and safe far-home/solid-block relocation remain unchanged.

Fill sums **all duplicate matching rows' wanted counts first**, subtracts visible
home stock once, then clamps the deficit at zero. Duplicate identities are not
fetched as separate row demands. This is the explicitly agreed fix for the
original summed-target/individual-row negative-deficit defect, not a claim that
the original maintained summed targets correctly. Six logical quantities replace
the original overlapping twelve Fill menu entries; this creates no real stock.

## Native inventories and actual stock ownership

- The finite whitelist remains vanilla chest/trapped chest, barrel, hopper,
  furnace/blast furnace/smoker and Hungry Chest. Native double-chest resolution
  and its connected-chunk checks are reused. The home double-chest partner is
  treated as the same storage owner, not a remote source/destination.
- Every current endpoint is re-resolved on action; native BE/state identity,
  same level, actual slot object/count, strict native components and sided
  extraction/insertion permissions are checked before payment. Fill stock counts
  use face exposure, not extractability. Unknown/custom providers are not probed
  through a broad Container adapter.
- Extraction splits the actual first matching source slot into actual private
  cargo **before** any known native setter/dirty notification. No copied preview
  becomes an output and no source `removeItem` callback runs before cargo owns
  the paid units. Request, actual stock, material space and native item maximum
  bound the transfer; excess real stock remains in its actual source slot.
- Deposits merge occupied matching slots before empties, then debit actual cargo
  before native setter/dirty notifications. Each accepted piece has one actual
  owner; remaining cargo is not cleared or replaced by a stock snapshot after
  callbacks. Local busy guards cover both directions and recall.
- Empty's room check is **some room**, not a full-batch reservation. It can take
  more than a destination ultimately accepts, then route/return the real residual.
  Whitelisted Empty selection follows filter-row identity order, with the first
  matching extractable source slot for that identity; unfiltered selection walks
  source exposure order. It does not withdraw quantities across multiple slots.
- These are bounded known-native operations, **not** arbitrary addon-callback or
  full power-loss atomicity proofs. Existing death/recall use only the existing
  durable `DeferredItemDrops` path; no shared drop/Bore code was changed.

## Persistence and deliberate gaps

Entity NBT adds `WantedCounts` (six native integers), `FillAny` (boolean), and
bounded native `GolemMarker.CODEC` data under `Markers`, alongside existing
one-count `Inventory` ghosts and separate complete-component `ItemCarried`.
Old native Gather saves with absent new fields default to wanted0/1 according
to empty/populated ghosts, false mode and no markers. Counts256 never become
oversized ItemStacks. Native synced fields expose wanted counts and mode; marker
work state remains server-owned and bell selection owns client outline snapshots.

Malformed types/counts/duplicate ghost rows, unsupported core/material/decor/
upgrade state, or marker data that fails bounded canonical native codec decoding
freeze work and refuse configured recall. Their original configuration tags and
real cargo remain retained; partial/future marker settings are not silently
normalized into an authorized configuration. `canConfigure` also refuses those
retained-invalid states, so bell/menu callers cannot replace them with a partial
decoded marker snapshot. Other-dimension valid markers are
not malformed and are not stripped.

**World throws are intentionally NOT implemented.** Empty skips loaded
non-container markers before taking stock. Fill requires a known supported home
inventory before a new fetch, including Any mode; it does not reserve room there.
Original `AIHomeDrop` (Fill missing/non-inventory home throw) and `AIEmptyDrop`
(deliberate non-inventory marker throw, including its original Goto range quirk)
remain concrete missing branches. Full/missing/unloaded/unknown destinations
never become implicit drops: existing cargo stays carried or is actually returned
to an accepting home. Optional real chest-opening animation remains deferred;
no fabricated player viewer or hopper disturbance was added.

Root still owns data/item/bell/client/menu/network/resources integration and the
actual final Fabric compile. Runtime route behavior, save/recall round-trips,
sided extraction, partial deposits, timeout expiry and visuals are unverified.
No builds, game runs, tests, source-text stubs or proof/QA infrastructure ran.

## Hash handoff

Under this task's **no execution** instruction, no checksum command was run.
Final SHA-256 values for the four production paths above are intentionally left
for root's actual integrated source/build capture; no earlier material hashes
are presented as hashes of this new Fill/Empty implementation.
