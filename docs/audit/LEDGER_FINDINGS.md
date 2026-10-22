# Ledger adversarial audit — a1ledger

Baseline: `c6feeda7`; branch `agent/a1ledger`. This is an evidence audit, not a gameplay or visual acceptance run. No Gradle command was run.

## Outcome

- Counts reproduce exactly: **1044/1044 parity_verified**, zero other coverage states; **163 excludes**.
- All **163** excluded rows received whole-oracle class-name and declared-member searches. **77** have zero class-name hits; **86 fail the literal no-counterpart/zero-class-hit test**. This is NOT a claim that 86 gameplay features are missing: 63 are original-bearing rows and 23 are relocated legacy names, including duplicates and dead/library/platform contracts.
- **ModelBoreEmit is a serious invented absence claim:** the original exists and is live. Its native geometry exists too; fix the disposition/rationale, not a nonexistent missing vortex feature.
- Seven excluded rows had false class-absence wording corrected: ModelBoreEmit, relocated item/ItemNodeRenderer, ItemBlocks/BlockMirrorItem, and four thaumcraft/truetyper rows. Other nonzero-name results often explicitly claim replacement or unreachability, not absence.
- **220 evidence entries**, paired across **110 units / 21 families**, were citation-spot-checked. **66 were wrong or materially incomplete** and corrected. Twelve initially had a missing file, past-EOF or single blank/brace citation. The remainder include drift to imports, unrelated methods and obsolete behavioral assertions.
- In total this branch corrects **79 evidence strings and 42 notes**, leaving every disposition, coverage verdict, fingerprint and counts value unchanged.
- Recommend downgrading BlockJar, INode and TileNode pending explicit acceptance of the audio and identity deviations (details below). No downgrade was applied.

## 1.1 / 1.3 — independent counts and fingerprints

The following block was recomputed from row source presence, path_relation, native_counterparts, review.disposition and queue.planned_milestone, not copied from the ledger counts. A Python equality assertion against the entire counts block passed. Matching current counts cannot prove no historical hand edit ever occurred; it does prove no present numeric discrepancy.

```json
{
  "counts": {
    "original_units": 859,
    "legacy_units": 934,
    "union_rows": 1044,
    "shared_path": 749,
    "legacy_only": 185,
    "original_only": 110,
    "native_linked_rows": 37,
    "unique_native_counterparts": 57,
    "dispositions": {
      "adapt": 263,
      "exclude": 163,
      "port": 291,
      "replace": 327
    },
    "milestone_queue": {
      "M00": 75,
      "M01": 57,
      "M02": 96,
      "M03": 37,
      "M04": 20,
      "M05": 29,
      "M06": 27,
      "M07": 14,
      "M08": 84,
      "M09": 120,
      "M10": 69,
      "M11": 143,
      "M12": 80,
      "M13": 68,
      "M14": 125
    },
    "stale_review_fingerprints": 0
  },
  "coverage": {
    "parity_verified": 1044
  }
}
```

Executed before and after each successful ledger-edit batch:

```text
python3 scripts/porting/update_scope_ledger.py --check --original-root /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle
stale_review_fingerprints: 0
```

**Important limitation:** reviewed_sha256 anchors original/legacy sources, not cited native files. Therefore zero stale fingerprints does not detect any of the native citation drift found here.

## 1.2 — full exclusion search table

Oracle root: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`; exactly 859 Java files were searched read-only, with 1,742 per-row identifier queries. Search is Python regex `\bIDENTIFIER\b` over every line (equivalent identifier-boundary grep); counts below are matching lines, not occurrences. C = class identifier; M = every extracted declared method/field plus matching distinctive identifiers quoted by the note. The complete term-by-term line/file/external-line counts are in `LEDGER_EXCLUDE_SEARCHES.json`. No member terms means this source declares no extracted method/field (typically a shell/subclass). Generic member hits are not evidence of a semantic counterpart.

Verdicts in this table intentionally apply the contract’s **literal no-counterpart test**, not the weaker rule “a justification for excluding this path can be imagined.” NOT reproduced means a class with that identifier is in the oracle; existing dead-code/replacement/dedup rationales must be considered separately. This avoids laundering real original classes into absence claims.

| Unit | Search run (all terms; number = matching lines) | C hit count | Verdict / qualification |
|---|---|---:|---|
| `thaumcraft/api/entities/GolemIds.java` | C: `GolemIds`; M: CORE_ALCHEMY=0, CORE_BUTCHER=0, CORE_CHOP=0, CORE_DECANTING=0, CORE_EMPTY=0, CORE_FILL=0, CORE_FISHING=0, CORE_GATHER=0, CORE_GUARD=0, CORE_HARVEST=0, CORE_NONE=0, CORE_SORTING=0, CORE_USE=0, TYPE_CLAY=0, TYPE_FLESH=0, TYPE_IRON=0, TYPE_STONE=0, TYPE_STRAW=0, TYPE_TALLOW=0, TYPE_THAUMIUM=0, TYPE_WOOD=0, UPGRADE_AIR=0, UPGRADE_EARTH=0, UPGRADE_ENTROPY=0, UPGRADE_FIRE=0, UPGRADE_ORDER=0, UPGRADE_WATER=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/api/entities/IGolemInfo.java` | C: `IGolemInfo`; M: getCore=61, getGolemTypeId=0, getUpgradeAmount=42, isAdvancedGolem=0, isAdvanced=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/api/internal/CommonInternals.java` | C: `CommonInternals`; M: generateUniqueItemstackId=0, generatedStack=0, put=548, scanEntities=5, ThaumcraftApi=870, objectTags=17, registerObjectTag=283 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/ClientModelRegistry.java` | C: `ClientModelRegistry`; M: ARCANE_FURNACE_TEXTURES=0, FROST_SHARD_OBJ=0, LIGHTNING_RING_SPRITE=0, bakeFrostShardModel=0, frostShardModel=0, getFrostShardModel=0, onModelBake=0, onTextureStitchPre=0, registerModels=0, replaceAlchemyFurnaceModel=0, replaceArcaneFurnaceModel=0, replaceArcaneFurnaceModels=0, replaceEldritchCrustModels=0, replaceObsidianTotemModels=0, replaceWardedGlassModel=0, ModelRegistryEvent=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/ThaumcraftGuiFactory.java` | C: `ThaumcraftGuiFactory`; M: getHandlerFor=1, initialize=5, runtimeGuiCategories=1, IModGuiFactory=7, ThaumcraftGuiConfig=5 | 2 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/fx/ITCParticle.java` | C: `ITCParticle`; M: getTCParticleLayer=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/fx/beams/FXArc.java` | C: `FXArc`; M: ARC_TEXTURE=0, addLitVertex=0, blendmode=19, buildArcPoints=0, getBrightnessForRender=1, getFXLayer=0, length=458, onUpdate=17, particle=145, points=9, renderParticle=0 | 4 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/fx/beams/FXBeam.java` | C: `FXBeam`; M: BEAM=0, BEAM_1=0, BEAM_2=0, BEAM_3=0, IMPACT=0, addLitVertex=0, blendmode=19, density=0, endMod=23, flicker=0, getBeamAlpha=0, getBeamStripCount=0, getBeamStripRotation=0, getBeamStripVOffset=0, getBeamTexture=0, getBrightnessForRender=1, getFXLayer=0, length=458, movX=4, movY=4, movZ=4, onUpdate=17, particle=145, prevPitch=27, prevSize=14, prevYaw=27, ptX=12, ptY=12, ptZ=12, pulse=48, renderImpact=6, renderParticle=0, reverse=26, rotPitch=34, rotYaw=38, rotationspeed=16, setBeamWidth=0, setBlendMode=0, setEndMod=9, setPulse=10, setReverse=10, setRotationspeed=4, setType=27, tX=49, tY=49, tZ=49, targetEntity=51, type=372, updateBeam=7, updateOrientationFromEndpoints=0, updated=4 | 8 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/fx/beams/FXBeamPower.java` | C: `FXBeamPower`; M: addFlareVertex=0, getBeamAlpha=0, getBeamStripCount=0, getBeamStripRotation=0, getBeamStripVOffset=0, isRevealed=0, onUpdate=17, opacity=25, renderImpact=6, setPulse=10, updateBeam=7 | 7 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/fx/beams/WandEffectOrigin.java` | C: `WandEffectOrigin`; M: SCEPTRE=5, STAFF=0, VERTICAL_EPSILON=0, WAND=0, WORLD_UP=0, active=20, basis=0, calibrationFor=0, forward=0, forwardOffset=0, isFirstPerson=0, pivotHeight=0, resolve=0, resolveContext=0, right=0, side=652, sideOffset=0, sourceYOffset=0, stack=922, up=4, upOffset=0, TipCalibration=0, debugFrame=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/fx/particles/FXBubbleAlt.java` | C: `FXBubbleAlt`; M: baseScale=0, getBrightnessForRender=1, onUpdate=17, particle=145, renderParticle=0, setRGB=11 | 2 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/fx/particles/FXScorch.java` | C: `FXScorch`; M: animals=1, getBrightnessForRender=1, lance=7, mobs=4, onUpdate=17, partDestEnt=1, pvp=1, px=157, py=154, pz=137, renderParticle=0, transferParticleScale=4 | 4 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/fx/particles/FXSmokeDrift.java` | C: `FXSmokeDrift`; M: getFXLayer=0, onUpdate=17, renderParticle=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/fx/particles/FXSmokeSpiral.java` | C: `FXSmokeSpiral`; M: getFXLayer=0, getTCParticleLayer=0, miny=20, onUpdate=17, radius=29, renderParticle=0, start=206 | 4 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/fx/particles/FXSmokeTrail.java` | C: `FXSmokeTrail`; M: getBrightnessForRender=1, onUpdate=17, particle=145, renderParticle=0, setGravity=23, target=423 | 4 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/gui/GuiFactory.java` | C: `GuiFactory`; M: createConfigGui=0, hasConfigGui=0, initialize=5, runtimeGuiCategories=1 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/gui/ThaumcraftGuiConfig.java` | C: `ThaumcraftGuiConfig`; M: getConfigElements=2 | 5 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/integration/jei/JeiRecipeData.java` | C: `JeiRecipeData`; M: addConcrete=0, arcane=3, aspects=840, catalyst=19, central=9, collect=26, components=35, copyRequiredTags=0, crucible=2, exactAlternatives=0, getSkippedCount=0, height=120, infusion=1, infusionAlternatives=0, inputs=4, instability=52, output=25, outputs=0, research=251, shaped=2, shapeless=2, skip=8, sourceCount=0, width=89 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/integration/jei/ResearchVisibility.java` | C: `ResearchVisibility`; M: categoryUid=0, entries=6, hideVisible=0, initializeHidden=0, player=2743, recipe=185, research=251, sink=0, update=2, visible=2, world=7023, isVisible=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/integration/jei/ResearchVisibilityTicker.java` | C: `ResearchVisibilityTicker`; M: onClientTick=0, visibility=0, ResearchVisibility=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/integration/jei/ThaumcraftJeiPlugin.java` | C: `ThaumcraftJeiPlugin`; M: onRuntimeAvailable=0, register=21, registerCategories=0, track=1, visibilityEntries=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/integration/jei/ThaumcraftRecipeCategory.java` | C: `ThaumcraftRecipeCategory`; M: background=6, getBackground=0, getModName=0, getTitle=4, getUid=0, setRecipe=0, titleKey=0, uid=0, IRecipeCategory=0, IRecipeLayout=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/integration/jei/ThaumcraftRecipeWrapper.java` | C: `ThaumcraftRecipeWrapper`; M: ASPECT_COLUMNS=0, OUTPUT_SLOT=0, aspectX=0, aspectY=0, drawInfo=0, getAspects=281, getIngredients=0, getTooltipStrings=0, recipe=185, IRecipeWrapper=0, IGuiItemStackGroup=0, UtilsFX=510, drawTag=35 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/lib/EldritchDiagnostics.java` | C: `EldritchDiagnostics`; M: LOG=0, TAG=0, countEntities=0, fieldHelperFaces=0, fieldHelperLayers=0, frameCounter=0, lastEntityCountNanos=0, lastSummaryNanos=0, lockTESRCalls=0, logRenderType=0, nothingFacesRendered=0, nothingFarFaces=0, nothingInRangeFaces=0, nothingLayerQuads=0, nothingTESRCalls=0, obeliskTESRCalls=0, onFrame=0, renderTypeLogged=0, reset=13, EldritchPerf=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/lib/ItemAspectTooltipHandler.java` | C: `ItemAspectTooltipHandler`; M: drawRectBackground=0, onPostTooltip=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/lib/KeyHandler.java` | C: `KeyHandler`; M: clientTick=0, handleFocusKey=0, handleHoverKey=0, handleMiscKey=0, keyF=3, keyG=3, keyH=3, keyPressedF=6, keyPressedG=5, keyPressedH=6, lastPressF=4, lastPressG=3, lastPressH=3, radialActive=12, radialLock=9, releaseAllKeys=0, PacketItemKeyToServer=10 | 19 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/client/lib/TCFontRenderer.java` | C: `TCFontRenderer`; M: FONT_GALACTIC=1, FONT_HEIGHT=13, FONT_NORMAL=2, alpha=108, bidiFlag=5, bidiReorder=3, blue=51, boldStyle=6, charWidth=7, colorCode=3, drawSplitString=3, drawString=17, drawStringWithShadow=1, field_111273_g=5, field_111274_c=4, fontRandom=2, func_111271_a=2, getBidiFlag=1, getCharWidth=4, getFormatFromString=2, getStringWidth=5, getUnicodeFlag=1, glyphWidth=8, green=50, isFormatColor=3, isFormatSpecial=2, italicStyle=7, listFormattedStringToWidth=4, loadGlyphTexture=2, posX=58, posY=53, randomStyle=6, readFontData=2, readFontTexture=2, readGlyphSizes=2, red=61, renderCharAtPos=3, renderDefaultChar=2, renderEngine=4, renderSplitString=4, renderString=5, renderStringAligned=3, renderStringAtPos=2, renderUnicodeChar=2, resetStyles=4, setBidiFlag=1, setUnicodeFlag=1, sizeStringToWidth=2, splitStringWidth=1, strikethroughStyle=6, textColor=6, trimStringNewline=5, trimStringToWidth=3, underlineStyle=7, unicodeFlag=8, FontRenderer=19 | 10 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/renderers/block/AlchemyFurnaceBakedModel.java` | C: `AlchemyFurnaceBakedModel`; M: FACE_BAKERY=0, FULL_UV=0, addFace=0, buildQuads=0, delegate=0, getExtendedBoolean=0, getItemCameraTransforms=0, getOverrides=0, getParticleTexture=12, getQuads=0, isAmbientOcclusion=0, isBuiltInRenderer=0, isGui3d=0, sprite=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/block/ArcaneFurnaceBakedModel.java` | C: `ArcaneFurnaceBakedModel`; M: FACE_BAKERY=0, FULL_UV=0, NOZZLE_SHEET_THICKNESS=0, addFace=0, addNozzleQuads=0, addTexturedCube=0, buildQuads=0, createNozzlePlane=0, delegate=0, fallbackType=0, getExtendedInt=0, getFacing=11, getItemCameraTransforms=0, getOverrides=0, getParticleTexture=12, getQuads=0, getType=13, isAmbientOcclusion=0, isBuiltInRenderer=0, isGui3d=0, sprite=0, textureForSide=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/block/BlockEssentiaReservoirRenderer.java` | C: `BlockEssentiaReservoirRenderer`; M: getRenderId=21, renderInventoryBlock=21, renderWorldBlock=21, shouldRender3DInInventory=21, ISimpleBlockRenderingHandler=67 | 4 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/renderers/block/BlockTaintFibreRenderer.java` | C: `BlockTaintFibreRenderer`; M: getRenderId=21, renderInventoryBlock=21, renderWorldBlock=21, shouldRender3DInInventory=21, ISimpleBlockRenderingHandler=67, BlockTaintFibres=27 | 4 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/renderers/block/BlockTaintRenderer.java` | C: `BlockTaintRenderer`; M: getRenderId=21, renderInventoryBlock=21, renderWorldBlock=21, shouldRender3DInInventory=21 | 4 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/renderers/block/BlockTubeRenderer.java` | C: `BlockTubeRenderer`; M: getConnectableTile=24, getRenderId=21, renderInventoryBlock=21, renderWorldBlock=21, shouldRender3DInInventory=21, BlockTube=19 | 11 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/renderers/block/BlockWardedRenderer.java` | C: `BlockWardedRenderer`; M: getRenderId=21, renderInventoryBlock=21, renderWorldBlock=21, shouldRender3DInInventory=21 | 3 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/renderers/block/WardedGlassBakedModel.java` | C: `WardedGlassBakedModel`; M: FACE_BAKERY=0, FULL_UV=0, TEXTURE_COUNT=0, delegate=0, getItemCameraTransforms=0, getOverrides=0, getParticleTexture=12, getQuads=0, getTextureIndex=0, isAmbientOcclusion=0, isBuiltInRenderer=0, isGui3d=0, makeFace=0, sprite=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/entity/RenderFallbackBiped.java` | C: `RenderFallbackBiped`; M: getEntityTexture=0, texture=357 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/entity/RenderFallbackLiving.java` | C: `RenderFallbackLiving`; M: getEntityTexture=0, texture=357 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/entity/RenderNoop.java` | C: `RenderNoop`; M: NOOP_TEXTURE=0, doRender=2, getEntityTexture=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/CrystalPerspectiveModel.java` | C: `CrystalPerspectiveModel`; M: delegate=0, getItemCameraTransforms=0, getOverrides=0, getParticleTexture=12, getQuads=0, isAmbientOcclusion=0, isBuiltInRenderer=0, isGui3d=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/FirstPersonWandTipOrigin.java` | C: `FirstPersonWandTipOrigin`; M: MAX_CAMERA_DISTANCE=0, MIN_CAMERA_DISTANCE=0, MODELVIEW=0, POINT=0, PROJECTION=0, RESULT=0, VIEWPORT=0, capture=0, depth=30, generation=2, hand=0, isFinite=0, isFirstPersonTransform=0, isLocalFirstPerson=0, nextWorldFrame=0, physicalSide=0, player=2743, projectPoint=0, readMatrices=0, request=1, resolveAndRequest=0, sample=5, side=652, sideForTransform=0, stack=922, u=74, unprojectPoint=0, v=162, validMatrix=0, validViewport=0, viewportHeight=0, viewportWidth=0, viewportX=0, viewportY=0, world=7023, gluUnProject=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/ItemBannerRenderer.java` | C: `ItemBannerRenderer`; M: handleRenderType=8, renderItem=10, shouldUseRenderHelper=10 | 3 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/renderers/item/ItemChestHungryRenderer.java` | C: `ItemChestHungryRenderer`; M: TEXTURE=0, model=743, renderByItem=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/ItemCrystalRenderer.java` | C: `ItemCrystalRenderer`; M: CURRENT_TRANSFORM=0, crystalRenderer=0, eldritchCrystalRenderer=0, getBlockMetadata=0, isFirstPerson=0, metadata=418, renderByItem=0, restoreLegacyInventoryOrigin=0, setTransformType=0, TileCrystalRenderer=3, renderItem=10 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/ItemEldritchRenderer.java` | C: `ItemEldritchRenderer`; M: altarRenderer=0, capRenderer=0, crabSpawnerRenderer=0, lockRenderer=0, obeliskRenderer=0, renderByItem=0, TileEntityItemStackRenderer=0, TileEldritchCapRenderer=6, TileEldritchObeliskRenderer=3, TileEldritchLockRenderer=4, TileEldritchCrabSpawnerRenderer=3 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/ItemEssentiaReservoirRenderer.java` | C: `ItemEssentiaReservoirRenderer`; M: createTile=0, renderByItem=0, renderReservoirCore=0, reservoirRenderer=0, TileEssentiaReservoirRenderer=3 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/ItemJarRenderer.java` | C: `ItemJarRenderer`; M: applyFillableData=0, applyNodeData=0, createTile=0, renderByItem=0, renderer=1378 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/ItemMetalDeviceRenderer.java` | C: `ItemMetalDeviceRenderer`; M: alembicRenderer=0, chargerRenderer=0, relayRenderer=0, renderByItem=0, restoreLegacyInventoryOrigin=0, thaumatoriumRenderer=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/ItemNodeRenderer.java` | C: `ItemNodeRenderer`; M: DEFAULT_ASPECTS=0, drawAnimatedQuadStrip=0, renderByItem=0, renderItemNode=7, TileNodeRenderer=18 | 10 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/client/renderers/item/ItemStoneDeviceRenderer.java` | C: `ItemStoneDeviceRenderer`; M: converterRenderer=0, fluxScrubberRenderer=0, renderByItem=0, stabilizerRenderer=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/ItemTableRenderer.java` | C: `ItemTableRenderer`; M: arcaneWorkbenchRenderer=0, deconstructionRenderer=0, renderByItem=0, researchTable=0, researchTableRenderer=0, restoreLegacyInventoryOrigin=0, tableRenderer=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/ItemThaumometerRenderer.java` | C: `ItemThaumometerRenderer`; M: CURRENT_TRANSFORM=0, DEBUG_LOG_INTERVAL_MS=0, FIRST_PERSON_CENTER_OFFSET_X=0, FIRST_PERSON_CENTER_OFFSET_Y=0, HUD_SCALE_MULTIPLIER=0, SCANNER_OBJ=0, SCANNER_TEXTURE=0, SCANSCREEN_TEXTURE=0, TC4_TO_TC6_VERTICAL_CENTER=0, TC4_TO_TC6_Y_ROTATION=0, applyTc6ScannerBasis=0, doScan=5, getScannedNodeTile=0, getScannerGlow=0, isFirstPerson=0, isThirdPerson=0, lastReadoutDebugLogMs=0, loadScannerModel=0, logReadoutDebug=0, renderByItem=0, renderFirstPerson=0, renderFirstPersonArms=0, renderScanReadout=0, renderScanner=0, renderScannerDisplay=0, renderScannerModel=0, renderTableDisplay=0, scannerModel=0, setTransformType=0, ItemThaumometer=7 | 3 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/renderers/item/ItemTubeRenderer.java` | C: `ItemTubeRenderer`; M: crystalizerRenderer=0, renderBakedShell=0, renderByItem=0, valveRenderer=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/ItemWoodenDeviceRenderer.java` | C: `ItemWoodenDeviceRenderer`; M: CURRENT_TRANSFORM=0, applyBannerData=0, bannerRenderer=0, bellowsRenderer=0, boreBaseRenderer=0, boreRenderer=0, renderByItem=0, restoreLegacyInventoryOrigin=0, setTransformType=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/LifterItemColor.java` | C: `LifterItemColor`; M: colorMultiplier=0, BlockCustomOreItem=27 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/ThaumometerPerspectiveModel.java` | C: `ThaumometerPerspectiveModel`; M: delegate=0, getItemCameraTransforms=0, getOverrides=0, getParticleTexture=12, getQuads=0, isAmbientOcclusion=0, isBuiltInRenderer=0, isGui3d=0, handlePerspective=0, ItemThaumometerRenderer=3, setTransformType=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/TrunkSpawnerPerspectiveModel.java` | C: `TrunkSpawnerPerspectiveModel`; M: delegate=0, getItemCameraTransforms=0, getOverrides=0, getParticleTexture=12, getQuads=0, isAmbientOcclusion=0, isBuiltInRenderer=0, isGui3d=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/WandPerspectiveModel.java` | C: `WandPerspectiveModel`; M: delegate=0, getItemCameraTransforms=0, getOverrides=0, getParticleTexture=12, getQuads=0, isAmbientOcclusion=0, isBuiltInRenderer=0, isGui3d=0, ItemWandRenderer=3 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/WandPoseMath.java` | C: `WandPoseMath`; M: rotateX=0, rotateZ=0, transformPoint=0, contextRotateX=0, contextRotateZ=0, startupRotateX=0, waveRotateX=0, waveRotateZ=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/WandRenderCalibration.java` | C: `WandRenderCalibration`; M: DEBUG_PROPERTY=0, LOGGER=0, applyContext=0, arr=0, copy=100, f=717, firstPersonScaleX=0, get=965, isDebug=0, mirrorFromRightHand=0, onResourceManagerReload=0, putContext=0, readAsset=0, readConfigFile=0, readFloatArray=0, readFully=0, reload=0, resolveKind=0, scaleMulX=0, scaleX=10, toString=106, ItemWandRenderer=3 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/WandUsePose.java` | C: `WandUsePose`; M: contextRotateX=0, contextRotateZ=0, startup=0, startupRotateX=0, waveRotateX=0, waveRotateZ=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/WandUsePoseSampler.java` | C: `WandUsePoseSampler`; M: sample=5, WandFocusAnimation=15 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/item/WoodenDevicePerspectiveModel.java` | C: `WoodenDevicePerspectiveModel`; M: delegate=0, getItemCameraTransforms=0, getOverrides=0, getParticleTexture=12, getQuads=0, isAmbientOcclusion=0, isBuiltInRenderer=0, isGui3d=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/models/ModelAlembic.java` | C: `ModelAlembic`; M: LEGS_TRIANGLES=0, NORMALS=0, PANEL_TRIANGLES=0, POT_TRIANGLES=0, TUBE_MAIN_TRIANGLES=0, TUBE_SMALL_TRIANGLES=0, UVS=0, VERTICES=0, addVertex=0, render=95, renderLegs=0, renderPanel=0, renderPot=0, renderTubeMain=0, renderTubeSmall=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/models/ModelBoreEmit.java` | C: `ModelBoreEmit`; M: cross1=0, cross2=0, cross3=0, knob=0, render=95, rod=35 | 4 | NOT reproduced — FALSE absence assertion; live model (serious) |
| `thaumcraft/client/renderers/models/ModelFluxScrubber.java` | C: `ModelFluxScrubber`; M: obeliskCap=0, renderCap=0, renderTip=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/models/ModelNodeStabilizer.java` | C: `ModelNodeStabilizer`; M: LOCK_TRIANGLES=0, NORMALS=0, PISTON_TRIANGLES=0, UVS=0, VERTICES=0, addVertex=0, renderGroup=0, renderLock=0, renderPiston=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/models/ModelThaumatorium.java` | C: `ModelThaumatorium`; M: NORMALS=0, OBJ_UV_INSET=0, TRIANGLES=0, UVS=0, VERTICES=0, addTriangle=0, addVertex=0, insetTextureCoordinate=0, renderAll=19 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/models/ModelVoidRobe.java` | C: `ModelVoidRobe`; M: func_78088_a=33, setRotation=465, setRotationAnglesZombie=10 | 2 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/renderers/tile/ExtrudedSpriteRenderHelper.java` | C: `ExtrudedSpriteRenderHelper`; M: vertex=1 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/tile/HoleRenderBatchCache.java` | C: `HoleRenderBatchCache`; M: MAX_GROUP_BLOCKS=0, buildGroup=0, buildRenderGroup=0, cachedDimension=0, cachedWorld=0, cachedWorldTime=0, cellA=0, cellB=0, compare=11, createRect=0, equals=131, face=201, frameId=0, from=33, hashCode=31, isHoleBlock=0, mergeCells=0, nextFrame=0, pack=17, plane=0, refreshWorldCache=0, renderedFrame=0, shouldRenderFace=0, unpackA=0, unpackB=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/tile/ItemJarFilledRenderer.java` | C: `ItemJarFilledRenderer`; M: handleRenderType=8, renderItem=10, shouldUseRenderHelper=10, ItemJar=0, itemJarFilled=10 | 3 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/renderers/tile/LayeredFieldPlaneHelper.java` | C: `LayeredFieldPlaneHelper`; M: TEX_GEN_Q_NS=0, TEX_GEN_Q_UP=0, TEX_GEN_R=0, TEX_GEN_S=0, TEX_GEN_T_NS=0, TEX_GEN_T_UP=0, addFaceVertices=0, disableTexGen=0, drawFallbackFace=0, drawGeneratedFace=0, makeBuffer=0, setTexGenPlanes=0, setupTexGen=0, setupTextureMatrix=0, vertex=1 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/tile/TileArcanePressurePlateRenderer.java` | C: `TileArcanePressurePlateRenderer`; M: MAX=0, MIN=0, TEXTURES=0, drawPlate=0, face=201, render=95, TileArcanePressurePlate=13 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/tile/TileEssentiaReservoirRenderer.java` | C: `TileEssentiaReservoirRenderer`; M: RESERVOIR_OBJ=0, RESERVOIR_TEXTURE=0, drawTexturedCuboid=0, face=201, loadReservoirModel=0, model=743, render=95, renderLiquid=4, renderReservoirShell=0, translateFromOrientation=20, TileEssentiaReservoir=32 | 3 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/client/renderers/tile/TileLifterRenderer.java` | C: `TileLifterRenderer`; M: SIDE_GLOW_COLOR=0, TOP_GLOW_COLOR=0, render=95, shouldRenderFace=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/tile/TileRenderHelper.java` | C: `TileRenderHelper`; M: WISPY_TEXTURE=0, drawWispyStrip=0, resolveRenderWorld=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/tile/TileSensorRenderer.java` | C: `TileSensorRenderer`; M: EPSILON=0, render=95, TileSensor=33 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/tile/TileTubeFilterRenderer.java` | C: `TileTubeFilterRenderer`; M: render=95 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/tile/TileTubeRestrictRenderer.java` | C: `TileTubeRestrictRenderer`; M: render=95 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/client/renderers/tile/TubeConduitRenderHelper.java` | C: `TubeConduitRenderHelper`; M: ARM_MAX=0, ARM_MIN=0, BUFFER_MAX=0, BUFFER_MIN=0, EXTENSION=0, FALLBACK_MAX=0, FALLBACK_MIN=0, FILTER_MAX=0, FILTER_MIN=0, JOINT_MAX=0, JOINT_MIN=0, armTexture=0, atlas=0, connected=3, drawArms=0, drawCenter=0, drawTubeCuboid=0, extendedMaximum=0, extendedMinimum=0, findNeighbours=0, hasExternalNeighbour=0, shouldExtend=0, vertex=1 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/codechicken/core/launch/DepLoader.java` | C: `DepLoader`; M: activateDeps=2, addClasspath=2, addDep=2, call=5, checkExisting=2, container=135, coreLib=7, currentActivity=5, deleteMod=3, depSet=4, download=8, downloadBuffer=8, downloadMonitor=12, existing=6, file=36, filename=13, getASMTransformerClass=1, getAccessTransformerClass=1, getModContainerClass=1, getSetupClass=1, hyperlinkUpdate=1, injectData=1, inst=17, load=835, loadDeps=2, loadJSon=3, loadJSonArr=2, loadJson=3, makeDialog=4, makeProgressPanel=2, matches=21, mergeNew=2, modFiles=2, modsDir=4, name=121, owner=67, pattern=13, popupWindow=5, progress=23, propertyChange=1, requestClose=3, resetProgress=4, scanDepInfo=3, scanDepInfos=2, scanDepInfosWeb=1, setPokeThread=5, shouldStopIt=5, showErrorDialog=4, updateProgress=4, updateProgressString=5, url=18, v_modsDir=9, version=10, windowClosing=1, IFMLLoadingPlugin=3, IFMLCallHook=3, DepLoadInst=4 | 13 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/colour/Colour.java` | C: `Colour`; M: a=1225, add=1768, argb=1, b=660, copy=100, equals=131, g=239, glColour=2, interpolate=3, invert=1, multiply=25, multiplyC=3, pack=17, r=359, rgb=2, rgba=18, scale=141, set=128, sub=13, toString=106, MathHelper=1179 | 21 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/colour/ColourARGB.java` | C: `ColourARGB`; M: copy=100, pack=17 | 7 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/colour/ColourRGBA.java` | C: `ColourRGBA`; M: copy=100, multiply=25, multiplyC=3, pack=17 | 13 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/lighting/LC.java` | C: `LC`; M: compute=5, computeO=2, copy=100, fa=28, fb=104, fc=19, fd=36, set=128, side=652 | 24 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/lighting/LightMatrix.java` | C: `LightMatrix`; M: aSamples=3, access=8, ao=5, bSamples=3, brightness=28, computed=4, interp=3, interpAO=2, interpBrightness=2, load=835, locate=1, operate=13, operationID=10, operationIndex=12, pos=74, qsamplem=2, sample=5, sampled=3, sideSample=3, sideao=2, ssamplem=2, BlockCoord=58 | 6 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/lighting/LightModel.java` | C: `LightModel`; M: addLight=2, ambient=8, apply=67, diffuse=5, lightCount=3, lights=3, load=835, operate=13, operationID=10, operationIndex=12, position=9, reducePlanar=2, setAmbient=3, setDiffuse=2, standardLightModel=2 | 9 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/lighting/PlanarLightModel.java` | C: `PlanarLightModel`; M: colours=13, load=835, operate=13, operationID=10, standardLightModel=2 | 6 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/render/CCModel.java` | C: `CCModel`; M: addNormal=3, apply=67, assertMatch=5, attributes=32, backfacedCopy=1, bounds=7, clean=4, collapse=1, combine=2, computeLightCoords=1, computeLighting=1, computeNormals=5, copy=100, createModel=3, expand=8, exportObj=1, findSide=4, generateBackface=3, generateBlock=7, generateBox=1, generateSidedModels=1, generateSidedModelsH=1, generateSidedPart=3, generateSidedParts=1, generateSidedPartsH=1, getVertices=4, hasAttribute=7, illegalAssert=6, newModel=12, normalMatcher=2, normalPattern=2, normals=41, parseDoubles=4, polyMatcher=2, polyPattern=2, pos=74, positionEqual=2, prepareVertex=3, quadModel=1, quadulate=2, render=95, setColour=9, shrinkUVs=1, sidedCopy=3, smoothNormals=3, triModel=1, triangulate=2, twoFacedCopy=1, uvwMatcher=2, uvwPattern=2, vertMatcher=2, vertPattern=2, vertexMode=26, verts=143, vp=24, zOffset=1 | 97 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/render/CCModelLibrary.java` | C: `CCModelLibrary`; M: addIcosahedronTriangle=21, generateIcosahedron=2, getRenderMatrix=1, i=4069, icosahedron4=6, icosahedron7=7 | 29 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/render/CCRenderPipeline.java` | C: `CCRenderPipeline`; M: add=1768, addAttribute=4, addDependency=10, addRequirement=3, attribs=5, build=1, builder=3, deps=6, loading=7, nodes=66, op=28, operate=13, ops=25, rebuild=5, render=95, reset=13, setPipeline=7, sorted=5, unbuild=3 | 9 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/render/CCRenderState.java` | C: `CCRenderState`; M: active=20, alphaOverride=5, arrayCopy=4, attributeIndex=6, baseColour=5, bindModel=2, brightness=28, changeTexture=3, colour=42, colourAttrib=9, colourRef=5, currentVertexFormat=0, draw=1, firstVertexIndex=3, getAttribute=4, getVertices=4, hasAttribute=7, hasBrightness=5, hasColour=6, hasNormal=5, lastVertexIndex=3, lc=11, lcRef=5, lightCoordAttrib=4, lightMatrix=3, load=835, model=743, newArray=7, nextOperationIndex=3, normal=37, normalAttrib=10, normalRef=5, operate=13, operationCount=2, operationID=10, operationIndex=12, pipeline=19, pos=74, prepareVertex=3, pullLightmap=1, registerOperation=6, registerVertexAttribute=2, render=95, reset=13, runPipeline=2, setBrightness=20, setColour=9, setModel=4, setNormal=4, setPipeline=7, side=652, sideAttrib=6, sideRef=4, startDrawing=3, useNormals=2, vec=353, vert=31, vertexAttributes=5, vertexIndex=7, writeVert=2 | 98 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/render/Vertex5.java` | C: `Vertex5`; M: apply=67, copy=100, set=128, toString=106, uv=65, vec=353 | 79 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/render/uv/UV.java` | C: `UV`; M: add=1768, apply=67, copy=100, equals=131, multiply=25, set=128, tex=45, toString=106, u=74, v=162 | 38 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/render/uv/UVRotation.java` | C: `UVRotation`; M: angle=76, apply=67, inverse=26, isRedundant=19, merge=172, toString=106, MathHelper=1179 | 6 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/render/uv/UVScale.java` | C: `UVScale`; M: apply=67, inverse=26, toString=106 | 5 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/render/uv/UVTransformation.java` | C: `UVTransformation`; M: at=22, load=835, operate=13, operationID=10, operationIndex=12, with=887 | 39 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/render/uv/UVTransformationList.java` | C: `UVTransformationList`; M: apply=67, compact=8, inverse=26, isRedundant=19, prepend=2, toString=106, transformations=42, with=887 | 15 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/render/uv/UVTranslation.java` | C: `UVTranslation`; M: apply=67, at=22, du=7, dv=12, inverse=26, isRedundant=19, merge=172, toString=106, MathHelper=1179 | 12 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/util/Copyable.java` | C: `Copyable`; M: copy=100 | 26 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/vec/AxisCycle.java` | C: `AxisCycle`; M: apply=67, cycles=3, inverse=26 | 1 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/vec/CuboidCoord.java` | C: `CuboidCoord`; M: bounds=7, contains=122, copy=100, expand=8, getCenter=1, getCenterVec=1, getSide=4, getVolume=1, hasNext=16, include=3, intArray=2, iterator=34, max=224, min=246, next=61, remove=139, set=128, setSide=4, size=688, toAABB=3, BlockCoord=58 | 24 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/vec/ITransformation.java` | C: `ITransformation`; M: apply=67, at=22, inverse=26, isRedundant=19, merge=172, with=887 | 8 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/vec/IrreversibleTransformationException.java` | C: `IrreversibleTransformationException`; M: getMessage=1, t=321 | 4 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/vec/Matrix4.java` | C: `Matrix4`; M: apply=67, applyN=14, copy=100, glApply=10, glBuf=4, inverse=26, leftMultiply=1, m00=32, m01=31, m02=30, m03=26, m10=31, m11=32, m12=30, m13=26, m20=31, m21=31, m22=31, m23=26, m30=30, m31=32, m32=29, m33=26, mult3x3=3, multiply=25, rotate=23, scale=141, set=128, setIdentity=1, toString=106, translate=5, transpose=1 | 55 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/vec/Quat.java` | C: `Quat`; M: aroundAxis=7, copy=100, mag=9, multiply=25, normalize=26, rightMultiply=1, rotate=23, rotation=29, s=247, set=128, setAroundAxis=4, toString=106, x=4128, y=3942, z=3824, MathHelper=1179 | 30 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/vec/RedundantTransformation.java` | C: `RedundantTransformation`; M: apply=67, applyN=14, at=22, glApply=10, inverse=26, isRedundant=19, merge=172, toString=106 | 8 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/vec/Rotation.java` | C: `Rotation`; M: angle=76, apply=67, applyN=14, axes=7, axis=49, getSideFromLookAngle=1, getSidedRotation=1, glApply=10, inverse=26, isRedundant=19, merge=172, quarterRotations=4, quat=33, rotSideMap=2, rotateSide=2, rotationTo=1, sideOrientation=1, sideRotMap=2, sideRotOffsets=2, sideRotations=8, toQuat=2, toString=106, MathHelper=1179 | 28 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/vec/Scale.java` | C: `Scale`; M: apply=67, applyN=14, factor=16, glApply=10, inverse=26, isRedundant=19, merge=172, toString=106 | 10 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/vec/SwapYZ.java` | C: `SwapYZ`; M: apply=67, inverse=26 | 2 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/vec/Transformation.java` | C: `Transformation`; M: apply=67, applyN=14, at=22, glApply=10, load=835, operate=13, operationID=10, operationIndex=12, with=887 | 85 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/vec/TransformationList.java` | C: `TransformationList`; M: apply=67, applyN=14, compact=8, compile=9, glApply=10, inverse=26, isRedundant=19, mat=119, prepend=2, reverseCompile=1, toString=106, transformations=42, with=887 | 18 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/vec/Translation.java` | C: `Translation`; M: apply=67, applyN=14, at=22, glApply=10, inverse=26, isRedundant=19, merge=172, toString=106, vec=353 | 12 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/codechicken/lib/vec/VariableTransformation.java` | C: `VariableTransformation`; M: apply=67, applyN=14, glApply=10, mat=119 | 16 | NOT reproduced — bundled original; zero external TC4 class-name references confirmed, not absence |
| `thaumcraft/common/blocks/BlockStairsArcaneStone.java` | C: `BlockStairsArcaneStone`; M: getRenderLayer=0, isFullCube=0, isOpaqueCube=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/blocks/BlockStairsGreatwood.java` | C: `BlockStairsGreatwood`; M: getRenderLayer=0, isFullCube=0, isOpaqueCube=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/blocks/BlockStairsSilverwood.java` | C: `BlockStairsSilverwood`; M: getRenderLayer=0, isFullCube=0, isOpaqueCube=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/blocks/BlockThaumcraftStairs.java` | C: `BlockThaumcraftStairs`; M: randomDisplayTick=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/blocks/ItemBlocks/BlockAiryItem.java` | C: `BlockAiryItem`; M: getMetadata=0, getTranslationKey=0, ItemBlocks=0, BlockAiry=8 | 4 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/ItemBlocks/BlockArcaneFurnaceItem.java` | C: `BlockArcaneFurnaceItem`; M: getTranslationKey=0, ItemBlocks=0, BlockArcaneFurnace=13 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/blocks/ItemBlocks/BlockCosmeticOpaqueItem.java` | C: `BlockCosmeticOpaqueItem`; M: getMetadata=0, getTranslationKey=0, ItemBlocks=0 | 4 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/ItemBlocks/BlockCosmeticSolidItem.java` | C: `BlockCosmeticSolidItem`; M: getMetadata=0, getTranslationKey=0, ItemBlocks=0 | 4 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/ItemBlocks/BlockCrystalItem.java` | C: `BlockCrystalItem`; M: colors=51, getMetadata=0, getTranslationKey=0, ItemBlocks=0 | 4 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/ItemBlocks/BlockCustomOreItem.java` | C: `BlockCustomOreItem`; M: colors=51, getMetadata=0, getTranslationKey=0, ItemBlocks=0 | 27 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/ItemBlocks/BlockCustomPlantItem.java` | C: `BlockCustomPlantItem`; M: getMetadata=0, getTranslationKey=0, ItemBlocks=0 | 4 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/ItemBlocks/BlockEssentiaReservoirItem.java` | C: `BlockEssentiaReservoirItem`; M: getMetadata=0, placeBlockAt=21, ItemBlocks=0, getOpposite=88 | 4 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/ItemBlocks/BlockFluxGasItem.java` | C: `BlockFluxGasItem`; M: getMetadata=0, getTranslationKey=0, ItemBlocks=0 | 4 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/ItemBlocks/BlockFluxGooItem.java` | C: `BlockFluxGooItem`; M: getMetadata=0, getTranslationKey=0, ItemBlocks=0 | 4 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/ItemBlocks/BlockMagicalLeavesItem.java` | C: `BlockMagicalLeavesItem`; M: getMetadata=0, getTranslationKey=0, ItemBlocks=0 | 4 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/ItemBlocks/BlockMetadataItem.java` | C: `BlockMetadataItem`; M: getMetadata=0, getTranslationKey=0, ItemBlocks=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/blocks/ItemBlocks/BlockMetalDeviceItem.java` | C: `BlockMetalDeviceItem`; M: placeBlockAt=21, ItemBlocks=0 | 4 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/ItemBlocks/BlockMirrorItem.java` | C: `BlockMirrorItem`; M: addInformation=0, getTranslationKey=0, makeLinkedMirrorStack=0, onItemUseFirst=19, placeBlockAt=21, ItemBlocks=0, BlockMirror=6 | 4 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/ItemBlocks/BlockStoneDeviceItem.java` | C: `BlockStoneDeviceItem`; M: placeBlockAt=21, ItemBlocks=0 | 4 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/ItemBlocks/BlockTubeItem.java` | C: `BlockTubeItem`; M: placeBlockAt=21, ItemBlocks=0, getOpposite=88 | 4 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/ItemBlocks/BlockWoodenDeviceItem.java` | C: `BlockWoodenDeviceItem`; M: canPlaceBlockOnSide=0, getTranslationKey=0, hasSolidSupport=0, placeBlockAt=21, ItemBlocks=0 | 4 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/blocks/JarStepSound.java` | C: `JarStepSound`; M: func_150495_a=12, func_150498_e=8, SoundType=8 | 2 | NOT reproduced — original class present but unreferenced; BlockJar uses CustomStepSound |
| `thaumcraft/common/compat/ThaumcraftSixCompatibility.java` | C: `ThaumcraftSixCompatibility`; M: initBlockAliases=0, initItemAliases=0, postAspectRegistryEvent=0, BlocksTC=0, ItemsTC=0, AspectRegistryEvent=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/config/MaterialAiry.java` | C: `MaterialAiry`; M: blocksMovement=0, isSolid=0 | 4 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/common/config/MaterialTaint.java` | C: `MaterialTaint`; M: func_76219_n=5, func_76220_a=9, func_76221_f=4, func_76222_j=2, func_76227_m=2, func_76228_b=2, func_76230_c=5, mobilityFlag=6 | 5 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/common/config/recipes/ConfigRecipesArcaneSlice.java` | C: `ConfigRecipesArcaneSlice`; M: getWandCapCost=0, getWandRodCost=0, initializeArcaneRecipeBaseline=0, registerArcaneRecipe=0, registerShapelessArcaneRecipe=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/config/recipes/ConfigRecipesCrucibleSlice.java` | C: `ConfigRecipesCrucibleSlice`; M: initializeCrucibleRecipeBaseline=0, recipeAlumentum=0, recipeNitor=0, recipeThaumium=0, recipeVoidMetal=0, recipeVoidSeed=0, CrucibleRecipe=88, ConfigRecipes=58 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/config/recipes/ConfigRecipesSmeltingSlice.java` | C: `ConfigRecipesSmeltingSlice`; M: initializeSmeltingBaseline=0, initializeSmeltingBonusBaseline=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/config/recipes/ConfigRecipesSpecialSlice.java` | C: `ConfigRecipesSpecialSlice`; M: DYES=0, createBasicWandRecipeOutput=0, registerSpecialRecipes=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/container/SlotLimitedByItemstack.java` | C: `SlotLimitedByItemstack`; M: func_75214_a=13 | 2 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `thaumcraft/common/integration/waila/GolemWailaPlugin.java` | C: `GolemWailaPlugin`; M: getWailaBody=0, register=21, WailaPlugin=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/items/resources/ItemCrystalEssence.java` | C: `ItemCrystalEssence`; M: (none) | 9 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/lib/CommandThaumcraft.java` | C: `CommandThaumcraft`; M: CommandBase=3 | 8 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/common/lib/SoundsTC.java` | C: `SoundsTC`; M: wandfail=7, SoundEvent=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/lib/TCSounds.java` | C: `TCSounds`; M: ALEMBICKNOCK=0, ALL=1, BRAIN=0, BUBBLE=0, CAMERACLACK=0, CAMERATICKS=0, CHANT=0, COINS=0, CRABCLAW=0, CRABDEATH=0, CRABTALK=0, CRAFTFAIL=0, CRAFTSTART=0, CREAK=0, CRYSTAL=47, DOORFAIL=0, EGATTACK=0, EGDEATH=0, EGIDLE=0, EGSCREECH=0, ERASE=0, EVILPORTAL=0, FIRELOOP=0, FLY=0, GOLEMIRONSHOOT=0, GORE=0, HEARTBEAT=0, HHOFF=0, HHON=0, ICE=0, INFUSER=0, INFUSERSTART=0, JACOBS=0, JAR=0, KEY=0, LEARN=0, MONOLITH=0, PAGE=0, PECH_CHARGE=0, PECH_DEATH=0, PECH_DICE=0, PECH_HIT=0, PECH_IDLE=0, PECH_TRADE=0, PUMP=0, ROOTS=0, RUMBLE=0, RUNICSHIELDCHARGE=0, RUNICSHIELDEFFECT=0, SHOCK=0, SPILL=0, SQUEEK=0, SWARM=0, SWARMATTACK=0, SWING=0, TENTACLE=0, TOOL=66, UPGRADE=0, URNBREAK=0, WAND=0, WANDFAIL=0, WHISPERS=0, WIND=0, WISPDEAD=0, WISPLIVE=0, WRITE=0, ZAP=0, registerSounds=0, sound=2, runicShieldEffect=2, runicShieldCharge=2 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/lib/events/LootHandler.java` | C: `LootHandler`; M: NO_CONDITIONS=0, addLoot=0, onLootTableLoad=0, LootTableLoadEvent=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/lib/network/PacketBase.java` | C: `PacketBase`; M: fromBytes=38, getServerPlayer=0, onMessage=38, scheduleServer=0, toBytes=38 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/lib/utils/ConnectedTextureUtils.java` | C: `ConnectedTextureUtils`; M: CONNECTED_TEXTURE_REF_BY_ID=0, getTextureIndex=0 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/lib/world/WorldGenMoundTemplate.java` | C: `WorldGenMoundTemplate`; M: (none) | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/world/aura/AuraChunk.java` | C: `AuraChunk`; M: base=83, flux=1, getBase=0, getFlux=0, getVis=22, setBase=0, setFlux=0, setVis=0, vis=41 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/common/world/aura/AuraHandler.java` | C: `AuraHandler`; M: addFlux=0, drainVis=11, getAuraChunk=0, VisNet=0, VisNetHandler=54 | 0 | reproduced — class identifier absent; member collisions do not establish equivalence |
| `thaumcraft/truetyper/FontHelper.java` | C: `FontHelper`; M: drawString=17, formatEscape=3, set2DMode=2, set3DMode=2 | 3 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/truetyper/FontLoader.java` | C: `FontLoader`; M: createFont=4, loadSystemFont=4 | 5 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/truetyper/Formatter.java` | C: `Formatter`; M: getFormatted=2 | 3 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `thaumcraft/truetyper/TrueTypeFont.java` | C: `TrueTypeFont`; M: ALIGN_CENTER=1, ALIGN_LEFT=1, ALIGN_RIGHT=1, antiAlias=8, charArray=6, correctL=6, correctR=4, createSet=2, customChars=6, destroy=4, drawQuad=2, drawString=17, font=38, fontHeight=11, fontMetrics=6, fontSize=3, fontTextureID=4, getFontImage=2, getFonts=2, getHeight=8, getLineHeight=2, getWidth=7, height=120, intToByteArray=2, isSupported=1, loadImage=2, setCorrection=1, storedX=3, storedY=3, textureHeight=5, textureWidth=7, width=89 | 20 | NOT reproduced — relocated/name counterpart; original has a separate canonical row |
| `truetyper/FontHelper.java` | C: `FontHelper`; M: drawString=17, formatEscape=3, set2DMode=2, set3DMode=2 | 3 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `truetyper/FontLoader.java` | C: `FontLoader`; M: createFont=4, loadSystemFont=4, TrueTypeFont=20, TrueType=0 | 5 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `truetyper/Formatter.java` | C: `Formatter`; M: getFormatted=2 | 3 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |
| `truetyper/TrueTypeFont.java` | C: `TrueTypeFont`; M: ALIGN_CENTER=1, ALIGN_LEFT=1, ALIGN_RIGHT=1, antiAlias=8, charArray=6, correctL=6, correctR=4, createSet=2, customChars=6, destroy=4, drawQuad=2, drawString=17, font=38, fontHeight=11, fontMetrics=6, fontSize=3, fontTextureID=4, getFontImage=2, getFonts=2, getHeight=8, getLineHeight=2, getWidth=7, height=120, intToByteArray=2, isSupported=1, loadImage=2, setCorrection=1, storedX=3, storedY=3, textureHeight=5, textureWidth=7, width=89, TrueType=0 | 20 | NOT reproduced — original present; dead-code/platform/replacement rationale is distinct from absence |

### Targeted exclusion follow-through

- ModelBoreEmit: oracle `thaumcraft/client/renderers/models/ModelBoreEmit.java:13-64`, instantiated/rendered at oracle `thaumcraft/client/renderers/tile/TileArcaneBoreRenderer.java:26,52`. Native `common/src/main/java/thaumcraft/client/renderers/models/ModelArcaneBore.java:27-31,53-58` retains all five boxes and focus-gates the knob; native `common/src/main/java/thaumcraft/client/renderers/tile/TileArcaneBoreRenderer.java:61` calls it. Reclassify exclude → replace/adapt; no missing geometry demonstrated.
- Relocations are not absence: the 15 `ItemBlocks` class identifiers hit their original `thaumcraft/common/blocks/` implementations; KeyHandler hits `common/lib/events`; ItemNodeRenderer hits `client/renderers/tile`; ItemCrystalEssence hits `common/items`; CommandThaumcraft hits `common/lib/events`; all four `thaumcraft/truetyper` names hit root `truetyper`. These counterpart rows exist in the ledger. Mirror, item-node and font notes that said the class itself was not original were corrected.
- FXBubbleAlt, ModelVoidRobe and SlotLimitedByItemstack: class-name search has zero external references, reproducing the narrow source-level dead-class claim. This is not a class-absence result.
- FXScorch: `shootFire` occurs only in oracle `thaumcraft/client/lib/UtilsFX.java:130` (declaration); no caller. FXSmokeTrail: `soulTrail` occurs only in oracle ClientProxy:883 and CommonProxy:234 declarations. FXBeam: `.beam(` has no oracle call; `PacketFXBeamPulse` has only its own implementation plus PacketHandler import/registration, no construction/send. These source-level unreachable claims reproduce, notwithstanding drifted historical line citations.
- Bundled CodeChicken excludes: every class-name hit was filtered outside `thaumcraft/codechicken/`; none remain. Member searches were still performed and are recorded, but homonymous methods such as copy/apply/load cannot establish receiver-specific invocation counts. I did NOT rerun a jar-wide invoke-site analysis or certify the full transitive graph.
- Root truetyper: external references to FontLoader/TrueTypeFont were opened at oracle RenderEventHandler:108-112,140; it loads the Arial field. FontHelper has zero external class-name references; Formatter is referenced from FontHelper. This corroborates the described root structure, not the relocated rows’ old assertion that no such original classes exist.

## 1.4 — evidence sample and every failed entry

`LEDGER_SAMPLE.json` preserves the baseline claim, citation and verdict for all 220 entries. Six evenly spaced eligible units per family were selected, or all available units for smaller families. One original and one native evidence entry per unit were selected. Every cited explicit range was loaded in full by the audit script; excerpts and follow-up source context were inspected. This was a citation spot-check, **not** an independent proof of every numeric subclaim in long notes, nor a bytecode or runtime comparison. Abbreviated secondary `:line` references in long strings were not exhaustively parsed.

| Family | Entries |
|---|---:|
| advanced_devices_and_visnet | 12 |
| alchemy_and_essentia | 12 |
| aspect_contracts_and_data | 8 |
| base_content_and_small_interactions | 12 |
| client_rendering_fx_input_and_gui | 12 |
| creatures_and_projectiles | 2 |
| device_and_menu_routing_review | 12 |
| eldritch_and_outer_lands | 12 |
| embedded_support_and_dependencies | 12 |
| essentia_transport | 12 |
| foci_equipment_and_enchantments | 12 |
| golems_and_tasks | 12 |
| infusion | 12 |
| natural_resources_and_worldgen | 4 |
| public_api_and_compatibility | 12 |
| recipes_and_arcane_crafting | 12 |
| research_scanning_and_knowledge | 12 |
| shared_infrastructure | 12 |
| wands_nodes_and_vis | 12 |
| warp_creatures_and_ecology | 12 |
| world_structures_and_ecology | 2 |

Severity: **medium** = broken supporting evidence; **high** = obsolete/fabricated behavioral or liveness assertion. These severities concern ledger trust, not an exploit rating. The companion oracle/native entries are given for each failure, satisfying the two-sided evidence requirement.

### E002 — thaumcraft/client/fx/particles/FXBoreSparkle.java (medium)

- Baseline evidence[6]: native common/src/main/java/thaumcraft/common/tiles/TileArcaneBore.java:673 the native client tick calls the dig effect when the dig target of the server arrives with the block entity update
- Actual `native common/src/main/java/thaumcraft/common/tiles/TileArcaneBore.java:673`: 673: soundDelay = System.currentTimeMillis() + 1200L + level.random.nextInt(100);
- Companion checked: oracle thaumcraft/client/ClientProxy.java:866 boreDigFx spawns FXBoreSparkle in one of ten digs
- Correction: native common/src/main/java/thaumcraft/common/tiles/TileArcaneBore.java:678-689 the native client tick calls the dig effect when the dig target of the server arrives with the block entity update

### E003 — thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java (medium)

- Baseline evidence[0]: oracle thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java:25 the 1.7 renderer draws the retained thaumatorium.obj model
- Actual `oracle thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java:25`: 25: import net.minecraft.client.renderer.entity.RenderItem;
- Companion checked: native common/src/main/java/thaumcraft/client/renderers/models/ModelThaumatorium.java:23 the native OBJ loader reads the retained asset and is never referenced
- Correction: oracle thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java:45,72 the 1.7 renderer draws the retained thaumatorium.obj model

### E004 — thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java (high)

- Baseline evidence[3]: native common/src/main/java/thaumcraft/client/renderers/models/ModelThaumatorium.java:23 the native OBJ loader reads the retained asset and is never referenced
- Actual `native common/src/main/java/thaumcraft/client/renderers/models/ModelThaumatorium.java:23`: 23: public final class ModelThaumatorium {
- Companion checked: oracle thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java:25 the 1.7 renderer draws the retained thaumatorium.obj model
- Correction: native common/src/main/java/thaumcraft/client/renderers/models/ModelThaumatorium.java:26-35 loads the retained OBJ; common/src/main/java/thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java:18 constructs it, so the old unused-loader assertion was false

### E005 — thaumcraft/common/container/ContainerThaumatorium.java (medium)

- Baseline evidence[0]: oracle thaumcraft/common/container/ContainerThaumatorium.java:96 the quick move of the catalyst slot
- Actual `oracle thaumcraft/common/container/ContainerThaumatorium.java:96`: 96: }
- Companion checked: native common/src/main/java/thaumcraft/common/lib/crafting/DeviceContainerRules.java:199 thaumatoriumQuickMove(...) keeps the original range
- Correction: oracle thaumcraft/common/container/ContainerThaumatorium.java:108-129 the quick move of the catalyst slot

### E006 — thaumcraft/common/container/ContainerThaumatorium.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/lib/crafting/DeviceContainerRules.java:199 thaumatoriumQuickMove(...) keeps the original range
- Actual `native common/src/main/java/thaumcraft/common/lib/crafting/DeviceContainerRules.java:199`: 199: 
- Companion checked: oracle thaumcraft/common/container/ContainerThaumatorium.java:96 the quick move of the catalyst slot
- Correction: native common/src/main/java/thaumcraft/common/lib/crafting/DeviceContainerRules.java:283 thaumatoriumQuickMove(...) keeps the original range

### E007 — thaumcraft/common/tiles/TileArcaneBoreBase.java (medium)

- Baseline evidence[0]: oracle thaumcraft/common/tiles/TileArcaneBoreBase.java:1 the 1.7 stockless Perditio suction endpoint of the bore base
- Actual `oracle thaumcraft/common/tiles/TileArcaneBoreBase.java:1`: 1: /*
- Companion checked: native common/src/main/java/thaumcraft/common/tiles/TileArcaneBoreBase.java:11 the native base is a stockless suction endpoint
- Correction: oracle thaumcraft/common/tiles/TileArcaneBoreBase.java:102-128 the 1.7 stockless Perditio suction endpoint of the bore base

### E008 — thaumcraft/common/tiles/TileArcaneBoreBase.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/tiles/TileArcaneBoreBase.java:11 the native base is a stockless suction endpoint
- Actual `native common/src/main/java/thaumcraft/common/tiles/TileArcaneBoreBase.java:11`: 11: import net.minecraft.server.level.ServerLevel;
- Companion checked: oracle thaumcraft/common/tiles/TileArcaneBoreBase.java:1 the 1.7 stockless Perditio suction endpoint of the bore base
- Correction: native common/src/main/java/thaumcraft/common/tiles/TileArcaneBoreBase.java:84-89 the native base is a stockless suction endpoint

### E010 — thaumcraft/common/tiles/TileMagicBox.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:669 magicBoxSize(...) keeps the 27 per box rule
- Actual `native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:669`: 669: /** TileEldritchObelisk:70 - the obelisk affects living entities within six blocks. */
- Companion checked: oracle thaumcraft/common/tiles/TileMagicBox.java:45 the size of the box is 27 * (linkedBoxes + 1)
- Correction: native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:833-835 magicBoxSize(...) keeps the 27 per box rule

### E011 — thaumcraft/common/tiles/TileWarded.java (medium)

- Baseline evidence[0]: oracle thaumcraft/common/tiles/TileWarded.java:1 the 1.7 warded tile that keeps the remembered block and owner
- Actual `oracle thaumcraft/common/tiles/TileWarded.java:1`: 1: /*
- Companion checked: native common/src/main/java/thaumcraft/common/tiles/TileWarded.java:1 the native tile keeps the block state and the owner
- Correction: oracle thaumcraft/common/tiles/TileWarded.java:18-20 the 1.7 warded tile that keeps the remembered block and owner

### E012 — thaumcraft/common/tiles/TileWarded.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/tiles/TileWarded.java:1 the native tile keeps the block state and the owner
- Actual `native common/src/main/java/thaumcraft/common/tiles/TileWarded.java:1`: 1: package thaumcraft.common.tiles;
- Companion checked: oracle thaumcraft/common/tiles/TileWarded.java:1 the 1.7 warded tile that keeps the remembered block and owner
- Correction: native common/src/main/java/thaumcraft/common/tiles/TileWarded.java:17-24 the native tile keeps the block state and the owner

### E013 — thaumcraft/api/crafting/CrucibleRecipe.java (medium)

- Baseline evidence[0]: oracle thaumcraft/api/crafting/CrucibleRecipe.java:31-47 matches(): catalyst test first, then per-aspect cost >= recipe cost
- Actual `oracle thaumcraft/api/crafting/CrucibleRecipe.java:31-47`: 31: }; 32: String hc = researchKey + result.toString();
- Companion checked: native common/src/main/java/thaumcraft/common/crafting/CrucibleRecipe.java:48-59 matches()/removeMatching() implement the same two rules
- Correction: oracle thaumcraft/api/crafting/CrucibleRecipe.java:46-61 matches(): catalyst test first, then per-aspect cost >= recipe cost

### E018 — thaumcraft/common/blocks/ItemJarNode.java (high)

- Baseline evidence[4]: native common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java:525-538 the drop of the node jar carries the block entity data of the tile
- Actual `native common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java:525-538`: 525: * but its own update does nothing, so only the brain jar needs a ticker here.; 526: */
- Companion checked: oracle thaumcraft/common/blocks/BlockJar.java:184-193 the drop of the node jar carries them
- Correction: native common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java:611-621 the node-jar drop copies aspects, type, modifier and id through ItemJarNode, not a raw block-entity-data copy

### E021 — thaumcraft/common/tiles/TileJarBrain.java (medium)

- Baseline evidence[0]: oracle thaumcraft/common/tiles/TileJarBrain.java:13-14 xp and xpMax 2000
- Actual `oracle thaumcraft/common/tiles/TileJarBrain.java:13-14`: 13: import net.minecraft.entity.Entity;; 14: import net.minecraft.entity.item.EntityXPOrb;
- Companion checked: native common/src/main/java/thaumcraft/common/tiles/TileJarBrain.java:38-66 keeps the radius 7, the pull factors and the 2000 cap
- Correction: oracle thaumcraft/common/tiles/TileJarBrain.java:27-28 xp and xpMax 2000

### E022 — thaumcraft/common/tiles/TileJarBrain.java (medium)

- Baseline evidence[2]: native common/src/main/java/thaumcraft/common/tiles/TileJarBrain.java:38-66 keeps the radius 7, the pull factors and the 2000 cap
- Actual `native common/src/main/java/thaumcraft/common/tiles/TileJarBrain.java:38-66`: 38: public float headRoll;; 39: public float lookYaw;
- Companion checked: oracle thaumcraft/common/tiles/TileJarBrain.java:13-14 xp and xpMax 2000
- Correction: native common/src/main/java/thaumcraft/common/tiles/TileJarBrain.java:30-32,60-76 keeps the radius 7, the pull factors and the 2000 cap

### E023 — thaumcraft/common/tiles/TileJarNode.java (medium)

- Baseline evidence[0]: oracle thaumcraft/common/tiles/TileJarNode.java:99-144 the node jar keeps the aspects, the base aspects, the node type, the modifier and the node id
- Actual `oracle thaumcraft/common/tiles/TileJarNode.java:99-144`: 99: public AspectList getAspects() {; 100: return this.aspects;
- Companion checked: native common/src/main/java/thaumcraft/common/tiles/TileJarNode.java:110-133 stores the same five pieces of data
- Correction: oracle thaumcraft/common/tiles/TileJarNode.java:39-43,53-95 the node jar keeps the aspects, the base aspects, the node type, the modifier and the node id

### E024 — thaumcraft/common/tiles/TileJarNode.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/tiles/TileJarNode.java:110-133 stores the same five pieces of data
- Actual `native common/src/main/java/thaumcraft/common/tiles/TileJarNode.java:110-133`: 110: if (accepted <= 0) return amount;; 111: aspects.add(aspect, accepted);
- Companion checked: oracle thaumcraft/common/tiles/TileJarNode.java:99-144 the node jar keeps the aspects, the base aspects, the node type, the modifier and the node id
- Correction: native common/src/main/java/thaumcraft/common/tiles/TileJarNode.java:205-262 stores the same five pieces of data

### E026 — thaumcraft/api/aspects/AspectSourceHelper.java (medium)

- Baseline evidence[4]: native common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java:19 drainEssentia(BlockEntity, Aspect, Direction, int)
- Actual `native common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java:19`: 19: * a direct call, because the native handler owns the search itself. The unknown direction of 1.7.10 has no
- Companion checked: oracle thaumcraft/api/aspects/AspectSourceHelper.java:21 drainEssentia(TileEntity, Aspect, ForgeDirection, int)
- Correction: native common/src/main/java/thaumcraft/api/aspects/AspectSourceHelper.java:34 drainEssentia(BlockEntity, Aspect, Direction, int)

### E034 — thaumcraft/common/blocks/BlockAiry.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:405-416 AIRY_GROUP_HARDNESS {0,2,100,-1} and AIRY_GROUP_RESISTANCE {0,200,50,MAX}
- Actual `native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:405-416`: 405: public static final int AIRY_LEAVES = 2;; 406: public static final int AIRY_LEAVES_ALT = 3;
- Companion checked: oracle thaumcraft/common/blocks/BlockAiry.java:115-155 the per metadata hardness (2, 100, -1), explosion resistance (200, 50, MAX_VALUE) and light (15, 0, 8)
- Correction: native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:490-491 AIRY_GROUP_HARDNESS {0,2,100,-1} and AIRY_GROUP_RESISTANCE {0,200,50,MAX}

### E037 — thaumcraft/common/blocks/BlockFluxGasItem.java (medium)

- Baseline evidence[2]: oracle thaumcraft/common/config/ConfigBlocks.java:296 the flux gas is registered with the item class BlockFluxGasItem
- Actual `oracle thaumcraft/common/config/ConfigBlocks.java:296`: 296: blockStairsSilverwood = new BlockCosmeticStairs(blockWoodenDevice, 7).func_149663_c("blockStairsSilverwood");
- Companion checked: native common/src/main/java/thaumcraft/common/config/ThaumcraftFlux.java:30 the native gas block
- Correction: oracle thaumcraft/common/config/ConfigBlocks.java:308 the flux gas is registered with the item class BlockFluxGasItem

### E038 — thaumcraft/common/blocks/BlockFluxGasItem.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/config/ThaumcraftFlux.java:30 the native gas block
- Actual `native common/src/main/java/thaumcraft/common/config/ThaumcraftFlux.java:30`: 30: 
- Companion checked: oracle thaumcraft/common/config/ConfigBlocks.java:296 the flux gas is registered with the item class BlockFluxGasItem
- Correction: native common/src/main/java/thaumcraft/common/config/ThaumcraftFlux.java:43,52-53 the native gas block

### E040 — thaumcraft/common/blocks/BlockMetalDevice.java (medium)

- Baseline evidence[2]: native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:572 the hardness 3.0
- Actual `native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:572`: 572: public static int stoneDeviceKind(int variant) {
- Companion checked: oracle thaumcraft/common/blocks/BlockMetalDevice.java:101 setHardness(3.0f)
- Correction: native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:588 the hardness 3.0

### E041 — thaumcraft/common/items/ItemKey.java (medium)

- Baseline evidence[0]: oracle thaumcraft/common/items/ItemKey.java:62-72 the iron and the gold key with the metadata suffix name and the uncommon rarity
- Actual `oracle thaumcraft/common/items/ItemKey.java:62-72`: 62: public ItemKey() {; 63: this.func_77625_d(64);
- Companion checked: native common/src/main/java/thaumcraft/common/items/ItemKey.java:30-95 keeps the two key types, the KEY_LINK component and the access grant
- Correction: oracle thaumcraft/common/items/ItemKey.java:70-72,86-95 the iron and the gold key with the metadata suffix name and the uncommon rarity

### E044 — thaumcraft/common/items/relics/ItemSanityChecker.java (high)

- Baseline evidence[6]: native common/src/main/java/thaumcraft/common/items/relics/ItemSanityChecker.java:21-40 the native relic and the reading of the warp
- Actual `native common/src/main/java/thaumcraft/common/items/relics/ItemSanityChecker.java:21-40`: 25: <PAST EOF>
- Companion checked: oracle thaumcraft/common/items/relics/ItemSanityChecker.java:16-40 the stack limit 1 and the uncommon rarity
- Correction: native common/src/main/java/thaumcraft/common/items/relics/ItemSanityChecker.java:19-24 the inert relic with stack limit one and uncommon rarity; no warp-reading behavior

### E048 — thaumcraft/client/fx/particles/FXBurst.java (medium)

- Baseline evidence[2]: native common/src/main/java/thaumcraft/client/fx/particles/MonsterSpriteParticle.java:39-40 'Exact FXBurst constructor entry, shared by the original Wisp/ShockOrb clients.'
- Actual `native common/src/main/java/thaumcraft/client/fx/particles/MonsterSpriteParticle.java:39-40`: 39: }; 40: public static void setLitShader(ShaderInstance shader) { litShader=java.util.Objects.requireNonNull(shader); }
- Companion checked: oracle thaumcraft/client/fx/particles/FXBurst.java:32-34,44-45 the size scaling, the 31 tick lifetime, the node strip texture and glColor4f alpha 0.75
- Correction: native common/src/main/java/thaumcraft/client/fx/particles/MonsterSpriteParticle.java:46-47 'Exact FXBurst constructor entry, shared by the original Wisp/ShockOrb clients.'

### E049 — thaumcraft/client/lib/PlayerNotifications.java (medium)

- Baseline evidence[0]: oracle thaumcraft/client/lib/PlayerNotifications.java:24-31 addNotification stores (text,image,expire=now+delay+bonus,created=now+delay/4,color)
- Actual `oracle thaumcraft/client/lib/PlayerNotifications.java:24-31`: 24: public static void addAspectNotification(Aspect aspect) {; 25: long time = System.nanoTime() / 1000000L + (long)Minecraft.func_71410_x().field_71441_e.field_73012_v.nextInt(1000);
- Companion checked: native common/src/main/java/thaumcraft/client/lib/PlayerNotifications.java:20-24 the same delay/2 first-entry bonus and the same queue
- Correction: oracle thaumcraft/client/lib/PlayerNotifications.java:39-42 addNotification stores (text,image,expire=now+delay+bonus,created=now+delay/4,color)

### E050 — thaumcraft/client/lib/PlayerNotifications.java (medium)

- Baseline evidence[3]: native common/src/main/java/thaumcraft/client/lib/PlayerNotifications.java:20-24 the same delay/2 first-entry bonus and the same queue
- Actual `native common/src/main/java/thaumcraft/client/lib/PlayerNotifications.java:20-24`: 20: private static final ResourceLocation PARTICLES = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");; 21: private static final SoundEvent WHISPERS = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "whispers"));
- Companion checked: oracle thaumcraft/client/lib/PlayerNotifications.java:24-31 addNotification stores (text,image,expire=now+delay+bonus,created=now+delay/4,color)
- Correction: native common/src/main/java/thaumcraft/client/lib/NotificationQueue.java:17-19 the delay/2 first-entry bonus and notification timestamps

### E056 — thaumcraft/client/renderers/tile/TileVisRelayRenderer.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/client/renderers/tile/TileVisRelayRenderer.java:39 the beam, and common/src/main/java/thaumcraft/client/renderers/models/ModelVisRelay.java the bounded OBJ adapter
- Actual `native common/src/main/java/thaumcraft/client/renderers/tile/TileVisRelayRenderer.java:39`: 39: // Original FXBeamPower:154-158 and :187-191 - the beam is ten times fainter than the flare
- Companion checked: oracle thaumcraft/client/renderers/tile/TileVisRelayRenderer.java:1-98 the relay with the RingBase/RingFloat/Crystal OBJ parts and the power beam
- Correction: native common/src/main/java/thaumcraft/client/renderers/tile/TileVisRelayRenderer.java:42 the beam, and common/src/main/java/thaumcraft/client/renderers/models/ModelVisRelay.java the bounded OBJ adapter

### E059 — thaumcraft/common/container/ContainerDeconstructionTable.java (medium)

- Baseline evidence[0]: oracle thaumcraft/common/container/ContainerDeconstructionTable.java:82 the quick move ranges of the table
- Actual `oracle thaumcraft/common/container/ContainerDeconstructionTable.java:82`: 82: }
- Companion checked: native common/src/main/java/thaumcraft/common/lib/crafting/DeviceContainerRules.java:177 deconstructionQuickMove(...) keeps the original ranges
- Correction: oracle thaumcraft/common/container/ContainerDeconstructionTable.java:95-107 the quick move ranges of the table

### E060 — thaumcraft/common/container/ContainerDeconstructionTable.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/lib/crafting/DeviceContainerRules.java:177 deconstructionQuickMove(...) keeps the original ranges
- Actual `native common/src/main/java/thaumcraft/common/lib/crafting/DeviceContainerRules.java:177`: 177: /** Oracle ContainerGhostSlots:73 - an empty cursor adds or removes one. */
- Companion checked: oracle thaumcraft/common/container/ContainerDeconstructionTable.java:82 the quick move ranges of the table
- Correction: native common/src/main/java/thaumcraft/common/lib/crafting/DeviceContainerRules.java:261-265 deconstructionQuickMove(...) keeps the original ranges

### E062 — thaumcraft/common/container/SlotGhostFluid.java (high)

- Baseline evidence[5]: native common/src/main/java/thaumcraft/common/container/SlotGhostFluid.java:33-40 the slot accepts every native fluid container, a bucket of the vanilla or the item of an add-on that carries the bucket entity data, and never gives the container back
- Actual `native common/src/main/java/thaumcraft/common/container/SlotGhostFluid.java:33-40`: 40: <PAST EOF>
- Companion checked: oracle thaumcraft/common/container/SlotGhostFluid.java:30 isItemValid asks the Forge FluidContainerRegistry
- Correction: native common/src/main/java/thaumcraft/common/container/SlotGhostFluid.java:34-38 accepts BucketItem or BUCKET_ENTITY_DATA; this predicate alone does not establish compatibility with every fluid-container API

### E063 — thaumcraft/common/tiles/TileBrainbox.java (medium)

- Baseline evidence[0]: oracle thaumcraft/common/tiles/TileBrainbox.java:1 the 1.7 brainbox that points at the machine half it serves
- Actual `oracle thaumcraft/common/tiles/TileBrainbox.java:1`: 1: /*
- Companion checked: native common/src/main/java/thaumcraft/common/tiles/TileBrainbox.java:1 the native brainbox keeps the face of the machine half
- Correction: oracle thaumcraft/common/tiles/TileBrainbox.java:16,20,25 the 1.7 brainbox that points at the machine half it serves

### E064 — thaumcraft/common/tiles/TileBrainbox.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/tiles/TileBrainbox.java:1 the native brainbox keeps the face of the machine half
- Actual `native common/src/main/java/thaumcraft/common/tiles/TileBrainbox.java:1`: 1: package thaumcraft.common.tiles;
- Companion checked: oracle thaumcraft/common/tiles/TileBrainbox.java:1 the 1.7 brainbox that points at the machine half it serves
- Correction: native common/src/main/java/thaumcraft/common/tiles/TileBrainbox.java:18-27 the native brainbox keeps the face of the machine half

### E065 — thaumcraft/common/tiles/TileHole.java (medium)

- Baseline evidence[0]: oracle thaumcraft/common/tiles/TileHole.java:1 the 1.7 hole that replaces a block and restores it later
- Actual `oracle thaumcraft/common/tiles/TileHole.java:1`: 1: /*
- Companion checked: native common/src/main/java/thaumcraft/common/tiles/TileHole.java:1 the native hole keeps the replaced block, the lifetime and the chain
- Correction: oracle thaumcraft/common/tiles/TileHole.java:115-123 the 1.7 hole that replaces a block and restores it later

### E066 — thaumcraft/common/tiles/TileHole.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/tiles/TileHole.java:1 the native hole keeps the replaced block, the lifetime and the chain
- Actual `native common/src/main/java/thaumcraft/common/tiles/TileHole.java:1`: 1: package thaumcraft.common.tiles;
- Companion checked: oracle thaumcraft/common/tiles/TileHole.java:1 the 1.7 hole that replaces a block and restores it later
- Correction: native common/src/main/java/thaumcraft/common/tiles/TileHole.java:27-43,61-78 the native hole keeps the replaced block, the lifetime and the chain

### E067 — thaumcraft/common/tiles/TileNitor.java (high)

- Baseline evidence[0]: oracle thaumcraft/common/tiles/TileNitor.java:18 the 1.7 nitor draws fire essentia from the vis network
- Actual `oracle thaumcraft/common/tiles/TileNitor.java:18`: 18: public void func_145845_h() {
- Companion checked: native common/src/main/java/thaumcraft/common/tiles/TileNitor.java:1 the native nitor exists as a source
- Correction: oracle thaumcraft/common/tiles/TileNitor.java:18-27 only emits client particles; it neither produces nor drains vis or essentia

### E068 — thaumcraft/common/tiles/TileNitor.java (high)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/tiles/TileNitor.java:1 the native nitor exists as a source
- Actual `native common/src/main/java/thaumcraft/common/tiles/TileNitor.java:1`: 1: package thaumcraft.common.tiles;
- Companion checked: oracle thaumcraft/common/tiles/TileNitor.java:18 the 1.7 nitor draws fire essentia from the vis network
- Correction: native common/src/main/java/thaumcraft/common/tiles/TileNitor.java:17-21 delegates client particles and has no vis/essentia production or drain

### E069 — thaumcraft/common/tiles/TileWardingStoneFence.java (high)

- Baseline evidence[0]: oracle thaumcraft/common/tiles/TileWardingStoneFence.java:18 the fence needs a warding stone below it
- Actual `oracle thaumcraft/common/tiles/TileWardingStoneFence.java:18`: 18: }
- Companion checked: native common/src/main/java/thaumcraft/common/tiles/TileWardingStoneFence.java:1 the native fence keeps that rule
- Correction: oracle thaumcraft/common/tiles/TileWardingStoneFence.java:20-26 the fence survives if a warding stone is one or two blocks below it

### E070 — thaumcraft/common/tiles/TileWardingStoneFence.java (high)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/tiles/TileWardingStoneFence.java:1 the native fence keeps that rule
- Actual `native common/src/main/java/thaumcraft/common/tiles/TileWardingStoneFence.java:1`: 1: package thaumcraft.common.tiles;
- Companion checked: oracle thaumcraft/common/tiles/TileWardingStoneFence.java:18 the fence needs a warding stone below it
- Correction: native common/src/main/java/thaumcraft/common/tiles/TileWardingStoneFence.java:25-32 checks for a warding stone one and two blocks below

### E071 — thaumcraft/api/entities/IEldritchMob.java (medium)

- Baseline evidence[0]: oracle thaumcraft/api/entities/IEldritchMob.java:8-9 empty marker interface
- Actual `oracle thaumcraft/api/entities/IEldritchMob.java:8-9`: 9: <PAST EOF>
- Companion checked: native common/src/main/java/thaumcraft/api/entities/IEldritchMob.java:3 empty marker interface
- Correction: oracle thaumcraft/api/entities/IEldritchMob.java:6-7 empty marker interface

### E073 — thaumcraft/client/renderers/models/ModelEldritchCrystal.java (medium)

- Baseline evidence[2]: oracle thaumcraft/client/renderers/tile/TileEldritchCrystalRenderer.java:27 and :37 the crystal is the Crystal and the Base group of the wavefront file thaumcraft/textures/models/vcrystal.obj
- Actual `oracle thaumcraft/client/renderers/tile/TileEldritchCrystalRenderer.java:27`: 27: import net.minecraft.util.MathHelper;
- Companion checked: native common/src/main/java/thaumcraft/client/renderers/models/ModelEldritchCrystal.java:1-60 the native model of the eldritch crystal
- Correction: oracle thaumcraft/client/renderers/tile/TileEldritchCrystalRenderer.java:39,46,54 loads vcrystal.obj and renders the Base and Crystal groups

### E076 — thaumcraft/common/blocks/BlockEldritch.java (medium)

- Baseline evidence[5]: native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:43-100 the same light, hardness and resistance tables
- Actual `native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:43-100`: 43: * block. It is hosted by the eldritch family here, because the crystal; 44: * block itself is already ported with its own crystal clusters.
- Companion checked: oracle thaumcraft/common/blocks/BlockEldritch.java:120-135 the per metadata light 12/5/4/0/8
- Correction: native common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java:68-120 the same light, hardness and resistance tables

### E086 — thaumcraft/codechicken/lib/raytracer/IndexedCuboid6.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:12-19 the same six ARMS boxes and the CORE box
- Actual `native common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:12-19`: 12: import net.minecraft.world.phys.BlockHitResult;; 13: import net.minecraft.world.phys.HitResult;
- Companion checked: oracle thaumcraft/common/tiles/TileTube.java:372-393 six per-direction IndexedCuboid6 arms plus the 0.34375..0.65625 core
- Correction: native common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:23-32 the same six ARMS boxes and the CORE box

### E088 — thaumcraft/codechicken/lib/raytracer/RayTracer.java (medium)

- Baseline evidence[2]: native common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:38-58 trace()/retrace() reimplement the indexed cuboid ray trace with native AABB.clip
- Actual `native common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:38-58`: 38: /** Raw neighbor presence: closed arms AND a valve's excluded stem remain indexed. */; 39: public static boolean hasTransportNeighbor(BlockGetter level, BlockPos pos, Direction face) {
- Companion checked: oracle thaumcraft/common/tiles/TileTube.java:316 RayTracer.retraceBlock(world, player, x, y, z) and :372-393 addTraceableCuboids
- Correction: native common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:48-81 trace()/retrace() reimplement the indexed cuboid ray trace with native AABB.clip

### E092 — thaumcraft/codechicken/lib/vec/Cuboid6.java (medium)

- Baseline evidence[2]: native common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:12-19 the same boxes as native AABBs
- Actual `native common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:12-19`: 12: import net.minecraft.world.phys.BlockHitResult;; 13: import net.minecraft.world.phys.HitResult;
- Companion checked: oracle thaumcraft/common/tiles/TileTube.java:35 and :376-393 the Cuboid6 arm/core boxes
- Correction: native common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:23-32 the same boxes as native AABBs

### E094 — thaumcraft/codechicken/lib/vec/Vector3.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:59 the native Vec3 start/end
- Actual `native common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:59`: 59: }
- Companion checked: oracle thaumcraft/common/blocks/BlockTube.java:65,370 new Vector3(start)/new Vector3(end) are the ray trace endpoints
- Correction: native common/src/main/java/thaumcraft/common/lib/essentia/TubeGeometry.java:48 the native Vec3 start/end

### E100 — thaumcraft/common/blocks/BlockEssentiaReservoir.java (medium)

- Baseline evidence[4]: native common/src/main/java/thaumcraft/common/lib/essentia/ReservoirRules.java:23 public static int comparator(int units) {{ return (int) Math.floor(ratio * 14.0F) + (units > 0 ? 1 : 0); }}
- Actual `native common/src/main/java/thaumcraft/common/lib/essentia/ReservoirRules.java:23`: 23: public static int space(int units) { return CAPACITY - units; }
- Companion checked: oracle /home/Acak/work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/blocks/BlockEssentiaReservoir.java:40 hardness 2.0f, :41 resistance 17.0f, :72 light value 1
- Correction: native common/src/main/java/thaumcraft/common/lib/essentia/ReservoirRules.java:28-30 public static int comparator(int units) {{ return (int) Math.floor(ratio * 14.0F) + (units > 0 ? 1 : 0); }}

### E122 — thaumcraft/common/entities/ai/combat/AIDartAttack.java (medium)

- Baseline evidence[3]: native common/src/main/java/thaumcraft/items/golems/GolemDartRules.java:24-46 interval 30-8*air, firstDelay interval/2, MIN_DISTANCE_SQ 9.0, REACH_FACTOR 0.8
- Actual `native common/src/main/java/thaumcraft/items/golems/GolemDartRules.java:24-46`: 24: <MISSING FILE>
- Companion checked: oracle thaumcraft/common/entities/ai/combat/AIDartAttack.java:25-27 interval 30 - 8*Air, first delay half, distance 9.0
- Correction: native common/src/main/java/thaumcraft/common/items/golems/GolemDartRules.java:14-16,24-43 interval 30-8*air, firstDelay interval/2, MIN_DISTANCE_SQ 9.0, REACH_FACTOR 0.8

### E124 — thaumcraft/common/entities/ai/interact/AIFish.java (medium)

- Baseline evidence[3]: native common/src/main/java/thaumcraft/items/golems/GolemUpgrades.java:211-231 fishJunkBase/fishTreasureBase/fishCategory/fishCatches
- Actual `native common/src/main/java/thaumcraft/items/golems/GolemUpgrades.java:211-231`: 211: <MISSING FILE>
- Companion checked: oracle thaumcraft/common/entities/ai/interact/AIFish.java:71 work radius ceil(range/2), :118-120 one catch plus one for every Air upgrade
- Correction: native common/src/main/java/thaumcraft/common/items/golems/GolemUpgrades.java:211-231 fishJunkBase/fishTreasureBase/fishCategory/fishCatches

### E130 — thaumcraft/common/lib/network/fx/PacketFXBeamPulseGolemBoss.java (high)

- Baseline evidence[5]: native common/src/main/java/thaumcraft/common/entities/monster/EntityEldritchGolem.java:247-260 the native shot with the same numbers and the same order
- Actual `native common/src/main/java/thaumcraft/common/entities/monster/EntityEldritchGolem.java:247-260`: 247: @Override public boolean doHurtTarget(Entity target) {; 248: if(attackTimer>0)return false;
- Companion checked: oracle thaumcraft/common/lib/network/fx/PacketFXBeamPulseGolemBoss.java:36-88 the message carries two entity ids and draws the two client beams (0.07/0.376/0.325 and 1.0/0.5/0.5, age 20, blendmode 1)
- Correction: native common/src/main/java/thaumcraft/common/entities/monster/EntityEldritchGolem.java:263-274 the native ranged shot; the old citation pointed to melee rather than packet visual handling

### E134 — thaumcraft/api/crafting/InfusionRecipe.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:54 matches delegates to InfusionStackRecipe
- Actual `native common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:54`: 54: /**
- Companion checked: oracle thaumcraft/api/crafting/InfusionRecipe.java:37 matches(ArrayList, ItemStack, World, EntityPlayer)
- Correction: native common/src/main/java/thaumcraft/api/crafting/InfusionRecipe.java:73-77 matches delegates to InfusionStackRecipe

### E135 — thaumcraft/common/lib/network/fx/PacketFXInfusionSource.java (medium)

- Baseline evidence[0]: oracle thaumcraft/common/lib/network/fx/PacketFXInfusionSource.java:33 the matrix, the offset and the colour
- Actual `oracle thaumcraft/common/lib/network/fx/PacketFXInfusionSource.java:33`: 33: private byte dz;
- Companion checked: native common/src/main/java/thaumcraft/common/lib/network/InfusionFxPayload.java:11 the native payload (dimension, matrix, event, target, data)
- Correction: oracle thaumcraft/common/lib/network/fx/PacketFXInfusionSource.java:28-34 the matrix, the offset and the colour

### E141 — thaumcraft/common/tiles/TileWandPedestal.java (high)

- Baseline evidence[0]: oracle thaumcraft/common/tiles/TileWandPedestal.java:194-211 the draw every fifth tick and the one vis reserve
- Actual `oracle thaumcraft/common/tiles/TileWandPedestal.java:194-211`: 194: boolean hasThingy = false;; 195: if (this.field_145850_b.func_147439_a(this.field_145851_c, this.field_145848_d + 1, this.field_145849_e) == ConfigBlocks.blockStoneDevice && this.field_145850_b
- Companion checked: native common/src/main/java/thaumcraft/common/tiles/TileWandPedestal.java:94-97 and :103-127 the same draw
- Correction: oracle thaumcraft/common/tiles/TileWandPedestal.java:193-214 draws every fifth tick, with a one-vis reserve except for iron caps or wood rods

### E148 — thaumcraft/api/BlockCoordinates.java (high)

- Baseline evidence[6]: native common/src/main/java/thaumcraft/api/IArchitect.java:7,9 the native focus surface (max area size and axis modes) instead of a block list
- Actual `native common/src/main/java/thaumcraft/api/IArchitect.java:7,9`: 7: ; 9: * The Architect focus capability of the original TC4 API.
- Companion checked: oracle thaumcraft/api/BlockCoordinates.java:13 the mutable x/y/z holder with equals, hashCode and compareWorldCoordinate
- Correction: native common/src/main/java/thaumcraft/api/IArchitect.java:51,63,71,74 publishes the original block-list/axis methods and the native area-size/axis-mode helpers

### E154 — thaumcraft/api/ThaumcraftApiHelper.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:39 cullTags / :75 areItemsEqual / :81,:86 isResearchComplete / :91 hasDiscoveredAspect / :95 getDiscoveredAspects / :100 getStackInRowAndColumn / :104,:110 getObjectAspects / :115 getBonusObjectTags / :120 generateTags / :125 containsMatch / :136 areItemStackTagsEqualForCrafting / :148 itemMatches / :156,:160 getConnectableTile / :167 getAllAspects / :176 getAllCompoundAspects / :185,:189,:193 consumeVis* / :197,:201 addWarp* / :211 rayTraceIgnoringSource
- Actual `native common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:39`: 39: * Original cap of six aspects, dropping the least valuable ones first.
- Companion checked: oracle /home/Acak/work/thaumcraft-4.2.3.5-oracle/thaumcraft/api/ThaumcraftApiHelper.java:40 cullTags / :83 areItemsEqual / :90 isResearchComplete / :94 hasDiscoveredAspect / :98 getDiscoveredAspects / :102 getStackInRowAndColumn / :106 getObjectAspects / :110 getBonusObjectTags / :114 generateTags / :118 containsMatch / :128 areItemStackTagsEqualForCrafting / :148 itemMatches / :155,:163 getConnectableTile / :171 getAllAspects / :182 getAllCompoundAspects / :193,:197,:201 consumeVis* / :205,:209 addWarp* / :213 rayTraceIgnoringSource (24 methods)
- Correction: native common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java:49,99,106,111,116,120,133,137,143,148,153,158,169,181,189,193,200,209,218,222,226,230,234,244 the published helper declarations, from cullTags to rayTraceIgnoringSource

### E158 — thaumcraft/api/damagesource/DamageSourceThaumcraft.java (medium)

- Baseline evidence[3]: native common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:22-25 the four damage type keys
- Actual `native common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:22-25`: 22: * source of the original. The type ids keep the namespace of the mod, so they never collide with the; 23: * vanilla ones.</p>
- Companion checked: oracle thaumcraft/api/damagesource/DamageSourceThaumcraft.java:19 the taint source, bypasses armour and magic damage
- Correction: native common/src/main/java/thaumcraft/api/damagesource/DamageSourceThaumcraft.java:27-33 the four damage type keys

### E174 — thaumcraft/client/gui/GuiResearchBrowser.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/client/gui/GuiResearchBrowser.java:51 panelWidth = Math.min(510, width-16); panelHeight = Math.min(300, height-16)
- Actual `native common/src/main/java/thaumcraft/client/gui/GuiResearchBrowser.java:51`: 51: public GuiResearchBrowser() {
- Companion checked: oracle thaumcraft/client/gui/GuiResearchBrowser.java:75-76 protected int paneWidth = 256; protected int paneHeight = 230
- Correction: native common/src/main/java/thaumcraft/client/gui/GuiResearchBrowser.java:64 panelWidth = Math.min(510, width-16); panelHeight = Math.min(300, height-16)

### E178 — thaumcraft/common/items/relics/ItemThaumometer.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/items/relics/ItemThaumometer.java:31 getUseDuration returns 25
- Actual `native common/src/main/java/thaumcraft/common/items/relics/ItemThaumometer.java:31`: 31: import thaumcraft.common.lib.network.ScanRuneFxNetworking;
- Companion checked: oracle thaumcraft/common/items/relics/ItemThaumometer.java:79-81 getMaxItemUseDuration returns 25
- Correction: native common/src/main/java/thaumcraft/common/items/relics/ItemThaumometer.java:37,50 getUseDuration returns 25

### E180 — thaumcraft/common/lib/network/playerdata/PacketSyncScannedItems.java (medium)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/lib/research/PlayerKnowledge.java:167 writeSet(tag, "scannedItems", scannedItems)
- Actual `native common/src/main/java/thaumcraft/common/lib/research/PlayerKnowledge.java:167`: 167: aspect.putInt("amount", entry.getValue());
- Companion checked: oracle thaumcraft/common/lib/network/playerdata/PacketSyncScannedItems.java:37-39 the scanned object list of the player
- Correction: native common/src/main/java/thaumcraft/common/lib/research/PlayerKnowledge.java:172 writeSet(tag, "scannedItems", scannedItems)

### E190 — thaumcraft/common/lib/network/fx/PacketFXSonic.java (medium)

- Baseline evidence[5]: native common/src/main/java/thaumcraft/common/entities/monster/EntityEldritchWarden.java:496 the native sender
- Actual `native common/src/main/java/thaumcraft/common/entities/monster/EntityEldritchWarden.java:496`: 496: /** Original PacketFXSonic broadcast to every player within thirty two blocks. */
- Companion checked: oracle thaumcraft/common/lib/network/fx/PacketFXSonic.java:1-59 the sonic effect
- Correction: native common/src/main/java/thaumcraft/common/entities/monster/EntityEldritchWarden.java:497-502 the native sender

### E193 — thaumcraft/common/lib/utils/Utils.java (medium)

- Baseline evidence[2]: oracle thaumcraft/common/lib/utils/Utils.java:65-86 the original useBonemealAtLoc refuses a crop that cannot grow any further with its IGrowable.canGrow test
- Actual `oracle thaumcraft/common/lib/utils/Utils.java:65-86`: 65: import net.minecraft.world.chunk.Chunk;; 66: import net.minecraftforge.common.MinecraftForge;
- Companion checked: native common/src/main/java/thaumcraft/common/lib/utils/Utils.java:281-303 the native useBonemealAtLoc keeps the canGrow refusal
- Correction: oracle thaumcraft/common/lib/utils/Utils.java:89-104 the original useBonemealAtLoc refuses a crop that cannot grow any further with its IGrowable.canGrow test

### E196 — thaumcraft/api/nodes/INode.java (high)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/api/nodes/INode.java:13-29 the nine members of the contract
- Actual `native common/src/main/java/thaumcraft/api/nodes/INode.java:13-29`: 13: * <p><b>The identity of the node.</b> Oracle {@code thaumcraft/api/nodes/INode.java:12-29} publishes; 14: * {@link #getId()}, and the original node derived that identity from its own position
- Companion checked: oracle thaumcraft/api/nodes/INode.java:12-29 the nine members of the contract
- Correction: native common/src/main/java/thaumcraft/api/nodes/INode.java:30-59 the contract declarations; its UUID identity format is a deviation from oracle TileNode.java:98-115

### E203 — thaumcraft/client/renderers/tile/ItemNodeRenderer.java (high)

- Baseline evidence[0]: oracle /home/Acak/work/thaumcraft-4.2.3.5-oracle/thaumcraft/client/renderers/tile/ItemNodeRenderer.java:172 the item form of the node (the block that drops a node item)
- Actual `oracle thaumcraft/client/renderers/tile/ItemNodeRenderer.java:172`: 172: 
- Companion checked: native common/src/main/java/thaumcraft/client/renderers/tile/TileNodeRenderer.java:38,197-206 the shared node atlas and the quad of the node strips
- Correction: oracle thaumcraft/client/renderers/tile/ItemNodeRenderer.java:49-72 renders the aura-node item; this does not establish a gameplay node-item drop

### E206 — thaumcraft/common/world/aura/AuraHandler.java (high)

- Baseline evidence[2]: native common/src/main/java/thaumcraft/api/aura/AuraHelper.java:48-60 the native drainVis(level, pos, amount, simulate) over the essence of the node
- Actual `native common/src/main/java/thaumcraft/api/aura/AuraHelper.java:48-60`: 48: * {@code VisNetHandler.drainVis(world, x, y, z, requested, simulate)}, and that method walks the; 49: * primal aspects of every root source that is in range of the position
- Companion checked: oracle thaumcraft/api/visnet/VisNetHandler.java:29 the original publishes the aspect drain only; the aspect-less form of the 1.12 tree is a later addition
- Correction: native common/src/main/java/thaumcraft/api/aura/AuraHelper.java:57-60 drainVis delegates to VisNetHandler, selecting the vis network rather than only the node at that position

### E207 — thaumcraft/api/IWarpingGear.java (medium)

- Baseline evidence[0]: oracle thaumcraft/api/IWarpingGear.java:13 public int getWarp(ItemStack var1, EntityPlayer var2);
- Actual `oracle thaumcraft/api/IWarpingGear.java:13`: 13: public interface IWarpingGear {
- Companion checked: native common/src/main/java/thaumcraft/api/IWarpingGear.java:11 int getWarp(ItemStack stack, Player player);
- Correction: oracle thaumcraft/api/IWarpingGear.java:14 public int getWarp(ItemStack var1, EntityPlayer var2);

### E209 — thaumcraft/common/config/MaterialTaint.java (high)

- Baseline evidence[0]: oracle thaumcraft/common/config/MaterialTaint.java:9-42 the taint material is not solid, not replaceable, does not block grass and carries the mobility flag one
- Actual `oracle thaumcraft/common/config/MaterialTaint.java:9-42`: 9: ; 10: import net.minecraft.block.material.MapColor;
- Companion checked: native common/src/main/java/thaumcraft/common/config/ThaumcraftTaint.java:72 the taint family with the push reaction BLOCK, the counterpart of the mobility flag one
- Correction: oracle thaumcraft/common/config/MaterialTaint.java:23-36,43-49 the material boolean overrides and mobilityFlag = 1; the assignment is outside the old cited range

### E214 — thaumcraft/common/entities/monster/mods/ChampionModGrim.java (high)

- Baseline evidence[1]: native common/src/main/java/thaumcraft/common/lib/mods/ChampionModGrim.java:11 Status.WEAKNESS for 200 ticks
- Actual `native common/src/main/java/thaumcraft/common/lib/mods/ChampionModGrim.java:11`: 11: @Override public Result performEffect(Context context, Random random) {
- Companion checked: oracle thaumcraft/common/entities/monster/mods/ChampionModGrim.java:26-30 nextFloat() < 0.4 -> Potion.field_82731_v (wither) for 200 ticks
- Correction: native common/src/main/java/thaumcraft/common/lib/mods/ChampionModGrim.java:11-13 Status.WITHER for 200 ticks with probability 0.4; the old WEAKNESS assertion was obsolete

## Contradictory and obsolete notes corrected

The following are additional authored-note findings, not added to the 220 evidence-entry denominator. Later notes did not erase earlier false assertions. Source anchors are supplied in the replacement text or the corrected evidence of the same unit.

| Unit / note index | Withdrawn assertion | Correction |
|---|---|---|
| `thaumcraft/api/BlockCoordinates.java` notes[0] | Every live consumer of the original class is an Architect-area consumer, and all of them were re-architected onto net.minecraft.core.BlockPos plus the bounded WandArchitect.Area record, so the original mutable triple has no consumer left. | Historical removal claims withdrawn: common/src/main/java/thaumcraft/api/IArchitect.java:51 publishes BlockCoordinates lists and common/src/main/java/thaumcraft/client/ThaumcraftArchitectClient.java:86 consumes them. |
| `thaumcraft/api/BlockCoordinates.java` notes[1] | The readNBT/writeNBT helpers of the class were never called in the 4.2.3.5 tree, so dropping the class loses no behaviour. | The old claim that the whole class could be dropped confused unused NBT helpers with the published architect value type; the type is retained natively. |
| `thaumcraft/api/BlockCoordinates.java` notes[2] | Partial, not verified: the published add-on type is gone, so an add-on written against thaumcraft.api.BlockCoordinates does not compile against the native. This is the same public-API regression that the thaumcraft.api.IArchitect row records. | Historical missing-type gap superseded: common/src/main/java/thaumcraft/api/BlockCoordinates.java is present again, as recorded by the later closure note. |
| `thaumcraft/api/IArchitect.java` notes[2] | REMAINING GAP: the native architect is a server-authoritative re-implementation, so the native path does not consult an add-on override of the original contract. An add-on that implements the original IArchitect therefore still cannot drive the native architect; an add-on that only reads the area still works. | The old blanket claim that native code never consults an add-on override was false: common/src/main/java/thaumcraft/client/ThaumcraftArchitectClient.java:86 invokes IArchitect.getArchitectBlocks for the preview. Server-authoritative casting is separate; this observation alone does not certify arbitrary add-on cast behavior. |
| `thaumcraft/client/ThaumcraftGuiFactory.java` notes[1] | The configuration it exposed is still readable/editable through the native properties file (ThaumcraftSettings), but there is no in-game configuration screen. | Historical no-screen claim superseded: common/src/main/java/thaumcraft/client/ThaumcraftConfigClient.java:36 opens ThaumcraftConfigScreen. |
| `thaumcraft/client/fx/beams/FXArc.java` notes[0] | Deliberately not ported: there is no native equivalent of the 1.7 FXArc ribbon. | Historical no-ribbon claim withdrawn: common/src/main/java/thaumcraft/client/fx/particles/FXArc.java:86 creates the native FXArc. |
| `thaumcraft/client/fx/beams/FXArc.java` notes[1] | The only native arc (InfusionArcParticle) serves a different original call site (nodeBolt type 0), not arcLightning. | InfusionArcParticle serves a different effect; it is not the only arc now that the native FXArc exists. |
| `thaumcraft/client/fx/beams/FXArc.java` notes[2] | Consequence: the eldritch golem's arcing lightning and PacketFXBlockArc show no arc in 1.21.1; the golem's arcX/arcY/arcZ fields are written but never consumed. | Historical invisible-arc claim superseded by the native callers cited below (ThaumcraftEldritchClient and BlockFxClientEffects). |
| `thaumcraft/client/fx/beams/FXBeamPower.java` notes[0] | The visnet relay beam is ported, but with a different construction: two crossed planes at half width .105 instead of the original's two quads at half width 0.15*0.7=0.105 plus the original's separate additive flare quad on textures/misc/particles.png. | The relay beam and flare are both drawn at common/src/main/java/thaumcraft/client/renderers/tile/TileVisRelayRenderer.java:42; the old beam-without-flare comparison was obsolete. |
| `thaumcraft/client/fx/beams/FXBeamPower.java` notes[1] | The original opacity ramp (0.3 floor, 0.8 on pulse, -0.025/tick decay) and the IRevealer x10 opacity boost are not reproduced by VisNetRender.beam. | The previous missing-opacity-ramp claim was superseded: common/src/main/java/thaumcraft/client/renderers/tile/TileVisRelayRenderer.java:32-42 updates opacity, applies the revealer factors and draws beam plus flare. |
| `thaumcraft/client/fx/particles/FXSmokeSpiral.java` notes[0] | Not ported: neither the eldritch warden smoke spiral nor the elemental sword smoke spiral exists in the native port. | Historical missing-spiral claim withdrawn: common/src/main/java/thaumcraft/client/fx/particles/FXSmokeSpiral.java:79 creates the native effect, with the client callers cited below. |
| `thaumcraft/client/gui/ThaumcraftGuiConfig.java` notes[0] | Not ported: 1.21.1/Architectury has no FML mod-config GUI hook, so the in-game configuration screen does not exist. | The FML hook itself is not retained, but common/src/main/java/thaumcraft/client/ThaumcraftConfigClient.java:36 opens the native configuration screen. |
| `thaumcraft/client/gui/ThaumcraftGuiConfig.java` notes[1] | The configuration values themselves are still settable, but only by editing the properties file, not from the options menu. | The earlier properties-file-only claim was superseded by the native screen and config store cited in this row. |
| `thaumcraft/client/renderers/item/ItemNodeRenderer.java` notes[0] | Not an original TC4 class: a 1.12 item renderer, not needed by the native. | This legacy package path is a relocation: the oracle counterpart is thaumcraft/client/renderers/tile/ItemNodeRenderer.java:49-72, tracked by its own ledger row, not an absent original feature. |
| `thaumcraft/client/renderers/item/ItemNodeRenderer.java` notes[1] | The node items are drawn by their block models in the native port, which is the original art of the node family. | The native item renderer is common/src/main/java/thaumcraft/client/renderers/tile/ItemNodeRenderer.java; the old block-model-only claim was superseded by that port. |
| `thaumcraft/client/renderers/models/ModelBoreEmit.java` notes[0] | Legacy only class: the 4.2.3.5 oracle has no emitter model, the native port draws the vortex in the bore effect class. | AUDIT CORRECTION: this is an original, live model, not a legacy-only helper. Its geometry is folded into ModelArcaneBore, not replaced by a vortex. The exclude disposition is left unchanged for the leader to reclassify. |
| `thaumcraft/client/renderers/models/ModelBoreEmit.java` notes[1] | The native bore client effects own the vortex and the beam of the original, so the legacy emitter model has no counterpart. | The native call is common/src/main/java/thaumcraft/client/renderers/tile/TileArcaneBoreRenderer.java:61; source geometry matches the five original parts. This is not runtime visual acceptance. |
| `thaumcraft/client/renderers/models/ModelThaumatorium.java` notes[0] | Legacy only class: no oracle counterpart. The native class of the same name is an unused OBJ loader; the thaumatorium is drawn by the block model assets/thaumcraft/models/block/thaumatorium.json. | The oracle has no class at this model path, but TileThaumatoriumRenderer.java:45,72 loads and draws the OBJ inline; native common/src/main/java/thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java:18 constructs ModelThaumatorium, so the old unused-loader claim was false. |
| `thaumcraft/client/renderers/models/ModelThaumatorium.java` notes[2] | The renderer unit thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java carries the open client visual of the original geometry and stays partial. | The renderer unit owns the native visual port. The old stays-partial statement predates its closure; this audit does not provide runtime visual acceptance. |
| `thaumcraft/client/renderers/tile/ItemNodeRenderer.java` notes[0] | The original drew the aura node as a dropped item; the native node is a permanent block with no item form, so this renderer has no counterpart. | Historical no-item-form claim withdrawn: common/src/main/java/thaumcraft/client/renderers/tile/ItemNodeRenderer.java implements the native aura-node item renderer, as recorded by the later closure note. |
| `thaumcraft/client/renderers/tile/ItemNodeRenderer.java` notes[1] | The harvest of the native node drops the wisp essence of the original BlockAiry:265-279 (NodeHarvest), not a node item, so the gameplay drop of the original is covered even though the dropped item has no renderer. | NodeHarvest handles the wisp essence gameplay drop; the existence of an aura-node item renderer is a separate contract and does not imply a node-item gameplay drop. |
| `thaumcraft/client/renderers/tile/TileEssentiaReservoirRenderer.java` notes[0] | DEVIATION: the native port registers no renderer for the reservoir, so the stored essentia is invisible in the world. The tile still keeps the display aspect and the colour interpolation state, but nothing draws it. | Historical no-renderer claim withdrawn: common/src/main/java/thaumcraft/client/ThaumcraftReservoirClient.java:15 registers TileEssentiaReservoirRenderer, which draws stored essentia. |
| `thaumcraft/common/blocks/ItemBlocks/BlockMirrorItem.java` notes[0] | Legacy only class: no oracle counterpart. | The legacy ItemBlocks package is a relocation, not proof of no original behavior: the counterpart is oracle thaumcraft/common/blocks/BlockMirrorItem.java:51-59, tracked by its own canonical ledger row. |
| `thaumcraft/common/blocks/ItemBlocks/BlockMirrorItem.java` notes[1] | Legacy only class: no oracle counterpart, so it is outside the port obligation. | This duplicate legacy path is excluded only as a duplicate; the original mirror item remains in scope under thaumcraft/common/blocks/BlockMirrorItem.java. |
| `thaumcraft/common/blocks/ItemBlocks/BlockMirrorItem.java` notes[2] | The link of a linked mirror is a data component in the native tree, so the 1.12 item class of the legacy tree has no behaviour to carry over. | Native mirror links use a data component; this does not erase the original item behavior obligation. |
| `thaumcraft/common/blocks/JarStepSound.java` notes[0] | 1.21.1 block step sounds come from the block SoundType, so the per-block sound class was dropped. | JarStepSound itself has zero external references in the oracle; BlockJar.java:88 instead constructs the equivalent CustomStepSound. Its exclusion can rest on that dead-class evidence, not the old false direct-call claim. |
| `thaumcraft/common/blocks/JarStepSound.java` notes[1] | REMAINING GAP (audio): the original jar step sound (thaumcraft:jar) is therefore replaced by the vanilla glass step sound. The original sound event is in the port and is played when the jar is emptied. | REMAINING GAP (audio), owned by BlockJar: oracle BlockJar.java:88 uses CustomStepSound for thaumcraft:jar, while common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java:288,294 uses SoundType.GLASS. Playing the jar event when emptying does not restore step/break audio. |
| `thaumcraft/common/config/MaterialAiry.java` notes[1] | Partial: the light, collision and replaceable rules of the two consumers are reproduced, but the requires-a-tool flag of the original material has no native counterpart - the airy blocks declare no requiresCorrectToolForDrops. | Historical requires-tool gap withdrawn: oracle MaterialAiry.java:39-41 returns this without setting the flag; :31-33 is a different boolean override. |
| `thaumcraft/common/config/MaterialAiry.java` notes[2] | CORRECTED: the previous review recorded the requires-a-tool flag as a remaining gap. The original class overrides setRequiresTool() with an empty body (oracle MaterialAiry.java:31-33), so the constructor call never sets the flag and the airy material never required a tool. The native airy blocks declare no requiresCorrectToolForDrops, which is therefore the original behaviour and not a deviation. | CORRECTED: the original func_76221_f override (oracle MaterialAiry.java:39-41) does not set the flag. Its body returns this rather than being empty. The native airy blocks declare no requiresCorrectToolForDrops. |
| `thaumcraft/common/config/MaterialTaint.java` notes[0] | 1.21.1 has no net.minecraft.block.material.Material, so the class itself is deliberately not carried over; its observable traits (solid, not replaceable, not liquid, grass map colour) are expressed with the 1.21.1 block properties of the taint block family. | The Material class is replaced by native block properties; the old solid description contradicted the original false-returning override (oracle MaterialTaint.java:23-24) and the later correction below. |
| `thaumcraft/common/config/MaterialTaint.java` notes[1] | REMAINING GAP: the mobility flag of MaterialTaint (func_76227_m == 1, set by setNoPushMobility) has no direct 1.21.1 counterpart. The closest native property is the push reaction of the block (BlockBehaviour.Properties.pushReaction), which belongs to the blocks layer and was not changed in this wave. | Historical mobility gap superseded: common/src/main/java/thaumcraft/common/config/ThaumcraftTaint.java:76-77 sets and applies PushReaction.BLOCK. |
| `thaumcraft/common/config/MaterialTaint.java` notes[2] | The common/blocks package is outside the editable scope of this wave, so the unit stays partial. | The old stays-partial statement is historical and superseded by the later closure; it is not the current ledger verdict. |
| `thaumcraft/common/lib/SoundsTC.java` notes[0] | 1.21.1 has no SoundEvent registry, so a sound is a resource location: the alias holder has no counterpart and the same sound id is used directly. | The legacy alias holder is omitted; native callers construct SoundEvent values from resource locations (for example common/src/main/java/thaumcraft/common/blocks/BlockJar.java:53). This does not imply that 1.21.1 lacks a SoundEvent registry. |
| `thaumcraft/common/tiles/TileNitor.java` notes[1] | The native nitor registers only a client ticker, so the server side production of the nitor was not confirmed in this audit. | Historical production concern withdrawn: oracle TileNitor.java:18-27 only emits client particles and has no server production contract. |
| `thaumcraft/truetyper/FontHelper.java` notes[0] | Legacy-only (1.12) CodeChicken TrueType font helper; the TC4 jar has no such package. | Only the thaumcraft/truetyper package path is legacy-only: the oracle contains truetyper/FontHelper.java:22, tracked by its own ledger row. |
| `thaumcraft/truetyper/FontHelper.java` notes[2] | The package is a 1.12 only addition of the legacy reference; the 1.7.10 original has no such package and the native port does not need one. | The original root-level truetyper helper must not be confused with this relocated legacy path; its separate ledger row owns the dead-code audit. |
| `thaumcraft/truetyper/FontLoader.java` notes[0] | Legacy-only (1.12) CodeChicken TrueType font loader; not part of the TC4 jar and not needed by the native screens. | Only this package path is legacy-only: oracle truetyper/FontLoader.java:15 contains the original loader, tracked by its own ledger row. |
| `thaumcraft/truetyper/FontLoader.java` notes[1] | The package is a 1.12 only addition of the legacy reference; the 1.7.10 original has no such package and the native port does not need one. | The old not-part-of-TC4 assertion was false for the class; the separate original row owns the unused-font audit. |
| `thaumcraft/truetyper/Formatter.java` notes[0] | Legacy-only (1.12) CodeChicken TrueType formatter; not part of the TC4 jar and not needed by the native screens. | Only this package path is legacy-only: oracle truetyper/Formatter.java:6 contains the original formatter, tracked by its own ledger row. |
| `thaumcraft/truetyper/Formatter.java` notes[1] | The package is a 1.12 only addition of the legacy reference; the 1.7.10 original has no such package and the native port does not need one. | The old not-part-of-TC4 assertion was false for the class; the separate original row owns the transitive dead-code audit. |
| `thaumcraft/truetyper/TrueTypeFont.java` notes[0] | Legacy-only (1.12) CodeChicken TrueType renderer; not part of the TC4 jar and not needed by the native screens. | Only this package path is legacy-only: oracle truetyper/TrueTypeFont.java contains the original renderer, tracked by its own ledger row. |
| `thaumcraft/truetyper/TrueTypeFont.java` notes[1] | The package is a 1.12 only addition of the legacy reference; the 1.7.10 original has no such package and the native port does not need one. | The old not-part-of-TC4 assertion was false for the class; the separate original row owns the unused-font audit. |

## Decisions reserved for the leader

1. **Downgrade `thaumcraft/common/blocks/BlockJar.java` to partial / explicit audio deviation.** Oracle BlockJar.java:88 constructs CustomStepSound, whose :17-22 supplies thaumcraft-prefixed sound names. JarStepSound is an unused duplicate, not the live call target. Native common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchBlocks.java:288,294 chooses SoundType.GLASS. Native BlockJar.java:53,107 still plays the jar event on emptying, which does not restore stepping/breaking audio. The JarStepSound note admits this remaining gap, but attributes it to the wrong class; the live BlockJar verdict still hides the substitution.
2. **Downgrade `thaumcraft/api/nodes/INode.java` and `thaumcraft/common/tiles/TileNode.java` pending an accepted identity-format deviation.** Oracle TileNode.java:98-115 generates `dimension:x:y:z`. Native common/src/main/java/thaumcraft/common/tiles/TileNode.java:63,142 uses a random UUID string; native api/nodes/INode.java:34-38 documents that difference. The interface is present, but identical method signatures do not prove the original identity contract. This is a deliberate documented deviation, not an undiscovered missing method.
3. **Reclassify ModelBoreEmit disposition, without inferring a missing native model.** The absence premise is false; native geometry/callsite evidence now replaces it. I would not downgrade its geometry solely because the class was folded into ModelArcaneBore. Runtime appearance was not checked.
4. **Resolve the exclude policy mismatch.** The contract asks that excluded units have no counterpart, but many excluded rows explicitly contain real original implementations whose behavior was replaced or claimed dead. Either the leader records those distinct reasons as accepted exceptions or changes disposition. The 86 failed literal absence searches are not 86 independently demonstrated regressions.

## Validation and limits

- Six successful ledger edit batches were each followed by the specified checker; every result was stale_review_fingerprints: 0. The first two attempted patches failed verification atomically and made no edit.
- Independent counts and all non-evidence/non-note row fields were asserted identical to baseline. No counts or verdict was changed; the ledger was not reformatted.
- The post-fix citation spot-check selected 220 entries again and found zero mechanical missing-file/EOF/single-blank-or-brace failures. This does not assert that every ledger citation is now correct.
- No OpenTC4 reference was used. Official 1.7.10 client.jar was downloaded read-only into ignored .tmp/a1ledger/vanilla and SHA-1 verified as e80d9b3bf5085002218d4be59e668bac718abbc6. Both Forge Maven MCP download endpoints returned HTTP 403, so no new vanilla/MCP interpretation was used as a finding.
- No Gradle, compiled tests, gameplay run or visual acceptance was performed. Dead-code checks were source-level as described, not a reflection-proof or binary-linkage proof.
- A redundant audit-helper import exceeded a 120-second timeout before producing output. The helper was changed to index identifiers once; the rerun completed, reproduced the counts/sample, and checked all 79 corrected evidence strings for mechanical citation failures: zero. No result is attributed to the timed-out attempt.
- Reproduction: `python3 scripts/porting/audit_ledger_evidence.py` regenerates current-tree ignored raw searches/sample under `.tmp/a1ledger/`; committed manifests preserve this audit’s baseline sample and hit counts.
