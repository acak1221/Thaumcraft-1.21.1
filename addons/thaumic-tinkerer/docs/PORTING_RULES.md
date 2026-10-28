# Porting rules for the Thaumic Tinkerer add-on

The behaviour specification is the 1.7.10 source at `/home/user/gtnewhorizons/thaumictinkerer` (commit
`28f4def9...`, see `NOTICE.md`; clone <https://github.com/GTNewHorizons/ThaumicTinkerer> to read it). Port values,
research keys, recipes, aspects, messages and quirks as they are. A deviation is allowed only for something that 1.21.1
cannot do, or a reproducible crash/exploit, and is written down in `docs/deviations/<group>.md` (one file per group).

## 1. Hard rules

1. **Header.** Every ported Java file keeps the original license header and gets the line
   ` * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).` (copy it from
   any existing file under `common/src/main/java/thaumic/tinkerer/common/lib`). A file that is new in the port (no
   1.7.10 counterpart: tests, 1.21.1 glue) says so in its header instead of claiming Vazkii as author.
2. **API only.** The add-on imports `java.*`, `net.minecraft.*`, `com.mojang.*`, `dev.architectury.*`,
   `net.fabricmc.*` (Fabric tree only), `com.google.*`, `org.slf4j.*` and `thaumcraft.api.*`. Never
   `thaumcraft.common.*` or `thaumcraft.client.*`. If the API lacks something the original used, do **not** reach
   into the mod: write the need into `docs/api-requests/<group>.md` (what, proposed signature in 1.21.1 types, which
   class of the mod backs it) and stub the feature behind a clearly named method so it compiles; the lead extends the
   API. `docs/API_GAPS.md` already analyses the known gaps. A test will enforce this rule.
3. **Groups do not reference each other's classes.** Reach an object of another group by its registry path:
   `TTRegistry.item("focus_flight")`, `TTRegistry.block("enchanter")`, `TTRegistry.stack("ichor")`, with the constants of
   `LibItemNames`/`LibBlockNames`. Only code run after registration may call those.
4. **Edit only your own files.** Your group's registrar (`common/content/<Group>.java` and `client/<Group>Client.java`
   if you need one), new files under your own packages, resources listed in section 4. Shared files (`TTRegistry`,
   `ThaumicTinkerer`, `ResearchHelper`, `ConfigHandler`, the build files) are the lead's; ask for a change in your report.
5. **No Gradle, no git.** Compile with `addons/thaumic-tinkerer/tools/compile.sh` (javac, section 5). Do not commit.
6. **Annotations.** `@Nullable` is `org.jetbrains.annotations.Nullable` (the one the mod uses); `javax.annotation` is not on the classpath.
7. **Do not weaken tests** and do not delete content because it is hard; list it as missing in your report.

## 2. Architecture

- `ThaumicTinkerer.init()` calls `ContentGroup.register()` of every group (both sides, mod init), then
  `TTRegistry.registerAll()`. `onThaumcraftReady()` runs the recipe of every `ITTinkererRegisterable`, then every
  `ContentGroup.ready()`, then the research of every registerable, then flushes the research queue (Kami entries last).
  `ThaumicTinkerer.initClient()` calls `ContentGroup.registerClient()` (client only).
- Register objects with `TTRegistry.item(path, factory)`, `block(path, factory[, itemFactory])`,
  `blockWithoutItem`, `hiddenItem`, and the `DeferredRegister`s `BLOCK_ENTITIES`, `MENUS`, `ENTITY_TYPES`,
  `MOB_EFFECTS`, `SOUND_EVENTS`, `DATA_COMPONENT_TYPES`, `ARMOR_MATERIALS`. A block or item class that has a
  research entry or recipe implements `ITTinkererRegisterable` (`getResearchItem()`, `getRecipeItem()`) exactly like the
  original; the recipe objects (`ThaumicTinkerer*Recipe`) register through `ThaumcraftApi` and are stored by name in
  `TTRecipes`; pages are made with `ResearchHelper.*Page(name)`.
- Registry paths are the 1.7.10 names in snake_case (`LibBlockNames`, `LibItemNames`; the research keys of
  `LibResearch` are unchanged). 1.7.10 metadata variants become separate items/blocks/block states (document the
  mapping in your deviations file). The mod id and namespace is `thaumictinkerer`.
- Crafting-table recipes are JSON files `data/thaumictinkerer/recipe/<name>.json`; register a
  `ThaumicTinkererCraftingBenchRecipe(name)` so pages can show them. Ore dictionary names become tags
  (`OreDictionaryNames` of the API resolves the 1.7.10 names; for plain recipes use `c:` tags).
- Stack NBT becomes data components (`TTRegistry.DATA_COMPONENT_TYPES` with `Codec` and `StreamCodec`), or
  `CustomData` for small ad-hoc values. Block entities save `CompoundTag` (`saveAdditional`/`loadAdditional` with the
  `HolderLookup.Provider` argument) and sync with `getUpdateTag`/`getUpdatePacket`.
- Menus: `MenuRegistry.ofExtended`/`of` (Architectury) registered in `TTRegistry.MENUS`; screens with
  `MenuRegistry.registerScreenFactory` in `registerClient()`. Networking: `CustomPacketPayload` records with a
  `StreamCodec`, registered with `NetworkManager.registerReceiver(Side.C2S/S2C, ...)`; S2C payload types must also be
  registered on the server (`NetworkManager.registerS2CPayloadType`) from `register()`.
- Renderers: `BlockEntityRendererRegistry.register`, `EntityRendererRegistry`, `ColorHandlerRegistry`,
  `ItemPropertiesRegistry`, `RenderTypeRegistry` (all `dev.architectury.registry.client...`) from `registerClient()`.
  The 1.7.10 ISBRH/`IItemRenderer`/`ModelBase` renderers become JSON models where the shape is static, otherwise block
  entity renderers. Keep client-only classes (anything importing `net.minecraft.client`) in `thaumic.tinkerer.client`.
- Events: Architectury events (`dev.architectury.event.events.common.*`) where they exist; where they do not, say so in
  your report (the lead adds a loader hook) instead of using a Fabric-only API in common code.
- Config: the static fields of `ConfigHandler` (same names as 1.7.10). Enchantments, dimensions, damage types are data
  (`data/thaumictinkerer/...`); 1.7.10 numeric ids are gone.
- Do not hold a `Level`, `Player` or `BlockEntity` in a static field. Server logic runs on the server thread; check
  `level.isClientSide`.

## 3. Resources

Textures were converted by `scripts/porting/tt_assets.py` (look at its output tree and `scripts/porting/tt_assets_map.json`
for the old-to-new name of every file): `assets/thaumictinkerer/textures/{item,block,gui,misc,models,enchant}/<snake_case>.png`.
Authored by you (JSON, hand-written, no generator): blockstates, block/item models (`assets/thaumictinkerer/models/...`),
loot tables (`data/thaumictinkerer/loot_table/blocks/<path>.json`), recipes. Block tools tags: add the block ids to
`data/thaumictinkerer/tags/block/mineable/<group>/pickaxe.json` (or `axe`, `shovel`) and
`data/thaumictinkerer/tags/block/needs/<group>/<tier>_tool.json` (`stone`, `iron`, `diamond`): the vanilla tags already
include `#thaumictinkerer:mineable/<group>/<tool>` and `#thaumictinkerer:needs/<group>/<tier>_tool` for the groups
`simple_blocks`, `tile_blocks`, `machines` and `kami`; the (empty) files exist, fill yours.
Language keys: the converted base already carries the 1.7.10 texts. New 1.21.1-only keys go into
`addons/thaumic-tinkerer/lang-extra/<group>.<lang>.json` (flat objects; at least `en_us` and `ru_ru`); never edit
`assets/thaumictinkerer/lang/*.json` directly (generated). 1.21.1 keys: `block.thaumictinkerer.<path>`,
`item.thaumictinkerer.<path>`, `container.thaumictinkerer.<path>`, `enchantment.thaumictinkerer.<path>`,
`effect.thaumictinkerer.<path>`, `itemGroup.thaumictinkerer`.

## 4. Tests

- JUnit (plain logic, no Minecraft world): `common/src/test/java/thaumic/tinkerer/<group>/...Test.java`.
- GameTest (a running server): checks in `common/src/gameTest/java/thaumic/tinkerer/testing/<Group>RuntimeChecks.java`
  as `public static void name(GameTestHelper test)`; the Fabric wrappers go in
  `fabric/src/gameTest/java/thaumic/tinkerer/fabric/testing/<Group>GameTests.java` (a stub per group exists: Core, SimpleBlocks, TileBlocks, Machines, BasicItems, Foci, Enchantments, KamiGear, KamiWorld):
  `@GameTest(template = FabricGameTest.EMPTY_STRUCTURE) public void name(GameTestHelper h) { Checks.name(h); }`.
  Test real behaviour (state transitions, drops, costs, persistence through `saveWithFullMetadata`/`loadWithComponents`),
  never "the class exists".
- Always assert `ThaumicTinkerer.failures().isEmpty()` in one check of your group.

## 5. Compiling

```bash
addons/thaumic-tinkerer/tools/compile.sh                       # everything
addons/thaumic-tinkerer/tools/compile.sh common/src/main/java/thaumic/tinkerer/common/block   # a directory (relative to addons/thaumic-tinkerer)
addons/thaumic-tinkerer/tools/compile.sh -g                    # with the GameTest sources
```
It needs `addons/thaumic-tinkerer/.tmp/classpath.txt` (written by the lead's Gradle task); the output goes to a
temporary directory.  Fix every error in your own files; an error in another group's file is not yours: tell the lead.

## 6. Report

End with: files created; what is implemented and compiled; what is **not** done or deviates (and why); API requests;
shared-file change requests; tests written and whether they could be run; anything you could not verify. Do not claim
a test passed that you could not run.
