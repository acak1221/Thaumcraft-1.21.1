# Research

Research entries of an add-on are research of the game: the Thaumonomicon draws them, the research table writes their
notes, secondary entries are bought with research points, scanning a trigger reveals hidden ones, completion pays their
warp, and the player's knowledge saves and synchronises them. The calls are the 1.7.10 ones.

## A category

<!-- snippet:category -->
```java
// A research category of its own, after the six original tabs: the key, the tab icon and the background.
ResearchCategories.registerCategory("EXAMPLE",
        ResourceLocation.withDefaultNamespace("textures/item/amethyst_shard.png"),
        ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchback.png"));
```
<!-- /snippet -->

The icon is drawn over the whole tab icon, whatever its size; the background is read with the geometry of the original
512×512 sheet (`thaumcraft:textures/gui/gui_researchback.png` is the original). Add-on tabs follow the six original ones
in registration order. Name the tab with the lang key `tc.research_category.<CATEGORY>`.

## Entries

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

- `key`: unique, upper case by convention. Name and subtitle come from `tc.research_name.<KEY>` and
  `tc.research_text.<KEY>`.
- `aspects`: what the research table asks for (and what a secondary entry costs in research points).
- `column`, `row`: the cell on the tab; two entries of one tab cannot share a cell.
- `complexity`: 1 to 3, the difficulty of the research note.
- `icon`: an `ItemStack` (drawn as an item) or a `ResourceLocation` of a texture (drawn over the whole cell).

Then the flags, as in 1.7.10:

| Call | Effect |
|---|---|
| `setParents(keys...)` | must be complete first; drawn as lines. A parent may be an original key (`"CRUCIBLE"`) |
| `setParentsHidden(keys...)` | must be complete first; no line is drawn |
| `setSiblings(keys...)` | completed together with this entry |
| `setSecondary()` | bought in the book with research points instead of a research note |
| `setConcealed()` | invisible until its parents are complete |
| `setHidden()` | invisible until the player has its clue (from scanning a trigger) or completes it |
| `setLost()` | like hidden, and its lines are never drawn |
| `setItemTriggers`, `setEntityTriggers`, `setAspectTriggers` | scanning one of these gives the clue of a hidden or lost entry |
| `setRound()`, `setSpecial()` | the round frame; the special (highlighted) frame |
| `setAutoUnlock()` | every player has it |
| `setVirtual()` | never drawn and never researched: a grouping parent |
| `setStub()` | kept for 1.7.10 code; it has no effect in this port |

`registerResearchItem()` adds the entry; it refuses (and returns the entry anyway) a missing category, a duplicate key
or an occupied cell. `ThaumcraftApi.addWarpToResearch(key, amount)` gives warp when the entry is completed.

Item triggers compare the item and its data, ignoring the damage of durable items. The ore dictionary shortcut of the
original has no counterpart: list every item of a family.

## Pages

| Page | Constructor |
|---|---|
| text | `new ResearchPage("lang.key")`; the text keeps the original markup `<BR>`, `<LINE>`, `<IMG>...</IMG>` |
| text shown once another entry is complete | `new ResearchPage("OTHER_KEY", "lang.key")` |
| an arcane, crucible, infusion or infusion enchantment recipe of the API | `new ResearchPage(recipe)`; an array cycles once per second |
| a vanilla recipe object | `new ResearchPage(Recipe<?>)` (or an array, or a list) |
| a data-driven recipe | `ResearchPage.recipe(ResourceLocation... ids)`: any recipe of the recipe manager, the native Thaumcraft types included |
| smelting | `new ResearchPage(ItemStack input)` |
| aspects | `new ResearchPage(AspectList)` |
| image | `new ResearchPage(ResourceLocation image, "caption.key")` |

`COMPOUND_CRAFTING` pages (the wand construct diagrams of the original) are not drawn for add-on entries.

A recipe page also makes the book open on that page from a recipe viewer or from the recipe's output, as in 1.7.10
(`ThaumcraftApi.getCraftingRecipeKey`).

## Changing original entries

The public view of an original entry can be changed as in 1.7.10; the change is laid over the original entry:

```java
ResearchCategories.getResearch("NITOR").setSiblings("MY_KEY");
```

## Player knowledge

What 1.7.10 add-ons reached through `ResearchManager` and `Thaumcraft.proxy.getPlayerKnowledge()` is on
`ThaumcraftApiHelper`, taking the player instead of a name:

| Need | Call |
|---|---|
| has the player completed it? | `isResearchComplete(player, key)` (both sides for the client's own player) |
| complete it (with parents, siblings, warp; synced) | `completeResearch(player, key)` (server) |
| everything the player completed | `getCompletedResearch(player)` |
| research points | `addAspectPool(player, aspect, amount)`, `getAspectPool(player, aspect)` |
| discovered aspects | `hasDiscoveredAspect(player, aspect)`, `getDiscoveredAspects(player)` |
| learn an aspect without a scan | `discoverAspect(serverPlayer, aspect)` (server; no notice, no points) |
| has the player scanned an item? | `hasScannedItem(player, stack)` (both sides for the client's own player) |
| copy what a player scanned | `getScans(player)` and `addScans(serverPlayer, scans)` (server; `Scans` is an immutable snapshot) |
| warp | `getWarpPermanent/Sticky/Temporary/Total(player)`, `addWarpToPlayer(player, amount, temporary)`, `addStickyWarpToPlayer(player, amount)` |

The example add-on's scroll uses them:

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
        // What the scroll teaches is the data component of the stack, saved with it and synced to the client.
        AspectList taught = stack.getOrDefault(ExampleAddon.STORED_ASPECTS, StoredAspects.NONE).aspects();
        for (Aspect aspect : taught.getAspects()) ThaumcraftApiHelper.addAspectPool(player, aspect, taught.getAmount(aspect));
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

### Scans, discovered aspects and the Tome of Sharing

The thaumometer records what a player scanned as markers of the mod's own: items, entities and phenomena (aura nodes).
`getScans` returns them as an immutable `ThaumcraftApiHelper.Scans`, three sets in the order they were scanned, and
`addScans` adds such a snapshot to another player. The example add-on's Tome of Sharing copies the scans and the discovered
aspects of the player who uses it to the player it is used on:

<!-- snippet:share-tome -->
```java
public class ShareTome extends Item {
    public ShareTome(Properties properties) { super(properties.stacksTo(1)); }

    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        // The helpers that change knowledge are for the server's player; a client answers PASS and the server decides.
        if (!(player instanceof ServerPlayer teacher) || !(target instanceof ServerPlayer student)) return InteractionResult.PASS;
        // A book is read by someone who has seen one: the thaumometer's record of scanned items.
        if (!ThaumcraftApiHelper.hasScannedItem(teacher, new ItemStack(Items.BOOK))) {
            teacher.displayClientMessage(Component.translatable("tc4_example.share_tome.unread"), true);
            return InteractionResult.FAIL;
        }
        // The scans travel as an immutable snapshot of the mod's own markers; the number is how many were new to the student.
        int scans = ThaumcraftApiHelper.addScans(student, ThaumcraftApiHelper.getScans(teacher));
        // Aspects are learned quietly (no discovery notice, no research points: the pools stay where they are).
        int aspects = 0;
        for (Aspect aspect : ThaumcraftApiHelper.getDiscoveredAspects(teacher).getAspects()) {
            if (ThaumcraftApiHelper.discoverAspect(student, aspect)) aspects++;
        }
        teacher.displayClientMessage(Component.translatable("tc4_example.share_tome.shared", scans, aspects, student.getDisplayName()), true);
        return InteractionResult.SUCCESS;
    }
}
```
<!-- /snippet -->

- The markers are opaque keys, made to travel unchanged: `"@"` plus the registry id for a basic scan, `"#"` plus the same
  for an advanced one. Adding is idempotent; a basic marker never replaces an advanced one, an advanced one replaces a
  basic one. `addScans` returns how many markers were new and syncs the player once, only when it is above zero.
- A scan also gives research points and may reveal a research clue. The snapshot carries neither: points are
  `addAspectPool`, and the clues (`"@KEY"` entries among the completed research) are left out of `getCompletedResearch` and
  refused by `completeResearch`, so no call copies them.
- `discoverAspect` is the quiet discovery: it syncs, but shows no notice, gives no points and does not check that the parents
  of a compound aspect are known (a scan does). `addAspectPool` with a positive amount discovers the aspect as well.
- `discoverAspect` and `addScans` change the player's knowledge and so belong to the server thread; on another thread they
  throw `IllegalStateException`. A client only knows its own player's knowledge.

### Server switches

Two settings of the original `Config` can be read: `ThaumcraftApi.isMirrorsAllowed()` (`allow_mirrors`; when it is off the
research of the mirrors, `MIRROR`, `MIRRORESSENTIA` and `MIRRORHAND`, is hidden) and `ThaumcraftApi.isWardedStoneAllowed()`
(`allow_warded_stone`; when it is on, warded doors, glass and plates are unbreakable and owned, and `FOCUSWARDING` and
`WARDEDARCANA` exist). On a client they answer the value the connected server sent. Ask them when a player acts, not
when you register: register the same research and recipes on both sides whatever the switches say.

```java
// A focus built on a mirror: it does nothing when the server switched mirrors off.
if (!ThaumcraftApi.isMirrorsAllowed()) return InteractionResultHolder.fail(stack);
```
