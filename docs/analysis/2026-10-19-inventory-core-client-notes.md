# Fill / Empty client integration — 2026-10-19

Implemented source changes only in `GuiGolem.java`, `RenderGolemBase.java`, and `ThaumcraftGolemClient.java`, plus this note. Root's current seven-material skins/frame selection, float bootup feed, cargo/damage/sway, `GOLEM_DATA.get()` and all-placer predicate loop were retained, not restored from the older handoff. No model, menu/runtime/items/data/registry, resources/lang, input/network/bootstrap, build inputs, tests or smoke files were edited. Data/Bell and runtime WIP were not read; no agents were spawned.

Authority: final `2026-10-19-golem-inventory-api.md` and `2026-10-19-golem-fill-empty.md`; original oracle `client/gui/GuiGolem.java:80–91,190–204,370–377` supplies caption and Fill toggle behavior.

## Client behavior / root contracts

- **Quantities:** Fill0 alone draws `ContainerGolem.getWantedCount(i)` for populated ghost rows0…5 using native `GuiGraphics.renderItemDecorations(..., customString)`. It passes the existing one-count exemplar unchanged: no oversized stack, `setCount`, synthetic cargo or quantity mutation. Empty1/Gather2 retain ordinary one-count whitelist rendering. All36 real inventory slots and all ghost interactions remain native/root-owned.
- **Caption/mode:** `menu.core()` selects existing translated `golemblurb.0/1/2.text` every frame. Fill alone shows its original8×8 control at `(62,54)`: base UV `(8,168)`, checked UV `(8,176)` when `isFillAnyAmount()==false` (**precise**); any-amount is unchecked. Original English mode labels and half-size placement are retained without language edits. Current server core/mode are reread for drawing/clicking; there is no stale widget or locally flipped mode.
- **Mode click:** left-click sends only `minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 50)`. Root must validate/apply button50 for Fill and sync its8 menu data fields; the client waits for that update. No new packet/channel or client-authoritative state was added.
- **Badges:** `ThaumcraftGolems.coreForType(entity.getGolemCore())` supplies the actual Fill0/Empty1/Gather2 item, or null for no supported fitted core. Only the paid fitted-core display stack is created; existing actual cargo snapshots/rendering are untouched. Root owns the new flat core item models/textures.

## Root Fabric LAST hook

New public entry point:

```java
public static void renderMarkers(PoseStack pose, MultiBufferSource buffers,
                                 float partialTick, Camera camera)
```

Root adds this to its existing Fabric `WorldRenderEvents.LAST` callback **before its final native buffer flush**:

```java
ThaumcraftGolemClient.renderMarkers(context.matrixStack(), buffers, partialTick, context.camera());
```

Pass the ordinary world-view pose without an extra camera translation. This method subtracts camera position from each marker position in double precision before native float vertices, and balances every pose push/pop. It does not flush buffers, register a world event/key handler, or directly mutate GL state. `partialTick` is intentionally unused for immutable block positions.

The implementation uses the agreed `common.items.golems.ItemGolemBell.selectedMarkers(ItemStack)` snapshot and `GolemMarker(ResourceLocation dimension, BlockPos pos, Direction face)` record. It draws only with a live local player and client world while a real bell is held. Main-hand bell wins, even if empty; otherwise offhand is used. Only current-dimension, already-loaded positions are shown (`hasChunkAt` status check only). No block/entity/inventory fetch, forced load, AI tick or world/item mutation occurs.

Native `LevelRenderer.renderLineBox` / `RenderType.lines()` draw a cyan unit-block outline and a thin gold outline just outside the clicked face, covering all six directions. These are bounded depth-tested coordinate outlines, **not** a claim of full original marker particles/through-wall FX, colors or upgrades. Unloaded positions are not inspected or rendered. Root remains responsible for the existing shared F item-action mapping, authoritative bell-reset packet, and tooltip semantics; no duplicate F binding or client reset handler was added here.

## Hashes and verification boundary

Paths below are under `common/src/main/java/thaumcraft/client/`; hashes are SHA-256 after these changes.

| File | SHA-256 |
| --- | --- |
| `gui/GuiGolem.java` | `8dc4daaa801de7d820dc1c6aa87b7468a1d7f0742908fcff032e6bb7b3e690a8` |
| `renderers/entity/RenderGolemBase.java` | `537a48b09dd38b312ba78445304ef71140b6ce9eedfb880acbb89b57196a8e85` |
| `ThaumcraftGolemClient.java` | `d4df60924cefc24bd6a32a621a229859a3169a56e5053ce7eed7a9fc387043c2` |

Source readback, original toggle flags/retained caption-key inspection, hashing and read-only cached native `renderLineBox` API metadata inspection were performed. No Gradle/compiler, Java/game launch, tests, smoke or world run occurred. These changes are **not yet compiled**; root owns the actual Fabric target build after all owner finals. Quantity layout, mode sync, badges, all face outlines and gameplay/input remain visually/runtime unverified. The earlier seven-material build is not evidence for these new inputs. Broader original Fill/Empty behavior (including the declared deferred world-throw branches) is not completed by this presentation work.
