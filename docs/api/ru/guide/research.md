# Исследования

Исследования аддона — это исследования игры: Таумономикон рисует их, стол исследований пишет по ним заметки,
вторичные записи покупаются за очки исследований, сканирование триггера открывает скрытые, завершение платит их варп, а
знания игрока сохраняют и синхронизируют их. Вызовы — из 1.7.10.

## Категория

<!-- snippet:category -->
```java
// A research category of its own, after the six original tabs: the key, the tab icon and the background.
ResearchCategories.registerCategory("EXAMPLE",
        ResourceLocation.withDefaultNamespace("textures/item/amethyst_shard.png"),
        ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchback.png"));
```
<!-- /snippet -->

Значок рисуется на весь значок вкладки, каким бы ни был его размер; фон читается с разметкой исходного листа 512×512
(`thaumcraft:textures/gui/gui_researchback.png` — исходный). Вкладки аддонов идут после шести исходных в порядке
регистрации. Название вкладки — ключ перевода `tc.research_category.<CATEGORY>`.

## Записи

<!-- snippet:research -->
```java
new ResearchItem("EXAMPLE_SHARDS", "EXAMPLE", new AspectList().add(Aspect.CRYSTAL, 3).add(Aspect.MAGIC, 3),
        0, 0, 1, new ItemStack(VIS_SHARD))
        .setParents("CRUCIBLE") // a parent from the original tree
        .setPages(new ResearchPage("tc4_example.research_page.EXAMPLE_SHARDS.1"),
                new ResearchPage(arcane),
                new ResearchPage(crucible),
                // Not in 1.7.10: a data-driven recipe of the add-on, shown by its id.
                ResearchPage.recipe(id("vis_shard_from_amethyst")),
                new ResearchPage(new AspectList().add(Aspect.CRYSTAL, 3).add(Aspect.MAGIC, 3).add(TEMPUS, 2)))
        .registerResearchItem();

new ResearchItem("EXAMPLE_INFUSION", "EXAMPLE", new AspectList().add(Aspect.SENSES, 4).add(Aspect.MAGIC, 3),
        2, 1, 2, new ItemStack(Items.ECHO_SHARD))
        .setParents("EXAMPLE_SHARDS", "INFUSION")
        .setConcealed() // shown only once its parents are known
        .setPages(new ResearchPage("tc4_example.research_page.EXAMPLE_INFUSION.1"), new ResearchPage(infusion))
        .registerResearchItem();

new ResearchItem("EXAMPLE_SECRET", "EXAMPLE", new AspectList().add(Aspect.METAL, 2).add(Aspect.FIRE, 2),
        -2, 1, 1, ResourceLocation.withDefaultNamespace("textures/item/raw_copper.png"))
        .setParents("EXAMPLE_SHARDS")
        .setHidden() // invisible until a scan of a trigger gives its clue
        .setItemTriggers(new ItemStack(Items.ECHO_SHARD))
        .setPages(new ResearchPage("tc4_example.research_page.EXAMPLE_SECRET.1"),
                new ResearchPage(new ItemStack(Items.RAW_COPPER)))
        .registerResearchItem();
ThaumcraftApi.addWarpToResearch("EXAMPLE_SECRET", 1);
```
<!-- /snippet -->

`new ResearchItem(key, category, aspects, column, row, complexity, icon)`:

- `key`: уникальный, по обычаю в верхнем регистре. Название и подзаголовок — из `tc.research_name.<KEY>` и
  `tc.research_text.<KEY>`.
- `aspects`: что просит стол исследований (и сколько очков стоит вторичная запись).
- `column`, `row`: клетка на вкладке; две записи одной вкладки не могут занимать одну клетку.
- `complexity`: от 1 до 3, сложность заметки исследования.
- `icon`: `ItemStack` (рисуется как предмет) или `ResourceLocation` текстуры (рисуется на всю клетку).

Флаги — как в 1.7.10:

| Вызов | Действие |
|---|---|
| `setParents(keys...)` | должны быть завершены раньше; рисуются линиями. Родителем может быть исходный ключ (`"CRUCIBLE"`) |
| `setParentsHidden(keys...)` | должны быть завершены раньше; линии не рисуются |
| `setSiblings(keys...)` | завершаются вместе с этой записью |
| `setSecondary()` | покупается в книге за очки исследований, без заметки |
| `setConcealed()` | не видна, пока не завершены родители |
| `setHidden()` | не видна, пока у игрока нет подсказки (от сканирования триггера) или пока он её не завершит |
| `setLost()` | как скрытая, и её линии никогда не рисуются |
| `setItemTriggers`, `setEntityTriggers`, `setAspectTriggers` | сканирование одного из них даёт подсказку скрытой или потерянной записи |
| `setRound()`, `setSpecial()` | круглая рамка; особая (выделенная) рамка |
| `setAutoUnlock()` | есть у каждого игрока |
| `setVirtual()` | никогда не рисуется и не исследуется: родитель для группировки |
| `setStub()` | оставлен для кода 1.7.10; в этом порте ни на что не влияет |

`registerResearchItem()` добавляет запись; при отсутствии категории, повторе ключа или занятой клетке он отказывает (и
всё равно возвращает запись). `ThaumcraftApi.addWarpToResearch(key, amount)` даёт варп при завершении записи.

Триггеры-предметы сравнивают предмет и его данные, не учитывая износ. Сокращения рудного словаря из оригинала нет:
перечислите все предметы семейства.

## Страницы

| Страница | Конструктор |
|---|---|
| текст | `new ResearchPage("lang.key")`; в тексте исходная разметка `<BR>`, `<LINE>`, `<IMG>...</IMG>` |
| текст, видимый после завершения другой записи | `new ResearchPage("OTHER_KEY", "lang.key")` |
| магический рецепт, тигель, наполнение или зачарование наполнением из API | `new ResearchPage(recipe)`; массив сменяется раз в секунду |
| объект ванильного рецепта | `new ResearchPage(Recipe<?>)` (или массив, или список) |
| рецепт из данных | `ResearchPage.recipe(ResourceLocation... ids)`: любой рецепт менеджера рецептов, включая родные типы Thaumcraft |
| переплавка | `new ResearchPage(ItemStack input)` |
| аспекты | `new ResearchPage(AspectList)` |
| картинка | `new ResearchPage(ResourceLocation image, "caption.key")` |

Страницы `COMPOUND_CRAFTING` (схемы сборки жезлов из оригинала) для записей аддонов не рисуются.

Страница с рецептом также позволяет открыть книгу на этой странице из просмотрщика рецептов или по результату рецепта,
как в 1.7.10 (`ThaumcraftApi.getCraftingRecipeKey`).

## Изменение исходных записей

Публичный вид исходной записи можно менять как в 1.7.10; изменение накладывается поверх исходной записи:

```java
ResearchCategories.getResearch("NITOR").setSiblings("MY_KEY");
```

## Знания игрока

То, что аддоны 1.7.10 брали из `ResearchManager` и `Thaumcraft.proxy.getPlayerKnowledge()`, есть в
`ThaumcraftApiHelper` и принимает игрока вместо имени:

| Нужно | Вызов |
|---|---|
| завершил ли игрок запись? | `isResearchComplete(player, key)` (на обеих сторонах для своего игрока клиента) |
| завершить её (с родителями, соседями, варпом; с синхронизацией) | `completeResearch(player, key)` (сервер) |
| всё, что игрок завершил | `getCompletedResearch(player)` |
| очки исследований | `addAspectPool(player, aspect, amount)`, `getAspectPool(player, aspect)` |
| открытые аспекты | `hasDiscoveredAspect(player, aspect)`, `getDiscoveredAspects(player)` |
| варп | `getWarpPermanent/Sticky/Temporary/Total(player)`, `addWarpToPlayer(player, amount, temporary)`, `addStickyWarpToPlayer(player, amount)` |

Свиток из примера использует их:

<!-- snippet:scroll -->
```java
public class InsightScroll extends Item {
    public InsightScroll(Properties properties) { super(properties.stacksTo(16)); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        // The knowledge helpers read and change the server's copy and synchronise it to the player.
        if (!ThaumcraftApiHelper.isResearchComplete(player, "EXAMPLE_SCROLL")) {
            player.displayClientMessage(Component.translatable("tc4_example.insight_scroll.unreadable"), true);
            return InteractionResultHolder.fail(stack);
        }
        for (Aspect primal : Aspect.getPrimalAspects()) ThaumcraftApiHelper.addAspectPool(player, primal, 2);
        ThaumcraftApiHelper.addAspectPool(player, ExampleAddon.TEMPUS, 1);
        // Completes the entry with its parents and pays its warp, as an original ResearchManager.completeResearch.
        if (!ThaumcraftApiHelper.isResearchComplete(player, "EXAMPLE_SECRET")) ThaumcraftApiHelper.completeResearch(player, "EXAMPLE_SECRET");
        player.displayClientMessage(Component.translatable("tc4_example.insight_scroll.read", ThaumcraftApiHelper.getWarpTotal(player)), false);
        stack.consume(1, player);
        return InteractionResultHolder.consume(stack);
    }
}
```
<!-- /snippet -->
