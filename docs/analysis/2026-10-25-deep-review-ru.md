# Углублённое ревью порта TC4 на Minecraft 1.21.1 — 25.10.2026

## Результат и точный снимок

Проверен **`c4a8f7a8cc5f56458d6afe7a5551aa31d6957eff`**, версия `0.1.0-alpha.5`, локальная ветка `work`. Удалённый `main` проверен через GitHub API: тот же SHA. Исходное рабочее дерево чистое; переключения ветки, reset, checkout, fetch с обновлением refs и изменения production-кода не выполнялись.

Найдено **18 замечаний в достижимом коде**, включая условные сценарии публичного API/аддонов: 3 P1, 13 P2, 2 P3. Дополнительно отдельно описаны дефект отложенного essentia-mirror, незавершённая survival-доступность и мёртвые реализации. Это результат целевого глубокого прохода, а не доказательство отсутствия остальных ошибок во всех 1495 Java-файлах современных production source sets.

Наиболее существенны смешение единиц при зарядке пьедесталом, создание объёма Liquid Death, падение наполненного Spa GUI на NeoForge, две независимые ошибки связи зеркал и отсутствие исполнения API-рецептов. Большинство выводов **статически подтверждены**. Обязательные компоненты ингредиента **воспроизведённо игнорируются точным production helper**; полный игровой сценарий этого дефекта не запускался. Ни один новый дефект не выдаётся за воспроизведение в физическом клиенте.

Прочитан корневой [AGENTS.md](/workspace/Thaumcraft-1.21.1/AGENTS.md). Других применимых AGENTS.md в активных модулях нет. Соблюдены Fabric-first и запрет сборок/тестов NeoForge. Старые разрешения на изменение/публикацию из проектных заметок не использовались: текущий запрос разрешает ревью и проверки, запрещает push/PR/публикацию/платный CI. Они не выполнялись. Оригинальный JAR, `thaumcraft_src`, штатные тесты, манифесты и контрольные суммы не менялись.

Источники спецификации:

- Приватный TC4 4.2.3.5 oracle вне репозитория: `/workspace/work/thaumcraft-4.2.3.5-oracle`. SHA-256 оригинального JAR: `3dba9786966974701578a658d1bb369bf35bdf5f363079f5ac9c4910a39113be`.
- Фактические cached Minecraft 1.21.1/Architectury 13.0.11/Fabric API/NeoForge 21.1.259: использованы sources и bytecode, где важен контракт вызова. Чтение NeoForge не является запуском его сборки.
- Для конечной жидкости — [родитель Forge 1.7.10, закреплённый коммит `9274e4fe`](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/src/main/java/net/minecraftforge/fluids/BlockFluidFinite.java). Донор 1.20.1 не использован как доказательство корректности 1.21.1.

P1 означает высокий приоритет: создание/потеря ресурсов или детерминированное падение. P2 — сломанная функция, синхронизация или контракт аддонов. P3 — ограниченный сценарий/несогласованность интерфейса. Приоритет не обозначает доказанный exploit.

## Приоритеты

| ID | Приоритет | Замечание | Доказательство / область |
|---|---|---|---|
| R01 | P1 | Пьедестал создаёт vis либо списывает в 100 раз больше | Статически; штатные жезлы, посохи и амулеты |
| R02 | P1 | Liquid Death увеличивает конечный объём 4→16 / 4→7 | Статически; bucket→scheduled tick |
| R03 | P1 | Наполненный Spa GUI падает на NeoForge | Статически; прежний открытый дефект |
| R04 | P2 | Размещение последнего привязанного зеркала теряет связь | Статически + фактический контракт Minecraft |
| R05 | P2 | Межизмерительное зеркало отвергает правильную обратную ссылку | Статически; item mirror активен |
| R06 | P2 | Усилитель пьедестала не преобразует compound; возможна потеря аспекта | Статически; доступный infusion item |
| R07 | P2 | Пьедестал не заполняет последние 1–99 centivis | Статически; обычный остаток после каста |
| R08 | P2 | Вставка/изъятие жезла на пьедестале не гарантирует S2C sync | Статически + контракт BlockEntity.setChanged |
| R09 | P2 | Смена измерения оставляет клиент без snapshot аксессуаров | Статически + bytecode respawn |
| R10 | P2 | Публичные add*Recipe не достигают игровых машин | Статически; аддоны |
| R11 | P2 | Arcane matcher игнорирует обязательные компоненты ингредиента | Воспроизведение helper; цепочка matcher статически |
| R12 | P2 | Enchantment infusion обходит overridable comparator | Статически; прямой вызов API подкласса |
| R13 | P2 | Craft callback верстака получает уже потраченную матрицу | Статически; подписчики аддонов |
| R14 | P2 | CastingWandHud не вызывается на NeoForge | Статически; регистрация callback |
| R15 | P2 | HUD не рисует focus icon/cooldown/Trade readout/Shift-цену | Статически против TC4 |
| R16 | P2 | Liquid Death наносит magic вместо dissolve | Статически; wrong damage protections |
| R17 | P3 | Разрешённый offhand wand не имеет HUD | Статически; двухручная адаптация |
| R18 | P3 | FocusTrade ограничен ray=5 вопреки native reach игрока | Статически; изменённый атрибут дальности |

## Доказательства и необходимые исправления

### R01 — P1: несовместимые единицы зарядки пьедестала

Место: [TileWandPedestal.java:111](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/common/tiles/TileWandPedestal.java:111), строки 111–120 и 134–144; [TileNode.java:434](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/common/tiles/TileNode.java:434), 434–441. **Статически подтверждено**, независимо прочитано агентом магии и root.

Цепочка: зарегистрированный pedestal → `BlockWandPedestal.getTicker:65–67` (также stone-device ticker) → `TileWandPedestal.tick:95–96` → `drain`. Узел хранит целые VIS; предмет — centivis. Резерв узла ошибочно умножен на 100; после credit 100cV вызывается `node.takeFromContainer(aspect,100)`, результат не проверен.

При узле aer=50 и greatwood/gold wand заряд полностью заблокирован резервом100; ожидались wand+100cV/node−1. С iron cap или wood rod резерв0: wand получает100cV, debit100 из узла50 возвращает false, узел остаётся50. При узле200 один credit100cV списывает100 целых VIS. Амулет имеет ту же ошибку. Оригинал `TileWandPedestal:200–214,249–260` использует резерв1/0 и debit1.

Исправление: хранить резерв/debit узла в целых единицах, конвертировать только credit предмета; связать успешное списание и зачисление с защитой от повторного входа. Тест: настоящий зарегистрированный ticker, node50/node200, iron/gold caps, staff и amulet, несколько draw ticks; точный баланс node−1/item+100cV для обеих форм пьедестала.

### R02 — P1: Liquid Death создаёт объём

Место: [BlockFluidDeath.java:206](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/common/blocks/BlockFluidDeath.java:206), 206–215, 226–242. **Статически подтверждено**, независимо world-агентом и root, включая контракт родителя оригинала.

Цепочка: `crucible/bucket_of_death.json`/LIQUIDDEATH → `ItemBucketDeath.use/pour` → level0 → `onPlace:261`/`neighborChanged:266` → scheduled `tick:271` → `flowTick`. На полу с четырьмя свободными соседями исходные4 кванта остаются, каждый сосед получает3: **4→16**. Над воздухом источник4 остаётся, снизу появляется3: **4→7**.

TC4 `BlockFluidDeath:33–41` наследует конечный `BlockFluidFinite`, задаёт4 кванта. Forge распределяет существующий total (`:94–131,231`) и при полном вертикальном переносе очищает origin (`:255–258`). Здесь перенесено правило силы неиссякаемого источника. Последствие — неправильный объём/площадь опасной жидкости; бесконечное получение bucket items не доказано.

Исправление: finite4 с вертикальным переносом, горизонтальным распределением суммы, остатком/RNG и корректным scheduling при существующем кодировании level. Тест: native ServerLevel, сумма квантов до/после каждого настоящего flow tick, пол/падение/слияние/серия тиков/reload. Проверка одних констант и strongest-neighbour rule этого не обнаруживает.

### R03 — P1: падение Spa GUI на NeoForge

Место: [GuiSpa.java:32](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/client/gui/GuiSpa.java:32), 32,41–42; единственная установка — [FabricSpaClient.java:11](/workspace/Thaumcraft-1.21.1/fabric/src/main/java/thaumcraft/fabric/client/FabricSpaClient.java:11). **Статически подтверждено**, root перепроверил; NeoForge не запускался.

Цепочка: NeoForge client setup → общий `ClientSetup` → `ThaumcraftSpaClient` screen factory → `GuiSpa.renderBg`. При непустом баке ветка требует `fluidAppearance != null`. Поле изначально null; NeoForge setter не вызывает. Фактически следует NPE вместо native fluid sprite; пустой бак маскирует проблему.

Исправление: NeoForge bridge через фактический `IClientFluidTypeExtensions`/atlas sprite с оригинальным белым tint. Тест после разрешения NeoForge-проверок: обычное открытие наполненного Spa, реальный draw, пустой бак и неподдерживаемая иконка. Server menu tests renderer не вызывают. Это **прежнее открытое замечание** `LOADER_QUALITY.md` (там P2 — идентификатор замечания, High — серьёзность).

### R04 — P2: последнее зеркало расходуется до чтения привязки

Место: [ItemMirrorBlock.java:76](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/common/blocks/ItemMirrorBlock.java:76), 76–88,97–101. **Статически подтверждено**, два агента и root отдельно сверили bytecode используемого MC.

Условие: survival, ровно один привязанный mirror item. Цепочка: `useOn:62–67` создаёт bound item → обычное размещение → `place:77` вызывает `super.place` → MC `BlockItem.place` расходует1 до возврата → `linkOf(context.getItemInHand()):81`. В MC `ItemStack.getComponents()` при count0 возвращает EMPTY; link становится null, метод выходит без `restoreLink`.

Ожидание по oracle `BlockMirrorItem:141–165`: установленное зеркало получает партнёра предмета. Фактически панель размещается несвязанной даже в том же измерении; creative/count>1 скрывают ошибку. Исправление: получить независимый `MirrorLink` до `super.place`, применить после успешного размещения. Тест: реальное связывание и placement count1 в survival, взаимные ссылки; count2/creative и отказ placement как контроль. Не ограничиваться прямым вызовом restoreLink.

### R05 — P2: неправильное измерение в обратной ссылке зеркала

Место: [TileMirror.java:118](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/common/tiles/TileMirror.java:118), 118–119,135–136. Аналогично `TileMirrorEssentia:110–111,126–127`, но его survival-доступность отложена. **Статически подтверждено**, независимо network/world/root.

Условие: корректная пара A Overworld ↔ B Nether, доступные чанки. `restoreLink:82` правильно записывает B.linkDim=Overworld. Но A сравнивает B.linkDim с A.linkDim=Nether, а не измерением A. Цепочка item ingress: `BlockMirror.entityInside` → `TileMirror.transport:155` → `isLinkValid` → false и сброс linked; repair ticker также использует ошибочный simple check.

Оригинал `TileMirror:134,159` сравнивает с `this.world.provider.dimensionId`. Исправление: сравнивать с текущим `level.dimension()` в обеих проверках обоих классов, желательно ResourceKey equality без hashCode. Тест: настоящая пара двух ServerLevel, оба направления, ItemEntity ingress/выход, точные count/components, reload. R04 маскирует initial count1 placement, поэтому для изоляции R05 нужны уже корректная сохранённая пара/creative либо отдельное устранение R04. Полный отказ hopper/hand-mirror paths не утверждается: у них другие проверки.

### R06 — P2: усилитель пьедестала не реализует compound conversion

Место: [TileWandPedestal.java:116](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/common/tiles/TileWandPedestal.java:116), 116–120,139–143,175–179; `BlockWandPedestal:71`. **Статически подтверждено**, агент магии/root.

Усилитель доступен через `infusion/wand_pedestal_focus.json`; размещается с FOCUS=true. Активный drain жезла перебирает все аспекты, `ItemWandCasting.addRealVis:83` отвергает compound, но результат игнорируется и следует debit. При достаточном запасе compound списывается без credit; амулет просто отфильтровывает compounds. Проверки установленного усилителя и преобразования нет.

Оригинал `TileWandPedestal:224–239,269–284` только с усилителем выбирает подходящий primal через reduceToPrimals и debit1 compound → credit1 whole primal. Дополнительно `hasFocus` не вызывается, а его `isFocus()` читает default state, всегда false: это отдельно мёртвый неверный helper.

Исправление: обычный обход только primal; отдельная разрешённая реальным state усилителя конверсия для wand/amulet с проверкой credit. Ошибочный размер debit уже учтён в R01; отсутствие конверсии останется и после его исправления. Тест: только compound в узле, with/without действительно размещённого усилителя, wand/amulet; без focus нет изменений, с ним node−1 compound/item+100cV подходящего primal.

### R07 — P2: неполная последняя единица не заряжается

Место: [TileWandPedestal.java:118](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/common/tiles/TileWandPedestal.java:118), также142. **Статически подтверждено**, root сверил oracle admission/addVis.

При room=1…99cV условие `current+100 > capacity` навсегда пропускает аспект. Например greatwood/gold wand 4950/5000 не дозарядится до5000. Такие остатки штатно появляются после фокусов с ценой в centivis. Цепочка — тот же зарегистрированный ticker R01.

Оригинал `ItemWandCasting.getAspectsWithRoom:219–225` допускает любое current<max; `addVis:340–349` ограничивает credit ёмкостью, расходуя целую единицу узла. Исправление: admission current<capacity и credit=min(room,100), с исходным whole debit/округлением. Тест: capacity−1/−50/−99/−100/full для wand/amulet, точный баланс. Это отдельное условие: исправление единиц R01 его не устраняет.

### R08 — P2: инвентарь пьедестала не синхронизируется при обычном взаимодействии

Место: [BlockWandPedestal.java:90](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/common/blocks/BlockWandPedestal.java:90), 90–103; `TileThaumcraftInventory:71–85`; `TileWandPedestal:91–93`. **Статически подтверждено**, независимая проверка network/magic/root и bytecode MC.

Цепочка: server use → removeItemNoUpdate/setItem → setChanged. Ни tile, ни его базовые классы не превращают setChanged в S2C update. MC `BlockEntity.setChanged` помечает сохранение и comparator neighbours, не вызывает sendBlockUpdated. Renderer читает клиентский `tile.getItem(0):42`.

После вставки без доступного узла клиент не получает предмет; после изъятия пустой slot исключает даже периодическую отправку, которая требует `somethingChanged && nodes nonempty && stack nonempty`. Возможны невидимый установленный жезл/оставшийся визуальный жезл до нового chunk snapshot. Серверное владение не теряется. Оригинал `TileWandPedestal:77,110` вызывал markBlockForUpdate при изменении inventory.

Исправление: явный update при успешной вставке/изъятии, независимо от зарядки. Тест: два клиента наблюдают реальную вставку/изъятие без узлов; проверить клиентский BE slot после wire update, точный серверный inventory и отсутствие дубликатов. Прямой тест сохранения tile этот пробел не обнаружит.

### R09 — P2: аксессуары пропадают из клиентского snapshot после смены измерения

Место: [PlayerAccessories.java:27](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/common/lib/baubles/PlayerAccessories.java:27), 27–38; `AccessoryNetworking:40–49`; `PlayerAccessoriesMixin:17–20`. **Статически подтверждено**, независимо network/client; проверен actual MC respawn bytecode и Architectury events.

Условие: надет неизменяющийся Focus Pouch, получен login sync, переход измерения без смерти. MC `ClientPacketListener.handleRespawn` создаёт новый LocalPlayer; отдельный accessory attachment новый и пустой. Sync зарегистрирован на JOIN/RESPAWN, не CHANGE_DIMENSION. Серверный Player остаётся тем же; LAST считает содержимое уже отправленным, `syncChanged` молчит.

В результате `FocusSelectionSources:37–38` не видит надетый pouch до полного sync/переэкипировки/соответствующего меню. Предметы на сервере сохранены: это клиентская рассинхронизация. Исправление: безусловный accessory snapshot после dimension transition либо корректный client holder transfer с серверным resync. Тест: реальный portal/смена ServerLevel, неизменяющийся pouch с focus, последующий выбор; отдельно обратный переход, смерть и relog. Проверка только JOIN/RESPAWN недостаточна.

### R10 — P2: программные рецепты API не исполняются машинами

Место: [ThaumcraftApi.java:263](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/api/ThaumcraftApi.java:263), 263–294,317–320; `ArcaneCrafting:42–48`, `CrucibleCrafting:28–44`, `TileInfusionMatrix:144–176`. **Статически подтверждено**, API/magic независимо.

Условие: аддон использует addArcane/addShapeless/addCrucible/addInfusion/addInfusionEnchantment; соответствующего datapack-рецепта нет. Методы добавляют только в private craftingRecipes. Getters возвращают регистрацию, но реальные menu/crucible/matrix выбирают только из level RecipeManager. Все modern production callers getCraftingRecipes находятся в query API; NativeRecipes.install даёт обратные read views, не bridge исполнения.

Оригинал ThaumcraftCraftingManager выбирает из API списка (например oracle:163–168). Фактически игрок не может исполнить зарегистрированный рецепт; катализатор тигля может затем раствориться обычным образом. Штатные JSON-рецепты этим замечанием не объявляются сломанными.

Исправление: единый registry/bridge или проверяемый fallback с определёнными order/identity/reload/virtual callbacks. Тест: каждый публичный add* регистрирует уникальный рецепт и исполняется реальной машиной с research gate, точным output/debit и reload. Наличие в getCraftingRecipes не достаточно.

### R11 — P2: теряются обязательные компоненты arcane-ингредиента

Место: [ArcaneIngredientMatching.java:10](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/common/crafting/ArcaneIngredientMatching.java:10), 10–20; `ArcaneShapedRecipe:94–100`, `ArcaneShapelessRecipe:87–90`, `api/crafting/ArcaneRecipes:54–57`. **Воспроизведено на точном production helper**, API-агент; root независимо сверил активный код и оригинал. Меню и полный игровой runtime мода не запускались; helper исполнялся в JVM с фактическим Minecraft classpath.

Цепочка: Java ItemStack ingredient с обязательным CUSTOM_NAME → Ingredient.of(stack) → shaped/shapeless native matcher → helper. MC Ingredient.test проверяет item identity; helper добавляет только damage. Named-required STONE принимает plain STONE: probe вывел `named-required/plain-offered=true`; matching named control=true, wrong-item refused=true.

Оригинальные Shaped:175–179/Shapeless:120–124 при required.hasTagCompound требуют crafting tag equality; plain required при этом разрешает дополнительные данные input. Последствие — обход declared ingredient predicate в Java/addon recipes, независимо от исправленной ранее проверки wear/damage. R10 сейчас дополнительно блокирует обычную API-регистрацию; дефект наблюдается уже в matcher.

Исправление: сохранять явный required component patch/predicate, различать default components и явно заданные данные; строгое сравнение всех components нарушит допустимые plain→named случаи. Тест: shaped/shapeless positive/missing/wrong required data, plain requirement+extra data, сохранность matrix/vis, codec/reload. Probe: `.tmp/deep-review/ArcaneIngredientProbe.java`, compile/runtime exit0; использованы actual mapped MC1.21.1 и точные библиотеки Mojang manifest, без source stubs.

### R12 — P2: protected comparator enchantment infusion не вызывается

Место: [InfusionEnchantmentRecipe.java:83](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/api/crafting/InfusionEnchantmentRecipe.java:83), 83–91; native matcher120–138. **Статически подтверждено**, API/root.

Условие: подкласс API переопределяет protected areItemStacksEqual и напрямую вызывает matches на иначе допустимом recipe fixture. `matches` делегирует независимому nativeRecipe, virtual comparator не вызывается. Oracle matches:73 вызывает `this.areItemStacksEqual`, метод protected:84; прямые original-JAR access flags подтверждены.

Фактически ограничения/допуски аддона молча игнорируются. Это callable API defect даже без прохождения R10. Исправление: virtual callback участвует в matching либо передаётся native predicate adapter. Тест: реальные enchantment/level и допустимый center, subclass comparator=false со счётчиком должен отвергать; declared alternate должен изменять допуск. Просто наличие protected метода в классе не проверяет dispatch.

### R13 — P2: crafting event получает состояние после оплаты

Место: [ContainerArcaneWorkbench.java:124](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/common/container/ContainerArcaneWorkbench.java:124), 124–146; `TileArcaneWorkbench:69–75`. **Статически подтверждено**, API/magic независимо.

Цепочка настоящего result click/quickMove → takeResult → applyCraft:133 расходует matrix/vis → remainders134–143 → CRAFT_ITEM:146 с тем же backing contents. При count1 listener видит пустые клетки/containers и уже списанный vis, а не произведшие результат ингредиенты. Snapshot match.input существует, но передаётся только awardUsedRecipes:148.

Оригинал SlotCraftingArcaneWorkbench:41 вызывает event до debit43–46/consumption48–63. Addon accounting и ingredient-dependent callbacks получают неверные данные. Собственный Warp handler использует output, его поломка не утверждается. Исправление: сохранить атомарную оплату, передать event совместимый pre-craft snapshot; не переносить произвольный callback перед авторизацией без reentrancy защиты. Тест: реальный menu craft count1 с remainder, listener наблюдает исходные count/components/wand ровно1 раз, physical inventory отдельно отражает debit.

### R14 — P2: CastingWandHud отсутствует в NeoForge callback

Место: [ThaumcraftNeoForgeClient.java:215](/workspace/Thaumcraft-1.21.1/neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java:215), 215–220; Fabric counterpart66–71. **Статически подтверждено**, client/root.

Цепочка NeoForge bootstrap → register:73 → actual RenderGuiEvent.Post → onRenderGui. Callback вызывает остальные HUD, но не CastingWandHud. Единственный modern production call — Fabric:67; общего компенсирующего вызова нет. При показанном HUD и wand в руке на NeoForge весь dial отсутствует, вопреки TC4 ClientTickEventsFML:397–485.

Исправление: общий путь регистрации либо один соответствующий NeoForge вызов за кадр. Тест после разрешения NeoForge: фактический loader callback с wand рисует dial; отдельно Fabric. Прямой unit-вызов render не ловит пропущенную регистрацию. Наличие обоих HUD callbacks, отмеченное старым LOADER_QUALITY, не доказывает равенство их состава.

### R15 — P2: неполный общий wand HUD

Место: [CastingWandHud.java:64](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/client/lib/CastingWandHud.java:64), 64–79. **Статически подтверждено**, client/root против оригинала.

На Fabric реальный HudRenderCallback доходит до render, получает focus/cost, рисует полосы/стрелки и заканчивается. Shift показывает лишь amount/100. Нет focus icon, cooldown readout, выбранного Trade block/material readout и числовой цены с consumption modifier. Oracle ClientTickEventsFML:473–478,486–509 все эти ветки выполняет.

Последствие: игрок не видит установленный focus/выбранный материал, цену и время ожидания. Серверные данные/каст существуют; это одна незавершённая часть HUD с несколькими симптомами. Исправление: восстановить секции от фактических FocusData/Trade selection/cooldown и общего pricing contract. Регрессия: физический клиент, разные/снятый focus, Trade selected block, ненулевая/нулевая cooldown, Shift с discount и без. После R14 общее исправление нужно обоим загрузчикам.

### R16 — P2: Liquid Death использует неправильный damage type

Место: [BlockFluidDeath.java:150](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/common/blocks/BlockFluidDeath.java:150), 150–153; правильная фабрика `api/damagesource/DamageSourceThaumcraft:40`. **Статически подтверждено**, world/root.

Цепочка native entityInside → vanilla magic → MagicWard.kind:38–41 / EntityEldritchGuardian.hurt:80. Guardian делит damage пополам, magic wards защищают. TC4 BlockFluidDeath:57 вызывает dissolve; original DamageSourceThaumcraft:22 содержит только bypass armor, в отличие от magic-marked taint:19. В порту правильный dissolve registry type и bypass tag уже есть.

Ожидаются dissolve hooks/защиты/сообщение о смерти, фактически magic. Величина квантового damage здесь не оспаривается. Исправление: DamageSourceThaumcraft.dissolve(level). Тест: настоящий entityInside с registered damage source, отсутствие magic tag, Guardian и MagicWard controls; не только статическая damage(level) арифметика.

### R17 — P3: offhand wand остаётся без HUD

Место: [CastingWandHud.java:27](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/client/lib/CastingWandHud.java:27), 27–28; `ThaumcraftFocusClient:106–111`, `ItemWandCasting:267–290`. **Статически подтверждено**, client/root.

При main hand без wand и wand в offhand input явно выбирает offhand, use передаёт actual hand в WandFocusActions:63–69. HUD читает только main hand и выходит. Это несогласованность заявленной двухручной адаптации, а не прямое нарушение одноручного TC4.

Исправление: общий hand selection с main-first политикой и кэшем vis по действительному источнику/руке. Тест: main empty/ordinary + offhand wand, две разные wand, смена рук; на NeoForge сначала устранить R14.

### R18 — P3: FocusTrade игнорирует изменяемую дальность

Место: [ItemFocusTrade.java:74](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/common/items/wands/foci/ItemFocusTrade.java:74), также90. **Статически подтверждено**; игровой extended-reach запуск не выполнен.

Условие: нативный blockInteractionRange>=6, цель на6. Правый путь ItemWandCasting.use:273 использует native range, но focus заново трассирует фиксированные5 и получает MISS. Левый путь Fabric attack hook/NeoForgeInteractions тоже приходит в ray5. Oracle ItemFocusTrade:109–114 получает server getBlockReachDistance;5 — fallback для другой стороны.

Фактически допустимая расширенная reach перестаёт работать для remember/trade. Исправление: player.blockInteractionRange в обеих anchor rays; не ограничивать им всю spreading очередь. Тест: normal, increased6 и decreased3, remember/right/left paths. Достижимость FocusTrade и очереди подтверждена отдельно ниже; это не stub finding.

## Отложенные функции, мёртвый код и гипотезы

### D1 — P2 условно: кэш essentia mirror не обновляется у живого зеркала

Место: [EssentiaHandler.java:81](/workspace/Thaumcraft-1.21.1/common/src/main/java/thaumcraft/common/lib/events/EssentiaHandler.java:81), 81–103,106–119. **Статически подтверждено, dormant/manual/API**, не объявлено сломанным survival gameplay.

Цепочка при вручную созданной связанной паре **в одном измерении**, исключающей R05: TileInfusionMatrix → EssentiaHandler.drainEssentia → EssentiaTransfers.consumeForInfusion/consumeFromMirror → TileMirrorEssentia.takeFromContainer → drainEssentiaForMirror. Если кэш источников непустой, затем источники удалены/опустели и появилась новая банка в другой позиции, drain возвращает false, но не удаляет mirrorSources и не ставит backoff. mirrorSources возвращает старый список бесконечно до retirement endpoint. Оригинальный EssentiaHandler:57–59 удаляет sources и задаёт retry5000ms. Исправление: инвалидировать неуспешный scan и повторять после bounded backoff, сохраняя ownership и skipMirror. Тест: реально созданный endpoint+банка → успешный drain → удалить источник → новый источник → после backoff ровно1 debit. Это отдельно от исправленной ранее очистки удалённого зеркала.

### D2 — незавершённая survival-доступность essentia mirror

`ResearchRecipeCoverage:600–616` прямо оставляет MirrorEssentia deferred. Единственный `ThaumcraftEldritchBlocks.MIRROR_ITEM:387` — ItemMirrorBlock; default BlockMirror ESSENTIA=false, размещение/связывание item работают с TileMirror. Отдельного исполнимого essentia-mirror recipe/item path не найдено. Значит наличие TileMirrorEssentia и BE registration не доказывает получаемую игроком функцию. Это **уже документированная незавершённость**, не новая регрессия. Завершение требует native item/placement/binding/recipe и исследования; тест должен пройти получение/размещение/связывание/реальный перенос, не setblock-only.

### D3 — обязательные кандидаты, которые нельзя выдавать за активные ошибки

- **SlotCraftingArcaneWorkbench недостижим в штатном меню.** Registry → TileArcaneWorkbench.createMenu:115 → ContainerArcaneWorkbench → anonymous Slot:50–56. Modern production не создаёт SlotCraftingArcaneWorkbench. Его onTake/debit нельзя использовать как доказательство live-дюпа. Отметки ledger/старые legacy tests не заменяют caller. Активная транзакция исследована отдельно в R13.
- **Arcane checkItemEquals — мёртвые современные extension methods.** Shaped.matches:144–146/Shapeless matches делегируют native recipe. Но в оригинальном JAR оба checkItemEquals **private (access flags0x0002)**, не protected; утверждение `2026-10-23-agent-coremisc.md:158–161` исправлено этим ревью. Следовательно это не утрата исходного override-контракта. Реальная потеря required tags — R11; настоящий protected hook — R12, flags0x0004.
- **Пассивная зарядка посоха достижима и соответствует oracle.** ItemWandCasting.inventoryTick:132–139 → WandRodPassive.tick; elemental staff100cV/200ticks, primal100cV/50ticks в один случайный низкий primal при amount<capacity/10. StaffRodsRuntimeChecks:222–258 содержит настоящие player ticks, но здесь заново не запускался. Неактивный похожий helper не доказывает отсутствие recharge. Зарядка посоха пьедесталом повреждена R01/R07.
- **FocusTrade не stub.** ThaumcraftFoci registration → WandFocusActions override; Thaumcraft.init:156 → TradeSwaps.init:52 → SERVER_LEVEL_PRE. Left-click/protection hooks есть в обоих загрузчиках. Утверждение «очередь не вызывается» отвергнуто; все fairness/reentry/performance сценарии не проверены.
- **Обычные границы VIS-полосы корректны.** CastingWandHud:57 совпадает с oracle:441: int(30*amount/max). Все штатные capacity>0, нормальные amount0…max дают0…30. Record допускает искусственный overcap non-sceptre до1.5x; обычный игровой путь к нему не установлен. Отсутствие clamp — кандидат hardening, не подтверждённая ошибка. Клиентский кадр с границами не осматривался.
- ObjectAspectResolver дополнительно сравнивает identity manager.getRecipes; blanket «любой reload даёт stale cache» не подтверждён. Runic maximum/lastCharge quirk совпадает с TC4, не новая регрессия. WeakHashMap без clear не доказывает утечку без сильной обратной ссылки.
- Watcher — retained extra без TC4 survival spawn; его self-target/goal/cache concerns не повышены до пропущенной штатной функции. Перевёрнутая высота неполного death-fluid и hilltop sea-level predicate оставлены визуальным кандидатом/прежним reasoned-only замечанием: нового игрового подтверждения нет.

## Реально выполненные проверки

| Проверка | Результат именно этого прохода |
|---|---|
| Git status/branch/HEAD, live remote main | Чисто до ревью; work и main совпадают на указанном полном SHA |
| Java21 / Gradle8.12.1: `GRADLE_USER_HOME=/workspace/Thaumcraft-1.21.1/.gradle21 ./gradlew --no-daemon --offline :common:test :fabric:build --dry-run` | Exit1 **до конфигурации**: FileLockContentionHandler → Could not determine a usable wildcard IP for this machine; sandbox запрещает необходимые локальные сокеты |
| Повтор с sandboxed additional network permission | Запрос не дал выполненного Gradle запуска; ожидающее действие прекращено при завершении проверки. Tool сообщил aborted after405.8s. Это не ошибка компиляции проекта и не успешная сборка |
| Common JUnit / Fabric build / dedicated GameTest | **Свежего запуска нет** из-за указанного blocker; старые build outputs/зелёные отчёты не засчитаны |
| NeoForge build/test/runtime | **Не запускались**, запрещены AGENTS.md |
| `python3 -m unittest discover -s scripts/porting -p 'test_*.py' -v` | Exit1:111 тестов,103 pass,2 errors,6 skipped,9.372s |
| `TC4_ORACLE=/workspace/work/thaumcraft-4.2.3.5-oracle python3 -m unittest discover -s scripts/porting -p test_aspect_research_audit.py -v` | Exit0:6/6 pass,2.288s. Закрывает первоначальные skips из-за старого default path; **итог111 уникальных:109 pass,2 errors** |
| `python3 scripts/porting/convert_assets.py --verify` | Exit1: Resource supplier changed для GuiResearchBrowser.java и GuiResearchRecipe.java |
| `python3 scripts/porting/update_scope_ledger.py --check` | Exit0,1044 rows,stale_review_fingerprints0. Не доказательство полноты gameplay |
| Required-component Java probe | Текущий production helper с actual MC1.21.1: compile/runtime exit0; неверное принятие plain STONE подтверждено, positive/negative controls есть |
| Общий API Java probe на старых common/build/classes | Не скомпилировался: отсутствующие API classes; **не pass**, stale alpha.1 outputs не признаны текущим target |
| Проверка буквальных client png/obj ссылок |158 ссылок/126 уникальных,0 missing; ограниченный existence scan, не весь resource graph/динамические пути |
| Реальный клиент, два клиента, visual/driver acceptance | **Не выполнялись**; seen/pass не заявляются |

Две Python-ошибки — `test_all_audited_expansions_exist_and_atlas_sprites_are_stitched` и `test_every_generated_texture_parent_blockstate_and_sound_resolves`. Общая причина: [composed_resources.py:99](/workspace/Thaumcraft-1.21.1/scripts/porting/composed_resources.py:99) сравнивает reviewed source contracts с изменёнными GUI; manifest `scripts/porting/composed-resource-paths.json` требует новой проверки expansions. **Не доказаны отсутствующие assets**, и не следует просто переписать hashes ради green. Нужна сверка фактических динамических запросов, bounds и atlas inclusion; затем обоснованное обновление reviewed contract и проверка, что удаление обязательного ресурса всё ещё вызывает отказ. Ошибка проверки воспроизведена; это отдельный verification gap, не девятнадцатый активный gameplay-дефект.

Логи: `.tmp/deep-review/python-tests.log`, `python-oracle-tests.log`, `assets-verify.log`, `ledger-check.log`, `gradle-dry-run.log`, `ingredient-probe-compile.log`, `ingredient-probe.log`. Неудачная первая сборка Java probe и несовместимая первоначальная смесь cached библиотек не скрыты; успешный helper использовал исправленный выбор точных Mojang dependency versions, без изменений production.

## Качество тестов и пробелы

В source inventory409 common JUnit Java-файлов и252 common+Fabric gameTest Java-файла. Это количество файлов, не выполненных тестов и не процент покрытия. Прочитаны целевые тесты API/recipe/menu, StaffRodsRuntimeChecks, HUD tests и соответствующие прошлые отчёты.

Повторяется конкретный пробел: тестируетcя helper или наличие API-объекта, но не вход в него через реальную регистрацию. CraftingApiTest проверяет add→list/getter, а не машину; mirror pure helper не ловит неправильный dimension аргумент tile; staff pure math не заменяет native tick, хотя meaningful staff runtime fixture уже существует. HUD unit/source tests не проверяют состав обоих loader callbacks и все секции render. Исследование мёртвого Slot не даёт покрытия active anonymous Slot. Native game tests без физического клиента не доказывают GUI/sprite/driver/multiplayer synchronization.

Приоритет нового покрытия: conservation тест R01/R02, singleton-placement и crossdim mirrors раздельно, API registration→machine, затем client dimension/pedestal sync и реальные HUD/Spa кадры. Production tests не добавлялись: задача — ревью; все необходимые тесты перечислены при находках.

## Независимость и устранение дубликатов

Шесть независимых субагентов покрыли десять направлений: client+loaders, API+crafting, network+lifecycle, world+entities, magic+machines, tests+coverage. Для старых критических registration findings выполнена отдельная вторая волна.

| Находки | Независимая перепроверка |
|---|---|
| R01,R02,R03,R06,R07,R08,R11,R12,R14–R17 | Root заново прочитал production/oracle или dependency contract, отдельно от первого агента |
| R04 | Magic + network; root отдельно прочитал actual BlockItem.consume/ItemStack.getComponents bytecode |
| R05 | Network + world; root перечитал reciprocal state/callers |
| R09 | Network + client; root прочитал sync owner/events |
| R10,R13 | API + magic; root прочитал active menu/API lookups |
| R18 | Статическая проверка magic; ограниченный reach-сценарий явно указан, игрового воспроизведения нет |

Симптомы общего урезанного HUD объединены R15. Potency/duplicate launch кандидаты не подменяют настоящие call chains. Потеря compound100 перекрывается R01 по величине debit, поэтому R06 учитывает самостоятельное отсутствие conversion. Две ошибки зеркал разделены по корню и условиям; одна может маскировать другую. D1 и D2 вынесены из активного survival списка.

## Сопоставление с прежними отчётами

Основной актуальный предшественник — `docs/audit/REPORT.md` и тематические findings от23.10, а не только ранний `2026-10-19-full-project-audit.md` и legacy-анализ15.10. Исторические числа/зелёные проверки не перенесены на этот запуск.

| Прежнее утверждение | Результат перепроверки |
|---|---|
| Два Critical duplicate nugget ID в LOADER_QUALITY R1/R2 | **Уже исправлено.** Content:162–165 единственный behavioral ItemNugget owner; Wands:51,55 aliases, повторная creative публикация удалена. Scan271 literal declarations/271 IDs,0 duplicates; не исчерпывающий computed-ID аудит |
| NeoForge нет attrs/Bore/Golem bridges (19.10) | **Уже исправлено** текущим ThaumcraftNeoForge:29–48; runtime parity этим не доказана |
| Unrestricted inhabited zombie placement | **Уже исправлено** NeoForgeSpawnPlacements:67–73:ON_GROUND/checkInhabitedSpawnRules |
| Spa bridge отсутствует | **Остаётся открытым**, R03 |
| NETWORK N1/N2 cleanup, N3 dead/spectator research editor | **Исправлено**, actual hooks/reset/viewer checks перечитаны; не переоткрываются |
| Golem goal bulk rebuild | **Исправлено** stopAndRemoveGoals перед rebuild; прежний red/green прочитан, здесь заново не запускался |
| Arcane damage mismatch CRAFT-01/02 | Исправление осталось; R11 касается required data, прежним bounded аудитом не покрытых |
| Arcane checkItemEquals оригинально protected (coremisc D11) | **Отвергнуто** original classfile:private; R12 действительно protected |
| Все HUD callbacks присутствуют | Callback есть, но состав различен: новый R14; поздний CastingWandHud не входил в прежний client baseline |
| Classic death finite flow реализован | **Опровергнуто** реальным Forge finite parent; новый R02 |
| Essentia mirror survival deferred, addon rod/cap integration incomplete | Подтверждённая прежняя незавершённость, не новая completed-функция |

R01/R02/R04–R18 не описаны как подтверждённые дефекты в прочитанных соответствующих предшествующих отчётах. Это сравнение выбранных актуальных отчётов, не заявление о поиске каждого упоминания во всей документации.

## Матрица реально проверенного покрытия

| Направление | Реально проверено | Метод / предел |
|---|---|---|
| 1. Fabric/NeoForge registration | Entrypoints/metadata, common bootstrap, HUD/render callbacks, attrs/platform bridges, attack/use hooks, spawn placement, nugget owners | Static+dependency sources; NeoForge не собирался, все851 declarations заново не раскрывались |
| 2. Клиент/HUD/GUI/resources | CastingWandHud целиком и TC4 equivalent, hand paths; GuiSpa, часть workbench/config GUI;158 literal resources | Нет физического кадра, driver compile, полного GUI/renderer/dynamic path corpus |
| 3. API/аддоны | Add/query/install и пять recipe types, NativeRecipes, ArcaneRecipes, hook dispatch; rod/cap boundaries | Original flags+caller trace+helper probe; не все API types/реальные сторонние аддоны |
| 4. Крафт/контейнеры/items | Active ArcaneWorkbench clicks/quickMove/debit/remainders/events, shaped/shapeless, recipe manager границы crucible/matrix | Полная целевая транзакция статически, не все меню/automation/reentrant addon callbacks |
| 5. Сеть/sync |12 modern receiver classes; table session/revision, focus/focal/accessory paths; Knowledge/Runic S2C owners, dimension lifecycle | Нет wire replay/fuzz, свежего handshake или двух клиентов; не каждый payload codec прочитан |
| 6. Сохранения/lifecycle/cache | Knowledge/accessory attachments, Runic save, recipe index invalidation, ChunkRegeneration/GrowthLamp/TradeSwaps ownership, mirrors | Нет нового дискового save/restart/recovery теста, полного Bore/DeferredItemDrops journal обзора |
| 7. Мир/жидкости/эссенция | Death placement/flow/contact целиком с Forge parent; Pure выборочно; worldgen registration, hilltop/ring portions; essentia transfer/cache entrypoints | Нет terrain/seed matrix, полного labyrinth generator/1200+строк essentia transactions |
| 8. Сущности/AI/interactions | Wisp/BrainyZombie/Watcher, shared cultist/Pech goals выборочно, Guardian damage, golem rebuild, spawn/orphan reachability | Не все mobs/goals/cargo, естественный spawn/save-reload заново не прогонялся |
| 9. Исследования/магия/машины | Staff native tick, FocusTrade queue/hooks, pedestal/Node units, research purchase/note authority, Matrix/Thaumatorium selection/output portions |6 oracle data tests выполнены; не весь survival graph/все фокусы/каждая машина/ritual lifecycle |
| 10. Тесты | Целевые behavioral/source tests,111 Python unique, gates/старая документация и их границы | Java suite/GameTest свежих нет; line/branch coverage не измерялось |

Непроверенные области включают физический клиент, весь визуальный каталог, полноценный multiplayer/latency, production-JAR загрузку, NeoForge runtime, холодный JVM restart/все повреждённые сохранения, все генераторы/seed/dimension комбинации, весь AI и произвольные аддоны. Fresh1.21.1 worlds — область проекта; миграция старых миров не проверялась и не предлагается. Отсутствие новых ошибок в выборке не доказывает корректность остальных областей.

## Созданные файлы и итоговое состояние

Постоянный файл: `docs/analysis/2026-10-25-deep-review-ru.md` (этот отчёт).

В ignored `.tmp/deep-review/` сохранены шесть независимых отчётов (`client-loaders.md`, `api-crafting.md`, `network-lifecycle.md`, `world-entities.md`, `magic-machines.md`, `checks-coverage.md`), Java probe sources/classes/classpaths, resource scan, original comparator access flags, dependency javap и перечисленные выше логи. Полный список — `created-files.txt` в том же каталоге. В `/tmp` также созданы копия Forge parent и первоначальные javap; приватный оригинал не копировался в репозиторий.

Фактический `git status --short` после создания документа:

```text
?? docs/analysis/2026-10-25-deep-review-ru.md
```

Фактический финальный status и перечень созданного дополнительно записываются в `.tmp/deep-review/final-git-status.txt` и `created-files.txt`. Изменений production/tests/checksum нет; commit/push/PR не выполнялись.
