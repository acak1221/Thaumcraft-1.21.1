# BasicItems: deviations from Thaumic Tinkerer 1.7.10

Group BasicItems: Spellbinding Cloth, Talisman of Remedium, Talisman of Withhold, Tome of Knowledge Sharing, Infused
Scribing Tools, Helmet of Revealing, the helpers `ExperienceHelper`, `ItemNBTHelper` and `MiscHelper`, and the two client
handlers `HUDHandler` and `ClientTickHandler`. Specification: the 1.7.10 sources in `/home/user/gtnewhorizons/thaumictinkerer`
(commit in `NOTICE.md`). Everything not listed here is ported as it was: numbers (35 uses of the cloth, 100 points of the
talisman, ten for the warp effects, range 3, 1500 points, cost 10, 800 ink, 500 durability, 5% discount), research keys,
coordinates, aspects, recipes, tooltips, sounds and messages. Requests to the API and to the lead are in
`docs/api-requests/BasicItems.md`.

Reading key: **forced** means 1.21.1 cannot do it the old way; **fix** means the original crashed or was exploitable;
**choice** means a free decision of the port.

## 1. Metadata and stack tags become data components (forced)

| 1.7.10 | 1.21.1 |
|---|---|
| Talisman of Withhold damage value 0 / 1 (off / absorbing), two icons | boolean component `thaumictinkerer:xp_talisman_absorbing` (absent = off), item model predicate `thaumictinkerer:enabled` |
| Talisman of Withhold tag `xp` | int component `thaumictinkerer:xp_talisman_xp` (absent = 0), synced to the client (tooltip) |
| Talisman of Remedium tag `enabled` | boolean component `thaumictinkerer:cleansing_talisman_enabled` (absent = off), the same model predicate |
| Tome tags `player`, `research`, `aspects`; the unassigned name `[none]` | one component `thaumictinkerer:share_tome` holding the player name, the player id, the research keys and the aspect tags; an unassigned tome has no component |

A value equal to the default is stored as no component, so a talisman that was switched off again, or an empty one, is the
same stack as a new one (stacks of the same kind merge). The components are saved and sent like any other
(`BasicItemComponents`). Neither the stacks nor the worlds of 1.7.10 are migrated (fresh worlds only).

`ItemNBTHelper` keeps its 1.7.10 function names and stores under the free-form component `minecraft:custom_data` for the
items that have no component of their own. **One thing could not stay:** `getNBT` handed out the live tag, and 1.21.1's
`CustomData` is immutable, so `getNBT` is a copy and every change goes through a setter, `injectNBT` or the new `update`.
`setCompound` no longer refuses the key `ench` (enchantments are a component).

## 2. Spellbinding Cloth

- **The recipe is a custom recipe type (forced).** 1.7.10 registered an `IRecipe` object with `CraftingManager` and an
  `ItemCraftedEvent` handler that emptied the slot of the enchanted item and added one damage to the cloth, and the crafting
  slot handed the cloth back as the container item. 1.21.1 has `CustomRecipe`: `SpellClothRecipe` (serializer
  `thaumictinkerer:spell_cloth`, file `data/thaumictinkerer/recipe/spell_cloth.json`, `BasicItems.RECIPE_SERIALIZERS`)
  matches exactly one cloth and one enchanted item and nothing else, `assemble` returns the item without its enchantments, and
  `getRemainingItems` is the replacement of the event handler: it hands back the cloth with one more damage in the cloth's slot.
  `SpellClothCraftingHandler` is now the function `wornCloth` of that rule; no event handler is needed.
- **Wear.** The 1.7.10 crafting slot destroyed the container item only when its damage went above its maximum, so the cloth
  works 36 times (damage 0 to 35, and the 36th cleaning destroys it). `wornCloth` returns an empty stack in that case.
- **Which items.** `isEnchanted()` of 1.21.1 is the component `enchantments`, as the tag `ench` was; the enchantments an
  enchanted book carries (`stored_enchantments`, tag `StoredEnchantments` in 1.7.10) are not wiped, in 1.7.10 either.
  An item whose class implements `thaumic.tinkerer.api.INoRemoveEnchant` is refused (no class of the add-on does).
  The rest of the item (name, repair cost, other components) stays, as 1.7.10 removed only the key `ench`.
- **Stacks of enchanted items (choice).** 1.7.10 copied the whole slot (`stack.copy()` with its count) and emptied the slot,
  so a stack of N enchanted items was wiped in one go for one use of the cloth. The 1.21.1 crafting slot takes one item from
  each slot, so the recipe gives one item per cleaning. Enchanted items do not stack in survival, so this only matters for
  stacks made with commands.
- **Anvil (choice).** `setNoRepair()` has no counterpart: 1.21.1 lets two damaged cloths be combined in an anvil.
- **Colour.** `getColorFromItemStack` is an item colour handler (`BasicItemsClient`) with the same formula and
  `Color.HSBtoRGB`; the model is a plain generated model, tinted on layer 0.
- Crucible recipe, research (`SPELL_CLOTH`, parent hidden `ENCHFABRIC`) and the page text are unchanged; the enchanted fabric
  ingredient is `thaumcraft:enchanted_fabric` (`itemResource:7`).

## 3. Talisman of Remedium (Cleansing Talisman)

- **Potion ids become effect keys (forced).** The harmful-effect test was a reflection on `Potion.isBadEffect` and the five
  warp potions were found by their configured ids. Now an effect is harmful when its category is `HARMFUL`
  (`MobEffect.getCategory()`), and the five that cost ten points are `WarpEffects.BLURRED_VISION`, `DEATH_GAZE`,
  `INFECTIOUS_VIS_EXHAUST`, `SUN_SCORNED` and `UNNATURAL_HUNGER` (the keys of the API). The warp ward is skipped as before
  (the port registers it as beneficial anyway). 1.21.1 marks a few more effects as harmful than 1.7.10 did (for example
  Darkness and Levitation); the talisman removes those too.
- **The decision is a pure function (choice).** `CleansingLogic.plan` is the 1.7.10 loop (one new effect per pass at a cost;
  an effect that returns right after its removal becomes permanent and is removed for nothing; permanent effects that stop
  are forgotten; burning is handled first) moved out of `onWornTick`, so JUnit tests it. The effects with an infinite
  duration or less than a second left are skipped, as 1.7.10 skipped `duration < 20`.
- **Wear.** `damageItem` becomes `hurtAndBreak`; Unbreaking works as in 1.7.10; a talisman that breaks leaves an empty stack
  in its accessory slot (1.7.10 set the first Baubles slot to null; the slots of 1.21.1 treat an empty stack as free) and
  plays the break sound.
- **Memory per wearer.** 1.7.10 kept two maps on the item instance (every talisman shared them). The port keeps them in a
  static map keyed by the player's id and forgets a player when he leaves and every player when the server stops.
- **Click (choice).** The sneak click toggles, as before. A plain click did nothing in 1.7.10; in the port it puts the
  talisman into the amulet slot (`IBauble.equipsOnUse` is true while not sneaking), like every other bauble of this port.
  The Baubles menu works as always.
- **Unbreaking and the enchantability (forced).** `getItemEnchantability() = 10` is `getEnchantmentValue()`. 1.21.1
  decides what an item may take by item tags, so the talisman is listed in `minecraft:enchantable/durability`
  (`data/minecraft/tags/item/enchantable/durability.json`): it can take Unbreaking from the enchanting table, as in 1.7.10.
- **Mending.** The Smokey Quartz (the dark quartz item) mends it at an anvil (`getIsRepairable`).
- The two icons are an item model override on the predicate `thaumictinkerer:enabled`.

## 4. Talisman of Withhold (XP Talisman)

- **Orbs.** `getEntitiesWithinAABB(EntityXPOrb)` becomes `getEntitiesOfClass(ExperienceOrb)` over the same cube (3 blocks in
  every direction); the consumption rule, including its quirk (`maxXP -= xpToAdd` subtracts the running total, so the loop
  can stop early), is copied. The zap sound is `thaumcraft:zap` with the volume `value / 10`.
- **Effects (forced, API gap A8).** The green wisp of Thaumcraft's particle engine at an absorbed orb (client) and the six
  sparkles at the player when a bottle is filled are internals of the mod; the port draws a green dust particle for the orb
  (client) and six happy-villager sparkles at the player (sent by the server). Same moment and place, another look.
- **Bottle.** `consumeInventoryItem(glass_bottle)` becomes a search of the main inventory, then of the off hand; the
  Bottle o' Enchanting goes into the inventory or is dropped at the player when it is full.
- **Click (choice).** The click toggles and bottles, so the talisman answers `equipsOnUse` with false: it is put on in the
  Baubles menu (toggle it first, wear it after, as in 1.7.10).
- **Tooltip.** The line "XP: n" was not translatable; it is the key `ttmisc.xpTalisman.xp` (`XP: %s`; Russian
  `Опыт: %s`, in `lang-extra/basic_items.*.json`).
- **Research icon.** The icon of the research entry is the switched-on talisman (1.7.10: damage 1).

## 5. Tome of Knowledge Sharing

- **Players are found by id (forced).** 1.7.10 found a player by name (`ResearchManager.getResearchForPlayer(name)`), which
  also answered for a player that was not online but whose data was loaded. 1.21.1 keeps knowledge on the player entity and
  the API reads it from a `Player`, so the tome stores the id of the player next to the name (the tooltip shows the name). If
  the bound player is online, the tome hands over his latest research and aspects; if not, the snapshot written into the
  tome, exactly the 1.7.10 fallback (API gap N2).
- **The snapshot is copied, not shared.** The research keys and the aspect tags are copied (the 1.7.10 lists, aspects sorted
  by tag). Unknown keys and aspects are ignored. The completion pays the warp of the research (the original did).
- **Clue markers are not copied (forced, API request R1).** The 1.7.10 list of completed research also held the `@KEY`
  entries that a scan wrote to reveal a hidden entry; the API neither returns nor accepts them. A research that was
  completed arrives complete; one that was only revealed does not arrive revealed.
- **Scanned objects** were never part of the 1.7.10 tome and are not copied now.
- **Messages.** `addChatMessage` becomes `sendSystemMessage` from the server only (1.7.10 ran the click on both sides).
- **Recipe (forced).** 1.7.10 registered the shaped recipe only when `enableSurvivalShareTome` was on. A data recipe is
  always loaded, so the file `share_tome.json` is of the type `thaumictinkerer:share_tome`
  (`ShareTomeRecipe extends ShapedRecipe`, same format and network form) which matches only while the option is on; the
  research page shows it as the shaped recipe it is, and `ItemShareBook.getRecipeItem` still answers null with the option off.
  The pattern's Scribing Tools had the damage value 0, which a 1.7.10 shaped recipe matches exactly: only an unused set
  made the tome. `ShareTomeRecipe.matches` keeps that rule by refusing a grid that holds a used `thaumcraft:inkwell`.
- **Network.** The whole snapshot is sent with the stack, as 1.7.10 did with `getShareTag`. A creative player who moves the
  tome in his inventory therefore keeps its contents.

## 6. Infused Scribing Tools

- 800 ink as durability, `IScribeTools` (the research table takes any such item), one stack, shining like an enchanted
  item (`isFoil`), no research entry, not in the creative tab, name "(NYI)": the item is unfinished in 1.7.10 too and its
  infusion recipe stays under a research key that does not exist, so it is only for the commands.
- **Refill recipe.** `data/thaumictinkerer/recipe/infused_inkwell0.json`: eight ink sacs (`dye:0`) around tools of any wear
  give a full set (the center was `new ItemStack(this, 1, 32767)`).
- **Look (choice).** 1.7.10 drew the Thaumcraft icon (`ConfigItems.itemInkwell.getIconFromDamage`); the port uses the
  add-on's own `infused_inkwell.png`, which is the same picture.

## 7. Helmet of Revealing

- **Armour material (forced).** `ThaumcraftApi.armorMatThaumium` is a registry entry in 1.21.1. The helmet has its own
  material `thaumictinkerer:revealing` with the numbers of Thaumcraft's Thaumium armour (2/6/5/2 armour points,
  enchantability 25, iron equip sound, thaumium ingot to mend), because the worn texture is part of the material. Durability
  500 as in `setMaxDamage(500)`.
- **Worn texture (forced).** `getArmorTexture` returned `textures/model/revealing.png`; the material's layer is
  `textures/models/armor/revealing_layer_1.png`, a byte-for-byte copy of the converted `textures/models/revealing.png`
  (a 64 x 32 humanoid layer).
- **Tags (forced).** The Repair enchantment of Thaumcraft is available for an `IRepairable` item only when the item is in the
  tag `thaumcraft:enchantable/repair` (`data/thaumcraft/tags/item/enchantable/repair.json`; the same file lists the Cursed
  Spirit's Blade of the Machines group, the other `IRepairable` item). Armour enchantments and Unbreaking come from
  `minecraft:enchantable/head_armor` and `minecraft:enchantable/durability`, two files of this group.
- **The debugging message is kept.** A click on a block tells the chat "Tile Entity: class ..." (or "Tile Entity: null"): a
  debugging leftover of 1.7.10 that the fork still has. The 1.7.10 item said it twice, once from the client and once from the
  server; the port says it once, from the server.
- The interfaces (`IRepairable`, `IRevealer`, `IGoggles`, `IVisDiscountGear`, `IRunicArmor`), the discount of 5, the tooltip
  line in dark purple and the arcane recipe (goggles and Thaumium helmet, 5 of each primal) are as before.

## 8. Client: HUDHandler and ClientTickHandler

- **Focus of Dislocation overlay.** The panel (box at 4, 50, the line "Currently Dislocating", the line "Extra Contents" when
  the block carries block-entity data, the block drawn at 16 x 16) is drawn with Architectury's `ClientGuiEvent.RENDER_HUD`
  while a wand is in the main hand, no screen is open and the GUI is not hidden. Thaumcraft's `dialBottom` option (the
  position of its own wand dial) is not in the API; the panel keeps the default place. The unicode flag of the font is the
  `minecraft:uniform` font.
- **The group does not know the focus (rule 3).** `HUDHandler` asks the focus item of the wand
  (`WandHelper.getFocusItem`) for the contract `thaumic.tinkerer.common.item.IDislocationHudSource` (the picked block and
  "has extra contents"), or a source given to `HUDHandler.setDislocationSource`. Neither is set, so the overlay of this
  class stays inert. **The Foci group has meanwhile ported the same overlay itself** (`DislocationHUDHandler`, registered by
  `FociClient`, reading its own data component), so the panel is drawn exactly once, by that class. Two ways to end the
  duplicate, for the lead: (a) keep the Foci group's class and delete `HUDHandler` (and its line in
  `BasicItemsClient.register()`); or (b) delete `DislocationHUDHandler` and let `ItemFocusDislocation` implement
  `IDislocationHudSource`. Version (a) is the smaller change; the only difference between the two drawings is that the 1.7.10
  box of the second line is as wide as the first line's text (mine) or as wide as the wider of the two (theirs).
- **ClientTickHandler.** Of its three jobs only the tick counter is left: the swap of the Thaumonomicon page for the
  ComputerCraft peripherals research (`GuiResearchPeripheral`, which opened a web address on Enter) is not ported with
  ComputerCraft, and the tick of `ToolModeHUDHandler` is registered by the Kami group itself and must not run twice.
  `GuiResearchPeripheral` is skipped for the same reason.

## 9. Helpers

- **`ExperienceHelper`.** The 1.7.10 helper hard-coded the 1.7.10 experience curve (17 points a level up to level 15, 3
  more per level up to 30, 7 more beyond). 1.21.1's curve is `2L+7`, `5L-38`, `9L-158` points per level, and
  `Player.getXpNeededForNextLevel()` follows it, so the conversion between points and levels does too
  (`getExperienceForLevel(30) = 1395`, was 825). The total never goes below 0 (the 1.7.10 loop gave level -1).
- **`MiscHelper`.** `Vector3` is `Vec3`; the block event is Architectury's `BlockEvent.BREAK` (asked for a `ServerPlayer`
  only; a break by something else is not vetoed), `server()` is `GameInstance.getServer()`, the stack trace goes to the log.
  The block metadata argument is the default state of the block.

## 10. Things the original did that the port keeps as they are

- Both talismans are amulets; their sneak clicks switch them on and off with the orb sound at volume 0.3 and pitch 0.1.
- The absorbing loop of the Talisman of Withhold keeps its arithmetic quirk (section 4), and the Talisman of Remedium keeps its
  "one new effect per pass, permanent when it comes back" rule (section 3).
- `ItemShareBook.getResearchItem` registers a text page and, with the option on, the recipe page; the entry is a stub that is
  auto-unlocked and round. The helmet and the talisman keep their research positions, parents and pages.
