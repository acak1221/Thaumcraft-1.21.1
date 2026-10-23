# e2paths — runtime resource requests

Branch: `agent/e2paths`; baseline: `974fde6a`.

## Outcome

- Reconciled all 37 client-audit concerns (33 construction sites + two classpath readers + two silent fallbacks), plus 11 shared/API construction sites found by independent grep.
- **56 exact requested paths were absent:** `thaumcraft:textures/blocks/warded_glass_1` through `_47` (missing `.png`) and nine `textures/models/wand_rod_<material>_staff.png` API requests (wrong suffix).
- Fixed the glass string composition and native API rod sheet selector. Existing artwork suffices; no downloads/extraction or new assets.
- Independently inspected the original TC4 JAR inventory and `javap -c` of original `StaffRod`: it uses the unsuffixed input for its texture, all corrected sheets are in the original, and no `_staff.png` sheets exist there. Read-only local evidence: `.tmp/e2paths-original-staffrod-javap.txt`; no original code/assets added to this commit.
- The 48 manifest entries now cover 285 distinct bundled files (590 site-path pairs), including 23 stitched block-atlas sprites. Four vanilla sprites are external, explicitly not missing mod files. Before correction: 341 distinct bundled request strings, 56 absent.
- Checked the four external lava/drip sprites against the actual 1.21.1 `minecraft-client.jar` downloaded into this worktree's isolated Loom cache: all present.
- Added one WARN per missing research icon per client session at both previously silent fallback sites.

The exact missing list, all concrete expansions, exclusions, provenance and limitations are in `docs/audit/RESOURCE_PATH_FIX.md`. Free-form public addon arguments are unbounded; the finite existence claims cover bundled callers/catalogues only. The native API rod-view constructor has no current bundled production caller; its faulty requests were not claimed to be observed client failures.

## Guard

`convert_assets.py --verify` now checks `composed-resource-paths.json` through `composed_resources.py`: explicit files, atlas inclusion, dynamic factory inventory, public helper callers, conservative supplier-code-token contracts (comments/formatting ignored), and live corpus icon/aspect/illustration sets. It also scans literal file factories and both classpath mesh streams (129 files). A future spelling, operator, range, data-flow, helper-caller or corpus change requires review, not implicit acceptance by globbing existing files. Unrelated supplier code changes can conservatively require re-review too.

Python mutation tests reproduce missing/extensionless/staff paths, a removed OBJ, an unstitched sprite, changed suppliers/inventories and a new corpus icon. A focused JUnit test constructs all 18 catalogue rods, asserts the real classloader path exists, and restores the mutable API registry.

## Executed checks

- `python3 scripts/porting/convert_assets.py --verify`: PASS, zero missing references. Local output: `.tmp/e2paths-verify.json`.
- `python3 -B -m unittest discover -s scripts/porting -p 'test_*.py'`: **111 tests passed**. Local output: `.tmp/e2paths-python.log`.
- `git diff --check`: PASS.
- `./gradlew :common:compileJava :fabric:compileJava :common:test --tests thaumcraft.api.WandApiTest --no-daemon`: **BUILD SUCCESSFUL**, seven executed tasks; all eight focused JUnit tests passed (zero skipped/failures/errors), including the new all-18-rod resource assertion. Local output: `.tmp/e2paths-gradle.log`; XML: `common/build/test-results/test/TEST-thaumcraft.api.WandApiTest.xml`. Initial wrapper/dependency setup exceeded a 120-second tool timeout; the retry completed in 9m 5s with the fresh isolated cache. Existing compiler/mixin warnings remain; no NeoForge build task was selected (Gradle did configure its included project/dependencies).

Gradle uses Java 21 and only `/home/cyberfox-kali/wt/e2paths/.gradle-e2`; `fabric/run-gametest/world` was removed before each Gradle invocation as instructed. No other worktree or the main checkout was touched. No client visual inspection, dedicated-server gameplay smoke, or NeoForge build was run; resource existence/compilation is not visual or gameplay parity. The two classpath readers' reload bypass remains a separate lifecycle concern, not silently claimed fixed.
