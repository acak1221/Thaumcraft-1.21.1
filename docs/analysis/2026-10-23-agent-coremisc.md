# Аудит публичного API TC4 4.2.3.5 → 1.21.1 — агент `coremisc`

Дата: 2026-10-23. Ветка: `agent/coremisc`. Worktree: `/home/cyberfox-kali/wt/coremisc`.
Домен: `public_api_and_compatibility`, `device_and_menu_routing_review`,
`aspect_contracts_and_data`, `infusion`, `foci_equipment_and_enchantments`,
`creatures_and_projectiles`.

## 0. Как читался оригинал

Приватное дерево оракула (`thaumcraft-4.2.3.5-oracle`) в этой песочнице **недоступно**: скрипт
`scripts/porting/update_scope_ledger.py` ищет его по умолчанию в `<repo>/../work/thaumcraft-4.2.3.5-oracle`
(`/home/cyberfox-kali/wt/work/...`), и этого каталога нет. Поэтому оригинал читался **из самого
оригинального JAR** `Thaumcraft-1.7.10-4.2.3.5.jar`: байт-код каждого класса API разобран локальным
читателем класс-файлов (`.tmp/classdump.py`, только чтение), а сверка с читаемым исходником
`/home/cyberfox-kali/OpenTC4` использовалась как второе мнение. Все ссылки «oracle …» в этом отчёте и в
ledger указывают на **байт-код оригинального JAR**, а не на недоступный oracle-декомпилят.

Следствие: `python3 scripts/porting/update_scope_ledger.py --check` **не может быть выполнен** в этой
песочнице (нет source root). Вместо него выполнена локальная эмуляция валидатора
(`.tmp/check_scope_ledger_local.py`): она пересчитывает легаси-отпечатки из репозитория, сверяет
`reviewed_sha256` с записанными в ledger хешами и повторяет `validate_review` из
`scripts/porting/update_scope_ledger.py`. Результат: `problem_count: 0`,
`stale_review_fingerprints: 0` для 1044 единиц. Сторона `original` не пересчитывается, но и не менялась:
ни один файл `src/main/java` (легаси-дерево) и ни один файл оракула этой волной не тронут.

## 1. Единицы ledger

| единица | было | стало | disposition |
|---|---|---|---|
| `thaumcraft/api/ThaumcraftApi.java` | partial | **parity_verified** | adapt |
| `thaumcraft/api/aspects/AspectList.java` | partial | **parity_verified** | adapt |
| `thaumcraft/api/damagesource/DamageSourceThaumcraft.java` | partial | **parity_verified** | adapt |
| `thaumcraft/api/BlockCoordinates.java` | partial | **parity_verified** | exclude → **port** |
| `thaumcraft/common/container/ContainerGhostSlots.java` | partial | **parity_verified** | adapt |
| `thaumcraft/common/container/SlotLimitedByItemstack.java` | not_assessed | **parity_verified** | exclude |
| `thaumcraft/common/entities/InventoryMob.java` | partial | **parity_verified** | adapt |
| `thaumcraft/common/tiles/TileBanner.java` | partial | **parity_verified** | adapt |
| `thaumcraft/common/tiles/TileManaPod.java` | partial | **parity_verified** | adapt |
| `thaumcraft/common/tiles/TileNitor.java` | partial | **parity_verified** | adapt |
| `thaumcraft/common/tiles/TileThaumcraftInventory.java` | partial | **parity_verified** | replace |
| `thaumcraft/common/config/recipes/ConfigRecipesInfusionDeviceSlice.java` | partial | **parity_verified** | replace |
| `thaumcraft/common/config/recipes/ConfigRecipesInfusionSlice.java` | partial | **parity_verified** | replace |
| `thaumcraft/api/IArchitect.java` | partial | partial | adapt |
| `thaumcraft/api/aspects/AspectSourceHelper.java` | partial | partial | adapt |
| `thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java` | partial | partial | adapt |
| `thaumcraft/common/container/SlotGhostFluid.java` | partial | partial | adapt |
| `thaumcraft/common/items/armor/ItemVoidRobeArmor.java` | partial | partial | port |

13 единиц перешли в `parity_verified`, 5 остались `partial` (см. §4 «Что НЕ закрыто»). Ни одна строка
ledger вне моих 18 единиц не изменена; `counts` не тронуты.

## 2. Найденные и исправленные дефекты

### D1. `ThaumcraftApiHelper.cullTags` — бесконечный цикл (вис сервера)

* Оригинал: `thaumcraft/api/ThaumcraftApiHelper.java:38-70` — `float low = Short.MAX_VALUE;` и
  `temp2.aspects.remove(lowest);` внутри `while (temp2.size() > 6)`. Если взвешенная стоимость **каждого**
  аспекта не меньше `Short.MAX_VALUE`, кандидат не находится, `lowest` остаётся `null`, `remove(null)` не
  меняет список, и цикл не заканчивается **никогда**.
* Порт (было): `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:38-70` — тот же алгоритм
  (унаследован из оригинала).
* Фикс: `if (lowest == null) break;` — сulling останавливается, когда нечего отбрасывать. Для обычного
  списка результат оригинала не меняется (проверено тестом `cullingDropsTheLeastValuableAspectUntilSixRemain`).
* Отклонение задокументировано в javadoc метода.

### D2. `ThaumcraftApiHelper.areItemsEqual` — потерянное правило «изношенного» предмета

* Оригинал: `thaumcraft/api/ThaumcraftApiHelper.java:68-74` —
  `if (s1.isItemStackDamageable() && s2.isItemStackDamageable()) return s1.getItem() == s2.getItem();`
  иначе сравниваются item **и** damage.
* Порт (было): всегда сравнивался и damage → два разных по износу кирки считались разными.
* Фикс: восстановлена оригинальная ветка; 1.21.1-эквивалент `isItemStackDamageable()` →
  `ItemStack.isDamageableItem()`. Null-стек отвечает `false` вместо NPE оригинала (безопасное отклонение).

### D3. `ThaumcraftApiHelper.getStackInRowAndColumn` — транспонированная ячейка сетки

* Оригинал: `thaumcraft/common/tiles/TileMagicWorkbench.getStackInRowAndColumn` читает ячейку
  `row + column * 3`; вызывающий код `thaumcraft/common/lib/crafting/ArcaneSceptreRecipe.java:42-46`
  спрашивает `(1,0)`, `(2,1)`, `(0,2)`, `(1,1)`, `(2,0)` — это слоты 1, 5, 6, 4, 2 сетки.
* Порт (было): `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java:60` считал
  `row * 3 + column` — **транспонированная** ячейка. Вторая, «параллельная» реализация в порту
  (`common/src/main/java/thaumcraft/common/lib/InternalMethodHandler.java:73`) считала правильно
  (`row + column * 3`) — две реализации одного публичного метода расходились.
* Фикс: публичный обработчик приведён к оригинальному порядку; javadoc в `ThaumcraftApiHelper`
  исправлен (он ошибочно утверждал, что «оригинальный порядок аргументов был column-then-row»).

### D4. `ItemApi.getBlockItem(null)` — NullPointerException

* Порт (было): `common/src/main/java/thaumcraft/api/ItemApi.java:74` → `id(null)` → `name.contains(":")` → NPE.
* Фикс: пустое/отсутствующее имя отвечает `null`, как и остальные методы класса.

### D5. `ThaumcraftApi.getCraftingRecipeKey` — утечка внутреннего массива

* Порт (было): `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:280` отдавал **тот же** массив,
  что лежит в `keyCache`; вызывающий мог испортить кеш registry.
* Фикс: отдаётся копия (оригинальный массив был новым при каждом вызове, потому что его ключ `int[]`
  сравнивался по идентичности и кеш никогда не срабатывал).

### D6. `ScanningManager.scannableThings()` — утечка реестра

* Порт (было): `common/src/main/java/thaumcraft/api/research/ScanningManager.java:24` отдавал внутренний
  `ArrayList`, который можно было править извне.
* Фикс: `List.copyOf(things)`.

### D7. `ContainerGhostSlots` — творческий клон менял превью ghost-слота

* Оригинал: `thaumcraft/common/container/ContainerGhostSlots.java`, ветка `mod == 3` — стек слота
  копируется в полный стек и отдаётся в курсор, **но только для настоящего слота**; для `SlotGhost`
  копия выбрасывается, превью не меняется.
* Порт (было): `common/src/main/java/thaumcraft/common/container/ContainerGhostSlots.java:97-101` ставил в
  превью полный стек того же предмета — то есть менял количество превью (а это меняет и поведение
  устройства, которое читает превью).
* Фикс: творческий клон ghost-слота не меняет превью.

### D8. Ложный «незакрытый» пункт аудита: hotbar-swap ghost-слота

* Оригинал: ветка `mod == 2` (hotbar swap) защищена `slot2.canTakeStack(player)`, а
  `thaumcraft/common/container/SlotGhost.java:31` возвращает `false` → превью сохраняется.
* Порт: превью сохраняется → **совпадает**. Пункт «REMAINING GAP» в ledger был ложным; исправлен.

### D9. Ложная посылка аудита о `InventoryMob`

* Оригинал (байт-код): `thaumcraft/common/entities/InventoryMob` **реализует `IInventory`**, а не является
  базовым классом сущности; пользователи класса — гolem-семейство (`EntityGolemBase`, `ContainerGolem`,
  `GolemHelper`, `ItemGolemBell`, `ItemGolemPlacer`, `AIItemPickup`) и GUI гolemа.
* Порт: `common/src/main/java/thaumcraft/common/entities/InventoryMob.java` — `Container` с теми же
  членами и теми же quirks (на один слот больше, чем есть; пустой слот не сливается; изношенный предмет
  кладётся целиком). Вывод аудита («аддон, наследовавший оригинал, переписывается») был неверен: наследовать
  было нечего. Единица переведена в `parity_verified` с исправленной посылкой.

### D10. `AspectList` и неизвестный тег аспекта — задокументированное отклонение

* Оригинал (байт-код `AspectList.add`): проверки `null` нет → неизвестный тег (`Aspect.getAspect` вернул
  `null`) становился **ключом карты**. При этом `getPrimalAspects` вызывает `isPrimal()` на каждом ключе
  без проверки `null` → **NPE оригинала**; `getAspectsSorted` наоборот null проверяет (оригинал сам себе
  противоречит).
* Порт: `add` отбрасывает `null`-аспект, поэтому неизвестный тег не считается `size()` и не отвечает в
  `getAmount`. Это предотвращает падение клиента на предмете другого мода. Отклонение задокументировано в
  javadoc `AspectList.add` и в ledger; вызов `getPrimalAspects`/`getAspectsSorted*` на таком списке
  безопасен.

### D11. Отсутствовавшие опубликованные методы API

Сверка байт-кода оригинала с нативным API (`.tmp/api_surface_diff.py`, сверка **имён** членов) нашла и
вернула в порт:

* `thaumcraft.api.FocusUpgradeType.getLocalizedName()` / `getLocalizedText()` — в оригинале
  `thaumcraft/api/wands/FocusUpgradeType.java` это переводы ключей `name`/`text`; в порту были только сырые
  ключи. Добавлены (`common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java`).
* `thaumcraft.api.wands.ItemFocusBasic.getOrnament(ItemStack)`,
  `getFocusDepthLayerIcon(ItemStack)`, `getMaxAreaSize(ItemStack)`,
  `onFocusBlockStartBreak(ItemStack,int,int,int,Player)` — в оригинале
  `thaumcraft/api/wands/ItemFocusBasic.java` это опубликованные методы базового класса (`null`, `null`,
  `1`, `false`); в порту их не было. Добавлены с оригинальными значениями по умолчанию.
* `thaumcraft.api.wands.WandTriggerRegistry.hasTrigger(Block,int meta)` — оригинальная форма с
  метаданными; в порту была только форма без метаданных. Добавлена (метаданные 1.21.1 не существует и
  игнорируются, как задокументировано в классе).
* `thaumcraft.api.crafting.ShapedArcaneRecipe.checkItemEquals(ItemStack,ItemStack)` и
  `ShapelessArcaneRecipe.checkItemEquals(ItemStack,ItemStack)` — `protected`-методы оригинала
  (`thaumcraft/api/crafting/ShapedArcaneRecipe.java`); в порту отсутствовали. Добавлены с оригинальной
  логикой (item + теги + damage, wildcard 32767).

### D12. `thaumcraft.api.BlockCoordinates` — опубликованный тип возвращён

* Оригинал: `thaumcraft/api/BlockCoordinates.java:13` — изменяемый тройник с `equals`, `hashCode`
  (`y*31 + x*91 + z*29303`), `compareWorldCoordinate`, `set(IIII)`, `getDistanceSquared` (**float**),
  `compareTo` и NBT-ключами `b_x`/`b_y`/`b_z`.
* Порт: класса не было (`disposition: exclude`). Класс возвращён
  (`common/src/main/java/thaumcraft/api/BlockCoordinates.java`) в 1.21.1-виде (`BlockEntity`,
  `CompoundTag`); числа, сравнение, хеш и NBT-ключи — оригинальные. `disposition` изменён на `port`.

### D13. Идентичность пяти зачарований оригинала опубликована ключами

* Оригинал: `thaumcraft/api/ThaumcraftApi.java:57-61` публиковал числовые id `enchantFrugal`,
  `enchantPotency`, `enchantWandFortune`, `enchantHaste`, `enchantRepair`.
* Порт: поля остались `-1` (в 1.21.1 нет числовых id зачарований), но рядом добавлены
  `ENCHANTMENT_FRUGAL`, `ENCHANTMENT_POTENCY`, `ENCHANTMENT_WAND_FORTUNE`, `ENCHANTMENT_HASTE`,
  `ENCHANTMENT_REPAIR` (`ResourceKey<Enchantment>`), что закрывает пробел идентичности.

### D14. Ответ «пустого» рецепта в `getCraftingRecipes` — фактически уже закрыт

Пункт ledger «the native recipe list holds the native recipe views and not the original IArcaneRecipe
objects» устарел: `common/src/main/java/thaumcraft/api/crafting/NativeRecipes.java:72-84` отдаёт именно
оригинальные представления (`ShapedArcaneRecipe`, `ShapelessArcaneRecipe`, `CrucibleRecipe`,
`InfusionRecipe`, `InfusionEnchantmentRecipe`). Проверено и отражено в ledger.

### D15. Сверимость рецептов инфузии (две единицы `common/config/recipes`)

Заметки ledger о «нет нативного файла рецепта» для `NODESTABILIZERADV` и `WANDPEDFOC` **устарели**: файлы
`data/thaumcraft/recipe/infusion/advanced_node_stabilizer.json` и `.../wand_pedestal_focus.json` есть,
как и весь остальной набор (`focus_*`, `jar_brain`, `golem_*`, `arcane_bore`, `growth_lamp`,
`fertility_lamp`, `essentia_reservoir`, `wand_cap_*`, `wand_rod_*`, 24 рецепта зачарований в
`data/thaumcraft/recipe/infusion_enchantment/`). Отложенным остаётся только семейство серебра
(в 1.21.1 нет серебра) и `MirrorEssentia` — обе причины записаны в `ResearchRecipeCoverage`.

### D16. Ответы на открытые вопросы аудита (проверено по байт-коду оригинала)

* `TileNitor`: `updateEntity` оригинала целиком лежит в ветке `isRemote`, а `BlockAiry.randomTick` рисует
  только клиентские частицы → **серверного производства эссенции у нитора в оригинале нет**; нативный
  нитор ведёт себя так же.
* `TileManaPod`: `checkGrowth` оригинала растёт до 7, на третьем росте собирает аспекты соседей в порядке
  `ForgeDirection` 2,3,4,5 (NORTH, SOUTH, WEST, EAST), комбинация аспектов попадает в список выбора
  **дважды**, запасной вариант — первый элемент `getAspectsSortedAmount()`, без аспекта — PLANT в одном
  случае из восьми, иначе случайный праймальный. Нативный под выполняет ровно это.
* `TileBanner`: NBT-ключи `facing`, `color`, `aspect`, `wall` совпадают.
* `TileThaumcraftInventory`: оригинальный базовый класс владеет **одним** слотом, но сообщает
  `getSizeInventory() == 64`, использует длину своего массива как предел стека и читает слоты **без проверки
  границ** (воспроизводимый `ArrayIndexOutOfBoundsException`); нативный класс сообщает настоящий размер и
  читает слоты безопасно — отклонение задокументировано.
* `SlotLimitedByItemstack`: единственный класс файла в JAR, который упоминает это имя, — сам класс:
  мёртвый код оригинала, исключение подтверждено по барджу.

## 3. Что НЕ удалось закрыть (и почему)

| единица | блокер |
|---|---|
| `thaumcraft/api/IArchitect.java` | Нативный архитектор — серверно-авторитетная переработка: сервер владеет областью (`WandArchitect.Area`), клиент рисует присланное значение, **списка подсвечиваемых блоков в нативе нет**. Опубликованные точки оригинала (`getArchitectBlocks`, `showAxis`, `EnumAxis`) воспроизвести нельзя без выдуманного превью, которым нативный путь не пользуется; нативный путь и не спрашивает переопределение аддона. Диспетчер лежит в `common/items/**` и `common/blocks/**`, которые этой волне не принадлежат. Тип `BlockCoordinates` возвращён (D12). |
| `thaumcraft/api/aspects/AspectSourceHelper.java` | Оригинал слал пакет «потока эссенции» игрокам вокруг тайла (`EssentiaHandler:54`). В нативе такого пакета для произвольного потребителя нет; матрица инфузии держит только свой эффект. Поиск, диапазон, направление и порядок обхода — оригинальные. |
| `thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java` | В 1.21.1 нет `EntityDamageSourceIndirect` и нет изменяемого источника урона: модификаторы «огонь» и «неуязвимость» стали свойствами типа урона, их выбирает вызывающий. Класс-фабрика отвечает именем прямого атакующего и настоящей причины, как оригинал. |
| `thaumcraft/common/container/SlotGhostFluid.java` | В 1.21.1 нет реестра fluid-контейнеров Forge; нативный слот принимает единственный ванильный fluid-контейнер — ведро. Контейнер аддона будет отвергнут. |
| `thaumcraft/common/items/armor/ItemVoidRobeArmor.java` | Разрез «одежда на игроке»: оригинальный `ModelRobe` не перенесён, нативный использует слои пустотной брони. Пробел в клиентском слое (`thaumcraft/client/**`), который этой волне не принадлежит. |

Найдено, но не исправлено из-за границы владения:

* **Реестр триггеров палочки (`thaumcraft.api.wands.WandTriggerRegistry`) нигде не вызывается.** В оригинале
  `ItemWandCasting:389-390` спрашивает `hasTrigger`/`performTrigger` — это первое, что делает правый клик
  палочки. В порту вызов живёт в `common/items/wands/ItemWandCasting.java` (вне моей зоны), а
  `WandTriggerRegistry` вызывается только тестами. Собственные применения оригинала (книга на полке,
  тигель на котле) натив закрывает напрямую (`WandManager.canCreateThaumonomicon`, `BlockCrucible.tryCreate`),
  поэтому теряется **только точка расширения для аддонов**.
* **`NodeAdapter.setNodeType/setNodeModifier` бросают `UnsupportedOperationException`**: у
  `common/tiles/TileNode.java` нет сеттеров типа/модификатора, а `common/tiles/**` вне моей зоны.

## 4. Тесты

**Авторованы, но НЕ выполнены** (Gradle в этой волне запускать нельзя, готовой сборки нет):

* `common/src/test/java/thaumcraft/api/PublicApiContractTest.java` (новый) — 12 тестов:
  завершаемость `cullTags` при отсутствии кандидата; правило «изношенного» предмета в `areItemsEqual`;
  порядок ячеек сетки `row + column * 3` (три разных предмета делают транспозицию видимой); отказ ячейки
  вне сетки; отсутствие исключений в `ItemApi`; обработка неизвестного тега аспекта (NBT round-trip);
  неизменяемость реестра сканирования; нулевой ответ ключа рецепта; полный список 48 аспектов; оригинальная
  семантика `BlockCoordinates` (сравнение, хеш, расстояние, NBT-ключи).
* `common/src/test/java/thaumcraft/common/container/GhostSlotMenuTest.java` (новый) — 5 тестов:
  творческий клон не меняет превью; быстрое перемещение очищает/дополняет превью (+16, предел слота);
  hotbar-swap сохраняет превью; бросок убирает один предмет или весь превью; взятая стек заполняет пустое
  превью, а другая заменяет превью целиком.
* `common/src/test/java/thaumcraft/api/ApiSurfaceTest.java` — в список оригинальных классов API добавлен
  `thaumcraft.api.BlockCoordinates`.

Проверка JSON-консистентности ledger: `.tmp/check_scope_ledger_local.py` → `problem_count: 0`,
`stale_review_fingerprints: 0` (1044 единицы). Настоящий
`python3 scripts/porting/update_scope_ledger.py --check` в этой песочнице не запускается: нет приватного
оракула (`Missing source root: /home/cyberfox-kali/wt/work/thaumcraft-4.2.3.5-oracle`).

## 5. Вынужденные изменения публичных сигнатур (1.21.1)

Полный список того, что обязано было измениться в `thaumcraft.api.*` при переходе на 1.21.1.

**Типы Minecraft**

| было (1.7.10) | стало (1.21.1) | где |
|---|---|---|
| `net.minecraft.world.World` | `net.minecraft.world.level.Level` | `ThaumcraftApiHelper`, `AspectSourceHelper`, `IInfusionStabiliser`, `InfusionRecipe`, `InfusionEnchantmentRecipe`, `IArcaneRecipe`, `IWandFocus`, `IWandable`, `IWandTriggerManager`, `IScanEventHandler`, `IArchitect` |
| `net.minecraft.entity.player.EntityPlayer` | `net.minecraft.world.entity.player.Player` | там же |
| `net.minecraft.tileentity.TileEntity` | `net.minecraft.world.level.block.entity.BlockEntity` | `AspectSourceHelper`, `TileThaumcraft`, `BlockCoordinates` |
| `net.minecraft.item.ItemStack` | `net.minecraft.world.item.ItemStack` (неизменяемый) | весь API |
| `net.minecraft.util.IIcon` | `net.minecraft.resources.ResourceLocation` | `IWandFocus`, `ItemFocusBasic` |
| `net.minecraft.util.MovingObjectPosition` | `net.minecraft.world.phys.BlockHitResult` | `IWandFocus` |
| `net.minecraft.nbt.NBTTagCompound` | `net.minecraft.nbt.CompoundTag` | `AspectList`, `TileThaumcraft`, `BlockCoordinates`, `WorldCoordinates` |
| `net.minecraftforge.common.util.ForgeDirection` | `net.minecraft.core.Direction` | `AspectSourceHelper`, `IEssentiaTransport` |
| `net.minecraft.inventory.IInventory` | `thaumcraft.common.crafting.ArcaneRecipeInput` / `net.minecraft.world.item.crafting.RecipeInput` | `IArcaneRecipe`, `Shaped/ShapelessArcaneRecipe` |
| `net.minecraft.util.ResourceLocation` | `net.minecraft.resources.ResourceLocation` | `Aspect`, `WandCap`, `WandRod`, `ResearchCategories`, `ResearchItem` |
| `net.minecraft.util.StatCollector` | `net.minecraft.network.chat.Component` | `Aspect.getLocalizedDescription`, `FocusUpgradeType.getLocalizedName/Text` |
| `net.minecraft.potion.Potion` | `net.minecraft.world.effect.MobEffect` (+ `Holder<MobEffect>`) | `PotionFluxTaint`, `PotionVisExhaust` |
| `net.minecraft.enchantment.Enchantment` (int id) | `Holder<Enchantment>` / `ResourceKey<Enchantment>` | `InfusionEnchantmentRecipe`, `IWandFocus.acceptsEnchant`, `ThaumcraftApi` (5 id) |
| `net.minecraft.item.Item.ToolMaterial` | `net.minecraft.world.item.Tier` | `ThaumcraftApi`, `ThaumcraftMaterials` |
| `ItemArmor.ArmorMaterial` | `net.minecraft.world.item.ArmorMaterial` | `ThaumcraftApi`, `ThaumcraftMaterials` |
| `net.minecraft.util.DamageSource` | `net.minecraft.world.damagesource.DamageSource` + `Holder<DamageType>` | `DamageSourceThaumcraft`, `DamageSourceIndirectThaumcraftEntity` |
| `net.minecraft.network.Packet` / `S35PacketUpdateTileEntity` | `ClientboundBlockEntityDataPacket` | `TileThaumcraft` |
| `net.minecraft.network.NetworkManager` | удалён (1.21.1) | `TileThaumcraft.onDataPacket` → `loadAdditional` |
| `net.minecraft.tileentity.TileEntity.getRenderBoundingBox` | `BlockEntity` не имеет (у нативного узла свой) | `TileVisNode` |

**Структурные изменения**

* `IInfusionStabiliser.canStabaliseInfusion(World,int,int,int)` → `(Level, BlockPos)`.
* `IArcaneRecipe.matches/getCraftingResult/getAspects` — `IInventory` → `ArcaneRecipeInput`.
* `InfusionRecipe`/`InfusionEnchantmentRecipe.matches(ArrayList<ItemStack>,ItemStack,World,EntityPlayer)` →
  `(..., Level, Player)`.
* `AspectList.readFromNBT/writeToNBT` — `NBTTagCompound` → `CompoundTag`; добавлен вариант с меткой.
* `AspectSourceHelper.drainEssentia/findEssentia(TileEntity,Aspect,ForgeDirection,int)` →
  `(BlockEntity,Aspect,Direction,int)`.
* `IEssentiaTransport` — `ForgeDirection` → `Direction` в восьми методах.
* `DamageSourceThaumcraft`: четыре статических поля `DamageSource` (`taint`, `tentacle`, `swarm`,
  `dissolve`) → `ResourceKey<DamageType>` + фабрики `taint(Level)`, `dissolve(Level)`,
  `causeTentacleDamage(LivingEntity)`, `causeSwarmDamage(LivingEntity)`. Причина: источник урона 1.21.1
  строится из `Holder` зарегистрированного типа, а реестр недоступен на этапе статической инициализации.
* `DamageSourceIndirectThaumcraftEntity`: класс-наследник `EntityDamageSourceIndirect` → фабрика
  `create(Entity,Entity,Holder<DamageType>)`; конструктор `(String,Entity,Entity)` и изменяемые
  `setFireDamage()/setUnblockable()` исчезли (модификаторы стали свойствами типа урона).
* `ThaumcraftApi.getCraftingRecipes()` → `List<Object>` из реестра рецептов загруженного уровня
  (`RecipeManager`), а не из статического списка; `getCraftingRecipeKey` → `{research, pageIndex}`.
* `ThaumcraftApi.registerObjectTag(String, AspectList)`: строка — теперь namespaced-тег предметов
  (в 1.21.1 нет OreDictionary); `registerComplexObjectTag` получил вариант с `Level`.
* `ThaumcraftApi.addArcaneCraftingRecipe/addInfusionCraftingRecipe/…`: аргумент-«руда» рецепта стал
  namespaced-тегом.
* `WandTriggerRegistry`: ключ `(Block,int meta)` → `Block` или точный `BlockState`; `performTrigger` →
  принимает `BlockState`.
* `ItemFocusBasic`: `getIconFromDamage/getRarity/isFoil/addInformation/onUpdate` (SRG-имена
  `func_77617_a/func_77613_e/func_77645_m/func_77624_a/func_77663_a`) → родные
  `getItemIcon`/предметные свойства/`appendHoverText`/`inventoryTick`; `getOrnament`/
  `getFocusDepthLayerIcon` → `ResourceLocation`.
* `TileThaumcraft`: конструктор требует `BlockEntityType`; `readFromNBT/writeToNBT` → `loadAdditional`/
  `saveAdditional` с `HolderLookup.Provider`; `getDescriptionPacket` → `getUpdatePacket`.
* `WorldCoordinates`: оригинал был изменяемым и без измерения; нативный — неизменяемый с
  `ResourceKey<Level>`; изменяемый опубликованный тройник возвращён отдельным классом
  `BlockCoordinates`.
* `TileVisNode`/`VisNetHandler`: `getParent()/getChildren()` оригинала отдавали сильные ссылки и
  `ArrayList`; нативные отдают `WeakReference` и живут в серверном графе уровня; `findClosestNodes`
  оригинала вместе с глобальным кешем не воспроизводим.
* `PotionFluxTaint`/`PotionVisExhaust`: классы `Potion` → зарегистрированные `MobEffect` с ключом
  `thaumcraft:flux_taint` / `thaumcraft:vis_exhaust` и фабриками `instance()`/`holder()`.

## 6. Файлы

Изменены (прод):

* `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java`
* `common/src/main/java/thaumcraft/api/ThaumcraftApi.java`
* `common/src/main/java/thaumcraft/api/ItemApi.java`
* `common/src/main/java/thaumcraft/api/IArchitect.java`
* `common/src/main/java/thaumcraft/api/BlockCoordinates.java` (новый)
* `common/src/main/java/thaumcraft/api/aspects/AspectList.java`
* `common/src/main/java/thaumcraft/api/internal/ThaumcraftInternalMethods.java`
* `common/src/main/java/thaumcraft/api/research/ScanningManager.java`
* `common/src/main/java/thaumcraft/api/crafting/ShapedArcaneRecipe.java`
* `common/src/main/java/thaumcraft/api/crafting/ShapelessArcaneRecipe.java`
* `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java`
* `common/src/main/java/thaumcraft/api/wands/ItemFocusBasic.java`
* `common/src/main/java/thaumcraft/api/wands/WandTriggerRegistry.java`
* `common/src/main/java/thaumcraft/common/container/ContainerGhostSlots.java`
* `docs/porting/scope-ledger.json` (только 18 единиц)

Добавлены (тесты):

* `common/src/test/java/thaumcraft/api/PublicApiContractTest.java`
* `common/src/test/java/thaumcraft/common/container/GhostSlotMenuTest.java`

Изменён (тест): `common/src/test/java/thaumcraft/api/ApiSurfaceTest.java`.

## 7. Что НЕ проверено

* Ни один тест не выполнен: Gradle в этой волне не запускается (общий кеш Loom), готовой сборки нет.
* Клиент, мультиплеер, survival-прогрессия и NeoForge-паритет не проверялись.
* Визуальная приёмка не проводилась.
* Оригинал читался из байт-кода JAR, а не из приватного декомпилята оракула; расхождения между этими
  двумя представлениями оригинала (OpenTC4 — это форк с модификациями) учтены: там, где OpenTC4 отличался
  от байт-кода (например, `AspectList.add` с проверкой `null` в OpenTC4 и без неё в оригинале), решал
  байт-код JAR.
