# Wave 7 — w7blocks

## Outcome and scope

Two of five assigned gaps closed in code: `BlockCandleRenderer` and
`EldritchCrustBakedModel`. Three remain `partial`: custom ore, gas, taint.
No Gradle, JUnit execution, client, GameTest or visual acceptance was run by this lane.
No original assets were changed. No other worktree was read or modified.

The initial assumption that the rules were wired was false: the three rule classes
had no production callers, and the crust baked model had no registration. The
existing crust state-property code also inspected `this.group` during `super()`,
when it was still zero. Thus even the previously implemented mask was absent from
the actual crust/decorated state definitions.

## Unit review

All oracle paths below are relative to
`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`.

### BlockCandleRenderer — partial → parity_verified (code; frame pending)

Oracle `thaumcraft/client/renderers/block/BlockCandleRenderer.java:43-66` draws
body, 1..5 drips, then white stub. The seed is the Java **int** expression
`x+y*z`; random calls are `nextInt(5)`, then boolean, `nextInt(2)`, `nextInt(3)`.
Drips start at y=0, **not** at the top of the wax. At the origin there is one
drip: x=.4375.. .5, y=0.. .1875, z=.3125.. .375. Wick bounds are
.475.. .525 horizontally, .5.. .625 vertically (oracle :64).

`common/src/main/java/thaumcraft/client/renderers/block/PositionBlockModels.java:38-48`
now calls the existing rule with the actual position, bakes the body, then every
drip, then preserves the native white stub sprite. Bounds become sixteenths only
at the FaceBakery boundary (:53-71); body/drips use tint 0. The plain candle's
world colour is also registered as original metadata-zero 0xF0F0F0 in
`common/src/main/java/thaumcraft/client/ThaumcraftColoredCandlesClient.java:13-14`.
Inventory models remain static and have no position-dependent drips, as in the oracle
inventory method (:28-40).

Actual terrain entry points:

* `fabric/src/main/java/thaumcraft/fabric/client/FabricPositionBlockModels.java:23-45`
  wraps **blockstate** models for plain and `*_tallow_candle` IDs, not item models;
  FRAPI emits the shared geometry with AO disabled and original diffuse shading.
* `common/src/main/java/thaumcraft/common/mixin/client/PositionBlockModelMixin.java:20-30`
  intercepts the exact vanilla tessellator descriptor.
* `neoforge/src/main/java/thaumcraft/neoforge/mixin/client/NeoForgePositionBlockModelMixin.java:22-32`
  separately intercepts NeoForge's ModelData/RenderType overload. Its existence was
  checked with `javap` on the actual patched 21.1.259 cache jar.

### EldritchCrustBakedModel — partial → parity_verified (code; frame pending)

The legacy helper has no oracle counterpart; authority is oracle
`thaumcraft/client/renderers/block/BlockEldritchRenderer.java:35-66` for metadata
**4, 5 and 6**, not just 4. Every solid-material neighbour removes that side's
2/16 inset. Inventory is 2..14 (:22-32).

`common/src/main/java/thaumcraft/common/blocks/BlockEldritch.java:84,115-155`
now hands the group through constructor-time ThreadLocal state, and adds the mask
to both the crust and decorated groups. `crustMask` (:165-177) uses the existing
`BlockFluidPure.legacySolid` material compatibility rule, not opaque/full-cube
`isSolidRender`; placement and updateShape still maintain the mask (:179-195).
`PositionBlockModels.java:33-37` also samples the render region for programmatic
worldgen placement, which never calls getStateForPlacement.
`common/src/main/java/thaumcraft/client/renderers/block/EldritchCrustBakedModel.java:120-124`
now consumes that state in its actual world entry point, generating bounds through
the existing pure 64-mask rules. The same loader adapters above select these quads.
The world quads use position-scaled UVs instead of stretching a whole texture into
every inset face, and no directional diffuse tint (oracle BlockRenderer:80-98).

### BlockCustomOreRenderer — stays partial

Oracle `thaumcraft/client/renderers/block/BlockCustomOreRenderer.java:54-69`:
native stone/base cube, then coloured full-cube overlay with **packed** brightness
`Math.max(bb,160)`, then stone in .005.. .995 when **particleSetting > 1**.
This is NOT fast graphics. Inventory overlay inset .005 is oracle :47-49 and is
already present; not changed here.

`common/src/main/java/thaumcraft/client/renderers/block/OreOverlayRules.java:20-65`
contains the right numbers, but still has no production caller. Neither terrain
adapter selects ore (`PositionBlockModels.java:25-28`). Remaining work is an actual
ordered, light-controlled overlay/conditional stone renderer, not another rule
class and not an asserted absence of a 1.21.1 hook. Test guards packed-light max,
particle settings and inset, but does **not** verify the missing renderer.

### BlockGasRenderer — stays partial

Oracle `thaumcraft/client/renderers/block/BlockGasRenderer.java:38-65,87-123,135-267`:
first the supporting-face test, with a full-cube fallback at :87-95; otherwise nine
samples/four corner averages, density inversion, .001 offset, flow UVs, face-specific
brightness and .5/1/.8/.6 colours. All-negative averages are **NaN** (:50-52).
Foreign quanta -1 means -.109375, not -.875.

`common/src/main/java/thaumcraft/client/renderers/block/GasSurfaceRules.java:57-113`
preserves those numerical rules but `corners` still has no production caller.
The blockstate models are still static; neither adapter selects flux blocks.
Remaining work is sampling and emission of the complete original surface, including
the full-cube branch, flow UVs, reversed winding and neighbour light. No claim that
1.21.1 cannot implement it is made. Existing oracle quirks were not changed.

### BlockTaintRenderer — stays partial

Oracle `thaumcraft/client/renderers/block/BlockTaintRenderer.java:35-65` draws the
cube for metadata 0/1/2 in solid pass and the white, brightness-200 overlay on
**crust/soil** metadata 0/1 in translucent pass, with per-face side indices
E=4,W=5,S=2,N=3,U=0,D=1 (:48-64).

`fabric/src/main/java/thaumcraft/fabric/client/FabricTaintFibresModel.java:43-49,54-67`
only wraps `taint_fibres` and emits the glow on fibre type 0. There is still no
NeoForge fibre renderer, and the shared adapter's selector does not pretend to
implement one. Remaining difference: crust/soil transparent overlay, metadata soil
texture selection, and loader-common fibre support/cross/overlay emission. No API
limitation justifies closing this row. This lane did not modify the old fibre code.

## Tests and compile proof

Authored **six** JUnit 5 tests in
`common/src/test/java/thaumcraft/client/BlockRendererOracleRulesTest.java`:
origin drip geometry, int-overflow/colliding position seeds, negative/NaN gas
quirks, all four gas corners and light constants, packed ore brightness/particles/
inset, all 64 crust masks. No Block is constructed. **Authored, compile-checked,
not executed.** No test substitutes for the missing ore/gas/taint renderers.

Whole `common/src/main/java` type-check: **1,413 sources, javac exit 0**, Java 21,
`-proc:none --release 21 -sourcepath '' -J-Xmx1200m`, output in
`.tmp/w7blocks-javac`; command arguments/log in `.tmp/w7blocks-javac.args` and
`.tmp/w7blocks-javac.log`. The classpath uses the required remapped
`minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2.jar`,
`common/build/classes/java/main` (absent initially in this fresh worktree), cached
modules-2 dependencies, and named Architectury API from its NeoForge jar.
The raw vanilla jar needs the project's three access wideners plus Fabric's
BlockEntitySupplier class accessibility; these were applied with Fabric's actual
AccessWidener ASM visitor into a **temporary copy**, never to the shared cache.
No stubs and no Gradle daemon. Mixing intermediary Architectury or sources jars
into this classpath initially produced unrelated baseline errors; that invalid
attempt is not claimed as proof.

The Fabric adapter was also separately type-checked against the **actual remapped
FRAPI** (TinyRemapper intermediary→named using the cache mappings), exposing and
fixing both the fromVanilla signature and MaterialFinder AO API before acceptance.
The NeoForge adapter and the new test class also type-check (final combined adapter
and test javac exit 0). JUnit and
the game were not started.

Ledger validation run after the five-row edit:
`python3 scripts/porting/update_scope_ledger.py --check --original-root /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`
→ **stale_review_fingerprints: 0**. `counts` was not changed.

## Real-frame acceptance (leader)

1. Fabric and NeoForge: coloured and plain candles at (0,0,0), (13,0,0), (7,2,3),
   plus a grid of distinct seeds. At origin: one 1/16-wide drip, 3/16 high, on the
   north wax side, x=7/16..8/16. It must not dangle down from y=.5. Identical seeds
   stay identical across chunk rebuild, reload and camera movement. All sixteen
   colours tint body/drips only; wick remains white. Item has no drips.
2. Isolated crust/glyphed/decorated blocks: 2..14 cube; touching pairs and 2×2
   fields extend to the shared border with **no 2/16 groove**, horizontally and
   vertically. Break/place neighbours and compare after rebuild/reload. Include
   solid transparent, non-solid liquid, ordinary stone, and programmatic placement.
   Check stretched/mirrored UVs, diffuse darkness, seam z-fighting and missing wick.
3. Ore (still open): dark cave vs sky, particles All/Decreased/Minimal independently
   of graphics mode; overlay light floor only per packed max, stone pass only Minimal;
   compare item inset .005 without a new item regression.
4. Gas (still open): supported/unattached, quanta 1/4/8, sloped 3×3 neighbourhood,
   both gas and goo; check original full-cube fallback, reversed surface, colours,
   winding and flow UVs. A plain translucent cube is a failure.
5. Taint (still open): crust/soil/flesh adjacent to non-taint supports, glow setting
   on/off, fibre types 0..4; inspect translucent crust/soil overlays on both loaders.
   Glow on fibres alone is a failure.

Remaining blockers are three **unimplemented** renderer paths, plus deferred
runtime/mixin/frame acceptance of this lane's two implemented paths; they are not
excused as impossible API migrations.
