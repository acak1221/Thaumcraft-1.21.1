# Machines: requests to the Thaumcraft API and to the loader modules

Group Machines: Aspectalyzer, Thaumic Restorer, Essentia Funnel, Kinetic and Corporeal Attractors, Necromancy Tablet,
soul mould, soul aspects, mob display, Cursed Spirit's Blade. The numbers `A3`, `A7`, `A8`, `A9` (closed, see the end) are those of
`docs/API_GAPS.md` section 2. Nothing below was done by reaching into `thaumcraft.common.*`: every item is either a named
stub or answered by the API or the loader layer (`docs/HOOKS.md`), with what the answer does and does not give written down.
Priorities are the group's own opinion (P1 changes what the player sees, P3 only hardens something that works today).

## 1. Jar identification (P3)

**Need.** The Funnel must (a) recognise a jar item, ordinary or void, filled or labelled, in its slot or in a hopper
offer; (b) recognise the jar block the hopper points at; (c) know whether the item jar is "filled" in the sense of 1.7.10
`ItemJarFilled` (holds essentia, or is empty but labelled, i.e. does not stack).

**What the add-on does today.** (a) `TileFunnel.isJarItem`: the item implements `IEssentiaContainerItem` and its registry
id is `thaumcraft:jar` or `thaumcraft:jar_void`; (b) `state.is(BlocksTC.jar()) || state.is(BlocksTC.voidJar())` and the
block entity as `IAspectContainer`; (c) contents through `IEssentiaContainerItem.getAspects`, label through
`getMaxStackSize() == 1`. This works but depends on two id strings and on a stack size that happens to encode the label.

**Proposed.** `BlocksTC.isJarItem(ItemStack)`, `BlocksTC.isVoidJar(ItemStack | BlockState)`,
`@Nullable Aspect BlocksTC.getJarLabel(ItemStack)` (backed by `ItemJar.isJar/isVoid/getFilter`).

## 2. Loader hook: built-in item renderer for `mob_display` (answered; NeoForge backend still to write)

**Need.** `mob_display` stands for a mob (the output of a necromancy recipe, shown in recipe viewers). 1.7.10 drew the
mob in place of the item with an `IItemRenderer`. In 1.21.1 that is a built-in item renderer, which has no Architectury
registry. The item model is `minecraft:builtin/entity`, so **without a hook the item is invisible**.

**Answered by the lead's `ItemRenderers`** (`thaumic.tinkerer.client.hooks.ItemRenderers`, `docs/HOOKS.md` section 9).
`MachinesClient.register()` now calls `ItemRenderers.register(TTRegistry.item("mob_display"), RenderMobDisplay::render)` as
its first statement (through `MachinesClient.registerItemRenderers(Item)`), and the group's own Fabric glue
(`MachinesFabricClient`, reached by name from common code) is deleted, because both registering the same item is an error.
*What the hook gives:* one registration point for both loaders with the six-argument signature of a built-in renderer
(`ItemRenderers.Renderer`, the same as Fabric's `DynamicItemRenderer` and NeoForge's `renderByItem`); a queue that waits for the
backend; a duplicate for one item is an `IllegalStateException`. On Fabric the backend is `FabricItemRenderers`, which forwards to
`BuiltinItemRendererRegistry` (read in fabric-rendering-v1 5.2.1: it registers the same `(stack, display context, pose, buffers, light,
overlay)` and throws on a second renderer for an item). *What it does not give:* anything on NeoForge, where **the backend is
still to be written** (`RegisterClientExtensionsEvent` → `IClientItemExtensions#getCustomRenderer` → a
`BlockEntityWithoutLevelRenderer` calling the renderer, for every queued item); until then the Mob Display is invisible there.
Verified: compile; a JUnit test of the helper and the queue (`ItemRenderers.get`) and of the wiring in the source; the Fabric
backend read. **Not verified: the drawing** (no client was run; a GameTest server cannot load the client classes).
`RenderMobDisplay.clearCache()` is still called by `MachinesClient` when the client leaves a world.

## 3. Drops of a killed mob (loader event; answered, NeoForge poster still to write)

**Need.** The Cursed Spirit's Blade replaces the drops of the mob it kills with soul aspects (`LivingDropsEvent` in
1.7.10: `event.drops.clear()` and add).

**Answered by the lead's `TinkererEvents.LIVING_DROPS`** (`docs/HOOKS.md` section 8), which is the proposed event of this
request (the victim, the source, a mutable list of item entities, Looting level, "recently hit", a cancel) with a Fabric poster.
The group's note-and-refuse workaround (`LIVING_DEATH` note, `ADD` filter, end-of-level-tick spawn) is deleted;
`ItemBloodSword.onLivingDrops` clears the list and adds the soul aspects.
*What the event gives:* the loot table's items, the equipment and (for a player) the inventory as item entities of the victim, before
they enter the world, so the soul aspects appear in the call of the death, as in 1.7.10; experience orbs are not in the list (as in
1.7.10). *What it does not give:* an item that another mod spawns somewhere else with `addFreshEntity` (it is not captured, so it is
neither editable nor refused: the old workaround refused any item that appeared at the victim's position), and the items of a mob
of another mod that spawns them before it calls `super.dropAllDeathLoot` (`docs/HOOKS.md` section 13 item 2; in vanilla 1.21.1 only the
fox is such a mob, it is handled and has no row in the table). **NeoForge needs
its poster** (a handler of `LivingDropsEvent` calling `TinkererEvents.postLivingDrops`); not written, so the blade does not reap there
until it is.

## 4. Not requested, for the record

- **ComputerCraft / OpenComputers peripherals** of the Aspectalyzer and the Kinetic Attractor are not ported (the lead's
  decision list in `API_GAPS.md` 1.11). The methods exist as plain Java: `TileAspectAnalyzer.hasItemMethod /
  itemHasAspectsMethod / getAspectsMethod / getAspectsAmountsMethod`, `TileMagnet.isPulling / setPulling / getRedstone`,
  `TileMobMagnet.adult / setAdult`. A compat glue (CC:Tweaked) only has to call them.
- **Pedestals.** The tablet reads `BlocksTC.get("arcane_pedestal")` as a `net.minecraft.world.Container` (the block entity
  `TilePedestal` is a `WorldlyContainer`). If the pedestal ever stops being a `Container` the tablet finds nothing; a
  public `IPedestal`-like interface would make that explicit.

## 5. Shared-file requests to the lead (files of other groups)

These need a file that this group does not own; nothing was edited there.

1. **Cursed Spirit's Blade is not enchantable with the vanilla sword enchantments** (Sharpness, Smite, Bane of Arthropods,
   Looting, Knockback, Fire Aspect, Sweeping Edge, Unbreaking, Mending), because every one of them hangs on
   `#minecraft:swords` (`#minecraft:enchantable/sword`, `sharp_weapon`, `weapon`, `fire_aspect`, `durability`, `vanishing`)
   and `thaumictinkerer:blood_sword` is in no vanilla sword tag. The 1.7.10 `ItemSword` could take them. Fix, two files:
   - `data/minecraft/tags/item/swords.json` (KamiGear's file): add `{ "id": "thaumictinkerer:blood_sword", "required": false }`.
   - `data/thaumictinkerer/tags/item/enchantable/vampirism.json` (Enchantments' file) includes `#minecraft:swords`, which
     would then include the blade, while the original `EnchantmentVampirism.canApply` refused `ItemBloodSword`. Replace the
     `#minecraft:swords` entry by a dedicated tag `thaumictinkerer:swords_without_blood_sword` (the same sword list without
     the blade) or by an explicit list of the swords. The four strike enchantments list `blood_sword` explicitly already.
2. **NeoForge backend of `ItemRenderers` for `mob_display`:** section 2 above (the Fabric one exists).
3. **`assets/minecraft/atlases/blocks.json`** carries the five entity-style textures of this group's JSON models (`models/repairer`,
   `magnet_n`, `magnet_s`, `mob_magnet_n`, `mob_magnet_s`). Two groups must not both create that file in the same jar: if
   another group needs sources in the block atlas, merge them into this one file.
4. **Research `PERIPHERALS`** (parent of `ASPECT_ANALYZER`, a ComputerCraft-only entry in the fork) is created by
   `BlockAspectAnalyzer.ensurePeripheralsEntry()` unless `ResearchHelper` registers it first.

## 6. Closed requests (the API now answers them; the add-on uses the API)

| Request | Now |
|---|---|
| A3 scan check of the Aspectalyzer screen | `ThaumcraftApiHelper.hasScannedItem(player, stack)` in `AnalyzerKnowledge.hasScanned` |
| A7 keys of Thaumcraft's entities and the key sound | `ThaumcraftEntities.WISP / FIREBAT / BRAINY_ZOMBIE / GIANT_BRAINY_ZOMBIE` in `EnumMobAspect`, `ThaumcraftSoundIds.KEY` in `BlockMagnet` |
| A8 client sparkles | `ThaumcraftClientFX.sparkle(level, x, y, z, type)` in `TileRepairer` (type 0) and `TileMagnet` (2 pulling, 4 pushing) |
| A9 drawing an aspect tag with its amount | `AspectRenderer.draw(graphics, aspect, x, y, amount, 0)` in `GuiAspectAnalyzer` |
| Section 2: built-in item renderer of the Mob Display | `ItemRenderers.register(...)` from `MachinesClient.register()`; Fabric backend `FabricItemRenderers` (NeoForge backend still open) |
| Section 3: drops of a killed mob | `TinkererEvents.LIVING_DROPS` in `ItemBloodSword.onLivingDrops` (Fabric poster done, NeoForge poster still open) |

