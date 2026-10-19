# API и совместимость дополнений

Дата: 15.10.2026. Исследован commit `4bc68454b1063cdc1255f833dd537e0fd4fc341d`.

## Область и достоверность

Инвентаризированы все 89 файлов API, 40 файлов bundled CodeChicken и 4 файла TrueTyper. Подробно прочитаны центральные `ThaumcraftApi`, `ThaumcraftApiHelper`, `Aspect`, `AspectList`, `CommonInternals`, research category/entry bridge, aura facade и compatibility lifecycle. Это статический разбор контрактов и отдельных алгоритмов, не полная проверка каждой математической функции CodeChicken или всех методов API в игре.

Оригинал TC4 декомпилирован CFR 0.152 из включённого JAR; материалы находятся вне Git-репозитория. Оригинал и donor JAR не изменялись. Оригинальные Java-файлы отсутствуют в `thaumcraft_src`: этот каталог содержит преимущественно ресурсы. Декомпилированные файлы не являются результатом текущего порта и не добавлены в Git.

## Как устроен гибридный API

`thaumcraft.api.*` сохраняет TC4 registry/aspect/research/wand контракты. Дополнения для Minecraft 1.12 часто ожидают TC6 API, поэтому порт добавляет aliases и facade с TC6 именами. При этом canonical gameplay state остаётся TC4. `ThaumcraftSixCompatibility` связывает item/block aliases и отправляет `AspectRegistryEvent` после регистрации core aspect tags, до загрузки исследований (`common/Thaumcraft.java:225–235`, `common/compat/ThaumcraftSixCompatibility.java:22–36`).

`Aspect.FLUX` — alias `TAINT`, а четыре новых TC6 aspect constants отображаются на TC4 (`api/aspects/Aspect.java:51,77–84`). Это намеренная потеря части семантики, не самостоятельная ошибка порта. `CommonInternals.objectTags` тоже не самостоятельное хранилище: `generateUniqueItemstackId` сохраняет последний stack в `ThreadLocal`, а `put` проецирует его в TC4 registry (`api/internal/CommonInternals.java:16–33`). Код, который ожидает обычный полный контракт `ConcurrentHashMap` — `get`, прежнее значение `put`, несколько отложенных идентификаторов — не получает его автоматически. Поддержка такого кода требует отдельного evidence-driven контракта.

`AuraHelper.getVis`, `getFlux`, `addVis`, `addFlux` вызывают явно неподдерживаемые TC6 chunk aura операции; `AuraHandler` выбрасывает `UnsupportedOperationException`. Это задокументировано в `docs/compatibility/tc6-validation.md:23–27` и не следует выдавать за реализованную TC6 механику. Только untyped `drainVis` проецируется на родную TC4 vis network.

Видимая Forge mod version — `6.1.BETA26`, версия сборочного artifact — `1.0.0` (`build.gradle:26–29,38–39`). Версия нужна для ограниченной совместимости, она не доказывает полноту реализации Thaumcraft 6.

Проверены существующие snapshots:

- donor ABI: 1665 записей, snapshot совпал;
- semantic target: 310 symbols — EXACT 20, PROJECTED 209, LINK_ONLY 76, UNSUPPORTED 5;
- Python tooling: 13/13 тестов прошли.

Это проверка donor/snapshots/tooling. Linkage окончательного собранного universal JAR и реальные addon modsets в этой сессии не проверены: JAR не собирался, Docker daemon недоступен.

## Находки

### A01 — кэш поиска рецептов никогда не узнаёт повторный ключ

Приоритет P2: постепенный рост памяти и лишние обходы research registry, унаследовано от оригинала TC4.

`ThaumcraftApi` хранит `HashMap<int[], Object[]> keyCache` (`:56`) и при каждом `getCraftingRecipeKey` создаёт новый `int[]` (`:177–179`). Java-массивы сравниваются по идентичности. Даже одинаковые item id/meta не дают попадание в `containsKey`; `put` при каждом запросе добавляет новый элемент, в том числе для отрицательного результата (`:212`). Существующий ключ не заменяется.

Реальный клиентский вызов: `GuiResearchRecipe.java:1000`. Чем больше повторных обращений, тем больше недостижимых логических дублей хранится в static map. Наличие `keyCache.put` проверяется source guard `ThaumcraftApiCrucibleRecipeKeyStaticGuardTest.java:24–25`; корректность повторного lookup этот тест не проверяет.

Подтверждение на Java 8 через Nashorn/JJS: вставлены 10 000 разных `int[]` с содержимым `[42,0]`, размер `HashMap` равен 10 000; запрос с новым `[42,0]` возвращает `containsKey=false`. Это проверка точной семантики ключа Java, не запуск метода мода в Minecraft. Оригинальный CFR `ThaumcraftApi.java:161–196` содержит тот же алгоритм.

Минимальное направление исправления: value-based private cache key без изменения публичной сигнатуры; явная invalidation при изменении research registry. Сначала отдельный тест повторного положительного и отрицательного lookup. Добавлять player-specific cache нельзя без проверки research gating.

### A02 — complex object tag не применяет новые количества к уже зарегистрированному предмету

Приоритет P2 для API/дополнений; подтверждено чтением реализации, унаследовано от оригинального TC4.

В ветке `exists=true` `registerComplexObjectTag` перебирает новые `aspects`, но вызывает `tmp.merge(tag, tmp.getAmount(tag))` (`ThaumcraftApi.java:294–309`). Параметр новых количеств игнорируется: добавляемый отсутствующий аспект получает 0; имеющийся аспект остаётся на прежнем количестве. Для существующего EARTH=1 и запроса EARTH=4/FIRE=2 результат EARTH=1/FIRE=0 вместо увеличения/добавления. `AspectList.merge` берёт максимум старого и переданного количества (`api/aspects/AspectList.java:171–181`).

Использование не ограничено сторонними модами: `ConfigAspects.java:225–258,421–423` использует этот API. Реальное проявление зависит от того, зарегистрированы ли tags данного предмета до вызова; не утверждается, что все эти stock предметы уже попадают в проблемную ветку.

Оригинальный CFR `ThaumcraftApi.java:259–276` содержит ту же ошибку. Исправление нужно обосновать как original bug и проверить обе ветки. Сравнить фактические tags до/после `ConfigAspects.init`, а не заменять все регистрации без разбора.

### A03 — cullTags не гарантирует уменьшение списка

Приоритет P3, условный некорректный/addon input; основной stock gameplay не объявляется зависающим.

`ThaumcraftApiHelper.cullTags` выполняет цикл, пока size > 6 (`:38`), но начальная нижняя граница равна `32767` (`:40`). Если каждый рассчитанный вес не меньше этой границы, `lowest` остаётся null (`:39,65–68`), `remove(null)` ничего не удаляет (`:70`), цикл не заканчивается. Например, 7 ненулевых стандартных аспектов с количеством 40 000 удовлетворяют этому условию даже для primal weights ×0.9.

Оригинальный CFR `ThaumcraftApiHelper.java:40–80` имеет тот же предел. В обычном `getObjectTags` значения ограничиваются 64 (`common/lib/crafting/ThaumcraftCraftingManager.java:65`), поэтому одна только регистрация больших object tags не доказывает stock-воспроизводимость. Однако direct API call и addon essentia-container bonus tags могут обходить этот нормализованный путь (`:73–86,116`). Требуется маленький тест на гарантированное завершение и выбор реального минимума; запускать бесконечный цикл на сервере для доказательства не нужно.

## Bundled библиотеки

CodeChicken helper использует static render state (`codechicken/lib/render/CCRenderState.java:26–54`) с явным `reset` (`:90–100`). Его классы импортируют клиентские Minecraft/LWJGL API; при просмотренных поисках server/common не обнаружен прямой вызов `CCRenderState`/TrueTyper. Это локальное наблюдение по символам, не доказательство отсутствия всех side-loading ошибок. Матрицы/UV, batching и reset каждого renderer требуют клиентской проверки.

`truetyper` исходники расположены под `thaumcraft/truetyper`, но объявляют пакет `truetyper`, как предусматривает `AGENTS.md`. Не следует массово менять package ради совпадения пути. `FontLoader.createFont` не закрывает resource input stream явно (`FontLoader.java:26–38`), а `TrueTypeFont.getFontImage` не вызывает `dispose` у Graphics2D (`:76–105`); это низкоприоритетные кандидаты для ресурсного review, не подтверждённые игровые crashes или измеренные утечки. GL font atlas lifecycle следует проверять вместе с client resource reload.

## Следующий шаг

1. Исправлять A01/A02 отдельно с поведенческими тестами и original-bug обоснованием.
2. Для TC6 API сохранять четыре уровня поддержки и точные JVM descriptors; не сводить совместимость к успешной компиляции.
3. После появления рабочего Docker запускать `./scripts/dev.sh compat-validate` на конечном universal JAR и нужные modsets.
4. A03 проверять ограниченным unit test; изменения величин default аспектов для обхода цикла не нужны.

Исходники, публичные сигнатуры, конфигурация, пакеты и JAR в рамках анализа не изменялись. Новый commit не создавался; runtime smoke не требуется для этих documentation-only файлов и в этой сессии не выполнялся.
