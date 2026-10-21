# Сверка порта сущностей TC4 4.2.3.5 с оракулом 1.7.10

Дата: 21.10.2026. Ветка: `port/ocentities`. Домен: `thaumcraft/common/entities/**`
(мобы, снаряды, ИИ целей), кроме общей базы големов (`entities/golems/**`,
кроме `EntityTravelingTrunk`, который прямо назван в задании).

* Оракул (истина): `/home/Acak/work/thaumcraft-4.2.3.5-oracle`
  (декомпиляция `Thaumcraft-1.7.10-4.2.3.5.jar`).
* Справочник имён ваниллы: читаемый checkout OpenTC4
  (`/home/Acak/work/opentc4/src/main/java/thaumcraft/...`) и, дополнительно,
  восстановленное дерево исходников **ванильной** 1.7.10, собранное локально
  из `client.jar` 1.7.10 + официальный `joined.srg` (`mcp-1.7.10-srg.zip`) +
  переименование через `SpecialSource` + CFR + подстановка имён
  `mcp_stable-12-1.7.10`. Оно лежит вне репозитория
  (`/home/Acak/work/mc1710-src`) и использовано только для чтения.
* Разрешитель SRG: `scripts/porting/resolve_srg.py` (1.7.10-карта, **не** 1.12).

Проверка велась чтением оракула, восстановленной ваниллы 1.7.10 и порта;
исправления внесены в рабочее дерево и закоммичены (`6cee3f7`).

---

## 1. Карта SRG-имён, на которую опирались выводы

`resolve_srg.py` даёт (карта `mcp_stable-12-1.7.10`, не 1.12!):

| SRG | MCP 1.7.10 | Что это |
|---|---|---|
| `field_76367_g` | `cactus` | статический `DamageSource.cactus` |
| `field_76376_m` | `magic` | статический `DamageSource.magic` (bypasses armour + magic) |
| `field_76366_f` | `starve` | `DamageSource.starve` |
| `field_76369_e` | `drown` | `DamageSource.drown` |
| `field_76372_a` | `inFire` | `DamageSource.inFire` |
| `field_76377_j` | `generic` | `DamageSource.generic` |
| `field_76380_i` | `outOfWorld` | `DamageSource.outOfWorld` |
| `field_82727_n` | `wither` | `DamageSource.wither` |
| `func_76348_h` | `setDamageBypassesArmor` | снимает броню |
| `func_76349_b` | `setProjectile` | помечает источник снарядным |
| `func_76361_j` | `setFireDamage` | помечает источник огнём |
| `func_82726_p` | `setMagicDamage` | помечает источник магическим |
| `func_76354_b` | `causeIndirectMagicDamage` | магический **и** снимающий броню |
| `func_76356_a` | `causeThrownDamage` | брошенный снаряд |
| `func_76358_a` | `causeMobDamage` | обычная атака моба |
| `func_82725_o` | `isMagicDamage` | «источник магический?» |
| `func_94541_c` | `isExplosion` | «источник взрыв?» |

Отдельно: в 1.12 карта SRG→MCP другая, поэтому `field_76367_g` в легаси
`src/` читается как `IN_WALL`. Именно на этом порт и споткнулся (дефект 11).

### Важная quirk оригинала (проверена, дефектом НЕ является)

`thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java:19-30`
оригинала объявляет **собственные** приватные поля `fireDamage`, `hungerDamage`,
`isUnblockable`, которые перекрывают приватные поля ванильного
`DamageSource`. Поэтому:

* `setDamageBypassesArmor()` оригинала (`func_76348_h`) ставит флаг в
  перекрытое поле → `isUnblockable()` остаётся `false`;
* `setFireDamage()` оригинала (`func_76361_j`) → `isFireDamage()` остаётся
  `false`.

Следствие: **примитивная стрела TC4 не снимает броню**, а её «огненный» тип
не является огненным источником (огонь ей ставит сам код стрелы,
`EntityPrimalArrow.java:349-351`). Порт это уже воспроизводил верно
(`DamageTypes.INDIRECT_MAGIC` / `FIREBALL` / `ARROW`); менять там ничего не
пришлось. Ниже это зафиксировано как подтверждённое соответствие.

---

## 2. Дефекты: оракул → порт → фикс

Все фиксы — в коммите `6cee3f7` ветки `port/ocentities`.

### 1. Культист: радиус вызова помощи был вдвое меньше оригинала

* **Оракул**: `thaumcraft/common/entities/ai/combat/AICultistHurtByTarget.java:39-40` —
  радиус вызова помощи берётся из `func_111175_f()` = `EntityAITarget.getTargetDistance()`,
  а это **атрибут follow range** вызывающего (у культиста 32), а не константа.
* **Порт**: `EntityCultist.CultistHurtByTargetGoal.callForHelp` — `double range = 16.0;`
  (хардкод 16).
* **Фикс**: `EntityCultist.callForHelpRange(Mob)` +
  `CultistRules.callForHelpRange(Double)` (fallback 16 — как в оригинале при
  отсутствии атрибута); радиус вызова теперь 32 у культиста/клирика/рыцаря и
  40 у претора.

### 2. Культист: потерян «дом» (якорь ритуального клирика)

* **Оракул**: `EntityCultist.java:100-115` — `readFromNBT`/`writeToNBT` пишут
  `HomeD`, `HomeX`, `HomeY`, `HomeZ`.
* **Порт**: `EntityCultist` не имел `addAdditionalSaveData`/`readAdditionalSaveData`
  вообще. 1.21.1 сама не сохраняет область ограничения моба
  (`Mob` не пишет `restrictCenter`), поэтому после перезагрузки мира
  ритуальный клирик терял алтарь (`getRestrictCenter()` → `null`) и сразу
  переставал быть ритуальным.
* **Фикс**: `EntityCultist.addAdditionalSaveData`/`readAdditionalSaveData`
  пишут и читают `HomeD/HomeX/HomeY/HomeZ` (формат оригинала).

### 3. Претор культистов: терялось имя (титул) и «дом» босса

* **Оракул**: `EntityCultistLeader.java:120-130` — байт `title` в NBT, и
  `EntityThaumcraftBoss.java:59-74` — `HomeD/HomeX/HomeY/HomeZ` базы босса.
* **Порт**: `EntityCultistLeader` не переопределял сохранение → после
  перезагрузки титул всегда «Alberic», а область ограничения претора
  (радиус 24) терялась.
* **Фикс**: сохранение/чтение байта `title` (с безопасным клампом индекса) и
  «дома» босса (формат оригинала).

### 4. Культисты и эльдритовый голем: поиск цели раз в 5 тиков вместо каждого тика

* **Оракул**: `net/minecraft/entity/ai/EntityAINearestAttackableTarget.java:41-46` —
  при `targetChance == 0` проверка цели идёт **каждый тик**
  (`if (targetChance > 0 && ...)` — шанса нет, значит проверка всегда).
* **Порт**: `new NearestAttackableTargetGoal<>(this, Player.class, true)` —
  у нативной цели `randomInterval = reducedTickDelay(10) = 5`, то есть поиск
  цели раз в 5 тиков.
* **Фикс**: 6-аргументный конструктор с интервалом 1
  (`reducedTickDelay(1) == 1` → проверка каждый тик) в
  `EntityCultistCleric`, `EntityCultistKnight`, `EntityCultistLeader`,
  `EntityEldritchGolem`.

### 5. Энергетический шар голема: самонаведение было в `distance` раз сильнее

* **Оракул**: `EntityGolemOrb.java:95-102` — делитель это
  `func_70068_e(target)` = `getDistanceSqToEntity`, то есть **квадрат**
  расстояния.
* **Порт**: `ProjectileRules.golemOrbHoming` делил на **обычное** расстояние
  (и комментарий ошибочно утверждал, что так в оригинале).
* **Фикс**: `golemOrbHoming(velocity, delta, squaredDistance)` делит на квадрат;
  `EntityGolemOrb.home()` передаёт `distanceToSqr(target)`; тест
  `ProjectileRulesTest` обновлён.

### 6. Энергетический шар голема: урон не был магическим и не снимал броню

* **Оракул**: `EntityGolemOrb.java:78` —
  `DamageSource.func_76354_b(this, thrower)` = `causeIndirectMagicDamage`
  (магический, снимает броню, `setDamageBypassesArmor().setMagicDamage()`).
* **Порт**: `damageSources().mobProjectile(this, owner)` — обычный снарядный
  источник: броня его гасит, а как магический он не считается (а значит не
  работает и ward-расчёт оригинала).
* **Фикс**: новый тип урона `thaumcraft:indirect_magic`
  (`data/thaumcraft/damage_type/indirect_magic.json`), добавлен в теги
  `minecraft:tags/damage_type/bypasses_armor.json` и
  `thaumcraft:tags/damage_type/is_magic.json`; `EntityGolemOrb` бьёт этим
  источником (прямой — шар, косвенный — владелец).

### 7. Ориентация летящего снаряда

* **Оракул**: ванильный 1.7.10 `EntityThrowable.setThrowableHeading` (восстановлен
  из `joined.srg`) — кроме движения задаёт `rotationYaw` и `rotationPitch`
  снаряда.
* **Порт**: `EntityCultist.originalShoot` задавал только движение.
* **Фикс**: `originalShoot` теперь целится шаром так же, как оригинал
  (`setYRot`/`yRotO`, `setXRot`/`xRotO`).

### 8. Тентакль порчи: неверный источник самоурона

* **Оракул**: `EntityTaintacle.java:117` — `DamageSource.field_76366_f` = `starve`.
* **Порт**: `damageSources().fellOutOfWorld()`.
* **Фикс**: `damageSources().starve()`.

### 9. Малый тентакль: неверный источник самоурона

* **Оракул**: `EntityTaintacleSmall.java:41` — `DamageSource.field_76376_m` = `magic`.
* **Порт**: `damageSources().drown()`.
* **Фикс**: `damageSources().magic()`.

### 10. Портал культистов: неверный источник самоурона

* **Оракул**: `EntityCultistPortal.java:224` — `DamageSource.field_76380_i` =
  `outOfWorld`.
* **Порт**: `damageSources().magic()`.
* **Фикс**: `damageSources().fellOutOfWorld()`.

### 11. Наблюдатель (Watcher): шипы били не по тем источникам

* **Оракул**: `EntityWatcher.java:287-293` — условие ответа:
  `!isGazing() && !source.isMagicDamage() && !source.isExplosion() && <атакующий — живое>`;
  урон `causeThornsDamage` 2.0.
* **Порт**: `!source.is(DamageTypeTags.BYPASSES_ARMOR)` (вместо «не магический»)
  и `!source.is(DamageTypeTags.IS_PROJECTILE)` (вместо «не взрыв»).
* **Фикс**: `!source.is(MagicWard.MAGIC_DAMAGE)` и `!source.is(DamageTypeTags.IS_EXPLOSION)`.

### 12. Сумка странствующего голема (одна из ловушек SRG-карты)

* **Оракул**: `EntityTravelingTrunk.java:107` — `ds == DamageSource.field_76367_g`,
  а в 1.7.10 `field_76367_g` = **cactus** (см. §1).
* **Порт**: `source.is(DamageTypes.IN_WALL)` — это прочтение той же SRG-метки по
  карте **1.12**, где она означает `IN_WALL`. Иммунитет к кактусу не работал,
  а несуществующий иммунитет к застреванию в стене — работал.
* **Фикс**: `source.is(DamageTypes.CACTUS)`.

### 13. Световой гейт мобов: оригинальные мобы не спавнились днём

* **Оракул** (ванильная 1.7.10 `EntityMob.isValidLightLevel`):
  `getSavedLightValue(Sky, x, y, z) > rand.nextInt(32)` → отказ; затем
  **block light** `<= rand.nextInt(8)` → разрешение. Ветка с грозой меняет только
  вычитание небесного света и перечитывает тот же block light — это no-op
  оригинала.
  Для виспа оригинал читает **затемнённый** небесный свет
  (`EntityWisp.java:306-321`, `getLightValue(Sky, ...)`), потом block light.
* **Порт**: `server.getMaxLocalRawBrightness(pos, skyDarken)` =
  `max(blockLight, skyLight - skyDarken)`. Под открытым небом днём это 15, то
  есть `15 <= rand.nextInt(8)` — **никогда**, и оригинальные мобы (висп,
  печ, злой зомби) не спавнились на поверхности днём никогда, хотя в оригинале
  спавнились в каждом тёмном месте дня.
* **Фикс**: новый помощник `thaumcraft/common/lib/entities/OriginalLightRules.java`
  с двумя правилами (`validMobLightLevel`, `validWispLightLevel`), порядок
  розыгрышей оригинала сохранён (сначала небо, потом block light); подключён в
  `EntityWisp.checkWispSpawnRules`, `EntityBrainyZombie.checkBrainySpawnRules`,
  `EntityPech.checkPechSpawnRules`.

---

## 3. Что сверено и оказалось верным (выборочно)

* **Числа культистов**: HP 20/30/36/125, урон 4/5, скорость 0.3/0.32,
  follow range 32/40, разброс 0.6/1.8, 0.75/2.25, опыт 10/40, knockback 0.95,
  home 24, аура 8 блоков, регенерация II на 60 тиков, порог ярости 35 урона,
  anger 200, регенерация `damage/15`, сила/скорость `damage/40`, +50 HP и +0.5
  урона за игрока (максимум 5), запас агро 25/1.1, дистанция 16384 —
  совпадают с `CultistRules`/`EldritchMobRules`.
* **Экипировка культистов**: плащ/латы/сапоги, шансы 0.3/0.1 (клирик) и
  0.05/0.01 (рыцарь) на HARD, `nextInt(5)` → 0 = пустотный меч, `nextBoolean()`
  → снятие шлема, чары 0.25·tension и (int)(5+tension·nextInt(18)) —
  совпадают с `CultistEquipmentRules`/`CultistRules` и порядком розыгрышей.
* **ИИ**: приоритеты целей оригинала (0 swim, 1 altar focus, 2 long range,
  3 melee, 4 restrict door, 5 open door, 6 restriction, 7 wander, 8 watch/look
  idle) сохранены; `CultistLongRangeGoal` повторяет
  `EntityAIArrowAttack` (1.7.10) 1-в-1, включая `seeTime >= 20`,
  `sqrt(d)/maxRange` и интервал `floor(ratio·(max-min)+min)`;
  `CultistMeleeGoal` повторяет `AIAttackOnCollide`, включая «длину» досягаемости
  `width·2·width·2 + target.width` в **квадратных** единицах.
* **Печ**: HP 30, урон 6, скорость 0.5, броня +2 (кап 20), высота глаз 0.66·h
  (1.188 — в `ThaumcraftPech` так и зарегистрировано), 9 слотов добычи,
  деспавн при 5 занятых слотах, таблица «ценных» предметов (мана-боб 1, золото
  2, золотое яблоко 2, эндер-жемчуг 3, алмаз 4, изумруд 5), злость
  400+rand(400), луки 20..50 / проклятия 20..30 на 15 блоков, честность
  `world.rand` при дропе — совпадают.
* **Висп**: HP 22, урон 3, размер 0.9, опыт 5, кластер спавна 2, громкость
  0.25, «thaumcraft:wisplive/random.fizz/wispdead», дальность атаки 16,
  шансы 0.4 (движущаяся цель) / 0.66 (неподвижная) и +1 урона, 6 аспектов в
  eerие, 9/10 primal/compound, `< 8` особей рядом — совпадают.
* **Взрывной шар**: гравитация 0.01, урон `strength·1.5`, жизнь 500 тиков,
  источник `fireball/onFire` = огонь + снаряд (без снятия брони — так в
  оригинале, `EntityExplosiveOrb.causeFireballDamage`), взрыв без владельца и
  без разрушения блоков — совпадает.
* **Бутылка порчи**: гравитация 0.05, скорость броска 0.5, угол −20°, радиус
  всплеска 5, отравление на 100 тиков, 10 попыток прорастания волокон с
  разбросом 5, `world.rand` для 50 % гейта, порядок «сначала биом, потом блок» —
  совпадает.
* **Примитивная стрела**: базовый урон 2.1, множители 1.5 / 0.8 / 0.8,
  `ceil(speed·damage)`, крит `nextInt(damage/2+2)`, 100 тиков в земле,
  эффекты (замедление / слабость / иссушение) и огонь — совпадают;
  см. также quirk про броню в §1.
* **Эльдритовый голем**: HP 250, урон 10, броня +6, скорость 0.3, размер
  1.75×3.5, высота глаз 3.0 / 3.33, таймер спавна 100, заряд луча 150,
  кулдаун ближнего боя 10, урон 0.75·атака, дуга 8+rand(5) — совпадают.
* **Злой зомби**: HP 25, урон 5, броня +3 (кап 20), шанс подкреплений 0,
  дроп `rotten_flesh` 3 попытки через `world.rand` и мозг при
  `world.rand.nextInt(10) - looting <= 4`, редкий дроп (железо/морковь/картофель)
  — совпадают.

---

## 4. Пробелы и осознанные отклонения (не исправлены)

1. **Разрыв дверей у культистов.** Оригинал в конструкторе делает
   `navigator.setBreakDoors(true)` / `setAvoidsWater(true)`
   (`EntityCultist.java:37-38`). В 1.21.1 у навигатора нет «ломать двери»
   (только `setCanOpenDoors`), а отдельной цели `BreakDoorGoal` у культиста
   в оригинале нет. Водяное избегание покрыто настройками навигатора по
   умолчанию. Не исправлено: нет однозначного соответствия.
2. **`EntityAIRestrictOpenDoor` (приоритет 4) у клирика и рыцаря** в оригинале
   закрывает двери за собой; в 1.21.1 такой цели нет, а порт её не заменяет.
   Поведение (оставленные открытыми двери) отличается.
3. **Цель «поиск двери»** в 1.21.1 (`NearestAttackableTargetGoal`) по умолчанию
   требует прямую видимость (`mustSee`), оригинальная 1.7.10 — нет; для
   примитивной стрелы порт это уже учитывал отдельно, для культистов
   видимость остаётся нативным требованием.
4. **`EntityCultistPortal.spawnCreature`** вызывает у оригинала
   `playLivingSound()` сразу после спавна; порт полагается на обычный
   ambient-цикл моба.
5. **Порог спавна использует RNG уровня**, а не RNG сущности, как в 1.7.10 —
   ограничение API 1.21.1 (правила спавна получают генератор уровня).
6. **Оригинального `EntityTaintCrawler` в 4.2.3.5 нет вообще**: сущность порта
   пришла из `Thaumcraft-1.12.2-6.1.BETA26.jar` (в дереве оригинала 4.2.3.5
   нет ни `EntityTaintCrawler`, ни `EntityCrystalCluster/EntityCrystalShard`,
   ни `EntityDarkling`, ни `EntityJarGolem`, ни `EntityInfused`, ни
   `EntityFishable`, ни «EntitySpawner»). Сверять их с оракулом 4.2.3.5 нечем.
7. **`EntityGolemBase` (общая база големов) — зона другого агента.** Там
   остаются две ошибки источника урона, найденные при этой сверке:
   * `EntityGolemBase.java:2395` проверяет `DamageTypes.IN_WALL` вместо
     `DamageTypes.CACTUS` — то же прочтение SRG-метки `field_76367_g` по
     карте 1.12 (см. дефект 12);
   * `EntityGolemBase.java:2399` считает «магическим» `DamageTypes.INDIRECT_MAGIC`,
     тогда как `field_76376_m` — это статический `DamageSource.magic`, то есть
     `DamageTypes.MAGIC`.

   Не исправлено здесь намеренно: файл принадлежит зоне `port/ocgolems`,
   чтобы не создавать конфликт при слиянии.

---

## 5. Проверка

```
export JAVA_HOME=/home/Acak/.jdk21; export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=/home/Acak/gradle-homes/ocentities
./gradlew --no-daemon --console=plain --no-build-cache --rerun-tasks :common:test
```

Результат (реальный вывод, принудительный прогон без кэша сборки):

```
> Task :common:compileJava
> Task :common:compileTestJava
> Task :common:processTestResources
> Task :common:testClasses
> Task :common:test
BUILD SUCCESSFUL in 31s
5 actionable tasks: 5 executed
```

Тестовый набор: 1673 теста, 0 падений, 0 пропусков
(`common/build/test-results/test/*.xml`).

Добавленные/изменённые тесты:

* `thaumcraft/entities/EntityParityRulesTest.java` (новый) — радиус вызова
  помощи (follow range и fallback 16) и оба световых гейта (небесный свет,
  block light, порядок розыгрышей).
* `thaumcraft/entities/ProjectileRulesTest.java` — самонаведение шара голема
  теперь делит на квадрат расстояния.
* `thaumcraft/assets/DamageTypeRecordsTest.java` — запись и теги нового типа
  `thaumcraft:indirect_magic`.
* `thaumcraft/assets/VoidRobeAssetsTest.java` — список тега `is_magic`
  дополнен новым типом.

Клиентская приёмка не выполнялась: в этой задаче проверялся серверный контур
(мобы, ИИ, урон, спавн), визуальная часть не затрагивалась.
