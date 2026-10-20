# Аудит соответствия порта: warp, существа и экология (TC4 4.2.3.5 → 1.21.1)

Дата: 2026-10-21. Домен: `warp_creatures_and_ecology` (121 строка worklist
`.tmp/audit/a2-warp.json`). Источник истины — декомпиляция
`Thaumcraft-1.7.10-4.2.3.5.jar` (`/home/Acak/work/thaumcraft-4.2.3.5-oracle`).
Нативный порт — `common/src/main/java/thaumcraft/` в ветке `audit/a2`.
Ревью: `docs/porting/audit-reviews-a2-warp.json`.

## Итог

| disposition | количество | расшифровка |
|---|---|---|
| `port` | 51 | нативный класс реализует тот же контракт (путь может отличаться) |
| `replace` | 46 | оригинальный класс заменён другой конструкцией (`Rules`-класс + тонкая сущность, зарегистрированный `MobEffect`, `CustomPacketPayload`, датапак-биом) |
| `adapt` | 21 | перенесено с вынужденным отклонением (1.21.1 API, сглаживание пакетов/путей, перенос пакетами) |
| `exclude` | 3 | сознательно не переносится (нет аналога в 1.21.1) |
| **всего** | **121** | |

| native_coverage | количество |
|---|---|
| `parity_verified` | 100 |
| `partial` | 19 |
| `missing` | 2 |

| source_semantics | количество |
|---|---|
| `equivalent` | 40 |
| `adapted` | 78 |
| `not_applicable` | 3 |

Ключевой вывод: **система warp перенесена с числами оригинала один в один** —
`WarpRules.decide` повторяет порядок розыгрышей и формулу
`min(100,(w+w+counter)/3)`, ладдер событий сохраняет все длительности и
усилители (5000 / `min(32000,10*w)` / 1200 / 6000 / `min(40*w,6000)` /
`min(32000,5*w)`), пустые ветки (0, 5..8, 85..88) и точное равенство 76
сохранены. Тaint-экология (блоки, волокна, споры, крейпер, пауки, пчёлы,
конверсия при смерти) перенесена с исходными шансами. Основные проблемы — не в warp,
а в трёх местах: **дефект эффекта champion «grim»**, **не подключённая бутылка
taint** и **перепутанные числа твёрдости/света у блока taint**.

## Расхождения по классам (все, кроме `parity_verified`)

### Клиентские частицы

* `thaumcraft/client/fx/particles/FXWisp.java` — `replace`/`partial`.
  Оригинал: `oracle thaumcraft/client/fx/particles/FXWisp.java:34-140` — шесть
  типов «wisp-искр», звук `random.orb` при появлении, физика и выталкивание из
  блоков (`:188-189`, `:196`).
  Порт: `native common/src/main/java/thaumcraft/client/fx/particles/PrimalWispParticle.java:37-49`
  и `native common/src/main/java/thaumcraft/client/fx/particles/ResourceParticle.java:66-70`
  — типы и размер воспроизведены, но поштучный звук появления и точное
  выталкивание из блоков не воспроизведены один в один.
* `thaumcraft/client/fx/particles/FXWispArcing.java` — `replace`/`partial`.
  Оригинал: `oracle thaumcraft/client/fx/particles/FXWispArcing.java:1-119`.
  Порт: `native common/src/main/java/thaumcraft/client/fx/particles/WispBoltParticle.java:18-33`
  — высота дуги, цветовая шкала и пошаговые розыгрыши оригинала не сверены.
* `thaumcraft/client/fx/particles/FXWispEG.java` — `replace`/`partial`.
  Оригинал: `oracle thaumcraft/client/fx/particles/FXWispEG.java:1-100` (вспышка
  появления eldritch guardian, `ClientProxy:746`).
  Порт: `native common/src/main/java/thaumcraft/client/fx/particles/WarpGuardianParticle.java`
  — визуальный результат другой (частица вместо сущности).

### Блочные рендереры

* `thaumcraft/client/renderers/block/BlockTaintRenderer.java` — `exclude`/`missing`.
  Оригинал: `oracle thaumcraft/client/renderers/block/BlockTaintRenderer.java:1-81`
  — `ISimpleBlockRenderingHandler` 1.7.10 (в 1.21.1 такого хука нет).
  Порт: `native common/src/main/java/thaumcraft/common/blocks/BlockTaint.java:113-116`
  — блок рисуется обычной моделью, капли корки воспроизведены через
  `clientEffects`. Спецрендерер не переносим.
* `thaumcraft/client/renderers/block/BlockTaintFibreRenderer.java` — `exclude`/`missing`.
  Оригинал: `oracle thaumcraft/client/renderers/block/BlockTaintFibreRenderer.java:1-124`
  и оверлей-проход `oracle thaumcraft/client/renderers/block/BlockTaintRenderer.java:33-50`
  (95 % пустого оверлея, 5 % одного из трёх).
  Порт: правило до сих пор считается в
  `native common/src/main/java/thaumcraft/common/lib/world/TaintSpreadRules.java:28-31`,
  но **вызывающей стороны нет** — оверлей не рисуется.

### Блоки

* `thaumcraft/common/blocks/BlockTaint.java` — `adapt`/`partial` — см. «Дефекты».
* `thaumcraft/common/blocks/BlockTaintFibres.java` — `adapt`/`partial` — наборы
  блоков, превращаемых в корку/почву, не совпадают с оригиналом (см. ниже).
* `thaumcraft/common/blocks/BlockTaintFibresItem.java` — `adapt`/`partial`.
  Оригинал: `oracle thaumcraft/common/blocks/BlockTaintFibresItem.java:1-49`
  (1.7 `ItemBlock` с метаданными), ограничение по биому —
  `oracle thaumcraft/common/blocks/BlockTaintFibres.java:355-358`.
  Порт: `native common/src/main/java/thaumcraft/common/blocks/BlockTaintFibresItem.java:14-30`
  и `native common/src/main/java/thaumcraft/common/blocks/BlockTaintFibres.java:135` —
  биом соблюдён, а требование «опора снизу» из легаси-референса 1.12 в оракуле
  отсутствует и в порт не перенесено.

### Прочее по taint

* `thaumcraft/common/blocks/world/taint/TaintHelper.java` (legacy-only) —
  `replace`/`partial`: фасад TC6 из 1.12 (`addTaintSeed`, `spreadFibres`) в порту
  отсутствует; обе функции покрыты
  `native common/src/main/java/thaumcraft/common/blocks/BlockTaintFibres.java:84-93`.
* `thaumcraft/common/config/MaterialTaint.java` — `exclude`/`partial`: в 1.21.1
  нет `net.minecraft.block.material.Material`; наблюдаемые свойства (твёрдый,
  не заменяемый, не жидкий, цвет карты GRASS) выражены через
  `native common/src/main/java/thaumcraft/common/config/ThaumcraftTaint.java:71`,
  но «mobility flag» (`func_76227_m`) не воспроизведён.

### Champion

* `thaumcraft/common/entities/monster/mods/ChampionModifier.java` — `replace`/`partial`:
  сам мод списка и триггеров совпадает, но есть два расхождения (см. «Дефекты»)
  и один пропуск в whitelist.

### Пакеты 1.12/TC6 (legacy-only)

`thaumcraft/common/entities/monster/tainted/EntityTaintCrawler.java`,
`EntityTaintSeed.java`, `EntityTaintSeedPrime.java`, `EntityTaintSwarm.java`,
`EntityTaintacle.java`, `EntityTaintacleSmall.java` — `adapt`/`partial`: в
оракуле TC4 4.2.3.5 этих классов нет вообще (это контент Thaumcraft 6),
в порту они лежат в сглаженном пакете
`native common/src/main/java/thaumcraft/common/entities/monster/`. Числа,
пришедшие из 1.12, отделены от чисел оракула в
`native common/src/main/java/thaumcraft/common/lib/taint/TaintAnimalRules.java:9-13`.

### Предметы

* `thaumcraft/common/items/ItemBottleTaint.java` — `adapt`/`partial` — см. «Дефекты».

### Система warp

* `thaumcraft/common/lib/WarpEvents.java` — `replace`/`partial`: вся арифметика
  совпадает, но есть одно расхождение по составу «warping gear» (см. ниже) и
  непроверенный пункт по ResearchManager.

## Отклонения, требующие решения

1. **Сумма warp от «warping gear».** Оригинал делает
   `w = EventHandlerRunic.getFinalWarp(рука) + 4 слота брони + 4 слота baubles`
   (`oracle thaumcraft/common/lib/WarpEvents.java:327-337`).
   Порт делает `WarpGear.fromGear` = рука + 4 слота брони
   (`native common/src/main/java/thaumcraft/common/lib/WarpGear.java:21-27`),
   слотов аксессуаров в 1.21.1-порту нет. Практического расхождения пока нет:
   ни один предмет baubles в TC4 не реализует `IWarpingGear` (только пустотные
   инструменты/броня, кровавый меч, первобытный дробил, одежда культиста и
   пустотная одежда — `oracle thaumcraft/common/items/equipment/ItemVoidAxe.java:49`
   и т. п.), но контракт сузился.
2. **Побочный урон у босса-«warded».** Оригинал лечит промоута на
   `(int)(значение атрибута MAX_HEALTH)/2` — то есть уже с учётом бонуса +30
   (`oracle thaumcraft/common/lib/utils/EntityUtils.java:271-274`).
   Порт лечит на `getBaseValue()/2`
   (`native common/src/main/java/thaumcraft/common/lib/mods/ChampionRules.java:238`),
   то есть на 10 вместо 25 у зомби.
3. **Бонусы атрибутов champion.** Оригинал: `CHAMPION_DAMAGE = 2.0` с операцией 2
   (умножение итога), `BOLDBUFF = 0.3` с операцией 1 (умножение базы),
   `MIGHTYBUFF = 3.0` с операцией 2
   (`oracle thaumcraft/common/lib/utils/EntityUtils.java:61-63`).
   Порт переводит их в `ADD_MULTIPLIED_TOTAL` с амплитудой «минус один»
   (`native common/src/main/java/thaumcraft/common/lib/mods/ChampionRules.java:221-237`):
   урон ×2 и ×3 совпадают, но множитель скорости ×1.3 в порту действует и на
   итог, а в оригинале — только на базовое значение.
4. **Whitelist champion.** Оригинал ищет по классу и включает
   `EntityThaumcraftBoss` (уровень 200), то есть любой босс всегда становится
   champion (`oracle thaumcraft/common/config/ConfigEntities.java:195-208`).
   Порт ищет по id реестра и перечисляет боссами только `eldritch_warden` и
   `eldritch_golem` (`native common/src/main/java/thaumcraft/common/lib/mods/ChampionRules.java:61-82`),
   а `EntityCultistLeader` в порту — обычный `Monster`, не `IThaumcraftBoss`
   (`native common/src/main/java/thaumcraft/common/entities/monster/EntityCultistLeader.java:57`).
   Практор в оригинале всегда champion, в порту — никогда.
5. **Наборы блоков taint-корки и taint-почвы.** Оригинал:
   `oracle thaumcraft/common/blocks/BlockTaintFibres.java:173-179` —
   корка из `Utils.isWoodLog` или материала `field_151572_C`/`field_151570_A`,
   почва из `field_151595_p`/`field_151578_c`/`field_151577_b`/`field_151571_B`.
   Порт: `native common/src/main/java/thaumcraft/common/lib/world/TaintMaterialRules.java:16-18`
   — корка = брёвна + кактус/тыква/арбуз, почва = земля/песок/трава/мицелий/пашня/глина/гравий/дурная песчаная почва.
   Легаси-референс 1.12 трактует тот же оригинальный код как
   «дерево + листва» и «трава/земля/камень/песок/глина»
   (`src/main/java/thaumcraft/common/blocks/BlockTaintFibres.java:112-131`).
   Идентичность полей `Material.field_*` восстановить не удалось (MCP-карты на
   машине нет), поэтому это расхождение зафиксировано как нерешённое.
6. **Урон taint-бутылки по tainted-мобам.** Оригинал исключает `ITaintedMob` и
   нежить (`oracle thaumcraft/common/entities/projectile/EntityBottleTaint.java:56-64`).
   Порт исключает только творческий режим и носителей уже активного эффекта
   (`native common/src/main/java/thaumcraft/common/items/ItemBottleTaint.java:58-65`),
   поэтому tainted-моб получает эффект, который его лечит.
7. **Смерть при taint-отравлении.** Оригинал вызывает `setDead()` на умирающем
   (`oracle thaumcraft/common/lib/events/EventHandlerEntity.java:481-507`),
   порт даёт мобу умереть штатно и добавляет близнеца
   (`native common/src/main/java/thaumcraft/common/entities/monster/TaintDeathConversion.java:46-67`),
   из-за чего дроп умирающего моба сохраняется. Это задокументированное
   вынужденное отклонение.
8. **Ограничения торговли печа.** Оригинал крутит бесконечный цикл подбора
   награды (`oracle thaumcraft/common/entities/ContainerPech.java:114-154`),
   порт ограничивает 128 раундами и 512 отклонениями по тиру и отказывает в
   сделке, если награда не помещается
   (`native common/src/main/java/thaumcraft/common/lib/pech/PechTrade.java:17-65`).
   Это отклонение зафиксировано в коде порта.

## Дефекты

1. **Champion «grim» накладывает слабость вместо иссыхания.**
   Оригинал: `oracle thaumcraft/common/entities/monster/mods/ChampionModGrim.java:26-30`
   — при 40 % шансе `new PotionEffect(Potion.field_82731_v.field_76415_H, 200)`
   (это **wither**, 200 тиков).
   Порт: `native common/src/main/java/thaumcraft/common/lib/mods/ChampionModGrim.java:11`
   — `Status.WEAKNESS` на 200 тиков.
   Внутренняя непоследовательность порта: то же самое оригинальное поле
   `Potion.field_82731_v` в самом порте трактуется как wither в
   `native common/src/main/java/thaumcraft/common/lib/WarpEvents.java:177`
   (`MobEffects.WITHER`), то есть один и тот же эффект оригинала в порте
   переведён двумя разными способами.
   Воспроизведение: подойти к champion-grim, ударить — цель получает
   Weakness, а оригинал даёт Wither.

2. **Бутылка taint не бросается, а «взрывается» в точке взгляда.**
   Оригинал: `oracle thaumcraft/common/items/ItemBottleTaint.java:55-65` —
   предмет создаёт `EntityBottleTaint`, который летит и взрывается в точке
   падения (`oracle thaumcraft/common/entities/projectile/EntityBottleTaint.java:29-70`).
   Порт: `native common/src/main/java/thaumcraft/common/items/ItemBottleTaint.java:38-75`
   — бутылка взрывается в блоке, куда смотрит игрок (clip 4 блока), а
   `EntityBottleTaint`, который в порту **зарегистрирован**
   (`native common/src/main/java/thaumcraft/common/config/ThaumcraftProjectiles.java:28-29`)
   и полностью реализован
   (`native common/src/main/java/thaumcraft/common/entities/projectile/EntityBottleTaint.java:60-140`),
   никогда не спавнится — это мёртвый код.
   Побочный эффект: комментарий в классе
   `native common/src/main/java/thaumcraft/common/items/ItemBottleTaint.java:29-30`
   («EntityBottleTaint … not part of this port») устарел.

3. **У блока taint перепутаны твёрдость и свет, потеряно сопротивление взрыву.**
   Оригинал: `oracle thaumcraft/common/blocks/BlockTaint.java:71-72` —
   `setHardness(2.0f)` и `setResistance(10.0f)`;
   `oracle thaumcraft/common/blocks/BlockTaint.java:248-260` — блок **светит**
   (1.75 корка, 1.5 почва, 0.2 плоть).
   Порт: `native common/src/main/java/thaumcraft/common/blocks/BlockTaint.java:42` —
   `strength(metadata==0?1.75F:metadata==1?1.5F:.2F, 6F)`, то есть значения
   света попали в слот твёрдости, сопротивление взрыву 6.0 вместо 10.0, а
   свечения блока нет вовсе (в
   `native common/src/main/java/thaumcraft/common/config/ThaumcraftTaint.java:40-41`
   `lightLevel` не задан).
   Воспроизведение: разбить корку taint — время добычи соответствует
   твёрдости 1.75, а не 2.0, взрывоустойчивость ниже оригинальной, блок не
   светится.

4. **Бонус урона от фокуса «hellbat» и от `damBonus` теряется.**
   Оригинал: `oracle thaumcraft/common/items/wands/foci/ItemFocusHellbat.java:109`
   пишет `damBonus` в моба, а
   `oracle thaumcraft/common/entities/monster/EntityFireBat.java:98` и
   `oracle thaumcraft/common/entities/monster/EntityTaintSwarm.java:100`
   добавляют его к урону.
   Порт: `native common/src/main/java/thaumcraft/common/entities/monster/EntityFireBat.java:60`
   и `native common/src/main/java/thaumcraft/common/entities/monster/EntityTaintSwarm.java:62`
   регистрируют фиксированный урон; поле `damBonus` сохраняется, но в атрибут
   не попадает.

## Не перенесено

* `thaumcraft/client/renderers/block/BlockTaintRenderer.java` и
  `thaumcraft/client/renderers/block/BlockTaintFibreRenderer.java` — в 1.21.1 нет
  `ISimpleBlockRenderingHandler`; оверлей волокон (95/5) не рисуется, хотя его
  правило ещё считается в `TaintSpreadRules.overlay`.
* `thaumcraft/common/config/MaterialTaint.java` — в 1.21.1 нет класса `Material`;
  переносимы только его наблюдаемые свойства, «mobility flag» потерян.
* Слотов baubles в `getWarpFromGear` в порту нет (в оригинале — четыре слота).
* Внутренние классы пакета `thaumcraft/common/entities/monster/tainted/*`
  (TC6-контент) не имеют соответствия в оракуле TC4 4.2.3.5 — в порту они
  остались 1.12-реализацией в сглаженном пакете.

## Что НЕ удалось проверить

1. **Идентичность полей `Material.field_1515xx`** оригинала (1.7.10) — на
   машине нет MCP-карт и декомпиляции ванильного 1.7.10, поэтому не удалось
   однозначно сопоставить `field_151570_A`, `field_151571_B`, `field_151572_C`,
   `field_151577_b`, `field_151578_c`, `field_151595_p` с блоками; расхождение
   по наборам taint-корки/почвы зафиксировано как нерешённое (п. 5 в
   «Отклонениях»).
2. **Точные строки торговых таблиц печа** (`EntityPech.tradeInventory`) — сверены
   алгоритм и вероятности, но не каждый предмет каждого тира.
3. **Поведение 787-строчного `EntityPech`** (цели, бормотание, деревенские
   gossip-цели, ИИangry) — построчно не сверялось.
4. **Точные числа частиц** `FXWisp`/`FXWispArcing`/`FXWispEG` (высота дуги,
   шкалы цвета, пошаговые розыгрыши) — воспроизведены приблизительно.
5. **Разделение warp при получении исследования.** Оригинал добавляет perm/sticky
   warp внутри `ResearchManager.completeResearch`; порт делает это в
   `WarpEvents.checkWarpEvent` по описанию из каталога исследования. Совпадение
   не сверялось с `ResearchManager` (домен research).
6. **Клиентская картинка** (текстуры, модели, оверлеи) не проверялась в игре —
   аудит читал код, а не запускал клиент.
7. **GameTest/смоук** не запускались: это аудит чтением, сборка и прогон
   выполняются на стороне root-сессии.
