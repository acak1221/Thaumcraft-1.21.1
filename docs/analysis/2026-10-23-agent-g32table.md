# g32table — research balance hypothesis

- Worktree `/home/cyberfox-kali/wt/g32table`, branch `agent/g32table`; initial status clean, base `efaa6977`.
- Scope remained one hypothesis: combination balance / note paper and ink loss. No other worktree or main checkout touched.
- Read original private `PacketAspectCombinationToServer`, `ScanManager`, `TileResearchTable`, `ResearchManager`, `ItemResearchNotes` before deciding whether gameplay changes were warranted.
- **Outcome:66 driven oracle rules,66 PROVEN within the fixtures,0 mismatches,0 gameplay fixes.** Findings and exact expected/native quantities: `docs/audit/RESEARCH_FINDINGS.md`.

## Changes

1. Expanded placed-table combination GameTest to all42 independently recorded oracle pairs: first discovery3, reversed repeat4 total, bonus-paid repeat5 total. Directly assert actual input and output pools, personal-before-bonus order, inventory preservation, empty initial pool, note replacement retaining pool.131 paid experiments including one invalid aer+aer pair; invalid experiment now compares complete expected knowledge state, not only one output pool.
2. Added one continuous actual issuance→four native table edits→copy→learn scenario. Paper3→2→1, tool90→91→95→95, lux/ignis10/10→8/8→5/7, notes1→2→1. No synthetic solved flag in this lifecycle; it uses generated NITOR seed42.
3. Added missing-paper and missing-black-dye copy refusal checks; refusal helper compares complete table NBT and every inventory slot in addition to knowledge.
4. Added fresh-player knowledge isolation check and JARBRAIN duplicate-note warp replay check.
5. Ignored only this lane's private `.gradle-g32/` cache.

No production source changed. No artificial failing test is claimed as a defect. Existing black-dye fix and server-authority guards were retained and exercised, not credited as new fixes.

## Verification

Java21, Gradle8.12.1, own `GRADLE_USER_HOME`; Fabric only. Gradle configures the existing NeoForge project but no NeoForge task/build/test was requested. Removed only this worktree's `fabric/run-gametest/world` before every Gradle invocation.

- First launch attempt stopped before Gradle: `.tmp/` did not exist. Created the owned log directory; not a gameplay failure.
- Initial focused run:6/6 passing, `BUILD SUCCESSFUL` in1m36s; `.tmp/g32-research.log`.
- Expanded lifecycle/edit run:7/7 passing, `BUILD SUCCESSFUL` in35s. Then strengthened invalid-pair whole-knowledge assertion and reran the final seven-test selection.
- Final post-change run:7/7 passing, `BUILD SUCCESSFUL` in27s. Evidence: `.tmp/g32-research-final.log`, `fabric/build/gametest-focused-results.xml`; exact command in findings. Successful dedicated GameTest server includes normal player/world save/shutdown. `git diff --check` passed.

## Limits / reasoned-only

Actual network relogin/on-disk player-file loading (only native player NBT save/load proven); client clicks/rendering/C2S dispatch; survival acquisition; all ecology producers and probability distributions; arbitrary addon combinations; all generated puzzle seeds (full lifecycle is NITOR42); every research warp magnitude (only JARBRAIN3); signed-short/cap extremes. No full-suite or NeoForge parity claim.
