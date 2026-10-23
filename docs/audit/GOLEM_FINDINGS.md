# Golem end-to-end audit — g21golem, 2026-10-23

Scope: the single hypothesis that a core starts but fails to finish real work, or performs forbidden work. Fabric 1.21.1 only, fresh GameTest worlds, Java 21, branch `agent/g21golem`, starting commit `9ef54a43`. No client, multiplayer, NeoForge or third-party protection acceptance is claimed.

## Contract corrections (not defects)

Original-contract notes `docs/analysis/2026-10-19-golem-harvest-runtime-notes.md` (oracle AIHarvestCrops lines 151–154,185–193,248–249) and `2026-10-19-golem-butcher.md` distinguish work from collection: ordinary Harvest and Lumber leave world drops. A separate Gather worker is needed for collection. The new test therefore requires actual drops to reach an actual chest through a real Gather, rather than inventing cargo in the cutters.

`docs/analysis/2026-10-19-golem-upgrades.md` records Earth increasing melee damage, Fire igniting victims (not increasing initial melee damage), and Entropy queuing connected liquids (not a generic item-job queue). Tests follow those actual contracts.

## Coverage inventory

**12 cores + 6 upgrades = 18 entries inspected end to end.** This includes all five requested upgrades plus Order. One core (Use) had a reproduced gameplay failure, now fixed; no upgrade failure was established. Final-source `.tmp-g21-final3.log` passed **15/15 focused GameTests**, Common JUnit (**2509 tests**, zero failures/errors/skips) and **Fabric build**, including production/test isolation checks. The prior complete run also passed 15/15; see the separate earlier timeout limitation below.

| Entry | Actual scenario / test | Result | Fix |
|---|---|---|---|
| Fill | `g21CarryAndRefusals`: real chest → carry → home chest; 40 requested from 64 | PASS: exactly 40 delivered, 24 remain; no excess/loss; full/unlinked/wrong-filter/sided negatives | None |
| Empty | `entropySwitchesChangeWhatRealGolemsFetchEmptyAndSort` | PASS: real home emptied into marked chest; conservation and negative controls | None |
| Sorting | Same existing test | PASS: actual stack reaches seeded chest; source emptied, seed retained, strict negative idle | None |
| Gather | `g21GatherAndAir` | PASS: walk >8 blocks, pick eight loose items, deposit all eight in chest | None |
| Harvest | `g21HarvestAndLumberDelivery` | PASS: melon broken; real Gather delivers actual slices; final fixture separates production/collection | None |
| Lumber | Same new test | PASS: three-log trunk removed; Gather deposits exactly three logs; final fixture separates production/collection | None |
| Fishing | `realFishingGolemsCastIntoPondsAndDeliverCooksAndExtraCatches` | PASS: native pond casting, actual catches and real delivery, including upgraded variants | None |
| Guard | `orderGuardTargetSwitchesChooseWhomRealGuardsFight` | PASS: real target acquisition/walking/striking/deaths; owner and excluded bystanders spared | None |
| Butcher | `g21ButcherLeavesBreedingPair` | PASS: oldest of three pigs killed; younger pair, single cow and connected survival owner unharmed through 1000 ticks | None |
| Use | `g21UsePlantsExactlyOneSeed`, `g21UseEmptySpacePlantsExactlyOneSeed` | **FAIL → PASS**: actual wheat, exactly one of eight real seeds paid, walking and repeat-action window asserted in both block/empty-space modes | Face-centered sight, `8fa5b24d` |
| Decanting | `entropyQueuesTheFarthestConnectedWaterForLiquidGolems` | PASS: real sources drained into crucibles; queue order, amount, exhaustion and no-Entropy control checked | None |
| Alchemy | `alchemyGolemsCollectAndDeliverMixedReservoirs` | PASS: actual mixed-reservoir collection/delivery; no recent-fluid regression observed in this scenario | None |
| Earth | `g21CarryAndRefusals` | PASS: **actual peak held stacks 8 → 16**, not a capacity getter; both deliver exactly 40 | None |
| Water | `g21MeasuredWaterPickupRadius`; existing running Lumber/Harvest range test | PASS: actual pickup/delivery at **15.9 plain / 19.9 Water**; **16.1 plain / 20.1 Water remain untouched for 800 ticks**. Running Lumber reaches trunk at 7 and Harvest fruit at 5 only after upgrade | None |
| Entropy | Existing connected-water queue test above | PASS: real farthest-connected-first drain behavior differs from marker-only control | None |
| Air | `g21GatherAndAir` | PASS: actual 12-block pickup times **54 plain / 43 Air ticks** in final run (53/42 in preceding run); both return eight items | None |
| Fire | `fireIgnitesGuardTargetsAndOrderReplantsHarvestedCrops`; live Fishing delivery | PASS: struck real spider has >40 and ≤80 fire ticks with Fire, none without; actual cooked catches delivered | None |
| Order | Same Fire/Order test and Guard target test | PASS: real crop replanted with actual seed payment/protection refusal; Guard switches change actual victims | None |

## Refusals and limits

New Fill negatives monitor each server tick for conservation, never overwrite a full chest, never fetch unlinked/wrong-filter goods, and never extract furnace input through its output-only bottom face. Existing `golemFetterHoldsGolemsWhileItsRedstoneSignalLasts` passed with real workers under active redstone fetters and after release. These are finite scenarios, not a proof of every inventory provider or every core under every fetter transition.

## PROVEN defects

**One: Use refuses a legitimate floor-level planting job.** Fresh focused run `g21UsePlantsExactlyOneSeed`, `.tmp-g21-use-red.log`, failed at tick 400: target air, unchanged farmland, chest zero seeds, golem still carrying all eight, `canWork=true`. Red regression committed **before** gameplay changes as `e62b7482`.

Fresh diagnostics independently confirmed a native-valid, empty survival machine actor. At actual click reach the center-aimed sight ray hit the **preceding stone floor**, while the face-aimed ray on the identical world was unobstructed (`.tmp-g21-use-sight-red.log`). Both entity preflight and borrowed-actor preflight made the same erroneous center check.

Fix `8fa5b24d`: share the actual clicked-face sight endpoint between `UseGolemGoals`, `EntityGolemBase.useEyesOn` and `GolemUsePlatform.sight`; check the endpoint's finite chunk footprint; accept the actual clicked block in empty-space mode. Earlier solids still deny sight. Reach, target/current-state/protection checks, actual-item ownership and exactly-one native attempt are unchanged. Focused run `.tmp-g21-use-sight-fix.log` passed, followed by both Use modes in the full 15-test run. The unsuccessful navigation-continuation candidate was removed.

## Additional observed failure, NOT a proven production defect

The first 15-case run (`.tmp-g21-final.log`) had one **unattributed 1500-tick Harvest/Lumber-plus-Gather deadline**, while the other 14 cases passed. No pre-deadline block/drop diagnostic was present, so this does not identify which producer/collector failed or why. A fresh focused rerun and a fresh complete 15-case rerun passed without a farming production change. Do not relabel that first run as accepted or call a concurrency bug fixed.

The final farming fixture starts the real Gather workers only after observing the real native melon/trunk removal, separating production from collection and eliminating simultaneous path competition as a test confound. It still supplies no drops, harvest, movement or delivery. This validates the real work/collection pipeline, **not concurrent cutter/collector liveness**.

## Reasoned-only / not covered

- Universal protection-mod/container-provider compatibility.
- Every crop/tree/animal/item and every Use option; test scenarios use native, explicitly supported resources.
- Concurrent cutter/collector liveness: one earlier mixed-fixture timeout is unclassified, not a second PROVEN defect or a claimed fix.
- Every core/upgrade combination and every core's individual fetter transition; the covered negative scenarios are listed above.
- Literal Harvest/Lumber self-collection is not an original behavior and is not added.
- Fire as an initial melee-damage multiplier and Entropy as a generic job queue are not original behaviors.
- Client visuals, multiplayer/network, NeoForge and survival acquisition.
