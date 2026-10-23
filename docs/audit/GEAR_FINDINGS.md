# Equipment hook audit — g28gear — 2026-10-23

## Result and evidence standard

**68 registered equipment items inspected. No dead equipment hook has been proven.**
The native attack, mining, armor, payment and accessory paths tested do run. A
different, reproducible repair regression was found in **two items**: the Primal
Crusher and Crimson Blade repaired in creative mode, unlike original TC4.

`P` below means at least the explicitly named gameplay outcome was measured
through a real player path, not that every property, visual or bonus of the item
has full acceptance. `R` means source/reasoning only for its application. Calling
an item override or discount helper directly does **not** prove dispatch.
Client drawings/node visibility remain unverified, even when the server behavior
is P. Wands, foci, consumables and projectiles other than the requested bone bow
are outside this equipment inventory.

## New measurements

| Native path | Control | Equipped / special item | Finding / change |
|---|---|---|---|
| Workbench result extraction, gold cap, 5 Aer recipe | 500 centivis actually debited | Goggles and each void robe piece: 475; robe chest/legs: 490; robe boots and Aer ring: 495 | Discount reaches the real payer; no production change |
| Player attack | Diamond sword: Weakness 0, Hunger 0 ticks | Crimson blade: Weakness 60, Hunger 120 ticks | Sap hook executes; no change |
| Survival `gameMode.destroyBlock`, nine-stone plane | Diamond pick: 8 blocks remain, 1 durability spent | Crusher: 0 blocks remain, 9 durability spent | Area break hook executes; no change |
| Native block use, nine dirt cells | Diamond hoe: 1 farmland, 1 wear | Growth hoe: 9 farmland, 9 wear | Native area-till dispatch executes |
| Native held-use ticks, no explicit release call | Bone bow starts with 16 arrows, wear0 | After22 native ticks: auto-released, one projectile, 15 arrows, wear1 | Native automatic-release dispatch executes |
| Native worn tick, both vis amulet variants | Held wand0 Aer, worn amulet10 Aer | Wand5, amulet5 centivis | Both real accessory variants transfer vis |
| Native `ServerPlayer.doTick`, damaged inventory item | At tick 19: damage 50; survival tick 20: 49 | Before fix, creative tick 40: **48** for both crusher and blade; expected **49** | Failing GameTest first; share existing `VoidMetal.repair`, retaining survival cadence and creative exclusion |

The creative-repair failure is **not** evidence of a dead hook: the hook runs too
eagerly. It is recorded separately rather than presented as confirmation of the
original hypothesis. The existing original-bytecode audit documents the crusher
rule in `docs/goals/2026-08-03-eldritch-full-parity-audit-fixes/reports/A-011-primal-runtime.md`,
finding A-011-F08. The audited legacy Crimson Blade delegates to the same
void-tool repair semantics; the original oracle locator is
`ItemCrimsonSword.java:81-85` in `docs/porting/audit-reviews-a4-foci.json`.

## Tests

New checks live in
`common/src/gameTest/java/thaumcraft/common/testing/GearHookAuditChecks.java`:

- `g28GearDiscountPayments`: actual workbench block interaction, native menu
  result extraction, registered gear equipped one piece at a time, finite payer
  balance measured. No call to the discount reducer for the observed result.
- `g28GearCrimsonAttack`: `Player.attack`, not `hurtEnemy` invoked by the test.
- `g28GearCrusherBreak`: `gameMode.destroyBlock`, not `mineBlock` invoked by the test.
- `g28GearCreativeRepair`: native inventory ticking; includes all five void
  tools, all four void armor pieces, all three void robe pieces, crusher and
  blade. Packed damage 50→49; worn void armor/robes 49→47 at the next repair
  boundary; creative must preserve the resulting damage.
- `g28GearNativeUseAndAmulets`: native block use for the Growth hoe, native
  held-use ticking for bone-bow automatic release, native worn ticks for both
  vis amulet variants. Never calls the item overrides directly.

Existing tests rerun for application evidence are abbreviated in the inventory:

| Key | GameTest / check class |
|---|---|
| T | `thaumiumEquipmentCraftsMinesProtectsAndRepairs` / `ThaumiumEquipmentRuntimeChecks` |
| V | `voidMetalGearMendsWithersAndWarps` / `VoidGearRuntimeChecks` |
| VR | `voidThaumaturgeRobesMendWardAndDye` / `VoidRobeRuntimeChecks` |
| F | `thaumiumFortressWardsSetsAndMasks` / `FortressRuntimeChecks` |
| R | `robesCraftDyeWashProtectAndRepair` / `RobeRuntimeChecks` |
| G | `gogglesCraftProtectAndReduceVis` / `GogglesRuntimeChecks` |
| A | `streamAxeChopsEndpointVacuumsAndFollowsDrops` |
| P | `pickaxeOfTheCoreBurnsWearsAndFindsClusters` |
| H | `hoeOfGrowthTillsFertilisesAndGrowsSaplings` — direct `useOn`; N adds native area-till dispatch proof |
| E | `shovelOfTheEarthmoverMinesAndLaysSquares` |
| Z | `swordOfTheZephyrLiftsBlowsAndSweeps` — actual attack/sweep; wind uses direct hook calls |
| B | `bone_bow_keeps_its_original_draw_and_damage` — native use, explicit release; N separately proves automatic release |
| BA | `runicAccessoriesUseOriginalEquipmentAndDepletionUpgrades` |
| VA | `visAmuletsTransferAndPayThroughActualNativeRelay` |
| TR | `travellerBootsPreserveOriginalNativeMovementAndJump` |
| HH | `hoverHarnessConsumesNativeFuelAndRestoresFlightPermissions` |
| HG | `hoverGirdleReducesOnlyPositiveFallDistanceThroughActualWornBelt` |
| W | `warpDispatchesOriginalBranchesWithNativeEffects` |
| N | New g28 checks above |

## Inventory (68 items)

All IDs below have namespace `thaumcraft`. “—” means no production fix warranted
by the measured result. Stats alone are not called a special set bonus.

| # | Item ID | Bonus / behavior inspected | Measured application / test | Fix |
|---:|---|---|---|---|
| 1 | thaumium_sword | Tier damage, repair eligibility | P: real attack and anvil, T | — |
| 2 | thaumium_pickaxe | Tier mining/harvest, repair | P: obsidian break/drop and anvil, T | — |
| 3 | thaumium_axe | Tier damage, repair | P: real attack/anvil, T | — |
| 4 | thaumium_shovel | Tier damage, repair | P: real attack/anvil, T | — |
| 5 | thaumium_hoe | Tier damage, repair | P: real attack/anvil, T | — |
| 6 | thaumium_helmet | Native armor, repair | P: equipped armor value and anvil, T | — |
| 7 | thaumium_chestplate | Native armor, repair | P: equipped armor value and anvil, T | — |
| 8 | thaumium_leggings | Native armor, repair | P: equipped armor value and anvil, T | — |
| 9 | thaumium_boots | Native armor, repair | P: equipped armor value and anvil, T | — |
| 10 | void_sword | Repair, Wither, gear warp | P: native repair N; actual attack Wither60, V; warp sum alone not proof | — |
| 11 | void_pickaxe | Repair, Wither, gear warp | P: native repair N; actual attack Wither80, V | — |
| 12 | void_axe | Repair, Wither, gear warp | P: native repair N; actual attack Wither80, V | — |
| 13 | void_shovel | Repair, Wither, gear warp | P: native repair N; actual attack Wither80, V | — |
| 14 | void_hoe | Repair, Wither, gear warp | P: native repair N; actual attack Wither80, V | — |
| 15 | void_helmet | Packed/worn repair, warp | P: native repair, N/V | — |
| 16 | void_chestplate | Packed/worn repair, warp | P: native repair, N | — |
| 17 | void_leggings | Packed/worn repair, warp | P: native repair, N | — |
| 18 | void_boots | Packed/worn repair, warp | P: native repair, N/V | — |
| 19 | fortress_helmet | Ward/set multiplier, goggles/masks | P: actual damage and masks F; Devil dispatch W; vision R | — |
| 20 | fortress_chestplate | Ward/set multiplier | P: actual damage, F | — |
| 21 | fortress_leggings | Ward/set multiplier | P: actual damage, F | — |
| 22 | void_robe_helmet | 5% discount, repair, ward, sight, warp | P: 500→475 N, repair N, actual ward VR; sight R | — |
| 23 | void_robe_chestplate | 5% discount, repair, ward, warp | P: 500→475 N, repair N, ward VR, event gate W | — |
| 24 | void_robe_leggings | 5% discount, repair, ward, warp | P: 500→475 N, repair N, actual ward VR | — |
| 25 | goggles_revealing | 5% discount, node sight | P: 500→475 N, actual menu G; node drawing R | — |
| 26 | robe_chestplate | 2% discount, dye, repair | P: 500→490 N; native dye/wash/anvil R | — |
| 27 | robe_leggings | 2% discount, dye, repair | P: 500→490 N; native craft/anvil R | — |
| 28 | robe_boots | 1% discount, dye, repair | P: 500→495 N; native craft/anvil R | — |
| 29 | elemental_axe | Tree harvest, vacuum | P: real break endpoint A; vacuum dispatch not separately certified | — |
| 30 | elemental_pickaxe | Clusters, paid ore scan | P: actual ore break P; scan hook dispatch has separate interaction coverage in repo, not certified here | — |
| 31 | elemental_shovel | Area dig, area placement | P: actual area break E; some placement checks call override directly | — |
| 32 | elemental_sword | Sweep and wind/lift | P: real attack sweep Z; wind tick dispatch R | — |
| 33 | elemental_hoe | Area till, fertilize, sapling growth | P: native area till1→9, wear1→9 N; native fertilize/sapling dispatch not separately measured | — |
| 34 | boots_traveller | Jump, movement, fall reduction | P: native jump +.275 and air travel ~.0455, TR; ground tick helper alone not sufficient | — |
| 35 | hover_harness | Paid flight, discounts | P: native living step flight and finite fuel HH; discount application R | — |
| 36 | bone_bow | Projectile/ammo/durability | P: N native held ticks auto-release one arrow, ammo16→15, wear0→1; B checks projectile numbers | — |
| 37 | primal_crusher | Area mining, self-repair | P: N native area9/use9, survival repair1/20; creative repair incorrectly1/20 | Shared repair |
| 38 | crimson_sword | Sap and self-repair | P: N native Weakness60/Hunger120; creative repair incorrectly1/20 | Shared repair |
| 39 | vis_amulet_lesser | Worn vis transfer, relay charging | P: native worn10→5/wand0→5 N; relay balances VA | — |
| 40 | vis_amulet | Worn vis transfer, relay charging | P: native worn10→5/wand0→5 N; greater-variant relay charging independently R | — |
| 41 | hover_girdle | Positive fall reduction, harness timing | P: actual worn belt HG; harness fuel boundary361→289 HH | — |
| 42 | mundane_amulet | Blank runic-augment target; base0 | R: no intrinsic bonus promised | — |
| 43 | mundane_ring | Blank runic-augment target; base0 | R: no intrinsic bonus promised | — |
| 44 | mundane_belt | Blank runic-augment target; base0 | R: no intrinsic bonus promised | — |
| 45 | runic_ring_lesser | Shield capacity1 | R: shared pipeline, not individually equipped here | — |
| 46 | runic_ring | Shield capacity5 | R: shared pipeline, not individually equipped here | — |
| 47 | runic_ring_charged | Capacity4, faster recharge | P: actual equipped shared capacity/recharge BA; charged timing independently R | — |
| 48 | runic_ring_healing | Capacity4, depleted regeneration | P: actual native hurt → Regen240 amp1, BA | — |
| 49 | runic_amulet | Shield capacity8 | R: shared pipeline, not individually equipped here | — |
| 50 | runic_amulet_emergency | Capacity7, emergency charge8 | P: actual native depletion charge1→8, BA | — |
| 51 | runic_girdle | Shield capacity10 | R: shared pipeline, not individually equipped here | — |
| 52 | runic_girdle_kinetic | Capacity9, depleted explosion | P: actual native hit damages nearby cow/preserves floor BA | — |
| 53 | discount_ring_aer | Aer discount1% | P: actual menu debit500→495, N | — |
| 54 | discount_ring_terra | Terra discount1% | R: same collector, no matched-aspect debit measured | — |
| 55 | discount_ring_ignis | Ignis discount1% | R: same collector, no matched-aspect debit measured | — |
| 56 | discount_ring_aqua | Aqua discount1% | R: same collector, no matched-aspect debit measured | — |
| 57 | discount_ring_ordo | Ordo discount1% | R: same collector, no matched-aspect debit measured | — |
| 58 | discount_ring_perditio | Perditio discount1% | R: same collector, no matched-aspect debit measured | — |
| 59 | cultist_robe_hood | Armor, warp1, discount1% | R: interfaces/collector inspected, no actual worn debit/event measured | — |
| 60 | cultist_robe | Armor, warp1, discount1% | R: interfaces/collector inspected | — |
| 61 | cultist_robe_legs | Armor, warp1, discount1% | R: interfaces/collector inspected | — |
| 62 | cultist_plate_helm | Armor, repair/runic eligibility | R: item/material and renderer paths inspected | — |
| 63 | cultist_plate | Armor, repair/runic eligibility | R: item/material and renderer paths inspected | — |
| 64 | cultist_plate_legs | Armor, repair/runic eligibility | R: item/material and renderer paths inspected | — |
| 65 | cultist_leader_helm | Armor, repair/runic eligibility | R: item/material and renderer paths inspected | — |
| 66 | cultist_leader | Armor, repair/runic eligibility | R: item/material and renderer paths inspected | — |
| 67 | cultist_leader_legs | Armor, repair/runic eligibility | R: item/material and renderer paths inspected | — |
| 68 | cultist_boots | Armor, warp, discount | R: interfaces/collector inspected | — |

Inventory totals: **46 P items, 22 R items**. P is deliberately partial per the
measured-outcome column. No set bonus is invented for Thaumium: its high
enchantability/repair eligibility is not an independent full-set tick effect.
Fortress's Grinning Devil reduces **event severity**, not permanently acquired
warp; plain Fortress pieces do not promise general warp removal. Void robes'
magic ward is measured by VR: bare/control magic20 versus full-set
`20 × 19 / 35 = 10.857…` health lost.

## Durability and infusion repair

The native T/R tests perform actual anvil extraction and check repaired damage
and XP. `equipmentEnchantmentsAffectNativeGameplay` checks ordinary player ticks:
Repair II sword damage10→8 and Repair I worn boots10→9, with actual finite wand
debits; packed slot9 and offhand stay10. Infusion acquisition is separately
covered by `customEnchantmentInfusionUsesNativeEquipmentAndPayments`; a marker
interface or recipe declaration alone is not reported as a functioning repair.

## Reasoned-only / open limitations

- The 22 R items listed explicitly above: three mundane blanks;
  four non-upgraded runic accessories; five non-Aer discount rings; ten cultist pieces.
- **All armor visuals**, including the recent void-robe fix, are source-inspected
  only. Fabric registers void robes (three), Fortress (three), harness (one);
  vanilla material layers supply ordinary sets. No physical client was run.
- **Possible missing custom cultist models:** `renderKnightArmor` and
  `renderLeaderArmor` exist in `ThaumcraftGearModelsClient` but no Fabric
  `ArmorRenderer.register` caller for them was found. This is a source-only
  candidate, not a measured invisible-armor finding; default material rendering
  may still draw armor. No speculative production change was made.
- Node vision/popups on goggles, upgraded Fortress helm and void hood have
  interface/consumer wiring but no inspected in-world drawing here.
- Native wind/vacuum dispatch, the hoe's fertilize/sapling dispatch,
  each warping item's individual event contribution, non-Aer discounts,
  charged-ring timing, greater-amulet relay charging and harness vis-price application are not newly proven by
  this audit. Shared implementation and direct-helper tests are not substitutes.

## Run record

- Baseline Fortress: 1/1 passed.
- First new trio: 2/3 passed; discount fixture incorrectly used `openMenu` rather
  than the block's extended-menu interaction. Fixture corrected; not a gear bug.
- Pre-fix equipment run: 22/22 passed.
- Repair regression red run: 0/1 passed; **both** crusher and blade logged
  survival49→creative48 before the assertion failed.
- Final post-fix run and exact local evidence: see
  `docs/analysis/2026-10-23-agent-g28gear.md`.
- Post-fix broad run: **25/25 passed**. Final source state, including the extra
  native-use test: **5/5 passed** (26 distinct tests across these two runs).
