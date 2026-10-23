# e3docs — API contract documentation

Date: 2026-10-23. Worktree: `/home/cyberfox-kali/wt/e3docs`; branch: `agent/e3docs`; starting commit: `974fde6a`. Initial required status/log command succeeded with a clean worktree. Did not access another worktree or `/home/cyberfox-kali/Thaumcraft-1.21.1`.

## Outcome

- Reconciled the 510-entry missing-contract inventory against the local original JAR's published API and reviewed native cross-package callers: **345 real API**, not the expected tiny subset (323 original counterparts plus 22 additional native call targets).
- **272 newly documented**, **3 pre-existing local comments**, **70 contract-unclear declarations deliberately left without invented member contracts**. Exact membership, exclusions, unclear reasons and all L01–L17 notes are in `docs/audit/API_CONTRACT_DOCS.md`.
- Added `docs/api/README.md`: consumer-specific units, per-member null/ownership, owning-server-thread rules, and retained-shape/deprecation boundaries.
- Legacy notes correct inert default effects/services, metadata identity, unsupported scannable/theorycraft/staged DTO/focus integration, unresolved furnace pages, limited infusion patches, ignored fuzzy flags and fluid/miss ray behavior. Added only appropriate replacement-bearing deprecations; no deletion/signature/access changes.
- Also clarified already-deprecated multi-aspect container/node replacements, raw native rod centivis versus original whole-vis rod capacities, detached compatibility material registration, and mutable list/array ownership.

## Evidence / checks

- Read `API_QUALITY.md` documentation/deprecation/legacy sections before source edits.
- Read original public API metadata from this worktree's original JAR without modifying original inputs. Excluded name-only call-token collisions and six misleading same-arity publication candidates.
- Java 21 DocTrees parse matched all 510 declarations; comments were associated with individual overloads.
- Executable-token equivalence after stripping comments and allowed Deprecated annotations: **40/40 changed Java files pass**. Only documentation, whitespace separating two formerly same-line declarations, and allowed deprecation metadata changed.
- Required remapped-jar + `common/build/classes/java/main` `javac` check: focused five-file subset **passes**. Full common compile **fails/blocked** by missing remapped Architectury/access-widener classpath in this initially unbuilt worktree (742 errors); not attributed to Javadoc and not presented as a full build pass. Final ignored logs: `.tmp/e3-final-focused-javac.log`, `.tmp/e3-final-full-javac.log`.
- No gameplay, client, server, NeoForge or behavioral test result is claimed. No public push performed.

## Integration follow-up

Run full common/Fabric compilation with the parent's normal mapped/widened dependencies and classes. Preserve the 70 explicit contract-unclear items for implementation/compatibility decisions; do not replace them with boilerplate interface promises. The documentation inventory ordinals intentionally refer to the unchanged 510-entry section of `API_QUALITY.md` at this baseline.
