# NeoForge half of the TC4 4.2.3.5 -> Minecraft 1.21.1 port — wave 2 (2026-10-23)

Agent: `neoforge`. Worktree: `/home/cyberfox-kali/wt/neoforge`, branch `agent/neoforge`.

Sources of truth (in order):

1. Oracle `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte-exact CFR 0.152
   decompilation of `Thaumcraft-1.7.10-4.2.3.5.jar`).
2. OpenTC4 `/home/cyberfox-kali/OpenTC4` (readable 1.7.10, second opinion only).
3. The shared 1.21.1 port in this worktree (`common/src/main/java`).

Environment of the runs:

```
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
GRADLE_USER_HOME=/home/cyberfox-kali/wt/neoforge/.gradle-neoforge   (isolated from the shared cache)
./gradlew --console=plain :neoforge:build :neoforge:runGameTest
```

Gradle 8.12.1 (wrapper), NeoForge 21.1.259, Architectury 13.0.11, Minecraft 1.21.1.

---

## 1. Baseline results (before this wave's changes)

| Check | Command | Result |
|---|---|---|
| Build | `:neoforge:build` | **BUILD SUCCESSFUL** in 34m 47s (first run: NeoForge 21.1.259 downloaded, Minecraft patched and access-transformed) |
| GameTest | `:neoforge:runGameTest` | **300 scenarios, 277 pass, 23 fail**, 0 errors, 0 skipped (`neoforge/build/gametest-results.xml`) |

The wave-1 shared changes did **not** break the NeoForge main source set: the
`ThaumcraftApi` enchantment keys, the new `TileMirror` save and the two new
`ThaumcraftFlux` items all compile in `neoforge/src/main/java/**` without a
change. The `gameTest` source set of the NeoForge module also compiles.

The old report (`docs/analysis/2026-10-21-close-neoforge.md`) recorded 22 failures
out of 300. The current run has 23: `magicalbiomesgeneratenaturallyandspawnactualpech`
now passes, and three wave-1 scenarios fail that the old run did not have
(`fluxoriginalexposureandgootail`, `fluxmovesandexhaustswithoriginalrules`,
`fullinventoryreusesfreedpaperslotordrops`).

### 1.1 The 23 failures, with their exact message

| # | Scenario | Message | Class |
|---|---|---|---|
| 1 | `nodetransductionconversionpersistenceandcooldown` | `Game-test mod must register node probes before registry freeze` | harness |
| 2 | `nodetransductionisolatedfailurelifecycle` | `Game-test mod must register node probes before registry freeze` | harness |
| 3 | `excavationextraremovalfailureaccounting` | `Register Excavation removal probe BEFORE block registry freeze` | harness |
| 4 | `growthlampconsumesessentiaandfertilisesplants` | `Game-test bootstrap must register the isolated native crop probe` | harness |
| 5 | `fluxcallbacksboundariesandneighborwake` | `registerTestProbes must register the test-only callback observer BEFORE registry freeze` | harness |
| 6 | `visamuletstransferandpaythroughactualnativerelay` | `test mod registered the native amulet pulse probe before registry freeze` | harness |
| 7 | `excavationharvestpreservesprotectionlootandcontainers` | `Fabric native event probe unavailable` | harness |
| 8 | `excavationupgradetreasureandenlarge` | `Actual native AFTER probe unavailable` | harness |
| 9 | `lumbergolemfellsonefarlogthroughtheguardedplatform` | `the protection BREAK event fired once, at the ACTUAL endpoint: [BlockPos{x=4515987, y=-53, z=-5104799}, BlockPos{x=4515987, y=-53, z=-5104799}]` | **behaviour** |
| 10 | `boreouterdestroycallbackremainsclosed` | `actual BLOCK_DESTROY callback after inner setBlock sees target air but dispatch still CLOSED until outer destroy returns` | **behaviour** |
| 11 | `purifyingfluidpreservesoriginalclassicflowwardandsaltexpiry` | `actual native entity tick sees eyes just inside original Forge filledPercentage: 0` | **behaviour** |
| 12 | `tablebreakevacuateseachhalfonce` | `after actual server-level-post, breaking either half delivers one raw table and each physical stack exactly once: true` | **behaviour** |
| 13 | `entropyqueuesthefarthestconnectedwaterforliquidgolems` | `long shaft, Entropy: every drained bucket is in the golem or the crucible, none conjured: 5000 mB for 6` | **behaviour** |
| 14 | `entropyswitcheschangewhatrealgolemsfetchemptyandsort` | `the item-match lanes did not finish within 1800 ticks: ...` | **behaviour** |
| 15 | `voidmetalgearmendswithersandwarps` | `void_sword withers the mob for 60 ticks: null` | **behaviour** |
| 16 | `travelingtrunkfollowsteleportsdefendsandpullsitems` | `the trunk phases did not finish within 1500 ticks: phase 0 (follow), follower (4515950.5, -57.0, -5104518.5) distance 8.0` | **behaviour** |
| 17 | `lootbagspreserveoriginalpoolsandpaidnativedropcustody` | `actual native dungeon table, loaded through the Fabric weighted-pool hook, naturally reaches a bag` | **behaviour** |
| 18 | `advancedfurnacepreservescustodyacrossrealchunkunload` | `own partial disassembly never force-loads saved remote shell terrain through inner-parts' indirect shape updates; shell=33 ...` | **behaviour** |
| 19 | `taintacleskeeporiginalcombatandrootedai` | `null` (an exception without a message) | **behaviour** |
| 20 | `taintacleskeeporiginalretaliationlifetimeandloot` | `null` (an exception without a message) | **behaviour** |
| 21 | `fluxmovesandexhaustswithoriginalrules` | `pending real Thaumic Slime branch does not silently consume goo (M11 not complete)` | **behaviour** |
| 22 | `fluxoriginalexposureandgootail` | `original lower25 MISS falls through30 HIT then boolean; exact observed calls=[nextInt(25)]` | **behaviour** |
| 23 | `fullinventoryreusesfreedpaperslotordrops` | `creative overflow spends one paper and the ink, and creates no note entity` | **behaviour** |

---

## 2. Work done in this wave

(filled in below, in commit order)

---

## 3. Parity evidence

(filled in below)

---

## 4. Still open

(filled in below)
