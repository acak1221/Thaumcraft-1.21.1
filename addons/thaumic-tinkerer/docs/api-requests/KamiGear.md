# KamiGear: requests to the Thaumcraft API and to the loader modules

Group KamiGear: Ichor resources, the Ichorcloth wand rod and cap, the eight Ichorium tools, the eight Ichorcloth armour
pieces, the Feline Amulet, the Bottomless Pouch, the Black Hole Ring, the Worldshaper's Looking Glass (Placement Mirror),
the Protoclay, the Celestial Pearl and the Foci of Shadowbeam, Experience Drain and Celestial Recall. The numbers `A1`..`A13`
are those of `docs/API_GAPS.md` section 2. Nothing in the add-on reaches into `thaumcraft.common.*` (the test
`TinkererApiOnlyTest` and a scan of every KamiGear source checked it): each item below is either already answered by the API
or a named stub / workaround with the loss written down. Priorities are the group's own opinion (P1 changes what the player
sees, P3 only hardens something that works today).

## 0. Requests that the API now answers (verified in the source of 2026-10-28, used by the group)

| Request | Answer in the API | Used in |
|---|---|---|
| A1 `IBauble.equipsOnUse(ItemStack, Player)` | `thaumcraft.api.baubles.IBauble.equipsOnUse` | `ItemBlockTalisman` (a sneak-click toggles the ring) and `ItemIchorPouch` (a click opens it) answer `false` |
| A2 `IFocusPouch { getInventory, setInventory }` | `thaumcraft.api.wands.IFocusPouch` (128 cells at most) | `ItemIchorPouch implements IFocusPouch` (117 cells, `ItemContainerContents` component); `SlotNoPouches.isPouch` tests `instanceof IFocusPouch`, so Thaumcraft's own pouch is refused as in 1.7.10 |
| A5 `isMirrorsAllowed()` | `ThaumcraftApi.isMirrorsAllowed()` | `KamiGear.ready()` queues the research of the Focus of Celestial Recall only while it is on (1.7.10 `getResearchItem` returned null otherwise) |
| A6 `isWoodLog` / `breakFurthestBlock` | `ThaumcraftApiHelper.isWoodLog(Level, BlockPos)`, `breakFurthestBlock(Level, BlockPos, Block, Player)` | `ToolHandler.fellTree`, the tree mode of the Awakened Ichorium Axe, is the 1.7.10 loop over these two |

Consequences of the API's own rules, kept as they are: `isWoodLog` is false for the plain oak log (the original's rule, so tree
mode does not fell oak trees), `breakFurthestBlock` wears no tool and asks neither protection nor loader break events (see
`docs/deviations/KamiGear.md` section 5).

## 1. Armour absorption (K1, new, P3)

**Need.** 1.7.10 `ItemIchorclothArmor implements ISpecialArmor`: every piece absorbs `0.0425 x armour points` of every blow
(ratio 0, maximum absorption unlimited), the four pieces 85% together, and `damageArmor` is a no-op (the pieces never wear).

**What the add-on does today.** The armour materials give 3/8/6/3 armour points and a toughness of 5 per piece (20 for the
set, the largest value the attribute takes); the pieces have no durability. That is strong, but the 1.21.1 formula cannot reach
85% flat: against a blow of 10 the set absorbs about 74%, against a blow of 40 about 57%.

**Proposed.** `thaumcraft.api.armor.ISpecialArmor`-like interface, for example
`interface IArmorAbsorption { double getAbsorptionRatio(LivingEntity wearer, ItemStack armor, EquipmentSlot slot, DamageSource source, float amount); }`,
applied by the mod in one place before the vanilla armour formula (a hook on `LivingEntity.getDamageAfterArmorAbsorb`), with
the original rule that damage which ignores armour (`DamageTypeTags.BYPASSES_ARMOR`) is not reduced. Architectury has no
event that changes a damage amount (`EntityEvent.LIVING_HURT` can only cancel), so this cannot be done from the add-on in
common code.

## 2. Stable keys for Thaumcraft's sounds (K2 = A7, P3)

**Need.** `thaumcraft:wand` (the sounds of the Placement Mirror and of the gateway teleport), by the name `wand`.

**What the add-on does today.** `KamiHooks.thaumcraftSound(path)` looks up `thaumcraft:<path>` in `BuiltInRegistries.SOUND_EVENT`
and plays nothing when it is missing. Works; a constant in the API would harden it.

## 3. Client sparkles for the Shadowbeam (K3 = A8, P3)

**Need.** 1.7.10 spawned a dark `FXSparkle` at every step of the beam on the shooter's side.

**What the add-on does today.** The server sends `PacketShadowBeam` (the first visible point, every bounce and the end, at
most 96 points) to the players around, and `ShadowBeamClientHandler` draws vanilla dust particles along that path. Not the
same look. Proposed: `thaumcraft.api.client.ThaumcraftClientFX.sparkle(Level, x, y, z, int colorIndex)` (A8).

## 4. Loader hooks (to the lead)

Architectury has no event for these two, so the add-on provides the code and the Fabric entrypoint class
`thaumic.tinkerer.fabric.client.KamiGearFabricClient` registers them. NeoForge needs the same two hooks (deferred):

- **Player render layer.** `thaumic.tinkerer.client.render.kami.WingsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>, EntityModelSet)`
  must be added to every player renderer (Fabric: `LivingEntityFeatureRendererRegistrationCallback`; NeoForge:
  `EntityRenderersEvent.AddLayers` for the `default` and `slim` skins). The model layer `ModelWings.LAYER` is registered by
  `KamiGearClient` through Architectury.
- **After-world render.** `PlacementMirrorPredictionRenderer.render(PoseStack, MultiBufferSource.BufferSource, Camera)` must be called
  after the translucent terrain when `ConfigHandler.showPlacementMirrorBlocks` is on (Fabric: `WorldRenderEvents.AFTER_TRANSLUCENT`;
  NeoForge: `RenderLevelStageEvent` at `AFTER_TRANSLUCENT_BLOCKS`).

## 5. Contracts with the KamiWorld group (not API: registry paths and one class of hooks)

KamiGear does not reference the KamiWorld classes; it reaches them like this (all in `thaumic.tinkerer.common.item.kami.KamiHooks`):

- **Soul Hearts.** `KamiHooks.soulHeartSink` is a `Consumer<ServerPlayer>` that the Soul Mode of the Awakened Sword calls (1.7.10
  `SoulHeartHandler.addHearts`). KamiWorld assigns it from its `register()`/`ready()`. Until then a warning is logged once and
  the mode gives no hearts.
- **Celestial Gateway.** The Focus of Celestial Recall calls `KamiHooks.warpGateTeleporter` (1.7.10 `TileWarpGate.teleportPlayer`). The
  default reads the saved data of the block entity at the destination: the block `thaumictinkerer:warp_gate` (`LibBlockNames.WARP_GATE`)
  and a boolean `locked` in its saved tag. KamiWorld may replace the hook with its own method.
- **Bedrock dimension.** `thaumictinkerer:bedrock` (`KamiHooks.BEDROCK_DIMENSION`) and the portal block `thaumictinkerer:bedrock_portal`
  (`LibBlockNames.PORTAL`). The Awakened Pickaxe turns bedrock into the portal near the Overworld's floor and at the top of the
  Bedrock dimension, and into air elsewhere in the Bedrock dimension; with no portal block registered it does nothing to bedrock.
  The left-click handler of 1.7.10 (`KamiDimensionHandler.onInteract`, which calls the pickaxe's `onBlockStartBreak` on bedrock) is
  **already ported** in `ItemIchorPickAdv.onLeftClickBlock` and registered by `KamiGear`; KamiWorld must not register it again.
- **Nitor gas.** The Leggings of the Burning Mantle place `thaumictinkerer:nitor_gas`; the strong light is the integer state property
  `level` = 1 (found by name; `bright`, `meta` and `variant` are accepted too).
- **Sky Pearl.** `ItemSkyPearl.setValues/isAttuned/getPos/getDim/addInfo` are the static methods that the gateway block and its screens
  call; the pearl's recipe is registered under the research `WARP_GATE`, which KamiWorld registers.
- **ICHOR research.** `ResearchHelper.kamiResearch` of 1.7.10 is `KamiGear.ichorResearch()` (null until `ready()` and with Kami off).
