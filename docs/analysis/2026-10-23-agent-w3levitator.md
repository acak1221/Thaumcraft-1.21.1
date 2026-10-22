# The item of the Arcane Levitator — the inventory art of the original (agent `w3levitator`, 2026-10-23)

## The question

The test `LevitatorAssetsTest.theBlockHasItsStateModelsTexturesAndNames` failed with

```
expected: <thaumcraft:block/levitator> but was: <minecraft:block/block>
```

because it asserted that the item model of the levitator is a plain parent reference to the
block model, while the port ships a model of its own: the cube plus the two `animatedglow`
elements with the tint indices 0 and 1
(`common/src/main/resources/assets/thaumcraft/models/item/levitator.json`).

Two readings were possible:

1. the item of the original was the plain cube (the glow was drawn by the **world renderer
   only**) — then the port's item model had to become `{"parent": "thaumcraft:block/levitator"}`
   and the glow had to live only in the world renderer;
2. the item of the original also showed the glowing panels — then the port's model is right and
   the **expectation of the test** is wrong.

## The verdict: reading 2 — the item of the original was the inventory art of the renderer

### The oracle

`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/thaumcraft/client/renderers/block/BlockLifterRenderer.java:32-53`
— `renderInventoryBlock`, the inventory art of the block:

```java
public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {
    block.func_149676_a(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);          // the cube
    renderer.func_147775_a(block);
    BlockLifterRenderer.drawFaces(renderer, block, iconBottom, iconTop, iconSide, iconSide, iconSide, iconSide, false);
    Color c = new Color(BlockCustomOreItem.colors[4]);                  // 0x00A000
    ...
    block.func_149676_a(0.01f, 0.9f, 0.01f, 0.99f, 0.99f, 0.99f);        // the upper emissive slab
    renderer.func_147775_a(block);
    BlockLifterRenderer.drawFaces(renderer, block, ((BlockLifter)block).iconGlow, false);
    c = new Color(BlockCustomOreItem.colors[5]);                        // 0xEECCFF
    ...
    block.func_149676_a(0.01f, 0.1f, 0.01f, 0.99f, 0.9f, 0.99f);         // the lower emissive slab
    renderer.func_147775_a(block);
    BlockLifterRenderer.drawFaces(renderer, block, ((BlockLifter)block).iconGlow, false);
```

* the sheet of the two slabs is `iconGlow` = `thaumcraft:animatedglow`
  (oracle `thaumcraft/common/blocks/BlockLifter.java:56`);
* the colour table of the original is
  `{0xFFFFFF, 0xFFFF7E, 16727041, 37119, 40960, 0xEECCFF, 0x555577}`
  (oracle `thaumcraft/common/blocks/BlockCustomOreItem.java:17`), so the upper slab is
  `colors[4]` = 40960 = `0x00A000` and the lower one is `colors[5]` = 15650047 = `0xEECCFF`;
* `shouldRender3DInInventory` answers `true`
  (oracle `thaumcraft/client/renderers/block/BlockLifterRenderer.java:90-92`);
* the block's render type is the render id of that very handler
  (oracle `thaumcraft/common/blocks/BlockLifter.java:73-75`, `getRenderType()` returns
  `ConfigBlocks.blockLifterRI`), and the handler is registered for the block
  (oracle `thaumcraft/client/ClientProxy.java:603`).

### Why that method really drew the item (the 1.7.10 dispatch)

The oracle is the specification, but the route from the item to the method is the Forge 1.7.10
framework the oracle runs on (`MinecraftForge/MinecraftForge`, branch `1.7.10`):

* `fml/src/main/java/cpw/mods/fml/client/registry/RenderingRegistry.java` —
  `renderItemAsFull3DBlock(modelId)` answers `bri.shouldRender3DInInventory(modelId)` and
  `renderInventoryBlock(renderer, block, metadata, modelID)` calls
  `bri.renderInventoryBlock(block, metadata, modelID, renderer)`;
* `fml/patches/minecraft/net/minecraft/client/renderer/RenderBlocks.java.patch` — the render type
  switch ends in `default: return FMLRenderAccessLibrary.renderWorldBlock(...)` (the world) and the
  inventory render ends in `FMLRenderAccessLibrary.renderInventoryBlock(this, block, metadata, j)`;
  `RenderBlocks.func_147739_a(renderType)` — the question `RenderItem` asks before it draws a block
  item in 3D — ends in `FMLRenderAccessLibrary.renderItemAsFull3DBlock(renderType)`.

So in 1.7.10 the levitator item in the inventory slot was drawn by
`BlockLifterRenderer.renderInventoryBlock`: **the cube plus the two emissive inner slabs**, tinted
with the two ore colours of the original. It was never the plain block model.

### The 1.12 successor of the same code (the retained reference tree)

The official 1.12 rewrite of exactly that method is in the retained reference tree of this
repository and is the same art in the 1.12 model system:

* `src/main/resources/assets/thaumcraft/models/item/blocklifter.json` — the cube plus the two
  `animatedglow` elements, `0.16..15.84` by `14.4..15.84` (the 0.01..0.99 by 0.9..0.99 of the
  original) and `0.16..15.84` by `1.6..14.4` (the 0.01..0.99 by 0.1..0.9 of the original), with
  `"tintindex": 0` and `"tintindex": 1` and `"ambientocclusion": false`;
* `src/main/java/thaumcraft/client/renderers/item/LifterItemColor.java:9-17` — the tint 0 is
  `BlockCustomOreItem.colors[4]` and the tint 1 is `BlockCustomOreItem.colors[5]`.

### The port before this change

The port already had that art, it was only the **expectation of the test** that was wrong:

* `common/src/main/resources/assets/thaumcraft/models/item/levitator.json` — the cube plus the two
  `animatedglow` elements with the tint indices 0 and 1 (the parent `minecraft:block/block` is the
  1.21.1 name of the 1.12 `block/block`);
* `common/src/main/java/thaumcraft/client/ClientSetup.java:228-234` — the native item colour of the
  two slabs: the tint 0 is `0x00A000` and the tint 1 is `0xEECCFF`, the ore colours of the
  original;
* `common/src/main/resources/assets/thaumcraft/models/block/levitator.json` — the plain
  `minecraft:block/cube_bottom_top` of the **world** block, which is right: the original draws the
  world cube itself (`renderWorldBlock`, oracle `BlockLifterRenderer.java:55-88`) and the glow
  faces around it.

**Verdict: the model of the port is right, the assertion of the test was wrong.**

## What I changed

1. `common/src/test/java/thaumcraft/assets/LevitatorAssetsTest.java`
   * the wrong assertion (`assertEquals("thaumcraft:block/levitator", itemModel.get("parent"))`)
     is gone — the item model is not a parent reference to the block model;
   * new test `theItemIsTheInventoryModelOfTheOriginal`: the parent is `minecraft:block/block`,
     the four textures of the original (`liftertop`, `lifterside`, `arcaneearbottom`,
     `animatedglow`), the cube of the original with its six faces and the two emissive slabs with
     their exact bounds and their tint indices 0 and 1 — every one of the six faces of a slab is
     checked;
   * new test `theEmissiveSlabsOfTheItemKeepTheOriginalOreColours`: the two ore colours of the
     original (`0x00A000` and `0xEECCFF`) are registered for the levitator item in
     `common/src/main/java/thaumcraft/client/ClientSetup.java`.
   * The test was not deleted and no assertion was weakened: the one wrong assertion was replaced
     by nine precise ones about the item art.
2. `common/src/main/resources/assets/thaumcraft/models/item/levitator.json` —
   `"ambientocclusion": false` added. The inventory render of 1.7.10 shaded the faces of the cube
   and had no ambient occlusion (1.7.10 had no ambient occlusion at all), and the 1.12 model of
   the same art says so as well. Without the key the model inherits `true` from
   `minecraft:block/block` (1.21.1 `BlockModel.hasAmbientOcclusion()` falls back to the parent and
   then to `true`).
3. `docs/porting/scope-ledger.json` — three levitator units, nothing else, no counter touched:
   * `thaumcraft/client/renderers/block/BlockLifterRenderer.java`: `partial` →
     `parity_verified`. The "REMAINING GAP" of the wave-1 review ("the two emissive inner slabs
     of the original inventory render … are not in the native model") was a mis-diagnosis: the
     slabs are in the **item** model, the wave-1 review had only looked at the block model. The
     evidence now names the inventory art of the oracle, the Forge 1.7.10 dispatch, the item model
     of the port and the native item colour;
   * `thaumcraft/client/renderers/item/LifterItemColor.java`: the false sentence "the native
     levitator item has no colour handler" is corrected — the handler exists in
     `ClientSetup.java:233-234`; the disposition stays `exclude` (the 1.12 class itself is not
     ported, its role is taken by the native registration) and the coverage stays
     `parity_verified`;
   * `thaumcraft/client/renderers/tile/TileLifterRenderer.java`: the false sentence "the native
     levitator uses a JSON model" is corrected — the native world art is the block entity
     renderer `TileLevitatorRenderer`.

## The checks

* `python3 scripts/porting/convert_assets.py --verify` →
  `{"atlas_mipmap_level_guard": 4, "atlas_sprites_available": 517, "checked_atlas_sprite_refs": 3883,
  "checked_local_model_refs": 2904, "checked_model_texture_refs": 3883, "checked_sound_event_ids": 66,
  "checked_sound_event_refs": 0, "checked_sound_refs": 111, "missing_references": 0}`
* `python3 scripts/porting/update_scope_ledger.py --check --original-root
  /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` → `stale_review_fingerprints: 0`, the
  counters are the ones of the file;
* `python3 scripts/porting/check_scope_ledger_reviews.py` → `invalid_reviews: 0`,
  `stale_review_fingerprints: 0`, `counts_match: true`;
* `./gradlew :common:test` → see the final message of this lane (the whole suite, 0 failures).

## What I could not settle

* **Are the two inner slabs visible in the inventory slot?** They lie inside the opaque cube
  (0.01..0.99 against 0..1) and the depth test hides them, so the visible art of the inventory
  slot is the plain cube — in the original and in the port alike. That is a geometric argument
  about the 1.7.10 depth test, not a screenshot: the 1.7.10 client does not run in this
  environment. It does not change the verdict, because the original *drew* the slabs and the port
  keeps them with the same tints.
* **The world glow of the levitator** (the panels one hundredth of a block inside the face of the
  original against the panels outside the face of the port) is the open item of the wave-1
  `clientrender` lane; I did not touch `TileLevitatorRenderer`.
* The colour of the world side panels (`0xDD11FF`) is not the colour of the lower slab of the
  item (`0xEECCFF`): that is the original's own difference between the world render
  (oracle `BlockLifterRenderer.java:71`) and the inventory render
  (oracle `BlockLifterRenderer.java:44-51`), and the port keeps both.

## Acceptance criteria for a client run (the leader)

* the levitator item in the inventory slot shows the cube with `liftertop` on top,
  `lifterside` on the sides and `arcaneearbottom` at the bottom, flat shaded (no ambient
  occlusion), exactly as the original;
* the two emissive slabs are inside the cube, so they are not visible — as in the original;
* the block in the world shows the glow panels around the cube, in `0x00A000` on top and
  `0xDD11FF` on the sides (the wave-1 item, untouched here).
