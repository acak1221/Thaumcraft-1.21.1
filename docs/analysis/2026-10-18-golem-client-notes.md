# Straw/Gather client integration — 2026-10-18

**Implemented source only; native compilation and visuals remain unverified.** New client-owned files are frozen for root integration after this handoff:

- `common/src/main/java/thaumcraft/client/renderers/entity/RenderGolemBase.java`
- `common/src/main/java/thaumcraft/client/ThaumcraftGolemClient.java`
- `common/src/main/java/thaumcraft/client/gui/GuiGolem.java`
- This note.

The existing frozen `ModelGolem.java` was **not edited**. No bootstrap, common registration/menu/entity/items, resources/lang, build inputs, tests, Python, smoke tooling, saves or source pins were edited. Runtime/item-author WIP was not inspected; only agreed contracts, original sources, existing non-WIP patterns and cached native API bytecode were used. No agents, Gradle, compiler, Java/game launch, test, smoke or world operation was run. This is not completion/acceptance of the entire functional golem unit.

## Root hook and exact dependencies

After common registration, root adds its sole existing-bootstrap hook:

```java
ThaumcraftGolemClient.init();
```

The initializer is idempotent. It registers `ThaumcraftGolems.GOLEM` with `RenderGolemBase::new`, `GOLEM_MENU.get()` with `GuiGolem::new`, and the placer predicate **`thaumcraft:has_core`**. The predicate is `1` when the actual placer component's `core() >= 0`, otherwise `0`; missing component uses `GolemData.EMPTY`. Root's `golem_straw` item model can use predicate value1 to select `thaumcraft:item/golem_straw_core` with its retained `golem_over_core` overlay. No placeholder/future variants are registered.

Contracts used, without reading their WIP implementations:

- `thaumcraft.common.config.ThaumcraftGolems`: `GOLEM` is the entity supplier; `GOLEM_STRAW.get()` / `CORE_GATHER.get()` supply items; `GOLEM_MENU.get()` supplies the menu type. **`GOLEM_DATA` is used directly as the stated `DataComponentType<GolemData>`, not as a supplier.** If root instead chooses a registry-supplier field, that predicate callsite needs `.get()` after the concrete API is finalized.
- `thaumcraft.common.items.golems.GolemData`: `EMPTY` and `core()`; no reflection, persistence rewrite or component mutation.
- `thaumcraft.common.entities.golems.EntityGolemBase` extends native `PathfinderMob`; getters used are `getGolemCore()`, `getCarriedItem()` snapshot, `getBootupTimer()`, `getHealingTimer()`, `getActionTimer()`, plus native living/render state. No equipment slot is fabricated and no cargo stack is synthesized or modified.
- **Menu package assumption:** `thaumcraft.common.container.ContainerGolem`, following modern menu placement. It supplies `IMAGE_WIDTH=176`, `IMAGE_HEIGHT=166`, `GHOSTS=6`, and nullable `golem()` resolving the real client entity. Match that FQCN at root integration, or correct the GUI's single import if root retains the original entity-package menu location. Slots/interactions/sync remain entirely root-owned.

## Frozen model API reconciliation

The earlier four-setter draft mentioned in the integration request is not the frozen model's current API. The corrected model has `setActive`, `setCarrying`, `setBootupTimer`, `setHealingTimer`, `setCombatTimer`, and optional `setInactive`, as recorded in `2026-10-18-golem-model-notes.md`. This renderer calls **every actual setter for every model draw**, including coreless/zero/false values. `setCombatTimer` receives the real `getActionTimer()` countdown; it is not derived from normalized native attack progress. `setInactive(false)` is explicit because no runtime pause getter was agreed. Core−1 selects the frozen model's fully dormant pose; positive bootup is the original degree countdown.

The separate damage model is freshly baked with the same geometry, receives the same pose state and native preparation/setup parameters, but always receives healing timer0. Thus the original primary-skin healing tint cannot leak into damage pass2. Each renderer construction/reload bakes its own main/damage models using `ModelGolem.createBodyLayer().bakeRoot()`; no model-layer registration/bootstrap hook is needed.

**Bounded gap:** the frozen corrected model exposes no independent left/right arm-timer setters. Those entity counters therefore are not consumed here; period-20 independent-arm animation is not claimed implemented. No hidden workaround, invented runtime getter or frozen-model edit was added. The ordinary Gather head/walk/carry/shared-action path uses the existing frozen model.

## Original appearance and native adaptations

Authority remains active oracle `client/renderers/models/entities/ModelGolem`, selected by `ClientProxy:471` / `RenderGolemBase:60`. Original renderer anchors below are `../work/thaumcraft-4.2.3.5-oracle/thaumcraft/client/renderers/entity/RenderGolemBase.java`:

- **78, 82–89:** bind retained `thaumcraft:textures/models/golem_straw.png`, native `MobRenderer`, shadow radius0.25. The model's normal Y30 geometry and internal uniform0.4 scale are unchanged. Native name, hurt/death and ordinary living rendering remain available.
- **160–166:** original 6.5-degree period13 triangular Z sway is applied after native base rotations when current walk speed is at least0.01. Native `walkAnimation.position(partialTick)` was verified to equal `position - speed*(1-partialTick)`, matching the original phase before adding6.
- **97–115:** core2 gets its fitted Gather badge using the actual registered core item model/animated atlas, with original X180 rotation, translation `(0.0875,-0.96,0.15)`, scale0.175 and Y180 rotation. The ephemeral icon stack represents the already fitted core only; it never enters an inventory/world or becomes cargo. Native generated-item centering is compensated by `(0.5,0.5,-0.1)` and Z scale3.2, matching the old `[0,1]` sprite and0.2 extrusion. No coreless/future badge is invented.
- **170–244:** render the real carried snapshot through native `ItemRenderer.renderStatic` with the real entity/level/seed, preserving component overrides, tint layers, foil and existing builtin item routes. Native `NONE` display context avoids adding hand/inventory JSON transforms on top of the original explicit transforms. Cargo has its own original0.4 scale, separate from the mesh's balanced internal pose. A 3D block uses original `(0,2.5,-1.25)` / X180; jars use `(0,2.5,-1)`, then `(-0.5,0,-0.8)` and the original flat-item rotations; ordinary flat cargo uses `(-0.5,2.5,-1.25)` and those rotations. Native sprite centering is compensated. Jars are checked before modern `BlockItem`, because the modern jar unifies the legacy item/block identities. Original `ItemJarFilledRenderer:48–81` confirms the non-player jar draw uses raw block coordinates. Cargo is hidden during native `hurtTime != 0`, as in the original.
- **150–154:** retained `golem_damage.png` is drawn with native translucent buffering and alpha `1-health/maxHealth`, using the matching animated model and native hurt-overlay coordinates. Full health skips this overlay; no healing tint is applied to it.

**Foot-origin adaptation:** cached native `LivingEntityRenderer` applies the Y flip, then its scale hook, then translation−1.501. The original normal model's neutral foot minimum is at pixel61, hence `61 * 0.4 / 16 = 1.525`. The scale hook adds only model-space Y `1.501-1.525 = -0.024`, lifting the neutral mesh by0.024 world blocks to analytically align feet with the entity origin. It does **not** apply another0.4 scale, alter boxes/UVs, change root's0.4×0.95 hitbox, or move the actual entity. Native foot clipping/walking contact remains visually unverified.

Bounded native item adaptation: `BakedModel.isGui3d()` replaces legacy RenderBlocks/Forge helper classification. Native item rendering owns all component/model tint passes; the obsolete `requiresMultipleRenderPasses` special rotation branch is not reproduced, and ordinary flat-item orientation is used for modern layered items. Other materials, advanced/decorations/upgrades, bucket/fishing geometry, independent-arm timers and their parity remain out of scope, not placeholder-complete.

## Gather screen

Original `GuiGolem:80–91,117–166,267–304` supplies caption, background, straw frames and real-entity preview intent. The new native screen draws retained 256×256 `textures/gui/guigolem.png` at176×166, with six24×24 straw frames sourced from `(184,0)` at `(96+i/2*28,12+i%2*31)`. These align with root ghost slots at `(100+i/2*28,16+i%2*31)` and its existing inventory/hotbar coordinates. Native container interactions and tooltips are untouched; no client filter edits or extra packets exist.

The actual existing `golemblurb.2.text` key is rendered with the original half-size caption placement/width; both native en_us and ru_ru resources already contain it. There are no fuzzy/color/upgrades/toggle controls or unsupported quotes. The native `InventoryScreen.renderEntityInInventoryFollowsMouse` helper renders only `menu.golem()` when resolved/alive, in the left-side preview region. Cached bytecode confirms it saves/restores body/head rotations and scissoring; it does not advance AI/ticks or set NoAI. There is no synthetic entity, replacement renderer or static model placeholder when resolution is pending. The original custom projection/viewport is replaced by this bounded native preview.

## SHA-256 / remaining verification

| File/input | SHA-256 |
| --- | --- |
| New `RenderGolemBase.java` | `8b59dee60996c4b66eed84b33fb5b6455e4544e7c9597947e95a68cca19ad3f4` |
| New `ThaumcraftGolemClient.java` | `f9afc8b63be4d52adbf8f20482eb8b80cf17bb3e6ce1ec244767b76c4fa5c8c7` |
| New `GuiGolem.java` | `0c1f3719b42247b908242ea3d4e1e76f524e1b940c4d1b22527667a7e73186de` |
| Unchanged frozen `ModelGolem.java` | `86eb173e7281ed7cb93511a91c82e76052c22ce7935c293e4ca9e0ed57023755` |
| Original oracle renderer | `57335bc2378c0517c79b42b66de36b54f8eb085807da773be57e6fd6598adb3f` |
| Original oracle GUI | `2854729ebda478e3c9c23ff5637ddd586f289db0e7d8349932c45f9be25e6480` |
| Retained straw PNG / original entry | `22061ca098038af515241fbfe69d17178b41cecd4c8ac526c26820b2ab45273a` |
| Retained damage PNG / original entry | `86e3d81fd092b69f49228822f6c59d43cf4402a43decfe4bf7684273c5c8a57f` |
| Retained GUI PNG / original entry | `42b28f98ead0bd6c8838ffb9672aa712fc4c0cb32e86c0d5dd89304530c487b9` |

Original-entry hash comparisons and source/API readback are not compile/runtime evidence. Root owns actual Fabric compilation once runtime/items/menu/resources are integrated. World rendering, core/cargo orientation, damage blending, foot contact, preview/layout, ghost interactions and configured-placer display remain unseen/unverified; final gameplay checks are deferred, not passed. No further client-owned edits after this handoff unless root supplies a concrete integration/API correction.
