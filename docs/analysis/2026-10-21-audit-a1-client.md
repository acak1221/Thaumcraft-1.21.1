# Аудит соответствия порта TC4 4.2.3.5 → Minecraft 1.21.1 — клиентский слой

Дата: 2026-10-21. Домен: `client_rendering_fx_input_and_gui` + `embedded_support_and_dependencies`.
Источник истины: `/home/Acak/work/thaumcraft-4.2.3.5-oracle` (декомпиляция `Thaumcraft-1.7.10-4.2.3.5.jar`, 859 классов).
Нативный порт: `common/src/main/java/thaumcraft/` + `common/src/main/resources/assets/thaumcraft/`.
Ревью: `docs/porting/audit-reviews-a1-client.json` (170 строк, все `callsite_audit: complete`).

Метод: для каждой строки worklist прочитан оригинальный класс целиком, найден нативный аналог
(по смыслу, а не по имени), сопоставлены числа/поведение, а не только сигнатуры. Для
`codechicken`/`truetyper` выполнен полный аудит вызовов по графу ссылок всего дерева оракла
(859 классов), чтобы отличить «мёртвый код» от «живой, но не перенесённой функциональности».

---

## Итог

### По verdict (`disposition`)

| вердикт | client_rendering_fx_input_and_gui | embedded_support_and_dependencies | всего |
|---|---|---|---|
| `port` | 49 | 0 | 49 |
| `adapt` | 22 | 0 | 22 |
| `replace` | 18 | 7 | 25 |
| `exclude` | 36 | 38 | 74 |
| `needs_review` | 0 | 0 | 0 |
| **строк** | **125** | **45** | **170** |

### По покрытию нативного кода (`native_coverage`)

| покрытие | client_rendering_fx_input_and_gui | embedded_support_and_dependencies | всего |
|---|---|---|---|
| `parity_verified` | 53 | 7 | 60 |
| `partial` | 38 | 0 | 38 |
| `missing` | 34 | 38 | 72 |
| `not_assessed` | 0 | 0 | 0 |

### По семантике источника (`source_semantics`)

| семантика | количество |
|---|---|
| `equivalent` | 49 |
| `adapted` | 47 |
| `not_applicable` | 64 (мёртвый код оригинала + 29 legacy-only строк без оригинала) |
| `different` | 10 (живая оригинальная функция, в порту отсутствует/заменена иначе) |

Ключевой вывод по встроенным библиотекам: **живой** функциональностью в TC4 4.2.3.5 были
только 7 классов `codechicken` (рейт-трассер труб + `MathHelper.floor_double` для
`EntityShockOrb`) и **ни одного** класса `truetyper` (загруженный TrueType-шрифт ни разу
не выводился). Остальные 34 класса `codechicken` и все 4 класса `truetyper` — мёртвый код
оригинала, и их отсутствие в порте корректно.

---

## Что совпадает (подтверждено по числам)

Это не «похоже», а проверено численно:

* **Геометрия JSON-моделей = оригинальные W1..W15.** `loot_urn_0.json` (`[3,0,3]-[13,1,13]`,
  `[2,1,2]-[14,13,14]`, `[4,13,4]-[12,16,12]`) точно повторяет
  `oracle thaumcraft/client/renderers/block/BlockLootUrnRenderer.java:22-34`
  (`W3..W13 × 0..W1`, `W2..W14 × W1..W13`, `W4..W12 × W13..1`);
  `loot_crate_0.json` (`[1,0,1]-[15,14,15]`) = `oracle thaumcraft/client/renderers/block/BlockLootCrateRenderer.java:22-27`;
  `tallow_candle.json` (`[6,0,6]-[10,8,10]`, `[7.6,8,7.6]-[8.4,10,8.4]`) =
  `oracle thaumcraft/client/renderers/block/BlockCandleRenderer.java:20-31`.
* **Частицы.** `WardHitParticle` (hemis0..15, 12–16 тиков, профиль alpha),
  `TaintDropParticle` (gravity .06, bob 40, `64/(rand*.8+.2)`, jar-коллизия),
  `SlurryBubbleParticle` (144..150, 15–19 тиков, подъём .005),
  `SanityBubbleParticle` (240 block light, .002 подъём, три pop-кадра),
  `TileCrucibleRenderer` (жизнь `(age+2)+8/(rand*.8+.2)`, froth 4+rand(3) / 12+rand(12),
  затухание .85), `MonsterSpriteParticle` (32-кадровая node-полоса, 31 тик),
  `TaintSwarmParticle` (8 клеток, преследование, fade при смерти цели),
  `FXVent` (масштаб `rand*.1+.05`, `.0025` подъём, `.85` затухание, quirk `TileTube:129`),
  `TileManaPodRenderer` (базовый цвет `.14509805/.6156863/.45882353`, смешивание с аспектом,
  свет `age*10+150*scale`) — совпадают с оригиналом.
* **HUD уведомлений.** `PlayerNotifications.renderHud` воспроизводит
  `oracle thaumcraft/client/lib/REHNotifyHandler.java:43-86` координат в ряд
  (`screenWidth - size - 10`, `screenHeight - index*8 + shift`), масштаб 0.5, упаковку цвета
  `(alpha/2 << 24) | 0xFFFFFF`, клетку частицы `16*((tickCount + index*3) % 16)` и
  индикатор `thaumonomicon`.
* **GUI.** `GuiDeconstructionTable` (176×166, полоса отсчёта `(93, 15+46-i1)` с crop
  `(176,46-i1,9,i1)`, иконка аспекта `(64,48)`), `GuiTravelingTrunk` (полоса HP `(134,2)`
  длиной `39*hp/max`, Earth-полоса `(0,80)`, метка stay `(112,0)`, подпись в 0.5 масштаба
  цветом 12624112), `GuiHoverHarness` (маркер хотбара `(8+slot*18, 142)`, crop `(240,0,16,16)`).
* **Модели.** `ModelCube`, `ModelTable`, `ModelBanner`, `ModelRobe`, `ModelTrunk`,
  `ModelFireBat`, `ModelManaPod`, `ModelCrystal` (свёрнута в `TileCrystalRenderer`),
  `ModelCrystal` — все с исходными пивотками/UV и с документированной quirks «mirror=true
  присваивался ПОСЛЕ `addBox`, поэтому кубы не зеркалятся».
* **Трубы.** `common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:12-19`
  воспроизводит ядро `0.34375..0.65625` и шесть «рукавов» оригинальных `IndexedCuboid6`
  (`oracle thaumcraft/common/tiles/TileTube.java:372-393`), а также порядок индексов 0..5 +
  core 6 и правило ближайшего расстояния.

---

## Построчный разбор всех не-`parity_verified` строк

Колонка «первое evidence» — конкретная пара `oracle …:строка` → `native …:строка`,
по которой вынесен вердикт; полный список evidence и notes — в
`docs/porting/audit-reviews-a1-client.json`.

| `thaumcraft/client/ClientModelRegistry.java` | exclude / not_applicable / missing | Legacy-only 1.12 artefact with no TC4 original counterpart. | legacy src/main/java/thaumcraft/client/ClientModelRegistry.java:30-90 the 1.12 ModelRegistryEvent handler that registers the TC4 model locations and the focus sprites |
| `thaumcraft/client/ClientProxy.java` | replace / adapted / partial | The single FML proxy was replaced by ClientSetup plus ~50 purpose-specific Thaumcraft*Client classes; there is no native ClientProxy. | oracle thaumcraft/client/ClientProxy.java:371-377 registerHandlers, :385-439 getClientGuiElement (gui ids 0..20), :453-462 setupItemRenderers, :464-517 setupEntityRenderers, :519-571 setupTileRenderers, :573-616 setupBlockRenderers |
| `thaumcraft/client/ThaumcraftGuiFactory.java` | exclude / different / missing | Not ported: 1.21.1/Architectury has no FML mod-gui factory entry point, so the class has no counterpart. | oracle thaumcraft/client/ThaumcraftGuiFactory.java:19-35 the FML IModGuiFactory that exposes ThaumcraftGuiConfig |
| `thaumcraft/client/fx/ITCParticle.java` | exclude / not_applicable / missing | Legacy-only 1.12 marker interface with no TC4 original counterpart; the 1.21.1 particle classes pick their blend/atlas explicitly instead. | legacy src/main/java/thaumcraft/client/fx/ITCParticle.java:1-9 a 1.12 marker interface with getTCParticleLayer() |
| `thaumcraft/client/fx/ParticleEngine.java` | replace / adapted / partial | The original standalone manager (4 layers x dimension, 2000-particle cap, glAlphaFunc 0.003921569) is replaced by the vanilla 1.21.1 particle engine plus per-effect native Particle subclasses. | oracle thaumcraft/client/fx/ParticleEngine.java:57 four per-layer HashMap<Integer,ArrayList<EntityFX>> particle buckets keyed by dimension |
| `thaumcraft/client/fx/WRMat4.java` | replace / adapted / partial | The 4x4 matrix helper has no 1.21.1 counterpart; native Vec3/JOML maths replaced it inside the rewritten lightning shape. | oracle thaumcraft/client/fx/WRMat4.java:1-56 4x4 float matrix helper; the only caller is WRVector3.rotate |
| `thaumcraft/client/fx/WRVector3.java` | replace / adapted / partial | The original float vector maths was re-implemented natively (WispLightningShape keeps the float arithmetic; the infusion arc uses Vec3). | oracle thaumcraft/client/fx/WRVector3.java:16-131 float 3-vector with cross/dot/rotate helpers |
| `thaumcraft/client/fx/beams/FXArc.java` | exclude / different / missing | Deliberately not ported: there is no native equivalent of the 1.7 FXArc ribbon. | oracle thaumcraft/client/fx/beams/FXArc.java:24-60 builds a gravity-bent arc (gravity 0.115, noise 0.25, max 50 segments) drawn with textures/misc/beamh.png |
| `thaumcraft/client/fx/beams/FXBeam.java` | exclude / different / missing | The original entity-to-entity beam (golem boss / PacketFXBeamPulse) has no native counterpart: the native payload list has no beam-pulse message. | oracle thaumcraft/client/fx/beams/FXBeam.java:130-143 three 60-degree rotated quads using textures/misc/beam.png, beam1.png, beam2.png, beam3.png, half width 0.15*size, pulse and rotationspeed 5 |
| `thaumcraft/client/fx/beams/FXBeamPower.java` | exclude / different / partial | The visnet relay beam is ported, but with a different construction: two crossed planes at half width .105 instead of the original's two quads at half width 0.15*0.7=0.105 plus the original's separate additive flare quad on textures/misc/particles.png. | oracle thaumcraft/client/fx/beams/FXBeamPower.java:96-118 two quads of textures/misc/beam1.png, size 0.7, opacity clamped to 0.3..0.8, plus a particle-texture flare quad at the target |
| `thaumcraft/client/fx/bolt/FXLightningBolt.java` | adapt / adapted / partial | Ported as three native particles: WispBoltParticle (type 0, the wisp zap), HarnessBoltParticle (type 6, the hover harness) and InfusionArcParticle (nodeBolt). | oracle thaumcraft/client/fx/bolt/FXLightningBolt.java:1-312 the bolt effect with defaultFractal(), setType() and finalizeBolt() |
| `thaumcraft/client/fx/bolt/FXLightningBoltCommon.java` | adapt / adapted / partial | The fractal generator was re-implemented natively in WispLightningShape for the wisp/harness bolts (float arithmetic, split parents and joined segment ends are documented as retained). | oracle thaumcraft/client/fx/bolt/FXLightningBoltCommon.java:1-306 the shared fractal/branch generator used by FXLightningBolt |
| `thaumcraft/client/fx/particles/FXBlockRunes.java` | adapt / adapted / partial | The rune particle itself is faithfully ported (same UV row quirk, same random 90 degree rotation, same 0.15 half-extent, same 3*duration lifetime, same gravity drift). | oracle thaumcraft/client/fx/particles/FXBlockRunes.java:20-49 runeIndex random(224..239) but rendered at UV row .375, rotation randomInt(4)*90, size 0.3*(1+gauss*.1), lifetime 3*duration, gravity-driven rise |
| `thaumcraft/client/fx/particles/FXBreaking.java` | adapt / adapted / partial | The slime-ball icon particle is ported three times (sploosh, tentacle arise, taint land) with the original colours, sizes and 66/(r*.9+.1) lifetime. | oracle thaumcraft/client/fx/particles/FXBreaking.java:1-79 slime-ball quarter-icon particle with setParticleMaxAge(66/(rand*.9+.1)) |
| `thaumcraft/client/fx/particles/FXBubbleAlt.java` | exclude / not_applicable / missing | Dead code in the original: the class is never instantiated anywhere in the TC4 4.2.3.5 decompilation. | oracle thaumcraft/client/fx/particles/FXBubbleAlt.java:1-99 an alternate bubble (particle 25, 0.2*age/life size growth, pop cells 17/18) |
| `thaumcraft/client/fx/particles/FXBurst.java` | adapt / adapted / partial | The 32-cell node strip, the 31 tick lifetime and the EntityFX size randomisation are reproduced. | oracle thaumcraft/client/fx/particles/FXBurst.java:32-34,44-45 the size scaling, the 31 tick lifetime, the node strip texture and glColor4f alpha 0.75 |
| `thaumcraft/client/fx/particles/FXEssentiaTrail.java` | adapt / adapted / partial | The essentia trail is ported inside InfusionParticle with the original count/scale parameters and colour. | oracle thaumcraft/client/fx/particles/FXEssentiaTrail.java:1-213 the essentia trail between two points |
| `thaumcraft/client/fx/particles/FXGeneric.java` | adapt / adapted / partial | The generic animated-atlas particle is reproduced in three native variants (block sparkle / growth lamp, focus fireball, infusion burst) with the original cells, delays, sizes and half-alpha first/last frame rule. | oracle thaumcraft/client/fx/particles/FXGeneric.java:16-79 loop/start/num/inc atlas animation, setMaxAge(age,delay), half alpha on the first and last frame |
| `thaumcraft/client/fx/particles/FXScorch.java` | exclude / different / missing | Not ported: the fire focus has no native scorch trail particle. | oracle thaumcraft/client/fx/particles/FXScorch.java:1-127 the scorch trail of the fire focus |
| `thaumcraft/client/fx/particles/FXSmokeDrift.java` | exclude / not_applicable / missing | Legacy-only 1.12 artefact with no TC4 original counterpart, deliberately not ported. | legacy src/main/java/thaumcraft/client/fx/particles/FXSmokeDrift.java:1-62 a 1.12-only smoke drift particle (no TC4 4.2.3.5 counterpart) |
| `thaumcraft/client/fx/particles/FXSmokeSpiral.java` | exclude / different / missing | Not ported: neither the eldritch warden smoke spiral nor the elemental sword smoke spiral exists in the native port. | oracle thaumcraft/client/fx/particles/FXSmokeSpiral.java:1-153 the rising smoke spiral |
| `thaumcraft/client/fx/particles/FXSmokeTrail.java` | exclude / different / missing | Not ported; the original path was already unreachable because ClientProxy.soulTrail has zero callers in the TC4 tree. | oracle thaumcraft/client/fx/particles/FXSmokeTrail.java:1-193 the smoke trail between two entities |
| `thaumcraft/client/gui/GuiFactory.java` | exclude / not_applicable / missing | Legacy-only 1.12 artefact with no TC4 original counterpart; 1.21.1 has no Forge GUI factory. | legacy src/main/java/thaumcraft/client/gui/GuiFactory.java:1-28 a 1.12 ForgeGuiFactory that opens the TC config screen |
| `thaumcraft/client/gui/MappingThread.java` | replace / adapted / partial | The id->ItemStack cache is replaced by the 1.21.1 RecipeManager/RecipeHolder lookup: the native research book resolves real native recipes instead of a hand-built stack cache. | oracle thaumcraft/client/gui/MappingThread.java:12-35 a background thread that fills GuiResearchRecipe.cache with the ItemStack of every scanned item id |
| `thaumcraft/client/gui/ThaumcraftGuiConfig.java` | exclude / different / missing | Not ported: 1.21.1/Architectury has no FML mod-config GUI hook, so the in-game configuration screen does not exist. | oracle thaumcraft/client/gui/ThaumcraftGuiConfig.java:10-34 the FML config screen exposing the general, Monster_Spawning, World_Generation, World_Regeneration and Research categories |
| `thaumcraft/client/integration/jei/ThaumcraftJeiPlugin.java` | exclude / not_applicable / missing | Legacy-only 1.12 integration with no TC4 original counterpart and no JEI dependency in the 1.21.1 build. | legacy src/main/java/thaumcraft/client/integration/jei/ThaumcraftJeiPlugin.java:1-105 a 1.12 JEI plugin registering TC4 aspect/infusion recipes |
| `thaumcraft/client/lib/ClientTickEventsFML.java` | replace / adapted / partial | The original monolithic FML tick/render handler was split into purpose-specific native clients (PlayerNotifications, ThaumcraftGogglesClient, ThaumcraftRunicClient, ThaumcraftGolemClient, ThaumcraftWarpClient, ThaumcraftFocusClient) plus the native HUD/event bus. | oracle thaumcraft/client/lib/ClientTickEventsFML.java:124-166 playerTick, 242-281 clientWorldTick, 282-331 renderTick and the HUD helpers 332-735 |
| `thaumcraft/client/lib/ItemAspectTooltipHandler.java` | exclude / not_applicable / missing | Legacy-only 1.12 feature with no TC4 original counterpart, deliberately not ported. | legacy src/main/java/thaumcraft/client/lib/ItemAspectTooltipHandler.java:1-183 a 1.12-only tooltip handler that appends aspect icons below an item tooltip while Shift is held |
| `thaumcraft/client/lib/KeyHandler.java` | exclude / not_applicable / partial | Legacy-only class path; the TC4 original key handler lives in thaumcraft/common/lib/events/KeyHandler.java, which is outside this family. | legacy src/main/java/thaumcraft/client/lib/KeyHandler.java:1-157 the 1.12 copy of the TC4 key handler (focus G, hover H, misc unbound) |
| `thaumcraft/client/lib/QuadHelper.java` | replace / adapted / partial | The helper has no native class: the rotation is expressed with the native Axis.ZP.rotation(angle) after the camera-facing pose, which is the same rotation about the view axis. | oracle thaumcraft/client/lib/QuadHelper.java:13-40 the axis-rotation helper used by UtilsFX.renderFacingQuad/renderFacingStrip |
| `thaumcraft/client/lib/RenderEventHandler.java` | replace / adapted / partial | Split into purpose-specific native clients; the aspect-tag world overlay, the note text, the golem-bell markers and the warp vignette are ported. | oracle thaumcraft/client/lib/RenderEventHandler.java:138-157 renderOverlay (notifications + wand radial), 199-248 blockHighlight (aspect tags, note text, architect overlay), 252-268 renderLast (tag scale, golem markers, scanned blocks), 778-812 renderVignette |
| `thaumcraft/client/lib/TCFontRenderer.java` | exclude / different / missing | Not ported: the research book now uses the vanilla font, so the galactic (ascii_sga) font option is unavailable. | oracle thaumcraft/client/lib/TCFontRenderer.java:38-96 a copy of the vanilla FontRenderer with an extra textures/font/ascii_sga.png (galactic) atlas |
| `thaumcraft/client/lib/UtilsFX.java` | replace / adapted / partial | The helper library was replaced by VisNetRender plus per-renderer implementations of drawTag, drawFloatyLine and the aspect icons. | oracle thaumcraft/client/lib/UtilsFX.java:1-924 the static client drawing helper library (drawTag, drawTexturedQuad, renderFacingQuad/Strip, renderQuad*, drawCustomTooltip, drawFloatyLine, getGuiX/Y/ZLevel, getParticleTexture ...) |
| `thaumcraft/client/renderers/block/BlockCandleRenderer.java` | adapt / adapted / partial | The candle geometry matches the original exactly (W6=0.375 -> 6/16, W10=0.625 -> 10/16, height 0.5 -> 8/16, stub 0.475/0.5/0.525 -> 7.6/8/8.4). | oracle thaumcraft/client/renderers/block/BlockCandleRenderer.java:20-31 the inventory candle: wax cube W6..W10 x 0..0.5 plus the stub 0.475..0.525 x 0.5..W10 |
| `thaumcraft/client/renderers/block/BlockCosmeticOpaqueRenderer.java` | adapt / adapted / partial | The cosmetic opaque family is now JSON models plus baked models; the totem rune selection is a unit-testable helper that reproduces the original's side+variant rule. | oracle thaumcraft/client/renderers/block/BlockCosmeticOpaqueRenderer.java:1-93 the cosmetic opaque family (rune blocks, obsidian totem, warded glass) |
| `thaumcraft/client/renderers/block/BlockCustomOreRenderer.java` | adapt / adapted / partial | The ore colour overlay is reproduced through the 1.21.1 tintindex mechanism instead of the manual GL colour. | oracle thaumcraft/client/renderers/block/BlockCustomOreRenderer.java:18-38 the inventory ore: the ore face plus an inset 0.005..0.995 coloured overlay |
| `thaumcraft/client/renderers/block/BlockGasRenderer.java` | adapt / adapted / partial | The flux gas/goo are now plain translucent blocks. | oracle thaumcraft/client/renderers/block/BlockGasRenderer.java:11-15 the LIGHT_Y_NEG/POS, LIGHT_XZ_NEG/POS and RENDER_OFFSET constants |
| `thaumcraft/client/renderers/block/BlockLifterRenderer.java` | adapt / adapted / partial | The levitator block itself is ported (BlockLevitator/TileLevitator/LevitatorRules) and its base cube model matches the original's bottom/top/side icons. | oracle thaumcraft/client/renderers/block/BlockLifterRenderer.java:18-38 the inventory lifter: the base cube plus two emissive inner slabs (0.01..0.99 x 0.9..0.99 and 0.01..0.99 x 0.1..0.9) |
| `thaumcraft/client/renderers/block/BlockMetalDeviceRenderer.java` | adapt / adapted / partial | The device family is ported as JSON models plus block entity renderers; the alembic (variant 1), magic workbench charger (variant 2) and vis relay (variant 14) still use their own renderers. | oracle thaumcraft/client/renderers/block/BlockMetalDeviceRenderer.java:18-40 the per-metadata inventory geometry: metadata 0/6 full cube, 5 a lid at 0.8125..1, 7/8/13 the box W4..W12 x W2..W14, 12 the box W3..W13, 1/2/14 delegated to the alembic / magic workbench charger / vis relay tile renderers |
| `thaumcraft/client/renderers/block/BlockRenderer.java` | replace / adapted / partial | The 1.7 RenderBlocks/IIcon helper has no 1.21.1 counterpart: faces are now JSON models and the light/brightness is the vanilla lightmap. | oracle thaumcraft/client/renderers/block/BlockRenderer.java:13-29 the W1..W15 sixteenth constants used by every TC4 block renderer |
| `thaumcraft/client/renderers/block/BlockStoneDeviceRenderer.java` | adapt / adapted / partial | The stone device family is ported as JSON models; several per-metadata variants do have their own model. | oracle thaumcraft/client/renderers/block/BlockStoneDeviceRenderer.java:1-178 the per-metadata stone device geometry |
| `thaumcraft/client/renderers/block/BlockWoodenDeviceRenderer.java` | adapt / adapted / partial | The wooden device family is ported as JSON models and the cultist banner keeps its own renderer (TileBannerRenderer). | oracle thaumcraft/client/renderers/block/BlockWoodenDeviceRenderer.java:1-161 the per-metadata wooden device geometry (planks, banner and crafting-table variants) |
| `thaumcraft/client/renderers/entity/RenderAspectOrb.java` | adapt / adapted / partial | The orb renderer is ported; the aspect icon and its spin are drawn with the native text/quad path instead of the 1.7 IIcon atlas. | oracle thaumcraft/client/renderers/entity/RenderAspectOrb.java:1-89 the aspect orb with its spinning aspect icon and the wisps |
| `thaumcraft/client/renderers/entity/RenderDart.java` | adapt / adapted / partial | Same projectile body and orientation; only the 1.7 Render class became the 1.21.1 EntityRenderer. | oracle thaumcraft/client/renderers/entity/RenderDart.java:1-95 the dart projectile renderer |
| `thaumcraft/client/renderers/entity/RenderElectricOrb.java` | adapt / adapted / partial | Same shared renderer for both original entities; only the rendering backend changed. | oracle thaumcraft/client/renderers/entity/RenderElectricOrb.java:1-78 the electric orb renderer shared by the golem orb and the shock orb |
| `thaumcraft/client/renderers/entity/RenderEmber.java` | adapt / adapted / partial | Same projectile body and orientation; only the rendering backend changed. | oracle thaumcraft/client/renderers/entity/RenderEmber.java:1-76 the ember projectile renderer |
| `thaumcraft/client/renderers/entity/RenderExplosiveOrb.java` | adapt / adapted / partial | Same projectile body plus the original FXGeneric trail, which is reproduced by FocusFireballParticle with the original atlas cells and lifetime. | oracle thaumcraft/client/renderers/entity/RenderExplosiveOrb.java:1-71 the explosive orb renderer |
| `thaumcraft/client/renderers/entity/RenderFallbackBiped.java` | exclude / not_applicable / missing | Legacy-only class; the 1.21.1 build does have a RenderFallbackBiped helper, but it is not the TC4 original and was not part of the audited original scope. | legacy src/main/java/thaumcraft/client/renderers/entity/RenderFallbackBiped.java:1-25 a 1.12 fallback renderer that renders any entity with the vanilla biped model |
| `thaumcraft/client/renderers/entity/RenderFallbackLiving.java` | exclude / not_applicable / missing | Legacy-only class; the 1.21.1 build does have a RenderFallbackLiving helper, but it is not the TC4 original and was not part of the audited original scope. | legacy src/main/java/thaumcraft/client/renderers/entity/RenderFallbackLiving.java:1-25 a 1.12 fallback renderer for living entities |
| `thaumcraft/client/renderers/entity/RenderNoop.java` | exclude / not_applicable / missing | Legacy-only class; the 1.21.1 build does have a RenderNoop helper, but it is not the TC4 original and was not part of the audited original scope. | legacy src/main/java/thaumcraft/client/renderers/entity/RenderNoop.java:1-27 a 1.12 empty renderer used for entities with no model |
| `thaumcraft/client/renderers/entity/RenderWatcher.java` | adapt / adapted / partial | The native port registers a watcher renderer that the original never registered, so 1.21.1 shows a watcher body and beam where TC4 4.2.3.5 showed none. | oracle thaumcraft/client/renderers/entity/RenderWatcher.java:1-153 the watcher body plus its target beam (watcher.png / watcher_beam.png) |
| `thaumcraft/client/renderers/item/CrystalPerspectiveModel.java` | exclude / not_applicable / missing | Legacy-only 1.12 artefact with no TC4 original counterpart; the native draws the crystal item through the block entity renderer's item path instead. | legacy src/main/java/thaumcraft/client/renderers/item/CrystalPerspectiveModel.java:1-64 a 1.12 perspective item model for the aspect crystals |
| `thaumcraft/client/renderers/item/ItemBannerRenderer.java` | exclude / different / missing | Not ported: the wooden device (cultist banner) item is a plain cube in 1.21.1, so the banner item appearance with its aspect colour is lost. | oracle thaumcraft/client/renderers/item/ItemBannerRenderer.java:16-45 renders the cultist banner for a wooden-device item that carries the 'aspect' and 'color' NBT tags |
| `thaumcraft/client/renderers/item/ItemBowBoneRenderer.java` | adapt / adapted / partial | The bone bow now uses the vanilla bow item model with the TC4 texture and the three pulling overrides. | oracle thaumcraft/client/renderers/item/ItemBowBoneRenderer.java:22-44 the held-bow transform: scale 2.6666667, rotations -20/90/-60 and the 0.625 inner scale |
| `thaumcraft/client/renderers/item/ItemChestHungryRenderer.java` | exclude / not_applicable / missing | Legacy-only 1.12 artefact with no TC4 original counterpart; the native uses the builtin/entity item model. | legacy src/main/java/thaumcraft/client/renderers/item/ItemChestHungryRenderer.java:1-27 a 1.12 item renderer for the hungry chest |
| `thaumcraft/client/renderers/item/ItemCrystalRenderer.java` | exclude / not_applicable / missing | Legacy-only 1.12 artefact with no TC4 original counterpart; the native crystal item is drawn by the block entity renderer's item path. | legacy src/main/java/thaumcraft/client/renderers/item/ItemCrystalRenderer.java:1-90 a 1.12 item renderer for the aspect crystals |
| `thaumcraft/client/renderers/item/ItemMetalDeviceRenderer.java` | exclude / not_applicable / missing | Legacy-only 1.12 artefact with no TC4 original counterpart; the native uses a JSON item model. | legacy src/main/java/thaumcraft/client/renderers/item/ItemMetalDeviceRenderer.java:1-87 a 1.12 item renderer for the metal device |
| `thaumcraft/client/renderers/item/ItemStoneDeviceRenderer.java` | exclude / not_applicable / missing | Legacy-only 1.12 artefact with no TC4 original counterpart; the native uses a JSON item model. | legacy src/main/java/thaumcraft/client/renderers/item/ItemStoneDeviceRenderer.java:1-50 a 1.12 item renderer for the stone device |
| `thaumcraft/client/renderers/item/ItemTableRenderer.java` | exclude / not_applicable / missing | Legacy-only 1.12 artefact with no TC4 original counterpart; the native uses JSON item models. | legacy src/main/java/thaumcraft/client/renderers/item/ItemTableRenderer.java:1-72 a 1.12 item renderer for the arcane workbench family |
| `thaumcraft/client/renderers/item/ItemWoodenDeviceRenderer.java` | exclude / not_applicable / missing | Legacy-only 1.12 artefact with no TC4 original counterpart; the native uses a JSON item model. | legacy src/main/java/thaumcraft/client/renderers/item/ItemWoodenDeviceRenderer.java:1-129 a 1.12 item renderer for the wooden device family |
| `thaumcraft/client/renderers/item/LifterItemColor.java` | exclude / not_applicable / missing | Legacy-only 1.12 artefact with no TC4 original counterpart. | legacy src/main/java/thaumcraft/client/renderers/item/LifterItemColor.java:1-18 a 1.12 item colour handler for the lifter |
| `thaumcraft/client/renderers/item/WoodenDevicePerspectiveModel.java` | exclude / not_applicable / missing | Legacy-only 1.12 artefact with no TC4 original counterpart; the native uses JSON item/block models. | legacy src/main/java/thaumcraft/client/renderers/item/WoodenDevicePerspectiveModel.java:1-64 a 1.12 perspective model for the wooden device |
| `thaumcraft/client/renderers/models/ModelCrystalizer.java` | replace / adapted / partial | The 1.12 crystalizer model was re-implemented as ModelEssentiaCrystalizer for the 1.21.1 model API. | legacy src/main/java/thaumcraft/client/renderers/models/ModelCrystalizer.java:1-786 the 1.12 essentia crystalizer model |
| `thaumcraft/client/renderers/models/ModelVisRelay.java` | replace / adapted / partial | The native does not port the 1.12 Java model: it loads the retained original vis_relay.obj and asserts the original vertex counts, which is a different construction. | legacy src/main/java/thaumcraft/client/renderers/models/ModelVisRelay.java:1-344 the 1.12 vis relay model |
| `thaumcraft/client/renderers/models/ModelVoidRobe.java` | exclude / not_applicable / missing | Dead code in the original: ModelVoidRobe is never instantiated; the void robe is rendered by ModelRobe in the original as well. | oracle thaumcraft/client/renderers/models/ModelVoidRobe.java:25-... a second robe model with hood, chest plate, belt and cloth parts |
| `thaumcraft/client/renderers/tile/ExtrudedSpriteRenderHelper.java` | exclude / not_applicable / missing | Legacy-only 1.12 helper with no TC4 original counterpart; the native expresses the same shapes as JSON models and ModelPart trees. | legacy src/main/java/thaumcraft/client/renderers/tile/ExtrudedSpriteRenderHelper.java:1-73 a 1.12 helper that extrudes a sprite into a block model |
| `thaumcraft/client/renderers/tile/HoleRenderBatchCache.java` | exclude / not_applicable / missing | Legacy-only 1.12 batching helper with no TC4 original counterpart. | legacy src/main/java/thaumcraft/client/renderers/tile/HoleRenderBatchCache.java:1-341 a 1.12 batching cache for the portable hole renderer |
| `thaumcraft/client/renderers/tile/LayeredFieldPlaneHelper.java` | exclude / not_applicable / missing | Legacy-only 1.12 helper with no TC4 original counterpart; the native expresses the effect as ModelTunnelField. | legacy src/main/java/thaumcraft/client/renderers/tile/LayeredFieldPlaneHelper.java:1-374 a 1.12 helper for the layered field plane effect |
| `thaumcraft/client/renderers/tile/TileEtherealBloomRenderer.java` | port / equivalent / partial | Same bloom core and the same light strip geometry (half a block, colour 0xAADDFF). | oracle thaumcraft/client/renderers/tile/TileEtherealBloomRenderer.java:1-126 the bloom with its ModelCube core and the scrolling light strip |
| `thaumcraft/client/renderers/tile/TileLifterRenderer.java` | exclude / not_applicable / missing | Legacy-only 1.12 artefact with no TC4 original counterpart. | legacy src/main/java/thaumcraft/client/renderers/tile/TileLifterRenderer.java:1-101 a 1.12 tile entity renderer for the lifter |
| `thaumcraft/client/renderers/tile/TileRenderHelper.java` | exclude / not_applicable / missing | Legacy-only 1.12 helper with no TC4 original counterpart. | legacy src/main/java/thaumcraft/client/renderers/tile/TileRenderHelper.java:1-267 a 1.12 shared tile rendering helper |
| `thaumcraft/client/renderers/tile/TileVisRelayRenderer.java` | port / equivalent / partial | Same relay geometry (loaded from the retained original OBJ with an asserted vertex count) and the same beam endpoints. | oracle thaumcraft/client/renderers/tile/TileVisRelayRenderer.java:1-98 the relay with the RingBase/RingFloat/Crystal OBJ parts and the power beam |
| `thaumcraft/codechicken/core/launch/DepLoader.java` | exclude / not_applicable / missing | Dead code in the original: the bundled CodeChicken class is never referenced by any TC4 class, so it had no effect in 4.2.3.5. | oracle thaumcraft/codechicken/core/launch/DepLoader.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/colour/Colour.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/colour/Colour.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/colour/ColourARGB.java` | exclude / not_applicable / missing | Dead code in the original: the bundled CodeChicken class is never referenced by any TC4 class, so it had no effect in 4.2.3.5. | oracle thaumcraft/codechicken/lib/colour/ColourARGB.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/colour/ColourRGBA.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/colour/ColourRGBA.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/lighting/LC.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/lighting/LC.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/lighting/LightMatrix.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/lighting/LightMatrix.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/lighting/LightModel.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/lighting/LightModel.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/lighting/PlanarLightModel.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/lighting/PlanarLightModel.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/render/CCModel.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/render/CCModel.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/render/CCModelLibrary.java` | exclude / not_applicable / missing | Dead code in the original: the bundled CodeChicken class is never referenced by any TC4 class, so it had no effect in 4.2.3.5. | oracle thaumcraft/codechicken/lib/render/CCModelLibrary.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/render/CCRenderPipeline.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/render/CCRenderPipeline.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/render/CCRenderState.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/render/CCRenderState.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/render/Vertex5.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/render/Vertex5.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/render/uv/UV.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/render/uv/UV.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/render/uv/UVRotation.java` | exclude / not_applicable / missing | Dead code in the original: the bundled CodeChicken class is never referenced by any TC4 class, so it had no effect in 4.2.3.5. | oracle thaumcraft/codechicken/lib/render/uv/UVRotation.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/render/uv/UVScale.java` | exclude / not_applicable / missing | Dead code in the original: the bundled CodeChicken class is never referenced by any TC4 class, so it had no effect in 4.2.3.5. | oracle thaumcraft/codechicken/lib/render/uv/UVScale.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/render/uv/UVTransformation.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/render/uv/UVTransformation.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/render/uv/UVTransformationList.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/render/uv/UVTransformationList.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/render/uv/UVTranslation.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/render/uv/UVTranslation.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/util/Copyable.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/util/Copyable.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/vec/AxisCycle.java` | exclude / not_applicable / missing | Dead code in the original: the bundled CodeChicken class is never referenced by any TC4 class, so it had no effect in 4.2.3.5. | oracle thaumcraft/codechicken/lib/vec/AxisCycle.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/vec/CuboidCoord.java` | exclude / not_applicable / missing | Dead code in the original: the bundled CodeChicken class is never referenced by any TC4 class, so it had no effect in 4.2.3.5. | oracle thaumcraft/codechicken/lib/vec/CuboidCoord.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/vec/ITransformation.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/vec/ITransformation.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/vec/IrreversibleTransformationException.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/vec/IrreversibleTransformationException.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/vec/Matrix4.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/vec/Matrix4.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/vec/Quat.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/vec/Quat.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/vec/RedundantTransformation.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/vec/RedundantTransformation.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/vec/Rotation.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/vec/Rotation.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/vec/Scale.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/vec/Scale.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/vec/SwapYZ.java` | exclude / not_applicable / missing | Dead code in the original: the bundled CodeChicken class is never referenced by any TC4 class, so it had no effect in 4.2.3.5. | oracle thaumcraft/codechicken/lib/vec/SwapYZ.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/vec/Transformation.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/vec/Transformation.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/vec/TransformationList.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/vec/TransformationList.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/vec/Translation.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/vec/Translation.java: the class exists in the TC4 4.2.3.5 decompilation |
| `thaumcraft/codechicken/lib/vec/VariableTransformation.java` | exclude / not_applicable / missing | Transitively dead in the original: the bundled CodeChicken class is only reachable from CodeChickenLib's own unused model/lighting subsystem, which nothing in TC4 4.2.3.5 instantiates. | oracle thaumcraft/codechicken/lib/vec/VariableTransformation.java: the class exists in the TC4 4.2.3.5 decompilation |
| `truetyper/FontHelper.java` | exclude / not_applicable / missing | Dead code in the original: the bundled font helper is never referenced. | oracle truetyper/FontHelper.java the class exists in the TC4 4.2.3.5 decompilation |
| `truetyper/FontLoader.java` | exclude / not_applicable / missing | The bundled TrueType font helper is only used to load a system Arial 12 font into a field that the original never draws, so the original had no visible TrueType text. | oracle thaumcraft/client/lib/RenderEventHandler.java:108-112 imports truetyper.FontLoader/TrueTypeFont and loads a system Arial 12 font into the unused field 'font' |
| `truetyper/Formatter.java` | exclude / not_applicable / missing | Transitively dead in the original: the formatter is only reachable from the unreferenced FontHelper. | oracle truetyper/Formatter.java the class exists in the TC4 4.2.3.5 decompilation |
| `truetyper/TrueTypeFont.java` | exclude / not_applicable / missing | The bundled TrueType font type is only instantiated by the unused RenderEventHandler font field. | oracle thaumcraft/client/lib/RenderEventHandler.java:112 declares the TrueTypeFont field and :140 loads it |

## Отклонения, требующие решения

Здесь только то, что меняет видимое поведение.

1. **Лучевой разряд элдритч-голема не рисуется.**
   Оригинал: `oracle thaumcraft/common/entities/monster/boss/EntityEldritchGolem.java:302` →
   `Thaumcraft.proxy.arcLightning` → `FXArc` (гравитационная дуга, `gravity 0.115`,
   `noise 0.25`, `textures/misc/beamh.png`).
   Порт: `native common/src/main/java/thaumcraft/common/entities/monster/EntityEldritchGolem.java:231-234`
   пишет `arcX/arcY/arcZ`, но **нигде не читает** их; класса `FXArc` и его аналога нет.
   Итог: голем во время «аркания» не показывает разряд. То же для `PacketFXBlockArc`.

2. **Луч между сущностями (golem boss) не рисуется.**
   Оригинал: `oracle thaumcraft/common/lib/network/fx/PacketFXBeamPulse.java:71` создаёт
   `FXBeam` (тип 1, reverse, pulse, размер 2.5, blendmode 771, 20 тиков).
   Порт: сетевого пакета beam-pulse нет; `VisNetRender.beam` — это **другой** эффект
   (луч visnet-реле, две скрещённые плоскости, полуширина .105), его нельзя считать переносом.
   Итог: урон и зарядка луча перенесены, визуальная часть — нет.

3. **Луч visnet-реле потерял прозрачность и «flare».**
   Оригинал: `oracle thaumcraft/client/fx/beams/FXBeamPower.java:96-118` — две плоскости
   `beam1.png` (полуширина 0.15×0.7 = .105), прозрачность ограничена диапазоном 0.3…0.8
   (`-0.025` за тик), ×10 при надетых `IRevealer`, **плюс** отдельная аддитивная flare-грань
   на `textures/misc/particles.png` в точке цели.
   Порт: `native common/src/main/java/thaumcraft/client/renderers/VisNetRender.java:77-80` —
   две плоскости той же полуширины, но без ограничения прозрачности 0.3, без затухания,
   без ×10 от `IRevealer` и без flare-граней.

4. **Флюкс-газ больше не имеет переменной высоты.**
   Оригинал: `oracle thaumcraft/client/renderers/block/BlockGasRenderer.java:11-48` —
   `LIGHT_Y_NEG/POS`, `LIGHT_XZ_NEG/POS`, `RENDER_OFFSET`, `getFluidHeightForRender`
   (per-corner высоты, фактор `0.875`).
   Порт: `native common/src/main/java/thaumcraft/client/ThaumcraftFluxClient.java:9-13` —
   обычный полупрозрачный блок.

5. **Лифтатор (Arcane Levitator) потерял свечение.**
   Оригинал: `oracle thaumcraft/client/renderers/block/BlockLifterRenderer.java:18-38` —
   базовый куб **плюс** два эмиссивных внутренних слоя (0.01..0.99 × 0.9..0.99 и
   0.01..0.99 × 0.1..0.9) и светящиеся грани, проецируемые на соседние блоки (`:40-80`).
   Порт: `native common/src/main/resources/assets/thaumcraft/models/block/levitator.json` —
   простой `cube_bottom_top`.

6. **Свечи не капают.**
   Оригинал: `oracle thaumcraft/client/renderers/block/BlockCandleRenderer.java:33-58` —
   1..5 детерминированных восковых потёков на блок (RNG от `x + y*z`).
   Порт: `native common/src/main/resources/assets/thaumcraft/models/block/tallow_candle.json` —
   статичная модель.

7. **Рудная руда потеряла «эмиссивный» минимум яркости и второй проход.**
   Оригинал: `oracle thaumcraft/client/renderers/block/BlockCustomOreRenderer.java:40-63` —
   цветная накладка рисуется с яркостью `max(bb, 160)`, а в fast-graphics добавляется второй
   проход с текстурой камня.
   Порт: `native common/src/main/resources/assets/thaumcraft/models/item/cinnabar_ore.json` —
   статичная модель с `tintindex`.

8. **Пери-метал/дерево/камень-девайсы: не все варианты различаются.**
   Оригинал: `BlockMetalDeviceRenderer.java:18-40` — для metadata 7/8/13 короб W4..W12×W2..W14,
   для 12 короб W3..W13, для 5 крышка 0.8125..1.
   Порт: `native common/src/main/resources/assets/thaumcraft/blockstates/metal_device.json` —
   варианты 0..4, 7..11, 13..15 используют один `metal_device_base`.

9. **Клавиши отличаются от оригинала.**
   Оригинал: `oracle thaumcraft/common/lib/events/KeyHandler.java:8-11` — `keyF` «Change Wand
   Focus» = 33 (`;`), `keyH` «Activate Hover Harness» = 35 (`[`), `keyG` «Misc Wand Toggle»
   = 34 (`'`).
   Порт: `native common/src/main/java/thaumcraft/client/ThaumcraftFocusClient.java:23-24` (F),
   `ThaumcraftTravelGearClient.java:48` (H), `ThaumcraftMiscKeyClient.java:17` (G).
   Кроме того, действие «Misc Wand Toggle» (оригинальный `PacketItemKeyToServer(player, 1)`)
   **не перенесено** — это зафиксировано в комментарии `ThaumcraftMiscKeyClient.java:13-16`.

10. **Аспект-строка на наведённом слоте контейнера отсутствует.**
    Оригинал: `oracle thaumcraft/client/lib/ClientTickEventsFML.java:660-729`
    (`renderAspectsInGui`) рисует ряд иконок аспектов под наведённым слотом GUI.
    Порт: `native common/src/main/java/thaumcraft/client/ThaumcraftGogglesClient.java:41-42`
    прямо фиксирует: «This is not the inventory Shift aspect HUD».

11. **Оптифайн/Iris-шейдеры не поддерживаются.**
    Оригинал: `oracle thaumcraft/client/lib/RenderEventHandler.java:161-197` (`renderShaders`,
    `updateShaderFrameBuffers`) и `oracle thaumcraft/client/lib/ClientTickEventsFML.java:168-241`
    (`checkShaders`, `deactivateShader`).
    Порт: в `common/src` нет ни `ShaderGroup`, ни Iris/OptiFine-интеграции; ресурсы
    `assets/thaumcraft/shaders/{core,post,program}` лежат в репозитории, но никем не
    используются.

12. **Оверлей просканированных блоков (17×17×17) не рисуется.**
    Оригинал: `oracle thaumcraft/client/lib/RenderEventHandler.java:366-463`
    (`startScan`, `showScannedBlocks`, `drawPickScannedObject`).
    Порт: сами данные сканирования перенесены (`ItemThaumometer`, `ScanSession`), но 3D-оверлей
    отсутствует.

13. **Предмет «деревянное устройство» (культистский баннер) выглядит как куб.**
    Оригинал: `oracle thaumcraft/client/renderers/item/ItemBannerRenderer.java:16-45` —
    для предмета с NBT `aspect`/`color` рисуется модель баннера.
    Порт: `native common/src/main/resources/assets/thaumcraft/models/item/wooden_device.json` —
    `parent: thaumcraft:block/wooden_device` (обычный куб), `BlockItem` без рендерера.

14. **Взрыв (`FXBurst`) на ~33 % ярче.**
    Оригинал: `oracle thaumcraft/client/fx/particles/FXBurst.java:45` — `glColor4f(1,1,1,0.75f)`.
    Порт: `native common/src/main/java/thaumcraft/client/fx/particles/MonsterSpriteParticle.java:32`
    — `alpha = 1` для `Kind.BURST`.

15. **Костяной лук в руке использует ванильную позу.**
    Оригинал: `oracle thaumcraft/client/renderers/item/ItemBowBoneRenderer.java:25-31` —
    собственный трансформ (scale 2.6666667, повороты −20/90/−60).
    Порт: `native common/src/main/resources/assets/thaumcraft/models/item/bone_bow.json` —
    `parent: minecraft:item/bow` + три override'а натяжения.

16. **Полоса света эфирного цветка не анимирована.**
    Оригинал: `oracle thaumcraft/client/renderers/tile/TileEtherealBloomRenderer.java:76` —
    `UtilsFX.renderFacingStrip` со скроллингом текстуры.
    Порт: `native common/src/main/java/thaumcraft/client/renderers/tile/TileEtherealBloomRenderer.java:24-25`
    — это уже зафиксировано как «Deviation».

17. **Вращение предмета на столе разрушения заменено подъёмом.**
    Оригинал: `oracle thaumcraft/client/renderers/tile/TileDeconstructionTableRenderer.java:71` —
    `entityitem.field_70290_d = sin(ticks/14)*0.2+0.2` (наклон предмета).
    Порт: `native common/src/main/java/thaumcraft/client/renderers/tile/TileDeconstructionTableRenderer.java:33`
    — подъём по Y на ту же величину.

---

## Дефекты

1. **Элдритч-голем: `arcX/arcY/arcZ` записываются и никогда не читаются**
   (`native common/src/main/java/thaumcraft/common/entities/monster/EntityEldritchGolem.java:231-234`).
   Воспроизводимо: спавн голема, наблюдение за «арканием» — визуала нет, а поля создают
   ложное впечатление работающей механики. Классифицировано как отклонение (1), но по
   признаку «записанное, но неиспользуемое состояние» это дефект.

2. **`FXBurst` теряет множитель прозрачности 0.75** — аддитивный вклад частицы на 33 % выше
   оригинала (`oracle thaumcraft/client/fx/particles/FXBurst.java:45` против
   `native common/src/main/java/thaumcraft/client/fx/particles/MonsterSpriteParticle.java:32`).

3. **Ключи по умолчанию не совпадают с оригиналом** (`;`/`[`/`'` → `F`/`H`/`G`), при этом
   подписи в UI совпадают («Change Wand Focus», «Activate Hover Harness», «Misc Wand
   Toggle»). Игрок, привыкший к TC4, нажимает не ту клавишу; расхождение молча
   меняет поведение, а не только раскладку.

4. **`ThaumcraftMiscKeyClient` заявляет клавишу «Misc Wand Toggle», но её исходное действие
   не перенесено** (комментарий `native common/src/main/java/thaumcraft/client/ThaumcraftMiscKeyClient.java:12-16`
   признаёт это). Клавиша G в 1.21.1 переключает ориентацию Earthmover-лопаты вместо
   оригинального `PacketItemKeyToServer(player, 1)`.

5. **Ресурсы шейдеров TC4 не используются.** `common/src/main/resources/assets/thaumcraft/shaders/`
   содержит `core/`, `post/`, `program/`, но в порту нет ни кода, ни зависимости, которые бы
   их читали — то есть либо мёртвый ресурс, либо недоделанная функция.

---

## Не перенесено

Живая оригинальная функциональность, отсутствующая в порту:

| класс / эффект | оригинальный вызов | состояние в порту |
|---|---|---|
| `FXArc` (дуга разряда) | `ClientProxy.arcLightning` ← `EntityEldritchGolem:302`, `PacketFXBlockArc:64` | отсутствует; в порту есть только `InfusionArcParticle` для другого вызова (`nodeBolt` type 0) |
| `FXBeam` (луч между сущностями) | `PacketFXBeamPulse:44-54` | отсутствует |
| `FXBeamPower` (луч реле) | `TileVisRelay:124` | заменён на `VisNetRender.beam` — геометрия та же, прозрачность/flare нет |
| `FXSmokeSpiral` | `ClientProxy.smokeSpiral` ← `EntityEldritchWarden:191`, `ItemElementalSword:133` | отсутствует |
| `FXSmokeTrail` | `ClientProxy.soulTrail` | отсутствует (в оригинале вызывался 0 раз) |
| `FXScorch` | `UtilsFX.shootFire:139` | отсутствует (в оригинале вызывался 0 раз) |
| `FXBreaking` (вариант slimeJumpFX) | `ClientProxy.slimeJumpFX:1024` | отсутствует |
| `FXBlockRunes` ( rune-блоки, элдритч-блоки, тауометр) | `BlockCosmeticSolid:336/342/350`, `BlockEldritch:304`, `ItemThaumometer:92/103/128` | частично: только для подставки инфузионной матрицы (`InfusionClientEffects:204`) |
| `renderAspectsInGui` | `ClientTickEventsFML:660-729` | отсутствует |
| Оптифайн/Iris-шейдеры | `RenderEventHandler:161-197`, `ClientTickEventsFML:168-241` | отсутствует |
| Оверлей просканированных блоков | `RenderEventHandler:366-463` | отсутствует |
| Радиальное меню фокуса (`REHWandHandler.handleFociRadial`) | `RenderEventHandler:152` | заменено экраном `GuiFocusSelect` (другая механика выбора) |
| `TCFontRenderer` (galactic-шрифт) | `GuiResearchRecipe:175` | отсутствует; atlas `ascii_sga.png` не выбирался и в оригинале |
| Экран конфигурации | `ThaumcraftGuiFactory:24` → `ThaumcraftGuiConfig` | отсутствует (нет FML-хука); значения читаются из properties-файла |
| `BlockGasRenderer` (переменная высота газа) | `ClientProxy.setupBlockRenderers` | заменён плоским полупрозрачным блоком |
| `BlockLifterRenderer` (свечение) | там же | внутренние эмиссивные слои отсутствуют |
| Предмет культистского баннера | `ItemBannerRenderer` | предмет — обычный куб |
| Костяной лук «в руке» (собственный трансформ) | `ItemBowBoneRenderer` | ванильная поза лука |

Мёртвый код оригинала, корректно не перенесённый (не считать потерей):

* `FXBubbleAlt` — 0 ссылок в оракуле.
* `ModelVoidRobe` — 0 ссылок (в оригинале тоже рендерится `ModelRobe`).
* `RenderWatcher` — 0 ссылок: **оригинал не регистрировал рендерер смотрителя**, а порт
  регистрирует (`native common/src/main/java/thaumcraft/client/ThaumcraftEldritchClient.java:37`).
  Это добавление, а не потеря точности.
* `thaumcraft.codechicken.**` — 41 класс: 7 живых (заменены нативно) и 34 мёртвых —
  8 корневых (`DepLoader`, `ColourARGB`, `CCModelLibrary`, `UVRotation`, `UVScale`,
  `AxisCycle`, `SwapYZ`, `CuboidCoord`) и 26 транзитивно мёртвых (весь `lib/render`,
  `lib/lighting`, `lib/colour`, `lib/util` и `lib/vec`-матрицы/кватернионы).
* `truetyper.**` — 4 класса: `FontHelper` (0 ссылок), `Formatter` (только из мёртвого
  `FontHelper`), `TrueTypeFont`/`FontLoader` — только для поля `RenderEventHandler.font`,
  которое ни разу не читается.

---

## Что НЕ удалось проверить

1. **Визуальная приёмка в игре.** Аудит — чтение кода; ни один приведённый выше «дефект»
   не подтверждён скриншотом или клиентским прогоном. Всё, что помечено «визуально»,
   основано на сравнении чисел в коде.
2. **Пиксельное сравнение текстур.** Сверялись пути (`textures/misc/particles.png`,
   `textures/misc/particles2.png`, `textures/models/hemis0..15`, `textures/models/watcher_beam.png`
   и т. п.) и UV-строки, но не сами PNG: возможны расхождения в содержимом атласов.
3. **Полное сравнение переписанных моделей.** `ModelEssentiaCrystalizer` (бывший 1.12
   `ModelCrystalizer`, 786 строк) и `ModelCrabVent` (893 строки) не сверялись куб за кубом;
   для них зафиксировано лишь наличие нативного аналога и его назначение.
4. **Поведение GUI-оверлеев при масштабе.** Раскладка HUD сверена по формулам, но не по
   фактическим координатам при `guiScale != 1` и при GUI-масштабе ≠ auto.
5. **Клиентские сетевые пакеты.** Соответствие wire-форматов (`PacketFXBlockArc`,
   `PacketFXBeamPulse`, `PacketFXZap`, `PacketFXSonic`, `PacketFXWispZap`, `PacketFXBlockZap`)
   нативным payload'ам проверялось по назначению, а не побайтно по протоколу — это область
   сетевого домена.
6. **Модели брони/робов.** `ModelFortressArmor` (646 → 247 строк), `ModelKnightArmor`
   (354 → 156), `ModelLeaderArmor` (365 → 190), `ModelRobe` (483 → 175) сверены по
   назначению и по заявленным в нативных комментариях числам (58 частей, лист 128×64),
   но не поэлементно; риск пропущенной части остаётся.
7. **Шейдеры.** `assets/thaumcraft/shaders/**` не разбирались: в порту они не используются,
   но остаются в репозитории, и их принадлежность к «перенесённому» или «брошенному» не
   установлено.
8. **Область вне домена.** Оригинальный `thaumcraft/common/lib/events/KeyHandler.java`
   (обработчик клавиш) лежит в другом семействе; здесь зафиксированы только расхождения
   клавиш по умолчанию и отсутствие действия «Misc Wand Toggle».
