# Анализ сборки, CI и тестирования

Дата: 15.10.2026. Базовый коммит: `4bc68454b1063cdc1255f833dd537e0fd4fc341d`, ветка `master`.

## Объём и ограничения

Прочитаны полностью `AGENTS.md`, `build.gradle`, `Dockerfile`, оба GitHub Actions workflow, `gradlew`, Gradle properties, `scripts/dev.sh`, `scripts/tc6-compat.py`, `scripts/tests/test_tc6_compat.py`, Xorg config, corpus/policy manifests и все 14 smoke manifests. `.opencode/goal.md` отсутствует. Выполнена инвентаризация всех Java-тестов и выборочная проверка их содержания. Это анализ всей инфраструктуры сборки и тестирования, а не построчная проверка каждого из 530 Java test files.

Производственный код и оригинальные JAR не изменялись. Начальный `git status --short` был пустым. Коммит не создавался. Полная Java-сборка и Forge smoke здесь не запускались: координация проверок выполнялась основным агентом; Docker daemon в доступной среде не работает. Поэтому утверждения об успешной компиляции, текущем прохождении всех JUnit-тестов и полном игровом паритете не делаются.

## Стек и граф сборки

| Компонент | Значение | Основание |
|---|---|---|
| Minecraft / Forge | `1.12.2-14.23.5.2847` | `build.gradle:33-39` |
| Java | source/target 8 | `build.gradle:31` |
| Gradle wrapper | `4.10.3` | `gradle/wrapper/gradle-wrapper.properties:3` |
| ForgeGradle | `2.3-SNAPSHOT` | `build.gradle:20` |
| MCP | `stable_39` | `build.gradle:36` |
| Обязательная Baubles | Curse file `2518667` | `build.gradle:54` |
| Optional compile APIs | JEI `4.16.1.1012:api`, Hwyla Curse file `2568753` | `build.gradle:55-56` |
| Java tests | JUnit `4.12` | `build.gradle:57` |
| Docker | плавающий `eclipse-temurin:8-jdk` | `Dockerfile:9` |
| Архив проекта | `Thaumcraft-1.0.0-universal.jar` | `build.gradle:26-29,120-123` |
| Объявляемая mod version | `6.1.BETA26`, отдельная от имени архива | `build.gradle:27,38-39,106-111` |

`compileJava` дополнительно меняет байткод `ResearchEntry.class` через ASM: добавляет JVM-дескрипторы `setParents(String[]):void` и `setSiblings(String[]):void`, сосуществующие с TC4 fluent setters. Это осознанный мост JVM ABI, который Java source выразить не может (`build.gradle:60-104`). Важный объект будущей бинарной проверки — наличие обоих return descriptors в итоговом universal/API JAR.

`apiJar` включает `thaumcraft/api/**`, `devJar` содержит compiled output и Java sources; reobfuscation объявлена для основного JAR (`build.gradle:125-148`). Release публикует только universal JAR. `dev.sh validate` явно запускает `compileJava test jar reobfJar`, затем строгую проверку MCP-ссылок (`scripts/dev.sh:1245-1248`). В CI тоже явно вызывается `reobfJar`.

## Выявленные проблемы

### B01 — подтверждённая ошибка определения static method handle

**Приоритет: средний.** `tc6-compat.py` ложно признаёт корректный `REF_invokeStatic` несовместимым. Это ошибка анализатора, а не игрового кода.

При чтении MethodHandle его kind превращается в строку `HANDLE6`, а последний элемент demand остаётся constant-pool member tag `10` или `11` (`scripts/tc6-compat.py:389-399`). При разрешении `expected_static` проверяет `tag in (6,)` вместо вида handle (`:845`). Для `HANDLE6` эта ветка всегда false, поскольку Methodref/InterfaceMethodref не имеют tag 6.

Проверено настоящим вызовом `resolve_demands` с in-memory fixture: публичный статический `thaumcraft/api/Fixture.run()V` и demand `("HANDLE6", owner, "run", "()V", 10)`. Результат:

```text
[('fixture', 'supported', 'METHOD_STATIC_MISMATCH',
  'HANDLE6 thaumcraft/api/Fixture.run()V')]
```

Python 3.14.4, exit code 0; assert подтвердил именно ошибочный результат. В текущем checked-in demand snapshot строк `HANDLE` нет, поэтому текущий corpus этот дефект не активирует. Появление аддона со статической method reference/lambda может дать ложное падение compatibility gate.

Рекомендация: вычислять static по `INVOKESTATIC`/`HANDLE6`, добавить отдельные проверки static и instance handles с Methodref/InterfaceMethodref.

### B02 — подтверждённое несоответствие пути Gradle cache в Docker wrapper

**Приоритет: высокий для воспроизводимой локальной сборки; runtime-сбой не воспроизводился.** Dockerfile создаёт `thaum` с home `/home/thaum` (`Dockerfile:90-96`). Wrapper монтирует cache в `/home/ubuntu/.gradle` и подменяет numeric UID/GID хоста, не задавая `GRADLE_USER_HOME` (`scripts/dev.sh:57-63,71-84`; smoke использует тот же mount в `:758-764`). В самом `gradlew` тоже нет такого override.

Само несовпадение конфигурации подтверждено исходниками. Оно означает, что Gradle не обязан использовать смонтированный cache; при несуществующем numeric user или неудобном `user.home` возможна ошибка прав на его фактический home. Даже если Gradle выполнится, последующий `check-jar` ожидает mappings в host `.gradle_home`, которые могли оказаться в другом месте.

CI отличается: оба workflow явно задают `GRADLE_USER_HOME=/workspace/thaumcraft/.gradle_home` (`.github/workflows/build.yml:50`, `release.yml:113,134,235`). Поэтому нельзя автоматически переносить проблему wrapper на CI build stage.

Рекомендация: согласовать Docker user и writable home/cache, задавать `GRADLE_USER_HOME` одинаково во всех Docker invocations. Не менять Java/Gradle/Forge версии ради исправления путей.

### B03 — подтверждённо неработающие overrides путей cache

**Приоритет: средний.** Два advertised environment overrides поддерживаются не всеми функциями.

- `THAUMCRAFT_GRADLE_HOME` объявлена в `scripts/dev.sh:6,43`, используется build/remap/compat functions, но `mcp_leak_summary` и `check_jar` жёстко читают `$ROOT/.gradle_home/...` (`:458,587`). Build с внешним cache может завершиться, а artifact check — искать mappings в другом каталоге.
- `THAUMCRAFT_SMOKE_DIR` объявлена в `:8,44`, manifest hash checks читают `$SMOKE_CACHE_DIR` (`:370-378`), но Docker remapping читает input `$container_root/.smoke/$jar` (`:260,280`). Для `dev-remap` manifests пользовательский cache принимается подготовкой и затем игнорируется remapper.

Рекомендация: весь путь проводить через одну переменную и явно монтировать внешний smoke cache в контейнер.

### B04 — подтверждённое отличие локального validate от заявленных non-GUI tests

**Приоритет: средний.** `AGENTS.md:130` описывает `validate` как non-GUI tests. На практике `validate` вызывает `test` без `-Pci` (`scripts/dev.sh:1247`), а Docker wrapper не передаёт host `CI` environment в контейнер (`:57-63,73-78,83-84`). В `build.gradle:152-155` client exclusion включается только при `CI == "true"` либо project property `ci`.

Следовательно, обычный локальный `dev.sh validate` включает client tests; простой `CI=true ./scripts/dev.sh validate` тоже не гарантирует их исключение внутри контейнера. Headless failure зависит от реально выполняемых client tests и здесь не проверялся.

CI command lines задают `-Pci=true` явно, поэтому этот конкретный mismatch относится к wrapper. Более точное решение — разделить headless-safe client source guards и настоящие GL/display tests, вместо исключения всего package.

### B05 — подтверждённый дефект final verdict release smoke

**Приоритет: средний.** Release workflow сначала ищет `Done (` и устанавливает `READY=1` (`.github/workflows/release.yml:153-165`), затем проверяет crash markers (`:168-173`) и crash reports (`:175-183`). После остановки контейнера final verdict проверяет только `READY` и выходит 0 (`:197-207`). Повторного анализа ошибок нет, crash markers проверяются только в stdout, не в `latest.log`.

Файл с одновременно ready и fatal/crash markers будет принят как PASSED. Это подтверждённый дефект логики gate; наличие такого реального crash при этом коммите не утверждается.

Более строгая реализация уже есть в `dev.sh`: после stop/wait собирает свежие markers из обоих логов и новые crash reports, проверяет их до ready (`scripts/dev.sh:810-834`). Рекомендация: вызывать этот общий gate из CI и сохранять logs/test results как artifacts при failure.

### B06 — exit code 2 может превратить failed stage в SKIP

**Приоритет: средний, условный сценарий.** `validate_step` и `validate_step_batch_gradle` интерпретируют любой status 2 как SKIP (`scripts/dev.sh:1151-1154,1160,1194-1197,1203`), включая сырой exit code Docker/Gradle. Для Git-status/no-test wrappers код 2 специально означает отсутствие подходящего этапа; у внешних команд такой контракт не установлен.

Если compile/test/reobf command завершится 2, а ранее созданный universal JAR всё ещё присутствует и проходит check-jar, pipeline может напечатать PASS с SKIP сборочного этапа. Сценарий выведен из control flow, но fake-command end-to-end воспроизведение здесь не запускалось. Рекомендация: skip разрешать только конкретным проверкам предусловий; любой nonzero build exit считать failure.

### B07 — подтверждённый Windows checkout blocker для Linux shell scripts

**Приоритет: высокий для текущего рабочего окружения.** В этом клоне `git config --get core.autocrlf` возвращает `true`; `.gitattributes` отсутствует. Локальная проверка байтов показала:

| Файл | CRLF | Самостоятельные LF |
|---|---:|---:|
| `gradlew` | 172 | 0 |
| `scripts/dev.sh` | 1357 | 0 |
| `scripts/tc6-compat.py` | 1032 | 0 |

На Windows это может быть обычным checkout, но Linux shebang/bash execution через bind mount получает лишние CR. Windows wrapper `gradlew.bat` отсутствует и специально ignored (`.gitignore:46`). Это установленное свойство текущего клона; новый Linux checkout не обязан иметь CRLF.

Рекомендация: отдельный LF checkout для Linux build либо `.gitattributes` для shell scripts и wrapper после согласованного инфраструктурного изменения. Анализ не менял line endings в производственном checkout.

## Тестовая база и реальная сила доказательств

Подсчитаны Java source files и `@Test` annotations, а не результаты тестового запуска и не измерение line/branch coverage.

| Test package | Java files | `@Test` annotations |
|---|---:|---:|
| `thaumcraft.api` | 13 | 33 |
| `thaumcraft.client` | 95 | 149 |
| `thaumcraft.common` | 400 | 955 |
| `thaumcraft.integration` | 3 | 8 |
| `thaumcraft.rendering` | 18 | 52 |
| `thaumcraft.scripts` | 1 | 2 |
| **Итого** | **530** | **1199** |

Основных Java source files — 934. Сопоставление этих количеств не даёт процент покрытого игрового поведения.

Есть заметная полезная база runtime/behavior tests: например `MazeGeneratorInvariantTest:19-64` строит лабиринты на 256 seeds и проверяет связность/feature rooms; `TileResearchTablePersistenceRuntimeTest:35-63` проверяет бонусные аспекты и round trip; packet authority tests используют player/world fixtures. Это существенно сильнее одной компиляции.

Одновременно 387 из 530 файлов содержат markers чтения source/resources (`Files.read`, `readFile`, paths `src/main/...` или `thaumcraft_src`); 403 имени содержат `Static|Guard|Contract|Coverage|Parity`, а 120 — `Runtime|Behavior|Serialization|Invariant`. Это эвристика с пересечениями, не классификация всех тестов. Пример source guard: `TileInfusionMatrixInstabilityStaticGuardTest:17-35` проверяет наличие строк вызовов и условий; такой тест не доказывает выполнение instability flow в игре. `DevScriptSmokeClientJarPreservationStaticGuardTest:18-34` тоже проверяет текст скрипта, а не выполнение rollback.

CI исключает весь `thaumcraft/client/**`: 95 файлов/149 annotations. Среди них есть headless-safe guards, например `GuiArcaneWorkbenchStaticGuardTest:16-38` только читает source text. При этом `thaumcraft/rendering` package исключение не затрагивает. Рекомендуется классифицировать тесты по фактической потребности в GL/display, а не только package name.

В inspected build/workflows нет JaCoCo, branch coverage gate, shellcheck или shell behavior harness. `scripts/tests/test_tc6_compat.py` содержит 13 Python unit tests, но Gradle, CI и `compat-validate` их не вызывают. Они покрывают donor hash/classfile shape, basic instruction scanning, inheritance resolution, static/owner-kind mismatch, ABI gaps и semantic classification; handle static case B01 ими не покрыт.

Основной агент отдельно выполнил все 13 Python tests: PASS после перенаправления `TEMP`/`TMP` в repo `.tmp`. Также donor ABI snapshot check прошёл для 1665 entries, а semantic target snapshot — для 310 symbols (`EXACT=20`, `PROJECTED=209`, `LINK_ONLY=76`, `UNSUPPORTED=5`). Последняя проверка классифицирует сохранённый demand snapshot; она не разрешает addon bytecode против свежесобранного target JAR и не является runtime evidence.

## Compatibility и smoke matrix

Compatibility design аккуратно отделяет binary linkage от semantic support: policy levels `EXACT`, `PROJECTED`, `LINK_ONLY`, `UNSUPPORTED`, first-match classification, failure на unclassified symbol. SHA-256 фиксированы для donor и addon corpus; remapping идёт в ignored staging, original third-party JAR не изменяется. См. `scripts/tc6-compat.py:644-678,681-728,898-929` и `docs/compatibility/tc6-validation.md:6-53`.

Важно, что ABI gap gate принимает reviewed snapshot, а не требует нуля gaps: `command_abi_diff` сравнивает список с файлом и возвращает успех даже при наличии gaps (`scripts/tc6-compat.py:882-895`). Checked-in `tc6-current-gaps.txt` содержит 505 gap records плюс 2 header lines. Это документированный bounded compatibility floor, а не обещание полной TC6 compatibility.

Corpus manifest содержит 11 addons. `compat-release` запускает только 5 server modsets: `enderio fossils magicbees jeid witchery` (`scripts/dev.sh:1269-1273`). Остальные smoke manifests существуют, но автоматическая release workflow этот compatibility gate не вызывает. Публичные CI workflows ограничены JUnit/reobf/main server startup; Python unit tests, full corpus ABI demand checks и addon smoke matrix отсутствуют в их mandatory gates.

`compat-validate` не runnable из одного fresh clone: исходные third-party jars сознательно не committed, требуются local `.smoke` и прогретый Forge cache (`scripts/dev.sh:181-204,370-397`). Некоторые manifests дают только user-provided/issue-era note вместо воспроизводимого download URL. Это явная предпосылка тестирования, а не сам по себе code defect.

Runtime smoke запускает ForgeGradle `runServer`, то есть dev runtime, после чего CI отдельно собирает reobfuscated artifact (`release.yml:138,231-242`). `check-jar` проверяет exact MCP field/method references, но не является запуском конечного JAR в обычном Forge install. Поэтому server startup плюс check-jar полезны, однако не доказывают загрузку artifact в production modpack, addon behavior, visuals или длительный tick/save/load сценарий.

## Дополнительные риски воспроизводимости и поддерживаемости

- `ForgeGradle:2.3-SNAPSHOT`, плавающий Docker tag и неприкреплённые apt packages делают свежий build зависимым от состояния внешних endpoints (`build.gradle:20`, `Dockerfile:9,21-60`). Нет Gradle distribution SHA-256 в wrapper properties. Это reproducibility risk, не доказательство текущей недоступности или подмены.
- Dockerfile download CFR фиксирует release 0.152, но без checksum (`Dockerfile:66-72`). Image source label указывает другой repository (`Dockerfile:12`), что затрудняет provenance.
- `.dockerignore` исключает build/.gradle/.git, но не `.gradle_home`, `.tmp`, `.smoke`, `run`, `thaumcraft_src` и TC6 donor. Dockerfile не делает COPY, поэтому отправка большого dev context не нужна для image и может замедлить локальный build.
- Fixed `thaumcraft-gradle-daemon` container name может переиспользовать mount/image другого checkout: startup проверяет только existence/running, не Root/Image (`scripts/dev.sh:9,50-63`). Параллельные рабочие копии способны выполнять задачи в неправильном workspace.
- `prepare_smoke_modset` удаляет все JAR из `run/mods` перед staging (`scripts/dev.sh:404-417`); isolated-world wrapper и production-JAR backups не имеют EXIT/INT/TERM traps (`:300-319,747-808,856-889`). При interruption восстановление может не выполниться. Для автоматизации нужен отдельный smoke workspace или гарантированный trap rollback.
- `validate --verbose` не всегда покажет обещанный log tail: stage functions меняют `VALIDATE_LAST_LOG` внутри command substitution subshell, а caller остаётся с пустой переменной (`scripts/dev.sh:1139-1143,1163-1168,1182-1186,1205-1208`). Основное сообщение часто содержит путь явно, но automatic tail теряется.
- `AGENTS.md` описывает quiet shortcuts `build`/`test`; dispatcher использует общий passthrough для них (`scripts/dev.sh:1288-1357`). Часть helper functions и `mcp_leak_summary` не используется текущим validate; два classfile parsers для MCP проверок дублируются (`:469-582,598-721`). Это docs/maintenance debt.

## Приоритет дальнейшей работы

1. Восстановить гарантированный Linux/LF/JDK8 build path и согласовать Docker cache paths; затем один чистый `validate` с подтверждёнными actual JUnit results.
2. Исправить B01 и добавить focused Python regression; включить Python tests в обязательный CI.
3. Перевести release smoke на общий строгий verdict, прекратить трактовать external exit 2 как skip, сохранять failure logs.
4. Разделить static client tests и display tests; включить первые в CI, добавить поведенческие проверки critical gameplay вместо новых text-only checks.
5. Явно запускать compatibility gates для изменений публичного API/TC6 shims и поддерживаемую addon matrix перед release.
6. Дополнить dev smoke отдельным production-JAR load test; игровой и visual parity валидировать на реальных сценариях.

## Выполненные проверки

- Read-only `git status --short`, `git rev-parse HEAD`, `git ls-files -s`, `git config --get core.autocrlf`.
- `rg --files` inventory build scripts/resources/tests и `rg -n` searches CI/compat/test guards.
- PowerShell byte checks CRLF/LF и annotation/file counts, чтение всех перечисленных инфраструктурных файлов с номерами строк.
- Python 3.14.4 in-memory `resolve_demands` fixture с assert, подтверждающий B01; `PYTHONDONTWRITEBYTECODE=1`, production sources не менялись.
- Основным агентом: `python -m unittest discover -s scripts/tests -v` с `TEMP`/`TMP` в repo `.tmp` — 13/13 PASS; `tc6-compat.py abi --jar Thaumcraft-1.12.2-6.1.BETA26.jar --check docs/compatibility/abi/tc6-6.1.BETA26-api.txt` — PASS; `tc6-compat.py target --demand docs/compatibility/abi/tc6-addon-demand.txt --policy scripts/tc6-semantic-policy.txt --check docs/compatibility/abi/tc6-target.txt` — PASS.
- Full Gradle build, Java JUnit run, Docker image build и Forge runtime smoke этим агентом **не запускались**. Для documentation-only report runtime smoke не требуется согласно `AGENTS.md:168`.

Создан только этот отчёт. Известные ограничения: отсутствие runtime build evidence в доступной среде, source/annotation metrics без coverage instrumentation, выборочная semantic inspection Java tests, отсутствие полного corpus jars для самостоятельного compatibility smoke.
