# Использование8 — первая нативная группа19.10.2026

**Интегрировано; actual Fabric build выполняется, не проверено в игре.**
Источник: [original contract](2026-10-19-golem-use.md),
[options](2026-10-19-golem-use-data.md), [actor/runtime agreement](2026-10-19-golem-use-api.md).

Цель первой группы — DEFAULT block/right/notSneak: пустая настоящая рука без
домашнего inventory включает vanilla рычаг/кнопку; реальные WHEAT_SEEDS из
supported home сажаются на markedFarmland/UP. Пустой существующий сундук не
выдаёт право bare-hand. Образцы не расходуются и не являются запасом семян.
У original первый переключатель Block/Empty space, **не entity target**.

Root подключил core8/data/recipe/book/model/lang/placer guards. Dedicated UseOptions
становится шестым полем, old constructors/default payload поддержаны; Entity/save/
recall обязан сохранять его явно. Nondefault обычные режимы пока запрещены
сервером, не изображаются рабочими переключателями и не названы upgrade-gated.

Runtime и actor FINAL прочитаны root: настоящие source/route/action/return/remainder
lifecycle. Actor integration использует действительную native GameMode/Fabric
цепочку, один borrow с настоящей ссылкой на stock и settled actual post-call
hand/extras, не копию/phantom seeds. Неизвестные ранее принадлежавшие actor вещи
не стираются: непустой actor отказывает. Мир не откатывается по PASS/FAIL/throw;
повторяется только публикация уже принадлежащих остатков через existing queue.

Recipe — Empty core1 center, comparator/flint-and-steel DAMAGE0/shears DAMAGE0/
lever, Instrumentum20/Machina20/Humanus20, instability3, genuine primary COREUSE.
Рецептные инструменты расходуются инфузией, не становятся бесплатным working gear.
Other seeds, ignition/TNT/fire, tools/shears, GUI blocks, Empty-space/Left/Sneaking,
colors/upgrades/addon interactions пока pending. Нет animal/player/PvP interaction.
Actual Fabric `golem-use-build1` выполняется после runtime/actor/Flesh FINAL,
main/remap/isolation и15dataJUnit; gameplay/claim/visual/save
проверки отложены. Ни защищённые существа, ни rejected save, ни Bore-only export
не меняются и не используются как повторный тестовый вход.
