# Right-click interaction audit — g4use

Baseline: `96b79de1`; branch `agent/g4use`; 2026-10-23.

**Inspected: 90 native block/item overrides, plus the focus dispatcher and 10 focus callbacks (101 entry points). Two wrong server results proved by failing GameTests; both fixed.** Other concerns below are source-review leads, not additional confirmed bugs. This is not client/multiplayer acceptance.

## Contract and scope

- `S` = `SUCCESS`; `C` = `CONSUME`; `P` = `PASS`; `F` = `FAIL`; `SC` = `sidedSuccess(level.isClientSide)` (client S/server C).
- In block `useItemOn`, `D` = `PASS_TO_DEFAULT_BLOCK_INTERACTION`, `K` = `SKIP_DEFAULT_BLOCK_INTERACTION`; these are **not** interchangeable with item `PASS`. `D` permits default block interaction; `K` skips that stage and permits item use. A consuming block result stops item dispatch; `SUCCESS` does **not** itself execute the held item again.
- `useWithoutItem` is also the default-block stage for nonempty main-hand items. It has no hand parameter; adding an unconditional main-hand gate to every handler would not fix the problem. Running client prediction plus server authority is not, by itself, double-firing. Both hands are legitimate for modern items; they need not all reject the offhand.
- S on a server is legal, not automatically a bug. Intentional refusal/feedback may consume without changing storage. Original oddities are not rewritten simply to force every no-op to P.
- `H` = uses supplied hand; `M` = default main-hand block stage; `U` = reads `getUsedItemHand()` (review lead); `server` = gameplay writes guarded by server-side execution; `both` = no explicit side guard around the indicated writes; `perm` = explicit `mayInteract` in handler/helper; `no-perm` = no such local check found (not proof that native dispatch is bypassable). Read-only diagnostics do not require new build-permission gates.
- `R` in the test/fix column means **reasoned-only, not executed in this audit; no fix**. Existing tests mentioned are navigation pointers, not new pass claims.
- Paths in the table are relative to `common/src/main/java/thaumcraft/common/`. Each row identifies one override, including the nested brain-jar block and the custom spawn egg. Inherited vanilla handlers and retained, uncompiled `src/` are not counted.

## Proven findings

| Finding | Baseline failure | Fix | Regression |
|---|---|---|---|
| Paid Pickaxe of the Core scan returned P | One native `ServerPlayerGameMode.useItemOn` paid **5** durability but returned P | Return SC for a player; retain null-player P. Let client S request the swing instead of manually swinging as well | `interactionPickaxeConsumesPaidScan`: exact C and five wear, unchanged target, main and offhand |
| Paid Earthmover placement returned P | One native call placed **1** stone, spent **1** inventory stone and **1** durability but returned P | Track accepted/paid placement; C after payment, P when no action occurred | `interactionShovelConsumesOnlyPaidPlacement`: no-stock P, exact one block/payment and C, exhausted-stock P, both hands |

These are modern-contract deviations from the original oracle: `ItemElementalPickaxe.java:82-91` returns its superclass boolean after the five-wear scan; `ItemElementalShovel.java:101-171` returns false even after placement. Oracle root: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/items/equipment/`. Values, square order, creative prices, and scan duration are unchanged. No duplicate cast, duplicate debit, client desync, or offhand suppression was demonstrated by these two failures; they prove the wrong return after an effect, not those possible downstream symptoms.

## Complete native override inventory (90)

“Required” describes outcomes, not a claim that all branches have runtime coverage. Current results describe the baseline unless an arrow records the fix.

| # | Handler | Required result per outcome | Baseline/current result | Hand / side / permission review | Test / fix |
|---|---|---|---|---|---|
| 1 | `blocks/BlockAlchemyFurnace.useItemOn` | Open SC; decline D | SC if helper consumes, else D | H menu; server; no-perm in open; `stillValid` gate | R; invalid-menu acceptance lead |
| 2 | `blocks/BlockAlchemyFurnace.useWithoutItem` | Open SC; decline P | SC; sneak/spectator/missing tile P | M; server; no-perm; returns SC even if `stillValid` rejects | R |
| 3 | `blocks/BlockAlembic.useItemOn` | Transfer/label/inspection/rotation SC; unrelated D; denied F | SC/S/C/F/D via helpers | H; server; perm | R; existing `AlembicRuntimeChecks` |
| 4 | `blocks/BlockAlembic.useWithoutItem` | Inspect/clear/remove label SC; unrelated P; denied F | S client, C action server; P/F gates | M explicitly empty; server; perm | R |
| 5 | `blocks/BlockArcaneBore.useItemOn` | Wand/menu helper result, decline D, denied F | Native result mapped including F | H; server tile helpers; permission delegated | R; no extra item invocation after consuming result |
| 6 | `blocks/BlockArcaneBore.useWithoutItem` | Menu SC; missing P; denied F | `top.openMenu`, otherwise P | M; server helper; permission delegated | R |
| 7 | `blocks/BlockArcaneBoreBase.useItemOn` | Wand SC; nonwand D; denied F | `itemResult(onWandUse)` or D | H; server helper; permission delegated | R |
| 8 | `blocks/BlockArcaneDoor.useWithoutItem` | Toggle/refusal feedback SC; wand P | Client S; server C toggle/refusal, P missing/wand | M; server; no-perm locally | R; owner permission distinct from world permission |
| 9 | `blocks/BlockArcaneEar.useWithoutItem` | Pitch/note SC; missing sensor P | Always SC, even missing sensor | M; server; no-perm | R; missing-tile result lead |
| 10 | `blocks/BlockArcanePressurePlate.useWithoutItem` | Setting SC; unauthorized/nonplate P | C for accepted setting on either side; P otherwise | M; both; owner/access gate but no-perm | R; client mutation lead |
| 11 | `blocks/BlockArcaneWorkbench.useItemOn` | Menu SC; decline D | SC or D | H menu; server; no-perm in open | R |
| 12 | `blocks/BlockArcaneWorkbench.useWithoutItem` | Menu SC; sneak/missing P | SC or P | M; server; no-perm in open | R |
| 13 | `blocks/BlockAuraNode.useWithoutItem` | Fixture diagnostic SC; natural/nonempty/missing P | SC/P as specified | M explicitly empty; server read-only message | R; no build-permission gate needed for diagnostic |
| 14 | `blocks/BlockChestHungry.useItemOn` | Open SC; absent D; denied F | SC/D/F | H identity; server; `canOpen` delegated permission plus `mayUseItemAt` | R |
| 15 | `blocks/BlockChestHungry.useWithoutItem` | Open SC; absent P; denied F | `open` returns SC/P/F | M; server; same helper | R |
| 16 | `blocks/BlockCrucible.useItemOn` | Fill/spill or intentional full-container refusal SC; unrelated D; denied F | SC; full server C; D/F | H identity; server; perm | R; C prevents the same bucket from also placing fluid |
| 17 | `blocks/BlockDeconstructionTable.useItemOn` | Menu SC; decline D | SC/D | H menu; server; no-perm in open | R |
| 18 | `blocks/BlockDeconstructionTable.useWithoutItem` | Menu SC; decline P | SC/P | M; server; no-perm in open | R |
| 19 | `blocks/BlockEldritch.useWithoutItem` | Accepted eye/key SC; unrelated/full P | Client always P; server C on insert/unlock, otherwise P | U; server; no-perm | R; stale active-use hand and client prediction leads; existing `EldritchBlockRuntimeChecks` |
| 20 | `blocks/BlockEssentiaReservoir.useItemOn` | Wand/inspection SC; unrelated D; denied F | S/C/partial/F/D mapped from helper | H identity; server; perm | R |
| 21 | `blocks/BlockFocalManipulator.useItemOn` | Open or missing-research feedback SC; decline D | SC/D | H menu; server; no-perm in open | R; research refusal deliberately reports feedback |
| 22 | `blocks/BlockFocalManipulator.useWithoutItem` | Menu/feedback SC; decline P | SC/P | M; server; no-perm in open | R |
| 23 | `blocks/BlockInfusionMatrix.useItemOn` | Delegate wand operation D; denied wand F | D/F; no mutation here | H; perm for wand | R; item performs operation once, not block plus item |
| 24 | `blocks/BlockItemGrate.useWithoutItem` | Toggle SC | SC | M; state server; sounds both; no-perm | R; existing `GrateRuntimeChecks` |
| 25 | `blocks/BlockJar.useItemOn` | Phial/label/inspection SC; unrelated D; denied F | SC/F/D through common helpers | H identity; server; perm | R; transfer helper returns immediately |
| 26 | `blocks/BlockJar.useWithoutItem` | Empty-hand sneak clear/label removal SC; unrelated P; denied F | SC/P/F | M explicitly empty; server; perm | R |
| 27 | `blocks/BlockMagicBox.useWithoutItem` | Menu SC; invalid/missing P | S client before validation; C server open; P invalid | M; server; `stillValid`, no-perm in handler | R; optimistic client acceptance lead |
| 28 | `blocks/BlockMagicWorkbenchCharger.useItemOn` | Delegate interaction SC; unrelated D; invalid F | S/C/partial/F/D mapped from tile | H identity; server validation; permission delegated | R |
| 29 | `blocks/BlockMirror.useWithoutItem` | Preserve deliberate mirror click swallow C | Always C | M; no mutation | R; intentional original block activation, not an inferred bug |
| 30 | `blocks/BlockMirror.useItemOn` | Hand mirror/mirror item must reach item K; others intentionally C | K for those two items, C otherwise | H; no mutation | R; K is crucial for item-link path |
| 31 | `blocks/BlockPedestal.useItemOn` | Transfer SC; empty/no-op D; denied F | SC/D/F | H identity; server; perm; client helper S before empty test | R; optimistic empty prediction lead |
| 32 | `blocks/BlockPedestal.useWithoutItem` | Transfer SC; empty/no-op P; denied F | SC/P/F | M; server; perm; same optimistic client branch | R |
| 33 | `blocks/BlockSpa.useItemOn` | Fill/menu/intentional rejected-fluid swallow SC; unrelated D; denied F | SC; busy C; D/F | H; server; perm for container | R; full/wrong fluid intentionally consumes without payment |
| 34 | `blocks/BlockSpa.useWithoutItem` | Menu SC; decline P; busy consume | SC/P/C | M; server; perm before actual opening | R |
| 35 | `blocks/BlockTable.useItemOn` | Inkwell conversion SC; unrelated D; denied F | SC/D/F | H; server; perm for both halves | R; early inkwell path consumes before item fallback |
| 36 | `blocks/BlockTable.useWithoutItem` | Menu SC; no main/sneak/plain P | SC after broad gate, including no resolved main | M; server; no-perm in open | R; orphan partner result lead |
| 37 | `blocks/BlockThaumatorium.useWithoutItem` | Menu SC; invalid/sneak/spectator P | SC/P; SC even if `stillValid` false | M; server; no-perm in open | R |
| 38 | `blocks/BlockThaumatorium.useItemOn` | Menu SC; decline D | SC/D | H menu; server; same helper | R |
| 39 | `blocks/BlockTube.useItemOn` | Inspection/wand/label/valve SC; unrelated D; denied F | Native result mapped preserving F | H identity; server; perm | R; ordered early returns exclude second item action |
| 40 | `blocks/BlockTube.useWithoutItem` | Label removal/valve SC; unrelated P; denied F | Helper SC/P/F | M; server; perm | R |
| 41 | `blocks/BlockTubeBuffer.useItemOn` | Inspection/wand SC; unrelated D; denied F; core intentionally consumed | S/C/partial/F/D mapping | H identity; server; perm | R |
| 42 | `blocks/BlockVisRelay.useItemOn` | Delegate interaction SC; unrelated D; invalid F | Native helper mapping | H identity; server validation; permission delegated | R |
| 43 | `blocks/BlockWandPedestal.useItemOn` | Transfer SC; focus/empty-ineligible D; denied F | Client S before empty-ineligible check; server C/P-to-D/F | H; server; perm | R; no-op prediction lead |
| 44 | `blocks/BlockWandPedestal.useWithoutItem` | Transfer SC; no-op P; denial F | Converts any consuming result to C; others P (including F) | M; server transfer; perm helper | R; lossy result mapping/client swing lead |
| 45 | `blocks/BlockWoodenDevice.useWithoutItem` | Banner change SC; unrelated P | C accepted on both sides; P otherwise | U; both; no-perm | R; client mutation/stale active-use hand leads |
| 46 | `blocks/ItemArcaneDoor.useOn` | Place SC; unsupported/occupied P | C placement both sides, P gates | H stack; both; no-perm | R; client placement/debit and clicked-position leads |
| 47 | `blocks/ItemMirrorBlock.useOn` | Link SC; already linked/invalid P; otherwise vanilla placement | Client S; server S link, P refusal; otherwise superclass | H; server link; no-perm locally | R; server S alone is not a double-fire proof |
| 48 | `config/ThaumcraftEldritchBlocks` nested `JarBlock.useItemOn` | Brain payout/delay C; other D | C if `emptyBrainJar` handled, else D | H not used for payout; XP server, delay both; no-perm locally | R; shared brain-jar payout helper |
| 49 | `config/ThaumcraftEldritchBlocks` nested `JarBlock.useWithoutItem` | Brain payout/delay C; other P | C/P via same helper | M; XP server, delay both; no-perm locally | R |
| 50 | `entities/ItemSpawnerEgg.useOn` | Spawn/spawner edit SC; failed spawn deliberate consumption policy | Client S; server C even when spawn returns null | H stack; server; no-perm locally | R; failed-spawn no-op lead, not proven |
| 51 | `items/ItemAlumentum.use` | Throw SC; invalid/rejected F | SC/F | H; server; air-use, no block mutation | R |
| 52 | `items/ItemBottleTaint.use` | Throw SC; rejected entity add should not masquerade as throw | Always SC | H; server entity/debit; sound both; add return ignored | R; entity-admission lead |
| 53 | `items/ItemBucketDeath.use` | Pour SC; miss/denied/unplaceable P | S after pour; P on failure | H; `pour` called before side branch; perm target | R; side/prediction review needed |
| 54 | `items/ItemBucketPure.use` | Pour SC; miss P; denied F | Delegates SC/P/F | H identity; server transaction; perm | R; `PureBucketActions` reviewed |
| 55 | `items/ItemCrimsonRites.use` | Study/feedback SC; invalid F | SC/F; already-known SC | H identity; server; air-use | R; intentional study activation not classified merely by storage change |
| 56 | `items/ItemEldritchObject.useOn` | Obelisk placement SC; wrong type/face/blocked P | Client S; server S; P gates | H context; server blocks; no-perm locally; cooldown removal both | R |
| 57 | `items/ItemEssence.useOn` | Transfer SC; unrelated P; denied F | `.result()` from shared phial helper | H; server; perm helper | R; same transaction as early block dispatch |
| 58 | `items/ItemInkwell.useOn` | Table conversion SC; unrelated P; denied F | Helper SC/P/F | H; server; perm both halves | R |
| 59 | `items/ItemJar.useOn` | Alembic transfer SC; otherwise native jar placement result | Helper non-P immediately; else superclass | H; server helper/native placement; perm helper | R |
| 60 | `items/ItemJarLabel.useOn` | Tube/jar/alembic label SC; unrelated P; denied F | First non-P helper, then `.result()` | H; server; perm helpers | R |
| 61 | `items/ItemKey.useOn` | Link/grant/refusal feedback SC; unrelated/not-owner P | S on handled branches both sides; P declines | H; server edits; owner/access logic, no-perm locally | R; feedback can intentionally consume |
| 62 | `items/ItemKnowledgeFragment.use` | Commit SC; invalid/reentrant F | SC/F | H identity; server atomic replacement; air-use | R |
| 63 | `items/ItemLootBag.use` | Paid drop commit SC; invalid F | Client S; server C, including post-payment exception | H identity; server; air-use | R; paid receipt must not fall through/retry |
| 64 | `items/ItemManaBean.useOn` | Plant/intentional occupied-underside acceptance SC; wrong target P; invalid F | SC; server plant C; P/F gates | H identity; server; perm clicked support | R; adjacent-target permission follow-up |
| 65 | `items/ItemNitor.useOn` | Place SC; invalid/blocked F | S client/C server; F gates | H; server; perm target | R |
| 66 | `items/ItemPrimordialPearl.useOn` | Paid node effect SC; nonnode P; invalid F | S client with manual swing; C paid server; P/F | H identity; server; permission in receipt helper | R; manual plus requested client swing lead |
| 67 | `items/ItemResearchNotes.use` | Learn or refusal feedback SC; no data P | SC/P | H stack; server research/debit; air-use | R; no-op already-known outcome needs oracle/UI review |
| 68 | `items/ItemSanitySoap.use` | Start channel C; invalid F | C/F | H identity; starts use both, no direct debit | R |
| 69 | `items/ItemUnknownResearchNotes.use` | Reveal C server/S client; completed valid note delegates | C server, SC client, `super.use` valid completed | H; server reveal; air-use | R |
| 70 | `items/armor/ItemHoverHarness.use` | Open SC; invalid/menu conflict F | SC/F | H menu binding; server; air-use | R |
| 71 | `items/armor/ItemRobeArmor.useOn` | Wash SC; unrelated superclass | Shared wash SC or superclass | H; server water/color writes; no-perm locally | R |
| 72 | `items/armor/ItemVoidRobeArmor.useOn` | Wash SC; unrelated superclass | Same shared wash SC or superclass | H; server; no-perm locally | R |
| 73 | `items/equipment/ItemBowBone.use` | Start draw C; no ammo F | C/F | H; channel state both; no direct arrow/payment | R |
| 74 | `items/equipment/ItemElementalAxe.useOn` | No block operation P | Always P | No mutation | R; permits air-use attraction channel |
| 75 | `items/equipment/ItemElementalAxe.use` | Start channel C | C | H; starts use both; no direct world mutation | R |
| 76 | `items/equipment/ItemElementalHoe.useOn` | Till/grow SC; unchanged P; sneak superclass | SC/P/superclass | H contexts; native till/server grow; `mayUseItemAt` loop, no `mayInteract` locally | R; fallback growth permission and MAINHAND wear leads |
| 77 | `items/equipment/ItemElementalPickaxe.useOn` | Paid scan SC; null player P | **P → SC for player, P null** | H context; native damage/server sound/client overlay; no-perm locally; MAINHAND wear remains | **PROVED; fixed**, `interactionPickaxeConsumesPaidScan` |
| 78 | `items/equipment/ItemElementalShovel.useOn` | Paid placement C server; no placement P; client prediction unresolved | **P after payment → C; no-op P retained** | H/hand wear; server only; perm per destination | **PROVED; fixed**, `interactionShovelConsumesOnlyPaidPlacement`; client still P, not accepted visually |
| 79 | `items/equipment/ItemElementalSword.use` | Start channel C | C | H; channel state both | R |
| 80 | `items/golems/ItemGolemBell.useOn` | Marker edit SC; no selection/wrong dimension/ineligible P; denied F | S client/C server; P/F | H identity; server; perm | R |
| 81 | `items/golems/ItemGolemPlacer.useOn` | Admitted golem SC; sneak P; denied/rejected F | S client/C server; P/F | H identity; server; `mayPlace` helper permission | R |
| 82 | `items/golems/ItemTrunkSpawner.useOn` | Spawn SC; failed add F | S client/C server; F failures | H; server; no-perm locally | R |
| 83 | `items/relics/ItemHandMirror.useOn` | Link SC; wrong mirror P | Client P; server S after link | H; server; no-perm locally | R; prediction asymmetry may allow client air-use after binding |
| 84 | `items/relics/ItemHandMirror.use` | Open view C server/S client; missing link P; broken-link feedback consumes/refuses | SC client, C open; F after link clearing; P otherwise | H; server link mutation/menu | R; F after mutation is intentional refusal candidate, not P fallthrough |
| 85 | `items/relics/ItemResonator.useOn` | Inspect SC; nontransport P; invalid F | SC/P/F | H identity; server diagnostic; explicitly no build-permission gate for free read-only tool | R; helper has no manual client swing |
| 86 | `items/relics/ItemThaumometer.use` | Raise/start scan C, including already-scanned raise | C | H; server session writes, channel both; read-only target | R |
| 87 | `items/relics/ItemThaumonomicon.use` | Open/sync SC; invalid F | SC/F | H identity; server research/network; air-use | R |
| 88 | `items/wands/ItemFocusPouch.use` | Open SC; invalid/current-menu conflict F | SC/F | H menu binding; server; air-use | R |
| 89 | `items/wands/ItemWandCasting.useOn` | First handled operation stops; no match P; deliberate refusal F | First non-P wins; legacy wandable/trigger S or F; formations SC/P/F | H passed to native helpers; side/permission delegated; addon IWandable path has no blanket guard | R; no direct focus call here |
| 90 | `items/wands/ItemWandCasting.use` | Wandable result handled; node channel C; crucible SC; otherwise focus result | Legacy wandable S; crucible SC; node C; focus P/C; permission F | H channel; server world edits; perm node/crucible; legacy wandable callback delegated | R; no claim ordinary focus-on-block is inherently wrong |

## Focus dispatcher and callbacks (11 additional entry points)

All paths below are relative to `items/wands/foci/`. Callbacks return an **ItemStack**, not an InteractionResult; their non-null result is mapped to C by `WandFocusActions.use`. They are listed separately rather than pretending they are native overrides. All are reasoned-only in this audit, no changes.

| # | Handler | Required/current result and operation | Side/hand review | Test/fix |
|---|---|---|---|---|
| 91 | `WandFocusActions.use` | P for missing/inactive/cooldown/reentrant focus or null result; otherwise C | Supplied hand, activation cooldown and player reentrancy gate; callback invoked once per accepted call | R |
| 92 | `ItemFocusExcavation.onFocusRightClick` | Returns wand → C; starts channel | Supplied hand; start-use state both; mining not done in click callback | R |
| 93 | `ItemFocusFire.onFocusRightClick` | Returns wand → C; fireball or starts beam channel | Supplied hand; projectile server, client affordability/swing; cooldown reset for channel | R |
| 94 | `ItemFocusFrost.onFocusRightClick` | Returns wand → C even unfunded; original unconditional swing | Supplied hand; emit server; swing both | R |
| 95 | `ItemFocusHellbat.onFocusRightClick` | Returns wand → C; nonliving/no target also consumes activation | Supplied hand bound on server; client swing only after living target; cast server | R |
| 96 | `ItemFocusPech.onFocusRightClick` | Returns wand → C; emit and swing | Supplied hand; emit server; swing both | R |
| 97 | `ItemFocusPortableHole.onFocusRightClick` | Returns wand → C; server helper ray/payment/hole | Supplied hand identity; perm and server helper; no client world edit | R |
| 98 | `ItemFocusPrimal.onFocusRightClick` | Returns wand → C; emit and swing | Supplied hand; emit server; swing both | R |
| 99 | `ItemFocusShock.onFocusRightClick` | Returns wand → C; earthshock or starts channel | Supplied hand; pulse server, client prediction; cooldown reset for channel | R |
| 100 | `ItemFocusTrade.onFocusRightClick` | Returns wand → C; remember block or enqueue swap | Supplied hand swing; all selection/enqueue work server; actor guard | R |
| 101 | `ItemFocusWarding.onFocusRightClick` | Returns wand → C; delegate ward cast | Supplied hand; server `WardingCasts` | R |

### Wand/focus chain conclusion

`ItemWandCasting.useOn` returns immediately at the first non-P operation. It never directly calls a focus. A successful native block/formation result therefore does not also cast through that item method. If no block handler consumes, `ItemWandCasting.use` can reach `WandFocusActions.use`; this is needed for block-targeted Excavation, Trade, Warding and Portable Hole. Blanket “a focus must never fire when clicking a block” would break intended gameplay. Existing cooldown/reentrancy gates are not proof of network-level exactly-once dispatch. Addon `IWandable` callbacks and failed trigger F/offhand behavior require a dedicated physical-client case before changing their contract.

## Untested leads / explicit limits

- Client writes in the pressure plate, wooden banner and arcane-door item; stale `getUsedItemHand()` in Eldritch/banner default-block methods. These are source concerns, **not newly proven findings**, and remain unfixed.
- Optimistic/no-op client S in pedestal/menu paths, lossy Wand Pedestal result mapping, orphan research-table acceptance, hand-mirror prediction asymmetry, and the pearl's manual client swing need real client tests. No visual “fixed” claim.
- Permission checks missing from several local handlers are recorded above. Native game-mode/packet admission, owner checks and direct helper calls are different boundaries; absence of a local `mayInteract` is not by itself a proven protection bypass.
- Earthmover client `useOn` remains P; this audit fixes the proven **server** paid-result defect only. The placement prediction, rejected-write payment policy, callbacks, and per-placement manual swing are not redesigned. Pickaxe offhand break-slot notification still uses its preexisting MAINHAND constant; tests cover wear before break, not that notification.
- Both new tests invoke real server game-mode dispatch and assert exact return plus resource changes. They do not emulate a physical client's complete two-hand packet sequence or prove a formerly duplicated effect. No no-op/incorrect-hand/client outcome is claimed tested unless specifically stated.

## Reproduction and evidence

Use Java 21; private `GRADLE_USER_HOME=.gradle-g4` (seeded by a private cache copy, no other worktree touched). Delete `fabric/run-gametest/world` before **every** Gradle invocation. Gradle's multi-project configuration mentions NeoForge; no NeoForge task was requested or run.

```sh
rm -rf fabric/run-gametest/world
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/g4use/.gradle-g4
./gradlew :fabric:runGameTest \
  -PgameTestFilter=interactionPickaxeConsumesPaidScan,interactionShovelConsumesOnlyPaidPlacement \
  -PgameTestHeap=768M -Dorg.gradle.jvmargs='-Xmx768M -Dfile.encoding=UTF-8' --no-daemon
```

Baseline with only regression tests (`a6f84396`, gameplay still `96b79de1`): **2/2 fail** at the intended exact-result assertions after resource/effect assertions passed. Report timestamp `2026-10-22T20:59:05.531306763Z` (machine local Oct 23); failures: `paid native scan must stop fallback, not return PASS in MAIN_HAND` and `paid native placement must stop fallback, not return PASS in MAIN_HAND`. Full local log `.gradle-g4/baseline.log`; normal ephemeral XML `fabric/build/gametest-focused-results.xml` is overwritten by subsequent runs.

Post-fix validation is recorded in `docs/analysis/2026-10-23-agent-g4use.md`.
