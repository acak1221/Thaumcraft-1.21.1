# TileBlocks: requests to the Thaumcraft API and to the loader modules

Group TileBlocks: Animation Tablet, Remote Placer, Levitational Locomotive and Relay, Golem Connector, Transvector
Interface, Transvector Dislocator, Transvector Binder. The numbers `A5`, `A7`, `A8`, `A11` are those of
`docs/API_GAPS.md` section 2. Nothing below was done by reaching into `thaumcraft.common.*`: every item is a named stub or a
workaround with the loss written down. Priorities are the group's own opinion (P1 changes what the player sees, P3 only
hardens something that works today).

## 1. Golem view for the Golem Connector (A11, P3)

**Need.** The Golem Connector exposes a bound golem to computers (ComputerCraft `tt_golemconnector`): type, core,
accessories, health, position, home with facing, and the markers, readable and, for the home and the markers, writable.
Thaumcraft's golems have no API ("Golems had no API in 1.7.10 and have none now", `docs/api/PORTING.md`).

**Proposed signature.** In 1.21.1 types (this is exactly the shape the add-on already codes against,
`thaumic.tinkerer.common.core.golems.GolemAccess`):

```java
package thaumcraft.api.golems;

public interface GolemView {
    record Marker(BlockPos pos, ResourceKey<Level> dimension, byte side, byte color) {}   // colour -1 = any
    String materialName();        // "Clay", "Flesh", "Iron", "Stone", "Straw", "Tallow", "Thaumium", "Wood"; null if unnamed
    int core();                   // the animation core id 0..11, or -1 for a golem without a core
    String decoration();          // one character per accessory, as the original's 'decoration' field
    float health();
    Vec3 position();
    BlockPos home();
    int homeFacing();             // a Direction's 3D data value
    List<Marker> markers();
    void setHome(BlockPos home, int facing);          // the original setHomeArea(x, y, z, 35) and homeFacing
    void setMarkers(List<Marker> markers);
}

public final class GolemApi {
    @Nullable public static GolemView view(Entity entity);   // null for an entity that is no golem
}
```

**Backed by.** `EntityGolemBase` (`getGolemCore`, `getDecoration`, `getHomePosition`, `setHome`, `getMarkers`,
`setMarkers`) and `GolemMarker`; the entity class is `final` and not exported.

**Stub in the add-on.** `GolemAccessProvider.forEntity(Entity)` returns `UnavailableGolemAccess.INSTANCE`, which behaves
like a connector with no golem (the 1.7.10 answers: empty results, and the original's error messages for the writes). The
peripheral logic (`GolemConnectorMethods`, 15 methods) already runs on `GolemAccess`. When the API exists, the body of
`GolemAccessProvider.forEntity` is the one place to change (`GolemAccessProvider.setProvider` already takes a
`Function<Entity, GolemAccess>`).

**Until then.** The Golem Connector remembers its golem (the Binder's sneak-click works, the UUID is saved, the golem is
found every 100 ticks within thirty blocks) but every question about the golem answers "no golem".

## 2. Stable entity key for the golem (A7, P3)

**Need.** The Binder and the Golem Connector recognise a golem by `EntityGolemBase`.

**Workaround.** The entity type id `thaumcraft:golem` (`ItemConnector.isGolem`, one `ResourceLocation` constant). It works
today; request A7 (`thaumcraft.api.entities.ThaumcraftEntities` with `ResourceKey<EntityType<?>>`) only hardens it. If A11
is built, `GolemApi.view(entity) != null` replaces it.

## 3. Client sparkles for the Relay beam (A8, P3)

**Need.** `TileEntityRelay` draws the beam between two linked relays with `tcProxy.sparkle(x, y, z, type)` every tick on the
client, type 2 (blue) or 14 (white), on a loop over the line; the sparkle is a Thaumcraft particle with its own sprite,
drift and fade.

**Workaround.** The vanilla dust particle (`DustParticleOptions`) in a blue or a white, with the same loops and the
original's four-in-six chance. It does not twinkle or drift as the original's does.

**Proposed.** `thaumcraft.api.client.ThaumcraftClientFX.sparkle(Level, double x, double y, double z, int colorIndex)` as in A8.

## 4. Sound constants (A7, P3)

**Need.** The wand click sound `thaumcraft:tool` (volume 0.6, pitch 1) when a wand turns the Tablet, the Remote Placer or the
Dislocator.

**Workaround.** `DeviceSounds.WAND_TOOL = SoundEvent.createVariableRangeEvent(thaumcraft:tool)`; the sound is registered by
the mod (`assets/thaumcraft/sounds.json`). A constant in the API would remove the string.

## 5. Mirrors switch (A5): answered

`TileBlocks.mirrorsAllowed()` calls `ThaumcraftApi.isMirrorsAllowed()`, which the API has now. Nothing to request.

## 6. Requests to the loader modules (to the lead)

* **Fluids and energy through the Transvector Interface (P1 for pipe and cable users).** `TileTransvectorInterface`
  forwards items, aspects and essentia, but 1.21.1 has no common fluid or energy interface. The Interface needs, per
  loader, a capability or transfer-storage provider for its block that asks `TileTransvector.getTile()` (public, returns
  the bound block entity or null) for the bound block's own capability and returns it:
  * Fabric: Fabric Transfer API `FluidStorage.SIDED` and `ItemStorage.SIDED` are not needed for items (the `WorldlyContainer`
    is found by hoppers and pipes already); fluids and, if wanted, Team Reborn Energy `EnergyStorage.SIDED`.
  * NeoForge: `Capabilities.FluidHandler.BLOCK`, `Capabilities.EnergyStorage.BLOCK`.
  The 1.7.10 Interface forwarded `IFluidHandler`, CoFH RF and IC2 EU.
* **Computer peripherals (P3).** The Tablet (`tt_animationTablet`), the Interface and the Golem Connector
  (`tt_golemconnector`) were ComputerCraft and OpenComputers peripherals. The plain-Java surface is there:
  `TileAnimationTablet.trigger/getRotation/setRotation/setLeftClick/setRedstone` (+ fields), and
  `TileGolemConnector.peripheral()` returning `GolemConnectorMethods` (`METHOD_NAMES`, `call(name, args...)`,
  `MethodException` with the original messages). A CC:Tweaked bridge on each loader would register them.
