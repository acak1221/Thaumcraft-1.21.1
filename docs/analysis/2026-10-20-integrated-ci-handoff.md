# Integrated Fabric content handed to CI

Base content: `0fa4a08`; CI caching: `175ee4c`. The user requested uninterrupted completion,
subagents, repository commits and GitHub test execution. This checkpoint is implemented;
its final complete GameTest verdict is pending the pushed commit's GitHub Actions run.

Included: original research-note copying and whole-stack lifecycle (100/127/128); automatic
Thaumatorium and Mnemonic Matrices; 22 enchantment infusion recipes and real matrix XP phase;
Warding/Architect and Equal Trade; Arcane Lamp, Amber and Vis Charge Relay; Warp dispatcher,
six effects, Mind Spider, Eldritch Guardian/Orb, client shaders/particles/icons. Individual
oracle contracts and remaining boundaries are in the dated analysis documents for these units.

Actual local evidence:

- `full-port-integrated3`: 582 JUnit passed; 185/186 Fabric GameTests passed. The remaining
  enchantment test mutated the scheduled-task map while it was being traversed.
- `full-port-integrated4`: 582 JUnit passed; 184/186 GameTests passed. The repaired focused
  enchantment ritual passed; its natural second ritual could not discover a late fixture jar
  before the original five-second source-cache backoff. The jar is now installed before discovery.
  The new ordinary Warp scheduler test also needed its embedded connection in the native server
  connection list; this fixture is now connected to that ordinary tick loop.
- After those fixture fixes and the icon crop fix: `:common:test :fabric:clientTestClasses
  :fabric:build` passed in 56 seconds, including production/test isolation. Python 78/78 passed.
  Final GameTests are handed to CI rather than replayed locally while all agents wait.
- Local remapped Fabric JAR: 11,969,580 bytes, SHA-256
  `7e24c2f111e9bc58f4f6d411aab97c24d47b8e2520e1c1bee0c2e53caf0dc41c`.

CI now caches Gradle/Loom dependencies and task outputs. It reruns JUnit with the task-specific
`--rerun`, runs the uncached GameTest server, and preserves fresh-source/report/attribution gates.
It does not force every compilation/remapping task to execute again. Logs and reports are kept
for seven days; superseded runs on the same ref are cancelled. Documentation-only pushes skip
this expensive workflow. A CI verdict applies to its exact commit, never newer local work.

Client visuals/audio, packaged multiplayer/survival and NeoForge remain unverified. Ward behavior
forwarding/overlays, several native Trade placement topologies and remaining content are still
open work; this is not a full-port acceptance or release. R11, Repair/Haste, Deconstruction and
further lamps are prepared outside source sets and will continue while CI tests this snapshot.
