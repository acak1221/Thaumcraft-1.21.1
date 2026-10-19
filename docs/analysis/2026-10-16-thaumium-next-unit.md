# Следующий Fabric-контент: таумиевое снаряжение и наконечники

**Equipment принят: `equipment-fabric-validate2` / `equipment-fabric-client1`, толькоFabric.**
200JUnit/66Fabric/3563unchangedinputs/48Python; отдельныйclient scope equipment,
6exactPNG61–66 осмотрены. Длинная стараяцепочка не повторялась. Ни caps/essence/
auramsource, ни полныйпорт этим не закрыты. Контракт ниже отличает следующиеunit.

## Самостоятельно полезная первая часть: 9 предметов снаряжения

Original `ConfigRecipes:459–467`: обычный крафт, без vis/эссенции/XP и **без runtime
research gate**. В книге страницы относятся к THAUMIUM; это не блокировка crafting.
`I`=ingotThaumium, `S`=stickWood; каждый output1.

| Recipe key | Pattern | Входы |
|---|---|---|
| ThaumiumShovel | `I/S/S` | ingot1, stick2 |
| ThaumiumPick | `III/ S / S ` | ingot3, stick2 |
| ThaumiumAxe | `II/SI/S ` | ingot3, stick2 |
| ThaumiumHoe | `II/S /S ` | ingot2, stick2 |
| ThaumiumSword | `I/I/S` | ingot2, stick1 |
| ThaumiumHelm | `III/I I` | ingot5 |
| ThaumiumChest | `I I/III/III` | ingot8 |
| ThaumiumLegs | `III/I I/I I` | ingot7 |
| ThaumiumBoots | `I I/I I` | ingot4 |

Existing metallurgy: iron ingot+praecantatio4→1Thaumium, crucible gateTHAUMIUM.
Tools: harvestlevel3, durability400, efficiency7, materialdamagebonus2,
enchantability22; **Hoe enchantability5**, не22. Armor: multiplier25, protection
helmet/chest/legs/boots=2/6/5/2; durability275/400/375/325, enchantability25.
Все9uncommon, repairingredientThaumium. Нет innateselfrepair/visdiscount/shield/warp;
IRepairable — пригодность для ещё не перенесённого Repair enchantment.
Anchors: Api48/51, ConfigItems387–404, ItemThaumium*, ItemThaumiumArmor30–77.

**Важно для native1.21 combat:** materialbonus2 сам по себе не задаёт итоговый
attack modifier. Не брать современные vanillaaxe/sword constructor arguments,
которые могут изменить TC4damage. Read-onlybytecodefollowup подтвердил:

| Item | Legacy additive modifier | Nativehelper argument(плюсtierbonus2) |
|---|---|---|
| Sword | +6 | SwordItem.createAttributes(tier,4,speed) |
| Axe | +5 | DiggerItem.createAttributes(tier,3F,speed) |
| Pickaxe | +4 | DiggerItem.createAttributes(tier,2F,speed) |
| Shovel | +3 | DiggerItem.createAttributes(tier,1F,speed) |
| Hoe | отсутствует | лучшеневыдаватьdamageattribute; −2Fhelperдаёт0 |

Baseplayer1→nominal7/6/5/4/1, не сырые modifier values. Native constructors только
`(Tier,Properties)`: attributes передаются Properties.attributes, не появляются
сами. Speeds/cooldown1.9+/blocking/sweeping — отдельная forcednativeadaptation,
не TC4values и неfullcombatparity. VanillaSharpnessтеперь.5+.5L вместоold1.25L;
не вписывать это в baseattributes. Tags swords/axes/pickaxes/shovels/hoes нужны
для nativeenchantmenteligibility. Miningtag — **INCORRECT_FOR_DIAMOND_TOOL**,
не netherite, дажееслисегодняванильныеincorrectспискиодинаковы.
Evidence: cachedoriginal1.7client `aeh`(sword31–39),`acg`(tool42–49), axe/pick/
shovel `abf/adn/ady`pass3/2/1; hoe`ada`inherits`adb.k()`empty. Player`yz`base1.
Нынешнийnamed1.21JARhelpersADD_VALUE/mainhand вычисляютarg+tierbonus.
Ещё один inheritancequirk сохранён root: originalHoe(`ada`)extendsItem(`adb`),
чей `a(add,sv,sv)` возвращаетfalse и не расходуетdurability. NativeHoeextends
DiggerItem и иначе теряет2 за hit. Поэтому толькоуThaumiumHoe hurtEnemy=false,
postHurtEnemy=noop; sword1/axe-pick-shovel2 wear остаются. Это не innateability,
а сохранение исходного поведения; serverhelper проверяет hoe0.
ObjectAspectResolver.legacyHarvestLevel должен узнать новыйtier3; bonuses остаются
исходными, aspectsснаряжения recipe-derived. Оригинальные9sprites/armor
`thaumium_1.png`/`thaumium_2.png` уже retained; нужны nativebipedlayers и bookaliases.

## Последующая каповая часть — только с настоящим источником auram

- Ingot→9nuggets,9nuggets→ingot: обычные recipes; nuggetmetallum1.
- Balanced shard→1Salis Mundus, ordinaryfurnace200ticks/XP1. Salisbase — исходный
  **unculled** balancedshardbase+praecantatio2−vitreus, не показанные послеcull6tags.
- Inertcap:5nuggets `NNN/N N`, arcane6ordo/6ignis/6aer, CAP_thaumium.
- Chargedcap: inertcenter+3отдельныхSalis, potentia12+auram6, instability5,
  CAP_thaumium. Пара:10nuggets/6Salis/36essentia;2ingots→18nuggets/8остаток.
  Никакая enchantedcloth здесь не требуется.
- Materialcapmodifier.9F/craftCost6; wood/greatwood/silverwood assembly6/18/54
  каждого primal до modifier **оплачивающего** wand. Для54нужен заработанный100vis
  payer; прежний greatwood/gold не вмещает54. Newoutput пустой. Fire9centivis/tick
  вместоgold10 должен подтвердиться настоящим cast, не только configregistration.
- Extendcapcodec/itemmapping и craftcostcomponent27→54. Inertcap никогда не usable
  assemblymaterial.3rods×bare/Fire=6новыхwandmodels, iconmetadata2/6.
  **Не ставить capproperty2**: nativeClampedItemPropertyFunction клипуетдо1.
  При третьемcapсогласованноперенормироватьproperty/JSON (напримерiron0/gold.5/
  thaumium1), какисправленоRODpropertyвM07, сохраняявсестарыеварианты.
- CAP_thaumium parentsCAP_gold/THAUMIUM/INFUSION, complexity2,
  researchmetallum6/praecantatio6/instrumentum3/auram3. No warp registrations.
  AnchorsConfigRecipes106–117/236/328/400/403/491, ConfigResearch266,
  ConfigItems304, ItemWandCasting247–259/297–335.

### Auram precondition, не выдуманный бесплатный ресурс

Read-onlyreport предлагает originalbeaconcomplex binding(ConfigAspects343):
+auram2/+praecantatio2/+permutatio2 над generatedbase. **Это пока НЕ доказанный
игровой источник auram**: оригинальный furnace вызывает getBonusTags/cullTags6,
а generatedbeaconbase имеет много аспектов. Проверить финальный realresolver и
реальную перегонку ДО обещания «6beacons→12auram». Не обходить cull и не снабжать
готовую auram под видом заработанной. OriginalConfigAspects430 даётwispessence
auram2, но её acquisition ещё не реализован; это отдельная честная dependency.
При отсутствии подтверждённого vanillaисточника сначала завершить9полезных
Thaumium equipment items, затем настоящий originalsource и chargedcaps; не
блокировать всё снаряжение и не регистрировать заряженный output-заглушку.

Найден более прямой original-путь без изобретения моба: **разрушение узла**.
`BlockAiry:265–279` (node metadata0) в server `onBlockHarvested` перебирает текущие
аспекты узла. Если amount≥5, inclusive `a=0…amount/10` порождает
`floor(amount/10)+1` отдельных WispEssence с сохранёнными `aspect×2`. Fortune в
этом коде не участвует. Не путать currentamount с basecapacity. `ItemWispEssence:
89–105` хранит настоящие аспекты контейнера; `ConfigAspects430` добавляет базовые
auram2, а `CraftingManager365–378` прибавляет containedaspect2. То есть даже
primalessence имеет настоящий auram2+primal2, без beacon/cullbootstrap и безспавна
несуществующего wisp. Аналогичный drop есть у knotlog(BlockMagicalLog105–118), но
его форма/PUREnode ещё не реализованы и не должны незаметно входить в эту unit.

Текущий nativeBlockAuraNode этого player-harvest пути ещё не имеет. Следующая
capsunit может честно перенести **NodeHarvest→physicalWispEssence→furnace→auram**
вместе с data-component/tooltip/tint/save/network и оригинальными costs. Нужно
проверить native server removal/drop ordering, game-rule/cancel/replacement и
creativeповедение по оригинальному MC/Forge, не выдавать drop на любой BEunload/
explosion/removal и не инъецировать ужеготовую essence. Это source-backed план,
не обещание принятой nativeперегонки или завершённых wisps/PUREnodes/API.

Enchantment/Repair/elementaltools/traveller/mirrors/runic gear остаются отдельными
units со своими abilities/session/XP/compatibility. Полные M07/M08/M09 не закрыты.

## Root integration в работе, не результат проверки

9обычных shapedrecipes/models,9native itemtags и два recipeingredienttags созданы.
THAUMIUM_TIER явно распознаётся как legacyharvest3; recipe-derived equipment bonuses
сохранены, новых выдуманных прямых objecttags нет.9native bookaliases/iconwhitelist/
en/ruимена связаны с оригинальными ConfigRecipes459–467/ConfigItems387–404/lang446–454.
Ни один обычный рецепт не требует исследования или wand/vis.

Оригинальные armor sheets byte-for-byte alias под native ArmorMaterial.Layer:

| Original retained | Native alias | SHA256 |
|---|---|---|
| textures/models/thaumium_1.png | textures/models/armor/thaumium_layer_1.png | d1ed81f59eb326ae14ce016c4284f8a343349282caec54141485e7131c826d51 |
| textures/models/thaumium_2.png | textures/models/armor/thaumium_layer_2.png | a4a83435d0c8e03e90abdaa3e8637a4ffd5d3bac1ebf284c116eec77c9e985e9 |

`.tmp/alias_thaumium_armor.py` сверяет оба с read-only originalart и отказывается
перезаписывать неожиданное изображение. Никакие original/donor assets не изменялись.
Это адаптация пути, не новая графика/новые права или ужеосмотренные игровые armor layers.

После замечания пользователя полный долгийclientstory НЕ является требованием
каждой новойфичи. Equipment проверяется отдельным existingrunner scope: privatecopy
настоящего принятогоsave сохраняет заработанныйстартовыйinventory, затем только
металлургия/крафт/добыча/ремонт/экипировка и новыеPNG. Предыдущие исследования/
инфузия в этомrun не выполняются и не заявляютсякакповторнопроверенные.
Полныйstory оставлендля крупныхsharedchanges/итоговыхcheckpoint, не на каждыйfix.

## Принятый equipment итог

Build/serverexit0, все66requiredcases,200JUnit,failures/errors/skips0;48Python/
FabricJARnotices+isolationPASS,3563неизменённыхinputs. Source-boundrecorders
`porting/equipment-client.json` и`porting/verification.json`,1Fabricartifact/noNeoPASS.
Все9ordinaryrecipes в настоящем vanillaCraftingMenu безTHAUMIUMresearch,
stats/strictrepair/nativeitemcodec/actualobsidiandrop/nativeattacks/equippedarmor/
9nativeanvilrepairs серверPASS. IRepairablemarker/IRunicArmor0 не являются реализацией
саморемонтаилищита. Modernnativecooldown/enchants/interactions остаютсяадаптацией.

Focusedclientexit0/SCRIPT_PASSED, run`client-smoke-560ad46d-c8e7-453a-ae0a-4f42126c9f23`,
1м24с/1203native ticks.24rawrods+12iron→48praecantatio→12физическиподобранныхingots,
pick3/chest8/repair1. Realnativeheldmining stone7ticks/obsidian220ticks,2wear→anvil
1ingot/1XPlevel→damage0/repaircost1. Затем7отдельныхdeclaredintermediateingots→legs,
nativeinventoryQUICK_MOVE→chest+legsarmor11, toughness0. Оба real savedwands/components/
vis(SilverwoodAer6000),129старойэссенции/5coal/прочиеitemsretainedбезtopups.

Rootвиделвсе6exactPNG: originalrecipes/sprites/menu, actualfloatingingot/obsidiandrop/
heldpick, nativeanvil и2equippedarmor layers. Ночь затемняетarmor66; обычныйhover
tooltipперекрываетчастьцены65 иadvancementnoticeчастьHUD. Exactcosts/miningticks/
statsневычитывалисьизпикселей. Audio/incomingarmorcombat/clientall9crafts/fullsurvival/
полныебоевые иRepair/runicмеханики не заявляются. Исходныересурсы/research/XP/empty
devices/Nitorheat/floor/targets/camera — fixtures. ПредыдущаяM07acquisitionисторическая,
этотrun используетactualsavedinputs и НЕ является full66-scenePASS.
