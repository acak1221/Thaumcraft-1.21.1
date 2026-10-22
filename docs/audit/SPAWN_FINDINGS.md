# Spawn audit — g13biome (2026-10-23)

Scope: Fabric 1.21.1, default configuration, fresh world. **36 registered mob types** inspected, including player-built mobs and five extra/legacy types. Projectiles, dropped items and falling blocks are not mobs. A missing biome row is not a defect for a special-path mob. NeoForge runtime parity is deferred by AGENTS.md.

## Evidence levels

- **PROVEN** means exercised by this audit's actual server GameTests; final run details below.
- **REASONED** means production callers/registrations inspected, not an end-to-end survival acquisition replay. Existing test names are pointers, not claims those tests ran here.
- **NO PATH (REASONED)** means no production survival creator found; commands/eggs/test construction do not count. Do not interpret this as a dynamically proven impossibility.

The blanket hypothesis “1.21.1 requires JSON biome modifiers, so code registration does nothing” is false on Fabric. `ThaumcraftFabric` calls `FabricBiomeSpawns.init`, which uses Fabric `BiomeModifications` at registry load. Native biome JSON provides constructor rows; the adapter applies `OriginalSpawnRules`. NeoForge has `data/thaumcraft/neoforge/biome_modifier/original_entity_spawns.json` with a custom modifier type, not a missing registration. No `forge/biome_modifier` files were found or needed by Fabric. The weaker hypothesis “a biome row can exist while its placement rule is missing/wrong” **is confirmed**, including Inhabited Zombie's absent Fabric placement predicate.

Oracle inspected directly: bundled immutable TC4 4.2.3.5 JAR, `javap -p -c` for `ConfigEntities`, `EventHandlerWorld`, `TileNode`, `EntityEldritchGuardian`, `EntityInhabitedZombie`. Prior source-specific biome audit: `docs/analysis/2026-10-20-original-biomes-and-worldgen.md`. No original/donor source modified.

## Mob inventory (36)

IDs below omit `thaumcraft:`. P = `spawnAuditNativeBiomePools`; G = `spawnAuditGuardianIsolation`; S = `spawnAuditSpecialPaths`. These query the actual native generator pool or operate real server entities, not source-text assertions.

| # | Mob | Survival creation path | Evidence / test | Fix |
|---|---|---|---|---|
| 1 | brainy_zombie | Global eligible-biome monster rows | P: Plains pool row | Combined-light helper correction |
| 2 | giant_brainy_zombie | Eerie row; DARK node ecology | P: pool row; node creator REASONED | Inherits corrected Brainy light helper |
| 3 | firebat | Nether rows; October 31 eligible-biome rows | P: Nether; seasonal branch REASONED | Combined-light gate correction |
| 4 | wisp | Magical Forest/Eerie/Nether rows; hilltop-stone spawner | P: all three pools; spawner REASONED | Saved-sky first roll; combined-light second roll |
| 5 | pech | MAGICAL biome rows | P: forest; other tags REASONED | Combined-light helper correction |
| 6 | mind_spider | Warp event (`WarpEvents`) | REASONED | None |
| 7 | eldritch_guardian | Eerie/Outer Lands rows; obelisk/warp creators | P + G; special creators REASONED | Restore original isolation gate |
| 8 | eldritch_warden | Outer Lands boss lock room | REASONED: `OuterLandsBossRooms` | None |
| 9 | eldritch_golem | Outer Lands boss lock room | REASONED: `OuterLandsBossRooms` | None |
| 10 | eldritch_crab | Inhabited zombie death; eldritch spawner | S: actual death creates crab; spawner REASONED | None |
| 11 | inhabited_zombie | Outer Lands biome row | P: pool row; S: added entity dies into crab | Restore missing registered original monster placement predicate |
| 12 | watcher | No production survival creator found | NO PATH (REASONED); absent original `ConfigEntities` registration | Not invented |
| 13 | thaum_slime | Flux-taint death conversion; goo; splitting | REASONED: `TaintDeathConversion`, `BlockFluxGoo` | None |
| 14 | taintacle_giant | Outer Lands taint boss room | REASONED: `OuterLandsBossRooms` | None |
| 15 | cultist_knight | Cultist portal emission | REASONED: `EntityCultistPortal` | None |
| 16 | cultist_cleric | Cultist portal emission | REASONED: `EntityCultistPortal` | None |
| 17 | cultist_leader | Cultist portal boss emission | REASONED: `EntityCultistPortal` | None |
| 18 | cultist_portal | Outer Lands cultist boss room / eldritch structures | REASONED: `OuterLandsBossRooms` | None |
| 19 | cultist_portal_greater | No production survival creator found | NO PATH (REASONED); absent original registration | Not invented |
| 20 | taint_spider | Spore burst; champion infestation | REASONED: `EntityTaintSpore`, `ChampionRules` | None |
| 21 | taint_spore | Taint fibre random tick | REASONED: `BlockTaintFibres` | None |
| 22 | taint_swarmer | Taint block random tick | REASONED: `BlockTaint`; S tests its downstream death burst | None |
| 23 | taint_swarm | Swarmer burst | S: actual swarmer death tick creates swarm | Combined-light gate correction |
| 24 | taintacle | Taint biome row; Outer Lands room | P: pool row; room REASONED | Combined-light gate correction |
| 25 | taintacle_small | Taintacle/seed attack emission | REASONED: `EntityTaintacle.spawnTentacles` | None |
| 26 | taint_creeper | Flux-taint creeper death conversion | REASONED: `TaintDeathConversion` | None |
| 27 | tainted_chicken | Flux-taint chicken death conversion | REASONED: `ThaumcraftTaintAnimals.onLivingDeath` | None |
| 28 | tainted_cow | Flux-taint cow death conversion | REASONED: same production event | None |
| 29 | tainted_pig | Flux-taint pig death conversion | REASONED: same production event | None |
| 30 | tainted_sheep | Flux-taint sheep death conversion | REASONED: same production event | None |
| 31 | tainted_villager | Flux-taint villager death conversion | REASONED: same production event | None |
| 32 | taint_crawler | No production survival creator found | NO PATH (REASONED); absent original registration | Not invented |
| 33 | taint_seed | No production survival creator found | NO PATH (REASONED); absent original registration | Not invented |
| 34 | taint_seed_prime | No production survival creator found | NO PATH (REASONED); absent original registration | Not invented |
| 35 | golem | Player deployment item | REASONED: `ItemGolemPlacer` | None |
| 36 | traveling_trunk | Player deployment item | REASONED: `ItemTrunkSpawner` | None |

## Rule comparison

`ConfigEntities.initEntitySpawns` original global additions: Brainy 10/1–1 for eligible nonempty monster lists; Pech 10/1–1 for MAGICAL nonempty lists; Firebat Nether 10/1–2 and Halloween eligible 5/1–2; Wisp Nether 5/1–1. Original Forge updates the first matching row **and appends another**, so duplicate Pech rows in Magical Forest are intentional, not a weight bug. Native constructor rows: Forest Wisp10/1–2; Eerie Giant8/1–1, Wisp3/1–1, Guardian1/1–1; Taint Taintacle1/1–1; Outer Lands Inhabited1/1–1 and Guardian1/1–1. P checks these through `ChunkGenerator.getMobsAt` with real biome holders and a server position.

Light/Y review: registered Pech/Brainy/Firebat/Wisp/Swarm/Taintacle predicates exist; Pech also rejects Taint and four nearby Pech; Wisp caps nearby population at eight. No new arbitrary Y cutoff introduced. Flying mobs use NO_RESTRICTIONS; ground mobs use ON_GROUND. These are distinct from adding a mob to a weighted pool. Full special-path height/collision fidelity remains unproved in this audit.

**One confirmed rule mismatch:** guardian lacked original `getCanSpawnHere` exclusion. Oracle bytecode constructs `[x,y,z,x+1,y+1,z+1].expand(32,16,32)`, returns false if another guardian is found, otherwise calls the superclass. Preserve that exact box and superclass rule in native `checkSpawnRules`; do not apply it to forced obelisk/warp creation which never calls the natural predicate.

**Second rule defect (shared light semantics):** original `World.func_72957_l` (MCP `getBlockLightValue`, obfuscated `ahb.k`) calls `ahb.b(IIIZ)`, which calls `apx.b(IIII)` with `skylightSubtracted`. It is **combined** light, not the saved BLOCK channel. Official vanilla1.7.10 bytecode was independently inspected, rather than trusting the misleading method name or earlier port test comments. The port's block-only implementation admitted firebats in open daylight. `spawnAuditOriginalCombinedDaylight` failed before the fix with SKY15/BLOCK0/combined15. Restore combined brightness in class gates and inherited monster gates, including the original subtraction10 override during thunder for the latter. Earlier tests asserting block-only semantics are corrected, not treated as an oracle. This affects seven mob types: Brainy, Giant Brainy, Pech, Firebat, Wisp, Swarm, Taintacle. DARK-node giant creator already used combined light correctly and was not changed.

**Third rule defect (Wisp first roll):** Wisp's original `isValidLightLevel` reads `func_72972_b(Sky,...)`, the saved SKY channel, not already-darkened sky. Use saved sky first, then combined light, with short-circuit RNG order. The new light regression also compares128 night seeds and post-call RNG state against that original sequence.

**Fourth rule defect (Inhabited Zombie placement):** its biome row exists but Fabric never registered its spawn-placement predicate, so `SpawnPlacements.checkSpawnRules` used the default always-true rule. The original Inhabited Zombie calls superclass Zombie/EntityMob spawning after its isolation check; the inherited32/8 darkness gate must not be skipped. Add the Fabric ON_GROUND placement with original monster light predicate, retaining existing instance isolation. NeoForge currently explicitly uses an unrestricted predicate for this type; that loader's integration is deferred, not claimed fixed.

## Champions and node wisps

`ChampionHooks.init` registers Architectury's production `EntityEvent.ADD`, called by normal server insertion, and rolls only NOT_ROLLED hostile mobs. `spawnAuditChampionNaturalLifecycle` finalizes a real zombie with NATURAL reason, chooses a deterministic winning world-RNG fixture seed, inserts it through `ServerLevel.addFreshEntity`, and asserts both champion ID and increased max health. It never calls `onEntityAdded` or promotion directly. This proves the natural lifecycle's production listener, **not** a complete random-tick natural-spawner acquisition.

No node-to-wisp creator found in modern code. However direct inspection of original TC4 `TileNode` bytecode also finds **no EntityWisp reference**, and original world-event bytecode contains no node-wisp spawning. Original `BlockAiry` has wisp-essence drops but no EntityWisp creation either. Therefore “wisps must spawn near aura nodes” is not established by this oracle; adding such a rule would invent gameplay. Existing biome and hilltop spawner paths must not be mistaken for a node path.

## Execution record

Guardian fix: four focused server GameTests passed (`/tmp/opencode/g13biome-guardian-after.log`). Before the fix, the guardian test failed on the actual neighbouring-guardian assertion. Light regression: failed before fix (`/tmp/opencode/g13biome-light-before.log`) on the actual daylight Firebat predicate; Taintacle's separate regression failed in the actual taint/fibre column (`/tmp/opencode/g13biome-taintacle-before.log`). Inhabited's regression failed on the actual default-always-true daylight predicate (`/tmp/opencode/g13biome-inhabited-before.log`).

**Final: 17 unit tests and all11 focused Fabric server GameTests passed; BUILD SUCCESSFUL57s**, `/tmp/opencode/g13biome-final-verification.log`. Includes all six audit cases, original biome-source/duplicate-row test, Brainy gameplay, flying population, Swarm gameplay, Pech entity. Sources were compiled after the final gameplay edits. No full-suite, client, NeoForge, forced-thunder-weather fixture, complete survival acquisition, or all-special-path runtime claim.

See `docs/analysis/2026-10-23-agent-g13biome.md` for commands and limitations. Counts: **36 mobs inspected; five no-discovered-survival-creator types (reasoned-only); four rule defects fixed across nine mob types; zero confirmed missing required biome registrations.** Remaining source-reasoned paths are individually marked in the table; neither this count nor existing source tests imply those paths were executed here.
