# Static-state inventory

Paths are relative to `common/src/main/java/thaumcraft/common/lib/`. Each row is one field, including comma-declared fields. Immutable screening rows are retained to make exclusions auditable.

| Field site | Field | Classification |
|---|---|---|
| `AdvancedFurnaceFormation.java:32` | `MUTATING` | ThreadLocal scope; keep remove/finally discipline |
| `AdvancedFurnaceFormation.java:33` | `BIRTH` | ThreadLocal scope; keep remove/finally discipline |
| `AdvancedFurnaceRules.java:8` | `FORM_VIS` | Immutable table (excluded from mutable count) |
| `ArcaneEarNotes.java:14` | `NOTES` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `ArcaneFurnaceFormation.java:73` | `FORM_VIS` | Immutable table (excluded from mutable count) |
| `GrowthLampTicks.java:28` | `SCHEDULING` | ThreadLocal scope; keep remove/finally discipline |
| `MagicWard.java:31` | `ARMOR` | Private lookup table; no writes found after initialization |
| `ThaumatoriumRules.java:11` | `FORM_VIS` | Immutable table (excluded from mutable count) |
| `WarpEvents.java:35` | `wussMode` | S5: process-global configuration; lifecycle not encoded in ownership |
| `baubles/PlayerAccessories.java:20` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `baubles/PlayerAccessories.java:21` | `CHARGE` | ThreadLocal scope; keep remove/finally discipline |
| `baubles/PlayerAccessories.java:22` | `WORN` | ThreadLocal scope; keep remove/finally discipline |
| `baubles/PlayerAccessoryInventory.java:15` | `TYPES` | Private lookup table; no writes found after initialization |
| `baubles/RunicBaubleRules.java:33` | `PRIMALS` | Immutable table (excluded from mutable count) |
| `baubles/RunicBaubleRules.java:53` | `RUNIC` | Immutable table (excluded from mutable count) |
| `baubles/RunicBaubleRules.java:102` | `BLANKS` | Immutable table (excluded from mutable count) |
| `baubles/RunicBaubleRules.java:152` | `CULTIST` | Immutable table (excluded from mutable count) |
| `baubles/RunicBaubleRules.java:184` | `RODS` | Immutable table (excluded from mutable count) |
| `baubles/RunicBaubleRules.java:206` | `CAPS` | Immutable table (excluded from mutable count) |
| `baubles/RunicBaubleRules.java:308` | `SHEEP_COLORS` | Private lookup table; no writes found after initialization |
| `bore/BoreNativeContext.java:33` | `REVOKED_LEVELS` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `bore/BoreNativeContext.java:34` | `REVOKED_SERVERS` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `bore/BoreNativeContext.java:35` | `REVOKED_OWNERS` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `bore/BoreNativeContext.java:36` | `DISPATCH` | ThreadLocal scope; keep remove/finally discipline |
| `bore/BoreNativeContext.java:37` | `OPERATION` | ThreadLocal scope; keep remove/finally discipline |
| `bore/BoreNativeContext.java:38` | `OBSERVATION` | ThreadLocal scope; keep remove/finally discipline |
| `bore/BorePlatform.java:16` | `bridge` | Bootstrap/configuration callback; installation-thread/lifetime contract needed (S7) |
| `bore/BoreRepairState.java:10` | `PRIMALS` | Immutable table (excluded from mutable count) |
| `bore/BoreWork.java:56` | `ORDINARY` | Immutable table (excluded from mutable count) |
| `bore/BoreWork.java:60` | `DRAW_ORDER` | Immutable table (excluded from mutable count) |
| `crafting/ArcaneSceptreRecipe.java:50` | `FORBIDDEN` | Private lookup table; no writes found after initialization |
| `crafting/SmeltingBonusCatalog.java:49` | `entries` | Registry/corpus publication state; do not treat as world cache |
| `crafting/SmeltingBonusCatalog.java:50` | `skipped` | Registry/corpus publication state; do not treat as world cache |
| `crafting/SmeltingBonusCatalog.java:51` | `loaded` | Bootstrap/configuration callback; installation-thread/lifetime contract needed (S7) |
| `crystals/CrystalClusterGeometry.java:10` | `COLORS` | Private lookup table; no writes found after initialization |
| `cult/CultistRules.java:172` | `LEADER_TITLES` | Public mutable rule data: S4 |
| `cult/CultistRules.java:272` | `CHAMPION_MOD_NAMES` | Public mutable rule data: S4 |
| `cult/CultistRules.java:277` | `CHAMPION_MOD_DISPLAY_NAMES` | Public mutable rule data: S4 |
| `essentia/EssentiaTransfers.java:45` | `MUTATION` | ThreadLocal scope; keep remove/finally discipline |
| `essentia/ThaumatoriumOutput.java:25` | `RAW` | ThreadLocal scope; keep remove/finally discipline |
| `essentia/TubeGeometry.java:25` | `ARMS` | Private lookup table; no writes found after initialization |
| `events/ChunkRegeneration.java:74` | `QUEUE` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `events/EssentiaHandler.java:25` | `LEVELS` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `events/ThaumcraftCommands.java:27` | `NORMALIZING` | ThreadLocal scope; keep remove/finally discipline |
| `fluid/GolemFluidAccess.java:40` | `PROVIDERS` | Registry; public mutation/freeze phase is not enforced (S6) |
| `fluid/GolemFluidAccess.java:41` | `CONTAINER_LOOKUPS` | Registry; public mutation/freeze phase is not enforced (S6) |
| `fluid/PureBucketActions.java:32` | `USING` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `fluid/PureBucketActions.java:33` | `CELLS` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `fluid/PureFlowRules.java:8` | `DX` | Private lookup table; no writes found after initialization |
| `fluid/PureFlowRules.java:8` | `DZ` | Private lookup table; no writes found after initialization |
| `fx/SparkleCallRules.java:31` | `clientEffects` | Bootstrap/configuration callback; installation-thread/lifetime contract needed (S7) |
| `golems/GolemLadderRules.java:88` | `LADDER` | Immutable table (excluded from mutable count) |
| `infusion/InfusionStructure.java:42` | `CORNERS` | Private lookup table; no writes found after initialization |
| `items/EquipmentRules.java:219` | `PRIMAL_ARROW_ASPECTS` | Immutable table (excluded from mutable count) |
| `items/EquipmentRules.java:387` | `SHARD_ASPECTS` | Immutable table (excluded from mutable count) |
| `loot/LootPotionVariants.java:11` | `VARIANTS` | Immutable table (excluded from mutable count) |
| `loot/LootTables.java:28` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `loot/OriginalLootEnchantments.java:33` | `RULES` | Immutable table (excluded from mutable count) |
| `loot/OriginalTaintExperience.java:26` | `ORIGINAL_SLOTS` | Private lookup table; no writes found after initialization |
| `loot/OriginalTaintExperience.java:27` | `EQUIPMENT` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `loot/OriginalTaintExperience.java:28` | `ACTIVE` | ThreadLocal scope; keep remove/finally discipline |
| `loot/ZombieBrainDrops.java:19` | `TYPES` | Immutable table (excluded from mutable count) |
| `mods/ChampionData.java:22` | `CHAMPION` | Registry; public mutation/freeze phase is not enforced (S6) |
| `mods/ChampionHooks.java:80` | `championsEnabled` | Bootstrap/configuration callback; installation-thread/lifetime contract needed (S7) |
| `mods/ChampionHooks.java:81` | `dangerousLocation` | Bootstrap/configuration callback; installation-thread/lifetime contract needed (S7) |
| `mods/ChampionHooks.java:82` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `mods/ChampionModifier.java:64` | `mods` | Public mutable rule data: S4 |
| `mods/ChampionRules.java:74` | `CHAMPION_WHITELIST` | Immutable table (excluded from mutable count) |
| `mods/ChampionRules.java:111` | `CHAMPION_CLASS_WHITELIST` | Immutable table (excluded from mutable count) |
| `mods/ChampionRules.java:429` | `renderer` | Bootstrap/configuration callback; installation-thread/lifetime contract needed (S7) |
| `network/AccessoryNetworking.java:17` | `LAST` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `network/AccessoryNetworking.java:18` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/AccessoryNetworking.java:18` | `client` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/BlockFxNetworking.java:28` | `serverInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/BlockFxNetworking.java:28` | `clientInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/BlockSparkleFxNetworking.java:30` | `serverInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/BlockSparkleFxNetworking.java:30` | `clientInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/EssentiaFlowFxNetworking.java:26` | `serverInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/EssentiaFlowFxNetworking.java:26` | `clientInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/ExcavationFxNetworking.java:14` | `serverInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/ExcavationFxNetworking.java:14` | `clientInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/FocalManipulatorNetworking.java:14` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/FocalManipulatorNetworking.java:15` | `clientAckReceiver` | Bootstrap/configuration callback; installation-thread/lifetime contract needed (S7) |
| `network/FocusSelectionNetworking.java:23` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/GolemBellNetworking.java:10` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/GrowthLampFxNetworking.java:13` | `serverInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/GrowthLampFxNetworking.java:13` | `clientInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/HoverNetworking.java:8` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/InfusionFxNetworking.java:15` | `serverInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/InfusionFxNetworking.java:15` | `clientInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/KnowledgeEvents.java:13` | `serverInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/KnowledgeEvents.java:14` | `clientInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/KnowledgeNotificationNetworking.java:17` | `server` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/KnowledgeNotificationNetworking.java:17` | `client` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/ResearchNoteRequestNetworking.java:15` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/ResearchPurchaseNetworking.java:16` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/ResearchSettingsNetworking.java:12` | `server` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/ResearchSettingsNetworking.java:12` | `client` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/ResearchTableCombineNetworking.java:12` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/ResearchTableCopyNetworking.java:17` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/ResearchTableEditNetworking.java:26` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/RunicNetworking.java:10` | `server` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/RunicNetworking.java:10` | `client` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/ScanRuneFxNetworking.java:21` | `serverInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/ScanRuneFxNetworking.java:21` | `clientInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/ShockFxNetworking.java:15` | `serverInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/ShockFxNetworking.java:15` | `clientInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/ShovelOrientationNetworking.java:12` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/VisNetFxNetworking.java:21` | `serverInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/VisNetFxNetworking.java:21` | `clientInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/VisNetFxNetworking.java:22` | `EFFECTS` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `network/WandArchitectNetworking.java:14` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/WarpEventNetworking.java:9` | `server` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/WarpEventNetworking.java:9` | `client` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/WarpMessageNetworking.java:16` | `server` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/WarpMessageNetworking.java:16` | `client` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/WispZapNetworking.java:12` | `server` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `network/WispZapNetworking.java:12` | `client` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `research/EntityAspectBindings.java:27` | `NAMES` | Immutable table (excluded from mutable count) |
| `research/EntityAspectBindings.java:66` | `rows` | Registry/corpus publication state; do not treat as world cache |
| `research/ObjectAspectCatalog.java:31` | `MATERIAL_IDS` | Immutable table (excluded from mutable count) |
| `research/ObjectAspectCatalog.java:36` | `SHARD_IDS` | Immutable table (excluded from mutable count) |
| `research/ObjectAspectCatalog.java:39` | `ORE_IDS` | Immutable table (excluded from mutable count) |
| `research/ObjectAspectCatalog.java:46` | `LEGACY_ONLY_FALLBACK_IDS` | Immutable table (excluded from mutable count) |
| `research/ObjectAspectCatalog.java:47` | `bindings` | Registry/corpus publication state; do not treat as world cache |
| `research/ObjectAspectCatalog.java:48` | `candidateBindings` | Registry/corpus publication state; do not treat as world cache |
| `research/ObjectAspectCatalog.java:49` | `blockCandidateIds` | Registry/corpus publication state; do not treat as world cache |
| `research/ObjectAspectCatalog.java:50` | `expectedContentIds` | Registry/corpus publication state; do not treat as world cache |
| `research/ObjectAspectCatalog.java:51` | `conflictingIds` | Registry/corpus publication state; do not treat as world cache |
| `research/ObjectAspectCatalog.java:52` | `loaded` | Bootstrap/configuration callback; installation-thread/lifetime contract needed (S7) |
| `research/ObjectAspectCatalog.java:53` | `publicTagsInitialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `research/ObjectAspectRegistry.java:17` | `serverContext` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `research/ObjectAspectResolver.java:62` | `INDEXES` | S1: client/server recipe cache; synchronized, keyed by manager, still shared owner |
| `research/ObjectAspectResolver.java:63` | `ENCHANTMENT_TAGS` | Immutable table (excluded from mutable count) |
| `research/PlayerKnowledge.java:18` | `PRIMAL_TAGS` | Immutable table (excluded from mutable count) |
| `research/PrimalCharmRules.java:9` | `PRIMALS` | Private lookup table; no writes found after initialization |
| `research/ResearchBrowserData.java:16` | `CATEGORY_ORDER` | Immutable table (excluded from mutable count) |
| `research/ResearchCatalog.java:23` | `definitions` | Registry/corpus publication state; do not treat as world cache |
| `research/ResearchConditions.java:13` | `nativeReady` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `research/ResearchRecipeCoverage.java:63` | `NATIVE_RECIPE_IDS` | Immutable table (excluded from mutable count) |
| `research/ResearchRecipeCoverage.java:65` | `CONSTRUCT_BLUEPRINTS` | Immutable table (excluded from mutable count) |
| `research/ResearchRecipeCoverage.java:67` | `DEFERRED` | Immutable table (excluded from mutable count) |
| `research/ResearchRecipeCoverage.java:69` | `DOCUMENTED_DUPLICATES` | Immutable table (excluded from mutable count) |
| `research/ResearchRecipeCoverage.java:71` | `PAGE_RECIPE_ARRAYS` | Immutable table (excluded from mutable count) |
| `research/ResearchRecipeCoverage.java:73` | `OUTSIDE_BOOK` | Immutable table (excluded from mutable count) |
| `research/ResearchRecipeCoverage.java:75` | `NATIVE_RECIPE_FILES` | Immutable table (excluded from mutable count) |
| `research/ResearchRegistrationOrder.java:14` | `CATEGORIES` | Immutable table (excluded from mutable count) |
| `research/ResearchRegistrationOrder.java:15` | `registrationKeys` | Registry/corpus publication state; do not treat as world cache |
| `research/ResearchTriggerBindings.java:46` | `ITEMS` | Immutable table (excluded from mutable count) |
| `research/ResearchTriggerBindings.java:62` | `CLOTHING` | Immutable table (excluded from mutable count) |
| `research/ResearchTriggerBindings.java:63` | `WOOLS` | Immutable table (excluded from mutable count) |
| `research/TableTopology.java:8` | `SEARCH_ORDER` | Immutable table (excluded from mutable count) |
| `runic/RunicSettings.java:13` | `current` | S5: process-global configuration; lifecycle not encoded in ownership |
| `runic/RunicShield.java:23` | `SERVERS` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `runic/RunicShield.java:24` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `utils/AspectOrbQueue.java:30` | `RAW` | ThreadLocal scope; keep remove/finally discipline |
| `utils/AspectOrbQueue.java:31` | `ACTIVE` | ThreadLocal scope; keep remove/finally discipline |
| `utils/AspectOrbQueue.java:32` | `RECEIPTS` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `utils/BlockUtils.java:34` | `lastx` | S3: invocation-local search cursor kept process-global |
| `utils/BlockUtils.java:35` | `lasty` | S3: invocation-local search cursor kept process-global |
| `utils/BlockUtils.java:36` | `lastz` | S3: invocation-local search cursor kept process-global |
| `utils/BlockUtils.java:38` | `lastdistance` | S3: invocation-local search cursor kept process-global |
| `utils/BlockUtils.java:51` | `followingItemSpawner` | Bootstrap/configuration callback; installation-thread/lifetime contract needed (S7) |
| `utils/CropUtils.java:30` | `standardCrops` | Registry; public mutation/freeze phase is not enforced (S6) |
| `utils/CropUtils.java:32` | `clickableCrops` | Registry; public mutation/freeze phase is not enforced (S6) |
| `utils/CropUtils.java:34` | `stackedCrops` | Registry; public mutation/freeze phase is not enforced (S6) |
| `utils/CropUtils.java:36` | `lampBlacklist` | Registry; public mutation/freeze phase is not enforced (S6) |
| `utils/DeferredItemDrops.java:68` | `LIVE_RECEIPTS` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `utils/DeferredItemDrops.java:69` | `ACTIVE` | ThreadLocal scope; keep remove/finally discipline |
| `utils/DeferredItemDrops.java:70` | `ROUTING_ACTIVE` | ThreadLocal scope; keep remove/finally discipline |
| `utils/EntityUtils.java:47` | `HPBUFF` | Public mutable rule data: S4 |
| `utils/EntityUtils.java:54` | `DMGBUFF` | Public mutable rule data: S4 |
| `utils/HexUtils.java:10` | `NEIGHBOURS` | Private lookup table; no writes found after initialization |
| `utils/InventoryUtils.java:46` | `oreTags` | Registry; public mutation/freeze phase is not enforced (S6) |
| `utils/OriginalOreTags.java:40` | `ORE_TAGS` | Registry; public mutation/freeze phase is not enforced (S6) |
| `utils/SpecialMining.java:46` | `RESULTS` | Immutable table (excluded from mutable count) |
| `utils/Utils.java:47` | `colorNames` | Public mutable rule data: S4 |
| `utils/Utils.java:50` | `colors` | Public mutable rule data: S4 |
| `utils/Utils.java:54` | `effectBuffer` | S2: dimension-key cache survives world replacement; no cleanup found |
| `visnet/VisNodeFailures.java:22` | `MUTATION` | ThreadLocal scope; keep remove/finally discipline |
| `visnet/VisNodeFailures.java:23` | `FAILURE` | ThreadLocal scope; keep remove/finally discipline |
| `visnet/VisNodeFailures.java:24` | `OBSERVING` | ThreadLocal scope; keep remove/finally discipline |
| `ward/WardedPearl.java:35` | `eventsEnabled` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `world/AuraGenRules.java:61` | `SCATTERED_TEMPLES` | Immutable table (excluded from mutable count) |
| `world/DungeonRooms.java:56` | `PAT_CONNECT` | Public mutable rule data: S4 |
| `world/DungeonRooms.java:70` | `PAT_DOORWAY` | Public mutable rule data: S4 |
| `world/EldritchBlockRules.java:128` | `ELDRITCH_GROUPS` | Public mutable rule data: S4 |
| `world/EldritchBlockRules.java:129` | `ELDRITCH_GROUP_HARDNESS` | Public mutable rule data: S4 |
| `world/EldritchBlockRules.java:130` | `ELDRITCH_GROUP_RESISTANCE` | Public mutable rule data: S4 |
| `world/EldritchBlockRules.java:295` | `COSMETIC_GROUPS` | Public mutable rule data: S4 |
| `world/EldritchBlockRules.java:297` | `COSMETIC_GROUP_HARDNESS` | Public mutable rule data: S4 |
| `world/EldritchBlockRules.java:299` | `COSMETIC_GROUP_RESISTANCE` | Public mutable rule data: S4 |
| `world/EldritchBlockRules.java:376` | `VARIANT_ITEMS` | Immutable table (excluded from mutable count) |
| `world/EldritchBlockRules.java:490` | `AIRY_GROUP_HARDNESS` | Public mutable rule data: S4 |
| `world/EldritchBlockRules.java:491` | `AIRY_GROUP_RESISTANCE` | Public mutable rule data: S4 |
| `world/EldritchMobRules.java:37` | `WARDEN_TITLES` | Public mutable rule data: S4 |
| `world/ElementalShovelHarvest.java:24` | `ACTIVE` | ThreadLocal scope; keep remove/finally discipline |
| `world/ElementalSwordSweep.java:27` | `ACTIVE` | ThreadLocal scope; keep remove/finally discipline |
| `world/ExcavationHarvest.java:46` | `ACTIVE` | ThreadLocal scope; keep remove/finally discipline |
| `world/FamilyVariant.java:31` | `PENDING` | ThreadLocal scope; keep remove/finally discipline |
| `world/MagicalTreeBiomeAdapter.java:18` | `TYPES` | Immutable table (excluded from mutable count) |
| `world/MagicalTreePlacement.java:18` | `knotsWithoutEntity` | Atomic process diagnostic counter; documented lifetime |
| `world/NodeEcologyPolicy.java:12` | `dimensionBlacklist` | Registry; public mutation/freeze phase is not enforced (S6) |
| `world/NodeEcologyPolicy.java:13` | `biomeBlacklist` | Registry; public mutation/freeze phase is not enforced (S6) |
| `world/NodeHarvest.java:36` | `PENDING` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `world/NormalNodeBiomeAdapter.java:21` | `TYPES` | Immutable table (excluded from mutable count) |
| `world/OuterLandsBossRooms.java:88` | `PEDESTAL` | Public mutable rule data: S4 |
| `world/OuterLandsRules.java:280` | `MONSTER_SPAWNS` | Immutable table (excluded from mutable count) |
| `world/OuterLandsRules.java:282` | `PASSIVE_SPAWNS` | Immutable table (excluded from mutable count) |
| `world/OuterLandsWiringRules.java:127` | `ELDRITCH_VARIANTS` | Immutable table (excluded from mutable count) |
| `world/StreamAxeHarvest.java:42` | `ACTIVE` | ThreadLocal scope; keep remove/finally discipline |
| `world/StructurePlacementRules.java:93` | `RING_GROUND_BLOCKS` | Immutable table (excluded from mutable count) |
| `world/StructurePlacementRules.java:97` | `HILLTOP_GROUND_BLOCKS` | Immutable table (excluded from mutable count) |
| `world/StructurePlacementRules.java:100` | `SNOW_COVER_BLOCKS` | Immutable table (excluded from mutable count) |
| `world/StructurePlacementRules.java:103` | `RING_SAMPLES` | Public mutable rule data: S4 |
| `world/StructurePlacementRules.java:106` | `HILLTOP_SAMPLES` | Public mutable rule data: S4 |
| `world/ThaumcraftOreFeature.java:32` | `PRIMALS` | Immutable table (excluded from mutable count) |
| `world/ThaumcraftOreFeature.java:37` | `BIOME_TYPES` | Immutable table (excluded from mutable count) |
| `world/TradeSwaps.java:41` | `JOBS` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `world/TradeSwaps.java:42` | `ACTIVE` | ThreadLocal scope; keep remove/finally discipline |
| `world/TradeSwaps.java:44` | `protection` | Bootstrap/configuration callback; installation-thread/lifetime contract needed (S7) |
| `world/TradeSwaps.java:45` | `initialized` | Bootstrap registration flag; S7 explicit init-thread contract missing |
| `world/VillageWorldGenRules.java:66` | `HOUSE_CHUNK_OFFSETS` | Private lookup table; no writes found after initialization |
| `world/VillageWorldGenRules.java:526` | `HILLTOP_SAMPLE_OFFSETS` | Public mutable rule data: S4 |
| `world/VillageWorldGenRules.java:528` | `HILLTOP_VALID_GROUND` | Public mutable rule data: S4 |
| `world/VillageWorldGenRules.java:530` | `HILLTOP_REPLACED_BLOCKS` | Public mutable rule data: S4 |
| `world/VillageWorldGenRules.java:573` | `ELDRITCH_SAMPLE_OFFSETS` | Public mutable rule data: S4 |
| `world/VillageWorldGenRules.java:575` | `ELDRITCH_VALID_GROUND` | Public mutable rule data: S4 |
| `world/VillageWorldGenRules.java:580` | `ELDRITCH_BANNER_OFFSETS` | Public mutable rule data: S4 |
| `world/VillageWorldGenRules.java:582` | `ELDRITCH_BANNER_FACINGS` | Public mutable rule data: S4 |
| `world/VillageWorldGenRules.java:646` | `ASPECT_SHARDS` | Immutable table (excluded from mutable count) |
| `world/VillageWorldGenRules.java:651` | `BAUBLE_BLANKS` | Immutable table (excluded from mutable count) |
| `world/VillageWorldGenRules.java:781` | `MAZE_RANGE_CHECK` | Bootstrap/configuration callback; installation-thread/lifetime contract needed (S7) |
| `world/WardedBehaviour.java:13` | `CALLBACK` | ThreadLocal scope; keep remove/finally discipline |
| `world/WardingCasts.java:39` | `RETRIES` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `world/WardingCasts.java:40` | `CREATING` | ThreadLocal scope; keep remove/finally discipline |
| `world/WardingCasts.java:41` | `ACTIVE` | ThreadLocal scope; keep remove/finally discipline |
| `world/WardingOverlay.java:12` | `TEXTURE` | Private lookup table; no writes found after initialization |
| `world/WardingOverlay.java:22` | `PLANE` | Private lookup table; no writes found after initialization |
| `world/WorldGenBigMagicTree.java:28` | `OTHER_COORDS` | Private lookup table; no writes found after initialization |
| `world/WorldGenBigMagicTree.java:29` | `WORLD_HEIGHTS` | Owner-keyed operational state; server-only/scope/lifecycle contract must remain explicit (S7) |
| `world/WorldGenGreatwoodTrees.java:28` | `OTHER_COORDS` | Private lookup table; no writes found after initialization |
| `world/WorldGenMoundTemplate.java:50` | `TEMPLATE` | Private lookup table; no writes found after initialization |
| `world/WorldGenWriteArea.java:22` | `TREES_REFUSED` | Atomic process diagnostic counter; documented lifetime |
| `world/biomes/BiomeHandler.java:47` | `BIOME_INFO` | Registry; public mutation/freeze phase is not enforced (S6) |
| `world/biomes/BiomePatchPolicy.java:15` | `ZOOM_SALTS` | Private lookup table; no writes found after initialization |
| `world/biomes/BiomePatchPolicy.java:16` | `WARM` | Private lookup table; no writes found after initialization |
| `world/biomes/BiomePatchPolicy.java:16` | `COOL` | Private lookup table; no writes found after initialization |
| `world/biomes/BiomePatchPolicy.java:17` | `TEMPERATURES` | Private lookup table; no writes found after initialization |
| `world/biomes/BiomeSourceBootstrapScope.java:7` | `DEPTH` | ThreadLocal scope; keep remove/finally discipline |
| `world/biomes/BiomeSurfaceRules.java:321` | `MAGICAL_FOREST_FLOWERS` | Immutable table (excluded from mutable count) |
| `world/biomes/BiomeTopBlocks.java:32` | `OVERRIDES` | Immutable table (excluded from mutable count) |
