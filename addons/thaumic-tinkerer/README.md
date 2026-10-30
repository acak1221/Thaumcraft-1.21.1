# Thaumic Tinkerer for Thaumcraft 4 Reload (Minecraft 1.21.1)

A port of **Thaumic Tinkerer** (1.7.10, by Vazkii, pixlepix, nekosune and the GTNewHorizons contributors) to Minecraft 1.21.1 as an
add-on of **Thaumcraft 4 Reload**. Mod id `thaumictinkerer`; a separate jar that depends on `thaumcraft`, `architectury` and (on Fabric)
`fabric-api`. It talks to Thaumcraft only through the public `thaumcraft.api`; a JUnit test (`TinkererApiOnlyTest`) fails the build if
a source file imports anything else of Thaumcraft.

Русская версия: [README.ru.md](README.ru.md).

> **Licence: CC BY-NC-SA 3.0** (see `LICENSE` and `NOTICE.md`). Non-commercial, share-alike. The rest of this repository is MIT; this
> directory is not, except `fabric.gradle` and `tools/`.

## Status

Fabric first (as for the whole project); NeoForge is **not** built for the add-on yet.

| Part | State |
|---|---|
| Eight feature groups (simple blocks, tile blocks, machines, basic items, foci, enchantments, Kami gear, Kami world) | ported, reviewed; GameTests exist for every group. What is **recorded as run** is uneven: the documents of Kami world, Foci and (first round only) Kami gear record runs with numbers; the Machines and Basic items documents record no result of a run; the Enchantments document says that its tests ran, without a result. The one record that covers all eight is a GameTest results file, `fabric/build/tinkerer-gametest-results.xml` (2026-10-30 10:29): 298 cases and no failure, error or skipped entry. It is not tied to a console log, and to the current tree only by file time stamps (`docs/HOOKS.md` section 12, "What ran") |
| Loader hooks (`LIVING_DAMAGE`, `LIVING_ARMOR_ABSORB`, `BREAK_SPEED`, jump, fall, item-use tick, drops, floating ticks, item renderers) | written for Fabric (Mixin); checks exist (JUnit and GameTests, `docs/HOOKS.md` section 12). The results file above lists the 14 hook GameTests, the two of `LIVING_ARMOR_ABSORB` among them, without a failure; no document records a run of the hook JUnit tests; **no NeoForge poster is written** |
| The groups that needed the hooks (Enchantments, Kami world, Kami gear, Machines, Foci) | **rewired** to them on Fabric, their workarounds deleted (`docs/HOOKS.md` section 10). Runs recorded in the group documents: Kami world 20 GameTests (together with the hook GameTests of its review round, 33 of 33) and 38 JUnit checks; Foci 36 GameTests; Kami gear a pass for its first round only (30 JUnit tests of the group plus `TinkererApiOnlyTest`, and 30 GameTests; the group has 34 JUnit tests and 36 GameTests now); Machines no result; Enchantments "ran" without a result. No totals are added up here |
| Research tree and recipes | registered through the Thaumcraft API; compared with the 1.7.10 source by script and by a GameTest (next row) |
| Parity with the 1.7.10 source | `docs/PARITY_REPORT.md` (runs of 2026-10-30): 74 research entries and 127 recipes compared field by field with the original, and 17 object tags; **11 documented differences, 0 undocumented**; Fabric only (a GameTest on a running server: all 7 required tests passed in the run recorded there; NeoForge not run) |
| NeoForge | not started: no module and no build file of the add-on, and none of the hook posters is written, so the Soul Hearts, the shard drops, the enchantment effects and the other event-based behaviour would not happen there. That is read from the code: the add-on has not been built or run on NeoForge (`docs/HOOKS.md` section 9, `docs/DEVIATIONS.md` "Not ported") |
| Client (models, screens, particles, sounds, HUD) | compiled and covered by resource tests; **not yet seen in a running client** |
| Recipe-viewer pages, Jade tooltips, ComputerCraft / OpenComputers peripherals, golem API | not ported (see `docs/DEVIATIONS.md`, "Not ported") |

Nothing in this table is claimed beyond what a document or a results file records as run. The group documents differ in how much they record (first rows); where a document records no run, this table says so.

## Layout

```
common/src/main      loader-neutral code, data (assets/, data/), language files
common/src/test      JUnit tests (no world)
common/src/gameTest  runtime checks used by the GameTests
fabric/src/main      Fabric entry points, Mixins, Fabric item-renderer backend
fabric/src/gameTest  GameTest wrappers and the Fabric GameTest mod
fabric.gradle        wires the add-on into the :fabric Gradle project (source sets, runs, jar tasks)
tools/compile.sh     fast javac compile outside Gradle
docs/                porting rules, deviations, API requests and gaps, hooks, parity report
lang-extra/          language fragments merged into the language files
```

The registry layer is in `thaumic.tinkerer.common.registry` (`TTRegistry`, `TTRecipes`), the content in
`thaumic.tinkerer.common.content` (one registrar per group, listed in `ContentGroups`).

## Build and test

From the repository root (Java 21, Gradle wrapper):

```bash
./gradlew :fabric:tinkererTest                 # JUnit tests of the add-on
./gradlew :fabric:runTinkererGameTest          # all GameTests on a Fabric GameTest server
./gradlew :fabric:runTinkererGameTest -PtinkererGameTestFilter='simpleblocksgametests.*'   # one group or test (comma list, trailing * allowed)
./gradlew :fabric:remapThaumicTinkererJar      # the release jar: fabric/build/libs/ThaumicTinkerer-fabric-<version>.jar
./gradlew :fabric:runTinkererClient            # a development client with Thaumcraft and the add-on
```

`runTinkererGameTest` writes `fabric/build/tinkerer-gametest-results.xml`. The generated world lives in
`fabric/run-tinkerer-gametest/` and is git-ignored.

## Where to read next

- `docs/PORTING_RULES.md` – the rules every file follows (naming, data components, licence headers, API-only imports).
- `docs/DEVIATIONS.md` – every difference from the 1.7.10 mod, grouped, with pointers to the per-group documents in `docs/deviations/`.
- `docs/API_GAPS.md`, `docs/api-requests/` – what the add-on needed from the Thaumcraft API and how each request was answered.
- `docs/HOOKS.md` – the loader events and how they are posted.
- `docs/PARITY_REPORT.md` – the research/recipe comparison with the original source.
