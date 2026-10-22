# h3parity: adversarial gameplay parity findings

Baseline `e79b8558`, branch `agent/h3parity`, 2026-10-23. Oracle paths below are relative to `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`; native paths are relative to this worktree. No game execution or visual acceptance is claimed.

## Quantitative result

| Hunt | Checked | Confirmed behavioral mismatches | Fixed in source |
|---|---:|---:|---:|
| Recipe data | 48 crucible recipes: research, aspect costs, output quantity, original registration association | 0 in checked fields | 0 |
| Entity aspect data and reachability | All 69 declarations (116 expanded rows including wisps), names, literal amounts, byte predicates | **9 missing live name bindings** | **9** |
| Aspect tree | All 48 declarations: 6 primal, 42 compounds; parents, order, tags, colours | 0 | 0 |
| Gameplay numbers | All 56 numeric cells and 8 boolean cells of the 8-material golem table | 0 | 0 |
| Ordering/side effects | 3 paths: aspect registration order, entity last-match replacement, golem ordinal lookup | 0 | 0 |
| Excludes | 20 suspicious original-bearing rows screened below | 0 newly proven dropped features; 1 additional false absence assertion | 0 |

This is NOT a full recipe/research audit: ingredient **identities**, the remaining recipe types, all-key completeness, item/block aspect bindings, and research page text were not certified. The 48 recipe checks do not imply 48 complete line-by-line ingredient proofs. The required 60 aspect-tag sample is met by the 69 entity declarations, not by pretending legacy-derived object tags are original evidence. `PARITY_CHECKS.json` records every checked recipe/key/source line and every entity declaration. Reproduce with:

```sh
python3 scripts/porting/audit_h3parity.py /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle
```

## F1–F9: correct entity tag rows were unreachable from real native mobs

Root cause: `EntityAspectBindings.describe` translated native IDs using an incomplete `NAMES` map. Unmapped mobs retained `thaumcraft:...`, whereas their registered rows use `Thaumcraft....`; `matchesName` accepts neither spelling as the other. Thus `select` returns null and `describe` returns null. The original returns the registered aspect list. This affects scanning/research rewards, not just a display label.

Shared native citations: `common/src/main/java/thaumcraft/common/lib/research/EntityAspectBindings.java:27-65` (map), `:71-82` (name translation and rejection), `:120-130` (row selection and equality). Before this patch the nine entries were absent at baseline `e79b8558`; the added map lines are `:49-57`.

| Finding | Native ID | Original registration and original reward | Native registration proving the mob exists |
|---|---|---|---|
| F1 | `thaumcraft:thaum_slime` | `thaumcraft/common/config/ConfigAspects.java:87`: limus2, praecantatio1, aqua1 | `common/src/main/java/thaumcraft/common/config/ThaumcraftEldritchMobs.java:42-44` |
| F2 | `thaumcraft:tainted_pig` | `ConfigAspects.java:96`: vitium2, terra2 | `common/src/main/java/thaumcraft/common/config/ThaumcraftTaintAnimals.java:44` |
| F3 | `thaumcraft:tainted_sheep` | `ConfigAspects.java:97`: vitium2, terra2 | `ThaumcraftTaintAnimals.java:45` |
| F4 | `thaumcraft:tainted_cow` | `ConfigAspects.java:98`: vitium3, terra3 | `ThaumcraftTaintAnimals.java:43` |
| F5 | `thaumcraft:tainted_chicken` | `ConfigAspects.java:99`: vitium2, volatus2, aer1 | `ThaumcraftTaintAnimals.java:42` |
| F6 | `thaumcraft:tainted_villager` | `ConfigAspects.java:100`: vitium3, aer2 | `ThaumcraftTaintAnimals.java:46` |
| F7 | `thaumcraft:taint_creeper` | `ConfigAspects.java:101`: vitium2, ignis2 | `common/src/main/java/thaumcraft/common/config/ThaumcraftTaintCreeper.java:28` |
| F8 | `thaumcraft:cultist_knight` | `ConfigAspects.java:105`: alienis1, humanus2, perditio1 | `common/src/main/java/thaumcraft/common/config/ThaumcraftCultists.java:29-32` |
| F9 | `thaumcraft:cultist_cleric` | `ConfigAspects.java:106`: alienis1, humanus2, perditio1 | `ThaumcraftCultists.java:33-36` |

Unqualified `ConfigAspects.java` in this table means oracle `thaumcraft/common/config/ConfigAspects.java`; abbreviated native filenames share the directory of the preceding full filename.

Fix: add exactly those nine original-name mappings. No aspect amounts invented or changed. Expose the existing translation as a package-private helper so tests exercise the same translation as `describe`, rather than testing only already-translated original names. Regression: `common/src/test/java/thaumcraft/research/EntityAspectBindingsTest.java:10-29` pins all nine original rewards and unknown-ID fallback. The independent Python test asserts all 69 original names have map coverage and pins these nine concrete native IDs. Baseline map inspection reproduces all nine failures; post-fix Python audit passes.

## Other checked behavior

- Aspect table: oracle `thaumcraft/api/aspects/Aspect.java:26-73` versus native `common/src/main/java/thaumcraft/api/aspects/Aspect.java:15-62`. All parent pairs and their registration order agree. No fabricated compound found.
- Golems: oracle `thaumcraft/common/entities/golems/EnumGolemType.java:10-17,43-51` versus native `common/src/main/java/thaumcraft/common/entities/golems/EnumGolemType.java:5-12,41-50`. Seven numeric properties plus one boolean per material agree. Array lookup preserves the original ordinal-keyed lookup, including invalid-material null.
- Entity replacement order: oracle `thaumcraft/common/lib/research/ScanManager.java:218-230` versus native `common/src/main/java/thaumcraft/common/lib/research/EntityAspectBindings.java:120-126`. Both keep the last matching row, not a sum. Aspect insertion order also agrees; deterministic player aspects depend on it.
- Crucible checks: oracle `thaumcraft/common/config/ConfigRecipes.java:106-180`; native `common/src/main/resources/data/thaumcraft/recipe/crucible/*.json`. All 48 native rows have an original research/cost/output-count association, including the six loop-expanded balanced shards. No mismatch found in these fields. Conditional tin/silver/lead recipes and ingredient flattening remain outside this conclusion.

## Exclude screening: 20 rows, not 20 runtime proofs

The names below identify ledger `canonical_path` basenames. Existing implementations were inspected, not deleted because an old row said “exclude.” No row was reclassified in this branch.

| Excluded row | Screening result |
|---|---|
| `FXArc` | Live original, native `client/fx/particles/FXArc` exists; exclusion is not absence. |
| `FXBeam` | Original generic beam has no discovered sender/caller; boss uses the separate `FXBeamGolemBoss`. No dropped live effect established. |
| `FXBeamPower` | Live relay effect is represented by native `VisNetRender`/`TileVisRelayRenderer`. |
| `FXBubbleAlt` | Oracle whole-tree name scan finds declaration/constructor only. No dropped feature proven. |
| `FXScorch` | Only construction is oracle `client/lib/UtilsFX.java:139`; `shootFire` has declaration only at `:130`. |
| `FXSmokeSpiral` | Native class and client hooks exist. |
| `FXSmokeTrail` | Original construction is `ClientProxy.java:892`; `soulTrail` has proxy declarations but no discovered caller. |
| `BlockEssentiaReservoirRenderer` | Oracle `:31,40` uses bounds 2..14 for BOTH inventory and world. Native `models/block/essentia_reservoir.json:9-10` has those bounds and item inherits it. Old “full cube” note is false. |
| `BlockTaintFibreRenderer` | Native shared `TaintFibreWorldQuads`/Fabric adapter exists. Not absent; frame correctness not accepted. |
| `BlockTaintRenderer` | Shared overlay implementation exists. Old unimplemented-overlay note is superseded; frames unverified. |
| `BlockTubeRenderer` | Tube arms/core represented by native `TileTubeRenderer`. |
| `BlockWardedRenderer` | **False oracle-absence assertion**: oracle file exists, `:20-27` renders remembered block. Native `TileWardedRenderer.java:34-37` renders remembered state. No dropped feature proven. |
| `ItemBannerRenderer` | Native same-name renderer exists and calls `TileBannerRenderer.draw`; old plain-cube claim is obsolete. |
| `ItemThaumometerRenderer` | Native same-name renderer, scanner model, arm and readout exist. Not an impossible API migration. |
| `ModelBoreEmit` | Live original emitter; geometry folded into native `ModelArcaneBore`. Reclassify as replacement, not dropped. |
| `ModelVoidRobe` | Oracle whole-tree name scan finds class/constructor only. Actual robe item uses `gear/ModelRobe`. |
| `ItemJarFilledRenderer` | Filled jar identity flattened; native `JarItemRenderer` exists. Needs visual acceptance, not an automatic feature-loss verdict. |
| `TileEssentiaReservoirRenderer` | Native same-name renderer exists. Historical “no liquid renderer” claim is false. |
| `MaterialAiry` | Material flags moved to block properties/piston rules. **Follow-up concern**: oracle mobility `1` (`:43-49`) must be checked against official vanilla piston semantics; interface-only native `OriginalPistonRule.java:31-32` is not a sufficient proof. Not fixed without vanilla proof. |
| `MaterialTaint` | Same mobility follow-up: oracle `:43-49` versus native `ThaumcraftTaint`/`ThaumcraftFlux` push reactions. No verified behavioral mismatch claimed without official vanilla proof. |

Rows believed to have conclusively dropped a real feature in this screening: **none**. Rows still worrying: **MaterialAiry, MaterialTaint** (piston semantics); renderer rows require actual frames. Separately, **BlockWardedRenderer** has a demonstrably false absence premise but a native replacement, not demonstrated feature loss.

## Verification and blocker

- PASS: independent Python audit, all counts above, with `PARITY_CHECKS.json` output.
- Java compile-check **attempted and FAILED** using `javac -proc:none`, the Mojang-remapped 1.21.1 merged jar, `common/build/classes/java/main`, cached dependency jars, and actual `common/src/main/java` sourcepath, targeting the changed production and JUnit files. Log: ignored `.tmp/h3parity/javac.log`.
- This clean worktree has **no** `common/build/classes/java/main`. Sourcepath dependency compilation encounters intermediary-named Architectury classes (`class_6880`, `class_1799`, etc.) and un-applied access wideners (`LivingEntity.lastHurtByPlayerTime`, `ItemEntity.pickupDelay`). The changed `EntityAspectBindings.class` was emitted, but compiler exit 1 is **not** a compile pass.
- JUnit test authored, **not executed**. No Gradle, server, client, or multiplayer run. Parent must compile/run the regression in its prepared mapped/widened build environment before marking this runtime-verified.
