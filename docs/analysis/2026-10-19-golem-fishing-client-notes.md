# Fishing bobber + presentation — FINAL handoff, 2026-10-19

Implemented source, **not compiled/tested/seen or accepted**. The [ROOT API agreement](2026-10-19-golem-fishing-api.md) supersedes proposed constructors/ownership in the earlier [bobber contract](2026-10-19-golem-bobber.md). Active Entity/Goal/Loot/registry/queue/platform WIP was not read. No extra setters/callbacks are required.

## Frozen files / public bridge

- New `common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBobber.java`.
- New `common/src/main/java/thaumcraft/client/renderers/entity/RenderGolemBobber.java`.
- Updated only `common/src/main/java/thaumcraft/client/renderers/models/ModelGolem.java`, `common/src/main/java/thaumcraft/client/renderers/entity/RenderGolemBase.java`, `common/src/main/java/thaumcraft/client/ThaumcraftGolemClient.java`, plus this new note.

```java
EntityGolemBobber(EntityType<? extends EntityGolemBobber> type, Level level);
public void setup(EntityGolemBase fisher, BlockPos water);
public EntityGolemBase getFisher(); // nullable actual live core11 golem
public BlockPos getWaterTarget();
// Client model, raw independent countdowns (no folding/normalization):
public void setArmTimers(int left, int right);
```

`setup` initializes an **unpublished server candidate** only (client/cross-level misuse throws); ROOT owns native `addFreshEntity`, admission rollback, the one-live-cast association and polling `isAlive()`. Owner **entity ID + optional UUID + immutable water position** use native SynchedEntityData. Resolution is a same-level indexed ID lookup, checked runtime type/UUID/liveness/core11, naturally retried after client packet reordering. Missing client owner hides only the rope. No scan, UUID-wide search, player hook, player fishing field, owner callback or recursive `getFishingBobber` check; removal cannot clear a newer association.

Required ROOT integration is the agreed `ThaumcraftGolems.GOLEM_BOBBER` type (size.25×.25, **noSave**, range4 chunks/64 blocks, updateInterval5) and `EntityGolemBase.getLeftArmTimer()/getRightArmTimer()/getGolemCore()`. Bobber calls agreed package-local **`GolemHarvestPlatform.mayInteract(golem,pos)`**, not crop-break or UseBlock events. No registry/bootstrap/platform/network source was edited. Ordinary native spawn packet is inherited; `recreateFromPacket` restores its motion (plain Entity does not), and client native transform packets interpolate at most5 steps, extrapolating the same motion between corrections.

## Actual behavior / source anchors

Original root `O = ../work/thaumcraft-4.2.3.5-oracle/thaumcraft/`; B=`common/entities/golems/EntityGolemBobber.java`, R=`client/renderers/entity/RenderGolemBobber.java`, G=`client/renderers/entity/RenderGolemBase.java`, M=active `client/renderers/models/entities/ModelGolem.java`.

- B:69–93 launch **at golem feet**; Δ=`(waterX+.5−gx,waterY+1−gy,waterZ+.5−gz)`, L=length(Δ), velocity=`(.1Δx,.1Δy+.08*sqrt(L),.1Δz)`. The nested/quarter root is retained; never teleport to water.
- B:116–217: block-only ray, non-water hit discards on server; water hit skips this update, next tick independently scales XYZ by `U*.2`. Native colliding move precedes forces. Yaw/pitch use wrapped **.2** interpolation. **Five equal-height BBox slices**, p+=.2 per wet slice; vy+=`.04*(2p−1)`, drag.92 (ground/horizontal collision.5); wet ⇒ drag×.9, vy×.8; all axes then multiply by drag. Native fluid pushing is disabled so it does not add a second current force.
- Server removes invalid/dead/missing/wrong-core owner, NoAI, invalid water/air, denied interaction/canonical-world checks and missing loaded trajectory/environment cells. A loaded swept BBox+1 margin is checked **before vanilla baseTick/ray/move/fluid reads**; non-finite motion or a >64-block single-update/correction span is rejected as a corruption bound. Root's helper controls world/border/profile/spawn authority. Target is actual **Blocks.WATER** (source/flowing), air above; no waterlogged-solid pond. Portals disabled; no force-loading, dimension following or fake/OP actor. Other pause/recall/unload/timeout lifecycle is ROOT goal cleanup (NoAI is also independently checked here).
- B:108–110 idle **2%** server splash is implemented with original count2–3/position/spread/speed. No bite counters, fake bite rewards, catch RNG, loot, cargo, paid rod or pending-output handling exists in this entity. The goal alone supplies chance, raw status8 cast/reel timing and real result publication.
- R:31–59 quad is centered unit geometry×.5, camera quaternion billboard. **Actual cached native asset** `minecraft:textures/entity/fishing_hook.png` replaces retired legacy particle-atlas UVs. R:75–91 effective rope anchor is interpolated golem **body yaw + eye height**, not the discarded earlier swing vector or local-player hand. Black **17 vertices / 16 segments** retain the exact source quadratic Y curve and bobber+.25 endpoint, all bobber-local; the dispatcher already applied camera translation.
- G:170–191 **every core11** displays a rod, cast or no cast, independent of real cargo. Original rotation `5°+90°*model.golemRightArm.xRot/π`, translation(−.26875,1.6,−.53), −Y90°, −Z30°, scale(.66,−.66,.66) are retained. Native `ItemDisplayContext.NONE` plus (.5,.5,−1/32) centering adapts its finite1/16 extrusion. The native player-only rod predicate leaves the source-backed base icon; no fake player/equipment/cargo/save/drop stock is created.
- M:133–168: fresh **carry/walk fallback for BOTH arms**, then independent positive raw-arm period**20** triangle overlays, then higher-priority folded ACT period**5**. Coreless/negative float boot resets both arms0; inactive remains head-only. This deliberately fixes M:151–157's stale untimed arm across frames/entities. `feedPose` supplies **every setter** for base and damage models, including zeros, via the same helper.
- Seven skins, generic `coreForType` badge, float bootup, FOOT_OFFSET/model scale/sway, healing/damage overlay, real carried-item layer, marker outlines, menu binding and **all** placer predicates were retained. Client init adds only ROOT bobber renderer registration.

## Declared adaptations / remaining gaps

- Native `ClipContext.COLLIDER/NONE` geometry and **WATER block + actual native fluid-height** slice overlap map old Material queries; not exact old block/fluid collision parity or generalized modded/waterlogged water. Ordinary client interpolation/extrapolation is visual approximation, not another catch simulation. Native tracking5/motion replaces original tracking64/no-velocity plus custom spawn doubles.
- **Native age >4000** is once per normal tick; it intentionally does not copy source B:112's extra `ticksExisted++`/quasi-double aging. Empty required custom NBT methods plus ROOT `noSave` mean no owner/cast/quality/counter restoration, correcting original dangling saved bobbers.
- Native hook sprite/full UV, packed-light quad, shader line-strip normals/default width/depth handling, rod item rendering/centering and mesh/rope alignment are adaptations **not visually inspected**. No animated/OBJ asset was added. Dormant legacy weather/bite-counter RNG scaffolding is omitted; no seed-for-seed FX/float-precision parity claimed. Cast/reel **sounds and reel particle burst** are not added here: those success-sensitive effects remain ROOT goal-owned, never guessed from generic discard.
- ROOT's agreed **one unpublished paid catch in native golem NBT** is allowed; the earlier bobber note's proposed “no saved pending rewards” is superseded. This patch implements **none** of pending-output conservation, queue handoff, catch count/quality/loot, recipes, core registration or goal lifecycle; no saved cast is introduced.

Source readback plus cached native JAR asset/signature/bytecode inspection (`unzip`/metadata `javap`) only. **No Gradle/compiler/Java application/game/tests/Python/worlds/agents**; no tests/resources/build scripts changed. ROOT performs the actual combined Fabric build after runtime + this FINAL. Fishing gameplay/protection/rates, visuals, full source parity and acceptance remain unverified; prior Guard/inputDamage/loot build evidence is not a Fishing PASS.
