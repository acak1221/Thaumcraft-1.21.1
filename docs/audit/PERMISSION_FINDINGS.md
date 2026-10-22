# Permission audit — g1perm, 2026-10-23

Scope: the 16 requested feature families, not every tile class in the mod. Fourteen are implemented block-entity families; the Traveling Trunk is an entity, and no modern golem press implementation was found. Inspection means source review unless a test is named below. Missing source guards are **not** counted as demonstrated exploits.

The new tests deliberately exercise the mod's entry points directly. Native menu click/button packets and ordinary block-use dispatch have additional vanilla/loader checks. A direct-method red test demonstrates missing local authority validation, **not** that an unprivileged remote packet can reach that method through those upstream checks. No external exploitability claim is made for these cases.

## Gate inventory

V = viewer identity AND current open menu; A = alive AND not spectator; P = fresh `Level.mayInteract`; D = distance. “Partial” describes the missing half in the note. For menu-free actions V is not applicable. This table concerns actor-driven entry points, not autonomous ticks, hoppers, or item collisions.

| Family / inspected implementation | V | A | P | D | Evidence / disposition |
| --- | --- | --- | --- | --- | --- |
| Crucible — `BlockCrucible`, `TileCrucible` | n/a | Partial: wand helper rejects spectator, block wand branch does not; no alive check | Water/helper yes; block wand branch no | Missing in both helper and water use | `permissionCrucibleRemoteWand` authored; execution pending. Item collision input has no actor to authenticate. |
| Research table — `BlockTable`, `TileResearchTable`, `ContainerResearchTable`, edit/combine/copy handlers | Yes for payloads | Yes in menu; missing conversion checks | Missing in menu and conversion | Yes in menu; missing conversion | Prior spectator payload guard already present. `permissionResearchConversionSpectator` authored; execution pending. |
| Arcane workbench — block/tile/menu | Yes | Yes in menu | Missing from menu validity; conversion has it | Yes in menu | Reasoned, untested; no speculative fix. |
| Alchemy furnace — block/tile/menu | Yes | Yes in menu | Missing from menu validity | Yes | Reasoned, untested. |
| Advanced alchemy furnace — `BlockAdvancedAlchemyFurnace`, `TileAlchemyFurnaceAdvanced`, `AdvancedFurnaceFormation` | n/a: no menu | Formation rejects spectator; incomplete alive coverage | Formation checks every piece | No equivalent explicit formation reach gate observed | Reasoned, untested. Input is dropped-item collision, not a menu intent. |
| Infusion matrix — block/tile, `InfusionStructure` | n/a | Yes at structure/start paths | Yes | Yes, `canInteractWithBlock` | Reasoned, untested; already has explicit checks. |
| Thaumatorium — block/tile/menu | Yes at menu entry | Yes via tile | Missing from menu validity | Yes | Reasoned, untested. |
| Golem press | n/a | n/a | n/a | n/a | No matching modern implementation/registration found; cannot test. |
| Jars — `BlockJar`, shared essentia/label interactions | n/a | Partial: spectator, no alive check in block gate | Yes | Yes | Reasoned, untested; not permission-free. |
| Tubes/valves — `BlockTube`, shared label/hand/wand paths | n/a | Partial: spectator, no alive check in `mayUse` | Yes, rechecked | Yes plus geometry retrace | Reasoned, untested; not permission-free. Buffer variants not exhaustively traced. |
| Deconstruction table — block/tile/menu | Yes | Partial: spectator in menu, no alive check | Missing | Yes | Reasoned, untested. |
| Focal manipulator — block/tile/menu/network | Yes | Yes | Missing from tile/menu validity | Yes | Reasoned, untested. |
| Warding focus — `WardingCasts`, `TileWarded` | n/a | Yes | Yes, each target | Initial ray uses block interaction range | Reasoned, untested; additionally invokes Architectury break/place events. |
| Hungry chest — block/tile, vanilla `ChestMenu` | Native packet-selected menu; not a custom viewer-bound menu | Alive at opening; native spectator click rejection | At opening only | Yes via tile | Reasoned, untested; direct menu methods do not imply a remotely reachable bypass. |
| Traveling Trunk — entity/menu | Missing before fix | Missing before fix | Missing before fix | Button validity yes; quick-move missing | `permissionTrunkSpectator`, `permissionTrunkRemote` authored; execution pending. |
| Bore — tile/menu, `BoreWork`, Fabric platform | Yes | Yes | Open/wand/work yes; menu validity lacks fresh P | Yes | Reasoned, untested here. Existing protection tests are not claimed as rerun. |

## Breaking and loader events

The reviewed table/workbench/furnace/deconstruction/focal/chest/tube/crucible removal paths evacuate from `onRemove`, after an actual replacement, rather than custom `playerWillDestroy` pre-drops. There is no common `onDestroyedByPlayer` override in these sources (that Forge-specific API cannot simply be assumed on Fabric). Native player breaking must pass the loader's break event before removal; direct `Level.setBlock` is not a player permission check and must still preserve inventory. Adding actor checks to `onRemove` would incorrectly lose contents during legitimate non-player replacement.

This is a source-level negative result, **not** a denied-break GameTest. No claim mod was installed or tested. `mayInteract` is not a universal claims API: loader callbacks and individual claim mods can impose additional policy. Bore explicitly bridges Architectury and Fabric BEFORE/CANCELED/AFTER events. Warding explicitly bridges Architectury break/place; Fabric-specific claim parity remains untested.

## Runtime evidence

Pending baseline/fixed runs. Do not treat authored tests as passing evidence.
