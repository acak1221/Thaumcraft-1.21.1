# Полный аудит проекта (Fabric-first порт TC4 → Minecraft 1.21.1), 19.10.2026

Статус: аудит снимка `3995fe6` (единственный коммит ветки; docs ссылаются на ветку `port/minecraft-1.21.1` и HEAD
`ac1734af`, которых в этой истории нет). Сделан Claude по запросу «проанализируй всё». Исходники не менялись до
раздела 6; все числа получены подсчётом или реальным запуском в контейнере (Linux, Java 21.0.12), а не из docs.

Маркеры: **implemented** — код есть; **built** — собрано; **game-tested** — Fabric dedicated-server GameTest;
**seen** — игровой клиент реально осмотрен; **unverified** — не проверялось.

## 1. Проверка сборки в этом контейнере (шаг B)

Холодный кэш Gradle (`GRADLE_USER_HOME=<repo>/.gradle21`, wrapper 8.12.1, `--no-daemon`), NeoForge не запускался.

| Команда | Результат |
|---|---|
| `./gradlew :common:test` | `BUILD SUCCESSFUL in 9m 32s`, exit 0; 47 классов, **384 теста, 0 failures / 0 errors / 0 skipped** (по XML-отчётам) |
| `./gradlew :fabric:remapJar :fabric:runGameTest` | `BUILD SUCCESSFUL in 1m 38s`, exit 0; сервер: «**All 151 required tests passed**»; `remapJar` вызывает `verifyProductionArchive` (изоляция теста от продакшн-JAR) |
| Fabric JAR | `Thaumcraft-fabric-0.1.0-alpha.1.jar`, SHA-256 `516d9ec3dc579849ca6d059cc6ad32b71f109e4a46bf73459302800b27e8d766` (локальная сборка, не экспортирована) |

Вывод: заявленные в docs **384 JUnit и 151 GameTest подтверждены независимо** на чистом клиенте репозитория.
Это *implemented + built + game-tested*; **seen — нет**. В логе GameTest присутствуют стектрейсы — это ожидаемые
исключения негативных сценариев внутри прошедших тестов (итоговая строка сервера: 151/151).
Не запускалось: `runClientSmoke` (нужны GPU/Xvfb и приватные сейвы), NeoForge, `scripts/porting/*.py`.

## 2. Состав

| Область | Факт |
|---|---|
| `common/src/main/java` | 421 файл, 49 457 LOC; ресурсы 1 761 файл (16 МБ); 64 блока, ≈200 предметов, 23 типа BE, 7 типов сущностей, 6 меню, 11 data-components, 3 типа рецептов |
| Тесты | JUnit 47 классов/384 теста (6,6 k LOC); `common/src/gameTest` 86 файлов (26,6 k LOC); 151 `@GameTest` в `fabric/src/gameTest` |
| Fabric | `main`: 5 файлов/291 LOC (бутстрап + 3 платформенных моста); `clientTest`: 75 файлов/27,5 k LOC (smoke-инструменты) |
| NeoForge | 4 java-файла/532 LOC; 63 GameTest против 151 у Fabric |
| Legacy | `src/` 3 140 файлов (1.12-справочник, не собирается), `thaumcraft_src/` 1 096 файлов (только ассеты из JAR оригинала) |
| Мир | 3 worldgen-фичи (руды, обычные узлы, великодревесные/серебряные деревья); локализация en/ru (в оригинале 22) |

Тестовая обвязка (≈62 k LOC) крупнее продакшн-кода (≈49 k LOC); `clientTest` — именно тот smoke-инструмент, развитие
которого пользователь остановил 18.10.

## 3. Полнота по milestone

Ни один milestone M00–M15 не закрыт (docs это признают). Scope-ledger: 1 044 строки, все `needs_review`, 34 привязаны
к нативному коду; `class-inventory.json`: complete = 0 (снимок 15.10: 324 файла, сейчас 427 — устарел).

| M | Состояние | Не сделано (главное) |
|---|---|---|
| 00 | частично | админ-команды (только `status`, `aspects`, `research grant`), версионирование сейвов |
| 01 исследования | лучшее | R10–R16: копирование, unknown scrolls, clues, режимы сложности, deconstruction; скан существ |
| 02 | частично | PURE-узлы, spider dungeon, retrogen |
| 03 | частично | 3 стержня/3 колпачка; нет staff/sceptre и спец-типов узлов (только NORMAL) |
| 04 | частично | 47 arcane / 25 crucible / 9 infusion рецептов; поздние гейты |
| 05 | частично | Thaumatorium, brain/node jar |
| 06 | широко | reservoir, адд-он I/O, ускорение кристаллизатора |
| 07 | частично | только stack-рецепты инфузии; enchant/runic/tag-patch нет |
| 08 | частично | 4 фокуса (Fire, Frost, Shock, Excavation) из ≈15; pouch, Repair, runic, void/fortress-броня |
| 09 | частично | стабилизатор и VisNet приняты; Bore — provisional; Hungry Chest — built, не seen |
| 10 големы | в работе | 12 ядер, 8 материалов, 6 улучшений есть; нет: advanced-голем, декорации, Traveling Trunk, Fetter, Entropy (нечёткое сравнение, очередь Liquid), горящий улов |
| 11–15 | не начато | warp-события, мобы (0 мобов TC), taint, Outer Lands, API-совместимость, packaged multiplayer |

## 4. Риски и несоответствия

1. **Доказательства не воспроизводимы из репозитория.** `.tmp/` пуст; `axe-stream-build.ps1`/`axe-stream-gametest.ps1`
   и все «accepted»-артефакты отсутствуют; `dev.ps1` и Bore client-runner — только Windows; `dev.sh validate --smoke`
   (GOAL.md) не существует. Для кода — смягчено разделом 1; для клиентских (seen) утверждений проверки нет.
2. **Дрейф документации.** README («211 JUnit / 95 Fabric», «последний срез — стабилизатор») и
   `docs/porting/verification.json` (238/113, 17.10) отстают от факта 384/151; работа после focal (Hungry Chest,
   големы, улучшения) в verification.json не записана; `VALIDATION.md` говорит о 41 GameTest; `GOAL.md` — закрытая цель
   Forge 1.12/Java 8, противоречит AGENTS.md; `settings.gradle`: `rootProject.name = 'Thaumcraft-4.2-FOREVA'`; шаг CI
   «both native GameTest servers» при Fabric-only `validate`; HANDOFF.md 548 КБ с шапкой 16.10.
3. **Верификация уже кода.** Hungry Chest, големы и улучшения: implemented/built/game-tested, seen = нет; только
   одиночный серверный GameTest (нет сети с двумя игроками, нет реального save/reload мира); Bore-экспорт
   `PROVISIONAL…FINAL_VALIDATION_DEFERRED`, native16 отклонён.
4. **Код.**
   - **Подтверждённый дефект: блоки `crystal_*` (6 штук) ничего не роняют при добыче** — нет loot-table и `getDrops`.
     В оригинале `BlockCrystal.getDrops` (декомпиляция CFR вне репозитория): md 0–5 → **6 осколков** своего типа,
     md 6 → по осколку каждого из шести, md 7 (Eldritch) → 1 осколок №6; Silk/Fortune не учитываются. Блоки участвуют
     в бонусе среды исследовательского стола (`TileResearchTable:271`).
   - Граница публичного API нарушена: `api/visnet/VisNetHandler` и `api/wands/ItemFocusBasic` импортируют `common.*`.
   - `EntityGolemBase` — 2 486 LOC; 365 строк длиннее 160 символов; ≈4 000 строк комментариев-провенанса.
   - 28 миксинов в нестабильные внутренности (chunk/ticker/entity-manager) — хрупко при смене версии Minecraft.
   - Данные исследований/аспектов читаются из classpath, не через datapack-reload.
   - NeoForge-модуль (по чтению кода, запуском не проверено): `Thaumcraft.init()` и `ClientSetup.init`; нет аналога
     Fabric-регистрации атрибутов голема (`FabricDefaultAttributeRegistry`), мостов Bore/Harvest/Use
     (`BorePlatform.required()` бросит исключение), use-block хуков. Корневой `build` и `Dockerfile` всё ещё собирают NeoForge.
5. **Правовое/провенанс.** В git лежат два JAR (оригинал 1.7.10 и TC6 1.12 — 12,6 и 11,4 МБ), `thaumcraft_src/`
   (ассеты Азанора), 3 140 файлов legacy. Репозиторий **приватный** (проверено через GitHub API), THIRD_PARTY_NOTICES
   прямо пишет, что права на публикацию не установлены — перед любой публикацией/форком это нужно вычистить. LICENSE
   (MIT) не покрывает чужие ассеты.
6. **Мелочи.** Устаревший shadow-плагин `com.github.johnrengelman`; строка `%SystemDrive%/` в `.gitignore`; 4 из 5
   skills в `.opencode/` — про Forge 1.12.

## 5. Рекомендуемая очередь (Fabric, без NeoForge)

1. Закрыть найденные дефекты (раздел 6).
2. Остаток M10: Entropy (нечёткое сравнение предметов, очередь Liquid) → Fetter + advanced-голем → декорации → Traveling Trunk.
3. M01 R10–R16; M03/M08 (жезлы, фокусы); M07 enchant-инфузия; Thaumatorium.
4. M11–M12 — следующая крупная «стена» (warp, мобы, taint, Outer Lands).
5. Обновить verification.json/README/HANDOFF по фактическим числам; NeoForge — после завершения Fabric.

## 6. Исправлено в этом проходе

Оракул для контракта получен локальной декомпиляцией CFR 0.152 (SHA-1 `48ef4892…f74`, Maven Central) из
`Thaumcraft-1.7.10-4.2.3.5.jar` в каталог вне репозитория; JAR не менялся, декомпиляция в коммиты не входит.

**D1. Дроп блоков `crystal_*` (implemented / built / game-tested).** Шесть `data/thaumcraft/loot_table/blocks/crystal_<тип>.json`:
`survives_explosion` + безусловная запись `thaumcraft:<тип>_shard` с `set_count 6` (оригинал `BlockCrystal.getDrops`:
6 осколков своего типа, Silk Touch и Fortune игнорируются). Не портированы кристаллы `md 6` (смешанный, шесть разных
осколков) и `md 7` (Eldritch, один осколок №6): таких блоков в порту нет.
Тесты: `CrystalLootTableTest` (чистый JUnit по содержимому loot-table) и `crystalBlocksDropSixOwnShards`
(`CrystalDropRuntimeChecks`: `Block.getDrops` с пустой рукой/Silk/Fortune III и реальное `destroyBlock`, считаются
выпавшие `ItemEntity`). **Мутация:** без шести JSON оба теста падают (JUnit FAILED; GameTest «1 required tests failed» из 152,
`BUILD FAILED`); с файлами — оба проходят.

**D2. Граница публичного API (implemented / built / game-tested).** `api/visnet/VisNetHandler` больше не импортирует
`thaumcraft.common.*`: локальный логгер с прежним именем `"Thaumcraft"` и `VisNetHandler.EffectSink`, который
`Thaumcraft.init()` связывает с `VisNetFxNetworking::send` сразу после `initServer()`. Поведение и порядок вызовов не меняются.
`api/wands/ItemFocusBasic` **не тронут**: он зависит от `FocusUpgradeData` и компонента `ThaumcraftFoci.FOCUS_UPGRADES`,
то есть для развязки нужно переносить тип данных между пакетами (AGENTS.md: сохранять имена пакетов, где возможно) —
оставлено как документированное вынужденное отклонение.

**Итог проверок после правок** (`:common:test :fabric:remapJar :fabric:runGameTest`, exit 0, `BUILD SUCCESSFUL in 1m 32s`):
**385 JUnit** (48 классов, 0 failures/errors/skips) и **152/152 GameTest** — на единицу больше базовых 384/151 за счёт
новых тестов. Seen — нет; NeoForge не запускался.
