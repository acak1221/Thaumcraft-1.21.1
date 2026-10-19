# TC4 4.2.3.5: покупка исследований и выдача записок

Дата: 15.10.2026. Read-only анализ original oracle и закреплённых references; production code и Gradle не менялись/не запускались. Ниже `O/` означает приватный `../work/thaumcraft-4.2.3.5-oracle/thaumcraft/`, `A/` — `../work/references/OpenTC4-Architectury/common/src/main/java/thaumcraft/`. Права на исходный код не меняются; исходные файлы в этот документ не копируются.

## Главный вывод для следующей unit

Минимальная верная следующая unit — **покупка Secondary research за точные aspect costs при исходном default difficulty=0**. Это не заменяет primary research notes/table. Проверки допуска и полного баланса нужно выполнять на сервере до первого изменения; исходный сервер доверял клиентской проверке стоимости, и воспроизводить этот authority defect нельзя. Новые отдельные `PacketPlayerCompleteResearchWithAspectC2S` / `PacketPlayerCreateResearchNoteC2S` принадлежат Architectury ref, а в оригинале обе операции реализует один `PacketPlayerCompleteToServer` с `type=0/1`.

## Правила исходной игры

| Правило | Точная семантика | Original evidence |
|---|---|---|
| Difficulty | Default **0**; -1 easy, 0 normal, 1 hard. `getInt(8)` — fallback argument существующего property, не default новой конфигурации: property создаётся со значением 0. | `O/common/config/Config.java:388–390`, static initialization `877` |
| Прямая покупка | Непустые tags и `(difficulty == -1 || difficulty == 0 && Secondary)`. Стоимость — исходные tag amounts, без разложения на primal aspects и без скидки/умножения difficulty. | `O/client/gui/GuiResearchBrowser.java:532–533`, `642–653` |
| Notes | Всё, что не попало в прямую покупку, проходит путь записки. При hard все исследования идут через notes; при normal — все primary. | `O/client/gui/GuiResearchBrowser.java:644–658` |
| Prerequisites | Все обычные `parents` **и** все `parentsHidden` должны быть завершены. Это AND, не выбор одного parent. `siblings` prerequisite не является. | `O/common/lib/research/ResearchManager.java:428–454`; GUI аналог `709–725` |
| Normal / Concealed | Обычная незавершённая запись может отображаться заблокированной; Concealed без выполненных prerequisites не рисуется. Наличие `@KEY` может показать Concealed раньше, но не отменяет prerequisites для действия. | `O/client/gui/GuiResearchBrowser.java:361–375`, `642`, `709–725` |
| Hidden / Lost | Для видимости незавершённой записи нужен `@KEY`; уже завершённый KEY также виден. Virtual не рисуется даже при completion. Категория ELDRITCH отдельно закрыта до `ELDRITCHMINOR`. | `O/client/gui/GuiResearchBrowser.java:361–371`, `673` |
| Clue | `@KEY` — отдельный marker, **не** завершение KEY, не оплата и не замена parent. После покупки оригинал не удаляет clue marker. | `O/common/lib/research/ResearchManager.java:88`, `111–115`, `497–527` |
| Проверка цены GUI | Перед отправкой type=0 клиент проверяет каждый pool на `pool >= cost`; unknown aspect в tooltip показывается неизвестным, кнопка становится недоступной. | `O/client/gui/GuiResearchBrowser.java:582–600`, `646–653` |
| Повторное действие | Уже завершённый KEY не покупается повторно, не списывает RP и не добавляет warp. | `O/common/lib/network/playerdata/PacketPlayerCompleteToServer.java:82`; `O/common/lib/research/ResearchManager.java:497–527` |

`Hidden` и `parentsHidden` — разные механизмы. Первый управляет раскрытием записи через clue, второй — дополнительными обязательными prerequisites, не рисуемыми обычными линиями. `Special`, `Round` и `Stub` сами по себе не задают скидку и не заменяют Secondary. AutoUnlock/Virtual следует оставлять своим исходным путям, а не создавать из них бесплатную покупку нулевой стоимости.

### Стоимость и исходный authority defect

`O/common/lib/network/playerdata/PacketPlayerCompleteToServer.java:76–106` берёт key, username, dimension и type из запроса. Он проверяет совпадение sender name, незавершённый key и prerequisites, но type=0 **не проверяет ни difficulty/Secondary, ни весь баланс, ни clue/visibility**. Каждый aspect списывается отдельно (`85–88`), затем исследование завершается (`90–91`). Unknown key до `doesPlayerHaveRequisites` может попасть в null lookup.

Это особенно опасно с исходным `PlayerKnowledge.addAspectPool`: при попытке снять больше имеющегося положительного pool оно сообщает success, а `AspectList.reduce` оставляет pool без изменения (`O/common/lib/research/PlayerKnowledge.java:103–121`, `O/api/aspects/AspectList.java:121–129`). Поэтому нельзя использовать boolean результата списания как проверку достаточности. Корректная native операция сначала валидирует **все** расходы и допуск, затем в одной server-thread транзакции списывает точные стоимости и завершает KEY. При любом отказе состояние знаний должно остаться побитово/структурно равным исходному snapshot.

**Необходимое задокументированное отклонение:** перенос клиентских admission/affordability проверок на сервер и отклонение некорректных запросов. Это исправление authority defect оригинала, а не новый игровой баланс. Не следует менять публичный `addAspectPool` quirk глобально ради одного action: проверка принадлежит покупке.

### Завершение, siblings и warp

1. Основной KEY добавляется только если отсутствует. При первой completion вызывается warp logic и планируется save: `O/common/lib/research/ResearchManager.java:497–527`.
2. При purchase type=0 после основного KEY выполняется **один проход только по его прямому списку siblings**, в исходном порядке. Для каждого sibling проверяются `not completed` и **его** prerequisites с уже обновлённым completed set; стоимость sibling не списывается. Рекурсивного обхода siblings здесь нет: `O/common/lib/network/playerdata/PacketPlayerCompleteToServer.java:92–97`.
3. Тот же порядок completion/siblings с проверкой prerequisites применяется при использовании законченной записки; записка расходуется только после успешной main completion: `O/common/items/ItemResearchNotes.java:87–104`.
4. Это отличается от normal Thaumonomicon use, у которого собственный sibling pass без такой проверки. **Нельзя вызывать `ResearchBrowserData.applyBookSiblingUnlocks` вместо purchase sibling logic.**
5. Warp начисляется по каждой впервые завершённой записи, включая sibling: `permanent += warp - floor(warp / 2)`, `sticky += floor(warp / 2)`, temporary не меняется. Для warp=1 permanent увеличивается на 1. При положительной прибавке counter обновляется до **всего текущего total warp**, а не только delta: `O/common/lib/research/ResearchManager.java:512–525`, `O/common/Thaumcraft.java:302–346`.
6. При `wuss=true` research warp не добавляется; исходный default **false**: `O/common/config/Config.java:419–421`, `840`. Пока native config не перенесён, фиксированный исходный default следует явно обозначать. UI warning cap 5 — только отображение: actual warp может быть 6 (`GuiResearchBrowser.java:544–550`).

В закреплённом каталоге 201 definition, 87 Secondary, из них 77 без unresolved config conditions; **ни одно Secondary не имеет siblings**. Единственные outgoing sibling lists: `DISTILESSENTIA -> JARLABEL`, `GOLEMSTRAW -> [COREGATHER, GOLEMBELL]` (`O/common/config/ConfigResearch.java:300`, `318`). Все эти sibling targets имеют prerequisite на main. Поэтому default secondary slice может проверить стоимость/warp на настоящих definitions, а обобщённый sibling executor — отдельно на фиксированном synthetic fixture либо при последующем переносе notes/easy mode. Нельзя приписывать существующим Secondary вымышленные sibling edges ради теста.

### Получение clue и notes — точные границы

`ResearchManager.createClue` (`O/common/lib/research/ResearchManager.java:84–117`) собирает Hidden/Lost записи с непустыми tags, у которых нет KEY и `@KEY`. Совпадение item trigger, entity trigger либо положительного награждённого aspect подходит; **prerequisites для выдачи clue не проверяются**. Из кандидатов world RNG выбирает ровно один, добавляется `@KEY`. Это объясняет возможность увидеть clue до выполнения parents. Первое slice покупки не должно само генерировать или подставлять clues.

Записка (`O/common/lib/research/ResearchManager.java:120–134`, `187–208`):

- В main inventory ищется уже существующая research note с тем же key. Если найдена, возвращается она без расхода материалов и без дубликата.
- Иначе нужен любой `IScribeTools` с damage ниже max damage и одна бумага. Поиск инструмента — первый подходящий main-inventory slot, не клиентский номер слота. При успехе расходуется одна бумага и вызывается vanilla `damageItem(1, player)` для ink tool; исходный inkwell имеет maxDamage 100 (`O/common/items/ItemInkwell.java:41–46`). Влияние creative/vanilla durability rules 1.7.10 на этот вызов в данном аудите не верифицировано; без отдельной проверки нельзя заменять его безусловным `damage += 1`. Переполненный inventory приводит к drop готовой записки, не к её потере.
- Создаётся незавершённая записка (`complete=false`, `copies=0`) с key/color и настоящим hexgrid. Grid radius = `1 + min(3, complexity)`, initial nodes соответствуют исходным tags: `O/common/lib/research/ResearchManager.java:268–333`. Само получение записки **не завершает исследование, не списывает research RP и не начисляет research warp**.
- Завершённый grid затем используется как item; наличие полных prerequisites проверяется снова. Незавершённый item не может заменить purchase action.

Нативные notes/table ещё отсутствуют. Создание декоративного item или прямое завершение primary по кнопке не является переносом этого пути. Первый default purchase slice должен честно оставлять primary доступным для чтения prerequisites с сообщением о pending notes/table.

## Конкретный native 1.21.1 дизайн

1. **Чистая общая политика** `ResearchUnlocks`/`ResearchPurchase`: default normal difficulty=0; lookup canonical case-sensitive KEY в `ResearchCatalog`; запрет неизвестного/пустого/`@` key, completed key, Virtual, unresolved conditional rows, неправильного action type; `ResearchBrowserData.isVisible` плюс `definition.hasPrerequisites`. Непустая `aspectCost` и Secondary обязательны для этого slice. Не угадывать config expressions, не разлагать costs на primal aspects.
2. **Preflight** проверяет весь cost map по текущему authoritative `PlayerKnowledge`. Выдать reason enum для client feedback: unknown/unavailable, already complete, prerequisites, clue needed, primary note required, insufficient aspects. Операция отказа не открывает/discovers aspects и не меняет pools/research/warp.
3. **Commit** на одном server thread без async-разрыва между preflight и debit. После успешного полного preflight снять точные amounts, add main KEY один раз, применить first-completion warp/defaults, затем прямые siblings с их prerequisites. Для дополнительной атомарности можно собрать next-state на `knowledge.copy()` и единожды `copyFrom` после всех проверок; нельзя сериализацией отфильтровать live snapshot.
4. **C2S payload** содержит только bounded canonical key. Не передавать username, dimension, amounts, balance или completed set. Sender — только `PacketContext.getPlayer()` / ServerPlayer; обработку поставить на `context.queue`, повторно проверить актуальные данные. Две одинаковые очередные покупки: вторая видит completion и ничего не снимает. Требование держать книгу в руке в самом original packet отсутствует; если выбран server-owned browser session или held-book gate, это нужно отдельно записать как native admission adaptation.
5. **S2C** существующий `KnowledgeEvents.sync` после commit передаёт новый snapshot самому sender; отдельный success/failure feedback может использовать исходные `tc.researcherror` и `thaumcraft:learn` (successful original action: volume 0.75, pitch 1.0, packet line102). Не изменять knowledge оптимистично на клиенте.
6. **UI** на доступном незавершённом Secondary показать точные costs и purchase action; при нехватке pools/parents/clue disable с понятной причиной. Нажатие отправляет key; чтение страниц открывается только после authoritative completion. Для обычного primary не показывать эквивалентную RP purchase при default 0.

### Почему Architectury packets — только пример адаптации

`A/common/lib/network/toserveraction/PacketPlayerCompleteResearchWithAspectC2S.java:39–60` использует sender ServerPlayer, валидирует lookup/type и **весь** balance до debit — это полезное направление исправления authority. Однако его новая `IAspectUnlockableResearch` и interface predicates не являются исходным `Secondary + Config.researchDifficulty`; `SimpleAspectUnlockedResearch.canPlayerCompleteResearchWithAspect` возвращает true (`A/api/research/implexample/SimpleAspectUnlockedResearch.java:25–36`). `PacketPlayerCreateResearchNoteC2S.java:40–67` валидирует client-supplied slot, а его InkWellItem создаёт note после расхода материалов (`A/common/items/research/InkWellItem.java:68–110`), без исходного reuse existing same-key note path. Копирование этих классов целиком изменит specification и не перенесёт original warp/sibling правила автоматически.

## Behavioral/runtime test oracle

| Сценарий | Отличимый ожидаемый результат |
|---|---|
| `ENCHFABRIC`, exact pools 3 pannus / 3 praecantatio | Успех; оба pool = 0, discovery сохраняется, completed ENCHFABRIC, warp прежний. Source costs/Secondary/no parents: `O/common/config/ConfigResearch.java:184`. |
| ENCHFABRIC, 3 pannus / **2** praecantatio | Отказ; весь knowledge snapshot неизменён. Это ловит частичное списание и underdraw quirk. |
| Повтор ENCHFABRIC после успеха | Отказ/already complete; pools и warp неизменны, повторная sync не создаёт эффектов. |
| `NITOR` с достаточными pools и CRUCIBLE | Default normal отказывает с primary-note-required; нельзя купить все исследования по tags. |
| `BANNERS`, pools достаточны, нет clue | Отказ и нулевое изменение; после добавления `@BANNERS` покупка проходит, marker остаётся. Source: `O/common/config/ConfigResearch.java:199`. |
| `FLUXSCRUB`, все visible parents, нет hidden `INFUSION` | Отказ; добавление hidden prerequisite разрешает действие при полной цене. Source: ConfigResearch `186`. |
| `COREBUTCHER`, parents COREGUARD/COREHARVEST и все costs по 3 | Успех, permanent +1, sticky/temp неизменны, counter = новый total; replay не добавляет warp. Source: ConfigResearch `338–339`. |
| Generic first-completion warp 3 при исходных perm=2,sticky=4,temp=5,counter=0 | После completion perm=4,sticky=5,temp=5,counter=14; при wuss=true все warp поля прежние. Не тестировать это покупкой primary при default 0. |
| Main со списком siblings [S1,S2] | Main завершён до проверки S1; S1 может удовлетворить prerequisite S2 при одном прямом проходе; отсутствие prerequisite пропускает соответствующий sibling; siblings собственных siblings не обходятся, RP на siblings не снимаются. Synthetic deterministic policy fixture не меняет настоящий corpus. |
| Некорректный key, @key, Virtual, conditional unavailable, primary key, чужой/повторный запрос | Нет crash и нет изменения knowledge; sender identity берётся из connection, клиентские цены отсутствуют в wire format. |
| Persistence | После успешной покупки и warp записать player NBT, создать второго player, загрузить и сравнить pools/discovery/completed/clue/warp/counter; проверить clone/respawn отдельно. |

Два обязательных real-loader GameTests: (1) negotiated player совершает успешную ENCHFABRIC purchase по тому же handler path, проверяются точное списание, completion и encoded knowledge S2C; replay не списывает повторно; (2) при цене 3/2 и при отсутствии prerequisites/clue request не изменяет полный snapshot и не публикует completion. Pure JUnit покрывает остальные комбинации и synthetic sibling order. Реальный клиент проверяет browser click, cost display, disabled reasons, результат sync и доступ к тексту после покупки; один только прямой вызов policy этих UI фактов не доказывает.

Существующий серверный snapshot/clone тест не доказывает C2S dispatch, а client startup/menu не доказывает кликабельную покупку. В этом read-only исследовании новые tests/implementation не создавались и фактические runtime passes не заявляются.
