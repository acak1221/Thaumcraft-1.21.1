# Optional central input damage — 2026-10-19

Implemented only in `common/src/main/java/thaumcraft/common/crafting/InfusionStackRecipe.java`, plus this note. Existing component predicates/matching helpers, Matrix/runtime, central snapshots/retention, recipes/resources, items, registration, client/network code and tests were not edited. No Axe or Lumber item/recipe was registered; this is their admission-predicate prerequisite, not implementation or acceptance of either unit.

## API / format

- The record appends **`Optional<Integer> inputDamage`** after its existing seven fields; generated accessor is `inputDamage()`.
- Existing **six-argument** and **seven-argument component-predicate** constructors remain, both defaulting to `Optional.empty()`. The new eight-argument constructor accepts explicit damage after `componentPredicates`, e.g. `Optional.of(0)`.
- JSON adds optional top-level **`"input_damage": 0`** for pristine admission, or another exact nonnegative native DAMAGE value. Omission means **no extra wear restriction**, not implicit zero. This is a field-format example only, not a placeholder recipe.
- Initial `matches(...)` delegates to existing `InfusionMatching.matches(input, inputDamage, offered.central())`. Ordinary ingredient/fuzzy-log/additional-component semantics stay with that helper; no generic NBT/component equality engine was introduced.

## Validation and stream

Explicit damage uses the unchanged component-damage declaration validator against a temporary one-ingredient list at index0. Every declared ingredient variant must expose native DAMAGE and have maximum damage **strictly greater** than the requested value; negative, empty/non-damageable or at/above-maximum declarations are rejected. Validation checks declared stock, not the offered stack's Unbreakable/name/enchantment data, matching existing component rules. It does not add a central entry to `component_predicates` or selected pedestal snapshots.

JSON CODEC validates the optional integer and existing recipe constraints. STREAM_CODEC appends a presence boolean and, when present, native VarInt damage after existing component declarations; decoding performs full recipe validation. Existing component/aspect/predicate count bounds and aspect insertion order remain intact. Both login endpoints must use the updated build; no cross-version recipe-packet compatibility or new protocol/schema version is promised.

## Admission is not running-ritual retention

Original authority under `../work/thaumcraft-4.2.3.5-oracle/thaumcraft/`:

- `api/crafting/InfusionRecipe.java:44–49,73–94`: initial recipe admission performs its declared metadata/damage match (with original ore/NBT behavior).
- `common/tiles/TileInfusionMatrix.java:339–345` instead calls `InventoryUtils.areItemStacksEqualForCrafting(..., true, true, false)` while running.
- `common/lib/utils/InventoryUtils.java:283–310`: that retention path deliberately ignores damageable-item wear while retaining its other matching rules.

Accordingly, **`input_damage: 0` rejects a worn central tool when selecting/starting a new recipe; it does not add a pristine recheck to an already-running ritual**. The existing actual central-stack snapshot and unchanged `InfusionMatching.centralStillMatches` continue to decide retention. No Matrix/session/snapshot field, journal, disk-schema change or late central predicate was added. This deliberately differs from existing late pedestal-component damage checks, which remain unchanged.

## Verification boundary

Source readback/review only. No Gradle/compiler, Java/game, tests, Python, smoke, worlds or agents were run; no test-source changes were needed for the preserved constructor API. Native CODEC/stream round trips, validation cases and integration are **not yet compiled or executed**. Root owns the actual target build after the parallel owner finals. Old recipes remain unrestricted unless they explicitly opt into `input_damage`; the future Axe recipe must wait for the real playable tool implementation.
