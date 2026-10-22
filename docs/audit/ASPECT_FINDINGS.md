# Research graph and aspect audit — 2026-10-23 (`g19aspect`)

## PROVEN by executed oracle/data tests

Oracle: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`, TC4 4.2.3.5.
Only this agent's worktree was edited. No original/donor source was modified.

| Check | Count | Result |
|---|---:|---|
| Research keys, category, column, row, parents, hidden parents, siblings | 201 | Zero differences, both retained native declarations and bundled modern data versus original |
| Full ordered original page descriptors versus bundled modern data | 591 | Zero differences (includes recipe descriptors and gated pages) |
| Parent closure | 201, from 34 parentless entries | All reachable; unreachable set **empty** |
| Coordinate overlaps | 199 nonvirtual entries | Zero within a category; two virtual entries deliberately excluded |
| Literal item/block aspect samples | 220 flattened IDs, 176 original source registrations | Zero aspect-amount mismatches after resolving the four bad anchors below; 144 Minecraft and 76 Thaumcraft IDs |
| Compound aspect component arrays, including multiplicity | 42 compounds, 84 input slots | Zero differences; six primals have no component pair |

The closure includes **both normal and hidden parents**, not siblings (siblings
are not prerequisites). Every iteration adds only entries whose entire parent
set has already been reached. Mutation tests verify that a nonexistent parent
and a two-node cycle are detected. This proves graph reachability, not that every
in-world clue/event can be obtained in survival with every configuration.

The tag test obtains **amounts from the original ConfigAspects**, never from the
native fixture. Explicit existing identifier-flattening rows supply modern IDs;
the original line and original item/ore-dictionary target are checked separately.
Only literal direct lists are counted. Dynamic/recipe-derived lists, complex
registrations and runtime stack bonuses are not falsely counted as literals.

## Confirmed defect and fix (no gameplay amount changes)

The first executed tag test failed before any resource edits:

```text
AssertionError: No direct registration at line 360: ['thaumcraft:cluster_iron']
No direct registration at line 361: ['thaumcraft:cluster_gold']
No direct registration at line 362: ['thaumcraft:cluster_cinnabar']
Wrong oracle target at line 454: ['thaumcraft:primordial_pearl']: new ItemStack(ConfigBlocks.blockEldritch, 1, 3)
FAILED (failures=1)
```

Corrected `early_object_aspects.json` provenance lines:

| Item | Before | Correct original line |
|---|---:|---:|
| Iron Cluster | 360 | 368 |
| Gold Cluster | 361 | 369 |
| Cinnabar Cluster | 362 | 370 |
| Primordial Pearl | 454 | 451 |

The cluster anchors previously pointed into potion iteration, not registrations.
Pearl's anchor pointed to an eldritch block. The existing literal amounts already
matched the **correct** oracle registrations, so no research edges, tag amounts
or combination recipes were changed to manufacture a gameplay fix.

## Tests and exact extractor output

```sh
python3 scripts/porting/test_aspect_research_audit.py -v
```

Result: **6 tests run, OK**. Includes full graph metadata/page parity, actual
closure, overlap checks, deliberate broken-graph controls, literal tag parity,
all combination arrays, and independently generated Java fixture freshness.

```sh
python3 scripts/porting/extract_tc4_definitions.py \
  --oracle /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle --check
```

Actual output:

```json
{"aspects": 48, "research_count": 201, "research_key_category_sha256": "397d620877d429c49d4db6ae65552a681ac5b26be5d60cd1e95e5e28cae9f40e", "text_page_count": 294, "text_page_sha256": "66a10cd39a47e78472134a35d3b7b4e1b78618996ac6c87907aeb63303d4bf38", "mode": "check"}
```

**Hash scope:** 294 distinct unconditional `tc.research_page.*` keys, not a hash
of rendered localized prose. The additional page-descriptor comparison checks
all 591 pages, their ordering, text keys, gates, and original recipe expressions.
It does not certify translations or client rendering.

## Native behavioral verification

`common/src/test/java/thaumcraft/research/AspectResearchOracleAuditTest.java`
exercises the real `ResearchCatalog`, `hasPrerequisites`, `ObjectAspectCatalog`
and `ResearchAspectCombination.plan`, using the oracle-generated data-only
fixture `common/src/test/resources/thaumcraft/aspect_research_oracle.json`.

It checks the native parent closure, coordinate uniqueness, 220 native direct
tags and both input orders of all 42 compounds. For every compound it checks
one point consumed per parent, first-discovery award 3 (1+2), subsequent award 1,
and preservation of the unstaged actor. Execution status is recorded in the
agent analysis log; authored tests alone are not runtime proof.

## Reasoned-only / outside this bounded proof

* There is **no separate numeric combination-cost table in original Aspect.java**:
  it declares two component references per compound. Original packet lines
  97–121 spend one point per input and call `checkAndSyncAspectKnowledge(..., 1)`;
  first discovery adds two in original ScanManager. Thus the constructor's color
  or blend integer is not an aspect quantity. Existing authority hardening of
  forged bonuses/same-input underpayment is not a new discrepancy fixed here.
* This sample is not a claim of exhaustive ConfigAspects coverage, all vanilla
  metadata adaptations, all public addon writes, or recipe-generated tag parity.
* No new game-server smoke or client visual acceptance is claimed for a change
  consisting only of four provenance anchors and audit tests.
