# The second physical client — Thaumcraft 4.2.3.5 → Minecraft 1.21.1 (Fabric)

*Agent:* `w3multi` (wave 3) — *worktree:* `/home/cyberfox-kali/wt/w3multi` — *branch:* `agent/w3multi`
*Date:* 2026-10-23

## 0. The gap this wave closes

The wave 2 acceptance (`docs/analysis/2026-10-23-agent-multiplayer.md`) ran **one**
physical client against a real dedicated server. It proved that the payload
registries of a physical client are complete and that eight clientbound and six
serverbound channels really crossed the socket, but it left three things open:

1. **no second physical client** — every "second player" behaviour was
   unverified over a real connection;
2. **the clientbound FX payloads** were proven *registered and decodable*, not
   *observed on the wire*;
3. the two-player behaviours that a single client can never see: the research
   table's partner half, the accessory menu of the other player, the golem bell
   and the travelling trunk of the other player, and the warp/creative rules
   that treat the two players differently.

**This report records the first acceptance of the port that runs two physical
clients** — two separate JVMs, two GL contexts, two Fabric payload registries,
two connections — against one real dedicated server. The accepted run passed
**38 of 38 stages**.

## 1. The setup

### 1.1 The processes

| process | Gradle task | JVM | notes |
|---|---|---|---|
| dedicated server | `:fabric:runMultiplayerServer` | own | run dir `fabric/run-multiplayer-server/`, `--nogui`, flat world |
| the SECOND physical client | `:fabric:runMultiplayerPeerClient` | own | run dir `fabric/run-multiplayer-peer/`, player `MPProbe2` |
| the LEAD physical client | `:fabric:runMultiplayerClient` | own | run dir `fabric/run-multiplayer/`, player `MPProbe` |

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/w3multi/.gradle-mp
export DISPLAY=:0
export XAUTHORITY=/home/cyberfox-kali/.xauth-display0
./gradlew --console=plain :fabric:runMultiplayerServer
./gradlew --console=plain :fabric:runMultiplayerPeerClient \
    -PmultiplayerAddress=127.0.0.1:25565 -PmultiplayerPeerPlayer=MPProbe2
./gradlew --console=plain :fabric:runMultiplayerClient \
    -PmultiplayerAddress=127.0.0.1:25565 -PmultiplayerPlayer=MPProbe
```

The whole cycle is scripted by `.tmp/mp-pair-cycle.sh` (untracked helper):
`stop; start the server; start the second client; start the lead; report`. The
three processes ran **at the same time** (the server and the two clients), each
with its own `-Xmx1024M` (the server `-Xmx640M`) — the two clients share the
one X display of the host and open one window each.

**The heap of the clients cannot be smaller than 1024 MB.** A first attempt
with `-Xmx640M` died with `OutOfMemoryError: Java heap space` in the
`Worker-Main` thread of the resource reload, before the game even started. The
wave 2 acceptance used 768 MB for its single client; the second client adds
another JVM, so the acceptance needed 1024 MB each and the server 640 MB. The
peak of the three processes was about 2.4 GB of heap on a host that shares
15 GB with the other wave agents; the OOM killer did not touch any of the
three processes during the accepted run.

The server runs `online-mode=false` (the acceptance needs an unauthenticated
login), `level-type=minecraft:flat`, `difficulty=peaceful` and
`max-tick-time=-1`; `ops.json` lists `MPProbe` and `MPProbe2` at permission
level 4. The world is deleted before every run, so every acceptance starts from
a fresh 1.21.1 world.

### 1.2 The harness

The harness lives in the `clientTest` source set and **never ships in a
production archive**:

* `fabric/src/clientTest/java/thaumcraft/fabric/clienttest/MultiplayerPairLink.java`
  — the payload census and the file link between the two client processes;
* `fabric/src/clientTest/java/thaumcraft/fabric/clienttest/MultiplayerPeerProbe.java`
  — the SECOND physical client;
* `fabric/src/clientTest/java/thaumcraft/fabric/clienttest/MultiplayerClientProbe.java`
  — the LEAD client with the new wire and pair stages;
* `fabric/src/clientTest/java/thaumcraft/fabric/clienttest/mixin/MultiplayerCensusMixin.java`
  — installs the census before the first play packet is read.

`common/build.gradle:37-44` rejects any `clienttest`/`gametest` output in a
production jar; `:fabric:verifyProductionIsolation` and
`:fabric:verifyTestIsolation` ran green on this branch (§7).

### 1.3 How the two clients talk

Two JVMs cannot share memory, so the lead writes one command file and the peer
writes one answer file, both in `fabric/run-multiplayer/pair/`:

```
lead.cmd     <sequence>\n<TOKEN> <arguments>
peer.result  <sequence>\n<answer>
peer.status  <key>=<value> ...
```

Every write is atomic (write to a temporary file, then an atomic rename), and
the reader takes the sequence and the body with **one** read — two reads are a
race, because the writer can replace the file between them.

## 2. The accepted run

`fabric/run-multiplayer/multiplayer-client-report.json`, produced by
`./gradlew :fabric:runMultiplayerClient -PmultiplayerAddress=127.0.0.1:25565
-PmultiplayerPlayer=MPProbe` against the dedicated server of
`:fabric:runMultiplayerServer`, with the second client of
`:fabric:runMultiplayerPeerClient` connected to the same server:

```
STATUS SCRIPT_PASSED
   PASS connect                        PASS wire_open_thaumonomicon
   PASS pair_peer_joined               PASS wire_research_table_combine
   PASS teleport_to_work_area          PASS wire_research_table_copy
   PASS payload_type_registries        PASS wire_client_toggles
   PASS reset_research                 PASS wire_eldritch_trap
   PASS knowledge_sync                 PASS wire_eldritch_warp_event
   PASS server_aspect_crosscheck       PASS wire_guardian_sonic
   PASS research_note_request          PASS wire_wisp_zap
   PASS server_research_grant          PASS pair_per_player_knowledge
   PASS research_purchase              PASS pair_accessory_isolation
   PASS focus_selection                PASS pair_golem_bell_owner
   PASS golem_bell                     PASS pair_traveling_trunk_owner
   PASS accessory_menu_over_network    PASS pair_research_table_partner
   PASS research_table_menu_over_network   PASS pair_creative_player_excluded
   PASS research_table_edit_over_network   PASS disconnect
   PASS golem_placement_over_network   PASS reconnect
   PASS traveling_trunk_over_network   PASS reconnect_state_consistent
   PASS visnet_block_over_network       PASS pair_peer_stop
   PASS arcane_bore_over_network
```

The second client's own report (`fabric/run-multiplayer-peer/multiplayer-peer-report.json`)
is `SCRIPT_PASSED` as well.

### 2.1 The payload census of the accepted run

**Clientbound (server → client), 14 distinct Thaumcraft channels crossed the
socket of the lead** (the wave 2 acceptance saw 8):

| payload | count | what it carried |
|---|---|---|
| `thaumcraft:knowledge_sync` | 20 | the authoritative knowledge snapshot of the lead |
| `thaumcraft:aspect_pool` | 7 | the research point receipts |
| `thaumcraft:research_table_state` | 8 | the research table's generation/revision/bonus state |
| `thaumcraft:runic_charge` | 20 | the runic charge HUD |
| `thaumcraft:warp_message` | **6** | the warp deltas of the eldritch trap |
| `thaumcraft:block_fx` | **11** | the node bolt of the runed eldritch trap |
| `thaumcraft:accessories_contents` | 2 | the four worn accessory stacks |
| `thaumcraft:research_settings` | 2 | the server's research configuration |
| `thaumcraft:aspect_discovery` | **1** | the newly discovered aspect `tempestas` |
| `thaumcraft:open_thaumonomicon` | **1** | the thaumonomicon of the original book |
| `thaumcraft:research_notice` | 1 | the purchased research notice |
| `thaumcraft:warp_event` | **1** | the eldritch guardian's warp event |
| `thaumcraft:guardian_sonic` | **1** | the eldritch guardian's sonic screech |
| `thaumcraft:wisp_zap` | **1** | the wisp's zap |
| `fabric-screen-handler-api-v1:open_screen` | 5 | the accessory, research table and travelling trunk menus |

**Serverbound (client → server), 10 distinct Thaumcraft channels crossed the
socket** (the wave 2 acceptance saw 6):

| payload | count | what it carried |
|---|---|---|
| `thaumcraft:research_note_request` | 1 | the primary-note issuance intent |
| `thaumcraft:research_purchase` | 1 | the secondary purchase intent |
| `thaumcraft:focus_selection` | 1 | the wand focus intent |
| `thaumcraft:golem_bell_reset` | 1 | the golem bell reset intent |
| `thaumcraft:accessories_open` | 1 | the accessory menu intent |
| `thaumcraft:research_table_edit` | 1 | the table cell placement intent |
| `thaumcraft:research_table_combine` | **1** | the table aspect combination intent |
| `thaumcraft:architect_toggle` | **1** | the architect wand toggle intent |
| `thaumcraft:hover_toggle` | **1** | the hover harness toggle intent |
| `thaumcraft:shovel_orientation` | **1** | the elemental shovel orientation intent |

**The census of the SECOND client** (`fabric/run-multiplayer-peer/multiplayer-peer-report.json`)
proves that the same channels reached the other connection as well:
`accessories_contents 2`, `knowledge_sync 3`, `research_settings 1`,
`research_table_state 1`, `block_fx 1`, `warp_message 2`, `runic_charge 48`,
and the serverbound `accessories_open 1`.

### 2.2 The decisive evidence of the run

```
pair_player_list_size          = 2      # both clients see two players
pair_peer_name                 = MPProbe2
pair_peer_uuid                 = 2ab1c3ab-710f-3217-9e86-a6da11f8e6cf
pair_peer_player_list          = 2      # the peer sees the lead as well
pair_peer_research_answer      = HAS BOTTLETAINT 1
pair_lead_has_peer_research    = False  # the lead never received the peer's research
pair_peer_has_own_research     = True
pair_peer_accessories_payloads_before = 1
pair_peer_accessories_payloads_after  = 2   # the snapshot of the equipped amulet
pair_lead_accessories_payloads = 1      # the lead never received the peer's snapshot
pair_lead_accessory_slots      = 0      # and the lead's own slots stay empty
pair_golem_owner_on_client     = MPProbe2
pair_golem_bell_refusal        = You are not my Master!
pair_lead_bell_selected        = False  # the lead's bell never took the golem
pair_peer_bell_answer          = BELL selected=1   # the owner may select it
pair_trunk_owner_on_client     = MPProbe2
pair_trunk_upgrade_on_client   = 3      # the water trunk
pair_peer_trunk_answer         = TRUNK open=1      # the owner may open it
pair_lead_trunk_opened         = False  # the stranger may not
pair_research_table_variant    = 2      # the original main half (north)
pair_research_table_partner_variant = 7 # the original partner half (north)
pair_peer_table_main           = 99,-60,101   # the peer opened the MAIN half
pair_lead_table_main           = 99,-60,101   # and so did the lead
pair_peer_gamemode_answer      = ... gm=creative ...
pair_lead_warp_before_trap     = 7
pair_lead_warp_after_trap      = 8      # the trap warped the non-creative player
pair_peer_warp_before_trap     = 2
pair_peer_warp_after_trap      = 2      # and never the creative one
wire_combined_aspect           = tempestas
wire_lead_discovered_aspects   = [aer, aqua, ignis, lux, ordo, perditio, sensus, tempestas, tenebrae, terra]
```

## 3. What the acceptance proved about the port

### 3.1 The second physical client is a real client

`pair_peer_joined` proves that two independent Minecraft clients, in two
independent JVMs, with two independent Fabric payload registries, are connected
to one dedicated server at the same time and see each other:
`pair_player_list_size = 2` on the lead, `pair_peer_player_list = 2` on the
peer. The peer's own report is `SCRIPT_PASSED`.

The second client registers **all 21 clientbound and all 12 serverbound**
Thaumcraft payload types of its own registry — the same list the lead
registers — and every stage that needs one of them decodes it on its own
socket.

### 3.2 The knowledge snapshot is per player

The server granted the SECOND client the research `BOTTLETAINT`
(`thaumcraft research MPProbe2 BOTTLETAINT`) while the lead watched. The
second client received it (`HAS BOTTLETAINT 1`), the lead **never** did
(`pair_lead_has_peer_research = False`). The lead's own knowledge snapshot is
sent by `KnowledgeEvents.sync(player)`
(`common/src/main/java/thaumcraft/common/lib/network/KnowledgeEvents.java:52-56`)
with `NetworkManager.sendToPlayer`, so the two players never share a snapshot.

### 3.3 The accessory snapshot is per player

The second client wore a vis amulet (the original's `VISAMULET` research was
granted to it first, because `ItemAmuletVis.canEquip` demands it — oracle
`ItemAmuletVis` and the port
`common/src/main/java/thaumcraft/common/items/baubles/ItemAmuletVis.java:54-56`).
Its own client received a second `accessories_contents` snapshot
(`1 -> 2`) and its accessory slot was filled (`EQUIP count=1`); the **lead's
client received no snapshot of it at all** (`pair_lead_accessories_payloads = 1`,
which is only the lead's own join-time snapshot) and the lead's own accessory
slots stayed empty (`pair_lead_accessory_slots = 0`).

### 3.4 The golem bell of the other player is refused

The second client owns a straw golem (`{Owner:"MPProbe2"}`). When the lead
used its own golem bell on that golem, the server refused the interaction with
the original's own words — **`You are not my Master!`** — and the lead's bell
never took the golem (`pair_lead_bell_selected = False`). The owner itself
could select its own golem with its own bell (`BELL selected=1`).

This is the original's rule: the oracle cancels **every** interaction with a
golem that has an owner when the interacting player is not that owner
(`thaumcraft/common/lib/events/EventHandlerEntity.java:347-356`), and the port
implements it in `GolemOwnership.denies`
(`common/src/main/java/thaumcraft/common/items/golems/GolemOwnership.java:37-40`),
which `EntityGolemBase.mobInteract` uses
(`common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java:2332-2339`).

### 3.5 The travelling trunk of the other player is refused

The second client owns a **water** trunk (upgrade 3, the upgrade the original
restricts to its owner: the oracle's
`ItemGolemBell.onLeftClickEntity:252-256` compares the trunk's owner UUID with
the player's). The owner opened it (`TRUNK open=1`), the lead could not
(`pair_lead_trunk_opened = False`). The port implements the rule in
`TrunkRules.mayOpen` (`common/src/main/java/thaumcraft/common/items/golems/TrunkRules.java:37`)
and `EntityTravelingTrunk.mobInteract`
(`common/src/main/java/thaumcraft/common/entities/golems/EntityTravelingTrunk.java:229`).

### 3.6 The research table's partner half resolves to the main half — for both players

The research table is a **pair**: the main half carries the original's variant
`2` (north) and the partner half the variant `7` (north) — the same two values
the wave 2 acceptance measured on one client. This time **both** players
opened the same table: the lead right-clicked the main half and the second
client right-clicked the **partner** half, and both menus referred to the same
position `99,-60,101` (`pair_lead_table_main = pair_peer_table_main`).

That is the original's rule of `BlockTable:187-196`: a partner half has no
research table of its own, it opens the neighbour that has one. The port
implements it in `BlockTable.findOpeningMain`
(`common/src/main/java/thaumcraft/common/blocks/BlockTable.java:129-141`).

### 3.7 The original's closest-player rule never finds a creative player

The runed eldritch trap looks for the closest player within three blocks and
warps half of its hits. The second client stood **closer** to the trap than the
lead and was in **creative** mode. The result:

* the lead (not creative) was warped — `pair_lead_warp_after_trap = 8` against
  `pair_lead_warp_before_trap = 7`;
* the creative second client was **not** warped — `pair_peer_warp_after_trap = 2`
  against `pair_peer_warp_before_trap = 2`.

This is the original's `World.getClosestPlayer`, which skips a creative player
(oracle `World.getClosestPlayer`: `if (!entityplayer1.isCreative() && ...)`),
and the port keeps it in `OriginalPlayerSearch.accepts`
(`common/src/main/java/thaumcraft/common/lib/entities/OriginalPlayerSearch.java:84-88`),
which `TileEldritchTrap` uses
(`common/src/main/java/thaumcraft/common/tiles/TileEldritchTrap.java:51-52`).

### 3.8 The clientbound FX channels really cross the wire

The seven new clientbound channels and the four new serverbound channels of
§2.1 were produced by real actions of the client and counted in the netty
census **of the connection**, not by a registry:

| channel | what the harness did |
|---|---|
| `open_thaumonomicon` | used the thaumonomicon of the original book |
| `aspect_discovery` | combined `aer` + `aqua` in the research table, which discovers `tempestas` |
| `block_fx` | the runed eldritch trap drew its node bolt |
| `warp_message` | the same trap warped the player |
| `warp_event` | the eldritch guardian's warp event (the original sends it to every player whose **squared** distance is below 256, oracle `EntityEldritchGuardian.java:162-168`) |
| `guardian_sonic` | the same guardian's sonic screech (its ranged attack, twenty to forty blocks) |
| `wisp_zap` | the wisp's zap |
| `research_table_combine` | the research table's aspect combination |
| `architect_toggle` | the architect wand's misc toggle |
| `hover_toggle` | the hover harness's misc toggle |
| `shovel_orientation` | the elemental shovel's misc toggle |

The two channels of the original's **two different radii** needed two stages:
the warp event needs the guardian within sixteen blocks, its sonic screech
needs the guardian at twenty to forty blocks, so the harness summons it twice
and keeps the distance with a repeated teleport.

## 4. Findings

### 4.1 No defect of the port was found by the two-client acceptance

The second physical client found **no** multiplayer defect in the common
layer: every per-player channel reaches exactly its own player, every
ownership rule of the original refuses the stranger and accepts the owner,
the research table's partner half resolves to the main half for both players,
and the original's closest-player rule skips the creative player.

### 4.2 The defects the acceptance found were in the HARNESS

Nine defects of the acceptance itself were found and fixed during the runs.
They are listed here because they are the reason a naive two-client acceptance
reports false failures:

1. **the census missed every join-time payload.** The server sends the
   knowledge snapshot, the research settings and the accessory contents in the
   same burst as the login packet, and the Fabric join event only fires while
   that burst is already being processed. The census is now installed by a
   `clientTest` mixin when the play connection is **built**
   (`MultiplayerCensusMixin`), and the peer reports
   `census_acc=1 census_know=1 census_settings=1` right after its join.
2. **the pair link read the sequence and the body with two reads.** The writer
   can replace the file between the two reads, so a reader saw the sequence of
   one answer and the body of the next one. The link reads the whole entry at
   once now, and the peer answers with the sequence of the command it answers.
3. **the vanilla carried item packet only accepts a HOTBAR slot.** The
   harness selected an inventory slot, and the server refused it with
   *"tried to set an invalid carried item"*. The item is now put into the hotbar
   by the **server** (`/item replace entity <player> hotbar.0 with <item>`),
   because the state id of the closed player inventory is stale on the client
   and a client-side click is rejected.
4. **two pick up clicks leave the stack on the cursor**, and the server then
   refuses the accessory menu (the original refuses it while the cursor is not
   empty). The accessory equip and the hotbar move use the vanilla **quick
   move** now, which never touches the cursor.
5. **the SNBT int array of an UUID needs SIGNED 32 bit values.** The harness
   wrote the unsigned value, so the travelling trunk of the second client had a
   wrong owner and its owner could not open it.
6. **the golem bell in the hand makes the travelling trunk interaction PASS**,
   so the trunk never opened. The peer uses an empty hand for the trunk.
7. **the original's vis amulet may only be worn with the `VISAMULET`
   research** (`ItemAmuletVis.canEquip`), so the second client could not equip
   it until it was granted that research.
8. **the vanilla commands that name a player are SIGNED commands**, and the
   offline-mode server drops a signed command whose signature does not match
   its chat chain **silently**. The research grant is repeated until the second
   client really reports the research, and the second client runs the vanilla
   `give` itself.
9. **a new question invalidates an answer that is still on its way.** The
   second client of an oversubscribed host can need more than a second to
   answer, so the question is asked again only after the answer of the
   previous question arrived.

### 4.3 The wave 2 findings that the second client confirms

The second client confirms the wave 2 result of the research table: the table
is a **pair**, and the partner half of variant `7` opens the main half of
variant `2`. The wave 2 acceptance measured the two variants on one client;
this acceptance measured them with **two** clients opening the two halves.

## 5. What is still unverified

* **Seven clientbound channels and two serverbound channels were not observed
  on the wire.** They are registered and decodable on both clients, but the
  harness could not trigger them:
  * `excavation_fx` and `shock_fx` need a wand with vis; the wand of the
    acceptance has no vis, and the original's vis sources (a node, a furnace)
    were not built.
  * `infusion_fx` needs an infusion matrix with a running infusion.
  * `growth_lamp_fx` needs the arcane growth lamp with a growing plant.
  * `vis_net_fx` needs a vis net that actually drains between a source and a
    consumer.
  * `runic_hit` needs a runic bauble that is actually hit.
  * `focal_manipulator_ack` and `focal_manipulator_start` need the focal
    manipulator machine with its menu.
  * `research_table_copy` — the client **refused** the intent, because the
    original's copy needs a *completed* discovery note, the `RESEARCHDUPE`
    research, a paper and an ink sac. The refusal is recorded as
    `wire_research_table_copy_observed = False` with its reason; the payload
    itself was therefore never sent.
* **The NeoForge loader was not exercised at all**; the acceptance is
  Fabric-only.
* **The overworld world generation is not covered** — the acceptance server
  uses a flat world, because a dedicated server stays responsive on an
  oversubscribed build host.
* **The acceptance is one acceptance, not a matrix.** The two clients ran on
  one host over the loopback socket; a real network with latency, packet loss
  and a proxy was not tested.
* **The two clients were on the same X display and the same machine**, so the
  two processes competed for the CPU: the second client was sometimes more
  than a second late with an answer, which is exactly the flakiness that the
  ninth finding of §4.2 had to absorb.
* The `golem_placement_over_network` stage delivered the golem with the
  server's `/summon` (`golem_delivered_by = summon`) instead of the original's
  placer item, because the vanilla `give` of the placer is not reliable enough
  on an offline-mode server. The original's placer path was verified by the
  wave 2 acceptance.

## 6. Production changes

**None.** Every change of this wave is inside the `clientTest` source set, the
loom run definitions of the `fabric` module, the `.gitignore` and this report.
The port's gameplay code is unchanged, because the two-client acceptance found
no defect in it.

## 7. The machine gates

```
python3 scripts/porting/update_scope_ledger.py --check \
    --original-root /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle
-> stale_review_fingerprints: 0
```

The scope ledger is **unchanged**: this wave changed no production unit, so no
row of `docs/porting/scope-ledger.json` was touched.

`:fabric:verifyProductionIsolation` and `:fabric:verifyTestIsolation` ran
green on this branch, so the harness of the second physical client does not
leak into a production archive.
