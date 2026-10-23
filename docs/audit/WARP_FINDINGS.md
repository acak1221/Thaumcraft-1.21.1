# G27 warp hypothesis audit

Date: 2026-10-23. Branch: `agent/g27warp`. Scope: accumulation, event dispatch and warp persistence, Fabric 1.21.1.

## Outcome and counts

**PROVEN: 30 source rows and 15 rule groups (45 top-level audit units), including 23 effect-tier subcases.** Eleven additional inspected paths/limitations remain reasoned-only or partially covered, explicitly below: 56 top-level inspected units total. Subcases are not double-counted as independent rules.

Three defects produced **30 mismatched source/API checks**: 28 built-in API lookups returned0 instead of their configured1–6 despite correct native awards; a registered craft amount7 awarded0; an overridden research amount7 awardedP1/N1 instead ofP4/N3. All 30 checks now match. None of the 28 built-in award amounts/channels, decay, counter arithmetic, dispatch representatives, relog or death persistence required a gameplay change.

Fix commits: `9040d48e` (original registry defaults), `5f5d566a` (craft registry consumer), `ee9d5aff` (live research award/warning consumers). Failing-test harness: `6bd30c40`.

## Specification and evidence boundaries

The specification is the private decompilation at `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`, read only. In particular: `api/ThaumcraftApi.java:277–295`, `common/config/ConfigResearch.java:81–415`, `common/lib/events/EventHandlerWorld.java:150–154`, `common/lib/research/ResearchManager.java:510–524`, `common/lib/WarpEvents.java:67–234`, `common/lib/events/EventHandlerEntity.java:288–289`, and `common/lib/research/PlayerKnowledge.java:138–210`. No OpenTC4 substitution.

`WarpRuntimeChecks.audit` uses a real `ServerPlayer`, `PlayerList.placeNewPlayer`, an embedded network connection, and registered crafting callbacks. Recipes, solved notes and prerequisites are declared inputs, **not survival acquisition replay**. The research-source sweep isolates one privileged research completion at a time, with all other researches already completed. The decay sweep explicitly drives player age through 40,000 calls of the production tick dispatcher; it is **not** a 40,000-wall-clock-tick server run.

`PROVEN` below means executed native assertions, not visual client acceptance. Both green runs passed all five focused GameTests; the first also passed 352 research/warp/API JUnit tests. The final run strengthened the20-research sweep to real authorized commands and verified the actual encoded outbound state snapshot.

## Sources (30 PROVEN rows)

P = permanent, N = normal/sticky, T = temporary. C = `g27WarpOriginalSources` crafting listener; R = the same test's isolated research completion. A = `g27WarpRegisteredCraft`; B = `g27WarpRegisteredResearch` physical solved-note use.

| # | Source | Oracle award | Native award before → after | Test | Fix |
|---|---|---|---|---|---|
| 1 | focus_hellbat craft | N1 | N1→N1; API0→1 | C | expose default |
| 2 | compass_stone craft | N1 | N1→N1; API0→1 | C | expose default |
| 3 | jar_brain craft | N1 | N1→N1; API0→1 | C | expose default |
| 4 | bucket_death craft | N1 | N1→N1; API0→1 | C | expose default |
| 5 | bottle_taint craft | N1 | N1→N1; API0→1 | C | expose default |
| 6 | golem_flesh craft | N1 | N1→N1; API0→1 | C | expose default |
| 7 | focus_primal craft | N1 | N1→N1; API0→1 | C | expose default |
| 8 | staff_rod_primal craft | N1 | N1→N1; API0→1 | C | expose default |
| 9 | FOCUSHELLBAT research | P1 N1 | P1 N1→same; API0→2 | R | expose default |
| 10 | ROD_bone | P1 | P1→P1; API0→1 | R | expose default |
| 11 | ROD_bone_staff | P1 | P1→P1; API0→1 | R | expose default |
| 12 | SINSTONE | P1 N1 | P1 N1→same; API0→2 | R | expose default |
| 13 | INFERNALFURNACE | P1 N1 | P1 N1→same; API0→2 | R | expose default |
| 14 | JARBRAIN | P2 N1 | P2 N1→same; API0→3 | R | expose default |
| 15 | MASKANGRYGHOST | P1 | P1→P1; API0→1 | R | expose default |
| 16 | MASKSIPPINGFIEND | P1 | P1→P1; API0→1 | R | expose default |
| 17 | LIQUIDDEATH | P2 N1 | P2 N1→same; API0→3 | R | expose default |
| 18 | BOTTLETAINT | P1 N1 | P1 N1→same; API0→2 | R | expose default |
| 19 | GOLEMFLESH | P2 N1 | P2 N1→same; API0→3 | R | expose default |
| 20 | COREBUTCHER | P1 | P1→P1; API0→1 | R | expose default |
| 21 | ADVANCEDGOLEM | P3 N2 | P3 N2→same; API0→5 | R | expose default |
| 22 | RESEARCHER2 | P1 | P1→P1; API0→1 | R | expose default |
| 23 | CRIMSON | P2 N1 | P2 N1→same; API0→3 | R | expose default |
| 24 | OCULUS | P3 N3 | P3 N3→same; API0→6 | R | expose default |
| 25 | PRIMNODE | P1 | P1→P1; API0→1 | R | expose default |
| 26 | CAP_void | P1 | P1→P1; API0→1 | R | expose default |
| 27 | FOCUSPRIMAL | P1 N1 | P1 N1→same; API0→2 | R | expose default |
| 28 | ROD_primal_staff | P2 N1 | P2 N1→same; API0→3 | R | expose default |
| 29 | API diamond output, amount7, count4 | N7 | N0→N7 | A | crafting uses registry |
| 30 | API FOCUSHELLBAT override7, solved note | P4 N3 | P1 N1→P4 N3 | B | live research resolution |

The API lookup is separately measured for every one of rows 1–28; matching built-in awards alone do not establish registry parity.

## Rules and limitations

The original has an effect-roll ladder, not a deterministic total-warp-to-named-tier mapping. It rolls `nextInt(min(100,(2*totalIncludingGear+counter)/3))` after the `roll100 <= sqrt(counter)` gate. Named effects therefore overlap possible total warp values.

| Rule | Oracle | Native measurement/test | Status |
|---|---|---|---|
| Temporary decay | −1 per unwarded, non-wuss age multiple2000, even if roll rejected | age0/2000/12000/24000/40000 → T12/11/6/0/0 | PROVEN |
| Temporary lower bound | zero, never negative | Same sweep beyond age24000 | PROVEN |
| Permanent stability | no passive decay | P7 over same sweep | PROVEN |
| Normal stability | no passive decay (clarity/soap are explicit reductions) | N5 over same sweep | PROVEN |
| Counter cap | no100 cap on stored counter; event effective warp cap100 | counter10000, roll99, resulting counter9800 | PROVEN |
| Effect tier | effective100, draw53 → Death Gaze duration6000 amplifier3 | Native effect instance | PROVEN |
| Event cadence | no repeat between scheduled checks | 1999 intervening dispatcher calls preserve counter/temp | PROVEN |
| Player NBT | all three pools and counter | Native player save/load | PROVEN |
| Relog | all three pools and counter | `PlayerList.remove` and fresh connected `placeNewPlayer` | PROVEN |
| Death | all three pools and counter | Native lethal damage and `PlayerList.respawn` | PROVEN |
| Server→client state | sync carries all three pools and counter | Decode actual outbound snapshot and compare server fields | PROVEN |
| Research unlock thresholds | actual=P+N; strictly >10/>25/>50; temporary excluded | Regression probes10/11,25/26,50/51 and T100-only | PROVEN |
| Suppression | ward/wuss skips scheduled check and decay; wuss skips craft award | Actual ward effect and wuss flag, no pool/counter change | PROVEN |
| Gear/mask inputs | gear affects event gate, not actual-warp research; devil mask subtracts2+draw4 | Native robe opens gate; severity32→27 with mask draw3 | PROVEN |
| Ordinary tick scheduling | one event at age2000, not once per tick | Connected player crosses1999→2000 in ordinary server connection loop; T150→149, counter10000→9800 | PROVEN |

Additional original dispatcher regression: `warpDispatchesOriginalBranchesWithNativeEffects` executes the effect branches (including the intentional gaps), strict research thresholds >10, >25, >50, ward/wuss suppression and a genuine ordinary-server age1999→2000 scheduling boundary. This is additional coverage, not extra source rows.

### Effect-tier subcases (23, within the effect-tier rule group above)

All execute against the real connected player at effective warp100. These are selected representative draws, **not** an exhaustive boundary-integer matrix. Every listed native result equaled the oracle; no effect-dispatch fix was needed.

| Declared draw(s) | Oracle result | Measured native result | Test / status |
|---|---|---|---|
| 0,5,85 | no extra effect/reward | none | dispatcher / PROVEN |
| 1 | one primal point under declared reward draws | one | dispatcher / PROVEN |
| 9 | noise/message only | no status/reward | dispatcher / PROVEN |
| 13 | Vis Exhaust5000, amplifier3, uncurable | exact | dispatcher / PROVEN |
| 17 | Thaumarhia1000, amplifier0, uncurable | exact | dispatcher / PROVEN |
| 21 | Hunger5000, amplifier3, uncurable | exact | dispatcher / PROVEN |
| 25 | following/message only | no status | dispatcher / PROVEN |
| 29 | one guardian | one targeting player | dispatcher / PROVEN |
| 33 | Blurred Vision1000, amplifier0 | exact | dispatcher / PROVEN |
| 37 | Sun Scorned5000, amplifier3, uncurable | exact | dispatcher / PROVEN |
| 41 | Mining Fatigue1200, amplifier3 | exact | dispatcher / PROVEN |
| 45 | Infectious Exhaust6000, amplifier3, uncurable | exact | dispatcher / PROVEN |
| 49 | Night Vision4000, amplifier0 | exact | dispatcher / PROVEN |
| 53 | Death Gaze6000, amplifier3, uncurable | exact | dispatcher / PROVEN |
| 57 | 50 harmless viewer-scoped spiders | 50 | dispatcher / PROVEN |
| 61 | watching/message only | no status | dispatcher / PROVEN |
| 65 | three guardians | three targeting player | dispatcher / PROVEN |
| 69 | Blindness500, amplifier0 | exact | dispatcher / PROVEN |
| 73,75,77,80 | Hunger6000, amplifier3 | exact | dispatcher / PROVEN |
| 76 | clarity reduces N by1; check decays T by1 | N2→1, T148→147 | dispatcher / PROVEN |
| 81 | primal reward, declared draws choose one | one | dispatcher / PROVEN |
| 89 | 50 real hostile spiders | 50 | dispatcher / PROVEN |
| 93 | six guardians | six targeting player | dispatcher / PROVEN |

## Reasoned-only / deliberately not claimed

| Inspected path/rule | Oracle | Native status in this lane |
|---|---|---|
| Brain food | 10% N1, otherwise T1–3 | Read, not measured here |
| Eldritch trap | 50% T1–2 to nearby player on trap activation | Read, not measured here |
| Infusion hazard | N1 or T1–5 | Read, not measured here |
| Warped champion hit | T1–3 | Read, not measured here |
| Guardian sonic attack | T1–3 | Existing dispatcher regression checks range; no exhaustive per-draw source sweep |
| Warden attack | T3–5 | Read, not measured here |
| Soap | explicit N−1 roll and clears T | Read, not measured here |
| Scanning / arbitrary tainted ingredient / merely using a dark block | No universal such addition found in oracle warp award call sites | Not invented as a source; individual scan/block acquisition paths not replayed |
| HUD | Native delta notifications and knowledge sync, not a numerical warp meter in the inspected client paths | No rendered client inspection; no visual parity claim |
| Research override consumers beyond solved-note use | Shared research value consumers | Follow-up behavioral acceptance beyond the directly exercised path |
| Gear, masks, all threshold boundary rolls | Original dispatcher/gear rules | Existing tests are regression coverage only; no new exhaustive boundary matrix in this lane |

The eleven rows above are **not full source/path-parity proofs**. Neither real socket-client HUD inspection nor full survival acquisition was performed. A green focused server suite is not complete client or gameplay acceptance.

## Reproduction and retained evidence

Use Java21 and `GRADLE_USER_HOME=/home/cyberfox-kali/wt/g27warp/.gradle-g27`; remove only this lane's `fabric/run-gametest/world` before every invocation.

```sh
./gradlew :common:test --tests '*research*' --tests '*warp*' --tests '*ThaumcraftApi*' :fabric:runGameTest -PgameTestFilter=g27WarpOriginalSources,g27WarpRegisteredCraft,g27WarpRegisteredResearch,g27WarpDecayCapAndPersistence,warpDispatchesOriginalBranchesWithNativeEffects --no-daemon --max-workers=2
```

The final run repeated the same five GameTests after test-only command-path and outbound-snapshot strengthening. Production code was unchanged after the 352-unit-test pass. Final server stopped normally and saved every dimension.

| Local evidence (ignored build/run logs, not committed) | Result | SHA-256 |
|---|---|---|
| `.tmp/g27-red-cases.log` | Three real defect failures; fourth was whole-attachment fixture overreach, corrected to four warp fields | `fc11708bf01a6f6dcdddf7b3b10fb7bda9240f068ec45e39ca353ef2131e414e` |
| `.tmp/g27-green.log` | 352 JUnit +5 GameTests, zero failures | `bbfa0d600f1c1da1ab499d5aab878f8bd55cccca0956984af24cf4532bdc9a43` |
| `.tmp/g27-final.log` | Final strengthened5/5, BUILD SUCCESSFUL | `c045aa0fa5ac0c93484a02c41fe3959a09869f267f17cd45f074f4ac48161482` |
| `fabric/build/gametest-focused-results.xml` | Five passing cases | `96fec5e1793d2fe56473796fb3941c60a23e73eb65ca2c727b4fe9468aeb6888` |
