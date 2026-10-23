# t1visual: native visual fixtures (2026-10-23)

## Scope and honesty boundary

`visuals` is a standalone creative visual-fixture scope. This worktree did not contain the accepted survival saves. Its source here is a freshly generated 1.21.1 Fabric world at `fabric/run/visual-seed`, copied by the existing preparation task to a unique `visuals-smoke-*` directory. Where `fabric/run-clienttest/saves/book-smoke` exists, preparation can instead copy that local world; no earned inventory is required. It supplies the displayed items, armor and blocks explicitly; it is **not** survival acquisition evidence.

Three new sequence stages use the existing native HUD/screen capture hooks (no new screen allowlist entries or render hooks):

- `EldritchVisualClientSequence`: real Outer Lands dimension, real obelisk tile, wide and close camera positions within detailed tunnel range.
- `CabinetVisualClientSequence`: held advanced stabilizer, held aura node and populated jar node, third-person void robe front/back, actual book `useItem` into the research browser, candle/crust/stairs and glass-contained finite flux gas.
- `MirrorVisualClientSequence`: bind a supplied mirror item by native `useItemOn`, place the bound item using the same real client handler, require reciprocal authoritative links, then capture the projected field front-on and obliquely.

The new scope requires exactly ten captures, with exact names and ordering; the existing full scope remains unchanged at 66.

## Run

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/t1visual/.gradle-t1
export DISPLAY=:0 XAUTHORITY=/home/cyberfox-kali/.xauth-display0
rm -rf fabric/run-clienttest/screenshots
./gradlew :fabric:runClientSmoke -PclientSmokeScope=visuals
```

One-time seed creation if absent: start `:fabric:runServer` with the owned `fabric/run/server.properties` containing `level-name=visual-seed`, `level-type=minecraft:flat`, a free server port, and `online-mode=false`; accept the Minecraft EULA in `fabric/run/eula.txt`, then issue native `stop` after `Done`. The seed and generated run files are ignored, not committed.

## Runtime / inspected PNGs

Pending execution and image inspection. A passing capture count is not visual acceptance.

## Not reached

Thaumometer scanning/readout, excavation beam, architect overlay, radial focus menu. NeoForge and survival progression are outside this scope. Still frames do not establish animation continuity or original pixel parity.
