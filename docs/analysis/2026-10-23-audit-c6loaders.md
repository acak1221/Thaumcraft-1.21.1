# c6loaders audit handoff — 2026-10-23

Branch/worktree: `agent/c6loaders`, `/home/cyberfox-kali/wt/c6loaders`. Started clean at `3340abe7`. No other worktree or main repository was accessed; original behavior was read only from `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/`.

Deliverable: [`../audit/LOADER_QUALITY.md`](../audit/LOADER_QUALITY.md). It includes all eight checklist sections, a 32-group loader table, all 142 mixin rows, supported-key/default/side/sync inventory, registration counts, menu review and explicit verification limitations.

## Quantitative outcome

| Checklist | Inspected | Findings |
|---|---|---:|
| Loader parity | 19 Fabric + 29 NeoForge files; 32 feature groups | 3 open |
| Mixin integrity | 9 configs, 142 classes, 84 targets, 154 artifact-specific checks | 0 |
| Containers | 27 files, 17 menus, 6 reusable slot classes | 0 confirmed |
| Configuration/data | 62 original keys, 34 supported, 25 editor entries; 774 data JSON | 4 open |
| Registration | 209 common owners; 635 call sites; 851 expanded declarations | 2 Critical open |
| Size/duplication | 48 loader classes; 0 over 250 lines; 9 mirrored lists | 1 open |
| Dead code | 201 concrete method declarations before / 199 after cleanup | 2 fixed |
| Errors/logging | 14 catch sites; both loader bootstraps | 3 open |

**15 findings: 2 fixed, 13 open. Severity: 2 Critical, 1 High, 9 Medium, 3 Low.** The two Critical findings are distinct duplicate registry IDs, not two descriptions of the same collision.

## Parent session: prioritize these

1. **Critical:** `ThaumcraftContent.java:162` and `ThaumcraftWands.java:50` both register item `thaumcraft:thaumium_nugget`. The content owner uses behavioral `ItemNugget`; wand owner uses plain `Item`.
2. **Critical:** same collision for `thaumcraft:void_nugget`, at content line 164 / wands line 54. Both shared registrars are called on both loaders. Consolidate suppliers and creative-tab publication; do not remove the behavioral item just because the older wand supplier exists.
3. **High:** NeoForge never calls `GuiSpa.setFluidAppearance`; rendering a filled spa tank reaches the required non-null bridge at `GuiSpa.java:42`.
4. **Medium parity:** NeoForge inhabited zombie uses unrestricted placement; Fabric now uses ground placement and its inhabited-specific predicate. NeoForge's explanatory comment and the source-parity test's seven-entry assertion are stale.
5. **Medium parity:** Fabric golem fluid lookup reports the first nonblank resource but sums all storage views, while NeoForge reads tank zero only. Define a consistent multi-tank projection before patching.

Other open issues: runtime/editor/default schema drift (9 supported keys absent from GUI); 18 meaningful original editable options ignored by the properties reader; stale public `Config` field mirrors; raw properties writer does not preserve escaped values; 9 duplicated lists; broad silent fluid-provider catches; silent reflection failures in worldgen setup; production INFO login diagnostic instrumentation.

## Safe fixes committed

Removed two unused loader-private loot operations and their interface declarations:

- `NeoForgeLootPoolAccess.thaumcraft$entries` / `NeoForgeLootPoolMixin` implementation.
- `NeoForgeLootTableAccess.thaumcraft$setPools` / `NeoForgeLootTableMixin` implementation.

The live first-pool append and read access remain. Removed the unused `ArrayList` import and unnecessary mutable shadow from the table mixin, and corrected replacement-oriented comments. Four production files changed. No gameplay values, registration ownership, config keys, callbacks or mixin-config entries were changed.

## Verification actually performed

- Isolated Java 21 `javac --release 21 -proc:none` of all four changed production files, real remapped NeoForge-patched 1.21.1 jar first on classpath, `common/build/classes/java/main`, and cached dependency jars under `~/.gradle-leader/caches/modules-2/**`: **exit 0**.
- **Important:** local common build output is absent. These four classes do not use common classes, so the compile is valid for the changed classes only. No full loader/common build is claimed.
- `javap -p -s -c` against cached remapped vanilla 1.21.1 and NeoForge 21.1.259 patched Minecraft; Architectury 13.0.11 and cached compiled mod classes for non-Minecraft targets: **0 missing target classes, 0 missing required selectors, 0 missing required invocation strings**. The absent NeoForge milk invocation is documented and `require=0` by design. Cached shared mod bytecode is not a fresh build of this worktree.
- All 142 mixin sources/config entries checked; all nine configs appear in applicable production or test mod metadata. All nine `defaultRequire=1`.
- Existing `NeoForgeLoaderParityTest` compiled with cached JUnit jars and its 12 annotated methods invoked through an ignored reflection runner: **11 pass, 1 fail** (`theSpawnPlacementCatalogueIsTheFabricOne`: expects 7, finds 8). No Gradle invoked.
- All 774 data JSON files parse. Expanded source registry identity checks resolve 1,034 required own-namespace tag / item-field / item-loot references: **0 missing**. All 66 sound keys match the 66 registered names.
- `git diff --check`: clean.

Ignored evidence/scripts: `.tmp/c6-mixins.py`, `.tmp/c6-mixins.json`, `.tmp/c6-javap.json`, `.tmp/c6-inventory.py`, `.tmp/c6-registers.json`, `.tmp/c6-expand.py`, `.tmp/c6-expanded.json`, `.tmp/c6-data.json`, `.tmp/c6-javac.log`, `.tmp/c6-parity-tests.log`, `.tmp/c6-classes/`.

Not run: Gradle, production boot, NeoForge/Fabric clients, actual mixin transformation, server smoke, multiplayer quick-move/dupe scenarios or full original→flattened registration-name certification. Menu conclusions are reviewed-source conclusions, not runtime acceptance.

## Corrections to preliminary candidates

- Small-taintacle attributes are registered on **both** loaders; not a finding.
- Prior NeoForge reload deadlock: barrier signaling is already present.
- Prior missing builtin item renderers: current lists match; no re-registration added.
- Taint-fibre “gap” text in the source test is stale: the shared position-model path handles fibres.
- No standalone sound interception callback is registered in current Fabric production bootstrap either; the historical missing-NeoForge-callback issue was not reproduced as current loader asymmetry.
