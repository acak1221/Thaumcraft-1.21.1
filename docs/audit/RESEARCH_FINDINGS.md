# Research table balance audit — g32table

Scope: one hypothesis: aspect combination misprices output, or note creation/copy loses materials. Fabric 1.21.1, real placed two-block table, connected GameTest ServerPlayer, real inventory stacks and production transaction receivers. Items/knowledge are supplied fixtures, not survival acquisition. No client or C2S transport claim.

## Result

**66 driven rules, 66 PROVEN within the stated fixtures, 0 mismatched, 0 gameplay fixes.** The final expanded Fabric run passed all7 GameTests, including all42 compound vectors (126 transactions) and one actual issuance→solve→copy→learn lifecycle. No production defect reproduced; no gameplay fix justified. Tests are written before any proposed fix; no artificial failing assertion or mutation is represented as a discovered bug.

Important premise correction: the oracle has **no per-note initial bonus allowance**. `TileResearchTable:64–66` starts an empty table-owned pool; ecology replenishes it. Removing/replacing notes must not reset it.

## Specification and method

Read-only original: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/`.

- `PacketAspectCombinationToServer:97–123`: pay personal RP first, then shared bonus; no recipe means paid failure, not refusal.
- `ScanManager:420–440`: base award1, first-discovery bonus2.
- `TileResearchTable:64–66,115–122,174–290,398–420`: empty bonus pool, first ecology recalculation on tick602, nonstacking bonus, copy prices.
- `ResearchManager:120–135`: creation consumes paper1 then ink1; an existing note is reused.
- `ItemResearchNotes:87–101`: learning requires prerequisites, consumes one note, awards once to reader.
- `ResearchManager:510–527`: first completion alone splits warp; JARBRAIN warp3 is permanent2/sticky1.
- Independent literal parent vectors already recorded in `common/src/test/resources/thaumcraft/aspect_research_oracle.json`; runtime vectors do not ask the native recipe resolver for expected output.

Tests below are methods in `fabric/.../ThaumcraftGameTests.java` (implementation under `common/src/gameTest/java/thaumcraft/common/testing/`):

| Alias | GameTest |
|---|---|
| A | `researchAspectsCombineAndBonusesRegenerate` |
| C | `researchTableCopiesDiscoveriesWithExactCosts` |
| E | `researchTableEditsSolveAndPreserveResources` |
| I | `primaryRequestConsumesPaperAndLastInk` |
| F | `primaryRequestRefusalsAreAtomic` |
| K | `solvedNoteLearnsOnceAndTransfers` |
| W | `noteRefusalsAndWarpUseOriginalRules` |

## Driven oracle rules

Counting convention: one compound family is one rule, with three transactions per family. Other rows count distinct workflow/resource rules, not individual assertions or inventory slots. **66 rules** below, all **PROVEN** by the focused run (not the reasoned-only items below). All measurements read actual knowledge, table state, or inventory; no test calls the planning helper to compute expected prices. A drives **131 paid experiments** total:126 vector transactions,4 other valid combinations and1 invalid pair.

For rows1–42, each run resets inputs to personal2/2 and bonus1/1. Oracle output totals are **3 → 4 → 5**, personal inputs **1/1 → 0/0 → 0/0**, bonuses **1/1 → 1/1 → 0/0**. Second transaction reverses the inputs. `3/4/5` in the native column means this entire asserted sequence, including exact input debits and unchanged player inventory. Fix `—` means no mismatch and no production change.

| # | Oracle pair → output | Oracle totals | Native totals | Test | Fix |
|---|---|---|---|---|---|
| 1 | aer + perditio → vacuos | 3/4/5 | 3/4/5 | A | — |
| 2 | aer + ignis → lux | 3/4/5 | 3/4/5 | A | — |
| 3 | aer + aqua → tempestas | 3/4/5 | 3/4/5 | A | — |
| 4 | aer + ordo → motus | 3/4/5 | 3/4/5 | A | — |
| 5 | ignis + perditio → gelum | 3/4/5 | 3/4/5 | A | — |
| 6 | terra + ordo → vitreus | 3/4/5 | 3/4/5 | A | — |
| 7 | aqua + terra → victus | 3/4/5 | 3/4/5 | A | — |
| 8 | aqua + perditio → venenum | 3/4/5 | 3/4/5 | A | — |
| 9 | ordo + ignis → potentia | 3/4/5 | 3/4/5 | A | — |
| 10 | perditio + ordo → permutatio | 3/4/5 | 3/4/5 | A | — |
| 11 | terra + vitreus → metallum | 3/4/5 | 3/4/5 | A | — |
| 12 | victus + perditio → mortuus | 3/4/5 | 3/4/5 | A | — |
| 13 | aer + motus → volatus | 3/4/5 | 3/4/5 | A | — |
| 14 | vacuos + lux → tenebrae | 3/4/5 | 3/4/5 | A | — |
| 15 | victus + mortuus → spiritus | 3/4/5 | 3/4/5 | A | — |
| 16 | victus + ordo → sano | 3/4/5 | 3/4/5 | A | — |
| 17 | motus + terra → iter | 3/4/5 | 3/4/5 | A | — |
| 18 | vacuos + tenebrae → alienis | 3/4/5 | 3/4/5 | A | — |
| 19 | vacuos + potentia → praecantatio | 3/4/5 | 3/4/5 | A | — |
| 20 | praecantatio + aer → auram | 3/4/5 | 3/4/5 | A | — |
| 21 | praecantatio + perditio → vitium | 3/4/5 | 3/4/5 | A | — |
| 22 | victus + aqua → limus | 3/4/5 | 3/4/5 | A | — |
| 23 | victus + terra → herba | 3/4/5 | 3/4/5 | A | — |
| 24 | aer + herba → arbor | 3/4/5 | 3/4/5 | A | — |
| 25 | motus + victus → bestia | 3/4/5 | 3/4/5 | A | — |
| 26 | mortuus + bestia → corpus | 3/4/5 | 3/4/5 | A | — |
| 27 | motus + mortuus → exanimis | 3/4/5 | 3/4/5 | A | — |
| 28 | ignis + spiritus → cognitio | 3/4/5 | 3/4/5 | A | — |
| 29 | aer + spiritus → sensus | 3/4/5 | 3/4/5 | A | — |
| 30 | bestia + cognitio → humanus | 3/4/5 | 3/4/5 | A | — |
| 31 | herba + humanus → messis | 3/4/5 | 3/4/5 | A | — |
| 32 | humanus + terra → perfodio | 3/4/5 | 3/4/5 | A | — |
| 33 | humanus + ordo → instrumentum | 3/4/5 | 3/4/5 | A | — |
| 34 | messis + instrumentum → meto | 3/4/5 | 3/4/5 | A | — |
| 35 | instrumentum + ignis → telum | 3/4/5 | 3/4/5 | A | — |
| 36 | instrumentum + terra → tutamen | 3/4/5 | 3/4/5 | A | — |
| 37 | victus + vacuos → fames | 3/4/5 | 3/4/5 | A | — |
| 38 | humanus + fames → lucrum | 3/4/5 | 3/4/5 | A | — |
| 39 | humanus + instrumentum → fabrico | 3/4/5 | 3/4/5 | A | — |
| 40 | instrumentum + bestia → pannus | 3/4/5 | 3/4/5 | A | — |
| 41 | motus + instrumentum → machina | 3/4/5 | 3/4/5 | A | — |
| 42 | motus + perditio → vinculum | 3/4/5 | 3/4/5 | A | — |
| 43 | Invalid aer+aer paid experiment | personal1+bonus1 consumed, output0 | 0/0 remaining; lux unchanged4 | A | — |
| 44 | Unfunded aer+aer | no partial debit | personal1 retained; whole state unchanged | A | — |
| 45 | RP before bonus, even creative/Researcher2 | personal−1 each, bonus unchanged | personal2→1 each, bonus1 each | A | — |
| 46 | Initial table bonus | 0 | empty | A | — |
| 47 | Ecology timing and first dirt bonus | 601 ticks no refill; tick602 terra1 | 0 then1, revision+1 | A | — |
| 48 | Bonus ownership across note insert/remove | table pool retained | all48 aspect entries retained | A | — |
| 49 | Recalculation does not stack existing bonuses | max(existing,1) | all48 remain1 | A | — |
| 50 | Bonus persistence | produced pool retained | equal after native BE NBT load | A | — |
| 51 | Writing note | paper−1, ink−1; exhausted tool retained | paper3→2, damage99→100, tool count1 | I | — |
| 52 | Reuse existing note | no new note or material debit | all inventory and knowledge unchanged | I | — |
| 53 | Missing paper when writing | refuse, ink−0 | complete inventory/knowledge unchanged | F | — |
| 54 | Missing usable ink when writing | refuse, paper−0 | complete inventory/knowledge unchanged | F | — |
| 55 | First NITOR copy | paper1+dye1+lux3+ignis1; count+1 | supplies2→1 each, pools1000→997/999; count1→2, copies0→1 | C | — |
| 56 | Second copy uses OLD copy counter | paper1+dye1+lux4+ignis2 | supplies1→0 each, pools997/999→993/997; count2→3, copies1→2 | C | — |
| 57 | Missing copy paper | refuse, consume0 | whole inventory/table/knowledge unchanged | C | — |
| 58 | Missing copy black dye | refuse, consume0 | whole inventory/table/knowledge unchanged | C | — |
| 59 | Failed operations preserve shelf/slots | neither slot consumed | whole table NBT unchanged on refusals | A/C/E | — |
| 60 | Copy uses loose dye, not table tool/bonus | ink damage+0; shared bonus−0 | exhausted tool damage100 retained; bonus99/99 retained | C | — |
| 61 | One actual issued-note lifecycle | issue paper1/ink1; 4 edits ink4/RP4; copy paper1/dye1/lux3/ignis1; learn note1 | paper3→2→1; ink90→91→95→95; RP10/10→8/8→5/7; notes1→2→1; NITOR learned | C | — |
| 62 | Solving is not learning | research award0 at puzzle completion | NITOR false after solve; true only on item use | E/C | — |
| 63 | Research belongs to reader | other player's award0 | fresh player has no NITOR | K | — |
| 64 | Knowledge persistence | learned NITOR retained | actual player NBT save/load equality | K | — |
| 65 | JARBRAIN warp once | warp3 split perm2/sticky1/temp0; replay0 | (2,4,5)→(4,5,5), counter14; replay unchanged | W | — |
| 66 | Missing prerequisite or unfinished note | no knowledge award, note retained | count1 and knowledge unchanged | W | — |

### Additional native authority/boundary diagnostics

These are guards covered by the same tests, not additional original pricing rules: stale/replayed revision, wrong session/out-of-range sender, unknown aspect admission, RESEARCHDUPE gate, forged/ordinary discovery identity, offhand exclusion, shared bonus cannot fund copy, copy stack99→100 and127→128, count/counter integer-overflow refusal, full-inventory in-place copy, custom components/count127 preservation, native item codec, edit fixed-cell/unknown-aspect refusal, literal table ink despite creative/Unbreakable, erase ink1 without RP refund, solver pruning, and fresh menu tokens. They must not be mistaken for an oracle balance mismatch. Existing server-authority corrections are retained, not introduced by this lane.

## Reasoned-only / not proven

- Actual disconnect/reconnect with on-disk player-file loading: player NBT lifecycle is proven, network relogin is not.
- Client clicks/rendering and packet transport: production receivers run directly on the server thread.
- Survival acquisition, all ecology producer types/probabilities, and custom addon combinations.
- All generated puzzles/seeds: full lifecycle uses real NITOR generation at seed42; separate edit test includes a supplied minimal puzzle.
- Every possible research warp value: only JARBRAIN's warp3 completion/replay is driven here.
- No extrapolation from these finite counts to every signed-short/cap boundary.

## Reproduction

Use Java21 and `GRADLE_USER_HOME=/home/cyberfox-kali/wt/g32table/.gradle-g32`. Before every Gradle invocation remove only this worktree's `fabric/run-gametest/world`.

```sh
./gradlew :fabric:runGameTest -PgameTestHeap=768M \
  -PgameTestFilter=researchAspectsCombineAndBonusesRegenerate,researchTableCopiesDiscoveriesWithExactCosts,researchTableEditsSolveAndPreserveResources,primaryRequestConsumesPaperAndLastInk,primaryRequestRefusalsAreAtomic,solvedNoteLearnsOnceAndTransfers,noteRefusalsAndWarpUseOriginalRules \
  --offline --no-daemon --max-workers=2
```

Local evidence: `.tmp/g32-research.log` (initial6), `.tmp/g32-research-final.log` (final7), `fabric/build/gametest-focused-results.xml`. Transient worlds/cache/logs are not committed.
