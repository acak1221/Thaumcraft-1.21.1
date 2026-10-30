# Thaumic Tinkerer для Thaumcraft 4 Reload (Minecraft 1.21.1)

Порт **Thaumic Tinkerer** (1.7.10; авторы Vazkii, pixlepix, nekosune и участники GTNewHorizons) на Minecraft 1.21.1 как аддон к
**Thaumcraft 4 Reload**. Идентификатор мода `thaumictinkerer`; это отдельный jar, зависящий от `thaumcraft`, `architectury` и
(на Fabric) `fabric-api`. С Thaumcraft аддон разговаривает только через публичный `thaumcraft.api`: JUnit-тест
`TinkererApiOnlyTest` роняет сборку, если какой-нибудь исходник импортирует что-то ещё из Thaumcraft.

English version: [README.md](README.md).

> **Лицензия: CC BY-NC-SA 3.0** (см. `LICENSE` и `NOTICE.md`). Некоммерческая, с условием распространения на тех же условиях.
> Остальной репозиторий под MIT; эта папка — нет, кроме `fabric.gradle`, `tools/` и файлов сборки в `standalone/` (их перечисляет `NOTICE.md`).

Аддон собирается и из собственного репозитория, <https://github.com/acak1221/Thaumic-Tinkerer-1.21.1> (только Fabric); его дерево
записывает из этой папки `tools/export-standalone.sh`, и пока тот репозиторий не стал основным, разработка идёт здесь. См. `docs/EXPORT.md`.

## Состояние

Сначала Fabric (как и для всего проекта); сборка под NeoForge для аддона **ещё не сделана**.

| Часть | Состояние |
|---|---|
| Восемь групп содержимого (простые блоки, блоки с TileEntity, машины, базовые предметы, фокусы, зачарования, Kami-снаряжение, Kami-мир) | перенесены, проверены ревью; GameTests есть у каждой группы. То, что **записано как запущенное**, неравномерно: документы Kami-мира, фокусов и (только первый раунд) Kami-снаряжения фиксируют запуски с числами; документы машин и базовых предметов не фиксируют результата запуска; документ зачарований говорит, что его тесты запускались, но без результата. Единственная запись, охватывающая все восемь групп, — файл результатов GameTest `fabric/build/tinkerer-gametest-results.xml`. В файле от 2026-10-30 10:29 было 298 случаев и ни одного элемента failure, error или skipped, и он не был связан с консольным логом. Более поздние запуски дерева (2026-10-30, логи Gradle со строкой «All 299 required tests passed»; логи не закоммичены) записали 299 случаев, снова без failure, error и skipped (блоки с TileEntity: тогда 34, теперь 35) (`docs/HOOKS.md`, раздел 12, «What ran») |
| Слой хуков (`LIVING_DAMAGE`, `LIVING_ARMOR_ABSORB`, `BREAK_SPEED`, прыжок, падение, тик использования предмета, дроп, floating-тики, рендереры предметов) | написан для Fabric (Mixin); проверки существуют (JUnit и GameTests, `docs/HOOKS.md`, раздел 12). Файл результатов выше перечисляет 14 GameTests хуков, среди них два для `LIVING_ARMOR_ABSORB`, без сбоев; ни один документ не фиксирует запуск JUnit-тестов хуков; **отправка событий для NeoForge не написана** |
| Группы, которым нужны хуки (зачарования, Kami-мир, Kami-снаряжение, машины, фокусы) | **переведены** на хуки на Fabric, обходные пути удалены (`docs/HOOKS.md`, раздел 10). Запуски, записанные в документах групп: Kami-мир — 20 GameTests (вместе с GameTests хуков его раунда ревью, 33 из 33) и 38 JUnit-проверок; фокусы — 36 GameTests; Kami-снаряжение — прохождение зафиксировано только для первого раунда (30 JUnit-тестов группы плюс `TinkererApiOnlyTest` и 30 GameTests; сейчас у группы 34 JUnit-теста и 36 GameTests); машины — без результата; зачарования — «запускались» без результата. Общих итогов здесь не складываем |
| Дерево исследований и рецепты | регистрируются через API Thaumcraft; сверены с исходником 1.7.10 скриптом и GameTest (следующая строка) |
| Паритет с исходником 1.7.10 | `docs/PARITY_REPORT.md` (прогоны 2026-10-30): сверены по полям 74 записи исследований и 127 рецептов с оригиналом, а также 17 тегов объектов; **11 задокументированных отличий, 0 незадокументированных**; только Fabric (GameTest на запущенном сервере: в записанном там прогоне пройдены все 7 обязательных тестов; NeoForge не запускался) |
| NeoForge | не начат: нет ни модуля, ни файла сборки аддона, и ни одна из реализаций отправки событий не написана, поэтому Soul Hearts, дроп осколков, эффекты зачарований и прочее поведение на событиях там не работали бы. Это следует из чтения кода: аддон под NeoForge не собирался и не запускался (`docs/HOOKS.md`, раздел 9, `docs/DEVIATIONS.md`, «Не перенесено») |
| Клиент (модели, экраны, HUD) | **просмотрен в запущенном dev-клиенте Fabric** под Xvfb с программным рендерингом (Mesa llvmpipe), `docs/CLIENT_VERIFICATION.md`: последний прогон стенда записал 726 пройденных проверок, 0 провалов, 1 не запущенную (подсказка режима инструмента требует нажатия клавиши), 80 снимков, английский и русский; дефекты, найденные первым прогоном, исправлены, прогон повторён. **Не просмотрено:** видеокарта, звук, ввод (клики, клавиши, перетаскивание), мультиплеер, другие моды (EMI, JEI, Jade), NeoForge, оригинальный клиент 1.7.10 для сравнения (`docs/CLIENT_VERIFICATION.md`, «What it cannot show») |
| Страницы в просмотрщиках рецептов, подсказки Jade, периферия ComputerCraft / OpenComputers, API големов | не перенесены (см. `docs/DEVIATIONS.md`, «Не перенесено») |

Ничего из этой таблицы не заявлено сверх того, что документ или файл результатов фиксирует как запущенное. Документы групп
записывают запуски в разном объёме (первые строки); где документ не фиксирует запуска, таблица так и говорит.

## Структура

```
common/src/main      код и данные (assets/, data/), не зависящие от загрузчика; языковые файлы
common/src/test      JUnit-тесты (без мира)
common/src/gameTest  проверки времени выполнения для GameTests
fabric/src/main      точки входа Fabric, Mixin, реализация рендереров предметов для Fabric
fabric/src/gameTest  обёртки GameTest и тестовый мод Fabric
fabric.gradle        подключает аддон к Gradle-проекту :fabric (наборы исходников, запуски, задачи jar); отдельный репозиторий подключает тот же файл
tools/compile.sh     быстрая компиляция javac вне Gradle
tools/client-smoke.sh   клиентский осмотр под Xvfb (docs/CLIENT_VERIFICATION.md)
tools/export-standalone.sh   записывает отдельный репозиторий из этого дерева (docs/EXPORT.md)
standalone/          файлы, которые есть только в отдельном репозитории (settings.gradle, build.gradle, workflow CI, README и др.)
docs/                правила переноса, отступления, запросы к API и пробелы, хуки, отчёт о паритете
lang-extra/          фрагменты локализации, сливаемые в языковые файлы
```

Слой реестров — `thaumic.tinkerer.common.registry` (`TTRegistry`, `TTRecipes`), содержимое —
`thaumic.tinkerer.common.content` (по регистратору на группу, список в `ContentGroups`).

## Сборка и тесты

Из корня репозитория (Java 21, Gradle wrapper):

```bash
./gradlew :fabric:tinkererTest                 # JUnit-тесты аддона
./gradlew :fabric:runTinkererGameTest          # все GameTests на тестовом сервере Fabric
./gradlew :fabric:runTinkererGameTest -PtinkererGameTestFilter='simpleblocksgametests.*'   # группа или тест (список через запятую, * в конце допустима)
./gradlew :fabric:remapThaumicTinkererJar      # релизный jar: fabric/build/libs/ThaumicTinkerer-fabric-<версия>.jar
./gradlew :fabric:runTinkererClient            # клиент для разработки с Thaumcraft и аддоном
```

`runTinkererGameTest` пишет `fabric/build/tinkerer-gametest-results.xml`. Созданный мир лежит в
`fabric/run-tinkerer-gametest/` и игнорируется git.

Хороший запуск (0.1.0-alpha.7, 2026-10-30; числа меняются, когда добавляются тесты, важно отсутствие сбоев): `:fabric:tinkererTest` заканчивается `BUILD SUCCESSFUL`,
364 теста, 0 сбоев, 0 ошибок, 0 пропущенных (`fabric/build/test-results/tinkererTest/`); `:fabric:runTinkererGameTest` пишет `All 299 required tests passed :)`
(299 элементов `testcase` в файле результатов, без `failure` и `error`); `tools/client-smoke.sh` (нужны `xvfb-run`, программный рендеринг Mesa и `python3`)
заканчивается `harness COMPLETED`, FAIL 0, HARNESS_ERROR 0, NOT_RUN 1 и PASS от 725 до 727. Сервер GameTest и клиент запускают каждый JVM с `-Xmx3G` рядом с демоном
Gradle на 3 ГБ: рассчитывайте примерно на 6 ГБ свободной памяти. В отдельном репозитории те же команды без `:fabric:` (`docs/EXPORT.md`).

## Что читать дальше

- `docs/PORTING_RULES.md` — правила, которым следует каждый файл (имена, компоненты данных, заголовки лицензий, импорты только из API).
- `docs/DEVIATIONS.md` — все отличия от мода 1.7.10 по группам, со ссылками на документы групп в `docs/deviations/`.
- `docs/API_GAPS.md`, `docs/api-requests/` — что аддону понадобилось от API Thaumcraft и как закрыт каждый запрос.
- `docs/HOOKS.md` — события загрузчика и как они отправляются.
- `docs/PARITY_REPORT.md` — сверка исследований и рецептов с исходником оригинала.
