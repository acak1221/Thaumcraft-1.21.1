# Aspects, object tags and scanning

Everything in Thaumcraft is made of aspects: the 48 originals (`Aspect.AIR`, `Aspect.ORDER`, `Aspect.MAGIC`...)
and the ones add-ons register. An `AspectList` is an ordered map from aspect to amount.

```java
AspectList list = new AspectList().add(Aspect.FIRE, 4).add(Aspect.MAGIC, 2);
int fire = list.getAmount(Aspect.FIRE);           // 4
Aspect[] sorted = list.getAspectsSorted();        // in the order the game draws them
Aspect ignis = Aspect.getAspect("ignis");         // by tag; null when unknown
```

`add` sums amounts, `merge` keeps the larger one, `remove` subtracts, `copy` makes an independent list.

## A new aspect

The original constructor registers the aspect at once:

<!-- snippet:aspect -->
```java
// An aspect of its own: a compound of two registered aspects, a white glyph tinted with its colour (this example
// borrows an original glyph), and the lang keys tc.aspect.tempus / tc.aspect.help.tempus.
TEMPUS = new Aspect("tempus", 0xB68CFF, new Aspect[]{Aspect.VOID, Aspect.ORDER},
        ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/aspects/ordo.png"), 1);
```
<!-- /snippet -->

- The tag is unique and lower case; registering a tag twice throws.
- The texture is a square white or grey glyph (the originals are 32×32), tinted with the colour. A 1.7.10 glyph works
  unchanged.
- The components make it a compound: the research table combines them into it, and it can be crystallised and carried
  as essentia like any other. `new Aspect(tag, color, chatColor, blend)` makes a primal (no components); it is given to
  players on their first join, but wand, sceptre and amulet vis, vis relays, shards and infused ores exist for the six
  original primals only.
- Lang keys: `tc.aspect.<tag>` (the meaning under the name) and `tc.aspect.help.<tag>`.
- Register the same aspects in the same order on both sides, in `onThaumcraftReady`: a client that lacks one cannot
  decode the items and the knowledge that name it.

The add-on aspect is then stored, synced, scanned, combined at the research table, carried as essentia in jars, tubes,
phials and crystals, and drawn wherever aspects are drawn. The full contract is the Javadoc of
[`Aspect`](../reference/thaumcraft.api.aspects.md#aspect).

## Object tags: the aspects of items

<!-- snippet:object-tags -->
```java
ThaumcraftApi.registerObjectTag(new ItemStack(Items.CLOCK), new AspectList().add(TEMPUS, 4).add(Aspect.MECHANISM, 2));
// The 1.7.10 ore dictionary call keeps working: "ingotCopper" is the c:ingots/copper tag.
ThaumcraftApi.registerObjectTag("ingotCopper", new AspectList().add(Aspect.METAL, 3).add(Aspect.EXCHANGE, 1));
// 1.7.10: ThaumcraftApi.registerObjectTag(new ItemStack(item, 1, meta), aspects). There is no metadata now.
ThaumcraftApi.registerObjectTag(new ItemStack(VIS_SHARD), new AspectList().add(Aspect.MAGIC, 2).add(Aspect.CRYSTAL, 2));
```
<!-- /snippet -->

`ThaumcraftApi.registerObjectTag` takes:

| Form | Meaning |
|---|---|
| `ItemStack` | that item (its damage, for a damageable item) |
| `Item, int metadata` / `ResourceLocation, int metadata` | the damage of a damageable item; `32767` is the wildcard |
| `TagKey<Item>` | every item of the tag |
| `String` | a 1.7.10 ore dictionary name (`"ingotCopper"`) or a namespaced tag id (`"c:ingots/copper"`) |

The old ore dictionary names resolve through `OreDictionaryNames`: the names Thaumcraft 4 itself used keep their
original member lists (`thaumcraft:original_ore/*`), and the Forge convention `<kind><Material>` is the `c:` tag
(`ingotCopper` → `c:ingots/copper`, `gemNetherQuartz` → `c:gems/nether_quartz`, `dyeLightBlue` →
`c:dyes/light_blue`).

Tags are data: when `onThaumcraftReady` runs, no tag is bound yet. A tag registration is kept and applied again when a
world starts and when a client joins a server; it fills every member that has no registration of its own. A
registration of the item itself always wins over a tag.

### Complex tags

A complex tag adds aspects to those the item gets from its recipe instead of replacing them. It needs the recipes of a
running server, so the call moves from 1.7.10's `postInit` into a server start event:

<!-- snippet:complex-tag -->
```java
// A complex tag adds to the aspects the item gets from its recipe, so it needs the recipes of a running server:
// 1.7.10 add-ons called it in postInit, in 1.21.1 it goes into a server start event.
ServerLifecycleEvents.SERVER_STARTED.register(server -> ThaumcraftApi.registerComplexObjectTag(
        new ItemStack(ESSENTIA_BOWL), new AspectList().add(Aspect.VOID, 2), server.overworld()));
```
<!-- /snippet -->

On NeoForge the same call goes into `ServerStartedEvent` with `event.getServer().overworld()`.

### Reading aspects

`ThaumcraftApiHelper.getObjectAspects(stack, level)` answers the aspects a scan of the item gives (its registration, a
tag, its recipe, or the inference the port uses for items nobody registered, plus the contents of an essentia
container); the form without a level cannot resolve the aspects that come from recipes. Essentia containers report
their contents through `IEssentiaContainerItem.getAspects(stack)`: jars, crystallised essentia, wisp essence and mana
beans implement it. Phials do not in this port; their aspect is part of their scanned aspects.

## Entity tags

<!-- snippet:entity-tag -->
```java
// An entity the original never knew, by its registry id: the thaumometer gives these aspects for scanning a frog.
ThaumcraftApi.registerEntityTag("minecraft:frog", new AspectList().add(Aspect.WATER, 2).add(Aspect.BEAST, 2).add(TEMPUS, 1));
```
<!-- /snippet -->

Vanilla and Thaumcraft entities that existed in 1.7.10 keep their original names (`"Zombie"`,
`"Thaumcraft.Firebat"`); any other entity is named by its registry id (`"mymod:my_entity"`, `"minecraft:frog"`).
`ThaumcraftApi.EntityTagsNBT` filters still select a row by entity data, as in the original (the creeper's `powered`
row, for instance). An entity whose right click does something of its own (an allay takes the held item, a villager
trades) answers that click before the thaumometer can scan it.

## Scan handlers

`ThaumcraftApi.registerScanEventhandler(IScanEventHandler)` keeps the original hook: when the thaumometer finds
neither an entity nor a block, the handlers are asked on the server in registration order, and the first non-null
`ScanResult` is what was scanned. Build it with `ScanResult.item(stack)` or `ScanResult.entity(entity)`; the numeric
`id`/`meta` constructor of 1.7.10 still works but registry ids are runtime-only. As in the original, a phenomenon
(type 3) counts only for an aura node; any other phenomenon string gives nothing.

```java
// Looking at nothing while standing in water scans a water bucket.
ThaumcraftApi.registerScanEventhandler((stack, level, player) ->
        player.isInWater() ? ScanResult.item(new ItemStack(Items.WATER_BUCKET)) : null);
```

## Research points

Scanning gives the player research points of the aspects scanned. An add-on can add or read them directly:
`ThaumcraftApiHelper.addAspectPool(player, aspect, amount)` and `getAspectPool(player, aspect)` (see
[research](research.md#player-knowledge)).
