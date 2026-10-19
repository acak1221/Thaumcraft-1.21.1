# Мясник9 — реализация19.10.2026

**Интегрировано и собрано на Fabric; не проверено в игре.**
Источник: [original contract](2026-10-19-golem-butcher.md),
[bounded agreement](2026-10-19-golem-butcher-api.md).

Root подключил Data/Core9/recipe/book/icon/model/lang/tooltip и native world/
spawn-position query. Runtime автор добавляет отдельный oldest-surplus selector
и использует прежний checked melee только под установленным core9. Guard4
proactive/retaliation/strike policy не получает право атаковать животных.

Первая finite группа — взрослые native Cow/Pig/Sheep/Chicken/MushroomCow,
без named/leashed/allied/pet/player/unknown types. Требуется больше2 подходящих
животных **того же точного runtime-класса**, с повторной проверкой перед каждым
ударом. Урон — настоящий native mob attack, а не raw health/скопированная добыча.
Native harm cancellation остаётся применимой; universal addon claim adapters и
глобальная гарантия сохранности пары при независимых смертях не заявлены.

Рецепт original177: Guard4 +Corpus5/Bestia5, COREBUTCHER, существующие parents
Guard/Harvest и permanent warp1 при research completion. Нет нового warp-hook
при craft/kill. Нет GUI/filter/meat cargo или автоматического pickup; обычные
реальные drops подбираются игроком/Gather. Фактических world/harm операций,
изменений защищённых/отклонённых save или запусков игры не выполняется.

Actual Fabric build `golem-butcher-build1` **PASS38s** после runtime/Data FINAL,
main/remap/isolation и11existingdataJUnit. Private JAR11139344bytes/SHA256
`a9df7c6ce1e180d468c63b286e8041cee6425fc7a2cf3e047b05e71469450e52`.
Gameplay/protection/drop/visual/save
проверки остаются отложенными; новые JAR не заменяют Bore-only distribution.
