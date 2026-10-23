# Focus casting audit — g31foci, 2026-10-23

## Scope and evidence

**10 registered foci inspected and actually cast in fresh Fabric dedicated-server GameTests. One PROVEN defect:** Pech's Curse repeats its impact damage attempt when a native hurt callback synchronously reenters that same projectile's tick. This is not a normal two-projectile launch or a client/server double-fire claim.

The existing real-wand runtime cases were rerun, not inferred from the earlier upgrade-matrix lane. New cases are `g31FocusCastingLaunchesAndPlayerCooldowns` and `g31PechCastImpactCannotReenter`, implemented in `common/src/gameTest/java/thaumcraft/common/testing/FocusCastingAuditChecks.java`. The latter launches through real native item use and places a real cow on the unmodified launch ray: no manual hurt, impact invocation or projectile motion replacement.

### PROVEN observations (not ten defect claims)

Test names below are methods in `fabric/src/gameTest/java/thaumcraft/fabric/testing/ThaumcraftGameTests.java`.

| # | Focus | Actual cast and measured output | Runtime test(s) | Fix |
|---|---|---|---|---|
| 1 | Fire | Basic RMB starts use without shooting; one paid held tick emits **2** embers. 32 paid ticks emit **64**. Fireball emits **1** orb; its collision intentionally has one direct hit **plus** one explosion. | `fireFocusSelectsPersistsAndPays`; `g31FocusCastingLaunchesAndPlayerCooldowns`; `combatFocusSpecialisationsKeepOriginalCastingAndProjectiles` | None; canonical multi-output, not double-fire. |
| 2 | Frost | Basic RMB emits **1** shard. One Potency-II Scattershot emits **9**; two separately collided fragile shards deal **2 total** damage. | `frostFocusCraftsSelectsAndPays`; launch audit; combat specialisations; `frostShardRicochetsDamagesAndPersists` | None. Ricochets and later impacts are intentional. |
| 3 | Shock | One basic eligible pulse deals **4** damage; replay during cooldown adds **0**. Chain fixture hits **1 primary + 6 distinct secondary** targets. Earthshock emits **1** orb. | `shockFocusPaysTargetsAndRespectsCooldown`; combat specialisations | None. A missed basic pulse deliberately pays vis. |
| 4 | Excavation | Completed base attempt removes **1** target block for **15 terra centivis**; cancellation produces **1** native break event, **0** drops and no duplicate completion on next tick. | `excavationFocusChannelsAndPaysCompletedAttempts`; launch audit (native no-target use) | None. |
| 5 | Warding | Basic cast wraps **1** original block for **25 terra + 25 ordo + 10 aqua centivis**. Architect fixture wraps **9**, paying exactly nine prices. Immediate same-coordinate replay changes **0** additional blocks. | `wardingPreservesOwnersStatesAndArchitectPayments`; launch audit (native no-target use) | None. |
| 6 | Portable Hole | One 12-deep wall cast creates **108** hole blocks, later restores them. Measured reach fixtures create **298** basic / **370** Enlarge-I holes, including the original terminal centre-cell quirk. | `portableHoleCutsTunnelsAndGivesBlocksBack`; launch audit (native no-target use) | None. One effect is a tunnel, not one changed block. |
| 7 | Equal Trade | Focus left-click swaps **1** ore for **1** material and **1** ordinary coal drop. Queue replay adds **0** debit/drop. Right-click flood changes **26** tracked cells; Architect changes **9**, with one price per changed cell. | `tradeSwapsPayOnceAndRespectArchitectAndProtection`; launch audit (native no-target use) | None. |
| 8 | Hellbat | One targeted RMB summons **1** bat; immediate replay summons **0** more. Occluded, out-of-range and unfunded attempts produce **0**, preserving wand. | `hellbatFocusUsesOriginalTargetPaymentAndUpgradeGates`; launch audit (native no-target use) | None. This is a summoned mob, not a projectile. |
| 9 | Pech's Curse | One RMB emits **1** blast. Reentrant native collision callback caused **2 damage attempts**, though native immunity limited health loss to **2**. After fix: **1 attempt**, **2 health lost**, **1** base status effect. Nightshade's three simultaneous statuses are intentional. | `pechOriginalCurseUsesNativeGameplay`; launch audit; `g31PechCastImpactCannotReenter` (**red → green**) | Guard synchronous impact reentry in `EntityPechBlast`, including nested ticks. |
| 10 | Primal | One RMB emits **1** orb, pays one six-aspect quotation; immediate replay emits **0** more; empty-wand activation emits **0** and preserves stack. | `primalFocusPaysActualVisAndUsesSeeker`; launch audit | None. |

The five target-dependent focus types (Excavation, Warding, Portable Hole, Equal Trade, Hellbat) are also cast through `ServerPlayerGameMode.useItem` into empty sky in the new audit: **0 debit and 0 projectiles** each. Basic Shock is deliberately not classified as target-required: the original pays before ray resolution. Empty Fire, Frost, Pech and Primal actual casts are checked for **0 projectiles and exact stack preservation**; existing cases additionally cover targeted tool and Hellbat affordability refusals.

## Measured launch and scatter

The audit reads positions and velocity vectors from actual spawned entities, not scatter constants. Original muzzle position is asserted, including Fire's advance by its final scattered velocity before insertion. First real projectile flight ticks for Fire/Frost/Pech/Primal survive, move forward, and leave caster health unchanged.

The final unseeded 14-case run produced:

| Sample | Actual count | Speed range (blocks/tick) | Angular RMS from aim |
|---|---:|---:|---:|
| Basic Fire, one tick | 2 | 0.97322–1.13215 | 6.70175° |
| Basic Frost | 1 | 1.49151 | 0.16823° |
| Pech | 1 | 1.48632 | 0.67598° |
| Primal | 1 | 0.49979 | 0.45816° |
| Basic Fire, 32 paid ticks | 64 | 0.69460–1.25376 | 8.01928° |
| Firebeam, 32 paid ticks | 64 | 0.99579–1.00554 | 0.68131° |
| Frost Scattershot, Potency II | 9 | 1.33461–1.66299 | 5.82717° |

These are measured samples, not exact distribution guarantees. No RNG seed or clock replacement is used. The sample loops invoke real player ticks synchronously and deliberately do **not** age the projectiles between samples: this measures launch spread rather than lifetime occupancy.

## Cooldown interpretation

The original dispatcher shares a real-millisecond deadline across a player's foci, not a per-world timer and not independent native item cooldowns. Fire/Excavation/Portable Hole/Warding/Trade have no dispatcher delay; Warding and Trade also own their coordinate/job protections. The new test casts a second player's Frost in the same real world while the first player's Scattershot deadline remains positive. Existing Shock testing waits for actual **250 ms** expiry, rather than treating accelerated GameTest ticks as wall time. Existing Frost/Fireball/Hellbat/Primal cases check immediate refusal and unchanged payment.

## Reasoned-only / not claimed

- No separate Rift or Flux focus is registered in this port; no casts claimed for those names.
- Simultaneous physical client/server activation, visuals, input packets and latency are not exercised by this headless suite. Server-only launch guards are inspected, not two-client proof.
- Exact impact-attempt counts for every specialized projectile and every reentrant callback combination are not established. Only Pech's reentrant defect has a failing regression and a fix here.
- First-flight owner safety is measured for the four basic projectile foci, not every mount, pose, wall-adjacent launch or later ricochet. Original Frost can hit its owner after four grace ticks.
- Scatter figures are finite samples, not a statistical proof of the entire Gaussian distribution. Other area upgrades retain their existing bounded fixtures, not an exhaustive rank/pose matrix.
- Wand setup and targets are fixtures. This is casting evidence, not survival acquisition, full client acceptance or NeoForge parity.
- Existing Warding/Trade successful-target fixtures call the production `WandFocusActions.use` dispatcher directly; their new empty-sky checks enter through the actual native item-use hook. They are not client packet-dispatch tests.

See `docs/analysis/2026-10-23-agent-g31foci.md` for commands, red/green evidence and final validation status.
