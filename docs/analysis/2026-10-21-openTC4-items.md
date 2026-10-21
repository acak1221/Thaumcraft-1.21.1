# Сверка предметов оригинала TC4 4.2.3.5 с портом 1.21.1 (OpenTC4-референс)

**Домен:** предметы оригинала и их поведение — `thaumcraft/common/items/**` (кроме `wands/` и
`relics/`, которыми заняты другие агенты; `ItemResonator` из `relics/` входит в объём по
явному списку задания), регистрации предметов в `ThaumcraftContent` / `ThaumcraftLegacyItems`.
**Дата:** 2026-10-21. **Ветка:** `port/ocitems`. **Worktree:** `/home/Acak/tcw/ocitems`.

**Оракул (истина):** `/home/Acak/work/thaumcraft-4.2.3.5-oracle/` — декомпиляция
`Thaumcraft-1.7.10-4.2.3.5.jar` (sha1 `3f51b473f9ef7a94d1a9e01f6784af1cfd811649`).
**Читаемый референс имён ваниллы:** `/home/Acak/work/opentc4/src/main/java/thaumcraft/...`
(тот же 1.7.10-мод в читаемом виде) и SRG-резолвер
`python3 -B scripts/porting/resolve_srg.py <имя>` (mcp-stable 12-1.7.10).
**Переводы оригинала:** `assets/thaumcraft/lang/en_US.lang` и `ru_RU.lang`, распакованные из
того же JAR в `/tmp/tc4jar`.

---

## 1. Итог

| Метрика | Значение |
|---|---|
| Дефектов найдено и исправлено | **10** (7 — аспекты/ConfigAspects, 2 — поведение/ёмкость, 8 имён/переводов) |
| Из них: числовых расхождений оригинала | 7 |
| Из них: расхождений поведения | 2 |
| Из них: расхождений имён/переводов (en/ru) | 8 (в 8 строках, 3 предмета в двух языках) |
| Пробелов, найденных и НЕ исправленных (чужой домен / риск регрессии) | 5 |
| `:common:test` | **1666 тестов, 0 failures, 0 errors, 0 skipped** |

Ключевая находка группы: **метаданные предметов в порте «сглажены» в отдельные предметы, и
при этом каталог объектных аспектов (`ConfigAspects`) для шести предметов не был перенесён
вовсе, а для одного — перенесён лишний (только в 1.12-бэкпорте).** Это влияет на сканирование,
диссоциацию, алхимию и инфузию (база аспектов = рецептурная), а не только на подсказки.

---

## 2. Дефекты: оракул → порт → фикс

### D1. `thaumcraft:eldritch_eye` — потерян прямой объектный тег

* **Оракул** `thaumcraft/common/config/ConfigAspects.java:448`:
  `registerObjectTag(new ItemStack(ConfigItems.itemEldritchObject, 1, 0), new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.AURA, 3).add(Aspect.MAGIC, 3).add(Aspect.SENSES, 3).add(Aspect.SOUL, 3));`
* **Порт:** тега нет. `object_aspects.json` содержит строку оригинала
  `ConfigItems.itemEldritchObject` / metadata 0, но с пустым `modern_ids`, а в
  `ObjectAspectCatalog` (строка 84-92) сглаживание метаданных выполняется только для
  `itemResource`, `itemShard` и `blockCustomOre` — `itemEldritchObject` в списке нет.
  Итог: `ObjectAspectCatalog.get("thaumcraft:eldritch_eye")` → пустой список, предмет «неизвестен»
  при сканировании и растворяется без аспектов.
* **Фикс:** `common/src/main/resources/data/thaumcraft/tc4/early_object_aspects.json` — новая
  строка `original_source_line: 448`, `direct`, `ConfigItems.itemEldritchObject`, metadata `0`,
  `modern_ids: ["thaumcraft:eldritch_eye"]`, аспекты `alienis5/auram3/praecantatio3/sensus3/spiritus3`.

### D2. `thaumcraft:eldritch_tablet` — потерян прямой объектный тег

* **Оракул** `ConfigAspects.java:450`: `TRAP 4 + MIND 4 + MECHANISM 4` для metadata 2.
* **Порт:** тега нет (та же причина, что в D1).
* **Фикс:** новая строка `original_source_line: 450` в `early_object_aspects.json`
  (`vinculum4/cognitio4/machina4`, `thaumcraft:eldritch_tablet`).

### D3. Три мешка сокровищ — потеряны прямые объектные теги

* **Оракул** `ConfigAspects.java:407-409`: `itemLootbag` metadata 0/1/2 → `GREED 8 / 16 / 32`.
* **Порт:** `ThaumcraftLoot` регистрирует `loot_bag_common/uncommon/rare`, но строки корпуса
  имеют пустые `modern_ids`, а `itemLootbag` не входит в сглаживание → тегов нет.
* **Фикс:** три новые строки `original_source_line: 407/408/409` в `early_object_aspects.json`
  (`thaumcraft:loot_bag_common|uncommon|rare`, `lucrum8/16/32`).

### D4. Четыре съедобных самородка — потеряны прямые объектные теги

* **Оракул** `ConfigAspects.java:410-413`: `itemNuggetBeef/Chicken/Pork/Fish` с wildcard-метаданными
  → `HUNGER 1`.
* **Порт:** `ThaumcraftLegacyItems.EDIBLE_NUGGETS` регистрирует `nugget_chicken/beef/pork/fish`,
  тегов нет.
* **Фикс:** четыре новые строки `original_source_line: 410..413` (`fames1`, `wildcard:32767`).

### D5. Тройное мясо — потерян сложный (complex) тег

* **Оракул** `ConfigAspects.java:414`:
  `registerComplexObjectTag(new ItemStack(ConfigItems.itemTripleMeatTreat, 1, Short.MAX_VALUE), new AspectList().add(Aspect.HEAL, 1).remove(Aspect.HUNGER, 1));`
  По оригинальной семантике `registerComplexObjectTag` (`ThaumcraftApi.java:259-274`) переданный
  список **добавляется** к рецептурной базе; `.remove(HUNGER,1)` действует на сам переданный
  список, где HUNGER отсутствует, поэтому итог = база рецепта + `HEAL 1`.
* **Порт:** сложных привязок к этому предмету нет (`registerComplexObjectTag`-строки корпуса
  отбрасываются загрузчиком, `modern_ids` пусты).
* **Фикс:** новая строка `original_source_line: 414`, `complex`, `wildcard:32767`,
  `modern_ids: ["thaumcraft:triple_meat_treat"]`, `sano: 1`.

### D6. Стрела первозданности — потерян сложный тег только у «воздушной»

* **Оракул** `ConfigAspects.java:432`:
  `registerComplexObjectTag(new ItemStack(ConfigItems.itemPrimalArrow), new AspectList().add(Aspect.WEAPON, 1));`
  `new ItemStack(item)` = метаданные **0**, то есть привязка относится только к «воздушной» стреле;
  остальные пять получают рецептурную базу без `WEAPON`.
* **Порт:** привязки нет ни у одной из шести сглаженных стрел.
* **Фикс:** новая строка `original_source_line: 432`, `complex`, metadata `0`,
  `modern_ids: ["thaumcraft:primal_arrow_air"]`, `telum: 1`. Остальные пять стрел намеренно
  остаются без сложной привязки (см. тест `originalItemObjectTagsArePresentAnd…`).

### D7. `thaumcraft:balance_shard` — лишний объектный тег (только из 1.12-бэкпорта)

* **Оракул (1.7.10)** `ConfigAspects.java:415-420` регистрирует **только шесть простых осколков**
  (metadata 0…5). Осколка равновесия (metadata 6) в оригинале **нет**; это подтверждает и
  читаемый OpenTC4 1.7.10: `src/main/java/thaumcraft/common/config/ConfigAspects.java:410-415`
  (строки заканчиваются на shard 5).
* **Порт:** корпус `object_aspects.json` (сгенерирован с 1.12-бэкпорта
  `src/main/java/thaumcraft/common/config/ConfigAspects.java:340-341`) содержит строку
  `ConfigItems.itemShard` / metadata 6 со списком
  `{aer2 ignis2 aqua2 terra2 ordo2 perditio2 vitreus1}`, и сглаживание
  `ObjectAspectCatalog:81-92` (список `SHARD_IDS`) автоматически навешивает этот 1.12-только тег
  на `thaumcraft:balance_shard`.
* **Последствие:** база осколка равновесия в порте — 1.12-литерал, а не рецептурная база
  оригинала. Оригинал (`ThaumcraftCraftingManager:513-540`, `generateTags`) выводит её из
  тигельного рецепта: база воздушного осколка `{MAGIC 1, AIR 2, CRYSTAL 1}` + по
  `(int)(sqrt(2)/1) = 1` остальных пяти простых →
  `{MAGIC1, AER2, CRYSTAL1, IGNIS1, AQUA1, TERRA1, ORDO1, PERDITIO1}`. Это же выражение
  оригинала используется для `salis_mundus` (`ConfigAspects:421`), поэтому в порте
  `salis_mundus` тоже получал 1.12-значение вместо 1.7.10-значения.
* **Фикс:** `ObjectAspectCatalog` — новое поле
  `LEGACY_ONLY_FALLBACK_IDS = Set.of("thaumcraft:balance_shard")` и проверка в ветке сглаживания:
  1.12-только строка не превращается в прямой тег. Теперь осколок равновесия, как в оригинале,
  выводится из рецепта (`Resolution.fromCrucible` в `ObjectAspectResolver` уже реализует
  именно эту формулу: база катализатора + `(int)(sqrt(amount)/count)`), а `salis_mundus`
  вычисляется как `base(balance_shard) + MAGIC 2 − CRYSTAL` — как в `ConfigAspects:421`.
* **Проверка:** `ObjectAspectCatalogTest.originalItemObjectTagsArePresentAndTheLegacyOnlyBalancedShardStaysRecipeDerived`
  фиксирует, что у `thaumcraft:balance_shard` нет прямого тега и его нет в `registeredIds()`.

### D8. Кольцо ученика (metadata 3) — потерян прямой объектный тег

* **Оракул** `ConfigAspects.java:435`:
  `registerObjectTag(new ItemStack(ConfigItems.itemBaubleBlanks, 1, 3), new AspectList().add(Aspect.MAGIC, 5));`
  Metadata 3 — первое простое аспектное кольцо скидки (`ItemBaubleBlanks.func_77653_i:121-127`
  сопоставляет metadata 3 первым простым аспектом, то есть воздухом).
* **Порт:** `ThaumcraftBaubles.DISCOUNT_RINGS` регистрирует шесть отдельных предметов
  `discount_ring_<аспект>`; строки корпуса по `itemBaubleBlanks` имеют пустые `modern_ids`,
  а `itemBaubleBlanks` не входит в сглаживание → тегов нет.
* **Фикс:** новая строка `original_source_line: 435`, `direct`, `ConfigItems.itemBaubleBlanks`,
  metadata `3`, `modern_ids: ["thaumcraft:discount_ring_aer"]`, `praecantatio: 5`. Остальные пять
  колец скидки в оригинале тега не имеют (рецепта у них тоже нет), поэтому они остаются
  рецептурно-пустыми.

### D9. Рунная скрижаль складывалась в стек по 64

* **Оракул** `thaumcraft/common/items/ItemEldritchObject.java:56`: `this.func_77625_d(1);`
  — предел стека **один для всех** метаданных исходного предмета (глаз, обряды, скрижаль,
  жемчужина, установщик обелисков).
* **Порт:** `ItemEldritchObject` задавал `stacksTo(type == ELDRITCH_OBELISK_PLACER ? 1 : 64)`,
  то есть скрижаль складывалась в 64. (Глаз — `ThaumcraftWarp.ELDRITCH_EYE`, обряды —
  `ItemCrimsonRites`, жемчужина — `ItemPrimordialPearl` — имели `stacksTo(1)` и были верны.)
* **Фикс:** `common/src/main/java/thaumcraft/common/items/ItemEldritchObject.java` —
  `properties.stacksTo(1)` для всех вариантов, с комментарием со ссылкой на оракул.

### D10. Ключ не «блестел» после привязки

* **Оракул** `thaumcraft/common/items/ItemKey.java:90-92`:
  `public boolean func_77636_d(ItemStack s) { return s.func_77942_o(); }`
  (`hasEffect` → наличие NBT, то есть привязанный ключ блестит). Тот же приём есть у
  `ItemResonator:77-79` (в порте уже реализован как `isFoil`).
* **Порт:** `ItemKey` не переопределял `isFoil` → привязанный ключ не выделялся.
* **Фикс:** `common/src/main/java/thaumcraft/common/items/ItemKey.java` — переопределение
  `isFoil(stack)` через наличие компонента `KEY_LINK` (нативный эквивалент NBT-присутствия).

### D11–D18. Имена и переводы (en/ru)

Сверка `en_US.lang`/`ru_RU.lang` оригинала с `en_us.json`/`ru_ru.json` порта по всем предметам
области (ключи оригинала → ключи порта):

| Ключ порта | Оригинал (en/ru) | Было в порту | Стало |
|---|---|---|---|
| `item.thaumcraft.resonator` | `Essentia Resonator` / `Резонатор` | `Resonator` | `Essentia Resonator` |
| `item.thaumcraft.inkwell` | `Scribing Tools` | `Inkwell` | `Scribing Tools` |
| `item.thaumcraft.bath_salts` | `Purifying Bath Salts` | `Bath Salts` | `Purifying Bath Salts` |
| `item.thaumcraft.sanity_soap` | `Sanitizing Soap` | `Sanity Soap` | `Sanitizing Soap` |
| `item.thaumcraft.bucket_pure` | `Bucket of Purifying Fluid` | `Bucket of Purity` | `Bucket of Purifying Fluid` |
| `item.thaumcraft.boots_traveller` | `Boots of the Traveler` / `Сапоги путешественника` | `Boots of the Traveller` / `Ботинки путешественника` | как в оригинале |
| `item.thaumcraft.eldritch_eye` (ru) | `Глаз Древних` | `Древний глаз` | `Глаз Древних` |
| `item.thaumcraft.primordial_pearl` (ru) | `Сингулярная жемчужина` | `Изначальная жемчужина` | `Сингулярная жемчужина` |

Остальные проверенные предметы области совпадают с оригиналом в обоих языках
(таум-инструменты и броня, пустотные инструменты и броня, крепостная броня, мантии тауматурга
и пустотного тауматурга, гогглы, очки откровения, зелья, флаконы, кристаллизованная и эфирная
эссенция, самоцветы/кластеры, стрелы первозданности, съедобные самородки, мешки сокровищ,
исследовательские записки и открытие, ключи, кровавый клинок, костяной лук, сингулярный крушитель,
мана-боб, банка смерти/очищения, зелье санитарности, скрижаль, обряды, жемчужина, обелиск-установщик).

---

## 3. Что сверено и сошлось (без правок)

* **Метаданные и ёмкости.** `ItemNugget:30-35` (стек 64, подтипы, `setMaxDamage(0)`) →
  `ItemNugget` порта (`stacksTo(64)`, `type()`); `ItemEssence:52-57` (64) → `phial_empty`/`phial_filled`;
  `ItemResearchNotes:63-67` (стек 1, `rare`/`epic` по метаданным) → `research_note`/`discovery`;
  `ItemResonator:52-57` (стек 1, `uncommon`) → `resonator`; `ItemBottleTaint:19-24` (стек **8**,
  `setHasSubtypes(false)`) → `bottle_taint`; `ItemBucketDeath:39-43` (стек 1) → `bucket_death`;
  `ItemPrimalArrow:29-34` (64) → шесть сглаженных стрел; `ItemKey:62-67` (64, `uncommon`) →
  `key_iron`/`key_gold`; `ItemCompassStone:42-44` (стек 1) → `compass_stone`;
  `ItemLootBag:43-45` (стек **16**) → три мешка; `ItemCrystalEssence:47-49` / `ItemWispEssence:41-43`
  (64) → `crystal_essence`/`wisp_essence`; `ItemInkwell:44-46` (`setMaxDurability(100)`) →
  `inkwell`; `ItemNuggetEdible:28-37` (1 голода, 0.3 насыщения, 10 тиков) → четыре самородка.
* **Поведение правой кнопки.** `ItemBottleTaint:36-47` — трата только вне творческого, звук
  `random.bow` (0.5 / `0.4/(rand*0.4+0.8)`), серверный спавн `EntityBottleTaint` → `ItemBottleTaint`
  порта воспроизводит всё; `ItemBucketDeath:55-105` — луч по игроку, смещение блока по
  `sideHit` (0↓1↑2−z3+z4−x5+x), проверка `canPlayerEdit`, возврат обычного ведра → `ItemBucketDeath`
  порта (`relative(hit.getDirection())` даёт ту же таблицу); `ItemEssence:123-193` — смена
  «пустой/полной» ампулы с банкой/алембиком, ровно **8** эссенции за раз, звук
  `game.neutral.swim` (0.25) → `ItemEssence.interact`; `ItemResearchNotes:87-120` — изучение
  исследования, выдача соседних исследований, трата одной записки даже в творческом, сообщение
  `tc.researcheserror` → `ItemResearchNotes.use` + `ResearchNoteLearning`;
  `ItemUnknownResearchNotes`/`UnknownKnowledge` — ветка «неизвестные знания» (metadata 24/42):
  7–9 осколков знания или превращение в записку, звуки `thaumcraft:erase`/`thaumcraft:write`.
* **Оружие и броня.** `ThaumcraftApi:48-55` — `toolMatThaumium` (3/400/7.0/2.0/22),
  `toolMatVoid` (4/150/8.0/3.0/10), `toolMatElemental` (3/1500/10.0/3.0/18),
  `armorMatThaumium` (25, 2/6/5/2, 25), `armorMatSpecial` (25, 1/3/2/1, 25),
  `armorMatThaumiumFortress` (40, 3/7/6/3, 25), `armorMatVoid` (10, 3/7/6/3, 10) — все перенесены
  в `ThaumcraftEquipment` / `ThaumcraftWearables` без расхождений; прочность брони использует
  нативные множители 11/16/15/13 (`ArmorItem.Type.getDurability`), что совпадает с 1.7.10
  (25→400/375/325 для SPECIAL, 350 для гогглов — `ItemGoggles:55`).
* **Рецептурная база.** `ConfigRecipes:105-114` (тигельные рецепты осколка равновесия) и
  `:190/201/205/209/212/213/216/219/221/247-251/265-270/282/293/330-355` (арканные и инфузионные
  с осколками) — аспекты и раскладки совпадают с портовыми `data/thaumcraft/recipe/**`.

---

## 4. Пробелы (найдены, не исправлены — чужой домен или риск регрессии)

1. **Ведро смерти: оригинальная «клетка метаданных 3» против нативной исходной клетки.**
   Оракул `ItemBucketDeath.java:103`: `world.func_147465_d(x, y, z, ConfigBlocks.blockFluidDeath, 3, 3)` —
   метаданные 3, тогда как чистая жидкость кладётся метаданными 0 (`ItemBucketPure.java:103`),
   и `EventHandlerWorld.java:216` проверяет именно метаданные 3 для возврата ведра смерти.
   Порт кладёт `ThaumcraftLegacyTiles.DEATH_FLUID.get().defaultBlockState()` (уровень 0 = 4 кванта,
   урон 5) вместо клетки уровня 3 (1 квант, урон 2). Модель конечной жидкости принадлежит
   юниту «legacy tiles» (`BlockFluidDeath`, `LegacyTileRules`), и её уровень/кванты в порте
   описаны иначе; правка только ведра без пересмотра модели жидкости дала бы клетку, которая
   высыхает на следующем тике. Не трогал.
2. **Наполнение ведра смертельной жидкостью отсутствует.** Оракул `EventHandlerWorld:212-223`
   (`FillBucketEvent`) возвращает `itemBucketDeath` при клике пустым ведром по клетке жидкости
   смерти с метаданными 3. В порту `DeathFluid.getBucket()` возвращает `Items.AIR`
   (`BlockFluidDeath.java:198`), и ни один обработчик не выдаёт `thaumcraft:bucket_death` —
   ведро смерти нельзя наполнить заново. Тот же домен, что и п.1.
3. **Зеркало рук не блестит при наличии NBT.** Оракул `ItemHandMirror.java:96-98`
   (`func_77636_d` → `hasTagCompound`), в порте `isFoil` не переопределён. Файл лежит в
   `items/relics/`, который по условию задания занят другим агентом — не трогал.
4. **Культистская броня без объектных тегов.** Оракул `ConfigAspects.java:437-446` регистрирует
   десять предметов культистской брони (металл 3/4/5 + элдритч 1/2). В порту строки корпуса
   имеют пустые `modern_ids`, а `itemHelmetCultistPlate` и подобные не входят в сглашивание →
   тегов нет. Броня культистов — собственность юнита «cultists», теги для неё в том же
   корпусе не сглажены; исправление затрагивает общий каталог, поэтому вынесено в отчёт.
5. **Осколок равновесия: рецептурная база проверена только чтением кода.** D7 меняет *источник*
   базы осколка равновесия (с 1.12-литерала на рецептурную), поэтому ожидаемые значения
   осколка `{MAGIC1, AER2, CRYSTAL1, IGNIS1, AQUA1, TERRA1, ORDO1, PERDITIO1}` и
   `salis_mundus` `{MAGIC3, AER2, IGNIS1, AQUA1, TERRA1, ORDO1, PERDITIO1}` выведены сверкой
   с `ThaumcraftCraftingManager:215-248/489-540` и `ConfigAspects:421`, но **живой прогон
   (gameTest на сервере) в этой сессии не выполнялся** — в `:common:test` нет серверного
   контекста рецептов. Требуется прогон `:common:runGameTest`/серверного smoke.

---

## 5. Вне области задания (объяснено, чтобы не искать)

* `ItemBottleEarth`, `ItemTome`, `ItemSunkenAmulet` в TC4 4.2.3.5 **не существуют**: в оракуле
  (`thaumcraft/common/items/**`) нет ни одного из этих классов, в `ConfigItems` нет
  соответствующих регистраций, в `en_US.lang` — нет ключей. Это предметы других модов/версий.
  `ItemPortableHole` существует только как фокус (`items/wands/foci/ItemFocusPortableHole.java`)
  и принадлежит юниту палочек.
* `ItemNugget` metadata 2/3/4 (олово/серебро/свинец) и кластеры 18/19/20 — олово, серебро и
  свинец в 1.21.1 отсутствуют, поэтому метаданные не сглажены (законно, ср. политика
  `early_object_aspects.json`: «Conditional mod-metal branches … the tin, silver and lead
  branches stay unmapped»). Железо заменено ванильным `minecraft:iron_nugget`, медь —
  `thaumcraft:copper_nugget` и `thaumcraft:cluster_copper`.

---

## 6. Изменённые файлы

```
common/src/main/java/thaumcraft/common/items/ItemEldritchObject.java   (D9: стек 1)
common/src/main/java/thaumcraft/common/items/ItemKey.java                (D10: isFoil)
common/src/main/java/thaumcraft/common/lib/research/ObjectAspectCatalog.java  (D7: 1.12-только тег)
common/src/main/resources/data/thaumcraft/tc4/early_object_aspects.json (D1-D8: 12 новых строк)
common/src/main/resources/assets/thaumcraft/lang/en_us.json              (D11-D18: 6 имён)
common/src/main/resources/assets/thaumcraft/lang/ru_ru.json              (D11-D18: 3 имени)
common/src/test/java/thaumcraft/aspects/EarlyObjectAspectBindingsTest.java  (счётчики, ветка новых строк)
common/src/test/java/thaumcraft/aspects/ObjectAspectCatalogTest.java         (новый тест D1-D7)
docs/analysis/2026-10-21-openTC4-items.md                                (этот отчёт)
```

## 7. Проверка (реальный вывод)

```
$ export JAVA_HOME=/home/Acak/.jdk21; export PATH=$JAVA_HOME/bin:$PATH
$ export GRADLE_USER_HOME=/home/Acak/gradle-homes/ocitems
$ ./gradlew --no-daemon --console=plain :common:test
...
> Task :common:compileJava
> Task :common:processResources
> Task :common:classes
> Task :common:compileTestJava
> Task :common:processTestResources
> Task :common:testClasses
> Task :common:test

BUILD SUCCESSFUL in 39s
5 actionable tasks: 2 executed, 3 up-to-date
```

Разбор `common/build/test-results/test/*.xml`:

```
tests 1666 skipped 0 failures 0 errors 0
```

Новые/изменённые тесты:
`thaumcraft.aspects.ObjectAspectCatalogTest.originalItemObjectTagsArePresentAndTheLegacyOnlyBalancedShardStaysRecipeDerived`,
`thaumcraft.aspects.EarlyObjectAspectBindingsTest.pistonTrapdoorAndWheatAdditionsLeaveEveryPreviousBindingUnchanged`
(12 новых строк проверяются по отдельности, исторический ALL90-дайджест не изменён).

**Не выполнялось в этой сессии:** клиентский прогон, серверный smoke, `:common:runGameTest`.
Клиентскую приёмку не заявляю.
