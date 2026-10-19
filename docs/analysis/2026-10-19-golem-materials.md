# Обычные материалы големов — 19.10.2026

После скомпилированного Straw/Gather интегрированы ещё шесть обычных материалов.
Авторитет: private oracle `common/entities/golems/EnumGolemType.java:10–17` и
`common/config/ConfigRecipes.java:165,167,169–170`. Сборка/игра новой группы пока
не выполнялись; это не закрытие всех големов.

| ID / материал | Health | Armor | Speed | Carry | Regen counter | Fire immune |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| 0 Straw | 10 | 0 | .38 | 1 | 75 | нет |
| 1 Wood | 20 | 6 | .35 | 4 | 75 | нет |
| 2 Tallow | 20 | 9 | .33 | 8 | 75 | нет |
| 3 Clay | 25 | 9 | .33 | 8 | 100 | да |
| 5 Stone | 30 | 12 | .32 | 16 | 100 | да |
| 6 Iron | 35 | 15 | .31 | 32 | 125 | да |
| 7 Thaumium | 40 | 15 | .32 | 32 | 100 | да |

Regen counter означает исходную steady recurrence counter+1, не gratuitous heal
при загрузке. Состояние повреждённого сохранённого голема не восстанавливается
до полного. Material задаётся реальным ID placer-предмета; recall возвращает
именно этот материал. GolemData остаётся только core/6 ghost samples, без cargo.

Root добавил registry/четыре crucible recipes/книгу/item models/оригинальные skins/
GUI material frames/predicate для всех пяти placers. Catalyst: Greatwood log,
clay block, обычные stone bricks, iron block соответственно. Каждый рецепт даёт
одного голема за Humanus4/Motus4/Spiritus4, с исходным собственным research gate.
Placers переданы; runtime автор завершает параметризацию характеристик и сохранений.

Enum сохраняет исходные восемь ordinal-значений. Flesh4 пока не регистрируется:
его исходный заражённый блок-ингредиент ещё не перенесён. Все прочие cores/upgrades/advanced/decorations и
marker routing остаются pending. Нет новых smoke/аудиторских инструментов.

Prerequisite-блоки Thaumium/Tallow уже переданы и интегрированы: исходные rock
свойства/прочность/текстуры/9↔1 recipes/pickaxe и исходный beacon-base quirk.
Игровая проверка блоков не выполнялась. Это позволило включить2/7 в тот же
content batch без ещё одной промежуточной сборки: root добавил native placers/
8-aspect recipes/skins/книгу/tooltip-имена, runtime автор получил обновлённый scope.
Runtime FINAL получен и прочитан: материал синхронизируется, параметры выбираются
из исходного enum, загрузка не лечит повреждённого голема, recall возвращает его
реальный материал, custom-name сохраняет обычный приоритет. Начата actual Fabric
compile `golem-materials-build1` **PASS35s**, включая remap/production isolation.
Private JAR10961197bytes/SHA256
`b7a4d3a1374e599f03e6f97f512802b9512ae19b73cb116edd744e4269d32916`.
Пакет содержит7golem recipes/itemmodels и реальные Thaumium/Tallow block recipes.
Игрового запуска/визуальной приёмки не было; Bore-only экспорт не заменён.
