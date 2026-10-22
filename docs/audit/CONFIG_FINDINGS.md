# Configuration audit — g16config, 2026-10-23

## Scope and result

Hypothesis: configuration is ignored, read on the wrong side, or not synchronized.
Inspected **39 parsed file keys** (25 offered by the editor, 12 additional keys in
`thaumcraft.properties`, 2 in `thaumcraft-client.properties`) and **86 public
compatibility fields** of modern `Config` (85 mutable plus `auraSize`). These are
overlapping surfaces, not 125 independent gameplay switches. Category constants,
the `Defaults` aliases, and removed Forge material/configuration objects are not keys.

* **0 unparsed file keys**, **1 parsed but gameplay-dead key** (`generate_taint`,
  intentionally also dead in TC4). A parser reference is not proof of gameplay use.
* **0 confirmed wrong-side gameplay readers**. Server gameplay uses server settings;
  client gameplay decisions use the received research snapshot. Visual settings stay local.
* **0 public initializer mismatches**; **1 editor default mismatch**, fixed:
  `glowing_taint=false` became `true`. Closing the editor with that property absent
  previously persisted `false` even without the user toggling it.
* **Sync exists for all 7 original PacketConfig facts**, on player join, with queued
  client application. **Restoration bug fixed for all 7**: initialization/disconnect
  now uses the loaded local configuration rather than hard-coded defaults.
* **85/86 public fields have no direct production reader** (only `showTags` is read).
  Do not report these as 85 dead file options: most native consumers use immutable
  settings instead. Compatibility-only fields and unsupported options are distinguished below.

## Evidence and tests

Oracle read-only root: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/`.
`Config.java:369-516` supplies file defaults; `:830-916` supplies field initializers.
`PacketConfig.java:30-59` sends/applies seven values. Unlike some existing port comments,
the oracle writes **unprefixed** fields on receipt; the `C` fields preserve local values.
`EventHandlerNetwork.java:69-78` restores those local values at logout.

Tests:

* **A**: `ConfigDeclarationAuditTest.everyEditableKeyHasAnExplicitRuntimeParserNotJustAnEditor`
  checks all 39 known keys for reader literals and all 25 editor keys for uniqueness/readers.
  This is deliberately a source guard, not a claim that each parser has a gameplay effect.
* **D**: `ConfigDeclarationAuditTest.editorDefaultsMatchTheOracleAndDoNotSilentlyDisableGlowingTaint`
  compares every editor default with independent oracle values and exercises actual file write/read.
* **P**: `ConfigDeclarationAuditTest.allRuntimeParserDefaultsMatchTheOracle` compares the
  default parsed research/biome/spawn/worldgen/regen records and taint/runic constants.
* **F**: existing `ConfigDefaultsTest` pins the public defaults (mutable values can be
  changed by other tests; immutable `Defaults` is used for the affected options).
* **S**: `ResearchSettingsTest.remoteServerSettingsStayOnTheClientThreadAndResetWithoutChangingIntegratedServerFacts`
  gives every local fact a non-default value and every remote fact a different value,
  verifies receipt, disconnect restoration, and unchanged server-thread authority.
* **W**: existing `ResearchSettingsPayloadTest` tests codec round trips and invalid input.

An independent source inspection resolved all `Config.Defaults.*` references and compared
all 86 initializers against the oracle static block: zero mismatches (generic `ArrayList<>`
and raw `ArrayList` represent the same empty default). No private code was copied into Git.

## PROVEN: file-key ledger (39)

Paths below are relative to `common/src/main/java/thaumcraft/`, except `FabricBiomeSpawns`
which is in `fabric/src/main/java/thaumcraft/fabric/world/`. “Local” means deliberately
not synced. “Server” means no client needs that rule to make an authoritative decision.
Defaults are port / oracle **file** defaults, not necessarily field initializers.
All rows have parser coverage A; test letters name additional coverage.

| Key | Runtime reader / side | Default port / oracle | Synced? | Test; fix |
| --- | --- | --- | --- | --- |
| `allow_mirrors` | ResearchSettings → ResearchConditions / server + client | true / true | Yes | P,S,W; restore local snapshot |
| `hard_mode_nodes` | ResearchSettings → TileNode, NodeEcology / server; ThaumcraftHungryNodeClient / client | true / true | Yes | P,S,W; restore local snapshot |
| `wuss_mode` | ResearchSettings → WarpEvents / server, ResearchCheatSheet | false / false | Yes | P,S,W; restore local snapshot |
| `allow_cheat_sheet` | ResearchSettings → ItemThaumonomicon / server; ThaumcraftContent creative tab / client | false / false | Yes | D,P,S,W; restore local snapshot |
| `allow_warded_stone` | ResearchSettings → WandWardedStone, BlockArcaneDoor, BlockCosmeticOpaque, BlockArcanePressurePlate / both | true / true | Yes | P,S,W; restore local snapshot |
| `research_difficulty` | ResearchSettings → ResearchPurchase, ResearchNoteIssuance / server; GuiResearchBrowser / client | 0 / 0 | Yes | P,S,W; restore local snapshot |
| `aspect_total_cap` | ResearchSettings → ScanManager / server | 100 / 100 | Yes (original protocol) | P,S,W; restore local snapshot |
| `shaders` | ThaumcraftSettings → client warp post chains | true / true | Local | D; no default change |
| `blue_magical_forest` | BiomeSettings → mixin/client/Tc4BiomeColorMixin / client | false / false | Local | D,P |
| `glowing_taint` | ThaumcraftTaintSettings → client/renderers/block/TaintFibreWorldQuads / client | true / true; editor was false | Local | D,P; editor default fixed |
| `biome_taint_spread` | ThaumcraftTaintSettings → BlockTaintFibres.randomTick / server | 200 / 200 | Server | D,P |
| `spawn_taint_spores` | ThaumcraftTaintSettings → BlockTaint, BlockTaintFibres / server | true / true | Server | D,P |
| `spawn_angry_zombies` | OriginalSpawnSettings → FabricBiomeSpawns / server spawn registration | true / true | Server | D,P |
| `spawn_fire_bats` | OriginalSpawnSettings → FabricBiomeSpawns / server spawn registration | true / true | Server | D,P |
| `spawn_wisps` | OriginalSpawnSettings → FabricBiomeSpawns / server spawn registration | true / true | Server | D,P |
| `spawn_taintacles` | OriginalSpawnSettings → FabricBiomeSpawns / server spawn registration | true / true | Server | D,P |
| `spawn_pechs` | OriginalSpawnSettings → FabricBiomeSpawns / server spawn registration | true / true | Server | D,P |
| `spawn_eldercreatures` | OriginalSpawnSettings → FabricBiomeSpawns / server spawn registration | true / true | Server | D,P |
| `generate_aura_nodes` | OriginalWorldgenSettings → ThaumcraftNormalNodeFeature, ThaumcraftTotemFeature, ChunkRegeneration / server | true / true | Server | D,P |
| `generate_structures` | OriginalWorldgenSettings → ThaumcraftTotemFeature, ChunkRegeneration / server | true / true | Server | D,P |
| `generate_cinnibar_ore` | OriginalWorldgenSettings → ThaumcraftOreFeature, ChunkRegeneration / server | true / true | Server | D,P |
| `generate_amber_ore` | OriginalWorldgenSettings → ThaumcraftOreFeature, ChunkRegeneration / server | true / true | Server | D,P |
| `generate_infused_stone` | OriginalWorldgenSettings → ThaumcraftOreFeature, ChunkRegeneration / server | true / true | Server | D,P |
| `generate_trees` | BiomeSettings → ThaumcraftTreeFeature, ChunkRegeneration / server | true / true | Server | D,P |
| `generate_taint` | BiomeSettings parser only; **no gameplay reader**, as in oracle | true / true | No | D,P; original inert behavior retained |
| `magical_forest_biome_weight` | BiomeSettings → mixin/Tc4MultiNoiseBiomeSourceMixin / server worldgen | 5 / 5 | Server | P |
| `taint_biome_weight` | BiomeSettings → mixin/Tc4MultiNoiseBiomeSourceMixin / server worldgen | 2 / 2 | Server | P |
| `regen_key` | OriginalRegenSettings → ChunkRegeneration / server | DEFAULT / DEFAULT | Server | P |
| `aura_nodes` | OriginalRegenSettings → ChunkRegeneration / server | false / false | Server | P |
| `structures` | OriginalRegenSettings → ChunkRegeneration / server | false / false | Server | P |
| `cinnibar_ore` | OriginalRegenSettings → ChunkRegeneration / server | false / false | Server | P |
| `amber_ore` | OriginalRegenSettings → ChunkRegeneration / server | false / false | Server | P |
| `infused_stone` | OriginalRegenSettings → ChunkRegeneration / server | false / false | Server | P |
| `trees` | OriginalRegenSettings → ChunkRegeneration / server | false / false | Server | P |
| `runic_recharge_speed` | RunicSettings → RunicShield / server | 2000 / 2000 | Server | P; existing RunicSettingsTest |
| `runic_recharge_delay` | RunicSettings → RunicShield / server | 80 / 80 | Server | P; existing RunicSettingsTest |
| `runic_cost` | RunicSettings → RunicShield / server | 50 / 50 | Server | P; existing RunicSettingsTest |
| `notification_delay` | client/lib/PlayerNotifications → NotificationQueue / client | 5000 / 5000 | Local, separate client file | A,F; source verified |
| `notification_max` | client/lib/PlayerNotifications HUD / client | 15 / 15 | Local, separate client file | A,F; source verified |

## PROVEN: public compatibility-field ledger (86)

All rows: initializer default **matches oracle**, verified by source comparison and F.
“Native” means there is a file-key-backed replacement in the table above; that field itself
has no direct reader. Only the seven research facts are synced, through ResearchSettings,
**not through mutable compatibility fields**. `C*` fields are retained ABI/local copies,
not independent server rules. “Compat” means no file-backed native equivalent in this audit;
do not silently claim add-ons can change the native behavior by assigning it.
No compatibility fields were deleted or renamed. No speculative compatibility fixes made.

| Field | Port / oracle initializer | Direct reader / native replacement |
| --- | --- | --- |
| `auraSize` | 4.0f / 4.0f | Compat constant |
| `biomeTaintID` | 192 / 192 | Compat numeric ID; registry biomes replace IDs |
| `biomeMagicalForestID` | 193 / 193 | Compat numeric ID; registry biomes replace IDs |
| `biomeEerieID` | 194 / 194 | Compat numeric ID; registry biomes replace IDs |
| `biomeEldritchID` | 195 / 195 | Compat numeric ID; registry biomes replace IDs |
| `biomeTaintWeight` | 2 / 2 | Native taint_biome_weight |
| `biomeMagicalForestWeight` | 5 / 5 | Native magical_forest_biome_weight |
| `taintSpreadRate` | 200 / 200 | Native biome_taint_spread |
| `taintFromFlux` | true / true | Compat |
| `hardNode` | true / true | Native hard_mode_nodes; synced snapshot |
| `wuss` | false / false | Native wuss_mode; synced snapshot |
| `dimensionOuterId` | -42 / -42 | Compat numeric ID; registry dimension replaces ID |
| `championMobs` | true / true | Compat |
| `shieldRecharge` | 2000 / 2000 | Native runic_recharge_speed |
| `shieldWait` | 80 / 80 | Native runic_recharge_delay |
| `shieldCost` | 50 / 50 | Native runic_cost |
| `colorBlind` | false / false | Compat |
| `shaders` | true / true | Native shaders; assigned, not read directly |
| `crooked` | true / true | Compat |
| `showTags` | false / false | **HoveredSlotAspectRow reads directly, client**; no file loader |
| `blueBiome` | false / false | Native blue_magical_forest |
| `allowMirrors` | true / true | Native allow_mirrors; synced snapshot |
| `dialBottom` | false / false | Compat |
| `nodeRefresh` | 10 / 10 | Compat |
| `genAura` | true / true | Native generate_aura_nodes |
| `genStructure` | true / true | Native generate_structures |
| `genCinnibar` | true / true | Native generate_cinnibar_ore |
| `genAmber` | true / true | Native generate_amber_ore |
| `genInfusedStone` | true / true | Native generate_infused_stone |
| `genTrees` | true / true | Native generate_trees |
| `genTaint` | true / true | Native generate_taint; originally inert |
| `regenAura` | false / false | Native aura_nodes |
| `regenStructure` | false / false | Native structures |
| `regenCinnibar` | false / false | Native cinnibar_ore |
| `regenAmber` | false / false | Native amber_ore |
| `regenInfusedStone` | false / false | Native infused_stone |
| `regenTrees` | false / false | Native trees |
| `regenTaint` | false / false | Compat; not among original six queue-enabling flags |
| `regenKey` | DEFAULT / DEFAULT | Native regen_key |
| `wardedStone` | true / true | Native allow_warded_stone; synced snapshot |
| `allowCheatSheet` | true / true | Native allow_cheat_sheet (file default false); synced snapshot |
| `golemChestInteract` | true / true | Compat; GolemChestInteraction does not read the field |
| `nodeRarity` | 36 / 36 | Compat; generation embeds default rarity |
| `specialNodeRarity` | 18 / 18 | Compat |
| `notificationDelay` | 5000 / 5000 | Native notification_delay (client file) |
| `notificationMax` | 15 / 15 | Native notification_max (client file) |
| `glowyTaint` | true / true | Native glowing_taint |
| `researchDifficulty` | 0 / 0 | Native research_difficulty; synced snapshot |
| `aspectTotalCap` | 100 / 100 | Native aspect_total_cap; synced snapshot |
| `golemDelay` | 5 / 5 | Compat |
| `golemIgnoreDelay` | 10000 / 10000 | Compat |
| `golemLinkQuality` | 16 / 16 | Compat |
| `CwardedStone` | true / true | Compat/local copy; native local restoration fixed |
| `CallowCheatSheet` | true / true | Compat/local copy; native local restoration fixed |
| `CallowMirrors` | true / true | Compat/local copy; native local restoration fixed |
| `ChardNode` | true / true | Compat/local copy; native local restoration fixed |
| `Cwuss` | false / false | Compat/local copy; native local restoration fixed |
| `CresearchDifficulty` | 0 / 0 | Compat/local copy; native local restoration fixed |
| `CaspectTotalCap` | 100 / 100 | Compat/local copy; native local restoration fixed |
| `spawnAngryZombie` | true / true | Native spawn_angry_zombies |
| `spawnFireBat` | true / true | Native spawn_fire_bats |
| `spawnTaintacle` | true / true | Native spawn_taintacles |
| `spawnWisp` | true / true | Native spawn_wisps |
| `spawnTaintSpore` | true / true | Native spawn_taint_spores |
| `spawnPech` | true / true | Native spawn_pechs |
| `spawnElder` | true / true | Native spawn_eldercreatures |
| `potionVisExhaustID` | 18 / 18 | Compat obsolete numeric potion ID |
| `potionInfVisExhaustID` | 18 / 18 | Compat obsolete numeric potion ID |
| `potionBlurredID` | 18 / 18 | Compat obsolete numeric potion ID |
| `potionThaumarhiaID` | 18 / 18 | Compat obsolete numeric potion ID |
| `potionTaintPoisonID` | 19 / 19 | Compat obsolete numeric potion ID |
| `potionUnHungerID` | 17 / 17 | Compat obsolete numeric potion ID |
| `potionSunScornedID` | 17 / 17 | Compat obsolete numeric potion ID |
| `potionWarpWardID` | 23 / 23 | Compat obsolete numeric potion ID |
| `potionDeathGazeID` | 17 / 17 | Compat obsolete numeric potion ID |
| `enchHaste` | null / null | Compat obsolete enchantment object |
| `enchRepair` | null / null | Compat obsolete enchantment object |
| `aspectOrder` | empty ArrayList / empty ArrayList | Compat bookkeeping |
| `foundCopperIngot` | false / false | Compat bookkeeping |
| `foundTinIngot` | false / false | Compat bookkeeping; strings in coverage explanations are not readers |
| `foundSilverIngot` | false / false | Compat bookkeeping; strings are not readers |
| `foundLeadIngot` | false / false | Compat bookkeeping; strings are not readers |
| `foundCopperOre` | false / false | Compat bookkeeping |
| `foundTinOre` | false / false | Compat bookkeeping |
| `foundSilverOre` | false / false | Compat bookkeeping |
| `foundLeadOre` | false / false | Compat bookkeeping |

## PROVEN: network and lifecycle

`ResearchSettingsNetworking.initServer:17` registers PLAYER_JOIN → sendToPlayer with
`ThaumcraftSettings.server()`. `initClient:22-23` registers S2C and queues `receive`.
`ResearchSettingsPayload` carries five boolean bits, difficulty and cap. `receive`
requires the initialized client thread and changes only `remote`; `current()` selects
that view on the client thread and the server view elsewhere. `initClient:29` registers
disconnect → resetClient. Both bootstrap registrations exist in the mod entry points.
This proves wiring, not that a human multiplayer session has been observed.

The old reset assigned `ResearchSettings.DEFAULTS`, losing a non-default local config.
The fix initializes/restores `remote = server`, where `server` is this process's locally
loaded snapshot. It never writes the authoritative server record during remote receipt.
Do not call `Config.apply(remote)` as a shortcut: that would overwrite shared statics in
an integrated server. Compatibility statics remain a separately listed limitation.

## PROVEN limitations / not fixed in this bounded lane

* Original `Thaumcraft.onConfigChanged:293-298` calls `Config.syncConfigurable` during
  the session. The native editor only writes the file on close and advertises restart;
  loaders publish startup snapshots. **Live editor reconfiguration parity is missing.**
  The two fixes here do not claim to implement live reload or broadcast mid-session changes.
* Public compatibility fields mostly do not drive native gameplay. In particular
  `golemChestInteract`, `nodeRarity`, and `showTags` have no equivalent parsed file key;
  documenting the compatibility default alone does not make these settings implemented.
  Connecting all original options needs feature-specific behavior tests, not invented gates.

## Reasoned-only / not multiplayer-observed

* Add-ons reading mutable `Config` fields on a remote client may see local values instead
  of the received snapshot. Native consumers use the safe record path; no add-on reproduction
  was run, and blindly mutating global fields would damage integrated-server isolation.
* Cross-server reconnect, actual dedicated-client rendering, login ordering relative to
  creative-tab cache rebuild, and visual changes after editor reload need live acceptance.
* Broader unsupported original settings (champion toggle, flux-to-taint, golem timing,
  colors, node rarity, etc.) are visible in the public ledger, but their gameplay impact
  and correct modern persistence strategy were not behaviorally reproduced in this lane.

## Execution evidence

Validation commands/results are recorded in `docs/analysis/2026-10-23-agent-g16config.md`.
Final-source validation passed: 15/15 selected JUnit tests, actual Fabric compilation,
Fabric test-root isolation, and 1/1 focused dedicated-server cheat-sheet/Wuss-mode GameTest.
Do not equate source inspection or JUnit success with multiplayer visual acceptance.
