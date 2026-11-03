# Thaumcraft 4 Reload 0.1.0-alpha.23

- Fixed a client crash near suspended taint crust. Droplets now resolve their hanging, falling and landing sprites from the particle atlas registered by Minecraft's particle engine, including after a resource reload. Original droplet physics and jar handling are preserved.
- Includes the alpha.20 matrix goggles, crystal lighting and scanned JEI aspect-source improvements.
- The champion promotion regression now declares an ordinary zombie's initial spawn roll before checking the original bonuses, preventing a random pre-promotion from contaminating the baseline. The same case is checked in isolation as well as in the full suite.

Minecraft 1.21.1, Java 21. Separate Fabric and NeoForge jars are provided. The reported crash was reproduced in an isolated native Fabric client; the corrected particle class passed all eight frame/lifecycle assertions before and after a resource reload. Alpha.21 and alpha.22 candidates were withheld by release checks and were not published. Automated server checks do not establish a full survival playthrough or remote multiplayer acceptance.
