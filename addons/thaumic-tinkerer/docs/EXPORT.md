# The standalone repository of the add-on

Thaumic Tinkerer is developed inside the Thaumcraft 4 Reload monorepo (`addons/thaumic-tinkerer`) and is also built from its own
repository, <https://github.com/acak1221/Thaumic-Tinkerer-1.21.1> (Fabric only; there is no NeoForge build of the add-on). The standalone
repository is **generated** from the monorepo by `tools/export-standalone.sh`; until it takes over (below), the monorepo is the place of
development and nobody edits the standalone tree by hand. This document says how the export works, how to sync, how to switch the
direction, and what has been checked.

## What is where

| In the monorepo | In the standalone repository |
|---|---|
| `addons/thaumic-tinkerer/` (`common/`, `fabric/`, `docs/`, `lang-extra/`, `tools/`, `fabric.gradle`, `LICENSE`) | the same files at the root |
| `addons/thaumic-tinkerer/standalone/` (the template: files that exist only in the standalone repository) | at the root: `settings.gradle`, `build.gradle`, `gradle.properties`, `.gitignore`, `.github/workflows/ci.yml`, `README.md`, `README.ru.md`, `NOTICE.md`, `LICENSE-MIT`, `tools/publish-thaumcraft.sh`, `tools/porting.sh`, `scripts/porting/README.md` |
| `gradlew`, `gradlew.bat`, `gradle/wrapper/`, `.gitattributes` of the monorepo root | the same files (Gradle 8.12.1; the line-ending rules) |
| `scripts/porting/{tt_assets.py,tt_assets_map.json,tt_oracle.py,test_tt_assets.py,test_tt_oracle.py}` (MIT) | `scripts/porting/` (same files, unchanged) |
| `addons/thaumic-tinkerer/tools/export-standalone.sh`, the `standalone/` directory | not exported |
| the build of Thaumcraft itself (`build.gradle`, `fabric/build.gradle` with its last line `apply from: .../fabric.gradle`) | none: Thaumcraft is a dependency, `thaumcraft:Thaumcraft-fabric:<thaumcraft_version>` from the local Maven repository |

The template holds only what differs. The add-on tree is never duplicated inside the monorepo: the export script is the only thing that copies.
`README.md`, `README.ru.md` and `NOTICE.md` of the template replace the ones of the add-on directory (those describe the monorepo layout);
the "Status" table of the template READMEs must equal the one of the add-on READMEs (apart from `fabric/build/` becoming `build/`), and the
script refuses to export when it does not (`-f` overrides), so that the standalone repository never states an older status than the monorepo.

## One `fabric.gradle`, two builds

`fabric.gradle` is applied by `fabric/build.gradle` in the monorepo and by the root `build.gradle` of the standalone repository. It finds the
add-on directory from its own location (`buildscript.sourceFile`), uses `sourceSets.main` only through its output and compile/runtime classpath, and
excludes build output and local state from the inputs of `tinkererTest`. In the monorepo `sourceSets.main` is Thaumcraft's own Fabric code; in the
standalone it is the empty `main` source set of the single project, whose classpath carries the Thaumcraft jar (`modImplementation
thaumcraft:Thaumcraft-fabric:$thaumcraft_version`, `mavenLocal()`), Minecraft, Fabric Loader, Fabric API and Architectury. The task names are the
same without the `:fabric:` prefix. Differences that remain:

* the version of the add-on is `mod_version` of the monorepo (the Thaumcraft version) there and `mod_version` of the standalone `gradle.properties`
  here (both `0.1.0-alpha.7` now);
* the Mixin refmap in the jar is named by Loom from the archives name (and the project name of a subproject): `thaumicTinkerer-Thaumcraft-fabric-fabric-refmap.json` in the
  monorepo, `thaumicTinkerer-ThaumicTinkerer-refmap.json` in the standalone repository; nothing depends on the name, the mixin config in the jar points to it;
* `check` also runs `tinkererTest` in the standalone repository (in the monorepo `check` compiles the three source sets and `:fabric:tinkererTest` is run by name);
* the add-on needs Thaumcraft built from the **branch** `claude/hopeful-hamilton-v8m5pi` of <https://github.com/acak1221/Thaumcraft-1.21.1>, because
  `thaumcraft.api` has extensions that exist only there. The standalone build pins a **commit** of that branch, `thaumcraft_ref` in its `gradle.properties` (a
  branch moves, a build should not): `tools/publish-thaumcraft.sh` clones or updates the checkout, moves it to that commit and runs
  `./gradlew :fabric:publishToMavenLocal` in it, and the CI checks out the same commit. Move `thaumcraft_ref` and `thaumcraft_version` (the `mod_version` of that
  commit) together when the add-on needs something newer from Thaumcraft. Without the Thaumcraft jar in the local Maven repository the standalone build stops at
  configuration time with a message that names `tools/publish-thaumcraft.sh`; the group `thaumcraft` is looked up in the local Maven repository only.

## How to sync

In the monorepo, after the add-on changed:

```bash
addons/thaumic-tinkerer/tools/export-standalone.sh /path/to/Thaumic-Tinkerer-1.21.1      # the clone of the standalone repository
addons/thaumic-tinkerer/tools/export-standalone.sh -n /path/to/Thaumic-Tinkerer-1.21.1   # dry run: what would change
```

then commit and push in the standalone clone by hand (the script never runs git there and never touches `.git`). **Push the standalone repository before any
release jar is published:** `fabric.mod.json` names <https://github.com/acak1221/Thaumic-Tinkerer-1.21.1> as the
homepage, the sources and the issue tracker, and that page has no code until the first export is committed and pushed there.

* **Idempotent.** The new tree is assembled in a temporary directory and compared with the target file by file (content and executable bit); only
  differing files are written, files that no longer exist in the monorepo are deleted, empty directories left behind are removed. A second run
  prints `no changes`.
* **Local state is left alone**: `.git/`, `.gradle/`, `build/`, `out/`, `run/`, `run-*/`, `.tmp/`, `.idea/`, `.vscode/` in the root; `logs/`, `crash-reports/`,
  `.mixin.out/`, `__pycache__/` at any depth; the files `*.log`, `*.pyc`, `*.hprof`, `*~`, `*.swp`, `.DS_Store`. So building in the target does not make the next
  export delete, report or count anything. The same names are never exported from the monorepo (one list, `list_files` in the script, serves the export, the removals and the digest).
* **Marker.** `EXPORTED-FROM-MONOREPO.txt` records the monorepo commit and branch, whether its worktree had uncommitted changes in the exported paths
  (an export of a modified worktree says so; so does a `git status` that failed), and the digest of the exported tree. It is written last, so an export that
  was cut short leaves a directory that the next run refuses instead of one that looks complete.
* **The script only writes into a directory that it made** (exit code 1 and nothing written, otherwise):
  * the target is empty (a clone with only `.git` counts as empty) or holds a marker, and the marker is a regular file whose first line is the one this script
    writes. A file of that name that is empty, a link, or has other text does not make a directory a target;
  * the files of a marked target are still the ones of the last export: the script computes the digest of the files that are there (local state not counted)
    and compares it with the `tree_sha256` of the marker. Hand edits, files added by hand, a marker copied from another directory and an earlier cut-short export
    all differ and are refused; `-n` reports the difference and goes on, `-d` overwrites and removes. A changed executable bit is not a difference
    (it is restored);
  * the target is not inside the monorepo and does not contain it, is not `/`, is not empty text, and has no `..` after a part that does not exist yet (the
    checks could not tell where it leads; a `..` in the part that exists is resolved by the shell). After the directory is made, the script checks that it
    resolves to itself;
  * a marked target holds no symbolic link to a directory or to nothing (the export would write through it). A link to a file is a stale file: it is removed
    and what it points to stays.
* **Nothing of Thaumcraft 4 travels**: the script stops if the new tree would contain a `.jar` other than the Gradle wrapper, a `thaumcraft_src`
  path or a `.class` file.
* Options: `-n` dry run, `-f` export although the README status tables differ, `-d` export although the target differs from its last export.
* Exit codes: 0 done or nothing to do; 1 refused; 2 usage or environment (also bash older than 4.4, an empty target); 3 the README status tables differ.

## Making the standalone repository the primary one

When the standalone repository takes over, in this order:

1. Run the export one last time into a clean clone and commit it there. From now on edit only the standalone repository.
2. Delete `EXPORTED-FROM-MONOREPO.txt`, `docs/EXPORT.md` (or rewrite it as a note on the history) and the sentence "Where this is developed" of both READMEs
   in the standalone repository, and move `thaumcraft_version` and `thaumcraft_ref` in its `gradle.properties` to a Thaumcraft release that contains the API extensions, once there
   is one. (If the marker is forgotten and the standalone repository is edited, the next export from the monorepo refuses it, see above; nothing is overwritten
   unless somebody passes `-d`.) Remove `tools/porting.sh` and the layout shim if the Python tools are moved to their final layout (they address the add-on as
   `addons/thaumic-tinkerer/` today; the monorepo copies are the originals).
3. In the monorepo, delete `addons/thaumic-tinkerer` (the add-on tree, `standalone/`, the export script), the last line of `fabric/build.gradle` (`apply from: ... fabric.gradle`),
   the `tinkererGameTest` and `tinkererClientSmoke` entries of the `configureTestIsolation` call in `fabric/build.gradle`, the `.gitignore` lines for the
   add-on, and the Python tools in `scripts/porting` that only the add-on used (`tt_assets.py`, `tt_assets_map.json`, `tt_oracle.py` and their tests).
4. **Keep in the monorepo** the extensions of `thaumcraft.api` that the add-on needs (`docs/API_GAPS.md` and `docs/api-requests/` say which and why) and
   the tests that guard them: the add-on builds against the published Thaumcraft jar, so removing them would break it. Keep publishing
   `thaumcraft:Thaumcraft-fabric` (`./gradlew :fabric:publishToMavenLocal`, `docs/api/PORTING.md`).
5. Point `.github/workflows/ci.yml` at a released Thaumcraft instead of the commit `thaumcraft_ref` when there is one.

## What has been checked

Run on 2026-10-30 in the development machine (Linux, JDK 21, no GPU, no GitHub Actions), for the monorepo commit `36a450c` plus the uncommitted changes that
made the export (the marker of an export says which commit and whether the worktree was modified). Two rounds: the build of the export and the first version of the
script, then the fixes after a review of the export (a clean-room build, a hygiene pass, a pass over the monorepo changes).

**Round 1, the first export**

* **Standalone, built from a clean export** (an empty directory outside the monorepo, Thaumcraft installed by `./gradlew :fabric:publishToMavenLocal` of the monorepo):
  `check` (the three source sets compile, the Mixin refmap guard passes, `tinkererTest` runs), `tinkererTest` (364 tests, 0 failures, 0 errors, 0 skipped),
  `runTinkererGameTest` ("All 299 required tests passed"), `remapThaumicTinkererJar` and `assemble` (`build/libs/ThaumicTinkerer-fabric-0.1.0-alpha.7.jar`),
  `exportTinkererClasspath` with `tools/compile.sh -g` (javac, no error), `tools/client-smoke.sh` (harness COMPLETED, 0 FAIL, 0 HARNESS_ERROR, 1 NOT_RUN; the PASS
  count was 727 and 725 in two runs, see below).
* **Monorepo, the same build files after the change** (`--rerun --no-build-cache`): `:fabric:tinkererTest` (364 tests, 0 failures), `:fabric:runTinkererGameTest`
  ("All 299 required tests passed"), `:fabric:remapThaumicTinkererJar`, `:fabric:compileThaumicTinkererJava`, `...ClientSmokeJava` and `...GameTestJava`, and
  `tools/client-smoke.sh` (726 PASS, 0 FAIL, 1 NOT_RUN).
* **The two release jars** have the same 1298 entries and byte-identical contents (class files, `fabric.mod.json`, `META-INF/LICENSE`, the refmap content) except the
  name of the refmap file and the `"refmap"` value in `thaumictinkerer.mixins.json` that points to it (see "One `fabric.gradle`, two builds").
* The number of passed client-smoke checks is not constant: the pass/info split of the `up=true` states of the three element fire blocks (`fire_chaos`, `fire_earth`,
  `fire_water`; the share of pixels that differ from the empty stage is random between runs) moves it by one or two (725, 726, 727 in runs of both builds). Nothing else differed.
* `scripts/porting` in the export, through `tools/porting.sh`: 153 unit tests OK with the pinned 1.7.10 checkout and a Thaumcraft checkout at hand;
  `tt_oracle.py --check` ("the checked-in oracle is current") and `tt_assets.py --check` ("check: ok (329 files)").

**Round 2, after the review fixes**

* **Standalone**, an export outside the monorepo, with the command line of the CI step, `--continue check runTinkererGameTest remapThaumicTinkererJar --no-build-cache`:
  BUILD SUCCESSFUL; `tinkererTest` 364 tests, 0 failures, 0 errors, 0 skipped; "All 299 required tests passed"; `tools/compile.sh -g` compiled 420 classes. The
  untracked files of that tree after the build are exactly the exported files (build output is ignored).
* **Monorepo**: `:fabric:tinkererTest --rerun`, `:fabric:runTinkererGameTest`, `:fabric:remapThaumicTinkererJar --rerun` and the three compile tasks `--rerun`, all in one
  Gradle run without the build cache: BUILD SUCCESSFUL; 364 tests, 0 failures; "All 299 required tests passed". The client smoke was **not** repeated after the fixes
  (they changed comments in `tools/client-smoke.sh`, the run directory of the `tinkererClient` dev client and documents).
* **Against a Thaumcraft built from a clean checkout of the pinned commit** (`git clone` of the monorepo, `git checkout 36a450c405c6...`,
  `./gradlew :fabric:publishToMavenLocal -Dmaven.repo.local=<empty directory>`: BUILD SUCCESSFUL, 2 minutes; its jar has the same 6457 entries with the same CRC for every
  entry as the jar from the working tree, except the name of one `architectury_inject_...` directory): `tinkererTest` 364 tests, 0 failures. The GameTests failed once in
  the first run, `theEnchantmentsAnswerForAnyLivingAttackerAndItsMainHandOnly` ("a healthy zombie is not boosted: 15.0 (expected 10.0)"); four runs of the Enchantments group and four full runs
  after it passed. A later monorepo run failed in another test, `anActivatedBladeClearsTheEquipmentToo` (an "Uncommon Treasure" among the drops of a zombie that was killed).
  **Cause: champions.** Thaumcraft rolls a champion for the hostile mobs of a whitelist (zombie, enderman, blaze, skeleton, spider, witch, ...) that enter a world
  (`ChampionHooks.onEntityAdded`); a champion has +30 maximum health and a loot bag among its drops. A zombie set to 20 health is 20 of 50 and below half, where Valiance answers
  (10 x 1.5 = 15). Measured with a scratch GameTest (not kept): 32 of 3000 zombies spawned by `GameTestHelper.spawn` were champions on NORMAL difficulty, 0 of 3000 spawned by the
  new helper. **Fix:** `PlainMobs` in the GameTest sources spawns like `GameTestHelper` and spawns again when the mob has a modifier on its maximum health (what a champion
  has and a mob of a fresh world never has); it is used at every `spawn` of a whitelisted mob in the tests (the eight zombie sites, and the `tough` helper of the hooks tests, which also makes
  the blaze). Mobs made with `EntityType.create` and never added to a level are not rolled and were left alone, as were the species that are not on the whitelist (pig, cow, creeper, ...).
  After the fix: "All 299 required tests passed" in the monorepo run below and, in the standalone export, in the run with the CI command line and in four more full runs (five
  of five, none failed). Five runs say little about a 1 in 100 per spawn; what makes the tests safe is that no test spawn of a whitelisted mob can be a champion any more.
* **Thaumcraft missing**: `--offline -Dmaven.repo.local=<empty directory> tasks` in the export stops at once with the message that names `tools/publish-thaumcraft.sh`
  ("BUILD FAILED in 1s"); with Thaumcraft installed `tasks --offline` is "BUILD SUCCESSFUL".
* **The export script**, in scratch directories, a battery of 42 checks (all passed, run against the final script): a `..` after a part that does not exist (the case that deleted files of an unmarked directory
  before the fix) refused, also towards the monorepo and with an existing prefix; an empty target, `/`, a file, a dangling link refused; a dot component accepted;
  an empty marker, a marker that is a link, a real marker copied into a foreign directory, hand edits and files added by hand refused (and unchanged afterwards, names, sizes
  and times), the same with `-n` reported and with `-d` overwritten and removed; a symbolic link to a directory refused without touching its target, a stale link to a file removed with
  the file it points to kept; nested `common/logs`, `crash-reports/`, `.mixin.out/`, `*.hprof`, `hs_err_pid*.log`, `build/`, `.gradle/`, `run-*/`, `__pycache__`
  and `.git` of a marked target left alone and not counted; a restored executable bit; a second run `no changes`; the existing exports (with `build/`, `.gradle/`, `logs/` in them)
  accepted by the digest of their old marker and updated. Not tested: bash older than 4.4 (the comparison was run on sample version numbers only), and that the marker is written last
  (read from the code, not provoked by a cut-short run).
* `tools/publish-thaumcraft.sh`: its git steps (clone, update, an unknown ref refused, an explicit branch, a missing `thaumcraft_ref` refused with exit 2) against a local clone with
  `GRADLE_CMD=true`; the default ref is the commit of `gradle.properties` and the checkout was detached at it.
* `tools/porting.sh` with `TT_1710_SOURCE`: 153 tests OK with a link to the pinned checkout; with an empty directory the 17 tests against the real checkout are skipped (the override is
  what the copies of the tools see); a path that is not a directory exits with 2; `tt_oracle.py --check` without `--source` OK.
* `.github/workflows/ci.yml`: parsed; the shell of its "Read the Thaumcraft commit" step run on its own with an empty input (reads `gradle.properties`) and with a ref given.
* The ignore patterns of the standalone `.gitignore`, with a temporary git directory: `*.hprof`, `crash-reports/`, `hs_err_pid*`, `.mixin.out/`, `eclipse/`, `*.launch`, `*.ipr`,
  `*.iws`, `*.swo` and the dev client run directory are ignored, `README.md` is not. In the monorepo `fabric/run-tinkerer-client/` (the new `runDir`) is covered by the existing line.

**Not checked:** the CI workflow itself (`.github/workflows/ci.yml` was parsed and its shell run on its own, never run on GitHub Actions); `tools/publish-thaumcraft.sh` against
GitHub and with a real `publishToMavenLocal` through the script (the Gradle command it ends with was run by hand on a clean clone); a clone from GitHub rather than from the local
repository (the same commit hash, so the same tree); a run on another operating system; a build without the retry wrapper around `./gradlew` that the development machine uses for its Gradle
downloads; NeoForge (no build exists).
