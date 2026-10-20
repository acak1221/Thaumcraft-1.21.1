# Ordinary node biome ecology and embedded Silverwood nodes

This unit follows the private TC4 4.2.3.5 oracle outside the repository:
`TileNode`129–151,667–676,703–798; `Utils.setBiomeAt`189–202;
`WorldGenSilverwoodTrees`95–105; shared `ThaumcraftWorldGenerator`218–399;
`BlockMagicalLog`104–186. It is a draft for one coherent native dependency
boundary, not evidence that the earlier ordinary-node checkpoint implemented
these types. Its exact activation paths are listed in
`.tmp/node-ecology/activation.json`.

## Native lifecycle and original ordering

The shared ordinary TileNode supports NORMAL, UNSTABLE, DARK, TAINTED, HUNGRY and
PURE when their actual biome/block/mob dependencies are present. Its original
order remains hungry-first, counter increment, lock polling, discharge,
recharge/pruning, taint, stability, DARK, PURE, hungry-second. Type changes from
Taint infection therefore precede the former DARK handler on that same tick.
Biome writes themselves do not manufacture aura or set the node's aura-change
flag. Existing pearl, transduction and Hungry paid-death receipts are retained.

PURE runs every50 node ticks after its original dimension gate. Nether, End and
blacklist levels0/2 refuse before offset draws; level1 and the default−1 allow
the operation. Four bound8 draws choose x+/x−/z+/z−. The sampled biome's
blacklist0/2 refuses, and Magical Forest already needs no update. An ordinary
wild PURE aura node cleanses only actual Taint terrain. A PURE node embedded
inside the real Silverwood knot also converts other allowed biomes.

DARK runs every50 ticks and checks the center biome's blacklist before its four
bound12 draws. The sampled target's blacklist is deliberately not checked. It
writes actual Eerie before evaluating hardNodes, the coin or player proximity.
The native nearest-player24 query includes creative players as the original did.
The original constructs a real Giant Brainy Zombie before counting existing
Giants in the unit block AABB expanded10/6/10; `<=3` allows one fourth Giant.
Only an eligible count proceeds to the original x/z double differences, Y
bound3−1 and world yaw float. Its own native entity RNG supplies brightness
bounds32/8. Original Creature/Living obstruction, liquid and path-weight checks
are preserved without a new supporting-floor requirement. No finalizeSpawn or
equipment/baby initialization is added.

TAINTED runs every50 ticks without the PURE/DARK blacklists. Four bound8 draws
write the actual Taint biome. hardNodes and its subsequent coin gate only the
six bound5 x+/x−/z+/z−/y+/y− draws and actual
`BlockTaintFibres.spreadFibres` call. Every100 ticks a non-PURE/non-TAINTED node
standing in actual Taint rolls bound500 and may become TAINTED. No marker block,
vanilla biome alias or fake fibre callback substitutes for these dependencies.

## Real embedded Silverwood owner

The original metadata2 log becomes native `thaumcraft:silverwood_knot`, a full
RotatedPillarBlock containing the registered shared TileNode. It is registered
before the TileNode type's valid-block list is built. Its original hardness2.5,
light7, retained knot side/top textures, wood sound and fire5 values are kept.
Its actual LOGS membership supports both leaf distances and Taint crust rules.
It has no creative/item identity: pick-block and ordinary loot return plain
Silverwood, matching original damageDropped metadata2→1.

The actual Silverwood generator now commits knot-bearing trees. It consumes
the original nonconsecutive knot rolls and invokes the same original shared
NodeGeneration plan as wild nodes and Primal impacts. Silverwood forces PURE
and uses original quarter aura; full aspects/modifier plans are retained during
tree preparation, rather than merely consuming and discarding their RNG. The
tree overlay preserves original generation order while native commit exposes
supporting logs before final leaf distances. Vanilla OAK leaves used by the
coordinated BigMagicTree share that distance/leaves-last adapter; TC4-only
CHECK_DECAY is set only on states owning the property.

The knot owns normal current/BASE vis, stable scan UUID, original type-specific
scan tags, wand use, recharge and persistence. Player harvest uses the existing
guarded native NodeHarvest receipt in addition to one plain log: each current
aspect>=5 yields `floor(current/10)+1` separate two-aspect essences, including
the original creative behavior. Generic destruction has no player receipt and
returns only ordinary block loot. Exhausting the final aspect at node count1200
changes the knot to plain Silverwood with the same axis. It does not create an
air hole, an essence reward or an orphan tile, and the metadata-only equivalent
does not start leaf decay as though the whole log had been broken.

## Forced native adapters

Original biome storage was a1×1 x/z byte column. Minecraft1.21 stores biomes in
4×4×4 quart cells, so one old horizontal cell maps to one native4×4 quart column
across all quart Y. All other columns retain their exact holders. The runtime
adapter touches only an already-loaded chunk, marks it unsaved and sends the
vanilla biome packet to its tracking players. Native surface-holder comparisons
use the selected stored quart, avoiding a Voronoi neighbor sample that would
misidentify the column just written.

WorldGenRegion has a separate overload operating only on its existing writable
generation chunks. It marks those actual chunks unsaved without querying a live
ServerLevel chunk owner or sending tracking packets from a worker. Eventual
native chunk publication carries the biome payload. Removed numeric biome and
dimension IDs map to native ResourceKey maps; only original levels0/2 block.
This is not old-world import or binary compatibility with Forge's dictionary.

Runtime generation can encounter an already-busy actual TileNode. The shared
factory consumes its complete original generation plan, then calls boolean
tryInitializeGenerated4. Active tick/notification/transduction/pearl/orb ownership
refuses safely without changing existing aura. Known-fresh committed knots can
use the void initializer. Invalid server thread/context remains an explicit
error. Generation regions initialize copied state through their explicit owner
instead of calling live-server packet/dirty callbacks from a worker.

## Prepared verification and remaining evidence

These draft files have not yet been compiled or run. Pure policy JUnit checks
cover levels0/2 versus1/default−1 and the separate Nether/End exclusion. Native
fixtures are prepared in separate batches:

- PURE distinguishes wild untainted terrain, real Taint cleansing and embedded
  Silverwood conversion; checks the target blacklist and dimension RNG gates.
- DARK checks target-blacklist bypass, center gating and biome conversion with
  hardNodes=false. Its real opaque room waits for native light propagation,
  then admits three real Giants and requires one fourth in unsupported air,
  with exact world draw ordering; four existing Giants refuse a fifth before
  position draws. The Giant's own brightness RNG is not replaced.
- TAINTED creates actual supported TYPE0 fibres through the native lottery,
  verifies hardNodes and blacklist distinctions, real infection/current/BASE,
  infection-before-DARK order and PURE immunity without a bound500 draw.
- Embedded gameplay uses a real connected survival actor, actual thaumometer
  UUID target acquisition, real live-hand wand use and one-unit vis payment,
  original count599/600 recharge, player harvest versus generic destruction,
  plain-log identity and exact AIR2 essence conservation. Manual count-boundary
  invocations are declared; they are not claims of elapsed world time.
- Storage compares all native quart holders, dirtiness and idempotence, encodes
  and decodes the vanilla biome packet into an uninstalled native chunk
  container, then requires actual disk unload/reload of a fresh chunk/tile.
  Knot axis/PURE type/UUID/current/BASE and biomes survive; the actual knot
  ticker exercises count1199/1200 depletion to plain axis-preserving Silverwood.
- The existing magical-tree fixture's obsolete seed0 skip assertion becomes
  real knot-bearing sapling growth plus TileNode codec checks. Its independent
  seed7 geometry, leaf-RNG and survival plank checks remain intact.

Compilation, full dedicated-server runtime, actual generation-region behavior,
inspected knot/biome client visuals and remote multiplayer still need evidence
for this joint snapshot. Earlier checkpoints and passing non-node tests do not
establish these results. NeoForge parity remains deferred under Fabric-first.
