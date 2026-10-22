# Network source audit — h4net, 2026-10-23

## Scope and evidence

Baseline `e79b8558`; branch `agent/h4net`. **Reading only:** no Gradle, server,
client, packet capture, codec execution or wire observation was performed.
The active `common/src/main/java` has **36 payload classes: 24 S2C, 12 C2S**.
Retained `src/main/java` packets are the uncompiled legacy reference, not extra
modern channels. Every modern codec and registration/handler was read.

**Results:** zero asymmetric codecs; zero dedicated-server client-class handler
violations found; two C2S authority omissions (edit/combine, one shared fix);
one base-game dormant/API-only channel; two world-state lifecycle defects.
Three focused fixes were committed, but compilation/runtime validation is deferred
because this task expressly prohibits Gradle and server runs.

The historical census is not the current registry: `docs/analysis/2026-10-23-agent-w3multi.md:139-174`
lists 14 observed S2C and 10 observed C2S of an older 21/12 registry.
Its arithmetic leaves **seven**, not eight, older S2C channels unobserved.
Current source adds `block_sparkle_fx`, `scan_rune`, `essentia_flow_fx` (24/12).
This audit does not extend that historical wire coverage to any new channel.

## Per-packet codec and direction table

Paths in this table are `common/src/main/java/thaumcraft/common/lib/network/<Name>.java`.
All rows are **verified by reading only**. `Yes` in the dedicated-server column
means C2S dispatch stays in common/server code, or S2C has no server receiver and
client behavior is installed only by client setup. It is not a server launch result.

Wire notation: `u8/i8` = unsigned/signed byte, `i16/u16` = short,
`i32/i64` = int/long, `v` = VarInt, `sN` = UTF string with max N characters,
`id` = ResourceLocation UTF, `p` = packed BlockPos long, `xyz` = three doubles,
`b` = boolean byte. `frame[...]` prefixes the body's byte length as a VarInt.
Order below is both encode order and decode order. Signed writes paired with
unsigned reads are symmetric within validated constructor ranges.

| Payload class | Direction | Wire layout | Symmetric? | Dedicated-server handler safe? | Evidence |
|---|---|---|---|---|---|
| AccessoryContentsPayload | S2C | four native OPTIONAL_STREAM_CODEC ItemStacks | Yes | Yes | Read only |
| AccessoryOpenPayload | C2S | empty | Yes | Yes | Read only |
| AspectDiscoveryPayload | S2C | key:s128 | Yes | Yes | Read only |
| AspectPoolPayload | S2C | key:s128, amount:i16, total:i16 | Yes | Yes | Read only |
| BlockFxPayload | S2C | dimension:id, kind:v, target:p, source:v, origin:xyz, destination:xyz, rgb:i32 | Yes | Yes | Read only |
| BlockSparkleFxPayload | S2C | dimension:id, target:p, rgb:i32 | Yes | Yes | Read only |
| EssentiaFlowFxPayload | S2C | dimension:id, consumer:p, dx:i8, dy:i8, dz:i8, rgb:i32 | Yes | Yes | Read only |
| ExcavationFxPayload | S2C | dimension:id, caster:v, origin:xyz, target:xyz, block:p, crack:v, impact:b, completed:b, success:b | Yes | Yes | Read only |
| FocalManipulatorAckPayload | S2C | menu:u8, session:UUID(16), expected:i64, upgrade:u16, accepted:b, revision:i64 (36 bytes) | Yes | Yes | Read only |
| FocalManipulatorStartPayload | C2S | menu:u8, session:UUID(16), expected:i64, upgrade:u16 (27 bytes) | Yes | Yes | Read only |
| FocusSelectionPayload | C2S | hand:u8, slot:i8; pouch: hand:u8, marker127:u8, slot:u16 | Yes | Yes | Read only |
| GolemBellResetPayload | C2S | hand:u8 | Yes | Yes | Read only |
| GrowthLampFxPayload | S2C | dimension:id, target:p | Yes | Yes | Read only |
| GuardianSonicPayload | S2C | source:i32 | Yes | Yes | Read only |
| HoverTogglePayload | C2S | empty | Yes | Yes | Read only |
| InfusionFxPayload | S2C | dimension:id, matrix:p, event:u8, target:p, data:v | Yes | Yes | Read only |
| KnowledgeSyncPayload | S2C | CompoundTag NBT | Yes | Yes | Read only |
| OpenThaumonomiconPayload | S2C | empty/unit | Yes | Yes | Read only |
| ResearchNoteRequestPayload | C2S | key:s128 | Yes | Yes | Read only |
| ResearchNoticePayload | S2C | key:s256 | Yes | Yes | Read only |
| ResearchPurchasePayload | C2S | key:s128 | Yes | Yes | Read only |
| ResearchSettingsPayload | S2C | flags:u8 (5 bits), difficulty:v, aspectCap:v | Yes | Yes | Read only |
| ResearchTableCombinePayload | C2S | frame[menu:v, session:i64, revision:i64, left:s128, right:s128] | Yes | Yes | Read only |
| ResearchTableCopyPayload | C2S | frame[menu:v, session:i64, revision:i64] | Yes | Yes | Read only |
| ResearchTableEditPayload | C2S | frame[menu:v, session:i64, revision:i64, action:u8, q:i8, r:i8, aspect:s128 only for PLACE] | Yes | Yes | Read only |
| ResearchTableStatePayload | S2C | frame[menu:v, session:i64, revision:i64, count:v, count*(aspect:s128, amount:v)] | Yes | Yes | Read only |
| RunicChargePayload | S2C | source:i32, charge:i16, maximum:i16 (8 bytes) | Yes | Yes | Read only |
| RunicHitPayload | S2C | source:i32, target:i32 (8 bytes) | Yes | Yes | Read only |
| ScanRuneFxPayload | S2C | dimension:id, x:f64, y:f64, z:f64, duration:v, gravity:f32 | Yes | Yes | Read only |
| ShockFxPayload | S2C | dimension:id, caster:v, origin:xyz, target:xyz, seed:i64, impact:b, hasBlock:b+[xyz], hasEntity:b+[xyz] | Yes | Yes | Read only |
| ShovelOrientationPayload | C2S | empty | Yes | Yes | Read only |
| VisNetFxPayload | S2C | dimension:id, origin:(i32,i32,i32), target:(i32,i32,i32), rgb:i32 | Yes | Yes | Read only |
| WandArchitectPayload | C2S | empty | Yes | Yes | Read only |
| WarpEventPayload | S2C | event:i16 (validated 0..2) | Yes | Yes | Read only |
| WarpMessagePayload | S2C | delta:i32, kind:i8 | Yes | Yes | Read only |
| WispZapPayload | S2C | source:i32, target:i32 | Yes | Yes | Read only |

Framed table codecs read slices of the transmitted byte length, reject trailing
body bytes and release temporary encode buffers. PLACE/ERASE, optional Shock
points and focus pouch marker decisions use **transmitted discriminators**, not
independently computed game state. No conditional reader/writer divergence found.
NBT and ItemStack use the same native codec in each direction. Essentia's signed
byte coordinate wrap is explicitly preserved original behavior, not a mismatch.
Not every S2C codec rejects trailing bytes itself; that is not an encode/decode
asymmetry and no remotely supplied C2S authority follows from it.

## Findings

### N1 — Medium — essentia flow geometry survives world replacement (fixed)

`common/src/main/java/thaumcraft/client/fx/EssentiaFlowClientEffects.java:32-44,95-138`:
the static map keys only coordinates/color; Flow stores no dimension or level.
Before this change init registered only ticks, not level-load/quit cleanup.
The tick's null-level check was the only automatic cleanup. Switching directly
to another non-null level replays the old flow at the same coordinates. If those
chunks are absent, line 130's continue prevents lifetime decrement indefinitely.
A reconnect not separated by an observed null-level tick can also retain it.
The pre-existing `clear()` comment promised cleanup that was never wired.

Fix `16a45bde`: register level-load and quit cleanup, and include it in
`ClientStateReset.reset()` for the loader's explicit disconnect path. Only visual
lifetime is affected; no resource, transfer, byte delta or search behavior changes.

### N2 — Medium — dimension queues retain particles bound to an obsolete world (fixed)

`common/src/main/java/thaumcraft/client/fx/ParticleEngine.java:11-27` keys queues by
dimension ResourceKey, but each Particle retains an actual ClientLevel.
`common/src/main/java/thaumcraft/common/mixin/client/Tc4ParticleEngineMixin.java:22-26`
previously cleared only for `next == null`. Inactive dimension queues do not tick,
retain their old world, and resume on returning to a new level with the same key.
This is a **dimension-change** leak, not a missing disconnect clear: disconnect
already cleared the queue correctly.

Fix `3a38e50e`: clear when the actual ClientLevel identity changes. Explicit
modern lifecycle safety deviation from the original dimension-map retention
documented in `ParticleLayers.java:10`: the old world's live object references
must not be rendered/ticked in its replacement. FIFO caps and active-world
particle behavior remain unchanged. No original JAR/decompile was edited.

### N3 — Medium — research edit/combine do not reject spectator/dead actors (fixed)

`common/src/main/java/thaumcraft/common/lib/network/ResearchTableEditNetworking.java:60-68`
and `ResearchTableCombineNetworking.java:29-37` bind the sender/menu/session but
delegate permission to `menu.stillValid`. Before the fix,
`common/src/main/java/thaumcraft/common/container/ContainerResearchTable.java:192`
delegated only to tile presence/distance; `common/.../tiles/TileResearchTable.java:164-169`
checks neither actor liveness nor spectator mode. Unlike vanilla slot clicks,
these custom payloads do not inherit vanilla spectator rejection. A currently
bound spectator menu can therefore mutate shared notes/ink/bonus pools and actor
knowledge. Copy already has an explicit spectator rejection at
`ResearchTableCopyNetworking.java:40`.

Fix `506dca3a`: require actual viewer, alive and non-spectator in the shared menu
validity check, preserving the existing distance, chunk and tile-identity checks.
This is authority hardening, not a change to TC4 costs/research/randomness.
Reachability timing across game-mode changes was not executed; the missing
admission conditions and mutation path are source-established.

### N4 — Low — registered essentia channel has no base-game caller (not changed)

`common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java:34-48` is the
only production caller of `EssentiaFlowFxNetworking.sendEssentiaFlow`.
There are **no production callers of that public drainEssentia helper**.
The built-in matrix instead calls `common/.../lib/events/EssentiaHandler.java:29-48`,
which sends `infusion_fx` event 1 through `matrix.sendEffect`, not this channel.
The public API can be called by an addon, so absolute impossibility is not claimed.
This channel is dormant in base content, despite a valid sender/codec/receiver.
Do not add a second matrix effect just to increase census coverage: that would
duplicate the existing infusion effect. A future wire test must invoke a genuine
public API consumer or explicitly label this channel API-only.

## Channels that can never fire

**No globally impossible channel was established. One base-game dead/API-only
channel:** `essentia_flow_fx` (N4). All other 35 have production entry paths;
no missing client handler was found. Never-observed does not mean unreachable.

The seven historical unobserved S2C channels and two C2S are identified below by
subtracting the historical observed list from that generation's registry:

| Channel | Production sender / receiver (paths under common/src/main/java/thaumcraft/) | Can fire by source? |
|---|---|---|
| excavation_fx | common/items/wands/foci/ItemFocusExcavation.java:165 → client/ThaumcraftExcavationClient.java:60,78 | Funded held focus tick; yes |
| shock_fx | common/items/wands/foci/ItemFocusShock.java:151,186 → client/ThaumcraftShockClient.java:46 | Committed shock/chain; yes |
| infusion_fx | common/tiles/TileInfusionMatrix.java:434 → client/ThaumcraftInfusionClient.java:25 | Real matrix action; yes |
| growth_lamp_fx | common/tiles/TileArcaneLampGrowth.java:35 → client/ThaumcraftBiologicalLampClient.java:19 | Observed plant state change; yes |
| vis_net_fx | common/Thaumcraft.java:192 effect sink → common/lib/network/VisNetFxNetworking.java:53 → client/ClientSetup.java:178 | Committed drain, per-consumer throttle; yes |
| runic_hit | common/lib/runic/RunicShield.java:104 → client/ThaumcraftRunicClient.java:25,33 | Shield hit; yes |
| focal_manipulator_ack | common/lib/network/FocalManipulatorNetworking.java:60 → client/ThaumcraftFocalClient.java:33 | Valid current menu receives start/refusal; yes |
| focal_manipulator_start (C2S) | client/gui/GuiFocalManipulator.java:145 → common/lib/network/FocalManipulatorNetworking.java:20,42 | Eligible selected upgrade/current menu; yes |
| research_table_copy (C2S) | common/container/ContainerResearchTable.java:143-157 → common/lib/network/ResearchTableCopyNetworking.java:24,34 | Completed discovery, RESEARCHDUPE, paper, **black dye**; yes |

The three newer S2C types absent from that historical census:
`block_sparkle_fx`: WandManager.java:61 and other real senders →
ClientSetup.java:61; `scan_rune`: ItemThaumometer.java:117 → ClientSetup.java:63;
`essentia_flow_fx`: AspectSourceHelper.java:47 → ClientSetup.java:67 (N4).
All paths above were read, none was exercised here. The historical report's
ink-sac wording is not current code: copy's server and client both use BLACK_DYE.

## State not reset on reconnect

* **Missed:** essentia flow map/tick phase (N1), now explicitly cleared on quit,
  loader reset and level load. Previously depended on seeing a null-level tick.
* **Dimension-only missed:** old-level TC4 particle queues (N2), now cleared on
  level identity replacement. Disconnect and resource reload already cleared them.
* `EventHandlerNetwork.clientLogsOut` in the uncompiled legacy tree
  (`src/main/java/.../EventHandlerNetwork.java:8-20`) restores seven config fields.
  Modern `ResearchSettingsNetworking.java:29` and `ClientStateReset.java:41`
  restore the client's own settings, rather than preserving the last server's.
* Knowledge and accessory snapshots are attached to Player mixins, not singleton
  client knowledge/inventory maps; a fresh player gets fresh holders.
  KnowledgeEvents.java:34-35 resyncs respawn/dimension changes; accessories join
  and respawn sync in PlayerAccessories.java:29,38.
* Shock and Excavation register their own quit **and level-load** clears;
  Infusion clears on quit and tracks actual ClientLevel identity. They are not
  omissions just because ClientStateReset does not name them.
* Warp, runic HUD, notifications/research popups, travel gear, taint spores and
  scan overlay are cleared by ClientStateReset. Resource/model/shader caches
  are not per-world gameplay state. Runic values are per local HUD, not server
  gameplay globals. No additional reconnect leak was source-established.

## Trust boundaries and multiplayer invariants

All 12 C2S receiver lambdas queue onto their context and derive ServerPlayer
from `context.getPlayer()`. **No modern C2S payload carries a BlockPos, world,
target player id, stack NBT, balance or permissions.** Research keys are intents
evaluated against server catalog/prerequisites/costs; table actions use the
current server menu's tile, session and revision. Focal admission additionally
uses menu UUID, actual tile lifetime, actor identity, distance and liveness
(`ContainerFocalManipulator.java:71-100`). Item-key requests re-read actual
held/worn items; Hover validates the canonical live player (`Hover.java:39,52-57`).
Research edit/combine's permission omission is N3, not arbitrary-position trust.

* Knowledge: `KnowledgeEvents.java:56` sends **KnowledgeAccess.get(player)** only
  to that player. `KnowledgeAccess.java:10-15` resolves the player's mixin holder.
* Accessories: `AccessoryNetworking.java:40-49` snapshots **PlayerAccessories.get(player)**
  and sends only to that player. LAST is a WeakHashMap keyed by actual ServerPlayer,
  not one last inventory for the whole server. PlayerAccessories.java:26 uses
  the actual actor's holder; charging ThreadLocals include and validate that actor.
* Bell reset takes only a hand. `ItemGolemBell.java:137-155,192-199` resolves the
  actual held bell and its server-side selected golem, checks loaded entity,
  dimension and `golem.canEditLinks(player)`. No global selection. Original
  long-range link editing is intentional, not a client-supplied block target.
* Nearest player: `TileEldritchTrap.java:51-63` reuses one selected local player
  for damage, warp and bolt endpoint; creative exclusion is delegated to
  `OriginalPlayerSearch.closestPlayer`. No global/current-client actor is used.

These checks do not replace the historical two-client acceptance and do not
claim any new multiplayer runtime evidence.
