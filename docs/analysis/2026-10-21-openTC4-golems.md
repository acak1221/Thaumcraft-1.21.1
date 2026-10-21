# Сверка порта Thaumcraft 4.2.3.5 (1.21.1) с оригиналом 1.7.10: големы и модификаторы чемпионов

Дата: 21.10.2026. Ветка: `port/ocgolems`.

Область: `thaumcraft/common/entities/golems/**` (`EntityGolemBase`, `EntityGolemBobber`,
`EntityTravelingTrunk`, `EnumGolemType`, набор целей `*GolemGoals`), `thaumcraft/common/lib/mods/**`
(`ChampionRules`, `ChampionWiringRules`, `IChampionModifierEffect`, `ChampionMod*`),
`thaumcraft/common/lib/golems/**`, `thaumcraft/common/items/golems/**` (`GolemDamageRules`,
`GolemUpgrades`, `GolemDecorations`, `ItemGolemCore`, `TrunkRules`), а также GUI/меню голема.

## Источники истины

| что | где |
|---|---|
| Оракул TC4 4.2.3.5 (декомпиляция 1.7.10) | `/home/Acak/work/thaumcraft-4.2.3.5-oracle` |
| Ваниль 1.7.10 (MCP-имена) | `/home/Acak/work/mc1710-src` |
| Разрешитель SRG→MCP 1.7.10 | `python3 -B scripts/porting/resolve_srg.py <имя>` |
| OpenTC4 (1.7.10, только имена ваниллы) | `/home/Acak/work/opentc4` |

Ключевое наблюдение аудита: **`/home/Acak/work/mc1710-src` — это читаемая ваниль 1.7.10**,
и она снимает половину спорных мест (свойства `EntityAITasks`, иерархия `EntityMob`,
`DamageSource.cactus`/`magic`/`inWall`/`outOfWorld`). Отчёт об аудите a7
(`docs/analysis/2026-10-21-audit-a7-golems.md`) сверял регистрации целей, но не сверял
**эффективное** множество целей 1.7.10 — это и дало дефекты 3 и 4 ниже.

## Итог

| № | дефект | серьёзность | состояние |
|---|---|---|---|
| 1 | Голем и traveling trunk отказывали не от того источника урона (`IN_WALL` вместо `cactus`) | высокая | исправлено |
| 2 | Голем и traveling trunk резали не тот «магический» урон (`INDIRECT_MAGIC` вместо `magic`) | высокая | исправлено |
| 3 | Голем имел цель плавания (`EntityAISwimming`), которой в оригинале нет; она отнимала MOVE у целей Decanting/Alchemy | высокая | исправлено |
| 4 | Порядок регистрации целей был обратным оригинальному (общие цели ставились раньше целей ядра) | средняя | исправлено |
| 5 | Голем не возвращался домой при уроне «в стене»/«из мира» | средняя | исправлено |
| 6 | Тяжёлые големы не удваивали скорость в воде | средняя | исправлено |
| 7 | Отсутствовала проверка «порядка установки целей» в тестах | низкая | исправлено |

Проверено и признано корректным (правок не потребовало): модификаторы чемпионов (все 13),
белый список чемпионов, ward чемпиона, таблица материалов голема, аксессуары, улучшения,
рецепты ядер и плейсеров, скорость атаки, радиусы, ёмкость, числа урона/HP/брони.

## Дефекты и исправления

### 1. Отказ от «cactus»-урона, а не от «in wall» (дефект 1)

**Оракул.** `thaumcraft/common/entities/golems/EntityGolemBase.java:1131-1134`

```java
public boolean func_70097_a(DamageSource ds, float par2) {
    if (ds == DamageSource.field_76367_g) { return false; }
```

и `thaumcraft/common/entities/golems/EntityTravelingTrunk.java:106-109` — тот же источник.

`field_76367_g` разрешается в 1.7.10 как **cactus**
(`python3 -B scripts/porting/resolve_srg.py field_76367_g` → `cactus`), и это подтверждается
ванилью 1.7.10: `net/minecraft/util/DamageSource.java:25`
`public static DamageSource cactus = new DamageSource("cactus");`
(`:22` — `inWall`, `:27` — `outOfWorld`, `:29` — `magic`).

**Порт (было).** `common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java:2395`
и `EntityTravelingTrunk.java:160` проверяли `DamageTypes.IN_WALL`, а комментарий утверждал,
что `field_76367_g` — это «inWall». Это верно только для SRG→MCP отображения **1.12**
(именно его использует легаси-дерево `src/main/java` этого репозитория); оракул — 1.7.10,
и `scripts/porting/resolve_srg.py` это прямо документирует в своей шапке.

**Фикс.** Новые чистые правила в `common/src/main/java/thaumcraft/common/items/golems/GolemDamageRules.java`
(`IMMUNE_SOURCE = DamageTypes.CACTUS`, `isImmuneSource(DamageSource)`), переиспользуемые обоими
мобами; `EntityGolemBase.java:2423` и `EntityTravelingTrunk.java:161` используют их.
Побочный эффект: в 1.21.1 голем теперь **дышит** в стене (источник `suffocation` больше не
отсекается), как в оригинале.

### 2. Половина «магического» урона (дефект 2)

**Оракул.** `EntityGolemBase.java:1136-1138`

```java
if (this.getGolemType() == EnumGolemType.THAUMIUM && ds == DamageSource.field_76376_m) { par2 *= 0.5f; }
```

`field_76376_m` → **magic** (`resolve_srg.py field_76376_m` → `magic`; ваниль 1.7.10
`DamageSource.java:29`, `magic` снимает броню и помечен как `magicDamage`).

**Порт (было).** `EntityGolemBase.java:2399` проверял `DamageTypes.INDIRECT_MAGIC`.

**Фикс.** `GolemDamageRules.MAGIC_SOURCE = DamageTypes.MAGIC` + `isMagicSource(DamageSource)`,
`EntityGolemBase.java:2427`. Побочный эффект: половину урона теперь получает и
`indirectMagic`? — нет: `indirect_magic` в 1.21.1 — отдельный тип («indirectMagic»),
и он **не** режется, как и в оригинале (оригинал резал ровно `magic`).

### 3. Цели плавания, которой нет в оригинале (дефект 3)

**Оракул.** `EntityGolemBase.java:265` — `this.field_70714_bg.field_75782_a.clear();`
(`field_75782_a` → `taskEntries`, `field_70714_bg` → `tasks`, см. `resolve_srg.py`),
то есть **все** цели селектора стираются, и затем регистрируются только цели TC4
(`EntityGolemBase.java:267-351`). В ванили 1.7.10 `EntityAISwimming` регистрируется
**в конкретных мобах** (`mc1710-src/net/minecraft/entity/passive/EntityWolf.java:55`,
`EntityHorse.java:86`, `EntityOcelot.java:45`, `EntitySheep.java:45`,
`boss/EntityWither.java:57`), а **не** в `EntityLiving`/`EntityLivingBase`/`EntityCreature`/
`EntityGolem` (в них нет ни одного `addTask`). Значит, у голема TC4 цели плавания не было.

**Порт (было).** `GatherGolemGoals.install()` регистрировал `FloatGoal` с номером 1
(приоритет `GoalPriority.SWIM`).

**Фикс.** Цель плавания удалена (`GatherGolemGoals.java`: `installCreeperFlight/installGather/installShared`),
константа `GoalPriority.SWIM` оставлена с описанием, что оригинального номера 1 не существует
(`EntityGolemBase.java:794-803`), и добавлен тест `GolemGoalPriorityTest.theOriginalGolemHadNoSwimmingGoal`.

Почему это mattered: нативный селектор 1.21.1 отдаёт флаги **первой** зарегистрированной
цели, а `FloatGoal` имеет `Flag.MOVE`. С номером 1 он делил MOVE с `AILiquidEmpty(1)` и
`AIEssentiaEmpty(1)` (цели номера 1 у големов Decanting и Alchemy) — эти цели не могли
запуститься никогда, а цели номера 2 и 3 (сбор/маршрут) не могли вытеснить `FloatGoal`,
потому что `WrappedGoal.canBeReplacedBy` требует строго меньший номер. В воде голем
Decanting/Alchemy был полностью заблокирован.

### 4. Порядок регистрации целей (дефект 4)

**Оракул.** 1.7.10 `EntityAITasks.onUpdateTasks` (ваниль `mc1710-src/net/minecraft/entity/ai/EntityAITasks.java:49-85`)
обходит `taskEntries` **в порядке регистрации**, а `canUse` (`:94-109`) блокирует цель, если
уже работает цель с номером `<=` её номера. TC4 регистрирует цели по возрастанию номера
(`EntityGolemBase.java:267-351`), поэтому порядок регистрации = лестница.

Нативный 1.21.1 `GoalSelector.tick()` (декомпиляция `net/minecraft/world/entity/ai/goal/GoalSelector.java`)
тоже обходит `availableGoals` в порядке регистрации, и `WrappedGoal.canBeReplacedBy` требует
строго меньший номер. То есть **порядок регистрации — вторая половина лестницы**.

**Порт (было).** `EntityGolemBase.rebuildGoals()` сначала ставил общие цели
(`work.install()`: полёт от крипера, плавание, дверь, домой, взгляд, простой), а цели ядра —
после них. Общие цели (номера 5-8) успевали забрать MOVE/LOOK раньше целей ядра.

**Фикс.** `EntityGolemBase.java:945-960`: сначала `work.installCreeperFlight()` (номер 0),
затем цели ядра (`harvest/lumber/guard/fishing/liquid/alchemy/use/inventories/work.installGather`),
и только потом `work.installShared()` (дверь 5, домой 6, взгляд 7, простой 8) — ровно
как в оригинале.

### 5. Возврат домой при уроне «в стене»/«из мира» (дефект 5)

**Оракул.** `EntityGolemBase.java:736-747`

```java
protected void func_70665_d(DamageSource ds, float par2) {
    if (ds.func_76347_k() && this.golemType.fireResist) { return; }
    if (ds == DamageSource.field_76368_d || ds == DamageSource.field_76380_i) {
        this.func_70012_b(home.x + 0.5, home.y + 0.5, home.z + 0.5, 0.0f, 0.0f);
    }
    super.func_70665_d(ds, par2);
```

`field_76368_d` → `inWall`, `field_76380_i` → `outOfWorld` (`resolve_srg.py`; ваниль
`DamageSource.java:22,27`).

**Порт (было).** Правила не было вовсе.

**Фикс.** `EntityGolemBase.java:2430-2431, 2436-2449`: `returnHomeFromDamage(ServerLevel)`
переносит голема на блок дома перед приёмом урона. Отличие (документировано в коде):
оригинал писал точную координату дома и обнулял поворот; порт сохраняет поворот голема и
отказывается переносить в незагруженный чанк.

### 6. Тяжёлые големы в воде (дефект 6)

**Оракул.** `EntityGolemBase.java:380-382`

```java
if (this.func_70090_H() && (getGolemType() == STONE || IRON || THAUMIUM)) { speed *= 2.0f; }
```

`func_70090_H()` — `isInWater` (ваниль `mc1710-src/net/minecraft/entity/Entity.java:633`).

**Порт (было).** Правила не было: скорость считалась только из материала/аксессуаров/улучшений.

**Фикс.** `EnumGolemType.heavyWater()` (`EnumGolemType.java:39`) и
`EntityGolemBase.travel(Vec3)` (`EntityGolemBase.java:316-325`): в воде каменный, железный и
таумиумовый голем двигаются вдвое быстрее. Нюанс нативной физики: водная ветка
`LivingEntity.travel` в 1.21.1 двигает константой 0.02 и **не использует** атрибут скорости,
поэтому удвоение атрибута (или `getSpeed()`) было бы мёртвым кодом — порт удваивает вход
движения этой ветки, то есть ровно то же удвоение того же движения. В 1.12-легаси это выросло
в «heavy water golem» (плавание и приоритет узла воды), в 1.7.10 — именно в удвоение скорости.

## Что проверено и оказалось корректным (правок не потребовало)

### Модификаторы чемпионов

Сверены все 13 модификаторов оракула
(`thaumcraft/common/entities/monster/mods/ChampionMod*.java`, `ChampionModifier.java:34`)
с портовыми `common/src/main/java/thaumcraft/common/lib/mods/ChampionMod*.java`:

| мод | оригинал | порт |
|---|---|---|
| bold (0) | урон 0, эффекта нет, скорость ×1.3 | ✓ (`BOLD_SPEED_MULTIPLIER = 1.3`) |
| spine (1) | отражение 1+`nextInt(3)` при `source.damageType == "thorns"` → 0 | ✓ |
| armor (2) | `amount*19/25`, если урон не `isUnblockable` | ✓ (`BYPASSES_ARMOR`) |
| mighty (3) | урон 0, атрибут ×3 | ✓ (см. ниже) |
| grim (4) | 40 % — `Potion.field_82731_v` = **wither**, 200 тиков | ✓ (`Status.WITHER`) |
| warded (5) | раз в 25 тиков +1 к ward, потолок = половина **итогового** HP | ✓ |
| warp (6) | 33 % — `1+nextInt(3)` варпа игроку | ✓ |
| undying (7) | раз в 20 тиков `heal(1)` | ✓ |
| fiery (8) | 40 % — `setFire(4)` | ✓ |
| sickly (9) | 40 % — `Potion.field_76438_s` = **hunger**, 500 тиков | ✓ |
| venomous (10) | 40 % — `Potion.field_76436_u` = poison, 100 тиков | ✓ |
| vampiric (11) | `heal(max(2, amount/2))` | ✓ |
| infested (12) | 40 % — спавн `EntityTaintSpider` | ✓ |

`Potion.field_82731_v` → `wither` и `Potion.field_76438_s` → `hunger` подтверждены
`resolve_srg.py`; в 1.21.1 `minecraft:magic` помечен `bypasses_armor`
(`data/minecraft/tags/damage_type/bypasses_armor.json`), поэтому `isUnblockable()` оригинала
и `DamageTypeTags.BYPASSES_ARMOR` совпадают.

**Проверка «×6», а не «×4» у могучего чемпиона.** Оригинал применяет модификаторы
последовательно (`EntityUtils.makeChampion:243-269`: сначала `CHAMPION_DAMAGE` (операция 2 —
умножение итога на 2), затем `MIGHTYBUFF` (операция 2 — умножение итога на 3)), то есть
обычный могучий чемпимон бьёт с уроном `база × 2 × 3 = 6`. Нативный 1.21.1
`AttributeInstance` складывает `ADD_MULTIPLIED_TOTAL`, а не перемножает последовательно,
поэтому гипотеза о расхождении требовала проверки. Замер на реальном `AttributeInstance`
(база 3, модификаторы 1.0 и 2.0) дал **18.0 = 3 × 2 × 3** — нативная операция
`ADD_MULTIPLIED_TOTAL` умножает последовательно, как 1.7.10. Расхождения нет, правка не
вносилась.

### Ward чемпиона (итоговое, а не базовое HP)

`EntityUtils.makeChampion:271-274` читает `IAttributeInstance.func_111125_b()` — **итог**
атрибута максимального здоровья (база моба + 30 HP бонуса чемпиона), и ward равен его
половине. Порт: `ChampionRules.Adapter.promote` → `ChampionWiringRules.wardedChampionWard(health.getValue())`
(итог) и `ChampionModWarded` меряет потолок по `context.maxHealth()` (итог) — ✓, это уже
было исправлено в `eb2e196` и закреплено тестом `ChampionRulesTest.originalWardedChampionTakesHalfOfItsTotalMaxHealthAsItsWard`.

### Белый список чемпионов

`ConfigEntities:195-208` — карта **классов**; оригинал сопоставлял её через
`Class.isAssignableFrom` (`EventHandlerEntity.entitySpawns:409-414`), поэтому подклассы
наследовали уровень. Порт разрешает и id, и класс (`ChampionRules.tierFor(String, Class)`):

* `minecraft:cave_spider` → уровень 0 (класс паука оригинала) ✓;
* `thaumcraft:cultist_knight` / `cultist_cleric` → уровень 1 (подклассы `EntityCultist`) ✓;
* `thaumcraft:mind_spider` → уровень 0 (`EntityMindSpider extends Spider`) ✓;
* `thaumcraft:taintacle_giant` → уровень 2 (`EntityTaintacleGiant extends EntityTaintacle`) ✓;
* класс зомби намеренно отсутствует в списке классов (в 1.21.1 зомби-деревня слилась с
  зомби, а в 1.7.10 `EntityVillager` в whitelist не входил) — id `minecraft:zombie` несёт
  уровень 0, id `minecraft:zombie_villager` не в списке ✓;
* `EntityThaumcraftBoss` → уровень 200 (портовый `IThaumcraftBoss`) ✓.

Ролл спавна сверен с `EventHandlerEntity.entitySpawns:389-421`: `nextInt(100)`, сдвиги
`+2` (easy или champions выключены), `−2` (hard), `−3` (Outer Lands), `−2/−1` (spooky/nether/end),
`−10/−3` (опасное место), сдвиг уровня `max(уровень−1)`, порог HP 10 — ✓
(`ChampionRules.spawnRoll/tierBonus/becomesChampion`, тест `ChampionRulesTest`).

Оригинальный ролл накрывал `EntityMob`; в 1.7.10 `EntityZombie` и `EntitySpider` — потомки
`EntityMob` (проверено по `mc1710-src`), так что нативный `Enemy` — правильный аналог ✓.

### Числа голема

* `EnumGolemType` — 8 материалов, `health/armor/speed/fireResist/upgrades/carry/regenDelay/strength`
  совпадают символ в символ с `oracle .../EnumGolemType.java:10-17` ✓.
* Аксессуары `H G B F R V P M` и их эффекты (шляпа +5 HP, очки +10 % радиуса, галстук ×1.1,
  феза ×0.66 регена, дротик, визор +1 брони, накладка +4 брони и ×0.88 скорости, булава +2 урона)
  — ✓ (`GolemDecorations`).
* Улучшения: `speed ×(1+0.15·Air)`, `carry + min(16,max(4,carry))·Earth`, `fluid floor(sqrt(carry))·1000`,
  `range 16+4·Water` (+10 % очки, +20 % продвинутый), `урон 2+strength+Earth` (+2 булавой),
  `attackSpeed 20−(advanced?2:0)`, `броня min(20, base+1+4)` — ✓.
* Рецепты: 7 ядер в тигле (`ConfigRecipes.java:172-178`) и 5 в инфузии (`:370-374`) и
  арканное «пустое ядро» (`:264`) сверены с `data/thaumcraft/recipe/**` порта: ингредиенты,
  аспекты, нестабильность и **повреждения** компонентов совпадают (в оригинале
  `InfusionRecipe.areItemStacksEqual:93` требует совпадения damage, если он не `Short.MAX_VALUE`,
  поэтому «неповреждённый` топор/нож/удочка» — это оригинальное правило, а не сужение).

## Пробелы и осознанные отклонения (не исправлялись)

1. **Слияние целей Fill/Empty/Sorting в одну цель-маршрут.** Оригинал регистрирует для Fill
   пять целей (`HomeReplace0/HomePlace1/HomeDrop2/FillTake3/FillGoto4`), порт — одну цель
   маршрута с номером 4 плюс работу в серверном тике. Это уже зафиксировано в аудите a7
   как `replace`; числа и порядок приоритетов при этом сохранены.
2. **Цель `AIHomeReplace` ядра Use.** Оригинал регистрирует её с номером 0, и в 1.7.10
   `EntityAITasks.addTask` **не** удаляет цели того же номера (`mc1710-src/.../EntityAITasks.java:31-33`),
   поэтому обе цели номера 0 (домашняя замена и использование) остаются в списке и
   конкурируют за флаги в порядке регистрации: домашняя замена зарегистрирована первой и
   выигрывает. Порт регистрирует обе в том же порядке и с теми же номерами — поведение
   совпадает (включая «используемый» голем Use держит бесполезный предмет в руке).
3. **Дверная цель.** В оригинале `AIDoorInteract`/`AIOpenDoor` не задают биты mutex
   (`func_75248_a` не вызывается) и не конфликтуют ни с чем; нативный `OpenDoorGoal` в 1.21.1
   тоже не задаёт флагов (`DoorInteractGoal` не вызывает `setFlags`) — совпадает ✓.
4. **Скорость в воде у «лёгких» материалов.** Оригинал удваивает скорость только для
   камня/железа/таумиума (исправлено), в 1.12 это выросло в «can swim + приоритет узла
   воды 0». Порт сохраняет 1.7.10-вариант.
5. **Инвентарь голема.** Оригинал: реальный инвентарь 6+6·Fire (ядра Fill/Empty/Gather/Use),
   1+Fire (Decanting) и одна пустая ячейка у остальных; GUI показывает «призрачные» слоты
   именно этого инвентаря. Порт не имеет реального инвентаря: призрачные слоты — это
   конфигурация фильтров, их число 6+6·Fire (и одна страница у остальных ядер), у
   Decanting фильтр жидкости допускается только в слоте 0. Расхождение зафиксировано в
   аудите a7 как `replace`.

## Проверка

```
export JAVA_HOME=/home/Acak/.jdk21
export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=/home/Acak/gradle-homes/ocgolems
./gradlew --no-daemon --console=plain :common:test
```

Реальный вывод прогона (после правок, с `--rerun`, чтобы тесты не взялись из кэша):

```
> Task :common:compileJava UP-TO-DATE
> Task :common:processResources UP-TO-DATE
> Task :common:classes UP-TO-DATE
> Task :common:compileTestJava
> Task :common:processTestResources UP-TO-DATE
> Task :common:testClasses
> Task :common:test

BUILD SUCCESSFUL in 21s
5 actionable tasks: 2 executed, 3 up-to-date
```

По отчёту JUnit (`common/build/test-results/test/TEST-*.xml`):
**241 набор, 1669 тестов, 0 падений, 0 ошибок, 0 пропущено.**

Новые/изменённые тесты:
* `thaumcraft/golems/GolemDamageRulesTest` — новый: закрепляет, что отказной источник урона —
  это `minecraft:cactus` (а не `minecraft:in_wall`), а резаный «магический» — `minecraft:magic`
  (а не `minecraft:indirect_magic`), и что половину урона получает только таумиумовый голем.
* `thaumcraft/golems/GolemGoalPriorityTest.theOriginalGolemHadNoSwimmingGoal` — новый: номер 1
  принадлежит целям опустошения Decanting/Alchemy, цели плавания у голема нет.
* `common/src/gameTest/java/thaumcraft/common/testing/TravelingTrunkRuntimeChecks.damageRules` —
  перевёрнут под исправленный источник (кактус больше не ранит trunk, «в стене» — ранит).

Клиентская визуальная проверка, мультиплеер и сборка NeoForge **не выполнялись** и не
заявляются. GameTest-набор (`common/src/gameTest`) только скомпилирован
(`:common:compileGameTestJava`), но не запускался: запуск требует Fabric-окружения с
загрузчиком, чего в этом прогоне не было.
