# Decanting5 runtime — FINAL owned-source handoff, 2026-10-19

**Implemented, NOT compiled/tested/gameplay-accepted.** Root builds Fabric after
all FINALS. Fishing's reported PASS41s predates these edits; its world-RNG count
fix and root's rejected-spawn Entity overloads are untouched. No agents, Gradle/
Java, tests, game, Python, saves or worlds used.

Owned: `common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java`,
new same-package `LiquidGolemGoals.java`, this note only. No other production edits.
Authority: FINAL [contract](2026-10-19-golem-decanting.md),
[cargo](2026-10-19-golem-water-cargo.md), [API](2026-10-19-golem-decanting-api.md);
original oracle `common/entities/ai/fluid/AILiquid{Gather,Goto,Empty}.java` (G/T/D)
and `common/entities/golems/GolemHelper.java` (H).

## Worker/source anchors

- Empty1/Gather2/Goto3, MOVE/LOOK, shared idle/ReturnHome. Actual-server-tick5
  decisions latch admission; active counters require every tick. Liquid intent is
  included in `hasInventoryRoute()`. No item collection/deposit/throw/combat/fishing.
- H:371–445: nearest **marked actual source WATER**, actor-to-block-center²≤16²,
  strict-nearer marker-order ties, no home radius. Reject foreign dimensions before
  reads; no flowing, bubble-column, waterlogged, BE/tank/lava or unmarked sources.
- T:49–99: closer normal cubes in3×3 X/Z **same Y**, integer native path input,
  speed modifier1; budget200 plus one local random(2,1)/20-update unstick if still
  at original floored position. Loaded navigation guard, no teleport/forced chunks.
  Normal cube maps conservatively to solid render + full collision shape.
- G:63–219/H:78–85: idle navigation, watched intent, marker-list nearby order,
  actor-feet-to-source-center²<4. Wait10 active updates; failures can continue until
  count20. Success resets timer and returns: **one source/1000mB per10 updates**,
  fresh room check, position dedup across faces. Original overflow bug is not copied.
- Missing/unloaded/denied home stops collection, retains stock. Full **known,
  permitted** crucible can leave bounded buffer waiting. No room/source yields to
  ReturnHome. D:43–82: idle delivery at home-center²≤5, only attached current
  `TileCrucible` at home−face, capacity1000, all sides, no heat/Nether gate/dumping.

## Root bridges required before compile

- Worker calls actual `GolemHarvestPlatform.collectSourceWater(golem,pos,expected)`
  and `TileCrucible.fillWaterFromGolem(golem,requested)`. Root enables core5 in
  `mayInteract` for nonbreaking native home/source preflight. Root owns captured-old-
  state BEFORE/AFTER removal callbacks and one-source payment. No direct removal,
  refund, fake-player query or generic fluid provider is implemented here.
- Public `canCollectSourceWater(source,expected)` requires private Gather intent,
  busy/exact source/state/home/face, native ID+UUID owner, permission preflight,
  current marker/source, idle reach and ≥1000mB room. Public post-removal
  `creditCollectedSourceWater(source,expectedAmountMb)` checks active owner/intent,
  unchanged expected stock/material room, **not old source/home presence**.
  **expectedAmountMb means stock BEFORE payment (first source passes0), not1000.**
  Raw adds1000 then immediately returns; no fallible work/callback/sync afterward.
- `canTransferWaterTo(actualHome)` requires exact current attached BE/private Empty
  intent. `detachWaterToCrucible(home,accepted,expectedAmountMb)` validates expected-
  before stock and sink room, raw subtracts, immediately returns. Root **raw-adds
  to that crucible next**, before notifications. Home650/cargo1000→accept350/cargo650.
- Worker entries hold busy/exact guards, clear guards in finally, then sync amount/
  action/swim feedback only after payment. Exceptions never refund/repeat accepted
  stock. `canWork()` still allows busy internally; raw bridges require their private
  guards. Home/owner/marker/filter/mode/material/core edits refuse during payment.

## Stock/UI/recall and gaps

- Actual private `waterMb`, not item cargo/configuration. Helper material capacities
  for0/1/2/3/5/6/7:1000/2000/2000/2000/4000/5000/5000. `getWaterCargo()` immutable;
  `getWaterAmountMb()` actual server stock / synced client INT. `applyWaterCargo`
  only fresh/unadmitted/same-thread; positive requires valid material/core5/controls,
  no existing stock; zero on a valid fresh non-liquid entity is a no-op.
- Root placer must strictly read payload, select material/configuration, apply water,
  then perform actual admission/payment; rejected placement retains complete item.
  Root owns this plus core registration/assets and menu/client integration.
- Active/menu-enabled5 is **not inventoryCore**. Slot0 EMPTY/pristine WATER_BUCKET,
  slots1…5 EMPTY, wanted0/modefalse; empty permits water only in this bounded slice.
  Filter edit never assigns wanted1; wanted/Fill setters refuse. Install demands
  entirely empty baseline before paying. Invalid controls stay raw/unsupported.
- Separate entity `WaterCargo` saves all amounts including zero, strict parse and
  material validation. Invalid/future/non-liquid-positive data remains raw, frozen
  against work/flattening/unsafe edits/destruction. Reload cannot overwrite stock;
  resets never clear water. No new journal/token or five-field schema expansion.
- Normal recall embeds exact water in the one placer's reserved CUSTOM_DATA before
  durable enqueue; after acceptance raw-clear water beside item cargo before removal.
  Positive water **refuses dismantle**; foreign-gear refusal also covers Decanting.
- **Known water still dies with the golem, as original.** No free bucket/placer death
  drops. Unread/protected water refuses destructive loss. Item/fishing conservation,
  materials/health/regen/attributes, safe-home, link delegation and Guard stay intact.

Deferred: other fluids/tanks/sinks/cauldrons/upgrades, universal addon claim coverage,
gameplay/protection/visual/save acceptance. Actual Fabric compile still awaits root's
external bridges/placer/core integration and all FINALS; no new PASS claimed.
