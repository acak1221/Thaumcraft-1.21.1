# Nugget ownership fix — R1 / R2

Baseline: `e961c6f2`, branch `agent/c6fix`. Shared ownership fix applies to both loaders; runtime validation targets Fabric only.

## Oracle and retained owner

Read-only oracle: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`.

- `thaumcraft/common/config/ConfigItems.java:407-408` constructs and registers one `ItemNugget`.
- `ConfigItems.java:281-282` assigns the ore dictionary names `nuggetThaumium` / `nuggetVoid` to that item's metadata 6 / 7.
- `thaumcraft/common/items/ItemNugget.java:43-44,66-67` supplies both icons and unconditional creative entries.

Therefore `ThaumcraftContent.MATERIALS` owns both flattened behavioral `ItemNugget` registrations. Their IDs and `EquipmentRules` metadata roles must not change.

## Regression checks

`NuggetOwnershipTest.everyLiteralItemIdHasExactlyOneOwner` scans every Java file below the common config directory for literal `ITEMS.register(...)` declarations. Registry cardinality alone cannot detect a rejected duplicate supplier. `wandIngredientsAliasTheBehavioralOwner` additionally requires the exact same supplier object, not just equal IDs.

`NuggetOwnershipRuntimeChecks` checks live registry cardinality, class, original metadata role, both public supplier identities, one creative entry, both original/common tags, every loaded recipe ingredient/result referencing either ID, ingot splitting, and both inert-cap recipes accepting the survivor. No test constructs a block.

## Consumer review

- Wand public suppliers become aliases of the content suppliers, preserving their `RegistrySupplier<Item>` API.
- Creative tab publication stays in the existing `ThaumcraftContent.MATERIALS` loop; remove the two extra wand entries.
- Six recipe resources (split/join and tag-based inert cap for each metal) keep their IDs/tags unchanged. Runtime checks verify loaded instances and cap matching.
- Four tag resources (`c:nuggets/{thaumium,void}` and `thaumcraft:original_ore/nugget_{thaumium,void}`) keep their IDs unchanged.
- `ResearchIconDisplay` maps original metadata 6/7 to the same IDs; aspect data and language/assets also keep those IDs. No consumer casts either supplier result to a plain-only item class; `ItemNugget extends Item` preserves all item APIs.
- Fabric's `ThaumiumCapsClientSequence.nugget()` returns the wand alias as `Item`; `GogglesClientSequence` passes it to its item-count helper. Both remain compatible without edits. NeoForge sources have no direct nugget consumers. Client sequences were reviewed, not launched.

## Full literal duplicate scan

Scope: all recursive `*.java` files under `common/src/main/java/thaumcraft/common/config/`, regex `\bITEMS\.register\(\s*"([^"]+)"`. This scans every literal, not only nuggets; computed/helper registrations are outside this particular scan.

Before: **273 declarations / 271 distinct IDs**. Only duplicates:

- `thaumium_nugget`: `ThaumcraftContent.java:162`, `ThaumcraftWands.java:50`.
- `void_nugget`: `ThaumcraftContent.java:164`, `ThaumcraftWands.java:54`.

After: **271 declarations / 271 distinct IDs; no duplicate literals** (also enforced by the JUnit scan).

## Executed red / green evidence

Environment: Java 21, Gradle 8.12.1 wrapper, private `GRADLE_USER_HOME=/home/cyberfox-kali/wt/c6fix/.gradle-c6fix`, offline cached dependencies. Removed `fabric/run-gametest/world` before each Gradle invocation.

Pre-fix command: `./gradlew :common:test --tests thaumcraft.common.config.NuggetOwnershipTest --offline --console=plain`.
The first attempt timed out during initial compilation at the command's 120-second limit; retry completed **2 tests, 2 failures**, build exit 1, in 1m 9s:

```text
wandIngredientsAliasTheBehavioralOwner(): expected DeferredRegister.Entry
  <minecraft:item@thaumcraft:thaumium_nugget> but was a different Entry for the same ID.
everyLiteralItemIdHasExactlyOneOwner(): duplicate literal item registrations
  expected: <{}> but was:
  <{thaumium_nugget=[ThaumcraftContent.java, ThaumcraftWands.java],
    void_nugget=[ThaumcraftContent.java, ThaumcraftWands.java]}>
```

Only after this failure were the production aliases and creative-list cleanup applied.
Local red log: `.tmp-c6fix-red-retry.log` (ignored).

Post-fix command: `./gradlew :common:test :fabric:runGameTest --offline --console=plain`:

- **Common JUnit: 2,521 tests, 0 failures/errors/skips.** Includes both formerly red checks.
- First full Fabric run: **438 tests, 436 passed, 2 failed**. `nuggetownership` passed (0.028s). Failures: `excavationpreservesspecialdropsanddurableoutputs` refused an owned lane containing an old entity/item/XP; `cultist_faction_keeps_its_numbers_gear_and_loot` saw a robe hood in the knight's gear assertion. This run is not a green suite. Log: `.tmp-c6fix-green.log`.
  Diagnostic review: `CultistFactionRuntimeChecks.java:46,53` invokes random equipment and then requires a plate helm; `CultistEquipmentRules.java:81-89` legitimately permits a robe hood in the rare void-sword branch. The excavation failure is the existing fixture admission assertion in `FocusUpgradeRuntimeChecks.java:228-229`. Neither path reads nugget ownership.
- Fresh-world full rerun command: `./gradlew :fabric:runGameTest --offline --console=plain`: **436/438 passed**, including `nuggetownership`. Cultist gear passed this time; `g31focuscastinglaunchesandplayercooldowns` failed its second-player-casts-during-first-cooldown assertion and excavation again failed fixture admission. Log: `.tmp-c6fix-green-retry.log`. No production/test changes were made between these first two runs.
- Then assigned the new nugget case its own `nugget_ownership` batch, rather than inserting it into the existing default-batch layout. Existing tests/assertions are unchanged. Final fresh-world command: `./gradlew :common:test :fabric:runGameTest --offline --console=plain`: **2,521 common tests green; full GameTests 436/438**, with `nuggetownership` passing (0.047s). Excavation again refused the occupied fixture; cultist gear saw an empty head slot (also permitted by the rare equipment branch). Log: `.tmp-c6fix-final.log`; full XML retained locally at `.tmp/c6fix-full-results.xml`.

**Full-suite green is blocked**, not claimed. Three completed full runs are recorded above; unrelated tests were neither weakened nor removed. No NeoForge runtime or client acceptance is claimed.

Focused nugget command: `./gradlew :fabric:runGameTest -PgameTestFilter=nuggetOwnership --offline --console=plain`: **1/1 passed**, `BUILD SUCCESSFUL in 53s`. XML: `fabric/build/gametest-focused-results.xml` (`nuggetownership`, 0.102s). Log: `.tmp-c6fix-focused.log`. Thus common JUnit and focused ownership runtime are green; the requested full GameTest green remains blocked by the recorded failures.
