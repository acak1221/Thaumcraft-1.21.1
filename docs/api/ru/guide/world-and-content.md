# Содержимое Thaumcraft, существа и мир

## Предметы и блоки Thaumcraft

У каждого предмета и блока есть id 1.21.1 в пространстве имён `thaumcraft`.

| Нужно | Вызов |
|---|---|
| предмет по id | `ItemsTC.require("thaumium_ingot")` (бросает, если такого нет), `ItemsTC.get(...)` (null, если нет), `ItemsTC.stack("salis_mundus", 4)` |
| блок по id | `BlocksTC.get("arcane_stone")`, `BlocksTC.require(...)` |
| поле `ConfigItems`/`ConfigBlocks` из 1.7.10 с метаданными | `ItemApi.getItem("itemResource", 14)` (соль мира), `ItemApi.getBlock(name, meta)`: исходные имена разрешаются в плоский предмет, блок или состояние 1.21.1 |

Полная таблица исходных имён — [LEGACY_IDS.md](../../LEGACY_IDS.md). Вызывайте это из `onThaumcraftReady` или позже:
когда выполняется ваш `onInitialize`, предметов Thaumcraft может ещё не быть.

## Стабилизаторы наполнения

Блок с `IInfusionStabiliser` (`canStabaliseInfusion(level, pos)`) стабилизирует наполнение, если стоит симметрично
вокруг алтаря, как свечи и черепа.

## Существа

| Интерфейс | Действие |
|---|---|
| `IEldritchMob` | потустороннее существо: потусторонние мобы Thaumcraft считают его союзником и не нападают |
| `ITaintedMob` | осквернённое существо: газ флюкса, бутылки скверны и обращение скверной его не трогают, щупальца и криперы скверны его не атакуют |
| `IThaumcraftBoss` | босс никогда не становится чемпионом; `generateName()` даёт ему имя по модификатору чемпиона |

Теги существ (их аспекты) — на странице [аспектов](aspects.md#теги-существ).

## Урон и эффекты

Типы урона в 1.21.1 — данные, поэтому источникам урона нужен мир или существо:

| 1.7.10 | 1.21.1 |
|---|---|
| `DamageSourceThaumcraft.taint` | `DamageSourceThaumcraft.taint(level)` |
| `DamageSourceThaumcraft.dissolve` | `DamageSourceThaumcraft.dissolve(level)` |
| `causeTentacleDamage(entity)`, `causeSwarmDamage(entity)` | те же имена |
| `DamageSourceIndirectThaumcraftEntity` | `DamageSourceIndirectThaumcraftEntity.create(direct, cause, type)` |

Исходные зелья — зарегистрированные эффекты: `PotionFluxTaint.instance()` / `holder()` / `KEY`, так же и
`PotionVisExhaust`. Наложить: `new MobEffectInstance(PotionFluxTaint.holder(), duration, amplifier)`. Эффекты варпа
имеют такие же ключи в `WarpEffects` (следующий раздел).

## Стабильные ключи

Существа, эффекты, материалы брони и звуки Thaumcraft — зарегистрированные объекты. Аддон 1.7.10 узнавал их по классу
(`instanceof EntityWisp`), по числовым id зелий из `Config` или по имени звука в строке; в 1.21.1 тот же аддон писал бы
`"thaumcraft:warp_ward"`, и компилятор ничем бы не помог. API называет каждый объект один раз:

| Нужно | Ключи | Получить |
|---|---|---|
| семь эффектов варпа (1.7.10 `Config.potionWarpWardID`, `potionSunScornedID`, `potionDeathGazeID`, `potionBlurredID`, `potionUnHungerID`, `potionInfVisExhaustID`, `potionThaumarhiaID`) | `WarpEffects.WARP_WARD`, `SUN_SCORNED`, `DEATH_GAZE`, `BLURRED_VISION`, `UNNATURAL_HUNGER`, `INFECTIOUS_VIS_EXHAUST`, `THAUMARHIA` (`WarpEffects.all()` перечисляет их) | `WarpEffects.holder(key)` для `addEffect`, `hasEffect`, `removeEffect` и `new MobEffectInstance`; `WarpEffects.instance(key)` — сам `MobEffect`. Полезен только оберег. Два эффекта флюкса сохраняют `PotionFluxTaint` и `PotionVisExhaust` |
| существо, голем или ледяной осколок (1.7.10 `EntityWisp`, `EntityFireBat`, `EntityBrainyZombie`, `EntityGolemBase`, `EntityFrostShard`...) | `ThaumcraftEntities.WISP`, `FIREBAT`, `BRAINY_ZOMBIE`, `GOLEM`, `FROST_SHARD` и ещё 32: монстры, культисты, потусторонние и осквернённые существа и сундук-путешественник | `ThaumcraftEntities.is(entity, key)` вместо `instanceof`; `type(key)` и `holder(key)` — для спавнера или предиката. `IEldritchMob`, `ITaintedMob` и `IThaumcraftBoss` по-прежнему спрашивают о целом семействе |
| материалы таумиумной, крепостной, пустотной брони и пустотной робы (1.7.10 `ThaumcraftApi.armorMatThaumium` как аргумент `ItemArmor`) | `ThaumcraftMaterials.ARMORMAT_THAUMIUM_KEY`, `ARMORMAT_THAUMIUM_FORTRESS_KEY`, `ARMORMAT_VOID_KEY`, `ARMORMAT_VOID_ROBE_KEY` | `ThaumcraftMaterials.armorMatThaumiumHolder()`, `armorMatThaumiumFortressHolder()`, `armorMatVoidHolder()`, `armorMatVoidRobeHolder()` (а `ThaumcraftApi.armorMatThaumiumHolder()` — для первых трёх): `Holder`, который принимает `ArmorItem`. `holder.value()` — тот самый объект, что возвращает `armorMatThaumium()` |
| звук (`"thaumcraft:wand"`, `zap`, `wind`, `tool`, `brain`, `egidle`, `key`...) | по одной константе `ResourceLocation` на каждое звуковое событие Thaumcraft: `ThaumcraftSoundIds.WAND`, `ZAP`, `WIND`, `TOOL`, `BRAIN`, `EGIDLE`, `KEY`... | `ThaumcraftSoundIds.event(id)` для `Level.playSound` |

Аддон, который узнаёт блуждающий огонёк, накладывает оберег и играет звук жезла, не написав ни одной строки-идентификатора:

<!-- snippet:stable-keys -->
```java
// Thaumcraft's creatures, effects and sounds by key, never by string. A wisp killed with the amethyst core in the hand
// gives its killer a minute of warp ward (the effect that suspends the warp check) and plays the sound of the wand.
ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
    if (!ThaumcraftEntities.is(entity, ThaumcraftEntities.WISP) || !(source.getEntity() instanceof ServerPlayer killer)
            || !killer.getMainHandItem().is(AMETHYST_ROD)) return;
    killer.addEffect(new MobEffectInstance(WarpEffects.holder(WarpEffects.WARP_WARD), 1200, 0));
    killer.level().playSound(null, killer.blockPosition(), ThaumcraftSoundIds.event(ThaumcraftSoundIds.WAND),
            SoundSource.PLAYERS, 0.6F, 1.0F);
});
```
<!-- /snippet -->

- Ключи — константы, они есть уже при загрузке класса. Сами объекты существуют, когда Thaumcraft их зарегистрировал, а это
  случилось к `onThaumcraftReady`: запрос эффекта, типа существа или материала раньше (в вашем статическом инициализаторе)
  бросает `IllegalStateException`, а `ThaumcraftSoundIds.event` бросает `IllegalArgumentException` для id, который не
  является зарегистрированным звуком. Получайте объект внутри обработчика или после хука, как в примере.
  `ThaumcraftEntities.is` не бросает никогда.
- `armorMatSpecial()` и `armorMatVoidFortress()` — простые объекты с исходной защитой (1/3/2/1 и 4/8/7/4) и чарованием
  (25 и 10), и в реестре материалов брони их **нет вообще**, поэтому у них нет ни ключа, ни `Holder`: предмет из такого
  материала собирайте через `Holder.direct(...)` или зарегистрируйте собственный `ArmorMaterial` со своими текстурами одежды.
  Коэффициент прочности оригинала (25 и 18) в 1.21.1 принадлежит предмету, поэтому в этих объектах его нет: задайте его на
  предмете, который собираете. Материал `thaumcraft:special` *зарегистрирован*, но
  это адаптер очков (те же числа защиты, золотой ремонтный ингредиент и слой одежды очков): другой объект, не
  `armorMatSpecial()`, и частью API он не является.
- Проверено модульным тестом `StableKeysApiTest` (каждый ключ корректен и называет id, который мод регистрирует) и GameTest
  `stableKeysResolveToTheRegisteredObjects`, который получает каждый ключ в запущенном мире и проверяет класс, категорию,
  числа защиты и то, что эффект можно наложить через этот хэндл.

## Генерация мира

| Вызов | Действие |
|---|---|
| `ThaumcraftApi.addDimBlacklist(ResourceKey<Level>, level)` | исходный числовой чёрный список измерений; 0 или 2 убирают руды и ауру Thaumcraft, любой уровень — его растительность и постройки |
| `ThaumcraftApi.addBiomeBlacklist(ResourceKey<Biome>, level)` | убирает растительность и руды Thaumcraft из биома (узлы ауры всё равно появляются) |
| тег блоков `thaumcraft:portable_hole_blacklist` | блоки, которые фокус переносной дыры не откроет (исходный `portableHoleBlackList`, теперь данные) |

## Мешки с добычей

`ThaumcraftApi.addLootBagItem(stack, weight, bagTypes...)` добавляет запись в мешки с добычей Thaumcraft; типы мешков:
0 — обычный, 1 — необычный, 2 — редкий (пример добавляет свой осколок в первые два; см.
[рецепты](recipes.md#дополнительно-к-крафту)).

## Рубка деревьев

`ThaumcraftApiHelper.isWoodLog(level, pos)` и `breakFurthestBlock(level, pos, block, player)` — два помощника, которыми
пользовался обход брёвен оригинального Топора потока (`Utils.isWoodLog`, `BlockUtils.breakFurthestBlock`), а
`findFurthestBlock(level, pos, block)` — третий, у которого оригинала нет: он отвечает, ГДЕ сломает следующий вызов
`breakFurthestBlock`, ничего не ломая, чтобы вы могли спросить свою защиту об этой клетке. Аддон из примера валит дерево,
когда бревно ломают с аметистовой сердцевиной в руке:

<!-- snippet:felling -->
```java
// Breaking a log with the amethyst core in the hand fells the tree, the way the Axe of the Stream did. One call of
// breakFurthestBlock breaks ONE block (the far end of its walk) and wears no tool; the loop and its limit are the
// caller's. The broken block is NOT the clicked one (it can lie 24 blocks sideways or 48 up or down), so the add-on's
// own rules are asked about the cell findFurthestBlock names, before every break. This example keeps the world spawn
// off limits; a claims mod would be asked at the same place. (The server's spawn protection, the world border and the
// game mode are applied to that cell by breakFurthestBlock itself.) The last pass breaks the clicked log itself, so
// the vanilla break is cancelled.
PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
    if (!(level instanceof ServerLevel server) || player.isShiftKeyDown() || !player.getMainHandItem().is(AMETHYST_ROD)
            || !ThaumcraftApiHelper.isWoodLog(server, pos)) return true;
    for (int logs = 0; logs < 64; logs++) {
        BlockPos end = ThaumcraftApiHelper.findFurthestBlock(server, pos, state.getBlock());
        if (end == null || server.getSharedSpawnPos().closerThan(end, 16.0)) break;
        if (!ThaumcraftApiHelper.breakFurthestBlock(server, pos, state.getBlock(), player)) break;
    }
    return false;
});
```
<!-- /snippet -->

- `isWoodLog` — правило обхода, а не определитель дерева: проходит каждый блок из тега `#minecraft:logs` (обычное
  дубовое бревно, обтёсанные брёвна, древесина, грибные стволы, брёвна Великого дерева и Серебряного дерева), а листва,
  доски и всё остальное — нет. Оригинал пропускал то, что может держать листву (бревно может, листва нет), или то, что
  есть в его списке брёвен; тег — и то и другое. Топор потока этого порта рубит более узкий набор намеренно. Позиция в
  незагруженном чанке — не бревно, и вызов не загружает чанков.
- `breakFurthestBlock` ломает **один** блок за вызов: конец жадного обхода, а не самое дальнее бревно дерева. Обход
  начинается в `pos` и раз за разом шагает в первую клетку в пределах двух блоков (перебор: x от −2 до +2, y от +2 вниз до
  −2, z от −2 до +2), где стоит ровно `block`, которая является бревном и дальше от `pos`, чем текущая. Он перешагивает
  промежутки в один блок и диагонали, может уйти на соседнее дерево той же породы и не выходит за 24 блока в стороны и 48
  вверх и вниз. Стартовая клетка сама должна быть бревном из `block` (всё остальное, сундук или земля, даёт false и
  остаётся нетронутым); если другой подходящей клетки нет, ломается сама стартовая.
- Блок выпадает с обычной добычей (она определяется предметом в основной руке игрока; для творческого игрока и для null
  выпадения нет) там, где он стоял. Инструмент **не** изнашивается, игрок не устаёт, событие разрушения загрузчика не
  вызывается.
- **Защиту нужно спрашивать о той клетке, которая ломается, а не о той, по которой кликнули.** Вызов применяет к этой
  клетке границу мира, защиту спауна сервера и режим игры игрока и отвечает false, если что-то из этого запрещает. Он
  **не** спрашивает мод приватов или защиты земли и не вызывает события разрушения ни для какой клетки: сначала вызовите
  `findFurthestBlock`, спросите этот мод о возвращённой клетке и вызывайте `breakFurthestBlock`, только если он согласен
  (пример делает так перед каждым разрушением; проверка одного `pos` ничего не защищает, дальний конец может быть в 24
  блоках). Метод отвечает той же клеткой, которую сломает следующий вызов, пока ничего не изменилось, и `null`, где
  ломать нечего.
- `breakFurthestBlock` отвечает true, когда в сборщик отдан ломаемый блок, не воздух, и false для клиентского мира,
  старта, который не бревно из `block`, незагруженного старта и конца, который запрещает правило защиты, поэтому цикл
  заканчивается, когда стартовая клетка стала воздухом; всё равно поставьте предел. Только поток сервера (в другом —
  `IllegalStateException`); состояния он не хранит и чанков не загружает (клетка в незагруженном чанке не звено обхода).

## Клиентские эффекты: частицы

Частицы рисуются на клиенте. Аддон 1.7.10 рисовал частицы Thaumcraft через `Thaumcraft.proxy`, серверный двойник которого
ничего не делал; `thaumcraft.api.client.ThaumcraftClientFX` — этот прокси, где первым аргументом идёт `Level`:

| 1.7.10 `Thaumcraft.proxy` | `ThaumcraftClientFX` | Что рисует |
|---|---|---|
| `sparkle(x, y, z, size, color, gravity)`, `sparkle(x, y, z, color)` | `sparkle(level, x, y, z, size, type, gravity)`, `sparkle(level, x, y, z, type)` | мерцающий квадрат на 18 тиков, `type` от 0 до 7 (фиолетовый, жёлтый, синий, зелёный, красный, чёрный, белый, голубой); рисуется по броску, четыре из шести при «всех» частицах |
| `burst(world, x, y, z, scale)` | `burst(level, x, y, z, size)` | белое облачко блуждающего огонька |
| `wispFX`, `wispFX2`, `wispFX3` | `wisp(level, x, y, z, size, red, green, blue)`, `wisp(level, x, y, z, size, type, shrink, noClip, gravity)`, `wisp(level, x, y, z, tx, ty, tz, size, type, shrink, gravity)` | искорка огонька любого цвета, первичного цвета (`type` от 0 до 5) или летящая к точке |
| `blockRunes(world, x, y, z, r, g, b, duration, gravity)` | `blockRunes(level, x, y, z, r, g, b, duration, gravity)` | один знак руны; **`x, y, z` — угол блока, руна появляется в полублоке от него по каждой оси** |
| `beamCont(world, player, tx, ty, tz, type, color, reverse, endmod, input, impact)` | `beam(level, player, to, rgb, type, reverse, endScale, impact)`, `beam(level, owner, from, to, rgb, type, reverse, endScale, impact)` | непрерывный луч фокуса раскопок: от кончика жезла игрока (`wandTip(player)`) или от заданной вами точки; вызывайте каждый тик, объекта `input` нет (и учитывайте фазу вызова, ниже) |
| `smokeSpiral(world, x, y, z, radius, start, miny, color)` | `smokeSpiral(level, x, y, z, radius, start, minY, rgb)` | **одна** клубящаяся частица дыма, летящая по спирали; кольцо Меча Зефира — это 5 вызовов, а пробуждающегося стража — 33, каждый со случайным углом `start`, так что вызывайте в цикле |
| `essentiaTrailFx(world, x, y, z, tx, ty, tz, count, color, scale)` | `essentiaTrail(level, from, to, phase, rgb, scale)` | одна искорка эссенции между двумя блоками |
| `particleCount(base)` | `particleCount(level, base)` | 0 при «минимальных», `base` при «уменьшенных», `2 * base` при «всех» (никогда не отрицательно: упирается в наибольший `int`) |

<!-- snippet:client-fx -->
```java
// While the amethyst core is in the hand: a sparkle at the hand every tick (drawn with the chance and the particle setting
// of the original), a rune on the block under the feet twice a second, and a beam from the wand tip to the block looked
// at while use is held. Every call takes the Level and draws only on a client level; a beam must be called every tick,
// it ends a few ticks after the last call. Thaumcraft steps its beams (and counts the glint of a beam down) at the end of
// the client tick, so the calls are made at the START of the tick, before that step, as the originals' item ticks were.
ClientTickEvents.START_CLIENT_TICK.register(client -> {
    var player = client.player;
    if (player == null || !player.getMainHandItem().is(ExampleAddon.AMETHYST_ROD)) return;
    var level = player.level();
    Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0, -0.3, 0);
    ThaumcraftClientFX.sparkle(level, hand.x, hand.y, hand.z, 1.0F, 2, 0.0F);
    if (player.tickCount % 10 == 0) {
        // The position of blockRunes is a block corner: the rune appears half a block from it on every axis, so y + 0.7
        // puts it 1.2 above the bottom of the block under the feet, just over its top face.
        var feet = player.blockPosition().below();
        ThaumcraftClientFX.blockRunes(level, feet.getX(), feet.getY() + 0.7, feet.getZ(), 0.6F, 0.2F, 0.9F, 20, -0.02F);
    }
    if (client.options.keyUse.isDown() && player.pick(8, 1.0F, false) instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
        ThaumcraftClientFX.beam(level, player, hit.getLocation(), 0x9933FF, 2, false, 1.0F, 5);
    }
});
```
<!-- /snippet -->

- **Только клиент, но из общего кода вызывать безопасно.** Вызов с серверным миром или с `null` ничего не рисует и не
  падает, как не падал `CommonProxy` 1.7.10; в сигнатурах класса нет клиентских классов, и он загружается на выделенном
  сервере. Вызов с клиентским миром должен идти из клиентского потока (`animateTick` блока, клиентский тик, обратный вызов
  отрисовки), иначе `IllegalStateException`: движок частиц не потокобезопасен.
- **Аргументы проверяются первыми, на любой стороне.** Нечисловое значение, координата вне мира, размер не больше нуля (или
  больше 16), тип вне диапазона (текстура луча 4 и выше, масштаб конца больше 8, удар больше 5), руна или спираль вне
  пределов — `IllegalArgumentException`, поэтому ошибка видна и в тесте только на сервере. Диапазоны — в Javadoc каждого
  метода (это относится и к двум позициям блоков в `essentiaTrail`). Компонент цвета (`wisp`, `blockRunes`) может быть любым
  конечным числом и обрезается до диапазона от 0 до 1: оригинал упаковывал его в байт без проверки, и компонент 2 затекал в
  соседний канал. У `wisp` сохранена причуда оригинала: красный ровно 0 рисуется как 1, поэтому вместе с обрезкой **красный 0
  и меньше рисуется как полный красный**: для частицы без красного передайте малое положительное число (`0.004`).
- **Отсчёт блика луча идёт вниз, поэтому важна фаза вызова.** Thaumcraft двигает свои лучи в конце каждого клиентского тика,
  и этот шаг уменьшает `impact` на единицу, как делала частица оригинала. Вызов до этого шага (клиентский тик предмета или
  существа, тик блока, сетевая задача, `ClientTickEvents.START_CLIENT_TICK`, как в примере) рисует блик со значением
  `impact - 1`, так выглядели фокусы оригинала; вызов после него (слушатель `END_CLIENT_TICK`, зарегистрированный после
  Thaumcraft, обратный вызов отрисовки) рисует его с полным значением, на шаг крупнее. Выберите одну фазу и держитесь её.
- **Спираль дыма — это одна частица.** `smokeSpiral` (и его серверный двойник) рисует один билборд, летящий по спирали, а не
  кольцо. Меч Зефира вызывает его 5 раз, пробуждающийся потусторонний страж — 33, каждый раз со случайным `start` от 0 до 359,
  и именно так получается кольцо: делайте то же в цикле.
- **Настройка частиц** прореживает искру и прячет луч, как в оригинале; остальные эффекты рисуют то, что вы просите. Где
  оригинал так делал, крутите цикл по `particleCount`.
- **Серверные двойники** `ThaumcraftFX.sparkle`, `burst`, `blockRunes` и `smokeSpiral` просят каждого игрока в 32 блоках от
  точки нарисовать эффект (поток сервера; клиент рисует его через тот же метод, что выше); `ThaumcraftFX.essentiaTrail` и
  `blockSparkle` были и раньше. Пользуйтесь ими, когда логика, которой нужен эффект, работает на сервере.
- **Не предоставлено:** блуждающий огонёк, преследующий существо (`wispFX4`), `beamPulseFX`, `bolt`, `arcLightning`,
  `crucibleBubble`, `blockWard`, частицы наполнения и объект-результат `beamCont`. Луч игрока (`Player`) начинается в
  `wandTip(player)` — точке, которую вычислял оригинал и которую посылает фокус раскопок
  (`eye + (-cos(yaw) * 0.066, -0.06, -sin(yaw) * 0.04) + 0.3 * look`, yaw в радианах); она одинакова для всех зрителей и не
  следует за моделью руки при виде от первого или третьего лица. Форма с существом берёт начало у вас.
- **Увидено на кадре: нет.** Правила аргументов, правила геометрии и серверные отправители покрыты `ClientFxApiTest`,
  `ClientFxPayloadTest`, `ClientFxParticleRulesTest`, `ClientFxReceiverTest` (какой слот полученной посылки становится каким
  аргументом вызова, которому он соответствует) и GameTest `clientFxServerTwinsCheckAndSendOnTheServerThread` (он читает
  настоящие байты, которые отправитель пишет игроку в зоне, показывает, что игрок вне зоны не получает ничего, и сверяет
  `wandTip(player)` с формулой для известных рыскания и тангажа). Частицы — те же, что в собственных эффектах Thaumcraft; как
  они выглядят от вашего вызова, не осматривалось, а клиентская половина серверных двойников не проверялась подключённым
  клиентом: проверено только отображение посылки на вызов, но не то, что этот вызов затем рисует.

## Значок аспекта на экране

`thaumcraft.api.client.AspectRenderer.draw(graphics, aspect, x, y, amount, bonus)` рисует значок аспекта так, как это делают
экраны Thaumcraft (1.7.10 `UtilsFX.drawTag`). Только клиент: класс называет `GuiGraphics`, поэтому вызывайте его из экрана,
обратного вызова HUD или другого клиентского кода. Пример рисует аспекты предмета в руке вдоль верхнего края экрана:

<!-- snippet:aspect-tags -->
```java
// The aspects of the item in the main hand, as tags along the top left of the screen: sixteen pixels each, the amount
// in the bottom right corner of the tag (AspectRenderer.draw takes the position of the top left corner).
HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
    Minecraft client = Minecraft.getInstance();
    if (client.player == null || client.level == null || client.options.hideGui) return;
    AspectList aspects = ThaumcraftApiHelper.getObjectAspects(client.player.getMainHandItem(), client.level);
    int x = 8;
    for (Aspect aspect : aspects.getAspectsSorted()) {
        AspectRenderer.draw(graphics, aspect, x, 8, aspects.getAmount(aspect), 0);
        x += AspectRenderer.TAG_SIZE + 2;
    }
});
```
<!-- /snippet -->

Геометрия в пикселях GUI от левого верхнего угла `(x, y)`, который вы передали, — это контракт (в `AspectRenderer` есть константа
для каждого числа, и модульный тест их закрепляет):

| Часть | Где |
|---|---|
| значок | квадрат 16 на 16: знак аспекта, окрашенный его цветом (у `Aspect` всегда есть расположение текстуры; если файла нет, в квадрате видна «текстура-заглушка» игры) |
| `amount` | `int` больше 0: вдвое меньше, белым с тенью, правый край — на правом краю значка (`x + 16`), верх — на `y + 11`, перед значком и перед предметом, нарисованным на этом месте; без «+» и разделителей |
| `bonus` | больше 0: искра стола исследований, кадр 16 на 16 над левым верхним углом (`x - 4, y - 4`), один из 16 кадров по тику игрока; больше 1 — ещё и число вдвое меньше с центром в `x + 4` и верхом на `y + 3` |

`amount` — целое число. Оригинальный `drawTag` принимал `float` и показывал до двух знаков после запятой
(`DecimalFormat("#######.##")` в проверенном унаследованном коде), так что доля эссенции там показывалась, а здесь нет; верх
числа — целый пиксель `y + 11`, тогда как тот код начинает его с `y + 11.5` (записано в `docs/audit/KNOWN_DEVIATIONS.md`,
раздел 9).

Отрисовщик не обрезает, не масштабирует под экран и не показывает подсказку: область наведения — квадрат 16 пикселей, а
значок большего размера — это масштабированный стек поз вокруг `(x, y)`. Смешивание остаётся включённым, цвет сбрасывается в
белый. Как и для частиц, числа проверены тестами, а пиксели на кадре не осматривались.

## Не публичный API

- У големов не было API в 1.7.10, нет и сейчас; `ThaumcraftEntities.GOLEM` лишь узнаёт существо.
- Классы `thaumcraft.api` в духе Thaumcraft 6 (`theorycraft`, `ResearchEntry`/`ResearchStage`, `ScanningManager`,
  `IWandFocus`) — оболочки совместимости для аддонов, собранных под 1.12; к механикам Thaumcraft 4 они не подключены.
  Пользуйтесь классами TC4 из этих руководств.
- Всё вне `thaumcraft.api` (`thaumcraft.common`, `thaumcraft.client`) может измениться без предупреждения.
