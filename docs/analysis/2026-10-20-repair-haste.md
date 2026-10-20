# Repair и Haste — original контракт и native реализация

Источник поведения: TC4 4.2.3.5 private oracle вне репозитория: `EnchantmentRepair`,
`EnchantmentHaste`, `EventHandlerEntity:245–339`, `WandManager:104–120`,
`Config:275–280`, `ConfigRecipes:298–299`. Retained `src/` и `object_aspects.json`
не заменяют original спецификацию.

## Зачарования и рецепты

- Repair: max II, weight2, min cost `20 + 10*(level-1)`, max cost `61 + 10*(level-1)`;
  damageable `IRepairable`, несовместим с Unbreaking в обе стороны.
- Haste: max III, weight3, min cost `15 + 9*(level-1)`, тот же max cost;
  доступные native boots. Original Hover Harness добавляется вместе с его собственным item/effect path.
- Max cost использует original superclass min cost `1 + level*10`, а не собственный min cost подкласса.
- Recipe `InfEnchRepair`: anvil + Salis Mundus, instability4, praecantatio8/fabrico10/ordo10,
  первый уровень требует6 XP levels, второй12.
- Recipe `InfEnchHaste`: Nitor + Salis Mundus, instability3, praecantatio4/iter8/volatus8,
  XP levels5/10/15.
- Native dynamic enchantment registry/datapack заменяет числовые id150/151. Definitions используют
  original localization keys. `non_treasure` добавляет table/books/trades/loot availability через native tags.
  Supported item tags содержат33 существующих repairable items и9 существующих boots.
  Matrix использует тот же `InfusionEnchantmentRecipe` engine, что и22 vanilla targets.

## Repair runtime

Server-side каждые40 player ticks, вне creative: только hotbar0..8, затем armor feet/legs/chest/head.
Это original `InventoryPlayer.func_70451_h()==9`, не весь main inventory. Item должен быть damaged
`IRepairable`, уровень Repair ограничен2.

`ObjectAspectResolver.baseTags(ItemStack, Level)` выделяет original `getObjectTags`: recipe-derived/direct
base и dynamic wand/potion tags, cap64 на аспект. Возвращает отдельный mutable list без container,
equipment/enchantment bonuses и без cull6. Прежний `get()` вызывает baseTags и сохраняет порядок бонусов.
Не добавлены ложные direct bindings инструментов из retained1.12 corpus: actual TC4 получает их base из рецептов.

SUM reduction в primals использует принятую original arithmetic helper `FocalUpgradeCosts.reduceToPrimals`.
Стоимость каждого primal — `floor(sqrt(amount*2))*min(level,2)` **centivis**, без умножения на100.
Optional `IRepairableExtended.doRepair` вызывается до оплаты; false запрещает ремонт.
Жезлы проверяются с main slot35 до0: один жезл должен полностью оплатить все primals. Неполный кандидат
не теряет vis. Стоимость учитывает cap/armor/exhaustion как crafting, игнорируя Frugal; original truncation
может дать0 centivis. После оплаты уменьшается текущий wear на1/2, минимум0.

## Haste runtime и проверка old field

Original `EventHandlerEntity` проверяет **field_70160_al**, то есть `isAirBorne`, а не onGround.
FML1.7.10 `conf/fields.csv`/`joined.srg` подтверждают `sa/al` и отдельно `field_70122_E=onGround`.
Official1.7.10 EntityTrackerEntry читает/сбрасывает `sa.al` в тех же ветках, в которых native1.21.1
ServerEntity читает/сбрасывает `Entity.hasImpulse`. Поэтому эффект использует native **hasImpulse**.
Retained1.12 `src/` применял другой onGround branch; он не перенесён.

Haste действует на обеих сторонах до travel, при forward input>0 и !abilities.flying. Бонус `level*.015f`,
вдвое при hasImpulse, затем ещё вдвое в воде. Original `moveFlying(0,1,bonus)` преобразует yaw как
FLOAT `yaw*PI/180`, перемножает float sin/cos и только потом прибавляет к double velocity. Это сохранено
явно, поскольку modern `moveRelative` выполняет vector products в double.

Для yaw37/levelIII actual official1.7.10 `qh` выполнен reflection-ом вне репозитория:
angle bits `3f25514e`, sin `3f1a0d51`, cos `3f4c75d0`, deltaX `bcddd5bc`, deltaZ `3d13361a`.
Official client SHA1 `e80d9b3bf5085002218d4be59e668bac718abbc6`.
FML mapping SHA256: fields `1d0d123cd80df147d613dc32c265e19daa594608ced33aa3b485162b2e5f6ade`,
joined.srg `43d4376110b2638213d3096f80f8395c076eb05b58d6c0dbd22c0d999b37a357`.
Все oracle файлы/исполняемые помощники находятся вне репозитория.

## Evidence и границы

**Implemented, ещё не заявлено tested/seen:** definitions, tags,24 total infusion-enchantment recipes,
player effects, baseTags API, IRepairableExtended, common tick mixin class. Shared mixin registration,
book aliases и Fabric GameTest wrappers интегрирует root.

`EquipmentEnchantmentRulesTest` проверяет original float bits, square-root-before-level и centivis rounding.
`EquipmentEnchantmentRuntimeChecks` использует объявленные enchanted/damaged input fixtures и подключённого
survival player: focused actual Haste velocity/gates, затем **обычные player ticks** для ремонта hotbar/armor,
точной оплаты одним конечным жезлом, сохранения slot9/offhand и неполного кандидата. На thaumium sword base
проверяется original recipe-derived metallum6/praecantatio3, отдельно от бонусов.

`CustomEnchantmentInfusionRuntimeChecks` выполняет оба ритуала **полностью обычными matrix ticks**:
точные XP before essentia, native component payment, finite jar balances и actual enchantment outputs.
Formed altar/stabilizers/resources/XP объявлены fixtures; готовые результаты не подставляются.
Embedded transport для player-effect test включён в настоящий native connection loop: именно packetListener.tick вызывает doTick/Player.aiStep; world tickCount без этого не доказывает living hook.
Все источники установлены до первого cache discovery. Наблюдатели регистрируются до обхода GameTest scheduled map.

Итоговые build/JUnit/server результаты добавляет root после запуска. Client movement/UI, multiplayer,
world restart и NeoForge этим source checkpoint не проверены.

**Missing integrations:** original vis amulet/Baubles priority, Hover Harness, Traveller's Boots, отдельный
Bore Repair VisNet path. Их наличие не следует из player Repair/Haste реализации. Runic accessories/shielding
также остаются самостоятельной следующей группой.
