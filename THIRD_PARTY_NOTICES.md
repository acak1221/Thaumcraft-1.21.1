# Sources and attribution

Thaumcraft 4.2.3.5 was created by Azanor. This is an unofficial local port and must not be presented as an official version.

## Pinned sources

- Behavioral oracle: `Thaumcraft-1.7.10-4.2.3.5.jar` retained in the baseline repository. Hashes and the baseline commit are recorded in the migration source lock.
- Audited Forge 1.12.2 baseline: <https://github.com/0FL01/Thaumcraft-4.2-FOREVA>, commit `4bc68454b1063cdc1255f833dd537e0fd4fc341d`.
- Original-source reference: <https://github.com/IgnoreLicensesCN/OpenTC4>, commit `80aac29c52543ef51766c4b0788fba782abb3b0b`.
- Platform-pattern reference: <https://github.com/IgnoreLicensesCN/OpenTC4-Architectury>, commit `1509b3192910aaca0af8bba1b55d189a675010bf`, target Minecraft 1.20.1. Its root `LICENSE.txt` claims MIT, while its README explicitly says the author did not obtain permission from Azanor. Its build/license statements are not treated as permission for the original Thaumcraft code or assets.
- Selective modern-API reference: <https://github.com/EugeneHeisst/thaumcraft-port>, commit `a9b7a613c9c3e5a4418f30e0cf38553ac328b1e6`. No gameplay code/assets from this incomplete reference were copied; its MDK-template notice is not treated as a licence for original Thaumcraft material.
- Read-only TC4 Reforged implementation reference: <https://github.com/notlikethiss/thaumcraft-4-reforged>, branch `mc/1.21.1` at `d1e64cc527a8fa0c99ae738cc9c0bd0370d4bd7e`; separately inspected newer `main` at `15c4ac0e9198049228a1d102137fd07fce44c5eb`. Original: Azanor; port credits: overseas/notlikethiss. Observed `All Rights Reserved` and README personal-use restriction; permission to copy or distribute their code/assets has not been established. No donor code/assets copied.
- Read-only TC6 modern-API reference: <https://github.com/DJDMM/Thaumcraft-6-1.20.1-port>, commit `3ee6ed6ec5a55520cc9f599a944e46f89ac3ba74`. Original: Azanor; adaptation: DJDMM, with upstream TheDarkTower314 credited by that repository. Observed `All rights reserved`; the translation-only MIT notice does not license gameplay code/assets. No donor code/assets copied; TC6 mechanics do not replace the TC4 specification.

## Forge 1.7.10 behavior adapter

The native finite-flux adapter follows the finite-fluid algorithms and curative-instance behavior of Minecraft Forge, by the Minecraft Forge contributors, pinned at [`9274e4fe435cb415099a8216c1b42235f185443e`](https://github.com/MinecraftForge/MinecraftForge/tree/9274e4fe435cb415099a8216c1b42235f185443e). Relevant sources are `src/main/java/net/minecraftforge/fluids/BlockFluidFinite.java`, `BlockFluidBase.java` and the original `PotionEffect` patch. This provenance applies to `FluxFiniteBlock` and the scoped effect-curative adaptation; it does not grant rights to Thaumcraft or Minecraft assets/classes.

The bounded native magical-plant soil adapter also follows the same pinned Forge revision's `patches/minecraft/net/minecraft/block/Block.java.patch` (`canSustainPlant`: inherited bush soils before the Plains/Desert type branches). Original Forge/Minecraft classes are not copied or bundled; modern block IDs replace old dirt/sand metadata explicitly.

That pinned Forge source supplies the [Minecraft Forge Public Licence version 1.0](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/MinecraftForge-License.txt), not the current Forge project's licence. Upstream source remains available at the pinned links; original Forge/Minecraft classes and MCP data files are not bundled in the mod. No external distribution is authorized by this local port task.

The retained baseline root MIT notice applies according to its provenance; it does not establish ownership of Azanor's original material. Original assets are locally reused from this checkout, with per-file hashes in `docs/porting/resource-manifest.json`. The new migration does not publish a repository, push source, or release a JAR externally. Publication rights have not been established by this task.

The native Taint Poison status icon `common/src/main/resources/assets/thaumcraft/textures/mob_effect/flux_taint.png` (SHA256 `ac11cb4f90ca145fb316a861efffce3296ecf94daea4459f0d6feac0d3c64121`) is a crop of Azanor's retained `textures/misc/potions.png`, rectangle `(54,216)`–`(72,234)`, corresponding to the original icon index `(3,1)`. It is not newly authored art or donor-port artwork. This standalone-icon adaptation is documented in `docs/analysis/2026-10-16-infusion-first-unit.md`; it changes neither attribution nor publication rights.

The native Thaumium armor layer paths `textures/models/armor/thaumium_layer_1.png` and `_2.png` are byte-for-byte aliases of Azanor's retained `textures/models/thaumium_1.png` and `_2.png`. Their hashes and path adaptation are documented in `docs/analysis/2026-10-16-thaumium-next-unit.md`; no donor-port artwork was copied and no new publication rights are inferred.

The native Void armor layers `textures/models/armor/void_layer_1.png` (SHA-256 `e2f42ab4a94effcaf2c95dec3e0c453f03973fc2542e6947fae481bf74b8682d`) and `void_layer_2.png` (`1210c1368c0ff9bb77c937d359d2baa6707714994d567aaec9d32d94e59c5d10`) are byte-for-byte aliases of Azanor's retained `textures/models/void_1.png` and `_2.png` (64×32). The originals are unchanged; this path adaptation does not grant new artwork or publication rights.

The native Goggles of Revealing layer `textures/models/armor/goggles_layer_1.png` is a byte-for-byte path alias of the retained `textures/models/goggles.png` (SHA-256 `cb117adf2d42d57767ce68110cf5d7ef3ae3458b67b376cd8c5e3f5e129cd73f`,128×64). The original is unchanged; this path adaptation does not grant new artwork or publication rights.

The four native robe layers `textures/models/armor/robes_layer_{1,2}[_overlay].png` are byte-for-byte path aliases of the retained `textures/models/robes_{1,2}[_overlay].png`, all64×32. Their hashes are recorded in `docs/analysis/2026-10-16-robes-and-early-foci.md`. No donor-port art was copied; the base and untinted overlay remain Azanor's original artwork, with no new publication rights implied.

Private decompiled oracle output remains outside this repository and is not included in source/build outputs. Original/donor JARs and Minecraft classes are not included in the modern mod artifacts.

## Vendored CodeChickenLib reference tree

The retained 1.12 reference tree `src/main/java/thaumcraft/codechicken/lib/**` (40 files) is a vendored copy of ChickenBones' CodeChickenLib classes, repackaged into the `thaumcraft.codechicken` namespace by the audited Forge 1.12.2 baseline named above. Upstream CodeChickenLib (<https://github.com/Chicken-Bones/CodeChickenLib>) is published by ChickenBones under the LGPL (the upstream repository declares LGPL-2.1) and CodeChickenCore (<https://github.com/Chicken-Bones/CodeChickenCore>) under the MIT licence. The 1.7.10 mod additionally bundles CodeChickenCore's `core/launch/DepLoader.java` as its FML 1.6 coremod (`META-INF/MANIFEST.MF`: `FMLCorePlugin: thaumcraft.codechicken.core.launch.DepLoader`); that class is not retained in this repository, because the 1.12 baseline already dropped the coremod.

The reference tree is not part of the 1.21.1 build: `settings.gradle` includes only `common`, `fabric` and `neoforge`, so no root `src/` Java file is compiled, and no CodeChicken class is shipped in any artifact. The modern production source has no CodeChicken implementation; `common/src/test/java/thaumcraft/essentia/CodeChickenGeometryParityTest.java` also cites the original geometry as test evidence. The tube ray trace, the cuboids and the shock-orb floor were re-implemented with the native 1.21.1 `AABB`, `Vec3`, `BlockPos` and `Mth` instead. Keeping the reference tree changes no ownership statement and grants no new distribution right.

## Tools and runtime dependencies

- Architectury API: LGPL-3.0; referenced as an external runtime dependency, not shaded into the mod.
- MixinExtras0.4.1 by LlamaLad7: MIT, <https://github.com/LlamaLad7/MixinExtras>. Its common annotations/processor are compile-only inputs; Fabric Loader supplies the external runtime. No MixinExtras implementation is shaded into the mod.
- Fabric Loader and Fabric API, NeoForge and their dependencies: external loader/runtime dependencies with their own notices.
- EMI by Emi (MIT, <https://github.com/emilyploszaj/emi>) and Mod Menu by TerraformersMC (MIT, <https://github.com/TerraformersMC/ModMenu>): optional integrations. Only their public API jars are compile-only inputs; neither is shaded into the mod or required at runtime.
- Gradle wrapper, Architectury Loom and Shadow: build tooling only. Wrapper/build patterns adapted from the Architectury reference; its MIT notice is retained below.
- Eclipse Temurin Java 21 and CFR: local inspection/build tools, not bundled.

## Architectury reference template notice

The MIT License (MIT)

Copyright (c) 2025

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

### Research notification frame

`common/src/main/resources/assets/thaumcraft/textures/gui/research_toast.png`
is the exact160×32 pixel crop at(96,202) used by TC4 GuiResearchPopup from Mojang's
Minecraft1.7.10 `assets/minecraft/textures/gui/achievement/achievement_background.png`.
Minecraft and that asset are owned by Mojang/Microsoft; the repository's source
license does not relicense it. Source client JAR SHA256:
`a4fc2284657544e0f4bcc964f927c2fda3e3a205178ed1d5d58883aaf9780cce`;
source PNG SHA256:`b99ec87194c344874949bcf3dcc40745128800c82def4a75c48b6d38e1694575`.
Only the required small crop is retained, with deterministic extraction in
`scripts/porting/extract_research_toast.py`; no original JAR or full atlas is included.

### Blurred-vision post program

`assets/minecraft/shaders/program/thaumcraft_blur.{json,fsh}` and
`thaumcraft_blur_vertex.vsh` under `common/src/main/resources` adapt Mojang's
Minecraft 1.7.10 `blur.json`, `blur.fsh` and `sobel.vsh` to GLSL 150 syntax and
collision-free program names. The squared asymmetric sample weights are retained.
Source official client JAR SHA1: `e80d9b3bf5085002218d4be59e668bac718abbc6`.
Minecraft and these assets are owned by Mojang/Microsoft; this repository's
source license does not relicense them. No external distribution is authorized.

### Bone-bow glint texture

`assets/thaumcraft/textures/misc/bone_bow_glint.png` under `common/src/main/resources`
is the byte-exact Minecraft 1.7.10 `assets/minecraft/textures/misc/enchanted_item_glint.png`
used by the original TC4 equipped bone-bow renderer. Official client JAR SHA1:
`e80d9b3bf5085002218d4be59e668bac718abbc6`; PNG SHA256:
`134be4ee82e2c9e505064a65abc01af098a8173f3f4234802ea186c61120c7bf`.
Minecraft and this texture are owned by Mojang/Microsoft; the source licence does
not relicense this asset. No original JAR is included and no external distribution
is authorized.

### Original ore inset stone

`common/src/main/resources/assets/thaumcraft/textures/blocks/legacy_stone.png` is
the byte-exact `assets/minecraft/textures/blocks/stone.png` from Mojang's official
Minecraft 1.7.10 client JAR, SHA1 `e80d9b3bf5085002218d4be59e668bac718abbc6`.
PNG SHA256: `489928bc2549424b7f8d979b7983e4398789dbff80b8c61f68368a02235d6927`.
It is used only for the original ore renderer's anisotropic-filtering inset pass;
Minecraft 1.21.1's retextured stone is not the original image. Mojang/Microsoft own
this asset; this repository's source licence does not relicense it. The original
JAR is not bundled and no external distribution is authorized.

## Extraction inventory checked on 2026-10-23

The official Mojang Minecraft 1.7.10 `client.jar` is an external inspection input,
SHA1 `e80d9b3bf5085002218d4be59e668bac718abbc6`, SHA256
`a4fc2284657544e0f4bcc964f927c2fda3e3a205178ed1d5d58883aaf9780cce`.
It is not retained in this repository. The TC4 4.2.3.5 JAR and its private CFR
0.152 oracle are pinned by `docs/porting/source-lock.json`; the private oracle
is not bundled. The original TC4 and TC6 reference JARs **are** tracked at the
repository root, distinct from the modern mod artifacts.

MCP names used to interpret the 1.7.10 bytecode come from
`de.oceanlabs.mcp:mcp_stable:12-1.7.10` (the external ZIP at
`https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_stable/12-1.7.10/mcp_stable-12-1.7.10.zip`;
see `scripts/porting/resolve_srg.py:5-17`). The historical FML mapping
(`MinecraftForge/FML`, branch `1.7.10`, `conf/fields.csv`) is also cited for the
ore anisotropic-filtering field. Mapping names/data are
inspection references, not newly authored Minecraft code or a grant of asset rights.
The Forge algorithm reference and its exact revision/licence are named above.

Paths in the following table are relative to
`common/src/main/resources/assets/thaumcraft/`. SHA256 values identify the
**retained output**, not an absent source atlas.

| Retained file | Source/adaptation | SHA256 |
|---|---|---|
| `textures/misc/particles.png` | Azanor, byte-exact TC4 particle sheet | `1fb548c3bc2bb99e7a6db1295214e6f119d2a3eddc36214b3c0abcb7dcc22d18` |
| `textures/misc/particles2.png` | Azanor, byte-exact TC4 second particle sheet | `1d12b71374d3731e5bcf0e391e0fa38db4facca32ae2e4a2d798196e543c806a` |
| `textures/models/scanner.png` | Azanor, byte-exact TC4 scanner atlas | `e9a8e15d7b1cf347d6239f9d236bfd15b5e41d642000dde3278792055e6a917e` |
| `textures/models/scanner.obj` | Azanor, TC4 model with canonical LF newlines | `65e59c067fcd3fbda4211d6b2c036811d7cbf9ca4cf3cd7c49f41cd0c96aec88` |
| `textures/entity/golem_bobber.png` | Mojang 1.7.10 particle sheet, cell (1,2), crop (8,16,8,8) | `b74493b953b4e1a0aef69ac208ebc3c3fdb941e3e2b8c01170e5761da3a5dfd9` |
| `textures/gui/research_toast.png` | Mojang achievement atlas crop described above | `42b1722ce07f63d2b1a20f2d10e84aa550a93f8ee218774b47c7ba13dc687c79` |

The source Mojang particle sheet `assets/minecraft/textures/particle/particles.png`
has SHA256 `a6bbffae17fdf43f76e793df6451aeeed1a6ea68549fba6d1f5d9613fb762769`;
only its bobber crop is retained. The bone-bow glint and ore stone are named and
hashed above. The adapted blur outputs under `common/src/main/resources/` are:

| File | Output SHA256 |
|---|---|
| `assets/minecraft/shaders/program/thaumcraft_blur.json` | `959241f3ef3f697536702a2d476e79b9ba9ed6ab7bf204f853f8703766bd17df` |
| `assets/minecraft/shaders/program/thaumcraft_blur.fsh` | `8de04ae4377861109e3a4bffdde2e0e1346099ef014ea64f777b35dcb85d1a6b` |
| `assets/minecraft/shaders/program/thaumcraft_blur_vertex.vsh` | `aecd4d6b92bb3cf3bc99c9143a25c2c792b117ff5f46966dbee0d7b351897750` |

The 111 retained `sounds/**/*.ogg` files are Azanor's TC4 sounds, including
lowercase path aliases `runicshieldcharge.ogg` and `runicshieldeffect.ogg` of
`runicShieldCharge.ogg` and `runicShieldEffect.ogg`. All 111 output paths and
SHA256 values are in `docs/porting/resource-manifest.json` (`copied_files`).
That manifest also identifies the other retained TC4 assets and their hashes.

**Font limitation:** this checkout contains no extracted 1.7.10 font atlas or
font file. The native book uses the modern vanilla font; earlier claims that
the old glyph artwork was restored are not asset evidence. The original
`TCFontRenderer`/unused galactic-atlas question is discussed in
`docs/analysis/2026-10-23-agent-w2clientfx.md:511-517`. No nonexistent font is
claimed as a retained extraction.

Minecraft assets remain Mojang/Microsoft's, and TC4 assets remain Azanor's.
Attribution, extraction and local reuse do not relicense them or establish
publication rights. No external distribution is authorized by this audit.
