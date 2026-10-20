# Taint Spider, Spore and Swarmer contract

Original authority: TC4 4.2.3.5 private oracle EntityTaintSpider,
EntityTaintSpore, EntityTaintSporeSwarmer, FXSwarm, FXBreaking, ClientProxy899-904,
931-952, the three entity renderers and models/entities Spore/Swarmer.
Activated as a coherent source group on 2026-10-20. Compilation, native runtime and client visual acceptance remain pending.

Public registry dependencies are root's ThaumcraftTaint.TAINT_SPIDER,
TAINT_SPORE, TAINT_SWARMER, TAINT_SWARM and FIBRES, plus review_tests'
BlockTaintFibres.TYPE0..4 and isTaintBiome(ServerLevel,BlockPos) using the actual
BiomeColumns surface holder / common.config.ThaumcraftBiomes.TAINT.
Swarmer constructs devices' actual EntityTaintSwarm(type,level). No substitute.

Spider: .4x.3, inherited eyeHeight=.3*.85=.255, pick radius .1, HP5, attack2,
XP2, constant voice pitch .7 (original func70647_i, not sound volume), inherited
vanilla spider climbing/leap/attack and nearest vulnerable player12 without
brightness/LOS gate. Custom loot world.nextInt(6)==0, then world.nextBoolean
selects resource11 tainted_goo /12 taint_tendril, one item at height/2. No vanilla
string/eye table. Body .4/.5/.4 scale, shadow .5, flip180, retained pink eyes.

Spore: size signed-byte watcher default1 then constructor2; NBT Size=size-1,
missing Size produces1. Physical max(.15F*size,.5F), inherited eyeHeight*.85,
HP1/attack1, XP=size, pick radius0. Growth and display are NOT persisted.
Growth test is size<10 && growth++==1200: size changes on call1201, resets0.
Native API rejects negative/overflowed watcher sizes that crash original RNG. Native fudgePositionAfterSizeChange is disabled for this stationary actor: original setSporeSize explicitly reapplies identical feet XYZ, so growth must not shift its exact support.
No goal AI; pickable but not pushable. All X/Z/upward movement clamped; exact
below boundingBox.minY-1 fibres TYPE4 prevents all movement. Outside actual
Taint biome generic1 damage every20ticks. Unsupported, deathTime>0 and player
collision burst. These conditions use deathTime, not hurtTime. Server claims a
single current installed entity; client effects cannot spawn or edit blocks.
Burst world draws: pitch float; count size/3+nextInt(size/2+1); for each child,
X two floats/Y one/Z two/yaw one. Actual native spider children and exact
support4->3, then discard. Alive rupture does not run death loot. Death loot is
one Boolean-selected goo/tendril; ordinary Spore disappears at deathTime1 before original death20 XP, so it grants0 XP. Client grows
display+.02 when below watcher, manages floor(size/3) original FXSwarm companions
one at a time and50 sploosh effects when its local burst condition is seen.
Fullbright body nested12/16 cubes64x64, negative -.12*display pulse.025, wobble
.02 or .04 while hurt, flip0, shadow.25, render squared-distance<4096.

Swarmer: Spore subclass, constructor size10, fixed1x1/eye.85, HP75/attack1,
XP=size. SpawnCounter500 is not saved. Each update pushes out of blocks,
decrements while positive, waits at0 for nearest vulnerable player16 without
LOS; then resets500 and spawns one true Swarm at ownY+.5/world yaw. On
deathTime==1 it spawns one more, preserving a possible same-frame timer+death
pair. Player collision does nothing and unsupported feet do not rupture it.
Status6 resets client counter500, emits25 sploosh and kills/clears companions.
Client companion target is (500-counter)/25 (one remove/add per update).
Two Boolean goo/tendril death drops. Ambient roots interval200/volume.1;
base Spore ambient swarm, hurt/death gore. Swarmer two-cube model keeps only
inner pulse fullbright and fixed base world-lit; no fake full-body glow.

Native sound transport gets an explicit non-gameplay UUID/tick seed to avoid
adding Level.playSound's new world.nextLong before the original burst draws.
Custom drops reuse OriginalMobLoot.spawn to keep native ItemEntity's two new
world draws out of the original per-drop Boolean schedule.

TaintSwarmVisuals.tick(target,desired,speed,turn,gravity) is exact count;
tick(target,base,minimum,speed,turn,gravity) scales with original particleCount:
ALL=base*2, DECREASED=base, MINIMAL=0, then minimum. Living Swarm uses
25/min10 =>50/25/10 with .22F/15F/.08F; Spore/Swarmer .1F/10F/0. Actual particle
has original eight atlas cells7..14 row4, independent particle RNG turn/noise,
world RNG initial position/RGB, target-death fade51ticks, liquid-aware block
push-out and shared three-buzz/nanotime behavior. Native slime-ball breaking
quarter sprites implement original physical purple sploosh/lifetime/fade.

Root hooks after dependency completion: attributes three builders, registry
physical sizes and inherited eyes above, ThaumcraftTaintSporesClient.init()
registers all three renderers and actual visual provider; client resource reset
calls ThaumcraftTaintSporesClient.reset(). No shared mixin or gameplay tick loop.
Runtime helpers: growthAndBurst (batch m08_taint_spore_growth, timeout200), swarmerBoundaryAndLoot (batch m08_taint_swarmer, timeout200), spiderTargetsAndLoot (batch m08_taint_spider, timeout200). All use native entity ticks, declared actual actors/biome inputs and actual physical loot; execution is pending.

Post-integration native audit (fixes implemented; new run pending):
- Native NearestAttackableTargetGoal mustSee=false controls continuation but
  its separate targetConditions still checks LOS at acquisition. The owned
  OriginalPlayerTarget explicitly calls ignoreLineOfSight, retaining original
  vulnerable-player12 search rather than weakening the wall-target assertion.
- Native refreshDimensions/fudgePositionAfterSizeChange preserves old centre
  and shifts feet when growing; original setSize+setPosition preserves feet.
  The Spore override retains exact anchored XYZ and tests alive/support at1201.
- Original EntityLivingBase.sv.aF increments deathTime then awards XP exactly20.
  Modern LivingEntity awards at die and ExperienceOrb.award adds WORLD nextInt40
  per split. Scoped OriginalTaintExperience suppresses immediate actor rewards,
  performs original recent-player/can-drop/DOMOBLOOT checks on20, then emits
  canonical native XP orbs directly with original independent Math.random
  scatter. Spider2 and Swarmer10 award at20; ordinary Spore bursts/removes at1
  and therefore0XP. No global XP rewrite, merge-roll fixture normalization or
  free/public enqueue-by-position is introduced. Native Sculk consumed-XP gate
  is retained to prevent a second reward. Original five equipment slots are
  remembered before native drops clear them; actual <=1 chances and reward
  RNG are evaluated at20. Native dropCustomDeathLoot/setItemSlot do not reset
  chances; guaranteed>1 equipment stays excluded. Offhand adds no XP.
- Claimed deathTime20 precedes ADD/save callbacks. Exact-source reentry returns
  without a second reward; actual native XP candidate pickup requires world
  admission. Each canonical split gets one native ADD, and a rejected unused
  UUID uses the existing generic recovery. No retry/refund is added to original
  mob XP veto behavior. Before each next split the same world/source-level and
  loaded birth chunk are checked, without forcing unavailable terrain.
- Runtime cases now assert no immediate/19-tick XP, exact20 terminal reward,
  actual XP ADD/source tick reentry and denied pre-admission pickup, plus
  ordinary Spore's absence of XP after its death1 rupture. Original loot/world
  draws remain strict; no native WORLD40 rolls were added to expected inputs.

The first integrated local/CI run exposed these three native deviations. These
fixes have not yet passed a subsequent build/runtime run; client rendering,
physical multiplayer and survival acquisition remain unverified.
