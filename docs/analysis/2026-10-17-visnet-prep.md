# Настоящий VisNet — подготовка после M06e

**Read-only dependency audit; не реализация/сборка/приёмка самой сети.**
[M06e](2026-10-17-crystallizer.md) принят/экспортирован как unassisted baseline;
сразу начат [M09a basic stabilizer](2026-10-17-stabilizer.md), без powered network.
Принятый кристаллизатор не размораживается ради этого audit. Полный разбор, hashes и native/client
матрица: `.tmp/visnet-dependency-closure.md` (B), original4.2.3.5 + закреплённый
OpenTC4 как читаемый reference, без копирования donor implementation.

## Минимальная игровая цепь

Обычный узел → **basic stabilizer снизу + powered transducer сверху** → настоящий
energized source → relay → EARTH consumer. Не требуется advanced construct,
infusion, advanced stabilizer, node jar, amulet или manipulator. Нельзя выдавать
непосредственный drain обычного узла/жезла за VisNet. В первой network-приёмке
нужен настоящий relay и consumer вне direct source range8, иначе source замаскирует
отсутствующую сеть. Четыре новые block/BE identities, три arcane recipes, ordinary
node lock, conversion/reversion/failure, graph lifecycle/FX и отдельное изменение
ускорения кристаллизатора — реальная closure, не один always-zero метод.

| Recipe / gate / whole wand vis | Исходные входы |
| --- | --- |
| NodeStabilizer / NODESTABILIZER / AQ32,TERRA32,ORDO32 | `" G "/"QPQ"/"SNS"`: gold1, quartz BLOCK2, piston1, arcane stone BRICKS2, Nitor ITEM1 |
| NodeTransducer / VISPOWER / IGNIS32,AER32,PD32 | `RCR/ISI/RAR`: redstone BLOCK4, comparator1, literal iron2, stabilizer1, Nitor ITEM1 |
| NodeRelay **x2** / VISPOWER / IGNIS8,ORDO8 | `" I "/"ISI"/" I "`: literal iron4, balanced shard1 |

Работающей tower нужны **два** crafted stabilizers: один ingredient transducer,
второй остаётся под узлом. NODESTABILIZER parentNODEPRESERVE, VISPOWER primary
Special parentNODESTABILIZER; не Secondary. Полная native research acquisition
не следует из completion fixtures будущего focused client. Masonry/Nitor/balanced
recipes уже существуют, но реальные finite crafting inputs надо объявить/получить.

## Важные контракты до implementation

- Raw VisNet units **не whole wand-vis**: charger вызывает addRealVis напрямую.
  Crystallizer использует `1+2*actualDrain`, без ×100/÷100. Powered node выдаёт
  возобновляемый budget каждого actual SOURCE tick; unused supply не накапливается.
  Conservation проверять внутри refill epoch, не объявлять это конечным топливом.
- Исходное преобразование BASE compound aspects использует MAX, не SUM по ветвям.
  Существующий `AspectList.reduceToPrimals` даёт другой результат на overlaps
  (terra9/metallum16: original16/16, current41/16). Не менять принятый API скрытно
  сейчас: нужен отдельно назначенный exact reducer или reviewed follow-up.
- Warm-up из count0: после1000 updates узел ещё normal, conversion на1001. Reversion
  после unpower из1000 происходит на951 updates, current normal становитсяZERO,
  base/type/modifier сохраняются. Count/status persisted, не reset как M06e timer.
- Basic lock реально влияет на bullying и recharge interval×2; advanced×20 —
  позднее. Decorative stabilizer без ordinary-node integration не закрывает unit.
- Failure при уничтожении/питании stabilizer или удалении converter: node removed
  FIRST, nonterrain explosion3,50trials flux metadata8. **Это9quanta**, не8.
  Current FluxFiniteBlock LEVEL0..7 этого не выражает; нужен отдельный exact
  nine-quanta birth/flow/save adapter, не clamp7 или подмена обычным spill().
  [Отдельный audit](2026-10-17-flux-overfull-prep.md) определил LEVEL0..8/put0..9
  при сохранении capacity8 и различий AIR/water/swap; это ещё НЕ implemented.
  Там же зафиксированы existing gasNausea-vs-Wither mapping и goo-tail gaps,
  не исправляемые скрытно в текущем stabilizer checkpoint.
- Relay support/placement/attunement/LOS/range/root selection/tick40 refresh не
  заменять generic transport. Shards не расходуются; colour — parent compatibility,
  не aspect filter. Consumer drain не получает лишний LOS/research/owner gate.
- Graph/cache per server-level lifetime/dimension, exact live BE identity, no
  force-load, cycle/duplicate-safe traversals, lifecycle invalidation, actual debit
  before notification/FX. Chunk merely unloaded — не уничтоженный tower support.
  Full addon provider atomicity и файловая power-loss exactly-once не следуют из
  первого native source/relay gate.

Возможный bounded порядок: сначала basic stabilizer + ordinary-node lock с реальным
recipe/render/gameplay checkpoint (НЕ power claim), затем transducer/energized/
relay/real network/EARTH integration вместе. Решение после M06e acceptance/export;
Выбран первый bounded шаг: M09a толькоbasic stabilizer/ordinary node; powered
conversion/network остаются отдельной будущей unit. Audit не задерживает текущие
owner freeze/центральную проверку и не доказывает fullVisNet.
