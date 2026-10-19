# Базовый Harvest3 — согласование native-интеграции

Спецификация: [исходный контракт](2026-10-19-golem-harvest.md).
Код этой группы пока не начат; текущая сборка ещё относится к Sorting/throws/brain.

## Минимальная игровая группа

Платное ядро Harvest3 обрабатывает ripe wheat/carrot/potato/wart/cocoa,
melon/pumpkin fruits, верхние cane/cactus segments; сохраняет stems/нижний сегмент.
Вещи выпадают обычным native destroy-with-drops. Сам Harvest их не собирает и
не пересаживает без ещё не перенесённого Order-upgrade. Нужен отдельный Gather
или настоящий игрок. Ни ghost inventory, ни GUI, ни метки для работы не нужны.

Transient goal: shuffled9×9 columns around home, Y−3…+3, one column per scan;
путь/дистанция/задержка/strength/hardness/one-target chain из original. Crack stages
и action6 используют native output, а не новый renderer/model/packet.

## Предлагаемое разделение

- Runtime автор: новый `HarvestGolemGoals` и явная allowlist/helper `GolemCrops`;
  `EntityGolemBase` подключает core3/goal/action, но **не** inventory-routing/menu
  предикат. Данные core3 имеют empty filters/wanted0/Fill-modefalse, cargo отдельно.
- Root: item/core/recipe/book/lang/data и Fabric permission bridge. Малый
  `GolemHarvestPlatform` использует тот же install-bridge pattern, что окружающий
  loader adapter, но не вызывает и не меняет Bore machine API/права.
- Hook `GolemHarvestPlatform.harvest(golem,pos,expectedState)` выполняет одно
  native `destroyBlock(...,true,golem)` только после permission и повторной проверки
  той же ripe allowlisted state/current worker/reach/loaded chunks/border/height.
  Отказ/смена состояния не выдаёт drops и не изображает успешный harvest.
- Fabric bridge сообщает стандартные BEFORE/CANCELED/AFTER block-break callbacks
  от unspawned non-OP `FakeThaumcraftGolem`, не от онлайн-владельца. Его личный
  empty tool не подменяет руки/инвентарь настоящего игрока. Native spawn protection,
  claim veto и mobGriefing учитываются; неизвестные actor permissions запрещают
  действие. Путь не выполняет generic right-click/addon Crop API.

Это явная safety-адаптация старого прямого разрушения. Нового resource journal,
NPC inventory/provider framework, имитации игрока с OP или тестовой обвязки нет.
Gameplay/visual/protection acceptance по-прежнему остаётся отдельной проверкой,
не следует из source-спецификации или предыдущей компиляции.
