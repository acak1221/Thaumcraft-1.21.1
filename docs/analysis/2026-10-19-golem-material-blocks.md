# Thaumium/Tallow material blocks — 2026-10-19

**Authored content, root integration/compile pending.** Added `ThaumcraftGolemMaterials.java`, two blockstates, two block models, two item models, two self-drop loot tables and four ordinary recipes. No existing files, worlds, exports/backups, golem registrations/material restrictions or other owners' WIP were changed/read. No Java/Gradle/game/tests/Python/smoke work ran here.

## Confirmed original contracts

Private oracle root: `../work/thaumcraft-4.2.3.5-oracle/thaumcraft/`.

- `common/config/ConfigRecipes.java:171`: **GolemThaumium** consumes `blockCosmeticSolid` metadata **4**; `:166`: **GolemTallow** consumes metadata **5**. Both later golem recipes cost humanus8/motus8/spiritus8. This module supplies the real ingredient blocks, **not those golems**.
- `ConfigBlocks:279,318` creates/registers `BlockCosmeticSolid`/its metadata ItemBlock. `common/blocks/BlockCosmeticSolid.java:64–70` uses **Material.rock, stone sound, hardness2, random-tick flag**. Neither material is wax/honey or metal for these inherited properties; map color stays stone.
- `BlockCosmeticSolid:213–252`: Thaumium4 overrides hardness to **4**, explosion resistance to **20**. Tallow5 keeps hardness **2** and effective resistance **6**: old `setResistance(10)` stores30, vanilla getter divides by5 (existing private vanilla disassembly `.tmp/vanilla1710-Block.javap.txt:2950–2958,3618–3624`). These are the native `.strength(4,20)` / `.strength(2,6)` values, not10 for Tallow.
- `:182–193,255–295,325–370`: full opaque solid cube, ordinary creature spawning and one self drop; **no BE, emitted light, tick effect, activation/sneak behavior or power transition for metadata4/5**. Speed paving/warding/node/fetter behavior belongs to other metadata. Retaining `.randomTicks()` preserves the original no-op flag; no new tick class is needed.
- No CosmeticSolid flammability override or fire registration exists in the oracle. Both native blocks therefore retain default **zero fire spread/flammability**; no Fabric flammable-block or fuel hook should be added just because the name is Tallow.
- **Beacon quirk:** `BlockCosmeticSolid:356–358` accepts every metadata value as a beacon base, including **Tallow5**. This needs a root tag addition below, not a custom block/BE.

## Recipes and retained assets

`ConfigRecipes:439–442,513–516` confirms all four conversions are **ordinary shaped crafting**, with no research/vis gate:

| Native recipe ID | Inputs → output | Original reference |
| --- | --- | --- |
| `thaumcraft:thaumium_block` | 3×3 `c:ingots/thaumium` → 1 block | `BlockThaumium`, line439, `ingotThaumium` ore recipe |
| `thaumcraft:thaumium_block_unpack` | single block → 9 `thaumium_ingot` | unnamed shaped recipe, line440 |
| `thaumcraft:tallow_block` | 3×3 exact `magic_tallow` → 1 block | `BlockTallow`, line441, itemResource4 |
| `thaumcraft:tallow_block_unpack` | single block → 9 `magic_tallow` | unnamed shaped recipe, line442 |

The existing `data/c/tags/item/ingots/thaumium.json` already contains the native ingot and is reused unchanged. No custom serializer or guessed nine-tallow rule was introduced. JSON results use 1.21.1 `id`/`count`.

Models reuse existing `assets/thaumcraft/textures/blocks/thaumiumblock.png`, `tallowblock.png`, `tallowblock_top.png`; all three were byte-matched against their original 4.2.3.5 JAR entries without extracting/replacing files. `BlockCosmeticSolid:84–86,115–120` specifies Thaumium texture on all faces, Tallow side texture on horizontal faces and top texture on **both top and bottom**. Tallow uses a fixed cube-column JSON model with a plain Block, **not an invented rotatable pillar**. No renderer/client initializer is needed.

## Root integration required

1. Call **`ThaumcraftGolemMaterials.register()` after `ThaumcraftContent.register()`**. The module registers BLOCK/ITEM deferred registers and appends both items to the existing creative tab. Public suppliers: `THAUMIUM_BLOCK`, `TALLOW_BLOCK` (`RegistrySupplier<Block>`); `THAUMIUM_BLOCK_ITEM`, `TALLOW_BLOCK_ITEM` (`RegistrySupplier<Item>`).
2. Append both IDs to `data/minecraft/tags/block/mineable/pickaxe.json` (**not axe**); no higher tool-tier tag is indicated by the original. Properties require a correct tool, so these entries are needed for ordinary harvesting/self drops. Also append both to **`data/minecraft/tags/block/beacon_base_blocks.json`** to preserve the original beacon behavior. Existing tags were not edited here.
3. Add language keys `block.thaumcraft.thaumium_block` / `block.thaumcraft.tallow_block`. Add recipe aliases **`BlockThaumium → thaumcraft:thaumium_block`** and **`BlockTallow → thaumcraft:tallow_block`** in `ThaumcraftRecipeDisplay`. If mapping literal cosmetic-solid icons, metadata4/5 map to these same native item IDs.
4. Original book pages (`ConfigResearch:320,327`): GOLEMTALLOW = `tc.research_page.GOLEMTALLOW.1`, `BlockTallow` ordinary recipe, `GolemTallow` crucible recipe; concealed, parents **GOLEMCLAY/TALLOW**. GOLEMTHAUMIUM = `tc.research_page.GOLEMTHAUMIUM.1`, `BlockThaumium` ordinary recipe, `GolemThaumium` crucible recipe; concealed, parents **GOLEMIRON/THAUMIUM**. No separate block-research key or artificial compression gate is needed. Golem2/7 remain disabled until their runtime/registration owner deliberately ports them.
5. **Object aspects remain recipe-derived.** Original `ConfigAspects` has no direct/complex registration for CosmeticSolid4/5. Line388 targets default metadata0, not a wildcard; lines398/399 target6/7. Do not copy those lists or invent a direct EarlyObjectAspectBindings row for either block. The existing recipe resolver should derive their actual compression inputs once registered.

No other interaction hooks or custom block class are required by these two original variants. The listed root tags/book/bootstrap work is not silently claimed complete; neither these prerequisites nor the deferred material golems imply all M10/addon behavior is done.

**Source SHA-256:** new `ThaumcraftGolemMaterials.java` = `43dcf9b5822e456c3f24d4c19be7968a78f58539816300581e7ab7d48917f483`; original oracle `BlockCosmeticSolid.java` = `8c0daef5ad9249901f6ae31fb75b8f44e6e2efe35ef09cba84ea1991389edc62`. These identify authored/reference sources, not a compile, visual or gameplay PASS.
