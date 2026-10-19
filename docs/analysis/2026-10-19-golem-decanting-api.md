# Decanting5 — bounded implementation agreement

Authority: [original contract](2026-10-19-golem-decanting.md),
[separate water stock](2026-10-19-golem-water-cargo.md).
This is next-unit API, not implemented transport or a gameplay/compile PASS.

## Prepared data / menu

`GolemData.LIQUID=5`, unchanged five fields/six carrier entries: slot0 EMPTY or
one pristine WATER_BUCKET, slots1…5 EMPTY, wanted0…5 all0, Fill modefalse. The
bucket exemplar is not stock; extra components/slots are refused, never stripped.
`isSupportedWaterFilter` shares this finite rule. Menu creates one ghost plus36
player slots, no count edits or button50; client draws one frame/water explanation.
Actual core5/goal/placement/recall remains unregistered/unconnected until runtime.

`GolemWaterCargo` is a separate real amount0…5000mB, validated against material
capacity. Entity saves its exact native compound separately from configuration;
normal recall transfers it to reserved CUSTOM_DATA WaterCargo on the one real
placer. Placement accepts only that sole known custom compound, verifies core5
and material, and preserves the full real item on rejected admission. Unknown
payloads remain raw/refused; empty decode is not a recovery mechanism. Death
water loss is original behavior unless a separate explicit policy is implemented;
do not fabricate bucket drops or claim normal recall fixes death automatically.

## Source and sink ownership

Only actual marked vanilla WATER source blocks (not flowing, waterlogged, fish
buckets or addon tanks), current dimension and loaded cells. Follow original
nearest/marker-order/side/reach/path/cadence and one1000mB source per10-update
work pulse; recheck room each pulse. No unmarked pond scan, forced chunks or
container-item source. A full permitted home crucible may leave bounded cargo
waiting; missing/denied/unsupported home must retain cargo and stop collection.

Root's narrow transfer helper will reuse GolemHarvestPlatform's actual native
actor and Fabric/Architectury callbacks, not Bore machine authorization. Source
removal uses the captured real old water state, fresh validation after each
BEFORE callback, guarded current worker/marker/room, and exactly one paid source.
Only confirmed removal credits1000mB; publish cargo before AFTER/neighbor feedback.
Do not restore over a replacement or credit an externally removed source. No
new source ledger/world journal/provider or saved source-position retry.

Sink is only the real loaded attached TileCrucible (home−face), capacity1000mB,
existing water-only semantics on all sides, no invented heat/Nether gate. Use
fresh native/spawn permission without break/use callbacks. Partial delivery
must preserve exact integer remainder, e.g. water650+cargo1000→water1000/cargo650.
The current `fillWater` syncs after mutation; a caller must not blindly refund
after a sync exception. If needed root adds one narrow raw cargo-debit/water-add
handoff inside TileCrucible before feedback, analogous to the existing queue's
raw handoff, not a generic transaction framework. Validate both owners before
mutation; no sync/world callback between raw debit and raw credit. Post-commit
feedback failure never refunds or repeats an accepted amount.

## Entity / client integration after Fishing

Core5 is active and has its one-slot menu, but is **not** an item-inventory core.
No Gather/Fill/Empty/Sorting/throw/fishing/Guard tasks. Keep shared return-home,
material/name/link authority and every previous core. SetFilter permits only slot0
water rule with count0, wanted/Fill edits refuse. Apply/install/load retain bad
controls rather than shrinking/copying six slots into a one-slot crash.

Transient Liquid goal owns route/intent/path; actual water is separate saved
stock. Recall must transfer it once, voluntary stripping must not orphan it.
Client may show source-backed water display/amount, never real bucket equipment
or fictitious inventory stock. No Entropy pumping, lava, tank sources, cauldrons
or universal fluid-provider support in this first crucible-refill unit.

### Agreed narrow production bridges

Entity/goal author owns EntityGolemBase +new LiquidGolemGoals. Root owns placer,
platform, TileCrucible, registry/resources and client. No overlapping writes.

```java
// Entity public, immutable stock snapshot / synced amount:
GolemWaterCargo getWaterCargo();
int getWaterAmountMb();
void applyWaterCargo(GolemWaterCargo water); // fresh unadmitted material/core5 only

// Entity public raw bridges, true ONLY inside private actual worker intent:
boolean canCollectSourceWater(BlockPos source, BlockState expected);
boolean creditCollectedSourceWater(BlockPos source, int expectedAmountMb);
boolean canTransferWaterTo(TileCrucible actualHome);
boolean detachWaterToCrucible(TileCrucible actualHome, int accepted, int expectedAmountMb);
// credit/detach validate before raw field assignment and do no callbacks/sync after it.

// Root's existing platform foundation, package-private real worker entry:
boolean GolemHarvestPlatform.collectSourceWater(EntityGolemBase, BlockPos, BlockState);
// Root's typed sink, only inside the actual guarded delivery intent:
int TileCrucible.fillWaterFromGolem(EntityGolemBase, int requestedAmountMb);
```

The private Entity collection/delivery methods enter busy +exact target guards,
call these root operations, then sync/feedback only after payment. A plain public
bridge call without the live guarded goal intent authorizes nothing. Goal does
not itself remove sources, invoke claim callbacks or debit/fill the crucible.
`canCollectSourceWater` rechecks before-removal source/marker/home/capacity;
`creditCollectedSourceWater` rechecks owner/guard/expected stock and room AFTER
the actual removal (does not require old source still present). No callback after
that raw credit. Sink bridge validates exact home BE and expected stock before
raw debit; TileCrucible raw-adds accepted water immediately, then notifies.

ApplyWaterCargo may receive zero on non-liquid fresh entities, but positive stock
requires material/core5 and its valid controls. Native entity NBT retains unknown
water payloads raw/refused. Normal recall embeds exact positive water into the
single placer before existing durable enqueue, then raw-clears water with cargo.
Positive water forbids dismantle; death's original water loss is declared, not
converted to free buckets. Root placement does not strip other custom-data keys.

Fishing's current source must be finalized and compiled first. Its build may
include these **unconnected** data/helper prerequisites; that does not make core5
playable. Each completed runtime batch needs actual Fabric compilation; native
gameplay/claim/visual/save acceptance stays separately deferred, not passed.
