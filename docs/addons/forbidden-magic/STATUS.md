# Forbidden Magic addon — port status

Started 2026-10-25, user-authorized ("Смотри на основе этого аддона нужно сделать под мою версию
порта аддон. Оригинал это эталон"). This is a new addon mod alongside the TC4 core port in this
repository, not part of the core port's own M00-M15 milestones.

## Source of truth

- **Oracle**: `Forbidden_Magic-1.7.10-0.575.jar` (SpitefulFox, WTFPL), modid `ForbiddenMagic`,
  dependency `required-after:Thaumcraft@[4.2.2.0,)`. Decompiled with Vineflower 1.11.1
  **outside this repository** (scratch workspace only, per this repo's own
  "never commit decompiled sources" rule for donor jars) to read exact aspect/recipe/item values.
  Lang strings (`en_US`/`ru_RU`) were extracted directly from the jar's `assets/forbidden/lang/`.
- **Pinned reference, not copied**: https://github.com/MxxFoxx2/forbidden-magic-reforged — an
  independent AI-assisted port of the *same* original mod, but onto a **different, unrelated
  host**: a separate reimplementation called "Thaumcraft 4: Reforged" (by `notlikethiss`, All
  Rights Reserved, not published), targeting Minecraft 26.3 / NeoForge / Java 25. Its Java source
  cannot be reused here (different host API, different MC version, different package namespace
  `dev.visfox.forbiddenreforged`). Its `docs/fidelity.md` documents verified facts about the
  *original* mod's exact behavior (drop-chance formulas, wand mechanics, recipe costs) that were
  useful as a secondary cross-check against the decompile — every fact taken from it here was also
  confirmed against the decompiled original class before being ported. It must not be treated as
  authoritative on its own, same as this repo's own policy toward OpenTC4/OpenTC4-Architectury.
- Where the sibling port had already solved a modern-API problem the original can't express
  1:1 (metadata flattening, config-gated content), its *documented solution* was reused
  deliberately when it fit this repo's own data-component/registry conventions; this is called
  out inline wherever it applies (see `ForbiddenMagicItems` class doc).

## Layout

- `forbiddenmagic-common/` — new Gradle module, modid `forbiddenmagic`, package `forbiddenmagic.*`.
  `compileOnly`/`testImplementation` on `:common`'s `namedElements` (thaumcraft.api only; never
  bundles host classes). **Deliberately not wired into the root `build`/`check` aggregate tasks**
  in `settings.gradle`/`build.gradle` — addon work in progress must never regress core Thaumcraft
  CI. Build/test it explicitly: `./gradlew :forbiddenmagic-common:build`.
- No `forbiddenmagic-fabric`/`forbiddenmagic-neoforge` platform module yet (see "Not done" below).
- `docs/addons/forbidden-magic/` (this file) is this addon's own status doc; it does not touch
  `docs/MIGRATION_STATUS.md` or the core port's own checkpoints.

## Checkpoint 1 (2026-10-25) — sin aspects + self-tagged items

**Implemented and tested**, `./gradlew :forbiddenmagic-common:test` (common module only; not run
through the full repo's `./scripts/dev.sh` or any GameTest/client check — see "Not done"):

- Six custom aspects ported field-for-field from the decompiled `DarkAspects.initAspects()`:
  `infernus`/NETHER, `ira`/WRATH, `invidia`/ENVY, `gula`/GLUTTONY, `superbia`/PRIDE,
  `desidia`/SLOTH, plus the seventh, `luxuria`/LUST, gated by `ForbiddenMagicConfig.noLust`
  (default `false`, i.e. enabled, matching the original's own default).
  `DarkAspectsTest` asserts tag/color/components against the decompiled literals.
- Confirmed (not a port decision, a verified fact about the original): the "Greed" and "Taint"
  shards reuse the **base TC4 aspects** `Aspect.GREED` (lucrum) and `Aspect.TAINT` (vitium) — the
  original never declares new aspects for them.
- Eleven items, the original's "NetherShard" family (`ItemDeadlyShard`, 7 damage values) plus the
  standalone `GluttonyShard` and two of `ItemResource`'s five subtypes: `wrath_shard`,
  `envy_shard`, `taint_shard`, `pride_shard`, `lust_shard`, `despair_shard`, `sloth_shard`,
  `greed_shard`, `gluttony_shard` (food, nutrition 2 / saturation 0.1, from
  `ItemFood(2, 0.1F, false)`), `emerald_fragment`, `shadow_ink` (dyes an unsheared non-black sheep
  black and consumes itself, from the original's `itemInteractionForEntity`).
  **Documented metadata-flattening deviation**: the original's single damage-value-4 item (Lust OR
  Despair depending on `Config.noLust`) becomes two always-registered items here; see the
  `ForbiddenMagicItems` class doc for why and for the prior art (the sibling reforged port hit the
  same flattening problem and solved it the same way).
- Each item's own aspect self-tag, ported from the decompiled `DarkAspects.addAspects()` lines
  185-203 verbatim (amounts, which aspect, including the `taint_shard`'s odd one out: no NETHER
  component, unlike every other shard). `ForbiddenMagicAspectTagsTest` asserts the exact
  `AspectList` contents per item via `ThaumcraftApiHelper.getObjectAspects`.
- `en_us`/`ru_us` lang (item names + `tc.aspect.<tag>` flavor text), copied from the original
  jar's own `en_US.lang`/`ru_RU.lang`.

**Explicitly not claimed**: no textures/models (items render with the missing-texture
placeholder; this is a data-correctness checkpoint, not a visual one — same split this repo's own
core port uses), no recipes, no research pages, no live game/GameTest/client verification, no
mod loader entrypoint (nothing registers these items in an actual running game yet — only
`ForbiddenMagic.bootstrap()` called directly, from JUnit).

## Deferred within "aspects" itself

`DarkAspects.addAspects()`'s remaining ~90 lines (glowstone, nether wart, blaze rod/powder,
magma block, obsidian, quartz ore, netherrack, soul sand, gold nugget/ingot/block/apple/horse
armor, diamond, leather, spider eye, rotten flesh, cooked porkchop, raw fish, bacon, and ~20
mob-entity aspect bindings) reference 1.7.10 **obfuscated SRG field names**
(`Items.field_151156_bN` etc.) in the decompiled class. Porting these correctly requires resolving
each one against a verified 1.7.10 SRG→item mapping table first; guessing would risk tagging the
wrong vanilla item, which this repo's own "fix with evidence, document deviations" policy does not
allow. Left for a follow-up checkpoint with the mapping actually checked.

## Full remaining content (not started)

Grouped by original package/class, all ~97 classes in the oracle jar minus what checkpoint 1 covers:

- **Blocks** (`blocks/`): `BlockArcaneCake`, `BlockBlackFlower`(+`BlockRoseBush`), tainted wood set
  (`BlockLogTainted`, `BlockLeavesTainted`, `BlockPlanksTainted`, `BlockSaplingTainted`,
  `BlockStoneTainted`+`ItemBlockStoneTainted`), `BlockResource`, `BlockWrathCage` (+its block
  entity `TileEntityWrathCage`, `WrathSpawnerLogic`, and renderer `TileEntityWrathCageRenderer`).
- **World gen**: `WorldGenTaintedTree`.
- **Enchantments** (`enchantments/`): `DarkEnchantments` registry + 8 enchantments (Fiery Core,
  Wrath, Capitalist/Greedy, Consuming, Educational, Corrupting, Voidtouched, Impact).
- **Potions** (`potions/`): `DarkPotions` + `PotionDragonwrack` (keep; not compat-gated).
  `PotionBloodSeal` is Blood Magic-gated — skip, see below.
- **Remaining items**: `ItemArcaneCake`, `ItemFruitTainted`, `ItemMobCrystal` (+ its creative tab,
  gated on an equivalent of `Config.wrathCage`), `baubles/ItemRingNutrition`,
  `scribes/ItemCrystalwell`, `scribes/ItemPrimewell`,
  `tools/ItemDiabolistFork`, `tools/ItemDragonslayer`, `tools/ItemMorphAxe`/`Pickaxe`/`Shovel`/`Sword`,
  `tools/ItemRidingCrop` (Lust-gated, like `baubles/ItemSubCollar`), `tools/ItemSkullAxe`,
  `tools/ItemTaintPickaxe`/`ItemTaintShovel`,
  `wands/ItemWandCores`, `wands/ItemWandCaps`, `wands/DarkWandCap`, `wands/ItemFocusBlink`,
  `wands/CreativeWandUpdate`, `wands/InfernalWandUpdate`, `wands/ProfaneWandUpdate`,
  `wands/TaintedWandUpdate`, `wands/YandereWandUpdate` (livingwood/dreamwood — vanilla TC wand
  types, not compat-gated).
- **Recipes and research**: `ForbiddenRecipes` (arcane/infusion/crucible recipes for everything
  above) and `ForbiddenResearch` (the `Apocrypha`/`FORBIDDEN` Thaumonomicon category and its full
  tree — currently nothing in this category exists yet, including the category itself).
- **Villager trading**: `VillagerHereticManager` + the Heretic villager profession/tab.
- **Mod glue**: `Forbidden` (main class/lifecycle — superseded here by `ForbiddenMagic.bootstrap()`,
  still needs an actual Fabric/NeoForge entrypoint), `FMEventHandler`, `WandOverlord` (wand block
  triggers for Hellfire), `Config` (real config file, only two static defaults ported so far),
  `LogHandler`, `XPReflectionHelper`.
- **Client**: `ClientProxy` (render registration) — no textures/models exist yet for anything.

## Out of scope (matches the sibling reforged port's own scope cut)

External-mod-gated content with no ported equivalent available in this repository: Blood Magic
(`compat/BloodMagic`, `ItemBloodRapier`, `ItemDivineOrb`, `scribes/ItemBloodwell`/`ItemBoundwell`,
`PotionBloodSeal`, `wands/BloodWandUpdate`/`BloodStaffUpdate`), Botania (`compat/ForbiddenBotany`,
the four `tiles/SubTile*` flowers, `wands/ManaWandUpdate`/`ManaStaffUpdate`), Ars Magica 2
(`compat/*` AM2 hooks, vinteum focus/witchwood AM2 recipe variants), CoFH (`compat/*`), and
`compat/DarkSignature`, `compat/RitualSanity`, `compat/ForbiddenLexicon` (Thaumic Tinkerer/other
addon-book hooks). Revisit only if/when this repository ports the underlying mod.

## Evidence

- `./gradlew :forbiddenmagic-common:test` — **PASS**, 19/19 (`DarkAspectsTest` 8/8,
  `ForbiddenMagicAspectTagsTest` 11/11), 0 failures/errors.
- `./gradlew :forbiddenmagic-common:build` — **PASS** (compiles, assembles and remaps a jar).
- `git status --short` after this checkpoint touches only `settings.gradle` (one `include` line
  added), `forbiddenmagic-common/` (new) and `docs/addons/forbidden-magic/` (new) — no file under
  `common/`, `fabric/`, `neoforge/`, or the root `build.gradle` was changed.
- Not run: `./scripts/dev.sh` (this repo's own core validate/build/smoke pipeline), any GameTest,
  any client/visual check. This checkpoint is common-module JUnit only.

### A real architectural constraint found and worked around (read before extending this module)

Architectury's `DeferredRegister` genuinely needs a live Fabric/NeoForge platform
(`architectury-fabric`/`-neoforge`'s `RegistrarManager` service) to do anything at all — not just
its own `register()`, but even `RegistrySupplier.get()` on an item that was never actually
registered throws (`NullPointerException: Registry Object not present: ...`), because it looks the
object up in the real game registry rather than memoizing the constructor call. A plain
`:forbiddenmagic-common:test` JUnit run has no such platform on its classpath (only `:fabric`/
`:neoforge` depend on `architectury-fabric`/`-neoforge`), so `ForbiddenMagicItems.register()`
cannot run there, and no `RegistrySupplier` from this module can resolve `.get()` either.

This is why `ForbiddenMagicAspectTags` exposes its aspect lists as plain `AspectList` constants
(`WRATH_SHARD_TAGS` etc.) independent of any `Item`, and the tests assert on those constants
directly rather than on `ThaumcraftApiHelper.getObjectAspects(new ItemStack(realItem))` — matching
this repo's own `common/src/test` convention of testing `ObjectAspectRegistry` against a throwaway
`Object` key instead of a real registered `Item` (see `ObjectAspectRegistryTest`). The real
`register()` path (building the actual `ItemStack`s and calling `ThaumcraftApi.registerObjectTag`)
is still there and is exactly what a future Fabric entrypoint will call; it is simply **not
exercised by any test yet** — that needs either a `forbiddenmagic-fabric` platform module running
inside an actual Fabric GameTest, or a `fabric-loader-junit`-style harness, neither of which exists
yet. Treat `register()` itself as implemented-but-unverified until one of those lands.
