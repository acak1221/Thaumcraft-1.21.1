# Original taint blocks and falling crust

The private TC4 4.2.3.5 source is the specification: `BlockTaint`1–355,
`BlockTaintFibres`1–360, their two item classes, `MaterialTaint`,
`EntityFallingTaint`1–207, both original block renderers, `RenderFallingTaint`,
`ClientProxy`1037–1058 and `FXDrop`/`FXBreaking`. No decompiled source is bundled.

The actual native family consists of crust0, soil1, the already accepted
`flesh_block`2, and one fibre block with TYPE0..4. Existing flesh recipes,
golem container recognition and block identity remain authoritative. Its
already accepted loot still emits one count9 rotten-flesh stack; the original
emitted nine separate count1 drops. That pre-existing difference remains a
separate targeted follow-up. There is no second flesh block or replacement recipe.

Crust/soil retain hardness1.75/1.5 and native resistance6, the translation of
old `setResistance(10)`'s internal30 divided by5. Fibres retain hardness1,
resistance3, zero collision/support, original first-sturdy-face selection,
TYPE2 light8 and TYPE4 light10. The hidden mature item4 does not replace
canonical `Block.asItem()` fibre0. All five real item placements select their
explicit subtype and require the actual TAINT surface biome column.

Ecology preserves original short-circuit order and draws: biome offset3/3,
chance(rate*5), six-neighbour family count; spread candidate3/5/3 with Y−3..+1;
fibre type10, optionally10 and12; at least2 family neighbours for log/cactus/
gourd conversion and3 for soil/sand/grass/clay; failed-spread-only stalk
birth and mature-spore presence recovery. Material predicates use native log,
dirt and sand tags plus the old vanilla material members because native1.21
removed the material enum. A modern tagged physical container with contents
is preserved instead of being silently overwritten. Unloaded cells are not
invented as air and no ecology helper loads terrain.

Actual crust attempts vertical falling, then the original one-direction
four-deep side test, then spreading/spawning/cleanup. The existing flesh alias
participates in the original shared-block equality tests. Sealed crust and
non-TAINT1/20 cleanup create actual exceptional flux-goo metadata8, nine
quanta, not ordinary full metadata7. Non-TAINT soil restores dirt at1/10.
Native contact/walking hooks apply the actual flux-taint effect with the
original player/nonplayer bounds1000/500 and100/20, duration80/160 and undead
exclusion. Flesh remains inert.

The native falling entity keeps the exact source state until admission and
its first actual tick. A canceled or reentrant ADD cannot debit it. First
tick gravity/move/friction precede source removal, as in TC4. Only one of
multiple admitted attempts can claim the source; stale attempts retire.
The old half-height yOffset is retained in its actual bounding box. The
entity owns the block after removal and persists that fact explicitly;
original `Time` remains a saved byte, so its255 wrap cannot claim a new
replacement block. It settles once, vanishes on failed placement as in the
original, ignores the three old piston states while grounded, stops on
goo metadata>=4, has no falling damage, and expires after the original
height/time gates adapted to native world height. Invalid saved material is
retained inert, never fabricated as sand. The unloaded32-cube fallback
retains the original quirk of restoring SOURCE coordinates instead of its
computed landing. It removes before scanning and never loads missing chunks.

Original positive `biome_taint_spread` was not bounded in Config. Its
`rate*5` int overflow could crash. Representable bounds retain exactly the
old nextInt schedule; larger positive bounds use long multiplication and
unbiased rejection on raw nextLong. This is a finite crash correction,
without resetting the public setting or consuming long RNG at normal rates.

Fabric's new dynamic model reads actual sturdy neighbours, including direct
worldgen fibre0: base planes exclude solid-family neighbours, type0 glowing
nodules use original per-side int-overflow hash/95% blank gate and packed
brightness200, grass1/2 uses the original position offset, and stalk3/4 uses
the actual vanilla1.7 `func_147746_l` crossed-squares path, confirmed by
untouched vanilla bytecode. No unreachable solid-block overlay renderer is
activated. Native biome grass tint and the native item tint7160201 apply.
Falling crust renders as a centred native block model. Actual particle-engine
effects retain drop RGB(.3,.1,.8), bob40, gravity.06, lifetime64/(Math.random
*.8+.2), original jar pass-through, and ten falling slime-ball fragments with
RGB(.1,0,.1), scale.4, gravity1 and life66/(worldFloat*.9+.1). Removed vanilla
drop cells113/112/114 use their modern drip_hang/drip_fall/drip_land sprites.

Validation prepared: five meaningful pure JUnit methods cover conditional
lottery boundaries/call order, exposure rates, independent literal render
vectors/int overflow, exact int bound2147483645 and overflow-long rejection.
They pass direct javac/JUnit invocation in the isolated draft directory.
`TaintBlockRuntimeChecks` uses actual placement/loot/native entities/biome
storage and checks both preservation and ordinary registered ticking paths,
including canceled admission, duplicate source custody and real NBT wrap.
Its actual biome consumer independently checks default3/3/1000 draws, chance
before the adjacency gate, and overflowing429496730 with3/3 plus one rawLong
while changing the actual stored surface column to TAINT.
Compilation, native GameTests and inspected client visuals remain pending
the joint root activation and validation; no Gradle ran in this agent.
