# Восьмой материал — плоть, 19.10.2026

**Интегрировано; Fabric build выполняется, не проверено в игре.**
Источник: [original contract](2026-10-19-golem-flesh.md).

FINAL автора прочитан root: настоящий блок плоти (original blockTaint:2), рецепт9
rotten flesh→block, Silk/normal loot и crucibleGolemFlesh. Root подключил восьмой
материал4/placer/skin/aliases/lang/block registry. Это **не coreGuard4** и не
косметическая Wood-модель: существующая точная строка enum15HP/armor6/speed.35/
carry4/regenDelay40/firefalse сохраняется. Healing loop даёт41tick recurrence,
не fuel/brain/advanced-only способность. Cookie остаётся общей механикой.

Block meta2 не распространяет заражение, не лечит, не спавнит mobs и не наносит
taint damage. Нормальный break даёт9rotted flesh, Silk — блок, reverse craft нет.
External taint-fibres adjacency остаётся отложенной частью общей taint-системы.
Original itemWarp1 declaration также gap общего item-Warp API/effect engine;
не заменена выдуманным per-kill/hold/craft начислением. ResearchWarp3 уже берётся
из существующих data и completion, без нового hook.

Все native material-specific data/recall/water/aspect carry paths используют
отдельный material ID и обычные capacities. Конфигурация/реальный stock, Use options
и core IDs не перемешиваются. Unknown/protected payloads не очищаются.
Совместная Fabric-сборка с Use8 `golem-use-build1` выполняется после всех FINAL.
Gameplay/protection/visual/
save acceptance остаётся pending; Bore-only distribution и защищённые worlds
не меняются, rejected save не используется как вход.
