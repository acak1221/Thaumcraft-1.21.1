# Wave 7 — world, chunk infrastructure, stairs and sparkle call sites

Branch: `agent/w7world`; starting commit `c3f1606a`. Only this worktree was used.
Authority: byte-exact `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`.

## Coverage outcome

| Oracle unit | Before | After | Reason |
| --- | --- | --- | --- |
| `thaumcraft/client/fx/particles/FXSparkle.java` | partial | parity_verified | Both missing block triggers now call the existing configurable particle through a client-only callback. |
| `thaumcraft/common/lib/events/EventHandlerWorld.java` | partial | parity_verified | Existing registered serializer mixin supplies the actual chunk marker; corrected the marker's presence-vs-truth semantics. |
| `thaumcraft/common/blocks/BlockCosmeticStairs.java` | partial | partial | Position application and four assets already work; six independent face textures do not. |
| `thaumcraft/common/lib/events/ServerTickEventsFML.java` | partial | partial | Queue exists; fixed structure replay random and gates. Nether aura replay still has no native implementation. |
| `thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java` | partial | partial | Supplied scattered/auraGen gap is already implemented; discovered hard Overworld restriction still excludes original Nether nodes. |

`parity_verified` here is oracle/code parity, not a claim of new runtime or visual acceptance.
No Gradle, JUnit execution, GameTest, client or NeoForge run was performed.

## Findings and fixes

### Sparkle block triggers

Oracle `BlockMagicalLeaves.java:214-215` short-circuits on silverwood metadata, rolls
`nextInt(500)`, takes **six world-random floats** for XYZ, and asks for size **2**,
type **7**, gravity **0**. Oracle `BlockLifter.java:78-82` requires unpowered and
`rangeAbove > 0`, uses the display random for X/Z `0.2 + nextFloat()*0.6`, Y+1,
size **1**, type **3**, gravity **-0.3**.

The native blocks existed but neither had an `animateTick` override. Added those in
`common/src/main/java/thaumcraft/common/blocks/BlockMagicalLeaves.java:54-65` and
`common/src/main/java/thaumcraft/common/blocks/BlockLevitator.java:31-40`.
`SparkleCallRules.java:26-39` provides a default-no-op, client-installed callback,
avoiding client classes on the dedicated-server path. Installation is in
`common/src/main/java/thaumcraft/client/ThaumcraftHolesClient.java:28-35`, reached
from existing `ClientSetup.java:120`.

The client repeats oracle `ClientProxy.java:648-655`: **world** random
`nextInt(6) < particleCount`, multiplier **6**, then the existing
`TypeSparkleParticle` (including its gravity and no-physics behavior).

Authored four pure tests in
`common/src/test/java/thaumcraft/common/lib/fx/SparkleCallRulesParityTest.java`.
Expected offsets use independent `java.util.Random` expressions of the oracle;
no blocks are constructed. **Compiled, not executed.**

Client acceptance still required: silverwood occasionally emits type-7 size-2
sparkles while greatwood does not; an open, unpowered levitator emits type-3
size-1 sparkles above its top, rising with negative gravity; power or an immediately
blocked column stops them. Reduced particle settings must preserve the proxy gate.

### Chunk marker

Oracle `EventHandlerWorld.java:114-137` writes `Thaumcraft/{regenKey:true}` on every
save, and **hasKey**, not getBoolean, decides whether a loaded chunk is marked.
The existing port already includes
`common/src/main/java/thaumcraft/common/mixin/Tc4ChunkRegenerationMarkerMixin.java:39-51`,
registered by `common/src/main/resources/thaumcraft-common.mixins.json:78`.
It injects into actual serializer read/write RETURN sites, so a SavedData workaround
is unnecessary. Verified both complete native method signatures with `javap`:

* `read(ServerLevel, PoiManager, RegionStorageInfo, ChunkPos, CompoundTag): ProtoChunk`
* `write(ServerLevel, ChunkAccess): CompoundTag`

Vanilla decompilation reference:
`net/minecraft/world/level/chunk/storage/ChunkSerializer.java:167,289-372`
(local `/tmp/opencode/dec1211/out/`): this owns read and the fixed-key write.
The mixin supplies the missing extension; **no vanilla limitation exemption is claimed**.

Fixed `common/src/main/java/thaumcraft/common/lib/events/ChunkRegeneration.java:95-110`
to check presence through a pure `hasMarker` helper. A false-valued or non-boolean
present key still counts, exactly as the oracle. Added a regression to
`ChunkRegenerationParityTest`: **compiled, not executed**. Live mixin application,
save/reload and key-change acceptance remain for the leader.

### Regeneration queue

The stale ledger claim that replay cannot happen is false. Native
`ChunkRegeneration.java:130-141` drains **10** chunks per dimension tick;
`:149-153` preserves the two `nextLong() >> 3` seeds of oracle
`ServerTickEventsFML.java:83-104`. `Thaumcraft.java:215` wires the world tick.
Direct feature bodies already replay into `ServerLevel`, not the terrain pipeline.

Found that the structure replay discarded the replay random and reseeded from
fresh-generation coordinates. Oracle `ServerTickEventsFML.java:96` hands the same
Random through oracle `ThaumcraftWorldGenerator.java:406-453`.
Fixed `ChunkRegeneration.java:205-235` to continue that same stream and enforce
the Overworld/nonflat/dimension-blacklist structure gates. Extracted existing build
bodies into `placeSelected` in `WorldGenMound.java:90`,
`WorldGenHilltopStones.java:59`, `EldritchRingFeature.java:62`.
**Fresh-generation callers still draw exactly the same stream as before.**
Neither `ThaumcraftTotemFeature` nor its random stream nor the GameTest search changed.
The existing exception guard remains: a replay feature RuntimeException is logged,
and the queue does not crash the server.

Authored one pure stream-continuation test in `ChunkRegenerationParityTest`;
**compiled, not executed**. This tests draw continuity, not a live world replay.

**Exact remaining difference:** oracle `ThaumcraftWorldGenerator.java:130-132,548-555`
dispatches Nether regeneration to its wild-node pass when `regenAura` is enabled.
Native `ThaumcraftNormalNodeFeature.java:127,160` refuses non-Overworld worlds;
the queue therefore does not reproduce Nether aura replay. This is a missing
native dimension path, not a datapack/serializer impossibility. Retain `partial`.

### Stairs

The suspected position-application gap is already fixed by native
`BlockCosmeticStairs.java:139-158`. The blockstate has **160** variants referencing
the four texture models for every facing/half/shape combination.

The actual difference is **per-face**. Oracle `BlockCosmeticStairs.java:38-40`
passes `side+100` to `BlockCosmeticSolid.java:157-162`. At **(0,0,0)** the oracle
down/up/north/south/west/east indices are **[3,0,1,2,3,0]**; native
`BlockCosmeticStairs.java:123-145` paints every face with the up-face index **0**.
The prior “all faces agree except one in ~12000” explanation is mathematically
wrong: changing the bound changes the modulus, even for ordinary random draws.
Added a literal six-value regression to `CosmeticStairsTextureParityTest`;
**compiled, not executed**.

**Exact remaining difference:** needs an actual position-aware, face-aware client
model/sprite path. One four-valued property cannot represent six independent face
choices. No vanilla impossibility claim and no asset/placement redo was made.
Retain `partial` rather than claim the top-face approximation is exact.

### World generator

The supplied gaps are already covered: native `AuraGenRules.java:61-115` queries
the three modern scattered-temple structure starts, and
`ThaumcraftNormalNodeFeature.java:63-77` places the node at surface+**3** then
overwrites the scattered flag, matching oracle
`ThaumcraftWorldGenerator.java:413-419`. Native `AuraGenRules.java:130-144` and
`ThaumcraftTotemFeature.placePass` share the final flag behavior, including the
quirk that a scattered node alone does not suppress a totem. Existing
`AuraGenRulesParityTest` was read, not rerun.

**Newly confirmed remaining difference:** oracle
`ThaumcraftWorldGenerator.java:130-132,548-555` supports wild Nether nodes in fresh
generation too. Native `ThaumcraftNormalNodeFeature.java:38,127,160` explicitly
hard-restricts the feature and node factory to Overworld. Neither modern structure
starts nor an API limitation justify silently excluding that dimension. Consequently
the full unit remains `partial`, despite its supplied Overworld gaps being closed.

## Verification actually performed

* Whole `common/src/main/java`: **1,411 Java sources**, Java 21 `javac` exit **0**.
  Command: `javac -J-Xmx1500m -proc:none --release 21 -sourcepath common/src/main/java
  -cp "$CP" -d .tmp/w7world/classes @.tmp/w7world/sources`.
* `$CP` includes the named 1.21.1 Loom-cache merged jar under
  `~/.gradle-leader/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/`,
  `common/build/classes/java/main` (absent in this clean worktree, so **all sources
  were compiled**, not trusted as existing classes), non-source module dependency
  jars from `~/.gradle-leader/caches/modules-2/`, named Architectury dependencies in
  `/tmp/opencode/remapped/`, and `/tmp/opencode/minecraft-widened.jar` first for
  the project's access-widener visibility. Confirmed this jar has the **same 8,269
  class paths** as the actual named vanilla jar; only **8** class byte streams differ.
  The initial raw-vanilla compile exposed expected access-widener access errors;
  the corrected classpath's whole-main compile passed.
* All three touched JUnit source files separately `javac`-compiled, exit **0**.
  **Seven tests authored; no JUnit test executed.**
* `python3 scripts/porting/convert_assets.py --verify`: **missing_references: 0**.
  No assets changed.
* `python3 scripts/porting/update_scope_ledger.py --check --original-root
  /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`:
  **stale_review_fingerprints: 0**. Only the five assigned reviews changed;
  `counts` left untouched.

## Commits and follow-up

* `db5011d8`: block sparkle triggers and marker-presence correction, tests.
* `b8e08ffe`: regeneration structure stream/gates and explicit stair-face regression.
* This report/ledger commit records two verified units and three honest partials.

Leader follow-up: execute JUnit/GameTest sequentially; exercise serializer mixin
save/reload and live retrogen; inspect sparkle visuals. Remaining implementation
work is per-face stair rendering and the Nether wild-node generation/replay path.
