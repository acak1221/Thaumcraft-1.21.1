# Excavation: Silk Touch и Dowsing — исходный контракт, 20.10.2026

Спецификация: оригинал TC4 4.2.3.5 `ItemFocusExcavation:72,94–112,126,195–199,225–258,302–319`,
`EventHandlerWorld.harvestEvent:171–195`, `Utils.findSpecialMiningResult:174–182`, `Config:821–823`.
Forge1.7.10 `Block.java.patch` подтверждает исходную проверку `canSilkHarvest`: по умолчанию
`renderAsNormalBlock && !hasTileEntity(metadata)`, с отдельными переопределениями стекла/льда/листвы.
Legacy1.12 не является спецификацией.

## Исходное поведение

- Dowsing — id20, значок `dowsing`, стоимость улучшения —1 Mining (`Aspect.MINE`, исходный tag `perfodio`).
  Dowsing доступен только на ранге3, Silk Touch id8 только на ранге5. Оба совместимы на одном фокусе;
  исходные Frugal/Potency/Treasure/Enlarge остаются на своих рангах.
- Базовая стоимость —15terra центивис за завершённую попытку, а не за каждый held tick.
  Исходные continuous-use проверки вызывают `getVisCost` на **жезле**, поэтому настоящая попытка остаётся
  15terra и с Silk/Dowsing. Отказ защиты происходит после оплаты завершённой основной попытки.
- Tooltip использует один общий static `cost2`: первый запрос Silk задаёт16terra+1 остальных пяти primals;
  первый запрос Dowsing задаёт15terra+2ignis+2ordo. Последующие запросы любого из двух улучшений возвращают
  уже созданную стоимость; при наличии обоих до первого запроса ветка Silk имеет приоритет.
  Обычный фокус/жезл всегда возвращает базовую стоимость.
- При разрешённом Silk harvest выпадает настоящий исходный block item/metadata и нет mining XP;
  иначе выполняются обычные Fortune-дропы и XP. Поэтому Spawner с TileEntity не становится silk-добываемым
  и сохраняет свои обычные XP даже при Silk-фокусе.
- Dowsing срабатывает в исходном HarvestDropsEvent и для обычной, и для Silk-ветки.
  Для каждого элемента списка, даже отсутствующего в таблице, расходуется один world `nextFloat()`.
  Вероятность — `0.2 + focusTreasure*0.075`, граница включительна. Нативное Fortune-зачарование на самом жезле
  не заменяет уровень Treasure фокуса. Железо имеет множитель1, золото/киноварь0,9; количество входного стака
  умножает исходный выход, исходно равный одному cluster. Каждая замена играет orb sound0,2 с исходным RNG pitch.

## Нативная реализация

- Закрытый `FocusUpgradeType` содержит20 и по-прежнему отклоняет неизвестные id, включая21.
  Rank checks распространяются на применение, напрямую присоединённые компоненты и nested focus codecs.
  Значки/переводы используются из исходных уже перенесённых ресурсов.
- `ExcavationVisCosts.Tooltip` сохраняет исходный порядок инициализации `cost2`, возвращая immutable значения;
  `ItemFocusExcavation` создаёт защитную копию AspectList. Существующий `attemptCost` и порядок оплаты не меняются.
- У уже существующей безопасной границы `ExcavationHarvest` приватная копия loot-tool получает Silk1 из
  настоящего фокуса. Инвентарь/зачарования исходного жезла не изменяются. Block entities исключены из Silk,
  как в исходной стандартной Forge-проверке; существующий отказ занятых ульев/гнёзд сохраняется.
  Остаётся принятая современная политика native1.21 loot tables, raw ores, captured BE/container data и XP,
  а не повторный вызов `playerDestroy`/износ инструмента/статистика/выпуск silverfish.
- `SpecialMining` принимает Dowsing-жезл и читает именно Treasure фокуса. Оно работает над настоящим списком
  `Block.getDrops` внутри существующего harvest пути; helper преобразования не публикует ItemEntity.
  Железная/золотая руда и её deepslate-варианты дают соответствующие cluster как при raw-ore дропе,
  так и при Silk block-item дропе. Deepslate/raw ore — явное отображение современного мира1.21.
- Ранее принятая Silk-exclusion Pickaxe of the Core сохранена отдельно: она не распространяется на Dowsing.
  Primal Crusher и допуск этого кирочного поведения в Arcane Bore остаются отдельными ещё не завершёнными группами.

## Evidence и оставшиеся проверки

**Implemented:** полный набор Excavation-рангов, исходный shared tooltip/wrong-stack casting quirk,
Silk native loot с сохранением Spawner XP и Dowsing/Treasure special mining, включая совместный Enlarge.

**Подготовлено, ещё не запускалось:** `ExcavationVisCostsTest` проверяет оба порядка инициализации и приоритет;
codec/catalog tests включают20 и отклоняют21. `ExcavationSpecialRuntimeChecks` использует отдельного подключённого
игрока, native RMB и настоящие player ticks: stone/glass/ice/coal ore, Spawner XP, raw/deepslate Silk Dowsing,
точную цену15terra, native protection refusal, совместный Enlarge2 и отсутствие повторного выхода.
Для вероятностных ветвей явно выбирается seed по настоящему `Block.getDrops` и восстанавливается непосредственно
перед завершённой попыткой; прохождение не выдаётся за естественную частоту или длительный survival playthrough.

**Built / game-tested / seen:** новая группа пока не собиралась и не запускалась; клиентский tooltip/иконки,
live multiplayer, survival progression и NeoForge не проверены. Существующие basic/Enlarge-проходы относятся
только к своим прежним входам и не доказывают работу новых специализаций.
