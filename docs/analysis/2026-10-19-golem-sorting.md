# Sorting core10 — source contract, 2026-10-19

**Meaningful without colors/upgrades: recommend ordinary Sort10 as a next container core.** Destination inventories' real existing items are its filters. This is documentation only; production/runtime WIP was not read or changed. Context: [Fill/Empty source contract](2026-10-19-golem-fill-empty.md), [final inventory API](2026-10-19-golem-inventory-api.md).

Oracle root `O = ../work/thaumcraft-4.2.3.5-oracle/thaumcraft/`. E/H = `O/common/entities/golems/EntityGolemBase.java` / `GolemHelper.java`; AI names are in `O/common/entities/ai/inventory/`; I = `O/common/lib/utils/InventoryUtils.java`. Other golem filenames below share E's directory.

## Canonical acquisition

- `O/common/config/ConfigRecipes.java:371`: recipe alias **CoreSorting**, research gate **CORESORTING**, **infusion instability3**, output one `itemGolemCore` metadata**10**. Central input is **`ConfigItems.itemZombieBrain`**, not blank100 or itemResource5. Ordered component array: **Fill core0, vanilla comparator, Empty core1, vanilla paper**, one each. Aspect cost **vacuos16/permutatio16/fames16/lucrum16**; no vis/crucible substitute.
- Vanilla identifiers are `Items.field_151132_bS=comparator`, `field_151121_aF=paper` (identifier-only mapping reference `../work/references/OpenTC4/src/main/fields.csv:4127,4138`); original brain item creation is `ConfigItems.java:479`.
- `ConfigResearch.java:332`: **CORESORTING**, GOLEMANCY, position(−7,2), complexity2, concealed + secondary, parents **COREEMPTY/COREFILL/INFUSION**, no siblings. Research aspects **vacuos3/permutatio3/lucrum3/fames3**. Pages: `tc.research_page.CORESORTING.1`, then the `CoreSorting` infusion recipe. Item icon registration is `golem_core_sorting` (`ItemGolemCore.java:55`); ordinary core stack64/uncommon (`:36–39,77–78`).

## Discrimination versus Empty1

| | Source | Allowed marked destination |
| --- | --- | --- |
| Empty1 | Attached home, optional ghost whitelist | Any container with positive insertion room; no seed required. |
| Sort10 | Attached home, candidates from its actual contents | Container with positive insertion room **and an existing matching extractable seed through a marked side**. Empty/unseeded containers do not qualify. |

H:448–497,545–565,658–685 supplies this distinction. `inventoryContains` is an **extraction simulation**, not a simple item-count lookup (I:204–265): a visible but side-locked seed does not qualify. The probe does not consume the seed. Default matching is item + damage + NBT, ignoring quantity (I:313–340); native equivalent is item + full components, ignoring count.

Seed chest A with real stone and chest B with real wood; a mixed attached input can then sort those types without upgrades. Unseeded types stay home. Multiple suitable destinations are chosen by distance, not quotas, balancing or requested quantities.

## Actual ordinary worker loop

1. E:335–350 registers **HomeReplace0 → SortingPlace1 → SortingGoto3 → HomeTakeSorting4**, plus ReturnHome6; **no world-drop goal**. Original walking home H₀/placement face F locate source **B=H₀−F**. Markers are destinations, never alternate input inventories; no marker-role/color schema is necessary.
2. At home with no cargo, idle navigation and home-center distance²≤5, every `golemDelay` ticks (default5), enumerate home face-visible stacks in slot order; discard candidates lacking a seeded, roomy marked destination or on transient item timeout (default10000ms). Probe, then re-read before extracting **up to material carry space from the first matching extractable source slot** (`AIHomeTakeSorting:38–70,94–138`; H:491–497,545–578; `Config.java:879–880`). This is not a reservation or a copied-stack grant.
3. With real cargo, every5 ticks select the nearest seeded/roomy marked container, excluding B, at golem-to-container-center distance²≤range²; ordinary range16, ties retain traversal order (`AISortingGoto:49–85`; E:460–473). Goto continues while navigating, at most200 updates, with20-update unsticking attempt (`:89–113`).
4. Idle and adjacent (container-center distance²<4), probe insertion **and seed extraction** on marked faces, then deposit actual cargo and keep leftovers; container interaction holds5 updates (`AISortingPlace:41–73,97–136`; H:66–75). If seeds/room disappear, HomeReplace returns cargo to B; blocked/missing home retains it (`AIHomeReplace:37–58,92–125`). Native must revalidate live destination/seed before commit and preserve existing sided permissions, real-count and busy guards. Unknown/unloaded endpoints cannot authorize movement or dumping.

## Ghosts, quantities, colors and upgrades

- **Sort10 has GUI/canSort=true but hasInventory=false** (`ItemGolemCore:81–119`). `setupGolemInventory` exits before six-slot allocation, and `ContainerGolem:32–58` binds **no ghost slots**. Thus no Sort wanted quantities, whitelist rows, duplicate-ghost summation or Fill double-slot behavior exists. H:658–685 copies each real home slot, including duplicate item types; these are candidate duplicates, not summed stock targets. Extraction replaces candidate count with carry space.
- Source/Goto query marker color **−1**; `Marker.equalsFuzzy:31–35` makes this a wildcard. Ordinary empty internal inventory yields −1 for commit too (E:142,170,560–578; `AISortingPlace:107–120`). **Order4/colors are not prerequisites or source/destination selectors for baseline Sorting.** Bell only enables color cycling with Order4 (`ItemGolemBell:131–165`).
- **Entropy5** optionally exposes ore-dictionary / ignore-damage / ignore-NBT flags (E:834–843; `O/client/gui/GuiGolem.java:237–260`); all remain off in the ordinary baseline. Fire2 does not create Sort ghost slots because hasInventory=false (E:198–232). Generic material carry/speed/range and their upgrades are separate, not an additional sorting rule. Colors, fuzzy flags, upgrades/decorations remain explicit later scope, not silently approximated.

## Minimum native extension / concrete gaps

- Add finite **`GolemData.SORTING=10`** support to the existing core predicate/CODEC/STREAM, actual Core item constructor/tooltip, root **CORE_SORTING** registration and exact `coreForType(10)` recall mapping. Do not enable other deferred core IDs. Existing `configuredData()/applyConfiguration`, home/cargo and `getMarkers()/setMarkers(List<GolemMarker>)` suffice; no new public inventory/provider API or marker fields are needed.
- Keep the five-field data schema: ordinary Sort configuration uses six EMPTY exemplars/zero counts, false Fill mode, existing dimension/position/face markers. Do not invent Sort quantity controls or silently normalize unsupported nonbaseline payloads. Entity save/normal recall must preserve core10, markers, material item and name, with **cargo separate** (E:581–623; `ItemGolemBell:313–334`). No persistent destination cache or journal is needed; physical destination stock remains the routing authority.
- Root menu/client must show Sorting caption/seed explanation, hide/disable ghosts, quantity edits and Fill button50 **on the server too**; preserve real inventory slots and marker outlines/reset. Existing core sync may carry10; no new input packet is required. Add native infusion recipe/book alias/language/model/badge, not registration alone. Advanced fuzzy persistence would need separate flags: existing fillAnyAmount is not an entropy-toggle byte; original recall omitted toggles.
- **Source quirks to disclose if corrected:** H:658–685 lists only the attached chest half; HomeTakeSorting's partner retry still uses that list, missing partner-only item types. Room preflight/Goto can find room on one marked face and seed on another, while SortingPlace requires both on its probed face. Its partner probe also checks seed on `te`, not the partner (`AISortingPlace:63–66`). Reusing native logical double-chest identity, excluding the entire home inventory, and requiring same-face seed+room throughout are bounded correctness adaptations—not claims of literal original behavior.

Usable scope is exact-component seeded sorting among the already supported vanilla/Hungry containers, with proper infusion acquisition, guarded real movement and recall/UI above. No color dependency forces choosing another core; unsupported containers, advanced matching and the listed literal-source quirks remain precise boundaries, not grounds for a new automation/audit framework.
