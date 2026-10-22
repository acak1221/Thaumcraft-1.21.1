# w8blocks — four renderer gaps implemented, frames pending

Oracle root: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`.
No Gradle, JUnit or client runs performed by this agent.

## Cosmetic stairs

Oracle `thaumcraft/common/blocks/BlockCosmeticStairs.java:38-40` delegates `side+100`
to `BlockCosmeticSolid.java:157-162`. The world model now remaps each quad's UVs to
the sprite selected by the hash for its **world-facing** direction, including the
unculled interior stair faces, instead of painting all faces with the up-face index.
Shared implementation: `common/src/main/java/thaumcraft/client/renderers/block/PositionBlockModels.java`;
Fabric registration: `fabric/src/main/java/thaumcraft/fabric/client/FabricPositionBlockModels.java`.
The position is sampled at rendering, not inferred from the placement property.
Port references: `PositionBlockModels.java:26-29,37-44,65-83` and
`BlockCosmeticStairs.java:117-120,126-129` (paths under `common/src/main/java/thaumcraft`).

Frame acceptance: at `(0,0,0)`, down/up/north/south/west/east use
`es_4/es_1/es_2/es_3/es_4/es_1`. Inspect all stair shapes, both halves and all
four facings, including the riser and tread faces; piston relocation must select
the destination's face hashes. No frame has yet been inspected.

## Custom ore

Oracle `thaumcraft/client/renderers/block/BlockCustomOreRenderer.java:54-70`
draws base, then unshaded coloured overlay at **Math.max(bb,160)**, then (only
when `field_151443_J > 1`) the coloured stone pass inset `.005..995` at **bb**.
The ledger's “fast graphics” interpretation is wrong: the FML 1.7.10 MCP mapping
at <https://raw.githubusercontent.com/MinecraftForge/FML/1.7.10/conf/fields.csv>
line **4214** maps that field to **anisotropicFiltering**. This is not the mipmap
level or Fancy/Fast graphics option. `BlockRenderer.java:105-123` does not reset
the overlay colour before the inset stone faces, so the stone remains tinted.

Port: `common/src/main/java/thaumcraft/client/renderers/block/LegacyBlockQuads.java:35-59`
now retains that order, light and tint; `OreRenderRules.java:3-15` retains the
packed-integer max (not a per-channel clamp or fullbright). The shared terrain
adapter explicitly sends the light to VertexConsumer, rather than assuming
vanilla reads vertex lightmaps. `common/src/main/java/thaumcraft/common/mixin/client/OreAnisotropyMixin.java:20-36`
samples the **actual block atlas anisotropy** on the render thread, caches it for
terrain workers, restores texture binding and rebuilds terrain when it changes.
Vanilla 1.21.1 uses AF=1; an AF-enabled renderer gets the second pass. This is the
effective GPU-state counterpart of the removed legacy vanilla option, not a new
claim that the modern Graphics menu controls AF.

The inset uses the byte-exact 1.7.10 stone, extracted using Python ZipFile.read
of `assets/minecraft/textures/blocks/stone.png` from the official client:
<https://launcher.mojang.com/v1/objects/e80d9b3bf5085002218d4be59e668bac718abbc6/client.jar>.
Downloaded client SHA1 checked: `e80d9b3bf5085002218d4be59e668bac718abbc6`.
Output: `common/src/main/resources/assets/thaumcraft/textures/blocks/legacy_stone.png`,
223 bytes, SHA256 `489928bc2549424b7f8d979b7983e4398789dbff80b8c61f68368a02235d6927`.
Ownership recorded in `THIRD_PARTY_NOTICES.md`.

Frame acceptance: all six infused ores in a dark room retain the original colour
and at least packed brightness160 on the unshaded overlay, while the base remains
normally lit. At block-atlas AF=1 there is no inset; AF=2/4/8/16 adds the original
tinted stone at `.005..995`, **after** the overlay and at ordinary bb. Inspect
nearby opaque neighbours for hidden-face suppression. Fast/Fancy alone must not
toggle the pass. AF-enabled renderer integration and its live-change rebuild are
implemented but **not runtime-verified** here.

## Gas / goo

Oracle `thaumcraft/client/renderers/block/BlockGasRenderer.java:32-65,87-123,140-188,219-267`:
solid-support gate, nine samples, four averages, `.875`, centre1 fast path,
`.001F` subtraction only when the surface is drawn, inverted rising surface,
Y shading `.5/1`, N/S `.8`, W/E `.6`, half-span side UVs and ordinary world light.
Foreign quanta is **-1** (`-.109375F`), and an all-negative corner remains **NaN**.
Neither quirk was changed in `GasSurfaceRules`.

Port: `common/src/main/java/thaumcraft/client/renderers/block/GasWorldQuads.java:19-101`
samples actual neighbours during terrain meshing and consumes the existing pure
rules. There is no need for a vanilla fluid corner hook: these are finite **block**
models, accepted by the existing shared `ModelBlockRenderer.tesselateBlock`
adapter and the Fabric model adapter. The former “impossible” assertion is not
accepted. Fallback without the required solid support draws a **full cube**, not
the old partial static model. The native FULL_HEIGHT state is not used as a
substitute for sampling corners.

Both flux blocks use `Config.java:917`'s MaterialTaint. `javap` of the original
JAR's `thaumcraft.common.config.MaterialTaint` confirms it extends Material,
overrides isSolid=false and does not override isLiquid. Forge's
`BlockFluidBase.java:373-381` therefore returns -1000 for its flow direction:
the surface uses the still UVs, not an invented rotated flow texture.
Forge source inspected at the already-pinned 1.7.10 implementation.

Frame acceptance: place goo on a solid floor, metadata0 at `(x,y,z)` and metadata3
immediately east, other horizontal cells air. Western corners before offset are
`.02734375`, eastern corners `.13671875`; exposed surface heights are approximately
`.02634375/.13571875`. Under a solid ceiling, gas uses `1-height` for the same
samples. Isolated metadata7 gives an exposed `.874` goo surface (gas `.126`).
Remove the required supporting solid cell: the oracle's full-cube fallback must
replace the partial surface. Verify N/S80%, W/E60%, goo bottom50%, gas bottom100%
and top100%, with no extra diffuse or AO multiplication. Verify same-fluid side
culling, fluid-above/below full-height samples and half-width side UVs.

## Taint cube and shared fibre geometry

Oracle `thaumcraft/client/renderers/block/BlockTaintRenderer.java:35-65` draws the
cube in pass0 for metadata0/1/2 and the white, brightness200 fibre overlay in
transparent pass1 for metadata0/1 only. Texture-side arguments are the **opposite**
of the visible face: down/up/north/south/west/east -> `1,0,3,2,5,4`.
`BlockTaintFibres.java:136-141` chooses blank95%, otherwise one of three sheets.

Port: `LegacyBlockQuads.java:25-29,60-70`,
`TaintOverlayRules.java:3-8` and
`common/src/main/java/thaumcraft/client/ThaumcraftTaintBlocksClient.java:16`
now put the overlay on CRUST/SOIL, with a translucent render layer and the
original sparse rule. FLESH receives no overlay. The full neighbour-aware fibre
geometry moved from the Fabric-only model to
`common/src/main/java/thaumcraft/client/renderers/block/TaintFibreWorldQuads.java:19-64`;
both loaders use it. The obsolete Fabric-only model and its separate bootstrap
registration were removed after auditing its plane/cross/grass-offset behavior.
The Fabric adapter now only emits the shared quads.

Important: the original **also** has a distinct valid overlay on the FIBRES
block (`BlockTaintFibreRenderer.java:64-89`); that overlay was retained, not
incorrectly removed. Its `.98` planes and glowyTaint gate differ from the cube
overlay, which is full-cube and unconditional. Base fibre planes are `.995`.

Frame acceptance, **both loaders**: CRUST and SOIL at `(0,53,0)` show overlay1
on the top face (texture-side0); FLESH at the same position does not. Disabling
glowyTaint hides the type0 free-fibre overlay but does **not** hide the original
CRUST/SOIL pass. Hidden cube faces are not drawn. Supported free fibres retain
their inward-facing planes, grass offsets, crossed stalks and sparse white
brightness200 overlay; no free-fibre plane attaches to a taint-family support.

## Verification and remaining acceptance

- Whole `common/src/main/java`: **1429 Java sources**, `javac --release 21
  -proc:none`, **exit0**, only deprecation/unchecked notes. No Gradle was run.
  Argument file/log: ignored `.tmp/w8-javac.args`, `.tmp/w8-javac.log`.
- Classpath: actual remapped Minecraft1.21.1 merged jar from
  `~/.gradle-leader/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/**`,
  `common/build/classes/java/main` (absent in this fresh worktree), modules-2
  dependency jars (excluding sources), and a named-remapped Architectury API.
  A **copy** of the vanilla jar was widened for the three checked-in common AW
  entries and Fabric's transitive `BlockEntityType$BlockEntitySupplier` access
  widener. Cached originals were not changed. No source/API stubs were used.
- FabricPositionBlockModels and the new JUnit source: additional `javac` exit0,
  using named-remapped Fabric renderer/model-loading/api-base jars.
- New `common/src/test/java/thaumcraft/client/BlockRendererFinalParityTest.java`:
  five JUnit5 tests, original packed light/AF/insets, cube metadata/texture-side,
  negative/NaN/shading fluid rules, and six distinct origin stair-face indices.
  **Authored and compile-checked, NOT executed.** No Block is constructed.
- Asset verifier initially passed `missing_references: 0`; final results below.
- All four units are implemented against the oracle, not claimed visually
  accepted. Remaining work is leader-run client frame acceptance and integrated
  JUnit/loader runs. No hard API limitation is claimed for these four units.
