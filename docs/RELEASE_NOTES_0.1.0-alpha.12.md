# Thaumcraft 4 Reload 0.1.0-alpha.12

Fortress helmets now show only their infused mask and goggles, and full armor sets show the original ornaments. The old `isHidden` flags had been copied into modern `visible` with the wrong polarity.

Add-on compatibility: registered API recipes supply canonical object aspects; custom fixed-output infusions can explicitly supply a native aspect view; add-ons can register focus upgrades, and fully paid focal-manipulator jobs complete without querying a null/offline payer. Existing armor, particle and explosive-orb backends expose the narrow source-compatible extensions needed by external TC4 add-ons. The base JAR does not bundle an add-on.

Fabric verification: common JUnit2851 (five existing skips), native GameTest559/559, Python155 (nine existing skips), asset/scope/archive checks pass. The separate Fabric add-on passes14 JUnit and37 native tests. Client evidence uses supplied1.21.1 fixtures; fresh survival and multiplayer acquisition were not replayed.

Requires Minecraft1.21.1, Java21 and the matching loader dependencies. NeoForge verification after Fabric acceptance: build/archive isolation,15JUnit and538/538 native GameTests pass. The separate add-on targets Fabric.
