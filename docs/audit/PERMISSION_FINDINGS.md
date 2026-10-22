# Permission audit — g1perm, 2026-10-23

Scope: the 16 requested feature families, not every tile class in the mod. Fourteen are implemented block-entity families; the Traveling Trunk is an entity, and no modern golem press implementation was found. Inspection means source review unless a test is named below. Missing source guards are **not** counted as demonstrated exploits.

The new tests deliberately exercise the mod's entry points directly. Native menu click/button packets and ordinary block-use dispatch have additional vanilla/loader checks. A direct-method red test demonstrates missing local authority validation, **not** that an unprivileged remote packet can reach that method through those upstream checks. No external exploitability claim is made for these cases.

## Gate inventory

V = viewer identity AND current open menu; A = alive AND not spectator; P = fresh `Level.mayInteract`; D = distance. “Partial” describes the missing half in the note. For menu-free actions V is not applicable. This table concerns actor-driven entry points, not autonomous ticks, hoppers, or item collisions. Changed rows describe final guards and identify the original omissions.

| Family / inspected implementation | V | A | P | D | Evidence / disposition |
| --- | --- | --- | --- | --- | --- |
| Crucible — `BlockCrucible`, `TileCrucible` | n/a | Was partial; now yes | Was missing in block wand branch; now shared gate | Was missing; now yes | `permissionCrucibleRemoteWand` failed on original source: water became empty. Fix `51a462e2`: shared living/non-spectator/level/reach/P/item-use gate for wand and water interactions. Item collision input has no actor to authenticate. |
| Research table — `BlockTable`, `TileResearchTable`, `ContainerResearchTable`, edit/combine/copy handlers | Yes for payloads | Menu already yes; conversion now yes | Menu still missing; conversion now checks both halves | Menu yes; conversion now yes | `permissionResearchConversionSpectator` failed on original source: both blocks converted. Fix `7dd49c74`: reject invalid actor/reach, check P on both halves. Prior spectator payload guard already present; menu P concern remains reasoned, untested. |
| Arcane workbench — block/tile/menu | Yes | Yes in menu | Missing from menu validity; conversion has it | Yes in menu | Reasoned, untested; no speculative fix. |
| Alchemy furnace — block/tile/menu | Yes | Yes in menu | Missing from menu validity | Yes | Reasoned, untested. |
| Advanced alchemy furnace — `BlockAdvancedAlchemyFurnace`, `TileAlchemyFurnaceAdvanced`, `AdvancedFurnaceFormation` | n/a: no menu | Formation rejects spectator; incomplete alive coverage | Formation checks every piece | No equivalent explicit formation reach gate observed | Reasoned, untested. Input is dropped-item collision, not a menu intent. |
| Infusion matrix — block/tile, `InfusionStructure` | n/a | Yes at structure/start paths | Yes | Yes, `canInteractWithBlock` | Reasoned, untested; already has explicit checks. |
| Thaumatorium — block/tile/menu | Yes at menu entry | Yes via tile | Missing from menu validity | Yes | Reasoned, untested. |
| Golem press | n/a | n/a | n/a | n/a | No matching modern implementation/registration found; cannot test. |
| Jars — `BlockJar`, shared essentia/label interactions | n/a | Partial: spectator, no alive check in block gate | Yes | Yes | Reasoned, untested; not permission-free. |
| Tubes/valves — `BlockTube`, shared label/hand/wand paths | n/a | Partial: spectator, no alive check in `mayUse` | Yes, rechecked | Yes plus geometry retrace | Existing `essentiaValvesSampleEdgesAndPreserveBufferedEssentia` passed, but uses the anonymous mock: its mode-switch assertion is **not counted as true spectator proof**. Gates reasoned, untested; not permission-free. Buffer variants not exhaustively traced. |
| Deconstruction table — block/tile/menu | Yes | Partial: spectator in menu, no alive check | Missing | Yes | Reasoned, untested. |
| Focal manipulator — block/tile/menu/network | Yes | Yes | Missing from tile/menu validity | Yes | Reasoned, untested. |
| Warding focus — `WardingCasts`, `TileWarded` | n/a | Yes | Yes, each target | Initial ray uses block interaction range | Reasoned, untested; additionally invokes Architectury break/place events. |
| Hungry chest — block/tile, vanilla `ChestMenu` | Native packet-selected menu; not a custom viewer-bound menu | Alive at opening; native spectator click rejection | At opening only | Yes via tile | Reasoned, untested; direct menu methods do not imply a remotely reachable bypass. |
| Traveling Trunk — entity/menu | Now yes | Now yes | Now yes | Now yes at each mutation | Five `permissionTrunk*` tests failed on original source: spectator/dead/other-viewer/closed-menu stay changes; remote quick-move removed diamond. Fix `1a21f764`: viewer + current-menu binding, living/non-spectator, P, preserved 10-block distance, rechecked upgrade access, guarded button/click/quick-move. |
| Bore — tile/menu, `BoreWork`, Fabric platform | Yes | Yes | Open/wand/work yes; menu validity lacks fresh P | Yes | Reasoned, untested here. Existing protection tests are not claimed as rerun. |

## Breaking and loader events

The reviewed table/workbench/furnace/deconstruction/focal/chest/tube/crucible removal paths evacuate from `onRemove`, after an actual replacement, rather than custom `playerWillDestroy` pre-drops. There is no common `onDestroyedByPlayer` override in these sources (that Forge-specific API cannot simply be assumed on Fabric). Native player breaking must pass the loader's break event before removal; direct `Level.setBlock` is not a player permission check and must still preserve inventory. Adding actor checks to `onRemove` would incorrectly lose contents during legitimate non-player replacement.

`permissionSpectatorCannotBreakContainers` additionally **passed on original source**: an actual spectator's native `gameMode.destroyBlock` was denied for workbench, basic alchemy furnace, deconstruction table, focal manipulator and hungry chest; block identity and full saved tile NBT (including an inserted diamond) were unchanged. No break/drop fix was justified there. Other break paths remain reasoned, untested. No claim mod was installed or tested. `mayInteract` is not a universal claims API: loader callbacks and individual claim mods can impose additional policy. Bore explicitly bridges Architectury and Fabric BEFORE/CANCELED/AFTER events. Warding explicitly bridges Architectury break/place; Fabric-specific claim parity remains untested.

## Runtime evidence

Initial runs used vanilla's misleading anonymous mock-player subclass; its `isSpectator()` is hardcoded false. Those “spectator” results are explicitly discarded. The fixtures now create real connected `ServerPlayer` instances and assert the spectator flag before exercising the entry point.

Corrected original-source baseline: **7 failures, 1 pass**, retained in `evidence/permission-baseline.xml` (same XML content, with a terminating newline). The three production files were temporarily restored to exact `d6ae5c78` contents for this run; then the committed fixes were restored. This demonstrates **two block-entity families plus one entity family** with missing local mutation guards, not fourteen broken block entities.

Fixed integrated verification: **14/14 focused Fabric GameTests passed**, including all eight new tests, native positive crucible use, positive trunk item/menu behavior, two table conversion cases, valve behavior and research-copy behavior. Report: `evidence/permission-fixed.xml`. **`:common:test`: 2,470 tests, zero failures/errors/skips.** Fresh `fabric/run-gametest/world` for corrected baseline and fixed run. No client visual test, claim-mod test, packet exploit demonstration, NeoForge task or full Fabric suite is claimed.

Invocation used Java 21 and isolated `.gradle-g1`:

```sh
./gradlew :common:test :fabric:runGameTest --offline \
  -I .tmp/g1-local-repository.init.gradle \
  -PgameTestFilter=permissionTrunkSpectator,permissionTrunkRemote,permissionTrunkOtherViewer,permissionTrunkClosedMenu,permissionTrunkDeadViewer,permissionCrucibleRemoteWand,permissionResearchConversionSpectator,permissionSpectatorCannotBreakContainers,crucibleDissolvesAndCraftsWithOriginalCosts,travelingTrunkItemUpgradesMenuBellDamageAndSaving,tableConversionDirectionsAndCosts,tableSecondaryQuirksAndSearchOrder,essentiaValvesSampleEdgesAndPreserveBufferedEssentia,researchTableCopiesDiscoveriesWithExactCosts
```

The ignored init script only supplies an isolated Maven index of copied general-cache artifacts and pins Architectury's tooling-only `fabric-loader:+` to cached `0.16.14`; production build/dependency files were not changed. With a healthy normal Gradle cache, omit that local workaround. See the analysis handoff for initial failed/cancelled infrastructure attempts.
