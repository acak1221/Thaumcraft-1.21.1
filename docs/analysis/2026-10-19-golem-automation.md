# Дополнение к обычной големантии — 19.10.2026

## Направленный выброс

Интегрирована исходная ветка Fill/Empty: выброс всего настоящего груза с половины
высоты голема, pickupDelay10 и ненормализованной trajectory distance/3. Используется
существующая очередь level-post, без нового журнала/версии/наблюдателя. Queue row
и raw cargo clear передаются без callback между ними, затем идут display/action6.
Gather и Sorting не получают эту задачу. Полный/неподдержанный контейнер не
превращается в разрешение на выброс. Unknown/non-inventory BEs остаются отказом;
маршруты в обычные loaded native non-BE blocks/air — явный ограниченный набор.

Источник: [throw contract](2026-10-19-golem-world-throws.md),
[runtime final](2026-10-19-golem-throws-runtime-notes.md). Shared queue root-метод
`DeferredItemDrops.enqueueGolemThrow(level,golem,target)` уже интегрирован.
Ещё не компилировалось/не запускалось; войдёт в следующий batch со Sorting/brain.

## Сортировка

Sorting10 направляет настоящие предметы из домашнего контейнера в отмеченный,
где уже есть совпадающий extractable seed и место **на той же стороне**. Это не
Fill с wanted quota: у Sorting нет ghost inventory, quantity controls или Fill
toggle. Root меню создаёт0 ghost slots/36 player slots и фиксирует opening core,
клиент прячет отсутствующие controls. Data сохраняет схему пяти полей, но
отказывается принимать nonbaseline ghosts/counts/mode для Sorting.

Добавлены точный infusion recipe (brain center, Fill/comparator/Empty/paper,
instability3, четыре аспекта по16), предмет/model/книжные aliases/переводы.
Seeded transport runtime FINAL прочитан: живой seed+room recheck перед каждым
оплаченным переносом, исходный source slot order, nearest destination, timeout и
условный возврат. Sorting не выбрасывает груз. Получение/food поведение zombie
brain — отдельный prerequisite, не выдаётся через fixtures.
Brain prerequisite FINAL прочитан и интегрирован: native food4/.2/Hunger80%,
исходные sticky/temp warp charges через существующее сохранение знаний; wolf-food
tag. Один mixin наблюдает настоящий recentlyHit внутри native death-loot guards
и выполняет original nextInt(10)−Looting<1 roll для обычной vanilla Zombie family.
Нет второго loot-пула или общего death callback; полная Warp-система не заявлена.

Источник: [Sorting source contract](2026-10-19-golem-sorting.md). Объединённая
сборка `golem-automation-build1` **PASS42s**, native main/remap/production isolation.
Private JAR11007976bytes/SHA256
`b3dc066f612a47615cef53a4d02c482d612e8487e47feec8742442d94271fb27`.
Нового game/smoke запуска нет;
gameplay/visual/recipe/menu/key/save acceptance
отложена пользователем, не объявлена PASS. Bore-only export остаётся неизменным.
