# Thaumcraft 4 Reload 0.1.0-alpha.20

- Goggles of Revealing now show the infusion matrix's live remaining aspect demand during crafting. The quantities follow synchronized payments, while unknown aspects keep their normal hidden identity.
- Crystal clusters transmit skylight and no longer cast a full-cube ambient-occlusion shadow. Their original full-cube collision/selection, light level, placement and stabilization behavior are preserved.
- JEI's new Aspect Sources category shows which scanned items contain a selected aspect, with the amount per item. Clicking Aqua lists scanned water-bearing items such as clay. New scan knowledge appears on the next lookup; forgotten and unscanned items are excluded. Normal and advanced scan markers are supported.
- The replant custody regression explicitly tests a crop across a chunk boundary with both navigation regions loaded.

Minecraft 1.21.1, Java 21. JEI is optional; the aspect-source feature was checked with JEI 19.43.0.398. Separate Fabric and NeoForge mod/source jars, SHA-256 checksums and exact automated results are attached to this release. Focused native Fabric client checks cover the goggles, seven cluster variants and actual JEI lookups/screens. They do not establish a full survival playthrough or remote multiplayer acceptance.
