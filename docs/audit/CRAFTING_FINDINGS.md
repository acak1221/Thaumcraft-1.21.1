# Crafting hypothesis audit — g6craft — 2026-10-23

## Scope and verdict

**4 recipe implementations; 23 type/rule checks. Two defects PROVEN by failing real-menu GameTests**, one shared root cause: vanilla `Ingredient.test` drops the original damage comparison. Shaped has a shipped reproduction (Deconstruction Table); shapeless uses a declared synthetic damageable-input recipe because the shipped shapeless set has no such input. Both are fixed separately. **The proposed extra-grid-stack dupe was not reproduced.**

The workbench is **3×3**, not 5×5: `ArcaneRecipeInput` requires nine cells and the original `ShapedArcaneRecipe.matches/checkMatch` uses dimension three. Changing it to 5×5 would violate the reference.

### Evidence and original specification

Read-only original JAR: `/tmp/opencode/rend/base/Thaumcraft-1.7.10-4.2.3.5.jar`, SHA-256 `3dba9786966974701578a658d1bb369bf35bdf5f363079f5ac9c4910a39113be`.
Inspected `javap -c -p` output for original `ShapedArcaneRecipe`, `ShapelessArcaneRecipe`, `CrucibleRecipe`, `InfusionRecipe`, `ConfigRecipes`, `TileCrucible`, and `TileInfusionMatrix`. Transient disassembly is `/tmp/opencode/g6craft-original-{recipes,config,machines}.javap`; no original code/JAR is modified or committed.

* Both arcane `checkItemEquals` methods compare required/offered damage, allowing required damage32767 as wildcard. Shaped bytecode offsets44–69 implement this comparison.
* `ConfigRecipes` Deconstructor registration uses new ItemStacks, not wildcard stacks, for the golden pickaxe/axe (registration offsets7507–7540); these require damage0.
* Original infusion spends essentia and pedestal inputs **during** a ritual, before the central output is placed. The original pedestal step replaces the whole stack with its container item (`craftCycle` offsets1317–1371). This is not a success-only/refund transaction.

## PROVEN defects

| Defect | Pre-fix behavior and failing test | Fix |
|---|---|---|
| CRAFT-01 shaped damage | Damage1 golden pickaxe crafts the real Deconstruction Table, consuming inputs/vis the original refuses. `craftingAuditShapedDamage` fails the complete physical-inventory preservation assertion. | `ArcaneIngredientMatching` restores required damage equality/wildcard matching; shaped matcher uses it. Commit `37a03726`. |
| CRAFT-02 shapeless damage | Synthetic damage0 golden-pickaxe ingredient accepts damage1 through the real menu. `craftingAuditShapelessDamage` fails the same inventory preservation assertion. | Shapeless matcher uses the same damage-aware predicate, preserving first-match order. Commit `fd8d51d9`. |

Pre-fix run: **3 tests, 2 expected failures**, grid boundaries passed. Raw result: [`evidence/g6craft-before.xml`](evidence/g6craft-before.xml). Test/failure evidence commit: `dc4e8288`.

The damage tests first establish a positive fresh-tool control, attempt the rejected craft, compare the entire saved table inventory (including wand), require no output, then craft with a fresh tool and compare every matrix cell to its snapshot minus exactly one and the wand to its exact price debit. Counts, names and unrelated components are not made stricter by the fix.

## Rule inventory (23 checks)

`A` = `craftingAuditGridBoundaries`; `D1/D2` = shaped/shapeless damage tests; `W` = `arcaneWorkbenchCraftsWithVisAndPersists`; `C` = `crucibleDissolvesAndCraftsWithOriginalCosts`; `I` = `infusionConsumesDistilledEssentiaAndPersistsSession`. Runtime results are recorded below; “reasoned” is explicitly not runtime proof.

| # | Type | Rule inspected / current behavior | Test / evidence | Fix |
|---|---|---|---|---|
| 1 | Arcane shaped | Translates within full3×3; checks empty cells outside pattern. Extra64 dirt stack refuses and preserves full inventory/vis. | A: all translations matcher-only, plus physical real-recipe refusal | None |
| 2 | Arcane shaped | Horizontal reversal accepted only with mirrored=true. | A: both flags, both orientations, all six offsets; matcher-only | None |
| 3 | Arcane shaped | Original whole-vis ×100 ×float cap modifier, truncated. | W: all six110 debits; D1:20vis→2200 | None |
| 4 | Arcane shaped | One-centivis shortage refuses before any debit. | W; D1 full table equality | None |
| 5 | Arcane shaped | Research checked at take, not just preview. | W stale-preview removal; D1 full table equality | None |
| 6 | Arcane shaped | One item per occupied cell, one priced wand debit, one output. | D1 each cell snapshot; W repeated/shift crafts | None |
| 7 | Arcane shaped | Was accepting damaged tools where original required fresh tools. | D1 **pre-fix FAIL** | CRAFT-01 |
| 8 | Arcane shapeless | Every nonempty cell consumes a required entry; extras refuse without debit. | A physical real Essentia Restrict recipe | None |
| 9 | Arcane shapeless | Original greedy first-match overlap ordering retained. | Source inspection; **reasoned only**, no new overlap runtime test | None |
| 10 | Arcane shapeless | Same original server vis pricing as shaped. | D2:20vis→2200 | None |
| 11 | Arcane shapeless | One-centivis shortage refuses unchanged. | D2 complete table equality | None |
| 12 | Arcane shapeless | Missing research refuses unchanged. | D2 complete table equality | None |
| 13 | Arcane shapeless | One ingredient and one debit per successful craft. | D2 all-cell snapshot and result/debit assertions | None |
| 14 | Arcane shapeless | Was accepting damaged tools where original required fresh tools. | D2 synthetic recipe, **pre-fix FAIL** | CRAFT-02 |
| 15 | Crucible | Each required aspect must meet threshold; lookup does not debit a short pool/catalyst. | C: Nitor2<3 independently for each required aspect | None |
| 16 | Crucible | Successful Nitor spends one catalyst,3 each of three aspects,50water; emits one result. | C actual collision and durable-output delivery | None |
| 17 | Crucible | Research-gated craft refused, but item ordinarily dissolves instead. | C checks dissolution aspects and zero recipe outputs | None; original behavior |
| 18 | Infusion stack | Exact occupied-pedestal multiset; eighth extra component refuses admission with no debit. | I admission + each pedestal/center/source checks | None |
| 19 | Infusion stack | Missing recipe research refuses before admission/debit. | I all pedestal snapshots, center, wand and source stock | None |
| 20 | Infusion stack | Exactly63 essentia spent once, source/demand decrease together, no finish re-debit. | I actual distilled stock192→129 and every drain-cycle invariant | None |
| 21 | Infusion stack | Seven occupied component pedestals replaced once, six successful cycles each. | I:42 ingredient cycles,7 consumed, all peripheral slots empty | None |
| 22 | Infusion stack | Central input retained until finish, then replaced by one result. | I physical center and single output | None |
| 23 | Infusion stack | Not success-only escrow: original spends essentia/components progressively; an interrupted ritual may lose paid inputs. Whole physical pedestal stacks are replaced, not decremented. | Current source and original bytecode; **reasoned only** for arbitrary cancellation/overstack cases | No deviation introduced |

## Execution

Java21; own `GRADLE_USER_HOME=/home/cyberfox-kali/wt/g6craft/.gradle-g6` (seeded from the external leader cache, not another worktree). Before **every** Gradle invocation: `rm -rf fabric/run-gametest/world`.

```sh
./gradlew :fabric:build :fabric:runGameTest --offline --no-daemon \
  -PgameTestFilter=craftingAuditGridBoundaries,craftingAuditShapedDamage,craftingAuditShapelessDamage,arcaneWorkbenchCraftsWithVisAndPersists,crucibleDissolvesAndCraftsWithOriginalCosts,infusionConsumesDistilledEssentiaAndPersistsSession
```

Initial post-fix dedicated-server run: **6/6 passed**, Gradle `BUILD SUCCESSFUL` (39s). Final Fabric build and fresh-world dedicated-server rerun with strengthened explicit research/shortage checks: **6/6 passed**, `BUILD SUCCESSFUL` (50s). Both `verifyTestIsolation` and `verifyProductionIsolation` passed; the remapped production JAR contains no test harness. Raw final result: [`evidence/g6craft-after.xml`](evidence/g6craft-after.xml). Full transient logs: `/tmp/opencode/g6craft-{before,after,final}.log`. XML timestamps are the machine-reported UTC timestamps (2026-10-22); the audit date follows the assigned2026-10-23 task date.

## Reasoned-only / limits

* Greedy overlapping shapeless ingredients (row9): inspected, not newly exercised.
* Infusion interruption after partial payment, arbitrary externally overstacked pedestals and full-stack replacement (row23): original/current control flow inspected, not claimed as new failing tests or changed to refundable crafting.
* Crucible “refusal consumes NOTHING” is true for recipe selection, **not** the physical collision: below-threshold/unresearched catalysts can dissolve ordinarily. This is intentional original behavior, not a newly proven defect.
* No exhaustive addon NBT predicate, tag wildcard metadata, every possible recipe, infusion enchantment/runic subtype, client rendering, multiplayer or NeoForge acceptance claim. Four implementations are counted, not all serializers in the project.
