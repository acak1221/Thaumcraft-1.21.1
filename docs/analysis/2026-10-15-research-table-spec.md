# R07: original research table — структура, inventory и native menu

Исходный аудит от 15.10.2026 был подготовлен до реализации R07 и сам не включал запусков Java/Gradle/Minecraft. Ниже сохранена спецификация original поведения. Последующая реализация и фактически выполненные проверки описаны в разделе11; этот документ не означает завершение интерактивного puzzle или всего M01.

`O` обозначает private TC4 4.2.3.5 oracle `../work/thaumcraft-4.2.3.5-oracle`, `N` — текущий native `common/src/main/java`, `L` — retained `src/main/java`. Путь после `O/` относителен к приватному источнику; его тело не копируется в Git. Original — gameplay specification, legacy/forks — только вспомогательные материалы. Здесь описываются свежие 1.21.1 миры; старые NBT/worlds не импортируются.

## 1. Граница R07

R07 даёт естественное получение обычных столов, размещение двух блоков, превращение чернильницей, согласованные main/secondary состояния, открытие native menu с двух половин, shared inventory, перенос/shift-click, persistence/sync и безопасный разбор структуры. Это **не** готовый интерактивный puzzle.

| Unit | Принадлежит ей |
|---|---|
| R07 | Table craft/place/conversion/break; два слота; общий inventory/menu; distance/lifecycle; визуальное состояние стола/предметов и read-only note state |
| R08 | Place/erase hex, table ink debit, actor pools/refunds/Researcher bonuses, solve → discovery presentation, защита fixed/holes/solved |
| R09 | Combine/discovery и ecology bonus-aspects, их RNG/timing/source rules; atomic actor-pool + table-bonus costs |
| R10 | Button5/RESEARCHDUPE, paper+ink_sac, copies, stacked discovery/overstack semantics |
| R11 | Unknown24/42 variants и их reveal; table не должен сам раскрывать/учить unknown note |
| M04/R14 | Arcane workbench (old meta15), deconstruction table (meta14); общий исходный block не делает эти устройства частью R07 |

R07 не выдаёт research/warp/RP/vis и не копирует player knowledge в block entity. Player/table ink semantics уже разделены в [ink audit](2026-10-15-scribing-ink-semantics.md); превращение стола **не вызывает** ни один ink-damage helper.

## 2. Источники и вызывающий путь

| Original source | Что проверено |
|---|---|
| `O/thaumcraft/common/config/ConfigBlocks.java:269,325,364–365` | BlockTable + BlockTableItem; TileResearchTable и TileTable зарегистрированы |
| `O/thaumcraft/common/config/ConfigRecipes.java:454` | Обычный shaped OreDict craft Table |
| `O/thaumcraft/common/config/ConfigRecipes.java:85–86` | ArcTable/ResTable — book recipe descriptions, не отдельный crafting factory research table |
| `O/thaumcraft/common/blocks/BlockTable.java:59–240` | States/placement/opening/neighbor/break/pick/visual selection; separate wand→arcane branch |
| `O/thaumcraft/common/items/ItemInkwell.java:59–91` | Forge onItemUseFirst превращает пару столов и помещает tool |
| `O/thaumcraft/common/container/ContainerResearchTable.java:31–139` | Два слота, player slots, quick move, buttons, stillValid |
| `O/thaumcraft/common/container/SlotLimitedByClass.java:18–44` | GUI predicate по классу; default slot limit64 |
| `O/thaumcraft/common/tiles/TileResearchTable.java:61–139,297–391` | Shared state, save/sync, cache и inventory contracts |
| `O/thaumcraft/api/TileThaumcraft.java:21–47` | Один custom-NBT путь для persistence и S35 block-entity update |
| `O/thaumcraft/common/lib/utils/InventoryUtils.java:354–378` | Разбор выбрасывает inventory stacks с копией NBT, затем очищает slots |
| `O/thaumcraft/common/CommonProxy.java:127–129`; `O/thaumcraft/client/ClientProxy.java:403–405` | GUI ID10 → ContainerResearchTable / GuiResearchTable по main coordinates |
| `O/thaumcraft/client/gui/GuiResearchTable.java:92–109,404–410` | GUI255×255, actor knowledge/Researcher flags, note читается из physical slot1 |

Нормализованные source SHA-256 (UTF-8 без BOM, CRLF/CR→LF; финальная newline сохранена):

| Source | SHA-256 |
|---|---|
| BlockTable | `6098ea8658435e2d941120149e2396e8e55da468b73353ff758577c967098ed4` |
| ItemInkwell | `a47ea891305a128ab48f9d38dbfb021bd80807a01b29218322e69cf973346fd7` |
| ContainerResearchTable | `48edb2f424652dbc0283bfaabe7704702e83401117104143cd3ceda0628ef30c` |
| TileResearchTable | `cfa85e36602963466912ff91217764ef5e8c0cd673b71ef521d04124012b3f37` |
| TileThaumcraft | `98d0a20c6a2a3aefd6d63bdba8de29371219e4e246ca4149c2582c2c1c144cf1` |

## 3. Обычный стол и исходные states

Craft `Table`: результат **1** blockTable/meta0; pattern `SSS`, `W W`, где `S=slabWood`, `W=plankWood`. Это обычный crafting recipe без research/vis. Native recipe должна использовать проверенные wooden slab/plank tags; нельзя менять результат на два стола. Для пары нужны два изготовленных блока. Evidence: ConfigRecipes:454.

BlockTable имеет wood material/sound, hardness2.5, nonopaque/non-normal rendering; верхняя грань explicitly side-solid. Creative variants source перечисляет meta0,14,15. Evidence: BlockTable:59–91; `fields.csv` pinned OpenTC4 подтверждает wood/soundTypeWood.

| Metadata | Original BE | Значение |
|---|---|---|
| 0/1 | TileTable | Отдельный обычный стол, две визуальные ориентации |
| 2 | TileResearchTable | Main, partner NORTH `(0,0,-1)`; partner meta7 |
| 3 | TileResearchTable | Main, partner SOUTH `(0,0,+1)`; partner meta6 |
| 4 | TileResearchTable | Main, partner WEST `(-1,0,0)`; partner meta9 |
| 5 | TileResearchTable | Main, partner EAST `(+1,0,0)`; partner meta8 |
| 6/7/8/9 | TileTable | Secondary; обратное направление `metadata−4` ведёт к main |
| 10–13 | TileTable | Допускаются старым factory, не нормальный получаемый research variant |
| 14 | TileDeconstructionTable | Отдельная будущая unit |
| 15 | TileArcaneWorkbench | Отдельная будущая unit |

При обычном размещении любого item meta<14 old `onBlockPlacedBy` заменяет состояние на 0/1: yaw quadrant0/2 →0, 1/3 →1. Таким образом pick research item не размещает уже собранный research table. Evidence: BlockTable:93–112.

**Pick и mining drop различаются.** `getDamageValue` возвращает2 для world meta2…9; это research presentation item. `damageDropped` возвращает0 для этих же states (14/15 сохраняют себя). Разбор одной половины оставляет второй обычный стол; стандартный block loot — отдельная unit от inventory drops. Не читать `getDamageValue` как количество выпадений. Evidence: BlockTable:132–149; MCP methods.csv:3848/3897.

Selected outline main/secondary охватывает два блока вдоль соответствующей оси (BlockTable:201–222). Collision не выводить из нарисованных ножек: BlockTable не переопределяет collision/bounds. Проверенный official vanilla1.7.10 `Block` constructor задаёт bounds0…1, inherited collision использует эти bounds, `setBlockBoundsBasedOnState` пуст. Это static bytecode evidence, **не запуск старого Forge клиента**. Источник — официальный private client SHA1 `e80d9b3bf5085002218d4be59e668bac718abbc6`; joined.srg:10219/10230/10250, obfuscated `aji` constructor/collision/state-bounds; BlockContainer `ajc` bounds не меняет. Native VoxelShape не должен незаметно менять full-block collision на геометрию модели.

## 4. Превращение двух столов чернильницей

Вызывающий путь — **ItemInkwell.onItemUseFirst**, а не wand trigger и не универсальный callback любого IScribeTools. Нативная marker API сама по себе не даёт стороннему tool способность создавать research table.

Проверенное поведение ItemInkwell:59–91:

1. Требуется clicked BE `instanceof TileTable` и clicked metadata **не6**. Client возвращает false до изменений.
2. Server обходит горизонтальные стороны строго **2,3,4,5 = NORTH,SOUTH,WEST,EAST**, ищет первый соседний `TileTable` с meta<6. Ось/ориентация обычных столов не ограничивает совпадение. Вертикальный сосед не участвует.
3. Clicked block становится main/meta=выбранная сторона; его BE заменяется на TileResearchTable. Сосед получает opposite direction ordinal+4. Metadata writes используют flags0, затем оба positions получают markBlockForUpdate.
4. В main slot0 помещается **copy исходного tool с его damage/NBT**. Slot1 пуст.
5. Survival снимает один предмет из выбранного hotbar slot и помечает inventory dirty. Creative оставляет held tool, но copy в столе всё равно появляется. Это проверенный creative duplication при строительстве, не расход ink point.

Нет проверки damage<100, usable ink, research, clue, aspect discovery, paper, RP, vis или sneak. Поэтому **damage100 inkwell всё ещё создаёт стол**; затем такой tool непригоден для R08 editing. Нельзя добавлять сюда `ScribingInk.consumeFromPlayer(...,true)` или reusable-ink preflight от R06. Нет отдельного opening GUI/sound в этом методе.

Если есть только один подходящий стол, нет соседнего подходящего BE или выбранный clicked metadata6, состояние/held stack не меняются. При нескольких соседях выбор определяется source direction order, не направлением взгляда.

### Ненормальные topology quirks: не потерять при проектировании

- Проверка clicked `md !=6` **не равна** `md<2`: secondary7/8/9 имеет TileTable и может стать новым main, если рядом есть свободный стол. Secondary6 специально не проходит. Не вводить одинаковый результат для всех четырёх secondary directions под видом mechanical metadata flattening.
- Neighbor validation у main смотрит лишь metadata partner `<6`, а не его block/BE identity. Secondary проверяет наличие TileResearchTable по обратному адресу. Это асимметричная, permissive old topology (BlockTable:150–165).
- При активации secondary old code сканирует **всех** четырёх соседей по 2…5 и открывает первый TileResearchTable, не обязательно геометрически связанный main (BlockTable:184–194).

Эти ветви доказаны исходником; конкретная сложная перестройка трёх столов/neighbor-notification sequence **не проигрывалась** в old runtime. Перед переносом такой observable edge case нужен целевой fixture/oracle. Если native unit намеренно ограничит conversion свободными BASE+BASE, opening строго linked main и проверкой reciprocal topology, это отдельное явно записанное отклонение; API1.21 сам такого изменения не требует. Не объявлять эти old branches dead только потому, что обычный двухблочный пример их не использует.

## 5. Break и распад структуры

`BlockTable.breakBlock` сначала вызывает InventoryUtils.dropItems, затем superclass удаляет BE (BlockTable:127–130; official BlockContainer `ajc` break bytecode:13–20 удаляет BE). У secondary TileTable нет inventory. У main выбрасываются оба непустых stacks с исходными count/item/damage/NBT; helper после spawn очищает slot. Он создаёт отдельный Random для scatter, а не использует world RNG, и **не проверяет результат world spawn** (InventoryUtils:354–378). Не добавлять RNG draws генератора/ink к этому пути.

При потере secondary main видит partner metadata<6, выбрасывает содержимое, меняет свой BE на TileTable и state на **meta0**, теряя прежнюю ориентацию. При потере main secondary без соответствующего TileResearchTable также становится **meta0**. Оставшийся стол не уничтожается. Новый neighbor update не должен второй раз выдать старые inventory stacks. Evidence: BlockTable:150–165.

Native implementation должна разделить block-item loot и inventory evacuation, выполнить их только authoritative server-side, очистить/передать inventory ровно один раз и не трактовать смену MAIN→BASE как второй разбор того же inventory. Reconfiguration, break другой половины, explosion/command replacement и pending menu clicks должны использовать один lifecycle contract. Raw block drops в creative/explosion зависят от inherited engine/loot policy; отдельный old Forge runtime этих случаев здесь не запускался.

**Unloaded partner не равен удалённому partner.** Не схлопывать структуру и не выбрасывать stacks только потому, что соседний chunk пока не доступен при reload. Перед implementation проверить old engine notification/load ordering; сравнение только `getBlockEntity(partner)==null` не является достаточным old break oracle. Конкретный native chunk-border fixture обязателен.

Отмена/провал создания output entity не разрешает молча терять inventory. Нужен воспроизводимый native break-output failure case и явная policy сохранения/retry/отказа; это resource/authority correction к helper, который игнорировал spawn result, а не новая игровая цена. Не обещать rollback уже разрушенного блока без реализованного lifecycle механизма.

## 6. Два слота, shared access и menu

TileResearchTable наследует **TileThaumcraft + IInventory**, а не TileThaumcraftInventory/ISidedInventory. Нельзя случайно перенести unrelated syncedSlots/default inventory rules базового класса. `contents[2]`, inventory limit64; display name `Research Table`, custom-name flag false. `openInventory/closeInventory` пусты; закрытие GUI не возвращает физические предметы пользователю и не очищает table. Evidence: TileResearchTable:61–68,297–358.

| Slot | GUI SlotLimitedByClass | Tile isItemValidForSlot |
|---|---|---|
| 0 `(14,10)` | любой IScribeTools, включая exhausted tool | любой IScribeTools; usability отдельно при edit |
| 1 `(70,10)` | любой ItemResearchNotes, **включая solved meta64** и unknown variants | exact ConfigItems.itemResearchNotes и metadata<64; discovery rejected |

GUI predicate не вызывает tile slot-validity. Иначе невозможно вставить discovery для R10. В текущем flattening normal research_note и discovery — две registry identities одного native ItemResearchNotes; menu принимает обе, а automation predicate соответствует normal identity. Unknown identities добавляются R11; наличие/отсутствие component само по себе не заменяет item discriminator. Validity/insertion не учит note и не решает grid. Evidence: ContainerResearchTable:32–39; SlotLimitedByClass:35–43; TileResearchTable:358–375.

Source slot limit64 не превращает обычный maxStack1 note/inkwell в stackable предметы. Однако R10 сам увеличивает count уже лежащей discovery; R07 не должен заранее придумать clamp-to1 для load/sync этого состояния. Полная overstack совместимость проверяется в R10, а не объявляется доказанной здесь.

Menu содержит **38 slots**: 0/1 table; 2…28 player main indices9…35; 29…37 hotbar0…8. Координаты player grid `(48+j*18,175+i*18)`, hotbar `(48+i*18,233)`. Quick move из table идёт в player range `[2,38)` в обратном порядке; из player — `[0,2)` вперёд, с class filtering. Нет отдельного player main↔hotbar fallback для неподходящего предмета. Evidence: ContainerResearchTable:32–55,69–134. Container button1 — no-op true, button5 вызывает duplicate; R07 не должен подключать R10 до его собственной server validation.

**Владельца нет.** `researcher` объявлен, но в original классе нигде не присваивается/не читается. Нет owner UUID, эксклюзивной блокировки, owner-only opening или research gate. Два игрока видят/забирают одни и те же physical slots и table bonus set; их player discoveries/pools/Researcher bonuses остаются личными. Не превращать distance/ownership requirement плана в новую приватизацию стола.

`canInteractWith` делегирует tile usability: world BE по **main position должен быть тем же экземпляром**, distance² до main center `(x+.5,y+.5,z+.5)` **<=64**, то есть8 blocks. Secondary opening использует main coordinates, поэтому расстояние считается от main. Замена блока на новый table того же типа не оставляет старый menu действительным. Нет ограничения «только один viewer». Evidence: ContainerResearchTable:137–139; TileResearchTable:344–346.

### Vanilla hopper behavior также часть physical inventory

Official1.7.10 Hopper bytecode подтверждён по тому же private client JAR: joined.srg:11820/11832 связывает `api.a/b(IInventory,ItemStack,slot,side)` с canInsert/canExtract. Insert вызывает inventory slot-validity, затем optional ISidedInventory restriction; extract для **не ISidedInventory** возвращает true. Поэтому main допускает extraction обоих slots; normal note/IScribeTools insertion проходит по tile predicate, discovery insertion не проходит. Secondary TileTable не inventory.

Для native vanilla hopper достаточно корректного Container bridge у main BE; направление само по себе не создаёт новые ограничения. Other-mod item-handler/transfer capabilities относятся к отдельным integrations M13, но обычный hopper нельзя забыть, реализовав только GUI slots.

## 7. Persistence, cache и synchronization

TileResearchTable custom data:

| Field | Source representation/behavior |
|---|---|
| `Inventory` | List compounds, Slot byte + полноценный ItemStack NBT; read максимум2 entries, unsigned slot, допустимые0/1 |
| `nextRecalc` | Int, сохраняется и синхронизируется |
| `bonusAspects` | List `{tag}` **без amount**; known aspects читаются через merge(aspect,1) |
| `data` | Transient decoded note cache; не отдельный NBT и не источник item truth |
| `researcher` | Не сохраняется/не используется |

Evidence: TileResearchTable:70–113. `markDirty` вызывает gatherResults: cache очищается и заново читается из slot1 ItemResearchNotes (129–139). При load cache может оставаться null до первого gather; native immutable ResearchNoteData лучше читать из authoritative stack/инвалидировать при каждой замене, не хранить mutable snapshot другого предмета.

Весь custom payload записывается и для save, и для old S35 update packet через TileThaumcraft:21–47. Это включает оба stacks, bonus set и timer, поэтому world renderer/несколько viewers должны видеть актуальные tool/note компоненты. `onDataPacket` вызывает render update; block event id1 играет **client-side** `thaumcraft:learn`, volume1/pitch1 (TileResearchTable:384–399). Событие относится к solve/duplicate, не открытию меню/перемещению обычного slot; actual trigger входит в R08/R10.

R09 timer boundary: server condition `nextRecalc++ >600` с reset0 даёт первый проход на **602-м** tick при начальном0, не600. Bonus set принадлежит столу, сохраняется как presence; R07 не должен начислять fake bonus или вызывать иной RNG до R09. R09 затем подключит environment scan `[-8,+8]³`, source priorities, ранние return и исходные RNG draws (TileResearchTable:115–123,189–295).

## 8. Вынужденные modern adaptations и integration contracts

1. **State/IDs.** Metadata flattening требует явного отображения BASE axis, MAIN facing и SECONDARY direction. Варианты arcane/deconstruction резервируются отдельными native IDs/units. Выбор одного Block с PART/facing или отдельных blocks допустим, если сохраняет таблицу выше, reciprocal resolution, survival drops и documented pick presentation. Не создавать второй inventory на secondary.
2. **Use ordering.** Forge `onItemUseFirst` предшествовал block GUI activation. На1.21 нельзя полагаться только на обычный Item.useOn после успешного block interaction: превращение уже существующего secondary может перехватиться menu opening. Common block `useItemOn`/проверенный early-use hook должен вызвать conversion до обычного open; использовать фактический hand, а не старый selected hotbar assumption. Любое исключение старых topology quirks описывается отдельно.
3. **BE/menu.** Main implements native Container/MenuProvider; server constructor получает actual BE, client constructor bounded main BlockPos/open data. Все38 slots корректно синхронизируют ItemStack components; no client copies становятся server authority. `stillValid` проверяет actual BE identity + level + source distance8. GUI opening не требует held book.
4. **Cross-loader opening.** В local Architectury13.0.11 sources существуют `MenuRegistry.ofExtended`, `openExtendedMenu(ServerPlayer, MenuProvider, Consumer<FriendlyByteBuf>)` и client-only `registerScreenFactory`. Это кандидаты integration, не утверждение о протестированном R07. Registry/menu factory в common; screen registration только client. Нет common import client screen.
5. **Save/update.** Native HolderLookup-aware ItemStack save/load сохраняет существующие ResearchNoteData components; `setChanged` + block update и vanilla menu slot synchronization. Текущие N TileJar/TileNode показывают target getUpdateTag/getUpdatePacket pattern, но не доказывают новый inventory/menu. Собственная schema/version validation не означает legacy importer.
6. **Authority.** Open menu должен относиться к существующему main; future R08/R09/R10 intents принимают настоящий sender, текущий containerId/target/revision и stillValid, затем повторяют costs/data guards. Старые PacketAspectPlaceToServer:75–91 и CombinationToServer:89+ принимали coordinates без проверки открытого menu/дистанции и доверяли части client flags; этот обход не переносится. Во время R07 необработанные edit/combine/copy intents не могут мутировать стол.
7. **Visuals.** Original renderer рисует двухблочный ModelResearchTable из main, secondary TileTableRenderer при meta>=6 ничего не рисует. Quill/inkwell показываются при наличии IScribeTools даже exhausted; note scroll использует saved color, malformed/no data fallback0x999999. Main render bounds `x−1..x+2,y..y+2,z−1..z+2`; не допускать frustum clipping половины стола. Evidence: TileResearchTableRenderer:40–118; TileTableRenderer:32–47; TileResearchTable:377–380. Model/texture adaptation нуждается в просмотренном клиенте, наличие PNG не является visual parity.

Обязательные минимальные интеграционные поверхности будущего implementation: raw table registry/BlockItem/recipe, pair-state resolver/conversion, один main BE, slot predicates GUI отдельно от automation, MenuType+server provider+client screen, BE persistence/update, one-time teardown и обоих loader runtime fixtures. Bootstrap/network/shared files назначает root; этот документ никому не передаёт право менять их параллельно.

## 9. Meaningful acceptance cases R07

| Case | Что должно различить правильную реализацию от ошибки |
|---|---|
| Natural craft/place | SSS/W W даёт1; два vanilla-crafted столика размещаются с обеими исходными axis; один/вертикальная пара не конвертируется |
| Four directions/priority | Все N/S/W/E main↔secondary mappings; при нескольких соседях N→S→W→E, не nearest/view direction |
| Exact construction costs | Survival переносит tool copy/NBT/damage и снимает1item; creative удерживает source tool; damage100 тоже конвертирует; ink/RP/vis/research/warp/RNG не расходуются construction policy |
| Hand/early use | Нативный main/offhand interaction не забирает предмет из другого hand; conversion проверяется до GUI opening; отдельный topology fixture для secondary6 против7/8/9 |
| Shared access | Два real players открывают один main с разных половин, видят одинаковые slots; slot transfer одним обновляется у другого; close/reopen не выдаёт private copies и не очищает table |
| Slot policies | GUI принимает ordinary/discovery, exhausted ink; чужой item отвергается; hopper принимает ordinary/tool, отвергает discovery insertion, извлекает physical slots; secondary не имеет отдельного хранилища |
| Quick move | Table→player reverse и player→table forward; full inventories, неподходящий stack и перенос1/count/components без duplication/drop; player indices не перепутаны с menu indices |
| Distance/stale menu | Main-center distance²64 допускается, чуть больше отвергается; removed/replaced main закрывает старый menu даже после установки такого же block; future edit intent без правильного menu отвергнут |
| Break either half | Все4 направления: inventory выпадает1раз с intact components/count, другая половина остаётся BASE0; повторный neighbor update/close/reopen не даёт второй output |
| Chunk/save/update | Пара пересекает chunk boundary; unload/reload/restart не схлопывает её и не теряет slot0/1; actual encoded S2C/menu state совпадает у обоих viewers и renderer |
| Declared failure policy | Native canceled output spawn при teardown не теряет/не дублирует inventory согласно выбранному retry/отказ contract; don't fake success по non-null entity |
| Unit boundary | R07 умеет хранить unfinished и solved note, но не изменяет grid, не начисляет bonus/combination/copy/learning и не заявляет реализованными R08–R10 |
| Client proof | Обычный поставленный стол и преобразованная пара, ориентации, tint/tool presence, opening с обоих halves, readable38-slot screen; real gameplay clicks, просмотр точных PNG обоих поддерживаемых loaders |

GameTests через production interaction/menu проверяют инвентарь, связь halves, distance, multi-player и persistence. Реальный клиент отдельно доказывает открытие/слоты/рендер; test setup готового grid не является R08 puzzle playthrough. До фактических запусков эти cases остаются **unverified**.

## 10. Открытые решения перед кодом (исходный аудит)

- Зафиксировать native block/item state mapping, включая source pick/meta2 presentation и будущие arcane/deconstruction identities.
- Получить explicit disposition трёх permissive topology branches; обычная BASE+BASE конверсия уже полностью определена, сложная перестройка source-observed, но runtime-unverified.
- Определить ресурсосберегающий teardown failure path и unloaded-neighbor handling, не заявляя атомарность destroy/spawn до его реализации.
- Согласовать R07 menu/read-only grid против полноценного R08/R09 интерфейса; UI не должен выглядеть работающим редактором без server policy.
- Проверить inherited modern/old loot/creative/explosion и registry-backed native menu APIs на actual target при реализации. В этом аудите запусков Gradle/игры нет.

## 11. Native реализация и проверенные границы

R07 использует один native `BlockTable` с variant0…9, два BE types, `ContainerResearchTable` с38 slots и общий main inventory. Регистрируется один обычный table craft с output1; flattened research pick item имеет отдельные имя/модель, но, как source meta<14, размещает base0/1. Его BlockItem не перезаписывает normal `Block.asItem` mapping. Common registry/menu не импортируют client screen; screen/BER и selection-only mixin загружаются на client.

Сохранённые source quirks: north/south/west/east priority, secondary7/8/9 reconversion при исключённом6, main допускает любой TABLE secondary>=6, shared public access без owner, exhausted inkwell допускается при construction, creative копирует tool, survival расходует held item1 без ink damage. Native menu сохраняет original whole-remainder quick-move; automation и GUI predicates различаются. Revision/edit/combine/copy здесь ещё не вводились.

Вынужденные адаптации к1.21.1:

- У произвольного современного не-TABLE блока нет общего old metadata. Такой partner считается невалидным; permissive TABLE-to-TABLE ветки сохранены. Unloaded partner не загружается принудительно: сохраняется структура с retry20 ticks. Constructor/`clearRemoved` создают transient first-native-BE-tick проверку, поскольку chunk deserialization не вызывает `onPlace` и может не содержать scheduled tick.
- Исходная full-cube collision/ray отделена от двухблочного selection outline. Client mixin меняет только `LevelRenderer.renderHitOutline`, а не взаимодействие/коллизию.
- Evacuation сохраняет копии в per-level native SavedData queue и очищает physical slots до доставки. Entity spawn выполняется на безопасном `SERVER_LEVEL_POST`, после завершения `onRemove/setBlock`: synchronous loader callback не может потерять новую callback-created BE из-за ещё не завершившегося outer remove. Неудачная доставка остаётся pending; каждый rejected output получает transient cooldown не менее20 ticks. Новый enqueue будит eligible работу, но не обходит этот cooldown; queue-wide cadence/batch64 могут отложить готовый retry. После reload transient delay сбрасывается. На один flush обрабатывается максимум64 entries; stable UUID не создаёт второй output при уже существующем идентичном entity. Это authority/data-loss adaptation; crash-atomic транзакция между разными world files не заявлена.
- Native ItemStack save codec ограничен count99. BE и pending queue хранят native stack count1 плюс отдельный integer count; count>99 dropped stack использует native ItemEntity subclass с serialization view count1 и сохранением полного `StoredCount`. Tracked physical stack при save не мутируется. **Player/container lifecycle>99 этим не закрыт:** original whole-stack quick-move может положить127 в player slot, а native `Inventory.save` тогда падает. R10 до включения natural copying обязан адаптировать player/container/cursor/drop пути и проверить сохранение, без скрытого clamp/split original quirk.

Текстуры и GUI:

- Original `models/table`64×32 и `models/restable`128×64 PNG оставлены byte-identical, UV не перенормируются. Native `AnimationMetadataSection.EMPTY` использует полные width/height, `SpriteResourceLoader` допускает такие frames, `SingleFile` подключает их explicit atlas sources. Converter разрешает только эти два проверенных static sheets с точными dimensions/no-mcmeta contract; legacy strips/low-mipmap guards сохранены. Provenance — `resource-manifest.json`; private native disassembly — `.tmp/r07-atlas-{javap,resource-javap,proof-javap}.txt`.
- Common native-codec tests исполняют настоящий `SingleFile.CODEC`, проверяют full-image frame и отказывают missing/invalid resource ID. Python mutation checks отвергают случайное animation slicing и неразрешённый rectangular strip.
- Baked DOWN-face UV исправлены после native ModelPart/FaceInfo/BlockFaceUV probe:264 vertices без расхождений в данном преобразовании. Probe использует JSON UP/WEST anchors и не доказывает original geometry/lighting parity сам по себе. Private evidence — `.tmp/R07UvProbe.java`, `.tmp/r07-uv-probe-{research,base}-after.log`.
- Client1 script прошёл19 scenes, но PNG17 выявил opaque hex/particle pixels: plain native texture blit не включает alpha blend. Исправлены scoped blend/flush/color restoration; final client2 PNG17 показывает faint hex и корректный unknown endpoint. Research table GUI пока read-only; palette/edit/combine/copy остаются R08–R10.

После финальных production исправлений `.tmp/r07-validate7.log` —161JUnit и41+41 real-loader GameTests без failures/errors/skips; `.tmp/r07-assets3.log` —27Python PASS. Native runtime различает loaded-cross-chunk NBT recreation, настоящий unload/disk reload и cold-load repair без scheduled ticks. Actual ADD cancellation/retry, unrelated enqueue cooldown и replacement inventory проверены отдельно, не выводятся из UUID-collision fixture. RejectedEntitySpawn с тремя accessor mixins освобождает только новую unattached reservation UUID после discard, exact-attempt/known-before/NULL-callback/no-owner guards. Проверка existing hidden owner создаёт его после snapshot, скрывает section и доказывает сохранность сущности/stack/UUID. Validate5 нашёл реальную Fabric reservation leak; validate6 обнаружил global cooldown early wake; финальный validate7 проверяет оба исправления.

Actual Fabric client3 `.tmp/r07-client3.log`/exit.json — SCRIPT_PASSED/exit0, run `client-smoke-91bd518d-9a58-4451-addf-4e3becd205c8`. Все19 exact PNG просмотрены root;15–19 независимо вторым агентом. Проверены native placement/conversion, actual container quick moves, opening обеих половин, world/GUI/item contexts. Restart probe перед любыми setup mutations сравнил **весь native Inventory NBT list** пары из завершённого client2 с заново загруженной BE; source run `client-smoke-b42fee38-3002-45f7-bf25-fa8984d74cd2`. Ожидаемый NBT извлечён read-only из owned saved `.mca`, а не создан повторной генерацией note; private expectation сохранён и помечен verified. Оригинальный source fixture world не открывался; его `level.dat` hash до/после совпал.

Это narrow Fabric-dev visual evidence, EN table640×360 logical GUI. NeoForge/packaged table client, research-item dropped context, full lighting/animation/audio/frustum parity и полный survival progression остаются unverified. Скриптовые inventory/knowledge/restart assertions не приписываются самим PNG. Дальнейшая реализация — [R08 hex editing](2026-10-15-research-table-editing-spec.md).
