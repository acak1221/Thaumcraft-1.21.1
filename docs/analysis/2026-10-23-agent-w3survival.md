# Wave three — the survival closure of the catalogue (agent `w3survival`)

Date: 2026-10-23. Worktree: `/home/cyberfox-kali/wt/w3survival`, branch `agent/w3survival`.

## 1. What was asked and what was done

The wave two survival acceptance (agent `survival`, `docs/analysis/2026-10-23-agent-survival.md`
§3.2) found exactly one closure gap: the **arcane furnace** had no construct, so the four
meat nuggets had no source and the **triple meat treat** was not craftable. The closure
scenario `survival_chain_ingredients_close_on_natural_acquisition` asserted that the set of
mod ingredients without any source was *exactly* those four.

This wave closes that gap:

* the original event two wand construct — the three by three by three shell that becomes
  the arcane furnace — is ported (`ArcaneFurnaceFormation`);
* the arcane furnace gets the **item** of the original and the light of the original;
* the four meat nuggets get their **named natural source** (the smelting bonus of the
  arcane furnace, the original `ConfigRecipes.java:507-510`);
* the closure scenario now asserts that the open set is **empty**, and a new scenario
  proves the construct on the real server.

The oracle is `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`; every citation below
is `file:line` of that byte-exact 4.2.3.5 decompilation.

## 2. The chain of the original, with the oracle

### 2.1 Where the four meat nuggets come from

The four edible meat nuggets of the original are `ConfigItems.itemNuggetChicken`,
`itemNuggetBeef`, `itemNuggetPork` and `itemNuggetFish`
(`thaumcraft/common/config/ConfigItems.java:411-418`, the `ItemNuggetEdible` items with
the food value of one hunger and the saturation of three tenths,
`thaumcraft/common/items/ItemNuggetEdible.java:28-33`).

**They are not a mob drop.** The whole oracle mentions the four items in exactly five
places:

| oracle | what it is |
|---|---|
| `ConfigItems.java:411-418` | the registration of the four items |
| `ConfigRecipes.java:507-510` | the four **smelting bonuses** of the arcane furnace |
| `ConfigRecipes.java:410-413` | the four **triple meat treat** recipes that consume them |
| `ConfigAspects.java:410-413` | their aspect, one hunger each |
| `VillageWizardManager.java:43-45` | the wizard village trades twenty four to thirty one nuggets of **chicken and beef only** for an emerald |

There is no drop of the four nuggets anywhere in the original: no `EntityTaintChicken`
or any other entity drops them (`EntityTaintChicken.java:156-162` drops the taint
resource, not a nugget), and the `EntityPech` loot bag only holds the *ore* nuggets
(`EntityPech.java:714-727`, the metadata of `ConfigItems.itemNugget`).

The real source is therefore the **arcane furnace**: the original registers the smelting
bonus of the raw chicken, the raw beef, the raw pork and the raw fish
(`ConfigRecipes.java:507-510`), and the arcane furnace pays it for every item it ejects
(`thaumcraft/common/tiles/TileArcaneFurnace.java:237-259`: one unit with a chance of one
in four without a bellows, one roll per bellows with a chance of 44 % after that). The
triple meat treat is the only consumer of the four nuggets
(`ConfigRecipes.java:410-413`).

So the original chain is:

```
raw chicken / beef / pork / fish
        |  smelted in the arcane furnace (the construct of the event two)
        v
nugget_chicken / nugget_beef / nugget_pork / nugget_fish
        |  + sugar, shapeless
        v
triple_meat_treat
```

### 2.2 How the arcane furnace is constructed

* The research: `INFERNALFURNACE` (`ConfigResearch.java:181`), the page of the construct
  is the plain list of the blueprint (`ConfigResearch.java:181`, the second page is
  `(List)recipes.get("InfernalFurnace")`).
* The registration: `ConfigRecipes.java:89` stores the blueprint, the aspect price
  (**fifty fire and fifty earth**) and the three by three by three shape;
  `ConfigRecipes.java:90-92` registers the three trigger blocks of the event two — the
  obsidian (`Blocks.field_150343_Z`), the nether brick (`Blocks.field_150385_bj`) and the
  iron bars (`Blocks.field_150411_aY`), each with the metadata `-1` (any).
* The dispatch: `WandManager.java:716-718`, the case two of `performTrigger` asks for the
  complete `INFERNALFURNACE` research **first** and then calls `createArcaneFurnace`.
* The construct itself: `WandManager.java:332-347` (`createArcaneFurnace`) walks the
  twenty seven candidate volumes that contain the clicked block (the corner from
  `x - 2` to `x` in every axis) and takes the first one that fits **and** that the wand
  can pay; `WandManager.java:349-373` (`fitArcaneFurnace`) is the blueprint:

  | layer | corners | edges | middle |
  |---|---|---|---|
  | top | nether brick | obsidian | **air** |
  | middle | nether brick | obsidian (one of them may be **iron bars**) | **lava** |
  | bottom | nether brick | obsidian | **obsidian** |

  The iron bar is mandatory: `fitArcaneFurnace` returns `fencefound`, the flag that is
  set only when the iron bar was accepted (`WandManager.java:357-372`), and exactly one
  iron bar is allowed, on a cross slot of the middle layer.
  The price is `WandManager.java:337`: `consumeAllVisCrafting(..., FIRE 50, EARTH 50, true)`
  — the original multiplies every amount by one hundred and applies the consumption
  modifier of the wand (`ItemWandCasting.java:306-338`).
* The replacement: `WandManager.java:375-407` (`replaceArcaneFurnace`) gives every cell of
  the shell the metadata of its position in its layer (one to nine, the z offset times
  three plus the x offset plus one), the lava becomes the metadata **zero** core, the
  iron bar becomes the metadata **ten** output nozzle, the empty cell of the top layer is
  left alone, and the wand sound `thaumcraft:wand` plays at the corner of the shell
  (`WandManager.java:406`).
* The block: `ConfigBlocks.java:266` creates it, `ConfigBlocks.java:336` registers it
  **with the default item block**, so the original has an item of its own. The core is
  the metadata zero block (`BlockArcaneFurnace.java:379-387`: the metadata zero block has
  the `TileArcaneFurnace`, the metadata two, four, five, six and eight blocks have the
  `TileArcaneFurnaceNozzle`), and the core and the nozzle light up to thirteen
  (`BlockArcaneFurnace.java:215-221`).
* What the furnace is for: it smelts (`TileArcaneFurnace.java:130-158`), it ejects the
  result towards its output nozzle (`TileArcaneFurnace.java:209-219`, the metadata ten
  neighbour) and it pays the smelting bonus (`TileArcaneFurnace.java:237-259`). It is
  the machine of the bellows (`TileArcaneFurnace.java:162-173`) and of the vis network
  (`TileArcaneFurnace.java:131-133`).

## 3. The port before this wave

* `thaumcraft:arcane_furnace` was registered as a block
  (`ThaumcraftLegacyTiles.java:58`) with the eleven original metadata values flattened
  into a `variant` block state and the output nozzle as its own block
  (`BlockArcaneFurnaceNozzle.java`), the tiles were complete
  (`TileArcaneFurnace.java`, `TileArcaneFurnaceNozzle.java`) and the smelting bonus
  catalogue was complete (`data/thaumcraft/tc4/smelting_bonus.json`).
* **But there was no way to build it.** The event two trigger was never ported, exactly
  like the oculus of the event six before the wave two agent. The port's own comment
  said so: *"The arcane furnace and the death fluid have no item in the original
  either"* (`ThaumcraftLegacyTiles.java:113`) — the second half of that sentence is
  wrong, the original registers the block with the default item block
  (`ConfigBlocks.java:336`).
* The consequence, measured by the wave two closure scenario: the four meat nuggets had
  **no source at all**, and the triple meat treat was not craftable.

## 4. The fix

### 4.1 The construct — `common/src/main/java/thaumcraft/common/lib/ArcaneFurnaceFormation.java`

A new class that follows the pattern of the wave two oculus
(`common/src/main/java/thaumcraft/common/lib/eldritch/OculusFormation.java`):

| original | native |
|---|---|
| `WandManager.java:332-347` the twenty seven candidate volumes | `ArcaneFurnaceFormation.candidate(...)` |
| `WandManager.java:349-373` the blueprint and the iron bar rule | `ArcaneFurnaceFormation.fits(...)` and `expected(...)` |
| `WandManager.java:337` fifty fire and fifty earth | `ArcaneFurnaceFormation.FORM_VIS` and the native pricing `ArcaneVis.price`/`debit` |
| `WandManager.java:375-397` the metadata of every cell | `ArcaneFurnaceFormation.replace(...)` |
| `WandManager.java:406` the wand sound | the native sound event `thaumcraft:wand` |
| `WandManager.java:716-718` the research gate | `KnowledgeAccess.get(player).isResearchComplete("INFERNALFURNACE")` |
| `ConfigRecipes.java:90-92` the three trigger blocks | `ArcaneFurnaceFormation.isTriggerBlock(...)` |

The dispatch is in `ItemWandCasting.useOn` (`ItemWandCasting.java:205-212`), right after
the oculus and before the other native construct handlers.

Forced 1.21.1 changes, all documented in the class:

* the original answered the click through the block metadata of the wand trigger
  registry, which no longer exists — the native port answers the three blocks the
  original registered;
* the original metadata of the furnace became the `variant` block state of
  `BlockArcaneFurnace` and the separate half block `BlockArcaneFurnaceNozzle` of the
  original metadata ten.

### 4.2 The item and the light of the furnace

* `ThaumcraftLegacyTiles.java:113-118` registers the item `thaumcraft:arcane_furnace`
  (the original `ConfigBlocks.java:336`) and
  `assets/thaumcraft/models/item/arcane_furnace.json` is its model. The localisation was
  already there: `block.thaumcraft.arcane_furnace` is "Arcane Furnace" and
  `tile.thaumcraft.arcane_furnace.name` is the original name "Infernal Furnace".
* `ThaumcraftLegacyTiles.java:91-99` now gives the output nozzle the light of thirteen
  of the original (`BlockArcaneFurnace.java:215-221`: the metadata zero **and** the
  metadata ten block light up).

## 5. The scenario

`SurvivalProgressionRuntimeChecks.arcaneFurnaceIsBuiltByAWandAndPaysTheMeatNuggets`
(the wrapper name `survival_arcane_furnace_builds_and_pays_the_meat_nuggets`, registered
in `scripts/porting/required-gametests.json` and in both loader wrappers) proves on the
real dedicated server that

1. the shell of the original is built from the blueprint of `WandManager.java:351-372`;
2. a real wand click on the shell forms the furnace — the core is the metadata zero
   block with the `TileArcaneFurnace`, the iron bar became the output nozzle, the middle
   of the top layer is still empty;
3. the wand paid **exactly** fifty vis of fire and fifty vis of earth;
4. the four meat nuggets of the original are the smelting bonus of the raw chicken, the
   raw beef, the raw pork and the raw fish.

The closure scenario itself was **strengthened, not weakened**: the open set of the mod
ingredients without a source is now asserted to be **empty**
(`SurvivalProgressionRuntimeChecks.java:320-333`), and the four nuggets are listed with
their named natural source in `WORLD_SOURCES`.

A unit test (`common/src/test/java/thaumcraft/wands/ArcaneFurnaceFormationTest.java`)
pins the pure rules of the construct: the vis price, the blueprint of the three layers
and the three trigger blocks. It never constructs a block of the port.

## 6. Deliberate deviations and open items

1. **The nether brick identity of the shell.** The original builds the corners of the
   shell from the 1.7.10 `Blocks.nether_brick` (the plain nether brick block;
   `WandManager.java:351`, `ConfigAspects.java:193`). The port's identifier corpus
   (`data/thaumcraft/tc4/early_object_aspects.json`, the row of `ConfigAspects.java:193`)
   maps the original nether brick block to the 1.21.1 `minecraft:nether_bricks` block, and
   the aspect binding, the collapse (`BlockArcaneFurnace.java:224`) and the loot table
   (`data/thaumcraft/loot_table/blocks/arcane_furnace.json`) all follow that corpus. The
   construct follows it as well, so the furnace, its corners and its aspect binding stay
   consistent inside the port, and the patterned nether bricks are craftable in 1.21.1.
   *Open for the owner of the corpus:* the 1.21.1 `minecraft:nether_brick` is the plain
   block of the original, so the corpus row, the collapse and the loot table could be
   moved to it. That is a different unit with its own fingerprints and was not touched
   here.
2. **The research page of the construct shows no diagram.** The original draws the three
   by three by three blueprint on the page of the `INFERNALFURNACE` research
   (`ConfigResearch.java:181`). The port's research book draws the diagram of the
   infusion altar, of the thaumatorium and of the advanced alchemical furnace, but not of
   the six other constructs — the arcane furnace is one of them, so the page shows the
   text pages and no diagram. The construct itself is fully playable; only the
   *presentation* of the blueprint page is missing, exactly as for the other six
   constructs.
3. **The metadata zero cell of the collapse.** The original restores the core of the
   furnace to the item of the numeric id zero (`BlockArcaneFurnace.java:347`,
   `Item.getItemById(0)`, the stone block of 1.7.10) and drops it as well. The port
   restores the core to air (`LegacyTileRules.java:117-122`, the rule the audited 1.12
   reference uses, `src/main/java/thaumcraft/common/blocks/BlockArcaneFurnace.java`, its
   `getRestoredState`). This is a cosmetic difference of the collapse, it is inherited
   from the 1.12 reference and it is **not** part of the closure chain; it is recorded
   here instead of being changed, because the unit of the restore is already closed and
   the change would touch an accepted unit test.
4. **The nether brick of the collapse and of the drops** follows the corpus of point 1.

## 7. How it was run

See §8 for the real output of this wave.

## 8. The files

* `common/src/main/java/thaumcraft/common/lib/ArcaneFurnaceFormation.java` — new, the
  original event two construct.
* `common/src/main/java/thaumcraft/common/items/wands/ItemWandCasting.java` — the
  dispatch of the construct.
* `common/src/main/java/thaumcraft/common/blocks/BlockArcaneFurnace.java` — the public
  `state(variant)` and `updateLevel(...)` helpers.
* `common/src/main/java/thaumcraft/common/blocks/BlockArcaneFurnaceNozzle.java` — the
  public `state()` helper.
* `common/src/main/java/thaumcraft/common/config/ThaumcraftLegacyTiles.java` — the item
  of the furnace and the light of the nozzle.
* `common/src/main/resources/assets/thaumcraft/models/item/arcane_furnace.json` — the
  model of the item.
* `common/src/gameTest/java/thaumcraft/common/testing/SurvivalProgressionRuntimeChecks.java`
  — the closed closure scenario and the new scenario.
* `common/src/test/java/thaumcraft/wands/ArcaneFurnaceFormationTest.java` — the unit
  test of the pure rules.
* `fabric/src/gameTest/java/thaumcraft/fabric/testing/ThaumcraftGameTests.java`,
  `neoforge/src/gameTest/java/thaumcraft/neoforge/testing/ThaumcraftGameTests.java` —
  the wrappers of the new scenario.
* `scripts/porting/required-gametests.json` — the new required case.
* `docs/porting/scope-ledger.json` — the evidence of the construct for
  `thaumcraft/common/items/wands/WandManager.java`,
  `thaumcraft/common/config/ConfigRecipes.java` and
  `thaumcraft/common/blocks/BlockArcaneFurnace.java`.
