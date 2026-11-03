# Thaumcraft 4 Reload 0.1.0-alpha.22

- Fixed a client crash near suspended taint crust. Taint droplets now resolve their hanging, falling and landing sprites from the particle atlas registered by Minecraft's particle engine, including after a resource reload.
- The droplet's original colour, gravity, lifetime, forty-tick bob, collisions and jar handling are preserved.
- Includes the alpha.20 matrix goggles, crystal lighting and scanned JEI aspect-source improvements.
- Reviewed the resource supplier contract for the corrected particle class; the three vanilla sprite dependencies are unchanged. The alpha.21 candidate was withheld by this resource audit and was not published.

Minecraft 1.21.1, Java 21. Separate Fabric and NeoForge jars are provided. The reported crash was reproduced in an isolated native Fabric client; the corrected particle class passed all eight frame/lifecycle assertions before and after a resource reload. Automated server checks do not establish a full survival playthrough or remote multiplayer acceptance.
