# a2quality — merged code audit, 2026-10-23

Branch: `agent/a2quality`; input `c6feeda7`. Initial status was clean.
Only this worktree was modified. The explicitly requested PLAN and the private
oracle were read; no other worktree was inspected or modified. No Gradle was run.

## Deliverables

* `docs/audit/CODE_FINDINGS.md`: eight checklist sections, severities, both-sided
  oracle citations for behavior-derived findings, and explicit remaining decisions.
* `docs/audit/CODE_INVENTORY.json`: full no-reference declaration list, first test
  locations, exact compiler-tree static non-final field list, static arrays,
  collection screens, broad catch bodies and composed resource candidates.

## Inspection totals and findings

| Checklist | Inspection | Findings |
|---|---|---|
| 3.1 | 3,455 public-static methods + 3,539 fields; 94 initial rule/pose classes | 428 disconnected non-API surfaces: 423 retained, four wired, one class deleted |
| 3.2 | 12 duplicate pairs, 20 owners | 12; three consolidated, nine left |
| 3.3 | 314 literal candidate lines; 30 oracle-checked sites | 30 should-fix sites retained |
| 3.4 | 6,950 comment blocks screened; 107 checked | 32 defective blocks; 31 repaired, one unsupported equivalence left |
| 3.5 | Whole 1,442-file input; seven diagnostic matches | Three sites fixed |
| 3.6 | 438 non-final static fields; 240 collection declarations; 90 explicit mutable initializers | Six concrete lifecycle/thread-ownership findings retained |
| 3.7 | 164 broad catches, 267 throw candidate lines, 34 resource/registry fail-fast sites | Five should-fix fallback/error-visibility findings; no reproduced save corruption |
| 3.8 | 37 concatenation candidates, 34 actual resource-loading sites | All 34 outside verifier Java-reference coverage |

Reference screening is deliberately conservative: it excludes strings/comments,
counts same-class calls and method references, and checks loader production trees.
Positive identifier matches are not resolved-call-graph proof. Final no-reference
count is 737, including 314 API/entrypoint candidates not automatically declared
defects. Native-only ownership/verifier issues have no original threading/build
specification; no fictional oracle citation is attached to them.

## Behavior-neutral changes

* Removed `ChestLootPoolRules` and its seven disconnected unit tests, after
  searching both loaders and common production code.
* `ContainerFocusPouch.java:38,41,44` now uses the four pre-existing position
  helpers in `DeviceContainerRules.java:143-146`, rather than duplicate formulas.
* `GasSurfaceRules.java:73-74` delegates the identical fluid corner average to
  `PureFlowRules.renderAverage`; the .875 override now has a single named owner.
* `ItemThaumometerRenderer.java:211` calls the existing brightness helper.
* `CandleDripRules.java:66` uses the existing `MAX_DRIPS` constant instead of 5.
* `UtilsFX.java:82,104`: contextual warnings replace raw stack traces.
* `ChunkRegeneration.java:141`: per-tick INFO progress becomes DEBUG.
* Corrected misleading/crossed citations, sparkle rate/range prose, triangular
  thaumometer-row prose, stale Repair/inhabited-zombie limitations, invented
  original datapack tolerance, and the tunnel's false fixed-function limitation.
  See the 107-block sample and individual source locations in CODE_FINDINGS.

## Actual validation

Fresh worktree lacked `common/build/classes/java/main`. A plain all-common javac
attempt first selected intermediary/sources API jars, then exposed 74 normal
access-widener visibility errors after classpath correction. Those attempts are
not reported as passes.

Final compile used Java **21.0.12.1**, all **1,441** current common Java sources:

```text
javac --release 21 -proc:none -sourcepath '' -cp <classpath> \
  -d common/build/classes/java/main <all common Java sources>
exit 0
Note: Some input files use or override a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
```

Classpath includes this worktree's common output, the remapped 1.21.1 merged
Minecraft jar under `~/.gradle-leader/caches/fabric-loom/minecraftMaven`, binary
`~/.gradle-leader/caches/modules-2` dependencies, and named Architectury NeoForge
API first. No NeoForge build/runtime test was performed. Source/javadoc jars were
excluded. Javac did not substitute source stubs.

A temporary bytecode **access-only** overlay contains the five real Minecraft
classes needed to reproduce declared access widening: Projectile constructor,
ItemEntity.pickupDelay, LivingEntity.lastHurtByPlayerTime, and BlockEntityType /
BlockEntitySupplier inner visibility. Their method bodies and signatures were not
changed, and the original cached jar was not modified. This loader-environment
adaptation is disclosed; the result is **not** a clean Gradle acceptance gate.

Temporary proof files are under `/tmp/opencode/a2quality/`: `javac-all.args`,
`classpath.txt`, `javac-final.log`, `regression-final.log`, `SymbolInventory.java`,
`symbols.tsv`, `all-static-fields.tsv`, and `inventory.py`.

The default `java` binary is Java 25, while `javac` is Java 21. Compiler-tree
inventory and final regression execution explicitly used
`/usr/lib/jvm/java-21-openjdk-amd64/bin/java` after observing that mismatch.

```text
CornerRegression: 100000 bit-identical cases; 2 pinned override cases; 22 pouch layout cases
exit 0
```

The randomized comparison includes NaN, infinities, signed zero, negative samples
and empty arrays. It proves refactor equivalence, not independent original-game
acceptance. The two .875/full-corner examples are fixed oracle checks.

Actual resource command:

```text
python3 scripts/porting/convert_assets.py --verify
{"atlas_mipmap_level_guard":4,"atlas_sprites_available":518,
 "checked_atlas_sprite_refs":3943,"checked_local_model_refs":3018,
 "checked_model_texture_refs":3943,"checked_sound_event_ids":66,
 "checked_sound_event_refs":0,"checked_sound_refs":111,"missing_references":0}
```

Its zero excludes Java-composed assets, shaders/OBJ dependencies and server
corpora. It is not proof of §3.8 compliance.

## Decisions for the leader

1. **423 retained test-only exports:** choose production ownership, move genuinely
   historical/test-only rules out of the runtime surface, or explicitly retain
   compatibility hooks. A mass delete would invalidate many tests and risks API
   damage; a mass wire could alter behavior. Disconnected unit proof is not live
   feature proof.
2. **Tunnel rendering:** `ModelTunnelField` still uses shrinking geometry rather
   than projected texgen, and retains RNG across calls; original resets per face.
   Mirror shader demonstrates this is an implementation gap, not an impossible
   modern port. Needs a renderer change plus visual acceptance, not comment edits.
3. **Nine remaining duplicate pairs and 30 magic-number sites:** shared palette,
   biome aura facts and renderer recipes need narrow shared owners. Ray draw
   accumulation and different aspect blend adapters must not be conflated.
4. **Six state findings:** API caches, recursive static scratch, per-world FX and
   compass maps, trunk links and mutable sample arrays need explicit lifecycle /
   thread owners. No active client/server buffer race was invented where callers
   were actually client- or server-gated.
5. **Five error-handling findings:** direct nozzle debit followed by a throwing
   addon is the most important failure-injection target. Broad catches in world
   generation currently turn unexpected errors into absent features. Corpus
   fail-fast on broken installs should remain fail-fast.
6. **Resource verifier:** manifest/enumerate composed resource families and include
   shader/OBJ/corpus dependency checks. “missing_references: 0” currently omits the
   very code paths that failed during renderer work.
7. **Aura centre equivalence:** `AuraGenRules:80` needs official vanilla finder /
   component-source arbitration. One-node-per-temple accounting does not prove
   identical placement.

No full JUnit, Gradle, dedicated-server or client run was performed by this lane.
None is claimed. Quality acceptance is **not green**.
