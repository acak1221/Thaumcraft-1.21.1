# w8world — final four partial units

Worktree `wt/w8world`, branch `agent/w8world`. Oracle: byte-exact
`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`. No Gradle, client, server or
JUnit execution in this lane. Runtime/visual acceptance is delegated to the leader.

## World generation and regeneration — implemented

Oracle `ThaumcraftWorldGenerator.java:130-139,548-555` dispatches Nether to a
wild-aura-only pass. `:149` rejects Nether totems before consuming randomness.
`:190-213` rolls rarity 36, X/Z in the chunk and the first uncovered cave height.
The native feature previously rejected Nether both at entry and in its node factory.

Changes: `ThaumcraftNormalNodeFeature` accepts Nether, calls the wild pass directly
(never the scattered-temple branch), and allows its shared factory there. The biome
tag now contains `#minecraft:is_nether`, so the existing Fabric biome modification
and NeoForge biome modifier actually schedule the feature. `ChunkRegeneration`
dispatches Nether before any surface vegetation/ore/temple/structure draws and skips
the End; its existing ten-chunk drain and original seed are unchanged.

Verified by source inspection: `AuraGenRules.java:61-115` restricts scattered starts
to desert/jungle pyramids and swamp huts and to the owning chunk. Native
`ThaumcraftNormalNodeFeature.java:64-78` places the node at surface+3, consumes the
wild rarity draw, and preserves the original overwritten scattered flag
(`ThaumcraftWorldGenerator.java:413-419`). Native `AuraGenRules.java:130-144` and
`ThaumcraftTotemFeature.placePass` preserve the wild/totem exclusivity.

The prior structure replay fix is present: `ChunkRegeneration.placeStructure` passes
the SAME random to `WorldGenMound.placeSelected`, `EldritchRingFeature.placeSelected`
and `WorldGenHilltopStones.placeSelected`; these bodies do not replace it with the
fresh-generation seed. Fresh entry points still own their separate seed.

Acceptance (real world): in a fresh Nether with aura enabled, explore enough chunks
to observe wild nodes inside caves, not on the roof. With regenAura enabled and a
missing chunk marker, replay the same candidate Nether chunk using the oracle seed:
the first draws must be the wild rarity/X/Z/height, with no tree, ore, temple or
totem draws and no terrain regeneration. Changing only regenStructure must not
place Nether nodes/totems. Queue 11 candidates: ten drain on the first tick and one
on the second. End replay must change no blocks. No out-of-region chunk request or
server crash is acceptable.

Tests authored, NOT executed: `NetherAuraParityTest` (dimension dispatch and both
original configuration gates). Existing scattered/exclusivity and regeneration
stream tests reviewed, not rerun. New tests construct no Blocks.

## Linked mirror — implemented, frame pending

Oracle `TileMirrorRenderer.java:52-127` and the other five axis variants define
six inset (.1875..8125) planes at .01/.99, sixteen layers, distances 65 then 15..1,
texture scales .125/.5/.0625, seed 31100 per face, RGB fade (NOT alpha fade),
700000/250000-ms scrolling and the original layer rotation.

Important correction to the supplied description: the actual oracle's GL constants
are S/T/R **OBJECT_LINEAR (9217)** with OBJECT_PLANE (9473), and Q **EYE_LINEAR
(9216)** with EYE_PLANE (9474), not the reverse. Native `MirrorProjection` preserves
that algebra and matrix multiplication order. `LinkedMirrorField` unprojects the
near-plane centre through the actual view/projection matrices, computes constant
eye-linear Q for each face, and supplies homogeneous S/T/Q to `linked_mirror.vsh` /
`.fsh` (`textureProj`). Multiplying the homogeneous tuple by f9+distance avoids
division by zero at a layer distance without changing its projected coordinates.
The only suppressed case is an edge-on plane (zero projected area).

The shader is registered in Fabric and NeoForge's existing core shader callbacks.
The old shrinking/depth-offset `ModelTunnelField` is no longer the linked mirror's
renderer. Other consumers of that approximation are deliberately outside this lane.
The shared immediate draw helper now accepts the original ONE/ONE inner blend;
outer uses SRC_ALPHA/ONE_MINUS_SRC_ALPHA. Native render-state restoration is reused.

Acceptance (real frames): inspect linked item AND essentia mirrors in all six
orientations, at normal and oblique angles and while strafing/rotating the camera.
The aperture must remain one inset square, not sixteen nested shrinking squares;
the field must move projectively with the view and scroll at the original rate.
The outer layer must be dim RGB .1 and inner particles additive; two mirrors with
the same orientation must share the same per-face colour sequence. Check third
person, hurt/view bobbing, resource reload, and a mirror exactly 15 blocks from the
eye: no shader error, NaN flash or leaked blend/depth state. Unlinked pane and
instability jitter must remain unchanged.

Tests authored, NOT executed: `MirrorProjectionParityTest` (four pure matrix,
layer-value and homogeneous-rescaling checks). No GL context or Blocks constructed.

## Bone bow — implemented, frame pending

Oracle `ItemBowBoneRenderer.java:91-118` uses EQUAL depth, SRC_COLOR/ONE, tint
(.38,.19,.608), .125 texture scale, rotations -50/+10, 3000/4873-ms opposite scrolls.
Native `BoneBowGlintRules` preserves the constants/matrix order; `BoneBowGlint`
substitutes two custom consumers for native foil only inside the original held
renderer. Independent fixed buffers preserve both passes and flush the base before
EQUAL-depth glints. The restored held pose is unchanged. GUI, ground and ordinary
first-person vanilla fallback are unaffected (original handles EQUIPPED only).

The glint texture was extracted byte-for-byte from Mojang's official 1.7.10 client
JAR (SHA1 `e80d9b3bf5085002218d4be59e668bac718abbc6`) at
`assets/minecraft/textures/misc/enchanted_item_glint.png`, retained under the TC
namespace as `textures/misc/bone_bow_glint.png`. Official `bly.a(bmh,float,float,
float,float,int,int,float)` bytecode proves front UV x=0->u=0, y=0->v=1;
the native consumer uses that orientation, not the atlas sprite UV.

Acceptance (real frames): hold an enchanted bone bow on a visible third-person
player/mob, idle and at all three pulling stages. Two independently moving purple
patterns must cover only equal-depth bow geometry, not its transparent rectangle;
their directions/angles and 3.000/4.873-second wraps must match the oracle. Compare
an unenchanted bow (no glint), GUI and dropped bow fallback, and both player hands.
No vanilla third glint, missing base bow, buffer exception or leaked depth state.

Tests authored, NOT executed: `BoneBowGlintParityTest` (three pure tint, period and
matrix-order tests). No GL context or Blocks constructed.

## Verification (in progress)

Whole `common/src/main/java` javac passed once (1,426 files before final helper
extraction). Final recheck will be recorded below. Uses Java 21, remapped cached
1.21.1 merged jar, locally remapped Architectury API, modules-2 binaries, and exactly
the project AW + Fabric transitive BlockEntitySupplier widening (no stubs). The
worktree starts without `common/build`; full sources are compiled, not borrowed
classes. Local logs/argfiles/tools stay ignored under `.tmp`.

`convert_assets.py --verify`: **missing_references: 0**.
