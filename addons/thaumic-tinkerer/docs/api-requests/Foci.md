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
| burst of sparkles over a block | `ThaumcraftFX.blockSparkle(ServerLevel, BlockPos, rgb)` | Dislocation (replaces `burst`) |
| ingredients by the 1.7.10 field names | `ItemApi.getItem/getBlock` and `legacy_ids.json` (`itemShard:0/4`, `itemResource:1/6/10`, `blockCosmeticSolid:3`, `blockMirror:0`, `itemFocusPech/Fire/Excavation/PortableHole`) | the recipes |

## 1. Client particles of the foci (F1 = A8, P3)

**Need.** 1.7.10 spawned, from the focus code on the client, `Thaumcraft.proxy.sparkle(x, y, z, type)` (Mending around the player,
Telekinesis on every pulled item, Distortion ×6 on a shifted projectile), `smokeSpiral(world, x, y, z, radius, angle, y, rgb)` (Uprising),
`wispFX2(world, x, y, z, size, type, shrink, gravity, gravityValue)` (Efreet's Flame) and `beamCont(world, player, x, y, z, 2, 0xFF0000, ...)`
(Efreet's Flame, a continuous beam from the wand).

**What the add-on does today.** `FociFx` adds vanilla particles on the client with `Level.addParticle`: coloured dust and `END_ROD` for
the sparkles, dust on a circle for the spiral, `FLAME` that rises for the wisps, red dust along the line for the beam. It works and
is cheap, but it is not the look of the original.

**Proposed.** `thaumcraft.api.client.ThaumcraftClientFX` (client only), signatures in 1.21.1 types:
`void sparkle(Level level, double x, double y, double z, int colorIndex)`, `void smokeSpiral(Level level, double x, double y, double z, float radius, int angle, int minY, int rgb)`,
`void wisp(Level level, double x, double y, double z, float size, int type, boolean shrink, boolean gravity, float gravityValue)`,
`void beam(Level level, Entity source, Vec3 target, int rgb, boolean continuous)`. Backing: `client/fx/ParticleEngine`, `Tc4Particle`
and the beam renderer of the mod.

## 2. The dial of the wand (F2, P3)

**Need.** 1.7.10 `HUDHandler` put the readout of the Dislocation under the dial of the wand and moved it to the bottom of the screen
with `Config.dialBottom` (the client setting of the Thaumcraft HUD).

**What the add-on does today.** `DislocationHUDHandler` always draws under the dial at the top-left (y = 50). A player who moved the
dial to the bottom finds the readout above the hotbar corner where nothing else is, which is harmless but not where 1.7.10 put it.

**Proposed.** `ThaumcraftApi.isWandDialAtBottom()` (client side, `false` on a server), backed by `thaumcraft.common.config.Config.dialBottom`.

## 3. The server's "flying is not enabled" check (F3, P1 for servers, loader module)

**Need.** The Focus of Uprising launches the player along his sight on every cast, and 1.7.10 reset
`playerNetServerHandler.floatingTickCount` on each cast so that the server never counted the player as floating. In 1.21.1 the counter is
the private `ServerGamePacketListenerImpl.aboveGroundTickCount` (and `clientIsFloating` decides whether it counts); a player who keeps
climbing for more than four seconds on a server with `allow-flight=false` is disconnected.

**What the add-on does today.** `FociHooks.floatingTickReset` (`Consumer<ServerPlayer>`) is called on every server-side cast and does
nothing but log one warning until a loader module assigns it. Common code cannot do more: there is no Architectury accessor for the
field.

**Proposed (to the lead, in the loader modules, not in the API).** An accessor mixin per loader:

```java
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerAccessor {
    @Accessor("aboveGroundTickCount") void tt$setAboveGroundTickCount(int ticks);
}
```

and in `ThaumicTinkererFabric.onInitialize()` (and later the NeoForge entry point):
`FociHooks.setFloatingTickReset(player -> ((ServerGamePacketListenerAccessor) player.connection).tt$setAboveGroundTickCount(0));`.
The same accessor serves the awakened armour of KamiGear if it ever needs it. (Alternatively the mod could expose a
`ThaumcraftApiHelper.resetFlightKick(ServerPlayer)` for every add-on that moves players by impulse.)

## 4. What the wand's ray hit (F4, P3, documentation)

`ItemFocusBasic.onFocusRightClick` has no hit result (1.7.10 passed the wand's `MovingObjectPosition`), so each add-on focus makes its own
ray (`ItemModFocus.trace`). The API documents neither the range nor whether fluids count for Thaumcraft's own foci. If the guide stated
them (the wand's `IWandable` ray is `blockInteractionRange` with fluids), add-on foci could agree with the native ones; or an overload
`onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand, @Nullable BlockHitResult hit)` could hand over the
wand's own hit. The add-on uses `blockInteractionRange()` with fluids for the Dislocation and a fixed ten blocks through fluids and
grass for Efreet's Flame (the original `BlockUtils.getTargetBlock`).
