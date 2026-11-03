# Thaumcraft 4 Reload 0.1.0-alpha.18

- First automatic release with Fabric and NeoForge mod jars, both source archives, SHA-256 checksums and a build report tied to the source commit.
- Both loaders are built and tested before publication. Fabric also runs a second full GameTest suite and five isolated regressions, including Order golem replant seed custody.
- Gameplay is unchanged from the alpha.17 source checkpoint. This release adds the automated release process and test diagnostics.

The alpha.17 gameplay checkpoint includes native Thaumonomicon and EMI recipe transfer, the crucible aspect HUD, smoother aura-node wand charging, six deepslate infused ores, modern Minecraft adaptations and the fixes described in `CODE_REVIEW_REPORT.md`.

Minecraft 1.21.1 and Java 21. Install the mod jar for your loader with the dependencies listed in the README. Start a new world. Exact automated results and documented original-JAR oracle skips are in `BUILD_INFO.json`. Physical client, long survival and remote multiplayer acceptance are not performed by the release workflow.
