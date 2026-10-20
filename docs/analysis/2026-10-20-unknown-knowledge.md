# R11 — неизвестные знания: original audit и план интеграции, 20.10.2026

**Статус: audited contract и staged implementation/test drafts в ignored `.tmp/r11-draft`; активных исходников R11 пока нет. Общая сборка R10 выполняется без частичного R11.**

Источники: TC4 4.2.3.5, CFR вне репозитория: `ItemResearchNotes.onItemRightClick`, `ResearchManager.findHiddenResearch/createNote`, `ResearchCategories`, `ResearchCategoryList`, `ConfigRecipes:431`, `ConfigResearch`; ранее — [research-notes-spec](2026-10-15-research-notes-spec.md), раздел5. Mapping имени часов дополнительно проверен по `methods.csv` MCP stable12-1.7.10 (скачанный ZIP только в ignored `.tmp`).

## Оригинальное поведение

- Оба metadata **24 и42** — Unknown Knowledge, обычное изображение notes с серой накладкой, rarity rare, max stack1. Их tooltip использует уже перенесённые `item.researchnotes.unknown.1/.2`. Натуральный recipe `KnowFrag` создаёт metadata42 из девяти `itemResource:9` (knowledge fragments) в полном квадрате3×3. В оригинальном TC4 callsite создания metadata24 при поиске по всему oracle не найден; сам use-branch24 явно существует и должен поддерживаться отдельно.
- На серверном right-click сначала действует обычная solved-note learning ветка. Если она не сработала, item metadata24/42 ищет скрытое исследование. Само раскрытие не выдаёт completion, clue `@KEY`, аспектные очки, warp и не требует бумаги/чернил.
- Кандидат: original research с `Hidden`, непустыми aspect tags, ещё не завершённый игроком, все normal и hidden parents завершены, и присутствует хотя бы один item/entity/aspect trigger array. Открытая игроком clue **не требуется**. Каталог содержит23 Hidden definitions с аспектами:22 имеют triggers, `BATHSALTS` не имеет и не может выпасть этим путём. Aspectless hidden/lost content не попадает в pool.
- Выбор использует **отдельный `java.util.Random`** с seed `world.func_72820_D() / 10L / 5L` и один `nextInt(candidates.size())`, если список непуст. Это **время суток**, нативный `Level.getDayTime()`. MCP: `func_72820_D = getWorldTime`, `func_82737_E = getTotalWorldTime`. Legacy1.12 код использует `getTotalWorldTime()/50` и здесь ошибочен. До отрицательного времени обе последовательные целочисленные операции совпадают с `/50`, но адаптация должна буквально сохранять исходные `/10/5` и отдельный RNG.
- Успех: тот же исходный stack переходит к normal-note presentation, `ResearchManager.createNote` задаёт ключ/primary color, `complete=false`, `copies=0`, обычную сетку. **Генерация сетки** берёт bounded RNG draws настоящего мира (отдельный RNG выбора исследования не подменяет генератор сетки). Count остаётся прежним, остальные исходные NBT сохраняются; звучит `thaumcraft:write`, volume0,75/pitch1.
- Flattening adaptation: modern `ItemStack` не меняет тип item на месте; success заменяет held stack через `transmuteCopy(normalNote, oldCount)` с сохранением component patch, затем заменяет только note data. Это сохранение одной физической выдачи, а не split/добавление второго предмета.
- Пустой список: расходуется ровно одна unknown note (включая creative), в мир выдаются **7 + world.rand.nextInt(3)** fragments и звук `erase`0,75/1. Original spawn не проверяет результат; порт должен использовать уже существующую долговечную очередь и не терять оплаченные fragments при отказе появления.

## Порядок кандидатов — часть RNG-контракта

Внешние категории хранятся в `LinkedHashMap`, порядок регистрации: `BASICS`, `THAUMATURGY`, `ALCHEMY`, `ARTIFICE`, `GOLEMANCY`, `ELDRITCH`. Внутри каждой категории — **обычный default `HashMap`**, заполняемый в порядке регистрации `ConfigResearch`. Поэтому сортировка по key меняет результат одного и того же seed.

Bundled JSON сортирует общий список research по key, но сохраняет `source.line`; `ResearchCatalog.loadBundled` сейчас этот provenance в `ResearchDefinition` не переносит. Узкая draft-адаптация: `ResearchHiddenSelection` отдельно читает retained provenance corpus, формирует immutable registration-order key list по `source.line` (без изменения конструктора/схемы `ResearchDefinition` и общего `ResearchCatalog`), затем отдельные default HashMap по категориям. До помещения в карты исключить `!definition.isEnabled()`, как original conditional registration. В частности `MIRROR` зависит от `allowMirrors`; нельзя заменять этот gate на `conditions().isEmpty()`.

Размеры полного extracted corpus по категориям: Basics19, Thaumaturgy42, Alchemy35, Artifice50, Golemancy39, Eldritch16. Размер/порядок вставок влияют на resize HashMap, поэтому нельзя строить HashMap только из22 hidden кандидатов: сначала весь enabled category, затем фильтрация original `values()`.

## Предлагаемые интеграционные точки

1. `ResearchHiddenSelection` — pure eligible-list/seeded selection; порядок регистраций из corpus; separate selection RNG, никаких world draws при просмотре списка. Draft JUnit проверяет exact vectors для нескольких dayTime50-block windows, отсутствия clue, parent gates, completed exclusion, Mirror conditions и пустого pool. Независимый scratch Java с default category HashMap дал no-parent order: `BANNERS,BONEBOW,TINYDART,TINYHAMMER,TINYBOWTIE,TINYHAT,TINYFEZ,TINYGLASSES,TINYVISOR,TINYARMOR`; dayTime0/49→BANNERS,50/99→TINYHAT,100→TINYVISOR,24000→BONEBOW. Это audit-векторы, пока не результат запущенного JUnit.
2. Два flattening item ids для original24/42 (оба наследуют `ItemResearchNotes`, чтобы принятые table filters и note-only count codecs продолжали работать). Metadata42 — recipe result,24 — поддержанный legacy variant без придуманного натурального источника.
3. Server use-service: same complete-note precedence; на успехе normal identity с сохранёнными count/дополнительными components и новым note data; на failure точный расход1, один world `nextInt(3)`, queued finite fragments. Не давать бонусных знаний за раскрытие.
4. Полный shaped recipe девяти fragments, alias `KnowFrag` в книге, неизвестный tooltip из исходного lang, модели/серый overlay и item-color registration для обоих новых item ids; bootstrap/общиеregistrylanghooks согласовать с ведущим.
5. Runtime: оба variants, success на доступный hidden key без clue/бумаги/чернил; deterministic selection при фиксированном dayTime; unsolved complete-data precedence; no-candidates failure7/8/9, creative consumption; реальная crafting-grid recipe; slot/save/packet и count/component preservation. Сетевой клиент и world restart отдельно pending, если не запускались.

R11 закрывает применение fragments/unknown notes. Полные натуральные источники fragments (мобы, торговля/loot и поздний контент) остаются привязаны к своим ещё не перенесённым units; recipe сам по себе не доказывает полный survival progression.

## Текущее доказательство

- Independent read-only review (`review_reservoir`,20.10.2026) сверил draft с oracle: blockers не найдено для reconstruction порядка категорий/HashMap, dayTime clock/RNG, complete-note precedence и creative fragment consumption. Это source review, не runtime PASS.
- `.tmp/r11-draft/manifest.json` сохраняет9 staged путей; `.tmp/r11-draft/INTEGRATION.md` — точные shared hooks. Ни один draft не включён в текущую R10 сборку; наследование требует вернуть non-final `ItemResearchNotes` лишь в следующем source window.
- Draft runtime test использует фактический `ServerPlayerGameMode.useItem` с real creative/survival modes, native shaped manager, persistent/wire codecs, world POST output и native UUID collision/retry; delay45 ticks. Его ещё не компилировали и не запускали.
