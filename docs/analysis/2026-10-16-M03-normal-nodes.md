# M03 — natural NORMAL aura node minimum unit

## Integration contract / verification

Root acceptance после source freeze: `early-content-validate3`192JUnit/47+47GameTests PASS;
actual Fabric `early-content-client2` held-use drain и arcane debit PASS. ExactPNG24
показывает normal node в supplied scene. Natural NORMAL distribution/full visuals не
закрываются этой fixture; [сводка](2026-10-16-early-content.md).

- Common root calls `ThaumcraftNodes.register()`; client root calls **client-only** `ThaumcraftNodeClient.init()`.
- Fabric root injects `ThaumcraftNodes.NODE_KEY` once at `GenerationStep.Decoration.TOP_LAYER_MODIFICATION`, selected by `ThaumcraftNodes.NODE_BIOMES` (bundled tag references `#minecraft:is_overworld`). NeoForge has its own node-only `normal_aura_nodes` biome modifier. Do not add a second rarity/count modifier: the feature already makes one 1/36 attempt per chunk.
- Feature/configured/placed ID: `thaumcraft:normal_aura_nodes`. Existing `ThaumcraftContent.AURA_NODE` and `NODE_BLOCK_ENTITY` are reused, never re-registered.
- `NaturalNodeRuntimeChecks.run(GameTestHelper, ServerPlayer)` is ready for root-owned loader test registration. It exercises real generation initialization with a fixed normal seed, final-partial-unit wand drain, distinct current/base persistence, scan/update identity, 600-tick recharge, compound retention, zero pruning and node death. No test-only production RNG/cost substitutions.
- **Implemented, not compiled, not executed, not visually inspected by this subtask.** User prohibited Gradle/game/test execution here; root performs centralized verification after source freeze. No bootstrap, held-wand trigger, ore agent source, shared language or central status files were edited here.
- Read-only checks: all 35 new node JSON files parsed; all required vanilla/local biome references exist in the cached actual 1.21.1 client JAR; the 31 type tags match the adapter. Selected native signatures and the generation-region block-entity creation path were inspected from the cached mapped JAR. These checks are **not** a compile/runtime/client pass.

## Original private-oracle anchors

All paths below are relative to `../work/thaumcraft-4.2.3.5-oracle/thaumcraft/`.

| Source | Behaviour retained |
|---|---|
| `common/config/Config.java:782-812,872-873` | Per-dictionary-type aura/aspect table; default node rarity 36, special rarity 18. |
| `common/lib/world/biomes/BiomeHandler.java:26-50` | Integer-average aura; uniformly chosen TYPE tag; unknown fallback 100/null. |
| `common/lib/world/ThaumcraftWorldGenerator.java:189-215` | One wild attempt per chunk; chunk-local x/z; first uncovered Y then 0..3 offset. |
| `common/lib/utils/Utils.java:116-122` | First-uncovered scan starts at Y=5, not the highest surface. This can put nodes in caves. |
| `common/lib/world/ThaumcraftWorldGenerator.java:229-274` | NORMAL probability 17/18; special branch DARK/UNSTABLE/PURE/HUNGRY = 3/10,3/10,3/10,1/10. Independent modifier chance 1/9, then uniform BRIGHT/PALE/FADING. |
| `common/lib/world/ThaumcraftWorldGenerator.java:275-308,334-399` | Value `nextInt(aura/2)+aura/2` (integer division), biome tag or compound+primal seed, three optional aspect draws, 11-cube environment thresholds, alphabetically sorted 50..74/25..74 spreads, float/truncation/merge quirks, current/base initialization. |
| `common/tiles/TileNode.java:185-210,274-329,547-695` | Separate current/base; zero-preserving reduction; nearby-node bullying; 600/400/900/0 regeneration; one random depleted CURRENT tag per interval; bounded wall-clock catch-up per interval*75ms; 1200-tick zero/base pruning, weakening and death. A pruned tag is not recreated from its historical base entry. |
| `common/tiles/TileNode.java:398-425`; `common/items/wands/ItemWandCasting.java:208-226,340-349` | Primal-only wand filter; `NODETAPPER1/2`; original centivis final-partial-unit rounding; wood/iron never receives `NODEPRESERVE`. |
| `common/blocks/BlockAiry.java:151-163,217-230`; `client/renderers/tile/TileNodeRenderer.java:48-199`; `client/lib/UtilsFX.java:195-233` | Light 8; non-colliding 0.3..0.7 target; original node atlas frame/strip UVs, aspect colours, pulse/brightness and faint unaided core. |

## Explicit 1.21 adaptations

- Modern vanilla terrain/biomes/tags are not seed/chunk-identical to 1.7. Original Y=5 start is retained and the upward scan is bounded by native build limits, including terrain above 256. The old impossible-in-this-Overworld-path `<2` fallback is not synthesized into a new distribution.
- `node_biome_types/*` is a separate, explicit mapping from vanilla modern biome families and optional shared `c:` tags to the original dictionary type table. It does not copy ore distribution or depth weights. These modern memberships are an adaptation, not a claim of exact old Forge dictionary membership/order. Surface-climate biome sampling avoids new 3D cave-biome bias.
- Fluid tags replace removed fluid materials (including modern waterlogged fluid); native leaves replace `isFoliage`; only vanilla stone counts toward the original `stone>500` threshold, not deepslate. Environmental reads and random neighbour-node checks skip unavailable chunks and never force-load them.
- UUID scan IDs retain the existing native identity scheme and persist/update unchanged; no legacy-world migration is attempted. Old native checkpoint saves without `AspectsBase` receive a base equal to their saved current state, never a free 25-each refill.
- Native billboard buffers replace fixed-function GL. Wand and offhand reveal are deliberate minimum-usability additions; original unaided faint core remains. Original atlas is reused with additive/ordinary-alpha aspect layers; visibility and animation still require actual client inspection.

## Remaining gaps — do not claim full node catalogue/ecology parity

- Natural special-type rolls are **skipped without replacement or reroll**. Each type's first skip logs a pending warning; `ThaumcraftNormalNodeFeature.skippedSpecialNodes(NodeType)` exposes aggregate per-type skips. Expected ordinary type density is therefore `(1/36)*(17/18)` before terrain eligibility, not one normal per 36 chunks. All three modifiers on NORMAL nodes are implemented, including legitimately non-regenerating FADING nodes.
- DARK, UNSTABLE, PURE, HUNGRY and TAINTED behaviours/generation are pending. The reserved type values persist and render distinct atlas rows, but loaded non-NORMAL nodes do not silently run normal ecology. Taint biome registrations/conversion, hungry suction/growth, unstable orbs, dark mobs, pure conversion and special biome aura multipliers are absent, not fake-biome placeholders.
- Stabilizers/locks, jarred/energized nodes, silverwood/structure/mound/totem nodes, spawn-node guarantee, dimension/biome blacklist config and legacy configuration UI/overrides are not part of M03. `auraGen` interactions with those unported generators are not claimed. Node-interaction zap FX are pending even though the normal-to-normal resource transfer is implemented.
- Goggles/revealer API, thaumometer FOV/depth-ignore, drain beam, full animation/blending parity, node item inventory appearance, native high-tier rod/cap/preservation and survival distribution/client discovery evidence remain pending. Existing creative node placement remains only a fixture, never the source of wild capacities.
- Manual player destruction/wisp-essence loot remain pending: the inherited content registration is still unbreakable (original `BlockAiry.java:115-140,265-279` instead used hardness 2, blast resistance 200 and aspect-bearing wisp essence). This unit implements drain/pruning death, not that unported resource/drop chain; no wrong item substitute is introduced. Empty-hand debug readout is retained only on old creative fixtures, never wild nodes.
