# Original taint swarm — 2026-10-20

Implemented sources activated for the next joint Fabric checkpoint. No target
compilation, native runtime acceptance or inspected client is claimed yet. Oracle: TC4 4.2.3.5 EntityTaintSwarm54–273,
RenderTaintSwarm, FXSwarm, ClientProxy899/931 and ConfigEntities149;
Minecraft1.7.10 EntityMob/EntityCreature supply inherited legacy behavior.

The native registered mob retains original2×2 dimensions, health30, attack2,
XP5, hostile peaceful despawn, full light value1, no entity pushing, no walk
sounds/fall injury and underwater breathing. It keeps ordinary native living
movement/gravity. After vanilla movement, BOTH server and client multiply Y
motion by the exact promoted FLOAT.6. No generic flying/wander/melee goal is
substituted for its original legacy action.

The original Creature action precedes swarm steering: natural acquisition uses
strict12-block closest vulnerable player, including original sneak/invisibility
scaling; summoned swarms acquire no ordinary player. Accepted damage retaliates
against its living attacker after native damage. LOS gates bites; missing/dead
and creative targets follow original ordering. Original path/idle decision and
random draws are preserved. Modern PathNavigation supplies actual collision
geometry for the removed1.7 pathfinder; WAIT move/jump controls are suppressed
so they cannot erase the original forward input. Inherited brightness-based
idle ageing and old path-vs-idle counter branches remain active.

Targetless summoned actions attempt generic5 self damage, retaining ordinary
native hurt immunity rather than inventing guaranteed damage every tick.
Waypoint generation retains original integer casts (including negative-coordinate
truncation), five random-coordinate draws (bounds7,7,6,7,7),1-in30 refresh, distance²<4,
original taint-biome restriction, light-opacity column height+8 ceiling and
original lower-bound meaning. A forced native world-height adaptation shifts
Y>=1 to Y>=minBuildHeight+1: old min0 admits1, native min−64 admits−63.
The exact boundary below each world minimum is rejected. Height lookup starts from native WORLD_SURFACE but
scans down to actual positive light opacity: transparent glass cannot become a
new ceiling; leaves/liquid still contribute. Empty columns use native minheight.
Native3D biome lookup uses the actual waypoint. Idle horizontal steering uses
literal.015000000014901161; pursuit X uses.025000000149011613 while pursuit Z
uses.02500000001490116. Vertical steering preserves promoted FLOAT.7 and.1;
forward input remains.1 and yaw wraps to actual velocity. These constants are
not replaced with rounded generic movement speeds.

A bite requires original cooldown<=0, strict distance<3 and vertical box
overlap. Summoned bites mark the victim recently hit100 BEFORE attempting
native damage, including refused hits. Cooldown is10+entityRandom.nextInt5.
Actual successful damage adds Weakness100/amplifier0; damage refusal adds none.
Every admitted attempt restores the exact prior victim velocity and clears its
hurt mark, then plays swarmattack volume.3/pitch.9+worldFloat*.2.

Inherited original EntityMob uses VANILLA mob damage, not the public but unused
DamageSourceThaumcraft.causeSwarmDamage API. Native melee/enchantment/postattack
hooks remain active. A detached attribute copy replaces modern Strength/Weakness
modifiers with original1.7 Strength130%×(amplifier+1) MULTIPLY_TOTAL and
Weakness−.5×(amplifier+1) ADD before the original attribute FLOAT conversion;
there are no tentacle-specific manual effect additions. Live attributes never
change during planning. Modern seeded sound transport supplies an explicit
cosmetic seed, avoiding a new world-nextLong draw inside the original gameplay
RNG sequence; the silent original empty-string ambient sound maps to native
SoundEvents.EMPTY with ordinary pitch draws.

Flags retain BYTE bit1 for summoning and the original unusual false mask,
which clears BOTH low bits while preserving other unknown bits. `damBonus`
saves/loads as a signed BYTE, including130→−126; setting or reloading it does
not recalculate attack damage. Original inherited native attributes persist
separately. Waypoint/path/cooldown/visual particle references remain transient.

Death retains one independent WORLD nextBoolean50% chance of exactly ONE
ItemResource11 tainted goo at half the mob height; no tendril is invented and
no recent-player/Looting multiplier is added to this drop. The original
inherited empty rare-drop hook's entity-random roll remains after admitted
recent-player deaths. Native doMobLoot gates all actual death output. The
existing OriginalMobLoot.spawn adapter avoids the modern convenience entity
constructor's extra world draws while retaining a real native ItemEntity.

Spawn first checks raw local light against entity nextInt7, BEFORE inherited
peaceful/sky nextInt32/raw nextInt8 checks. Original thunder substitution10 and
original `.5 - worldLightBrightness` path weight remain. The native collision
check rejects solid obstruction and liquid without inventing modern monster
solid-floor requirements. Taint biome population wiring belongs to the shared
biome/registry owner; the predicate itself does not add an original-absent
biome restriction to command/spawner admission.

The original renderer draws no body, shadow or nameplate. Real FXSwarm particles
provide the visible mob, shared with native spores through TaintSwarmVisuals.
Actual desired counts are50/25/10 for ALL/DECREASED/MINIMAL particles settings
(original particleCount25 is50/25/0, bounded by10). Only one stale particle is
removed and at most one particle is added per mob tick; speed.22/turn15/gravity.08
are exact. Shared particle motion, atlas row4/columns7..14, hurt tint, death
fade, original packed particle light240 (block15/sky0) and buzzing are owned by the coherent spores/FX unit.
No new texture or placeholder geometry is added for the swarm.

TaintSwarmRuntimeChecks exercises actual native registry factories, movement,
breathing, flags/save/attribute quirks, legacy steering, actual natural/summoned
AI, real damage/effects/no-knockback/recent-hit and cooldown, original Strength,
real death/drop RNG and registered spawn admission. Pure lower-bound vectors
cover original0→1 and native−64→−63, including rejection at the actual bottom. Its protected action call
is a test-only reflective invocation of the actual native method for declared
motion vectors; ordinary whole native ticks independently cover movement and
bites. Raw actors/terrain, vector/seed inputs and trusted save restoration are
explicit fixtures, not survival acquisition or inspected client evidence.
