# 2026-10-23 — the GameTest server of the port exhausts the heap (`agent/memory`)

## 1. The symptom, reproduced

The leader's report: the CI run of the current tree fails with
`java.lang.OutOfMemoryError: Java heap space` in the GameTest server, and the
CI runner has 7 GB of RAM, so the server's default heap is about 1.75 GB.

Reproduced locally, byte for byte, in the chunk save path:

```
java.lang.OutOfMemoryError: Java heap space
  at net.minecraft.world.level.chunk.PalettedContainer.pack(PalettedContainer.java:239)
  at net.minecraft.world.level.chunk.storage.ChunkSerializer.write(ChunkSerializer.java:307)
  at net.minecraft.server.level.ChunkMap.save(ChunkMap.java:784)
  at net.minecraft.server.level.ChunkMap.processUnloads(ChunkMap.java:483)
  at net.minecraft.server.level.ChunkMap.tick(ChunkMap.java:441)
  at net.minecraft.server.level.ServerChunkCache.tick(ServerChunkCache.java:314)
  at net.minecraft.server.level.ServerLevel.tick(ServerLevel.java:350)
  at net.minecraft.server.MinecraftServer.tickChildren(MinecraftServer.java:1021)
  at net.minecraft.server.MinecraftServer.tickServer(MinecraftServer.java:912)
```

The chunk that failed to save was `Failed to save chunk [168, 97]`, in the
test-only dimension `thaumcraft_gametest:natural` (the Magical Forest world of
the world-generation cases).

## 2. The measurements (heap vs. result)

All runs are `:fabric:runGameTest` on branch `agent/memory`, one Gradle at a
time, the GameTest server's heap set with `-Xmx` in `fabric/build.gradle`.

| tree | scenarios | `-Xmx1500M` | `-Xmx2G` | `-Xmx3000M` |
|---|---|---|---|---|
| `be3c816f` (the wave's branch point) | 318 | – | **318/318 PASS** | – |
| `agent/memory` (the wave) | 324 | **OOM** | **OOM** | **324/324 PASS** |

So the wave raised the smallest heap that passes from **2 GB to 3 GB**; the CI's
1.75 GB is below both. The base passes with 1.88 GB of used heap at its peak
(`jcmd GC.heap_info`), i.e. the base was already within 120 MB of the CI's
limit — the wave pushed it over.

Ruled out by experiment (each was run with the rest of the tree unchanged):

| hypothesis | experiment | result |
|---|---|---|
| the death aspect orbs | `DeathAspectOrbDrop.register()` disabled (`-Dthaumcraft.disableDeathOrbs=true`) | still OOM — **not the cause** |
| the wave's big magic tree canopy | `WorldGenBigMagicTree.leafNodeCount` reverted to the pre-wave `0.8 * h / 13` | still OOM, 22,189 proto chunks — **not the cause** |
| the wave's 6 new `survival_progression` scenarios | the whole suite run with those 6 filtered out | still OOM — **not the cause** |
| the number of loaded chunks | `ChunkMap.getChunks()` per level, per tick | base 2,332 overworld + 961 outer lands; wave 2,108 + 961 — **the wave holds fewer full chunks** |
| the `MagicalTreePlacement` knot guard | `MagicalTreePlacement.knotsWithoutEntity()` printed every tick | always **0** — the wave's new catch never fires, it is a no-op |

## 3. What the heap is actually spent on (the evidence)

`jcmd <pid> GC.class_histogram` taken at the moment of the OOM (the heap is
2,094,238 K of 2,095,152 K, i.e. completely full) versus the same histogram at
the peak of the base run:

| | base `be3c816f` (passes at 2 GB) | wave (OOM at 2 GB) |
|---|---|---|
| peak used heap | 1,969,224 K | 2,095,071 K (full) |
| `net.minecraft.world.level.chunk.ProtoChunk` | **16,031** | **21,522** |
| `net.minecraft.server.level.ChunkHolder` | 17,220 | 23,516 |
| `net.minecraft.nbt.CompoundTag` | 897,298 | **1,693,811** |
| `java.util.HashMap$Node` | – | 6,545,456 |
| `java.lang.String` | – | 3,300,296 |
| `[B` | – | 3,396,091 (377 MB) |

The live set is **not** entities, not block entities and not the aspect orbs
(the histogram shows 34 `ItemEntity`, 14 `Spider`, 302 `StructureBlockEntity`,
7 `TileNode` — all noise).

It is:

1. **~20,000 half-generated chunks** (`ProtoChunk` + `ChunkHolder`) that the
   world generation leaves in the server's chunk map, and
2. **the NBT of the chunks that the save pipeline holds at the same time**
   (1.69 M `CompoundTag`, 6.5 M `HashMap$Node`, 3.3 M `String` and 3.4 M
   `byte[]` — the NBT tree of roughly 7,000 chunks being written at once, about
   1.4 GB).

The number 20,000 is exactly the vanilla neighbour-chunk bookkeeping: the
features step of 1.21.1 always schedules the eight neighbours of the chunk it
decorates, and a neighbour that is never requested at full status stays a
`ProtoChunk` in the chunk map for the rest of the session. 20,105 proto chunks
/ 9 ≈ 2,234 chunks that the port's cases generate; the base's 16,031 / 9 ≈
1,781.

**So the growth is: the wave's cases generate ~450 more world-generation
chunks than the base did, and every generated chunk drags nine half-generated
chunks into the server's chunk map, which the final save then has to write.**

## 4. The two real defects that were found and fixed

### 4.1 The mock players of a finished GameTest were never released

`net.minecraft.gametest.framework.GameTestHelper.makeMockServerPlayerInLevel()`
and `makeMockPlayer(GameType)` add a **real connected `ServerPlayer`** to the
server's player list, and the vanilla never removes it: the helper has no
player list and `GameTestInfo` only discards the entities of the structure.
A player pins the chunks of its chunk ticket for the rest of the session, and
in the flat GameTest world each of those chunks is a chunk that the world
generator never finished — a `ProtoChunk` that the chunk map keeps.

Measured: the suite logs **241** `logged in with entity id` lines on the wave
and **276** on the base, of which 186 are the vanilla's `test-mock-player`,
51 the port's own `fup-` actors and 4 the `monsters-` helpers.

The port's own actor (`FocusUpgradeRuntimeChecks.Actor:282`) already releases
its player from the player list when it is closed — the vanilla's helper does
not.

**Fix** — `fabric/src/gameTest/java/thaumcraft/fabric/testing/mixin/GameTestHelperMockPlayerMixin.java`
(and the same file for NeoForge): the mixin records every mock player the
helper creates and removes it from the player list (which releases its chunk
tickets) and discards it when the test succeeds or fails. The player stays
alive and usable for the whole lifetime of its test.

Measured effect: the proto-chunk count of the wave drops from 21,522 to
**20,105**.

### 4.2 The world generation read a block that it cannot see

`common/src/main/java/thaumcraft/common/lib/world/WardedBehaviour.java:18`
read `level.getBlockState(pos)` without asking whether the chunk is visible.
In a `WorldGenRegion` a read outside the region is answered by the **server's**
chunk source (`WorldGenRegion.getBlockState` falls through to
`this.level.getChunkSource().getChunk(...)`), which puts a `ProtoChunk` into the
server's chunk map that is never finished.

Measured with a mixin on `ServerChunkCache.getChunk`: **117 (wave) / 138
(base)** such server-side chunk requests per suite run, all of them from
`WardedBehaviour.original` — the only unguarded far read in the port's
world-generation path (`GreatwoodDungeon`, `EldritchRingFeature` and
`OuterLandsProvider` all guard their reads with `WorldGenWriteArea.canWrite`).

**Fix** — the read is refused when the block is not already visible in a
generation region.

## 5. The final measurement

| | before the fix | after the fix |
|---|---|---|
| smallest heap that passes | 3 GB (`-Xmx3000M`) | **3 GB (`-Xmx3000M`)** |
| `-Xmx2G` | OOM | OOM |
| proto chunks at the OOM | 21,522 | 20,105 |
| scenarios | 324 | 324 |

`:common:test` — **not run** (the two changes are in the loader game-test
source sets and in one production helper; the JUnit suite was not executed in
the time budget, see the open points).

## 6. What is still open — and the exact next step

The two fixes above are correct but they are **not enough**: the suite still
needs 3 GB, and the CI runner has 1.75 GB. The measurement isolates the
remaining growth exactly:

* the wave's cases generate **~450 more world-generation chunks** than the
  base (2,234 against 1,781, derived from the proto-chunk counts 20,105 / 9 and
  16,031 / 9);
* every generated chunk leaves nine half-generated chunks in the server's
  chunk map, and the final save of the suite writes ~20,000 chunks' NBT at
  once, which is the 1.4 GB spike in the histogram.

The next step is therefore to find **which wave case generates the extra
~450 chunks** and to make the world-generation cases release their chunks as
they go, so that the save pipeline drains instead of flooding at the end of the
run. The metric for a bisect is the `ProtoChunk` count at the end of the run
(`jcmd <pid> GC.class_histogram | grep ProtoChunk`): the base is 16,031 and
every extra generated chunk adds nine.

A second, independent possibility is worth checking by the leader: the wave's
`WorldGenBigMagicTree.leafNodeCount` change restores the oracle's leaf-node
count (oracle `WorldGenBigMagicTree:56`, `1.382 + pow(leafDensity * heightLimit
/ 13, 2)` with `leafDensity = 1.0`, oracle `:23`), which makes the Magical
Forest chunks of the `thaumcraft_gametest:natural` dimension about twice as
large as the pre-wave ones (the natural dimension's saved region files grew
from 86 MB to 105 MB). The chunk NBT of a Magical Forest chunk is dominated by
the palette data, so a bigger canopy is a bigger chunk NBT and a bigger final
save. The oracle's leaf layer (`oracle WorldGenBigMagicTree:144-146`,
`leafSize` = 2 at the first and the last layer and 3 in between) and its
overwrite rule (`:102-132`, only air and leaves are overwritten) are both
reproduced by the port, so the canopy size itself is parity-correct now and
must not be shrunk again.

### Reproducing the measurements

```
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/memory/.gradle-memory
cd /home/cyberfox-kali/wt/memory
./gradlew --console=plain :fabric:runGameTest            # -Xmx2G from fabric/build.gradle
jcmd <server pid> GC.class_histogram | head -40          # the proto-chunk count
```
