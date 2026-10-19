# Следующая M06 unit: basic tube → настоящая банка

**Принят Fabric-only `tubes-fabric-validate3`/`tubes-fabric-client1`.**
200JUnit/63Fabric/52captures, rootexact47–52; source-bound recorderPASS3458inputs.
NeoForgeпо решениюпользователя paused, не билдился/непрогонялся для этого среза.
Исходный read-only contract ниже сохранён. Reforged1.21.1/TC6 полезны как native API примеры, не
копируемые классы и не gameplay specification. Oracle — privateTC4.2.3.5.

## Что действительно нужно

- Basic `TileTube`/`BlockTube`, native ID `essentia_tube`; не пустые registrations
  шести advanced variants. Valve/filter/buffer/resonator не обязательны для цепи.
- Маленький реальный prerequisite: `quicksilver_drop` = original ItemNugget5,
  metallum1 (ConfigAspects371). ConfigRecipes404–405: wholequicksilver↔9drops,
  обычные single-cell и3×3 recipes.
- ConfigRecipes283:8tubes, TUBES, `" Q "/"IGI"/" B "`, Q=quicksilverDROP,
  I=iron_ingot×2, G=glassBLOCK, B=gold_nugget, aqua5/ordo5. Не целая ртуть/tagguess.
- Existing original research TUBES: ConfigResearch307, Secondary+Concealed, parent
  DISTILESSENTIA; добавляем aliasTube, не выдумываем новое условие исследования.

## Серверные правила

- TileTube44–53,102–120:capacity1/open6/facingNORTH, приcount0 random.nextInt10;
  вне vent ++count, suction/ventcheck%2, pull%5 еслиsuction>0. Не global-world phase.
- Suction133–155: сбросtype/amount, directionsDOWN/UP/N/S/W/E; совместимый сосед с
  otherS>0 иotherS>ownS+1 даётotherS−1. Равенство сохраняет первый выбор.
- Pull182–207: только пустая труба, own open, neighbor opposite connectable/output,
  compatibletype, ownS>otherS иownS>=other.minimum; requested1, успех завершаетscan.
  Wanted=suction→тип соседней стороны→UNKNOWN. Не BFS solver/cache/push отalembic.
- Vent158–174,294–309: conflict types при adjacentS ownS/ownS−1 даёт40serverticks,
  event50clientticks. Не уничтожает buffer/не создаётflux/не очищает suction;
  внешний API не блокируется, уже начатыйtick ещё может pull%5.
- Tube API210–291:UNKNOWN неconnect/input/output, getters допускаютUNKNOWN,
  минимум0/extendedfalse, add/take максимум1, empty→nulltype. Facing неone-way.
- Disk55–96 сохраняетtype/amount/facing/open6/stype/samount, count/vent transient;
  suction не входит в исходный custom update. Broken tube — обычный item безbuffer.
- JarFillable133–224:capacity64, connect/input/outputUPonly, minimum32/label64 даже
  full; suction32/64 покаamount<64 иначе0; typefilter→stored. Jar сам pull1 сверху
  каждые5ticks. SourceS<jarS; source.minimum проверяется только для empty-unlabelled
  fallback типа, не безусловно. API low-level add не проверяетfilter; ручной phial
  preflight по-прежнему должен его проверять. Исходный containerContains=0,
  deprecatedlisttake=false, listcontainment=intersection (103–129), не новые догадки.
- Alembic192–248 уже имеетoutput32/excludedDOWN+facing/inputfalse/suction0; не push.
  Original empty markerIAspectSource допустим уjar, не уtubes/alembics.
- HelperThaumcraftApiHelper155–168 проверяет соседа/oppositeside, own проверяетcaller.
  Loaded/current-BE guards обязательны. Native transfer: обе storage-мутации ДО
  callback-bearing notifications, identity/side/type/capacity preflight +guard.
  Не take→callback→refusedadd, не refund через Alembic.addEssentia(always0), не hidden
  buffer. Unknown-addon transaction semantics отдельно, не fullAPIparity claim.

## Native shapes и wand

- Placement clickedFACE, неopposite; floating допустим (BlockTubeItem41–49).
- TileTube372–393 indexed arms0…5 существуют при ЛЮБОМ neighboring transport,
  дажеclosed/incompatible, чтобы reopen работал. Arms.42–.58 откраядо.5, coreID6
  .34375–.65625. Независимый nativeAABBclip, nearest/strict<, ties0…5→6;
  server retrace в actualreach, clientsubHit неавторитетен. CodeChickenне копировать.
- TileTube315–346: arm toggles side и SAMEvalue opposite соседнейTileTube; core
  циклически ищет facing с connectable/openopposite. Sneak не меняет действие.
- Не смешивать collision/selection/render: BlockTube199–240 collisionОДИН expanded
  AABBcenter6/16…10/16; ordinaryselection136–187 X6…10,Y/Z5…11 шестнадцатых.
  Render103–235 arms7…9/16 толькоbothopen, extendedentersjar/alembic6/16;
  core6…10 еслиnoarms/non-tube, иначе6.5…9.5. Retainedpipe1/2/3textures доступны.
  Донор TubeArms union-collision/reach+1 не соответствует этому original контракту.

## Владельцы и приёмка после Fire

1. Transport owner: только новыеBlockTube/TileTube/TubeGeometry/EssentiaTransfers/
   ThaumcraftTubes, exportsregister иtryWandInteraction. Не existingcontainers/resources.
2. Container owner: existingTileJar/BlockJar, минимальные no-notify hooksTileAlembic,
   IAspectSource; не furnace/manual phial/label handlers/drop schema/client/registry.
3. Root: bootstrap/wand/material+recipes+aspects/book/icon/models+ventFX/lang/wrappers.

Actual furnace→2typed alembics→network→labelled jar: matchingтиппо1, иной остаётся;
точная сумма furnace+alembics+tubes+jar, full64, close/reopen, vent/reload и прежний
ручной phial/label path. Это следующий игровой срез, **не уже пройденный тест**.

## Native root integration, пока без runtime verdict

Root добавил exact recipes1→9/9→1 и8tubes/aqua5ordo5, genuine quicksilver_drop
model+metallum1binding (source371), aliasTube иoriginalblockTube0icon. Tube aspects
остаются recipe-derived: originalliteralTube direct tag не найден, не выдумывается.
Native BER использует retainedpipe1/2/3/samebounds иgetFacingпер-face. Sourceoutside
arms extend6/16 вjar/alembic; native directional lighting вместоlegacyRenderBlocksAO
— renderadapter, ещё безclientосмотра. Itemmodel повторяетinventoryshaft/core.
FXVent — nativeParticleEngine/originalmiscparticles, sourcegrowing.05….15→>1,
heading.125/gaussian.0075×5, drag.85/rise.0025/alpha.4 иTileTube129quirkfx,fy,fx.
Ни CodeChickenLib, ни donor classes/assets не копируются. Client hook/export от
Tubeowner ещё согласуется; centralfreeze только после всех3agents.
Root также закрыл reentrant manual-label/алембик entry черезтотжелок: иначе
label-removal мог вернуть blankitem после отказавшего lockedsetFilter. Это native
authority guard только на уже committed transfer notifications, без изменения
обычных manual costs, filter rules или исходного suction.

## Fabric-first и source freeze

Пользователь изменил порядок: сначала весь Fabric, NeoForge отложен. Модуль/
исторические проверки сохранены, новые Neo builds/game не запускать. Все3content
agents frozen; root linked52capture path с actualretainedquicksilver→9drops→8tubes,
2nativeplacements, wandclose/reopen,4inputcoal+1fuel→8+8essentia→labelledjar8 и
survivalbreak/pickup/replacement. Это пока source implementation, не runtimePASS.
Новая минимальнаяserverregression floor63 толькоFabric. Genericdevscripts иrecorder
поумолчаниюFabric; новые отчёты не импортируют старые NeoXML/JAR.

## Первые Fabric-запуски

- `tubes-fabric-validate1/2`: compile/commonJUnit успешны,63runtime изкоторых62PASS;
  новыйtubehelperупалнаindexedray. Diagnosticrun2 показалправильныеeye/target, но
  view отстарогоheadyaw. NativebytecodeLivingEntity.getViewYRot(1) читаетyHeadRot,
  неbodyyaw. Исправлен только синхронныйtestfixtureaim/setYHeadRot+restore; genuine
  serverreach/retrace не ослаблялся и не подменялся suppliedhit.
- `tubes-fabric-validate3`exit0:200JUnit/63Fabric/allrequired63, failures/errors/skips0,
  3458inputs unchanged,48PythonPASS,FabricJAR notice/test-isolationPASS. Ни одного
  NeoGradletask/новогоNeoartifact/result. Это отдельныйFabricservercheckpoint.
- `tubes-fabric-client1` сейчас выполняет52scenes; exact47–52 ещё не просмотрены,
  client/visual итог не принят. VentFX в этомstoryнепокрыт: servervent проверен,
  nativeFXVentтолькоcompiled, не объявлять егоосмотренным пообычнымснимкамтруб.

## Принятый Fabric-результат

Client1 exit0/SCRIPT_PASSED52, run`client-smoke-f4c37215-d61b-402e-a496-1c06a345abff`;
rootосмотрелexact47–52. Реальнаяпрежняяquicksilver→9drops→8tubes за500aqua/500ordo;
2nativeplacements/EAST,1floating. Жезлзакрылfirst.EAST/second.WEST на51serverticks,
затемreopen.4inputcoal+1normalfuel→8potentia+8ignis без suppliedessentia;8potentia
поединице дошли вбанку,8ignis остались вupperalembic.150native-tickchecks exactsum.
Blanklabelнативнопривязанпослеfirstpull; realsurvivalbreak/itempickup/placement
сохранил8+label/facingcomponents. Остались6tubes/8drops/4phials ипрежниеpaidwand/
focus/book/apple/blazepowder. Bookentry — declarednativescreenfixture, не naturalunlock.

BERgeom/gap/rejoin/label видны, нетmissingtextures.49/51capturedimmediatehelditem
transitions,51HUDзакрываетчастьнизабанки; неfullanimation/layoutparity. VentFX/audio
вstoryневызывались/неосмотрены, хотяnativecodecompiled иservervent/statePASS.
Full64/reload/sourceprotection покрытысервернымиcases, неclientstory. Всеinput/
world/vis/researchfixtures записаны в`porting/tubes-client.json`; это неполныйsurvival.

Source-boundrecorder записалновый`porting/verification.json`,200JUnit/63Fabric/
48Python/3458unchangedinputs,1Fabricartifactnotices+isolationPASS. СтарыеNeoXML/JAR
нечитаются/неподтягиваются; oldFireverification сохранён в.tmp. После этого среза
новаяM07реализация будет отдельнонепроверенной, не расширением этого PASS.
