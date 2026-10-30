# Foci: requests to the Thaumcraft API and to the loader modules

Group Foci: the seven ordinary wand foci (Distortion, Dislocation, Ender Rift, Uprising, Mending, Efreet's Flame, Telekinesis),
`ProjectileHelper` and the Dislocation's readout. The numbers `A1`..`A13` are those of `docs/API_GAPS.md` section 2. Nothing in the
add-on reaches into `thaumcraft.common.*` or `thaumcraft.client.*` (the test `TinkererApiOnlyTest` scans for it): each item below
is either answered by the API or a named stand-in with the loss written down. Priorities are the group's own opinion.

## 0. What the API already answers (checked in the source of 2026-10-28, used by the group)

| Need | Answer in the API | Used in |
|---|---|---|
| `ItemFocusBasic` hooks, units in centivis, `getOrnament`/`getFocusDepthLayerIcon` as `ResourceLocation`, animated depth layers | `thaumcraft.api.wands.ItemFocusBasic`, `PrimalWandFocusRenderer` reads `.png.mcmeta` | all seven foci (Mending's depth layer is the 8x72 strip) |
| vis payment, frugal, caps, gear; potency and enlarge | `WandHelper.consumeAllVis`, `getConsumptionModifier`, `getFocusPotency`, `getFocusEnlarge` | every focus |
| the mounted focus, its data components | `WandHelper.getFocusItem` (a copy), `WandHelper.setFocus` (keeps the whole stack) | Dislocation's load |
| A5 `isMirrorsAllowed()` | `ThaumcraftApi.isMirrorsAllowed()` | the research of Distortion, Dislocation, Ender Rift, Mending and the recipe of the Ender Rift |
| portable-hole blacklist | block tag `#thaumcraft:portable_hole_blacklist` | Dislocation |
| sounds | `ThaumcraftSoundIds.WAND`, `WIND` | all |
| the client particles of the foci (F1, answered) | `thaumcraft.api.client.ThaumcraftClientFX.sparkle/smokeSpiral/wisp/beam` and the server twin `ThaumcraftFX.burst` | Mending, Telekinesis, Distortion (sparkle), Uprising (smokeSpiral), Efreet's Flame (wisp, beam), Dislocation (burst x8) |
| ingredients by the 1.7.10 field names | `ItemApi.getItem/getBlock` and `legacy_ids.json` (`itemShard:0/4`, `itemResource:1/6/10`, `blockCosmeticSolid:3`, `blockMirror:0`, `itemFocusPech/Fire/Excavation/PortableHole`) | the recipes |

## 1. Client particles of the foci (F1 = A8, P3): answered, nothing is requested

The first version of the group drew vanilla stand-ins because the API had no client FX facade. `thaumcraft.api.client.ThaumcraftClientFX`
now has the twins of the four proxy calls the originals made (`sparkle(level, x, y, z, type)`, `smokeSpiral(level, x, y, z, radius, start,
minY, rgb)`, `wisp(level, x, y, z, size, type, shrink, noClip, gravity)`, `beam(level, player, to, rgb, type, reverse, endScale, impact)`)
and `ThaumcraftFX.burst` is the server twin of the Dislocation's `burst`. `FociFx` calls them with the arguments of 1.7.10 (listed in
`docs/deviations/Foci.md`). Open for the lead: nothing; unverified: how they look in a client.

## 2. The dial of the wand (F2, P3)

**Need.** 1.7.10 `HUDHandler` put the readout of the Dislocation under the dial of the wand and moved it to the bottom of the screen
with `Config.dialBottom` (the client setting of the Thaumcraft HUD).

**What the add-on does today.** `DislocationHUDHandler` always draws under the dial at the top-left (y = 50). A player who moved the
dial to the bottom finds the readout above the hotbar corner where nothing else is, which is harmless but not where 1.7.10 put it.

**Proposed.** `ThaumcraftApi.isWandDialAtBottom()` (client side, `false` on a server), backed by `thaumcraft.common.config.Config.dialBottom`.

## 3. The server's "flying is not enabled" check (F3, answered by the loader layer; NeoForge mixin still to write)

**Need.** The Focus of Uprising launches the player along his sight on every cast, and 1.7.10 reset
`playerNetServerHandler.floatingTickCount` on each cast so that the server never counted the player as floating. In 1.21.1 the counter is
the private int `ServerGamePacketListenerImpl.aboveGroundTickCount` (and `clientIsFloating` decides whether it counts); a player who keeps
climbing for more than four seconds on a server with `allow-flight=false` is disconnected ("Flying is not enabled on this server").

**Answered by the lead's `FloatingTicks`** (`thaumic.tinkerer.common.hooks.FloatingTicks`, `docs/HOOKS.md` section 9), not by the
proposed `ThaumcraftApiHelper.resetFlightKick`. `Foci.register()` installs `FociHooks.setFloatingTickReset(FloatingTicks.RESET)`; the
reflection stop-gap of the first version (`FociHooks` with the two field names, `canReachCounter`, `aboveGroundTicks`,
`setAboveGroundTicks`) is deleted, so there is one path. *What it gives:* `get`, `set`, `reset`, `isAvailable` on the connection of a
`ServerPlayer` through an interface that the Fabric mixin `ServerGamePacketListenerFloatingMixin` adds to the connection class (no reflection,
no dependence on the mod's own accessor); a player without a connection (a player just created, or a fake player without one) gives `-1`/`false` and
`reset` does nothing. *What it does not give:* anything on NeoForge, where **the same mixin is still to be written**; until then the
reset silently does nothing there (no warning: the old warning for an unreachable field went with the reflection). The heads-up about
`thaumcraft.common.lib.utils.Utils.resetFloatCounter` (it writes `tickCount`, not `aboveGroundTickCount`) still holds for anyone who
thinks of using it. Verified by GameTests on the real connection of a mock player (`flightPutsTheKickCounterOfTheConnectionBackToZero`,
`flightResetOfTheStartUpClearsTheRealCounterOfTheCaster`); the kick itself (80 ticks of a floating player on a server without
`allow-flight`) was not run.

## 4. What the wand's ray hit (F4, P3, documentation)

`ItemFocusBasic.onFocusRightClick` has no hit result (1.7.10 passed the wand's `MovingObjectPosition`), so each add-on focus makes its own
ray (`ItemModFocus.trace`). The API documents neither the range nor whether fluids count for Thaumcraft's own foci. If the guide stated
them (the wand's `IWandable` ray is `blockInteractionRange` with fluids), add-on foci could agree with the native ones; or an overload
`onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand, @Nullable BlockHitResult hit)` could hand over the
wand's own hit. The add-on uses `blockInteractionRange()` with fluids for the Dislocation and a fixed ten blocks through fluids and
grass for Efreet's Flame (the original `BlockUtils.getTargetBlock`).
