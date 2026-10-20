# Primal focus on native wands and staffs

Authority is original TC4 4.2.3.5 `ModelWand`173–210 and
`ItemFocusPrimal` depth icon/color declarations in the private external oracle.
The standalone focus remains its original single item icon. This change applies
only when the actual native wand contains ItemFocusPrimal.

The selector now has18 rods ×4 caps ×11 focuses =792 ordered overrides. Primal
uses exact binary fraction.984375 between Pech and Shock. The generator creates
72 new rod/cap Primal composites and two dedicated shape parents; every earlier
model file is retained byte-for-byte except the updated selector. Their original
rod/cap elements and display transforms remain intact through body-only shape
parents. A dedicated direct-texture renderer draws both focus cubes. No generic
opaque focus cube covers the depth.

Depth geometry follows the original cube: wand scale.16 and translationY−.09;
staff scale.165/.1765/.165 and translationY−.15. Native body models already
establish top-cap centers at15.25/16 and28.15/16, so those are the anchors for
the original X180 item orientation. Depth bounds in model pixels are
wand[6.72,15.41,6.72]..[9.28,17.97,9.28] and
staff[6.68,29.138,6.68]..[9.32,31.962,9.32]. The ambient-lit inner cube uses all
six full-face UVs from the retained6×180 PNG, advancing the original36-entry
mcmeta sequence including duplicate frames. Its phase follows local player
ticks, with zero when no player exists. This replaces the removed old shared
atlas animation phase. The existing atlas filter remains intact: adding this
6px sprite would lower mipmaps for every block. Direct texture/frame UVs keep
global mipmap4 and all original PNG/mcmeta bytes unchanged.

The outer shell is a real separate six-pixel ModelPart cube using original
`textures/models/wand.png`, original focus RGB10854849 and alpha.6. Wand uses
scale.5. Staff translatesY−.0475 then scales.525/.5525/.525. Original cube UVs
were constructed with32×32 dimensions before the later setTextureSize64×32 and
mirror assignments; the native cube retains those effective UVs.

Mapped Minecraft1.21.1 `ItemRenderer.render(ItemStack,ItemDisplayContext,boolean,
PoseStack,MultiBufferSource,int,int,BakedModel)` has one final PoseStack.popPose
after its selected-model rendering. A client-only mixin renders the two cubes
immediately before that call, preserving actual GUI/held/dropped/frame display
transforms. It neither cancels vanilla rendering nor changes other items/foci.

Original `int(195 + sin(playerTicks/3)*10 +10)` lightmap coordinate195..215 is
retained as the actual tick pulse. Modern integer lightmap sampling cannot
reproduce the former continuously addressed fixed-function lightmap. The
bounded native adapter therefore renders fullbright/emissive and multiplies
outer RGB by pulse/240, keeping alpha.6 independent. This preserves visible
pulsation independent of ambient lighting, but does not claim exact old lightmap
gamma/color math. Native translucency/buffer sorting replaces old GL blend calls.

The resource test now checks all792 native selector combinations, original
binary fractions, inherited body geometry, the renderer's actual depth bounds,
original animation sequence/wrapping and pulse golden values. A staging generation verified exactly74 new
models and only wand.json changed among older models. Compilation, native client
startup and inspected rendered visuals have not yet been verified for this
change. Server tests cannot establish client appearance or transparency parity.
