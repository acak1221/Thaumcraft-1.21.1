# Multiplayer acceptance — Thaumcraft 4.2.3.5 → Minecraft 1.21.1 (Fabric)

*Agent:* `multiplayer` (wave 2) — *worktree:* `/home/cyberfox-kali/wt/multiplayer` — *branch:* `agent/multiplayer`
*Date:* 2026-10-23

## 0. Why this document exists

Every acceptance the port had so far ran a **dedicated server with mock players** or a
**single-player integrated client**. Both share one JVM and therefore one Fabric
`PayloadTypeRegistry`, so a physical client that never registered a payload type still
"worked", and a serverbound packet was never really encoded on a client that has to
register it itself.

This report records the first acceptance of the port that runs **two separate JVMs**:
a real dedicated server (`:fabric:runServer`) and a real physical client
(`:fabric:runMultiplayerClient`) that connect over the loopback socket. Every payload
below crossed a real socket in both directions.

## 1. The setup

### 1.1 Processes

| Process | Gradle task | JVM | Notes |
|---|---|---|---|
| dedicated server | `:fabric:runServer` (now `:fabric:runMultiplayerServer`) | own | run dir `fabric/run-multiplayer-server/`, `--nogui` |
| physical client | `:fabric:runMultiplayerClient` | own | run dir `fabric/run-multiplayer/`, `--quickPlayMultiplayer 127.0.0.1:25565` |

Both use the **same** `fabric/run-multiplayer-server` and `fabric/run-multiplayer`
directories as untracked, git-ignored run state. The harness source lives in
`fabric/src/clientTest/java/thaumcraft/fabric/clienttest/MultiplayerClientProbe.java`
(the `clientTest` source set), so **none of it ships in the production archive** —
`common/build.gradle:37-44` rejects any `clienttest`/`gametest` output in a production
jar and `:fabric:verifyProductionIsolation` was run as part of `:fabric:build`.

### 1.2 Server configuration (test only, git-ignored)

`fabric/run-multiplayer-server/server.properties`:

```
online-mode=false          # the acceptance needs an unauthenticated login
server-port=25565
level-type=minecraft:flat  # the acceptance measures the NETWORK layer, not worldgen
view-distance=4
simulation-distance=4
max-tick-time=-1           # the vanilla watchdog kills a server on a slow host
difficulty=peaceful
```

`fabric/run-multiplayer-server/ops.json` lists the probe player `MPProbe`
(offline UUID `9b9ed44f-4703-36ed-9343-80f6bd3431a1`) at permission level 4, so the
client can drive the server with the vanilla `/give`, `/tp`, `/setblock`, `/summon`,
`/data get entity` and the port's own `/thaumcraft` commands.

**Nothing of this is a production setting.** `server.properties` and `ops.json` live in
a git-ignored run directory and are created by the acceptance script only.

### 1.3 The client harness

`MultiplayerClientProbe` is a `ClientModInitializer` that is only active with
`-Dthaumcraft.multiplayerClient=true`. It never mocks a player and never calls a
server handler directly: every stage can only succeed when a real packet crossed the
real socket.

It also installs a **netty census** on the play connection
(`MultiplayerClientProbe.installCensus`). The census must be inserted **before** the
native `Connection` handler in the pipeline, because `Connection` extends
`SimpleChannelInboundHandler` and therefore never forwards an inbound packet to a
handler placed after it. The census records every clientbound
`ClientboundCustomPayloadPacket` id, every serverbound
`ServerboundCustomPayloadPacket` id and every system chat message.

## 2. The commands

```bash
# the dedicated server
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/multiplayer/.gradle-mp
./gradlew --console=plain :fabric:runMultiplayerServer

# the physical client (DISPLAY/XAUTHORITY as the leader established)
export DISPLAY=:0
export XAUTHORITY=/home/cyberfox-kali/.xauth-display0
./gradlew --console=plain :fabric:runMultiplayerClient \
    -PmultiplayerAddress=127.0.0.1:25565 -PmultiplayerPlayer=MPProbe
```

The client task owns its own evidence: `fabric/run-multiplayer/multiplayer-client-report.json`
(`status`, `steps`, `failures`, `evidence`, `clientbound_payloads`, `serverbound_payloads`,
`server_messages`). The Gradle task `runMultiplayerClient` fails the build unless the
report says `SCRIPT_PASSED`.

The whole cycle is scripted by `.tmp/mp-cycle.sh` (untracked helper):
`stop_server; start_server; run_client`.

## 3. The acceptance stages and their results

See §6 for the final run's numbers.

| # | Stage | What it proves | Result |
|---|---|---|---|
| 1 | `connect` | the physical client joins a dedicated server over the socket | PASS |
| 2 | `teleport_to_work_area` | the vanilla spawn protection does not block the later interactions | PASS |
| 3 | `payload_type_registries` | **the physical client registered all 21 clientbound and all 12 serverbound Thaumcraft payload types** | PASS |
| 4 | `reset_research` | the server's privileged research reset reaches the client | PASS |
| 5 | `knowledge_sync` | the clientbound knowledge snapshot is applied to the client's knowledge | PASS |
| 6 | `server_aspect_crosscheck` | the server's own aspect list and the client's snapshot are **equal** | PASS |
| 7 | `research_note_request` | the primary-note issuance over the C2S channel | PASS |
| 8 | `server_research_grant` | a server-side grant arrives at the client through the snapshot | PASS |
| 9 | `research_purchase` | the secondary purchase with its debit and its clientbound receipts | PASS |
| 10 | `focus_selection` | the wand focus selection over the C2S channel | PASS |
| 11 | `golem_bell` | the golem bell reset over the C2S channel | PASS |
| 12 | `accessory_menu_over_network` | **the accessory menu opens from a real server menu packet** | PASS |
| 13 | `research_table_menu_over_network` | the original two-table + inkwell conversion and the research table menu | PASS |
| 14 | `research_table_edit_over_network` | a real table edit (a cell placement) over the C2S channel | PASS |
| 15 | `golem_placement_over_network` | the original golem placement produces a golem **on the client** | PASS |
| 16 | `traveling_trunk_over_network` | the travelling trunk entity reaches the client | PASS |
| 17 | `visnet_block_over_network` | a vis relay block and its block entity reach the client | see §5 |
| 18 | `arcane_bore_over_network` | the arcane bore block reaches the client | see §5 |
| 19 | `disconnect` | the client can drop the real connection | PASS |
| 20 | `reconnect` | the client reconnects to the same dedicated server | PASS |
| 21 | `reconnect_state_consistent` | the research and aspect state survived the reconnect | PASS |

## 4. What the acceptance proved about the port

### 4.1 The physical client knows every payload type in both directions

This is the one property a single-player run can never prove: the integrated server and
the client share one `PayloadTypeRegistry`, so a missing client-side registration is
invisible.

`MultiplayerClientProbe.stagePayloadTypeRegistries` reads Fabric's
`PayloadTypeRegistryImpl.PLAY_S2C` / `PLAY_C2S` on the **physical** client and requires
all of them:

* clientbound (21): `accessories_contents`, `aspect_discovery`, `aspect_pool`,
  `block_fx`, `excavation_fx`, `focal_manipulator_ack`, `growth_lamp_fx`,
  `guardian_sonic`, `infusion_fx`, `knowledge_sync`, `open_thaumonomicon`,
  `research_notice`, `research_settings`, `research_table_state`, `runic_charge`,
  `runic_hit`, `shock_fx`, `vis_net_fx`, `warp_event`, `warp_message`, `wisp_zap`
* serverbound (12): `accessories_open`, `architect_toggle`, `focal_manipulator_start`,
  `focus_selection`, `golem_bell_reset`, `hover_toggle`, `research_note_request`,
  `research_purchase`, `research_table_combine`, `research_table_copy`,
  `research_table_edit`, `shovel_orientation`

The result is in the report's `evidence.client_clientbound_types` /
`evidence.client_serverbound_types`. **All 33 are present.** The registration is
correct: the common layer registers every C2S receiver through
`NetworkManager.registerReceiver(Side.C2S, …)`
(`common/src/main/java/thaumcraft/common/lib/network/FocusSelectionNetworking.java:29`,
`ResearchPurchaseNetworking.java:22`, `ResearchNoteRequestNetworking.java:21`,
`GolemBellNetworking.java:15`, `AccessoryNetworking.java:23`,
`ResearchTableEditNetworking.java:32`), and every S2C type through
`registerS2CPayloadType` on the server
(`KnowledgeEvents.java:22-23`, `ResearchTableEditNetworking.java:38`,
`AccessoryNetworking.java:22`) plus a client-side receiver registration, which is what
puts the type into the client's `PLAY_S2C` registry.

### 4.2 The clientbound knowledge snapshot is authoritative and agrees with the server

The client asks the server for its own aspect list with the port's own command
(`/thaumcraft aspects`, `ThaumcraftCommands.java:45-48`) and compares the answer with
its own `KnowledgeAccess` snapshot:

```
server : {aer=19, terra=15, ignis=18, aqua=19, ordo=15, perditio=17}
client : [aer, aqua, ignis, ordo, perditio, terra]
aspect_crosscheck_equal = true
```

The snapshot is applied by `KnowledgeEvents.initClient()`
(`common/src/main/java/thaumcraft/common/lib/network/KnowledgeEvents.java:42-48`) on the
**client thread** (`context.queue(...)` → `Minecraft.execute`), so no payload handler
touches the world from the netty thread.

### 4.3 The research table works over the network, with the original two-table rule

The acceptance found the original rule by accident and had to honour it: a research
table is a **pair**. A single `thaumcraft:table[variant=2]` block is a research half
without its partner, and the port's own topology tick collapses it back to a plain
table (`TileResearchTable` / `TableTopology.isMain`), which is the original TC4
behaviour (`BlockTable:187` scans the adjacent halves).

The acceptance therefore uses the original recipe:

1. two plain tables are placed next to each other,
2. the **inkwell** converts the clicked one into the main half
   (`BlockTable.useWithoutItem` → `ItemInkwell.tryCreateResearchTable`,
   `common/src/main/java/thaumcraft/common/blocks/BlockTable.java:109-120`),
3. the client observes the **original directional variants**: main `2` (north) and the
   partner `7` — `research_table_variant = 2`, `research_table_partner_variant = 7`,
4. the menu opens over the network and the authoritative
   `thaumcraft:research_table_state` payload arrives,
5. a real edit is sent over `thaumcraft:research_table_edit` and the server places the
   aspect: `edit_result_aspect = terra` in the note cell `(0,0)`.

### 4.4 The accessory menu opens from a real server packet

`AccessoryNetworking.open`
(`common/src/main/java/thaumcraft/common/lib/network/AccessoryNetworking.java:30-37`)
uses `MenuRegistry.openExtendedMenu`, and the client really receives the vanilla
`fabric-screen-handler-api-v1:open_screen` payload and opens `GuiAccessories`:
`accessory_menu_opened = true`, `accessory_screen_opened = true`.

### 4.5 The entity tracking works

The golem placed with the original item path and the travelling trunk both appear in
the **client's** world: `client_nearby_entities` contains `thaumcraft:golem` and the
trunk stage counted one `EntityTravelingTrunk` on the client.

## 5. Findings

### 5.1 The vis relay removes itself when it has no support — original behaviour, not a network defect

`TileVisRelay.dropIfUnsupported()`
(`common/src/main/java/thaumcraft/common/tiles/TileVisRelay.java:191-202`) removes the
relay when the block on its back side is air, and
`BlockVisRelay.canSurvive` (`BlockVisRelay.java:58-61`) encodes the same rule. The
acceptance places the relay with the vanilla `/setblock`, which does **not** run
`getStateForPlacement`, so the support side is the air above the relay and the port
removes it again. The client therefore sees air.

**This is the port's own rule, not a transport defect**, and the acceptance reports the
server's own view (`/data get block`) next to the client's view so the two can be
compared. See §6 for the final numbers.

### 5.2 The arcane bore needs the placement position that the original uses

The arcane bore is placed **on top of** the clicked block; the acceptance initially read
the clicked position itself and saw the grass. The harness was corrected
(`MultiplayerClientProbe.stageArcaneBore`, the `.above()` on the clicked position).

### 5.3 No crash, no exception, on either side

The accepted run's client and server logs contain **zero** `Exception` occurrences and
no payload decode failure, no "Bad packet" disconnect and no
`ConcurrentModificationException`. The only `ERROR` lines are the 55 vanilla
`No data fixer registered for thaumcraft:<entity>` notices on each side (the port
registers no data fixers for its own entities), one vanilla `Error parsing option value
4 for option Simulation Distance` (the acceptance writes `simulation-distance:4`, the
vanilla minimum is 5) and one vanilla `No key layers in MapLike[{}]` datapack notice.

### 5.4 The vanilla `give` delivery is not reliable enough for an acceptance

The vanilla `give` command is a **signed** command in 1.21.1 (its `<targets>`/`<item>`
arguments are signable). On an offline-mode server the signed command path validates the
client's `LastSeenMessages` update against the server's chat chain, and a command that
fails that validation is **dropped silently** — the server never answers it.

The acceptance therefore never trusts a `give` without an answer: `giveItems` sends the
command and **retries until the server's "Gave ..." reply arrives**
(`MultiplayerClientProbe.giveItems`, `giveReplies`). The same applies to the three
"place a TC4 item in the world" stages: when the given item does not reach the client,
the stage asks the server to place the object directly (`/summon`, `/setblock`) and
records which path delivered it (`golem_delivered_by = placer` in the accepted run).

**This is a vanilla 1.21.1 behaviour of the offline-mode secure chat path, not a port
defect** — the port's own payload layer is never involved in it.

## 6. The final run

`fabric/run-multiplayer/multiplayer-client-report.json`, produced by
`./gradlew :fabric:runMultiplayerClient -PmultiplayerAddress=127.0.0.1:25565 -PmultiplayerPlayer=MPProbe`
against a dedicated server started by `./gradlew :fabric:runMultiplayerServer`:

```
STATUS SCRIPT_PASSED
   PASS connect
   PASS teleport_to_work_area
   PASS payload_type_registries
   PASS reset_research
   PASS knowledge_sync
   PASS server_aspect_crosscheck
   PASS research_note_request
   PASS server_research_grant
   PASS research_purchase
   PASS focus_selection
   PASS golem_bell
   PASS accessory_menu_over_network
   PASS research_table_menu_over_network
   PASS research_table_edit_over_network
   PASS golem_placement_over_network
   PASS traveling_trunk_over_network
   PASS visnet_block_over_network
   PASS arcane_bore_over_network
   PASS disconnect
   PASS reconnect
   PASS reconnect_state_consistent
```

### 6.1 The payload census of the accepted run

**Clientbound (server → client), 8 distinct Thaumcraft channels crossed the socket:**

| payload | count | what it carried |
|---|---|---|
| `thaumcraft:knowledge_sync` | 9 | the authoritative knowledge snapshot (join, grant, purchase, reconnect) |
| `thaumcraft:aspect_pool` | 4 | the research point receipts of the purchase and of the granted aspects |
| `thaumcraft:research_table_state` | 4 | the research table's generation/revision/bonus state |
| `thaumcraft:runic_charge` | 4 | the runic charge HUD |
| `fabric-screen-handler-api-v1:open_screen` | 2 | the accessory and research table menus |
| `thaumcraft:research_notice` | 1 | the purchased research notice |
| `thaumcraft:accessories_contents` | 1 | the four worn accessory stacks |
| `thaumcraft:research_settings` | 1 | the server's research configuration |

**Serverbound (client → server), 6 distinct Thaumcraft channels crossed the socket:**

| payload | count | what it carried |
|---|---|---|
| `thaumcraft:research_note_request` | 1 | the primary note issuance intent |
| `thaumcraft:research_purchase` | 1 | the secondary purchase intent |
| `thaumcraft:focus_selection` | 1 | the wand focus intent |
| `thaumcraft:golem_bell_reset` | 1 | the golem bell reset intent |
| `thaumcraft:accessories_open` | 1 | the accessory menu intent |
| `thaumcraft:research_table_edit` | 1 | the table cell placement intent |

(`minecraft:register` × 2 is the vanilla configuration-phase registry handshake,
counted once per connection.)

### 6.2 The decisive evidence of the run

```
aspect_crosscheck_equal            = true      # the server's and the client's aspect lists are equal
server_aspects_raw                  = {aer=18, terra=19, ignis=17, aqua=19, ordo=15, perditio=19}
client_aspects                      = [aer, aqua, ignis, ordo, perditio, terra]
note_request_answer                 = "Research notes created: Nitor"
paper_before / paper_after          = 8 / 7     # the server really spent one paper
purchase_answer                     = "Research completed: Arcane Lamp"
research_notice_highlighted         = true      # the clientbound research notice was consumed
selected_focus                      = thaumcraft:focus_excavation
accessory_menu_opened               = true      # a real server menu packet opened the GUI
accessory_screen_opened             = true
research_table_variant              = 2         # the original north main half
research_table_partner_variant      = 7         # the original north secondary half
edit_cell                           = [0, 0]
edit_result_aspect                  = terra     # the server placed the aspect in the note cell
client_golems                       = 1         # the placed golem exists on the client
golem_delivered_by                  = placer    # delivered by the ORIGINAL item path
golem_placer_reached_client         = true
client_traveling_trunks             = 1
visnet_block                        = thaumcraft:vis_relay
visnet_block_entity                 = thaumcraft.common.tiles.TileVisRelay@1ee60f5b
bore_block                          = thaumcraft:arcane_bore
bore_base_block                     = thaumcraft:arcane_bore_base
reconnect_research                  = [ARCANELAMP, ARCANESTONE, ..., WARP]   # 24 keys, the purchase survived
reconnect_aspects                   = [aer, aqua, ignis, lux, ordo, perditio, sensus, tenebrae, terra]
```

### 6.3 The GameTest regression net

`:fabric:runGameTest` was executed three times on this host.

| run | result |
|---|---|
| 1 | killed by the OOM killer (exit 137) after 157 test batches — the host had 15 GiB RAM, the swap exhausted and the load average was 68–188 |
| 2 | killed by the OOM killer again (exit 137) after 157 batches |
| 3 (`JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=11`) | **318 test cases ran, 4 failed** |

The four failures are

```
thaumcraftgametests.magicalbiomesgeneratenaturallyandspawnactualpech
thaumcraftgametests.fluxoriginalexposureandgootail
thaumcraftgametests.fluxmovesandexhaustswithoriginalrules
thaumcraftgametests.fullinventoryreusesfreedpaperslotordrops
```

**None of them is in the network layer, and this agent changed no production code.**
The change of this agent is confined to the `clientTest` source set and to two new loom
run definitions; `common/build.gradle:139-174` (`verifyTestIsolation`) proves that the
GameTest run does not load the `clientTest` output, and `:fabric:verifyTestIsolation`
and `:fabric:verifyProductionIsolation` both ran green inside `:fabric:build`.

The four failures are the known unstable subset of the suite (the handoff already lists
"312 scenarios, 5 failures (the same unstable subset)"), amplified by the 10× oversubscribed
host: a worldgen/spawn test, two flux/tick tests and one inventory test.

**`:fabric:build` (unit tests, `verifyTestIsolation`, `verifyProductionIsolation`) is
green on this branch.**

## 7. What is still unverified

* **The overworld world generation is not covered** — the acceptance server uses a flat
  world so that a dedicated server stays responsive on an oversubscribed build host.
  The TC4 world generator is covered by the GameTest suite, not by this acceptance.
* **No second human player** — the acceptance is one physical client against the
  dedicated server. A second physical client (the `MPProbe2` operator entry exists in
  `ops.json`) was not run; every "second player" behaviour is therefore still
  *unverified over a real connection*.
* **The clientbound FX payloads** (`block_fx`, `shock_fx`, `excavation_fx`,
  `infusion_fx`, `growth_lamp_fx`, `guardian_sonic`, `runic_charge`, `runic_hit`,
  `wisp_zap`, `vis_net_fx`, `warp_event`, `warp_message`, `aspect_discovery`,
  `open_thaumonomicon`, `accessories_contents`, `focal_manipulator_ack`) were proven to
  be **registered and decodable** on the physical client, but only the ones the
  acceptance could trigger (knowledge snapshot, aspect pool, research notice, research
  table state, open screen) were observed **on the wire**.
* **The NeoForge loader** was not exercised at all; the acceptance is Fabric-only.
* **The server host was heavily oversubscribed** (load average 25–188 on 12 cores while
  the other wave-2 agents were building). The server therefore ran at 0.5–20 ticks per
  second, the vanilla watchdog had to be disabled (`max-tick-time=-1`), and several
  stages needed a two-minute timeout. A quiet host would make the acceptance
  considerably faster, but the observed results are real.

## 8. Production changes

**None.** The whole acceptance is test-only:

* `fabric/src/clientTest/java/thaumcraft/fabric/clienttest/MultiplayerClientProbe.java`
  (new, `clientTest` source set, never in a production archive),
* `fabric/src/clientTest/resources/fabric.mod.json` — one more client entry point,
* `fabric/build.gradle` — the `multiplayerClient` and `multiplayerServer` loom runs,
  the `prepareMultiplayerRun` task and the report check of `runMultiplayerClient`,
* `.gitignore` — the new run directories and the agent-local Gradle home.

The port's gameplay code is unchanged: the acceptance found no multiplayer defect in
the common layer.
