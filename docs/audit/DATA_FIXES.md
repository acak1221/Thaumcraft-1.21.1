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
