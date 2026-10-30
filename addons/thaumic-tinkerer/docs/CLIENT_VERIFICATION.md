# Client verification of the add-on

The server side of the add-on is covered by JUnit tests and by GameTests on a Fabric dedicated server. None of that loads the
client: block and item models, textures, translations, screens, block-entity renderers and the HUD are not touched by it. The
**client smoke harness** closes that gap as far as the development machine allows: it starts a real Minecraft 1.21.1 client with
Thaumcraft 4 Reload and the add-on, under Xvfb with Mesa software rendering, looks at the resources, builds a world, takes pictures
and writes a report. It is a development tool: it is never part of a release jar (see "Wiring").

What it produces is **evidence of what a software renderer drew and what the game's own lookups answered**. It is not an
acceptance by a person looking at the game on a GPU; the list of what it cannot show is at the end and is part of the document.

## Running it

```bash
addons/thaumic-tinkerer/tools/client-smoke.sh                        # whole run, about 2 minutes after the first compile
addons/thaumic-tinkerer/tools/client-smoke.sh -l /path/gradle.log    # where the Gradle output goes (default $TMPDIR/tt-client-smoke-gradle.log)
addons/thaumic-tinkerer/tools/client-smoke.sh -t 600                 # watchdog limit in seconds (default 1500)
```

The script runs `:fabric:runTinkererClientSmoke` through `/root/bin/gradle-mirror-loop.sh` (which takes the shared Gradle lock
itself: do not wrap the script in `flock`, and expect it to hold the lock for the whole run) inside
`xvfb-run -a -s "-screen 0 1280x720x24"` with `LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe`. It needs the Minecraft assets in the
Loom cache (`:fabric:downloadAssets`), `xvfb-run` and the Mesa software drivers; there is no GPU, no window manager and no audio device
in the environment it was written for.

The run directory is `fabric/run-tinkerer-clientsmoke/` (git-ignored; its `options.txt` is rewritten every run: English, GUI scale 2,
view distance 8, no sound). The world is made by the harness itself and deleted first: nothing depends on a prepared save.

**Exit code.** The script exits 0 only when the harness itself reached its last phase (`harness.status` is `COMPLETED` in `report.json`; the Gradle task checks it
and fails otherwise). What the checks found is in `report.json`, never in the exit code: a run that found thirty defects exits 0. A hang ends with the watchdog
(`TIMED_OUT`, partial report, `Minecraft.stop()`, and a JVM halt with exit code 3 if the game is still running ten seconds later); a crash or a kill leaves `ABORTED`
(shutdown hook) and the crash report in `crash-reports/`. In all three cases the task fails and the script exits non-zero.

## Output

Everything is in `fabric/build/tinkerer-client-smoke/` (deleted at the start of every run):

| File | What it is |
|---|---|
| `report.json` | the result: see below |
| `screenshots/*.png` | the pictures, 1280x720; `screenshots/INDEX.md` lists them with one sentence each |
| `client-log-excerpt.txt` | every WARN/ERROR/FATAL line of the client log that names the add-on, a missing model/texture/translation or an exception, with the stack lines that follow; classified `addon`, `resource`, `exception` or `environment` |
| `gradle.log`, `client-latest.log`, `crash-reports/` | copies made by the script, for the case that something went wrong |

### report.json

* `harness`: `status` (`RUNNING`, `COMPLETED`, `TIMED_OUT`, `ABORTED`), start and end time.
* `summary`: the number of checks per result and `failures_by_group` (content group, then defect kind, then count).
* `checks[]`: one entry per result: `phase`, `name`, `group`, `target`, `result`, `detail`.
  * `result` is `PASS`, `FAIL` (a defect of the port or of a resource it ships), `HARNESS_ERROR` (the check or the harness threw or timed out: suspect the
    harness first, the detail has the stack), `NOT_RUN` (skipped on purpose, the detail says why) or `INFO` (a number or a fact; never a failure).
  * `name` is the kind of defect for a `FAIL`: `missing_model`, `missing_blockstate`, `missing_texture`, `missing_translation` (en_us: the player
    sees the raw key), `missing_translation_ru_ru` (the game falls back to the English text), `screen_missing`, `screen_not_opened`, `screen_exception`,
    `renderer_missing`, `renderer_exception`, `item_render_exception`, `tooltip_exception`, `missing_sound_definition`, `missing_sound`, ...
  * `group` is the content group (`SimpleBlocks`, `TileBlocks`, `Machines`, `BasicItems`, `Foci`, `Enchantments`, `KamiGear`, `KamiWorld`) of the registry path in
    `target`, or `Research`, `Core`, `ForeignInTab`, `Unassigned`. The group of a path is decided by `GroupIndex`: the path constants that the registrar
    `content/<Group>.java` passes to `TTRegistry`, and for a computed path (`infused_grain_aer`, `mob_aspect_*`) the group registered just before it;
    `facts.group_assignment` says which rule gave which group.
  * `PASS` lines exist for the things that were looked at and found right; the checks that found nothing wrong still say what they looked at.
* `inventory`: the lists of what was looked at: `blocks`, `items` (as `Group:path`), `block_entity_types`, `block_entity_types_without_renderer`,
  `renderer_classes_in_sources`, `menus`, `enchantments_from_data`, `research_keys`.
* `facts`: `group_assignment`, `tooltips` (the tooltip lines of every stack), `item_grid_cells`.
* `screenshots[]`: `name`, `group`, `shows`, the screen class, and a colour measure (`blank` is true when 99.5 % of the pixels have one colour: nothing was drawn).
* `suspicious_log_lines`: the same entries as `client-log-excerpt.txt`.

## What it checks

The phases run in this order. Each check is its own small class and is isolated: if one throws, it is recorded as `HARNESS_ERROR` and the next one runs.

**Resources (title screen, no world)**

| Check | Looks at |
|---|---|
| `CanaryCheck` | that the checks can fail: an unknown model must come back as the missing model, an unknown sprite as missingno, an unknown key as not translated |
| `BlockModelsCheck` | every state of every block: blockstate file present; baked model (`BlockModelShaper`) is not the missing model; particle sprite is not the missing-texture sprite |
| `ItemModelsCheck` | every item: the model `ItemRenderer.getModel` picks for the default stack is not the missing model, its particle sprite is real |
| `OverridesCheck` | the item-model overrides: every predicate an item model names has a property function registered for the item, and each state of `ItemVariants` (a Kami tool in mode 1 and 2, an enabled talisman, an active Black Hole ring, an activated blood sword) makes the item renderer pick another model than the default stack's; the six infused seeds (one per primal aspect) must each get a model of their own (the first has the default stack's model: a seed without an aspect reads the predicate as 0) |
| `ModelTexturesCheck` | the JSON of every model reached from a blockstate, an item or an override, parents followed: parent files exist, every declared texture has a file and is in the block atlas, every `#variable` a face uses is defined; unreferenced model files; the atlas sources of `atlases/blocks.json` |
| `TranslationsCheck`, `SourceKeysCheck`, `ResearchTextCheck` | the keys of item and block names (the key the game asks for), mob effects, entities, the creative tab, the enchantment descriptions of the data files, every `ttmisc.*`-style key that a source file names as a literal, the research tab, `tc.research_name/text.<KEY>` and the page texts of every research the add-on registered, and the research icon and image files: in en_us through `Language.has`, in ru_ru by comparing the language files |
| `ArmorAndEffectTexturesCheck` | the worn-armor layer each armor item draws (outer, inner for leggings) and the status-effect icon of each mob effect: neither is in any model |
| `SoundsCheck`, `ParticlesCheck` | `sounds.json` and the registered sound events, particle definitions; "nothing to check" is stated when the add-on has none |
| `RenderersCheck` | the renderer the game picks for a block entity of every type; every `RenderTile*` class of the sources must be the renderer of some type (a class whose javadoc says 1.7.10 never registered it is reported as a known omission) |
| `ScreensCheck` | every menu type has a screen factory |
| `GuiTexturesCheck` | every `LibResources` path exists |

**World** (a fresh flat creative world made by `WorldPhase`: seed fixed, no structures, peaceful, noon, no weather, no mobs, no fire or random ticks)

| Phase | Does |
|---|---|
| `BlockRowsPhase` | every block, in rows by group, six cells per picture; each cell one representative state (the default and every other value of every property; a property with more than three values gives its first, middle and last), a name tag over each cell; the interface is hidden. Each cell is compared with the empty stage seen by the same camera (`Visibility`): a block that draws nothing is recorded as `block_not_visible` (an `INFO`: for a gas or an empty camouflage that is intended, for anything else it is a defect) |
| `ItemGridPhase` | every item on an inventory-like screen (`ItemGridScreen`: `renderItem` and the item decorations at twice the size, path and translated name under each icon), pages by group; the stacks are the ones of the add-on's creative tab, the items the tab omits and a Mob Display for every mob. Every page is drawn once into a throw-away `GuiGraphics` first, so that an item whose renderer throws is named instead of ending the game. The tooltip of every stack is built and searched for keys without text |
| `MenusPhase` | every menu opened for real: the server-side player right-clicks the block (or uses the item) that opens it, the screen has to be a container screen of that menu type, drawing it once as a guard must not throw, nothing suspicious may reach the log (`LogWatcher`); then a picture. The way each menu is opened is one table in the class; a menu type it does not know is `NOT_RUN`. The warp gate's destinations screen is opened by sneaking above the gate |
| `RendererPhase` | a close-up of every block entity that has a renderer, in a state that makes it draw something (an item in the tablet, the funnel and the repairer, a wand in the enchanter, camouflage); the renderer is called once on its own first, so an exception names the block |
| `CreativeTabPhase` | the creative inventory with the add-on's tab selected (the tab's title is where a missing `itemGroup` key shows) |
| `HudPhase` | the Dislocation readout (a wand whose focus carries a chest) and the soul hearts; the tool-mode tooltip is `NOT_RUN` |
| `LanguagePhase`, then `ItemGridPhase` and `MenusPhase` again | the game is switched to ru_ru through the `LanguageManager` and the resources are reloaded; the item pages and the menus are taken again (`-ru_ru` pictures). A name that is still English there is a gap in the translation |

Last, `FinishPhase` reads the client log, writes `client-log-excerpt.txt`, `report.json` and `screenshots/INDEX.md`, and ends the game with `Minecraft.stop()`.

How a picture is taken: `Screenshot.grab` reads the window's frame buffer from the client tick, which runs between two frames, so the buffer holds the last complete
frame. A step waits for the chunk sections to be built and for a number of rendered frames (`Frames`; the software renderer draws far fewer frames than the tick
runs) before it asks for the picture.

## Wiring

* Source set `thaumicTinkererClientSmoke` (`fabric.gradle`): Java in `addons/thaumic-tinkerer/fabric/src/clientSmoke/java`, its own `fabric.mod.json` (mod id
  `thaumictinkerer_clientsmoke`, client only, depends on `thaumictinkerer`). Compiled by `check`; no jar task includes it.
* Run `tinkererClientSmoke` (`./gradlew :fabric:runTinkererClientSmoke`): a client with a 1280x720 window, run directory `run-tinkerer-clientsmoke`, the add-on and the harness
  as two classpath groups (like the GameTest run). `prepareTinkererClientSmoke` resets the options and the result directory first. The watchdog limit is the Gradle property
  `tinkererClientSmokeTimeout` (seconds; the script's `-t`).
* `fabric/build.gradle` (`configureTestIsolation`): `tinkererClientSmoke: 'thaumicTinkererClientSmoke'` was added to the map of the isolated runs, so that the isolation check
  verifies this run uses that source set and the classpath groups, like the GameTest run.
* The harness imports Minecraft, Fabric, Architectury, the Thaumcraft API and the add-on only.

## Extending it

* A new check of the resource phase: a class implementing `SmokeCheck`, added to the list in `ResourcesPhase`.
* A new menu: one line in `MenusPhase.HOW`. A new block entity that needs a state for its renderer: one line in `RendererPhase.SETUPS`.
* A new item-model override: its predicate is looked up without help; to see it switch, one line in `ItemVariants` (the add-on's own setter or component for that state).
* A new block, item, menu, block entity or enchantment needs nothing: they are read from the registries. A new group needs its name in `GroupIndex.GROUPS`.

## What it cannot show

* **No human.** A `PASS` says a lookup answered or an exception did not happen; it does not say the thing looks right. The pictures are for a person (or a model) to read afterwards,
  and the visibility measure is a number, not a judgement.
* **No real GPU.** Mesa llvmpipe renders everything: shader, blending, mipmap, anisotropy, translucency sorting and animated-texture timing can differ from a GPU, and a
  driver-specific bug cannot appear. The frames are slow, so nothing about frame-rate or time-based motion is learned.
* **No sound output.** OpenAL fails to open a device; sounds are checked only as files (`sounds.json`, `.ogg`), never heard.
* **No input devices.** Nothing is clicked, dragged, typed or scrolled: screens are opened and drawn, their buttons are not pressed, slots are not clicked, keys are not
  bound and pressed (the sneak key is set programmatically to open the warp gate screen). The scroll, drag and click handling of the screens is untested.
* **No multiplayer.** One integrated server, one client in the same process; packets are real but in-memory. A dedicated server, a second client or a desync is not covered.
* **One setting.** 1280x720, GUI scale 2, view distance 8, English (the Russian pictures are a repeat after a language switch), one flat peaceful world at noon, the default
  field of view. Other GUI scales, resolutions, languages (the de/fr/ko/zh files are not compared), resource packs and other mods (EMI, JEI, Jade) are not seen.
* **Default states only.** Items are shown as the default stack of the creative tab (damaged and stacked copies for the decorations); the model overrides are resolved at their default
  values plus the states of `ItemVariants`; a branch that is not in that list is checked only as a file and by its predicate name. Blocks are shown in representative states, the
  block entities in the few states of `RendererPhase`. Animated textures are seen in one frame.
* **Not visited.** The Bedrock dimension and its portal in use, the Thaumonomicon pages of the add-on's research (only their keys are checked), entities (none are registered), the
  wings layer and the Placement Mirror's ghost blocks (Fabric glue that needs the items in use), the Kami tool-mode tooltip, chat messages, the commands, particles over time.
* **Fabric only.** NeoForge is not started by this harness.
