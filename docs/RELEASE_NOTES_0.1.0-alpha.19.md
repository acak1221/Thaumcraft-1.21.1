# Thaumcraft 4 Reload 0.1.0-alpha.19

- Automatic releases build and test Fabric first, then NeoForge, and publish both mod jars, both source archives, SHA-256 checksums and a build report tied to the source commit.
- Required test lists retain all 656 Fabric cases and pin all 538 NeoForge cases. Native Fabric fixtures and NeoForge capability/event fixtures are required on their respective loaders.
- Fabric runs a second full GameTest suite and six isolated regressions. The Pech pickup marker test verifies a fully empty native search box, with a separate real unmarked item outside that box, so items beyond its narrow combat lane cannot invalidate the negative control.
- Gameplay is unchanged from the alpha.17 source checkpoint. This release adds release automation, acceptance metadata and test-fixture isolation/diagnostics.

The alpha.17 gameplay checkpoint includes native Thaumonomicon and EMI recipe transfer, the crucible aspect HUD, smoother aura-node wand charging, six deepslate infused ores, modern Minecraft adaptations and the fixes described in `CODE_REVIEW_REPORT.md`.

Minecraft 1.21.1 and Java 21. Install the mod jar for your loader with the dependencies listed in the README. Start a new world. Exact automated results and documented original-JAR oracle skips are in `BUILD_INFO.json`. The release workflow does not perform physical-client, long survival or remote multiplayer acceptance, and does not establish complete gameplay parity between loaders.
