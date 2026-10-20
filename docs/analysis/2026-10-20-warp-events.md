# Warp Events — original contract and native implementation, 20.10.2026

Specification: private TC4 4.2.3.5 oracle outside this repository, `WarpEvents`,
`EventHandlerEntity.livingTick/finishedUsingItem`, `ResearchManager.completeResearch`,
`Thaumcraft.addWarpToPlayer/addStickyWarpToPlayer`, `PacketMiscEvent`, six `Potion*`
classes, `EntityMindSpider`, `EntityEldritchGuardian`, `EntityEldritchOrb`, and their renderers.
The 1.12 reference is not the specification.

## Scheduling and randomness

- Server player living tick, positive age divisible by **2000**, `!wuss`, no Warp Ward.
- Read total permanent+sticky+temporary warp, and actual permanent+sticky warp separately.
  Draw `nextInt(100)` **even when the following gate fails**.
- Accept only counter>0, total+gear>0, and draw<=sqrt(counter), inclusive.
  Gear is held item plus four armor pieces plus original Baubles slots (the latter requires
  the separate accessory contract in the native port).
- Effective severity limit is `min(100, (2*warp+counter)/3)`. Subtract
  `max(5, 2*sqrt(counter))` from the counter using the original final integer cast.
  Draw `nextInt(effective)`; a Grinning Devil Fortress mask subtracts `2+nextInt(4)`.
- Every accepted event sends the heartbeat/vignette cue before dispatch. Every check,
  whether accepted or not, reduces temporary warp by one and syncs; Warp Ward/wuss
  suppress the entire check, including that decay.

The severity branches are preserved, including empty windows 0/5–8/85–88 and the
less obvious **73–75 hunger** branch (the original tests `eff==76`, then `eff<=80`).
Research reward is 1+nextInt(times) separate additions of one random primal point,
in the original primal-aspect iteration order.

## Complete event table

| Severity | Original action |
|---|---|
| 1–4 | research reward times1, message3 |
| 9–12 | message11 |
| 13–16 | uncurable Vis Exhaust5000, amplifier min(3,warp/15), message1 |
| 17–20 | uncurable Thaumarhia min(32000,10*warp), message15 |
| 21–24 | uncurable Unnatural Hunger5000, same amplifier, message2 |
| 25–28 | message12 |
| 29–32 | mist2400 + one guardian, message6 |
| 33–36 | Blurred Vision min(32000,10*warp) |
| 37–40 | uncurable Sun Scorned5000, message5 |
| 41–44 | Mining Fatigue1200, same amplifier, message9 |
| 45–48 | uncurable Infectious Vis Exhaust6000, non-ambient, message1 |
| 49–52 | Night Vision min(40*warp,6000), message10 |
| 53–56 | uncurable Death Gaze6000, same amplifier, message4 |
| 57–60 | up to min(50,warp) harmless viewer-only mind spiders, message7 |
| 61–64 | message13 |
| 65–68 | mist2400 + min(8,warp/30) guardians, message6 |
| 69–72 | Blindness min(32000,5*warp) |
| 76 | sticky warp−1 when positive, message14 |
| 73–75,77–80 | uncurable Unnatural Hunger6000, message2 |
| 81–84 | research reward times warp/10, message3 |
| 89–92 | real mind spiders, message7 |
| 93–99 | mist2400 + min(8,warp/15) guardians, message6 |

## Research and effects

Only an accepted event checks actual warp. `>10` grants `@BATHSALTS` unless either
that hint or BATHSALTS is already known; `>25` grants ELDRITCHMINOR with times10
research reward; `>50` grants ELDRITCHMAJOR with times20 reward. Repeat events do not
repeat those rewards. Research completion must keep the shared research-warp rules.

Thaumarhia checks every twentieth remaining-duration tick, with one chance in15 to
place default flux goo in an empty feet cell. Unnatural Hunger causes `.025*(amp+1)`
food exhaustion every tick. Rotten flesh/zombie brain reduce duration600 and amp1;
other food warns and does not cure. Sun Scorned checks every40 duration ticks:
brightness>.5 and `random*30 < (brightness-.4)*2` with sky access ignites4 seconds;
otherwise brightness<.25 and random>brightness*2 heals1. Death Gaze runs every10
player ticks; range min(8+3*amp,24), original .75-radian cone, actual line of sight,
PvP policy and no existing Wither, then Wither80 plus revenge targeting.

## Dependencies and honest status

Sources now cover server rules, every dispatch branch, six effect types, two native
mob classes, the orb, its undead exclusion, guardian eye/essence drops, the S2C
payload, consumed-food hook and the shared crafting-warp helper. Client sources
include the original guardian model (54 parts/15 child links), spider/guardian/orb
renderers, heartbeat/vignette/mist, four converted post chains, sonic hemisphere,
the guardian's per-tick body haze and the orb's thirty dark shrinking wisps. The original shader inputs and all
retained art remain untouched; shader and potion-icon conversions are repeatable
with the two `scripts/porting/convert_warp_*` scripts.

Registry/network setup and event scheduling are deliberately separate:
`ThaumcraftWarp.register()` does not start events; root enables
`ThaumcraftWarp.enableEvents()` only after integrated native checks. The initial
server implementation compiled in root's first build. Later client/food/effect
changes compiled in root's integrated builds. Integrated run3 passed all dispatcher,
effect/food/gaze/mob/undead assertions. Run4 added a test of the enabled ordinary
PLAYER_POST scheduler and exposed a fixture transport omission: unlike a real
network connection, an EmbeddedChannel placed only in PlayerList does not join
ServerConnectionListener's loop and cannot reach Player.tick/doTick. The fixture
now joins/removes that native loop and awaits its final run. No direct event call
is used in the scheduler assertion. Final build/runtime validation is pending;
no inspected game-client result is asserted.

The six effect icons were corrected to include original GUIInventory's y198 atlas
origin. Focused assets tests passed (3/3): nontransparent pixels/exact retained
cells, deterministic GLSL150 conversion/resolved links and the explicit shader
allowlist. The visual correction requires a final resource build; these tests do
not substitute for in-game inspection.

Native adaptations requiring explicit review: loaded chunks only for ambient spawn
attempts (never force-load); registered 1.21 entity dimensions/attributes, modern
combat/navigation/projectile/particle base physics, core GLSL150 and namespaced
post-program paths, modern box blur replacing the removed built-in legacy blur,
and overflow-safe arithmetic for corrupt extreme warp values. The original
exponential mist currently maps density to a native reciprocal linear fog range;
this is an explicit visual adaptation, awaiting client inspection. Ordinary
canonical arithmetic/RNG/order stays unchanged.

`WarpRulesTest` covers RNG ordering, conditional windows, inclusive gate/mask,
strict automatic research thresholds and the gaze cone. `WarpRuntimeChecks` uses
a real connected server player, declared random inputs, a restored loaded floor,
native dispatch/effects/rewards/entity spawning, the actual food mixin and native
combat/save/collision calls. Assertions cover all event branches, temporary decay,
non-repeated unlock rewards, actual versus temporary/gear warp, real mask0,
ward/wuss gating, exhaustion/goo/sun ticks, cone retaliation, spider behavior,
guardian magic resistance/home and both ranged attacks, orb lifetime/radius and
undead immunity. These are behavioral test sources, not evidence of a passed run.
Original harmless-spider reload behavior is retained: watcher restores
harmless/viewer but does not reset its lifespan.

Crafting warp is exactly one sticky point per original output, independent of
stack count: Hellbat Focus, Sinister Lodestone, Brain in a Jar, Liquid Death bucket,
Bottled Taint, Flesh Golem, Primal Focus and Primal Staff core. Several outputs are
still separate unported content; no warp is assigned to unrelated voidmetal.

This does **not** close M11 as a whole. Natural obelisk/Outer Lands spawns, cultists,
Pech, other original mobs, taint ecology, bath salts/pure fluid acquisition, accessory
warp slots, packaged multiplayer and inspected client evidence remain separate work.
