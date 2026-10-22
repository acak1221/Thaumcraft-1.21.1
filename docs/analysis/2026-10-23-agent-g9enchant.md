# g9enchant — enchantment/infusion side-effect hypothesis

Worktree `/home/cyberfox-kali/wt/g9enchant`, branch `agent/g9enchant`.
First requested command succeeded with clean status and baseline `7008358c`.
No other worktree or primary repository was read/modified. Oracle was read-only at
`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`.

## Scope and discoveries

See `docs/audit/ENCHANT_FINDINGS.md` for all38 tabulated effect entries and the
separation of behavioral proof from reasoned-only source inspection.

1. Modern enchantments are already registry-data definitions plus explicit living-tick
   dispatch. There are no obsolete modern Enchantment-subclass overrides to reconnect.
   Frugal/Potency/Wand Fortune are also inert in the original, not missing combat effects.
2. Champion damage effects wrap LivingEntity.actuallyHurt but Player overrides it.
   `javap -c -p` against the lane's mapped 1.21.1 jar confirms Player performs its own
   armor/magic/absorption/health logic and never calls LivingEntity.actuallyHurt.
   Therefore six attack modifiers skip players, and Warped cannot affect any intended
   victim (only players qualify for its warp).
3. Crafting warp dispatch uses two nonexistent old-looking IDs: brain_jar and
   sinister_loadstone. Registered outputs and real infusion recipes use jar_brain and
   compass_stone. Oracle ConfigResearch declares one sticky warp for each.
4. The Warded champion's baseline source/tests incorrectly interpret func_111125_b as
   total max-health value. Oracle EntityUtils:272 and ChampionModWarded:26 read base
   health. The retained legacy mapping independently calls getBaseValue. A spider's
   ward is8, not23; champion's common +30 health remains unchanged.
5. Original invalid-center infusion executes one hazard THEN aborts. Hazard poison,
   exhaustion, warp, explosions and ejection are intentionally possible on failure;
   success-only output/crafting callback must not execute there.
6. Public `ThaumcraftApiHelper` routes through `ThaumcraftInternalMethods`, not the
   separately implemented `InternalMethodHandler.INSTANCE`. Its warp methods bypass
   original negative-permanent guards, server-side filtering, knowledge sync and warp
   messages. A dedicated API-trigger test observes permanent3→2 from a -1 request;
   original Thaumcraft:309-310 must refuse it. The two methods now delegate to the
   existing guarded implementation rather than copying its policy yet again.

## Behavioral fixtures

- Real ServerPlayer.hurt from a promoted vampiric zombie: exact +2 healing of attacker,
  rejected repeat no healing; ordinary pig victim checks living-hook non-duplication.
- Real native player hits assert poison100, wither200, hunger500, fire80 ticks, and
  declared original random chance negatives. A declared nextInt3=1 produces exactly2
  temporary warp; immunity and failed roll award none.
- Two genuine infusion recipes consume declared finite jars/components, including
  water-bucket remainder. No synthetic recipe or completion-method call. Tests check
  one sticky warp on success, no replay, no admission award and no completion award
  after an invalid-center abort (declared non-warp hazard9).
- Native ADD promotion of a Warded spider asserts health46/base16/absorption8 and native
  tick25 regeneration+1, never beyond8 and never on tick26.

Fixtures are not a complete survival acquisition replay or visual acceptance.

## Commands / outcomes

All Gradle invocations use Java21 and private `GRADLE_USER_HOME=$PWD/.gradle-g9`.
Every invocation first removes only `fabric/run-gametest/world`.
The initial cold-cache invocation exceeded the tool's120-second timeout during setup;
it is not a test result. Subsequent background invocations use no timeout.

Observed baseline results:

| Test | Baseline failure |
|---|---|
| champion_effects_reach_player_victims_once | One accepted player hit leaves vampiric attacker health10, expected12 |
| champion_warp_reaches_player_victim_once | Actual temporary-warp delta0, expected2 |
| infusion_jar_brain_awards_warp_once | Genuine physical success output exists, crafting sticky-warp delta0, expected1 |
| infusion_compass_stone_awards_warp_once | Genuine physical success output exists, crafting sticky-warp delta0, expected1 |
| champion_ward_uses_original_base_health | Native promoted spider ward23, expected8 |
| public_warp_api_preserves_original_guards | Public API accepts negative permanent request, resulting2 instead of3 |

The first two-test run was **rejected as proof**: newly joined players still had vanilla
login damage protection, and the first assertion said the hit was not accepted. The
fixture now advances61 actual ServerPlayer ticks to expire that protection before
attacking. Re-running against the unchanged ChampionHurtMixin produces the valid
health10/warp0 failures above, after accepted native damage. No protection field is
patched, no effect method is invoked directly.

Evidence logs (ignored): `.tmp-g9-red.log` (rejected fixture), `.tmp-g9-red2.log`
(infusion/ward baseline), `.tmp-g9-red3.log` (valid champion baseline). XML snapshots:
`.tmp/g9-infusion-ward-red.xml`, `.tmp/g9-champion-valid-red.xml`.

`a799d848` adds behavioral audit fixtures; `09656086` corrects the two crafting IDs;
`4cd7f94b` targets the native player damage override and expires fixture login protection;
`80e2b5a3` corrects initial ward and regeneration ceiling, including the old incorrect tests.

First fixed run: `:common:test :fabric:runGameTest` with the five new tests, three original
champion tests and original equipment-enchantment test passed. **2490 unit tests,0
failures/errors/skips;9 GameTests,0 failures**. Log `.tmp-g9-green.log`, XML
`.tmp/g9-green-nine.xml`. The player test includes exact native non-player healing to
guard against double invocation from the two mixin targets.

Public API baseline subsequently failed at exactly the negative-permanent assertion
(`.tmp-g9-api-red.log`, `.tmp/g9-api-red.xml`). `20449ce2` fixes its dispatch.

## Final validation

Command (after the same world deletion and Java/private-Gradle-home exports):

```sh
./gradlew :common:test :fabric:build :fabric:runGameTest \
  -PgameTestFilter=champion_effects_reach_player_victims_once,champion_warp_reaches_player_victim_once,infusion_jar_brain_awards_warp_once,infusion_compass_stone_awards_warp_once,champion_ward_uses_original_base_health,public_warp_api_preserves_original_guards,champion_promotion_uses_the_original_bonuses,champion_modifiers_answer_their_tick,champion_modifiers_answer_their_hit_and_wound,equipmentEnchantmentsAffectNativeGameplay,infusionAltarFormsAndExecutesAllInstabilityOutcomes,enchantmentInfusionPaysLevelsBeforeEssentiaAndRaisesActualCentre,customEnchantmentInfusionUsesNativeEquipmentAndPayments,infusionConsumesDistilledEssentiaAndPersistsSession \
  -PgameTestHeap=1536M --no-daemon --max-workers=2 -Dorg.gradle.jvmargs=-Xmx768m
```

**BUILD SUCCESSFUL** in1m18s. **14 GameTests passed;2490 common unit tests passed**,
no failures/errors/skips. Dedicated GameTest server saved/shut down normally. Fabric
production JAR remapped successfully; `verifyProductionIsolation` found no test harness.
Final log `.tmp-g9-final.log`, focused XML `fabric/build/gametest-focused-results.xml`,
unit XML `common/build/test-results/test/`. All are ignored runtime outputs.

Totals:38 tabulated entries inspected;8 effects unwired (six champion-player effects,
two crafting awards);2 further incorrect behaviors (ward magnitude, public API guard).
Six explicit baseline-failing tests passed after fixes. Four other attacking modifiers'
individual baseline failures remain reasoned from the shared override gap, though their
exact post-fix native effects passed. The complete reasoned-only list remains in the
audit; no exhaustive every-action, client, multiplayer or NeoForge claim is made.
