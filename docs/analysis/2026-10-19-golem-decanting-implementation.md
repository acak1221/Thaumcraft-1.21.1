# Переливание5 — реализация19.10.2026

**Интегрировано и собрано на Fabric; не проверено в игре.**
Источник: [original contract](2026-10-19-golem-decanting.md),
[agreed API](2026-10-19-golem-decanting-api.md).

Реальный unit — отмеченные vanilla water sources, отдельный переносимый объём
по исходной ёмкости материала и пополнение прикреплённого тигля. Нет выдачи/расхода
реальных вёдер, unmarked pond scan, universal fluid providers, lava/Entropy pumping.
Один ghost water-container sample задаёт фильтр, не запас воды или quantity goal.

Root подключил core/recipe/book/data/menu/client и строгий water payload к placer.
Runtime Entity/LiquidGoals FINAL прочитаны root; source-removal permission commit
реализовал по [native consultation](2026-10-19-golem-water-commit.md): стандартный
LevelChunk write со всем heightmap/light bookkeeping, raw cargo credit до
native dirty/client/neighbors/shapes/POI и AFTER. Source/room/marker/current owner
повторно проверяются после настоящих veto callbacks. Только exact vanilla WATER
source→AIR, без generic block/fatal-error/addon-mixin atomicity promises.
Типизированный crucible handoff меняет оба raw owners до sync, сохраняет частичный
остаток. Ошибка уведомления после commit не возвращает уже принятую воду.
Recall сохраняет stock отдельно от five-field конфигурации; dismantle с водой
будет отказом, а не потерей/выдачей bucket. Смертельная потеря воды — original,
не объявляется исправленной сохранностью обычного recall.

Client показывает carry-pose, отдельный display-only native empty/water bucket и
объём мБ. Это объявленная графическая адаптация: original OBJ bucket и поверхность
воды ещё не перенесены/не осмотрены. Display не является equipment/cargo/item drop.
Права основаны на unspawned non-OP профиле, current source/home/marker/room и
реальных Fabric/Architectury callbacks; unknown addon contracts не заявлены.

Actual Fabric build `golem-decanting-build1` **PASS36s** после всех FINAL:
main/remap/production isolation, private JAR11094943bytes/SHA256
`8ba3f263bb313b01cc9c72f5e2621acc439effcefef18202dc38a9215812383c`.
Root установил placement-callback guard и отложенный native removal после raw
water handoff: ни unpaid recall, ни забытый terminal callback не разрешаются.
Игровые/визуальные/claim/save проверки остаются отложенными; Bore-only экспорт
не меняется и новые unverified JAR его не заменяют.
