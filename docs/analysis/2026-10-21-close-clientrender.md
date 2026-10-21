# Закрытие остатков порта TC4 4.2.3.5 → Minecraft 1.21.1 (Fabric) — клиентские рендереры

Дата: 2026-10-21. Ветка: `port/clientrender`. Рабочая директория: `/home/Acak/tcw/clientrender`
(репозиторий `/home/Acak/thaumcraft-1.21.1` не трогался).

Источники истины (в порядке приоритета):

1. Оракул `/home/Acak/work/thaumcraft-4.2.3.5-oracle` — декомпиляция `Thaumcraft-1.7.10-4.2.3.5.jar`
   (859 классов, имена ваниллы в SRG-форме).
2. OpenTC4 (`/home/Acak/work/opentc4`) — читаемый 1.7.10, использован только для сверки спорных мест.
3. Читаемая ванилла 1.7.10 (`/home/Acak/work/mc1710-src`) — для SRG→MCP раскрытия
   (`python3 -B scripts/porting/resolve_srg.py <srg-имя>`) и для поведения `Tessellator`/`ItemRenderer`.
4. `thaumcraft_src/` и `src/main/java` — 1.12-справочник, **только** как пример.

Объём ревью: **49 единиц** ledger (`docs/porting/scope-ledger.json`) — 40 строк группы
`thaumcraft/client/renderers` с `native_coverage: missing`, плюс `thaumcraft/client/lib/TCFontRenderer.java`,
плюс 8 строк `truetyper/*` и `thaumcraft/truetyper/*`.

---

## Итог

| покрытие (`native_coverage`) | до | после | изменение |
|---|---|---|---|
| `parity_verified` | 556 | 594 | +38 |
| `partial` | 345 | 352 | +7 |
| `missing` | 126 | 81 | **−45** |
| `not_assessed` | 17 | 17 | 0 |

Из 49 единиц моей области **45 закрыты** (вердикт переведён из ложного `missing` в `parity_verified`
или `partial` с доказательствами), **4 остались `missing`** — это честные открытые пробелы
(поза поднятой палочки и начало луча палочки), перечислены ниже.

Соглашение о вердиктах, применённое в этом отчёте (оно же записано в `review.notes` каждой строки):

* `parity_verified` — нативный порт покрывает поведение единицы; для строк, которых нет в
  оригинале (мёртвый код оригинала или 1.12-только помощники), это означает «в оригинале эта
  функция никогда не выполняется, поэтому её отсутствие в порту ничего не меняет»;
* `partial` — покрытие есть, но с зафиксированным расхождением;
* `missing` — поведение в порту отсутствует, это открытый пробел.

Все 49 строк получили `review.owner = port-clientrender`, новые `review.evidence` (с указанием
`файл:строка` оракула и нативного файла) и `review.notes`. Отпечатки `reviewed_sha256` не менялись,
`stale_review_fingerprints = 0`.

---

## Что реализовано в этом заходе

### 1. `ItemBannerRenderer` (оригинал, `source_semantics: different` → `adapted`)

Оригинал `thaumcraft/client/renderers/item/ItemBannerRenderer.java:25-59` — кастомный рендерер
предмета знамени, который рисует `TileBanner` с цветом и аспектом из NBT. В нативе предмета знамени
не было вообще: `models/item/banner.json` ссылался на `thaumcraft:block/wooden_device_base`, то есть
в инвентаре был обычный куб дерева, а цвет и аспект знамени терялись.

Сделано:

* новый `common/src/main/java/thaumcraft/client/renderers/item/ItemBannerRenderer.java`:
  условие оригинала (`ItemBannerRenderer:27-32` — только вариант знамени с цветом **и** аспектом),
  цепочка трансформаций оригинала (`ItemBannerRenderer:43-50`: сдвиг на блок в
  `EQUIPPED`/`EQUIPPED_FIRST_PERSON`, центрирование, поворот на 180° вокруг Z, сдвиг на
  `(-0.5, -1, -0.5)`), отрисовка знамени через общий с оригиналом код;
* `models/item/banner.json` → `minecraft:builtin/entity`, регистрация в
  `fabric/.../ThaumcraftFabricClient.java` (`BuiltinItemRendererRegistry`);
* если у знамени нет ни цвета, ни аспекта — откат на обычное искусство деревянного устройства
  (как в оригинале, где рендерер не забирает такой предмет).

Попутно исправлен нативный `TileBannerRenderer` (`common/.../renderers/tile/TileBannerRenderer.java`),
в котором были отклонения от оригинала:

| что | оригинал | было в порту | стало |
|---|---|---|---|
| высота знамени | `TileBannerRenderer:41` — `y + 1.5` | `y + 1.0` | `y + 1.5` |
| поворот по facing | `:44-48` — `Rz(180)` + `Rz(facing*360/16)` | `Ry(180)` + `Ry(facing*90)` | как в оригинале, `facing` берётся из `EldritchBlockRules.bannerOriginalFacing` (NORTH→0, SOUTH→8, WEST→12, EAST→4 — по `BlockWoodenDevice:266-283` и `EntityCultistPortal:143-157`) |
| цвет ткани | `:55-62` — цвет задаётся **до** вкладок и сбрасывается **после** ткани | красились только вкладки | красятся и вкладки, и ткань |
| палитра красителя | `Utils.colors:80` — своя таблица (белый `0xF0F0F0`, чёрный `0x1E1B1B`) | ванильная таблица 1.21.1 | своя таблица оригинала в `EldritchBlockRules.bannerColour` |
| значок аспекта | `UtilsFX.drawTag:552-560` — квадрат 16×16 в масштабе 0.0375, цвет аспекта, alpha 0.75 | квадрат 1×1 пиксель, белый | квадрат 16×16, цвет аспекта, alpha 0.75 |

### 2. `ItemThaumometerRenderer` (оригинал, `source_semantics: adapted`, `partial`)

Оригинал `thaumcraft/client/renderers/item/ItemThaumometerRenderer.java:72-312` рисует
трехмерный сканер (`textures/models/scanner.obj` + `scanner.png`) и светящийся экран
(`scanscreen.png`). В нативе предмет был плоским спрайтом
(`minecraft:item/generated` + `items/thaumometer`).

Сделано:

* `common/src/main/java/thaumcraft/client/renderers/models/ModelThaumometer.java` — свой загрузчик
  OBJ (в порте нет Forge `AdvancedModelLoader`), читает 24 исходных квада группы `scanner`
  с исходными UV и нормалями;
* `common/src/main/java/thaumcraft/client/renderers/item/ItemThaumometerRenderer.java` — цепочка
  трансформаций оригинала для трёх видов (инвентарь: `Rx(60)`, `Rz(-30)`, `Rz(-248)`, масштаб 0.5;
  в руке: сдвиг `(1.6, 0.3, 2.0)`, `Rx(-90)`, `Rz(-30)`; от первого лица — цепочка
  `ItemThaumometerRenderer:98-135` с удвоенным размером), экран с оригинальной яркостью
  `190 + sin(tick − random(2))*10 + 10`, рука игрока (зеркально, как `RenderPlayer.renderFirstPersonArm:318-324`);
* `models/item/thaumometer.json` → `minecraft:builtin/entity` + регистрация в Fabric.

### 3. `TileLifterRenderer` / `TileLevitatorRenderer` (левитатор)

Оригинал `thaumcraft/client/renderers/block/BlockLifterRenderer.java:66-92` рисует светящиеся
грани ливитатора: верхняя грань цветом `BlockCustomOreItem.colors[4]` (`0xA000A000`) и четыре
боковые цветом `0xDD11FF`, со смещением в 1/100 блока и фиксированной lightmap-координатой 180.
В нативе не было ни BER ливитатора, ни этих граней.

Сделано: `common/src/main/java/thaumcraft/client/renderers/tile/TileLevitatorRenderer.java`
(цвета, смещения, условия «сосед — воздух», lightmap 180) + регистрация в `ClientSetup.java`.

---

## Таблица «единица → вердикт → что сделано»

| единица | вердикт | что сделано |
|---|---|---|
| `thaumcraft/client/renderers/item/ItemBannerRenderer.java` | `parity_verified` | **реализовано**: новый нативный рендерер предмета знамени (цвет, аспект, цепочка оригинала), модель предмета → `builtin/entity` |
| `thaumcraft/client/renderers/item/ItemThaumometerRenderer.java` | `partial` | **реализовано**: 3D-сканер из оригинального OBJ + светящийся экран; открыты две части (см. «Пробелы») |
| `thaumcraft/client/renderers/tile/TileLifterRenderer.java` | `parity_verified` | **реализовано**: `TileLevitatorRenderer` со светящимися гранями оригинала |
| `thaumcraft/client/renderers/tile/TileBannerRenderer.java` | `parity_verified` | **исправлено**: высота 1.5, поворот по `facing*360/16`, палитра `Utils.colors`, цвет ткани, значок аспекта 16×16 |
| `thaumcraft/client/renderers/models/ModelVoidRobe.java` | `parity_verified` | мёртвый код оригинала (0 ссылок); пустая мантия рисуется `gear/ModelRobe` (`ItemVoidRobeArmor:155-158`), он уже есть в порту |
| `thaumcraft/client/renderers/block/BlockTubeRenderer.java` | `parity_verified` | закрыт нативным `TileTubeRenderer` (руки, сердцевина, цвет фильтра) |
| `thaumcraft/client/renderers/block/BlockTaintFibreRenderer.java` | `parity_verified` | закрыт `FabricTaintFibresModel` (0.995/0.98, lightmap 200, детерминированный сдвиг травы) |
| `thaumcraft/client/renderers/tile/TileTubeValveRenderer.java` | `parity_verified` | закрыт `TileTubeRenderer.renderValve` + `extrudedWheel` (глубина 0.1) |
| `thaumcraft/client/renderers/tile/TileTubeOnewayRenderer.java` | `parity_verified` | закрыт `TileTubeRenderer.renderOneWay` (3 стержня, цвет, масштаб 1.1/0.5/1.1) |
| `thaumcraft/client/renderers/tile/TileEssentiaReservoirRenderer.java` | `parity_verified` | нативный BER с исходными числами (3/16..13/16, заполнение units/256, alpha 0.9, lightmap 200) |
| `thaumcraft/client/renderers/tile/TileTubeFilterRenderer.java` | `parity_verified` | закрыт `TileTubeRenderer` (фильтр с цветом аспекта) |
| `thaumcraft/client/renderers/tile/TileTubeRestrictRenderer.java` | `parity_verified` | закрыт `TileTubeRenderer` (ядро restrict) |
| `thaumcraft/client/renderers/tile/TubeConduitRenderHelper.java` | `parity_verified` | закрыт `TileTubeRenderer` + `EssentiaTransfers` |
| `thaumcraft/client/renderers/tile/TileRenderHelper.java` | `parity_verified` | 1.12-хелпер записи граней; нативные рендереры пишут грани в нативные буферы сами |
| `thaumcraft/client/renderers/tile/ExtrudedSpriteRenderHelper.java` | `parity_verified` | 1.12-хелпер; нативная экструзия спрайта — `TileTubeRenderer.extrudedWheel` |
| `thaumcraft/client/renderers/tile/HoleRenderBatchCache.java` | `parity_verified` | 1.12-хелпер батчинга; нативный `TileHoleRenderer` пишет квады сам |
| `thaumcraft/client/renderers/tile/LayeredFieldPlaneHelper.java` | `parity_verified` | 1.12-хелпер слоёв тоннеля; нативный `TileHoleRenderer` |
| `thaumcraft/client/renderers/entity/RenderNoop.java` | `parity_verified` | нет в оригинале 1.7.10; нативный эквивалент есть |
| `thaumcraft/client/renderers/entity/RenderFallbackBiped.java` | `parity_verified` | нет в оригинале 1.7.10; нативный эквивалент есть |
| `thaumcraft/client/renderers/entity/RenderFallbackLiving.java` | `parity_verified` | нет в оригинале 1.7.10; нативный эквивалент есть |
| `thaumcraft/client/renderers/item/ItemChestHungryRenderer.java` | `parity_verified` | нативный builtin-рендерер голодного сундука рисует его же блок-сущность |
| `thaumcraft/client/renderers/item/ItemCrystalRenderer.java` | `parity_verified` | нативные кластеры кристаллов через `TileCrystalRenderer::renderItem` |
| `thaumcraft/client/renderers/item/ItemMetalDeviceRenderer.java` | `parity_verified` | `models/item/metal_device.json` → блочная модель |
| `thaumcraft/client/renderers/item/ItemStoneDeviceRenderer.java` | `parity_verified` | `models/item/stone_device.json` → блочная модель |
| `thaumcraft/client/renderers/item/ItemWoodenDeviceRenderer.java` | `parity_verified` | блочные модели + builtin-рендереры бура |
| `thaumcraft/client/renderers/item/ItemNodeRenderer.java` | `parity_verified` | блочные модели узлов |
| `thaumcraft/client/renderers/item/ItemTableRenderer.java` | `parity_verified` | модели стола, исследовательского стола, стола деконструкции и арканной верстака |
| `thaumcraft/client/renderers/item/ItemTubeRenderer.java` | `parity_verified` | базовая труба — блочная модель, четыре продвинутых — builtin-рендерер |
| `thaumcraft/client/renderers/item/CrystalPerspectiveModel.java` | `parity_verified` | в 1.21.1 нет реестра моделей 1.12; нативная модель предмета + builtin-рендерер |
| `thaumcraft/client/renderers/item/ThaumometerPerspectiveModel.java` | `parity_verified` | то же; нативный 3D-сканер |
| `thaumcraft/client/renderers/item/WoodenDevicePerspectiveModel.java` | `parity_verified` | то же; блочная модель |
| `thaumcraft/client/renderers/item/WandRenderCalibration.java` | `parity_verified` | калибровочной таблицы нет в оригинале; порту она не нужна |
| `thaumcraft/client/renderers/block/BlockEssentiaReservoirRenderer.java` | `partial` | сосуд и содержимое закрыты; в инвентаре оригинал сжимает сосуд до 2/16..14/16, нативная модель — полный куб |
| `thaumcraft/client/renderers/block/BlockTaintRenderer.java` | `partial` | куб и наложение волокон закрыты; оригинал берёт одну из трёх текстур почвы по metadata, натив — один куб |
| `thaumcraft/client/renderers/item/ItemEssentiaReservoirRenderer.java` | `partial` | то же расхождение по усадке в инвентаре |
| `thaumcraft/client/renderers/item/LifterItemColor.java` | `partial` | светящиеся грани ливитатора реализованы; арт предмета ливитатора не тонируется цветами `colors[4]/[5]` |
| `thaumcraft/client/renderers/item/WandPerspectiveModel.java` | `partial` | display-трансформация есть; сама палочка рисуется плоской моделью, а не 3D `ModelWand` — это единица `ItemWandRenderer` чужой полосы |
| `thaumcraft/client/lib/TCFontRenderer.java` | `partial` | исследовательская книга рисует текст ванильным шрифтом 1.21.1; галлактический атлас оригинала не воспроизведён (он и в оригинале не использовался), но начертание глифов между 1.7.10 и 1.21.1 изменилось |
| `truetyper/FontHelper.java` | `parity_verified` | мёртвый код оригинала (0 ссылок) |
| `truetyper/FontLoader.java` | `parity_verified` | оригинал один раз грузит Arial 12 (`RenderEventHandler:139-141`) и **ни разу** не рисует им |
| `truetyper/Formatter.java` | `parity_verified` | используется только мёртвым `FontHelper` |
| `truetyper/TrueTypeFont.java` | `parity_verified` | используется мёртвым хелпером, загрузчиком и неиспользуемым полем `RenderEventHandler` |
| `thaumcraft/truetyper/FontHelper.java` | `parity_verified` | пакета нет в оракуле 1.7.10 (1.12-only легаси) |
| `thaumcraft/truetyper/FontLoader.java` | `parity_verified` | то же |
| `thaumcraft/truetyper/Formatter.java` | `parity_verified` | то же |
| `thaumcraft/truetyper/TrueTypeFont.java` | `parity_verified` | то же |

---

## Открытые пробелы (оставлены `missing`, обоснование)

1. **`thaumcraft/client/renderers/item/WandUsePose.java`, `WandUsePoseSampler.java`, `WandPoseMath.java`** —
   оригинал `ItemWandRenderer:63-93` поднимает палочку, пока игрок использует предмет (фокус),
   с поворотом на `60° * (t/3)` и волной WAVE/CHARGE. В нативе этого нет: палочка рисуется
   нативным `ThaumcraftGearModelsClient.renderWand` без позы использования. Не реализовано в этом
   заходе сознательно: сама отрисовка палочки — единица `ItemWandRenderer` (вне моей полосы,
   владелец `audit-a9-core`), а поза считается в системе координат после `Rx(180)`, который в
   нативе запечён в геометрию `ModelWand`; перенос без совместной правки чужой единицы дал бы
   непроверяемый результат. Требуется совместная правка `ItemWandRenderer`.
2. **`thaumcraft/client/renderers/item/FirstPersonWandTipOrigin.java`** — 1.12-хелпер начала луча
   палочки. В оригинале луч — `thaumcraft/client/fx/beams/FXBeamWand.java`, в нативе луча от палочки
   нет вообще; это полоса клиентских эффектов (`client/fx`), не рендереров.
3. **Подача результата сканирования в первом лице** (оригинал `ItemThaumometerRenderer:155-259`:
   значки аспектов и имя цели) — требует клиентской части исследования; натив держит сканирование
   на сервере, поэтому выводить на экране нечего. Оставлено как `partial` у
   `ItemThaumometerRenderer`.
4. **Два поворота палочки по качанию руки** (`ItemThaumometerRenderer:103-104`) — в 1.21.1 нет
   публичного аналога приватных углов руки 1.7.10.
5. **Тонировка арта предмета ливитатора** цветами `BlockCustomOreItem.colors[4]/[5]` — нативная
   модель предмета ливитатора не тонируется.

---

## Проверка (реальный вывод)

Окружение: `JAVA_HOME=/home/Acak/.jdk21`, `GRADLE_USER_HOME=/home/Acak/gradle-homes/clientrender`.

```
$ ./gradlew --no-daemon --console=plain :common:test :fabric:compileClientJava --rerun-tasks
> Task :common:test
> Task :fabric:compileClientJava
BUILD SUCCESSFUL in 33s
9 actionable tasks: 9 executed
```

Результаты `:common:test` (по `common/build/test-results/test/*.xml`): **1773 теста, 0 failures,
0 errors, 0 skipped** (254 файла отчёта).

Скрипты порта:

```
$ python3 -B scripts/porting/update_migration_inventory.py --write
{"mode": "write", "legacy_units": 934, "partial": 34, "pending": 900, "complete": 0, "active_native_java_files": 1312}
$ python3 -B scripts/porting/apply_clientrender_verdicts.py
{"updated": 49}
$ python3 -B scripts/porting/update_scope_ledger.py --original-root /home/Acak/work/thaumcraft-4.2.3.5-oracle --write
$ python3 -B scripts/porting/update_scope_ledger.py --original-root /home/Acak/work/thaumcraft-4.2.3.5-oracle --check
{"mode": "check", ... "stale_review_fingerprints": 0}
```

### Попытка запуска клиента

В песочнице есть `xvfb-run`/`Xvfb`, клиент запускался трижды
(`xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew :fabric:runClient`, затем с
`LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe` и с `LIBGL_ALWAYS_INDIRECT=1`).
Во всех трёх запусках JVM падает **внутри нативного кода** на этапе создания GL-контекста:

```
# Problematic frame:
# C  [libgallium-26.0.8-1.so+0x17a1e0]  dri2GalliumConfigQueryb+0x10
# The crash happened outside the Java Virtual Machine in native code.
> Task :fabric:runClient FAILED
Execution failed for task ':fabric:runClient'.
> Process 'command '/home/Acak/.jdk21/bin/java'' finished with non-zero exit value 134
```

Падение происходит сразу после `Setting user:` / `Backend library: LWJGL version 3.3.3-snapshot`,
то есть **до** перезагрузки ресурсов и запекания моделей. Значит:

* клиент в этой песочнице запустить нельзя — стек GL/Mesa (`libgallium`) падает на DRI2-запросе;
* сцены не смотрелись, **визуальная приёмка не заявляется**;
* ошибок загрузки моделей в логе нет, но это ничего не доказывает: до запекания моделей дело
  не дошло.

---

## Ограничения этого отчёта

* Приёмка ограничена компиляцией и юнит-тестами; игровые сцены не просматривались (см. выше).
* Правки выполнены только в ветке `port/clientrender` репозитория `/home/Acak/tcw/clientrender`;
  `/home/Acak/thaumcraft-1.21.1` не изменялся.
* Изменённые нативные файлы: `ClientSetup.java`, `ModelBanner.java`, `TileBannerRenderer.java`,
  `EldritchBlockRules.java`, `models/item/banner.json`, `models/item/thaumometer.json`,
  `ThaumcraftFabricClient.java`; новые файлы: `ItemBannerRenderer.java`,
  `ItemThaumometerRenderer.java`, `ModelThaumometer.java`, `TileLevitatorRenderer.java`,
  `scripts/porting/apply_clientrender_verdicts.py`.
