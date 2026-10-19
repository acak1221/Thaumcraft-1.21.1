# Basic Fire Focus и две настоящие магические культуры

Продолжение после принятого `distillation-validate5`/`distillation-client5`.
**Принят `fire-validate7`/`fire-client5`:200JUnit/61+61/46captures, exact41–46 inspected.**
Source-bound recorder PASS3437unchangedinputs,48PythonPASS, обаJAR notice/isolation.
Прежние distillation200JUnit/58+58/40captures не являются его проверкой. По общему плану это bounded M08focus,
использующий M03/M04 engines, и2M02plants; полного M02/M04/M08/API completion нет.

## Игровой контракт

- Настоящий `thaumcraft:focus_fire`, original FOCUSFIRE research и arcane recipe:
  CQC/Q#Q/CQC, C=fire shard, Q=quartz, #=fire charge; ignis20/perditio10.
- Установленный фокус — отдельный persistent/synchronized component с настоящим
  ItemStack и его native components. Defensive copies и value equality не дают
  внешний mutable alias; поддерживается ровно один настоящий basic fire focus.
  Неподдержанные/вложенные wands/foci и stack count≠1 отклоняются.
- F удерживается для radial выбора; hover/release/click выбирают реальный inventory
  slot. Sneak+F снимает. Сервер получает только hand+slot (-1remove), заново читает
  actor/wand/inventory и выполняет перестановку. Creative не копирует фокус.
  Swap сперва извлекает новый предмет, затем возвращает старый в first-empty slot;
  full-inventory removal отказывается без drop/потери. Смена предмета/компонентов/
  eligible inventory закрывает stale UI. Offhand — явная native adaptation.
- Original F конфликтует с новым vanilla swap-offhand. Очень узкий client mixin
  перехватывает только `handleKeybinds` swap `consumeClick`, только при совпавших
  bindings и удержании native wand. Не меняет bindings и не подавляет unrelated keys.
- Basic RMB начинает channel даже при пустом wand/без research: research требуется
  для рецепта, не выдуманного запрета пользоваться найденным фокусом. Native BOW
  animation и original Integer.MAX_VALUE use duration. Успешный tick тратит10
  **centivis** до float cap/exhaustion modifier и truncation; gold10/iron11, не10vis.
  Один debit предшествует ДВУМ actual spawn attempts; rejected spawns не refund/retry.
  Creative платит vis, empty next tick останавливает use без бесплатного выстрела.
- `IWandable`/node activation предшествует фокусу. Активная node-use session помнит
  исходную цель/hand/stack identity; текущий crosshair не выбирает новую способность
  каждый tick. Отведение взгляда останавливает зарядку, а не включает огнемёт.
  Session ephemeral/weak entity keyed вместо сохранения старого ObjectInUse NBT;
  после перезапуска native actor всё равно не находится в состоянии held-use.
- Настоящий `thaumcraft:ember`: damage2, scatter15, lifetime20, ignition3sec.
  Двойной Gaussian launch (inherited + ember), muzzle advance, .95 damping до
  inherited .99air/.8water, нулевая gravity, four-tick owner exclusion. Damage
  server-only/attributed, ignition лишь после accepted hurt, fire-immune exclusion.
  Basic не зажигает блоки; upgrade-only branch есть в entity, но upgrades не выданы
  игроку. Lifetime/NBT/owner/native tracking сохраняются; expired entity terminal.
- Native Ember renderer использует retained particles.png row9/cells7–15,
  white/.9alpha/growing billboard/SRC_ALPHA+ONE. Отдельного basic beam/trail/burst
  в original нет. Fireloop .33/pitch2 throttled500ms original singleton wall clock.
- Equipped head имеет отдельный native component model override для четырёх
  реально доступных сочетаний rod/cap, original wand.png и focuscolor15028484.
  ARGB opaque, rod/cap не перекрашиваются. Это bounded native cuboid adaptation,
  а не полный pulsing/translucent/animation/Halloween/staff legacy renderer.

## Сребролист / огненный жемчуг

- Оба настоящие BlockCustomPlant variants2/3, light8, .8-high selection, no collision,
  original cross sprites, original flammability100/spread60. Не Ethereal Bloom и не
  Vishroom placeholders. PlantItem placement permissive как original; обычные
  stay/neighbor/random tick soil/light rules сохраняются.
- Soil mapping следует old Forge Block.canSustainPlant: сначала inherited Bush
  grass/dirt/farmland, затем Desert дополнительно принимает оба metadata-варианта
  sand. Native dirt variants и red sand заданы явно. Вне этих soils растение не живёт.
- Harvest даёт один plant; ordinary one-cell recipes дают1quicksilver или1blaze
  powder. Scans/crucible получают original direct herba2/praecantatio2 и соответственно
  permutatio2 либоignis2; это не recipe-derived ресурсы из ванильного цветка.
- Silverwood natural generation больше не пропускает shimmerleaf: original18attempts
  входят в существующую atomic tree transaction, через planned canopy/trunk state.
  Неуспешные PURE-knot candidates по-прежнему отклоняются, без reroll/NORMAL fallback.
- Cinderpearl идёт в том же vegetation pass ПОСЛЕ обоих tree draws: sampledx/z,
  hot sand-top biome и one-in30. Explicit native biome tag пока maps vanilla desert;
  нет невысказанного распространения на все новые hot biomes. Flower generator
  выполняет18attempts/108integer draws, air +grass/sand soil, без chunk forcing.
- Shimmerleaf имеет original coloured wisp/half-life growth и cinderpearl smoke/flame.
  Native ParticleEngine — адаптация, не обещание полного старого particle engine.

## Original anchors

Read-only private4.2.3.5 oracle `thaumcraft/`:

- `common/config/ConfigRecipes:247,414–415`, `ConfigResearch:78`;
  `common/items/wands/ItemWandCasting:164–204,247–259,318–335,373–440,492–580`;
  `WandManager:444–523`, `common/tiles/TileNode:177–181,384–455`.
- Entire `common/items/wands/foci/ItemFocusFire`, `api/wands/ItemFocusBasic`;
  `common/entities/projectile/EntityEmber:48–79,90–130,142–153`,
  `client/renderers/entity/RenderEmber:33–65`, `ConfigEntities:104`.
- `common/lib/events/KeyHandler:36,60–88`, `client/lib/REHWandHandler:98–179,212–285`,
  `client/renderers/models/gear/ModelWand:75–79,148–207`.
- `common/blocks/BlockCustomPlant:62–68,119–143,174–182,194–229`,
  `common/lib/world/WorldGenCustomFlowers:28–35`, `WorldGenSilverwoodTrees:155–158`,
  `ThaumcraftWorldGenerator:459–478`, `ConfigAspects:395–396`;
  `client/fx/particles/FXWisp:38–67,152–177`.
- Old Forge canSustainPlant patch pinned `9274e4fe435cb415099a8216c1b42235f185443e`,
  `patches/minecraft/net/minecraft/block/Block.java.patch`; same existing attribution
  in THIRD_PARTY_NOTICES, no vanilla/Forge source copied into native runtime.

## Границы и следующая проверка

Все source agents frozen; client-sequence дал6новых scenes41–46. Root связал
bootstrap/network/mixins/models/lang/book и wrappers. После source freeze compile/
build both loaders, compact61-gameplay-case floor, then actual
native focus craft→Fselect→RMBdamage→remove and inspect exact41–46. Supplied input/
machines/research/vis/targets/plant patch не выдавать за полный survival/naturalgen.

Открыты: все другие foci/beam/fireball/upgrades/pouches, staves/sceptres, complete
focus API, original wand-vis HUD, custom held animations/pulse/Halloween/legacy tooltip parity; PURE/taint
ecology, greatwood spider dungeon/loot, Vishroom и Ethereal Bloom. Modern native
FIREBALL damage tags/armour/protection — не порт old vanilla combat/AI. Persistence
содержит age/scatter и integer duration; terminal expiry намеренно не допускает
original post-setDead collision. NeoForge client/packaged/remote/survival всё ещё
не подтверждены. Dedicated ordinary launch не запускался без принятой local EULA.

## Запуски текущего среза

- `fire-validate1`: compile FAILED — root использовал неверное имя
  `registerItem` вместо Architectury `registerItemColors`, и ambiguous CreativeTab
  varargs. Исправлены по окружающим native registrations, не меняя gameplay.
- `fire-validate2`: common/test source компилируются, clientTest FAILED — native
  AABB принимает две Vec3, не пару BlockPos. Ограниченный target cleanup fixture
  исправлен на atLowerCornerOf, без изменения production target/damage.
- Inventory refreshed194main Java, оба reference audits pinned/read-only. Сейчас
  source-bound `fire-validate3` завершён exit0:200JUnit/61Fabric/61NeoForge,
  failures/errors/skips0/allrequired61.3437inputs unchanged, оба JAR notices/
  test-isolationPASS. `.tmp/fire-assets3.log`48PythonPASS.
- Отдельный `fire-client1` запущен:46сцен, exact41–46 ещё не осмотрены. Серверный
  PASS не объявляется ни успешным F-input, ни проверкой новых кадров/анимации.
- `fire-client1` FAILED/BOOK_CATEGORY,40ранних captures: fixture менял server selected
  slot6→8, оставляя native client last-sent cache6. Последующий useItem(slot6) не
  отправлял carried-slot packet, сервер видел пустую8. Теперь fixture также выбирает8
  на клиенте и ждёт обычных native ticks до native useItem; никаких fake book screens.
- Source inspection native KeyMapping/Fabric registry выявил другой настоящий риск:
  shared F доставляется одному mapping, так что регистрация фокуса могла съесть
  vanilla swap даже без жезла. Узкий swap-site adapter теперь при обычных руках
  coalesces native focus/vanilla counters, при жезле подавляет только vanilla.
  Никакой global key rebinding/новый input mixin. Client path дополнен настоящими
  двумя swap packets с обычными руками и восстановлением apple/пустой руки, без
  дополнительных screenshots или ручного изменения counters.
- Новые source changes frozen snapshot4; `fire-client2` выполняется. После client
  результата нужен общий `fire-validate4`; validate3 не доказывает эти исправления.
- Client2 реально изготовил focus за2000/1000centivis, показал43radial/equipped,
  затем ошибся observer assertion. По cached Architectury source actual outbound
  custom payload — BufCustomPacketPayload, не исходный Javarecord. Passive observer
  теперь читаетtypeID/копию2bytes и production codec; реальные hand/inventory
  transactions не менялись. Client3/snapshot5 активны.
- Предварительно осмотренный exact client2PNG43 выявил ещё visual defect: в1.21.1
  Screen.render вызывает renderBackground в super.render, размывая уже нарисованные
  header/rings. Подтверждено native bytecode. После текущего run нужен NOOP override
  background у GuiFocusSelect и повторный client; текущий PNG не final visual PASS.
- Client3 выполнил все46scenes: настоящий focus craft/equip→6paidticks/60centivis/
  12ember ADD attempts→cowhealth10→8/fire, native remove и2flowers→quicksilver/blaze.
  Но whole-script FAILED на ordinary-F passthrough. Counter ownership исправлен:
  CLIENT_POST не съедает shared ordinary-hand click до native swap site; background
  radial NOOP override тоже применён. Это не объявляется whole-run PASS.
- Client4 прерван **environment inactive window** на первом F после42captures,
  до проверки последних input fixes. Теперь input steps ждут настоящее активное
  окно с прежним общим420sec deadline, не подделывают флаг и не шлют F вне окна.
  Client5/snapshot7 активны; отдельный scoped helper может активировать единственное
  собственное Minecraft окно без keyboard/mouse input или изменения game flags.
  После успешного client нужен финальный общий validate7 по последним source changes.

## Принятый результат

`fire-client5`exit0/SCRIPT_PASSED46, run
`client-smoke-c1af70f4-508c-42b3-9790-fc8c3da7613d`. Root осмотрел exact41–46.
Фокус реально изготовлен за2000ignis/1000perditio centivis из9ingredients; F/C2Sslot8
установил тот же предмет,6nativepaidticks стоили60centivis и создали12ADDattempts,
realcow health10→8+fire. NativeShiftF вернул1исходныйfocus, остальныеprimals/phials/
apple сохранены. Затем2обычныхF дали2vanillaswap packets и восстановили обе руки,
без focus intents. Survival2plantbreaks/2native2×2crafts дали1quicksilver+1blazepowder.
Модели и naturally ticked plant particles видны на46, damage/Embers/head на44.

43теперь имеет sharpheader/radial иunblurredworld. **Известный visual gap:** native
footer/help накладывается наvanillaHUD/selecteditemname; selection/tooltip/кнопки
работают, но полное layout/HUD parity не принято. Полировать вместе со следующим
client content slice, а не выдавать этот снимок за идеальный UI.

После последних source changes общий `fire-validate7`exit0:200JUnit/61Fabric/
61NeoForge, failures/errors/skips0/allrequired61. Fresh48Python/3437unchangedinputs,
обаartifact notices/isolation; recorder успешно записал `porting/verification.json`
и`porting/fire-focus-client.json`. Runtime/code на последующих M06changes будет
новым непроверенным срезом, не расширением этого PASS.
