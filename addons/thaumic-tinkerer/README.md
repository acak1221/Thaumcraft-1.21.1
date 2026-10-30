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
| Eight feature groups (simple blocks, tile blocks, machines, basic items, foci, enchantments, Kami gear, Kami world) | ported, reviewed, each group's GameTests run green on a Fabric GameTest server |
| Loader hooks (`LIVING_DAMAGE`, `BREAK_SPEED`, jump, fall, item-use tick, drops, floating ticks, item renderers) | written for Fabric (Mixin), tested; the groups are being rewired to them |
| Research tree (~90 entries) and recipes | registered through the Thaumcraft API; a parity check against the 1.7.10 source is in `docs/PARITY_REPORT.md` |
| NeoForge | not started (the hooks layer is loader-neutral; see `docs/HOOKS.md` section 9) |
| Client (models, screens, particles, sounds, HUD) | compiled and covered by resource tests; **not yet seen in a running client** |
| Recipe-viewer pages, Jade tooltips, ComputerCraft / OpenComputers peripherals, golem API | not ported (see `docs/DEVIATIONS.md`, "Not ported") |

Nothing in this table is claimed beyond what was run; every group's deviation document says what was and was not executed.

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
