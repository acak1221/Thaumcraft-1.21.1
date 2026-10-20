# Angry / Furious Zombies — original contract and native port

Specification: immutable TC4 4.2.3.5 private oracle `EntityBrainyZombie`,
`EntityGiantBrainyZombie`, `RenderBrainyZombie`, `ConfigEntities:170-177`,
`ConfigAspects:88-89`; vanilla 1.7.10 dependency `EntityZombie`,
`EntityLivingBase.onDeath`, `ModelZombieVillager`; Forge 1.7.10
`WorldChunkManager.java.patch` defines the static allowed-biome list.

Angry Zombie is the display name of BrainyZombie; there is no separate third
AngryZombie class. Native IDs are `brainy_zombie` and `giant_brainy_zombie`.
Brainy has25 health,5 damage,40 follow range, original reinforcement base0 and
`min(20, zombieArmor+3)`. Its three world-random booleans each emit one rotten
flesh. Brain roll is a separate `nextInt(10)-Looting<=4`, with height1.5,
including environmental deaths. Ordinary zombie's separate brain hook must
not run for either new type. Native equipment drops remain supported.

Giant has60 health,7 initial damage,15 base XP, leap0.4, anger1.0. Every hurt
attempt, including an invulnerable/refused one, increases anger0.1 capped2.
Living update subtracts0.002 only when anger>1, without clamping the possible
undershoot below1; base damage becomes `7+(anger-1)*5`. Anger is synced and
not persisted. Twelve independent world booleans (two consecutive loops of
six in original) each emit TWO flesh, maximum24, followed by the same brain
roll with height2. Original rare gate, inherited from vanilla1.7, is
`entityRandom.nextInt(200)-playerLooting<5`; it is not a floating2.5%+1% rule.
Brainy rare choice is iron/carrot/potato; Giant adds thaumium/amber in place
of iron and uses four equal choices. The earlier guardian eye gate is fixed
to the same exact integer rule in this unit.

The native1.21 convenience ItemEntity constructor consumes two WORLD random
doubles for motion, whereas original1.7 EntityItem uses independent Math.random
and casts each motion component to float. Using that constructor inside the
flesh loop changes the following flesh/brain rolls. OriginalMobLoot.spawn now
uses explicit native item motion with the original independent float draws;
Brainy/Giant and Guardian route custom/equipment emissions through it. A native
runtime assertion checks admitted item motion and both untouched gameplay RNG
streams before the existing actual-death maximum-flesh assertions.

Important retained quirk: original Zombie.setSize(float,float) is final and
updates STORED width/height after initialization, without applying the box.
Giant's constructor/rage update therefore leave a fresh adult's ACTUAL box
at0.6x1.8. A child-size setter applies the current stored `(1.2+anger)` scale.
This was verified by executing the original vanilla1.7 Zombie class, not only
reading decompilation. Outside-repo oracle output: initial0.6x1.8; ctor stored
2.2 actual0.6x1.8; rage stored2.298 actual0.6x1.8; explicit child-size(false)
actual1.3788x4.1364. Rendering scales only by anger, a separate original quirk.

The 1.21 base uses ZombieVillager with a separate synced `IsVillager` flag to
retain the old zombie class's5% curable villager variant. Normal Brainy refuses
the apple; the variant uses native weakness/apple payment,3600..6000 conversion,
beds/bars acceleration, NBT and actual villager output. Both use zombie sounds
and original torso/head/nose texture coordinates. Native villager professions,
curing advancement/gossip, equipment/enchantment processing, pathfinding and
modern collision APIs are explicit vanilla-platform adaptations. They are
not a claim that vanilla1.21 internals equal all vanilla1.7 behavior.

Natural Brainy population is weight10/group1 in the original Forge allowed
forest/plains/taiga/jungle families with existing monsters; removed hill biome
IDs collapse to their current base biome. An explicit datapack tag preserves
that bounded list instead of adding all Overworld biomes. Predicate preserves
old sky draw32/local brightness draw8 and thunder subtraction10. Giant Eerie
weight8 and Sinister node spawning depend on the later biome/node-type unit;
no invented vanilla substitute is registered for those missing sources.

Integrated common/Fabric production, native GameTest and clientTest compilation
passed for checkpoint4d6a458, alongside647 JUnit and79 Python tests. The complete
native server run of that exact checkpoint is pending CI; these compile results
do not establish the actual death/cure/spawn assertions passed. Entity scanning
aspects and exact JARBRAIN clue names are
integrated by the separate EntityAspectBindings owner. Client rendering and
natural survival acquisition still require inspected gameplay evidence.
