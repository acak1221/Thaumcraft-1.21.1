# R08: редактирование hex research note — original contract и native integration

Обновление16.10.2026: immutable `ResearchNoteEditing` подключён к native
Tile/Container transaction, edit/state payloads и editable GUI. Выполнен Fabric
place→erase→solve→take→learn, dedicated scenario обоих loaders; evidence в
`../MIGRATION_STATUS.md`. Ниже сохраняется исходная проектная спецификация,
не полный runtime/visual parity verdict. Raw-int/short-query контракт уточнён в §11.

Аудит 15.10.2026. Это **спецификация будущей R08**, не реализация и не результат runtime R08. Прочитаны private original TC4 4.2.3.5 и текущие native API. Для этой работы изменён только данный документ; source sets, общие status/evidence, resources/build не изменялись, Gradle не запускался. Проверки R07, выполняемые ведущим, не доказывают ни placement, ни erase, ни solving через GUI R08.

`O` — private `../work/thaumcraft-4.2.3.5-oracle`, `N` — `common/src/main/java`. Исходные тела декомпиляции остаются вне Git. Спецификация относится к свежим мирам Minecraft 1.21.1, Java21, Fabric и NeoForge. Legacy 1.12 и OpenTC4 не подменяют original gameplay oracle.

## 1. Граница unit

R08 включает авторитетные действия place/erase на существующих клетках записки, literal table ink, расходы actor research pool, использование уже имеющегося shared table bonus, Researcher placement/refund RNG, вызов готового solver, переход solved note в discovery presentation, sync общего инвентаря и actor knowledge, native drag/drop/click GUI.

R09 отдельно отвечает за получение table bonus из окружения, его таймер/ecology и combine/discovery. Есть отдельный исходный R08 discovery side effect успешного erase refund неизвестного actor аспекта, описанный ниже; его нельзя случайно запретить. R10 — paper/ink_sac/copy/RESEARCHDUPE и overstack. R11 — unknown24/42. R12/R13 — остальные clues и режимы. R08 не выдаёт research completion, siblings, warp, vis, бумагу, fragments или новый note. Завершить puzzle и выучить исследование — разные действия; последнее остаётся `ResearchNoteLearning` через предмет.

Если native bonus storage/sync вводится только в R09, R08 может принять player-pool путь отдельно, но bonus-funded parity остаётся явно pending. Предпочтительный небольшой prerequisite — общий persisted/synced bonus container без ecology tick: R08 проверяет его потребление, R09 добавляет источники и combine. Нельзя трактовать отсутствующий bonus container как бесплатный запас.

## 2. Доказательства и fingerprints

Нормализация SHA-256: UTF-8 без BOM; CRLF/CR → LF; остальные символы и завершающая newline сохранены. Original JAR закреплён в `docs/porting/source-lock.json`: TC4 4.2.3.5, SHA-1 `3f51b473f9ef7a94d1a9e01f6784af1cfd811649`.

| Private original path | Основные anchors | SHA-256 LF |
|---|---|---|
| `thaumcraft/common/tiles/TileResearchTable.java` | 130–139 cache refresh; 141–187 place/erase; 189–294 bonus ecology; 388–395 completion event | `cfa85e36602963466912ff91217764ef5e8c0cd673b71ef521d04124012b3f37` |
| `thaumcraft/common/lib/network/playerdata/PacketAspectPlaceToServer.java` | 35–76 fields/codec; 78–93 receiver | `523d0f6953d74cc7b92b819539e12b81eb6464a2d47b4e112521be2ad18aa6c0` |
| `thaumcraft/client/gui/GuiResearchTable.java` | 92–108 constructor; 155–214 drag; 247–274 palette; 404–514 grid/connections; 540–639 click/hit-test; 641–659 sounds | `3f53b3dd1aa87217248213f98ab149c2dc0fce8b8dc7342e3cc638ace0aca557` |
| `thaumcraft/common/lib/research/ResearchManager.java` | 211–219 table ink; 221–266 solver; `getData`/`updateData` note serialization | `4cc13ec51f85ae315c4097296a4ff4c6d848f7f4dcbbe2c54075bf2501ae0fd5` |
| `thaumcraft/common/lib/research/PlayerKnowledge.java` | 44–45 discovery; 95–133 pool getters/mutation | `b5309c2723a39a5ead0afe4c3c061f345131b7a897ef81cbb587d081a940baa1` |
| `thaumcraft/api/aspects/AspectList.java` | 77–97 tag sorting; 125–170 get/reduce/remove/add/merge | `9f93c9d3c04aa4d0d57d29bc911312255ac28659113d0839ba73445294dd566a` |
| `thaumcraft/common/lib/utils/HexUtils.java` | 19–35 cubic rounding; 92–96 pixel→hex; 128–130 hex→pixel | `eab0048a429cb830a2318cc4d99bff315c7cc12f655ae4ffbdb8debba1aeb299` |
| `thaumcraft/common/lib/network/playerdata/PacketAspectPool.java` | 47–57 wire totals; 60–76 client update/notifications | `b9ad3bf015fcd55c6f68f9a9ec7e3def67249af20f4a1b903cac2d5e48266ad6` |

Дополнительные anchors: `O/thaumcraft/common/config/ConfigResearch.java:384–388` задаёт RESEARCHER1, RESEARCHER2 и RESEARCHDUPE; RESEARCHER2 имеет parent RESEARCHER1. Серверный placement проверяет именно наличие каждого ключа, не выводит R1 автоматически из R2. `O/thaumcraft/common/config/Config.java:388–393`: research difficulty default0, scanning soft cap default100. Table refund не вызывает scan reward/soft-cap код.

## 3. Обычный допустимый gameplay path

Оригинальный пользовательский путь: player открывает table GUI → берёт доступный discovered aspect из palette → отпускает его над blank cell → C2S → `TileResearchTable.placeAspect` → сохранение grid/ink → solver → shared table update и actor pool correction. Стирание: click по placed/type2 → тот же C2S с отсутствующим aspect.

Не смешивать **правила GUI допустимого действия** с отсутствующими серверными проверками оригинала. Для native R08 сервер закрепляет правила нормального GUI и исправляет доказанные обходы пакета из раздела 7.

| Действие | Допустимый target | Доступ к aspect | Расход |
|---|---|---|---|
| Place | Существующая клетка `type=0` у unfinished normal note | Aspect известен actor; actor pool>0 либо table bonus>0 **до** Researcher roll | Одна table ink point всегда; actor RP1 либо bonus1, кроме R2 free-placement |
| Erase | Существующая клетка `type=2`, occupied, unfinished normal note | Используется аспект самой клетки; не принимать его от клиента | Одна table ink point всегда; RP не списываются; Researcher может вернуть actor RP1 |
| Fixed/type1, hole/отсутствующий target, blank erase, placed overwrite | Отказ | Ничего не вычислять по nullable missing cell | Нет изменений и gameplay RNG |
| Discovery presentation или `complete=true` | Отказ редактирования | Только просмотр/изучение или R10 copy | Нет изменений и gameplay RNG |

Оригинальный table-side outer gate проверяет marker `IScribeTools`, usable damage, наличие `ItemResearchNotes`/data и item metadata<64 (`O/TileResearchTable:145–148`). У него нет дополнительной проверки research key completion/prerequisites, note owner, RESEARCHER1 для самого редактирования, paper или RESEARCHDUPE. Передаваемую записку может решать другой игрок; расход/refund относятся к действующему actor, не к создателю note и не ко всем viewers. Native distance/menu/identity gates относятся к авторитетности взаимодействия, а не к добавлению ownership.

Связь solved/presentation: нормальная выдача создаёт unfinished note. После solver original выставляет `note.complete=true` и item metadata64; это discovery presentation. Native должен согласованно обновить component и item identity. Для неканонической комбинации normal+complete или discovery+incomplete использовать явный отказ редактирования, сохранив stack для recovery; это не повод автоматически выучить/стереть данные.

## 4. Точный порядок costs и RNG

### 4.1 Table ink

`O/ResearchManager:211–219`: nonnull `IScribeTools` и `damage<maxDamage`. Preflight не меняет stack и не делает RNG. Commit — literal damage+1. Ветка не вызывает player `damageItem`, не применяет creative, Unbreaking или Unbreakable; item count не уменьшается. Последняя point99→100 сохраняет tool, следующий edit отклоняется. То же относится к успешному free-placement или erase без refund: слово «free» описывает RP/bonus, а ink всё равно1.

Готовый native `ScribingInk.consumeFromTable(stack, doIt)` соответствует этому контракту. `consumeFromPlayer` здесь применять нельзя: он сохраняет другой original путь issuance с creative/Unbreakable/Unbreaking и player RNG. Никакой paper/ink_sac или повторной player-inventory чернильницы при hex edit нет.

### 4.2 Place

Нормальный исходный порядок `O/TileResearchTable:149–165,176–183`:

1. Прочитать R1/R2 у actor; создать planned type2 entry выбранного аспекта.
2. Только при R2 сделать `tile world.nextFloat()`; значение **строго<0.1F** даёт free-placement.
3. Free branch не списывает RP и bonus; делает второй `player world.nextFloat()` для orb pitch `0.9F + f*0.2F`, volume0.2. Для допустимого actor это тот же world RNG instance.
4. Если free branch не сработала: при actor pool>0 списать **actor RP1**, даже если table bonus тоже есть. Только при actor pool<=0 расходуется table bonus1. Порядок RP-first нельзя заменить bonus-first.
5. Записать entry/grid в note, израсходовать literal table ink1, вызвать solver с discovered aspects actor. Затем update/sync.

R1 отдельно **не даёт** free placement и не добавляет RNG. R2 без R1 даёт ту же 10% placement-ветку: проверяются два независимых completion ключа, хотя normal progression требует R1 раньше R2.

На bonus-funded пути original `AspectList.remove` при последней единице удаляет ключ; actor discovery/pool не меняется. Нормальная ecology создаёт boolean-like bonus presence через `merge(aspect,1)`, который берёт максимум, не суммирует одинаковые бонусы. Случай полностью отсутствующего funding не является легальным free-placement: GUI его не позволяет; отсутствие server check — доказанный defect, раздел 7.

### 4.3 Erase

`O/TileResearchTable:166–179` сначала делает **один world.nextFloat всегда**, даже при R1=false и R2=false. Для legal type2 aspect!=null. Refund условие: `(R1 && f<0.25F) || (R2 && f<0.5F)`. Обе проверки используют **один** roll. При обеих Researcher нет сложения процентов и нет двух попыток: итог50%.

Успешный refund сначала делает **второй world.nextFloat** для того же orb pitch0.9…1.1/volume0.2, затем добавляет **actor RP1**, независимо от того, каким источником клетка была оплачена при placement. История оплаты не сохраняется в note; возврат в table bonus не производится. Ветка refund не ограничивает результат scanning soft cap100 и не вызывает отдельный scan reward flow. Это сохраняемая особенность: bonus-funded или R2-free cell может дать actor RP при успешном стирании.

**Erase не требует discovery аспекта у actor.** Чужая type2 cell рисуется unknown, но GUI erase helper проверяет только type2 и позволяет её стереть. Если Researcher refund срабатывает, original `addAspectPool(+1)` → `AspectList.add` вставляет неизвестный до этого tag; presence в discovered map становится true с RP1. Это доступный нормальному shared-table GUI способ открыть аспект, а не forged-packet exploit. Native `PlayerKnowledge.addAspectPool` уже имеет такое поведение. Не добавлять discovery/known-components gate к ERASE; для PLACE discovery обязателен. Server `PacketAspectPool` отправляется с amount0 и total1: client узнаёт tag/total, но не запускает amount>0 scan notifications; орб-звук refund остаётся отдельным.

После решения refund entry становится blank/type0 без aspect, ink+1, затем solver. Даже erase, который разрывает путь, вызывает тот же solver; при провале он не должен произвольно prune оставшиеся клетки. Blank остаётся в grid; erase не превращает её в hole.

| Accepted действие | Основных world float draws | Дополнительный pitch float | Player ink RNG |
|---|---:|---:|---:|
| Place без R2 | 0 | 0 | 0 |
| Place с R2, f>=0.1 | 1 | 0 | 0 |
| Place с R2, f<0.1 | 1 | 1 | 0 |
| Erase без Researcher | 1 | 0 | 0 |
| Erase с неудачным refund | 1 | 0 | 0 |
| Erase с успешным refund | 1 | 1 | 0 |
| Refused по native admission | 0 | 0 | 0 |

Последняя строка — native authority contract для отклонённых/повторных запросов, не утверждение о небезопасных forged ветках старого сервера. У sound API есть отдельная граница: actual mapped1.21.1 `Level.playSound(Player,DDD,SoundEvent,SoundSource,FF)` делает `threadSafeRandom.nextLong` на bytecode offsets15–24; это **не** `Level.random`. `ServerLevel.playSeededSound` создаёт packet без дополнительного gameplay world draw. Проверено локальным javap в ignored `.tmp/r08-native-sound-api.txt`; не объявлять обычный modern sound seed лишним draw именно gameplay world RNG. Не брать seed вручную из `level.random`, если нужен ровно исходный placement/refund поток. Client hover/erase sounds и rune animation имеют отдельный client-world RNG; они не являются server policy draws.

## 5. Solver, completion и материальный commit

Существующий pure `N/thaumcraft/common/lib/research/ResearchNotePuzzle.java:21–79` переносит `O/ResearchManager:221–266`: первый fixed endpoint; направления0…5; occupied соседи; оба аспекта discovered actor; непосредственный component в любой стороне. Равные аспекты и общий предок не соединяют. Должны быть достижимы **все fixed endpoints**. Success сохраняет connected occupied component и все fixed, включая боковые connected branches; blanks и disconnected placed удаляются. Failure возвращает исходный candidate без частичного pruning; успех даёт solved copy. Single-fixed vacuous completion сохранена существующим solver; zero-fixed исключён data contract.

Обёртка редактирования должна строить candidate immutable note, затем передавать его solver. Нельзя решить старый note до применения выбранного place/erase или повторно генерировать puzzle/color/copies. Порядок cells сохраняется для остающихся координат. На успешном accepted edit расход ink происходит даже если этот edit завершил puzzle.

**Solver использует post-refund discovery, а не исходную snapshot actor.** Original `O/TileResearchTable:170` выполняет `addAspectPool(+1)` до `checkResearchCompletion` на строке180. Поэтому успешный erase refund неизвестного аспекта способен открыть его и немедленно сделать оставшийся путь проходимым в той же action. Pure policy должна сначала применить planned refund к staged actor knowledge (положительное добавление вставляет tag), затем solve candidate с discovered set **этого staged состояния**. Это не требует публиковать промежуточный material commit: итоговые note/knowledge/ink фиксируются атомарно. Solver с pre-action discovered set здесь даёт неверный отказ.

Обязательный real-catalog synthetic vector: fixed aer `(0,0)` — placed lux `(1,0)` — fixed ignis `(2,0)`, плюс дополнительная placed lux `(-1,0)`; actor знает aer/ignis, но не lux, и имеет R1. Erase `(-1,0)` с успешным refund открывает lux/RP1, после чего solver видит оставшийся aer–lux–ignis bridge и завершает note. Стираемая клетка становится blank и удаляется successful pruning; fixed/bridge остаются. При failed refund lux остаётся unknown, note остаётся unsolved, а erased target остаётся blank в candidate; accepted erase всё равно расходует ink1.

Original completion: `O/TileResearchTable:180–182` устанавливает item metadata64 и block event1; `:388–395` client event играет `thaumcraft:learn`, volume1/pitch1, у стола. Сам research/warp не выдаётся. Original placed/erase client feedback — `hhon`0.3/1 плюс `write`0.2/1 или `erase`0.2/(1+client f*0.1); он оптимистичен и может прозвучать до server refusal. Native можно сохранить input feedback, но authoritative orb/completion effects посылать только после commit и один раз; не дублировать их actor+nearby маршрутами.

Atomic native service: admission → immutable note/pool/bonus/ink snapshots → все bounded проверки → original decision/pitch rolls → planned costs/refund в staged actor/bonus/ink → построить валидный candidate/solve с **post-refund discovered set** → перепроверить физическую table/menu identity при необходимости → commit note+costs+refund+revision вместе → dirty/sync/effects. Между material writes не вызывать внешние mod callbacks, звуки или network listeners. Policy исключение до commit не должно оставить снятый RP/ink. Произвольный откат общего world RNG после ошибки не вводить; валидаторы делают inputs безопасными **до** draw, unexpected exception логируется как defect.

Original stack count при редактировании не меняется: одна action меняет общий note NBT всего stack и стоит ink1/RP1, не count×cost. Нельзя потерять count, custom name, прочие components, key/color/copies при переходе normal→discovery. Текущий `ItemResearchNotes.create` конструирует новый stack только с note component; слепой вызов без переноса остальных данных недостаточен. Текущий `TileResearchTable.setItem` clamps64: его нельзя использовать как скрытую нормализацию direct-loaded count>64. R10 ещё должен закрыть всю overstack семантику; R08 как минимум не теряет материалы и явно задаёт свой допустимый диапазон stack состояния.

## 6. GUI geometry и client prechecks

Оригинальный layout255×255. `(gx,gy)=((screenWidth−255)/2,(screenHeight−255)/2)`. Современные `leftPos/topPos` заменяют эти origin; UI scale вычисляет их заново. Original `HEX_SIZE=9`.

| Элемент | Координаты относительно GUI origin | Поведение |
|---|---|---|
| Grid/hit-test origin | `(169,83)` | Mouse pixel `(mx−gx−169,my−gy−83)` → existing `HexUtils.Pixel.toHex(9)` |
| Aspect icon origin в клетке | `(161,75)+hex.toPixel(9)` | Иконка16×16 центрирована в grid origin+pixel |
| Palette | `(10,40)`, 80×80 | 25 discovered entries; column-major: `x=(drawn/5)*16`, `y=(drawn%5)*16` |
| Previous/next palette page | `(27,121)`/`(51,121)`,24×8 | Source страница сдвигает начало на **5**, а не25 entries; display window перекрывается |
| Tool/note slots | `(14,10)`/`(70,10)` | Уже создаются menu R07; GUI hit processing не перехватывает vanilla slot drag |
| Combine controls | Select centers около `(20,146)`/`(79,146)`, button `(35,139)`32×16 | R09, не подменять этим hex actions |
| Copy | `(37,5)`24×24 | RESEARCHDUPE + solved, R10 |

`O/HexUtils`: pixel→cubic q=`(2/3)*x/9`, cubic y=`(sqrt(3)*−y−x)/(3*9)`, z=`−q−y`; round three axes с коррекцией наибольшей ошибки → axial(x,z). Hex→pixel: x=`13.5*q`, y=`9*sqrt(3)*(r+q/2)`. Использовать существующий native utility; не менять знак r, не заменять hex hit-test квадратами16×16. Existing native `toCubicHex` имеет отдельно документированную inverse correction, но pixel-to-hex путь соответствует original. Нужны центры и границы положительных/отрицательных hex, включая tie rounding.

Palette сортируется по canonical aspect tag, не переведённому имени. Discovery определяется присутствием в discovered map, даже при RP0. Zero-pool aspect остаётся в списке и доступен при table bonus>0. При обоих запасах0 он рисуется faded и не берётся для drag. `getClickedAspect(ignoreZero=false)` реализует этот precheck; ignoreZero=true применяется к R09 shortcut combine, не к бесплатному placement.

Left mouse polling запускает drag только в palette80×80; отпускание над существующим type0 отправляет place. Отпускание на fixed/type1, occupied/type2, hole или вне grid не отправляет place; origin/membership проверяется до packet. Нажатие в `mouseClicked` вызывает erase только для existing type2. В original erase helper не проверяет конкретный mouse button: аргумент button не используется для этого пути. Не утверждать, что erase разрешён исключительно правой кнопкой. Modern UX должен явно определить, как это сочетается с `AbstractContainerScreen` slot handling, и сохранить обычный original click path.

Важное различие: original no-ink tooltip и скрытие незавершённой сетки при solved не являются server authority. Drag/client click не содержит общего usable-ink gate. Solved type2 всё ещё может дать erase packet из click helper; сервер отклоняет его по item metadata64. В native допустимо заранее не посылать заведомо refused action, но authoritative gate остаётся на сервере.

Original GUI кеширует R1/R2 статическими flags в constructor. Новый GUI должен читать актуальную actor knowledge после sync и не наследовать flags предыдущего игрока/мира. При входящей новой note revision отменять stale drag; до initial authoritative state не строить editable guessed grid. Не менять cell/RP/ink оптимистично: курсорный preview и feedback отделены от server snapshot.

## 7. Доказанные server authority defects и обязательная native адаптация

`O/PacketAspectPlaceToServer:35–93` сериализует client dimension, entity ID, table XYZ, aspect string (`"null"` для erase) и signed byte q/r. Receiver делает ограниченную проверку entity ID sender, выбирает world по client dimension, получает entity по ID и tile по XYZ, затем напрямую вызывает mutation. Отсутствие следующих проверок видно в receiver и `placeAspect`:

| Original defect | Наблюдаемый путь | Native правило |
|---|---|---|
| Нет открытого menu, distance, актуальной BE identity | Можно адресовать remote table XYZ; GUI не обязателен | Действующий `ServerPlayer`, текущий research menu, `tile.stillValid`, same level/BE identity/main state |
| Client dimension/entity lookup | Сервер ищет entity в выбранном client world; casts к player после lookup | Player и level только из authenticated connection; никаких player IDs/имен/dimension в intent |
| Нет проверки fixed/holes/type | Nonnull aspect перезаписывает fixed или вставляет новую координату; erase fixed удаляет endpoint | Membership в current component; PLACE только0, ERASE только2; fixed никогда не менять |
| Нет funding/discovery admission | Нулевой actor pool идёт в `bonus.remove`, даже если bonus отсутствует; placement продолжается | Discovered canonical tag и RP>0 либо bonus>0 до RNG; refusal атомарен |
| Unknown string превращается в `aspect=null` | `Aspect.getAspect` не отличает invalid tag от erase | Явный bounded enum PLACE/ERASE; unknown/place-empty tag отвергать, не превращать в erase |
| Erase dereference missing coordinate | После RNG читается `entry.aspect` у missing entry | Bounded/membership validation до draws и dereference; no force-load |
| Нет thread scheduling/revision | Handler напрямую меняет world; поздний запрос может попасть в другой note | Loader enqueue на server thread; menu session и expected revision; duplicate/stale rejection |

Особенно опасное сочетание: стирание fixed может убрать endpoints, а старый solver с нулём main endpoints считает puzzle complete. Native R01 уже требует хотя бы один fixed; R08 дополнительно не разрешает такую mutation. Это исправление обхода/краша, а не изменение нормальных puzzle costs. Старый dimension/entity путь также не доказывает range/sender protection только наличием одного ID comparison.

Эти отклонения должны получить authored evidence в будущей реализации; они не дают оснований добавлять note ownership, paper cost, новые research prerequisites или блокировать zero-RP+positive-bonus legal placement. Свободный R2 roll без доступного исходного funding не является самостоятельной новой услугой интерфейса.

## 8. Предлагаемый bounded menu/revision C2S contract

Это предложение, **ещё не существующий API**. Полезное имя payload — `ResearchTableEditPayload`; server service — `ResearchTableEditing`; pure decision — `ResearchTableEditPolicy` или та же service с immutable input/output.

Минимальный intent: `containerId`, server-issued **menu session token**, `expectedTableRevision`, signed-byte q/r, bounded action enum PLACE/ERASE и canonical aspect tag только для PLACE. Размер аспекта<=`ResearchNoteData.MAX_ASPECT_LENGTH`128; q/r∈[−4,4], |q+r|<=4. Packet budget можно ограничить256 bytes для этого intent; окончательный codec обязан измерять UTF-8 bytes, framing и trailing input, а не полагаться только на char count. Aspect tags текущего original короткие ASCII; ограничения не разрешают произвольные новые grid coordinates.

Не передавать world/dimension, player ID/name, note NBT/grid, pool totals, цену, free/refund roll, solver result, ink damage или bonus availability. Table определяется текущим открытым menu; `mainPos` уже дан клиенту при opening и при желании сверяется как дополнительный контекст, но не является адресом произвольной mutation.

Session token меняется при каждом открытии menu и предотвращает ABA при reuse/wrap vanilla containerId. Его generation не расходует gameplay world RNG. Token идентифицирует viewer session; физическая таблица общая, owner lock не появляется. Revision относится к shared state этой BE, не к копии каждого viewer. Изменения note/tool/count, edit, внешняя slot transfer и будущие bonus mutations обязаны менять revision и рассылать state обоим viewers. После reload меняется BE identity; старый menu невалиден независимо от совпавшего revision number.

Не ограничиваться revision++, вызываемым только из `setItem`: vanilla menu/container операции способны менять `getItem()` in place и вызывать `setChanged` позже. Ввести один audited revision owner либо сравнение material snapshot в admission. Старый запрос к note A не должен редактировать новый note B с тем же research key/равным component. Для полного ABA нужен session/state generation, не один hash cells. Увеличивать revision на успешном edit ровно один логический раз; никакого wrap в допустимом сеансе.

Server admission по порядку: authenticated sender → server thread → current menu/session/container ID → current BE identity/main state/loaded chunk/distance²<=64 → revision → actual physical slot1 note и slot0 ink → unfinished presentation/data → bounded existing cell/type → canonical/discovered aspect и funding для PLACE → pure original decision/solver → atomic commit. Порядок отказов не даёт RNG oracle или бесплатных costs. Wrong session/unknown tag не вызывает force-load или state mutation.

Accepted edit отправляет current note/tool через native menu/BE sync, actor knowledge snapshot при RP изменении/refund и подтверждённую shared revision. Other viewers получают table state, **не actor private pools**. Stale/known-valid refusal возвращает bounded authoritative correction, без расходов/RNG/звука успеха. Sender/revision надо проверять заново на server-thread execution, а не только при Netty receive. На client разрешить один outstanding edit либо явно учитывать ack; слепо переотправлять stale action с новой revision нельзя.

## 9. Существующие native API и недостающие integration points

| Native source и anchors на момент аудита | Уже есть | Чего R08 ещё не делает |
|---|---|---|
| `ResearchNoteData.java:26–35,146–170,190–216` | Immutable key/color/complete/copies/cells; radius4/max61; known aspect/key; unique coordinates; min1 fixed; `cellAt`, `withCells` | Нет edit/revision/cost mutation; complete component сам по себе не доказательство решённости произвольного клиентского input |
| `ResearchNotePuzzle.java:21–79` | Pure solve/direct connectivity; original pruning, identity on failure/already solved | Нет inventory/knowledge/warp/presentation side effects |
| `ScribingInk.java:62–66,95–100` | Pure table plan и separate native consumeFromTable | Edit service ещё не вызывает его |
| `PlayerKnowledge.java:51–87,128–149` | Discovery presence, actor pools, immutable snapshots/copy/copyFrom | Нет shared table bonus; actor pool int не повторяет автоматически все старые short overflow ветки |
| `TileResearchTable.java:33–163` | Два physical slots, stillValid, save/update components/counts, safe deferred evacuation | Нет bonus state/ecology, cached note, revision или hex mutation; `setChanged` не является готовым transaction API |
| `ContainerResearchTable.java:22–62` | Server/client constructors, mainPos/tile, native shared slots и validity | Нет R08 network session/revision/state ack; button5 остаётся R10 |
| `GuiResearchTable.java:24–141` |255×255, read-only grid/unknown icons/connectivity/tool warning | Нет palette drag/place/erase, RP/bonus display, action codec/ack/revision UI |
| `ItemResearchNotes.java:31–40` | Normal/discovery item identities и component creation | Нет preserving transition normal→discovery в table service |

Native LF fingerprints для обнаружения устаревания именно этого сопоставления: `ResearchNoteData=e4d0ca589e30c65ef84356201214141a0cd1038874c49440e85a837540e74dc7`; `ResearchNotePuzzle=efcffcb69655116a64b6e56a8967080b0af008fe1faf1f75457c686d1c19c3e2`; `ScribingInk=e71c0d89b2e88370c56faf640e18a60886400b524069b1fb864907aa346254bb`; `PlayerKnowledge=d38d3f22777b8271a39675666e6d8259efaff4862d0a45686b952b57c7e151bb`; `TileResearchTable=0db86cebc9682c100bba0ab265d290d3e263ceb6f40fb3a88c80eec33cc96c8d`; `ContainerResearchTable=2e3abc95af99a52e0b495dafee7a4b64de099c681eb8bbeedcbecb820db1d4ee`; `GuiResearchTable=215c83dbb4fe50f05630885a6b5fcd5b7d40eefd6c89cd4ddde431dafed94203`. Это dirty-tree source fingerprints, не новые accepted artifact hashes.

## 10. Предлагаемые небольшие implementation units и meaningful cases

1. **Pure edit decision.** Immutable input note/actor discovered+R1/R2/pools/bonus/ink, injected float supplier; output updated note, exact deltas, ink plan, solved transition и optional orb pitch. Не внедрять world/inventory в pure тесты и не копировать solver второй раз.
2. **Shared state/transaction.** Native table revision/session binding, minimal bonus contract, preserving note identity/count/components, commit/dirty/sync. Проверки одной физической таблицы у двух игроков и внешнего slot transfer.
3. **Bounded codec+receiver.** PLACE/ERASE, sender/menu/session/revision admission, loader server-thread scheduling, accepted/refused correction. Codec tests не объявлять C2S dispatch.
4. **Client editing.** Palette, original geometry/tag sorting/page stride, drag preview и erase, known/unknown/fixed/holes/solved guards, актуальные pools/bonus и stale feedback. R09/R10 controls остаются явно pending.
5. **Real-loader/client acceptance.** Выполнить actual paths на Fabric и NeoForge, затем inspected screenshots/input в реальном клиенте и соединении. До выполнения результаты остаются unverified.

Обязательные сценарии:

| Группа | Проверяемый результат |
|---|---|
| Place funding | RP2+bonus1→RP1/bonus1; RP0+bonus1→RP0/bonus0; RP0+bonus0 refusal; неизвестный actor aspect refusal даже при положительном bonus |
| R2 placement RNG | Без R2 никаких float draws; `nextDown(0.1F)`→free+второй pitch draw; `0.1F`→точный обычный debit и один draw; ink1 во всех accepted branches |
| Erase/refund | Без исследований всё равно один draw/нет RP; R1 `nextDown(.25F)` успех, `.25F` отказ refund; R2 `nextDown(.5F)` успех, `.5F` отказ; bothResearcher максимум50%, не75% и не два rolls |
| Refund destination/discovery | Ранее bonus-funded/free placement → erase success даёт actorRP1, не bonus; A.place lux, B с неизвестным lux+R1.erase при f<.25 получает discovered lux/RP1, не открывает research/clue/warp; failed refund оставляет lux неизвестным. Точное следующее RNG состояние; выше scanning soft cap refund не обрезать по scan правилу |
| Ink boundaries |99→100 с сохранением tool/count;100 отказ без RNG; creative/Unbreakable/Unbreaking на table всё равно+1; отсутствие marker/tool refusal |
| Grid integrity | Fixed place/erase, hole, outside radius, blank erase, placed overwrite, solved/discovery, missing component: неизменны note/items/pools/bonus/knowledge/RNG |
| Solver integration | Synthetic валидный keyNITOR: fixed aer(0,0), fixed ignis(2,0), blank(1,0); placement lux соединяет их. Connected potentia branch у ignis остаётся, disconnected perditio branch и blank удаляются. Использовать real aspect catalog; это synthetic geometry, не утверждение о fixed tags original NITOR generator |
| Post-refund solve | Fixed aer(0,0)—placed lux(1,0)—fixed ignis(2,0), дополнительная placed lux(-1,0), actor lux unknown+R1: успешный erase(-1,0) refund делает lux discovered/RP1 **до solver** и решает оставшийся bridge. Failed refund оставляет candidate unsolved/lux unknown. Проверить ink1, exact draw trace, pruning и отсутствие research/warp награды |
| Discovery gate | Place известных tags не завершает путь через неизвестный actor fixed aspect; unknown endpoint остаётся unknown; solving не открывает aspect/research/warp автоматически |
| Presentation/materials | Solve normal→discovery, сохранить key/color/copies/count/customname/прочие components; следующий edit refusal, learn отдельным use расходует1 и начисляет research/warp один раз по существующей learning policy |
| Two actors | A.place расходует A, B.erase refund получает B; общий grid/revision синхронен. Два intent с одной revision дают один commit, второй authoritative stale отказ без RNG/cost |
| Replay/lifecycle | Закрытый menu, другой container/session, заменённый BE, смена note с тем же key, unload/reopen, distance64 boundary, disconnect/reconnect: старый action не применяется |
| Protocol abuse | Unknown enum/tag не erase; byte bounds/framing/truncation/trailing input; forged NBT/pool/price поля не существуют; wrong level/table не force-load; sender из настоящего connection |
| Actual GUI | Центр и границы +/-hex, palette sorted tags/RP0+bonus, left drag-release/type0, click/type2, cancel outside, fixed/holes/solved, inventory quick-move не перехвачен, page stride5, GUI scales и EN/RU |
| Persistence | Edited partial note/tool/bonus/revision contract переживает native save/reload; complete note pruned state остаётся complete; другие components/count не теряются |

Pure tests должны проверять exact ordered draw trace и полные before/after snapshots, а не только success boolean. Реальные GameTests вызывают production service/receiver с настоящими registries/BEs/menus; C2S claim требует actual dispatch. Положительный клиентский кадр не доказывает stale/replay refusal, поэтому оба слоя нужны отдельно.

## 11. Не закрытые этим аудитом границы

- Bonus ecology/recalc, combine и их отдельные resource/RNG branches остаются R09; сохранение/рассылка bonus container должно быть согласовано до его использования в R08. Здесь подтверждён только consumer path и различие RP-first/bonus-fallback.
- Уточнение16.10: original `getAspectPoolFor` делает `i2s`, а `addAspectPool(-1)` проверяет **raw int>0**; `TileResearchTable` игнорирует false debit return. Refund использует обычный int `iadd`, включая overflow; short narrowing относится к query/wire/persistence, а не live storage. Новый pure planner явно сохраняет эти ветки: raw32768 идёт в bonus, raw65537 списывается до65536, raw−65535 имеет положительный short query, выбирает actor branch, но raw debit остаётся no-op. Последний не является Researcher free-placement и всё равно соответствует source save/correction branch. MAX_VALUE refund переходит в MIN_VALUE. Нет нового scan cap100, clamp или refusal из-за величины. Глобальные native knowledge save/sync пока не изменены; полная lifecycle parity oversized pools всё ещё не заявлена. Native commit не должен записывать `PoolChange.queriedTotal()` вместо raw `after`.
- Source GUI действительно не ограничивает erase конкретной кнопкой и может отправлять packet при discovery; modern interaction UX/optimistic sound behaviour нужно подтвердить в actual client. Здесь не заявлено, что оригинальный unsafe server packet нужно сохранять.
- Режимы −1/0/1 и conditional content policy принадлежат R13. Сам edit не должен добавлять скрытое ограничение default0 или новые prerequisites; условие доступности записки задаётся issuance/mode resolver отдельно.
- R10 ещё должен принять полный контракт stacked/discovery count и copy. R08 preserving transition не должен случайно использовать setter clamp64 или выбросить компоненты; поддержка спорных некорректных normal/complete/presentation состояний явно требует решения.
- Полное визуальное/аудио соответствие grid/runes/hover/connectivity и оба loaders требует выполненного клиента. Настоящий C2S, decoder rejection, simultaneous viewers и reload после R08 пока не исполнялись в рамках этой работы.

До implementation review эти proposed native class names, session/revision wire schema и transaction boundaries остаются проектом. Original costs/probabilities/draw order и перечисленные source-level authority defects подтверждены прочитанным private кодом; это отдельное, более узкое утверждение, чем gameplay/runtime parity R08.
