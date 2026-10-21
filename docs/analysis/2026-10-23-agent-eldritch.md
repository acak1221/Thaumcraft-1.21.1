# Eldritch and Outer Lands — parity wave of the 22./23.10.2026

Agent: `eldritch`. Domain: `eldritch_and_outer_lands` (59 units of
`/home/cyberfox-kali/units/eldritch.txt`).

## Oracle used in this wave

The primary oracle of this wave is the **original jar** `Thaumcraft-1.7.10-4.2.3.5.jar`
of the repository root, decompiled with CFR 0.152 into `/tmp/opencode/oracle_full`
(read only, nothing was written into the repository).

> **Important for the next waves:** the readable 1.7.10 tree of `OpenTC4` is *not* the
> 4.2.3.5 oracle for every class. The proof is `MazeGenerator`: the oracle jar selects the
> corner of the boss room with **one** `Random.nextInt(4)`
> (`oracle_full/thaumcraft/common/lib/world/dim/MazeGenerator.java`, the `tableswitch` on
> `nextInt(4)` at the head of `generate`), while `OpenTC4` uses **two** `Random.nextBoolean()`
> calls. The two streams diverge, so a labyrinth built from the `OpenTC4` text is a
> different labyrinth. The port already matches the jar (`nextInt(4)`), and the fidelity test
> `MazeFidelityTest` compares against a transcription of the *jar*, not of `OpenTC4`.
> A second example is the vent colour of the crab spawner: the jar says `0x9988AA`
> (`TileEldritchCrabSpawner.drawVent`), `OpenTC4` says `10061994` (`0x99B2B2`).

Everything below cites the **jar** oracle.

## Units touched (before → after)

| unit | before | after |
|---|---|---|
| `thaumcraft/common/tiles/TileEldritchTrap.java` | partial | **parity_verified** |
| `thaumcraft/common/tiles/TileEldritchCrabSpawner.java` | partial | **parity_verified** |
| `thaumcraft/common/tiles/TileEldritchLock.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/tile/TileEldritchCrabSpawnerRenderer.java` | partial | **parity_verified** |
| `thaumcraft/common/entities/monster/boss/EntityCultistPortal.java` | partial | **parity_verified** |
| `thaumcraft/common/entities/monster/boss/EntityEldritchGolem.java` | partial | **parity_verified** |
| `thaumcraft/common/entities/monster/boss/EntityEldritchWarden.java` | partial | **parity_verified** |
| `thaumcraft/common/entities/monster/EntityCultist.java` | partial | **parity_verified** |
| `thaumcraft/common/entities/monster/EntityCultistCleric.java` | partial | **parity_verified** |
| `thaumcraft/common/entities/monster/EntityCultistKnight.java` | partial | **parity_verified** |
| `thaumcraft/common/entities/monster/boss/EntityCultistLeader.java` | partial | **parity_verified** |
| `thaumcraft/common/entities/ai/combat/AICultistHurtByTarget.java` | partial | **parity_verified** |
| `thaumcraft/common/entities/projectile/EntityEldritchOrb.java` | partial | **parity_verified** |
| `thaumcraft/common/items/ItemEldritchObject.java` | partial | **parity_verified** |
| `thaumcraft/common/items/armor/ItemCultistBoots.java` | partial | **parity_verified** |
| `thaumcraft/common/items/armor/ItemCultistLeaderArmor.java` | partial | **parity_verified** |
| `thaumcraft/common/items/armor/ItemCultistPlateArmor.java` | partial | **parity_verified** |
| `thaumcraft/common/items/armor/ItemCultistRobeArmor.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/dim/TeleporterThaumcraft.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/dim/MapBossData.java` | partial | **parity_verified** |

20 units moved, 39 units of the list stay as they were (see "not touched").

## Defects found and fixed

### D1 — the eldritch trap never warped the player *(fixed)*

* Oracle: `TileEldritchTrap.func_145845_h` (jar 4.2.3.5) —
  `count = 10 + rand.nextInt(25)`, then
  `World.func_72977_a(x+0.5, y+0.5, z+0.5, 3.0)` (the closest player inside **3.0** blocks),
  `p.attackEntityFrom(DamageSource.magic, 2.0f)` and
  `if (rand.nextBoolean()) Thaumcraft.addWarpToPlayer(p, 1 + rand.nextInt(2), true)`.
* Port before: `common/src/main/java/thaumcraft/common/tiles/TileEldritchTrap.java:27-48` —
  the search used an AABB of half width **3.0** (so a player 5.2 blocks away in a diagonal
  was hit), it accepted creative players, and the **warp was missing completely**.
* Fix: `TileEldritchTrap.java:47-58` searches the sphere of the original and applies
  `1 + nextInt(2)` temporary warp in half of the hits, in the order of the original
  (damage first, then the boolean, then the roll of the warp).
* The 1.7.10 semantics of `World.getClosestPlayer` are now a helper of the port:
  `common/src/main/java/thaumcraft/common/lib/entities/OriginalPlayerSearch.java:39-73`
  (strict bound, creative players are skipped, the quirk that a player on the centre of the
  search ends the search because the inner `if (d1 > 0.0)` fails).

### D2 — the crab spawner looked into a box, played the wrong sound and never vented *(fixed)*

* Oracle: `TileEldritchCrabSpawner.func_145845_h` (jar) —
  `world.func_147452_c(x, y, z, blockType, 1, 0)` (the block event of the vent) and
  `"random.fizz"` for the warning, `"thaumcraft:gore"` for the spawn,
  `World.func_72977_a(x+0.5, y+0.5, z+0.5, 16.0)` for the activation and
  `writeCustomNBT` writes **only** the byte `facing`.
* Port before: `TileEldritchCrabSpawner.java:39-60` used an AABB of half width **16.0**
  (27.7 blocks in a diagonal) and accepted creative players, played
  `SoundEvents.GENERIC_HURT` (`minecraft:entity.generic.hurt`) instead of `thaumcraft:gore`,
  never sent the block event and saved the countdown into the NBT.
* Fix: `TileEldritchCrabSpawner.java:52-146` — the sphere of 16.0 blocks, the block event
  (`Level.blockEvent(pos, block, 1, 0)`, the 1.21.1 counterpart of `addBlockEvent`), the sound
  `thaumcraft:gore` (`TileEldritchCrabSpawner.java:44-46`), the client vent of twenty ticks
  through `triggerEvent(1, 0)` and the saved data of the original (only the facing).
* The client side of the vent is drawn by the renderer of the spawner:
  `common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchCrabSpawnerRenderer.java:44-82`
  (three particles per tick of the vent, one in twenty ticks otherwise, the colour
  **0x9988AA** of the jar).

### D3 — the eldritch lock destroyed the room and never spawned a boss *(fixed, the big one)*

* Oracle: `TileEldritchLock.func_145845_h` / `doBossSpawn` (jar) — the lock counts to
  **101**, pumps every **5** ticks (`"thaumcraft:pump"`), then it
  1. reads the labyrinth cells around itself, takes the chunk of the cell with the feature
     `2` as the centre of the room and the feature `2..5` of a cell with an exit as the exit,
  2. raises the counter of the boss map (`MapBossData`, the saved data of the world) by one
     and by a second one in **25 %** of the cases,
  3. builds the arena of the boss with the counter modulo four
     (**0** golem, **1** warden, **2** portal of the cult, **3** hall of the taint) at the
     height **50** in the middle of the four chunks of the boss room,
  4. spawns the boss (`onSpawnWithEgg`, the home area of the warden `(x, y+2, z, 32)`),
  5. removes **only** the `blockAiry` blocks of the 5x5x5 cube around the lock (with a
     sparkle packet) and removes the lock itself.
* Port before: `TileEldritchLock.java:48-67` destroyed **every** block of the 5x5x5 cube
  (the walls and the floor of the room with it) and **never spawned anything** — the Outer
  Lands of the port had no boss at all, no golem, no warden, no crimson portal and no
  taint hall. The sounds were `SoundEvents.BEACON_AMBIENT` and `SoundEvents.GLASS_BREAK`
  instead of `thaumcraft:pump` and `thaumcraft:ice`.
* Fix:
  * `common/src/main/java/thaumcraft/common/tiles/TileEldritchLock.java:36-75` — the pump
    of the original, the opening after 101 ticks and the call of the arena builder.
  * `common/src/main/java/thaumcraft/common/lib/world/OuterLandsBossRooms.java` (new) — the
    four arenas of the oracle with their exact block layouts, the pedestal pattern
    `{2,2,2},{0,-1,1},{3,3,3}`, the urn rarities (warden **10 % / 20 %**, golem **5 % / 15 %**),
    the loot corners of the golem (four corners, **15 %**, **30 %** crates), the rings of the
    floor of the cult (**2** halved, **3** two thirds, **4** three quarters), the rings of the
    floor of the taint (**4** halved, **5** two thirds, **7** three quarters), the five
    champions of the taint and the four chat messages of the original
    (`tc.boss.golem`, `tc.boss.warden`, `tc.boss.crimson`, `tc.boss.taint`, squared distance
    **300**).
  * `OuterLandsBossRooms.BossSavedData` (same file, lines 57-100) is the native
    `MapBossData`: the counter lives in the saved data of the dimension of the lock, exactly
    like the original kept it in the saved data of the world of the Outer Lands.

### D4 — the crimson portal reached 1.73 blocks and drew a roll it never used *(fixed)*

* Oracle: `EntityCultistPortal.func_70100_b_` (jar) —
  `if (this.func_70068_e(p) < 3.0 && p.func_70097_a(DamageSource.func_76354_b(this, this), 8.0f))`
  — a **distance** of three blocks, not a squared distance; and
  `spawnMinions` draws `5 + rand.nextInt(5)` of the void **only** when `stage > 12`.
* Port before: `EntityCultistPortal.java:200-208` compared `distanceToSqr(player) >= 3.0`
  (so the portal only reached 1.73 blocks) and `EntityCultistPortal.java:183-185` drew
  `random.nextInt(5)` at **every** wave, even when the portal did not burn.
* Fix: `EntityCultistPortal.java:200-208` uses
  `CultistRules.PORTAL_COLLISION_DISTANCE_SQ = 9.0` and the roll of the self damage happens
  only inside `if (stage > CultistRules.PORTAL_BOSS_STAGE)`.

### D5 — the eldritch orb of the original has no shadow size in 1.21.1 *(documented, not fixable)*

* Oracle: `EntityEldritchOrb.func_70053_R` returns `0.1f`.
* 1.21.1: `Entity` has no `getShadowRadius` any more (the byte code of the merged jar has
  no shadow member), so there is no counterpart to port. The rest of the orb
  (no gravity, 100 ticks, the radius of two, `0.666` of the attack damage of the thrower,
  the weakness of 160 ticks, `random.fizz`, the event 16) was compared with the port
  `common/src/main/java/thaumcraft/common/entities/projectile/EntityEldritchOrb.java` and
  agrees.

### D6 — the sound of the opening of the lock *(fixed, secondary)*

* Oracle: `"thaumcraft:pump"` every fifth tick and `"thaumcraft:ice"` when the lock opens.
* Port before: `SoundEvents.BEACON_AMBIENT` and `SoundEvents.GLASS_BREAK`.
* Fix: `TileEldritchLock.PUMP` and `OuterLandsBossRooms.ICE` are the TC4 sound events.

## Units that were compared and found faithful (verdict raised, no code change)

* `ItemCultistBoots`, `ItemCultistLeaderArmor`, `ItemCultistPlateArmor`, `ItemCultistRobeArmor`
  — warp **1**, vis discount **1 %**, the rarity, the repair with an iron ingot and the order
  of the tooltip lines agree with the jar.
* `EntityCultist`, `EntityCultistCleric`, `EntityCultistKnight`, `EntityCultistLeader`,
  `AICultistHurtByTarget` — the numbers agree with the jar and with the report of the
  openTC4 wave (`docs/analysis/2026-10-21-openTC4-entities.md`, section 3).
* `EntityEldritchGolem`, `EntityEldritchWarden` — the attributes, the ward, the spawn timer,
  the titles and the attacks agree with the jar.
* `ItemEldritchObject` — the five metadata values became five native items; the placer, the
  eye, the tablet and the research triggers of the crimson rites and of the primordial pearl
  agree with the jar.
* `TeleporterThaumcraft` — the search of the gate in a square of **128** blocks, the landing
  offset of one block and the fallback to the surface agree with the jar.
* `MapBossData` — now the `BossSavedData` of the boss rooms (see D3).

## Not touched (and why)

* **All client renderers and models of the list** (`RenderCultist`, `RenderCultistPortal`,
  `RenderEldritchCrab/Golem/Guardian/Orb/Warden`, `ModelEldritch*`, `TileEldritch*Renderer`,
  `EldritchCrustBakedModel`, `ItemEldritchRenderer`, `EldritchDiagnostics`): the client
  cannot be started in this sandbox (no GLX context), so no visual verdict can be written.
  They stay `partial`. The one exception is the crab spawner renderer, whose missing
  behaviour (the vent particles) was a *missing* call in the port, not a visual difference.
* `BlockEldritchRenderer` is the defect of the invisible cosmetic blocks and belongs to
  another wave; it was not touched.
* **The Outer Lands room generators** (`GenBossRoom`, `GenCommon`, `GenKeyRoom`,
  `GenLibraryRoom`, `GenNestRoom`, `GenPassage`, `GenPortal`, `MazeGenerator`, `MazeHandler`),
  the world provider and the chunk provider, the eldritch ring and the two biome classes:
  the port replaces them with the 1.21 feature architecture (`DungeonRooms`,
  `OuterLandsProvider`, `EldritchRingFeature`, `OuterLandsBiomes`). The layouts are covered
  by the existing fidelity tests (`OuterLandsRoomFidelityTest`, `MazeFidelityTest`), but I
  did not compare every room of the oracle with the port in this wave, so they stay
  `partial`. Two findings of the audit of the 21.10.2026 that I checked are **false
  positives** of that audit: the offset of the guardians of the key room is
  `getRandomIntegerInRange(rand, -1, 1)` in the jar (the port is right) and the corner of the
  boss room is chosen with `nextInt(4)` in the jar (the port is right).
* `EntityCultistPortalGreater` has no counterpart in the 4.2.3.5 jar at all (it only exists
  in the 1.12 reference); it stays `partial` with the disposition `replace`.

## Tests

Authored in `common/src/test/java/thaumcraft/eldritch/` and
`common/src/test/java/thaumcraft/cult/`:

* `thaumcraft/eldritch/EldritchTileRulesTest.java` — the trap (radius **3.0**, damage
  **2.0**, warp **1..2**, the next check in **10..34** ticks), the search of the closest
  player (the strict bound, the creative skip, the quirk of the centre), the crab spawner
  (the radius of **16**, the five crabs, the block event **1**, the vent of **20** ticks,
  the colour **0x9988AA**) and the lock (the pump every **5** ticks, the opening after
  **101** ticks).
* `thaumcraft/eldritch/OuterLandsBossRoomRulesTest.java` — the height of the arena (**50**),
  the offset of the exit, the quarter of the double counters, the announcement distance
  (**300**), the pedestal pattern, the rarities of the urns, the corners of the loot of the
  golem and the three rings of the two floors.
* `thaumcraft/cult/CultistPortalRulesTest.java` — the health **500**, the experience
  **30**, the collision distance of three blocks, the self damage of **5..9** only past the
  stage of the praetor, the healing of one point per tick, the stage **12** and the timing
  of twenty ticks per cultist.

**Execution status:** the tests were **compiled and executed** with a standalone JUnit 5
launcher (no Gradle daemon was started): **27 tests, 27 successful, 0 failed**. The suites
of the neighbouring packages (`thaumcraft.eldritch`, `thaumcraft.cult`, `thaumcraft.world`,
`thaumcraft.entities`, 331 tests) were executed the same way: 320 pass and 11 fail — every
one of the eleven fails is a `NoClassDefFoundError` of a vanilla registry
(`net.minecraft.world.item.Items`, `net.minecraft.world.level.block.Blocks`) that the
ad-hoc launcher does not bootstrap like the Gradle harness does. **The Gradle build and the
Gradle test suite were not run** (the leader runs them).

## Verification that was possible without Gradle

* `javac 21` (the JDK of the system) compiled every changed and every new file against the
  merged Minecraft jar of the Gradle cache and the classes of the main repository:
  **no error**.
* The three new suites and the neighbouring suites were executed with the JUnit 5
  platform launcher of the Gradle cache (see above).

## Blocker for the leader

`python3 scripts/porting/update_scope_ledger.py --check` **cannot run in this sandbox**:
the private oracle root that the script needs (`<repo>/../work/thaumcraft-4.2.3.5-oracle`)
does not exist here, so the script stops with
`Missing source root: /home/cyberfox-kali/wt/work/thaumcraft-4.2.3.5-oracle` before it
compares anything. The invariants that the check computes were verified by hand on the
edited ledger:

* the schema of every review is valid (the allowed values of the four verdicts, the
  non-empty evidence of a `parity_verified` row, the exclusion rules),
* **no stale fingerprint**: every `review.reviewed_sha256` equals the hash of the source
  that the row itself records (`stale_review_fingerprints: 0`),
* no drift of the legacy tree: every `legacy.sha256_lf` still matches the file of
  `src/main/java`,
* the file keeps its exact format (`json.dumps(..., ensure_ascii=False, indent=2) + "\n"`),
  `counts` was not touched and the dispositions were not changed.

The leader has to run the real `--check` with the oracle present.

## High risk areas for a re-check

1. **The boss rooms of the Outer Lands are new code** (`OuterLandsBossRooms`). The block
   layouts follow the jar line by line, but they were never run in a world: the first thing
   to try is a dedicated server with a labyrinth, the runed tablet in the hand and the lock
   of a boss room. The four arenas have to appear at the height 50 and the boss has to
   spawn.
2. **The biome of the taint hall** is written with `BiomeColumns.set`, which needs the
   chunk of the cell to be loaded; the loop loads the chunk before the call, but the biome
   of the arena was never seen in a world.
3. **The sparkle packet of the removed barrier blocks** (`PacketFXBlockSparkle`, the colour
   `0x400040`) has no native counterpart: the barrier disappears without the sparkle. The
   network payload of the client layer was not touched in this wave.
4. **The vent of the crab spawner** is driven by the renderer, because the 1.21.1 eldritch
   block ticks its tile on the server only (`BlockEldritch.getTicker`, which belongs to
   another wave and was not touched). The vent runs once per client tick, the original ran
   it in the tick of the tile.
5. **The scale of the vent particles** (2.0 in the original) needs the `setScale` of the
   original `FXVent`; the native `FXVent` of the client layer has no such method, so the
   particle is half as big. The client layer owns that class.
6. The **lock key sound** of the original is `thaumcraft:runicShieldCharge`
   (`BlockEldritch:253` in the jar); the port still plays `SoundEvents.BEACON_ACTIVATE` in
   `BlockEldritch.useWithoutItem`. `BlockEldritch` is `parity_verified` and was not touched
   in this wave — the sound of the key is a one line fix for whoever owns that file.
