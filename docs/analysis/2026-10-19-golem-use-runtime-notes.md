# Use8 runtime — FINAL owned-source handoff, 2026-10-19

**Implemented owned sources; NOT compiled/game-tested.** The supplied Butcher
PASS38s / 11 existing data JUnit results predate Use and do not validate it.
Only `EntityGolemBase.java`, new same-package `UseGolemGoals.java` and this note
were edited. No extra helper, agents, tests, Gradle/Java, game, Python, saves/worlds
or native actions were run. Actor/reviewer WIP was not read; actor invocation is
left entirely to root's separately owned frozen platform API.

Authority: FINAL [Use contract](2026-10-19-golem-use.md),
[data prerequisite](2026-10-19-golem-use-data.md),
[updated frozen API](2026-10-19-golem-use-api.md). Original oracle reads:
`AIUseItem.java:62–202`, `AIHomeTake.java:37–125`,
`AIHomeReplace.java:37–125`, `GolemHelper.java:448–512,568–655`.

## Bounded worker / source behavior

- Core8 is active and has a menu, but is deliberately **not** an `inventoryCore`:
  no generic Fill/Empty/Sorting, Gather pickup, combat, throw, Fishing, water or
  essentia work. It installs **HomeReplace0 / Use0 / HomeTake4**, MOVE+LOOK,
  every-tick updates; existing shared doors/avoidance/ReturnHome6 remain. ReturnHome
  yields to the actual Use path or paid transfer delay, not indefinitely to cargo.
- DEFAULT only: block/right/not-sneaking. **Known loaded non-BE/non-container home
  + actually empty cargo + six empty ghosts** permits exact native LeverBlock/
  ButtonBlock activation. An empty supported inventory is NOT bare-hand permission;
  unknown BE/provider homes also refuse it. Seeds require a supported finite home.
- Home is `P=home−homeFacing`, same native side/double-chest adapter. Select pristine
  actual WHEAT_SEEDS in exposed slot order, matching six full-component whitelist
  ghosts (empty0/populated1, no quota/Fill). Unsupported home items stay untouched.
  Preflight a real applicable marker and native home/target/adjacent permissions
  **before withdrawal**; reuse Entity's actual split/cargo-assignment loop, up to
  material carry and real stack maximum. Idle/home-center distance²≤5, age%5==0;
  successful take/partial return waits five active updates, including across pauses.
  Chest open/close animation is not added.
- First applicable stored marker, default uncolored: M non-air, M+face air.
  Seeds only FARMLAND/UP; empty hand only native lever/button, no BE/GUI action.
  Dimension rejection precedes marker reads; loaded/border/height and **integer
  M-to-home distance²≤16²** are explicit safety narrowing. Original unused distance6
  did NOT enforce a range. No unmarked search, forced chunks or Use teleport.
- Native path to M+(.5,.5,.5), speed1, budget200, cached M AND adjacent state,
  selected face/home binding. At click: fresh unobstructed eye ray and feet-to-M-
  center distance²≤4. A thin button/lever may miss the center ray, but any earlier
  solid denies it. No alternate-face substitution; this corrects original's later
  numeric-first-side relookup. Native click hit point/face belong to root's actor.
- Initial nextInt(6) admission; later `age+3*max(1,5-age)` is retained (normally3),
  **entity age, not Air upgrades**. One attempt always stops/reset paths, regardless
  of native result. Unusable real-item timeout is original wall-clock10000ms with
  full component identity. Return through the same native home accepts only actual
  inserted units; full/missing/unknown home retains leftovers, never a drop fallback.

## Frozen actor bridges / ownership

- Public `getUseOptions()` / `boolean setUseOptions(GolemUseOptions)`; server core8,
  valid configuration/current worker and no ownership scope. Only DEFAULT succeeds;
  true Empty-space/Left/Sneaking flags return false, not an upgrade gate.
- `canUseMarkedBlock(marker,expectedTarget,expectedAdjacent)` is true only inside
  the private live Use attempt's busy, exact marker/face/state/home/identity/mode/
  permission/reach/sight binding. Before detach it requires the actual original
  carried reference/count/components. While detached it requires raw carried EMPTY
  **and root `isBorrowing(this)`**; remembered item identity is authorization only,
  never a copied supply/refund. Root must still validate its actual selected hand
  after the genuine Fabric BEFORE hook, immediately before native execution.
- `detachUseCargo()` requires root `isDetaching` plus that exact current job;
  raw-clears carried, enters detached phase even for EMPTY, immediately returns the
  **actual reference**, no sync/callback. `restoreUseCargo(actualHand,actualExtras)`
  requires root `isReturning` and the same private scope/detached phase, but accepts
  actual owned returns even if the worker died/became unsupported. It validates
  nonnull stacks/native stack limits/distinct live owners and copies list structure
  before raw assignment; at most **41 extras** (40 non-selected native inventory
  slots plus one cursor). No component stripping or material-carry clipping of
  real returned stock. Root raw-clears its matching slots only AFTER owners install.
- Root is called once via `GolemUsePlatform.useBlock(this,marker,target,adjacent)`.
  PASS/FAIL/throw never refunds a pre-call snapshot. Reconcile actual hand/extras in
  root finally on every ordinary RuntimeException path. Worker sync follows return;
  rightArm5/status8 only on native true, not the Guard both-arm timer or left-arm work.
- Extra actual owners stay outside GolemData. Existing `DeferredItemDrops.enqueue`
  must accept custody before the private list raw-clears. Failure retains the same
  list and blocks all new Use jobs; ticks/save/terminal handling retry **publication
  only**, never the click. Native `UseRemainders` NBT is a bounded all-or-nothing
  canonical ItemStack list; malformed/future/unread entries retain the whole raw
  tag, freeze unsafe edits/work/destructive removal, and are not partly published.
- Native removal during Use/borrow/return publication remembers one reason. Clear
  the runtime scope only after actual reconciliation/feedback; finish removal after
  extra publication and evacuate returned carried stock through the existing death
  queue before destructive handling. Normal recall publishes extras, includes actual
  carried separately, and refuses foreign equipment. Save/unload outside the call
  preserves failed-publication owners and does not publish again after a saved image.
- Saving DURING the transient Use scope/borrow refuses, rather than persisting an
  apparently empty paid hand. If root unexpectedly fails to return ownership,
  retain the private reference/binding, busy/detached authority and deferred removal;
  log/freeze, refuse save/new jobs. A later properly scoped actual return can settle;
  there is no invented EMPTY recovery, saved half-click, replay journal or token.

## Root integration / explicit gaps

- Root: enable `GolemData.USE=8` with six one-count ghosts, Fill=false and dedicated
  ordinary options; core registration/acquisition/book/assets/menu/client/placer.
  Entity uses the **six-argument configuredData/apply/recall** path and strict native
  `UseOptions` CODEC NBT; invalid/nondefault raw options survive retainedConfiguration.
  Non-Use cores require DEFAULT. Coreless installation8 refuses incompatible counts/
  Fill/options before payment, rather than stripping an existing whitelist.
- Root: provide the frozen `GolemUsePlatform` methods, actor/shared-borrow fence,
  native exactly-once game-mode/Fabric route selected by the independent reviewer,
  and nonbreaking `GolemHarvestPlatform.mayInteract` core8 admission. Query must use
  actual canWork while worker busy; no break/use callback as a permission probe,
  online-owner/OP borrowing, fake equipment or generic provider.
- Guard4/Butcher9 target/strike policies, seven materials/attributes, placement
  static guards, safe-home, remote links, item ownership, Fishing lifecycle/RNG,
  water/essentia cargo and their independent deferred-removal scopes are preserved.
  No other existing goal/stock/platform/data/client/recipe source was edited.
- Deferred: nondefault controls, tools/durability, other seeds/items/blocks, entity
  interactions/attacks, mining/fire/GUI use, colors/upgrades/addons. Native claim
  hooks are root's actual route, not a universal compatibility promise. FatalError,
  arbitrary addon cloning and power-loss atomicity are not promised. Root builds
  actual Fabric **after all FINALS**; gameplay/protection/save/visual acceptance
  remains separate and unexecuted.
