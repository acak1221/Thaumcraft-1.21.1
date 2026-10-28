# Machines: requests to the Thaumcraft API and to the loader modules

Group Machines: Aspectalyzer, Thaumic Restorer, Essentia Funnel, Kinetic and Corporeal Attractors, Necromancy Tablet,
soul mould, soul aspects, mob display, Cursed Spirit's Blade. The numbers `A3`, `A7`, `A8`, `A9` are those of
`docs/API_GAPS.md` section 2. Nothing below was done by reaching into `thaumcraft.common.*`: every item is either a named
stub or a workaround with the loss written down. Priorities are the group's own opinion (P1 changes what the player
sees, P3 only hardens something that works today).

## 1. Scan check for the Aspectalyzer screen (A3, P1)

**Need.** The screen of the Aspectalyzer shows the aspects of the item in the slot only if the player has scanned that
item (1.7.10: the item hash in `Thaumcraft.proxy.getScannedObjects()`, keys `@hash` and `#hash`).

**Proposed signature.** `ThaumcraftApiHelper.hasScannedItem(Player player, ItemStack stack)`; it has to work for the
client player, because the screen asks every frame (the knowledge is synced to the client by the mod already).

**Backed by.** `ScanManager.hasBeenScanned(KnowledgeAccess.get(player), "@" + itemId, ITEM)` (`ItemThaumometer` shows the
call).

**Stub in the add-on.** `thaumic.tinkerer.common.core.helper.AnalyzerKnowledge.hasScanned(Player, ItemStack, AspectList)`.
Until the API exists it answers "the player has discovered every aspect of the item" (`ThaumcraftApiHelper.hasDiscoveredAspect`).
That is more generous than the original: an item whose aspects the player knows from other scans is shown without being
scanned itself. The only caller is `GuiAspectAnalyzer`; when the API arrives the body of the stub is one line.

## 2. Jar identification (P3)

**Need.** The Funnel must (a) recognise a jar item, ordinary or void, filled or labelled, in its slot or in a hopper
offer; (b) recognise the jar block the hopper points at; (c) know whether the item jar is "filled" in the sense of 1.7.10
`ItemJarFilled` (holds essentia, or is empty but labelled, i.e. does not stack).

**What the add-on does today.** (a) `TileFunnel.isJarItem`: the item implements `IEssentiaContainerItem` and its registry
id is `thaumcraft:jar` or `thaumcraft:jar_void`; (b) `state.is(BlocksTC.jar()) || state.is(BlocksTC.voidJar())` and the
block entity as `IAspectContainer`; (c) contents through `IEssentiaContainerItem.getAspects`, label through
`getMaxStackSize() == 1`. This works but depends on two id strings and on a stack size that happens to encode the label.

**Proposed.** `BlocksTC.isJarItem(ItemStack)`, `BlocksTC.isVoidJar(ItemStack | BlockState)`,
`@Nullable Aspect BlocksTC.getJarLabel(ItemStack)` (backed by `ItemJar.isJar/isVoid/getFilter`).

## 3. Client sparkles (A8, P3)

**Need.** The Repairer shows a sparkle above the block every 10 ticks while it mends (`tcProxy.sparkle(x, y, z, 0)`), and
the Attractors a sparkle on every entity they move (colour 2 pulling, 4 pushing).

**Workaround.** Vanilla particles: `END_ROD` for the Repairer, a coloured `ENTITY_EFFECT` (blue 0xFF3F7FFF / red
0xFFFF3F3F) for the Attractors. Not the same look.

**Proposed.** `thaumcraft.api.client.ThaumcraftClientFX.sparkle(Level, x, y, z, int colorIndex)` as in A8.

## 4. Drawing an aspect tag with its amount (A9, P3)

**Need.** `UtilsFX.drawTag` of the Aspectalyzer screen: the aspect icon tinted with the aspect colour and the amount in the
small TC font.

**Workaround.** `thaumic.tinkerer.client.gui.AspectIcons.drawTag(GuiGraphics, Font, x, y, Aspect, amount)` draws
`Aspect.getImage()` tinted with `Aspect.getColor()` at 16 by 16 pixels, and the amount in the vanilla font at half size in
the lower right corner. The digits do not look like Thaumcraft's.

**Proposed.** `thaumcraft.api.client.AspectRenderer.draw(GuiGraphics, Aspect, int x, int y, int amount, int bonus)`.

## 5. Loader hook: built-in item renderer for `mob_display` (to the lead, P1 for the recipe viewers)

**Need.** `mob_display` stands for a mob (the output of a necromancy recipe, shown in recipe viewers). 1.7.10 drew the
mob in place of the item with an `IItemRenderer`. In 1.21.1 that is a built-in item renderer, which has no Architectury
registry.

**What exists.** `thaumic.tinkerer.client.render.item.RenderMobDisplay.render(ItemStack, ItemDisplayContext, PoseStack,
MultiBufferSource, int light, int overlay)` draws the mob (cached per aspect row, cleared by `clearCache()`, which
`MachinesClient` calls when the client leaves a world).

**Needed from the lead.**
- Fabric: `BuiltinItemRendererRegistry.INSTANCE.register(item, (stack, mode, pose, buffers, light, overlay) ->
  RenderMobDisplay.render(stack, mode, pose, buffers, light, overlay))` for `TTRegistry.item("mob_display")`.
- NeoForge: `RegisterClientExtensionsEvent.registerItem(IClientItemExtensions { getCustomRenderer() ... }, item)` with a
  `BlockEntityWithoutLevelRenderer` whose `renderByItem` calls the same method.
- Switch `assets/thaumictinkerer/models/item/mob_display.json` from the placeholder (a flat generated item) to
  `{ "parent": "minecraft:builtin/entity" }` once the hook is in place. Until then the item shows its placeholder icon.

## 6. Drops of a killed mob (loader event, P2)

**Need.** The Cursed Spirit's Blade replaces the drops of the mob it kills with soul aspects (`LivingDropsEvent` in
1.7.10: `event.drops.clear()` and add).

**Workaround.** Architectury has no event that can edit the drops. The add-on notes the victim in
`EntityEvent.LIVING_DEATH`, refuses the item entities that appear at the victim's position in `EntityEvent.ADD` while the
note exists, and spawns the soul aspects at the end of the level tick (`TickEvent.SERVER_LEVEL_POST`). It works for every
item that the death spawns *at the victim*; a drop that another mod spawns elsewhere is not refused. Experience orbs are
not item entities and stay, as in 1.7.10 (`event.drops` held only items).

**Proposed.** A common event `LivingDrops { EventResult onDrops(LivingEntity victim, DamageSource source,
Collection<ItemEntity> drops) }` with the loader backends `LivingDropsEvent` (NeoForge) and a mixin into
`LivingEntity.dropAllDeathLoot` (Fabric); the Blade would then clear the collection and add its items.

## 7. Stable keys for Thaumcraft entities and the key sound (A7, P3)

**Need.** The Necromancy Tablet summons `thaumcraft:wisp`, `thaumcraft:firebat`, `thaumcraft:brainy_zombie` and recognises
`thaumcraft:giant_brainy_zombie`; the Attractor opens with the sound `thaumcraft:key`.

**Workaround.** The ids are strings in `EnumMobAspect` and `BlockMagnet` (`isType(entity, "thaumcraft:firebat")` compares
the registry id; the sound is `SoundEvent.createVariableRangeEvent`). Missing entities (a build without them) make the row
unusable but do not crash (`createEntity` returns null).

**Proposed.** `thaumcraft.api.entities.ThaumcraftEntities` and `thaumcraft.api.ThaumcraftSounds` constants as in A7.

## 8. Not requested, for the record

- **ComputerCraft / OpenComputers peripherals** of the Aspectalyzer and the Kinetic Attractor are not ported (the lead's
  decision list in `API_GAPS.md` 1.11). The methods exist as plain Java: `TileAspectAnalyzer.hasItemMethod /
  itemHasAspectsMethod / getAspectsMethod / getAspectsAmountsMethod`, `TileMagnet.isPulling / setPulling / getRedstone`,
  `TileMobMagnet.adult / setAdult`. A compat glue (CC:Tweaked) only has to call them.
- **Pedestals.** The tablet reads `BlocksTC.get("arcane_pedestal")` as a `net.minecraft.world.Container` (the block entity
  `TilePedestal` is a `WorldlyContainer`). If the pedestal ever stops being a `Container` the tablet finds nothing; a
  public `IPedestal`-like interface would make that explicit.
