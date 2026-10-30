# Thaumic Tinkerer for Thaumcraft 4 Reload (Minecraft 1.21.1)

A port of **Thaumic Tinkerer** (1.7.10, by Vazkii, pixlepix, nekosune and the GTNewHorizons contributors) to Minecraft 1.21.1 as an
add-on of **Thaumcraft 4 Reload**. Mod id `thaumictinkerer`; a separate jar that depends on `thaumcraft`, `architectury` and (on Fabric)
`fabric-api`. It talks to Thaumcraft only through the public `thaumcraft.api`; a JUnit test (`TinkererApiOnlyTest`) fails the build if
a source file imports anything else of Thaumcraft.

Русская версия: [README.ru.md](README.ru.md).

> **Licence: CC BY-NC-SA 3.0** (see `LICENSE` and `NOTICE.md`). Non-commercial, share-alike. The add-on sources are under it. The build
> files of this repository (`settings.gradle`, `build.gradle`, `gradle.properties`, `fabric.gradle`, `tools/`, the CI workflow,
> `scripts/porting/`) are MIT (`LICENSE-MIT`); the Gradle wrapper is Gradle's own (Apache-2.0); `NOTICE.md` lists them.

> **Where this is developed.** This repository is a generated copy: the add-on is developed in `addons/thaumic-tinkerer` of the
> [Thaumcraft 4 Reload monorepo](https://github.com/acak1221/Thaumcraft-1.21.1) until this repository takes over, and
> `tools/export-standalone.sh` of the monorepo writes this tree (`EXPORTED-FROM-MONOREPO.txt` says from which commit). Do not edit files here by
> hand while that is so: the next export refuses a tree that was edited (and overwrites the edits only when told to). See `docs/EXPORT.md`.

## Status


Fabric first (as for the whole project); NeoForge is **not** built for the add-on yet.

| Part | State |
|---|---|
| Eight feature groups (simple blocks, tile blocks, machines, basic items, foci, enchantments, Kami gear, Kami world) | ported, reviewed; GameTests exist for every group. What is **recorded as run** is uneven: the documents of Kami world, Foci and (first round only) Kami gear record runs with numbers; the Machines and Basic items documents record no result of a run; the Enchantments document says that its tests ran, without a result. The one record that covers all eight is a GameTest results file, `build/tinkerer-gametest-results.xml`. The file of 2026-10-30 10:29 had 298 cases and no failure, error or skipped entry and was not tied to a console log. Later runs of the tree (2026-10-30, Gradle logs with the line "All 299 required tests passed"; the logs are not committed) wrote 299 cases, again without a failure, error or skipped entry (Tile blocks: 34 then, 35 now) (`docs/HOOKS.md` section 12, "What ran") |
| Loader hooks (`LIVING_DAMAGE`, `LIVING_ARMOR_ABSORB`, `BREAK_SPEED`, jump, fall, item-use tick, drops, floating ticks, item renderers) | written for Fabric (Mixin); checks exist (JUnit and GameTests, `docs/HOOKS.md` section 12). The results file above lists the 14 hook GameTests, the two of `LIVING_ARMOR_ABSORB` among them, without a failure; no document records a run of the hook JUnit tests; **no NeoForge poster is written** |
| The groups that needed the hooks (Enchantments, Kami world, Kami gear, Machines, Foci) | **rewired** to them on Fabric, their workarounds deleted (`docs/HOOKS.md` section 10). Runs recorded in the group documents: Kami world 20 GameTests (together with the hook GameTests of its review round, 33 of 33) and 38 JUnit checks; Foci 36 GameTests; Kami gear a pass for its first round only (30 JUnit tests of the group plus `TinkererApiOnlyTest`, and 30 GameTests; the group has 34 JUnit tests and 36 GameTests now); Machines no result; Enchantments "ran" without a result. No totals are added up here |
| Research tree and recipes | registered through the Thaumcraft API; compared with the 1.7.10 source by script and by a GameTest (next row) |
| Parity with the 1.7.10 source | `docs/PARITY_REPORT.md` (runs of 2026-10-30): 74 research entries and 127 recipes compared field by field with the original, and 17 object tags; **11 documented differences, 0 undocumented**; Fabric only (a GameTest on a running server: all 7 required tests passed in the run recorded there; NeoForge not run) |
| NeoForge | not started: no module and no build file of the add-on, and none of the hook posters is written, so the Soul Hearts, the shard drops, the enchantment effects and the other event-based behaviour would not happen there. That is read from the code: the add-on has not been built or run on NeoForge (`docs/HOOKS.md` section 9, `docs/DEVIATIONS.md` "Not ported") |
| Client (models, screens, HUD) | **seen in a running Fabric dev client** under Xvfb with software rendering (Mesa llvmpipe), `docs/CLIENT_VERIFICATION.md`: the last run of the harness recorded 726 checks passed, 0 failed, 1 not run (the tool-mode tooltip needs a key press), 80 pictures, English and Russian; the defects the first run found were fixed and the run repeated. **Not seen:** a GPU, sound, input (clicks, keys, drag), multiplayer, other mods (EMI, JEI, Jade), NeoForge, the original 1.7.10 client for comparison (`docs/CLIENT_VERIFICATION.md`, "What it cannot show") |
| Recipe-viewer pages, Jade tooltips, ComputerCraft / OpenComputers peripherals, golem API | not ported (see `docs/DEVIATIONS.md`, "Not ported") |

Nothing in this table is claimed beyond what a document or a results file records as run. The group documents differ in how much they record (first rows); where a document records no run, this table says so.

## What you need

* **JDK 21** (any vendor; the build uses a Java 21 toolchain) and git. The Gradle wrapper (8.12.1) is in the repository.
* **Thaumcraft 4 Reload, built from the commit the add-on needs.** The add-on uses extensions of `thaumcraft.api` that exist only on the branch
  `claude/hopeful-hamilton-v8m5pi` of <https://github.com/acak1221/Thaumcraft-1.21.1>; a Thaumcraft built from another branch (or a release without
  them) does not compile the add-on. `thaumcraft_ref` in `gradle.properties` is the commit of that branch that the add-on is built and tested against
  (the CI uses it too). The build reads Thaumcraft from your local Maven repository (`~/.m2`, `mavenLocal()`), as
  `thaumcraft:Thaumcraft-fabric:<thaumcraft_version>` (`gradle.properties`, now `0.1.0-alpha.7`). Install it first, below; a build without it stops
  with a message that says so.
* Internet access for Maven Central, `maven.fabricmc.net`, `maven.architectury.dev` and Mojang (Minecraft, assets) on the first build.
* Memory: the Gradle daemon is set to 3 GB (`org.gradle.jvmargs` in `gradle.properties`), and the GameTest server and the client smoke each start a JVM with
  `-Xmx3G` (`fabric.gradle`), so plan for about 6 GB of free memory during `runTinkererGameTest` and `tools/client-smoke.sh`.
* Only for the client smoke: Linux with `xvfb-run`, Mesa software rendering (no GPU needed) and `python3` (the script prints a summary of `report.json`
  with it). Only for `scripts/porting`: Python 3.

## Build and test

All commands run in the root of this repository.

```bash
# 1. Thaumcraft 4 Reload into ~/.m2 (once; again when thaumcraft_ref in gradle.properties changes)
tools/publish-thaumcraft.sh                  # clones ../Thaumcraft-1.21.1 at thaumcraft_ref and runs ./gradlew :fabric:publishToMavenLocal there
#    by hand:  git clone https://github.com/acak1221/Thaumcraft-1.21.1 ../Thaumcraft-1.21.1
#              cd ../Thaumcraft-1.21.1 && git checkout <thaumcraft_ref> && ./gradlew :fabric:publishToMavenLocal

# 2. The add-on
./gradlew tinkererTest                       # JUnit tests of the add-on
./gradlew runTinkererGameTest                # all GameTests on a Fabric GameTest server
./gradlew runTinkererGameTest -PtinkererGameTestFilter='simpleblocksgametests.*'   # one group or test (comma list, trailing * allowed)
./gradlew remapThaumicTinkererJar            # the release jar: build/libs/ThaumicTinkerer-fabric-<version>.jar
./gradlew check                              # compiles the main, GameTest and client-smoke source sets and runs tinkererTest
./gradlew runTinkererClient                  # a development client with Thaumcraft and the add-on
tools/client-smoke.sh                        # the client smoke under Xvfb, about 2 minutes after the first compile (docs/CLIENT_VERIFICATION.md)
./gradlew exportTinkererClasspath && tools/compile.sh [-g]   # javac outside Gradle, for a quick edit-compile loop (-g: with the GameTest sources)
```

* **What a good run looks like** (0.1.0-alpha.7, 2026-10-30; the counts move when tests are added, a failure is what matters):

  | Command | Result |
  |---|---|
  | `./gradlew tinkererTest` | `BUILD SUCCESSFUL`; the files in `build/test-results/tinkererTest/` add up to 364 tests, 0 failures, 0 errors, 0 skipped |
  | `./gradlew runTinkererGameTest` | the log line `All 299 required tests passed :)` and `BUILD SUCCESSFUL`; `build/tinkerer-gametest-results.xml` has 299 `testcase` elements and no `failure` or `error` |
  | `./gradlew remapThaumicTinkererJar` | `BUILD SUCCESSFUL` and `build/libs/ThaumicTinkerer-fabric-0.1.0-alpha.7.jar` (about 2.2 MB) |
  | `./gradlew check` | `BUILD SUCCESSFUL` (the three source sets compile, the Mixin refmap check passes, `tinkererTest` runs) |
  | `tools/client-smoke.sh` | `client-smoke: harness COMPLETED; checks {PASS: ..., FAIL: 0, HARNESS_ERROR: 0, NOT_RUN: 1, ...}` and 80 screenshots in `build/tinkerer-client-smoke/`; PASS was 725 to 727 in four runs (three fire-block checks depend on random pixels), FAIL 0 and NOT_RUN 1 did not move |
* The release jar is `build/libs/ThaumicTinkerer-fabric-<version>.jar` (`mod_version` in `gradle.properties`; it is also what `./gradlew assemble` builds). It
  is remapped for production, carries `LICENSE` under `META-INF/` and holds the Mixin refmap. To play with it, put it into `mods/` next to the
  Thaumcraft 4 Reload jar, Architectury API 13.0.11+ and Fabric API.
* `runTinkererGameTest` writes `build/tinkerer-gametest-results.xml`; its world lives in `run-tinkerer-gametest/` (deleted at the start of every run). The
  JUnit reports are in `build/reports/tests/tinkererTest/`, the client smoke results in `build/tinkerer-client-smoke/`. All of that is git-ignored.
* `tools/client-smoke.sh` runs `./gradlew` under `xvfb-run`; the environment variable `GRADLE_CMD` replaces the Gradle command (see the header of the script).
* The compile step fails if the Mixin annotation processor wrote no refmap or one without an entry for a mixin that has an injector (`fabric.gradle`).
* The documents in `docs/` were written in the monorepo. Where they say `./gradlew :fabric:tinkererTest`, here it is `./gradlew tinkererTest` (drop
  `:fabric:`); `fabric/build/` is `build/`, `fabric/run-tinkerer-*` is `run-tinkerer-*`, and `addons/thaumic-tinkerer/` is the root of this repository.
  The licence headers of the Java files point to `addons/thaumic-tinkerer/NOTICE.md`: that is `NOTICE.md` here.
* Files that the documents cite and that belong to the **Thaumcraft 4 Reload repository**, not to this one: `AGENTS.md`, `docs/api/` (`PORTING.md`,
  `LEGACY_IDS.md`, the guides), `docs/MIGRATION_STATUS.md`, `common/src/main/java/thaumcraft/` and `common/src/main/resources/*/thaumcraft/` (the sources and resources of Thaumcraft; `docs/API_GAPS.md` abbreviates them as `API/` and `INT/`), `fabric/build.gradle`
  and `neoforge/build.gradle`. Results that the documents quote from runs of Thaumcraft's own suites (JUnit 2761, 515 GameTests, 296 Python tests,
  `docs/DEVIATIONS.md` section 5) were monorepo runs and cannot be reproduced here; section 5 says which rows those are.
* `scripts/porting/` holds the Python tools that converted the 1.7.10 resources and extracted the parity oracle (MIT, copied from the monorepo; see its
  `README.md`). They need a checkout of the original 1.7.10 source; `tools/porting.sh` runs them against this layout.

## Layout

```
common/src/main      loader-neutral code, data (assets/, data/), language files
common/src/test      JUnit tests (no world)
common/src/gameTest  runtime checks used by the GameTests
fabric/src/main      Fabric entry points, Mixins, Fabric item-renderer backend
fabric/src/gameTest  GameTest wrappers and the Fabric GameTest mod
fabric/src/clientSmoke  the client smoke harness (a development tool, never in the jar)
build.gradle         the project: Loom, Minecraft, Fabric, Architectury and the Thaumcraft jar
fabric.gradle        source sets, runs and tasks of the add-on (the same file the monorepo applies from its :fabric project)
tools/               compile.sh, client-smoke.sh, publish-thaumcraft.sh, porting.sh
scripts/porting/     Python tools of the resource and oracle pipeline (MIT)
docs/                porting rules, deviations, API requests and gaps, hooks, parity report, client verification, export
lang-extra/          language fragments merged into the language files
```

The registry layer is in `thaumic.tinkerer.common.registry` (`TTRegistry`, `TTRecipes`), the content in
`thaumic.tinkerer.common.content` (one registrar per group, listed in `ContentGroups`).

## Where to read next

- `docs/PORTING_RULES.md` – the rules every file follows (naming, data components, licence headers, API-only imports).
- `docs/DEVIATIONS.md` – every difference from the 1.7.10 mod, grouped, with pointers to the per-group documents in `docs/deviations/`.
- `docs/API_GAPS.md`, `docs/api-requests/` – what the add-on needed from the Thaumcraft API and how each request was answered.
- `docs/HOOKS.md` – the loader events and how they are posted.
- `docs/PARITY_REPORT.md` – the research/recipe comparison with the original source.
- `docs/CLIENT_VERIFICATION.md` – the client smoke harness and what it cannot show.
- `docs/EXPORT.md` – how this repository relates to the monorepo.
