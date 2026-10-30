# Thaumic Tinkerer для Thaumcraft 4 Reload (Minecraft 1.21.1)

Порт **Thaumic Tinkerer** (1.7.10; авторы Vazkii, pixlepix, nekosune и участники GTNewHorizons) на Minecraft 1.21.1 как аддон к
**Thaumcraft 4 Reload**. Идентификатор мода `thaumictinkerer`; это отдельный jar, зависящий от `thaumcraft`, `architectury` и
(на Fabric) `fabric-api`. С Thaumcraft аддон разговаривает только через публичный `thaumcraft.api`: JUnit-тест
`TinkererApiOnlyTest` роняет сборку, если какой-нибудь исходник импортирует что-то ещё из Thaumcraft.

English version: [README.md](README.md).

> **Лицензия: CC BY-NC-SA 3.0** (см. `LICENSE` и `NOTICE.md`). Некоммерческая, с условием распространения на тех же условиях. Ей подчинены
> исходники аддона. Файлы сборки этого репозитория (`settings.gradle`, `build.gradle`, `gradle.properties`, `fabric.gradle`, `tools/`,
> workflow CI, `scripts/porting/`) под MIT (`LICENSE-MIT`); Gradle wrapper — собственная работа Gradle (Apache-2.0); их перечень в `NOTICE.md`.

> **Где ведётся разработка.** Этот репозиторий — сгенерированная копия: аддон развивается в `addons/thaumic-tinkerer`
> [монорепозитория Thaumcraft 4 Reload](https://github.com/acak1221/Thaumcraft-1.21.1), пока этот репозиторий не станет основным, а
> `tools/export-standalone.sh` монорепозитория записывает это дерево (`EXPORTED-FROM-MONOREPO.txt` называет коммит). Пока это так, файлы здесь не
> правят вручную: следующий экспорт отказывается работать с изменённым деревом (и затирает правки, только если ему так велено). См. `docs/EXPORT.md`.

## Состояние


Сначала Fabric (как и для всего проекта); сборка под NeoForge для аддона **ещё не сделана**.

| Часть | Состояние |
|---|---|
| Восемь групп содержимого (простые блоки, блоки с TileEntity, машины, базовые предметы, фокусы, зачарования, Kami-снаряжение, Kami-мир) | перенесены, проверены ревью; GameTests есть у каждой группы. То, что **записано как запущенное**, неравномерно: документы Kami-мира, фокусов и (только первый раунд) Kami-снаряжения фиксируют запуски с числами; документы машин и базовых предметов не фиксируют результата запуска; документ зачарований говорит, что его тесты запускались, но без результата. Единственная запись, охватывающая все восемь групп, — файл результатов GameTest `build/tinkerer-gametest-results.xml`. В файле от 2026-10-30 10:29 было 298 случаев и ни одного элемента failure, error или skipped, и он не был связан с консольным логом. Более поздние запуски дерева (2026-10-30, логи Gradle со строкой «All 299 required tests passed»; логи не закоммичены) записали 299 случаев, снова без failure, error и skipped (блоки с TileEntity: тогда 34, теперь 35) (`docs/HOOKS.md`, раздел 12, «What ran») |
| Слой хуков (`LIVING_DAMAGE`, `LIVING_ARMOR_ABSORB`, `BREAK_SPEED`, прыжок, падение, тик использования предмета, дроп, floating-тики, рендереры предметов) | написан для Fabric (Mixin); проверки существуют (JUnit и GameTests, `docs/HOOKS.md`, раздел 12). Файл результатов выше перечисляет 14 GameTests хуков, среди них два для `LIVING_ARMOR_ABSORB`, без сбоев; ни один документ не фиксирует запуск JUnit-тестов хуков; **отправка событий для NeoForge не написана** |
| Группы, которым нужны хуки (зачарования, Kami-мир, Kami-снаряжение, машины, фокусы) | **переведены** на хуки на Fabric, обходные пути удалены (`docs/HOOKS.md`, раздел 10). Запуски, записанные в документах групп: Kami-мир — 20 GameTests (вместе с GameTests хуков его раунда ревью, 33 из 33) и 38 JUnit-проверок; фокусы — 36 GameTests; Kami-снаряжение — прохождение зафиксировано только для первого раунда (30 JUnit-тестов группы плюс `TinkererApiOnlyTest` и 30 GameTests; сейчас у группы 34 JUnit-теста и 36 GameTests); машины — без результата; зачарования — «запускались» без результата. Общих итогов здесь не складываем |
| Дерево исследований и рецепты | регистрируются через API Thaumcraft; сверены с исходником 1.7.10 скриптом и GameTest (следующая строка) |
| Паритет с исходником 1.7.10 | `docs/PARITY_REPORT.md` (прогоны 2026-10-30): сверены по полям 74 записи исследований и 127 рецептов с оригиналом, а также 17 тегов объектов; **11 задокументированных отличий, 0 незадокументированных**; только Fabric (GameTest на запущенном сервере: в записанном там прогоне пройдены все 7 обязательных тестов; NeoForge не запускался) |
| NeoForge | не начат: нет ни модуля, ни файла сборки аддона, и ни одна из реализаций отправки событий не написана, поэтому Soul Hearts, дроп осколков, эффекты зачарований и прочее поведение на событиях там не работали бы. Это следует из чтения кода: аддон под NeoForge не собирался и не запускался (`docs/HOOKS.md`, раздел 9, `docs/DEVIATIONS.md`, «Не перенесено») |
| Клиент (модели, экраны, HUD) | **просмотрен в запущенном dev-клиенте Fabric** под Xvfb с программным рендерингом (Mesa llvmpipe), `docs/CLIENT_VERIFICATION.md`: последний прогон стенда записал 726 пройденных проверок, 0 провалов, 1 не запущенную (подсказка режима инструмента требует нажатия клавиши), 80 снимков, английский и русский; дефекты, найденные первым прогоном, исправлены, прогон повторён. **Не просмотрено:** видеокарта, звук, ввод (клики, клавиши, перетаскивание), мультиплеер, другие моды (EMI, JEI, Jade), NeoForge, оригинальный клиент 1.7.10 для сравнения (`docs/CLIENT_VERIFICATION.md`, «What it cannot show») |
| Страницы в просмотрщиках рецептов, подсказки Jade, периферия ComputerCraft / OpenComputers, API големов | не перенесены (см. `docs/DEVIATIONS.md`, «Не перенесено») |

Ничего из этой таблицы не заявлено сверх того, что документ или файл результатов фиксирует как запущенное. Документы групп
записывают запуски в разном объёме (первые строки); где документ не фиксирует запуска, таблица так и говорит.

## Что нужно

* **JDK 21** (любого поставщика; сборка использует toolchain Java 21) и git. Gradle wrapper (8.12.1) лежит в репозитории.
* **Thaumcraft 4 Reload, собранный из нужного аддону коммита.** Аддон использует расширения `thaumcraft.api`, которые есть только в ветке
  `claude/hopeful-hamilton-v8m5pi` репозитория <https://github.com/acak1221/Thaumcraft-1.21.1>; Thaumcraft из другой ветки (или релиз без них) аддон
  не скомпилирует. `thaumcraft_ref` в `gradle.properties` — коммит этой ветки, с которым аддон собирается и проверяется (CI берёт его же).
  Сборка берёт Thaumcraft из локального Maven-репозитория (`~/.m2`, `mavenLocal()`) как
  `thaumcraft:Thaumcraft-fabric:<thaumcraft_version>` (`gradle.properties`, сейчас `0.1.0-alpha.7`). Сначала установите его, см. ниже; сборка без него
  останавливается сообщением об этом.
* Доступ в интернет к Maven Central, `maven.fabricmc.net`, `maven.architectury.dev` и Mojang (Minecraft, ассеты) при первой сборке.
* Память: демон Gradle настроен на 3 ГБ (`org.gradle.jvmargs` в `gradle.properties`), а сервер GameTest и клиентский осмотр запускают каждый свою JVM с
  `-Xmx3G` (`fabric.gradle`), поэтому на время `runTinkererGameTest` и `tools/client-smoke.sh` рассчитывайте примерно на 6 ГБ свободной памяти.
* Только для клиентского осмотра: Linux с `xvfb-run`, программным рендерингом Mesa (видеокарта не нужна) и `python3` (скрипт печатает с его помощью
  сводку `report.json`). Только для `scripts/porting`: Python 3.

## Сборка и тесты

Все команды выполняются в корне этого репозитория.

```bash
# 1. Thaumcraft 4 Reload в ~/.m2 (один раз; снова — когда изменился thaumcraft_ref в gradle.properties)
tools/publish-thaumcraft.sh                  # клонирует ../Thaumcraft-1.21.1 на thaumcraft_ref и запускает там ./gradlew :fabric:publishToMavenLocal
#    вручную:  git clone https://github.com/acak1221/Thaumcraft-1.21.1 ../Thaumcraft-1.21.1
#              cd ../Thaumcraft-1.21.1 && git checkout <thaumcraft_ref> && ./gradlew :fabric:publishToMavenLocal

# 2. Аддон
./gradlew tinkererTest                       # JUnit-тесты аддона
./gradlew runTinkererGameTest                # все GameTests на тестовом сервере Fabric
./gradlew runTinkererGameTest -PtinkererGameTestFilter='simpleblocksgametests.*'   # группа или тест (список через запятую, * в конце допустима)
./gradlew remapThaumicTinkererJar            # релизный jar: build/libs/ThaumicTinkerer-fabric-<версия>.jar
./gradlew check                              # компилирует наборы main, GameTest и client-smoke и запускает tinkererTest
./gradlew runTinkererClient                  # клиент для разработки с Thaumcraft и аддоном
tools/client-smoke.sh                        # клиентский осмотр под Xvfb, около 2 минут после первой компиляции (docs/CLIENT_VERIFICATION.md)
./gradlew exportTinkererClasspath && tools/compile.sh [-g]   # javac вне Gradle для быстрого цикла правка-компиляция (-g: вместе с исходниками GameTest)
```

* **Как выглядит хороший запуск** (0.1.0-alpha.7, 2026-10-30; числа меняются, когда добавляются тесты, важно отсутствие сбоев):

  | Команда | Результат |
  |---|---|
  | `./gradlew tinkererTest` | `BUILD SUCCESSFUL`; файлы в `build/test-results/tinkererTest/` дают в сумме 364 теста, 0 сбоев, 0 ошибок, 0 пропущенных |
  | `./gradlew runTinkererGameTest` | строка лога `All 299 required tests passed :)` и `BUILD SUCCESSFUL`; в `build/tinkerer-gametest-results.xml` 299 элементов `testcase` и ни одного `failure` или `error` |
  | `./gradlew remapThaumicTinkererJar` | `BUILD SUCCESSFUL` и `build/libs/ThaumicTinkerer-fabric-0.1.0-alpha.7.jar` (около 2,2 МБ) |
  | `./gradlew check` | `BUILD SUCCESSFUL` (три набора исходников компилируются, проверка refmap Mixin проходит, `tinkererTest` запускается) |
  | `tools/client-smoke.sh` | `client-smoke: harness COMPLETED; checks {PASS: ..., FAIL: 0, HARNESS_ERROR: 0, NOT_RUN: 1, ...}` и 80 снимков в `build/tinkerer-client-smoke/`; PASS был 725–727 в четырёх запусках (три проверки блоков огня зависят от случайных пикселей), FAIL 0 и NOT_RUN 1 не менялись |
* Релизный jar — `build/libs/ThaumicTinkerer-fabric-<версия>.jar` (`mod_version` в `gradle.properties`; его же собирает `./gradlew assemble`). Он
  переотображён для продакшена, несёт `LICENSE` в `META-INF/` и содержит refmap Mixin. Чтобы играть с ним, положите его в `mods/` рядом с jar
  Thaumcraft 4 Reload, Architectury API 13.0.11+ и Fabric API.
* `runTinkererGameTest` пишет `build/tinkerer-gametest-results.xml`; его мир лежит в `run-tinkerer-gametest/` (удаляется в начале каждого запуска).
  Отчёты JUnit — в `build/reports/tests/tinkererTest/`, результаты клиентского осмотра — в `build/tinkerer-client-smoke/`. Всё это игнорируется git.
* `tools/client-smoke.sh` запускает `./gradlew` под `xvfb-run`; переменная окружения `GRADLE_CMD` заменяет команду Gradle (см. заголовок скрипта).
* Компиляция падает, если процессор аннотаций Mixin не записал refmap или записал refmap без записи для миксина с инжектором (`fabric.gradle`).
* Документы в `docs/` написаны в монорепозитории. Где в них `./gradlew :fabric:tinkererTest`, здесь `./gradlew tinkererTest` (без `:fabric:`);
  `fabric/build/` — это `build/`, `fabric/run-tinkerer-*` — `run-tinkerer-*`, а `addons/thaumic-tinkerer/` — корень этого репозитория.
  Заголовки лицензии Java-файлов ссылаются на `addons/thaumic-tinkerer/NOTICE.md`: здесь это `NOTICE.md`.
* Файлы, на которые ссылаются документы и которые принадлежат **репозиторию Thaumcraft 4 Reload**, а не этому: `AGENTS.md`, `docs/api/` (`PORTING.md`,
  `LEGACY_IDS.md`, руководства), `docs/MIGRATION_STATUS.md`, `common/src/main/java/thaumcraft/` и `common/src/main/resources/*/thaumcraft/` (исходники и ресурсы Thaumcraft; `docs/API_GAPS.md` сокращает их до `API/` и `INT/`), `fabric/build.gradle`
  и `neoforge/build.gradle`. Результаты, которые документы приводят по запускам собственных наборов тестов Thaumcraft (JUnit 2761, 515 GameTest,
  296 Python-тестов, `docs/DEVIATIONS.md` раздел 5), — запуски монорепозитория и здесь не воспроизводятся; раздел 5 говорит, какие строки это.
* В `scripts/porting/` лежат Python-инструменты, которыми конвертировались ресурсы 1.7.10 и извлекался оракул паритета (MIT, скопированы из
  монорепозитория; см. их `README.md`). Им нужен checkout исходников оригинала 1.7.10; `tools/porting.sh` запускает их для этой структуры.

## Структура

```
common/src/main      код и данные (assets/, data/), не зависящие от загрузчика; языковые файлы
common/src/test      JUnit-тесты (без мира)
common/src/gameTest  проверки времени выполнения для GameTests
fabric/src/main      точки входа Fabric, Mixin, реализация рендереров предметов для Fabric
fabric/src/gameTest  обёртки GameTest и тестовый мод Fabric
fabric/src/clientSmoke  стенд клиентского осмотра (инструмент разработки, в jar не попадает)
build.gradle         проект: Loom, Minecraft, Fabric, Architectury и jar Thaumcraft
fabric.gradle        наборы исходников, запуски и задачи аддона (тот же файл, что монорепозиторий подключает из проекта :fabric)
tools/               compile.sh, client-smoke.sh, publish-thaumcraft.sh, porting.sh
scripts/porting/     Python-инструменты конвейера ресурсов и оракула (MIT)
docs/                правила переноса, отступления, запросы к API и пробелы, хуки, отчёт о паритете, клиентская проверка, экспорт
lang-extra/          фрагменты локализации, сливаемые в языковые файлы
```

Слой реестров — `thaumic.tinkerer.common.registry` (`TTRegistry`, `TTRecipes`), содержимое —
`thaumic.tinkerer.common.content` (по регистратору на группу, список в `ContentGroups`).

## Что читать дальше

- `docs/PORTING_RULES.md` — правила, которым следует каждый файл (имена, компоненты данных, заголовки лицензий, импорты только из API).
- `docs/DEVIATIONS.md` — все отличия от мода 1.7.10 по группам, со ссылками на документы групп в `docs/deviations/`.
- `docs/API_GAPS.md`, `docs/api-requests/` — что аддону понадобилось от API Thaumcraft и как закрыт каждый запрос.
- `docs/HOOKS.md` — события загрузчика и как они отправляются.
- `docs/PARITY_REPORT.md` — сверка исследований и рецептов с исходником оригинала.
- `docs/CLIENT_VERIFICATION.md` — стенд клиентского осмотра и то, чего он показать не может.
- `docs/EXPORT.md` — как этот репозиторий связан с монорепозиторием.
