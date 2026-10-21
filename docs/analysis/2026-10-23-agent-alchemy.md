# 2026-10-23 — agent `alchemy`: essence and alchemy surface of the 1.7.10 oracle

**Ветка:** `agent/alchemy` (worktree `/home/cyberfox-kali/wt/alchemy`, база `be3c816f`).
**Домен:** `alchemy_and_essentia` + `essentia_transport` (11 открытых единиц ledger) плюс
аспектная поверхность `ThaumcraftCraftingManager` (таблица бонусов предметов, таблица
зачарований, таблица зелий) — это и есть «расчёт аспектов предмета», который решает
аспекты сканирования, растворения и цену каждой инфузии.

**Оракул:** `Thaumcraft-1.7.10-4.2.3.5.jar` (декомпиляция CFR в
`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`) и читаемый
`/home/cyberfox-kali/OpenTC4`. Все числа ниже прочитаны из **байт-кода самого jar-а**
(метод `getBonusTags`/`getObjectTags` класса
`thaumcraft/common/lib/crafting/ThaumcraftCraftingManager`) и подтверждены
декомпиляцией оракула.

**Gradle не запускался** (волна не имеет права запускать сборку). Все новые тесты
**авторские, но не выполненные**.

---

## 1. Итог

| Метрика | Значение |
|---|---|
| Дефектов найдено | **6** (5 исправлены, 1 — исправлен частично, см. §4) |
| Из них: бесшумная потеря поведения (висячий тег) | 1 |
| Из них: переставленные/неверные значения аспектов | 4 |
| Из них: потерянный звук | 1 |
| Висячих ссылок на теги найдено в порту | **1** (`thaumcraft:taint`) — устранена |
| Единиц ledger переведено `partial` → `parity_verified` | **2** |
| Единиц ledger дополнено доказательствами без смены вердикта | **10** (9 моих + `ThaumcraftCraftingManager`) |
| Новых/изменённых файлов production | 4 |
| Новых тестовых файлов | 3 (авторских, не выполненных) |

---

## 2. Дефекты: оракул → порт → фикс

### D1. Зачарования `thorns` и `smite` были переставлены местами

* **Оракул** `thaumcraft/common/lib/crafting/ThaumcraftCraftingManager.java:412-470`
  (таблица зачарований в `getBonusTags`): сравнение `eid` с константами
  `Enchantment` и `merge` соответствующего аспекта. Из байт-кода jar-а:

  | константа SRG | имя (mcp-stable 12-1.7.10) | аспект оригинала |
  |---|---|---|
  | `Enchantment.field_92091_k` | `thorns` | **WEAPON** |
  | `Enchantment.field_77339_k` | `smite` | **ENTROPY** |

* **Порт (до фикса)** `common/src/main/java/thaumcraft/common/lib/research/ObjectAspectResolver.java:471-473`:
  `addEnchantments(tags, Aspect.WEAPON, "power", "sharpness", "smite")` и
  `addEnchantments(tags, Aspect.ENTROPY, "thorns")` — то есть `smite` давал WEAPON,
  а `thorns` давал ENTROPY. Ровно наоборот.
* **Фикс**: `smite` → ENTROPY, `thorns` → WEAPON
  (`ObjectAspectResolver.java:471,473`).
* **Следствие**: у зачарованного на меч/щит предмета аспект `telum`/`perditio` был
  перепутан; это меняло аспекты скана, растворения и цену инфузии.

### D2. Бонус меча был масштабирован по материалу вместо константы оригинала

* **Оракул** `thaumcraft/common/lib/crafting/ThaumcraftCraftingManager.java:385-386`:
  `else if (item instanceof ItemSword && ((ItemSword)item).func_150931_i() + 1.0f > 0.0f) {
  tmp.merge(Aspect.WEAPON, (int)(((ItemSword)item).func_150931_i() + 1.0f)); }`
* **Значение `func_150931_i()`**: 1.7.10 `ItemSword` вызывает `super(ToolMaterial)`,
  то есть наследует `ItemTool`, а `ItemTool` добавляет урон материала в модификатор
  атаки. `func_150931_i()` — это **константные 4.0F** базового урона меча для любого
  меча. Косвенное подтверждение в 1.12-референсе этого репозитория:
  `src/main/java/thaumcraft/common/items/equipment/ItemThaumiumSword.java:17-25`
  переопределяет **и** `getAttackDamage()`, **и** `getAttributeModifiers()` — а это
  необходимо только если ванильное значение не содержит урон материала; и
  `src/main/java/thaumcraft/common/lib/crafting/ThaumcraftCraftingManager.java:92-94`
  использует то же выражение с переименованным `getAttackDamage()`.
  ⇒ **каждый** меч оригинала получает WEAPON **5**.
* **Порт (до фикса)** `ObjectAspectResolver.java:372`:
  `tags.merge(Aspect.WEAPON, (int)(sword.getTier().getAttackDamageBonus() + 1))` —
  1 (дерево) … 5 (незерит).
* **Фикс**: `ObjectAspectResolver.java:356,372` — константа
  `SWORD_WEAPON_BONUS = 5` для любого `SwordItem` (включая четыре меча TC4:
  `ItemThaumiumSword`, `ItemElementalSword`, `ItemVoidSword`, `ItemCrimsonSword`
  — все `extends SwordItem`).
* **Следствие**: деревянный/каменный/железный/золотой/алмазный меч получал
  `telum` 1/2/3/1/4 вместо 5 оригинала.

### D3. Мотык получал бонус `meto` вместо бонуса `instrumentum`

* **Оракул** `ThaumcraftCraftingManager.java:383-411`: порядок ветвей —
  `ItemArmor` → `ItemSword` → `ItemBow` → `ItemPickaxe` → **`ItemTool`** →
  `ItemShears || ItemHoe`; каждая ветвь уходит из цепочки (`else if`).
  1.7.10 `ItemHoe extends ItemTool`, поэтому **мотык ловится ветвью `ItemTool`**
  и до `ItemShears || ItemHoe` не доходит: оригинал даёт мотыку
  `instrumentum` = уровень сбора + 1 и **не** даёт `meto`.
  1.21.1 `HoeItem extends DiggerItem` — та же иерархия
  (`net/minecraft/world/item/HoeItem` → `DiggerItem` в 1.21.1-jar).
* **Порт (до фикса)** `ObjectAspectResolver.java:371-377`:
  `else if (item instanceof DiggerItem tool && !(item instanceof HoeItem) …)` +
  `else if (item instanceof ShearsItem || item instanceof HoeItem)` — мотык получал
  `meto` (1/2/3/4 по прочности) и **не** получал `instrumentum`.
* **Фикс** `ObjectAspectResolver.java:375-376`: мотык остаётся в ветви `DiggerItem`
  (→ `instrumentum` = уровень сбора + 1), ветвь ножниц больше не содержит мотык.
  Ножницы (1.7.10 `ItemShears extends Item`, 1.21.1 `ShearsItem extends Item`)
  сохраняют `meto` (238 прочности ⇒ 3, как в оригинале: 238 > 131 и > 32, но ≤ 250).

### D4. Таблица зелий: `confusion`, `weakness` и отсутствие `invisibility`

* **Оракул** `ThaumcraftCraftingManager.getObjectTags` (ветвь зелий;
  байт-код jar-а, строки 376-1162): `merge(WATER, 1)` для любого зелья,
  `merge(ENTROPY, 2)` для splash, `merge(MAGIC, (амплитуда+1)*2)` на каждое
  действие и `merge(<аспект действия>, (амплитуда+1)*3)`. Полная таблица
  оригинала (SRG → mcp-stable):

  | действие 1.7.10 | аспект оригинала | порт (было) |
  |---|---|---|
  | `blindness` | DARKNESS | DARKNESS ✔ |
  | `confusion` | **ELDRITCH** | DEATH ✘ |
  | `damageBoost` | WEAPON | WEAPON ✔ |
  | `digSlowdown` / `moveSlowdown` | TRAP | TRAP ✔ |
  | `digSpeed` | TOOL | TOOL ✔ |
  | `fireResistance` | ARMOR (+ FIRE ×2) | ✔ |
  | `harm` / `hunger` | DEATH | DEATH ✔ |
  | `heal` / `regeneration` | HEAL | HEAL ✔ |
  | `invisibility` | **SENSES** | — (строки не было вовсе) ✘ |
  | `jump` | FLIGHT | FLIGHT ✔ |
  | `moveSpeed` | MOTION | MOTION ✔ |
  | `nightVision` | SENSES | SENSES ✔ |
  | `poison` | POISON | POISON ✔ |
  | `resistance` | ARMOR | ARMOR ✔ |
  | `waterBreathing` | AIR | AIR ✔ |
  | `weakness` | **DEATH** | SENSES ✘ |

* **Фикс** `ObjectAspectResolver.java:406-427`: `confusion` → ELDRITCH,
  `weakness` → DEATH, добавлен `invisibility` → SENSES; splash-зелье без
  эффектов тоже получает ENTRPY 2 (оригинал проверяет только вид зелья).
  Эффект `wither` (в 1.7.10 его нет) оставлен на ELDRITCH — это поведение порта
  сохранено, чтобы не менять недоказанное.

### D5. Висячий тег `thaumcraft:taint` — правило «фокус Trade не меняет заражённые блоки» никогда не срабатывало

* **Оракул** `thaumcraft/common/items/wands/foci/ItemFocusTrade.java:122` и `:152`:
  `world.getBlock(x,y,z).getMaterial() != Config.taintMaterial` — фокус Trade
  отказывается менять блок с материалом заражения. Материал заражения получают
  `thaumcraft/common/blocks/BlockTaint.java:42` и `BlockTaintFibres.java:41`.
* **Порт** `common/src/main/java/thaumcraft/common/lib/world/TradeSwaps.java:101`
  реализует то же правило тегом блока
  `TagKey.create(Registries.BLOCK, …"thaumcraft","taint")` — **но файла тега не
  было**: `data/thaumcraft/tags/block/taint.json` отсутствовал. `BlockState.is(tag)`
  для несуществующего тега всегда `false`, поэтому заражённый блок можно было
  обменять через фокус Trade.
* **Фикс**: новый `common/src/main/resources/data/thaumcraft/tags/block/taint.json`
  со всей семьёй заражения оригинала: `thaumcraft:taint_crust`,
  `thaumcraft:taint_soil`, `thaumcraft:flesh_block` (оригинальный `blockTaint`
  metadata 2) и `thaumcraft:taint_fibres` (оригинальный `blockTaintFibres`).
* **Проверка**: скан всех ссылок на теги в порту (JSON-рецепты + `TagKey.create` в
  Java + вложенные `#`-ссылки в файлах тегов) — **0 неразрешённых** после фикса.

### D6. Звук банки оригинала (`thaumcraft:jar`) не воспроизводился

* **Оракул** `thaumcraft/common/blocks/BlockJar.java:88`:
  `this.func_149672_a(new CustomStepSound("jar", 1.0f, 1.0f));` и
  `thaumcraft/common/blocks/CustomStepSound.java` отвечает `thaumcraft:jar` и для
  звука шага, и для звука разрушения (базовый `SoundType` даёт то же имя шагу,
  установке, падению и удару).
* **Порт (до фикса)**: `common/src/main/java/thaumcraft/common/config/ThaumcraftContent.java:95-96`
  использовал `SoundType.GLASS`; собственный
  `common/src/main/java/thaumcraft/common/lib/CustomSoundType.java` (мёртвый код)
  искал звук по схеме `<имя>.<вид>` (`thaumcraft:jar.step`) вместо одиночного
  имени оригинала и потому не мог нести оригинальный звук.
* **Фикс**: `CustomSoundType` отвечает одиночному имени оригинала (реестр →
  `minecraft:block.<имя>` → собственный `SoundEvent`), а `ThaumcraftContent`
  заводит `JAR_SOUND = new CustomSoundType("jar", 1.0F, 1.0F)` и использует его
  для `thaumcraft:jar` и `thaumcraft:jar_void`.

---

## 3. Что ещё проверено и совпало (без правок)

* **Аспект-порядок**: 48 аспектов регистрируются в том же порядке, что и в
  оригинале (`thaumcraft/api/aspects/Aspect.java:142-200` оригинала ↔
  `common/src/main/java/thaumcraft/api/aspects/Aspect.java:15-62` порта) —
  порядок канонический и используется `Config.aspectOrder` оригинала
  (`Config.java:870`).
* **48 аспектов, их состав и цвета** — совпадают.
* **`AspectList`**: `add` суммирует, `merge` берёт максимум, `reduce` не удаляет
  ноль, `copy()` независим, `getAspectsSortedAmount()` — стабильная сортировка по
  убыванию с тем же разбиением на серии, что и оригинал
  (`AspectList.java:129-153`); `cullTags` (`ThaumcraftApiHelper.java:23-66`)
  взвешивает составной аспект тем же произведением 1.1/1.05.
* **Таблица объектов `ConfigAspects`**: 83 ванильных объектных тега и корпус
  `early_object_aspects.json` — порядок аспектов внутри записи (важно для
  отображения) сохраняется, потому что JSON-объект читается в порядке вставки.
* **Ёмкости эссенции**: резервуар 256 (всего, не на аспект), буфер 8, алембик 32
  — совпадают с оригиналом (`TileEssentiaReservoir.java:20`,
  `TileTubeBuffer.java:25`, `TileAlembic.java:25`).
* **Поиск источников эссенции** (`EssentiaHandler.java:84-134` оригинала ↔
  `common/src/main/java/thaumcraft/common/lib/essentia/EssentiaSourceSearch.java`):
  тот же полубокс, тот же порядок обхода, тот же пропуск самого тайла.
* **Подбор рецепта тигля**: `findMatchingCrucibleRecipe` оригинала
  (`ThaumcraftCraftingManager.java:110-133`) берёт первую запись с максимальным
  числом различных аспектов; порт (`common/crafting/CrucibleCrafting.java:21-46`)
  сортирует по `source_order` и пропускает равные (`<= highest`) — то же.

---

## 4. Что НЕ удалось исправить и почему

| # | Что | Где | Почему |
|---|---|---|---|
| N1 | Фокус Trade меняет заражённый блок | остальное — исправлено (D5) | исправлено |
| N2 | Зеркало эссенции играет `thaumcraft:jar` с громкостью 0.5 и тоном 2.0 | оригинал `thaumcraft/common/blocks/BlockMirror.java:68`; порт `common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java:214-218` (зеркало всё ещё `SoundType.GLASS`) | файл в домене другого агента (eldritch), правка в этом волне не делалась; фикс — одна строка `.sound(new CustomSoundType("jar", 0.5F, 2.0F))` |
| N3 | Сундук с лутом играет `thaumcraft:urnbreak` (1.0, 0.7) | оригинал `thaumcraft/common/config/ConfigBlocks.java:302`; порт `ThaumcraftEldritchBlocks.java:246-247` (`SoundType.STONE`) | тот же домен, что N2 |
| N4 | Слизь флюкса играет `thaumcraft:gore` (1.0, 1.0) | оригинал `thaumcraft/common/blocks/BlockFluxGoo.java:50`; порт `common/src/main/java/thaumcraft/common/config/ThaumcraftFlux.java:31` (звука нет) | тот же домен |
| N5 | Значение `func_150931_i()` 1.7.10 подтверждено цепочкой «`ItemSword` → `ItemTool`», а не самим исходником ваниллы 1.7.10 (его нет в этом окружении) | D2 | правка сделана по этой цепочке; при наличии исходника ваниллы её стоит перепроверить |
| N6 | Остатки девяти единиц моего списка | `client/**`, `common/blocks/**`, `common/tiles/**` | эти каталоги принадлежат другим агентам волны (`clientrender`, `devices`); правки там запрещены, поэтому в ledger записан точный блокер каждой единицы |
| N7 | Визуальная приёмка (звук, вид банки) | — | клиент в этой песочнице не запускается (нет GLX), проверка выполнена только по коду и по оракулу |

---

## 5. Единицы ledger

| Единица (canonical_path) | Было | Стало | Что сделано |
|---|---|---|---|
| `thaumcraft/common/blocks/BlockJar.java` | partial | **parity_verified** | D6 — оригинальный звук банки |
| `thaumcraft/common/blocks/JarStepSound.java` | partial | **parity_verified** | D6 — звук восстановлен, класс 1.7.10 в 1.21.1 не нужен |
| `thaumcraft/common/blocks/ItemJarNode.java` | partial | partial | блокер записан (нужен предмет-исследователь NODEJAR) |
| `thaumcraft/common/tiles/TileCrucible.java` | partial | partial | блокер записан (Forge fluid handler, FML-событие, Material.FIRE) |
| `thaumcraft/common/tiles/TileJarBrain.java` | partial | partial | блокер записан (радиус орбов, задержка 40 тиков, звук вздоха, выдача опыта) |
| `thaumcraft/common/tiles/TileJarNode.java` | partial | partial | блокер записан (мёртвый `onWandRightClick`, `IAspectContainer`) |
| `thaumcraft/client/renderers/tile/TileJarRenderer.java` | partial | partial | блокер записан (клиентский визуал) |
| `thaumcraft/client/renderers/tile/ItemJarFilledRenderer.java` | partial | partial | блокер записан (клиентский визуал) |
| `thaumcraft/client/renderers/tile/ItemJarNodeRenderer.java` | partial | partial | блокер записан; заодно отмечено, что «свет 11» раннего аудита не подтверждается оракулом |
| `thaumcraft/client/renderers/block/BlockEssentiaReservoirRenderer.java` | partial | partial | блокер записан (клиентский визуал) |
| `thaumcraft/client/renderers/item/ItemEssentiaReservoirRenderer.java` | partial | partial | блокер записан (клиентский визуал) |
| `thaumcraft/common/lib/crafting/ThaumcraftCraftingManager.java` | partial | partial | добавлены доказательства D1-D4; вердикт остаётся `partial` из-за исчезновения статических точек входа оригинала |

`python3 scripts/porting/update_scope_ledger.py --check --original-root
/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` → `stale_review_fingerprints: 0`.

---

## 6. Тесты (авторские, НЕ выполненные)

| Файл | Что закрепляет |
|---|---|
| `common/src/test/java/thaumcraft/crafting/AspectBonusParityTest.java` | 7 тестов: константный бонус меча (5) для шести мечей 1.21.1; `instrumentum` мотыка и отсутствие `meto`; `meto` ножниц (3) и отсутствие `instrumentum`; бонус лука/кирки/брони оригинала; таблица зачарований (`thorns` → WEAPON, `smite` → ENTROPY) и остальные; таблица зелий (`confusion` → ELDRITCH, `weakness` → DEATH, `invisibility` → SENSES, splash → ENTRPY 2) |
| `common/src/test/java/thaumcraft/taint/TaintBlockTagTest.java` | тег `thaumcraft:taint` существует и несёт ровно семью заражения оригинала |
| `common/src/test/java/thaumcraft/essentia/JarSoundTypeTest.java` | оригинальный звук `thaumcraft:jar` для всех пяти звуков типа, громкость 1.0 и тон 1.0 |

**Ни один из них не запускался**: волна не имеет права запускать Gradle
(общий кэш Loom). Компиляционная проверка тоже не выполнялась — изменения
сделаны вручную и вычитаны против 1.21.1 API (`SoundType` в 1.21.1 имеет
конструктор `(float, float, SoundEvent, SoundEvent, SoundEvent, SoundEvent,
SoundEvent)` и геттеры `getStepSound()/getBreakSound()/…`, `ItemEnchantments.Mutable`
имеет публичные `set(Holder,int)` и `toImmutable()`, `PotionContents` имеет
конструктор `(Holder, Optional, List)`).

---

## 7. Висящие ссылки на теги: полный список

Скан выполнен по всему порту: JSON-рецепты (`"tag": "ns:path"`), Java
(`TagKey.create(Registries.X, …)`) и вложенные `#ns:path` в файлах тегов.
Сравнение — с тегами ванильного 1.21.1 из
`minecraft-merged-1.21.1-…jar` (`data/minecraft/tags/{item,block}/`) и с
собственными тегами порта (все namespace, включая `c:`).

**Найдено и устранено (1):**

| namespace | путь | где используется | эффект |
|---|---|---|---|
| `thaumcraft` | `taint` (блок) | `common/src/main/java/thaumcraft/common/lib/world/TradeSwaps.java:101` | правило «фокус Trade не меняет заражённые блоки» не срабатывало никогда |

**Проверено и НЕ является висящим (ранее найденные волной `c:*` — в порту
присутствуют):** `c:dusts/glowstone`, `c:dyes/black`, `c:dyes/blue`,
`c:gems/emerald`, `c:ingots/thaumium`, `c:ingots/void`, `c:nuggets/copper`,
`c:nuggets/iron`, `c:nuggets/thaumium`, `c:nuggets/void`, `c:ores/gold`,
`c:ores/iron`, `c:raw_materials/copper`, `c:raw_materials/gold`,
`c:raw_materials/iron`, `c:rods/wooden` — все они объявлены в
`common/src/main/resources/data/c/tags/…` и разрешаются.

**Остаются необязательными (документированный пробел 1.21.1: нет олова, серебра и
свинца):** `c:ingots/gold`, `c:ingots/iron`, `c:ingots/lead`, `c:ingots/silver`,
`c:ingots/tin`, `c:nuggets/lead`, `c:nuggets/silver`, `c:nuggets/tin`,
`c:ores/lead`, `c:ores/silver`, `c:ores/tin` — они стоят в файлах тегов с
`"required": false`.

**Прочие проверенные пространства имён:** `minecraft:*` (все 270 ванильных тегов
`item`/`block` из jar-а 1.21.1), `thaumcraft:*` (152 собственных тега) и теги
биома `thaumcraft:magical_biomes`, `thaumcraft:magical_tree_biomes`,
`thaumcraft:magical_tree_blacklist`, `thaumcraft:cinderpearl_biomes`,
`thaumcraft:outer_lands_biomes`, `thaumcraft:brainy_zombie_spawns` (все шесть
существуют в `data/thaumcraft/tags/worldgen/biome/`).

---

## 8. Изменённые файлы

| Файл | Изменение |
|---|---|
| `common/src/main/java/thaumcraft/common/lib/research/ObjectAspectResolver.java` | D1-D4: бонус меча, бонус мотыка/ножниц, таблица зачарований, таблица зелий, ограничение 64 (`capAspects`) перед отсевом; новые публичные `bonusTags(ItemStack)` и `potionTags(ItemStack)` для тестов |
| `common/src/main/java/thaumcraft/common/lib/CustomSoundType.java` | D6: одиночное имя оригинала вместо `<имя>.<вид>`; рабочий резолв звука |
| `common/src/main/java/thaumcraft/common/config/ThaumcraftContent.java` | D6: `JAR_SOUND` и его использование для `thaumcraft:jar` и `thaumcraft:jar_void` |
| `common/src/main/resources/data/thaumcraft/tags/block/taint.json` | D5: новый тег блока заражения |
| `common/src/test/java/thaumcraft/crafting/AspectBonusParityTest.java` | новый (авторский, не выполнен) |
| `common/src/test/java/thaumcraft/taint/TaintBlockTagTest.java` | новый (авторский, не выполнен) |
| `common/src/test/java/thaumcraft/essentia/JarSoundTypeTest.java` | новый (авторский, не выполнен) |
| `docs/porting/scope-ledger.json` | 12 единиц: 2 переведены в `parity_verified`, 10 дополнены доказательствами и блокерами |
