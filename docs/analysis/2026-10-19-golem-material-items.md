# Golem material items — 2026-10-19

Updated only `common/src/main/java/thaumcraft/common/items/golems/ItemGolemPlacer.java` and added this note. No runtime/enum, registry/bootstrap, recipe, renderer, menu, resource, dependency, export or save changes; other owners' WIP was not read.

## Item API

- Existing `ItemGolemPlacer(Properties)` delegates to material0 (Straw).
- New `ItemGolemPlacer(Properties, int material)` stores a final material, accepting **only 0/1/3/5/6** with a pure finite check, no registry lookup. Invalid IDs throw rather than falling back to Straw.
- Immediately after constructing the entity, placement calls the agreed **`EntityGolemBase.setGolemMaterial(int)`**, **before** `applyConfiguration(data)`.
- Material comes from the native placer item identity, **not GolemData**. Root supplies `golem_straw`, `golem_wood`, `golem_clay`, `golem_stone`, `golem_iron` with stack limit1 and the existing GOLEM_DATA/EMPTY default. The existing root `instanceof ItemGolemPlacer` use-first callback covers all variants without another hook.

All previous collision/support/permissions, fence offset, owner/name, core/ghost-filter setup, admission, creative handling and survival payment code is unchanged. No new teleport correction or shrink path. Runtime owns actual synced `getGolemMaterial()`, material attributes and recall through root's `placerForMaterial(int)`; those paths must preserve material rather than defaulting to Straw. No additional item getter is required.

Original private oracle `thaumcraft/common/config/ConfigRecipes.java:165,167,169–170` confirms the next four **crucible** catalysts:

| Material | Catalyst |
| --- | --- |
| Wood1 | Greatwood log, original blockMagicalLog metadata0 |
| Clay3 | Clay **block**, not clay ball |
| Stone5 | Stone **bricks**, not cobblestone |
| Iron6 | Iron **block**, not ingot |

Each costs humanus4/motus4/spiritus4 and produces one corresponding placer. Root owns those recipes/research aliases and visuals. Tallow2, Flesh4, Thaumium7 and other cores remain deferred.

## Inputs / verification boundary

SHA-256 of updated `ItemGolemPlacer.java`: `0d07dd67b58ccaba6f4ad42981d4578e3e5911ba48c50711b2376b79a7df5aed`.

The three other item/data files were **not edited**, and their before/after hashes match:

| File | Unchanged SHA-256 |
| --- | --- |
| `GolemData.java` | `e347b76721fcbe63c5015d42ed45a04d8b6f85b188749d6927b5e63e0d10ef4f` |
| `ItemGolemCore.java` | `a4a78e388b8ff1d77c2f2cbb94dcfc12235d94264e5b04dddd9e9c1a6fe59f5d` |
| `ItemGolemBell.java` | `6b54a21afa7d954624a7023828caa1c91ab011b58792b3e8e698f9337263c712` |

GolemData remains only core/6 ghosts, including its semantic native-stack equality/codecs; existing bell marker limitations remain unchanged. **Root compilation of this material extension is pending.** No Gradle/Java/game/tests/Python/world work ran here; the earlier Straw/Gather build result is not a pass for these new inputs.
