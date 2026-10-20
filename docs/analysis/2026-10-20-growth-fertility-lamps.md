# Growth / Fertility Lamps — контракт TC4, 20.10.2026

Источник — оригинал TC4 4.2.3.5, CFR вне репозитория: `TileArcaneLampGrowth`,
`TileArcaneLampFertility`, `CropUtils`, `Config.initMisc`, `BlockMetalDevice`
metadata8/13, `BlockMetalDeviceItem`, `TileArcaneLampRenderer`, `ConfigRecipes:377–378`,
`ConfigResearch:219–220`, `PacketFXBlockSparkle`, `ClientProxy.blockSparkle/particleCount`.

## Growth

Начальные charges−1, reserve=false. Если charges≤0, имеющийся reserve превращается
в100 активных зарядов; иначе лампа пытается взять herba1. Затем отдельным вызовом
пытается пополнить пустой reserve ещё одной herba. `drawDelay` увеличивается на каждый
**вызов**, а не на тик; только каждый пятый вызов делает запрос транспорта. Поэтому
у пустой лампы первый платёж происходит в тик3, резерв — в тик7. Если после попыток
charges всё ещё0, исходный код меняет его на−1. Нет редстоун-паузы.

Сторона трубы — facing к опоре, то есть opposite clicked face. На этой стороне
сосание128, пока нет резерва или charges≤0; иначе0. Фиксированный запрашиваемый аспект
herba. Исходный draw проверяет connectable/output и строго меньшую suction источника,
затем exact take1; он не запрашивает representative type, количество или minimum
suction. Публичные add/take/amount/type лампы — соответственно0/0/0/null: заряды
не являются доступным транспортным резервуаром.

При положительных charges каждый тик сканирует одну колонку из перемешанного списка
13×13. Колонка обходится сверху+6 до−6, но растение должно быть строго внутри сферы
radius6 (`distanceSquared<36`). Для первого подходящего нематёрого растения списывается
один заряд и планируется **один block update с задержкой1**, а не bonemeal. Зрелые
стебли специально не исключаются, пока они могут попытаться породить плод. Исключаются
grass material, зрелые культуры, cocoa age≥2, wart age≥3, все небазовые сегменты
сахарного тростника/кактуса и lamp blacklist. Изменение последней целевой block/meta
обнаруживается последующим положительным тиком и отправляет зелёные block sparkle
0x40ff40 игрокам в32 блоках. Частицы — исходные atlas112..120,7 при reduced,14 при all,
0 при minimal, исходные задержка/срок/масштаб; это только клиентский S2C эффект.

Доказанный crash оригинала: `checklist.remove(0)` предшествует чтению нового
`checklist.get(0).y`; при169-й колонке список пуст. С разрешения ведущего используется
Y удалённой колонки. У всех169 колонок Y одинаковый, поэтому поведение остальных168
проходов сохранено. Перемешивание повторяет нисходящие Fisher–Yates draws
`Collections.shuffle`, используя нативный world RandomSource.

## Fertility

Начальные charges0, накопление victus1 раз в5 вызовов до charges4. Суccion стороны
опоры — `128−charges*10`, в том числе88 при полном запасе4. Automatic draw прекращается
при4. Публичного извлечения/добавления топлива нет.

`count` увеличивается только при charges>1. Проверка `count++%300==0` означает первую
попытку сразу при получении второго заряда, затем каждые300 подходящих тиков.
Лампа смотрит животных в AABB собственного блока, расширенном на7 во все стороны.
Кандидаты — age0 и не in-love. Группа строится по **точному runtime class**, включает
также детей и уже влюблённых при подсчёте плотности; при размере>7 пропускается.
Первой подходящей паре списываются два заряда, обоим вызывается `setInLove(null)`,
после чего обход прекращается. Топливо не создаёт ребёнка напрямую и не кормит детей.

Обе лампы имеют исходную форму4,2,4..12,14,12, hardness3/resistance10,2, металл,
исходные active/off текстуры и сопло в шести направлениях. Свет15 при charges>0,
иначе8. Только Growth находится в исходной neighbor support-removal ветке;
Fertility сохраняется после удаления опоры — эта особенность сохранена.

## Рецепты и данные

- `LampGrowth` / `LAMPGROWTH`: центральная arcane lamp, компоненты gold, bone meal,
  earth shard, gold, bone meal, earth shard; instability4; herba16,lux8,victus16.
- `LampFertility` / `LAMPFERTILITY`: arcane lamp, gold,wheat,fire shard,gold,golden carrot,
  fire shard; instability4; bestia16,victus16,lux8.

Нативные id — `growth_lamp` / `fertility_lamp`, отдельный
`ThaumcraftBiologicalLamps`. Facing хранится в block state и дублируется исходным NBT
orientation; charges и reserve сохраняются. Счётчики draw/count, scan checklist и
последний target остаются исходными transient полями. Неверные типы/диапазоны платного
состояния сохраняются как `RejectedEssentiaLamp` и выключают лампу, без бесплатного
clamp/refill. ACTIVE — современный light/model cache фактического charges>0;
при оплате/расходе cache синхронизируется с нативным block state.

## Вынужденные адаптации

Удалённый Material/IGrowable заменён native BonemealableBlock, BushBlock, Vine,
cane/cactus и соответствующей проверкой зрелости. Оригинальный IMC lamp blacklist
представлен пустым по умолчанию data tag `thaumcraft:growth_lamp_blacklist`.
Сканирование работает только в уже загруженной области и внутри высоты/границы мира.

В1.21.1 CropBlock растёт в `randomTick`, а его обычный scheduled `tick` пуст.
`GrowthLampTicks` — persistent SavedData адаптер исходного +1 block update: захватывает
block identity и срок, дедуплицирует координаты+блок как исходный block scheduler,
после срока вызывает **реальный randomTick**, если в уже загруженной клетке остался
тот же block. Смена только возраста не отменяет update, удаление/замена блока отменяет.
Запись переживает удаление лампы и save/load; отсутствующий chunk не загружается ради
растения. Каждый вызов лампы оплачивает заряд даже при уже существующей одинаковой
записи, как исходное безусловное списание перед deduplicated scheduleBlockUpdate.
Закрытый точный native frame списывает charge перед публикацией оплаченного задания
и до уведомлений; публичный scheduler не может создать задание вне подготовленной
фазы финального `TileArcaneLampGrowth`.

Существующий atomic executor получил только закрытый профиль LAMP для двух финальных
нативных плиток, с обязательной native draw phase. Реальный источник и credit100/reserve1
или fertility charge1 меняются callback-free до любых source/receiver уведомлений.
Публичный общий callback/credit API не добавлен. Неподдерживаемые transport addons
не получают обещания атомарности; источник без native endpoint не дебетуется.

## Состояние проверки

**Implemented:** gameplay, transport profile, persistent delayed growth, оригинальные
рецепты/текстуры/сопло и sparkle. Общие bootstrap/client/book/tags подключает ведущий.
**Built / game-tested / seen:** pending.

`BiologicalLampRuntimeChecks.growth` использует реальные herba jars, native оплаты
пятого вызова и резерва, отказ публичного неподготовленного draw, сохранение/quarantine,
все169 колонок и один реальный CropBlock.randomTick после нативной задержки,
save/load задания и удаления лампы. Для последнего шага используется только
зарегистрированный TEST-ONLY подкласс CropBlock: его inherited crop/soil/age код
реальный; ambient random ticks у входного fixture отключены, только его growth RNG
ветвь получает объявленный локальный0. Production RNG не подменяется.

`fertility` использует восьмерых настоящих коров, отказ плотности8, точные300
подходящих обновлений, оплату2 за одну пару, сохранение charges/сброс transient count,
и сохранение лампы после потери опоры. Рецепты сверяются с реальным RecipeManager.
Клиентская картинка/частицы, multiplayer и NeoForge здесь не подтверждены.

**Отдельный pending regression:** синхронная замена лампы из source notification.
Первый draft ошибочно наследовал финальный production `TileJar` и не компилировался.
Этот невалидный test-only subclass удалён, production class/policy не ослаблены.
Реальное уведомление jar идёт через нативный comparator; специальный reachable
comparator callback fixture в этом срезе не реализован. Этот сценарий нельзя считать
пройденным на основании остальных resource-conservation проверок.
