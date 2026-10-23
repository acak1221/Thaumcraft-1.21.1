# Runtime resource path audit and fixes — 2026-10-23

## Outcome

**56 distinct requested resources were absent: 47 extensionless warded-glass requests and nine native staff-rod requests with an erroneous `_staff` suffix.** Both families have existing artwork. Corrected composition; no assets downloaded, fabricated, renamed, or copied. All expanded bundled paths now exist.

The original **37 findings are 33 asset-construction sites, two classpath mesh-loading concerns, and two silent-icon fallback concerns**, not 37 unique files or even 37 distinct factory calls. This report preserves that numbering. A wider independent grep found 11 additional shared/API construction sites. The 48 entries expand to **285 distinct bundled files / 590 site-path pairs after correction**, including 23 block-atlas sprites. Before correction there were **341 distinct bundled request paths**, of which 56 were missing (the erroneous names coexist with correct names requested elsewhere). The original 37 entries alone cover 259 distinct files after correction (306 distinct requests before, 47 missing). Four vanilla sprite identifiers are external dependencies, not absent mod assets.

## Method and scope

Started from clean `agent/e2paths` at `974fde6a`. Read `LIB_QUALITY.md` and `CLIENT_QUALITY.md` resource sections. Independently searched `ResourceLocation.fromNamespaceAndPath`, unqualified `fromNamespaceAndPath`, `new ResourceLocation`, and `parse`/`tryParse`/`withDefaultNamespace`; followed helper callers, enum fields, numeric loops, material catalogues, research icon expressions, all shipped language `<IMG>` tokens, aspect tags, and mesh streams. Grep output was kept locally as `/tmp/opencode/e2paths-factories.txt`, `/tmp/opencode/e2paths-common-factories.txt`, and `/tmp/opencode/e2paths-helper-calls.txt`.

The executable inventory records **183 nonliteral factories across common/Fabric/NeoForge** in `scripts/porting/composed-resource-paths.json`, including exclusions. Sound-event IDs, registry IDs, tags, recipe IDs and model-layer IDs are not file paths and were not falsely reported as missing files. The client audit explicitly excludes those ten client sites and the vanilla drip selector. Literal texture/shader/OBJ/MTL factory requests and both classpath mesh streams are also checked automatically (129 unique literal files).

**Finite does not mean every possible public API argument.** `UtilsFX` texture readers, `VisNetRender.texture`, model material methods, `PlayerNotifications.addNotification`, public aspect/wand constructors, and addon-supplied `ResourceLocation`s accept arbitrary names. They have no finite mathematical expansion. The lists below enumerate the bundled catalogue and all current bundled callers; addon assets remain addon-owned. Existing UtilsFX WARN/error handling stays intact. Both research GUI fallbacks now issue one WARN per missing identifier per client session rather than silently omit an icon. No claim of addon asset completeness is made.

## Exact missing paths at the baseline

### Warded-glass renderer: 47 missing direct-file requests

`TileWardedGlassRenderer.java:35` constructed extensionless strings, then passed them directly to `RenderType.entityTranslucent` through `ResourceLocation.parse(TEXTURES[texture])` at line 52. This is **not** an atlas sprite lookup: the engine does not append `.png`. The earlier audit checked the PNG family, not these exact strings. Appended `.png` to the stored string; all 47 real PNGs were already present.

```text
thaumcraft:textures/blocks/warded_glass_1
thaumcraft:textures/blocks/warded_glass_2
thaumcraft:textures/blocks/warded_glass_3
thaumcraft:textures/blocks/warded_glass_4
thaumcraft:textures/blocks/warded_glass_5
thaumcraft:textures/blocks/warded_glass_6
thaumcraft:textures/blocks/warded_glass_7
thaumcraft:textures/blocks/warded_glass_8
thaumcraft:textures/blocks/warded_glass_9
thaumcraft:textures/blocks/warded_glass_10
thaumcraft:textures/blocks/warded_glass_11
thaumcraft:textures/blocks/warded_glass_12
thaumcraft:textures/blocks/warded_glass_13
thaumcraft:textures/blocks/warded_glass_14
thaumcraft:textures/blocks/warded_glass_15
thaumcraft:textures/blocks/warded_glass_16
thaumcraft:textures/blocks/warded_glass_17
thaumcraft:textures/blocks/warded_glass_18
thaumcraft:textures/blocks/warded_glass_19
thaumcraft:textures/blocks/warded_glass_20
thaumcraft:textures/blocks/warded_glass_21
thaumcraft:textures/blocks/warded_glass_22
thaumcraft:textures/blocks/warded_glass_23
thaumcraft:textures/blocks/warded_glass_24
thaumcraft:textures/blocks/warded_glass_25
thaumcraft:textures/blocks/warded_glass_26
thaumcraft:textures/blocks/warded_glass_27
thaumcraft:textures/blocks/warded_glass_28
thaumcraft:textures/blocks/warded_glass_29
thaumcraft:textures/blocks/warded_glass_30
thaumcraft:textures/blocks/warded_glass_31
thaumcraft:textures/blocks/warded_glass_32
thaumcraft:textures/blocks/warded_glass_33
thaumcraft:textures/blocks/warded_glass_34
thaumcraft:textures/blocks/warded_glass_35
thaumcraft:textures/blocks/warded_glass_36
thaumcraft:textures/blocks/warded_glass_37
thaumcraft:textures/blocks/warded_glass_38
thaumcraft:textures/blocks/warded_glass_39
thaumcraft:textures/blocks/warded_glass_40
thaumcraft:textures/blocks/warded_glass_41
thaumcraft:textures/blocks/warded_glass_42
thaumcraft:textures/blocks/warded_glass_43
thaumcraft:textures/blocks/warded_glass_44
thaumcraft:textures/blocks/warded_glass_45
thaumcraft:textures/blocks/warded_glass_46
thaumcraft:textures/blocks/warded_glass_47
```

### Native API rod view: nine missing staff-sheet requests

`WandRod.java:66` used `rod.tag()` even for the nine catalogue staff entries. Changed it to `rod.baseTag()`, matching the client renderer and `StaffRod` override. The retained legacy `src/main/java/thaumcraft/api/wands/StaffRod.java:14,23` explicitly uses the unsuffixed `tag` for textures; the modern `WandMaterials.Rod.baseTag()` documents the same rule. The native-view constructor is public but has no current bundled production caller: this repairs a real API request defect, not a claim that all nine were observed on screen.

```text
thaumcraft:textures/models/wand_rod_blaze_staff.png
thaumcraft:textures/models/wand_rod_bone_staff.png
thaumcraft:textures/models/wand_rod_greatwood_staff.png
thaumcraft:textures/models/wand_rod_ice_staff.png
thaumcraft:textures/models/wand_rod_obsidian_staff.png
thaumcraft:textures/models/wand_rod_primal_staff.png
thaumcraft:textures/models/wand_rod_quartz_staff.png
thaumcraft:textures/models/wand_rod_reed_staff.png
thaumcraft:textures/models/wand_rod_silverwood_staff.png
```

The corresponding existing files are each name with `_staff` removed. The public free-form WandRod constructors keep their addon naming contract; StaffRod constructors override the transient parent texture with the original unsuffixed sheet. No nonexistent transient constructor value is claimed to be a requested asset.

## Original-JAR confirmation

Read the original `Thaumcraft-1.7.10-4.2.3.5.jar` without modifying it. Its archive contains all 47 corrected glass PNGs and all nine unsuffixed staff material PNGs, and none of the nine `_staff.png` sheets. `javap -c thaumcraft.api.wands.StaffRod` confirms the original constructor concatenates `textures/models/wand_rod_` with its unsuffixed input argument (`aload_1`) and `.png`, even though it passes a suffixed registry tag to the parent constructor. Local bytecode evidence: `.tmp/e2paths-original-staffrod-javap.txt` (not committed). No source decompilation or original JAR modification was performed.

## Guard and regression evidence

`convert_assets.py --verify` and the shared `validate_resources()` entry point (also used by verification recording) now invoke `composed_resources.validate` in addition to existing model/atlas/sound checks. The checked-in manifest explicitly lists every allowed concrete expansion, never infers completeness by globbing existing PNGs, and checks files under `common/src/main/resources`. Sprite requests are additionally required to be stitched in the block atlas. Both classpath-only mesh paths and literal ResourceLocation-based meshes are checked. Only the tiny model-only unit-test fixture explicitly opts out of the Java contract.

The balanced factory inventory rejects changed/new nonliteral factories. A separate helper-call inventory prevents a new `VisNetRender.texture(...)` caller from escaping coverage just because its factory already existed. Conservative supplier contracts cover all code tokens in the relevant supplier classes, ignoring comments/formatting; changes to strings, operators, numeric bounds or data flow require review of the expansion rather than blessing whatever happens to exist. Live research icons, aspect paths and all locale illustrations are compared to the declared sets on every verification. This is a deliberately bounded audited contract, not a Java interpreter; unrelated code changes can conservatively require re-review. Review the expansion and update the relevant inventory/contract when intentionally changing a supplier; do not blindly regenerate hashes.

`test_composed_resources.py` exercises removal of an otherwise unreferenced ripple texture, exact extensionless warded requests, exact bad staff requests, an unstitched-but-existing sprite, a missing literal OBJ, changed factory/helper inventories, changed supplier bounds/strings, a new corpus icon, and comment/string-safe parsing. `WandApiTest.everyNativeRodIncludingStaffsUsesAnExistingBaseMaterialSheet` additionally constructs all 18 real catalogue rods and asserts classloader resource existence, restoring the mutable public registry afterward.

Both classpath loaders already fail loudly for absent OBJ files. Their static caching/resource-pack reload bypass remains an independent concern; this task adds existence coverage, not a mesh lifecycle refactor. Neither the verifier nor compilation is visual/gameplay acceptance.

## The 37 audit findings and exact post-fix expansions

All identifiers below are actual direct resource IDs unless marked **sprite**. A sprite `thaumcraft:blocks/name` maps to `common/src/main/resources/assets/thaumcraft/textures/blocks/name.png` and must also be in the block atlas. A direct ID maps to `common/src/main/resources/assets/<namespace>/<path>`. Every listed post-fix file exists.

### 1. `common/src/main/java/thaumcraft/client/BoreClientEffects.java`

**file; 5 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/misc/beam1.png
thaumcraft:textures/misc/beam2.png
thaumcraft:textures/misc/vortex.png
thaumcraft:textures/models/bore.png
thaumcraft:textures/models/jar.png
```

### 2. `common/src/main/java/thaumcraft/client/ThaumcraftArchitectClient.java`

**file; 47 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/blocks/warded_glass_1.png
thaumcraft:textures/blocks/warded_glass_10.png
thaumcraft:textures/blocks/warded_glass_11.png
thaumcraft:textures/blocks/warded_glass_12.png
thaumcraft:textures/blocks/warded_glass_13.png
thaumcraft:textures/blocks/warded_glass_14.png
thaumcraft:textures/blocks/warded_glass_15.png
thaumcraft:textures/blocks/warded_glass_16.png
thaumcraft:textures/blocks/warded_glass_17.png
thaumcraft:textures/blocks/warded_glass_18.png
thaumcraft:textures/blocks/warded_glass_19.png
thaumcraft:textures/blocks/warded_glass_2.png
thaumcraft:textures/blocks/warded_glass_20.png
thaumcraft:textures/blocks/warded_glass_21.png
thaumcraft:textures/blocks/warded_glass_22.png
thaumcraft:textures/blocks/warded_glass_23.png
thaumcraft:textures/blocks/warded_glass_24.png
thaumcraft:textures/blocks/warded_glass_25.png
thaumcraft:textures/blocks/warded_glass_26.png
thaumcraft:textures/blocks/warded_glass_27.png
thaumcraft:textures/blocks/warded_glass_28.png
thaumcraft:textures/blocks/warded_glass_29.png
thaumcraft:textures/blocks/warded_glass_3.png
thaumcraft:textures/blocks/warded_glass_30.png
thaumcraft:textures/blocks/warded_glass_31.png
thaumcraft:textures/blocks/warded_glass_32.png
thaumcraft:textures/blocks/warded_glass_33.png
thaumcraft:textures/blocks/warded_glass_34.png
thaumcraft:textures/blocks/warded_glass_35.png
thaumcraft:textures/blocks/warded_glass_36.png
thaumcraft:textures/blocks/warded_glass_37.png
thaumcraft:textures/blocks/warded_glass_38.png
thaumcraft:textures/blocks/warded_glass_39.png
thaumcraft:textures/blocks/warded_glass_4.png
thaumcraft:textures/blocks/warded_glass_40.png
thaumcraft:textures/blocks/warded_glass_41.png
thaumcraft:textures/blocks/warded_glass_42.png
thaumcraft:textures/blocks/warded_glass_43.png
thaumcraft:textures/blocks/warded_glass_44.png
thaumcraft:textures/blocks/warded_glass_45.png
thaumcraft:textures/blocks/warded_glass_46.png
thaumcraft:textures/blocks/warded_glass_47.png
thaumcraft:textures/blocks/warded_glass_5.png
thaumcraft:textures/blocks/warded_glass_6.png
thaumcraft:textures/blocks/warded_glass_7.png
thaumcraft:textures/blocks/warded_glass_8.png
thaumcraft:textures/blocks/warded_glass_9.png
```

### 3. `common/src/main/java/thaumcraft/client/ThaumcraftExcavationClient.java`

**file; 2 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/misc/beam2.png
thaumcraft:textures/misc/particles.png
```

### 4. `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java`

**file; 2 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/void_robe_armor.png
thaumcraft:textures/models/void_robe_armor_overlay.png
```

### 5. `common/src/main/java/thaumcraft/client/ThaumcraftShockClient.java`

**file; 2 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/misc/p_large.png
thaumcraft:textures/misc/p_small.png
```

### 6. `common/src/main/java/thaumcraft/client/ThaumcraftWarpClient.java`

**file; 4 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:shaders/post/warp_blurred.json
thaumcraft:shaders/post/warp_death_gaze.json
thaumcraft:shaders/post/warp_hunger.json
thaumcraft:shaders/post/warp_sun_scorned.json
```

### 7. `common/src/main/java/thaumcraft/client/fx/particles/WarpSonicParticle.java`

**file; 15 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/ripple1.png
thaumcraft:textures/models/ripple10.png
thaumcraft:textures/models/ripple11.png
thaumcraft:textures/models/ripple12.png
thaumcraft:textures/models/ripple13.png
thaumcraft:textures/models/ripple14.png
thaumcraft:textures/models/ripple15.png
thaumcraft:textures/models/ripple2.png
thaumcraft:textures/models/ripple3.png
thaumcraft:textures/models/ripple4.png
thaumcraft:textures/models/ripple5.png
thaumcraft:textures/models/ripple6.png
thaumcraft:textures/models/ripple7.png
thaumcraft:textures/models/ripple8.png
thaumcraft:textures/models/ripple9.png
```

### 8. `common/src/main/java/thaumcraft/client/gui/GuiFocalManipulator.java`

**file; 2 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/gui/gui_wandtable.png
thaumcraft:textures/misc/particles.png
```

### 9. `common/src/main/java/thaumcraft/client/gui/GuiFocusSelect.java`

**file; 2 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/misc/radial.png
thaumcraft:textures/misc/radial2.png
```

### 10. `common/src/main/java/thaumcraft/client/gui/GuiResearchBrowser.java`

**file; 31 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/blocks/alchemyblock.png
thaumcraft:textures/foci/vampirebats.png
thaumcraft:textures/gui/gui_research.png
thaumcraft:textures/gui/gui_researchback.png
thaumcraft:textures/gui/gui_researchbackeldritch.png
thaumcraft:textures/misc/nodes.png
thaumcraft:textures/misc/particles.png
thaumcraft:textures/misc/r_alchent.png
thaumcraft:textures/misc/r_alchman.png
thaumcraft:textures/misc/r_alchmult.png
thaumcraft:textures/misc/r_aspects.png
thaumcraft:textures/misc/r_eldritchmajor.png
thaumcraft:textures/misc/r_eldritchminor.png
thaumcraft:textures/misc/r_enchant.png
thaumcraft:textures/misc/r_infernalfurnace.png
thaumcraft:textures/misc/r_mask0.png
thaumcraft:textures/misc/r_mask1.png
thaumcraft:textures/misc/r_mask2.png
thaumcraft:textures/misc/r_nodepreserve.png
thaumcraft:textures/misc/r_nodes.png
thaumcraft:textures/misc/r_nodes_2.png
thaumcraft:textures/misc/r_nodetap1.png
thaumcraft:textures/misc/r_nodetap2.png
thaumcraft:textures/misc/r_outer.png
thaumcraft:textures/misc/r_outerrev.png
thaumcraft:textures/misc/r_pech.png
thaumcraft:textures/misc/r_resdupe.png
thaumcraft:textures/misc/r_researcher1.png
thaumcraft:textures/misc/r_researcher2.png
thaumcraft:textures/misc/r_runicupg.png
thaumcraft:textures/misc/r_warp.png
```

### 11. `common/src/main/java/thaumcraft/client/gui/GuiResearchPopup.java`

**file; 26 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/blocks/alchemyblock.png
thaumcraft:textures/foci/vampirebats.png
thaumcraft:textures/misc/r_alchent.png
thaumcraft:textures/misc/r_alchman.png
thaumcraft:textures/misc/r_alchmult.png
thaumcraft:textures/misc/r_aspects.png
thaumcraft:textures/misc/r_eldritchmajor.png
thaumcraft:textures/misc/r_eldritchminor.png
thaumcraft:textures/misc/r_enchant.png
thaumcraft:textures/misc/r_infernalfurnace.png
thaumcraft:textures/misc/r_mask0.png
thaumcraft:textures/misc/r_mask1.png
thaumcraft:textures/misc/r_mask2.png
thaumcraft:textures/misc/r_nodepreserve.png
thaumcraft:textures/misc/r_nodes.png
thaumcraft:textures/misc/r_nodes_2.png
thaumcraft:textures/misc/r_nodetap1.png
thaumcraft:textures/misc/r_nodetap2.png
thaumcraft:textures/misc/r_outer.png
thaumcraft:textures/misc/r_outerrev.png
thaumcraft:textures/misc/r_pech.png
thaumcraft:textures/misc/r_resdupe.png
thaumcraft:textures/misc/r_researcher1.png
thaumcraft:textures/misc/r_researcher2.png
thaumcraft:textures/misc/r_runicupg.png
thaumcraft:textures/misc/r_warp.png
```

### 12. `common/src/main/java/thaumcraft/client/gui/GuiResearchRecipe.java`

**file; 13 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/items/amber.png
thaumcraft:textures/items/golem_upgrade_air.png
thaumcraft:textures/items/golem_upgrade_entropy.png
thaumcraft:textures/items/golem_upgrade_fire.png
thaumcraft:textures/items/golem_upgrade_order.png
thaumcraft:textures/items/quicksilver.png
thaumcraft:textures/misc/eldritchajor1.png
thaumcraft:textures/misc/eldritchajor2.png
thaumcraft:textures/misc/research1.png
thaumcraft:textures/misc/research2.png
thaumcraft:textures/misc/research3.png
thaumcraft:textures/misc/research4.png
thaumcraft:textures/misc/research5.png
```

### 13. `common/src/main/java/thaumcraft/client/gui/GuiResearchTable.java`

**file; 7 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/aspects/_back.png
thaumcraft:textures/aspects/_unknown.png
thaumcraft:textures/gui/guiresearchtable2.png
thaumcraft:textures/gui/hex1.png
thaumcraft:textures/misc/parchment3.png
thaumcraft:textures/misc/particles.png
thaumcraft:textures/misc/script.png
```

### 14. `common/src/main/java/thaumcraft/client/gui/HoveredSlotAspectRow.java`

**file; 2 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/aspects/_back.png
thaumcraft:textures/aspects/_unknown.png
```

### 15. `common/src/main/java/thaumcraft/client/lib/PlayerNotifications.java`

**file; 48 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/aspects/aer.png
thaumcraft:textures/aspects/alienis.png
thaumcraft:textures/aspects/aqua.png
thaumcraft:textures/aspects/arbor.png
thaumcraft:textures/aspects/auram.png
thaumcraft:textures/aspects/bestia.png
thaumcraft:textures/aspects/cognitio.png
thaumcraft:textures/aspects/corpus.png
thaumcraft:textures/aspects/exanimis.png
thaumcraft:textures/aspects/fabrico.png
thaumcraft:textures/aspects/fames.png
thaumcraft:textures/aspects/gelum.png
thaumcraft:textures/aspects/herba.png
thaumcraft:textures/aspects/humanus.png
thaumcraft:textures/aspects/ignis.png
thaumcraft:textures/aspects/instrumentum.png
thaumcraft:textures/aspects/iter.png
thaumcraft:textures/aspects/limus.png
thaumcraft:textures/aspects/lucrum.png
thaumcraft:textures/aspects/lux.png
thaumcraft:textures/aspects/machina.png
thaumcraft:textures/aspects/messis.png
thaumcraft:textures/aspects/metallum.png
thaumcraft:textures/aspects/meto.png
thaumcraft:textures/aspects/mortuus.png
thaumcraft:textures/aspects/motus.png
thaumcraft:textures/aspects/ordo.png
thaumcraft:textures/aspects/pannus.png
thaumcraft:textures/aspects/perditio.png
thaumcraft:textures/aspects/perfodio.png
thaumcraft:textures/aspects/permutatio.png
thaumcraft:textures/aspects/potentia.png
thaumcraft:textures/aspects/praecantatio.png
thaumcraft:textures/aspects/sano.png
thaumcraft:textures/aspects/sensus.png
thaumcraft:textures/aspects/spiritus.png
thaumcraft:textures/aspects/telum.png
thaumcraft:textures/aspects/tempestas.png
thaumcraft:textures/aspects/tenebrae.png
thaumcraft:textures/aspects/terra.png
thaumcraft:textures/aspects/tutamen.png
thaumcraft:textures/aspects/vacuos.png
thaumcraft:textures/aspects/venenum.png
thaumcraft:textures/aspects/victus.png
thaumcraft:textures/aspects/vinculum.png
thaumcraft:textures/aspects/vitium.png
thaumcraft:textures/aspects/vitreus.png
thaumcraft:textures/aspects/volatus.png
```

### 16. `common/src/main/java/thaumcraft/client/lib/UtilsFX.java`

**file; 2 expansions.** Present; added verifier coverage (no asset/code-name change required).

Bundled callers only; public path arguments are addon-owned and unbounded; existing WARN fallback retained.

```text
thaumcraft:textures/items/lightningringv.png
thaumcraft:textures/misc/wisp.png
```

### 17. `common/src/main/java/thaumcraft/client/renderers/VisNetRender.java`

**file; 26 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/items/lightningringv.png
thaumcraft:textures/misc/beam1.png
thaumcraft:textures/misc/nodes.png
thaumcraft:textures/misc/p_large.png
thaumcraft:textures/misc/p_small.png
thaumcraft:textures/misc/particles.png
thaumcraft:textures/misc/particles2.png
thaumcraft:textures/models/hemis0.png
thaumcraft:textures/models/hemis1.png
thaumcraft:textures/models/hemis10.png
thaumcraft:textures/models/hemis11.png
thaumcraft:textures/models/hemis12.png
thaumcraft:textures/models/hemis13.png
thaumcraft:textures/models/hemis14.png
thaumcraft:textures/models/hemis15.png
thaumcraft:textures/models/hemis2.png
thaumcraft:textures/models/hemis3.png
thaumcraft:textures/models/hemis4.png
thaumcraft:textures/models/hemis5.png
thaumcraft:textures/models/hemis6.png
thaumcraft:textures/models/hemis7.png
thaumcraft:textures/models/hemis8.png
thaumcraft:textures/models/hemis9.png
thaumcraft:textures/models/node_converter.png
thaumcraft:textures/models/node_converter_over.png
thaumcraft:textures/models/vis_relay.png
```

### 18. `common/src/main/java/thaumcraft/client/renderers/block/LegacyBlockQuads.java`

**sprite; 9 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:blocks/legacy_stone
thaumcraft:blocks/taint_fibres
thaumcraft:blocks/taint_over_1
thaumcraft:blocks/taint_over_2
thaumcraft:blocks/taint_over_3
thaumcraft:blocks/taint_spore_stalk_1
thaumcraft:blocks/taint_spore_stalk_2
thaumcraft:blocks/taintgrass1
thaumcraft:blocks/taintgrass2
```

### 19. `common/src/main/java/thaumcraft/client/renderers/block/PositionBlockModels.java`

**sprite; 4 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:blocks/es_1
thaumcraft:blocks/es_2
thaumcraft:blocks/es_3
thaumcraft:blocks/es_4
```

### 20. `common/src/main/java/thaumcraft/client/renderers/entity/RenderPech.java`

**file; 3 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/pech_forage.png
thaumcraft:textures/models/pech_stalker.png
thaumcraft:textures/models/pech_thaum.png
```

### 21. `common/src/main/java/thaumcraft/client/renderers/entity/RenderTravelingTrunk.java`

**file; 6 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/items/golem_upgrade_air.png
thaumcraft:textures/items/golem_upgrade_earth.png
thaumcraft:textures/items/golem_upgrade_entropy.png
thaumcraft:textures/items/golem_upgrade_fire.png
thaumcraft:textures/items/golem_upgrade_order.png
thaumcraft:textures/items/golem_upgrade_water.png
```

### 22. `common/src/main/java/thaumcraft/client/renderers/models/ModelEssentiaCrystalizer.java`

**file; 2 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/crystalizer.obj
thaumcraft:textures/models/vis_relay.obj
```

### 23. `common/src/main/java/thaumcraft/client/renderers/models/gear/HarnessArmorRenderer.java`

**file; 3 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/hoverharness.obj
thaumcraft:textures/models/hoverharness.png
thaumcraft:textures/models/hoverharness2.png
```

### 24. `common/src/main/java/thaumcraft/client/renderers/models/gear/ModelHoverHarness.java`

**file; 3 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/hoverharness.obj
thaumcraft:textures/models/hoverharness.png
thaumcraft:textures/models/hoverharness2.png
```

### 25. `common/src/main/java/thaumcraft/client/renderers/models/gear/ModelWand.java:rodTexture`

**file; 10 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/wand_rod_blaze.png
thaumcraft:textures/models/wand_rod_bone.png
thaumcraft:textures/models/wand_rod_greatwood.png
thaumcraft:textures/models/wand_rod_ice.png
thaumcraft:textures/models/wand_rod_obsidian.png
thaumcraft:textures/models/wand_rod_primal.png
thaumcraft:textures/models/wand_rod_quartz.png
thaumcraft:textures/models/wand_rod_reed.png
thaumcraft:textures/models/wand_rod_silverwood.png
thaumcraft:textures/models/wand_rod_wood.png
```

### 26. `common/src/main/java/thaumcraft/client/renderers/models/gear/ModelWand.java:capTexture`

**file; 5 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/wand_cap_copper.png
thaumcraft:textures/models/wand_cap_gold.png
thaumcraft:textures/models/wand_cap_iron.png
thaumcraft:textures/models/wand_cap_thaumium.png
thaumcraft:textures/models/wand_cap_void.png
```

### 27. `common/src/main/java/thaumcraft/client/renderers/tile/TileAlchemyFurnaceAdvancedRenderer.java`

**file; 4 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/alch_furnace.png
thaumcraft:textures/models/alch_furnace_on.png
thaumcraft:textures/models/alch_furnace_tank.png
thaumcraft:textures/models/alch_furnace_tank_on.png
```

### 28. `common/src/main/java/thaumcraft/client/renderers/tile/TileAlchemyFurnaceAdvancedRenderer.java:quad`

**sprite; 2 expansions.** Present; added verifier coverage (no asset/code-name change required).

Also minecraft:block/lava_still: vanilla external sprite, not a bundled file.

```text
thaumcraft:blocks/fluxgoo
thaumcraft:blocks/metalbase
```

### 29. `common/src/main/java/thaumcraft/client/renderers/tile/TileNodeStabilizerRenderer.java`

**file; 2 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/node_stabilizer.png
thaumcraft:textures/models/node_stabilizer_over.png
```

### 30. `common/src/main/java/thaumcraft/client/renderers/tile/TileResearchTableRenderer.java`

**file; 4 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/blocks/tablequill.png
thaumcraft:textures/misc/parchment.png
thaumcraft:textures/models/restable.png
thaumcraft:textures/models/restable2.png
```

### 31. `common/src/main/java/thaumcraft/client/renderers/tile/TileTubeRenderer.java`

**sprite; 8 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:blocks/pipe_1
thaumcraft:blocks/pipe_2
thaumcraft:blocks/pipe_3
thaumcraft:blocks/pipe_filter
thaumcraft:blocks/pipe_filter_core
thaumcraft:blocks/pipe_oneway
thaumcraft:blocks/pipe_restrict
thaumcraft:blocks/pipe_valve
```

### 32. `common/src/main/java/thaumcraft/client/renderers/tile/TileWardedGlassRenderer.java`

**file; 47 expansions.** 47 missing extensionless requests corrected by appending `.png`.

```text
thaumcraft:textures/blocks/warded_glass_1.png
thaumcraft:textures/blocks/warded_glass_10.png
thaumcraft:textures/blocks/warded_glass_11.png
thaumcraft:textures/blocks/warded_glass_12.png
thaumcraft:textures/blocks/warded_glass_13.png
thaumcraft:textures/blocks/warded_glass_14.png
thaumcraft:textures/blocks/warded_glass_15.png
thaumcraft:textures/blocks/warded_glass_16.png
thaumcraft:textures/blocks/warded_glass_17.png
thaumcraft:textures/blocks/warded_glass_18.png
thaumcraft:textures/blocks/warded_glass_19.png
thaumcraft:textures/blocks/warded_glass_2.png
thaumcraft:textures/blocks/warded_glass_20.png
thaumcraft:textures/blocks/warded_glass_21.png
thaumcraft:textures/blocks/warded_glass_22.png
thaumcraft:textures/blocks/warded_glass_23.png
thaumcraft:textures/blocks/warded_glass_24.png
thaumcraft:textures/blocks/warded_glass_25.png
thaumcraft:textures/blocks/warded_glass_26.png
thaumcraft:textures/blocks/warded_glass_27.png
thaumcraft:textures/blocks/warded_glass_28.png
thaumcraft:textures/blocks/warded_glass_29.png
thaumcraft:textures/blocks/warded_glass_3.png
thaumcraft:textures/blocks/warded_glass_30.png
thaumcraft:textures/blocks/warded_glass_31.png
thaumcraft:textures/blocks/warded_glass_32.png
thaumcraft:textures/blocks/warded_glass_33.png
thaumcraft:textures/blocks/warded_glass_34.png
thaumcraft:textures/blocks/warded_glass_35.png
thaumcraft:textures/blocks/warded_glass_36.png
thaumcraft:textures/blocks/warded_glass_37.png
thaumcraft:textures/blocks/warded_glass_38.png
thaumcraft:textures/blocks/warded_glass_39.png
thaumcraft:textures/blocks/warded_glass_4.png
thaumcraft:textures/blocks/warded_glass_40.png
thaumcraft:textures/blocks/warded_glass_41.png
thaumcraft:textures/blocks/warded_glass_42.png
thaumcraft:textures/blocks/warded_glass_43.png
thaumcraft:textures/blocks/warded_glass_44.png
thaumcraft:textures/blocks/warded_glass_45.png
thaumcraft:textures/blocks/warded_glass_46.png
thaumcraft:textures/blocks/warded_glass_47.png
thaumcraft:textures/blocks/warded_glass_5.png
thaumcraft:textures/blocks/warded_glass_6.png
thaumcraft:textures/blocks/warded_glass_7.png
thaumcraft:textures/blocks/warded_glass_8.png
thaumcraft:textures/blocks/warded_glass_9.png
```

### 33. `common/src/main/java/thaumcraft/client/renderers/tile/TileWardedRenderer.java`

**file; 47 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/blocks/warded_glass_1.png
thaumcraft:textures/blocks/warded_glass_10.png
thaumcraft:textures/blocks/warded_glass_11.png
thaumcraft:textures/blocks/warded_glass_12.png
thaumcraft:textures/blocks/warded_glass_13.png
thaumcraft:textures/blocks/warded_glass_14.png
thaumcraft:textures/blocks/warded_glass_15.png
thaumcraft:textures/blocks/warded_glass_16.png
thaumcraft:textures/blocks/warded_glass_17.png
thaumcraft:textures/blocks/warded_glass_18.png
thaumcraft:textures/blocks/warded_glass_19.png
thaumcraft:textures/blocks/warded_glass_2.png
thaumcraft:textures/blocks/warded_glass_20.png
thaumcraft:textures/blocks/warded_glass_21.png
thaumcraft:textures/blocks/warded_glass_22.png
thaumcraft:textures/blocks/warded_glass_23.png
thaumcraft:textures/blocks/warded_glass_24.png
thaumcraft:textures/blocks/warded_glass_25.png
thaumcraft:textures/blocks/warded_glass_26.png
thaumcraft:textures/blocks/warded_glass_27.png
thaumcraft:textures/blocks/warded_glass_28.png
thaumcraft:textures/blocks/warded_glass_29.png
thaumcraft:textures/blocks/warded_glass_3.png
thaumcraft:textures/blocks/warded_glass_30.png
thaumcraft:textures/blocks/warded_glass_31.png
thaumcraft:textures/blocks/warded_glass_32.png
thaumcraft:textures/blocks/warded_glass_33.png
thaumcraft:textures/blocks/warded_glass_34.png
thaumcraft:textures/blocks/warded_glass_35.png
thaumcraft:textures/blocks/warded_glass_36.png
thaumcraft:textures/blocks/warded_glass_37.png
thaumcraft:textures/blocks/warded_glass_38.png
thaumcraft:textures/blocks/warded_glass_39.png
thaumcraft:textures/blocks/warded_glass_4.png
thaumcraft:textures/blocks/warded_glass_40.png
thaumcraft:textures/blocks/warded_glass_41.png
thaumcraft:textures/blocks/warded_glass_42.png
thaumcraft:textures/blocks/warded_glass_43.png
thaumcraft:textures/blocks/warded_glass_44.png
thaumcraft:textures/blocks/warded_glass_45.png
thaumcraft:textures/blocks/warded_glass_46.png
thaumcraft:textures/blocks/warded_glass_47.png
thaumcraft:textures/blocks/warded_glass_5.png
thaumcraft:textures/blocks/warded_glass_6.png
thaumcraft:textures/blocks/warded_glass_7.png
thaumcraft:textures/blocks/warded_glass_8.png
thaumcraft:textures/blocks/warded_glass_9.png
```

### 34. `common/src/main/java/thaumcraft/client/renderers/models/ModelAdvancedAlchemyFurnace.java`

**file; 1 expansions.** Present; added verifier coverage (no asset/code-name change required).

Audit classpath bypass; already throws for absent mesh. No reload refactor in this existence fix.

```text
thaumcraft:textures/models/adv_alch_furnace.obj
```

### 35. `common/src/main/java/thaumcraft/client/renderers/models/ModelThaumometer.java`

**file; 1 expansions.** Present; added verifier coverage (no asset/code-name change required).

Audit classpath bypass; already throws for absent mesh. No reload refactor in this existence fix.

```text
thaumcraft:textures/models/scanner.obj
```

### 36. `common/src/main/java/thaumcraft/client/gui/GuiResearchPopup.java:silent-fallback`

**file; 27 expansions.** All builtin icons present; added once-per-session missing-icon WARN for unexpected/pack/addon omissions.

```text
thaumcraft:textures/blocks/alchemyblock.png
thaumcraft:textures/foci/vampirebats.png
thaumcraft:textures/items/thaumonomicon.png
thaumcraft:textures/misc/r_alchent.png
thaumcraft:textures/misc/r_alchman.png
thaumcraft:textures/misc/r_alchmult.png
thaumcraft:textures/misc/r_aspects.png
thaumcraft:textures/misc/r_eldritchmajor.png
thaumcraft:textures/misc/r_eldritchminor.png
thaumcraft:textures/misc/r_enchant.png
thaumcraft:textures/misc/r_infernalfurnace.png
thaumcraft:textures/misc/r_mask0.png
thaumcraft:textures/misc/r_mask1.png
thaumcraft:textures/misc/r_mask2.png
thaumcraft:textures/misc/r_nodepreserve.png
thaumcraft:textures/misc/r_nodes.png
thaumcraft:textures/misc/r_nodes_2.png
thaumcraft:textures/misc/r_nodetap1.png
thaumcraft:textures/misc/r_nodetap2.png
thaumcraft:textures/misc/r_outer.png
thaumcraft:textures/misc/r_outerrev.png
thaumcraft:textures/misc/r_pech.png
thaumcraft:textures/misc/r_resdupe.png
thaumcraft:textures/misc/r_researcher1.png
thaumcraft:textures/misc/r_researcher2.png
thaumcraft:textures/misc/r_runicupg.png
thaumcraft:textures/misc/r_warp.png
```

### 37. `common/src/main/java/thaumcraft/client/gui/GuiResearchBrowser.java:silent-fallback`

**file; 26 expansions.** All builtin icons present; added once-per-session missing-icon WARN for unexpected/pack/addon omissions.

```text
thaumcraft:textures/blocks/alchemyblock.png
thaumcraft:textures/foci/vampirebats.png
thaumcraft:textures/misc/r_alchent.png
thaumcraft:textures/misc/r_alchman.png
thaumcraft:textures/misc/r_alchmult.png
thaumcraft:textures/misc/r_aspects.png
thaumcraft:textures/misc/r_eldritchmajor.png
thaumcraft:textures/misc/r_eldritchminor.png
thaumcraft:textures/misc/r_enchant.png
thaumcraft:textures/misc/r_infernalfurnace.png
thaumcraft:textures/misc/r_mask0.png
thaumcraft:textures/misc/r_mask1.png
thaumcraft:textures/misc/r_mask2.png
thaumcraft:textures/misc/r_nodepreserve.png
thaumcraft:textures/misc/r_nodes.png
thaumcraft:textures/misc/r_nodes_2.png
thaumcraft:textures/misc/r_nodetap1.png
thaumcraft:textures/misc/r_nodetap2.png
thaumcraft:textures/misc/r_outer.png
thaumcraft:textures/misc/r_outerrev.png
thaumcraft:textures/misc/r_pech.png
thaumcraft:textures/misc/r_resdupe.png
thaumcraft:textures/misc/r_researcher1.png
thaumcraft:textures/misc/r_researcher2.png
thaumcraft:textures/misc/r_runicupg.png
thaumcraft:textures/misc/r_warp.png
```

## Additional independently found shared/API construction sites


### 38. `common/src/main/java/thaumcraft/api/aspects/Aspect.java`

**file; 48 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/aspects/aer.png
thaumcraft:textures/aspects/alienis.png
thaumcraft:textures/aspects/aqua.png
thaumcraft:textures/aspects/arbor.png
thaumcraft:textures/aspects/auram.png
thaumcraft:textures/aspects/bestia.png
thaumcraft:textures/aspects/cognitio.png
thaumcraft:textures/aspects/corpus.png
thaumcraft:textures/aspects/exanimis.png
thaumcraft:textures/aspects/fabrico.png
thaumcraft:textures/aspects/fames.png
thaumcraft:textures/aspects/gelum.png
thaumcraft:textures/aspects/herba.png
thaumcraft:textures/aspects/humanus.png
thaumcraft:textures/aspects/ignis.png
thaumcraft:textures/aspects/instrumentum.png
thaumcraft:textures/aspects/iter.png
thaumcraft:textures/aspects/limus.png
thaumcraft:textures/aspects/lucrum.png
thaumcraft:textures/aspects/lux.png
thaumcraft:textures/aspects/machina.png
thaumcraft:textures/aspects/messis.png
thaumcraft:textures/aspects/metallum.png
thaumcraft:textures/aspects/meto.png
thaumcraft:textures/aspects/mortuus.png
thaumcraft:textures/aspects/motus.png
thaumcraft:textures/aspects/ordo.png
thaumcraft:textures/aspects/pannus.png
thaumcraft:textures/aspects/perditio.png
thaumcraft:textures/aspects/perfodio.png
thaumcraft:textures/aspects/permutatio.png
thaumcraft:textures/aspects/potentia.png
thaumcraft:textures/aspects/praecantatio.png
thaumcraft:textures/aspects/sano.png
thaumcraft:textures/aspects/sensus.png
thaumcraft:textures/aspects/spiritus.png
thaumcraft:textures/aspects/telum.png
thaumcraft:textures/aspects/tempestas.png
thaumcraft:textures/aspects/tenebrae.png
thaumcraft:textures/aspects/terra.png
thaumcraft:textures/aspects/tutamen.png
thaumcraft:textures/aspects/vacuos.png
thaumcraft:textures/aspects/venenum.png
thaumcraft:textures/aspects/victus.png
thaumcraft:textures/aspects/vinculum.png
thaumcraft:textures/aspects/vitium.png
thaumcraft:textures/aspects/vitreus.png
thaumcraft:textures/aspects/volatus.png
```

### 39. `common/src/main/java/thaumcraft/api/wands/FocusUpgradeType.java`

**file; 21 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/foci/alchemistsfire.png
thaumcraft:textures/foci/alchemistsfrost.png
thaumcraft:textures/foci/architect.png
thaumcraft:textures/foci/batbombs.png
thaumcraft:textures/foci/chainlightning.png
thaumcraft:textures/foci/devilbats.png
thaumcraft:textures/foci/dowsing.png
thaumcraft:textures/foci/earthshock.png
thaumcraft:textures/foci/enlarge.png
thaumcraft:textures/foci/extend.png
thaumcraft:textures/foci/fireball.png
thaumcraft:textures/foci/firebeam.png
thaumcraft:textures/foci/frugal.png
thaumcraft:textures/foci/iceboulder.png
thaumcraft:textures/foci/nightshade.png
thaumcraft:textures/foci/potency.png
thaumcraft:textures/foci/scattershot.png
thaumcraft:textures/foci/seeker.png
thaumcraft:textures/foci/silktouch.png
thaumcraft:textures/foci/treasure.png
thaumcraft:textures/foci/vampirebats.png
```

### 40. `common/src/main/java/thaumcraft/api/wands/WandCap.java:32`

**file; 5 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/wand_cap_copper.png
thaumcraft:textures/models/wand_cap_gold.png
thaumcraft:textures/models/wand_cap_iron.png
thaumcraft:textures/models/wand_cap_thaumium.png
thaumcraft:textures/models/wand_cap_void.png
```

### 41. `common/src/main/java/thaumcraft/api/wands/WandCap.java:44`

**file; 5 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/wand_cap_copper.png
thaumcraft:textures/models/wand_cap_gold.png
thaumcraft:textures/models/wand_cap_iron.png
thaumcraft:textures/models/wand_cap_thaumium.png
thaumcraft:textures/models/wand_cap_void.png
```

### 42. `common/src/main/java/thaumcraft/api/wands/WandCap.java:57`

**file; 5 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/wand_cap_copper.png
thaumcraft:textures/models/wand_cap_gold.png
thaumcraft:textures/models/wand_cap_iron.png
thaumcraft:textures/models/wand_cap_thaumium.png
thaumcraft:textures/models/wand_cap_void.png
```

### 43. `common/src/main/java/thaumcraft/api/wands/WandRod.java:51`

**file; 10 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/wand_rod_blaze.png
thaumcraft:textures/models/wand_rod_bone.png
thaumcraft:textures/models/wand_rod_greatwood.png
thaumcraft:textures/models/wand_rod_ice.png
thaumcraft:textures/models/wand_rod_obsidian.png
thaumcraft:textures/models/wand_rod_primal.png
thaumcraft:textures/models/wand_rod_quartz.png
thaumcraft:textures/models/wand_rod_reed.png
thaumcraft:textures/models/wand_rod_silverwood.png
thaumcraft:textures/models/wand_rod_wood.png
```

### 44. `common/src/main/java/thaumcraft/api/wands/WandRod.java:56`

**file; 10 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/wand_rod_blaze.png
thaumcraft:textures/models/wand_rod_bone.png
thaumcraft:textures/models/wand_rod_greatwood.png
thaumcraft:textures/models/wand_rod_ice.png
thaumcraft:textures/models/wand_rod_obsidian.png
thaumcraft:textures/models/wand_rod_primal.png
thaumcraft:textures/models/wand_rod_quartz.png
thaumcraft:textures/models/wand_rod_reed.png
thaumcraft:textures/models/wand_rod_silverwood.png
thaumcraft:textures/models/wand_rod_wood.png
```

### 45. `common/src/main/java/thaumcraft/api/wands/WandRod.java:66`

**file; 10 expansions.** Nine absent `_staff` requests corrected to base material sheets.

```text
thaumcraft:textures/models/wand_rod_blaze.png
thaumcraft:textures/models/wand_rod_bone.png
thaumcraft:textures/models/wand_rod_greatwood.png
thaumcraft:textures/models/wand_rod_ice.png
thaumcraft:textures/models/wand_rod_obsidian.png
thaumcraft:textures/models/wand_rod_primal.png
thaumcraft:textures/models/wand_rod_quartz.png
thaumcraft:textures/models/wand_rod_reed.png
thaumcraft:textures/models/wand_rod_silverwood.png
thaumcraft:textures/models/wand_rod_wood.png
```

### 46. `common/src/main/java/thaumcraft/api/wands/StaffRod.java:17`

**file; 9 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/wand_rod_blaze.png
thaumcraft:textures/models/wand_rod_bone.png
thaumcraft:textures/models/wand_rod_greatwood.png
thaumcraft:textures/models/wand_rod_ice.png
thaumcraft:textures/models/wand_rod_obsidian.png
thaumcraft:textures/models/wand_rod_primal.png
thaumcraft:textures/models/wand_rod_quartz.png
thaumcraft:textures/models/wand_rod_reed.png
thaumcraft:textures/models/wand_rod_silverwood.png
```

### 47. `common/src/main/java/thaumcraft/api/wands/StaffRod.java:26`

**file; 9 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/models/wand_rod_blaze.png
thaumcraft:textures/models/wand_rod_bone.png
thaumcraft:textures/models/wand_rod_greatwood.png
thaumcraft:textures/models/wand_rod_ice.png
thaumcraft:textures/models/wand_rod_obsidian.png
thaumcraft:textures/models/wand_rod_primal.png
thaumcraft:textures/models/wand_rod_quartz.png
thaumcraft:textures/models/wand_rod_reed.png
thaumcraft:textures/models/wand_rod_silverwood.png
```

### 48. `common/src/main/java/thaumcraft/common/config/ThaumcraftResearchCategories.java`

**file; 8 expansions.** Present; added verifier coverage (no asset/code-name change required).

```text
thaumcraft:textures/gui/gui_researchback.png
thaumcraft:textures/gui/gui_researchbackeldritch.png
thaumcraft:textures/items/thaumonomiconcheat.png
thaumcraft:textures/misc/r_artifice.png
thaumcraft:textures/misc/r_crucible.png
thaumcraft:textures/misc/r_eldritch.png
thaumcraft:textures/misc/r_golemancy.png
thaumcraft:textures/misc/r_thaumaturgy.png
```

## External vanilla dependencies

Not expected in `common/src/main/resources`, and not included in the 56 missing mod requests. Independently checked all four against the actual 1.21.1 client JAR in this worktree's isolated Loom cache: `textures/block/lava_still.png` and `textures/particle/drip_{hang,fall,land}.png` are present. Mojang provides these; no unnecessary legacy copies were extracted.

- `TileAlchemyFurnaceAdvancedRenderer.java:quad`: `minecraft:block/lava_still`.
- `TaintDropParticle.java:frame`: `minecraft:drip_hang`, `minecraft:drip_fall`, `minecraft:drip_land`.

## Verification

- `python3 scripts/porting/convert_assets.py --verify`: PASS; 285 composed files, 48 entries, 23 composed block-atlas sprites, 129 literal client files; existing checks: 3,944 model texture references, 3,018 local model references, 518 atlas sprites, 111 sound files, 66 sound events; zero missing references.
- Python suite: 111 tests passed. Common/Fabric Java compilation: PASS. Focused `thaumcraft.api.WandApiTest`: eight tests passed, zero skipped/failures/errors; includes actual classloader checks for all 18 native rods. Full commands/log locations: `docs/analysis/2026-10-23-agent-e2paths.md`.
- Client visual inspection and dedicated-server gameplay smoke were not run for this path-only fix.
