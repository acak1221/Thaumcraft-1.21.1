# Нечёткое сравнение предметов голема (Entropy) — контракт оригинала, 19.10.2026

Источник: оригинал TC4 4.2.3.5 (локальная декомпиляция CFR вне репозитория). `E:` — `EntityGolemBase`, `GH:` — `GolemHelper`,
`IU:` — `InventoryUtils`, `IM:` — `InventoryMob`, `GUI:` — `GuiGolem`.

## Три переключателя

- Хранятся в `toggles` сущности (E:785-805): индекс 5 — `checkOreDict()`, 6 — `ignoreDamage()`, 7 — `ignoreNBT()`; в отличие от
  индексов 1–4 (Guard) они **положительные** (бит установлен = включено). Кнопки меню `55 + n` (`ContainerGolem:86`,
  `GUI:376-389`), подписи «Use Ore dictionary» / «Ignore item damage» / «Ignore NBT values».
- Показываются при `Entropy ≥ 1` у ядер `ItemGolemCore.canSort`: **Fill 0, Empty 1, Gather 2, Use 8, Sorting 10**
  (`GUI:214-262`). Позиция: Sorting `(66, 12 + 24 + 10n)`, остальные `(180, 24 + 10n)`.
- Слот цвета Order и переключатели не связаны; оба читают одно и то же сравнение (`getColorsMatching`, E:517-535).

## Сравнение `IU.areItemStacksEqual(a, b, ore, damage, nbt)` (IU:295-323)

1. `null`/`null` → равны; один `null` → нет.
2. `ore`: если у `a` есть **первое** ore-имя и `b` входит в его список — равны (несимметрично; иначе падение на обычное сравнение).
3. Иначе: предмет один и тот же; `nbt` выключен → теги равны (`areItemStackTagsEqual`); повреждение: разные `damage` не равны,
   **кроме** `ignoreDamage` при двух ломающихся стеках или при wildcard-метаданных.

## Где оно применяется

| Ядро | Место | Эффект |
|---|---|---|
| Fill 0 | `GH.getContainersWithGoods` (:133-152), `AIFillTake:48-50` (`extractStack`), `GH.getMissingItems` (:186-196) | поиск источника, **забирается предмет слота** (не образец), подсчёт имеющегося в домашнем хранилище |
| Empty 1, Use 8 | `GH.getItemsNeeded` (:432-448) → `IM.getItemsNeeded(Entropy>0)` (:236-277), `AIHomeTake:53`, `AIHomeReplace:56` | при Entropy образцы **расширяются всеми предметами первого ore-имени** образца — независимо от переключателей; затем `extractStack` с флагами |
| Sorting 10 | `GH.findSomethingSortCore` (:543), `AISortingGoto:60`, `AISortingPlace:49,58`, `AIHomeTakeSorting:51,106` | «в получателе уже лежит такой предмет» (затравка) и забор из дома |
| Fill/Empty/Use с Order | `getColorsMatching` (E:517-535) | цвета слотов, чьи образцы «равны» предмету |
| Gather 2 | — | флаги **не читаются** (`AIItemPickup` их не использует), хотя переключатели показаны |

## Очередь Liquid (`AILiquidGather:168-183, 255-300`)

С Entropy у ядра Liquid строится очередь соединённых жидких блоков (флуд по 26 соседям от размеченного блока, радиус `range²`,
только блоки той же жидкости, годные к откачке), **отсортированных от самых далёких** (`Collections.reverseOrder()`), и голем
забирает по одному блоку из очереди вместо только размеченного; очередь пересчитывается, когда пуста.

## Решения порта (до реализации)

- Метаданные 1.7 в 1.21 сплющены в отдельные предметы: «игнорировать повреждение» — игнорировать компонент `DAMAGE`,
  «игнорировать NBT» — игнорировать все компоненты (сравнивать только предмет); без повреждений/компонентов у обоих — как есть.
- «Ore dictionary» — теги: материальный тег `c:` (`c:ingots/iron`, `c:ores/…`, `c:dusts/…`, `c:gems/…`, `c:nuggets/…`, `c:storage_blocks/…`,
  `c:raw_materials/…`, `c:rods/…`, `c:plates/…`, `c:gears/…`, `c:dyes/…`, `c:crops/…`, `c:seeds/…`) или один из `minecraft:logs`,
  `minecraft:planks`, `minecraft:saplings`, `minecraft:leaves`, `minecraft:wooden_slabs`, `minecraft:wooden_stairs`. Отклонение:
  симметрично, без «первого ore-имени» (порядок регистрации в 1.21 не определён); широкие корневые теги `c:ingots` и нематериальные
  `c:tools/pickaxe`, `c:armors/…`, `c:foods/…` не считаются (первый прогон GameTest показал, что `c:tools/pickaxe` содержит все кирки и
  уравнял бы железную с алмазной — в оригинале у инструментов ore-имён нет).
- Переключатели — свой ключ данных (как маска Guard), оригинальный `toggles` не переиспользуется.
