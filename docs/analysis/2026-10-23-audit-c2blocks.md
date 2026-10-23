# c2blocks quality-audit handoff — 2026-10-23

Worktree `/home/cyberfox-kali/wt/c2blocks`, branch `agent/c2blocks`, baseline
`3340abe7`. First required status/log command succeeded; tree initially clean.
No other worktree or primary checkout accessed/modified. No Gradle run.

## Outcome

Report: `docs/audit/BLOCK_ITEM_QUALITY.md`, with eleven linked TSV evidence
tables under `docs/audit/BLOCK_ITEM_QUALITY/`. Required find census: **331 Java
files = 102 blocks + 76 tiles + 153 items; 36,334 baseline lines**.

| Checklist | Inspected/inventoried | Findings |
|---|---|---:|
| Size | 331 files; 9 oversized files manually assessed | 9 |
| Duplication | 4,289 declarations; 37 substantial exact-body groups | 80 pairs |
| Literals | 10,838 literal nodes; 5,484 numbers; 77 RL calls; 360 local schema-key groups | 298 |
| World safety | 1,914 access lines, 387 directly in requested entry methods | 4 |
| NBT/components | 1,006 schema-like operations; 116 save/load overrides | 10 |
| Dead code | 535 private methods; 1,619 fields; 2,045 overrides inventoried | 2 redundant forwarders |
| Consistency | 89 interaction, 38 tooltip, 116 serialization declarations | 3 |
| Registration | 255 block/item-package files mapped; 91 config files, 984 source lines | 2 |

Total **408 observations: 0 blocker, 21 should-fix, 387 nit**. Counts are not
unique defects; groups and overlapping checklist perspectives are explained in
the report. Source inventories are mechanical evidence, not claims that every
method is semantically proved. There are 292 remaining repeated-key nits, not
292 data-loss defects.

## Safe changes

* `TileMirror`: four private schema-key constants (`linkX/Y/Z/Dim`), twelve call
  arguments replaced without changing their values, types or omission behavior.
* `RetainedDataBlockEntity`: removed load/save overrides that only forwarded to
  super. Inherited callbacks remain; no guard or serialization behavior removed.

## Verification

The worktree initially had no build classes. Used direct Java 21 javac to
compile all **1,445 common sources** with cached remapped Minecraft/dependency
jars and the existing access-widening overlay. Baseline passed. Focused changed
source compile against remapped jars **plus local common/build/classes/java/main**
passed, and final all-common compilation passed (deprecation/unchecked notes).
An intermediate compile caught an over-removed HolderLookup import; restored it
and repeated both checks successfully. No behavioral/client/server tests run.
Source-equivalence assertions passed: expanding constants restores the baseline
mirror exactly, and only the two super-forwarder lines differ in the base. All
TSV references were checked for existing files and valid source line ranges.
Transient arguments/logs and AST inventory helper: `.tmp/c2blocks/` (not committed).

## Left open / limits

No behavior-sensitive NBT/default/quarantine, remote chunk-loading, dimension
lookup, canonical reservoir policy, property profile or class-responsibility
refactor was performed. Findings need schema/authority tests or original-policy
evidence before changes. No proven uncalled private methods, write-only fields or
obsolete engine callbacks were deleted. Original-JAR identity/properties for
every variant and runtime hook reachability remain **unverified**, not passed.
The supplied report is a bounded quality review plus complete source indexes,
not exhaustive original-parity acceptance.
