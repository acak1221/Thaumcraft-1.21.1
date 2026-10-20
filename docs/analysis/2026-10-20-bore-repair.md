# Arcane Bore Repair — original контракт и bounded native extension

Источник: TC4 4.2.3.5 private oracle вне репозитория,
`TileArcaneBore:195–253`, `ResearchManager.reduceToPrimals`, `AspectList.merge`.
Это отдельный потребитель raw VisNet; player Repair оплачивается жезлом.

## Original поведение

При установленной кирке server тик после обычного mining branch увеличивает
transient counter. Условие попытки — **старое** значение `counter++ % 40 == 0`
и damaged pick. Поэтому первая попытка происходит до первого пополнения.
Redstone OFF, отсутствие focus и nearly-broken gate не запрещают ремонт.

Repair level ограничен2. Только `IRepairable` получает object **base** tags,
SUM reduction в primals и стоимость `floor(sqrt(amount*2))*level` raw vis.
Это не centivis conversion и не стоимость с crafting discount. Стоимость
каждого primal объединяется через **MAX** с прежним значением: более дешёвая
кирка не понижает уже накопленную стоимость. При forced Repair на non-repairable
кирке стоимость очищается. Отсутствие enchantment оставляет прежнюю стоимость.

Optional `IRepairableExtended.doRepair` вызывается перед оплатой на native
machine actor. Когда все сохранённые primals полностью оплачены, резерв
уменьшается на всю стоимость, wear уменьшается на1/2 с минимумом0.

После попытки, при **новом** counter кратном5, для каждого недостаточного primal
VisNet запрашивает **полную сохранённую стоимость**, а не недостающее количество.
Несколько ограниченных pulse grants могут переполнить требуемый остаток.
Counter, MAX cost и накопленный reserve **никогда не записываются в NBT**.

## Native реализация

Использует существующий `EssentiaTransfers` executor: дополнительная REPAIR
operation и native phase, immutable `BoreRepairState` в прежнем WorkSnapshot,
private-constructor Change и прежний raw commit. Публичный запрос не даёт
native ticker authority. Оплата кирки и резерва применяется одним закрытым
изменением. VisNet списывает реальные source grants; после каждого callback
проверяется тот же current owner/lifetime/revision/snapshot. Уже списанный grant
при замене owner не возвращается и не зачисляется новому owner.

Ремонт выполняется после прежнего serverTick, включая его обычные ранние
возвраты OFF/no-focus. Quarantined/retired/reconciling/exhausted-revision owners
не получают новый write capability. Изменение не расширяет mining whitelist.

## Проверки и пределы доказательства

**Implemented, ещё не заявлено tested/seen** до запуска root:

- `BoreRepairStateTest`: post-increment attempt0/40 vs fill5..40; partial funding
  не оплачивает ремонт; full-cost fill может оставить overfill; MAX сохраняется
  после уменьшения цены, immutable read maps.
- `BoreRepairRuntimeChecks.run`, отдельный wrapper timeout200: existing Bore и
  VisNet fixtures, actual raw node16x6 проходит1001 production converter updates
  и становится источником с4-unit pulse capacities. Затем **только обычные world
  ticks** питают реальную damaged Thaumium Pickaxe RepairII при OFF/no focus.
  Исходные зачарованная кирка, raw node и устройства — объявленные input fixtures,
  не survival acquisition. Результат/phase/reserve/credit не подставляются.
- Native pick base проверяется как metallum9/praecantatio4/arbor1, bill как
  aer6/terra12/ignis4/aqua2/ordo10/perditio4. Первый terra4 partial grant не снижает
  wear. Успешная попытка тратит весь bill, сохраняет исходный aer/ordo overfill.
  Отдельный прежний normal prefill расходует raw perditio104 на float acceleration
  `20.799997` (bits`0x41a66665`):25 последовательных `.8F` дают ещё значение<20,
  поэтому original threshold допускает26-й pulse. Disk short сохраняет20.
  mining/output не происходит. Нет общего конечного запаса у renewable node:
  ограничен каждый его pulse, и reserve получает только реально выданные grants.
- Native BE save/decode применяется к тому же idle installed test BE и проверяет
  wear/credit вместе с обнулением counter/cost/reserve. Это не world restart и
  не disk acquisition replay. Существующие fixtures отвечают за exact cleanup.

Root добавляет wrapper и публикует фактические compile/JUnit/server результаты.
Client GUI/visuals, multiplayer и NeoForge здесь не проверены.

**Видимые оставшиеся границы:** native mining по-прежнему допускает прежний
закрытый набор обычных blocks и plain/Thaumium picks. Repair работает с установленным
`IRepairable` pick, но original вызов `installedPick.onUpdate(fakePlayer,0,true)`
ещё не перенесён. Поэтому innate Void/Elemental tool effects внутри Bore этим
изменением не заявлены. Runic accessories, vis amulet priority и Hover Harness
остаются отдельными задачами. Общего нового actor/ticker/smoke framework нет.
