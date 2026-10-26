# Thaumcraft 4 Reload: первый системный проход полного анализа

Дата: 15 октября 2026, местное время пользователя. Репозиторий: исходный порт Forge 1.12.2 (Andrey Bash и участники).

Ветка: `master`. Зафиксирован commit `4bc68454b1063cdc1255f833dd537e0fd4fc341d`, `chore(opencode): fall back to default subagent models`. Клон содержит полную доступную Git-историю — 1500 commits. Рабочая копия до анализа была чистой.

## Результат и границы

Репозиторий склонирован. Построена полная файловая инвентаризация Java и карта основных подсистем; выполнены параллельные статические обзоры сборки, сети, состояния игрока, магических механик, мира/устройств, клиента/ресурсов и API. Значимые находки перепроверены по текущему коду; для отличий от TC4 использована декомпиляция включённого оригинального JAR.

Это законченный первый проход большого анализа, а не утверждение, что прочитана и доказана корректность каждой строки 934 production Java-файлов. Детальная runtime-проверка, полный запуск Java tests, production-JAR load и визуальная сверка ещё не выполнены. В каждом разделе отделены статические факты, унаследованные ошибки, новые регрессии, условные addon-сценарии и гипотезы, требующие игры.

README оценивает готовность порта примерно в 90%. Здесь этот процент не подтверждается: в репозитории нет единого измеренного покрытия всех игровых контрактов, а source guards, компиляция и server ready marker не равны полной TC4 parity.

Производственные исходники, идентификаторы, API, зависимости и JAR не изменялись. Созданы только документация и инвентаризация. Новый commit не создавался. Закрытая историческая цель в `GOAL.md` не возобновлялась.

## Что находится в репозитории

| Область | Java-файлы | Физические строки | Назначение |
|---|---:|---:|---|
| `thaumcraft.common` | 543 | 89 300 | Lifecycle, registries, crafting, research, networking, blocks/items, entities/AI, tiles, worldgen |
| `thaumcraft.client` | 258 | 42 866 | GUI, rendering, models, particles, shaders, keybinds |
| `thaumcraft.api` | 89 | 5 510 | TC4 public API и ограниченные TC6 compatibility facade |
| `thaumcraft.codechicken` | 40 | 5 353 | Bundled rendering/math helpers |
| `truetyper` | 4 | 547 | Font loading и GL atlas rendering |
| **Основной Java-код** | **934** | **143 576** | Production source tree |
| Java-тесты | 530 | 55 153 | 1199 `@Test` annotations, включая source guards и behavioral fixtures |
| **Всего Java** | **1464** | **198 729** | Строки включают комментарии и пустые строки |

Всего tracked файлов: **4345**. Основных ресурсов: **1676**. Эти количества не являются процентом тестового покрытия. Полный список Java-файлов с package, физическими строками, количеством `@Test` и SHA-256 текущих checkout bytes находится в [инвентаризации](2026-10-15-source-inventory.csv); сводка — [JSON](2026-10-15-inventory-summary.json).

`thaumcraft_src` содержит оригинальные assets/metadata, но не matching Java sources. Для behavioral oracle CFR 0.152 декомпилировал `Thaumcraft-1.7.10-4.2.3.5.jar` в 859 Java-файлов. Декомпиляция сохранена вне репозитория; оригинал и TC6 donor не изменялись.

## Стек и сборка

| Компонент | Текущий контракт |
|---|---|
| Minecraft | 1.12.2 |
| Forge | 14.23.5.2847 |
| Java | source/target 8 |
| Gradle wrapper | 4.10.3 |
| ForgeGradle | 2.3-SNAPSHOT |
| MCP mappings | stable_39 |
| Обязательная зависимость | Baubles, Curse file 2518667 |
| Optional compile API | JEI 4.16.1.1012 и Hwyla |
| Java tests | JUnit 4.12 |
| Artifact version | 1.0.0 |
| Advertised Forge mod version | 6.1.BETA26 |
| Основной результат | `build/libs/Thaumcraft-1.0.0-universal.jar` |

Главные источники: `build.gradle`, `gradle/wrapper/gradle-wrapper.properties`, `Dockerfile`, `scripts/dev.sh`, `.github/workflows/build.yml`, `.github/workflows/release.yml`.

Порт TC4 использует TC6 видимую version и API shims для ограниченного набора дополнений. Это не реализация всех подсистем TC6. Сборка дополнительно патчит `ResearchEntry.class` через ASM, добавляя JVM void setters рядом с TC4 fluent setters. Поэтому ABI надо проверять на окончательном output после Gradle hooks и reobfuscation; одних Java source signatures недостаточно.

Публичные API/dev JAR сохраняют readable names; universal JAR должен пройти reobfuscation и MCP leak check. Startup smoke через `runServer` проверяет dev runtime; отдельная загрузка final production JAR в чистый Forge/modpack остаётся необходимой проверкой релиза.

## Архитектура и основные потоки

| Поток | Точки входа и связи | Главный инвариант |
|---|---|---|
| Загрузка мода | `common/Thaumcraft.preInit/init/postInit`, ConfigBlocks/Items/Entities/Recipes/Aspects/Research, CommonProxy/ClientProxy | Registry names и lifecycle order стабильны; сервер не загружает обязательный клиентский GL-код |
| Клиентское действие | GUI/keybind → serverbound packet → `PacketBase.scheduleServer` → server gameplay → client sync | Изменение состояния подтверждает сервер по sender, инструменту, контейнеру, цели и стоимости |
| Игрок | `PlayerKnowledgeProvider`, `PlayerKnowledgeCapability`, `EventHandlerEntity`, ResearchManager | Canonical capability → save NBT и sync; faithful in-memory copy отличается от persistence filtering |
| Исследования | ConfigResearch, ResearchCategories/ResearchItem, ResearchManager, ScanManager, TileResearchTable | Прогресс, prerequisites, aspects и warp выдаются атомарно, повторный запрос не дублирует награды |
| Крафт | ThaumcraftCraftingManager + API recipes → arcane/crucible/infusion tiles → ItemStack/NBT | Ресурсы списываются согласованно с результатом; output count/NBT сохраняются |
| Магическая энергия | TileNode/TileNodeEnergized + VisNetHandler + wand/focus/device callers | Родная TC4 node/vis механика; TC6 chunk aura не подменяется независимым фиктивным state |
| Транспорт/автоматизация | EssentiaHandler, tubes/jars, mirrors, golems, inventory/fluid helpers | Extraction/placement/spawn учитывают фактический успех; предметы/жидкости не теряются и не создаются повторно |
| Мир | ThaumcraftWorldGenerator, EventHandlerWorld, Outer Lands provider/teleporter/maze, entity AI | Работа с загруженными чанками, bounded tick cost, fresh-port save/load invariants |
| Клиент | ClientProxy/model registries, renderers, ParticleEngine, shaders, JEI | GL/resource lifecycle, bounded queues, корректные paths и отдельные visual evidence |
| Дополнения | ThaumcraftSixCompatibility + aliases/projections + ABI/corpus policy | Package/class/member descriptors и уровень semantic support проверяются отдельно |

В архитектуре есть ясная common/client граница и централизованный scheduling пакетов. Основные риски находятся на стыках: static process-wide caches, преобразование live state через save serializer, ранний доступ к миру до авторизации, inventory/event success и cleanup клиентских ресурсов.

## Приоритетные находки

P1 означает высокий практический приоритет проверки availability; P2 — исправление функционального/инфраструктурного дефекта; P3 — условный addon input, узкая совместимость или косметика. Это приоритеты анализа, не CVSS и не утверждение о воспроизведённом exploit.

| ID в детальном отчёте | Приоритет | Находка | Достоверность и область |
|---|---|---|---|
| NS-01 | P1 проверить | PacketNote делает lookup произвольной tile по dim/coords; table packets также читают clientPos до сравнения с открытым столом | Отсутствие ограничений подтверждено; стоимость chunk loading/DoS требует runtime. PacketNote проблема унаследована |
| NS-02 | P2 | GUI research API в integrated server выбирает server capability и сериализует его с client thread | Неправильная сторона подтверждена; конкретная гонка/exception ещё не воспроизведена |
| NS-03/04 | P2 | Static research cache не сбрасывается между мирами; in-memory NBT copy теряет auto-unlock entries | Control flow/serializer подтверждены; offline/world-switch и addon manifestation требуют fixtures |
| A01 | P2 | `HashMap<int[],…>` кэш рецептов не узнаёт одинаковые новые массивы и растёт на повторных обращениях | Унаследовано. Семантика ключа воспроизведена на Java 8: 10 000 одинаковых логических ключей → size 10 000, cache miss |
| A02 | P2 | registerComplexObjectTag игнорирует новые количества, если tags уже существуют | Унаследовано. Чтение конкретной ветки; влияние stock предметов зависит от registration order |
| WD-01/02 | P2 | Торговая AI отвергает приручённого Pech; trading flag ошибочно сохраняется между сессиями | Новые регрессии против оригинала; реальная navigation/save-reload проверка ещё нужна |
| WD-03 | P2 | Lava ошибочно включена в water-vaporize запрет и не размещается SPA в Nether | Новая регрессия против оригинала; условие и original oracle сопоставлены, игра не запускалась |
| MAG-01 | P2 | Infusion output count >1 режется до одного через обычный inventory setter пьедестала | Новая регрессия, потеря результата addon-рецепта; stock recipes в просмотренном corpus output1 |
| WD-04 | P2 | Возврат `world.spawnEntity` игнорируется, очередь зеркала уменьшается даже при отменённом spawn | Унаследовано. Event-cancellation сценарий требует focused runtime fixture |
| WD-05 | P2 | Голем может собрать разные жидкости за один проход и сложить их amount как один тип | Унаследовано; статическая multi-fluid трасса показывает нарушение сохранения типа, fixture ещё не запускался |
| WD-07 | P2 | WeakHashMap удерживает WorldServer через сильное значение teleporter | Цепочка ссылок подтверждена; фактическое удержание heap при unload/restart не измерено |
| MAG-02/04 | P2 условно | Частичная партия тигля может сохранить уже использованный addon-катализатор; чрезмерная addon research complexity не завершает note generation | Унаследованные ограниченные сценарии; штатные stock recipes/research не объявлены уязвимыми |
| B01 | P2 | ABI resolver ошибочно считает static HANDLE6 instance method | Воспроизведён in-memory fixture на реальном Python resolver; текущий corpus snapshot handle-path не использует |
| B02/03 | P2 | Docker cache path не согласован с user/home; overrides cache используются не всеми stages | Статические конкретные paths подтверждены, build manifestation не проверен |
| B05 | P2 | Release smoke может принять ready marker вместе с crash markers | Final-verdict control flow подтверждён; реальный crash текущего commit не утверждается |
| B07 | Окружение | Этот Windows checkout переводит shell scripts/gradlew в CRLF, gradlew.bat отсутствует | Байты/checkout config подтверждены; Linux/LF checkout решает отдельную платформенную предпосылку |
| Client shader/particles | P2/P3 | Shader cleanup при null player недостижим через tick; pending TC particle queue не ограничена active cap | Статические lifecycle/queue условия подтверждены; retention/menu effects/memory growth не измерены |
| RenderTaintCreeper | P3 | Ссылка на отсутствующую creeper_armor texture | Missing asset path подтверждён; powered rendering branch надо посмотреть в игре |

Дополнительные находки и точные file:line references приведены в разделах, включая Pech AI/persistence, essentia/inventory transfers, malformed clientbound payload и bounded iteration. Они не должны исправляться большой общей правкой: несколько ошибок унаследованы, некоторые требуют addon input, а некоторые являются намеренными compatibility limitations.

Особенно важно: обычный таумометр использует prefix `@`, и сервер принимает только этот prefix. Найденное reward-order отличие в прямом `ScanManager.completeScan(...,"#")` относится к альтернативному compatibility вызову; оно не доказывает неправильные награды штатного таумометра и не доступно через forged `#` scan packet.

Texture collision `Bore.png`/`bore.png` в клоне реальна, но оба Git paths имеют один и тот же blob `0c9d0a462ff01981b421518944562ef9e96f04f3`. Это case portability/packaging debt, не потеря двух разных изображений. Сравнение texture corpus сохранит этот нюанс в клиентском отчёте.

## Доступные проверки: что действительно выполнено

| Проверка | Результат |
|---|---|
| Clone, status, HEAD, history | Успешный полный клон; исходная working tree чистая, HEAD зафиксирован |
| Файловая Java инвентаризация | 1464 файлов, CSV/JSON сформированы |
| CFR original JAR | Exit 0, 859 Java outputs вне репозитория |
| `python -m unittest discover -s scripts/tests -v` | **13/13 PASS** после задания TEMP/TMP в repo `.tmp` |
| Donor ABI `--check` | **PASS, 1665 entries** |
| Semantic target `--check` | **PASS, 310 symbols**, четыре уровня поддержки |
| HANDLE6 fixture | **Дефект воспроизведён:** корректная static method demand получает METHOD_STATIC_MISMATCH |
| Java 8 `int[]` HashMap fixture | **Дефект ключа подтверждён:** 10 000 entries, повторный logical key не найден |
| Client resource checks | JSON/.mcmeta parsing и source resource reference/corpus checks, детали в клиентском отчёте |
| Docker runtime availability | **Недоступен:** Docker client установлен, daemon named pipe отсутствует |
| Git Bash в restricted shell | MSYS startup не разрешён средой; это ограничение запуска, не синтаксическая ошибка dev.sh |
| `compileJava`, Java JUnit suite, `validate`, `build` | **Не запускались:** отсутствует рабочий Docker build runtime; готовность полного JDK8/Forge environment не установлена |
| Dedicated server smoke / final JAR / addons | **Не запускались**, pass не заявляется |
| Manual client visual parity | **Не проверялась** |
| Изменения production code | Нет; финальная сборка для documentation-only diff не требуется по AGENTS.md |

Начальная попытка Python tests дала две ошибки доступа к временному каталогу среды. После TEMP/TMP → repo `.tmp` все 13 прошли; это устранённая предпосылка запуска, не провал тестируемого resolver. Java 8 runtime был подготовлен только для CFR и небольшого JJS fixture; Java target/dependencies проекта не менялись.

Основные воспроизводимые команды из repo root:

```powershell
git status --short
git rev-parse HEAD
git rev-list --count HEAD
$env:TEMP=(Resolve-Path .tmp).Path
$env:TMP=$env:TEMP
& 'C:\Program Files\Python314\python.exe' -m unittest discover -s scripts/tests -v
& 'C:\Program Files\Python314\python.exe' scripts/tc6-compat.py abi --jar Thaumcraft-1.12.2-6.1.BETA26.jar --check docs/compatibility/abi/tc6-6.1.BETA26-api.txt
& 'C:\Program Files\Python314\python.exe' scripts/tc6-compat.py target --demand docs/compatibility/abi/tc6-addon-demand.txt --policy scripts/tc6-semantic-policy.txt --check docs/compatibility/abi/tc6-target.txt
git diff --check
```

`git diff --check` не проверяет untracked report bytes; новые документы дополнительно проверяются на UTF-8, внутренние пути и пробелы отдельно. Python version в сессии — 3.14.4. Это tooling tests, не версия Java/Minecraft.

## Почему большая тестовая база оставляет ошибки

1199 `@Test` — количество annotations. 387 из 530 файлов содержат признаки чтения текста source/resources; это эвристика с пересечениями, не coverage measurement. Source guard полезен для package/descriptor/wiring контрактов, но часто подтверждает наличие даже ошибочного условия, не реальное поведение.

CI исключает весь client package: 95 файлов / 149 annotations, включая некоторые headless-safe source guards. Локальный `dev.sh validate` сам по себе не передаёт `-Pci`, поэтому его заявленный non-GUI режим расходится с фактическим dispatcher. Python resolver tests и compatibility corpus gates не включены в обязательные публичные CI workflows.

Рабочее направление — сохранить полезные guards, но закрывать существенные ошибки точными state-transition тестами: inventory count, canceled spawn, ранний world lookup, client/server capability, world-scoped cache. Server startup не заменяет эти сценарии.

## План следующего углублённого этапа

1. **Воспроизводимая сборка.** Подготовить Linux/LF checkout и рабочий Docker/JDK8 environment; согласовать GRADLE_USER_HOME/cache paths. Выполнить один полный validate, сохранить actual JUnit reports, затем dedicated server ready/crash gate. Не обновлять Minecraft/Forge/Java/Gradle для обхода локальных проблем.
2. **Граница сервера и состояние.** Focused fixtures NS-01/02/03/04: untrusted lookup не вызывается до validation; client GUI не получает server capability; два свежих 1.12 мира не делят cache; in-memory snapshot сохраняет derived research. Проверить реальное chunk behavior и integrated-thread scenario.
3. **Результат операций.** Focused Spa Nether lava, infusion output 4, mirror canceled spawn, Pech session/AI и golem capability transfers. Сравнить с оригиналом, обозначить retained original bug corrections и changes только после конкретного test evidence.
4. **API/CI.** Воспроизводимые A01/A02 и HANDLE6 regression; classification/linkage final universal JAR; поддерживаемые addon modsets. Поведенческий смысл PROJECTED/LINK_ONLY проверять отдельно от resolution.
5. **Клиент.** Manual affected-view matrix: taint creeper layer, shader logout/rejoin, particle bursts, models/UV, infusion/GUI. Измерить queues/resource lifetime прежде, чем утверждать memory leak или visual parity.
6. **Системное подтверждение.** Save/reload и chunk unload/load на fresh-port data, длительная автоматизация/миры/измерения, конечный production JAR. Изменения подтверждать small reversible checkpoints по AGENTS.md.

Эти шаги — список дальнейших проверок, а не автоматически запущенная фоновая задача. Первый проход завершён сохранёнными результатами; полнота runtime audit остаётся явно открытой.

## Подробные разделы

- [Сборка, тесты и CI](2026-10-15-build-tests.md)
- [Сеть и данные игрока](2026-10-15-network-state.md)
- [Исследования, крафт, vis и магические механики](2026-10-15-magic-systems.md)
- [Мир, устройства, entities и transfers](2026-10-15-world-devices.md)
- [Клиент, rendering, ресурсы и интеграции](2026-10-15-client-assets.md)
- [Публичный API и TC6 compatibility](2026-10-15-api-compatibility.md)
- [Java source inventory CSV](2026-10-15-source-inventory.csv)
- [Inventory summary JSON](2026-10-15-inventory-summary.json)

Все разделы относятся к указанному commit. Старые repo audits прочитаны как исторические подсказки; их незакрытые/закрытые выводы не переносились в текущий список без проверки кода.
