# Taintacle and Small: original contract and native adaptation

Activated with the coherent Taint/biome/node group. Target compilation and native runtime execution are pending; no client inspection is claimed here.
TC4 4.2.3.5 EntityTaintacle/Small, actual ClientProxy renderer registration,
models/entities/ModelTaintacle + ModelRendererTaintacle, RenderTaintacle,
ClientProxy.tentacleAriseFX and FXBreaking are the gameplay/visual oracle.
Official Mojang1.7.10 rv/rs (not a modern potion assumption) supply inherited attributes.

## Gameplay

The main hostile has50 health,7 attack, .66x3 body and .25 pick radius; .25 was
not eye height. Small has8 health,2 attack, .22x1 body, no XP or custom resource
loot. Native eye heights preserve old LivingEntity height*.85. Original horizontal
and positive vertical movement are removed, downward motion remains possible.
The native monster has no wandering/pathing goal; retained targets turn at most5deg
per update. The nearest non-tainted LivingEntity in the point AABB height*(6,3,6)
is acquired without a new LOS/radial/player-only filter. Agitation uses strict
FLOAT-computed (height*7)^2. Melee requires native LOS, vertical overlap and
distance<=height; attack cooldown20. Distant grounded targets over real taint
material/taint biome create Small with40..59 cooldown and original two-float X/Z
offsets. Actual >16-block attributed damage can also retaliate, independently of
melee cooldown. A publication guard closes native ADD-callback reentry before the
one child enters the world. Existing dead/unloaded/foreign worlds are not revived.

The original2D biome lookup is the actual surface-holder adapter and mutation is
BiomeColumns.set, preserving the native4x4 quart-column limits. The old Eldritch
branch remains keyed to thaumcraft:eldritch, but that unregistered biome is not
invented or aliased to another biome. Taint material means actual fibres, crust,
soil and existing flesh_block alias. Existing shock fields count as old Airy air
where the original explicitly queried air; their native random-ticking nonAIR
representation otherwise remains intact.

Every20 entity updates outside taint, original protected out-of-world damage1
bypasses generic public hurt grace. Small lifetime is transient200 and uses
post-decrement: protected drowning10 begins on update201. Full native NBT keeps
other entity data; it does not invent persisted lifetime. Main resource loot is
exactly one50/50 goo/tendril at height/2. Externally supplied native equipment still
uses normal native death equipment handling for either size.

Original natural spawning needs fibres type0 or soil *at the feet*, the taint
surface biome, no other Taintacle subclass in AABB(24,8,24), nonpeaceful and original
sky32/local8 random light checks. The inherited creature path weight is
.5 - native getLightLevelDependentMagicValue: nonlinear light brightness, not
blocklight/15. Obstruction checks actual liquid and native collisions without a
support-floor condition. Root placement registration must use NO_RESTRICTIONS,
not ON_GROUND. Small refuses natural spawning. Direct retaliation does not call
natural spawn rules.

## Damage inheritance

Official1.7 rv.g Strength registers op2 and rs overrides its amount to
1.3*(amp+1), rather than the apparent rv constructor3.0. Weakness rv.t is op0 and
rs supplies -.5F*(amp+1), rather than its apparent constructor2.0. The helper
replaces only these actual native active-effect modifier IDs on a detached
AttributeInstance copy, preserving unrelated modifiers and all live attribute/
effect state. mobDamage returns the original attribute; tentacleDamage separately
adds Java(3<<amp), then subtracts Java(2<<amp), including original shift overflow.
Main I-vectors: clean7; Strength19.1; Weakness4.5; both15.950001F (raw bits0x417f3334 after separate FLOAT additions).

Actual native enchanting, normal armor/resistance, knockback, equipment and
post-attack effects run through modern EnchantmentHelper after that reducer.
Foreign modern enchantments retain their modern definitions; this is not a claim
that Sharpness1.21 values or every third-party effect equal the1.7 registry.
Native spectator exclusion, modern damage/death bookkeeping and collision engine
are forced engine adaptations. No full legacy global combat engine is introduced.

## Client

Regular renderer shadow.6,length10; Small shadow.2,length6. The64x64 original
texture, cubes/UVs, nested .88 scale BEFORE each child translation, separate
fullbright leaf orb/head, original FLOAT sine joints and translucent rendering
are implemented. Empty native ModelPart wrappers preserve the unusual transform
order; cube traversal chooses per-leaf packed light without a global state leak.
Original integer-age rising transform, un-clamped float flail crossings and
height/3 scale remain. Giant boss size/HUD is a later actual boss unit, not a fake
subclass here.

Arise FX generates6/2 purple slime-item quarter sprites per qualifying early
client update, original PI*height angle/factor distribution, half scale,
66/(random*.9+.1) lifetime and age-based size/alpha fade. Support debris uses the
actual native support block/tint and independent RNG samples. Native item/block
atlases, particle physics/collision and rendering pipeline replace display lists
and global OpenGL state. No visual acceptance is claimed before a live client run.

## Shared integration

Root-owned registry supplies TAINTACLE/TAINTACLE_SMALL actual EntityTypes and
sizes/eye heights, damage type TENTACLE, existing original resource bindings.
Register native attributes EntityTaintacle.createAttributes() and
EntityTaintacleSmall.createAttributes(); spawn placement as above. Client bindings:

- TAINTACLE -> ctx -> new RenderTaintacle(ctx,.6F,10)
- TAINTACLE_SMALL -> ctx -> new RenderTaintacle(ctx,.2F,6)
- EntityTaintacle.setClientEffects(TaintacleAriseParticle::arise)

OriginalTaintCombat.mobDamage(LivingEntity) and tentacleDamage(LivingEntity) are
public shared helper APIs; devices' actual Swarm uses only mobDamage then native
normal mob damage/enchantment handling. The originally unused causeSwarmDamage
API does not change the Swarm's actual damage identity.

Six JUnit methods distinguish real potion operations/Java overflow, strict
agitation, float overshoot, integer-age rising and stem structure. Three native
runtime wrappers, each timeout140, are TaintacleRuntimeChecks.combatAndRootedAi,
retaliationLifetimeAndLoot and noInventedFloorOrPotionMutation. They use actual
registered monsters, native potion/attribute operations, AI/LOS/normal damage,
retaliation,201 ServerLevel.tickNonPassenger updates, native save/reload with an
explicit distinct clone UUID, actual drops and collision/liquid gates. The high
lane, potion/equipment/biome inputs and synchronous native entity ticker are
fixtures, not acquisition, elapsed-world-time or inspected-client evidence.
Palettes are snapshot/restored with native section codecs and resend; all owned
actors/entities/extra soil are retired/restored. No fake BE or native ticker type
substitution is used. Full positive natural spawning/light RNG and client visuals
remain a later integrated gameplay check; runtime material/biome/collision
refusals and absence of a floor gate are covered here.
