## 2026-10-26 — TC4Tweaks wave 1: four gameplay improvements as settings

At the user's request, TC4Tweaks (AGPL-3.0; ideas only, no code copied) was
triaged against the port. Four improvements are adopted, each a setting in the
new Tweaks tab, on by default like in TC4Tweaks and documented as an accepted
deviation (KNOWN_DEVIATIONS §8): varied loot (books and vis amulets rolled per
draw), earth shock that spares items and experience, infusion that keeps the
centre armour or tool's enchantments, name and other data, and a filled phial
(or alembic jar) that stays in the emptied hand. The exploit fixes of TC4Tweaks
are already closed in the port; Thaumonomicon search, completion counter and the
notification clear button are wave 2. Verified: 2577 JUnit, both builds,
471/471 Fabric and 456/456 NeoForge GameTests on fresh worlds (every setting
both ways), 115 Python tests; the tab was inspected in English and Russian. See
[the report](docs/analysis/2026-10-26-tc4tweaks-wave1-ru.md).

## 2026-10-26 — real NeoForge client: two defects that broke every client; EMI on NeoForge

First run of a real NeoForge client. Two shared-code defects broke every
NeoForge client: `TaintOverlayChunkLayerMixin` selected NeoForge's delegating
`SectionCompiler.compile` and crashed on joining a world (now `compile*`; a Mixin
audit then applied every target), and `ClientSetup.init()` ran from
FMLClientSetupEvent, after the events in which Architectury registers menu
screens, model layers and entity renderers, so all 17 GUIs, 8 gear layers and
every entity renderer were lost. The setup now runs from the first client hook
event, pinned by a loader parity test. The EMI plugin moved to a shared
`compat/emi` source folder and works on both loaders. The relic GameTest fixture
now sets its own biome, since a natural taint biome legitimately grows no
fibres. Verified: 2571 JUnit, 13 NeoForge JUnit, both builds, 467/467 Fabric
and 452/452 NeoForge GameTests on fresh worlds, 115 Python tests. Not run: NeoForge multiplayer. See
[the report](docs/analysis/2026-10-26-neoforge-client-ru.md).

## 2026-10-26 — NeoForge GameTest parity and three NeoForge-only defects

NeoForge built and ran on the current shared code for the first time since the
content waves. Its GameTest catalogue no longer compiled and covered 307 of 469
wrappers; it is now regenerated from Fabric by
`scripts/porting/neoforge_gametests.py`, and a Python test catches drift. The
17 wrappers on Fabric-only APIs (Transfer API, Fabric loot and protection events)
are skipped and their NeoForge counterparts stay untested. Harness: three check
classes moved out of mod packages (JPMS split packages; four mod methods became
public), disk-reload checks wait for NeoForge's asynchronous SavedData writes,
sounds.json is read through the mod's class. Shared-code fixes: the Primal
Crusher no longer charges and breaks the centre block a second time on NeoForge;
removing an advanced alchemical furnace part at a chunk edge no longer loads the
unavailable neighbour chunk through NeoForge's comparator notice; wand block
triggers run outside NeoForge's block snapshot capture, as the original's
onItemUseFirst did, so the Oculus keeps its dark node when a neighbour undoes
the gate. NeoForge also registers the config screen. Verified: NeoForge build,
12 NeoForge JUnit, 452/452 NeoForge GameTests and 467/467 Fabric GameTests on
fresh worlds, 2571 JUnit, 115 Python tests. Not run: a real NeoForge client,
EMI on NeoForge (no plugin yet), multiplayer. See
[the report](docs/analysis/2026-10-26-neoforge-gametest-parity-ru.md).

## 2026-10-25 — 1.21.1 integrations: EMI, Mod Menu, config screen, tag translations

Optional EMI plugin in the Fabric module: arcane workbench (every rod x cap the
registered wand/sceptre recipes accept), crucible, infusion incl. runic
augmentation, infusion enchantment and aspect combinations; aspects are EMI
stacks and "+" fills the workbench grid. Mod Menu opens the config screen, now
rebuilt on 1.21.1 widgets (scrolling list, readable names, tooltips; file format
unchanged). Tag translations silence the Fabric/EMI warnings, a narrow mixin
removes the 55 `No data fixer registered for thaumcraft:*` errors, and the Shock
focus GameTest fixture declares its champion roll. Verified: 2571 JUnit, Fabric
build/isolation, 467/467 Fabric GameTests on a fresh world, 112 Python tests and
the asset check; a real client with EMI and Mod Menu was inspected frame by
frame. Ledger check (needs the private oracle) and NeoForge were not run. See
[the report](docs/analysis/2026-10-25-emi-modmenu-config-ru.md).

## 2026-10-25 — shared wand and thaumometer framing

Restore the missing original caller transforms, fit/center GUI icons, mirror the
left view and suppress only the scanner's duplicate empty-hand arm. Verified
2571 common JUnit,22 NeoForge JUnit, both builds and inspected both physical clients.
See [the framing report](docs/analysis/2026-10-25-item-framing-ru.md).

## 2026-10-25 — 0.1.0-alpha.6: review fixes and Fabric release preparation

The gameplay fixes in `61c8a3c6` restore mirror links and pedestal resource
accounting, addon recipe hooks, the wand HUD/depth layers, canonical champion
identity/attributes, worldgen policies, bucket transactions and Oculus rollback.
Maze cell serialization now has a stable order. The follow-up review report
records every candidate, including original quirks and retained compatibility code.

Verified gameplay inputs: 2561 JUnit, 467 Fabric GameTests on a fresh world and 112
Python tests; Fabric build/isolation, asset and ledger checks passed. The alpha.6
release changes only the version metadata and documentation after that checkpoint.
Its package is rebuilt locally; the release verification compares every JAR entry
with the accepted gameplay build. No NeoForge build, physical client inspection
or new multiplayer acceptance is claimed. Release notes are in
`docs/RELEASE_NOTES_0.1.0-alpha.6.md`.

## 2026-10-23 — Repository audit: ledger closed, integrated acceptance still pending

At base `c6feeda7`, recomputing every ledger unit gives **1044/1044
`parity_verified`, 0 partial/missing/not_assessed**. The status documents' leading
1017/27 checkpoint (and older 976, 969, 945 totals) is superseded. This is ledger
code closure, not proof of a fresh green build or visual acceptance.

The last integrated numbers recorded in the wave-4/5 status summary are 2384
JUnit, 325 Fabric GameTest and 90 Python, on earlier inputs. Wave-7/8 reports
record javac/resource checks, **not executed JUnit/GameTests or client frames**.
The wave-3 client smoke reached 66/66 frames (65 accepted, last-frame fix after
the run); two physical clients reached 37/37 stages. Neither accepts subsequent
renderer/input changes. NeoForge visual acceptance remains unperformed. This
audit ran no Gradle or game; the `a3tests` lane owns the new integrated gates.

Later reports supersede the earlier mirror, particle-budget, scattered-feature
and chunk-replay impossibility claims. Ore inset is controlled by anisotropic
filtering, not Fast/Fancy or particle settings. The old font atlas is absent,
not a verified extraction. See `docs/audit/REPO_FINDINGS.md` for exact arbitration
and hygiene findings, and `THIRD_PARTY_NOTICES.md` for checked asset provenance.

**Everything below is historical evidence on its stated inputs.** Old test
counts, remaining-unit counts, claims that the client cannot run, and pending
acceptance statements are not the current state. They are retained rather than
rewritten as tests that never happened.

## 2026-10-21 — Closing wave: the client lanes of the scope ledger

The OpenTC4 comparison left 126 units of the scope ledger without a native
counterpart. The largest block of them was the client lane, and this wave closed
45 of the 49 units of the client renderers and 12 of the client fx, gui and jei
units.

The client render lane gained the renderers the port never had: the cultist
banner item (its colour and its aspect were lost, the model pointed at a wooden
cube), the three dimensional thaumometer scanner of the original
(`scanner.obj`) and the glowing faces of the levitator. The banner block
renderer of the port was corrected as well: its height, its rotation, the cloth
colour, the tab colours, the palette of `Utils.colors` and the 16x16 aspect
icon.

The client fx lane found a false gap: vanilla 1.21.1 DOES ship the post effect
programs, and the warp chains failed to load only because a post program exists
in the default namespace alone while the passes were named
`minecraft:box_blur` and `thaumcraft:bloom2`. Three TC4 programs were ported to
GLSL 150 under `assets/minecraft/shaders/program/`, the chains were renamed, the
original `Config.shaders` came back and a regression test now pins it. The smoke
spiral of the original was ported with its 720 degree turn, its polar angle, its
radius, its 0.15 quad and its 0.66 alpha, and the additive power flash of the
original was added.

`scripts/porting/merge_wave_branch.py` merges a wave branch and resolves the two
bookkeeping files by the union of the two sides, so the agents of a wave can
update their own lane of the ledger without colliding.

Verified in the integrated tree: the JUnit suite, `:fabric:compileClientJava`,
the 84 Python tests and `convert_assets.py --verify` with
`missing_references: 0`. The client itself does NOT run in this sandbox: the
GLX context cannot be created with any Mesa driver, so no visual acceptance is
claimed.

## 2026-10-21 — The warp post chains load again, and the client effects of the wave are closed

The previous wave closed the four warp post effects as an impossible gap ("vanilla 1.21.1 ships no
post program at all"). Both halves of that claim were wrong. Vanilla 1.21.1 does ship post programs
(`blit`, `box_blur`, `color_convolve`, ... in `assets/minecraft/shaders/program/`), and the real
reason the chains never loaded was the resolution rule of
`net.minecraft.client.renderer.EffectInstance`: a post program only exists in the DEFAULT
namespace, so a pass named `minecraft:box_blur` or `thaumcraft:bloom2` is looked up as
`minecraft:shaders/program/minecraft:box_blur.json` and can never be found. The four native chains
now name their programs plainly, and the three original TC4 programs (`bloomColor`, `bloom2`,
`color_convolve2`) are ported to GLSL 150 into `assets/minecraft/shaders/program/` with the
original pass order, uniforms and numbers. The original switch `Config.shaders` came back as a
local setting, and `WarpPostChainTest` fails when a pass names a program that neither the port nor
1.21.1 can resolve.

The wave also ported the smoke spiral of the Sword of the Zephyr and of the spawning eldritch
warden (`FXSmokeSpiral`, with the original 720 degree turn and the first five cells of the particle
sheet), added the missing additive flare of the visnet relay beam, and replaced the Forge
configuration screen of the original with a native screen that keeps its five categories and its
keys over the same properties file.

The ledger verdicts of nineteen client rows were corrected against the oracle. Two of them were
false "missing" verdicts of the audit wave: `FXBeam` and `FXBeamGolemBoss` are dead code in the
original, because the packets that would show them are registered in the original packet handler
but never sent by any class of TC4 4.2.3.5. The JEI plugin stays excluded: the original has no JEI
integration at all and the 1.21.1 JEI is an external mod with its own API artifact.

Verified in the integrated tree: 1781 JUnit tests with no failure, `:fabric:compileClientJava`
successful, the ledger and the class inventory consistent. No client run: none of the ported
effects was inspected in the game, so every visual verdict stays partial.
The report is `docs/analysis/2026-10-21-close-clientfx.md`.

## 2026-10-21 — The world generation gaps of TC4 4.2.3.5: the mound, the village traders and the aura totem

The domain report `docs/analysis/2026-10-21-openTC4-worldgen.md` had left the world generation
of the overworld with seven declared gaps. This wave closed them against the oracle.

The **mound** of the original (`WorldGenMound.java:29-2550`, one chunk in 150) had no
generator at all: the roll of `StructurePlacementRules` ended without a structure. It is now
ported. The original writes a fixed nineteen by nineteen by sixteen figure with 2441 explicit
block calls, so the native port keeps that shape in a layer table; the table was rebuilt from
the oracle with `scripts/porting/extract_mound_template.py` and compared cell by cell with the
oracle (sha256 `93a2b6f8b8b1722733d5a005d02e25b92d0e2ba28d612b7f69409c5463a65433`, 2441 blocks,
sixteen layers, no difference) and with the 1.12 reference of this repository (no difference
either). The loot room, the chest with the dungeon loot, the two mob spawners and the dark
aura node over the centre are written in the order of the original. The ledger unit
`thaumcraft/common/lib/world/WorldGenMound.java` leaves `needs_review` for `port` with
`parity_verified`; **no unit of the ledger is in `needs_review` any more**.

The **village traders** now belong to a profession. The oracle gave the banker home a villager
of the profession `ConfigEntities.entBankerId` and the managers added their recipes to the
trade list of every villager of that profession; the native tree had the tables as data and no
profession at all. The port registers `thaumcraft:banker` and `thaumcraft:wizard`, sets the
profession of the spawned villager and adds the tables in `Villager#updateTrades`. Two more
defects of the tables fell out of the new check that compares every item of both tables with
the item registry: the wizard table named `thaumcraft:chicken_nugget` and
`thaumcraft:beef_nugget`, which are not registered, and the six aspect shards
`thaumcraft:aer_shard` and friends, while the registry carries `thaumcraft:nugget_chicken`,
`thaumcraft:nugget_beef` and `thaumcraft:air_shard` and friends. The price of the book of the
banker is seven to nine coins (`VillageBankerManager:47`, `7 + nextInt(3)`); the previous audit
had shortened it to seven to eight by misreading the oracle.

The **top block of the biome** is a table of its own now (`BiomeTopBlocks`), because 1.21.1 has
no `topBlock` in the biome document; the hilltop stones fill their ground with it instead of
with the surface block of the middle column, and their list of replaced flowers is the ten
flowers of the 1.7.10 blocks again.

The **aura totem** was checked against `ThaumcraftWorldGenerator:148-187` and fixed: the
original puts the cosmetic solid of the metadata eight at the place of the node before it asks
for a node, and the native port put nothing there, so its totem had no node at all, while its
second branch created a node where the original has none. The jar of the original settles one
question: `BlockCosmeticSolid` extends `Block` and not `BlockContainer` (`javap` over
`Thaumcraft-1.7.10-4.2.3.5.jar`), so its `createTileEntity` is dead code and the totem of
4.2.3.5 never carries a node. The native cosmetic solid follows the 1.12 reference, which
fixed that, so the native totem carries the dark node the original asked for; the deviation is
documented in the report.

The claim that the `place()` of the village components is never called stays refuted: the game
test builds the house and the tower in a generated chunk. The traders of the houses do reach
the world with the profession of the original and with the trade table of the original.

Verified in this tree: 1779 JUnit tests with no failure, and the three domain game tests
`surfacestructuresreachtheoverworld`, `villagecomponentsreachtheoverworld` and the new
`villagetradesreachthevillagers` pass. The full Fabric set is 313 scenarios with the four
known unstable cases and no new failure. No client run, no multiplayer and no full survival
progression.

## 2026-10-21 — The OpenTC4 comparison wave: nine agents, sixty-two defects

The readable 1.7.10 sources settled what the SRG names of the oracle could not: the mapping of
`DamageSource.field_76367_g` is the cactus source and `field_76376_m` is the magic source in
1.7.10, while the 1.12 line of this repository resolves the same fields to IN_WALL and
INDIRECT_MAGIC. The 1.7.10 `Block.setResistance` stores three times the argument and its getter
divides it by five, so the effective blast resistance of the original is 0.6 times the setter
value, and 1.21.1 neither multiplies nor divides.

Nine agents compared the port with the oracle, OpenTC4 and the readable 1.7.10 vanilla tree
(`/home/Acak/work/mc1710-src`) and fixed sixty-two defects. The worst of them: the wand never
removed a warded stone block, the smelting bonus registry of the arcane furnace was empty, the
aura totem of the world generation did not exist, half of the eldritch ring was air, eighty-three
vanilla object tags of `ConfigAspects` were never carried, the crucible recounted its bellows on
every tick, the brain jar pulled every orb of a seven block cube and the fire bat had an invented
light restriction.

`scripts/porting/resolve_srg.py` is the permanent tool for the SRG names, and
`docs/analysis/2026-10-21-blast-resistance-verification.md` holds the byte code proof of the
resistance semantics. The consolidated report is
`docs/analysis/2026-10-21-openTC4-audit-summary.md`, the domain reports are
`docs/analysis/2026-10-21-openTC4-*.md`.

Verified in the integrated tree: 1773 JUnit tests with no failure, `convert_assets.py --verify`
with `missing_references: 0`, the 84 Python tests, and 312 Fabric game test scenarios with the
five known unstable cases. No client run, no multiplayer and no full survival progression.

## 2026-10-21 — The golems of the 1.7.10 oracle: the damage sources, the goal ladder and the heavy water golem

The readable 1.7.10 vanilla sources (`/home/Acak/work/mc1710-src`) settled three questions the
previous audit of the golems had left open, and every change below follows the oracle
(`/home/Acak/work/thaumcraft-4.2.3.5-oracle`) and that vanilla tree, never the 1.12 legacy line
of this repository, whose SRG-to-MCP mapping resolves the same fields to other names.

**The two damage sources of the golem and of the traveling trunk were the wrong ones.** Oracle
`thaumcraft/common/entities/golems/EntityGolemBase.java`:1131-1134 refuses
`DamageSource.field_76367_g` and `:1136-1138` halves `DamageSource.field_76376_m` for a thaumium
golem; oracle `thaumcraft/common/entities/golems/EntityTravelingTrunk.java`:106-109 refuses the
same first source. In 1.7.10 those fields are `DamageSource.cactus` ("cactus") and
`DamageSource.magic` ("magic") — 1.7.10 `net/minecraft/util/DamageSource.java`:25 and `:29` — and
`scripts/porting/resolve_srg.py` resolves them the same way. The port tested
`DamageTypes.IN_WALL` and `DamageTypes.INDIRECT_MAGIC`, the names the 1.12 mapping gives to
those SRG fields, so a golem never suffocated and a thaumium golem lost half of the *indirect*
magic damage instead of half of the magic damage. The new pure rules
`thaumcraft.common.items.golems.GolemDamageRules` (`IMMUNE_SOURCE = DamageTypes.CACTUS`,
`MAGIC_SOURCE = DamageTypes.MAGIC`) carry the two sources and are used by the golem
(`EntityGolemBase.hurt`) and by the trunk (`EntityTravelingTrunk.hurt`).

**The golem had a swimming goal the original never had.** Oracle `EntityGolemBase.java`:265
clears the whole goal list (`tasks.taskEntries.clear()`) and registers only its own goals, and
1.7.10 registered `EntityAISwimming` in the individual mobs (the wolf, the horse, the ocelot, the
sheep), never in `EntityLiving`, `EntityLivingBase`, `EntityCreature` or `EntityGolem`. The port
installed a native `FloatGoal` with the number one, and the native selector hands the `MOVE`
flag to the first registered goal of a number, so the float goal took the flag away from the
emptying goals of the number one (the Decanting and the Alchemy golem) and could not be
preempted by the goals of the numbers two and three. The float goal is gone.

**The order of the registration is the second half of the ladder.** The 1.7 selector walks its
goal list in the order of the registration (1.7.10 `EntityAITasks.onUpdateTasks`:49-85) and the
native `GoalSelector.tick()` walks `availableGoals` in the order of the registration as well.
The original registered the creeper flight (the number zero), then the goals of the core and only
then the door, the return home, the watch and the idle look; the port installed the shared goals
first, so they took the `MOVE` and the `LOOK` flags before the goals of the core could use them.
The installation now follows the original order.

**The golem did not come home when it suffocated or fell out of the world.** Oracle
`EntityGolemBase.java`:740-742 sets the golem back onto its home block when the damage source is
`DamageSource.field_76368_d` (the 1.7.10 "inWall") or `DamageSource.field_76380_i` (the 1.7.10
"outOfWorld"). The port had no such rule. It has one now, with the documented difference that the
port keeps the rotation of the golem and refuses a home that is not loaded.

**The heavy golems move twice as fast in water.** Oracle `EntityGolemBase.java`:380-382 doubles
the speed of the stone, the iron and the thaumium golem while it is in water. The native water
branch of `LivingEntity.travel` moves with the constant 0.02 and does not use the movement speed
attribute at all, so the port doubles the movement input of that branch — the same doubling of
the same movement.

The champion system was checked against the oracle as well and needed no change: the thirteen
modifiers (the grim one withers with `Potion.field_82731_v`, the sickly one starves with
`Potion.field_76438_s`), the warded champion with half of its *total* maximum health
(`EntityUtils.makeChampion`:271-274), the class whitelist of `ConfigEntities`:195-208 resolved by
id **and** class (the cave spider, the cultist knight and cleric, the mind spider and the taintacle
giant), the spawn roll of `EventHandlerEntity.entitySpawns`:389-421, the three dozen exact numbers
of the golem (materials, accessories, upgrades, ranges, capacities) and the recipes of the cores
and of the placers were all verified and are exact. The mighty champion deals six times the base
damage in the original, because the 1.7 attribute multiplies the running total twice; the native
`ADD_MULTIPLIED_TOTAL` was measured on a real `AttributeInstance` and multiplies the same way,
so the port needed no change either.

New JUnit tests pin the corrected damage sources and the missing swimming goal; the damage rules
of the traveling trunk in the GameTest source set follow the corrected source. Local:
`:common:compileJava`, `:common:compileTestJava`, `:common:compileGameTestJava` and
`:common:test` pass — 1669 JUnit tests in 241 suites, 0 failures. No client visual, multiplayer
or NeoForge acceptance is claimed; the GameTest set was compiled but not run.

## 2026-10-21 — Outer Lands eldritch variants: the metadata table, the lock key and the boss lock

Every eldritch block of the Outer Lands was placed through one native block, `thaumcraft:eldritch`,
so every original metadata of the oracle family landed on a block that does not host it. The
doors, the boss lock, the traps, the glyphed library desks and the crust decorations of the
labyrinth got a missing model and the hardness, the blast resistance and the light of the wrong
group; the original per-metadata values never reached the world. The native blockstates of the
port already carry the real per-metadata models and the oracle `BlockEldritch` decides the
grouping, so the table is now explicit (`OuterLandsWiringRules.ELDRITCH_VARIANTS`) and the
resolver of the port (`ThaumcraftOuterLands.state`) follows it:

| original metadata | native block | variant | model | texture | hardness | blast resistance | light |
|---|---|---|---|---|---|---|---|
| 0 | `thaumcraft:eldritch` | 0 | `block/eldritch` | `blocks/obsidiantile` | 50 | 20000 | 8 |
| 1 | `thaumcraft:eldritch` | 1 | `block/eldritch_1` | `blocks/obsidiantile` | 50 | 20000 | 8 |
| 2 | `thaumcraft:eldritch` | 2 | `block/eldritch_2` | `blocks/obsidiantile` | 50 | 20000 | 8 |
| 3 | `thaumcraft:eldritch` | 3 | `block/eldritch_3` | `blocks/obsidiantile` | 50 | 20000 | 8 |
| 4 | `thaumcraft:eldritch_crust` | 4 | `block/eldritch_crust` | `blocks/es_i_1` | 2 | 30 | 12 |
| 5 | `thaumcraft:eldritch_crust` | 5 | `block/eldritch_crust_5` | `blocks/es_i_2` | 2 | 30 | 12 |
| 6 | `thaumcraft:eldritch_deco` | 6 | `block/eldritch_deco` | `blocks/deco_1` | 4 | 100 | 5 |
| 7 | `thaumcraft:eldritch_doorway` | 7 | `block/eldritch_doorway` | `blocks/deco_3` | -1 | `Float.MAX_VALUE` | 12 |
| 8 | `thaumcraft:eldritch_doorway` | 8 | `block/eldritch_doorway_8` | `blocks/deco_2` | -1 | `Float.MAX_VALUE` | 5 |
| 9 | `thaumcraft:eldritch_spawner` | 9 | `block/eldritch_spawner` | `blocks/crust` | 15 | 30 | 4 |
| 10 | `thaumcraft:eldritch_spawner` | 10 | `block/eldritch_spawner_10_0..3` | `blocks/es_5..es_8` | 15 | 30 | 0 |

The hardness, the blast resistance and the light are the values of the oracle `BlockEldritch:120-135`
and `:231-260`; the textures are the icons of `BlockEldritch:78-94`, the metadata nine being the
icon of the cosmetic solid with the metadata fourteen. A missing native block is now reported
instead of answered with the air block of the registry default, which had put air where the
original puts a block.

Three more defects of the audit are closed against the oracle:

* `BlockEldritch:278` — the eldritch lock accepted the item path `eldritch_object_2`, which no
  item carries. The original carried the five metadata values of `ItemEldritchObject` on one item
  and the port flattens them into one item each, so the key is the runed tablet
  (`thaumcraft:eldritch_tablet`), the same item the key room keeps.
* `GenBossRoom:44-86` — the doorway of a boss room quadrant without an exit keeps the unknown
  direction and the coordinates zero, so the original writes the lock, the door and the invisible
  door of that quadrant into the world origin. The native reproduced the coordinates, but the
  1.21.1 generation writes into the chunk that is generated: that write is outside of the chunk
  unless the quadrant is the origin chunk itself, and `WorldGenRegion.getChunk` answers with
  `IllegalStateException: Requested chunk unavailable during world generation`, which aborts the
  chunk. **Deviation:** the doorway keeps the corner of its own chunk, the chunk relative zero of
  the original; the direction of the lock stays the unknown direction of the original.
* `GenPortal:84` — the portal room is cleared in all four corners; the native cleared only three
  and left the invisible wall of the lower right corner standing.

Item 11 of the audit is closed as far as the 1.21.1 data model allows: the document
`data/thaumcraft/dimension_effects/outer_lands.json` that the dimension type references was
missing and is added with the values of the oracle `WorldProviderOuter` (black sky, no sky, cloud
height one, no ambient light and the fog colour `0xA080A0` scaled by `0.15` = 1577752). 1.21.1
has no `dimension_effects` registry: the vanilla client picks the effects of a dimension from
`ultrawarm`, `natural` and `has_ceiling` and the NeoForge 21.1 client registers custom effects
through the event `RegisterDimensionSpecialEffectsEvent`. The document is therefore the reference
of the port and not read by the 1.21.1 client; the remaining visual difference needs a client
side registration, which is out of the scope of this change.

Items 8 and 9 of the audit stay open and are reported, not guessed: the eerie node over the altar
of the eldritch ring cannot be created inside a generating chunk, because the shared node factory
refuses a position whose chunk is not loaded (`NodeGeneration.available`) and would therefore
desynchronise the random stream of the labyrinth seed from the original, and the centre of the
ring cannot leave the three to twelve block window of its chunk, because the seven by seven
footprint of the ring has to fit into the chunk that the 1.21.1 generation writes.

Local: `:common:compileJava` and `:common:test` pass, 1569 JUnit tests in 226 suites, 0 failures,
0 skipped. `python3 -B scripts/porting/convert_assets.py --verify` reports 0 missing references.
No runtime, client visual, multiplayer or NeoForge acceptance is claimed.

## 2026-10-21 — Champion ward, headless golem eye height and the champion whitelist

Three production defects of the champion system and of the eldritch bestiary are closed against the
1.7.10 oracle.

The warded champion took half of its BASE max health as its ward. Oracle `EntityUtils.makeChampion:271-274`
reads `IAttributeInstance.func_111125_b()`, the TOTAL of the max health attribute, so a champion of a spider (base
sixteen plus the thirty health of the champion bonus) keeps a ward of twenty three and not eight. The native
promotion (`ChampionRules.Adapter.promote`) read the base value of the attribute, and the regeneration ceiling of
the warded champion (`ChampionModWarded:26`) measured the same base value, so the ward of a champion grew back to
eight and never to twenty three. Both places read the total now, and the effect context of the champion modifiers
carries the total maximum health instead of the base one. The 1.12 legacy tree of this repository still uses the
base value; the 1.7.10 oracle is the spec.

The headless eldritch golem kept the eye height of the headed golem. Oracle
`EntityEldritchGolem.func_70047_e:133-135` answers 3.33 for the headless golem, and the explosion that takes its
head starts at `y + 3.33` (`EntityEldritchGolem.func_70097_a:199`). The native port emulated the answer with
`getDefaultDimensions().withEyeHeight(...)`, but 1.21.1 caches the dimensions of a mob and only recomputes them in
`refreshDimensions()`, and `setHeadless` only wrote the entity data flag, so the cached 3.0 stayed. The golem
refreshes its cached dimensions with the flag now, and the client mob does the same when the flag arrives as entity
data.

`minecraft:cave_spider` was missing from the champion whitelist. The original whitelist was a map of classes
(`ConfigEntities:195-208`) and the spawn roll matched them with `Class.isAssignableFrom`
(`EventHandlerEntity.entitySpawns:409-414`), so a cave spider was a level zero champion candidate. The native
whitelist is keyed by the registry id, so the lookup resolves the id AND the class of the mob now: the cave spider
joins the whitelist with the level of the spider, and every subclass of a whitelisted class inherits the level of
its parent again (the taintacle giant, the cultist knights and clerics, the mind spider). The class of the original
zombie stays out of the class list on purpose: 1.21.1 merged the zombie villager into the class of the zombie, and
the zombie villager of the original 1.7 was a class of its own that the original never whitelisted.

Two fixtures of the harness declare the original champion join roll now, because the mind spider and the cultist
knight are champion candidates of the original and their exact numbers would otherwise depend on the world random
of the run: the warp event of the warp scenario and the cultists of the cultist scenario.

The game test source set of this branch did not compile: `EquipmentItemRuntimeChecks` calls
`ItemBottleTaint.burst(level, pos, player)`, a helper the main tree owns and this branch missed. The helper and the
`EntityBottleTaint.burst(ServerLevel)` it uses are ported from the main tree, so the scenarios of the harness run
again.

Local: `:common:compileJava` and `:common:test` pass, 1570 JUnit tests in 225 suites, 0 failures. The focused
GameTest run passes its eight cases. No client visual, multiplayer or NeoForge acceptance is claimed.

## 2026-10-21 — Outer Lands labyrinth fidelity: the maze generator and the eldritch rooms

The critical defect of the a5 eldritch audit is closed. The native labyrinth generator skipped a
cell without a single exit only when the cell *had* an exit, so it gave such a cell a random
room feature and drew one extra `nextInt(8)`: the random stream of the whole labyrinth diverged
and the key, nest and library rooms and every passage of every seed were different. The oracle
condition of `MazeGenerator:332` is restored. A new JUnit test compares the native generator
with a transcription of the oracle generator over the eight original labyrinth sizes and forty
seeds: 159 821 cells, 0 differences in the walls and 0 in the feature bytes. The same comparison
against the compiled oracle class measures 45 429 differing cells before the fix, all of them in
the feature byte.

Five more deviations of the audit are closed against the oracle: the trapped passage draws the
buried stone of the floor and of the ceiling separately (`GenPassage:54-55`), the nest room rolls
its own loot rarity of 15 % / 25 % / 60 % instead of the rarity of the common decoration
(`GenNestRoom:117`), the lock of a boss room takes the direction of the side it stands on
(`GenBossRoom:72-75`), the library desk slab keeps the upper half of the original metadata nine
(`GenLibraryRoom:86,96`), and the permanent item of the key room keeps its metadata two, the runed
tablet (`GenKeyRoom:97`). The fog, grass and foliage colours of `OuterLandsRules` now agree with
the native biome document `eldritch_lands.json`.

The key room guardian offset was checked against the oracle and was already correct: the 1.7
helper `MathHelper.getRandomIntegerInRange` is inclusive on both ends, so the sign factor is
minus one to one and not minus one to zero as the audit read it; a new test proves the offsets
against the recorded draws of the world random.

Local: `:common:compileJava` and `:common:test` pass, 1512 JUnit tests in 218 suites, 0 failures.
No runtime, client visual, multiplayer or NeoForge acceptance is claimed.

## 2026-10-21 — Parallel port wave: 33 sub-agents over three waves

The second (parallel) port line was stopped by the user and its whole scope taken over here. Three
waves of sub-agents, each in its own git worktree with a disjoint file list: 20 content waves
(eldritch bestiary, cultists, champion modifiers, tainted animals, the public API surface, the client
renderers and gear models, the eldritch blocks and tiles, the remaining machine blocks, the device
containers, the Outer Lands with its labyrinth, the villages, the recipe catalogue, the missing items,
the runic accessories, the golem AI, the projectiles, the utility layer, the wand enchantments, the
biome generators, the device screens), 5 repair waves (the cosmetic family consolidation, the assets
of ~30 blocks that arrived without any, the server game tests, the dedicated server start, the golem
goal audit) and 8 gap-closing waves that closed exactly what wave one had to defer.

Fixed on the way: duplicated registry ids that would have crashed the game, two registration classes
that were never called (the whole eldritch block family and the legacy items), a champion system whose
init was never run, two copies of the golem orb, the key access-list format, and the goal priority
order (1.21.1 keeps the lowest number as the highest precedence, like 1.7.10).

The recipe catalogue now resolves 247 original keys; 19 stay deferred (11 need another mod's ores,
8 need a block state a 1.21.1 recipe result cannot carry). Local: compile clean, 1494 JUnit tests,
0 missing resource references, 84/84 Python. Client inspection, multiplayer, full survival
progression and NeoForge parity are not confirmed by this checkpoint.

## 2026-10-20 — Fourth-group runtime corrections, Hover Girdle and Pech goals

Fix incompatible test relay ticker, real typed-payload wire inspection, optional-potion fixture bytes,
exact discounted payment and native XP RNG. Correct Pech wrong-wand cost query and unrelated wand
formation dispatch. Add original Hover Girdle and Pech avoid/watch goals. Local691JUnit/84Python and
all Fabric source-set compilation pass;237runtimecases await GitHub. The aborted04208de run is unaccepted.

## 2026-10-20 — Native accessories, Pech, hungry nodes and loot (runtime pending)

Six-agent content integration: object-tag API; HUNGRY node ecology; Hellbat; native Baubles,
FocusPouch and coloured candles; Pech/trades/curse/ManaBeans; seven crystal clusters;
Advanced Furnace; original loot bags/potions/enchant selection; administrator parser and warp feedback.
Local691JUnit/84Python and Fabric main/gameTest/clientTest compilation pass;235 native cases are
required by the next GitHub run. No full runtime, client visual or NeoForge acceptance is claimed.
Remaining natural biomes, Pech goal differences and vanilla brewing adaptation are documented.

## 2026-10-20 — Fabric b1e49f2 accepted by GitHub CI

648 JUnit, all212 required Fabric GameTests and79 Python checks pass for exact commit
b1e49f24d0c1112800bec4276277623939d4c670. Full build/freshness/isolation/attribution gates pass.
Run: https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39409582347
Original orb collision escape restored; native lamp/runic physical fixtures now deterministic.
Later source groups and client/multiplayer/survival/NeoForge acceptance remain pending.
Entries below are historical checkpoints.

## 2026-10-20 — Runic, special foci, nodes and scan API (CI pending)

Native runic shield/augmentation, Fire/Frost/Shock specialisations, transient shock fields,
UNSTABLE node/orb custody, Primordial Pearl node mutation, gated cheat sheet, Firebat/Wisp,
scan callbacks/instance-specific sessions and original admin state commands. Local647JUnit/79Python
and Fabric main/gameTest/clientTest compilation pass. New full server/client acceptance pending;
previous two remaining runtime failures corrected without relabeling failed CI as green.

## 2026-10-20 — Next Fabric content group (remote runtime validation pending)

Unknown knowledge and scan clues/entity variants; server research settings; Repair/Haste infusion
and native Bore repair; Ward behaviour and Excavation Silk/Dowsing; Growth/Fertility lamps,
Deconstruction table, Brainy/Giant zombies. Local 620 JUnit and 78 Python pass; full server
validation is offloaded to GitHub CI for the next commit. Client/NeoForge acceptance remains pending.

# MODLOG — анализ Thaumcraft 4.2 FOREVA

## 2026-10-20: интеграция исследований, инфузии, устройств и Warp — CI passed

- R10: исходные расходы копирования и whole-stack persistence/count100/127/128.
- Thaumatorium и Mnemonic Matrix; 22 рецепта зачарования с оплатой XP до эссенции.
- Warding/Architect/Trade, Arcane Lamp/Amber и Vis Charge Relay с оплатой до callbacks.
- Warp: серверные ветки, эффекты, Mind Spider/Guardian/Orb и клиентские ресурсы.
- GitHub CI на d508ada: 582 JUnit / 186 Fabric GameTest / 78 Python, fresh-result/production-isolation/attribution gates прошли. Клиент/NeoForge не подтверждены.
- CI: кэш Gradle/Loom, свежие тесты без принудительной пересборки всех задач, отчёты 7 дней, отмена устаревших прогонов.


## 2026-10-20: магическое ухо

- `thaumcraft:arcane_ear`: исходные настройки/рецепт, пять инструментов, 25 нот, дальность 64 и сигнал 15 на 10 тиков.
- Настоящий vanilla note hook, очередь до POST тика, EN/RU, книга, модели off/on, дроп без сохранённой настройки.
- Совместная проверка с предыдущими резервуарами: 542 JUnit / 177 Fabric GameTest / 76 Python, Fabric build/isolation.
- Субагенты выполнили реализацию и два независимых ревью; найденные ошибки timing/packet codec исправлены.
- [Контракт и ограничения](docs/analysis/2026-10-20-arcane-ear.md); client visuals/NeoForge остаются непроверенными.

## 2026-10-20: резервуары в работе алхимических големов

- Обновление с GitHub до `88d683b`; реализованы исходные ветки сбора/доставки резервуара, его приоритеты и частичный остаток.
- Содержимое резервуара и настоящий груз голема оплачиваются одним native commit; прямые вызовы без активной задачи отказывают.
- Исправлен старый предел 32 в общем переносчике: два Earth у Thaumium допускают 64.
- 542 JUnit / 176 Fabric GameTest / 76 Python прошли; Fabric JAR и production isolation проверены.
- [Источники, точные результаты и открытые проверки](docs/analysis/2026-10-20-golem-reservoir.md); клиента/NeoForge не запускали.

## 2026-10-15: начальный системный анализ

- Полный клон `0FL01/Thaumcraft-4.2-FOREVA`, ветка `master`, baseline `4bc68454b1063cdc1255f833dd537e0fd4fc341d`.
- Production code и original/donor JAR не изменялись; новый commit не создан.
- Инвентаризация: 934 main Java + 530 Java test files, 1199 test annotations, 1676 resource paths.
- Original behavioral oracle: CFR 0.152, 859 Java files вне Git-репозитория; `thaumcraft_src` здесь содержит ресурсы.
- Проверено: Python tests 13/13; donor ABI snapshot 1665 entries; semantic target snapshot 310 symbols.
- Java 8 HashMap int-array cache miss и Python HANDLE6 resolver mismatch воспроизведены отдельными fixtures.
- Сервер/клиент Minecraft и full Gradle build не запускались: Docker daemon недоступен; локальный Windows checkout имеет CRLF shell scripts.
- Подробные подтверждённые факты, условия и открытые runtime вопросы: [главный отчёт](docs/analysis/2026-10-15-CODE_ANALYSIS_RU.md).
- В Git добавлять только аналитику/собственный код; декомпиляция оригинала, transient tools, logs и saves остаются вне tracked deliverables.

Следующий этап: воспроизводимый build, focused critical state tests, dedicated server и final-JAR checks, client visual scenarios. Documentation-only files не требуют runtime smoke по AGENTS.md.

## 2026-10-15: нативная основа Minecraft 1.21.1

- Создана локальная ветка `port/minecraft-1.21.1`; legacy `src/` сохранён без изменений и исключён из новой сборки.
- Переход на Java 21, Gradle 8.12.1, Architectury API 13.0.11, Fabric и NeoForge; оба референса зафиксированы в `docs/porting/source-lock.json`.
- Реализованы 48 аспектов, данные 201 исследования, знания/сохранение/mixin/S2C-синхронизация игрока, серверное сканирование, wood/iron wand, тестовый узел, банки64 и phials8; четыре начальных рецепта соответствуют оригиналу.
- Перенесены исходные PNG/OGG, modern модели и переводы; исправлены atlas inclusion, nested filter schema, uppercase sound IDs и общий mipmap. Текстовые ресурсы/provenance fingerprints приведены к canonical LF для одинакового результата checkout на Windows/Linux.
- Проверки фактически выполнены: 46 JUnit, 19 Python asset tests, 5 Fabric и 5 NeoForge GameTests; `scripts/dev.ps1 -Task validate` через официальный wrapper завершился с exit0 (`.tmp/modern-validation3.log`).
- NeoForge client загрузил итоговый atlas1024x1024x4; integrated world login и сообщения сканирования наблюдались в логе. Полное визуальное соответствие не установлено. Fabric startup проверен до последней правки исключения unused mipmap sprites; общие ресурсы после неё проверены Neo/native codecs.
- Production JAR обоих загрузчиков проверены на отсутствие Minecraft/legacy classes и вложенных оригинальных JAR; hashes и результаты: `docs/porting/verification.json`.
- Neo mock handshake настроен до реального JOIN; проверяется отправка и декодирование фактического Architectury encoded payload. Production networking не подменялся для прохождения теста.
- CI переведён на Java21 и оба headless harness; remote workflow не запускался, автоматическая публикация старой 1.12 release workflow заменена ручной проверкой. Никаких push/release не выполнено.
- Механики, оставшиеся pending, перечислены в `docs/MIGRATION_STATUS.md`; все934 legacy paths учтены:12partial/922pending/0complete. Первый рабочий checkpoint не означает полный перенос TC4. Следующий приоритет: таумономикон, исследовательский стол/записки/подсказки, затем arcane crafting/crucible/infusion.


## 2026-10-15: таумономикон, актуальные refs и Git checkpoint

- Продолжен существующий checkpoint `8abb7feb` на ветке `port/minecraft-1.21.1`; полный порт всё ещё не завершён.
- Три субагента независимо проверили active gameplay, build/packaging и оба reference corpora. Созданы пофайловые inventories pinned FOREVA/OpenTC4/Architectury и свежего Architectury upstream3a836a39, delta45; пины gameplay/reference не заменены.
- Добавлены normal Thaumonomicon, bookshelf wand trigger без vis cost, native floating/fire-resistant item entity, server snapshot/open, original single-pass sibling effects, category/research graph visibility и completed/gated text+illustration pages с EN/RU pagination.
- Исправлены original node rewards max(4,vis/10), block picked-item identity/redstone duplicates и HexUtils original fixed orientation с расходом RNG draw. PowerShell single-task splatting исправлен; оба remapped JAR включают проверенные canonical notices+LICENSE.
- Реальные проверки final production source: `.tmp/session-book-final-validate.log` exit0;61 JUnit,9 Fabric и9 NeoForge GameTests,0 failures/errors. Python tooling24PASS; extractor/resource checksPASS;492 sprites/59 refs/66 events/111 sounds/missing0.
- Реальный клиент выявил double-blur из Screen.render1.21.1; устранён. Final isolated Fabric smoke3 exit0/SCRIPT_PASSED, все8 точных PNG просмотрены root, подтверждены sharp map/text/inline illustration, Next/Previous и RU overflow. Промежуточное подозрение о пустом Previous опровергнуто равными pixel ROI03/05; не является gameplay defect. Test-only harness снимает afterRender+flush и не входит в production JAR.
- Evidence: `docs/porting/verification.json`, `client-smoke-verification.json`; source inventory43 active Java,17partial/917pending/0complete из934. Private source/saves/originalJAR не менялись и не добавлялись.
- User explicitly предложил использовать Git: текущая проверенная unit сохраняется локальным коммитом; public push/release не выполняется. Следующая unit — research unlock/note service, затем table/puzzle и native recipe pages.


## 2026-10-15: отдельная спецификация следующего research этапа

- После user steering про дополнительных субагентов и Git продолжен read-only original audit: записаны research-unlock-spec и research-notes-spec, production исходники не менялись.
- Установлены original default difficulty0, Secondary-only point purchase, exact cost/prerequisite/clue/warp rules; native purchase обязана атомарно recheck баланс на сервере вместо original authority defect.
- Сопоставлены note NBT/data kinds, solved vs learned flags, endpoint/RNG/HashMap ordering, paper/tool costs, original copy ink_sac и расхождение legacy FOREVA FEATHER. Эти документы — specification/test plan, не реализованная research note механика.
- Проверенная playable book unit сохранена локально в commit85e22827; следующий docs commit отделяет план будущей unit от реализации. Никакой public push не выполнялся.

## 2026-10-15: покупка вторичных исследований

- Продолжена ветка `port/minecraft-1.21.1` после8540b3dc. Три субагента разделили policy/tests, client UI/smoke и real-loader fixtures; независимые повторные review policy и networking/UI не нашли конкретных дефектов.
- Книга покупает Secondary при default difficulty0/wussfalse за исходные aspect costs. Native key-only C2S (лимит128) использует sender connection и server thread. Полный admission/balance preflight, atomic copy/commit, exact debit, first-completion warp и direct ordered siblings; replay без повторных расходов.
- Исправлен подтверждённый original authority defect: server больше не доверяет client affordability/difficulty/clue. Публичный PlayerKnowledge overdraw quirk не изменён; native config, primary notes и clue generation остаются pending.
- Реальные проверки `.tmp/purchase-validate1.log` exit0:79JUnit,13Fabric+13NeoForgeGameTests,0failures/errors;24PythonPASS, extractor/resource/noticesPASS. Новые NeoForge scenarios декодируют настоящий encoded knowledge S2C. Both production JAR собраны с final main source; clientTest в них отсутствует.
- Actual Fabric client `.tmp/purchase-client1.log` exit0/SCRIPT_PASSED: node click при3/2 без изменений, затем6/6→3/3 по C2S/S2C, без optimistic completion/debit, completed-entry reopening без доплаты. Root просмотрел все10 exact PNG;09 показывает green3/3/red2/3 tooltip,10 readable purchased text. Снимки EN/RU, map и pagination сохранены. Полное TC4 visual parity и NeoForge purchase client не заявлены.
- Обновлены verification/hashes, asset manifest, handoff и inventory:47active main Java,19partial/915pending/0complete из934. Следующий этап — primary note component/generation/ink/paper и research table.
- Всё сохранено локально через Git; public push/release, original/legacy edits и включение private decompile/saves не выполнялись.

## 2026-10-15: план полного переноса

- Добавлен `docs/MIGRATION_PLAN.md`: границы TC4 4.2.3.5 → Java21/Minecraft1.21.1/Fabric+NeoForge, этапы M00–M15, зависимости, конкретные research units R01–R13 и критерии приёмки полного игрового цикла.
- Три субагента проверили gameplay coverage, architecture/verification и original-vs-legacy scope. Сверка paths:859 original,934 legacy,749 shared,185 legacy-only,110 original-only. Пофайловые числа не объявлены процентом игрового прогресса; введён план отдельного disposition ledger.
- Учтены early survival resources/nodes, accessory adapter, API, save schema/version policy, bounded knowledge sync, clean CI/fresh evidence, test helper isolation, packaged multiplayer, визуальные/звуковые и человеческие проверки.
- Изменена только документация. Production code, проверенные counts и evidence не повышались; Gradle/игра заново не запускались. Проверены ссылки, структура плана и Git diff; план сохранён отдельным локальным commit.

## 2026-10-15: начало полного плана, данные записок и изучение

- Активирована полная пользовательская цель M00–M15; она не завершена этим checkpoint. Три субагента реализовали note data/generator/solver и independent reviews, client scenarios, scope ledger и original ink audit.
- R01 immutable validated component persistent/wire, R02 original HashMap/RNG generator и516 private algorithm goldens наJava8/21 для172definitions, R03 direct-component solver/pruning. R04 частично:100inktool+IScribeTools+3nativeScribe recipes. R05 normal/discovery item learning/presentation с server parents/transfer/consume1/warp/siblings; shared completion helper сохраняет purchase semantics.
- Final central `.tmp/notes-validate5.log` exit0:131JUnit,17Fabric+17NeoForgeGameTests;24PythonPASS, extract/assets/noticesPASS. Initial integration failures исправлены до final run; validate компилирует clientTest, его source надо замораживать тоже.
- Actual `.tmp/notes-client1.log` exit0/SCRIPT_PASSED, все12PNG просмотреныroot;11/12 native inventory notes/discovery, ordinary useItem учитNITOR/consume1 безoptimisticstate и безreader lux, replayоставляеткопию. Pools/warp/другаяnote/ink99неизменны. Interactive table ещёнеигрался; серверfixtureгенерирует/решаетgrid.
- M00 scopeledger1044paths учёл859original/934legacy/749shared с conservative needs_review verdicts; self-test/checkPASS. Inventory56active/23partial/911pending/0complete. 42itemmodels/7recipes/64texturerefs, безclaimsполногоgameplay.
- Oldvanilla ink semantics доказаныofficialJAR bytecode/mappings, privateevidenceвнеGit; R06 ScribingInk draft вignored.tmp (15directJUnitPASS) ещёнеproduction. Следующий шаг — book primary issuance сpaper/ink/dedup/atomicdelivery, затемtableR07/R08.
- Original/legacy/private assets и JAR не изменялись; публичныйpush/release не выполнялись. Goal остаётсяactive.

## 2026-10-15: пересмотр плана полного переноса

- По запросу пользователя пересмотрен `docs/MIGRATION_PLAN.md` с тремя субагентами. Обновлена отправная точка до проверенного `03fe0782`: 131 JUnit, 24 Python, 17+17 GameTests, 12 просмотренных PNG. Это прежние результаты; новых игровых запусков в рамках планирования не было.
- Уточнены этапы M00–M15, технические пакеты A01–A07, исследовательские units R01–R16 и игровые checkpoints C1–C5. Добавлены зависимости manipulator → VisNet, deconstruction → arcane, harness → jars/accessory contract; original commands/IMC, fake-player adapter, crafting warp, infusion XP и retrogen собственных миров 1.21.1.
- Расширены критерии API/config/resource coverage, учёт 22 исходных локализаций, ранние packaged проверки обоих loaders, protocol/reload matrix, public/private CI и привязка evidence к проверенной сборке. Роль OpenTC4 в source-lock уточнена как modified reference; commit pins и целевые версии не менялись.
- R06 остаётся замороженной незавершённой работой в исходниках. Центральные validate/client smoke после этих изменений ещё не выполнены; новые тестовые сценарии не объявлены пройденными. Документация и metadata плана сохраняются отдельным локальным commit, без включения незавершённого кода и без публичной отправки.

## 2026-10-15: R06 — выдача первичных записок

- Продолжен полный перенос после `35232eaa`. Добавлены key-only request, authoritative admission/dedup, staged paper/ink, native inventory/drop delivery и book GUI/feedback без optimistic changes. Issuance не завершает research и не меняет RP/warp. Независимый review service/network не нашёл блокирующих дефектов текущего default0 пути.
- Подключён проверенный old ink:99→100/count1, creative/Unbreakable bypass, один player nextInt для positive Unbreaking. Сохранён original full-creative discard с расходом бумаги; native null/unspawned drop отказывает без debit. Old InventoryPlayer paper/discard bytecode evidence добавлен к ink audit, private disassembly остаётся вне Git.
- Central `.tmp/r06-validate1.log` exit0:155JUnit/26Fabric/26NeoForge GameTests, без failures/errors/skips. Новые9cases на loader проверяют request/reuse/refusals/order/full inventory/ink/RNG/generation/delivery failure. Python24/extractor/resource/notices PASS; actual XML и remapped artifact hashes записаны.
- Client1 прошёл actual request/S2C до смерти test player от Slime и завершился failure. Test-only entity invulnerability сохраняет survival costs; failed owned world сохранён, fresh isolated retry client2 exit0/SCRIPT_PASSED. Production AI/gameplay не менялись.
- Все14 exact PNG client2 просмотрены root,13/14 независимо другим агентом. Actual outbound request1, paper2→1/ink99→100, одна unfinished NITOR note; missing/duplicate GUI не отправляет intent, immediate client не списывает ресурсы, knowledge/RP/warp неизменны. Full visuals/NeoForge primary client/packaged multiplayer не заявлены.
- Inventory61main/23partial/911pending/0complete; scope1044needs_review,32counterparts. Обновлены status/handoff/plan/README/evidence; добавлена подробная R07 table specification. Следующий код — shared research table, затем hex edits. Полная цель M00–M15 остаётся активной; public push/release не выполнялись.

## 2026-10-15: полный маршрут переноса после R06

- По запросу пользователя уточнён `docs/MIGRATION_PLAN.md` после read-only проверки трёх субагентов: сохранён полный охват M00–M15, добавлены практические волны W0–W7, явные выходы C1–C5 и границы отложенного контента. Последний принятый игровой checkpoint — R06 `7222a5a1`.
- Закреплены зависимости VisNet→manipulator/Excavation upgrades→полный Arcane Bore и warp-driven research gates; добавлены below-class contracts, packet/event/config manifests, публичные поля API и исходящий MFR capture contract. Расширены resource closure, consumer/atlas/UV, production/test isolation, ранний packaged smoke, mixed-loader policy и ранние performance budgets.
- R07 существует в dirty tree и остаётся непринятой: resource/Python проверка FAIL из-за baked atlas references прямоугольных table textures; центральные Gradle/runtime/client запуски ещё не выполнены. Написанные36 cases на loader и17PNG сценарий не названы PASS; реальные unload/reload/restart остаются отдельной проверкой. Проверенный baseline155JUnit/26+26GameTests/14PNG не изменён.
- Обновлены status/handoff. Этот commit содержит только документацию плана; незавершённый R07 код и verification JSON в него не входят. Для documentation-only изменений Minecraft/Gradle не запускались; публичная отправка не выполнялась.

## 2026-10-15: актуальный полный план и очередь после R07

- По новому запросу пользователя три субагента повторно проверили gameplay, платформу/поставку и original scope. Полный маршрут W0–W7/M00–M15 сохранён; добавлены конкретные следующие задачи P01–P06 с результатами, владельцами и независимой проверкой.
- Отправная точка отделена от промежуточного R07 evidence: R06 `7222a5a1` принят; validate3 имеет161 JUnit/38+38 GameTests, client2 —19 просмотренных PNG и process restart; последующий validate5 —39/40 Fabric PASS и реальный cancellation retry FAIL. Четыре UUID adapter/accessor файла ещё не интегрированы и не приняты. Старый atlas FAIL больше не назван текущим блокером.
- Добавлены minimal shared bonus prerequisite R08, post-refund discovery до solver, полный count100/127 lifecycle prerequisite R10, общий spawn-admission/rollback contract, нагрузка небольших C2S, CI required case IDs и внешний addon fixture обоих loaders. Сохранена подробная спецификация R08 как проект будущей реализации.
- Актуализированы plan/status/handoff; ledger snapshots и принятая готовность не повышены. Этот checkpoint содержит только документацию: production, build, worlds и verification JSON не менялись; новые Gradle/client прогоны не запускались. Публичный push/release не выполнялся.

## 2026-10-15: R07 — исследовательский стол и безопасная выдача содержимого

- Реализованы original table craft output1, placement/conversion всех направлений и permissive topology quirks, общий public inventory/menu38slots, predicates/quick-move, save/update, first-native-tick repair, world BER/baked item модели и read-only note GUI. Ink/pools/edit/combine/copy не подменены заглушками и остаются R08–R10.
- Evacuation переносит точные stacks в durable per-level SavedData, очищает physical BE и доставляет только в level-post, сохраняя callback-created replacement inventory. Stable UUID/count/components, batch64, quarantine и over99 entity serialization проверены; player/container codec99 остаётся R10 blocker.
- Actual Fabric ADD cancellation выявила UUID reservation leak (validate5). Guarded snapshot/discard/cleanup с native accessor mixins сохраняет существующих visible/hidden владельцев. Validate6 выявил early wake общей очереди; transient per-output cooldown и явный unrelated-enqueue regression исправили это без ослабления tests/RNG/material semantics.
- Final `.tmp/r07-validate7.log` exit0:161JUnit и41Fabric+41NeoForge GameTests; XML failures/errors/skips0, required case IDs проверены. Actual native cancellation/retry, hidden owner, callback replacement, unload/reload и cold-load repair прошли.27PythonPASS, extractor/resource/notices PASS;494sprites/69refs/66events/111OGG/missing0.
- Original table PNG byte-identical; explicit full-frame static atlas sources/native codec mutation tests, DOWN-face UV probe264vertices и scoped GUI alpha исправлены. Client1 script PASS не скрыл visual-дефект17; повторные client2/client3 просмотрены после исправления.
- Final Fabric client3 exit0/SCRIPT_PASSED, run `client-smoke-91bd518d-9a58-4451-addf-4e3becd205c8`; root19exactPNG, independent15–19 matching hashes. Process restart client2→client3 сравнил весь native Inventory NBT до setup, expectation read-only из owned saved region. Source fixture world не открывался; level.dat hash совпал.
- Обновлены evidence с final JAR hashes, inventory81main/33partial/901pending/0complete, scope1044needs_review/49counterparts, status/handoff/plan/README. R07 implementation checkpoint не закрывает NeoForge/packaged visual приёмку, full visual/audio parity или весь M01. Следующая работа A07 production/test isolation и R08 shared bonus/hex editing. Публичный push/release не выполнялся; полная цель остаётся активной.

## 2026-10-17: M09b — настоящий VisNet, принят и экспортирован (Fabric-only)

- Реализованы обычный node transducer, energized source с обновляемым per-source-tick raw CV budget, relay graph и Terra-ускорение кристаллизатора. Сохранены original MAX reduction, 1001/951 переходы, первый нулевой pulse, CURRENT0 при возврате и ordinary zero-key pruning. Исправлены исходно подтверждённые flux9/Nausea/goo-tail native расхождения; special nodes/advanced tier/other consumers/full API остаются открытыми.
- После реального клиента финальный `visnet-fabric-validate7`: **220 JUnit / все105 Fabric / 72 Python (95.133с)**, failures/errors/skips0, **3827 неизменённых входов**. Проверены свежие XML, production ZIP/notices/test isolation/one intermediary AW/recipes/resources. NeoForge не запускался.
- `visnet-fabric-client5` SCRIPT_PASSED:255.1941779с/5103 client ticks, все10 точных PNG154–163 осмотрены root. Три настоящие оплаты, две зарядки другого узла, mining/pickup/reinstall прежнего кристаллизатора, three60tick cycles по94rawEARTH и full1000tick baseline без энергии, четыре новых физических outputs. Итог282CV; прямое source питание исключено расстоянием²197, relay пути²49/50.
- Независимые normal-stop tail и stopped-NBT audit: count1091<1200, BASE80/NORMAL/NONE/шесть нулевых current keys;95non-nodeBE/20jars333/72candles, oldstock/gear/wands/foci/XP11 и именованная cowhealth6 сохранены, pending/quarantine пусты, все183parentfiles неизменны. Failedclient1–4 worlds/diagnostics сохранены и не используются как accepted input.
- Source-bound evidence: `docs/porting/visnet-client.json`, `docs/porting/verification.json`, accepted archive `.tmp/visnet-accepted-verification7.json`. Экспорт **10,556,826bytes / SHA256 bac83fff54504682f7a00523d22f10e00a334eb922f559f93ea27bf349408caa**. Прежний M09a сохранён внеmods в `backups/stabilizer-fabric-validate2/`; dependencies/olderbackups неизменны, README/hashes обновлены.
- Это не fullTC4/fullvisual/audio/ordinarypackaged/EULAserver/remote/survival/NeoForge приёмка. Сразу начата следующая boundedFabric unit: focal manipulator + Excavation upgrades перед Arcane Bore; original audit/контракты, пока не новая реализация. Public push/release/commits не выполнялись.

## 2026-10-23: волна 22–23.10 — двадцать два субагента, закрытый клиентский пробел, паритет NeoForge

- **Волна.** Две волны субагентов — шестнадцать в первой (отчёт `docs/analysis/2026-10-23-wave-16-agents.md`) и шесть во второй (`w2api`, `w2blocks`, `w2clientblocks`, `w2clientfx`, `w2devices`, `w2golems`) — и перезапуск полосы `w2clientfx` после прерванной сборки: **двадцать два субагента**. Волну закрыли полосы лидера: клиентские прогоны (`clientrun`, `clientrun2`), рендереры сущностей (`renderers`), поршень (`piston`), разбор точности чисел (`floatfix`), надпись радиального фокуса (`focuslabel`), красные сценарии (`gametest5`, `testfix4`, `testfix9`, `testsuite`), survival-приёмка (`survival`), мультиплеер (`multiplayer`) и NeoForge (`neoforge`).
- **Ledger.** `parity_verified` 653 → **945**; `partial` 315 → **86**; `missing` 58 → **10**; `not_assessed` 18 → **3**; `needs_review`/`unreviewed` — **ноль**. Всего 1044 единицы: 327 replace / 290 port / 263 adapt / 164 exclude. Все десять `missing` и все три `not_assessed` — намеренные `exclude` с конкретным доказательством: пять классов интеграции JEI 1.12 (весь пакет — шесть классов — исключён целиком: в оригинале 4.2.3.5 интеграции JEI нет вообще, а в 1.21.1 JEI — внешний мод со своим API-артефактом; данные, которые показал бы плагин, покрыты — все 308 ключей рецептов оригинала стали нативными рецептами, и их показывает книга исследований), пять помощников позы и начала луча палочки 1.12 (свёрнуты в нативный рендерер палочки `PrimalWandFocusRenderer`), эльдритч-диагностика FPS 1.12, рендерер эльдритч-предметов (в оракуле его нет) и плагин WAILA (в 1.7.10 WAILA не существовало).
- **Клиентский пробел закрыт.** Прежний handoff писал: «GLX-контекст не создаётся ни с одним драйвером Mesa», поэтому визуальная приёмка не была выполнена ни разу. В этой машине настоящий контекст OpenGL 4.6 (`DISPLAY=:0`, авторитет sddm извлечён один раз в приватный `XAUTHORITY`): клиент стартует, рисует настоящие кадры и играет. Собственный smoke-харнесс порта дошёл до `SCRIPT_PASSED` с **66 кадрами из 66** — 65 сверены с оригиналом и прошли, 66-й (наложение двух строк в радиальном фокусе) исправлен после прогона. Настоящий выделенный сервер и настоящий клиент через loopback-сокет прошли **21 этап из 21**; на физическом клиенте зарегистрированы все **21** clientbound и все **12** serverbound типов payload TC4 (это свойство одиночный прогон доказать не может), а по сокету прошли 8 clientbound и 6 serverbound каналов TC4.
- **Дефекты, которые нашёл только запущенный мир** (каждый с `file:line` в оракуле и в порте):
  - **Каждый мир TC4 показывал игроку диалог «Worlds using Experimental Settings are not supported».** 1.21.1 считает жизненный цикл сохранённого мира по его level stems: `WorldDimensions.bake` (`WorldDimensions.java:229`) отдаёт `experimental()`, как только в мире больше трёх ванильных стволов, а `isVanillaLike` (`WorldDimensions.java:146-161`) отвергает любой ключ, кроме overworld/nether/end. Порт регистрирует Внешние земли как настоящий level stem, поэтому **каждый** мир мода был экспериментальным **для каждого** игрока — генератор ни при чём. Доказательство — побайтовая NBT-проба, убравшая ровно одну запись: тот же билд открыл мир без диалога. Исправлено `common/src/main/java/thaumcraft/common/mixin/Tc4LevelStemLifecycleMixin.java`; харнесс теперь **валит** прогон, если диалог появится.
  - **Подбор особого предмета TC4 убивал клиент.** 1.21.1 рисует анимацию подбора *копией* подобранной сущности, а `ItemEntity.copy()` сохраняет **тип** сущности, но не её **класс**, поэтому рендерер, типизированный на `EntitySpecialItem`, бросал `ClassCastException` (`RenderSpecialItem.render` → `EntityRenderDispatcher.render` → `ItemPickupParticle.render`). Оба рендерера предметов теперь принимают любой `ItemEntity` (`RenderSpecialItem.java:33`, `RenderFollowingItem.java:23`).
  - **Семь типов сущностей из 56 не имели клиентского рендерера.** 1.21.1 ищет рендерер по типу сущности и не имеет запасного варианта (`EntityRenderDispatcher.getRenderer` → `renderers.get(entity.getType())`), поэтому это `NullPointerException` в тот кадр, когда сущность рисуют. Все семь зарегистрированы (`bottle_taint`, `golem_orb`, `primal_arrow`, `taint_crawler`, `taint_seed`, `taint_seed_prime`, `taintacle_giant`); новая таблица `ThaumcraftEntityRenderers` стала единственным местом регистрации, а тест `EntityRendererCoverageTest` утверждает, что у каждого типа сущности есть рендерер, — забытый рендерер теперь читаемая ошибка на старте клиента, а не краш в кадре.
  - **Строки сундуков TC4 не доходили ни до одной таблицы сундука на NeoForge** — круг encode/decode невозможен: таблица подземелия несёт *набор* тегов, который не привязан, пока грузится лут, а кодек строки не пишет `type`. Строки теперь дописываются в загруженный пул самой таблицы — точное совпадение взвешенного списка Fabric и порядка розыгрыша.
  - **Уведомление о разрушении срабатывало дважды на NeoForge**: общий код стрелял `BlockEvent.BREAK`, а NeoForge — своим нативным `BlockEvent.BreakEvent`, который NeoForge же и пересылает в общий.
  - **Тик тентакля падал на NeoForge** — NeoForge 21.1 переписывает конвейер урона (`hurt` кладёт `DamageContainer`, `actuallyHurt` его читает), а общий код тентакля вызывал `actuallyHurt` напрямую.
  - **Очищающая жидкость была невидима для правил воды на NeoForge** — NeoForge сравнивает идентичность *типа* жидкости, а не тег.
  - **Пробы GameTest вообще не регистрировались на NeoForge** — одна регистрация позволила зелёными шесть сценариев.
  - **Внешние земли были недостижимы**: события шестого вида не существовало, а `WandTriggerRegistry` не содержал ни одной регистрации. Новая `OculusFormation` повторяет оригинальный оculus (`ConfigRecipes.java:99`, `WandManager.java:427-441`, `:708-740`): четыре глаза на эльдритч-алтаре, тёмный узел над ним, исследование `OCULUS` и палочка с шестьюстами виса открывают врата.
  - **Волшебный лес не выращивал ни одного мана-боба**: граница поиска брала региональную карту высот, а 1.21.1 замораживает её до шага features.
  - **Скорость взмаха, амплитуда взмаха и затухание падения заражённых животных считались во float, а не в double оригинала** — расхождение для 7 549 747 из 16 777 216 входов.
  - **Расстояние столкновения багрового портала было 9.0 вместо 3.0 оригинала** (оригинал сравнивает квадрат расстояния с тремя).
  - **У таумометра было два осколка вместо четырёх** оригинала (`ConfigRecipes.java:458`, `{" 1 ","IGI"," 1 "}` — осколок любого вида в каждом углу).
  - **Поршень 1.7.10 решал по классу, а не по метаданным**: `BlockPistonBase#canPushBlock` заканчивается на `!(block instanceof ITileEntityProvider)` — проверено по официальному `client.jar` Mojang 1.7.10 и официальному srg MCP. Семейство `BlockContainer` не двигалось никогда, обычный `Block` двигался по любой метаданной, и перемещение уничтожала плитку (в 1.7.10 `TileEntityPiston` нёс только блок и метаданные). Порт повторяет это ровно.
  - **Аспекты смерти не выпадали**: оригинал роняет по одному шару на каждый первичный аспект при смерти с 50 % броском и значением `1 + nextInt(amount)`, взятыми из **мирового** генератора. Порядок потока доказан по патчу Forge 1.7.10 для `EntityLivingBase.onDeath` и точке внедрения Architectury `LIVING_DEATH`; три затронутых игровых теста выведены заново.
- **Урок волны (записан как правило).** Три отдельные волны написали одно и то же неверное утверждение о правиле «креативный игрок не находится», и его устояли только чтением **оригинального исходника 1.7.10**: `if (!entityplayer1.isCreative() && entityplayer1.getDistanceSq(x, y, z) < d1)`. То же вышло с поршнем (по классу, а не по метаданным) и с диалогом экспериментального мира (по level stem, а не по генератору). **Оракул — единственный авторитет, а комментарии самого порта доказательством не являются.**
- **Оракул восстановлен побайтово в этой среде:** `java -jar cfr-0.152.jar Thaumcraft-1.7.10-4.2.3.5.jar --outputdir <dir>` с умолчаниями даёт **859 файлов из 859**, совпадающих с `sha256_lf` и числом строк, которые ledger уже записывает; поэтому `scripts/porting/extract_tc4_definitions.py --check` и `scripts/porting/update_scope_ledger.py --check` действительно выполняются (`stale_review_fingerprints: 0`, 201 запись исследования, 294 текстовые страницы). **Дерево `OpenTC4` — только второе мнение и отличается от jar** (угол комнаты босса лабиринта, цвет вентиляции): три волны писали обратное как факт — это исправлено.
- **Проверки (реальный вывод).** `:common:test` — **2203 теста, 0 падений**; `:fabric:runGameTest` — **324 сценария, все пройдены** (CI-прогон `39714792328`, зелёный); манифест `scripts/porting/required-gametests.json` вырос с 310 до **316** обязательных случаев (шесть сценариев survival-прогрессии добавила волна); `:neoforge:build` — зелёный; `:neoforge:runGameTest` — 300 сценариев, 285 проходят, 15 падают (было 23/24 на базисе волны, и **ни одно** из оставшихся падений больше не является поведенческим расхождением только NeoForge); новый `NeoForgeLoaderParityTest` доказывает девять областей паритета исходник-против-исходника; `scripts/porting/convert_assets.py --verify` → `missing_references: 0`; Python-набор — 84/84; `update_scope_ledger.py --check` → `stale_review_fingerprints: 0`; выделенный сервер стартует («Done (15.319s)!») без ошибки в логе; свежий survival-мир генерирует дальние чанки (`forceload add 4000 4000` и `forceload add -6000 8000`, региональные файлы — доказательство) без исключения. Статический верификатор production-миксинов разрешает каждую `@At`-цель против настоящих перемапленных классов 1.21.1 — **все разрешаются**, поэтому ни один миксин не молча инертен.
- **Инфраструктурные решения волны.** Сервер GameTest теперь несёт явный `-Xmx2G` в `fabric/build.gradle` и `neoforge/build.gradle`: раньше JVM брала четверть ОЗУ машины, а это около 1.75 ГБ на 7-гигабайтном раннере CI, и набор умирал с `OutOfMemoryError` при сохранении чанка. **Мир GameTest, оставленный прерванным прогоном, делает несвязанные сценарии красными** — набор осмыслен только на чистом мире (`rm -rf fabric/run-gametest/world`).
- **Что осталось открытым.** Три клиентские возможности 1.7.10 без аналога в NeoForge (рендер чистой воды, модель `taint_fibres`, рендерер брони — четыре семейства брони оригинала: одежда пустоты, броня культиста, крепостная броня и грудь hover harness; нативный механизм NeoForge найден для каждой из трёх возможностей, но рюкзак hover harness нативного выражения не имеет); три пробы GameTest NeoForge, которым нужен свой наблюдатель `BEFORE`/`CANCELED`/`AFTER`; тик `server-level-post` сценария исследовательского стола на NeoForge; **арканная печь и четыре мясных кусочка** (`nugget_beef`, `nugget_chicken`, `nugget_fish`, `nugget_pork`) — единственный пробел замыкания, который нашла survival-приёмка, из-за него не собирается тройное мясное лакомство; две клиентские возможности 1.7.10, у которых нет аналога в 1.21.1 (драйвер групп шейдеров OptiFine/Iris и вытеснение 2000 частиц на слой у оригинального `ParticleEngine`; у `ClientTickEventsFML` нет и нативного эквивалента — строки аспектов над наведённым слотом контейнера); фикстура сцены `ALCHEMY` в харнессе (ванильный огонь в 1.21.1 выгорает — порт прав, хрупка фикстура); **второй физический клиент не запускался**, поэтому поведение «второго игрока» над настоящим соединением не проверено.
- Подробные отчёты по полосам — `docs/analysis/2026-10-23-agent-*.md`, сводка волны — `docs/analysis/2026-10-23-wave-16-agents.md`.

## 2026-10-23: волна 23.10 — третья волна: лестница целей голема, арканная печь, арбитраж двух вердиктов

- **Волна.** Третья волна субагентов — одиннадцать полос: `w3clientrender` (25 клиентских единиц рендера, десять закрыты), `w3clientfx` (17 единиц клиентских эффектов, GUI и HUD), `w3golems` (23 единицы големов, двенадцать закрыты), `w3rest` (28 серверных и общих `partial`), `w3neo` (пробелы NeoForge), `w3survival` (замыкание каталога выживания), `w3multi` (харнесс двух клиентов), `w3arb` (арбитраж), `w3levitator` (искусство предмета левитатора), `w3fix3` (три теста волны, противоречившие оригиналу) и полоса `memory` (рост кучи GameTest-сервера). Отчёты по полосам — `docs/analysis/2026-10-23-agent-w3*.md` и `docs/analysis/2026-10-23-agent-memory.md`.
- **Ledger.** `parity_verified` 945 → **969**; `partial` 86 → **62**; `missing` 10; `not_assessed` 3; `needs_review`/`unreviewed` — **ноль**. Всего 1044 единицы; разбивка 327 replace / 290 port / 263 adapt / 164 exclude. Счётчик **пересчитан из самого файла** `docs/porting/scope-ledger.json` (поле `review.native_coverage` каждой из 1044 строк), а не взят из отчёта.
- **Лестница целей голема была устроена неверно — и перестроена.** Оригинал регистрирует **пять записей селектора на каждое инвентарное ядро** (Fill, Empty, Sorting) с битами взаимного исключения 3 (оракул `thaumcraft/common/entities/golems/EntityGolemBase.java:266-351`), а проверка взаимного исключения 1.7.10 — это **побитовое И** `(mutexA & mutexB) == 0` (`net.minecraft.entity.ai.EntityAITasks`, официальный `client.jar` Mojang 1.7.10, `func_75777_a`), а не равенство чисел. Следствие: лестница (3) исключает и полёт от крипера (1), и взгляд на ближайшего игрока (2), и возвращение домой (3), но **не** исключает простой взгляд (0). В нативном дереве был **один маршрутный гол на ядро**, а остальные четыре действия выполнялись в серверном тике, поэтому лестница никогда не удерживалась, а частичная передача повторялась на следующем тике решения. Новая `common/src/main/java/thaumcraft/common/lib/golems/GolemLadderRules.java` несёт биты, побитовый тест, `ladderFlags()` = `MOVE`+`LOOK` (это и есть биты 3 оригинала), `PARTIAL_HOLD = 200` (удержание лестницы на десять секунд после частичной передачи) и таблицу лестницы из 14 строк (5 + 5 + 4); `InventoryGolemGoals` переписан, `GolemChestInteraction` и `GolemHelper` — новые файлы.
- **Счётчик 200 есть не у всех записей оригинала.** У `AIHomeReplace`, `AIHomePlace` и `AIHomeTake` поля `count` **нет вовсе** (оракул `AIHomePlace.java:27-28`): их продолжение — это `shouldExecute() || countChest > 0`. Счётчик несут только записи постановки, взятия и три маршрута, и натив воспроизводит это **по каждой записи отдельно**, а не «у всех лестницы».
- **Остаточное различие лестницы названо прямо.** Видимую крышку сундука воспроизвести нельзя: в 1.21.1 её анимирует число **игроков** с открытым меню (`ChestBlockEntity.getOpenCount`), а у моба нет эквивалента флага 1.7.10 `TileEntityChest.field_70495_d`; пятитиковое окно (`countChest` = 5) и звуки открытия/закрытия воспроизведены.
- **Миграция перечисления `INode`.** `TileNode.NodeType`/`NodeModifier` удалены, `TileNode` реализует `INode` (оракул `thaumcraft/api/nodes/INode.java:12-29`, `thaumcraft/common/tiles/TileNode.java:75-77` — **сам узел оригинала реализует контракт**), а опубликованные `thaumcraft.api.nodes.NodeType`/`NodeModifier` стали каноническими: вложенные перечисления использовались в 166 местах в 25 файлах — все переведены. Личность узла остаётся UUID: оригинал выводил её из позиции (`dim:x:y:z`, оракул `TileNode:98-115`), порт хранит устойчивую случайную. Это **точное остаточное различие** двух единиц, и менять его нельзя, пока принятые миры и клиентский прогон несут UUID в NBT узла.
- **Арканная печь — последний пробел замыкания каталога.** Конструкт второго события (оболочка 3×3×3) теперь существует: `ArcaneFurnaceFormation` повторяет 27 объёмов-кандидатов (`WandManager.java:332-347`), чертёж и правило железной решётки (`:349-373`), метаданные ячеек (`:375-407`), цену 50 огня и 50 земли (`:337`) и гейт исследования `INFERNALFURNACE` (`:716-718`, `ConfigRecipes.java:89-92`, `ConfigResearch.java:181`). **Четыре мясных кусочка получили именованный природный источник** — бонус переплавки арканной печи (`ConfigRecipes.java:507-510`, выплата `TileArcaneFurnace.java:237-259`); в оригинале они **не являются добычей мобов**, единственный их потребитель — тройное мясное лакомство (`ConfigRecipes.java:410-413`). Сценарий замыкания теперь утверждает, что открытое множество ингредиентов без источника **пусто** (`SurvivalProgressionRuntimeChecks.java:320-333`) — это был последний пробел замыкания каталога.
- **Таблица исследований.** Копия разгаданной записки стоит **бумагу и чёрную краску** (оригинальный `Items.dye` с метаданными 0), а не чернильный мешочек: оракул `thaumcraft/common/tiles/TileResearchTable.java:398-422`. Порт исправлен в приёмнике сети, в коммите таблицы, в меню, в подсказке GUI и в двух пробах харнесса.
- **Постановка блока рядом с живым боссом снова отменяется.** Оракул `thaumcraft/common/lib/events/EventHandlerWorld.java:225-245` (Внешние земли, ячейка лабиринта 2..5, босс в радиусе 32). Правило живёт в `BossPlacementRules`, а половина — **один крюк на платформу** (`FabricBossPlacement` с событием Architectury `BlockEvent.PLACE`, `NeoForgeBossPlacement` с нативными `BlockEvent.EntityPlaceEvent`/`EntityMultiPlaceEvent`), потому что общий артефакт Architectury и событие NeoForge не имеют общей сигнатуры. Практор багрового культа носит маркер босса, как в оригинале (`EntityCultistLeader.java:70-72`).
- **Глаз капюшона стража — аддитивный, а «скрытый `HoodEye`» оказался ложной находкой.** Метод рендера оригинала (`ModelEldritchGuardian.java:428-465`) рисует **38 частей**, и `HoodEye` среди них нет: глаз рисуется **только для стража**, отдельным аддитивным проходом в масштабе 1.01 (`:467-479`, `glBlendFunc(GL_SRC_ALPHA, GL_ONE)`). Порт теперь рисует его через `RenderType.eyes(SKIN)` — аддитивный и эмиссивный тип рендера 1.21.1. Клиентский агент прочитал `isRender`, а не список рисования.
- **Полные кубы и пустые модели.** Арканное ухо было полным кубом, а должно быть **десятью коробками** (оракул `BlockWoodenDeviceRenderer:44-83`); пьедестал фокуса был пустой моделью, а должен быть **девятью перекладинами** (`BlockStoneDeviceRenderer:36-70`); резервуар эссенции был полным кубом, а должен быть **сосудом 12/16** (`BlockEssentiaReservoirRenderer:28-52`); **предмет** левитатора в порту был простым кубом, а должен быть кубом с двумя эмиссивными плитами. Все четыре переписаны по моделям оригинала.
- **Серверная половина payload'а блеска блока.** Её не было вовсе: очищатель флюкса убирал блок флюкса молча. Новые `BlockSparkleFxPayload`, `BlockSparkleFxNetworking`, `BlockSparkleParticle` и `BlockSparkleClientEffects` дают **все шесть** отправителей оригинала с их цветами: очищатель флюкса `0xDD00FF` (`TileFluxScrubber.java:100`), эльдритч-замок `0x400040` (`TileEldritchLock.java:130`), охраняющий фокус `16556032` (`ItemFocusWarding.java:137,151`), палочка `-9999` (`WandManager.java:244,245,420,793-800`), страж `0x800080` (`EntityEldritchWarden.java:231-234`) и серверный тик `0xC0C0FF` (`ServerTickEventsFML.java:154`), радиус 32 блока. Из той же таблицы выпал второй дефект: страж в 70 % случаев не слал **ничего**.
- **Банка узла и банка мозга получили block entity renderers.** В нативном дереве их не было вообще, а `ModelBrain` и `ModelJar` были мёртвым кодом. Новые `TileJarNodeRenderer` (узел всегда виден, размер 0.7, тест глубины сохранён) и `TileJarBrainRenderer` (покачивание `sin(tick/14)*0.03+0.03`, поворот, масштаб 0.4, рассол) воспроизводят оригинал. «Односекундный рост узла в банке» — **мёртвый код оригинала**: масштаб применяется после всех вызовов рисования, а матрица снимается сразу (`TileJarRenderer.java:107-117`), поэтому расти не должен и натив.
- **Рост кучи GameTest-сервера.** Ванильная `GameTestHelper.makeMockServerPlayerInLevel()` добавляет настоящего подключённого `ServerPlayer` в список игроков и **никогда его не убирает**: игрок закрепляет билеты чанков до конца сессии, поэтому каждый завершённый сценарий держал чанки, которые генератор мира так и не дочитал. Порт теперь отпускает мок-игроков (`GameTestHelperMockPlayerMixin` на обеих платформах); число `ProtoChunk` по замеру падает с 21 522 до **20 105**. Второй дефект: `WardedBehaviour.original` читал блок, которого не видно (`common/src/main/java/thaumcraft/common/lib/world/WardedBehaviour.java:18`), а в `WorldGenRegion` такой запрос уходит к **серверному** источнику чанков и оставляет незавершённый `ProtoChunk` (117 таких запросов за прогон на волне, 138 на базисе — все из `WardedBehaviour.original`); чтение теперь отказывает, если блок не виден.
- **Ложное утверждение волны 2 о NeoForge исправлено.** NeoForge 21.1 **имеет** крюк рендера жидкости (`IClientFluidTypeExtensions.renderFluid(...)`, `IClientFluidTypeExtensions.java:347`), поэтому чистая вода **не** заблокирована отсутствующим событием. Она всё равно не годится для очищающей жидкости: жидкость обязана делить ванильный тип воды (сравнение по идентичности), а у типа воды уже есть своё клиентское расширение, а вторая регистрация для того же типа **роняет клиент на старте** (`ClientExtensionsManager.java:42`). Поэтому геометрия переехала в общий `PureFluidRender`, а NeoForge-половина — миксин `BlockRenderDispatcher#renderLiquid`. Попутно найден **скрытый краш старта NeoForge-клиента**: общий миксин `RobeArmorLayerMixin` перенаправлял `ItemStack.is(TagKey)`, а в пропатченном NeoForge-классе этого вызова нет, а `@Redirect` без цели **валит применение миксина** (`defaultRequire: 1`) — клиент упал бы при старте. Поведение разделено по загрузчикам.
- **Три теста волны противоречили оригиналу — виноваты были тесты, не порт.** `MathHelper.func_76134_b` 1.7.10 — это **таблица синуса на 65 536 значений** (та же, что `Mth.sin` в 1.21.1: `SIN[(int)(f * 10430.378f) & 0xFFFF]`), и собственный синус оригинала уходит **ниже** границы, которую утверждали тесты: покачивание мозга ходит от нуля до 0.06, и **248 из 512** тиков ниже 0.03. То же с поворотом мозга (оригинал рисует абсолютный интерполированный угол, а не разность) и с оболочкой арканной печи (воздух в середине верхнего слоя, лава в среднем, обсидиан в нижнем — тест применял правило верхнего слоя ко всем трём). Ни одно утверждение не ослаблено и ни один тест не удалён.
- **Арбитраж: два вердикта, которые противоречили друг другу.** `w3clientrender` утверждал, что предмет левитатора — куб и две светящиеся плиты `animatedglow` (0.01..0.99×0.9..0.99 с цветом `BlockCustomOreItem.colors[4]` = 0x00A000 и 0.01..0.99×0.1..0.9 с цветом `colors[5]` = 0xEECCFF; оракул `BlockLifterRenderer.java:32-53`, лист `thaumcraft:animatedglow` — `BlockLifter.java:56`), а `w3levitator` — что это простой куб. Решено **по оракулу и официальным jar'ам**: `BlockLifterRenderer.renderInventoryBlock` действительно рисует куб и две плиты, а официальный `client.jar` 1.7.10 отправляет инвентарный рендер блока именно в этот метод (`RenderItem` → `RenderBlocks.renderBlockAsItem` (`func_147800_a`) → патч Forge → `ISimpleBlockRenderingHandler.renderInventoryBlock`), и 1.12-наследник того же метода — ровно та же модель с двумя элементами. **Вердикт `w3clientrender` верен, ожидание теста `w3levitator` было неверным.** Второй спор — ось зеркала: 1.7.10 `ModelBox` меняет местами min/max X и переворачивает каждый quad (`bis.java:33-37, 60-64`), итоговая норма — `(-nx, ny, nz)`, а это в точности `normal.mul(-1,1,1)` 1.21.1 (`ModelPart.java:340-343`): **порт был прав, неверна была константа теста** (зеркало считалось вокруг 2.0 — модельных единиц 1.7.10 — вместо 2/16 = 0.125 мировых единиц 1.21.1). Оба спора закрыты **без единого изменения production-кода**.
- **Правило, которое вывела волна.** Когда два агента противоречат друг другу, решает **оракул и официальные jar'ы** — не голосование большинством и не комментарии самого порта. Это прямое продолжение правила волн 1–2 («оракул — единственный авторитет»).
- **Проверки (реальный вывод).** `:common:test` — **2252 теста, 0 падений** (прогон полосы `w3arb` по слитому дереву; до арбитража тот же набор давал 2252 теста и 2 падения; **339** классов — так в отчёте и так в самом дереве: 339 файлов с `@Test` в `common/src/test/java`; в более раннем отчёте `w3levitator` стояло 337 при 2237 тестах — это счётчик того прогона). `:fabric:runGameTest` — **325 сценариев**, из них **три сценария големов сейчас красные** из-за регрессии переписывания големной лестницы волны (отдельный агент её чинит) — **набор не объявляется зелёным**. `:neoforge:build` — зелёный; `:neoforge:runGameTest` — по отчёту полосы `w3neo`: **306 сценариев, 300 проходят, 6 падают** (на базисе волны 2 было 300 / 286 / 14), и три пробы раскопки, которым был нужен свой наблюдатель `BEFORE`/`CANCELED`/`AFTER`, теперь зелёные. `convert_assets.py --verify` → `missing_references: 0`; Python-набор — 84/84; `update_scope_ledger.py --check` → `stale_review_fingerprints: 0`; манифест `scripts/porting/required-gametests.json` — 316 → **317** обязательных случаев (сценарий арканной печи добавила полоса выживания).
- **Инфраструктура.** Сервер GameTest несёт явный `-Xmx2G` в `fabric/build.gradle` и `neoforge/build.gradle`, и набор осмыслен **только на чистом мире** (`rm -rf fabric/run-gametest/world`). По замеру полосы `memory` даже после двух исправлений набору локально нужно 3 ГБ (`-Xmx3000M` проходит, `-Xmx2G` всё ещё даёт OOM), и причина измерена: волна генерирует примерно на **450 чанков** больше базиса, а каждый сгенерированный чанк тащит в карту сервера девять полуготовых `ProtoChunk` (20 105 / 9 ≈ 2 234 против 16 031 / 9 ≈ 1 781), и финальное сохранение пишет NBT примерно 20 000 чанков разом (около 1.4 ГБ в гистограмме кучи). Следующий шаг — найти сценарий, который даёт эти лишние ~450 чанков, и заставлять сценарии генерации отпускать чанки по ходу.
- **Что осталось открытым.** Три красных сценария големов (регрессия лестницы волны 3, чинит отдельный агент) — и потому `:fabric:runGameTest` **не зелёный**; модель `taint_fibres` на NeoForge (нужен миксин в нативный блочный рендерер; место инъекции найдено и проверено, но видимый регресс нельзя проверить без клиента); **все клиентские изменения NeoForge этой волны визуально не проверены** (чистая вода, одежда пустоты, крепостная броня, грудь hover harness, краска одежды — критерии приёмки перечислены в отчёте `w3neo`); единицы големов, остающиеся `partial` (набор жидкостей Decanting и Alchemy, отмеченные переносы эссенции, три переключателя ядра Use, текстура булавки); единицы `w3rest`, остающиеся `partial` (очередь регенерации чанков оригинала, события создания предмета в 1.21.1 нет вовсе, пассив прокрутки эссенции, поверхность зеркала на фиксированной функции, страница чертежа арканной печи, идентичность узла как UUID); две клиентские возможности 1.7.10, у которых нет аналога в 1.21.1 (драйвер групп шейдеров OptiFine/Iris и вытеснение 2000 частиц на слой у оригинального `ParticleEngine`; у `ClientTickEventsFML` нет и нативного эквивалента — строки аспектов над наведённым слотом контейнера); **второй физический клиент в этой волне по-прежнему не запускался** — харнесс двух клиентов переписан (пять дефектов самого харнесса найдены и исправлены), но приёмка второго клиента не выполнена.
- Подробные отчёты по полосам — `docs/analysis/2026-10-23-agent-w3*.md` и `docs/analysis/2026-10-23-agent-memory.md`.

## 2026-10-23: волна 23.10 закрыта — лестница голема починена, харнесс изолирован, клиент и мультиплеер приняты, ledger 976

- **Закрытие волны.** Третью волну (11 полос, её подробный отчёт — предыдущая запись) довели до конца ещё четыре полосы: `golemfix` (регрессия переписывания големной лестницы), `isolate` (дефект харнесса GameTest), `memory` (рост кучи сервера GameTest) и `w3multi` (второй физический клиент). Отчёты — `docs/analysis/2026-10-23-agent-golemfix.md`, `docs/analysis/2026-10-23-agent-isolate.md`, `docs/analysis/2026-10-23-agent-memory.md`, `docs/analysis/2026-10-23-agent-w3multi.md`.
- **Ledger.** `parity_verified` 969 → **976**; `partial` 62 → **55**; `missing` 10; `not_assessed` 3; `needs_review`/`unreviewed` — **ноль**. Всего 1044 единицы; разбивка 327 replace / 290 port / 263 adapt / 164 exclude. На старте всего усилия было 653 / 315 / 58 / 18 — закрыто **323** единицы. Счётчик **пересчитан из самого `docs/porting/scope-ledger.json`** (поле `review.native_coverage` каждой из 1044 строк), а не взят из отчёта.
- **Регрессия лестницы целей голема и её точная причина.** Переписанная волной 3 лестница дала каждой записи свой крюк действия `ladderStart()`, который **никто не подключил** к ванильному `Goal.start()` — а в 1.21.1 это **пустая** функция (`net.minecraft.world.entity.ai.goal.Goal`: `public void start() { }`). Следствие: каждая запись забирала два исключительных флага лестницы (`MOVE` и `LOOK`) и **не делала ничего** — голем стоял на месте. 1.7.10 вызывает `startExecuting` селектора ровно там, где 1.21.1 вызывает `wrappedGoal.start()` (оракул `thaumcraft/common/entities/golems/EntityGolemBase.java:266-351` — пять записей селектора на каждое инвентарное ядро с битами взаимного исключения 3). Действие записи теперь **и есть** её `start()`, и оно **абстрактно** в базе записей, так что компилятор отказывает записи без действия: `common/src/main/java/thaumcraft/common/entities/golems/InventoryGolemGoals.java:293` (`@Override public abstract void start();`). Бисект по четырём точкам на чистом мире: ветвление `61d44d6b` — 3 из 3 зелёные, миграция `INode` `d94f4cd2` — 3 из 3 зелёные, переписывание лестницы `1549c855` — **3 из 3 красные**, исправление `598902ce` — 3 из 3 зелёные. Регрессионный тест `GolemLadderGoalStructureTest` (8 тестов) падает, если у записи нет действия; охрана **доказана** намеренным возвратом формы волны 3.
- **Дефект харнесса, убивавший големов соседнего сценария.** Общая фикстура `WaveLane` (36 сценариев) убирала за собой **шестнадцать блоков** и выбрасывала каждого `Mob`, `ItemEntity` и `Projectile` — а голем это `Mob` (`EntityGolemBase extends Mob`). Ванильная сетка GameTest упаковывает сценарии **на 13 блоков** друг от друга (пустая структура Fabric-API 8×8×8 плюс поле в 5 блоков, `StructureGridSpawner.spawnStructure` двигает следующую структуру на `bounds.getXsize() + 5`), а полоса смещена на 3 блока внутрь своей структуры — **ровно 16**. Второй дефект того же механизма: принудительные чанки уровня — это **множество, а не счётчик**, поэтому освобождение чанков одной полосы выгружало кольцо, которое другой полосе ещё было нужно, и у голема пропадала область навигации (`EntityGolemBase.loadedNavigationArea`, `EntityGolemBase.java:1021-1033`; маршрут отказывается стартовать, `InventoryGolemGoals.java:722`). Исправление — **только в харнессе**, ни одно утверждение не ослаблено: радиус уборки полосы стал **5** блоков (`common/src/gameTest/java/thaumcraft/common/testing/WaveLane.java:40`, `CLEAN_RADIUS = 5`), три големных сценария забирают своё кольцо чанков обратно (`GolemChunks.java:43`, `refresh(...)`) и **взводят** голема заново, если он был выброшен; новая диагностика `GolemState` печатает в сообщении об отказе `removed=… alive=… area=… work=… fettered=… nav=…`. Охрана `GameTestIsolationTest` (3 теста) доказана: с радиусом 16 первый тест падает.
- **Проверки (реальный вывод).** `:common:test` — **2272 теста, 0 падений**; `:fabric:runGameTest` — **325 сценариев, все пройдены** на чистом мире (прогон лидера, 14:39); манифест `scripts/porting/required-gametests.json` — **317** обязательных случаев; `:neoforge:build` — зелёный; `:neoforge:runGameTest` — по отчёту полосы `w3neo`: **306 сценариев, 300 проходят, 6 падают** (на базисе волны 2 было 300 / 286 / **14**), и три пробела волны 2 теперь зелёные: пробы раскопки, общие фазы разрушения блока и общая геометрия жидкости; `scripts/porting/convert_assets.py --verify` → `missing_references: 0`; Python-набор — 84/84; `update_scope_ledger.py --check` → `stale_review_fingerprints: 0`. Оракул восстановлен **побайтово** в этой среде: 859 файлов из 859 совпадают с отпечатками, которые ledger уже записывает.
- **Клиентская приёмка закрыта.** Smoke-харнесс порта дошёл до `SCRIPT_PASSED` с **66 кадрами из 66**; 65 сверены с оригиналом и прошли, 66-й (наложение двух строк в радиальном фокусе) исправлен после прогона: центральная строка радиального меню называет **только** сфокусированный фокус, а когда фокуса нет — не рисуется вовсе (`common/src/main/java/thaumcraft/client/gui/GuiFocusSelect.java:210-216` и `:258-264`; причина измерена в `docs/analysis/2026-10-23-agent-focuslabel.md`: экран рисуется в кадр дважды, игровой проход с настоящим курсором и проход харнесса с подставленной точкой наведения, а содержимое центральной строки зависело от наведения).
- **Мультиплеер закрыт.** Настоящий выделенный сервер и **два** настоящих физических клиента (два отдельных JVM, два GL-контекста, два реестра payload) через loopback-сокет: **37 этапов из 37**, `SCRIPT_PASSED` на обоих клиентах. По сокету прошли **14 из 21** clientbound и **10 из 12** serverbound каналов payload TC4. Приёмка не нашла ни одного дефекта общего слоя: снимок знаний и снимок аксессуаров — **на игрока** (чужим отказано; колокол голема — оригинальными словами «You are not my Master!», и колокол чужого голема не берёт его), партнёрская половина исследовательского стола открывает главную половину у обоих игроков, а правило ближайшего игрока оригинала не находит творческого игрока. Девять дефектов самого харнесса найдены и исправлены (в их числе census, терявший пакеты залогина, и чтение пары двумя чтениями).
- **Уборка истории.** Один агент (`w3fix3`) закоммитил по ошибке свой частный Gradle-home `.gradle-fix3/` (**1.1 ГБ**): коммит `af442838` попал в `refs/heads/work` при слиянии в 11:47 (`docs/analysis/2026-10-23-agent-w3fix3.md` §7). Лидер убрал каталог из вершины и вычистил его из истории `git filter-repo --path .gradle-fix3 --invert-paths` с последующим `git reflog expire --expire=now --all && git gc --prune=now`: репозиторий **с 845 МБ до 115 МБ**, потребовался принудительный пуш `work`. Остальные ветки (`main` и две `claude/*`) не затронуты.
- **Правило, которое вывела волна (оно же — правило всего усилия).** Когда два агента противоречат друг другу, решает **оракул и официальные jar'ы** — не голосование большинством и не комментарии самого порта. Три волны подряд написали одно и то же неверное утверждение как факт, прежде чем его установили чтением **оригинального исходника 1.7.10**.
- **Что осталось открытым (с точной привязкой).**
  - **55 `partial` и 10 `missing` / 3 `not_assessed`.** Каждая из 55 несёт в ledger своё **точное остаточное различие**; разбивка по семействам подсистем: `golems_and_tasks` — 11, `client_rendering_fx_input_and_gui` — 9, `wands_nodes_and_vis` — 8, `research_scanning_and_knowledge` — 6, `alchemy_and_essentia` — 6, `advanced_devices_and_visnet` — 4, `base_content_and_small_interactions` — 3, `shared_infrastructure` — 2, `natural_resources_and_worldgen` — 2, `aspect_contracts_and_data` — 1, `warp_creatures_and_ecology` — 1, `eldritch_and_outer_lands` — 1, `foci_equipment_and_enchantments` — 1. Все десять `missing` и все три `not_assessed` — намеренные `exclude` с доказательством (пакет JEI 1.12, помощники позы палочки 1.12, эльдритч-диагностика FPS, рендерер эльдритч-предметов, плагин WAILA).
  - **Клиент NeoForge визуально не проверен** — в этой среде нельзя запустить NeoForge-клиент (критерии приёмки перечислены в `docs/analysis/2026-10-23-agent-w3neo.md` §5), и модель `taint_fibres` на NeoForge (место инъекции найдено — `ModelBlockRenderer#tesselateWithoutAO`, `HEAD`, — но без клиента видимый регресс не проверить).
  - **Семь clientbound и два serverbound канала payload не наблюдались на проводе** — машины не были построены (палочка с визом, матрица инфузии, лампа роста, vis-сеть, рунный буржет, фокальный манипулятор, копия записи стола).
  - **Видимая крышка сундука голема** — в 1.21.1 её анимирует число **игроков** с открытым меню (`ChestBlockEntity.getOpenCount`), а у моба нет эквивалента флага 1.7.10 `TileEntityChest.field_70495_d`; пятитиковое окно и звуки открытия/закрытия воспроизведены.
  - **Идентичность узла** — оригинал выводит её из позиции (`dim:x:y:z`, оракул `TileNode:98-115`), порт хранит устойчивый случайный UUID; формат **вшит** в принятые миры и в клиентскую приёмку (`fabric/src/clientTest/java/thaumcraft/fabric/clienttest/FocalManipulatorClientSequence.java:509` разбирает идентичность через `UUID.fromString`).
  - **Куча сервера GameTest.** Набору из 325 сценариев нужно **3 ГБ** на нагруженной машине: `-Xmx2G` умирает с `OutOfMemoryError` в пути сохранения чанка (`PalettedContainer.pack`), `-PgameTestHeap=3G` проходит (`fabric/build.gradle:240-263`); раннер CI имеет 7 ГБ и проходит с явным `-Xmx2G` — его куча по умолчанию (четверть ОЗУ, около 1.75 ГБ) не хватает. Набор осмыслен **только на чистом мире**: `rm -rf fabric/run-gametest/world`.
- Подробные отчёты по закрывающим полосам — `docs/analysis/2026-10-23-agent-golemfix.md`, `docs/analysis/2026-10-23-agent-isolate.md`, `docs/analysis/2026-10-23-agent-memory.md`, `docs/analysis/2026-10-23-agent-w3multi.md`.

## 2026-10-23: волна 4 — 27 `partial` разобраны, 10 тестов arbitrated, ledger 1006

- **Шесть полос четвёртой волны.** `w4research` (6 юнитов), `w4golems` (11), `w4wands` (9), `w4alchemy` (7), `w4client` (11), `w4devices` (11) — плюс две полосы арбитража (`w4fix5`, `w4fix5b`) на десять красных тестов. Отчёты — `docs/analysis/2026-10-23-agent-w4*.md`.
- **Ledger.** `parity_verified` 976 → **1006**; `partial` 55 → **27**; `missing` 8; `not_assessed` 3. Счётчик пересчитан из самого `docs/porting/scope-ledger.json`.
- **Дефекты поведения, найденные чтением оригинала (не «похоже на оригинал»).**
  - **`EntityItemGrate` — семантика была инвертирована.** Переопределение оригинала отвечает `true` на `pushOutOfBlocks`, а ваниль 1.7.10 кладёт этот ответ в `Entity.field_70145_X` = **`noClip`** (MCP), и `Entity.moveEntity` пропускает столкновение: предмет **проваливается сквозь решётку**. Порт, наоборот, **выталкивал** предмет импульсом 0.1–0.3. Оракул `EntityItemGrate.java:26-36`, ванильные `sa`/`xk` из официального `client.jar` 1.7.10.
  - **Поворот головы банки мозга шёл по оси Z, а не по Y.** Оракул `TileJarRenderer:166-167`: **оба** вызова `glRotatef(angle,0,1,0)` — ось **Y**.
  - **Клиентское событие 9 банки узла отсутствовало.** Оракул `TileJarNode:204-219`: 3×4×3 = 36 искр, по 5 частиц каждая, случайный серый, таймер 1 с. «Рост узла на 3» — **мёртвый код оригинала** (`TileJarRenderer:107-115` масштабирует после всех вызовов рисования и снимает матрицу), натив расти не должен.
  - **Уровень света банки узла 9 → 11** (оракул `BlockJar:314-320`); **у банки мозга не было выхода компаратора и зелёной частицы** (`BlockJar:340-351`, `:322-334`).
  - **У тигля не было ни бака жидкости, ни события крафта.** Оракул `TileCrucible:61-71,373-410` и `:253-259`; теперь Fabric `FluidStorage.SIDED` и NeoForge `Capabilities.FluidHandler.BLOCK`, крафт стреляет `PlayerEvent.CRAFT_ITEM` (липкая порча достигает бросившего).
  - **Пакета потока эссенции не существовало вообще** — ни серверной, ни клиентской половины. Оракул `EssentiaHandler:54`, `PacketFXEssentiaSource:20-78`, `ClientTickEventsFML:249-269`; дельты байт перестраиваются по-оригинальному, хвост сжимается как `ticks²/25`.
  - **Руны скана не рисовались** (ни сервер, ни клиент). Оракул `ItemThaumometer:92,103,128` и `ClientProxy:918-922`: одна `FXBlockRunes` в `x+0.5,y+0.5,z+0.5`, руна сущности в `x-0.5, y+eyeHeight/2, z-0.5` с длительностью `(int)(height*15)`, руна блока/узла в `y+0.25` с длительностью 15 и гравитацией 0.03, цвет из **клиентского** случайного.
  - **Скан запускался на дубликат цели.** Оракул `ScanManager.isValidScanTarget:277-315`: повторная цель не должна начинать скан. Портал шёл 20 тиков камеры и отказывал только в конце.
  - **Одежда пустоты надевалась «одной подложкой»** — порт рисовал только обрезку, причём тонированную, и никогда не рисовал окрашенную ткань. Теперь окрашенная накладка + неокрашенная обрезка, в оригинальном порядке (1.7.10 `RenderPlayer:bop:52-100`, `RenderLivingBase:boh:113-121`).
  - **Предмет узла был статичной текстурой осколка** вместо оригинального анимированного узла (два скрещённых квада 32-кадрового атласа).
  - **`thaumium_nugget` и `void_nugget` имели модель, текстуру, язык и тег, но **никогда не были зарегистрированы** — два битых тега и четыре битых рецепта. Все четырнадцать предметов существуют, каталог воспроизведён по оракулу `ItemNugget.java:62-88` и `ConfigRecipes.java:402,404`.
  - **Сундуки генерации несли ванильную таблицу 1.21.1** вместо категории Forge `dungeonChest` — 15 записей ванильного `WorldGenDungeons`, зачарованная книга Forge и дополнения TC4, 8 предметов за бросок, случайный слот (оракул `WorldGenHilltopStones.java:86-93`, `WorldGenMound.java:2518-2536`).
  - **Ядро Use голема было «закрытой единицей»** (только рычаг/кнопка/пашня) — оригинал берёт **любой** предмет на **любой** отмеченной клетке и читает три переключателя (оракул `AIUseItem:135,142-146,155-157,190,192`; состояние сервера `ContainerGolem:92-94`, кнопки 50/51/52; рисование `GuiGolem:206-236`).
  - **Набор назначений эссенции был «банка или резервуар»** — оригинал платит **любому** отмеченному `IEssentiaTransport` на первой принимающей отмеченной стороне (оракул `AIEssentiaEmpty:64-112`, `GolemHelper:245-248`).
  - **Таблица почвы растения была таблицей Forge без случая Cave** и принимала подзол и грубую землю. Воспроизведена ровно: `BlockBush` (трава/земля/пашня) для всех типов, **песок для Desert**, `isSideSolid(world,x,y,z,UP)` для Cave.
  - **Наблюдатель (`watcher`) имел рендерер**, хотя оригинал рендерер **не регистрировал** вовсе — теперь рисует ничего.
  - **Дырка в портативном виде не имела искр**, и у фокуса удара их не было в точке попадания; добавлены `TypeSparkleParticle` (цвета типов, жизнь `3*multiplier`, кадры `16+age/multiplier`, сжатие, альфа 0.75, гравитация) и 24 вызова оракула `TileHole.java:145-210`.
  - **Страницы массивов книги исследования** (BANNERS, PRIMALARROW, ORE) не рисовались, хотя оригинал строит два разных массива одним и тем же выражением `rc.toArray(new IRecipe[0])`.
- **Четыре утверждения ledger'а оказались неверными, а не «неполными».** `AuraChunk` — в TC4 **нет** почанковой ауры вовсе (`TileNodeEnergized:27,165`); `AuraHandler` — его единственный рабочий метод сливает **vis-сеть**, и нативная проекция теперь делает то же (`AuraHelper:57-60`); луч `FXBeamWand` **не начинается** с кончика палочки (`FXBeamWand:209-228` = глаз + взгляд·0.3), и натив уже использовал это начало; `WandEffectOrigin`/`FirstPersonWandTipOrigin`/`WandPoseMath`/`WandUsePose`/`WandUsePoseSampler` — помощники 1.12, которых у оригинала нет. Ни один контракт **не выдуман**: публикаций `IGolemInfo`, `AuraChunk`, `AuraHandler` и всего пакета `thaumcraft/client/integration/jei` в оракуле нет (каталога `jei` не существует) — они закрыты как осознанный `exclude` с этим доказательством.
- **Арбитраж десяти красных тестов: виноваты восемь тестов и два продакшена.**
  - `LangParityTest` — **продакшен**: шесть ключей `tc.port.golem.use.*` добавлены только в `en_us.json`. Оригинал рисует три подписи Use **жёстко вшитым английским** (`GuiGolem:206-227`), а в `assets/thaumcraft/lang/ru_RU.lang` оригинального jar'а ключа для них **нет** — значит имена принадлежат порту, и их нужно перевести.
  - `ItemNodeRendererTest` ×2 — **тест**: оракул `ItemNodeRenderer:115` — `sin(ticks/(14-count))*0.25+0.5`, делитель `14-count`; и `:116` — `0.2 + scale*(amount/50)`, то есть при 100 эссенциях и пульсе 0.5 размер **1.2**, а не 1.0.
  - `GolemUseRulesTest` — **тест** (инвертированный булев): `AIUseItem:190` и байт переключателя по умолчанию 0 (`EntityGolemBase:396`) → по умолчанию режим блока: блок принимает, пустое пространство отвергает.
  - `ExcavationBeamRulesTest` — **тест** (приоритет операций): `FXBeamWand:171` — `%` и `*` левоассоциативны, то есть `(worldTime % 72)*5 + 5*partial`: на 71-м тике 355°, на 72-м возврат **к 0**, а не к 360.
  - `JarBrainTurnAxisParityTest` — **продакшен**: второй поворот оригинала кладёт мозг набок (ось X).
  - `EssentiaFlowPacketParityTest` ×2 — **продакшен**: дельта байта знаковая и оборачивается, фаза считает назад по сжимающемуся хвосту.
  - `CrucibleFluidTankParityTest` — **продакшен**: бак отвечает **принятым** количеством.
  - `NormalRecipeParityTest` — **тест**: компрессия железа оригинала — только ванильная пара (`iron_ingot_from_nuggets` в 4.2.3.5 не зарегистрирован).
  - Ни одно утверждение не ослаблено и ни один тест не удалён.
- **Агенты не могли выполнить набор — поэтому их тесты приехали непроверенными.** Каждый агент волны 4 писал тесты по оригиналу, но не мог запустить Gradle (общий кэш Loom, 15 ГБ RAM, OOM-killer). Все десять падений разобраны в двух отдельных полосах арбитража, полосы запускали Gradle в собственном частном Gradle-home.
- **Дефект инструмента слияния волн найден и починен.** `scripts/porting/merge_wave_branch.py` при юнион-слиянии оставлял заметки и evidence только если у принимающей стороны их «не было» — то есть **терял аудит каждой строки, к которой прикасалась любая волна**; там же терялись **юниты, которые добавила только ветка**. Починено (union для evidence, «более новая сторона» по merge-base для notes, добавление новых юнитов) и покрыто регрессионным набором `scripts/porting/test_merge_wave_branch.py` — **90 Python-тестов**.
- **Агент клиентской полосы умер, не закончив отчёт.** Его последний коммит (`d851f5c0`: наблюдатель не рисует ничего, `UtilsFX` с прогрессом экипировки и атласом частиц, отступ 0.005 у накладок зачарованных камней) **не попал в слияние** — сливали его ветку в момент, когда он коммитил. Работа была найдена в истории ветки и восстановлена слиянием; её три юнита остались `partial` и переданы полосе пятой волны.
- **Проверки (реальный вывод).** `:common:test` — **2350 тестов, 0 падений** (360 классов); `:fabric:runGameTest` — **325 сценариев**, из них **четыре красные** (устаревшие утверждения после изменений волны 4: правило окклюзии скана, правило перенацеливания, и правило почвы растения, которое волна 4 изменила вместе с оракулом) — арбитраж отдельной полосой; `update_scope_ledger.py --check` → `stale_review_fingerprints: 0`; `extract_tc4_definitions.py --check` → 201 запись исследования, 294 текстовые страницы, 48 аспектов, закреплённые хеши.
- **Правило волны 4 подтверждено девять раз.** Каждая полоса обязана доказать своё утверждение **оригинальным исходником** (оракул, официальный `client.jar` 1.7.10, официальный MCP-маппинг, патчи Forge 1.7.10). Четыре «остаточных разных» из ledger'а на поверку оказались **неверными утверждениями ledger'а** — их опровергли, а не закрыли. Ни один контракт не выдуман: там, где у оригинала не было публикации, юнит закрывается как осознанный `exclude` с доказательством отсутствия.
