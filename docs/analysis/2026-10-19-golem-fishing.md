# Рыбалка11 — реализация19.10.2026

**Интегрирована и собрана на Fabric; не проверена в игре.**
Authority: [original acquisition/task](2026-10-19-golem-next-core.md),
[loot helper](2026-10-19-golem-fishing-loot.md),
[bobber/display](2026-10-19-golem-bobber.md), [agreed API](2026-10-19-golem-fishing-api.md).

После Guard root подключил core11/data/recipe/book/icons/model/lang и transient
bobber type. Серверная задача FINAL прочитана root; physics/client FINAL:
moving bobber, source launch/buoyancy, реальная леска, rod-display и independent
arm5, model priority/reset. APIs совпали с registration и agreed contract;
никакой визуальной проверки этот readback не заменяет.
Обычная рыбалка выбирает loaded воду возле дома, держит один cast и изредка
создаёт один исходный fish/junk/treasure result. Это не убийство passive fish,
не ванильный FishingHook, не guaranteed bite timer и не ghost supplies.
Нет runtime rod payment/износа/XP/статистики/автоподбора; нужен игрок/Gather.

Root queue extension сохраняет один уже выпавший результат/траекторию/delay20
через существующую Pending, без повторного RNG, нового журнала/версии/аудитора.
Реальная raw передача pending из goal должна завершаться до feedback callbacks.
Права проверяются unspawned non-OP служебным профилем без owner borrowing:
native world/spawn checks, не break callbacks и не effectful use-query.
Addon-specific fishing claims и полный enchantment-registry parity не заявлены.

Рецепт из ConfigRecipes373: Harvest center, rod DAMAGE0/cod/pufferfish/salmon,
instability3/Aqua16/Messis16/Bestia16, COREFISHING. No controls/GUI для11;
five-field placer configuration остаётся прежней. Rod-display и cast state не
являются stock и не попадают в real cargo/recall/drop. Root исправил placer
tooltip: уже установленное Fill/Empty/Harvest/Guard/Sorting/Fishing ядро больше
не подписывается «без ядра» после отзыва.

Предыдущий Guard build включает только loot helper, не Fishing runtime/client.
Actual Fabric compilation `golem-fishing-build1` **PASS41s** после всех FINAL,
main/remap/production isolation. Private JAR11073129bytes/SHA256
`013564631e37ca78b5aa689765c3a5f6b6654ab8d39b717ce932df487c1b4eee`.
Root добавил Entity overload существующего rejected-spawn cleanup и исправил
count RNG на исходный worldRandom. Gameplay/visual/loot-rate/
protection/save acceptance отложена. Bore-only distribution не меняется.
