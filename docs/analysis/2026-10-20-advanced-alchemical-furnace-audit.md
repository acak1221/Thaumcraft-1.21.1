# Advanced Alchemical Furnace — аудит оригинала, 20.10.2026

Это **контракт следующей единицы**, без реализации или заявления о проверке.
Источник — TC4 4.2.3.5, CFR вне репозитория: `WandManager:744–807`,
`TileAlchemyFurnaceAdvanced`, `TileAlchemyFurnaceAdvancedNozzle`, `BlockAlchemyFurnace`,
`TileAlchemyFurnaceAdvancedRenderer`, `ConfigRecipes:100,291`, `ConfigResearch:403`,
`EventHandlerWorld.onCrafting:163–167`, `ItemEldritchObject`, оригинальный `AspectList`.


**Исправление первоначальной ошибки аудита:** нижний center — НЕ crucible.
Original `WandManager:752` проверяет `ConfigBlocks.blockStoneDevice`, metadata0;
`BlockStoneDevice.createTileEntity:257–259` явно создаёт для metadata0
`new TileAlchemyFurnace()`. `ConfigRecipes:100` использует тот же stoneDevice0
в схеме, а normal furnace recipe `ConfigRecipes:280` превращает metalDevice0
(crucible) в stoneDevice0. Поэтому нормальная печь служит основанием и является
результатом обратного превращения formed metadata0 (`BlockAlchemyFurnace:159–177`).
Приватный oracle — декомпиляция исходного TC4 bytecode, не предположение по имени.

## Зависимости и сборка

Нативно уже есть обычные alchemical furnace, alembic, alchemical construct, void ingot, VisNet,
ObjectAspectResolver с bonus tags. **Ещё нет:** primordial pearl с полным применением
к node, его настоящая survival выдача из bosses/rare loot, advanced construct,
собственно formed furnace и nozzle. Pearl не следует подменять декоративным предметом
или объявлять survival доступность через выдачу в тесте.

Исследование `ADVALCHEMYFURNACE`, ELDRITCH, secondary, родители
`PRIMPEARL`, `DISTILESSENTIA`, `VISPOWER`. Страницы: текст1, `AdvAlchemyConstruct`,
текст2, схема `AdvAlchemyFurnace`.

`AdvAlchemyConstruct`: arcane recipe даёт **четыре** advanced constructs
(metalDevice metadata3), `VAV/APA/VAV`, V — void ingot, A — ordinary alchemical
construct metadata9, P — primordial pearl (eldritchObject metadata3).
Оплата `aqua10,ordo30,terra10` до cap/gear скидок.

Pearl не расходуется. Это реализовано **не** контейнерным предметом: исходный
`EventHandlerWorld.onCrafting` для результата metalDevice3 увеличивает count
центрального ingredient перед последующим уменьшением crafting slot. Современный
порт должен возвращать ровно исходную pearl с компонентами через реальный оплаченный
craft, без произвольного общего исключения для всех редких предметов.

Blueprint3×2×3, вид сверху, C — normal alchemical furnace, A — advanced construct, N — normal
construct, L — alembic, `?` — **непроверяемая клетка**:

```
верх: L N L       низ: A A A
      N ? N            A C A
      L N L            A A A
```

Необходимы8 advanced constructs,4 normal constructs,4 alembics и1 normal alchemical furnace.
Верхний центр не проверяется и не заменяется — существующий блок/BE там должен
сохраниться. В исходном коде поиск первого normal alchemical furnace идёт a,b,c=−1..1 вокруг клика;
после первого подходящего центра неверный blueprint сразу возвращает false,
а не продолжает искать другую normal alchemical furnace. Успех оплачивает `ignis50,aqua50,ordo50`
через `consumeAllVisCrafting`, преобразует17 физических частей и включает original
wand sound/sparkles. Сохранять оригинальную семантику содержимого обязательно: center NORMAL furnace возвращает
свои два физических inventory stacks через `InventoryUtils.dropItems`
(`BlockStoneDevice:312–318`), накопленная slurry/энергия теряются; alembic возвращает
label (`BlockMetalDevice:534–535`), его stock теряется. Не вводить отказ наполненным
исходным устройствам: нативные removal hooks уже владеют возвратом физических предметов.

## Работа

У core нет GUI, обычного inventory или fuel slot. Вход — настоящий dropped
ItemEntity, столкнувшийся с core. При успешном `process` удаляется **один** предмет
из stack, играет bubble volume0,2/pitch1..1,4. Entity не становится всем batch.

Mixed essentia buffer maxVis500. Теги — original getObjectTags+getBonusTags,
полностью, без вероятностной потери или primal-only ограничения. Отказ при null/
пустых tags, переполнении500, processing cooldown>0 или недостаточной энергии.

Каждый серверный тик transient count++ и cooldown уменьшается на1, если>0.
Каждый пятый тик:

1. Сохраняется старое heat и выполняется **heat-- без нижнего clamp**.
2. Если новое heat≤500, запросить ignis50 raw CV и прибавить реальный grant.
3. Если power1≤500, запросить perditio50 и прибавить grant.
4. Если power2≤500, запросить aqua50 и прибавить grant.

Сравнения `<=500`, запросы всегда50, не `min(room,50)`: энергия способна достичь550.
Heat без источника уходит ниже0; нельзя незаметно заменить это состоянием0.
Perditio/aqua не теряются вхолостую, но могут заранее заполняться до550 без предмета.
Один processing с total tags=a требует `heat>=2*a,power1>=a,power2>=a`, списывает
`2*a/a/a`, затем вычисляет cooldown на **оставшемся heat**:

```
(int)(5F + max(0F, (1F - heat/500F)*100F))
```

Полученные аспекты добавляются полностью, vis обновляется суммой. Bellows объявлен
как поле, но никакого влияния bellows в этом классе нет. Нет редстоун-паузы.

Disk NBT содержит aspects, shorts vis/heat/power1/power2. Client custom NBT —
только vis/heat. Счётчики count/processed и destroy transient; исходная перезагрузка
не сохраняет processing cooldown. Для современных int-накопителей важно отдельно
согласовать сохранение отрицательного heat и его short overflow, не назвать clamp
исходным поведением.

## Четыре nozzle и демонтаж

Нижние cross части — nozzle (formed metadata1). Соседний core ищется один раз
в порядке DOWN,UP,N,S,W,E; facing смотрит от core наружу. Только эта сторона
connectable/output; suction0, minimum0, extendedtube=false, add/accept=false.
Это **общий** mixed pool core, а не четыре независимых буфера. Type/amount — первый
аспект в исходном insertion order, не random weighted selection.

В оригинале пустой `AspectList.getAspects()` возвращает `[null]`, потому что делает
`toArray(new Aspect[1])`. Поэтому bound empty nozzle возвращает type=null, amount0.
Современный AspectList возвращает настоящий пустой массив — здесь нужен guard как
вынужденная API-адаптация, не как исправление несуществующего original array crash.

Подтверждённые опасные исходные пути, которые требуют явного bounded решения:

- Unbound `getEssentiaAmount` возвращает ternary null из int метода: NPE unboxing.
- `getAspects` раскрывает живой pool; отрицательный `takeFromContainer` способен
  увеличить количество; в modern endpoint нужны defensive copy и запрет negative.
- Сильный cached furnace не проверяет current BE после разборки/замены: stale nozzle
  может обращаться к отсоединённому pool. Owner identity должна проверяться до дебета.
- Если первый поиск core неуспешен, facing=null и исходный nozzle больше не тикает.

Comparator оригинала имеет precedence quirk: `floor(fill*14)+vis >0 ?1:0`.
Он выдаёт только0/1, не0..15. Сохранять именно это, пока нет разрешённой коррекции.

Поломка non-core части отмечает ближайший core destroy=true. Его следующий тик
возвращает оставшиеся formed части в исходные блоки и сам core в normal alchemical furnace. Поломка
core возвращает остальные части сразу. Одна сломанная часть даёт собственный
исходный материал: metadata1/4 advanced construct,2 alembic,3 normal construct,0
normal alchemical furnace. Pool/энергия не становятся новым предметом или бесплатной порцией essentia.

Destroy transient создаёт ещё один путь для аудита: сохранение после поломки части,
но до следующего core tick может потерять pending disassembly. При реализации надо
проверить этот restart authority case отдельно и согласовать исправление, а не
заявлять текущую invalid multiblock проверку как исходную.

## Клиент

Core для живых сущностей имеет полный куб collision, для остальных entity — высоту0,7,
чтобы dropped items попадали в рабочую область. Остальные formed части fullcube.
Outline fullcube, hardness3/resistance10,2, металл. Core emits при heat>100:
`(int)(heat/500F*12F)`, то есть при550 получается13. Формула не ограничивается12.

Оригинальный retained `adv_alch_furnace.obj` и четыре alch_furnace PNG уже есть.
Renderer рисует Base,4 повёрнутых Tank, active/inactive texture по heat>100/vis>0,
slurry/heat quads с реальной долей заполнения. Bounds3×2×3. Ambient bubbles приvis>0,
исходный lava-pop chance1/50, без server gameplay RNG для клиентских эффектов.

## Необходимые проверки следующей реализации

Реальный retained pearl в оплаченной партии advanced constructs; shortage/refused
destination не расходует ничего. Полная схема, permission/research/точные vis,
сохранение unconstrained upper-center BE. Реальные source epochs и три50-CV grants,
overshoot550 и отрицательный heat. Один ItemEntity ingredient за cooldown, полные
base+bonus tags, отказ capacity/energy без side effects. Общий pool четырёх nozzle,
без копирования/negative takes/unbound crash/stale source и без повторной оплаты
после callback. Демонтаж по каждому типу части, restart с отсутствующей частью,
native save/load, Boolean comparator и entity-dependent collision. Клиентская
инспекция остаётся самостоятельной; server pass её не заменяет.
