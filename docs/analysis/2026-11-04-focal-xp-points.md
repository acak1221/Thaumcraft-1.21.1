# Focal upgrades consume experience points

The owner requested point payment instead of level payment. Original TC4 and the
port both checked `rank * 8` levels and removed that many levels. Consequently the
same upgrade consumed more underlying experience when started at a higher level.

The focal manipulator now charges a fixed point price equal to reaching the old
threshold from zero under Minecraft 1.21.1's experience curve:

| Upgrade rank | Original level price | Fixed XP points |
| --- | ---: | ---: |
| 1 | 8 | 112 |
| 2 | 16 | 352 |
| 3 | 24 | 828 |
| 4 | 32 | 1628 |
| 5 | 40 | 2920 |

The GUI and the authoritative server use the same price. The displayed value and
hover text identify point units and show the available balance. An insufficient
balance disables the GUI action; the server checks it again after extension queries
and immediately before payment. Session/revision authorization and vis costs are
preserved. Creative retains the original affordability requirement and debit exemption.

Spendable experience is reconstructed from the level and rounded current-bar
points. `totalExperience` is not a currency balance: vanilla level debits and
previous saves can leave it inconsistent with the current bar. Payment uses native
`giveExperiencePoints(-price)`, which handles level transitions, partial bars and
the historical total counter. `ServerPlayer` unconditionally dirties its XP packet
state after this native call, including a historical counter that stays at zero.
The conversion uses bounded long arithmetic, including native float-boundary errors.

No persisted focal job format changes. Already paid jobs resume without another XP
charge; cancellation does not refund the upfront payment. The legacy planner's
`xpLevels()` remains a nominal compatibility query; payment uses `xpPoints()`.

The focused server checks cover exact/one-point-short thresholds, the same 112-point
debit at low/high levels with partial bars, a stale total counter, creative behavior,
replay rejection, cancellation, vis processing, reload and durable drops. A separate
native client probe uses actual GUI input and START/ACK packets on a fresh survival
world with explicitly supplied research, focus and point fixtures. No vis is supplied
there: its jobs remain pending vis, and the probe does not claim free completion or
survival acquisition. The long historical focal progression assertions were adapted
to points, but that full saved-world progression is not replayed for this change.

The focal GUI resource supplier contract was reviewed: its two texture families
are unchanged; only the price/affordability text and tooltip changed. Resource
verification reports no missing references. Point-price text is bounded to the
original frame; full-size hover text identifies the price and available balance.
NeoForge validation remains deferred under the repository's Fabric-first instructions.


## Verified result

- `:common:test :fabric:clientTestClasses :fabric:build`: BUILD SUCCESSFUL after final source changes; 2,912 JUnit cases, 2,907 passed, five existing oracle skips, zero failures/errors. Production/test isolation passed.
- Focused native dedicated server: all five required focal tests passed (`focalRealCharmAndManipulatorPayments`, `focalAdmissionAndCancellation`, `focalSourceDebitsAndReload`, `focalPostDebitFences`, `focalDurableDropRefusal`).
- `python3 -B scripts/porting/convert_assets.py --verify`: zero missing references, 52 composed sites and 294 composed files checked.
- `:fabric:runFocalXpClient`: BUILD SUCCESSFUL, final run `focal-xp-2026-11-03T21-17-15.687257195Z`, PASS at stage 19. Actual GUI admission preserves 111 points on refusal; 1412 → 1300 and 129 → 17 each pay exactly 112 points. Client/server native XP gauges match. Duplicate click, replay and physical pickup cancellation cause no extra XP change. Rank-five preview quotes 2920 with 2937 points available.
- All six final native RU frames were visually inspected by the root and client reviewer. Price availability, disabled/enabled START, remaining vis, native level changes and cost/balance tooltips are visible. Actual render mouse coordinates `[134, 65]` and an active window are recorded for each capture.

An earlier payment-only client run completed without visible hover text and is not
the visual acceptance. A subsequent actual-pointer guard timed out at stage 2,
before any capture. That attempt is declined; focusing the window and issuing a
fresh native cursor movement produced the final accepted run above. Transient logs
and worlds remain in ignored run/`.tmp` directories.

Exact balances, frame hashes, declared fixtures and artifact checksum are in the
[receipt](../porting/focal-xp-points.json). Final Fabric JAR:
`Thaumcraft-fabric-0.1.0-alpha.24.jar`, 16024220 bytes,
SHA-256 `70283b1cd797afff4f4683445545350ae9b727e580f13d26883a01afcad5b85b`.

[Insufficient balance](../images/focal-xp-points/rank1-112-points-disabled-at111.png) ·
[Level 30, paid 112](../images/focal-xp-points/level30-paid112-pending-vis-job.png) ·
[Level 8, paid 112](../images/focal-xp-points/level8-paid112-leaves17-points.png) ·
[Rank-five price](../images/focal-xp-points/rank5-2920-point-price-and-tooltip.png).
