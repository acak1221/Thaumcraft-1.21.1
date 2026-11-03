# Thaumatorium recipe navigation — 2026-11-04

Reported case: iron ingots can make both thaumium and Tainted Magic shadow metal,
but the machine screen appears to offer only thaumium.

The screen drew `<   >` starting at x96 while its click handler treated every
x-coordinate below 117 as “previous”. Most of the visible next glyph therefore
selected the previous page. The server already includes all researched recipes
matching the catalyst, together with installed formulas.

The original TC4 4.2.3.5 `GuiThaumatorium` (oracle lines 82–92 and 204–225) displays
one output at a time with two textured arrows at (128,16) and (128,24), each 16×8.
Restore those controls and their exact hitboxes. Clamp the page against the entire
available list before rendering or selecting a formula, and retain the original
server recipe indices through filtering. The screen also shows the current page
and total, output quantity and the hovered output's item tooltip. The page counter
is a small addition to the original interface.

The separately maintained Tainted Magic source was inspected read-only at commit
`c7a6174d749a20092e28d5e66cf54a0b93bfc70d`:
`src/main/java/taintedmagic/common/registry/RecipeRegistry.java:1457–1467` registers
ordinary iron ingot → `taintedmagic:shadow_metal_ingot` through
`ThaumcraftApi.addCrucibleRecipe`, gated by `SHADOWMETAL`, costing tenebrae 3,
metallum 7 and praecantatio 2. Its research must be complete for the choice to
appear. No recipe or research-gate change is needed for this navigation repair.

Regression coverage: navigation through sparse authoritative indices; every pixel
of the next-arrow hitbox; adjacent boundaries; repeated clicks at either end;
availability changes and empty lists. The native opening-snapshot scenario now
checks bundled thaumium and an API output from the same plain iron ingot, a locked
third recipe, one-formula capacity, deselection/reselection, every transmitted
preview and native menu-data synchronization.

Validation on Java 21 / Gradle 8.12.1:

- `:common:test --tests thaumcraft.client.gui.ThaumatoriumRecipeNavigationTest`:
  5 tests, zero failures, errors or skips.
- `:fabric:build`: passed, including production/test-harness isolation checks.
- `:fabric:runGameTest
  -PgameTestFilter=thaumatoriumFormsPullsAndPaysNativeOutputs,reviewThaumatoriumUsesServerRecipeSnapshot`:
  both required cases passed on a fresh dedicated-server test world.

Fabric artifact: `Thaumcraft-fabric-0.1.0-alpha.24.jar`, SHA-256
`70a13252aea6cabff005dfb17136d0fc0f49d1b6d91c6e8a9a617bda6e1b7b03`.
Local logs: `.tmp/thaumatorium-navigation-build.log` and
`.tmp/thaumatorium-navigation-gametest.log`; focused XML reports are in the usual
common/Fabric build directories.

The user's installed addon/version, live research state, rendered game screen
and NeoForge have not been exercised by this change. The native addon regression
uses an API fixture with the same ordinary iron-ingot catalyst contract.
