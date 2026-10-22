# g6craft crafting audit — 2026-10-23

Branch `agent/g6craft`; owned worktree `/home/cyberfox-kali/wt/g6craft`. Initial clean revision `e8995568`.

## Hypothesis and outcome

Tested only custom-recipe input/admission/payment parity. **4 implementations /23 rules inspected;2 defects proven by pre-fix failing GameTests.** Full inventory, original evidence, limits and commands: [`../audit/CRAFTING_FINDINGS.md`](../audit/CRAFTING_FINDINGS.md).

Extra-grid-stack dupe disproved for the exercised shaped/shapeless recipes: full3×3 scan already rejects extras. The workbench was never5×5 in this port or the reference.

Found damage-rule loss in both native arcane matchers: vanilla `Ingredient.test` admits damaged tools while original default-damage-zero stacks do not. The real Deconstruction Table is affected. Shapeless proof uses a scoped synthetic recipe; it does not imply a shipped damageable shapeless recipe exists.

## Commits

* `dc4e8288`: regression tests and raw pre-fix XML (two failures, one pass).
* `37a03726`: shaped damage-aware comparison.
* `fd8d51d9`: shapeless comparison, retaining greedy order.

New tests use the existing connected-survival-player fixture and real menu extraction; synthetic recipe replacement is fully restored in `finally`. Additional crucible/infusion checks extend existing actual-content workflows rather than replacing them with mocked recipe matches.

## Validation status

Pre-fix focused Fabric dedicated-server run executed:3 tests,2 expected failures at the complete input/wand preservation assertion. Initial post-fix focused six-test run:6/6 passed, `BUILD SUCCESSFUL` in39s. Final `:fabric:build :fabric:runGameTest` with strengthened explicit research and one-centivis-shortage checks:6/6 passed, `BUILD SUCCESSFUL` in50s; production/test isolation checks passed. Raw before/after XML is committed under `docs/audit/evidence/`. Full logs are `/tmp/opencode/g6craft-{before,after,final}.log`. No NeoForge task or client acceptance claimed.

## Important non-bugs / remaining reasoning

Original crucible collision ordinarily dissolves a rejected catalyst, so research/threshold craft refusal is not a promise to preserve a thrown item. Original infusion pays essentia and components progressively, not only on final success, and replaces the whole pedestal stack. These requested premises cannot justify changing original behavior. Greedy overlap and arbitrary cancellation/overstack cases remain source-reviewed, not newly proven failures. See the audit limits for subtype/addon exclusions.
