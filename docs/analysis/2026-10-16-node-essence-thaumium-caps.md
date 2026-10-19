# Fabric: эфирная эссенция и таумиевые наконечники

**Принято: `caps-fabric-validate1` / `caps-fabric-client1`, только Fabric.**
200JUnit/67Fabric/3586unchangedinputs/48Python;7focusedPNG67–73 осмотрены,
source-bound recorders PASS. Только TC4 4.2.3.5 oracle, без доноров/NeoForge работы.
Проверка будет целевой: существующий сохранённый алтарь/инвентарь — inputs, не
повтор всей цепочки исследований/перегонки/формирования.

## Оригинальный игровой контракт

- `BlockAiry265–279`: player-harvest metadata0, server-only. Для каждого **текущего**
  аспекта узла с amount≥5 выдаются `floor(amount/10)+1` отдельных WispEssence,
  содержащих этот aspect×2. Это inclusive-loop, не capacity/fortune/node-item.
  Удаление/выгрузка BE само по себе не является harvest. Creative/gamerule/removal
  ordering уточняются по vanilla1.7/Forge, не подменяются современным предположением.
- `ItemWispEssence89–105` и `ConfigAspects430`: containerdata и baseauram2 отдельны.
  `CraftingManager365–378` складывает их: AIRessence→AIR2+AURA2; AURAessence→AURA4.
  Нативный component сохраняется/синхронизируется; tooltip не раскрывает неизвестный
  аспект. Originalsprite/tint используются без новой графики/эффектов моба.
- `ConfigRecipes400/403`: ingot→9Thaumium nuggets и9nuggets→ingot, обычный крафт.
  `ConfigAspects372`: nuggetmetallum1. Рецепт разборки использует actualingot,
  не всеitems из c:ingots; caprecipe принимает originalnuggetThaumium через c:tag.
- `ConfigRecipes491`: balancedshard→SalisMundus, nativefurnace200ticks/XP1.
  `ConfigAspects421/AspectList25–37/InternalMethodHandler53–55`: Salis tags —
  **unculled** balancedshardbase+praecantatio2−vitreus. Не smeltingbase и не уже
  обрезанное до6аспектов display/dissolution. Root заменил divergentlegacyliteral
  этой единственной source-anchored динамической direct-expression.
- `ConfigRecipes236`: inertcap из5nuggets (`NNN/N N`), gateCAP_thaumium,
  ordo6/ignis6/aer6. `ConfigRecipes328`: chargedcap=centerinert+3отдельныхSalis,
  potentia12/auram6, instability5. Пара —10nuggets/6Salis/36essentia.
- `ConfigItems304`: modifier.9F, craftCost6. Assemblycost6/18/54 каждогоprimal для
  wood/greatwood/silverwood до modifier **paying wand**; новый результат пустой.
  Нельзя оплачивать54 жезлом ёмкостью50 или применять скидку output к его сборке.
  Inertcap не является usableматериалом. BaseFire расходует9centivis/tick, не10.

## Текущие владельцы и root integration

- `ses_ebbb782b2ffemUvPaQqNiZcHHw` SOURCEFROZEN: existingBlockAuraNode, новые
  ThaumcraftEssences/ItemWispEssence/WispEssenceData/IEssentiaContainerItem,
  при необходимости небольшой NodeHarvest и один компактныйruntimehelper.
- `ses_ebbb5922effe881X2NDuqe83CM` SOURCEFROZEN: только новый ThaumiumCapsClientSequence.
  Constructor(newworkbench,existingcenter),tick(client,capture),report; новые67…final.
- Root: остальные existingfiles/resources/registries/bootstrap/tint/book/wands/tests.
  Уже добавлены3items(nugget/inert/charged),5recipes+nativeingredienttag, capmaterial
  .9/cost6, craftcostcomponentceiling54,9materialpaircodec coverage,6held/focused
  models и2capbookaliases. CAPproperty/JSON согласованно iron0/gold.5/thaumium1,
  потому что nativeproperty clip0…1; RODproperty0/.5/1 без изменения.
  ExistingWandAssemblycase дополнен inertrefusal/исследованием/native54payment/
  emptyoutput/codec54/price90+Fire9. Новыйnode-case поднимает plannedfloor до67.

Root scope`caps` копирует **настоящий принятый M07save** в новый ownedcaps-smoke-UUID,
передаёт existingcenter из acceptedinfusionrecord и freshworkbench=oldworkbench+96X.
Altar/candles/old129essentia не выдаются заново. Source `book-smoke` не меняется.
Полный existingclientstory остаётся66scenes throughEquipment; новыйcaps scope
отдельный и не выдаётся за полныйсквознойrun. Actualcapturecount ещёдолженвернуть
clientagent; дообоихSOURCEFROZEN никакихGradle/game.

## Намеченная целевая приёмка, не выполненный результат

Nativeharvest нового declaredNORMAL AIR70node→8physicalessences→furnace16auram/
16aer;12rawcoal→24potentia/24ignis,10manualphialpairs→4jars. Две настоящие
инфузии расходуют36/6Salis, оставляют44surplus и старые129units. Salis выплавляется
из6declaredintermediatebalancedshards;2Thaumiumingots и1Silverwoodrod также явно
intermediateINPUTS: не повторяем уже принятую металлургию и63essentia core-story.
Никакой разборки старогоwand вrod не изобретается. Nativecapassembly54 каждогоprimal
оплачивается старым100vis/goldwand после **реальной finite node зарядки**;
потом новыйwand/retainedFireFocus должен реально показать9centivis/paidtick.
Существующий72candle altar стабилизирует3компонента oppositepair+oneunpaired
естественным symmetry, без I/RNGwrites. Исходы нестабильности не отключаются.

Wispmobs, PURE/knotlogdrops, другиеcaps/staves, Repair/runic/enchantmentinfusion,
общийAPI/fullsurvival/naturalspawnfrequency/fullvisual/audio/NeoForge не входят
в обещание этой unit. Native-removal/scatter/cancellation adaptations должны
быть явно описаны после review, а не скрываться под словом «порт».

## Root review после essence SOURCEFROZEN

Root прочитал implementation/helper. NodeHarvest.prepare после nativecallbacks
захватываетточныйживойNORMALBE/currentaspects; onRemove толькосвидетельствуетidentity,
Block.destroyпослеуспешногоremove коммититодинраз, втомчислеcreative. Короткие
weaklevel/weakBEattemptsочищаютсяSERVER_LEVEL_POST доDeferredItemDrops.tick и
SERVER_STOPPED; unload/death/обычныйnonplayerremoveнеявляютсяharvest. Исходные
NORMALpropertieshardness2/resistance200восстановлены; whole-node loot запрещён.
Rootподключилtooltipviewerнаclient иреальныеcreativeaspect2variants.

От предложенной inventory-scatter adaptation root отказался: после read-only
MC1.7 `aji.a(World,x,y,z,ItemStack)` offset.7F/world.nextFloat×3/pickup10 и native
ItemEntity explicit-velocity constructor добавленмаленький отдельный
`DeferredItemDrops.enqueueBlockHarvest`. Он сохраняетисходные[.15,.85)offsets/
worldRNG, item-localuniformXZmotion±.1/Y.2 иpickup10. Старыйinventorypath(localRandom,
Gaussian.05,pickup0) не менялся. OptionalPickupDelay вsavedqueueпоумолчанию0 для
старыхinventoryentries; nativeexplicitvelocityctorнерасходуетлишниеworlddraws.
Counts/componentsнеразбиваются/неинъецируются. Явнаяоставшаясяadaptation: доставка
level-postпослеcommit и сохранениеотменённогоADD вместоисходнойпотерипредмета,
чтобыspawncallbackневходилвнезавершённоеудаление. Это sourceimplemented,
**ещёнесобрано/непрогонялось**, clientagentещёактивен.

Clientтеперьтакжеfrozen/rootread:7contiguouscaptures67–73. Rootexistingrunner
scopecaps/count7/FurnaceScreencapture wired, fullthroughEquipment66непрогоняется.
Разрешёнcompile-only, затемfocusedclientновогоконтента; finalbuild/server67после
последнихsourcechanges. Никакихещёнеполученныхрезультатовниже/выше не подразумевается.

## Принятый результат

Сборка/server exit0;200JUnit/67Fabric/allrequired67/failures-errors-skips0,
3586неизменённыхinputs,48Python. FabricJARnotices/isolation PASS. Клиент
`client-smoke-4940ea58-8d98-460d-b3c2-609a76b13298`,7focusedcaptures67–73,
все точныеPNG осмотрены.6м7с/6778ticks — настоящие новые процессы без ускорения;
старые60сцен не повторялись. Сырьё/пустыеустройства/finiteNORMALnodes/research/
floor/camera объявлены, не fullsurvival/naturaldiscovery.

61heldminingticks→8ONEessenceADDs/AIR70. Из8essences+12rawcoal actualfurnace
создала80units;2из5сохранённыхfuelcoal израсходованы,3остались.10phialwithdrawals,
4новыхjars;6balancedshards→6nativeSalis. Дваinertcapcraft/двеpaidinfusions:
36oneunitdrains по10ticks и6реальныхSalisconsumptions. Получены2caps, физически
eject/pickup иconsumednativeassembly за54каждогоprimal realold100visgoldpayer.
Новыйwandизначальнопустой/capthaumium/rodsilverwood/craftcomponent54; дальше
finiteheldnodecharge иF/RMB→6paidFireticks по9centivis,54debit/12embers/cowignite.
44новойэссенции+старые129/8nuggets/3fuelcoal/обаoldwands/прочиеoutputs retained.
Silverwoodrodи2Thaumiumingots — intermediateINPUTS, неэтогопрогонаearnedcores.

Сервердополнительнопроверилthreshold/current-not-base/mixedaspect/creative/
doTileDrops/cancel/reentry/replacement/nativecodec/packet/tooltip/addonaspect и
15earnedessences→actualalembicsauram30+aer30. Это неchunk-unload/multiplayerPASS.
Originalscatter/pickup10 + delayedcommit/rejectedADDretentionadaptation каквыше.
67ночнойtintтёмный,70непоказываетдлинныйstreamполностью,73wandмодельопущена;
nativecountersневычитывалисьизпикселей. Аудио не прослушивалось. Следомочки,
неclaims о готовности specialnodes/Wispmobs/fullAPI/Repair/runic/NeoForge.
