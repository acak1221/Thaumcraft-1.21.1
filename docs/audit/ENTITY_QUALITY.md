# Entity and AI quality audit

Date: 2026-10-23. Baseline: `3340abe7`, branch `agent/c3items`.

**Review outcome: request changes for lifecycle ownership; accept the constant-only cleanup.**
This is a maintainability audit, not a claim of gameplay parity or a runtime defect count.
H = high review priority; M = medium; L = low. Findings are grouped review requests;
multiple occurrences under one ID count once. Duplication is measured, not automatically
an instruction to build a shared superclass. Existing original-game quirks are not rewritten.

All abbreviated source references below are relative to
`common/src/main/java/thaumcraft/common/entities/`.

## Coverage and verification

The required initial `git status --short` was empty; `git log --oneline -1` was
`3340abe7 Merge branch 'agent/g32table' into work`. The required `find ... -type f | wc -l`
returned **95**. All 95 files are Java, **19,285 baseline lines**.

Source inventory: 196 class/record/anonymous declarations, 817 field declarations
(including record components), 2,267 method/constructor declarations, 1,042 `@Override`
annotations. The goal inventory contains **49 named concrete goals, 4 abstract goal bases,
and 10 anonymous goal adaptations**: 59 concrete implementations, 63 declarations total.
Vanilla goals instantiated without an adaptation were checked at registration sites, not
counted as locally implemented classes.

The checkout initially had no `common/build/classes/java/main`. Built all **1,445 common
sources** directly with Java 21 `javac --release 21 -proc:none`, using the cached Mojang
remapped 1.21.1 jar, the pre-existing access-widener overlay and dependency jars. No Gradle,
no main checkout access, no other worktree access. Then compiled the changed source against
that remapped classpath **plus this worktree's `common/build/classes/java/main`**.
`javap -c -p` confirms every changed class method has identical instructions before/after.
Transient inventory and compiler evidence are in `.tmp/c3items/` (not committed).
No server/client execution, path interruption test, save/reload test or original-jar
behavioral replay was run. Source and bytecode inspection do not establish those passes.

| Checklist | Number inspected | Findings |
|---|---:|---:|
| 1. Goal lifecycle | 59 concrete + 4 bases | 7 |
| 2. Ladder coherence | 12 core configurations + shared goals | 3 |
| 3. Switch-surviving state | 157 non-final goal/controller fields + 4 mutable collections | 3 |
| 4. Size / duplication | 95 files; 31 save + 33 load declarations | 24 |
| 5. Magic numbers | 803 numeric tokens in 21 named-goal-bearing files; 30 selected sites | 30 |
| 6. Dead code | 324 private non-constructor methods; 817 fields; 1,042 overrides screened | 0 confirmed |
| 7. Null safety | 909 target/navigation/level calls; 123 tick entry methods | 2 reviewability findings; 0 proven NPEs |
| 8. Family consistency | 8 family/policy comparisons | 4 |

## 1. Goal lifecycle — 59 concrete implementations and 4 bases

M/L/J/T mean MOVE/LOOK/JUMP/TARGET. `default` continuation means vanilla `Goal`
delegates to `canUse()`; an absent `stop` is the no-op base method. For inherited vanilla
adaptations, the parent owns lifecycle. Acquiring LOOK means scheduling the look control,
not an owned Java resource requiring the head orientation to be zeroed.

### Inventory goal table

All ten concrete inventory goal types inherit **M+L**, `requiresUpdateEveryTick=true`,
`canUse = canWork && ladderCanUse`, and `LadderGoal.stop` at
`golems/InventoryGolemGoals.java:272` (zero both counters; close and forget chest).
`decision` below is `GolemRouteRules.decisionTick`; ranges use the named rule helpers.

| Goal / declaration | canUse condition, beyond canWork | canContinueToUse | Acquires | Releases in stop |
|---|---|---|---|---|
| HomeReplace `InventoryGolemGoals.java:316` | cargo, decision, idle, home, timeout/no destination | chest window OR predicate | chest on successful home placement | counters + chest |
| HomePlace `:352` | cargo, decision, idle, home, live inventory with room | chest window OR predicate | chest | counters + chest |
| HomeDrop `:385` | cargo, idle, home, known world-drop home | count AND (predicate OR chest window) | count after throw | counters; no path acquired |
| FillTake `:412` | empty cargo, watched identity, idle, missing stock, adjacent source | count AND (unfinished navigation OR chest window) | chest; consumes watched identity | counters + chest |
| EmptyPlace `:461` | cargo, idle, adjacent container with room | count AND (predicate OR chest window) | chest | counters + chest |
| EmptyDrop `:501` | cargo, idle, adjacent world marker | count AND predicate | count after throw | counters; no path acquired |
| HomeTake `:528` | decision, empty cargo, idle, home, extractable deliverable candidate | chest window | chest | counters + chest |
| SortingPlace `:584` | cargo, idle, adjacent seeded container | count AND (predicate OR chest window) | chest | counters + chest |
| HomeTakeSorting `:624` | decision, empty cargo, idle, home, extractable seeded destination candidate | count AND (predicate OR chest window) | chest | counters + chest |
| Route `:680` | no arrival handoff; core-specific decision selects supply/destination | budget, same pursued marker, unfinished path | target/pursued, path, budget, previous position | `:744`: pursued, owned target, running marker, counters, chest, navigation; watched explicitly handed to FillTake |

### Other golem goal table

References in this table are under `golems/`.

| Goal / declaration | Flags | canUse → continuation | Acquires | Releases in stop |
|---|---|---|---|---|
| Gather.Pickup `GatherGolemGoals.java:118` | M+L | eligible selected item → work, budget, eligible pursued item and path/in reach | path, pursued, budget | `:148` target if ours, pursued, budget, navigation |
| Gather.ReturnHome `:156` | M+L | work, no work route, no backoff, away → same + budget + unfinished path | path, previous, budget | `:191` navigation only; start overwrites previous/budget |
| Gather.AvoidSwell `:194` | M | work, loaded area, visible swelling creeper, reachable escape → work, live threat, unfinished path | threat, escape, Path, navigation, initial velocity | `:224` threat only; **path not stopped** |
| GolemDoorGoal `GolemDoorGoal.java:56` | J | work, collision, unfinished path, door/gate → openTicks/count | opened door, selected position/type, counters | `:111` closes door, clears position; counters/type retained but overwritten; noninterruptible |
| Guard.Melee `GuardGolemGoals.java:94` | M+L | working, policy-valid current target, loaded target, candidate path → running, same policy target, unfinished path | victim, Path, running, repath, navigation | `:125` victim/path/repath/running; stops navigation if running |
| Guard.Dart `:157` | M+L | valid target beyond minimum distance → running, predicate, unfinished path | chosen, running, wait, navigation | `:183` target/running, half-interval wait, navigation if running |
| Guard.Retaliate `:206` | T | Guard, work, new hurt timestamp, eligible attacker → same current/hurt target, policy | chosen; mob target; timestamp | `:225` clears mob target iff ours, chosen; keeps timestamp |
| Guard.Nearest `:231` | T | Guard/work/server, nearest eligible → chosen is current valid target | chosen, mob target; repicks each tick | `:261` clears target iff ours, chosen |
| Guard.OldestButcher `:279` | T | Butcher/work/server thread, oldest surplus animal → same valid surplus target | chosen, mob target | `:312` clears target iff ours, chosen |
| Harvest `HarvestGolemGoals.java:18` | M+L | not running, current ripe target → running, budget, current target, work delay/path | path, target/state, work timers, crack overlay | `:170` clearTarget + running false; crack overlay retired |
| Lumber `LumberGolemGoals.java:23` | M+L | not running, current log → running, budget, current log | path, target/state, work timers, crack overlay | `:197` clearTarget + running false; crack overlay retired |
| Fishing `FishingGolemGoals.java:39` | M+L | live admitted cast, idle navigation, no attempt/output → running, live cast, no attempt/output | admitted bobber/reservation, pond snapshot, timer | `:349` guarded cast cancellation and running false; **paid pending output intentionally survives**; no path acquired |
| Liquid.Empty `LiquidGolemGoals.java:308` | M+L | emptyReady and canEmpty → false (one shot) | attempted latch, deliveryAttempt during callback | `:322` emptying false; callback finally clears deliveryAttempt; start resets latch |
| Liquid.Gather `:324` | M+L | watched/ready/room/idle, wanted adjacent source → gathering, count<20, watched/ready/room/idle | watched source, gather timer, attempt authority | `:376` clearSource, owned path and authority; queues intentionally retained; noninterruptible |
| Liquid.Goto `:378` | M+L | gotoReady, route, watched/ready/room → moving + budget + unfinished path | owned path + route level, route, origin, budget | `:410` owned path, moving/gotoReady/route/count; watched retained for adjacent gather only |
| Alchemy.Gather `AlchemyGolemGoals.java:293` | M+L | delay elapsed, current source → false (one shot) | gathering, attempted; collection authority during callback | `:315` gathering false; callback finally clears endpoint/aspect; cooldown intentionally survives |
| Alchemy.Empty `:317` | M+L | emptyReady, ready, idle, cargo → false (one shot) | emptying/attempted, delivery authority | `:336` emptying false; callback finally clears endpoint/aspect/target/gotoReady |
| Alchemy.Goto `:338` | M+L | gotoReady and current destination → moving + budget + destination + unfinished path | path/level, origin, nudge latch, target, budget | `:370` owned path/level, moving/gotoReady/target/budget; start overwrites origin/nudge latch |
| Use.Replace `UseGolemGoals.java:185` | M+L | working, pending delay OR returnable cargo at ready home → working and delay | guarded return authority, paid-transfer delay | inherited `:183` deliberately preserves delay; finally clears attempting |
| Use.Take `:201` | M+L | pending delay OR ready empty golem with home seed → working and delay | seed, guarded take authority, paid-transfer delay | inherited no-op; start finally clears seed/attempting; reset also clears seed |
| Use.Click `:218` | M+L | working, no home delay, scan due, idle, applicable loaded target → running, budget, current target, unfinished path | selected snapshot, path, budget, attempting during call | `:255` path if running, running/budget/selected; callback finally clears attempting |
| Shared LookAtPlayer anonymous `GatherGolemGoals.java:68` | L inherited | work AND parent acquisition/continuation | parent look target/timer | inherited parent stop |
| Shared RandomLookAround anonymous `:72` | L inherited | work AND parent acquisition/continuation | parent look duration/direction | inherited parent lifecycle |

### Non-golem goal table

| Goal / declaration | Flags | canUse → continuation | Acquires | Releases in stop |
|---|---|---|---|---|
| TaintCreeperSwell `ai/combat/TaintCreeperSwell.java:19` | M | already swelling OR target within range → saved attackTarget OR positive swell | cached target; stops existing path | `:50` cached target; swell changed in tick, not stop |
| PechAvoid `ai/pech/PechAvoidGoal.java:16` | M | nearest player, valid retreat/exact path end → unfinished navigation | target, path, navigation | `:41` target only; **navigation/path retained** |
| PechItem `ai/pech/PechItemGoal.java:15` | M+L | five-tick/count gate, nearest pickup candidate → live target/path/range | target, navigation, retry counters | `:32` target only; **navigation retained** |
| PechMelee `ai/pech/PechMeleeGoal.java:11` | M+L | live target, throttled path candidate → live target/unfinished path | path, navigation, retry coordinates/timers | `:21` navigation only; cached path/penalties survive |
| PechRanged `ai/pech/PechRangedGoal.java:11` | M+L | current target → predicate OR unfinished path | cached target, sight/attack timer, navigation | `:25` target/sight/attack timer; **navigation retained** |
| PechTrade `ai/pech/PechTradePlayer.java:21` | M+J | alive, dry, tamed, grounded, not passenger/hurt, trading → default predicate | freezes navigation, trade state | `:44` trading false; no path acquired |
| PechWatch `ai/pech/PechWatchGoal.java:12` | player M+L; mob L | chance + nearest entity → live target/range/time | look target/time | `:28` target; start overwrites time |
| TaintAnimalMelee `monster/TaintAnimalMeleeGoal.java:25` | M+L | live matching target, throttled path → live target and longMemory/path | path, navigation, retry coordinates/timers | `:59` navigation only; cached state survives |
| TaintAnimal.Swim `:114` | M | nonpassenger, in water, submerged eye → default predicate | vertical impulse, no persistent goal state | no stop; impulse is physical state, not a path |
| CultistMelee `monster/EntityCultist.java:223` | M+L | live target, throttled candidate path → live target and path/range | navigation, pathFindTimer/attackTick | `:254` navigation; counters survive |
| CultistLongRange `:285` | M+L | live target, min/max distance, LOS → predicate OR unfinished path | target, navigation, attack/sight timers | `:323` target/timers; **navigation retained** |
| CultistHurtByTarget `:366` | T | new hurt timestamp and attackable type → default predicate (timestamp makes it false after start) | mob target, lastRevengeTimer, ally targets | **no stop**; entity target intentionally/implicitly outlives selector entry |
| CultistSwim `:416` | M | in water → in water | swimming flag and vertical motion | **no stop**; swimming flag reset delegated to entity lifecycle, undocumented here |
| CultistRestrictDoor `:452` | none | night, village POI, nearby door → night, selected nearby door | door reference, navigation door permissions | `:482` restores permissions, clears door; does not claim MOVE |
| CultistAltarFocus `monster/EntityCultistCleric.java:180` | L | ritualist and restriction center → same | look control; may end ritualist state in tick | no owned path/target; no stop |
| WatchClosestWatcher `monster/EntityWatcher.java:176` | **M** | closest watcher query → default predicate | **look control despite no L flag** | no stop/state; re-queries each tick |
| GuardianAttack `:207` | M+L | live target → live target and elder/min distance | attack timer, synced target ID/gaze counter; freezes path | `:228` target ID/gaze counter, mob target; start resets attack timer |
| OriginalPlayerTarget `monster/EntityTaintSpider.java:34` | T inherited | parent acquisition without LOS → inherited parent continuation | parent chosen/mob target | inherited TargetGoal.stop |

The remaining **8 anonymous adaptations**, with no additional owned fields:

| Adaptation | Flags / acquisition / continuation / release |
|---|---|
| `monster/EntityTaintCrawler.java:67` | M+L; TaintAnimalMelee lifecycle above; adds afterAttack only |
| `monster/EntityTaintSeed.java:71` | M+L; TaintAnimalMelee lifecycle above; adds beforeAttack only |
| `monster/EntityEldritchCrab.java:64` | M+L; vanilla MeleeAttackGoal lifecycle; changes attack interval only |
| `monster/EntityEldritchGolem.java:111` | M+L; same vanilla melee lifecycle; interval only |
| `monster/EntityEldritchGolem.java:240` | M+L; vanilla RangedAttackGoal; canUse adds target/min range; inherited continuation and stop |
| `monster/EntityEldritchGuardian.java:56` | M+L; vanilla RangedAttackGoal; adds min range; inherited continuation and stop |
| `monster/EntityEldritchWarden.java:281` | M+L; vanilla RangedAttackGoal; adds min range; inherited continuation and stop |
| `monster/EntityEldritchWarden.java:287` | M+L; vanilla melee lifecycle; interval only |

Four abstract bases inspected: `InventoryGolemGoals.java:253`, `LiquidGolemGoals.java:304`,
`AlchemyGolemGoals.java:289`, `UseGolemGoals.java:176`. The first owns chest cleanup; the
last intentionally preserves a paid-transfer delay. Liquid/Alchemy bases only supply flags
and full-rate updates. Their concrete methods above supply admission and cleanup.

### Lifecycle findings

1. **G1 H — bulk goal removal bypasses resource cleanup.** `golems/EntityGolemBase.java:1026-1039`
   manually resets controllers then calls `removeAllGoals`. Remapped `GoalSelector` bytecode
   proves that method only `removeIf`s available entries; unlike `removeGoal`, it never calls
   running goals' `stop()`. `inventories.reset()` clears route/timeout state, not the private
   LadderGoal chest; the active GolemDoorGoal is not retained by a controller at all. Thus the
   rebuild has no explicit closing path for `InventoryGolemGoals.java:295-303` or
   `GolemDoorGoal.java:111-115`. Maintain an explicit stop-before-remove lifecycle, with tests
   for menu/core/decoration rebuilds during a chest or door window. Not fixed: behavior change.
2. **G2 M — missing path release:** `golems/GatherGolemGoals.java:224`; AvoidSwell starts
   navigation at `:221`, but stop only nulls threat. Stop the owned path and retire cached
   path/escape when ownership is established. Not fixed blindly: affects interrupted flight.
3. **G3 M — missing path release:** `ai/pech/PechAvoidGoal.java:41`; navigation started `:40`,
   target only cleared. Relevant during tame/combat-task changes (`monster/EntityPech.java:124`).
4. **G4 M — missing path release:** `ai/pech/PechItemGoal.java:32`; navigation acquired `:31,40`,
   stop clears only target. Next goal may inherit motion before creating its own path.
5. **G5 M — missing path release:** `ai/pech/PechRangedGoal.java:25`; navigation acquired `:29`,
   stop clears combat state but not navigation. Naming constants does not repair ownership.
6. **G6 M — missing path release:** `monster/EntityCultist.java:323-327`; ranged goal drives
   navigation at `:336`, but cleanup only resets target/sight/timer.
7. **G7 M — target lifetime is implicit:** `monster/EntityCultist.java:366-410`; the custom
   hurt goal writes entity/ally targets but has no stop/ownership guard. Its default continuation
   becomes false immediately after recording the timestamp. Either explicitly document this
   as an intentional target publication (not a retained goal lease), or adopt an ownership-aware
   TargetGoal lifecycle. Blindly clearing targets on stop would break retaliation; not fixed.

## 2. Goal ladder coherence — 12 core configurations

One **active** owner per intersecting flag is the invariant, not one registered goal per
combination. Multiple registered M+L goals are correct when the selector excludes them.
Original mutex bits 3 map to M+L (`GolemLadderRules.ladderFlags`, used at
`golems/InventoryGolemGoals.java:261`).

| Core | Work entries in registration/priority order | Core work flags | Concurrent movement owners found |
|---|---|---|---:|
| Fill | HomeReplace0, HomePlace1, HomeDrop2, FillTake3, Route4 | all M+L | 0 |
| Empty | HomeReplace0, EmptyPlace1, EmptyDrop2, Route3, HomeTake4 | all M+L | 0 |
| Sorting | HomeReplace0, SortingPlace1, Route3, HomeTakeSorting4 | all M+L | 0 |
| Gather | Pickup2; home deposit in controller serverTick | M+L | 0 selector collisions; see L3 |
| Harvest | Harvest2 | M+L | 0 |
| Guard | Dart2 if decorated, Melee3; Retaliate1/Nearest2 in target selector | M+L; T separately | 0 |
| Liquid | Empty1, Gather2, Goto3 | all M+L | 0 |
| Alchemy | Empty1, Gather2, Goto3 | all M+L | 0 |
| Lumber | Lumber2 | M+L | 0 |
| Use | Replace0, Click0, Take4 | all M+L | 0; equal priority cannot preempt |
| Butcher | Dart2 if decorated, Melee3; Oldest1 target selector | M+L; T separately | 0 |
| Fishing | Fish2 | M+L | 0 |

Every enabled core adds AvoidSwell0 (M) first, then shared Door5 (J), ReturnHome6 (M+L),
Watch7 (L), IdleLook8 (L). Door may coexist with work by design; target goals are in a
separate selector. No golem target goal starts navigation. No simultaneous native golem
MOVE owners were found. Equal-priority Use/flight/replace interactions merit a scheduler
test, not renumbering on suspicion.

1. **L1 L — inaccurate five-entry narrative.** `golems/InventoryGolemGoals.java:19,33,61`
   says five entries per core, but `:89-93` correctly registers four for Sorting. Its own list
   at `:27-28` and EntityGolemBase's detailed list agree with four. User checklist's universal
   five-entry premise is not supported by these registrations; do not invent a fifth goal.
2. **L2 M — LOOK writer lacks LOOK mutex.** `monster/EntityWatcher.java:183,190` reserves M
   but changes look control. `:72,74` also registers vanilla player-watch/idle-look goals with L;
   they can run beside this goal and compete for the same look controller. This is not a golem
   M+L collision, but it is a real ladder ownership inconsistency outside that subsystem.
3. **L3 M — transfer work bypasses the ladder.** `golems/GatherGolemGoals.java:104-105`
   deposits cargo from serverTick with no work-goal ownership. `EntityGolemBase.java:1001`
   claims Gather has HomeReplace0/HomePlace1/Pickup2, but `:1054` installs Pickup only.
   The selector has one movement owner; it does not arbitrate the deposit operation. Document
   and test deposit-during-flight/door/return behavior, or move transfer ownership into goals.

## 3. Switch-surviving state — 161 storage slots reviewed

**157 non-final fields** declared in named goals/controller objects plus **4 final mutable
collections**. Owner references/constants are not mutable lifecycle slots. Each field name
below appears in this inventory, including intentional carryover and overwrite-on-start.
Entity-owned state written by goals is listed separately. A cooldown, paid output, timeout
or deterministic search cursor must **not** be zeroed just to satisfy a mechanical rule.

### Golem state ledger

References in this ledger are under `golems/`. `start` is a reset site, not `stop` cleanup.

| Owner / fields | Reset / handoff / retention site |
|---|---|
| Inventory: `target, travelStarted, interactionReady` | `InventoryGolemGoals.java:121-126`; Route.stop `:744-752`; arrival flag consumed `:118` |
| Inventory: `watched` | cleared configuration `:105`, successful FillTake `:434`; **intentionally survives Route.stop** `:753` |
| Inventory: `failedItems` (final mutable list) | expiry sweep `:225-228`; reset `:110`; survives ordinary goal switch |
| LadderGoal: `count, countChest, chest` | stop `:272-275`, closeChest `:300-303` |
| Route: `pursued` | stop `:749` |
| Route: `previous` | overwritten start `:714`; not cleared stop |
| Gather controller: `target` | reset `GatherGolemGoals.java:79`, idle scan `:101-109`, pickup success/stop `:144,149` |
| Gather controller: `returnDelay, returnDelayIncrement` | reset `:80-81`; clock `:100`; successful path `:177`; intentionally preserve failure backoff between runs |
| Gather controller: `pickup` | replaced by install `:44`; reference to installed goal, not work cargo |
| Pickup: `pursued, budget` | stop `:150-151` |
| ReturnHome: `budget, previous` | start `:170-171`; stop only navigation `:191` |
| AvoidSwell: `threat` | stop `:224`; admission resets `:203` |
| AvoidSwell: `escape, path` | overwritten successful admission `:211,213`; no stop reset; G2 |
| Door: `door` | stop `GolemDoorGoal.java:114` after close |
| Door: `gate, count, openTicks` | select `:123-124`, admission `:82,87`, start `:98`; not zeroed stop |
| Guard controller: `attackCooldown` | initialized once; paid attempt `GuardGolemGoals.java:86`, decremented active melee `:135`; explicit NEVER reset comment `:25` |
| Guard controller: `attempting, strikeTarget, strikeCore` | try/finally `:91`; reset `:57-59` |
| Melee: `victim, path, repath, running` | stop `:125-131` |
| Dart: `chosen, running, wait` | stop `:183-188`; wait deliberately half interval, not zero |
| Retaliate: `chosen` | stop `:225-227`; controller reset `:62` |
| Retaliate: `lastHurtTimestamp` | start `:222`; intentionally remembers processed hurt event |
| Nearest / Oldest: `chosen` each | stop `:261-263 / :312-314`; controller reset `:62` |
| Harvest: `target, expected, delay, maxDelay, pulseMod, budget, attempting` | clearTarget `HarvestGolemGoals.java:156-168`, called stop `:170` |
| Harvest: `running` | stop finally `:172` |
| Harvest: `scanHome, columns` (final mutable list) | reset `:44`; scanHome set `:51-52`; columns exhausted/rebuilt `:56-65`; cursor survives ordinary switch |
| Harvest: `columnsRadius` | overwritten rebuild `:58`; not zeroed reset, but empty list forces rebuild |
| Lumber: `target, expected, delay, maxDelay, pulseMod, budget, attempting` | clearTarget `LumberGolemGoals.java:183-195`, called stop `:197` |
| Lumber: `running, scanHome` | stop finally `:199`; reset finally `:47` |
| Fishing: `water, waterState, airState, admitted, count, quality, attempted, firstUpdate` | cancelCast `FishingGolemGoals.java:321-330`; attempted becomes true to retire cast |
| Fishing: `bobber, admission, castLevel, nativeAdded` | cleanBobber `:332-347`; exact references retained on cleanup failure for retry, not discarded blindly |
| Fishing: `addingBobber, cleaningBobber` | finally `:169,347` around native reservation/cleanup callbacks |
| Fishing: `running` | stop finally `:351`; resetRuntime `:83` |
| Fishing: `pending, unreadPending` | paid output deliberately survives stop/cancel; publish clears pending `:299`; load reconstructs or retains raw `:367-392` |
| Liquid: `source, route, origin, watched, moving, gathering, gotoReady, gatherReady, pathCount, gatherCount` | clearSource `LiquidGolemGoals.java:287-291`; Goto.stop `:410-415` preserves only adjacent-gather watch handoff; start overwrites origin |
| Liquid: `ownedPath, routeLevel` | identity-checked stopOwnedPath `:282-285` |
| Liquid: `attemptSource, attemptAnchor, attemptState, attemptHandler, attemptHandlerFace, attemptFluid` | clearSource `:292-295`; payment finally `:350,367` |
| Liquid: `home, wanted, deliveryAttempt, emptying, emptyReady` | reset `:299-301`; delivery finally `:320`; Empty.stop clears emptying `:322`; readiness re-evaluated in serverTick |
| Liquid: `queues` (final mutable map) | prune unmarked anchors `:154`; reset `:302`; intentionally retains Entropy traversal between pulses |
| Liquid.Empty: `attempted` | start `:312`; one-shot latch intentionally remains true through stop |
| Alchemy: `target, gotoReady, emptyReady, gathering, moving, emptying, pathCount` | reset `AlchemyGolemGoals.java:279-286`; per-goal stops `:315,336,370`; decisions republish target/readiness |
| Alchemy: `ownedPath, pathLevel` | identity-checked clearPath `:274-277` |
| Alchemy: `collecting, delivering, attemptedAspect` | callback finally `:313,334`; reset `:281-283` |
| Alchemy: `origin` | reset `:284`; overwritten Goto.start `:347`; not cleared Goto.stop |
| Alchemy: `nudged` | overwritten Goto.start `:348`; not cleared Goto.stop/reset |
| Alchemy: `gatherAfter` | paid/failed attempt `:301,310`; deliberately survives reset `:287` |
| Alchemy.Gather / Empty: `attempted` each | respective start `:301,321`; retained one-shot latch until next start |
| Use: `nextTick, timeouts` (final mutable list) | install `UseGolemGoals.java:34-35`; timeouts expire `:139`; cadence updated `:229`; preserve through ordinary stop |
| Use.HomeTransfer: `delay, attempting` (in both concrete instances) | delay decremented `:182`, install `:33`; no-op stop deliberate `:183`; attempting cleared finally `:196,215` |
| Use.Take: `seed` | reset `:42`; start finally `:215`; canUse candidate `:206` |
| Use.Click: `selected, running, budget` | stop `:255-259` |
| Use.Click: `attempting` | callback finally `:253`; not unconditionally reset by stop during an in-progress callback |

### Non-golem state ledger

| Owner / fields | Reset / retention site |
|---|---|
| TaintCreeperSwell: `attackTarget` | `ai/combat/TaintCreeperSwell.java:50-51` |
| PechAvoid: `target, path` | target cleared `ai/pech/PechAvoidGoal.java:41`; path only overwritten `:36` |
| PechItem: `target, count, failedPathFindingPenalty` | target stop `ai/pech/PechItemGoal.java:32`; count start/pickup `:31,44`; penalty updated `:41`, not reset stop |
| PechMelee: `path, delay, attackTick, failedPenalty, oldX, oldY, oldZ` | `ai/pech/PechMeleeGoal.java:17,20,26-33`; path/timers/coordinates retained across stop; delay overwritten start |
| PechRanged: `target, sight, attackTime` | stop `ai/pech/PechRangedGoal.java:25` |
| PechWatch: `target, time` | target stop `ai/pech/PechWatchGoal.java:28`; time start `:27` |
| TaintAnimalMelee: `path, pathDelay, attackTick, failedPathFindingPenalty, oldX, oldY, oldZ` | `monster/TaintAnimalMeleeGoal.java:43-44,57,68-82`; path/penalties/coordinates survive stop |
| CultistMelee: `attackTick, pathFindTimer` | `monster/EntityCultist.java:241,264,274`; retained across stop |
| CultistLongRange: `target, attackTime, seeTime` | stop `monster/EntityCultist.java:323-326` |
| CultistHurtByTarget: `lastRevengeTimer` | start `monster/EntityCultist.java:388`; processed-event latch deliberately retained |
| CultistRestrictDoor: `door` | stop `monster/EntityCultist.java:485` |
| Watcher.GuardianAttack: `attackTimer` | start `monster/EntityWatcher.java:224`; retained on stop, overwritten next start |

Additional entity-owned writes: native navigation/look requests in the goal table;
Pech `trading` cleared `PechTradePlayer.java:44`; creeper swell set by tick
`TaintCreeperSwell.java:55-64` (not stop); target-goal writes to native target and allies
as described G7; watcher target ID and `gazeCounter` cleared by `EntityWatcher.java:89,229`;
cultist swimming flag set `EntityCultist.java:434`, no local false/reset; ritualist flag may
be retired by `EntityCultistCleric.java:195`; paid cargo is changed only through transfer
methods, never erased as goal cleanup. Harvest/lumber crack overlays are closed by clearTarget.
No additional fields are introduced by the anonymous goal adaptations.

1. **S1 M — stale client target cache.** `monster/EntityWatcher.java:42,89-101`: cached
   `targetedEntity` is assigned when resolving a synced ID, but never invalidated when that
   ID changes or is cleared. Attack stop resets ID/gaze counter, not this client cache. Review
   a synced-data invalidation hook and a two-target rendering test; not a no-op fix.
2. **S2 M — path/target history lifetime lacks a local contract.** `ai/pech/PechMeleeGoal.java:12-33`
   and `monster/TaintAnimalMeleeGoal.java:30-79` retain path, old XYZ and failure penalties after
   stop. Admission may return true while its path-delay remains positive, reusing the prior
   path. Some carryover is authentic legacy policy; explicitly distinguish paid attack/retry
   timing from target-bound path state and test target switches before changing it.
3. **S3 L — reset policy is scattered.** `golems/GatherGolemGoals.java:191`,
   `InventoryGolemGoals.java:744`, `AlchemyGolemGoals.java:370`, `GolemDoorGoal.java:111`
   leave overwritten-on-start scratch fields (budget/previous, previous, origin/nudged,
   counters/gate respectively). These are not demonstrated behavioral defects. Local lifecycle
   comments or explicit scratch resets would make this ledger unnecessary for future reviewers.

## 4. Class size and duplication — all 95 files

### Size finding

**D1 M — oversized responsibility boundaries.** Ten source units exceed 400 lines;
five are actual entity source units. Counts below are baseline and unaffected by this patch.
`EntityEldritchWarden.java` also contains the package-private boss base.

| File:line | Lines | Review burden |
|---|---:|---|
| `golems/EntityGolemBase.java:1` | 3,050 | entity, cargo custody, three payment systems, configuration, persistence, diagnostics |
| `golems/InventoryGolemGoals.java:1` | 809 | shared route controller + ten concrete selector entries |
| `monster/EntityCultist.java:1` | 534 | mob, targeting predicates, five goal implementations |
| `monster/EntityEldritchWarden.java:1` | 524 | shared boss base + concrete boss |
| `golems/GolemUsePlatform.java:1` | 519 | actor/callback/custody boundary |
| `golems/GolemHarvestPlatform.java:1` | 481 | break/replant/callback boundary |
| `monster/EntityCultistLeader.java:1` | 445 | duplicate boss concerns outside shared boss base |
| `golems/LiquidGolemGoals.java:1` | 418 | scan/queue/route/transfer authority |
| `golems/EntityTravelingTrunk.java:1` | 414 | persistence, inventory, interactions and inline movement/combat |
| `golems/GolemHelper.java:1` | 404 | inventory/fluid query facade |

### Exact duplicate method bodies

Parsed Java method bodies were compared after whitespace normalization (comments are absent
from the AST), minimum 90 non-whitespace characters. **20 non-constructor groups** follow.
Tiny overrides below that threshold are not automatically findings. Four identical animal
type/level constructors were also observed (`EntityTaintPig.java:35`, `EntityTaintCow.java:35`,
`EntityTaintSheep.java:53`, `EntityTaintChicken.java:37`); routine constructors are acceptable,
not a finding. Exact duplicates are candidates for small helpers, not proof of wrong behavior.

| ID / severity | Duplicate method | Every location |
|---|---|---|
| D2 L | custom sound lookup | `monster/EntityEldritchGolem.java:337`, `EntityEldritchCrab.java:154`, `EntityEldritchGuardian.java:126`, `EntityInhabitedZombie.java:118`, `EntityEldritchWarden.java:523`, `EntityWisp.java:131`, `EntityThaumicSlime.java:273`, `EntityTaintacle.java:200` |
| D3 L | adjacentSolid AI/world predicate | `monster/EntityTaintCrawler.java:93`, `monster/EntityTaintSeed.java:166` |
| D4 L | tickDeath XP routing | `monster/EntityTaintCrawler.java:117`, `EntityTaintPig.java:84`, `EntityTaintCow.java:83`, `EntityTaintSheep.java:156`, `EntityTaintChicken.java:101`, `EntityTaintVillager.java:114`, `EntityTaintSeed.java:183`, `EntityTaintacle.java:186` |
| D5 L | playSound | `monster/EntityTaintSpore.java:98`, `monster/EntityTaintSpider.java:40` |
| D6 L | isAlliedTo AI predicate | `monster/EntityCultistLeader.java:437`, `monster/EntityCultist.java:99` |
| D7 L | canAttackClass AI predicate | `monster/EntityCultistLeader.java:442`, `monster/EntityCultist.java:104` |
| D8 L | registerGoals, including acquisition predicates | `monster/EntityTaintPig.java:48`, `monster/EntityTaintCow.java:48` |
| D9 L | sound helper, registry fallback | `monster/EntityPech.java:241`, `golems/EntityTravelingTrunk.java:90`, `golems/EntityGolemBase.java:252` |
| D10 M | legacyCreatureAction targeting | `monster/EntityTaintSwarm.java:140`, `monster/EntityFireBat.java:127` |
| D11 M | legacyIdleAction | `monster/EntityTaintSwarm.java:181`, `monster/EntityFireBat.java:168` |
| D12 L | getMovementToShoot, legacy float path | `projectile/EntityGolemOrb.java:102`, `EntityPrimalArrow.java:131`, `EntityBottleTaint.java:91`, `EntityPechBlast.java:63`, `EntityFrostShard.java:93` |
| D13 L | getMovementToShoot, other precision path | `projectile/EntityShockOrb.java:58`, `EntityExplosiveOrb.java:63`, `EntityPrimalOrb.java:51` |
| D14 L | updateRotation | `projectile/EntityPechBlast.java:88`, `projectile/EntityFrostShard.java:171` |
| D15 L | canHitEntity predicate | `projectile/EntityPechBlast.java:95`, `EntityFrostShard.java:184`, `EntityEmber.java:118` |
| D16 L | handleEntityEvent | `projectile/EntityPechBlast.java:115`, `projectile/EntityPrimalOrb.java:120` |
| D17 L | loaded-cell predicate (different method names) | `golems/LiquidGolemGoals.java:89`, `GolemHarvestPlatform.java:451`, `AlchemyGolemGoals.java:56` |
| D18 L | startPath | `golems/LumberGolemGoals.java:115`, `golems/HarvestGolemGoals.java:110` |
| D19 L | clearTarget | `golems/LumberGolemGoals.java:183`, `golems/HarvestGolemGoals.java:156` |
| D20 L | HomeReplace.start / HomePlace.start | `golems/InventoryGolemGoals.java:334,365` |
| D21 L | decideEmptyRoute / decideSortingRoute | `golems/InventoryGolemGoals.java:779,793` |

Additional duplication findings:

* **D22 M — repeated restriction serialization.** **31 save and 33 load declarations**
  inspected (Minecraft names `addAdditionalSaveData/readAdditionalSaveData`, not legacy
  `saveAdditional/loadAdditional`). **Zero exact complete duplicate save/load bodies**.
  The same HomeD/HomeX/HomeY/HomeZ block is repeated in **three save/load pairs**:
  `monster/EntityCultist.java:112,123`, `EntityCultistLeader.java:148,161`,
  `EntityEldritchWarden.java:88,99` (boss base). Guardian is a fourth analogous pair using
  a different schema (`EntityEldritchGuardian.java:146,150`). A tiny restriction codec can
  remove key/shape drift without creating an inheritance tangle. Do not silently change keys.
* **D23 M — three diverging legacy melee implementations.** `ai/pech/PechMeleeGoal.java:15-33`,
  `monster/TaintAnimalMeleeGoal.java:38-85`, `monster/EntityCultist.java:237-276` repeat
  acquisition, continuation, reach and retry logic with different memory/cadence policies.
  Extract only policy-free geometry/timer rules after tests; unifying the entire goals is unsafe.
* **D24 L — repeated string-based cultist predicate.** `monster/EntityEldritchCrab.java:72`,
  `EntityEldritchGuardian.java:68`, `EntityEldritchWarden.java:297` use the same translated
  description-ID prefix test. Prefer a named type/tag/family predicate when parity is established.

## 5. Magic numbers — 803 tokens screened, worst 30 sites

Screened whole source text of the **21 files declaring named custom Goal implementations**,
including surrounding entity code, after removing comments/strings. The 803 count includes
0/1, indices, named constant initializers and geometric offsets: **not 803 defects**. Thirty
review sites below prioritize AI policy, not harmless half-block center coordinates. Names
are suggestions; preserve exact values and arithmetic precision. Source citations are the
current code's legacy class/method references, not newly proven oracle equivalence.

| ID / severity | File:line | Values / proposed names | Existing provenance to retain / action |
|---|---|---|---|
| M1 L | `golems/GatherGolemGoals.java:103` | 5, DECISION_INTERVAL | original golemDelay / cadence comment `:98` |
| M2 L | `golems/GatherGolemGoals.java:104` | 5 squared, HOME_DEPOSIT_REACH_SQ | ordinary Gather home deposit rule |
| M3 L | `golems/GatherGolemGoals.java:127` | 2 squared, PICKUP_REACH_SQ | original reach documented `:91,134` |
| M4 L | `golems/GatherGolemGoals.java:131` | 200, PICKUP_PATH_BUDGET | AIItemPickup legacy work budget |
| M5 L | `golems/GatherGolemGoals.java:163` | 3 squared, RETURN_HOME_THRESHOLD_SQ | AIReturnHome admission |
| M6 L | `golems/GatherGolemGoals.java:170` | 20, RETURN_PATH_BUDGET | AIReturnHome route/detour window |
| M7 L | `golems/GatherGolemGoals.java:180` | 5,50, RETURN_BACKOFF_STEP/MAX | failed-return policy; do not merge with decision interval merely because both are 5 |
| M8 L | `golems/GatherGolemGoals.java:204` | 5,3,5, CREEPER_SCAN_XZ/Y | AIAvoidCreeperSwell |
| M9 L | `golems/GatherGolemGoals.java:211` | 16,7, FLEE_SEARCH_XZ/Y | same flight geometry |
| M10 L | `golems/GatherGolemGoals.java:220` | 1.2,.8,.3, FLEE_INERTIA/IMPULSE/LIFT | explicit original escape velocity policy |
| M11 L | `golems/GatherGolemGoals.java:223` | 49,1.25,1.125, CLOSE_THREAT_SQ/FAST/SLOW_FLEE_SPEED | flight speed thresholds |
| M12 L | `golems/GuardGolemGoals.java:144` | 4 + nextInt(7), REPATH_BASE/JITTER | AIGolemAttackOnCollide |
| M13 L | `golems/GuardGolemGoals.java:271` | 4, GUARD_SCAN_HEIGHT | original actor AABB, comment `:269` |
| M14 L | `golems/HarvestGolemGoals.java:66` | -3..3, CROP_SCAN_Y_RADIUS | AIHarvestCrops column scan |
| M15 L | `golems/HarvestGolemGoals.java:111` | 200, HARVEST_PATH_BUDGET | original work approach budget |
| M16 L | `golems/HarvestGolemGoals.java:126` | 10,20,2, MIN_WORK_DELAY/BASE/STRENGTH_SCALE | hardness/strength formula, preserve float arithmetic |
| M17 L | `golems/HarvestGolemGoals.java:151` | 2 XZ / 1 Y, CHAIN_SCAN_XZ/Y | original traversal comment `:150` |
| M18 L | `golems/LumberGolemGoals.java:116` | 200, LUMBER_PATH_BUDGET | AIHarvestLogs; work delay already extracted into LumberGeometry |
| M19 L | `golems/FishingGolemGoals.java:174` | 300 + nextInt(200), CAST_DURATION_BASE/JITTER | explicit AIFish:161 / world RNG |
| M20 L | `golems/FishingGolemGoals.java:258` | .00015F, STRENGTH_CATCH_CHANCE | original AIFish active roll; preserve exact float |
| M21 L | `golems/LiquidGolemGoals.java:330-337` | 20,10, GATHER_TIMEOUT/PULSE_DELAY | AILiquidGather pulse/reset rules; do not collapse these values |
| M22 L | `golems/LiquidGolemGoals.java:387,406` | 200,20, ROUTE_BUDGET/DETOUR_BUDGET | AILiquidGoto:84-92 cited `:402` |
| M23 L | `golems/AlchemyGolemGoals.java:301,310` | 20,2, FAILED_GATHER_DELAY/SUCCESS_DELAY | AIEssentiaGather; local early-failure explanation `:296` |
| M24 L | `golems/AlchemyGolemGoals.java:349,366` | 200,20, ROUTE_BUDGET/NUDGE_BUDGET | AIEssentiaGoto; single nudge latch must stay distinct from Liquid |
| M25 L | `golems/UseGolemGoals.java:145-146` | 64,10000L, MAX_TIMEOUTS/IGNORE_DELAY_MS | AIUseItem identity timeout; existing shared route delay is same units but not necessarily same policy |
| M26 L | `golems/UseGolemGoals.java:236` | 200, USE_PATH_BUDGET | AIUseItem one-attempt approach |
| M27 L | `ai/pech/PechItemGoal.java:21,23,26` | 5,16,256, SCAN_INTERVAL/RANGE/RANGE_SQ | AIPechItemEntityGoto cited class comment |
| M28 L | `ai/pech/PechMeleeGoal.java:28-33` | 4,7,10,15,256,1024,.6,10 | AIAttackOnCollide retry, chase and attack constants; shared policy needs separate names |
| M29 L | `monster/EntityCultist.java:264-274` | 4,7,15,1024,10,256,5,10 | same legacy melee policy, currently independent copy |
| M30 L — FIXED | `ai/pech/PechRangedGoal.java:12-20,29-34` | 15,225,20(sight),20(interval),.6,30,.1F | **seven named constants**, cited Vanilla 1.7 EntityAIArrowAttack used by EntityPech; method bytecode identical |

These are 30 grouped sites, not 30 individual numeric literals. 29 sites remain open;
M30 is fixed. Existing good examples include GolemRouteRules/GolemLadderRules use throughout
the inventory ladder, GolemDartRules, LumberGeometry, CultistRules and EldritchMobRules.

## 6. Dead code — 324 private methods / 817 fields / 1,042 overrides screened

**0 confirmed findings; no dead code deleted.** Private non-constructor method names were
cross-referenced across common source/tests, including `::` method references. A call-only
regex falsely flags `monster/EntityCultistLeader.java:353` (`this::probeTarget`, `:343`)
and `golems/GolemHarvestPlatform.java:261` (`::chopEndpointEligible`, `:246`): both live.
Field reference/assignment candidates were checked; decrement-only lifetime/timer fields are
reads as well as writes. No private field was proven write-only. Scratch-state retention in
section 3 is not dead code. No obsolete superclass hook was proven unreachable.

All 1,042 override annotations were compiler-checked as part of the 1,445-source build.
That proves signatures, **not** that vanilla still invokes each hook in every relevant path.
Locally implemented goal lifecycle methods were followed through the selector contract;
full reachability of every combat/render/spawn hook requires runtime tracing, not an invented
"all overrides called" assertion. In particular, no deletion is justified merely because a
protected override has no textual mod caller.

## 7. Null safety — 909 calls screened

Whole surface: **93 `getTarget()`**, **161 `getNavigation()`**, **655 `level()`** calls
(excluding the two local getTarget declarations).
Within **123 tick/aiStep/customServerAiStep/serverTick/activeTick/idleTick/tickMotion/
tickFishingCast declarations**: **35 / 36 / 153**, respectively (**224 direct calls**).
Delegated decision/attack helpers were also followed for the goal tables; call-site totals
are syntactic, not runtime frequencies. A per-file inventory follows below.

Native `Mob.getTarget()` is nullable. Native navigation is constructed for a live Mob;
`level()` is a constructor-bound live Entity invariant. Adding repetitive null checks around
these latter two APIs would obscure ownership without fixing a supported lifecycle case.
Nullable *navigation results* (Path/end node/random position) generally have explicit checks,
e.g. `GatherGolemGoals.java:132-136,212-214`, `PechMeleeGoal.java:29`.

Tick paths use either captured nullable targets (`PechMeleeGoal.java:24`,
`PechRangedGoal.java:27`, `EntityCultist.java:257-258`, `EntityWatcher.java:233-234`),
validated current-job snapshots (Harvest/Lumber/Liquid/Alchemy), or guard repeated native
lookups on the server thread. Level-specific operations use instanceof/server entry points.
No null `level()` or `getNavigation()` tick dereference was established.

1. **N1 L — repeated nullable getter pattern is harder to reason about.**
   `monster/EntityTaintacle.java:64,89-106`, `monster/EntityEldritchWarden.java:115`,
   `golems/EntityTravelingTrunk.java:289` repeatedly fetch a target after a null check.
   No intervening concurrent mutation is proved; capture once in a local to expose the lifetime.
2. **N2 L — non-null implication is hidden in an effectful predicate.**
   `golems/GuardGolemGoals.java:103-105,136-138` dereferences target only after
   `targetPolicy` succeeds. This is currently safe because `isCombatTarget` rejects null,
   but `targetPolicy` can also stop the melee goal and clear entity state (`:46-51`).
   Split named validity/ownership checks or document the non-null postcondition; do not add
   redundant checks everywhere. GuardianAttack.start (`monster/EntityWatcher.java:226`)
   similarly relies on immediate canUse without snapshotting; tick itself guards null.

The two findings are reviewability concerns, **not two demonstrated NPEs**.

## 8. Family consistency — 8 comparisons

| Family/policy compared | Inspection result |
|---|---|
| Fill / Empty / Sorting | shared flags/counter/chest lifecycle; intentional four-entry Sorting; L1 |
| Guard / Butcher | separate target policy, shared melee/dart authority; no owner-retaliation mix-up found |
| Harvest / Lumber | consistent crack/path cleanup; exact repeated lifecycle bodies D18/D19 |
| Liquid / Alchemy | both distinguish actual cargo from route intent and identity-stop owned paths; cooldown/nudge semantics intentionally differ |
| Cow / Pig / Sheep / Chicken / Villager / Crawler | shared tainted-drop/XP helpers, but loot/equipment ordering repeated locally; C3 |
| Cultist leader / Eldritch boss base / Guardian | parallel restriction and boss concerns with incompatible home schemas; C2 |
| Ember / FrostShard / ExplosiveOrb / PechBlast | materially different reload validation / payload retention; C1 |
| Cultist melee / Pech melee / TaintAnimal melee | same stated ten-tick design, different modern tick contracts; C4 |

1. **C1 M — projectile reload-validation policy is inconsistent.**
   `projectile/EntityEmber.java:171-177` directly imports damage/duration/age/scatter, whereas
   `EntityFrostShard.java:276-287` finite-checks/clamps damage/frost/age/velocity and
   `EntityExplosiveOrb.java:114-119` finite-checks strength/clamps age. PechBlast `:117`
   explicitly forgets strength/duration/nightshade while retaining age (documented legacy
   semantics, not automatically a defect). Define a per-field native-save validation contract;
   do not normalize legacy quirks or paid effects without a reload test.
2. **C2 M — related mobs encode home restrictions differently.**
   `monster/EntityCultist.java:112-128`, `EntityCultistLeader.java:148-170`,
   `EntityEldritchWarden.java:88-102` use separate integer coordinates/radius;
   `EntityEldritchGuardian.java:146-152` uses a packed long and float radius, cast back to int.
   Preserve existing native keys but centralize/document shape/default handling. D22 covers
   duplicated implementation; this finding covers inconsistent persisted representation.
3. **C3 L — tainted-family drop ordering has no single explicit policy boundary.**
   `monster/EntityTaintCow.java:89-94`, `EntityTaintPig.java:90-95`,
   `EntityTaintSheep.java:162-167`, `EntityTaintChicken.java:107-112`,
   `EntityTaintVillager.java:120-128` publish custom drops, remember equipment, then call super;
   `EntityTaintacle.java:192-196` calls a dedicated native-equipment helper first. These may be
   faithful differences. Record ordering/RNG expectations in one family contract before any
   extraction; test cancellation/loot-gamerule behavior rather than treating super calls as uniform.
4. **C4 M — melee update cadence is inconsistent despite the same stated attack gate.**
   `ai/pech/PechMeleeGoal.java:22` and `monster/TaintAnimalMeleeGoal.java:60` explicitly
   require every-tick updates. `monster/EntityCultist.java:223-279` does not override that
   contract, yet decrements its ten-update attack timer in tick. Modern selector scheduling
   can therefore give a different real-time gate. This is not a dead override issue: add a
   cadence test against the cited original behavior before modifying it.

## Accepted change and remaining work

Changed only `ai/pech/PechRangedGoal.java`: seven private named policy constants,
no new public API, no control-flow changes, exact method-bytecode equality verified.
No reset, ladder, persistence, drop, target or scheduler behavior was changed.

Open review requests: **72 of 73 grouped findings** (many are low-priority duplication/name
requests, not defects). Prioritize G1, then path cleanup with interruption tests, watcher LOOK
ownership/cache invalidation, and cultist cadence. Do not merge large inheritance/codec
refactors merely to reduce line counts. No runtime acceptance is claimed.

### Per-file call-site inventory

T = getTarget; N = getNavigation; L = level. Counts are source call occurrences; files
with no such calls are still included to make the 95-file surface explicit.

| File | T | N | L |
|---|---:|---:|---:|
| `DeathAspectOrbDrop.java` | 0 | 0 | 1 |
| `EntityAspectOrb.java` | 0 | 0 | 22 |
| `EntityFallingTaint.java` | 0 | 0 | 5 |
| `EntityFollowingItem.java` | 1 | 0 | 2 |
| `EntityInventoryItem.java` | 0 | 0 | 0 |
| `EntityItemGrate.java` | 0 | 0 | 2 |
| `EntityPermanentItem.java` | 0 | 0 | 0 |
| `EntitySpecialItem.java` | 0 | 0 | 0 |
| `InventoryMob.java` | 0 | 0 | 0 |
| `InventoryPech.java` | 0 | 0 | 0 |
| `ItemSpawnerEgg.java` | 0 | 0 | 0 |
| `ai/combat/TaintCreeperSwell.java` | 2 | 1 | 0 |
| `ai/pech/PechAvoidGoal.java` | 0 | 4 | 1 |
| `ai/pech/PechItemGoal.java` | 0 | 5 | 4 |
| `ai/pech/PechMeleeGoal.java` | 3 | 6 | 0 |
| `ai/pech/PechRangedGoal.java` | 2 | 3 | 0 |
| `ai/pech/PechTradePlayer.java` | 0 | 1 | 0 |
| `ai/pech/PechWatchGoal.java` | 0 | 0 | 1 |
| `golems/AlchemyGolemGoals.java` | 0 | 11 | 16 |
| `golems/EntityGolemBase.java` | 1 | 21 | 132 |
| `golems/EntityGolemBobber.java` | 0 | 0 | 24 |
| `golems/EntityTravelingTrunk.java` | 4 | 0 | 19 |
| `golems/EnumGolemType.java` | 0 | 0 | 0 |
| `golems/FishingGolemGoals.java` | 0 | 2 | 6 |
| `golems/GatherGolemGoals.java` | 0 | 15 | 2 |
| `golems/GolemChestInteraction.java` | 0 | 0 | 0 |
| `golems/GolemCrops.java` | 0 | 0 | 0 |
| `golems/GolemDoorGoal.java` | 0 | 1 | 3 |
| `golems/GolemFishingLoot.java` | 0 | 0 | 0 |
| `golems/GolemFishingSmelting.java` | 0 | 0 | 0 |
| `golems/GolemHarvestPlatform.java` | 0 | 0 | 11 |
| `golems/GolemHelper.java` | 0 | 0 | 5 |
| `golems/GolemHomeInventory.java` | 0 | 0 | 0 |
| `golems/GolemReplant.java` | 0 | 0 | 0 |
| `golems/GolemUsePlatform.java` | 0 | 0 | 4 |
| `golems/GuardGolemGoals.java` | 25 | 12 | 5 |
| `golems/HarvestGolemGoals.java` | 0 | 5 | 7 |
| `golems/InventoryGolemGoals.java` | 0 | 17 | 2 |
| `golems/InventoryTrunk.java` | 0 | 0 | 0 |
| `golems/LiquidGolemGoals.java` | 0 | 12 | 6 |
| `golems/LumberGeometry.java` | 0 | 0 | 0 |
| `golems/LumberGolemGoals.java` | 0 | 4 | 7 |
| `golems/UseGolemGoals.java` | 0 | 6 | 4 |
| `monster/EntityBrainyZombie.java` | 0 | 0 | 0 |
| `monster/EntityCultist.java` | 5 | 9 | 6 |
| `monster/EntityCultistCleric.java` | 0 | 0 | 7 |
| `monster/EntityCultistKnight.java` | 0 | 0 | 1 |
| `monster/EntityCultistLeader.java` | 1 | 0 | 9 |
| `monster/EntityCultistPortal.java` | 0 | 0 | 26 |
| `monster/EntityCultistPortalGreater.java` | 0 | 0 | 0 |
| `monster/EntityEldritchCrab.java` | 1 | 0 | 1 |
| `monster/EntityEldritchGolem.java` | 2 | 0 | 37 |
| `monster/EntityEldritchGuardian.java` | 2 | 0 | 10 |
| `monster/EntityEldritchWarden.java` | 6 | 0 | 45 |
| `monster/EntityFireBat.java` | 7 | 3 | 14 |
| `monster/EntityGiantBrainyZombie.java` | 0 | 0 | 0 |
| `monster/EntityInhabitedZombie.java` | 0 | 0 | 4 |
| `monster/EntityMindSpider.java` | 0 | 0 | 1 |
| `monster/EntityPech.java` | 1 | 1 | 26 |
| `monster/EntityTaintChicken.java` | 0 | 1 | 1 |
| `monster/EntityTaintCow.java` | 0 | 1 | 1 |
| `monster/EntityTaintCrawler.java` | 0 | 1 | 3 |
| `monster/EntityTaintCreeper.java` | 1 | 0 | 3 |
| `monster/EntityTaintPig.java` | 0 | 1 | 1 |
| `monster/EntityTaintSeed.java` | 1 | 1 | 6 |
| `monster/EntityTaintSeedPrime.java` | 0 | 0 | 0 |
| `monster/EntityTaintSheep.java` | 0 | 3 | 5 |
| `monster/EntityTaintSpider.java` | 0 | 0 | 1 |
| `monster/EntityTaintSpore.java` | 0 | 0 | 19 |
| `monster/EntityTaintSporeSwarmer.java` | 0 | 0 | 7 |
| `monster/EntityTaintSwarm.java` | 7 | 3 | 16 |
| `monster/EntityTaintVillager.java` | 0 | 2 | 2 |
| `monster/EntityTaintacle.java` | 11 | 0 | 8 |
| `monster/EntityTaintacleGiant.java` | 0 | 0 | 4 |
| `monster/EntityTaintacleSmall.java` | 0 | 0 | 0 |
| `monster/EntityThaumicSlime.java` | 0 | 0 | 12 |
| `monster/EntityWatcher.java` | 6 | 3 | 6 |
| `monster/EntityWisp.java` | 0 | 0 | 15 |
| `monster/OriginalTaintCombat.java` | 0 | 0 | 0 |
| `monster/TaintAnimalMeleeGoal.java` | 3 | 6 | 2 |
| `monster/TaintDeathConversion.java` | 0 | 0 | 1 |
| `monster/TaintSwarmFlightRules.java` | 0 | 0 | 0 |
| `monster/TaintSwarmVisuals.java` | 0 | 0 | 4 |
| `projectile/EntityAlumentum.java` | 0 | 0 | 3 |
| `projectile/EntityBottleTaint.java` | 0 | 0 | 2 |
| `projectile/EntityDart.java` | 0 | 0 | 2 |
| `projectile/EntityEldritchOrb.java` | 0 | 0 | 4 |
| `projectile/EntityEmber.java` | 0 | 0 | 5 |
| `projectile/EntityExplosiveOrb.java` | 0 | 0 | 5 |
| `projectile/EntityFrostShard.java` | 0 | 0 | 10 |
| `projectile/EntityGolemOrb.java` | 1 | 0 | 3 |
| `projectile/EntityPechBlast.java` | 0 | 0 | 8 |
| `projectile/EntityPrimalArrow.java` | 0 | 0 | 3 |
| `projectile/EntityPrimalOrb.java` | 0 | 0 | 10 |
| `projectile/EntityShockOrb.java` | 0 | 0 | 5 |
