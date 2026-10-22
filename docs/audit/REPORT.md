# Final audit report

Baseline when the audit began: `c6feeda7` — the scope ledger claimed **1044 of 1044
`parity_verified`**. The audit was written as a contract *before* it ran
(`docs/audit/PLAN.md`), so its standard could not be fitted to its results.

Four auditors ran independently, each told to **try to disprove** the claim rather than confirm
it, and each required to report the exact number of things it inspected.

---

## Summary — what the audit changed

| Area | Verdict |
|---|---|
| Ledger counts | **Correct.** Recomputed independently from 1044 rows; the whole `counts` block matches. `stale_review_fingerprints: 0`. |
| Exclusions (163) | All 163 searched against the oracle. **77 have zero class-name hits; 86 fail the literal "no counterpart" test** — but 63 of those are original-bearing rows and 23 are relocated legacy names, so this is a **policy mismatch, not 86 missing features**. |
| Evidence strings | 220 sampled across 21 families; **66 wrong or materially incomplete**. 79 evidence strings and 42 notes corrected. |
| Code quality | 6994 declarations screened; **428 dead-code findings** (423 are retained API exports, 4 wired, 1 class deleted); 12 duplication pairs; 32 defective comments of 107 checked; 7 diagnostics; 6 static-state findings; 5 error-handling; 34 resource paths outside the verifier. |
| Tests | 613 files / 2479 JUnit methods / 632 GameTest entries screened; 41 files and 232 bodies read. **47 weak methods found, 20 repaired**, 27 left open. |
| Repository | 91 MB `.git`, 1060 manifest hashes verified, **0 wrong**, no build artefacts tracked. Status docs corrected from 1017/27 to 1044/1044. |

---

## The three tests that could not fail for the right reason

The test audit named three tests that stayed green **with their feature deleted**. Each was
replaced with a real observation, and each was then verified by **mutating the production code**
and confirming the test goes red:

| Test | Old defect | Mutation that now fails it |
|---|---|---|
| Bone bow scenario | Read `EquipmentRules` constants; never used the item | `releaseUsing` returning immediately → **RED** |
| Mirror projection | Asserted a homogeneous rescale stays proportional — the identity matrix satisfies that | `texture()` returning a bare `Matrix4f` → **RED** |
| Centivis arithmetic | Did the cast arithmetic inside the test, never called a cost path | 4-arg `focusModifier` returning `.1F` → **RED** |

While fixing the centivis test the leader's first attempt asserted `.9F - .5F == .4F` and
`5 × .4 = 2`. **Both were wrong**: Java float gives `0.39999998F`, so 5 centivis cost **1**. The
run caught it. This is recorded because writing expected numbers by hand is exactly the failure
mode the audit was hunting.

---

## Real defects the audit found and fixed

1. **`jar_brain` and `jar_node` played the vanilla glass sound**, while the plain jar used the
   original `CustomSoundType("jar")`. The oracle sets the sound in the `BlockJar` constructor
   (`oracle BlockJar.java:88`), so all three must share it. Fixed to `JAR_SOUND`.
2. **`ChestLootPoolRules` was disconnected** — a rule class with no caller. Deleted with its seven
   tests.
3. **Pouch layout helpers were unwired**; now called by `ContainerFocusPouch:38,41,44`.
4. **Four GameTest scenarios forced chunks before entering their cleanup scope**, so an exception
   leaked chunk tickets — the class of bug that made the suite need 3 GB. Fixed in `GolemChunks`,
   `WaveLane`, `GrateRuntimeChecks`, `LevitatorRuntimeChecks`.
5. **A duplicated fluid-averaging rule and a duplicated scanner-brightness formula** consolidated.
6. **Stack traces and per-tick logging** removed from `UtilsFX` and `ChunkRegeneration`.

---

## The pattern this effort kept hitting

Five separate claims of "no 1.21.1 counterpart" were **disproved** by later lanes that actually
opened the vanilla class:

| Claim | Reality |
|---|---|
| The mirror's `glTexGen` cannot be reproduced | `rendertype_end_portal` demonstrates projected UVs in a shader; implemented |
| The chunk regeneration queue is impossible | `ChunkRegeneration` exists, ten chunks per tick, oracle seed |
| The scattered feature / `auraGen` cannot be reproduced | `AuraGenRules` maps the native starts |
| The 2000-particle cap has no analogue | Implemented in wave 8 |
| The ore overlay needs "fast graphics" | It is anisotropic filtering |

Every one of those claims had been recorded in the ledger **as fact**. That is the single most
important finding of the whole audit, and it is why `docs/audit/KNOWN_DEVIATIONS.md` lists the
disproved claims next to the accepted ones.

---

## What is NOT green, and must not be reported as green

The audit's §5 (client and multiplayer acceptance) **was not performed by the auditors** — they
cannot run the client. As of this report:

* **No visual frame of any wave 4–8 feature has been inspected**: thaumometer screen and readout,
  research graph, void-robe layers, node item, excavation beam, architect overlay, essence flow,
  sparkles, candles, gas surface, crust join, eldritch stairs, mirror, bone bow glint, radial menu.
* **Runtime mixin injection is unproven on both loaders.**
* **The projective mirror shader has never been compiled by the driver.**
* **Nether wild nodes have never been generated in a real world.**
* **The chunk regeneration queue has never run on a saved world.**
* **The two-client multiplayer acceptance predates waves 4–8.**

These are listed as unverified in `KNOWN_DEVIATIONS.md` §7 by design, so that "unverified" cannot
be read as "verified".

---

## Gate results (after every fix in this audit)

| Gate | Result |
|---|---|
| `./gradlew :common:test` | green |
| `./gradlew :fabric:runGameTest` (no flags, 3G default) | **325/325** |
| `./gradlew :fabric:build` | green |
| `./gradlew :neoforge:build` | green |
| `convert_assets.py --verify` | `missing_references: 0` |
| `python -m unittest discover scripts/porting` | green |
| `update_scope_ledger.py --check` | `stale_review_fingerprints: 0` |

---

## Outstanding, by owner

**Leader must decide (not a code task):**
* The exclude policy: 86 rows contain a real original implementation whose behaviour was replaced
  or ruled dead, yet the disposition is `exclude`. Either those reasons are accepted as a
  documented exception class or the rows change disposition. The audit deliberately did not mass-edit
  163 rows.
* 423 API exports are retained with no internal caller. They are a public surface, not necessarily
  dead code; the audit refused to mass-delete them.

**Should fix (reported, not done):**
* 27 weak tests remain.
* The mock-player cleanup in both loaders only hooks `succeed` and `fail(String)`, so an assertion
  failure or a timeout bypasses it.
* 34 Java-composed resource paths are invisible to `convert_assets.py`.
* Tunnel rendering still differs from the oracle and needs visual validation.
