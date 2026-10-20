# Алхимические данные: Alt-рецепты тигля и Зеркальное стекло — контракт и реализация, 20.10.2026

Источник: оригинал TC4 4.2.3.5 (локальная декомпиляция CFR вне репозитория): `ConfigRecipes:101-111` (десять `Alt*` рецептов `ThaumcraftApi.addCrucibleRecipe`), `ConfigRecipes:196` (`MirrorGlass`, `addShapelessArcaneCraftingRecipe`), `ConfigResearch` (`ALCHEMICALDUPLICATION`, `ALCHEMICALMANUFACTURE`, `ENTROPICPROCESSING`, `BASICARTIFACE`), `ItemResource` (метаданные 3 — Quicksilver, 10 — Mirrored Glass).

## Что было до

Все 201 ключ исследований уже были в `research.json`, а страницы этих четырёх записей ссылались на рецепты `AltGunpowder … AltBonemeal` и `MirrorGlass`, за которыми ничего не стояло; предмет `mirrored_glass` был зарегистрирован, но нечем получить. В нумерации тигля `source_order` зияла дыра 12–21 между Tallow (11) и Pure Iron (22).

## Что делает оригинал

Тигель берёт рецепт, у которого выполнены исследование бросившего игрока, катализатор и набор аспектов; из всех подходящих побеждает рецепт с наибольшим числом **различных** аспектов, а при равенстве — зарегистрированный раньше (`CrucibleCrafting.find` в порте уже так работает, порядок задаёт `source_order`). За одну варку платятся аспекты рецепта и 50 воды, катализатор расходуется по одному.

| Рецепт | Исследование | Катализатор | Результат | Аспекты |
|---|---|---|---|---|
| AltGunpowder (12) | ALCHEMICALDUPLICATION | порох | 2 пороха | ignis 4, perditio 4 |
| AltSlime (13) | ALCHEMICALDUPLICATION | слизь | 2 слизи | aqua 2, victus 2 |
| AltClay (14) | ALCHEMICALDUPLICATION | глиняный шарик | 2 шарика | aqua 1, terra 2 |
| AltGlowstone (15) | ALCHEMICALDUPLICATION | светопыль (`dustGlowstone`) | 2 светопыли | lux 3, sensus 1 |
| AltInk (16) | ALCHEMICALDUPLICATION | чернильный мешок | 2 мешка | aqua 2, sensus 2 |
| AltWeb (17) | ALCHEMICALMANUFACTURE | нить | паутина | vinculum 2, pannus 2 |
| AltMossyCobble (18) | ALCHEMICALMANUFACTURE | булыжник | замшелый булыжник | herba 2, praecantatio 1 |
| AltIce (19) | ALCHEMICALMANUFACTURE | **блок снега** | лёд | ordo 1, gelum 1 |
| AltCrackedBrick (20) | ENTROPICPROCESSING | каменные кирпичи | потрескавшиеся кирпичи | perditio 2 |
| AltBonemeal (21) | ENTROPICPROCESSING | кость | 4 костной муки | perditio 1 |

`MirrorGlass`: бесформенный арканный рецепт (`BASICARTIFACE`, ignis 10 + terra 10): Quicksilver + стеклянная панель → одно Зеркальное стекло.

## Решения порта

- Чистые данные, новой Java нет: десять файлов `data/thaumcraft/recipe/crucible/alt_*.json` с исходными `source_order` 12–21 (формат тот же, что у `tallow.json`), `arcane/mirrored_glass.json` (`arcane_shapeless`), одиннадцать алиасов `recipe_key → id` в `ThaumcraftRecipeDisplay`, чтобы страницы книги нашли рецепты.
- Идентификаторы 1.7 → 1.21.1: `dye 0` → `ink_sac`, `dye 15` → `bone_meal`, `stonebrick 2` → `cracked_stone_bricks`, `Blocks.snow` (полный блок, SRG `field_150433_aE`; слой снега — `field_150431_aC`) → `snow_block`, `dustGlowstone` → светопыль и тег `c:dusts/glowstone` (как в Nitor).
- Катализатор Ice — блок снега, а не слой: проверено по использованию SRG-имён в оракуле (слой встречается только в логике банок и узла).

## Отклонения и ограничения

1. Стеклянная панель — только простая `minecraft:glass_pane` (в оригинале `Blocks.glass_pane`); окрашенные панели не подходят, как и стекло-блок.
2. Книга показывает схемы рецептов через существующий класс `ThaumcraftRecipeDisplay` (клиент): **не проверено в клиенте.**
3. Объектные аспекты катализаторов (которые тигель растворяет без исследования) не менялись.

## Проверка

- **Implemented / built / game-tested:** JUnit `AlchemyLeftoversAssetsTest` (5: все данные рецептов как у оригинала; дубликаты только у `ALCHEMICALDUPLICATION`; все `source_order` тигля уникальны, дыра 12–21 заполнена; страницы исследований перечисляют рецепты и алиасы книги указывают на существующие ресурсы; Зеркальное стекло);
  GameTest `alchemyLeftoversBrewFromTheirCatalystsAtTheOriginalCost` на настоящем сервере: десять рецептов из настоящего менеджера рецептов (исследование, порядок, результат, аспекты, свой катализатор принимается, чужой — нет, без одного аспекта — нет), выбор по знаниям **настоящего бросившего игрока** (без исследования — ничего, с исследованием — именно этот рецепт, на единицу короче — ничего), настоящая нагретая плита-тигель (без исследования только растворяется без трат; с исследованием платятся катализатор, аспекты и 50 воды, появляются 2 пороха / лёд / 4 костной муки) и Зеркальное стекло (любые две ячейки, стеклоблок, лишний предмет, две панели без ртути — нет).
- **Локально на финальных источниках** (поверх `b65cd6c`): `:common:test` **937 JUnit** (+5), сфокусированный GameTest — PASS, полный `:fabric:runGameTest` **272 из 273** (+1 мой сценарий): красный только `pechoriginaltradeusesnativegameplay` («cancelled native ADD retains every actual returned stack durably») — он красный и на чистом `b65cd6c` без моих правок в этом же контейнере (проверено через `git stash`), при том что GitHub-прогон 68 на этом коммите был зелёным; это не от группы и не принято мной как PASS этого теста. `:fabric:remapJar` PASS; `convert_assets.py --verify` — 0 пропущенных ссылок (2746 текстур / 1142 моделей); 84 Python-теста; `git diff --check` чист.
- **Мутации:** временно сломанные данные (аспекты пороха, катализатор льда) ловятся и GameTest, и JUnit.
- **Seen:** нет. **Не проверено:** книга в клиенте, мультиплеер, NeoForge.
