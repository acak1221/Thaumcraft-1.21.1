# Straw golem item/data handoff — 2026-10-18

Implemented only these four new classes under `common/src/main/java/thaumcraft/common/items/golems/`, plus this note. Runtime/AI, registry/bootstrap, menus, renderers, recipes, language files, dependencies, exports and saves were not edited; other owners' WIP was not read. **Root's actual Fabric compile/integrated checks remain pending; no Java/Gradle/game/tests/Python/smoke tooling ran here.**

## Public API / integration

- `GolemData(int core, List<ItemStack> filters)`; `NO_CORE=-1`, `GATHER=2`, `FILTER_SLOTS=6`, `EMPTY` = coreless/six empty ghosts. Public `CODEC`, `STREAM_CODEC<RegistryFriendlyByteBuf, GolemData>`, `core()`, defensive `filters()`.
- `ItemGolemPlacer(Item.Properties)`; native `useOn(UseOnContext)`.
- `ItemGolemCore(Item.Properties, int core)` accepts only `BLANK=100` or Gather2; public `getCore()`. No competing attachment/payment interaction.
- `ItemGolemBell(Item.Properties)`; public static `InteractionResult select(ItemStack, Player, EntityGolemBase)`, native living-item fallback, block marker toggles, public static `boolean clearMarkers(ItemStack)` for a future existing server item-key hook. No new attack callback or air/sneak binding.

Uses the agreed `ThaumcraftGolems.GOLEM` and `GOLEM_DATA` suppliers, and only the agreed entity methods `applyConfiguration`, `setHome`, `setOwnerName`, `getHomePosition`, `getHomeFacing`, `canConfigure` plus inherited native mob methods. **No extra entity API is required.** Entity `mobInteract` should return `select`'s consuming result; entity `hurt` owns bell recall/cargo. Root supplies stack limits, recipes, menus and item rendering. Root's stated UseBlockCallback already dispatches placer `useOn` before chest GUI: propagate its result (including FAIL), rather than retrying native item use. This slice adds no second hook or teleport correction.

Register the component with `.persistent(GolemData.CODEC).networkSynchronized(GolemData.STREAM_CODEC)` as agreed; no custom JSON/SNBT adapter is needed. Constructors do not resolve deferred registry suppliers or import client classes.

## Implemented contracts

**Ghost configuration:** native optional ItemStack codecs preserve EMPTY and full components. Persistent representation requires exactly `core` and `filters`; unsupported/duplicate fields, unsupported/nonintegral core, non-six lists and nonempty counts other than one fail, without offering a partial result. Wire format is core varint then exactly six native optional stacks, with no supplied list size. Constructor and accessor copy stacks/lists. Equality explicitly loops `ItemStack.matches`; hash uses native `hashItemAndComponents`, count, slot order and core—not record/List stack identity. Thus defensive copies and legitimate codec reloads are intended to compare semantically; that round trip has not been executed here.

Names remain `CUSTOM_NAME`. No owner/home/health/fuel/cargo/advanced/upgrades/decorations/markers enter this component. Placer deployment refuses nonempty CUSTOM_DATA or ENTITY_DATA/BLOCK_ENTITY_DATA/BLOCK_STATE payloads rather than treating unsupported configuration as a fresh empty golem. Only an absent GOLEM_DATA means fresh/coreless; callers must not convert a codec error into EMPTY.

**Placement:** original `ItemGolemPlacer:164–240` adjacent-cell feet/home and clicked face are retained, including `(UP && oak fence) || nether-brick fence`'s +0.5 offset. Names/core/ghosts/owner/home/persistence are configured before admission. Only actual new server candidates sample random yaw. Non-sneaking, alive, same-hand/world/thread, reach and native world/item permissions are checked; body and neighbor collision reads require loaded chunks. No block replacement/breaking occurs. Real body collision, entity overlap, liquids, border/build height and supporting collision geometry are checked; support allows up to the original one-cell settling fall onto chest/slab geometry, not unsupported air placement. These are documented safety restrictions beyond the old unchecked spawn.

One per-item/per-thread busy bit blocks recursive placement. Exactly one native admission is attempted. False/throw without a native owner keeps the placer and discards only the detached fresh candidate. Native success or installed native entity callback/lookup ownership is decisive, including later throwing/removal callbacks; no entity retry, output grant, UUID-registry surgery, refund or world rollback is attempted. After admission survival payment occurs only against the same unchanged hand stack/count/components; creative keeps it. If an ADD callback already replaces/exhausts/changes that hand, its changes are retained, not charged to a different stack or reconstructed. Notifications occur after payment. This is a bounded native adaptation, not a generic callback/crash/power-loss exactly-once claim. It reuses the already-existing `EntityLevelCallbackAccess`, with no new mixin/bootstrap work.

**Bell:** right-click selects the actual live/configurable entity without an owner-only lock or any home/cargo mutation. Identical re-selection consumes without repeated data/sound effects. Selection uses native CUSTOM_DATA under `thaumcraft:golem_bell`: `Golem` UUID, canonical namespaced `Dimension`, packed BlockPos long `Home`, byte `Face`, compound-list `Markers`. Each local marker has packed long `Pos`, namespaced `Dimension`, byte `Side` and byte `Color=-1`; duplicates/unknown fields/invalid faces/colors are rejected intact. Local markers are capped at64. Switching selected entity resets these local links; selecting the same entity preserves them. Marker edits require the selected live golem in the same dimension and ordinary player/entity/block permissions. Block click toggles the marker, never rehomes Gather. The feedback is the original orb sound adaptation (.7, pitch1..1.1). Committed edits remain CONSUME even if notification fails.

## Explicit remaining work / adaptations

- **Marker propagation, rendering, color cycling and other-core routing are not implemented.** There is no entity marker API yet; links are local bell metadata only, and the tooltip explicitly says so. No markers are copied into the placer. `clearMarkers` is not bound to a key: its future server caller must validate the real held item/action and send normal inventory/sound updates. No original F-key claim is made.
- Core install, six-ghost GUI, real carried stock, recall drops, AI, health and entity persistence are runtime/menu owner work. Normal/sneak recall must continue to distinguish ghost filters from real cargo. No advanced/upgrades/decorations/fuel or world migration support is implied. Unsupported future data must be retained/refused, not decoded and stripped.
- `canAttackBlock=false` is the requested native non-mining bell tool behavior, not an assertion that the original bell overrode that method. Original source anchors and the four exact recipes/research are in `docs/analysis/2026-10-18-straw-golem-original.md`.
- Root language additions used here: `tooltip.thaumcraft.golem.{core_blank,core_gather,coreless,invalid_configuration,bell,bell_markers_local,bell_selected}`. `bell_selected` parameters are dimension, home X, Y, Z, local-marker count. Existing original textures remain root's model/icon inputs; no resource files were edited here.

## Authored source SHA-256

| File | SHA-256 |
| --- | --- |
| `GolemData.java` | `e347b76721fcbe63c5015d42ed45a04d8b6f85b188749d6927b5e63e0d10ef4f` |
| `ItemGolemPlacer.java` | `933cc11cc04b1c0a087010ff9ff08351e268271447deea353a4a90c83dba3490` |
| `ItemGolemCore.java` | `a4a78e388b8ff1d77c2f2cbb94dcfc12235d94264e5b04dddd9e9c1a6fe59f5d` |
| `ItemGolemBell.java` | `6b54a21afa7d954624a7023828caa1c91ab011b58792b3e8e698f9337263c712` |

These identify authored inputs, not a compiled or accepted gameplay result.
