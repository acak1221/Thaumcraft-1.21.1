# Research notes: спецификация следующей native unit

Проверено 15.10.2026. **Gameplay source — только TC4 4.2.3.5 original oracle.** Эта работа читает исходники и проектирует следующий шаг; production code, Gradle и game runtime не изменялись/не запускались.

Обозначения evidence:

- `O` = `C:/Users/Acak/Documents/Codex/2026-10-15/new-chat/work/thaumcraft-4.2.3.5-oracle` — приватный CFR source оригинального JAR, вне repository.
- `L` = repository root `src/main/java` — сохранённый FOREVA 1.12 reference, вторичный источник.
- `N` = repository root `common/src/main/java` — активный 1.21.1 port.
- `R` = `../work/references/OpenTC4` at `80aac29c52543ef51766c4b0788fba782abb3b0b` — вспомогательная readable mapping, не gameplay specification.

Строки ниже относятся к проверенным файлам oracle/legacy на дату аудита, а не к будущим исправлениям. Fresh 1.21.1 worlds остаются единственным target; описание old NBT **не разрешает импорт старых миров**.

## 1. Данные и отдельный item discriminator

Original `ResearchNoteData` содержит `key`, `color`, `hexEntries`, `hexes`, `complete`, `copies`; оба map keyed строкой `q:r`. `HexEntry` содержит `Aspect aspect` и `int type`. Evidence: `O/thaumcraft/common/lib/research/ResearchNoteData.java:11`; `O/thaumcraft/common/lib/research/ResearchManager.java:853`.

Original item NBT schema:

| Tag | NBT type | Значение |
|---|---|---|
| `key` | String | Canonical research key, без переименования в modern namespaced research ID |
| `color` | Int | RGB primary aspect на момент генерации; поле сохраняется, а не вычисляется заново при чтении |
| `complete` | Byte/Boolean | Решён ли puzzle |
| `copies` | Int | Счётчик сделанных копий; при генерации 0 |
| `hexgrid` | List of Compound | Только существующие cells; вырезанные holes отсутствуют |
| `hexgrid[].hexq` | Byte, signed | Axial q |
| `hexgrid[].hexr` | Byte, signed | Axial r |
| `hexgrid[].type` | Byte | 0 blank; 1 fixed endpoint; 2 player placement |
| `hexgrid[].aspect` | Optional String | Canonical aspect tag; writer опускает tag при null |

Evidence: `O/thaumcraft/common/lib/research/ResearchManager.java:277`, `:324`, `:339`, `:367`.

`getData` возвращает null только для null stack или отсутствующего NBT. Он не проверяет тип item/key/shape: отсутствующие tags читаются vanilla defaults, повторная coordinate перезаписывает предыдущую запись в maps. `updateData` записывает эти пять верхних полей и новый `hexgrid`, сохраняя другие NBT tags stack. Это permissive original parser, **не подходящий server validation** для нового component.

Отсутствующие primitive tags дают `key=""`, `color=0`, `complete=false`, `copies=0`; отсутствующий aspect string становится пустой строкой и resolve→null. Эти primitive defaults подтверждаются MCP method mappings R `src/main/methods.csv:1120`, `:1124`, `:1128`, `:1136`. Native component absence следует отличать от такого malformed/default-filled data. Original item также возвращает true из `getShareTag` (`O/thaumcraft/common/items/ItemResearchNotes.java:140`; mapping `R/src/main/methods.csv:2085`), поэтому note component обязан иметь network synchronization, а не только persistence.

**Metadata не хранится в ResearchNoteData и не равен complete flag:**

| Original meta | Нормальная роль |
|---:|---|
| 0 | Обычная unfinished research note |
| 24 / 42 | Unknown Knowledge, reveal on use; изначально может не иметь ResearchNoteData |
| 64 | Discovery после решения puzzle |

Original renderer/name/rarity используют разделение `<64` и `>=64`; right-click learning проверяет `data.complete`, **не meta64**; table placement проверяет meta `<64`; duplicate проверяет meta `==64`, **не data.complete**. Нормальный table solve меняет оба значения. Нельзя просто удалить один из сигналов при flattening без описанного mapping. Evidence: `O/thaumcraft/common/items/ItemResearchNotes.java:82`, `:90`, `:154`; `O/thaumcraft/common/tiles/TileResearchTable.java:148`, `:180`, `:402`.

Первый component milestone может охватить только известные normal research notes. Unknown 24/42 и решённый item presentation должны остаться явно pending до выбора native item/component mapping; component отсутствует у неизвестного свитка, пока он не раскрыт.

## 2. Исходный генератор

### Стоимость, порядок аспектов, цвет

`createNote` ищет ResearchItem и его primary aspect; при отсутствии primary возвращает null. Primary — **первый аспект с наибольшим strictly positive amount**; при равном максимуме остаётся первый. `AspectList` использует `LinkedHashMap`; `getAspects()` сохраняет insertion order. Эта же последовательность назначается fixed endpoints. Нельзя сортировать `aspectCost` по tag/amount. Evidence: `O/thaumcraft/api/research/ResearchItem.java:244`; `O/thaumcraft/api/aspects/AspectList.java:23`, `:62`; `O/thaumcraft/common/lib/research/ResearchManager.java:268`, `:290`.

`ResearchDefinition.aspectCost` в N уже сохраняет insertion order через `LinkedHashMap`. На corpus проверено: 172 definitions имеют ненулевые aspect costs; complexities: 73×1, 83×2, 16×3; фиксированных endpoints от 2 до 8. Все эти complexities уже входят в [1,3]. Original nonvirtual ResearchItem constructors дополнительно clamp complexity к [1,3], хотя `setComplexity` сам не clamp. Evidence: `O/thaumcraft/api/research/ResearchItem.java:56`, `:73`, `:239`.

**Extractor ordering отдельно перепроверен:** `scripts/porting/extract_tc4_definitions.py:173` добавляет aspect costs в Python dict по порядку `.add` calls; `json.dumps` на строке402 **не** передаёт `sort_keys`. Sorted только список research rows по key, flags и fingerprint input; вложенный aspect_cost не сортируется. Read-only re-extraction всех201 original definitions и comparison `list(aspect_cost.items())` с bundled JSON: **0 order mismatches**. Например INFUSION остаётся `[praecantatio:6, machina:3, fabrico:6]`, primary `praecantatio`; сортировка по tag поменяла бы primary на `fabrico`. Repair данных сейчас не нужен; обязательный следующий regression должен проверять ordered pairs, потому что существующее Python dict equality metadata audit не замечает перестановку ключей.

### Геометрия и RNG

Axial neighbours в порядке направлений 0…5: `(1,0), (1,-1), (0,-1), (-1,0), (-1,1), (0,1)`. Ring начинает в `(-radius,+radius)` и проходит эти направления; grid включает origin и все rings. Evidence: `O/thaumcraft/common/lib/utils/HexUtils.java:11`, `:38`, `:67`.

`radius = 1 + min(3, complexity)`. Сначала создаётся полный grid и type0/null во всех cells. На outer ring равномерно выбирается столько endpoints, сколько distinct research aspects, и им по insertion order назначается type1/aspect. Evidence: `O/thaumcraft/common/lib/research/ResearchManager.java:281`.

| Effective complexity | Radius | Полный grid | Удаляемые blanks | Cells после генерации |
|---:|---:|---:|---:|---:|
| 1 | 2 | 19 | 0 | 19 |
| 2 | 3 | 37 | 4 | 33 |
| 3 | 4 | 61 | 6 | 55 |

**Quirk kept:** `distributeRingRandomly` потребляет `random.nextInt(ring.size())`, но не использует полученный start; position начинается в 0. Spacing считается как float `ring.size()/entries`, индекс выбирается Java `Math.round(position)`. Native HexUtils уже исправлен отдельно с exact endpoints/RNG regression tests. Legacy `L/thaumcraft/common/lib/utils/HexUtils.java:55` всё ещё использует случайный start — брать этот участок из legacy нельзя. Evidence original: `O/thaumcraft/common/lib/utils/HexUtils.java:53`.

Для complexity>1 генератор удаляет `complexity*2` blank cells. На каждой попытке:

1. `temp = hexes.values().toArray(...)`, где `hexes` — original `HashMap<String,Hex>`.
2. Потребляется `world.rand.nextInt(temp.length)`; fixed endpoint не удаляется, но RNG draw уже потрачен.
3. Для каждого соседнего fixed endpoint считается его **текущее** число существующих neighbours; candidate запрещён, если это число меньше 2. Сам candidate входит в счётчик, поэтому после удаления у endpoint может остаться один neighbour.
4. После успешного удаления оба maps обновляются, temp пересоздаётся; неудачные попытки повторяются без decrement blanks.

Evidence: `O/thaumcraft/common/lib/research/ResearchManager.java:296`. Алгоритм не проверяет полную связность grid и не выбрасывает все неудачные candidates заранее. Original retry loop не имеет лимита.

**Открытая fidelity-проверка до включения генератора:** порядок `HashMap.values()` влияет на holes и число RNG draws. N HexUtils возвращает `LinkedHashMap` для полного grid, original — HashMap; последующее заполнение нового HashMap может менять collision-chain insertion order. Совпадение coordinates и количества cells не доказывает seed parity. Нужен private Java8 oracle harness и сравнение с Java21, либо явная документированная политика для этого вынужденного различия. Не сортировать grid молча. Замена retry loop на shuffle тоже меняет RNG/распределение.

World RNG должен получать те же bounded-int calls: при complexity1 ровно один draw с bound12; complexity2 начинает с bound18, далее hole attempts на текущих размерах37→36→35→34; complexity3 начинает с bound24 и hole attempts на61→60→59→58→57→56. Rejected attempts повторяют текущий bound. Нельзя seed отдельный Random через лишний world `nextLong` и утверждать идентичный random stream. Удобен injectable bounded-int adapter к target `RandomSource`, с `java.util.Random` в oracle vectors.

Простые endpoints, не зависящие от seed:

- `FOCUSFIRE`: complexity1, radius2, `ignis` в `(-2,2)`, `praecantatio` в `(2,-2)`, цвет `ignis` при tie3/3, 19 cells.
- `FOCUSHELLBAT`: complexity2, radius3, по порядку `iter→(-3,3)`, `bestia→(2,1)`, `ignis→(3,-3)`, `praecantatio→(-2,-1)`, primary `bestia`, 33 cells после удаления4. Точные holes требуют oracle RNG vector.

## 3. Создание note и расход scribing tools

`createResearchNoteForPlayer` сначала ищет **первый** research-note item с тем же key в main inventory (обычный/solved discriminator не ограничен). Если найден, возвращает существующий stack и ничего не расходует. Иначе требует usable scribe tool и одну paper, тратит paper/один damage tool, генерирует note и добавляет в inventory; при отсутствии места drop. Сам factory не проверяет parents, hidden clue, difficulty или learning state — внешний browser/network flow делает часть этих проверок. Evidence: `O/thaumcraft/common/lib/research/ResearchManager.java:120`, `:187`.

В original условие с `InventoryPlayer.consumeInventoryItem(paper)` уже расходует paper до вызова createNote, а ink расходуется перед генерацией; invalid key/null primary мог привести к потере resources/crash. Native service должен подготовить и проверить note **до atomic commit costs**, сохранив нормальную стоимость, с documented crash/authority correction. L уже генерирует note до расходов, но это вторичное evidence, не новая gameplay норма.

Original ink rules (`O/thaumcraft/common/lib/research/ResearchManager.java:199`, `:211`):

- Player: первый `IScribeTools` в main inventory с `damage < maxDamage`; preflight не меняет state, commit вызывает vanilla `damageItem(1, player)`.
- Table: тот же usability predicate; commit напрямую `setItemDamage(old+1)`. Он не вызывает vanilla damage/break logic.
- TC4 `ItemInkwell` max stack1, maxDamage100; evidence `O/thaumcraft/common/items/ItemInkwell.java:41`.

Разницу player `damageItem` и table raw damage нельзя скрыть за одним универсальным helper без проверки creative/Unbreaking semantics старого vanilla. Эти engine-specific ветки пока **UNVERIFIED** по vanilla source; сами два различных вызова доказаны oracle.

Browser normal note request: unfinished research, parents satisfied, primary-mode branch, usable tool+paper и нет уже имеющейся note. Secondary mode — difficulty −1 для всех aspectful research или difficulty0 для Secondary. Evidence: `O/thaumcraft/client/gui/GuiResearchBrowser.java:636`. Server packet type1 создаёт note после parent check, но не повторяет все client checks; native C2S должен опираться на реального sender, known key и main server thread, не на client player name/dimension (`O/thaumcraft/common/lib/network/playerdata/PacketPlayerCompleteToServer.java:89`).

## 4. Puzzle completion и progress

Original completion check (`O/thaumcraft/common/lib/research/ResearchManager.java:221`):

1. Собирает все type1 endpoints в pending main.
2. Выбирает первый type1 из `HashMap.values()`, удаляет его из pending и начинает DFS.
3. Переход возможен к существующему type>=1 neighbour, только если player **открыл оба аспекта** и один аспект — непосредственный component другого. Это отношение симметрично; равенство аспектов само по себе не соединяет, общий предок/потомок на несколько уровней не соединяет.
4. Направления DFS0…5; visited не обходится повторно. Достигнутые fixed endpoints удаляются из pending.
5. Если все type1 достигнуты, сохраняются все endpoints и вся достигнутая occupied component, а остальные blank/unconnected placed cells удаляются. `complete=true`, component/NBT записывается. Table затем ставит meta64 и шлёт событие learn.

Completion check сам **не** открывает research игроку, не снимает note и не начисляет warp. Это отдельное right-click learning. Malformed grid с нулём endpoints в original считается complete и очищается; все172 native aspectful definitions имеют минимум2 endpoints. Новый validator не должен позволять malformed state превратиться в forgeable discovery; это проверка доверенных данных, не новая puzzle mechanic.

Place/erase table logic (`O/thaumcraft/common/tiles/TileResearchTable.java:141`) относится к следующей table unit:

- Каждый successful edit требует и тратит1 table ink.
- Placement создаёт type2; обычный расход1 research point этого аспекта, либо bonus aspect при нулевом pool. RESEARCHER2 даёт 10% free placement; successful free branch также делает RNG draw для sound pitch.
- Erase создаёт type0/null; RNG float берётся всегда. Refund1 research point при RESEARCHER1 и `f<.25` либо RESEARCHER2 и `f<.5`; успешный refund дополнительно берёт RNG float для pitch.
- Original server method не повторяет client guards координат/типов/доступности pool. UI допускает place только на existing type0 и erase только на type2 (`O/thaumcraft/client/gui/GuiResearchTable.java:175`, `:609`). Native server обязан повторить эти guards, не позволяя изменять fixed endpoints, holes или solved notes; deviation документируется как authority fix.

## 5. Learning при использовании предмета

Evidence: `O/thaumcraft/common/items/ItemResearchNotes.java:90`; prerequisite helper `O/thaumcraft/common/lib/research/ResearchManager.java:428`; research completion/warp `O/thaumcraft/common/lib/research/ResearchManager.java:497`.

На сервере, если data присутствует, complete=true и research ещё не completed:

1. Требуются **normal и hidden parents**.
2. Открывается key и через original completeResearch начисляется соответствующий warp (при default wuss=false permanent ceil(warp/2), sticky floor(warp/2), counter обновляется).
3. Siblings открываются по одному только если ещё не завершены **и их собственные prerequisites соблюдены**. Это отличается от normal Thaumonomicon use, где sibling prerequisites не проверяются.
4. У stack уменьшается count ровно на1, звучит `thaumcraft:learn` volume.75/pitch1.

Если prerequisites отсутствуют: `tc.researcherror`, без consumption/progress/warp. Если key уже completed: обычная solved note остаётся, ничего не расходуется. На клиенте gameplay state не меняется. Не вводить requirement «player сам решил именно этот puzzle»: original notes передаваемы и позволяют другому player выучить completed note при наличии parents.

Unknown meta24/42 branch остаётся отдельной unit: выбирает Hidden research с tags, prerequisites и хотя бы одним trigger array; RNG выбора — **новый Random(worldTime/10/5)**, а не world RNG; `@` clue не требуется. Успех переводит meta в0 и генерирует обычную note. При FAIL расходуется unknown note и spawn 7–9 knowledge fragments с world nextInt(3), sound erase. Evidence: `O/thaumcraft/common/lib/research/ResearchManager.java:137`; `O/thaumcraft/common/items/ItemResearchNotes.java:108`; original9-fragment crafting recipe создаёт meta42 (`O/thaumcraft/common/config/ConfigRecipes.java:431`).

## 6. Копирование: точные расходы и найденный legacy drift

Evidence: `O/thaumcraft/common/tiles/TileResearchTable.java:398`; GUI research gate `O/thaumcraft/client/gui/GuiResearchTable.java:100`, `:592`; container button5 `O/thaumcraft/common/container/ContainerResearchTable.java:53`.

Normal client flow требует completed `RESEARCHDUPE` и `note.complete`. Сам original server duplicate проверяет research-note item, data и **meta==64**, наличие paper и `dye meta0` в main inventory; RESEARCHDUPE и complete flag он не повторяет. Native server check RESEARCHDUPE, solved flag и реального открытого table menu — required authority correction.

Стоимость одной операции при исходном `copies=c`:

- 1 paper.
- 1 legacy `dye:0` = ink sac, target `minecraft:ink_sac`.
- По каждому research aspect: `originalResearchAmount + c` points. Сначала проверяются **все** amounts, затем снимаются все costs.
- **Scribing-tool ink не требуется и не расходуется.** Это другой ink resource.

После расходов: `copies++`, updated data записывается обратно в **тот же** stack, затем count++ в table slot; block event/update. Ничего не добавляется отдельно в player inventory. Все copies в stack разделяют обновлённый счётчик. Original note maxStack1, но duplication вручную делает overstack; нельзя молча заменить механизм одним новым item с copies0. Перед реализацией проверить native save/network/slot поведение overstack и явно зафиксировать вынужденные bounds/deviation, если потребуется. Original duplicate не имеет stack-count cap.

**Доказанный legacy gameplay drift:** L `TileResearchTable.java:250`, `:273` требует/снимает `Items.FEATHER`, тогда как original использует `Items.field_151100_aR` meta0. Проверенная mapping: R `src/main/fields.csv:4106` = dye; paper mapping `:4127`. R readable `TileResearchTable.java:417`, `:434` тоже использует dye0. Переносить FEATHER из L нельзя. Legacy source-text tests проверяют stack/counter, но не ловят этот расход.

## 7. Компонент и независимые классы для следующего checkpoint

| Новый native class/unit | Ответственность | Можно до table GUI |
|---|---|---|
| `ResearchNoteData` immutable record | key/color/complete/copies + единый immutable list/map cells; persistent Codec и validated StreamCodec | Да |
| `ResearchNoteData.Cell` и cell kind | Одна coordinate + type + optional canonical aspect; не два независимо изменяемых maps | Да |
| `ResearchNoteGenerator` | Только definition + injected bounded-int RNG → data; no world/player/network; provenance на original line268 | Да, после HashMap/RNG oracle vectors |
| `ResearchNotePuzzle` | Adjacency/direct-component checks, reachability и pruning; explicit discovered-aspect set → solved data | Да, backend logic без UI |
| `ResearchNoteCosts` / creation plan | Проверенный результат генерации, tool/paper/copy cost plan без мутации inventory | Да |
| `ItemResearchNotes` + note component registration | Server-authoritative learning, transferability, count1 consumption, tooltip/tint/discriminator | Да, note выдаётся проверенной debug/GameTest setup до production issuance |
| Issuance C2S / browser primary action | Runtime difficulty/visibility/parents/duplicate-note validation + atomic inventory commit | Следующий отдельный checkpoint |
| Research table/menu/slots/edit/copy packets | Реальная structure, inventory, bonus points, edits, duplicate, per-action authority и feedback | Отдельная table unit |

Сначала attach component только к server-generated known research notes. Preserve `key/color/complete/copies/hexgrid` semantics, но native schema может иметь version1 и forced discriminator mapping. Это не legacy-NBT world importer. Unknown note form/copy overstack и production recipe не считаются реализованными по наличию DTO.

Codec validation план (нуждается в согласованном scope, не выдаётся за original permissive parser): known supported key; coordinates в max native radius4; максимум61 cells; unique coordinates; type0 без aspect, type1/2 с canonical known aspect; nonnegative copies и overflow-safe cost arithmetic; matched endpoint count/assignments для generated definition. Unknown/unsupported catalogue row и invalid payload отвергаются без resource/progress mutation. Semantic key resolution выполнять после загрузки catalog, чтобы codec static initialization не создал bootstrap cycle.

Immutable component должен действительно копировать collections и иметь stable equals/hashCode; при edit/copy создаётся новое data value, иначе Minecraft component change tracking/save/sync может не заметить mutation. Persistent и network codecs должны применять одинаковые semantic bounds.

## 8. Минимальные meaningful tests

| Тест/сценарий | Что должен различать |
|---|---|
| Codec + StreamCodec round-trip | Unfinished/solved, negative coordinates, all3 cell kinds, optional aspect, color/copies; data equality и отсутствие shared mutation |
| Invalid component rejection | Duplicate coord, unknown tag/key, type1/null, type0/aspect, outside radius, >61 cells, negative copies; обе codec paths отвергают |
| Generation order/tie | FOCUSFIRE color ignis при tie, exact fixed endpoints и ordered aspects; sorting aspects должен ломать тест |
| Complex2/3 oracle vectors | Full exact cell set, removed holes, typed endpoints и следующее RNG значение; те же counts при неправильных holes недостаточны |
| Resource preflight | Already-owned same-key note → no costs/RNG; no paper/tool/full damage → no costs; failed generation → no costs; full inventory → one dropped note |
| Ink boundary | 99→100 usable once, затем exhausted; player/table distinct mutation paths; creative/Unbreaking engine behavior проверить отдельно |
| Puzzle direct relation | AIR↔MOTION link по component; два одинаковых аспекта сами не link; common ancestor не link; unknown endpoint aspect блокирует проход |
| Puzzle result | Все endpoints connected → complete и pruning disconnected placed cells; один missing endpoint → всё ещё unfinished; никакого player unlock/warp от solve |
| Learn note | Parents/hidden parents required; first use unlock+warp+consume1, second/already-known не расходует; note передаётся другому player |
| Sibling distinction | Note learning rechecks sibling parents; normal book sibling unlock не rechecks |
| Copy consecutive | При base costs3/6, copies0: расходы3/6; следующая copies1:4/7; общий counter2/count+2; 2paper+2ink_sac, 0feather/0tool-damage |
| Copy atomic failure | Недостаток любого pool/resource/RESEARCHDUPE/menu authority → no deductions/count/copies; costs не overflow |
| Native runtime | ItemStack component save/load + encoded sync на обоих loaders; drop/pickup/death persistence; solved copy overstack под реальным engine |

Все пункты — план будущей verification. В рамках этого read-only аудита были прочитаны original/legacy files, проверены mapping и corpus counts; JUnit/GameTest/client evidence для research note unit пока отсутствует.
