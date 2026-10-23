# Boss fight audit — g23boss (2026-10-23)

**Final verification:** Fabric production build passed; **128 focused eldritch unit tests
and 8/8 dedicated-server GameTests passed** after all four fix categories. Evidence:
`.tmp/g23-final-verified.log`, `common/build/test-results/test/`,
`fabric/build/gametest-focused-results.xml`. These are focused checks, not the full suite.

## Scope and evidence

**Six requested entity types inspected and driven from finalized spawn through lethal damage.**
`BossFightRuntimeChecks.fight` uses real registered entities in a Fabric dedicated-server
GameTest, seven-point player-attributed hits every 20 native entity updates, and
`ServerLevel.tickNonPassenger` for spawn protection, regeneration, phase and death updates.
No health, phase flags, damage cooldowns or internal counters are set by the test.
The fixture declares a diamond sword, stationary/no-gravity mob positioning and a remote
survival mock player. This is a damage-to-death integration test, **not** an actual
player-attack-input replay, survival duel, natural encounter or client acceptance.
The ordinary and giant taintacles are outside their natural tainted biome in this fixture;
their native environmental damage also runs. Pech, ordinary taintacle and giant taintacle
are not members of the original `EntityThaumcraftBoss`/boss-display family.
The final regression fixture declares the join champion draw: Bold for original bosses,
ordinary for the other mobs. Praetor spawn equipment/title/modifier still use native draws.

Specification read directly from the private original 4.2.3.5 oracle under
`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`:
`EntityThaumcraftBoss:48-50,216-219`, `EntityEldritchGolem:192-208`,
`EntityEldritchWarden:301-319`, `EntityCultistLeader:170-183`,
`EntityTaintacleGiant:98-102,118-136`, `EntityPech:368-387`,
`EventHandlerWorld:225-244`, `EntityUtils:181-191`.

## PROVEN before fixes

| Entity (6 total) | Driven / observed | GameTest | Finding / fix |
|---|---|---|---|
| Eldritch golem | Spawn protection; damage down to headless phase; second phase dies | `g23BossGolemFight` | **FAIL:** no pearl. Death lookup uses unregistered old metadata-family ID. Resolve original metadata 3 as native `primordial_pearl`. |
| Eldritch warden | Spawn ward depleted; temporary frenzy rejects hits; later dies; inspect actual frenzy blocks and a victim collision/expiry callback | `g23BossWardenFight` | **FAIL:** no pearl (shared fix). **FAIL:** frenzy places nitor metadata 1, not field metadata 11. Resolve native `airy_hole`/`variant`. **FAIL after mapping correction:** the field neither slows/weakens a victim nor expires on its scheduled tick; restore the metadata-11 hazard lifecycle. |
| Cultist leader / praetor | Armor-bearing finalized spawn, repeated damage, rare bag, death removal | `g23BossPraetorFight`, `g23BossPraetorBar` | Fight/loot pass. **FAIL:** no server boss event exists after tracking callback. Restore original inherited boss display contract using native server boss event. |
| Taintacle | Repeated damage, original goo/tendril drop, death removal | `g23BossTaintacleFight` | PASS; no new gameplay fix. No original health phase or boss bar to invent. |
| Giant taintacle | Repeated damage reaches death | `g23BossGiantFight` | **FAIL:** last giant drops no pearl. Same lookup/fix. No original boss-display bar to invent. |
| Pech | Repeated damage, death removal | `g23BossPechFight` | PASS kill/removal. Probabilistic loot is **not** exhaustively validated by this fight. No original boss bar/health phase. |

Baseline run: **8 tests, 4 failures**: golem/warden/giant pearl checks and praetor
boss registration. All six reached lethal damage; no phase/killability failure was observed.
Ignored evidence: `.tmp/g23-reproduced.log`, `.tmp/g23-reproduced.xml`.

After the pearl and praetor-bar fixes, all **8/8** selected cases passed in the
Fabric dedicated server (`.tmp/g23-all-fixed.log`). Praetor callbacks register/unregister
viewers, update name/health, and clear viewers on death/removal. The final fight checks also
exercise warden drown/wither immunity, golem fire immunity/headless beam charging and giant
heavy-hit enrage/capping. No gameplay phase threshold or damage immunity was changed.

A stronger subsequent warden check failed against that intermediate version:
`warden frenzy creates original damaging eldritch-field rings (metadata 11), observed=[1]`
(`.tmp/g23-field-before.log`). The resolver queried nonexistent property `type` on the
ordinary airy group and returned its nitor default. Metadata 11 belongs to `airy_hole`,
whose actual property is the shared sparse `variant` property. Oracle warden:197-229
requires metadata 11 for both the footprint and frenzy rings.

The corrected field identity exposed missing block behavior. A real ring field's native
collision and scheduled-tick callbacks yielded `collision=false, expired=false`
(`.tmp/g23-hazard-expiry-before.log`). Original `BlockAiry:384-415` requires 0.66
horizontal drag, Weakness II for 100 ticks, player exhaustion 0.05, 1% wither damage 1,
eldritch immunity and field removal on a block tick. Collision/expiry callback tests are
not elapsed-world-time acceptance; only the native scheduler registration is asserted.
An extended pre-fix run also observed `randomTicking=false`
(`.tmp/g23-field-lifecycle-before.log`). The native hole group is now eligible for random
ticks, and metadata 11 has both scheduled/random expiry handlers. The regression checks
original ambient Weakness II duration, exact horizontal drag, player exhaustion, eldritch
ally exemption and both expiry callbacks. The 1% wither roll is implemented from the
oracle but its probability distribution is not claimed tested.

### Count interpretation

- **6** requested types driven through scripted spawn-to-death checks.
- **3** distinct types failed a phase/drop check: golem, warden and giant taintacle.
- **1** additional type failed boss-bar registration: praetor.
- **4 PROVEN fix categories:** shared pearl lookup, praetor bar, warden field identity,
  metadata-11 collision/expiry lifecycle. The warden has overlapping findings; do not
  count it as several additional bosses. No permanent unkillability was established.

### Placement lock: suspicion rejected, no gameplay change

`g23BossDeathPlacement` confirms live praetor detection, detection during its death
animation, and no detection after native death removal. The oracle helper performs an
entity-box query without an alive-health predicate. Keeping the lock for the death animation
is therefore original behavior, not a proven permanent lockout. This test exercises the
entity search, **not** the Fabric placement cancellation hook or a generated Outer Lands cell.

## Reasoned-only / incomplete acceptance

- Generated structure → lock activation → arena replacement → encounter start → victory
  has **not** been replayed. `TileEldritchLock`/`OuterLandsBossRooms` were inspected only.
- Actual Fabric single/multi-block placement cancellation in an Outer Lands boss cell,
  inventory preservation on rejection, creative/spectator policy and combat access remain
  unverified. No claim that the placement rule cannot lock out a real player.
- Nearby-player automatic tracking, client boss-bar rendering and actual multiplayer
  departure/death packets require connected-client acceptance. The bar test drives native
  tracking callbacks and inspects the real `ServerBossEvent`; it is not a screenshot claim.
- Pech loot probabilities/inventory payout, multiple-giant last-survivor reward, all champion
  variants, elapsed-world-time field decay and encounter-specific cleanup are not exhaustively
  tested. Field tick registration and actual expiry callbacks were tested, not their wall clock.
- A normally equipped player winning while taking real boss attacks remains unverified.
  The fixture proves normal-sized incoming damage can win, not survival balancing.
- NeoForge not built or run.
