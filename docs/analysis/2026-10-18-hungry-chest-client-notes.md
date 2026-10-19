# Hungry Chest client presentation — 2026-10-18

## Handoff status and scope

Implemented source only; **native compilation and in-game visuals remain unverified**. The client-owned Java/resource files are frozen for root integration at the hashes below. No further edits by this owner after handoff.

Only these new implementation files were written:

- `common/src/main/java/thaumcraft/client/renderers/tile/TileChestHungryRenderer.java`
- `common/src/main/java/thaumcraft/client/ThaumcraftHungryChestClient.java`
- `common/src/main/resources/assets/thaumcraft/blockstates/hungry_chest.json`
- `common/src/main/resources/assets/thaumcraft/models/block/hungry_chest.json`
- `common/src/main/resources/assets/thaumcraft/models/item/hungry_chest.json`

Bootstrap, existing item routes, registration, runtime classes, language, recipes/loot/research, Python, tests/smoke/readers, accepted binaries, saves and retained assets were not edited. WIP Hungry Chest runtime/registry classes were not read. No agents were spawned; no Gradle, Java/game launch, build, test or world operation was run. Read-only `javap` inspection of the cached mapped 1.21.1 API was used.

## Agreed runtime/client contract

- `thaumcraft.common.tiles.TileChestHungry` is the BER's tile type. Its `getOpenNess(float partialTick)` returns **raw linear interpolation** of previous/current lid angle, normally `[0, 1]`. Runtime retains all animation advancement, including the original `0.1`/tick rate; the renderer only applies cubic easing once.
- World facing is read from `BlockStateProperties.FACING`, not horizontal-facing or a custom orientation API. The block must use `RenderShape.ENTITYBLOCK_ANIMATED`.
- `ThaumcraftHungryChest.HUNGRY_CHEST_BLOCK_ENTITY.get()` supplies `BlockEntityType<TileChestHungry>`. Root owns `HUNGRY_CHEST` and `HUNGRY_CHEST_ITEM` registration.
- Root calls `ThaumcraftHungryChestClient.init()` from its client bootstrap after common registration. This initializer is idempotent and registers only the BER; it adds no custom menu/screen. `GENERIC_9x3` / `ChestMenu` already use the vanilla chest screen.
- `TileChestHungryRenderer(BlockEntityRendererProvider.Context)` bakes `ModelLayers.CHEST` per native renderer lifecycle/reload. No static model cache, player/world lookup or independently registered layer is needed.
- The renderer also exposes the **instance** method `public void renderStatic(Direction facing, float openness, PoseStack pose, MultiBufferSource buffers, int light, int overlay)`. `openness` is raw, not already eased. This is the shared world/item draw path; it does not require or mutate a tile.

## Root-owned item route

Use the native **item** dispatcher entry, not its world-render entry. The agreed root-owned fallback has the ordinary `(BlockPos, BlockState)` tile constructor, no level, no ticks and fresh/closed lid state. The exact two-line builtin-router hook is:

```java
private final TileChestHungry hungryChest = new TileChestHungry(BlockPos.ZERO, ThaumcraftHungryChest.HUNGRY_CHEST.get().defaultBlockState());
if (stack.is(ThaumcraftHungryChest.HUNGRY_CHEST_ITEM.get())) { Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(hungryChest, pose, buffers, light, overlay); return; }
```

The field belongs with the router's other private snapshots; the branch belongs in its item rendering method, after the native item display transform. Imports are `TileChestHungry`, `ThaumcraftHungryChest`, `BlockPos`, and `Minecraft`. Do not attach the snapshot to a level or read a block under the player. Native `renderItem` dispatch reaches this BER with partial tick `0`; the BER deliberately uses original metadata0 rotation for every detached tile, independent of the block's default facing. A direct renderer caller can instead use `renderStatic(Direction.DOWN, 0, pose, buffers, light, overlay)` on the dispatcher-owned renderer instance.

For the current Fabric `BuiltinItemRendererRegistry` pattern, the equivalent registration uses the same root-owned snapshot:

```java
BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftHungryChest.HUNGRY_CHEST.get(),
        (stack, context, pose, buffers, light, overlay) -> Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(hungryChest, pose, buffers, light, overlay));
```

Neither existing route was edited here. No additional/future items should be registered.

## Original transform, geometry and asset provenance

Authority: `../work/thaumcraft-4.2.3.5-oracle/thaumcraft/client/renderers/tile/TileChestHungryRenderer.java`, especially lines 28, 39, 43–65. Its `ModelChest` uses the original 64x64 `thaumcraft:textures/models/chesthungry.png`, which is already retained under `common/src/main/resources/assets/thaumcraft/textures/models/`. SHA-256 of that retained PNG exactly matched the original JAR entry; neither was modified. The similarly named block texture is **not** the model texture.

The 1.21.1 native single-chest layer has the same cube dimensions and box-UV offsets, but different local positions. Directly applying the old mirrored transform to an unchanged native layer would put the bottom/lid at the wrong height and interpret the retained texture in the modern orientation. The renderer keeps the baked cubes and UVs, changing only part translations and adding empty hinge parents:

| Part | Native cube minimum; dimensions | Adapter before original renderer transform | Resulting old ModelChest coordinates |
| --- | --- | --- | --- |
| bottom | `(1,0,1)`; `14x10x14`, UV `(0,19)` | part position `(0,6,0)` | unrotated bounds `(1,6,1)` to `(15,16,15)` |
| lid | `(1,0,0)`; `14x5x14`, UV `(0,0)` | child offset `(-1,-5,-14)`, hinge `(1,7,15)` | local cube `(0,-5,-14)` to `(14,0,0)` at original hinge |
| lock | `(7,-2,14)`; `2x4x1`, UV `(0,0)` | child offset `(-8,0,-29)`, hinge `(8,7,15)` | local cube `(-1,-2,-15)` to `(1,2,-14)` at original hinge |

This preserves each cube's UV orientation instead of flipping/converting the original asset. The original transform is then applied literally: translate `(0,1,1)`, scale `(1,-1,-1)`, pivot `(0.5,0.5,0.5)`, Y rotation, inverse pivot. Original rotations are NORTH/metadata2 `180`, SOUTH/3 `0`, WEST/4 `90`, EAST/5 `-90`, and DOWN/0 or UP/1 `0`. No vertical tilt or horizontal-facing reinterpretation is introduced.

Lid and lock share `-(1 - (1 - rawOpenness)^3) * PI/2`; draw order is lid, lock, bottom. Each draw overwrites both rotations, so a closed item cannot inherit an open world's previous pose. Light/overlay are supplied by the native caller; `entityCutout` samples the retained PNG directly, not the vanilla chest atlas, seasonal textures or a cuboid JSON substitute.

The blockstate covers all six `facing` values with the same `builtin/entity` model; orientation belongs to the BER. Block/item particle is the existing `thaumcraft:blocks/woodplain`. The item model also uses `builtin/entity`, with the native single-chest GUI/hand/ground/frame transforms, not inheritance from `minecraft:item/chest` or its default texture.

Cached native API inspection confirmed `ChestRenderer.createSingleBodyLayer()`'s coordinates/UVs/64x64 layer, `ModelPart`'s public child/position/constructor/render API, `Context.bakeLayer`, and `BlockEntityRenderDispatcher.renderItem`. The mapped reference was `.gradle21/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2/minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2.jar`, SHA-256 `09170566a275cbf2dbd20f80841c47e5e68e150e1dddfd3667ca5b41a67a5ce3`.

## Frozen hashes

Paths are relative to the repository unless marked otherwise; hashes are SHA-256. This note is not self-hashed.

| File | SHA-256 |
| --- | --- |
| `common/src/main/java/thaumcraft/client/renderers/tile/TileChestHungryRenderer.java` | `faf48e15e202462ebcdfa04e840fcb32987c831b011f4138b8005399ef8145f6` |
| `common/src/main/java/thaumcraft/client/ThaumcraftHungryChestClient.java` | `b56fa1a5939cdb29af56211ca83d706182f66e131b070110308505954e63e790` |
| `common/src/main/resources/assets/thaumcraft/blockstates/hungry_chest.json` | `31b64893cfc151a2ef4240a4908172702cb2d6477c9c603f07926e968811a572` |
| `common/src/main/resources/assets/thaumcraft/models/block/hungry_chest.json` | `0809d62da74db03040f83d9e7fe523d0426f304b4494a8d806ceda62dd977f16` |
| `common/src/main/resources/assets/thaumcraft/models/item/hungry_chest.json` | `3bb41b9131bf2fccd0c05884194f24ddfe5a0db2088f606f450f82420c7150ec` |
| Original oracle renderer (path above) | `6a1218bc6c1514d8da9401ae7acae468fbc4698374c16ae03b7fdb5cdb876f78` |
| `Thaumcraft-1.7.10-4.2.3.5.jar` | `3dba9786966974701578a658d1bb369bf35bdf5f363079f5ac9c4910a39113be` |
| Retained `textures/models/chesthungry.png` and original JAR entry | `2b8dc5862822afa315c2c797e7b8081daa7be89517bdb36c69ff437ee04ac8cb` |
| Retained `textures/blocks/woodplain.png` | `3344f48acac3f396a4b2608e1c694427540def6ed7a2075b6f196d362e14a0a6` |

## Verification still owned by root

Readback/static review and original-texture hash comparison are not native compilation or visual evidence. Root must integrate the agreed runtime/bootstrap/item hooks and compile the actual Fabric target after this handoff. In-game chest texture orientation, all six facings, opening/closing/nibble interpolation, closed item display and native menu presentation remain **unseen/unverified**. No compile, test, runtime, screenshot or visual pass is claimed by this client handoff.
