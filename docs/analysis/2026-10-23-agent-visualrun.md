# visualrun: execution and actual image inspection

## Input and boundary

- Started on `agent/visualrun` at `2aa9f03f`, the untouched WIP visual scenes.
- All work and generated data remain in `/home/cyberfox-kali/wt/visualrun`.
- Source: a fresh locally generated flat 1.21.1 seed, `fabric/run/visual-seed`.
  The fixtures supply creative display inventory and elevated platforms, so this
  is adequate for rendering checks, not survival acquisition evidence.
- `GuiResearchBrowser` was already in `supportsCapture` and the generic
  `ScreenEvents.AFTER_INIT` registers its `afterRender` capture callback.

## Commands and compilation

Environment for every Gradle command:

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/visualrun/.gradle-visualrun
export DISPLAY=:0 XAUTHORITY=/home/cyberfox-kali/.xauth-display0
```

```sh
flock /tmp/opencode/gradle.lock -c 'cd /home/cyberfox-kali/wt/visualrun && ./gradlew :fabric:compileClientTestJava'
```

Passed unchanged, including all visual fixture APIs (11m38s, first-time isolated
cache setup). Log: `/tmp/opencode/visualrun-compile.log`. Gradle configured the
existing NeoForge project as part of its normal configuration; no NeoForge test
or build task was requested.

## Runtime and inspected frames

Seed creation used `:fabric:runServer --console=plain` under the same lock, with
accepted EULA, `level-name=visual-seed`, `level-type=minecraft:flat`, port 25637,
and `online-mode=false`. A subprocess sent native `stop` upon `Done`. The server
logged all dimensions saved, but Gradle eventually reported JVM exit 143 and
`BUILD FAILED` (21m08s). This is **not** a passing dedicated-server test. The
saved seed is used only as the input of the creative display fixtures.
Seed log: `/tmp/opencode/visualrun-seed.log`.

Client command (screenshots removed inside the lock immediately before launch):

```sh
flock /tmp/opencode/gradle.lock -c 'cd /home/cyberfox-kali/wt/visualrun && rm -rf fabric/run-clienttest/screenshots && ./gradlew :fabric:runClientSmoke -PclientSmokeScope=visuals'
```

Run 1 failed before any capture: `BlockEldritch.VARIANT` exists at compile time
but is not the family-specific state property hosted by `thaumcraft:eldritch`.
Both obelisk fixture assignments now use the established `FamilyVariant.with`
helper. The real obelisk tile assertion and all capture assertions are unchanged.
Log: `/tmp/opencode/visualrun-run1.log`.

Run 2 requests both `:fabric:compileClientTestJava :fabric:runClientSmoke` with
the same `-PclientSmokeScope=visuals`, environment, lock, and screenshot cleanup.
Log: `/tmp/opencode/visualrun-run2.log`.

Run 2 crashed before capture in `ModelTunnelField.quad`: `Not building!`.
It retained the outer translucent consumer, then requested the additive eyes
consumer before using either; a non-fixed render type switch ends the shared
builder. The contained model fix requests only the active layer's consumer
immediately before emission. No render assertion or scene was removed.

Run 3 rebuilds and repeats the same combined compile/client command.
Log: `/tmp/opencode/visualrun-run3.log`.

## Run 3: eight actual opened PNGs, then mirror timeout

Run 3 reached eight captures and failed `Mirror visual timeout at 1`. All eight
PNGs were opened with the image reader. Before the next required screenshot
cleanup, they were preserved under `.tmp/visualrun/run3/screenshots/`. Every file
there has prefix `client-smoke-0d6035b2-17a5-4572-b1fd-9187b2608d15-`:

| Suffix | What I actually saw |
| --- | --- |
| `201-outer-lands-obelisk-tunnel-wide-en.png` | A floating, narrow, dark framed obelisk with pointed caps; faint cyan marks within two black inset faces. Coherent, not shattered. No clearly readable deep tunnel/opening. |
| `202-outer-lands-obelisk-tunnel-close-en.png` | Larger coherent housing, pointed caps, black interior and faint cyan streaks. Dark, not an illuminated receding passage. |
| `203-advanced-node-stabilizer-held-en.png` | Gold-rimmed, dark 3D stabilizer visibly held at lower right. Not an empty hand or checkerboard. A glowing white/cyan node is visible in the placed glass jar. |
| `204-aura-node-held-and-jar-node-en.png` | Jar node present, but the held item is an oversized yellow/white bloom washing out the right half of the view. **Defect.** |
| `205-void-robe-front-en.png` | Purple/black hood, layered pointed shoulder plates, long robe and split side panels worn on the body. Front hood opening is black. No missing-texture checkerboard. Lighting is dark. |
| `206-void-robe-back-en.png` | Back of hood/shoulders and long purple robe; diagonal brown/gold back accessory and belt pouches visible. No checkerboard. Dark lighting and unnaturally rectangular black floor regions also visible. |
| `207-research-graph-parchment-en.png` | Actual graph icons and connecting lines, but uniform dark panel: category background entirely absent. **Defect**, cannot accept 1:2 texture density from this frame. |
| `208-candle-crust-stairs-contained-flux-en.png` | Lit pale candle with smoke, purple crust cube, dark two-step eldritch stairs, and pale lavender gas cell inside glass. All four present. |

The obelisk's weak interior is left as a documented deeper rendering gap, not
"fixed" by brightening a fixture or guessing at original projection. The model
already documents that it approximates rather than reconstructs the original
view-dependent tunnel. These images prove it no longer crashes, not effect parity.

Run 4 changes:

- Research background used the 11-argument `blit` overload in the wrong order:
  pan offsets became destination width/height (both initially zero). Corrected
  destination dimensions and UV offsets; retained the existing 1:2 sampling.
- Aura node builtin model had no first-person transforms. Added the same native
  block first-person rotation/0.4 scale used by the advanced stabilizer model.
  Node aspect values, animation and renderer strip sizes are unchanged.
- Mirror fixture now selects client slot zero before setup, matching its supplied
  server slot zero. Previously the client remained on empty slot three; server
  inventory broadcasts do not select the client's hotbar slot. Native binding,
  placement, both client linked checks and authoritative reciprocal validation
  remain unchanged.

Run 4 rebuilds and repeats the same compile/client command.
Log: `/tmp/opencode/visualrun-run4.log`. Its visual inspection is pending.

## Run 4: script passed, all ten images opened

All three sequences survived, all ten exact captures passed, and the real native
mirror binding/placement established authoritative reciprocal links. All ten
images were opened, not inferred from counts. They are preserved under
`.tmp/visualrun/run4/screenshots/`, with prefix
`client-smoke-921d6619-5f61-4a24-bfca-8a0b1b23d29c-`.

- 201/202: same coherent dark pointed obelisk and faint cyan interior marks as
  run 3. No readable receding opening: unresolved.
- 203: visibly held gold/black stabilizer, glowing node inside glass jar.
- 204: placed jar contents visible; held glow no longer washes out half the
  screen, but remains a clipped bright vertical streak at the bottom right.
  Not yet visually accepted as a properly framed node.
- 205: real purple hood, layered shoulders, front robe, split side panels.
- 206: rear hood and shoulders, long robe and gold/brown diagonal accessory with
  two belt pouches. Neither robe frame has checkerboards; both are dark, and
  floor lighting has stark rectangular black regions. Lighting anomaly unresolved.
- 207: purple nebula background (the actual supplied `gui_researchback.png`, not
  beige parchment) is now visible with graph icons and connectors. But adjacent
  repeated 112x98 crops produce obvious vertical and horizontal seams. Defect.
- 208: lit candle/smoke, purple crust, dark stairs and lavender contained gas
  all visible.
- 209: front mirror face contains a dark purple speckled field and pale inset
  border. Not a blank or missing-texture plane.
- 210: oblique mirror face retains purple field, with the other linked mirror
  visible at left. Still images establish texture visibility, not animation or
  exact original projective-motion parity.

Run 5 advances adjacent background UVs by half their screen offset rather than
repeating each crop, preserving 1:2 density while removing the artificial seams.
It also raises the held-node display by 3/16 block and removes cube-specific
first-person yaw; the crossed sprite planes are not a cube. Scale remains 0.4.
Log: `/tmp/opencode/visualrun-run5.log`. Final image inspection pending.

## Run 5: all ten opened; background fixed, node still oversized

Run 5 passed (1m19s). All ten PNGs were opened. Preserved at
`.tmp/visualrun/run5/screenshots/`, prefix
`client-smoke-e65a11c0-cd35-4c4a-a3ae-498d05eecf60-`.
201/202, 203, 205/206, 208, 209/210 show the same objects/limitations described
for run 4. 207 is now a continuous purple nebula crop with no hard crop seams.
204 shows a visible yellow-white held glow, but still far too large (rightmost
roughly quarter of the image), not accepted merely because it is no longer a
thin streak.

The 207 density was also checked against the source: source texels (10,10),
(20,20), (150,15), (180,110) exactly match PNG pixels at
`(362 + 4*x, 116 + 4*y)`: RGB (27,7,34), (34,11,44), (57,18,68), (124,39,126).
GUI scale is two, so this is exactly one source texel to two GUI pixels, not
anisotropic stretching.

Run 6 reduces only the node's first-person display scale from 0.4 to 0.125:
its maximum aspect-strip half-size is 0.8, scaled by two by the original node
renderer, giving total diameter 3.2 model units. At 0.125 display scale the
maximum diameter is 0.4, matching an ordinary held block's one-unit geometry
at 0.4. Original aspect values, strip sizes, alpha and animation are unchanged.
Log: `/tmp/opencode/visualrun-run6.log`. Final inspection pending.

## Final run 6: compiled, script passed, all ten PNGs opened

`BUILD SUCCESSFUL in 1m 14s`, `SCRIPT_PASSED`, scope `visuals`, ten captures.
All three scenes remain; no scene or assertion was deleted or weakened.
The final source/model changes were included in this compile and client run.

Exact command, after exporting the environment above:

```sh
flock /tmp/opencode/gradle.lock -c 'cd /home/cyberfox-kali/wt/visualrun && rm -rf fabric/run-clienttest/screenshots && ./gradlew :fabric:compileClientTestJava :fabric:runClientSmoke -PclientSmokeScope=visuals'
```

Every row below was opened with the image reader, not inferred from its filename.
Paths are relative to `/home/cyberfox-kali/wt/visualrun`:

| Final PNG path | Actual visual judgement |
| --- | --- |
| `fabric/run-clienttest/screenshots/client-smoke-397d835c-e777-4bce-a93c-d8d3eb4760e8-201-outer-lands-obelisk-tunnel-wide-en.png` | Obelisk clearly present: narrow coherent dark housing, pointed top/bottom, faint blue/cyan interior streaks. Mostly black inset faces, no readable illuminated receding tunnel. **Unresolved effect-readability finding.** |
| `fabric/run-clienttest/screenshots/client-smoke-397d835c-e777-4bce-a93c-d8d3eb4760e8-202-outer-lands-obelisk-tunnel-close-en.png` | Close housing/caps coherent, not shattered. Dark inset openings and faint cyan/purple marks, not a convincing deep passage. Same unresolved finding, although the real model no longer crashes. |
| `fabric/run-clienttest/screenshots/client-smoke-397d835c-e777-4bce-a93c-d8d3eb4760e8-203-advanced-node-stabilizer-held-en.png` | Gold rim and dark/red body visibly held at lower right, no checkerboard or empty hand. Jar has bright white/cyan contents. |
| `fabric/run-clienttest/screenshots/client-smoke-397d835c-e777-4bce-a93c-d8d3eb4760e8-204-aura-node-held-and-jar-node-en.png` | Bounded yellow-white held node orb, wholly within lower-right view, separate from the bright white node inside the glass jar. Oversized/clipped presentation fixed; its core is bright/saturated, not detailed aspect lobes. No missing texture. |
| `fabric/run-clienttest/screenshots/client-smoke-397d835c-e777-4bce-a93c-d8d3eb4760e8-205-void-robe-front-en.png` | Armour genuinely on body: black-faced purple hood, layered pointed shoulder plates, long central robe and split side panels. No checkerboard. Dark lighting limits texture detail; hard rectangular black floor regions remain an unresolved lighting anomaly. |
| `fabric/run-clienttest/screenshots/client-smoke-397d835c-e777-4bce-a93c-d8d3eb4760e8-206-void-robe-back-en.png` | Rear hood, pointed shoulder silhouette, long purple rear robe, diagonal brown/gold back accessory and twin tan pouches visible. No missing texture. Same dark/rectangular floor-lighting anomaly, not diagnosed or patched speculatively. |
| `fabric/run-clienttest/screenshots/client-smoke-397d835c-e777-4bce-a93c-d8d3eb4760e8-207-research-graph-parchment-en.png` | Continuous purple nebula texture behind graph nodes/connectors, not blank, not tiled crop seams. The actual asset is purple nebula, despite the capture's parchment label. Four source/PNG RGB matches confirm 1 texel → 2 GUI pixels → 4 PNG pixels, repeated against this final frame. |
| `fabric/run-clienttest/screenshots/client-smoke-397d835c-e777-4bce-a93c-d8d3eb4760e8-208-candle-crust-stairs-contained-flux-en.png` | Pale candle and wick, purple crust block, dark two-step stairs, lavender gas cell inside glass all present. No flame particle visible at this exact instant; earlier opened run-3/4/5 frames visibly show its flame/smoke. No displayed object missing. |
| `fabric/run-clienttest/screenshots/client-smoke-397d835c-e777-4bce-a93c-d8d3eb4760e8-209-linked-mirror-projected-field-front-en.png` | Frontal mirror contains dark purple speckled field with inset pale rectangular border. Textured, not a flat untextured plane. Native reciprocal link assertions passed. |
| `fabric/run-clienttest/screenshots/client-smoke-397d835c-e777-4bce-a93c-d8d3eb4760e8-210-linked-mirror-projected-field-angle-en.png` | Oblique face retains purple speckled field and border; other linked mirror visible left. Not blank. Static images do not establish exact projective motion or original effect parity. |

### Final disposition

Fixed: wrong fixture family property; shared-builder tunnel crash; stale client
mirror hotbar selection; zero-size research-background blit; repeated crop seams;
oversized/clipped first-person aura-node presentation. `GuiResearchBrowser`
capture registration needed no modification.

Left open, with actual evidence: mostly black/unreadable obelisk tunnel effect
(201/202), hard rectangular dark floor regions and dark robe presentation
(most obvious in 205/206). These are not silently accepted or hidden by fixtures.
The former needs deeper original-projection reconstruction; the latter's cause
is not established, so no lighting/gameplay change was guessed.

Not reached: animation continuity, exact original pixel/projection parity,
left-hand runtime node presentation (model transform supplied but not captured),
NeoForge, multiplayer, survival acquisition/progression, thaumometer readout,
excavation beam, architect overlay or radial focus menu. Seed generation's exit
143 remains recorded above; this client success is not a dedicated-server pass.

PNG/save files stay ignored and local. The human visual findings are committed
here; the generated harness JSON deliberately still says visual inspection
pending rather than being rewritten to imply full visual parity.
