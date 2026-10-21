## Текущая работа — 21.10.2026, закрытие 17 пробелов каталога (audit a10 §2.4)

Ветка `port/fixmissingentries`, worktree `/home/Acak/tcw/fixmissingentries`. Оракул —
`/home/Acak/work/thaumcraft-4.2.3.5-oracle/`; исходный аудит —
[2026-10-21-audit-a10-data](analysis/2026-10-21-audit-a10-data.md), §2.4 (17 записей).
Закрыты все семнадцать; девять остаются документированным пробелом, потому что в 1.21.1 нет
соответствующего металла.

**A. Самородки и родные кластеры (9 записей).**
`ItemNugget` 0 (железный самородок) — перенесён адаптацией идентификатора на
`minecraft:iron_nugget` (ванильный 1.21.1): теги `c:nuggets/iron`,
`thaumcraft:original_ore/nugget_iron`, `thaumcraft:iron_nuggets`. Девятикратное сжатие железа
даёт сама ванильная пара `minecraft:iron_nugget` / `minecraft:iron_ingot_from_nuggets`; она
закреплена тестом, потому что порт своего рецепта на этот предмет не заводит.
`ItemNugget` 1 (медный самородок) — ПОРТ: `thaumcraft:copper_nugget` (ItemNugget:46-49,
ConfigItems:277). Медь в 1.21.1 ванильная, значит оригинальная ветка `Config.foundCopperIngot`
включена; рецепты сжатия взяты по форме оригинального железного (ConfigRecipes:399/402), потому
что ванильная медь своего самородка не имеет.
`ItemNugget` 17 (родной медный кластер) — ПОРТ: `thaumcraft:cluster_copper` с рецептами
`crucible/pure_copper` (ConfigRecipes:134) и `crucible/trans_copper` (ConfigRecipes:148) и
привязкой аспектов оригинала (ConfigAspects:149 и :153).
`ItemNugget` 2/3/4 (самородки олова, серебра, свинца) и 18/19/20 (их родные кластеры) —
ДОКУМЕНТИРОВАННЫЙ ПРОБЕЛ: в 1.21.1 нет олова, серебра и свинца как металла, а оригинал
регистрировал эти рецепты только при `Config.foundTinIngot` / `foundSilverIngot` /
`foundLeadIngot`.

**B. Медный наконечник палочки.** ПОРТ: `thaumcraft:wand_cap_copper`; модификатор цены 1.1,
особые аспекты order и entropy со специальным модификатором 1.0, цена крафта 2 (ConfigItems:331),
arcane-рецепт `arcane/wand_cap_copper` (ConfigRecipes:231). Модель выбора палочки пересобрана
генератором (990 переопределений, 18 стержней × 5 наконечников × 11 фокусов).

**C. Двойные плиты (2 записи).** ПОРТ: четыре блока `cosmetic_double_slab_wood_greatwood`,
`cosmetic_double_slab_wood_silverwood`, `cosmetic_double_slab_stone_arcane`,
`cosmetic_double_slab_stone_eldritch` (ConfigBlocks:300-301). Полный блок, как в оригинале;
двойная плита роняет две одиночные деревянные и одну одиночную каменную
(BlockCosmeticWoodSlab:56-58, BlockCosmeticStoneSlab:56-58). В оригинале двойные плиты не были
в творческой вкладке и не имели рецепта — так и здесь.

**D. Три варианта `blockStoneDevice`.** ПОРТ: 8 (составной фокус перезарядки,
`thaumcraft:wand_pedestal_focus`, ConfigRecipes:342), 10 (улучшенный стабилизатор узла,
`thaumcraft:advanced_node_stabilizer`, ConfigRecipes:343 — он же снимает отложенный рецепт
`NodeStabilizerAdv`; блок делит тайл обычного стабилизатора и отдаёт запор 2 вместо 1,
TileNodeStabilizer:24) и 14 (скребок потока, `thaumcraft:flux_scrubber`, ConfigRecipes:194).

**E. Два варианта `blockWoodenDevice`.** ПОРТ: 2 (арканная нажимная плита,
`thaumcraft:arcane_pressure_plate`, ConfigRecipes:197) и 8 (багровый культовый вымпел): sixteen
arcane-рецептов ConfigRecipes:184-189, по предмету на цвет (`thaumcraft:crimson_banner_*`),
потому что результат рецепта в 1.21.1 не может нести NBT-байт цвета.

**F. Четыре обычных рецепта «тройного мяса».** ПОРТ (ConfigRecipes:410-413).

Отложенный список каталога рецептов сократился с 16 до 9: остались восемь рецептов чужих руд
(олово, серебро, свинец) и `MirrorEssentia`, чей результат в 1.21.1 не может нести состояние
блока.

Попутно исправлено: восемь висячих ссылок на теги `c:*` — в ванильном 1.21.1 тегов `c:` нет
вообще, поэтому `pure_iron`, `pure_gold`, `trans_iron`, `iron_key` и подкраска свечей не
срабатывали. Порту добавлены `c:nuggets/{iron,gold,copper}`, `c:ores/{iron,gold,copper}`,
`c:raw_materials/{iron,gold,copper}`, `c:ingots/copper`, `c:dusts/glowstone`,
`c:dyes/{blue,black}`; отсутствующие имена предметов `triple_meat_treat` и четырёх съедобных
самородков дописаны в оба языка.

Клиент не трогался. Известные клиентские следствия: у иконки исследования `CAP_copper` нет
случая для медного наконечника (`ResearchIconDisplay`), а предмет улучшенного стабилизатора
рисуется через `minecraft:builtin/entity` без рендерера — так же, как обычный стабилизатор.
Аспект вымпелa при разбитии не переносится в предмет (в оригинале переносился в NBT).

Проверка: `:common:compileJava` чисто, `:common:test` — 1568 тестов, 0 падений;
`python3 -B scripts/porting/convert_assets.py --verify` — `missing_references: 0`.
Клиентский осмотр, multiplayer и полная survival-прогрессия этим чекпойнтом не подтверждены.

