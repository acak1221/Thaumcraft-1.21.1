# 26.10.2026 — Жезлы, посохи, наконечники и фокусы аддонов через API

## Задача

Аддоны TC4 1.7.10 (Forbidden Magic, Thaumic Tinkerer, Thaumic Bases и др.) регистрируют свои
стержни, посохи и наконечники конструкторами `WandRod` / `StaffRod` / `WandCap` и свои фокусы
наследниками `ItemFocusBasic`. До этой правки порт хранил эти объекты в `WandRod.rods` /
`WandCap.caps`, но игра их не читала: каталог `WandMaterials` был закрытым (18 стержней,
5 наконечников, `rod(tag)` бросал исключение), а `FocusData.isSupportedFocus` пропускал только
десять родных фокусов. Нужно, чтобы зарегистрированные аддоном части работали в игре
(верстак, ёмкость, скидка, `onUpdate`, имена), а жезл с ними рисовался так же, как родной.

## Что в оригинале (javap над `Thaumcraft-1.7.10-4.2.3.5.jar`, вне репозитория)

| Место | Поведение |
|---|---|
| `ItemWandCasting.getRod/getCap:455-458, 508-511` | часть берётся из `WandRod.rods.get(nbt "rod")` / `WandCap.caps.get(nbt "cap")`; родные и аддонные части — один реестр |
| `ConfigItems:292-334` | родные части регистрируются тем же конструктором; `WAND_ROD_BLAZE.setGlowing(true)` — `ConfigItems:305`; стихийные стержни получают `WandRodPrimalOnUpdate` |
| `ItemWandCasting.getMaxVis:91` | `getRod().getCapacity() * (sceptre ? 150 : 100)` — ёмкость в API в **целых vis**, жезл хранит сотые |
| `ArcaneWandRecipe.getCraftingResult:27-68` | обход `WandCap.caps`, затем `WandRod.rods` в порядке реестра, первая часть с подходящим предметом; дерево+железо исключено (:59); метаданные = `cc * cr` (:60-61) |
| `ArcaneWandRecipe.getAspects:74-111` | `cc * cr` каждого первичного аспекта |
| `ArcaneWandRecipe.checkMatch:138-161` | ячейка годится, если исследована **любая** часть с этим предметом (`getResearch()`) |
| `ArcaneSceptreRecipe:27-73, 79-120, 125-143` | то же для скипетра: три наконечника, первородный амулет, `SCEPTRE`, цена `(int) (cc * cr * 1.5F)`, NBT `sceptre` |
| `ItemWandCasting.getConsumptionModifier:243-261` | особая скидка наконечника для аспектов из его списка, иначе базовая; минус снаряжение, минус «бережливость» фокуса вне крафта, минус 0,1 у скипетра, не ниже 0,1 |
| `ItemWandCasting.onUpdate:350-358` | на сервере `getRod().getOnUpdate().onUpdate(stack, player)` каждый тик в инвентаре |
| `ItemWandCasting.isStaff:462-466`, `hasRunes:477-481`, `setRod:485-505` | посох — `rod instanceof StaffRod`, руны — `StaffRod.hasRunes`, у посоха модификатор урона +6 |
| `ItemWandCasting.getItemStackDisplayName:134-143` | `item.Wand.name` = `%CAP %ROD %OBJ`, ключи `item.Wand.<cap>.cap`, `item.Wand.<rod без _staff>.rod`, `item.Wand.{staff,sceptre,wand}.obj` |
| `ItemWandCasting.getVis/getAllVis/getAspectsWithRoom/addVis/addRealVis/consumeVis/consumeAllVis/consumeAllVisCrafting/getFocus*:200-344` | публичные методы, которыми пользовались аддоны (единицы см. ниже) |
| `ModelWand.render:69-204` | текстура стержня — `getRod().getTexture()` (:75), свечение — световая карта `(int) (200 + sin(ticks) * 5 + 5)` (:81-86), текстура наконечника — `getCap().getTexture()` (:101), украшение фокуса — два скрещённых `ItemRenderer.renderItemIn2D` (:136-160), слой глубины (:165-179), куб фокуса с цветом (:196-204) |

## Что сделано

### Мост реестров и каталога

- `WandRod` / `WandCap` при загрузке класса регистрируют **виды родных частей**: 18 стержней (девять
  посохов — `StaffRod`, у первородного руны), 5 наконечников (у медного особые аспекты ORDO/PERDITIO),
  свечение огненного стержня, `WandRodPrimalOnUpdate` у стихийных стержней (теперь он
  `implements IWandRodOnUpdate`). `WandRod.rods.get("greatwood").getCapacity() == 50`, как в оригинале.
  Предмет вида разрешается лениво (на NeoForge предметы регистрируются позже).
- `WandMaterials.rod/cap(tag)`: родной тег → родная константа (тот же объект, что и раньше), иначе запись
  реестра, прочитанная **в момент вызова** (`Rod.addon()` / `Cap.addon()` ссылаются на живой объект
  аддона). Поэтому порядок инициализации модов неважен, ничего не замораживается при старте
  Thaumcraft, а поздние вызовы сеттеров аддона видны. `allRods()` / `allCaps()` — родные, затем аддонные.
- Аддонный стержень: ёмкость `getCapacity() * 100` сотых, стоимость, исследование (`getResearch()`,
  переопределение уважается), `isStaff` = `instanceof StaffRod`, руны, `onUpdate`, текстура, свечение.
  Аддонный наконечник: базовая и особая скидка (`modifierFor`), стоимость, исследование, текстура.
- Компонент жезла: тег части проверяется по мосту; сумма каждого аспекта ограничена ёмкостью
  скипетра **этого** стержня (для родных стержней это прежние ≤ 37500 — приём данных не изменился).
  Компонент стоимости сборки — от 0 до 32767 (метаданные оригинала — short), раньше 0..432.

### Верстак

`ArcaneWandRecipe` и `ArcaneSceptreRecipe` ищут части через `WandPartItems` (родные, затем аддонные
по порядку реестра), сравнивают предметы прежними правилами (`checkItemEquals` оригинала у жезла,
только предмет у скипетра), проверяют исследование по правилу `checkMatch` (любая часть с этим
предметом), берут цену `cr * cc` / `(int) (cr * cc * 1.5F)`. `displayInput` и EMI/JEI
(`RecipeViews.constructs`) показывают и аддонные сочетания.

### Игра

- `ItemWandCasting.inventoryTick`: для аддонного стержня — его `IWandRodOnUpdate` (сервер, игрок);
  родные стержни по-прежнему пополняются через `WandRodPassive`, виды их хуки никогда не вызывают
  (иначе стихийный стержень пополнялся бы дважды).
- Посох из `StaffRod` аддона: длинная модель, +6 урона, не принимается в слот верстака, руны дают +1
  к силе фокуса.
- Имя и подсказка: ключи оригинала `item.Wand.<tag>.cap/.rod`; аддон кладёт их в свой `lang/*.json`.
  Подсказка и HUD берут ёмкость и скидки из моста без изменений.
- Фокусы аддонов: `FocusData.isSupportedFocus` и `FocusUpgradeData.supportedForFocus` принимают любой
  `ItemFocusBasic`, не являющийся родным фокусом (как `setFocus`/`changeFocus` оригинала). Такой фокус
  ставится на жезл, выбирается, разыгрывается через **свои** хуки (`onFocusRightClick` и т. д.), его
  улучшения проверяются его `getPossibleUpgradesByRank`. Остальные проверки (один предмет, без
  вложенного жезла/фокуса) прежние.

### Фасад `thaumcraft.api.wands.WandHelper`

Статические методы оригинального `ItemWandCasting`, которые звали аддоны, чтобы порту не нужен был
внутренний класс: `isWand`, `getRod/getCap` (объект API: вид родной части или часть аддона),
`setRod/setCap`, `isStaff/isSceptre/hasRunes`, `getFocus/getFocusItem/setFocus`, `getMaxVis`,
`getVis`, `getAllVis`, `getAspectsWithRoom`, `addVis` (целые vis, возвращает остаток в целых, как
оригинал), `addRealVis` (сотые), `getConsumptionModifier`, `consumeVis`, `consumeAllVis` (сотые),
`consumeAllVisCrafting` (целые), `getFocusPotency/Treasure/Frugal/Enlarge/Extend`. Модификатор
считает `WandVisDiscount` (тот же код, что у верстака и фокусов), списание — `FocusVis.canPay/debit`
(атомарно, только первичные). Методы с игроком при игроке на клиенте отвечают, но стек не меняют.

### Отрисовка

- Родной путь не тронут: 990 JSON-моделей с переопределениями по-прежнему выбираются для жезлов из
  родных частей. Все модели жезла (и раньше) имеют корень `minecraft:builtin/entity`; рисует
  `ThaumcraftGearModelsClient.renderWand`, зарегистрированный на Fabric (`BuiltinItemRendererRegistry`)
  и NeoForge (`IClientItemExtensions#getCustomRenderer`).
- Новое свойство `thaumcraft:wand_addon` (1, если стержень или наконечник аддонный) и последнее
  переопределение `wand.json` → `thaumcraft:item/wand_addon` (родитель `wand_shape`, без `display`,
  поэтому позы руки/GUI/земли те же, что у родного жезла — их задаёт рендерер). Генератор
  `scripts/porting/gen_wand_models.py` дописывает это переопределение; перегенерированный `wand.json`
  отличается ровно одной записью.
- `renderWand`: текстуры стержня и наконечника — `WandRod/WandCap.getTexture()` аддона (null →
  `thaumcraft:textures/models/wand_rod_<tag>.png`, как у конструкторов оригинала); раскладка UV та же,
  что у `ModelWand`, поэтому PNG аддона 1.7.10 подходят без изменений; свечение стержня — световая
  карта оригинала (`ModelWand.Geometry.glowLight`).
- Фокус: цвет и слой глубины работали и раньше; добавлено украшение `getOrnament` — порт
  `renderItemIn2D` (передняя и задняя грани и полоски по пикселям, толщина 0,1), дважды, второй
  повёрнут на 90° вокруг Y, внутри поворота на 180° вокруг X, сдвиг (-0,25; -0,1; 0,0275), масштаб 0,5.
  Родные фокусы `getOrnament` не переопределяют, их отрисовка не изменилась. Родной жезл с аддонным
  фокусом выбирает прежнюю модель «прочий фокус» (тоже builtin) и рисует фокус по его хукам.

## Контракт для аддона (кратко)

```java
WandRod rod = new WandRod("infernal", 150 /* целые vis */, new ItemStack(MY_ROD), 12, new MyRodOnUpdate(),
        ResourceLocation.fromNamespaceAndPath("myaddon", "textures/models/wand_rod_infernal.png"));
WandCap cap = new WandCap("alchemical", 0.9F, List.of(Aspect.WATER), 0.8F, new ItemStack(MY_CAP), 7);
cap.setTexture(ResourceLocation.fromNamespaceAndPath("myaddon", "textures/models/wand_cap_alchemical.png"));
// lang/en_us.json аддона: "item.Wand.infernal.rod": "Infernal", "item.Wand.alchemical.cap": "Alchemical"
// исследования ROD_infernal и CAP_alchemical аддон регистрирует сам
// в IWandRodOnUpdate: WandHelper.addVis(stack, Aspect.FIRE, 1, true)
```

Регистрировать можно в любой момент инициализации, на любом загрузчике; реестры — обычные карты,
поэтому только во время инициализации. Жезл, чьей части больше нет в реестре, не загружается.

## Проверки

Новые тесты:

- JUnit `thaumcraft.wands.WandAddonPartsTest` (10): виды родных частей и их значения оригинала (целые
  vis, `StaffRod`, руны, свечение, `WandRodPrimalOnUpdate`, особые аспекты медного наконечника); родные
  поиски отдают те же константы; аддонный стержень/посох/наконечник разрешаются лениво и «вживую»
  (сеттеры, переопределённое исследование, снятие с регистрации); регистрация под родным тегом ничего не
  меняет; пределы; кодек компонента с аддонными частями и ёмкостью выше 37500; флаг модели;
  `WandHelper` на не-жезле ничего не меняет.
- JUnit `thaumcraft.client.renderers.item.WandAddonRenderingTest` (4): текстуры частей (родные прежние,
  аддонные свои, null → производное имя), значение свечения, украшение фокуса (число вершин двух
  `renderItemIn2D`, позиции и UV углов, поворот второго прохода, восстановление позы), родные фокусы
  без украшения рисуются как раньше.
- GameTest `WandApiRuntimeChecks` (3 сценария, тестовый «аддон» — четыре предмета, которые регистрирует
  только GameTest-мод; части API создаются уже на запущенном сервере): сборка жезла в настоящем верстаке
  настоящим игроком в режиме выживания (`TubeVariantsRuntimeChecks.Fixture`) с проверкой исследований,
  цены 8400 сотых, оплаты, ёмкости 150, скидки 0,8/0,9, имени, кодека; посох и скипетр из аддонных
  частей; родные цены рядом не изменились; `onUpdate` аддона на тике инвентаря и однократное пополнение
  родного обсидианового стержня; `WandHelper` на настоящем жезле (единицы, остатки, атомарная оплата,
  модификатор, «бережливость» только вне крафта, установка и розыгрыш аддонного фокуса, `setRod/setCap`).

| Проверка | Команда | Результат |
|---|---|---|
| Новые и затронутые JUnit | `./gradlew :common:test --no-daemon --tests 'thaumcraft.wands.WandAddonPartsTest' --tests 'thaumcraft.client.renderers.item.WandAddonRenderingTest' --tests 'thaumcraft.api.WandApiTest' --tests 'thaumcraft.wands.WandCraftCostTest' --tests 'thaumcraft.assets.WandModelAssetsTest' --tests 'thaumcraft.client.renderers.item.WandFocusRenderingTest'` | **35/35** |
| Весь `:common:test` | `./gradlew :common:test --no-daemon` | **2608**, 0 падений, 5 пропусков (прежние) |
| GameTest Fabric (новые + жезловые + фокусные) | `rm -rf fabric/run-gametest/world && ./gradlew :fabric:runGameTest --no-daemon -PgameTestFilter=wandApiAddonPartsAssembleInTheArcaneWorkbench,wandApiAddonRodOnUpdateRefillsTheCarriedWand,wandApiHelperPaysChargesAndMountsAddonFoci,wandAssemblyUsesMaterialsAndResearch,elementalWandRodsBrewAssembleAndRefillTheirOwnPrimal,staffRodsBrewAssembleStrikeAndRefill,voidMetalAndTheVoidCapBrewCraftAndDiscount,followupSceptreGrindstonePreservesCapacity,reviewWandHandSelection,focusUpgradeDataAndCasting,fireFocusSelectsPersistsAndPays,focusPouchUsesNativeMenuAndAtomicWandSelection,shockFocusPaysTargetsAndRespectsCooldown` | **13/13** |
| Обёртки NeoForge | `python3 scripts/porting/neoforge_gametests.py --write`, затем `python3 scripts/porting/neoforge_gametests.py` | совпадает с Fabric (466 обёрток) |
| Сборка | `./gradlew :fabric:build :neoforge:build --no-daemon`; `./gradlew :neoforge:compileGameTestJava --no-daemon` | пройдено |
| `scripts/porting/test_gametest_order.py` | `python3 -m unittest scripts/porting/test_gametest_order.py` | 3/3 |

Осмотр в клиенте (Fabric, Xvfb, временный незакоммиченный хук регистрировал «аддонные» стержень с
листом `wand_rod_blaze`, посох с листом `wand_rod_obsidian` и рунами, наконечник с листом
`wand_cap_silver` — этот лист не использует ни одна родная часть — и фокус с украшением
`focus_warding_orn`; хук удалён):

- [на земле и в хотбаре](images/2026-10-26-addon-wand-ground-hotbar.png): аддонный жезл с фокусом,
  аддонный посох с рунами, аддонный скипетр с кольцом рун, родной жезл с аддонным фокусом; в руке —
  родной стержень с аддонным наконечником. Текстуры аддона применены (нет «шахматки» отсутствующей
  текстуры), украшение — два скрещённых значка вокруг куба фокуса;
- [от первого лица](images/2026-10-26-addon-wand-first-person.png): жезл, посох, скипетр аддона и
  родной жезл. Имена — сырые ключи `item.Wand.vis_silver.cap item.Wand.vis_ember.rod Wand`, потому что
  хук не нёс файла языка (как в оригинале); у родного — «Gold Banded Greatwood Wand». «Шахматка» в
  центре HUD — значок самого предмета-фокуса хука, у которого нет модели предмета (в журнале
  `Unable to load model: thaumcraft:item/visual_addon_focus`), а не жезл;
- [от третьего лица](images/2026-10-26-addon-wand-third-person.png): аддонный жезл, родной с аддонным
  фокусом, родной без фокуса, аддонный посох — поза в руке одинаковая.

Не проверялось: GameTest и клиент NeoForge (по приоритету Fabric — только сборка и компиляция
GameTest-исходников), мультиплеер, заметность свечения стержня на глаз (днём не различимо; значение
проверено JUnit), поведение `WandHelper` с игроком на клиенте (в GameTest игрока клиента нет).

## Отклонения

1. **Родной тег всегда родной.** В оригинале повторная регистрация `new WandRod("greatwood", ...)`
   заменяла родной стержень; здесь игра читает родной каталог, а запись аддона под родным тегом
   игнорируется (и `WandHelper.getRod` отвечает видом родной части). Так же сеттеры родных видов
   (`WandRod.rods.get("silverwood").setCapacity(...)`) не меняют игру. Причина: требование не менять
   родные значения.
2. **Единицы в API — целые vis** (как в конструкторах оригинала). Раньше javadoc порта называл
   ёмкость в API сотыми, а родные виды отвечали 2500 для деревянного стержня; теперь 25, как в оригинале.
3. **Пределы.** Ёмкость аддонного стержня ограничена 0..143165 целых vis (ёмкость скипетра в сотых
   должна помещаться в `int`), стоимость части — не меньше нуля, стоимость сборки — не больше 32767
   (метаданные оригинала — short). Компонент стоимости сборки принимает 0..32767 вместо 0..432.
4. **Отрицательные количества** в `WandHelper.addVis/addRealVis/consumeVis/consumeAllVis` ничего не
   делают (в оригинале `addVis(-n)` опустошал жезл, `consumeVis(-n)` пополнял его).
5. **Клиентский игрок.** `consumeVis`, `consumeAllVis(..., doit=true)` при игроке на клиенте отвечают,
   как оригинал, но стек не меняют (оригинальный `consumeVis` списывал и на клиенте).
   `getFocusItem` без фокуса — пустой стек, а не null (в 1.21.1 стеки не бывают null).
6. **Фокусы аддонов** принимаются на жезл; прежний закрытый список отказывал всем, кроме десяти родных.
7. Свечение огненного стержня есть у его вида в API (как `ConfigItems:305`), но родной рендерер
   огненный жезл по-прежнему не подсвечивает — это расхождение было и раньше, правка его не трогает.
   Украшения родных фокусов (адская летучая мышь, обмен, охрана: `focus_*_orn.png`) порт тоже по-прежнему
   не рисует — родные фокусы не возвращают `getOrnament`; механизм отрисовки теперь есть.
