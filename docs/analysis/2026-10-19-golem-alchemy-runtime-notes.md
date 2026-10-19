# Alchemy6 runtime — FINAL owned-source handoff, 2026-10-19

**Implemented, NOT compiled or game-tested.** Root builds actual Fabric after this
and the transfer/other owners' FINALS. Decanting's supplied PASS36s is earlier
evidence, not Alchemy validation. No agents, Gradle/Java, tests, game, Python or
worlds/saves used. Transfer/cargo, BE and other owners' WIP were not read or edited.

Owned files: `common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java`,
new same-package `AlchemyGolemGoals.java`, this note only. Authority: FINAL
[Alchemy contract](2026-10-19-golem-alchemy.md), [updated API](2026-10-19-golem-alchemy-api.md),
original oracle `common/entities/ai/fluid/AIEssentia{Gather,Goto,Empty}.java` (G/T/D)
and `common/entities/golems/GolemHelper.java` (H).

## Real source / path / jar worker

- Empty1/Gather2/Goto3, MOVE/LOOK, every-active-tick updates; shared ReturnHome6
  yields to an actual Alchemy path/queued target or active one-shot, not indefinitely
  to positive stock. No menu, ghosts, item routing/pickup/deposit/throw/combat/fishing.
- G:43–123: idle actor-to-**attached base P center** distance²≤6, P=home−face.
  Direct native TileAlembic checks current `canOutputTo(homeFacing)` and considers
  **only itself**; actual extraction is UP. Only exact native TileAlchemyFurnace
  enables a loaded contiguous actual-alembic stack1…5; stop gaps/unknowns. Choose
  largest compatible positive amount, highest-Y ties. No arbitrary BE scan, filter/
  crucible/catalog supplies or suction-difference test. Source type/amount/facing
  and whole selected binding are re-read at paid preflight, not inferred from a copy.
- One bulk attempt up to fresh source amount / remaining **item carry C**, not
  Decanting capacity or one-unit/10-update work. No destination prerequisite to take;
  compatible positive cargo may fill again while idle/waiting. G:61,73,122's wall-
  clock delays map explicitly to **2 server ticks after a qualifying take attempt
  (including0), 20 after early failure**; not exact wall-clock parity. Failed take
  never reproduces G:119–120's broken clearing of an already owned aspect identity.
- H:225–314: directly marked, loaded **normal TileJar only**, foreign dimension
  skipped before reads. Jar-center distance²≤16² to **integer home**, not actor or
  home center. Fresh label/type/room, stages: labelled matching partial → labelled
  empty → unlabelled matching partial → empty unlabelled. Least home-distance in
  first nonempty stage; strict ties keep marker order (deterministic HashMap adaptation).
  Native jar direct-add semantics do not invent a marked-side top-port restriction.
- T:44–81: actual-server-tick5 route decisions, integer jar XYZ path, budget200
  active updates; one local random(2,1)/20-update unstick if still at original floor.
  Reuse loaded navigation-area guard; no approach-grid/Y+1, teleport or forced chunks.
  D:42–79: idle deposit freshly selects again and needs feet-to-jar-center²≤4.
  Full/missing/replaced/denied/label-mismatched endpoints retain exact partial stock.

## Exact production boundary / root dependencies

- Private guarded worker entries call agreed `EssentiaTransfers.collectAlembic`
  `(this,actualSource,Direction.UP,aspect,limit)` / `deliverNormalJar`
  `(this,actualSink,aspect,limit)`, inspect `Attempt.committedUnits()/failure()` and
  never refund/repeat committed units. No ordinary take/add followed by an item
  setter, fake BE endpoint, new provider, callback-plan supplier, token or journal.
- Root must enable6 in `GolemHarvestPlatform.mayInteract`. Selection/payment uses
  nonbreaking native/profile/spawn preflight on actual positions; after callbacks,
  recheck native ID+UUID owner, level, home binding and current endpoints. Collection
  queries base/source; delivery queries home-anchor/sink but does not require a
  surviving source machine. No break/use events, OP or online-player borrowing.
- Public `canCollectEssentia(TileAlembic,Aspect,int)` / `canDeliverEssentia`
  `(TileJar,Aspect,int)` authorize only private live Goal attempt + busy/exact
  source/sink/home/type/stock/permission/limit guards. Both repeat pure current
  checks before commit; standalone public executor calls grant nothing.
- `applyAspectCargoRaw(before,after)` FIRST calls
  `EssentiaTransfers.requireGolemCargoChange(this,before,after)`, then validates
  actual before-stock, material capacity, private intent, aspect/direction/limit.
  One immutable owner replacement is last work; no callback/sync follows it.
  `notifyGolemEssentiaChanged()` requires the active executor scope and syncs only
  after both raw owners are paid. Executor notification failures retain actual stock;
  the outer scope can repair metadata and emit post-payment swim feedback.
- `isGolemMutationActive(this)` and busy refuse splitting edits/read/recall. Native
  removal requests during the held scope are remembered; finally clears guards,
  finishes notifications/feedback, then completes **ONE** deferred removal, never
  permanently swallowing it. Root's existing deferredWaterRemoval remains unchanged.

## Stock / placement / recall

- Actual immutable `GolemAspectCargo`, separate raw unread tag, native STRING tag /
  INT amount synchronization. Public `getAspectCargo/getEssentiaAspect/getEssentiaAmount`
  expose actual values, never a minted/display jar. `applyAspectCargo` is fresh,
  unadmitted, material-validated; positive requires6/baseline controls and no existing
  item/water/fish/essentia stock. Root owns known-key/core/material placer dispatch.
- Separate entity `AspectCargo` uses strict helper save/parse/validateMaterial;
  unknown version/tag/type/amount/over-capacity/non-Alchemy positive stays raw,
  unsupported/frozen against work/flattening/unsafe destruction. Existing stock
  cannot be overwritten by reload. Route/reset never clears actual cargo.
- Core6 requires EMPTY6/wanted0/modefalse, no schema5 expansion; install refuses
  nonbaseline controls before item pay. Normal recall writes positive AspectCargo
  onto the one configured real placer, preserving material/name/core/markers, then
  raw-clears beside item/water after durable enqueue and before removal callbacks.
  Positive essentia refuses dismantle; foreign-gear refusal also covers6.
- **Known essentia loss on death/forced removal remains original policy**, not fixed
  by recall. No phials/crystals/filled jars are fabricated. All existing water death
  and item/fishing pending conservation policies remain; unknown raw stock is retained.

Root still owns Data/core/recipe/book, typed potion predicate, placer, client and
platform integration. Client may construct only a readonly display jar with
floor(64*amount/C), never actual64 stock. Placement-in-progress canWork/canEditLinks
guards, deferred water removal, seven materials/health/regen/attributes, safe-home,
remote links, Guard and Fishing RNG/lifecycle/raw world-throw authority are preserved.
Deferred: void/connected jars, tubes/reservoirs/addon networks/upgrades, exact wall
clock, universal claim coverage and gameplay/visual/save acceptance. No new PASS.
