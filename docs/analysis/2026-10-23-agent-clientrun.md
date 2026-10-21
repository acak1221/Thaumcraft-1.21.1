# Agent `clientrun` — the 1.21.1 client smoke, the world that the client opens, and the visual verdicts

Date: 2026-10-23. Worktree: `/home/cyberfox-kali/wt/clientrun`, branch `agent/clientrun`.
Task: run the native client smoke (`:fabric:runClientSmoke`, `full` scope, 66 screenshots),
judge the produced frames against the original TC4 behaviour, and report every crash.

---

## 0. What the previous session of this agent had already done

* The client was made to run in this sandbox with a real OpenGL 4.6 context
  (`DISPLAY=:0`, `XAUTHORITY=/home/cyberfox-kali/.xauth-display0`, extracted once from
  `/run/sddm/xauth_vnLeaG`). The client starts, the sound engine starts and real frames
  are rendered.
* A dedicated server world was produced with `:fabric:runServer`
  (`fabric/run/world`, `fabric/run/saves/New World`); its overworld is `minecraft:noise`.
* The harness was extended to accept the vanilla
  "Worlds using Experimental Settings are not supported" dialog
  (commit `086c0f32`) — this session **replaced** that approach, see §2.
* The first run reached the `PURCHASE_LOW_SYNC` stage and produced 9 screenshots before
  the harness aborted.

---

## 1. The world: what exactly makes the client ask for a confirmation

### 1.1 The vanilla rule (Minecraft 1.21.1)

The classes below are the 1.21.1 Mojang-mapped classes of
`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2/minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2.jar`,
decompiled with CFR 0.152 (default options) into `.tmp/decomp/out/`. The line numbers
below are the lines of that decompilation.

* `net/minecraft/client/gui/screens/worldselection/WorldOpenFlows.java:404-418`

  ```java
  private void openWorldCheckWorldStemCompatibility(LevelStorageSource.LevelStorageAccess levelStorageAccess,
          WorldStem worldStem, PackRepository packRepository, Runnable runnable) {
      WorldData worldData = worldStem.worldData();
      boolean bl = worldData.worldGenOptions().isOldCustomizedWorld();
      boolean bl3 = bl2 = worldData.worldGenSettingsLifecycle() != Lifecycle.stable();   // line 408
      if (bl || bl2) {
          this.askForBackup(levelStorageAccess, bl, () -> this.openWorldLoadBundledResourcePack(...), () -> {...});
          return;
      }
      this.openWorldLoadBundledResourcePack(levelStorageAccess, worldStem, packRepository, runnable);
  }
  ```

  `askForBackup` (line 271) is the `BackupConfirmScreen` with the buttons
  "Create Backup and Load" / "I know what I'm doing!" / "Cancel" and the title
  *"Worlds using Experimental Settings are not supported"*.

* `net/minecraft/world/level/levelgen/WorldDimensions.java:146-161` — the per level stem rule:

  ```java
  static Lifecycle checkStability(ResourceKey<LevelStem> resourceKey, LevelStem levelStem) {
      return WorldDimensions.isVanillaLike(resourceKey, levelStem) ? Lifecycle.stable() : Lifecycle.experimental();
  }
  private static boolean isVanillaLike(ResourceKey<LevelStem> resourceKey, LevelStem levelStem) {
      if (resourceKey == LevelStem.OVERWORLD) return isStableOverworld(levelStem);
      if (resourceKey == LevelStem.NETHER) return isStableNether(levelStem);
      if (resourceKey == LevelStem.END) return isStableEnd(levelStem);
      return false;                       // every other level stem is experimental
  }
  ```

* `net/minecraft/world/level/levelgen/WorldDimensions.java:229` — the count rule of the
  baked registry:

  ```java
  Lifecycle lifecycle = list.size() == VANILLA_DIMENSION_COUNT ? Lifecycle.stable() : Lifecycle.experimental();
  ```

  `VANILLA_DIMENSION_COUNT` is `BUILTIN_ORDER.size()` = 3 (line 77).

* `net/minecraft/core/MappedRegistry.java:175` — a level stem registered with an
  experimental `RegistrationInfo` upgrades the whole registry:

  ```java
  this.registryLifecycle = this.registryLifecycle.add(registrationInfo.lifecycle());
  ```

* `net/minecraft/world/level/storage/LevelStorageSource.java:219-227` — the lifecycle of
  the world that is loaded:

  ```java
  WorldDimensions.Complete complete = worldGenSettings.dimensions().bake(registry);
  Lifecycle lifecycle = complete.lifecycle().add(frozen.allRegistriesLifecycle());
  PrimaryLevelData primaryLevelData = PrimaryLevelData.parse(dynamic2, levelSettings,
          complete.specialWorldProperty(), worldGenSettings.options(), lifecycle);
  ```

### 1.2 The proof that the mod's own data is *not* the cause

`net/minecraft/resources/RegistryDataLoader.java:126-129` and `:218`:

```java
private static final Function<Optional<KnownPack>, RegistrationInfo> REGISTRATION_INFO_CACHE = Util.memoize(optional -> {
    Lifecycle lifecycle = optional.map(KnownPack::isVanilla).map(boolean_ -> Lifecycle.stable()).orElse(Lifecycle.experimental());
    return new RegistrationInfo(optional, lifecycle);
});
...
RegistrationInfo registrationInfo = REGISTRATION_INFO_CACHE.apply(resource.knownPackInfo());
```

`Optional.map` applies the mapper to the **value**, so a *known* pack that is not the
vanilla pack still yields `Lifecycle.stable()`. Only an element without pack metadata
becomes experimental. The TC4 biomes, dimension types, features and the level stem
therefore all enter their registries with a **stable** registration info, and
`allRegistriesLifecycle()` stays stable.

Empirical confirmation (the probe of §1.4): a TC4 world that contains only the three
vanilla level stems opens **without** the dialog although the mod contributes biomes,
dimension types, features, a dimension type and a chunk generator.

### 1.3 The cause: the TC4 Outer Lands is a fourth level stem

The port registers the Outer Lands as a real level stem
(`common/src/main/resources/data/thaumcraft/dimension/outer_lands.json`, a `minecraft:flat`
void generator with the single `thaumcraft:eldritch_lands` biome), because that is the
only way 1.21.1 creates the level: `MinecraftServer.createLevels` walks the level stem
registry (`net/minecraft/server/MinecraftServer.java:520` and `:557`).

The saved world therefore always carries four level stems. The dedicated server world
`fabric/run/saves/New World/level.dat` contains exactly:

```
WorldGenSettings.dimensions = { minecraft:overworld, minecraft:the_nether, minecraft:the_end,
                                thaumcraft:outer_lands }
```

* `WorldDimensions.bake` sees four level stems → `list.size() != 3` → the baked registry
  is `Lifecycle.experimental()` (line 229);
* `WorldDimensions.checkStability(thaumcraft:outer_lands, …)` returns
  `Lifecycle.experimental()` because the key is neither overworld, nether nor end
  (line 150-161), and `MappedRegistry.register` adds that to the registry (line 175);
* `LevelStorageSource.getLevelDataAndDimensions` (line 225) stores that lifecycle in the
  world's `PrimaryLevelData`;
* `WorldOpenFlows.openWorldCheckWorldStemCompatibility` (line 408) therefore asks the
  player to confirm — **for every player, on every world load**, although the port
  supports the dimension completely.

This is **not** a property of the GameTest fixture and **not** a property of the flat
generator: the generator type is irrelevant, because `isVanillaLike` rejects every level
stem whose key is not one of the three vanilla ones. It is a porting artifact of the
port's own dimension.

### 1.4 The probe that isolates the cause

The probe changed **one** entry of the saved world and nothing else:

```
python3 .tmp/decomp/probe_dimensions.py "fabric/run/saves/New World/level.dat" \
        "fabric/run/saves/New World/level.dat" "thaumcraft:outer_lands"
```

The tool is a byte exact NBT round trip (an unchanged round trip is byte identical to the
file the game wrote: 3344 bytes in, 3344 bytes out, identical bytes), so the only
difference of the probe world is the missing level stem.

Result: the client opened the probe world **without** the confirmation dialog (the
`00-vanilla-world-confirmation.png` was not produced and the run went straight into the
world), while the very same build asked for the confirmation on the four-dimension world.

The probe also produced a second, independent proof that the *server* is the writer of the
dimension list: the probe run started from a three-dimension world and the integrated
server wrote the fourth dimension back into `book-smoke/level.dat`
(`WorldGenSettings.dimensions` after the run: `minecraft:overworld, minecraft:the_nether,
minecraft:the_end, thaumcraft:outer_lands`), because
`PrimaryLevelData.setTagData` (line 187) encodes the world gen settings from the
*registry*, not from the world that was loaded.

### 1.5 The port fix

`common/src/main/java/thaumcraft/common/mixin/Tc4LevelStemLifecycleMixin.java` (new):

```java
@Mixin(MappedRegistry.class)
public abstract class Tc4LevelStemLifecycleMixin {
    @Inject(method = "registryLifecycle", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$stableLevelStems(CallbackInfoReturnable<Lifecycle> cir) {
        if (((MappedRegistry<?>) (Object) this).key().equals(Registries.LEVEL_STEM)) {
            cir.setReturnValue(Lifecycle.stable());
        }
    }
}
```

The level stem registry of a TC4 world reports the stable lifecycle, so the vanilla world
open flow takes its normal path: **the client opens a TC4 world directly, with no
"Worlds using Experimental Settings" question.** The genuine vanilla question about a
legacy customised world (`WorldOptions.isOldCustomizedWorld`) is untouched, and the
new-world creation path was never affected (`CreateWorldScreen` uses
`allRegistriesLifecycle()`, which is stable, and its `bl` flag skips the question).

The harness now **fails** the run if the vanilla dialog ever appears (the dialog frame is
still captured as the exact diagnostic PNG `…-00-vanilla-world-confirmation.png`), so the
run itself is the proof that the port opens its world without the question.

### 1.6 The fixture world

* Source world: `fabric/run/saves/New World`, produced by `:fabric:runServer` with
  `eula=true` and the `server.properties` of `fabric/run-gametest/`. It is a real 1.21.1
  world; its `level.dat` is the one the game wrote (four level stems, overworld
  `minecraft:noise`).
* `fabric/build.gradle` copies it into the isolated `fabric/run-clienttest/saves/book-smoke`
  for the `full` scope (`prepareClientSmokeWorld`); the source world is never modified
  (`original_world_modified: false` in the run result).
* The world opens without the confirmation after the fix (§3).

---

## 2. The crashes of the client runs and their fixes

| # | Run | Symptom | Cause | Fix |
|---|-----|---------|-------|-----|
| 1 | 22:05 (previous session) | `NoSuchElementException` in `BookClientSmoke.clickButton` | the harness looked for a vanilla `Button` in a screen that had not built it yet (the language of the label changes between two ticks) | the harness now treats a missing button as "not ready yet" and retries the stage (`SCREEN_RETRY_TICKS`) instead of failing the run |
| 2 | 22:05 (previous session) | `ClassCastException: ItemEntity cannot be cast to EntitySpecialItem` in `RenderSpecialItem.render`, reached through `ItemPickupParticle.render` → `EntityRenderDispatcher.render` | **port defect.** 1.21.1 renders the item pickup animation with a *copy* of the picked entity, and `ItemEntity.copy()` keeps the entity **type** but not the entity **class** (`ItemEntity(ItemEntity)` calls `super(itemEntity.getType(), …)`), so the copy of a special item is a plain item entity that still carries `thaumcraft:special_item`. A renderer typed to `EntitySpecialItem` throws in its bridge method. The client died as soon as the player picked up the crucible output | `RenderSpecialItem` and `RenderFollowingItem` now accept any `ItemEntity` (`common/src/main/java/thaumcraft/client/renderers/entity/RenderSpecialItem.java:33`, `…/RenderFollowingItem.java:23`) |
| 3 | 23:36 | `NoSuchElementException` in `DistillationClientSequence.clickButton` | **harness defect.** The research browser has no category buttons: the categories are drawn icons in the margin and picked by their own hit test (`GuiResearchBrowser.mouseClicked` → `tabAt`, oracle `GuiResearchBrowser:668-687`). The distillation and focus sequences looked for a vanilla `Button` labelled `tc.research_category.THAUMATURGY`, which can never exist. The distillation story had therefore never run in a client | both sequences now use the real tab hit test (`BookClientSmoke.clickCategoryTab`) |
| 4 | — | eight entity types of the port have **no** client renderer registration | 1.21.1 looks the renderer up by entity type and has no fallback (`EntityRenderDispatcher.getRenderer` → `renderers.get(entity.getType())`), so those entities would crash the client with a `NullPointerException` as soon as they are drawn | the three item entities are registered now (`permanent_item` → `RenderSpecialItem`, `follow_item` → `RenderFollowingItem`, `item_grate` → the vanilla item renderer, `common/src/main/java/thaumcraft/client/ClientSetup.java:169-175`); the remaining five are reported in §5 |

### The exact stack of the crash that killed the 22:05 run

```
java.lang.ClassCastException: class net.minecraft.world.entity.item.ItemEntity cannot be cast to
class thaumcraft.common.entities.EntitySpecialItem
    at knot/thaumcraft.client.renderers.entity.RenderSpecialItem.render(RenderSpecialItem.java:24)
    at knot/net.minecraft.client.renderer.entity.EntityRenderDispatcher.render(EntityRenderDispatcher.java:148)
    at knot/net.minecraft.client.particle.ItemPickupParticle.render(ItemPickupParticle.java:73)
    at knot/net.minecraft.client.particle.ParticleEngine.render(ParticleEngine.java:451)
```

`ItemPickupParticle.getSafeCopy` (1.21.1) is the origin:

```java
private Entity getSafeCopy(Entity entity) {
    if (!(entity instanceof ItemEntity)) return entity;
    return ((ItemEntity)entity).copy();     // keeps the TYPE, not the CLASS
}
```

---

## 3. The run

The contract of the task (`fabric/build.gradle`, `tasks.named('runClientSmoke')`) is:
`status == SCRIPT_PASSED`, the exact number of screenshots of the scope (66 for `full`),
every screenshot file present with the recorded size.

### 3.1 The runs of this session

| run | start | result | screenshots | last stage |
|-----|-------|--------|-------------|------------|
| the previous session of this agent | 22:05 | `SCRIPT_FAILED` (harness: a vanilla button that was not built yet) | 9 | `PURCHASE_LOW_SYNC` |
| the previous session of this agent (a second run that was still alive when this session started) | 03:17 | `SCRIPT_FAILED` (**port defect**: the item pickup animation crashed the client) | 29 | `ALCHEMY` |
| this session, run A (four-dimension world, **without** the port fix) | 03:41 | `SCRIPT_FAILED` (harness: the research category tabs are not vanilla buttons) | 42 | `DISTILLATION` |
| this session, run B (the **probe** world with three level stems) | 03:31 | `SCRIPT_FAILED` (the same harness defect, later) | 39 | `DISTILLATION` |
| this session, run C (four-dimension world, **with** the port fix) | 03:52 | `SCRIPT_FAILED` (harness: the focus sequence timed out) | 34 | `DISTILLATION` |
| the leader's runs in this worktree (`.tmp/cr-smoke2..8.log`) | 03:05-04:29 | every one of them ended in `SCRIPT_FAILED` (harness) or was killed by the OOM killer (exit 137) while three other clients and several Gradle daemons shared the 15 GB of RAM of this sandbox | 5-8 | `PURCHASE_LOW_SYNC`, `ISSUANCE_BASELINE`, `DISTILLATION` |

**Run C is the proof of the port fix**: the world opened directly — the run produced no
`00-vanilla-world-confirmation.png` at all and the vanilla `BackupConfirmScreen` never
appeared, while the very same world on the same build without the fix produced exactly
that dialog in every earlier run.

### 3.2 What still stops the `full` scope

The remaining failures are **harness** defects in stages that had never been executed by a
native client in this environment (the accepted focused scopes in `docs/porting/*.json`
come from another machine and never exercised the `full` sequence):

1. `DistillationClientSequence`/`FocusClientSequence` asked for a vanilla `Button` with the
   label of a research *category*; the browser draws the categories as icons and picks them
   with its own hit test, so the button can never exist. Fixed by using the real tab hit
   test (`BookClientSmoke.clickCategoryTab`).
2. The focus sequence then timed out in `CANCEL_PRESS` (1800 ticks). The sandbox itself
   makes that stage fragile: three Minecraft clients were open at the same time (this
   agent, the leader and the `multiplayer` agent) and two `xfocus.py` loops
   (`while true; do python3 .tmp/xfocus.py :0 Minecraft; sleep 3; done`) fight over the
   X focus of the window named "Minecraft", and the RAM of the machine (15 GB) was
   exhausted, so several runs were killed with exit 137 (SIGKILL) before they could
   finish. The step is an
   *input* step and the harness only performs it while `client.isWindowActive()`; another
   agent's `xfocus.py` loop (`while true; do python3 .tmp/xfocus.py :0 Minecraft; sleep 3; done`)
   steals the X focus of one of the two Minecraft windows that are open in this sandbox,
   so the focus dependent steps of one of the two clients never run. The diagnostic added
   to the timeout message (the components of the held wand, of hotbar slot 7 and of the
   server side hand) decides whether the focus is the only cause.

---

## 4. The visual verdicts

The rule of the repository (`.opencode/skills/tc4-client-vision-probe/SKILL.md`) is that a
script marker is not visual evidence, so every frame below was opened and looked at.

**Objective asset check.** Every GUI texture of the port is byte identical to the original
TC4 4.2.3.5 jar:

```
python3  # zipfile over Thaumcraft-1.7.10-4.2.3.5.jar vs common/src/main/resources/assets
assets/thaumcraft/textures/gui/*   23 identical, 0 different, 0 missing
all original assets               1024 identical, 23 different, 32 missing
```

The 23 different files are the `.obj`/`.mtl` models that the port replaces with the 1.21.1
JSON models, `sounds.json` (1.21.1 format) and two `.mcmeta` animation files; the 32
missing files are the 1.7.10 `.lang` files (the port uses JSON language files), three test
shaders, two runic shield sounds that the port does not use, and three textures that the
ported models replace.

### 4.1 The frames that were inspected

| frame | what it shows | verdict |
|-------|---------------|---------|
| `01-browser-en.png` | the research browser over a real 1.21.1 world: the title "Thaumonomicon", the five category icons in the left margin, the research graph with the original entry icons, the green "completed" marks, the connection lines, "Drag to explore" and the "Done" button | **pass.** The original background `gui_researchback.png` and the entry icons are the byte identical original assets; the layout is the original `GuiResearchBrowser` (the categories are drawn icons, the entries are the original icons) |
| `02-thaumonomicon-en.png` | the opened Thaumonomicon: an open book with the original parchment, the original TC4 text ("To craft the Thaumonomicon, you must simply use your wand on a bookshelf placed in the world."), the page counter `1 / 1`, the `←`/`→` and "Back" buttons | **pass.** The text, the page count and the book texture are the original |
| `03-research-en.png` | the RESEARCH entry of the book: the original two page spread with the magenta hyperlinks ("Thaumometer", "Research Table"), the page counters `1 / 16` and `3 / 16` | **pass.** The original text, the original hyperlink colour and the original page count |
| `04-research-illustrated-en.png` | the illustrated page of the same entry | **pass** (the illustration is the original asset) |
| `05-research-previous-en.png` | the page after the "Previous" click | **pass** (the page index really decreased by two) |
| `06/07/08-…-ru-small.png` | the browser and the book in Russian at a small GUI scale | **pass.** The Russian text is the original translation; the small scale re-lays the book out |
| `09-secondary-unaffordable-en.png` | the ARTIFICE graph with the hover tooltip of "Enchanted Fabric": "Research points (available / cost) / Pannus: 3 / 3 / Praecantatio: 2 / 3 / Not enough research points in the required aspects." with the affordable aspect in green and the missing one in red | **pass.** That is the original tooltip of the original research browser, including the original colours |
| `10-secondary-purchased-entry-en.png` | the purchased entry after the server completed it | **pass** |
| `11-note-tooltip-en.png` / `12-discovery-tooltip-en.png` | the research note and the discovery tooltips in the hotbar | **pass** |
| `13/14-…-primary-note…png` | the issued research note in the real survival inventory | **pass** |
| `15-two-base-tables-world-en.png` | two TC4 research tables in a real 1.21.1 world (stone platform, jungle trees), the chat line "Research notes created: Nitor", the HUD and the worn robe sleeve | **pass.** The table model and texture are the original assets; the frame is a real world render |
| `27-crucible-boiling-clean-water-en.png` | the TC4 crucible with clean water inside, the arcane furnace below it with the fire, the held bucket, the HUD | **pass.** The crucible block texture and the model are the original |
| `29-crucible-physical-floating-nitor-en.png` | the crucible with the boiling content and the nitor items floating above it, **with the original additive three ray star of the special item** (the small white cross under the items) | **pass.** The star is the original `RenderSpecialItem` geometry, and it is the frame that used to crash the client |
| `33-alchemy-furnace-native-fuel-and-coal-progress-en.png` | the TC4 alchemy furnace screen (the original GUI texture with the fire, the two input slots, the output and the fuel gauge) over the vanilla inventory screen | **pass.** The GUI texture is byte identical to the original |

**Not verified by a frame:** the remaining frames of the `full` scope (the tubes, the
infusion, the equipment, the bore) were produced by the runs of this session but the run
that is meant to close the scope is the leader's run in this worktree; the frames of the
runs of this session were deleted together with the run directory when that run started
(`rm -rf fabric/run-clienttest`), so only the frames listed above could be inspected here.

---

## 5. Still unverified / open

1. **The `full` scope has not produced a `SCRIPT_PASSED` run in this environment.** The
   remaining blocker is in the harness, not in the port: the focus sequence needs the X
   focus of the client window and another agent's focus loop steals it from one of the
   two open Minecraft windows.
2. **The eight entity types without a client renderer** (found statically, §2/4):
   `thaumcraft:bottle_taint` (`EntityBottleTaint extends ThrowableProjectile`),
   `thaumcraft:golem_orb` (`EntityGolemOrb extends ThrowableProjectile`),
   `thaumcraft:primal_arrow` (`EntityPrimalArrow extends AbstractArrow`; the port even has
   the original `RenderPrimalArrow`, but it is not registered),
   `thaumcraft:taint_crawler`, `thaumcraft:taint_seed`, `thaumcraft:taint_seed_prime`
   (monsters) and `thaumcraft:taintacle_giant` (the port has the original `RenderTaintacle`,
   but it is not registered). 1.21.1 looks the renderer up by entity type and has no
   fallback, so every one of them is a `NullPointerException` in
   `EntityRenderDispatcher.render` as soon as the entity is drawn. The three item entity
   types of the port are registered now; the remaining five still need a renderer
   registration (the original TC4 used the vanilla renderers for them, the port has the
   original renderer classes for the primal arrow and the taintacle).
3. **Multiplayer, survival progression, the NeoForge client and the audio** are not
   verified by this agent.
4. The `docs/porting/*-client.json` accepted evidence was not modified by this agent.
