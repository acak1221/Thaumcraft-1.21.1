# Аудит нативной сборки Minecraft 1.21.1

Дата: 15.10.2026. Проверен checkout `port/minecraft-1.21.1`, исходный HEAD `8abb7febda5e4aceac5304277b17154760fc1d38`. Начальный `git status --short` был пустым. Работу выполнял отдельный агент; Gradle и игру он **не запускал**. Последующие source edits и финальный Gradle запуск координируются основным агентом.

## Объём

Прочитаны активные Gradle settings/properties/build scripts всех трёх модулей, wrapper properties, оба loader metadata, mixin config, PowerShell/bash entrypoints, оба CI workflow, notices, source lock, verification recorder, asset converter и его проверки. Проверены реальные содержимое/хеши production JAR, существующие JUnit/GameTest XML. Для ресурсов выполнены штатные read-only проверки, а не визуальное сравнение каждого изображения.

`2026-10-15-build-tests.md` описывает старый Forge 1.12.2 build. Его проблемы и ссылки на строки нельзя автоматически применять к новой сборке: старые configs сохранены в `docs/legacy-build/`, активный граф другой.

## Подтверждённая сборочная основа

| Свойство | Проверенный источник | Результат |
|---|---|---|
| Изоляция legacy Java | `settings.gradle:10`, `settings.gradle:11`; `build.gradle:8` | Компилируются только `common`, `fabric`, `neoforge`; root `src/` не добавлен в source sets |
| Target | `gradle.properties:9`; `build.gradle:20` | Minecraft 1.21.1 |
| Java | `build.gradle:23`, `build.gradle:24` | JDK toolchain/release 21 |
| Mapping | `build.gradle:21` | Official Mojang mappings |
| Wrapper | `gradle/wrapper/gradle-wrapper.properties:3`, `:4` | Gradle 8.12.1, distribution SHA-256 закреплён |
| Loader artifacts | `fabric/build.gradle:15`, `:21`, `:22`; `neoforge/build.gradle:14`, `:20`, `:21` | Transformed common → shadow → loader remap |
| Metadata | `fabric/src/main/resources/fabric.mod.json:28`; `neoforge/src/main/resources/META-INF/neoforge.mods.toml:19` | Заявлена ровно версия Minecraft 1.21.1 |
| Runtime mixin | `common/src/main/resources/thaumcraft-common.mixins.json:4`, `:6` | Java 21, PlayerKnowledgeMixin |
| Windows entrypoint | `scripts/dev.ps1:24`, `:41` | Validate включает JUnit, оба builds и оба headless GameTest servers; wrapper запускается напрямую через Java |
| Linux/macOS entrypoint | `scripts/dev.sh:9` | Тот же набор задач |
| CI | `.github/workflows/build.yml:23`, `:24`, `:28`, `:32` | Java 21, Python asset checks, modern validate |
| Публикация | `.github/workflows/release.yml:3`, `:9` | Только ручной вызов validation; publish/upload release actions отсутствуют |

Исходный wrapper JAR имеет SHA-256 `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`, совпадающий с `docs/porting/source-lock.json:40`. Повторное скачивание и независимая online-проверка происхождения в этом аудите не выполнялись.

Git mode `100755` установлен для `gradlew` и `scripts/dev.sh`. `.gitattributes:1`, `:2` закрепляет LF; современный `gradlew.bat` существует. Поэтому старый Windows/Linux blocker из исторического отчёта не описывает текущий tracked layout.

## N01 — отсутствующая атрибуция в JAR: исправлено в исходниках

**Подтверждённый packaging defect.** Fabric metadata `:33` и NeoForge metadata `:3` направляют к `THIRD_PARTY_NOTICES.md`. В двух существовавших при начале аудита remapped JAR не было ни этого документа, ни `LICENSE`. В Gradle scripts не было правила копирования root notices. Пользователь, имеющий только JAR, не получал документа с указанной в metadata атрибуцией.

По поручению основного агента изменён `common/build.gradle:10`: `processResources` включает root `THIRD_PARTY_NOTICES.md` и `LICENSE`, с явными UTF-8 и LF. Это использует штатный общий resource output для обоих loaders и не дублирует тексты в source tree.

В `scripts/porting/record_verification.py:19` добавлена канонизация notices, а в `:26` — проверка ровно одной актуальной копии каждого файла в final JAR. Проверка отвергает отсутствующие, повторённые, устаревшие и CRLF-копии. Полный recorder сохраняет hashes notices в artifact evidence. Отдельный read-only режим `--check-notices` позволяет проверять свежие JAR в CI без `.tmp` runtime logs и без перезаписи verification report.

`scripts/porting/test_record_verification.py` содержит пять in-memory ZIP проверок; **5/5 PASS**, Python 3.14.4, exit code 0. Новый validator отдельно применён к двум старым JAR: оба отклонены с `Artifact must contain exactly one THIRD_PARTY_NOTICES.md`. Это реальное воспроизведение дефекта. Успешное включение notices в пересобранные artifacts должен подтвердить центральный final build; этот агент Gradle не запускал.

## N02 — GameTest support включён в production output: остаётся

Подтверждённое ограничение, уже раскрытое в `docs/MIGRATION_STATUS.md:95`:

- `common/src/main/java/thaumcraft/common/testing/RuntimeChecks.java:31` содержит shared runtime assertions в основном source set.
- `fabric/src/main/resources/fabric.mod.json:19` регистрирует `fabric-gametest`; wrapper находится в `fabric/src/main/java/thaumcraft/fabric/testing/ThaumcraftGameTests.java:8`.
- `neoforge/src/main/java/thaumcraft/neoforge/ThaumcraftNeoForge.java:10`–`:27` импортирует test wrapper и reporter, регистрирует GameTests из production constructor; только замена reporter ограничена GameTest server flag.
- NeoForge network mock находится в `neoforge/src/main/java/thaumcraft/neoforge/testing/NetworkedGameTestPlayer.java:25` и использует testing handshake API в `:42`.
- NeoForge `src/main/resources/data/thaumcraft/structure/empty.nbt` — test template, также входит в main resources.

В проинспектированных binary JAR Fabric содержит `RuntimeChecks` + Fabric wrapper; NeoForge — `RuntimeChecks` + NeoForge wrapper + network mock и `empty.nbt`. JUnit sources из `common/src/test` в JAR отсутствуют. Факт упаковки GameTest support не доказывает сбой обычного server; здесь воспроизведено именно включение лишней тестовой поверхности.

Практическая изоляция возможна без перестройки gameplay: вынести общий helper в `common/src/gametest`, loader wrappers/metadata/template в отдельные gametest source sets/test mod; привязать их runtime classpath только к `loom.runs.gameTest`; NeoForge регистрацию/reporting переместить в test bootstrap. Простого `jar.exclude('**/testing/**')` недостаточно: Fabric entrypoint и NeoForge constructor сохранят ссылки на удалённые классы. На этой итерации перенос source sets **не выполнялся** по решению основного агента, поскольку одновременно расширялись runtime scenarios.

## N03 — пределы доказательства результатов и CI

`scripts/porting/record_verification.py` проверяет exit record, существующие XML и JAR. Его ветви чтения результатов не связывают их с идентичным source commit/fingerprint или временем запуска. Поэтому один запуск recorder после изменения Java не заменяет новую сборку. Это ограничение provenance, а не утверждение о поддельности текущих результатов. Основной агент выполняет final validate после source edits.

В `fabric/build.gradle:25` и `neoforge/build.gradle:23` настроены **development** GameTest runs. Они проверяют mapped runtime и применённые mixins, но не устанавливают remapped production JAR в обычный loader install. Для production JAR в данном аудите установлены структура, metadata и bytecode; обычный packaged-JAR dedicated/client запуск этим агентом **UNRUN**.

Проверка `extract_tc4_definitions.py --check` требует private oracle по `scripts/porting/extract_tc4_definitions.py:353`. Текущий CI её не вызывает; fresh clone не содержит этот oracle намеренно. Сохранённые 48/201 data проверяются foundation tests, но CI не является независимым повторением извлечения оригинального decompile. Ни эта граница, ни отсутствие автоматической публикации не требуют включать private decompile в repository.

## Проверенные artifacts до текущих исправлений

Эти значения получены непосредственно из `build/libs`, а не скопированы из документации; они совпали с исходным `docs/porting/verification.json`. После нового build хеши и размеры должны обновиться.

| Loader | Production file | Bytes | SHA-256 |
|---|---|---:|---|
| Fabric | `fabric/build/libs/Thaumcraft-fabric-0.1.0-alpha.1.jar` | 9 120 836 | `bd7d07c75f9764c04c2c651614c21c3ccb543a66f92e967c07a79b62a95d7cba` |
| NeoForge | `neoforge/build/libs/Thaumcraft-neoforge-0.1.0-alpha.1.jar` | 9 127 201 | `1ec2d991c2874f34feb1f3cabb488cd88d2c28efaa5ea6aecaf7040d5fda1cd5` |

Каждый содержал 43 class entries: 42 класса `thaumcraft/**` с classfile major 65 (Java 21), один генерируемый Architectury `PlatformMethods` с major 52. Последний — platform adapter, не legacy Thaumcraft class. Проверены отсутствие вложенных JAR, Java source entries и классов `net/minecraft`, `net/minecraftforge`, `cpw/mods`; original/donor JAR как вложения отсутствуют. Fabric manifest указывает Minecraft 1.21.1 и namespace `intermediary`; оба loader metadata имеют resolved `0.1.0-alpha.1`.

Имеющиеся GameTest XML содержали 5 cases на Fabric и 5 на NeoForge, без failure/error; JUnit XML суммарно 46 tests, 0 failures/errors/skipped. Это **прочитанное evidence предыдущего запуска**, не повторно выполненные этим агентом Gradle tests. Артефакты являются foundation alpha; количественный успех тестов не подтверждает полного TC4 gameplay.

## Выполненные этим агентом проверки

| Проверка | Результат |
|---|---|
| ZIP/metadata/classfile/hash inspection обоих final JAR | Выполнено read-only; исходные hashes совпали с verification record |
| Wrapper SHA-256 | Совпал с pinned source lock |
| Python `convert_assets.py --verify` | PASS: 492 atlas sprites, 58 texture refs, 9 local model refs, 66 sound events, 111 sound refs, missing 0, mip guard 4 |
| Python `extract_tc4_definitions.py --check` | PASS: 48 aspects, 201 research, 294 ungated text page corpus |
| Python `test_record_verification.py` | 5/5 PASS, 0.004 s, exit 0 |
| Validator против старых JAR | Оба отклонены из-за отсутствующего notices document |
| Gradle, новое runtime evidence, remote CI | Этим агентом не запускались; central verification принадлежит основному агенту |

Не выполнялись public push/release, изменения original/donor JAR, private oracle или legacy `src/`. Изначальные изменения этого агента включали root notices в common resources и их проверку; последующее отдельное поручение добавило описанный ниже изолированный client smoke harness.

## Дополнительная проверка нового Thaumonomicon и реального клиента

По отдельному поручению выполнен read-only review новых `ItemThaumonomicon`, `WandManager`, `EntitySpecialItem`, browser/page screens и network open flow. Целевые API проверялись через `javap` локального mapped Minecraft 1.21.1 JAR и исходники закреплённого Architectury 13.0.11. Gradle по-прежнему запускал только основной агент.

Выявлены и исправлены основным агентом две реальные проблемы:

1. `Font.substrByWidth(FormattedText, int)` возвращает `FormattedText`, который не принимается `GuiGraphics.drawString`; исправлено использование поддерживаемого текстового типа до client launch.
2. В Minecraft 1.21.1 `Screen.render` уже вызывает `renderBackground`. В первом реальном client smoke оба custom screens вызывали background самостоятельно, затем `super.render` в конце. Повторный blur размывал уже нарисованные карту/текст/иллюстрации; кнопки оставались резкими. Дефект был обнаружен просмотром native screenshot и подтверждён target bytecode. Основной агент оставил один явный `super.renderBackground` в начале и no-op override `renderBackground`, чтобы завершающий `super.render` рисовал только widgets.

Для воспроизводимого наблюдения добавлен opt-in **отдельный** Fabric source set `src/clientTest` с test mod `thaumcraft_client_smoke`, run task `:fabric:runClientSmoke` и `runDir=run-clienttest`. Его bootstrap ничего не делает без system property, запускаемой этим task. Обычный `main` JAR не включает этот source set; дополнительная проверка `remapJar` отвергает утечку `thaumcraft/fabric/clienttest/**`.

Preparation task один раз копирует существующий 1.21.1 fixture `fabric/run/saves/New World` в `fabric/run-clienttest/saves/book-smoke`, исключая `session.lock`. Он не удаляет/не переносит source world и отказывается перезаписывать неполную target world. Только изолированные client options сбрасываются перед каждым запуском: English, GUI scale 2, simulation distance 5. Client bootstrap дополнительно умеет восстановить English resources после предыдущего Russian phase.

Сценарий использует `ClientTickEvent.CLIENT_POST`, реального local player и integrated server. Он выдаёт книгу/синхронизирует знания только в изолированной world, вызывает обычный `gameMode.useItem`, получает server-open payload, кликает mapped THAUMONOMICON/RESEARCH entries и native Next/Previous/Back handlers. Ввод ОС, перемещение пользовательского курсора и управление уже открытым NeoForge process не применяются.

Native `Screenshot.grab` сохраняет восемь PNG; JSON записывает transitions, actual screen class, язык, GUI size и page index. Каждый запуск имеет свежий UUID, который Gradle передаёт клиенту и сверяет с JSON. `runClientSmoke` требует `SCRIPT_PASSED` и восемь существующих screenshot files с совпадающими размерами. Прежний success JSON или нормальный process exit после script failure не дают успешный task. Поле `visual_inspection` явно остаётся pending: автоматические assertions не заменяют просмотр изображений.

Второй централизованный запуск `.tmp/session-book-client2.log` завершился `BUILD SUCCESSFUL`, JSON содержит `SCRIPT_PASSED`, run ID `client-smoke-609d16d5-9275-4b93-9512-055995202898`. Этот агент **просмотрел все восемь PNG** из `fabric/run-clienttest/screenshots/`:

| Сцена | Наблюдение |
|---|---|
| `01-browser-en.png` | Карта/заголовок резкие после устранения второго blur |
| `02-thaumonomicon-en.png` | Английский текст книги читается, остаётся внутри страницы |
| `03-research-en.png` | Bold/underline/color markup и rule line видны |
| `04-research-illustrated-en.png` | Illustration `research1.png` отображена с правильным crop, page index 2 |
| `05-research-previous-en.png` | Previous вернул index 0; title/text/page numbers совпадают со сценой 03, что дополнительно подтверждено сравнением pixels |
| `06-browser-ru-small.png` | Русская карта при GUI 427×240; scissor ограничивает viewport |
| `07-thaumonomicon-ru-small.png` | Читаемый русский текст распределён по страницам 1/3, 2/3 |
| `08-thaumonomicon-ru-small-next.png` | Next открыл оставшийся текст на странице 3/3 без выхода за книгу |

Полные имена PNG имеют указанный run ID перед именем сцены; точные absolute paths записаны в `fabric/run-clienttest/client-smoke-result.json`. На маленьком GUI длинная русская подпись Basic Information обрезается шириной стандартной кнопки; полный текст доступен в её tooltip. Частично обрезанные research nodes ожидаемо остаются внутри панорамируемого viewport.

Окончательный третий central smoke `.tmp/session-book-client3.log` выполнен с capture из `ScreenEvents.afterRender` именно ожидаемого screen instance после явного `GuiGraphics.flush`. Tick только запрашивает screenshot; native readback привязан к завершённому render pass. Результат: `SCRIPT_PASSED`, fresh run ID `client-smoke-886de186-422b-416c-af46-c91bbfb44174`, восемь PNG, Gradle `BUILD SUCCESSFUL`. Основной агент просмотрел все восемь точных final files и подтвердил читаемые map/text/illustration, переход Next и восстановление предыдущего разворота, Russian overflow pagination.

Возникшее у вспомогательного агента подозрение о «пустом» screenshot 05 оказалось артефактом inline image display, **не** ошибкой сохранённого PNG. Выполнены независимый stdlib PNG decode и сравнение text ROI: `x=240..609` плюс `680..1019`, `y=40..599`, RGB. В final 03 и 05 ROI идентичны: SHA-256 `af46e350c0a8a0ab2606e976d71705407ba727fcf9e274a8a09b6a4a86f37e4d`, 38 670 pixels с каждым RGB channel ниже 100. Whole-file hashes отличаются из-за прочих областей: final 03 `d6cbf377f05d6e5dd11284add99efaf56fd69e4c23898f013717cc6a3e33b68a`, final 05 `2f5ec79112fed241282a2154156d422ffa61569fba39dd6b2ca92a776ff94a83`. Та же ROI equality подтверждена для второго run. Предположение о blank-page defect отозвано; временные непроверенные диагностические edits удалены, оставлена точная tested afterRender версия. Четвёртый запуск не требовался.

Финальные central XML дополнительно прочитаны этим агентом: 61 JUnit, 9 Fabric GameTests, 9 NeoForge GameTests, failures/errors/skipped 0. Final JAR: Fabric 9 157 889 bytes, SHA-256 `2e57abb09cc8d4381a2580f7fe13957d83464042f265617ead7c0bc4908b6e57`; NeoForge 9 165 196 bytes, SHA-256 `b8a644d6d2410a91e9157b3876a46b0a6c86b3e5e23379d1ce77f8e7b102abbe`. Оба проходят current-notices validator и не содержат `/clienttest/` classes. GameTest support из main, описанный N02, по-прежнему остаётся. Это подтверждение узкого native book/text slice, **не** полный visual/gameplay parity TC4, проверка всех GUI resolutions или NeoForge client evidence.
