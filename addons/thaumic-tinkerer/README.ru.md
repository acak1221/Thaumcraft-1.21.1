# Thaumic Tinkerer для Thaumcraft 4 Reload (Minecraft 1.21.1)

Порт **Thaumic Tinkerer** (1.7.10; авторы Vazkii, pixlepix, nekosune и участники GTNewHorizons) на Minecraft 1.21.1 как аддон к
**Thaumcraft 4 Reload**. Идентификатор мода `thaumictinkerer`; это отдельный jar, зависящий от `thaumcraft`, `architectury` и
(на Fabric) `fabric-api`. С Thaumcraft аддон разговаривает только через публичный `thaumcraft.api`: JUnit-тест
`TinkererApiOnlyTest` роняет сборку, если какой-нибудь исходник импортирует что-то ещё из Thaumcraft.

English version: [README.md](README.md).

> **Лицензия: CC BY-NC-SA 3.0** (см. `LICENSE` и `NOTICE.md`). Некоммерческая, с условием распространения на тех же условиях.
> Остальной репозиторий под MIT; эта папка — нет, кроме `fabric.gradle` и `tools/`.

## Состояние

Сначала Fabric (как и для всего проекта); сборка под NeoForge для аддона **ещё не сделана**.

| Часть | Состояние |
|---|---|
| Восемь групп содержимого (простые блоки, блоки с TileEntity, машины, базовые предметы, фокусы, зачарования, Kami-снаряжение, Kami-мир) | перенесены, проверены ревью, GameTests каждой группы пройдены на тестовом сервере Fabric |
| Слой хуков (`LIVING_DAMAGE`, `BREAK_SPEED`, прыжок, падение, тик использования предмета, дроп, floating-тики, рендереры предметов) | написан для Fabric (Mixin), протестирован; группы переводятся на него |
| Дерево исследований (~90 записей) и рецепты | регистрируются через API Thaumcraft; сверка с исходником 1.7.10 — в `docs/PARITY_REPORT.md` |
| NeoForge | не начат (слой хуков не зависит от загрузчика, см. `docs/HOOKS.md`, раздел 9) |
| Клиент (модели, экраны, частицы, звуки, HUD) | компилируется и проверен тестами ресурсов; **в запущенном клиенте пока не просматривался** |
| Страницы в просмотрщиках рецептов, подсказки Jade, периферия ComputerCraft / OpenComputers, API големов | не перенесены (см. `docs/DEVIATIONS.md`, «Не перенесено») |

Ничего из этой таблицы не заявлено сверх того, что реально запускалось; документ отступлений каждой группы говорит, что
выполнялось, а что нет.

## Структура

```
common/src/main      код и данные (assets/, data/), не зависящие от загрузчика; языковые файлы
common/src/test      JUnit-тесты (без мира)
common/src/gameTest  проверки времени выполнения для GameTests
fabric/src/main      точки входа Fabric, Mixin, реализация рендереров предметов для Fabric
fabric/src/gameTest  обёртки GameTest и тестовый мод Fabric
fabric.gradle        подключает аддон к Gradle-проекту :fabric (наборы исходников, запуски, задачи jar)
tools/compile.sh     быстрая компиляция javac вне Gradle
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

## Что читать дальше

- `docs/PORTING_RULES.md` — правила, которым следует каждый файл (имена, компоненты данных, заголовки лицензий, импорты только из API).
- `docs/DEVIATIONS.md` — все отличия от мода 1.7.10 по группам, со ссылками на документы групп в `docs/deviations/`.
- `docs/API_GAPS.md`, `docs/api-requests/` — что аддону понадобилось от API Thaumcraft и как закрыт каждый запрос.
- `docs/HOOKS.md` — события загрузчика и как они отправляются.
- `docs/PARITY_REPORT.md` — сверка исследований и рецептов с исходником оригинала.
