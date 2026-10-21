# Закрытие остатков порта TC4 4.2.3.5 → Minecraft 1.21.1 (Fabric) — общий слой

Дата: 2026-10-21. Ветка: `port/commonleft`. Рабочая директория: `/home/Acak/tcw/commonleft`
(репозиторий `/home/Acak/thaumcraft-1.21.1` не трогался; `work` не переписывался).

Источники истины (в порядке приоритета):

1. Оракул `/home/Acak/work/thaumcraft-4.2.3.5-oracle` — декомпиляция `Thaumcraft-1.7.10-4.2.3.5.jar`
   (859 классов, имена ваниллы в SRG-форме).
2. OpenTC4 (`/home/Acak/work/opentc4`) — читаемый 1.7.10, использован для сверки спорных мест
   (в частности «кто реально отправляет сетевой пакет»).
3. Читаемая ванилла 1.7.10 (`/home/Acak/work/mc1710-src`) — для SRG→MCP раскрытия
   (`python3 -B scripts/porting/resolve_srg.py <srg-имя>`).
4. `src/main/java` (1.12-справочник) — **только** как пример, спецификация — оракул.
5. Оригинальный jar `Thaumcraft-1.7.10-4.2.3.5.jar` (только чтение) — для сверки `assets/thaumcraft/sounds.json`.

Объём ревью: **18 единиц** `docs/porting/scope-ledger.json` — все 18 стояли с
`review.native_coverage: missing` (у 17 `disposition: exclude`, у `PacketFXBeamPulseGolemBoss` —
`replace`, и именно у него `missing` было ложным).
Клиентские `fx/gui/jei/renderers`, `WorldGenMound`, рецепты/алхимия, сущности, тайлы, големы,
предметы, сеть и 345 partial не входят в область и не менялись.

---

## Итог

| покрытие (`native_coverage`) | до | после | изменение |
|---|---|---|---|
| `parity_verified` | 594 | 606 | +12 |
| `partial` | 356 | 361 | +5 |
| `missing` | 77 | 59 | **−18** |
| `not_assessed` | 17 | 18 | +1 |

Ложного `missing` в моей области не осталось: 12 строк закрыты как `parity_verified`, 5 как `partial`
с зафиксированным расхождением, 1 (`GolemWailaPlugin`) как `not_assessed` — целевой мод не существует
в 1.21.1, покрывать нечего, пробел задокументирован.

Диспозиции не менялись (все 18 строк сохраняют `exclude`, кроме `PacketFXBeamPulseGolemBoss` —
`replace`), поэтому `counts.dispositions` в ledger не поехали; `stale_review_fingerprints = 0`.

**Кода не потребовалось:** каждая из 18 единиц уже имеет нативный аналог в порту; единственная
ошибка была в **вердикте** (ложное «оригинальный луч потерян»), а не в поведении.

---

## Таблица: единица → вердикт → обоснование

| единица (canonical_path) | исходный вердикт | новый вердикт | обоснование (файл:строка) |
|---|---|---|---|
| `thaumcraft/api/BlockCoordinates.java` | exclude / not_applicable / missing | exclude / **adapted** / **partial** | Оракул `api/BlockCoordinates.java:13` — изменяемый `x/y/z`; единственный опубликованный потребитель `api/IArchitect.java:18`, остальные — `client/lib/REHWandHandler.java:79`, `common/items/wands/foci/ItemFocusWarding.java:196`, `common/items/equipment/ItemElementalShovel.java:228`. `readNBT/writeNBT` (`BlockCoordinates.java:77,83`) в дереве 4.2.3.5 **не вызываются нигде** (мёртвый код). Натив: `api/IArchitect.java:7,9` + `common/items/wands/WandArchitect.java:28,63` (Area) — всё переархитектировано на `BlockPos`. Partial: опубликованный тип add-on не переносится (тот же публичный-API регресс, что у строки `thaumcraft/api/IArchitect.java`). |
| `thaumcraft/api/entities/GolemIds.java` | exclude / not_applicable / missing | exclude / not_applicable / **parity_verified** | В оракуле класса нет (это 1.12-хелпер). Оригинальные id: `common/entities/golems/ItemGolemCore.java:45-56` (0..11), `ItemGolemUpgrade.java:46-51` (0..5), `EnumGolemType.java:10-17` (0..7). Натив несёт **те же числа**: `common/items/golems/GolemData.java:40-52`, `GolemUpgrade.java:10-15`, `common/entities/golems/EnumGolemType.java:5-12`. Ни один id не потерян. |
| `thaumcraft/api/entities/IGolemInfo.java` | exclude / not_applicable / missing | exclude / not_applicable / **partial** | В оракуле класса нет; единственный потребитель — 1.12 WAILA-плагин (`legacy .../waila/GolemWailaPlugin.java:23,28`). Данные отдаёт нативный голем: `common/entities/golems/EntityGolemBase.java:281,326,357,373` (`getGolemMaterial`, `getGolemCore`, `isAdvanced`, `getUpgradeAmount`) + `common/items/golems/GolemUpgrade.java:34` (`byId`). Partial: сам интерфейс не опубликован — публикация потребовала бы правки сущности голема (это lane «големы», не мой). |
| `thaumcraft/api/internal/CommonInternals.java` | exclude / not_applicable / missing | exclude / not_applicable / **parity_verified** | В оракуле класса нет (1.12-шим). Три сервиса (`legacy .../CommonInternals.java:14,16,18`): алиас `scanEntities`, карта object-tag, `generateUniqueItemstackId`. Натив: `api/ThaumcraftApi.java:74,75` + `registerObjectTag` (`:367,371,374,378,383,388`), плюс живой plug point `api/internal/IInternalMethodHandler.java:21`. Целочисленный id не нужен: `common/lib/research/ObjectAspectRegistry.java:45,49` держит исходный ключ `(item, metadata)`. |
| `thaumcraft/common/blocks/ItemBlocks/BlockEssentiaReservoirItem.java` | exclude / not_applicable / missing | exclude / not_applicable / **parity_verified** | Класса нет по этому пути: оригинал лежит в `common/blocks/BlockEssentiaReservoirItem.java` (отдельная строка ledger), 1.12 — это перестановка пакетов. 1.12-класс (`legacy .../BlockEssentiaReservoirItem.java:27-36`) ставит facing = **opposite** клика. Натив: `common/config/ThaumcraftTubes.java:70-80` → `common/lib/essentia/ReservoirRules.java:56 placedFacing(clicked) = clicked.getOpposite()`. Флаги damage-подтипа 1.12 в 1.21.1 аналога не имеют и не нужны. |
| `thaumcraft/common/blocks/ItemBlocks/BlockTubeItem.java` | exclude / not_applicable / missing | exclude / not_applicable / **parity_verified** | Оригинал 1.7 — `common/blocks/BlockTubeItem.java` (отдельная строка ledger). 1.12-класс (`legacy .../BlockTubeItem.java:20-35`): труба — clicked face, кристаллизатор — opposite. Натив: труба `common/config/ThaumcraftTubes.java:116-127` (clicked face), кристаллизатор `common/blocks/BlockEssentiaCrystalizer.java:32` (`getClickedFace().getOpposite()`), центрифуга `ThaumcraftTubes.java:81-84` сохраняет исходный facing по умолчанию, как и в оригинале. |
| `thaumcraft/common/compat/ThaumcraftSixCompatibility.java` | exclude / not_applicable / missing | exclude / not_applicable / **parity_verified** | В оракуле класса нет — это TC6-bootstrap 1.12-дерева. Три сервиса (`legacy .../ThaumcraftSixCompatibility.java:22,26,31`): `initBlockAliases`, `initItemAliases`, `postAspectRegistryEvent`. Натив: `api/blocks/BlocksTC.java:29` и `api/items/ItemsTC.java:29` резолвят реестр по требованию (init-прохода не нужно — статических полей нет), `api/aspects/AspectRegistryEvent.java:22,35-42` — листенеры, а `common/Thaumcraft.java:235` поднимает событие после регистрации ядровых аспектов. **Поправка к прежней формулировке**: «поля алиасов не заполняются» было неверно — в нативе полей-алиасов нет вовсе. |
| `thaumcraft/common/config/MaterialAiry.java` | exclude / not_applicable / missing | exclude / **adapted** / **partial** | Оракул `common/config/MaterialAiry.java:13` — 1.7-подкласс `Block.Material`: не solid, не replaceable, не блокирует свет, блокирует движение, `setNoPushMobility` + `setRequiresTool` (`:19,20`); создаётся один раз `common/config/Config.java:916`; потребители — `common/blocks/BlockAiry.java:82`, `common/blocks/BlockEldritchPortal.java:43`. В 1.21.1 класса `Block.Material` нет: флаги стали свойствами блока — натив `common/config/ThaumcraftEldritchBlocks.java:298-303` (`noOcclusion`, `noCollission`), `common/blocks/BlockAiry.java:76-88` (форма по метаданным), `common/blocks/BlockEldritchPortal.java:47-55`. Partial: флаг «нужен правильный инструмент» не воспроизведён — airy-блоки не объявляют `requiresCorrectToolForDrops`. |
| `thaumcraft/common/integration/waila/GolemWailaPlugin.java` | exclude / not_applicable / missing | exclude / not_applicable / **not_assessed** | В оракуле класса нет: WAILA не существовал для 1.7.10. 1.12-плагин (`legacy .../GolemWailaPlugin.java:17,23,28`) пишет в hover материал/ядро/улучшения голема. **Мягкой зависимости в порту нет вообще**: grep `waila|huduma|journeymap` по `common/src`, `fabric/src`, `neoforge/src` — ноль совпадений; нет и ссылок на Forge (`net.minecraftforge`, `cpw.mods`) — мод грузится без этих модов. `not_assessed`: целевого мода в 1.21.1 не существует, нативного аналога нет. **Документированный пробел**: hover-строки не воспроизведены; экран голема (`client/gui/GuiGolem.java:32`) показывает материал и ядро, список установленных улучшений не показан нигде. |
| `thaumcraft/common/lib/CommandThaumcraft.java` | exclude / not_applicable / missing | exclude / not_applicable / **parity_verified** | В оракуле класса нет: оригинальная команда — `common/lib/events/CommandThaumcraft.java` (отдельная строка ledger), 1.12-дубликат (`legacy .../CommandThaumcraft.java:6`) — пустой абстрактный `CommandBase`. Натив: `common/lib/events/ThaumcraftCommands.java:31-35` (алиасы `/thaumcraft`, `/thaum`, `/tc`), `:50-82` (help/research/aspect/warp, permission 2), `common/lib/research/ResearchAdministration.java:13,33,41,48` — рекурсия выдачи по parents/siblings, reset с пере-выдачей auto-unlock, положительный amount аспекта, диапазоны warp. Числа сверены с оригиналом: аспект ≥ 1, warp set ≥ 0, warp add −100..100. |
| `thaumcraft/common/lib/SoundsTC.java` | exclude / not_applicable / missing | exclude / not_applicable / **parity_verified** | В оракуле класса нет; 1.12-алиас `legacy .../SoundsTC.java:10`. В 1.21.1 нет реестра `SoundEvent` — звук это ресурс-локация: натив играет те же id через `SoundEvent.createVariableRangeEvent` (`common/items/wands/foci/ItemFocusPortableHole.java:35`, `common/entities/monster/EntityCultistPortal.java:136`). Полнота набора звуков — в строке `TCSounds`. |
| `thaumcraft/common/lib/TCSounds.java` | exclude / not_applicable / missing | exclude / not_applicable / **parity_verified** | В оракуле класса нет: оригинал везёт звуки в `assets/thaumcraft/sounds.json`. 1.12-класс (`legacy .../TCSounds.java:16-93`) регистрировал 66 событий через Forge. Сверка с оригинальным jar: **66 id в оригинале, 66 id в нативе** (`common/src/main/resources/assets/thaumcraft/sounds.json`), ogg-файлы на месте. Единственное расхождение — два id рунического щита приведены к нижнему регистру (`runicShieldEffect/runicShieldCharge` → `runicshieldeffect/runicshieldcharge`), потому что путь ресурс-локации в 1.21.1 должен быть lowercase; натив играет именно их (`common/lib/runic/RunicShield.java:115,120`). |
| `thaumcraft/common/lib/events/LootHandler.java` | exclude / not_applicable / missing | exclude / not_applicable / **parity_verified** | В оракуле класса нет: оригинал добавляет лут в сундуки через Forge 1.7 `ChestGenHooks` (`common/config/Config.java:574-597`). 1.12-класс (`legacy .../LootHandler.java:26,32`) — та же операция через `LootTableLoadEvent`. Натив: `common/config/ThaumcraftLoot.java:97-133 chestAdditions(category)` — **исходные числа и веса**: common 1..3 @5 (dungeon/jungle/desert pyramid) и @4 (mineshaft, stronghold corridor/crossing/library), uncommon 1..2 @4/@3, rare 1..1 @1, stronghold library knowledge fragment 3..6 @20, village blacksmith thaumium ingot 1..3 @10; `fabric/src/main/java/thaumcraft/fabric/loot/FabricThaumcraftLoot.java:16-26,30-49` — те же 8 категорий на 1.21.1-таблицах, запись в основной взвешенный пул. Предметы сверены с `Config.java:571-597` (loot bag 0/1/2, resource 2 = thaumium, resource 6 = amber, bauble blanks 0/1/2 = mundane amulet/ring/belt, resource 9 = knowledge fragment, vis-amulet, шесть примальных discount ring). NeoForge-модуль пока — стаб, инъекция лута есть только на Fabric. |
| `thaumcraft/common/lib/network/PacketBase.java` | exclude / not_applicable / missing | exclude / not_applicable / **parity_verified** | В оракуле класса нет: оригинал использует simpleimpl-сообщения Forge. 1.12-класс (`legacy .../PacketBase.java:15,34,42`) — пустые `toBytes/fromBytes`, хук `onMessage`, `scheduleServer`. В 1.21.1 нет базового `IMessage`: сериализация уехала в `StreamCodec` каждого payload, обработка — в парный класс `Networking` (например `common/lib/network/VisNetFxNetworking.java:22-40`: `TYPE` + `STREAM_CODEC`, регистрация и `context.queue(...)` на игровом потоке). Разделение обязанностей то же. |
| `thaumcraft/common/lib/network/fx/PacketFXBeamPulseGolemBoss.java` | replace / different / missing | replace / **equivalent** / **parity_verified** | **Главная поправка ревью.** Оракул `common/lib/network/fx/PacketFXBeamPulseGolemBoss.java:36-88` (два id сущностей, два клиентских луча 0.07/0.376/0.325 и 1.0/0.5/0.5, age 20) и `common/lib/network/PacketHandler.java:96` (регистрация на клиент). Grep по всему дереву оракула: сообщение **объявлено и зарегистрировано, но ни один класс его не отправляет**; читаемый 1.7.10 OpenTC4 (`/home/Acak/work/opentc4/.../PacketFXBeamPulseGolemBoss.java:16-57`) — то же самое. Значит в TC4 4.2.3.5 видимый выстрел обезглавленного голема — это снаряд `EntityGolemOrb`, а не луч: оригинал `common/entities/monster/boss/EntityEldritchGolem.java:227-241` (`beamCharge -= 15 + rand.nextInt(5)`, снаряд со скоростью 0.66 и неточностью 5.0, звук `thaumcraft:egattack`). Натив повторяет это дословно: `common/entities/monster/EntityEldritchGolem.java:247-260` и `common/lib/world/EldritchMobRules.java:142-146` (charge 150, drain 15+0..4, 0.66, 5.0). Прежняя запись («видимый луч оригинала потерян») была **фактически неверна**: оригинал его не рисует. Класс `thaumcraft/client/fx/beams/FXBeamGolemBoss.java` закрыт клиентской полосой с той же поправкой — это не моя единица. |
| `thaumcraft/common/lib/utils/ConnectedTextureUtils.java` | exclude / not_applicable / missing | exclude / not_applicable / **parity_verified** | В оракуле класса нет: оригинал держит таблицу в `client/lib/UtilsFX.java:75`, а маску соседей — в `common/blocks/BlockCosmeticOpaque.java:111-160` (connected texturing рутового стекла — **оригинальная** фича TC4). 1.12-хелпер (`legacy .../ConnectedTextureUtils.java:6,21,65`) — рефакторинг той же таблицы. Натив: `client/renderers/block/WardedGlassBakedModel.java:25-41` — таблица из 256 элементов **побайтово та же** (сверено с оракулом и с 1.12), `client/renderers/tile/TileWardedGlassRenderer.java:58-93` — те же восемь флагов соседей на грань. Рунная грань выбирается ровно как в 4.2.3.5. |
| `thaumcraft/common/world/aura/AuraChunk.java` | exclude / not_applicable / missing | exclude / not_applicable / **partial** | В оракуле класса нет: в TC4 нет почанковой ауры, аура — это аура-нода (`common/tiles/TileNodeEnergized.java:27,165`). 1.12-состояние чанка (`legacy .../AuraChunk.java:6`: base/vis/flux) — TC6-концепция, и сам 1.12-обработчик отказывает: `legacy .../AuraHandler.java:15,19,23` бросает `UnsupportedOperationException`. Натив проецирует поверхность на ноду: `api/aura/AuraHelper.java:20,26,48`. Partial: per-chunk base и flux аналога в TC4 не имеют. |
| `thaumcraft/common/world/aura/AuraHandler.java` | exclude / not_applicable / missing | exclude / not_applicable / **partial** | В оракуле класса нет. 1.12 `drainVis` (`legacy .../AuraHandler.java:27-33`) округляет amount вниз и уходит в VisNet. Натив: `api/aura/AuraHelper.java:48-60 drainVis(level, pos, amount, simulate)` по эссенции ноды + `api/visnet/VisNetHandler.java:516,517` (дрен по аспекту). Partial — по той же причине, что и `AuraChunk`. |

---

## Необязательные интеграции: WAILA и Thaumcraft 6

Задача волны: «привести необязательную интеграцию к безопасному виду, чтобы мод грузился без этих модов».
Фактическое состояние порта:

* **WAILA** — в порту нет ни одной ссылки: `grep -rni "waila|huduma|journeymap"` по `common/src`,
  `fabric/src`, `neoforge/src` — ноль совпадений; `mcp.mobius.waila` не импортируется. Мод грузится
  без WAILA «из коробки», мягкой зависимости нет — устранять нечего.
* **Thaumcraft 6** — TC6-шимпов в нативе нет как классов: `net.minecraftforge` и `cpw.mods`
  встречаются только в тексте комментариев (`api/aspects/AspectRegistryEvent.java:10`,
  `common/config/ThaumcraftVillages.java:39`, `common/blocks/FluxFiniteBlock.java:35`).
  TC6-форму публичного API, которая **есть**, перенесена и работает без Forge:
  `api/blocks/BlocksTC`, `api/items/ItemsTC`, `api/aspects/AspectEventProxy`,
  `api/aspects/AspectRegistryEvent` (событие поднимается в `common/Thaumcraft.java:235`).
* Итог: обе интеграции безопасны по построению — «мягкая зависимость отсутствует, а код, который
  требовал бы внешнего мода, не импортируется».

---

## Побочный ремонт метаданных (не единицы волки)

Слияние клиентской полосы (`693819a Merge branch 'port/clientfx' into work`) оставило в
`docs/porting/class-inventory.json` **дубликаты**: 8 повторов строк `legacy_units` и 11 повторов
`active_native_sources` (у повторов разные sha256 — файлы менялись после первого снимка).
Из-за этого `scripts/porting/update_scope_ledger.py --check` падал с
`Duplicate legacy inventory path: .../FXArc.java` и `Duplicate active native inventory path: .../ClientSetup.java`,
то есть валидатор ledger был сломан ещё до моего захода.

Сделано:

* `legacy_units`: оставлена одна строка на путь (пересмотренная, со `status: partial` и `native_paths`),
  удалены 8 устаревших «pending»-заглушек → снова 934 строки, счётчики `partial: 42 / pending: 892`
  совпадают с файлом;
* `active_native_sources`: одна строка на путь, sha256 пересчитаны по текущему содержимому файлов →
  1316 строк, набор путей совпадает с фактическими файлами (проверено), счётчик
  `active_native_java_files` обновлён с 1312 на 1316;
* `docs/porting/scope-ledger.json` пересобран `update_scope_ledger.py --write` (авторские ревью
  сохраняются), после чего `--check` проходит.

Побочные эффекты в ledger: `native_linked_rows` 34 → 37 и `unique_native_counterparts` 53 → 57 —
это авторские связи клиентской полосы, которые стали видны после дедупликации.

---

## Что осталось открытым (честные пробелы)

1. `thaumcraft/common/integration/waila/GolemWailaPlugin.java` — hover-информация WAILA-плагина
   (материал/ядро/улучшения голема) в порту не воспроизведена: целевого мода для 1.21.1 нет.
   Экран голема показывает материал и ядро (`client/gui/GuiGolem.java:32`), список улучшений —
   нигде. Правка потребовала бы клиентского hover-tooltip (lane клиентских GUI), поэтому не
   выполнялась здесь; вердикт — `not_assessed` с объяснением.
2. `thaumcraft/common/config/MaterialAiry.java` — флаг «нужен правильный инструмент» 1.7-материала
   не имеет нативного эквивалента (airy-блоки не объявляют `requiresCorrectToolForDrops`); вердикт `partial`.
3. `thaumcraft/api/BlockCoordinates.java`, `thaumcraft/api/entities/IGolemInfo.java` — опубликованные
   типы для add-on не перенесены (натив использует `BlockPos` и методы голема); вердикт `partial`.
   Публикация `IGolemInfo` потребовала бы правки сущности голема — это lane «големы».
4. `thaumcraft/common/world/aura/AuraChunk.java`, `thaumcraft/common/world/aura/AuraHandler.java` —
   per-chunk base и flux ауры TC6 не имеют аналога в TC4 (и сам 1.12-обработчик их отказывал);
   вердикт `partial`.
5. NeoForge-модуль остаётся стабом: инъекция лута в сундуки (`ThaumcraftLoot.chestAdditions`)
   сейчас существует только на Fabric. Это не новый пробел волки, а состояние модуля.

Наблюдение для чужой полосы (сущности, не мой unit): оригинал перед выстрелом обезглавленного голема
выставляет `getLookHelper().setLookPosition(target.x, target.y + height/2, target.z, 30, 30)`
(OpenTC4 `EntityEldritchGolem.java:198`), а натив читает текущий `getLookAngle()`
(`common/entities/monster/EntityEldritchGolem.java:252`). Расхождение ограничено начальной точкой
снаряда (сама цель и числа совпадают).

---

## Проверка

Команда (по заданию волны):

```
export JAVA_HOME=/home/Acak/.jdk21
export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=/home/Acak/gradle-homes/commonleft
./gradlew --no-daemon --console=plain :common:test
```

Результат — в разделе «Тесты» ниже (реальный вывод, без пересказа).

Проверки метаданных:

```
python3 scripts/porting/audit_ledger.py --merge docs/porting/audit-reviews-commonleft.json
# {"applied": 18, "unknown": [], "unknown_count": 0}

python3 scripts/porting/update_scope_ledger.py --check --original-root /home/Acak/work/thaumcraft-4.2.3.5-oracle
# mode=check ... stale_review_fingerprints: 0
```

---

## Тесты

Команда и реальный вывод (сокращён по длине, начало и конец):

```
$ export JAVA_HOME=/home/Acak/.jdk21
$ export PATH=$JAVA_HOME/bin:$PATH
$ export GRADLE_USER_HOME=/home/Acak/gradle-homes/commonleft
$ ./gradlew --no-daemon --console=plain :common:test

To honour the JVM settings for this build a single-use Daemon process will be forked. ...

> Configure project :
Architect Plugin: 3.4.162
Architectury Loom: 1.11.458

> Task :common:compileJava
/home/Acak/tcw/commonleft/common/src/main/java/thaumcraft/common/mixin/ResearchNoteStackCodecMixin.java:13: warning: Unable to locate method mapping for @At(INVOKE.<target>) 'Lcom/mojang/serialization/Codec;lazyInitialized(Ljava/util/function/Supplier;)Lcom/mojang/serialization/Codec;'
1 warning

> Task :common:processResources
> Task :common:classes

> Task :common:compileTestJava
Note: /home/Acak/tcw/commonleft/common/src/test/java/thaumcraft/essentia/BoreInventoryPlanTest.java uses or overrides a deprecated API.

> Task :common:processTestResources
> Task :common:testClasses
> Task :common:test

[Incubating] Problems report is available at: file:///home/Acak/tcw/commonleft/build/reports/problems/problems-report.html

BUILD SUCCESSFUL in 47s
5 actionable tasks: 5 executed
EXIT=0
```

Сводка по отчётам JUnit (`common/build/test-results/test/*.xml`), без домысливания:

```
test classes: 256
tests: 1781 failures: 0 errors: 0 skipped: 0
```

Набор перезапущен на закоммиченном состоянии ветки (`--rerun-tasks`, все 5 задач выполнены заново) —
результат тот же: `BUILD SUCCESSFUL`, `EXIT=0`, падений нет.

Падений нет. Это серверный/юнит-набор модуля `:common`; клиентскую, мультиплеерную и survival-приёмку
здесь не заявляю.

---

## Файлы коммита

* `docs/porting/audit-reviews-commonleft.json` — авторские ревью 18 единиц (формат
  `scripts/porting/audit_ledger.py --merge`).
* `docs/porting/scope-ledger.json` — вердикты 18 единиц + обновлённые счётчики и хэш инвентаря.
* `docs/porting/class-inventory.json` — ремонт дублей после слияния клиентской полосы.
* `docs/analysis/2026-10-21-close-commonleft.md` — этот отчёт.
