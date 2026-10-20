# Firebat and Wisp: canonical contracts and native implementation

Specification is the private immutable TC4 4.2.3.5 oracle: EntityFireBat,
EntityWisp, ConfigEntities, ItemFocusHellbat, RenderFireBat, ModelFireBat,
RenderWisp, FXGeneric, FXWisp, FXBurst, PacketFXWispZap, FXLightningBolt and
FXLightningBoltCommon/WRVector3/WRMat4. Vanilla1.7 EntityMob/EntityCreature,
EntityFlying and World.getClosestVulnerablePlayer were audited outside the
repository, alongside the immutable original1.7 client JAR.

Firebat has health5, damage1, box0.5x0.9, starts hanging, cannot be pushed,
ignores fire/explosion/fall damage and takes drown1 each living update while
in water. Its five synchronized BatFlags are hanging/summoned/explosive/devil/
vampire. Summoned damage is (Devil?3:2)+damBonus; clearing Summoned sets1.
Setting Devil true recalculates damage; clearing it alone does not. The old
constructor's health15 conditional cannot see a later Devil flag, so actual
ordinary summoned Devil bats retain health5; the original focus does not
change health. Flags and signed-byte damBonus persist; owner does not.

Its removed legacy creature action runs before original custom flight steering:
target acquisition12, LOS melee, target death/creative clearing, random stroll
draws, path following and idle look, followed by ceiling wake/roaming/pursuit.
The1.21 Path supplies current collision geometry; no default melee/stroll goal
is added over the legacy action. Native move/jump controls cannot overwrite the
old direct movement inputs. Both monsters retain the original vulnerable-player
selector's strict distance, sneaking0.8 and invisible0.7*armor-cover gates.
Modern spectators are excluded, a platform adaptation for the later game mode.

Melee cooldown20, range max(2.5,targetWidth*1.1) and box overlap are retained.
Summoned hits mark recent-player time100 without inventing a player killer.
Vampire gives its owner regeneration26/amplifier1 only when absent and heals
itself1 before the attack choice. Explosive always explodes unless Devil;
ordinary bats have a world-RNG1/10 explosion chance. Explosion strength is1.5
plus explosive damBonus*0.33, with no fire or block destruction, then consumes
the bat. Other choices are Vampire/one-half melee with restored target motion,
or ignition4 seconds summoned/2 natural. A flying summoned bat without a target
takes generic2, not magic2. Natural death gunpowder is entity nextInt(3), plus
nextInt(Looting+1) only when Looting>0; summoned emits no item. Empty inherited
rare hooks still consume the original recent-hit nextInt(200) draw.

Wisp has health22, attack3, box0.9, XP5, no drowning decrement and original
Type String persistence/sync. Native Entity.getType() already returns an
EntityType, so the public aspect API is getAspectTag/setAspectTag. Invalid
types initialize from world-RNG90% primal/10% compound; a registered future
thaumcraft:eerie biome selects one of the six exact dark aspects with entity
RNG. That biome's population is not claimed by this unit.

Wisp preserves the waypoint replacement's PREVIOUS steering vector, float
sqrt, post-decrement course cooldown, collision path checks, truncation of end
coordinates, liquid rejection and ground within10 below. Retaliation is set
before the parent hurt call, including refused hits, with true attacker taking
precedence over direct attacker. Aggro cooldown decrements, one-in1000 world
acquisition and its conditional second decrement retain original order. LOS
charges20; blocked/out-of-range positive charge decays1. A moving target has
40% damage3, a still target66% damage4, followed by counter=-20+nextInt(20).
Sound and the two-ID S2C zap occur even on a missed damage roll. Wisp death
emits exactly one own-aspect2 essence, independent of player credit/Looting,
inside the native doMobLoot gate. Nearby existing Wisp count must be<8;
cluster cap is2. Original sky32/local8/thunder10 light rule is retained.

Documented original defect fix: when a Wisp's previous waypoint equals its
position, the original uses old distance0 after choosing a new waypoint and
can divide0/0, putting NaN into entity velocity. The port still chooses/checks
the same course but skips the undefined acceleration when distance is0. The
native runtime check exercises this input and asserts finite motion.

An additional reproduced visual defect is the original float joint dot falling
outside acos's[-1,1] domain. The actual migrated float algorithm with native
Vec3 endpoints(1,2,3)->(7,5,8), seed2, fork12/segment110 produced sinNext=NaN;
the outside-repo javac audit retained that pre-fix output. Joint calculation now
clamps the rounding error and falls back to ordinary width for a zero/undefined
denominator, preserving all finite original joints. The64-seed finite-geometry
test verifies the boundary without feeding NaN/Infinity to the native GPU mesh.

Natural population is Firebat10/group1..2 and Wisp5/group1 in Nether-tagged
biomes; Firebat adds original allowed Overworld families5/group1..2 only when
initialization occurs on October31. Firebat has no original spawn egg; Wisp
does. Original MagicalForest Wisp10/group1..2 and Eerie Wisp3/group1 remain
dependencies of their biome units. Hellbat focus acquisition/payment/rank
upgrades are separate; all summoned entity branches and setters are present.

Client sources include both original Firebat skins, original mesh/fold/fly
animation and scale0.35/0.6, full light, smoke/flame plus nine-cell explosive
FXGeneric. Its native PARTICLE shader retains world lightmap/fog and the
original low alpha threshold. Wisp uses its two additive billboards with exact
atlas cells, default/low alpha thresholds, aspect/hurt tint, two-size pulse,
ambient FXWisp, node-sheet birth/death FXBurst and the original seven-stage
float fractal bolt, parent links, joined widths, rounded ends and two passes.
Independent client particle life RNG replaces the old VM-global Math.random
for lifetime only; native particle collisions replace removed pushOutOfBlocks.
Native particle lifecycle owns world/disconnect cleanup. All raw drawing uses
the existing VisNet state transaction, with additive shader/format overloads.

GameTest APIs are FireBatWispRuntimeChecks.fireBat/wisp/population, each native
empty template and timeout200. They use declared bare entities/player/floor/
RNG inputs and real native ticks/deaths/loot admission; they do not claim fresh
survival acquisition. JUnit checks the complete seeded bolt geometry across64
seeds; the resource contract checks the native lit particle shader linkage.
Checkpoint4d6a458 passed integrated common/Fabric production, native GameTest
and clientTest compilation,647 JUnit and79 Python tests. WispLightningShapeTest
passed2 tests, including64 seeded geometries/21,760 joined segments. The exact
checkpoint's212-test native server CI remains pending; no server GameTest pass
is inferred from compilation. Client appearance,
multiplayer visuals and survival acquisition remain unverified until inspected
game evidence; resource/class loading is not visual acceptance.
