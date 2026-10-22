# Enchantment / infusion / champion audit — g9enchant

Baseline: `7008358c`; Fabric 1.21.1 only. Oracle read-only:
`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`.

## Evidence vocabulary

**PROVEN** requires an observed failing baseline GameTest, then a passing fixed run.
**Reasoned** means source/oracle inspection, even when an older test already exists.
Existing test names below are coverage pointers, not claims that this lane executed them.

## Enchantments (5 inspected)

The compiled modern tree contains **zero Enchantment subclasses**: 1.21.1 definitions are
native registry JSON. The five similarly named classes are helpers, not dead overrides.
Legacy `src/` is not the runtime tree.

| Enchantment | Hook / original effect | Test / status | Fix |
|---|---|---|---|
| Repair | `PlayerEnchantmentTickMixin` → `EquipmentEnchantments.playerLivingTick`, server every40 ticks, hotbar+armor; level1/2 durability with primal centivis payment | `equipmentEnchantmentsAffectNativeGameplay` passed native repair/payment assertions; no baseline defect | None |
| Haste | Same living `aiStep` hook before travel, both sides; forward non-flying boots acceleration, not mining speed | Same test (its Haste assertions call helper directly); reasoned native wiring | None |
| Frugal | Original definition/helper exists but no accepted focus and not instantiated by original Config; not a vanilla break/damage override | `WandEnchantmentRulesTest`, `CustomEnchantmentInfusionRuntimeChecks`; reasoned intentionally inert | Do not invent an effect |
| Potency | Original focus **upgrade**, not this enchantment, supplies power; enchantment multiplier1 | Same; reasoned intentionally inert | None |
| Wand Fortune | Original treasure focus **upgrade**, not this enchantment, supplies block fortune; extra drops0 | Same; reasoned intentionally inert | None |

Original references: five `common/lib/enchantment/Enchantment*.java`, `Config.java:274-281`,
`EventHandlerEntity` living-update hook. Modern helper has no vanilla damage/loot override
to reconnect; hitting with a sword would not be a valid trigger for any of these five.

## Infusion side effects (11 inspected)

| Effect | Reachability / original value | Test / status | Fix |
|---|---|---|---|
| Central-item component/NBT patch | `InfusionStackRecipe.assemble` prepares copy; `finishCrafting` writes it once; no early center write | `InfusionResultPatches`, infusion tests; reasoned | None |
| Enchantment level write | `finishCrafting` → `InfusionEnchantmentRecipe.raise`, current center level+1 retaining its current components | `enchantmentInfusionPaysLevelsBeforeEssentiaAndRaisesActualCentre`; reasoned | None |
| Pedestal debit / container remainder | Entire matched stack replaced once after six successful cycles; water bucket returns empty bucket | New jar-brain fixture passed finite-debit/remainder assertions | None |
| Essentia payment | One unit per successful cycle before ingredients; zero-valued keys retained | `infusionConsumesDistilledEssentiaAndPersistsSession`; reasoned | None |
| XP drain / damage | Enchantment phase first, one nearby player's whole level per cycle; magic damage nextInt2; no start debit | Enchantment infusion test; reasoned | None |
| Instability accumulation | Initial symmetry+recipe instability, shortage rolls cap25; server matrix tick only | Existing infusion tests; reasoned | None |
| Harm (“poison”) | Events5/16 first living victim,18 all; taint120 ticks OR uncurable vis exhaustion2400, amplifier0 | `infusionAltarFormsAndExecutesAllInstabilityOutcomes`; reasoned | None |
| Zap | Events3/8/14 first victim,12 all; magic4..7 | Same; reasoned | None |
| Warp hazard | Event20 one random nearby player; 25% sticky1, otherwise temporary1..5; server-only | Same; reasoned | None |
| Explosion | Event9 power1.5+float, eject5 power1; entities damaged but terrain unchanged | Same and invalid-center test; reasoned | None |
| Ejection / flux | Original21-event switch, one selected nonempty pedestal, drop/destroy whole stack; goo/gas metadata7 | Same; reasoned | None |

**Important negative-case correction:** oracle `TileInfusionMatrix:349-419` explicitly
executes one hazard when the center is invalid, then aborts. Poison/warp/hazards are **not**
success-only rewards. Completion output and crafting-warp callback must not run on abort;
already paid XP/essentia/components are not refunded. No change made to this original quirk.

## Crafting warp (8 item effects inspected)

Oracle `ConfigResearch:82,179,237,262,265,324,413,416` declares exactly these eight
one-sticky-warp outputs. `EventHandlerWorld:153` awards once per crafting event, not per
output count. Modern callback is registered once in `Thaumcraft.init`; matrix completion
invokes it after setting crafting=false. No extra per-tick crafting-warp award was found.

| Native output | Baseline routing | Test / status | Fix |
|---|---|---|---|
| `focus_hellbat` | Matching case, sticky1 | Source inspection; reasoned | None |
| `compass_stone` | Wrong case `sinister_loadstone`, drops award | `infusion_compass_stone_awards_warp_once`; **PROVEN** delta0→1, refusal/abort/replay negatives passed | Use registered ID (`09656086`) |
| `jar_brain` | Wrong case `brain_jar`, drops award | `infusion_jar_brain_awards_warp_once`; **PROVEN** delta0→1, refusal/abort/replay negatives passed | Use registered ID (`09656086`) |
| `bucket_death` | Matching case, sticky1 | Reasoned | None |
| `bottle_taint` | Matching case, sticky1 | Reasoned | None |
| `golem_flesh` | Matching case, sticky1 | Reasoned | None |
| `focus_primal` | Matching case, sticky1 | Reasoned | None |
| `staff_rod_primal` | Matching case, sticky1 | Reasoned | None |

## Champions (13 modifiers inspected)

Join path: `Thaumcraft` → `ChampionWiring.init` → single Architectury ADD listener;
NOT_ROLLED guard prevents rerolls. Promotion adds30 max health, doubles attack, heals25;
Bold multiplies speed1.3 and Mighty additionally multiplies damage3 (total6).
These values are covered by existing `champion_promotion_uses_the_original_bonuses`.

| Modifier | Original effect / baseline hook | Test / status | Fix |
|---|---|---|---|
| Bold | Promotion speed×1.3; no per-hit effect | Promotion test; reasoned | None |
| Spined | Defensive hurt hook, reflects1..3, excludes thorns recursion | Existing champion rules; reasoned | None |
| Armored | Defensive hurt hook, damage×19/25 unless bypass armor | Existing champion rules; reasoned | None |
| Mighty | Promotion damage×3 beyond common×2 | Promotion test; reasoned | None |
| Grim | Attack hook,40% wither200 ticks | Baseline gap reasoned; exact fixed runtime value passed | Wrap player override (`4cd7f94b`) |
| Warded | Promotion ward and tick25 regen ceiling use half **base** max health, not boosted total | `champion_ward_uses_original_base_health`; **PROVEN** ward23→8, native regen+1/ceiling/non-cadence passed | Correct attribute read (`80e2b5a3`) |
| Warped | Attack hook,33% temporary1..3 to player victim | `champion_warp_reaches_player_victim_once`; **PROVEN** delta0→2, rejected-hit/failed-roll negatives passed | Wrap player override (`4cd7f94b`) |
| Undying | Mob tick, heal1 every20 | Existing champion rules; reasoned | None |
| Fiery | Attack hook,40% fire4 seconds | Baseline gap reasoned; exact fixed runtime value passed | Wrap player override (`4cd7f94b`) |
| Sickly | Attack hook,40% hunger500 ticks | Baseline gap reasoned; exact fixed runtime value passed | Wrap player override (`4cd7f94b`) |
| Venomous | Attack hook,40% poison100 ticks | Baseline gap reasoned; exact fixed value/failed-roll/wrong-entity negatives passed | Wrap player override (`4cd7f94b`) |
| Vampiric | Attack hook, heal attacker max(2,damage/2) | `champion_effects_reach_player_victims_once`; **PROVEN** attacker10→12, rejected-repeat/non-player single application passed | Wrap player override (`4cd7f94b`) |
| Infested | Defensive hurt hook,40% one tainted spiderling | `champion_modifiers_answer_their_hit_and_wound`; reasoned | None |

`ChampionHurtMixin` only targets LivingEntity at baseline. Player overrides actuallyHurt
without calling super, skipping all six attacking modifiers against players. This does
not imply effects should fire for immune/rejected hits; tests include hurt-immunity,
failed random rolls and creative immunity.

Warded oracle: `EntityUtils:272` and `ChampionModWarded:26` both read SRG
`func_111125_b` (**getBaseValue**). The retained legacy source corroborates the mapping.
The baseline modern comments/tests claiming the boosted46-health spider's ward is23 are
not authority: a base16 spider's original ward is8. Absorption, not legacy's later
health-healing regression, remains the original effect.

## Additional warp paths

### Public warp API (1 additional inspected effect path)

| Effect | Baseline routing | Test / status | Fix |
|---|---|---|---|
| Public `ThaumcraftApiHelper` warp grants | API default `ThaumcraftInternalMethods` bypasses the guarded `InternalMethodHandler`; negative permanent warp reduces3 to2 | `public_warp_api_preserves_original_guards`: **PROVEN** baseline2→fixed3; positive permanent/temporary/sticky amounts and counter passed | Delegate both methods to guarded handler (`20449ce2`); server-only/sync/message wiring restored, not client-observed |

### Remaining call-site inspections (not replayed)

Inspected call sites: research completion and book siblings (new completion guard,
permanent ceil(warp/2)+sticky floor(warp/2)); brain eating (10% sticky1 else temporary1..3);
eldritch trap (50% temporary1..2); guardian scream (temporary1..3); warden scream
(temporary3..5). No second award hook was found in these inspected paths. Their every-action
runtime exactness is **not proven by this lane**. Administration/cheat-sheet, portal
research grants, Crimson Rites, cultist-leader warp and warp-event decay/soap reductions
were located, not exhaustively replayed; no correctness claim is made for them.

## Execution / totals

Inspected table entries: **38** =5 enchantments +11 infusion effects +8 crafting-warp
effects +13 champion modifiers +1 public warp API path. The other warp
families above are additional reasoned call-site inspections, not counted as individual
exhaustively verified effects. No visual, NeoForge or complete-survival acceptance claim.

Observed so far: **8 unwired effects** (six attacking champion modifiers against players,
two crafting awards), plus the incorrect Warded amount and API negative-permanent guard.
Six baseline-failing cases now pass: Vampiric, Warped, jar-brain, compass-stone, Warded,
public warp API.
The four other attacking champion effects have exact post-fix native runtime assertions,
but their individual baseline failure is reasoned from the same override gap, not separately
replayed. Final validation: **14/14 focused Fabric GameTests,2490/2490 common unit tests,
`:fabric:build` and production test-harness isolation all passed**. The dedicated
GameTest server shut down and saved all dimensions normally. Four production-fix commits
are separate from the initial regression-fixture commit. No mod enchantment was found
unwired: three historically inert enchantments were deliberately not given new powers.

Valid baseline failure messages:

- Vampiric: accepted native hit leaves attacker health10 instead of12.
- Warped: temporary-warp delta0 instead of2.
- Jar-brain/compass-stone: physical infusion success with sticky-warp delta0 instead of1.
- Warded: native promoted spider ward23 instead of8.
- Public API: negative permanent grant changes3 to2 instead of remaining3.

The first champion run was rejected as evidence because vanilla login damage protection
rejected the hit. The corrected fixtures expire it through61 native ServerPlayer ticks;
both champion tests were rerun against the unchanged baseline damage mixin and failed on
the intended effect assertions. See the analysis note for logs and commands.

## Reasoned-only / not exhaustively replayed

- Native Haste dispatch (its arithmetic is runtime-tested via helper); intentional original
  inactivity of Frugal/Potency/Wand Fortune.
- Individual baseline failures of Grim/Fiery/Sickly/Venomous; all four exact fixed native
  player-hit values were observed, but the combined baseline test stopped at Vampiric.
- Spined/Armored/Undying native exact-once effects, and every spawn species/roll combination.
  Promotion bonuses and Warded/Venomous/Infested native paths did run in the final selection.
- All remaining crafting-warp outputs and research/brain/trap/guardian/warden warp action
  exactness, plus administration/cheat-sheet/portal/Crimson Rites/cultist-leader/decay paths.
- Every component-patch recipe through full physical infusion, client-only authority
  negatives, packet delivery/UI feedback, visual and multiplayer acceptance.
