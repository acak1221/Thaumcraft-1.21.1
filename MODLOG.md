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
