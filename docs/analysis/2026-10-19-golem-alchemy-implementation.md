# Алхимия6 — реализация19.10.2026

**Интегрировано и собрано на Fabric; не проверено в игре.** Источник:
[original contract](2026-10-19-golem-alchemy.md), [agreed API](2026-10-19-golem-alchemy-api.md).

Полезная первая группа: attached actual alembic/ограниченный contiguous stack
над native alchemy furnace → отдельный aspect+amount stock → marked normal jars.
Ни фильтры, ни display-банка, ни crucible AspectList не являются источником.
Transfer/cargo FINAL прочитан root: два closed entry в existing executor, exact
raw authorization, BE и Entity меняются до notifications. Partial committed
units не становятся unpaid0 при ошибке post-commit notification.
Label/contents/room/currentness/reach должны проверяться на настоящих endpoints;
partial acceptance сохраняет непринятые единицы у голема.

Все три автора завершили Entity/Goal, существующий raw essentia executor+stock
и typed infusion component predicates; root прочитал их final sources. Добавлены core6/data/
registry/recipe/book/icons/placer/client и query permission support. Recipe —
Decanting center, empty normal jar +3 real water potions, instability2 и
Praecantatio15/Aqua15/Motus15, COREALCHEMY. Predicates FINAL прочитаны root:
water contents/empty meaningful jar проверяются в initial и later paired component
matching, сохраняются в session codec и recipe stream. Book preview получает
настоящий water bottle. Нативный recipe в игре ещё не проверен.

Client display jar использует исходный material scale и floor(64*amount/capacity),
arms ±(1−scale)*25°. Это только изображение реального scalar stock; у полного
соломенного голема1unit выглядит как полная банка, но не создаёт64units или
настоящий предмет. ItemCarried/equipment/recall stock не подменяются preview.

Normal recall/placement хранит separate AspectCargo, не GolemData/ghost values;
неизвестные payloads остаются raw/refused, положительный stock запрещает dismantle.
Известная death loss пока original; нет бесплатных phials/filled jars/crystals.
Connected jars/void/generic transports/reservoirs/upgrades и universal addon claim
parity отдельно pending. Actual Fabric build `golem-alchemy-build1` **PASS35s**,
main/remap/isolation и11 existingdataJUnit (6 aspects/5 declarations). Private JAR
11123385bytes/SHA256`17bfb3fe8d69f6c0156ecff6c4e13c263fd459e4067c3dca890f60e8b6e11c2e`.
Gameplay,
claim/visual/save acceptance отложена, Bore-only экспорт не меняется.
