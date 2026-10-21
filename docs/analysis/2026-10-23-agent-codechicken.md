# Домен `embedded_support_and_dependencies`: вендорённая библиотека CodeChicken (TC4 4.2.3.5 → 1.21.1)

**Дата:** 23.10.2026 · **Ветка:** `agent/codechicken` · **Домен:** `embedded_support_and_dependencies`
**Оракул:** `Thaumcraft-1.7.10-4.2.3.5.jar` (в репозитории) + читаемый 1.7.10-справочник
`/home/cyberfox-kali/OpenTC4` · **Нативный порт:** `common/src`, `fabric/src`, `neoforge/src`

Область: 34 единицы ledger `thaumcraft/codechicken/**` (`missing`/`exclude` после прошлой волны).

---

## 1. Итог

| Показатель | Значение |
|---|---|
| Единиц домена | 34 |
| Закрыто (`missing` → `parity_verified`, `disposition: exclude` с доказательствами) | **34** |
| Перенесено в порт новых классов CodeChicken | **0** (ни один класс не нужен) |
| Осталось открытых | **0** |
| Найдено и исправлено дефектов ревью | **1** (вердикт по `DepLoader` был фактически неверен) |
| Изменено файлов | 3 (`scope-ledger.json`, `THIRD_PARTY_NOTICES.md`, новый JUnit-тест) + этот отчёт |
| Авторских тестов | 1 класс, 11 методов (**авторские, не выполнены сьютой репозитория**) |

Покрытие ledger по всему файлу: `missing` 58 → **24**, `parity_verified` 653 → **687**,
`partial` 315, `not_assessed` 18. Блок `counts` не тронут.

Соседние 7 единиц того же дерева (`MathHelper`, `RayTracer`, `IndexedCuboid6`, `ExtendedMOP`,
`Vector3`, `Cuboid6`, `BlockCoord`) были закрыты прошлой волной как `replace`/`parity_verified`
с нативной заменой `TubeGeometry`; в этом отчёте они перепроверены (§4) и оставлены как есть.

---

## 2. Метод аудита (как считалось доказательство)

Прошлая волна пометила все 34 единицы как «мёртвый код оригинала, ноль ссылок» **без**
проверки байткода. В этом отчёте ссылка считается по байткоду оригинального jar:

1. Разобран constant pool всех **942** `.class` файлов `Thaumcraft-1.7.10-4.2.3.5.jar`
   (из них 73 — дерево `thaumcraft/codechicken/**`, 869 — остальные классы TC4).
2. Для каждого метода каждого класса разобран `Code`-атрибут и выписаны все
   `invoke{virtual,special,static,interface}` с точными `owner#name(descriptor)`.
   Разбор 942/942 классов завершился без пропусков и без ошибок разбора опкодов.
3. Построен обратный граф вызовов «вызываемый метод → вызывающие классы».

**Результат: во всём моде ровно восемь вызовов в код CodeChicken, и все восемь — из шести
классов TC4:**

| Вызываемый метод CodeChicken | Вызывающий класс TC4 |
|---|---|
| `MathHelper.floor_double(D)I` | `EntityShockOrb` |
| `IndexedCuboid6.<init>(Object, Cuboid6)` | `TileTube`, `TileTubeBuffer` |
| `RayTracer.<init>()` | `BlockTube` |
| `RayTracer.rayTraceCuboids(Vector3, Vector3, List, BlockCoord, Block)` | `BlockTube` |
| `RayTracer.retraceBlock(World, EntityPlayer, int, int, int)` | `BlockTube`, `TileTube`, `TileTubeBuffer`, `TileTubeValve`, `ItemResonator` |
| `BlockCoord.<init>(III)` | `BlockTube` |
| `Cuboid6.<init>(DDDDDD)` | `TileTube`, `TileTubeBuffer` |
| `Vector3.<init>(Vec3)` | `BlockTube` |

**Ни один из 34 классов домена не имеет ни одного вызова.** Все 34 единицы закрыты как
`exclude` с этим доказательством в `review.evidence`.

Дополнительно проверена цепочка загрузки классов: `Vector3`/`Cuboid6` ссылаются на
`Transformation`, `Quat`, `Translation`, `ITransformation`, а те — на `CCRenderState`,
`CCRenderPipeline`, `LightMatrix`, `ColourRGBA`, `Colour`, `CCModel`, `Vertex5`, `UV*`.
То есть 33 из 34 «достижимы» на уровне константного пула, но этот путь открывается только
через `Vector3.apply/rotate/translation` и `Cuboid6.apply`, а TC4 4.2.3.5 вызывает оттуда
**только конструкторы** `Vector3` и `Cuboid6`. Ни один метод 34 классов в 4.2.3.5 не
выполняется.

---

## 3. Решение по каждой единице

Колонка «используется портом» — ответ на вопрос «нужен ли этот класс нативному порту 1.21.1».
Для всех 34 ответ «нет»: порт не содержит пакета `thaumcraft/codechicken` вообще.

| Единица (canonical_path) | Используется портом? | Доказательство | Действие |
|---|---|---|---|
| `thaumcraft/codechicken/core/launch/DepLoader.java` | нет | manifest оригинала: `FMLCorePlugin: thaumcraft.codechicken.core.launch.DepLoader`; класс реализует `IFMLLoadingPlugin`/`IFMLCallHook`; 0 Java-вызовов; в 1.21.1 нет FML-coremod | `exclude`, `parity_verified` (см. §5) |
| `lib/colour/Colour.java` | нет | 0 invoke-сайтов; достижим только через `Vector3.apply`→`Transformation`→`CCRenderState`→`LightMatrix`→`ColourRGBA`→`Colour` | `exclude`, `parity_verified` |
| `lib/colour/ColourARGB.java` | нет | 0 invoke-сайтов; даже внутри библиотеки на него никто не ссылается | `exclude`, `parity_verified` |
| `lib/colour/ColourRGBA.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/lighting/LC.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/lighting/LightMatrix.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/lighting/LightModel.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/lighting/PlanarLightModel.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/render/CCModel.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/render/CCModelLibrary.java` | нет | 0 invoke-сайтов; внутри библиотеки тоже 0 ссылок | `exclude`, `parity_verified` |
| `lib/render/CCRenderPipeline.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/render/CCRenderState.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/render/Vertex5.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/render/uv/UV.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/render/uv/UVRotation.java` | нет | 0 invoke-сайтов; внутри библиотеки 0 ссылок | `exclude`, `parity_verified` |
| `lib/render/uv/UVScale.java` | нет | 0 invoke-сайтов; внутри библиотеки 0 ссылок | `exclude`, `parity_verified` |
| `lib/render/uv/UVTransformation.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/render/uv/UVTransformationList.java` | нет | 0 invoke-айтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/render/uv/UVTranslation.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/util/Copyable.java` | нет | 0 invoke-сайтов; реализуется только самими классами CodeChicken (`Colour`, `LC`, `CCModel`, `Vertex5`, `UV`, `BlockCoord`, `Cuboid6`, `CuboidCoord`, `Matrix4`, `Quat`, `Vector3`) | `exclude`, `parity_verified` |
| `lib/vec/AxisCycle.java` | нет | 0 invoke-сайтов; внутри библиотеки 0 ссылок | `exclude`, `parity_verified` |
| `lib/vec/CuboidCoord.java` | нет | 0 invoke-сайтов; внутри библиотеки 0 ссылок | `exclude`, `parity_verified` |
| `lib/vec/ITransformation.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply`/`Cuboid6.apply` | `exclude`, `parity_verified` |
| `lib/vec/IrreversibleTransformationException.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/vec/Matrix4.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/vec/Quat.java` | нет | 0 invoke-сайтов; `Vector3.rotate` его вызывает, но TC4 `rotate` не вызывает | `exclude`, `parity_verified` |
| `lib/vec/RedundantTransformation.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/vec/Rotation.java` | нет | 0 invoke-сайтов; `Vector3.rotate` его вызывает, но TC4 `rotate` не вызывает | `exclude`, `parity_verified` |
| `lib/vec/Scale.java` | нет | 0 invoke-сайтов; внутри библиотеки 0 ссылок | `exclude`, `parity_verified` |
| `lib/vec/SwapYZ.java` | нет | 0 invoke-сайтов; внутри библиотеки 0 ссылок | `exclude`, `parity_verified` |
| `lib/vec/Transformation.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply`/`Cuboid6.apply` | `exclude`, `parity_verified` |
| `lib/vec/TransformationList.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |
| `lib/vec/Translation.java` | нет | 0 invoke-сайтов; `Vector3.translation` его вызывает, но TC4 не вызывает | `exclude`, `parity_verified` |
| `lib/vec/VariableTransformation.java` | нет | 0 invoke-сайтов; путь загрузки только через `Vector3.apply` | `exclude`, `parity_verified` |

Общее для всех 34 строк доказательство, добавленное в `review.evidence`:

* аудит байткода 942 классов оригинала: **0 invoke-сайтов** у каждого класса домена;
* перечисление всех восьми вызовов в мод и шести вызывающих классов;
* `grep -rni codechicken common/src fabric/src neoforge/src` → **1 совпадение**, это комментарий
  `common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:21`; нативных классов
  CodeChicken в порте нет;
* нативная замена по группам: ванильный рендер-пайплайн 1.21.1 (`PoseStack`/`VertexConsumer`,
  например `common/src/main/java/thaumcraft/client/renderers/tile/TileTubeRenderer.java:62-79`)
  вместо библиотечного пайплайна CodeChickenLib; `Vec3`/`AABB`/`BlockPos` + JOML
  (`Matrix4f`/`Quaternionf`) вместо алгебры трансформаций; `ItemColors`/ARGB-int вместо
  `Colour*`; нативный `AABB`/`Vec3`/`BlockPos`/`Mth` вместо `Cuboid6`/`Vector3`/`BlockCoord`/
  `MathHelper`.

Замечание к формулировке покрытия: `native_coverage: parity_verified` означает «наблюдаемое
поведение оригинала воспроизведено», а не «класс перенесён». Класс не переносится: у 34
единиц в оригинале **нет** наблюдаемого эффекта (0 вызовов), поэтому падение поведения равно
нулю, и порт ведёт себя как 4.2.3.5. Для `DepLoader` единственное исключение — coremod-связка
(см. §5); её эффект был только в provisioning'е сторонней библиотеки, которой в порте нет.

---

## 4. Перепроверка семи «живых» соседей (уже закрытых)

Прошлой волне они помечены `replace`/`parity_verified`; в этом домене они не числятся, но их
заявление проверено, потому что именно на них держится поведение труб:

* `RayTracer.rayTraceCuboids` / `retraceBlock` → нативный
  `common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:38-95`;
* `Cuboid6`/`IndexedCuboid6` → `TubeGeometry.java:23-32`: шесть рукавов
  `0.42..0.58` и ядро `0.34375..0.65625` — совпадают с `TileTube.addTraceableCuboids`
  1.7.10 до последнего бита (в оригинале `0.42F`/`0.58F`/`0.34375F` — float, в порте те же
  float-литералы);
* буфер: `TileTubeBuffer` 1.7.10 даёт ядро `0.25..0.75`, порт — `BUFFER_CORE` с теми же
  числами;
* `MathHelper.floor_double` → `Mth.floor` в
  `common/src/main/java/thaumcraft/common/entities/projectile/EntityShockOrb.java:95`.

Отдельная находка (не дефект): класс `TileTubeValve` оригинала в порте слит с `TileTube` как
вид `BlockTube.Kind.VALVE` (`common/src/main/java/thaumcraft/common/tiles/TileTube.java:61`),
поэтому отсутствие отдельного файла — сознательное решение, а не пробел.

---

## 5. Дефект ревью: `DepLoader` не был «мёртвым кодом»

**Было (прошлая волна, `docs/analysis/2026-10-21-audit-a1-client.md:173`):**
> «Dead code in the original: the bundled CodeChicken class is never referenced by any TC4
> class, so it had no effect in 4.2.3.5.»

**Что на самом деле:**

* `META-INF/MANIFEST.MF` оригинального jar:
  `FMLCorePlugin: thaumcraft.codechicken.core.launch.DepLoader`,
  `FMLCorePluginContainsFMLMod: true` — FML 1.6 грузит класс **рефлективно по имени** из
  манифеста, поэтому в байткоде TC4 на него и нет ни одной ссылки;
* иерархия 1.7.10: `DepLoader implements cpw.mods.fml.relauncher.IFMLLoadingPlugin,
  cpw.mods.fml.relauncher.IFMLCallHook`;
* тело `injectData(Map)` в оригинальном jar **пустое** (метод в 1 байт), `call()` вызывает
  только `load()` → `DepLoadInst.load()` — загрузчик зависимостей CodeChicken;
* в 1.21.1 механизма FML-coremod нет: `fabric/src/main/resources/fabric.mod.json` объявляет
  только точки входа `main`/`client`, `neoforge/src/main/resources/META-INF/neoforge.mods.toml`
  использует `modLoader="javafml"`; ключа `FMLCorePlugin` нет ни в одном загрузочном файле;
* в сохранённом 1.12-справочнике этого класса нет вообще (`legacy = null` в ledger), и в
  `src/main/resources` есть только `mcmod.info` — без `META-INF/MANIFEST.MF`: базовая линия
  1.12 coremod уже выбросила.

**Вывод:** `exclude` остаётся правильным вердиктом (переносить нечего и некуда), но обоснование
было неверным. Класс был **живым** в 4.2.3.5 — он provisioning'ил сторонний runtime
CodeChicken, который в 1.21.1 отсутствует. Ревью переписано, `notes` фиксируют исправление
ошибки.

Удалять было нечего: класса нет ни в порте (`common/src`, `fabric/src`, `neoforge/src`), ни в
сохранённом 1.12-справочнике, ни в метаданных загрузчиков. Ничего не выбрасывалось из
`src/main/java/thaumcraft/codechicken/**`: это сохранённый, **не компилируемый** 1.12-справочник
(`settings.gradle` включает только `common`, `fabric`, `neoforge`), и его файлы зафиксированы
sha256 в `docs/porting/class-inventory.json` — удаление сломало бы отпечатки ledger.

---

## 6. Атрибуция (`THIRD_PARTY_NOTICES.md`)

Ревью: **уведомление было неполным** — в нём не было ни слова о вендорённом CodeChicken,
хотя в репозитории остаётся `src/main/java/thaumcraft/codechicken/lib/**` (40 файлов).

Факты, добавленные в `THIRD_PARTY_NOTICES.md` (без изменения каких-либо утверждений о
владении):

* дерево — вендорённая копия классов CodeChickenLib от ChickenBones, переупакованная в
  пространство имён `thaumcraft.codechicken` базовой линией Forge 1.12.2, которая уже
  перечислена в «Pinned sources»; upstream CodeChickenLib — LGPL (в репозитории upstream
  заявлена LGPL-2.1), CodeChickenCore — MIT;
* `core/launch/DepLoader.java` (CodeChickenCore) в репозитории **не** хранится: его нет ни в
  1.12-справочнике, ни в порте;
* дерево не компилируется и не попадает ни в один артефакт: `settings.gradle` включает только
  `common`, `fabric`, `neoforge`;
* в порте кода CodeChicken нет: `grep -rni codechicken common/src fabric/src neoforge/src` →
  одно совпадение, комментарий в `TubeGeometry.java:21`.

Вывод: уведомление теперь корректно и полно для того, что реально остаётся в репозитории и
в артефактах; права на распространение CodeChicken это не создаёт и не меняет.

---

## 7. Тесты (авторские, **не выполнены** сьютой репозитория)

`common/src/test/java/thaumcraft/essentia/CodeChickenGeometryParityTest.java` — 11 методов.

Тест переносит в тестовый код оригинальные 1.7.10-исходники (`TileTube.addTraceableCuboids`,
`TileTubeBuffer.addTraceableCuboids`, `RayTracer.traceSide/rayTraceCuboid/rayTraceCuboids`,
`Vector3`-перехваты плоскостей, `MathHelper.between`) и проверяет **оригинальные** числа:

* рукавы `0.42..0.58` (6 штук, порядок 0..5 = DOWN, UP, NORTH, SOUTH, WEST, EAST), ядро трубы
  `0.34375..0.65625`, ядро буфера `0.25..0.75` — совпадают с нативными `TubeGeometry.ARMS`,
  `CORE`, `BUFFER_CORE` (через reflection по тем же именам полей);
* ближняя грань рукава и ядра, промах мимо сечения рукава, ближайшая индексированная часть
  (индекс 5, точка `(1, 0.5, 0.5)`), вырожденный старт на грани (оригинал возвращает стартовую
  точку);
* паритет: для 12 лучей нативный `AABB.clip` выбирает **ту же** индексированную часть и даёт ту
  же точку (с допуском 1e-6 — у ванильного clip своя эпсилон-смещение 1e-7 на плоскости грани);
* часть за пределами луча отбрасывается и оригиналом, и нативным кодом одинаково.

**Статус выполнения.** Сьют репозитория **не запускался**: Gradle в этой волне запрещён
(общий кэш Loom). Что реально сделано:

* файл проверен `javac` (без Gradle) против настоящих классов 1.21.1 из кэша Loom и
  `junit-jupiter-api:5.11.4` — компилируется без ошибок;
* все 11 методов **выполнены** в отдельной сборочной проверке вне репозитория
  (`/tmp`, реальные `AABB`/`Vec3` 1.21.1 + заглушка `TubeGeometry` с теми же значениями
  коробок) — 11/11 PASS;
* прогон с настоящим `TubeGeometry` невозможен без Gradle: класс тянет граф классов порта.

Ограничение проверки: тест читает приватные статические поля `ARMS`, `CORE`, `BUFFER_CORE`
класса `TubeGeometry`; при переименовании этих полей тест нужно поправить.

---

## 8. Что осталось за рамками

* **Запуск Gradle/сьюта** — вне возможностей волны; тест authored, но не выполнен сьютом.
* **Клиентский визуальный осмотр** — не проводился: в песочнице не создаётся GLX-контекст.
  На мои правки это не влияет (правки только в ledger, уведомлении и тесте), но «видеоподтверждена»
  ни одна из 34 единиц не может быть: речь о мёртвом в оригинале коде.
* **`update_scope_ledger.py --check`** в этой среде выполнить нельзя: приватный оракул
  (`<repo>/../work/thaumcraft-4.2.3.5-oracle`) в песочнице отсутствует, скрипт падает с
  «Missing source root». Вместо этого локально выполнены те же проверки, что делает скрипт:
  `validate_review()` для всех 1044 строк — **0 ошибок**; пересчёт `reviewed_sha256` против
  recorded `sha256_lf` — **`stale_review_fingerprints: 0`**; JSON-форматирование побайтно
  совпадает с `json.dumps(..., ensure_ascii=False, indent=2) + "\n"`; изменены ровно 34 строки
  домена, блок `counts` не тронут.
* **Правки вне домена не вносились**: `common/src/main/java/thaumcraft/{api,client,common}/**`
  не тронуты (запрещено волной). `TubeGeometry` и классы труб не редактировались; расхождение
  на ~1e-7 между арифметикой оригинала и эпсилон-смещением ванильного `AABB.clip` зафиксировано
  тестом как допуск, а не как дефект.
