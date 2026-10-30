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
| K1 armour absorption (`ISpecialArmor`) | not the API: the add-on's loader event `TinkererEvents.LIVING_ARMOR_ABSORB` (section 1) | `IchorAbsorption`, registered by `KamiArmorHandler.register()` |

Consequences of the API's own rules, kept as they are: `isWoodLog` is false for the plain oak log (the original's rule, so tree
mode does not fell oak trees), `breakFurthestBlock` wears no tool and asks neither protection nor loader break events (see
`docs/deviations/KamiGear.md` section 5).

## 1. Armour absorption (K1, answered by a loader event of the add-on)

**Need.** 1.7.10 `ItemIchorclothArmor implements ISpecialArmor`: every piece absorbs `0.0425 x armour points` of every blow
(ratio 0, maximum absorption unlimited), the four pieces 85% together, and `damageArmor` is a no-op (the pieces never wear).

**Answer.** Not an API interface of Thaumcraft but a loader event of the add-on, `TinkererEvents.LIVING_ARMOR_ABSORB`
(`thaumic.tinkerer.common.hooks`), posted from `LivingEntity.getDamageAfterArmorAbsorb` (Fabric: `LivingEntityArmorMixin`). The first proposal
of this request (an `IArmorAbsorption` interface that the mod would apply in one place) is therefore **not needed**; the Thaumcraft mod itself was not touched.
The full description of the event is in `docs/deviations/KamiGear.md` section 13 (text for `docs/HOOKS.md`).

*What the event gives*: the listener is called for every armour step of every living entity (a source of the tag `bypasses_armor` excepted), with the damage
that comes to the step and the amount vanilla's formula takes off, and **replaces that amount** (held between 0 and the blow). That is the power of
`ArmorProperties.ApplyArmor` for a priority-0 piece: `IchorAbsorption` returns `damage x (0.0425 x points of the worn pieces)` plus the formula's share of the
armour that is not Ichorcloth, so the full set takes 85% flat and a mixed set keeps the other armour's worth. Wear costs nothing because the pieces have no durability.
The armour points of the materials (3/8/6/3, the original's `getArmorDisplay`) stay as the display of the HUD and the tooltip and do not stack with it; the stand-in toughness is removed.

*What the event does not give*: it is not `ISpecialArmor`. There is no per-piece `getProperties`/`damageArmor` call, no priority levels of its own (the one Thaumcraft pass of
priority 1, the wards, runs before the event by the order of the two mixins), no `AbsorbMax`, and no hook on the wear of other armour. The mixed-set share is the 1.21.1 formula's, not
the original's 4% a point. A hit that bypasses armour is not posted (the original's `getProperties` had no such look; whether Forge applied it to those hits could not be checked).
On NeoForge the equivalent is `LivingIncomingDamageEvent#addReductionModifier(Reduction.ARMOR, ...)`; the poster is still to be written (`docs/deviations/KamiGear.md` section 13).

## 2. Stable keys for Thaumcraft's sounds (K2 = A7, P3)

**Need.** `thaumcraft:wand` (the sounds of the Placement Mirror and of the gateway teleport), by the name `wand`.

**What the add-on does today.** `KamiHooks.thaumcraftSound(path)` looks up `thaumcraft:<path>` in `BuiltInRegistries.SOUND_EVENT`
and plays nothing when it is missing. Works; a constant in the API would harden it.

## 3. Client sparkles for the Shadowbeam (K3, P3; the generic sparkle is A8, now in the API)

**Need.** 1.7.10 spawned a custom black `FXSparkle` subclass (scale `size`, colour 0.001, five ticks to live, no shrinking, no
slow-down) at every step of the beam on the shooter's side.

**What the add-on does today.** The server sends `PacketShadowBeam` (the first visible point, every bounce and the end, at
most 96 points) to the players around, and `ShadowBeamClientHandler` draws vanilla dust particles along that path. Not the
same look. A8 has arrived in the API (`ThaumcraftClientFX.sparkle`, `ThaumcraftFX.sparkle`) and the add-on uses the server twin for the
white sparkles of the Celestial Recall and of the Robes' deflection; it does **not** use it for the beam, because the beam is hundreds of
steps per tick (a packet each) and the API's sparkles shrink away and fall, unlike the original particle. Proposed if wanted: a
`ThaumcraftClientFX.sparkle` variant with a fixed life, no shrink, no slow-down and a custom colour.

## 4. Loader hooks (to the lead)

Architectury has no event for these two, so the add-on provides the code and the Fabric class
`thaumic.tinkerer.fabric.client.KamiGearFabricClient` registers them. `KamiGearClient.register()` (common) starts that class by
name on Fabric, so no change of `fabric.mod.json` is needed (the class implements `ClientModInitializer` and registers only once, so
listing it as a `client` entrypoint later is harmless). NeoForge needs the same two hooks (deferred):

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

## 6. Requests to the lead and to other groups (shared files; nothing here was edited by the KamiGear group)

- **`LIVING_JUMP` order: Gem Boots run after Ascent Boost (the reverse of 1.7.10).** Both listeners register in the `register()` phase and `ContentGroups.ALL`
  has Enchantments before KamiGear, so a Gem Boots wearer with Ascent Boost leggings jumps `0.42 x factor + 0.3` (0.93 for Ascent Boost I, 1.35 for III) where 1.7.10
  gave `(0.42 + 0.3) x factor` (1.08, 1.80): the boots' handler was registered in `preInit`, `ModEnchantmentHandler` in `init`, both at normal priority.
  The cheapest fix matches the original's phases: `Enchantments.register()` calls `ModEnchantments.initEnchantments()` (it only registers the event handler)
  from `Enchantments.ready()` instead. Alternatives: KamiGear before Enchantments in `ContentGroups.ALL`, or a priority on the `TinkererEvents` listeners. The GameTest
  `KamiGearRuntimeChecks.gemBootsAndAscentBoostLeggingsComposeTheirJumps` pins today's 0.93 / 1.35 and says in its comment what to change (the two numbers) after the fix.
  Details: `docs/deviations/KamiGear.md` section 3.
- **Documents of the lead that this group's `LIVING_ARMOR_ABSORB` makes false** (not edited here): `docs/DEVIATIONS.md` section 4, the bullet "Armour absorption of Kami
  armour (`ISpecialArmor`): needs a hook on `getDamageAfterArmorAbsorb`; not written"; `docs/API_GAPS.md` line 82 ("`ISpecialArmor` has no counterpart (3a)") and line 239
  ("`ISpecialArmor` ... becomes `UNBREAKABLE` + attribute modifiers or a hurt event"); `docs/HOOKS.md` section 3, the sentence "It still needs a hook on
  `getDamageAfterArmorAbsorb`. Not written." (lines 92-93) and the sentence of section 10 "The armour absorption K1 needs a hook of its own" (line 282). The text for the new
  row of the section 1 table and for the NeoForge notes is in `docs/deviations/KamiGear.md` section 13.
- **KamiWorld, `common/content/KamiWorld.java` line 93: the same S2C registration blocker.** `NetworkManager.registerS2CPayloadType(PacketSoulHearts.TYPE, ...)`
  runs on the client as well, and `KamiWorldClient` registers the receiver for the same type, which makes Fabric's
  `PayloadTypeRegistry.playS2C()` throw `IllegalArgumentException: Packet type ... is already registered!` on every client start. Fix as in
  `KamiGear.registerNetwork()`: guard the call with `if (Platform.getEnvironment() == Env.SERVER)` (`dev.architectury.platform.Platform`,
  `dev.architectury.utils.Env`). Architectury's own documentation: the receiver registers the type on the client, `registerS2CPayloadType` is for the server.
- **KamiWorld, `TileWarpGate.teleportPlayer`: the 20 + 20 white sparkles of 1.7.10 are missing.** Call `KamiHooks.sparklesAround(serverLevel, player, 20)`
  before the jump (after the first sound) and after it (before the second), as `KamiHooks.teleportToWarpGate` does.
- **Lead, `client/lib/LibResources.java` lines 59 to 62.** `MODEL_ARMOR_ICHOR_1`, `_2`, `MODEL_ARMOR_ICHOR_GEM_1`, `_2` name files that do not exist
  (`ichor1.png`...). They are unused; delete them, or point them at `textures/models/armor/ichor_layer_1.png`, `ichor_layer_2.png`,
  `ichor_gem_layer_1.png`, `ichor_gem_layer_2.png`.
- **Lead, `fabric/src/main/resources/fabric.mod.json`: optional.** The Kami client glue is started by name from `KamiGearClient`; listing
  `thaumic.tinkerer.fabric.client.KamiGearFabricClient` as a `client` entrypoint is not needed and harmless.
