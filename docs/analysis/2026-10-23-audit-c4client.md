# c4client — client rendering quality audit

Date: 2026-10-23. Baseline: `3340abe7`. Branch: `agent/c4client`.
Worktree: `/home/cyberfox-kali/wt/c4client` only. Initial status clean.

## Outcome

**Not review-ready.** Full findings, complete pose/state ledgers, transform
operation ledger, allocation formulas, resource and dead-surface inventories:
[`docs/audit/CLIENT_QUALITY.md`](../audit/CLIENT_QUALITY.md).

| Checklist | Inspected | Findings at baseline / disposition |
|---|---|---|
| Pose | 242 pushes + 242 pops, 108 files | 85 exception-exposure sites; fixed 5, left 80 |
| State | 374 mutating calls, 45 files | 38 file-level incoming-state/exception ownership gaps; unchanged |
| Allocation | 336 files; 2,256 current declarations; 1,053 class-new, 2,030 array, 307 lambda AST nodes | 20 prioritized frame hotspots with per-instance/per-frame formulas |
| Size/duplication | 336 sizes, including 188 renderer files | 22 over250 (6 renderer files); 20 size candidates + 5 duplicate-family findings; 2 generated-data exceptions |
| Transforms | 767 direct PoseStack operations, 199 scopes (850 lexical transform-like calls including non-pose math) | 113 scopes without a method-local file:line-shaped oracle anchor |
| Numbers | 26,764 numeric literal nodes | Worst30 provenance/naming cases; fixed1 |
| Resources | 246 ResourceLocation constructions, 44 parameterized; 15 stream opens including 2 classloader streams | 33 mod-asset path coverage sites + 2 classpath reload-bypass sites + 2 silent-icon paths =37 |
| Dead/consistency | 188 renderer names; 2,256 current method/constructor declarations, plus removed accessor | 7 disconnected classes +37 method candidates +3 family discrepancies +1 fixed accessor =48 baseline inventory rows |

The counts are different units and overlap. In particular:

* **Zero** unmatched textual pushes/pops and **zero** identified normal-return
  stack bypasses does **not** mean exception safety: 80 exposed scopes remain.
* **38** state-ownership findings are file-level source review gaps, **not** a
  measured count of GL leaks. Default resets differ from incoming-state restore;
  intentional fog output/flat-GUI postconditions and RenderType callback
  contracts are separately classified. No claim of world-depth corruption seen.
* Transform anchors were checked for review traceability, **not independently
  reread/verified against the private oracle**. Anchor presence is not proof of
  multiplication-order parity. Missing local anchors can coexist with broad
  class-level citations.
* Allocation counts are source-level pre-escape-analysis estimates. Constructors,
  bake/reload and chunk rebuilding are not relabelled per-frame. Capturing
  lambdas are potential allocations, empty List.of is shared. No GC profile.
* No-production-caller methods include test-facing seams and public compatibility
  APIs, not 37 proven deletable methods. Method references are included. Overload,
  reflection and external-addon reachability are not proven by a name inventory.

## Changes (six production files)

1. `renderers/block/LegacyBlockQuads.java`: protect its single render pose with
   immediate try/finally.
2. `renderers/entity/RenderFrostShard.java`: same for the mesh render scope.
3. `renderers/tile/TileArcaneWorkbenchRenderer.java`: protect the table and stored
   wand scopes separately; keep the intervening early return outside both.
4. `renderers/tile/TileTubeBufferRenderer.java`: protect the per-side choke stem.
5. `renderers/item/ItemThaumometerRenderer.java`: name
   `INVENTORY_Y_ROTATION_DEGREES = 248.0F` with existing original
   ItemThaumometerRenderer:141–145 provenance. Correct class prose from Z to -Y;
   the actual transform is unchanged.
6. `ThaumcraftGearModelsClient.java`: remove the uncalled private
   `fortressLight()` accessor and its cached field; leave model-layer
   registration and all drawing intact.

No blend/depth/cull/shader behavior, palette, RNG, emitted vertex, successful-path
transform sequence, resource or registration changed. Kept the explicitly
historical invisible Watcher body and all disconnected public classes/methods.

## Validation actually run

* Java21 `javac` bootstrap and final all-common compile: **PASS**, **1,445** real
  production sources, no stubs. Args `.tmp/client-javac-all.args`; final log
  `.tmp/client-javac-final.log`, both ignored. Options `--release 21 -proc:none
  -sourcepath ''`; output `common/build/classes/java/main` in this worktree and
  that directory also on classpath.
* Named/remapped original Minecraft1.21.1 merged jar from
  `~/.gradle-leader/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/`
  plus cached dependencies/named Architectury API. The pre-existing
  `/tmp/opencode/a2quality/minecraft-access-overlay.jar` reproduces compile-time
  access-widener visibility only. It is not a stub or a changed upstream jar.
  No Gradle/NeoForge build was run. No other worktree/primary checkout was used.
* Java compiler-tree parsing of all336 client files: **PASS**. Ignored
  `.tmp/ClientTrees.java` and `.tmp/client-trees.tsv` retain the per-method/node
  inventory; `.tmp/client_inventory.py` and `.tmp/client_inventory.json` retain
  push/pop/state sites. The generated report's ledgers are committed, not these
  transient tools/build classes.
* Source-token comparison for all five pose edits and the constant substitution:
  **PASS**. Removing newly added try/finally wrappers recovers the exact old
  executable tokens in the four affected pose files; substituting248.0F for the
  named constant and removing its declaration recovers the old item renderer.
  Private unused accessor/field removal was inspected separately.
* `python3 scripts/porting/convert_assets.py --verify`: **PASS**: 3,944 model
  texture refs, 3,018 local model refs, 518 atlas sprites, 111 sound refs,
  66 sound events, zero missing refs. It never inspects Java or OBJ/MTL/shader
  graphs, so this does **not** close resource path validation.
* Independent literal asset check: **159 calls /127 unique texture/shader paths**
  exist in common resources or vanilla named jar. All15 ripple frames and all47
  warded-glass variants exist. No claim all addon/dynamic path outputs validated.
* `git diff --check`: **PASS**.

**Not run:** Gradle, JUnit, client/server, screenshots, runtime GL/exception
injection, resource-reload simulation, allocation profiler. No visual acceptance
claimed. No public push.

## Follow-up priorities

1. Isolate remaining80 pose scopes; ensure queued GUI flush/colour restoration
   cannot prevent pop. Exception injection would be a useful narrow test.
2. Agree on draw-time state ownership, then consolidate snapshots rather than
   replacing default-reset code with uncoordinated GL reads before buffered work.
3. Replace high-multiplier temporary corner arrays with scalar emission, retaining
   per-family UV/winding/RNG quirks. Avoid caching sprites across reload without
   invalidation.
4. Add explicit bounded Java resource-family and OBJ/MTL/shader/corpus dependency
   coverage; mandatory missing assets should produce actionable reload diagnostics.
5. Add method-local oracle/native-adapter provenance before reviewing large
   transform/number-heavy classes. Do not invent citations or normalize numbers.
