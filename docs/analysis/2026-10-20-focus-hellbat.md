# Hellbat focus native contract

Authority: private TC4 4.2.3.5 oracle `ItemFocusHellbat`, `EntityUtils.getPointedEntity(World,EntityPlayer,double,Class)`, `ItemFocusBasic.applyUpgrade/canApplyUpgrade`, `ConfigRecipes:338`, `ConfigResearch:80-82,162`. The legacy1.12 source is not the gameplay specification. The existing native `EntityFireBat` supplies the actual summoned mob and all attack/flight/variant behavior; this unit does not create another mob identity.

## Implemented behavior

- Native item `thaumcraft:focus_hellbat`, rare inherited WAVE, sorting prefixHH, color14431746,1000ms activation cooldown. The inventory model preserves the ornament followed by the main original icon.
- Real infusion recipe `infusion/focus_hellbat`, gated byFOCUSHELLBAT: magma cream center; quartz/fire shard/quartz/air shard/quartz/entropy shard;25Ignis,15Aer,15Bestia,25Perditio; instability3; one focus. Original field mappings confirm `field_151064_bs=magma_cream` and `field_151128_bU=quartz`.
- Original32-block entity ray: broadphase padding1.1, per-candidate eye LOS, at least0.8 pick padding and the original zero-distance selection behavior. Exclude Firebat instances. A nearer nonliving pickable entity occludes a living target, and no living target spends no vis. A player target with server PvP disabled also spends nothing.
- Spawn position is the original body-midpoint offset, yaw-side offset0.16, look offset0.5, then the actual bat's height; no eye-position substitute. Set its real target, Potency damage bonus, summoned=true and hanging=false before native admission. Bomb, Devil and Vampire flags are applied to the real Firebat; only Vampire records the caster as owner.
- The original passes the **wand** into `getVisCost`, so every actual variant pays base200Ignis+100Perditio+100Aer, including creative, with each original float modifier/truncation. Tooltip Bomb instead shows100Ignis+200Perditio+100Aer; Devil shows100of each Ignis/Perditio/Aer/Terra. No missing research gate is added to casting a found focus, even a Vampire focus.
- One paid native spawn attempt. A rejected admission remains paid and plays the original failure sound; there is no retry/refund. Success uses original event2004 at the truncated original muzzle coordinate and the native ice sound at the bat. Ordinary unfunded target attempts remain atomic and use the failure sound. All living-target paths swing; no target and PvP-disabled paths do not swing on the server.
- Original upgrade IDs13Bombs (Energy+Trap),14Devil (Armor),19Vampire (Hunger+Life), one of each declared aspect. All ranks offer Frugal/Potency; rank3 additionally offers Bomb/Devil, rank5 Vampire. Closed codec/catalogue checks remain in force.

## Vampire actor/API contract

Original `canApplyUpgrade(...,player,vampirebats,...)` calls the player's name without a null guard and requiresVAMPBAT. Original `applyUpgrade` does not call that actor-dependent query. The native Hellbat preserves both behaviors: explicit legal Vampire queries with null actor throw; the actor-free apply API performs a guarded structural write. The actual Focal Manipulator calls the real actor query before XP/vis admission and later uses the structural apply API after paying its debt. Direct raw component/structural API access is not a player packet or machine authorization.

## Authority adaptations

The server owns the actual connected hand, target ray, native mob construction and payment. After native target callbacks and before payment, recheck actor/hand/components, position/look, target identity/liveness/world, PvP and the current ray. A callback cannot redirect or refuse a target and retain a pending paid summon. The existing dispatcher guards reentrant activation. Native spawn callbacks observe an already debited real wand. No client-supplied target or summon data is accepted.

This does not rewrite the existing Firebat's native AI, combat, save policy or original transient Vampire owner behavior. Their separate unit and remaining engine differences remain visible. World/actor protection-mod behavior beyond the native selection/admission checks is unverified.

## Prepared verification

`FocusUpgradeDataTest` extends the exact catalogue, defensive multi-aspect declarations and JSON/NBT/ten-byte wire round trips. Unknown/unimplemented IDs remain rejected.

`HellbatFocusRuntimeChecks.run` uses real connected native players and actual RMB dispatch. It checks the registered infusion contract; null/real-actor/structural Vampire API differences; actual variant/Potency/owner/muzzle; precise base-price Frugal debit; immediate real cooldown replay; funded creative and one-short atomic refusal; native boat occlusion, Firebat exclusion, stone LOS, long target reach and PvP; native ADD cancellation after payment and reentrant RMB refusal. A separate actual Focal menu first refuses missingVAMPBAT before resources, then debits40levels and pays the full rank5 primal debt from an actually converted energized node. Removing actor research after admission still permits the original actor-free completion without a null-player failure or second charge. These are declared server fixtures and direct ticker advancement, not elapsed survival acquisition.

**Implemented:** production, recipe/model, catalogue/API and native regression sources. **Built / runtime-tested / seen:** pending root integration; no Gradle command was run by this author. JSON parsing and owned whitespace checks pass. Original art/audio, full survival infusion acquisition, restart/multiplayer/protection-mod acceptance and NeoForge remain unverified; this unit does not close the full migration.
