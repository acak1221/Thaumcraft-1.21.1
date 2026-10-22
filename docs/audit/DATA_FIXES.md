# Data-integrity fixes — agent/f1data

Repair policy: rejected payloads are inert, retained without normalization, and require an
administrator to back up and repair the offending tag offline before ordinary use resumes.
No automatic replacement with defaults is a repair.

## F09 — advanced furnace

- Site: `TileAlchemyFurnaceAdvanced.loadAdditional`.
- Malformed inputs: `Aspects=["unread stock"]` with absent vis; string-valued
  `RejectedAdvancedFurnace` envelope.
- Tests: `AdvancedFurnaceSaveIntegrityTest.wrongElementStockIsQuarantinedLosslessly`
  and `malformedRejectedEnvelopeSurvivesRepeatedReload`.
- Before: accepted an empty pool / replaced the malformed envelope with `{}`.
  After: rejects, logs position/reason, retains the complete payload across reloads,
  and uses existing quarantine fences. Players could lose their aspect stock.
- Red proof: both tests failed against the old parser (2026-10-23).

## F07 — labyrinth map

- Site: `OuterLandsProvider.MazeSavedData.load` and all three map operations.
- Malformed inputs: scalar/wrong-element `cells`, string coordinate, duplicate coordinate.
- Tests: `MazeSaveIntegrityTest.malformedCollectionsDoNotBecomeEmptyWorldMaps`
  and `malformedCoordinatesAndDuplicateCellsAreNotNormalized`.
- Before: malformed collections became empty, coordinates defaulted to zero, duplicates
  overwrote earlier rooms. After: log the SavedData ID and offending row/coordinate,
  retain the entire original owner across reload/save, refuse cell reads, range checks,
  and new labyrinth writes until offline repair. Player loss: existing labyrinth layout,
  navigation and room identities; new generation could overlap old rooms.
- Red proof: both tests failed before the fix (2026-10-23).

## F05 — player knowledge and outer attachments

- Sites: `PlayerKnowledge.load`, `PlayerKnowledgeMixin` disk attachment,
  `PlayerAccessories.load` (including accessory clone transfer).
- Inputs: string-valued research collection, wrong-element aspect list, unknown
  data version, scalar outer knowledge/accessory attachment.
- Tests: `KnowledgeSaveIntegrityTest.malformedKnowledgeRetainsOwnerAndPreviousLiveValue`,
  `wrongAspectElementAndUnknownVersionAreNotDefaults`,
  `nonCompoundOuterAttachmentSurvivesCloneAndSync`; and
  `AccessoryAttachmentIntegrityTest.nonCompoundOuterAttachmentSurvivesCloneDeathAndSyncEnvelope`.
- Before: typed getters silently reset research, pools, scans, warp or accessories;
  non-compound attachments were ignored and replaced at the next save.
- After: validate before mutation, retain the previous understood live knowledge,
  fence all knowledge mutations (including wipe/initialization), preserve original
  attachments exactly on disk and clone. Sync uses a lossless envelope for scalar
  attachments. Accessory rejection uses the existing wear/equip/death fences.
  Player attachment logs identify UUID/position; direct knowledge/sync calls log
  the offending schema field. Offline repair is required; no automatic wipe.
- Red proof: both compound-knowledge tests failed before the fix, then all four
  tests passed. These are owner-unit tests, not a live player lifecycle GameTest.
- Player loss prevented: research progression, discoveries/pools, scans, warp,
  and four equipped accessory slots.

## F06 — six stock owners

- Sites: `loadAdditional` in `TilePedestal`, `TileResearchTable`, `TileMirror`,
  `TileMagicBox`, `TileArcaneFurnace`, `TileThaumcraftInventory` (and its actual
  `TileWandPedestal` subclass's tick/save/load fences).
- Input: `id="missing_addon:owned_item"` in each owner's native stack row;
  wrong-element inventory lists and invalid research bonus tags also tested.
- Tests: `StackOwnerSaveIntegrityTest.pedestalRetainsUnreadStack`,
  `researchTableRetainsUnreadStack`, `mirrorRetainsUnreadStack`,
  `magicBoxRetainsUnreadStack`, `arcaneFurnaceRetainsUnreadStack`,
  `sharedInventoryRetainsUnreadStack`, `wrongCollectionElementsAndInvalidBonusAreRetained`,
  `retirementArchiveIsLosslessAndDeduplicatesOnlyIdenticalRecords`.
- Before: failed item decode became EMPTY and the next save omitted the owned
  item; bad table rows/bonuses were truncated or filtered.
- After: parsers throw on unread stacks; validation precedes owner changes;
  the loader logs class/position/reason and retains the entire original owner.
  Menus, insertion/extraction, processing, mirror transfers and table commits
  are fenced. Saves preserve the opaque owner rather than writing absent stock.
- Retirement: `RetainedDataBlockEntity` archives rejected data on server attachment,
  live rejection, and removal to overworld SavedData
  `thaumcraft_rejected_block_entities`. Records include dimension, position, owner,
  blockstate and full payload. This is an offline-repair archive, **not** a second
  inventory or an automatic item grant. Removal can never consume the only copy.
- Red proof: all six native loaders failed the malformed missing-item tests before
  their fixes. The research test initially encountered an unrelated pre-registry
  constructor limitation; it was rerun with the old parser after fixing that
  fixture, and failed at the intended re-save equality assertion.
- Green proof: eight owner/archive JUnit cases and existing valid/empty inventory
  regressions pass. Fabric `rejectedPedestalSurvivesPhysicalBlockRemoval` passed
  on a headless server: real registered pedestal, unknown saved item, physical
  block replacement, retained archive payload, and archive reload. Runtime
  removal of each of the other five owner types is not separately claimed.
- Player loss prevented: pedestal output, research tools/notes and bonus pools,
  mirror buffered deliveries, linked-box stock, furnace inventory and pedestal wand.

## F08 — partly paid infusion session

- Site: `TileInfusionMatrix.loadAdditional`, predicate and stack readers.
- Inputs: invalid `ingredient` predicate, string-element `recipein`, missing-addon
  central stack; fixtures include central/output counts, outstanding essentia,
  remaining XP and recipe-player UUID/name.
- Tests: `InfusionSessionSaveIntegrityTest.malformedPredicateDoesNotBecomeAnEmptyIngredient`,
  `wrongRequirementElementDoesNotBecomeNoRequirements`,
  `unreadCentralStackDoesNotErasePaidSession`, and valid-empty control
  `validEmptyRequirementsAreStillReadable`.
- Before: invalid predicate became `Ingredient.EMPTY`; wrong-element list became
  no requirements; missing item became an empty central snapshot. Saving destroyed
  the selected session and its recoverability.
- After: strict predicate/stack/list/debt/scalar validation precedes **all** session
  assignments. Errors log the position/reason and retain the entire session
  verbatim. `current()` fences cycling, start/activation and essentia demand.
  Shared rejected-owner custody also archives the session before block removal.
  Player loss prevented: partly paid crafting work, exact selected requirements,
  output/central binding and remaining debts. No recipe rematching or sentinel repair.
- Red proof: all three malformed-session JUnit tests failed before the fix;
  all pass after. Fabric `rejectedInfusionSurvivesPhysicalBlockRemoval` passed:
  real matrix rejects malformed requirements, cannot request essentia, and preserves
  the whole paid session in the archive after physical block replacement.

## Final verification

- Java 21, isolated `GRADLE_USER_HOME=.gradle-f1`, one invocation at a time.
- `./gradlew :common:test`: **2,490 tests, zero failures/errors/skips** after
  the final source changes. Log: `.tmp/f1data/common-test-final.log` (ignored).
- Two focused Fabric headless GameTests passed, one required test per run:
  `rejectedPedestalSurvivesPhysicalBlockRemoval` and
  `rejectedInfusionSurvivesPhysicalBlockRemoval`. The latter log is
  `.tmp/f1data/f08-gametest.log` (ignored).
- No full-client, multiplayer, server-restart, or NeoForge acceptance is claimed.
  The Gradle wrapper configured all existing modules while running the common task;
  no NeoForge test/build task was requested.
