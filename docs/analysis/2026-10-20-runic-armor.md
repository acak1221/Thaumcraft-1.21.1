# Runic augmentation и armor-first shield

Original oracle: TC4 4.2.3.5 `InfusionRunicAugmentRecipe`, `EventHandlerRunic`,
`ResearchManager:642–644,740–741`, `PacketRunicCharge`, `PacketFXShield`,
`FXShieldRunes`, `ClientTickEventsFML:316,515–560`, `Config:503–511,843–845`.

## Настоящая armor-first реализация

Существующие armor items уже реализуют original `IRunicArmor` с base charge0.
Новый native component `thaumcraft:runic_hardening` хранит signed-byte value
`RS.HARDEN` как bounded integer[-128,127]; disk codec проверяет диапазон,
network codec передаёт ровно signed byte. Output сохраняет всё содержимое
центрального item и записывает `(byte)(hardening+1)`. Final charge — base плюс
signed hardening, без придуманного ограничения24.

`InfusionRunicAugmentRecipe` — настоящий dynamic native recipe type, а не набор
фиксированных metadata outputs. Он требует `RUNICAUGMENTATION`, diamond и
`1+max(finalCharge,0)` отдельных Salis Mundus; один занятый pedestal — один
компонент. Цена `vis=(int)(32D*Math.pow(2D,finalCharge))`: tutamen=`vis/2`,
praecantatio=`vis/2`, potentia=`vis`; instability=`5+finalCharge/2`, Java signed
division. В matrix это обычный prepared STACK output с **нулевой XP ценой**.

Server living tick суммирует настоящие четыре armor slots каждые40 player ticks
или при original global dirty. Максимум ограничивает текущий заряд; при отсутствии
экипировки заряд0. Действительно оплаченный recharge добавляет1 за default2000ms
и50 **centivis** aer+terra, не50vis. Настройки server-only читаются из существующего
`thaumcraft.properties`: `runic_recharge_speed` (минимум500), `runic_recharge_delay`
(минимум0, default80), `runic_cost` (минимум0, default50). Пропущенные keys используют
original defaults. Wall-clock сравнение строго `<`, как в оригинале.

`WandManager.consumeVisFromInventory` объединяет player Repair и Runic оплату:
main slots35→0, один wand полностью оплачивает оба primals, без разделения цены
между жезлами; raw costs учитывают crafting modifier и игнорируют Frugal.
Неполный кандидат не теряет vis. Нулевой discounted cost разрешён original.

Shield обрабатывается после Fortress masks и до MagicWard/native armor. Исключены
ровно drown/wither/out_of_world/starve. При `charge>damage` новый charge —
`(int)((float)charge-damage)`, damage0; иначе оставшийся damage проходит дальше,
charge0. Поэтому даже hit0.5 расходует целую единицу charge1. Depletion запускает
original **общую** rechargeDelay, которая уменьшается на каждый player event,
а не один раз на мировой tick. Этот multiplayer quirk сохранён явно.

Текущий charge сохраняется native player NBT key `thaumcraft:runic_charge`,
заменяя отдельный original `.thaum` файл. **Charge не transient:** original
ResearchManager сохраняет/загружает `Thaumcraft.shielding`. NextCycle, last/max,
global dirty/delay остаются transient. Per-server entity-id map соответствует
original; official1.7.10 `oi.a(mw,int,boolean)` и native `PlayerList.respawn`
сохраняют entityId, поэтому respawn сохраняет тот же runtime state. На disconnect
и server stop state удаляется; нет strong world/player references в значениях.

Client получает только S2C HUD/FX snapshots. Original charge packet имеет8bytes:
int id, signed short charge/max; gameplay остаётся int на сервере. Rune hit packet
содержит victim/attacker ids. Original `func_76346_g` — indirect attacker (`rq/j`),
в отличие от direct source `func_76364_f` (`rq/i`) в Fortress masks: различие
проверено actual official1.7.10 bytecode. Визуал использует retained hemisphere
OBJ, hemis1..15,8..11ticks, исходные scale/alpha/orientation и10-icon HUD.
Tooltip показывает original Runic Shield +charge. Book helper циклически
показывает original пять robe examples hardening0..4 из того же dynamic recipe.

## Доказанное переполнение original и исправление

Вне репозитория выполнен **неизменённый** `AspectList.visSize/getAmount` из actual
TC4 JAR, CodeSource проверен. Harness:
`/workspace/work/thaumcraft-runic-oracle/InspectOriginalVisSize.java`; минимальные
Aspect/Minecraft linking shells не заменяют исполняемые gameplay methods.

| Final charge | Individual vis | True total | Actual original int visSize |
| --- | ---: | ---: | ---: |
| 24 | 536870912 | 1073741824 | 1073741824 |
| 25 | 1073741824 | 2147483648 | -2147483648 |
| 26+ | 2147483647 | 4294967293 | -3 |

Original matrix453 проверяет только `visSize()>0`, затем оплачивает компоненты
и выдаёт output. При fc25+ это воспроизводимый бесплатный essence output.
Native matrix сохраняет **каждый original int cost**, включая double→int saturation,
но проверяет наличие **любого положительного остатка**, не сумму int. В остальных
matrix/transfer/NBT путях суммирования нет: pending/extract/reduce работают с одним
aspect и одной единицей; NBT хранит каждый amount как int. Новый arbitrary fc24 cap
не введён. Native fc25 может реально продвигаться только через source payments.

Прежний matrix safety gate instability0..24 остаётся: fc39 имеетinst24 и положительный
shortage RNG bound2; fc40 имеетinst25 и `nextInt(50-25*2)==nextInt(0)`, original crash
при missing-component branch. Такой ритуал отклоняется **до старта**, а signed-byte
data и его127→-128 wrap сохраняются. Это явная граница из original crash, не скрытая
изменённая цена. Отрицательные signed значения читаются; безопасные instabilities
используют original zero/truncated costs.

## Проверки и честные границы

**Implemented, ещё не заявлено tested/seen** до root build:

- Rules JUnit: цены/final-charge components, signed wrap/division, fc25/fc26 overflow
  vectors, positive demand, inst24/25 RNG boundary, fractional absorption.
- Payload JUnit: original8-byte signed formats и отказ при truncated charge.
- `RunicAugmentationRuntimeChecks`: объявленные altar/devices/essence/armor fixtures;
  обычные matrix ticks делают первый hardening заdiamond+salis+16/16/32essentia,
  без XP, сохраняя wear/name. Отдельный **declared** fc25 input с27 компонентами
  начинает настоящий ритуал и расходует конечный источник; огромный output не
  подставляется. Native BE save/decode сохраняет individual demand/endpoint amount.
  Проверены admission inst24/25 и native ItemStack signed-byte codec paths.
- `RunicShieldRuntimeChecks`: connected survival player и finite wand45aer/45terra
  с thaumium cap(.9; gold имеет original1.0). Embedded transport включён в настоящий
  server connection loop, который вызывает Player living tick; мир отдельно
  увеличивает ServerPlayer.tickCount и сам по себе не доказывает aiStep.
  Только native player ticks создают1 charge. Проверка ждёт natural
  истечения60-tick spawn protection, настоящий hurt0.5 забирает shield без health/
  armor loss. Четыре excluded sources сохраняют charge, пустой wand не пополняет
  его. Native player save/decode восстанавливает объявленный checkpoint charge
  без refund жезлу; это явный NBT replay, не world restart или новый recharge.

**Ещё отсутствует:** четыре Baubles-compatible native accessory slots/menu,
their equip/unequip/death persistence; mundane/accessory/runic8 crafting recipes,
charged/healing/kinetic/emergency modifiers, vis amulet priority. Mob champion/
Eldritch shield FX требуют отдельной связи с их existing absorption path.
Данная группа — реальный shield/augmentation **существующей брони**, не весь runic
accessory port. Client HUD/particles/book ещё не inspected in-game, multiplayer,
world restart и NeoForge этой группой не проверены.

CI4d6a458 found a real native recipe-sync defect: StreamCodec.unit rejects distinct stateless recipe instances created by JSON. The runic serializer now accepts any instance, writes no recipe fields and reconstructs a fresh stateless recipe on decode; MapCodec.unit supplier already accepts independent instances. A focused JSON→native wire round trip covers independently constructed and JSON-decoded recipes. Full update-recipes packet rerun is pending.
