# HUNGRY ordinary nodes — TC4 4.2.3.5

The private original oracle outside the repository is the authority:
`TileNode` tick129–151, HungryFirst487–544, discharge547–604,
HungrySecond798–821, lock824–842; `ScanManager` entity/item scan tags466–495;
`ResearchManager.reduceToPrimals`466–495; wild-node generation218–399;
`ClientProxy.hungryNodeFX`1058–1060 and `FXBoreParticles`.

## Server gameplay and the two phases

First runs **before incrementing the node counter**, then existing lock sampling,
aura discharge/recharge/pruning and stability run; Second runs after those
handlers (the later DARK/PURE ecology remains pending). Damage callbacks cannot
reenter ticking, transduction or a primordial-pearl preparation on the same node.
Retired/replaced nodes stop before changing other live world objects.

`hardNodes` controls only First's entity behavior. The original query is the
node's one-block cube expanded15 in every direction, with actual attraction
inside a15-block sphere measured from node centre to entity FEET, not eyes.
Force is normalized direction times squared remaining-distance fraction,
.15 horizontally and .25 vertically. No line-of-sight gate is added.
Player abilities.invulnerable (creative/spectator) skips damage AND force;
other invulnerable entities still feel force. A living non-invulnerable target
inside squared distance2 receives native drowning1 each node tick. Native
LivingEntity damage cooldowns still apply; item health5 is natively consumed.
Stabilizer lock does NOT disable either hungry phase.

Only an actual target death yields a meal. Item entities spend their WHOLE
actual named/component-bearing stack through native damage, but tags are for
ONE item, deliberately not multiplied by count. The original ScanManager path
honors scan-only grouped canonical metadata; ordinary dissolution stays separate.
Living entities use their real
public entity scan bindings. Tags reduce to primals through the original SUM
component paths. One unweighted distinct primal is chosen; depleted CURRENT
refills1 without a capacity chance, otherwise `nextInt(1+BASE*2)<primalAmount`
adds1 to BASE only. A failed chance still leaves the killed source consumed.

No reward setter, synthetic item removal or public meal token can grant aura.
Actual source references/count/components are checked before damage and retained
through death, apart from the exact native BlockItem container transition described
below; changed/revived source or stale node snapshots do not credit.
The existing native destroyed/picked-up item custody hooks still observe native
item deaths. A canceled damage call keeps physical stock and grants no aura. A private
identity-only damage frame spans native damage and reward across hungry nodes,
because ItemEntity calls onDestroyed before discard; reentrant neighbors may
pull, but cannot damage/credit the same in-flight physical source twice.
Node current/base state persists through its existing native NBT codec.

### Native destroyed-block-item component transition

Minecraft1.21.1 `BlockItem.onDestroyed` unconditionally writes
`DataComponents.CONTAINER = ItemContainerContents.EMPTY`, even for ordinary dirt
that had no container component. It also releases any prior container contents
through native `ItemUtils.onContainerDestroyed`. This engine behavior has no
equivalent component in the original1.7 item receipt. Comparing every component
to the unmodified pre-death receipt therefore rejected ordinary block-item meals
after the real item had already died.

The adapter accepts exactly this transition only after native death of the same
actual BlockItem stack. Entity/stack identity, count and every other component
must still match; replacing the stack or changing its name/count refuses reward.
Both post-damage and post-RNG checks use this witness. Tags still come from the
unaltered pre-death one-item receipt. Any native released contents remain separate
physical item entities and are never converted to an extra meal for the parent.
The runtime fixture preserves exactly seven named iron ingots from a nonempty
container and requires exactly one parent-primal draw. Separate real native death
callbacks change the name, count or stack reference and must produce no meal draw
or aura. No normalization is allowed before the physical payment.

Second runs every50 post-increment ticks **outside hardNodes and lock gates**.
It consumes exactly six nextInt16 offsets, clamps Y to the sampled column's
height, traces from the node centre ignoring only the source block and stops on
source fluids. The first hit must have squared block-position distance<256 and
hardness>=0,<5; native destroyBlock(drop=true) handles actual block loot and
native inventory evacuation. Hungry does not fabricate an inventory snapshot or
replace the ordinary native block-destruction path. It can destroy its own basic
stabilizer after the same tick sampled lock1; the cache changes at a later poll.

The native ray uses OUTLINE/SOURCE_ONLY and a source-ignoring BlockGetter that
never loads missing chunks. Modern MOTION_BLOCKING height/voxel outlines replace
old height/collision metadata; unloaded ray cells stop instead of loading terrain.
Finite-zero attraction and invalid overflowed growth-bound guards replace
original divide-by-zero/nextInt(<=0) crashes. Other ordinary capacities/quirks
remain literal.

## Generation, Pearl, player harvesting and client

The actual original wild-node HUNGRY roll is now admitted once, without reroll.
HUNGER merges2, optional GREED merges1 before environment counts and spread RNG.
NORMAL/UNSTABLE remain enabled; DARK/PURE rolls remain skipped until their own
ecology exists. TAINTED biome conversion, silverwood knots and non-Overworld
node ecology remain pending.

Primordial Pearl uses the coordinated opaque prepared transition for HUNGRY as
well as other ordinary nodes: the physical pearl pays once, current/BASE/modifier
change with original arithmetic, while type, scan identity, transient count,
lock and regeneration cache stay intact. Native explosion/flux effects still
run and may destroy surrounding support, as the original allowed.

Original BlockAiry player harvesting has no node-type restriction. The native
NodeHarvest whitelist now includes the three actually supported ordinary types
NORMAL/UNSTABLE/HUNGRY. Actual CURRENT25 yields three aspect2 essences even in
creative, through the existing deferred owned outputs; neither Second's generic
block destruction nor explosions acquire player-harvest authority.

The common client ticker delegates only to an installed client adapter, avoiding
client class loading on a dedicated server. Original particles use2/1/0 attempts
for ALL/DECREASED/MINIMAL settings, six independent offsets per attempt and the
same source-ignoring ray, independent of hardNodes. Native block-atlas fragments
accelerate inward, friction.985, shrink below4, and clamp each axis to.35.
Client predicted entity force follows the original hardNodes gate; authoritative
health and aura remain server-only. Modern model particle sprites/native tint
and visual no-collision motion replace legacy random-side icons/push-out. Exact
client visual parity has not been inspected or claimed.

## Verification status

Five pure JUnit cases cover force/distance/vertical coefficient, no-empty draw,
unweighted selection/refill/growth bound, missing-current BASE-only growth and
hardness boundaries. `HungryNodeRuntimeChecks.run(test)` declares real native
actors/items/blocks and explicit server RNG branch inputs, then verifies:

- native health5 consumes one named thirteen-item stack once for one current
  refill; damage sees counter0..4 before increments; callback reentry/unpaid
  pearl refuse and a second actual hungry node cannot double-spend the source;64 dirt with one-itemEarth2 can fail growth, another paid stack
  grows BASE only; native grouped scan metadata differs from ordinary dissolution;
  canceled native damage preserves stock; native chicken death
  uses entity scan tags; current/BASE/type NBT survives;
- actual15-block motion, stronger vertical force, creative/survival/invulnerable
  and live hardNodes behavior; basic lock1 retains First's force;
- actual50-tick native stone drop, hardness5/unbreakable/zero-outline boundaries,
  source/flowing-water interception, out-of-sphere first hit, eating a locked
  stabilizer after the stability handler, and all seven named chest inputs;
- real paid Pearl on HUNGRY retains caches/type; native player breaks of HUNGRY
  and UNSTABLE produce exactly original three AIR2 essences from current25;
- ONE fixed natural roll466 initializes HUNGRY/HUNGER2, then ordinary registered
  entity/node ticks consume a real five-item source and refill one current unit.

The full `b7c7845` CI run reached this test but failed its full-BASE dirt chance
assertion. A subsequent local focused diagnostic ran exactly Hungry plus the
crystal reload test using the existing Fabric server task. All native dirt
health/current-owner/one-item Earth2 preconditions passed; the recorded failure
had `bounds=[]`, `effects=[]`, a dead item and unchanged current/BASE. Native
bytecode then established the BlockItem CONTAINER mutation above as the cause.
The production adapter and expanded custody/conservation assertions were written
after that diagnostic. The subsequent `075df78` CI run passed every original
meal, native container conservation and adversarial callback assertion, then
failed the survival-player damage assertion. Native `ServerPlayer.hurt` has a
separate private60-tick spawn protection; clearing public LivingEntity damage
cooldown cannot bypass it. The fixture started after only5 world ticks. It now
waits65 actual native world ticks and requires the actor to have completed at
least60 native player ticks before running its unchanged damage/pull assertions.
This fixture correction still requires subsequent compile/runtime verification;
production damage is unchanged.
That two-test run is not acceptance of the full243-test snapshot. The temporary
wrapper annotation selection was restored byte-for-byte afterward. Exact log:
`.tmp/hungry-crystal-focused.log`; no test-filter infrastructure was added.
Client inspection, remote multiplayer, full M02 ecology/public node API and
NeoForge parity remain unverified/pending.
